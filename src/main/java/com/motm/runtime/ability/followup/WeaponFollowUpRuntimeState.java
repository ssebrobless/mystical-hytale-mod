package com.motm.runtime.ability.followup;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns active weapon follow-ups by player.
 */
public final class WeaponFollowUpRuntimeState {

    private final Map<String, ActiveWeaponFollowUp> followUpsByPlayer = new ConcurrentHashMap<>();

    public void put(String playerId, ActiveWeaponFollowUp followUp) {
        if (playerId != null && !playerId.isBlank() && followUp != null) {
            followUpsByPlayer.put(playerId, followUp);
        }
    }

    public ActiveWeaponFollowUp get(String playerId) {
        return playerId == null ? null : followUpsByPlayer.get(playerId);
    }

    public ActiveWeaponFollowUp remove(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return null;
        }
        return followUpsByPlayer.remove(playerId);
    }

    public boolean contains(String playerId) {
        return playerId != null && followUpsByPlayer.containsKey(playerId);
    }

    public int size() {
        return followUpsByPlayer.size();
    }

    public List<Map.Entry<String, ActiveWeaponFollowUp>> entries() {
        return List.copyOf(followUpsByPlayer.entrySet());
    }
}
