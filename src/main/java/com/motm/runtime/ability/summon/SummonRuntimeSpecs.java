package com.motm.runtime.ability.summon;

import com.motm.model.AbilityData;

import java.util.List;

public final class SummonRuntimeSpecs {

    public static final long THINK_INTERVAL_MS = 450L;
    public static final long HATCHLING_DELAY_MS = 4_000L;

    private SummonRuntimeSpecs() {
    }

    public static SummonRuntimeSpec resolve(AbilityData ability) {
        String summonName = summonName(ability);
        String role = role(summonName);
        return new SummonRuntimeSpec(
                role,
                ranged(role),
                attackRange(role),
                chaseRange(ability),
                attackIntervalMillis(role),
                "hatchling".equals(role) ? HATCHLING_DELAY_MS : 0L,
                baseDamageMultiplier(role),
                attackToken(summonName, role),
                modelId(summonName),
                appearanceModelId(summonName)
        );
    }

    public static double baseDamage(double rawDamage, SummonRuntimeSpec spec) {
        return rawDamage * (spec != null ? spec.baseDamageMultiplier() : 1.0);
    }

    public static double rawBaseDamage(double damagePercent, int playerLevel, double abilityPowerMultiplier) {
        double damage = damagePercent > 0
                ? damagePercent * (0.55 + (playerLevel * 0.035))
                : 5.0 + (playerLevel * 0.75);
        return damage * abilityPowerMultiplier;
    }

    public static List<String> modelIds(AbilityData ability) {
        String summonName = summonName(ability);
        return switch (summonName) {
            case "crawler_void", "void_spawn" -> List.of("Crawler_Void", "Crawler_Void", "Crawler_Void");
            case "scarak_egg" -> List.of("Scarak_Seeker", "Scarak_Fighter", "Scarak_Fighter");
            default -> {
                String modelId = modelId(summonName);
                yield modelId == null || modelId.isBlank() ? List.of() : List.of(modelId);
            }
        };
    }

    private static String role(String summonName) {
        return switch (summonName) {
            case "frosty_golem", "yeti_frosty" -> "tank";
            case "snow_imp", "snowman_imp" -> "ground_snowman";
            case "skeleton_minion" -> "skirmisher";
            case "crawler_void", "void_spawn" -> "caster";
            case "swamp_monster", "crocodile_swamp_monster", "snapjaw_abyssal", "treant_sapling", "locust_queen" -> "bruiser";
            case "shadow_clone" -> "clone";
            case "scarak_egg" -> "hatchling";
            default -> "bruiser";
        };
    }

    private static boolean ranged(String role) {
        return switch (role) {
            case "skirmisher", "artillery", "caster", "swarm", "clone" -> true;
            default -> false;
        };
    }

    private static double attackRange(String role) {
        return switch (role) {
            case "tank" -> 2.8;
            case "ground_snowman" -> 2.25;
            case "skirmisher", "clone" -> 7.5;
            case "artillery", "caster", "swarm" -> 9.5;
            default -> 3.2;
        };
    }

    private static double chaseRange(AbilityData ability) {
        return Math.max(10.0, ability != null && ability.getRange() > 0 ? ability.getRange() + 4.0 : 12.0);
    }

    private static long attackIntervalMillis(String role) {
        return switch (role) {
            case "tank" -> 1700L;
            case "ground_snowman" -> 1850L;
            case "clone" -> 900L;
            case "swarm" -> 1100L;
            case "artillery", "caster" -> 1400L;
            default -> 1250L;
        };
    }

    private static double baseDamageMultiplier(String role) {
        return switch (role) {
            case "tank" -> 0.75;
            case "clone" -> 1.25;
            case "swarm" -> 0.9;
            case "caster" -> 1.1;
            default -> 1.0;
        };
    }

    private static String attackToken(String summonName, String role) {
        if (!summonName.isBlank()) {
            return switch (summonName) {
                case "frosty_golem", "yeti_frosty" -> "root";
                case "snow_imp", "snowman_imp" -> "slow";
                case "swamp_monster", "crocodile_swamp_monster", "snapjaw_abyssal", "treant_sapling" -> "root";
                case "crawler_void", "void_spawn" -> "vulnerability";
                case "locust_queen", "scarak_egg" -> "dot";
                case "shadow_clone" -> "vulnerability";
                default -> roleAttackToken(role);
            };
        }
        return roleAttackToken(role);
    }

    private static String roleAttackToken(String role) {
        return switch (role) {
            case "tank", "skirmisher" -> "slow";
            case "ground_snowman" -> "slow";
            case "caster" -> "curse";
            case "swarm", "hatchling" -> "dot";
            case "clone" -> "vulnerability";
            default -> "root";
        };
    }

    private static String modelId(String summonName) {
        return switch (summonName) {
            case "treant_sapling" -> "Spirit_Root";
            case "snow_imp", "snowman_imp" -> "MOTM_Summon_Driver";
            case "frosty_golem", "yeti_frosty" -> "Tamed_Frosty";
            case "swamp_monster" -> "Frog_Green";
            case "crocodile_swamp_monster" -> "Crocodile";
            case "snapjaw_abyssal" -> "Snapjaw";
            case "skeleton_minion", "shadow_clone" -> "Shadow_Knight";
            case "void_spawn", "crawler_void" -> "Crawler_Void";
            case "scarak_egg" -> "Scarak_Fighter";
            case "locust_queen" -> "Scarak_Fighter";
            default -> null;
        };
    }

    private static String appearanceModelId(String summonName) {
        return switch (summonName) {
            case "snow_imp", "snowman_imp" -> "Snow_Imp_Snowman";
            case "frosty_golem", "yeti_frosty" -> "Yeti";
            case "treant_sapling" -> "Spirit_Root";
            case "swamp_monster" -> "Frog_Green";
            case "crocodile_swamp_monster" -> "Crocodile";
            case "snapjaw_abyssal" -> "Snapjaw";
            case "skeleton_minion", "shadow_clone" -> "Shadow_Knight";
            case "void_spawn", "crawler_void" -> "Crawler_Void";
            case "scarak_egg" -> "Scarak_Fighter";
            case "locust_queen" -> "Scarak_Fighter";
            default -> null;
        };
    }

    private static String summonName(AbilityData ability) {
        return ability == null ? "" : lower(ability.getSummonName());
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
