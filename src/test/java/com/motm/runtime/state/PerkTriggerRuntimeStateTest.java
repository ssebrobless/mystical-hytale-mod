package com.motm.runtime.state;

import com.motm.model.PerkTriggerBinding;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkTriggerRuntimeStateTest {

    @Test
    void filtersBindingsByNormalizedType() {
        PerkTriggerRuntimeState state = new PerkTriggerRuntimeState();
        state.add("player", new PerkTriggerBinding("one", "ON_HIT", 1.0));
        state.add("player", new PerkTriggerBinding("two", "on_cast", 2.0));

        assertEquals("one", state.get("player", "on_hit").getFirst().perkId());
        assertEquals(1, state.get("player", " ON_HIT ").size());
    }

    @Test
    void clearRemovesPlayerBindings() {
        PerkTriggerRuntimeState state = new PerkTriggerRuntimeState();
        state.add("player", new PerkTriggerBinding("one", "on_hit", 1.0));

        state.clear("player");

        assertTrue(state.get("player", "on_hit").isEmpty());
    }
}
