param(
    [string]$SaveName = "MOTM Creative Test",
    [string]$WorldName,
    [switch]$SkipLogRotate,
    [switch]$SkipClassAudit,
    [switch]$LaunchAndLoad,
    [switch]$Setup,
    [switch]$EnsureFlatlands,
    [ValidateSet("Launcher", "Auto", "Direct")]
    [string]$LaunchStrategy = "Launcher"
)

$ErrorActionPreference = "Stop"

$hytaleInstallRoot = Join-Path $env:APPDATA "Hytale"

function Test-HytaleJavaPath([string]$Path) {
    return (-not [string]::IsNullOrWhiteSpace($Path)) -and
        $Path.StartsWith($hytaleInstallRoot, [System.StringComparison]::OrdinalIgnoreCase)
}

function Stop-HytaleProcess {
    $processes = Get-Process -ErrorAction SilentlyContinue |
        Where-Object {
            $_.ProcessName -like "Hytale*" -or
            $_.ProcessName -eq "CortexLauncherService" -or
            ($_.ProcessName -eq "java" -and (Test-HytaleJavaPath $_.Path))
        }

    foreach ($process in $processes) {
        try {
            $path = $process.Path
        } catch {
            $path = ""
        }
        Write-Host "[cold-launch] Stopping $($process.ProcessName) PID=$($process.Id) Path=$path"
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    }
}

function Get-NewestJar([string]$Path, [string]$Filter) {
    Get-ChildItem -Path $Path -Filter $Filter -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($WorldName)) {
    $WorldName = $SaveName
} else {
    $SaveName = $WorldName
}
$modsDir = Join-Path $env:APPDATA "Hytale\UserData\Mods"
$saveLogDir = Join-Path $env:APPDATA ("Hytale\UserData\Saves\" + $SaveName + "\logs")
$clientLogDir = Join-Path $env:APPDATA "Hytale\UserData\Logs"

Stop-HytaleProcess
Start-Sleep -Seconds 2

$leftovers = Get-Process -ErrorAction SilentlyContinue |
    Where-Object {
        $_.ProcessName -like "Hytale*" -or
        ($_.ProcessName -eq "java" -and (Test-HytaleJavaPath $_.Path))
    }
if ($leftovers) {
    $names = ($leftovers | ForEach-Object { "$($_.ProcessName):$($_.Id)" }) -join ", "
    throw "Hytale process(es) still running after stop: $names"
}

$built = Get-NewestJar (Join-Path $repoRoot "build\libs") "mentees_of_the_mystical-*.jar"
$installed = Get-NewestJar $modsDir "mentees_of_the_mystical-*.jar"
if ($built -and ((-not $installed) -or ($built.LastWriteTime -gt $installed.LastWriteTime))) {
    if (-not (Test-Path -LiteralPath $modsDir)) {
        New-Item -ItemType Directory -Path $modsDir -Force | Out-Null
    }
    Copy-Item -LiteralPath $built.FullName -Destination (Join-Path $modsDir $built.Name) -Force
    Write-Host "[cold-launch] Re-installed jar: $($built.Name)"
}

if (-not $SkipClassAudit) {
    & (Join-Path $PSScriptRoot "audit-classes.ps1")
    if ($LASTEXITCODE -ne 0) {
        throw "Class-table audit failed."
    }
}

if (-not $SkipLogRotate) {
    $stamp = Get-Date -Format "yyyy-MM-dd_HH-mm-ss"
    foreach ($dir in @($saveLogDir, $clientLogDir)) {
        if (-not (Test-Path -LiteralPath $dir)) { continue }
        $archive = Join-Path $dir "archive"
        if (-not (Test-Path -LiteralPath $archive)) {
            New-Item -ItemType Directory -Path $archive -Force | Out-Null
        }
        Get-ChildItem -Path $dir -File -Filter "*.log" -ErrorAction SilentlyContinue |
            ForEach-Object {
                $target = Join-Path $archive ($stamp + "_" + $_.Name)
                Move-Item -LiteralPath $_.FullName -Destination $target -Force
                Write-Host "[cold-launch] Archived log: $($_.FullName)"
            }
    }
}

Write-Host "[cold-launch] PASS: Hytale stopped, classes audited, logs prepared."

if ($LaunchAndLoad) {
    & (Join-Path $PSScriptRoot "start-hytale.ps1") -WorldName $WorldName -Strategy $LaunchStrategy
    if ($LASTEXITCODE -ne 0) {
        throw "start-hytale failed."
    }
    & (Join-Path $PSScriptRoot "load-world.ps1") -WorldName $WorldName
    if ($LASTEXITCODE -ne 0) {
        throw "load-world failed."
    }
    if ($EnsureFlatlands) {
        & (Join-Path $PSScriptRoot "ensure-flatlands.ps1")
        if ($LASTEXITCODE -ne 0) {
            throw "ensure-flatlands failed."
        }
    }
    if ($Setup) {
        & (Join-Path $PSScriptRoot "setup-test-world.ps1") -WorldName $WorldName
        if ($LASTEXITCODE -ne 0) {
            throw "setup-test-world failed."
        }
    }
}
