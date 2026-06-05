# Current Canon Checklist

Purpose: prevent implementation drift. This file is the working checklist for decisions that have already been grilled through and should not be reconstructed from memory.

Primary canon source:

- `docs/primitive-audit-2026-05-31.md`
- `docs/ui-canon-2026-06-01.md`
- `docs/style-ability-intent-canon-2026-06-01.md`
- `docs/primitive-implementation-plan-2026-06-01.md`

## Current Layout Canon

```text
Game HUD layout
+--------------------------------------------------------------+
|                                                        MOTM  |
|                                                        HUD   |
|                                                              |
|                                                              |
|                                                              |
|                                                              |
|                 normal game view                            |
|                                                              |
|                                                              |
|                                                              |
|                  hotbar / ability UI                         |
+--------------------------------------------------------------+
```

- MOTM passive/perk/XP HUD belongs at the top-right.
- Resource HUD is removed. Do not show Hydro Waterskin, water bars, souls bars, or style-resource strips.
- Bottom HUD stays reserved for normal game hotbar and ability input UI.
- Passive and perk info must remain visible/readable in the MOTM HUD when applicable.
- Spellbook tabs must not include Journey, Codex, Journal, Grimoire, Resources, lore, quests, or story sections.
- Player spellbook tabs are Class, Styles/Abilities, Perks, and Stats.
- Dev/test spellbook may add class/style test controls, but only in the dev/test variant.

Current implementation checkpoint:

- `src/main/resources/Common/UI/Custom/HUD/MOTM_StatusHud.ui`: root anchored `Right: 24`, `Top: 72`.
- `src/main/java/com/motm/ui/MotmStatusHud.java`: resource render path removed; `hideResource()` clears stale resource widgets.
- Last live check: world loaded and screenshot showed passive/perk/XP HUD with no resource strip.

## Ability Primitive Canon

The grilled audit covered all authored style abilities, class passives, and perks. Do not redo that pass unless the authored data changes.
The recovered style-ability grill notes now live in `docs/style-ability-intent-canon-2026-06-01.md`; use that file for ability-specific intent before changing runtime behavior.

```text
Authored surface
+-----------------+--------------------------------------------+
| Style abilities | 120 abilities across 40 styles             |
| Class passives  | 4 class identities                         |
| Perks           | 800 authored, 560 loaded after tier filter |
+-----------------+--------------------------------------------+
```

The audit conclusion is that ability correctness should be proven through shared primitives, not one-off visual guesses.
The implementation order and acceptance gates for those shared primitives live in `docs/primitive-implementation-plan-2026-06-01.md`.

## Priority Work Queue

P0:

- Summon combat acceptance primitive.
  - Spawn summon.
  - Acquire hostile target.
  - Visibly move/chase or hold ranged position.
  - Visibly attack.
  - Deal owner-attributed damage.
  - Despawn/cleanup.
- Pull/tether visual primitive.
  - Caster/source anchor.
  - Target anchor.
  - Default future read: a thin particle-only line connecting caster/source to target.
  - Themed link effect: water stream, vine, chain, void pull, wind funnel.
  - Accepted exception: Saltwater `rip_current` may keep its temporary water-fluid trace because the current visual was approved for that ability.
  - Target movement synchronized with link.
  - Clear start/end visual evidence.
- Controlled-ally primitive.
  - Enemy can become friendly for a duration or until released.
  - Converted target follows caster and fights hostile mobs.
  - Friendly summons and controlled targets never attack caster/allies.
  - Release/death/cleanup is explicit and observable.

P1:

- Persistent field readability primitive.
- Status/coating visual primitive.
- Transformation/form acceptance primitive.
- Dash/burst locomotion primitive.

P2:

- Projectile travel/impact theme pass.
- Barrier/world-object behavior pass.
- Perk effect-family implementation plan.
- Cleanup/no-drop regression pass for temporary blocks, summons, and visual props.

## High-Risk Canon Examples

- Saltwater `rip_current`: movement alone is not enough; needs a visible water tether/stream from caster or pool to target.
- Surf `riptide`: same pull/tether primitive family.
- Arbor `vines`: needs vine/whip tether visual, not just target displacement.
- Bilgewater `anchor_haul`: needs chain/anchor pull readability.
- Void `rift`: needs field/pull/status proof with readable void pull.
- Snow `snow_imp`: must visibly use the `WinterHoliday_Snowman` item/model as its summoned minion, while still fighting for the caster.
- Snow `frosty`, Freshwater `swamp_monster`, Terra `sapling`, Corruptus `raise_dead`, Void `void_spawn`, Scarak `scarak_egg`, Scarak `locust_queen`: summons must visibly fight for the caster, not merely spawn.

## Passive And Perk Canon

- Class passives are partial but real and need explicit verification against authored text.
- Corruptus passive/resource behavior needs a focused audit; do not assume it is complete because generic resource plumbing exists.
- Perks should not be treated as implemented as a full tree. Selection exists and some low-level stat/trigger hooks exist, but most authored perk fantasies need their own primitive plan.

## Working Rule

Before changing any ability, passive, perk, summon, tether, field, or HUD behavior:

1. Cite the relevant row/category from this checklist or `docs/primitive-audit-2026-05-31.md`.
2. Check `docs/primitive-implementation-plan-2026-06-01.md` for the matching primitive contract.
3. State the intended visual/mechanical contract.
4. Implement only the mismatch.
5. Verify with log proof plus screenshot/video when visual correctness matters.
