package com.motm.runtime.ability;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbilityRuntimeMathTest {

    private static final Gson GSON = new Gson();

    @Test
    void resolvesMovementFromConfiguredValuesFallbacksAndCaps() {
        assertEquals(0.0, AbilityRuntimeMath.horizontalMovement(ability("""
                {"cast_type":"air_stall","dash_distance":12.0}
                """), "air_stall"));
        assertEquals(8.0, AbilityRuntimeMath.horizontalMovement(ability("""
                {"cast_type":"teleport"}
                """), "teleport"));
        assertEquals(12.0, AbilityRuntimeMath.horizontalMovement(ability("""
                {"cast_type":"dash","dash_distance":24.0}
                """), "dash"));

        assertEquals(2.5, AbilityRuntimeMath.verticalMovement(ability("""
                {"cast_type":"air_stall"}
                """), "air_stall"));
        assertEquals(6.0, AbilityRuntimeMath.verticalMovement(ability("""
                {"cast_type":"leap","launch_height":8.0}
                """), "leap"));
    }

    @Test
    void resolvesRangeFromPreferredAuthoredFields() {
        assertEquals(5.0, AbilityRuntimeMath.range(ability("""
                {"range":5.0,"max_range":9.0,"dash_distance":12.0}
                """)));
        assertEquals(9.0, AbilityRuntimeMath.range(ability("""
                {"max_range":9.0,"dash_distance":12.0}
                """)));
        assertEquals(12.0, AbilityRuntimeMath.range(ability("""
                {"dash_distance":12.0}
                """)));
        assertEquals(8.0, AbilityRuntimeMath.range(ability("{}")));
        assertEquals(8.0, AbilityRuntimeMath.range(null));
    }

    @Test
    void resolvesDamageAndCastTypeMultipliers() {
        PlayerData player = new PlayerData();
        player.setLevel(10);
        double abilityPowerMultiplier = 1.25;

        assertEquals(187.5, AbilityRuntimeMath.damageAmount(player, ability("""
                {"damage_percent":100.0}
                """), abilityPowerMultiplier));
        assertEquals(243.75, AbilityRuntimeMath.damageAmount(player, ability("""
                {"damage_percent":100.0,"cast_type":"execute"}
                """), abilityPowerMultiplier));
        assertEquals(140.625, AbilityRuntimeMath.damageAmount(player, ability("""
                {"damage_percent":100.0,"cast_type":"projectile_volley"}
                """), abilityPowerMultiplier));
        assertEquals(159.375, AbilityRuntimeMath.damageAmount(player, ability("""
                {"damage_percent":100.0,"cast_type":"chain"}
                """), abilityPowerMultiplier));
        assertEquals(0.0, AbilityRuntimeMath.damageAmount(player, ability("""
                {"damage_percent":0.0}
                """), abilityPowerMultiplier));
    }

    @Test
    void resolvesFieldPulseDamageAndPullPolicy() {
        PlayerData player = new PlayerData();
        player.setLevel(1);
        AbilityData sinkhole = ability("""
                {"damage_percent":100.0,"cast_type":"ground_zone","terrain_effect":"sinkhole"}
                """);
        AbilityData support = ability("""
                {"damage_percent":100.0,"cast_type":"support_zone"}
                """);

        assertEquals(32.64, AbilityRuntimeMath.fieldPulseDamage(player, sinkhole, 1.0), 0.0001);
        assertEquals(0.0, AbilityRuntimeMath.fieldPulseDamage(player, support, 1.0));
        assertEquals(1.65, AbilityRuntimeMath.pullStep(ability("""
                {"pull_force":3.0}
                """), 0.55, 0.75), 0.0001);
        assertEquals(5.5, AbilityRuntimeMath.pullStep(ability("""
                {"pull_force":99.0}
                """), 0.55, 0.75), 0.0001);
        assertEquals(0.75, AbilityRuntimeMath.pullStep(ability("{}"), 0.55, 0.75), 0.0001);
        assertEquals(0.35, AbilityRuntimeMath.fieldPullLift(ability("""
                {"travel_type":"funnel"}
                """)), 0.0001);
    }

    @Test
    void resolvesTargetSequenceDamageMultiplier() {
        AbilityData ability = ability("{}");

        assertEquals(1.0, AbilityRuntimeMath.targetSequenceDamageMultiplier(ability, "chain", 0));
        assertEquals(0.82, AbilityRuntimeMath.targetSequenceDamageMultiplier(ability, "chain", 1));
        assertEquals(0.67, AbilityRuntimeMath.targetSequenceDamageMultiplier(ability, "chain", 2));
        assertEquals(0.55, AbilityRuntimeMath.targetSequenceDamageMultiplier(ability, "chain", 3));
        assertEquals(0.88, AbilityRuntimeMath.targetSequenceDamageMultiplier(ability, "projectile_volley", 1));
        assertEquals(0.7, AbilityRuntimeMath.targetSequenceDamageMultiplier(ability, "projectile_volley", 8));
        assertEquals(1.0, AbilityRuntimeMath.targetSequenceDamageMultiplier(ability, "projectile", 3));
    }

    private static AbilityData ability(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
