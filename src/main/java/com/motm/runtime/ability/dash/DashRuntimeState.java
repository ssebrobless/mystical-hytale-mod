package com.motm.runtime.ability.dash;

import java.util.ArrayList;
import java.util.List;

/** Mutable owner-local lifecycle state for in-flight dash cues and swept paths. */
public final class DashRuntimeState {
    private final List<ActiveDash> activeDashes = new ArrayList<>();

    public void add(ActiveDash dash) {
        if (dash != null) {
            activeDashes.add(dash);
        }
    }

    public int activeDashCount() {
        return activeDashes.size();
    }

    public List<ActiveDash> snapshot() {
        return List.copyOf(activeDashes);
    }

    public void removeProcessed(java.util.function.Predicate<ActiveDash> processor) {
        if (processor != null) {
            activeDashes.removeIf(processor);
        }
    }

    public int removeForPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return 0;
        }
        int before = activeDashes.size();
        activeDashes.removeIf(dash -> dash != null && playerId.equals(dash.ownerPlayerId()));
        return before - activeDashes.size();
    }
}
