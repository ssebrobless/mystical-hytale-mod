# Hytale P0-P4 Runtime Research - 2026-05-23

## Purpose

This closes the immediate implementation gap behind the Terra proof list:

```text
P0 coating proof
P1 temporary block proof
P2 temporary fluid proof
P3 model/proxy proof
P4 movement safety proof
```

The goal is to stop guessing from concept language and prove which Hytale
runtime surfaces can carry repeated mechanics across Terra, Hydro, Aero, and
Corruptus.

## Sources Checked

```text
Local, authoritative for this install
  +-- %APPDATA%/Hytale/install/release/package/game/latest/Server/HytaleServer.jar
  +-- %APPDATA%/Hytale/install/release/package/game/latest/Assets.zip
  +-- src/main/java/com/motm/manager/GameplayPlaybackManager.java
  +-- src/main/java/com/motm/util/HytaleAssetResolver.java
  +-- CODEX_HYTALE_ASSET_MANIPULATION_LIBRARY_2026-05-22.md
  +-- CODEX_TERRA_IMPLEMENTATION_FEASIBILITY_REVIEW_2026-05-22.md

Public, directional only
  +-- Hytale Modding Strategy and Status
      https://hytale.com/news/2025/11/hytale-modding-strategy-and-status
  +-- Hytale Custom UI reference mirror
      https://hytalemodding.dev/pl-PL/docs/official-documentation/custom-ui
  +-- Hytale Entity Effects reference mirror
      https://hytale-docs.pages.dev/modding/systems/entity-effects/
  +-- Hytale server docs root
      https://release.server.docs.hytale.com/
```

Repeatable local probe:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/probe-hytale-runtime-capabilities.ps1 -RunId 2026-05-23-p0-p4
```

Output:

```text
audits/hytale-runtime-capabilities/2026-05-23-p0-p4/
  +-- report.md
  +-- api-class-hits.txt
  +-- api-public-signatures.txt
```

## Summary Shape

```text
Terra proof needs
+-- P0 coating
|   +-- proven API: EntityEffect + EffectControllerComponent
|   +-- first users: Metal Coat, Obsidian Skin, Gargoyle, Glare
+-- P1 temp blocks
|   +-- proven API surface: BlockType + BlockSelection
|   +-- first users: Iron Wall, Pillar Strike, sapling/flower markers
+-- P2 temp fluids
|   +-- proven API surface: Fluid + BlockSelection
|   +-- first users: Lava Pool, Mudpit, water-style Hydro fields
+-- P3 model/proxy
|   +-- proven route: existing NPC visual proxy + resolver roles/effects
|   +-- likely route: ModelAsset/ModelComponent for custom model entities
|   +-- first users: Magma Sling, Cacti Cluster, Lapidary, Vitrification
+-- P4 movement safety
    +-- proven API surface: Velocity + TransformComponent
    +-- first users: Burrow, Tunnel, Dust Devil, Rockslide
```

## P0 - Coating Proof

Verdict: implementable now.

Confirmed local APIs:

```text
EntityEffect
  +-- getApplicationEffects()
  +-- getModelOverride()
  +-- getModelChange()
  +-- getDuration()
  +-- getOverlapBehavior()
  +-- isInvulnerable()

EffectControllerComponent
  +-- addEffect(...)
  +-- addInfiniteEffect(...)
  +-- removeEffect(...)
  +-- clearEffects(...)
  +-- setModelChange(...)
  +-- tryResetModelChange(...)
```

Best MOTM route:

```text
1. Create small MOTM EntityEffect JSONs.
2. Use the already-working GameplayPlaybackManager.applyEffectById(...) path.
3. Validate in third person with V key.
4. Tune only after screenshot proof.
```

Ability mapping:

| Ability family | Route |
| --- | --- |
| Metal Coat | Tight gray/silver top+bottom tint, no loose sparks. |
| Obsidian Skin ended state | Dark purple/black body tint. |
| Gargoyle / Glare | Stone body tint or model-change effect. |
| Poison/suffocation/rooted states | Body-hugging smoke/tint EntityEffect. |

Open risk:

```text
Limb-only or held-item-only coating is not proven.
Rubble Rouser arms and Alloy Enhancement may need a proxy fallback if Hytale
does not expose item/limb-specific visual mutation through EntityEffect.
```

## P1 - Temporary Block Proof

Verdict: likely exact, but must be isolated before ability use.

Confirmed local APIs:

```text
BlockType
  +-- getAssetMap()
  +-- getBlockIdOrUnknown(...)

BlockSelection
  +-- addBlockAtWorldPos(...)
  +-- addEmptyAtWorldPos(...)
  +-- copyFromAtWorld(...)
  +-- place(...)
  +-- placeNoReturn(...)
  +-- canPlace(...)
```

Important research result:

```text
Use BlockSelection first, not BlockPlaceUtils.

BlockPlaceUtils.placeBlock(...) exists, but it models player placement and
requires item stack, inventory, entity refs, chunk refs, rotation, and accessors.
That is useful for vanilla-like placement validation later. For controlled
temporary MOTM structures, BlockSelection is the direct world-edit surface.
```

Best MOTM route:

```text
TemporaryStructureRuntime
  +-- resolve block id with BlockType.getBlockIdOrUnknown(assetId,...)
  +-- construct BlockSelection at world positions
  +-- copy original area before placement
  +-- place the new selection
  +-- track owner/style/ability/expiresAt/originalSelection
  +-- restore original selection on expiry, cancel, recast, logout, death, world change
```

Ability mapping:

| Ability | Literal route |
| --- | --- |
| Iron Wall | 2x2 temporary metal block wall in front of caster; knock enemies from wall zone; cooldown starts after cleanup. |
| Pillar Strike | Temporary stone pillar blocks under/near target; apply vertical launch; cleanup quickly. |
| Sapling / Nightshade | Temporary plant marker on ground/surface if placement supports the asset. |
| Frolick | Temporary flower placement trail behind moving caster. |
| Lapidary fallback | Floating 2x2 gem block cluster only if block placement can be non-griefing and cleanup-safe. |

Open risk:

```text
Block id names must be validated locally.
Block placement can overwrite real world state if cleanup is wrong.
Collision needs in-game proof, not only compile proof.
```

## P2 - Temporary Fluid Proof

Verdict: likely feasible, higher risk than blocks.

Confirmed local APIs:

```text
Fluid
  +-- getAssetMap()
  +-- getFluidIdOrUnknown(...)
  +-- getMaxFluidLevel()
  +-- getDamageToEntities()
  +-- getFluidFXId()

BlockSelection
  +-- addFluidAtWorldPos(...)
  +-- getFluidAtWorldPos(...)
  +-- getFluidLevelAtWorldPos(...)
  +-- forEachFluid(...)
  +-- place(...)
```

Best MOTM route:

```text
TemporaryFluidRuntime
  +-- resolve fluid id with Fluid.getFluidIdOrUnknown(assetId,...)
  +-- copy original blocks/fluids first
  +-- place shallow/full fluid selection for a short lifetime
  +-- apply MOTM status logic separately for friendly immunity
  +-- restore original selection on cleanup
```

Ability mapping:

| Ability | Literal route |
| --- | --- |
| Lava Pool | Expanding lava fluid/block ring; remove old ring as the ring expands. |
| Obsidian Skin shell | Very short lava-block/fluid shell around caster, then cleanup and body coating. |
| Mudpit | Temporary water/fluid field with brown overlay particles/tint if raw water cannot be recolored. |
| Hydro fields later | Same runtime can carry rain/current/tidal/geyser water fields. |

Open risk:

```text
Fluid tinting is not proven.
If Hytale cannot tint placed water/lava directly, the field should use real
fluid for mechanics plus brown/colored smoke or proxy effects for the style read.
Friendly immunity must be MOTM-owned status filtering, not relying on vanilla
fluid behavior.
```

## P3 - Model / Proxy Proof

Verdict: existing proxy route is proven; direct model entities are promising.

Confirmed local APIs:

```text
ModelAsset
  +-- getAssetMap()
  +-- getModel()
  +-- getTexture()
  +-- getBoundingBox()
  +-- getDefaultAttachments()

Model
  +-- createUnitScaleModel(...)
  +-- createScaledModel(...)

ModelComponent
  +-- present in local jar

PrefabStore
  +-- getAssetPrefab(...)
  +-- getAssetPrefabFromAnyPack(...)
```

Already-proven MOTM route:

```text
GameplayPlaybackManager
  +-- creates NPCEntity visual proxies
  +-- setRoleName(resolvedRoleId)
  +-- setDespawnTime(...)
  +-- world.spawnEntity(...)
  +-- applyEffectById(proxyRef,...)
```

Best MOTM route:

```text
1. Prefer existing role-resolved NPC proxy visuals for particles/fields/projectiles.
2. Add resolver-backed role/model mappings, never invented role strings.
3. Use direct ModelComponent experiments only inside P3 proof harness.
4. Use PrefabStore for larger static objects only after temp block proof is green.
```

Ability mapping:

| Ability | First route |
| --- | --- |
| Magma Sling | Projectile proxy with magma/lava model or lava particle trail. |
| Cacti Cluster | Large cactus model proxy; sticky sync to target/surface. |
| Lapidary | Persistent gem/crystal proxy or block cluster; HP/status tracking. |
| Vitrification | Floating shard charge proxies around caster. |
| Tunnel | Stone-block player disguise proxy plus movement safety runtime. |

Open risk:

```text
Model paths and role names are different things.
Passing model-looking strings into setRoleName can cause nonexistent-role logs.
Every proxy must go through HytaleAssetResolver or a local proof command.
```

## P4 - Movement Safety Proof

Verdict: force/dash/teleport primitives exist; safe traversal needs a harness.

Confirmed local APIs:

```text
Velocity
  +-- set(...)
  +-- setClient(...)
  +-- addForce(...)
  +-- addInstruction(...)
  +-- setZero()
  +-- getVelocity()
  +-- getSpeed()

TransformComponent
  +-- getPosition()
  +-- setPosition(...)
  +-- teleportPosition(...)
  +-- setRotation(...)
```

Best MOTM route:

```text
MovementRuntime
  +-- record start position and facing
  +-- choose destination with block/world checks
  +-- apply Velocity for natural dashes when possible
  +-- use TransformComponent.teleportPosition for scripted pop-up/pop-down
  +-- force surface recovery when destination is unsafe
  +-- log before/after position, distance, and safety decision
```

Ability mapping:

| Ability | Route |
| --- | --- |
| Burrow | Hide/pop-down visual, scripted 4-block forward dash, safe re-emerge, exit AoE. |
| Tunnel | Controlled step movement as stone block; consume stone resource; forced surface recovery if resource ends underground. |
| Dust Devil | Dash while Sandstorm active, drag targets in radius, expel at end, deactivate Sandstorm. |
| Rockslide | Ground dash/trail with path collision, knockback, temporary immunity. |

Open risk:

```text
True free noclip through terrain is not guaranteed.
Tunnel should be implemented as controlled step/teleport traversal with surface
recovery, not as an uncontrolled physics state that can strand the player.
```

## Implementation Order

```text
P0. Coating proof
    +-- /motm dev proof coating metal
    +-- /motm dev proof coating obsidian
    +-- /motm dev proof coating stone
    +-- /motm dev proof coating poison-target

P1. Temporary block proof
    +-- /motm dev proof tempblock metal-wall
    +-- /motm dev proof tempblock stone-pillar
    +-- /motm dev proof tempblock flower
    +-- /motm dev proof tempblock sapling

P2. Temporary fluid proof
    +-- /motm dev proof tempfluid lava-ring
    +-- /motm dev proof tempfluid water-field
    +-- /motm dev proof tempfluid mud-overlay

P3. Model/proxy proof
    +-- /motm dev proof proxy magma-blob
    +-- /motm dev proof proxy cactus-projectile
    +-- /motm dev proof proxy gem
    +-- /motm dev proof proxy glass-shards

P4. Movement safety proof
    +-- /motm dev proof movement burrow
    +-- /motm dev proof movement tunnel
    +-- /motm dev proof movement dust-devil
```

Each proof writes:

```text
audits/proofs/<proof-id>/<timestamp>/
  +-- report.md
  +-- server.log
  +-- screen-*.png
```

PASS means:

```text
1. Local Hytale API path compiles.
2. In-world visual/mechanic appears in flatlands.
3. Cleanup restores state.
4. Logs contain no missing asset, missing role, classload, or crash errors.
5. If visual quality matters, screenshot or user confirmation says it reads correctly.
```

## What This Unlocks For Other Classes

```text
Hydro
  +-- P0 for bubble/barrier coatings
  +-- P2 for water fields, currents, geysers, rain pools
  +-- P4 for pushes/pulls/current movement

Aero
  +-- P3 for wind/thunder proxies
  +-- P4 for vertical mobility, dashes, cyclone pull/expel
  +-- P0 for body buffs that should not conflict with Skybound

Corruptus
  +-- P0 for shadow/void/blood/decay body states
  +-- P3 for summons, void objects, flame blobs
  +-- P4 for pulls, launches, possession-like movement constraints
```

## Current Recommendation

Implement P0-P4 as isolated dev proof commands before building the next Terra
style runtime. That gives us a shared library of proven primitives:

```text
If proof passes
  +-- implement the literal concept route

If proof fails
  +-- use the documented fallback
  +-- record the residual
  +-- avoid pretending the concept is exact
```

This is the cleanest bridge between "what the ability should be" and "what this
Hytale build can safely do."
