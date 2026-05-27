package com.motm.runtime.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellbookInputDebouncerTest {

    @Test
    void identifiesDuplicateInputsInsideWindow() {
        SpellbookInputDebouncer debouncer = new SpellbookInputDebouncer();

        assertFalse(debouncer.isDuplicate("player", 1, 1000L, 250L, 1000L));
        assertTrue(debouncer.isDuplicate("player", 1, 1100L, 250L, 1000L));
        assertFalse(debouncer.isDuplicate("player", 1, 1400L, 250L, 1000L));
    }

    @Test
    void clearPlayerRemovesOnlyThatPlayersDebounceState() {
        SpellbookInputDebouncer debouncer = new SpellbookInputDebouncer();
        debouncer.isDuplicate("player", 1, 1000L, 250L, 1000L);
        debouncer.isDuplicate("other", 1, 1000L, 250L, 1000L);

        debouncer.clearPlayer("player");

        assertFalse(debouncer.isDuplicate("player", 1, 1100L, 250L, 1000L));
        assertTrue(debouncer.isDuplicate("other", 1, 1100L, 250L, 1000L));
    }
}
