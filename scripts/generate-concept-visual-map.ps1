param(
    [string]$OutputRoot,
    [switch]$NoTimestamp
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $OutputRoot) {
    if ($NoTimestamp) {
        $OutputRoot = Join-Path $repoRoot "audits\concept-visual-map\latest"
    } else {
        $stamp = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
        $OutputRoot = Join-Path $repoRoot (Join-Path "audits\concept-visual-map" $stamp)
    }
}
New-Item -ItemType Directory -Path $OutputRoot -Force | Out-Null

$styleVisuals = @{
    "terra/quake" = @{
        feel = "heavy tremor, cracked earth, dust shockwave"
        palette = "earth brown, dark soil, tan dust"
        assets = "Mace ground-hit cracks, stone dust, Earth_Brazier_Glow, Mace_Signature_Shockwave"
    }
    "terra/metal" = @{
        feel = "hard polished plating, forge sparks, barrier weight"
        palette = "steel gray, dark metal, pale highlights"
        assets = "metal sparks, stone/metal impact particles, solid barrier proxy"
    }
    "terra/magma" = @{
        feel = "molten orange, black smoke, viscous heat"
        palette = "orange lava, black-red smoke, amber glow"
        assets = "Fire_Charge1_Fire, Impact_Fire, Impact_Smoke, Fire_AoE_Grow"
    }
    "terra/stone" = @{
        feel = "slab-heavy rock impacts, falling rubble"
        palette = "gray stone, charcoal, pale chips"
        assets = "stone dust, stone parts/sparks, mace impact shockwaves"
    }
    "terra/arbor" = @{
        feel = "living roots, leaves, wood growth"
        palette = "leaf green, bark brown, pale growth"
        assets = "root spirit model, heal sparks, wind sparks as leaf motion"
    }
    "terra/bloom" = @{
        feel = "floral poison, spores, vivid toxic bloom"
        palette = "purple flower, dark green, sickly pink"
        assets = "Acid_Sparks, Impact_Poison, Slowness_Totem smoke"
    }
    "terra/self_petrification" = @{
        feel = "statue gray, stone shell, frozen gaze"
        palette = "gray stone, dark slate, silver"
        assets = "stone dust, ice shockwave, entity gray tint, gaze proxy"
    }
    "terra/soil" = @{
        feel = "mud, burrow trails, ruptured damp ground"
        palette = "mud brown, dark soil, tan grit"
        assets = "stone dust as dirt, ground cracks, brown tint, dust trail proxies"
    }
    "terra/sand" = @{
        feel = "desert dust, sandstorm, glass heat"
        palette = "sand gold, brown shadow, pale heat"
        assets = "sand dust, Sand_Storm weather particles, fire/sparks for glass"
    }
    "terra/gem" = @{
        feel = "crystal shards, refraction, shield facets"
        palette = "white crystal, purple-blue gem, pale sparkle"
        assets = "crystal sparks, gem sparks, crystal golem attachments"
    }
    "hydro/icicle" = @{
        feel = "sharp brittle ice shards"
        palette = "white-blue, deep cyan, ice white"
        assets = "Impact_Ice_Shockwave, Iceball crystals, water bubbles"
    }
    "hydro/snow" = @{
        feel = "soft powder drift, snow spirits, muffled cold"
        palette = "snow white, muted blue-gray, pale frost"
        assets = "ice impact, slow smoke, frost spirit/golem models"
    }
    "hydro/surf" = @{
        feel = "bright wave motion, rushing tide"
        palette = "cyan water, deep blue, pale foam"
        assets = "Water_Bubble_Stream_Alpha, Water_Small_Burst, water splash variants"
    }
    "hydro/rain" = @{
        feel = "falling rain, healing rainbow, splash sustain"
        palette = "cool rain blue, sky blue, pale light"
        assets = "Water_Dripping, water burst, heal sparks/smoke"
    }
    "hydro/boiling" = @{
        feel = "steam pressure, scalding jets, hot water"
        palette = "steam white, amber heat, rusty red"
        assets = "Geyzer water beam particles, Impact_Smoke, Fire/Water mix"
    }
    "hydro/vapor" = @{
        feel = "mist vanish, reform, soft translucence"
        palette = "pale mist, blue-gray, white"
        assets = "Totem_Heal_SmokeFlat_Constant, bubbles, smoke proxies"
    }
    "hydro/iceberg" = @{
        feel = "large heavy ice slabs, armor, crush"
        palette = "glacial blue, deep sea blue, icy white"
        assets = "Impact_Ice_Shockwave, crystal sparks, barrier proxy"
    }
    "hydro/saltwater" = @{
        feel = "deep ocean pressure, undertow, tide pool"
        palette = "dark teal, navy, seafoam"
        assets = "Water_Bubble_Stream_Alpha, water bursts, pressure shockwave"
    }
    "hydro/freshwater" = @{
        feel = "river motion, green-blue life, swamp summon"
        palette = "fresh green-blue, river green, pale water"
        assets = "water stream, Frog model, heal/water particles"
    }
    "hydro/bilgewater" = @{
        feel = "dirty oil, foul water, corrosion"
        palette = "olive sludge, dark green, dirty yellow"
        assets = "Impact_Poison, Acid_Sparks, slow smoke, anchor/metal sparks"
    }
    "aero/scream" = @{
        feel = "sonic rings, pale shockwaves, ringing air"
        palette = "pale cyan, white, light blue"
        assets = "Battleaxe shockwave, wind sparks, mace shockwave rings"
    }
    "aero/jet" = @{
        feel = "fast golden streaks, afterburn trails"
        palette = "gold, amber, white-hot highlight"
        assets = "wind tail, ready sparks, fire smoke for afterburn"
    }
    "aero/thunder" = @{
        feel = "violet-yellow lightning, crackling arcs"
        palette = "violet, gold, pale flash"
        assets = "Void_Lightning, Sword ready sparks, thunder spirit model"
    }
    "aero/tornado" = @{
        feel = "gray funnel, spiral wind, sustained vortex"
        palette = "pale gray, storm blue, white"
        assets = "Battleaxe whirlwind, wind sparks, bash shockwave"
    }
    "aero/jump" = @{
        feel = "aerial arcs, launch trails, landing bursts"
        palette = "gold air, pale wind, warm highlight"
        assets = "wind tail, bash shockwave, jump/landing dust"
    }
    "aero/wind_blade" = @{
        feel = "sharp cutting air, white blade trails"
        palette = "white, lavender-gray, pale blue"
        assets = "wind sparks, slash-like mace/battleaxe particles"
    }
    "aero/smoke" = @{
        feel = "dark smoke clouds, stealth, vanish/reform"
        palette = "dark gray, black-blue, pale ash"
        assets = "Mace cast smoke, cast end smoke, Bat model for smoke form"
    }
    "aero/gale_wizard" = @{
        feel = "refined magical wind, purple-tinted gusts"
        palette = "lavender, purple, pale wind"
        assets = "Battleaxe whirlwind, wind sparks, magic smoke"
    }
    "aero/pressure" = @{
        feel = "compressed air pulses, invisible-force rings"
        palette = "pale blue-white, gray-blue, white"
        assets = "bash shockwaves, wind sparks, round impact rings"
    }
    "aero/pollution" = @{
        feel = "sickly toxic haze, acidic rain"
        palette = "green-yellow, dark olive, acid highlight"
        assets = "Acid_Sparks, Impact_Poison, slow smoke, rain particles"
    }
    "corruptus/flame" = @{
        feel = "red-orange fire, explosive burn"
        palette = "orange-red, dark red, amber"
        assets = "Fire_Charge1_Fire, Impact_Fire, Impact_Smoke, Fire_AoE_Grow"
    }
    "corruptus/necro" = @{
        feel = "undead wisps, drain, grave rise"
        palette = "violet, black, sickly magenta"
        assets = "VoidSmoke_Impact, VoidImpact, Shadow_Knight model"
    }
    "corruptus/shadow" = @{
        feel = "inky stealth, clone, dark zones"
        palette = "black-purple, dark indigo, muted violet"
        assets = "Mace smoke, Void_Sparks, VoidSplash, Shadow_Knight model"
    }
    "corruptus/hell_flame" = @{
        feel = "brutal hellfire, self-scorch, infernal ground"
        palette = "red-orange, black-red, hot amber"
        assets = "Fire_Charge1_Fire, Impact_Fire, Fire_AoE_Grow, fire golem"
    }
    "corruptus/mentokinesis" = @{
        feel = "psychic violet, mind control, gaze"
        palette = "psychic purple, dark violet, pale pink"
        assets = "Void_Sparks, Eye/Void smoke candidates, VoidImpact"
    }
    "corruptus/imbuement" = @{
        feel = "dark body enchantment, saturated arcane buffs"
        palette = "arcane purple, black, pale violet"
        assets = "Void_Sparks, void smoke, entity tint, self aura"
    }
    "corruptus/attonement" = @{
        feel = "corruption-cleansing golden holy break"
        palette = "gold-white, warm tan, clean white"
        assets = "heal sparks, heal smoke, sanctuary field proxy"
    }
    "corruptus/void" = @{
        feel = "cosmic void, rift, consuming darkness"
        palette = "deep purple, black, violet highlight"
        assets = "Void_Sparks, VoidSmoke_Impact, VoidImpact, Spawn_Void/Eye_Void"
    }
    "corruptus/scarak" = @{
        feel = "insect swarm, brood nest, chitin"
        palette = "green-brown, dark shell, dull gold"
        assets = "Acid_Sparks, Impact_Poison, Scarak models, slow smoke"
    }
    "corruptus/primordial" = @{
        feel = "ancient beast transformations, primal roar"
        palette = "earthy brown, dark hide, orange accent"
        assets = "Pterodactyl, Toad_Rhino, Rex_Cave, roar/shockwave particles"
    }
}

function Get-Scenario($Ability) {
    $cast = ([string]$Ability.cast_type).ToLowerInvariant()
    $target = ([string]$Ability.target_type).ToLowerInvariant()
    $trigger = ([string]$Ability.trigger).ToLowerInvariant()
    if ($trigger -eq "jump_land") { return "jump_land" }
    if ($cast -in @("dash", "dash_buff", "dash_strike", "leap", "dive_strike", "teleport", "air_stall", "transformation")) { return "movement_or_form" }
    if ($cast -in @("cone", "gaze")) { return "cone_or_gaze" }
    if ($cast -in @("ground_zone", "support_zone")) { return "persistent_field" }
    if ($cast -in @("ground_target", "ground_strike", "barrier")) { return "ground_mark_or_barrier" }
    if ($cast -like "projectile*" -or $cast -in @("line_control", "wave_line", "chain")) { return "projectile_or_line" }
    if ($cast -in @("summon", "summon_buff")) { return "summon_or_command" }
    if ($target -eq "self" -or $cast -match "self|cleanse|channel") { return "self_or_channel" }
    return "single_target"
}

function Get-MotionShape([string]$Scenario) {
    switch ($Scenario) {
        "jump_land" { "arm -> jump -> landing ring/cracks" }
        "movement_or_form" { "body motion trail, start/end burst, target-side impact" }
        "cone_or_gaze" { "forward fan/gaze beam from caster to target cone" }
        "persistent_field" { "ground proxy loop, radius readable, tick pulses" }
        "ground_mark_or_barrier" { "ground mark/telegraph, delayed hit or solid line wall" }
        "projectile_or_line" { "visible travel path, impact burst on target side" }
        "summon_or_command" { "summon gate/nest/rise, persistent model, action proof" }
        "self_or_channel" { "third-person body aura/tint, HUD/status proof, channel pulses" }
        default { "single target cast flash plus target impact" }
    }
}

function Get-ImplementationBridge([string]$Scenario) {
    switch ($Scenario) {
        "jump_land" { "StyleManager arms ability; GameplayPlaybackManager landing resolver; quake impact ring proxies." }
        "movement_or_form" { "Velocity/Teleport/Transform where proven; otherwise visual trail plus target-side combat proof." }
        "cone_or_gaze" { "Facing acquisition plus cone target query; caster effect plus target status proof." }
        "persistent_field" { "ActiveField tick plus spawnFieldVisualProxy loop EntityEffect." }
        "ground_mark_or_barrier" { "Ground target block, field/barrier proxy, delayed activation when delay_seconds exists." }
        "projectile_or_line" { "launchProjectiles or line runtime, projectile role resolver, target-side impact logging." }
        "summon_or_command" { "handleSummonRuntime with verified role/model mapping and summon action logs." }
        "self_or_channel" { "StatusEffectManager plus EntityEffect on caster; channel tick logs where relevant." }
        default { "applyCombat plus impact EntityEffect and status logs." }
    }
}

$rows = New-Object System.Collections.Generic.List[object]
Get-ChildItem -LiteralPath (Join-Path $repoRoot "src\main\resources\data\styles") -Filter "*_styles.json" |
    Sort-Object Name |
    ForEach-Object {
        $classId = $_.BaseName -replace "_styles$", ""
        $data = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
        foreach ($style in @($data.styles)) {
            $key = "$classId/$($style.id)"
            $visual = $styleVisuals[$key]
            foreach ($ability in @($style.abilities)) {
                $scenario = Get-Scenario $ability
                $rows.Add([pscustomobject]@{
                    class_id = $classId
                    style_id = [string]$style.id
                    ability_id = [string]$ability.id
                    concept = [string]$ability.description
                    style_theme = [string]$style.theme
                    style_feel = if ($visual) { $visual.feel } else { "MISSING STYLE VISUAL MAP" }
                    palette = if ($visual) { $visual.palette } else { "" }
                    hytale_asset_family = if ($visual) { $visual.assets } else { "" }
                    scenario = $scenario
                    motion_shape = Get-MotionShape $scenario
                    implementation_bridge = Get-ImplementationBridge $scenario
                }) | Out-Null
            }
        }
    }

$md = New-Object System.Collections.Generic.List[string]
$md.Add("# MOTM Concept To Hytale Visual Map")
$md.Add("")
$md.Add("- Generated: $(Get-Date -Format o)")
$md.Add("- Rows: $($rows.Count)")
$md.Add("- Sources: protected style JSON + realignment visual identities + local Hytale asset discovery")
$md.Add("")
$md.Add("| Class | Style | Ability | Concept | Style feel | Asset family | Motion shape |")
$md.Add("| --- | --- | --- | --- | --- | --- | --- |")
foreach ($row in $rows) {
    $concept = $row.concept.Replace("|", "/")
    $feel = $row.style_feel.Replace("|", "/")
    $assets = $row.hytale_asset_family.Replace("|", "/")
    $motion = $row.motion_shape.Replace("|", "/")
    $md.Add("| $($row.class_id) | $($row.style_id) | $($row.ability_id) | $concept | $feel | $assets | $motion |")
}

$jsonPath = Join-Path $OutputRoot "concept-visual-map.json"
$mdPath = Join-Path $OutputRoot "concept-visual-map.md"
$rows | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8
$md | Set-Content -LiteralPath $mdPath -Encoding UTF8

Write-Host "[generate-concept-visual-map] Wrote $mdPath"
Write-Host "[generate-concept-visual-map] Wrote $jsonPath"
