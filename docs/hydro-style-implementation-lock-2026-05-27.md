# Hydro Style Implementation Lock - 2026-05-27

This file records the current Hydro concept contract used by the runtime and
agent verification harness. It is intentionally concrete so future agents can
compare code, data, and in-game evidence against the intended design.

## Class Passive

- Tidal Flow heals the Hydro player for 3% of Hydro ability damage dealt.
- Hydro swim speed is increased by 40%.
- Underwater breathing duration is increased by 50%.
- Aqua Barrier is a top-layer blue bubble shield worth 10% max HP. It stays on
  until destroyed, then goes on 8 seconds cooldown. Hydro passives must not add a
  permanent body tint or water trail when the barrier is inactive. The visual
  must be first-person safe: world/third-person can show the large bubble shell,
  but first-person must use only non-obstructive water-bubble cues and no
  player/item tint.

## Icicle

- Frozen Needles fires sharp ice shards and applies stacking slow.
- Stalactite Crash auto-targets the nearest four enemies with downward calcite
  stalactites using `Rock_Calcite_Stalactite_Large` as the visual model.
- Skate is a toggle that gives icy momentum, a 15% speed feel, and an icy trail.

## Snow

- Snow Imp summons an animated `WinterHoliday_Snowman` ally.
- Snowstorm is a radius 5, height 4 snow particle storm around the caster.
- Frosty summons a `Yeti` ally. True mounting is a live API proof item; until the
  API exposes a mount-control hook, the Yeti follows and fights as an ally.

## Surf

- High Tide is a broad water surge that pushes/slows enemies and buffs allies.
- Waverider is a forward wave ride with water trail and collision knockback.
- Riptide is an aimed current that displaces and weakens the target.

## Rain

- Piercing Rain creates a 5x5 cloud above the caster, up to 12 blocks high, that
  follows the caster and rains damage on enemies while buffing allies.
- Rainbow creates a rainbow field around the caster that heals allies for 5% max
  HP per second, grants 10% damage, and 5% speed.
- Splash remains the simple water impact burst.

## Boiling

- Scald is an aimed boiling-water projectile/jet.
- Geyser is a ground eruption that launches enemies upward with steam/water.
- Overheat is a pressure/steam buff only. It must not self-burn or self-damage.

## Vapor

- Vapor Vanish applies a pale misty evasion effect with no leftover tint.
- Dispersion has four charges, deals no damage, and slightly displaces enemies
  when reforming.
- Hidrosis is a subtle condensation/evasion veil.

## Iceberg

- Ice Cap creates a 5 second ice tube: blocks stack 3 high around the caster.
  Punched blocks should burst and slow enemies by 30% for 3 seconds; all clear
  without drops.
- Glacier is an ice wall using the same burst/slow/cleanup contract.
- Ice Shelf creates a 3 high by 2 wide ice wall; recast should push it forward
  4 blocks, displacing and slowing enemies.

## Saltwater

- Tide Pool is caster-centered, radius 5, 2 blocks high, lasts 12 seconds, and
  uses salt/ocean water if available. Enemies inside take 20% more damage.
- Abyssal Assist can only be cast while Tide Pool is active and summons a
  friendly `Snapjaw` inside the pool.
- Rip Current is a 5 second crosshair water tether. Outside Tide Pool it drags
  the target with caster movement; inside Tide Pool it pulls the target into the
  pool. Tide Pool and Rip Current vulnerability do not stack intentionally.

## Freshwater

- Leap Frog arms a frog-like jump, has 4 charges, deals no damage, displaces
  enemies on takeoff/landing, and protects the caster from fall damage caused by
  the leap.
- River Rapids is a 7 second toggle that immediately propels the player forward,
  leaves a water trail, and uses a fixed 5 second cooldown on end.
- Swamp Monster summons a friendly `Crocodile` until killed or cancelled.

## Bilgewater

- Bilge Dump sprays `Fluid_Tar` flavored filth: 1% caster max HP DoT per second
  for 5 seconds, 20% slow, and a 10 second Toxic mark.
- Anchor Haul fires a 5 block tether using Hydro impact FX, pulls a
  hit target to one block from the caster, chains nearby Toxic-marked enemies,
  and deals 10% more damage to Toxic targets.
- Oil Spill creates a radius 4 `Fluid_Tar` pool around the caster. Enemies inside
  are slowed 60% and Toxic-marked. Allies inside receive a 40% dodge-style
  defensive chance. The pool must not harm allies and must clean up without
  drops.
