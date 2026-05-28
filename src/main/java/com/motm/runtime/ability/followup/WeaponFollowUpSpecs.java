package com.motm.runtime.ability.followup;

import com.motm.model.AbilityData;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class WeaponFollowUpSpecs {

    private WeaponFollowUpSpecs() {
    }

    public static WeaponFollowUpSpec resolve(AbilityData ability) {
        if (!shouldArm(ability)) {
            return WeaponFollowUpSpec.none();
        }

        List<String> tokens = parseEffectTokens(ability.getEffect());
        String abilityId = lower(ability.getId());
        return new WeaponFollowUpSpec(
                true,
                resolveUses(abilityId, tokens),
                resolveFlatDamageBonus(abilityId, ability.getDamagePercent(), tokens),
                resolveRiderToken(abilityId),
                tokens.contains("lifesteal") ? 0.18 : 0.0,
                resolveShieldPercentOnHit(abilityId, ability.getShieldPercent()),
                resolveHealRatioOnHit(abilityId, tokens),
                resolveSplashRadius(abilityId),
                resolveSplashDamageRatio(abilityId),
                resolveSecondaryRiderToken(abilityId),
                resolveDamageMultiplierBonus(abilityId),
                "alloy_enhancement".equals(abilityId)
        );
    }

    private static boolean shouldArm(AbilityData ability) {
        if (ability == null) {
            return false;
        }
        String castType = lower(ability.getCastType());
        if (!"self_buff".equals(castType) && !"dash_buff".equals(castType)) {
            return false;
        }

        String abilityId = lower(ability.getId());
        if ("metal_coat".equals(abilityId) || "obsidian_skin".equals(abilityId)) {
            return false;
        }

        List<String> tokens = parseEffectTokens(ability.getEffect());
        return tokens.stream().anyMatch(token -> switch (token) {
            case "attack_buff", "damage_buff", "stealth", "lifesteal", "shield", "evasion", "speed", "self_burn" -> true;
            default -> false;
        }) || ability.getShieldPercent() > 0;
    }

    private static int resolveUses(String abilityId, List<String> tokens) {
        return switch (abilityId) {
            case "alloy_enhancement" -> 3;
            case "umbral_veil" -> 1;
            case "lapidary", "imbue_fortitude", "absorb" -> 2;
            case "battle_cry", "waverider", "river_rapids", "frolick", "refraction", "imbue_swiftness" -> 3;
            default -> {
                if (tokens.contains("damage_buff") || tokens.contains("stealth")) {
                    yield 1;
                }
                if (tokens.contains("attack_buff") || tokens.contains("speed")) {
                    yield 3;
                }
                yield 2;
            }
        };
    }

    private static double resolveDamageMultiplierBonus(String abilityId) {
        return switch (abilityId) {
            case "alloy_enhancement" -> 0.35;
            default -> 0.0;
        };
    }

    private static double resolveFlatDamageBonus(String abilityId, double damagePercent, List<String> tokens) {
        double bonus = 4.0
                + (damagePercent * 0.20)
                + (tokens.contains("attack_buff") ? 4.0 : 0.0)
                + (tokens.contains("damage_buff") ? 7.0 : 0.0);

        return switch (abilityId) {
            case "alloy_enhancement" -> bonus + 9.0;
            case "imbue_power" -> bonus + 8.0;
            case "battle_cry", "overheat", "river_rapids", "refraction" -> bonus + 4.0;
            case "waverider", "frolick", "imbue_swiftness" -> bonus + 2.0;
            default -> bonus;
        };
    }

    private static double resolveShieldPercentOnHit(String abilityId, double shieldPercent) {
        double base = shieldPercent > 0 ? Math.min(shieldPercent * 0.35, 12.0) : 0.0;
        return switch (abilityId) {
            case "lapidary" -> Math.max(base, 14.0);
            case "imbue_fortitude", "absorb" -> Math.max(base, 10.0);
            case "waverider" -> Math.max(base, 8.0);
            default -> base;
        };
    }

    private static double resolveHealRatioOnHit(String abilityId, List<String> tokens) {
        double base = tokens.contains("heal") ? 0.20 : 0.0;
        return switch (abilityId) {
            case "imbue_fortitude", "absorb" -> Math.max(base, 0.38);
            case "frolick" -> Math.max(base, 0.30);
            default -> base;
        };
    }

    private static double resolveSplashRadius(String abilityId) {
        return switch (abilityId) {
            case "battle_cry" -> 2.5;
            case "overheat" -> 2.6;
            case "river_rapids" -> 2.8;
            case "refraction" -> 4.5;
            default -> 0.0;
        };
    }

    private static double resolveSplashDamageRatio(String abilityId) {
        return switch (abilityId) {
            case "battle_cry" -> 0.35;
            case "overheat" -> 0.45;
            case "river_rapids" -> 0.30;
            case "refraction" -> 0.55;
            default -> 0.0;
        };
    }

    private static String resolveSecondaryRiderToken(String abilityId) {
        return switch (abilityId) {
            case "alloy_enhancement" -> "vulnerability";
            case "imbue_swiftness" -> "disoriented";
            case "refraction" -> "slow";
            case "frolick" -> "root";
            default -> null;
        };
    }

    private static String resolveRiderToken(String abilityId) {
        return switch (abilityId) {
            case "overheat" -> "burn";
            case "hidrosis", "smoke_form" -> "blind";
            case "battle_cry", "triceratops_form" -> "knockback";
            case "waverider" -> "slow";
            case "imbue_power", "refraction" -> "vulnerability";
            case "t_rex_form" -> "stun";
            default -> null;
        };
    }

    private static List<String> parseEffectTokens(String effect) {
        if (effect == null || effect.isBlank()) {
            return List.of();
        }
        return Arrays.stream(effect.toLowerCase(Locale.ROOT).split("\\+"))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .toList();
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
