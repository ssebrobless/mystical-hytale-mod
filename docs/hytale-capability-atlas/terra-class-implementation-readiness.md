# Terra Class Implementation Readiness

Date: 2026-05-24

Purpose: decide whether the Terra class can be fully implemented as intended, with no ability resource costs, using only Hytale mechanisms that are documented, locally discoverable in `HytaleServer.jar`, present in `Assets.zip`, or already proven in the MOTM runtime.

This is not a promise that every visual is already solved. It is the map for getting Terra from "partly working prototypes" to "implemented with the right Hytale primitive for each ability."

## Authority Stack

```
╔══════════════════════════════════════════════════════════════════════╗
║ TERRa IMPLEMENTATION TRUTH                                         ║
╠══════════════════╦═══════════════════════════════════════════════════╣
║ 1. User concepts ║ CODEX_CONCEPT_REVIEW_DECISIONS_2026-05-22.md     ║
║ 2. Local data    ║ src/main/resources/data/styles/terra_styles.json  ║
║ 3. Local runtime ║ GameplayPlaybackManager + command/test harness    ║
║ 4. Local API     ║ HytaleServer.jar javap signatures                 ║
║ 5. Local assets  ║ Assets.zip inventory + runtime loaded assets      ║
║ 6. Web docs      ║ Directional only; never overrides local jar        ║
╚══════════════════╩═══════════════════════════════════════════════════╝
```

Current Terra JSON already has all ability `resource_cost` values at `0` and blank style `resource_type` values. The remaining resource-removal work is therefore not mostly "cost field" work; it is replacing resource-dependent gameplay concepts with duration, cooldown, charge, distance, or target-count rules.

## Research Result

Hytale gives us enough surface area to finish Terra, but not by guessing. The correct route is to build a small set of runtime-proven primitives and then express every Terra ability through those primitives.

Public docs confirm the broad systems:

- Projectiles are a first-class asset/API system.
- Entity effects and model VFX are asset-driven visual/status layers.
- Server-side UI can be customized and driven by server callbacks.
- Trails exist as assets for moving entities, projectiles, and weapon motion.

Local evidence is stronger:

- Runtime logs show loaded assets including `ProjectileConfig`, `Projectile`, `Trail`, `ModelVFX`, `EntityEffect`, `Fluid`, and `BlockBreakingDecal`.
- `ProjectileModule.get().spawnProjectile(...)` exists in the local jar.
- `Trail`, `ModelVFX`, `BlockBreakingDecal`, fluid, block-selection, and input event classes exist in the local jar.
- MOTM already has temporary block/fluid placement, restoration, proxy visuals, body coatings, movement proofs, and ability test commands.

The conclusion: Terra is implementable, but Magma, Self Petrification, Soil, Sand, and parts of Bloom/Gem require one more proof pass before I should harden their final implementations.

## Core Primitives

```
╔════════════════════╦════════════╦════════════════════════════════════╗
║ Primitive          ║ Readiness  ║ Terra Uses                         ║
╠════════════════════╬════════════╬════════════════════════════════════╣
║ Temporary blocks   ║ HIGH       ║ Iron Wall, Pillar, Gem, flowers    ║
║ Temporary fluids   ║ MEDIUM     ║ Lava Pool, Mudpit, Obsidian shell  ║
║ Entity/Model VFX   ║ HIGH       ║ Metal, stone, obsidian coatings    ║
║ First-class proj.  ║ MEDIUM     ║ Magma Sling, Sapling, Cacti        ║
║ Proxy visuals      ║ MEDIUM     ║ Gem object, aura, fallback visuals ║
║ Trails             ║ MEDIUM     ║ Debris, sand, projectile streaks   ║
║ Block decals       ║ LOW-MED    ║ Quake cracks, Sinkhole marker      ║
║ Movement control   ║ MEDIUM     ║ Burrow, Dust Devil, Tunnel         ║
║ Item-bound buffs   ║ MEDIUM     ║ Alloy, Rubble Rouser               ║
║ Friendly filtering ║ MEDIUM     ║ All AoEs, fields, summons          ║
║ Structured logs    ║ NEEDED     ║ All acceptance tests               ║
╚════════════════════╩════════════╩════════════════════════════════════╝
```

Readiness means:

- `HIGH`: already locally proven enough to implement, though tuning may remain.
- `MEDIUM`: API/assets exist and partial MOTM code exists, but one focused runtime proof should precede final Terra wiring.
- `LOW-MED`: assets/classes exist, but the exact spawn/control route is still unproven.

## Resource-Free Design

Terra can move to no ability resource costs cleanly if these concepts replace materials:

```
╔════════════════════╦══════════════════════╦══════════════════════════╗
║ Former resource    ║ Replacement gate     ║ Needed user decision     ║
╠════════════════════╬══════════════════════╬══════════════════════════╣
║ Stone for Tunnel   ║ Duration + distance  ║ Max duration/distance    ║
║ Sand for Sandstorm ║ Duration + cooldown  ║ Does Dust Devil end it?  ║
║ Blocks for fields  ║ Cooldown + lifespan  ║ Field duration tuning    ║
║ Metal for Alloy    ║ 3-use item binding   ║ Already user-approved    ║
║ Plants/seeds/gems  ║ Target/object limit  ║ Mostly concept-approved  ║
╚════════════════════╩══════════════════════╩══════════════════════════╝
```

User-approved defaults before coding:

- `Tunnel`: duration-gated form with no material consumption. When the timer ends, pull the player up to a safe surface so they cannot remain stuck in terrain. Follow-up research/proof should check whether "exit into discovered cave" can be supported safely by choosing the nearest valid air pocket instead of always forcing the full world surface.
- `Sandstorm`: 10 second active duration. It can also be manually deactivated.
- `Sandstorm cooldown`: 2 second cooldown window after manual deactivation or natural expiry.
- `Dust Devil`: can only be used while Sandstorm is active. If Sandstorm is not active, Dust Devil fails. Dust Devil keeps its combo identity and should end/consume the active Sandstorm when it performs the expel.
- `Vitrification`: normal cooldown buff/debuff layer, no resource dependency.
- `Lapidary`: one active gem at a time, recall/recast replaces old gem.

`Tunnel`, `Sandstorm`, and `Dust Devil` are now conceptually approved for no-resource implementation. The remaining uncertainty is technical proof, not design: safe tunnel exit, Sandstorm toggle state, and Dust Devil's active-Sandstorm precondition/consume behavior must be verified in runtime logs and user visual review.

## Style Readiness Matrix

```
╔════════════════════╦══════════╦══════════════════════════════════════╗
║ Style              ║ Status   ║ Main remaining proof/fix             ║
╠════════════════════╬══════════╬══════════════════════════════════════╣
║ Quake              ║ READY-   ║ Make Sinkhole marker more readable   ║
║ Metal              ║ READY-   ║ Finalize alloy cleanup/item coating  ║
║ Magma              ║ BLOCKED  ║ Projectile, safe lava, shell crash   ║
║ Stone              ║ PARTIAL  ║ Pillar proof + arm/item coating      ║
║ Arbor              ║ PARTIAL  ║ One-target Vines + surface roots     ║
║ Bloom              ║ PARTIAL  ║ Frolick surface placement + cactus   ║
║ Self Petrification ║ RISKY    ║ Tunnel movement safety               ║
║ Soil               ║ RISKY    ║ Burrow movement + mud safety         ║
║ Sand               ║ PARTIAL  ║ Sand-colored cloud/trail + combos    ║
║ Gem                ║ PARTIAL  ║ HP/readability + gem-centered AoEs   ║
╚════════════════════╩══════════╩══════════════════════════════════════╝
```

`READY-` means final polish remains, but no unknown Hytale capability is blocking the style.

## Per-Ability Implementation Plan

### Quake

| Ability | Intended result | Best primitive | Readiness |
|---|---|---|---|
| Stomp | Arm next jump; landing creates AoE crack/flash | existing armed-stomp + particle ring + optional decal | READY- |
| Aftershock | 8 block spherical tremor with flash/cracks | particle ring + server AoE | READY- |
| Sinkhole | target appears buried; cracked ground + brown dust | root/status + body tint + ground particles/decals | READY- |

Needed work: make Sinkhole test/visual more legible by logging victim id, ground anchor, effect duration, and cleanup; use repeated block-break particles and possibly block-breaking decal if proof succeeds.

### Metal

| Ability | Intended result | Best primitive | Readiness |
|---|---|---|---|
| Iron Wall | 3x3 metal barricade one block in front, pushes enemies away | temporary block selection | READY |
| Metal Coat | strong dark gray full-body coating, item can share coating | ModelVFX/EntityEffect coating | READY- |
| Alloy Enhancement | next 3 melee/tool actions boosted; item coating; ends on charges or item swap | follow-up state + active item id + coating | READY- |

Needed work: keep the "good bug" coating behavior as a named coating primitive, bind Alloy to the first eligible physical melee/tool action, remove on 3 uses or item swap, and emit structured `alloy-charge-used` logs.

### Magma

| Ability | Intended result | Best primitive | Readiness |
|---|---|---|---|
| Lava Pool | visible lava field from caster body; caster/allies not slowed/damaged | safe fluid selection + caster compensation or fake lava blocks | BLOCKED |
| Obsidian Skin | short lava box/root, then dark purple-black stone-skin coating | temporary block shell + ModelVFX coating | BLOCKED |
| Magma Sling | lava blob projectile that flies/hits/despawns | first-class projectile preferred | BLOCKED |

Needed proofs before final code:

- First-class projectile proof for a lava-looking `ProjectileConfig`, not a living NPC proxy.
- Safe-lava proof: either actual lava with caster speed/fire/damage immunity, or non-fluid lava-like blocks/visuals if native lava remains hostile.
- Obsidian shell proof with camera-safe geometry to avoid the client "index outside bounds" crash.

### Stone

| Ability | Intended result | Best primitive | Readiness |
|---|---|---|---|
| Rubble Rouser | arms coated like stone-block material; melee focus | coating + item/unarmed event proof | PARTIAL |
| Pillar Strike | 1x1x4 stone pillar rapidly stacks under target, launches/stuns, disappears 0.6s after full height | staged temporary block column | PARTIAL |
| Rockslide | stone/dust forward control hit | projectile or trail/particle wave | PARTIAL |

Needed work: use staged column selection for Pillar Strike with target-centered logs; prove whether item/body coating can isolate arms or approximate with full-body/held-item VFX.

### Arbor

| Ability | Intended result | Best primitive | Readiness |
|---|---|---|---|
| Rooted | roots/vines on top of ground at player feet, no block destruction | surface decoration blocks + root status | PARTIAL |
| Vines | one target at a time; old target releases; vanish on death | target state + surface/root visual + EntityRemoveEvent | PARTIAL |
| Sapling | projectile lands on ground and spawns sapling marker | projectile ground impact + surface block | PARTIAL |

Needed work: centralize "place on top of support block" anchoring so roots/saplings never replace the block under a player.

### Bloom

| Ability | Intended result | Best primitive | Readiness |
|---|---|---|---|
| Nightshade | pass-through projectile lands on surface, flower lures then poison explodes | projectile + flower block/proxy + taunt field | PARTIAL |
| Frolick | movement leaves flowers on top of ground behind player | surface decoration trail + movement sampler | PARTIAL |
| Cacti Cluster | slow cactus projectile sticks, DoT, then AoE DoT/slow | first-class projectile or proxy + attachment state | PARTIAL |

Needed work: fix Frolick from "replace support block" to "place flower above support block"; add ability-specific test that requires actual movement and records sample positions.

### Self Petrification

| Ability | Intended result | Best primitive | Readiness |
|---|---|---|---|
| Gargoyle | stone coating, cancellable, 6s cooldown after end | ModelVFX/EntityEffect + status | PARTIAL |
| Glare | target stone coating while petrified, then 2s slow after release | target effect + slow status | PARTIAL |
| Tunnel | player becomes stone block and moves through ground; safe auto-surface | movement state + temporary visual + collision/surface guard | RISKY |

Resource-free change needed: replace stone-block consumption with duration + distance cap. Tunnel must never leave player inside terrain; this needs a dedicated movement safety proof.

### Soil

| Ability | Intended result | Best primitive | Readiness |
|---|---|---|---|
| Burrow | drop down, dash 4 blocks, pop up; damage/knockback on exit | movement dash + surface re-emerge visual | RISKY |
| Mudpit | expanding brown water-like pool; Hydro synergy; caster/allies not slowed | safe fluid/field + brown visual overlay | RISKY |
| Debris | forward brown dust/smoke wave | trail + particles + server hit sweep | PARTIAL |

Needed work: safe-movement proof for Burrow on floating platform and flatlands; safe fluid/field proof for Mudpit before using real water/fluids broadly.

### Sand

| Ability | Intended result | Best primitive | Readiness |
|---|---|---|---|
| Sandstorm | beige-yellow cloud around player showing radius; 10s toggle/duration; 2s cooldown after end | active state + trails/particles/ring blocks | PARTIAL |
| Dust Devil | only usable while Sandstorm is active; dash, drag enemies, expel, ends Sandstorm | movement + pull/knockback + active storm state | RISKY |
| Vitrification | combo layer with other sand abilities | status + visual overlay | PARTIAL |

Resource-free decision is now made: Sandstorm is a 10 second toggle with a 2 second post-end cooldown, and Dust Devil requires active Sandstorm. Visual proof should test whether `Trail` assets solve the "white smoke" problem better than plain particles.

### Gem

| Ability | Intended result | Best primitive | Readiness |
|---|---|---|---|
| Lapidary | persistent controllable 2x2x2 floating green cube, HP bar, recall | temporary blocks + proxy/nameplate + HP state | PARTIAL |
| Fracture | expanding green sphere/circle from gem; no allies/caster | gem-centered AoE + particle/proxy sphere | PARTIAL |
| Refraction | bright aura sphere around gem radius | aura proxy/particles around gem | PARTIAL |

Needed work: improve HP/readability by logging gem id/HP and verifying nameplate/proxy visibility; use green assets if present, else make all gem visuals match chosen available crystal/gem color.

## Required Proof Gates Before Full Terra Rewrite

```
╔══════╦════════════════════════════╦══════════════════════════════════╗
║ Gate ║ Name                       ║ Pass condition                    ║
╠══════╬════════════════════════════╬══════════════════════════════════╣
║ G1   ║ Structured ability logs    ║ cast/hit/cleanup lines per cast   ║
║ G2   ║ First-class projectile     ║ projectile flies, hits, despawns  ║
║ G3   ║ Coating palette            ║ metal/stone/obsidian visibly vary ║
║ G4   ║ Surface decoration         ║ flowers/roots never replace floor ║
║ G5   ║ Safe fluid/field           ║ caster not slowed/damaged/burned  ║
║ G6   ║ Shell safety               ║ no Obsidian Skin client crash      ║
║ G7   ║ Trail/debris visual        ║ sand/debris read as material       ║
║ G8   ║ Movement safety            ║ burrow/tunnel/dash cannot strand   ║
║ G9   ║ Friendly filter            ║ caster/allies/summons skipped      ║
║ G10  ║ Item buff lifecycle        ║ Alloy/Rubble end on correct rules  ║
╚══════╩════════════════════════════╩══════════════════════════════════╝
```

I should not claim "Terra complete" until these gates pass under the in-game controls, not only `/motm dev test ability`.

## Better Test Harness Requirements

The current screenshot-based audits are not enough for Terra. Terra needs text-first proof plus visual evidence:

- Every ability emits one-line structured events:
  - `terra.cast`
  - `terra.hit`
  - `terra.visual.spawn`
  - `terra.visual.cleanup`
  - `terra.status.apply`
  - `terra.status.remove`
  - `terra.friendly.skip`
  - `terra.error`
- Every style test starts from a cleaned arena:
  - no leftover mobs beyond declared test count
  - no leftover temporary blocks/fluids/proxies/effects
  - third-person confirmed before visual captures
  - player location/orientation logged
- Movement tests must run in a safe lane, not near a floating-platform edge.
- Projectile tests need a target line and a miss/hit report.
- Field tests need a caster-in-field check and an enemy-in-field check.
- Item tests need equipped item id, first eligible item id, charge count, damage numbers, and cleanup on swap.

## Implementation Order

```
╔════╦══════════════════════════════════════════════════════════════════╗
║ 1  ║ Add structured Terra telemetry and cleanup audit command         ║
║ 2  ║ Prove first-class projectiles for Magma/Cactus/Sapling candidates║
║ 3  ║ Fix surface placement for roots/flowers/saplings                 ║
║ 4  ║ Prove coating palette and preserve the strong metal-coat tactic  ║
║ 5  ║ Prove safe lava/water/mud or choose visual-only field fallback   ║
║ 6  ║ Prove Obsidian shell with crash-safe geometry                    ║
║ 7  ║ Prove trails/particles for sand/debris readability               ║
║ 8  ║ Prove movement safety for Tunnel/Burrow/Dust Devil               ║
║ 9  ║ Implement Terra styles one at a time with no resource costs      ║
║ 10 ║ Run user-control review plus autonomous log/evidence audit       ║
╚════╩══════════════════════════════════════════════════════════════════╝
```

This order intentionally fixes the primitives that many styles share before polishing individual abilities.

## Sources Used

Local:

- `audits/terra-research/2026-05-24/manifest.json`
- `audits/terra-research/2026-05-24/api-signatures/`
- `docs/hytale-capability-atlas/research-completeness-audit.md`
- `docs/hytale-capability-atlas/implementation-readiness-checklist.md`
- `CODEX_CONCEPT_REVIEW_DECISIONS_2026-05-22.md`
- `src/main/resources/data/styles/terra_styles.json`
- `src/main/resources/data/classes/terra.json`
- `src/main/java/com/motm/manager/GameplayPlaybackManager.java`

Web:

- https://hytale.com/news/2025/11/hytale-modding-strategy-and-status
- https://hytalemodding.dev/en/docs/established-information/server/interface/ui-customization
- https://hytale-docs.pages.dev/modding/systems/projectiles/
- https://doctale.dev/asset-development/vfx/trails/
- https://doctale.dev/asset-development/vfx/model-effects/
- https://hytalemodding.dev/en/docs/guides/plugin/browsing-serverjar
- https://hytale-docs.dev/

## Bottom Line

Yes: Terra looks implementable in the way the user intends, but the correct next move is not to keep tuning ability code blindly. The next move is to harden the Terra primitive layer with proofs for projectiles, coatings, safe fields, surface placement, movement safety, and structured telemetry. Once those pass, the ten Terra styles can be implemented systematically without resource costs and with much less back-and-forth.
