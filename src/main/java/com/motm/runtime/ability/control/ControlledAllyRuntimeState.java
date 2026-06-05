package com.motm.runtime.ability.control;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;

public final class ControlledAllyRuntimeState {
    private final Map<String, List<ActiveControlledAlly>> alliesByOwner = new HashMap<>();

    public void addOrRefresh(String ownerPlayerId, ActiveControlledAlly ally) {
        if (ownerPlayerId == null || ownerPlayerId.isBlank() || ally == null || ally.ref() == null) {
            return;
        }
        List<ActiveControlledAlly> allies = alliesByOwner.computeIfAbsent(ownerPlayerId, ignored -> new ArrayList<>());
        for (ActiveControlledAlly existing : allies) {
            if (existing != null && ally.ref().equals(existing.ref())) {
                existing.extendUntil(ally.expireAtMillis());
                return;
            }
        }
        allies.add(ally);
    }

    public List<ActiveControlledAlly> alliesForOwner(String ownerPlayerId) {
        if (ownerPlayerId == null || ownerPlayerId.isBlank()) {
            return List.of();
        }
        List<ActiveControlledAlly> allies = alliesByOwner.get(ownerPlayerId);
        return allies == null ? List.of() : List.copyOf(allies);
    }

    public ActiveControlledAlly findByRef(Object ref) {
        if (ref == null) {
            return null;
        }
        for (List<ActiveControlledAlly> allies : alliesByOwner.values()) {
            for (ActiveControlledAlly ally : allies) {
                if (ally != null && ref.equals(ally.ref())) {
                    return ally;
                }
            }
        }
        return null;
    }

    public boolean isControlled(Object ref) {
        return findByRef(ref) != null;
    }

    public int activeOwnerCount() {
        return alliesByOwner.size();
    }

    public int activeAllyCount() {
        return alliesByOwner.values().stream().mapToInt(List::size).sum();
    }

    public int removeAlliesForPlayer(String ownerPlayerId, Consumer<ActiveControlledAlly> cleanup) {
        if (ownerPlayerId == null || ownerPlayerId.isBlank()) {
            return 0;
        }
        List<ActiveControlledAlly> allies = alliesByOwner.remove(ownerPlayerId);
        if (allies == null || allies.isEmpty()) {
            return 0;
        }
        allies.forEach(ally -> {
            if (ally != null && cleanup != null) {
                cleanup.accept(ally);
            }
        });
        return allies.size();
    }

    public boolean removeOwnedRef(String ownerPlayerId, Ref<EntityStore> ref, Consumer<ActiveControlledAlly> cleanup) {
        if (ownerPlayerId == null || ownerPlayerId.isBlank() || ref == null) {
            return false;
        }
        List<ActiveControlledAlly> allies = alliesByOwner.get(ownerPlayerId);
        if (allies == null || allies.isEmpty()) {
            return false;
        }
        boolean removed = allies.removeIf(ally -> {
            boolean match = ally != null && ref.equals(ally.ref());
            if (match && cleanup != null) {
                cleanup.accept(ally);
            }
            return match;
        });
        if (allies.isEmpty()) {
            alliesByOwner.remove(ownerPlayerId);
        }
        return removed;
    }

    public void removeProcessedAllies(Predicate<ActiveControlledAlly> processor,
                                      Consumer<ActiveControlledAlly> cleanup) {
        if (processor == null) {
            return;
        }
        alliesByOwner.values().removeIf(allies -> {
            if (allies == null || allies.isEmpty()) {
                return true;
            }
            allies.removeIf(ally -> {
                boolean remove = processor.test(ally);
                if (remove && cleanup != null) {
                    cleanup.accept(ally);
                }
                return remove;
            });
            return allies.isEmpty();
        });
    }
}
