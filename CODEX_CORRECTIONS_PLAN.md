# Codex Corrections Plan — Mentees of the Mystical

> **Generated:** 2026-03-27 by deep review of all 120 abilities cross-referenced against the `executeAbility()` pipeline in `GameplayPlaybackManager.java` (6117 lines).
>
> **Execution order matters.** Complete Phase 1 first — the server will not boot without it. Then Phase 2, then Phase 3 ability corrections, then Phase 4 protection.

---

## Phase 1 — Boot Blockers (DO FIRST — server crashes without these)

### 1.1 Set `IncludesAssetPack` to `false`

**File:** `src/main/resources/manifest.json` line 19

Change:
```json
"IncludesAssetPack": true
```
To:
```json
"IncludesAssetPack": false
```

**Why:** Hytale's AssetModule rejects the mod's asset pack structure and crashes the server with `Failed to load any asset packs`. All RPG systems (classes, styles, abilities, perks, leveling, mob scaling) work without the asset pack. VFX effects degrade gracefully when absent.

### 1.2 Delete stale smoke test jar

**Action:** Delete `mentees_of_the_mystical-1.0.0.jar` from `.tmp/mods-smoke/`

**Why:** The server was loading this old v1.0.0 jar (which has the constructor hook registration bug and no lenient Gson) instead of the fixed v1.0.1 jar.

### 1.3 Fix `server_version` in gradle.properties

**File:** `gradle.properties` line 6

Change:
```
server_version=2026.03.26-89796e57b
```
To:
```
server_version=*
```

**Why:** The hardcoded version `2026.03.26-89796e57b` doesn't match the installed server (`2026.02.19-1a311a592`). The build script (`scripts/build-install.ps1`) already reads the real version from the server jar and passes it via `-Pserver_version=...`, so the hardcoded value is unnecessary and causes mismatch warnings.

### Phase 1 Verification
```
powershell -ExecutionPolicy Bypass -File scripts/build-install.ps1
```
- Server boots without SEVERE errors
- Log shows `[MOTM] Plugin enabled successfully!`
- Log shows `[MOTM] Loaded class: terra`, `hydro`, `aero`, `corruptus`
- Log shows all 40 styles, 120 abilities, 12 races loaded

---

## Phase 2 — Data Loading Hardening

### 2.1 Register lenient `double` TypeAdapter on Gson

**File:** `src/main/java/com/motm/util/DataLoader.java` line 24

Current:
```java
private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
```

Change to:
```java
private static final Gson GSON = new GsonBuilder()
    .setPrettyPrinting()
    .registerTypeAdapter(double.class, new LenientDoubleAdapter())
    .registerTypeAdapter(Double.class, new LenientDoubleAdapter())
    .create();
```

Add inner class:
```java
private static class LenientDoubleAdapter extends com.google.gson.TypeAdapter<Double> {
    @Override
    public void write(com.google.gson.stream.JsonWriter out, Double value) throws java.io.IOException {
        out.value(value);
    }

    @Override
    public Double read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
        if (in.peek() == com.google.gson.stream.JsonToken.STRING) {
            String s = in.nextString().trim().toLowerCase();
            return switch (s) {
                case "infinite", "infinity" -> Double.POSITIVE_INFINITY;
                case "-infinite", "-infinity" -> Double.NEGATIVE_INFINITY;
                case "universal" -> 999999.0;
                default -> {
                    try { yield Double.parseDouble(s); }
                    catch (NumberFormatException e) { yield 0.0; }
                }
            };
        }
        return in.nextDouble();
    }
}
```

**Why:** Perk JSON uses strings like `"infinite"`, `"universal"` in fields Gson deserializes as `double`. The `Perk.Effect` `parseNumberish()` fix only covers `value` and `duration` fields. If any other model class encounters these strings, Gson throws `NumberFormatException` and crashes the plugin.

### 2.2 Wrap `loadAll()` in per-category try/catch

**File:** `src/main/java/com/motm/util/DataLoader.java` — `loadAll()` method at line 45

Current:
```java
public void loadAll() {
    loadClasses();
    loadPerks();
    loadStyles();
    loadReactions();
    loadRaces();
    loadLevelingData();
    loadMobData();
    loadEliteTitles();
    LOG.info("[MOTM] All data files loaded successfully.");
}
```

Change to:
```java
public void loadAll() {
    safeLoad("classes", this::loadClasses);
    safeLoad("perks", this::loadPerks);
    safeLoad("styles", this::loadStyles);
    safeLoad("reactions", this::loadReactions);
    safeLoad("races", this::loadRaces);
    safeLoad("leveling", this::loadLevelingData);
    safeLoad("mobs", this::loadMobData);
    safeLoad("elite titles", this::loadEliteTitles);
    LOG.info("[MOTM] Data loading complete.");
}

private void safeLoad(String category, Runnable loader) {
    try {
        loader.run();
    } catch (Exception e) {
        LOG.severe("[MOTM] Failed to load " + category + ": " + e.getMessage());
        e.printStackTrace();
    }
}
```

**Why:** Currently one bad JSON file crashes the entire plugin. With per-category try/catch, the mod starts with partial data and logs which category failed, so the rest of the systems still work.

### 2.3 Filter unimplemented high-tier perks

**File:** `src/main/java/com/motm/util/DataLoader.java` — after `loadPerks()` completes

After perks are loaded into `perkCache`, add a post-processing step:

```java
private void filterUnimplementedPerks() {
    int removed = 0;
    for (Map.Entry<String, List<Perk>> entry : perkCache.entrySet()) {
        int before = entry.getValue().size();
        entry.getValue().removeIf(perk -> perk.getTier() > 14);
        removed += before - entry.getValue().size();
    }
    if (removed > 0) {
        LOG.info("[MOTM] Filtered " + removed + " unimplemented high-tier perks (tier > 14)");
    }
}
```

Call `filterUnimplementedPerks()` at the end of `loadPerks()`.

**Why:** Perks at tiers 15-20 reference mechanics that don't exist in code: `"ascension"`, `"reality_destruction"`, `"element_creation"`, `"immortal": true`. Loading them causes undefined behavior when the perk system tries to apply their effects.

---

## Phase 3 — Ability Runtime Corrections

These are bugs found by cross-referencing all 120 ability JSON definitions against the `executeAbility()` pipeline in `GameplayPlaybackManager.java`.

### 3.1 `gaze` cast type has NO runtime handler

**Affected abilities:**
- Terra: 1 ability at `data/styles/terra_styles.json` line 468 (effect: "stun", range: 12, duration: 2s)
- Corruptus: 1 ability at `data/styles/corruptus_styles.json` line 304 (effect: "vulnerability+slow", range: 14, duration: 3s)

**Problem:** `gaze` is not in any cast type set (`MOVEMENT_CAST_TYPES`, `LINE_CAST_TYPES`, `PERSISTENT_FIELD_CAST_TYPES`, `AREA_CAST_TYPES`, `CONE_CAST_TYPES`, `MULTI_TARGET_CAST_TYPES`). It falls through to the default single-target resolution in `resolveTargets()` at line 4302, making it act as an instant single-target debuff — identical to a generic untyped ability. The intended gaze fantasy (sustained beam / hold-to-stun) is completely missing.

**Fix — Option A (simpler):** Add `"gaze"` to `CONE_CAST_TYPES` at line 58 so gaze abilities hit multiple enemies in a narrow cone. Set the default cone angle narrow (20-25 degrees) if `coneAngle` is not specified in the JSON:

```java
// Line 58
private static final Set<String> CONE_CAST_TYPES = Set.of("cone", "gaze");
```

And in `createTargetingFrame()`, when computing `coneThreshold`, use a tighter default for gaze:

```java
double coneThreshold = ability.getConeAngle() > 0
    ? Math.cos(Math.toRadians(ability.getConeAngle() / 2.0))
    : "gaze".equals(lower(ability.getCastType()))
        ? Math.cos(Math.toRadians(12.0))  // narrow gaze beam
        : Math.cos(Math.toRadians(35.0)); // standard cone
```

**Fix — Option B (richer):** Create a dedicated gaze handler similar to `startChannelRuntime()` that locks onto a target for the ability's duration, applying the CC effect in pulses. This is more faithful to the "sustained stare" fantasy but requires more code.

**Recommendation:** Start with Option A. It's functional and can be upgraded to Option B later.

### 3.2 `curse` cast type has no dedicated behavior

**Affected abilities:**
- `hex` at `data/styles/corruptus_styles.json` line 132 (effect: "vulnerability", range: 16, duration: 6s)
- `phantasmal_chains` at `data/styles/corruptus_styles.json` line 276 (effect: "vulnerability+slow", range: 16, duration: 6s)

**Problem:** `curse` falls through to default single-target resolution which requires `forwardDot > 0.2` (must be looking roughly at target). Curses should be castable on enemies you're aware of, not just ones you're directly facing.

**Fix:** In `resolveTargets()` (line 4214), add a special case before the default fallback:

```java
// After the LINE_CAST_TYPES check (line 4300), before the default:
if ("curse".equals(castType)) {
    // Curses use relaxed facing — can target enemies you're not directly looking at
    TargetCandidate nearest = frame.candidates().stream()
        .filter(c -> c.forwardDot() > -0.5 && c.distance() <= frame.range())
        .min((a, b) -> Double.compare(a.distance(), b.distance()))
        .orElse(null);
    return nearest != null ? List.of(nearest.ref()) : List.of();
}
```

**Why:** A curse is a magical debuff — the caster should be able to curse enemies in a wide arc around them, not just ones in their direct line of sight.

### 3.3 Remove double speed-stacking for 4 hardcoded abilities

**File:** `src/main/java/com/motm/manager/GameplayPlaybackManager.java` — `applySpecificCastRuntime()` at line 608

**Problem:** `applySpecificCastRuntime()` (line 616) applies a speed buff to `high_tide`, `river_rapids`, `frolick`, `refraction`. But these abilities also have "speed" in their effect tokens, which means `applyCasterRuntime()` (line 2329-2346) ALSO applies a speed buff via `CASTER_EFFECT_TOKENS`. Result: double speed stacking.

**Fix:** Remove the hardcoded speed application from `applySpecificCastRuntime()`. The `CASTER_EFFECT_TOKENS` set already includes "speed" (line 62), and `applyCasterRuntime()` already handles it for ALL self_buff abilities. Delete lines 616-619:

```java
// DELETE THIS BLOCK (lines 616-619):
if (Set.of("high_tide", "river_rapids", "frolick", "refraction").contains(abilityId)
        && applyOwnerStatusToken("speed", player, ability)) {
    granted.add("speed");
}
```

If `applySpecificCastRuntime()` becomes empty after this, have it return `AbilitySpecificRuntimeResult.none()` unconditionally, or remove the method and its call in `executeAbility()`.

### 3.4 Tune `execute` cast type damage (optional — tuning decision)

**Affected abilities:**
- `combust` at `data/styles/corruptus_styles.json` line 66 (damage_percent: 130)
- `consume` at `data/styles/corruptus_styles.json` line 547 (damage_percent: 160)

**Problem:** The `execute` multiplier at line 4912 is 1.3x. Combined with already-high base damage:
- `combust`: 130 * 1.3 = 169% effective base
- `consume`: 160 * 1.3 = 208% effective base
- At level 200: multiplied by ~12.9 → 2180% and 2683% damage

This may be intentional for "execute" (finisher) abilities, but could one-shot most mobs.

**Fix (if tuning down):** Reduce `damage_percent` in the JSON:
- `combust`: 130 → 100
- `consume`: 160 → 120

OR reduce the execute multiplier:
- Line 4912: `case "execute" -> damage * 1.3;` → `case "execute" -> damage * 1.15;`

**Note:** This is a design/balance decision. If executes are meant to be high-damage finishers on low-HP targets, the current values might be fine if the execute condition (target below X% HP) is strict enough. Verify whether `applyCombat()` checks target HP threshold for execute cast type.

### 3.5 Verify projectile abilities apply debuff effects on collision

**File:** `src/main/java/com/motm/manager/GameplayPlaybackManager.java`

**Problem:** In `executeAbility()` (lines 154-162), when `projectileLaunch.launched() > 0`, the pipeline skips `applyCombat()`, `applyLifesteal()`, AND `applyTargetEffects()`. This is correct for damage (projectiles deal damage on collision), but debuff effects like "burn", "slow", "stun" from the ability's `effect` field must be applied in the projectile collision handler.

**Action:** Find `processProjectileCollision()` (or equivalent collision handler in the projectile tick loop). Verify it calls something equivalent to `applyTargetEffects()` for the projectile's associated `AbilityData`. If it only deals damage without applying effects, add effect application:

```java
// In the projectile collision handler, after dealing damage:
for (String token : parseEffectTokens(projectile.ability().getEffect())) {
    if (TARGET_EFFECT_TOKENS.contains(token)) {
        applyTokenToTarget(token, targetRef, store, projectile.ownerRef(), projectile.ownerPlayerId(), projectile.ability());
    }
}
```

**Why:** Many projectile abilities define effects (burn, slow, etc.) that should apply on hit. Without this, those effects are silently dropped.

### 3.6 Fix transformation aborting when VFX effect ID is null

**File:** `src/main/java/com/motm/manager/GameplayPlaybackManager.java` — `applyTransformation()` at line 2540

**Problem:** `resolveTransformationEffectId()` (line 4511) only maps 4 ability IDs to VFX effects: `smoke_form`, `pterodactyl_form`, `triceratops_form`, `t_rex_form`. For any other transformation ability, it returns `null`. Then at line 2555:

```java
if (effectId == null || !applyEffectById(playerRef, store, effectId)) {
    return FormRuntimeResult.none();  // SILENTLY ABORTS the transformation
}
```

This means any new transformation ability silently fails — no VFX AND no combat bonuses.

**Fix:** Allow transformations to proceed without VFX:

```java
// Line 2554-2557, change:
String effectId = resolveTransformationEffectId(ability.getId());
if (effectId != null) {
    applyEffectById(playerRef, store, effectId);  // Apply VFX if available, but don't abort if absent
}
```

**Why:** The transformation combat bonuses from `createTransformationState()` (damage bonus, weapon bonus, speed multiplier, reach, CC type) are valuable gameplay mechanics independent of visual effects.

### 3.7 Allow `dash_buff` to arm weapon follow-up

**File:** `src/main/java/com/motm/manager/GameplayPlaybackManager.java` — `shouldArmWeaponFollowUp()` at line 3903

**Problem:** Only `self_buff` cast type can arm weapon follow-up. `dash_buff` abilities (dash + gain a buff) should also arm weapon follow-up since the buff represents empowered attacks.

**Fix:**
```java
private boolean shouldArmWeaponFollowUp(AbilityData ability) {
    String castType = lower(ability.getCastType());
    if (!"self_buff".equals(castType) && !"dash_buff".equals(castType)) {
        return false;
    }
    // ... rest of method unchanged
}
```

**Why:** `dash_buff` is conceptually "dash into position, then attack with empowered strikes." The `resolveFollowUpUses()` method (line 3915) already has entries for movement abilities like `battle_cry`, `waverider`, `frolick` — these abilities are `self_buff` but are thematically similar to `dash_buff`.

---

## Phase 4 — Stability and Protection

### 4.1 Create `CLAUDE.md` at project root

**File:** `CLAUDE.md` (new file)

```markdown
# Mentees of the Mystical — Project Rules

## Data Protection
- `src/main/resources/data/styles/*.json` are PROTECTED. These contain carefully authored ability profiles. NEVER regenerate these files wholesale. Only make surgical, targeted edits to specific fields.
- If you need to modify ability data, change only the specific field(s) requested. Do not reformat, reorder, or rewrite surrounding content.
- 3 "Restore" commits in git history show ability data was previously lost and had to be manually recovered.

## Design Principles
- This is a PURE RPG OVERLAY mod. It adds class/style/ability identity, perk progression, and mob scaling on top of Hytale's native systems.
- NEVER create custom biomes, weapons, armor, or economy systems — Hytale provides all of those natively.
- Styles = the ONLY source of active abilities (3 per style)
- Perks = ALWAYS passive bonuses (never active abilities)
- Races = passive identity bonuses

## Plugin Lifecycle
- `setup()` = data loading (JSON → model objects). Runs before hooks are available.
- `start()` = hook registration (Hytale event listeners). Runs after the server is ready.
- NEVER register hooks in the constructor or in `setup()`.

## Build
- Build: `powershell -ExecutionPolicy Bypass -File scripts/build-install.ps1`
- JDK 25 + Gradle 9.1 (auto-downloaded to `.tools/`)
- Internal build installs to `%APPDATA%/Hytale/UserData/Mods/`

## Known Issues
- `MenteesMod.java` (2361 lines) and `GameplayPlaybackManager.java` (6117 lines) are god classes. Extraction direction is documented in the corrections plan.
- `IncludesAssetPack` must stay `false` until the correct asset pack structure is investigated.
```

### 4.2 Add ability completeness checks to preflight audit

**File:** `src/main/java/com/motm/util/MotmPreflightAudit.java`

Add validation that each loaded ability has:
- Non-empty `cast_type`
- At least one spatial field (`range`, `radius`, `width`, `length`, or `cone_angle`) OR is a self-targeting type (`self_buff`, `self_burst`, `transformation`, `cleanse`)
- Non-empty `name`

Log warnings for any ability that fails these checks. This catches "stripped" abilities where data was accidentally overwritten.

### 4.3 Add perk effect type validation to preflight audit

**File:** `src/main/java/com/motm/util/MotmPreflightAudit.java`

Whitelist the implemented perk effect types based on what the code actually processes. Flag unrecognized types as warnings:
- Implemented: damage_bonus, damage_reduction, health_bonus, speed_bonus, cooldown_reduction, resource_regen, lifesteal, critical_chance, critical_damage, element_resistance, etc.
- Unimplemented (warn): ascension, reality_destruction, element_creation, immortal, etc.

---

## Phase 5 — Structural Refactoring (incremental, one extraction per session)

Each extraction is a pure refactor — no behavior changes — done as its own commit.

### 5.1 Extract from MenteesMod.java (2361 lines)
- `HydroContainerHandler`: all `HYDRO_CONTAINER_*` constants and methods (~150 lines)
- `TerraResourceHandler`: all `TERRA_*` constants and methods (~150 lines)

### 5.2 Extract from GameplayPlaybackManager.java (6117 lines)
- `ProjectileSystem`: `ActiveProjectile` record, `launchProjectiles()`, projectile tick/collision
- `FieldSystem`: `ActiveField` record, `activatePersistentField()`, `registerFieldRuntime()`, field tick/pulse/pull
- `ChannelSystem`: `ActiveChannel` record, `startChannelRuntime()`, channel tick
- `TransformationSystem`: `ActiveTransformation` record, `applyTransformation()`, `createTransformationState()`, transformation tick

### 5.3 Split MotmCommand.java (1346 lines)
- Each top-level subcommand (`class`, `style`, `perks`, `dev`, `spellbook`) becomes its own handler class

---

## Critical Files Reference

| File | Lines | Changes |
|------|-------|---------|
| `src/main/resources/manifest.json` | 20 | `IncludesAssetPack: false` |
| `gradle.properties` | 6 | `server_version=*` |
| `.tmp/mods-smoke/` | — | Delete stale 1.0.0 jar |
| `src/main/java/com/motm/util/DataLoader.java` | 433 | Lenient Gson adapter, try/catch wrapper, tier filter |
| `src/main/java/com/motm/manager/GameplayPlaybackManager.java` | 6117 | gaze handler (3.1), curse targeting (3.2), remove speed double-stack (3.3), transformation null-effectId fix (3.6), dash_buff follow-up (3.7), verify projectile debuffs (3.5) |
| `src/main/java/com/motm/util/MotmPreflightAudit.java` | 682 | Ability completeness + perk effect validation |
| `src/main/resources/data/styles/corruptus_styles.json` | ~400 | Optional: tune execute damage_percent (3.4) |
| `CLAUDE.md` | New | Data protection rules |
| `src/main/java/com/motm/MenteesMod.java` | 2361 | Phase 5 extraction target |
| `src/main/java/com/motm/command/MotmCommand.java` | 1346 | Phase 5 extraction target |

## Execution Summary

| Priority | Items | Effort |
|----------|-------|--------|
| **IMMEDIATE** | Phase 1 (boot blockers: 1.1, 1.2, 1.3) | 5 min |
| **HIGH** | Phase 2 (data hardening: 2.1, 2.2, 2.3) | 30 min |
| **HIGH** | Phase 3 (ability corrections: 3.1-3.7) | 2-3 hrs |
| **MEDIUM** | Phase 4 (protection: 4.1, 4.2, 4.3) | 1 hr |
| **LOW** | Phase 5 (refactoring: 5.1, 5.2, 5.3) | 1 per session |
