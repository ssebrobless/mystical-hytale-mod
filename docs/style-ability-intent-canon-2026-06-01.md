# Style Ability Intent Canon - 2026-06-01

Purpose: preserve style-ability intent recovered from session
`019e7f50-c434-7320-a35a-fb1da172ef5b`, especially the notes from the
style-ability grill. This document records player-facing behavior and visual
readability expectations. Do not treat it as proof that the current runtime
already implements these details.

Primary companion docs:

- `docs/primitive-audit-2026-05-31.md`
- `docs/current-canon-checklist.md`
- `docs/ui-canon-2026-06-01.md`

## Recovery Map

```text
Session grill notes
├─ Terra corrections
│  ├─ Quake: stomp / aftershock / sinkhole
│  ├─ Stone: pillar strike
│  ├─ Arbor + Bloom: place on top of blocks, do not replace them
│  ├─ Self Petrification + Soil: missing readability
│  └─ Sand: sand visuals must read as sand, not white smoke
├─ Hydro corrections
│  ├─ Aqua Barrier: persistent blue bubble until destroyed
│  └─ permanent tint/water trail is wrong
├─ Aero corrections
│  ├─ Wind Blade: air slash / gale cutter / razor wind
│  ├─ Thunder: smite / chain lightning / thunderclap
│  ├─ Pressure: charged pressure burst
│  ├─ Tornado: twister / funnel cloud
│  ├─ Smoke: smoke bomb projectile
│  └─ dash feel: burst movement, not teleport
└─ Corruptus corrections
   ├─ Void: Void Spawn replaces Null Zone
   ├─ Flame / Hell Flame: fire fields and blue fire
   ├─ Mentokinesis: dominate / mind shatter / hivemind
   ├─ Imbuement: self-only, color-coded glows
   ├─ Scarak: real egg item and friendly Scarak summons
   └─ Primordial: real creature transformations and form abilities
```

## Global Ability Rules

- Abilities must be verified through intended player controls, not only through
  dev commands.
- Buffs that are activated while holding the spellbook must persist when the
  player switches to bare hands or a native weapon, when the ability fantasy
  requires that.
- Temporary blocks, roots, flowers, cracks, summons, and other visuals must not
  drop resources when cleaned up.
- Avoid replacing or destroying world blocks for cosmetic visuals when placing a
  temporary object on top of an existing block is enough.
- Dashes should generally feel like a burst of movement, not a teleport. Explicit
  exceptions may exist for abilities whose fantasy is disappearance or burrowing.
- Training dummies should not be hard-wired into abilities or proofs as a hidden
  dependency.

## Terra Notes

### Quake

Source lines: session `019e7f50...` lines 751, 12993.

- `stomp`: left-click should enhance the next jump. The AoE occurs at the ground
  point where the caster lands.
- `stomp` visual: impact should read as a flash plus ground-crack effect.
- `aftershock`: approved visual direction is the flash/crack effect from testing.
- `aftershock`: radius should be an 8 block spherical radius.
- `sinkhole`: should read as dragging targets under the ground during the effect,
  then releasing/raising them when finished.
- `sinkhole`: actual terrain ripping is not required. The accepted approach is a
  buried visual: break-block/crack effects over the impact spot, brown dust above
  the cracks, root/status during duration, and clear release.
- `sinkhole`: the affected location must be easy to track while the target is
  buried.

### Stone

Source line: session `019e7f50...` line 19166.

- `pillar_strike`: must visibly create a one-block-wide, three-block-tall stone
  pillar at the target location.
- It does not have to physically grow from the ground, but it should appear in
  rapid stacked segments: one block below/at the target, then two more quickly.
- The timing should be fast enough to feel like a pillar eruption and to visually
  explain the launch/hit.

### Arbor And Bloom

Source line: session `019e7f50...` line 19166.

- `rooted` / `vines`: roots should be placed on top of existing blocks and tether
  targets to that spot. They should not destroy the block underneath the player
  or target.
- `frolick`: flowers should be placed on top of blocks. Do not replace the block
  beneath the player.
- General terrain rule for these styles: visuals should sit on top of terrain
  unless the ability truly requires terrain replacement.

### Self Petrification, Soil, Bloom, Sand

Source line: session `019e7f50...` line 19166.

- `tunnel` and `burrow`: need visible indication; the user did not see evidence
  that they were working.
- `mudpit` and `debris`: need visible indication; the user did not see evidence
  that they were working.
- `cacti_cluster`: looked good enough from the user review.
- `nightshade`: looked good enough from the user review.
- `glare`: unclear visual/function proof; needs a focused test.
- `gargoyle`: user saw a gray encasing moment; likely direction is acceptable,
  but still needs proof.
- `sandstorm` and `dust_devil`: white smoke is not acceptable as the dominant
  visual. These must read as sand. If tinting smoke is not enough, use particles
  or another game-native sand-like effect.

## Hydro Notes

Source line: session `019e7f50...` line 40288, plus current UI discussion.

- Permanent Hydro tint on the player is wrong.
- Permanent water trail on the player is wrong unless a specific active ability
  is responsible and the duration is still active.
- `aqua_barrier`: should activate and stay active until destroyed, then go on its
  cooldown.
- `aqua_barrier` HUD state should not toggle on/off as if it were repeatedly
  refreshing when the barrier is already active.
- `aqua_barrier` visual: a single large opaque blue bubble surrounding the player
  model, so the player appears inside the bubble.
- When `aqua_barrier` breaks, the bubble visual disappears.
- `/motm dev clear` must clear all Hydro lingering visuals; water trail lingering
  after clear is a regression.
- Saltwater `rip_current`: current water-block/fluid trace is accepted for that
  ability, but future tether abilities should prefer a thin particle line unless
  explicitly themed otherwise.

## Aero Notes

### Wind Blade

Source line: session `019e7f50...` line 53946.

- `air_slash`: a sideways arced line, 3 blocks wide, with a faint bright yellow
  tint.
- `gale_cutter`: similar to `air_slash`, but the lines form an X when fired
  forward.
- `razor_wind`: should perform five small consecutive slash effects on each
  target affected by the cast.

### Thunder

Source line: session `019e7f50...` line 53964.

- `smite`: impact area should be 3x3 so the ability is not too difficult to hit.
- `chain_lightning`: chains to at most 6 targets.
- `chain_lightning`: each jump chains to only one additional target at a time.
- `chain_lightning`: next target is whichever target is closest within 3 blocks
  of the last affected target.
- `thunderclap`: stun only, with a 3.5 second stun duration.

### Pressure

Source line: session `019e7f50...` line 54019.

- `pressure_burst`: charged shot by holding cast for up to 4 seconds.
- Each second charged adds bonus damage.
- Fires like a larger version of `air_shot`, at the same speed.
- `pressure_burst` travels 20 blocks before disappearing if it has not hit.
- `air_shot` and `bullet_storm` travel 15 blocks before disappearing if they have
  not hit.

### Tornado

Source line: session `019e7f50...` line 54046.

- `twister`: does not lift targets as high, but swirls them more.
- `funnel_cloud`: should lift enemies 12 blocks off the ground, or less in an
  enclosed space.

### Smoke

Source line: session `019e7f50...` line 54064.

- `smoke_bomb`: should be a projectile the caster fires, so it can be thrown at
  the caster's feet or at enemies.

### Dash Feel

Source line: session `019e7f50...` line 57536.

- Aero and other dash-like abilities should feel like burst movement, not an
  instant teleport.
- `dust_devil` was called out as feeling too teleport-like.
- `burrow` may be one of the acceptable exceptions because its fantasy can read
  as disappearance/underground movement.

## Corruptus Notes

### Corruptus Passive

Source line: session `019e7f50...` line 40288.

- Use the name `Soul Harvest`.
- Max stacks: 5.
- Resurrection requires 5 stacks.
- Stacks must provide stat buffs; do not implement only resurrection.

### Void

Source lines: session `019e7f50...` lines 54278, 54317.

- Replace `null_zone` with `void_spawn`.
- `void_spawn`: summon 3 friendly allied `Crawler_Void` mobs to fight for the
  player.
- `void_spawn`: summons disappear after 10 seconds.
- `void_spawn`: cooldown begins after they disappear and lasts 8 seconds.
- `consume`: if an enemy hit by `consume` is below 10% health, it is auto-killed.

### Flame And Hell Flame

Source lines: session `019e7f50...` lines 54118, 54327, 54336.

- Fire-trail style effects may be represented as an actual trail of fire.
- Fire field under caster should behave like `lava_pool`: lights the area around
  the player on fire.
- Fire field must not affect the player or friendly entities.
- Hell Flame uses cone breath in front of the player.
- Hell Flame visuals should use blue fire for all visuals.

### Mentokinesis

Source line: session `019e7f50...` line 54354.

- `dominate`: afflicted target becomes friendly and fights for the caster.
- `dominate`: can be toggled off to release control.
- `dominate`: lasts until toggled off or until the target dies.
- Dominated target follows the player as an ally.
- `mind_shatter`: bright pink explosion on enemies within 6 blocks of the player
  or the player's dominated ally.
- `mind_shatter`: deals significant damage and also damages the dominated ally.
- `hivemind`: temporarily controls all enemy mobs in a 7 block radius to fight
  for the caster for 6 seconds.
- `hivemind`: cannot be canceled early.
- `hivemind`: cooldown begins when effect ends; cooldown is 12 seconds.
- `hivemind` can combine with `mind_shatter`, triggering damage on all currently
  controlled targets.
- Mentokinesis visuals should use bright pink colors.

### Imbuement And Atonement

Source lines: session `019e7f50...` lines 54373, 54382, 54400.

- Imbues are self-only.
- `imbue_power`: dark red glow.
- `imbue_fortitude`: dark green glow.
- `imbue_swiftness`: bright yellow glow.
- Atonement should stay visually holy.

### Scarak

Source line: session `019e7f50...` line 54436.

- `scarak_egg`: use the real existing item `Deco_Scarak_Eggsacks`.
- Egg hatch spawns three existing mobs: `Scarak_Seeker`, `Scarak_Fighter`, and
  `Scarak_Defender`.
- `locust_queen`: spawns `Scarak_Broodmother`; it does not need to start as an
  egg.
- `brood_surge`: skips the Scarak egg waiting period.
- Scaraks are always friendly summons and attack nearby hostile mobs.
- `brood_surge`: affected Scaraks move 40% faster for 6 seconds.

### Primordial

Source lines: session `019e7f50...` lines 54463, 54472, 54481, 54490, 54499,
54508, 54517.

- Transform the player into the actual creature models.
- Each form has two unique abilities.
- The third ability recasts to leave the form early.
- Form duration is 30 seconds.
- Leaving a form early cancels the form first.
- When a form ends naturally or is canceled, that form goes on a 15 second
  cooldown.
- Player can still swap to other dinosaur forms that are not on cooldown.
- Dino forms cannot use hotbar items except the spellbook.
- Dino forms can punch bare-fisted.

#### Pterodactyl Form

- Use existing mob model `Pterodactyl`.
- Player can fly by holding space.
- Flight should visually use/read like the existing Pterodactyl flight animation.
- `swoop`: only usable while flying. Caster aims at a ground point and slams down
  there, dealing damage in an 8 block radius and knocking mobs away from impact.
- `carry_on`: player punches a single target to latch onto it. The form should
  look like it is carrying the target.
- Caster can use `carry_on` again to drop the carried target.
- Enemies cannot attack while carried.
- Allies can be carried, and while carried allies can still attack, use weapons,
  and use their spellbook.
- Pterodactyl has the lowest bare-fist damage of the three dino forms.

#### Trillodon Form

- Use existing mob model `Trillodon`.
- Use `stampede` and `horn_guard` as the two abilities.
- `horn_guard`: defensive stance makes the form unable to be knocked back.
- `horn_guard`: absorbs all damage taken by allies within a 7 block radius.
- `horn_guard`: reduces incoming damage to the form by 60%.
- Medium melee damage among the three dino forms.
- Bare-hand mining is extremely fast.
- Each mined block should clear a 5x5 space large enough for the form to fit
  through.
- `stampede` can break through blocks.

#### T-Rex Form

- Use existing model `Rex_Cave`.
- T-Rex has the highest base melee attack.
- `crushing_bite`: executes enemies below 10% health.
- Killing enemies with `crushing_bite` heals the caster by 5% max HP per enemy
  killed.
- One accepted ability cannot be canceled and goes on cooldown for 5 seconds when
  its duration ends.

## Primitive Implications

```text
Recovered intent
├─ summon combat
│  ├─ Scarak summons
│  ├─ Void Spawn Crawler_Void allies
│  ├─ dominated enemies as allies
│  └─ friendly summon target acquisition and attacks
├─ tether / carried target
│  ├─ Arbor vines/root tether
│  ├─ Rip Current water tether exception
│  ├─ thin particle line preference for future tethers
│  └─ Pterodactyl Carry On latch/carry/drop
├─ persistent fields
│  ├─ sinkhole cracks + brown dust
│  ├─ sandstorm/dust devil sand readability
│  ├─ fire field under caster
│  └─ smoke bomb projectile-deployed field
├─ status / coating / bubble
│  ├─ Aqua Barrier opaque blue bubble
│  ├─ imbuement glows
│  ├─ freeze/burn/pink control feedback
│  └─ no accidental permanent class tint
├─ transformation forms
│  ├─ real creature model swap
│  ├─ form-specific abilities
│  ├─ flight / carrying / mining / block breaking
│  └─ leave-form cooldown semantics
├─ dash / burst locomotion
│  ├─ burst feel, not teleport
│  └─ exception handling for burrow-like fantasies
└─ cleanup safety
   ├─ no resource drops from temporary visuals
   ├─ no hidden training dummy dependencies
   └─ dev clear removes lingering visuals
```

## Next Primitive Pass

Use this order after the canon is accepted:

1. Summon combat and controlled-ally primitive.
2. Pull/tether/carry primitive.
3. Persistent field readability primitive.
4. Status/coating/bubble primitive.
5. Transformation/form primitive.
6. Dash/burst locomotion primitive.
7. Cleanup/no-drop/no-training-dummy regression primitive.

Each primitive must define:

- mechanical contract
- visual contract
- accepted Hytale-native assets/entities/effects
- setup scenario
- observability proof
- screenshot/video requirement when visual correctness matters
