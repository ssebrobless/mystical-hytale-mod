package com.motm.runtime.player;

import com.hypixel.hytale.math.vector.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RuntimePlayerViewTest {

    @Test
    void normalizeHorizontalDropsVerticalComponent() {
        Vector3d normalized = RuntimePlayerView.normalizeHorizontal(new Vector3d(3.0, 9.0, 4.0));

        assertEquals(0.6, normalized.x, 0.0001);
        assertEquals(0.0, normalized.y, 0.0001);
        assertEquals(0.8, normalized.z, 0.0001);
    }

    @Test
    void normalizeHorizontalUsesForwardFallbackForInvalidInput() {
        Vector3d normalized = RuntimePlayerView.normalizeHorizontal(new Vector3d(0.0, 5.0, 0.0));

        assertEquals(0.0, normalized.x, 0.0001);
        assertEquals(0.0, normalized.y, 0.0001);
        assertEquals(1.0, normalized.z, 0.0001);
    }

    @Test
    void formatVectorPreservesDevCommandShape() {
        assertEquals("(unknown)", RuntimePlayerView.formatVector(null));
        assertEquals("(1.23, 5.68, -9.10)", RuntimePlayerView.formatVector(new Vector3d(1.234, 5.678, -9.101)));
    }
}
