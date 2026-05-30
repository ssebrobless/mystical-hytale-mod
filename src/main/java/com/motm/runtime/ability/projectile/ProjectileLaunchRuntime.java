package com.motm.runtime.ability.projectile;

import com.hypixel.hytale.component.Ref;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class ProjectileLaunchRuntime {

    public Result launch(String ownerPlayerId,
                         Ref<EntityStore> ownerRef,
                         String classId,
                         String styleId,
                         AbilityData ability,
                         String castType,
                         ProjectileRuntimeSpec spec,
                         Vector3d origin,
                         Vector3d direction,
                         double baseDamage,
                         long launchBaseTimeMillis,
                         double ticksPerSecond,
                         String traceId,
                         Hooks hooks) {
        if (ownerPlayerId == null || ownerRef == null || !ownerRef.isValid()
                || spec == null || origin == null || direction == null || hooks == null) {
            return Result.none();
        }

        List<ActiveProjectile> projectiles = new java.util.ArrayList<>(Math.max(0, spec.projectileCount()));
        for (int index = 0; index < spec.projectileCount(); index++) {
            double angleOffset = spec.projectileCount() == 1
                    ? 0.0
                    : (index - ((spec.projectileCount() - 1) / 2.0)) * spec.spreadDegrees();
            Vector3d projectileDirection = rotateAroundY(direction, angleOffset);
            long activateAtMillis = launchBaseTimeMillis + spec.launchDelayMillis(index);
            ProjectileVisualRuntime visual = hooks.spawnVisual(
                    origin,
                    projectileDirection,
                    activateAtMillis,
                    activateAtMillis + spec.lifetimeMillis(),
                    spec.hideVisualProxyIdentityComponents()
            );

            projectiles.add(new ActiveProjectile(
                    ownerPlayerId,
                    ownerRef,
                    classId,
                    styleId,
                    ability,
                    new Vector3d(origin),
                    projectileDirection,
                    spec.speedPerTick(),
                    spec.maxDistance(),
                    spec.impactRadius(),
                    spec.collisionRadius(),
                    spec.trajectoryProfile().ownerSelfClearanceDistance(),
                    activateAtMillis,
                    activateAtMillis + spec.lifetimeMillis(),
                    baseDamage,
                    new LinkedHashSet<>(),
                    visual == null ? null : visual.visualRef(),
                    visual == null ? null : visual.travelEffectId(),
                    visual == null ? activateAtMillis : visual.nextRefreshAtMillis(),
                    traceId
            ));
        }

        return new Result(projectiles, launchSummary(projectiles.size(), spec, castType, ticksPerSecond));
    }

    private static String launchSummary(int projectileCount,
                                        ProjectileRuntimeSpec spec,
                                        String castType,
                                        double ticksPerSecond) {
        if (projectileCount <= 0) {
            return "";
        }
        String label = projectileCount == 1 ? "projectile" : "projectiles";
        return "launched " + projectileCount + " " + label + " at "
                + formatDistance(spec.speedPerTick() * ticksPerSecond) + "m/s"
                + switch (lower(castType)) {
                    case "projectile_volley" -> " | volley cadence";
                    case "projectile_burst" -> " | burst spread";
                    default -> "";
                };
    }

    private static Vector3d rotateAroundY(Vector3d vector, double degrees) {
        if (Math.abs(degrees) < 0.001) {
            return normalize(vector);
        }

        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        Vector3d rotated = new Vector3d(
                (vector.x * cos) - (vector.z * sin),
                vector.y,
                (vector.x * sin) + (vector.z * cos)
        );
        return normalize(rotated);
    }

    private static Vector3d normalize(Vector3d vector) {
        Vector3d normalized = vector == null ? new Vector3d(0.0, 0.0, 1.0) : new Vector3d(vector);
        if (normalized.length() < 0.0001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        normalized.normalize();
        return normalized;
    }

    private static String formatDistance(double distance) {
        return String.format(Locale.US, "%.1f", distance);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public record Result(List<ActiveProjectile> projectiles, String summary) {
        public Result {
            projectiles = projectiles == null ? List.of() : List.copyOf(projectiles);
            summary = summary == null ? "" : summary;
        }

        public int launched() {
            return projectiles.size();
        }

        public static Result none() {
            return new Result(List.of(), "");
        }
    }

    public interface Hooks {
        ProjectileVisualRuntime spawnVisual(Vector3d origin,
                                            Vector3d direction,
                                            long activateAtMillis,
                                            long expireAtMillis,
                                            boolean hideIdentityComponents);
    }
}
