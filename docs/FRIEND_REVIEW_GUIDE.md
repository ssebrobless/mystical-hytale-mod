# Friend Review Guide

Updated: 2026-05-24

Purpose: give an outside reviewer the fastest path to understand the mod, the
player fantasy, the ability concepts, the Hytale implementation approach, and
the current review branch.

This repo is being prepared so another developer or AI/code-review tool can
inspect the mod and help make abilities function and look closer to the user's
intended concepts.

## Start Here

```text
Reviewer path
+-- 1. README.md
|     +-- project shape, build command, current branch status
+-- 2. docs/FRIEND_REVIEW_GUIDE.md
|     +-- this orientation file
+-- 3. CODEX_CLASS_STYLE_ABILITY_REVIEW_MOCKUP_2026-05-22.md
|     +-- full concept matrix for all 4 classes, 40 styles, 120 abilities
+-- 4. CODEX_CONCEPT_REVIEW_DECISIONS_2026-05-22.md
|     +-- user-approved corrections and clarifications
+-- 5. docs/ABILITY_REFERENCE.md
|     +-- current checked-in JSON/data view of all 120 abilities
+-- 6. docs/hytale-capability-atlas/README.md
      +-- Hytale asset/API research, proof gates, and Terra implementation maps
```

## Source Priority

When two files disagree, use this order:

```text
User-reviewed concept decisions
  > recovered original Hytale concept details
  > capability atlas implementation maps
  > current JSON data
  > old plans/archive notes
```

Important distinction:

- `docs/ABILITY_REFERENCE.md` shows what the current JSON says now.
- `CODEX_CLASS_STYLE_ABILITY_REVIEW_MOCKUP_2026-05-22.md` shows the recovered
  intended function plus Hytale visual/read plan for every active ability.
- `CODEX_CONCEPT_REVIEW_DECISIONS_2026-05-22.md` records user corrections made
  after the recovered concept matrix, so it overrides older concept wording.

## Current Branch For Review

Branch: `codex/terra-magma-review-runtime`

Draft PR: https://github.com/ssebrobless/mystical-hytale-mod/pull/1

The branch currently includes:

- Terra no-resource runtime/audit work.
- Global active ability resource-cost removal.
- `docs/ABILITY_REFERENCE.md`, covering all 40 styles and 120 active abilities.
- `docs/hytale-capability-atlas/`, covering Hytale capability research and Terra
  implementation maps.

## Global Gameplay Direction

```text
MOTM active abilities
+-- no class resource spending
+-- balanced by cooldowns, durations, charges, toggles, action timing,
|   movement, positioning, item requirements, and cleanup rules
+-- must be tested with normal player controls, not only dev commands
+-- must protect caster, allies, and allied summons unless explicitly approved
+-- must look like the style concept, not just "some particles happened"
```

Global safety rule:

- Abilities, passive effects, fields, summons, hazards, debuffs, pulls, slows,
  roots, and knockbacks must not negatively affect the caster, allied players,
  allied summons, pets, converted mobs, or friendly minions unless an ability
  explicitly says otherwise and the user approves it.

Global visual rule:

- If the concept is a physical object, prefer actual Hytale blocks, temporary
  structures, or block-like models.
- If the concept is a body/item coating, use tight body/item-hugging VFX/tints.
- If the concept is a projectile made from a material, use the closest safe
  material-like projectile, proxy, model, or particle trail.
- Particles should support the main action/object, not replace it when a better
  physical primitive exists.

## Class-Level Concept Notes

### Terra

Terra is earth, stone, terrain, structures, resilience, and grounded control.

User-approved passive changes:

- Remove Earthen Strike.
- Remove Bare-Handed Excavation.
- Immovable only reduces knockback taken by 20%; it does not increase knockback
  dealt.
- Miner's Affinity remains part of the concept. Current reviewed value in
  `terra.json` is 50% faster pickaxe mining.
- Terra should not get a free timed attack proc from its class passive.

Terra implementation direction:

- Use physical blocks for walls, pillars, saplings, flowers, gem cubes, lava
  shells, mud/water/lava fields where safe.
- Use tight coating VFX for Metal Coat, Alloy Enhancement, Gargoyle, Glare, and
  Obsidian Skin's post-lava phase.
- Use ground cracks, dust, debris, and block-break effects to make earth impacts
  readable.

Detailed Terra decisions are in:

- `CODEX_CONCEPT_REVIEW_DECISIONS_2026-05-22.md`
- `docs/hytale-capability-atlas/terra-30-ability-full-scope-cross-audit.md`
- `docs/hytale-capability-atlas/terra-30-ability-implementation-map.md`

### Hydro

Hydro is water, healing, ice, shields, pressure, waves, rain, vapor, and fluid
movement.

User-approved class rule:

- Aqua Barrier should look like a giant bubble around the player's whole body.
- If any Hydro style grants its own shield/defense overlay, Aqua Barrier is the
  top/outer layer and should trigger or deplete first.
- Hydro abilities no longer spend waterskin water. Waterskins are not cast fuel.

Watch especially for:

- Wave movement and water trails.
- Defensive overlays conflicting with Aqua Barrier.
- Rain/rainbow automatic interactions.
- Saltwater Tide Pool/Abyssal Assist/Rip Current chaining.
- Ice and snow styles feeling distinct from each other.

### Aero

Aero is air, speed, evasion, lightning, sound, pressure, smoke, and vertical
movement.

User-approved class concern:

- Wind Walker and any Aero style vertical movement must be tested against every
  Aero style that jumps, dives, hovers, launches, prevents fall damage, or
  modifies momentum.
- Vertical movement effects should layer clearly instead of duplicating,
  blocking, or hiding each other.

Watch especially for:

- Jet Burst, Afterburner, Mach Punch chaining.
- Jump/Divebomb/Hang Time interactions.
- Scream/Sonic Boom sound-wave readability.
- Smoke stealth/form behavior.
- Tornado and Gale Wizard field control.

### Corruptus

Corruptus is void, fire, darkness, risk, corruption, summons, control, and
transformations.

User-approved passive change:

- Soul Harvest should build to 5 stacks. Each stack should grant Infernal Aura
  bonuses, and 5 stacks should revive/heal to half HP instead of full HP.
- If Soul Harvest resurrection triggers, its 10 minute cooldown applies to all
  Corruptus passive abilities, and passive stacks cannot be gained during that
  cooldown.
- Corruptus abilities no longer spend Souls. Dark power is governed by cooldown,
  duration, risk, status, and transformation/summon rules.

Watch especially for:

- Flame burn stacking and Combust consuming burn.
- Necro summons and Life Drain channeling.
- Shadow teleport/clone/stealth readability.
- Mentokinesis domination and Hivemind interactions.
- Primordial form-specific behavior.

## Full 40-Style Concept Map

This is the high-level style identity map. The exact ability function and visual
rows are in `CODEX_CLASS_STYLE_ABILITY_REVIEW_MOCKUP_2026-05-22.md`.

| Class | Style | Abilities | Intended Read |
| --- | --- | --- | --- |
| Terra | Quake | Stomp, Aftershock, Sinkhole | Heavy earth control: jump-land cracks, lingering tremor, buried-look suffocation. |
| Terra | Metal | Iron Wall, Metal Coat, Alloy Enhancement | Steel defense: solid barrier, armor plating, weapon/tool enhancement. |
| Terra | Magma | Lava Pool, Obsidian Skin, Magma Sling | Molten area denial: lava fields/shells, burn ticks, lava projectile. |
| Terra | Stone | Rubble Rouser, Pillar Strike, Rockslide | Heavy rubble: stone-coated arms, 1x1x4 pillar, falling/rolling rock pressure. |
| Terra | Arbor | Rooted, Vines, Sapling | Living roots: self-root healing, entangling target, ground sapling lure object. |
| Terra | Bloom | Nightshade, Frolick, Cacti Cluster | Toxic floral: flower lure/explosion, flower trail, cactus DoT/explosion. |
| Terra | Self Petrification | Gargoyle, Glare, Tunnel | Statue/stone body: self stone form, petrifying gaze, block-like tunneling. |
| Terra | Soil | Burrow, Mudpit, Debris | Dirt/mud control: whack-a-mole dash, muddy water field, brown debris wave. |
| Terra | Sand | Sandstorm, Dust Devil, Vitrification | Desert combo: sand cloud, dash vortex, glass shard follow-ups. |
| Terra | Gem | Lapidary, Fracture, Refraction | Crystal object play: floating gem cube, expanding explosion, refractive aura. |
| Hydro | Icicle | Frozen Needles, Stalactite Crash, Skate | Sharp ice: needle volley, falling ice, icy sliding movement. |
| Hydro | Snow | Snow Imp, Snowstorm, Frosty | Soft frost: snow minions, attack-slow storm, tanky snow summon. |
| Hydro | Surf | High Tide, Waverider, Riptide | Wave momentum: forward wave, water-trail glide, pulling current. |
| Hydro | Rain | Piercing Rain, Rainbow, Splash | Weather support: damaging rain, automatic healing rainbow, shield splash. |
| Hydro | Boiling | Scald, Geyser, Overheat | Steam pressure: boiling stream, vertical geyser, self-burn power. |
| Hydro | Vapor | Vapor Vanish, Dispersion, Hidrosis | Mist form: vanish/fly, dash, evasive moisture shell. |
| Hydro | Iceberg | Ice Cap, Glacier, Ice Shelf | Heavy ice: ice casing, large glacier form/barrier, crushing shelf. |
| Hydro | Saltwater | Tide Pool, Abyssal Assist, Rip Current | Ocean pressure: pool/orb, abyssal stun/weaken, dragging current. |
| Hydro | Freshwater | Leap Frog, River Rapids, Swamp Monster | River life: leap strike, rapid movement, swamp creature summon. |
| Hydro | Bilgewater | Bilge Dump, Anchor Haul, Oil Spill | Dirty water: toxic cone, hooked anchor drag, oily defensive buff. |
| Aero | Scream | Shriek, Sonic Boom, Battle Cry | Sonic force: deafen cone, shockwave, team/self cry buff. |
| Aero | Jet | Jet Burst, Afterburner, Mach Punch | Speed combat: dash launch, damaging trail, post-dash punch. |
| Aero | Thunder | Thunderclap, Smite, Chain Lightning | Lightning: AoE shock, bolt, chained arcs. |
| Aero | Tornado | Twister, Funnel Cloud, Eye of the Storm | Vortex control: spiral knockback, sustained funnel, calm healing center. |
| Aero | Jump | Leap, Divebomb, Hang Time | Airborne gameplay: launch, dive impact, aerial dodge/float. |
| Aero | Wind Blade | Air Slash, Gale Cutter, Razor Wind | Cutting air: slash line, knockback blade, sharpening aura. |
| Aero | Smoke | Smoke Bomb, Vanish, Smoke Form | Smoke stealth: obscuring field, vanish next-hit, smoke body/form. |
| Aero | Gale Wizard | Gust, Cyclone Shield, Tempest | Refined wind magic: shaped gust, defensive cyclone, stunning storm. |
| Aero | Pressure | Air Shot, Bullet Storm, Pressure Burst | Compressed air: hard projectile, rapid bullets, pressure ring. |
| Aero | Pollution | Smog, Toxic Breath, Acid Rain | Toxic air: haze field, poison cone, corrosive rain. |
| Corruptus | Flame | Fireball, Ignite, Combust | Fire loop: projectile burn, self aura, consume existing burns. |
| Corruptus | Necro | Raise Dead, Life Drain, Death Mark | Undeath: minion rise, drain channel, vulnerability mark. |
| Corruptus | Shadow | Shadow Step, Umbral Veil, Dark Embrace | Darkness: teleport clone, invis next-hit, shadow evasion zone. |
| Corruptus | Hell Flame | Hellfire, Infernal Ground, Soul Scorch | Harsher fire: hell projectile, infernal field, cursed self-risk. |
| Corruptus | Mentokinesis | Dominate, Mind Shatter, Hivemind | Psychic control: gaze/control, mind projectile, collective buff/link. |
| Corruptus | Imbuement | Imbue Power, Imbue Fortitude, Imbue Swiftness | Body enchantment: attack, defense, speed follow-up combat. |
| Corruptus | Attonement | Sanctuary, Absorb, Purify | Cleansing corruption: holy field, absorb-to-heal, cleanse/protect. |
| Corruptus | Void | Rift, Void Spawn, Consume | Cosmic void: pulling rift, void summon, consuming finisher. |
| Corruptus | Scarak | Scarak Egg, Brood Surge, Locust Queen | Insect brood: egg hatch, commander buff, queen summon. |
| Corruptus | Primordial | Pterodactyl Form, Triceratops Form, T-Rex Form | Beast forms: flight/evasion, armored charge, heavy predator damage. |

## Final Shared Perks

The perk system is now a shared 20-choice pool. Players unlock one perk choice every 10 levels through level 100, for 10 total chosen perks. Perks are passive only; active combat remains in class styles.

| Theme | Perks |
| --- | --- |
| Aero | Twinkletoes, Accelerate, Bunny Hop, Big Strides, Sharpshooter |
| Hydro | Neptune's Grace, Semiaquatic, Big Lungs, Rainy Day, Freezing Winds |
| Corruptus | Ignite, Desperation, Haunting, Vampirism, Terror |
| Terra | Heavyweight, Eco-friendly, Mole Man, Blacksmith, Toolsmith |

Runtime/proof status is tracked in `docs/PERK_RUNTIME_STATUS.md`. The in-world proof runner is:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-perk-runtime-proofs.ps1 -WorldName "MOTM Creative Test"
```

## Current Review Questions For Your Friend

Useful review targets:

1. Does the current runtime support the exact mechanics in the concept matrix,
   or are we still faking important parts?
2. Which abilities should be implemented with real Hytale primitives first:
   blocks, fluids, projectile APIs, entity effects, or proxy models?
3. Which current Terra/Magma client crashes are caused by geometry/selection
   size, camera clipping, invalid block placement, or lifecycle cleanup?
4. Where should we add structured logs so visual snapshots are not the only
   proof of success?
5. Does any current ability still risk harming/slowing/debuffing the caster,
   allies, or allied summons?
6. Are the current no-resource cooldowns/durations enough to balance each style,
   or do any abilities need charge, toggle, cancel, or item-condition changes?

## Validation Commands

```powershell
powershell -ExecutionPolicy Bypass -File scripts/audit-no-resource.ps1
powershell -ExecutionPolicy Bypass -File scripts/audit-terra-no-resource.ps1
powershell -ExecutionPolicy Bypass -File scripts/audit-terra-implementation.ps1
powershell -ExecutionPolicy Bypass -File scripts/build-install.ps1
```

## Review Warning

The mod is still in active concept-alignment and Hytale capability-discovery
work. A green build proves compile/install success. It does not prove that every
ability visually and mechanically matches the concept yet. For visual/mechanical
acceptance, require:

- normal player controls,
- third-person review when relevant,
- structured logs,
- target hit/skip evidence,
- cleanup evidence,
- and user visual approval.
