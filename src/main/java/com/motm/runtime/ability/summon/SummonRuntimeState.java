package com.motm.runtime.ability.summon;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public final class SummonRuntimeState {
    private final Map<String, List<ActiveSummon>> activeSummonsByOwner = new HashMap<>();

    public void addSummon(String ownerPlayerId, ActiveSummon summon) {
        if (ownerPlayerId == null || ownerPlayerId.isBlank() || summon == null) {
            return;
        }
        activeSummonsByOwner.computeIfAbsent(ownerPlayerId, ignored -> new ArrayList<>()).add(summon);
    }

    public List<ActiveSummon> summonsForOwner(String ownerPlayerId) {
        if (ownerPlayerId == null || ownerPlayerId.isBlank()) {
            return List.of();
        }
        List<ActiveSummon> summons = activeSummonsByOwner.get(ownerPlayerId);
        return summons == null ? List.of() : List.copyOf(summons);
    }

    public int activeOwnerCount() {
        return activeSummonsByOwner.size();
    }

    public int activeSummonCount() {
        return activeSummonsByOwner.values().stream().mapToInt(List::size).sum();
    }

    public int summonCountForOwner(String ownerPlayerId) {
        List<ActiveSummon> summons = activeSummonsByOwner.get(ownerPlayerId);
        return summons == null ? 0 : summons.size();
    }

    public ActiveSummon findSummonByRef(Object ref) {
        if (ref == null) {
            return null;
        }
        for (List<ActiveSummon> summons : activeSummonsByOwner.values()) {
            if (summons == null) {
                continue;
            }
            for (ActiveSummon summon : summons) {
                if (summon != null && ref.equals(summon.ref())) {
                    return summon;
                }
            }
        }
        return null;
    }

    public int removeSummonsForPlayer(String ownerPlayerId, Predicate<ActiveSummon> cleanup) {
        if (ownerPlayerId == null || ownerPlayerId.isBlank()) {
            return 0;
        }
        List<ActiveSummon> summons = activeSummonsByOwner.remove(ownerPlayerId);
        if (summons == null || summons.isEmpty()) {
            return 0;
        }
        int cleaned = 0;
        for (ActiveSummon summon : summons) {
            if (cleanup == null || cleanup.test(summon)) {
                cleaned++;
            }
        }
        return cleaned;
    }

    public void removeProcessedSummons(Predicate<ActiveSummon> processor) {
        if (processor == null) {
            return;
        }
        activeSummonsByOwner.values().removeIf(summons -> {
            if (summons == null || summons.isEmpty()) {
                return true;
            }
            summons.removeIf(processor);
            return summons.isEmpty();
        });
    }
}
