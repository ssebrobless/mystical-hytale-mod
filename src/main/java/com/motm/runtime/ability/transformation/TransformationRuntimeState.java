package com.motm.runtime.ability.transformation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public final class TransformationRuntimeState {
    private final Map<String, ActiveTransformation> activeTransformationsByPlayer = new HashMap<>();
    private final Map<String, Long> nextPulseAtByPlayer = new HashMap<>();

    public void putTransformation(String playerId, ActiveTransformation transformation, long nextPulseAtMillis) {
        if (playerId == null || playerId.isBlank() || transformation == null) {
            return;
        }
        activeTransformationsByPlayer.put(playerId, transformation);
        nextPulseAtByPlayer.put(playerId, nextPulseAtMillis);
    }

    public ActiveTransformation getTransformation(String playerId) {
        return playerId == null || playerId.isBlank() ? null : activeTransformationsByPlayer.get(playerId);
    }

    public boolean containsTransformation(String playerId) {
        return getTransformation(playerId) != null;
    }

    public int activeTransformationCount() {
        return activeTransformationsByPlayer.size();
    }

    public ActiveTransformation removeTransformation(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return null;
        }
        nextPulseAtByPlayer.remove(playerId);
        return activeTransformationsByPlayer.remove(playerId);
    }

    public ActiveTransformation removeTransformationForAbility(String playerId, String normalizedAbilityId) {
        ActiveTransformation transformation = getTransformation(playerId);
        if (transformation == null || normalizedAbilityId == null || normalizedAbilityId.isBlank()
                || !normalizedAbilityId.equals(normalize(transformation.abilityId()))) {
            return null;
        }
        removeTransformation(playerId);
        return transformation;
    }

    public long nextPulseAt(String playerId, long defaultValue) {
        return nextPulseAtByPlayer.getOrDefault(playerId, defaultValue);
    }

    public void scheduleNextPulse(String playerId, long nextPulseAtMillis) {
        if (playerId != null && !playerId.isBlank() && activeTransformationsByPlayer.containsKey(playerId)) {
            nextPulseAtByPlayer.put(playerId, nextPulseAtMillis);
        }
    }

    public void clearNextPulse(String playerId) {
        if (playerId != null && !playerId.isBlank()) {
            nextPulseAtByPlayer.remove(playerId);
        }
    }

    public void removeProcessedTransformations(Predicate<ActiveTransformation> processor) {
        if (processor == null) {
            return;
        }
        activeTransformationsByPlayer.entrySet().removeIf(entry -> {
            ActiveTransformation transformation = entry.getValue();
            if (transformation == null || processor.test(transformation)) {
                nextPulseAtByPlayer.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(java.util.Locale.ROOT);
    }
}
