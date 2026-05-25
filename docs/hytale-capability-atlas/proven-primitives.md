# Proven And Candidate Hytale Primitives

Updated: 2026-05-24

## Primitive Map

```
╔════════════════════╦════════════════════════════╦════════════════════╗
║ Concept Need        ║ Preferred Hytale Primitive ║ Current Risk       ║
╠════════════════════╬════════════════════════════╬════════════════════╣
║ Body/item coating   ║ EntityEffect/model change  ║ Low-Medium         ║
║ Real barricade      ║ Temporary blocks           ║ Medium             ║
║ Ground objects      ║ Temporary blocks/prefabs   ║ Medium             ║
║ Lava/water fields   ║ Temporary fluids + guards  ║ High               ║
║ Projectile visuals  ║ Projectile API or proxy    ║ Medium-High        ║
║ Summons/objects     ║ NPC/model/proxy entities   ║ Medium             ║
║ Dashes/knockback    ║ Velocity/status runtime    ║ Medium             ║
║ Player relocation   ║ Teleport/Transform, guarded║ High               ║
║ UI/spellbook        ║ Server Custom UI + .ui     ║ Medium             ║
║ Test evidence       ║ Structured logs + captures ║ Low once built     ║
╚════════════════════╩════════════════════════════╩════════════════════╝
```

## P0: EntityEffect Coating

Status: partially proven and reusable.

Use for:

- Metal Coat
- Alloy Enhancement visual coating
- Obsidian Skin post-lava state
- Gargoyle/Glare petrification
- poison smoke or target status states
- rooted/buried status readability

Local/API support:

- `EntityEffect.getAssetMap()`
- `EffectControllerComponent.addEffect(...)`
- `EffectControllerComponent.addInfiniteEffect(...)`
- `EffectControllerComponent.removeEffect(...)`
- `EffectControllerComponent.setModelChange(...)`
- `EffectControllerComponent.tryResetModelChange(...)`
- `EntityEffect.getApplicationEffects()`
- `EntityEffect.getModelOverride()`
- `EntityEffect.getModelChange()`

Known design constraint:

- We have not proven true material texture replacement on a player model.
- We have proven useful body coating via model/VFX/tint behavior.
- Recolorable coating should be treated as the main reusable route, not loose
  smoke particles.

## P1: Temporary Blocks

Status: API exists, Iron Wall-style behavior is promising, needs hardened proof
for each object family before broad use.

Use for:

- Iron Wall
- Pillar Strike
- Frolick flowers
- Sapling/Nightshade markers
- Cacti Cluster surface cactus fallback
- Gem cube if using block cluster
- future ground props

Local/API support:

- `BlockPlaceUtils.placeBlock(...)`
- `BlockPlaceUtils.canPlaceBlock(...)`
- `BlockType`
- `BlockSelection`
- many local block/item assets

Implementation rules:

- Never overwrite unknown valuable blocks without saving exact previous state.
- Always store owner, ability id, positions, original states, expiry tick.
- Cleanup on expiry, recast, death, disconnect, world unload, and crash-recovery
  where possible.
- For decorative trails, place on top of support blocks, never replace the block
  under the player.

## P2: Temporary Fluids

Status: high-risk. Use only after focused proof.

Use for:

- Lava Pool
- Obsidian Skin lava shell
- Mudpit
- future Hydro water fields

Observed risk:

- Real lava can slow/damage/light the caster because vanilla fluid behavior runs
  underneath MOTM gameplay.
- Large/offset shell placements caused client crashes during manual tests.
- Fluid visuals have regressed between visible/invisible when switching
  implementations.

Implementation rules:

- Prefer small, bounded, floor-level placements before shells.
- Keep a strict max block count per tick and per ability.
- Apply caster/ally immunity and speed compensation while inside friendly
  fields.
- If client crash repeats, downgrade that ability to block/model/particle
  fallback and record the limitation.

## P3: Projectiles, Models, And Proxies

Status: useful but needs stricter separation between visual proxy and real NPC.

Use for:

- Magma Sling lava blob
- Cacti Cluster cactus projectile
- Lapidary gem object
- Vitrification shard charges
- summons and field visuals

Local/API support:

- `ProjectileModule` exists in local jar.
- `ModelComponent` exists.
- `TransformComponent` exists.
- MOTM already has NPC/model proxy patterns.
- Resolver-referenced assets currently validate against `Assets.zip`.

Known failure pattern:

- A visual model chosen through the wrong route can spawn as a real living mob
  with a health bar.

Implementation rules:

- Real damaging projectile: prefer Hytale projectile module when possible.
- Pure visual projectile: proxy must be intangible/invulnerable/no AI/no health
  bar/no targetability, or use particles.
- Role names must be resolved through `HytaleAssetResolver`; never pass invented
  role ids to `setRoleName`.
- Every projectile test needs logs for spawn, travel vector, hit, expiry, and
  cleanup.

## P4: Movement, Rooting, Knockback, And Position

Status: possible, but player movement is more sensitive than NPC movement.

Use for:

- Stomp jump/landing
- Rockslide
- Burrow
- Dust Devil
- enemy drag/expel
- knockback and launch
- root/immobilize states

Local/API support:

- `Velocity.set(...)`
- `Velocity.addForce(...)`
- `Velocity.setZero()`
- `Velocity.addInstruction(...)`
- `TransformComponent.setPosition(...)`
- `TransformComponent.teleportPosition(...)`
- `Teleport` classes are present in earlier probes.

Implementation rules:

- Prefer velocity/status effects for short movement changes.
- For controlled player displacement, snapshot before/after position in logs.
- For underground/tunnel behavior, implement surface-recovery first.
- Avoid direct player teleport/position loops unless a proof harness has already
  shown the client stays stable.

## P5: Structured Test Evidence

Status: needed before the next large implementation pass.

Screenshots alone are not enough. Every ability run should emit machine-readable
events:

```
ability.cast.begin
ability.cast.end
projectile.spawn
projectile.travel
projectile.hit
terrain.place
terrain.restore
fluid.place
fluid.restore
effect.apply
effect.remove
damage.apply
shield.apply
movement.before
movement.after
cleanup.complete
```

This is the bridge between "I saw something happen" and "the ability behaved
correctly." The user still owns final visual judgment, but Codex should be able
to prove mechanics from logs.

