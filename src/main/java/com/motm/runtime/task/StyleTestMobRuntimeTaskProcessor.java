package com.motm.runtime.task;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.runtime.MotmRuntimeTasks;

import java.util.Map;

/**
 * Processes style-test mob spawn, clear, and count requests.
 */
public final class StyleTestMobRuntimeTaskProcessor implements RuntimeTaskProcessor {

    private final MotmRuntimeTasks tasks;
    private final Hooks hooks;

    public StyleTestMobRuntimeTaskProcessor(MotmRuntimeTasks tasks, Hooks hooks) {
        this.tasks = tasks;
        this.hooks = hooks;
    }

    @Override
    public String id() {
        return "style-test-mobs";
    }

    @Override
    public void process(Store<EntityStore> currentStore) {
        processClears(currentStore);
        processSpawns(currentStore);
        processCounts(currentStore);
    }

    private void processSpawns(Store<EntityStore> currentStore) {
        for (Map.Entry<String, String> entry : tasks.pendingStyleTestMobSpawns().entrySet()) {
            String playerId = entry.getKey();
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("style-test-mob-spawn", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeStyleTestMobSpawn(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("style-test-mob-spawn", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            String mode = entry.getValue();
            String result = hooks.spawnStyleTestMobsNow(playerId, player, mode);
            hooks.recordServerTruth("style_test_mobs_spawned", Map.of(
                    "playerId", playerId,
                    "mode", mode,
                    "result", String.valueOf(result),
                    "trackedCount", hooks.countTrackedStyleTestTargets(playerId)
            ));
            hooks.sendMessage(player, result);
            tasks.recordTaskExecuted("style-test-mob-spawn", playerId, Map.of(
                    "mode", String.valueOf(mode),
                    "result", String.valueOf(result),
                    "trackedCount", hooks.countTrackedStyleTestTargets(playerId)
            ));
            tasks.completeStyleTestMobSpawn(playerId);
        }
    }

    private void processClears(Store<EntityStore> currentStore) {
        for (String playerId : tasks.pendingStyleTestMobClears()) {
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("style-test-mob-clear", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeStyleTestMobClear(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("style-test-mob-clear", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            String result = hooks.clearStyleTestMobsNow(playerId, currentStore, player);
            hooks.recordServerTruth("style_test_mobs_cleared", Map.of(
                    "playerId", playerId,
                    "result", String.valueOf(result),
                    "trackedCount", hooks.countTrackedStyleTestTargets(playerId)
            ));
            hooks.sendMessage(player, result);
            tasks.recordTaskExecuted("style-test-mob-clear", playerId, Map.of(
                    "result", String.valueOf(result),
                    "trackedCount", hooks.countTrackedStyleTestTargets(playerId)
            ));
            tasks.completeStyleTestMobClear(playerId);
        }
    }

    private void processCounts(Store<EntityStore> currentStore) {
        for (String playerId : tasks.pendingStyleTestMobCounts()) {
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("style-test-mob-count", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeStyleTestMobCount(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("style-test-mob-count", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            String result = hooks.countStyleTestMobsNow(playerId, currentStore, player);
            hooks.recordServerTruth("style_test_mobs_counted", Map.of(
                    "playerId", playerId,
                    "result", String.valueOf(result),
                    "trackedCount", hooks.countTrackedStyleTestTargets(playerId)
            ));
            hooks.sendMessage(player, result);
            tasks.recordTaskExecuted("style-test-mob-count", playerId, Map.of(
                    "result", String.valueOf(result),
                    "trackedCount", hooks.countTrackedStyleTestTargets(playerId)
            ));
            tasks.completeStyleTestMobCount(playerId);
        }
    }

    public interface Hooks {
        Player runtimePlayer(String playerId);

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);

        String spawnStyleTestMobsNow(String playerId, Player runtimePlayer, String mode);

        String clearStyleTestMobsNow(String playerId, Store<EntityStore> currentStore, Player player);

        String countStyleTestMobsNow(String playerId, Store<EntityStore> currentStore, Player player);

        int countTrackedStyleTestTargets(String playerId);

        void recordServerTruth(String type, Map<String, Object> data);

        void sendMessage(Player player, String message);
    }
}
