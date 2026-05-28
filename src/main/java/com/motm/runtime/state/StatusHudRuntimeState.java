package com.motm.runtime.state;

import com.motm.ui.MotmStatusHud;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Tracks installed custom HUD handles by player.
 */
public final class StatusHudRuntimeState {

    private final Map<String, MotmStatusHud> hudsByPlayer = new ConcurrentHashMap<>();

    public boolean contains(String playerId) {
        return playerId != null && hudsByPlayer.containsKey(playerId);
    }

    public MotmStatusHud get(String playerId) {
        return playerId == null ? null : hudsByPlayer.get(playerId);
    }

    public void put(String playerId, MotmStatusHud hud) {
        if (playerId != null && !playerId.isBlank() && hud != null) {
            hudsByPlayer.put(playerId, hud);
        }
    }

    public void remove(String playerId) {
        if (playerId != null) {
            hudsByPlayer.remove(playerId);
        }
    }

    public void removeIfPlayer(Predicate<String> predicate) {
        if (predicate != null) {
            hudsByPlayer.keySet().removeIf(predicate);
        }
    }

    public void forEach(BiConsumer<String, MotmStatusHud> consumer) {
        hudsByPlayer.forEach(consumer);
    }
}
