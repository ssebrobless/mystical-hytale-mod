package com.motm.runtime;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Owns MOTM's per-server-tick runtime sequencing and heartbeat cadence.
 */
public final class MotmRuntimeLoop {

    private final int hudRefreshIntervalTicks;
    private final long heartbeatIntervalMs;
    private final Hooks hooks;

    private long lastHeartbeatAtMs = 0L;
    private int hudRefreshTickCounter = 0;
    private boolean perkTickActivationObserved;


    public MotmRuntimeLoop(int hudRefreshIntervalTicks, long heartbeatIntervalMs, Hooks hooks) {
        this.hudRefreshIntervalTicks = Math.max(1, hudRefreshIntervalTicks);
        this.heartbeatIntervalMs = Math.max(1L, heartbeatIntervalMs);
        this.hooks = hooks;
    }

    public void tick(Store<EntityStore> currentStore) {
        Map<String, Double> dotDamageByEntity = hooks.tickStatusEffects();
        hooks.tickElementalReactions();
        hooks.tickStyleCooldowns();
        hooks.tickResources();
        hooks.processRuntimeTask("player-maintenance", currentStore);
        hooks.processFreeCastSafety(currentStore);
        hooks.tickClassPassives(currentStore);
        hooks.tickRuntimePerks(currentStore);
        if (!perkTickActivationObserved) {
            perkTickActivationObserved = true;
            hooks.logInfo("[MOTM] event=perk_tick_active");
        }

        hooks.processRuntimeTask("style-test-sequence", currentStore);
        hooks.processRuntimeTask("ability-test", currentStore);
        hooks.processRuntimeTask("dev", currentStore);
        hooks.processDevCommandInbox(currentStore);
        hooks.processRuntimeTask("style-review", currentStore);
        hooks.processRuntimeTask("proof", currentStore);
        hooks.processActiveProofCleanups(currentStore);
        hooks.processRuntimeTask("style-test-mobs", currentStore);
        hooks.processRuntimeTask("ability-cast", currentStore);
        hooks.tickArmedStomps(currentStore);
        hooks.tickGameplayPlayback(currentStore);
        hooks.processRuntimeTask("terra-review", currentStore);
        hooks.processRuntimeTask("inventory", currentStore);
        hooks.processRuntimeTask("status-hud", currentStore);
        tickHudRefresh(currentStore);
        recordHeartbeat(currentStore);
        logPendingDotDamage(dotDamageByEntity);
    }

    private void tickHudRefresh(Store<EntityStore> currentStore) {
        hudRefreshTickCounter++;
        if (hudRefreshTickCounter >= hudRefreshIntervalTicks) {
            hudRefreshTickCounter = 0;
            hooks.refreshAllStatusHuds(currentStore);
        }
    }

    private void recordHeartbeat(Store<EntityStore> currentStore) {
        if (!hooks.observabilityActive()) {
            return;
        }

        long now = hooks.nowMs();
        if (now - lastHeartbeatAtMs < heartbeatIntervalMs) {
            return;
        }
        lastHeartbeatAtMs = now;

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("world", worldName(currentStore));
        payload.put("onlinePlayers", hooks.onlineRuntimePlayerCount());
        payload.put("pendingTasks", hooks.runtimeTasksSnapshot());
        payload.put("activeProofSelections", hooks.activeProofSelections());
        payload.put("activeProofProxies", hooks.activeProofProxies());
        payload.put("activeStyleTests", hooks.activeStyleTests());
        payload.put("trackedStyleTargetOwners", hooks.trackedStyleTargetOwners());
        hooks.recordCausality("server_tick_heartbeat", payload);
    }

    private void logPendingDotDamage(Map<String, Double> dotDamageByEntity) {
        if (dotDamageByEntity == null || dotDamageByEntity.isEmpty()) {
            return;
        }

        dotDamageByEntity.forEach((entityId, dotPercent) ->
                hooks.logFine("[MOTM] TODO: Apply " + (dotPercent * 100)
                        + "% max HP DoT to entity " + entityId + " via Hytale's damage API."));
    }

    private static String worldName(Store<EntityStore> currentStore) {
        if (currentStore == null || currentStore.getExternalData() == null
                || currentStore.getExternalData().getWorld() == null) {
            return "unknown";
        }
        return currentStore.getExternalData().getWorld().getName();
    }

    public interface Hooks {
        Map<String, Double> tickStatusEffects();

        void tickElementalReactions();

        void tickStyleCooldowns();

        void tickResources();

        void processRuntimeTask(String id, Store<EntityStore> currentStore);

        void processFreeCastSafety(Store<EntityStore> currentStore);

        void tickClassPassives(Store<EntityStore> currentStore);
        void tickRuntimePerks(Store<EntityStore> currentStore);


        void processDevCommandInbox(Store<EntityStore> currentStore);

        void processActiveProofCleanups(Store<EntityStore> currentStore);

        void tickArmedStomps(Store<EntityStore> currentStore);

        void tickGameplayPlayback(Store<EntityStore> currentStore);

        void refreshAllStatusHuds(Store<EntityStore> currentStore);

        boolean observabilityActive();

        int onlineRuntimePlayerCount();

        Map<String, Object> runtimeTasksSnapshot();

        int activeProofSelections();

        int activeProofProxies();

        int activeStyleTests();

        int trackedStyleTargetOwners();

        void recordCausality(String type, Map<String, Object> data);

        void logFine(String message);
        void logInfo(String message);

        long nowMs();
    }
}
