package com.motm.runtime.state;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns dev style-test sequences and tracked test-target refs.
 */
public final class StyleTestRuntimeState {

    private final Map<String, List<Ref<EntityStore>>> targetsByPlayer = new ConcurrentHashMap<>();
    private final Map<String, ActiveStyleTest> activeTestsByPlayer = new ConcurrentHashMap<>();

    public List<Ref<EntityStore>> targets(String playerId) {
        return targetsByPlayer.getOrDefault(playerId, List.of());
    }

    public void putTargets(String playerId, List<Ref<EntityStore>> targets) {
        if (playerId != null && !playerId.isBlank()) {
            targetsByPlayer.put(playerId, targets == null ? List.of() : List.copyOf(targets));
        }
    }

    public List<Ref<EntityStore>> removeTargets(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return null;
        }
        return targetsByPlayer.remove(playerId);
    }

    public int targetOwnerCount() {
        return targetsByPlayer.size();
    }

    public void start(ActiveStyleTest test) {
        if (test != null && test.playerId() != null && !test.playerId().isBlank()) {
            activeTestsByPlayer.put(test.playerId(), test);
        }
    }

    public ActiveStyleTest get(String playerId) {
        return playerId == null ? null : activeTestsByPlayer.get(playerId);
    }

    public List<ActiveStyleTest> activeTests() {
        return List.copyOf(activeTestsByPlayer.values());
    }

    public ActiveStyleTest stop(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return null;
        }
        return activeTestsByPlayer.remove(playerId);
    }

    public void advance(String playerId, long nextActionAtMs) {
        ActiveStyleTest active = get(playerId);
        if (active != null) {
            activeTestsByPlayer.put(playerId, active.advance(nextActionAtMs));
        }
    }

    public int activeCount() {
        return activeTestsByPlayer.size();
    }

    public void clearPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        targetsByPlayer.remove(playerId);
        activeTestsByPlayer.remove(playerId);
    }
}
