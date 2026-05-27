package com.motm.runtime.ability.field;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuriedVictimTest {

    @Test
    void ownsBuriedVictimExpiryState() {
        BuriedVictim victim = new BuriedVictim(null, 1.25f, 3_000L);

        assertEquals(1.25f, victim.originalScale());
        assertEquals(3_000L, victim.expireAtMillis());
        assertTrue(victim.expired(3_000L));
    }
}
