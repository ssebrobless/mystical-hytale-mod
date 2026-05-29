# Aero + Corruptus Implementation Lock - 2026-05-29

This document records the user-approved implementation target for the remaining
Aero and Corruptus style pass. It is the local handoff for runtime/data/scenario
work and should be kept aligned with the style JSON and harness evidence.

## Class Passives

```
╔════════════╦════════════════════════════════════════════════════════╗
║ Class      ║ Passive                                                ║
╠════════════╬════════════════════════════════════════════════════════╣
║ Aero       ║ Wind Walker: +25% move speed and +80% native energy.   ║
║            ║ No visual effect and no extra vertical movement.       ║
╠════════════╬════════════════════════════════════════════════════════╣
║ Corruptus  ║ Soul Harvest: hostile kill grants 1 stack, max 5.      ║
║            ║ Each stack: +2% damage, +1% damage reduction. At       ║
║            ║ 5 stacks, lethal damage resurrects at 50% HP, clears   ║
║            ║ stacks, and starts a 10 minute no-stack lockout.       ║
╚════════════╩════════════════════════════════════════════════════════╝
```

## Aero Styles

```
Wind Blade
├─ Air Slash: crosshair line, 3 block wide sideways pale yellow arc, pierces.
├─ Gale Cutter: crosshair line, X-shaped pale yellow slashes, pierces.
└─ Razor Wind: 5 small consecutive slashes on each affected target.

Thunder
├─ Thunderclap: caster radial stun-only shockwave, 3.5s stun, no knockback.
├─ Smite: crosshair lightning strike, 3x3 impact footprint.
└─ Chain Lightning: max 6 targets, one closest chain jump at a time, 3 block jump radius.

Gale Wizard
├─ Gust: crosshair wind cone/line, push/disorient.
├─ Cyclone Shield: tight wind barrier, 65% projectile DR, 10% all DR, close enemy push.
└─ Tempest: target storm field with pull/slow/repeated wind hits.

Scream
├─ Shriek: forward sound cone, disorient/deafen.
├─ Sonic Boom: forward shockwave with damage and knockback.
└─ Battle Cry: 8 block radius ally/caster buff, +15% damage, +10% speed, 10s duration, 15s cd.

Pressure
├─ Air Shot: fast compressed-air projectile, 15 block range.
├─ Bullet Storm: rapid compressed-air volley, 15 block range.
└─ Pressure Burst: hold up to 4s, larger fast air shot, 20 block range, bonus per charge second.

Tornado
├─ Twister: smaller forward tornado, stronger swirl/pull, low lift.
├─ Funnel Cloud: larger target funnel, lifts up to 12 blocks if space allows.
└─ Eye of the Storm: caster-centered storm zone, ally protection, enemy slow/tick damage.

Smoke
├─ Smoke Bomb: thrown projectile; impact creates 5 block smoke cloud for 6s, blind + 30% slow.
├─ Vanish: self smoke puff, 3s untargetable/stealth, ends on attack/damaging cast.
└─ Smoke Form: smoky body form, move through enemies, no terrain noclip, 50% projectile DR.

Jet
├─ Jet Burst: 3 charge movement dash, no damage, displaces enemies.
├─ Afterburner: 8s speed/accel buff, fire trail, 5s cd after end.
└─ Mach Punch: spellbook/hand-agnostic lunge punch with compressed-air impact.

Jump
├─ Leap: arm next jump for 5s, +60% jump, forward boost, 2 charges, no damage.
├─ Divebomb: airborne-only dive to ground impact, AoE damage, knock away from center.
└─ Hang Time: 3s airborne float/drift, cancel by jump/dive/movement ability.

Pollution
├─ Smog: targeted 5 block lingering toxic cloud, DoT, slow, vulnerability.
├─ Toxic Breath: 8 block cone, 60 degrees, Toxic/poison DoT 5s, green tint/smoke.
└─ Acid Rain: target acid cloud/rain, radius 5, duration 6s, enemy DoT/debuff.
```

## Corruptus Styles

```
Flame
├─ Fireball: crosshair fireball, 3 block explosion, 5s burn.
├─ Ignite: fire effect/DoT interaction around hit or caster-side targets.
└─ Combust: detonates burning enemies; weak fallback if no burn setup.

Necro
├─ Raise Dead: summons one stronger undead ally.
├─ Life Drain: 4s channel tether, damage each second, heals 50% of actual damage.
└─ Death Mark: 8s mark, +20% damage taken; death triggers small enemy-only dark explosion.

Shadow
├─ Shadow Step: fixed-distance collision-safe blink forward, dark smoke start/end.
├─ Umbral Veil: defensive stealth veil, reduced incoming damage, ends on attack/damaging cast.
└─ Dark Embrace: caster or ally/summon dark wrap, damage reduction + small HoT.

Hell Flame
├─ Hellfire: blue-fire cone breath in front of player.
├─ Infernal Ground: blue-fire field under caster, caster/allies/summons safe.
└─ Soul Scorch: single-target blue soul-fire DoT + vulnerability.

Mentokinesis
├─ Dominate: permanent friendly control until toggled off or target dies.
├─ Mind Shatter: bright pink 6 block explosion on dominated ally if present, else caster.
└─ Hivemind: controls enemies in 7 block radius for 6s; 12s cd after; combos with Mind Shatter.

Imbuement
├─ Imbue: Power: self-only dark red damage buff.
├─ Imbue: Fortitude: self-only dark green damage reduction buff.
└─ Imbue: Swiftness: self-only bright yellow speed/attack speed buff.
   All imbuements are mutually exclusive, last 8s, and have 6s cd after end.

Attonement
├─ Sanctuary: placed holy field; heals/protects friendlies, lightly damages/weakens enemies.
├─ Absorb: caster-only white-gold shield/channel, converts absorbed damage to healing.
└─ Purify: radius cleanse for friendlies and white-gold burst damage to enemies.

Void
├─ Rift: black/purple void tear field, pulls/slows enemies.
├─ Void Spawn: 3 friendly Crawler_Void around caster for 10s, 8s cd after.
└─ Consume: damage + heal; executes targets below 10% health.

Scarak
├─ Scarak Egg: spawns Deco_Scarak_Eggsacks; hatches after 4s into Scarak Seeker/Fighter/Defender.
├─ Brood Surge: instantly hatches eggs and grants Scarak summons +40% move speed for 6s.
└─ Locust Queen: summons Scarak_Broodmother directly for 20s.

Primordial
├─ Pterodactyl Form: Pterodactyl model, 30s, flight on Space, low melee.
│  ├─ Swoop: flying-only aimed ground slam, 8 block AoE, knock away.
│  └─ Carry On: punch to carry target; enemies suppressed, allies can still act.
├─ Trillodon Form: Trillodon model, medium melee, fast 5x5 barehand mining.
│  ├─ Stampede: charge, breaks through blocks, damages/knocks enemies.
│  └─ Horn Guard: 5s uncancelable stance, absorbs ally damage within 7 blocks, 60% DR, 5s cd.
└─ Rex_Cave Form: highest melee, 30s.
   ├─ Crushing Bite: heavy bite, executes below 10%, heals 5% max HP per kill.
   └─ Primal Roar: fear away, then slow/weaken.
```
