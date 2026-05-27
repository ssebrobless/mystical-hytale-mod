# Agent-Friendly Architecture Scaffolding

Updated: 2026-05-26

This document captures the architecture review that followed the runtime
refactor and Hybrid/HytaleCompleteAPI reference passes. It is the implementation
record for the next cleanup layer: making MOTM easier for agents to extend
without drifting into one-off branches, hidden data/code mismatches, or
evidence-light feature claims.

The goal is not smaller files for their own sake. The goal is an architecture
where an agent can quickly answer:

```
╔════════════════════════════════════════════════════════════════════╗
║ Agent Feature Test                                                ║
╠════════════════════════════════════════════════════════════════════╣
║ Where do I add this behavior?                                     ║
║ What contract must I preserve?                                    ║
║ What evidence proves it works?                                    ║
║ What automated guard catches the most likely bad patch?            ║
╚════════════════════════════════════════════════════════════════════╝
```

## Current Debt Shape

```
╔════════════════════════════════════════════════════════════════════╗
║ Data JSON                                                         ║
║   effect / cast_type / target_type / terrain_effect / travel_type ║
╚═══════════════════════════════╦════════════════════════════════════╝
                                ▼
╔════════════════════════════════════════════════════════════════════╗
║ AbilityData                                                       ║
║   flat optional fields, mostly stringly typed                     ║
╚═══════════════════════════════╦════════════════════════════════════╝
                                ▼
╔════════════════════════════════════════════════════════════════════╗
║ GameplayPlaybackManager                                          ║
║   projectiles, fields, terrain, summons, transformations,         ║
║   follow-ups, visual proxies, active state, cleanup, evidence     ║
╚═══════════════════════════════╦════════════════════════════════════╝
                                ▼
╔════════════════════════════════════════════════════════════════════╗
║ MenteesMod                                                        ║
║   lifecycle, runtime loop, proof hooks, style-test targets,        ║
║   HUD, observability, config, player runtime facade               ║
╚════════════════════════════════════════════════════════════════════╝
```

The harness is now strong enough to support agent-driven verification, but the
Java extension surface still nudges agents toward local patches in large
classes. The Magma Sling origin branch was only one example. The broader smell
is that many feature changes must be discovered by search archaeology instead
of by following an explicit feature rail.

## Resume Strategy

This branch should continue as one cohesive migration PR. Do not commit, push,
or open the final PR merely because a safe subset builds. The goal is to close
the migration checklist, then commit the completed architecture with its
evidence bundle summary.

```
╔════════════════════════════════════════════════════════════════════╗
║ Remaining Work Strategy                                           ║
╠════════════════════════════════════════════════════════════════════╣
║ freeze checklist as source of truth                               ║
║   └─▶ preserve completed plugin-shell facade                      ║
║        └─▶ prove GameplayPlaybackManager facade with live evidence║
║             └─▶ run CLI-only harness scenarios                    ║
║                  └─▶ write final evidence summary                  ║
╚════════════════════════════════════════════════════════════════════╝
```

Most vertical runtime-family migrations have now landed. The remaining
`GameplayPlaybackManager` question is not whether more helper extraction is
possible; it is whether the retained facade surface has enough live harness
evidence to be trusted as a compatibility boundary while future features move
through the named runtime-family owners. If fresh inspection finds policy,
state, target selection, damage/effect routing, world mutation, or
ability-specific decisions still living in the manager, migrate that behavior
behind the appropriate owner and add a ratchet. If the manager is truly a
compatibility facade, prove it through scenarios and record the evidence.

## Resume Run Contract

When this goal-driven run resumes, treat this document and
`docs/runtime-architecture-refactor-checklist.md` as the working contract. The
right next action is not to re-plan from scratch; it is to inspect the unchecked
items, verify the current branch still passes the rails, and continue the
vertical migration sequence until the checklist is actually closed.

```
╔════════════════════════════════════════════════════════════════════╗
║ Resume Protocol                                                   ║
╠════════════════════════════════════════════════════════════════════╣
║ read unchecked checklist items                                    ║
║   └─▶ run architecture/static rails                               ║
║        └─▶ finish next vertical runtime-family slice              ║
║             └─▶ add or tighten ratchets for removed old paths     ║
║                  └─▶ run focused tests + build + harness evidence ║
║                       └─▶ repeat until no real migration remains  ║
╚════════════════════════════════════════════════════════════════════╝
```

Current operating decisions:

- Keep the work in one cohesive migration PR. Do not commit, push, or open the
  PR until the migration has been fully implemented, checked, and summarized.
- Continue from the current branch state. If `main` changed, integrate it
  deliberately, preserve this branch's architecture decisions, and re-run the
  rails before continuing.
- Prefer fresh-slate replacement over legacy accommodation. When a new runtime
  owner covers behavior, migrate callers and delete the old internal path in
  the same change unless an explicit compatibility-register entry proves a real
  external consumer.
- Keep `MenteesMod` and `GameplayPlaybackManager` as temporary facades only.
  Any retained method containing policy, sequencing, state ownership, target
  selection, damage/effect routing, world mutation, or ability-specific
  decisions is still architecture debt.
- Treat the `MenteesMod` plugin shell modularity as currently complete unless
  fresh inspection proves a regression. The remaining open facade concern is
  `GameplayPlaybackManager` harness proof and any manager-local behavior found
  during that proof pass.
- The harness is part of the product surface for agents. Agents may add
  scenarios, probes, diagnostics, or script support when a feature cannot be
  verified tightly enough, provided the raw evidence contract and trust model
  stay intact.
- Verification must be CLI-first on macOS and Windows. Do not design a required
  path around Computer Use, screenshot-only judgment, launcher clicking, or
  local GUI automation. Official launcher/session state may be required, but
  readiness and failure diagnostics must be inspectable from the CLI.
- Authentication must not be faked, scraped, or bypassed. The harness should
  detect whether the official session appears usable, fail with a precise
  diagnostic when it is not, and rerun cleanly after a human completes official
  launcher setup.
- A remote Windows agent may not be able to drive the Hytale launcher UI at all.
  Treat launcher interaction as a human prerequisite and CLI diagnostics as the
  agent responsibility: setup/preflight commands should prove what is installed,
  what session signal is missing, and what exact rerunnable command comes next.

## Resume Execution Plan

The most elegant continuation is a deletion-driven migration, not another
planning pass. On resume, the agent should use the current branch as the
source of truth, refresh the rails, and then work only on the remaining proof
gaps. `MenteesMod` should already be a boring facade; `GameplayPlaybackManager`
must either be proven as a temporary facade or reduced further if inspection
finds live behavior that still belongs behind a named runtime owner.

```
╔════════════════════════════════════════════════════════════════════╗
║ Resume Execution                                                  ║
╠════════════════════════════════════════════════════════════════════╣
║ sync with current branch state                                    ║
║   └─▶ refresh architecture/build/static rails                     ║
║        └─▶ inspect only unchecked completion items                ║
║             └─▶ rerun live harness after Hytale is running        ║
║                  └─▶ write final PASS/FAIL/UNKNOWN summary        ║
║                       └─▶ repeat until checklist is closed        ║
╚════════════════════════════════════════════════════════════════════╝
```

Order of attack for the resumed goal:

- Refresh the rails, then inspect the unchecked items rather than re-planning
  completed migrations.
- Treat processor/harness assertion depth as satisfied by focused lifecycle
  tests plus live-runner checks that require `runtime_task_executed` evidence
  for scenario-driven style-test mob spawn/count/clear commands. Extend this
  only if the next live run exposes a missing task family.
- Re-run the command observability harness after Hytale is running through the
  official launcher and world `Main` is loaded. If official launcher/session
  state or world readiness is missing, the script should produce a precise
  diagnostic bundle and stop with `UNKNOWN`; it should not require GUI control,
  screenshot inference, local Computer Use, token scraping, or any credential
  workaround.
- Inspect `GameplayPlaybackManager` during the harness-proof pass. If it still
  owns behavior, migrate/delete that owner and add a ratchet. If it is only a
  compatibility facade, keep it temporarily and name the evidence that proves
  the retained path is safe.
- Keep the harness malleable. If a feature or migration cannot be proven with
  the existing scenarios/probes, add the missing scenario, command, diagnostic,
  or evidence query as part of the implementation while preserving raw
  provenance-rich output.
- End only after final static rails, focused tests, build/install, and the
  relevant harness scenarios have run or have explicit `UNKNOWN` evidence with
  a named blocker.

Current continuation anchor:

```
╔════════════════════════════════════════════════════════════════════╗
║ Last Verified Rails In This Branch                                ║
╠════════════════════════════════════════════════════════════════════╣
║ check-architecture.ps1                                            ║
║ validate-content-shape.ps1                                        ║
║ run-agent-observability-baseline.ps1 parser check                  ║
║ gradlew test --warning-mode all                                   ║
║ gradlew compileJava --warning-mode all                            ║
║ gradlew build --warning-mode all                                  ║
║ gradlew -Pmotm_build_channel=internal installMod --warning-mode all║
║ git diff --check, with only known CRLF warnings in two .ps1 files  ║
╚════════════════════════════════════════════════════════════════════╝
```

The branch is not complete merely because these rails passed in the latest
known checkpoint. They prove that the extracted owners and harness assertions
were coherent enough to continue from. The next run should refresh these rails
first, then finish the remaining live `GameplayPlaybackManager` facade proof
and final evidence summary before any commit, push, or PR work.

The older green checkpoint also included proof/spellbook coverage. That broader
live rail was refreshed after the player-ready lifecycle fix:
`mac-baseline-proof-spellbook-20260526-final-readyfix` passed scenario
`baseline` with proof id `coating-metal` and expected `proof_end` plus
`CustomHud` evidence.

Latest live harness attempt:

```
╔════════════════════════════════════════════════════════════════════╗
║ Live Harness Checkpoint                                           ║
╠════════════════════════════════════════════════════════════════════╣
║ run id: mac-command-observability-smoke-20260526-final-readyfix    ║
║ static/internal-jar checks: PASS                                   ║
║ command observability scenario: PASS                              ║
║ live bridge + player-ready lifecycle + task evidence: PASS         ║
║ evidence: run-local baseline-report.md and raw MOTM streams        ║
╚════════════════════════════════════════════════════════════════════╝
```

The final live blocker is resolved. The first post-world run
`mac-command-observability-smoke-20260526-final` proved the command bridge was
alive but exposed a real lifecycle regression: dev commands returned
`[MOTM] Error: Player data not found.` because Hytale's stable player identity
arrived at `onPlayerReady`, not the earlier `onPlayerConnect` callback.
`PlayerSessionLifecycleActions` now backfills player-data initialization and
saved-loadout rebuild from `onPlayerReady`; the corrected internal tester jar
was installed, Hytale was restarted through the official launcher, world `Main`
was loaded, and `mac-command-observability-smoke-20260526-final-readyfix`
passed.
On Windows, a remote agent should not attempt to drive the launcher UI; it
should run the CLI preflight/diagnostic path, report the exact missing
official-session signal, and wait for the human launcher prerequisite to be
satisfied.

Resume handoff:

```
╔════════════════════════════════════════════════════════════════════╗
║ Resume Handoff                                                    ║
╠════════════════════════════════════════════════════════════════════╣
║ Migration checklist is closed by current evidence.                 ║
║ Do not reopen migrated proof/style/Terra/free-cast/dev slices.    ║
║ Refresh rails before commit/PR.                                    ║
║ Treat facades as temporary compatibility only.                     ║
║ Keep launcher/auth handling official, CLI-diagnosed, rerunnable.   ║
╚════════════════════════════════════════════════════════════════════╝
```

Immediate pre-PR checklist:

- Confirm no unchecked items remain with
  `rg -n "\[ \]" docs/agent-friendly-architecture-scaffolding.md`.
- Run the final static/test/build rails before commit or PR.
- Keep `mac-command-observability-smoke-20260526-final-readyfix` as the current
  PR-facing live proof unless code changes invalidate the installed jar.
- For any fresh live rerun, install the internal tester jar first, launch Hytale
  through the official launcher, enter world `Main`, and run the command
  observability scenario without relying on GUI automation as a required harness
  step.
- If a future bridge run fails, keep the result `UNKNOWN` or `FAIL` as
  appropriate, inspect the run-local diagnostic JSON/Markdown, and improve the
  CLI readiness diagnostics rather than inferring from screenshots.

Current migration snapshot:

```
╔════════════════════════════════════════════════════════════════════╗
║ Completed Vertical Slices                                         ║
╠════════════════════════════════════════════════════════════════════╣
║ plugin shell/task queue/inventory/config/proof/scenario rails      ║
║ server-tick runtime loop behind a named runtime owner              ║
║ projectile runtime + concrete Hytale launch/hit/tick/impact        ║
║ field target/visual/pulse/support/sinkhole/terrain/mobility        ║
║ field activation execution behind a Hytale adapter                 ║
║ terrain tick/restoration hooks behind a Hytale adapter             ║
║ terrain placement/world mutation behind a Hytale adapter           ║
║ terrain ability-specific routing behind a Hytale adapter           ║
║ Lapidary gem proxy lifecycle behind a Hytale adapter               ║
║ sinkhole terrain marker behind a Hytale adapter                    ║
║ supplemental trail/aura activation behind a Hytale adapter         ║
║ summon spawn/register/despawn lifecycle behind a Hytale adapter    ║
║ summon buff/tick/move/target control behind a Hytale adapter       ║
║ summon damage/effect/splash mutation behind a Hytale adapter       ║
║ follow-up primary/native/visual/splash mutation behind adapter     ║
║ transformation activation/tick/pulse/weapon mutation behind adapter║
║ self-effect/anchor mutation behind a Hytale adapter                ║
║ channel/line-control pulse mutation behind a Hytale adapter        ║
║ one-off proof/coating routing behind an explicit adapter           ║
║ generic ability execution policy behind a profile edge             ║
║ runtime effect-id resolver policy behind ability utility           ║
║ status-effect token construction behind ability utility            ║
║ pure ability runtime math policy behind ability utility            ║
║ Hydro water-source refill behind a resource handler                ║
║ block-damage Alloy tool-use routing behind interaction handler     ║
║ spellbook input routing behind interaction handler                 ║
║ spellbook custom-page opening behind UI action owner               ║
║ proof cleanup iteration behind proof processor                     ║
║ proof world/effect actions behind proof action owner               ║
║ style-test sequence loop behind runtime task processor             ║
║ style-test command policy behind action owner                      ║
║ Terra review inventory kit behind resource owner                   ║
║ inventory command queue policy behind action owner                 ║
║ free-cast safety mutation behind runtime processor                 ║
║ dev runtime command queue policy behind action owner               ║
║ dev player relocation/game-mode mutation behind action owner       ║
║ style-test mob/review arena actions behind action owner            ║
║ observability snapshot evidence-shape behind snapshot builder      ║
║ observability run control behind action owner                      ║
║ observability event/trace policy behind event owner                ║
║ progression stat mutation and world-average policy behind owner    ║
║ perk trigger registration/effects behind player runtime owner      ║
║ player runtime rebuild sequencing behind player runtime owner      ║
║ live mob-spawn scaling behind player runtime owner                 ║
║ combat lifecycle kill/death handling behind player runtime owner   ║
║ player session join/connect/ready/disconnect behind owner          ║
║ runtime-player lookup/geometry behind a read-only view             ║
║ spellbook/dev-book delivery behind inventory kit owner             ║
║ runtime player handles, HUD handles, progression runtime state      ║
║ custom HUD install/refresh behavior behind UI action owner          ║
║ summon/transform/follow-up pure runtime ownership                  ║
╚════════════════════════════════════════════════════════════════════╝
                                │
                                ▼
╔════════════════════════════════════════════════════════════════════╗
║ Migration Closure                                                 ║
╠════════════════════════════════════════════════════════════════════╣
║ GameplayPlaybackManager facade is harness-proven                   ║
║ live command observability passed after official Hytale launch     ║
║ final PR evidence summary names PASS/FAIL/UNKNOWN areas            ║
╚════════════════════════════════════════════════════════════════════╝
```

The next resume should start by refreshing the latest green rails and checking
whether code changed after the final evidence run. Do not treat proof actions,
style-test sequencing, Terra review inventory, free-cast safety, dev player
mutation, style-test mob/review arena actions, server tick looping, session
lifecycle handling, combat lifecycle handling, mob-spawn scaling, command
authorization, dev command inbox processing, or observability event/trace
handling as open frontier unless fresh code inspection proves a regression.
The closed frontier is now:

- `GameplayPlaybackManager` facade proof passed in
  `mac-command-observability-smoke-20260526-final-readyfix`. If future
  inspection finds generic combat/support/targeting callbacks that still contain
  timing, target policy, damage ordering, effect routing, world mutation, or
  ability-specific decisions, migrate the behavior behind a named owner and add
  a ratchet; do not add new behavior to the facade.
- Processor/harness assertion depth is now covered by local lifecycle tests,
  scenario `expectedEvidence` shape validation, and runner checks that require
  task-type-specific `runtime_task_executed` evidence for scenario-driven
  style-test mob spawn/count/clear commands.
- Pure ability runtime math now lives in `AbilityRuntimeMath`, including
  movement distance, vertical displacement, range, pulse damage, pull step,
  pull lift, damage amount, and target-sequence damage multiplier policy. Do
  not reintroduce those private resolver methods in `GameplayPlaybackManager`.
- Supplemental non-persistent terrain trail/aura activation now lives in
  `TerrainSupplementalHytaleAdapter`, including trail/aura selection, field
  visual spawn handoff, field runtime registration, surface cues, and summary
  formatting. Do not reintroduce `activateSupplementalTerrainRuntime` or its
  result record inside `GameplayPlaybackManager`.
- Remaining manager-local ability-id branches or duplicate behavior paths after
  descriptor/spec/runtime parity. Generic execution policy such as caster visual
  suppression, support-token filtering, cast-family classification,
  ground/anchor policy, direct damage cause, and special-damage selection now
  lives in `AbilityExecutionPolicy`; do not recreate it in the manager.
- Windows/macOS CLI setup, official-session preflight, install, and diagnostics
  hardening remains in scope only insofar as the live run exposes gaps. A
  remote Windows agent must be able to self-diagnose from PowerShell without
  GUI control, token scraping, or auth bypass.

Use this command set as the initial sanity check before continuing:

```
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/check-architecture.ps1
pwsh -NoProfile -ExecutionPolicy Bypass -File scripts/validate-content-shape.ps1
./gradlew test --warning-mode all
./gradlew compileJava --warning-mode all
./gradlew test --tests 'com.motm.observability.MotmObservabilityActionsTest' --tests 'com.motm.runtime.player.PlayerRuntimeRebuildActionsTest' --tests 'com.motm.runtime.player.PerkTriggerRuntimeActionsTest' --tests 'com.motm.runtime.ability.AbilityRuntimeMathTest' --warning-mode all
./gradlew test --tests 'com.motm.proof.*' --tests 'com.motm.runtime.state.SpellbookInputDebouncerTest' --tests 'com.motm.runtime.task.StyleTestSequenceRuntimeTaskProcessorTest'
./gradlew test --tests 'com.motm.runtime.task.RuntimeTaskProcessorLifecycleTest'
./gradlew build --warning-mode all
git diff --check
./gradlew -Pmotm_build_channel=internal -Pmotm_internal_test_build=true installMod
```

After install, verify the installed jar is an internal tester build before
trusting harness output. If a live Hytale run is blocked by launcher/session
state, record `UNKNOWN` with the CLI diagnostic bundle rather than substituting
visual speculation.

Preferred continuation order:

- Treat summon concrete mutation as migrated. Spawn, active summon
  registration, raw base-damage calculation, spawn-position lookup, model
  resolution, owner cleanup, and despawn now live in
  `SummonLifecycleHytaleAdapter`; buff routing, tick hook wiring, NPC movement,
  target acquisition, awaken visuals, and clone repositioning now live in
  `SummonControlHytaleAdapter`; damage, impact effects, attack-effect tokens,
  shields, pulls, splash target iteration, and splash damage now live in
  `SummonAttackHytaleAdapter`.
- Treat terrain concrete mutation as migrated. Terrain placement, block/fluid
  asset resolution, `BlockSelection` construction, restore-before-place,
  moving-trail registration, stacking-column registration, persistent field
  terrain placement, and Iron Wall overlap-push mutation now live in
  `TerrainPlacementHytaleAdapter`. Terra ability-specific terrain routing now
  lives in `TerrainAbilityHytaleAdapter`; Lapidary gem proxy spawn, label, tick,
  despawn, and anchor lookup now live in `TerrainGemHytaleAdapter`. Sinkhole
  crack/dust terrain marker orchestration now lives in
  `TerrainSinkholeMarkerHytaleAdapter`. Supplemental non-persistent trail/aura
  activation, surface cue placement, field visual handoff, field runtime
  registration, and summary formatting now live in
  `TerrainSupplementalHytaleAdapter`. Refresh live terrain scenario evidence
  after the final migration pass, but do not re-open a manager-local terrain
  implementation owner.
- Treat follow-up concrete mutation as migrated. Primary hit damage/effect
  mutation, payoff shields/healing/splash, native Alloy damage/tool-use hooks,
  Alloy held-item visuals, and follow-up splash mutation now live in
  `WeaponFollowUpHytaleAdapter`.
- Treat transformation concrete mutation as migrated. Activation, owner refresh,
  locomotion pressure, form pulse damage/effects, charge impacts, weapon riders,
  and transformation cleave now live in `TransformationHytaleAdapter`.
- Treat self concrete mutation as migrated. Active self-effect ticking,
  completion effects, player-anchor enforcement, movement freeze, and velocity
  zeroing now live in `SelfHytaleAdapter`.
- Treat channel concrete mutation as migrated. Channel and line-control
  activation, tick iteration, pulse effects, lifesteal/life-drain/healing,
  line-control pulls, and repeat target-token application now live in
  `ChannelHytaleAdapter`.
- Treat spellbook input routing as migrated. Interact/mouse/custom-interaction
  slot routing, spellbook/dev-book gesture policy, duplicate debounce ownership,
  Hydro refill handoff, weapon follow-up hit handoff, and player messaging now
  live in `SpellbookInputHandler`.
- Treat proof cleanup iteration as migrated. Temporary selection restoration,
  proxy despawn, store/world filtering, cleanup removals, and diagnostic logging
  now live in `MotmProofCleanupProcessor`.
- Treat proof world/effect actions as migrated. Effect application, target
  effect lookup, temporary block/fluid placement, proxy spawn, movement proof
  teleport, cleanup-state registration, and proof evidence emission now live in
  `MotmProofActions`.
- Treat live style-test sequencing as migrated. Active test advancement,
  nearest NPC target resolution, target-block derivation, ability cast queueing,
  completion/missing-player cleanup, and server-truth events now live in
  `StyleTestSequenceRuntimeTaskProcessor` and `StyleTestTargetResolver`.
- Treat Terra review inventory policy as migrated. Review-kit contents,
  essential-item retention, item asset checks, review-item backfill, grant/remove
  loops, and summaries now live in `TerraReviewInventoryKit`.
- Treat free-cast safety as migrated. Health-drop diagnostics, health/stamina/
  mana/signature refill, burn/dot clearing, movement normalization/reset, and
  native invulnerability attach/remove now live in `FreeCastSafetyProcessor`.
- Treat dev player test mutation as migrated. Relocation destination policy,
  flatlands/lane platform placement, teleport execution, and game-mode mutation
  now live in `DevPlayerTestActions`; the plugin shell only queues commands and
  wires the task processor.
- Treat style-test mob and review arena world actions as migrated. Spawn layout
  policy, NPC spawn/despawn, tracked-target counting, nearby cleanup-role
  scanning, and arena scrub block/fluid mutation now live in
  `StyleTestMobActions`.
- Remove migrated ability-id branches only after the matching runtime spec or
  profile proves parity. Do not leave old and new behavior owners active just
  because both currently pass tests.
- Treat generic execution policy as migrated. Caster visual suppression,
  movement/line/multi-target cast family classification, caster/target token
  filtering, Dominate extra target riders, Alloy caster-token exclusion,
  grounded restriction, anchor drag, direct damage cause, and special damage
  policy now live in `AbilityExecutionPolicy`; do not recreate those branches
  inside `GameplayPlaybackManager`.
- End with facade cleanup: `MenteesMod` and `GameplayPlaybackManager` should
  retain only construction, delegation, and public compatibility surfaces that
  are explicitly registered.

Near-term resume slice:

```
╔════════════════════════════════════════════════════════════════════╗
║ Next Slice: Finish Facade Modularity And Harness Readiness         ║
╠════════════════════════════════════════════════════════════════════╣
║ refresh current rails                                              ║
║   └─▶ inspect residual manager/plugin behavior owners              ║
║        └─▶ extract next policy owner or delete duplicate old path   ║
║             └─▶ add/adjust ratchet for that exact smell             ║
║                  └─▶ run focused tests, build, harness/auth checks  ║
╚════════════════════════════════════════════════════════════════════╝
```

Do not treat this as a prescription for exact helper names. Re-read the
residual manager/plugin methods before implementing. The important contract is
that the runtime family owns sequencing and domain policy, the Hytale adapter
owns concrete store/world/effect calls, and the manager/plugin shells keep only
temporary wiring or registered compatibility methods.

The intended pattern is callback inversion with explicit Hytale adapters, not a
generic service layer:

```
╔════════════════════════════════════════════════════════════════════╗
║ Runtime Family                                                    ║
║   owns sequencing, profile use, state transitions, summaries       ║
╚═══════════════════════════════╦════════════════════════════════════╝
                                ▼
╔════════════════════════════════════════════════════════════════════╗
║ Narrow Hytale Adapter                                             ║
║   resolve origin / collect targets / mutate world / visuals        ║
╚═══════════════════════════════╦════════════════════════════════════╝
                                ▼
╔════════════════════════════════════════════════════════════════════╗
║ GameplayPlaybackManager                                           ║
║   temporary wiring facade only                                    ║
╚════════════════════════════════════════════════════════════════════╝
```

`GameplayPlaybackManager` should become boring wiring. If a retained callback
contains timing, target policy, damage ordering, effect routing, state mutation,
or ability-specific decisions, it is still an implementation owner and should
move into the runtime family before the checklist item is marked done.

Current runtime frontier:

```
╔════════════════════════════════════════════════════════════════════╗
║ Already Migrated                                                  ║
╠════════════════════════════════════════════════════════════════════╣
║ TerrainRuntimeSpecs: classification, geometry, timing constants    ║
║ TerrainRuntimeState: selections, trails, columns, owner cleanup    ║
║ TerrainActivationRuntime: trail/gem/column construction            ║
║ TerrainTickRuntime: tick ordering and restoration decisions        ║
║ TerrainHytaleAdapter: tick/restoration hooks and block staging     ║
║ TerrainPlacementHytaleAdapter: block/fluid placement + evidence    ║
║ TerrainAbilityHytaleAdapter: Terra ability terrain routing         ║
║ TerrainGemHytaleAdapter: Lapidary proxy lifecycle + anchor lookup  ║
║ TerrainSinkholeMarkerHytaleAdapter: crack/dust marker placement    ║
║ TerrainSupplementalHytaleAdapter: supplemental trail/aura fields   ║
║ SummonLifecycleHytaleAdapter: spawn/register/despawn lifecycle      ║
║ SummonControlHytaleAdapter: buff/tick/move/target control           ║
║ SummonAttackHytaleAdapter: damage/effect/splash mutation            ║
║ WeaponFollowUpHytaleAdapter: primary/native/visual/splash mutation  ║
║ TransformationHytaleAdapter: activation/tick/pulse/weapon mutation  ║
║ SelfHytaleAdapter: self-effect/anchor mutation                      ║
║ ChannelHytaleAdapter: channel/line-control pulse mutation           ║
║ AbilitySpecificHytaleAdapter: one-off proof/coating routing         ║
║ AbilityExecutionPolicy: generic cast/token/special-damage policy    ║
║ AbilityRuntimeEffects: runtime effect-id resolver policy            ║
║ AbilityStatusEffects: token-to-status construction policy           ║
║ AbilityRuntimeMath: pure movement/damage/range/pull math policy     ║
║ HydroContainerRefillHandler: water-source refill behavior           ║
║ BlockDamageInteractionHandler: Terra block damage + Alloy tool use  ║
║ SpellbookInputHandler: interact/mouse/custom slot routing           ║
║ SpellbookPageActions: custom page open + client intent              ║
║ MotmProofCleanupProcessor: temporary proof selection/proxy cleanup  ║
║ MotmProofActions: proof world/effect mutation + evidence emission   ║
║ MotmObservabilitySnapshotBuilder: raw snapshot evidence shape       ║
║ MotmObservabilityActions: observability run control commands        ║
║ MotmObservabilityEvents: observability event/trace policy           ║
║ PlayerProgressionRuntimeActions: progression stat/world policy      ║
║ PerkTriggerRuntimeActions: perk trigger registration/effects         ║
║ PlayerRuntimeRebuildActions: player runtime rebuild sequencing      ║
║ MobSpawnRuntimeActions: live mob-spawn scaling policy               ║
║ PlayerCombatLifecycleActions: mob-kill/player-death side effects    ║
║ PlayerSessionLifecycleActions: session join/connect/ready/disconnect║
║ RuntimePlayerView: live player lookup/store/geometry readback       ║
║ SpellbookInventoryKit: spellbook/dev-book grant + migration         ║
║ MotmStatusHudActions: custom HUD install/refresh/native hide intent ║
║ MotmRuntimeLoop: server-tick sequencing + heartbeat/HUD cadence     ║
║ MotmDevCommandInboxProcessor: dev inbox runtime-player/trace adapter║
║ AbilityCastCommandActions: command-facing ability-cast queue policy ║
║ FreeCastCommandActions: command-facing free-cast state access       ║
║ StyleTestSequenceRuntimeTaskProcessor: live style-test sequencing   ║
║ StyleTestCommandActions: dev style-test command policy              ║
║ StyleTestTargetResolver: nearest NPC/target-block lookup            ║
║ TerraReviewInventoryKit: review-kit grant/cleanup inventory policy  ║
║ InventoryCommandActions: inventory task queue policy                ║
║ FreeCastSafetyProcessor: free-cast safety/refill/invulnerability    ║
║ DevRuntimeCommandActions: dev runtime command queue policy          ║
║ DevPlayerTestActions: test relocation/platform/game-mode mutation   ║
║ StyleTestMobActions: style-test mob/review arena world mutation     ║
╚════════════════════════════════════════════════════════════════════╝
                                │
                                ▼
╔════════════════════════════════════════════════════════════════════╗
║ Still To Close                                                    ║
╠════════════════════════════════════════════════════════════════════╣
║ GameplayPlaybackManager facade proof with live harness evidence    ║
║ rerun command observability after official Hytale launch/world load║
║ final PASS/FAIL/UNKNOWN evidence summary                           ║
╚════════════════════════════════════════════════════════════════════╝
```

When resuming, do not start by extracting generic helpers or reopening migrated
owners. Start by confirming the current rails still pass, then close the
remaining proof gaps. If proof exposes real behavior still hiding in
`GameplayPlaybackManager`, migrate/delete that owner and prove the same
behavior still passes. That cadence keeps the branch reviewable while
preserving the larger fresh-slate goal.

## Target Shape

```
╔════════════════════════════════════════════════════════════════════╗
║ MenteesMod                                                        ║
║   thin plugin facade: lifecycle calls, compatibility accessors     ║
╚═══════════════════════════════╦════════════════════════════════════╝
                                ▼
╔════════════════════════════════════════════════════════════════════╗
║ Lifecycle + Runtime Shell                                         ║
║   MotmLifecycleRegistrar                                          ║
║   MotmRuntimeLoop                                                 ║
║   MotmRuntimeTaskQueue + RuntimeTaskProcessor registry            ║
║   MotmDevHarness + MotmProofRuntime                               ║
╚═══════════════════════════════╦════════════════════════════════════╝
                                ▼
╔════════════════════════════════════════════════════════════════════╗
║ Content Shape Layer                                               ║
║   AbilityShape                                                    ║
║   normalized effect/cast/target/terrain/projectile descriptors    ║
║   validation diagnostics and generated catalogs                    ║
╚═══════════════════════════════╦════════════════════════════════════╝
                                ▼
╔════════════════════════════════════════════════════════════════════╗
║ Ability Runtime Registry                                          ║
║   projectile | field | terrain | summon | transform | follow-up   ║
║   each runtime owns state, tick, cleanup, observability contracts  ║
╚═══════════════════════════════╦════════════════════════════════════╝
                                ▼
╔════════════════════════════════════════════════════════════════════╗
║ Agent Verification Harness                                        ║
║   scenario catalog -> runner -> raw evidence bundle -> query      ║
╚════════════════════════════════════════════════════════════════════╝
```

## Architecture Principles

- New feature behavior should be added through a named owner, not by adding a
  new branch to a generic method.
- Ability identity may be used while building a profile or descriptor. Generic
  runtime code should execute capabilities, not ask "is this ability X?"
- JSON remains the authored content source, but runtime should consume
  normalized descriptors with known tokens, known primitive types, and explicit
  validation errors.
- Harness contracts stay stable; scenario/probe coverage is expected to grow
  with feature work.
- Observability is part of the runtime contract. New behavior should declare the
  evidence stream that proves it worked.
- Compile/build checks should catch unsupported descriptors, invalid scenario
  references, missing proof ids, and broad architectural violations before an
  agent needs to inspect the game manually.
- Do not preserve old code merely because it exists. Fresh replacement is the
  default: when a new owner replaces a legacy path, delete the old path in the
  same PR once parity is proven. Retaining old code has the burden of proof and
  must be quarantined behind a named compatibility boundary with a removal
  checklist.
- Verification must be CLI-driven. Do not rely on Computer Use, screen control,
  manual launcher clicking, or local GUI automation as a required part of the
  architecture. If authentication or launcher state blocks a run, the harness
  should report a precise CLI diagnostic and setup action rather than guessing
  or silently downgrading evidence quality.
- Windows support must be designed for a remote agent that cannot control the
  launcher UI. Setup, preflight, install, scenario execution, and diagnostics
  must be invokable from PowerShell. If official launcher login/session state is
  missing, the correct result is a clear diagnostic and rerunnable command path,
  not token scraping, auth bypass, screenshots, or local GUI automation.
- The auth/setup path must be agent-drivable from the CLI even when a human is
  the only actor who can complete official launcher login. Scripts should
  detect the Hytale root, installed jar, internal-build status, latest client
  log, command bridge readiness, and session-readiness signals; if any are
  missing, they should emit exact diagnostics and recovery commands rather than
  continuing with low-trust evidence.
- The harness may be modified by feature work. Adding a scenario, probe,
  diagnostic bundle, or evidence query is part of implementing a feature when
  the existing harness cannot prove the behavior tightly enough. The stable
  contract is raw, provenance-rich evidence plus trusted command/control
  boundaries; the catalog can grow.

## Legacy Code Policy

This project should not accumulate loose "legacy" concepts. The default posture
is fresh replacement: move the behavior to the correct owner, prove parity, and
delete the superseded implementation. A compatibility layer is acceptable only
when it has a clear consumer, a clear expiration condition, and evidence that
deleting it immediately would risk supported user data, public commands, or
harness contracts.

The policy is intentionally deletion-biased. MOTM should not remain compatible
with obsolete internal architecture. Internal package layout, method names,
manager responsibilities, pending queues, lookup tables, proof wiring, and data
flow are all replaceable implementation details. Keeping them alive just because
existing code once used them makes the next agent's path less obvious and
creates a second source of truth.

The unit of preservation is behavior, not code. When a subsystem is internal and
the supported behavior can be proven by focused unit tests, static architecture
rails, generated catalogs, or an observability scenario, a full rewrite is the
preferred cleanup path. Agents should feel empowered to remove the old structure
entirely instead of threading a new implementation through historical
vocabulary.

This is a strong rule, not a vibe. Legacy code does not get protected because it
exists, because it was hard to write, because a branch once depended on it, or
because deleting it makes the diff look larger. It is protected only when a
named active consumer would break and no proven replacement is ready. The
architecture should optimize for a fresh, current shape that future agents can
follow without archaeology.

```
╔════════════════════════════════════════════════════════════════════╗
║ Legacy Decision Rule                                              ║
╠════════════════════════════════════════════════════════════════════╣
║ Is the old path still called by a supported runtime or harness?    ║
║   ├─ no  ──▶ delete it                                             ║
║   └─ yes ──▶ can the new owner cover it with proven parity?        ║
║             ├─ yes ──▶ migrate callers, run evidence, delete old   ║
║             └─ no  ──▶ quarantine, document owner/removal gate      ║
╚════════════════════════════════════════════════════════════════════╝
```

Rules:

- A refactor is not complete when the new path works; it is complete when the
  new path works and the superseded internal path is gone or explicitly
  registered as a narrow compatibility exception.
- Deleting obsolete internal code is the default success path, not an optional
  cleanup phase. The compatibility register is an allowlist for named external
  contracts, not a parking lot for old architecture.
- A fresh-slate rewrite is appropriate when it lowers the number of concepts an
  agent must understand, makes the extension point obvious, and preserves
  supported behavior through evidence.
- Legacy support means compatibility for an external consumer, not permission to
  preserve internal shape. Old implementation architecture should be removed as
  soon as a new owner can carry the behavior.
- "Legacy" is not a neutral bucket. It is allowed only for active user-data
  migrations, public command/API compatibility, or harness contracts that would
  break a supported workflow today.
- Internal old/new duplication is a defect. If both paths exist after a change,
  the PR should explain the active consumer, the failing deletion gate, and the
  evidence required to remove the old path.
- Prefer a clean replacement over a compatibility shim when the changed surface
  can be proven through build checks, content validation, unit tests, or the
  observability harness.
- A compatibility facade may preserve public method names temporarily, but it
  should delegate to the new owner and contain no new feature logic.
- A bridge that contains decision logic is not a bridge; it is a competing
  implementation. Move that logic into the new owner or delete the bridge.
- Every retained compatibility exception must be recorded in
  `docs/compatibility-register.md` with its owner, consumer, preferred
  replacement, removal gate, and verification evidence.
- Deprecated behavior must have a named owner, a removal trigger, and an
  architecture-check exemption if it would otherwise look like forbidden code.
- Do not keep old concepts in neutral names. If a system is retained only for
  migration, name it as legacy or compatibility so agents do not treat it as the
  preferred extension point.
- Do not preserve code to reduce merge anxiety, keep speculative fallback
  behavior, or avoid touching callers. If the replacement is trusted enough to
  use, migrating callers and deleting the old path is part of the work.
- Do not preserve old code merely because the replacement is broad. Broad
  replacement is preferred when it gives agents a clearer single model and can
  be proven by the verification harness.
- Prefer a larger deletion-backed rewrite over a small adapter patch when the
  adapter would keep obsolete concepts in circulation.
- Do not split "new implementation" and "old cleanup" into separate intentions
  unless an active external consumer blocks deletion. For internal subsystems,
  the cleanup is part of the implementation.
- Do not write "temporary" compatibility without the deletion path in the same
  change. If the deletion path is unknowable, the design is not ready.
- Do not leave duplicate sources of truth. Once `AbilityShape`, runtime specs,
  scenario catalogs, or typed config owners exist, remove equivalent hard-coded
  tables and comments that point agents back to the old path.
- If a feature cannot be migrated safely in the same PR, add a checklist item
  with the exact behavior still depending on the legacy path and the evidence
  required to remove it.
- PR review should ask four questions for every retained old path: who consumes
  this, what breaks if it is deleted, what evidence proves the new path, and
  what exact gate removes the exception. If any answer is vague, delete or
  redesign instead of retaining it.

Expected deletion targets include obsolete pending maps after typed task
processors land, old ability-id branches after runtime profiles land, stale
script-only scenarios after scenario catalog support lands, and resource-cost
concepts that no longer belong after the no-resource casting pivot.

Compatibility has only three supported meanings:

```
╔════════════════════════════════════════════════════════════════════╗
║ Allowed Retention                                                 ║
╠════════════════════════════════════════════════════════════════════╣
║ saved user data migration                                         ║
║ public command/API contract still used by operators                ║
║ harness/tool contract still used by agents                         ║
╚════════════════════════════════════════════════════════════════════╝
```

Everything else should be treated as technical debt to remove, not legacy to
support. In particular, do not retain old package shape, old method
organization, duplicate lookup tables, ad hoc pending queues, or ability-id
branches just because existing code already points there. If the new owner is
not yet expressive enough to replace the old path, improve the owner contract
first; do not add another bridge that leaves both concepts alive.

Fresh-slate replacement is especially preferred when:

- the old path is internal-only and has no saved-data or public-command
  consumer;
- the old abstraction name teaches agents the wrong extension point;
- the adapter would preserve old data flow just to avoid changing callers;
- a scenario, proof command, or static architecture check can prove parity
  quickly;
- the compatibility layer would need feature logic rather than delegation.
- retaining the old path would teach an agent to edit the wrong owner.
- a complete rewrite can reduce the number of concepts agents must hold while
  preserving supported behavior through tests and harness evidence.
- the old code exists mostly to satisfy historical call sites that can be
  migrated in the same PR.
- the old shape would make future autonomous changes require search archaeology
  instead of following the runtime-family owner.

When old support is truly required, use names that advertise its status:
`legacy`, `migration`, or `compatibility`. Neutral names make future agents
expand the wrong path. The implementation file or package must be named in
`docs/compatibility-register.md`, and `scripts/check-architecture.ps1` should
reject new Java legacy/compatibility mentions until that explicit boundary
exists.

## Refactor Deletion Gate

Every broad refactor should leave the codebase with fewer normal paths than it
started with. Before a PR is considered done, answer these in the PR notes or
the relevant checklist:

```
╔════════════════════════════════════════════════════════════════════╗
║ Deletion Gate                                                     ║
╠════════════════════════════════════════════════════════════════════╣
║ What old concept disappeared?                                     ║
║ What supported behavior proves the replacement is equivalent?      ║
║ What raw/unit/static evidence backs that claim?                    ║
║ What retained path has an active external consumer?                ║
║ What exact gate deletes that retained path later?                  ║
╚════════════════════════════════════════════════════════════════════╝
```

If the first answer is "nothing disappeared," the work is probably an adapter
layer rather than a cleanup. If the retained consumer is an internal call site,
old package shape, stale test, branch merge concern, or agent uncertainty, it is
not a compatibility reason. Migrate the caller, strengthen the owner, and delete
the old path.

Fresh-slate replacement test:

```
╔════════════════════════════════════════════════════════════════════╗
║ Refactor Decision                                                 ║
╠════════════════════════════════════════════════════════════════════╣
║ Can the new owner express the behavior clearly?                   ║
║   ├─ yes ──▶ implement there, migrate callers, delete old code     ║
║   └─ no  ──▶ improve the owner contract before adding behavior     ║
║                                                                    ║
║ Is old code retained only to lower merge risk or avoid cleanup?    ║
║   ├─ yes ──▶ delete it; cleanup is part of the feature             ║
║   └─ no  ──▶ register the explicit compatibility exception         ║
╚════════════════════════════════════════════════════════════════════╝
```

Before settling on "support legacy," write down the exact consumer. If the
answer is an internal class, an old method name, an old package, a stale test, a
merge branch, or an agent's uncertainty, that is not legacy support. Improve the
new owner, migrate the caller, and delete the old path.

## Proposed Packages

```
src/main/java/com/motm
├─ lifecycle
│  ├─ MotmLifecycleRegistrar
│  ├─ MotmEventRegistrar
│  ├─ MotmSystemRegistrar
│  └─ MotmRegistrationLedger
├─ runtime
│  ├─ loop
│  ├─ task
│  ├─ ability
│  │  ├─ AbilityRuntimeRegistry
│  │  ├─ AbilityRuntimeContext
│  │  ├─ projectile
│  │  ├─ field
│  │  ├─ terrain
│  │  ├─ summon
│  │  ├─ transform
│  │  ├─ channel
│  │  ├─ self
│  │  └─ followup
│  ├─ state
│  └─ visual
├─ content
│  ├─ ability
│  │  ├─ AbilityShape
│  │  ├─ AbilityShapeFactory
│  │  └─ AbilityContentValidator
│  └─ catalog
├─ harness
│  ├─ dev commands
│  ├─ proof runtime
│  └─ scenario contracts
└─ util
   └─ narrow Hytale API affordances only
```

This package map is intentionally broad. Preserve external public APIs only while
they have active consumers; replace internal architecture aggressively once the
harness proves parity.

## Completion Checklist

### 1. Content Shape And Validation

- [x] Add `AbilityShape` as the normalized runtime view of `AbilityData`.
- [x] Normalize effect tokens, cast type, target type, terrain effect, travel
      type, visual overlay, projectile settings, field settings, and summon
      settings once during load or runtime catalog build.
- [x] Add a content validator that fails on unknown tokens, unsupported
      primitive combinations, missing required fields, and contradictory fields.
- [x] Add generated/readable catalogs of valid class ids, style ids, ability ids,
      proof ids, scenario ids, effect tokens, and cast primitives.
- [x] Add Gradle or script checks so invalid content shape fails before runtime.
- [x] Add Java tests for validator success/failure cases that do not need a
      running Hytale client.

### 2. Ability Runtime Registry

- [x] Introduce `AbilityRuntimeRegistry` and `AbilityRuntimeContext`.
- [x] Migrate projectile count/speed/radius/spread/cadence/lifetime and
      launch-profile decisions into a projectile runtime spec package.
  - [x] Move active projectile state into
        `runtime.ability.projectile.ActiveProjectile`, so projectile position,
      direction, profile-backed values, hit tracking, visual runtime refs,
      travel distance, and visual refresh scheduling no longer live as a
      private playback-manager inner class.
  - [x] Move the active projectile collection into
        `runtime.ability.projectile.ProjectileRuntimeState`, so projectile
        lifecycle removal, owner filtering, and processed-projectile cleanup are
        owned by the projectile runtime family instead of a raw
        playback-manager list.
  - [x] Move projectile visual runtime contract into
        `runtime.ability.projectile.ProjectileVisualRuntime`, so missing visual
        state and next-refresh timing are owned by the projectile family.
  - [x] Move shared visual proxy tracking into
        `runtime.state.VisualProxyRuntimeState`, so proxy registration,
        contains checks, counts, immutable snapshots, and removal are no longer
        a raw playback-manager set.
  - [x] Move projectile tick ordering into
        `runtime.ability.projectile.ProjectileTickRuntime`, so pre-activation
        visual refresh, travel advancement, traversal-hit ordering, direct-hit
        resolution order, impact/despawn decisions, and invalid-owner cleanup
        are owned by the projectile runtime family through Hytale-facing
        callbacks.
  - [x] Move projectile launch loop and active projectile construction into
        `runtime.ability.projectile.ProjectileLaunchRuntime`, so spread-angle
        calculation, launch delays, visual timing handoff, active projectile
        construction, trace propagation, and launch summaries are runtime-owned
        while concrete Hytale origin/direction lookup, visual spawn, and state
        registration stay callback-driven.
  - [x] Move concrete projectile visual proxy spawn/sync/refresh/despawn and
        identity-component hiding into `ProjectileVisualHytaleAdapter`, leaving
        `GameplayPlaybackManager` to delegate effect resolution and launch/tick
        wiring until the rest of the projectile Hytale adapter is migrated.
  - [x] Move concrete projectile launch origin/direction lookup into
        `ProjectileLaunchHytaleAdapter`, including owner transform reads,
        profile offsets, explicit-target aiming, look-direction fallback, and
        target-block aiming.
  - [x] Move concrete projectile hit scanning and impact/traversal target
        collection into `ProjectileHitHytaleAdapter`, including Hytale chunk
        scans, NPC/death/visual-proxy filtering, collision-radius segment
        projection, direct-hit splash fallback, radius collection, and processed
        entity de-duplication.
  - [x] Move projectile batch state registration and player cleanup delegation
        into `ProjectileRuntimeState`, so the playback manager no longer loops
        over launched projectiles or owns a projectile-specific cleanup wrapper.
  - [x] Move concrete projectile impact and traversal damage/effect mutation
        into `ProjectileImpactHytaleAdapter`, including damage application,
        impact effects, travel-type tokens, splash tokens, lightning arcs,
        target effect tokens, player damage statistics, lifesteal handoff, and
        kill/passive callbacks behind explicit support methods.
  - [x] Delete playback-manager projectile visual wrapper methods; launch and
        tick callbacks now call `ProjectileVisualHytaleAdapter` directly.
  - [x] Move concrete projectile tick hook wiring into
        `ProjectileTickHytaleAdapter`, so player lookup, owner-store checks,
        visual refresh/sync/despawn hooks, piercing classification, direct-hit
        resolution, traversal/impact mutation calls, and max-step distance are
        no longer owned by `GameplayPlaybackManager`.
  - [x] Move Hytale-facing projectile launch execution into
        `ProjectileLaunchHytaleAdapter`, including delayed projectile cast-type
        gating, player reference/store checks, spec resolution, origin/direction
        lookup, visual spawn handoff, state registration, trace propagation, and
        launched-count summary.
- [x] Move active projectile tick iteration, trace scoping, store ownership
      filtering, reset cleanup, and active-count reporting into
      `ProjectileLifecycleHytaleAdapter`.
- [x] Continue projectile migration by reducing the remaining projectile facade
      surface in `GameplayPlaybackManager` to constructor wiring and generic
      cross-family service callbacks once harness scenarios prove parity.
  - [x] Collapse the manager-local `launchProjectiles` wrapper and launch
        runtime/support fields into `ProjectileLaunchHytaleAdapter`
        construction, so casts call the projectile adapter directly and the
        manager no longer passes launch internals through every cast.
  - [x] Collapse projectile state/adapter wiring into `ProjectileRuntimeFacade`,
        so `GameplayPlaybackManager` keeps one projectile-family dependency plus
        generic support callbacks instead of individual projectile state,
        launch, hit, impact, tick, lifecycle, and visual adapter fields.
- [x] Migrate persistent field activation/tick/pulse/cleanup into a field
      runtime package.
  - [x] Move persistent field classification, dimensions, timing, pulse-damage
        ratios, pull-lift decisions, and field tick/visual refresh intervals
        into `runtime.ability.field.FieldRuntimeSpecs`.
  - [x] Move persistent field terrain policy into the field runtime package,
        including field terrain kind, placement reason, restore-before-place
        behavior, block/fluid candidates, column height, brown-debris summary
        decoration, Iron Wall origin policy, and caster-centered origin policy.
  - [x] Move recent field-origin state into
        `runtime.ability.field.RecentFieldOrigin`, so implausible player-position
        jump guards for Iron Wall and caster-centered fields are no longer
        represented as a generic playback-manager record.
  - [x] Move recent field-origin guard maps into
        `runtime.ability.field.FieldOriginRuntimeState`, so Iron Wall and
        caster-centered stable-origin windows, implausible-jump rejection, and
        reset cleanup are owned by the field runtime family instead of loose
        playback-manager maps.
  - [x] Move active field state into `runtime.ability.field.ActiveField`, so
        owner metadata, center/anchor state, field geometry, visual refs,
        trace id, and pulse/visual scheduling no longer live as a private
        playback-manager inner class.
  - [x] Move persistent field activation state construction and summaries into
        `runtime.ability.field.FieldActivationRuntime`, so active field
        construction, activation/expiry timing, visual runtime handoff,
        trace propagation, delayed/immediate summary wording, pull labels,
        terrain labels, and spawn-overlap push labels are runtime-owned while
        concrete Hytale origin/center lookup, terrain placement, overlap pushes,
        visual spawn, and state registration stay callback-driven.
  - [x] Move the active field collection and sinkhole burial entries into
        `runtime.ability.field.FieldRuntimeState`, so field lifecycle removal,
        owner/ability filtering, and empty engaged sinkhole state are owned by
        the field runtime family instead of raw playback-manager collections.
  - [x] Move field batch registration into `FieldRuntimeState`, so activation
        paths no longer loop over active fields in `GameplayPlaybackManager`.
  - [x] Move field visual runtime contract into
        `runtime.ability.field.FieldVisualRuntime`, so visual refs, loop effect
        id, missing-visual state, and next-refresh timing are owned by the field
        family.
  - [x] Move persistent field tick ordering into
        `runtime.ability.field.FieldTickRuntime`, so owner/store cleanup,
        pre-activation visual refresh, expiry cleanup ordering, sinkhole
        engagement, pulse target collection order, support pulses, suffocation
        pulses, and next-pulse scheduling are owned by the field runtime family
        through Hytale-facing callbacks.
  - [x] Move concrete Hytale field target collection into
        `runtime.ability.field.FieldTargetHytaleAdapter`, including Hytale chunk
        scans, NPC/death/visual-proxy filtering, ground-target checks, barrier
        bounds checks, and radius checks.
  - [x] Move concrete Hytale field visual proxy spawn, sync, refresh, despawn,
        and visual-position planning into
        `runtime.ability.field.FieldVisualHytaleAdapter`.
  - [x] Move concrete Hytale field pulse damage/effect mutation and per-target
        terrain token routing into `runtime.ability.field.FieldPulseHytaleAdapter`,
        including damage application, impact effects, target effect tokens,
        terrain-effect tokens, player damage statistics, lifesteal handoff, and
        pull/barrier callbacks behind explicit support methods.
  - [x] Move concrete Hytale field support and owner pulse mutation into
        `runtime.ability.field.FieldSupportPulseHytaleAdapter`, including
        owner-in-field checks, caster effect token routing, support heal/shield
        pulse scaling, owner terrain-effect routing, and owner status tokens
        behind explicit support methods.
  - [x] Move concrete Hytale sinkhole mutation into
        `runtime.ability.field.FieldSinkholeHytaleAdapter`, including burial
        state engagement, target collection handoff, buried/root effect
        application, suffocation damage ticks, release cleanup, and surface
        marker handoff behind explicit support methods.
  - [x] Move concrete Hytale field terrain restoration into
        `runtime.ability.field.FieldTerrainHytaleAdapter`, including restore
        reason resolution and temporary-selection restoration handoff.
  - [x] Move concrete Hytale field owner mobility into
        `runtime.ability.field.FieldOwnerMobilityHytaleAdapter`, including
        follow-owner anchor sync, Lava Pool protection, negative-status cleanup,
        movement setting boosts, movement reset, and velocity boost cleanup.
  - [x] Move persistent field activation execution into
        `runtime.ability.field.FieldActivationHytaleAdapter`, including player
        ref/store validation, origin/direction reads, field spec assembly,
        terrain placement handoff, Iron Wall overlap-push handoff, visual spawn
        handoff, state registration, trace propagation, and activation summary.
        Remaining terrain placement and overlap-push support callbacks are
        tracked by the terrain world-mutation item below.
- [x] Migrate temporary terrain placement/restoration into a terrain runtime
      package.
  - [x] Move terrain trail/aura classification, trail node/radius decisions,
        aura radius decisions, trail-center interpolation, temporary selection
        minimum lifetime, moving-trail cadence, and stacking-column cadence into
        `runtime.ability.terrain.TerrainRuntimeSpecs`.
  - [x] Move temporary terrain selection, moving trail, stacking column, and
        terrain collection ownership into `runtime.ability.terrain`, so raw
        terrain lists and mutable terrain scheduling state no longer live
        directly in `GameplayPlaybackManager`.
  - [x] Move moving-trail, Lapidary gem, and stacking-column construction into
        `runtime.ability.terrain.TerrainActivationRuntime`, so terrain-family
        validation and active runtime object construction are owned outside the
        playback manager while concrete Hytale block/entity spawn and mutation
        stay callback-driven.
  - [x] Move active Lapidary gem state into
        `runtime.ability.terrain.ActiveLapidaryGem`, so gem ownership, health,
        center copying, label updates, and expiry checks no longer live as an
        untyped playback-manager record.
  - [x] Move active Lapidary gem lifecycle collection into
        `runtime.ability.terrain.LapidaryGemRuntimeState`, so gem registration,
        owner cleanup, processed tick removal, and gem-anchor lookup are owned
        by the terrain runtime family instead of a raw playback-manager list.
  - [x] Move Lava Pool and magma hazard bookkeeping into
        `runtime.ability.terrain.LavaHazardRuntimeState`, so hazard protection
        expiry, movement-boost flags, velocity-boost copies, and per-player
        cleanup are owned by the terrain runtime family instead of loose
        playback-manager maps.
  - [x] Move temporary terrain tick ordering into
        `runtime.ability.terrain.TerrainTickRuntime`, so expired-selection
        restoration decisions, moving-trail cadence, owner-position checks,
        block-type usability decisions, trail stamp interpolation, stacking
        column staging, and removal decisions are owned by the terrain runtime
        family through Hytale-facing callbacks.
  - [x] Move concrete Hytale terrain tick/restoration hooks into
        `runtime.ability.terrain.TerrainHytaleAdapter`, including expired
        selection restoration, moving-trail owner/store/position reads, moving
        trail stamp block construction, stacking-column stage block
        construction, and reset-world restoration delegation.
  - [x] Move concrete Hytale terrain placement into
        `runtime.ability.terrain.TerrainPlacementHytaleAdapter`, including
        block/fluid asset resolution, temporary selection registration, moving
        trail registration, stacking-column registration, surface/ring/trail/
        column/shell/fluid `BlockSelection` construction, persistent field
        terrain placement, restore-before-place calls, and Iron Wall overlap
        push mutation.
  - [x] Move ability-specific terrain routing into
        `runtime.ability.terrain.TerrainAbilityHytaleAdapter`, leaving explicit
        callbacks for cross-family effects, position/direction reads, active gem
        anchor lookup, and Lapidary proxy spawn.
  - [x] Move Lapidary gem proxy lifecycle into
        `runtime.ability.terrain.TerrainGemHytaleAdapter`, including proxy NPC
        spawn, HP label mutation, visual-proxy registration/despawn, active gem
        tick processing, owner cleanup, and active gem anchor lookup.
  - [x] Move sinkhole terrain marker orchestration into
        `runtime.ability.terrain.TerrainSinkholeMarkerHytaleAdapter`, including
        crack and dust-ring placement around sinkhole fields.
  - [x] Move supplemental non-persistent terrain trail/aura activation into
        `runtime.ability.terrain.TerrainSupplementalHytaleAdapter`, including
        trail/aura selection, visual spawn handoff, field-runtime registration,
        surface cue placement, summary formatting, and an architecture ratchet
        preventing the playback manager from re-owning that activation path.
- [x] Migrate summon/proxy state and attacks into a summon runtime package.
  - [x] Move summon role/profile decisions into
        `runtime.ability.summon.SummonRuntimeSpecs`, including named model
        mappings, role classification, ranged/melee behavior, attack/chase
        ranges, attack cadence, hatch delay, damage multipliers, and default
        attack tokens.
  - [x] Move active summon state into
        `runtime.ability.summon.ActiveSummon`, so owner refs, summon refs,
        profile-backed values, target locks, hatch/think/attack/buff timing,
        clone expiry, and base damage no longer live as a private
        playback-manager inner class.
  - [x] Move active summon construction into
        `runtime.ability.summon.SummonActivationRuntime`, so profile
        resolution, hatch timing, initial think/attack scheduling, awakened
        state, and raw-to-profile base damage conversion are runtime-owned.
  - [x] Move the active summon owner index into
        `runtime.ability.summon.SummonRuntimeState`, so owner counts, owner
        snapshots, spawn registration, despawn cleanup, and processed summon
        removal are owned by the summon runtime family instead of a raw
        playback-manager map.
  - [x] Move summon tick decisions into
        `runtime.ability.summon.SummonTickRuntime`, so expiry cleanup,
        hatch/awake timing, owner validation, target resolution order, owner
        follow behavior, target chase/retreat decisions, attack readiness, and
        next-think scheduling are owned by the summon runtime family through
        Hytale-facing callbacks.
  - [x] Move summon attack-effect routing into
        `runtime.ability.summon.SummonAttackEffectRuntime`, so base attack
        tokens, tank shield/pull behavior, buff-window dot behavior, named
        summon splash/rider/shield/damage specials, hatchling awakened specials,
        and clone blind routing are runtime-owned.
  - [x] Move summon attack lifecycle into
        `runtime.ability.summon.SummonAttackRuntime`, so clone pre-hit
        repositioning, buffed damage multiplier, incoming/absorb ordering,
        damage/post-damage/lifesteal/impact/effect ordering, next-attack
        scheduling, resolved-attack logging, and clone post-strike expiry are
        runtime-owned.
  - [x] Move summon buff/command routing into
        `runtime.ability.summon.SummonBuffRuntime`, so active-summon filtering,
        radius checks, duration extension, buff-window extension, attack-soon
        scheduling, hatch/awakened command gating, commanded strike counting,
        and summaries are runtime-owned.
  - [x] Move summon target selection into
        `runtime.ability.summon.SummonTargetRuntime`, so target-lock reuse,
        lock expiration, tank/clone/default search anchors, role-specific search
        radii, fallback anchors, and lock update/clear behavior are
        runtime-owned.
  - [x] Move summon movement destination planning into
        `runtime.ability.summon.SummonMovementRuntime`, so owner-follow
        placement, target approach travel clamping, ranged retreat travel
        clamping, and clone beside-target placement are runtime-owned.
  - [x] Move concrete Hytale summon spawn, appearance mutation, active summon
        registration, raw base-damage calculation, spawn-position lookup, model
        resolution, owner cleanup, and despawn mutation into
        `runtime.ability.summon.SummonLifecycleHytaleAdapter`.
  - [x] Move concrete Hytale summon buff routing, tick hook wiring, NPC
        movement, target acquisition, awaken visual mutation, and clone
        repositioning into `runtime.ability.summon.SummonControlHytaleAdapter`.
  - [x] Move concrete Hytale summon damage, impact effects, attack-effect
        tokens, shields, pulls, splash target iteration, and splash damage into
        `runtime.ability.summon.SummonAttackHytaleAdapter`.
  - [x] Move summon splash iteration and splash damage math into
        `runtime.ability.summon.SummonSplashRuntime`, so primary-target
        exclusion, max-candidate padding, base-damage ratio math,
        incoming/absorb ordering, damage/post/impact ordering, and null
        collection handling are runtime-owned.
  - [x] Migrate transformations and weapon follow-ups into dedicated runtimes.
  - [x] Move transformation profile decisions into
        `runtime.ability.transformation.TransformationRuntimeSpecs`, including
        form visual effect ids, combat bonuses, movement values, rider tokens,
        collision radii, locomotion trigger distances, and summaries.
  - [x] Move transformation capability kind, owner refresh tokens, owner shield
        refresh amount, and grounded-ending policy into the transformation
        runtime spec, so playback execution branches on resolved capabilities
        instead of raw transformation ability ids.
  - [x] Move active transformation state into
        `runtime.ability.transformation.ActiveTransformation`, so profile-backed
        bonuses, owner position tracking, model id, expiry, and source ability
        state no longer live as a private playback-manager inner class.
  - [x] Move the active transformation map and pulse schedule into
        `runtime.ability.transformation.TransformationRuntimeState`, so
        player lookup, ability deactivation, processed tick removal, and
        next-pulse ownership no longer live as playback-manager maps.
  - [x] Move transformation tick decisions into
        `runtime.ability.transformation.TransformationTickRuntime`, so owner
        validity, expiry, pulse cadence, player lookup, end-condition checks,
        owner-position gating, owner-state refresh ordering, locomotion
        pressure ordering, form pulse dispatch, and next-pulse scheduling are
        owned by the transformation runtime family through Hytale-facing
        callbacks.
  - [x] Move transformation pulse and locomotion effect routing into
        `runtime.ability.transformation.TransformationEffectRuntime`, so
        Smoke/Pterodactyl/Triceratops/T-Rex target selection shape, damage
        ratios, rider tokens, knockback flags, movement-factor clamping, and
        charge-shield ordering are runtime-owned.
  - [x] Move concrete Hytale transformation activation, tick hook wiring, owner
        refresh mutation, locomotion/pulse damage and effects, charge impacts,
        weapon rider mutation, and transformation cleave into
        `runtime.ability.transformation.TransformationHytaleAdapter`.
  - [x] Move weapon follow-up arming/profile decisions into
        `runtime.ability.followup.WeaponFollowUpSpecs`, including Alloy
        Enhancement native-hit routing metadata, generic buff follow-up uses,
        damage bonuses, rider tokens, splash settings, and excluded shield-only
        buffs.
  - [x] Move active weapon follow-up state into
        `runtime.ability.followup.ActiveWeaponFollowUp`, so use countdown,
        bound item state, source ability metadata, and spec-derived values no
        longer live as a private manager inner class.
  - [x] Move the active follow-up map into
        `runtime.ability.followup.WeaponFollowUpRuntimeState`, so
        player-indexed follow-up lifecycle state is owned by the runtime family
        instead of a raw playback-manager collection.
  - [x] Move the pure Alloy item-binding rule into
        `runtime.ability.followup.ActiveWeaponFollowUp`/
        `WeaponFollowUpItemBinding`, so the runtime family owns first-item
        binding and switched-item rejection.
  - [x] Move Alloy durability restoration into
        `runtime.ability.followup.WeaponFollowUpDurabilityRestorer`, so
        Hytale inventory repair for follow-ups is no longer a playback-manager
        helper.
  - [x] Move pure follow-up hit math into
        `runtime.ability.followup.WeaponFollowUpHitMath`, including base damage,
        attack modifier, passive/incoming multiplier application, Alloy native
        hit scaling, and splash ratio calculation.
  - [x] Move follow-up rider/payoff orchestration into
        `runtime.ability.followup.WeaponFollowUpHitEffects`, with callbacks for
        Hytale token, shield, healing, and splash effects so runtime-owned order
        is testable while concrete Hytale calls stay in the manager.
  - [x] Move Alloy visual effect identity and apply/clear callback contract into
        `runtime.ability.followup.WeaponFollowUpVisualEffects`, so the
        follow-up runtime family owns the visual contract while concrete Hytale
        effect mutation remains callback-driven.
  - [x] Move follow-up splash target iteration and per-target ordering into
        `runtime.ability.followup.WeaponFollowUpSplashRuntime`, with callbacks
        for concrete Hytale damage, post-damage passives, rider tokens, and
        impact effects.
  - [x] Move primary follow-up hit ordering into
        `runtime.ability.followup.WeaponFollowUpPrimaryHitRuntime`, with
        callbacks for concrete Hytale damage, post-damage passives, lifesteal,
        impact effects, and rider tokens.
  - [x] Move native Alloy hit and tool-use ordering into
        `runtime.ability.followup.WeaponFollowUpNativeAlloyRuntime`, with
        callbacks for Hytale damage mutation, visual mutation, durability
        restoration, rider/effect application, and follow-up removal.
  - [x] Move follow-up lifecycle expiry into
        `runtime.ability.followup.WeaponFollowUpLifecycleRuntime`, with
        callbacks for player availability, current-store mutation gating, Alloy
        visual cleanup, and explicit end-reason logging.
  - [x] Move combat support bookkeeping into
        `runtime.ability.combat.CombatRuntimeState`, so ability-kill
        de-duplication and recent shock expiry windows are no longer raw
        playback-manager collections.
  - [x] Move concrete Hytale follow-up primary hit mutation, payoff
        shields/healing/splash, native Alloy damage/tool-use hooks, Alloy
        held-item visual mutation, and follow-up splash mutation into
        `runtime.ability.followup.WeaponFollowUpHytaleAdapter`.
- [x] Move channel and line-control active state into
      `runtime.ability.channel`, so channel owner/target refs, ability metadata,
      expiry, and pulse scheduling no longer live as private playback-manager
      classes.
- [x] Move channel and line-control construction into
      `runtime.ability.channel.ChannelActivationRuntime`, so active channel
      input validation and runtime object construction are owned outside the
      playback manager while concrete Hytale target lookup and pulse mutation
      remain callback-driven.
- [x] Move channel and line-control lifecycle collections into
      `runtime.ability.channel.ChannelRuntimeState`, so replace-per-player,
      owner cleanup, ability deactivation, and processed tick removal are owned
      by the channel runtime family instead of raw playback-manager lists.
- [x] Move concrete Hytale channel and line-control activation/tick mutation
      into `runtime.ability.channel.ChannelHytaleAdapter`, including
      owner/target validation, line-control duration inference, channel pulse
      damage, lifesteal/life-drain/healing, line-control pulls, repeat
      target-token application, processed runtime removal, and deactivation
      handoff.
- [x] Move active self-effect and player-anchor state into
      `runtime.ability.self`, so Obsidian Skin-style anchors, completion
      effects, repeated self-effect timing, and anchor position copying are
      owned outside the playback manager.
- [x] Move self-effect and player-anchor construction into
      `runtime.ability.self.SelfActivationRuntime`, so input validation and
      active self runtime object construction are owned outside the playback
      manager while concrete Hytale effect application and anchor enforcement
      remain callback-driven.
- [x] Move active self-effect and player-anchor lifecycle collections into
      `runtime.ability.self.SelfRuntimeState`, so replace-per-player anchors,
      effect replacement, owner cleanup, and processed tick removal are owned
      by the self runtime family instead of raw playback-manager lists.
- [x] Move concrete Hytale self-effect and player-anchor mutation into
      `runtime.ability.self.SelfHytaleAdapter`, including repeated effect
      ticking, completion effect dispatch, anchor position enforcement,
      movement-freeze mutation, velocity zeroing, processed runtime removal,
      and owner cleanup.
- [x] Move armed stomp state into `runtime.ability.stomp.ArmedStomp`, so
      transform landing observation, airborne tracking, and trigger expiry are
      owned outside the playback manager.
- [x] Move armed stomp lifecycle map into
      `runtime.ability.stomp.StompRuntimeState`, so arming, lookup, snapshot
      iteration, compare-and-remove, and observation replacement are owned by
      the stomp runtime package instead of a raw playback-manager map.
- [x] Move buried-victim state into `runtime.ability.field.BuriedVictim`, so
      sinkhole victim refs, previous gravity, and expiry are owned by the field
      runtime family instead of an anonymous playback-manager record.
- [x] Keep `GameplayPlaybackManager` as a facade until all migrated paths are
      harness-proven.
  - [x] Final tightened command observability proof passed against a running
        official Hytale session after the player-ready lifecycle fix.
        Evidence: `mac-command-observability-smoke-20260526-final-readyfix`.
- [x] Add an architecture check that flags new `isXAbility` helpers or
      `ability.getId()` switches inside generic runtime packages unless they
      live in a registry/profile builder.
- [x] Remove migrated ability-id branches after profile/spec parity is proven;
      do not leave duplicate old/new behavior paths.
  - [x] Move the manager-local proof/coating/terrain one-off switch into
        `runtime.ability.specific.AbilitySpecificHytaleAdapter` and add a
        ratchet preventing that switch from returning to
        `GameplayPlaybackManager`.
  - [x] Move generic execution-policy classification into
        `AbilityExecutionPolicy`, including caster visual suppression,
        movement/line/multi-target cast families, caster/target token filters,
        Dominate extra target riders, Alloy caster-token exclusion, ground
        restriction, anchor-drag classification, direct damage cause, special
        damage policy selection, and a ratchet preventing those branches from
        returning to `GameplayPlaybackManager`.
- [x] Add scenario coverage for at least one ability in each runtime family:
      projectile (`terra-projectile-magma-sling`), field/terrain
      (`terra-field-iron-wall`), summon (`hydro-summon-snow-imp`),
      transformation (`aero-transformation-smoke-form`), and follow-up
      (`terra-followup-alloy-enhancement`).

### 3. Runtime Loop And Task Ownership

- [x] Add intent methods for runtime task scheduling and architecture checks
      that prevent direct `add`/`put` mutations from `MenteesMod`.
- [x] Add a `processPending...` migration ratchet so pending task processors in
      `MenteesMod` cannot grow while families move to `runtime.task`.
- [x] Finish replacing exposed mutable map/set processor access with typed task
      records and processor-owned drain/complete methods.
- [x] Add a `RuntimeTaskProcessor` registry with ordered processor execution.
- [x] Add one processor per task family; inventory-facing grants/syncs use
      `InventoryRuntimeTaskProcessor`, HUD install/refresh uses
      `StatusHudRuntimeTaskProcessor`, dev relocation/daylight/mode changes use
      `DevRuntimeTaskProcessor`, proof requests use
      `ProofRuntimeTaskProcessor`, player maintenance uses
      `PlayerMaintenanceRuntimeTaskProcessor`, combat ability casts use
      `AbilityCastRuntimeTaskProcessor`, ability test scheduling uses
      `AbilityTestRuntimeTaskProcessor`, style review resets use
      `StyleReviewRuntimeTaskProcessor`, style-test mobs use
      `StyleTestMobRuntimeTaskProcessor`, and Terra review work uses
      `TerraReviewRuntimeTaskProcessor`.
- [x] Move remaining `processPending...` methods out of `MenteesMod` into
      processors; the architecture ratchet now allows zero such methods.
- [x] Move server-tick runtime sequencing out of `MenteesMod` into
      `MotmRuntimeLoop`, including processor order, free-cast safety,
      class-passive ticking, dev command inbox processing, proof cleanup,
      armed-stomp/gameplay ticks, HUD refresh cadence, observability heartbeat
      cadence, pending-task heartbeat payloads, DoT diagnostics, focused tests,
      and an architecture ratchet keeping `onServerTick` delegation-only.
- [x] Move dev command inbox runtime-player selection and trace fallback out of
      `MenteesMod` into `MotmDevCommandInboxProcessor`, leaving file I/O in
      `MotmDevCommandInbox`, adding focused tests, and ratcheting the plugin
      helper to delegation-only.
- [x] Move command-facing ability-cast queue policy out of `MenteesMod` into
      `AbilityCastCommandActions`, including request validation, queue evidence
      handoff through `MotmRuntimeTasks`, focused tests, and an architecture
      ratchet keeping the public queue method delegation-only.
- [x] Move command-facing free-cast access out of `MenteesMod` into
      `FreeCastCommandActions`, including enabled-state readback, enable/disable
      mutation, invulnerability-clear task scheduling/cancellation, focused
      tests, and an architecture smoke check keeping public free-cast methods
      delegation-only.
- [x] Delete direct collection accessors and tick branches once their typed task
      processors are verified; pending collections are now private state behind
      immutable pending views and completion/cancel intent methods.
- [x] Preserve ordering/coalescing semantics with explicit processor order.
- [x] Emit task accepted, task executed, task failed, and task skipped evidence.
- [x] Add tests for queue coalescing and snapshot output.
- [x] Confirm the observability heartbeat still reports actionable task state
      through the `pendingTasks` snapshot.

### 4. Lifecycle And Plugin Shell

- [x] Split event, command, and system registration into a lifecycle registrar
      class.
- [x] Track event/command registrations in a cleanup ledger and keep system
      registration guarded in the same owner.
- [x] Move observability packet watcher registration into a lifecycle owner with
      cleanup handling.
- [x] Move native Hydro recipe registration into a lifecycle owner with cleanup
      notes for the asset-store API path.
- [x] Move codec registration into lifecycle owners with cleanup notes for APIs
      that do not return handles.
- [x] Keep Hytale hook registration in the correct lifecycle phase; static
      architecture smoke checks assert `start()` registers hooks and
      `shutdown()`/`onDisable()` remove packet watchers and Hytale hooks.
- [x] Move server config loading into a typed `MotmServerConfig` owner.
- [x] Remove handwritten config defaults and duplicated config parsing once the
      typed config owner is active.
- [x] Move plugin data directory migration and scanner-safe legacy manifest
      writing into `MotmPluginDataDirectories`; update the compatibility
      register so this exception no longer lives in the plugin shell.
- [x] Move file-backed dev command inbox/outbox polling, command normalization,
      trace envelope, and outbox writes into `MotmDevCommandInbox`.
- [x] Add an architecture ratchet so `MenteesMod` cannot re-own
      `dev-command-inbox.txt`/`dev-command-outbox.log` file protocol handling.
- [x] Move Terra inventory resource prefix/unit policy into
      `TerraInventoryResourcePolicy`.
- [x] Move Terra inventory count/spend bridge orchestration into
      `TerraInventoryResourceBridge`; leave `MenteesMod` with bridge wiring
      only.
- [x] Move combined Hytale player inventory assembly into
      `MotmPlayerInventory` so inventory owners can share one container view.
- [x] Move Hydro waterskin item ids, metadata keys, tier detection, stack
      creation, resource-manager bridge lookup, and sync/remove orchestration
      into `HydroContainerItems` and `HydroInventoryBridge`.
- [x] Move spellbook/dev grimoire item identity and saved-inventory migration
      item tables into `SpellbookInventoryItems`, leaving public plugin methods
      as delegates for current callers.
- [x] Add an architecture ratchet so `MenteesMod` cannot re-own Terra resource
      prefix tables, unit tables, local prefix matching helpers, or inventory
      count/spend bridge methods.
- [x] Add an architecture ratchet so `MenteesMod` cannot re-own combined player
      inventory container assembly.
- [x] Add an architecture ratchet so `MenteesMod` cannot re-own Hydro waterskin
      constants, BSON metadata parsing, stack creation, tier checks, or
      container sync/remove orchestration.
- [x] Add an architecture ratchet so `MenteesMod` cannot re-own spellbook/dev
      grimoire item identity tables.
- [x] Keep `MenteesMod` as a thin facade for lifecycle and compatibility access.
  - [x] Re-inspect remaining plugin-shell methods after the latest extractions.
        Treat any remaining public compatibility accessor or private plugin
        method as suspicious until it is proven delegation-only or moved behind
        a named owner. Do not re-open observability snapshots, progression
        policy, runtime-player accessors, geometry readback, spellbook delivery,
        or HUD install/refresh unless fresh inspection finds behavior outside
        their current owners.
  - [x] Extract or delete any remaining plugin-shell method that owns behavior
        sequencing, target selection, world mutation, evidence shaping outside
        the observability boundary, or nontrivial runtime state.
  - [x] Move agent observability snapshot evidence-shape ownership out of
        `MenteesMod` into `MotmObservabilitySnapshotBuilder`, including pending
        task counts, player data/runtime/native-effect/stat/movement/inventory
        snapshots, style-test target rows, proof native-effect readback, and a
        ratchet preventing those helpers from returning to the plugin shell.
  - [x] Move command-facing observability run control out of `MenteesMod` into
        `MotmObservabilityActions`, including start/stop/status, scenario
        mutation, markers, snapshot capture, metadata assembly, focused tests,
        and smoke checks keeping public plugin methods delegation-only.
  - [x] Move observability event emission and trace-context policy out of
        `MenteesMod` into `MotmObservabilityEvents`, including control/
        causality/server-truth/client-intent writes, current trace lookup,
        enter/restore semantics, active-run client trace allocation, focused
        tests, and ratchets preventing trace ownership from returning to the
        plugin shell.
  - [x] Move player progression runtime stat mutation and world-average level
        policy out of `MenteesMod` into `PlayerProgressionRuntimeActions`,
        including target-health tracking, health modifier apply/clear,
        free-cast max-health preservation, all-online progression refresh
        iteration, and a ratchet preventing progression stat mutation from
        returning to the plugin shell.
  - [x] Move runtime-player lookup and geometry helpers out of `MenteesMod`
        into `RuntimePlayerView`, including player-id/ref lookup, universe
        refs, store/world membership checks, position/forward readback,
        movement-state/crouch readback, vector formatting, dev position
        summaries, and a ratchet preventing those helpers from returning to the
        plugin shell.
  - [x] Move mob-scaling anchor-level policy out of `MenteesMod` into
        `PlayerProgressionRuntimeActions`.
  - [x] Move perk trigger registration and on-kill trigger health mutation out
        of `MenteesMod` into `PerkTriggerRuntimeActions`, leaving public plugin
        methods as delegates for current stat-modifier callers and adding a
        ratchet preventing perk-trigger runtime effects from returning to the
        plugin shell.
  - [x] Move player runtime rebuild sequencing out of `MenteesMod` into
        `PlayerRuntimeRebuildActions`, including cooldown reset, passive/status/
        reaction/resource cleanup, persistent resource sync, synergy/race reset,
        class resource/perk/race reapply, Hydro sync queueing, progression/HUD
        refresh, free-cast invulnerability handling, focused tests, and a smoke
        check keeping the plugin method delegation-only.
  - [x] Move live mob-spawn scaling policy out of `MenteesMod` into
        `MobSpawnRuntimeActions`, including scaling-player lookup, category
        anchor resolution, base-stat fallback, boss/ordinary scaling,
        party/environment modifiers, elite promotion, display name/color
        resolution, model-level result ownership, focused tests, and a ratchet
        preventing the plugin shell from re-owning that scaling pipeline.
  - [x] Move combat lifecycle kill/death side effects out of `MenteesMod` into
        `PlayerCombatLifecycleActions`, including mob-kill leveling/resource/
        passive/perk/achievement refresh flow, player-death statistics/combo
        reset, status/elemental/armed-stomp cleanup, HUD refresh, focused tests,
        and smoke checks keeping the public lifecycle methods delegation-only.
  - [x] Move player session lifecycle handling out of `MenteesMod` into
        `PlayerSessionLifecycleActions`, including join data/rested/resource/
        perk/race/passive rehydration, connect saved-loadout rebuild and
        spellbook recovery, ready-time dev cleanup/free-cast/HUD setup,
        disconnect persistence and runtime cleanup, focused tests, and smoke
        checks keeping public session callback methods delegation-only.
  - [x] Move spellbook/dev-book delivery and saved-inventory migration out of
        `MenteesMod` into `SpellbookInventoryKit`, including legacy spellbook
        cleanup, default spellbook grant, dev grimoire grant, player messages,
        and a ratchet preventing those inventory mutation paths from returning
        to the plugin shell.
  - [x] Move custom HUD install/refresh behavior out of `MenteesMod` into
        `MotmStatusHudActions`, including HUD construction, install queuing,
        refresh queuing, native HUD component hiding, custom HUD client intent
        emission, refresh intent emission, stale HUD cleanup, and player/store
        filtering, with a ratchet preventing that behavior from returning to
        the plugin shell.
  - [x] Move dev style-test command policy out of `MenteesMod` into
        `StyleTestCommandActions`, including style lookup, live style-test
        start/stop/status messaging, single-ability test queueing, proof request
        validation, style-test mob task queueing, review arena reset queueing,
        weapon follow-up probe execution, forced stomp landing, free-cast
        toggles, and a ratchet preventing that policy from returning to the
        plugin shell.
  - [x] Move dev runtime command queue policy out of `MenteesMod` into
        `DevRuntimeCommandActions`, including relocation target validation,
        daylight request queueing, review game-mode parsing, Terra review
        kit/cleanup queueing, dev-tools gating, player-runtime availability
        checks, and a ratchet preventing that policy from returning to the
        plugin shell.
  - [x] Move dev-tools disabled messaging into `MotmCommandAuth`, so the plugin
        shell delegates command authorization text instead of owning build/config
        message policy.
  - [x] Move spellbook custom-page opening out of `MenteesMod` into
        `SpellbookPageActions`, including custom page construction, Hytale
        page-manager mutation, player-ref validation, custom-page client intent
        emission, and a ratchet preventing that page-opening behavior from
        returning to the plugin shell.
  - [x] Move command-facing inventory task queue policy out of `MenteesMod` into
        `InventoryCommandActions`, including Hydro container sync queueing,
        spellbook/dev-book grant queueing, runtime-player id resolution,
        runtime-player handle refresh for player overloads, dev-tools gating,
        and a ratchet preventing that queue policy from returning to the plugin
        shell.
- [x] Move runtime effect-id resolver policy out of `GameplayPlaybackManager`
      into `AbilityRuntimeEffects`, including class fallback effects, themed
      style overrides, runtime effect-id filtering, movement cast effect
      selection, tests, and a ratchet preventing those resolver tables from
      returning to the manager.
- [x] Move ability status-effect construction policy out of
      `GameplayPlaybackManager` into `AbilityStatusEffects`, including token to
      status type/value mapping, configured/default duration handling, one-shot
      buff duration defaults, tests, and a ratchet preventing that switch table
      from returning to the manager.
- [x] Keep any `MenteesMod` compatibility methods delegation-only, with no new
      runtime behavior added there.
  - [x] Any retained compatibility method must either delegate to a named owner
        or be registered in `docs/compatibility-register.md` with consumer,
        verification evidence, and removal gate.
- [x] Move Hydro water-source refill handling out of `MenteesMod` into
      `HydroContainerRefillHandler`, including water-source probing, resource
      refill, persistence, HUD refresh, and player messages.
- [x] Move block-damage interaction handling out of `MenteesMod` into
      `BlockDamageInteractionHandler`, including pickaxe detection, nearest
      Terra miner selection, block damage scaling, Alloy tool-use dispatch, and
      player messaging.
- [x] Move spellbook input routing out of `MenteesMod` into
      `SpellbookInputHandler`, including interact/mouse/custom-interaction
      slot routing, spellbook/dev-book gesture policy, duplicate debounce,
      Hydro refill handoff, weapon follow-up hit handoff, and player messages.
- [x] Move proof cleanup processing out of `MenteesMod` into
      `MotmProofCleanupProcessor`, including temporary selection restoration,
      proxy despawn, store/world filtering, cleanup removals, and diagnostic
      logging.
- [x] Move free-cast protection state and spellbook input debounce state out of
      `MenteesMod`; lower the mutable runtime collection ratchet from ten to
      seven.
- [x] Move live style-test target tracking and active style-test sequencing out
      of `MenteesMod`; lower the mutable runtime collection ratchet from seven
      to five.
- [x] Move temporary proof cleanup selections/proxies out of `MenteesMod` into
      a named proof cleanup state owner.
- [x] Move progression target-health tracking and perk trigger bindings out of
      `MenteesMod`; lower the mutable runtime collection ratchet from five to
      three.
- [x] Move runtime player handles/initialization and custom HUD handles behind
      named state owners; lower the mutable runtime collection ratchet from
      three to zero.
- [x] Move proof world/effect actions out of `MenteesMod` into
      `MotmProofActions`, including effect application, target-effect lookup,
      temporary block/fluid placement, proxy spawn, movement proof teleport,
      cleanup-state registration, and proof evidence emission.
- [x] Move live style-test sequencing and target lookup out of `MenteesMod`
      into `StyleTestSequenceRuntimeTaskProcessor` and
      `StyleTestTargetResolver`.
- [x] Move Terra review inventory kit grant/cleanup policy out of `MenteesMod`
      into `TerraReviewInventoryKit`.
- [x] Move free-cast safety, refill, movement normalization, and native
      invulnerability attach/remove out of `MenteesMod` into
      `FreeCastSafetyProcessor`.
- [x] Move dev player relocation/platform/game-mode test mutation out of
      `MenteesMod` into `DevPlayerTestActions`.
- [x] Move style-test mob spawn/cleanup/count and review arena scrub world
      mutation out of `MenteesMod` into `StyleTestMobActions`.
- [x] Add a smoke check that startup/shutdown registration behavior still works.

### 5. Dev Harness And Proof Runtime

- [x] Move proof runner registration into `MotmProofRuntime`; keep low-level
      Hytale mutation helpers in `MenteesMod` until harness parity allows
      deeper extraction.
- [x] Make proof ids, proof runners, and proof evidence contracts discoverable
      from one catalog.
- [x] Add a scenario catalog under `scripts/scenarios/` or equivalent.
- [x] Teach `run-agent-observability-baseline.ps1` to execute scenario plans
      instead of relying only on hard-coded command sequences.
- [x] Delete or archive hard-coded scenario flows once equivalent catalog
      scenarios are validated; setup/cleanup command choreography now lives in
      scenario JSON and the runner executes catalog phases.
- [x] Validate scenario files against current style/ability/proof catalogs.
- [x] Add known-good scenario examples for projectile, field, terrain, summon,
      transformation, HUD, and command-only changes.
- [x] Keep raw evidence, manifests, provenance, indexes, and rerunnable commands
      as stable harness contracts.

### 6. Agent Instructions And Drift Guards

- [x] Update `AGENTS.md` with the new extension rails once implemented.
- [x] Update README with generated or verified known-good commands.
- [x] Add a short "when adding a feature" flow:
      content shape -> runtime family -> scenario/proof -> harness evidence.
- [x] Replace stale examples that reference invalid style/proof ids.
- [x] Add architecture checks for forbidden patterns:
      new generic ability-id branches, new pending maps in `MenteesMod`, new
      direct inventory mutation outside `MotmInventoryOps`, new unvalidated
      scenario ids, and new screenshot-only acceptance language.
- [x] Add a compatibility register and architecture check requiring explicit
      removal rules for retained legacy/compatibility exceptions.
- [x] Expand stale legacy checks to flag duplicate old/new behavior owners and
      comments that tell agents to extend deprecated paths.
- [x] Make architecture check output agent-readable with exact file/line and a
      suggested owner package.
- [x] Add a `GameplayPlaybackManager` raw collection ratchet so new private
      runtime `List`/`Map`/`Set` fields fail architecture checks and must move
      into runtime-family state owners instead.

### 7. Regression And Completion Gates

- [x] `./gradlew build` succeeds with the installed Hytale server jar.
- [x] Java unit tests cover validators, registries, task processors, and pure
      runtime specs.
  - [x] Validator tests cover success/failure cases for `AbilityShape`.
  - [x] Pure projectile runtime spec tests cover Magma Sling, volley cadence,
        burst defaults, and speed clamping.
  - [x] Active projectile state tests cover profile-backed state, defensive
        vector copying, travel advancement, hit tracking, and visual refresh
        scheduling.
  - [x] Projectile runtime-state tests cover active projectile collection
        ownership, owner removal cleanup, and processed-projectile removal.
  - [x] Projectile visual runtime tests cover missing-visual sentinel state.
  - [x] Projectile tick runtime tests cover invalid-owner cleanup, missing
        store/player cleanup, pre-activation refresh, travel advancement,
        non-piercing impact/despawn, piercing traversal ordering, and
        leave-visual impact handling.
  - [x] Visual proxy runtime-state tests cover proxy tracking, removal,
        contains checks, and immutable snapshots.
  - [x] Field runtime spec tests cover persistent classification, dimensions,
        timing, pulse-damage ratios, pull-lift decisions, terrain policy
        resolution, restore reasons, and origin policies.
  - [x] Recent field-origin tests cover defensive vector copying and freshness
        windows for stable field-origin guards.
  - [x] Field origin runtime-state tests cover Iron Wall and caster-centered
        origin maps, implausible jump reuse, expiration-window acceptance, and
        reset cleanup.
  - [x] Active field state tests cover geometry/state ownership, scheduling, and
        defensive vector/list handling.
  - [x] Field runtime-state tests cover active field collection ownership,
        owner/ability removal cleanup, processed-field removal, and sinkhole
        burial entries including empty engaged fields.
  - [x] Field visual runtime tests cover missing-visual sentinel state and
        immutable visual refs.
  - [x] Field tick runtime tests cover invalid owner cleanup, expiry cleanup
        ordering, pre-activation refresh, active wait-before-pulse behavior,
        pulse/support/suffocation order, sinkhole engagement, next-pulse
        scheduling, and missing owner-player cleanup.
  - [x] Channel and line-control state tests cover pulse scheduling ownership.
  - [x] Channel activation runtime tests cover channel/line-control creation
        and missing-input rejection.
  - [x] Channel runtime-state tests cover replace-per-player behavior,
        owner/ability cleanup, and processed channel/line-control removal.
  - [x] Self-effect and player-anchor tests cover repeated self-effect timing,
        expiry checks, anchor expiry, and defensive anchor-position copying.
  - [x] Self activation runtime tests cover self-effect creation, player-anchor
        creation, defensive anchor copying, and missing-input rejection.
  - [x] Self runtime-state tests cover anchor replacement, self-effect
        replacement, owner cleanup, and processed anchor/effect removal.
  - [x] Terrain runtime spec tests cover movement trail classification,
        personal aura classification, geometry decisions, trail interpolation,
        and temporary selection lifetime.
  - [x] Active terrain state tests cover moving trail scheduling/copying,
        stacking column staging, and terrain runtime collection ownership.
  - [x] Terrain activation runtime tests cover moving-trail, Lapidary gem, and
        stacking-column construction plus missing-input rejection.
  - [x] Terrain tick runtime tests cover expired-selection restoration,
        moving-trail readiness/current-owner gating, missing owner positions,
        missing block-type cleanup, stationary rechecks, surface stamp
        placement/scheduling, and stacking-column staging.
  - [x] Active Lapidary gem tests cover health mutation, expiry, label update
        ownership, and defensive center copying.
  - [x] Lapidary gem runtime-state tests cover active gem collection ownership,
        owner cleanup, processed removal, and gem-anchor lookup.
  - [x] Lava hazard runtime-state tests cover magma protection expiry, movement
        boost flags, velocity boost copying/removal, and per-player cleanup.
  - [x] Armed stomp tests cover airborne observation transitions and expiry.
  - [x] Stomp runtime-state tests cover arming, lookup, snapshot iteration,
        compare-and-remove, replacement, and owner cleanup.
  - [x] Buried-victim tests cover previous-gravity restoration state and expiry.
  - [x] Summon runtime spec tests cover named summon roles, model mappings,
        ranged/melee behavior, cadence, hatch delays, attack tokens, chase
        ranges, and damage multipliers.
  - [x] Active summon state tests cover timing mutation, buff windows, attack
        scheduling, and clone expiry.
  - [x] Summon runtime-state tests cover owner indexing, immutable owner
        snapshots, despawn cleanup, and processed summon removal.
  - [x] Summon tick runtime tests cover invalid/expired summon removal,
        think gating, missing store/owner cleanup, hatch delay scheduling,
        awaken ordering, invalid owner-ref cleanup, owner-follow behavior,
        target chase/retreat decisions, attack readiness, and missing-position
        handling.
  - [x] Summon attack-effect runtime tests cover tank shield/pull, Snow Imp and
        Skeleton specials, Treant owner shield, Void Spawn splash damage,
        buff-window/hatchling specials, clone blind, and Swamp Monster splash
        routing.
  - [x] Summon attack runtime tests cover invalid-target no-ops, incoming/
        absorb ordering, buffed damage, zero-or-lower damage callback skipping,
        damage/post-damage/lifesteal/impact/effect ordering, attack scheduling,
        clone pre-hit movement, resolved logging, and clone post-strike expiry.
  - [x] Summon buff runtime tests cover no-active and no-in-range summaries,
        range filtering, duration and buff-window extension, attack-soon
        scheduling, hatchling command gating, invalid target handling, and
        commanded-strike summaries.
  - [x] Summon target runtime tests cover locked-target reuse, expired-lock
        searching, tank/clone/default search anchors and radii, owner-position
        fallback, lock updates, and missing-target lock clearing.
  - [x] Summon movement runtime tests cover owner-follow placement, approach
        max/min travel clamps, retreat max/min travel clamps, clone beside-target
        owner approach, fallback beside-target direction, and null-input no-ops.
  - [x] Pure follow-up runtime spec tests cover Alloy Enhancement, excluded
        shield-only buffs, generic attack-buff follow-ups, Refraction
        splash/riders, and non-buff rejection.
  - [x] Active weapon follow-up state tests cover bound item state and use
        countdown behavior, including Alloy first-item binding and switched-item
        rejection.
  - [x] Weapon follow-up runtime-state tests cover player-indexed put/get,
        remove, contains, entries, and invalid request handling.
  - [x] Weapon follow-up durability-restorer tests cover safe handling of
        missing player/item inputs.
  - [x] Weapon follow-up hit-math tests cover base damage, attack modifiers,
        passive/incoming multiplier math, Alloy native-hit scaling, and splash
        damage checks.
  - [x] Weapon follow-up hit-effect orchestration tests cover rider order,
        payoff order, and native Alloy secondary-rider behavior.
  - [x] Weapon follow-up visual-effect tests cover Alloy effect id application,
        removal result propagation, and missing-hook safety.
  - [x] Weapon follow-up splash-runtime tests cover target filtering,
        multiplier/absorb ordering, damage/post-damage ordering, secondary rider
        ordering, impact ordering, and no-op inputs.
  - [x] Weapon follow-up primary-hit runtime tests cover damage/post-damage/
        lifesteal/impact/rider order, zero-damage impact behavior, and missing
        hook safety.
  - [x] Weapon follow-up native-Alloy runtime tests cover native damage
        mutation ordering, use exhaustion cleanup, switched-item rejection,
        tool-use cleanup, durability-restoration messaging, and no-op inputs.
  - [x] Weapon follow-up lifecycle runtime tests cover null cleanup, active
        retention, Alloy current-store mutation deferral, duration expiry,
        unavailable-player cleanup, and use-exhaustion end reasons.
  - [x] Combat runtime-state tests cover ability-kill de-duplication and recent
        shock expiry cleanup.
  - [x] Transformation runtime spec tests cover known form profiles, fallback
        profile behavior, visual-effect lookup, capability kind, owner refresh
        tokens, owner shield amount, and grounded-ending policy.
  - [x] Active transformation state tests cover profile-backed values and
        defensive cloning of owner position tracking.
  - [x] Transformation runtime-state tests cover player lookup, pulse
        scheduling, ability-specific removal, and processed transformation
        cleanup.
  - [x] Transformation tick runtime tests cover invalid/expired owner cleanup,
        pulse waiting, missing player/end-condition/position cleanup, owner
        refresh ordering, locomotion ordering, form pulse dispatch, and
        next-pulse scheduling.
  - [x] Transformation effect runtime tests cover kind-specific pulse plans,
        locomotion movement thresholds, movement-factor clamping, secondary
        rider/knockback routing, T-Rex radius selection, and Triceratops
        charge-shield ordering.
  - [x] Runtime task tests cover queue coalescing, snapshot cleanup, and
        processor ordering.
  - [x] Runtime task tests cover invalid request rejection, immutable pending
        views, and explicit completion/cancel behavior so agents cannot mutate
        task queues outside the named owner.
  - [x] Proof runtime tests cover catalog/runner parity.
  - [x] Registry and task lifecycle tests cover processor ordering plus
        unavailable-player skip/completion behavior for representative task
        processors.
  - [x] Harness-backed runtime-task assertions require
        `runtime_task_executed` evidence for scenario-driven style-test mob
        spawn/count/clear commands, while content validation now checks
        `expectedEvidence` source/type shape. Focused evidence:
        `RuntimeTaskProcessorLifecycleTest` plus
        `scripts/run-agent-observability-baseline.ps1` runtime-task expectation
        checks.
- [x] PowerShell parser checks pass for scripts.
- [x] Static content audits pass.
- [x] Static content audits pass through `validateContentShape`.
- [x] Architecture checks pass.
- [x] Agent observability baseline passes after installing the new jar.
      Evidence: `mac-internal-baseline-20260525-1441` on macOS after restarting
      Hytale onto the corrected internal tester jar.
- [x] Runtime-family scenarios pass for projectile, field, terrain, summon,
      transformation, and follow-up behavior.
      Evidence: `mac-terra-projectile-magma-sling-20260525-1442`,
      `mac-terra-field-iron-wall-20260525-1442`,
      `mac-hydro-summon-snow-imp-20260525-1442`,
      `mac-aero-transformation-smoke-form-20260525-1442`, and
      `mac-terra-followup-alloy-enhancement-20260525-1452`.
- [x] Runtime command failure diagnostics produce a run-local
      `dev-command-diagnostic.json`/`.md` with Hytale process state, latest
      client log tail, inbox/outbox paths, and recovery steps.
      Smoke evidence:
      `/tmp/motm-dev-command-diagnostic-test/dev-command-diagnostic.md`.
- [x] Final PR summary reports run ids, scenario ids, evidence streams, and any
      remaining `UNKNOWN` areas.
      Evidence ledger:
      `docs/runtime-architecture-final-evidence-summary.md`.

## Example Agent Flows

### Flow A: Add A Projectile Ability Variant

```
feature request
  └─▶ inspect ability JSON and AbilityShape validation
       └─▶ add projectile profile fields or handler override
            └─▶ add/update projectile scenario
                 └─▶ build + content validation + architecture check
                      └─▶ run scenario
                           └─▶ inspect projectile spawn/travel/hit/despawn evidence
```

Expected ergonomic result:

- The agent should find `runtime.ability.projectile` without searching a 9k-line
  playback manager.
- A new `if ("new_ability".equals(...))` in generic runtime code should fail the
  architecture check.
- If the scenario references an invalid ability or proof id, validation should
  fail before launch.
- The final answer should cite raw projectile evidence, not infer success from a
  screenshot.

### Flow B: Change A Persistent Field Tick Effect

```
feature request
  └─▶ AbilityShape identifies field primitive
       └─▶ edit field runtime spec or field handler
            └─▶ add field pulse evidence if missing
                 └─▶ run field scenario
                      └─▶ verify activation, pulse, target filtering, cleanup
```

Expected ergonomic result:

- The agent should extend `runtime.ability.field`, not projectile or transform
  code.
- Cleanup evidence should be part of the scenario. A feature is not complete if
  the field works but leaves temporary state behind.
- The harness should expose enough server truth to separate "field visual
  appeared" from "field applied correct runtime behavior."

### Flow C: Add Temporary Terrain Behavior

```
feature request
  └─▶ content validator checks terrain_effect support
       └─▶ terrain runtime owns placement and restoration
            └─▶ scenario asserts placement and original block restore
                 └─▶ run bundle keeps raw before/after evidence
```

Expected ergonomic result:

- Terrain mutation should not be mixed into field/projectile code.
- The scenario should fail if cleanup cannot be proven.
- Architecture checks should reject ad hoc terrain selection queues outside the
  terrain runtime owner.

### Flow D: Add A Dev Harness Command

```
feature request
  └─▶ add typed RuntimeTask if command mutates world/player state
       └─▶ route through dev harness command owner
            └─▶ emit control + task evidence
                 └─▶ add scenario step using command
                      └─▶ query evidence by run id
```

Expected ergonomic result:

- The agent should not add another pending map to `MenteesMod`.
- Command authorization should be inherited from the dev harness owner.
- The harness should show command received, task accepted, task executed, and
  resulting server truth.

### Flow D2: Prepare A Headless-Friendly Windows Harness Run

```
remote Windows agent
  └─▶ run setup-agent-workstation.ps1
       └─▶ run CLI Hytale session preflight
            ├─ valid launcher/session state ──▶ build/install/run scenario
            └─ missing auth/session state ───▶ write exact diagnostic + stop
```

Expected ergonomic result:

- The agent should not use Computer Use, image recognition, browser clicking, or
  any other GUI-control dependency to get Hytale into a scenario-ready state.
- Authentication must be treated as an official-launcher/session prerequisite,
  not as something the harness fakes, scrapes, or bypasses.
- Setup scripts should detect the Hytale root, installed jar, Java 25, Gradle
  wrapper, internal jar install path, command bridge readiness, latest client log
  path, and whether the official launcher/session state appears usable.
- When auth/session state is missing, the script should fail with an actionable
  diagnostic bundle that a human or remote agent can follow. It should not
  continue into screenshot-only or inferred validation.
- Once the user has completed any required official launcher login/setup, the
  same CLI command should be rerunnable without changing code.

### Flow E: Modify Ability Data Only

```
feature request
  └─▶ surgical JSON edit
       └─▶ run content validator and generated catalog check
            └─▶ run targeted scenario if runtime behavior changes
                 └─▶ report PASS / FAIL / UNKNOWN
```

Expected ergonomic result:

- The agent should not regenerate protected style JSON wholesale.
- The validator should catch unknown tokens or unsupported field combinations.
- Docs and known-good command examples should stay aligned with generated
  catalogs.

## Definition Of Fully Complete

This architecture is fully implemented when an agent can add or modify a
representative feature in each major runtime family without touching
`MenteesMod` or adding generic ability-id branches, and when the automated gates
plus observability scenarios prove the behavior from raw evidence.

Completion means:

- Feature location is obvious from package and registry names.
- Invalid data and invalid scenarios fail early.
- Bad local-patch patterns produce actionable architecture-check errors.
- Obsolete paths have been deleted, not merely bypassed.
- Remaining compatibility facades are delegation-only and have explicit removal
  gates.
- Harness scenarios are easy to add and are trusted because raw evidence is
  retained.
- Launcher/auth readiness can be checked from the CLI on Windows and macOS. A
  missing official session produces an actionable diagnostic instead of requiring
  local GUI automation or weakening verification.
- The final PR is created only after the migration checklist is closed, static
  rails and targeted tests pass, the internal jar is installed, scenario evidence
  is collected or explicitly marked `UNKNOWN`, and the final summary names the
  run ids, evidence streams, deleted old paths, and remaining compatibility
  exceptions.
- The final verification loop is natural for an agent:

```
read intent
  └─▶ find owner
       └─▶ extend descriptor/runtime/scenario
            └─▶ run checks
                 └─▶ run harness
                      └─▶ inspect raw evidence
                           └─▶ report PASS / FAIL / UNKNOWN
```
