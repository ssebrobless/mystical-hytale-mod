package com.motm.runtime.task;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.runtime.MotmRuntimeTasks;

import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes dev convenience tasks that mutate world/player test state.
 */
public final class DevRuntimeTaskProcessor implements RuntimeTaskProcessor {

    private final MotmRuntimeTasks tasks;
    private final Hooks hooks;
    private final Logger log;

    public DevRuntimeTaskProcessor(MotmRuntimeTasks tasks, Hooks hooks, Logger log) {
        this.tasks = tasks;
        this.hooks = hooks;
        this.log = log;
    }

    @Override
    public String id() {
        return "dev";
    }

    @Override
    public void process(Store<EntityStore> currentStore) {
        processRelocations(currentStore);
        processDaylightRequests(currentStore);
        processGameModeChanges(currentStore);
    }

    private void processRelocations(Store<EntityStore> currentStore) {
        for (Map.Entry<String, String> entry : tasks.pendingDevRelocations().entrySet()) {
            String playerId = entry.getKey();
            String target = entry.getValue();
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("dev-relocation", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeDevRelocation(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("dev-relocation", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            String result;
            try {
                result = hooks.relocateRuntimePlayerForTesting(playerId, player, target);
            } catch (Throwable e) {
                result = "[MOTM] Dev relocate failed safely: " + e.getMessage();
                log.log(Level.SEVERE, result, e);
                tasks.recordTaskFailed("dev-relocation", playerId, Map.of(
                        "target", String.valueOf(target),
                        "error", String.valueOf(e.getMessage())
                ));
            } finally {
                tasks.completeDevRelocation(playerId);
            }
            log.info(result);
            if (result != null && !result.contains("failed")) {
                tasks.recordTaskExecuted("dev-relocation", playerId, Map.of(
                        "target", String.valueOf(target),
                        "result", String.valueOf(result)
                ));
            }
            hooks.sendMessage(player, result);
        }
    }

    private void processDaylightRequests(Store<EntityStore> currentStore) {
        for (String playerId : tasks.pendingDaylightRequests()) {
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("daylight", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeDaylightRequest(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("daylight", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            String result;
            try {
                World world = currentStore != null && currentStore.getExternalData() != null
                        ? currentStore.getExternalData().getWorld()
                        : player.getWorld();
                WorldTimeResource time = currentStore == null
                        ? null
                        : currentStore.getResource(WorldTimeResource.getResourceType());
                if (world == null || time == null) {
                    result = "[MOTM] Dev daylight failed: world time resource unavailable.";
                    tasks.recordTaskFailed("daylight", playerId, Map.of("error", "world_time_resource_unavailable"));
                } else {
                    time.setDayTime(0.5d, world, currentStore);
                    result = "[MOTM] Dev daylight applied: dayTime=0.5 sunlight="
                            + String.format(Locale.ROOT, "%.2f", time.getSunlightFactor());
                    tasks.recordTaskExecuted("daylight", playerId, Map.of(
                            "dayTime", 0.5d,
                            "sunlight", time.getSunlightFactor()
                    ));
                }
            } catch (Throwable e) {
                result = "[MOTM] Dev daylight failed safely: " + e.getMessage();
                log.log(Level.SEVERE, result, e);
                tasks.recordTaskFailed("daylight", playerId, Map.of("error", String.valueOf(e.getMessage())));
            } finally {
                tasks.completeDaylightRequest(playerId);
            }
            log.info(result);
            hooks.sendMessage(player, result);
        }
    }

    private void processGameModeChanges(Store<EntityStore> currentStore) {
        for (Map.Entry<String, GameMode> entry : tasks.pendingDevGameModeChanges().entrySet()) {
            String playerId = entry.getKey();
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("game-mode-change", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeDevGameModeChange(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("game-mode-change", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            String result = hooks.applyDevGameModeChange(player, entry.getValue());
            hooks.sendMessage(player, result);
            tasks.recordTaskExecuted("game-mode-change", playerId, Map.of(
                    "gameMode", String.valueOf(entry.getValue()),
                    "result", String.valueOf(result)
            ));
            tasks.completeDevGameModeChange(playerId);
        }
    }

    public interface Hooks {
        Player runtimePlayer(String playerId);

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);

        String relocateRuntimePlayerForTesting(String playerId, Player player, String target);

        String applyDevGameModeChange(Player player, GameMode gameMode);

        void sendMessage(Player player, String message);
    }
}
