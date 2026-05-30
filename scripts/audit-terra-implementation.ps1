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
    if ($Condition) {
        Pass $Message
    } else {
        Fail $Message
    }
}

function Get-Ability([hashtable]$Abilities, [string]$Id) {
    if (-not $Abilities.ContainsKey($Id)) {
        Fail "Terra ability '$Id' is missing"
        return $null
    }
    return $Abilities[$Id]
}

function Get-JavaSurface([string]$Root) {
    $paths = @(
        "src/main/java/com/motm/manager/GameplayPlaybackManager.java",
        "src/main/java/com/motm/manager/StyleManager.java",
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

$stylePath = Join-Path $ProjectRoot "src/main/resources/data/styles/terra_styles.json"
$playbackPath = Join-Path $ProjectRoot "src/main/java/com/motm/manager/GameplayPlaybackManager.java"
$styleManagerPath = Join-Path $ProjectRoot "src/main/java/com/motm/manager/StyleManager.java"
$sinkholeMarkerPath = Join-Path $ProjectRoot "src/main/java/com/motm/runtime/ability/terrain/TerrainSinkholeMarkerHytaleAdapter.java"

Assert (Test-Path $stylePath) "terra_styles.json exists"
Assert (Test-Path $playbackPath) "GameplayPlaybackManager.java exists"
Assert (Test-Path $styleManagerPath) "StyleManager.java exists"

$data = Get-Content $stylePath -Raw | ConvertFrom-Json
$playback = Get-JavaSurface $ProjectRoot
$styleManager = Get-Content $styleManagerPath -Raw
$sinkholeMarker = ""
if (Test-Path $sinkholeMarkerPath) {
    $sinkholeMarker = Get-Content $sinkholeMarkerPath -Raw
}

$terraStyles = @($data.styles | Where-Object { $_.class_id -eq "terra" })
$abilities = @{}
foreach ($style in $terraStyles) {
    foreach ($ability in $style.abilities) {
        $abilities[$ability.id] = $ability
    }
}

$expected = @(
    "stomp", "aftershock", "sinkhole",
    "iron_wall", "metal_coat", "alloy_enhancement",
    "lava_pool", "obsidian_skin", "magma_sling",
    "rubble_rouser", "pillar_strike", "rockslide",
    "rooted", "vines", "sapling",
    "nightshade", "frolick", "cacti_cluster",
    "gargoyle", "glare", "tunnel",
    "burrow", "mudpit", "debris",
    "sandstorm", "dust_devil", "vitrification",
    "lapidary", "fracture", "refraction"
)

Assert ($terraStyles.Count -eq 10) "Terra has 10 styles"
Assert ($abilities.Count -eq 30) "Terra has 30 abilities"
foreach ($id in $expected) {
    Assert ($abilities.ContainsKey($id)) "Terra ability present: $id"
}

foreach ($entry in $abilities.GetEnumerator()) {
    Assert ($entry.Value.resource_cost -eq 0) "$($entry.Key) resource_cost is 0"
}

$aftershock = Get-Ability $abilities "aftershock"
Assert ([double]$aftershock.radius -eq 8.0) "Aftershock radius is 8"
Assert ([double]$aftershock.damage_percent -eq 5.0) "Aftershock damage is 5 percent max HP"

$ironWall = Get-Ability $abilities "iron_wall"
Assert ([double]$ironWall.width -eq 3.0) "Iron Wall data width is 3"
Assert ([double]$ironWall.height -eq 4.0) "Iron Wall data height is 4"
Assert ($playback -match 'placeIronWallSelection\(') "Iron Wall runtime uses dedicated placement"
Assert ($playback -match 'Vector3i anchor = surfaceDecorationAnchor\(center\);') "Iron Wall runtime is grounded to the surface"
Assert ($playback -match 'for \(int y = 0; y < 4; y\+\+\)') "Iron Wall runtime builds 4 blocks high"
Assert ($playback -match '12 grounded iron blocks') "Iron Wall runtime reports 3x4 grounded wall"

$pillar = Get-Ability $abilities "pillar_strike"
Assert ([double]$pillar.height -eq 4.0) "Pillar Strike data height is 4"
Assert ($playback -match 'FieldTerrainRuntimeKind\.STONE_PILLAR') "Pillar Strike routes through stone pillar terrain runtime"
Assert ($playback -match 'STONE_PILLAR[\s\S]*?4,[\s\S]*?List\.of\("Rock_Stone_Brick_Pillar_Middle", "Rock_Stone_Brick"\)') "Pillar Strike terrain spec is 4 staged stone blocks"
Assert ($playback -match 'STACKING_COLUMN_STAGE_INTERVAL_MS = 90L') "Pillar Strike stages rapidly"
Assert ($playback -match 'placeStackingColumnSelection') "Pillar Strike uses stacking column placement"

$vines = Get-Ability $abilities "vines"
Assert ([double]$vines.cooldown_seconds -eq 0.0) "Vines has no cooldown"
Assert ([double]$vines.duration_seconds -eq 5.0) "Vines roots for 5 seconds"

$sapling = Get-Ability $abilities "sapling"
Assert ($sapling.cast_type -eq "projectile_line") "Sapling fires as a ground-marking projectile line"
Assert ($sapling.target_type -eq "ground_target") "Sapling targets ground"
Assert ($sapling.summon_name -eq "") "Sapling no longer spawns a treant summon"
Assert ($sapling.effect -eq "lure") "Sapling is tagged as lure"

$sandstorm = Get-Ability $abilities "sandstorm"
Assert ([double]$sandstorm.duration_seconds -eq 10.0) "Sandstorm lasts 10 seconds"
Assert ($sandstorm.toggleable -eq $true) "Sandstorm is toggleable"
Assert ([double]$sandstorm.toggle_cooldown_seconds -eq 2.0) "Sandstorm has a 2s toggle cooldown"
Assert ([double]$sandstorm.radius -eq 5.0) "Sandstorm radius is 5"
Assert ($styleManager -match 'requiresActiveToggle\(ability,\s*"sandstorm"\)') "Dust Devil requires active Sandstorm"
Assert ($styleManager -match 'consumeActiveToggle\(playerId,\s*consumedToggle\)') "Dust Devil consumes active Sandstorm"
Assert ($playback -match 'case "sandstorm" -> \{[\s\S]*?sand particle aura[\s\S]*?\}') "Sandstorm uses particle aura path, not block placement"
Assert ($playback -notmatch 'case "sandstorm" ->[\s\S]*?placeRingBlockSelection[\s\S]*?"Soil_Sand"') "Sandstorm does not place sand/sandstone block rings"
Assert ($playback -notmatch 'dust_devil_sand') "Dust Devil does not place supplemental sand/sandstone block rings"
Assert ($playback -match 'AbilityMovementRuntime\.MovementMode\.BURST') "Normal dashes use burst velocity planning instead of direct teleport movement"
Assert ($playback -match 'applyBurstVelocity') "Dash burst movement writes Velocity instead of only moveTo"
Assert ((Get-Content (Join-Path $ProjectRoot "src/main/java/com/motm/runtime/ability/movement/AbilityMovementRuntime.java") -Raw) -match '"burrow"') "Burrow remains an explicit whack-a-mole reposition exception"
Assert ($playback -notmatch 'case "debris" -> placementAdapter\.placeTrailSelection') "Debris does not place placeholder trail blocks"
Assert ($playback -match 'case "debris" -> "brown debris wave"') "Debris is routed as a brown debris wave cue"
Assert ($playback -notmatch 'case "tunnel" -> placementAdapter\.placeTrailSelection') "Tunnel does not place placeholder stone trail blocks"
Assert ($playback -match 'case "tunnel" -> "stone tunnel form"') "Tunnel is routed as a transformation/movement cue"

$lapidary = Get-Ability $abilities "lapidary"
Assert ($lapidary.cast_type -eq "ground_target") "Lapidary places a ground target object"
Assert ($lapidary.target_type -eq "ground_target") "Lapidary targets ground"
Assert ([double]$lapidary.shield_percent -eq 0.0) "Lapidary is not a self-shield"
Assert ([double]$lapidary.duration_seconds -ge 30.0) "Lapidary has a persistent test duration"
Assert ($lapidary.terrain_effect -eq "crystal_gem") "Lapidary terrain effect is crystal_gem"

$fracture = Get-Ability $abilities "fracture"
Assert ($fracture.cast_type -eq "ground_burst") "Fracture is an expanding burst"
Assert ([double]$fracture.radius -eq 20.0) "Fracture radius is 20"
Assert ([double]$fracture.height -eq 12.0) "Fracture height is 12"
Assert ([double]$fracture.damage_percent -eq 40.0) "Fracture damage is 40 percent max HP"
Assert ($fracture.terrain_effect -eq "crystal_fracture") "Fracture terrain effect is crystal_fracture"
Assert ($playback -notmatch 'case "fracture" -> placementAdapter\.placeRingBlockSelection') "Fracture does not place placeholder crystal ring blocks"
Assert ($playback -match 'case "fracture" -> \{[\s\S]*?green fracture burst at') "Fracture is routed as a green burst cue"

$refraction = Get-Ability $abilities "refraction"
Assert ($refraction.cast_type -eq "support_zone") "Refraction is a support zone"
Assert ([double]$refraction.radius -eq 20.0) "Refraction radius is 20"
Assert ([double]$refraction.height -eq 12.0) "Refraction height is 12"
Assert ($refraction.terrain_effect -eq "crystal_refraction") "Refraction terrain effect is crystal_refraction"
Assert ($playback -notmatch 'case "refraction" -> placementAdapter\.placeRingBlockSelection') "Refraction does not place placeholder crystal ring blocks"
Assert ($playback -match 'case "refraction" -> \{[\s\S]*?green refraction aura at') "Refraction is routed as a green aura cue"

Assert ($playback -match '\[MOTM\]\[terra-audit\]') "Terra cast telemetry is present"
Assert ($playback -match 'targetBlock\.y \+ 1\.0') "Ground markers are offset above targeted blocks"
Assert ($playback -match 'surfaceOverlayAnchor\(pos\)') "Trails place decorations on surfaces, not inside blocks"
Assert ($playback -match 'case "fracture"') "Fracture has terrain runtime hooks"
Assert ($playback -match 'case "refraction"') "Refraction has terrain runtime hooks"
Assert ($playback -match 'resolveActiveLapidaryGemCenter') "Gem abilities resolve the active Lapidary anchor"
Assert ($playback -match '2,\s*2,\s*2,\s*expireAt') "Lapidary visual is a 2x2x2 cube"
Assert ($sinkholeMarker -match 'Sinkhole surface marker uses particle/effect cue only') "Sinkhole marker avoids placeholder crack block platforms"
Assert ($sinkholeMarker -notmatch 'placeSurfacePatchSelection') "Sinkhole marker does not place surface patch block cracks"
Assert ($sinkholeMarker -notmatch 'placeRingBlockSelection') "Sinkhole marker does not place dust ring blocks"
Assert ($playback -match 'GLOBAL_CLEANUP_ROLES[\s\S]*?Test_Dummy_Stationary') "Style-test dummy cleanup includes stale global dummy cleanup"
Assert ($playback -match 'MOTM_Proof_Coating_Obsidian') "Obsidian Skin queues the obsidian coating effect"
Assert ($playback -match 'Plant_Flower_Tall_Red') "Nightshade uses the locked tall red flower marker"
Assert ($playback -match 'Plant_Cactus_Ball_1') "Cacti Cluster uses the locked cactus ball marker"
Assert ($playback -match 'case "gargoyle" -> \{[\s\S]*?MOTM_Proof_Coating_Stone[\s\S]*?owner stone coating') "Gargoyle uses entity stone coating without marker blocks"
Assert ($playback -match 'case "glare" -> \{[\s\S]*?MOTM_Proof_Coating_Stone[\s\S]*?target stone coating') "Glare uses target stone coating without marker blocks"
Assert ($playback -notmatch 'case "gargoyle" -> placementAdapter\.placeSurfaceColumnSelection') "Gargoyle does not place placeholder statue blocks"
Assert ($playback -notmatch 'case "glare" ->[\s\S]*?Furniture_Temple_Light_Statue') "Glare does not place placeholder statue blocks"
Assert ($playback -match '"rubble_rouser"[\s\S]*?4\.0') "Rubble Rouser splash radius is 4"
Assert ($playback -match 'case "alloy_enhancement" -> 0\.30') "Alloy Enhancement native damage multiplier is 30 percent"
Assert ($playback -match 'terrainEffect\.contains\("mudpit"\)[\s\S]*?applyTargetToken\("slow"[\s\S]*?applyTargetToken\("vulnerability"') "Mud Pit applies slow and vulnerability without root"
Assert ($playback -match 'terrainEffect\.contains\("sandstorm"\)[\s\S]*?applyTargetToken\("slow"[\s\S]*?applyTargetToken\("vulnerability"') "Sandstorm applies slow and vulnerability"

if ($script:Failed) {
    Write-Host "Terra implementation audit: FAIL" -ForegroundColor Red
    exit 1
}

Write-Host "Terra implementation audit: PASS" -ForegroundColor Green
