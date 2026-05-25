# Agent-Driven Verification Observability

Updated: 2026-05-25

Purpose: define the architecture-level contract for a tight MOTM verification
platform where an AI agent can request a change, implement it, run Hytale, and
inspect enough trustworthy evidence to understand whether the change actually
worked.

This is not an implementation spec for exact classes, endpoints, or storage
formats. Each implementation pass should re-read the relevant references below,
verify signatures against the installed Hytale build, then choose the simplest
code shape that fits the repo.

```
╔════════════════════════════════════════════════════════════════════╗
║                         Core Principle                            ║
╠════════════════════════════════════════════════════════════════════╣
║ Bring maximal trustworthy data to the agent.                      ║
║ Preserve raw evidence.                                            ║
║ Add indexes, markers, and query paths without clipping signal.     ║
║ Let the agent perform judgment; do not build a hard-coded judge.   ║
╚════════════════════════════════════════════════════════════════════╝
```

## Reference Index

These are the files, folders, and external sources the implementation agent
should keep close. Public/community docs are leads; the local installed jar,
assets, logs, and runtime evidence win when details conflict.

```
╔══════════════════════╦══════════════════════════════════════════════╗
║ Source Class          ║ Trust Rule                                   ║
╠══════════════════════╬══════════════════════════════════════════════╣
║ repo-architecture     ║ Use for current MOTM intent and workflow      ║
║ local-authoritative   ║ Installed Hytale build, jar, assets, logs     ║
║ runtime-evidence      ║ Final authority for "did it work here"        ║
║ community-reference   ║ Useful examples; verify before coding         ║
║ official-directional  ║ Good design signal; may lag Early Access      ║
║ exploratory-tooling   ║ Useful spike candidates, not core dependency  ║
╚══════════════════════╩══════════════════════════════════════════════╝
```

### Repo Architecture References

| Reference | Why It Matters |
| --- | --- |
| `docs/hytale-capability-atlas/README.md` | Existing truth-stack framing for concept to Hytale implementation. |
| `docs/hytale-capability-atlas/source-index.md` | Current public/local source map and trust rules. |
| `docs/hytale-capability-atlas/research-completeness-audit.md` | Existing list of underweighted Hytale surfaces, proof gates, and harness gaps. |
| `docs/hytale-capability-atlas/proven-primitives.md` | Current known usable Hytale primitives. |
| `docs/hytale-capability-atlas/ability-translation-rules.md` | Rules for turning concept intent into Hytale mechanics. |
| `docs/hytale-capability-atlas/research-gates.md` | Prior proof gates that should not be bypassed. |
| `CODEX_HARNESS_EXPANSION_PLAN_2026-05-21.md` | Historical harness automation plan; useful context, but Windows-oriented. |
| `CODEX_AUTONOMOUS_IMPLEMENTATION_AND_TEST_PLAN_2026-05-21.md` | Prior autonomous loop framing and evidence layout. |
| `CODEX_AUTONOMOUS_P0_P4_PROOF_TEST_PLAN_2026-05-23.md` | Proof-plan lineage for runtime primitive validation. |
| `CODEX_CONCEPT_AWARE_TESTING_PLAN_2026-05-22.md` | Concept-aware acceptance framing. |
| `CODEX_TEST_AUDIT_OPTIMIZATION_2026-05-22.md` | Prior audit and test optimization thinking. |
| `README.md` | Public project entrypoint and currently documented scripts. |

### Repo Code And Script References

| Reference | Why It Matters |
| --- | --- |
| `src/main/java/com/motm/MenteesMod.java` | Main runtime, dev command polling, proof hooks, HUD install, player lifecycle. |
| `src/main/java/com/motm/command/MotmCommand.java` | `/motm` and `/motm dev` command surface. |
| `src/main/java/com/motm/manager/GameplayPlaybackManager.java` | Actual ability playback/runtime implementation. |
| `src/main/java/com/motm/util/HytaleAssetResolver.java` | Asset/role/particle/model resolution rules. |
| `src/main/java/com/motm/util/MotmPreflightAudit.java` | Preflight readiness and static validation surface. |
| `src/main/resources/Server/Entity/Effects/MOTM/` | Current custom effect assets and server-authored visual/status intent. |
| `gradlew` / `gradlew.bat` | Canonical Gradle entrypoint; do not require machine-specific Gradle installs. |
| `scripts/ensure-dev-environment.ps1` | Cross-platform Java 25/Hytale/Gradle wrapper bootstrap. |
| `scripts/diagnose-dev-environment.ps1` | Setup diagnostic and agent-readable next-step walkthrough. |
| `scripts/setup-agent-workstation.ps1` | Fresh-machine agent entrypoint; builds, installs, and writes setup diagnostics. |
| `scripts/build-install.ps1` | Compatibility wrapper around the cross-platform bootstrap and Gradle wrapper. |
| `scripts/send-dev-command.ps1` | Current file-backed control bridge. |
| `scripts/run-agent-observability-baseline.ps1` | Current cross-platform agent observability baseline entrypoint. |
| `scripts/run-runtime-proofs.ps1` | Existing proof orchestration to reuse conceptually. |
| `scripts/assert-ability-proof.ps1` | Current proof classifier; useful as a contrast, not the desired final judge. |
| `scripts/probe-hytale-runtime-capabilities.ps1` | Local API/capability probing precedent. |
| `scripts/discover-hytale-assets.ps1` | Local asset-indexing precedent. |
| `scripts/setup-test-world.ps1` and `scripts/load-world.ps1` | Existing world setup/load automation ideas. |

### Local Hytale Installation References

Use the current machine paths first. The observability harness is written to the
common Hytale user-data layout; on Windows that root is normally `%APPDATA%`, and
on macOS it is `~/Library/Application Support`.

| Reference | Why It Matters |
| --- | --- |
| `~/Library/Application Support/Hytale/install/release/package/game/latest/Server/HytaleServer.jar` | Local API signatures and protocol packet classes. |
| `%APPDATA%/Hytale/install/release/package/game/latest/Server/HytaleServer.jar` | Windows equivalent local API signatures and protocol packet classes. |
| `~/Library/Application Support/Hytale/install/release/package/game/latest/Assets.zip` | Local asset ids, UI files, particles, models, prefabs, effects. |
| `%APPDATA%/Hytale/install/release/package/game/latest/Assets.zip` | Windows equivalent asset ids, UI files, particles, models, prefabs, effects. |
| `~/Library/Application Support/Hytale/install/release/package/game/latest/Client/Hytale.app` | Actual Mac client bundle, Info.plist, dylibs, UI resources. |
| `%APPDATA%/Hytale/install/release/package/game/latest/Client/HytaleClient.exe` | Actual Windows client binary when installed in the standard layout. |
| `~/Library/Application Support/Hytale/UserData/Logs/` | Client logs, embedded local server stdout, launch command lines, auth/lifecycle messages. |
| `%APPDATA%/Hytale/UserData/Logs/` | Windows equivalent client logs and lifecycle messages. |
| `~/Library/Application Support/Hytale/UserData/Telemetry/` | Structured client telemetry JSONL gzip files. |
| `%APPDATA%/Hytale/UserData/Telemetry/` | Windows equivalent structured client telemetry. |
| `~/Library/Application Support/Hytale/UserData/Settings.json` | Debug toggles, input bindings, HUD/display settings. |
| `~/Library/Application Support/Hytale/UserData/Saves/Main/logs/` | Local world server logs. |
| `~/Library/Application Support/Hytale/UserData/Saves/Main/motm-data/com.motm_Mentees of the Mystical/` | MOTM runtime state, preflight report, dev command inbox/outbox. |
| `~/Library/Application Support/Hytale/UserData/Mods/mentees_of_the_mystical-1.0.1-internal.jar` | Installed artifact to compare against repo builds. |

### Adjacent Local Repos

| Reference | Why It Matters |
| --- | --- |
| `~/codebases/HytaleCompleteAPI/` | Documentation index generated from Hytale server API material; useful for discovery, verify locally. |
| `~/codebases/Hytale-Mod-Agent/.github/skills/hytale-player-input/SKILL.md` | PacketAdapters, packet watcher/filter, and input packet patterns. |
| `~/codebases/Hytale-Mod-Agent/.github/skills/hytale-hotbar-actions/SKILL.md` | Client prediction and hotbar desync patterns. |
| `~/codebases/Hytale-Mod-Agent/.github/skills/hytale-ecs/SKILL.md` | ECS/query/component mental model. |
| `~/codebases/Hytale-Mod-Agent/.github/skills/hytale-player-stats/SKILL.md` | EntityStatMap access patterns. |
| `~/codebases/Hytale-Mod-Agent/.github/skills/hytale-entity-effects/SKILL.md` | EffectControllerComponent and effect data patterns. |
| `~/codebases/Hytale-Mod-Agent/.github/skills/hytale-camera-controls/SKILL.md` | Server-driven camera command patterns. |
| `~/codebases/Hytale-Mod-Agent/.github/skills/update-server-lib/SKILL.md` | Reference-library maintenance concept; Windows scripts are not directly reusable on Mac. |

### Public And Tooling References

| Reference | Class | Why It Matters |
| --- | --- | --- |
| `https://hytale.com/news/2019/1/an-overview-of-hytales-server-technology` | official-directional | Server technology is intended as the modding foundation; client is closed/common baseline. |
| `https://support.hytale.com/hc/en-us/articles/45326769420827-Hytale-Server-Manual` | official-current | Server package layout, auth/update/protocol context. |
| `https://support.hytale.com/hc/en-us/articles/45315447521947-How-to-Find-Your-Hytale-Logs` | official-current | Confirms log locations. |
| `https://hytalemodding.dev/en/docs/established-information/client` | community-reference | Server-side modding/client limitation model; verify against local behavior. |
| `https://hytalemodding.dev/en/docs/guides/plugin/listening-to-packets` | community-reference | PacketAdapters patterns. |
| `https://hytalemodding.dev/en/docs/guides/plugin/player-input-guide` | community-reference | Client input is packet/action based, not raw keyboard state. |
| `https://hytalemodding.dev/en/docs/established-information/server/interface/ui-customization` | community-reference | Server-driven UI control flow. |
| `https://release.server.docs.hytale.com/` | official-api/community-api | Generated API browsing; local jar still wins. |
| `https://www.noesisengine.com/docs/Gui.Core.InspectorTutorial.html` | exploratory-tooling | Potential UI tree inspection concept; requires exposed/instrumented client support. |
| `https://apitrace.github.io/` | exploratory-tooling | Possible OpenGL trace/profiling spike on Mac. |
| `https://developer.apple.com/documentation/security/hardened-runtime` | exploratory-tooling | Explains why native injection/library interposition is brittle on signed Mac apps. |
| `https://frida.re/docs/examples/macos/` | exploratory-tooling | Native instrumentation option; last-resort only. |
| Minecraft GameTest, Fabric automated testing, Unreal Gauntlet, Unity PlayMode docs | external-patterns | Architecture patterns for scenario cells, runtime tests, orchestration, and evidence bundles. |

## Architecture Goals

```
╔════════════════════════════════════════════════════════════════════╗
║ Agent-Driven Verification Platform                               ║
╠════════════════════════════════════════════════════════════════════╣
║ Act       ║ Agent can change code, build, install, launch, command ║
║ Observe   ║ Agent can access raw and indexed evidence              ║
║ Correlate ║ Evidence carries run markers, timestamps, trace ids     ║
║ Query     ║ Agent can ask focused questions without losing raw data ║
║ Infer     ║ Agent performs judgment; harness does not over-decide   ║
║ Repeat    ║ Loop is fast enough to debug behavior empirically       ║
╚═══════════╩════════════════════════════════════════════════════════╝
```

### Non-Negotiable Design Rules

- Preserve raw evidence exactly where practical.
- Build indexes and query helpers as maps to raw data, not replacements for raw
  data.
- Avoid lossy summaries as the only artifact for any run.
- Record enough provenance to reconstruct a run: build id, installed jar, Hytale
  version, world, player, command, timestamps, trace ids, source files.
- Prefer server-supported APIs over client injection or auth bypass.
- Treat screenshots/video as artifact evidence for rendered appearance, not as
  the core verification substrate.
- Keep the final interpretation in the AI agent. The harness may expose facts,
  counts, timelines, and comparisons; it should not become a rigid judge layer.
- Before relying on a public claim, verify the current local jar/assets/runtime.
- On a fresh machine, run `scripts/setup-agent-workstation.ps1` before attempting
  runtime verification. If setup fails, inspect the printed
  `audits/setup-diagnostics/...` bundle and fix the concrete blocker before
  changing gameplay code.

## Evidence Planes

```
╔════════════════════════════════════════════════════════════════════╗
║ Evidence Planes                                                   ║
╠══════════════════════╦═════════════════════════════════════════════╣
║ Control              ║ How the agent asks the game to do things    ║
║ Server truth          ║ ECS, stats, effects, inventory, world state ║
║ Causality             ║ ticks, events, packets, command timelines   ║
║ Client intent         ║ UI/HUD/camera/VFX/sound commands sent       ║
║ Client-adjacent       ║ client logs, telemetry, settings, launch     ║
║ External artifacts    ║ screenshots, video, GPU traces, reports      ║
║ Reference/index       ║ jar/assets/docs/source indexes               ║
╚══════════════════════╩═════════════════════════════════════════════╝
```

### Control Plane

Goal: let the agent reliably perform experiments.

Expected capabilities:
- mark a run and scenario with a stable id
- execute MOTM/dev commands
- configure player/class/style/freecast modes
- spawn/reset test targets
- move/relocate player or scenario anchor
- trigger proof or ability actions
- clear MOTM temporary objects and test-world state

### Server Truth Plane

Goal: expose authoritative game state without requiring the agent to infer it
from logs or pixels.

Expected surfaces:
- player identity, world, position, facing, movement state
- active class/style/ability/cooldown state
- stats, health, resource-like values if present, status effects
- inventory, hotbar, active slot, spellbook/HUD state
- relevant ECS components for entities near a scenario
- active projectiles, proxies, summons, temporary blocks/fluids/selections
- cleanup registries and lingering-object checks

### Causality Plane

Goal: reconstruct what happened in order.

Expected surfaces:
- command received/executed timelines
- tick/event timelines around each run marker
- player lifecycle events
- ability cast/hit/status/cleanup/error events
- inbound packet observations for player actions
- outbound packet observations for server instructions
- correlation ids connecting agent command, server runtime, and client intent

### Client Intent Plane

Goal: avoid pixel guessing for server-authored presentation.

Expected surfaces:
- UI/HUD/page/custom UI commands emitted by MOTM
- camera packets/settings emitted by MOTM
- particle/sound/animation/model/effect presentation instructions
- entity spawn/despawn/presentation instructions relevant to a run
- player-facing messages/notifications emitted by MOTM
- correlation from each instruction to source ability/proof/scenario

This plane does not prove pixels rendered perfectly. It proves the server
authored the intended client instructions. Pixel/video/GPU artifacts are then
reserved for the narrower question: what did the client actually render?

### Client-Adjacent Evidence Plane

Goal: include the client's own exhaust without pretending it is a full client
API.

Expected surfaces:
- raw client logs copied into the run bundle
- raw Hytale telemetry JSONL gzip files copied or referenced losslessly
- parsed indexes for telemetry event boundaries and timestamps
- launch command, auth mode, Hytale client/server version, world load events
- client warnings/errors around run markers
- heartbeat metrics: FPS, frame time, network, chunks, entity count
- settings/debug-toggle state used during the run

### External Artifact Plane

Goal: retain what the semantic planes cannot prove.

Expected surfaces:
- screenshots with run id and timestamp
- optional video for animation/timing claims
- optional GPU/API traces where practical
- raw audit reports and copied logs
- a manifest that links artifacts to the relevant scenario and time window

### Reference And Index Plane

Goal: keep implementation grounded in current facts.

Expected surfaces:
- local jar class/signature indexes for high-risk packages
- local asset indexes for particles, models, UI, prefabs, effects, sounds,
  blocks, projectiles, trails, decals, camera assets
- installed build metadata and Hytale version
- repo build/install metadata
- pointer files to the reference documents listed above

## Implementation Checklist

Each item is complete only when it has raw artifacts, an index/query path, and a
small end-to-end validation run.

### Phase 0: Refresh The Reference Base

- [ ] Confirm current git state and preserve unrelated user changes.
- [ ] Record current Hytale version, installed package paths, and MOTM installed
      jar path.
- [ ] Re-run or refresh local jar/API and asset indexes where stale.
- [ ] Update `docs/hytale-capability-atlas/source-index.md` if new sources become
      part of the standard workflow.
- [ ] Record Mac-specific launcher/client/log/telemetry paths.
- [ ] Verify dev tools are enabled in the active world.

### Phase 1: Evidence Bundle Foundation

- [ ] Define a run id and scenario id convention.
- [ ] Create a per-run artifact layout that stores raw logs, raw telemetry,
      server evidence, client-intent evidence, and external artifacts.
- [ ] Write a manifest for every run with build id, installed jar, Hytale
      version, world, player, timestamps, and commands.
- [ ] Add run markers that appear in server logs, harness files, and command
      traces.
- [ ] Build indexes that point to raw file offsets/time windows without removing
      raw lines.
- [ ] Ensure the agent can request raw evidence by run id, source, timestamp,
      and correlation id.

### Phase 2: Control Plane

- [ ] Stabilize the file-backed dev command bridge on Mac.
- [ ] Add or expose a higher-throughput control surface only if the file bridge
      becomes the bottleneck.
- [ ] Support run markers, scenario setup, player mode/style/class controls,
      target setup, proof/ability triggers, and cleanup.
- [ ] Record every control request and result as raw evidence.
- [ ] Validate with a cold run from build/install through in-world command
      execution.

### Phase 3: Server Truth Plane

- [ ] Expose targeted state queries for player, ability runtime, stats/effects,
      inventory/hotbar, nearby entities, temporary objects, and cleanup state.
- [ ] Preserve raw values and component identities wherever possible.
- [ ] Include query provenance: player, world, tick/time, scenario, source.
- [ ] Validate queries before action, during action, after action, and after
      cleanup.
- [ ] Confirm the agent can inspect unexplained behavior without adding custom
      one-off logging each time.

### Phase 4: Causality Plane

- [ ] Add event/tick timeline capture around run markers.
- [ ] Add packet watcher instrumentation for inbound/outbound observations.
- [ ] Keep watchers read-only by default; filters require a specific experiment.
- [ ] Correlate command, ability runtime, packet, and state-query evidence.
- [ ] Validate with player join, ClientReady, movement/input, chat/dev command,
      and at least one ability/proof flow.

### Phase 5: Client Intent Plane

- [ ] Ledger server-authored UI/HUD/page commands.
- [ ] Ledger camera commands and settings.
- [ ] Ledger particle, sound, animation, model/effect, entity-presentation, and
      notification instructions where MOTM emits them.
- [ ] Attach trace ids to client-intent entries so the agent can connect an
      ability/proof to the emitted presentation.
- [ ] Validate with a HUD/spellbook action, an effect/particle proof, a sound or
      notification if available, and a camera/helper scenario if available.

### Phase 6: Client-Adjacent Ingestion

- [ ] Copy or reference raw client logs for each run.
- [ ] Copy or reference raw telemetry JSONL gzip files for each run.
- [ ] Index telemetry event names, sequence numbers, timestamps, and heartbeat
      blocks.
- [ ] Index client log warnings/errors and run-relevant lifecycle markers without
      discarding non-matching lines.
- [ ] Capture client settings/debug-toggle state used for the run.
- [ ] Validate that the agent can answer lifecycle questions and still inspect
      the raw logs directly.

### Phase 7: Scenario And World Harness

- [ ] Define resettable scenario cells or test arenas.
- [ ] Support stationary targets, moving targets if needed, friendly/hostile
      target categories, and cleanup.
- [ ] Record initial state, action state, final state, and cleanup state.
- [ ] Keep scenario setup programmable enough for the agent to modify on the fly.
- [ ] Validate with at least one scenario per major ability shape: self/status,
      targeted damage/effect, projectile/proxy, movement, temporary block/fluid,
      UI/HUD.

### Phase 8: Agent Query Surface

- [ ] Provide commands or scripts to list runs and sources.
- [ ] Provide commands or scripts to show raw windows around a timestamp or trace
      id.
- [ ] Provide commands or scripts to query current server truth.
- [ ] Provide commands or scripts to show packets/events/client-intent entries
      for a scenario.
- [ ] Provide commands or scripts to bundle evidence for final review.
- [ ] Verify the query surface is convenient from shell-based agent execution.

### Phase 9: Mac Launch And Baseline Loop

- [ ] Keep using the official launcher/auth flow unless a supported direct path is
      discovered.
- [ ] Preserve credential handling; do not scrape or bypass auth tokens.
- [ ] Build/install MOTM, launch Hytale, enter the active world, and run baseline
      evidence collection.
- [ ] Confirm client logs, telemetry, server logs, and MOTM outbox all line up for
      the same run.
- [ ] Document any Mac-specific setup or limitations in the run report.

### Phase 10: Exploratory Spikes

These are useful only after the core platform works.

- [ ] Headless/protocol-client feasibility spike.
- [ ] Hytale proxy/protocol relay spike.
- [ ] `apitrace` OpenGL trace spike on Mac.
- [ ] `xctrace`/Instruments performance trace spike on Mac.
- [ ] Noesis Inspector/UI-tree exposure probe.
- [ ] Accessibility/OCR/computer-vision fallback probe.
- [ ] Native instrumentation only if a recurring gap justifies the risk.

## Full-System Test Checklist

The platform is not complete until these checks pass in a clean current Hytale
run on this Mac.

### Foundation Tests

- [ ] `gradle build` succeeds with the required Java version.
- [ ] The built MOTM jar installs to the active Hytale mods folder.
- [ ] Installed jar contents match the built jar where expected.
- [ ] Active world has MOTM enabled and dev tools enabled.
- [ ] Agent observability baseline script can run without Windows-only assumptions.

### Run Bundle Tests

- [ ] A run creates a manifest with Hytale version, MOTM build, world, player,
      timestamps, and command list.
- [ ] Raw client logs are preserved or losslessly referenced.
- [ ] Raw server logs are preserved or losslessly referenced.
- [ ] Raw telemetry JSONL gzip data is preserved or losslessly referenced.
- [ ] Index files can point the agent to relevant raw time windows.
- [ ] The agent can inspect both summary/index views and raw files.

### Control Tests

- [ ] Agent sends a run marker and sees it in the evidence bundle.
- [ ] Agent sends `/motm dev position` or equivalent and receives a result.
- [ ] Agent toggles freecast/mode/style/class state and can verify the state.
- [ ] Agent spawns/reset targets and can verify target state.
- [ ] Agent clears temporary objects and can verify cleanup.

### Server Truth Tests

- [ ] Query before/after player position and facing.
- [ ] Query before/after stats/effects for a controlled status proof.
- [ ] Query before/after inventory/hotbar/spellbook state.
- [ ] Query before/during/after temporary block or proxy proof.
- [ ] Query cleanup registries and confirm no lingering objects.
- [ ] Query failure cases without crashing or hiding raw error details.

### Causality Tests

- [ ] Player connect/ready/disconnect events are captured.
- [ ] A dev command has command, tick/event, state, and result entries.
- [ ] Inbound client packets for representative actions are observed.
- [ ] Outbound server packets for representative instructions are observed.
- [ ] Packet watchers do not block normal gameplay by default.
- [ ] A single trace id connects action request, runtime behavior, packet/client
      instruction, and final state.

### Client Intent Tests

- [ ] HUD/status update instructions are logged with trace ids.
- [ ] Custom UI/page/spellbook instructions are logged with trace ids.
- [ ] Effect/particle/model/sound instructions are logged with trace ids where
      available.
- [ ] Camera instructions are logged if used.
- [ ] The agent can answer "did the server instruct the client to show X?" without
      using screenshots.

### Client-Adjacent Tests

- [ ] Telemetry shows session start, state transitions, server connect, world
      joined, heartbeat, and session end for a normal run.
- [ ] Client log indexing finds launch command, telemetry file path, auth/lifecycle
      status, warnings, and errors.
- [ ] Client settings/debug toggles used for the run are recorded.
- [ ] A deliberate client/server warning remains visible in raw evidence and is
      not filtered away.

### Scenario Tests

- [ ] Self/status scenario completes with state and client-intent evidence.
- [ ] Targeted damage/effect scenario completes with target-side evidence.
- [ ] Projectile/proxy scenario completes with spawn/action/cleanup evidence.
- [ ] Movement scenario completes with before/after displacement evidence.
- [ ] Temporary block/fluid scenario completes with placement and restoration
      evidence.
- [ ] UI/HUD scenario completes with server intent plus optional visual artifact.

### Close-The-Loop Test

- [ ] Start from a requested behavior change.
- [ ] Modify code/data.
- [ ] Build and install.
- [ ] Launch or reuse a clean Hytale run.
- [ ] Execute a controlled scenario.
- [ ] Capture raw and indexed evidence across all relevant planes.
- [ ] Let the agent inspect evidence and identify pass/fail/unknown with reasons.
- [ ] If unknown, add only the missing observability and rerun.
- [ ] Finish with a run bundle that a second agent or human can audit.

## Current Implementation Status

This section records the concrete state proven on this Mac without turning the
architecture checklist above into a brittle implementation prescription.

```
╔══════════════════════╦═════════════════════════════════════════════╗
║ Area                 ║ Status                                      ║
╠══════════════════════╬═════════════════════════════════════════════╣
║ Control              ║ Implemented through `/motm dev observe`      ║
║ Server truth          ║ Implemented through JSONL snapshots/events   ║
║ Causality             ║ Implemented through trace-linked timelines   ║
║ Client intent         ║ Implemented for current MOTM emissions       ║
║ Packets               ║ Implemented read-only inbound/outbound watch ║
║ Client-adjacent       ║ Implemented log/telemetry/settings capture   ║
║ Query surface         ║ Implemented through shell-friendly scripts   ║
║ Local baseline loop   ║ Implemented and Mac-validated through launcher║
║ Client internals      ║ Not claimed; use artifacts for final render  ║
╚══════════════════════╩═════════════════════════════════════════════╝
```

Implemented surfaces:

- `MotmObservability` writes per-run raw JSONL evidence for control,
  causality, server truth, client intent, packet, and external-artifact planes.
- `/motm dev observe` starts/stops runs, marks scenarios, snapshots server
  truth, opens the MOTM spellbook page, and records control events.
- Runtime trace contexts link dev commands, proof execution, ability playback,
  delayed projectile/field processing, client-intent records, and packet watcher
  records where the server emission occurs inside an active trace.
- Evidence collection preserves raw client logs, server logs, telemetry JSONL or
  JSONL gzip files, settings, MOTM runtime evidence, manifests, and indexes.
- Query scripts expose summaries, source lists, event lookup, and raw windows
  while leaving the raw evidence intact.
- The agent observability baseline runner can build/install, drive world commands, collect a run
  bundle, and assert the core close-the-loop invariants.

Validation runs:

| Run | What It Proved |
| --- | --- |
| `full-observability-20260524-234537` | End-to-end baseline across build/install reuse, ability cast, HUD intent, proof scenarios, movement proof, packet capture, logs, telemetry, manifest, and indexes. |
| `input-packet-probe-20260524-234849` | Read-only inbound packet observation for live player action, including interaction and movement packets. |
| `page-intent-20260524-235446` | Custom spellbook page intent plus outbound `CustomPage` packet under the same command trace, with inbound `CustomPageEvent` response preserved. |
| `movement-fixed-20260524-232456` | Movement proof changed from speculative success to measured displacement and destination error. |
| `live-native-fixed-20260524-231433` | Native Hytale entity effects are visible in proof events and server-truth snapshots. |

Current boundaries:

- Windows uses the same in-mod observability streams and Hytale user-data layout,
  and the scripts are path-safe PowerShell. This pass hardened the scripts for
  Windows execution, but the final live validation run was performed on macOS.
- Camera and sound/notification ledgers remain architectural hooks; this pass did
  not find current MOTM code paths that emit dedicated camera or sound commands
  to validate. Add those ledgers when MOTM starts emitting those instructions.
- Packet evidence is intentionally raw. Packets emitted inside MOTM trace
  contexts carry trace ids; background protocol traffic is preserved without
  inventing causality.
- Screenshots and optional video remain external artifacts, not the primary
  verification substrate.
- Phase 10 exploratory spikes are follow-up research options after the core
  platform. They are not required for the current server-supported verification
  loop unless a recurring gap justifies them.

## Completion Definition

The platform is complete when a future agent can perform this loop without
manual interpretation from the user:

```
Request
  └─▶ inspect references
       └─▶ implement change
            └─▶ build/install
                 └─▶ run scenario
                      └─▶ gather lossless evidence
                           └─▶ query raw/indexed facts
                                └─▶ explain result
                                     └─▶ patch or conclude
```

The result does not need perfect client internals. It does need enough raw,
trustworthy, correlated evidence that speculation stops being the default
debugging mode.
