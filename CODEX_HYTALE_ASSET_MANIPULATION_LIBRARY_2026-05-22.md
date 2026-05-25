# Hytale Asset Manipulation Library - 2026-05-22

## Purpose

This is the practical bridge between MOTM concepts and what the installed Hytale
build can actually show or manipulate.

Use this before approving more style visuals. The rule is now:

```text
Concept says physical object
  +-- try block, prefab, block model, or NPC/model proxy first
  +-- use particles only as support

Concept says coating
  +-- try EntityEffect tint/model VFX first
  +-- use loose particles only if tight coating is not available

Concept says projectile made of material
  +-- try model/role projectile proxy first
  +-- use particles as trail/impact support
```

## Sources Checked

```text
Local source of truth
  +-- %APPDATA%/Hytale/install/release/package/game/latest/Assets.zip
  +-- %APPDATA%/Hytale/install/release/package/game/latest/Server/HytaleServer.jar
  +-- src/main/java/com/motm/util/HytaleAssetResolver.java
  +-- src/main/resources/Server/Entity/Effects/MOTM/*.json

Public references
  +-- Hytale Modding Strategy and Status
      https://hytale.com/news/2025/11/hytale-modding-strategy-and-status
  +-- Hytale Custom UI reference mirror
      https://hytalemodding.dev/pl-PL/docs/official-documentation/custom-ui
  +-- Hytale Entity Effects reference mirror
      https://hytale-docs.pages.dev/modding/systems/entity-effects/
```

Public references are directional only. Local jar/assets/logs are final for this
mod because that is what the current game build loads.

## Generated Local Library

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/generate-hytale-asset-manipulation-library.ps1 -RunId latest
```

Output:

```text
audits/hytale-asset-library/latest/
  +-- report.md
  +-- all-assets.txt
  +-- particles.txt
  +-- models.txt
  +-- animations.txt
  +-- block-models.txt
  +-- block-items.txt
  +-- prefabs.txt
  +-- entity-effects.txt
  +-- ui.txt
  +-- keyword-catalog.md
  +-- api-classes.txt
  +-- api-public-signatures.txt
```

Current counts:

```text
Assets.zip entries:       59,518
HytaleServer.jar entries: 38,672
Particles:                 2,320
Models:                    2,815
Animations:                6,717
Block models:              1,151
Block/item JSON:           4,101
Prefabs:                   7,823
Entity effects:              140
UI files:                    100
Relevant API classes:        823
```

## Manipulation Surfaces

```text
Temporary blocks / structures
  +-- Local assets include block item JSON for rock, metal, lava, flowers, saplings, cactus, etc.
  +-- Jar exposes BlockPlaceUtils.placeBlock(...) and BlockReplaceEvent.
  +-- Status: promising but not proven inside MOTM runtime yet.
  +-- Use for: Iron Wall, Lava Pool, Obsidian Skin shell, Pillar Strike, flower trail.
  +-- Required proof: place, collide/affect, restore/remove without corrupting world.

Prefabs
  +-- Assets include 7,823 Server/Prefabs/*.prefab.json files.
  +-- Jar exposes SpawnPrefabInteraction, PrefabStore, and prefab buffer utilities.
  +-- Status: promising but not proven inside MOTM runtime yet.
  +-- Use for: vine patches, root clumps, larger terrain/plant/void constructs.
  +-- Required proof: spawn at target point, track ownership, clean up on expiry.

Entity effects
  +-- Already proven by MOTM EntityEffect JSON files.
  +-- EffectControllerComponent supports addEffect, addInfiniteEffect, removeEffect,
      clearEffects, setModelChange, and reset model change.
  +-- EntityEffect supports duration, overlap behavior, application effects,
      model change, model override, invulnerability, resistance, and tints.
  +-- Use for: Metal Coat, Obsidian Skin coating, poison smoke, roots-on-target,
      petrification, transformations.

Models and role proxies
  +-- Local assets include 2,815 blockymodel files plus NPC models.
  +-- MOTM already uses NPCEntity proxy visuals for projectiles/fields/summons.
  +-- Role names are stricter than model paths; resolve through HytaleAssetResolver.
  +-- Use for: lava blob, cactus projectile, stone chunks, summons, body proxies.

Particles
  +-- Local assets include 2,320 particle assets.
  +-- Use as secondary support unless the concept is purely energetic/smoky.
  +-- Good for: impact, trail, smoke, sparks, poison haze, cracks, shockwaves.

Movement / force / positioning
  +-- Velocity exposes set/addForce/setZero/addInstruction/getVelocity/getSpeed.
  +-- TransformComponent exposes getPosition/setPosition/teleportPosition.
  +-- Use for: knockback, pull, dash, projectile proxy syncing, target pinning.
  +-- Hard rule: do not chase undocumented movement APIs when a visual/status
      solution is the approved design.
```

## Key Asset Families For Current Terra Review

```text
Metal
  +-- Server/Item/Items/Metal/Iron/Metal_Iron.json
  +-- Server/Item/Items/Metal/Iron/Metal_Iron_Smooth.json
  +-- Common/BlockTextures/Metal_Iron*.png
  +-- Server/Particles/Block/Metal/Spawners/Block_Break_Metal_Sparks.particlespawner

Magma / lava
  +-- Server/Item/Items/Fluid/Fluid_Lava.json
  +-- Server/Item/Block/Fluids/Lava.json
  +-- Server/Item/Block/Fluids/Lava_Source.json
  +-- Server/Particles/Block/Lava/Spawners/Lava_Circle.particlespawner
  +-- Server/Particles/Block/Lava/Spawners/Lava_Bubbles.particlespawner
  +-- Server/Item/Items/Rock/Magma/Rock_Magma_Cooled.json
  +-- Server/Item/Items/Rock/Volcanic/Rock_Volcanic_Cracked_Lava.json

Stone
  +-- Server/Item/Items/Rock/Stone/Rock_Stone.json
  +-- Server/Item/Items/Rock/Stone/Rock_Stone_Brick_Pillar_Base.json
  +-- Server/Item/Items/Rock/Stone/Rock_Stone_Brick_Pillar_Middle.json
  +-- Common/Blocks/Stone/Rubble_*.blockymodel
  +-- Common/BlockTextures/Cracks/T_Crack_Stone_*.png

Arbor
  +-- Common/Blocks/Foliage/Tree/Sapling.blockymodel
  +-- Server/Item/Items/Plant/Plant_Sapling_Oak.json
  +-- Server/Prefabs/Plants/Vines/Green/Vines_Green_001.prefab.json
  +-- Common/Blocks/Foliage/Plants/Roots_Wide.blockymodel
  +-- Common/Blocks/Foliage/Plants/Vine_Thick_Roots.blockymodel

Bloom
  +-- Server/Item/Items/Plant/Flowers/Plant_Flower_Common_Purple.json
  +-- Server/Item/Items/Plant/Flowers/Plant_Flower_Common_Poisoned.json
  +-- Server/Item/Items/Plant/Cactus/Plant_Cactus_1.json
  +-- Common/Blocks/Foliage/Cactus/Cactus_Spike_Large.blockymodel
  +-- Common/Blocks/Foliage/Cactus/Cacti_Tall_Kit_*.blockymodel
  +-- Server/Particles/Combat/Impact/Misc/Impact_Poison.particlesystem
  +-- Server/Particles/Projectile/Acid/Spawners/Acid_Sparks.particlespawner
```

## Feasibility Recheck: Approved Terra Styles

| Style | Ability | Intended object-first route | Confidence | Required proof before final PASS |
| --- | --- | --- | --- | --- |
| Metal | Iron Wall | Place four temporary `Metal_Iron`/`Metal_Iron_Smooth` blocks in a 2x2 wall; track wall lifetime; knock enemies away from wall contact zone. | Medium | Prove block placement/removal and collision/contact knockback. |
| Metal | Metal Coat | EntityEffect with tight gray top/bottom tint, no sparks. | High | Third-person screenshot shows tight metallic body coating. |
| Metal | Alloy Enhancement | Restrict to physical melee/tools; try held-item tint/model effect; fallback to hand/item proxy glow if item tint is unavailable. | Medium | Prove ranged/magic excluded and visual attaches to held item rather than body. |
| Magma | Lava Pool | Place/move temporary lava/fluid blocks as an expanding ring; lava particles only support. | Medium-low | Prove fluid placement/removal and friendly safety. |
| Magma | Obsidian Skin | Brief temporary lava-block shell, then dark purple/black EntityEffect body coating. | Medium | Prove shell placement cleanup plus coating screenshot. |
| Magma | Magma Sling | Projectile visual proxy using magma/lava role/model if possible, with lava blob particles/trail. | Medium | Prove projectile reads as molten blob and no nonexistent role logs. |
| Stone | Rubble Rouser | EntityEffect/model VFX on arms if possible; fallback to tight upper-body/arm stone effect using stone block visual language. | Medium | Prove arm-focused read; avoid loose aura. |
| Stone | Pillar Strike | Place temporary `Rock_Stone_Brick_Pillar_Base/Middle` blocks under targets; launch target. | Medium | Prove pillar placement under target and cleanup. |
| Stone | Rockslide | Ground dash with rubble/stone trail proxy and knockback path. | High | Prove player moves, immunity window, enemy knockback, trail. |
| Arbor | Rooted | Root/vine model or prefab at player's legs plus heal/status. | Medium | Prove root visual attached to lower body/ground. |
| Arbor | Vines | Single-target persistent vine/root effect; old target clears on retarget/death. | Medium | Prove no cooldown, one target, cleanup on death/retarget. |
| Arbor | Sapling | Projectile ignores enemies and resolves on ground; place/spawn sapling at impact. | Medium | Prove ground-hit targeting and sapling taunt object. |
| Bloom | Nightshade | Projectile passes enemies, lands on surface, spawns purple/poison flower lure, then poison explosion. | Medium | Prove pass-through, lure radius, poison body smoke. |
| Bloom | Frolick | Place actual flowers behind moving player. | Medium | Prove flowers are placed while moving and cleaned up if temporary. |
| Bloom | Cacti Cluster | Large cactus model projectile; stick to first enemy/surface; delayed visual explosion and DoT spread. | Medium | Prove stick/sync, no extra attached-target explosion damage, secondary DoT+slow. |

## New Review Rule For Remaining Styles

For every ability we review from Self Petrification onward, I should answer these
before asking for your changes:

```text
1. What literal object/effect does the concept want?
2. Which local Hytale asset family can represent that literally?
3. Which manipulation surface expresses it?
   +-- temporary block
   +-- prefab
   +-- EntityEffect tint/model change
   +-- NPC/model proxy
   +-- projectile proxy
   +-- particle-only fallback
4. Is it already proven in MOTM runtime, or does it need a proof harness?
5. What fallback is acceptable if the literal route fails?
```

## Immediate Implementation Implication

Before implementing object-heavy Terra styles, add a tiny isolated proof harness
for temporary block/prefab placement:

```text
/motm dev visual tempblock <assetId> <seconds>
/motm dev visual prefab <prefabPath> <seconds>
```

Acceptance:

```text
PASS
  +-- object appears at controlled test position
  +-- object is visible in screenshot
  +-- object has expected collision if it is a block
  +-- object cleans up after duration
  +-- logs contain no asset/role/API errors

FAIL
  +-- fallback to model/proxy route for that ability
  +-- document fallback in CODEX_PHASE9_RESIDUALS.md
```
