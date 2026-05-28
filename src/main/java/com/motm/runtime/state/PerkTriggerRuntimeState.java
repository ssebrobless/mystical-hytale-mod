package com.motm.runtime.state;

import com.motm.model.PerkTriggerBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks perk trigger bindings by player.
 */
public final class PerkTriggerRuntimeState {

    private final Map<String, List<PerkTriggerBinding>> triggersByPlayer = new ConcurrentHashMap<>();

    public void add(String playerId, PerkTriggerBinding binding) {
        if (playerId == null || playerId.isBlank() || binding == null) {
            return;
        }
        triggersByPlayer.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(binding);
    }

    public void clear(String playerId) {
        if (playerId != null) {
            triggersByPlayer.remove(playerId);
        }
    }

    public List<PerkTriggerBinding> get(String playerId, String type) {
        if (playerId == null || type == null) {
            return List.of();
        }
        List<PerkTriggerBinding> bindings = triggersByPlayer.get(playerId);
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }
        String normalizedType = type.trim().toLowerCase(Locale.ROOT);
        return bindings.stream()
                .filter(binding -> binding.type() != null
                        && binding.type().trim().toLowerCase(Locale.ROOT).equals(normalizedType))
                .toList();
    }
}
