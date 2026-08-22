# MOTM Observation Catalog (120 abilities)

## Cast_type legend

| cast_type | semantics (policy/validator) |
|---|---|
| ground_burst | self-centered burst at feet/impact; ground_targets_only may apply |
| ground_zone | persistent AoE field at ground point/self |
| ground_target | aimed ground/enemy point strike |
| ground_strike | delayed vertical strike (pillar/ice shelf) |
| barrier | spawned wall/line barrier volume |
| self_buff | self-applied buff/coating/shield |
| self_burst | self-centered radial pulse |
| projectile / projectile_line / projectile_volley / projectile_burst | ranged travel actors; speed/range authoritative |
| dash / dash_buff / dash_strike / leap / dive_strike / teleport / air_stall | movement casts; dash_distance/travel_type |
| line_control / wave_line | line pull/wave control |
| summon / summon_buff | spawns allied entity; summon_name required |
| transformation | form swap; toggleable exit common |
| channel / gaze / curse / execute / cone / chain / support_zone | specialized single-target or channeled behaviors |

**Subgroups:** SUMMON = summon_name set; TRANSFORM = cast_type=transformation

## TERRA

### quake

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| stomp | Stomp | ground_burst | damage_percent=6; effect="knockback"; terrain_effect="seismic_shockwave"; radius=3; charges=4; cooldown_seconds=2; trigger="jump_land"; target_type="self_centered" | terrain:seismic_shockwave \| overlay:ground_cracks \| fx:knockback \| trigger:jump_land | ability_cast_end stomp; terrain_effect seismic_shockwave; trigger jump_land |
| aftershock | Aftershock | ground_zone | damage_percent=5; effect="slow+knockback"; terrain_effect="lingering_tremor"; radius=8; duration_seconds=2; cooldown_seconds=5; target_type="self_centered" | terrain:lingering_tremor \| overlay:ground_cracks \| fx:slow+knockback | ability_cast_end aftershock; terrain_effect lingering_tremor |
| sinkhole | Sinkhole | ground_target | damage_percent=15; effect="root+dot"; terrain_effect="sinkhole"; radius=3; range=10; duration_seconds=3; cooldown_seconds=6; target_type="enemy" | terrain:sinkhole \| overlay:ground_cracks \| fx:root+dot | ability_cast_end sinkhole; terrain_effect sinkhole |

### metal

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| iron_wall | Iron Wall | barrier | effect="heal"; terrain_effect="iron_wall"; range=7; duration_seconds=4; cooldown_seconds=4; toggleable=true; target_type="line"; heal_percent=10 | terrain:iron_wall \| fx:heal | ability_cast_end iron_wall; terrain_effect iron_wall; toggle on/off |
| metal_coat | Metal Coat | self_buff | effect="defense_buff"; terrain_effect="metal_plating"; duration_seconds=8; cooldown_seconds=8; target_type="self" | terrain:metal_plating \| fx:defense_buff | ability_cast_end metal_coat; terrain_effect metal_plating |
| alloy_enhancement | Alloy Enhancement | self_buff | effect="damage_buff"; duration_seconds=8; target_type="self" | fx:damage_buff | ability_cast_end alloy_enhancement |

### magma

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| lava_pool | Lava Pool | ground_zone | damage_percent=2; effect="burn"; terrain_effect="lava_pool"; radius=5; range=12; duration_seconds=6; cooldown_seconds=4; target_type="self_centered" | terrain:lava_pool \| fx:burn | ability_cast_end lava_pool; terrain_effect lava_pool |
| obsidian_skin | Obsidian Skin | self_buff | effect="shield+damage_reduction"; terrain_effect="obsidian_plates"; duration_seconds=6; cooldown_seconds=10; target_type="self"; shield_percent=20 | terrain:obsidian_plates \| fx:shield+damage_reduction | ability_cast_end obsidian_skin; terrain_effect obsidian_plates |
| magma_sling | Magma Sling | projectile | damage_percent=8; effect="burn+slow"; travel_type="arcing_shot"; range=18; cooldown_seconds=3; projectile_speed=16; target_type="enemy" | travel:arcing_shot \| fx:burn+slow | ability_cast_end magma_sling; projectile travel |

### stone

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| rubble_rouser | Rubble Rouser | self_buff | damage_percent=10; effect="knockback"; travel_type="rubble_followup"; range=16; duration_seconds=8; cooldown_seconds=9; projectile_speed=20; target_type="self" | travel:rubble_followup \| fx:knockback | ability_cast_end rubble_rouser; projectile travel |
| pillar_strike | Pillar Strike | ground_strike | damage_percent=8; effect="stun+knockback"; terrain_effect="stone_pillar"; radius=2.5; range=14; knockup=true; cooldown_seconds=8; target_type="ground_target" | terrain:stone_pillar \| fx:stun+knockback | ability_cast_end pillar_strike; terrain_effect stone_pillar |
| rockslide | Rockslide | dash | damage_percent=4; effect="knockback+grounded"; terrain_effect="ruptured_earth"; radius=2.4; range=5; duration_seconds=1.2; cooldown_seconds=6; dash_distance=5; target_type="self" | terrain:ruptured_earth \| fx:knockback+grounded | ability_cast_end rockslide; terrain_effect ruptured_earth; movement path |

### arbor

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| rooted | Rooted | self_buff | effect="heal"; terrain_effect="root_circle"; duration_seconds=5; cooldown_seconds=6; target_type="self"; heal_percent=10 | terrain:root_circle \| fx:heal | ability_cast_end rooted; terrain_effect root_circle |
| vines | Vines | line_control | damage_percent=1.5; effect="root+dot"; travel_type="thorn_whip"; range=14; duration_seconds=5; target_type="enemy" | travel:thorn_whip \| fx:root+dot | ability_cast_end vines |
| sapling | Sapling | projectile_line | effect="lure"; terrain_effect="sprouting_grove"; range=10; duration_seconds=8; cooldown_seconds=8; target_type="ground_target" | terrain:sprouting_grove \| fx:lure | ability_cast_end sapling; terrain_effect sprouting_grove |

### bloom

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| nightshade | Nightshade | projectile_line | damage_percent=8; effect="dot+slow"; terrain_effect="toxic_spores"; travel_type="nightshade_seed"; radius=5; range=12; duration_seconds=5; cooldown_seconds=4; projectile_speed=18; target_type="ground_target" | terrain:toxic_spores \| travel:nightshade_seed \| fx:dot+slow | ability_cast_end nightshade; terrain_effect toxic_spores; projectile travel |
| frolick | Frolick | self_buff | effect="heal+attack_buff+speed"; duration_seconds=10; cooldown_seconds=6; target_type="self"; heal_percent=5 | fx:heal+attack_buff+speed | ability_cast_end frolick |
| cacti_cluster | Cacti Cluster | projectile | damage_percent=5; effect="dot+slow"; travel_type="cactus_cluster"; radius=4; range=14; duration_seconds=4; cooldown_seconds=5; projectile_speed=10; target_type="enemy" | travel:cactus_cluster \| fx:dot+slow | ability_cast_end cacti_cluster; projectile travel |

### self_petrification

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| gargoyle | Gargoyle | self_buff | effect="heal+damage_reduction+untargetable"; terrain_effect="stone_shell"; duration_seconds=5; cooldown_seconds=7; target_type="self"; heal_percent=35 | terrain:stone_shell \| fx:heal+damage_reduction+untargetable | ability_cast_end gargoyle; terrain_effect stone_shell |
| glare | Glare | gaze | effect="stun"; range=16; duration_seconds=2.5; charges=2; cooldown_seconds=6; target_type="enemy" | fx:stun | ability_cast_end glare |
| tunnel | Tunnel | dash | damage_percent=12; effect="evasion"; terrain_effect="tunnel_path"; travel_type="burrow_strike"; range=10; duration_seconds=5; knockup=true; cooldown_seconds=7; dash_distance=5; target_type="enemy" | terrain:tunnel_path \| travel:burrow_strike \| fx:evasion | ability_cast_end tunnel; terrain_effect tunnel_path; movement path |

### soil

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| burrow | Burrow | dash | damage_percent=7; effect="knockback"; terrain_effect="ruptured_earth"; travel_type="underground_burst"; radius=3; range=9; knockup=true; charges=2; cooldown_seconds=6; dash_distance=4; target_type="enemy" | terrain:ruptured_earth \| travel:underground_burst \| fx:knockback | ability_cast_end burrow; terrain_effect ruptured_earth; movement path |
| mudpit | Mudpit | ground_zone | damage_percent=1; effect="slow+vulnerability"; terrain_effect="mudpit"; radius=5; range=12; duration_seconds=6; cooldown_seconds=2; target_type="ground_target" | terrain:mudpit \| fx:slow+vulnerability | ability_cast_end mudpit; terrain_effect mudpit |
| debris | Debris | projectile_volley | damage_percent=1; effect="vulnerability+blind"; travel_type="debris_spray"; range=10; cooldown_seconds=5; projectile_speed=20; target_type="enemy" | travel:debris_spray \| fx:vulnerability+blind | ability_cast_end debris; projectile travel |

### sand

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| sandstorm | Sandstorm | self_buff | damage_percent=1; effect="dot+slow"; terrain_effect="sandstorm"; radius=5; duration_seconds=10; cooldown_seconds=6; toggleable=true; target_type="self" | terrain:sandstorm \| fx:dot+slow | ability_cast_end sandstorm; terrain_effect sandstorm; toggle on/off |
| dust_devil | Dust Devil | dash | damage_percent=5; effect="knockback"; terrain_effect="dust_devil"; travel_type="rolling_tornado"; radius=5; range=5; duration_seconds=2; cooldown_seconds=5; dash_distance=5; target_type="self_centered" | terrain:dust_devil \| travel:rolling_tornado \| fx:knockback | ability_cast_end dust_devil; terrain_effect dust_devil; movement path |
| vitrification | Vitrification | self_buff | effect="sand_empower"; travel_type="heated_glass_shard"; duration_seconds=8; cooldown_seconds=4; target_type="self" | travel:heated_glass_shard \| fx:sand_empower | ability_cast_end vitrification |

### gem

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| lapidary | Lapidary | ground_target | effect="persistent_object"; terrain_effect="crystal_gem"; duration_seconds=30; cooldown_seconds=7; target_type="ground_target" | terrain:crystal_gem \| fx:persistent_object | ability_cast_end lapidary; terrain_effect crystal_gem |
| fracture | Fracture | ground_burst | damage_percent=40; effect="burst"; terrain_effect="crystal_fracture"; travel_type="crystal_shatter"; radius=20; range=20; cooldown_seconds=8; target_type="self_centered" | terrain:crystal_fracture \| travel:crystal_shatter \| fx:burst | ability_cast_end fracture; terrain_effect crystal_fracture |
| refraction | Refraction | support_zone | effect="damage_reduction+heal+damage_buff"; terrain_effect="crystal_refraction"; radius=20; cooldown_seconds=5; toggleable=true; target_type="self_centered" | terrain:crystal_refraction \| fx:damage_reduction+heal+damage_buff | ability_cast_end refraction; terrain_effect crystal_refraction; toggle on/off |

## HYDRO

### icicle

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| frozen_needles | Frozen Needles | projectile_volley | damage_percent=12; effect="slow_stack"; travel_type="ice_shard_volley"; range=18; cooldown_seconds=2; projectile_speed=28; target_type="enemy" | travel:ice_shard_volley \| fx:slow_stack | ability_cast_end frozen_needles; projectile travel |
| stalactite_crash | Stalactite Crash | ground_strike | damage_percent=20; effect="slow"; terrain_effect="stalactite_crash"; radius=3; range=14; cooldown_seconds=5; target_type="ground_target" | terrain:stalactite_crash \| fx:slow | ability_cast_end stalactite_crash; terrain_effect stalactite_crash |
| skate | Skate | dash_buff | effect="evasion_buff"; terrain_effect="ice_skate_trail"; travel_type="ice_slide"; duration_seconds=4; cooldown_seconds=4; dash_distance=8; target_type="self" | terrain:ice_skate_trail \| travel:ice_slide \| fx:evasion_buff | ability_cast_end skate; terrain_effect ice_skate_trail; movement path |

### snow **[SUMMON]**

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| snow_imp | Snow Imp | summon | damage_percent=5; effect="summon"; terrain_effect="snow_imp_summon"; summon_name="snow_imp"; range=10; duration_seconds=120; cooldown_seconds=5; target_type="ground_target" | terrain:snow_imp_summon \| summon:snow_imp \| fx:summon | ability_cast_end snow_imp; summon spawns snow_imp; terrain_effect snow_imp_summon |
| snowstorm | Snowstorm | ground_zone | damage_percent=3; effect="attack_slow"; terrain_effect="snowstorm"; radius=5; range=12; duration_seconds=6; cooldown_seconds=5; target_type="ground_target" | terrain:snowstorm \| fx:attack_slow | ability_cast_end snowstorm; terrain_effect snowstorm |
| frosty | Frosty | summon | effect="summon_tank"; terrain_effect="frozen_guardian"; summon_name="frosty_golem"; range=10; duration_seconds=60; cooldown_seconds=8; target_type="ground_target" | terrain:frozen_guardian \| summon:frosty_golem \| fx:summon_tank | ability_cast_end frosty; summon spawns frosty_golem; terrain_effect frozen_guardian |

### surf

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| high_tide | High Tide | wave_line | damage_percent=8; effect="knockback+speed"; travel_type="tidal_surge"; range=14; cooldown_seconds=3; target_type="line" | travel:tidal_surge \| fx:knockback+speed | ability_cast_end high_tide |
| waverider | Waverider | self_buff | effect="shield+speed"; travel_type="surf_ride"; duration_seconds=5; cooldown_seconds=4; dash_distance=10; target_type="self"; shield_percent=20 | travel:surf_ride \| fx:shield+speed | ability_cast_end waverider |
| riptide | Riptide | line_control | damage_percent=5; effect="vulnerability"; travel_type="rip_current"; range=15; cooldown_seconds=5; pull_force=5; target_type="enemy" | travel:rip_current \| fx:vulnerability | ability_cast_end riptide; pull_force |

### rain

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| piercing_rain | Piercing Rain | ground_zone | damage_percent=8; effect="dot+attack_buff"; terrain_effect="piercing_rain"; radius=6; range=14; duration_seconds=6; cooldown_seconds=6; target_type="ground_target" | terrain:piercing_rain \| fx:dot+attack_buff | ability_cast_end piercing_rain; terrain_effect piercing_rain |
| rainbow | Rainbow | support_zone | effect="heal"; terrain_effect="healing_rainbow"; radius=5; range=12; duration_seconds=6; cooldown_seconds=8; target_type="ground_target"; heal_percent=10 | terrain:healing_rainbow \| fx:heal | ability_cast_end rainbow; terrain_effect healing_rainbow |
| splash | Splash | projectile_burst | damage_percent=5; effect="shield"; travel_type="splash_burst"; radius=3; range=12; cooldown_seconds=5; projectile_speed=20; target_type="enemy"; shield_percent=20 | travel:splash_burst \| fx:shield | ability_cast_end splash; projectile travel |

### boiling

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| scald | Scald | projectile | damage_percent=10; effect="burn+knockback"; travel_type="boiling_jet"; range=14; cooldown_seconds=3; projectile_speed=24; target_type="enemy" | travel:boiling_jet \| fx:burn+knockback | ability_cast_end scald; projectile travel |
| geyser | Geyser | ground_strike | damage_percent=15; effect="burn+knockback"; terrain_effect="geyser"; radius=3; range=12; knockup=true; cooldown_seconds=5; target_type="ground_target" | terrain:geyser \| fx:burn+knockback | ability_cast_end geyser; terrain_effect geyser |
| overheat | Overheat | self_buff | effect="attack_buff+self_burn"; terrain_effect="steam_pressure"; duration_seconds=8; cooldown_seconds=7; target_type="self" | terrain:steam_pressure \| fx:attack_buff+self_burn | ability_cast_end overheat; terrain_effect steam_pressure |

### vapor

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| vapor_vanish | Vapor Vanish | self_buff | effect="evasion"; terrain_effect="mist_shroud"; duration_seconds=5; cooldown_seconds=5; target_type="self" | terrain:mist_shroud \| fx:evasion | ability_cast_end vapor_vanish; terrain_effect mist_shroud |
| dispersion | Dispersion | dash | damage_percent=10; travel_type="mist_reform"; range=11; charges=3; cooldown_seconds=3; dash_distance=11; target_type="enemy" | travel:mist_reform | ability_cast_end dispersion; movement path |
| hidrosis | Hidrosis | self_buff | effect="evasion"; terrain_effect="condensation_veil"; duration_seconds=7; cooldown_seconds=6; target_type="self" | terrain:condensation_veil \| fx:evasion | ability_cast_end hidrosis; terrain_effect condensation_veil |

### iceberg

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| ice_cap | Ice Cap | self_buff | effect="shield+stun"; terrain_effect="ice_shell"; duration_seconds=6; cooldown_seconds=6; target_type="self"; shield_percent=25 | terrain:ice_shell \| fx:shield+stun | ability_cast_end ice_cap; terrain_effect ice_shell |
| glacier | Glacier | barrier | effect="defense_buff"; terrain_effect="glacier_wall"; range=8; duration_seconds=7; cooldown_seconds=7; target_type="line" | terrain:glacier_wall \| fx:defense_buff | ability_cast_end glacier; terrain_effect glacier_wall |
| ice_shelf | Ice Shelf | ground_strike | damage_percent=20; effect="stun+slow"; terrain_effect="ice_shelf_collapse"; range=14; cooldown_seconds=8; target_type="ground_target" | terrain:ice_shelf_collapse \| fx:stun+slow | ability_cast_end ice_shelf; terrain_effect ice_shelf_collapse |

### saltwater

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| tide_pool | Tide Pool | ground_zone | effect="slow+attack_buff"; terrain_effect="tide_pool"; radius=4; range=10; duration_seconds=6; cooldown_seconds=6; target_type="ground_target" | terrain:tide_pool \| fx:slow+attack_buff | ability_cast_end tide_pool; terrain_effect tide_pool |
| abyssal_assist | Abyssal Assist | ground_burst | damage_percent=12; effect="stun+vulnerability"; terrain_effect="abyssal_pressure"; radius=3; range=14; duration_seconds=2.5; cooldown_seconds=5; target_type="enemy" | terrain:abyssal_pressure \| fx:stun+vulnerability | ability_cast_end abyssal_assist; terrain_effect abyssal_pressure |
| rip_current | Rip Current | line_control | damage_percent=8; effect="slow"; travel_type="undertow_stream"; range=16; duration_seconds=2; cooldown_seconds=4; pull_force=4; target_type="enemy" | travel:undertow_stream \| fx:slow | ability_cast_end rip_current; pull_force |

### freshwater **[SUMMON]**

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| leap_frog | Leap Frog | dash | damage_percent=10; effect="vulnerability"; travel_type="river_leap"; range=9; charges=2; cooldown_seconds=4; dash_distance=9; target_type="enemy" | travel:river_leap \| fx:vulnerability | ability_cast_end leap_frog; movement path |
| river_rapids | River Rapids | self_buff | effect="attack_buff+speed"; travel_type="rapids_ride"; duration_seconds=8; cooldown_seconds=6; dash_distance=12; target_type="self" | travel:rapids_ride \| fx:attack_buff+speed | ability_cast_end river_rapids |
| swamp_monster | Swamp Monster | summon | effect="summon"; terrain_effect="swamp_spawn"; summon_name="swamp_monster"; range=10; duration_seconds=20; cooldown_seconds=8; target_type="ground_target" | terrain:swamp_spawn \| summon:swamp_monster \| fx:summon | ability_cast_end swamp_monster; summon spawns swamp_monster; terrain_effect swamp_spawn |

### bilgewater

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| bilge_dump | Bilge Dump | cone | damage_percent=5; effect="dot+slow"; terrain_effect="bilge_spray"; range=10; duration_seconds=5; cooldown_seconds=5; target_type="cone" | terrain:bilge_spray \| fx:dot+slow | ability_cast_end bilge_dump; terrain_effect bilge_spray |
| anchor_haul | Anchor Haul | projectile_line | damage_percent=15; effect="knockback"; travel_type="anchor_drag"; range=14; cooldown_seconds=6; projectile_speed=18; target_type="enemy" | travel:anchor_drag \| fx:knockback | ability_cast_end anchor_haul; projectile travel |
| oil_spill | Oil Spill | self_buff | effect="defense_buff+attack_buff"; terrain_effect="oily_sheen"; duration_seconds=8; cooldown_seconds=7; target_type="self" | terrain:oily_sheen \| fx:defense_buff+attack_buff | ability_cast_end oil_spill; terrain_effect oily_sheen |

## AERO

### scream

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| shriek | Shriek | cone | effect="deafen"; range=10; duration_seconds=4; cooldown_seconds=3; target_type="cone" | fx:deafen | ability_cast_end shriek |
| sonic_boom | Sonic Boom | wave_line | damage_percent=12; effect="stun+knockback"; travel_type="sonic_wave"; range=14; cooldown_seconds=4; target_type="line" | travel:sonic_wave \| fx:stun+knockback | ability_cast_end sonic_boom |
| battle_cry | Battle Cry | self_buff | effect="attack_buff+speed"; terrain_effect="resonant_aura"; radius=8; duration_seconds=10; cooldown_seconds=15; target_type="self" | terrain:resonant_aura \| fx:attack_buff+speed | ability_cast_end battle_cry; terrain_effect resonant_aura |

### jet

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| jet_burst | Jet Burst | dash | effect="knockback"; travel_type="jet_burst"; range=10; knockup=true; charges=3; cooldown_seconds=2; dash_distance=10; target_type="enemy" | travel:jet_burst \| fx:knockback | ability_cast_end jet_burst; movement path |
| afterburner | Afterburner | dash | damage_percent=15; effect="burn"; terrain_effect="ember_trail"; travel_type="afterburner_dash"; range=14; duration_seconds=8; cooldown_seconds=5; dash_distance=14; target_type="line" | terrain:ember_trail \| travel:afterburner_dash \| fx:burn | ability_cast_end afterburner; terrain_effect ember_trail; movement path |
| mach_punch | Mach Punch | dash_strike | damage_percent=20; effect="stun_if_wall"; travel_type="mach_punch"; range=8; cooldown_seconds=3; dash_distance=8; target_type="enemy" | travel:mach_punch \| fx:stun_if_wall | ability_cast_end mach_punch; movement path |

### thunder

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| thunderclap | Thunderclap | self_burst | effect="stun+shocked"; terrain_effect="thunderclap"; radius=5; duration_seconds=3.5; cooldown_seconds=4; target_type="self_centered" | terrain:thunderclap \| fx:stun+shocked | ability_cast_end thunderclap; terrain_effect thunderclap |
| smite | Smite | projectile | damage_percent=20; effect="lightning"; travel_type="lightning_bolt"; radius=1.5; range=18; charges=2; cooldown_seconds=3; projectile_speed=30; target_type="enemy" | travel:lightning_bolt \| fx:lightning | ability_cast_end smite; projectile travel |
| chain_lightning | Chain Lightning | chain | damage_percent=10; effect="dot"; travel_type="chain_lightning"; radius=3; range=16; duration_seconds=4; cooldown_seconds=5; target_type="enemy_cluster" | travel:chain_lightning \| fx:dot | ability_cast_end chain_lightning |

### tornado

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| twister | Twister | ground_zone | damage_percent=10; effect="knockback"; travel_type="twister"; radius=3; range=12; duration_seconds=4; cooldown_seconds=3; target_type="ground_target" | travel:twister \| fx:knockback | ability_cast_end twister |
| funnel_cloud | Funnel Cloud | ground_zone | damage_percent=8; effect="dot"; terrain_effect="funnel_cloud"; radius=5; range=14; duration_seconds=6; cooldown_seconds=5; pull_force=3; target_type="ground_target" | terrain:funnel_cloud \| fx:dot | ability_cast_end funnel_cloud; terrain_effect funnel_cloud; pull_force |
| eye_of_the_storm | Eye of the Storm | self_buff | effect="heal+shield"; terrain_effect="eye_of_the_storm"; duration_seconds=6; cooldown_seconds=7; target_type="self"; heal_percent=10; shield_percent=15 | terrain:eye_of_the_storm \| fx:heal+shield | ability_cast_end eye_of_the_storm; terrain_effect eye_of_the_storm |

### jump

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| leap | Leap | leap | travel_type="jump_arc"; radius=3; range=10; duration_seconds=5; charges=2; cooldown_seconds=4; dash_distance=10; target_type="enemy" | travel:jump_arc | ability_cast_end leap; movement path |
| divebomb | Divebomb | dive_strike | damage_percent=20; effect="slow"; travel_type="divebomb"; radius=4; range=12; cooldown_seconds=6; target_type="ground_target" | travel:divebomb \| fx:slow | ability_cast_end divebomb; movement path |
| hang_time | Hang Time | air_stall | effect="evasion"; travel_type="hang_time"; duration_seconds=3; charges=2; cooldown_seconds=5; target_type="self" | travel:hang_time \| fx:evasion | ability_cast_end hang_time; movement path |

### wind_blade

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| air_slash | Air Slash | projectile_line | damage_percent=8; travel_type="wind_blade"; range=16; cooldown_seconds=2; projectile_speed=28; target_type="enemy" | travel:wind_blade | ability_cast_end air_slash; projectile travel |
| gale_cutter | Gale Cutter | projectile_line | damage_percent=12; effect="knockback"; travel_type="gale_cutter"; range=18; cooldown_seconds=5; projectile_speed=30; target_type="enemy" | travel:gale_cutter \| fx:knockback | ability_cast_end gale_cutter; projectile travel |
| razor_wind | Razor Wind | projectile_volley | damage_percent=5; travel_type="razor_wind"; range=12; cooldown_seconds=6; projectile_speed=34; target_type="enemy_cluster" | travel:razor_wind | ability_cast_end razor_wind; projectile travel |

### smoke **[TRANSFORM]**

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| smoke_bomb | Smoke Bomb | ground_zone | effect="blind+slow"; terrain_effect="smoke_bomb"; radius=5; range=10; duration_seconds=6; cooldown_seconds=5; target_type="ground_target" | terrain:smoke_bomb \| fx:blind+slow | ability_cast_end smoke_bomb; terrain_effect smoke_bomb |
| vanish | Vanish | self_buff | effect="stealth"; terrain_effect="vanish"; duration_seconds=3; cooldown_seconds=6; target_type="self" | terrain:vanish \| fx:stealth | ability_cast_end vanish; terrain_effect vanish |
| smoke_form | Smoke Form | transformation | effect="evasion"; travel_type="smoke_form"; duration_seconds=5; cooldown_seconds=7; toggleable=true; target_type="self" | travel:smoke_form \| fx:evasion | ability_cast_end smoke_form; form state toggles; toggle on/off |

### gale_wizard

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| gust | Gust | projectile | damage_percent=12; effect="knockback"; travel_type="gust_blast"; range=16; cooldown_seconds=4; projectile_speed=26; target_type="enemy" | travel:gust_blast \| fx:knockback | ability_cast_end gust; projectile travel |
| cyclone_shield | Cyclone Shield | self_buff | effect="defense_buff+shield"; terrain_effect="cyclone_shield"; duration_seconds=6; cooldown_seconds=6; target_type="self"; shield_percent=15 | terrain:cyclone_shield \| fx:defense_buff+shield | ability_cast_end cyclone_shield; terrain_effect cyclone_shield |
| tempest | Tempest | ground_zone | damage_percent=15; effect="stun"; terrain_effect="tempest"; radius=6; range=14; duration_seconds=4; cooldown_seconds=8; pull_force=3; target_type="ground_target" | terrain:tempest \| fx:stun | ability_cast_end tempest; terrain_effect tempest; pull_force |

### pressure

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| air_shot | Air Shot | projectile | damage_percent=15; travel_type="compressed_air_shot"; range=15; cooldown_seconds=3; projectile_speed=32; target_type="enemy" | travel:compressed_air_shot | ability_cast_end air_shot; projectile travel |
| bullet_storm | Bullet Storm | projectile_volley | damage_percent=12; effect="slow"; travel_type="air_bullets"; range=15; cooldown_seconds=5; projectile_speed=34; target_type="enemy" | travel:air_bullets \| fx:slow | ability_cast_end bullet_storm; projectile travel |
| pressure_burst | Pressure Burst | projectile | damage_percent=20; effect="knockback"; travel_type="pressure_burst"; range=20; duration_seconds=4; cooldown_seconds=7; projectile_speed=32; target_type="enemy" | travel:pressure_burst \| fx:knockback | ability_cast_end pressure_burst; projectile travel |

### pollution

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| smog | Smog | ground_zone | damage_percent=5; effect="dot+slow"; terrain_effect="smog_cloud"; radius=5; range=12; duration_seconds=6; cooldown_seconds=5; target_type="ground_target" | terrain:smog_cloud \| fx:dot+slow | ability_cast_end smog; terrain_effect smog_cloud |
| toxic_breath | Toxic Breath | cone | damage_percent=8; effect="dot+vulnerability"; terrain_effect="toxic_breath"; range=8; duration_seconds=5; cooldown_seconds=6; target_type="cone" | terrain:toxic_breath \| fx:dot+vulnerability | ability_cast_end toxic_breath; terrain_effect toxic_breath |
| acid_rain | Acid Rain | ground_zone | damage_percent=10; effect="dot+vulnerability"; terrain_effect="acid_rain"; radius=5; range=14; duration_seconds=6; cooldown_seconds=8; target_type="ground_target" | terrain:acid_rain \| fx:dot+vulnerability | ability_cast_end acid_rain; terrain_effect acid_rain |

## CORRUPTUS

### flame

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| fireball | Fireball | projectile | damage_percent=8; effect="burn"; travel_type="explosive_fireball"; radius=3; range=18; charges=3; cooldown_seconds=2; projectile_speed=24; target_type="enemy" | travel:explosive_fireball \| fx:burn | ability_cast_end fireball; projectile travel |
| ignite | Ignite | self_burst | damage_percent=12; effect="self_burn+aoe"; terrain_effect="living_flame"; radius=4; duration_seconds=4; cooldown_seconds=4; target_type="self_centered" | terrain:living_flame \| fx:self_burn+aoe | ability_cast_end ignite; terrain_effect living_flame |
| combust | Combust | execute | damage_percent=25; effect="consume_burn"; range=14; cooldown_seconds=5; target_type="enemy" | fx:consume_burn | ability_cast_end combust |

### necro **[SUMMON]**

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| raise_dead | Raise Dead | summon | damage_percent=5; effect="summon"; terrain_effect="grave_rise"; summon_name="skeleton_minion"; range=10; duration_seconds=20; cooldown_seconds=3; target_type="ground_target" | terrain:grave_rise \| summon:skeleton_minion \| fx:summon | ability_cast_end raise_dead; summon spawns skeleton_minion; terrain_effect grave_rise |
| life_drain | Life Drain | channel | damage_percent=10; effect="lifesteal"; range=14; duration_seconds=4; cooldown_seconds=4; target_type="enemy" | fx:lifesteal | ability_cast_end life_drain |
| death_mark | Death Mark | curse | effect="vulnerability"; range=16; duration_seconds=8; cooldown_seconds=5; target_type="enemy" | fx:vulnerability | ability_cast_end death_mark |

### shadow **[SUMMON]**

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| shadow_step | Shadow Step | teleport | effect="stealth"; summon_name="shadow_clone"; travel_type="shadow_step"; range=12; duration_seconds=4; charges=2; cooldown_seconds=3; dash_distance=12; target_type="self" | travel:shadow_step \| summon:shadow_clone \| fx:stealth | ability_cast_end shadow_step; summon spawns shadow_clone; movement path |
| umbral_veil | Umbral Veil | self_buff | effect="stealth"; terrain_effect="umbral_shroud"; duration_seconds=5; cooldown_seconds=5; target_type="self" | terrain:umbral_shroud \| fx:stealth | ability_cast_end umbral_veil; terrain_effect umbral_shroud |
| dark_embrace | Dark Embrace | self_buff | effect="defense_buff+heal"; terrain_effect="shadow_zone"; range=10; duration_seconds=6; cooldown_seconds=6; target_type="self" | terrain:shadow_zone \| fx:defense_buff+heal | ability_cast_end dark_embrace; terrain_effect shadow_zone |

### hell_flame

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| hellfire | Hellfire | cone | damage_percent=8; effect="burn+slow"; terrain_effect="blue_hellfire_breath"; range=9; duration_seconds=5; cooldown_seconds=4; target_type="cone" | terrain:blue_hellfire_breath \| fx:burn+slow | ability_cast_end hellfire; terrain_effect blue_hellfire_breath |
| infernal_ground | Infernal Ground | ground_zone | damage_percent=5; effect="burn+attack_buff"; terrain_effect="infernal_ground"; radius=5; duration_seconds=7; cooldown_seconds=7; target_type="self_centered" | terrain:infernal_ground \| fx:burn+attack_buff | ability_cast_end infernal_ground; terrain_effect infernal_ground |
| soul_scorch | Soul Scorch | curse | damage_percent=8; effect="dot+vulnerability"; range=16; duration_seconds=6; cooldown_seconds=8; target_type="enemy" | fx:dot+vulnerability | ability_cast_end soul_scorch |

### mentokinesis

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| dominate | Dominate | gaze | effect="root+disoriented"; range=15; duration_seconds=999; cooldown_seconds=5; target_type="enemy" | fx:root+disoriented | ability_cast_end dominate |
| mind_shatter | Mind Shatter | self_burst | damage_percent=25; effect="stun"; terrain_effect="psychic_shatter"; radius=6; cooldown_seconds=6; target_type="self_centered" | terrain:psychic_shatter \| fx:stun | ability_cast_end mind_shatter; terrain_effect psychic_shatter |
| hivemind | Hivemind | self_burst | effect="root+disoriented"; terrain_effect="psychic_link"; radius=7; duration_seconds=6; cooldown_seconds=12; target_type="self_centered" | terrain:psychic_link \| fx:root+disoriented | ability_cast_end hivemind; terrain_effect psychic_link |

### imbuement

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| imbue_power | Imbue: Power | self_buff | effect="attack_buff"; duration_seconds=8; cooldown_seconds=6; target_type="self" | fx:attack_buff | ability_cast_end imbue_power |
| imbue_fortitude | Imbue: Fortitude | self_buff | effect="defense_buff+heal"; terrain_effect="abyssal_armor"; duration_seconds=8; cooldown_seconds=6; target_type="self"; heal_percent=10 | terrain:abyssal_armor \| fx:defense_buff+heal | ability_cast_end imbue_fortitude; terrain_effect abyssal_armor |
| imbue_swiftness | Imbue: Swiftness | self_buff | effect="evasion+attack_buff"; travel_type="shadow_haste"; duration_seconds=8; cooldown_seconds=6; dash_distance=9; target_type="self" | travel:shadow_haste \| fx:evasion+attack_buff | ability_cast_end imbue_swiftness |

### attonement

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| sanctuary | Sanctuary | support_zone | effect="heal"; terrain_effect="sanctuary"; radius=5; range=12; duration_seconds=6; cooldown_seconds=7; target_type="ground_target"; heal_percent=15 | terrain:sanctuary \| fx:heal | ability_cast_end sanctuary; terrain_effect sanctuary |
| absorb | Absorb | self_buff | effect="defense_buff+heal"; duration_seconds=5; cooldown_seconds=6; target_type="self"; heal_percent=10 | fx:defense_buff+heal | ability_cast_end absorb |
| purify | Purify | self_burst | effect="burn+vulnerability"; terrain_effect="purifying_aura"; radius=6; duration_seconds=6; cooldown_seconds=8; target_type="self_centered" | terrain:purifying_aura \| fx:burn+vulnerability | ability_cast_end purify; terrain_effect purifying_aura |

### void **[SUMMON]**

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| rift | Rift | ground_zone | damage_percent=8; effect="dot+slow"; terrain_effect="void_rift"; radius=5; range=14; duration_seconds=6; cooldown_seconds=6; pull_force=4; target_type="ground_target" | terrain:void_rift \| fx:dot+slow | ability_cast_end rift; terrain_effect void_rift; pull_force |
| void_spawn | Void Spawn | summon | effect="summon"; terrain_effect="void_gate"; summon_name="crawler_void"; duration_seconds=10; cooldown_seconds=8; target_type="self_centered" | terrain:void_gate \| summon:crawler_void \| fx:summon | ability_cast_end void_spawn; summon spawns crawler_void; terrain_effect void_gate |
| consume | Consume | execute | damage_percent=30; effect="lifesteal"; range=12; duration_seconds=2.5; cooldown_seconds=9; target_type="enemy" | fx:lifesteal | ability_cast_end consume |

### scarak **[SUMMON]**

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| scarak_egg | Scarak Egg | summon | effect="summon"; terrain_effect="brood_nest"; summon_name="scarak_egg"; range=8; duration_seconds=30; cooldown_seconds=8; target_type="ground_target" | terrain:brood_nest \| summon:scarak_egg \| fx:summon | ability_cast_end scarak_egg; summon spawns scarak_egg; terrain_effect brood_nest |
| brood_surge | Brood Surge | summon_buff | effect="attack_buff"; radius=12; duration_seconds=6; cooldown_seconds=7; target_type="allied_summons" | fx:attack_buff | ability_cast_end brood_surge |
| locust_queen | Locust Queen | summon | effect="summon"; terrain_effect="swarm_gate"; summon_name="locust_queen"; range=10; duration_seconds=20; cooldown_seconds=10; target_type="ground_target" | terrain:swarm_gate \| summon:locust_queen \| fx:summon | ability_cast_end locust_queen; summon spawns locust_queen; terrain_effect swarm_gate |

### primordial **[TRANSFORM]**

| id | name | cast_type | key mechanics | OBSERVE | TEST CONDITION |
|---|---|---|---|---|---|
| pterodactyl_form | Pterodactyl Form | transformation | damage_percent=10; effect="evasion+attack_buff"; travel_type="flight_form"; duration_seconds=30; cooldown_seconds=8; toggleable=true; target_type="self" | travel:flight_form \| fx:evasion+attack_buff | ability_cast_end pterodactyl_form; form state toggles; toggle on/off |
| triceratops_form | Triceratops Form | transformation | damage_percent=12; effect="defense_buff"; travel_type="stampede_charge"; duration_seconds=30; cooldown_seconds=8; toggleable=true; dash_distance=10; target_type="self" | travel:stampede_charge \| fx:defense_buff | ability_cast_end triceratops_form; form state toggles; toggle on/off |
| t_rex_form | T-Rex Form | transformation | damage_percent=20; effect="attack_buff+stun"; terrain_effect="primal_roar"; radius=4; duration_seconds=30; cooldown_seconds=10; toggleable=true; target_type="self" | terrain:primal_roar \| fx:attack_buff+stun | ability_cast_end t_rex_form; form state toggles; terrain_effect primal_roar; toggle on/off |

