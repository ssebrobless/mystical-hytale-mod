# MOTM Ability Reference

Updated: 2026-05-25

This is the GitHub-facing ability reference for all classes, styles, and active abilities currently shipped in the mod data. It is generated from the checked-in JSON under `src/main/resources/data/classes` and `src/main/resources/data/styles`.

For concept review, do not treat this file as the whole design brief. Pair it
with:

- `docs/FRIEND_REVIEW_GUIDE.md`
- `CODEX_CLASS_STYLE_ABILITY_REVIEW_MOCKUP_2026-05-22.md`
- `CODEX_CONCEPT_REVIEW_DECISIONS_2026-05-22.md`

Those files describe the intended function, visual read, and user-approved
corrections for the abilities.

## Casting Model

```text
MOTM active abilities
  -> no class resource spending
  -> balanced by cooldowns, durations, charges, action timing, movement, positioning, and item conditions
  -> verified by scripts/audit-no-resource.ps1
```

Resource notes:

- Terra abilities do not spend mined or carried materials.
- Hydro abilities do not spend waterskin water; water remains thematic/environmental utility only.
- Aero abilities do not use a class resource; its passive may interact with native Hytale energy/stamina, but not as a cast cost.
- Corruptus abilities do not spend souls; dark/void power is handled through cooldown, duration, risk, and status effects.

## Summary

| Class | Styles | Active Abilities | Resource-Cost Status |
| --- | ---: | ---: | --- |
| terra | 10 | 30 | no resource costs |
| hydro | 10 | 30 | no resource costs |
| aero | 10 | 30 | no resource costs |
| corruptus | 10 | 30 | no resource costs |

## Terra - The Earthen Guardian

Theme: Earth  
Passive: **Immovable** - Reduces knockback taken by 20% without increasing knockback dealt. Regenerates 1% max health per second while below 30% health, mines 50% faster with pickaxes, and gains cave vision underground.

### Quake (`quake`)

Theme: Knockback, AoE, ground control  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Stomp** (`stomp`) | Shockwave that damages and knocks back | ground_burst / self_centered | cd 2s | radius 4 | knockback |
| 2 | **Aftershock** (`aftershock`) | Slow and disorient enemies | ground_zone / self_centered | cd 5s, dur 4s | radius 8 | slow+disoriented |
| 3 | **Sinkhole** (`sinkhole`) | Bury enemy, dealing suffocation damage | ground_target / enemy | cd 6s, dur 2.5s | range 10, radius 3 | stun |

### Metal (`metal`)

Theme: Defense, walls, self-healing  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Iron Wall** (`iron_wall`) | Create barrier, heal 10% HP | barrier / line | cd 4s, dur 4s | range 7, width 3, height 3 | heal |
| 2 | **Metal Coat** (`metal_coat`) | Gain 20% damage reduction for 3 turns | self_buff / self | cd 6s, dur 6s | - | defense_buff |
| 3 | **Alloy Enhancement** (`alloy_enhancement`) | Boost next attack by 35% | self_buff / self | dur 8s | - | damage_buff |

### Magma (`magma`)

Theme: Lava, area denial, burn damage  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Lava Pool** (`lava_pool`) | Create lava that burns over time | ground_zone / ground_target | cd 4s, dur 6s | range 12, radius 4 | burn |
| 2 | **Obsidian Skin** (`obsidian_skin`) | Shield equal to 30% HP, reduce damage taken | self_buff / self | cd 6s, dur 6s | - | shield |
| 3 | **Magma Sling** (`magma_sling`) | Ranged attack with slow and burn | projectile / enemy | cd 3s | range 18 | burn+slow |

### Stone (`stone`)

Theme: Heavy strikes, knockback, crowd control  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Rubble Rouser** (`rubble_rouser`) | Hurl rubble that knocks back the enemy | projectile / enemy | cd 3s | range 16 | knockback |
| 2 | **Pillar Strike** (`pillar_strike`) | Slam a stone pillar down, stunning the target | ground_strike / ground_target | cd 5s | range 14, radius 2.5, height 3 | stun |
| 3 | **Rockslide** (`rockslide`) | Trigger a rockslide dealing heavy damage and slowing | ground_zone / ground_target | cd 6s, dur 3s | range 16, radius 6 | slow |

### Arbor (`arbor`)

Theme: Nature, healing, minions  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Rooted** (`rooted`) | Root yourself to heal 20% HP and regenerate | self_buff / self | cd 6s, dur 5s | - | heal |
| 2 | **Vines** (`vines`) | Entangle one enemy with thorny vines, rooting them while damage ticks over the duration | line_control / enemy | dur 2.5s | range 14, width 2 | root+dot |
| 3 | **Sapling** (`sapling`) | Fire a seed that lands on the ground, grows a sapling, and lures enemies nearby | projectile_line / ground_target | cd 8s, dur 18s | range 10 | lure |

### Bloom (`bloom`)

Theme: Poison, healing, nature magic  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Nightshade** (`nightshade`) | Launch a nightshade seed that blooms into a poisonous lure | projectile_line / ground_target | cd 4s, dur 5s | range 12, radius 5 | dot+slow |
| 2 | **Frolick** (`frolick`) | Dance among flowers, healing and gaining speed | self_buff / self | cd 6s, dur 6s | - | heal+attack_buff+speed |
| 3 | **Cacti Cluster** (`cacti_cluster`) | Launch a heavy cactus cluster that sticks, poisons, slows, then bursts | projectile / enemy | cd 5s, dur 4s | range 14, radius 4, width 2 | dot+slow |

### Self Petrification (`self_petrification`)

Theme: Stone form, stuns, invulnerability  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Gargoyle** (`gargoyle`) | Turn to stone, healing and gaining a shield | self_buff / self | cd 6s, dur 5s | - | heal+shield |
| 2 | **Glare** (`glare`) | Petrifying gaze that stuns for 2 turns | gaze / enemy | cd 6s, dur 2.5s | range 16 | stun |
| 3 | **Tunnel** (`tunnel`) | Burrow underground and strike from below | dash / enemy | cd 7s, dur 5s | range 10 | evasion |

### Soil (`soil`)

Theme: Burrowing, debuffs, area control  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Burrow** (`burrow`) | Emerge from the ground in a devastating strike | dash / enemy | cd 5s | range 9, radius 2.5 | knockback |
| 2 | **Mudpit** (`mudpit`) | Trap the enemy in sticky mud | ground_zone / ground_target | cd 2s, dur 5s | range 12, radius 4 | slow+vulnerability |
| 3 | **Debris** (`debris`) | Fling debris that blinds and weakens | projectile_volley / enemy | cd 5s | range 16, width 4 | vulnerability+blind |

### Sand (`sand`)

Theme: Desert storms, erosion, glass  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Sandstorm** (`sandstorm`) | Conjure a blinding sandstorm that damages over time | self_buff / self | cd 6s, dur 10s | radius 5 | dot+slow |
| 2 | **Dust Devil** (`dust_devil`) | Spin up a dust devil that knocks back | dash / self_centered | cd 5s, dur 2s | range 5, radius 3 | knockback |
| 3 | **Vitrification** (`vitrification`) | Superheated sand burns the enemy | projectile / enemy | cd 4s | range 15 | burn |

### Gem (`gem`)

Theme: Crystals, shields, refraction  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Lapidary** (`lapidary`) | Place a persistent green gem cube that can be recalled and acts as the anchor for Gem abilities | ground_target / ground_target | cd 7s, dur 30s | - | persistent_object |
| 2 | **Fracture** (`fracture`) | Shatter the active gem in a fast expanding green crystal burst | ground_burst / self_centered | cd 8s | range 15, radius 5 | burst |
| 3 | **Refraction** (`refraction`) | Project a bright green light aura around the active gem that boosts allies inside it | support_zone / self_centered | cd 6s, dur 8s | radius 5 | attack_buff+speed |


## Hydro - The Tidal Sage

Theme: Water  
Passive: **Tidal Flow** - Abilities heal you for 3% of damage dealt. Hydro swims 40% faster and can breathe underwater 50% longer. Aqua Barrier forms a whole-body bubble shield that depletes before other Hydro defensive overlays. Hydro abilities use cooldowns instead of waterskin costs.

### Icicle (`icicle`)

Theme: Rapid attacks, stacking slow  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Frozen Needles** (`frozen_needles`) | Rapid ice shards, stacking slow | projectile_volley / enemy | cd 2s | range 18, width 3 | slow_stack |
| 2 | **Stalactite Crash** (`stalactite_crash`) | Heavy ice impact with explosion | ground_strike / ground_target | cd 5s | range 14, radius 3, height 6 | slow |
| 3 | **Skate** (`skate`) | Gain 50% evasion for 2 turns | dash_buff / self | cd 4s, dur 4s | - | evasion_buff |

### Snow (`snow`)

Theme: Minions, AoE slow  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Snow Imp** (`snow_imp`) | Summon minion that attacks each turn | summon / ground_target | cd 5s, dur 18s | range 10 | summon |
| 2 | **Snowstorm** (`snowstorm`) | Slow enemy attack speed for 3 turns | ground_zone / ground_target | cd 5s, dur 6s | range 12, radius 5 | attack_slow |
| 3 | **Frosty** (`frosty`) | Summon golem that taunts and absorbs damage | summon / ground_target | cd 8s, dur 20s | range 10 | summon_tank |

### Surf (`surf`)

Theme: Waves, knockback, mobility  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **High Tide** (`high_tide`) | Push enemy back, gain speed | wave_line / line | cd 3s | range 14, width 5 | knockback+speed |
| 2 | **Waverider** (`waverider`) | Gain shield and speed for 2 turns | self_buff / self | cd 4s, dur 5s | - | shield+speed |
| 3 | **Riptide** (`riptide`) | Pull enemy in, increase damage taken | line_control / enemy | cd 5s | range 15 | vulnerability |

### Rain (`rain`)

Theme: Sustain, auto-healing, splash damage  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Piercing Rain** (`piercing_rain`) | Acidic rain that damages over time and speeds attacks | ground_zone / ground_target | cd 6s, dur 6s | range 14, radius 6 | dot+attack_buff |
| 2 | **Rainbow** (`rainbow`) | Summon a healing rainbow that restores HP | support_zone / ground_target | cd 8s, dur 6s | range 12, radius 5 | heal |
| 3 | **Splash** (`splash`) | Splash water that damages and creates a shield | projectile_burst / enemy | cd 5s | range 12, radius 3 | shield |

### Boiling (`boiling`)

Theme: Steam, burns, explosive damage  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Scald** (`scald`) | Scorch with boiling water, burning and knocking back | projectile / enemy | cd 3s | range 14 | burn+knockback |
| 2 | **Geyser** (`geyser`) | Erupt a geyser of scalding water | ground_strike / ground_target | cd 5s | range 12, radius 3, height 6 | burn |
| 3 | **Overheat** (`overheat`) | Supercharge attacks but burn yourself | self_buff / self | cd 7s, dur 8s | - | attack_buff+self_burn |

### Vapor (`vapor`)

Theme: Evasion, mist, intangibility  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Vapor Vanish** (`vapor_vanish`) | Dissolve into mist for 50% evasion | self_buff / self | cd 5s, dur 5s | - | evasion |
| 2 | **Dispersion** (`dispersion`) | Reform from vapor in a quick strike | dash / enemy | cd 3s | range 11 | - |
| 3 | **Hidrosis** (`hidrosis`) | Coat yourself in moisture for enhanced dodging | self_buff / self | cd 6s, dur 7s | - | evasion |

### Iceberg (`iceberg`)

Theme: Heavy ice, freezing, defense  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Ice Cap** (`ice_cap`) | Encase in ice for a shield and freeze attacker | self_buff / self | cd 6s, dur 6s | - | shield+stun |
| 2 | **Glacier** (`glacier`) | Conjure glacial armor for damage reduction | barrier / line | cd 7s, dur 7s | range 8, width 8, height 4 | defense_buff |
| 3 | **Ice Shelf** (`ice_shelf`) | Crush enemy with a massive ice shelf | ground_strike / ground_target | cd 8s | range 14, width 6 | stun+slow |

### Saltwater (`saltwater`)

Theme: Ocean, tides, debuffs  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Tide Pool** (`tide_pool`) | Create a tide pool that speeds you and slows enemy | ground_zone / ground_target | cd 6s, dur 6s | range 10, radius 4 | slow+attack_buff |
| 2 | **Abyssal Assist** (`abyssal_assist`) | Call upon deep-sea pressure to stun and weaken | ground_burst / enemy | cd 5s, dur 2.5s | range 14, radius 3 | stun+vulnerability |
| 3 | **Rip Current** (`rip_current`) | Drag the enemy with a powerful current | line_control / enemy | cd 4s, dur 2s | range 16 | slow |

### Freshwater (`freshwater`)

Theme: Rivers, mobility, summons  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Leap Frog** (`leap_frog`) | Leap and strike, leaving the enemy vulnerable | dash / enemy | cd 4s | range 9 | vulnerability |
| 2 | **River Rapids** (`river_rapids`) | Ride the rapids to gain speed and momentum | self_buff / self | cd 6s, dur 8s | - | attack_buff+speed |
| 3 | **Swamp Monster** (`swamp_monster`) | Summon a creature from the swamp to fight | summon / ground_target | cd 8s, dur 20s | range 10 | summon |

### Bilgewater (`bilgewater`)

Theme: Filth, corrosion, naval warfare  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Bilge Dump** (`bilge_dump`) | Dump foul bilge water that poisons and slows | cone / cone | cd 5s, dur 5s | range 10 | dot+slow |
| 2 | **Anchor Haul** (`anchor_haul`) | Haul an anchor across the enemy for heavy damage | projectile_line / enemy | cd 6s | range 14, width 3 | knockback |
| 3 | **Oil Spill** (`oil_spill`) | Coat yourself in protective oil for defense | self_buff / self | cd 7s, dur 8s | - | defense_buff+attack_buff |


## Aero - The Storm Dancer

Theme: Air  
Passive: **Wind Walker** - You move 25% faster at all times and your native Hytale energy bar is increased by 80%. Vertical movement from Aero style abilities is handled by the style ability itself so Wind Walker does not duplicate jumps, dives, hovers, or launch resets.

### Scream (`scream`)

Theme: Sonic attacks, stuns, team buffs  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Shriek** (`shriek`) | Deafen enemy, reduce their accuracy | cone / cone | cd 3s, dur 4s | range 10 | deafen |
| 2 | **Sonic Boom** (`sonic_boom`) | Shockwave that stuns | wave_line / line | cd 4s | range 14, width 6 | stun |
| 3 | **Battle Cry** (`battle_cry`) | Boost your attack and speed for 3 turns | self_buff / self | cd 5s, dur 6s | - | attack_buff+speed |

### Jet (`jet`)

Theme: Fast dashes, momentum  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Jet Burst** (`jet_burst`) | Dash attack, launch enemy | dash / enemy | cd 2s | range 10 | knockback |
| 2 | **Afterburner** (`afterburner`) | Enhanced dash that leaves damage trail | dash / line | cd 4s, dur 3s | range 14 | burn |
| 3 | **Mach Punch** (`mach_punch`) | Powerful strike after dash | dash_strike / enemy | cd 3s | range 8 | stun_if_wall |

### Thunder (`thunder`)

Theme: Stuns, lightning damage  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Thunderclap** (`thunderclap`) | Stun all and apply shocked | self_burst / self_centered | cd 4s, dur 2.5s | radius 5 | stun+shocked |
| 2 | **Smite** (`smite`) | Lightning bolt, bonus vs shocked | projectile / enemy | cd 3s | range 18 | lightning |
| 3 | **Chain Lightning** (`chain_lightning`) | Continuous lightning damage | chain / enemy_cluster | cd 5s, dur 4s | range 16, radius 8 | dot |

### Tornado (`tornado`)

Theme: Whirlwinds, knockback, sustain  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Twister** (`twister`) | Spin up a twister that knocks enemies back | ground_zone / ground_target | cd 3s, dur 4s | range 12, radius 3 | knockback |
| 2 | **Funnel Cloud** (`funnel_cloud`) | Create a funnel cloud that damages over time | ground_zone / ground_target | cd 5s, dur 6s | range 14, radius 5 | dot |
| 3 | **Eye of the Storm** (`eye_of_the_storm`) | Find calm in the storm, healing and shielding | self_buff / self | cd 7s, dur 6s | - | heal+shield |

### Jump (`jump`)

Theme: Aerial mobility, diving strikes  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Leap** (`leap`) | Launch into the air and crash down on the enemy | leap / enemy | cd 4s | range 10, radius 3 | knockback+vulnerability |
| 2 | **Divebomb** (`divebomb`) | Dive from great height for devastating damage | dive_strike / ground_target | cd 6s | range 12, radius 4 | slow |
| 3 | **Hang Time** (`hang_time`) | Stay airborne to dodge incoming attacks | air_stall / self | cd 5s, dur 3s | - | evasion |

### Wind Blade (`wind_blade`)

Theme: Cutting wind, rapid strikes  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Air Slash** (`air_slash`) | Slice the air to send a cutting gust | projectile_line / enemy | cd 2s | range 16, width 2 | - |
| 2 | **Gale Cutter** (`gale_cutter`) | A powerful wind blade that knocks back | projectile_line / enemy | cd 5s | range 18, width 3 | knockback |
| 3 | **Razor Wind** (`razor_wind`) | Sharpen the wind around you for stronger attacks | self_buff / self | cd 6s, dur 8s | - | attack_buff |

### Smoke (`smoke`)

Theme: Stealth, debuffs, evasion  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Smoke Bomb** (`smoke_bomb`) | Deploy smoke to slow enemies and speed yourself | ground_zone / ground_target | cd 5s, dur 6s | range 10, radius 4 | slow+attack_buff+speed |
| 2 | **Vanish** (`vanish`) | Disappear in smoke, boosting next attack by 30% | self_buff / self | cd 6s, dur 5s | - | stealth |
| 3 | **Smoke Form** (`smoke_form`) | Become smoke for 40% evasion over 2 turns | transformation / self | cd 7s, dur 5s | - | evasion |

### Gale Wizard (`gale_wizard`)

Theme: Wind magic, control, reflection  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Gust** (`gust`) | Blast the enemy with a powerful gust of wind | projectile / enemy | cd 4s | range 16 | knockback |
| 2 | **Cyclone Shield** (`cyclone_shield`) | Surround yourself with a defensive cyclone | self_buff / self | cd 6s, dur 6s | - | defense_buff+shield |
| 3 | **Tempest** (`tempest`) | Unleash a devastating tempest that stuns | ground_zone / ground_target | cd 8s, dur 4s | range 14, radius 6 | stun |

### Pressure (`pressure`)

Theme: Air pressure, charged attacks  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Air Shot** (`air_shot`) | Compressed air projectile that hits hard | projectile / enemy | cd 3s | range 18 | - |
| 2 | **Bullet Storm** (`bullet_storm`) | Rapid air bullets that slow the target | projectile_volley / enemy | cd 5s | range 18, width 4 | slow |
| 3 | **Pressure Burst** (`pressure_burst`) | Release built-up pressure in an explosive burst | self_burst / self_centered | cd 7s | radius 5 | knockback |

### Pollution (`pollution`)

Theme: Toxic air, corrosion, debuffs  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Smog** (`smog`) | Engulf the enemy in toxic smog | ground_zone / ground_target | cd 5s, dur 6s | range 12, radius 5 | dot+slow |
| 2 | **Toxic Breath** (`toxic_breath`) | Exhale poisonous fumes that weaken defenses | cone / cone | cd 6s, dur 5s | range 10 | dot+vulnerability |
| 3 | **Acid Rain** (`acid_rain`) | Call down acidic rain that corrodes armor | ground_zone / ground_target | cd 8s, dur 7s | range 14, radius 6 | dot+vulnerability |


## Corruptus - The Void Flame

Theme: Dark Magic + Fire  
Passive: **Soul Harvest** - Hostile kills build up to 5 Soul Harvest stacks. Each stack fuels Infernal Aura, granting 2% increased damage and 1% damage reduction per stack. At 5 stacks, lethal damage heals you to half HP instead of killing you, clears the stacks, and starts a 10 minute lockout that prevents all Corruptus passive stack gain. Corruptus abilities use cooldowns instead of Souls.

### Flame (`flame`)

Theme: Fire attacks, burning DoT  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Fireball** (`fireball`) | Explosive fire, applies burn | projectile / enemy | cd 2s | range 18, radius 3 | burn |
| 2 | **Ignite** (`ignite`) | Set self on fire, damage nearby enemies | self_burst / self_centered | cd 4s, dur 4s | radius 4 | self_burn+aoe |
| 3 | **Combust** (`combust`) | Consume burns for massive damage | execute / enemy | cd 5s | range 14 | consume_burn |

### Necro (`necro`)

Theme: Undead summons, life drain  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Raise Dead** (`raise_dead`) | Summon skeleton minion | summon / ground_target | cd 3s, dur 20s | range 10 | summon |
| 2 | **Life Drain** (`life_drain`) | Damage enemy, heal yourself | channel / enemy | cd 4s, dur 3s | range 14 | lifesteal |
| 3 | **Death Mark** (`death_mark`) | Mark enemy to take 25% more damage | curse / enemy | cd 5s, dur 6s | range 16 | vulnerability |

### Shadow (`shadow`)

Theme: Stealth, clones, evasion  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Shadow Step** (`shadow_step`) | Teleport and leave damaging clone | teleport / enemy | cd 3s, dur 4s | range 12 | clone |
| 2 | **Umbral Veil** (`umbral_veil`) | Go invisible, next attack +40% damage | self_buff / self | cd 5s, dur 5s | - | stealth |
| 3 | **Dark Embrace** (`dark_embrace`) | Create shadow zone, 25% dodge chance | ground_zone / ground_target | cd 6s, dur 6s | range 10, radius 5 | evasion_zone |

### Hell Flame (`hell_flame`)

Theme: Hellfire, self-damage, overwhelming power  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Hellfire** (`hellfire`) | Unleash hellfire that burns and slows, scorching yourself | projectile_burst / enemy | cd 4s | range 16, radius 3 | burn+slow |
| 2 | **Infernal Ground** (`infernal_ground`) | Set the ground ablaze, burning enemies and boosting attacks | ground_zone / ground_target | cd 7s, dur 7s | range 12, radius 5 | burn+attack_buff |
| 3 | **Soul Scorch** (`soul_scorch`) | Scorch the enemy's soul, weakening and slowing them | curse / enemy | cd 8s, dur 6s | range 16 | vulnerability+slow |

### Mentokinesis (`mentokinesis`)

Theme: Mind control, psychic damage, stuns  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Dominate** (`dominate`) | Dominate the enemy's mind, stunning them | gaze / enemy | cd 7s, dur 3s | range 15 | stun+vulnerability |
| 2 | **Mind Shatter** (`mind_shatter`) | Shatter the enemy's psyche with raw power | projectile / enemy | cd 6s | range 16 | stun |
| 3 | **Hivemind** (`hivemind`) | Tap into collective dark intelligence for power | self_buff / self | cd 8s, dur 8s | - | attack_buff |

### Imbuement (`imbuement`)

Theme: Self-buffs, enhancement magic  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Imbue: Power** (`imbue_power`) | Channel dark energy to massively boost attack | self_buff / self | cd 6s, dur 8s | - | attack_buff |
| 2 | **Imbue: Fortitude** (`imbue_fortitude`) | Reinforce your body with dark magic | self_buff / self | cd 7s, dur 8s | - | defense_buff+heal |
| 3 | **Imbue: Swiftness** (`imbue_swiftness`) | Enhance your reflexes with shadow magic | self_buff / self | cd 5s, dur 7s | - | evasion+attack_buff |

### Attonement (`attonement`)

Theme: Purification, healing, cleansing  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Sanctuary** (`sanctuary`) | Create a holy space that heals and cleanses | support_zone / ground_target | cd 7s, dur 6s | range 12, radius 5 | heal |
| 2 | **Absorb** (`absorb`) | Absorb incoming damage and convert to healing | self_buff / self | cd 6s, dur 5s | - | defense_buff+heal |
| 3 | **Purify** (`purify`) | Purge all negative effects and bolster defenses | cleanse / self | cd 8s, dur 6s | - | defense_buff |

### Void (`void`)

Theme: Void magic, summoning, destruction  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Rift** (`rift`) | Open a void rift that damages over time | ground_zone / ground_target | cd 6s, dur 6s | range 14, radius 5 | dot |
| 2 | **Void Spawn** (`void_spawn`) | Summon a creature from the void | summon / ground_target | cd 7s, dur 18s | range 10 | summon |
| 3 | **Consume** (`consume`) | The void consumes the enemy for massive damage | execute / enemy | cd 9s, dur 2.5s | range 12 | stun |

### Scarak (`scarak`)

Theme: Insect swarms, summoning, overwhelming numbers  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Scarak Egg** (`scarak_egg`) | Lay a scarak egg that hatches into a minion | summon / ground_target | cd 3s, dur 16s | range 8 | summon |
| 2 | **Brood Surge** (`brood_surge`) | Empower your minions with dark energy | summon_buff / allied_summons | cd 7s, dur 8s | radius 12 | attack_buff |
| 3 | **Locust Queen** (`locust_queen`) | Summon a massive locust queen to devastate | summon / ground_target | cd 10s, dur 22s | range 10 | summon |

### Primordial (`primordial`)

Theme: Ancient beast forms, transformation  
Resource cost: none

| Slot | Ability | Description | Cast / Target | Timing | Shape | Effect |
| ---: | --- | --- | --- | --- | --- | --- |
| 1 | **Pterodactyl Form** (`pterodactyl_form`) | Take flight in pterodactyl form, gaining evasion | transformation / self | cd 8s, dur 10s | - | evasion+attack_buff |
| 2 | **Triceratops Form** (`triceratops_form`) | Charge in armored triceratops form | transformation / self | cd 8s, dur 10s | - | defense_buff |
| 3 | **T-Rex Form** (`t_rex_form`) | Transform into a terrifying T-Rex for devastating damage | transformation / self | cd 10s, dur 12s | radius 4 | attack_buff+stun |

## Verification

## Shared Passive Perks

Perks are a shared passive-only pool: players unlock one choice every 10 levels through level 100, for a maximum of 10 chosen perks. All 20 are visible to every class so players can mix class-themed passives.

| Theme | Perks |
| --- | --- |
| Aero | Twinkletoes, Accelerate, Bunny Hop, Big Strides, Sharpshooter |
| Hydro | Neptune's Grace, Semiaquatic, Big Lungs, Rainy Day, Freezing Winds |
| Corruptus | Ignite, Desperation, Haunting, Vampirism, Terror |
| Terra | Heavyweight, Eco-friendly, Mole Man, Blacksmith, Toolsmith |

See `docs/PERK_RUNTIME_STATUS.md` for runtime hook status and proof commands.

Run these before claiming the ability reference and runtime data are still aligned:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/audit-no-resource.ps1
powershell -ExecutionPolicy Bypass -File scripts/build-install.ps1
```
