package com.motm.runtime;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MotmRuntimeLoopTest {

    @Test
    void tickRunsRuntimeSystemsInStableOrder() {
        Hooks hooks = new Hooks();
        hooks.dotDamageByEntity = Map.of("mob-a", 0.25);
        MotmRuntimeLoop loop = new MotmRuntimeLoop(2, 1000L, hooks);

        loop.tick(null);

        assertEquals(List.of(
                "status-effects",
                "elemental-reactions",
                "style-cooldowns",
                "resources",
                "task:player-maintenance",
                "free-cast-safety",
                "class-passives",
                "runtime-perks",
                "log-perk-tick-active",
                "task:style-test-sequence",
                "task:ability-test",
                "task:dev",
                "dev-command-inbox",
                "task:style-review",
                "task:proof",
                "proof-cleanups",
                "task:style-test-mobs",
                "task:ability-cast",
                "armed-stomps",
                "gameplay-playback",
                "task:terra-review",
                "task:inventory",
                "task:status-hud",
                "log-dot"
        ), hooks.events);
    }

    @Test
    void tickOwnsHudAndHeartbeatCadence() {
        Hooks hooks = new Hooks();
        hooks.observabilityActive = true;
        hooks.nowMs = 1000L;
        MotmRuntimeLoop loop = new MotmRuntimeLoop(2, 1000L, hooks);

        loop.tick(null);
        hooks.nowMs = 1500L;
        loop.tick(null);

        assertEquals(1, hooks.heartbeats.size());
        assertEquals("server_tick_heartbeat", hooks.heartbeats.get(0).type());
        assertEquals("unknown", hooks.heartbeats.get(0).data().get("world"));
        assertEquals(1, hooks.hudRefreshes);
    }

    private static final class Hooks implements MotmRuntimeLoop.Hooks {
        final List<String> events = new ArrayList<>();
        final List<Heartbeat> heartbeats = new ArrayList<>();
        Map<String, Double> dotDamageByEntity = Map.of();
        boolean observabilityActive = false;
        long nowMs = 0L;
        int hudRefreshes = 0;

        @Override
        public Map<String, Double> tickStatusEffects() {
            events.add("status-effects");
            return dotDamageByEntity;
        }

        @Override
        public void tickElementalReactions() {
            events.add("elemental-reactions");
        }

        @Override
        public void tickRuntimePerks(Store<EntityStore> currentStore) {
            events.add("runtime-perks");
        }

        @Override
        public void tickStyleCooldowns() {
            events.add("style-cooldowns");
        }

        @Override
        public void tickResources() {
            events.add("resources");
        }

        @Override
        public void processRuntimeTask(String id, Store<EntityStore> currentStore) {
            events.add("task:" + id);
        }

        @Override
        public void processFreeCastSafety(Store<EntityStore> currentStore) {
            events.add("free-cast-safety");
        }

        @Override
        public void tickClassPassives(Store<EntityStore> currentStore) {
            events.add("class-passives");
        }

        @Override
        public void processDevCommandInbox(Store<EntityStore> currentStore) {
            events.add("dev-command-inbox");
        }

        @Override
        public void processActiveProofCleanups(Store<EntityStore> currentStore) {
            events.add("proof-cleanups");
        }

        @Override
        public void tickArmedStomps(Store<EntityStore> currentStore) {
            events.add("armed-stomps");
        }

        @Override
        public void tickGameplayPlayback(Store<EntityStore> currentStore) {
            events.add("gameplay-playback");
        }

        @Override
        public void refreshAllStatusHuds(Store<EntityStore> currentStore) {
            hudRefreshes++;
            events.add("status-hud-refresh-all");
        }

        @Override
        public boolean observabilityActive() {
            return observabilityActive;
        }

        @Override
        public int onlineRuntimePlayerCount() {
            return 2;
        }

        @Override
        public Map<String, Object> runtimeTasksSnapshot() {
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("ability-cast", 1);
            return snapshot;
        }

        @Override
        public int activeProofSelections() {
            return 3;
        }

        @Override
        public int activeProofProxies() {
            return 4;
        }

        @Override
        public int activeStyleTests() {
            return 5;
        }

        @Override
        public int trackedStyleTargetOwners() {
            return 6;
        }

        @Override
        public void recordCausality(String type, Map<String, Object> data) {
            heartbeats.add(new Heartbeat(type, data));
        }

        @Override
        public void logFine(String message) {
            events.add("log-dot");
        }

        @Override
        public void logInfo(String message) {
            events.add(message.contains("perk_tick_active") ? "log-perk-tick-active" : "log-info");
        }

        @Override
        public long nowMs() {
            return nowMs;
        }
    }

    private record Heartbeat(String type, Map<String, Object> data) {
    }
}
