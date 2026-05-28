package com.motm.runtime.state;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks free-cast test protection state outside the plugin shell.
 */
public final class FreeCastRuntimeState {

    private final Set<String> enabledPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, Float> lastObservedHealthByPlayer = new ConcurrentHashMap<>();

    public boolean isEnabled(String playerId) {
        return playerId != null && enabledPlayers.contains(playerId);
    }

    public void setEnabled(String playerId, boolean enabled) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        if (enabled) {
            enabledPlayers.add(playerId);
        } else {
            enabledPlayers.remove(playerId);
            lastObservedHealthByPlayer.remove(playerId);
        }
    }

    public List<String> enabledPlayers() {
        return List.copyOf(enabledPlayers);
    }

    public int enabledCount() {
        return enabledPlayers.size();
    }

    public Float rememberObservedHealth(String playerId, float health) {
        if (playerId == null || playerId.isBlank()) {
            return null;
        }
        return lastObservedHealthByPlayer.put(playerId, health);
    }

    public void clearPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        enabledPlayers.remove(playerId);
        lastObservedHealthByPlayer.remove(playerId);
    }
}
