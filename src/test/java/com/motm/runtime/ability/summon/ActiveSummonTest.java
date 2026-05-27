package com.motm.runtime.ability.summon;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveSummonTest {

    private static final Gson GSON = new Gson();

    @Test
    void ownsSummonStateAndSchedulesRuntimeActions() {
        AbilityData ability = ability("""
                {
                  "id": "shadow_clone",
                  "cast_type": "summon",
                  "summon_name": "shadow_clone"
                }
                """);
        SummonRuntimeSpec spec = SummonRuntimeSpecs.resolve(ability);
        ActiveSummon summon = new ActiveSummon(
                "player",
                null,
                null,
                "corruptus",
                "shadow",
                ability,
                spec,
                1000L,
                10_000L,
                1200L,
                2000L,
                0L,
                33.75,
                null,
                0L,
                true
        );

        assertEquals("player", summon.ownerPlayerId());
        assertEquals("corruptus", summon.classId());
        assertEquals("shadow", summon.styleId());
        assertEquals("clone", summon.role());
        assertTrue(summon.ranged());
        assertEquals(7.5, summon.attackRange(), 0.0001);
        assertEquals(33.75, summon.baseDamage(), 0.0001);
        assertEquals("vulnerability", summon.attackToken());

        summon.extend(500L);
        summon.extendBuffUntil(2600L);
        summon.commandAttackSoon(2000L, 150L);
        summon.scheduleNextThink(3000L);

        assertEquals(10_500L, summon.expireAtMillis());
        assertEquals(2600L, summon.buffExpireAtMillis());
        assertEquals(2000L, summon.nextAttackAtMillis());
        assertEquals(3000L + SummonRuntimeSpecs.THINK_INTERVAL_MS, summon.nextThinkAtMillis());
    }

    @Test
    void awakensAndSchedulesBuffedOrNormalAttacks() {
        ActiveSummon summon = new ActiveSummon(
                "player",
                null,
                null,
                "hydro",
                "snow",
                ability("""
                        {
                          "id": "snow_imp",
                          "cast_type": "summon",
                          "summon_name": "snow_imp"
                        }
                        """),
                SummonRuntimeSpecs.resolve(ability("""
                        {
                          "id": "snow_imp",
                          "cast_type": "summon",
                          "summon_name": "snow_imp"
                        }
                        """)),
                3000L,
                10_000L,
                0L,
                9000L,
                0L,
                12.0,
                null,
                0L,
                false
        );

        assertFalse(summon.awakened());
        summon.awaken(4000L);

        assertTrue(summon.awakened());
        assertEquals(4200L, summon.nextAttackAtMillis());
        assertEquals(5800L, summon.buffExpireAtMillis());
        assertTrue(summon.nowWithinBuffWindow(5000L));

        summon.scheduleNextAttack(5000L);
        assertEquals(5937L, summon.nextAttackAtMillis());
        summon.scheduleNextAttack(7000L);
        assertEquals(8250L, summon.nextAttackAtMillis());
    }

    @Test
    void cloneExpiresShortlyAfterStrike() {
        ActiveSummon clone = new ActiveSummon(
                "player",
                null,
                null,
                "corruptus",
                "shadow",
                ability("""
                        {
                          "id": "shadow_clone",
                          "cast_type": "summon",
                          "summon_name": "shadow_clone"
                        }
                        """),
                SummonRuntimeSpecs.resolve(ability("""
                        {
                          "id": "shadow_clone",
                          "cast_type": "summon",
                          "summon_name": "shadow_clone"
                        }
                        """)),
                0L,
                10_000L,
                0L,
                0L,
                0L,
                1.0,
                null,
                0L,
                true
        );

        clone.expireCloneAfterStrike(2000L);
        assertEquals(2150L, clone.expireAtMillis());
    }

    private static AbilityData ability(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
