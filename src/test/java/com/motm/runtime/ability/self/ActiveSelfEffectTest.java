package com.motm.runtime.ability.self;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveSelfEffectTest {

    @Test
    void ownsSelfEffectTiming() {
        ActiveSelfEffect effect = new ActiveSelfEffect(
                "player",
                null,
                "effect",
                5_000L,
                1_000L
        );

        assertEquals("player", effect.ownerPlayerId());
        assertEquals("effect", effect.effectId());
        assertFalse(effect.readyToApply(999L));
        assertTrue(effect.readyToApply(1_000L));
        assertTrue(effect.expired(5_000L));

        effect.scheduleNextApply(2_000L, 650L);

        assertEquals(2_650L, effect.nextApplyAtMillis());
    }
}
