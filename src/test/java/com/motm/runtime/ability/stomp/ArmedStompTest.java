package com.motm.runtime.ability.stomp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArmedStompTest {

    @Test
    void ownsStompObservationState() {
        ArmedStomp stomp = new ArmedStomp(
                "player",
                null,
                null,
                null,
                "trace",
                100L,
                5_000L,
                10.0,
                false
        );

        ArmedStomp updated = stomp.withObservation(11.0, true);

        assertEquals("player", updated.playerId());
        assertEquals("trace", updated.traceId());
        assertEquals(100L, updated.armedAtMillis());
        assertEquals(5_000L, updated.expireAtMillis());
        assertEquals(11.0, updated.previousY(), 0.0001);
        assertTrue(updated.wasAirborne());
        assertTrue(updated.expired(5_000L));
    }
}
