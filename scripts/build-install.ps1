param(
    [switch]$BuildOnly,
    [switch]$PublicRelease
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$toolsDir = Join-Path $repoRoot ".tools"
$gradleVersion = "9.1.0"
$gradleZip = Join-Path $toolsDir "gradle-$gradleVersion-bin.zip"
$gradleHome = Join-Path $toolsDir "gradle-$gradleVersion"
$gradleExe = Join-Path $gradleHome "bin\\gradle.bat"
$gradleUrl = "https://services.gradle.org/distributions/gradle-$gradleVersion-bin.zip"
$jdkVersion = "25"
$jdkRoot = Join-Path $toolsDir "jdk-$jdkVersion"
$jdkZip = Join-Path $toolsDir "jdk-$jdkVersion.zip"
$jdkJavaExe = Join-Path $jdkRoot "bin\\java.exe"
$jdkJavacExe = Join-Path $jdkRoot "bin\\javac.exe"
$jdkDownloadUrl = "https://api.adoptium.net/v3/binary/latest/$jdkVersion/ga/windows/x64/jdk/hotspot/normal/eclipse?project=jdk"
$hytaleServerJar = Join-Path $env:APPDATA "Hytale\\install\\release\\package\\game\\latest\\Server\\HytaleServer.jar"

function Install-PortableJdk {
    param(
        [string]$DestinationRoot,
        [string]$ArchivePath,
        [string]$DownloadUrl
    )

    $extractDir = Join-Path $toolsDir "jdk-$jdkVersion-extract"

    if (Test-Path $ArchivePath) {
        Remove-Item $ArchivePath -Force
    }

    if (Test-Path $extractDir) {
        Remove-Item $extractDir -Recurse -Force
    }

    Write-Host "Downloading Temurin JDK $jdkVersion..."
    Invoke-WebRequest -Uri $DownloadUrl -OutFile $ArchivePath

    Write-Host "Extracting JDK..."
    Expand-Archive -Path $ArchivePath -DestinationPath $extractDir -Force

    $extractedRoot = Get-ChildItem -Path $extractDir -Directory | Select-Object -First 1
    if ($null -eq $extractedRoot) {
        throw "Could not find the extracted JDK directory in $extractDir"
    }

    if (Test-Path $DestinationRoot) {
        Remove-Item $DestinationRoot -Recurse -Force
    }

    Move-Item -Path $extractedRoot.FullName -Destination $DestinationRoot
    Remove-Item $extractDir -Recurse -Force
}

function Get-HytaleServerVersion {
    param(
        [string]$ServerJarPath
    )

    if (-not (Test-Path $ServerJarPath)) {
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

if (-not (Test-Path $toolsDir)) {
    New-Item -ItemType Directory -Path $toolsDir | Out-Null
}

if (-not (Test-Path $gradleExe)) {
    Write-Host "Downloading Gradle $gradleVersion..."
    Invoke-WebRequest -Uri $gradleUrl -OutFile $gradleZip

    Write-Host "Extracting Gradle..."
    Expand-Archive -Path $gradleZip -DestinationPath $toolsDir -Force
}

if (-not (Test-Path $jdkJavaExe) -or -not (Test-Path $jdkJavacExe)) {
    Install-PortableJdk -DestinationRoot $jdkRoot -ArchivePath $jdkZip -DownloadUrl $jdkDownloadUrl
}

if (-not (Test-Path $jdkJavaExe) -or -not (Test-Path $jdkJavacExe)) {
    throw "Could not find the portable JDK at $jdkRoot after extraction."
}

$env:JAVA_HOME = $jdkRoot
$env:PATH = "$jdkRoot\\bin;$env:PATH"
$serverVersion = Get-HytaleServerVersion -ServerJarPath $hytaleServerJar
$buildChannel = if ($PublicRelease) { "public" } else { "internal" }

$tasks = @("build")
if (-not $BuildOnly) {
    $tasks += "installMod"
}

Push-Location $repoRoot
try {
    & $gradleExe "-Dorg.gradle.java.installations.paths=$jdkRoot" "-Pserver_version=$serverVersion" "-Pmotm_build_channel=$buildChannel" @tasks

    if (-not $BuildOnly) {
        $modsDir = Join-Path $env:APPDATA "Hytale\\UserData\\Mods"
        $modVersion = ((Get-Content (Join-Path $repoRoot 'gradle.properties') | Where-Object { $_ -like 'mod_version=*' }).Split('=')[1])
        $jarName = if ($buildChannel -eq "internal") {
            "mentees_of_the_mystical-$modVersion-internal.jar"
        } else {
            "mentees_of_the_mystical-$modVersion.jar"
        }
        $builtJar = Join-Path $repoRoot "build\\libs\\$jarName"

        Get-ChildItem -Path $modsDir -Filter "mentees_of_the_mystical-*.jar" -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -ne $builtJar } |
            Remove-Item -Force -ErrorAction SilentlyContinue

        if (Test-Path $builtJar) {
            Copy-Item -Path $builtJar -Destination $modsDir -Force
        }
    }
} finally {
    Pop-Location
}
