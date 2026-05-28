package com.motm.runtime.task;

import com.motm.runtime.MotmRuntimeTasks;
import com.motm.runtime.state.FreeCastRuntimeState;

/**
 * Command-facing access to free-cast runtime state and follow-up safety tasks.
 */
public final class FreeCastCommandActions {

    private final FreeCastRuntimeState freeCastState;
    private final MotmRuntimeTasks runtimeTasks;

    public FreeCastCommandActions(FreeCastRuntimeState freeCastState, MotmRuntimeTasks runtimeTasks) {
        this.freeCastState = freeCastState;
        this.runtimeTasks = runtimeTasks;
    }

    public boolean isEnabled(String playerId) {
        return freeCastState.isEnabled(playerId);
    }

    public void setEnabled(String playerId, boolean enabled) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }

        if (enabled) {
            freeCastState.setEnabled(playerId, true);
            runtimeTasks.cancelFreeCastInvulnerabilityClear(playerId);
        } else {
            freeCastState.setEnabled(playerId, false);
            runtimeTasks.requestFreeCastInvulnerabilityClear(playerId);
        }
    }
}
