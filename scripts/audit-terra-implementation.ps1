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

$stylePath = Join-Path $ProjectRoot "src/main/resources/data/styles/terra_styles.json"
$playbackPath = Join-Path $ProjectRoot "src/main/java/com/motm/manager/GameplayPlaybackManager.java"
$styleManagerPath = Join-Path $ProjectRoot "src/main/java/com/motm/manager/StyleManager.java"

Assert (Test-Path $stylePath) "terra_styles.json exists"
Assert (Test-Path $playbackPath) "GameplayPlaybackManager.java exists"
Assert (Test-Path $styleManagerPath) "StyleManager.java exists"

$data = Get-Content $stylePath -Raw | ConvertFrom-Json
$playback = Get-Content $playbackPath -Raw
$styleManager = Get-Content $styleManagerPath -Raw

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

$pillar = Get-Ability $abilities "pillar_strike"
Assert ([double]$pillar.height -eq 3.0) "Pillar Strike data height is 3"
Assert ($playback -match 'placeStackingColumnSelection\(runtimePlayer\.getWorld\(\),\s*"stone_pillar",\s*center,\s*3,') "Pillar Strike runtime forces a 3-block staged column"

$vines = Get-Ability $abilities "vines"
Assert ([double]$vines.cooldown_seconds -eq 0.0) "Vines has no cooldown"

$sapling = Get-Ability $abilities "sapling"
Assert ($sapling.cast_type -eq "projectile_line") "Sapling fires as a ground-marking projectile line"
Assert ($sapling.target_type -eq "ground_target") "Sapling targets ground"
Assert ($sapling.summon_name -eq "") "Sapling no longer spawns a treant summon"
Assert ($sapling.effect -eq "lure") "Sapling is tagged as lure"

$sandstorm = Get-Ability $abilities "sandstorm"
Assert ([double]$sandstorm.duration_seconds -eq 10.0) "Sandstorm lasts 10 seconds"
Assert ($sandstorm.toggleable -eq $true) "Sandstorm is toggleable"
Assert ([double]$sandstorm.toggle_cooldown_seconds -eq 2.0) "Sandstorm has a 2s toggle cooldown"
Assert ($styleManager -match 'requiresActiveToggle\(ability,\s*"sandstorm"\)') "Dust Devil requires active Sandstorm"
Assert ($styleManager -match 'consumeActiveToggle\(playerId,\s*consumedToggle\)') "Dust Devil consumes active Sandstorm"

$lapidary = Get-Ability $abilities "lapidary"
Assert ($lapidary.cast_type -eq "ground_target") "Lapidary places a ground target object"
Assert ($lapidary.target_type -eq "ground_target") "Lapidary targets ground"
Assert ([double]$lapidary.shield_percent -eq 0.0) "Lapidary is not a self-shield"
Assert ([double]$lapidary.duration_seconds -ge 30.0) "Lapidary has a persistent test duration"
Assert ($lapidary.terrain_effect -eq "crystal_gem") "Lapidary terrain effect is crystal_gem"

$fracture = Get-Ability $abilities "fracture"
Assert ($fracture.cast_type -eq "ground_burst") "Fracture is an expanding burst"
Assert ([double]$fracture.radius -eq 5.0) "Fracture radius is 5"
Assert ($fracture.terrain_effect -eq "crystal_fracture") "Fracture terrain effect is crystal_fracture"

$refraction = Get-Ability $abilities "refraction"
Assert ($refraction.cast_type -eq "support_zone") "Refraction is a support zone"
Assert ([double]$refraction.radius -eq 5.0) "Refraction radius is 5"
Assert ($refraction.terrain_effect -eq "crystal_refraction") "Refraction terrain effect is crystal_refraction"

Assert ($playback -match '\[MOTM\]\[terra-audit\]') "Terra cast telemetry is present"
Assert ($playback -match 'context\.targetBlock\(\)\.getY\(\) \+ 1\.0') "Ground markers are offset above targeted blocks"
Assert ($playback -match 'surfaceDecorationAnchor\(pos\)') "Trails place decorations on surfaces, not inside blocks"
Assert ($playback -match '"fracture",\s*"refraction"\s*->') "Gem abilities have terrain runtime hooks"
Assert ($playback -match 'resolveActiveLapidaryGemCenter') "Gem abilities resolve the active Lapidary anchor"
Assert ($playback -match '2,\s*2,\s*2,\s*expireAt') "Lapidary visual is a 2x2x2 cube"
Assert ($playback -match 'MOTM_Proof_Coating_Obsidian') "Obsidian Skin queues the obsidian coating effect"

if ($script:Failed) {
    Write-Host "Terra implementation audit: FAIL" -ForegroundColor Red
    exit 1
}

Write-Host "Terra implementation audit: PASS" -ForegroundColor Green
