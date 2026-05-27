package com.motm.runtime.state;

import com.hypixel.hytale.server.core.entity.entities.Player;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * Tracks live Hytale player handles and one-time MOTM initialization state.
 */
public final class RuntimePlayerRegistry {

    private final Map<String, Player> playersById = new ConcurrentHashMap<>();
    private final Set<String> initializedPlayerIds = ConcurrentHashMap.newKeySet();

    public void put(String playerId, Player player) {
        if (playerId != null && !playerId.isBlank() && player != null) {
            playersById.put(playerId, player);
        }
    }

    public Player get(String playerId) {
        return playerId == null ? null : playersById.get(playerId);
    }

    public boolean contains(String playerId) {
        return playerId != null && playersById.containsKey(playerId);
    }

    public Player remove(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return null;
        }
        initializedPlayerIds.remove(playerId);
        return playersById.remove(playerId);
    }

    public boolean markInitialized(String playerId) {
        return playerId != null && !playerId.isBlank() && initializedPlayerIds.add(playerId);
    }

    public int size() {
        return playersById.size();
    }

    public List<Player> players() {
        return List.copyOf(playersById.values());
    }

    public List<Map.Entry<String, Player>> entries() {
        return List.copyOf(playersById.entrySet());
    }

    public Map<String, Player> snapshot() {
        return Map.copyOf(playersById);
    }

    public void forEach(BiConsumer<String, Player> consumer) {
        playersById.forEach(consumer);
    }
}
