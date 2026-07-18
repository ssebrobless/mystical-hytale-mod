package com.motm.runtime.ability.dash;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.runtime.ability.AbilityExecutionPolicy;
import com.motm.runtime.ability.AbilityRuntimeEffects;
import com.motm.util.MotmEntityLiveness;
import org.joml.Vector3d;

import java.util.List;
import java.util.Locale;

/** Owns world-space dash cues and swept-path target mutation. */
public final class DashHytaleAdapter {
    private static final long TRAIL_INTERVAL_MILLIS = 90L;
    private static final long MAX_DASH_WINDOW_MILLIS = 2_200L;
    private static final double ARRIVAL_TOLERANCE = 0.45;
    // ENGINE RULE (live-verified 2026-07-18): ParticleUtil world spawns are
    // fire-and-forget - a system whose spawners lack TotalParticles emits
    // FOREVER at that position. World bursts MUST use burst-only systems
    // (every spawner TotalParticles-capped). Sand_Storm/Block_Sprint_Sand/
    // Wind_Spirit_Tail/Fire_Charge1/Fire_AoE_Grow are continuous - never
    // world-spawn them.
    private static final String SAND_STORM = "Block_Break_Sand";
    private static final String SAND_SPRINT = "Block_Land_Sand_Hard";
    private static final String WIND_TAIL = "Block_Break_Dust";
    private static final String WATER_TRAIL = "Bubbles_Breathing";
    private static final String VOID_TRAIL = "VoidImpact";
    private static final String STONE_TRAIL = "Block_Break_Stone";

    private final DashRuntimeState state;
    private final Hooks hooks;

    public DashHytaleAdapter(DashRuntimeState state, Hooks hooks) {
        this.state = state;
        this.hooks = hooks;
    }

    public boolean start(Player runtimePlayer,
                         PlayerData player,
                         String classId,
                         String styleId,
                         AbilityData ability,
                         Vector3d start,
                         Vector3d direction,
                         long now) {
        if (runtimePlayer == null || player == null || ability == null || start == null || direction == null
                || !isDashFamily(ability)) {
            return false;
        }
        Ref<EntityStore> ownerRef = runtimePlayer.getReference();
        if (ownerRef == null || !ownerRef.isValid()) {
            return false;
        }
        double distance = Math.max(0.0, ability.getDashDistance() > 0.0
                ? ability.getDashDistance() : ability.getRange());
        if (distance <= 0.0) {
            return false;
        }
        long window = dashWindowMillis(ability);
        state.removeForPlayer(player.getPlayerId());
        state.add(new ActiveDash(
                player.getPlayerId(), classId, styleId, ability, ownerRef, start,
                normalize(direction), now, now + window, distance));
        applyEffect(ownerRef, ownerRef.getStore(), castEffectId(classId, ability));
        applyEffect(ownerRef, ownerRef.getStore(), travelEffectId(classId, styleId, ability));
        if (isDustDevil(ability)) {
            applyEffect(ownerRef, ownerRef.getStore(), "MOTM_Terra_Dash_Sand_Travel");
        }
        return true;
    }

    public boolean startFromEndpoints(Player runtimePlayer,
                                      PlayerData player,
                                      String classId,
                                      String styleId,
                                      AbilityData ability,
                                      Vector3d start,
                                      Vector3d end,
                                      long now) {
        Vector3d direction = end == null || start == null
                ? new Vector3d(0.0, 0.0, 1.0)
                : new Vector3d(end).sub(start);
        return start(runtimePlayer, player, classId, styleId, ability, start, direction, now);
    }

    public void processForStore(Store<EntityStore> store, long now) {
        if (store == null || state.activeDashCount() == 0) {
            return;
        }
        state.removeProcessed(dash -> processOne(dash, store, now));
    }

    public int removeForPlayer(String playerId) {
        return state.removeForPlayer(playerId);
    }

    public int activeDashCount() {
        return state.activeDashCount();
    }

    private boolean processOne(ActiveDash dash, Store<EntityStore> store, long now) {
        Player player = hooks.resolvePlayer(dash.ownerPlayerId());
        Ref<EntityStore> ref = dash.ownerRef();
        if (player == null || ref == null || !ref.isValid() || ref.getStore() != store) {
            return true;
        }
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        Vector3d position = transform == null || transform.getTransform() == null
                ? null : transform.getTransform().getPosition();
        if (position == null || !position.isFinite()) {
            return true;
        }

        double progress = new Vector3d(position).sub(dash.startPosition()).dot(dash.direction());
        boolean arrived = progress >= dash.distance() - ARRIVAL_TOLERANCE;
        if (arrived || now >= dash.expiresAtMillis()) {
            if (!dash.endCueApplied()) {
                // Cleanup rule 8: the cast/travel effects were re-applied every
                // trail tick; remove them explicitly so nothing lingers on the
                // caster past the dash window (live finding 2026-07-18).
                hooks.removeEffect(ref, store, travelEffectId(dash.classId(), dash.styleId(), dash.ability()));
                hooks.removeEffect(ref, store, castEffectId(dash.classId(), dash.ability()));
                if (isDustDevil(dash.ability())) {
                    hooks.removeEffect(ref, store, "MOTM_Terra_Dash_Sand_Travel");
                }
                applyEffect(ref, store, impactEffectId(dash.classId(), dash.styleId(), dash.ability()));
                spawnWorldBurst(endSystemId(dash.classId(), dash.ability()), position, store);
                dash.markEndCueApplied();
            }
            return true;
        }
        if (now - dash.lastTrailAtMillis() >= TRAIL_INTERVAL_MILLIS) {
            spawnWorldBurst(trailSystemId(dash.classId(), dash.ability()), position, store);
            applyEffect(ref, store, travelEffectId(dash.classId(), dash.styleId(), dash.ability()));
            if (isDustDevil(dash.ability())) {
                applySweptKnockback(dash, store, position);
                spawnWorldBurst(SAND_SPRINT, position, store);
            } else if (isAfterburner(dash.ability())) {
                spawnWorldBurst("Explosion_Small", position, store);
            }
            dash.markTrail(now);
        }
        return false;
    }

    private void applySweptKnockback(ActiveDash dash, Store<EntityStore> store, Vector3d position) {
        double radius = dash.ability().getRadius() > 0.0 ? dash.ability().getRadius() : 5.0;
        List<Ref<EntityStore>> targets = hooks.collectNearbyTargets(store, position, radius, 32);
        if (targets == null) {
            return;
        }
        for (Ref<EntityStore> target : targets) {
            if (target == null || !MotmEntityLiveness.isLiveTarget(target, store)
                    || target.equals(dash.ownerRef()) || dash.hasSweptTarget(target.getIndex())) {
                continue;
            }
            if (hooks.applyKnockback(target, store, dash.ownerRef(), dash.ability())) {
                dash.markSweptTarget(target.getIndex());
            }
        }
    }

    private void applyEffect(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
        if (ref != null && ref.isValid() && store != null && effectId != null && !effectId.isBlank()) {
            hooks.applyEffect(ref, store, effectId);
        }
    }

    private void spawnWorldBurst(String systemId, Vector3d position, Store<EntityStore> store) {
        if (systemId == null || systemId.isBlank() || position == null || store == null) {
            return;
        }
        try {
            ParticleUtil.spawnParticleEffect(systemId, position, store);
        } catch (Throwable error) {
            hooks.logWarning("Dash particle burst failed system=" + systemId + " error=" + error.getMessage());
        }
    }

    private static String castEffectId(String classId, AbilityData ability) {
        if (isDustDevil(ability)) {
            return "MOTM_Terra_Dash_Sand_Cast";
        }
        return switch (lower(classId)) {
            case "terra" -> "MOTM_Terra_Dash_Cast";
            case "hydro" -> "MOTM_Hydro_Dash_Cast";
            case "corruptus" -> "MOTM_Corruptus_Dash_Cast";
            default -> "MOTM_Aero_Dash_Cast";
        };
    }

    private static String travelEffectId(String classId, String styleId, AbilityData ability) {
        if (isDustDevil(ability)) {
            return "MOTM_Terra_Dash_Sand_Travel";
        }
        String candidate = AbilityRuntimeEffects.projectileVisualEffectId(classId, styleId, ability);
        return isMOTMEffect(candidate) ? candidate : familyFallback(classId, "move");
    }

    private static String impactEffectId(String classId, String styleId, AbilityData ability) {
        if (isDustDevil(ability)) {
            return "MOTM_Terra_Dash_Sand_Impact";
        }
        String candidate = AbilityRuntimeEffects.impactEffectId(classId, styleId, ability);
        return isMOTMEffect(candidate) ? candidate : familyFallback(classId, "impact");
    }

    private static String familyFallback(String classId, String cue) {
        return switch (lower(classId)) {
            case "terra" -> "MOTM_Terra_Dash_" + ("move".equals(cue) ? "Travel" : "Impact");
            case "hydro" -> "MOTM_Hydro_Dash_" + ("move".equals(cue) ? "Travel" : "Impact");
            case "corruptus" -> "MOTM_Corruptus_Dash_" + ("move".equals(cue) ? "Travel" : "Impact");
            default -> "MOTM_Aero_Dash_" + ("move".equals(cue) ? "Travel" : "Impact");
        };
    }

    private static String trailSystemId(String classId, AbilityData ability) {
        if (isDustDevil(ability)) return SAND_STORM;
        if (isAfterburner(ability)) return "Explosion_Small";
        return switch (lower(classId)) {
            case "hydro" -> WATER_TRAIL;
            case "corruptus" -> VOID_TRAIL;
            case "terra" -> STONE_TRAIL;
            default -> WIND_TAIL;
        };
    }

    private static String endSystemId(String classId, AbilityData ability) {
        if (isDustDevil(ability)) return SAND_STORM;
        if (isAfterburner(ability)) return "Explosion_Small";
        return switch (lower(classId)) {
            case "hydro" -> WATER_TRAIL;
            case "corruptus" -> VOID_TRAIL;
            case "terra" -> STONE_TRAIL;
            default -> WIND_TAIL;
        };
    }

    private static boolean isDashFamily(AbilityData ability) {
        String castType = lower(ability.getCastType());
        String id = lower(ability.getId());
        return AbilityExecutionPolicy.isMovementCastType(castType)
                || "waverider".equals(id)
                || "river_rapids".equals(id);
    }

    private static long dashWindowMillis(AbilityData ability) {
        if (isDustDevil(ability)) return 2_000L;
        return Math.min(MAX_DASH_WINDOW_MILLIS, Math.max(350L,
                Math.round(Math.max(0.35, ability.getCastTimeSeconds()) * 1000.0)));
    }

    private static boolean isDustDevil(AbilityData ability) {
        return ability != null && "dust_devil".equals(lower(ability.getId()));
    }

    private static boolean isAfterburner(AbilityData ability) {
        return ability != null && "afterburner".equals(lower(ability.getId()));
    }

    private static boolean isMOTMEffect(String effectId) {
        return effectId != null && effectId.startsWith("MOTM_");
    }

    private static Vector3d normalize(Vector3d vector) {
        Vector3d normalized = new Vector3d(vector.x, 0.0, vector.z);
        return normalized.isFinite() && normalized.lengthSquared() > 0.0001
                ? normalized.normalize() : new Vector3d(0.0, 0.0, 1.0);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public interface Hooks {
        Player resolvePlayer(String playerId);
        boolean applyEffect(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);
        boolean removeEffect(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);
        List<Ref<EntityStore>> collectNearbyTargets(Store<EntityStore> store, Vector3d center, double radius, int maxTargets);
        boolean applyKnockback(Ref<EntityStore> targetRef, Store<EntityStore> store,
                               Ref<EntityStore> sourceRef, AbilityData ability);
        void logWarning(String message);
    }
}
