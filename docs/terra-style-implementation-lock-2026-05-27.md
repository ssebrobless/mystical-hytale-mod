# Terra Style Implementation Lock - 2026-05-27

This file records the Terra decisions that are now treated as the implementation
target after the user visual/functionality grill. It is intentionally concrete:
future passes should compare runtime behavior to this document before changing
Terra style abilities.

## Global Terra Rules

- Terra abilities have no resource costs. Balance uses cooldowns, durations,
  charges, toggles, item binding, and field lifetimes.
- Temporary ability blocks, flowers, roots, fluids, statues, and markers must
  restore without dropping resources or materials.
- Ground markers should be placed on top of existing surfaces, not by replacing
  the support block.
- Caster and allies must not be punished by friendly Terra fields.
- Projectiles should follow the crosshair/aim line and should despawn on impact
  unless the concept explicitly creates a persistent marker.

## Locked Terra Concepts

| Style | Ability | Locked behavior |
| --- | --- | --- |
| Quake | Stomp | Arm next jump. Landing creates one central 3 block crack/flash impact, deals 6% max HP, slows, and knocks enemies away from the landing point. |
| Quake | Aftershock | 8 block spherical pulse, ground crack/shockwave visual, 5% max HP damage, slow, and outward knockback. Aerial targets may receive body clarity VFX. |
| Quake | Sinkhole | Target appears waist-deep buried, rooted/immobile for 3 seconds, small cracks and light brown dust near body, 2% max HP suffocation per second, vulnerability while buried. |
| Metal | Iron Wall | Grounded 3x4 wall in front of player, metal block look, pushes enemies out of spawn overlap, lasts 4 seconds. |
| Metal | Metal Coat | Dark shiny metal coating, 50% damage reduction, 8 second duration, can cancel early, cooldown after end/cancel. |
| Metal | Alloy Enhancement | Next eligible physical melee weapon/tool binds to first item used. Three no-durability uses, 30% damage/mining bonus, ends on item swap or uses exhausted. |
| Magma | Lava Pool | Caster-centered visible lava pool, radius 5, lasts 6 seconds, 2% max HP burn ticks on enemies, caster/allies protected from lava damage/fire/slow. |
| Magma | Obsidian Skin | 1.5 second 1x1x2 lava tower root phase, then midnight purple-black obsidian coating for 6 seconds, 20% max HP shield, damage reduction, 10 second cooldown. |
| Magma | Magma Sling | Small molten projectile/blob, crosshair aimed, 8% max HP direct hit, burn, 2 block splash with reduced effect. |
| Stone | Rubble Rouser | Full body stone coating, next unarmed hit only, 10% max HP primary hit, 4 block splash for 5% max HP, slow, 9 second cooldown. |
| Stone | Pillar Strike | Fast 1x1x4 stone pillar under target/point, launch/stun, cleanup 0.6s after full height, 8 second cooldown. |
| Stone | Rockslide | 5 block dash, dirt/stone debris underfoot, push enemies in path, 4% max HP damage, slow. |
| Arbor | Rooted | Root caster for 5 seconds with protected roots on top of ground, heal 10%, regen while active, 6 second cooldown after end. |
| Arbor | Vines | One target at a time, no cooldown, root 5 seconds, 1.5% max HP DoT, tether visual to caster, recast releases previous target. |
| Arbor | Sapling | Ground-marker projectile, uses `Furniture_Temple_Emerald_Statue` with pink glow, lures 5 block radius, 8 second marker duration. |
| Bloom | Nightshade | Ground-marker projectile, uses `Plant_Flower_Tall_Red`, pink/purple poison glow/smoke, lures 5 blocks, poison/slow burst. |
| Bloom | Frolick | 10 second movement buff/heal, flowers placed behind player on top of blocks, mixed flower assets, no drops, vanish on end/cancel. |
| Bloom | Cacti Cluster | Uses `Plant_Cactus_Ball_1`, sticks to first target/surface, 4 second DoT/slow, then radius 4 spread DoT/slow without new cactus attachments. |
| Self Petrification | Gargoyle | Full statue lock using `Furniture_Ancient_Statue` marker plus stone coating, immobile/untargetable intent, heal 35%, 7 second cooldown, tunnel can be used during form. |
| Self Petrification | Glare | Single-target gaze, target becomes `Furniture_Temple_Light_Statue`, 2 charges, 2.5 second stun, flying target should drop, 2 second slow after release. |
| Self Petrification | Tunnel | Stone/block movement fantasy with safe exit to cave pocket or surface; can be used from Gargoyle and return to Gargoyle if still active. |
| Soil | Burrow | 4 block forward whack-a-mole dash, no path damage, exit radius 3 burst, 7% max HP damage, knockback. |
| Soil | Debris | 5 wide by 6 tall brown dirt/stone dust wave from caster, travels 10 blocks, passes through enemies, blinds 2s and vulnerability 5s. |
| Soil | Mud Pit | Brown water/mud pool radius 5, lasts 6s, caster/allies not slowed, enemies slowed/vulnerable and take 1% max HP ticks. |
| Sand | Sandstorm | 10 second toggleable mini sand tornado around caster, radius 5, height 4, sand particles, enemies take 1% max HP/sec, slow 30%, vulnerability 10%. |
| Sand | Dust Devil | Requires active Sandstorm, dashes with tornado, drags/contacts enemies, final expel consumes Sandstorm, 5% burst, slow/vulnerability. |
| Sand | Vitrification | 8 second self-buff, upgrades Sandstorm/Dust Devil with glass-sand visual and stronger follow-up damage/vulnerability. |
| Gem | Lapidary | Persistent floating 2x2x2 green gem cube, HP bar/proxy, 80% caster max HP, recall/despawn support object. |
| Gem | Fracture | Requires/uses gem epicenter, radius 20 and height 12 green crystal explosion, 40% max HP damage, slow/vulnerability. |
| Gem | Refraction | Toggle aura around gem, radius 20 and height 12, allies get 40% damage reduction, 5% damage buff, 2% max HP healing per second. |

## Verified Asset Names

- `Furniture_Temple_Emerald_Statue`
- `Furniture_Ancient_Statue`
- `Furniture_Temple_Light_Statue`
- `Plant_Flower_Tall_Red`
- `Plant_Flower_Common_Poisoned2`
- `Plant_Flower_Common_White2`
- `Plant_Flower_Common_Yellow2`
- `Plant_Flower_Common_Violet`
- `Plant_Cactus_Ball_1`
- `Sand_Storm`
- `Block_Sprint_Sand`
- `Block_Break_Sand`
- `Block_Break_Crystal`
- `GreenOrbCrystals`

## Current Implementation Notes

- `GameplayPlaybackManager` owns the Terra visual/mechanical runtime seams.
- `scripts/audit-terra-implementation.ps1` now asserts the locked Terra data,
  runtime asset strings, no-resource behavior, and key runtime branches.
- Some visual fidelity still requires live review because server truth can prove
  placement/status/damage, but not whether a VFX is aesthetically ideal.
