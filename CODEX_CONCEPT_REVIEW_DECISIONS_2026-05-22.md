# Concept Review Decisions - 2026-05-22

This file records user-approved concept changes made during the class/style/ability review pass. It is the handoff surface for later surgical data/code updates.

## Class Passives

```text
Global Rule
+-- Class passives, style passives, ability side effects, AoE fields, summons, and debuffs must not harm, slow, displace, root, debuff, or otherwise negatively affect:
    +-- the caster
    +-- allied players
    +-- allied summons/minions/pets/converted mobs
+-- Exceptions require explicit user approval per ability.
+-- Class passive conditions must not conflict with style ability requirements or make a style feel worse to play.
```

```text
Global Visual Asset Priority
+-- If an ability concept describes a physical structure, use actual in-game blocks or block-like models first.
    +-- Examples: walls, barricades, lava rings, cages, pillars, shells, pools, terrain objects.
+-- If an ability concept describes a coating, use a tight body/item-hugging tint/effect first.
    +-- Examples: Metal Coat, Alloy Enhancement, Obsidian Skin post-lava coating.
+-- If an ability concept describes a projectile made from a material, use the closest visible material-like projectile proxy first.
    +-- Examples: lava blob, stone chunk, ice shard, wind blade, void orb.
+-- Particles should enhance the main object/action, not replace it unless no safe object/block/model option exists.
+-- When object/block/model support is uncertain, research local Hytale APIs/assets first and document the fallback before implementation.
```

```text
Terra
+-- Remove: Earthen Strike
+-- Remove: Bare-Handed Excavation
+-- Change: Immovable
    +-- Old: -20% knockback taken, +10% knockback dealt to mobs
    +-- New: -20% knockback taken only
+-- Keep for now: Subterranean Fortitude
+-- Keep for now: Miner's Affinity
```

### Terra Notes

- Terra should not receive a free timed attack proc from the class passive layer.
- Terra should not receive a bare-handed mining passive.
- Terra's knockback passive is defensive only.

```text
Terra / Metal
+-- Iron Wall changes:
    +-- Wall is a 2x2 wall in front of the player, not "2 blocks taller than the player."
    +-- Wall should visually appear as four actual metal-like blocks.
    +-- Wall should function as a temporary barricade/structure.
    +-- Enemies that get too close to the wall or contact it are knocked away from the wall.
    +-- Wall lasts 4 seconds.
    +-- Cooldown starts only when the wall disappears.
+-- Metal Coat visual:
    +-- Solid gray metallic shiny coating hugging the player character model.
    +-- No occasional metal sparks required.
+-- Alloy Enhancement changes:
    +-- Applies only to physical melee weapons and tools.
    +-- Does not apply to ranged weapons.
    +-- Does not apply to magic weapons.
    +-- Active held weapon/tool gets a solid gray metallic shiny coating hugging the item model, visually similar to Metal Coat.
```

```text
Terra / Magma
+-- Lava Pool visual/function:
    +-- Use actual lava blocks if Hytale exposes safe temporary block placement.
    +-- The ability should create an actual ring of lava blocks moving outward from the player.
    +-- The ring should read as lava structure first, particles second.
    +-- It must still obey the global friendly-safety rule.
+-- Obsidian Skin visual/function:
    +-- Initial encase should look like actual lava blocks surrounding the user.
    +-- After the lava blocks vanish, the player model gets a very dark purple, almost black, tight full-body coating.
    +-- The coating should hug the character body, like Metal Coat but obsidian-colored.
+-- Magma Sling visual:
    +-- Should look like a lava projectile/blob flying through the air.
    +-- If an actual small lava block/projectile is not available, use the closest Hytale-supported proxy that reads as a molten blob.
```

```text
Terra / Stone
+-- Rubble Rouser visual:
    +-- Stone coating should cover the player's arms, not only the hands.
    +-- Coating should use the visual design/read of a stone block.
    +-- Coating should hug the arm model tightly rather than appear as a loose aura.
+-- Pillar Strike visual:
    +-- Pillars must be made from actual stone blocks if safe temporary block placement is available.
    +-- Stone dust/chips are secondary effects, not the main pillar.
```

```text
Terra / Arbor
+-- Rooted visual:
    +-- No visible text/UI wording is part of the ability visual.
    +-- Player should have vines/roots at the legs or lower body attached to the ground while rooted.
+-- Vines function/visual:
    +-- No cooldown.
    +-- Only one target can be affected at a time.
    +-- Vines disappear from the old target when moved to a new target.
    +-- Vines disappear when the affected target dies.
    +-- Persistent vine/root visual should remain on the currently rooted target.
+-- Sapling function/visual:
    +-- Projectile is used to mark a ground point, not directly damage enemies.
    +-- Projectile should always resolve by impacting the ground.
    +-- At the impacted ground block, spawn a tree sapling to represent the taunt object.
    +-- Prefer an existing basic Hytale tree sapling asset/block/model if available.
```

```text
Terra / Bloom
+-- Nightshade function/visual:
    +-- Projectile should pass through enemies and prioritize landing on an object/surface.
    +-- On landing, it creates a flower on that object/surface.
    +-- Flower applies a taunt/lure effect to enemies within 5 blocks so they move toward it before the poison explosion.
    +-- Poison explosion should still be the damaging moment.
    +-- Poison visual on afflicted targets should be light purple smoke tightly hugging the target body.
+-- Frolick function/visual:
    +-- While active, the moving player should place an actual trail of flowers on the ground behind them.
    +-- Prefer existing Hytale flower assets/blocks/models for the trail.
+-- Cacti Cluster function/visual:
    +-- Initial projectile is slow and large.
    +-- Projectile should look like an actual cactus from the game if available.
    +-- Projectile sticks to the first enemy target or first surface it contacts.
    +-- If stuck to an enemy, only that attached enemy takes the initial DoT.
    +-- Initial DoT lasts 4 seconds and deals damage equivalent to 5% of caster max HP.
    +-- Attached target is slowed by 20% during the DoT.
    +-- After 4 seconds, cactus visually explodes/disappears.
    +-- The visual explosion deals no extra damage to the attached target.
    +-- Explosion applies the same DoT effect to enemies/mobs within 4 blocks.
    +-- Explosion-applied DoT does not attach new cacti to secondary targets.
    +-- Secondary DoT targets are slowed by 20% during the DoT.
```

```text
Hydro
+-- Aqua Barrier visual: giant bubble surrounding the player's whole body.
+-- Aqua Barrier stacking rule:
    +-- If Hydro style abilities also grant shields, barriers, armor, or defensive overlays, Aqua Barrier is the outer/top layer.
    +-- Incoming damage should trigger or deplete Aqua Barrier first before style-specific defensive effects.
    +-- Visuals should read as the big bubble outside any inner Hydro style aura/shield.
```

```text
Aero
+-- Aero class identity approved as speed, air, evasion, lightning, sound, pressure, and movement.
+-- Skybound interaction rule:
    +-- Skybound extra jumps must be tested against every Aero style ability that modifies vertical movement.
    +-- Watch for overlapping jump charges, boosted leaps, dives, hovering, fall-damage prevention, aerial slams, and momentum resets.
    +-- Visual effects from Skybound and style abilities should layer clearly instead of hiding or duplicating each other.
    +-- Any conflicting vertical movement interaction must be resolved per-style during ability review.
```

```text
Corruptus
+-- Dark Resurrection change:
    +-- Old: at 3 stacks, lethal damage heals the player to full HP and resets stacks to 0.
    +-- New: at 3 stacks, lethal damage heals the player to half HP and resets stacks to 0.
+-- Dark Resurrection cooldown lockout:
    +-- The 10 minute cooldown applies to all Corruptus passive abilities.
    +-- During this cooldown, Soul Harvest cannot gain new stacks.
    +-- Infernal Aura cannot activate from stacks during this cooldown.
    +-- Dark Resurrection cannot trigger again until the cooldown ends.
```
