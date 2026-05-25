# Research Gates Before Broad Ability Work

Updated: 2026-05-24

These gates are meant to stop us from repeating the same loop where an ability
sort of works visually but fails the real concept.

## Gate Map

```
╔════════╦══════════════════════════════╦════════════════════════════╗
║ Gate   ║ What It Proves                ║ Blocks                     ║
╠════════╬══════════════════════════════╬════════════════════════════╣
║ R0     ║ Local docs/assets indexed     ║ all future style work      ║
║ R1     ║ Structured telemetry exists   ║ reliable self-audits       ║
║ R2     ║ Block lifecycle safe          ║ walls, pillars, flowers    ║
║ R3     ║ Fluid lifecycle safe          ║ lava, mud, water fields     ║
║ R4     ║ Projectile/proxy separation   ║ Magma Sling, cactus, shards ║
║ R5     ║ Movement proof scripts        ║ stomp, dash, burrow, tunnel ║
║ R6     ║ UI route clarified            ║ spellbook class/style/perks ║
╚════════╩══════════════════════════════╩════════════════════════════╝
```

## R0: Capability Atlas

Status: PASS for initial research refresh.

Evidence:

- `audits/hytale-asset-library/2026-05-24-capability-atlas/report.md`
- `audits/hytale-runtime-capabilities/2026-05-24-capability-atlas/report.md`
- `audits/harness/assets/2026-05-24-capability-atlas/report.md`

## R1: Structured Telemetry

Status: required next.

Deliverables:

- Add an internal `MotmEventLog` JSONL writer.
- Add `/motm dev eventlog reset`.
- Add `/motm dev eventlog tail`.
- Add `scripts/assert-motm-events.ps1`.
- Record event ids, player id, class, style, ability, position, target ids,
  asset ids, and result.

Acceptance:

- A Metal/Iron Wall run produces cast, terrain.place, push, terrain.restore, and
  cleanup events.
- A Magma/Lava Pool run produces cast, fluid/place-or-field, immunity, target
  damage, restore, and cleanup events.

## R2: Temporary Block Lifecycle Proof

Status: needed.

Test objects:

- iron/metal block wall
- stone pillar stack
- flower placed on top of grass
- sapling placed on top of grass
- cactus placed on surface or target fallback

Acceptance:

- appears in correct position
- does not replace support block unless explicitly allowed
- restores exactly
- no orphaned block after recast/death/disconnect/manual cleanup
- log includes all positions and restored block ids

## R3: Temporary Fluid Lifecycle Proof

Status: needed before continuing Magma/Mudpit/Hydro fields.

Test objects:

- small lava floor patch
- expanding lava ring
- small water/mud patch

Acceptance:

- caster/allies take no damage, fire, or unwanted slow
- enemies are affected
- fluid appears at caster-centered ground position
- cleanup completes
- no client crash after repeated casts

Decision:

- If a real fluid crashes twice in the same shape, stop using that shape and
  implement a visual field fallback for that ability.

## R4: Projectile/Proxy Separation Proof

Status: needed.

Test objects:

- Magma Sling blob
- cactus projectile
- gem shard/glass shard

Acceptance:

- projectile is not a living mob
- no health bar unless the concept asks for HP
- uses correct aim vector
- hits target/surface consistently
- despawns on hit/expiry
- logs contain no nonexistent role/model warnings

## R5: Movement Proof Scripts

Status: needed.

Scripts must set up the right scenario instead of just standing still:

- Stomp: arm, jump, land near grounded target.
- Frolick: move along safe lane, verify flowers behind player on top of blocks.
- Burrow: dash four blocks to a safe marked destination, pop up, hit targets.
- Dust Devil: start Sandstorm, dash through targets, drag/expel, end Sandstorm.
- Tunnel: enter block state, traverse, force surface recovery.

Acceptance:

- before/after positions logged
- third-person confirmed before visual proof
- world-entry screenshot checks death/void state first
- mob count reset before each ability
- safe lane prevents walking off floating platform

## R6: Spellbook/UI Scope

Status: design clarified, implementation pending.

User-approved scope:

- no story/journey/codex/grimoire framing
- production spellbook tabs:
  - Class
  - Style
  - Abilities
  - Perks
- dev/test variant:
  - same as production plus class/style switching for testing

Acceptance:

- user opens spellbook and sees only approved tabs
- dev/test variant is clearly separate from player-facing UI
- no resource-cost text remains while resources are disabled

