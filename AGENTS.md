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
- Use `docs/agent-friendly-architecture-scaffolding.md` as the architecture
  contract for feature ownership, legacy deletion policy, runtime-family
  extension points, scenario catalog expectations, and debt-pattern ratchets.
- Use `docs/runtime-architecture-final-evidence-summary.md` as the PR-facing
  evidence ledger whenever closing, reviewing, or resuming the architecture
  refactor checklist.
- Before runtime validation, run the static agent rails:
  `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/validate-content-shape.ps1`
  and
  `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/check-architecture.ps1`.
  These are also wired into Gradle through `validateContentShape` and
  `checkArchitecture`.
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
- Prefer adding a JSON scenario under `scripts/scenarios/` before changing the
  baseline runner. Scenario ids, style ids, ability ids, and proof ids are
  validated by `scripts/validate-content-shape.ps1`.
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
├─ MotmPlayerInventory owns combined Hytale player inventory access
├─ SpellbookInventoryItems owns spellbook/dev-book item identity
├─ HydroContainerItems/HydroInventoryBridge own Hydro waterskin inventory state
├─ TerraInventoryResourceBridge/Policy own Terra inventory resource spending
├─ MotmDevCommandRouter + MotmCommandAuth own dev command routing/gating
├─ MotmProofCatalog owns proof ids/help text
└─ GameplayPlaybackManager delegates shared geometry to MotmPlaybackGeometry
```

- Authored ability JSON should now be read through the `AbilityShape` layer for
  new runtime work. New behavior should target the relevant runtime family under
  `runtime.ability` rather than adding ability-id branches to generic playback
  code.
- Add new deferred in-game work to `MotmRuntimeTasks`; do not add new ad hoc
  pending maps, sets, or queues to `MenteesMod`.
- Route inventory grants/removals/restores through `MotmInventoryOps` so
  transaction handling and failure logging stay consistent.
- Route combined Hytale player inventory assembly through `MotmPlayerInventory`
  instead of rebuilding hotbar/storage/backpack/tool/armor container lists in
  feature code.
- Keep spellbook and dev grimoire item ids in `SpellbookInventoryItems`;
  `MenteesMod` public methods may remain as delegation-only accessors while
  existing command/UI callers migrate.
- Keep Hydro waterskin item ids, metadata, tier detection, bridge lookup, and
  sync behavior in `HydroContainerItems` and `HydroInventoryBridge`; the plugin
  shell should only expose any public method names as delegation-only accessors.
- Keep Terra resource item matching and units-per-item policy in
  `TerraInventoryResourcePolicy`, and Terra inventory count/spend bridge logic
  in `TerraInventoryResourceBridge`; the plugin shell should only wire the
  bridge into `ResourceManager`.
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
- Treat refactors as replacement work, not preservation work. Once a new owner
  proves parity, migrate callers and delete the old behavior path in the same
  PR. Do not keep duplicate implementations, shadow branches, or neutral
  "legacy" names for future convenience.
- Compatibility is an exception for user data, public commands, or harness
  contracts that still have an active consumer. It is not a reason to keep old
  internal architecture. Temporary facades must be delegation-only and listed in
  `docs/compatibility-register.md` with an owner, consumer, and removal gate.
- If legacy support cannot be deleted yet, isolate it as migration or
  compatibility code, make the preferred replacement obvious, and add the
  evidence needed to remove it. Agents should expand the new owner, not the old
  path.
- Do not preserve legacy code for speculative fallback, merge comfort, or vague
  "maybe useful later" reasons. If no active external consumer or user-data
  migration can be named, delete it. If an active consumer can be named, record
  the exception and the evidence required to delete it next.
- When replacing a subsystem, prefer a clean fresh-slate owner over adapters
  that keep old internal concepts alive. Public method names may remain
  temporarily as delegation-only facades, but new feature logic belongs in the
  new owner.
- Do not treat large deletions or rewrites as inherently risky when they remove
  internal bloat. The risk question is whether supported behavior is proven by
  tests or harness evidence, not whether old code remains available as a
  fallback.
- Every implementation pass should include an explicit deletion pass. After the
  new owner works, search for stale callers, duplicated tables, old helper
  names, compatibility comments, and manager-local branches that the new owner
  replaces. Delete them in the same PR unless the compatibility register names
  the active external consumer and the removal gate.

## Fresh-Slate Refactor Contract

Treat old code as guilty until it proves it protects a real external contract.
The goal is not to keep MOTM backwards-compatible with its own internal bloat;
the goal is to protect users, operators, and harness consumers while giving the
codebase the cleanest current shape. When a cleaner owner exists or can be
created, the correct end state is:

```
old caller -> new owner -> harness evidence -> old path deleted
```

- Fresh-slate replacement is the normal path. Legacy support is the exception
  path. Do not start from "what can we preserve"; start from "what behavior is
  still externally owed, and what code can disappear once that behavior is
  proven elsewhere?"
- Preserve supported behavior, not historical implementation. If a clean rewrite
  gives the codebase a single clearer model and tests/harness evidence can prove
  parity, prefer the rewrite over layering adapters around the old shape.
- Default to deleting old internal code after migration. Keeping both the old
  and new implementation paths is a failed refactor unless the compatibility
  register names the active external contract being protected.
- "Legacy" is never a vague status label. It is a temporary support burden for
  a named saved-data, public-command/API, or harness/tool consumer. Anything else
  is ordinary technical debt and should be removed.
- Prefer a full replacement over an adapter stack when the old concepts make
  future feature work harder to reason about. The fresh-slate owner should
  become the only obvious place to extend the feature.
- Do not keep old implementations because they make the diff smaller, reduce
  merge anxiety, provide speculative rollback, or feel safer for a future agent.
- A retained old path must protect one of three things: supported saved data, a
  public command/API that an operator still uses, or a harness contract that
  another agent/tool relies on.
- Every retained path needs a compatibility-register entry in the same PR,
  including the implementation boundary, active consumer, preferred replacement,
  verification evidence, and removal gate.
- New code should make the new owner the easiest place to extend. If an old
  path remains, name it `legacy`, `migration`, or `compatibility` rather than a
  neutral feature name, so agents do not accidentally build on it.
- Never keep an old path just to make future agents feel safer. Trust comes from
  contracts, tests, harness evidence, and raw observability, not from keeping a
  fallback implementation that no one actively owns.
- Before adding an adapter or shim, ask whether the caller can be migrated and
  the old implementation deleted in this PR. If yes, do that. If no, explain the
  exact behavior that still depends on the old path and add evidence that will
  let the next PR remove it.
- Do not create compatibility for internal package names, old method shapes,
  duplicate lookup tables, stale manager responsibilities, or outdated data
  flow. Those are implementation details, not compatibility contracts.
- Do not create "bridge" abstractions that merely preserve old vocabulary. A
  bridge is acceptable only when it is delegation-only, protects an active
  external contract, and is listed in the compatibility register with a deletion
  gate.
- When deleting old code, update nearby docs, scenarios, and agent instructions
  in the same PR so future agents see the new path as the only normal path.
- When reviewing a refactor, deletion is positive evidence. A smaller surviving
  concept graph is usually better than a smaller diff. Prefer the change that
  leaves one trusted owner over the change that leaves old and new paths
  coexisting.
- Review PRs with a deletion checklist: what old path disappeared, what
  compatibility exception remains, who consumes it, what evidence proves the new
  path, and what gate deletes the exception next.
- Treat a surviving old path as a failing deletion gate, not as harmless
  context. It must either become a delegation-only registered compatibility
  facade or disappear before the work is called complete.
- `scripts/check-architecture.ps1` intentionally fails new Java
  legacy/compatibility mentions unless the file is listed in
  `docs/compatibility-register.md`. Either delete the stale code, register the
  real exception, or improve the new owner until the old path is unnecessary.

## Required Fresh-Slate Review

Every implementation or refactor pass must actively decide what old shape can
be removed. Treat this as part of building the feature, not as optional cleanup
afterward.

```
╔════════════════════════════════════════════════════════════════════╗
║ Fresh-Slate Review                                                ║
╠════════════════════════════════════════════════════════════════════╣
║ name the old concept being replaced                               ║
║   └─▶ prove the new owner with tests/static rails/harness evidence ║
║        └─▶ migrate callers                                        ║
║             └─▶ delete old code, tables, branches, docs, scripts   ║
║                  └─▶ register only real external compatibility     ║
╚════════════════════════════════════════════════════════════════════╝
```

- Start from the desired current architecture, not from preserving historical
  seams. If the old internal model is awkward, broad, duplicated, or confusing,
  replace it outright once supported behavior can be proven.
- Do not keep legacy code because another branch might have used it, because it
  makes review feel safer, or because it could hypothetically help debugging.
  Debuggability should come from observability, scenarios, raw evidence, and
  focused tests.
- Do not introduce "legacy mode," "old path," fallback behavior, bridge classes,
  adapter stacks, or duplicate tables unless they protect a named saved-data,
  public command/API, or harness/tool contract. If they do, the implementation
  boundary must be in `docs/compatibility-register.md` before the PR is
  considered complete.
- If a retained path needs feature logic, scheduling, policy decisions, damage
  semantics, targeting, state mutation, or ability-specific branching, it is not
  compatibility code. Move that logic into the new owner and leave at most a
  delegation-only facade.
- A fresh-slate rewrite is the preferred answer when it reduces the concept
  count future agents must carry. The project values one trusted current path
  over a smaller diff that leaves old and new paths coexisting.
- PR summaries should explicitly say what obsolete concepts were deleted, what
  compatibility exceptions remain, who consumes each exception, and what
  evidence will delete it next.

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
- If runtime command delivery fails, inspect the command log named in
  `baseline-report.md`. `scripts/send-dev-command.ps1` writes
  `dev-command-diagnostic.json` and `.md` into the run directory with process
  state, latest Hytale log tail, inbox/outbox paths, and recovery steps.
- Screenshots/video are optional external artifacts. The trusted core evidence is
  server truth, causality, client intent, packet observations, logs, telemetry,
  manifests, and raw JSONL streams.

## Feature Verification Checklist

- Record the run id, world, style/ability/proof targets, and any assumptions.
- Build and install before in-game validation unless intentionally reusing an
  already-installed matching jar.
- Include at least one scenario that exercises the changed behavior directly.
- Run `validateContentShape`, `checkArchitecture`, and relevant Java tests before
  launching the heavier in-game harness.
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
- Styles are the only source of active abilities. Perks are passive
  progression layers. Races are intentionally not part of this mod.
- Register Hytale hooks in `start()`, not constructors or `setup()`.
- Use `docs/hytale-complete-api-alignment-audit.md` and
  `/Users/viathebrink/codebases/HytaleCompleteAPI` as API-pattern references, but
  verify implementation details against the installed Hytale jar and live
  observability evidence before changing runtime behavior.
