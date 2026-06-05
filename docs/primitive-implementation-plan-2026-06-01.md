# Primitive Implementation Plan - 2026-06-01

This is the implementation plan for turning the recovered ability/UI canon into stable reusable primitives. The goal is to stop proving abilities one-off and instead prove the shared runtime behavior that whole families of abilities depend on.

```
╔══════════════════════╗
║ Recovered canon      ║
║ UI + ability intent  ║
╚══════════╦═══════════╝
           ▼
╔══════════════════════╗
║ Primitive contracts  ║
║ behavior + visuals   ║
╚══════════╦═══════════╝
           ▼
╔══════════════════════╗
║ Runtime owners       ║
║ managers + effects   ║
╚══════════╦═══════════╝
           ▼
╔══════════════════════╗
║ Proof scenarios      ║
║ logs + screenshots   ║
╚══════════╦═══════════╝
           ▼
╔══════════════════════╗
║ Style sweeps         ║
║ ability-by-ability   ║
╚══════════════════════╝
```

## Working Rules

- No ability implementation is considered complete until its primitive contract and proof criteria exist.
- Use existing Hytale assets, entities, effects, blocks, and item IDs. Do not invent unavailable assets.
- Verify the intended player controls where possible, not only dev commands.
- Visual proof must show the intended visual, not merely the mechanical outcome.
- Functional proof must include raw observability/log evidence where the harness can capture it.
- Temporary entities, blocks, fluids, coatings, and particles must clean up without drops or stuck visuals.
- Training dummies are not allowed to be hidden dependencies for ordinary ability behavior.
- UI proof must compare against the current canon, especially top-right HUD placement and spellbook tab structure.

## Primitive Map

```
P0 Now
├─ Summon Combat
├─ Controlled Ally
└─ Pull / Tether / Carry

P1 Next
├─ Persistent Field Readability
├─ Status / Coating / Bubble
├─ Transformation / Form
└─ Dash / Burst Locomotion

P2 After Core Feel Is Stable
├─ Projectile / Line Travel + Impact
├─ Barrier / World Object
├─ Perk Effect Families
└─ Cleanup / No-Drop / No-Dummy Guards
```

## P0 - Summon Combat Primitive

Primary abilities:

- Terra: sapling-style summons if retained by the current ability list.
- Hydro: Snow, Ice, Swamp summons where canon calls for allied creatures.
- Corruptus: Raise Dead, Void Spawn, Scarak Egg, Locust Queen.
- Any passive/perk that summons a temporary ally on trigger.

Contract:

- Spawn an allied entity from an existing game entity ID.
- Bind the summon to the caster as owner.
- Acquire hostile targets without requiring training dummies.
- Move toward targets, attack, and deal attributable damage.
- Respect friendly fire rules.
- Despawn on timeout, caster death/logout, `/motm dev clear`, or scenario reset.
- Remove any temporary status, AI override, owner metadata, and visual markers on cleanup.

Visual requirements:

- The summon must be visible as the intended creature.
- Snow Imp specifically must use the existing `WinterHoliday_Snowman` item/model visual, not the generic frost spirit.
- It must visibly move and attack on behalf of the caster.
- Hit feedback must appear on the enemy or through the existing combat feedback system.

Proof gate:

- Scenario spawns caster, summon, and hostile target.
- Logs show summon spawn, target acquisition, attack attempt, and enemy damage.
- Screenshot or video shows the summon between caster and hostile and actively fighting.
- Cleanup proof shows no lingering summon, drops, or owner metadata.

## P0 - Controlled Ally Primitive

Primary abilities:

- Mentokinesis: Dominate, Hivemind.
- Carry-adjacent ally control cases where the target temporarily follows caster intent.

Contract:

- Convert a valid enemy into a temporary allied actor.
- Preserve enough original identity that release/death is understandable.
- Follow or defend the caster based on ability rules.
- Attack hostile targets during the control window.
- Release cleanly on timeout, target death, caster death/logout, `/motm dev clear`, or scenario reset.
- Avoid player/friendly harm unless a specific ability says otherwise.

Visual requirements:

- Bright pink Mentokinesis visuals must identify the controlled state.
- The controlled target must visibly behave differently from ordinary hostile AI.
- Release must remove the control marker.

Proof gate:

- Scenario controls one hostile while another hostile is present.
- Logs show control applied, faction/AI change, hostile target acquisition, and attack or damage.
- Screenshot/video shows the controlled entity fighting for the caster.
- Release proof shows state restoration or clean despawn.

## P0 - Pull / Tether / Carry Primitive

Primary abilities:

- Hydro: Rip Current, Surf/Riptide-style movement.
- Terra: Arbor vine pulls if retained.
- Aero: Funnel Cloud, Tempest carry/lift behavior.
- Corruptus: Void Rift pull behavior.
- Primordial: Pterodactyl Carry On.
- Bilgewater: Anchor Haul if retained by the current list.

Contract:

- Define source anchor, target anchor, line ownership, range, break rules, and release state.
- Move the target in a readable path rather than teleporting unless the specific ability is an exception.
- Keep movement synchronized with visible link/carry effects.
- Avoid permanent clipping, stuck velocity, or post-release drift.
- Clean up the link immediately when the ability ends or fails.

Visual requirements:

- Default tether should be a thin particle line connecting caster/source to target.
- Themed variants may use water, vines, chain, void, wind, or similar existing effects.
- Rip Current may keep the accepted water-block/fluid trace as a special-case visual if it remains readable.

Proof gate:

- Scenario has target outside melee range.
- Logs show pull/carry start, movement ticks, release reason, and final target location.
- Screenshot/video shows the target moving while the link/carry visual is present.
- Cleanup proof shows no lingering particles, water/blocks, or velocity.

## P1 - Persistent Field Readability Primitive

Primary abilities:

- Terra: Sinkhole, Aftershock, Mudpit, Rockslide.
- Hydro: Tide Pool, Piercing Rain, Rainbow, Snowstorm.
- Pyro: Lava Pool, Smog, Acid Rain, Infernal Ground.
- Aero: Sandstorm, Dust Devil, Tornado fields.
- Corruptus: Void Rift, Sanctuary-style areas.

Contract:

- Define footprint shape, radius, duration, pulse interval, target filters, and cleanup behavior.
- Apply effects only while targets are inside the field unless ability canon says otherwise.
- Support friendly/fire rules per ability.
- Emit clear start, pulse, target-hit, and end observability events.

Visual requirements:

- Field boundary must be readable from normal play camera distance.
- Center, edge, and pulse behavior should communicate what the field is doing.
- Sinkhole must show cracks and brown dust around the buried location.
- Fire, water, void, sand, and storm fields must use distinct game-native visuals.

Proof gate:

- Scenario places target inside and outside the field.
- Logs show inside target affected and outside target ignored.
- Screenshot/video shows footprint and at least one pulse.
- Cleanup proof shows no leftover blocks, particles, or damage ticks.

## P1 - Status / Coating / Bubble Primitive

Primary abilities:

- Hydro: Aqua Barrier, freeze, Abyssal Assist.
- Terra: Stoneskin, Self Petrification, Metal Coat, Obsidian Skin, root/stun/vulnerability.
- Pyro: burn, Hell Flame blue fire, Infernal Ground status.
- Corruptus: Imbuement red/green/yellow glows, Atonement holy state.
- Aero: stun, pressure, smoke, wind status markers.

Contract:

- Attach visual state to an actual buff/debuff, never as decoration only.
- Remove visual state exactly when the buff/debuff ends.
- Define stacking priority so bubbles, coatings, tint, and glow effects do not hide each other incorrectly.
- Support periodic refresh without permanent tint or duplicate effects.

Visual requirements:

- Aqua Barrier must be a readable protective bubble with water trail/tint behavior when specified.
- Coatings must wrap the player/entity clearly enough to distinguish material.
- Imbuement colors must read as red, green, and yellow.
- Hell Flame must read as blue fire.

Proof gate:

- Scenario applies, refreshes if applicable, and expires the state.
- Logs show buff/debuff applied and removed.
- Screenshot/video captures active visual and post-expire absence.
- Cleanup proof confirms no lingering tint, particle loop, or stat modifier.

## P1 - Transformation / Form Primitive

Primary abilities:

- Primordial: Pterodactyl, Trillodon, Rex_Cave forms.
- Aero/Corruptus: Smoke Form or any body-form replacement ability.

Contract:

- Enter a recognizable alternate form for the intended duration.
- Restrict or replace normal attacks/hotbar behavior according to form rules while keeping spellbook access if canon requires it.
- Support form-specific movement, attacks, and exit ability.
- End on timeout, explicit exit, death, logout, `/motm dev clear`, or scenario reset.

Visual requirements:

- The player must visibly become the intended creature/form or an unmistakable proxy using existing assets.
- Form locomotion must match fantasy: Pterodactyl flies, Trillodon breaks blocks if intended, Rex_Cave reads as heavy melee.

Proof gate:

- Scenario enters form, uses each form action, then exits.
- Logs show form enter, action, exit reason, cooldown start, and cleanup.
- Third-person screenshot/video shows the form, not only first-person mechanics.

## P1 - Dash / Burst Locomotion Primitive

Primary abilities:

- Aero: Dust Devil, burst movement, wind mobility.
- Terra: Burrow if retained.
- Hydro: Skate, Waverider.
- Primordial: Leap, Divebomb.
- Corruptus: Shadow Step, Dispersion.
- Any style dash that should feel like a burst instead of a teleport.

Contract:

- Move the caster through a short readable displacement over time.
- Define collision handling, target carry rules, interruption, and ending velocity.
- Mark explicit teleport exceptions separately.
- Emit start, movement, collision/interruption, and end events.

Visual requirements:

- Movement should have a start burst, trail, and end cue.
- Burrow may be less visible while underground, but entry and exit must be clear.
- Generic teleport visuals are not acceptable for dash fantasies unless canon says teleport.

Proof gate:

- Scenario records start/end position and movement duration.
- Logs show movement path and no stuck velocity.
- Video shows the travel path, not only before/after positions.

## P2 - Projectile / Line Travel + Impact Primitive

Primary abilities:

- Aero: Wind Blade, Air Slash, Gale Cutter, Razor Wind.
- Aero: Smite, Chain Lightning, Pressure Burst.
- Pyro/Hydro/Terra/Corruptus: any projectile, beam, cone, breath, or line attack.
- Smoke Bomb and charged-shot abilities.

Contract:

- Define travel speed, range, width, collision, pierce/bounce, charge scaling, and impact behavior.
- Support nearest-target chaining where canon requires it.
- Support cones/breaths as shaped projectile variants.

Visual requirements:

- Travel must be visible before impact.
- Impact must show where and why damage/status occurred.
- Charged shots must visibly scale or charge.

Proof gate:

- Scenario includes hit, miss, max range, and obstruction if relevant.
- Logs show projectile spawn, travel/impact, target list, and damage/status.
- Screenshot/video shows travel and impact.

## P2 - Barrier / World Object Primitive

Primary abilities:

- Terra: Stone Pillar Strike, Iron Wall.
- Hydro: Glacier, Ice Shelf.
- Terra/Pyro/Nature-adjacent: Cacti Cluster, Arbor/Bloom placement, Scarak Egg object.
- Primordial: Trillodon terrain interaction.

Contract:

- Place temporary world objects on top of blocks unless true terrain replacement is explicitly intended.
- Define collision/blocking, health/damageability, duration, and cleanup.
- Prevent item/resource drops unless canon explicitly grants an item.
- Restore terrain state cleanly.

Visual requirements:

- Objects must be readable as real world objects, not invisible collision.
- Stone Pillar Strike should present a 1x3 pillar.
- Arbor/Bloom-style placements should not replace the support block unless intended.

Proof gate:

- Scenario places object, interacts with it, then expires/clears it.
- Logs show placement, collision/damage interaction, cleanup, and no-drop result.
- Screenshot/video shows the object and its cleanup.

## P2 - Perk Effect Family Primitive

Primary abilities:

- The current reduced perk design: 20 broader class perks that can be mixed and matched.
- Any class passive that modifies cooldown, stats, on-hit, on-kill, summon, life steal, weather, crafting, or ultimate readiness.

Contract:

- Group perks by behavior family instead of implementing each perk as a one-off special case.
- Tie every perk effect to a visible HUD/status state only when the player needs to know it is active.
- Support top-right passive/perk HUD rendering from the UI canon.
- Emit trigger, refresh, expire, and stat-modifier events.

Visual requirements:

- Passive/perk state belongs in the top-right HUD.
- Combat-triggered perk effects should have lightweight world feedback if the player needs confirmation.
- No resource bars or resource labels should reappear.

Proof gate:

- One proof scenario per perk family before proving every individual perk.
- Logs show trigger condition, effect application, and expiration.
- UI screenshot confirms passive/perk placement and no resource regression.

## P0/P2 - Cleanup / No-Drop / No-Dummy Guard

This is cross-cutting. It should be wired early for P0 primitives, then expanded as P2 coverage grows.

Contract:

- `/motm dev clear` removes all temporary summons, controlled states, tethers, particles, coatings, fields, blocks, fluids, and form state.
- Ability expiration cleanup runs even if the target dies, caster dies, command reloads, or scenario resets.
- Temporary blocks/world objects do not drop items.
- Proof scenarios must not rely on training-dummy-only code paths unless testing dummy compatibility explicitly.

Proof gate:

- Run cleanup after each primitive scenario.
- Logs show cleanup count by primitive type.
- World/entity scan shows no lingering temporary artifacts.
- Inventory/drop scan shows no unintended dropped items.

## Implementation Order

```
Phase A ──▶ Proof scaffolding
           scenario IDs, screenshot/video checklist, cleanup guard

Phase B ──▶ Summon Combat + Controlled Ally
           prove allies really fight

Phase C ──▶ Pull / Tether / Carry
           prove visible link + synchronized movement

Phase D ──▶ Persistent Fields + Status/Coating/Bubble
           prove readable area/state visuals

Phase E ──▶ Transformations + Dash/Burst
           prove form identity and locomotion feel

Phase F ──▶ Projectile/Object/Perk families
           broaden style coverage and regression guards
```

## Done Means

For each primitive, done means:

- Contract exists in this plan or a follow-up spec.
- Runtime owner is identified in code.
- At least one representative ability is wired through the primitive.
- Scenario proof captures functional logs.
- Visual proof captures screenshot/video of the intended visual.
- Cleanup proof passes.
- A short result note is added to the current audit/checklist docs.

## Do Not Do

- Do not continue broad per-ability patching before P0 primitive proof exists.
- Do not accept damage-only evidence for visual abilities.
- Do not accept movement-only evidence for tether/carry abilities.
- Do not accept summon-spawn evidence unless the summon fights for the caster.
- Do not let UI resource labels, bottom XP, or missing top-right passive/perk HUD regress again.
- Do not assume an existing manager is correct until a scenario proves the behavior and visual together.
