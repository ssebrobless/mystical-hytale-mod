# Class / Style / Ability Review Mockup - 2026-05-22

Use this file to review my current understanding before implementation. The **Function** column comes from the recovered original Hytale concept. The **Hytale appearance/read** column maps that concept onto verified local Hytale assets and EntityEffect paths.

```text
Review Goal
  +-- Is this what the ability should do?
  +-- Is this how the ability should look/read in Hytale?
  +-- Is anything missing, stale, or conceptually wrong?
```

## Source Priority Used For This Mockup

```text
1. Original Hytale concept: motm-hytale-extract/original-concept/MOD_DESIGN.md
2. Current corrections from user conversation
3. Verified Hytale asset plan: audits/ability-asset-plan/latest/ability-asset-plan.json
4. Current protected style JSON only as current implementation data
```

## Terra

### Metal

```text
Style read: hard polished plating, forge sparks, barrier weight
Palette:    steel gray, dark metal, pale highlights
Bridge:     Read as armor/weight: metallic sparks on cast, hard impact flash, barrier/field proxies with steel tint.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Iron Wall | Summon a wall 2 blocks taller than the player in front of you. Pushes nearby enemies away on cast. Instantly heals 10% of max HP. Wall lasts 2 seconds. 7 second cooldown. | Motion: ground mark/telegraph, delayed hit or solid line wall Cast: Block_Break_Metal_Sparks.particlespawner Travel: Block_Land_Metal_Sparks.particlespawner Impact: Block_Break_Metal_Flash.particlespawner, Mace_Signature_Shockwave.particlespawner | Log telegraph, delay, impact; capture ground mark/barrier before impact. |
| 2 | Metal Coat | Consumes 5 matching smelted ingots from inventory. Grants +20% durability for iron tier, +5% more per higher ingot tier. Lasts 3 minutes. Cannot stack with itself. 30 second cooldown after buff ends. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Block_Break_Metal_Sparks.particlespawner Travel: Block_Land_Metal_Sparks.particlespawner Impact: Block_Break_Metal_Flash.particlespawner, Mace_Signature_Shockwave.particlespawner | Log caster status/aura/channel ticks; capture third-person body effect. |
| 3 | Alloy Enhancement | Requires tool/weapon in hand. Enlarges the item, granting +20% attack range, +15% damage, and a durability shield equal to 50% of item's max durability. Buff ends when shield depletes. 20 second cooldown. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Block_Break_Metal_Sparks.particlespawner Travel: Block_Land_Metal_Sparks.particlespawner Impact: Block_Break_Metal_Flash.particlespawner, Mace_Signature_Shockwave.particlespawner | Log caster status/aura/channel ticks; capture third-person body effect. |

### Magma

```text
Style read: molten orange, black smoke, viscous heat
Palette:    orange lava, black-red smoke, amber glow
Bridge:     Use fire plus smoke, but keep motion viscous/heavy with delayed ground fields and molten proxies.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Lava Pool | Generate an outward expanding ring of lava from your feet. Cannot be used while jumping. Enemies caught are pulled with the ring. Max radius 5 blocks. Lava has normal properties but spreads at water speed. Disappears at max radius. Does not hurt caster. | Motion: ground proxy loop, radius readable, tick pulses Cast: Fire_Charge1_Fire.particlespawner Travel: Impact_Smoke.particlespawner Impact: Impact_Fire.particlespawner Loop/model: Fire_AoE_Grow.particlesystem, Model.blockymodel | Log field engage/ticks/release; capture field radius and victim status. |
| 2 | Obsidian Skin | A 3x3 lava cube encases you for 1 second, leaving you burned for 10 seconds. Grants shield equal to 70% max HP and 80% damage reduction. -10% move speed, -20% jump height while active. Shield breaks after 6 seconds. 10 second cooldown. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Fire_Charge1_Fire.particlespawner Travel: Impact_Smoke.particlespawner Impact: Impact_Fire.particlespawner Loop/model: Fire_AoE_Grow.particlesystem, Model.blockymodel | Log caster status/aura/channel ticks; capture third-person body effect. |
| 3 | Magma Sling | Fire a slow-moving lava projectile that vanishes on impact. Hit enemies are slowed by 15% and burned for 5 seconds. | Motion: visible travel path, impact burst on target side Cast: Fire_Charge1_Fire.particlespawner Travel: Impact_Smoke.particlespawner Impact: Impact_Fire.particlespawner Loop/model: Fire_AoE_Grow.particlesystem, Model.blockymodel | Log projectile role, launch, hit; capture travel line and target impact. |

### Stone

```text
Style read: slab-heavy rock impacts, falling rubble
Palette:    gray stone, charcoal, pale chips
Bridge:     Favor rubble, vertical drops, and mace-style ground hits over generic earth glow.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Rubble Rouser | Requires unarmed. Coat fists in stone for the next 5 punches. +90% knockback and grants AoE damage to punches. 5 second cooldown after punches depleted. | Motion: visible travel path, impact burst on target side Cast: Block_Break_Stone_Dust.particlespawner Travel: Block_Break_Stone_Parts.particlespawner Impact: Mace_Signature_Ground_Hit_Crack.particlespawner, Block_Break_Stone_Sparks.particlespawner | Log projectile role, launch, hit; capture travel line and target impact. |
| 2 | Pillar Strike | Stone pillars erupt beneath the 3 strongest enemies within 7 blocks. Deals 5% of their max HP and launches them airborne. Ground targets only. | Motion: ground mark/telegraph, delayed hit or solid line wall Cast: Block_Break_Stone_Dust.particlespawner Travel: Block_Break_Stone_Parts.particlespawner Impact: Mace_Signature_Ground_Hit_Crack.particlespawner, Block_Break_Stone_Sparks.particlespawner | Log telegraph, delay, impact; capture ground mark/barrier before impact. |
| 3 | Rockslide | Ground only. Dash 5 blocks in facing direction. Immune to attacks while dashing. Knocks enemies in path away. | Motion: ground proxy loop, radius readable, tick pulses Cast: Block_Break_Stone_Dust.particlespawner Travel: Block_Break_Stone_Parts.particlespawner Impact: Mace_Signature_Ground_Hit_Crack.particlespawner, Block_Break_Stone_Sparks.particlespawner | Log field engage/ticks/release; capture field radius and victim status. |

### Arbor

```text
Style read: living roots, leaves, wood growth
Palette:    leaf green, bark brown, pale growth
Bridge:     Represent roots/growth with green-tinted heal sparks, root-spirit summon/model, and low ground loops.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Rooted | Root yourself for 4 seconds. Instantly heal 20% HP, then regenerate 15% max HP per second while active. Attackers take 5% of damage dealt back. 8 second cooldown after duration ends. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Totem_Heal_Sparks_Constant.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Totem_Heal_SmokeFlat_Constant.particlespawner Loop/model: Earth_Brazier_Glow.particlespawner, Model.blockymodel | Log caster status/aura/channel ticks; capture third-person body effect. |
| 2 | Vines | Fire a projectile at full arrow speed. Hit mob is rooted by vines until killed or a new target is hit. Only one mob can be rooted at a time. | Motion: visible travel path, impact burst on target side Cast: Totem_Heal_Sparks_Constant.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Totem_Heal_SmokeFlat_Constant.particlespawner Loop/model: Earth_Brazier_Glow.particlespawner, Model.blockymodel | Log projectile role, launch, hit; capture travel line and target impact. |
| 3 | Sapling | Consume 20 matching wood (prioritizes higher tier). Fire projectile at full arrow speed. Spawns immobile tree that taunts all mobs within 5 blocks. Tree HP = 75% of caster's max HP + bonus based on wood tier. 20 second cooldown when destroyed. | Motion: summon gate/nest/rise, persistent model, action proof Cast: Totem_Heal_Sparks_Constant.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Totem_Heal_SmokeFlat_Constant.particlespawner Loop/model: Earth_Brazier_Glow.particlespawner, Model.blockymodel | Log role/model resolution and summon behavior; capture summon visible beside target. |

### Bloom

```text
Style read: floral poison, spores, vivid toxic bloom
Palette:    purple flower, dark green, sickly pink
Bridge:     Make flower powers readable as spores/toxic pollen: purple/pink tint on poison/acid particles and visible slow field.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Nightshade | Fire arrow-speed projectile. Creates a flower on impact that explodes, poisoning all mobs within 4 blocks for 5% max HP/sec for 4 seconds. 6 second cooldown. | Motion: forward fan/gaze beam from caster to target cone Cast: Acid_Sparks.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Impact_Poison.particlesystem Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner | Log facing direction and targets; capture cone/gaze from over-shoulder view. |
| 2 | Frolick | +25% speed for 5 seconds. Leaves flower path that enemies prioritize over players; mobs touching it are stunned until ability ends. Allies touching path gain speed boost. Heals 50% max HP over duration. 10 second cooldown after. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Acid_Sparks.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Impact_Poison.particlesystem Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner | Log caster status/aura/channel ticks; capture third-person body effect. |
| 3 | Cacti Cluster | Fire large slow projectile that pierces multiple enemies. Hit enemies stick to cactus and take DoT for 4 seconds, then cactus disappears and they are freed. | Motion: visible travel path, impact burst on target side Cast: Acid_Sparks.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Impact_Poison.particlesystem Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner | Log projectile role, launch, hit; capture travel line and target impact. |

### Self Petrification

```text
Style read: statue gray, stone shell, frozen gaze
Palette:    gray stone, dark slate, silver
Bridge:     Use caster body tint/model-change proof for statue form, dust at feet, and target gaze impact.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Gargoyle | Turn to stone for up to 5 seconds (press again to cancel). Instantly heal 15% HP. While active: invincible, immobile, no actions, untargetable by hostile mobs. 3 second cooldown after ending. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Block_Break_Stone_Dust.particlespawner Impact: Impact_Ice_Shockwave.particlespawner Loop/model: MOTM_Terra_Ground_Cracks, Model.blockymodel | Log caster status/aura/channel ticks; capture third-person body effect. |
| 2 | Glare | Mobs in front of you (up to 4 blocks) turn to stone for 5 seconds. Can cancel early. 3 second cooldown after ending. Released mobs are slowed 40% for 2 seconds. | Motion: forward fan/gaze beam from caster to target cone Cast: Block_Break_Stone_Dust.particlespawner Impact: Impact_Ice_Shockwave.particlespawner Loop/model: MOTM_Terra_Ground_Cracks, Model.blockymodel | Log facing direction and targets; capture cone/gaze from over-shoulder view. |
| 3 | Tunnel | Ground only. Transform into a stone block that moves freely through terrain. Consumes 1 stone per block traveled. Cannot activate with 0 stone. Reverts when out of stone. Destroys blocks at exit point to prevent getting stuck. No cooldown. | Motion: body motion trail, start/end burst, target-side impact Cast: Block_Break_Stone_Dust.particlespawner Impact: Impact_Ice_Shockwave.particlespawner Loop/model: MOTM_Terra_Ground_Cracks, Model.blockymodel | Log start/end positions and target result; capture third-person trail/form frame. |

### Soil

```text
Style read: mud, burrow trails, ruptured damp ground
Palette:    mud brown, dark soil, tan grit
Bridge:     Use dirt-substitute particles for burrow/mud trails, with low-opacity brown field loops.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Burrow | Disappear and emerge 4 blocks forward. Deals 20% max HP damage to mobs within 2 blocks on exit, knocks them back. Max 2 charges, 5 second cooldown per charge. | Motion: body motion trail, start/end burst, target-side impact Cast: Block_Break_Grass_Earth.particlesystem Travel: Block_Sprint_Stone_Dust.particlespawner Impact: Mace_Signature_Ground_Hit_Crack.particlespawner Loop/model: Block_Land_Soft_Stone.particlesystem | Log start/end positions and target result; capture third-person trail/form frame. |
| 2 | Mudpit | Burst a ring of muddy water outward (8 block radius), passes through mobs. Hit mobs are slowed 40% and take 10% more damage from all sources for 5 seconds. 10 second cooldown. | Motion: ground proxy loop, radius readable, tick pulses Cast: Block_Break_Grass_Earth.particlesystem Travel: Block_Sprint_Stone_Dust.particlespawner Impact: Mace_Signature_Ground_Hit_Crack.particlespawner Loop/model: Block_Land_Soft_Stone.particlesystem | Log field engage/ticks/release; capture field radius and victim status. |
| 3 | Debris | Throw AoE dirt projectile that travels 6 blocks, passes through all mobs. Hit enemies cannot target anything for 3 seconds and take 10% more damage for 5 seconds. 9 second cooldown. | Motion: visible travel path, impact burst on target side Cast: Block_Break_Grass_Earth.particlesystem Travel: Block_Sprint_Stone_Dust.particlespawner Impact: Mace_Signature_Ground_Hit_Crack.particlespawner Loop/model: Block_Land_Soft_Stone.particlesystem | Log projectile role, launch, hit; capture travel line and target impact. |

### Sand

```text
Style read: desert dust, sandstorm, glass heat
Palette:    sand gold, brown shadow, pale heat
Bridge:     Sand abilities need moving cloud volume, not just impact dust; use sandstorm as loop and dust on hit.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Sandstorm | Toggle: Continuous sandstorm attached to you (5 block radius). Deals 1% of your max HP/sec to mobs. Mobs: 50% slow + 15% less damage dealt. Consumes 1 sand every 2 seconds. Manual deactivate or out of sand = 5 second cooldown. | Motion: ground proxy loop, radius readable, tick pulses Cast: Block_Break_Sand_Dust.particlespawner Travel: Sand_Storm.particlespawner Impact: Block_Land_Sand_Hard.particlesystem Loop/model: Sand_Storm.particlesystem | Log field engage/ticks/release; capture field radius and victim status. |
| 2 | Dust Devil | Requires Sandstorm active. Dash 5 blocks at slight incline, dragging mobs caught in sandstorm with you. At end, knock back nearby mobs and apply sandstorm debuffs for 5 seconds. Ends Sandstorm and forces 8 second cooldown. | Motion: ground proxy loop, radius readable, tick pulses Cast: Block_Break_Sand_Dust.particlespawner Travel: Sand_Storm.particlespawner Impact: Block_Land_Sand_Hard.particlesystem Loop/model: Sand_Storm.particlesystem | Log field engage/ticks/release; capture field radius and victim status. |
| 3 | Vitrification | Transform sand into 5 glass shards floating above you. Next 5 primary attacks fire a shard at crosshair (75% arrow speed, has AoE). Hit mobs take 10% of your max HP as damage over 3 seconds. | Motion: visible travel path, impact burst on target side Cast: Block_Break_Sand_Dust.particlespawner Travel: Sand_Storm.particlespawner Impact: Block_Land_Sand_Hard.particlesystem Loop/model: Sand_Storm.particlesystem | Log projectile role, launch, hit; capture travel line and target impact. |

### Gem

```text
Style read: crystal shards, refraction, shield facets
Palette:    white crystal, purple-blue gem, pale sparkle
Bridge:     Use sharp crystal sparks plus faceted model proxies; proof should capture refraction/spark density.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Lapidary | Requires 1 emerald. Summon a floating gem that taunts hostiles in 10 blocks. Gem HP = 100% your max HP + 5% per emerald (max 3). Gem takes 50% less damage. Emeralds only consumed if gem is destroyed. Can recall gem without losing emeralds. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: MOTM_Terra_Gem_Cast Travel: Block_Run_Crystal_Sparks_Big.particlespawner Impact: MOTM_Terra_Gem_Impact Loop/model: MOTM_Terra_Gem_Field, Model.blockymodel | Log caster status/aura/channel ticks; capture third-person body effect. |
| 2 | Fracture | Requires Lapidary active. Explode the gemstone dealing 85% of its HP as damage to nearby mobs, falling to 10% at max range (7 block radius). Consumes emeralds and puts Lapidary on 15 second cooldown. | Motion: visible travel path, impact burst on target side Cast: MOTM_Terra_Gem_Cast Travel: Block_Run_Crystal_Sparks_Big.particlespawner Impact: MOTM_Terra_Gem_Impact Loop/model: MOTM_Terra_Gem_Field, Model.blockymodel | Log projectile role, launch, hit; capture travel line and target impact. |
| 3 | Refraction | Generate a glowing green aura (light source). Grants +5% durability, +10% attack damage, +5% move speed for 10 seconds. 10 second cooldown after. If Lapidary active: buffs doubled, cooldown reduced to 8 seconds. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: MOTM_Terra_Gem_Cast Travel: Block_Run_Crystal_Sparks_Big.particlespawner Impact: MOTM_Terra_Gem_Impact Loop/model: MOTM_Terra_Gem_Field, Model.blockymodel | Log caster status/aura/channel ticks; capture third-person body effect. |

### Quake

```text
Style read: heavy tremor, cracked earth, dust shockwave
Palette:    earth brown, dark soil, tan dust
Bridge:     Use stacked ground proxies: dust close to feet, crack ring at impact, brown-tinted quake loop for lingering AoE.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Stomp | Next jump creates shockwave on landing. Knocks ground mobs up and away (6 block radius), deals 5% your max HP damage. Up to 4 charges, 2 second delay between uses, 5 seconds per charge to regenerate. | Motion: arm -> jump -> landing ring/cracks Cast: MOTM_Terra_Quake_Cast Travel: Block_Break_Stone_Dust.particlespawner Impact: MOTM_Terra_Quake_Impact, Mace_Signature_Ground_Hit_Crack.particlespawner Loop/model: MOTM_Terra_Quake_Loop | Log armed jump, airborne movement, landing resolved with targets; screenshot landing ring/cracks. |
| 2 | Aftershock | Player becomes idle. Mobs in 10 blocks: 80% slow for 2 seconds then 50% slow for 5 more seconds, "Disoriented" debuff (50% chance attacks deal zero damage) for 7 seconds, and +15% damage taken from all sources. 5 second cooldown after effects end. | Motion: ground proxy loop, radius readable, tick pulses Cast: MOTM_Terra_Quake_Cast Travel: Block_Break_Stone_Dust.particlespawner Impact: MOTM_Terra_Quake_Impact, Mace_Signature_Ground_Hit_Crack.particlespawner Loop/model: MOTM_Terra_Quake_Loop | Log field engage/ticks/release; capture field radius and victim status. |
| 3 | Sinkhole | Drag up to 5 strongest ground enemies within 10 blocks underground for up to 10 seconds. They take suffocation damage while buried. Ground targets only. Press again to cancel early. 9 second cooldown. | Motion: ground mark/telegraph, delayed hit or solid line wall Cast: MOTM_Terra_Quake_Cast Travel: Block_Break_Stone_Dust.particlespawner Impact: MOTM_Terra_Quake_Impact, Mace_Signature_Ground_Hit_Crack.particlespawner Loop/model: MOTM_Terra_Quake_Loop | Log telegraph, delay, impact; capture ground mark/barrier before impact. |

## Hydro

### Icicle

```text
Style read: sharp brittle ice shards
Palette:    white-blue, deep cyan, ice white
Bridge:     Sharp projectile/line reads: crystal trail during travel, brittle ice shockwave on impact, slow/freeze loop after hit.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Frozen Needles | Rapidly fire 10 icicles (full arrow speed). Each deals 2% of your max HP as damage and slows target by 10% for 1 second. Slow stacks up to 10 times on a single target. | Motion: visible travel path, impact burst on target side Cast: Impact_Ice_Shockwave.particlespawner Travel: IceBall_Trail_Crystals.particlespawner Impact: Impact_Ice_Shockwave.particlespawner Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner, Model.blockymodel | Log projectile role, launch, hit; capture travel line and target impact. |
| 2 | Stalactite Crash | Slam a giant icicle in front of you, dealing 20% max HP damage (3 block radius). Stays planted 5 seconds, then explodes (5 blocks) slowing enemies 30% for 3s. If hit by Frozen Needle: explodes early with 7 block radius and deals 10% max HP damage. 8 second cooldown on cast. | Motion: ground mark/telegraph, delayed hit or solid line wall Cast: Impact_Ice_Shockwave.particlespawner Travel: IceBall_Trail_Crystals.particlespawner Impact: Impact_Ice_Shockwave.particlespawner Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner, Model.blockymodel | Log telegraph, delay, impact; capture ground mark/barrier before impact. |
| 3 | Skate | Gain up to 50% move speed by building momentum (ice sliding physics). No momentum loss on quick turns, only on wall collision. Lasts 30 seconds, cancel early by reactivating. Can use Frozen Needles and Stalactite Crash while active. 5 second cooldown. | Motion: body motion trail, start/end burst, target-side impact Cast: Impact_Ice_Shockwave.particlespawner Travel: IceBall_Trail_Crystals.particlespawner Impact: Impact_Ice_Shockwave.particlespawner Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner, Model.blockymodel | Log start/end positions and target result; capture third-person trail/form frame. |

### Snow

```text
Style read: soft powder drift, snow spirits, muffled cold
Palette:    snow white, muted blue-gray, pale frost
Bridge:     Snow should be softer than Icicle: use misty slow loops and frost spirit support, not only sharp impacts.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Snow Imp | Summon 3 persistent snow minions (25% of your max HP each). Attack nearest hostile within 50 blocks with melee or snowball (for air/ranged targets). Deal 5% of your max HP damage per attack, 1 attack every 2 seconds. Only explode on death (3 block radius, 5% max HP damage, 30% slow for 4s). Can manually detonate. 15 second cooldown when all imps die. | Motion: summon gate/nest/rise, persistent model, action proof Cast: Totem_Slow_SmokeFlat_Constant.particlespawner Travel: Impact_Ice_Shockwave.particlespawner Impact: Impact_Ice_Shockwave.particlespawner Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner, Model.blockymodel | Log role/model resolution and summon behavior; capture summon visible beside target. |
| 2 | Snowstorm | Create a snowstorm around you (8 block radius) that slows mob attack speed and movement speed by 50%. Heals snow minions in area. Follows you. Lasts 20 seconds, cancel early by reactivating. 10 second cooldown, reduced to 5 seconds if cancelled with at least half duration remaining. | Motion: ground proxy loop, radius readable, tick pulses Cast: Totem_Slow_SmokeFlat_Constant.particlespawner Travel: Impact_Ice_Shockwave.particlespawner Impact: Impact_Ice_Shockwave.particlespawner Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner, Model.blockymodel | Log field engage/ticks/release; capture field radius and victim status. |
| 3 | Frosty | Summon a large rideable snow golem (2x your max HP, 10% damage reduction). Moves at walk speed, taunts all hostiles nearby. Steer into mobs to deal 5% of golem's HP + knockback. Snowstorm works while riding and heals golem. Reactivate to dismount and explode for 50% of your max HP damage, slowing attack/move speed by 30% for 10s (10 block radius). 20 second cooldown. | Motion: summon gate/nest/rise, persistent model, action proof Cast: Totem_Slow_SmokeFlat_Constant.particlespawner Travel: Impact_Ice_Shockwave.particlespawner Impact: Impact_Ice_Shockwave.particlespawner Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner, Model.blockymodel | Log role/model resolution and summon behavior; capture summon visible beside target. |

### Surf

```text
Style read: bright wave motion, rushing tide
Palette:    cyan water, deep blue, pale foam
Bridge:     Wave and rush effects need visible travel direction, foam burst on impact, and movement/displacement proof.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | High Tide | Gain 20% move speed for 5 seconds. Send a wave forward that pushes enemies back 7 blocks and slows them 10% for 2 seconds. Also triggers Hydro healing passive (Tidal Restoration) without putting it on cooldown. 10 second cooldown. | Motion: visible travel path, impact burst on target side Cast: Bubbles.particlespawner Travel: Water_Bubble_Stream_Alpha.particlespawner Impact: Water_Small_Burst.particlespawner Loop/model: Water_Beam_Waves.particlespawner | Log projectile role, launch, hit; capture travel line and target impact. |
| 2 | Waverider | Glide across ground on a water trail (vanishes behind you) with free movement for 5 seconds. Activates Aqua Barrier passive without triggering its cooldown (doesn't stack if already active). Can use High Tide while active. 8 second cooldown after duration ends. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Bubbles.particlespawner Travel: Water_Bubble_Stream_Alpha.particlespawner Impact: Water_Small_Burst.particlespawner Loop/model: Water_Beam_Waves.particlespawner | Log caster status/aura/channel ticks; capture third-person body effect. |
| 3 | Riptide | Create an 8x8 water pool at target location. Pulls in all mobs for 6 seconds and makes them take 10% more damage from all sources. Cannot cancel early. 8 second cooldown after duration ends. | Motion: visible travel path, impact burst on target side Cast: Bubbles.particlespawner Travel: Water_Bubble_Stream_Alpha.particlespawner Impact: Water_Small_Burst.particlespawner Loop/model: Water_Beam_Waves.particlespawner | Log projectile role, launch, hit; capture travel line and target impact. |

### Rain

```text
Style read: falling rain, healing rainbow, splash sustain
Palette:    cool rain blue, sky blue, pale light
Bridge:     Use rain as sustained vertical motion, then water bursts/heal sparks for storm and rainbow effects.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Piercing Rain | Rain falls around you (10 block radius), follows you. Enemies take 2% of your max HP damage per second. +10% attack speed while active. Lasts 20 seconds, 10 second cooldown. Cancel early for reduced cooldown (up to 5 seconds). | Motion: ground proxy loop, radius readable, tick pulses Cast: Water_Dripping.particlespawner Travel: Water_Dripping.particlesystem Impact: Water_Small_Burst.particlespawner Loop/model: Totem_Heal_Sparks_Constant.particlespawner | Log field engage/ticks/release; capture field radius and victim status. |
| 2 | Rainbow | Automatically triggers when Piercing Rain ends. Heals player and friendlies for 5% of their own max HP per second for 10 seconds. Cannot be manually activated. Does not stack with itself. | Motion: ground proxy loop, radius readable, tick pulses Cast: Water_Dripping.particlespawner Travel: Water_Dripping.particlesystem Impact: Water_Small_Burst.particlespawner Loop/model: Totem_Heal_Sparks_Constant.particlespawner | Log field engage/ticks/release; capture field radius and victim status. |
| 3 | Splash | Fire a rain beam (10 blocks) for 5 seconds. Deals 4% of your max HP/sec to enemies (double Piercing Rain). Hitting friendlies grants them a 20% max HP bubble shield lasting 5 seconds. Cannot cancel early. 8 second cooldown after. | Motion: visible travel path, impact burst on target side Cast: Water_Dripping.particlespawner Travel: Water_Dripping.particlesystem Impact: Water_Small_Burst.particlespawner Loop/model: Totem_Heal_Sparks_Constant.particlespawner | Log projectile role, launch, hit; capture travel line and target impact. |

### Boiling

```text
Style read: steam pressure, scalding jets, hot water
Palette:    steam white, amber heat, rusty red
Bridge:     Boiling is pressure plus scalding: vertical geyser, steam/smoke, and burn/status proof.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Scald | Fire a stream of boiling water (8 blocks). Pushes enemies, hits multiple targets, applies burn for 10 seconds. | Motion: visible travel path, impact burst on target side Cast: Water_Beam_Spawn.particlespawner Travel: Water_Beam.particlespawner Impact: Water_Beam_Splash.particlespawner, Impact_Smoke.particlespawner Loop/model: Geyzer.particlesystem | Log projectile role, launch, hit; capture travel line and target impact. |
| 2 | Geyser | Become idle for 1 second. Water streams erupt beneath the 5 strongest mobs within 10 blocks, launching them airborne and applying burn for 10 seconds. | Motion: ground mark/telegraph, delayed hit or solid line wall Cast: Water_Beam_Spawn.particlespawner Travel: Water_Beam.particlespawner Impact: Water_Beam_Splash.particlespawner, Impact_Smoke.particlespawner Loop/model: Geyzer.particlesystem | Log telegraph, delay, impact; capture ground mark/barrier before impact. |
| 3 | Overheat | Burn yourself for 10 seconds (80% reduced damage, no armor damage). Gain +50% attack speed while burning. 15 second cooldown on activation. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Water_Beam_Spawn.particlespawner Travel: Water_Beam.particlespawner Impact: Water_Beam_Splash.particlespawner, Impact_Smoke.particlespawner Loop/model: Geyzer.particlesystem | Log caster status/aura/channel ticks; capture third-person body effect. |

### Vapor

```text
Style read: mist vanish, reform, soft translucence
Palette:    pale mist, blue-gray, white
Bridge:     Vapor needs vanish/reform timing: smoke at old position, smoke end at new position, defensive/status logs.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Vapor Vanish | Turn invisible for 10 seconds, appear as a floating rain block. Can fly, +50% move speed, cannot attack. Take fall damage if airborne when reverting to normal form. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Totem_Heal_SmokeFlat_Constant.particlespawner Travel: Mace_Signature_Cast_Smoke.particlespawner Impact: Mace_Signature_Cast_End_Smoke.particlespawner Loop/model: Totem_Heal_SmokeFlat_Constant.particlespawner | Log caster status/aura/channel ticks; capture third-person body effect. |
| 2 | Dispersion | Teleport dash 5 blocks in facing direction. Up to 3 charges, each takes 4 seconds to recharge. | Motion: body motion trail, start/end burst, target-side impact Cast: Totem_Heal_SmokeFlat_Constant.particlespawner Travel: Mace_Signature_Cast_Smoke.particlespawner Impact: Mace_Signature_Cast_End_Smoke.particlespawner Loop/model: Totem_Heal_SmokeFlat_Constant.particlespawner | Log start/end positions and target result; capture third-person trail/form frame. |
| 3 | Hidroses | For 10 seconds, each instance of damage has 25% chance to deal zero damage. 7 second cooldown after duration ends. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Totem_Heal_SmokeFlat_Constant.particlespawner Travel: Mace_Signature_Cast_Smoke.particlespawner Impact: Mace_Signature_Cast_End_Smoke.particlespawner Loop/model: Totem_Heal_SmokeFlat_Constant.particlespawner | Log caster status/aura/channel ticks; capture third-person body effect. |

### Iceberg

```text
Style read: large heavy ice slabs, armor, crush
Palette:    glacial blue, deep sea blue, icy white
Bridge:     Make iceberg large/heavy with slab proxy/model, freeze armor proof, and crushing impact.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Ice Cap | Create a box of ice surrounding the player for up to 20 seconds. Hitting any ice block triggers an explosion (2 block radius) that freezes enemies for 3 seconds and knocks them back 2 blocks. If 4 ice blocks are broken, the structure breaks immediately and goes on 10 second cooldown. When the structure breaks, gain +15% durability for 5 seconds. Can reactivate to end early and trigger cooldown. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Impact_Ice_Shockwave.particlespawner Travel: Block_Break_Crystal_Sparks.particlespawner Impact: Impact_Ice_Shockwave.particlespawner, Block_Break_Crystal_Parts.particlespawner Loop/model: Model.blockymodel | Log caster status/aura/channel ticks; capture third-person body effect. |
| 2 | Glacier | Triple in size for 10 seconds. Take 30% less damage from all sources while active; this stacks with Ice Cap's durability buff. Cannot be cancelled early. If you receive damage equal to or greater than 25% of your max HP during the effect, cooldown is reduced to 8 seconds instead of 10. | Motion: ground mark/telegraph, delayed hit or solid line wall Cast: Impact_Ice_Shockwave.particlespawner Travel: Block_Break_Crystal_Sparks.particlespawner Impact: Impact_Ice_Shockwave.particlespawner, Block_Break_Crystal_Parts.particlespawner Loop/model: Model.blockymodel | Log telegraph, delay, impact; capture ground mark/barrier before impact. |
| 3 | Ice Shelf | On land, become a pillar of ice for 5 seconds. All enemies within 6 blocks are frozen for 3 seconds and take damage equal to 30% of their max HP. After the freeze ends, enemies are slowed 30% for 5 seconds. Cannot be cancelled early. 12 second cooldown. | Motion: ground mark/telegraph, delayed hit or solid line wall Cast: Impact_Ice_Shockwave.particlespawner Travel: Block_Break_Crystal_Sparks.particlespawner Impact: Impact_Ice_Shockwave.particlespawner, Block_Break_Crystal_Parts.particlespawner Loop/model: Model.blockymodel | Log telegraph, delay, impact; capture ground mark/barrier before impact. |

### Saltwater

```text
Style read: deep ocean pressure, undertow, tide pool
Palette:    dark teal, navy, seafoam
Bridge:     Deep-sea pressure should pull/slow: water streams plus pressure shockwave and target displacement/status proof.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Tide Pool | Create a large water orb mount. Sit on top with free movement at +40% walk speed. Can perform any actions while riding. Cannot fly or jump, glides across ground. Requires a bucket of water in inventory (not consumed). Lasts indefinitely. If the bucket is removed while active, ends and goes on 15 second cooldown. Can reactivate to end early (15 second cooldown). Passes through mobs, slowing them 20% for 3 seconds. | Motion: ground proxy loop, radius readable, tick pulses Cast: Bubbles.particlespawner Travel: Water_Bubble_Stream_Alpha.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner Loop/model: Water_Bubble_Stream.particlesystem | Log field engage/ticks/release; capture field radius and victim status. |
| 2 | Abyssal Assist | Requires Tide Pool active. Spawn a moray eel that idles inside Tide Pool for 10 seconds. While active, mobs that Tide Pool passes through are stunned for 3 seconds, then receive Tide Pool's slow. Stunned mobs take 15% more damage from all sources until ability ends. Debuffs do not stack on the same mob. 8 second cooldown after duration ends. | Motion: single target cast flash plus target impact Cast: Bubbles.particlespawner Travel: Water_Bubble_Stream_Alpha.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner Loop/model: Water_Bubble_Stream.particlesystem | Log target acquired and combat/status result; capture target impact. |
| 3 | Rip Current | Requires Tide Pool active. Tide Pool grows 3x larger for 8 seconds. While active, mobs passed through are dragged along and cannot attack. If Abyssal Assist is active, the moray eel attacks all captured mobs (cannot fall out of Tide Pool), dealing 10% of your max HP per attack. When duration ends, all captured mobs are launched away from the player; other ability debuffs still apply to launched enemies. 8 second cooldown. | Motion: visible travel path, impact burst on target side Cast: Bubbles.particlespawner Travel: Water_Bubble_Stream_Alpha.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner Loop/model: Water_Bubble_Stream.particlesystem | Log projectile role, launch, hit; capture travel line and target impact. |

### Freshwater

```text
Style read: river motion, green-blue life, swamp summon
Palette:    fresh green-blue, river green, pale water
Bridge:     Use clean water motion and small companion/summon model; proof should show ally/support value.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Leap Frog | Next jump is 2x height and launches forward up to 5 blocks; sprinting increases distance. On landing, create a water pool (6 block radius) for 5 seconds. Enemies in the pool take 30% more damage from all sources; debuff does not stack. Up to 2 charges, 6 seconds per charge to recharge, 1 second delay between activations. | Motion: body motion trail, start/end burst, target-side impact Cast: Bubbles.particlespawner Travel: Water_Bubble_Stream_Alpha.particlespawner Impact: Water_Small_Burst.particlespawner Loop/model: Totem_Heal_Sparks_Constant.particlespawner, Model.blockymodel | Log start/end positions and target result; capture third-person trail/form frame. |
| 2 | River Rapids | Gain 60% speed boost until you travel 20 blocks while touching ground; jumping/sprinting extends duration. Every ground block touched spawns non-spreading water on top for 15 seconds. Player or friendlies passing through the water gain 40% speed buff for 2 seconds; buff does not stack. 10 second cooldown after max distance. Can reactivate to cancel early. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Bubbles.particlespawner Travel: Water_Bubble_Stream_Alpha.particlespawner Impact: Water_Small_Burst.particlespawner Loop/model: Totem_Heal_Sparks_Constant.particlespawner, Model.blockymodel | Log caster status/aura/channel ticks; capture third-person body effect. |
| 3 | Swamp Monster | Summon a friendly crocodile with 100% of your max HP. Moves 80% faster than normal and attacks deal 10% of your max HP damage. Aggressive toward hostiles, prioritizing enemies closest to you. Can be targeted by enemies. Lasts until killed or deactivated. 20 second cooldown if killed, 15 seconds if deactivated. Cannot deactivate if attacked in the last 2 seconds. | Motion: summon gate/nest/rise, persistent model, action proof Cast: Bubbles.particlespawner Travel: Water_Bubble_Stream_Alpha.particlespawner Impact: Water_Small_Burst.particlespawner Loop/model: Totem_Heal_Sparks_Constant.particlespawner, Model.blockymodel | Log role/model resolution and summon behavior; capture summon visible beside target. |

### Bilgewater

```text
Style read: dirty oil, foul water, corrosion
Palette:    olive sludge, dark green, dirty yellow
Bridge:     Bilgewater is dirty water: acid/poison tint, sluggish field, and corrosion/debuff proof.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Bilge Dump | Vomit a pool of toxic oily water (6 block radius) that persists for 12 seconds. Enemies inside are slowed 25%, take 3% of your max HP/sec as poison damage, and gain "Tarred" debuff (lasts 5 seconds after leaving pool). Tarred enemies have -20% attack speed. Can reactivate to end early. 10 second cooldown after pool ends. | Motion: forward fan/gaze beam from caster to target cone Cast: Acid_Sparks.particlespawner Travel: Water_Bubble_Stream_Alpha.particlespawner Impact: Impact_Poison.particlesystem Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner | Log facing direction and targets; capture cone/gaze from over-shoulder view. |
| 2 | Anchor Haul | Throw a heavy anchor (8 block range). First enemy hit is impaled and dragged back to you, taking 15% of your max HP as damage. If target is Tarred, nearby enemies (4 blocks) are chained to the anchor and also dragged. Chained enemies take 10% max HP damage. 8 second cooldown. | Motion: visible travel path, impact burst on target side Cast: Acid_Sparks.particlespawner Travel: Water_Bubble_Stream_Alpha.particlespawner Impact: Impact_Poison.particlesystem Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner | Log projectile role, launch, hit; capture travel line and target impact. |
| 3 | Oil Spill | Spill a pool of heavily oil-polluted water (6 block radius) for 10 seconds. Player and friendlies inside have 40% chance to take 40% less damage from each attack and gain +70% move speed. Enemies entering the pool lose directional control and slide through in their facing direction (ice physics). Cannot cancel early. All buffs/debuffs end instantly when pool disappears or is exited. 6 second cooldown after pool ends. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Acid_Sparks.particlespawner Travel: Water_Bubble_Stream_Alpha.particlespawner Impact: Impact_Poison.particlesystem Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner | Log caster status/aura/channel ticks; capture third-person body effect. |

## Aero

### Scream

```text
Style read: sonic rings, pale shockwaves, ringing air
Palette:    pale cyan, white, light blue
Bridge:     Sonic reads as pale rings: shockwave impact plus cone/facing target proof.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Shriek | Release a piercing scream in a cone (6 blocks). Enemies hit are "Deafened" for 4 seconds (cannot hear player footsteps, -50% awareness/aggro range, -10% move speed). If used in an enclosed area (caves, indoors), range is 4x larger (24 blocks) and Deafened lasts 10 seconds instead. Deals no damage. 8 second cooldown; reduced to 6 seconds if 6+ enemies are hit. | Motion: forward fan/gaze beam from caster to target cone Cast: MOTM_Aero_Scream_Cast Travel: Wind_Sparks_Tail.particlespawner Impact: MOTM_Aero_Scream_Impact Loop/model: MOTM_Aero_Scream_Field | Log facing direction and targets; capture cone/gaze from over-shoulder view. |
| 2 | Sonic Boom | Charge up to 1 second, then release a massive shockwave in all directions (8 block radius). Enemies are knocked back 5 blocks and stunned for 1 second. Deafened enemies are stunned for 5 seconds instead. If aimed downward while at least half charged, launches player upward (2 blocks at half charge, 4 blocks at full charge). Can cancel fall damage by releasing before landing. 7 second cooldown. | Motion: visible travel path, impact burst on target side Cast: MOTM_Aero_Scream_Cast Travel: Wind_Sparks_Tail.particlespawner Impact: MOTM_Aero_Scream_Impact Loop/model: MOTM_Aero_Scream_Field | Log projectile role, launch, hit; capture travel line and target impact. |
| 3 | Battle Cry | Let out a loud shout, granting yourself and all friendlies within 15 blocks +20% attack speed and +10% move speed for 10 seconds. Cannot cancel early. 8 second cooldown after duration ends. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: MOTM_Aero_Scream_Cast Travel: Wind_Sparks_Tail.particlespawner Impact: MOTM_Aero_Scream_Impact Loop/model: MOTM_Aero_Scream_Field | Log caster status/aura/channel ticks; capture third-person body effect. |

### Tornado

```text
Style read: gray funnel, spiral wind, sustained vortex
Palette:    pale gray, storm blue, white
Bridge:     Tornado must show sustained spiral/funnel and repeated pull/tick proof.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Twister | Transform into a small tornado for 5 seconds. Move 80% faster than default run speed. Drag any enemies you contact along with you. Invincible while active. Absorbs and negates all projectiles within 20 block radius. Cannot cancel early. 10 second cooldown. | Motion: ground proxy loop, radius readable, tick pulses Cast: Battleaxe_Signature_Whirlwind_Spin.particlespawner Travel: Battleaxe_Signature_Whirlwind_Sparks.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner Loop/model: Battleaxe_Signature_Whirlwind.particlesystem | Log field engage/ticks/release; capture field radius and victim status. |
| 2 | Funnel Cloud | Summon a stationary tornado at target location (up to 12 blocks away). Tornado lasts 8 seconds, pulling enemies within 6 blocks toward its center. Throws debris at enemies inside, dealing 3% of your max HP per hit. Number of debris projectiles equals the number of projectiles absorbed by your last Twister (minimum 3, maximum 15). Debris fires once per second. 12 second cooldown. | Motion: ground proxy loop, radius readable, tick pulses Cast: Battleaxe_Signature_Whirlwind_Spin.particlespawner Travel: Battleaxe_Signature_Whirlwind_Sparks.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner Loop/model: Battleaxe_Signature_Whirlwind.particlesystem | Log field engage/ticks/release; capture field radius and victim status. |
| 3 | Eye of the Storm | Activate to gain storm healing for 10 seconds. While active: standing in the center of Funnel Cloud heals 40% of your max HP over the duration. Using Twister instantly heals 20% of your max HP. 15 second cooldown. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Battleaxe_Signature_Whirlwind_Spin.particlespawner Travel: Battleaxe_Signature_Whirlwind_Sparks.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner Loop/model: Battleaxe_Signature_Whirlwind.particlesystem | Log caster status/aura/channel ticks; capture third-person body effect. |

### Jet

```text
Style read: fast golden streaks, afterburn trails
Palette:    gold, amber, white-hot highlight
Bridge:     Jet must prove motion: start burst, body trail, end impact, and before/after position log.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Jet Burst | Instantly dash 6 blocks in facing direction. Stops on enemy contact, dealing 10% of your max HP as damage and launching them 4 blocks. Can cancel fall damage if used before landing. 3 charges, 4 seconds per charge to regenerate. 1 second delay between uses. | Motion: body motion trail, start/end burst, target-side impact Cast: Sword_Signature_Ready_Sparks.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner Loop/model: Impact_Smoke.particlespawner | Log start/end positions and target result; capture third-person trail/form frame. |
| 2 | Afterburner | Your next Jet Burst travels 10 blocks instead of 6 and passes through enemies instead of stopping. Leaves a damaging trail for 3 seconds. Enemies touching the trail take 5% of your max HP/sec and are slowed 20%. If you hit 3+ enemies with the enhanced dash, refund 1 Jet Burst charge. 10 second cooldown. | Motion: body motion trail, start/end burst, target-side impact Cast: Sword_Signature_Ready_Sparks.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner Loop/model: Impact_Smoke.particlespawner | Log start/end positions and target result; capture third-person trail/form frame. |
| 3 | Mach Punch | After using Jet Burst, your next melee attack within 2 seconds deals 20% bonus damage and knocks the enemy back 4 blocks in your dash direction. If the enemy collides with a wall, they are stunned for 3 seconds and take an additional 15% of your max HP as damage. 3 second cooldown. | Motion: body motion trail, start/end burst, target-side impact Cast: Sword_Signature_Ready_Sparks.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner Loop/model: Impact_Smoke.particlespawner | Log start/end positions and target result; capture third-person trail/form frame. |

### Jump

```text
Style read: aerial arcs, launch trails, landing bursts
Palette:    gold air, pale wind, warm highlight
Bridge:     Every jump ability needs airborne displacement proof and landing/impact screenshots, not standing casts.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Leap | Your next jump launches you 3x normal height. Can be used mid-air. Knocks back enemies within 3 blocks on takeoff and upon landing. Knocked enemies take 30% more damage from all sources for 3 seconds (does not stack). Player takes no fall damage after using Leap. 2 charges, 6 seconds per charge to regenerate. | Motion: body motion trail, start/end burst, target-side impact Cast: Wind_Sparks_Tail.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner | Log start/end positions and target result; capture third-person trail/form frame. |
| 2 | Divebomb | While airborne, slam straight down at high speed. Deal 20% of your max HP as damage on impact (4 block radius). Damage increases by 5% for each block fallen (max +25% bonus at 5+ blocks). Creates a shockwave that slows enemies 30% for 5 seconds. Player takes fall damage at 90% reduction. 8 second cooldown. | Motion: body motion trail, start/end burst, target-side impact Cast: Wind_Sparks_Tail.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner | Log start/end positions and target result; capture third-person trail/form frame. |
| 3 | Hang Time | Activate while airborne to hover in place for up to 4 seconds. Can attack, use abilities, and rotate freely while hovering. Press jump to cancel and resume falling. Reactivating Hang Time mid-hover launches you slightly upward (1 block) and resets hover duration. 2 charges, 10 seconds per charge to regenerate. 2 second cooldown between consecutive uses. | Motion: body motion trail, start/end burst, target-side impact Cast: Wind_Sparks_Tail.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner | Log start/end positions and target result; capture third-person trail/form frame. |

### Wind Blade

```text
Style read: sharp cutting air, white blade trails
Palette:    white, lavender-gray, pale blue
Bridge:     Wind blade should be a visible cutting line/arc with impact proof at target side.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Air Slash | Swing your weapon to release a crescent-shaped wind blade (12 block range, full arrow speed). Passes through enemies, dealing 8% of your max HP as damage to each. Deflects any incoming projectiles in its path. Can fire without a weapon equipped (uses punch animation). 2 second cooldown. | Motion: visible travel path, impact burst on target side Cast: Wind_Sparks_Tail.particlespawner Travel: Mace_Signature_Slash_Alpha.particlespawner Impact: Mace_Signature_Slash_Bright.particlespawner | Log projectile role, launch, hit; capture travel line and target impact. |
| 2 | Gale Cutter | Fire 5 rapid wind blades in a spread pattern (10 block range). Each blade deals 5% of your max HP as damage. Each blade hit knocks the enemy back 1 block (max 5 blocks if all blades connect). 8 second cooldown. | Motion: visible travel path, impact burst on target side Cast: Wind_Sparks_Tail.particlespawner Travel: Mace_Signature_Slash_Alpha.particlespawner Impact: Mace_Signature_Slash_Bright.particlespawner | Log projectile role, launch, hit; capture travel line and target impact. |
| 3 | Razor Wind | Toggle: Your melee attacks release a small wind blade on every swing, extending attack range by 3 blocks and dealing 50% of your weapon damage as bonus wind damage. Drains stamina or costs 1% max HP per swing. No cooldown, toggle freely. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Wind_Sparks_Tail.particlespawner Travel: Mace_Signature_Slash_Alpha.particlespawner Impact: Mace_Signature_Slash_Bright.particlespawner | Log caster status/aura/channel ticks; capture third-person body effect. |

### Smoke

```text
Style read: dark smoke clouds, stealth, vanish/reform
Palette:    dark gray, black-blue, pale ash
Bridge:     Smoke needs third-person vanish/form proof plus dark cloud field, not first-person wall staring.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Smoke Bomb | Throw a smoke bomb at target location (up to 10 blocks). Creates a smoke cloud (6 block radius) for 8 seconds. Enemies inside have vision reduced to 2 blocks and lose target lock on player. Player and friendlies inside gain +20% move speed. Standing inside reduces Smoke Form cooldown 5% faster. 10 second cooldown. | Motion: ground proxy loop, radius readable, tick pulses Cast: Mace_Signature_Cast_Smoke.particlespawner Travel: Mace_Signature_Cast_End_Smoke.particlespawner Impact: Mace_Signature_Cast_End_Smoke.particlespawner Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner, Model.blockymodel | Log field engage/ticks/release; capture field radius and victim status. |
| 2 | Vanish | Instantly become invisible and leave a stationary smoke decoy at your location for 4 seconds. Enemies will target the decoy. Breaking invisibility (attacking or taking damage) causes your next attack within 2 seconds to deal 30% bonus damage. Standing near the decoy reduces Smoke Form cooldown 5% faster. 12 second cooldown. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Mace_Signature_Cast_Smoke.particlespawner Travel: Mace_Signature_Cast_End_Smoke.particlespawner Impact: Mace_Signature_Cast_End_Smoke.particlespawner Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner, Model.blockymodel | Log caster status/aura/channel ticks; capture third-person body effect. |
| 3 | Smoke Form | Transform into smoke for 5 seconds. Gain free flight and pass through enemies and mobs, but not walls. +40% move speed. Cannot attack or perform any actions while active. Enemies you pass through are unable to attack for 5 seconds (does not stack). Press ability again to end early. 15 second cooldown. | Motion: body motion trail, start/end burst, target-side impact Cast: Mace_Signature_Cast_Smoke.particlespawner Travel: Mace_Signature_Cast_End_Smoke.particlespawner Impact: Mace_Signature_Cast_End_Smoke.particlespawner Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner, Model.blockymodel | Log start/end positions and target result; capture third-person trail/form frame. |

### Gale Wizard

```text
Style read: refined magical wind, purple-tinted gusts
Palette:    lavender, purple, pale wind
Bridge:     Blend wizard-like casting with controlled wind: cast pulse, shaped gust, sustained field.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Gust | Cast a targeted wind burst at an enemy up to 15 blocks away. Deals 12% of your max HP as damage and pushes them 5 blocks in any direction you choose (aim with crosshair). Can push enemies off ledges, into hazards, or into walls. 4 second cooldown. | Motion: visible travel path, impact burst on target side Cast: Battleaxe_Signature_Whirlwind_Spin.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner Loop/model: Battleaxe_Signature_Whirlwind.particlesystem | Log projectile role, launch, hit; capture travel line and target impact. |
| 2 | Cyclone Shield | Summon a swirling wind barrier around yourself for 6 seconds. Reflects all projectiles back at attackers. Melee attackers are pushed back 3 blocks and take 5% of your max HP as damage. Can cast on a friendly instead if aimed at them. 8 second cooldown. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Battleaxe_Signature_Whirlwind_Spin.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner Loop/model: Battleaxe_Signature_Whirlwind.particlesystem | Log caster status/aura/channel ticks; capture third-person body effect. |
| 3 | Tempest | Channel for 2 seconds, then unleash a massive wind storm around yourself (8 block radius). Enemies are lifted into the air and spun for 4 seconds, unable to act. When released, they take 15% of your max HP as fall damage. Friendlies caught in the storm are launched high into the air and take no fall damage when landing. 15 second cooldown. | Motion: ground proxy loop, radius readable, tick pulses Cast: Battleaxe_Signature_Whirlwind_Spin.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Battleaxe_Bash_Shockwave.particlespawner Loop/model: Battleaxe_Signature_Whirlwind.particlesystem | Log field engage/ticks/release; capture field radius and victim status. |

### Pressure

```text
Style read: compressed air pulses, invisible-force rings
Palette:    pale blue-white, gray-blue, white
Bridge:     Pressure should be nearly invisible force with readable shock rings and knockback/displacement proof.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Air Shot | Charge up to 2 seconds, then fire a compressed air bullet (20 block range, instant travel). Minimum charge (0.5s) deals 5% of your max HP. Maximum charge (2s) deals 25% of your max HP and pierces through enemies. Damage decreases the further it travels (full damage at 5 blocks, 50% damage at max range). Uncharged shots deal no damage. No cooldown. | Motion: visible travel path, impact burst on target side Cast: Battleaxe_Bash_Shockwave.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Mace_Signature_Shockwave.particlespawner | Log projectile role, launch, hit; capture travel line and target impact. |
| 2 | Bullet Storm | Fire a rapid barrage of 12 air bullets over 2 seconds (15 block range). Each bullet deals 1% of your max HP as damage. Damage decreases with distance (full damage at 5 blocks, 50% damage at max range). Can aim freely while firing. Each hit slows enemy by 5% for 1.5 seconds (stacks up to 60%). 8 second cooldown. | Motion: visible travel path, impact burst on target side Cast: Battleaxe_Bash_Shockwave.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Mace_Signature_Shockwave.particlespawner | Log projectile role, launch, hit; capture travel line and target impact. |
| 3 | Pressure Burst | Channel for up to 5 seconds, building pressure. Release to unleash an explosion around you. Radius and damage scale with charge time: 1s = 4 blocks/15% max HP, 3s = 6 blocks/30% max HP, 5s = 8 blocks/50% max HP. Damage decreases toward edge of radius (full damage at center, 50% at max radius). All enemies hit are knocked back (distance scales with charge: 2/4/6 blocks). Cannot cancel without releasing. 10 second cooldown. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Battleaxe_Bash_Shockwave.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Mace_Signature_Shockwave.particlespawner | Log caster status/aura/channel ticks; capture third-person body effect. |

### Thunder

```text
Style read: violet-yellow lightning, crackling arcs
Palette:    violet, gold, pale flash
Bridge:     Use lightning arcs and bright sparks; proof should catch flash frame plus stun/shock state.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Thunderclap | Create a deafening crack of thunder around you (8 block radius). Deals no damage but stuns all enemies for 2 seconds. Stunned enemies are "Shocked" for 5 seconds (take 15% more damage from lightning abilities). 10 second cooldown. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Sword_Signature_Ready_Sparks.particlespawner Travel: Void_Lightning.particlespawner Impact: Void_Lightning.particlespawner Loop/model: Sword_Signature_Ready_Sparks.particlespawner, Model.blockymodel | Log caster status/aura/channel ticks; capture third-person body effect. |
| 2 | Smite | Call down a lightning bolt at target location (up to 15 blocks away). Small radius (2 blocks), deals 20% of your max HP as damage and stuns enemies hit for 3 seconds (not stackable). 2 charges, 6 seconds per charge to regenerate. | Motion: visible travel path, impact burst on target side Cast: Sword_Signature_Ready_Sparks.particlespawner Travel: Void_Lightning.particlespawner Impact: Void_Lightning.particlespawner Loop/model: Sword_Signature_Ready_Sparks.particlespawner, Model.blockymodel | Log projectile role, launch, hit; capture travel line and target impact. |
| 3 | Chain Lightning | Activate to hover and gain free flight for up to 6 seconds. Cannot attack or perform any actions while active. Chains electricity to enemies within 5 blocks as you move, dealing 10% of your max HP as damage per hit (hits once per second per enemy). Shocked enemies take 15% bonus damage and chain range extends to 7 blocks. Can cancel early. 8 second cooldown after ending. | Motion: visible travel path, impact burst on target side Cast: Sword_Signature_Ready_Sparks.particlespawner Travel: Void_Lightning.particlespawner Impact: Void_Lightning.particlespawner Loop/model: Sword_Signature_Ready_Sparks.particlespawner, Model.blockymodel | Log projectile role, launch, hit; capture travel line and target impact. |

### Pollution

```text
Style read: sickly toxic haze, acidic rain
Palette:    green-yellow, dark olive, acid highlight
Bridge:     Toxic haze should mix airborne rain/smoke with poison/acid effects and debuff proof.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Smog | Activate to leave a trail of toxic smog behind you as you move for 6 seconds. Each smog segment (6 block radius) lingers for 10 seconds before vanishing. Enemies inside take 2% of your max HP/sec as poison damage and have -25% attack speed (debuffs do not stack). Player and friendlies are immune. Cannot cancel early. 10 second cooldown after trail ends. | Motion: ground proxy loop, radius readable, tick pulses Cast: Acid_Sparks.particlespawner Travel: Water_Dripping.particlespawner Impact: Impact_Poison.particlesystem Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner | Log field engage/ticks/release; capture field radius and victim status. |
| 2 | Toxic Breath | Exhale a cone of polluted air (8 blocks long). Enemies hit are "Contaminated" for 6 seconds (take 4% max HP/sec poison damage, -15% move speed, and spread Contaminated to nearby allies on contact). 10 second cooldown. | Motion: forward fan/gaze beam from caster to target cone Cast: Acid_Sparks.particlespawner Travel: Water_Dripping.particlespawner Impact: Impact_Poison.particlesystem Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner | Log facing direction and targets; capture cone/gaze from over-shoulder view. |
| 3 | Acid Rain | Call down a rain of acidic pollution at target location (7 block radius, up to 15 blocks range) for 8 seconds. Enemies inside take 3% of your max HP/sec as damage and have armor effectiveness reduced by 30%. Contaminated enemies take double damage from Acid Rain. 13 second cooldown. | Motion: ground proxy loop, radius readable, tick pulses Cast: Acid_Sparks.particlespawner Travel: Water_Dripping.particlespawner Impact: Impact_Poison.particlesystem Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner | Log field engage/ticks/release; capture field radius and victim status. |

## Corruptus

### Necro

```text
Style read: undead wisps, drain, grave rise
Palette:    violet, black, sickly magenta
Bridge:     Necro should drain/raise: void smoke, undead model proxy, and life-steal/summon proof.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Raise Dead | Cost: 5 souls to summon, +5 souls if skeleton is killed (10 total if lost). Summon a skeleton minion (20% of your max HP) that lasts until killed. Skeleton attacks nearest enemy, dealing 5% of your max HP per hit. Prioritizes Death Marked targets. When skeleton dies, explodes dealing 10% of your max HP to nearby enemies. Max 3 skeletons active at a time. | Motion: summon gate/nest/rise, persistent model, action proof Cast: Void_Sparks.particlespawner Travel: VoidSmoke_Impact.particlespawner Impact: VoidImpact.particlesystem Loop/model: VoidSmoke_Impact.particlespawner, Model.blockymodel | Log role/model resolution and summon behavior; capture summon visible beside target. |
| 2 | Life Drain | Cost: 3 souls. Create a draining aura around you (6 block radius) for 7 seconds. Enemies inside take 5% of your max HP/sec as damage. Heals you and all friendlies inside based on damage dealt. Skeleton minions are healed regardless of being inside the radius. 10 second cooldown after duration ends. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Void_Sparks.particlespawner Travel: VoidSmoke_Impact.particlespawner Impact: VoidImpact.particlesystem Loop/model: VoidSmoke_Impact.particlespawner, Model.blockymodel | Log caster status/aura/channel ticks; capture third-person body effect. |
| 3 | Death Mark | Cost: 5 souls. Mark an enemy for 10 seconds. Marked enemy takes 25% more damage from all sources. If marked enemy dies, refund 5 souls. Only one mark active at a time. | Motion: single target cast flash plus target impact Cast: Void_Sparks.particlespawner Travel: VoidSmoke_Impact.particlespawner Impact: VoidImpact.particlesystem Loop/model: VoidSmoke_Impact.particlespawner, Model.blockymodel | Log target acquired and combat/status result; capture target impact. |

### Flame

```text
Style read: red-orange fire, explosive burn
Palette:    orange-red, dark red, amber
Bridge:     Fire needs bright cast, smoke trail, impact flame, burn/status proof.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Fireball | Hurl a fireball at target location (15 block range). Explodes on impact dealing 5% of your max HP as damage (4 block radius). Enemies hit are burned for 3% max HP/sec for 5 seconds (burn stacks). 3 charges, 6 seconds per charge to regenerate. | Motion: visible travel path, impact burst on target side Cast: Fire_Charge1_Fire.particlespawner Travel: Impact_Smoke.particlespawner Impact: Impact_Fire.particlespawner Loop/model: Fire_AoE_Grow.particlesystem | Log projectile role, launch, hit; capture travel line and target impact. |
| 2 | Ignite | Set yourself ablaze for 8 seconds. Enemies within 4 blocks take 4% of your max HP/sec as damage and are burned (burn stacks). You take 3% of your max HP/sec as self-damage (reduced by Flame Warden passive, self-damage stacks). Your attacks deal 15% bonus fire damage while active. 2 charges, 10 seconds per charge to regenerate. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Fire_Charge1_Fire.particlespawner Travel: Impact_Smoke.particlespawner Impact: Impact_Fire.particlespawner Loop/model: Fire_AoE_Grow.particlesystem | Log caster status/aura/channel ticks; capture third-person body effect. |
| 3 | Combust | Target a burning enemy within 10 blocks. Instantly consume all burn effects, dealing 200% of remaining burn damage as immediate burst damage. If this kills the enemy, nearby enemies (5 blocks) are ignited for 5 seconds. 12 second cooldown. | Motion: single target cast flash plus target impact Cast: Fire_Charge1_Fire.particlespawner Travel: Impact_Smoke.particlespawner Impact: Impact_Fire.particlespawner Loop/model: Fire_AoE_Grow.particlesystem | Log target acquired and combat/status result; capture target impact. |

### Hell Flame

```text
Style read: brutal hellfire, self-scorch, infernal ground
Palette:    red-orange, black-red, hot amber
Bridge:     Hell flame should be harsher than Flame: darker tint, self-cost proof, and persistent infernal ground.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Hellfire | Unleash a wave of dark red flames in a cone (8 blocks). Enemies hit are afflicted with "Hellburn" for 5 seconds (2% max HP/sec damage, -20% attack damage dealt, -15% move speed). Hellburn stacks up to 3 times. You take 2% of your max HP as self-damage on cast. 7 second cooldown. | Motion: visible travel path, impact burst on target side Cast: Fire_Charge1_Fire.particlespawner Travel: Impact_Smoke.particlespawner Impact: Impact_Fire.particlespawner Loop/model: Fire_AoE_Grow.particlesystem, Model.blockymodel | Log projectile role, launch, hit; capture travel line and target impact. |
| 2 | Infernal Ground | Ignite the ground around you (7 block radius) with crimson fire for 10 seconds. Enemies inside have Hellburn applied every 2 seconds and cannot regenerate health. You take 2% of your max HP/sec while standing in your own fire but gain +20% attack speed. 14 second cooldown after duration ends. | Motion: ground proxy loop, radius readable, tick pulses Cast: Fire_Charge1_Fire.particlespawner Travel: Impact_Smoke.particlespawner Impact: Impact_Fire.particlespawner Loop/model: Fire_AoE_Grow.particlesystem, Model.blockymodel | Log field engage/ticks/release; capture field radius and victim status. |
| 3 | Soul Scorch | Mark yourself with hellfire for 6 seconds, taking 5% of your max HP/sec as self-damage. All enemies within 10 blocks are "Cursed" (deal 30% less damage, take 20% more damage from all sources, abilities cost double cooldown/resources). Curse ends when Soul Scorch ends. Cannot cancel early. 14 second cooldown. | Motion: single target cast flash plus target impact Cast: Fire_Charge1_Fire.particlespawner Travel: Impact_Smoke.particlespawner Impact: Impact_Fire.particlespawner Loop/model: Fire_AoE_Grow.particlesystem, Model.blockymodel | Log target acquired and combat/status result; capture target impact. |

### Shadow

```text
Style read: inky stealth, clone, dark zones
Palette:    black-purple, dark indigo, muted violet
Bridge:     Shadow needs clone/stealth proof plus dark zone visuals and target-side hit proof.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Shadow Step | Teleport up to 10 blocks in facing direction, leaving a shadow clone at your starting position for 4 seconds. Clone mimics all player actions. Clone explodes when destroyed or expires, dealing 10% of your max HP as damage. 2 charges, 8 seconds per charge to regenerate. | Motion: body motion trail, start/end burst, target-side impact Cast: Mace_Signature_Cast_Smoke.particlespawner Travel: Void_Sparks.particlespawner Impact: VoidSplash.particlespawner Loop/model: VoidSmoke_Impact.particlespawner, Model.blockymodel | Log start/end positions and target result; capture third-person trail/form frame. |
| 2 | Umbral Veil | Cloak yourself in shadow for 6 seconds. Gain +30% move speed, become semi-invisible (harder to detect), and all cooldowns are reduced by 20% while active. Your next attack from stealth deals 40% bonus damage and applies "Blinded" (50% miss chance for 3 seconds). Attacking or taking damage ends the veil early. 12 second cooldown. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Mace_Signature_Cast_Smoke.particlespawner Travel: Void_Sparks.particlespawner Impact: VoidSplash.particlespawner Loop/model: VoidSmoke_Impact.particlespawner, Model.blockymodel | Log caster status/aura/channel ticks; capture third-person body effect. |
| 3 | Dark Embrace | Surround yourself with a shadow aura (5 block radius) for 8 seconds. Enemies inside deal 25% less damage and have their vision reduced to 2 blocks. You have 25% chance to dodge any attack while inside. 15 second cooldown after duration ends. | Motion: ground proxy loop, radius readable, tick pulses Cast: Mace_Signature_Cast_Smoke.particlespawner Travel: Void_Sparks.particlespawner Impact: VoidSplash.particlespawner Loop/model: VoidSmoke_Impact.particlespawner, Model.blockymodel | Log field engage/ticks/release; capture field radius and victim status. |

### Mentokinesis

```text
Style read: psychic violet, mind control, gaze
Palette:    psychic purple, dark violet, pale pink
Bridge:     Psychic abilities need gaze/control proof; use eye/void sparks, but mark portal assets for showcase review before final visual PASS.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Dominate | Take control of target enemy within 12 blocks for 7 seconds. Dominated enemy doubles in size, deals 200% of its normal damage, and moves 80% faster. Fights for you, attacking its former allies. When domination ends, enemy is stunned for 2 seconds. Cannot dominate bosses. 12 second cooldown. | Motion: forward fan/gaze beam from caster to target cone Cast: Void_Sparks.particlespawner Travel: MagicPortal_VoidSparks.particlespawner Impact: MagicPortal_VoidFlash.particlespawner Loop/model: MagicPortal_VoidWaves.particlespawner, Model.blockymodel | Log facing direction and targets; capture cone/gaze from over-shoulder view. |
| 2 | Mind Shatter | Target a dominated or friendly minion within 15 blocks. Triggers an explosion dealing 80% of the target's current HP as damage to all enemies within 6 blocks. Does not kill friendly minions (only triggers explosion). Applies "Confused" to survivors (attack random targets for 4 seconds). 10 second cooldown. | Motion: visible travel path, impact burst on target side Cast: Void_Sparks.particlespawner Travel: MagicPortal_VoidSparks.particlespawner Impact: MagicPortal_VoidFlash.particlespawner Loop/model: MagicPortal_VoidWaves.particlespawner, Model.blockymodel | Log projectile role, launch, hit; capture travel line and target impact. |
| 3 | Hivemind | For 10 seconds, all dominated enemies and friendly minions within 20 blocks gain +30% attack speed, +20% move speed, and share damage taken (damage split evenly among all linked units). You can have up to 2 additional Dominated enemies active during Hivemind. 18 second cooldown. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Void_Sparks.particlespawner Travel: MagicPortal_VoidSparks.particlespawner Impact: MagicPortal_VoidFlash.particlespawner Loop/model: MagicPortal_VoidWaves.particlespawner, Model.blockymodel | Log caster status/aura/channel ticks; capture third-person body effect. |

### Imbuement

```text
Style read: dark body enchantment, saturated arcane buffs
Palette:    arcane purple, black, pale violet
Bridge:     Imbuement is body enchantment: third-person aura/tint and outgoing empowered hit proof.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Imbue: Power | Cost: 25 mana. For 12 seconds, gain +40% attack damage, +20% attack speed, and +15% melee range. Can stack with other Imbue effects. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Void_Sparks.particlespawner Impact: VoidSplash.particlespawner Loop/model: VoidSmoke_Impact.particlespawner | Log caster status/aura/channel ticks; capture third-person body effect. |
| 2 | Imbue: Fortitude | Cost: 30 mana. For 12 seconds, gain +35% damage reduction, +25% max HP (current HP scales proportionally), and immunity to knockback. Can stack with other Imbue effects. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Void_Sparks.particlespawner Impact: VoidSplash.particlespawner Loop/model: VoidSmoke_Impact.particlespawner | Log caster status/aura/channel ticks; capture third-person body effect. |
| 3 | Imbue: Swiftness | Cost: 20 mana. For 12 seconds, gain +50% move speed, +30% dodge chance, and +3 extra jumps. Can stack with other Imbue effects. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Void_Sparks.particlespawner Impact: VoidSplash.particlespawner Loop/model: VoidSmoke_Impact.particlespawner | Log caster status/aura/channel ticks; capture third-person body effect. |

### Attonement

```text
Style read: corruption-cleansing golden holy break
Palette:    gold-white, warm tan, clean white
Bridge:     Atonement should look cleansing/golden, with heal/cleanse/protection proof rather than story/codex framing.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Sanctuary | Create a healing aura around you (8 block radius) for 10 seconds. Friendlies inside regenerate 4% of their max HP per second and are cleansed of one debuff every 2 seconds. You take 15% of all damage dealt to friendlies inside. Your move speed is reduced by 25% while active (does not stack with Absorb). 12 second cooldown after duration ends. | Motion: ground proxy loop, radius readable, tick pulses Cast: Totem_Heal_Sparks_Constant.particlespawner Impact: Totem_Heal_SmokeFlat_Constant.particlespawner Loop/model: Totem_Heal_Sparks_Constant.particlespawner | Log field engage/ticks/release; capture field radius and victim status. |
| 2 | Absorb | Toggle: Redirect 50% of all damage dealt to friendlies within 12 blocks to yourself. Every 100 HP of damage absorbed increases your max HP by 5% (stacks up to 50% bonus max HP). Bonus max HP decays by 5% every 10 seconds when Absorb is toggled off. Your move speed is reduced by 25% while active (does not stack with Sanctuary). No cooldown, toggle freely. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Totem_Heal_Sparks_Constant.particlespawner Impact: Totem_Heal_SmokeFlat_Constant.particlespawner Loop/model: Totem_Heal_Sparks_Constant.particlespawner | Log caster status/aura/channel ticks; capture third-person body effect. |
| 3 | Purify | Instantly cleanse all debuffs from yourself and all friendlies within 15 blocks. Grant +20% damage reduction and +15% healing received for 8 seconds. Heal all affected targets for 2% of their max HP per debuff cleansed. 12 second cooldown. | Motion: third-person body aura/tint, HUD/status proof, channel pulses Cast: Totem_Heal_Sparks_Constant.particlespawner Impact: Totem_Heal_SmokeFlat_Constant.particlespawner Loop/model: Totem_Heal_Sparks_Constant.particlespawner | Log caster status/aura/channel ticks; capture third-person body effect. |

### Void

```text
Style read: cosmic void, rift, consuming darkness
Palette:    deep purple, black, violet highlight
Bridge:     Void needs rift/consumption: portal-like field with pulling/status proof; portal assets require in-game showcase review before final.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Rift | Tear open a void rift at target location (up to 15 blocks). Rift lasts 8 seconds, pulling enemies within 6 blocks toward its center. Enemies inside the rift take 4% of your max HP/sec as void damage (ignores armor). 10 second cooldown. | Motion: ground proxy loop, radius readable, tick pulses Cast: Void_Sparks.particlespawner Travel: MagicPortal_VoidSparks.particlespawner Impact: VoidImpact.particlesystem Loop/model: MagicPortal_VoidWaves.particlespawner, Model.blockymodel | Log field engage/ticks/release; capture field radius and victim status. |
| 2 | Void Spawn | Summon a Void Crawler with a glowing purple hue for 15 seconds. Crawler has 75% of your max HP, deals 8% of your max HP per attack, and chases targets. Attacks apply "Void Touch" (3% max HP/sec for 4 seconds, ignores armor). 2 charges, 13 seconds per charge to regenerate. | Motion: summon gate/nest/rise, persistent model, action proof Cast: Void_Sparks.particlespawner Travel: MagicPortal_VoidSparks.particlespawner Impact: VoidImpact.particlesystem Loop/model: MagicPortal_VoidWaves.particlespawner, Model.blockymodel | Log role/model resolution and summon behavior; capture summon visible beside target. |
| 3 | Consume | Mark an area (7 block radius) for 2 seconds, then erase everything inside into the void. Enemies caught take 30% of your max HP as void damage and are "Banished" for 3 seconds (removed from reality - cannot act, take damage, or be targeted). When Banished ends, enemies reappear stunned for 1 second. 16 second cooldown. | Motion: single target cast flash plus target impact Cast: Void_Sparks.particlespawner Travel: MagicPortal_VoidSparks.particlespawner Impact: VoidImpact.particlesystem Loop/model: MagicPortal_VoidWaves.particlespawner, Model.blockymodel | Log target acquired and combat/status result; capture target impact. |

### Scarak

```text
Style read: insect swarm, brood nest, chitin
Palette:    green-brown, dark shell, dull gold
Bridge:     Scarak needs swarm/brood proof: acid/poison visuals, Scarak model role, nest/summon behavior.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Scarak Egg | Place an egg at your feet that hatches after 3 seconds into a random Scarak minion (Stinger, Crawler, or Spitter). Minion has 30% of your max HP and deals 5% of your max HP per attack. Lasts until killed. Enemies prioritize attacking eggs over all other targets. 10 charges, 5 seconds per charge to regenerate. Max 8 Scarak minions active at once. | Motion: summon gate/nest/rise, persistent model, action proof Cast: Acid_Sparks.particlespawner Travel: Impact_Poison.particlesystem Impact: Impact_Poison.particlesystem Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner, Model.blockymodel | Log role/model resolution and summon behavior; capture summon visible beside target. |
| 2 | Brood Surge | Instantly hatch all placed eggs and enrage all active Scarak minions for 8 seconds. Enraged minions gain +50% attack speed, +30% move speed, and explode on death dealing 10% of your max HP to nearby enemies. 15 second cooldown. | Motion: summon gate/nest/rise, persistent model, action proof Cast: Acid_Sparks.particlespawner Travel: Impact_Poison.particlesystem Impact: Impact_Poison.particlesystem Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner, Model.blockymodel | Log role/model resolution and summon behavior; capture summon visible beside target. |
| 3 | Locust Queen | Drop a giant egg that hatches after 2 seconds into an enlarged mountable Scarak Locust. Locust has 150% of your max HP, fires rapid projectiles (150% arrow speed), and prioritizes air enemies. While mounted, gain +40% move speed and can perform short flight bursts. Lasts until killed or dismounted. 25 second cooldown after locust dies or is dismissed. | Motion: summon gate/nest/rise, persistent model, action proof Cast: Acid_Sparks.particlespawner Travel: Impact_Poison.particlesystem Impact: Impact_Poison.particlesystem Loop/model: Totem_Slow_SmokeFlat_Constant.particlespawner, Model.blockymodel | Log role/model resolution and summon behavior; capture summon visible beside target. |

### Primordial

```text
Style read: ancient beast transformations, primal roar
Palette:    earthy brown, dark hide, orange accent
Bridge:     Primordial is form-based: each ability needs the right beast model plus movement/roar/impact proof.
```

| Slot | Ability | Function | Hytale appearance/read | Proof we need |
| ---: | --- | --- | --- | --- |
| 1 | Pterodactyl Form | Transform into a Pterodactyl for up to 30 seconds. Gain free flight and +60% move speed. Primary attack: Dive strike dealing 10% of your max HP (must be airborne). Secondary attack: Swoop down and grab a target, carrying them for up to 4 seconds (can drop from height for fall damage). Press again to end transformation early. 20 second cooldown after form ends. | Motion: body motion trail, start/end burst, target-side impact Cast: Battleaxe_Bash_Shockwave.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Mace_Signature_Shockwave.particlespawner Loop/model: Model.blockymodel, Model.blockymodel, Model.blockymodel | Log start/end positions and target result; capture third-person trail/form frame. |
| 2 | Triceratops Form / Mosasaurus Form | On land: Transform into a Triceratops for up to 30 seconds. Gain +50% damage reduction, -30% move speed. Primary attack: Gore dealing 12% of your max HP with knockback. Secondary attack: Charge forward up to 12 blocks, dealing 25% of your max HP to all enemies hit and stunning them for 2 seconds. In water: Transform into a Mosasaurus instead. Gain +100% swim speed, unlimited breath. Primary attack: Bite dealing 15% of your max HP. Secondary attack: Lunge 10 blocks, swallowing smaller enemies whole (instant kill on enemies below 25% HP). Press again to end transformation early. 25 second cooldown after form ends. | Motion: body motion trail, start/end burst, target-side impact Cast: Battleaxe_Bash_Shockwave.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Mace_Signature_Shockwave.particlespawner Loop/model: Model.blockymodel, Model.blockymodel, Model.blockymodel | Log start/end positions and target result; capture third-person trail/form frame. |
| 3 | T-Rex Form | Transform into a T-Rex for up to 20 seconds. Gain +25% size, -20% move speed. Primary attack: Bite dealing 20% of your max HP (slow attack speed). Secondary attack: Roar that fears all enemies within 10 blocks for 4 seconds (flee in terror) and reduces their damage by 30% for 8 seconds. Press again to end transformation early. 30 second cooldown after form ends. | Motion: body motion trail, start/end burst, target-side impact Cast: Battleaxe_Bash_Shockwave.particlespawner Travel: Wind_Sparks_Tail.particlespawner Impact: Mace_Signature_Shockwave.particlespawner Loop/model: Model.blockymodel, Model.blockymodel, Model.blockymodel | Log start/end positions and target result; capture third-person trail/form frame. |

## Review Notes

- Hidroses from the original Hydro/Vapor concept currently maps to Hidrosis in the active mod data.
- Triceratops Form / Mosasaurus Form from the original Corruptus/Primordial concept currently maps to Triceratops Form; the water-form branch is not represented in the active ability name.
- The current mod descriptions are compressed for all 120 abilities, so this mockup should be reviewed before using current JSON descriptions as final player-facing Spellbook text.
