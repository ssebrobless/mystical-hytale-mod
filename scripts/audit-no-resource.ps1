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

function Read-Json([string]$Path) {
    if (-not (Test-Path $Path)) {
        Fail "Missing JSON file: $Path"
        return $null
    }
    return Get-Content $Path -Raw | ConvertFrom-Json
}

function Get-JsonPropertyCount($Object) {
    if ($null -eq $Object) {
        return 0
    }
    return @($Object.PSObject.Properties).Count
}

$expectedClasses = @("terra", "hydro", "aero", "corruptus")
$stylesDir = Join-Path $ProjectRoot "src/main/resources/data/styles"
$classesDir = Join-Path $ProjectRoot "src/main/resources/data/classes"
$abilityCostsPath = Join-Path $ProjectRoot "src/main/resources/data/resources/ability_costs.json"

$abilityCosts = Read-Json $abilityCostsPath
Assert ($null -ne $abilityCosts -and $abilityCosts.ability_resource_costs_enabled -eq $false) "Global ability resource costs are disabled"
Assert ($null -ne $abilityCosts -and (Get-JsonPropertyCount $abilityCosts.resource_config) -eq 0) "Legacy resource_config is empty"
Assert ($null -ne $abilityCosts -and (Get-JsonPropertyCount $abilityCosts.style_resource_map) -eq 0) "Legacy style_resource_map is empty"

$totalStyles = 0
$totalAbilities = 0
foreach ($classId in $expectedClasses) {
    $stylePath = Join-Path $stylesDir ($classId + "_styles.json")
    $styleData = Read-Json $stylePath
    if ($null -eq $styleData) {
        continue
    }

    $styles = @($styleData.styles | Where-Object { $_.class_id -eq $classId })
    Assert ($styles.Count -eq 10) "$classId has 10 styles"
    foreach ($style in $styles) {
        $totalStyles++
        Assert ([string]::IsNullOrWhiteSpace([string]$style.resource_type)) "$classId/$($style.id) resource_type is blank"
        $abilities = @($style.abilities)
        Assert ($abilities.Count -eq 3) "$classId/$($style.id) has 3 abilities"
        foreach ($ability in $abilities) {
            $totalAbilities++
            Assert ([int]$ability.resource_cost -eq 0) "$classId/$($style.id)/$($ability.id) resource_cost is 0"
        }
    }
}

Assert ($totalStyles -eq 40) "All 40 styles checked"
Assert ($totalAbilities -eq 120) "All 120 abilities checked"

$forbiddenPassiveEffects = @(
    "resource_generation",
    "resource_cast_pool",
    "conditional_cost_reduction"
)
$forbiddenPassiveConditions = @(
    "current_water_percent",
    "current_soul",
    "current_souls",
    "souls"
)

foreach ($classId in $expectedClasses) {
    $classPath = Join-Path $classesDir ($classId + ".json")
    $classData = Read-Json $classPath
    if ($null -eq $classData -or $null -eq $classData.passive_ability) {
        continue
    }

    $effects = @($classData.passive_ability.effects)
    foreach ($effect in $effects) {
        $type = ([string]$effect.type).ToLowerInvariant()
        if ($forbiddenPassiveEffects -contains $type) {
            Fail "$classId passive still uses resource-gated effect type '$type'"
        }
        $condition = ([string]$effect.condition).ToLowerInvariant()
        foreach ($forbidden in $forbiddenPassiveConditions) {
            if ($condition.Contains($forbidden)) {
                Fail "$classId passive condition still references resource state: '$condition'"
            }
        }
    }
}
if (-not $script:Failed) {
    Pass "Class passives contain no ability-resource spending hooks"
}

$styleManagerPath = Join-Path $ProjectRoot "src/main/java/com/motm/manager/StyleManager.java"
$resourceManagerPath = Join-Path $ProjectRoot "src/main/java/com/motm/manager/ResourceManager.java"
$hudPath = Join-Path $ProjectRoot "src/main/java/com/motm/ui/MotmStatusHud.java"
$bookPath = Join-Path $ProjectRoot "src/main/java/com/motm/manager/BookInteractionManager.java"
$hydroRefillPath = Join-Path $ProjectRoot "src/main/java/com/motm/resource/HydroContainerRefillHandler.java"

$styleManager = Get-Content $styleManagerPath -Raw
$resourceManager = Get-Content $resourceManagerPath -Raw
$hud = Get-Content $hudPath -Raw
$book = Get-Content $bookPath -Raw
$hydroRefill = Get-Content $hydroRefillPath -Raw

Assert ($styleManager -match 'areAbilityResourceCostsEnabled\(\)[\s\S]*resourceManager\.spend') "StyleManager spends resources only behind the global flag"
Assert ($styleManager -match 'areAbilityResourceCostsEnabled\(\)[\s\S]*Not enough') "StyleManager blocks for missing resources only behind the global flag"
Assert ($resourceManager -match 'getResourceTypesForClass[\s\S]*areAbilityResourceCostsEnabled\(\)[\s\S]*Collections\.emptyList') "ResourceManager returns no resource types when disabled"
Assert ($resourceManager -match 'createDefaultResources[\s\S]*areAbilityResourceCostsEnabled\(\)[\s\S]*return resources') "ResourceManager initializes empty resource state when disabled"
Assert ($resourceManager -match 'refillWater[\s\S]*areAbilityResourceCostsEnabled\(\)[\s\S]*return') "Hydro water refill is inert when resources are disabled"
Assert ($hud -match 'buildResourceSnapshot[\s\S]*areAbilityResourceCostsEnabled\(\)[\s\S]*ResourceSnapshot\.hidden') "HUD resource root is hidden by the global disabled flag"
Assert ($book -match 'getResourceOptionLabels[\s\S]*areAbilityResourceCostsEnabled\(\)[\s\S]*Collections\.emptyList') "Dev book resource tools are hidden when resources are disabled"
Assert ($hydroRefill -match 'tryHandle[\s\S]*areAbilityResourceCostsEnabled\(\)[\s\S]*return false') "Hydro refill interaction is bypassed when resources are disabled"

if ($script:Failed) {
    Write-Host "No-resource audit: FAIL" -ForegroundColor Red
    exit 1
}

Write-Host "No-resource audit: PASS"
