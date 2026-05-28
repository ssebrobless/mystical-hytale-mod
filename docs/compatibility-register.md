# Compatibility Register

Updated: 2026-05-25

Compatibility exists to protect active users, saved data, public commands, and
trusted harness contracts while MOTM moves to cleaner owners. It is not a place
to keep old internal architecture. If an entry no longer has an active consumer,
the next refactor should delete it instead of extending it.

This register is deletion infrastructure, not a museum. An entry records why an
old path cannot be deleted today and what evidence will make it deletable. It
does not grant permission to add feature logic, broaden scope, preserve old
package shape, or keep duplicate sources of truth.

An entry is a failing deletion gate, not a design endorsement. The preferred
steady state is no exception at all. Agents should treat this file as a queue of
cleanup obligations and should remove entries as soon as their removal gates are
met.

Compatibility preserves supported behavior contracts, not old code. If the
consumer is internal-only and the behavior can be proven through tests, static
rails, generated catalogs, or the observability harness, the right outcome is a
fresh-slate replacement plus deletion of the old path. Do not add an entry here
just to make a refactor feel safer.

The register is also an implementation allowlist. Java files that mention
legacy, compatibility, deprecation, or old paths must appear here as an
implementation boundary. If a file cannot be named under an active exception,
delete the old code instead of teaching agents to route around it.

## Removal Rules

```
╔════════════════════════════════════════════════════════════════════╗
║ Compatibility Exception                                           ║
╠════════════════════════════════════════════════════════════════════╣
║ active consumer + named owner + preferred replacement              ║
║   └─▶ verification evidence                                        ║
║        └─▶ removal gate                                            ║
║             └─▶ delete when gate is met                            ║
╚════════════════════════════════════════════════════════════════════╝
```

- New compatibility code must be listed here in the same PR that introduces it.
- Compatibility code may delegate to new owners; it must not accumulate new
  feature behavior.
- Compatibility code that needs branching, policy decisions, scheduling,
  mutation ownership, or ability/runtime semantics belongs in the new owner
  instead. A registered compatibility facade should be thin and boring.
- Compatibility code may not exist for speculative fallback, merge comfort, or
  vague "legacy safety." An active consumer must be named before code is kept.
- Internal architecture is not an active consumer. Old manager layout, old
  method names, old queues, old lookup tables, and old data flow should be
  deleted unless they protect saved data, public commands/APIs, or harness/tool
  contracts.
- Do not register compatibility for stale tests, stale docs, stale branches,
  old package names, old call-site convenience, or agent uncertainty. Update the
  caller/documentation/test to the new owner instead.
- Refactors should migrate callers first, prove parity through tests or harness
  evidence, then delete the old path in the same PR whenever possible.
- A compatibility entry must shrink over time. If a PR touches an exception and
  does not remove it, the PR should either narrow the boundary, improve the
  removal evidence, or explain the remaining blocker.
- If an agent cannot identify the active consumer, it should delete the code or
  stop and document why deletion would be unsafe.
- Compatibility entries must name the implementation boundary. Broad subsystem
  labels are not enough; future agents need the exact file/package where support
  is allowed and where it is not.
- Public method names may remain temporarily as delegation-only facades, but old
  implementation concepts should not survive under neutral names.

## Active Compatibility Exceptions

### Plugin Data Directory Migration

- Owner: `MotmPluginDataDirectories` startup data migration.
- Implementation boundary:
  `src/main/java/com/motm/config/MotmPluginDataDirectories.java`.
- Consumer: existing installs that may still have MOTM data in the old
  asset-scanned mod directory.
- Preferred replacement: operational data under the current plugin data path.
- Verification evidence: `MotmPluginDataDirectoriesTest` verifies existing
  files are copied once, scanner-safe manifest handling still works, and normal
  runtime data resolves to the operational path.
- Removal gate: no supported release can reasonably contain the old directory
  layout, or a one-way migration release has been shipped and verified.

### Legacy Spellbook Item Migration

- Owner: spellbook restoration/update flow in `SpellbookInventoryKit`, with
  item identity owned by `SpellbookInventoryItems`.
- Implementation boundary:
  `src/main/java/com/motm/resource/SpellbookInventoryKit.java` and
  `src/main/java/com/motm/resource/SpellbookInventoryItems.java`; public
  plugin methods in `src/main/java/com/motm/MenteesMod.java` are
  delegation-only entrypoints for existing command/runtime callers.
- Consumer: player inventories that still contain the old spellbook item.
- Preferred replacement: current casting focus/spellbook item behavior.
- Verification evidence: scenario or manual harness run starts with a legacy
  item, migrates it, preserves inventory intent, and does not create duplicates.
- Removal gate: saved inventory compatibility is no longer required for the old
  item id.

### Resource Cost Save Compatibility

- Owner: `AbilityData` parsing and content-shape validation.
- Implementation boundary: `src/main/java/com/motm/model/AbilityData.java`,
  `src/main/java/com/motm/model/StyleData.java`,
  `src/main/java/com/motm/MenteesMod.java`, and
  `src/main/java/com/motm/command/MotmCommand.java`.
- Consumer: authored JSON and saved/configured ability data that may still
  contain historical resource-cost fields.
- Preferred replacement: no-resource casting semantics with cooldown/style
  state as the active runtime gate.
- Verification evidence: validator accepts existing content, runtime casting
  does not spend resource costs, and generated catalogs make ignored legacy
  fields visible.
- Removal gate: all protected style JSON and external references have removed
  resource-cost fields, and the validator has a replacement schema version.

### Free-Cast Test Protection Command Text

- Owner: `/motm` command compatibility messaging.
- Implementation boundary: `src/main/java/com/motm/command/MotmCommand.java`.
- Consumer: existing operator workflows that know the old free-cast testing
  phrase.
- Preferred replacement: explicit dev harness proof/scenario commands.
- Verification evidence: command help points users toward proof/scenario flows,
  and observability scenarios cover the same setup behavior.
- Removal gate: command help and agent instructions no longer reference the old
  phrase, and harness scenarios replace the workflow.

### HUD Legacy Icon Slot Suppression

- Owner: `MotmStatusHud` rendering compatibility.
- Implementation boundary: `src/main/java/com/motm/ui/MotmStatusHud.java`.
- Consumer: UI state/layout code that previously expected icon-slot widgets.
- Preferred replacement: text-first status summary rendering.
- Verification evidence: HUD scenario or visual probe confirms old slot widgets
  stay hidden and current status text remains accurate.
- Removal gate: HUD renderer no longer constructs or receives the old icon-slot
  widgets.

### Player Craft Event Perk Stamping

- Owner: crafted-item perk stamping in `RuntimePerkManager`.
- Implementation boundary:
  `src/main/java/com/motm/lifecycle/MotmLifecycleRegistrar.java`,
  `src/main/java/com/motm/MenteesMod.java`, and
  `src/main/java/com/motm/manager/RuntimePerkManager.java`.
- Consumer: Blacksmith and Toolsmith perks need the current player-scoped craft
  output hook so crafted armor/tools can receive permanent item metadata.
- Preferred replacement: the next supported Hytale player crafting/output API
  that exposes the crafter and crafted stack without using the deprecated
  `PlayerCraftEvent`.
- Verification evidence: perk/runtime proof or manual crafting-table flow shows
  Blacksmith armor receives `Blacksmith Perk` metadata and Toolsmith tools or
  weapons receive `Toolsmith Perk +25% Durability` metadata plus 125% max
  durability.
- Removal gate: Hytale exposes and MOTM verifies a non-deprecated player craft
  output hook, then the event registration and imports are replaced in the same
  change.

### MenteesMod Mutable State Ratchet

- Owner: architecture check and runtime state refactor plan.
- Implementation boundary: `scripts/check-architecture.ps1` and
  `docs/agent-friendly-architecture-scaffolding.md`.
- Consumer: live runtime state still anchored in the plugin shell, such as
  online player handles, HUD instances, active style/proof selections, free-cast
  state, and observability timing.
- Preferred replacement: named owners under `runtime.state`, `runtime.task`, or
  feature-specific runtime packages, with `MenteesMod` reduced to lifecycle and
  delegation.
- Verification evidence: extracted owners preserve tick ordering, cleanup,
  player disconnect behavior, heartbeat summaries, and scenario evidence.
- Current ratchet: `MenteesMod` may contain zero mutable runtime
  collection fields; free-cast state, spellbook input debounce state, and live
  style-test state have moved to named runtime-state owners. Temporary proof
  cleanup queues, target-health tracking, and perk trigger bindings have also
  moved to named runtime-state owners. Runtime player handles and custom HUD
  handles are also behind named shell-state owners.
- Removal gate: the remaining `MenteesMod` mutable state is either moved to
  named owners or proven to be plugin-shell lifecycle state, then the
  architecture check ratchet is lowered again.
