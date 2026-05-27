package com.motm.runtime.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks progression health targets already applied to live players.
 */
public final class TargetHealthRuntimeState {

    private final Map<String, Double> lastAppliedTargetHealthByPlayer = new ConcurrentHashMap<>();

    public Double get(String playerId) {
        return playerId == null ? null : lastAppliedTargetHealthByPlayer.get(playerId);
    }

    public void remember(String playerId, double targetHealth) {
        if (playerId != null && !playerId.isBlank()) {
            lastAppliedTargetHealthByPlayer.put(playerId, targetHealth);
        }
    }

    public void clear(String playerId) {
        if (playerId != null && !playerId.isBlank()) {
            lastAppliedTargetHealthByPlayer.remove(playerId);
        }
    }
}
