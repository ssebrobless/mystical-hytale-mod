package com.motm.runtime.task;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.PlayerData;
import com.motm.runtime.MotmRuntimeTasks;

import java.util.Map;

/**
 * Processes player maintenance tasks that need a live player/store.
 */
public final class PlayerMaintenanceRuntimeTaskProcessor implements RuntimeTaskProcessor {

    private final MotmRuntimeTasks tasks;
    private final Hooks hooks;

    public PlayerMaintenanceRuntimeTaskProcessor(MotmRuntimeTasks tasks, Hooks hooks) {
        this.tasks = tasks;
        this.hooks = hooks;
    }

    @Override
    public String id() {
        return "player-maintenance";
    }

    @Override
    public void process(Store<EntityStore> currentStore) {
        processFreeCastInvulnerabilityClears(currentStore);
        processRuntimeRebuilds(currentStore);
        processProgressionBonusRefreshes(currentStore);
    }

    private void processFreeCastInvulnerabilityClears(Store<EntityStore> currentStore) {
        for (String playerId : tasks.pendingFreeCastInvulnerabilityClears()) {
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("free-cast-invulnerability-clear", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeFreeCastInvulnerabilityClear(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("free-cast-invulnerability-clear", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            hooks.clearFreeCastInvulnerability(playerId);
            tasks.recordTaskExecuted("free-cast-invulnerability-clear", playerId, Map.of());
            tasks.completeFreeCastInvulnerabilityClear(playerId);
        }
    }

    private void processRuntimeRebuilds(Store<EntityStore> currentStore) {
        for (String playerId : tasks.pendingRuntimeRebuilds()) {
            Player player = hooks.runtimePlayer(playerId);
            PlayerData playerData = hooks.playerData(playerId);
            if (player == null || playerData == null) {
                tasks.recordTaskSkipped("runtime-rebuild", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeRuntimeRebuild(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("runtime-rebuild", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            hooks.rebuildPlayerRuntimeNow(playerData);
            tasks.recordTaskExecuted("runtime-rebuild", playerId, Map.of());
            tasks.completeRuntimeRebuild(playerId);
        }
    }

    private void processProgressionBonusRefreshes(Store<EntityStore> currentStore) {
        for (String playerId : tasks.pendingProgressionBonusRefreshes()) {
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("progression-bonus-refresh", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeProgressionBonusRefresh(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("progression-bonus-refresh", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            hooks.refreshPlayerProgressionBonusesNow(playerId);
            tasks.recordTaskExecuted("progression-bonus-refresh", playerId, Map.of());
            tasks.completeProgressionBonusRefresh(playerId);
        }
    }

    public interface Hooks {
        Player runtimePlayer(String playerId);

        PlayerData playerData(String playerId);

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);

        void clearFreeCastInvulnerability(String playerId);

        void rebuildPlayerRuntimeNow(PlayerData playerData);

        void refreshPlayerProgressionBonusesNow(String playerId);
    }
}
