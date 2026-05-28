package com.motm.runtime.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeCastRuntimeStateTest {

    @Test
    void tracksEnabledPlayersAndObservedHealth() {
        FreeCastRuntimeState state = new FreeCastRuntimeState();

        state.setEnabled("player", true);

        assertTrue(state.isEnabled("player"));
        assertEquals(1, state.enabledCount());
        assertNull(state.rememberObservedHealth("player", 20.0f));
        assertEquals(20.0f, state.rememberObservedHealth("player", 18.0f));

        state.setEnabled("player", false);

        assertFalse(state.isEnabled("player"));
        assertEquals(0, state.enabledCount());
        assertNull(state.rememberObservedHealth("", 12.0f));
    }

    @Test
    void clearPlayerRemovesAllState() {
        FreeCastRuntimeState state = new FreeCastRuntimeState();
        state.setEnabled("player", true);
        state.rememberObservedHealth("player", 20.0f);

        state.clearPlayer("player");

        assertFalse(state.isEnabled("player"));
        assertEquals(0, state.enabledCount());
    }
}
