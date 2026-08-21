# Ability Verification Matrix - 120 abilities

Deterministic sweep 2026-08-19T19-24-15. `scripts/verify_abilities.py` casts every ability in-world and asserts from logs:
- **L1** assets wired - preflight audit READY (all 120 manifest rows resolve).
- **L2** dispatched - server `Queued ability cast result ... Cast ...` + **0** `Missing gameplay effect asset`.
- **L3** client render - no client-side asset/animation/load failure (failure-absence).
- **L4** runtime/mech - cast entered its expected runtime (armed/projectile/summon/field/etc.).

Positive on-screen render + aesthetics are NOT log-provable -> covered by a separate visual sample.
Visual sample DONE 2026-08-19 (see `docs/visual-verification-sweep-2026-08-19.md`): direct screenshot
PASS across all 4 classes + families (Vines, Frozen Needles, Snow Imp, Acid Rain, Raise Dead, T-Rex Form);
correct color/model/family, 0 visual defects. Transient/self-centered effects remain L3-render-proven
here; a frame-perfect aesthetic pass of every ability needs a human in-world (headless first-person +
single-slot cast channel cannot catch sub-second/player-centered particles).

**Result: 120/120 wired+dispatched (0 missing assets), 120/120 runtime, 0 real client failures.** `dust_devil` is a combo (requires `sandstorm` active) - confirmed casts in-sequence.

| Class | Style | Ability | id | cast_type | L1 | L2 | L3 | L4 | Notes |
|---|---|---|---|---|---|---|---|---|---|
| Terra | quake | Stomp | `stomp` | ground_burst | PASS | PASS | PASS | PASS | armed (impact needs jump/land - visual sample) |
| Terra | quake | Aftershock | `aftershock` | ground_zone | PASS | PASS | PASS | PASS |  |
| Terra | quake | Sinkhole | `sinkhole` | ground_target | PASS | PASS | PASS | PASS |  |
| Terra | metal | Iron Wall | `iron_wall` | barrier | PASS | PASS | PASS | PASS |  |
| Terra | metal | Metal Coat | `metal_coat` | self_buff | PASS | PASS | PASS | PASS |  |
| Terra | metal | Alloy Enhancement | `alloy_enhancement` | self_buff | PASS | PASS | PASS | PASS |  |
| Terra | magma | Lava Pool | `lava_pool` | ground_zone | PASS | PASS | PASS | PASS |  |
| Terra | magma | Obsidian Skin | `obsidian_skin` | self_buff | PASS | PASS | PASS | PASS |  |
| Terra | magma | Magma Sling | `magma_sling` | projectile | PASS | PASS | PASS | PASS |  |
| Terra | stone | Rubble Rouser | `rubble_rouser` | self_buff | PASS | PASS | PASS | PASS |  |
| Terra | stone | Pillar Strike | `pillar_strike` | ground_strike | PASS | PASS | PASS | PASS |  |
| Terra | stone | Rockslide | `rockslide` | dash | PASS | PASS | PASS | PASS |  |
| Terra | arbor | Rooted | `rooted` | self_buff | PASS | PASS | PASS | PASS |  |
| Terra | arbor | Vines | `vines` | line_control | PASS | PASS | PASS | PASS |  |
| Terra | arbor | Sapling | `sapling` | projectile_line | PASS | PASS | PASS | PASS |  |
| Terra | bloom | Nightshade | `nightshade` | projectile_line | PASS | PASS | PASS | PASS |  |
| Terra | bloom | Frolick | `frolick` | self_buff | PASS | PASS | PASS | PASS |  |
| Terra | bloom | Cacti Cluster | `cacti_cluster` | projectile | PASS | PASS | PASS | PASS |  |
| Terra | self_petrification | Gargoyle | `gargoyle` | self_buff | PASS | PASS | PASS | PASS |  |
| Terra | self_petrification | Glare | `glare` | gaze | PASS | PASS | PASS | PASS |  |
| Terra | self_petrification | Tunnel | `tunnel` | dash | PASS | PASS | PASS | PASS |  |
| Terra | soil | Burrow | `burrow` | dash | PASS | PASS | PASS | PASS |  |
| Terra | soil | Mudpit | `mudpit` | ground_zone | PASS | PASS | PASS | PASS |  |
| Terra | soil | Debris | `debris` | projectile_volley | PASS | PASS | PASS | PASS |  |
| Terra | sand | Sandstorm | `sandstorm` | self_buff | PASS | PASS | PASS | PASS |  |
| Terra | sand | Dust Devil | `dust_devil` | dash | PASS | PASS | PASS | PASS | combo: requires sandstorm active (confirmed in-sequence) |
| Terra | sand | Vitrification | `vitrification` | self_buff | PASS | PASS | PASS | PASS |  |
| Terra | gem | Lapidary | `lapidary` | ground_target | PASS | PASS | PASS | PASS |  |
| Terra | gem | Fracture | `fracture` | ground_burst | PASS | PASS | PASS | PASS |  |
| Terra | gem | Refraction | `refraction` | support_zone | PASS | PASS | PASS | PASS |  |
| Hydro | icicle | Frozen Needles | `frozen_needles` | projectile_volley | PASS | PASS | PASS | PASS |  |
| Hydro | icicle | Stalactite Crash | `stalactite_crash` | ground_strike | PASS | PASS | PASS | PASS |  |
| Hydro | icicle | Skate | `skate` | dash_buff | PASS | PASS | PASS | PASS |  |
| Hydro | snow | Snow Imp | `snow_imp` | summon | PASS | PASS | PASS | PASS |  |
| Hydro | snow | Snowstorm | `snowstorm` | ground_zone | PASS | PASS | PASS | PASS |  |
| Hydro | snow | Frosty | `frosty` | summon | PASS | PASS | PASS | PASS |  |
| Hydro | surf | High Tide | `high_tide` | wave_line | PASS | PASS | PASS | PASS |  |
| Hydro | surf | Waverider | `waverider` | self_buff | PASS | PASS | PASS | PASS |  |
| Hydro | surf | Riptide | `riptide` | line_control | PASS | PASS | PASS | PASS |  |
| Hydro | rain | Piercing Rain | `piercing_rain` | ground_zone | PASS | PASS | PASS | PASS |  |
| Hydro | rain | Rainbow | `rainbow` | support_zone | PASS | PASS | PASS | PASS |  |
| Hydro | rain | Splash | `splash` | projectile_burst | PASS | PASS | PASS | PASS |  |
| Hydro | boiling | Scald | `scald` | projectile | PASS | PASS | PASS | PASS |  |
| Hydro | boiling | Geyser | `geyser` | ground_strike | PASS | PASS | PASS | PASS |  |
| Hydro | boiling | Overheat | `overheat` | self_buff | PASS | PASS | PASS | PASS |  |
| Hydro | vapor | Vapor Vanish | `vapor_vanish` | self_buff | PASS | PASS | PASS | PASS |  |
| Hydro | vapor | Dispersion | `dispersion` | dash | PASS | PASS | PASS | PASS |  |
| Hydro | vapor | Hidrosis | `hidrosis` | self_buff | PASS | PASS | PASS | PASS |  |
| Hydro | iceberg | Ice Cap | `ice_cap` | self_buff | PASS | PASS | PASS | PASS |  |
| Hydro | iceberg | Glacier | `glacier` | barrier | PASS | PASS | PASS | PASS |  |
| Hydro | iceberg | Ice Shelf | `ice_shelf` | ground_strike | PASS | PASS | PASS | PASS |  |
| Hydro | saltwater | Tide Pool | `tide_pool` | ground_zone | PASS | PASS | PASS | PASS |  |
| Hydro | saltwater | Abyssal Assist | `abyssal_assist` | ground_burst | PASS | PASS | PASS | PASS |  |
| Hydro | saltwater | Rip Current | `rip_current` | line_control | PASS | PASS | PASS | PASS |  |
| Hydro | freshwater | Leap Frog | `leap_frog` | dash | PASS | PASS | PASS | PASS |  |
| Hydro | freshwater | River Rapids | `river_rapids` | self_buff | PASS | PASS | PASS | PASS |  |
| Hydro | freshwater | Swamp Monster | `swamp_monster` | summon | PASS | PASS | PASS | PASS |  |
| Hydro | bilgewater | Bilge Dump | `bilge_dump` | cone | PASS | PASS | PASS | PASS |  |
| Hydro | bilgewater | Anchor Haul | `anchor_haul` | projectile_line | PASS | PASS | PASS | PASS |  |
| Hydro | bilgewater | Oil Spill | `oil_spill` | self_buff | PASS | PASS | PASS | PASS |  |
| Aero | scream | Shriek | `shriek` | cone | PASS | PASS | PASS | PASS |  |
| Aero | scream | Sonic Boom | `sonic_boom` | wave_line | PASS | PASS | PASS | PASS |  |
| Aero | scream | Battle Cry | `battle_cry` | self_buff | PASS | PASS | PASS | PASS |  |
| Aero | jet | Jet Burst | `jet_burst` | dash | PASS | PASS | PASS | PASS |  |
| Aero | jet | Afterburner | `afterburner` | dash | PASS | PASS | PASS | PASS |  |
| Aero | jet | Mach Punch | `mach_punch` | dash_strike | PASS | PASS | PASS | PASS |  |
| Aero | thunder | Thunderclap | `thunderclap` | self_burst | PASS | PASS | PASS | PASS |  |
| Aero | thunder | Smite | `smite` | projectile | PASS | PASS | PASS | PASS |  |
| Aero | thunder | Chain Lightning | `chain_lightning` | chain | PASS | PASS | PASS | PASS |  |
| Aero | tornado | Twister | `twister` | ground_zone | PASS | PASS | PASS | PASS |  |
| Aero | tornado | Funnel Cloud | `funnel_cloud` | ground_zone | PASS | PASS | PASS | PASS |  |
| Aero | tornado | Eye of the Storm | `eye_of_the_storm` | self_buff | PASS | PASS | PASS | PASS |  |
| Aero | jump | Leap | `leap` | leap | PASS | PASS | PASS | PASS |  |
| Aero | jump | Divebomb | `divebomb` | dive_strike | PASS | PASS | PASS | PASS |  |
| Aero | jump | Hang Time | `hang_time` | air_stall | PASS | PASS | PASS | PASS |  |
| Aero | wind_blade | Air Slash | `air_slash` | projectile_line | PASS | PASS | PASS | PASS |  |
| Aero | wind_blade | Gale Cutter | `gale_cutter` | projectile_line | PASS | PASS | PASS | PASS |  |
| Aero | wind_blade | Razor Wind | `razor_wind` | projectile_volley | PASS | PASS | PASS | PASS |  |
| Aero | smoke | Smoke Bomb | `smoke_bomb` | ground_zone | PASS | PASS | PASS | PASS |  |
| Aero | smoke | Vanish | `vanish` | self_buff | PASS | PASS | PASS | PASS |  |
| Aero | smoke | Smoke Form | `smoke_form` | transformation | PASS | PASS | PASS | PASS |  |
| Aero | gale_wizard | Gust | `gust` | projectile | PASS | PASS | PASS | PASS |  |
| Aero | gale_wizard | Cyclone Shield | `cyclone_shield` | self_buff | PASS | PASS | PASS | PASS |  |
| Aero | gale_wizard | Tempest | `tempest` | ground_zone | PASS | PASS | PASS | PASS |  |
| Aero | pressure | Air Shot | `air_shot` | projectile | PASS | PASS | PASS | PASS |  |
| Aero | pressure | Bullet Storm | `bullet_storm` | projectile_volley | PASS | PASS | PASS | PASS |  |
| Aero | pressure | Pressure Burst | `pressure_burst` | projectile | PASS | PASS | PASS | PASS |  |
| Aero | pollution | Smog | `smog` | ground_zone | PASS | PASS | PASS | PASS |  |
| Aero | pollution | Toxic Breath | `toxic_breath` | cone | PASS | PASS | PASS | PASS |  |
| Aero | pollution | Acid Rain | `acid_rain` | ground_zone | PASS | PASS | PASS | PASS |  |
| Corruptus | flame | Fireball | `fireball` | projectile | PASS | PASS | PASS | PASS |  |
| Corruptus | flame | Ignite | `ignite` | self_burst | PASS | PASS | PASS | PASS |  |
| Corruptus | flame | Combust | `combust` | execute | PASS | PASS | PASS | PASS |  |
| Corruptus | necro | Raise Dead | `raise_dead` | summon | PASS | PASS | PASS | PASS |  |
| Corruptus | necro | Life Drain | `life_drain` | channel | PASS | PASS | PASS | PASS |  |
| Corruptus | necro | Death Mark | `death_mark` | curse | PASS | PASS | PASS | PASS |  |
| Corruptus | shadow | Shadow Step | `shadow_step` | teleport | PASS | PASS | PASS | PASS |  |
| Corruptus | shadow | Umbral Veil | `umbral_veil` | self_buff | PASS | PASS | PASS | PASS |  |
| Corruptus | shadow | Dark Embrace | `dark_embrace` | self_buff | PASS | PASS | PASS | PASS |  |
| Corruptus | hell_flame | Hellfire | `hellfire` | cone | PASS | PASS | PASS | PASS |  |
| Corruptus | hell_flame | Infernal Ground | `infernal_ground` | ground_zone | PASS | PASS | PASS | PASS |  |
| Corruptus | hell_flame | Soul Scorch | `soul_scorch` | curse | PASS | PASS | PASS | PASS |  |
| Corruptus | mentokinesis | Dominate | `dominate` | gaze | PASS | PASS | PASS | PASS |  |
| Corruptus | mentokinesis | Mind Shatter | `mind_shatter` | self_burst | PASS | PASS | PASS | PASS |  |
| Corruptus | mentokinesis | Hivemind | `hivemind` | self_burst | PASS | PASS | PASS | PASS |  |
| Corruptus | imbuement | Imbue: Power | `imbue_power` | self_buff | PASS | PASS | PASS | PASS |  |
| Corruptus | imbuement | Imbue: Fortitude | `imbue_fortitude` | self_buff | PASS | PASS | PASS | PASS |  |
| Corruptus | imbuement | Imbue: Swiftness | `imbue_swiftness` | self_buff | PASS | PASS | PASS | PASS |  |
| Corruptus | attonement | Sanctuary | `sanctuary` | support_zone | PASS | PASS | PASS | PASS |  |
| Corruptus | attonement | Absorb | `absorb` | self_buff | PASS | PASS | PASS | PASS |  |
| Corruptus | attonement | Purify | `purify` | self_burst | PASS | PASS | PASS | PASS |  |
| Corruptus | void | Rift | `rift` | ground_zone | PASS | PASS | PASS | PASS |  |
| Corruptus | void | Void Spawn | `void_spawn` | summon | PASS | PASS | PASS | PASS |  |
| Corruptus | void | Consume | `consume` | execute | PASS | PASS | PASS | PASS |  |
| Corruptus | scarak | Scarak Egg | `scarak_egg` | summon | PASS | PASS | PASS | PASS |  |
| Corruptus | scarak | Brood Surge | `brood_surge` | summon_buff | PASS | PASS | PASS | PASS |  |
| Corruptus | scarak | Locust Queen | `locust_queen` | summon | PASS | PASS | PASS | PASS |  |
| Corruptus | primordial | Pterodactyl Form | `pterodactyl_form` | transformation | PASS | PASS | PASS | PASS |  |
| Corruptus | primordial | Triceratops Form | `triceratops_form` | transformation | PASS | PASS | PASS | PASS |  |
| Corruptus | primordial | T-Rex Form | `t_rex_form` | transformation | PASS | PASS | PASS | PASS |  |
