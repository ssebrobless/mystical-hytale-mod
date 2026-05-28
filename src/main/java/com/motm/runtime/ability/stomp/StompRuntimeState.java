package com.motm.runtime.ability.stomp;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class StompRuntimeState {
    private final Map<String, ArmedStomp> armedStompByPlayer = new ConcurrentHashMap<>();

    public void arm(String playerId, ArmedStomp stomp) {
        if (playerId != null && !playerId.isBlank() && stomp != null) {
            armedStompByPlayer.put(playerId, stomp);
        }
    }

    public ArmedStomp get(String playerId) {
        return playerId == null || playerId.isBlank() ? null : armedStompByPlayer.get(playerId);
    }

    public boolean contains(String playerId) {
        return get(playerId) != null;
    }

    public boolean isEmpty() {
        return armedStompByPlayer.isEmpty();
    }

    public List<ArmedStomp> armedStomps() {
        return List.copyOf(armedStompByPlayer.values());
    }

    public ArmedStomp remove(String playerId) {
        return playerId == null || playerId.isBlank() ? null : armedStompByPlayer.remove(playerId);
    }

    public boolean remove(String playerId, ArmedStomp stomp) {
        return playerId != null && stomp != null && armedStompByPlayer.remove(playerId, stomp);
    }

    public boolean replace(String playerId, ArmedStomp previous, ArmedStomp next) {
        return playerId != null && previous != null && next != null
                && armedStompByPlayer.replace(playerId, previous, next);
    }
}
