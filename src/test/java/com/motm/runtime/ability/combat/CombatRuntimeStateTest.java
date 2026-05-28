package com.motm.runtime.ability.combat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatRuntimeStateTest {

    @Test
    void deduplicatesAbilityKillReports() {
        CombatRuntimeState state = new CombatRuntimeState();

        assertTrue(state.markAbilityKillReported("target"));
        assertFalse(state.markAbilityKillReported("target"));
        assertFalse(state.markAbilityKillReported(""));
        assertEquals(1, state.reportedKillCount());
    }

    @Test
    void tracksRecentShockAndExpiresOldEntries() {
        CombatRuntimeState state = new CombatRuntimeState();

        state.markShocked("target", 1000L);

        assertTrue(state.hasActiveOrRecentShock("target", 1300L, 500L));
        assertFalse(state.hasActiveOrRecentShock("target", 1700L, 500L));
        assertEquals(0, state.recentShockCount());
    }
}
