package com.motm.runtime.ability.combat;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class CombatRuntimeState {
    private final Set<String> reportedAbilityKillEntityIds = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> recentShockedTargets = new ConcurrentHashMap<>();

    public boolean markAbilityKillReported(String killKey) {
        return killKey != null && !killKey.isBlank() && reportedAbilityKillEntityIds.add(killKey);
    }

    public void markShocked(String entityId, long now) {
        if (entityId != null && !entityId.isBlank()) {
            recentShockedTargets.put(entityId, now);
        }
    }

    public boolean hasActiveOrRecentShock(String entityId, long now, long windowMillis) {
        if (entityId == null || entityId.isBlank()) {
            return false;
        }
        Long appliedAt = recentShockedTargets.get(entityId);
        if (appliedAt == null) {
            return false;
        }
        if (now - appliedAt <= windowMillis) {
            return true;
        }
        recentShockedTargets.remove(entityId, appliedAt);
        return false;
    }

    int reportedKillCount() {
        return reportedAbilityKillEntityIds.size();
    }

    int recentShockCount() {
        return recentShockedTargets.size();
    }
}
