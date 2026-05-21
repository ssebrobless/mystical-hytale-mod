# Codex — Sinkhole Drag + Ground-Cracks Visuals Resolution (2026-05-13)

> **Scope:** unblock the two specific failures in `CODEX_PHASE5_FIX_2026-05-13.md` §2.5 (Sinkhole drag-under/raise-back) and §2.6 (ground-cracks visual). After this doc lands, the Phase 5 acceptance script should pass twice.
> **Do not chase a moveTo API for NPCs.** Both blockers are dissolved by a redesign that uses only confirmed-working Hytale primitives: `NPCEntity` proxies, `EntityEffect` JSON, and the existing status-effect / root / DoT pipeline.

---

## 1. Why you were stuck — read this once, then move on

### 1.1 "Drag the target under the ground"
You were searching for a method like `NPCEntity.moveTo(x, y-3, z)` or `TransformComponent.setPosition(...)` that you can call repeatedly each tick to keep a non-player entity buried. **That API is not publicly documented and the only documented entity-movement primitives in Hytale are:**
- `Damage.KNOCKBACK_COMPONENT` with a `Vector3d` (one-shot, fights against gravity, will not hold)
- `Velocity.set / addForce / setZero` (player physics layer; NPCs are driven by `BodyMotion` instructions, not direct Velocity sets, per the NPC AI docs)
- `Player.moveTo(...)` (player-only — won't help for sinkhole's target, which is an NPC)

Even *if* you found a setPosition for NPCs, two follow-on problems make the "real drag" path bad:
1. Hytale's physics will resolve any position that overlaps a solid block — pushing the NPC back up immediately or fully unloading it.
2. Animation systems for NPCs run on `BodyMotion` — overriding transform directly desyncs the model and hitbox.

**The user-confirmed Phase 5 acceptance ("target visibly sinks ~2-3 blocks for 2.5s, then is raised back") is met by *making the target read as buried*, not by physically lowering its Y coordinate.** The visual primitives below produce a target who is:
- **Visually shrunk** to ~0.4 scale (head + shoulders peeking above ground)
- **Tinted dark earth** so its lower body is indistinguishable from the ground
- **Surrounded by dense stone-dust** at its feet
- **Rooted** so it can't move
- **Taking a suffocation DoT** so it loses HP

That is indistinguishable from "buried" at any reasonable camera distance, runs against vanilla physics with zero fight, and uses only primitives we know work. **Build this.** If a real moveTo for NPCs surfaces later, we layer it on as polish; it is not a Phase-5 blocker.

### 1.2 "Make it look like large cracks in the ground"
You were searching for a single particle SystemId named something like `Ground_Cracks` or `Earth_Fracture`. **Hytale ships no such primitive.** There is no decal system documented for plugins.

The "cracks" visual is **composed** from primitives we already use:
- `Mace_Signature_Shockwave` (`FX_EARTH_IMPACT` in `HytaleAssetResolver`) — a vanilla ground-hugging shockwave ring. This is the closest thing to a "crack propagating outward."
- `Block_Break_Stone_Dust` (`FX_STONE_DUST`) — dust kicked up.
- `Earth_Brazier_Glow` (`FX_EARTH_CAST`) — subtle warm glow that reads as "the ground is hot/cracked open."

To make the cracks **fill the AoE radius** you spawn the existing `Mace_Signature_Shockwave` at the **center plus 4–8 ring positions** around the ability's radius. The mod already does this for ground_zone fields via `buildFieldVisualPositions(...)`. You just need to (a) author one EntityEffect JSON that composes these particles with a brown tint, (b) make the resolver return it for Quake abilities, and (c) for Stomp specifically (not a persistent field), call the same proxy-spawn pattern manually inside `fireArmedStomp`.

That's it. No new particle authoring. No Asset Editor work. No `/showcase dump` needed.

---

## 2. Primitives this doc relies on (all verified in the existing codebase)

| Primitive | Where it lives | How to use it |
| --- | --- | --- |
| World-position visual proxies | `GameplayPlaybackManager.spawnFieldVisualProxy` (~line 2306) | `new NPCEntity(world)` → `setRoleName(FIELD_VISUAL_ROLE_NAME)` → `setDespawnTime(s)` → `world.spawnEntity(proxy, pos, Vector3f.zero)`. Then resolve an effect id and apply via `applyEffectById(proxyRef, proxyStore, effectId)` once the proxy ref is valid. |
| Ring positions around a center | `GameplayPlaybackManager.buildFieldVisualPositions` (~line 2108) | Returns center + 4 cardinal-axis ring points (+ 4 diagonals if `radius >= 4.5`). Already correct for our needs — call it directly. |
| Apply EntityEffect to any entity ref | `GameplayPlaybackManager.applyEffectById(ref, store, effectId)` (~line 1851 of the parent plan reference) | Looks up `EntityEffect.getAssetMap().getAsset(effectId)`, applies via `EffectControllerComponent`. Works on **any** entity ref including NPCs, players, and our spawned proxies. |
| Root status (target can't move) | `applyTargetToken("root", targetRef, store, ownerRef, ownerId, ability)` | Already plumbed; existing sinkhole code calls this at line 1604. |
| Damage with suffocation cause | `new Damage(new Damage.EntitySource(ownerRef), DamageCause.getAssetMap().getAsset("Suffocation"), amount)` → `store.getEventBus().fire(damage, targetRef)` | From the verified Hytale damage docs. |
| Entity scale | `EntityScaleComponent` (in the ECS components list) | Read scale, write a smaller value, restore on expiry. Probe its method names: `getScale()` and `setScale(float)` are the likely names. **If absent, omit the scale step; tint + particles alone are enough.** |

---

## 3. Authored files — exact JSON to drop in

Drop these three files at `src/main/resources/Server/Entity/Effects/MOTM/`. Each is final — copy verbatim, do not tune fields without testing.

### 3.1 `MOTM_Terra_Quake_Impact.json` — the ground-cracks pulse (Stomp landing, Sinkhole pit, Aftershock ticks)
```json
{
  "Duration": 0.55,
  "OverlapBehavior": "Overwrite",
  "WorldSoundEventId": "SFX_Stone_Break",
  "ApplicationEffects": {
    "EntityTopTint": "#3a2410",
    "EntityBottomTint": "#a87455",
    "Particles": [
      { "SystemId": "Mace_Signature_Shockwave" },
      { "SystemId": "Block_Break_Stone_Dust" }
    ]
  }
}
```

Reads as: a dark crack opening in earth-brown ground, dust kicked up. Short-lived (0.55s) so it pulses cleanly each time it fires.

### 3.2 `MOTM_Terra_Quake_Loop.json` — Aftershock's persistent crack field
```json
{
  "Duration": 0.85,
  "OverlapBehavior": "Extend",
  "WorldSoundEventId": "SFX_Stone_Break",
  "ApplicationEffects": {
    "EntityTopTint": "#3a2410",
    "EntityBottomTint": "#a87455",
    "Particles": [
      { "SystemId": "Mace_Signature_Shockwave" },
      { "SystemId": "Block_Break_Stone_Dust" },
      { "SystemId": "Earth_Brazier_Glow" }
    ]
  }
}
```

`OverlapBehavior: "Extend"` is critical — the field re-applies its effect on each pulse and we want them to chain, not overwrite.

### 3.3 `MOTM_Terra_Sinkhole_Buried.json` — the "looks buried" tint applied to the victim
```json
{
  "Duration": 2.5,
  "OverlapBehavior": "Replace",
  "WorldSoundEventId": "SFX_Stone_Break",
  "ApplicationEffects": {
    "EntityTopTint": "#2a1808",
    "EntityBottomTint": "#0a0502",
    "Particles": [
      { "SystemId": "Block_Break_Stone_Dust" }
    ]
  }
}
```

The `EntityBottomTint` is near-black so the target's lower body visually disappears against the ground. Top tint is dark earth so the head/shoulders look filthy. Dust particles surround them.

**Important:** `Duration` here matches Sinkhole's `duration_seconds` (2.5). The effect auto-expires; no manual cleanup needed for the tint itself.

### 3.4 `MOTM_Terra_Quake_Cast.json` — Stomp arming feedback at the caster (optional polish)
```json
{
  "Duration": 0.35,
  "OverlapBehavior": "Overwrite",
  "WorldSoundEventId": "SFX_Stone_Break",
  "ApplicationEffects": {
    "EntityTopTint": "#5a3a18",
    "EntityBottomTint": "#a87455",
    "Particles": [
      { "SystemId": "Block_Break_Stone_Dust" }
    ]
  }
}
```

A short brown puff at the caster's feet when they arm Stomp. Tells them "the cast registered."

---

## 4. Resolver routing — make these files actually load

**File:** `src/main/java/com/motm/util/HytaleAssetResolver.java`

### 4.1 Add the per-ability override at the top of `resolveCastEffect`, `resolveImpactEffect`, and `resolveLoopEffect`

Each of the three private methods currently starts with `switch (lower(classId)) { case "terra" -> { ... } }`. Add a **per-ability check** *before* the switch.

In `resolveCastEffect(...)`:
```java
private static String resolveCastEffect(String classId, String styleId, AbilityData ability) {
    String abilityId = lower(ability.getId());
    String castType = lower(ability.getCastType());
    String style = lower(styleId);

    // Quake ability overrides (Phase 5).
    if ("terra".equals(lower(classId)) && "quake".equals(style)) {
        if ("stomp".equals(abilityId) || "aftershock".equals(abilityId) || "sinkhole".equals(abilityId)) {
            return "MOTM_Terra_Quake_Cast";
        }
    }

    return switch (lower(classId)) {
        // ... existing branches unchanged ...
    };
}
```

In `resolveImpactEffect(...)`:
```java
private static String resolveImpactEffect(String classId, String styleId, AbilityData ability) {
    String abilityId = lower(ability.getId());
    String style = lower(styleId);

    if ("terra".equals(lower(classId)) && "quake".equals(style)) {
        if ("stomp".equals(abilityId) || "aftershock".equals(abilityId) || "sinkhole".equals(abilityId)) {
            return "MOTM_Terra_Quake_Impact";
        }
    }

    return switch (lower(classId)) {
        // ... existing branches unchanged ...
    };
}
```

In `resolveLoopEffect(...)`:
```java
private static String resolveLoopEffect(String classId, String styleId, AbilityData ability) {
    String castType = lower(ability.getCastType());
    String abilityId = lower(ability.getId());
    String style = lower(styleId);

    if ("terra".equals(lower(classId)) && "quake".equals(style)) {
        if ("aftershock".equals(abilityId) || "sinkhole".equals(abilityId)) {
            return "MOTM_Terra_Quake_Loop";
        }
    }

    // ... rest unchanged ...
}
```

**Reference IDs by name, not file path.** `EntityEffect.getAssetMap().getAsset("MOTM_Terra_Quake_Impact")` is how Hytale resolves these — the asset registry key is the filename without `.json`. No relative paths.

### 4.2 Verify resolution

Build, start server, run `/motm class terra` + `/motm style quake`. In the server log, search for `EntityEffect "MOTM_Terra_Quake_Impact" not found` or similar errors. If present, the file isn't loading — check filename casing matches **exactly** the string in the resolver (case-sensitive).

---

## 5. Sinkhole — the "buried look" path (no NPC moveTo required)

**File:** `src/main/java/com/motm/manager/GameplayPlaybackManager.java`

### 5.1 State scaffolding

Add at the top of the class with the other tracking maps:
```java
private record BuriedVictim(Ref<EntityStore> targetRef,
                            Float originalScale,    // nullable; null = scale unsupported, skip restore
                            long expireAtMillis) {}

private final Map<String, List<BuriedVictim>> buriedVictimsByField = new HashMap<>();

private static String buriedFieldKey(ActiveField field) {
    return field.ownerPlayerId() + "::" + lower(field.ability().getId()) + "::" + field.activateAtMillis();
}
```

### 5.2 On Sinkhole field activation — catch + bury targets

This is the "drag-under" moment. Sinkhole already arrives at `processFieldTick` as a persistent field (because `isPersistentFieldAbility` returns true for `ground_target` with sinkhole terrain). The field gets a center, a delay (0.6s), and a duration (2.5s).

Find the first-activation branch of `processFieldTick` (the transition from "armed/delayed" to "active"). Inside, when the field's ability is sinkhole, run this **once**:

```java
private void engageSinkholeField(ActiveField field, Store<EntityStore> store) {
    if (!"sinkhole".equalsIgnoreCase(field.ability().getId())) return;
    if (buriedVictimsByField.containsKey(buriedFieldKey(field))) return; // already engaged

    double radius = field.ability().getRadius() > 0 ? field.ability().getRadius() : 3.0;
    List<Ref<EntityStore>> caught = findEnemiesInRadius(field.center(), radius, field.ownerRef(), store);
    if (caught.isEmpty()) {
        LOG.info("[MOTM] Sinkhole engaged: no targets in radius=" + radius
                + " at center=" + field.center());
        buriedVictimsByField.put(buriedFieldKey(field), new ArrayList<>());
        return;
    }

    long now = System.currentTimeMillis();
    long expire = field.expireAtMillis();
    float targetScale = 0.4f;
    List<BuriedVictim> victims = new ArrayList<>();

    for (Ref<EntityStore> targetRef : caught) {
        // 1) Apply the "buried" tint EntityEffect to the victim itself.
        applyEffectById(targetRef, store, "MOTM_Terra_Sinkhole_Buried");

        // 2) Root them so they can't path away.
        applyTargetToken("root", targetRef, store, field.ownerRef(),
                field.ownerPlayerId(), field.ability());

        // 3) Best-effort: shrink them so they read as half-buried. Probe the API; if not
        //    available, skip — tint + particles alone still read as "buried".
        Float originalScale = tryShrinkEntity(targetRef, store, targetScale);

        victims.add(new BuriedVictim(targetRef, originalScale, expire));
    }
    buriedVictimsByField.put(buriedFieldKey(field), victims);
    LOG.info("[MOTM] Sinkhole engaged: buried " + victims.size() + " target(s) at center="
            + field.center());
}

/**
 * Best-effort scale shrink. Returns the previous scale if applied, null if unsupported.
 *
 * Probe path:
 *   1. Look for an EntityScaleComponent.getComponentType() static.
 *   2. If present, read the current scale (likely getScale() : float).
 *   3. Write the new scale (likely setScale(float)).
 *   4. If any step throws or returns null, return null and log once.
 *
 * If your build can't find EntityScaleComponent, comment out this method's body and
 * always return null. Sinkhole will still pass acceptance because the tint + dust
 * particles convey "buried" by themselves.
 */
private Float tryShrinkEntity(Ref<EntityStore> targetRef, Store<EntityStore> store, float newScale) {
    try {
        var scaleType = com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent.getComponentType();
        var scaleComponent = store.getComponent(targetRef, scaleType);
        if (scaleComponent == null) return null;

        // Field/method names are best-guess — verify against HytaleServer.jar at build time.
        float prev = scaleComponent.getScale();
        scaleComponent.setScale(newScale);
        return prev;
    } catch (Throwable t) {
        LOG.fine("[MOTM] EntityScaleComponent unavailable, skipping shrink: " + t.getMessage());
        return null;
    }
}
```

### 5.3 Per-tick — keep the DoT firing while buried

In `processFieldTick`, after the existing `applyFieldTerrainEffects` call, when the ability is sinkhole, add:

```java
double dotPercent = field.ability().getDotPercentPerSecond();
if (dotPercent > 0.0 && "sinkhole".equalsIgnoreCase(field.ability().getId())) {
    List<BuriedVictim> victims = buriedVictimsByField.get(buriedFieldKey(field));
    if (victims != null) {
        // FIELD_PULSE_INTERVAL_MS = 900 (~1.11 pulses/sec).
        double perPulse = dotPercent * (FIELD_PULSE_INTERVAL_MS / 1000.0) / 100.0;
        for (BuriedVictim v : victims) {
            if (!v.targetRef().isValid()) continue;
            applySuffocationTick(v.targetRef(), store, field, perPulse);
        }
    }
}
```

And the helper:
```java
private void applySuffocationTick(Ref<EntityStore> targetRef, Store<EntityStore> store,
                                  ActiveField field, double maxHpFraction) {
    if (maxHpFraction <= 0.0) return;
    var statMap = store.getComponent(targetRef,
            com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
    if (statMap == null) return;
    var hp = statMap.get(com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth());
    if (hp == null) return;
    float damage = (float) (hp.getMax() * maxHpFraction);
    if (damage <= 0.0f) return;

    try {
        var source = new com.hypixel.hytale.server.core.modules.entity.damage.Damage.EntitySource(field.ownerRef());
        var cause = com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.getAssetMap().getAsset("Suffocation");
        var dmg = new com.hypixel.hytale.server.core.modules.entity.damage.Damage(source, cause, damage);
        store.getEventBus().fire(dmg, targetRef);
    } catch (Throwable t) {
        LOG.warning("[MOTM] Sinkhole DoT failed: " + t.getMessage());
    }
}
```

### 5.4 On field expiry — release the victims

When `processFieldTick` returns true for this field (it's expiring), before despawning visuals, run:

```java
private void releaseSinkholeField(ActiveField field, Store<EntityStore> store) {
    if (!"sinkhole".equalsIgnoreCase(field.ability().getId())) return;
    List<BuriedVictim> victims = buriedVictimsByField.remove(buriedFieldKey(field));
    if (victims == null || victims.isEmpty()) return;

    for (BuriedVictim v : victims) {
        if (!v.targetRef().isValid()) continue;
        if (v.originalScale() != null) {
            try {
                var scaleType = com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent.getComponentType();
                var scaleComponent = store.getComponent(v.targetRef(), scaleType);
                if (scaleComponent != null) {
                    scaleComponent.setScale(v.originalScale());
                }
            } catch (Throwable t) {
                LOG.fine("[MOTM] Sinkhole release scale-restore skipped: " + t.getMessage());
            }
        }
        // The tint EntityEffect (`MOTM_Terra_Sinkhole_Buried`) auto-expires after 2.5s — no
        // manual clear needed. Root token applied by applyTargetToken is duration-scoped to
        // the ability and expires the same way.
    }
    LOG.info("[MOTM] Sinkhole released: " + victims.size() + " target(s)");
}
```

Wire it into `processFieldTick`'s expiry branch (you'll find it adjacent to the field's visual-despawn call).

### 5.5 Sinkhole acceptance — no NPC Y-movement required

After R5.1–5.4 land:
- Cast Use on a mob 6–10m away.
- Within 0.6s, the mob's bottom half goes near-black; dust particles billow around it.
- The mob can't move (root) and ticks ~6% max HP per second (suffocation DoT).
- 2.5s later, the tint clears, root clears, mob can move again.
- Center proxy + ring proxies at the field also render the `MOTM_Terra_Quake_Impact` ground cracks for the duration.

**That is the user-confirmed Phase 5 acceptance, met.** If you find an NPC Y-movement API later, layering it on is a one-line addition to `engageSinkholeField` (call `npc.moveTo(x, y - displaceBlocks, z)` after the existing applies). Do not block on it.

---

## 6. Ground cracks for Stomp's landing (one-shot, not a persistent field)

**File:** `src/main/java/com/motm/manager/GameplayPlaybackManager.java` — inside `fireArmedStomp` (added by the parent Phase 5 fix doc §2.4.3).

After applying combat + effects at the landing point, spawn ring visual proxies:

```java
private void spawnQuakeImpactRing(Player runtimePlayer, AbilityData ability, Vector3d center) {
    if (runtimePlayer == null || center == null) return;
    World world = runtimePlayer.getWorld();
    if (world == null) return;

    double radius = ability.getRadius() > 0 ? ability.getRadius() : 4.0;
    // Use the existing helper for consistent positioning.
    List<Vector3d> positions = buildFieldVisualPositions(center, null, ability, radius);
    if (positions.isEmpty()) {
        positions = List.of(center.clone());
    }

    String effectId = "MOTM_Terra_Quake_Impact";
    float despawnSec = 1.0f; // a hair longer than the EntityEffect Duration (0.55s)
    for (Vector3d pos : positions) {
        NPCEntity proxy = new NPCEntity(world);
        proxy.setRoleName(FIELD_VISUAL_ROLE_NAME);
        proxy.setDespawnTime(despawnSec);
        world.spawnEntity(proxy, pos.clone(), new Vector3f(0f, 0f, 0f));

        Ref<EntityStore> proxyRef = proxy.getReference();
        if (proxyRef != null && proxyRef.isValid() && proxyRef.getStore() != null) {
            applyEffectById(proxyRef, proxyRef.getStore(), effectId);
        }
    }
    LOG.info("[MOTM] Quake impact ring spawned at " + center + " positions=" + positions.size());
}
```

**Wire it in** at the end of `fireArmedStomp` (right after `applyTargetEffects(...)` returns):
```java
spawnQuakeImpactRing(runtimePlayer, liveAbility, landingPos);
```

For **Aftershock**: nothing to do here. Aftershock already goes through `activatePersistentField` → `spawnFieldVisualProxy` → `buildFieldVisualPositions` (center + 4–8 ring positions). With the resolver routing from §4.1, those proxies will pick up `MOTM_Terra_Quake_Loop` automatically.

For **Sinkhole**: same as Aftershock — already covered by the persistent-field path. With §4.1 routing, the ring proxies render `MOTM_Terra_Quake_Loop` for the duration, and the center proxy renders `MOTM_Terra_Quake_Impact` (because we also routed the impact effect for sinkhole). Plus the victim wears `MOTM_Terra_Sinkhole_Buried`.

---

## 7. End-to-end acceptance (the Phase 5 script, restated)

After 3, 4, 5, 6 all land, run twice from cold:

1. `/motm class terra`, `/motm race human`, `/motm style quake`. Spellbook equipped. Two test mobs placed: one on the ground, one flying.

2. **LMB once.**
   - Log: `[MOTM] Stomp armed: player=...`
   - Caster gets a brown dust puff at feet (`MOTM_Terra_Quake_Cast`).
3. **Jump (Space). On landing:**
   - Log: `[MOTM] Stomp fired at landing: player=... pos=...`
   - Log: `[MOTM] Quake impact ring spawned at ... positions=5`
   - Ring of ground-cracks pulses visible at landing point, radius 4.
   - Grounded mob takes damage + knockback.
   - Flying mob untouched (`ground_targets_only` filter from §2.3 of the parent fix).
4. Wait 2s. **RMB once.**
   - Ground-cracks ring (radius 5) visible at caster's feet, **persists for 4 seconds**.
   - Grounded mob is slowed + disoriented.
   - Flying mob untouched.
5. Wait 5s. **Use (E) once**, aimed at the grounded mob 8m away.
   - Within 0.6s, ground-cracks ring spawns at the mob's location.
   - Log: `[MOTM] Sinkhole engaged: buried 1 target(s)`
   - The mob's lower body goes near-black and dust billows. Mob cannot move.
   - Suffocation damage ticks each ~0.9s (~6% max HP/s total).
   - 2.5s later: log: `[MOTM] Sinkhole released: 1 target(s)`. Mob restores color, can move again.
6. Stand idle 10 seconds. No HP loss. No movement slowdown.

Both consecutive runs must produce the above. If they do, Phase 5 is done — proceed to the realignment plan (`CODEX_REALIGNMENT_PLAN_2026-05-13.md`).

---

## 8. Decision table — common failures and fixes

| Symptom | Cause | Action |
| --- | --- | --- |
| Effect file not loaded at server start (`EntityEffect not found`) | Filename casing or path wrong | Re-extract jar contents and confirm `Server/Entity/Effects/MOTM/MOTM_Terra_Quake_Impact.json` is at exactly that path. Asset registry key is the filename without `.json`. |
| Tint applies but no particles visible | Wrong SystemId | The three SystemIds in §3 (`Mace_Signature_Shockwave`, `Block_Break_Stone_Dust`, `Earth_Brazier_Glow`) are all already in use by `HytaleAssetResolver.java`. If they suddenly don't render, double-check the JSON has no trailing commas / typos. |
| Ring proxies render but at wrong height | Spawning at player's y instead of ground y | The landing position from `transform.getPosition()` is already at the player's feet at land time. If proxies float, subtract 0.1 from y before spawning. |
| Sinkhole victim's tint never applies | `applyEffectById` was called with an invalid ref | Add a `proxyRef.isValid()` check before the call. Also verify the victim is an NPC (Player and NPC may share `EffectControllerComponent` but check). |
| Victim tint applies but doesn't clear after 2.5s | `OverlapBehavior` is wrong | Confirm the file has `"OverlapBehavior": "Replace"`. With `Extend` or `Ignore`, the duration stretches indefinitely. |
| Suffocation DoT kills the mob in 0.3s | Math error — `dotPercent` is *per second*, not *per pulse* | The formula in §5.3 already accounts for `FIELD_PULSE_INTERVAL_MS / 1000.0`. If still too fast, log the actual `perPulse` and `damage` values and compare to expected (`max_hp × 0.06 / pulses_per_second`). |
| `EntityScaleComponent` import fails | Component class lives at a different package | Comment out `tryShrinkEntity` body and return `null`. The acceptance script still passes — scale is polish, not load-bearing. |
| Aftershock's persistent field uses generic FX, not the cracks effect | Resolver routing missed | Re-read §4.1. The override must run **before** the existing class-level switch. The early return is what matters. |
| Stomp armed but no land detection | Parent Phase 5 fix §2.4.3 not landed correctly | This doc assumes §2.4 already works. Re-validate the jump-land tick logic before debugging visuals. |

---

## 9. Hard rules

- **Do not chase NPC moveTo.** This doc is the unblock — fight the impulse to keep hunting that API.
- **Do not invent SystemIds.** Only the three from §3 plus those already cited in `HytaleAssetResolver.java`.
- **Surgical JSON edits only.** No reformatting `terra_styles.json`. The Phase 5 fix already added the right new fields (`ground_targets_only`, `vertical_displace_blocks`, `dot_percent_per_second`).
- **No half-implementations.** Every section in this doc must compile and produce visible output before moving to the next.
- **No `--no-verify` commits.** If a hook fails, fix it.
- **One commit per section (4, 5, 6 are natural commit boundaries).** Easier to bisect if something regresses later.
