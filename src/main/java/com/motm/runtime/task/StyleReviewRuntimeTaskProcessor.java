package com.motm.runtime.task;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.runtime.MotmRuntimeTasks;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Resets the style-review arena and clears queued review work for one player.
 */
public final class StyleReviewRuntimeTaskProcessor implements RuntimeTaskProcessor {

    private final MotmRuntimeTasks tasks;
    private final Hooks hooks;
    private final Logger log;

    public StyleReviewRuntimeTaskProcessor(MotmRuntimeTasks tasks, Hooks hooks, Logger log) {
        this.tasks = tasks;
        this.hooks = hooks;
        this.log = log;
    }

    @Override
    public String id() {
        return "style-review";
    }

    @Override
    public void process(Store<EntityStore> currentStore) {
        for (String playerId : tasks.pendingStyleReviewResets()) {
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("style-review-reset", playerId, Map.of("reason", "player_unavailable"));
                tasks.completeStyleReviewReset(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("style-review-reset", playerId, Map.of("reason", "wrong_store"));
                continue;
            }

            String mobResult = hooks.clearStyleTestMobsNow(playerId, currentStore, player);
            String runtimeResult = hooks.resetReviewRuntime(playerId, currentStore, player);
            String arenaResult = hooks.scrubStyleReviewArena(player);
            hooks.clearStatusEffects(playerId);
            hooks.clearElementalMarks(playerId);
            hooks.resetCooldowns(playerId);
            hooks.setFreeCastEnabled(playerId, false);
            hooks.clearActiveStyleTest(playerId);
            tasks.completeStyleAbilityTest(playerId);
            tasks.cancelAbilityCastsForPlayer(playerId);
            tasks.completeStyleReviewReset(playerId);

            String summary = "[MOTM] Style review arena reset: " + mobResult
                    + " | runtime=" + runtimeResult
                    + " | arena=" + arenaResult;
            log.info(summary + " playerId=" + playerId);
            tasks.recordTaskExecuted("style-review-reset", playerId, Map.of(
                    "mobResult", String.valueOf(mobResult),
                    "runtimeResult", String.valueOf(runtimeResult),
                    "arenaResult", String.valueOf(arenaResult)
            ));
            hooks.sendMessage(player, summary);
        }
    }

    public interface Hooks {
        Player runtimePlayer(String playerId);

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);

        String clearStyleTestMobsNow(String playerId, Store<EntityStore> currentStore, Player player);

        String resetReviewRuntime(String playerId, Store<EntityStore> currentStore, Player player);

        String scrubStyleReviewArena(Player player);

        void clearStatusEffects(String playerId);

        void clearElementalMarks(String playerId);

        void resetCooldowns(String playerId);

        void setFreeCastEnabled(String playerId, boolean enabled);

        void clearActiveStyleTest(String playerId);

        void sendMessage(Player player, String message);
    }
}
