param(
    [string]$RunId = "",
    [string[]]$Keywords = @(
        "Stone", "Earth", "Mace", "Metal", "Crystal", "Sand", "Water", "Ice",
        "Wind", "Lightning", "Smoke", "Void", "Poison", "Acid", "Fire",
        "Root", "Frost", "Spark", "Slug", "Golem", "Scarak", "Frog"
    ),
    [int]$MaxPerKeyword = 80
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}

$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path "harness/assets" $RunId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$report = New-Object System.Collections.Generic.List[string]

$latestRoot = Join-Path $env:APPDATA "Hytale\install\release\package\game\latest"
$assetsZip = Join-Path $latestRoot "Assets.zip"
$serverJar = Join-Path $latestRoot "Server\HytaleServer.jar"
if (-not (Test-Path -LiteralPath $assetsZip)) {
    throw "Assets.zip not found at $assetsZip"
}
if (-not (Test-Path -LiteralPath $serverJar)) {
    throw "HytaleServer.jar not found at $serverJar"
}

Add-Type -AssemblyName System.IO.Compression.FileSystem

function Get-ZipEntries([string]$Path) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try {
        return @($zip.Entries | ForEach-Object { $_.FullName })
    } finally {
        $zip.Dispose()
    }
}

function Write-Matches([string[]]$Entries, [string]$Kind, [string]$Pattern, [string]$FileName) {
    $matches = $Entries |
        Where-Object { $_ -match $Pattern } |
        Sort-Object -Unique
    $matches | Set-Content -LiteralPath (Join-Path $outDir $FileName) -Encoding UTF8
    $report.Add("- $Kind matches: $($matches.Count) -> $FileName")
    return $matches
}

$assetEntries = Get-ZipEntries $assetsZip
$jarEntries = Get-ZipEntries $serverJar

$report.Add("# Hytale Asset Discovery")
$report.Add("")
$report.Add("- Run: $RunId")
$report.Add("- Assets.zip: $assetsZip")
$report.Add("- HytaleServer.jar: $serverJar")
$report.Add("- Assets.zip entries: $($assetEntries.Count)")
$report.Add("- HytaleServer.jar entries: $($jarEntries.Count)")
$report.Add("")

Write-Matches $assetEntries "Particle" '\.(particlespawner|particlesystem)$' "particles-all.txt" | Out-Null
Write-Matches $assetEntries "Entity effect" 'Entity/Effects|Effects/.+\.json$' "effects-candidates.txt" | Out-Null
Write-Matches $assetEntries "Model" '\.blockymodel$' "models-all.txt" | Out-Null
Write-Matches $assetEntries "UI" '\.ui$' "ui-all.txt" | Out-Null
Write-Matches $jarEntries "API class" 'CustomUI|InteractiveCustomUI|UICommandBuilder|OpenCustomUI|InteractionContext|Teleport|TransformComponent|Velocity|DamageSystems|PlayerChatEvent|PlayerReadyEvent' "api-classes-key.txt" | Out-Null

$keywordRows = New-Object System.Collections.Generic.List[string]
foreach ($keyword in $Keywords) {
    $escaped = [regex]::Escape($keyword)
    $particleHits = $assetEntries |
        Where-Object { $_ -match '\.(particlespawner|particlesystem)$' -and $_ -match $escaped } |
        Sort-Object -Unique |
        Select-Object -First $MaxPerKeyword
    $modelHits = $assetEntries |
        Where-Object { $_ -match '\.blockymodel$' -and $_ -match $escaped } |
        Sort-Object -Unique |
        Select-Object -First ([Math]::Max(10, [int]($MaxPerKeyword / 4)))
    $keywordRows.Add("## $keyword")
    $keywordRows.Add("")
    $keywordRows.Add("### Particles")
    if ($particleHits) {
        $particleHits | ForEach-Object { $keywordRows.Add("- $_") }
    } else {
        $keywordRows.Add("- none")
    }
    $keywordRows.Add("")
    $keywordRows.Add("### Models")
    if ($modelHits) {
        $modelHits | ForEach-Object { $keywordRows.Add("- $_") }
    } else {
        $keywordRows.Add("- none")
    }
    $keywordRows.Add("")
}
$keywordRows | Set-Content -LiteralPath (Join-Path $outDir "keyword-catalog.md") -Encoding UTF8
$report.Add("- Keyword catalog -> keyword-catalog.md")

$resolverPath = Join-Path $repoRoot "src\main\java\com\motm\util\HytaleAssetResolver.java"
if (Test-Path -LiteralPath $resolverPath) {
    $resolverText = Get-Content -LiteralPath $resolverPath -Raw
    $usedAssets = [regex]::Matches($resolverText, '"([^"]+\.(?:particlespawner|particlesystem|blockymodel))"') |
        ForEach-Object { $_.Groups[1].Value } |
        Sort-Object -Unique
    $usedAssets | Set-Content -LiteralPath (Join-Path $outDir "resolver-assets.txt") -Encoding UTF8
    $missing = @($usedAssets | Where-Object { $assetEntries -notcontains $_ })
    $missingPath = Join-Path $outDir "resolver-assets-missing-from-zip.txt"
    if ($missing.Count -eq 0) {
        "" | Set-Content -LiteralPath $missingPath -Encoding UTF8
    } else {
        $missing | Set-Content -LiteralPath $missingPath -Encoding UTF8
    }
    $report.Add("- Resolver referenced assets: $($usedAssets.Count) -> resolver-assets.txt")
    $report.Add("- Resolver assets missing from Assets.zip: $($missing.Count) -> resolver-assets-missing-from-zip.txt")
}

$report.Add("")
$report.Add("PASS")
$reportPath = Join-Path $outDir "report.md"
$report | Set-Content -LiteralPath $reportPath -Encoding UTF8
Write-Host "[discover-hytale-assets] PASS report: $reportPath"
