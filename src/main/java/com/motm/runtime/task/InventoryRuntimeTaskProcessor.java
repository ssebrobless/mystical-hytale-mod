package com.motm.runtime.task;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.PlayerData;
import com.motm.runtime.MotmRuntimeTasks;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Processes deferred inventory-facing tasks that require a live player/store.
 */
public final class InventoryRuntimeTaskProcessor implements RuntimeTaskProcessor {

    private final MotmRuntimeTasks tasks;
    private final Hooks hooks;
    private final Logger log;

    public InventoryRuntimeTaskProcessor(MotmRuntimeTasks tasks, Hooks hooks, Logger log) {
        this.tasks = tasks;
        this.hooks = hooks;
        this.log = log;
    }

    @Override
    public String id() {
        return "inventory";
    }

    @Override
    public void process(Store<EntityStore> currentStore) {
        processSpellbookGrants(currentStore);
        processDevBookGrants(currentStore);
        processHydroContainerSyncs(currentStore);
    }

    private void processSpellbookGrants(Store<EntityStore> currentStore) {
        for (String playerId : tasks.pendingSpellbookGrants()) {
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("spellbook-grant", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeSpellbookGrant(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("spellbook-grant", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            boolean granted = hooks.ensureSpellbookItem(player);
            log.info("[MOTM] Pending spellbook grant processed: playerId=" + playerId
                    + " granted=" + granted
                    + " nowHasSpellbook=" + hooks.playerHasSpellbook(player));
            if (!granted && hooks.playerHasSpellbook(player)) {
                hooks.sendMessage(player, "[MOTM] You already have a spellbook in your inventory.");
            }
            tasks.recordTaskExecuted("spellbook-grant", playerId, Map.of(
                    "granted", granted,
                    "nowHasSpellbook", hooks.playerHasSpellbook(player)
            ));
            tasks.completeSpellbookGrant(playerId);
        }
    }

    private void processDevBookGrants(Store<EntityStore> currentStore) {
        for (String playerId : tasks.pendingDevBookGrants()) {
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("dev-book-grant", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeDevBookGrant(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("dev-book-grant", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            boolean granted = hooks.ensureDevBookItem(player);
            if (!granted && hooks.playerHasDevBook(player)) {
                hooks.sendMessage(player, "[MOTM] You already have a Dev Grimoire in your inventory.");
            }
            tasks.recordTaskExecuted("dev-book-grant", playerId, Map.of(
                    "granted", granted,
                    "nowHasDevBook", hooks.playerHasDevBook(player)
            ));
            tasks.completeDevBookGrant(playerId);
        }
    }

    private void processHydroContainerSyncs(Store<EntityStore> currentStore) {
        for (String playerId : tasks.pendingHydroContainerSyncs()) {
            Player player = hooks.runtimePlayer(playerId);
            PlayerData playerData = hooks.playerData(playerId);
            if (player == null || playerData == null) {
                tasks.recordTaskSkipped("hydro-container-sync", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeHydroContainerSync(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("hydro-container-sync", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            hooks.syncHydroContainerItem(player, playerData, false);
            tasks.recordTaskExecuted("hydro-container-sync", playerId, Map.of());
            tasks.completeHydroContainerSync(playerId);
        }
    }

    public interface Hooks {
        Player runtimePlayer(String playerId);

        PlayerData playerData(String playerId);

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);

        boolean ensureSpellbookItem(Player player);

        boolean ensureDevBookItem(Player player);

        boolean playerHasSpellbook(Player player);

        boolean playerHasDevBook(Player player);

        void syncHydroContainerItem(Player player, PlayerData playerData, boolean notify);

        void sendMessage(Player player, String message);
    }
}
