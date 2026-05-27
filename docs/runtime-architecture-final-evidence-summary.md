# Runtime Architecture Refactor Evidence Summary

Updated: 2026-05-26

This is the PR-facing evidence ledger for the architecture refactor checklist.
It is intentionally separate from the design rationale so a reviewing agent can
quickly see what is proven, which commands or run ids back each claim, and
whether any `UNKNOWN` areas remain.

## Current Status

```
╔════════════════════════════════════════════════════════════════════╗
║ Evidence State                                                    ║
╠════════════════════════════════════════════════════════════════════╣
║ Static architecture rails: PASS                                   ║
║ Content/scenario validation: PASS                                 ║
║ Unit/regression tests: PASS                                       ║
║ Internal tester build/install: PASS                               ║
║ Historical live runtime-family scenarios: PASS                    ║
║ Latest post-refactor live command scenario: PASS                  ║
║ Latest post-refactor live proof baseline: PASS                    ║
║ Remaining blocker: none known                                     ║
╚════════════════════════════════════════════════════════════════════╝
```

The final live facade proof passed after Hytale was launched through the
official launcher, world `Main` was loaded, and the internal tester jar was
restarted onto the player-ready lifecycle fix.

## Local Rails

Latest local verification on this branch:

- `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/check-architecture.ps1`
  - PASS.
- `pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/validate-content-shape.ps1`
  - PASS.
- PowerShell parser check for `scripts/run-agent-observability-baseline.ps1`
  - PASS.
- `./gradlew test --warning-mode all`
  - PASS.
- `./gradlew compileJava --warning-mode all`
  - PASS.
- `./gradlew build --warning-mode all`
  - PASS.
- `./gradlew -Pmotm_build_channel=internal -Pmotm_internal_test_build=true installMod --warning-mode all`
  - PASS.
- `git diff --check`
  - PASS, with known line-ending warnings for `scripts/run-agent-observability-baseline.ps1`
    and `scripts/send-dev-command.ps1`.

## Architecture Ratchets

Current static rails prove:

- `MenteesMod` owns zero mutable runtime collections.
- `MenteesMod` owns zero `processPending...` methods.
- `GameplayPlaybackManager` owns zero raw runtime collection fields.
- `GameplayPlaybackManager` owns zero direct migrated ability-state
  constructions.
- Generic runtime code cannot add new ability-id branching outside approved
  ability runtime/profile layers.
- `GameplayPlaybackManager` cannot reintroduce migrated projectile, field,
  terrain, summon, follow-up, transformation, self, channel, ability-specific,
  execution-policy, effect-id, status-effect, or runtime-math ownership without
  failing `scripts/check-architecture.ps1`.

Manual static smell check:

- The targeted search for manager-local `isXAbility(...)` helpers and
  `ability.getId()` switch/equality branches in
  `src/main/java/com/motm/manager/GameplayPlaybackManager.java` returned no
  matches.

## Harness Contract

Current harness contract proof:

- Scenario `expectedEvidence` entries are validated as `source:type`.
- All scenario files now require `causality:runtime_task_executed` evidence
  where scenario commands exercise deferred runtime task processing.
- `scripts/run-agent-observability-baseline.ps1` derives task-type expectations
  from scenario commands and requires `runtime_task_executed` evidence for:
  - `style-test-mob-spawn`
  - `style-test-mob-count`
  - `style-test-mob-clear`
- Runtime command failure diagnostics produce run-local
  `dev-command-diagnostic.json` and `dev-command-diagnostic.md` with process
  state, latest client log tail, inbox/outbox paths, and recovery steps.
- The final live smoke requires concrete `runtime_task_executed` evidence for
  scenario-driven style-test spawn/count/clear tasks and passed against the
  running official Hytale session.
- The final baseline proof scenario requires `runtime_task_executed`,
  `proof_end`, and `CustomHud` packet evidence and passed after the same
  player-ready lifecycle fix.

## Historical Live Evidence

These runs were collected on macOS with an internal tester jar loaded and passed
before the latest harness assertion tightening:

| Run id | Scenario id | Primary evidence streams | Status |
| --- | --- | --- | --- |
| `mac-internal-baseline-20260525-1441` | `baseline` | `control`, `causality`, `packets` | PASS |
| `mac-command-observability-smoke-20260525-1442` | `command-observability-smoke` | `control`, `causality`, `packets` | PASS |
| `mac-terra-projectile-magma-sling-20260525-1442` | `terra-projectile-magma-sling` | `causality`, `client-intent` | PASS |
| `mac-terra-field-iron-wall-20260525-1442` | `terra-field-iron-wall` | `causality`, `server-truth` | PASS |
| `mac-hydro-summon-snow-imp-20260525-1442` | `hydro-summon-snow-imp` | `causality`, `client-intent` | PASS |
| `mac-aero-transformation-smoke-form-20260525-1442` | `aero-transformation-smoke-form` | `causality`, `client-intent` | PASS |
| `mac-terra-followup-alloy-enhancement-20260525-1452` | `terra-followup-alloy-enhancement` | `causality`, `server-truth` | PASS |

These runs prove the migrated runtime-family behavior that existed at that
checkpoint. The tightened command observability smoke has now also passed in the
latest live evidence below.

## Latest Live Attempts

Run id: `mac-command-observability-smoke-20260526-refactor`

Scenario id: `command-observability-smoke`

Status: `UNKNOWN`

What passed:

- Static scenario validation.
- Environment bootstrap.
- Internal build/install.
- Installed jar internal tester verification.

What blocked:

- First runtime command timed out because no Hytale process was running.
- `pgrep -fl Hytale` also returned no running Hytale process after the latest
  local rails and internal install.

Diagnostic files:

- `audits/agent-observability/mac-command-observability-smoke-20260526-refactor/baseline-report.md`
- `audits/agent-observability/mac-command-observability-smoke-20260526-refactor/dev-command-diagnostic.md`
- `audits/agent-observability/mac-command-observability-smoke-20260526-refactor/dev-command-diagnostic.json`

Run id: `mac-command-observability-smoke-20260526-mainmenu`

Scenario id: `command-observability-smoke`

Status: `UNKNOWN`

What passed:

- Static scenario validation.
- Installed jar internal tester verification.
- Official launcher successfully started authenticated `HytaleClient` with the
  user's session.

What blocked:

- First runtime command timed out because the client was still at the main menu
  and world `Main` was not loaded.
- Diagnostic showed `HytaleClient` process count `1`, but
  `dev-command-outbox.log` length and last write time did not advance.
- The outbox tail remained from `mac-terra-followup-alloy-enhancement-20260525-1452`,
  proving the active server-side MOTM command bridge was not consuming the new
  inbox command.

Diagnostic files:

- `audits/agent-observability/mac-command-observability-smoke-20260526-mainmenu/baseline-report.md`
- `audits/agent-observability/mac-command-observability-smoke-20260526-mainmenu/dev-command-diagnostic.md`
- `audits/agent-observability/mac-command-observability-smoke-20260526-mainmenu/dev-command-diagnostic.json`

Required next live command after official launcher/world state is ready.
If the already-running client is on the installed internal tester jar, prefer
`-SkipBuild` because a rebuild/install cannot affect that process until Hytale
is restarted:

```powershell
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/run-agent-observability-baseline.ps1 `
  -WorldName Main `
  -ScenarioId command-observability-smoke `
  -RunId mac-command-observability-smoke-20260526-final `
  -SkipBuild `
  -SkipScreenshot
```

For a fresh run, first build/install the internal tester jar, launch Hytale
through the official launcher, enter world `Main`, and then omit `-SkipBuild`
only if the script is responsible for installing before launch.

Run id: `mac-command-observability-smoke-20260526-final`

Scenario id: `command-observability-smoke`

Status: `FAIL`

What passed:

- Static scenario validation.
- Installed jar internal tester verification.
- Official launcher/world startup.
- Dev command bridge consumption.

What failed:

- Every dev command returned `[MOTM] Error: Player data not found.`
- The harness correctly failed because `runtime_task_executed` evidence for
  `style-test-mob-spawn` was missing.

Root cause and fix:

- Hytale can fire `onPlayerConnect` before the runtime player has a stable
  store/world reference. The refactored lifecycle therefore skipped player-data
  creation at connect time.
- `PlayerSessionLifecycleActions.onPlayerReady(...)` now also guarantees MOTM
  player-data initialization and saved-loadout rebuild when ready is the first
  stable identity point.
- Focused regression:
  `./gradlew test --tests 'com.motm.runtime.player.PlayerSessionLifecycleActionsTest' --warning-mode all`
  passed.

Diagnostic files:

- `audits/agent-observability/mac-command-observability-smoke-20260526-final/baseline-report.md`
- `audits/agent-observability/mac-command-observability-smoke-20260526-final/report.md`

Run id: `mac-command-observability-smoke-20260526-final-readyfix`

Scenario id: `command-observability-smoke`

Status: `PASS`

What passed:

- Static scenario validation.
- Installed jar internal tester verification.
- Official launcher authenticated the user session.
- World `Main` loaded into a singleplayer Hytale server.
- `onPlayerReady` backfilled `onPlayerJoin`, rebuilt the saved Terra/metal
  loadout, and installed the HUD.
- Dev command bridge executed every scenario command successfully.
- Evidence collection copied 14 sources, indexed 3,540 client-log rows, 2,207
  server-log rows, and 71 telemetry rows.
- Harness assertions found the expected evidence streams:
  `control:dev_command_executed`, `causality:server_tick_heartbeat`,
  `causality:runtime_task_executed`, and `packets:CustomHud`.

Evidence files:

- `audits/agent-observability/mac-command-observability-smoke-20260526-final-readyfix/baseline-report.md`
- `audits/agent-observability/mac-command-observability-smoke-20260526-final-readyfix/report.md`
- `audits/agent-observability/mac-command-observability-smoke-20260526-final-readyfix/control-requests.jsonl`
- `audits/agent-observability/mac-command-observability-smoke-20260526-final-readyfix/raw/motm-observability/control.jsonl`
- `audits/agent-observability/mac-command-observability-smoke-20260526-final-readyfix/raw/motm-observability/causality.jsonl`
- `audits/agent-observability/mac-command-observability-smoke-20260526-final-readyfix/raw/motm-observability/packets.jsonl`
- `audits/agent-observability/mac-command-observability-smoke-20260526-final-readyfix/raw/motm-observability/server-truth.jsonl`
- `audits/agent-observability/mac-command-observability-smoke-20260526-final-readyfix/raw/client-logs/2026-05-26_01-05-33_client.log`
- `audits/agent-observability/mac-command-observability-smoke-20260526-final-readyfix/raw/server-logs/2026-05-26_01-06-05_server.log`

Run id: `mac-baseline-proof-spellbook-20260526-final-readyfix`

Scenario id: `baseline`

Status: `PASS`

What passed:

- Static scenario validation.
- Installed jar internal tester verification.
- Command flow and target setup.
- Proof id `coating-metal`.
- Evidence collection copied 14 sources, indexed 3,569 client-log rows, 2,235
  server-log rows, and 72 telemetry rows.
- Harness assertions found the expected evidence streams:
  `control:dev_command_executed`, `causality:runtime_task_executed`,
  `causality:proof_end`, and `packets:CustomHud`.

Evidence files:

- `audits/agent-observability/mac-baseline-proof-spellbook-20260526-final-readyfix/baseline-report.md`
- `audits/agent-observability/mac-baseline-proof-spellbook-20260526-final-readyfix/report.md`
- `audits/agent-observability/mac-baseline-proof-spellbook-20260526-final-readyfix/control-requests.jsonl`
- `audits/agent-observability/mac-baseline-proof-spellbook-20260526-final-readyfix/raw/motm-observability/control.jsonl`
- `audits/agent-observability/mac-baseline-proof-spellbook-20260526-final-readyfix/raw/motm-observability/causality.jsonl`
- `audits/agent-observability/mac-baseline-proof-spellbook-20260526-final-readyfix/raw/motm-observability/packets.jsonl`

## Completion Items

- `GameplayPlaybackManager` facade proof:
  - Static rails and targeted searches show migrated behavior is not being
    reintroduced.
  - Historical runtime-family scenarios passed.
  - Final status is `PASS` after
    `mac-command-observability-smoke-20260526-final-readyfix` passed the
    tightened command observability scenario against a running official Hytale
    session.
- Final PR summary:
  - Use this document as the PR evidence source.
  - Remaining `UNKNOWN` areas: none known after the final live smoke.
