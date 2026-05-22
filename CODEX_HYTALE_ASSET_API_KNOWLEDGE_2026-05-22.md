# Hytale Asset/API Knowledge Brief - 2026-05-22

## Purpose

This is the working knowledge base for making MOTM abilities look good and play
as intended. It ties together public Hytale docs, local Hytale package
discovery, and this repo's implementation paths.

Use this before adding new visuals, touching ability mechanics, or claiming a
style is gameplay-complete.

## Source-Of-Truth Order

```text
Ability/style intent
  +-- src/main/resources/data/styles/*_styles.json
  +-- CODEX_REALIGNMENT_PLAN_2026-05-13.md
      +-- description is gameplay source of truth
      +-- palette/particle/motion are visual source of truth

Available game primitives
  +-- local Assets.zip
  +-- local HytaleServer.jar
  +-- HytaleAssetResolver.java
  +-- in-game logs and /showcase proof

Implementation
  +-- GameplayPlaybackManager.java
  +-- StatusEffectManager.java
  +-- HytaleAssetResolver.java
  +-- Server/Entity/Effects/MOTM/*.json
  +-- UI pages and scripts harness
```

Public docs are useful for direction, but local jar/assets/logs are final for
this project.

## Fresh Local Discovery

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/discover-hytale-assets.ps1 -RunId 2026-05-22Tknowledge-pass
```

Output:

```text
audits/harness/assets/2026-05-22Tknowledge-pass/
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

Key counts from the run:

```text
Assets.zip entries:        59,518
HytaleServer.jar entries:  38,672
Particle assets:            2,320
Entity-effect candidates:     152
Models:                     2,815
UI assets:                    100
Key API class matches:        167
Resolver assets referenced:    47
Resolver assets missing:        0
```

Important correction: the resolver currently references only assets that exist
in the local `Assets.zip`.

## Confirmed API Surfaces

Confirmed by `javap` against local
`%APPDATA%/Hytale/install/release/package/game/latest/Server/HytaleServer.jar`
using `.tools/jdk-25/bin/javap.exe`.

```text
Visual effects
  +-- EffectControllerComponent
      +-- addEffect(...)
      +-- addInfiniteEffect(...)
      +-- removeEffect(...)
      +-- clearEffects(...)
      +-- setModelChange(...)
      +-- isInvulnerable / setInvulnerable

Combat
  +-- DamageSystems.executeDamage(...)

Movement
  +-- Velocity
      +-- set(...)
      +-- addForce(...)
      +-- setZero()
      +-- addInstruction(...)
      +-- getVelocity()
      +-- getSpeed()

Position / teleport
  +-- TransformComponent
      +-- getPosition()
      +-- setPosition(...)
      +-- teleportPosition(...)
      +-- setRotation(...)
  +-- Teleport
      +-- createForPlayer(...)
      +-- createExact(...)
      +-- withoutVelocityReset()

UI
  +-- UICommandBuilder
      +-- append / appendInline
      +-- set(...)
      +-- clear / remove
      +-- getCommands()
  +-- CustomUIPage / InteractiveCustomUIPage are present

Projectiles
  +-- ProjectileModule is present in the jar
  +-- GameplayPlaybackManager.launchProjectiles already wraps projectile use
```

Use these class names exactly. Do not invent Hytale API paths.

## Visual Asset Vocabulary Already Proven

These resolver assets are verified present locally and are safe starting points.

```text
Terra / ground
  +-- Block_Break_Stone_Dust
  +-- Earth_Brazier_Glow
  +-- Mace_Signature_Shockwave
  +-- Block_Break_Metal_Sparks
  +-- Block_Break_Crystal_Sparks
  +-- Block_Break_Sand_Dust

Hydro / ice / healing
  +-- Bubbles
  +-- Water_Bubble_Stream_Alpha
  +-- Water_Small_Burst
  +-- Impact_Ice_Shockwave
  +-- Totem_Heal_Sparks_Constant
  +-- Totem_Heal_SmokeFlat_Constant
  +-- Totem_Slow_SmokeFlat_Constant

Aero / wind / lightning / smoke
  +-- Battleaxe_Signature_Whirlwind_Spin
  +-- Wind_Sparks_Tail
  +-- Battleaxe_Bash_Shockwave
  +-- Battleaxe_Signature_Whirlwind
  +-- Void_Lightning
  +-- Sword_Signature_Ready_Sparks
  +-- Mace_Signature_Cast_Smoke
  +-- Mace_Signature_Cast_End_Smoke

Corruptus / fire / void / poison
  +-- Fire_Charge1_Fire
  +-- Impact_Fire
  +-- Impact_Smoke
  +-- Fire_AoE_Grow
  +-- Void_Sparks
  +-- VoidImpact
  +-- VoidSmoke_Impact
  +-- VoidSplash
  +-- Impact_Poison
  +-- Acid_Sparks
```

The keyword catalog also exposes richer candidates for future tuning, including
weather sandstorm, geyser water beams, mace ground-hit cracks, crystal run/sprint
sparks, and water splash variants. Any new candidate must be routed through
discovery proof or `/showcase`, not guessed from a name.

## Model / Role Vocabulary Already Proven

Resolver model paths are verified present locally:

```text
Elemental / class identity
  +-- Spirit_Root
  +-- Spirit_Frost
  +-- Spirit_Thunder
  +-- Golem_Crystal
  +-- Golem_Firesteel

Summon / transformation / proxy candidates
  +-- Scarak_Fighter
  +-- Scarak_Broodmother
  +-- Spark_Living
  +-- Pterodactyl
  +-- Bat
  +-- Shadow_Knight
  +-- Toad_Rhino
  +-- Rex_Cave
  +-- Spawn_Void
  +-- Eye_Void
  +-- Frog
```

Role names are stricter than model paths. Continue resolving roles through
`HytaleAssetResolver.resolveProjectileRoleId(...)` and
`resolveFieldRoleId(...)`; do not pass invented names to `setRoleName`.

## Current Runtime Architecture

```text
Ability execution
  +-- GameplayPlaybackManager.executeAbility(...)
      +-- launchProjectiles(...)
      +-- applyCombat(...)
      +-- startLineControlRuntime(...)
      +-- handleSummonRuntime(...)
      +-- spawnFieldVisualProxy(...)
      +-- applyFieldTerrainEffects(...)
      +-- spawnQuakeImpactRing(...)

Status truth
  +-- StatusEffectManager
      +-- applyEffect(...)
      +-- tickEffects(...)
      +-- getDamageReduction(...)
      +-- getDamageIncrease(...)
      +-- getSpeedBonus(...)
      +-- getSlowMultiplier(...)
      +-- absorbDamage(...)
      +-- consumeOneShot(...)

Visual truth
  +-- HytaleAssetResolver
      +-- resolveCastEffect(...)
      +-- resolveTravelEffect(...)
      +-- resolveImpactEffect(...)
      +-- resolveLoopEffect(...)
      +-- resolveModel(...)
      +-- resolveProjectileRoleId(...)
      +-- resolveFieldRoleId(...)
```

Important pattern: world-position particles are currently implemented through
tiny NPC visual proxies plus EntityEffect assets. Keep using that pattern unless
local jar discovery proves a first-class particle-at-position API.

## Ability Scenario Coverage

Generated from all 120 abilities:

```text
cleanse:            1
facing_cone:        6
ground_target:      7
ground_zone:       20
jump_land:          1
movement:          12
projectile_line:   25
self_buff:         33
single_target:      5
summon:             9
support_heal:       1
```

This means most future quality work is not simple target damage. The largest
proof buckets are:

```text
self_buff
  +-- third-person body/HUD/status proof

projectile_line
  +-- launch proof plus target-side hit/effect proof

ground_zone
  +-- field duration/radius plus tick/status proof

movement
  +-- before/after displacement plus target-side result

summon
  +-- role exists, summon appears, survives/acts
```

Use `scripts/generate-ability-matrix.ps1`,
`scripts/setup-ability-scenario.ps1`, and `scripts/assert-ability-proof.ps1`
before any focused style acceptance.

## Public Research Notes

Public sources checked on 2026-05-22:

- Hytale's modding strategy/status post describes plugins, data assets, art
  assets, save/prefab content, and uneven early docs/tooling as the practical
  modding surface.
  Source: https://hytale.com/news/2025/11/hytale-modding-strategy-and-status
- Custom UI docs describe Java-driven custom UI commands with client `.ui`
  markup and event flow back to Java.
  Source: https://hytalemodding.dev/pl-PL/docs/official-documentation/custom-ui
- Entity effect docs describe `EffectControllerComponent`, `EntityEffect`
  assets, duration/overlap behavior, model changes, tint, particles, sounds, and
  removal behavior. Treat public docs as directional; the local jar confirmed
  the component/method surface.
  Source: https://hytale-docs.pages.dev/modding/systems/entity-effects/

## Implementation Rules From This Pass

```text
For visuals
  +-- pick from resolver assets first
  +-- then keyword catalog / Assets.zip
  +-- then /showcase proof
  +-- then update resolver

For mechanics
  +-- ability description is source of truth
  +-- use scenario matrix to choose setup
  +-- prove target-side or caster-side outcome in logs
  +-- screenshot/video is required for visual PASS

For API use
  +-- javap local jar before adding imports
  +-- do not chase undocumented movement APIs
  +-- prefer Velocity, TransformComponent, Teleport, DamageSystems, EntityEffect

For protected data
  +-- surgical edits only
  +-- never regenerate style/perk/class JSON
```

## Next Work Order

```text
1. Phase 6 spellbook UI
   +-- only Class/Style/Abilities and Perks
   +-- separate dev/test variant with class/style switching

2. Cross-cutting runtime proof improvements
   +-- projectile target-side impact logs
   +-- movement before/after logs
   +-- summon role/action proof
   +-- field tick proof

3. Mechanical compliance batch
   +-- delayed ground_strike telegraphs
   +-- terrain_effect gaps from realignment Section 3
   +-- self-buff/reactive hooks

4. Visual identity batch
   +-- class by class
   +-- style by style
   +-- use palette + verified asset vocabulary
   +-- no FULL PASS without runtime + mechanical + visual proof
```
