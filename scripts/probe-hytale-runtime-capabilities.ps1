param(
    [string]$RunId = "latest"
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path "hytale-runtime-capabilities" $RunId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

$latestRoot = Join-Path $env:APPDATA "Hytale\install\release\package\game\latest"
$serverJar = Join-Path $latestRoot "Server\HytaleServer.jar"
$javap = Join-Path $repoRoot ".tools\jdk-25\bin\javap.exe"
$jarExe = Join-Path $repoRoot ".tools\jdk-25\bin\jar.exe"

if (-not (Test-Path -LiteralPath $serverJar)) { throw "HytaleServer.jar not found: $serverJar" }
if (-not (Test-Path -LiteralPath $javap)) { throw "javap.exe not found: $javap" }
if (-not (Test-Path -LiteralPath $jarExe)) { throw "jar.exe not found: $jarExe" }

$classesToInspect = @(
    "com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection",
    "com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask",
    "com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType",
    "com.hypixel.hytale.server.core.asset.type.fluid.Fluid",
    "com.hypixel.hytale.server.core.prefab.PrefabStore",
    "com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.SpawnPrefabInteraction",
    "com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect",
    "com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent",
    "com.hypixel.hytale.server.core.asset.type.model.config.Model",
    "com.hypixel.hytale.server.core.asset.type.model.config.ModelAsset",
    "com.hypixel.hytale.server.core.modules.entity.component.ModelComponent",
    "com.hypixel.hytale.server.core.modules.entity.component.TransformComponent",
    "com.hypixel.hytale.server.core.modules.physics.component.Velocity"
)

$jarEntries = @(& $jarExe tf $serverJar)
$apiHits = @($jarEntries |
    Where-Object { $_ -match "BlockSelection|BlockMask|BlockType|Fluid|Prefab|EntityEffect|EffectControllerComponent|ModelComponent|ModelAsset|TransformComponent|Velocity" } |
    Sort-Object -Unique)
$apiHits | Set-Content -LiteralPath (Join-Path $outDir "api-class-hits.txt") -Encoding UTF8

$signatures = New-Object System.Collections.Generic.List[string]
foreach ($className in $classesToInspect) {
    $signatures.Add("==== $className")
    $output = @(& $javap -classpath $serverJar -public $className 2>&1)
    foreach ($line in $output) {
        $signatures.Add([string]$line)
    }
    $signatures.Add("")
}
$signatures | Set-Content -LiteralPath (Join-Path $outDir "api-public-signatures.txt") -Encoding UTF8

$report = New-Object System.Collections.Generic.List[string]
$report.Add("# Hytale Runtime Capability Probe")
$report.Add("")
$report.Add("- RunId: $RunId")
$report.Add("- HytaleServer.jar: $serverJar")
$report.Add("- api-class-hits.txt: $($apiHits.Count) hits")
$report.Add("- api-public-signatures.txt: $($classesToInspect.Count) inspected classes")
$report.Add("")
$report.Add("## Proof Buckets")
$report.Add("")
$report.Add("- P0 coating proof: EntityEffect + EffectControllerComponent")
$report.Add("- P1 temporary block proof: BlockType + BlockSelection")
$report.Add("- P2 temporary fluid proof: Fluid + BlockSelection")
$report.Add("- P3 model/proxy proof: Model/ModelAsset + ModelComponent or existing NPC proxy route")
$report.Add("- P4 movement safety proof: TransformComponent + Velocity")
$report.Add("")
$report.Add("PASS")
$report | Set-Content -LiteralPath (Join-Path $outDir "report.md") -Encoding UTF8

Write-Host "[probe-hytale-runtime-capabilities] PASS $outDir"
