package com.motm.runtime.ability.self;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class SelfRuntimeState {
    private final List<ActivePlayerAnchor> activePlayerAnchors = new ArrayList<>();
    private final List<ActiveSelfEffect> activeSelfEffects = new ArrayList<>();

    public void replacePlayerAnchor(String playerId, ActivePlayerAnchor anchor) {
        removePlayerAnchorsForPlayer(playerId);
        if (anchor != null) {
            activePlayerAnchors.add(anchor);
        }
    }

    public void replaceSelfEffect(String ownerPlayerId, String effectId, ActiveSelfEffect effect) {
        if (ownerPlayerId != null && effectId != null && !effectId.isBlank()) {
            activeSelfEffects.removeIf(existing -> existing != null
                    && ownerPlayerId.equals(existing.ownerPlayerId())
                    && effectId.equals(existing.effectId()));
        }
        if (effect != null) {
            activeSelfEffects.add(effect);
        }
    }

    public int activePlayerAnchorCount() {
        return activePlayerAnchors.size();
    }

    public int activeSelfEffectCount() {
        return activeSelfEffects.size();
    }

    public void removeProcessedPlayerAnchors(Predicate<ActivePlayerAnchor> processor) {
        if (processor != null) {
            activePlayerAnchors.removeIf(processor);
        }
    }

    public void removeProcessedSelfEffects(Predicate<ActiveSelfEffect> processor) {
        if (processor != null) {
            activeSelfEffects.removeIf(processor);
        }
    }

    public int removePlayerAnchorsForPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return 0;
        }
        int before = activePlayerAnchors.size();
        activePlayerAnchors.removeIf(anchor -> anchor != null && playerId.equals(anchor.ownerPlayerId()));
        return before - activePlayerAnchors.size();
    }

    public int removeSelfEffectsForPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return 0;
        }
        int before = activeSelfEffects.size();
        activeSelfEffects.removeIf(effect -> effect != null && playerId.equals(effect.ownerPlayerId()));
        return before - activeSelfEffects.size();
    }
}
