# Terra Manual Review Runbook - 2026-05-23

This runbook turns Terra review into a repeatable user-driven loop. Codex prepares
the world, inventory, class/style, mobs, and camera; the user performs the real
controls and gives visual/mechanical feedback.

## Local Research Facts

```text
Hytale game modes
├── Creative
│   └── best for world setup, screenshots, cleanup, and visual-only checks
└── Adventure
    └── survival-like proof mode in this build
        ├── mining affinity
        ├── durability / shield behavior
        ├── real damage / low-health regen
        └── weapon/tool behavior while not holding the spellbook
```

`HytaleServer.jar` exposes `GameMode.Adventure` and `GameMode.Creative`; no
separate `Survival` enum is present in this build. Use Adventure whenever a test
needs non-creative behavior.

The Terra review kit item IDs were checked against the installed `Assets.zip`:

```text
Core casting
├── MOTM_Spellbook_Focus
└── Recipe_Book_Magic_Void

Tools / weapons
├── Tool_Pickaxe_Iron          mining affinity primary proof
├── Tool_Pickaxe_Wood          mining baseline comparison
├── Tool_Shovel_Iron           non-pickaxe negative control
├── Weapon_Sword_Iron          melee buff / weapon follow-up checks
└── Weapon_Shield_Iron         blocking / durability / shield checks

Terra resources
├── Rock_Stone                 stone_blocks
├── Soil_Dirt                  dirt_blocks
├── Soil_Sand                  sand_blocks
├── Ingredient_Bar_Iron        metal
├── Rock_Gem_Emerald           gems
└── Plant_Seeds_Wheat          seeds

Visual / marker blocks
├── Rock_Crystal_Green_Block   gem/Lapidary visual proof
├── Build_GreyDark_Cube        dark stone / obsidian marker
└── Build_Grey_Cube            neutral marker
```

## Review Loop

```text
For each Terra style
├── Codex setup
│   ├── switch Creative
│   ├── daylight
│   ├── relocate to wide lane
│   ├── grant Terra kit
│   ├── freecast off by default
│   ├── class=terra
│   ├── style=<current style>
│   ├── clear old mobs
│   ├── spawn style-specific stationary layout
│   ├── verify tracked mob count
│   └── verify third-person
├── User test
│   ├── use normal player controls
│   ├── switch held item when the ability requires it
│   ├── move/jump/attack/block/mine as the concept requires
│   └── report what felt wrong or passed
└── Codex cleanup
    ├── capture logs/screenshots if needed
    ├── clear mobs/temporary setup
    ├── record residuals
    └── prep next style
```

Operational target:

```text
User responsibility
└── be in the Hytale window and use the normal ability / movement / attack inputs

Codex responsibility
├── pick the correct style layout
├── switch Creative/Adventure when needed
├── grant the Terra review kit
├── set class/style
├── clear and spawn test targets
├── verify third-person
├── focus the Hytale window
├── capture logs/screenshots if needed
└── prep the next style after feedback
```

## Ground Layouts

```text
stationary
└── one non-hostile dummy 1.6m ahead
    └── self buffs, single-target checks, precise aim

close
├── one grounded dummy close
└── one flying Bat offset
    └── basic projectile + airborne target checks

cluster
├── central dummy ~4m ahead
├── left/right dummies ~3m from center
├── far dummy ~7m ahead
└── near dummy ~2m ahead
    └── explosion, AoE, pull/drag, Cacti Cluster secondary spread

line
├── dummy ~4m ahead
├── dummy ~8m ahead
└── dummy ~12m ahead
    └── line/projectile penetration, debris waves, refraction/fracture lines

surround
├── front dummy
├── rear dummy
├── left dummy
└── right dummy
    └── self-centered fields, Sandstorm, Aftershock, Dust Devil drag/expel
```

All default review targets should be stationary test dummies. Hostile mobs are
only used for dedicated damage-taking checks after the arena is ready, so the
user does not get attacked before the test begins.

## Class Passive Checks

| Terra passive | Mode | Setup | Items | User action | Pass signal |
| --- | --- | --- | --- | --- | --- |
| Miner's Affinity | Adventure | stone block strip + timing lane | Iron pickaxe, wood pickaxe, iron shovel | Mine same block type with each tool | Pickaxes are faster; shovel does not get Terra pickaxe bonus |
| Subterranean Fortitude shield | Adventure | empty safe lane | shield optional | stand still for 2s, then take controlled hit | shield appears and absorbs/reduces damage |
| Low-health regen | Adventure | safe lane, no mobs unless controlled | none | get lowered below 30% HP, wait | regen starts only below threshold |
| Immovable | Adventure | controlled knockback source | shield optional | take a knockback hit | player receives reduced knockback only, no extra outgoing knockback passive |
| Cave vision | Creative then Adventure | underground/dim test pocket | none | enter low-light/underground area | visibility improves without conflicting with style effects |

## Style Plan

### 1. Terra / Metal

```text
Default setup: stationary
Mode split:
├── Creative: Iron Wall placement/visual, Metal Coat visual, Alloy visual
└── Adventure: wall collision/push, damage reduction, weapon/tool buff behavior
Items:
├── Ingredient_Bar_Iron for metal resource
├── Weapon_Sword_Iron for Alloy Enhancement
├── Tool_Pickaxe_Iron for Alloy Enhancement tool proof
└── Weapon_Shield_Iron for damage reduction/blocking comparison
```

Ability checks:

| Ability | User action | Must verify |
| --- | --- | --- |
| Iron Wall | cast facing dummy | 2x2 metal block barricade appears in front, lasts 4s, contact pushes enemies away, cooldown starts after disappearance |
| Metal Coat | cast, rotate camera | tight shiny gray coating hugs player body, no sparks needed, damage reduction works in Adventure |
| Alloy Enhancement | cast, switch from spellbook to sword/pickaxe, attack/mine | buff persists after leaving spellbook, affects physical melee/tool only, does not apply to ranged/magic |

### 2. Terra / Magma

```text
Default setup: cluster
Mode split:
├── Creative: lava ring visuals, obsidian coating, magma projectile visual
└── Adventure: burn/slow/damage, friendly safety, no caster trapping
Items:
├── Rock_Stone for stone_blocks resource
└── Build_GreyDark_Cube as obsidian/dark visual comparison
```

Ability checks:

| Ability | User action | Must verify |
| --- | --- | --- |
| Lava Pool | cast into cluster | lava/fluid ring or pool is grounded, expands outward, does not appear floating, enemies burn, caster/allies are not punished |
| Obsidian Skin | cast in third-person | lava shell appears briefly around caster, then tight dark purple/black coating remains |
| Magma Sling | aim at central dummy | slow lava-like blob/projectile travels through air, hit applies burn+slow |

### 3. Terra / Stone

```text
Default setup: stationary for Pillar Strike, line for Rubble/Rockslide if needed
Mode split:
├── Creative: stone pillar/block visuals, arm coating visual
└── Adventure: stun/launch/slow/knockback interactions
Items:
├── Rock_Stone for resource
└── Weapon_Sword_Iron if Rubble Rouser/arm coating needs attack follow-up
```

Ability checks:

| Ability | User action | Must verify |
| --- | --- | --- |
| Rubble Rouser | cast/attack target | stone coating covers arms, not just hands; projectile/impact reads as stone/rubble |
| Pillar Strike | cast on target | 1x1x3 stone pillar appears at target quickly in stacked sequence and visually launches/stuns target |
| Rockslide | cast down lane/cluster | ground/stone wave or slide hits path/area, slows, and reads as heavy stone movement |

### 4. Terra / Arbor

```text
Default setup: stationary
Mode split:
├── Creative: roots/vines/sapling placement and cleanup
└── Adventure: root, heal, reflect/target cleanup, taunt survival behavior
Items:
└── Plant_Seeds_Wheat for seeds resource
```

Ability checks:

| Ability | User action | Must verify |
| --- | --- | --- |
| Rooted | cast, stand still, rotate camera | roots/vines appear on top of ground at lower body, do not replace floor block, player is tethered/heals |
| Vines | cast on target, retarget if needed | one target at a time, old vines disappear on retarget/death, no cooldown behavior is respected |
| Sapling | aim at ground near target | projectile marks ground, sapling appears on top of ground, target is lured/taunted |

### 5. Terra / Bloom

```text
Default setup: cluster for Nightshade/Cacti, clear lane for Frolick
Mode split:
├── Creative: flower/cactus placement, trail visibility, poison visuals
└── Adventure: DoT/slow/heal/speed and enemy spread behavior
Items:
└── Plant_Seeds_Wheat for seeds resource
```

Ability checks:

| Ability | User action | Must verify |
| --- | --- | --- |
| Nightshade | fire past central dummy toward surface | projectile prioritizes surface, flower appears on top of surface, enemies within 5 blocks lure toward it, purple body-hug smoke on poison |
| Frolick | activate, then move forward/strafe in lane | flowers are placed behind player on top of ground, never replacing floor blocks or pulling player underground |
| Cacti Cluster | use cluster mob layout, hit central target | large slow cactus sticks to first target/surface, attached target gets 4s DoT+20% slow, explosion spreads DoT+slow to surrounding targets without reattaching cacti |

### 6. Terra / Self Petrification

```text
Default setup: stationary
Mode split:
├── Creative: tight stone coating and tunnel visual
└── Adventure: invulnerability/immobility, Glare slow, Tunnel resource/safety
Items:
└── Rock_Stone for stone_blocks and Tunnel fuel
```

Ability checks:

| Ability | User action | Must verify |
| --- | --- | --- |
| Gargoyle | cast, wait/cancel if available | tight stone coating, immobile/invulnerable feel, cooldown begins after end/cancel, cooldown is 6s |
| Glare | face target and cast | target gets same stone coating, release removes coating, target remains slowed 2s |
| Tunnel | cast while normal, then while Gargoyle active | player becomes single stone-block visual/proxy, controlled movement works, resource drain works, surface recovery prevents getting stuck |

### 7. Terra / Soil

```text
Default setup: line for Debris/Burrow, cluster for Mudpit
Mode split:
├── Creative: safe movement route, mud/water field visual, debris wave visual
└── Adventure: evasion/damage/knockback/slow/vulnerability and friendly safety
Items:
├── Soil_Dirt for resource
└── Tool_Shovel_Iron as non-pickaxe control if mining/passive overlap is checked
```

Ability checks:

| Ability | User action | Must verify |
| --- | --- | --- |
| Burrow | face down safe lane, cast | player visually drops, dashes 4 blocks, re-emerges, exit applies damage/knockback |
| Mudpit | cast on cluster | grounded muddy water/pool field, not floating; caster/allies are not slowed; enemies get slow/vulnerability |
| Debris | cast down line | brown smoke/debris wave travels forward, not a single dirt block, applies blind/vulnerability |

### 8. Terra / Sand

```text
Default setup: surround
Mode split:
├── Creative: beige sand cloud readability and combo visuals
└── Adventure: DoT/slow/damage-down, drag/expel, combo persistence
Items:
└── Soil_Sand for sand_blocks resource
```

Ability checks:

| Ability | User action | Must verify |
| --- | --- | --- |
| Sandstorm | cast in surround layout | beige-yellow cloud surrounds player and shows radius; does not just look like white smoke |
| Dust Devil | activate Sandstorm, then cast Dust Devil while moving forward | dash drags enemies in cloud, expels at end, and ends Sandstorm |
| Vitrification | cast before/during Sandstorm combo | shard/burn effect layers with Sandstorm/Dust Devil without canceling or hiding them |

### 9. Terra / Gem

```text
Default setup: cluster
Mode split:
├── Creative: 2x2x2 green gem cube, aura, expanding sphere visuals
└── Adventure: HP bar/recall/damage falloff/friendly safety
Items:
├── Rock_Gem_Emerald for gem resource
└── Rock_Crystal_Green_Block for visual comparison
```

Ability checks:

| Ability | User action | Must verify |
| --- | --- | --- |
| Lapidary | cast and observe object | persistent controllable floating green 2x2x2 cube/gem one block off ground, HP bar visible, recall/destroy behavior exists |
| Fracture | cast from Lapidary into cluster | fast expanding green sphere/ring from gem epicenter, does not hit caster/allies, damage falls off if designed |
| Refraction | cast with Lapidary active | bright green radius aura surrounds gem, not just caster, buff applies while active |

### 10. Terra / Quake

```text
Default setup: surround for Aftershock, stationary/cluster for Sinkhole, close for Stomp
Mode split:
├── Creative: flash/crack/brown dust visuals
└── Adventure: landing hit, slow/disorient, suffocation/buried status
Items:
└── Rock_Stone for stone_blocks resource
```

Ability checks:

| Ability | User action | Must verify |
| --- | --- | --- |
| Stomp | left-click to arm, jump, land near target | next jump is enhanced, landing creates flash+crack ring, ground AoE hits/knocks enemies |
| Aftershock | stand still intentionally, then observe/cast | 8-block spherical radius, flash/crack/tremor field, slow/disoriented/vulnerability staged correctly |
| Sinkhole | cast on target | buried look is obvious, cracks/dust mark target location, suffocation ticks, release/cleanup works |

## Per-Style Setup Commands

Preferred wrapper after Hytale has restarted into the newly installed mod build:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/prepare-terra-style-review.ps1 -StyleId metal
```

Use that same wrapper with each style id. It picks the correct target layout and
prints only the user actions for that style.

Lower-level commands, if manual override is needed:

```powershell
# Metal
powershell -ExecutionPolicy Bypass -File scripts/setup-terra-review.ps1 -StyleId metal -Mode creative -MobMode stationary

# Magma
powershell -ExecutionPolicy Bypass -File scripts/setup-terra-review.ps1 -StyleId magma -Mode creative -MobMode cluster

# Stone
powershell -ExecutionPolicy Bypass -File scripts/setup-terra-review.ps1 -StyleId stone -Mode creative -MobMode line

# Arbor
powershell -ExecutionPolicy Bypass -File scripts/setup-terra-review.ps1 -StyleId arbor -Mode creative -MobMode stationary

# Bloom
powershell -ExecutionPolicy Bypass -File scripts/setup-terra-review.ps1 -StyleId bloom -Mode creative -MobMode cluster

# Self Petrification
powershell -ExecutionPolicy Bypass -File scripts/setup-terra-review.ps1 -StyleId self_petrification -Mode creative -MobMode stationary

# Soil
powershell -ExecutionPolicy Bypass -File scripts/setup-terra-review.ps1 -StyleId soil -Mode creative -MobMode line

# Sand
powershell -ExecutionPolicy Bypass -File scripts/setup-terra-review.ps1 -StyleId sand -Mode creative -MobMode surround

# Gem
powershell -ExecutionPolicy Bypass -File scripts/setup-terra-review.ps1 -StyleId gem -Mode creative -MobMode cluster

# Quake
powershell -ExecutionPolicy Bypass -File scripts/setup-terra-review.ps1 -StyleId quake -Mode creative -MobMode surround
```

Switch to Adventure only when the visual setup is ready and the next check needs
real mechanics:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/send-dev-command.ps1 -Command "motm dev mode adventure"
```

Switch back to Creative for cleanup or the next style:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/send-dev-command.ps1 -Command "motm dev mode creative"
```

## Current Gaps To Keep Honest

```text
Controlled incoming damage
└── Still needs a safer dev command or reliable non-rushing source before
    shield/durability/passive damage proofs can be called complete.

Actual hostile AI behavior
└── Stationary dummies are correct for most review. Hostile mobs should be
    introduced only for a dedicated aggression/damage pass.

Creative vs Adventure reload
└── New /motm dev mode and /motm dev kit terra require a world/server restart
    because Hytale does not hot-reload the installed mod jar.

Manual controls
└── Final acceptance for player-facing ability use must use normal controls,
    not only /motm dev test ability.
```
