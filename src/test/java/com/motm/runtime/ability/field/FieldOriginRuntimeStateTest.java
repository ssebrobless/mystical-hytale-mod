package com.motm.runtime.ability.field;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldOriginRuntimeStateTest {

    @Test
    void reusesImplausibleIronWallOriginWithinShortWindow() {
        FieldOriginRuntimeState state = new FieldOriginRuntimeState();
        Vector3d first = new Vector3d(1.0, 2.0, 3.0);
        Vector3d jump = new Vector3d(40.0, 2.0, 3.0);

        FieldOriginRuntimeState.StableOrigin initial = state.resolveIronWallOrigin("player", first, 1000L);
        FieldOriginRuntimeState.StableOrigin resolved = state.resolveIronWallOrigin("player", jump, 2000L);

        assertFalse(initial.reusedPrevious());
        assertTrue(resolved.reusedPrevious());
        assertEquals(1.0, resolved.origin().x, 0.0001);
        assertEquals(40.0, resolved.rejectedOrigin().x, 0.0001);
        assertNotSame(first, resolved.origin());
    }

    @Test
    void acceptsIronWallOriginAfterWindowExpires() {
        FieldOriginRuntimeState state = new FieldOriginRuntimeState();
        state.resolveIronWallOrigin("player", new Vector3d(1.0, 2.0, 3.0), 1000L);

        FieldOriginRuntimeState.StableOrigin resolved = state.resolveIronWallOrigin(
                "player",
                new Vector3d(40.0, 2.0, 3.0),
                6000L);

        assertFalse(resolved.reusedPrevious());
        assertEquals(40.0, resolved.origin().x, 0.0001);
    }

    @Test
    void ownsCasterCenteredOriginAndClear() {
        FieldOriginRuntimeState state = new FieldOriginRuntimeState();
        state.resolveCasterCenteredOrigin("player", new Vector3d(1.0, 2.0, 3.0), 1000L);

        assertEquals(1, state.casterCenteredOriginCount());
        state.clearCasterCenteredOrigin("player");
        assertEquals(0, state.casterCenteredOriginCount());
        assertEquals(0, state.ironWallOriginCount());
    }
}
