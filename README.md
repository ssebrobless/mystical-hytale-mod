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
- `scripts/collect-observability-evidence.ps1` - copies raw client logs, server logs, telemetry, MOTM observability JSONL, settings, and build metadata into a run bundle with indexes
- `scripts/query-observability-evidence.ps1` - lists runs, sources, events, and raw evidence windows for shell-based agents
- `scripts/audit-no-resource.ps1` - verifies the no-resource casting model across all classes/styles/abilities
- `docs/agent-driven-verification-observability.md` - architecture contract and completion checklist for the verification platform
- `docs/runtime-architecture-refactor-checklist.md` - current runtime ownership map
  and refactor completion record

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

Direct wrapper usage is also supported after Java and Hytale are resolved:

```powershell
./gradlew -Dorg.gradle.java.installations.paths=".tools/jdk-25" -Pmotm_build_channel=internal build installMod
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
+-- 0 active ability resource costs
```

Use this check before claiming ability data and docs are still aligned:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/audit-no-resource.ps1
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

Example targeted run:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/run-agent-observability-baseline.ps1 `
  -WorldName Main `
  -RunId feature-check-20260525 `
  -StyleId terra `
  -Abilities terra_quake `
  -Proofs terra_quake
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
+-- MotmDevCommandRouter / MotmCommandAuth for dev command surfaces
+-- MotmProofCatalog for proof ids and help text
+-- GameplayPlaybackManager, with shared geometry in MotmPlaybackGeometry
```

When adding a feature, extend the nearest owner first. New deferred runtime work
belongs in `MotmRuntimeTasks`; new proof probes belong in `MotmProofCatalog` and
the observability harness; new inventory mutation paths belong in
`MotmInventoryOps`; new `/motm dev` routes belong behind `MotmDevCommandRouter`
and `MotmCommandAuth`. Keep raw evidence contracts intact when changing or
adding harness commands, and keep `AGENTS.md` plus `CLAUDE.md` aligned whenever
agent workflow expectations change.

Still in progress:

- deeper perk effect integration for damage triggers, transformations, auras, and advanced conditions
- broader in-game validation for the remaining 33 styles
- public release polish
