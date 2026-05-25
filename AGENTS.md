# MOTM Agent Guide

```
╔════════════════════════════════════════════════════════════════════╗
║ Canonical Feature Loop                                            ║
╠════════════════════════════════════════════════════════════════════╣
║ Read intent + references                                           ║
║   └─▶ make the smallest code/data change                           ║
║        └─▶ build/install internal jar                              ║
║             └─▶ run agent observability scenario                   ║
║                  └─▶ inspect raw + indexed evidence                ║
║                       └─▶ patch again or explain the result        ║
╚════════════════════════════════════════════════════════════════════╝
```

## Canonical Testing Path

- Use `docs/agent-driven-verification-observability.md` as the verification
  architecture contract.
- Use `scripts/run-agent-observability-baseline.ps1` as the default in-game
  harness for new feature work. It starts a MOTM observability run, drives
  `/motm dev` commands, snapshots server truth, collects logs/telemetry/packets,
  and writes `audits/agent-observability/<runId>/`.
- Use `scripts/collect-observability-evidence.ps1` when the game is already in a
  useful state and evidence needs to be bundled without driving a scenario.
- Use `scripts/query-observability-evidence.ps1` to inspect summaries, sources,
  events, and raw windows. Do not replace raw evidence with summaries.
- Older scripts such as `run-runtime-proofs.ps1`, `audit-phase9-class.ps1`,
  `acceptance-phase5.ps1`, `visual-validate.ps1`, and screenshot-heavy flows are
  historical or supplemental. Use them only when their specific narrow check is
  needed, and route final acceptance through the observability harness.

## Feature Work Verification

- A feature request is not complete until the changed behavior has been exercised
  through the agent observability harness, or until the agent explicitly explains
  why the change is docs/data-only and does not affect runtime behavior.
- Before coding, decide what in-game evidence would prove the feature works:
  scenario, style, ability, proof id, player state, world state, packets, logs,
  telemetry, or raw MOTM JSONL streams.
- If the existing baseline scenario does not cover the feature, extend the
  harness. Add a new scenario, `/motm dev` probe, proof command, JSONL event,
  collector source, or query mode as needed.
- Harness changes are welcome when they improve verification, but preserve the
  core trust contracts: raw files are retained, indexed summaries point back to
  raw evidence, every run has a stable run id and manifest, build/install
  provenance is captured, and results can be rerun from a documented command.
- Final feature answers should report the run id, the exact scenario or commands
  used, the evidence streams inspected, and whether the result is `PASS`,
  `FAIL`, or `UNKNOWN`.
- If the harness cannot surface enough signal to distinguish success from
  failure, add observability first and then rerun. Do not treat screenshots or
  inferred behavior as sufficient when server truth or MOTM telemetry can be
  exposed directly.

## Runtime Architecture Boundaries

```
MenteesMod
├─ lifecycle wiring + compatibility facade
├─ MotmRuntimeTasks owns deferred tick work
├─ MotmInventoryOps owns inventory mutations
├─ MotmDevCommandRouter + MotmCommandAuth own dev command routing/gating
├─ MotmProofCatalog owns proof ids/help text
└─ GameplayPlaybackManager delegates shared geometry to MotmPlaybackGeometry
```

- Add new deferred in-game work to `MotmRuntimeTasks`; do not add new ad hoc
  pending maps, sets, or queues to `MenteesMod`.
- Route inventory grants/removals/restores through `MotmInventoryOps` so
  transaction handling and failure logging stay consistent.
- Add new `/motm dev proof ...` ids to `MotmProofCatalog` before wiring runtime
  behavior, then exercise them through the observability harness.
- Keep `/motm dev` entry points gated through `MotmCommandAuth` and routed
  through `MotmDevCommandRouter`.
- Store lifecycle registration handles when the Hytale API returns them, and
  verify API signatures against the installed Hytale jar before assuming docs
  are current.
- When `GameplayPlaybackManager` grows, prefer extracting mechanically reusable
  slices such as geometry, effect application, visual proxy helpers, or
  ability-family runtimes while preserving the public manager API until tests
  prove the slice stable.

## Platform Notes

```
Windows              macOS
%APPDATA%\Hytale     ~/Library/Application Support/Hytale
PowerShell OK        PowerShell Core OK
official launcher    official launcher
same MOTM JSONL      same MOTM JSONL
```

- Always launch Hytale through the official launcher/auth flow unless a supported
  direct path is explicitly discovered and documented.
- On Windows, `%APPDATA%` normally points at the correct Hytale root. Run:
  `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-agent-workstation.ps1`
  on a fresh machine. It bootstraps Java 25, uses `gradlew.bat`, builds,
  installs, and writes `audits/setup-diagnostics/...` on success or failure.
- On macOS, the harness sets `APPDATA` to `~/Library/Application Support` when
  needed. Run:
  `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/run-agent-observability-baseline.ps1 -WorldName Main`
- Build/install should go through `scripts/build-install.ps1` or the checked-in
  Gradle Wrapper, not a machine-specific Gradle install. `scripts/build-install.ps1`
  and the observability baseline call `scripts/ensure-dev-environment.ps1`, which
  finds or downloads Java 25 and verifies the Hytale install path first.
- If setup fails, run
  `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/diagnose-dev-environment.ps1`
  and follow its concrete next steps. Inspect any printed
  `audits/setup-diagnostics/...` bundle before retrying; fix the concrete blocker
  rather than guessing.
- Screenshots/video are optional external artifacts. The trusted core evidence is
  server truth, causality, client intent, packet observations, logs, telemetry,
  manifests, and raw JSONL streams.

## Feature Verification Checklist

- Record the run id, world, style/ability/proof targets, and any assumptions.
- Build and install before in-game validation unless intentionally reusing an
  already-installed matching jar.
- Include at least one scenario that exercises the changed behavior directly.
- Inspect `manifest.json` and confirm `builtJarMatchesInstalled` when a build was
  part of the run.
- Inspect relevant `server-truth.jsonl`, `causality.jsonl`,
  `client-intent.jsonl`, `packets.jsonl`, client/server log indexes, and raw
  files before calling the behavior verified.
- If the evidence is ambiguous, add the missing observability and rerun. Do not
  paper over an unknown with screenshot interpretation.

## Data Protection

- `src/main/resources/data/styles/*.json` are protected. They contain carefully
  authored ability profiles. Never regenerate these files wholesale.
- If ability data must change, make surgical edits to specific fields and avoid
  reformatting, reordering, or rewriting nearby content.
- Preserve existing user changes in the worktree. Do not reset or revert
  unrelated edits.

## Design Boundaries

- MOTM is an RPG overlay mod on top of Hytale native systems.
- Do not create custom biomes, weapons, armor, or economy systems unless the user
  explicitly redirects the design.
- Styles are the only source of active abilities. Perks and races are passive
  identity/progression layers.
- Register Hytale hooks in `start()`, not constructors or `setup()`.
- Use `docs/hytale-complete-api-alignment-audit.md` and
  `/Users/viathebrink/codebases/HytaleCompleteAPI` as API-pattern references, but
  verify implementation details against the installed Hytale jar and live
  observability evidence before changing runtime behavior.
