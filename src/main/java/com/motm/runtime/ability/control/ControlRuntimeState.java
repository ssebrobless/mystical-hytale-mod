package com.motm.runtime.ability.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Registry of active controlled allies keyed by owning player, mirroring
 * {@code SummonRuntimeState}. In addition to per-owner tracking it exposes
 * controlled-entity lookups so the friendly-fire filter can treat a controlled
 * ally as the owner's ally, and so {@code mind_shatter} can resolve its center
 * on a currently-controlled entity.
 */
public final class ControlRuntimeState {
    private final Map<String, List<ActiveControlledAlly>> byOwner = new HashMap<>();

    public void add(String ownerPlayerId, ActiveControlledAlly ally) {
        if (ownerPlayerId == null || ownerPlayerId.isBlank() || ally == null) {
            return;
        }
        byOwner.computeIfAbsent(ownerPlayerId, ignored -> new ArrayList<>()).add(ally);
    }

    public List<ActiveControlledAlly> alliesForOwner(String ownerPlayerId) {
        if (ownerPlayerId == null || ownerPlayerId.isBlank()) {
            return List.of();
        }
        List<ActiveControlledAlly> allies = byOwner.get(ownerPlayerId);
        return allies == null ? List.of() : List.copyOf(allies);
    }

    public int activeOwnerCount() {
        return byOwner.size();
    }

    public int activeControlledCount() {
        return byOwner.values().stream().mapToInt(List::size).sum();
    }

    public int controlledCountForOwner(String ownerPlayerId) {
        List<ActiveControlledAlly> allies = byOwner.get(ownerPlayerId);
        return allies == null ? 0 : allies.size();
    }

    /** True when {@code entityId} is currently controlled by any player (friendly-fire gate). */
    public boolean isControlledEntity(String entityId) {
        return findByControlledEntityId(entityId) != null;
    }

    public ActiveControlledAlly findByControlledEntityId(String entityId) {
        if (entityId == null || entityId.isBlank()) {
            return null;
        }
        for (List<ActiveControlledAlly> allies : byOwner.values()) {
            if (allies == null) {
                continue;
            }
            for (ActiveControlledAlly ally : allies) {
                if (ally != null && entityId.equals(ally.controlledEntityId())) {
                    return ally;
                }
            }
        }
        return null;
    }

    public ActiveControlledAlly findByControlledRef(Object controlledRef) {
        if (controlledRef == null) {
            return null;
        }
        for (List<ActiveControlledAlly> allies : byOwner.values()) {
            if (allies == null) {
                continue;
            }
            for (ActiveControlledAlly ally : allies) {
                if (ally != null && controlledRef.equals(ally.controlledRef())) {
                    return ally;
                }
            }
        }
        return null;
    }

    public Set<String> controlledEntityIdsForOwner(String ownerPlayerId) {
        List<ActiveControlledAlly> allies = byOwner.get(ownerPlayerId);
        if (allies == null || allies.isEmpty()) {
            return Set.of();
        }
        Set<String> ids = new LinkedHashSet<>();
        for (ActiveControlledAlly ally : allies) {
            if (ally != null && ally.controlledEntityId() != null) {
                ids.add(ally.controlledEntityId());
            }
        }
        return ids;
    }

    /** Removes every controlled ally for a player (logout / death), running {@code cleanup} per entry. */
    public int removeForOwner(String ownerPlayerId, Predicate<ActiveControlledAlly> cleanup) {
        if (ownerPlayerId == null || ownerPlayerId.isBlank()) {
            return 0;
        }
        List<ActiveControlledAlly> allies = byOwner.remove(ownerPlayerId);
        if (allies == null || allies.isEmpty()) {
            return 0;
        }
        int cleaned = 0;
        for (ActiveControlledAlly ally : allies) {
            if (cleanup == null || cleanup.test(ally)) {
                cleaned++;
            }
        }
        return cleaned;
    }

    /**
     * Runs {@code processor} over every active ally; entries for which it returns
     * {@code true} (control ended) are removed, mirroring
     * {@code SummonRuntimeState.removeProcessedSummons}.
     */
    public void removeProcessed(Predicate<ActiveControlledAlly> processor) {
        if (processor == null) {
            return;
        }
        byOwner.values().removeIf(allies -> {
            if (allies == null || allies.isEmpty()) {
                return true;
            }
            allies.removeIf(processor);
            return allies.isEmpty();
        });
    }
}
