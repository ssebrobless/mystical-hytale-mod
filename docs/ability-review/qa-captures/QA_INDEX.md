# In-Game Visual QA — Recording Harness Output (2026-08-21)

**What this is.** A resumable, crash-recovering capture harness drove the live
Hytale client (world `MOTM Creative Test`, v0.5.9) and cast **all 120 abilities**
(4 classes × 30) against a close-spawned dummy, grabbing **6 burst frames** per
ability. Output = four labeled **contact sheets** you scrub by eye, plus an
**advisory** automated first-pass that flags which abilities showed a clear VFX.

## Deliverables (scrub these)
- `CONTACT_terra.png` — 30 abilities × 6 frames, labeled (name / style / id)
- `CONTACT_hydro.png`
- `CONTACT_aero.png`
- `CONTACT_corruptus.png`
- Raw frames + capture manifests: `_cap_<class>.json`, `_seq_<class>.json`
- Slices used for the vision pass: `_slice_<class>_<0..2>.png`

## How to read the advisory pass
- Automated verdicts come from a strict, **by-row-number** vision pass over each
  sheet (name-based passes were discarded after a labeling-mismatch check).
- **`— none` does NOT mean broken.** Scripted 6-frame sampling misses effects that
  are (a) sub-second transient, (b) play on the target NPC off the fixed forward
  camera, or (c) self-auras that read faintly in a static frame. Treat `✅ seen`
  as high-confidence "renders"; treat `— none` as "needs your eyes on the sheet."
- Automated **✅ seen** count: **23 / 120** (terra 3, hydro 5, aero 4, corruptus 11).
  The high-confidence hits are dominated by projectiles, summons, transforms, and
  crystal/tornado structures — exactly the non-transient VFX a still frame catches.

## ⚠ Real bug found by the harness
- **Swamp Monster (hydro / freshwater)** — casting it **crashes the client**
  (`NullReferenceException`, renderer) seconds after the summon spawns. Snow Imp
  and Frosty summons captured fine, so it is specific to the Swamp Monster
  summon model/effect. Full detail in `_crashes.md`. The harness auto-recovered
  (relaunch + resume) and skipped it on continuation.

## Known automated-pass caveats (human scrub overrides)
- `Shriek / Sonic Boom / Battle Cry` (aero): an earlier pass saw coloured
  sound-wave rings; the strict pass logged none. **Scrub these first.**
- `T-Rex Form` (corruptus): strict pass logged none, but a dedicated in-world test
  earlier **confirmed** the model swaps to a reddish primordial beast. Renders.
- `Lava Pool` (terra): field-ring particle proxy is intentionally suppressed
  (client-crash avoidance), so no ground VFX is expected — not a defect.

## Terra
| # | Ability | Style | Auto-VFX | Note / flag |
|---|---------|-------|----------|-------------|
| 1 | Stomp | quake | — none |  |
| 2 | Aftershock | quake | — none |  |
| 3 | Sinkhole | quake | — none |  |
| 4 | Iron Wall | metal | — none |  |
| 5 | Metal Coat | metal | — none |  |
| 6 | Alloy Enhancement | metal | — none |  |
| 7 | Lava Pool | magma | — none | field-ring proxy suppressed by design (client crash avoidance) - no ground VFX expected |
| 8 | Obsidian Skin | magma | — none |  |
| 9 | Magma Sling | magma | — none |  |
| 10 | Rubble Rouser | stone | — none |  |
| 11 | Pillar Strike | stone | — none |  |
| 12 | Rockslide | stone | — none |  |
| 13 | Rooted | arbor | — none |  |
| 14 | Vines | arbor | — none |  |
| 15 | Sapling | arbor | — none |  |
| 16 | Nightshade | bloom | ✅ seen | flower+green aura |
| 17 | Frolick | bloom | — none |  |
| 18 | Cacti Cluster | bloom | — none |  |
| 19 | Gargoyle | self_petrification | — none |  |
| 20 | Glare | self_petrification | — none |  |
| 21 | Tunnel | self_petrification | — none |  |
| 22 | Burrow | soil | — none |  |
| 23 | Mudpit | soil | — none |  |
| 24 | Debris | soil | — none |  |
| 25 | Sandstorm | sand | — none |  |
| 26 | Dust Devil | sand | — none |  |
| 27 | Vitrification | sand | — none |  |
| 28 | Lapidary | gem | ✅ seen | lavender crystal structure |
| 29 | Fracture | gem | ✅ seen | purple crystal shard |
| 30 | Refraction | gem | — none |  |

## Hydro
| # | Ability | Style | Auto-VFX | Note / flag |
|---|---------|-------|----------|-------------|
| 1 | Frozen Needles | icicle | ✅ seen | white icicle projectile |
| 2 | Stalactite Crash | icicle | — none |  |
| 3 | Skate | icicle | — none |  |
| 4 | Snow Imp | snow | ✅ seen | summoned snow imp creature |
| 5 | Snowstorm | snow | — none |  |
| 6 | Frosty | snow | ✅ seen | red present box (summon) |
| 7 | High Tide | surf | — none |  |
| 8 | Waverider | surf | — none |  |
| 9 | Riptide | surf | — none |  |
| 10 | Piercing Rain | rain | ✅ seen | chain/vine on ground |
| 11 | Rainbow | rain | — none |  |
| 12 | Splash | rain | — none |  |
| 13 | Scald | boiling | — none |  |
| 14 | Geyser | boiling | — none |  |
| 15 | Overheat | boiling | — none |  |
| 16 | Vapor Vanish | vapor | — none |  |
| 17 | Dispersion | vapor | — none |  |
| 18 | Hidrosis | vapor | — none |  |
| 19 | Ice Cap | iceberg | — none |  |
| 20 | Glacier | iceberg | — none |  |
| 21 | Ice Shelf | iceberg | — none |  |
| 22 | Tide Pool | saltwater | — none |  |
| 23 | Abyssal Assist | saltwater | — none |  |
| 24 | Rip Current | saltwater | — none |  |
| 25 | Leap Frog | freshwater | — none |  |
| 26 | River Rapids | freshwater | — none |  |
| 27 | Swamp Monster | freshwater | — none | CLIENT CRASH (summon NRE) - see _crashes.md; no effect captured |
| 28 | Bilge Dump | bilgewater | ✅ seen | creature + brown bilge splash |
| 29 | Anchor Haul | bilgewater | — none |  |
| 30 | Oil Spill | bilgewater | — none |  |

## Aero
| # | Ability | Style | Auto-VFX | Note / flag |
|---|---------|-------|----------|-------------|
| 1 | Shriek | scream | — none | earlier pass saw yellow sound-wave ring; strict pass NONE - human scrub |
| 2 | Sonic Boom | scream | — none | earlier pass saw blue sound-wave ring; strict pass NONE - human scrub |
| 3 | Battle Cry | scream | — none | earlier pass saw red sound-wave ring; strict pass NONE - human scrub |
| 4 | Jet Burst | jet | — none |  |
| 5 | Afterburner | jet | — none |  |
| 6 | Mach Punch | jet | — none |  |
| 7 | Thunderclap | thunder | — none |  |
| 8 | Smite | thunder | — none |  |
| 9 | Chain Lightning | thunder | — none |  |
| 10 | Twister | tornado | ✅ seen | grey tornado/twister |
| 11 | Funnel Cloud | tornado | — none |  |
| 12 | Eye of the Storm | tornado | ✅ seen | storm/tornado vortex |
| 13 | Leap | jump | — none |  |
| 14 | Divebomb | jump | — none |  |
| 15 | Hang Time | jump | — none |  |
| 16 | Air Slash | wind_blade | — none |  |
| 17 | Gale Cutter | wind_blade | — none |  |
| 18 | Razor Wind | wind_blade | — none |  |
| 19 | Smoke Bomb | smoke | ✅ seen | pink/white smoke cloud |
| 20 | Vanish | smoke | — none |  |
| 21 | Smoke Form | smoke | — none |  |
| 22 | Gust | gale_wizard | — none |  |
| 23 | Cyclone Shield | gale_wizard | — none |  |
| 24 | Tempest | gale_wizard | — none |  |
| 25 | Air Shot | pressure | — none |  |
| 26 | Bullet Storm | pressure | — none |  |
| 27 | Pressure Burst | pressure | — none |  |
| 28 | Smog | pollution | ✅ seen | white aura + blood splatter |
| 29 | Toxic Breath | pollution | — none |  |
| 30 | Acid Rain | pollution | — none |  |

## Corruptus
| # | Ability | Style | Auto-VFX | Note / flag |
|---|---------|-------|----------|-------------|
| 1 | Fireball | flame | — none |  |
| 2 | Ignite | flame | — none |  |
| 3 | Combust | flame | — none |  |
| 4 | Raise Dead | necro | ✅ seen | purple spectral summon |
| 5 | Life Drain | necro | ✅ seen | blue slash + purple energy |
| 6 | Death Mark | necro | ✅ seen | blue/green energy slash |
| 7 | Shadow Step | shadow | ✅ seen | purple teleport |
| 8 | Umbral Veil | shadow | — none |  |
| 9 | Dark Embrace | shadow | — none |  |
| 10 | Hellfire | hell_flame | ✅ seen | purple projectile/beam |
| 11 | Infernal Ground | hell_flame | — none |  |
| 12 | Soul Scorch | hell_flame | — none |  |
| 13 | Dominate | mentokinesis | — none |  |
| 14 | Mind Shatter | mentokinesis | — none |  |
| 15 | Hivemind | mentokinesis | — none |  |
| 16 | Imbue: Power | imbuement | — none |  |
| 17 | Imbue: Fortitude | imbuement | — none |  |
| 18 | Imbue: Swiftness | imbuement | — none |  |
| 19 | Sanctuary | attonement | — none |  |
| 20 | Absorb | attonement | — none |  |
| 21 | Purify | attonement | — none |  |
| 22 | Rift | void | ✅ seen | void portal + rift |
| 23 | Void Spawn | void | — none |  |
| 24 | Consume | void | — none |  |
| 25 | Scarak Egg | scarak | ✅ seen | scarak egg hatch |
| 26 | Brood Surge | scarak | ✅ seen | scarak surge |
| 27 | Locust Queen | scarak | ✅ seen | locust queen + insects |
| 28 | Pterodactyl Form | primordial | ✅ seen | transform model |
| 29 | Triceratops Form | primordial | ✅ seen | transform model |
| 30 | T-Rex Form | primordial | — none | strict pass NONE but dedicated in-world test CONFIRMED model swap to reddish primordial |
