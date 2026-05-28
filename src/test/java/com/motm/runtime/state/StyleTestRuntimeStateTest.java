package com.motm.runtime.state;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StyleTestRuntimeStateTest {

    @Test
    void tracksActiveStyleTestsAndAdvancesByPlayer() {
        StyleTestRuntimeState state = new StyleTestRuntimeState();
        ActiveStyleTest test = new ActiveStyleTest(
                "player",
                "terra",
                "magma",
                "Magma",
                List.of("one", "two"),
                0,
                1000L
        );

        state.start(test);
        state.advance("player", 2000L);

        assertEquals(1, state.activeCount());
        assertEquals(1, state.get("player").nextAbilityIndex());
        assertEquals(2000L, state.get("player").nextActionAtMs());
        assertEquals(test.styleName(), state.stop("player").styleName());
        assertEquals(0, state.activeCount());
    }

    @Test
    void clearPlayerRemovesTargetsAndActiveTest() {
        StyleTestRuntimeState state = new StyleTestRuntimeState();
        state.putTargets("player", List.of());
        state.start(new ActiveStyleTest("player", "class", "style", "Style", List.of(), 0, 0L));

        state.clearPlayer("player");

        assertEquals(0, state.targetOwnerCount());
        assertNull(state.get("player"));
    }
}
