param(
    [string]$OutputRoot,
    [switch]$NoTimestamp
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $OutputRoot) {
    $stamp = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
    if ($NoTimestamp) {
        $OutputRoot = Join-Path $repoRoot "audits\ability-matrix\latest"
    } else {
        $OutputRoot = Join-Path $repoRoot (Join-Path "audits\ability-matrix" $stamp)
    }
}
New-Item -ItemType Directory -Path $OutputRoot -Force | Out-Null

function Get-ScenarioKind($Ability) {
    $castType = ([string]$Ability.cast_type).ToLowerInvariant()
    $targetType = ([string]$Ability.target_type).ToLowerInvariant()
    $trigger = ([string]$Ability.trigger).ToLowerInvariant()
    $effect = ([string]$Ability.effect).ToLowerInvariant()
    $description = ([string]$Ability.description).ToLowerInvariant()

    if ($trigger -eq "jump_land") { return "jump_land" }
    if ($castType -in @("dash", "dash_buff", "dash_strike", "leap", "dive_strike", "teleport", "air_stall")) { return "movement" }
    if ($castType -in @("cone", "gaze")) { return "facing_cone" }
    if ($castType -in @("ground_zone", "support_zone")) { return "ground_zone" }
    if ($castType -in @("ground_target", "ground_strike", "barrier")) { return "ground_target" }
    if ($castType -like "projectile*" -or $castType -in @("line_control", "wave_line", "chain")) { return "projectile_line" }
    if ($castType -in @("summon", "summon_buff")) { return "summon" }
    if ($castType -in @("cleanse") -or $effect -match "cleanse|purify" -or $description -match "cleanse|purge|purify") { return "cleanse" }
    if ($targetType -eq "self" -or $castType -match "self|transformation|form|buff") { return "self_buff" }
    if ($effect -match "heal" -or $description -match "heal") { return "support_heal" }
    return "single_target"
}

function Get-ScenarioSetup([string]$Kind) {
    switch ($Kind) {
        "jump_land" { "Arm ability, jump and land within 3m of grounded target." }
        "movement" { "Face target, start with clear lane, use forward/jump movement if required." }
        "facing_cone" { "Face target in a narrow camera cone before cast." }
        "ground_zone" { "Clear mobs, spawn grounded target inside radius, wait through at least one field tick." }
        "ground_target" { "Aim at target block or target feet, wait any delay_seconds before proof." }
        "projectile_line" { "Place grounded target in visible lane and keep aim steady through impact window." }
        "summon" { "Clear arena before cast, then wait for summon appearance/action window." }
        "cleanse" { "Pre-apply debuff/damage condition where available; otherwise report setup as incomplete." }
        "self_buff" { "Use third-person camera and capture caster body/HUD/status after cast." }
        "support_heal" { "Use damaged or buffable caster/ally; capture HP/status proof." }
        default { "Spawn grounded target in range and face it before cast." }
    }
}

function Get-ScenarioProof([string]$Kind) {
    switch ($Kind) {
        "jump_land" { "Stomp/landing resolution log with targets>=1 plus impact screenshot." }
        "movement" { "Before/after displacement or target-side hit/effect after movement." }
        "facing_cone" { "Target-side effect or hit; no 'No valid target' accepted." }
        "ground_zone" { "Field active/pulse line, target-side damage/status, screenshot of persistent area." }
        "ground_target" { "Telegraph/delay if present, then hit/status/impact visual." }
        "projectile_line" { "Launch line and target-side hit/effect/impact evidence." }
        "summon" { "Summon appears and survives or acts; no unmapped/nonexistent role warnings." }
        "cleanse" { "Debuff setup exists, then clear/remove log after cast." }
        "self_buff" { "Buff/shield/heal/status log plus third-person visual screenshot." }
        "support_heal" { "HP/stat/status improvement in log or HUD screenshot." }
        default { "Cast result plus target-side hit/effect evidence." }
    }
}

$rows = New-Object System.Collections.Generic.List[object]
Get-ChildItem -LiteralPath (Join-Path $repoRoot "src\main\resources\data\styles") -Filter "*_styles.json" |
    Sort-Object Name |
    ForEach-Object {
        $classId = $_.BaseName -replace "_styles$", ""
        $data = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
        foreach ($style in @($data.styles)) {
            foreach ($ability in @($style.abilities)) {
                $kind = Get-ScenarioKind $ability
                $rows.Add([pscustomobject]@{
                    class_id = $classId
                    style_id = [string]$style.id
                    style_name = [string]$style.name
                    style_theme = [string]$style.theme
                    ability_id = [string]$ability.id
                    ability_name = [string]$ability.name
                    description = [string]$ability.description
                    cast_type = [string]$ability.cast_type
                    target_type = [string]$ability.target_type
                    trigger = [string]$ability.trigger
                    scenario = $kind
                    required_setup = Get-ScenarioSetup $kind
                    required_proof = Get-ScenarioProof $kind
                }) | Out-Null
            }
        }
    }

$md = New-Object System.Collections.Generic.List[string]
$md.Add("# MOTM Ability Scenario Matrix")
$md.Add("")
$md.Add("- Generated: $(Get-Date -Format o)")
$md.Add("- Ability count: $($rows.Count)")
$md.Add("")
$md.Add("| Class | Style | Ability | Scenario | Setup | Proof |")
$md.Add("| --- | --- | --- | --- | --- | --- |")
foreach ($row in $rows) {
    $md.Add("| $($row.class_id) | $($row.style_id) | $($row.ability_id) | $($row.scenario) | $($row.required_setup) | $($row.required_proof) |")
}

$jsonPath = Join-Path $OutputRoot "ability-scenarios.json"
$mdPath = Join-Path $OutputRoot "ability-scenarios.md"
$rows | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8
$md | Set-Content -LiteralPath $mdPath -Encoding UTF8

Write-Host "[generate-ability-matrix] Wrote $mdPath"
Write-Host "[generate-ability-matrix] Wrote $jsonPath"
