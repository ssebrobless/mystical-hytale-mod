package com.motm.runtime.ability.movement;

import com.motm.model.AbilityData;
import org.joml.Vector3d;

import java.util.Locale;
import java.util.Set;

public final class AbilityMovementRuntime {
    private static final Set<String> REPOSITION_ABILITIES = Set.of(
            "burrow",
            "tunnel",
            "dispersion",
            "shadow_step"
    );
    private static final double MIN_BURST_SPEED = 7.0;
    private static final double MAX_BURST_SPEED = 18.0;
    private static final double BURST_SPEED_PER_BLOCK = 2.4;

    public MovementPlan plan(AbilityData ability,
                             String castType,
                             Vector3d start,
                             Vector3d horizontalDirection,
                             double horizontalDistance,
                             double verticalDistance) {
        if (ability == null || start == null || horizontalDirection == null
                || horizontalDistance <= 0.0 && verticalDistance <= 0.0) {
            return MovementPlan.none();
        }

        Vector3d direction = normalizedHorizontal(horizontalDirection);
        Vector3d target = new Vector3d(start)
                .add(direction.x * Math.max(0.0, horizontalDistance), Math.max(0.0, verticalDistance),
                        direction.z * Math.max(0.0, horizontalDistance));

        String abilityId = lower(ability.getId());
        String normalizedCastType = lower(castType);
        if ("teleport".equals(normalizedCastType) || REPOSITION_ABILITIES.contains(abilityId)) {
            return MovementPlan.reposition(horizontalDistance, verticalDistance, target);
        }

        if (isBurstCast(normalizedCastType)) {
            double horizontalSpeed = clamp(horizontalDistance * BURST_SPEED_PER_BLOCK, MIN_BURST_SPEED, MAX_BURST_SPEED);
            Vector3d velocity = new Vector3d(
                    direction.x * horizontalSpeed,
                    verticalDistance > 0.0 ? Math.max(4.0, verticalDistance * 2.0) : Double.NaN,
                    direction.z * horizontalSpeed
            );
            return MovementPlan.burst(horizontalDistance, verticalDistance, target, velocity);
        }

        return MovementPlan.reposition(horizontalDistance, verticalDistance, target);
    }

    private static boolean isBurstCast(String castType) {
        return "dash".equals(castType)
                || "dash_buff".equals(castType)
                || "dash_strike".equals(castType)
                || "leap".equals(castType)
                || "dive_strike".equals(castType);
    }

    private static Vector3d normalizedHorizontal(Vector3d vector) {
        if (vector == null || !vector.isFinite()) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        Vector3d normalized = new Vector3d(vector.x, 0.0, vector.z);
        if (!normalized.isFinite() || normalized.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        normalized.normalize();
        return normalized;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public record MovementPlan(
            MovementMode mode,
            double horizontalDistance,
            double verticalDistance,
            Vector3d target,
            Vector3d velocity
    ) {
        public static MovementPlan none() {
            return new MovementPlan(MovementMode.NONE, 0.0, 0.0, null, null);
        }

        public static MovementPlan reposition(double horizontalDistance, double verticalDistance, Vector3d target) {
            return new MovementPlan(MovementMode.REPOSITION, horizontalDistance, verticalDistance,
                    target == null ? null : new Vector3d(target), null);
        }

        public static MovementPlan burst(double horizontalDistance,
                                         double verticalDistance,
                                         Vector3d target,
                                         Vector3d velocity) {
            return new MovementPlan(MovementMode.BURST, horizontalDistance, verticalDistance,
                    target == null ? null : new Vector3d(target),
                    velocity == null ? null : new Vector3d(velocity));
        }

        public boolean applied() {
            return mode != MovementMode.NONE;
        }
    }

    public enum MovementMode {
        NONE,
        BURST,
        REPOSITION
    }
}
