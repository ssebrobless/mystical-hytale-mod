# Mystical-Hytale-Mod — Next Steps Implementation Plan

## Context

Phase 1 (Foundation) is ~80% complete. The model classes, manager classes, JSON data files, and command wiring are all created. However, several integration gaps remain — systems are built but not connected to each other. This plan covers 6 steps: finishing Phase 1 gaps (Steps 1-4) and slim Phase 2 additions (Steps 5-6).

**Key decision**: We are NOT creating custom biomes, weapons, armor, resource gathering, or economy systems. Hytale provides all of these natively. Our mod is a pure RPG overlay — classes, styles, perks, elemental reactions, races, and mob scaling. The only crafting we add is ~10 mod-specific consumables that buff our custom systems.

**Project root**: `C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/`

---

## Step 1: Phase 1 Gap — Server Tick Handler

**Problem**: `StatusEffectManager.tickAll()`, `ElementalReactionManager.tickAll()`, `StyleManager.tickCooldowns()`, and `ResourceManager.tick()` all exist but are never called. Without a tick loop, effects don't expire, cooldowns don't count down, and TP doesn't regenerate.

**File**: `src/main/java/com/motm/MenteesMod.java`

**Changes**:
- Add a `onServerTick()` method that calls all four tick methods
- Add a TODO comment for registering it with Hytale's tick event (same pattern as existing event handlers)
- Wire DoT damage from `statusEffectManager.tickAll()` return value into entity damage (log for now, same as other Hytale API TODOs)

---

## Step 2: Phase 1 Gap — Elite Title Integration

**Problem**: `elite_titles.json` is loaded by DataLoader but MobScalingManager has no `tryMakeElite()` method, and MobStats has no `eliteTitle`/`isElite` fields. Elites can't actually spawn. Additionally, the current elite titles use our custom 2D game biomes (Forest, Dark Woods, etc.) — these need to be remapped to Hytale's native zones.

**Files**:
- `src/main/resources/data/mobs/elite_titles.json` — Remap biome keys from custom biomes to Hytale zones:
  - `Forest` → `Zone 1` (Emerald Grove / temperate)
  - `Dark Woods` → `Zone 2` (Howling Sands / desert-dark)
  - `Mountains` → `Zone 3` (Borea / arctic-mountain)
  - `Swamp` → `Zone 4` (Devastated Lands / corrupted)
  - `Ruins` + `Abyss` → `Zone 5` (unnamed / end-game). Merge these two into one zone.
- `src/main/java/com/motm/model/MobStats.java` — Add fields: `String eliteTitle`, `boolean isElite`, getters/setters, copy constructor update
- `src/main/java/com/motm/manager/MobScalingManager.java` — Add `tryMakeElite(MobStats stats, String zone, String mobType)`:
  - Roll against 5% spawn chance (from `elite_titles.json` → `elite_config.spawn_chance`)
  - If elite: pick random style from zone titles, apply HP/damage/XP multipliers from config, set `eliteTitle` and `isElite`
  - Return the modified MobStats
- `src/main/java/com/motm/MenteesMod.java` — Call `tryMakeElite()` in `onMobSpawn()` after scaling, pass the Hytale zone ID

---

## Step 3: Phase 1 Gap — PerkManager StatusEffect Integration

**Problem**: `PerkManager.applyPerkEffects()` (line 151) currently just logs. Many of the 800 perks have effects like `damage_increase`, `damage_reduction`, `on_hit`, `passive` etc. that should connect to StatusEffectManager.

**File**: `src/main/java/com/motm/manager/PerkManager.java`

**Changes**: Replace the TODO log in `applyPerkEffects()` with a switch on `effect.getType()` that:
- `stat_increase` / `stat_multiplier` → Store in PlayerData synergy maps (already exist)
- `damage_increase` / `damage_reduction` → Store in PlayerData synergy maps
- `passive` → Register as permanent StatusEffect via StatusEffectManager
- `on_hit` / `on_kill` → Store as trigger conditions (new `Map<String, Perk.Effect> onHitEffects` transient field on PlayerData)
- `immunity` → Track in a `Set<String> immunities` transient field on PlayerData
- `ability` / `summon` / `transformation` → Log as TODO (Phase 3 features)
- `conditional_buff` / `aura` → Store conditions, apply when met

This requires adding `StatusEffectManager` as a dependency of `PerkManager`.

---

## Step 4: Phase 1 Gap — RaceManager

**Problem**: `RaceData.java`, `races.json`, PlayerData `race` field, and `/motm race` command all exist, but race bonuses are never applied to gameplay. No `RaceManager.java` exists.

**File**: New `src/main/java/com/motm/manager/RaceManager.java`

**Changes**:
- Constructor takes `DataLoader`
- `applyRaceBonuses(PlayerData player, StatusEffectManager sem)`:
  - Read `RaceData` for the player's race
  - Apply `hpBonus` to player's effective max HP
  - Apply permanent effects: evasion (Elf/Halfling/Half-Elf/Dark Elf/Goliath), damage_reduction (Dwarf/Goliath), damage boost (Half-Orc/Orc), cooldown_reduction (Gnome)
  - Track special mechanics: `relentless` (Half-Orc survives lethal once), `breath_weapon` (Dragonborn), `lucky_reroll` (Halfling), `stone_skin` (Dwarf), `blood_rage` (Orc), `hellish_rebuke` (Tiefling)
- Wire into `MenteesMod.java` — call `applyRaceBonuses()` on player join after class is set
- Wire into `MotmCommand.handleClass()` — re-apply race bonuses when class is selected

---

## Step 5: Phase 2 — Mod-Only Consumables

**Problem**: We want RPG depth without duplicating Hytale's native crafting, weapons, armor, and economy. Instead, we add a small set of mod-specific consumables that enhance our ability/perk/elemental systems — things Hytale wouldn't have natively.

**Design philosophy**: Hytale handles weapons, armor, blocks, gathering, and base economy. Our mod adds elemental potions, ability-boosting food, and style-specific consumables that interact with our custom systems.

### Consumable types (~10 recipes):

**Elemental Potions** (crafted from Hytale mob drops):
- Potion of Flame Resistance — +30% fire resist, 60s (useful vs Corruptus players/mobs)
- Potion of Frost Ward — +30% ice resist, 60s
- Potion of Grounding — Immune to knockback, 30s
- Potion of Elemental Surge — +15% elemental reaction damage, 45s

**Ability Boosters** (crafted from Hytale materials):
- Focus Tonic — -25% ability cooldowns, 60s
- Mana Spring — Refill all class resources (water/tp/souls) to max
- Adrenaline Draught — +20% ability damage, 45s

**Style-Specific Food** (from Hytale cooking ingredients):
- Terra Stew — +10% damage reduction, 90s
- Ocean Broth — +15% healing received, 90s
- Wind Salad — +10% evasion, 90s

### New files:

**`src/main/java/com/motm/model/Consumable.java`**:
- Fields: `id`, `name`, `description`, `effectType`, `effectValue`, `durationSeconds`, `materials` (Map<String, Integer> — references Hytale native item IDs)

**`src/main/java/com/motm/manager/ConsumableManager.java`**:
- Constructor takes `DataLoader`, `StatusEffectManager`, `ResourceManager`
- `useConsumable(String playerId, String consumableId)` — apply StatusEffect for duration, deduct item from inventory
- `canUse(String playerId, String consumableId)` — check if player has the item
- Manages a `Map<String, List<Consumable>> playerInventory` for mod-specific items only

**New JSON**:
- `data/consumables/consumables.json` — ~10 consumable definitions with effects and material costs

**PlayerData changes**: Add `Map<String, Integer> modConsumables` (consumableId → quantity)

**MotmCommand changes**: Add `/motm consumables` (list owned), `/motm use <consumableId>` (use one)

**Note**: Material costs reference Hytale's native item IDs (e.g., `hytale:bone`, `hytale:iron_ingot`). The exact IDs will need updating once Hytale's modding API item registry is confirmed. Use placeholder strings with `hytale:` prefix for now.

---

## Step 6: Phase 2 — Enriched Perk Synergy Tags

**Problem**: The 800 existing perks have synergy_tags but the tag vocabulary may not match the expanded categories from the 2D game. SynergyEngine already handles tag matching — this is a pure data update.

**Files**: `data/perks/{terra,hydro,aero,corruptus}_perks.json` (4 files, 200 perks each)

**Changes**: Audit each perk's `synergy_tags` against the expanded vocabulary: `damage`, `defense`, `healing`, `summon`, `dot`, `crowd_control`, `buff`, `debuff`, `shielding`, `aoe`, `lifesteal`, `evasion`, `mobility`. Add missing tags where the perk's effects match.

This is a data-only change — no Java code modifications. The SynergyEngine (`src/main/java/com/motm/manager/SynergyEngine.java`) already reads and matches these tags.

---

## Implementation Order

```
Step 1: Server tick handler                    (1 file modified)
Step 2: Elite title integration                (1 JSON remapped + 3 Java files)
Step 3: PerkManager StatusEffect integration   (2 files modified)
Step 4: RaceManager                            (1 new file + 2 modified)
Step 5: Mod-only consumables                   (2 new files + 1 JSON + 2 modified)
Step 6: Perk synergy tag enrichment            (4 JSON files)
```

Steps 1-4 finish Phase 1. Steps 5-6 are Phase 2.

**All steps are independent** — no dependencies between them. Codex can run all 6 in parallel.

Exception: Step 3 benefits from Step 1 being done first (perk effects need the tick handler to expire), but they don't strictly depend on each other at compile time.

---

## Design Principle: Use Hytale Native Systems

This mod does NOT create custom biomes, items, weapons, armor, resource gathering, or economy systems. Hytale provides all of these natively. Our mod is a pure **RPG overlay** that adds:
- Class/style/ability identity (40 styles, 120 abilities)
- Level progression 1-200 with 800 perks
- Elemental reaction combos between players
- 12 playable races with passives
- Dynamic mob scaling + elite variants
- Mod-specific consumables that enhance the above systems

Elite titles are keyed to Hytale's 5 zones (not custom biomes). Consumable material costs reference Hytale item IDs with `hytale:` prefix placeholders.

---

## Verification

For each step, verify:
1. **Compilation**: `./gradlew build` should succeed (no import errors, no missing methods)
2. **DataLoader**: All new JSON files load without error on startup (check log for `[MOTM] Loaded...` messages)
3. **Commands**: Each new `/motm` subcommand returns formatted output when called
4. **Integration**:
   - Step 1: Tick handler calls all four tick methods each server tick
   - Step 2: Elite mobs spawn with Hytale-zone-specific titles and buffed stats
   - Step 3: Perks apply real StatusEffects, synergy map values, and trigger conditions
   - Step 4: Race bonuses modify effective HP, evasion, damage reduction, and special mechanics
   - Step 5: `/motm use` applies timed StatusEffects from consumables, deducts from inventory
   - Step 6: Perk synergies fire more frequently with enriched tags
