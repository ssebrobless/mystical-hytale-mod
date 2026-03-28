param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$gradleProperties = Join-Path $repoRoot "gradle.properties"
$modsDir = Join-Path $env:APPDATA "Hytale\\UserData\\Mods"

if (-not (Test-Path $gradleProperties)) {
    throw "Could not find gradle.properties at $gradleProperties"
}

$modVersion = ((Get-Content $gradleProperties | Where-Object { $_ -like 'mod_version=*' }).Split('=')[1]).Trim()
$publicJar = Join-Path $repoRoot "build\\libs\\mentees_of_the_mystical-$modVersion.jar"
$publicJarInMods = Join-Path $modsDir "mentees_of_the_mystical-$modVersion.jar"
$internalJarInMods = Join-Path $modsDir "mentees_of_the_mystical-$modVersion-internal.jar"

if (-not (Test-Path $publicJar)) {
    throw "Public release jar not found at $publicJar. Build it first."
}

New-Item -ItemType Directory -Path $modsDir -Force | Out-Null

Get-ChildItem -Path $modsDir -Filter "mentees_of_the_mystical-*.jar" -ErrorAction SilentlyContinue |
    Where-Object { $_.FullName -ne $publicJarInMods } |
    Remove-Item -Force -ErrorAction SilentlyContinue

Copy-Item -Path $publicJar -Destination $publicJarInMods -Force

if (Test-Path $internalJarInMods) {
    Remove-Item $internalJarInMods -Force -ErrorAction SilentlyContinue
}

Write-Host "MOTM public release build is now active in $modsDir"
