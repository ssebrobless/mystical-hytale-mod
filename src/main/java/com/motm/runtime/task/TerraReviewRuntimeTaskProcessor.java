package com.motm.runtime.task;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.runtime.MotmRuntimeTasks;

import java.util.Map;

/**
 * Processes Terra review kit and inventory cleanup requests.
 */
public final class TerraReviewRuntimeTaskProcessor implements RuntimeTaskProcessor {

    private final MotmRuntimeTasks tasks;
    private final Hooks hooks;

    public TerraReviewRuntimeTaskProcessor(MotmRuntimeTasks tasks, Hooks hooks) {
        this.tasks = tasks;
        this.hooks = hooks;
    }

    @Override
    public String id() {
        return "terra-review";
    }

    @Override
    public void process(Store<EntityStore> currentStore) {
        processKitGrants(currentStore);
        processInventoryCleans(currentStore);
    }

    private void processKitGrants(Store<EntityStore> currentStore) {
        for (String playerId : tasks.pendingTerraReviewKitGrants()) {
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("terra-review-kit-grant", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeTerraReviewKitGrant(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("terra-review-kit-grant", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            String result = hooks.grantReviewKit(player);
            hooks.sendMessage(player, result);
            tasks.recordTaskExecuted("terra-review-kit-grant", playerId, Map.of("result", String.valueOf(result)));
            tasks.completeTerraReviewKitGrant(playerId);
        }
    }

    private void processInventoryCleans(Store<EntityStore> currentStore) {
        for (String playerId : tasks.pendingTerraReviewInventoryCleans()) {
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("terra-review-inventory-clean", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeTerraReviewInventoryClean(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("terra-review-inventory-clean", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            String result = hooks.cleanReviewInventory(player);
            hooks.sendMessage(player, result);
            tasks.recordTaskExecuted("terra-review-inventory-clean", playerId, Map.of("result", String.valueOf(result)));
            tasks.completeTerraReviewInventoryClean(playerId);
        }
    }

    public interface Hooks {
        Player runtimePlayer(String playerId);

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);

        String grantReviewKit(Player player);

        String cleanReviewInventory(Player player);

        void sendMessage(Player player, String message);
    }
}
