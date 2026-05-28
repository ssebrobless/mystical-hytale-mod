package com.motm.runtime.ability;

import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.runtime.ability.field.FieldRuntimeSpecs;

import java.util.Locale;

/**
 * Owns pure ability runtime formulas that are shared by the temporary playback
 * facade and runtime-family adapters.
 */
public final class AbilityRuntimeMath {

    public static final double MAX_HORIZONTAL_MOVEMENT = 12.0;
    public static final double MAX_VERTICAL_MOVEMENT = 6.0;
    public static final double MAX_PULL_STEP_DISTANCE = 5.5;

    private AbilityRuntimeMath() {
    }

    public static double targetSequenceDamageMultiplier(AbilityData ability, String castType, int hitIndex) {
        if (ability == null || hitIndex <= 0) {
            return 1.0;
        }

        return switch (lower(castType)) {
            case "chain" -> switch (hitIndex) {
                case 1 -> 0.82;
                case 2 -> 0.67;
                default -> 0.55;
            };
            case "projectile_volley" -> Math.max(0.7, 1.0 - (0.12 * hitIndex));
            default -> 1.0;
        };
    }

    public static double horizontalMovement(AbilityData ability, String castType) {
        if (ability == null) {
            return 0.0;
        }

        String normalizedCastType = lower(castType);
        if ("air_stall".equals(normalizedCastType)) {
            return 0.0;
        }

        double configured = ability.getDashDistance() > 0 ? ability.getDashDistance() : ability.getRange();
        double fallback = switch (normalizedCastType) {
            case "teleport" -> 8.0;
            case "leap", "dive_strike" -> 6.0;
            case "dash_strike" -> 5.5;
            default -> 4.5;
        };

        double resolved = configured > 0 ? configured : fallback;
        return clamp(resolved, 0.0, MAX_HORIZONTAL_MOVEMENT);
    }

    public static double verticalMovement(AbilityData ability, String castType) {
        if (ability == null) {
            return 0.0;
        }

        double configured = ability.getLaunchHeight();
        double fallback = switch (lower(castType)) {
            case "air_stall" -> 2.5;
            case "leap", "dive_strike" -> 1.75;
            default -> 0.0;
        };

        double resolved = configured > 0 ? configured : fallback;
        return clamp(resolved, 0.0, MAX_VERTICAL_MOVEMENT);
    }

    public static double range(AbilityData ability) {
        if (ability == null) {
            return 8.0;
        }
        if (ability.getRange() > 0) {
            return ability.getRange();
        }
        if (ability.getMaxRange() > 0) {
            return ability.getMaxRange();
        }
        if (ability.getDashDistance() > 0) {
            return ability.getDashDistance();
        }
        return 8.0;
    }

    public static double fieldPulseDamage(PlayerData player,
                                          AbilityData ability,
                                          double abilityPowerMultiplier) {
        return FieldRuntimeSpecs.pulseDamage(damageAmount(player, ability, abilityPowerMultiplier), ability);
    }

    public static double pullStep(AbilityData ability, double scale, double minimumStep) {
        double configured = ability != null && ability.getPullForce() > 0
                ? ability.getPullForce()
                : minimumStep;
        return clamp(Math.max(minimumStep, configured * scale), minimumStep, MAX_PULL_STEP_DISTANCE);
    }

    public static double fieldPullLift(AbilityData ability) {
        return FieldRuntimeSpecs.pullLift(ability);
    }

    public static double damageAmount(PlayerData player,
                                      AbilityData ability,
                                      double abilityPowerMultiplier) {
        if (player == null || ability == null || ability.getDamagePercent() <= 0) {
            return 0.0;
        }

        double damage = ability.getDamagePercent() * (0.9 + (player.getLevel() * 0.06));
        damage *= abilityPowerMultiplier;
        return switch (lower(ability.getCastType())) {
            case "execute" -> damage * 1.3;
            case "projectile_volley" -> damage * 0.75;
            case "chain" -> damage * 0.85;
            default -> damage;
        };
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
