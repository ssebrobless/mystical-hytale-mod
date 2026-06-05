param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"
$script:Failed = $false

function Fail([string]$Message) {
    Write-Host "FAIL: $Message" -ForegroundColor Red
    $script:Failed = $true
}

function Pass([string]$Message) {
    Write-Host "PASS: $Message" -ForegroundColor Green
}

function Assert([bool]$Condition, [string]$Message) {
    if ($Condition) {
        Pass $Message
    } else {
        Fail $Message
    }
}

function Read-Text([string]$RelativePath) {
    $path = Join-Path $ProjectRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        Fail "Missing file: $RelativePath"
        return ""
    }
    return Get-Content -LiteralPath $path -Raw
}

function Read-Json([string]$RelativePath) {
    $path = Join-Path $ProjectRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        Fail "Missing JSON file: $RelativePath"
        return $null
    }
    return Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
}

$spellbookUi = Read-Text "src/main/resources/Common/UI/Custom/Pages/MOTM_Spellbook.ui"
$spellbookPage = Read-Text "src/main/java/com/motm/ui/SpellbookPage.java"

foreach ($term in @("Journey", "Codex", "Journal", "Grimoire", "Resources")) {
    Assert ($spellbookUi -notmatch $term) "Spellbook UI contains no stale '$term' selector or visible label"
    Assert ($spellbookPage -notmatch $term) "Spellbook page code contains no stale '$term' selector or visible text"
}

foreach ($classId in @("terra", "hydro", "aero", "corruptus")) {
    $classData = Read-Json "src/main/resources/data/classes/$classId.json"
    if ($null -eq $classData) {
        continue
    }
    Assert ($null -eq $classData.PSObject.Properties["lore"]) "$classId class data has no lore/story field"
}

$classModel = Read-Text "src/main/java/com/motm/model/ClassData.java"
Assert ($classModel -notmatch "\blore\b|getLore") "ClassData model exposes no lore/story field"

$abilityCosts = Read-Json "src/main/resources/data/resources/ability_costs.json"
Assert ($null -ne $abilityCosts -and $abilityCosts.ability_resource_costs_enabled -eq $false) "Ability resource costs remain globally disabled"

$stylesDir = Join-Path $ProjectRoot "src/main/resources/data/styles"
$badCosts = @()
foreach ($path in Get-ChildItem -LiteralPath $stylesDir -Filter "*.json") {
    $styleData = Get-Content -LiteralPath $path.FullName -Raw | ConvertFrom-Json
    foreach ($style in @($styleData.styles)) {
        if (-not [string]::IsNullOrWhiteSpace([string]$style.resource_type)) {
            $badCosts += "$($style.class_id)/$($style.id) has resource_type '$($style.resource_type)'"
        }
        foreach ($ability in @($style.abilities)) {
            if ([int]$ability.resource_cost -ne 0) {
                $badCosts += "$($style.class_id)/$($style.id)/$($ability.id) has resource_cost $($ability.resource_cost)"
            }
        }
    }
}
Assert ($badCosts.Count -eq 0) "All style abilities remain no-resource"
foreach ($bad in $badCosts) {
    Fail $bad
}

$levelingManager = Read-Text "src/main/java/com/motm/manager/LevelingManager.java"
$perkManager = Read-Text "src/main/java/com/motm/manager/PerkManager.java"
$xpConfig = Read-Json "src/main/resources/data/leveling/xp_config.json"

Assert ($levelingManager -match "PERK_CAP_LEVEL\s*=\s*100") "Perk unlock cap remains level 100"
Assert ($levelingManager -match "STAT_CAP_LEVEL\s*=\s*200") "Stat point cap remains level 200"
Assert ($levelingManager -match "STAT_POINTS_PER_LEVEL\s*=\s*2") "Players earn 2 stat points per level"
Assert ($perkManager -match "MAX_TOTAL_PERKS\s*=\s*10") "Player selected perk cap remains 10"
Assert ($null -ne $xpConfig -and $xpConfig.perk_milestones.perks_per_milestone -eq 1) "XP config grants 1 perk choice per milestone"
Assert ($null -ne $xpConfig -and $xpConfig.perk_milestones.total_perks_at_max_level -eq 10) "XP config caps selected perks at 10"

if ($script:Failed) {
    Write-Host "Canon drift audit: FAIL" -ForegroundColor Red
    exit 1
}

Write-Host "Canon drift audit: PASS"
