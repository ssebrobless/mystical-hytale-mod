package com.motm.runtime.task;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.motm.runtime.MotmRuntimeTasks;
import com.motm.runtime.player.RuntimePlayerView;
import com.motm.runtime.state.RuntimePlayerRegistry;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Owns command-facing inventory task queue policy.
 */
public final class InventoryCommandActions {

    private final BooleanSupplier devToolsEnabled;
    private final Supplier<String> devToolsDisabledMessage;
    private final RuntimePlayerRegistry runtimePlayers;
    private final RuntimePlayerView runtimePlayerView;
    private final MotmRuntimeTasks runtimeTasks;

    public InventoryCommandActions(BooleanSupplier devToolsEnabled,
                                   Supplier<String> devToolsDisabledMessage,
                                   RuntimePlayerRegistry runtimePlayers,
                                   RuntimePlayerView runtimePlayerView,
                                   MotmRuntimeTasks runtimeTasks) {
        this.devToolsEnabled = devToolsEnabled;
        this.devToolsDisabledMessage = devToolsDisabledMessage;
        this.runtimePlayers = runtimePlayers;
        this.runtimePlayerView = runtimePlayerView;
        this.runtimeTasks = runtimeTasks;
    }

    public void queueHydroContainerSync(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        runtimeTasks.requestHydroContainerSync(playerId);
    }

    public String queueSpellbookGrant(Player player) {
        String playerId = runtimePlayerView.findOnlinePlayerId(player);
        if (playerId == null) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        runtimePlayers.put(playerId, player);
        return queueSpellbookGrant(playerId);
    }

    public String queueSpellbookGrant(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        boolean added = runtimeTasks.requestSpellbookGrant(playerId);
        return added
                ? "[MOTM] Spellbook delivery queued."
                : "[MOTM] Spellbook delivery is already queued.";
    }

    public String queueDevBookGrant(Player player) {
        if (!devToolsEnabled.getAsBoolean()) {
            return devToolsDisabledMessage.get();
        }
        String playerId = runtimePlayerView.findOnlinePlayerId(player);
        if (playerId == null) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        runtimePlayers.put(playerId, player);
        return queueDevBookGrant(playerId);
    }

    public String queueDevBookGrant(String playerId) {
        if (!devToolsEnabled.getAsBoolean()) {
            return devToolsDisabledMessage.get();
        }
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        boolean added = runtimeTasks.requestDevBookGrant(playerId);
        return added
                ? "[MOTM] Dev Grimoire delivery queued."
                : "[MOTM] Dev Grimoire delivery is already queued.";
    }
}
