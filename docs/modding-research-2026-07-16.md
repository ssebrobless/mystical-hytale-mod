# Hytale Modding Research - 2026-07-16

Purpose: mined external + local knowledge on how real Hytale mods are built, what the
0.5.6 visual API surface offers, and which vanilla assets best serve MOTM's themes.
Companion to `docs/intent-canon-visual-fitness-2026-07-16.md`; feeds the plan revision in
`docs/completion-plan-2026-07-16.md` §8.

Sources: community docs (hytalemodding.dev, hytale-docs.com, official modding strategy
post), open-source plugin repos, javap against the local 0.5.6 `HytaleServer.jar`, direct
`Assets.zip` mining (60,148 members), and MOTM's own capability atlas. Web sources linked
inline; CurseForge pages were Cloudflare-blocked, techniques corroborated via GitHub/docs.

---

## 1. Ecosystem Facts (July 2026)

- Server-first modding: Java plugins + streamed asset packs; clients get everything on
  join, assets cleared on disconnect. No client mods, no shaders, no post-processing.
- Asset pack = `Common/` (models `.blockymodel`, textures, `.particlespawner`,
  `.particlesystem`) + `Server/` (JSON: items, projectiles, effects, roles, interactions),
  shipped inside the plugin jar with `IncludesAssetPack: true` (MOTM already does this).
- Model constraints: cubes/quads only; 32px/block-unit textures (64px for mobs/equipment);
  Blockbench pipeline (community `ModelCreator` tool: github.com/marggx/ModelCreator).
- **Custom particle systems CAN ship in the pack** (same `Server/Particles/` schema as
  vanilla). Caveat: prove a new SystemId in a proof command first — unknown SystemIds are
  silent no-ops and can crash asset validation.
- One custom HUD slot per client; multi-HUD mods use HUDRouter. MOTM owns its slot.

## 2. How Real Mods Do Visuals (community patterns)

Hybrid strategy is universal: **custom assets for signature content, vanilla composition
for common feedback**, plugin code for timing/chaining.

| Pattern | Used by | Relevance to MOTM |
|---|---|---|
| ProjectileConfig + custom/vanilla model + trail particles | Mage Pack/Arcanum, HyGuns | exactly our projectile family need |
| "Visual tokens" (ephemeral visual-only entities) | Magic and Machines | = our NPC visual proxies; industry-validated |
| EntityEffect composition for buffs/DoT/auras | Brutal Impacts, Arcanum | our 52 MOTM_* effects |
| Particle chains along raycast for beams/tethers | multiple | **no native beam primitive exists** — tether primitive must be particle-chain |
| Vanilla-first prototyping, custom-swap per tier | Effect Showcase pattern | matches our proof-then-promote flow |
| Interaction overrides over packet interception for hotbar | ability mods | avoids ghost-item desync |

## 3. The 0.5.6 Visual API Menu (javap-verified)

Tier A (proven in MOTM): `EffectControllerComponent.addEffect` (tint/particles/
ModelVFX/ModelChange), `ModelComponent` override, `ProjectileModule.spawnProjectile`
(+`ProjectileConfig` assets), **`ParticleUtil.spawnParticleEffect(systemId, pos, store)`**,
`BlockPlaceUtils`/`BlockSelection` temp terrain, Nameplate/DisplayName add/remove,
`Invulnerable`, NPC visual proxies.

> **Key discovery:** `ParticleUtil` gives world-space particles WITHOUT an NPC proxy.
> Many pure-visual proxy uses (field pulses, impact accents, tether links) can drop the
> proxy entirely. Needs one proof gate, then it simplifies the field/tether primitives.

Tier B (present, unused/partial): `EntityScaleComponent` (silhouette scaling — sinkhole,
summon sizing, charged-shot growth), `PersistentDynamicLight` (field glow; avoid on
players — tints the model), `Intangible` (smoke/ghost phasing), `SoundUtil` (layered
cast/travel/impact SFX), `BlockParticleSet`, `HiddenFromAdventurePlayers`.

Tier C (stale names from old docs — do NOT use): `ParticleModule`, `SoundModule`,
`TintComponent`, `NameplateComponent`.

## 4. Retheming Recipe (make borrowed assets read intentional)

Identity = composition stack, not one asset:

```
Layer 1  silhouette   ProjectileConfig.Model / ModelChange / proxy model
Layer 2  palette      EntityTopTint + EntityBottomTint + 2-3 same-family particles
Layer 3  motion       distinct cast / travel / impact / loop effects
Layer 4  accent       ModelVFXId, ParticleUtil burst, DynamicLight (non-player)
Layer 5  silence      strip Nameplate/Interactable/RespondToHit/collision on proxies
```

Knobs ranked by leverage: tint pair > particle stack > ModelVFXId > ModelChange >
scale > light > proxy role (last resort).

Proven example — `MOTM_Proof_Magma_Sling_Travel.json`: Fireball silhouette + tints
`#ff7a18/#6b1000` + `Fire_Charge1`/`Impact_Fire`/`Block_Lava_Bubbles` particles.

Decision rule:

```
COMPOSITION ONLY when: vanilla silhouette is close (Fireball, Rubble, Skeleton, Yeti);
  read is mostly color+particles; effect is short-lived; proof gate exists.
SHIP CUSTOM ASSET when: no silhouette fits at any scale; palette unreachable via tints;
  loop needs >3 stacked systems; ModelChange form has no vanilla model;
  a whole style is misfit (fix once in pack, reuse across its abilities).
ALWAYS: author as MOTM_Proof_* first, validate via /motm dev proof, then promote.
```

## 5. Vanilla Asset Catalog (theme -> best candidates)

Vocabulary size (direct jar listing, 2026-07-16): 598 `.particlesystem`, 1,744
`.particlespawner`, 112 ProjectileConfigs, 140 EntityEffects, ~611 NPC blockymodels.
(Prior audit's "2,320 particles" counted systems+spawners together.)

| Theme | Particles/effects | Projectiles | Models/roles | Blocks/fluids |
|---|---|---|---|---|
| Magma/fire | `_Test/Fire/Fire_Projectile` (+Sparks/Trail/Core), `Fireball_Charge_To_4`, `Flamethrower`, `Block_Lava_Bubbles`, `Firelands/Embers`; effects `Status/Burn`, `Lava_Burn`, `Flame_Staff_Burn` | **`Projectile_Config_Fireball(.Charged_0-3)` -> Model `Fireball`**; `Projectiles/Spells/Fireball.json` (Explosion_Medium, Impact_Fire, light `#fb8`) | Spirit_Ember, Golem_Firesteel, Dragon_Fire, Emberwulf | Fluid_Lava, Rock_Volcanic_LavaCracks, `Items/Projectiles/Fireball.blockymodel` |
| Water/ice/snow | `IceBall`, `IceBall_Explosion`, `Underwater_Effects`, `Water_Bubble_Stream`, `Block_Break_Ice/Snow` | `Projectile_Config_Ice_Ball` (Model `Ice_Ball` -> `Iceball.blockymodel`), `Ice_Bolt` | Spirit_Frost, Dragon_Frost, Frostgill, Yeti | Fluid_Water, Rock_Ice(_Blue), Soil_Snow |
| Wind/storm/lightning | `Spell/Lightning` (+Trail/Sparks), `Spirit_Wind` Hand/Tail/Tentacle, `Battleaxe_Signature_Whirlwind`, `Lightning_Sword` | none dedicated — build `Projectile_Config_MOTM_*` like magma_sling did | **Spirit_Thunder**, Feran_Windwalker, Golem_Crystal_Thunder | Dev_Lightsource_Blue/Cyan |
| Earth/stone/sand | `Block_Break_Stone/Sand` (+Dust spawners), `Block_Gem_Sparks` | `Projectile_Config_Rubble` (Model `Rubble_Default`) | Golem_Crystal_Earth/_Sand, Lizard_Sand | Rock_Stone/Basalt/Sandstone, Soil_Sand |
| Plant/vine | `Plant_Eternal`, `Plant_Health/Mana_Tier1-3`, `Nature_Buff_*` (LeavesUp/Mist/Bolt_Trail) | none — particle-only `Nature_Buff_Projectile` | Spirit_Root, Kweebec_Rootling (+Vine attachments) | Moss_Block, Transition_Plant_Moss |
| Void/shadow | `Eye_Void_*`, `Spectre_Void_*`, `VoidImpact`, `MagicPortal_VoidKeyArt` | `Projectile_Config_Bow_Vamp` family (dark) | Dragon_Void, Crawler/Eye/Larva/Spawn/Spectre_Void, Shadow_Knight, Wraith(_Lantern) | Portal_Void texture, `Fireball_Textures/Void.png` |
| Undead/soul | `Praetorian_Summon_Flames`, Spectre systems | Bow_Vamp | **Skeleton, Skeleton_Mage/Archmage**, Skeleton_Giant, Horse_Skeleton | Bone blocks |
| Insect/scarak | `Status_Poisoned`, `Impact_Poison`, Acid spawners | **`Projectile_Config_Scarak_Seeker_Spitball`** (Acid model+trail, Poison 10) | Scarak_Broodmother/Defender/Fighter/Louse/Seeker (+roles) | Insect_Hive transition, Fluid_Poison |
| Psychic/mind | NONE named — nearest `Spell/MagicBlast`, `Flying_Orb`, `GreenOrb`, `Spell/Beam`, Eye_Void eye motifs | none | Eye_Void (mind-eye surrogate); no psychic NPC exists | Rock_Crystal_Purple/Cyan/Iridescent, rune lightstones |
| Poison/acid | Acid family + `Status_Effect/Poison`, `Weather_Posion_Smoke` | Scarak spitball | Scarak set | Fluid_Poison/Slime, poisoned soil/stone/roots |
| Holy/light | `Totem_Heal_AoE/Extra` (+BeamStart/GlowStart), `Spell/Beam` | Healing_Totem deploy | Skeleton_Incandescent Mage/Footman (only "radiant" NPCs) | Build_Lightsource_White, LightStone runes |
| Smoke/gas | `SmokesRnD` family (Fluffy_Floor/Tall_Round/braziers), `Impact_Fire_Smoke` | Bomb configs (Base/Popberry/Stun/Large) | Spectre_Void, Spirit_Thunder | Fluid_Tar |

Gap themes (no vanilla family — custom-asset candidates or deliberate surrogates):
**psychic/mind** (Mentokinesis!), **holy/light** (atonement), pure-wind projectile.
Note `_Test/` particle paths are prototype-tier: keep, but flag for re-verify each patch.

### The magma-projectile answer (trigger example, ranked)

1. `Projectile_Config_Fireball` -> Model `Fireball` (+ charged variants) — MOTM's
   `Projectile_Config_MOTM_Magma_Sling_Visual` already wraps this. For proofs: replace
   `proxy-magma-blob`'s Slug_Magma with this native projectile path.
2. `Fire_Projectile` particle system (Sparks + Fire_Trail + Fire_Core) as travel stack.
3. `Firelands/Embers` + `Block_Lava_Bubbles` as magma-specific accents.
   -> A magma projectile should be the Fireball silhouette with magma tints and ember/lava
   accents; never a mob role.

## 6. What This Changes About the Plan

1. **Tether primitive** = particle-chain between anchors (community-confirmed only path);
   `ParticleUtil` proof first, then the link is proxy-free.
2. **Projectile theming (P2)** = native `ProjectileModule` + per-family
   `Projectile_Config_MOTM_*` configs (magma_sling is the template) instead of
   Spark_Living proxies. Wind/psychic configs need custom or surrogate models.
3. **New proof gates to add** (capability atlas R-gates): ParticleUtil world-space burst;
   EntityScaleComponent scale up/down; pack-shipped custom `.particlesystem` id;
   `PersistentDynamicLight` on a non-player proxy; `Intangible` for smoke_form phasing.
4. **Custom-asset shortlist** (decision rule applied): smoke_form humanoid mist model
   (Bat misfit, no vanilla mist-human), pink Mentokinesis control marker + psychic
   particle system (no vanilla family), Aqua Barrier bubble model (if tint test fails),
   per-class palette particle systems where 3-stack composition falls short.
5. **Perk/reaction wiring bugs** (D1-D3 in the intent doc) must fix before any perk
   visual pass — the visuals would be invisible anyway.
6. **160-effect target**: full 40-style identity needs roughly tripling the 52 MOTM
   EntityEffects; generate per-style stacks from the REALIGNMENT palette tables.

## 7. Source Index

- hytalemodding.dev (llms-full.txt, art-assets, server-first, hotbar, animated textures)
- hytale.com/news/2025/11/hytale-modding-strategy-and-status
- docs.rpg-leveling.zuxaw.com; github.com/marggx/ModelCreator; github.com/HytaleModding/site
- Local: `HytaleServer.jar` javap; `Assets.zip` member reads; `docs/hytale-capability-atlas/`;
  `CODEX_HYTALE_ASSET_API_KNOWLEDGE_2026-05-22.md`; `audits/hytale-asset-library/latest/`;
  `audits/ability-asset-plan/latest/ability-asset-plan.md` (120 rows, 0 missing refs)
