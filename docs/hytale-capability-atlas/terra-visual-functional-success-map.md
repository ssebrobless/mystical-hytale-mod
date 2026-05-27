# Terra Visual and Functional Success Map

Date: 2026-05-24

Purpose: define what "Terra is complete" means in concrete visual, functional, implementation, and audit terms. This file is the execution contract before finishing Terra style implementation with no resource costs.

## Success Shape

```
╔════════════════════════════════════════════════════════════════════════╗
║                          TERRA DONE                                  ║
╠═══════════════╦════════════════════════════════════════════════════════╣
║ Concept       ║ Matches user-reviewed intent for all 10 styles        ║
║ Function      ║ Ability mechanics work through normal player controls ║
║ Visual        ║ Main visual is object/block/model first when relevant ║
║ Safety        ║ Caster, allies, allied summons are never harmed        ║
║ Resources     ║ No ability consumes material/resource costs           ║
║ Cleanup       ║ No lingering blocks, fluids, proxies, mobs, effects   ║
║ Evidence      ║ Logs prove cast, hit, skip, cleanup, and timings      ║
║ Review        ║ User visual review passes in third-person testing     ║
╚═══════════════╩════════════════════════════════════════════════════════╝
```

Terra is not complete if an ability only works through `/motm dev test ability`, only looks right from a still screenshot, leaves terrain/effects behind, harms the caster, or uses a living NPC as a projectile where Hytale's projectile system can do the job.

## Source Truth

```
User concept decisions
  └─▶ CODEX_CONCEPT_REVIEW_DECISIONS_2026-05-22.md
        └─▶ Terra data and code
              ├─▶ src/main/resources/data/styles/terra_styles.json
              ├─▶ src/main/resources/data/classes/terra.json
              └─▶ src/main/java/com/motm/manager/GameplayPlaybackManager.java
                    └─▶ Local Hytale contract
                          ├─▶ Assets.zip
                          ├─▶ HytaleServer.jar javap signatures
                          └─▶ cold-launch runtime logs
```

Public docs are useful for discovering systems. Local jar/assets/logs decide exact API names, asset IDs, and whether an implementation really works in this Hytale build.

## Research Conclusions

The research supports a confident Terra implementation, with a primitive-first strategy:

- Use Hytale `ProjectileModule.spawnProjectile(...)` for real projectile abilities where possible. Public docs and local signatures both confirm it exists.
- Use temporary block selections for physical structures: Iron Wall, Pillar Strike, Lapidary cube, flowers, roots, saplings, cactus markers.
- Use `EntityEffect` plus `ModelVFX` and tint values for tight body/item coatings. This is the proven route for metal, stone, obsidian, and poison body effects.
- Use temporary fluid/block fields only after a safety proof. Real lava/water can have native movement/damage side effects, so the field layer must compensate or use a visual-only fallback.
- Use trails and particles as motion/readability overlays for debris, sand, weapon swings, impact frames, and projectile streaks.
- Use structured text logs as the main audit evidence; screenshots/video are visual support, not the source of truth.

## Global Terra Rules

```
╔════════════════════╦═══════════════════════════════════════════════════╗
║ Rule               ║ Required Behavior                                ║
╠════════════════════╬═══════════════════════════════════════════════════╣
║ No resources       ║ Costs stay 0; use duration/cooldown/charges       ║
║ Friendly safety    ║ Skip caster, allies, allied summons for negatives ║
║ Surface placement  ║ Flowers/roots/saplings go on top, not in floor    ║
║ Physical objects   ║ Blocks/models first, particles second             ║
║ Coatings           ║ Tight body/item VFX, not loose smoke clouds       ║
║ Projectiles        ║ First-class projectile before NPC proxy fallback  ║
║ Movement           ║ Never strand player inside terrain or off arena   ║
║ Cleanup            ║ Every spawned object has owner, reason, expiry    ║
╚════════════════════╩═══════════════════════════════════════════════════╝
```

## Implementation Primitives

| Primitive | Ideal Use | Terra Abilities | Readiness |
|---|---|---|---|
| Temporary block selection | Real world objects and markers | Iron Wall, Pillar, flowers, roots, sapling, cactus, Lapidary | High |
| Surface decoration anchor | Safe placement on top of ground | Rooted, Vines, Sapling, Frolick, Nightshade | Needs cleanup pass |
| Staged block stack | Fast "rises from ground" effect | Pillar Strike | High |
| Temporary fluid/field | Lava Pool, Mudpit | Magma, Soil | Needs safety proof |
| EntityEffect/ModelVFX | Tight coatings | Metal Coat, Alloy, Gargoyle, Glare, Obsidian, poison | High |
| First-class projectile | Physics/collision projectile | Magma Sling, Sapling, Nightshade, Cacti, Vitrification | Needs proof integration |
| Proxy object | Persistent controllable object | Lapidary gem, possible aura anchor | Medium |
| Trail asset | Motion streaks/material waves | Debris, Sandstorm, Dust Devil, Magma Sling | Needs visual proof |
| Field runtime | Repeating AoE ticks | Sinkhole, Lava Pool, Mudpit, Sandstorm, Refraction | Medium |
| Movement state | Dash/burrow/tunnel | Stomp, Burrow, Tunnel, Dust Devil | Needs safety proof |
| Structured telemetry | Acceptance evidence | All Terra abilities | Needed first |

## Ability Success Matrix

### Quake

| Ability | Functional Success | Visual Success | Ideal Implementation | Acceptance Gate |
|---|---|---|---|---|
| Stomp | Left-click arms next jump; landing hits AoE at landing point | Flash + crack/shockwave on ground where player lands | Armed state + landing detector + AoE + quake impact ring | Log `stomp.armed`, `stomp.land`, `targets>=1`, screenshot of impact |
| Aftershock | 8 block spherical AoE around caster/target rule; applies intended knock/slow/damage only to enemies | Same flash/crack/tremor style as Stomp | Field or instant AoE + quake impact ring | Log radius=8, target count, friendly skips |
| Sinkhole | Target rooted/buried-look duration, suffocation/damage ticks, release cleanup | Cracked ground marker + brown dust + buried lower-body visual | Target status + EntityEffect + surface crack particles/decals | Log engage/tick/release and no lingering effect |

### Metal

| Ability | Functional Success | Visual Success | Ideal Implementation | Acceptance Gate |
|---|---|---|---|---|
| Iron Wall | grounded 3x4 wall one block in front of player, faces player, pushes overlapped enemies, lasts 4s, cooldown after disappear | Real mixed metal/iron blocks; no player coating; should look like it comes from the earth | Temporary block wall + overlap push + delayed cooldown | Log wall center/facing/expiry/push count |
| Metal Coat | Defensive buff applies for duration without affecting movement negatively | Strong dark gray metal coating on player and optionally held item | EntityEffect/ModelVFX coating using known strong coating route | User confirms coating; log apply/remove |
| Alloy Enhancement | First eligible melee/tool action starts binding; next 3 actions boosted; ends on 3 uses or item swap | Weapon/tool has metal coating and gray impact/swing frames | Active item binding + damage/durability hook + item/body coating | Log item id, charge 1-3, damage boost, cleanup |

### Magma

| Ability | Functional Success | Visual Success | Ideal Implementation | Acceptance Gate |
|---|---|---|---|---|
| Lava Pool | Field spawns from caster, enemies affected, caster/allies not slowed/damaged/burned, cleanup reliable | Visible lava pool/ring on ground, not air or cursor misplacement | Caster-centered temporary lava/fluid field with safety compensation or visual-only lava blocks | Log caster-safe checks and terrain cleanup |
| Obsidian Skin | Player rooted/immobile during lava shell, then shield/coat phase; no crash | Short lava block shell, then midnight purple-black stone-skin coating | Camera-safe shell geometry + queued obsidian ModelVFX | 5 casts no client crash; log shell/spawn/remove |
| Magma Sling | Lava projectile fires along aim, hits/despawns on impact | Lava blob/fireball-like projectile, not living mob/nameplate | `ProjectileModule` with lava/fireball config or custom projectile asset | Log projectile id, origin, direction, hit target |

### Stone

| Ability | Functional Success | Visual Success | Ideal Implementation | Acceptance Gate |
|---|---|---|---|---|
| Rubble Rouser | Melee/unarmed stone-arm buff works and expires cleanly | Stone coating on arms if possible; fallback strong player/held-item stone coating | Coating + melee hit hook + impact frame VFX | Log buff start, hit, damage/knockback, remove |
| Pillar Strike | Target hit/stunned/launched by target-centered pillar | 1x1x4 stone pillar stacks rapidly upward under target, then disappears 0.6s after full height | Staged temporary block column + combat pulse | Log target anchor, stages 1-4, target launch |
| Rockslide | Forward stone/dust control hits enemies in path | Rock/dust wave, not generic invisible damage | Projectile/line sweep + stone debris trail | Log path, hits, debris visual cleanup |

### Arbor

| Ability | Functional Success | Visual Success | Ideal Implementation | Acceptance Gate |
|---|---|---|---|---|
| Rooted | Player gets healing/rooted state without block damage | Vines/roots on top of ground at legs/lower body | Surface decoration blocks + self status | Screenshot confirms above-ground; log cleanup |
| Vines | Only one target at a time; old target releases; target death clears state | Vines/root visual on current target | Single-target state + EntityRemove cleanup + surface/root visual | Log old release, new engage, death cleanup |
| Sapling | Projectile marks ground, not enemy damage; emerald statue taunt object appears | `Furniture_Temple_Emerald_Statue` at impact point with pink glow | Ground-impact projectile + surface statue + taunt field | Log ground impact, marker id, lure targets |

### Bloom

| Ability | Functional Success | Visual Success | Ideal Implementation | Acceptance Gate |
|---|---|---|---|---|
| Nightshade | Projectile passes through enemies, lands on surface, lures enemies, then poison explosion | Flower on surface plus light purple body-hugging poison smoke | Ground/surface projectile + flower object + lure field + poison effect | Log pass-through, flower anchor, lure, explosion |
| Frolick | Player movement leaves flowers behind; no floor replacement | Flower trail on top of blocks behind moving player | Movement sampler + surface decoration anchor | Movement test video/screenshot; no underground/floor damage |
| Cacti Cluster | Large slow cactus projectile sticks to target/surface, DoT/slow 4s, explodes to secondary DoT/slow | Cactus-like projectile/object; explosion visual without extra attached cacti | Projectile + attachment state + delayed AoE | Log attach, DoT ticks, explode, secondary targets |

### Self Petrification

| Ability | Functional Success | Visual Success | Ideal Implementation | Acceptance Gate |
|---|---|---|---|---|
| Gargoyle | Stone form/shield/heal works, cancellable, 6s cooldown after end | Tight stone-skin coating | Toggle/status + ModelVFX coating | Log activate/end/cooldown and coating remove |
| Glare | Target petrified, released, then slowed for 2s | Target stone coating while petrified; coating disappears before slow tail | Target status + delayed slow + effect cleanup | Log petrify, release, slow tail, cleanup |
| Tunnel | Duration-based no-resource form; player can move through/under ground safely; ends by surfacing or valid cave exit | Player reads as singular stone block/form cue | Movement state + safe-surface/air-pocket resolver + stone form VFX | 10 casts in safe lane; never stuck; log exit reason |

### Soil

| Ability | Functional Success | Visual Success | Ideal Implementation | Acceptance Gate |
|---|---|---|---|---|
| Burrow | Drop, dash 4 blocks, re-emerge, exit damage/knockback; evasion use | Whack-a-mole ground entry/exit dust | Movement dash + temporary entry/exit markers + exit AoE | Log start/end positions, no void/fall, hits |
| Mudpit | Expanding field debuffs enemies, counts as water where relevant, caster/allies not slowed | Brown muddy water/pool on ground | Safe field with water/fluid plus brown overlay or visual-only fallback | Log caster speed/damage safe and cleanup |
| Debris | Forward wave blinds/weakens enemies | Brown dust/smoke/debris wave | Line sweep + trail/particles + block-break dust | Log wave path, hits, visual cleanup |

### Sand

| Ability | Functional Success | Visual Success | Ideal Implementation | Acceptance Gate |
|---|---|---|---|---|
| Sandstorm | 10s toggle, manual deactivate, 2s cooldown, follows player, damages/debuffs enemies only | Beige-yellow cloud/radius around player, not plain white smoke | Toggle state + follow-owner field + sand trail/particles/ring | Log activate/tick/end/cooldown and user visual pass |
| Dust Devil | Only usable during Sandstorm; dash drags enemies; expels at end; consumes Sandstorm | Moving sandstorm/tornado dash and expel burst | Active-Sandstorm precondition + movement + pull/knockback + Sandstorm cleanup | Log failure when inactive and consume when active |
| Vitrification | Layers with Sandstorm/Dust Devil without canceling; applies burn/glass effect | Superheated/glass/sand visual that does not hide cloud | Projectile/status + compatible overlay | Log active combo state and effect application |

### Gem

| Ability | Functional Success | Visual Success | Ideal Implementation | Acceptance Gate |
|---|---|---|---|---|
| Lapidary | One persistent controllable/recallable gem with HP tracking | Floating 2x2x2 green crystal/gem cube one block off ground, readable HP/nameplate | Temporary block cluster + proxy/HP state | Log gem id/HP/recall/despawn; user sees HP |
| Fracture | AoE originates from gem, skips allies/caster | Bright green expanding circle/sphere from gem epicenter | Gem-centered expanding pulse + damage sweep | Log gem origin, expansion ticks, targets |
| Refraction | Aura/shield/radius originates from gem | Bright green sphere/aura around gem radius | Field visual anchored to Lapidary proxy | Log aura attach/refresh/remove |

## Ideal Execution Order

```
╔════╦══════════════════════════════════════════════════════════════════╗
║ 1  ║ Add Terra telemetry: cast/hit/visual/status/cleanup/error logs   ║
║ 2  ║ Build cleanup audit: mobs, fields, fluids, blocks, proxies       ║
║ 3  ║ Prove first-class projectile path with fireball/stone candidates ║
║ 4  ║ Fix surface decoration anchor for roots, flowers, saplings       ║
║ 5  ║ Prove coating palette: metal, stone, obsidian, poison            ║
║ 6  ║ Prove safe field/fluid or choose visual-only fallback            ║
║ 7  ║ Prove movement safety: burrow, tunnel, dust devil lane           ║
║ 8  ║ Implement styles in dependency order: Metal, Quake, Stone, Arbor ║
║ 9  ║ Implement complex styles: Bloom, Magma, Soil, Sand, Gem          ║
║ 10 ║ Run user review style-by-style, then full Terra regression       ║
╚════╩══════════════════════════════════════════════════════════════════╝
```

This order is deliberately different from style list order. It finishes shared primitives first so Magma, Bloom, Soil, Sand, and Gem do not repeat the same broken projectile/field/movement mistakes.

## Immediate Proof Targets

### P1: Structured Terra Logs

Every ability test should produce parseable lines like:

```text
[MOTM][terra.cast] player=<id> style=<style> ability=<ability> input=<slot|lmb|rmb>
[MOTM][terra.visual.spawn] ability=<ability> kind=<block|fluid|projectile|effect|proxy> id=<asset> owner=<player>
[MOTM][terra.hit] ability=<ability> target=<entity> damage=<n> status=<list>
[MOTM][terra.friendly.skip] ability=<ability> target=<entity> relation=<caster|ally|summon>
[MOTM][terra.visual.cleanup] ability=<ability> kind=<...> count=<n>
```

### P2: First-Class Projectile Proof

Use local `ProjectileConfig` assets before custom NPC projectile proxies. Candidate visuals from local research include Fireball, Stone projectile, rubble models, cactus assets, poison/flame projectile textures, and crystal projectile sounds/visuals. The pass condition is a projectile that visibly flies, collides, applies hit logic, and despawns without a mob health bar/nameplate.

### P3: Coating Palette Proof

Preserve the strong Metal Coat route that looked good to the user, then make named variants:

- Metal: dark gray/iron.
- Stone: lighter stone gray.
- Obsidian: near-black midnight purple.
- Poison: light purple smoke/body-hugging effect.

Pass condition is user-visible difference in third person and runtime logs showing apply/remove.

### P4: Safe Placement Proof

Any flower/root/sapling/cactus marker must place above the support block and restore itself without removing or replacing the floor. This directly fixes prior Frolick/root issues.

### P5: Safe Field Proof

Before final Magma/Soil, prove one caster-centered field where:

- caster takes no damage;
- caster does not ignite;
- caster movement is normal or compensated;
- enemies are affected;
- all temporary fluid/block selections restore.

### P6: Movement Safety Proof

Run on a safe flat lane, not the platform edge. Pass condition:

- Burrow never walks player off platform or into void.
- Tunnel always exits to a valid air/surface position.
- Dust Devil moves forward while Sandstorm is active and ends Sandstorm.

## Known Non-Negotiables

- Do not reintroduce resource costs.
- Do not use invented Hytale API paths.
- Do not use living mobs as final projectiles if `ProjectileModule` can satisfy the ability.
- Do not place flowers/roots by replacing floor/support blocks.
- Do not rely on screenshots alone for "mechanically good."
- Do not let AoEs, fields, slows, roots, burns, lava, mud, or summons negatively affect caster/allies/allied summons unless the user explicitly approves an exception.
- Do not call Terra complete until every style passes normal input testing, not only dev commands.

## Bottom Line

The research is good enough to proceed, but the implementation should be primitive-led:

1. prove/log the shared primitives;
2. wire each Terra ability to the right primitive;
3. run style-specific tests that perform the actual actions each ability requires;
4. user-review visuals in third person;
5. only then mark Terra complete.

This is how we bridge the gap between "the concept is clear" and "the game actually behaves and looks that way."
