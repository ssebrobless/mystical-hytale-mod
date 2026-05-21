param(
    [string]$BuiltJar,
    [string]$InstalledJar,
    [string]$DiffOutput
)

$ErrorActionPreference = "Stop"

function Get-NewestJar([string]$Path, [string]$Filter) {
    Get-ChildItem -Path $Path -Filter $Filter -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Get-JarEntries([string]$JarPath) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [System.IO.Compression.ZipFile]::OpenRead($JarPath)
    try {
        $zip.Entries |
            Where-Object { $_.FullName -like "*.class" } |
            ForEach-Object { $_.FullName } |
            Sort-Object
    } finally {
        $zip.Dispose()
    }
}

$repoRoot = Split-Path -Parent $PSScriptRoot
$modsDir = Join-Path $env:APPDATA "Hytale\UserData\Mods"

if (-not $BuiltJar) {
    $built = Get-NewestJar (Join-Path $repoRoot "build\libs") "mentees_of_the_mystical-*.jar"
    if (-not $built) { throw "No built MOTM jar found under build\libs." }
    $BuiltJar = $built.FullName
}

if (-not $InstalledJar) {
    $installed = Get-NewestJar $modsDir "mentees_of_the_mystical-*.jar"
    if (-not $installed) { throw "No installed MOTM jar found under $modsDir." }
    $InstalledJar = $installed.FullName
}

if (-not (Test-Path -LiteralPath $BuiltJar)) { throw "Built jar not found: $BuiltJar" }
if (-not (Test-Path -LiteralPath $InstalledJar)) { throw "Installed jar not found: $InstalledJar" }

$builtEntries = @(Get-JarEntries $BuiltJar)
$installedEntries = @(Get-JarEntries $InstalledJar)
$diff = @(Compare-Object $builtEntries $installedEntries)

Write-Host "[audit-classes] Built:     $BuiltJar"
Write-Host "[audit-classes] Installed: $InstalledJar"
Write-Host "[audit-classes] Built classes: $($builtEntries.Count)"
Write-Host "[audit-classes] Installed classes: $($installedEntries.Count)"

if ($DiffOutput) {
    $parent = Split-Path -Parent $DiffOutput
    if ($parent -and -not (Test-Path -LiteralPath $parent)) {
        New-Item -ItemType Directory -Path $parent -Force | Out-Null
    }
    $diff | Out-File -FilePath $DiffOutput -Encoding UTF8
}

if ($diff.Count -gt 0) {
    Write-Host "[audit-classes] FAIL: built and installed class tables differ."
    $diff | Select-Object -First 80 | Format-Table -AutoSize
    exit 1
}

Write-Host "[audit-classes] PASS: built and installed class tables match."
exit 0
