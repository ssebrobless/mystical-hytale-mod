# Ability Completion Checklist

```text
Current Truth
╔══════════════════════════════════════════════════════════════════════╗
║ 120 total abilities                                                ║
╠═══════════════════════════════ Live Now ════════════════════════════╣
║ selectable by class/style                                          ║
║ MOTM cooldown + recovery timing                                    ║
║ resource spending                                                  ║
║ generic damage / heal / shield / buff / debuff resolution          ║
║ generic cone / line / area / chain hit checks                      ║
║ movement casts                                                     ║
║ some summons                                                       ║
║ some transformation visuals                                        ║
╠══════════════════════════ Still Partial / Missing ══════════════════╣
║ true moving projectile entities                                    ║
║ persistent zones / barriers / terrain hazards                      ║
║ pull-force mechanics                                               ║
║ summon AI roles and bespoke behavior                               ║
║ transformation gameplay state                                      ║
║ special mechanics like consume-burn and true channeling            ║
╚══════════════════════════════════════════════════════════════════════╝
```

## Legend

| Label | Meaning |
| --- | --- |
| `Playable` | Works in live gameplay now through the current MOTM runtime. |
| `Partial` | Cast works now, but the advertised fantasy is not fully represented yet. |
| `Gap` | The ability has a specific mechanic in data that the runtime does not truly implement yet. |

## Runtime Matrix

| Ability Field / Mechanic | State | Current Truth |
| --- | --- | --- |
| `damage_percent` / `heal_percent` / `shield_percent` | `Live` | Applied in gameplay now. |
| `range` / `radius` / `width` / `cone_angle` | `Live` | Used by generic hit resolution now. |
| `dash_distance` / `launch_height` / `knockback_force` | `Live` | Used for movement and knockback now. |
| `duration_seconds` | `Live` | Used for buffs, debuffs, summons, cast windows, and persistent battlefield volumes, including delayed trap windows. |
| `projectile_speed` | `Partial` | Used now for moving server-side projectile travel time, stagger cadence, impact timing, dodge windows, and style-aware proxy travel visuals, but these are still not native bespoke projectile actors. |
| `travel_type` | `Gap` | Currently presentation / flavor only. |
| `terrain_effect` | `Partial` | Now influences persistent hazard behavior, owner-side field buffs, and denser style-aware field/barrier loop proxies, but still does not spawn fully bespoke world actors. |
| `pull_force` | `Partial` | Used now by line-control pulls and persistent vortex/current fields; later passes should keep polishing per-style feel and physics. |
| `summon_name` | `Partial` | Summons now spawn, hatch, lock targets more intelligently, use role-aware targeting/pressure, and have stronger summon-specific riders, but they still need deeper bespoke AI and polish. |
| `cast_type=transformation` | `Partial` | Forms now create active gameplay states with damage/movement/weapon-rider modifiers, periodic form pressure, owner-side stance refreshes, and movement-driven collision pressure, but they still need cleaner exit rules and final bespoke polish. |
| `blind` / `disoriented` / `grounded` | `Live` | These now affect live gameplay more directly: blind/disoriented reduce outgoing damage pressure, and grounded blocks mobility/airborne casts. |

## Cross-Cutting Completion Passes

```text
Finish Order
1. Projectile Entity Pass
2. Persistent Zone / Barrier Pass
3. Pull / Push / Battlefield Control Pass
4. Summon AI Role Pass
5. Transformation State Pass
6. Special Mechanic Pass
7. Weapon Follow-Up / Enhancer Pass
8. Final Style-by-Style Validation
```

### 1. Projectile Entity Pass

- [x] Replace trace-only projectile resolution with moving server-side projectile runtimes for:
  `sonic_boom`, `smite`, `chain_lightning`, `air_slash`, `gale_cutter`, `gust`, `air_shot`, `bullet_storm`, `fireball`, `hellfire`, `mind_shatter`, `frozen_needles`, `high_tide`, `riptide`, `splash`, `scald`, `rip_current`, `magma_sling`, `rubble_rouser`, `vines`, `cacti_cluster`, `debris`, `vitrification`, `fracture`, `anchor_haul`.
- [x] Make `projectile_speed` authoritative for travel time, impact timing, and dodge windows.
- [x] Spawn distinct travel VFX proxies instead of only instantaneous cast/impact summaries.

### 2. Persistent Zone / Barrier Pass

- [x] Turn `ground_zone`, `support_zone`, and `barrier` abilities into real persistent world volumes with visible loop proxies.
- [ ] Affected abilities:
  `twister`, `funnel_cloud`, `smoke_bomb`, `tempest`, `smog`, `acid_rain`, `dark_embrace`, `infernal_ground`, `sanctuary`, `rift`, `snowstorm`, `piercing_rain`, `rainbow`, `glacier`, `tide_pool`, `aftershock`, `iron_wall`, `lava_pool`, `rockslide`, `mudpit`, `sandstorm`, `dust_devil`.
- [x] Re-apply effects over time while targets stay inside the zone.
- [ ] Use `terrain_effect` to spawn the actual zone / wall / hazard representation.

### 3. Pull / Push / Battlefield Control Pass

- [x] Implement `pull_force` for:
  `funnel_cloud`, `tempest`, `rift`, `riptide`, `rip_current`.
- [x] Improve line-control abilities so they feel like pulls / drags / currents instead of generic damage lines.
- [x] Revisit `stun_if_wall` on `mach_punch` so wall collision matters instead of only generic stun logic.

### 4. Summon AI Role Pass

- [x] Give summons real combat roles, target selection, and attack behavior.
- [x] Affected summon kit:
  `raise_dead`, `void_spawn`, `scarak_egg`, `brood_surge`, `locust_queen`, `snow_imp`, `frosty`, `swamp_monster`, `sapling`, plus the `shadow_step` clone.
- [ ] Add role-specific behavior:
  [x] `frosty` should actually taunt / tank.
  [x] `shadow_clone` should act like a clone, not just a spawned shell.
  [x] `brood_surge` should do more than extend/flash summons.

### 5. Transformation State Pass

- [x] Make forms more than effect/model swaps for:
  `smoke_form`, `pterodactyl_form`, `triceratops_form`, `t_rex_form`.
- [x] Apply form-specific locomotion pressure and collision behavior so movement through combat matters while transformed.
- [ ] Add the remaining form-specific exit rules and any missing combat permission edge cases.
- [ ] Finish the last movement-feel polish and collision tuning pass for each form.

### 6. Special Mechanic Pass

- [x] Implement true `consume_burn` behavior for `combust`.
- [x] Implement true channeled damage / sustain logic for `life_drain`.
- [x] Keep `dominate` as a stronger control debuff by layering stun/vulnerability with extra root/disorientation pressure.
- [x] Add actual speed / haste support for `battle_cry` and `waverider`.
- [x] Give `consume` a stronger void-finisher profile by amplifying it harder on weakened or debuffed targets.

### 7. Weapon Follow-Up / Enhancer Pass

- [x] Make melee / ranged enhancement abilities explicitly feed into weapon follow-up windows.
- [x] Prioritize self-buff / empowerment styles such as:
  `metal`, `imbuement`, `attonement`, `vapor`, `gem`, `self_petrification`, `bloom`.
- [x] Ensure these buffs clearly affect normal Hytale weapon combat after swapping off the spellbook.

### 8. Final Style-by-Style Validation

- [ ] For every style, verify:
  cast timing, cooldown, resource drain, HUD slot state, hit resolution, VFX, summary text, and edge-case behavior.

## Style Queue

### Terra

| Style | Current | Finish Next |
| --- | --- | --- |
| `quake` | `Playable` | `aftershock` now lingers and `sinkhole` now behaves like a delayed trap field; revisit later only for richer visible terrain collapse representation. |
| `metal` | `Partial` | Turn `iron_wall` into a real spawned barrier; keep `metal_coat` and `alloy_enhancement` as weapon-follow-up buff validation targets. |
| `magma` | `Partial` | Make `lava_pool` persist and punish standing in it; convert `magma_sling` to a real projectile actor. |
| `stone` | `Partial` | `rockslide` now arms and pulses like a falling-rock hazard; keep `rubble_rouser` as the main validation target for the projectile pass. |
| `arbor` | `Partial` | `sapling` now has a real treant-summon role and owner-protection payoff; `vines` remains the main validation target for further control polish. |
| `bloom` | `Partial` | `cacti_cluster` now fires a staggered volley instead of a flat burst; validate whether `frolick` should influence weapon follow-up or movement more strongly. |
| `self_petrification` | `Playable` | `gargoyle`, `glare`, and `tunnel` already fit the current runtime well; revisit later if `gargoyle` should gain fuller form-state behavior. |
| `soil` | `Partial` | `mudpit` now persists as a control zone and `debris` now fires a staggered spray; later polish the visible debris actor feel. |
| `sand` | `Partial` | Make `sandstorm` and `dust_devil` true persistent battlefield hazards; convert `vitrification` into a projectile actor. |
| `gem` | `Partial` | Convert `fracture` into a true line projectile / crystal shard actor; validate buff interaction on `lapidary` and `refraction`. |

### Hydro

| Style | Current | Finish Next |
| --- | --- | --- |
| `icicle` | `Partial` | `frozen_needles` now fires as a staggered shard volley; keep `stalactite_crash` and `skate` as validation targets after projectile polish. |
| `snow` | `Partial` | `snowstorm` now persists as a real area, and `snow_imp` / `frosty` now have clearer summon behavior; keep `frosty` tank feel as the main validation target. |
| `surf` | `Partial` | `riptide` now behaves more like a current pull and `high_tide` now sweeps through targets more like a traveling wave; `waverider` remains a validation target for speed support. |
| `rain` | `Partial` | `piercing_rain` and `rainbow` now behave more like distinct weather/support fields, and `splash` now fires as a broader staggered burst; later polish the water-arc presentation. |
| `boiling` | `Partial` | Convert `scald` into a projectile actor; validate `geyser` launch behavior and `overheat` weapon-follow-up synergy. |
| `vapor` | `Playable` | `vapor_vanish`, `dispersion`, and `hidrosis` already fit the current runtime reasonably well; revisit only after movement polish. |
| `iceberg` | `Partial` | `glacier` now behaves more like a slowing fortified barrier, but it still wants final wall/actor polish; keep `ice_cap` and `ice_shelf` in the validation pass. |
| `saltwater` | `Partial` | `tide_pool` now persists with stronger owner-side support and `rip_current` now behaves more like a sustained drag; validate `abyssal_assist` after control/support validation. |
| `freshwater` | `Partial` | `leap_frog` is already usable, and `swamp_monster` now has a real bruiser/control summon loop; validate whether `river_rapids` should gain stronger movement/weapon synergy. |
| `bilgewater` | `Partial` | `anchor_haul` now behaves like a hooked drag instead of generic knockback; validate `bilge_dump` cone behavior and decide whether `oil_spill` should become a terrain hazard or remain a self buff. |

### Aero

| Style | Current | Finish Next |
| --- | --- | --- |
| `scream` | `Partial` | `sonic_boom` now sweeps through targets more like a traveling wave; `battle_cry` still needs final speed/support validation. |
| `jet` | `Playable` | `jet_burst`, `afterburner`, and `mach_punch` already work well enough to play-test; later polish dash collision and wall-stun behavior. |
| `thunder` | `Partial` | `chain_lightning` now behaves more like a chained strike with falloff and a shocked rider, and `smite` now chains to nearby targets more like lightning; both still need true lightning actors for final polish. |
| `tornado` | `Partial` | `twister` and `funnel_cloud` now behave more like persistent vortex/control zones, but they still need final world-volume/actor polish. |
| `jump` | `Playable` | `leap`, `divebomb`, and `hang_time` are already strong validation targets for movement timing; revisit later only for movement polish. |
| `wind_blade` | `Partial` | `air_slash` and `gale_cutter` now cut through targets more like wind blades, but they still need final projectile actor polish. |
| `smoke` | `Partial` | `smoke_bomb` now behaves more like a real obscuring zone and `smoke_form` is a real transformation state, but both still need final presentation polish. |
| `gale_wizard` | `Partial` | Convert `gust` into a projectile actor; make `tempest` a real pull/stun storm field with `pull_force`. |
| `pressure` | `Partial` | `bullet_storm` now fires with a distinct staggered volley cadence; `air_shot` still wants the final visible projectile-actor pass, and `pressure_burst` remains a good follow-up validation case. |
| `pollution` | `Partial` | `smog` and `acid_rain` now behave like persistent toxic hazards with extra rider pressure; later polish them with fuller visible hazard actors. |

### Corruptus

| Style | Current | Finish Next |
| --- | --- | --- |
| `flame` | `Partial` | Convert `fireball` into a true projectile actor; implement real `consume_burn` logic for `combust`. |
| `necro` | `Partial` | `raise_dead` now has a real skirmisher summon loop and `life_drain` is a true channel; keep `death_mark` in the validation pass. |
| `shadow` | `Partial` | `shadow_step` now leaves a more convincing strike clone, and `dark_embrace` persists while refreshing owner-side evasion; later polish clone presentation. |
| `hell_flame` | `Partial` | `infernal_ground` now persists as a burning zone and refreshes its owner-side attack buff while inside it; keep `hellfire` as the main projectile validation target. |
| `mentokinesis` | `Partial` | `dominate` now acts as a stronger control debuff; `mind_shatter` still needs the real projectile actor pass. |
| `imbuement` | `Playable` | This style already fits the current self-buff runtime well; use it as a weapon-enhancer validation target later. |
| `attonement` | `Partial` | `sanctuary` now behaves more like a true cleansing support field; keep `absorb` and `purify` as validation targets after zone/support work. |
| `void` | `Partial` | `rift` now behaves more like a persistent pulling void zone with extra vulnerability pressure; keep `void_spawn` summon AI and `consume` execute feel in the special-mechanic pass. |
| `scarak` | `Partial` | `scarak_egg` and `locust_queen` now have clearer summon behavior, and `brood_surge` now feels more like a commander skill; later polish swarm presentation and egg hatch visuals. |
| `primordial` | `Partial` | Forms now have distinct pulse, weapon-hit, and movement-collision payoffs plus active owner-side stance refreshes, but they still need final transformation exit-rule and feel polish. |

## First Recommended Finish Order

```text
Most Efficient Next Sprint
╔══════════════════════════════════════════════════════════════╗
║ 1. Projectile entity pass                                  ║
║ 2. Persistent zone / barrier pass                          ║
║ 3. Pull-force + control pass                               ║
║ 4. Summon AI roles                                         ║
║ 5. Transformation states                                   ║
║ 6. Special mechanics: consume-burn, channel, speed, clone  ║
║ 7. Weapon follow-up enhancer validation                    ║
╚══════════════════════════════════════════════════════════════╝
```

## Working Definition Of "Finished"

- [ ] The ability casts from the spellbook with MOTM timing, not weapon timing.
- [ ] The HUD slot reflects cast, recovery, cooldown, and ready state.
- [ ] The ability creates the correct spatial gameplay shape in-world.
- [ ] The advertised secondary mechanic actually happens.
- [ ] The visuals and model behavior match the ability fantasy.
- [ ] The ability still works after swapping to a weapon when it is meant to empower follow-up combat.
- [ ] The spellbook summary text matches real behavior, not planned behavior.
