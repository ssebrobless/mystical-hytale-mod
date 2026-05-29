package com.motm.runtime.ability.projectile;

import com.motm.model.AbilityData;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolver for projectile runtime specs.
 *
 * Ability-id checks are allowed here because this is the profile-building edge
 * where authored content is normalized into capability data.
 */
public final class ProjectileRuntimeSpecs {

    private static final int DEFAULT_PROJECTILE_CLUSTER_COUNT = 3;
    private static final double DEFAULT_PROJECTILE_COLLISION_RADIUS = 0.9;
    private static final double DEFAULT_PROJECTILE_SPEED = 20.0;
    private static final double MAX_PROJECTILE_SPEED = 38.0;
    private static final double DEFAULT_PROJECTILE_TTL_SECONDS = 2.5;
    private static final double DEFAULT_IMPACT_RADIUS = 0.0;
    private static final long DEFAULT_VOLLEY_STAGGER_MS = 80L;
    private static final long DEFAULT_BURST_STAGGER_MS = 22L;
    private static final double VOLLEY_SPREAD_DEGREES = 8.0;
    private static final double BURST_SPREAD_DEGREES = 12.0;

    private ProjectileRuntimeSpecs() {
    }

    public static ProjectileRuntimeSpec resolve(AbilityData ability,
                                                String castType,
                                                double maxDistance,
                                                double ticksPerSecond) {
        String normalizedCastType = lower(castType != null ? castType : ability != null ? ability.getCastType() : null);
        int projectileCount = resolveProjectileCount(normalizedCastType, ability);
        double speedPerTick = resolveProjectileSpeedPerTick(ability, ticksPerSecond);
        double resolvedMaxDistance = Math.max(maxDistance, 4.0);
        double impactRadius = resolveProjectileImpactRadius(ability, normalizedCastType);
        double collisionRadius = resolveProjectileCollisionRadius(ability, normalizedCastType);
        double spreadDegrees = resolveProjectileSpreadDegrees(normalizedCastType, ability, projectileCount);
        long lifetimeMillis = resolveProjectileLifetimeMillis(ability, speedPerTick, resolvedMaxDistance, ticksPerSecond);
        List<Long> launchDelays = resolveLaunchDelays(normalizedCastType, ability, projectileCount);
        ProjectileTrajectoryProfile trajectoryProfile = resolveTrajectoryProfile(ability);
        boolean hideVisualProxyIdentityComponents = isMagmaSlingAbility(ability);

        return new ProjectileRuntimeSpec(
                projectileCount,
                speedPerTick,
                resolvedMaxDistance,
                impactRadius,
                collisionRadius,
                spreadDegrees,
                lifetimeMillis,
                launchDelays,
                trajectoryProfile,
                hideVisualProxyIdentityComponents
        );
    }

    private static int resolveProjectileCount(String castType, AbilityData ability) {
        String abilityId = lower(ability != null ? ability.getId() : null);
        String travelType = lower(ability != null ? ability.getTravelType() : null);
        return switch (castType) {
            case "projectile_volley" -> switch (abilityId) {
                case "bullet_storm" -> 6;
                case "razor_wind" -> 5;
                case "frozen_needles", "cacti_cluster" -> 5;
                case "debris" -> 4;
                default -> travelType.contains("storm") ? 5 : DEFAULT_PROJECTILE_CLUSTER_COUNT + 1;
            };
            case "projectile_burst" -> switch (abilityId) {
                case "splash", "scald", "hellfire" -> 4;
                default -> DEFAULT_PROJECTILE_CLUSTER_COUNT;
            };
            default -> 1;
        };
    }

    private static double resolveProjectileSpeedPerTick(AbilityData ability, double ticksPerSecond) {
        double speedPerSecond = ability != null && ability.getProjectileSpeed() > 0
                ? ability.getProjectileSpeed()
                : DEFAULT_PROJECTILE_SPEED;
        return clamp(speedPerSecond, 6.0, MAX_PROJECTILE_SPEED) / ticksPerSecond;
    }

    private static double resolveProjectileImpactRadius(AbilityData ability, String castType) {
        if (isMagmaSlingAbility(ability)) {
            return 2.0;
        }
        if (ability != null && ability.getRadius() > 0) {
            return ability.getRadius();
        }

        return switch (castType) {
            case "projectile_burst", "wave_line" -> 2.25;
            case "projectile_volley" -> 0.0;
            default -> DEFAULT_IMPACT_RADIUS;
        };
    }

    private static double resolveProjectileCollisionRadius(AbilityData ability, String castType) {
        if (isMagmaSlingAbility(ability)) {
            return 1.8;
        }
        if (ability != null && ability.getWidth() > 0) {
            return Math.max(DEFAULT_PROJECTILE_COLLISION_RADIUS, ability.getWidth() / 3.5);
        }

        return switch (castType) {
            case "wave_line" -> 1.4;
            case "projectile_burst", "projectile_volley" -> 1.0;
            default -> DEFAULT_PROJECTILE_COLLISION_RADIUS;
        };
    }

    private static double resolveProjectileSpreadDegrees(String castType,
                                                         AbilityData ability,
                                                         int projectileCount) {
        if (projectileCount <= 1) {
            return 0.0;
        }

        String abilityId = lower(ability != null ? ability.getId() : null);
        return switch (castType) {
            case "projectile_burst" -> switch (abilityId) {
                case "splash" -> 13.0;
                case "scald" -> 11.5;
                case "hellfire" -> 12.5;
                default -> BURST_SPREAD_DEGREES;
            };
            case "projectile_volley" -> switch (abilityId) {
                case "bullet_storm" -> 4.5;
                case "razor_wind" -> 3.5;
                case "frozen_needles" -> 5.0;
                case "cacti_cluster" -> 6.5;
                case "debris" -> 7.5;
                default -> VOLLEY_SPREAD_DEGREES;
            };
            default -> 0.0;
        };
    }

    private static List<Long> resolveLaunchDelays(String castType,
                                                  AbilityData ability,
                                                  int projectileCount) {
        List<Long> delays = new ArrayList<>(Math.max(projectileCount, 0));
        String abilityId = lower(ability != null ? ability.getId() : null);
        for (int index = 0; index < projectileCount; index++) {
            long delay = switch (castType) {
                case "projectile_volley" -> switch (abilityId) {
                    case "bullet_storm" -> index * 65L;
                    case "razor_wind" -> index * 45L;
                    case "frozen_needles" -> index * 55L;
                    case "debris" -> index * 90L;
                    default -> index * DEFAULT_VOLLEY_STAGGER_MS;
                };
                case "projectile_burst" -> switch (abilityId) {
                    case "hellfire" -> index * 35L;
                    case "splash" -> index * 28L;
                    default -> index * DEFAULT_BURST_STAGGER_MS;
                };
                default -> 0L;
            };
            delays.add(delay);
        }
        return List.copyOf(delays);
    }

    private static long resolveProjectileLifetimeMillis(AbilityData ability,
                                                        double speedPerTick,
                                                        double maxDistance,
                                                        double ticksPerSecond) {
        double travelSeconds = Math.max(
                DEFAULT_PROJECTILE_TTL_SECONDS,
                maxDistance / Math.max(0.1, speedPerTick * ticksPerSecond)
        );
        if (ability != null && ability.getDurationSeconds() > 0) {
            travelSeconds = Math.max(travelSeconds, Math.min(ability.getDurationSeconds(), 8.0));
        }
        return (long) (travelSeconds * 1000);
    }

    private static ProjectileTrajectoryProfile resolveTrajectoryProfile(AbilityData ability) {
        if (isMagmaSlingAbility(ability)) {
            return ProjectileTrajectoryProfile.magmaSling();
        }
        return ProjectileTrajectoryProfile.generic();
    }

    private static boolean isMagmaSlingAbility(AbilityData ability) {
        return ability != null && "magma_sling".equals(lower(ability.getId()));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String lower(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(java.util.Locale.ROOT);
    }
}
