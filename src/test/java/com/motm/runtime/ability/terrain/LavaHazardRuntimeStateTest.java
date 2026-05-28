package com.motm.runtime.ability.terrain;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LavaHazardRuntimeStateTest {

    @Test
    void ownsMagmaHazardProtectionExpiry() {
        LavaHazardRuntimeState state = new LavaHazardRuntimeState();

        state.protectUntil("player", 2000L);

        assertTrue(state.isProtected("player", 1500L));
        assertFalse(state.isProtected("player", 2500L));
        assertNull(state.protectionUntil("player"));
    }

    @Test
    void ownsMovementBoostFlagsAndVelocityBoostCopies() {
        LavaHazardRuntimeState state = new LavaHazardRuntimeState();
        Vector3d boost = new Vector3d(1.0, 0.0, 2.0);

        state.markMovementBoosted("player");
        state.storeVelocityBoost("player", boost);
        boost.x = 9.0;

        Vector3d removed = state.removeVelocityBoost("player");

        assertTrue(state.isMovementBoosted("player"));
        assertTrue(state.consumeMovementBoosted("player"));
        assertFalse(state.consumeMovementBoosted("player"));
        assertNotSame(boost, removed);
        assertEquals(1.0, removed.x, 0.0001);
    }

    @Test
    void clearsAllPlayerState() {
        LavaHazardRuntimeState state = new LavaHazardRuntimeState();
        state.protectUntil("player", 2000L);
        state.markMovementBoosted("player");
        state.storeVelocityBoost("player", new Vector3d(1.0, 0.0, 2.0));

        state.clearPlayer("player");

        assertFalse(state.isMovementBoosted("player"));
        assertFalse(state.isProtected("player", 1500L));
        assertNull(state.removeVelocityBoost("player"));
    }
}
