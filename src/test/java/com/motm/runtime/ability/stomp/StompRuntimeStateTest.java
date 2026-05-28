package com.motm.runtime.ability.stomp;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StompRuntimeStateTest {

    @Test
    void ownsArmedStompMapAndSnapshots() {
        StompRuntimeState state = new StompRuntimeState();
        ArmedStomp stomp = stomp("player", 1.0);

        state.arm("player", stomp);

        assertFalse(state.isEmpty());
        assertTrue(state.contains("player"));
        assertSame(stomp, state.get("player"));
        assertEquals(1, state.armedStomps().size());
    }

    @Test
    void replacesAndRemovesOnlyMatchingStomp() {
        StompRuntimeState state = new StompRuntimeState();
        ArmedStomp first = stomp("player", 1.0);
        ArmedStomp second = stomp("player", 2.0);
        ArmedStomp unrelated = stomp("other", 1.0);

        state.arm("player", first);
        state.arm("other", unrelated);

        assertTrue(state.replace("player", first, second));
        assertFalse(state.remove("player", first));
        assertTrue(state.remove("player", second));
        assertTrue(state.contains("other"));
        assertSame(unrelated, state.remove("other"));
        assertTrue(state.isEmpty());
    }

    private static ArmedStomp stomp(String playerId, double y) {
        return new ArmedStomp(playerId, null, null, null, null, 100L, 1000L, y, false);
    }
}
