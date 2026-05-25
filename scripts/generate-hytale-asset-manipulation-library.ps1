param(
    [string]$RunId = "latest"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path "hytale-asset-library" $RunId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

$latestRoot = Join-Path $env:APPDATA "Hytale\install\release\package\game\latest"
$assetsZip = Join-Path $latestRoot "Assets.zip"
$serverJar = Join-Path $latestRoot "Server\HytaleServer.jar"
$javap = Join-Path $repoRoot ".tools\jdk-25\bin\javap.exe"
$jarExe = Join-Path $repoRoot ".tools\jdk-25\bin\jar.exe"

if (-not (Test-Path -LiteralPath $assetsZip)) { throw "Assets.zip not found: $assetsZip" }
if (-not (Test-Path -LiteralPath $serverJar)) { throw "HytaleServer.jar not found: $serverJar" }
if (-not (Test-Path -LiteralPath $javap)) { throw "javap.exe not found: $javap" }
if (-not (Test-Path -LiteralPath $jarExe)) { throw "jar.exe not found: $jarExe" }

Add-Type -AssemblyName System.IO.Compression.FileSystem

function Get-ZipEntries([string]$Path) {
    $zip = [System.IO.Compression.ZipFile]::OpenRead($Path)
    try {
        return @($zip.Entries | ForEach-Object { $_.FullName })
    } finally {
        $zip.Dispose()
    }
}

function Write-List([string]$Name, [string[]]$Rows) {
    $path = Join-Path $outDir $Name
    $Rows | Sort-Object -Unique | Set-Content -LiteralPath $path -Encoding UTF8
    return $path
}

$assetEntries = Get-ZipEntries $assetsZip
$jarEntries = & $jarExe tf $serverJar

$categories = [ordered]@{
    "all-assets.txt" = $assetEntries
    "particles.txt" = @($assetEntries | Where-Object { $_ -match '\.(particlespawner|particlesystem)$' })
    "models.txt" = @($assetEntries | Where-Object { $_ -match '\.blockymodel$' })
    "animations.txt" = @($assetEntries | Where-Object { $_ -match '\.blockyanim$' })
    "block-models.txt" = @($assetEntries | Where-Object { $_ -match '^Common/Blocks/.+\.blockymodel$' })
    "block-items.txt" = @($assetEntries | Where-Object { $_ -match '^Server/Item/(Block|Items)/.+\.json$' })
    "prefabs.txt" = @($assetEntries | Where-Object { $_ -match '^Server/Prefabs/.+\.prefab\.json$' })
    "entity-effects.txt" = @($assetEntries | Where-Object { $_ -match '^Server/Entity/Effects/.+\.json$' })
    "ui.txt" = @($assetEntries | Where-Object { $_ -match '\.ui$' })
}

$report = New-Object System.Collections.Generic.List[string]
$report.Add("# Hytale Asset Manipulation Library Discovery")
$report.Add("")
$report.Add("- RunId: $RunId")
$report.Add("- Assets.zip: $assetsZip")
$report.Add("- HytaleServer.jar: $serverJar")
$report.Add("- Total asset entries: $($assetEntries.Count)")
$report.Add("- Total jar entries: $($jarEntries.Count)")
$report.Add("")
$report.Add("## Category Indexes")
$report.Add("")
foreach ($entry in $categories.GetEnumerator()) {
    Write-List $entry.Key @($entry.Value) | Out-Null
    $report.Add("- $($entry.Key): $(@($entry.Value).Count)")
}

$keywords = @(
    "Metal_Iron", "Metal", "Iron",
    "Lava", "Fluid_Lava", "Magma_Cooled", "Volcanic",
    "Stone", "Rock_Stone", "Rubble", "Pillar",
    "Root", "Vine", "Sapling", "Flower", "Cactus",
    "Poison", "Acid", "Smoke", "Crystal", "Gem", "Sand", "Soil",
    "Water", "Bubble", "Ice", "Snow", "Steam", "Wave", "Rain",
    "Wind", "Lightning", "Thunder", "Void", "Shadow", "Fire",
    "Bone", "Soul", "Scarak", "Pterodactyl", "Rex", "Frog"
)

$keywordMd = New-Object System.Collections.Generic.List[string]
$keywordMd.Add("# Keyword Asset Catalog")
$keywordMd.Add("")
foreach ($keyword in $keywords) {
    $pattern = [regex]::Escape($keyword)
    $hits = @($assetEntries |
        Where-Object { $_ -match $pattern } |
        Where-Object { $_ -match '\.(json|blockymodel|particlespawner|particlesystem|prefab\.json|png)$' } |
        Sort-Object -Unique |
        Select-Object -First 120)
    $keywordMd.Add("## $keyword")
    $keywordMd.Add("")
    if ($hits.Count -eq 0) {
        $keywordMd.Add("- none")
    } else {
        foreach ($hit in $hits) {
            $keywordMd.Add("- $hit")
        }
    }
    $keywordMd.Add("")
}
$keywordMd | Set-Content -LiteralPath (Join-Path $outDir "keyword-catalog.md") -Encoding UTF8
$report.Add("- keyword-catalog.md: $($keywords.Count) keyword sections")

$apiPatterns = "BlockPlaceUtils|BlockReplaceEvent|BlockState|BlockType|Prefab|EntityEffect|ApplicationEffects|ModelOverride|ModelComponent|EffectControllerComponent|Projectile|Velocity|TransformComponent|Tint|ItemStack|Particle"
$apiClasses = @($jarEntries | Where-Object { $_ -match $apiPatterns } | Sort-Object -Unique)
$apiClasses | Set-Content -LiteralPath (Join-Path $outDir "api-classes.txt") -Encoding UTF8
$report.Add("- api-classes.txt: $($apiClasses.Count)")

$apiDetails = New-Object System.Collections.Generic.List[string]
$classesToInspect = @(
    "com.hypixel.hytale.server.core.modules.interaction.BlockPlaceUtils",
    "com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.SpawnPrefabInteraction",
    "com.hypixel.hytale.server.core.prefab.PrefabStore",
    "com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect",
    "com.hypixel.hytale.server.core.asset.type.entityeffect.config.ApplicationEffects",
    "com.hypixel.hytale.server.core.asset.type.entityeffect.config.ModelOverride",
    "com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent",
    "com.hypixel.hytale.server.core.modules.entity.component.ModelComponent",
    "com.hypixel.hytale.server.core.modules.entity.component.TransformComponent",
    "com.hypixel.hytale.server.core.modules.physics.component.Velocity",
    "com.hypixel.hytale.protocol.Tint"
)
foreach ($className in $classesToInspect) {
    $apiDetails.Add("==== $className")
    foreach ($line in @(& $javap -classpath $serverJar -public $className 2>$null)) {
        $apiDetails.Add([string]$line)
    }
    $apiDetails.Add("")
}
$apiDetails | Set-Content -LiteralPath (Join-Path $outDir "api-public-signatures.txt") -Encoding UTF8
$report.Add("- api-public-signatures.txt: $($classesToInspect.Count) inspected classes")

$report.Add("")
$report.Add("PASS")
$report | Set-Content -LiteralPath (Join-Path $outDir "report.md") -Encoding UTF8
Write-Host "[generate-hytale-asset-manipulation-library] PASS $outDir"
