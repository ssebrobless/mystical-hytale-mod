# Improvement Proposals - 2026-07-16 (catalog cross-examination)

Six parallel lanes cross-examined every class, style, ability, and passive against
`docs/hytale-catalog/` + `docs/modding-research-2026-07-16.md`. Every proposal carries
the required chain: WHY (gap/misfit) -> INTENT (author citation / grill decision G#) ->
BUILD (exact assets + API entry points) -> CONFIDENCE (PROVEN / GATED Rn) -> EFFORT.
Settled canon (G1-G14, cuts) was built on, never re-litigated. Confidence vocabulary:
PROVEN = mechanism already used by MOTM or named vanilla/mod precedent; GATED = needs
capability gate R6-R11 first.

## 0. Unified engine decisions (dedup of cross-lane consensus)

Four proposals appeared independently in 4+ lanes — treat as settled direction:

| id | decision | consensus basis |
|---|---|---|
| U1 | **Manifest resolver**: replace `HytaleAssetResolver` keyword routing with a per-ability visual manifest JSON (`{cast,travel,impact,loop,model,role,projectileConfig}` per ability id), preflight-validated via `EntityEffect/ProjectileConfig.getAssetMap().getAsset(id)` + `NPCPlugin.hasRoleName`; unresolved rows FAIL preflight, never fall to `FX_VOID_*`/`Spark_Living`. | E-P1, H11, AERO-09, C1. Fixes defect #11; serves G4/G9; PROVEN; S/M |
| U2 | **Native projectile actors**: kill `Spark_Living` production proxies; per-family `Projectile_Config_MOTM_*` + companion appearance JSON via `ProjectileModule.spawnProjectile` (magma_sling is the shipped template). 12-family table in E-P3 (magma=Fireball, wind=Tornado, ice=Ice_Ball/Bolt, lightning=Spirit_Thunder+Spell/Lightning, void=Eye_Void, acid=Scarak spitball/Acid model, earth=Rubble, plant/psychic/radiant/smoke per table). | E-P3, T1, H2, AERO-01, C1. Universal grammar 2; PROVEN; M |
| U3 | **Summon roles -> `Template_Summoned_Ally` variants** (free DespawnTimer=300/SummonerLeashRange=30/combat params/native Attack): rewrite MOTM combat roles as Variants; keep Summon_Driver (livestock) and Tamed_Frosty (mount) special. Per-summon table in C4 (void_spawn=Crawler_Void x3, raise_dead=Skeleton/Skeleton_Mage + Praetorian_Summon_Flames + bone-block grave, scarak_egg=Deco_Scarak_Eggsacks -> delayed Seeker+Fighter+Defender hatch, locust_queen=Scarak_Broodmother, swamp_monster=Crocodile, snow_imp=WinterHoliday_Snowman, shadow_clone=dark Mannequin interim/R11). | E-P4, C3, C4, C10, H4, H8. Universal grammar 4; PROVEN; M |
| U4 | **Tether primitive engine**: one `TetherStateService` + `TetherTickSystem` + pluggable renderer shared by all 9 tether abilities. Primary renderer = bounded ParticleUtil particle chain (gate R6); fallback = GrapplingHook budgeted chain-segment entities (PROVEN precedent). Geyzer `Water_Beam*` kit = Hydro SKIN, not the engine. Per-ability skins: vines=Plant_Vine endpoints+Nature_Buff chain, riptide/rip_current=Water_Beam family (rip_current keeps its approved fluid-trace), anchor_haul=chain-entity route with hook projectile, rift/life_drain=void/Spectre systems, funnel_cloud/tempest=wind. | E-P2, T3, H3, C9. Universal grammar 5, G12; GATED R6 / PROVEN fallback; M/L |

Also engine-level: **E-P5 controlled ally** (below), **E-P6 ParticleUtil fields +
weather-as-ability**, **E-P7 RootInteraction entrypoints** (hybrid: roots own
click/cooldown/static VFX; Java keeps dynamic execution), **E-P8 zero-code sound wiring**
(WorldSoundEventId/Launch/Hit fields; per-class palettes in P5 below; PROVEN; S).

### Controlled ally stack (E-P5 + C2 merged — the W1 build spec)

Mechanism choice from the catalog's three faction layers: **MOTM control state
(authoritative clock/cleanup per G2) + transient per-target `OverrideAttitude`
(friendly toward owner/allies, duration-bound) + behavior source** — E lane argued
reusing summon movement/attack runtimes without role swap; C lane argued a reversible
swap to a `MOTM_Corruptus_ControlledAlly` role (Template_Summoned_Ally behavior) with
original-role capture/restore. DECISION: implement C2's three-layer stack but treat the
role swap as the *behavior* provider only if the summon-runtime reuse (E5 step 2) proves
insufficient in the W1 scenario — start without swap (lower risk, no restore hazard),
add swap only on proof failure. Friendly-fire: generalize
`shouldSuppressFriendlySummonDamage` -> `isOwnedFriendlyOrControlled`. `mind_shatter`
resolves `ControlledAllyRuntime.resolveCenter(owner)` (controlled target position else
caster). Marker per G1: `MOTM_Corruptus_Control_Marker` faint pink tints + pink-colored
`Eye_Void_Smoke_Teal`/`MagicBlast`/`Flying_Orb`; R8 halo upgrade. Evidence:
control_acquired/released. PROVEN mechanisms; L effort.

## 1. Terra (12 proposals, lane report T1-T12)

| id | target | build (condensed) | conf | effort |
|---|---|---|---|---|
| T1 | earth projectiles | U2: `Projectile_Config_MOTM_Terra_Stone_Rubble/_Soil_Debris/_Sand_Vitrification` parents of `Projectile_Config_Rubble` (Model Rubble_Default) + Block_Break_Stone/Sand; Golem_Crystal_Earth/Sand only as silent large-impact tokens | PROVEN | L |
| T2 | all fields | physical-object-first service: owned temp blocks (record owner/original/expiry) via PlaceBlock/BlockAccessor; per-style materials (stone brick, volcanic/magma-cooled, plant, clay, sand, 35 crystal ids); particles accent only | PROVEN | L |
| T3 | vines | U4 skin: Plant_Vine endpoint pieces (support-checked, decorative not collision) + Nature_Buff_Projectile particle chain (R6) or chain-segment fallback | GATED R6 | M |
| T4 | self_petrification | vanilla `Stoneskin` effect + `MOTM_Terra_Petrification_*` (stone tints, Block_Break_Stone); kill Impact_Ice_Shockwave | PROVEN | S/M |
| T5 | Immovable prime | rising-edge one-shot `MOTM_Terra_Immovable_Prime` (0.25-0.5s stone tint + feet burst) + HUD ping; per G8, never a permanent aura | PROVEN | S |
| T6 | stomp/aftershock/rockslide/fracture etc. | data-driven interaction verbs: Selector+Aoe+ApplyForce for knockbacks; `Explode` (DamageBlocks:false) for fracture; PlaceBlock for constructs | PROVEN | M |
| T7 | sand identity | Sand_Storm family + Block_Break_Sand + Soil_Sand/Sandstone; ochre tints; NEVER white smoke (author rejection) | PROVEN | S |
| T8 | pillar_strike | serialized PlaceBlock: Rock_Stone_Brick_Pillar_Base + _Middle segments over 0.7s, launch on completion, Block_Break_Stone per segment | PROVEN | S |
| T9 | gem style | Rock_Crystal_* block anchor (35 ids) + fullbright Light-block glow trick for refraction; Explode entity-only fracture | PROVEN (R9 only if dynamic light needed) | M |
| T10 | lava_pool/obsidian_skin | Rock_Volcanic_LavaCracks + Rock_Magma_Cooled blocks + Block_Lava_Bubbles; Fluid_Lava only as deliberate simulation variant; obsidian = dark tint coat | PROVEN | M |
| T11 | arbor/bloom per-ability | rooted=vine ring, sapling=wire orphaned MOTM_Arbor_Sapling_Pink_Glow + tree token, frolick=flower/moss trail blocks (no acid) | PROVEN | M |
| T12 | quake/sinkhole/soil footprints | keep proven MOTM_Terra_Quake_*/Sinkhole_* grammar; replace proxy carrier with owned block markers + ApplyForce/ApplyEffect | PROVEN | M |

## 2. Hydro (11 proposals, lane report H1-H11)

| id | target | build (condensed) | conf | effort |
|---|---|---|---|---|
| H1 | 18 liquid-style abilities | per-ability phase recipes implementing G12 (full table in lane report): Surf=Bubbles/Water_Bubble_Stream_Alpha/Water_Small_Burst/Water_Beam_Waves `#39e2d0/#087f9b`; Rain=Water_Dripping/Rain_Heavy; Saltwater=Water_Beam kit; Freshwater=Bubbles_Breathing/WateringCan; Bilgewater=Corrupted_Bubbles/Impact_Poison/Tar; Boiling=Water_Beam+Geyzer_Smoke steam | PROVEN | M |
| H2 | icicle style | U2: Projectile_Config_Ice_Bolt/Ice_Ball + IceBoulderTrail travel + Ice_Blast impact + Weapon_Frost_Mist slow; stalactite_crash places physical Rock_Calcite_Stalactite_Large | PROVEN | M |
| H3 | pulls | U4 skins: rip_current=approved fluid-trace + Water_Beam chain; riptide=Water_Beam line; anchor_haul=GrapplingHook chain-entity route with zero-G hook config (never FX_METAL_SPARKS) | PROVEN/R6 | M |
| H4 | swamp_monster | U3: Crocodile appearance (lock says friendly Crocodile) on Template_Summoned_Ally variant | PROVEN | S/M |
| H5 | frosty mount | complete the vanilla mount recipe: keep Yeti+anchors, add `MountMovementConfig:"MOTM_Frosty_Mount"` + MovementConfig JSON (Walk+Dive) + CanInteract->Mount instruction; purge Golem_Crystal drift | PROVEN (live proof owed) | M |
| H6 | rainbow | arc of colored Light-blocks (Dev/Build_Lightsource_{6 colors}) + Aura_Heal/Crown_Gold/Totem_Heal_Sparks; custom MOTM_Rainbow_Arc via R8 only if six-light arc fails the read; never heal-smoke | PROVEN/R8 | M/L |
| H7 | Aqua Barrier | interim per P4 below; destination opaque shell model committed (G6) | GATED | M/L |
| H8 | snow_imp/snowstorm | U3 + Effect_Snow/Ice_Blast; snowstorm local radius-5 field, Totem slow accent | PROVEN | S/M |
| H9 | vapor + iceberg | vapor=Mace_Signature_Cast_Smoke dissolve/reform, no ModelChange; iceberg=temporary Rock_Ice(_Blue) walls + Block_Break_Ice + Weapon_Frost_Mist, owned cleanup | PROVEN/R6 | M |
| H10 | storm-scale weather | `MOTM_Hydro_Ice_Storm/Glacial_Storm` weather JSONs + WeatherResource.setForcedWeather ONLY in isolated worlds, ref-counted restore; piercing_rain stays a local 5x5 cloud | PROVEN API, gated rollout | M/L |
| H11 | resolver | U1 rows `MOTM_Hydro_<Style>_<Ability>_<Phase>` | PROVEN | M |

## 3. Aero (10 proposals, lane report AERO-01..10)

| id | target | build (condensed) | conf | effort |
|---|---|---|---|---|
| A1 | wind projectiles | U2: `Projectile_Config_MOTM_Aero_Wind_Arc/_Vortex/_Pressure` Model Tornado; tune against Feran_Windwalker_Wind_Burst (v6,g0,1s) / _Vortex (v10,g1,15s); Spirit_Wind + Wind_Sparks_Tail + Battleaxe_(Signature_)Whirlwind stack | GATED R6 (trail) | L |
| A2 | wind-blade geometry | air_slash=one 3-wide arc `#FFD700/#E6E6FA`; gale_cutter=two mirrored configs (SpawnRotationOffset) forming X; razor_wind=`AeroWindVolleyState` launches exactly 5 sequential shots via EntityTickingSystem | PROVEN | M |
| A3 | thunder | Spell/Lightning 5-phase + Beam_Lightning2 + Spirit_Thunder + Lightning_Sword (thunderclap cue); chain_lightning = one visible hop at a time (nearest-in-3-blocks, max 6, per-hop ParticleUtil burst) | GATED R6 | M |
| A4 | Portal_Teleport removal | replace ModelVFXId Portal_Teleport with Dagger_Dash + Daggers_Dash_Straight + Wind_Sparks_Tail (already proven in MOTM_Aero_Scream_Move); burst-not-teleport restored | PROVEN | S |
| A5 | pressure_burst | data `Charging` interaction + `PressureChargeState` proxy + EntityScaleComponent growth (R7) over 4s -> release native projectile (speed 32, 20-block TTL); scale the PROJECTILE, never the player | GATED R7 | M |
| A6 | smoke style | smoke_bomb wraps `Projectile_Config_Bomb_Base` -> 6s SmokesRnD+Smoke_Black field; smoke_form per G5: delete ModelChange Bat, shroud + `Intangible` (R10) + DamageResistance{Projectile:0.50} | GATED R10 | M |
| A7 | jet/jump movement | Zephyr pattern (RootInteraction + codec state + tick system + Velocity component) per ability; jet=gold Fire_Charge1/Fire_AoE_Grow afterburner; jump=gold Wind_Sparks_Tail arcs; Wind Walker stays invisible | GATED R6 | M |
| A8 | 9 style stacks | `MOTM_Aero_{Style}_{Phase}` for all non-Scream styles per family mapping; pollution deliberately borrows poison family | PROVEN | L |
| A9 | resolver | U1 rows; fail-closed, keep Scream rows as known-good | PROVEN | M |
| A10 | scream/battle_cry | keep A- Scream stack, apply A4 impact fix, add Battleaxe_Bash_Shockwave to sonic_boom, Battle Cry = radius 8 (G14) 10s ally pulse | PROVEN | S |

## 4. Corruptus (12 proposals, lane report C1-C12)

| id | target | build (condensed) | conf | effort |
|---|---|---|---|---|
| C1 | fire routing | U1 explicit table for all 26 corruptus ability ids; hellfire/soul_scorch can never fall to void | PROVEN | S |
| C2 | dominate/hivemind | the W1 stack (Sec. 0 merged spec) | PROVEN | L |
| C3 | raise_dead | `MOTM_Corruptus_SkeletonMinion(+Mage)` Template_Summoned_Ally variants, Appearance Skeleton/Skeleton_Mage, Praetorian_Summon_Flames spawn + bone-block grave rise | PROVEN | M |
| C4 | summon table | U3 per-summon contract table (authoritative; replaces modelIds/modelId drift) | PROVEN | M |
| C5 | blue fire | per-phase: Fire_Center_Blue (cast/core), Fire_Blue (travel/loop/ground), Fire_Blue_Smoke (impact/expiry) for hellfire/infernal_ground/soul_scorch; custom projectile-shaped blue fire only after R8 | PROVEN interim | S/M |
| C6 | atonement | G10 recipes: Totem_Heal_AoE/BeamStart/GlowStart + temporary Build_Lightsource_White + white `#FFFFFF` primary, gold `#FFD700` glow, `#4B0082` undertone; per-ability MOTM_Corruptus_Atonement_* stacks | PROVEN | M |
| C7 | psychic language | pink-Colored Eye_Void_Smoke/MagicBlast (cast/impact)/Flying_Orb (travel) + faint pink tints; marker on all controlled targets; R8 halo upgrade | PROVEN interim | M |
| C8 | imbuement stances | 3 EntityEffects (dark red #8B0000 / dark green / bright yellow) + body particles (Aura_Sphere + Effect_Fire / Effect_Crown_Gold accents); OverlapBehavior Overwrite + single MOTM runtime source slot; one-shot switch flash | PROVEN | M |
| C9 | necro grammar | Praetorian_Summon_Flames (raise moments) + Spectre_Void_Body (drain/curse) + bone-block grave fields; life_drain endpoint effects until R6 chain | PROVEN/R6 | M |
| C10 | scarak_egg | place `Deco_Scarak_Eggsacks`, 4s hatch, then Spawn exactly Seeker+Fighter+Defender (JoinFlock/FanOut) as summoned roles | PROVEN | M |
| C11 | shadow_clone | G11 interim: silent Template_Summoned_Ally + Mannequin appearance + `#4B0082` tint + void smoke at both step endpoints; R11 for true clone | PROVEN interim | M |
| C12 | Soul Harvest | atomic lethal-save transition (consume 5 stacks -> 50% HP -> 600s lockout, once) + one-shot Praetorian/Spectre burst; HUD remains stack truth | PROVEN | S/M |

## 5. Passives / Perks / Reactions / Mobs (8 proposals, lane report P1-P8)

| id | target | build (condensed) | conf | effort |
|---|---|---|---|---|
| P1 | 5 trigger-moment perks (G8) | Haunting: keep Empty_Role shell + `setAppearance("Wraith")` + Spectre_Void_Body/Effect_Death_Feathers spawn burst + ghostly tints; Neptune's Grace: Totem_Heal_AoE+Extra+Water_Bubble_Stream pulse; Freezing Winds: Ice_Blast+Effect_Snow; Desperation: VoidImpact+Smoke_Black flash; Ignite: Fire_AoE_Grow+Effect_Fire 5s | PROVEN | M |
| P2 | 6 reactions (G7) | per-pair dual-palette bursts + status tints + exact HUD pings (full table in lane report): Storm Surge=Lightning+Water_Bubble_Stream; Mud Snare=Water+Block_Break_Sand; Dust Cyclone=Leaves_Oak_Wind+Sand; Black Steam=Geyzer_Smoke+Corrupted_Bubbles; Gravebind=VoidImpact+Block_Break_Stone; Hellstorm=Lightning+Fire_AoE_Grow; data-driven visualEffectId/hudPing mapping | PROVEN | M |
| P3 | class passive HUD | icon/counter/timer tracker rows using vanilla `UI/StatusEffects/{Burn,Poison,Stamina,HealthRegen}.png` + ItemQualities slot frames; Soul Harvest shows `n/5` and `LOCKOUT 600s`; Aero stays quiet | PROVEN | S/M |
| P4 | Aqua Barrier interim | recompose MOTM_Hydro_Aqua_Barrier: Water_Bubble_Stream(1.6) + Underwater_Effects(1.15) + Bubbles_Breathing(1.35) + blue tints `#6ecbff/#145a9a` + FirstPersonParticles; replaces unverified E_Sphere | PROVEN | S/M |
| P5 | sound palettes | per-class stems (Terra SFX_Golem_Earth_Stomp(_Impact)/SFX_Stone_Break; Hydro SFX_Staff_Ice_Shoot/SFX_Ice_Ball_Death/SFX_Water_MoveIn-Out; Aero SFX_Tornado/SFX_Global_Weather_Thunder; Corruptus SFX_Staff_Flame_Fireball_Launch+Impact/SFX_Portal_Void/SFX_Effect_Burn_World) via ApplicationEffects/DamageEffects fields; zero Java | PROVEN | S |
| P6 | mob title bands | add `Nameplate` component: `<Name> — <Band>\nLevel <n>` + existing threat color; fixes formatMobName dropping identity; NO elite mechanics (G3) | PROVEN | S |
| P7 | 20-perk HUD semantics | HUD entry metadata {icon,frame,state READY/ACTIVE/COOLDOWN/ARMED,counter} + 0.8s PROC flash; icon families mapped per perk type | PROVEN | M |
| P8 | feedback budget | finite Durations, one ping per reaction event, per-player visual ownership/expiry, caps (3 ghosts etc.); cleanup-is-visual enforcement | PROVEN | S |

## 6. Gate dependencies & recommended order

```
W0.4 (perk/reaction wiring)  ──▶ P1, P2, C12 consume it
R6 ParticleUtil ─────────────▶ U4 primary renderer, A3 hops, A7 trails, T3, C9, E-P6
R7 EntityScaleComponent ─────▶ A5 pressure growth
R8 custom particlesystem ────▶ G1 halo, G4 projectile blue fire, C8 flash, H6 arc (cond.)
R10 Intangible ──────────────▶ A6 smoke_form
R11 player-model clone ──────▶ C11 final doppelganger
(everything else is PROVEN and unblocked)
```

Execution order (impact-per-effort, respecting deps):
1. **U1 manifest resolver** (S/M, unblocks honest routing for everything; do with W0.4)
2. **E-P8/P5 sound wiring + A4 Portal_Teleport removal + T7 sand + C5 blue fire + C1**
   (the S-effort, PROVEN, high-visibility batch)
3. **Gates R6/R7/R8 proofs** (small isolated proof commands)
4. **W1 Controlled Ally** per Sec. 0 spec (+C7 psychic language)
5. **U3 summon migration** (C3/C4/C10/H4/H8 + Scarak/void tables)
6. **U2 projectile actors** family-by-family (A2 geometry, H2 icicle, T1 earth first)
7. **U4 tether engine** + per-ability skins (H3, T3, C9)
8. **Field/object pass** (T2/T8/T9/T10/T12, H9, C6/C9 fields) + P1/P2/P3/P4/P6/P7
9. **Style identity sweeps** (H1, A8) as part of W5 with the tightened sweep gate

## 7. Distilled rejection principles (from all six lanes)

- Tint cannot fix silhouette; never recolor `Spark_Living`/wrong-family models.
- Fluids are simulation, not VFX — inert textured blocks for ephemeral visuals.
- Weather is world-scoped — never force it for local abilities.
- OverrideAttitude alone, role-swap alone, or MOTM-state alone each fail control; the
  layered stack is required.
- No custom assets before vanilla composition fails a visual proof (decision rule).
- Never scale/tint/trail the PLAYER for ability feedback unless canon says so (G5/G6).
- Deliberately invisible things (Wind Walker, always-on stat perks) stay invisible.
- Raw vanilla ids returned by the resolver are dropped by the runtime gate — wrap in
  validated `MOTM_*` effects.
