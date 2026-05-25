param(
    [switch]$Build,
    [switch]$InstallMod,
    [switch]$PublicRelease,
    [switch]$DiagnoseOnly,
    [switch]$NoDownload,
    [string]$JavaHome = "",
    [string]$HytaleRoot = ""
)

$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$toolsDir = Join-Path $repoRoot ".tools"
$jdkVersion = "25"
$portableJdkRoot = Join-Path $toolsDir "jdk-$jdkVersion"
$script:SetupContext = [ordered]@{
    repository = $repoRoot
    toolsDir = $toolsDir
    requestedJavaHome = $JavaHome
    requestedHytaleRoot = $HytaleRoot
    powershellVersion = $PSVersionTable.PSVersion.ToString()
    osDescription = [System.Runtime.InteropServices.RuntimeInformation]::OSDescription
    processArchitecture = [System.Runtime.InteropServices.RuntimeInformation]::ProcessArchitecture.ToString()
}

function Test-IsWindows {
    return $env:OS -eq "Windows_NT"
}

function Test-IsMacOS {
    return $global:IsMacOS -eq $true
}

function Get-JavaExecutableName {
    if (Test-IsWindows) { return "java.exe" }
    return "java"
}

function Get-JavacExecutableName {
    if (Test-IsWindows) { return "javac.exe" }
    return "javac"
}

function Test-JdkHome {
    param([string]$Path)

    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path)) {
        return $false
    }

    $java = Join-Path (Join-Path $Path "bin") (Get-JavaExecutableName)
    $javac = Join-Path (Join-Path $Path "bin") (Get-JavacExecutableName)
    if (-not (Test-Path -LiteralPath $java) -or -not (Test-Path -LiteralPath $javac)) {
        return $false
    }

    $versionOutput = ""
    try {
        $versionOutput = (& $javac -version 2>&1 | Out-String).Trim()
    } catch {
        return $false
    }

    if ($LASTEXITCODE -ne 0) {
        return $false
    }

    return $versionOutput -match "javac\s+$jdkVersion(\D|$)"
}

function Resolve-AdoptiumOs {
    if (Test-IsWindows) { return "windows" }
    if (Test-IsMacOS) { return "mac" }
    if ($global:IsLinux -eq $true) { return "linux" }
    throw "Unsupported OS for automatic JDK download. Install JDK $jdkVersion manually and pass -JavaHome."
}

function Resolve-AdoptiumArch {
    $arch = [System.Runtime.InteropServices.RuntimeInformation]::ProcessArchitecture.ToString().ToLowerInvariant()
    switch ($arch) {
        "x64" { return "x64" }
        "arm64" { return "aarch64" }
        default { throw "Unsupported CPU architecture for automatic JDK download: $arch" }
    }
}

function Find-JdkHomeInExtract {
    param([string]$ExtractDir)

    $javaName = Get-JavaExecutableName
    $javacName = Get-JavacExecutableName
    $candidates = Get-ChildItem -LiteralPath $ExtractDir -Recurse -Directory -ErrorAction SilentlyContinue |
        Where-Object {
            (Test-Path -LiteralPath (Join-Path (Join-Path $_.FullName "bin") $javaName)) -and
            (Test-Path -LiteralPath (Join-Path (Join-Path $_.FullName "bin") $javacName))
        } |
        Sort-Object { $_.FullName.Length }

    $first = $candidates | Select-Object -First 1
    if ($null -eq $first) {
        throw "Could not find a JDK home after extracting the archive."
    }
    return $first.FullName
}

function Invoke-DownloadFile {
    param(
        [string]$Uri,
        [string]$OutFile,
        [int]$MaxAttempts = 3
    )

    if ($PSVersionTable.PSVersion.Major -lt 6) {
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    }

    $lastError = $null
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        try {
            Write-Host "[ensure-dev-environment] Download attempt $attempt/${MaxAttempts}: $Uri"
            $downloadArgs = @{
                Uri = $Uri
                OutFile = $OutFile
            }
            if ($PSVersionTable.PSVersion.Major -lt 6) {
                $downloadArgs.UseBasicParsing = $true
            }
            Invoke-WebRequest @downloadArgs
            if (Test-Path -LiteralPath $OutFile) {
                return
            }
            throw "Download completed but $OutFile was not created."
        } catch {
            $lastError = $_
            if ($attempt -lt $MaxAttempts) {
                Start-Sleep -Seconds ([Math]::Min(10, 2 * $attempt))
            }
        }
    }

    throw "Failed to download $Uri after $MaxAttempts attempts. Last error: $($lastError.Exception.Message)"
}

function Install-PortableJdk {
    if ($NoDownload) {
        throw "JDK $jdkVersion is missing and -NoDownload was supplied. Install it manually or rerun without -NoDownload."
    }

    if (-not (Test-Path -LiteralPath $toolsDir)) {
        New-Item -ItemType Directory -Path $toolsDir | Out-Null
    }

    $os = Resolve-AdoptiumOs
    $arch = Resolve-AdoptiumArch
    $extension = if (Test-IsWindows) { "zip" } else { "tar.gz" }
    $archivePath = Join-Path $toolsDir "jdk-$jdkVersion-$os-$arch.$extension"
    $extractDir = Join-Path $toolsDir "jdk-$jdkVersion-extract"
    $downloadUrl = "https://api.adoptium.net/v3/binary/latest/$jdkVersion/ga/$os/$arch/jdk/hotspot/normal/eclipse?project=jdk"
    $script:SetupContext.jdkDownloadUrl = $downloadUrl

    if (Test-Path -LiteralPath $archivePath) {
        Remove-Item -LiteralPath $archivePath -Force
    }
    if (Test-Path -LiteralPath $extractDir) {
        Remove-Item -LiteralPath $extractDir -Recurse -Force
    }
    if (Test-Path -LiteralPath $portableJdkRoot) {
        Remove-Item -LiteralPath $portableJdkRoot -Recurse -Force
    }

    Write-Host "[ensure-dev-environment] Downloading Temurin JDK $jdkVersion for $os/$arch..."
    Invoke-DownloadFile -Uri $downloadUrl -OutFile $archivePath

    New-Item -ItemType Directory -Path $extractDir -Force | Out-Null
    Write-Host "[ensure-dev-environment] Extracting JDK..."
    if (Test-IsWindows) {
        Expand-Archive -Path $archivePath -DestinationPath $extractDir -Force
    } else {
        & tar -xzf $archivePath -C $extractDir
        if ($LASTEXITCODE -ne 0) {
            throw "tar failed while extracting $archivePath"
        }
    }

    $extractedHome = Find-JdkHomeInExtract $extractDir
    Move-Item -LiteralPath $extractedHome -Destination $portableJdkRoot
    Remove-Item -LiteralPath $extractDir -Recurse -Force
    Remove-Item -LiteralPath $archivePath -Force
}

function Resolve-JavaHome {
    if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
        if (-not (Test-JdkHome $JavaHome)) {
            throw "The supplied -JavaHome is not a usable JDK $jdkVersion home with java and javac: $JavaHome"
        }
        return (Resolve-Path -LiteralPath $JavaHome).Path
    }

    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME) -and (Test-JdkHome $env:JAVA_HOME)) {
        return (Resolve-Path -LiteralPath $env:JAVA_HOME).Path
    }

    if (Test-JdkHome $portableJdkRoot) {
        return (Resolve-Path -LiteralPath $portableJdkRoot).Path
    }

    if (Test-IsMacOS) {
        $macCandidates = @(
            "/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home",
            "/opt/homebrew/opt/openjdk@25",
            "/usr/local/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home",
            "/usr/local/opt/openjdk@25"
        )
        foreach ($candidate in $macCandidates) {
            if (Test-JdkHome $candidate) {
                return (Resolve-Path -LiteralPath $candidate).Path
            }
        }
    }

    if (Test-IsWindows) {
        $roots = @(
            (Join-Path $env:ProgramFiles "Eclipse Adoptium"),
            (Join-Path $env:ProgramFiles "Java")
        )
        foreach ($root in $roots) {
            if ([string]::IsNullOrWhiteSpace($root) -or -not (Test-Path -LiteralPath $root)) {
                continue
            }
            $candidate = Get-ChildItem -LiteralPath $root -Directory -Filter "jdk-25*" -ErrorAction SilentlyContinue |
                Sort-Object LastWriteTime -Descending |
                Select-Object -First 1
            if ($candidate -and (Test-JdkHome $candidate.FullName)) {
                return $candidate.FullName
            }
        }
    }

    Install-PortableJdk
    if (-not (Test-JdkHome $portableJdkRoot)) {
        throw "Portable JDK installation finished but $portableJdkRoot is not usable."
    }
    return (Resolve-Path -LiteralPath $portableJdkRoot).Path
}

function Resolve-HytaleRoot {
    if (-not [string]::IsNullOrWhiteSpace($HytaleRoot)) {
        return $HytaleRoot
    }
    if (-not [string]::IsNullOrWhiteSpace($env:HYTALE_ROOT)) {
        return $env:HYTALE_ROOT
    }
    if (Test-IsWindows -and [string]::IsNullOrWhiteSpace($env:APPDATA)) {
        $env:APPDATA = [Environment]::GetFolderPath([Environment+SpecialFolder]::ApplicationData)
    }
    if (Test-IsMacOS -and [string]::IsNullOrWhiteSpace($env:APPDATA)) {
        $env:APPDATA = Join-Path $HOME "Library/Application Support"
    }
    if (-not [string]::IsNullOrWhiteSpace($env:APPDATA)) {
        return (Join-Path $env:APPDATA "Hytale")
    }
    return ""
}

function Get-HytaleServerVersion {
    param([string]$ServerJarPath)

    if ([string]::IsNullOrWhiteSpace($ServerJarPath) -or -not (Test-Path -LiteralPath $ServerJarPath)) {
        return "*"
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($ServerJarPath)
    try {
        $entry = $zip.GetEntry("META-INF/MANIFEST.MF")
        if ($null -eq $entry) {
            return "*"
        }

        $reader = New-Object System.IO.StreamReader($entry.Open())
        try {
            $manifestText = $reader.ReadToEnd()
        } finally {
            $reader.Dispose()
        }
    } finally {
        $zip.Dispose()
    }

    foreach ($line in ($manifestText -split "`r?`n")) {
        if ($line -like "Implementation-Version:*") {
            return $line.Substring("Implementation-Version:".Length).Trim()
        }
    }
    return "*"
}

function Resolve-GradleWrapper {
    $wrapperName = if (Test-IsWindows) { "gradlew.bat" } else { "gradlew" }
    $wrapper = Join-Path $repoRoot $wrapperName
    if (-not (Test-Path -LiteralPath $wrapper)) {
        throw "Gradle wrapper is missing at $wrapper. Regenerate with: gradle wrapper --gradle-version 9.5.1 --distribution-type bin"
    }
    return $wrapper
}

function New-SetupDiagnosticBundle {
    param([object]$Failure)

    $diagRoot = Join-Path $repoRoot "audits/setup-diagnostics"
    New-Item -ItemType Directory -Path $diagRoot -Force | Out-Null
    $diagDir = Join-Path $diagRoot (Get-Date -Format "yyyy-MM-ddTHH-mm-ss")
    New-Item -ItemType Directory -Path $diagDir -Force | Out-Null

    $script:SetupContext.envAPPDATA = $env:APPDATA
    $script:SetupContext.envHYTALE_ROOT = $env:HYTALE_ROOT
    $script:SetupContext.envJAVA_HOME = $env:JAVA_HOME
    $script:SetupContext.path = $env:PATH
    $script:SetupContext.hytaleRootExists = if ($script:SetupContext.resolvedHytaleRoot) {
        Test-Path -LiteralPath $script:SetupContext.resolvedHytaleRoot
    } else {
        $false
    }
    $script:SetupContext.serverJarExists = if ($script:SetupContext.serverJar) {
        Test-Path -LiteralPath $script:SetupContext.serverJar
    } else {
        $false
    }
    $script:SetupContext.modsDirExists = if ($script:SetupContext.modsDir) {
        Test-Path -LiteralPath $script:SetupContext.modsDir
    } else {
        $false
    }

    $script:SetupContext | ConvertTo-Json -Depth 6 |
        Set-Content -LiteralPath (Join-Path $diagDir "setup-context.json") -Encoding UTF8

    if ($Failure) {
        ($Failure | Out-String) |
            Set-Content -LiteralPath (Join-Path $diagDir "setup-error.txt") -Encoding UTF8
    }

    $commands = @("pwsh", "powershell", "git", "java", "javac")
    $commandInfo = foreach ($command in $commands) {
        $resolved = Get-Command $command -ErrorAction SilentlyContinue
        [PSCustomObject]@{
            command = $command
            source = if ($resolved) { $resolved.Source } else { "" }
        }
    }
    $commandInfo | ConvertTo-Json -Depth 4 |
        Set-Content -LiteralPath (Join-Path $diagDir "commands.json") -Encoding UTF8

    return $diagDir
}

function Get-RecoveryHint {
    param([string]$Message)

    if ($Message -match "HytaleServer\.jar|Hytale root") {
        return @(
            "Launch or update Hytale once through the official launcher, then rerun this script.",
            "If Hytale is installed somewhere unusual, rerun with -HytaleRoot or set HYTALE_ROOT to the folder that contains install/ and UserData/."
        )
    }
    if ($Message -match "download|Adoptium|network|TLS|SSL|proxy") {
        return @(
            "Check network/proxy access to api.adoptium.net and services.gradle.org, then rerun this script.",
            "As a fallback, install Temurin/OpenJDK 25 manually and rerun with -JavaHome."
        )
    }
    if ($Message -match "Gradle wrapper|gradlew") {
        return @(
            "Make sure the repository was fully pulled, including gradlew, gradlew.bat, and gradle/wrapper/.",
            "If wrapper download fails, check access to services.gradle.org and rerun."
        )
    }
    if ($Message -match "JDK 25|JavaHome|java and javac") {
        return @(
            "Install Temurin/OpenJDK 25 or let this script download it into .tools/jdk-25.",
            "If JAVA_HOME points at another Java version, unset it or pass -JavaHome to a JDK 25 home."
        )
    }
    return @(
        "Rerun scripts/diagnose-dev-environment.ps1 and inspect the diagnostic bundle.",
        "If the failure is in Gradle, rerun the printed wrapper command with --stacktrace."
    )
}

function Write-SetupFailure {
    param([object]$Failure)

    $message = $Failure.Exception.Message
    $diagDir = New-SetupDiagnosticBundle -Failure $Failure
    Write-Host ""
    Write-Host "MOTM development setup failed."
    Write-Host "Diagnostic bundle: $diagDir"
    Write-Host "Error: $message"
    Write-Host ""
    Write-Host "Recovery hints:"
    foreach ($hint in (Get-RecoveryHint $message)) {
        Write-Host "- $hint"
    }
    Write-Host ""
    Write-Host "Agent next command:"
    Write-Host "  pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/diagnose-dev-environment.ps1"
}

try {
    $resolvedJavaHome = Resolve-JavaHome
    $script:SetupContext.resolvedJavaHome = $resolvedJavaHome
    $env:JAVA_HOME = $resolvedJavaHome
    $env:PATH = (Join-Path $resolvedJavaHome "bin") + [IO.Path]::PathSeparator + $env:PATH

    $resolvedHytaleRoot = Resolve-HytaleRoot
    $script:SetupContext.resolvedHytaleRoot = $resolvedHytaleRoot
    $serverJar = if ([string]::IsNullOrWhiteSpace($resolvedHytaleRoot)) {
        ""
    } else {
        Join-Path $resolvedHytaleRoot "install/release/package/game/latest/Server/HytaleServer.jar"
    }
    $modsDir = if ([string]::IsNullOrWhiteSpace($resolvedHytaleRoot)) {
        ""
    } else {
        Join-Path $resolvedHytaleRoot "UserData/Mods"
    }
    $script:SetupContext.serverJar = $serverJar
    $script:SetupContext.modsDir = $modsDir
    $wrapperPath = Resolve-GradleWrapper
    $script:SetupContext.gradleWrapper = $wrapperPath
    $serverVersion = Get-HytaleServerVersion $serverJar
    $script:SetupContext.serverVersion = $serverVersion
    $serverJarExists = (-not [string]::IsNullOrWhiteSpace($serverJar)) -and (Test-Path -LiteralPath $serverJar)

    Write-Host "[ensure-dev-environment] Repository: $repoRoot"
    Write-Host "[ensure-dev-environment] Java home: $resolvedJavaHome"
    Write-Host "[ensure-dev-environment] Gradle wrapper: $wrapperPath"
    Write-Host "[ensure-dev-environment] Hytale root: $resolvedHytaleRoot"
    Write-Host "[ensure-dev-environment] Hytale server jar: $serverJar"
    Write-Host "[ensure-dev-environment] Hytale server jar exists: $serverJarExists"
    Write-Host "[ensure-dev-environment] Hytale mods dir: $modsDir"

    if ($DiagnoseOnly) {
        Write-Host ""
        Write-Host "Suggested next steps:"
        if (-not $serverJarExists) {
            Write-Host "- Launch/update Hytale once through the official launcher so HytaleServer.jar exists."
            Write-Host "- If Hytale is in a nonstandard location, rerun with -HytaleRoot or set HYTALE_ROOT."
        } else {
            Write-Host "- Build/install: scripts/build-install.ps1"
            Write-Host "- Wrapper build: $wrapperPath -Dorg.gradle.java.installations.paths=`"$resolvedJavaHome`" -Phytale_root=`"$resolvedHytaleRoot`" build installMod"
        }
        exit 0
    }

    if (-not $serverJarExists) {
        throw "Could not find HytaleServer.jar. Launch/update Hytale through the official launcher, or rerun with -HytaleRoot."
    }

    if ($Build -or $InstallMod) {
        $buildChannel = if ($PublicRelease) { "public" } else { "internal" }
        $gradleArgs = @(
            "-Dorg.gradle.java.installations.paths=$resolvedJavaHome",
            "-Phytale_root=$resolvedHytaleRoot",
            "-Pserver_version=$serverVersion",
            "-Pmotm_build_channel=$buildChannel",
            "build"
        )
        if ($InstallMod) {
            $gradleArgs += "installMod"
        }
        $script:SetupContext.gradleArgs = $gradleArgs

        Push-Location $repoRoot
        try {
            Write-Host "[ensure-dev-environment] Running: $wrapperPath $($gradleArgs -join ' ')"
            & $wrapperPath @gradleArgs
        } finally {
            Pop-Location
        }
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle wrapper failed with exit code $LASTEXITCODE"
        }
    }
} catch {
    Write-SetupFailure $_
    exit 1
}
