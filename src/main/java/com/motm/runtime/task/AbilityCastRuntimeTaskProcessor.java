package com.motm.runtime.task;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.runtime.MotmRuntimeTasks;
import com.motm.runtime.PendingAbilityCast;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Drains queued ability casts once a live player/store is available.
 */
public final class AbilityCastRuntimeTaskProcessor implements RuntimeTaskProcessor {

    private final MotmRuntimeTasks tasks;
    private final Hooks hooks;
    private final Logger log;

    public AbilityCastRuntimeTaskProcessor(MotmRuntimeTasks tasks, Hooks hooks, Logger log) {
        this.tasks = tasks;
        this.hooks = hooks;
        this.log = log;
    }

    @Override
    public String id() {
        return "ability-cast";
    }

    @Override
    public void process(Store<EntityStore> currentStore) {
        for (PendingAbilityCast request : tasks.pendingAbilityCasts()) {
            Player player = hooks.runtimePlayer(request.playerId());
            if (player == null) {
                tasks.recordTaskSkipped("ability-cast", request.playerId(), Map.of(
                        "abilityId", String.valueOf(request.abilityId()),
                        "reason", "player_unavailable"
                ));
                tasks.completeAbilityCast(request);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("ability-cast", request.playerId(), Map.of(
                        "abilityId", String.valueOf(request.abilityId()),
                        "reason", "wrong_store"
                ));
                continue;
            }

            String failureMessage = null;
            try {
                log.info("[MOTM] Processing queued ability cast: playerId="
                        + request.playerId()
                        + " abilityId=" + request.abilityId());
                failureMessage = hooks.executeQueuedAbilityCast(request, player);
                log.info("[MOTM] Queued ability cast result: playerId="
                        + request.playerId()
                        + " abilityId=" + request.abilityId()
                        + " result=" + (failureMessage == null || failureMessage.isBlank() ? "<success>" : failureMessage));
            } catch (Throwable e) {
                failureMessage = "[MOTM] Queued ability cast failed safely for "
                        + request.abilityId() + ": " + e.getMessage();
                log.log(Level.SEVERE, failureMessage, e);
                tasks.recordTaskFailed("ability-cast", request.playerId(), Map.of(
                        "abilityId", String.valueOf(request.abilityId()),
                        "error", String.valueOf(e.getMessage())
                ));
            } finally {
                tasks.completeAbilityCast(request);
            }
            if (failureMessage == null || failureMessage.isBlank()) {
                tasks.recordTaskExecuted("ability-cast", request.playerId(), Map.of("abilityId", String.valueOf(request.abilityId())));
            }
            if ((request.notifyFailures() || hooks.devToolsEnabled())
                    && failureMessage != null
                    && !failureMessage.isBlank()) {
                hooks.sendMessage(player, failureMessage);
            }
        }
    }

    public interface Hooks {
        Player runtimePlayer(String playerId);

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);

        String executeQueuedAbilityCast(PendingAbilityCast request, Player player);

        boolean devToolsEnabled();

        void sendMessage(Player player, String message);
    }
}
