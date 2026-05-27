package com.motm.runtime.ability.summon;

import com.google.gson.Gson;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummonActivationRuntimeTest {
    private static final Gson GSON = new Gson();

    private final SummonActivationRuntime runtime = new SummonActivationRuntime();

    @Test
    void createsAwakeSummonFromResolvedProfileAndRawDamage() {
        AbilityData ability = ability("snow_imp", "snow_imp");

        ActiveSummon summon = runtime.create(
                "player",
                new TestRef(),
                new TestRef(),
                "hydro",
                "snow",
                ability,
                1_000L,
                8_000L,
                20.0
        );

        assertEquals("player", summon.ownerPlayerId());
        assertEquals("hydro", summon.classId());
        assertEquals("snow", summon.styleId());
        assertEquals("skirmisher", summon.role());
        assertTrue(summon.ranged());
        assertTrue(summon.awakened());
        assertEquals(1_000L, summon.hatchAtMillis());
        assertEquals(1_000L, summon.nextThinkAtMillis());
        assertEquals(1_000L, summon.nextAttackAtMillis());
        assertEquals(8_000L, summon.expireAtMillis());
        assertEquals(20.0, summon.baseDamage(), 0.0001);
        assertEquals("slow", summon.attackToken());
    }

    @Test
    void createsSleepingHatchlingAndAppliesDamageMultiplier() {
        ActiveSummon summon = runtime.create(
                "player",
                new TestRef(),
                new TestRef(),
                "scaraks",
                "egg",
                ability("scarak_egg", "scarak_egg"),
                2_000L,
                12_000L,
                10.0
        );

        assertFalse(summon.awakened());
        assertEquals(2_000L + SummonRuntimeSpecs.HATCHLING_DELAY_MS, summon.hatchAtMillis());
        assertEquals("hatchling", summon.role());
        assertEquals(10.0, summon.baseDamage(), 0.0001);
    }

    @Test
    void returnsNullWhenRequiredInputsAreMissing() {
        AbilityData ability = ability("snow_imp", "snow_imp");

        assertNull(runtime.create(null, new TestRef(), new TestRef(), "hydro", "snow", ability,
                1_000L, 8_000L, 20.0));
        assertNull(runtime.create("player", new InvalidRef(), new TestRef(), "hydro", "snow", ability,
                1_000L, 8_000L, 20.0));
        assertNull(runtime.create("player", new TestRef(), new InvalidRef(), "hydro", "snow", ability,
                1_000L, 8_000L, 20.0));
        assertNull(runtime.create("player", new TestRef(), new TestRef(), "hydro", "snow", null,
                1_000L, 8_000L, 20.0));
    }

    private static AbilityData ability(String id, String summonName) {
        return GSON.fromJson("""
                {
                  "id": "%s",
                  "cast_type": "summon",
                  "summon_name": "%s"
                }
                """.formatted(id, summonName), AbilityData.class);
    }

    private static final class TestRef extends Ref<EntityStore> {
        private TestRef() {
            super(null, 1);
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    private static final class InvalidRef extends Ref<EntityStore> {
        private InvalidRef() {
            super(null, 1);
        }

        @Override
        public boolean isValid() {
            return false;
        }
    }
}
