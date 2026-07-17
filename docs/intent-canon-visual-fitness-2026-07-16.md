# Intent Canon & Visual Fitness Audit - 2026-07-16

Purpose: the author-intent record for all 4 classes, 40 styles, 120 abilities, 4 class
passives, and 20 shared perks — plus a fitness audit of how the current implementation's
visuals/behavior match that intent. Built from five parallel intent investigations
cross-referencing the original concept (`motm-hytale-extract/original-concept/`), the
grilled canon docs, the protected style JSONs, and the live resolver/runtime code.

Trigger example: `proxy-magma-blob` rides the vanilla `Slug_Magma` mob for a magma
visual — mechanically PASS, thematically wrong. This doc systematizes finding and fixing
that class of drift. (Note: `Slug_Magma` is proof-harness-only; production `magma_sling`
already uses the Fireball projectile model. `ROLE_SLUG_MAGMA` in the resolver is dead code.)

Evidence tiers used throughout: AUTHOR-STATED (quoted/cited) > INFERRED (from data/naming,
marked) > IMPLEMENTED (code/JSON reality).

---

## 1. Universal Visual Grammar (author's cross-cutting rules)

These rules are grilled canon and govern every ability:

1. **Physical object first, particles second.** Lava ring = actual lava blocks; pillars =
   stone blocks; sapling = tree model. Particles accent, they don't substitute.
2. **Visible travel before impact.** Projectiles/waves must be watchable and dodgeable —
   never instant-hit with an impact flash.
3. **Burst, not teleport.** Dash-family movement shows start burst + trail + end cue over
   time. `dust_devil` was explicitly rejected as too teleport-like; `burrow` is the one
   allowed disappearance exception.
4. **Summons visibly fight.** Spawning is not the fantasy; owner-attributed combat is.
5. **Tethers are visible links.** Thin particle line caster<->target, themed (water
   stream / vine / chain / void pull / wind funnel), synced with movement.
   `rip_current`'s water-fluid trace is an accepted exception.
6. **Coatings hug the body.** Tight tint+model effect; no permanent class tint, no
   permanent trails (Aqua Barrier: one large opaque blue bubble until destroyed).
7. **Composition stack = identity** (from asset research): silhouette first, palette
   second (tint pair + 2-3 same-family particle ids), motion third (cast/travel/impact/
   loop are distinct), silence fourth (strip nameplate/collision on proxies).
   **Max one wrong-family asset per stack.**
8. **Cleanup is part of the visual.** No drops, no stuck visuals, no lingering tints.

## 2. Terra — The Unmovable Mountain

Fantasy: earth, stone, metal, sand, growth; heavyweight defense and terrain control.
Consolidated passive **Immovable**: -20% knockback taken, regen below 30% HP
(1%/s), +50% pickaxe mining window (2s on mine), cave vision light below Y=80 under
cover. Implementation matches authored data (`ClassPassiveManager` verified).
`Subterranean Fortitude` (original second passive) was dropped SILENTLY — record or revive.

High-fidelity outliers (prove the ceiling): **quake** (`MOTM_Terra_Quake_Cast/Impact/Loop`),
**gem**, **sandstorm**, **sinkhole** (`MOTM_Terra_Sinkhole_Cracks`) — per-style MOTM
effect stacks that read correctly.

| Misfit | Current | Should read as | Severity |
|---|---|---|---|
| ALL Terra fields + most projectiles | `Spark_Living` proxy role | earth/stone/plant silhouettes | SYSTEMIC |
| `self_petrification` | `Impact_Ice_Shockwave` | stone-skin hardening (Stoneskin.json exists vanilla) | HIGH |
| Arbor `vines` | `Wind_Sparks_Tail` travel | vine/whip tether (Universal rule 5) | HIGH |
| Bloom `frolick` | Acid_Sparks palette | flower trail on ground (canon) | MED |
| Magma `lava_pool` plan model | `Golem_Firesteel` | lava blocks + `Block_Lava_Bubbles` | MED |
| Arbor blanket model | `Spirit_Root` for whole style | per-ability (sapling=tree, vines=whip) | MED |

Sand canon: white smoke is NOT acceptable for sandstorm/dust_devil — must read as sand
(`MOTM_Proof_Sand_Cloud` direction is right).

## 3. Hydro — waters, ice, snow, storms

Passive **Tidal Flow + Aqua Barrier**: swim speed +40%, oxygen bonus, spell vamp 3%,
shield-fraction barrier with cooldown. Mechanical shield implemented
(`StatusEffectManager` + `MOTM_Hydro_Aqua_Barrier` player effect). Canon visual:
**one large opaque blue bubble until destroyed; no permanent Hydro tint; no permanent
water trail** — the effect JSON is the open fitness question (bubble vs tint).

Locked summon canon: `snow_imp` = `WinterHoliday_Snowman` model (shipped as
`Server/Models/MOTM/Summons/Snow_Imp_Snowman.json`) with hidden mobile driver;
`frosty` = mountable Yeti (`Tamed_Frosty` role). Both must FIGHT for the caster,
not merely exist. Snow is the accepted representative summon baseline.

| Misfit | Current | Should read as | Severity |
|---|---|---|---|
| 6 styles (surf/rain/saltwater/freshwater/bilgewater/boiling) | ALL slots -> generic `MOTM_Hydro_Wave_*` | per-style palettes (surf breaker vs boiling steam vs bilge grime) | SYSTEMIC |
| `rainbow` | `FX_HEAL_SMOKE` | prismatic arc (light/color, not smoke) | HIGH |
| `anchor_haul` | `FX_METAL_SPARKS` | chain/anchor pull tether | HIGH |
| `swamp_monster` | Frog model | crocodile/swamp-creature family | MED |
| `frosty` resolver row | Golem_Crystal in one path vs Yeti canon | Yeti everywhere | MED |
| Icicle family | generic water FX | sharp brittle shards (`IceBall`/`Ice_Bolt` vanilla stack exists) | MED |

Exception register: Saltwater `rip_current` keeps its approved water-fluid trace. Do not
generalize it into the tether primitive.

## 4. Aero — The Storm Dancer

Fantasy: *"Swift as the wind and deadly as lightning."* Palette `#9370DB/#FFD700/#E6E6FA`.
Skirmisher/assassin. Passive **Wind Walker**: +25% move speed, +80% native energy,
**deliberately no visual** and no vertical movement (verticality lives in style abilities).
Implemented data-only — correct by design.

Locked canon: `smoke_form` = smoky body, move through enemies, **50% projectile DR**
(REALIGNMENT's "40% evasion" is stale), no terrain noclip, toggle 7s.
Wind-blade projectiles = pale-yellow arcs with visible travel; `gale_cutter` = X-shaped
crossing slashes; `razor_wind` = five distinct sequential slashes; `chain_lightning` =
one visible hop at a time (max 6, 3-block jumps); `pressure_burst` = hold-charge shot
that visibly grows (P2 representative).

Per-style resolver grades (from the full audit): scream A- (only fully themed style),
tornado B+, jet/thunder/jump B, wind_blade/gale_wizard/pressure C+, smoke C.

| Misfit | Current | Should read as | Severity |
|---|---|---|---|
| `MOTM_Aero_Impact` | `Portal_Teleport` ModelVFX | burst-movement cue (violates rule 3) | HIGH |
| `smoke_form` | `ModelChange: Bat` | smoky humanoid (canon-locked) | HIGH |
| Most Aero projectiles | `Spark_Living` proxy | wind-slash / compression-ring silhouettes | SYSTEMIC |
| Thunder lightning | Void_Dragon lightning FX | `Spirit_Thunder` family (declared placeholder) | MED |
| 9 of 10 styles | generic `MOTM_Aero_{Cast,Move,Impact}` fallbacks | per-style stacks like Scream's | SYSTEMIC |

## 5. Corruptus — corruption, souls, flame, void, swarm

Passive **Soul Harvest**: 5 stacks on kills, +2% dmg & +1% DR per stack, lethal-save at
5 stacks, 600s lockout. Implemented; HUD row exists. Canon flag stands: Corruptus
passive/resource behavior still needs its focused audit.

Locked canon highlights: hell_flame burns **blue**; Mentokinesis control shows **bright
pink markers**; controlled allies never harm caster/allies; Primordial forms need model
identity + form-specific actions + hotbar restrictions + early exit + third-person proof.

| Misfit | Current | Should read as | Severity |
|---|---|---|---|
| Mentokinesis trio | dominate = root+disoriented tokens only | true controlled ally (P0 primitive, greenfield) | BLOCKING |
| `raise_dead` | `Shadow_Knight` model | skeleton minion (`Skeleton`/`Skeleton_Mage` vanilla) | HIGH |
| Fire abilities routing | default to `FX_VOID_CAST` unless id contains fire/hell/infernal/ignite — `hellfire`, `soul_scorch` can miss | fire family; hell_flame = BLUE fire | HIGH |
| `triceratops_form` | spec says Triceratops, effect JSON `ModelChange: Toad_Rhino`, resolver names Trillodon | one identity (canon: dino read) | HIGH |
| `locust_queen` | `Scarak_Fighter` | Scarak_Broodmother (queen silhouette) | MED |
| `scarak_egg` hatch | Seeker/Fighter mix vs canon eggsacks->hatchlings | Deco_Scarak_Eggsacks + hatchling swarm | MED |
| `shadow_step` clone | Shadow_Knight | shadowy copy of the player (needs research) | MED |
| `void_spawn` | modelIds()=Crawler_Void x3 vs modelId()=Spawn_Void drift | Crawler_Void x3 (canon) — fix the drift | LOW |

## 6. Passives, Perks, Reactions — intent vs wiring (NEW DEFECTS FOUND)

The audit found the perk/reaction layer is authored and largely implemented but
**disconnected at three seams** (code-verified, contradicts PERK_RUNTIME_STATUS claims):

| # | Defect | Effect |
|---|---|---|
| D1 | `RuntimePerkManager.onPlayerTick()` never invoked from `MotmRuntimeLoop`/player maintenance | ALL tick-based perk state dead: Accelerate ramp, Bunny Hop, Semiaquatic, Ignite DoT ticks, ghost lifetimes, eco-tree expiry |
| D2 | `PlayerCombatLifecycleActions.onMobKilled()` never calls `RuntimePerkManager.afterMobKilled()` | Haunting ghosts never spawn; kill-triggered perks dead |
| D3 | `ElementalReactionManager.applyMark()` has no ingress from ability combat; `tickAll()` only ages marks | Entire elemental reaction system dormant |

Also: elite mobs disabled (`canBecomeElite()` hard false) despite authored
`elite_titles.json` — confirm intent. Haunting ghost visual = `Empty_Role` +
`Spawn_Void` model: acceptable silhouette, needs ghostly palette pass.

Class passive feedback quality: Corruptus/Hydro HUD good; Terra/Aero weak (Terra
Immovable prime state has no body visual; intentional for Aero, unresolved for Terra).

## 7. Systemic Fitness Ledger (ranked by unlock leverage)

1. **`Spark_Living` as default projectile/field proxy role** — the single biggest
   thematic debt; touched by ~3 classes' projectile styles and ALL persistent fields.
   Fix via per-family silhouettes + `ProjectileModule` native path (see research doc).
2. **Per-style effect stacks exist for only ~4 of 40 styles** (Scream, Quake, Gem,
   Wave-as-group). The REALIGNMENT per-style palette tables were never wired into
   per-style `MOTM_*` EntityEffects (52 shipped today; ~160 needed for full identity).
3. **Keyword-routing brittleness** in `HytaleAssetResolver` (substring matches like
   `contains("fire")`) silently misroutes new/renamed abilities (hellfire case). The
   themed-id gate `asRuntimeEffectId()` only accepts `MOTM_*` ids — vanilla spawner paths
   returned by the resolver are IGNORED at runtime; several resolver rows are dead.
4. **Transformation identity drift** — forms' real visual is EntityEffect `ModelChange`,
   not resolver model fields; three sources disagree (triceratops case).
5. **Summon model drift** — `SummonRuntimeSpecs.modelIds()` vs `modelId()` inconsistency.
6. **Orphans**: `MOTM_Arbor_Sapling_Pink_Glow` unwired; `crawler_void` in specs but
   missing from resolver.

## 8. Undocumented Intent — Author Grill List

Where NO author-stated look/behavior exists (candidates for a grill session before
implementation; do not guess):

- Terra: 17 facets flagged (notably metal-style non-alloy abilities, soil style reads,
  gem `lapidary` presentation details).
- Hydro: icicle per-ability visuals; vapor style; boiling steam read vs generic wave.
- Aero: pollution style palette beyond acid-green; battle_cry aura radius conflict
  (lock 8 vs mockup 15 — lock wins, confirm).
- Corruptus: imbuement glow colors per weapon; atonement holy palette (no vanilla holy
  family exists — see research doc); shadow_step clone fidelity.
- Perks: intended VISIBLE feedback for most movement perks (currently HUD-text only).
- Reactions: intended visual signature per reaction pair (6 pairs, none specified).

## 9. How to Build to Intent (process rules)

1. Per style: read this doc's class section + `style-ability-intent-canon` row BEFORE
   touching runtime; cite both (existing Working Rule, now with fitness columns).
2. Every visual change picks assets from the same elemental family
   (`docs/modding-research-2026-07-16.md` theme catalog) — max one borrowed-family asset.
3. New per-style EntityEffect ids follow `MOTM_{Class}_{Style}_{Cast|Move|Impact|Field}`;
   promote from `MOTM_Proof_*` only after a visual pass in the proof harness.
4. Misfits in this doc's tables are the authoritative visual backlog; wire them into
   style sweep acceptance (a sweep cannot PASS a style with an open HIGH misfit).
5. SYSTEMIC rows are engineering work (resolver/proxy architecture), not per-ability
   polish — schedule as primitives, not sweeps.

## 10. Grill Session Decisions - 2026-07-16 (AUTHOR-STATED, supersedes Sec. 8 where overlapping)

| # | Topic | Decision |
|---|---|---|
| G1 | Mentokinesis control marker | Staged: interim = bright pink OUTLINE read (faint pink tint + pink shimmer particles hugging silhouette; mob keeps own colors), upgrade = custom pink halo particle system after gate R8. True rim-outline is not a confirmed capability; outline is the target read, not the mechanism. |
| G2 | Dominate control model | ~15-20s control window; cooldown starts on release/expiry; recast while active = early release; released target reverts to NORMAL AI (no auto-aggro, no death). `hivemind` = same clock, radius/multi variant. Authored 3s duration is superseded for control (was stun-era data). |
| G3 | Scope stamps | 3-style/9-ability loadout: INTENTIONAL CUT. Elite mobs: DEFERRED (keep data + gate; revisit post-v1). Terra "Subterranean Fortitude": INTENTIONAL CUT (consolidated into Immovable). |
| G4 | Hell_flame blue fire | Destination = custom blue-fire particle system (WV.4/R8). Interim = composition. AMENDED by asset scan: vanilla `Item/Fireplace_Blue/Fire_Blue.particlesystem` (+`Fire_Center_Blue`, `Fire_Blue_Smoke`) is NATIVE blue fire - use it for the interim; custom system likely only needed for projectile-shaped blue fire. |
| G5 | Smoke_form body | Keep the PLAYER's own model wrapped in dense smoke shroud + `Intangible` phasing (gate R10). No ModelChange. Bat morph removed (WV.3). |
| G6 | Aqua Barrier | Opacity IS load-bearing: destination = custom opaque bubble shell model (WV.4, committed not conditional). Interim = blue bubble-particle composition. |
| G7 | Elemental reactions | IN SCOPE for v1. Feedback = dual-tone particle burst at target (both elements' palettes) + reaction-specific status visual + HUD tracker ping. 6 composed EntityEffects; gated behind defect D10 wiring fix. |
| G8 | Perk visibility rule | HUD primary. Body/world visuals ONLY for trigger-moment perks (Haunting, Neptune's Grace, Freezing Winds, Desperation, Ignite AoE). Always-on stat/movement perks stay invisible. |
| G9 | Triceratops_form | `Trillodon` model everywhere (spec, effect JSON, resolver). Display name stays "Triceratops Form". Resolves the 3-way drift. |
| G10 | Atonement palette | Corrupted-holy: WHITE primary, GOLD glows, DARK PURPLE accents (totem heal beams/glows + lightsource assets). Style and ability names unchanged. |
| G11 | Shadow_step clone | Staged: interim = dark-tinted Mannequin silhouette wreathed in void smoke. New research gate R11: player-model cloning onto NPC proxy; upgrade to true doppelganger if proven. |
| G12 | Hydro six-style palettes | Stamped: Surf = bright foamy turquoise breakers; Rain = soft grey-blue drizzle; Saltwater = deep teal currents/undertow; Freshwater = clear light blue-green, restorative; Bilgewater = murky brown-green grime; Boiling = white steam over angry blue. Asset mapping (author-directed liquid mining): Surf = `_Test/WaterRnD` splash toolkit; Rain = `Weather/Rain/*` + `Water_Dripping`; Saltwater = `Weather/Geyzer/Water_Beam*` (beam kit fits rip_current/riptide pulls); Freshwater = WateringCan splashes + bubble streams + `Bubbles_Breathing`; Bilgewater = Mud/Tar splashes + `Cauldron/Corrupted_Bubbles`; Boiling = `Geyzer_Smoke` steam + beam-splash eruptions + lava-splash accents. |
| G13 | Imbuement (CORRECTION) | NOT weapon coating - it is an exclusive self-buff STANCE system, colors already authored: Power = dark red, Fortitude = dark green (+10% heal, abyssal_armor), Swiftness = bright yellow (shadow_haste). Visual contract: full-body aura in authored color, exactly one active, visible flash on stance swap. Sec. 8's "imbuement glow colors" grill item was an audit paraphrase error - REMOVED. |
| G14 | Battle Cry radius | 8 blocks (implementation lock wins over mockup's 15). |

Process addendum (author-directed): before WV work begins, run a verification pass of
every style row in this doc against the authored JSON descriptions in
`src/main/resources/data/styles/*.json` - canon must inherit the author's words, not
scout paraphrases (the G13 error class).
