# Primitive Status Checkpoint - 2026-06-05

Purpose: resume primitive work from current `main` without falling back to old
race/story/resource/perk concepts. This document is the working status map after
the Snow summon work, shared 20-perk cleanup, and stale UI canon cleanup.

## Current Base

```text
Clean mainline
+-- UI canon: top-right MOTM HUD, no resource HUD, no story/race tabs
+-- Progression canon: 2 stat points/level through 200
+-- Perk canon: 20 shared passive choices, 10 chosen max through level 100
+-- Ability canon: 120 style abilities, 4 class passives
+-- Primitive strategy: prove shared behavior families before per-ability polish
```

Confirmed guardrails:

- `scripts/audit-canon-drift.ps1` checks stale UI/resource/perk/stat drift.
- `scripts/audit-no-resource.ps1` checks that ability resource costs stay off.
- `scripts/audit-public-readiness.ps1` checks visible proxy and summon safety.
- `scripts/build-install.ps1 -BuildOnly` remains the build/test gate.

## Status Overview

```text
Primitive family
+-- P0 Summon combat .......... PARTIAL / representative Snow path accepted
+-- P0 Controlled ally ........ NOT PROVEN as a reusable primitive
+-- P0 Pull / tether / carry .. PARTIAL / Rip Current exception accepted
+-- P1 Persistent fields ...... PARTIAL / many runtime adapters, weak visual proof
+-- P1 Status/coating/bubble .. PARTIAL / needs stacking and expiry proof
+-- P1 Transformation/form .... PARTIAL / Smoke Form path exists, dino forms not proven
+-- P1 Dash/burst locomotion .. PARTIAL / needs non-teleport visual proof
+-- P2 Projectile/line impact . PARTIAL / native projectile path exists, theme proof needed
+-- P2 Barrier/world object ... PARTIAL / terrain/object adapters exist, family proof needed
+-- P2 Perk effect families ... PARTIAL / hooks exist, live visual/UI proof incomplete
+-- Cross-cutting cleanup ..... PARTIAL / no-drop lessons proven, expand per primitive
```

## P0 - Summon Combat

Current status: BEHAVIORALLY PROVEN (2026-08-17, RunId summon-void, game 0.5.9). The DoD - at
least one NON-Snow summon family through the same functional gate - is met: `void_spawn` spawned
3 Crawler_Void allies (triangle) that engaged 3 distinct hostiles (21 attacks over the full 10s
window) with zero SEVERE. `SummonRuntimeSpecs` already maps all non-Snow families (no code change);
remaining U3 families (raise_dead/scarak_egg/swamp_monster) are data-only proof scenarios. See
`docs/completion-roadmap-2026-08-14.md` (E2 section).

What we learned from Snow:

- Item/model-like summons may need a hidden mobile driver plus an appearance
  override rather than dropped-item movement.
- Friendly summons need owner binding, friendly-fire suppression, no item drops,
  enemy target acquisition, and hostile retaliation support.
- Smooth follow should feel like the caster's local friendly zone moves under
  the summon, not like the player is dragging a chess piece.
- Mounted/tamed-control summons need a separate mount-state layer so mounted
  contact damage/knockback does not leak into unmounted behavior.

Current summon-dependent abilities:

```text
Summon-like abilities
+-- Hydro / Snow / snow_imp .......... snowman appearance, ground_snowman role
+-- Hydro / Snow / frosty ............ Yeti appearance, mountable/tank role
+-- Hydro / Freshwater / swamp_monster crocodile/frog family still needs proof
+-- Corruptus / Necro / raise_dead .... skeleton_minion still needs proof
+-- Corruptus / Shadow / shadow_step .. shadow_clone summon-like decoy path
+-- Corruptus / Void / void_spawn ..... 3 Crawler_Void allies
+-- Corruptus / Scarak / scarak_egg ... egg visual + hatchling summons
+-- Corruptus / Scarak / locust_queen . Scarak heavy summon, current safe role
+-- Perk / Haunting .................. temporary ghost allies on kill
```

Next work for this family:

- Convert Snow lessons into an explicit summon acceptance scenario that can be
  reused for `swamp_monster`, `raise_dead`, `void_spawn`, `scarak_egg`,
  `locust_queen`, `shadow_clone`, and Haunting ghosts.
- Keep Snow/Frosty as the representative accepted baseline, but do not mark
  the full summon primitive complete until at least one non-Snow summon family
  passes the same visual and functional gate.

## P0 - Controlled Ally

Current status: BEHAVIORALLY PROVEN (2026-08-17, RunId control-g2-2mob, game 0.5.9).
`runtime/ability/control/` engine (ActiveControlledAlly + ControlRuntimeState + ControlTickRuntime
+ ControlHytaleAdapter) wired into GameplayPlaybackManager; `dominate`/`hivemind` casts convert a
hostile, the tick drives it to attack another hostile (control_attack x19), and it auto-releases on
the 15-20s clock (control_released), no crash. Polish owed: pink-marker/follow visual screenshot,
FF caster-immunity stress, `mind_shatter` centering, logout/death release hook. See
`docs/completion-roadmap-2026-08-14.md` (E1 G2 sections).

Dependent abilities:

```text
Controlled ally dependencies
+-- Corruptus / Mentokinesis / dominate
+-- Corruptus / Mentokinesis / hivemind
+-- Corruptus / Mentokinesis / mind_shatter, because it can center on controlled allies
```

Contract still needed:

- Convert hostile to allied actor with bright pink control marker.
- Controlled actor follows/defends caster and attacks hostiles.
- Caster/allies are not targetable by controlled actors.
- Toggle/release/death/cleanup removes all control state.
- `mind_shatter` can resolve controlled ally center and damage it when intended.

Recommended priority: do this before broad Scarak/Void summon work because it
shares faction, targeting, friendly-fire, and follow-zone problems with summons.

## P0 - Pull / Tether / Carry

Current status: SHARED VISUAL-LINK PROVEN (2026-08-17, RunId tether-vines, game 0.5.9).
`runtime/ability/tether/TetherLinkRenderer` renders a burst-only (capped) bead chain between the
anchor-lifted caster and target, re-sampled each tether tick (synced movement) and self-cleaning
when the tick stops (no permanent-emitter leak). Wired into both the line-control tick
(vines/pull) and the channel tick (life_drain). Live: `tether_link` x5 over the full 5s vines
window, 6 beads caster->target, zero SEVERE. Owed: moving-target screenshot, canon per-ability
skins (Plant_Vine/Water_Beam) pending cap verification, and unifying all 9 abilities under one
service. See `docs/completion-roadmap-2026-08-14.md` (E3 section).

What is known:

- `rip_current` currently has an accepted water-block/fluid trace exception.
- Future tethers should prefer a thin particle-only line unless a specific theme
  justifies blocks/fluid.
- Movement-only evidence is not enough; the link must be visible and synchronized.

Dependent abilities:

```text
Tether / carry / forced-movement dependencies
+-- Terra / Arbor / vines ............. vine/whip tether, one target, recast release
+-- Hydro / Surf / riptide ............ water current pull
+-- Hydro / Saltwater / rip_current ... accepted water-trace exception
+-- Hydro / Bilgewater / anchor_haul .. chain/anchor pull readability
+-- Aero / Tornado / funnel_cloud ..... lift/carry field
+-- Aero / Gale Wizard / tempest ...... storm carry/pull field
+-- Corruptus / Void / rift ........... void field pull
+-- Corruptus / Necro / life_drain .... channel link/readability
+-- Primordial / pterodactyl carry_on . future carried-target subprimitive
```

Next work for this family:

- Build a shared visual-link contract with source anchor, target anchor, line
  style, release reason, and cleanup proof.
- Start with `vines` or `anchor_haul`, not `rip_current`, because Rip Current's
  approved water trace is an exception and could bias the generic primitive in
  the wrong direction.

## P1 - Persistent Field Readability

Current status: IN-AREA EFFECT + CLEANUP PROVEN (2026-08-17, RunId field-lava, game 0.5.9).
`lava_pool` (self-centered ground_zone, radius 5, 6s) burned all 4 surround dummies every tick
(`field_pulse affected=4 x7` over the full 6s, then pulses stop = cleanup), zero SEVERE. Unified
`field_pulse` observability added to `FieldPulseHytaleAdapter`. Radius-readable visual-proxy grid
attested via the visualProxyRefs snapshot. Owed: broad per-ability visual screenshots and canon
field skins. See `docs/completion-roadmap-2026-08-14.md` (E4 section).

Representative runtime owners:

- `FieldRuntimeSpecs`
- `FieldActivationHytaleAdapter`
- `FieldVisualHytaleAdapter`
- `FieldPulseHytaleAdapter`
- `FieldTerrainHytaleAdapter`
- `FieldSinkholeHytaleAdapter`

High-value dependent abilities:

```text
Field dependencies
+-- Terra: aftershock, sinkhole, lava_pool, mudpit, sandstorm, dust_devil
+-- Hydro: snowstorm, piercing_rain, rainbow, tide_pool, oil_spill
+-- Aero: funnel_cloud, tempest, smog, acid_rain, smoke_bomb
+-- Corruptus: ignite, infernal_ground, sanctuary, rift, mind_shatter, hivemind
```

Next work:

- Pick one field where visual footprint matters most and proof it end to end.
- Best candidates: `sinkhole` for terrain readability or `smoke_bomb` because
  it also forces projectile-deployed field behavior.

## P1 - Status / Coating / Bubble

Current status: partial.

Dependent abilities/passives/perks:

```text
Status / coating / bubble dependencies
+-- Hydro passive Aqua Barrier: opaque blue bubble until destroyed
+-- Hydro: ice_cap, abyssal_assist, vapor_vanish, hidrosis
+-- Terra: metal_coat, obsidian_skin, gargoyle, rooted, glare
+-- Corruptus: imbuement glows, atonement holy state, death_mark, consume
+-- Aero: vanish, smoke_form, cyclone_shield, thunderclap stun
+-- Perks: Freezing Winds, Ignite, Terror, Heavyweight, Eco-friendly cooldown state
```

Next work:

- Define visual stacking and expiry rules before adding more coatings.
- Aqua Barrier remains the clearest representative because the user has a
  precise visual expectation: one large opaque blue bubble, no permanent Hydro
  tint, no permanent water trail.

## P1 - Transformation / Form

Current status: partial.

Dependent abilities:

```text
Transformation dependencies
+-- Aero / Smoke / smoke_form
+-- Corruptus / Primordial / pterodactyl_form
+-- Corruptus / Primordial / triceratops_form
+-- Corruptus / Primordial / t_rex_form
```

Next work:

- Do not mark this family complete from Smoke Form alone.
- Dino forms need model identity, form-specific actions, hotbar restrictions,
  early exit, cooldown behavior, and third-person visual proof.

## P1 - Dash / Burst Locomotion

Current status: partial.

Dependent abilities:

```text
Dash / burst dependencies
+-- Terra: rockslide, burrow, dust_devil
+-- Hydro: skate, waverider, dispersion, leap_frog, river_rapids
+-- Aero: jet_burst, mach_punch, leap, divebomb, shadow-like movement variants
+-- Corruptus: shadow_step
```

Next work:

- Define the difference between accepted teleport exceptions and burst travel.
- `dust_devil` remains a high-risk target because the user explicitly called
  teleport-like movement unacceptable.

## P2 - Projectile / Line Travel + Impact

Current status: partial.

Representative runtime owners:

- `ProjectileRuntimeSpecs`
- `ProjectileLaunchHytaleAdapter`
- `ProjectileVisualHytaleAdapter`
- `ProjectileImpactHytaleAdapter`
- `NativeProjectileHytaleAdapter`

High-value dependent abilities:

```text
Projectile / line dependencies
+-- Aero: air_slash, gale_cutter, razor_wind, air_shot, bullet_storm,
|        pressure_burst, smite, chain_lightning, sonic_boom
+-- Hydro: frozen_needles, splash, scald, high_tide, bilge_dump
+-- Terra: magma_sling, debris, glare, cacti_cluster
+-- Corruptus: fireball, combust, hellfire, soul_scorch, consume
```

Next work:

- Separate projectile families into visible travel, cone/breath, chain, charged
  shot, and instant-target exceptions.
- Pressure Burst is a good later representative because it combines charge,
  projectile scale, travel range, and impact proof.

## P2 - Barrier / World Object

Current status: partial.

Dependent abilities:

```text
World object dependencies
+-- Terra: iron_wall, pillar_strike, sapling, cacti_cluster, lapidary
+-- Hydro: glacier, ice_shelf, stalactite_crash
+-- Corruptus: scarak_egg
+-- Primordial: trillodon block breaking / stampede
```

Next work:

- Preserve the surface-placement rule: visuals sit on top of blocks unless true
  terrain replacement is explicitly intended.
- Confirm no-drop behavior for every temporary object family.

## P2 - Perk Effect Families

Current status: hooks exist for all current shared 20 perks, but some still need
strong live proof and UI/HUD proof.

```text
Perk primitive families
+-- Movement/passive stat: Twinkletoes, Accelerate, Bunny Hop, Big Strides,
|   Semiaquatic, Big Lungs, Mole Man
+-- Combat trigger: Sharpshooter, Ignite, Desperation, Vampirism, Terror,
|   Heavyweight
+-- Low-health / area reactive: Neptune's Grace, Freezing Winds
+-- Weather/world action: Rainy Day, Eco-friendly
+-- Summon-on-kill: Haunting
+-- Crafting/item metadata: Blacksmith, Toolsmith
```

Next work:

- Keep perk proof family-based. Do not return to old tier-2+ perk fantasies.
- Movement perks and crafting/world-action perks need stronger live proof than
  chat-only command simulation.

## Recommended Next Primitive

```text
Best next target
+-- Controlled Ally
    +-- Why: shares summon targeting/faction/friendly-fire lessons
    +-- Why: small ability surface, only Mentokinesis core
    +-- Why: unlocks Dominate/Hivemind/Mind Shatter correctness
    +-- Gate: hostile becomes ally, visibly fights hostile, follows caster,
        cannot hurt allies, releases/cleans up, pink marker visible
```

If we want the least risky code path, start with `dominate` as a single-target
controlled ally. Once that behaves correctly, extend the same primitive to
`hivemind` as a radius/multi-target variant and verify `mind_shatter` centers on
controlled allies correctly.

## Immediate Work Queue

1. Add/verify a `controlled-ally` scenario that spawns two hostile mobs, controls
   one, and proves it attacks the other.
2. Implement or harden controlled ally owner/faction/friendly-fire state using
   the summon lessons.
3. Add pink control visual marker and cleanup proof.
4. Run harness proof plus visual capture in-world.
5. Update this status doc with the result before moving to the next primitive.
