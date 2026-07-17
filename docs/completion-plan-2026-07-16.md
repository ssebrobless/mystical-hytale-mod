# MOTM Completion Plan - 2026-07-16

Purpose: concrete, evidence-backed plan for completing the mod to current canon
(40 styles / 120 abilities / 20 shared perks / L200 progression, pure RPG overlay).
Built by cross-examining six parallel investigations (ability gap matrix, controlled-ally
code audit, harness/scenario inventory, original-intent drift ledger, residual debt
ledger, Hytale 0.5.6 API research) against the recovered canon docs and a live
re-baseline run performed today.

Verification baseline: build green, world join fixed (HUD path casing, commit
`eb1ba7d`), observability baseline PASS at
`audits/agent-observability/2026-07-16T21-42-40/`.

---

## 1. Correlation With Original Intent

Canon precedence when sources conflict:
1. User-reviewed decisions (`CODEX_CONCEPT_REVIEW_DECISIONS_2026-05-22.md`)
2. `docs/current-canon-checklist.md`, `docs/ui-canon-2026-06-01.md`, startup/leveling plan
3. `motm-hytale-extract/original-concept/MOD_DESIGN.md` intent
4. Current JSON/runtime behavior
5. Old `PLAN.md` / archive notes

Drift ledger (original -> current):

| System | Original Spec | Current Canon | Status |
|---|---|---|---|
| Classes | 4 elemental (Terra/Hydro/Aero/Corruptus) | Same four | KEPT |
| Styles | 40 (10/class) | Same 40 IDs/names | KEPT |
| Abilities | 120; %HP scaling, charges, item/world costs | Same 120 identities; cooldown/duration only | KEPT identity, CHANGED mechanics |
| Active loadout | 3 styles equipped -> 9 abilities | 1 style -> 3 abilities | NARROWED |
| Perks | 800-perk augment tree (200/class, 20 tiers) | 20 shared passives, 1 pick/10 levels to L100 | CUT to shared pool |
| Level cap | 200 | 200 (`LevelingManager.MAX_LEVEL`) | KEPT |
| Stat points | absent | 2/level across 5 stats | ADDED |
| Class passives | multiple per class | one consolidated passive/class | NARROWED |
| Mob scaling | global hostile scaling | floor(player*0.9)+/-2, title bands Intern->Master | KEPT core, CHANGED |
| Elite mobs | zone elite variants | data present, `canBecomeElite()` hard-false | CUT (silently) |
| Elemental reactions | absent | 6 pairwise reactions | ADDED |
| Races | 12 playable | removed | CUT (explicit) |
| Resource costs | heavy per-style economies | zero-cost casting (`ability_costs.json` disabled) | CUT (explicit) |
| Story/lore UI | Journey/Codex/Grimoire | banned from spellbook | CUT (explicit) |

Silently dropped (no explicit decision record found - reconfirm or record intentionally):
- 3-style / 9-ability loadout (only implied by startup plan)
- Perk tree tiers 2-20 (archived JSON only)
- Elite mob spawns (`MobScalingManager.canBecomeElite() = false` with live data files)
- Style resource economies (disabled globally; dead code bridges remain in `resource/`)

Verdict: the mod is a faithful *identity* implementation of the original concept with a
deliberately compressed *mechanics* layer. Completion means finishing the current canon,
not resurrecting the 800-perk tree. Rule already in canon: "Do not return to old
tier-2+ perk fantasies."

---

## 2. Current Verified State

```
Authored content (JSON)      ██████████  COMPLETE (protected files)
Runtime architecture         ██████████  COMPLETE (ratchets 0/0, evidence ledger)
Build + environment          ██████████  GREEN as of 2026-07-16 (Hytale 0.5.6)
Harness + observability      █████████░  PASS baseline; gaps for new primitives (Sec. 4)
Perk runtime                 ████████░░  20/20 hooked; movement/crafting live-proof gaps
Primitives (11 families)     ███░░░░░░░  all PARTIAL or NOT PROVEN
Style sweeps                 ██░░░░░░░░  8/40 playable, 32 partial
```

Primitive family status (from `docs/primitive-status-2026-06-05.md`, still accurate):

| Family | Status | Representative gap |
|---|---|---|
| P0 Summon combat | PARTIAL | Snow accepted; no non-Snow family proven |
| P0 Controlled ally | NOT PROVEN | contract entirely absent in code (Sec. 3) |
| P0 Pull/tether/carry | PARTIAL | no shared visual-link contract |
| P1 Persistent fields | PARTIAL | adapters exist, weak visual proof |
| P1 Status/coating/bubble | PARTIAL | stacking/expiry rules undefined |
| P1 Transformation/form | PARTIAL | Smoke Form only; dino forms unproven |
| P1 Dash/burst | PARTIAL | teleport-vs-travel rule undefined |
| P2 Projectile/line | PARTIAL | theme families unproven |
| P2 Barrier/world object | PARTIAL | no-drop proof per family missing |
| P2 Perk families | PARTIAL | movement/crafting live proof missing |
| Cross-cutting cleanup | PARTIAL | expand no-drop guard per primitive |

---

## 3. Controlled Ally - Next Work Unit (code-audited)

Cast pipeline: `StyleManager.useAbility` -> `MotmRuntimeTasks` ->
`AbilityCastRuntimeTaskProcessor` -> `GameplayPlaybackManager.executeAbility` (L1557)
-> family fan-out. The mentokinesis trio routes through *generic* cast-type paths only:

- `dominate` = gaze/enemy; only special-case is `AbilityExecutionPolicy` (~L88) adding
  `root` + `disoriented` tokens. No conversion.
- `mind_shatter`, `hivemind` = self_burst; terrain/self paths. No controlled-ally
  centering or linking.

Reusable machinery (from summon family):
- Owner binding + friendly-fire: `MotmDamageEventSystem.shouldSuppressFriendlySummonDamage`,
  `SummonControlHytaleAdapter` (skips `isMotmSummon` targets)
- Target acquisition/movement/attack: `SummonTargetRuntime`, `SummonMovementRuntime`,
  `SummonAttackHytaleAdapter`
- `isMotmSummon` allowlist (GPM L141-147, L4341-4346): `motm_summon`,
  `MOTM_Summon_Driver`, `Tamed_Frosty`, `motm_projectile`, `motm_field`

ABSENT (must be built):
- Hostile->ally conversion state (new runtime family, e.g. `runtime/ability/control/`)
- Pink control marker visual
- Toggle/release/death/logout cleanup
- `mind_shatter` resolution against controlled-ally center
- Any proof id (catalog has ~25 proofs: coatings, temp blocks/fluids, proxies,
  movement, one native projectile - zero summon/control/tether proofs)
- Any dev command (`MotmDevCommandRouter` has no control/summon/tether subcommands)
- Scenario assertions (`scripts/scenarios/corruptus-style-mentokinesis.json` exists but
  `proofs: []`, generic evidence only)

CONTRADICTION RESOLVED: `ABILITY_COMPLETION_CHECKLIST.md` (~L108) marks dominate done as
"stronger control debuff" `[x]`. The newer `primitive-status-2026-06-05.md` supersedes it:
dominate must become a true single-target controlled ally. The checklist entry should be
reopened when the primitive lands.

Hytale API facts for implementation (javap-verified against 0.5.6 server jar):
- No first-class "player owns NPC" API. Compose: role JSON (`Server/NPC/Roles/`) with
  friendly `AttitudeGroup` + `NPCEntity.setRoleName` + `NPCEntity.addReservation(UUID)`
  for MOTM-side ownership + MOTM friendly-fire filter.
- Guard every role change with `NPCPlugin.hasRoleName(id)` /
  `validateSpawnableRole(id)`; runtime swaps via `RoleChangeSystem.requestRoleChange`.
- Attitudes: `IGNORE | HOSTILE | NEUTRAL | FRIENDLY | REVERED`.
- Prefer NOT role-swapping dominated NPCs where a MOTM-side control state suffices
  (role swap on persisted NPCs is what caused the Scarak SEVERE below).

---

## 4. Harness Gaps For Primitive Acceptance

Infrastructure inventory: 49 scenarios, ~25 proof ids, 4 JSONL evidence planes
(control/causality/server-truth/client-intent), sweep orchestration via
`run-style-observability-sweep.ps1` (-Classes/-Styles/-RunId; parallel lanes = disjoint
style sets + distinct run ids).

Missing for P0 acceptance (build these WITH the primitives, per AGENTS.md
"extend the harness first"):

| Need | For | Status |
|---|---|---|
| `summon_spawned` / `summon_attack` / `control_acquired` / `control_released` JSONL events | Ctrl + Summon | ABSENT |
| Proof ids: `controlled-ally`, `summon-acceptance`, `tether-visual` + runners | P0 all | ABSENT |
| Dev probes: `/motm dev control status`, summon/tether state dumps | P0 all | ABSENT |
| `controlled-ally` scenario (2 hostiles, dominate one, assert it attacks the other) | Ctrl | ABSENT (mentokinesis scenario is a shell) |
| Summon scenario asserting combat, not just `entity_effect_add` | Summon | PARTIAL (`hydro-summon-snow-imp.json`) |

---

## 5. Standing Defects & Debt (deduplicated)

| # | Item | Severity | Evidence |
|---|---|---|---|
| 1 | Missing NPC role JSONs: code references `MOTM_Scarak_Fighter_Ally`, `MOTM_Scarak_Defender_Ally`, `motm_field` but only `Tamed_Frosty.json` + `MOTM_Summon_Driver.json` ship under `Server/NPC/Roles/MOTM/` -> SEVERE at world load, `RoleChangeSystem` failure | Blocks summon primitive; corrupts persisted-world NPCs | client log 2026-07-16 L1718-1720 |
| 2 | 17 `[removal]` deprecations; successors verified: `Inventory.getSectionById(InventoryComponent.*_SECTION_ID)` / ECS components, `InventoryComponent.getItemInHand(accessor, ref)`, `CraftRecipeEvent.Pre/Post` via `EntityEventSystem` (replaces `PlayerCraftEvent`), `PlayerRef.getPacketHandler()` / `PlayerRef.sendMessage` (replaces `getPlayerConnection`) | Breaks on a future game update | build output; javap |
| 3 | 8 code TODO stubs: `LevelingManager` (XP/level-up/milestone events, stat recalc; L330-365), `PerkManager` L171 (log-only effects - superseded by RuntimePerkManager?), `SynergyEngine` L177, `MotmRuntimeLoop` L90 (DoT via damage API) | Feature gaps in leveling feedback loop | grep verified |
| 4 | Perk live-proof gaps: movement perks + crafting/world-action perks proven only via chat-command simulation | Perk family P2 | PERK_RUNTIME_STATUS.md |
| 5 | Phase 9 / Terra residuals: mud tint, sand EntityEffect, tunnel safety, cross-class visual kinks | Visual polish, mostly absorbed into P1/P2 family work | PHASE9_RESIDUALS, TERRA kinks doc |
| 6 | Asset-path case sensitivity (new 0.5.6 behavior) - fixed for HUD; audit remaining `append()`/resource paths once | Regression class | CLAUDE.md Known Issues |
| 7 | `audits/blockers/` - all historical blockers PASS/resolved; only subjective idle-slow noted | None open | audits/blockers/2026-05-21 |

Cross-exam notes: proof-count discrepancy between reports (24 vs 25) is a counting
difference, not a defect. The `[x] dominate` checklist row is the only true
inter-document contradiction found (resolved in Sec. 3).

---

## 6. Execution Plan

```
W0 Hardening (short, unblocks everything)
├─ W0.1 Ship missing role JSONs or hasRoleName guards (defect #1)
├─ W0.2 API migration (defect #2): MotmPlayerInventory first, then 9 call sites,
│        MotmCraftRecipeEventSystem, sendMessage; build gate = zero [removal] warnings
└─ W0.3 One-pass case-sensitivity audit of resource path strings (defect #6)

W1 P0 Controlled Ally  ◀ per primitive-status "Recommended Next"
├─ runtime/ability/control/ family (state, tick, HytaleAdapter) reusing summon
│  owner/faction/FF patterns; dominate single-target first
├─ pink marker visual + explicit release/death/logout/clear cleanup
├─ harness: control_* events, controlled-ally proof + scenario (2 hostiles)
├─ extend: hivemind (radius/multi), mind_shatter centers controlled ally
└─ GATE: hostile converts, visibly fights hostiles, follows caster, cannot hurt
   allies, cleanup proven, harness PASS + visual capture; update status doc

W2 P0 remainder (parallelizable after W1's faction/FF layer exists)
├─ Summon acceptance generalized: swamp_monster, raise_dead, void_spawn,
│  scarak_egg, locust_queen, shadow_clone, Haunting ghosts (reusable scenario)
└─ Tether visual contract: start with vines or anchor_haul (NOT rip_current);
   thin particle line, source/target anchors, synced movement, cleanup proof

W3 P1 primitives (two parallel lanes)
├─ Lane A: fields (sinkhole or smoke_bomb first) + coatings (Aqua Barrier
│  representative; stacking/expiry rules first)
└─ Lane B: transformations (dino forms: model identity, form actions, hotbar
   restrictions, early exit) + dash (define teleport-exception rule; dust_devil
   is the high-risk target)

W4 P2 + leveling loop
├─ projectile theme families (visible travel / cone / chain / charged / instant)
├─ barrier/world-object no-drop pass
├─ perk families: movement + crafting live proofs (needs W0.2 CraftRecipeEvent)
└─ TODO stubs: leveling XP/level-up/milestone events + DoT application (defect #3)

W5 Style sweeps (widest parallelism)
├─ 32 partial styles vs style-ability-intent-canon, per-style scenario lanes
├─ orchestrate with run-style-observability-sweep.ps1 (disjoint -Styles sets)
└─ reopen/close ABILITY_COMPLETION_CHECKLIST rows per sweep evidence

W6 Public release
├─ audit-public-readiness.ps1 + audit-canon-drift.ps1 + audit-no-resource.ps1
├─ public channel jar (no -internal classifier), manifest verified at jar root,
│  IncludesAssetPack=true, Common/ + Server/ trees present
└─ CurseForge upload (plugin .jar; confirm category/deps schema at upload time)
```

Working rules (unchanged, from canon):
- Extend the harness before or with each primitive; PASS requires log evidence AND
  the intended visual, not just mechanical outcome.
- No training-dummy hidden dependencies; temporary entities/blocks clean up
  without drops.
- Update `docs/primitive-status-*.md` after each family gate before starting the next.
- Live proof runs require a human (or idle desktop) in world "MOTM Creative Test";
  Direct client launch is dead (needs launcher offline token) - use official launcher.

Sequencing rationale: W0 is cheap and removes the two failure modes that can invalidate
any later proof run (role SEVEREs polluting logs, API removal breaking a build mid-sweep).
W1 before W2 because controlled ally forces the faction/targeting/FF layer that every
remaining summon reuses. W5 is deferred until primitives are proven so 32 styles are
swept against contracts instead of one-off guesses - the exact methodology shift the
project committed to on 2026-06-01.

## 7. Definition of Done (current canon)

1. All 11 primitive families proven with harness evidence + visual capture.
2. All 40 styles pass their sweep against `style-ability-intent-canon-2026-06-01.md`.
3. All 20 perks live-proven (including movement/crafting).
4. Leveling feedback loop complete (XP/level-up/milestone events, DoT).
5. Zero `[removal]` warnings; zero SEVERE role/asset errors at world load.
6. Public-readiness + canon-drift + no-resource audits PASS; public jar on CurseForge.

## 8. Revision 1 - 2026-07-16 (intent & visual research wave)

Inputs: `docs/intent-canon-visual-fitness-2026-07-16.md` (intent canon + fitness audit)
and `docs/modding-research-2026-07-16.md` (ecosystem/API/asset research). W0.1 is DONE
(commit `faeb9d9`): role assets shipped, all role assignment guarded, role SEVEREs gone.

### New defects (add to Sec. 5 ledger)

| # | Item | Severity |
|---|---|---|
| 8 | `RuntimePerkManager.onPlayerTick()` never invoked from the runtime loop - all tick-based perk state dead (Accelerate, Bunny Hop, Semiaquatic, Ignite DoT, ghost lifetimes, eco-tree expiry) | HIGH - contradicts PERK_RUNTIME_STATUS "implemented" |
| 9 | `PlayerCombatLifecycleActions.onMobKilled()` never calls `RuntimePerkManager.afterMobKilled()` - Haunting ghosts and kill-trigger perks dead | HIGH |
| 10 | `ElementalReactionManager.applyMark()` has no ingress from ability combat - entire reaction system dormant | HIGH |
| 11 | Resolver keyword-routing brittleness (`hellfire`/`soul_scorch` can miss the fire branch and fall to void FX); `asRuntimeEffectId()` silently drops vanilla-path resolver rows | MED |
| 12 | Identity drifts: triceratops (3 conflicting model ids), summon `modelIds()` vs `modelId()`, orphaned `MOTM_Arbor_Sapling_Pink_Glow`, `crawler_void` missing from resolver | MED |

### Workstream changes

- **W0 gains W0.4**: fix defects #8-#10 (three wiring seams; small diffs, big behavioral
  unlock). Do BEFORE the perk family pass in W4 - perk visuals are pointless while ticks
  are dead. W0.2 (API migration) and W0.3 (casing audit) unchanged, still open.
- **New research gates (extend capability atlas before W2/W3)**: R6 `ParticleUtil`
  world-space burst (may delete proxy dependency from tether/field visuals); R7
  `EntityScaleComponent`; R8 pack-shipped custom `.particlesystem`; R9
  `PersistentDynamicLight` on non-player proxy; R10 `Intangible` phasing (smoke_form);
  R11 player-model cloning onto NPC proxy (shadow_step doppelganger).
- **New workstream WV - Visual Identity Pass** (runs beside W3-W5, feeds sweep gates):
  - WV.1 replace `Spark_Living` projectile/field proxies with native
    `Projectile_Config_MOTM_*` configs per family (magma_sling is the template);
    wind/psychic need surrogate or custom models.
  - WV.2 generate per-style EntityEffect stacks (`MOTM_{Class}_{Style}_*`) from the
    REALIGNMENT palette tables; target ~160 effects (52 today); theme candidates from
    the research doc catalog.
  - WV.3 fix HIGH misfits from the intent doc tables (smoke_form Bat, Portal_Teleport
    impact, raise_dead Shadow_Knight, blue hell_flame, rainbow, anchor_haul,
    self_petrification, vines).
  - WV.4 custom-asset shortlist (decision rule applied): pink psychic control halo
    particle system, opaque Aqua Barrier bubble shell model (COMMITTED per G6),
    projectile-shaped blue fire (interim uses vanilla `Fire_Blue` per G4).
    Removed: mist-humanoid model (G5 keeps player model + shroud).
- **Sweep gate tightened**: a style sweep cannot PASS with an open HIGH misfit for that
  style (intent doc Sec. 9 rule 4).
- **Grill session HELD 2026-07-16**: 14 decisions recorded in intent doc Sec. 10
  (G1-G14), including W1 contract inputs (pink outline marker, 15-20s dominate clock,
  recast-release, revert-to-normal-AI). Remaining Sec. 8 items shrink to per-ability
  minutiae; resolve during style sweeps with the Sec. 10 verification-pass rule.
