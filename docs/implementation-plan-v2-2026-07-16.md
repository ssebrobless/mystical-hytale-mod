# Implementation Plan v2 - 2026-07-16 (the whole mod, 100% proven ground)

Supersedes the execution sections of `docs/completion-plan-2026-07-16.md` (its analysis,
defect ledger, and canon precedence remain valid). Every work item below references a
proposal in `docs/improvement-proposals-2026-07-16.md` (T/H/A/C/P/U ids), an author
decision (G1-G14), or a live-verified gate (`docs/hytale-capability-atlas/
gate-results-2026-07-16.md`). Nothing here rests on an unproven mechanism.

Definition of done is unchanged (completion plan Sec. 7): 11 primitives proven, 40
styles sweep-passed, 20 perks live-proven, leveling loop complete, zero [removal]
warnings, zero load SEVEREs, public jar on CurseForge.

```
Phase 0        Phase 1              Phase 2                Phase 3
HARDEN ──────▶ FOUNDATIONS ───────▶ PRIMITIVE ENGINES ───▶ CONTENT PASSES ──▶ Phase 4
(1 session)    (manifest resolver   (control, tethers,     (per-class visual   SWEEP &
               + quick wins)        projectiles, summons,  + perk/reaction     RELEASE
                                    fields)                passes, parallel)
```

## Phase 0 - Hardening (single session, all PROVEN, mostly S-effort)

| item | source | content |
|---|---|---|
| 0.1 API migration (W0.2) | completion plan defect #2 | Migrate the 17 [removal] call sites: `getSectionById(InventoryComponent.*_SECTION_ID)`, `InventoryComponent.getItemInHand(accessor, ref)`, `MotmCraftRecipeEventSystem` on `CraftRecipeEvent.Post` (EntityEventSystem pattern), `PlayerRef.sendMessage`. Gate: zero [removal] warnings. |
| 0.2 Wiring seams (W0.4) | defects #8-10 | Invoke `RuntimePerkManager.onPlayerTick()` from the runtime loop; call `afterMobKilled()` from `PlayerCombatLifecycleActions`; give `ElementalReactionManager.applyMark()` its combat ingress. Gate: Haunting ghost spawns on kill in live test; a two-element combo triggers one reaction. |
| 0.3 Case-sensitivity audit (W0.3) | defect #6 | One-pass grep of all resource-path strings vs on-disk casing. |
| 0.4 Soul Harvest transition | C12 | Atomic lethal-save (consume 5 -> 50% HP -> 600s lockout, exactly once) + one-shot Praetorian/Spectre burst. |

## Phase 1 - Foundations (unblocks everything downstream)

| item | source | content | gate |
|---|---|---|---|
| 1.1 Manifest resolver | U1 (E-P1/H11/A9/C1) | `data/ability-visual-manifest.json` keyed by ability id -> {cast,travel,impact,loop,model,role,projectileConfig}; preflight validation via asset maps + hasRoleName; `HytaleAssetResolver` becomes lookup+fail-closed; all adapters consume manifest rows. Seed the 120 rows from the current resolver truth + intent-doc fixes (hellfire->fire, etc.). | preflight Errors=0 with all 120 rows resolving; hellfire proof shows blue-fire family |
| 1.2 Quick-win visual batch | A4, T7, C5, C1, P5, E-P8 | Portal_Teleport removal (Dagger_Dash cues); sand identity (Sand_Storm/Block_Break_Sand); blue fire interim (Fire_Blue family per-phase); per-class sound palettes via effect/projectile JSON fields (zero Java). | per-item visual/audio capture in proof world |
| 1.3 HUD upgrades | P3, P6, P7 | Class-passive icon/counter tracker rows (vanilla status icons + quality frames); mob nameplate `<Name> - <Band> / Level n`; 20-perk HUD semantics (icon/state/PROC flash). | screenshot review vs ui-canon |

## Phase 2 - Primitive Engines (the P0/P1 program, in dependency order)

| item | source | content | acceptance gate |
|---|---|---|---|
| 2.1 Controlled Ally (W1) | Sec. 0 merged spec (E-P5+C2), G1, G2 | `runtime/ability/control/` state+tick+adapter; transient OverrideAttitude + summon-runtime behavior reuse (role swap only if behavior proof fails); `isOwnedFriendlyOrControlled` FF filter; pink marker (tint+shimmer interim, R8 halo now unblocked); dominate 15-20s recast-release clock; hivemind radius variant; mind_shatter resolveCenter. New harness: control_acquired/released events, controlled-ally scenario (2 hostiles, control 1, prove it attacks the other). | the G2 gate verbatim: hostile converts, visibly fights hostiles, follows caster, cannot hurt allies, releases/cleans, marker visible |
| 2.2 Summon migration | U3 (C3/C4/C10/H4/H8) | All combat summon roles -> Template_Summoned_Ally Variants per the C4 table (skeletons, Crawler_Void x3, Scarak egg-hatch trio, Broodmother queen, Crocodile, Snowman, dark-human shadow clone per R11 verdict); SummonLifecycleHytaleAdapter simplification; reusable summon-acceptance scenario. | one non-Snow summon family passes the full fight/leash/cleanup gate (primitive-status requirement) |
| 2.3 Tether engine | U4 (E-P2/T3/H3/C9), R6 PASS | TetherStateService + TetherTickSystem + particle-chain renderer (primary, now proven) + chain-segment entity fallback impl; skins: vine/water-beam/chain/void/wind; anchor_haul hook projectile; rip_current keeps fluid-trace exception. | vines + anchor_haul pass link-visibility/sync/cleanup proofs |
| 2.4 Projectile actors | U2 (E-P3/T1/H2/A1/A2/A3), R7 PASS | Family-by-family `Projectile_Config_MOTM_*` + appearance JSONs per the 12-family table; geometry specials (gale_cutter X, razor_wind 5-sequence volley state, chain_lightning per-hop bursts, pressure_burst R7 growth + Charging verb); retire Spark_Living production use. Order: earth (T1) -> ice (H2) -> wind (A1/A2) -> lightning (A3) -> remaining families. | per-family travel/impact proof; no Spark_Living in production paths |
| 2.5 Field/object engine | T2 + E-P6, R6 PASS | Physical-object-first temp-block service (owner/original/expiry records) + ParticleFieldRenderer replacing proxy pulses; weather-as-ability reserved for isolated-world storm perks (H10 scoping). | sinkhole + smoke_bomb field proofs; zero proxy refs after expiry |
| 2.6 Transformation + dash polish | A5/A6/A7, T4, G5, R10 PASS | smoke_form = shroud+Intangible+50% projectile DR (Bat removed); dino forms on Trillodon (committed) with form actions/hotbar/exit per canon; dash family = Zephyr-pattern per-ability state (burst-not-teleport cues). | transformation family gate (model identity, actions, exit, third-person capture); dust_devil passes the no-teleport read |

## Phase 3 - Content Passes (parallel lanes once engines exist)

| lane | source | content |
|---|---|---|
| 3.T Terra | T2 fields, T8 pillar, T9 gem (light-BLOCK glow per R9 verdict), T10 magma, T11 arbor/bloom, T12 quake footprints, T5 Immovable prime cue | per-style physical constructs + effect stacks |
| 3.H Hydro | H1 six-palette recipes (18 abilities), H5 Frosty true mount, H6 rainbow light-arc, H9 vapor/iceberg, P4 Aqua Barrier interim -> G6 custom opaque shell (R8-proven pipeline) | palette identity + mount + barrier |
| 3.A Aero | A8 nine style stacks, A10 scream/battle_cry (r=8) | per-style identity |
| 3.C Corruptus | C6 atonement white/gold/purple, C7 psychic language + R8 halo, C8 imbuement stances, C9 necro grammar, C5 custom blue-fire projectile (R8) | style identity + stance system |
| 3.P Passives/Reactions | P1 five trigger-perk visuals, P2 six reaction bursts + HUD pings, P8 feedback budget | after 0.2 wiring |
| 3.X Custom assets (R8-proven pipeline) | G1 halo, G4 projectile blue fire, G6 bubble shell model, C8 flash, H6 arc (conditional) | Blockbench/particle authoring batch |

Each lane ships with its abilities' manifest rows + proof scenarios; the tightened sweep
gate applies (no style passes with an open HIGH misfit).

## Phase 4 - Sweep, Leveling, Release

| item | content |
|---|---|
| 4.1 Style sweeps (W5) | 32 partial styles vs style-ability-intent-canon via run-style-observability-sweep parallel lanes (disjoint -Styles, distinct RunIds); reopen/close ABILITY_COMPLETION_CHECKLIST rows on evidence |
| 4.2 Leveling loop (W4 tail) | LevelingManager TODO stubs: XP/level-up/milestone events + notifications, DoT via damage API, stat recalc; retire legacy PerkManager/SynergyEngine log-only paths or wire them |
| 4.3 Cross-cutting cleanup pass | P2 no-drop/no-dummy regression across all temporary entities/blocks/visuals |
| 4.4 Public release (W6) | public-readiness + canon-drift + no-resource audits; public jar (no -internal), manifest at root, asset pack verified; CurseForge upload |

## Sequencing rules

1. Phase 0 and 1.2/1.3 can interleave; 1.1 (manifest) MUST land before Phase 2 items
   that route visuals (2.3-2.5) and before any Phase 3 lane.
2. 2.1 before 2.2 (control forces the faction/FF layer summons reuse) - per the
   primitive-status checkpoint's own rationale.
3. Phase 3 lanes are fully parallel (subagent lanes); each depends only on the engines
   its abilities use.
4. Live proof cadence: every engine/lane gate needs a human (or idle desktop) in
   "MOTM Creative Test" - batch gate confirmations into sessions like the R6-R11 run.
5. Update `docs/primitive-status-*.md` after each Phase 2 gate (standing working rule).

## Risk register (post-gates)

- R9 dynamic light FAILED: all glow via light blocks - already reflected above.
- R11 partial: shadow clone = dark default-human silhouette; do not promise gear/skin.
- NoesisGUI migration (community roadmap): keep HUD/page code behind current seams.
- API churn: manifest preflight + MotmNpcRoles guards + deprecation ledger re-check per
  game update (catalog README procedure).
