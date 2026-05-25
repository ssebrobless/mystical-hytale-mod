# Mentees of the Mystical - Project Rules

`AGENTS.md` is the canonical guide for agent-driven feature work and testing.
Use the observability harness there for final in-game verification; this file
keeps the older Claude-oriented project rules in sync.

## Data Protection
- `src/main/resources/data/styles/*.json` are PROTECTED. These contain carefully authored ability profiles. NEVER regenerate these files wholesale. Only make surgical, targeted edits to specific fields.
- If you need to modify ability data, change only the specific field(s) requested. Do not reformat, reorder, or rewrite surrounding content.
- 3 "Restore" commits in git history show ability data was previously lost and had to be manually recovered.

## Design Principles
- This is a PURE RPG OVERLAY mod. It adds class/style/ability identity, perk progression, and mob scaling on top of Hytale's native systems.
- NEVER create custom biomes, weapons, armor, or economy systems - Hytale provides all of those natively.
- Styles = the ONLY source of active abilities (3 per style)
- Perks = ALWAYS passive bonuses (never active abilities)
- Races = passive identity bonuses

## Plugin Lifecycle
- `setup()` = data loading (JSON -> model objects). Runs before hooks are available.
- `start()` = hook registration (Hytale event listeners). Runs after the server is ready.
- NEVER register hooks in the constructor or in `setup()`.

## Build
- Windows build/install: `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-install.ps1`
- macOS build/install: `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/build-install.ps1`
- JDK 25 + Gradle Wrapper are canonical. `scripts/ensure-dev-environment.ps1`
  finds or downloads the portable JDK into `.tools/jdk-25` and runs the checked-in
  wrapper instead of requiring a machine-specific Gradle install.
- Internal builds install to the detected Hytale `UserData/Mods` directory.

## Testing
- Default in-game harness: `scripts/run-agent-observability-baseline.ps1`
- Evidence bundle: `audits/agent-observability/<runId>/`
- Query evidence with `scripts/query-observability-evidence.ps1`
- If the harness does not expose enough signal for a feature, extend the
  harness first: add a scenario, `/motm dev` probe, proof id, JSONL event,
  collector source, or query mode while preserving raw evidence and run
  manifests.
- Older screenshot/log-tail audit scripts are supplemental only. Do not use them
  as the final acceptance path for new behavior unless the observability harness
  has also produced a supporting run bundle.

## Runtime Architecture
- `AGENTS.md` is the source of truth for the feature loop and runtime ownership.
- Add deferred tick work to `MotmRuntimeTasks`, not new ad hoc pending
  collections in `MenteesMod`.
- Route inventory grants/removals/restores through `MotmInventoryOps`.
- Keep `/motm dev` gating/routing in `MotmCommandAuth` and
  `MotmDevCommandRouter`.
- Add discoverable runtime proofs through `MotmProofCatalog` and the proof runner
  registry, then exercise them through the observability harness.
- Keep `GameplayPlaybackManager`'s public API stable while extracting reusable
  slices such as geometry, visual proxy helpers, effect helpers, or ability
  family runtimes.

## Known Issues
- `GameplayPlaybackManager.java` is still large. Prefer small, test-backed slices
  that keep public behavior stable rather than broad rewrites.
- Custom HUD documents require `IncludesAssetPack=true`, and HUD install should be deferred briefly after join so the client can resolve the UI safely.
