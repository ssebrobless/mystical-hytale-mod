# Phase 9 Stash Port Audit - 2026-05-29

## Scope

Audited `stash@{0}`:

```text
On feat/phase9-remaining-styles: partial-phase9-feature-work-before-main-harness-cleanup
```

The stash was created before the current runtime architecture and observability
harness baseline. It does not apply cleanly to `main`, and it contains a mix of
old harness scripts, old Terra tuning, and implementation ideas that have since
been moved into runtime owners.

## Decision

Do not apply the stash wholesale. Keep it as historical reference only until the
team explicitly decides to drop it. The useful runtime ideas are already present
on `main` through newer architecture-owned implementations, while the stale
pieces would reintroduce removed scripts or older ability behavior.

## Port Matrix

| Stash area | Current status on `main` | Action |
| --- | --- | --- |
| `scripts/audit-phase9-class.ps1` | Removed by harness consolidation. Current canonical path is `scripts/run-agent-observability-baseline.ps1` plus scenario JSON under `scripts/scenarios/`. | Do not restore. |
| `scripts/capture-evidence.ps1` | Removed with screenshot-heavy legacy flow. Current evidence path keeps raw observability streams and diagnostics. | Do not restore. |
| `scripts/load-world.ps1` | Removed after it became stale around `onPlayerConnect`/load detection. Current launcher/world entry is handled by the harness and official-launcher flow. | Do not restore. |
| `scripts/send-input.ps1` | Useful hunks are already present: title-bar focus, clipboard slash + `T` chat open, and direct `V` third-person toggle. | No action. |
| `MenteesMod` dev command inbox polling | Superseded by `com.motm.command.MotmDevCommandInbox` and `MotmDevCommandInboxProcessor`; `MenteesMod.processDevCommandInbox` is delegation-only per architecture ratchet. | Do not port manager-local polling. |
| `MenteesMod` weapon-hit and forced Stomp helpers | Superseded by `StyleTestCommandActions` delegation methods exposed through `MenteesMod`. | No action. |
| `MotmCommand` dev audit/test helpers | Already present through `MotmDevCommandRouter` and `MotmCommand` handlers, including audit marker, weapon-hit, and stomp/jump landing aliases. | No action. |
| `GameplayPlaybackManager` temporary terrain state | Superseded by `runtime.ability.terrain` owners: `TerrainRuntimeState`, `TerrainActivationRuntime`, `TerrainPlacementHytaleAdapter`, `TerrainHytaleAdapter`, and `TerrainTickRuntime`. | Do not port manager-local terrain lists/helpers. |
| `GameplayPlaybackManager` forced Stomp landing and weapon follow-up | Already present on `main`. | No action. |
| `HytaleAssetResolver` proof constants/routes | Already present on `main` with additional newer routes such as alloy impact, magma sling travel, lava pool field, and sand dust. | No action. |
| `terra_styles.json` | Mixed. Some stash edits are already present, but others are stale relative to later concept locks. | Do not port directly. |

## Terra Data Notes

Useful stash concepts already present on `main`:

- Quake `aftershock` radius is `8`, resource cost is `0`, and visual overlay is
  ground cracks.
- Bloom `nightshade` is a terrain-grounding projectile line rather than a cone.
- Bloom `cacti_cluster` uses a cactus projectile / delayed AoE pattern.
- Soil `burrow` is a short forward dash.
- Sand `sandstorm` is a 10 second self-centered toggle with a 2 second toggle
  cooldown.
- Sand `dust_devil` is a self-centered dash.

Stale stash concepts that must not overwrite current intent:

- Metal `iron_wall` in the stash regressed to a smaller `2x2` wall. Current
  concept is a grounded `3x4` wall that pushes overlapping enemies.
- Self Petrification `gargoyle` in the stash had a 6 second cooldown. Current
  concept uses 7 seconds, 35 percent healing, damage reduction, and untargetable
  statue behavior.
- Older terrain/block placement in `GameplayPlaybackManager` predates the
  runtime terrain owner extraction and would violate the current architecture
  boundary.

## Verification Performed

Static inspection commands used:

```powershell
git stash list
git diff --stat 'stash@{0}^1' 'stash@{0}'
git diff 'stash@{0}^1' 'stash@{0}' -- <file>
rg "TemporaryTerrainSelection|ActiveMovingTerrainTrail|ActiveStackingColumn" src/main/java scripts docs
rg "DevCommandInbox|dev command inbox|runStyleTestWeaponHit|forceStyleTestStompLanding" src/main/java scripts docs
rg "audit-phase9-class|capture-evidence|load-world|run-style-observability-sweep" scripts docs AGENTS.md README.md
```

The result is a documentation-only closeout of the stash. No runtime code or
protected style JSON was changed by this audit.
