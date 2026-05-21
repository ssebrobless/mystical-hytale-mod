param(
    [Parameter(Mandatory = $true)]
    [string]$Pattern,
    [switch]$Javap,
    [string]$ClassName
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$serverJar = Join-Path $env:APPDATA "Hytale\install\release\package\game\latest\Server\HytaleServer.jar"
if (-not (Test-Path -LiteralPath $serverJar)) {
    throw "HytaleServer.jar not found: $serverJar"
}

$jdkBin = Join-Path $repoRoot ".tools\jdk-25\bin"
$jarExe = Join-Path $jdkBin "jar.exe"
$javapExe = Join-Path $jdkBin "javap.exe"
if (-not (Test-Path -LiteralPath $jarExe)) { throw "jar.exe not found: $jarExe" }

if ($Javap) {
    if (-not $ClassName) { $ClassName = $Pattern }
    if (-not (Test-Path -LiteralPath $javapExe)) { throw "javap.exe not found: $javapExe" }
    & $javapExe -p -cp $serverJar $ClassName
    exit $LASTEXITCODE
}

& $jarExe tf $serverJar | Select-String -Pattern $Pattern
