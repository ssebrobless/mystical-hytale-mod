package com.motm.runtime.ability.terrain;

import org.joml.Vector3d;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class LavaHazardRuntimeState {
    private final Map<String, Vector3d> velocityBoostByPlayer = new HashMap<>();
    private final Set<String> movementBoostedPlayers = new LinkedHashSet<>();
    private final Map<String, Long> hazardProtectionUntilByPlayer = new HashMap<>();

    public void protectUntil(String playerId, long expireAtMillis) {
        if (playerId != null && !playerId.isBlank()) {
            hazardProtectionUntilByPlayer.put(playerId, expireAtMillis);
        }
    }

    public boolean isProtected(String playerId, long now) {
        if (playerId == null || playerId.isBlank()) {
            return false;
        }
        Long until = hazardProtectionUntilByPlayer.get(playerId);
        if (until == null) {
            return false;
        }
        if (now > until) {
            hazardProtectionUntilByPlayer.remove(playerId);
            return false;
        }
        return true;
    }

    public Long protectionUntil(String playerId) {
        return playerId == null || playerId.isBlank() ? null : hazardProtectionUntilByPlayer.get(playerId);
    }

    public void clearProtection(String playerId) {
        if (playerId != null && !playerId.isBlank()) {
            hazardProtectionUntilByPlayer.remove(playerId);
        }
    }

    public void storeVelocityBoost(String playerId, Vector3d boost) {
        if (playerId != null && !playerId.isBlank() && boost != null) {
            velocityBoostByPlayer.put(playerId, new Vector3d(boost));
        }
    }

    public Vector3d removeVelocityBoost(String playerId) {
        return playerId == null || playerId.isBlank() ? null : velocityBoostByPlayer.remove(playerId);
    }

    public void markMovementBoosted(String playerId) {
        if (playerId != null && !playerId.isBlank()) {
            movementBoostedPlayers.add(playerId);
        }
    }

    public boolean isMovementBoosted(String playerId) {
        return playerId != null && movementBoostedPlayers.contains(playerId);
    }

    public boolean consumeMovementBoosted(String playerId) {
        return playerId != null && movementBoostedPlayers.remove(playerId);
    }

    public void clearPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        velocityBoostByPlayer.remove(playerId);
        movementBoostedPlayers.remove(playerId);
        hazardProtectionUntilByPlayer.remove(playerId);
    }
}
