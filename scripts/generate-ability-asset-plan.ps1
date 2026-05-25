param(
    [string]$OutputRoot,
    [switch]$NoTimestamp
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $OutputRoot) {
    if ($NoTimestamp) {
        $OutputRoot = Join-Path $repoRoot "audits\ability-asset-plan\latest"
    } else {
        $stamp = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
        $OutputRoot = Join-Path $repoRoot (Join-Path "audits\ability-asset-plan" $stamp)
    }
}
New-Item -ItemType Directory -Path $OutputRoot -Force | Out-Null

$conceptMapPath = Join-Path $repoRoot "audits\concept-visual-map\latest\concept-visual-map.json"
if (-not (Test-Path -LiteralPath $conceptMapPath)) {
    & powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot "generate-concept-visual-map.ps1") -NoTimestamp
}
$conceptRows = Get-Content -LiteralPath $conceptMapPath -Raw | ConvertFrom-Json

$assetCatalogRoot = Join-Path $repoRoot "audits\harness\assets\2026-05-22Tknowledge-pass-verify"
$particleCatalog = Join-Path $assetCatalogRoot "particles-all.txt"
$modelCatalog = Join-Path $assetCatalogRoot "models-all.txt"
$catalogEntries = New-Object 'System.Collections.Generic.HashSet[string]'
foreach ($catalog in @($particleCatalog, $modelCatalog)) {
    if (Test-Path -LiteralPath $catalog) {
        foreach ($line in Get-Content -LiteralPath $catalog) {
            if (-not [string]::IsNullOrWhiteSpace($line)) {
                [void]$catalogEntries.Add($line.Trim())
            }
        }
    }
}

$localEffectRoot = Join-Path $repoRoot "src\main\resources\Server\Entity\Effects\MOTM"
$localEffects = New-Object 'System.Collections.Generic.HashSet[string]'
if (Test-Path -LiteralPath $localEffectRoot) {
    Get-ChildItem -LiteralPath $localEffectRoot -Filter "*.json" |
        ForEach-Object { [void]$localEffects.Add($_.BaseName) }
}

function New-AssetSet($cast, $travel, $impact, $loop, $model, $bridge) {
    [pscustomobject]@{
        cast = @($cast) | Where-Object { $_ }
        travel = @($travel) | Where-Object { $_ }
        impact = @($impact) | Where-Object { $_ }
        loop = @($loop) | Where-Object { $_ }
        model = @($model) | Where-Object { $_ }
        bridge = $bridge
    }
}

function Test-Asset($asset) {
    if ([string]::IsNullOrWhiteSpace($asset)) { return "none" }
    if ($asset -like "MOTM_*") {
        if ($localEffects.Contains($asset)) { return "local-effect-ok" }
        return "missing-local-effect"
    }
    if ($catalogEntries.Contains($asset)) { return "asset-ok" }
    return "missing-asset"
}

function Join-Cell($values) {
    $items = @($values) | Where-Object { -not [string]::IsNullOrWhiteSpace($_) }
    if ($items.Count -eq 0) { return "" }
    return ($items -join "<br>")
}

$S = @{
    "terra/quake" = New-AssetSet "MOTM_Terra_Quake_Cast" "Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner" @("MOTM_Terra_Quake_Impact","Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Ground_Hit_Crack.particlespawner") "MOTM_Terra_Quake_Loop" $null "Use stacked ground proxies: dust close to feet, crack ring at impact, brown-tinted quake loop for lingering AoE."
    "terra/metal" = New-AssetSet "Server/Particles/Block/Metal/Spawners/Block_Break_Metal_Sparks.particlespawner" "Server/Particles/Block/Metal/Spawners/Block_Land_Metal_Sparks.particlespawner" @("Server/Particles/Block/Metal/Spawners/Block_Break_Metal_Flash.particlespawner","Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner") $null $null "Read as armor/weight: metallic sparks on cast, hard impact flash, barrier/field proxies with steel tint."
    "terra/magma" = New-AssetSet "Server/Particles/Combat/Fire_Stick/Spawners/Fire_Charge1_Fire.particlespawner" "Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Smoke.particlespawner" "Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Fire.particlespawner" "Server/Particles/Combat/Fire_Stick/Fire_Trap/Fire_AoE_Grow.particlesystem" "Common/NPC/Elemental/Golem_Firesteel/Models/Model.blockymodel" "Use fire plus smoke, but keep motion viscous/heavy with delayed ground fields and molten proxies."
    "terra/stone" = New-AssetSet "Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner" "Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Parts.particlespawner" @("Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Ground_Hit_Crack.particlespawner","Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Sparks.particlespawner") $null $null "Favor rubble, vertical drops, and mace-style ground hits over generic earth glow."
    "terra/arbor" = New-AssetSet "Server/Particles/Deployables/Healing_Totem/Totem_Heal_Sparks_Constant.particlespawner" "Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner" "Server/Particles/Deployables/Healing_Totem/Totem_Heal_SmokeFlat_Constant.particlespawner" "Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner" "Common/NPC/Elemental/Spirit_Root/Models/Model.blockymodel" "Represent roots/growth with green-tinted heal sparks, root-spirit summon/model, and low ground loops."
    "terra/bloom" = New-AssetSet "Server/Particles/Projectile/Acid/Spawners/Acid_Sparks.particlespawner" "Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner" "Server/Particles/Combat/Impact/Misc/Impact_Poison.particlesystem" "Server/Particles/Deployables/Slowness_Totem/Totem_Slow_SmokeFlat_Constant.particlespawner" $null "Make flower powers readable as spores/toxic pollen: purple/pink tint on poison/acid particles and visible slow field."
    "terra/self_petrification" = New-AssetSet "Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner" $null "Server/Particles/Combat/Impact/Misc/Ice/Spawner/Impact_Ice_Shockwave.particlespawner" "MOTM_Terra_Ground_Cracks" "Common/NPC/Elemental/Golem_Crystal/Models/Model.blockymodel" "Use caster body tint/model-change proof for statue form, dust at feet, and target gaze impact."
    "terra/soil" = New-AssetSet "Server/Particles/Block/Grass/Block_Break_Grass_Earth.particlesystem" "Server/Particles/Block/Stone/Spawners/Block_Sprint_Stone_Dust.particlespawner" "Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Ground_Hit_Crack.particlespawner" "Server/Particles/Block/Stone/Block_Land_Soft_Stone.particlesystem" $null "Use dirt-substitute particles for burrow/mud trails, with low-opacity brown field loops."
    "terra/sand" = New-AssetSet "Server/Particles/Block/Sand/Spawners/Block_Break_Sand_Dust.particlespawner" "Server/Particles/Weather/Sand/Spawners/Sand_Storm.particlespawner" "Server/Particles/Block/Sand/Block_Land_Sand_Hard.particlesystem" "Server/Particles/Weather/Sand/Sand_Storm.particlesystem" $null "Sand abilities need moving cloud volume, not just impact dust; use sandstorm as loop and dust on hit."
    "terra/gem" = New-AssetSet "MOTM_Terra_Gem_Cast" "Server/Particles/Block/Crystal/Spawners/Block_Run_Crystal_Sparks_Big.particlespawner" "MOTM_Terra_Gem_Impact" "MOTM_Terra_Gem_Field" "Common/NPC/Elemental/Golem_Crystal/Models/Model.blockymodel" "Use sharp crystal sparks plus faceted model proxies; proof should capture refraction/spark density."
    "hydro/icicle" = New-AssetSet "Server/Particles/Combat/Impact/Misc/Ice/Spawner/Impact_Ice_Shockwave.particlespawner" "Server/Particles/Projectile/Iceball/Spawners/IceBall_Trail_Crystals.particlespawner" "Server/Particles/Combat/Impact/Misc/Ice/Spawner/Impact_Ice_Shockwave.particlespawner" "Server/Particles/Deployables/Slowness_Totem/Totem_Slow_SmokeFlat_Constant.particlespawner" "Common/NPC/Elemental/Spirit_Frost/Models/Model.blockymodel" "Sharp projectile/line reads: crystal trail during travel, brittle ice shockwave on impact, slow/freeze loop after hit."
    "hydro/snow" = New-AssetSet "Server/Particles/Deployables/Slowness_Totem/Totem_Slow_SmokeFlat_Constant.particlespawner" "Server/Particles/Combat/Impact/Misc/Ice/Spawner/Impact_Ice_Shockwave.particlespawner" "Server/Particles/Combat/Impact/Misc/Ice/Spawner/Impact_Ice_Shockwave.particlespawner" "Server/Particles/Deployables/Slowness_Totem/Totem_Slow_SmokeFlat_Constant.particlespawner" "Common/NPC/Elemental/Spirit_Frost/Models/Model.blockymodel" "Snow should be softer than Icicle: use misty slow loops and frost spirit support, not only sharp impacts."
    "hydro/surf" = New-AssetSet "Server/Particles/Block/Water/Spawners/Bubbles.particlespawner" "Server/Particles/Block/Water/Spawners/Water_Bubble_Stream_Alpha.particlespawner" "Server/Particles/Block/Water/Spawners/Water_Small_Burst.particlespawner" "Server/Particles/Weather/Geyzer/Spawners/Water_Beam_Waves.particlespawner" $null "Wave and rush effects need visible travel direction, foam burst on impact, and movement/displacement proof."
    "hydro/rain" = New-AssetSet "Server/Particles/Weather/Rain/Spawners/Water_Dripping.particlespawner" "Server/Particles/Weather/Rain/Water_Dripping.particlesystem" "Server/Particles/Block/Water/Spawners/Water_Small_Burst.particlespawner" "Server/Particles/Deployables/Healing_Totem/Totem_Heal_Sparks_Constant.particlespawner" $null "Use rain as sustained vertical motion, then water bursts/heal sparks for storm and rainbow effects."
    "hydro/boiling" = New-AssetSet "Server/Particles/Weather/Geyzer/Spawners/Water_Beam_Spawn.particlespawner" "Server/Particles/Weather/Geyzer/Spawners/Water_Beam.particlespawner" @("Server/Particles/Weather/Geyzer/Spawners/Water_Beam_Splash.particlespawner","Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Smoke.particlespawner") "Server/Particles/Weather/Geyzer/Geyzer.particlesystem" $null "Boiling is pressure plus scalding: vertical geyser, steam/smoke, and burn/status proof."
    "hydro/vapor" = New-AssetSet "Server/Particles/Deployables/Healing_Totem/Totem_Heal_SmokeFlat_Constant.particlespawner" "Server/Particles/Combat/Mace/Signature/Spawners/Cast/Mace_Signature_Cast_Smoke.particlespawner" "Server/Particles/Combat/Mace/Signature/Spawners/Cast/Mace_Signature_Cast_End_Smoke.particlespawner" "Server/Particles/Deployables/Healing_Totem/Totem_Heal_SmokeFlat_Constant.particlespawner" $null "Vapor needs vanish/reform timing: smoke at old position, smoke end at new position, defensive/status logs."
    "hydro/iceberg" = New-AssetSet "Server/Particles/Combat/Impact/Misc/Ice/Spawner/Impact_Ice_Shockwave.particlespawner" "Server/Particles/Block/Crystal/Spawners/Block_Break_Crystal_Sparks.particlespawner" @("Server/Particles/Combat/Impact/Misc/Ice/Spawner/Impact_Ice_Shockwave.particlespawner","Server/Particles/Block/Crystal/Spawners/Block_Break_Crystal_Parts.particlespawner") $null "Common/NPC/Elemental/Golem_Crystal/Models/Model.blockymodel" "Make iceberg large/heavy with slab proxy/model, freeze armor proof, and crushing impact."
    "hydro/saltwater" = New-AssetSet "Server/Particles/Block/Water/Spawners/Bubbles.particlespawner" "Server/Particles/Block/Water/Spawners/Water_Bubble_Stream_Alpha.particlespawner" "Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner" "Server/Particles/Block/Water/Water_Bubble_Stream.particlesystem" $null "Deep-sea pressure should pull/slow: water streams plus pressure shockwave and target displacement/status proof."
    "hydro/freshwater" = New-AssetSet "Server/Particles/Block/Water/Spawners/Bubbles.particlespawner" "Server/Particles/Block/Water/Spawners/Water_Bubble_Stream_Alpha.particlespawner" "Server/Particles/Block/Water/Spawners/Water_Small_Burst.particlespawner" "Server/Particles/Deployables/Healing_Totem/Totem_Heal_Sparks_Constant.particlespawner" "Common/NPC/Critter/Frog/Models/Model.blockymodel" "Use clean water motion and small companion/summon model; proof should show ally/support value."
    "hydro/bilgewater" = New-AssetSet "Server/Particles/Projectile/Acid/Spawners/Acid_Sparks.particlespawner" "Server/Particles/Block/Water/Spawners/Water_Bubble_Stream_Alpha.particlespawner" "Server/Particles/Combat/Impact/Misc/Impact_Poison.particlesystem" "Server/Particles/Deployables/Slowness_Totem/Totem_Slow_SmokeFlat_Constant.particlespawner" $null "Bilgewater is dirty water: acid/poison tint, sluggish field, and corrosion/debuff proof."
    "aero/scream" = New-AssetSet "MOTM_Aero_Scream_Cast" "Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner" "MOTM_Aero_Scream_Impact" "MOTM_Aero_Scream_Field" $null "Sonic reads as pale rings: shockwave impact plus cone/facing target proof."
    "aero/jet" = New-AssetSet "Server/Particles/Combat/Sword/Signature/Spawners/Ready_Flash/Sword_Signature_Ready_Sparks.particlespawner" "Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner" "Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner" "Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Smoke.particlespawner" $null "Jet must prove motion: start burst, body trail, end impact, and before/after position log."
    "aero/thunder" = New-AssetSet "Server/Particles/Combat/Sword/Signature/Spawners/Ready_Flash/Sword_Signature_Ready_Sparks.particlespawner" "Server/Particles/NPC/Void_Dragon/Spawners/Void_Lightning.particlespawner" "Server/Particles/NPC/Void_Dragon/Spawners/Void_Lightning.particlespawner" "Server/Particles/Combat/Sword/Signature/Spawners/Ready_Flash/Sword_Signature_Ready_Sparks.particlespawner" "Common/NPC/Elemental/Spirit_Thunder/Models/Model.blockymodel" "Use lightning arcs and bright sparks; proof should catch flash frame plus stun/shock state."
    "aero/tornado" = New-AssetSet "Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner" "Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Sparks.particlespawner" "Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner" "Server/Particles/Combat/Battleaxe/Signature/Battleaxe_Signature_Whirlwind.particlesystem" $null "Tornado must show sustained spiral/funnel and repeated pull/tick proof."
    "aero/jump" = New-AssetSet "Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner" "Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner" "Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner" $null $null "Every jump ability needs airborne displacement proof and landing/impact screenshots, not standing casts."
    "aero/wind_blade" = New-AssetSet "Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner" "Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Slash_Alpha.particlespawner" "Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Slash_Bright.particlespawner" $null $null "Wind blade should be a visible cutting line/arc with impact proof at target side."
    "aero/smoke" = New-AssetSet "Server/Particles/Combat/Mace/Signature/Spawners/Cast/Mace_Signature_Cast_Smoke.particlespawner" "Server/Particles/Combat/Mace/Signature/Spawners/Cast/Mace_Signature_Cast_End_Smoke.particlespawner" "Server/Particles/Combat/Mace/Signature/Spawners/Cast/Mace_Signature_Cast_End_Smoke.particlespawner" "Server/Particles/Deployables/Slowness_Totem/Totem_Slow_SmokeFlat_Constant.particlespawner" "Common/NPC/Flying_Critter/Bat/Models/Model.blockymodel" "Smoke needs third-person vanish/form proof plus dark cloud field, not first-person wall staring."
    "aero/gale_wizard" = New-AssetSet "Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner" "Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner" "Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner" "Server/Particles/Combat/Battleaxe/Signature/Battleaxe_Signature_Whirlwind.particlesystem" $null "Blend wizard-like casting with controlled wind: cast pulse, shaped gust, sustained field."
    "aero/pressure" = New-AssetSet "Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner" "Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner" "Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner" $null $null "Pressure should be nearly invisible force with readable shock rings and knockback/displacement proof."
    "aero/pollution" = New-AssetSet "Server/Particles/Projectile/Acid/Spawners/Acid_Sparks.particlespawner" "Server/Particles/Weather/Rain/Spawners/Water_Dripping.particlespawner" "Server/Particles/Combat/Impact/Misc/Impact_Poison.particlesystem" "Server/Particles/Deployables/Slowness_Totem/Totem_Slow_SmokeFlat_Constant.particlespawner" $null "Toxic haze should mix airborne rain/smoke with poison/acid effects and debuff proof."
    "corruptus/flame" = New-AssetSet "Server/Particles/Combat/Fire_Stick/Spawners/Fire_Charge1_Fire.particlespawner" "Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Smoke.particlespawner" "Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Fire.particlespawner" "Server/Particles/Combat/Fire_Stick/Fire_Trap/Fire_AoE_Grow.particlesystem" $null "Fire needs bright cast, smoke trail, impact flame, burn/status proof."
    "corruptus/necro" = New-AssetSet "Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner" "Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner" "Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem" "Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner" "Common/NPC/Undead/Shadow_Knight/Models/Model.blockymodel" "Necro should drain/raise: void smoke, undead model proxy, and life-steal/summon proof."
    "corruptus/shadow" = New-AssetSet "Server/Particles/Combat/Mace/Signature/Spawners/Cast/Mace_Signature_Cast_Smoke.particlespawner" "Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner" "Server/Particles/Combat/Impact/Misc/Void/VoidSplash.particlespawner" "Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner" "Common/NPC/Undead/Shadow_Knight/Models/Model.blockymodel" "Shadow needs clone/stealth proof plus dark zone visuals and target-side hit proof."
    "corruptus/hell_flame" = New-AssetSet "Server/Particles/Combat/Fire_Stick/Spawners/Fire_Charge1_Fire.particlespawner" "Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Smoke.particlespawner" "Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Fire.particlespawner" "Server/Particles/Combat/Fire_Stick/Fire_Trap/Fire_AoE_Grow.particlesystem" "Common/NPC/Elemental/Golem_Firesteel/Models/Model.blockymodel" "Hell flame should be harsher than Flame: darker tint, self-cost proof, and persistent infernal ground."
    "corruptus/mentokinesis" = New-AssetSet "Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner" "Server/Particles/Spell/Portal/Spawners/MagicPortal/MagicPortal_VoidSparks.particlespawner" "Server/Particles/Spell/Portal/Spawners/MagicPortal/MagicPortal_VoidFlash.particlespawner" "Server/Particles/Spell/Portal/Spawners/MagicPortal/MagicPortal_VoidWaves.particlespawner" "Common/NPC/Void/Eye_Void/Models/Model.blockymodel" "Psychic abilities need gaze/control proof; use eye/void sparks, but mark portal assets for showcase review before final visual PASS."
    "corruptus/imbuement" = New-AssetSet "Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner" $null "Server/Particles/Combat/Impact/Misc/Void/VoidSplash.particlespawner" "Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner" $null "Imbuement is body enchantment: third-person aura/tint and outgoing empowered hit proof."
    "corruptus/attonement" = New-AssetSet "Server/Particles/Deployables/Healing_Totem/Totem_Heal_Sparks_Constant.particlespawner" $null "Server/Particles/Deployables/Healing_Totem/Totem_Heal_SmokeFlat_Constant.particlespawner" "Server/Particles/Deployables/Healing_Totem/Totem_Heal_Sparks_Constant.particlespawner" $null "Atonement should look cleansing/golden, with heal/cleanse/protection proof rather than story/codex framing."
    "corruptus/void" = New-AssetSet "Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner" "Server/Particles/Spell/Portal/Spawners/MagicPortal/MagicPortal_VoidSparks.particlespawner" "Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem" "Server/Particles/Spell/Portal/Spawners/MagicPortal/MagicPortal_VoidWaves.particlespawner" "Common/NPC/Void/Spawn_Void/Models/Model.blockymodel" "Void needs rift/consumption: portal-like field with pulling/status proof; portal assets require in-game showcase review before final."
    "corruptus/scarak" = New-AssetSet "Server/Particles/Projectile/Acid/Spawners/Acid_Sparks.particlespawner" "Server/Particles/Combat/Impact/Misc/Impact_Poison.particlesystem" "Server/Particles/Combat/Impact/Misc/Impact_Poison.particlesystem" "Server/Particles/Deployables/Slowness_Totem/Totem_Slow_SmokeFlat_Constant.particlespawner" "Common/NPC/Beast/Scarak_Fighter/Models/Model.blockymodel" "Scarak needs swarm/brood proof: acid/poison visuals, Scarak model role, nest/summon behavior."
    "corruptus/primordial" = New-AssetSet "Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner" "Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner" "Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner" $null @("Common/NPC/Flying_Beast/Pterodactyl/Models/Model.blockymodel","Common/NPC/Beast/Toad_Rhino/Models/Model.blockymodel","Common/NPC/Beast/Rex_Cave/Models/Model.blockymodel") "Primordial is form-based: each ability needs the right beast model plus movement/roar/impact proof."
}

function Get-ProofRequirement($scenario) {
    switch ($scenario) {
        "jump_land" { "Log armed jump, airborne movement, landing resolved with targets; screenshot landing ring/cracks." }
        "movement_or_form" { "Log start/end positions and target result; capture third-person trail/form frame." }
        "cone_or_gaze" { "Log facing direction and targets; capture cone/gaze from over-shoulder view." }
        "persistent_field" { "Log field engage/ticks/release; capture field radius and victim status." }
        "ground_mark_or_barrier" { "Log telegraph, delay, impact; capture ground mark/barrier before impact." }
        "projectile_or_line" { "Log projectile role, launch, hit; capture travel line and target impact." }
        "summon_or_command" { "Log role/model resolution and summon behavior; capture summon visible beside target." }
        "self_or_channel" { "Log caster status/aura/channel ticks; capture third-person body effect." }
        default { "Log target acquired and combat/status result; capture target impact." }
    }
}

$rows = New-Object System.Collections.Generic.List[object]
foreach ($row in $conceptRows) {
    $key = "$($row.class_id)/$($row.style_id)"
    $assets = $S[$key]
    if (-not $assets) { throw "Missing style asset plan for $key" }
    $allAssets = @() + @($assets.cast) + @($assets.travel) + @($assets.impact) + @($assets.loop) + @($assets.model)
    $allAssets = @($allAssets | Where-Object { -not [string]::IsNullOrWhiteSpace([string]$_) })
    $checks = @($allAssets | ForEach-Object { [pscustomobject]@{ asset = $_; status = Test-Asset $_ } })
    $rows.Add([pscustomobject]@{
        class_id = $row.class_id
        style_id = $row.style_id
        ability_id = $row.ability_id
        concept = $row.concept
        palette = $row.palette
        style_feel = $row.style_feel
        scenario = $row.scenario
        motion_shape = $row.motion_shape
        cast_assets = $assets.cast
        travel_assets = $assets.travel
        impact_assets = $assets.impact
        loop_assets = $assets.loop
        model_assets = $assets.model
        implementation_bridge = $assets.bridge
        proof_requirement = Get-ProofRequirement $row.scenario
        validation = $checks
        validation_summary = (($checks | Group-Object status | ForEach-Object { "$($_.Name)=$($_.Count)" }) -join "; ")
        showcase_review = ($allAssets | Where-Object { $_ -like "Server/Particles/_Test/*" -or $_ -like "Server/Particles/Spell/Portal/*" }).Count -gt 0
    }) | Out-Null
}

$bad = @($rows.validation | Where-Object { $_.status -like "missing*" })
$showcase = @($rows | Where-Object { $_.showcase_review })

$md = New-Object System.Collections.Generic.List[string]
$md.Add("# MOTM Ability Asset Implementation Plan")
$md.Add("")
$md.Add("- Generated: $(Get-Date -Format o)")
$md.Add("- Ability rows: $($rows.Count)")
$md.Add("- Missing asset references: $($bad.Count)")
$md.Add("- Rows needing showcase review: $($showcase.Count)")
$md.Add("- Sources: protected style JSON, concept visual map, local Assets.zip catalogs, local MOTM EntityEffect files")
$md.Add("")
$md.Add("| Class | Style | Ability | Concept | Scenario | Cast | Travel | Impact | Loop | Model | Proof | Validation |")
$md.Add("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
foreach ($r in $rows) {
    $md.Add("| $($r.class_id) | $($r.style_id) | $($r.ability_id) | $(([string]$r.concept).Replace('|','/')) | $($r.scenario) | $(Join-Cell $r.cast_assets) | $(Join-Cell $r.travel_assets) | $(Join-Cell $r.impact_assets) | $(Join-Cell $r.loop_assets) | $(Join-Cell $r.model_assets) | $(([string]$r.proof_requirement).Replace('|','/')) | $($r.validation_summary) |")
}
if ($bad.Count -gt 0) {
    $md.Add("")
    $md.Add("## Missing References")
    foreach ($missing in $bad) {
        $md.Add("- $($missing.status): $($missing.asset)")
    }
}
if ($showcase.Count -gt 0) {
    $md.Add("")
    $md.Add("## Showcase Review Required")
    $md.Add("")
    $md.Add("These rows use portal or test/cinematic particle families that exist locally but should be viewed in-game before final visual PASS.")
    foreach ($r in $showcase) {
        $md.Add("- $($r.class_id)/$($r.style_id)/$($r.ability_id)")
    }
}

$jsonPath = Join-Path $OutputRoot "ability-asset-plan.json"
$mdPath = Join-Path $OutputRoot "ability-asset-plan.md"
$rows | ConvertTo-Json -Depth 10 | Set-Content -LiteralPath $jsonPath -Encoding UTF8
$md | Set-Content -LiteralPath $mdPath -Encoding UTF8

Write-Host "[generate-ability-asset-plan] Wrote $mdPath"
Write-Host "[generate-ability-asset-plan] Wrote $jsonPath"
if ($bad.Count -gt 0) {
    Write-Warning "[generate-ability-asset-plan] Missing asset references: $($bad.Count)"
}
