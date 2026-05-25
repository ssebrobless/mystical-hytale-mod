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
      +-- switch to third-person camera (V)
      +-- clear tracked test mobs
      +-- spawn close + distant targets
      +-- log tracked mob count
          +-- cast ability
              +-- capture screenshot
              +-- tail log
              +-- write runtime smoke / gameplay acceptance / residuals
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

## Runtime Smoke Is Not Gameplay Acceptance

The 2026-05-22 Terra flatlands run proved that the command path can cast all
30 Terra abilities without blocking class/runtime errors. That is only a
runtime smoke pass. It is not enough to claim an ability is mechanically or
visually accepted.

```text
╔══════════════════════╦══════════════════════════════╦══════════════════════════════╗
║ Gate                 ║ Evidence                     ║ Status Meaning               ║
╠══════════════════════╬══════════════════════════════╬══════════════════════════════╣
║ Runtime smoke        ║ Cast log + no crash           ║ Command path works           ║
║ Mechanical gameplay  ║ Required hit/status/movement  ║ Description is performed     ║
║ Visual identity      ║ Third-person screenshot/video  ║ Style reads at a glance      ║
╚══════════════════════╩══════════════════════════════╩══════════════════════════════╝
```

Future class/style reports must keep these separate. A cast line with
`No valid target in range`, no jump-land resolution, or no target-side projectile
impact is a runtime smoke result at best, not a gameplay acceptance pass.

## Mob Hygiene

Repeated `/motm dev test mobs close` calls can crowd the arena and make both
targeting and screenshots dishonest. The harness must:

```text
Before each ability
├── /motm dev test mobs clear
├── /motm dev test mobs close
├── /motm dev test mobs count
└── require spawn log with clearedPrevious=<n> and tracked=2
```

The Java dev command now despawns tracked test mobs before spawning the next
pair and logs `clearedPrevious` plus `tracked`. This is still scoped to tracked
MOTM test mobs; wild/leftover mobs from older sessions should be handled by
cold-launch/world reset if they pollute screenshots.

## Scenario-Aware Visual Checks

Ability descriptions and cast metadata decide the audit setup:

```text
cast_type / trigger
├── jump_land       ▶ arm ability, jump, land, require landing-resolution log
├── cone / gaze     ▶ face target before cast; camera cone matters
├── movement        ▶ capture before/after displacement, not only cast log
├── projectile      ▶ prove launch and target-side hit/effect
├── ground field    ▶ prove field persists for duration_seconds
└── self buff       ▶ use third-person camera for body/tint/status readability
```

Pressing `V` toggles third person in Hytale and is now part of the default
class audit runner. Use first person only when aiming precision is the thing
being tested.

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
3. switch third-person unless aiming precision is under test
4. clear tracked test mobs
5. spawn close target and distance target
6. log tracked mob count
7. cast each of 3 abilities with scenario-specific movement
8. screenshot each impact/active state
9. log-scan for hit/effect/movement proof
10. compare visuals to realignment palette/feel
11. update residuals instead of hiding weak evidence
```

## Concept-Aware Harness Upgrade

The next audit layer is now script-backed:

```text
Style JSONs
  +-- scripts/generate-ability-matrix.ps1
      +-- audits/ability-matrix/<timestamp>/ability-scenarios.md
      +-- per ability: scenario, required setup, required proof

Per-ability setup
  +-- scripts/setup-ability-scenario.ps1
      +-- sets class/style/freecast
      +-- clears tracked mobs
      +-- chooses close targets, third-person, or movement lane by scenario

Per-ability proof
  +-- scripts/assert-ability-proof.ps1
      +-- runtime PASS/FAIL
      +-- mechanical PASS/REVIEW/FAIL
      +-- visual REVIEW unless screenshot/video review is explicit
```

`scripts/audit-phase9-class.ps1` now reports runtime, mechanical, and visual
separately. Its default remains a runtime smoke gate so class sweeps can keep
running, but `-RequireConceptProof` upgrades REVIEW rows into failures for
focused gameplay acceptance.

Use this before claiming an ability works:

```text
1. Generate/read ability matrix.
2. Run setup for the exact ability scenario.
3. Cast the ability.
4. Assert proof from logs.
5. Review screenshots/video against style identity.
6. Only then call it FULL PASS.
```

The research direction remains unchanged: public Hytale docs support
server-side, asset-driven testing, but local `HytaleServer.jar`, `Assets.zip`,
and in-game logs are the final authority for concrete APIs, particle IDs, and
role names.
