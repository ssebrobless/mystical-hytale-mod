# Codex Test/Audit Optimization Notes - 2026-05-22

## Why This Exists

Recent Phase 9 audits proved that commands and log capture work, but several
visual checks were weak because the player was still near the creative spawn,
sometimes facing walls, portals, or dense geometry. Future style audits must
start from the flatlands arena whenever the ability does not specifically need
spawn geometry.

```text
Cold launch / load world
  +-- ensure flatlands visual gate
      +-- spawn close + distant targets
          +-- cast ability
              +-- capture screenshot
              +-- tail log
              +-- write PASS/FAIL + residuals
```

## Current Plan Index

```text
Master operations
+-- CODEX_AUTONOMOUS_IMPLEMENTATION_AND_TEST_PLAN_2026-05-21.md
+-- CODEX_HARNESS_EXPANSION_PLAN_2026-05-21.md
+-- CLAUDE.md

Phase work
+-- CODEX_IMPLEMENTATION_PLAN_2026-05-13.md
+-- CODEX_PHASE5_FIX_2026-05-13.md
+-- CODEX_SINKHOLE_VISUALS_FIX_2026-05-13.md
+-- CODEX_REALIGNMENT_PLAN_2026-05-13.md
+-- ABILITY_COMPLETION_CHECKLIST.md
+-- SPELLBOOK_UI_SPEC.md

Live residual ledger
+-- CODEX_PHASE9_RESIDUALS.md
```

Governing order:

```text
Harness / testing / evidence -> autonomous + harness plans
Phase 5                    -> Phase 5 fix docs override parent
Phase 6 spellbook          -> revised SPELLBOOK_UI_SPEC.md
Phase 9 visual identity    -> realignment plan + asset discovery output
Hard safety rules          -> CLAUDE.md always wins
```

## Research Summary

Online sources confirm the cautious approach:

- Hytale modding is server-side first, with plugins, data assets, art assets,
  and save/prefab content as the core categories. The official status post also
  warns that tools/docs are uneven and that inspecting implementation is a valid
  unblock path while docs catch up.
  Source: https://hytale.com/news/2025/11/hytale-modding-strategy-and-status
- Custom UI is server-controlled and asset-driven. Java builds UI commands with
  `UICommandBuilder`; client `.ui` markup defines the visual tree; interaction
  events flow back to Java.
  Source: https://hytalemodding.dev/pl-PL/docs/official-documentation/custom-ui
- Entity effects can be applied through `EffectControllerComponent`, use
  `EntityEffect` assets, support durations/overlap behavior, and can include
  application particles/sounds/animation. Treat the page as unofficial unless
  the same API is confirmed in the local jar.
  Source: https://hytale-docs.pages.dev/modding/systems/entity-effects/

Local jar inspection is the final authority for this project. Confirmed in the
installed `HytaleServer.jar`:

```text
Custom UI
+-- com.hypixel.hytale.server.core.entity.entities.player.pages.CustomUIPage
+-- com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage
+-- com.hypixel.hytale.server.core.entity.entities.player.pages.PageManager
+-- com.hypixel.hytale.server.core.ui.builder.UICommandBuilder
+-- com.hypixel.hytale.server.core.modules.interaction.interaction.config.server.OpenCustomUIInteraction

Interaction
+-- com.hypixel.hytale.server.core.entity.InteractionContext

Movement / placement
+-- com.hypixel.hytale.server.core.modules.entity.teleport.Teleport
+-- com.hypixel.hytale.server.core.modules.entity.component.TransformComponent
+-- com.hypixel.hytale.server.core.modules.physics.component.Velocity

Combat
+-- com.hypixel.hytale.server.core.modules.entity.damage.Damage
+-- com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems
```

## Asset Discovery Rule

Do not invent particle `SystemId`s, model paths, or role names. Use this order:

```text
1. Existing HytaleAssetResolver constants
2. audits/harness/assets/<run>/keyword-catalog.md
3. Assets.zip entry path from discover-hytale-assets.ps1
4. HytaleServer.jar class discovery for API symbols
5. In-game /showcase or direct runtime log proof
```

`scripts/discover-hytale-assets.ps1` scans the installed `Assets.zip` and
`HytaleServer.jar` into:

```text
audits/harness/assets/<timestamp>/
+-- report.md
+-- particles-all.txt
+-- effects-candidates.txt
+-- models-all.txt
+-- ui-all.txt
+-- api-classes-key.txt
+-- keyword-catalog.md
+-- resolver-assets.txt
+-- resolver-assets-missing-from-zip.txt
```

## Flatlands Gate

`scripts/ensure-flatlands.ps1` is now the visual gate before style audits. It:

```text
1. focuses Hytale
2. captures a screenshot
3. samples sky / grass / center-obstruction regions
4. PASSes only if the view looks like the flatlands arena
5. if not already there, attempts a bounded portal route
6. writes report.md and screenshots under audits/harness/ensure-flatlands/
```

This does not replace user judgment for artistic style identity. It only proves
the test arena is visually clean enough to make screenshots meaningful.

## Spellbook Scope Correction

The spellbook is no longer a story/lore/codex surface.

```text
Player Spellbook
+-- Class / Style overview
+-- Active abilities with descriptions
+-- Perks

Dev/Test Spellbook
+-- Class / Style overview
+-- Active abilities with descriptions
+-- Perks
+-- Test controls
    +-- change class
    +-- change style
    +-- refresh current spellbook
    +-- trigger or inspect abilities
```

No Journey, Codex, Journal, Grimoire, lore, or quest tabs.

## Restarting Terra

When Terra restarts from the beginning, validate all 10 Terra styles as a class
group from flatlands. Do not treat earlier wall/spawn screenshots as final visual
proof. Existing prior audits can be used as historical context, not as the final
pass.

Per style:

```text
1. ensure flatlands
2. set class/style
3. spawn close target and distance target
4. cast each of 3 abilities
5. screenshot each impact/active state
6. log-scan for hit/effect proof
7. compare visuals to realignment palette/feel
8. update residuals instead of hiding weak evidence
```
