# Codex Realignment Plan — Class/Style/Ability ⇄ Implementation Alignment (2026-05-13)

> **Read this together with `CODEX_PHASE5_FIX_2026-05-13.md` and `CODEX_IMPLEMENTATION_PLAN_2026-05-13.md`.** The Phase 5 fix gets Quake passing; this document gets every other style and ability passing the same bar.
> **Surgical edits only to `data/styles/*.json` and `data/classes/*.json`.** No regenerations. See CLAUDE.md.
> **Order:** Finish the Phase 5 fix first (Quake). Then start here at Section 0 and walk forward. Do not skip ahead.

---

## 0. The contract every class / style / ability must satisfy

Before any code change, internalize the four invariants that make an ability "feel right" in this mod:

### 0.1 Identity rolls down from class → style → ability
- **Class** = identity envelope: color palette, lore voice, passive flavor, resource type.
- **Style** = signature within the class: 10 distinct flavors that *visually* differ from each other even though they share the class palette.
- **Ability** = one of three concrete actions inside a style. Three abilities in the same style use **the same color palette and the same primary particle family**, differing only by *shape* (cast / travel / impact).

This means: a Terra player using Quake should look unmistakably different from a Terra player using Magma, even though both are Terra (brown/green/tan family). A Magma player using `lava_pool` and the same player using `magma_sling` should look related (orange + black smoke), differing by motion pattern.

### 0.2 Activation must mirror the description
The English description (`"description"` field) is the source of truth for what the ability does. If the description says "Bury enemy, dealing suffocation damage" the runtime must produce visible bury + DoT, not just a knockback particle. Phase 5 failed exactly because the visible behavior was a generic AoE while the description promised something else. The Phase-5 fix doc gives the per-ability pattern; this doc generalizes it across 120 abilities.

### 0.3 Visuals must be easily identifiable
Three orthogonal axes:
- **Color** (driven by the style's palette) — answers *who* is casting.
- **Particle family** (driven by the style's signature particle set) — answers *what style*.
- **Motion pattern** (driven by `cast_type` + `target_type`) — answers *what shape* (projectile / AoE / cone / dash / persistent zone).

A spectator should be able to identify any of the three from the visuals alone within ~0.5s.

### 0.4 No identity leakage between styles in the same class
Currently `HytaleAssetResolver` routes mostly at the **class** level — every Terra ability of every Terra style returns roughly the same `FX_STONE_DUST` / `FX_EARTH_IMPACT` / `FX_EARTH_CAST`. This is the root cause of style sameness. The resolver must route at the **(class, style)** level first and only fall back to class defaults if the style has no specific entry.

---

## 1. What's been verified about Hytale's API surface (research summary)

These are the primitives Codex must build on. Sources are the official-style Hytale modding docs and the existing mod source.

### 1.1 EntityEffect JSON — the visual composition language
**Path:** `src/main/resources/Server/Entity/Effects/MOTM/<Name>.json`. Loaded by Hytale's `EffectControllerComponent`.

Confirmed fields (from existing MOTM JSONs in this repo):
- `Duration` (seconds, decimal)
- `OverlapBehavior` (`Overwrite` / `Extend` / `Replace` / `Ignore` — match the existing MOTM files; the docs also mention `Replace` and `Ignore` for full coverage)
- `WorldSoundEventId` (vanilla sound event ID — e.g. `"SFX_Wolf_Alerted"`)
- `ApplicationEffects`:
  - `EntityTopTint` (hex `#RRGGBB`) — tints upper body of the entity the effect is applied to
  - `EntityBottomTint` (hex `#RRGGBB`) — tints lower body
  - `ModelVFXId` (one-shot VFX node id, e.g. `"PrototypeBlockPlaceSuccess"`)
  - `Particles` (array of `{ "SystemId": "<vanilla particle id>" }`)
- Other documented fields we may add later: `Name`, `Infinite`, `Debuff`, `RemovalBehavior`, `StatModifiers` (with `ValueType: Absolute|Percent`), `DamageCalculator`, `ModelChange`. Treat these as advanced; we don't need them for the visual identity pass.

### 1.2 Particle SystemIds — the visual vocabulary
These are **vanilla string IDs**. The current `HytaleAssetResolver.java` already hard-codes ~30 of them — those are the proven-loadable starting palette. Discovery for new IDs is via:
- The in-game **Effect Showcase** mod (`/showcase particles`, `/showcase dump`) — required for finding new IDs.
- Browsing vanilla assets via the in-game **Asset Editor** (`B` → Creative Tools → Assets).
- Inspecting `HytaleServer.jar` contents: `& "$env:JAVA_HOME\bin\jar.exe" tf "$env:APPDATA\Hytale\install\release\package\game\latest\Server\HytaleServer.jar" | Select-String "Particles"`.

**Rule:** Do not invent SystemIds. Only use IDs already cited in `HytaleAssetResolver.java` or confirmed via `/showcase dump`. Unknown IDs cause silent visual no-ops.

### 1.3 Damage + knockback (combat layer)
- `Damage` class: `new Damage(new Damage.EntitySource(attackerRef), DamageCause.getAssetMap().getAsset("Physical"), amount)` then `store.getEventBus().fire(damage, targetRef)`.
- Damage causes: `PHYSICAL`, `PROJECTILE`, `FALL`, `DROWNING`, `SUFFOCATION`, `OUT_OF_WORLD`, `ENVIRONMENT`, `COMMAND`.
- Knockback: `KnockbackComponent kb = new KnockbackComponent(); kb.setVelocity(new Vector3d(dx, dy, dz)); damage.putMeta(Damage.KNOCKBACK_COMPONENT, kb);`. Modes: `Add` / `Set`.
- The mod already uses these via `applyCombat` / `applyTargetToken("knockback", ...)`.

### 1.4 Velocity + position (movement layer)
- `Velocity` component: `set(Vector3d)`, `addForce(Vector3d)`, `getSpeed()`, `setZero()`. The internal field is `protected final Vector3d velocity`.
- `PhysicsBodyState.position` (Vector3d). Set via `transform.setPosition(stateAfter.position)`.
- `GRAVITY_ACCELERATION = 32.0 blocks/sec²` — useful for predicting jump apex / land timing.
- `onGround` boolean is exposed inside the physics force-provider context. To read it from outside (our tick loop), the practical path is **per-tick polling** of vertical velocity + fractional-Y heuristic (already designed in `CODEX_PHASE5_FIX_2026-05-13.md` §2.3).

### 1.5 Projectiles
`ProjectileModule.spawnProjectile(creatorRef, commandBuffer, config, position, direction)`. `ProjectileImpact` interaction runs on collision with `hitEntity` / `hitBlockPosition` / `impactPoint` / `impactNormal`. The mod's `launchProjectiles(...)` already wraps this.

### 1.6 Interactions
- `Interactions` component maps interaction types to root interaction IDs.
- `InteractionType` enum: `Primary` (LMB), `Secondary` (RMB), `Tertiary` (MMB), `Ability1` ... `Ability4`. Our spellbook uses Primary/Secondary/Use (which the mod treats as slot 3 via `Use`).
- `InteractionContext` provides `getEntity()`, command buffer, root interaction IDs.

### 1.7 Player / entity events (confirmed)
- Confirmed: `PlayerConnectEvent`, `PlayerDisconnectEvent`, `PlayerChatEvent`, `PlayerCraftEvent`, `PlayerSetupConnectEvent`, `PlayerSetupDisconnectEvent`, `AddPlayerToWorldEvent`, `DrainPlayerFromWorldEvent`, `PlayerReadyEvent`.
- Confirmed: `EntityRemoveEvent`, `LivingEntityInventoryChangeEvent`, `LivingEntityUseBlockEvent`.
- Confirmed: `PlaceBlockEvent`, `BreakBlockEvent`, `DamageBlockEvent`, `UseBlockEvent`.
- **NOT confirmed:** `PlayerJumpEvent`, `PlayerLandEvent`, `PlayerMoveEvent`, `PlayerInteractEvent`. For jump/land, poll Velocity each tick (already designed).
- `PlayerMouseButtonEvent` is documented in the parent plan as confirmed working ([Hytale-Docs#25](https://github.com/timiliris/Hytale-Docs/issues/25)) but our custom-interaction path supersedes it.

### 1.8 Field visuals at world positions
Hytale doesn't (per public docs) expose "spawn a particle system at a world position not attached to an entity." The mod's existing workaround (in `GameplayPlaybackManager.spawnFieldVisualProxy`) is to **spawn a tiny NPC proxy entity at the field center and apply an EntityEffect to it**. This pattern is correct; we use it for all world-position visuals.

### 1.9 Block manipulation
No documented programmatic block-set API. **Do not** try to "carve a pit" by replacing blocks for Sinkhole or to lay actual ice walls for Glacier. The visual proxy pattern (1.8) plus a barrier-style field is the correct path.

---

## 2. The per-style visual identity table (DESIGN DATA — to be added to JSON)

Each style gets a **palette block** added to its style object. This is the only data change in this section; abilities remain untouched.

### 2.1 JSON shape — new fields per style object

In each of the 40 style objects across `data/styles/{terra,hydro,aero,corruptus}_styles.json`, add a single new sibling field next to `"theme"`:

```json
"palette": {
  "tint_top": "#RRGGBB",
  "tint_bottom": "#RRGGBB",
  "tint_accent": "#RRGGBB",
  "particles_cast":   ["<vanilla SystemId>"],
  "particles_travel": ["<vanilla SystemId>"],
  "particles_impact": ["<vanilla SystemId>"],
  "particles_loop":   ["<vanilla SystemId>"],
  "sound_cast": "<vanilla sound id>",
  "sound_impact": "<vanilla sound id>"
}
```

**Hard rule:** add this exact block in place. Do not reformat any other field. The protected-data clause in CLAUDE.md applies.

### 2.2 Terra (palette anchor: brown / forest green / tan — `#8B4513 / #228B22 / #CD853F`)

| Style | Tint Top | Tint Bottom | Accent | Cast / Travel / Impact / Loop particle family | Sound family | Feel |
| --- | --- | --- | --- | --- | --- | --- |
| **quake** | `#a87455` | `#3d2817` | `#d4a574` | Stone_Dust / Stone_Dust / Mace_Signature_Shockwave / Earth_Brazier_Glow | Combat_Earth_Impact-like | Heavy ground tremor, dust kicked up |
| **metal** | `#9d9da8` | `#525260` | `#d4d4dc` | Block_Break_Metal_Sparks / Block_Break_Metal_Sparks / Block_Break_Metal_Sparks / Block_Break_Metal_Sparks | Anvil/forge ring | Polished, hard, reflective |
| **magma** | `#ff6b1f` | `#4a1810` | `#ffc14d` | Combat/Fire_Stick fire-charge / Impact_Smoke / Impact_Fire / Fire_AoE_Grow | Fire crackle | Molten orange + black smoke, viscous |
| **stone** | `#7b6f65` | `#3a342f` | `#bcb0a4` | Block_Break_Stone_Dust / Block_Break_Stone_Dust / Mace_Signature_Shockwave / Block_Break_Stone_Dust | Rock crack | Slab-grey, blunt, slow |
| **arbor** | `#6fa861` | `#2a4d22` | `#c8e09d` | Healing_Totem leaf-sparks / Wind_Sparks_Tail / Mace_Signature_Shockwave / Totem_Heal_Sparks_Constant | Wood creak + leaf rustle | Living green, leaf swirl |
| **bloom** | `#c46ee0` | `#4a1f5c` | `#ffb3e6` | Acid_Sparks / Acid_Sparks / Impact_Poison / Slowness_Totem | Wet plop | Vivid floral + sickly toxic purples |
| **self_petrification** | `#8a8a8a` | `#2e2e2e` | `#c0c0c0` | Block_Break_Stone_Dust / Block_Break_Stone_Dust / Impact_Ice_Shockwave / Block_Break_Stone_Dust | Stone scrape | Greyscale, statuesque, motionless |
| **soil** | `#6e4b2a` | `#2a1a0e` | `#a8855d` | Block_Break_Stone_Dust (sub for dirt) / Block_Break_Stone_Dust / Battleaxe_Bash_Shockwave / Block_Break_Stone_Dust | Soft thud | Muddy brown, damp |
| **sand** | `#e8d39a` | `#8a6b3a` | `#fff6dc` | Block_Break_Sand_Dust / Block_Break_Sand_Dust / Block_Break_Sand_Dust / Block_Break_Sand_Dust | Desert wind hiss | Warm sand + heat shimmer |
| **gem** | `#f6ecff` | `#6b5ec9` | `#bda6ff` | Block_Break_Crystal_Sparks / Block_Break_Crystal_Sparks / Block_Break_Crystal_Sparks / Block_Break_Crystal_Sparks | Crystal chime | Iridescent purple-white, refractive |

### 2.3 Hydro (palette anchor: dodger blue / turquoise / sky — `#1E90FF / #00CED1 / #87CEEB`)

| Style | Tint Top | Tint Bottom | Accent | Particle family (cast/travel/impact/loop) | Sound family | Feel |
| --- | --- | --- | --- | --- | --- | --- |
| **icicle** | `#d6f7ff` | `#1f7aab` | `#ffffff` | Impact_Ice_Shockwave / Bubbles / Impact_Ice_Shockwave / Water_Bubble_Stream_Alpha | Ice crack | Sharp white-blue, brittle |
| **snow** | `#f4f9ff` | `#6d92b3` | `#e0e8ee` | Bubbles / Water_Bubble_Stream_Alpha / Impact_Ice_Shockwave / Water_Bubble_Stream_Alpha | Soft snow muffle | Powder-soft, gentle white drift |
| **surf** | `#5fc8ff` | `#0a4e7a` | `#a8e6ff` | Water_Bubble_Stream_Alpha / Water_Bubble_Stream_Alpha / Water_Small_Burst / Water_Bubble_Stream_Alpha | Wave whoosh | Bright cyan, kinetic motion |
| **rain** | `#7fb5ff` | `#1c3f6e` | `#cfe2ff` | Water_Small_Burst / Water_Bubble_Stream_Alpha / Water_Small_Burst / Bubbles | Rain patter | Cool wet drizzle pattern |
| **boiling** | `#ffd4a0` | `#a04020` | `#fff2cc` | Impact_Fire / Impact_Smoke / Water_Small_Burst / Impact_Smoke | Hiss + bubble | Steam + amber; transitional fire-water |
| **vapor** | `#e8f4ff` | `#7d9bb5` | `#ffffff` | Totem_Heal_SmokeFlat_Constant / Totem_Heal_SmokeFlat_Constant / Totem_Heal_SmokeFlat_Constant / Totem_Heal_SmokeFlat_Constant | Soft hiss | Diffuse pale mist, half-translucent |
| **iceberg** | `#bfe6f5` | `#1f5470` | `#e7faff` | Impact_Ice_Shockwave / Block_Break_Crystal_Sparks / Impact_Ice_Shockwave / Block_Break_Crystal_Sparks | Heavy ice crunch | Big slab-blue, weight |
| **saltwater** | `#3a8db0` | `#0a2640` | `#8ad0e6` | Water_Bubble_Stream_Alpha / Water_Bubble_Stream_Alpha / Water_Small_Burst / Water_Bubble_Stream_Alpha | Deep ocean rumble | Dark teal, abyssal pressure |
| **freshwater** | `#7fd0a8` | `#1e6f4a` | `#c8f5d6` | Water_Bubble_Stream_Alpha / Water_Bubble_Stream_Alpha / Water_Small_Burst / Water_Bubble_Stream_Alpha | River gurgle | Greener fresher river-blue |
| **bilgewater** | `#7a8a40` | `#2e3a15` | `#c8d090` | Acid_Sparks / Slowness_Totem / Impact_Poison / Slowness_Totem | Sloshy filth | Dirty olive-green, gritty |

### 2.4 Aero (palette anchor: purple / gold / lavender — `#9370DB / #FFD700 / #E6E6FA`)

| Style | Tint Top | Tint Bottom | Accent | Particle family (cast/travel/impact/loop) | Sound family | Feel |
| --- | --- | --- | --- | --- | --- | --- |
| **scream** | `#f4ffff` | `#51d2ff` | `#ffffff` | Battleaxe_Signature_Whirlwind_Spin / Wind_Sparks_Tail / Battleaxe_Bash_Shockwave / Battleaxe_Signature_Whirlwind | Howl / animal cry | Pale ringing sonic wave |
| **jet** | `#ffe28a` | `#8a4a00` | `#fff5c8` | Sword_Signature_Ready_Sparks / Wind_Sparks_Tail / Battleaxe_Bash_Shockwave / Battleaxe_Signature_Whirlwind | Jet whoosh | Sharp gold streaks at speed |
| **thunder** | `#d4c8ff` | `#3a2870` | `#fffadd` | Void_Lightning / Void_Lightning / Sword_Signature_Ready_Sparks / Void_Lightning | Thunder crack | Vivid violet-yellow lightning |
| **tornado** | `#dfe4f4` | `#5b6480` | `#ffffff` | Battleaxe_Signature_Whirlwind / Wind_Sparks_Tail / Battleaxe_Bash_Shockwave / Battleaxe_Signature_Whirlwind | Howling wind | Spiral-grey funnel |
| **jump** | `#ffe7a0` | `#7a5a1a` | `#fff5d0` | Wind_Sparks_Tail / Wind_Sparks_Tail / Battleaxe_Bash_Shockwave / Wind_Sparks_Tail | Wing flap + thud | Buoyant gold trails on arc |
| **wind_blade** | `#e8e8f5` | `#6a6a8a` | `#ffffff` | Wind_Sparks_Tail / Wind_Sparks_Tail / Battleaxe_Bash_Shockwave / Battleaxe_Signature_Whirlwind | Slicing whoosh | Sharp white wind cuts |
| **smoke** | `#aab0bf` | `#202028` | `#d6d8e0` | Mace_Signature_Cast_Smoke / Mace_Signature_Cast_End_Smoke / Mace_Signature_Cast_Smoke / Mace_Signature_Cast_Smoke | Hushed thump | Dark grey clouds |
| **gale_wizard** | `#c8d2ff` | `#4a3a8a` | `#f0e8ff` | Battleaxe_Signature_Whirlwind / Wind_Sparks_Tail / Battleaxe_Bash_Shockwave / Battleaxe_Signature_Whirlwind | Mystical wind hum | Purple-tinted refined gusts |
| **pressure** | `#dfeaff` | `#4a6080` | `#ffffff` | Battleaxe_Bash_Shockwave / Wind_Sparks_Tail / Battleaxe_Bash_Shockwave / Wind_Sparks_Tail | Compression burst | Pulsing pale rings |
| **pollution** | `#a8c46a` | `#3a4818` | `#d4e88a` | Acid_Sparks / Slowness_Totem / Impact_Poison / Slowness_Totem | Wet cough | Sickly green-yellow haze |

### 2.5 Corruptus (palette anchor: dark red / indigo / orange-red — `#8B0000 / #4B0082 / #FF4500`)

| Style | Tint Top | Tint Bottom | Accent | Particle family (cast/travel/impact/loop) | Sound family | Feel |
| --- | --- | --- | --- | --- | --- | --- |
| **flame** | `#ff6e2a` | `#5a0a0a` | `#ffb86b` | Fire_Charge1_Fire / Impact_Smoke / Impact_Fire / Fire_AoE_Grow | Furnace roar | Vibrant orange-red blaze |
| **necro** | `#7a3a6a` | `#1a0a18` | `#c870b8` | VoidSmoke_Impact / VoidSmoke_Impact / VoidImpact / VoidSmoke_Impact | Bone rattle | Sickly violet wisps |
| **shadow** | `#3e3a55` | `#0a0a15` | `#7a6ea0` | Mace_Signature_Cast_Smoke / Void_Sparks / VoidSplash / Mace_Signature_Cast_Smoke | Whisper / hush | Inky black-purple veils |
| **hell_flame** | `#ff4a00` | `#3a0000` | `#ffa030` | Fire_Charge1_Fire / Impact_Fire / Impact_Fire / Fire_AoE_Grow | Demonic roar | Brutal red-orange hellfire |
| **mentokinesis** | `#bfaaff` | `#2a113d` | `#ffeaff` | Eye_Void_Smoke_Teal / Void_Sparks / VoidImpact / Eye_Void_Smoke_Teal | Psychic chime | Iridescent psychic violet |
| **imbuement** | `#a060ff` | `#1a0a3a` | `#e0c8ff` | Void_Sparks / Void_Sparks / VoidImpact / Mace_Signature_Cast_Smoke | Resonant chant | Saturated arcane purple |
| **attonement** | `#fff8c8` | `#a87a30` | `#ffffff` | Totem_Heal_Sparks_Constant / Totem_Heal_SmokeFlat_Constant / Totem_Heal_Sparks_Constant / Totem_Heal_Sparks_Constant | Choral hum | Off-palette golden white (intentional — atonement breaks corruption) |
| **void** | `#7a3aff` | `#0a0a1a` | `#bfaaff` | Void_Sparks / VoidSmoke_Impact / VoidImpact / Void_Sparks | Otherworldly hum | Cosmic deep purple voids |
| **scarak** | `#3a5018` | `#1a0a05` | `#a88a30` | Acid_Sparks / Acid_Sparks / Impact_Poison / Slowness_Totem | Insect chitter | Chitinous green-brown |
| **primordial** | `#5a3a18` | `#1a1005` | `#a86830` | Block_Break_Stone_Dust / Wind_Sparks_Tail / Mace_Signature_Shockwave / Mace_Signature_Cast_Smoke | Primal roar | Earthen brown beast hides |

> The palette table above is the **design ledger**. After Codex transcribes it into the JSONs and into per-style EntityEffect files (Section 4), the table becomes the source of truth for visual identity audits.

---

## 3. The mechanical compliance audit — what each cast_type owes the description

The mod's `GameplayPlaybackManager` already supports most cast_types generically. The gaps are mostly: (a) descriptions promising specific signature behavior the generic pipeline doesn't deliver, (b) visual cues missing, (c) per-style branching in resolver.

Each row below is a **claim** in the description and the **runtime guarantee** that must hold. The right-hand column flags whether the current code satisfies the claim. **You must verify each "OK" by reading the call path before trusting it.**

### 3.1 Terra

| Style | Ability | Description claim | Guaranteed by | Status / Action |
| --- | --- | --- | --- | --- |
| Quake | stomp | "Shockwave that damages and knocks back" (revised: jump-land AoE) | Phase 5 fix | covered in `CODEX_PHASE5_FIX_2026-05-13.md` |
| Quake | aftershock | "Slow and disorient enemies" | ground_zone field + applyTargetEffects | **gap**: terrain_effect `lingering_tremor` not in `applyFieldTerrainEffects` switch — falls through. **Action**: add a `lingering_tremor` branch that re-applies slow + disoriented each tick. |
| Quake | sinkhole | "Bury enemy, dealing suffocation damage" (revised: drag-under) | Phase 5 fix | covered |
| Metal | iron_wall | "Create barrier, heal 10% HP" | barrier cast_type + heal_percent | **OK**. Verify the barrier visual proxy is solid-looking, not particle-soft. |
| Metal | metal_coat | "Gain 20% damage reduction" | self_buff + defense_buff via applyTargetToken | **gap**: defense_buff token must actually mutate incoming damage in `resolveIncomingDamageMultiplier`. Audit: trace the path from `applyTargetToken("defense_buff", ...)` → `statusEffectManager`. Likely already wired but confirm DR is observable. |
| Metal | alloy_enhancement | "Boost next attack by 35%" | self_buff + damage_buff one-shot (`ONE_SHOT_BUFF_SECONDS`) | **OK** — `consumeOneShot(StatusEffect.Type.DAMAGE_BUFF)` exists. |
| Magma | lava_pool | "Lava that burns over time" | ground_zone + burn terrain | **gap**: terrain_effect `lava_pool` not in switch. **Action**: add branch → apply burn token per tick. |
| Magma | obsidian_skin | "Shield equal to 30% HP, reduce damage taken" | self_buff + shield_percent + defense_buff | **OK** — `applyCasterRuntime` returns SupportResolution with shield. |
| Magma | magma_sling | "Ranged attack with slow and burn" | projectile + burn+slow effects | **OK**. Confirm both tokens apply on hit. |
| Stone | rubble_rouser | "Hurl rubble that knocks back" | projectile + knockback | **OK**. |
| Stone | pillar_strike | "Slam a stone pillar down, stunning the target" | ground_strike + delay + stun | **gap**: ground_strike needs a *delayed* impact (telegraph 0.7s) and *upward* visual pillar. Action: in `applySpecificCastRuntime`, when cast_type=ground_strike and ability has `height`, spawn a vertical column of stone-dust at the target ground position during delay, then apply combat on activation. Today the strike fires immediately. |
| Stone | rockslide | "Heavy damage and slowing" | ground_zone + slow | **OK** — terrain_effect `falling_rocks` is in switch (knockback). **Action**: also apply slow per tick (description says slow, not knockback). Adjust branch. |
| Arbor | rooted | "Heal yourself, regenerate" | self_buff + heal_percent + regen | **OK**. |
| Arbor | vines | "Entangle enemy with thorny vines" | line_control + stun | **gap**: line_control already pulls; vines should *root + damage tick over duration_seconds*, not just stun. Action: add a `vines`-specific branch in `startLineControlRuntime` to apply root + dot for `duration_seconds`. |
| Arbor | sapling | "Summon treant" | summon | **OK** — model `MODEL_ROOT_SPIRIT`. |
| Bloom | nightshade | "Toxic spores poison" | cone + dot | **OK**. |
| Bloom | frolick | "Dance among flowers, heal + speed" | self_buff + heal + attack_buff | **gap**: description says speed, ability lists `attack_buff` instead of `speed`. **Action**: surgically add `+speed` to `effect` (becomes `"heal+attack_buff+speed"`). |
| Bloom | cacti_cluster | "Needles slow + poison" | projectile_volley + dot+slow | **OK**. |
| Self_Petrification | gargoyle | "Stone form, heal + shield" | self_buff + heal_percent + shield_percent | **OK**. **Action**: in resolver, when `terrain_effect=stone_shell`, force `EntityTopTint=#8a8a8a` so the player visibly greys out. |
| Self_Petrification | glare | "Petrifying gaze, stuns 2 turns" | gaze + stun | **OK**, but visual is currently weak. **Action**: spawn an `Eye_Void_Smoke_Teal`-like proxy attached to caster during cast for 0.5s. |
| Self_Petrification | tunnel | "Burrow underground, strike from below" | dash + travel_type `burrow_strike` | **gap**: dash currently moves player on the surface. Action: during the dash, suppress the player's vertical visual via tint (set `EntityTopTint=#00000000` if alpha is supported; else apply a stone-dust loop along ground at start position) for the dash duration, then apply combat at end point. |
| Soil | burrow | "Emerge in devastating strike" | dash + knockup | **gap** similar to tunnel — needs underground concealment visual. |
| Soil | mudpit | "Trap enemy in sticky mud" | ground_zone + slow+vulnerability | terrain_effect `mudpit` IS in switch (root + slow). **OK**. |
| Soil | debris | "Fling debris, blinds + weakens" | projectile_volley + vulnerability | **gap**: description says blind, effect field only says vulnerability. **Action**: change effect to `"vulnerability+blind"`. |
| Sand | sandstorm | "Blinding sandstorm DoT" | ground_zone + `sandstorm` terrain (blind+slow in switch) | **OK**. |
| Sand | dust_devil | "Dust devil knocks back" | ground_zone + knockback | terrain_effect `dust_devil` IS in switch. **OK**. |
| Sand | vitrification | "Superheated sand burns" | projectile + burn | **OK**. |
| Gem | lapidary | "Crystal shield" | self_buff + shield_percent | **OK**. |
| Gem | fracture | "Shatter crystals into enemy" | projectile_line | **OK**. **Action**: route resolver to FX_CRYSTAL_SPARKS for travel **and** impact (currently impact uses generic FX_EARTH_IMPACT). |
| Gem | refraction | "Bend light, attack + speed buff" | self_buff + attack_buff | **gap**: description says speed too. Add `+speed` to effect. |

### 3.2 Hydro

| Style | Ability | Description claim | Guaranteed by | Status / Action |
| --- | --- | --- | --- | --- |
| Icicle | frozen_needles | "Rapid ice shards, stacking slow" | projectile_volley + slow_stack | **OK** — `slow_stack` is in TARGET_EFFECT_TOKENS. |
| Icicle | stalactite_crash | "Heavy ice impact" | ground_strike + delay + slow | **gap** same as pillar_strike: delayed vertical telegraph missing. **Action**: same fix pattern. |
| Icicle | skate | "50% evasion" | dash_buff + evasion + terrain `ice_skate_trail` (in switch) | **OK**. |
| Snow | snow_imp | "Summon minion" | summon | **OK**. |
| Snow | snowstorm | "Slow enemy attack speed" | ground_zone + attack_slow + terrain `snowstorm` (in switch) | **OK**. |
| Snow | frosty | "Summon tank golem" | summon | **OK**. |
| Surf | high_tide | "Push back + speed" | wave_line + knockback | **gap**: description says caster also gains speed. **Action**: add `+speed` to effect → `"knockback+speed"`. |
| Surf | waverider | "Shield + speed" | self_buff + shield_percent + speed | **OK**. |
| Surf | riptide | "Pull enemy in, vulnerability" | line_control + pull_force | **OK**. |
| Rain | piercing_rain | "Acidic rain DoT + attack speed" | ground_zone + dot+attack_buff + terrain `piercing_rain` (in switch) | **OK**. |
| Rain | rainbow | "Healing rainbow" | support_zone + heal_percent | **OK**. |
| Rain | splash | "Splash water + shield" | projectile_burst + shield | **OK**. |
| Boiling | scald | "Boiling water, burn + knockback" | projectile + burn+knockback | **OK**. |
| Boiling | geyser | "Geyser of scalding water" | ground_strike + burn | **gap** (delayed strike pattern). |
| Boiling | overheat | "Buff attacks, burn yourself" | self_buff + attack_buff+self_burn | **OK**. |
| Vapor | vapor_vanish | "50% evasion" | self_buff + evasion | **OK**. |
| Vapor | dispersion | "Reform from vapor strike" | dash + damage | **OK**. **Action**: visual — apply a brief 0.3s `Totem_Heal_SmokeFlat_Constant` tint at start and end points. |
| Vapor | hidrosis | "Enhanced dodging" | self_buff + evasion | **OK**. |
| Iceberg | ice_cap | "Shield + freeze attacker" | self_buff + shield + stun | **gap**: stun-on-attacker requires `on_hit` hook that doesn't fire on self_buff. **Action**: register a temporary `on_hit` listener while the buff is active that applies stun token to the attacker (single use). New code path. |
| Iceberg | glacier | "Conjure glacial armor" | barrier + defense_buff | **OK**. **Action**: ensure the barrier visual is large and clearly blocks line-of-sight, not just particles. |
| Iceberg | ice_shelf | "Crush enemy w/ ice shelf" | ground_strike + stun+slow | **gap** (delayed strike pattern). |
| Saltwater | tide_pool | "Slows enemy, speeds you" | ground_zone + slow+attack_buff | **gap**: caster speed not currently applied from this field. **Action**: when caster stands inside, apply speed token to caster each tick. |
| Saltwater | abyssal_assist | "Stun + weaken" | ground_burst + stun+vulnerability | **OK**. |
| Saltwater | rip_current | "Drag enemy with current" | line_control + pull_force + slow | **OK**. |
| Freshwater | leap_frog | "Leap + strike + vulnerability" | dash + launch_height + vulnerability | **OK**. |
| Freshwater | river_rapids | "Speed + momentum" | self_buff + attack_buff + dash_distance | **gap**: description says speed; current effect is attack_buff. Add `+speed`. |
| Freshwater | swamp_monster | "Summon swamp creature" | summon | **OK**. |
| Bilgewater | bilge_dump | "Foul bilge DoT + slow" | cone + dot+slow | **OK**. |
| Bilgewater | anchor_haul | "Haul anchor across enemy" | projectile_line + knockback | **OK**. |
| Bilgewater | oil_spill | "Protective oil" | self_buff + defense_buff + attack_buff | **OK**. |

### 3.3 Aero

| Style | Ability | Description claim | Guaranteed by | Status / Action |
| --- | --- | --- | --- | --- |
| Scream | shriek | "Deafen, reduce accuracy" | cone + deafen | **OK** if `deafen` token reduces accuracy. Verify in `applyTargetToken` switch. |
| Scream | sonic_boom | "Shockwave that stuns" | wave_line + stun + knockback_force | **OK**. |
| Scream | battle_cry | "Boost attack + speed" | self_buff + attack_buff+speed | **OK**. |
| Jet | jet_burst | "Dash attack + launch" | dash + launch_height + knockup | **OK**. |
| Jet | afterburner | "Damage trail" | dash + terrain `ember_trail` (in switch as burn) | **OK**. |
| Jet | mach_punch | "Powerful strike, stun if wall" | dash_strike + stun_if_wall | **OK** — `stun_if_wall` token exists. |
| Thunder | thunderclap | "Stun all + shocked" | self_burst + stun+shocked | **OK**. |
| Thunder | smite | "Lightning bolt, bonus vs shocked" | projectile + lightning | **gap**: "bonus vs shocked" not applied. **Action**: in `applyCombat`, if target has SHOCKED status, multiply outgoing damage by 1.25 when the ability has `effect` containing `"lightning"`. |
| Thunder | chain_lightning | "Continuous lightning" | chain + dot | **OK**. |
| Tornado | twister | "Knock enemies back" | ground_zone + knockback + terrain `twister` (in switch: knockback+disoriented) | **OK**. |
| Tornado | funnel_cloud | "Funnel cloud DoT" | ground_zone + dot + terrain `funnel_cloud` (in switch) | **OK** — pull_force applied. |
| Tornado | eye_of_the_storm | "Heal + shield" | self_buff + heal+shield | **OK**. |
| Jump | leap | "Crash down + knockback + vulnerability" | leap + radius + knockback+vulnerability | **OK**. |
| Jump | divebomb | "Dive from height" | dive_strike + delay + knockback | **OK**. |
| Jump | hang_time | "Stay airborne, dodge" | air_stall + evasion | **OK**. |
| Wind_Blade | air_slash | "Cutting gust" | projectile_line | **OK**. |
| Wind_Blade | gale_cutter | "Wind blade knocks back" | projectile_line + knockback | **OK**. |
| Wind_Blade | razor_wind | "Sharpen wind, attack buff" | self_buff + attack_buff | **OK**. |
| Smoke | smoke_bomb | "Slow enemies, speed yourself" | ground_zone + slow + attack_buff + terrain `smoke_bomb` | **gap**: caster speed missing (only attack_buff in `effect`). **Action**: add `+speed` to effect. |
| Smoke | vanish | "Disappear, +30% next attack" | self_buff + stealth | **OK** — stealth one-shot consumed by next attack via `consumeOneShot(STEALTH)`. |
| Smoke | smoke_form | "40% evasion form" | transformation + evasion | **OK**. |
| Gale_Wizard | gust | "Gust knock back" | projectile + knockback | **OK**. |
| Gale_Wizard | cyclone_shield | "Defensive cyclone" | self_buff + defense_buff + shield | terrain `cyclone_shield` in switch (knockback to entrants) | **OK**. |
| Gale_Wizard | tempest | "Devastating tempest, stuns" | ground_zone + stun + pull_force + terrain `tempest` | **gap**: terrain `tempest` not in switch. **Action**: add branch → apply stun + slow per tick. |
| Pressure | air_shot | "Compressed air, hits hard" | projectile | **OK**. |
| Pressure | bullet_storm | "Rapid air bullets slow" | projectile_volley + slow | **OK**. |
| Pressure | pressure_burst | "Explosive burst" | self_burst + knockback | terrain `pressure_burst` in switch | **OK**. |
| Pollution | smog | "Toxic smog" | ground_zone + dot+slow + terrain `smog` (in switch: blind) | **gap**: description says slow + dot but switch only does blind. **Action**: add slow + dot to the smog branch. |
| Pollution | toxic_breath | "Poisonous fumes weaken" | cone + dot+vulnerability | **OK**. |
| Pollution | acid_rain | "Acidic rain corrodes" | ground_zone + dot+vulnerability + terrain `acid_rain` | **gap**: terrain `acid_rain` not in switch. **Action**: add branch → apply dot + vulnerability per tick. |

### 3.4 Corruptus

| Style | Ability | Description claim | Guaranteed by | Status / Action |
| --- | --- | --- | --- | --- |
| Flame | fireball | "Explosive fire + burn" | projectile + radius (splash) + burn | **OK**. |
| Flame | ignite | "Self on fire, damage nearby" | self_burst + self_burn + AoE + terrain `living_flame` (in switch: burn) | **OK**. |
| Flame | combust | "Consume burns for damage" | execute + consume_burn | **gap**: `consume_burn` not currently a token. **Action**: implement — when ability has `effect=consume_burn`, look up target's active burn stacks, multiply ability damage by `(1 + 0.5 × burnStacks)`, then clear burn from target. Add a `consumeBurnStacks(targetId)` method on `StatusEffectManager`. |
| Necro | raise_dead | "Summon skeleton" | summon | **OK** — `summon_name=skeleton_minion` routes to `MODEL_SHADOW_KNIGHT`. |
| Necro | life_drain | "Damage + heal yourself" | channel + lifesteal | **gap**: channel currently a single hit, not a 3s channel. **Action**: `startChannelRuntime` already exists — verify that `duration_seconds=3` means 3 hits over 3s with lifesteal each tick. |
| Necro | death_mark | "Mark for +25% damage" | curse + vulnerability | **gap**: vulnerability multiplier needs to be 1.25, not the default. **Action**: when ability id is `death_mark`, set the vulnerability multiplier to 1.25. Trace via `setVulnerability(targetId, 0.25, duration)`. |
| Shadow | shadow_step | "Teleport + clone" | teleport + summon clone | **OK** — `shadow_clone` summon routes to `MODEL_SHADOW_KNIGHT`. Verify clone deals damage. |
| Shadow | umbral_veil | "Invisible, +40% next attack" | self_buff + stealth | **OK**. |
| Shadow | dark_embrace | "Shadow zone, dodge chance" | ground_zone + evasion_zone | **gap**: `evasion_zone` token must apply +25% evasion to caster while inside. **Action**: in `applyFieldTerrainEffects`, when `field.ability().getEffect().contains("evasion_zone")` and target is the owner, apply evasion buff each tick. |
| Hell_Flame | hellfire | "Hellfire burns + slows + self-scorches" | projectile_burst + burn+slow | **gap**: self-scorch not applied. **Action**: in `applyCasterRuntime`, if ability id is `hellfire`, apply self_burn token to caster for `cast_time_seconds + recovery_seconds`. |
| Hell_Flame | infernal_ground | "Burning + attack buff" | ground_zone + burn+attack_buff + terrain `infernal_ground` (in switch: burn for targets) | **gap**: attack_buff for caster missing. **Action**: when caster stands inside, apply attack_buff token to caster each tick. |
| Hell_Flame | soul_scorch | "Weaken + slow" | curse + vulnerability+slow | **OK**. |
| Mentokinesis | dominate | "Mind dominate, stun" | gaze + stun+vulnerability | **OK**. |
| Mentokinesis | mind_shatter | "Shatter psyche" | projectile + stun | **OK**. |
| Mentokinesis | hivemind | "Tap dark intelligence" | self_buff + attack_buff | **OK**. |
| Imbuement | imbue_power | "Boost attack" | self_buff + attack_buff | **OK**. |
| Imbuement | imbue_fortitude | "Reinforce body" | self_buff + defense_buff + heal | **OK**. |
| Imbuement | imbue_swiftness | "Enhance reflexes" | self_buff + evasion + attack_buff | **OK**. |
| Attonement | sanctuary | "Holy space heals + cleanses" | support_zone + heal | **gap**: cleanse not applied. **Action**: in field tick, when `ability.getId()=="sanctuary"`, remove all debuff tokens from allies (or owner if no party) inside the field. |
| Attonement | absorb | "Absorb damage → healing" | self_buff + defense_buff + heal | **gap**: "damage → healing" is a special on_damage hook. **Action**: register a temporary on_damage listener while the buff is active that heals caster for `0.5 × damageTaken` (single tier; tuning later). |
| Attonement | purify | "Purge negative effects" | cleanse + defense_buff | **gap**: `cleanse` cast_type exists in resolver but the cleansing logic isn't visible in the pipeline. **Action**: when cast_type=`cleanse`, call `statusEffectManager.clearDebuffs(playerId)`. Add the helper if absent. |
| Void | rift | "Void rift DoT" | ground_zone + dot + terrain `void_rift` (in switch) | **OK** — pull_force applied. |
| Void | void_spawn | "Summon void creature" | summon → MODEL_VOID_SPAWN | **OK**. |
| Void | consume | "Void consumes enemy" | execute + stun | **OK**. **Action**: in resolver, route the cast to FX_VOID_CAST + a large FX_VOID_IMPACT on success. |
| Scarak | scarak_egg | "Lay egg → minion" | summon → MODEL_SCARAK | **OK**. |
| Scarak | brood_surge | "Empower minions" | summon_buff + attack_buff | **gap**: summon_buff path needs to apply attack_buff to all owned summons in `radius`. **Action**: in `handleSummonRuntime` extend with a summon_buff branch: for each `activeSummonsByOwner.get(ownerId)`, apply attack_buff token. |
| Scarak | locust_queen | "Massive locust queen" | summon → MODEL_SCARAK_BROODMOTHER | **OK**. |
| Primordial | pterodactyl_form | "Flight + evasion + attack buff" | transformation + evasion + attack_buff + flight | **OK**. |
| Primordial | triceratops_form | "Armored charge form" | transformation + defense_buff + dash_distance + knockback | **OK**. |
| Primordial | t_rex_form | "Devastating T-Rex" | transformation + attack_buff + stun + radius | **OK**. **Action**: when active, every melee hit by the player should apply stun token. Hook via `armWeaponFollowUp`. |

> Where the table says **"verify"** Codex must *read the call path* and confirm the token actually reaches the entity. Don't trust a name match.

---

## 4. Implementation roadmap (the order of work)

Each phase has an acceptance gate. Do not start the next phase until the current one passes.

### Phase R0 — Prerequisites
Phase 5 (Quake vertical slice) must pass twice consecutively per `CODEX_PHASE5_FIX_2026-05-13.md`. This proves: the input contract works, the resolver routes per-ability, the persistent field system works, and combat resolution honors `ground_targets_only` and ability-specific overrides.

### Phase R1 — Data: stamp the visual palette into every style (no code yet)

**Files:** `data/styles/{terra,hydro,aero,corruptus}_styles.json` (4 files, 40 styles).

**Step R1.1:** For each style object, insert the `"palette"` block from Section 2's tables **directly after** the existing `"theme"` field. Do not reorder anything else. Do not change ability data.

**Step R1.2:** Verify with `./gradlew build` and a server start. Log should still report `[MOTM] Loaded 40 styles, 120 abilities`. No JSON parse errors.

**Acceptance:** Build clean. No behavior change yet (palette isn't read by code yet — that's R3).

### Phase R2 — Code: read the palette into `StyleData`

**File:** `src/main/java/com/motm/model/StyleData.java`.

**Step R2.1:** Add a nested inner class or record:
```java
public static final class Palette {
    private String tint_top;
    private String tint_bottom;
    private String tint_accent;
    private List<String> particles_cast;
    private List<String> particles_travel;
    private List<String> particles_impact;
    private List<String> particles_loop;
    private String sound_cast;
    private String sound_impact;
    public String getTintTop() { return tint_top; }
    public String getTintBottom() { return tint_bottom; }
    public String getTintAccent() { return tint_accent; }
    public List<String> getParticlesCast() { return particles_cast != null ? particles_cast : List.of(); }
    public List<String> getParticlesTravel() { return particles_travel != null ? particles_travel : List.of(); }
    public List<String> getParticlesImpact() { return particles_impact != null ? particles_impact : List.of(); }
    public List<String> getParticlesLoop() { return particles_loop != null ? particles_loop : List.of(); }
    public String getSoundCast() { return sound_cast; }
    public String getSoundImpact() { return sound_impact; }
}
private Palette palette;
public Palette getPalette() { return palette; }
```
(If `StyleData` uses Gson `@SerializedName`, annotate with snake_case names; else match the existing pattern in that file.)

**Step R2.2:** `./gradlew build`. Smoke test: at server start, `LOG.info("[MOTM] terra/quake palette tint_top=" + style.getPalette().getTintTop())` for one style to confirm parse.

**Acceptance:** Build clean. Palette object readable from `StyleData`.

### Phase R3 — Code: route the resolver by (class, style) not just class

**File:** `src/main/java/com/motm/util/HytaleAssetResolver.java`.

**Step R3.1:** Add a method:
```java
public static String resolveStylePalettedEffectName(String classId, String styleId, String phase) {
    // phase ∈ { "Cast", "Travel", "Impact", "Loop", "Field" }
    return "MOTM_" + capitalize(classId) + "_" + capitalize(styleId) + "_" + phase;
}
```

**Step R3.2:** Change `resolve(classId, styleId, ability)` to look up by `(classId, styleId, phase)` first and only fall through to the existing class-level FX_* defaults if no MOTM_<Class>_<Style>_<Phase>.json exists at runtime (check via the asset map).

The check is: `EntityEffect.getAssetMap().contains("MOTM_Terra_Quake_Cast")`. If yes, return it; else fall through to the existing class default.

**Step R3.3:** Once a style has its 4 EntityEffect files (Phase R4), this routing will pick them up automatically. Until R4 lands files, every style falls through to the class default → no visual regression.

**Acceptance:** Build clean. Existing visuals unchanged. With one test JSON named `MOTM_Terra_Quake_Cast.json` placed in the effects folder, casting Stomp now renders that effect's tint+particles instead of the generic class default.

### Phase R4 — Content: author EntityEffect files for every style (the big content pass)

**Files:** `src/main/resources/Server/Entity/Effects/MOTM/MOTM_<Class>_<Style>_{Cast,Travel,Impact,Loop}.json` — up to 4 × 40 = 160 files.

**Step R4.1:** Use one of the existing files (`MOTM_Terra_Gem_Cast.json`, `MOTM_Hydro_Wave_Field.json`) as a template:
```json
{
  "Duration": 0.45,
  "OverlapBehavior": "Overwrite",
  "WorldSoundEventId": "<sound_cast or sound_impact>",
  "ApplicationEffects": {
    "EntityTopTint": "<tint_top>",
    "EntityBottomTint": "<tint_bottom>",
    "Particles": [
      { "SystemId": "<particles_cast[0]>" },
      { "SystemId": "<particles_cast[1] if present>" }
    ]
  }
}
```

Per-phase tuning:
- **Cast** (`Duration` 0.35–0.55, `Overwrite`): the wind-up. Uses `particles_cast`, `sound_cast`.
- **Travel** (`Duration` 0.25–0.45, `Overwrite`): the projectile/dash trail. Uses `particles_travel`.
- **Impact** (`Duration` 0.30–0.50, `Overwrite`): on hit. Uses `particles_impact`, `sound_impact`.
- **Loop** / **Field** (`Duration` 0.55–0.90, `Extend`): persistent zones. Uses `particles_loop`.

**Step R4.2:** **Author one style at a time.** For each style, in this rotation order (1 from each class per session for balance):
1. Terra/Quake (already authored if R4 grabs it from Phase 5)
2. Hydro/Icicle
3. Aero/Thunder
4. Corruptus/Flame
5. Terra/Metal
6. Hydro/Snow
7. Aero/Jet
8. Corruptus/Necro
... continue through all 40.

For each style: build, log in, equip spellbook, run `/motm style <styleId>`, cast each of the 3 abilities. Confirm the new tint + particles are visible. If the style has signature behavior gaps from Section 3, fix them in the same session (Phase R5 for the mechanical fixes).

**Step R4.3 — Decision table for new files:**

| Symptom | Action |
| --- | --- |
| New JSON not loading | Check the file's path is exactly `Server/Entity/Effects/MOTM/<Name>.json`. The filename (without `.json`) must match what `resolveStylePalettedEffectName` returns. |
| Tint visible but particles invisible | The SystemId is wrong or doesn't exist in vanilla. Use `/showcase dump` to find a valid one. Don't invent IDs. |
| Duration too short / too long | Tune `Duration` per phase guidance above. |
| Visual feels wrong color | Adjust tint hex in the JSON. Verify against the design table in Section 2. |
| Two effects overlap weirdly | Set `"OverlapBehavior": "Replace"` for one-shots; `"Extend"` for persistent fields. |

**Acceptance:** Each style, after its 4 EntityEffect files exist, must render visibly distinct from every other style in its class. Manual test: cast Aftershock (Terra/Quake) and then Lava Pool (Terra/Magma) — they must look obviously different in color and particle composition, not just at the impact moment but throughout the field's duration.

### Phase R5 — Mechanical compliance fixes (Section 3's gap rows)

Work the table in Section 3 row by row. Each gap has a specific action listed. Group fixes by file:

**`GameplayPlaybackManager.java` — `applyFieldTerrainEffects` (line ~1594):** add branches for `lingering_tremor`, `lava_pool`, `tempest`, `acid_rain`. Modify `smog` (add slow + dot), `falling_rocks` (add slow per tick). Each branch follows the existing pattern of calling `applyTargetToken(...)` with the right token strings.

**`GameplayPlaybackManager.java` — `applySpecificCastRuntime`:** add a delayed-strike branch for `ground_strike` cast_type with `delay_seconds > 0` and `height > 0`. Spawn a vertical column of class-appropriate particles (stone-dust for Terra, ice for Hydro, fire for Corruptus, wind for Aero) at the target ground position during delay, then apply combat at activation. Affects: pillar_strike, stalactite_crash, geyser, ice_shelf.

**`GameplayPlaybackManager.java` — new `consumeBurnStacks` flow for Combust:** in `applyCombat`, when ability.id == `combust`, look up burn-stack count on target via `statusEffectManager.getBurnStacks(targetId)` (add accessor if missing). Multiply damage by `1 + 0.5 × stacks`. After damage, `statusEffectManager.clearEffectByType(targetId, BURN)`.

**`GameplayPlaybackManager.java` — `applyCombat` damage modifiers:** in `resolveOutgoingDamageMultiplier`, add: if `effect.contains("lightning")` and target has SHOCKED, multiply by 1.25 (Smite).

**`GameplayPlaybackManager.java` — `setVulnerability` override for death_mark:** when ability.id == `death_mark`, force vulnerability multiplier to 1.25 (not the default).

**`StatusEffectManager.java` — `clearDebuffs(playerId)` helper:** add a method that iterates active effects and removes any whose `StatusEffect.Type.isDebuff()` is true. Used by Purify (cleanse cast_type).

**`GameplayPlaybackManager.java` — handle `cleanse` cast_type:** in `executeAbility`, when `cast_type=cleanse`, call `statusEffectManager.clearDebuffs(playerId)`.

**`GameplayPlaybackManager.java` — `startLineControlRuntime`:** add a vines-specific branch where if ability.id == `vines`, apply root + dot for `duration_seconds` to the line targets.

**`GameplayPlaybackManager.java` — `handleSummonRuntime`:** add a summon_buff branch — for cast_type `summon_buff`, iterate `activeSummonsByOwner.get(playerId)` and apply attack_buff token to each. Used by Brood Surge.

**`GameplayPlaybackManager.java` — caster-inside-field hooks:** in `applyFieldTerrainEffects`, when the iterated entity equals `field.ownerRef()`, also apply caster-side buffs for: `tide_pool` (speed for caster), `infernal_ground` (attack_buff for caster), `shadow_zone` (evasion for caster — Dark Embrace).

**`GameplayPlaybackManager.java` — `on_hit` reactive listener for Ice Cap:** when an `ice_cap` self_buff is active and caster takes damage, apply stun token to attacker. Implement via the existing `consumeOneShot` pattern but with a different token type: `STUN_ON_HIT`.

**`GameplayPlaybackManager.java` — `on_damage` reactive listener for Absorb:** when an `absorb` self_buff is active and caster takes damage `D`, heal caster by `0.5 × D`. Hooks the same damage-event listener you'd register in Phase 7's perk integration.

**JSON surgical edits — descriptions vs effects mismatches:** these are single-line additions to existing ability `effect` fields. Surgical only:
- terra/bloom/frolick: `"effect": "heal+attack_buff"` → `"effect": "heal+attack_buff+speed"`
- terra/soil/debris: `"effect": "vulnerability"` → `"effect": "vulnerability+blind"`
- terra/gem/refraction: `"effect": "attack_buff"` → `"effect": "attack_buff+speed"`
- hydro/surf/high_tide: `"effect": "knockback"` → `"effect": "knockback+speed"`
- hydro/freshwater/river_rapids: `"effect": "attack_buff"` → `"effect": "attack_buff+speed"`
- aero/smoke/smoke_bomb: `"effect": "slow+attack_buff"` → `"effect": "slow+attack_buff+speed"`

**Acceptance per Section 3 row:** test the specific ability in-game, confirm description matches behavior. Each fix gets a one-line log to confirm the new path fires. Mark rows as DONE in the table as you complete them — copy the table into a working scratch file if you prefer not to edit this doc.

### Phase R6 — Audit pass + cleanup

Once Phases R1–R5 are done:

**Step R6.1:** Run through every style in every class. Confirm:
- Three abilities cast cleanly via LMB / RMB / Use.
- Each ability's visible output matches its description.
- Each style's three abilities share a coherent palette + particle family (not visually unrelated to each other).
- No two styles in the same class look identical at a glance.

**Step R6.2:** Trim dead code: any class-level `FX_*` constants in `HytaleAssetResolver` that no style references after R3/R4 should be deleted, not left as fallback. Backwards compat doesn't matter — there's only one consumer of this resolver.

**Step R6.3:** Update `README.md` and `project_mystical_hytale_mod.md` memory note to reflect:
- 40 styles authored with distinct visual identity.
- Mechanical compliance audit passing.
- Per-style EntityEffect file convention documented for future styles.

---

## 5. The "easily noticeable" / "easily identifiable" / "accurately reflects" checklist

Apply this to every ability after R5 completes:

1. **Easily noticeable:** within 0.5s of cast, a spectator 15m away sees the cast happen. No silent abilities (every cast has a `*_Cast` EntityEffect firing on the player).
2. **Easily identifiable — by class:** the dominant tint hex falls inside the class palette family (Section 2.2–2.5).
3. **Easily identifiable — by style:** if shown three abilities of the same style in sequence, the three share a particle family and tint accent; if shown three abilities across styles, they look different.
4. **Accurate motion:** projectile abilities visibly travel from caster to target along a straight or arcing line (depending on `travel_type`); AoE abilities visibly fill a ring of `radius`; cone abilities visibly fan in front; persistent fields visibly persist for `duration_seconds`.
5. **Accurate effect feedback:** if the ability slows, the target visibly slows (current slow already applies a movement speed mod). If it stuns, the target's attack animation stops. If it heals, the caster's health visibly rises (HUD pulse).

If an ability fails any of the five, log a follow-up in this doc's Section 5 "Failures" subsection (add it) and fix before declaring the style done.

---

## 6. Hard rules and rails

- **Surgical edits only** to JSON. Add new fields, never reformat. Never regenerate `data/styles/*.json` wholesale.
- **No invented particle SystemIds.** Use only the ~30 already in `HytaleAssetResolver` or those confirmed via `/showcase dump`.
- **No new cast_type tokens** unless a generic implementation absolutely cannot express the design. Prefer adding terrain_effect branches in `applyFieldTerrainEffects` or per-ability_id special cases.
- **No half-implementations.** If a phase's acceptance gate fails, fix it before moving on. Don't leave TODO comments where a real implementation belongs.
- **No `--no-verify` commits.**
- **Order:** R1 → R2 → R3 → R4 (interleaved with R5 per style) → R6. Don't start R4 before R3 (palette routing) lands or new files will appear unused.
- **Visual regression check after every commit:** boot the server, run `/motm style quake`, cast Stomp / Aftershock / Sinkhole. They must still pass Phase 5. Catching regressions early is cheaper than full re-validation.

---

## 7. Reference appendices

### 7.1 File-pattern cheat sheet

| Type | Path pattern | Authoring tool |
| --- | --- | --- |
| Style data | `src/main/resources/data/styles/<class>_styles.json` | Hand-edit. Surgical only. |
| Class data | `src/main/resources/data/classes/<class>.json` | Hand-edit. |
| Per-style EntityEffect | `src/main/resources/Server/Entity/Effects/MOTM/MOTM_<Class>_<Style>_{Cast,Travel,Impact,Loop}.json` | Hand-edit. Template from `MOTM_Terra_Gem_Cast.json`. |
| Asset resolver | `src/main/java/com/motm/util/HytaleAssetResolver.java` | Hand-edit. Route (class, style, phase). |
| Cast pipeline | `src/main/java/com/motm/manager/GameplayPlaybackManager.java` | Hand-edit. |
| Status effects | `src/main/java/com/motm/manager/StatusEffectManager.java` | Hand-edit. |

### 7.2 Discovery commands

| What you want | Command |
| --- | --- |
| Vanilla particle IDs | In-game: `/showcase particles` (Effect Showcase mod) |
| Full vanilla asset dump | In-game: `/showcase dump` |
| Inspect built jar | `& "$env:JAVA_HOME\bin\jar.exe" tf <jar>` |
| Find Hytale API class | `& "$env:JAVA_HOME\bin\jar.exe" tf "$env:APPDATA\Hytale\install\release\package\game\latest\Server\HytaleServer.jar" \| Select-String -Pattern "<ClassName>"` |
| Inspect class methods | `& "$env:JAVA_HOME\bin\javap.exe" -cp <jar> <fully.qualified.ClassName>` |

### 7.3 Sources used to compile this plan

- Hytale modding docs index — `https://hytale-docs.pages.dev/`
- Entity effects reference — `https://raw.githubusercontent.com/vulpeslab/hytale-docs/main/src/content/docs/modding/systems/entity-effects.md`
- Interactions reference — `https://raw.githubusercontent.com/vulpeslab/hytale-docs/main/src/content/docs/modding/systems/interactions.md`
- Damage system reference — `https://raw.githubusercontent.com/vulpeslab/hytale-docs/main/src/content/docs/modding/systems/damage.md`
- Projectiles reference — `https://raw.githubusercontent.com/vulpeslab/hytale-docs/main/src/content/docs/modding/systems/projectiles.md`
- Physics + Velocity component — `https://raw.githubusercontent.com/vulpeslab/hytale-docs/main/src/content/docs/modding/ecs/physics.md`
- Components reference — `https://raw.githubusercontent.com/vulpeslab/hytale-docs/main/src/content/docs/modding/ecs/components.md`
- Plugin events list — `https://raw.githubusercontent.com/vulpeslab/hytale-docs/main/src/content/docs/modding/plugins/events.md`
- Effect Showcase mod — `https://www.curseforge.com/hytale/mods/effect-showcase` (for vanilla particle discovery)
- Asset Editor + Blockbench guide — `https://hytalecharts.com/news/hytale-asset-editor-blockbench-custom-models-guide`
- Hytale Blockbench plugin — `https://github.com/JannisX11/hytale-blockbench-plugin`
- Existing MOTM EntityEffect files in `src/main/resources/Server/Entity/Effects/MOTM/` (31 files) — the canonical schema reference for `ApplicationEffects` keys (`EntityTopTint`, `EntityBottomTint`, `ModelVFXId`, `Particles[].SystemId`, `WorldSoundEventId`, `Duration`, `OverlapBehavior`).
