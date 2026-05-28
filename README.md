# Mystical Hytale Mod

`Mentees of the Mystical` is a Hytale mod prototype centered around:

- 4 elemental classes
- 40 combat styles
- 20 shared class-themed perk choices
- level 0-200 progression with spendable stat points
- title-band mob scaling with internal stat presets
- elemental reactions and runtime combat systems

## Project Layout

- `src/main/java/com/motm` - mod source
- `src/main/resources/data` - gameplay JSON data
- `docs/FRIEND_REVIEW_GUIDE.md` - orientation guide for outside reviewers and review tools
- `docs/ABILITY_REFERENCE.md` - GitHub-facing catalog of all 40 styles and 120 active abilities
- `docs/PERK_RUNTIME_STATUS.md` - final 20-perk concept/runtime/proof status
- `docs/hytale-capability-atlas/` - Hytale capability research, primitive choices, and implementation gates
- `PLAN.md` - original implementation plan used to start the project
- `scripts/build-install.ps1` - local build and install helper
- `scripts/ensure-dev-environment.ps1` - cross-platform setup helper that finds
  or downloads Java 25, resolves the Hytale install, and invokes the Gradle
  wrapper when requested
- `scripts/diagnose-dev-environment.ps1` - agent-friendly setup diagnostic and
  next-step walkthrough
- `scripts/setup-agent-workstation.ps1` - one-command setup entrypoint for an
  agent on a fresh machine; creates a setup diagnostic transcript on success or
  failure
- `AGENTS.md` - canonical agent rules, feature loop, and testing path
- `docs/hytale-complete-api-alignment-audit.md` - comparison against the local
  HytaleCompleteAPI reference repo, including quality smells and refactor
  priorities
- `scripts/run-agent-observability-baseline.ps1` - cross-platform agent verification slice that starts an in-mod observability run, drives MOTM dev commands, snapshots runtime state, and collects evidence
- `scripts/validate-content-shape.ps1` - validates authored ability data and
  scenario references, then writes generated content catalogs under
  `build/reports/motm/content-shape/`
- `scripts/check-architecture.ps1` - agent-facing architecture ratchets that
  prevent new generic ability-id branches, new main-plugin mutable runtime
  state, missing scenarios, and direct inventory mutation regressions
- `scripts/scenarios/` - JSON scenario catalog for extending the observability
  harness without hard-coding one-off command flows
- `scripts/collect-observability-evidence.ps1` - copies raw client logs, server logs, telemetry, MOTM observability JSONL, settings, and build metadata into a run bundle with indexes
- `scripts/query-observability-evidence.ps1` - lists runs, sources, events, and raw evidence windows for shell-based agents
- `scripts/audit-no-resource.ps1` - verifies the no-resource casting model across all classes/styles/abilities
- `scripts/run-perk-runtime-proofs.ps1` - in-world proof runner for the final shared perk runtime hooks
- `docs/agent-driven-verification-observability.md` - architecture contract and completion checklist for the verification platform
- `docs/runtime-architecture-refactor-checklist.md` - current runtime ownership map
  and refactor completion record
- `docs/runtime-architecture-final-evidence-summary.md` - PR-facing evidence
  ledger with run ids, scenario ids, evidence streams, and remaining
  `PASS`/`FAIL`/`UNKNOWN` state for the architecture refactor

## Build And Install

The project uses the Gradle Wrapper (`gradlew` / `gradlew.bat`) so agents and
humans do not need to install a matching Gradle version globally. The build still
requires a Hytale install because the mod compiles against the installed
`HytaleServer.jar`.

Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-install.ps1
```

macOS:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/build-install.ps1
```

That helper will:

- find or download a portable Temurin/OpenJDK 25 into `.tools/jdk-25`
- use the checked-in Gradle Wrapper to download/run Gradle 9.5.1
- resolve the Hytale root from `APPDATA`, `HYTALE_ROOT`, `-HytaleRoot`, or the
  standard macOS user-data path
- build the mod jar and install it into `Hytale/UserData/Mods`

Use this when a machine is not building cleanly:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/diagnose-dev-environment.ps1
```

For a remote Windows agent, the intended first instruction can be this simple:

```text
Pull the latest code, run powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-agent-workstation.ps1, inspect any printed diagnostics if it fails, and only then proceed to the observability harness.
```

Direct Gradle Wrapper usage is also supported after Java and Hytale are resolved:

```powershell
./gradlew -Dorg.gradle.java.installations.paths=".tools/jdk-25" -Pmotm_build_channel=internal build installMod
```

Static agent rails can run without launching the game:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/validate-content-shape.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/check-architecture.ps1
./gradlew validateContentShape checkArchitecture test
```

## Current Status

Implemented so far:

- Hytale Java plugin bootstrap and manifest wiring
- local build/install flow against the installed Hytale server jar
- command bridge for `/motm`
- data loading for classes, shared perks, styles, leveling, and mobs
- class/style ability identity with no race layer
- mob stat loading with Intern/Apprentice/Journeyman/Master title bands
- plugin lifecycle alignment with Hytale's `setup/start/shutdown`
- real server tick registration
- real mob spawn/death event hooks
- Terra/Quake vertical slice validated across two cold launches
- custom spellbook and development review surfaces
- Phase 7 perk runtime layer for stat modifiers and on-kill healing
- Phase 9 live validation for Terra/Metal, Hydro/Icicle, Aero/Wind Blade, Corruptus/Flame, Terra/Magma, and Hydro/Snow
- no-resource active ability model: all 40 styles and 120 abilities cast through cooldowns, durations, charges, action timing, positioning, and item conditions instead of class resource spending
- final shared 20-perk data layer and runtime hook manager for movement, damage, healing, projectile speed, ghost allies, and knockback

## Ability Reference

The current class/style/ability catalog is documented in [`docs/ABILITY_REFERENCE.md`](docs/ABILITY_REFERENCE.md).

For the intended ability fantasy, exact recovered function details, visual read,
and review priorities, start with [`docs/FRIEND_REVIEW_GUIDE.md`](docs/FRIEND_REVIEW_GUIDE.md), then read
[`CODEX_CLASS_STYLE_ABILITY_REVIEW_MOCKUP_2026-05-22.md`](CODEX_CLASS_STYLE_ABILITY_REVIEW_MOCKUP_2026-05-22.md) and
[`CODEX_CONCEPT_REVIEW_DECISIONS_2026-05-22.md`](CODEX_CONCEPT_REVIEW_DECISIONS_2026-05-22.md).

Current shape:

```text
MOTM
+-- 4 classes
+-- 40 styles
+-- 120 active abilities
+-- 20 shared passive perk choices
+-- 0 active ability resource costs
```

Use this check before claiming ability data and docs are still aligned:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/audit-no-resource.ps1
```

The current final perk contract and proof matrix are documented in [`docs/PERK_RUNTIME_STATUS.md`](docs/PERK_RUNTIME_STATUS.md). In an already loaded world, run:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-perk-runtime-proofs.ps1 -WorldName "MOTM Creative Test"
```

## Agent Observability Harness

This is the canonical in-game verification path for new feature work. Launch
Hytale through the official launcher, enter the target world, then run the
scenario harness. On a fresh machine, run `scripts/setup-agent-workstation.ps1`
first.

Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-agent-observability-baseline.ps1 -WorldName Main
```

macOS:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/run-agent-observability-baseline.ps1 -WorldName Main
```

To collect/index evidence from an existing session without driving commands:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/collect-observability-evidence.ps1 -WorldName Main
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/query-observability-evidence.ps1 -Action summary -RunId latest
```

For feature implementation, agents should choose or add a scenario that directly
exercises the changed behavior, run it with a stable `-RunId`, inspect the raw
and indexed evidence, and report `PASS`, `FAIL`, or `UNKNOWN` from that evidence.
If the current harness does not expose enough signal, extend the harness first:
new `/motm dev` commands, proof ids, JSONL events, collector sources, and query
modes are expected additions as long as raw evidence, manifests, provenance, and
rerunnable commands remain intact.

If a runtime command times out or the data directory cannot be resolved, inspect
the command log named in `baseline-report.md`. The command bridge writes
`dev-command-diagnostic.json` and `dev-command-diagnostic.md` into the run
directory with the selected MOTM data path, inbox/outbox state, Hytale process
summary, latest client log tail, and concrete recovery steps.

Example targeted run:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/run-agent-observability-baseline.ps1 `
  -WorldName Main `
  -RunId feature-check-20260525 `
  -ScenarioId terra-projectile-magma-sling
```

Older screenshot/log-tail testing plans remain as historical context or narrow
supplemental checks. Final acceptance for new behavior should be supported by an
`audits/agent-observability/<runId>/` bundle with raw MOTM JSONL streams,
client/server logs, telemetry, indexes, and a manifest.

## Runtime Architecture

Runtime ownership is intentionally narrow so feature agents can add behavior and
verification signal without guessing through `MenteesMod` internals:

```text
MenteesMod
+-- lifecycle wiring and public compatibility facade
+-- MotmRuntimeTasks for deferred tick work
+-- MotmInventoryOps for inventory mutations
+-- MotmPlayerInventory for combined Hytale player inventory access
+-- SpellbookInventoryItems for spellbook/dev-book item identity
+-- HydroContainerItems / Bridge for Hydro waterskin inventory state
+-- TerraInventoryResourceBridge / Policy for Terra resource inventory spending
+-- MotmDevCommandRouter / MotmCommandAuth for dev command surfaces
+-- MotmProofCatalog for proof ids and help text
+-- GameplayPlaybackManager, with shared geometry in MotmPlaybackGeometry
```

When adding a feature, extend the nearest owner first. New deferred runtime work
belongs in `MotmRuntimeTasks`; new proof probes belong in `MotmProofCatalog` and
the observability harness; new inventory mutation paths belong in
`MotmInventoryOps`; combined player inventory assembly belongs in
`MotmPlayerInventory`; spellbook/dev-book item identity belongs in
`SpellbookInventoryItems`; Hydro waterskin identity, tiering, inventory lookup,
and sync belong in `HydroContainerItems` and `HydroInventoryBridge`; new Terra
resource item/unit classification belongs in `TerraInventoryResourcePolicy`,
while inventory counting/spending belongs in `TerraInventoryResourceBridge`; new `/motm dev` routes belong behind
`MotmDevCommandRouter` and `MotmCommandAuth`. Keep raw evidence contracts intact when changing or
adding harness commands, and keep `AGENTS.md` plus `CLAUDE.md` aligned whenever
agent workflow expectations change.

The next architecture rail is described in
[`docs/agent-friendly-architecture-scaffolding.md`](docs/agent-friendly-architecture-scaffolding.md):
new runtime work should consume normalized `AbilityShape` data, grow the
appropriate `runtime.ability` family, add or update a scenario in
`scripts/scenarios/`, and delete obsolete legacy paths once parity is proven.
Compatibility facades are temporary and must not accumulate new behavior.
Treat legacy support as an explicitly registered exception, not a default
implementation strategy: user-data migrations and public command compatibility
may stay while they have active consumers, but old internal behavior paths
should be replaced, verified, and deleted in the same change whenever parity is
proven. Do not keep legacy code for speculative fallback, merge comfort, or
"maybe useful later" reasons. Any remaining compatibility exception belongs in
[`docs/compatibility-register.md`](docs/compatibility-register.md) with an
owner, consumer, and removal gate.

The project intentionally favors fresh-slate replacement over carrying old
internal shapes forward. Retain compatibility only for supported saved data,
public command/API contracts, or harness/tool contracts with active consumers.
When a new owner proves parity, migrate callers and delete the old path in the
same PR. New Java code that mentions legacy or compatibility must be listed as
an implementation boundary in
[`docs/compatibility-register.md`](docs/compatibility-register.md), or
`scripts/check-architecture.ps1` should fail and force the agent to either
delete the old path or document the real compatibility exception.

Old package layout, old manager responsibilities, duplicate data tables, and
stale method shapes are not compatibility contracts. They should be removed once
the replacement owner has tests or harness evidence. A PR that introduces a new
path while leaving the old path alive should also name the active consumer,
verification evidence, and removal gate; otherwise the correct cleanup is to
delete the old code.

Large rewrites are acceptable when they simplify the internal model. The project
should optimize for one trusted current path, backed by automated checks and
harness evidence, instead of keeping old implementations as speculative
fallbacks.

Compatibility means preserving supported behavior contracts, not preserving the
old code that happened to implement them. If an internal subsystem can be
replaced cleanly and parity can be proven by unit tests, static rails, or the
observability harness, agents should take the fresh-slate route and delete the
superseded path. A compatibility shim is only appropriate for named saved-data,
public-command/API, or harness/tool consumers, and it must be delegation-only
with a removal gate in
[`docs/compatibility-register.md`](docs/compatibility-register.md).

Treat deletion as part of the feature, not cleanup left for later. If a new
owner is trusted enough to run, migrate the callers, run the evidence, and
remove the obsolete owner in the same PR unless the compatibility register names
the active external consumer that still blocks removal.

Every feature/refactor pass should include a stale-path sweep: search for old
callers, duplicated lookup tables, manager-local ability branches, compatibility
comments, and neutral legacy names that the new owner replaces. A surviving old
path is a failed deletion gate unless it is a delegation-only registered
compatibility facade with an explicit removal condition.

For agent-driven work, prefer the fresh-slate path whenever it leaves fewer
concepts to understand. A larger rewrite that deletes obsolete internal
architecture is better than a narrow adapter that keeps two behavior owners
alive. If the old path is kept for more than delegation, the PR should be
treated as incomplete until it either migrates that logic into the new owner or
records a real compatibility exception in
[`docs/compatibility-register.md`](docs/compatibility-register.md).

Still in progress:

- deeper perk effect integration for damage triggers, transformations, auras, and advanced conditions
- broader in-game validation for the remaining 33 styles
- public release polish
