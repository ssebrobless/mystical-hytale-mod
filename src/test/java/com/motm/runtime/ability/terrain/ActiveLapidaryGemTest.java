package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.math.vector.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveLapidaryGemTest {

    @Test
    void ownsGemStateAndDefensiveCenterCopy() {
        Vector3d center = new Vector3d(1.0, 2.0, 3.0);
        ActiveLapidaryGem gem = new ActiveLapidaryGem(
                "player",
                null,
                center,
                10.0,
                20.0,
                5_000L,
                "old"
        );

        assertEquals("player", gem.ownerPlayerId());
        assertEquals(10.0, gem.currentHp(), 0.0001);
        assertEquals(20.0, gem.maxHp(), 0.0001);
        assertTrue(gem.expired(5_000L));
        assertNotSame(center, gem.center());
        center.x = 99.0;
        assertEquals(1.0, gem.center().x, 0.0001);

        assertFalse(gem.updateHealthLabel(8.0, "old"));
        assertTrue(gem.updateHealthLabel(8.0, "new"));
        assertEquals(8.0, gem.currentHp(), 0.0001);
        assertEquals("new", gem.lastLabel());
    }
}
