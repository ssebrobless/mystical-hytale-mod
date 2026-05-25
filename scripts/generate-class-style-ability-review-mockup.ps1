param(
    [string]$OutputPath
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $OutputPath) {
    $OutputPath = Join-Path $repoRoot "CODEX_CLASS_STYLE_ABILITY_REVIEW_MOCKUP_2026-05-22.md"
}

$conceptPath = Join-Path $repoRoot "audits\concept-source-discovery\latest\original-concept-ability-comparison.json"
$assetPath = Join-Path $repoRoot "audits\ability-asset-plan\latest\ability-asset-plan.json"
if (-not (Test-Path -LiteralPath $conceptPath)) {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "extract-original-concept-abilities.ps1") -NoTimestamp
}
if (-not (Test-Path -LiteralPath $assetPath)) {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "generate-ability-asset-plan.ps1") -NoTimestamp
}

$conceptRows = Get-Content -LiteralPath $conceptPath -Raw | ConvertFrom-Json
$assetRows = Get-Content -LiteralPath $assetPath -Raw | ConvertFrom-Json

function To-Slug([string]$value) {
    if ([string]::IsNullOrWhiteSpace($value)) { return "" }
    $s = $value.Trim().ToLowerInvariant()
    $s = $s -replace ":", ""
    $s = $s -replace "/", " "
    $s = $s -replace "[^a-z0-9]+", "_"
    $s = $s.Trim("_")
    if ($s -eq "t_rex_form") { return "t_rex_form" }
    if ($s -eq "triceratops_form_mosasaurus_form") { return "triceratops_form" }
    if ($s -eq "hidroses") { return "hidrosis" }
    return $s
}

function Join-Assets($values) {
    $items = @($values) | Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) }
    if ($items.Count -eq 0) { return "" }
    $names = foreach ($item in $items) {
        $leaf = Split-Path ([string]$item) -Leaf
        if ([string]::IsNullOrWhiteSpace($leaf)) { $item } else { $leaf }
    }
    return ($names -join ", ")
}

function Escape-Cell([string]$value) {
    if ($null -eq $value) { return "" }
    return ($value -replace "\|", "/" -replace "`r?`n", " ").Trim()
}

$assetIndex = @{}
foreach ($row in $assetRows) {
    $key = "$($row.class_id)|$($row.style_id)|$($row.ability_id)"
    $assetIndex[$key] = $row
}

$md = New-Object System.Collections.Generic.List[string]
$md.Add("# Class / Style / Ability Review Mockup - 2026-05-22")
$md.Add("")
$md.Add("Use this file to review my current understanding before implementation. The **Function** column comes from the recovered original Hytale concept. The **Hytale appearance/read** column maps that concept onto verified local Hytale assets and EntityEffect paths.")
$md.Add("")
$md.Add('```text')
$md.Add("Review Goal")
$md.Add("  +-- Is this what the ability should do?")
$md.Add("  +-- Is this how the ability should look/read in Hytale?")
$md.Add("  +-- Is anything missing, stale, or conceptually wrong?")
$md.Add('```')
$md.Add("")
$md.Add("## Source Priority Used For This Mockup")
$md.Add("")
$md.Add('```text')
$md.Add("1. Original Hytale concept: motm-hytale-extract/original-concept/MOD_DESIGN.md")
$md.Add("2. Current corrections from user conversation")
$md.Add("3. Verified Hytale asset plan: audits/ability-asset-plan/latest/ability-asset-plan.json")
$md.Add("4. Current protected style JSON only as current implementation data")
$md.Add('```')
$md.Add("")

$classOrder = @("Terra", "Hydro", "Aero", "Corruptus")
foreach ($className in $classOrder) {
    $md.Add("## $className")
    $md.Add("")
    $styles = $conceptRows |
        Where-Object { $_.class -eq $className } |
        Group-Object style
    foreach ($styleGroup in $styles) {
        $styleName = $styleGroup.Name
        $classId = To-Slug $className
        $styleId = To-Slug $styleName
        $firstAsset = $assetRows | Where-Object { $_.class_id -eq $classId -and $_.style_id -eq $styleId } | Select-Object -First 1

        $md.Add("### $styleName")
        $md.Add("")
        if ($firstAsset) {
            $md.Add('```text')
            $md.Add("Style read: $($firstAsset.style_feel)")
            $md.Add("Palette:    $($firstAsset.palette)")
            $md.Add("Bridge:     $($firstAsset.implementation_bridge)")
            $md.Add('```')
            $md.Add("")
        }
        $md.Add("| Slot | Ability | Function | Hytale appearance/read | Proof we need |")
        $md.Add("| ---: | --- | --- | --- | --- |")
        foreach ($concept in ($styleGroup.Group | Sort-Object slot)) {
            $abilityId = To-Slug $concept.current_ability
            $key = "$classId|$styleId|$abilityId"
            $asset = $assetIndex[$key]
            if (-not $asset) {
                $abilityId = To-Slug $concept.original_ability
                $key = "$classId|$styleId|$abilityId"
                $asset = $assetIndex[$key]
            }

            if ($asset) {
                $loopModel = @(
                    Join-Assets $asset.loop_assets
                    Join-Assets $asset.model_assets
                ) | Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) }
                $appearanceParts = @(
                    "Motion: $($asset.motion_shape)"
                    "Cast: $(Join-Assets $asset.cast_assets)"
                    "Travel: $(Join-Assets $asset.travel_assets)"
                    "Impact: $(Join-Assets $asset.impact_assets)"
                    "Loop/model: $($loopModel -join ', ')"
                ) | Where-Object { $_ -notmatch ":\s*$" }
                $appearance = ($appearanceParts -join " ")
                $proof = $asset.proof_requirement
            } else {
                $appearance = "MISSING ASSET PLAN ROW"
                $proof = "Resolve concept row before implementation."
            }

            $md.Add("| $($concept.slot) | $($concept.original_ability) | $(Escape-Cell $concept.original_description) | $(Escape-Cell $appearance) | $(Escape-Cell $proof) |")
        }
        $md.Add("")
    }
}

$md.Add("## Review Notes")
$md.Add("")
$md.Add("- `Hidroses` from the original Hydro/Vapor concept currently maps to `Hidrosis` in the active mod data.")
$md.Add("- `Triceratops Form / Mosasaurus Form` from the original Corruptus/Primordial concept currently maps to `Triceratops Form`; the water-form branch is not represented in the active ability name.")
$md.Add("- The current mod descriptions are compressed for all 120 abilities, so this mockup should be reviewed before using current JSON descriptions as final player-facing Spellbook text.")

$md | Set-Content -LiteralPath $OutputPath -Encoding UTF8
Write-Host "[generate-class-style-ability-review-mockup] Wrote $OutputPath"
