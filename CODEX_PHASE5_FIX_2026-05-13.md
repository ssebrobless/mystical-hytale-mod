# Codex — Phase 5 Fix Addendum (2026-05-13)

> **Read this together with `CODEX_IMPLEMENTATION_PLAN_2026-05-13.md`.** Phases 2, 3 are confirmed passing.
> Phase 5 (Quake vertical slice) **failed on behavior, not plumbing.** This document replaces the Phase 5 section of the parent plan and is the only thing you implement until it passes twice.
> **Surgical edits only to `data/styles/terra_styles.json`. See CLAUDE.md.**

---

## 0. Why the previous Phase 5 attempt failed

The cast pipeline works. Logs prove it:
- `[MOTM] Custom spellbook interaction fired:` fires for all three slots.
- `[MOTM] Queue ability cast: ... abilityId=stomp/aftershock/sinkhole` fires.
- No `Block_Secondary` / `Root_Unarmed` binding remains; no MOTM-side idle HP loss.

The slice still fails because **the visible/behavioral identity of each ability does not match the design intent**. Specifically:
1. **Stomp** is currently an *immediate* AoE around the caster (per its JSON `ground_burst` + `self_centered`). The intended design is **press → arm → next jump → land → AoE at landing point**.
2. **Aftershock** is a generic ground-zone persistent field. Intended: visible **ring of ground-cracks** centered on caster, affecting **ground targets only**.
3. **Sinkhole** logs target damage/stun and applies `root`, but is invisible. Intended: targeted enemy is **dragged ~2-3 blocks below ground** for the duration (2.5s) with a suffocation DoT, then **raised back** to the surface.

User-confirmed design (2026-05-13):
| Slot | Ability | Design |
| --- | --- | --- |
| 1 LMB | Stomp | Press → armed state. On next jump-land, AoE fires from landing point (radius 4, knockback 4.5, damages **ground targets only**). Visible large ground cracks in the radius. |
| 2 RMB | Aftershock | Cast at caster's feet, ground_zone radius 5, duration 4s. Slows + disorients **ground targets only**. Visible: ring of ground cracks on the floor. |
| 3 Use | Sinkhole | Targeted enemy within 10m. After 0.6s delay, target is dragged ~2.5 blocks below ground, held 2.5s (root + DoT), then raised back to surface. Visible pit + dust. |

---

## 1. How styles / abilities / classes are supposed to interact (the contract Phase 5 enforces)

Before touching code, internalize this:

### 1.1 Class → Style → Ability → Slot
- A player picks **one class** (`terra` / `hydro` / `aero` / `corruptus`) — sets passives + resource type.
- A player picks **one style per class** (10 per class, `/motm style <id>`). The style defines the **active identity**: theme, resource_type, and exactly **3 abilities**.
- The 3 abilities are bound by **declaration order** to **slot 1 / 2 / 3** = LMB / RMB / Use (see `MotmCommand.castAbilityBySlot`).
- Perks are **always passive**, races are **passive identity bonuses**. They never enter the slot pipeline.

### 1.2 Input → cast pipeline (post-Phase 2, confirmed working)
```
LMB/RMB/Use on Mentees Spellbook
  → MotmSpellbookInteraction.{Primary,Secondary,Use}.firstRun()
  → MenteesMod.castSpellbookSlotFromInteraction(player, slot)
  → MenteesMod.tryCastSpellbookSlot(...)                         // cooldown/resource/charge checks
  → enqueue PendingAbilityCast
  → processPendingAbilityCasts() on next server tick
  → GameplayPlaybackManager.executeAbility(player, style, ability, context)
```

### 1.3 `executeAbility` is a **parallel resolution pipeline** of 13 sub-systems (see `GameplayPlaybackManager.java:138`). Each ability's "identity" is the *combination* of which sub-systems fire and how:
- `playAbility` — cast visuals (vanilla + MOTM `EntityEffect` IDs from `HytaleAssetResolver`) + movement (`MOVEMENT_CAST_TYPES`).
- `launchProjectiles` — for `LINE_CAST_TYPES`.
- `activatePersistentField` — for `PERSISTENT_FIELD_CAST_TYPES` (`ground_zone`, `support_zone`, `barrier`) **and** for `ground_target` when terrain_effect or ability_id matches sinkhole/hazard (see `isPersistentFieldAbility`, line ~4883).
- `applyCombat` — resolves targets via `resolveTargets(playerRef, store, ability, context)` using cast_type to determine shape; applies damage.
- `applyTargetEffects` — applies status tokens from `ability.effect` (e.g. `"slow+disoriented"` → SLOW + DISORIENTED via `applyTargetToken`).
- `applyFieldTerrainEffects` (called from active fields each tick, line ~1594) — maps `terrain_effect` strings to repeated `applyTargetToken` calls on entities inside the field. **This is where `sinkhole` currently applies `root` (line 1603).**
- `applyCasterRuntime` — caster-side buffs (`CASTER_EFFECT_TOKENS`).
- `applyTransformation`, `handleSummonRuntime`, `startChannelRuntime`, `startLineControlRuntime`, `armWeaponFollowUp` — out-of-scope for Quake.

### 1.4 Where each Quake ability "lives" in that pipeline today
| Ability | Sub-systems that fire | What's missing |
| --- | --- | --- |
| Stomp (`ground_burst`) | `playAbility` (FX_EARTH_CAST) + `applyCombat` (radius 4 around caster) + `applyTargetEffects` (knockback) | Immediate; no arming, no land detection. No ground-targets-only filter. No visible cracks. |
| Aftershock (`ground_zone`) | `playAbility` + `activatePersistentField` (radius 5, duration 4s) + `applyFieldTerrainEffects` ignores `lingering_tremor` (no match in switch) + `applyTargetEffects` (slow+disoriented at cast time only) | Status effects only land on initial targets at cast; targets entering the field after t=0 get no slow because `lingering_tremor` isn't in the terrain switch. No ground-targets-only filter. No visible cracks. |
| Sinkhole (`ground_target`) | `playAbility` + `activatePersistentField` (delay 0.6s, duration 2.5s) + `applyFieldTerrainEffects` applies `root` per target | No vertical drag of target. No surface raise. No DoT. No visible pit. |

### 1.5 The two missing primitives Phase 5 needs
**Primitive A — Ground-targets-only filter.** A function that takes a target entity ref and returns whether it is currently on the ground. Filter `resolveTargets` output and `applyFieldTerrainEffects` per-tick checks by this when the ability's data declares `"ground_targets_only": true`.

**Primitive B — Per-ability special handlers** for the two distinctive behaviors:
- B1: `stomp` — arm-and-jump-land
- B2: `sinkhole` — vertical drag of target + raise on expiry

These are per-ability code paths keyed on `ability.getId()`. We accept the small amount of hard-coding because they are visually-signature behaviors that the generic pipeline cannot express. Later styles that want similar mechanics can either re-declare the same ability_id keying or we add cast_type tokens like `"land_burst"` and `"vertical_displace"` once we have a second user.

---

## 2. Implementation plan — what Codex does next, in order

> **Do not start any step until the previous step's acceptance gate passes.** Compile after every step. No half-finished implementations: each step ends with the project building.

### Step 2.1 — Spec the design in data (surgical JSON edits)

**File:** `src/main/resources/data/styles/terra_styles.json`

**Edit only the three Quake ability objects (lines 11-31, 33-50, 52-72).** Add the following fields. **Do not reorder, do not regenerate, do not change any other field's value.**

For **all three** abilities (stomp, aftershock, sinkhole) — add:
```json
"ground_targets_only": true,
"visual_overlay": "ground_cracks"
```

For **stomp specifically** — also add:
```json
"trigger": "jump_land"
```
(Do **not** change cast_type, target_type, radius, knockback_force, charges, or cooldown.)

For **sinkhole specifically** — also add:
```json
"vertical_displace_blocks": 2.5,
"dot_percent_per_second": 6
```
(Do **not** change range, radius, delay_seconds, duration_seconds, damage_percent.)

**Acceptance gate:** `./gradlew build` succeeds. Server start log shows `[MOTM] Loaded ... styles` with no JSON parse error. `/motm abilities` still lists Quake's three abilities with their unchanged numbers.

---

### Step 2.2 — Extend the model classes to read the new fields

**File:** `src/main/java/com/motm/model/AbilityData.java`

Add four fields with getters (no setters needed if the loader uses constructor or reflective JSON binding — match the existing pattern in that file):
- `private boolean groundTargetsOnly;` → `isGroundTargetsOnly()`
- `private String visualOverlay;` → `getVisualOverlay()`
- `private String trigger;` → `getTrigger()`
- `private double verticalDisplaceBlocks;` → `getVerticalDisplaceBlocks()`
- `private double dotPercentPerSecond;` → `getDotPercentPerSecond()`

If `AbilityData` uses Gson with `@SerializedName` annotations, add them with the snake_case names (`ground_targets_only`, `visual_overlay`, `trigger`, `vertical_displace_blocks`, `dot_percent_per_second`). Otherwise just match the existing pattern in that file.

**Acceptance gate:** `./gradlew build` succeeds. Log line on server start: `[MOTM] Loaded 40 styles, 120 abilities` (whatever number it currently reports must remain unchanged).

---

### Step 2.3 — Ground-targets-only filter (Primitive A)

**File:** `src/main/java/com/motm/manager/GameplayPlaybackManager.java`

Add a private helper method near the other entity utility methods:

```java
/**
 * Returns true if the target is currently considered "on the ground" — i.e. its feet
 * are within a small tolerance of a solid block. Used by abilities whose data declares
 * ground_targets_only=true (Quake's three abilities at minimum).
 *
 * Implementation note: probe HytaleServer.jar for whichever of the following exists:
 *   - a PhysicsBodyComponent / VelocityComponent with a y-velocity accessor
 *   - a flag on TransformComponent or NPCEntity indicating onGround
 *   - a util in com.hypixel.hytale.server.core.universe.world to test block solidity below a position
 * Use the first available. If none exists, fall back to: vertical position fractional part < 0.15
 * (treats targets standing on a block-aligned y as grounded). Log a one-time WARN if falling back.
 */
private boolean isTargetGrounded(Ref<EntityStore> targetRef, Store<EntityStore> store) {
    if (targetRef == null || !targetRef.isValid()) return false;
    TransformComponent transform = store.getComponent(targetRef, TransformComponent.getComponentType());
    if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
        return true; // default-permit if we cannot tell
    }
    // TODO Codex: probe API for a real onGround check (see method javadoc). Until then, fractional-Y heuristic.
    double y = transform.getTransform().getPosition().y;
    return Math.abs(y - Math.floor(y)) < 0.15;
}
```

**Then plumb it into the two target-resolution sites:**

1. `resolveTargets(playerRef, store, ability, context)` — at the end, before `return targets;`, add:
   ```java
   if (ability.isGroundTargetsOnly()) {
       targets.removeIf(ref -> !isTargetGrounded(ref, store));
   }
   ```

2. `processFieldTick(field, now)` — wherever it iterates entities inside a field to call `applyFieldTerrainEffects`/`applyTargetToken`, skip targets that fail `isTargetGrounded` when `field.ability().isGroundTargetsOnly()` is true. Find the call site (around `applyFieldTerrainEffects(field, ...)`) and gate it.

**Acceptance gate:** Build succeeds. Manual smoke test: a player jumping while a Quake AoE fires from another caster does not take Stomp damage or get Aftershock-slowed. (Skip this run-time check until Step 2.4 lands the jump-land trigger; for now just confirm the code compiles and existing slice still runs.)

---

### Step 2.4 — Stomp jump-land arming (Primitive B1)

**Files:**
- `src/main/java/com/motm/manager/GameplayPlaybackManager.java`
- `src/main/java/com/motm/MenteesMod.java`

**2.4.1 — Define armed state**

In `GameplayPlaybackManager`, add at top of class:
```java
private record ArmedStomp(String playerId, PlayerData player, StyleData style, AbilityData ability,
                          long armedAtMillis, long expireAtMillis,
                          double previousY, boolean wasAirborne) {}

private final Map<String, ArmedStomp> armedStompByPlayer = new ConcurrentHashMap<>();
private static final long STOMP_ARM_TIMEOUT_MILLIS = 30_000L;
private static final double STOMP_JUMP_THRESHOLD_BLOCKS = 0.45;
private static final double STOMP_LAND_TOLERANCE_BLOCKS = 0.10;
```

**2.4.2 — Branch `executeAbility` to arm instead of fire when `trigger=jump_land`**

At the very top of `executeAbility` (after the null checks at line 143-145), add:
```java
if ("jump_land".equalsIgnoreCase(ability.getTrigger())) {
    return armJumpLandAbility(runtimePlayer, player, style, ability);
}
```

Add the method:
```java
private ExecutionResult armJumpLandAbility(Player runtimePlayer, PlayerData player,
                                           StyleData style, AbilityData ability) {
    var playerRef = runtimePlayer.getReference();
    if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
        return ExecutionResult.none("Cannot arm: player reference invalid.");
    }
    TransformComponent transform = playerRef.getStore()
            .getComponent(playerRef, TransformComponent.getComponentType());
    if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
        return ExecutionResult.none("Cannot arm: player position unavailable.");
    }
    long now = System.currentTimeMillis();
    armedStompByPlayer.put(player.getPlayerId(), new ArmedStomp(
            player.getPlayerId(), player, style, ability,
            now, now + STOMP_ARM_TIMEOUT_MILLIS,
            transform.getTransform().getPosition().y,
            false
    ));
    LOG.info("[MOTM] Stomp armed: player=" + player.getPlayerName()
            + " — next jump's landing will trigger the shockwave");
    // Cast visuals at the caster's feet now so the player gets feedback that the arm landed.
    String effectId = resolveEffectId(player.getPlayerClass(), currentStyleId(player), ability);
    applyEffectById(playerRef, playerRef.getStore(), effectId);
    return new ExecutionResult(
            PlaybackResult.none("Stomp armed — jump and land to release."),
            0, 0.0, 0, 0, false,
            "Stomp armed (jump → land to release the shockwave)");
}
```

**2.4.3 — Tick: detect jump → land**

Add a method to `GameplayPlaybackManager`:
```java
public synchronized void tickArmedStomps(Store<EntityStore> currentStore) {
    if (armedStompByPlayer.isEmpty()) return;
    long now = System.currentTimeMillis();
    armedStompByPlayer.entrySet().removeIf(entry -> {
        ArmedStomp armed = entry.getValue();
        if (now >= armed.expireAtMillis()) {
            LOG.info("[MOTM] Stomp arm expired: player=" + armed.player().getPlayerName());
            return true;
        }
        Player runtimePlayer = mod.getRuntimePlayer(armed.playerId());
        if (runtimePlayer == null) return false;
        var playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() != currentStore) return false;
        TransformComponent transform = currentStore.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
            return false;
        }
        double y = transform.getTransform().getPosition().y;
        double dy = y - armed.previousY();
        boolean nowAirborne = armed.wasAirborne() || dy > STOMP_JUMP_THRESHOLD_BLOCKS / 20.0;
        boolean landed = armed.wasAirborne()
                && Math.abs(y - Math.floor(y)) < STOMP_LAND_TOLERANCE_BLOCKS
                && dy <= 0.0;
        if (landed) {
            fireArmedStomp(runtimePlayer, armed, transform.getTransform().getPosition());
            return true;
        }
        entry.setValue(new ArmedStomp(
                armed.playerId(), armed.player(), armed.style(), armed.ability(),
                armed.armedAtMillis(), armed.expireAtMillis(),
                y, nowAirborne
        ));
        return false;
    });
}

private void fireArmedStomp(Player runtimePlayer, ArmedStomp armed, Vector3d landingPos) {
    LOG.info("[MOTM] Stomp fired at landing: player=" + armed.player().getPlayerName()
            + " pos=" + landingPos);
    // Build a CastContext targeted at the landing position so applyCombat resolves against the ring.
    CastContext landingCtx = CastContext.atPosition(landingPos);   // add a static factory if it doesn't exist
    AbilityData liveAbility = armed.ability();
    PlaybackResult playback = playAbility(runtimePlayer, armed.player(), armed.style(), liveAbility);
    CombatResolution combat = applyCombat(runtimePlayer, armed.player(), liveAbility, landingCtx);
    EffectResolution effects = applyTargetEffects(runtimePlayer, armed.player(), liveAbility, landingCtx);
    // Reuse the ground-cracks visual overlay routing from Step 2.6.
}
```

**2.4.4 — Wire the tick**

In `MenteesMod.onServerTick(Store<EntityStore> currentStore)` (line 719), add **before** `gameplayPlaybackManager.tick(currentStore);`:
```java
gameplayPlaybackManager.tickArmedStomps(currentStore);
```

**2.4.5 — Lifecycle cleanup**

- On `onPlayerDisconnect`/style change/death: call `gameplayPlaybackManager.clearArmedStomp(playerId)` (add this one-liner method that does `armedStompByPlayer.remove(playerId)`). Find the existing cleanup sites that clear `pendingAbilityCasts` and add the same call there.

**Decision table — Phase 5 forks (Stomp):**

| Symptom | Action |
| --- | --- |
| `CastContext.atPosition(...)` doesn't exist | Inspect `CastContext` class — there's likely an overload constructor or a `Builder`. Use it. |
| Land trigger never fires (player jumps but no AoE) | Add a per-tick `LOG.fine` of `y`, `dy`, `wasAirborne`. The threshold may be wrong for Hytale's tick rate (20Hz vs 60Hz). Tune `STOMP_JUMP_THRESHOLD_BLOCKS`. |
| Land trigger fires immediately on cast | `armed.previousY()` is being seeded with a fractional value. Force `wasAirborne=false` on arm and require a positive `dy` sample first. |
| `EntityVelocity`/physics API exists in Hytale | Replace the y-delta heuristic with the velocity component. Document the swap in a code comment. |

**Acceptance gate:**
- Build succeeds.
- `/motm class terra` + `/motm style quake` + equip spellbook + LMB once.
- Log: `[MOTM] Stomp armed: player=<name>` (no damage yet, no log line for queue ability cast triggering damage).
- Jump (spacebar). On landing, log: `[MOTM] Stomp fired at landing: player=<name> pos=<x,y,z>`. AoE damage + knockback applied to nearby ground targets. Cracks visual present (Step 2.6 will polish this).
- A jumping enemy in the radius takes **no damage** (ground-targets-only filter from Step 2.3 works).

---

### Step 2.5 — Sinkhole vertical drag + raise (Primitive B2)

**File:** `src/main/java/com/motm/manager/GameplayPlaybackManager.java`

**2.5.1 — Extend `ActiveField` (or add a sibling structure)**

The existing `activatePersistentField` already creates an `ActiveField` for Sinkhole because `isPersistentFieldAbility` returns true for `ground_target` with terrain_effect contains `sinkhole`. Add tracking for displaced targets:

```java
private record SinkholeVictim(Ref<EntityStore> targetRef, double originalY, long expireAtMillis) {}

private final Map<String, List<SinkholeVictim>> sinkholeVictimsByField = new HashMap<>();
```

(Field key = `field.ownerPlayerId() + ":" + field.ability().getId() + ":" + field.activateAtMillis()` or similar deterministic key. Or — cleaner — add a `Map<UUID, SinkholeVictim>` directly inside `ActiveField` if you can extend the record without too much surgery; otherwise keep the external map.)

**2.5.2 — On field activation (after the field's delay elapses), capture targets and lower them**

Inside `processFieldTick`, when the field first transitions from "armed" to "active" (i.e., `now >= field.activateAtMillis()` and `!field.activated()`), and when the ability is sinkhole (`"sinkhole".equalsIgnoreCase(field.ability().getId())`), do:

```java
// Find all enemy NPC targets inside the field radius at the moment the sinkhole arms.
List<Ref<EntityStore>> caught = findEnemiesInRadius(field.center(), field.ability().getRadius(),
        field.ownerRef(), currentStore);
double displace = field.ability().getVerticalDisplaceBlocks() > 0
        ? field.ability().getVerticalDisplaceBlocks() : 2.5;
long expire = field.expireAtMillis();
List<SinkholeVictim> victims = new ArrayList<>();
for (Ref<EntityStore> targetRef : caught) {
    Vector3d pos = getPosition(targetRef, currentStore);
    if (pos == null) continue;
    if (!moveEntityTo(targetRef, currentStore, pos.x, pos.y - displace, pos.z)) {
        LOG.warning("[MOTM] Sinkhole: could not lower target — skipping drag for this entity");
        continue;
    }
    applyTargetToken("root", targetRef, currentStore, field.ownerRef(),
            field.ownerPlayerId(), field.ability());
    victims.add(new SinkholeVictim(targetRef, pos.y, expire));
}
sinkholeVictimsByField.put(fieldKey(field), victims);
LOG.info("[MOTM] Sinkhole engaged: dragged " + victims.size() + " target(s) at center=" + field.center());
```

**2.5.3 — Per-tick: keep them held down + DoT**

Inside `processFieldTick` after the active branch, for each sinkhole victim:
```java
double dotPercent = field.ability().getDotPercentPerSecond() > 0
        ? field.ability().getDotPercentPerSecond() : 0.0;
if (dotPercent > 0) {
    // 20 ticks/sec assumed; if FIELD_PULSE_INTERVAL_MS is different, prorate accordingly.
    applyDamagePercent(victim.targetRef(), currentStore, dotPercent / 20.0, /*ability=*/field.ability());
}
// Reapply position lock (in case target's AI tried to climb out)
Vector3d current = getPosition(victim.targetRef(), currentStore);
if (current != null && current.y > victim.originalY() - (displace * 0.5)) {
    moveEntityTo(victim.targetRef(), currentStore, current.x, victim.originalY() - displace, current.z);
}
```

**2.5.4 — On field expiry: raise targets back**

When `processFieldTick` returns true (field expiring) for a sinkhole, before despawning visuals:
```java
List<SinkholeVictim> victims = sinkholeVictimsByField.remove(fieldKey(field));
if (victims != null) {
    for (SinkholeVictim v : victims) {
        if (!v.targetRef().isValid()) continue;
        Vector3d pos = getPosition(v.targetRef(), currentStore);
        if (pos == null) continue;
        moveEntityTo(v.targetRef(), currentStore, pos.x, v.originalY(), pos.z);
    }
    LOG.info("[MOTM] Sinkhole released: raised " + victims.size() + " target(s)");
}
```

**2.5.5 — Discover the entity move API**

`moveEntityTo` is the unknown. **Codex must probe `HytaleServer.jar`** before assuming a signature. Run:
```powershell
& "$env:JAVA_HOME\bin\jar.exe" tf "$env:APPDATA\Hytale\install\release\package\game\latest\Server\HytaleServer.jar" | Select-String -Pattern "NPCEntity|TransformComponent|moveTo|setPosition"
```
Then `javap` the relevant classes. Likely candidates, in order of probability:
1. `NPCEntity.moveTo(Ref<EntityStore>, double x, double y, double z, Store<EntityStore>)` (mirrors `Player.moveTo`).
2. `TransformComponent.setPosition(Vector3d)` — direct ECS write; may require a `CommandBuffer.update(...)` rather than direct mutation.
3. `World.teleportEntity(Ref<EntityStore>, Vector3d)`.

If none of those work, vertical drag is **infeasible without an API change** — in which case: fall back to **visible particle pit + heavy root + DoT** (the option-B fallback from the design discussion) and log a TODO. Do **not** delete the drag scaffolding; keep it gated on the API check.

**Decision table — Phase 5 forks (Sinkhole):**

| Symptom | Action |
| --- | --- |
| No entity move API found | Fallback to visual-only: keep root + DoT, render a ring of stone-dust particles + a downward-pointing dust column at target. Document this in a code comment as "stub until move API confirmed." |
| Target is moved down but immediately climbs back via AI | The per-tick reapply loop in 2.5.3 should handle it. If still climbing, increase reapply frequency or apply a temporary AI-pause / freeze effect alongside the move. |
| Target is moved down but physics throws them up next frame | Try `moveEntityTo` to `pos.y - displace - 0.1` so they remain "buried" inside a block. If physics still resists, document the limitation; visual-only fallback. |
| DoT damage applied too fast (target dies in 1s) | The `dotPercentPerSecond / 20.0` math assumes 20Hz ticks. Inspect `FIELD_PULSE_INTERVAL_MS` (currently 900ms) and recompute. |

**Acceptance gate:**
- Build succeeds.
- Aim at a test mob 6-10m away, press Use. Within ~0.6s, the mob visibly descends ~2-3 blocks below the surface. DoT damage logged each tick. After 2.5s, the mob is raised back to the surface. If the move API isn't available, the fallback ring + root + DoT must be visible and the log must say `Sinkhole: visual-only mode (no move API)`.

---

### Step 2.6 — Ground-cracks visual overlay

**Files:**
- `src/main/resources/Server/Entity/Effects/MOTM/MOTM_Terra_Ground_Cracks.json` *(new)*
- `src/main/java/com/motm/util/HytaleAssetResolver.java`

**2.6.1 — Author the effect**

Use the existing `MOTM_Terra_Gem_Cast.json` as a template (open it, copy structure, replace particle composition). Compose:
- A ring of stone-dust particles at the radius edge.
- An interior fill of sparse stone-dust at center.
- (Optional, if vanilla has it) screen-shake or low-frequency rumble sound (`ISS_Combat_Earth_Impact` or similar — discover via `/showcase dump`).

**2.6.2 — Route Quake abilities to it**

In `HytaleAssetResolver.java`:

1. Add a constant near the other `FX_*` constants:
```java
private static final String FX_TERRA_GROUND_CRACKS = "MOTM_Terra_Ground_Cracks";
```

2. In `resolveLoopEffect`, in the `terra` branch of the `ground_zone` case (~line 351), prefer the cracks effect when `abilityId.contains("aftershock")` (Aftershock is the only Quake ability that creates a persistent loop).

3. In `resolveImpactEffect`, in the `terra` branch (~line 298), prefer the cracks effect when `abilityId.equals("stomp")` or `abilityId.equals("sinkhole")`.

4. (Optional) add a `resolveOverlayEffect(...)` that reads `ability.getVisualOverlay()` directly and returns the right effect — gives data-driven control without code edits next time we want a new overlay.

**Acceptance gate:**
- Build succeeds. All three Quake abilities visibly render the cracks effect when they fire (Stomp on landing; Aftershock for the 4s field; Sinkhole at the pit location).

---

### Step 2.7 — Update Phase 5 acceptance script

**File:** `CODEX_IMPLEMENTATION_PLAN_2026-05-13.md`

Replace the existing Phase 5 test script section (the numbered list around line 540-562) with the following text. Keep the surrounding "Goal", "Acceptance gate," and "Decision table" structure. **Do not delete the parent plan's Phase 6-10 sections.**

```
### Test script (run twice — both must pass)

1. Kill any running Hytale server.
2. Launch Hytale, enter a singleplayer creative world.
3. Open chat. Run:
   - /motm class terra
   - /motm race human
   - /motm style quake
4. Verify spellbook in inventory; equip it.
5. Place 2 test mobs in front of you: one standing on the ground, one floating (use creative tools).

6. **LMB once.** Observe:
   - In log: `[MOTM] Stomp armed: player=...` (no damage line yet).
   - In chat: "Stomp armed — jump and land to release the shockwave."

7. Jump (spacebar). On landing observe:
   - In log: `[MOTM] Stomp fired at landing: player=... pos=<x,y,z>`.
   - Visible: ground-cracks ring at landing point, radius 4.
   - The grounded test mob takes damage and is knocked back.
   - The floating test mob is NOT affected (ground-targets-only filter).

8. Wait 2 seconds (Stomp cooldown).

9. **RMB once.** Observe:
   - Visible: ground-cracks ring at caster's feet, radius 5, persistent for 4s.
   - The grounded test mob is slowed + disoriented.
   - The floating test mob is NOT affected.
   - In log: `abilityId=aftershock`.

10. Wait 5 seconds (Aftershock cooldown).

11. **Use (default E) once** while looking at the grounded test mob ~8 blocks away.
    - Within 0.6s, that mob visibly descends ~2-3 blocks below the ground.
    - DoT damage ticks in log: `[MOTM] Sinkhole DoT: ...`.
    - After 2.5s, the mob is raised back to the surface.
    - In log: `[MOTM] Sinkhole engaged: dragged 1 target(s)` and later `[MOTM] Sinkhole released: raised 1 target(s)`.

12. Stand idle 10 seconds. No HP loss. No movement slowdown.

13. Re-equip Stomp via LMB and confirm cooldown messages appear correctly if pressed before 2s have passed since the previous land.
```

**Acceptance gate:** Both runs of the new test script pass. If either fails, use the per-step decision tables above. Do **not** proceed to Phase 6/7/8/9.

---

## 3. Out of scope for this fix

- Phase 6 (Custom Spellbook UI page), Phase 7 (Perk effect integration), Phase 8 (PlayerInteractLib fallback), Phase 9 (broaden to 39 other styles), Phase 10 (cleanup). The parent plan governs those — do not start them.
- Generalizing the `trigger=jump_land` mechanic into a reusable cast-type. Wait until a second style wants it.
- Refactoring `GameplayPlaybackManager` god-class. Out of scope; tracked in `CODEX_CORRECTIONS_PLAN.md`.

## 4. Hard rules

- **Surgical edits only** to `data/styles/terra_styles.json`. Add the five new fields per ability, do not reformat or reorder anything else. The "Restore" commits in git history exist because previous AI runs ignored this rule.
- **No half-implementations.** If a step's acceptance gate fails, fix it before moving on — do not skip ahead with a TODO.
- **No backwards-compat shims.** If you decide to remove or rename an existing field (you shouldn't need to), delete it cleanly. No `// old name preserved for compat` comments.
- **No empty `catch (Throwable t) {}` blocks.** Always log.
- **No `--no-verify` on commits.**
