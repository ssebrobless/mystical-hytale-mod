package com.motm.runtime.state;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks the most recent generic caster cast/move EntityEffect applied to each
 * player so loadout swaps and runtime resets can remove the native visual
 * instead of waiting for its JSON duration to expire (defect #13).
 */
public final class CasterVisualRuntimeState {

    private final Map<String, String> lastCasterEffectByPlayer = new ConcurrentHashMap<>();

    public void record(String playerId, String effectId) {
        if (playerId == null || playerId.isBlank() || effectId == null || effectId.isBlank()) {
            return;
        }
        lastCasterEffectByPlayer.put(playerId, effectId);
    }

    /** Returns the tracked effect id for the player, clearing it, or null. */
    public String take(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return null;
        }
        return lastCasterEffectByPlayer.remove(playerId);
    }
}
