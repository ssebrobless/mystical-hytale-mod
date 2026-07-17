# Vanilla Asset Catalog (Assets.zip, 60,148 members, 0.5.6)

Counts (direct jar listing 2026-07-16): 598 particle systems + 1,744 spawners ·
436 model wrappers · 669 NPC blockymodels (169 base + 371 attachments) · 4,310 NPC
blockyanims · 454 item blockymodels · 140 entity effects · 112 projectile configs ·
87 projectile appearances · 87 weathers · 1,341 interactions + 611 root interactions ·
3,641 items · 1,177 sound events · 916 block textures · 14 fluid JSONs · 452 UI files.
Prior audit's "2,320 particles" = systems+spawners combined.

## Particle systems by family

Combat 138 (Sword 29, Daggers 28, Fire_Stick 24, Mace 10) · Block 107 (Leaves 28,
Crystal 7, per-material break/run/land) · NPC 53 (Emotions 22, Spectre/Eye_Void 9) ·
Item 53 (Plants 15, Fireplace 5) · Spell 38 (Portal 19) · Weather 34 · Status 17 ·
Deployables 9 · Explosion 6 · Drop 6 · Memories 6 · `_Test`/`_Example` 130 (prototype
tier — usable but re-verify per patch; duplicates production stems).

Themed best-of (production unless noted):

| Theme | Systems |
|---|---|
| Fire | `Fire_Charge1`, `Fire_AoE_Grow`, `Campfire_New_Cartoon`, `Effect_Fire`; **blue fire**: `Item/Fireplace_Blue/Fire_Blue` (+Fire_Center_Blue, Fire_Blue_Smoke); green: `Fire_Green`, `Torch_Fire_Green` |
| Ice | `IceBoulderTrail`, `Ice_Blast`, `Block_Break_Ice`, `Effect_Snow`, `Weapon_Frost_Mist`, IceBall trails (Crystals/Mist/Snowflake) |
| Water | `Water_Run`, `Water_Bubble_Stream`, `Rain(_Heavy/_Light/_Horizontal)`, `Water_Can_Splash`, `_Test/WaterRnD` splash toolkit (7 systems), **`Weather/Geyzer/Water_Beam*` beam kit** |
| Lightning | `Spell/Lightning` (5-phase), `Beam_Lightning2`, `Lightning_Sword` |
| Nature | `Plant_Eternal`, `Plant_Health/Mana_Tier1-3`, `Jungle_Flower_Sparks`, `Nature_Buff_*`, `Leaves_Oak_Wind` |
| Void | `MagicPortal_VoidKeyArt` (12-spawner), `VoidImpact`, `Eye_Void_Smoke_Teal/Green`, `Spectre_Void_Body` |
| Radiant (no Holy family) | `Aura_Heal`, `Aura_Sphere`, `Effect_Crown_Gold`, `Temple_Light_Lantern`, `Totem_Heal_*` (+BeamStart/GlowStart), `Effect_Death_Feathers` |
| Smoke | `Smoke_Black`, `Weather_Posion_Smoke`, `_Test/SmokesRnD` family, `Geyzer_Smoke` (steam) |
| Poison/acid | `Status_Poisoned`, `Effect_Poison`, Acid spawner family, `Impact_Poison`, `Corrupted_Bubbles` |
| Magic | `Azure_Spiral`, `MagicPortal_Default`, `Magic_Sparks_Heavy_GS`, `GreenOrbTrail`, `Flying_Orb`, `Spell/Beam`, Fireworks |
| Beams (test-tier) | `_Test/HealBeams` (green/red), `_Test/MagicRnD/Test_Beam`, `ForgottenTemple_Beam` |
| No vanilla family | blood, holy, psychic/mind — compose or ship custom |

## Model wrappers (the `ModelChange`/appearance id space)

Beast 45 (Yeti, Emberwulf, Slug_Magma, Scarak_*, Toad_Rhino[_Magma], Cactee, Spark_Living)
· Intelligent 73 (Feran incl. Windwalker, Goblin, Kweebec, Outlander, Trork, Saurian)
· Undead 59 (Skeleton + Archer/Mage/Archmage/Knight + Burnt/Frost/Incandescent/Sand/
Pirate variants, Shadow_Knight, Wraith[_Lantern], Ghoul, Werewolf) · Livestock 35 ·
Elemental 13 (Dragon_Fire/Frost/Void, Golem_Crystal_{Earth,Flame,Frost,Sand,Thunder},
Golem_Firesteel, Spirit_{Ember,Frost,Root,Thunder}) · Void 6 (Crawler/Eye/Larva/Spawn/
Spectre_Void, Necromancer_Void) · Flying 20 · Swimming 32 · Wildlife 12 (incl.
**Trillodon**, Mosshorn) · Projectiles 80 (Fireball, Ice_Ball/Bolt, **Tornado**, Rubble,
NPC attack shots incl. Windwalker wind burst/vortex) · Deployables 8 · Pets 4 (Cat,
Corgi, Dog, Kitten) · Boss 1 · Vehicles/Human/seasonal misc.

Theme surrogate table: Fire=Dragon_Fire/Golem_Firesteel/Spirit_Ember; Frost=Spirit_Frost/
Yeti/Bat_Ice; Earth=Spirit_Root/Golem_Crystal_Earth/Cactee/Mosshorn; Water=Fen_Stalker/
Crocodile/Jellyfish_*; Air=Bat/Pterodactyl/Hawk/Scarak_Seeker + Tornado projectile;
Void/undead=Crawler_Void/Spectre_Void/Shadow_Knight/Skeleton_*; Insect=Scarak family.

Animation vocabulary: shared baseline Idle/Walk/WalkBackward/Run/Jump/Fall/Alerted/
Hurt/Death/Sleep/Wake/Crouch*/Spawn + Fluid*/Swim*/Fly* locomotion. Family extras:
Intelligent has social/work set (Greet, Interact, Cheer, Dance*, Mine, Sit, Wave,
Taunt, Kneel...); Beast has Eat/Howl/Roar/Threaten/Dig; Deployables use Deploy/Grow/
Loop/Shoot. Check wrapper `AnimationSets` before requesting a set.

## Blocks / fluids / UI quick reference

- Temp-construct palettes: Rock_Ice(_Blue/_Cracked), Rock_Crystal_{8 colors}_{Block,
  Large,Medium,Small} (35 ids), Rock_Volcanic_LavaCracks, Rock_Magma_Cooled, Basalt,
  Sandstone, Clay_{12 colors}, Runic_Brick, bone blocks, Portal_Void.
- Glow trick: `Build_Lightsource_*` / `Dev_Lightsource_{Blue,Cyan,Orange,Pink,Red,
  Yellow}` — Cube + Transparent + Fullbright + Light{Color} = colored light block.
- Vines: Plant_Vine family (11 variants incl. Wall/Hanging/Liana/Thick) — model-type,
  need support; decorative tether material, not collision walls.
- Stone structures: Rock_Stone_Brick_{Pillar_Base,Pillar_Middle (DrawType Model),
  Wall, Stairs, Beam...}.
- Fluids: 6 block-listed (Water, Lava, Poison, Slime, Slime_Red, Tar) + Fire
  (particle-only). Collisions produce blocks (Water+Lava->cobble/stone).
- UI assets to reference: ProgressBar(+Fill/Effect)@2x, CircularProgressBarMask,
  ContainerFullPatch/Popup panels, Spinner@2x (72-frame), Buttons/Dropdown/InputBox/
  Slider/Scrollbar states, status icons (Burn/Poison/Stamina/HealthRegen), reticles/
  crosshairs (Melee/Bow/Staff/Rifle, DamageConfirm1-5), `Common.ui` macro library.

## Weather ids (87)

Zone1 21 (Rain/Storm/Sunny/Swamp/Fog/caves/dungeon) · Zone2 12 (desert: Sand_Storm,
Blazing_Light, Corrupted_Oasis, Thunder_Storm) · Zone3 13 (boreal: Snow/Snow_Storm/
Northern_Lights) · Zone4 21 (AshWastes, Lava_Fields, GhostForest, Spooky, Underground_
Jungle_{Pink,Red}) · Skylands 6 · Poisonlands 4 · unique 9 (Blood_Moon, Void,
Portals_Void_Event_*, Forgotten_Temple, Default_Flat/Void) · minigame 1.
Weather = palette curves + a Particle SystemId + Tags; forced via WorldConfig/
WeatherResource. Cheap "ability weather" = custom weather JSON + forced switch.
