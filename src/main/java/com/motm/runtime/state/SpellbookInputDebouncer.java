package com.motm.runtime.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Debounces repeated spellbook slot inputs per player.
 */
public final class SpellbookInputDebouncer {

    private final Map<String, Long> recentInputs = new ConcurrentHashMap<>();

    public boolean isDuplicate(String playerId, int slot, long nowMillis, long debounceMillis, long cleanupMillis) {
        if (playerId == null || playerId.isBlank() || slot <= 0) {
            return false;
        }

        String key = key(playerId, slot);
        Long previous = recentInputs.put(key, nowMillis);
        recentInputs.entrySet().removeIf(entry -> nowMillis - entry.getValue() > cleanupMillis);
        return previous != null && nowMillis - previous < debounceMillis;
    }

    public void clearPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        recentInputs.keySet().removeIf(key -> key.startsWith(playerId + ":"));
    }

    private String key(String playerId, int slot) {
        return playerId + ":" + slot;
    }
}
