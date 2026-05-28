package com.motm.runtime.ability.summon;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummonRuntimeSpecsTest {

    private static final Gson GSON = new Gson();

    @Test
    void resolvesNamedSummonProfiles() {
        SummonRuntimeSpec golem = SummonRuntimeSpecs.resolve(ability("""
                {
                  "id": "frosty_golem",
                  "cast_type": "summon",
                  "summon_name": "frosty_golem"
                }
                """));
        SummonRuntimeSpec imp = SummonRuntimeSpecs.resolve(ability("""
                {
                  "id": "snow_imp",
                  "cast_type": "summon",
                  "summon_name": "snow_imp"
                }
                """));
        SummonRuntimeSpec clone = SummonRuntimeSpecs.resolve(ability("""
                {
                  "id": "shadow_clone",
                  "cast_type": "summon",
                  "summon_name": "shadow_clone"
                }
                """));

        assertEquals("tank", golem.role());
        assertFalse(golem.ranged());
        assertEquals(2.8, golem.attackRange(), 0.0001);
        assertEquals("root", golem.attackToken());
        assertEquals("Golem_Crystal_Frost", golem.modelId());

        assertEquals("skirmisher", imp.role());
        assertTrue(imp.ranged());
        assertEquals(7.5, imp.attackRange(), 0.0001);
        assertEquals("slow", imp.attackToken());

        assertEquals("clone", clone.role());
        assertTrue(clone.ranged());
        assertEquals(900L, clone.attackIntervalMillis());
        assertEquals(1.25, clone.baseDamageMultiplier(), 0.0001);
        assertEquals("Shadow_Knight", clone.modelId());
    }

    @Test
    void resolvesHatchlingAndFallbackProfiles() {
        SummonRuntimeSpec hatchling = SummonRuntimeSpecs.resolve(ability("""
                {
                  "id": "scarak_egg",
                  "cast_type": "summon",
                  "summon_name": "scarak_egg"
                }
                """));
        SummonRuntimeSpec fallback = SummonRuntimeSpecs.resolve(ability("""
                {
                  "id": "custom_summon",
                  "cast_type": "summon",
                  "range": 20.0
                }
                """));

        assertEquals("hatchling", hatchling.role());
        assertEquals(SummonRuntimeSpecs.HATCHLING_DELAY_MS, hatchling.hatchDelayMillis());
        assertEquals("dot", hatchling.attackToken());
        assertEquals("Scarak_Fighter", hatchling.modelId());

        assertEquals("bruiser", fallback.role());
        assertEquals(24.0, fallback.chaseRange(), 0.0001);
        assertEquals("root", fallback.attackToken());
        assertNull(fallback.modelId());
    }

    @Test
    void computesSummonDamageInputs() {
        double raw = SummonRuntimeSpecs.rawBaseDamage(20.0, 10, 1.5);
        assertEquals(27.0, raw, 0.0001);
        assertEquals(20.25, SummonRuntimeSpecs.baseDamage(raw, SummonRuntimeSpecs.resolve(ability("""
                {
                  "id": "frosty_golem",
                  "cast_type": "summon",
                  "summon_name": "frosty_golem"
                }
                """))), 0.0001);
        assertEquals(33.75, SummonRuntimeSpecs.baseDamage(raw, SummonRuntimeSpecs.resolve(ability("""
                {
                  "id": "shadow_clone",
                  "cast_type": "summon",
                  "summon_name": "shadow_clone"
                }
                """))), 0.0001);
    }

    private static AbilityData ability(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
