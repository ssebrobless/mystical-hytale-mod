# Hytale Implementation Readiness Checklist

Created: 2026-05-24

Purpose: turn the research and local probes into a concrete setup list that makes ability implementation less guessy. This document is the bridge between "the concept says X" and "the mod can reliably do X in Hytale."

## Working Shape

```
╔══════════════════════════════════════════════════════════════════════╗
║ Concept Intent                                                      ║
║ class/style/ability fantasy, controls, allies, timing, visuals       ║
╚══════════════════════════════════════════════════════════════════════╝
          │
          ▼
╔══════════════════════════════════════════════════════════════════════╗
║ Translation Layer                                                   ║
║ choose Hytale primitives: input, projectile, block, fluid, effect    ║
╚══════════════════════════════════════════════════════════════════════╝
          │
          ▼
╔══════════════════════════════════════════════════════════════════════╗
║ Capability Proof                                                    ║
║ isolated command or script proves the primitive works safely         ║
╚══════════════════════════════════════════════════════════════════════╝
          │
          ▼
╔══════════════════════════════════════════════════════════════════════╗
║ Ability Implementation                                              ║
║ code/data route using only proven APIs, assets, and cleanup rules    ║
╚══════════════════════════════════════════════════════════════════════╝
          │
          ▼
╔══════════════════════════════════════════════════════════════════════╗
║ Runtime Evidence                                                    ║
║ structured logs, screenshots/video, player review, residuals         ║
╚══════════════════════════════════════════════════════════════════════╝
```

## Concrete Setup List

### 1. Source Authority Map

Status: ready.

Needed:
- Repo concept docs and user-reviewed decisions identify what each class/style/ability is supposed to do.
- Public Hytale docs are directional only.
- Local `HytaleServer.jar`, local `Assets.zip`, and in-game runtime proof are final authority.

Why this matters:
- Public docs mention systems that may differ from the local pre-release build.
- Ability specs have moved across earlier 2D/3D/Hytale attempts, so every style needs a concept-to-primitive review before implementation.

### 2. Local Asset Index

Status: ready for lookup, not yet ergonomic enough for daily use.

Known counts from `audits/hytale-asset-library/2026-05-24-capability-atlas/report.md`:
- Assets.zip entries: 59,518
- particle assets: 2,320
- models: 2,815
- animations: 6,717
- block models: 1,151
- block/item JSON: 4,101
- prefabs: 7,823
- entity effects: 140
- UI files: 100

Needed next:
- Searchable per-category indexes in UTF-8:
  - `assets-particles.txt`
  - `assets-models.txt`
  - `assets-blocks.txt`
  - `assets-items.txt`
  - `assets-effects.txt`
  - `assets-ui.txt`
- A small "recommended assets by theme" table for Terra, Hydro, Aero, and Corruptus.
- A rule that every visual asset used by an ability must be either already referenced in the resolver or linked to an asset-index hit.

### 3. Local API Index

Status: partially ready.

Known counts from `audits/hytale-runtime-capabilities/2026-05-24-capability-atlas/report.md`:
- HytaleServer.jar entries: 38,672
- API class hits: 521

Confirmed local symbols:
- `ProjectileModule.get().spawnProjectile(...)` exists.
- `InteractionContext` exposes held item, target entity, target block, owning entity, and command buffer.
- `PlayerInteractEvent` exposes action type, held item, target block, target entity, and target ref.
- `InteractionType` includes `Primary`, `Secondary`, `Ability1`, `Ability2`, `Ability3`, `Use`, `SwapTo`, `SwapFrom`, `ProjectileHit`, `ProjectileMiss`, `ProjectileBounce`, and more.
- `BlockAccessor` supports `getBlock`, `setBlock`, `breakBlock`, `placeBlock`, and fluid reads.
- `SetBlockSettings` exposes flags for safe temporary block placement.
- Fluid classes exist, including `Fluid`, `FluidState`, and network fluid packets.

Needed next:
- Probe `com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig`, not only `com.hypixel.hytale.protocol.ProjectileConfig`.
- Probe fluid placement routes beyond block reads.
- Probe slot-swap ownership route because `SwitchActiveSlotEvent` public methods do not expose a player directly.
- Probe model/proxy role behavior for health bars, AI disabling, collision, and cleanup.

### 4. Structured Runtime Telemetry

Status: missing and now high priority.

Needed:
- A single MOTM event log writer, for example:
  - `%APPDATA%/Hytale/UserData/Logs/motm-events.jsonl`
  - or `audits/runtime/<timestamp>/motm-events.jsonl`
- Every important ability action emits structured lines:
  - `ability_cast`
  - `ability_hit`
  - `ability_visual_spawned`
  - `ability_visual_removed`
  - `status_applied`
  - `status_removed`
  - `block_placed`
  - `block_restored`
  - `fluid_placed`
  - `fluid_removed`
  - `projectile_spawned`
  - `projectile_hit`
  - `movement_adjusted`
  - `friendly_filter_skipped`
  - `cleanup_failure`

Why this matters:
- Screenshots prove appearance only at one moment.
- Text logs can prove "projectile hit target 2 after 412ms" or "wall removed after 4s" without relying on visual guesswork.
- This directly addresses the recurring issue where something looked plausible but mechanically was not actually firing, hitting, cleaning up, or ending.

### 5. Proof Command Library

Status: partially present.

Existing proof ids in the mod already cover many primitive families:
- Coatings:
  - `coating-metal`
  - `coating-obsidian`
  - `coating-stone`
  - `coating-poison-target`
- Temporary blocks:
  - `tempblock-metal-wall`
  - `tempblock-stone-pillar`
  - `tempblock-flower`
  - `tempblock-sapling`
  - `tempblock-gem-cluster`
  - `tempblock-cactus`
  - `tempblock-roots`
- Temporary fluids:
  - `tempfluid-lava-ring`
  - `tempfluid-water-field`
  - `tempfluid-mud-field`
- Proxies/models:
  - `proxy-magma-blob`
  - `proxy-cactus-projectile`
  - `proxy-gem`
  - `proxy-gem-aura`
  - `proxy-glass-shards`
  - `proxy-sand-cloud`
  - `proxy-debris-wave`
- Movement:
  - `movement-burrow`
  - `movement-tunnel`
  - `movement-dust-devil`

Needed:
- Proof commands must emit structured telemetry.
- Proof commands must include cleanup assertions.
- Proof commands must support third-person screenshot checkpoints.
- Proof commands must be mapped to every ability before implementation begins.

### 6. Input And Control Proofs

Status: partial.

Known:
- `PlayerInteractEvent` and mouse button events are already registered in the mod.
- Spellbook slots can be cast through existing routing.
- Local `InteractionType` includes ability and item-swap concepts.

Needed:
- A proof that intended controls trigger the same code path as `/motm dev test ability`.
- A proof for abilities that require:
  - moving before cast
  - jumping/landing after cast
  - swapping from spellbook to weapon/tool
  - using melee attacks
  - mining blocks
  - being hit by enemies
  - standing inside a field
- A style-specific manual test script so the player only needs to focus the Hytale window and use the intended controls.

### 7. Projectile Capability

Status: partially researched, not proven enough.

Confirmed:
- Local `ProjectileModule` exists.
- Public docs describe `ProjectileModule.get().spawnProjectile(...)` and projectile configs.
- The current mod has a custom projectile tick/proxy path.

Gap:
- Magma Sling showed why the custom model/proxy route can look like a mob, miss visually, or not align with aim.
- We still need an isolated first-class projectile proof using the local server-side `ProjectileConfig` class.

Needed:
- Build `projectile-proof-magma-blob`:
  - launches from player aim
  - has clear visual
  - hits a stationary target
  - emits `projectile_spawned`, `projectile_hit`, `projectile_removed`
  - does not spawn as a living target with health bar or AI
- If first-class projectiles cannot carry the desired model, use a hybrid:
  - invisible/vanilla projectile for mechanics
  - synced visual proxy for appearance
  - structured logs proving both stay aligned.

### 8. Temporary Block Lifecycle

Status: partially proven.

Known:
- `BlockAccessor.setBlock(...)` exists.
- The current mod already uses temporary block placement/restoration proof commands.
- Existing cleanup registries track active proof selections and active proof proxies.

Needed:
- A reusable temporary-block service with ownership:
  - ability id
  - caster id
  - original block state
  - intended restore time
  - forced cleanup command
- Placement policy:
  - never replace the block under the player for flowers/roots/trails
  - place decorative objects on top of the support block
  - reject unsafe positions over void
  - clamp walls/pillars to valid ground
- Structured cleanup logs:
  - placed count
  - restored count
  - failed restore count
  - positions.

### 9. Temporary Fluid Lifecycle

Status: high-risk partial.

Known:
- Fluid assets and classes exist.
- `BlockAccessor` can read fluid ids and levels.
- `Fluid` includes damage, FX id, light, particle color, and interactions.
- The current mod can create lava/water-like selections through existing code.

Gaps:
- Public `BlockAccessor` does not expose an obvious direct `setFluid(...)`.
- Real lava causes vanilla slow/damage/fire/visual effects unless explicitly compensated or avoided.
- Crashes occurred around large shell/block/fluid visuals, including "Index was outside the bounds of the array."

Needed:
- Isolate fluid placement into a proof command:
  - one safe ring
  - one cleanup pass
  - no player slow/damage/fire for caster
  - logs exact placed/restored cells.
- Decide per ability:
  - true fluid blocks when gameplay needs water/lava behavior
  - block/proxy/particle fake fluid when vanilla physics causes too much side effect.
- Cap shell/ring sizes and never spawn geometry around the camera without bounds checks.

### 10. EntityEffect And Coating Palette

Status: partial but promising.

Known:
- Metal Coat discovered a high-value stone-skin-style coating route.
- User approved the bug-derived VFX look for Metal Coat and wants that tactic reused with different colors.
- EntityEffect docs and local assets support model/effect/tint concepts directionally.

Gaps:
- True block-texture coating is not proven.
- Color manipulation of the stone-skin-style VFX is not fully proven across metal, stone, obsidian, poison, and water shields.

Needed:
- A coating palette proof table:
  - metal: dark gray
  - stone: lighter gray
  - obsidian: midnight purple-black
  - poison: light purple smoke/body hug
  - hydro barrier: bubble layer
  - root/vine: lower-body tether.
- Each palette entry gets screenshot and structured event proof.
- If tint cannot reach a desired color, document fallback:
  - VFX overlay
  - particles close to body
  - held-item proxy
  - actual armor/item visual is not guaranteed.

### 11. Model And Proxy Resolver

Status: partial.

Known:
- Resolver references currently resolve against local asset indexes.
- Proxy commands exist for magma blob, cactus, gem, shards, sand, and debris.

Gaps:
- Some proxies look like real mobs, have health bars, or interact like entities.
- Need a clean distinction between:
  - visual-only proxy
  - targetable summoned object
  - projectile visual
  - temporary block structure.

Needed:
- Resolver table for each proxy use:
  - asset id
  - role id
  - health-bar visibility
  - AI disabled
  - collision behavior
  - lifetime
  - cleanup owner.
- Fallback rule:
  - if a visual-only proxy cannot hide health bars or AI, prefer particles/blocks or first-class projectile mechanics plus a minimal visual.

### 12. Movement And Safety Harness

Status: partial.

Known:
- Movement abilities require actual player actions, not stationary screenshots.
- User observed tests failing because the player walked off a floating platform or stayed still when movement was required.

Needed:
- Safe arena:
  - wide ground area
  - no void edge near test lane
  - lanes for dash/projectile tests
  - marked origin and target pads
  - cleanup command before every ability.
- Test modes:
  - stationary target
  - cluster target
  - moving player
  - jump/landing
  - melee/tool use
  - survival/adventure damage validation
  - creative visual-only validation.
- Before every run:
  - screenshot spawn state
  - confirm alive
  - confirm not in void
  - confirm third person when visual review needs it
  - clear mobs and previous blocks/fluids/proxies.

### 13. Friendly Filtering

Status: needs audit.

Rule:
- Passive abilities, fields, damage, debuffs, slows, roots, knockback, and hazards must not negatively affect:
  - caster
  - allies
  - friendly summons.

Needed:
- Central helper:
  - `isHostileTarget(caster, target)`
  - `isFriendlyTarget(caster, target)`
  - `isCasterOwnedSummon(caster, target)`
- Every AoE and lingering field must call the helper before applying harm.
- Structured logs must record friendly skips.

### 14. Cleanup Ownership Registry

Status: partial.

Needed:
- One registry for all temporary owned things:
  - blocks
  - fluids
  - proxies
  - status effects
  - action windows
  - movement modifiers
  - spawned test mobs.
- Cleanup triggers:
  - ability end
  - caster death
  - target death
  - world disconnect
  - class/style change
  - manual `/motm dev cleanup`
  - acceptance test start.

Why this matters:
- Stuck lava pools, lingering slows, persistent coatings, and leftover mobs are all cleanup/ownership failures.

### 15. Test World Harness

Status: partial.

Needed:
- Mob count check before spawning.
- Mob clear before each ability.
- Optional stationary/rooted test mob command.
- Target layouts:
  - single grounded mob
  - target plus surrounding mobs
  - floating target
  - dummy at known distance
  - melee durability target
  - block-mining strip.
- Inventory setup per style:
  - spellbook
  - melee weapon
  - pickaxe
  - shield
  - test blocks/items only when needed.

### 16. Spellbook UI Scope

Status: known pending work.

Production UI should include only:
- Class
- Style
- Abilities with descriptions
- Perks

Dev/test UI can additionally include:
- class switch
- style switch
- ability trigger helpers
- cleanup/reset
- test inventory setup
- audit start/stop.

Remove from production spellbook:
- Journey (completed: replaced by Class)
- Codex (completed: hidden/removed from production navigation)
- Grimoire/story framing (completed: replaced by Abilities)
- unrelated story tabs.

### 17. Style Review Runbook

Status: needed before broad Terra continuation.

Each style needs:
- intended controls
- whether it requires creative, survival, or adventure
- required items
- required target layout
- visual acceptance notes
- mechanical acceptance logs
- cleanup expectations
- residual categories.

Example: Metal
- Iron Wall: cast in front of player, 3x3 wall, pushes overlapping enemies, removes after 4s, cooldown begins after wall removal.
- Metal Coat: body/held-item coating VFX, no resource cost, clear duration/end.
- Alloy Enhancement: next valid melee/tool action starts the three-use buff, buff ends after three uses or item swap, damage/durability logs prove effect.

Example: Magma
- Magma Sling: real projectile path from aim, visible lava/blob projectile, hit/removal logs.
- Obsidian Skin: safe lava/obsidian encase visual, immobilizes during shell, applies purple-black coating after release, no camera-crash geometry.
- Lava Pool: caster-centered ground pool, visible, hostile-only harm/slow, caster speed compensation and no caster damage/fire.

### 18. Residual Register And Decision Tables

Status: partial.

Needed:
- Every non-blocking issue goes into `CODEX_PHASE9_RESIDUALS.md` or the relevant audit report.
- Every repeated issue becomes a decision table row:
  - symptom
  - likely cause
  - evidence command
  - allowed fix
  - stop condition.

## Readiness Matrix

```
┌─────┬───────────────────────────────┬──────────────┬───────────────────────────────┐
│ ID  │ Capability                     │ Status       │ Next Gate                     │
├─────┼───────────────────────────────┼──────────────┼───────────────────────────────┤
│ R0  │ Source authority map           │ Ready        │ Keep current                  │
│ R1  │ Asset index                    │ Ready        │ Add theme recommendations     │
│ R2  │ API index                      │ Partial      │ Probe projectile/fluid/slot   │
│ R3  │ Structured telemetry           │ Missing      │ Add JSONL event writer        │
│ R4  │ Intended input routing         │ Partial      │ Prove controls vs commands    │
│ R5  │ Projectile mechanics           │ Gap          │ First-class projectile proof  │
│ R6  │ Temporary blocks               │ Partial      │ Ownership service             │
│ R7  │ Temporary fluids               │ High risk    │ Isolated safe-fluid proof     │
│ R8  │ EntityEffect coating palette   │ Partial      │ Per-color palette proof       │
│ R9  │ Model/proxy visuals            │ Partial      │ Healthbar/AI/collision proof  │
│ R10 │ Movement/action tests          │ Partial      │ Style-specific runbooks       │
│ R11 │ Friendly filtering             │ Needs audit  │ Central hostile/friendly rule │
│ R12 │ Cleanup registry               │ Partial      │ Single owner cleanup service  │
│ R13 │ Spellbook UI scope             │ Pending      │ Production/dev split          │
└─────┴───────────────────────────────┴──────────────┴───────────────────────────────┘
```

## Research Findings Applied

Public docs/community research:
- Hytale projectile docs describe `ProjectileModule.get().spawnProjectile(...)`, but local jar signatures must govern implementation.
- Custom UI docs confirm server-driven `.ui` pages are the correct path for spellbook pages, while client main menu/inventory are not the right target.
- EntityEffect docs support model/effect/tint-style composition, but exact visuals must be proven in the local runtime.
- Community docs explicitly recommend browsing/decompiling `HytaleServer.jar`, which matches the local-jar-first workflow.
- Block docs show blocks have rich behavior fields, but temporary ability structures must still be validated through local APIs.

Local jar findings:
- First-class projectile APIs exist, so Magma Sling and future projectile abilities should stop relying only on mob-like proxies until that path has been tested.
- Interaction events contain enough information for target/held-item routing, but slot-swap cleanup needs deeper proof.
- Block placement APIs are available and should be the default for walls, pillars, flowers, roots, saplings, gems, cactus, and trails.
- Fluid reads and fluid asset classes exist, but write/removal safety needs isolated proof before broad lava/water/mud ability use.

## Highest-Value Gaps To Close Next

1. Structured telemetry.
   - Without this, we keep guessing from screenshots.
   - This should be implemented before another serious Terra style pass.

2. First-class projectile proof.
   - Magma Sling is currently the clearest example of why the custom proxy path is not enough.
   - A successful projectile proof will benefit cactus, sapling, nightshade, gem shards, icicle, flame, wind blade, and many future abilities.

3. Safe fluid proof.
   - Lava Pool, Mud Pit, hydro fields, geysers, and water/rain effects all depend on knowing when to use real fluids versus fake visuals.

4. Coating palette proof.
   - Metal Coat taught us a strong reusable tactic.
   - Stone, obsidian, poison, hydro barrier, and future defensive effects need color-specific proof.

5. Cleanup ownership service.
   - Lingering pools, stuck slows, persistent visual effects, leftover mobs, and animation locks all point to ownership cleanup debt.

6. Style-specific test runbooks.
   - Every ability should have a planned movement/action/target setup before testing.
   - This avoids tests where the player stands still for movement abilities or spawns too many moving targets.

## Recommended Execution Order

```
╔══════════════════════════════════════════════════════════════════════╗
║ Capability Hardening Before More Broad Style Implementation          ║
╠════╦══════════════════════════════════════════════╦══════════════════╣
║ 1  ║ Add structured MOTM event telemetry          ║ R3               ║
║ 2  ║ Add API probe scripts for projectile/fluid   ║ R2, R5, R7       ║
║ 3  ║ Build first-class projectile proof           ║ R5               ║
║ 4  ║ Build safe fluid lifecycle proof             ║ R7               ║
║ 5  ║ Build coating palette proof                  ║ R8               ║
║ 6  ║ Centralize temp object cleanup ownership     ║ R6, R12          ║
║ 7  ║ Add friendly filtering audit                 ║ R11              ║
║ 8  ║ Write Terra style runbooks                   ║ R10, R17         ║
║ 9  ║ Resume Terra implementation style by style   ║ all above        ║
╚════╩══════════════════════════════════════════════╩══════════════════╝
```

## Definition Of "Enough Information"

An ability is ready to implement when all of these are true:
- The user-reviewed concept is written down.
- Each visual requirement maps to a real asset, particle, block, fluid, UI element, EntityEffect, or proxy.
- Each mechanic maps to a local jar API or already-working mod primitive.
- Every temporary object has an owner and cleanup rule.
- Friendly filtering is explicit.
- Intended controls are part of the test, not only dev commands.
- Runtime telemetry can prove cast, hit, effect, cleanup, and failure states.
- Screenshots/video are supporting evidence, not the only evidence.

If any one of those is missing, the ability enters research/proof mode before it enters implementation mode.
