# Ability Concept Reconciliation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reconcile every class/style ability against the original detailed Hytale concept and current user corrections, then implement the missing mechanics, visuals, cleanup, and observability proof paths style by style.

**Architecture:** Treat the original `MOD_DESIGN.md` concept as the design baseline, current user corrections as overrides, and protected style JSON as implementation data only. Build shared runtime primitives first, then repair each style through small, testable commits with observability evidence.

**Tech Stack:** Java Hytale mod runtime, protected JSON data, PowerShell audit scripts, MOTM observability harness, Hytale EntityEffects, temporary block/fluid selections, NPC/proxy/summon runtime state.

---

## Source Authority

```
╔════════════════════════════════════════════════════════════════════╗
║ Ability Truth Stack                                               ║
╠════════════════════════════════════════════════════════════════════╣
║ 1. Current user corrections in chat and CODEX_CONCEPT_REVIEW_*     ║
║ 2. Original Hytale concept: motm-hytale-extract/.../MOD_DESIGN.md  ║
║ 3. Local Hytale API/assets and proven primitive docs               ║
║ 4. Current protected style JSON as current implementation state     ║
║ 5. Existing Java behavior as current state, not design authority    ║
╚════════════════════════════════════════════════════════════════════╝
```

Primary generated gap matrix:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-ability-concept-reconciliation.ps1 -NoTimestamp
```

Expected outputs:

```text
audits/concept-reconciliation/latest/ability-concept-gap-matrix.md
audits/concept-reconciliation/latest/ability-concept-gap-matrix.json
```

Current result: 120 concept rows, 120 current rows, 57 major gap rows.

## File Structure

Modify or create only these surfaces unless a task explicitly discovers a required API split:

- `scripts/build-ability-concept-reconciliation.ps1`: generates the concept/current/runtime-hit matrix.
- `docs/superpowers/plans/2026-05-26-ability-concept-reconciliation.md`: this implementation plan.
- `docs/ABILITY_REFERENCE.md`: human-readable ability reference after implementation changes.
- `docs/FRIEND_REVIEW_GUIDE.md`: tester-facing visual/function notes.
- `src/main/resources/data/styles/*_styles.json`: surgical field changes only.
- `src/main/resources/Server/Entity/Effects/MOTM/*.json`: VFX definitions for proven effect primitives.
- `src/main/java/com/motm/manager/GameplayPlaybackManager.java`: style/ability runtime behavior.
- `src/main/java/com/motm/util/HytaleAssetResolver.java`: ability-specific asset routing.
- `src/main/java/com/motm/util/MotmPlaybackGeometry.java`: shared geometry helpers.
- `src/main/java/com/motm/system/MotmDamageEventSystem.java`: hit, weapon, projectile, and follow-up hooks.
- `src/main/java/com/motm/manager/StyleManager.java`: cooldown, charge, toggle, action-window state.
- `src/main/java/com/motm/manager/MotmRuntimeTasks.java`: deferred cleanup and tick work.
- `src/main/java/com/motm/command/MotmDevCommandRouter.java` and `MotmProofCatalog.java`: proof commands and scenarios.
- `scripts/run-agent-observability-baseline.ps1`: extend scenarios when a feature is not covered.

## Implementation Order

```
╔════════════════════════════════════════════════════════════════════╗
║ Reconciliation Flow                                               ║
╠════════════════════════════════════════════════════════════════════╣
║ Audit matrix                                                       ║
║   └─▶ shared primitive repairs                                     ║
║        └─▶ Terra residuals                                         ║
║             └─▶ Hydro styles                                       ║
║                  └─▶ Aero styles                                   ║
║                       └─▶ Corruptus styles                         ║
║                            └─▶ full observability + manual review  ║
╚════════════════════════════════════════════════════════════════════╝
```

## Task 1: Lock The Concept Gap Matrix

**Files:**
- Modify: `scripts/build-ability-concept-reconciliation.ps1`
- Generate: `audits/concept-reconciliation/latest/ability-concept-gap-matrix.md`
- Generate: `audits/concept-reconciliation/latest/ability-concept-gap-matrix.json`

- [x] **Step 1: Generate a full 120-row ability matrix**

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-ability-concept-reconciliation.ps1 -NoTimestamp
```

Expected:

```text
Concept rows: 120
Current rows: 120
Major gap rows: 57
```

- [ ] **Step 2: Re-run the matrix before every class batch**

Run the same command after each class batch. Expected: the high-risk count decreases or remaining rows are intentionally marked with a documented Hytale-safe approximation.

- [ ] **Step 3: Preserve the full matrix as evidence**

Do not replace the full matrix with a summary. The implementation worker must inspect the row for each ability before touching that ability.

## Task 2: Build Shared Runtime Primitives First

**Files:**
- Modify: `GameplayPlaybackManager.java`
- Modify: `MotmPlaybackGeometry.java`
- Modify: `MotmRuntimeTasks.java`
- Modify: `MotmDamageEventSystem.java`
- Modify: `MotmProofCatalog.java`

- [ ] **Step 1: Add one owned runtime cleanup registry**

Every temporary block, fluid, proxy, summon, projectile, coating, trail, and persistent field must register an owner player id, ability id, style id, creation tick, expiry tick, and cleanup callback.

Acceptance:

```text
/motm dev clear
/motm dev styles clear
/motm class <other>
/motm style <other>
```

Each command must remove all ability leftovers and log counts for blocks, fluids, proxies, effects, projectiles, summons, and fields.

- [ ] **Step 2: Add friendly-safety filtering as a shared helper**

All AoE, field, projectile, summon, and debuff targeting must call a shared helper before applying negative effects.

Acceptance evidence:

```text
target_skipped reason=caster
target_skipped reason=ally
target_skipped reason=allied_summon
target_hit reason=hostile
```

- [ ] **Step 3: Add aimed projectile proof support**

Projectiles must log aim vector, spawn position, flight ticks, hit target/surface, despawn reason, and visual proxy id. This is required for Magma Sling, Cacti Cluster, Air Slash, Gale Cutter, Air Shot, Bullet Storm, Scald, Anchor Haul, and many Corruptus projectiles.

- [ ] **Step 4: Add surface-placement helper**

The helper must place decorative/temporary objects on top of the surface block, never replacing the block below the player or target unless the ability explicitly calls for terrain replacement.

Acceptance:

```text
surface_place_result ability=frolick replacedOriginalBlock=false
surface_place_result ability=sapling placedOnTop=true
surface_place_result ability=nightshade placedOnTop=true
```

## Task 3: Terra Residual Repair Batch

**Files:**
- Modify: `terra_styles.json` surgically only where current fields contradict approved concepts.
- Modify: `GameplayPlaybackManager.java`
- Modify: `HytaleAssetResolver.java`
- Modify: Terra MOTM EntityEffect files as needed.

High-risk rows from the matrix:

| Style | Ability | Missing Core Work |
| --- | --- | --- |
| Magma | Magma Sling | true aimed lava blob projectile, not living mob/proxy |
| Stone | Rockslide | dash path with debris under feet, hostile-only push |
| Bloom | Frolick | flowers placed on top of blocks behind moving player |
| Bloom | Cacti Cluster | large slow cactus projectile, attach DoT, delayed explosion DoT |
| Sand | Vitrification | 5 glass shard follow-up attacks fired by future primary attacks |

Also repair user-observed Terra misses even if the heuristic did not flag them:

| Style | Ability | Required Repair |
| --- | --- | --- |
| Stone | Rubble Rouser | unarmed/next-punch state, stone arms/body coating, AoE knockback |
| Stone | Pillar Strike | 1x1x4 staged stone pillar under target, launch/stun, disappear 0.6s after full height |
| Arbor | Rooted | actual root/immobile state, roots on top of surface, no breakable resource drops |
| Arbor | Vines | one rooted target at a time, persistent target visual, cleanup on death/new target |
| Arbor | Sapling | ground-impact sapling/lure, projectile follows crosshair |
| Quake | Stomp/Aftershock | one ground VFX at impact/field center, not per-target body VFX except aerial clarity |
| Quake | Sinkhole | crack marker on ground, not floating, reduced smoke |

Acceptance:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-install.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-agent-observability-baseline.ps1 -WorldName Main -RunId terra-reconcile-YYYYMMDD
```

Manual review:

```text
/motm dev clear
/motm class terra
/motm style <styleId>
```

Test every Terra style with normal controls, third-person view, clean arena, and one report row per ability.

## Task 4: Hydro Concept Repair Batch

High-risk rows:

| Style | Ability | Missing Core Work |
| --- | --- | --- |
| Icicle | Stalactite Crash | planted giant icicle, delayed/early explosion interaction with Frozen Needles |
| Snow | Snowstorm | follow-player aura, cancel/reduced cooldown, heals snow minions |
| Surf | High Tide | wave + speed + Hydro passive interaction |
| Surf | Waverider | water trail movement, Aqua Barrier trigger without cooldown misuse |
| Surf | Riptide | target water pool that pulls, vulnerability, proper duration |
| Rain | Piercing Rain | follower rain aura and automatic Rainbow trigger on end |
| Boiling | Scald | boiling stream, multi-target push, burn |
| Vapor | Vapor Vanish | invisible/floating rain-block form with flight and fall-risk exit |
| Vapor | Dispersion | 3-charge teleport dash |
| Iceberg | Ice Cap | ice box around player, block-break explosion counter, early end |
| Iceberg | Ice Shelf | land-only ice pillar/self form, freeze/damage/slow |
| Saltwater | Abyssal Assist | requires Tide Pool, eel inside pool, stun/vulnerability |
| Saltwater | Rip Current | requires Tide Pool, enlarged pool, drag/capture/release combo |
| Freshwater | Leap Frog | jump/landing pool, sprint distance, vulnerability |
| Freshwater | River Rapids | movement-distance water trail with friendly speed buff |
| Bilgewater | Bilge Dump/Oil Spill | toxic/oil pools, ally-safe buffs, enemy-only debuffs |

Acceptance:

```text
Hydro player never keeps permanent tint or passive water trail after /motm dev clear.
Aqua Barrier appears as one large blue bubble until destroyed, then cooldown starts.
Hydro fields never slow, damage, burn, or debuff caster/allies/summons unless explicitly approved.
```

## Task 5: Aero Concept Repair Batch

High-risk rows:

| Style | Ability | Missing Core Work |
| --- | --- | --- |
| Scream | Shriek/Sonic Boom | enclosed-area scaling, charge/release, fall-cancel/upward launch |
| Tornado | Twister/Funnel Cloud | tornado form, projectile absorption count, stationary funnel debris |
| Jet | Jet Burst/Afterburner/Mach Punch | charge dash state, next-dash enhancer, post-dash melee follow-up and wall collision |
| Jump | Divebomb/Hang Time | airborne-only slam, fall-distance scaling, hover/cancel/recast state |
| Wind Blade | Air Slash/Gale Cutter/Razor Wind | wind blade projectiles, spread, melee toggle follow-up |
| Smoke | Smoke Bomb | thrown smoke bomb, friendly speed, enemy vision/target loss |
| Pressure | Air Shot/Bullet Storm | charge-time projectiles, barrage, distance falloff |
| Thunder | Thunderclap | shock/stun state and lightning follow-up hooks |
| Pollution | Toxic Breath/Acid Rain | cone contamination spread and acid rain armor corrosion |

Acceptance:

```text
Wind Walker remains one passive row and does not create duplicate vertical movement behavior.
Every movement ability is tested with actual movement/action inputs, not standing-still command casts.
Every charge/follow-up ability logs armed/bound/consumed/expired state.
```

## Task 6: Corruptus Concept Repair Batch

High-risk rows:

| Style | Ability | Missing Core Work |
| --- | --- | --- |
| Necro | Raise Dead/Life Drain/Death Mark | owned skeleton minions, death explosion, drain aura, one active mark |
| Flame | Fireball/Ignite | burn stack accounting, self-ignite aura, Combust burn consumption chain |
| Hell Flame | Infernal Ground/Soul Scorch | crimson field, self-damage, curse, no-regen, cooldown-after-duration |
| Shadow | Shadow Step/Umbral Veil/Dark Embrace | clone/decoy, stealth break, shadow aura vision/dodge |
| Mentokinesis | Mind Shatter/Hivemind | dominated/friendly minion dependencies and linked-minion behavior |
| Void | Consume | delayed marked area, banish/reappear/stun |
| Scarak | Brood Surge/Locust Queen | egg hatch/enrage, mountable locust behavior |
| Primordial | Triceratops Form | land/water branch decision and form action support |

Acceptance:

```text
Soul Harvest shows 0-5 stacks, applies stack stat bonuses, resurrects at 5, and blocks stack gain during lockout.
Corruptus summons/minions are allied and obey friendly-safety filters.
Self-damage mechanics do not accidentally kill the caster outside the intended risk rules.
```

## Task 7: Documentation And UI Alignment

**Files:**
- Modify: `docs/ABILITY_REFERENCE.md`
- Modify: `docs/FRIEND_REVIEW_GUIDE.md`
- Modify: Spellbook UI files if ability descriptions are stale.

- [ ] **Step 1: Update ability descriptions after each style**

The spellbook and docs must say what the runtime actually does after the style is repaired. Do not leave old compressed descriptions.

- [ ] **Step 2: Keep no-resource wording**

Any original concept resource dependency should be converted to duration, charge, cooldown, item-state, or environmental gating unless the user explicitly reintroduces resources.

## Task 8: Verification Gate For Every Style

For each style:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-install.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-agent-observability-baseline.ps1 -WorldName Main -RunId <class>-<style>-reconcile-YYYYMMDD
```

Manual-ready commands:

```text
/motm dev clear
/motm class <classId>
/motm style <styleId>
```

Evidence required:

```text
1. Normal-control cast intent for all 3 abilities
2. Target hit/skip logs
3. Cleanup logs after dev clear/style swap
4. Third-person visual review for object/field/coating/projectile abilities
5. Report row: PASS / FAIL / USER_VISUAL_REVIEW_NEEDED
```

## Stop Conditions

Stop and report instead of improvising if:

- A required Hytale API is not discoverable in the installed jar.
- A visual requires a new particle/model id not found locally.
- A temporary block/fluid proof causes client crashes.
- Friendly-safety filtering cannot identify allies/summons for a mechanic.
- The generated matrix conflicts with a newer user correction.

## First Execution Target

Start with Terra residual repair because the user is already reviewing Terra live and because it proves shared primitives needed by all later classes:

```text
1. Surface placement helper
2. Runtime cleanup registry extension
3. Terra Arbor/Bloom placement repairs
4. Terra Stone movement/pillar repairs
5. Terra Magma projectile repair
6. Terra Sand Vitrification follow-up state
7. Quake visual cleanup polish
```

After Terra passes, move to Hydro because current user-visible passive/field residue makes it the next highest-friction class.
