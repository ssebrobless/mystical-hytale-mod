package com.motm.runtime.task;

import com.motm.runtime.MotmRuntimeTasks;
import com.motm.runtime.state.FreeCastRuntimeState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FreeCastCommandActionsTest {

    @Test
    void enablingFreeCastCancelsPendingInvulnerabilityClear() {
        FreeCastRuntimeState state = new FreeCastRuntimeState();
        MotmRuntimeTasks tasks = new MotmRuntimeTasks();
        FreeCastCommandActions actions = new FreeCastCommandActions(state, tasks);

        tasks.requestFreeCastInvulnerabilityClear("player-a");
        actions.setEnabled("player-a", true);

        assertTrue(actions.isEnabled("player-a"));
        assertTrue(tasks.pendingFreeCastInvulnerabilityClears().isEmpty());
    }

    @Test
    void disablingFreeCastRequestsInvulnerabilityClear() {
        FreeCastRuntimeState state = new FreeCastRuntimeState();
        MotmRuntimeTasks tasks = new MotmRuntimeTasks();
        FreeCastCommandActions actions = new FreeCastCommandActions(state, tasks);

        actions.setEnabled("player-a", true);
        actions.setEnabled("player-a", false);

        assertFalse(actions.isEnabled("player-a"));
        assertTrue(tasks.pendingFreeCastInvulnerabilityClears().contains("player-a"));
    }
}
