package com.motm.runtime.ability.terrain;

import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class LapidaryGemRuntimeState {
    private final List<ActiveLapidaryGem> activeLapidaryGems = new ArrayList<>();

    public void addGem(ActiveLapidaryGem gem) {
        if (gem != null) {
            activeLapidaryGems.add(gem);
        }
    }

    public int activeGemCount() {
        return activeLapidaryGems.size();
    }

    public void removeProcessedGems(Predicate<ActiveLapidaryGem> processor) {
        if (processor != null) {
            activeLapidaryGems.removeIf(processor);
        }
    }

    public int removeGemsForPlayer(String playerId, Predicate<ActiveLapidaryGem> cleanup) {
        if (playerId == null || playerId.isBlank()) {
            return 0;
        }
        int[] removed = {0};
        activeLapidaryGems.removeIf(gem -> {
            if (gem == null || !playerId.equals(gem.ownerPlayerId())) {
                return false;
            }
            if (cleanup != null && !cleanup.test(gem)) {
                return false;
            }
            removed[0]++;
            return true;
        });
        return removed[0];
    }

    public Vector3d firstCenterForPlayer(String playerId, Predicate<ActiveLapidaryGem> matcher) {
        if (playerId == null || playerId.isBlank()) {
            return null;
        }
        for (ActiveLapidaryGem gem : activeLapidaryGems) {
            if (gem == null || !playerId.equals(gem.ownerPlayerId())) {
                continue;
            }
            if (matcher != null && !matcher.test(gem)) {
                continue;
            }
            return gem.center();
        }
        return null;
    }
}
