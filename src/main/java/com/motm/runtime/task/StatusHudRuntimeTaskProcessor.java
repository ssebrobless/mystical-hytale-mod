package com.motm.runtime.task;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.runtime.MotmRuntimeTasks;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Processes queued status HUD installs and refreshes.
 */
public final class StatusHudRuntimeTaskProcessor implements RuntimeTaskProcessor {

    private final MotmRuntimeTasks tasks;
    private final Hooks hooks;
    private final Logger log;

    public StatusHudRuntimeTaskProcessor(MotmRuntimeTasks tasks, Hooks hooks, Logger log) {
        this.tasks = tasks;
        this.hooks = hooks;
        this.log = log;
    }

    @Override
    public String id() {
        return "status-hud";
    }

    @Override
    public void process(Store<EntityStore> currentStore) {
        processInstalls(currentStore);
        processRefreshes(currentStore);
    }

    private void processInstalls(Store<EntityStore> currentStore) {
        for (Map.Entry<String, Integer> entry : tasks.pendingStatusHudInstalls().entrySet()) {
            String playerId = entry.getKey();
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("status-hud-install", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeStatusHudInstall(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("status-hud-install", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            int ticksRemaining = entry.getValue() - 1;
            if (ticksRemaining > 0) {
                tasks.updateStatusHudInstallDelay(playerId, ticksRemaining);
                continue;
            }

            log.info("[MOTM] Installing HUD: playerId=" + playerId);
            hooks.installStatusHud(player);
            tasks.completeStatusHudInstall(playerId);
            tasks.requestStatusHudRefresh(playerId);
            tasks.recordTaskExecuted("status-hud-install", playerId, Map.of());
        }
    }

    private void processRefreshes(Store<EntityStore> currentStore) {
        for (String playerId : tasks.pendingStatusHudRefreshes()) {
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("status-hud-refresh", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeStatusHudRefresh(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("status-hud-refresh", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            hooks.refreshStatusHudNow(playerId);
            tasks.recordTaskExecuted("status-hud-refresh", playerId, Map.of());
            tasks.completeStatusHudRefresh(playerId);
        }
    }

    public interface Hooks {
        Player runtimePlayer(String playerId);

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);

        void installStatusHud(Player player);

        void refreshStatusHudNow(String playerId);
    }
}
