package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.model.StyleData;
import com.motm.runtime.ability.AbilityRuntimeEffects;
import com.motm.runtime.ability.field.FieldRuntimeSpecs;
import com.motm.runtime.ability.field.FieldVisualHytaleAdapter;
import com.motm.runtime.ability.field.FieldVisualRuntime;
import com.motm.util.AbilityPresentation;

import java.util.List;
import java.util.ArrayList;
import java.util.Locale;

/**
 * Owns supplemental non-persistent terrain trails and aura fields created by
 * movement/self abilities after the base cast playback resolves.
 */
public final class TerrainSupplementalHytaleAdapter {

    private final FieldVisualHytaleAdapter fieldVisualAdapter;
    private final TerrainPlacementHytaleAdapter terrainPlacementAdapter;
    private final FieldRegistrar fieldRegistrar;

    public TerrainSupplementalHytaleAdapter(FieldVisualHytaleAdapter fieldVisualAdapter,
                                            TerrainPlacementHytaleAdapter terrainPlacementAdapter,
                                            FieldRegistrar fieldRegistrar) {
        this.fieldVisualAdapter = fieldVisualAdapter;
        this.terrainPlacementAdapter = terrainPlacementAdapter;
        this.fieldRegistrar = fieldRegistrar;
    }

    public Result activate(Player runtimePlayer,
                           PlayerData player,
                           StyleData style,
                           AbilityData ability,
                           boolean movementApplied,
                           Vector3d startPosition,
                           Vector3d endPosition) {
        if (runtimePlayer == null || player == null || style == null || ability == null
                || fieldVisualAdapter == null || terrainPlacementAdapter == null || fieldRegistrar == null) {
            return Result.none();
        }

        if (FieldRuntimeSpecs.isPersistentField(ability)) {
            return Result.none();
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return Result.none();
        }

        Store<EntityStore> store = playerRef.getStore();
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
            return Result.none();
        }

        String terrainEffect = lower(ability.getTerrainEffect());
        String abilityId = lower(ability.getId());
        List<Vector3d> centers = new ArrayList<>();
        Vector3d forward = currentForward(transform.getTransform().getDirection());
        Vector3d lineDirection = rotateAroundY(new Vector3d(forward.x, 0.0, forward.z), 90.0);
        double radius = Math.max(1.8, ability.getRadius() > 0 ? ability.getRadius() : 2.75);
        double halfWidth = Math.max(1.2, ability.getWidth() > 0 ? ability.getWidth() / 2.0 : radius);
        double thickness = Math.max(1.1, Math.min(radius, 2.5));
        double durationSeconds = Math.max(2.0, ability.getDurationSeconds() > 0 ? ability.getDurationSeconds() : 3.0);
        boolean followOwner = false;
        boolean useFieldVisualProxy = true;
        String summary;

        if (TerrainRuntimeSpecs.shouldCreateMovementTrail(ability, movementApplied, startPosition, endPosition)) {
            centers.addAll(TerrainRuntimeSpecs.buildTrailCenters(
                    startPosition,
                    endPosition,
                    TerrainRuntimeSpecs.trailNodeCount(ability)));
            radius = TerrainRuntimeSpecs.trailRadius(ability);
            halfWidth = radius;
            thickness = Math.max(1.0, radius * 0.8);
            // No visual proxies for movement trails: the dash runtime renders the
            // trail with world-space particle bursts, and the proxy cohort (4
            // nodes x row of Spark_Living spawn+model-strip packets in one tick)
            // is the prime suspect for the client NPE (2026-07-18 bisection:
            // single-field ground zones with proxies do NOT crash; every
            // trail-family cast did).
            useFieldVisualProxy = false;
            summary = humanize(terrainEffect.isBlank() ? abilityId : terrainEffect) + " trail";
        } else if (TerrainRuntimeSpecs.shouldCreatePersonalAuraField(ability)) {
            centers.add(new Vector3d(transform.getTransform().getPosition()));
            radius = TerrainRuntimeSpecs.auraRadius(ability);
            halfWidth = radius;
            thickness = Math.max(1.1, radius * 0.9);
            followOwner = true;
            useFieldVisualProxy = false;
            summary = humanize(terrainEffect.isBlank() ? abilityId : terrainEffect) + " aura";
        } else {
            return Result.none();
        }

        long now = System.currentTimeMillis();
        long activateAtMillis = now;
        long expireAtMillis = now + (long) (durationSeconds * 1000);
        boolean created = false;
        for (Vector3d center : centers) {
            FieldVisualRuntime visual = useFieldVisualProxy
                    ? fieldVisualAdapter.spawn(
                    runtimePlayer,
                    player.getPlayerClass(),
                    style.getId(),
                    ability,
                    center,
                    normalize(lineDirection),
                    halfWidth,
                    activateAtMillis,
                    expireAtMillis,
                    AbilityRuntimeEffects.fieldVisualEffectId(player.getPlayerClass(), style.getId(), ability)
            )
                    : FieldVisualRuntime.none();
            fieldRegistrar.register(
                    player.getPlayerId(),
                    playerRef,
                    player.getPlayerClass(),
                    style.getId(),
                    ability,
                    center,
                    normalize(forward),
                    normalize(lineDirection),
                    radius,
                    halfWidth,
                    thickness,
                    activateAtMillis,
                    expireAtMillis,
                    followOwner,
                    visual
            );
            terrainPlacementAdapter.placeSupplementalSurfaceCue(runtimePlayer.getWorld(), ability, center, expireAtMillis);
            created = true;
        }

        if (!created) {
            return Result.none();
        }

        String detail = centers.size() > 1
                ? centers.size() + " nodes"
                : "radius " + formatDistance(radius) + "m";
        return new Result(true,
                summary + " | " + detail + " | "
                        + AbilityPresentation.formatDecimal(durationSeconds) + "s");
    }

    private static Vector3d currentForward(Vector3d direction) {
        if (direction == null || !direction.isFinite()) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        Vector3d forward = new Vector3d(direction.x, 0.0, direction.z);
        if (!forward.isFinite() || forward.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        forward.normalize();
        return forward;
    }

    private static Vector3d rotateAroundY(Vector3d vector, double degrees) {
        if (Math.abs(degrees) < 0.001) {
            return normalize(vector);
        }

        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return normalize(new Vector3d(
                (vector.x * cos) - (vector.z * sin),
                vector.y,
                (vector.x * sin) + (vector.z * cos)
        ));
    }

    private static Vector3d normalize(Vector3d vector) {
        Vector3d normalized = vector == null ? new Vector3d(0.0, 0.0, 1.0) : new Vector3d(vector);
        if (!normalized.isFinite() || normalized.length() < 0.0001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        normalized.normalize();
        return normalized;
    }

    private static String humanize(String rawValue) {
        return rawValue == null ? "" : rawValue.replace('_', ' ');
    }

    private static String formatDistance(double distance) {
        return String.format(Locale.US, "%.1f", distance);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    @FunctionalInterface
    public interface FieldRegistrar {
        void register(String ownerPlayerId,
                      Ref<EntityStore> ownerRef,
                      String classId,
                      String styleId,
                      AbilityData ability,
                      Vector3d center,
                      Vector3d forwardDirection,
                      Vector3d lineDirection,
                      double radius,
                      double halfWidth,
                      double thickness,
                      long activateAtMillis,
                      long expireAtMillis,
                      boolean followOwner,
                      FieldVisualRuntime visual);
    }

    public record Result(boolean activated, String summary) {
        public Result {
            summary = summary == null ? "" : summary;
        }

        public static Result none() {
            return new Result(false, "");
        }
    }
}
