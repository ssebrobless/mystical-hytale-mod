# Primitive Audit - 2026-05-31

Scope: all authored style abilities, class passives, and perk data in the current checkout.

Primary source files:

- `src/main/resources/data/styles/*_styles.json`
- `src/main/resources/data/classes/*.json`
- `src/main/resources/data/perks/*_perks.json`
- `src/main/java/com/motm/manager/GameplayPlaybackManager.java`
- `src/main/java/com/motm/manager/ClassPassiveManager.java`
- `src/main/java/com/motm/manager/PerkManager.java`
- `src/main/java/com/motm/manager/PlayerStatModifierManager.java`
- `src/main/java/com/motm/util/DataLoader.java`
- `src/main/java/com/motm/util/HytaleAssetResolver.java`

## Current Shape

```text
╔════════════════════════════════════════════════════════════════════╗
║                         AUTHORED CONTENT                          ║
╠══════════════════╦═════════════════════════════════════════════════╣
║ Style abilities  ║ 120 abilities across 40 styles                 ║
║ Class passives   ║ 4 class identities                             ║
║ Perks            ║ 800 authored perks, 560 loaded after tier >14   ║
║                  ║ filter, 240 currently filtered out             ║
╚══════════════════╩═════════════════════════════════════════════════╝
             │
             ▼
╔════════════════════════════════════════════════════════════════════╗
║                         LIVE RUNTIME                              ║
╠══════════════════╦═════════════════════════════════════════════════╣
║ Abilities        ║ One large generic manager handles casts,        ║
║                  ║ projectiles, fields, pulls, forms, summons,     ║
║                  ║ damage, status, healing, shields, visuals       ║
╠══════════════════╬═════════════════════════════════════════════════╣
║ Passives         ║ ClassPassiveManager implements several real     ║
║                  ║ stat/resource/passive behaviors                ║
╠══════════════════╬═════════════════════════════════════════════════╣
║ Perks            ║ Selection pipeline exists; most effect types    ║
║                  ║ are not translated into runtime behavior        ║
╚══════════════════╩═════════════════════════════════════════════════╝
```

## Ability Primitive Counts

Authored cast type counts:

```text
self_buff         26    ground_zone        18    projectile          9
summon             8    dash                6    ground_strike       4
projectile_line    4    projectile_volley   4    transformation      4
cone               4    line_control        3    self_burst          3
projectile_burst   2    ground_burst        2    curse               2
gaze               2    support_zone        2    barrier             2
execute            2    wave_line           2    others              8
```

Top authored effect tokens:

```text
knockback 10   stun 8   summon 7   evasion 5   burn 5   slow 5
dot 4          dot+slow 4          defense_buff 4       attack_buff 4
heal 4         shield 3            vulnerability 3
```

These counts are important because they show where shared primitives give the highest payoff. A summon primitive immediately affects 9 abilities with summon names. A field/zone primitive affects 24 abilities. A pull/tether primitive affects the exact abilities the player is worried about: Saltwater `rip_current`, Surf `riptide`, Arbor `vines`, Void `rift`, Bilgewater `anchor_haul`, and chain lightning style linking.

## Primitive Coverage Map

```text
╔══════════════════════╦════════════════════════════╦════════════════════════════╗
║ Primitive            ║ Current State              ║ Audit Verdict              ║
╠══════════════════════╬════════════════════════════╬════════════════════════════╣
║ Projectile/line      ║ Implemented generically    ║ Needs per-style visual      ║
║                      ║ with visual proxies, hit   ║ acceptance and themed       ║
║                      ║ detection, impact damage   ║ travel/impact contracts     ║
╠══════════════════════╬════════════════════════════╬════════════════════════════╣
║ Field/zone           ║ Implemented generically    ║ Functional base exists,     ║
║                      ║ with pulsing effects,      ║ but persistent readability  ║
║                      ║ support pulses, terrain    ║ is still high risk          ║
║                      ║ handling, visual proxies   ║                            ║
╠══════════════════════╬════════════════════════════╬════════════════════════════╣
║ Pull/tether          ║ Pull movement exists       ║ Missing first-class visible ║
║                      ║ through NPC movement       ║ tether/beam/stream contract ║
╠══════════════════════╬════════════════════════════╬════════════════════════════╣
║ Summon combat AI     ║ Source has target search,  ║ Partially implemented,      ║
║                      ║ chase, attack, damage,     ║ not proven visually or      ║
║                      ║ role logic                 ║ with acceptance scenarios   ║
╠══════════════════════╬════════════════════════════╬════════════════════════════╣
║ Transformation/form  ║ Source applies model id,   ║ Functional base exists,     ║
║                      ║ effect, pulse impacts,     ║ visual/form identity needs  ║
║                      ║ locomotion pressure        ║ explicit verification       ║
╠══════════════════════╬════════════════════════════╬════════════════════════════╣
║ Status/coating VFX   ║ Status effects apply       ║ Gameplay effects exist,     ║
║                      ║ through tokens             ║ readable target feedback    ║
║                      ║                            ║ is not first-class enough   ║
╠══════════════════════╬════════════════════════════╬════════════════════════════╣
║ Shields/heals        ║ Health/shield effects      ║ Functional base exists,     ║
║                      ║ implemented in several     ║ needs visual language by    ║
║                      ║ places                     ║ style                       ║
╠══════════════════════╬════════════════════════════╬════════════════════════════╣
║ Barriers/world       ║ Some barriers/fields and   ║ Partial. Blocking, shape,   ║
║ objects              ║ repulsion logic exist      ║ collision, and cleanup need ║
║                      ║                            ║ per-ability contracts       ║
╠══════════════════════╬════════════════════════════╬════════════════════════════╣
║ Perks                ║ Selection and some stat    ║ Major gap. Most authored    ║
║                      ║ modifiers/triggers exist   ║ effect families are log-only║
║                      ║                            ║ or not represented          ║
╚══════════════════════╩════════════════════════════╩════════════════════════════╝
```

## Style Primitive Breakdown

```text
Terra
├─ arbor: rooted=heal+terrain; vines=pull_tether+status; sapling=summon_ai+terrain
├─ bloom: nightshade=status+terrain; frolick=heal; cacti_cluster=projectile+status
├─ gem: lapidary=shield+terrain; fracture=projectile; refraction=generic
├─ magma: lava_pool=field+status+terrain; obsidian_skin=shield+terrain; magma_sling=projectile+status
├─ metal: iron_wall=barrier+heal+terrain; metal_coat=shield+terrain; alloy_enhancement=generic/followup-adjacent
├─ quake: stomp=terrain; aftershock=field+status+terrain; sinkhole=field+status+terrain
├─ sand: sandstorm=field+status+terrain; dust_devil=field+terrain; vitrification=projectile+status
├─ self_petrification: gargoyle=heal+shield+terrain; glare=status; tunnel=movement+terrain
├─ soil: burrow=movement+terrain; mudpit=field+status+terrain; debris=projectile+status
└─ stone: rubble_rouser=projectile+weapon_followup; pillar_strike=status+terrain; rockslide=field+status+terrain

Hydro
├─ bilgewater: bilge_dump=status+terrain; anchor_haul=projectile/line+pull/chain visual expectation; oil_spill=shield+terrain
├─ boiling: scald=projectile+status; geyser=status+terrain; overheat=status+terrain
├─ freshwater: leap_frog=movement+status; river_rapids=generic; swamp_monster=summon_ai+terrain
├─ iceberg: ice_cap=shield+status+terrain; glacier=barrier+shield+terrain; ice_shelf=status+terrain
├─ icicle: frozen_needles=projectile+status; stalactite_crash=status+terrain; skate=movement+terrain
├─ rain: piercing_rain=field+status+terrain; rainbow=support_field+heal+terrain; splash=projectile+shield
├─ saltwater: tide_pool=field+status+terrain; abyssal_assist=status+terrain; rip_current=pull_tether+status
├─ snow: snow_imp=summon_ai+terrain; snowstorm=field+status+terrain; frosty=summon_ai+terrain
├─ surf: high_tide=projectile/line; waverider=shield; riptide=pull_tether+status
└─ vapor: vapor_vanish=terrain; dispersion=movement; hidrosis=terrain

Aero
├─ gale_wizard: gust=projectile; cyclone_shield=shield+terrain; tempest=field+pull+status+terrain
├─ jet: jet_burst=movement; afterburner=movement+status+terrain; mach_punch=movement+status
├─ jump: leap=movement+status; divebomb=movement+status; hang_time=movement
├─ pollution: smog=field+status+terrain; toxic_breath=status+terrain; acid_rain=field+status+terrain
├─ pressure: air_shot=projectile; bullet_storm=projectile+status; pressure_burst=terrain
├─ scream: shriek=status; sonic_boom=projectile+status; battle_cry=terrain
├─ smoke: smoke_bomb=field+status+terrain; vanish=terrain; smoke_form=form
├─ thunder: thunderclap=status+terrain; smite=projectile; chain_lightning=chain/projectile+status
├─ tornado: twister=field; funnel_cloud=field+pull+status+terrain; eye_of_the_storm=heal+shield+terrain
└─ wind_blade: air_slash=projectile; gale_cutter=projectile; razor_wind=generic

Corruptus
├─ attonement: sanctuary=support_field+heal+terrain; absorb=heal+shield; purify=shield+terrain
├─ flame: fireball=projectile+status; ignite=status+terrain; combust=status
├─ hell_flame: hellfire=projectile+status; infernal_ground=field+status+terrain; soul_scorch=status
├─ imbuement: imbue_power=generic; imbue_fortitude=heal+shield+terrain; imbue_swiftness=generic
├─ mentokinesis: dominate=status; mind_shatter=projectile+status; hivemind=terrain
├─ necro: raise_dead=summon_ai+terrain; life_drain=channel; death_mark=status
├─ primordial: pterodactyl_form=form; triceratops_form=form+shield; t_rex_form=form+status+terrain
├─ scarak: scarak_egg=summon_ai+terrain; brood_surge=summon_command; locust_queen=summon_ai+terrain
├─ shadow: shadow_step=movement+clone summon; umbral_veil=terrain; dark_embrace=field+terrain
└─ void: rift=field+pull+status+terrain; void_spawn=summon_ai+terrain; consume=status
```

## Concrete High-Risk Examples

```text
Saltwater / Rip Current
authored: line_control + slow + undertow_stream + pull_force
runtime: target is moved toward owner on pulse
gap: no dedicated water tether/stream from caster or pool to target

Hydro / Snow Imp, Frosty, Freshwater Swamp Monster, Terra Sapling,
Corruptus Raise Dead, Void Spawn, Scarak Egg, Locust Queen
authored: summon entities that should fight for caster
runtime: summon spawn, target acquisition, chase, attack, damage, and role logic exist
gap: no focused acceptance scenario proving a spawned summon visibly engages, attacks,
     damages, and survives/despawns correctly for every summon family

Bilgewater / Anchor Haul and Arbor / Vines
authored: chain/whip pull fantasy
runtime: pull movement or projectile-line behavior exists
gap: chain/vine/whip tether is not a reusable visual primitive

Rain / Piercing Rain, Rainbow, Tide Pool, Snowstorm, Lava Pool, Smog,
Void Rift, Sandstorm, Dust Devil, Rockslide, Mudpit
authored: persistent field readability matters
runtime: field visual proxies and loop effects exist
gap: field boundary, floor coverage, pulse timing, and target-affect readability are
     generic and not encoded as visual acceptance criteria
```

## Class Passive Audit

```text
╔═══════════╦════════════════════╦═══════════════════════════════════╗
║ Class     ║ Authored Passive   ║ Runtime Verdict                   ║
╠═══════════╬════════════════════╬═══════════════════════════════════╣
║ Terra     ║ Earthen Resilience ║ Partial but real: stationary      ║
║           ║                    ║ shield, low-health regen, cave    ║
║           ║                    ║ light/vision-ish behavior. Mining ║
║           ║                    ║ speed bonus is authored but not   ║
║           ║                    ║ clearly represented in the        ║
║           ║                    ║ inspected manager.                ║
╠═══════════╬════════════════════╬═══════════════════════════════════╣
║ Hydro     ║ Tidal Flow         ║ Partial but real: spell vamp,     ║
║           ║                    ║ swim speed, oxygen, low-water     ║
║           ║                    ║ cost/damage modifiers.            ║
╠═══════════╬════════════════════╬═══════════════════════════════════╣
║ Aero      ║ Tempo Surge        ║ Partial but real: movement speed  ║
║           ║                    ║ and signature energy max bonus.   ║
╠═══════════╬════════════════════╬═══════════════════════════════════╣
║ Corruptus ║ Soul Harvest       ║ Mostly outside ClassPassiveManager║
║           ║                    ║ and routed through resource kill  ║
║           ║                    ║ handling. Needs explicit audit of ║
║           ║                    ║ soul generation/cast pool behavior║
║           ║                    ║ against authored passive text.    ║
╚═══════════╩════════════════════╩═══════════════════════════════════╝
```

## Perk Audit

Perks are the biggest source/data mismatch.

```text
╔═════════════════════════╦══════════════════════════════════════════╗
║ Authored                 ║ Runtime                                 ║
╠═════════════════════════╬══════════════════════════════════════════╣
║ 800 total perks          ║ DataLoader removes tier >14, leaving    ║
║ 200 per class            ║ 560 loaded perks                        ║
╠═════════════════════════╬══════════════════════════════════════════╣
║ 20 tiers                 ║ PerkManager supports tier milestones    ║
║                          ║ through 20 and max 60 selected perks    ║
╠═════════════════════════╬══════════════════════════════════════════╣
║ 80+ effect type labels   ║ PlayerStatModifierManager handles only  ║
║ across authored data     ║ stat_increase, stat_multiplier, crude   ║
║                          ║ damage_reduction, on_hit, on_kill.      ║
║                          ║ damage_increase and most other effects  ║
║                          ║ are log-only or ignored.                ║
╚═════════════════════════╩══════════════════════════════════════════╝
```

Loaded perk effect types after the tier >14 filter are still broad:

```text
ability 162             damage_increase 46     summon 41
stat_increase 28        passive 20             damage_reduction 20
immunity 17             effect_enhancement 16  conditional_buff 12
transformation 12       stat_multiplier 10     ascension 10
on_hit 9                aura 9                 structure 8
```

Verdict: the perk tree should not be treated as implemented. The UI/selection shell exists and a few low-level stat or trigger hooks exist, but the authored perk fantasy needs its own primitive plan.

## Recommended Next Build Order

```text
P0
├─ Summon combat acceptance primitive
│  ├─ spawn
│  ├─ acquire hostile target
│  ├─ visibly move/chase or hold ranged position
│  ├─ visibly attack
│  ├─ deal owner-attributed damage
│  └─ despawn/cleanup
│
└─ Pull/tether visual primitive
   ├─ caster/source anchor
   ├─ target anchor
   ├─ themed link effect: water stream, vine, chain, void pull, wind funnel
   ├─ target movement sync
   └─ clear start/end visual evidence

P1
├─ Persistent field readability primitive
│  ├─ visible footprint/boundary
│  ├─ loop effect cadence
│  ├─ target hit/readability pulse
│  └─ cleanup/despawn evidence
│
├─ Status/coating visual primitive
│  ├─ burn, slow/freeze, root, stun, vulnerability, poison/toxic, blind/deafen
│  └─ target feedback separate from raw damage
│
└─ Transformation/form acceptance primitive
   ├─ model or unmistakable form FX
   ├─ stat/locomotion effect
   ├─ attack/pulse behavior
   └─ return/cleanup

P2
├─ Projectile travel/impact theme pass
├─ Barrier/world-object behavior pass
└─ Perk effect-family implementation plan
```

## Bottom Line

The right next step is not to manually inspect all 120 abilities one by one in-game yet. The runtime needs a small set of stronger shared primitives first. The two highest-confidence first targets are:

1. Summon combat acceptance, because source code claims summons can fight but the user has not seen convincing visual proof.
2. Pull/tether visuals, because the exact Saltwater complaint is a primitive gap: pull movement exists, but the water tether fantasy is not guaranteed.

Once those are implemented as reusable primitives, we can rerun style sweeps with evidence that checks the actual concept, not just "damage happened" or "a target moved."
