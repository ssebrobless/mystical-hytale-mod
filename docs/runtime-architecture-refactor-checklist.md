# Runtime Architecture Refactor Checklist

Updated: 2026-05-25

This checklist turns `docs/hytale-complete-api-alignment-audit.md` into the
single implementation record for the full cleanup PR. The verification harness
branch has landed on `main`; keep this refactor rebased on current `main`.

```
╔════════════════════════════════════════════════════════════════════╗
║ Target Shape                                                      ║
╠════════════════════════════════════════════════════════════════════╣
║ MenteesMod                                                        ║
║   ├─ lifecycle + manager wiring                                   ║
║   ├─ runtime task queue owner                                     ║
║   ├─ command/harness surfaces                                     ║
║   └─ narrow compatibility facade                                  ║
║ Runtime services                                                  ║
║   ├─ runtime task queue                                           ║
║   ├─ inventory ops                                                ║
║   ├─ command auth/dev dispatch                                    ║
║   ├─ proof/scenario registry                                      ║
║   └─ gameplay playback slices                                     ║
╚════════════════════════════════════════════════════════════════════╝
```

## Checklist

- [x] Runtime task queue and mutation context
  - [x] Replace ad hoc pending sets/maps/queues in `MenteesMod` with a named
        runtime task owner.
  - [x] Preserve existing ordering and one-shot/coalescing behavior.
  - [x] Expose queue sizes to observability.
  - [x] Record failures with enough context to debug the task that failed.
- [x] Inventory operations
  - [x] Add one helper for grant, remove-slot, remove-count, and set-slot
        operations.
  - [x] Verify current jar semantics by compiling against the installed Hytale
        jar rather than assuming old docs are exact.
  - [x] Replace direct inventory mutations in `MenteesMod` and
        `GameplayPlaybackManager`.
  - [x] Emit compact observability/log evidence for failed mutations.
- [x] Command authorization and dev dispatch
  - [x] Introduce a small command auth policy for public, dev, and future admin
        surfaces.
  - [x] Keep public player commands available without dev tools.
  - [x] Move `/motm dev ...` routing out of the main command class or behind a
        clearly named collaborator.
  - [x] Keep file-backed harness commands gated to internal test builds and
        `dev_tools_enabled`.
- [x] Proof/scenario registry
  - [x] Replace the large proof switch with a registry/list that can be extended
        by feature work.
  - [x] Make available proof ids discoverable from help text and observability.
  - [x] Keep raw run evidence and trace ids unchanged.
- [x] Lifecycle registration cleanup
  - [x] Verify local API return types for event, command, and system
        registration.
  - [x] Store/unregister handles where the API supports it.
  - [x] Keep packet watcher cleanup unchanged or stronger.
- [x] Gameplay playback slicing
  - [x] Split the most mechanically separable playback concerns out of
        `GameplayPlaybackManager`.
  - [x] Start with inventory/held-item helpers and shared geometry helpers
        before attempting ability-family splits.
  - [x] Preserve the public `GameplayPlaybackManager` API until tests prove the
        slice is stable.
- [x] Documentation and agent workflow
  - [x] Keep `AGENTS.md`, README, and observability docs aligned with the final
        refactor shape.
  - [x] Ensure future feature requests direct agents to extend the harness when
        evidence is missing.
- [ ] Regression battery
  - [x] PowerShell parser checks for setup/harness scripts.
  - [x] `git diff --check`.
  - [x] `scripts/setup-agent-workstation.ps1`.
  - [x] Gradle wrapper build/install with `--warning-mode all`.
  - [x] Static data audits such as `scripts/audit-no-resource.ps1`.
  - [ ] Agent observability baseline in Hytale after restarting the game onto
        the newly installed jar.

## Completion Bar

The refactor is complete only when the code structure reflects the target shape,
the old ad hoc ownership is gone or intentionally isolated behind compatibility
facades, and the regression battery proves the branch still builds, installs,
and preserves the verification harness contracts.
