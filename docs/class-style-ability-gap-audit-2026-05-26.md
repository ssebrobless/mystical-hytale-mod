# Class / Style / Ability Gap Audit - 2026-05-26

Purpose: keep the implementation honest while live Terra review continues. This
is not a completion claim. It is the working checklist for every class, all 40
styles, and all 120 abilities.

```
Concept -> Hytale primitive -> Runtime owner -> Evidence
  |          |                  |                |
  |          |                  |                +-- observability logs + live visual pass
  |          |                  +------------------- GameplayPlaybackManager / passives / perks
  |          +-------------------------------------- blocks, effects, projectiles, fields, summons
  +------------------------------------------------ user concept docs + latest live corrections
```

## Global Rules

| Rule | Requirement |
|---|---|
| Friendly safety | Caster, allies, and allied summons must not be harmed, slowed, rooted, burned, displaced, or debuffed unless explicitly designed. |
| Object-first visuals | Walls, pillars, flowers, roots, saplings, lava/mud/gem objects should use real temporary blocks or block-like proxies before particle-only approximations. |
| Cleanup | Every temporary block, fluid, proxy, status, and field must restore or despawn deterministically. |
| Normal controls | Final review must use the intended spellbook/hotbar controls, not only `/motm dev` commands. |
| No resources | Ability resource costs are being removed; duration/cooldown/charges replace old resource gates. |

## Current Terra Live Review

| Style | Ability | Current decision / gap |
|---|---|---|
| Quake | Stomp | PASS server evidence in `terra-quake-recheck-20260526f`: one ground impact marker at y=80.0, `targets=1`, damage/effects applied, final cleanup zero. Still needs final user visual taste pass. |
| Quake | Aftershock | PASS server evidence in `terra-quake-recheck-20260526f`: field applied slow/disoriented and final cleanup zero. Still needs final user visual taste pass. |
| Quake | Sinkhole | PASS server evidence in `terra-quake-recheck-20260526f`: one grounded impact marker at y=80.0 (`positions=1 applied=1`), target rooted/DOTed, final cleanup zero. Still needs final user visual taste pass. |
| Metal | Iron Wall | User-approved except watch facing alignment; use 3x3 iron block wall and spawn-overlap push. |
| Metal | Metal Coat | User-approved strong coating route; no class passive visuals should leak into Terra no-style state. |
| Metal | Alloy Enhancement | User-approved mechanics after damage-number fix; visual coating plus impact frames. |
| Stone | Rubble Rouser | PASS server evidence in `terra-stone-recheck-20260526f`: projectile launched and impacted a target without missing model/role errors. Needs final visual pass that it reads as rock/rubble. |
| Stone | Pillar Strike | PASS server evidence in `terra-stone-recheck-20260526f`: stages 3 stone pillar blocks, hits/stuns 1 target, then restores all 3 blocks. Still needs final user visual taste pass. |
| Stone | Rockslide | PASS server evidence in `terra-stone-recheck-20260526f`: dash 5m, ruptured-earth trail, 3 terrain nodes, hit/knockback/grounded on 1 target. Still needs final user visual taste pass. |
| Arbor | Rooted | PASS server evidence in `terra-arbor-recheck-20260526d`: self root + player anchor + protected root terrain + restore, final cleanup zero. Still needs final manual confirmation that movement is visibly blocked. |
| Arbor | Vines | PASS server evidence in `terra-arbor-recheck-20260526d`: 1 target hit, root/DOT applied, repeated `vines.hold` anchor logs. Still needs recast/death cleanup proof. |
| Arbor | Sapling | PASS server evidence in `terra-arbor-recheck-20260526d`: single impact-only ground-marker projectile placed sapling on top of support block and restored. Needs final aim/crosshair feel review. |
| Bloom | Nightshade | PASS server evidence in `terra-bloom-recheck-20260526d`: single impact-only ground marker placed/restored the flower marker and `lured=5` in clustered target setup. Needs final visual taste pass. |
| Bloom | Frolick | PARTIAL server evidence in `terra-bloom-recheck-20260526d`: moving flower trail runtime started and final cleanup zero. Needs movement-specific manual/harness proof that flowers appear behind the moving player and never replace floor. |
| Bloom | Cacti Cluster | PASS server evidence in `terra-bloom-recheck-20260526d`: cactus terrain proxy placed/restored; projectile impact hit 5 targets for 108 damage and applied `dot+slow`. Still needs final visual taste pass. |
| Self Petrification | Gargoyle | Stone coating/root/cancel/cooldown; no resource logic. Needs live no-stuck proof. |
| Self Petrification | Glare | Target stone coating, stun, release then 2s slow. Needs target-state proof. |
| Self Petrification | Tunnel | Timed underground traversal with safe surface/cave exit. Needs movement safety proof. |
| Soil | Burrow | Whack-a-mole dash: vanish, move 4 blocks, re-emerge, damage/knockback at exit. Needs live movement proof. |
| Soil | Mudpit | Brown water-block field on ground; caster/allies not slowed; enemies slowed/vulnerable. Needs friendly-safety proof. |
| Soil | Debris | Forward brown dust/debris wave through enemies. Needs projectile/field visual proof. |
| Sand | Sandstorm | 10s toggle, beige/yellow storm volume; can deactivate; 2s cooldown after end. Needs better sand read. |
| Sand | Dust Devil | Requires Sandstorm; dash drags enemies then expels and ends Sandstorm. Needs linked-state proof. |
| Sand | Vitrification | Shard setup must coexist with Sandstorm/Dust Devil. Needs charges/projectile proof. |
| Gem | Lapidary | Persistent 2x2x2 green gem cube with HP/nameplate and recall. Needs HP visibility proof. |
| Gem | Fracture | Gem-centered expanding green explosion, no ally/caster damage. Needs gem-anchor proof. |
| Gem | Refraction | Gem-centered green aura/shield field. Needs aura and buff proof. |

## Non-Terra Coverage Matrix

These classes are not cleared. Each style must receive the same concept-vs-runtime
live pass before the mod is called complete.

Structural no-resource coverage passed for all 40 styles / 120 abilities in
`coverage-20260526c` and `full-style-post-sweep-20260526`; this proves data
shape and no-resource setup, not visual or full mechanical correctness.

| Class | Styles | Main primitive risks to verify |
|---|---|---|
| Hydro | icicle, snow, surf, rain, boiling, vapor, iceberg, saltwater, freshwater, bilgewater | Water/ice fields, swim/speed proofs, bubble shields, ride/summon behavior, rain/weather detection, ally-safe healing/shields. |
| Aero | wind_blade, thunder, gale_wizard, scream, pressure, tornado, smoke, jet, jump, pollution | Projectile aim/trails, vertical movement interactions with Wind Walker, sprint/bunny-hop style movement, smoke/stealth visibility, storm/tornado fields. |
| Corruptus | flame, necro, shadow, hell_flame, mentokinesis, imbuement, attonement, void, scarak, primordial | Soul Harvest stack buffs/resurrection lockout, corruption/fire visuals, summons/ghosts/minions, self-damage tradeoffs, execute/curse rules, no ally harm. |

## Required Verification For Every Ability

| Check | Evidence needed |
|---|---|
| Cast route | Log line showing ability id, style id, cast type, target source, and normal control input. |
| Visual route | Live third-person visual or harness capture matched against the concept. |
| Mechanical route | Damage/heal/status/movement/knockback/shield/summon logs with target ids. |
| Cleanup route | Log or observation that temporary blocks, fields, effects, proxies, and statuses ended. |
| Safety route | Explicit skip logs or proof that caster/allies/summons were not negatively affected. |

## Full Runtime Cast Sweep - 2026-05-26

```
40 styles
  |-- Terra:      10/10 PASS in full-style-sweep-20260526-terra-full
  |-- Hydro:      10/10 PASS in full-style-sweep-20260526-non-terra-full-r2
  |-- Aero:       10/10 PASS in full-style-sweep-20260526-non-terra-full-r2
  `-- Corruptus:  10/10 PASS in full-style-sweep-20260526-non-terra-full-r2

120 abilities
  `-- all queued, observed, snapshotted, collected, and reported by the
     agent observability baseline without missing ability_cast_end evidence.
```

| Scope | Evidence | Result | Meaning |
|---|---|---|---|
| Terra all styles | `audits/style-sweeps/full-style-sweep-20260526-terra-full/report.md` | PASS | Every Terra style swapped cleanly, fired all three abilities, collected observability bundles, and cleared test mobs. |
| Hydro/Aero/Corruptus all styles | `audits/style-sweeps/full-style-sweep-20260526-non-terra-full-r2/report.md` | PASS | Every non-Terra style swapped cleanly, fired all three abilities, collected observability bundles, and cleared test mobs. |
| No-resource data/runtime gates | `scripts/audit-no-resource.ps1` | PASS | Resources remain disabled globally across all class/style ability data and runtime resource gates. |
| Ability data coverage | `audits/ability-coverage/full-style-post-sweep-20260526/report.md` | PASS | All 40 styles and all 120 abilities have required structural fields and no resource costs. |
| Warning scan | sweep driver logs | PASS with note | No real failure signatures found; the only warning-pattern hits were the ability id `stalactite_crash` containing the substring `crash`. |

This sweep proves the baseline runtime route: style selection, ability queuing,
ability completion telemetry, snapshots, evidence collection, and target cleanup.
It does **not** by itself prove exact player-facing aesthetics, exact crosshair
trajectory feel, movement-path behavior, ally/caster safety, summon AI quality,
or normal-control input timing. Those still require purpose-built probes or live
manual review, not generic stationary/cluster casts.

## Concept Verification Layer Audit - 2026-05-26

`scripts/audit-concept-verification-layers.ps1` now reads the raw observability
JSONL from the full style sweeps and classifies each ability against the next
verification layers.

```
120 abilities
  |-- runtime cast evidence:       120 PASS / 0 FAIL
  |-- mechanical concept signal:   110 PASS / 10 REVIEW
  |-- visual/proxy signal:         120 PASS / 0 FAIL
  |-- safety proof:                 55 PASS / 65 UNKNOWN
  |-- normal-control proof:          0 PASS / 120 UNKNOWN
  `-- aim/movement proof:           32 PASS / 88 REVIEW
```

Evidence:

| Report | Meaning |
|---|---|
| `audits/concept-verification-layers/concept-layers-20260526-final/report.md` | Full 120-ability layer matrix, including the next probe queue. |
| `audits/concept-verification-layers/concept-layers-20260526-final/concept-verification-layers.csv` | Machine-readable per-ability gate status. |

Current interpretation: the mod has broad runtime coverage, but the next
work is not another generic sweep. The next work is purpose-built proof for
normal spellbook/hotbar controls, hostile ability friendly-safety, and the
abilities whose concept depends on crosshair accuracy, projectile trajectory,
movement path, dash, leap, tunnel, swim, summon AI, or player-positioned fields.

## Normal-Control Probe

`scripts/run-normal-control-probe.ps1` is the first purpose-built next-layer
runner. It sets a class/style, selects the spellbook hotbar slot, sends actual
player input for slots 1-3, collects MOTM observability evidence, and then
requires the expected ability id to appear after the matching input marker.

Example:

```
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-normal-control-probe.ps1 `
  -WorldName "MOTM Creative Test" `
  -ClassId terra `
  -StyleId quake `
  -ControlMode PrimarySecondaryUse
```

Launch note: use the official launcher/auth path before running this probe.
`scripts/cold-launch.ps1` and `scripts/start-hytale.ps1` now default to
`Launcher`; direct client launch is reserved for a future explicitly supported
offline-token path because the current direct route fails with Hytale's
`Offline mode requires an offline token` client error.

Control modes:

| Mode | Slot 1 | Slot 2 | Slot 3 |
|---|---|---|---|
| `PrimarySecondaryUse` | Left click | Right click | Ability 3 / use key |
| `AbilityKeys` | Ability 1 key | Ability 2 key | Ability 3 key |

Use this probe before calling any style complete through normal player controls.
If the probe fails, the failure is either the custom spellbook interaction
contract, the current keybind/action mapping, or the ability's normal route, not
the generic `/motm dev test ability` route.

## Immediate Implementation Order

1. Run `scripts/run-concept-verification-layers.ps1` for primitive proof and
   normal-control evidence. Start narrow, for example:

   ```
   powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-concept-verification-layers.ps1 `
     -WorldName "MOTM Creative Test" `
     -Classes terra `
     -Styles quake `
     -Layers PrimitiveProofs,NormalControl `
     -ControlMode PrimarySecondaryUse `
     -SkipBuild
   ```

   Then widen to all styles once the narrow run is green.
2. Run `scripts/run-normal-control-probe.ps1` class-by-class and style-by-style
   when a single style needs isolated normal input evidence.
3. Add target-position/aim probes for crosshair-sensitive projectiles and
   ground-target abilities; compare player facing, intended target position, and
   impact/field origin.
4. Add movement/path probes for Frolick, Tunnel, Burrow, Sandstorm/Dust Devil,
   Gem object control, Aero vertical movement, Hydro swim lanes, and Corruptus
   summons/transforms.
5. Add ally/caster/summon safety probes for every AoE, field, DoT, root, slow,
   burn, knockback, and temporary terrain ability.
6. Add visual/proxy probes that expose the chosen asset/effect ids, colors,
   block/material ids, proxy counts, and cleanup counts per ability.
6. Keep every style's result tied to a run id; do not mark visual quality PASS
   without either user confirmation or a purpose-built visual/proxy proof.
