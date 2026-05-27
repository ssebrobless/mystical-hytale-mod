package com.motm.runtime.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TargetHealthRuntimeStateTest {

    @Test
    void remembersAndClearsLastAppliedHealth() {
        TargetHealthRuntimeState state = new TargetHealthRuntimeState();

        state.remember("player", 125.0);

        assertEquals(125.0, state.get("player"));
        state.clear("player");
        assertNull(state.get("player"));
    }
}
