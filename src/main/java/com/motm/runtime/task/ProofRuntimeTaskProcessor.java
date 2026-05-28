package com.motm.runtime.task;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.runtime.MotmRuntimeTasks;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Processes queued `/motm dev proof` requests and records their trace envelope.
 */
public final class ProofRuntimeTaskProcessor implements RuntimeTaskProcessor {

    private final MotmRuntimeTasks tasks;
    private final Hooks hooks;
    private final Logger log;

    public ProofRuntimeTaskProcessor(MotmRuntimeTasks tasks, Hooks hooks, Logger log) {
        this.tasks = tasks;
        this.hooks = hooks;
        this.log = log;
    }

    @Override
    public String id() {
        return "proof";
    }

    @Override
    public void process(Store<EntityStore> currentStore) {
        for (Map.Entry<String, String> entry : tasks.pendingProofRequests().entrySet()) {
            String playerId = entry.getKey();
            String proofId = entry.getValue();
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("proof", playerId, Map.of(
                        "proofId", String.valueOf(proofId),
                        "reason", "player_unavailable"
                ));
                tasks.completeProofRequest(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("proof", playerId, Map.of(
                        "proofId", String.valueOf(proofId),
                        "reason", "wrong_store"
                ));
                continue;
            }

            String result = null;
            String traceId = hooks.nextProofTraceId();
            hooks.recordCausality("proof_begin", traceId, playerId, proofId, null);
            String previousTraceId = hooks.enterTrace(traceId);
            try {
                result = hooks.runProofNow(playerId, player, currentStore, proofId);
            } catch (Throwable e) {
                result = "[MOTM] Proof " + proofId + " failed safely: " + e.getMessage();
                log.log(Level.SEVERE, result, e);
                tasks.recordTaskFailed("proof", playerId, Map.of(
                        "proofId", String.valueOf(proofId),
                        "error", String.valueOf(e.getMessage())
                ));
            } finally {
                hooks.restoreTrace(previousTraceId);
                tasks.completeProofRequest(playerId);
            }
            log.info(result);
            hooks.recordCausality("proof_end", traceId, playerId, proofId, result);
            if (result != null && !result.contains("failed")) {
                tasks.recordTaskExecuted("proof", playerId, Map.of(
                        "proofId", String.valueOf(proofId),
                        "result", String.valueOf(result)
                ));
            }
            hooks.sendMessage(player, result);
        }
    }

    public interface Hooks {
        Player runtimePlayer(String playerId);

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);

        String nextProofTraceId();

        String enterTrace(String traceId);

        void restoreTrace(String previousTraceId);

        String runProofNow(String playerId, Player player, Store<EntityStore> currentStore, String proofId);

        void recordCausality(String event, String traceId, String playerId, String proofId, String result);

        void sendMessage(Player player, String message);
    }
}
