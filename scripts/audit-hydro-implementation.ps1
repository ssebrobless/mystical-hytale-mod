param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

function Fail([string]$Message) {
    Write-Host "FAIL: $Message" -ForegroundColor Red
    $script:Failed = $true
}

function Pass([string]$Message) {
    Write-Host "PASS: $Message" -ForegroundColor Green
}

function Assert([bool]$Condition, [string]$Message) {
    if ($Condition) { Pass $Message } else { Fail $Message }
}

function Get-Ability([hashtable]$Abilities, [string]$Id) {
    if (-not $Abilities.ContainsKey($Id)) {
        Fail "Hydro ability '$Id' is missing"
        return $null
    }
    return $Abilities[$Id]
}

function Get-JavaSurface([string]$Root) {
    $paths = @(
        "src/main/java/com/motm/manager/GameplayPlaybackManager.java",
        "src/main/java/com/motm/manager/ClassPassiveManager.java",
        "src/main/java/com/motm/util/HytaleAssetResolver.java"
    )
    $runtimeRoot = Join-Path $Root "src/main/java/com/motm/runtime"
    $content = New-Object System.Collections.Generic.List[string]
    foreach ($path in $paths) {
        $fullPath = Join-Path $Root $path
        if (Test-Path $fullPath) {
            $content.Add((Get-Content $fullPath -Raw))
        }
    }
    if (Test-Path $runtimeRoot) {
        Get-ChildItem $runtimeRoot -Recurse -Filter *.java | Sort-Object FullName | ForEach-Object {
            $content.Add((Get-Content $_.FullName -Raw))
        }
    }
    return ($content -join "`n")
}

$stylePath = Join-Path $ProjectRoot "src/main/resources/data/styles/hydro_styles.json"
$classPath = Join-Path $ProjectRoot "src/main/resources/data/classes/hydro.json"
$playbackPath = Join-Path $ProjectRoot "src/main/java/com/motm/manager/GameplayPlaybackManager.java"
$resolverPath = Join-Path $ProjectRoot "src/main/java/com/motm/util/HytaleAssetResolver.java"
$passivePath = Join-Path $ProjectRoot "src/main/java/com/motm/manager/ClassPassiveManager.java"
$aquaBarrierEffectPath = Join-Path $ProjectRoot "src/main/resources/Server/Entity/Effects/MOTM/MOTM_Hydro_Aqua_Barrier.json"
$fieldSpecsPath = Join-Path $ProjectRoot "src/main/java/com/motm/runtime/ability/field/FieldRuntimeSpecs.java"
$fieldActivationPath = Join-Path $ProjectRoot "src/main/java/com/motm/runtime/ability/field/FieldActivationHytaleAdapter.java"
$fieldPulsePath = Join-Path $ProjectRoot "src/main/java/com/motm/runtime/ability/field/FieldPulseHytaleAdapter.java"
$terrainPlacementPath = Join-Path $ProjectRoot "src/main/java/com/motm/runtime/ability/terrain/TerrainPlacementHytaleAdapter.java"

Assert (Test-Path $stylePath) "hydro_styles.json exists"
Assert (Test-Path $classPath) "hydro.json exists"
Assert (Test-Path $playbackPath) "GameplayPlaybackManager.java exists"
Assert (Test-Path $resolverPath) "HytaleAssetResolver.java exists"
Assert (Test-Path $passivePath) "ClassPassiveManager.java exists"
Assert (Test-Path $aquaBarrierEffectPath) "MOTM_Hydro_Aqua_Barrier.json exists"
Assert (Test-Path $fieldSpecsPath) "FieldRuntimeSpecs.java exists"
Assert (Test-Path $fieldActivationPath) "FieldActivationHytaleAdapter.java exists"
Assert (Test-Path $fieldPulsePath) "FieldPulseHytaleAdapter.java exists"
Assert (Test-Path $terrainPlacementPath) "TerrainPlacementHytaleAdapter.java exists"

$data = Get-Content $stylePath -Raw | ConvertFrom-Json
$classData = Get-Content $classPath -Raw | ConvertFrom-Json
$playback = Get-JavaSurface $ProjectRoot
$resolver = Get-Content $resolverPath -Raw
$passive = Get-Content $passivePath -Raw
$aquaBarrierEffect = Get-Content $aquaBarrierEffectPath -Raw
$fieldSpecs = Get-Content $fieldSpecsPath -Raw
$fieldActivation = Get-Content $fieldActivationPath -Raw
$fieldPulse = Get-Content $fieldPulsePath -Raw
$terrainPlacement = Get-Content $terrainPlacementPath -Raw

$hydroStyles = @($data.styles | Where-Object { $_.class_id -eq "hydro" })
$abilities = @{}
foreach ($style in $hydroStyles) {
    foreach ($ability in $style.abilities) {
        $abilities[$ability.id] = $ability
    }
}

$expected = @(
    "frozen_needles", "stalactite_crash", "skate",
    "snow_imp", "snowstorm", "frosty",
    "high_tide", "waverider", "riptide",
    "piercing_rain", "rainbow", "splash",
    "scald", "geyser", "overheat",
    "vapor_vanish", "dispersion", "hidrosis",
    "ice_cap", "glacier", "ice_shelf",
    "tide_pool", "abyssal_assist", "rip_current",
    "leap_frog", "river_rapids", "swamp_monster",
    "bilge_dump", "anchor_haul", "oil_spill"
)

Assert ($hydroStyles.Count -eq 10) "Hydro has 10 styles"
Assert ($abilities.Count -eq 30) "Hydro has 30 abilities"
foreach ($id in $expected) {
    Assert ($abilities.ContainsKey($id)) "Hydro ability present: $id"
}
foreach ($entry in $abilities.GetEnumerator()) {
    Assert ($entry.Value.resource_cost -eq 0) "$($entry.Key) resource_cost is 0"
}

$passiveEffects = @{}
foreach ($effect in $classData.passive_ability.effects) {
    $passiveEffects[$effect.type] = [double]$effect.value
}
Assert ($passiveEffects["spell_vamp"] -eq 0.03) "Hydro passive spell-vamp is 3 percent"
Assert ($passiveEffects["swim_speed_bonus"] -eq 0.40) "Hydro swim speed passive is 40 percent"
Assert ($passiveEffects["oxygen_capacity_bonus"] -eq 0.50) "Hydro breathing passive is 50 percent"
Assert ($passiveEffects["aqua_barrier_shield"] -eq 0.10) "Aqua Barrier shield is 10 percent max HP"
Assert ($passiveEffects["aqua_barrier_cooldown_seconds"] -eq 8.0) "Aqua Barrier cooldown is 8 seconds"
Assert ($passive -match 'HYDRO_AQUA_BARRIER_EFFECT_ID') "Hydro Aqua Barrier runtime effect is wired"
Assert ($aquaBarrierEffect -notmatch 'EntityTopTint|EntityBottomTint') "Aqua Barrier does not tint the player or held item"
Assert ($aquaBarrierEffect -match 'FirstPersonParticles') "Aqua Barrier declares first-person-safe particles"
Assert ($aquaBarrierEffect -match 'FirstPersonParticles[\s\S]*Water_Bubble_Stream') "Aqua Barrier first-person cue is non-obstructive bubbles"
Assert ($passive -match 'devPassiveSuppressedUntilMillisByPlayer[\s\S]*suppressHydroAquaBarrierForDevCleanup[\s\S]*clearHydroPassiveRuntime[\s\S]*hydroBarrierReadyTickByPlayer\.put') "Dev cleanup suppresses immediate Aqua Barrier reapply"
Assert ($passive -match 'devPassiveSuppressedUntilMillisByPlayer\.get[\s\S]*System\.currentTimeMillis\(\)[\s\S]*clearHydroPassiveRuntime[\s\S]*continue;') "Passive tick honors wall-clock dev cleanup suppression before reapplying Aqua Barrier"
Assert ($fieldSpecs -match 'isCasterCentered[\s\S]*snowstorm[\s\S]*piercing_rain[\s\S]*healing_rainbow[\s\S]*tide_pool[\s\S]*oil_spill') "Hydro self fields are caster-centered instead of cursor-targeted"
Assert ($fieldSpecs -match 'shouldFollowOwner[\s\S]*snowstorm[\s\S]*piercing_rain[\s\S]*healing_rainbow') "Hydro moving aura fields follow the caster"
Assert ($fieldActivation -match 'FieldRuntimeSpecs\.shouldFollowOwner\(ability\)') "Field activation passes follow-owner routing to runtime"
Assert ($fieldSpecs -match '"self_buff"\.equals\(castType\)[\s\S]*terrainSpec\(ability\)\.kind\(\) != FieldTerrainRuntimeKind\.NONE') "Self-buff field terrain such as Ice Cap can activate persistent terrain"
Assert ($fieldSpecs -match 'FieldTerrainRuntimeKind\.GLACIER_WALL') "Glacier wall terrain kind is routed"
Assert ($fieldSpecs -match 'FieldTerrainRuntimeKind\.ICE_SHELF_WALL') "Ice Shelf wall terrain kind is routed"
Assert ($terrainPlacement -match 'case GLACIER_WALL, ICE_SHELF_WALL -> placeWallSelection') "Hydro ice walls place temporary wall selections"
Assert ($fieldPulse -match 'terrainEffect\.contains\("tide_pool"\)[\s\S]*applyTargetToken\("vulnerability"') "Tide Pool applies enemy vulnerability inside the pool"

$stalactite = Get-Ability $abilities "stalactite_crash"
Assert ($stalactite.travel_type -eq "calcite_stalactite") "Stalactite Crash uses calcite stalactite travel type"
Assert ($resolver -match 'Rock_Calcite_Stalactite_Large') "Resolver maps calcite stalactite model"
Assert ($playback -match '"stalactite_crash"\.equals\(abilityId\)') "Runtime resolver has Stalactite Crash visual route"
Assert ($playback -match 'MODEL_CALCITE_STALACTITE') "Runtime can spawn calcite stalactite visual"

$skate = Get-Ability $abilities "skate"
Assert ($skate.toggleable -eq $true) "Skate is toggleable"
Assert ($skate.terrain_effect -eq "ice_skate_trail") "Skate leaves an ice trail"
Assert ($playback -match 'terrainEffect\.contains\("ice_skate_trail"\)') "Skate trail terrain runtime is wired"

$snowImp = Get-Ability $abilities "snow_imp"
Assert ($snowImp.summon_name -eq "snowman_imp") "Snow Imp uses snowman summon name"
Assert ($resolver -match 'WinterHoliday_Snowman') "Resolver maps WinterHoliday snowman model"

$frosty = Get-Ability $abilities "frosty"
Assert ($frosty.summon_name -eq "yeti_frosty") "Frosty uses Yeti summon name"
Assert ($resolver -match 'MODEL_YETI') "Resolver maps Yeti model"

$snowstorm = Get-Ability $abilities "snowstorm"
Assert ([double]$snowstorm.radius -eq 5.0) "Snowstorm radius is 5"
Assert ([double]$snowstorm.height -eq 4.0) "Snowstorm height is 4"
Assert ($snowstorm.target_type -eq "self") "Snowstorm is caster-centered"

$piercing = Get-Ability $abilities "piercing_rain"
Assert ([double]$piercing.width -eq 5.0) "Piercing Rain cloud width is 5"
Assert ([double]$piercing.height -eq 12.0) "Piercing Rain max height is 12"
Assert ($piercing.target_type -eq "self") "Piercing Rain is caster-centered"
Assert ($playback -match 'terrainEffect\.contains\("piercing_rain"\)') "Piercing Rain field pulse is wired"

$rainbow = Get-Ability $abilities "rainbow"
Assert ([double]$rainbow.heal_percent -eq 5.0) "Rainbow heal is 5 percent per pulse"
Assert ($playback -match 'terrainEffect\.contains\("rainbow"\)[\s\S]*?support\.applyShield') "Rainbow runtime applies support zone sustain"

$overheat = Get-Ability $abilities "overheat"
Assert ($overheat.effect -notmatch 'self_burn|self_damage') "Overheat has no self-burn/self-damage"

$dispersion = Get-Ability $abilities "dispersion"
Assert ([double]$dispersion.damage_percent -eq 0.0) "Dispersion deals no damage"
Assert ([double]$dispersion.charges -eq 4.0) "Dispersion has 4 charges"

$iceCap = Get-Ability $abilities "ice_cap"
Assert ($iceCap.terrain_effect -eq "ice_cap_tube") "Ice Cap uses tube terrain effect"
Assert ([double]$iceCap.duration_seconds -eq 5.0) "Ice Cap lasts 5 seconds"
Assert ($playback -match 'ice_cap_tube') "Runtime recognizes Ice Cap tube terrain"

$glacier = Get-Ability $abilities "glacier"
Assert ([double]$glacier.duration_seconds -eq 5.0) "Glacier lasts 5 seconds"
Assert ($playback -match 'terrainEffect\.contains\("glacier"\)') "Runtime applies Glacier field effects"

$iceShelf = Get-Ability $abilities "ice_shelf"
Assert ($iceShelf.cast_type -eq "barrier") "Ice Shelf starts as a barrier wall"
Assert ([double]$iceShelf.width -eq 2.0) "Ice Shelf width is 2"
Assert ([double]$iceShelf.height -eq 3.0) "Ice Shelf height is 3"

$tidePool = Get-Ability $abilities "tide_pool"
Assert ($tidePool.target_type -eq "self") "Tide Pool is caster-centered"
Assert ([double]$tidePool.radius -eq 5.0) "Tide Pool radius is 5"
Assert ([double]$tidePool.height -eq 2.0) "Tide Pool is 2 blocks high"
Assert ($playback -match 'terrainEffect\.contains\("tide_pool"\)') "Runtime applies Tide Pool support/target effects"

$abyssal = Get-Ability $abilities "abyssal_assist"
Assert ($abyssal.summon_name -eq "snapjaw_abyssal") "Abyssal Assist summons Snapjaw"
Assert ($resolver -match 'MODEL_SNAPJAW') "Resolver maps Snapjaw model"

$rip = Get-Ability $abilities "rip_current"
Assert ([double]$rip.damage_percent -eq 0.0) "Rip Current deals no damage"
Assert ([double]$rip.duration_seconds -eq 5.0) "Rip Current tether lasts 5 seconds"

$leap = Get-Ability $abilities "leap_frog"
Assert ([double]$leap.charges -eq 4.0) "Leap Frog has 4 charges"
Assert ([double]$leap.damage_percent -eq 0.0) "Leap Frog deals no damage"

$rapids = Get-Ability $abilities "river_rapids"
Assert ($rapids.toggleable -eq $true) "River Rapids is toggleable"
Assert ([double]$rapids.duration_seconds -eq 7.0) "River Rapids lasts 7 seconds"
Assert ($rapids.cast_type -eq "dash_buff") "River Rapids propels on activation"

$swamp = Get-Ability $abilities "swamp_monster"
Assert ($swamp.summon_name -eq "crocodile_swamp_monster") "Swamp Monster summons Crocodile"

$bilge = Get-Ability $abilities "bilge_dump"
Assert ($bilge.effect -match 'toxic') "Bilge Dump applies Toxic"
Assert ([double]$bilge.dot_percent_per_second -eq 1.0) "Bilge Dump DoT is 1 percent per second"

$anchor = Get-Ability $abilities "anchor_haul"
Assert ([double]$anchor.range -eq 5.0) "Anchor Haul range is 5"
Assert ($anchor.travel_type -eq "anchor_chain") "Anchor Haul uses chain travel type"
Assert ($resolver -match 'Deco_Iron_Chains_Vertical') "Resolver maps iron chain model"
Assert (($playback -match '"anchor_haul"\.equals\(abilityId\)') -and ($playback -match 'damage \* 1\.10')) "Anchor Haul has Toxic follow-up damage bonus"

$oil = Get-Ability $abilities "oil_spill"
Assert ($oil.terrain_effect -eq "oil_spill_tar") "Oil Spill uses tar pool terrain effect"
Assert ([double]$oil.radius -eq 4.0) "Oil Spill radius is 4"
Assert ($playback -match 'terrainEffect\.contains\("oil_spill"\)') "Runtime recognizes Oil Spill tar terrain"
Assert ($playback -match 'terrainEffect\.contains\("oil_spill"\)[\s\S]*?applyTargetToken\("slow"[\s\S]*?applyTargetToken\("toxic"') "Oil Spill slows and Toxic-marks enemies"

Assert ($playback -match 'snowman_imp') "Runtime understands snowman_imp summon"
Assert ($playback -match 'yeti_frosty') "Runtime understands yeti_frosty summon"
Assert ($playback -match 'crocodile_swamp_monster') "Runtime understands crocodile_swamp_monster summon"
Assert ($playback -match 'snapjaw_abyssal') "Runtime understands snapjaw_abyssal summon"

if ($script:Failed) {
    Write-Host "Hydro implementation audit: FAIL" -ForegroundColor Red
    exit 1
}

Write-Host "Hydro implementation audit: PASS" -ForegroundColor Green
