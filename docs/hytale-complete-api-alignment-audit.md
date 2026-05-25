# HytaleCompleteAPI Alignment Audit

Updated: 2026-05-25

This compares MOTM against `/Users/viathebrink/codebases/HytaleCompleteAPI` at
commit `04cb153` (`Update Java ReadMe.md`). Treat that repository as a strong
reference and smell detector, not as the final authority. The installed Hytale
server jar, compile behavior, and live observability evidence still win when the
reference and runtime disagree.

Implementation status: the first cleanup pass is captured in
`docs/runtime-architecture-refactor-checklist.md`. Future agents should treat
this audit as historical rationale and use the current owners in `AGENTS.md`,
`CLAUDE.md`, and the README when adding new runtime behavior.

```
╔═══════════════════════╦═══════════════════════╦══════════════════════╗
║ HytaleCompleteAPI     ║ MOTM Today            ║ Improvement Pressure ║
╠═══════════════════════╬═══════════════════════╬══════════════════════╣
║ JavaPlugin lifecycle  ║ setup/start/shutdown  ║ Low                  ║
║ Event extension       ║ Real global events    ║ Medium               ║
║ World mutation safety ║ Mostly tick-context   ║ High                 ║
║ Inventory mutation    ║ Mixed direct/helpers  ║ High                 ║
║ Command permissions   ║ Dev config gate       ║ Medium               ║
║ Packet observation    ║ Read-only watchers    ║ Low                  ║
║ Runtime observability ║ Strong raw JSONL      ║ Low/Medium           ║
║ Code modularity       ║ Very large god files  ║ Very high            ║
╚═══════════════════════╩═══════════════════════╩══════════════════════╝
```

## Reference Model

`HytaleCompleteAPI` repeatedly pushes the same core patterns:

- Plugins extend `JavaPlugin`, initialize in lifecycle methods, and clean up on
  shutdown.
- Event handlers are the main extension point.
- World mutations should be executed on the world/runtime-safe path.
- Inventory changes should go through transaction-like APIs and check results.
- Commands should validate permission or authorization before protected actions.
- Event registrations should be tracked when the API returns a handle.

Those rules are visible in:

- `/Users/viathebrink/codebases/HytaleCompleteAPI/INDEX.md`
- `/Users/viathebrink/codebases/HytaleCompleteAPI/QUICK_REFERENCE.md`
- `/Users/viathebrink/codebases/HytaleCompleteAPI/PRACTICAL_MOD_PATTERNS.md`

## Scorecard

| Area | Score | Current MOTM state | Smell | Recommended direction |
| --- | ---: | --- | --- | --- |
| Plugin lifecycle | 8/10 | `MenteesMod` extends `JavaPlugin`, uses `setup()`, `start()`, and `shutdown()`. Hytale hooks are registered in `start()`. | Startup is real, but much of the runtime is still concentrated in the plugin class. | Keep lifecycle placement. Extract feature systems and harness services behind small lifecycle-owned collaborators. |
| Event registration and cleanup | 6/10 | Global player/block/input events, command, and ECS systems are registered in `registerHytaleHooks()`. Packet watchers are explicitly deregistered. | Non-packet event/system registrations are not stored or unregistered. This may be fine if the current Hytale API owns plugin cleanup, but it is not yet proven. | Verify local API return types. If registrations return handles, add a lifecycle registry and unregister in shutdown. |
| World/runtime mutation context | 5/10 | Many risky operations happen from `onServerTick()` or ECS system context, which is likely the correct runtime lane. | The invariant is implicit. Calls such as terrain placement and teleports are scattered across large classes without a single mutation boundary. | Define a `MotmRuntimeQueue` or mutation helper that documents tick-context rules, records observability, and becomes the only path for deferred world changes. |
| Inventory operations | 5/10 | Some inventory operations check transaction-like results. Others call slot removal or slot replacement directly. | The reference warns that item operations commonly require execute/check/rollback semantics. Current code may be relying on newer API behavior, but the rule is not centralized. | Add an `InventoryOps` helper after verifying current jar signatures. All inventory mutation should log attempt/result to observability. |
| Command authorization | 6/10 | Dev-only operations are gated by `MotmBuildInfo.INTERNAL_TEST_BUILD` and `dev_tools_enabled`; normal commands are in-game player only. | `MotmCommandBase` disables generated permissions and does not have a clear permission policy. Dev config gating is practical locally but coarse. | Add a small command auth policy: public player commands, dev harness commands, and future admin commands are separated and observable. |
| Packet/client observation | 8/10 | Observability packet watchers are read-only, gated to internal builds, scoped by config, and deregistered in shutdown. | High-volume packet scope could become expensive if left on `all`. | Keep default packet scope narrow. Add targeted packet probes per feature instead of broad packet firehoses. |
| Agent verification harness | 8/10 | The harness captures MOTM JSONL, client/server logs, telemetry, packets, manifests, and build/install provenance. | Scenario coverage is still only as good as the scenarios and probes we add. | Treat the harness as extensible. For each feature, add scenarios/proofs/JSONL events when current signals cannot prove success. |
| Modularity and feature ergonomics | 3/10 | `MenteesMod.java` is about 5.5k lines, `GameplayPlaybackManager.java` about 9.1k, and `MotmCommand.java` about 1.7k. | Feature changes are hard to reason about, hard to test in isolation, and likely to create hidden coupling. | Move toward feature slices: command auth/dispatch, runtime task queue, inventory ops, proof/scenario registry, ability playback families, and observability adapters. |
| API freshness discipline | 7/10 | The code compiles against the installed jar and has live Mac validation evidence. | External docs may be stale or overgeneralized. | Use HytaleCompleteAPI as a checklist, then verify against the local jar and live harness before changing code. |

## Highest-Value Refactors

```
╔════════════════════════════════════════════════════════════════════╗
║ Refactor Path                                                     ║
╠════════════════════════════════════════════════════════════════════╣
║ 1. Runtime queue + mutation context                               ║
║    └─▶ one place for deferred world/store work and observability   ║
║ 2. InventoryOps                                                   ║
║    └─▶ one place for transaction/result handling and rollback      ║
║ 3. Command auth + dev dispatch                                    ║
║    └─▶ public commands, dev harness commands, admin commands       ║
║ 4. Proof/scenario registry                                        ║
║    └─▶ features can add targeted verification without script drift ║
║ 5. Lifecycle registration registry                                ║
║    └─▶ register/unregister if current Hytale handles support it    ║
║ 6. Gameplay playback slices                                       ║
║    └─▶ split very large ability playback code by family/purpose    ║
╚════════════════════════════════════════════════════════════════════╝
```

The first three refactors matter most because they directly tighten correctness:
world mutation context, inventory semantics, and privileged commands. The fourth
matters most for agent autonomy because every new feature needs a clean way to
add evidence without turning shell scripts into bespoke one-offs.

## Harness Implications

The improved testing framework should not be frozen. It should be stable at the
contract layer and flexible at the scenario/probe layer.

Stable contracts:

- Every run has a stable `runId`.
- Raw JSONL/log/telemetry files are retained.
- Indexes and summaries point back to raw evidence.
- `manifest.json` captures world, platform, build/install provenance, and source
  files.
- Query commands can recover the evidence used for a verdict.

Flexible extension points:

- New `/motm dev` commands for targeted setup and observation.
- New proof ids for specific ability or mechanic assertions.
- New JSONL event types when existing streams are too ambiguous.
- New collector sources when Hytale writes useful data elsewhere.
- New query modes when an agent needs a tighter readout.

For feature work, prefer this loop:

```
feature intent
  └─▶ identify required proof signal
       └─▶ add/adjust code
            └─▶ add/adjust harness scenario if needed
                 └─▶ build/install
                      └─▶ run observability bundle
                           └─▶ inspect raw evidence
                                └─▶ PASS / FAIL / UNKNOWN
```

## Practical Smell List

- `MenteesMod` owns lifecycle, command inbox, runtime queues, proof handling,
  inventory grants, status HUD refreshes, observability snapshots, and many
  feature-specific dev actions. That is too much surface for one class.
- `GameplayPlaybackManager` is large enough that ability changes become search
  archaeology. Splitting by ability family or playback primitive would make
  targeted verification easier.
- Pending runtime work is represented by many independent sets/maps/queues.
  This works today, but a single runtime task queue would make ordering,
  cancellation, error reporting, and observability much clearer.
- Inventory mutation behavior should be centralized before more item or UI
  mechanics are added.
- Command authorization is acceptable for local internal testing, but it needs a
  clearer policy before expanding dev automation or admin-like features.

## Acceptance Standard For Future Improvements

A quality improvement in this area should be considered complete only when:

- The relevant HytaleCompleteAPI pattern has been checked against the local jar.
- The implementation path has a small, named owner class instead of more
  unrelated logic in `MenteesMod` or `GameplayPlaybackManager`.
- The observability harness can produce direct evidence for the behavior.
- The final run bundle includes raw evidence and a manifest.
- The agent can explain the result from evidence without relying on screenshots
  or speculation.
