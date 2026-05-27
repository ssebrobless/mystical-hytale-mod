package com.motm.runtime.ability.field;

import com.hypixel.hytale.math.vector.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecentFieldOriginTest {

    @Test
    void clonesMutablePositionAndChecksFreshnessWindow() {
        Vector3d position = new Vector3d(1.0, 2.0, 3.0);
        RecentFieldOrigin origin = new RecentFieldOrigin(position, 1_000L);

        assertNotSame(position, origin.position());
        position.x = 99.0;
        assertEquals(1.0, origin.position().x, 0.0001);
        assertTrue(origin.withinWindow(4_000L, 3_000L));
    }
}
