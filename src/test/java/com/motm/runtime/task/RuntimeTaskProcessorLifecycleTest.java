package com.motm.runtime.task;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.runtime.MotmRuntimeTasks;
import com.motm.runtime.PendingAbilityCast;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RuntimeTaskProcessorLifecycleTest {

    @Test
    void abilityCastProcessorCompletesUnavailablePlayerWithSkippedEvidence() {
        MotmRuntimeTasks tasks = new MotmRuntimeTasks();
        List<Map<String, Object>> events = captureEvents(tasks);
        tasks.enqueueAbilityCast(new PendingAbilityCast("player", "ability", null, null, true));

        new AbilityCastRuntimeTaskProcessor(tasks, new AbilityCastRuntimeTaskProcessor.Hooks() {
            @Override
            public Player runtimePlayer(String playerId) {
                return null;
            }

            @Override
            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                return false;
            }

            @Override
            public String executeQueuedAbilityCast(PendingAbilityCast request, Player player) {
                return null;
            }

            @Override
            public boolean devToolsEnabled() {
                return false;
            }

            @Override
            public void sendMessage(Player player, String message) {
            }
        }, Logger.getLogger("test")).process(null);

        assertTrue(tasks.pendingAbilityCasts().isEmpty());
        assertEquals("accepted", events.get(0).get("phase"));
        assertEquals("skipped", events.get(1).get("phase"));
        assertEquals("ability-cast", events.get(1).get("taskType"));
    }

    @Test
    void proofProcessorCompletesUnavailablePlayerWithSkippedEvidence() {
        MotmRuntimeTasks tasks = new MotmRuntimeTasks();
        List<Map<String, Object>> events = captureEvents(tasks);
        tasks.requestProof("player", "proof-id");

        new ProofRuntimeTaskProcessor(tasks, new ProofRuntimeTaskProcessor.Hooks() {
            @Override
            public Player runtimePlayer(String playerId) {
                return null;
            }

            @Override
            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                return false;
            }

            @Override
            public String nextProofTraceId() {
                return "trace";
            }

            @Override
            public String enterTrace(String traceId) {
                return null;
            }

            @Override
            public void restoreTrace(String previousTraceId) {
            }

            @Override
            public String runProofNow(String playerId, Player player, Store<EntityStore> currentStore, String proofId) {
                return "ok";
            }

            @Override
            public void recordCausality(String event, String traceId, String playerId, String proofId, String result) {
            }

            @Override
            public void sendMessage(Player player, String message) {
            }
        }, Logger.getLogger("test")).process(null);

        assertTrue(tasks.pendingProofRequests().isEmpty());
        assertEquals("accepted", events.get(0).get("phase"));
        assertEquals("skipped", events.get(1).get("phase"));
        assertEquals("proof", events.get(1).get("taskType"));
    }

    @Test
    void abilityTestProcessorCompletesUnavailablePlayerWithSkippedEvidence() {
        MotmRuntimeTasks tasks = new MotmRuntimeTasks();
        List<Map<String, Object>> events = captureEvents(tasks);
        tasks.requestStyleAbilityTest("player", "ability");

        new AbilityTestRuntimeTaskProcessor(tasks, new AbilityTestRuntimeTaskProcessor.Hooks() {
            @Override
            public Player runtimePlayer(String playerId) {
                return null;
            }

            @Override
            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                return false;
            }

            @Override
            public List<Ref<EntityStore>> styleTestTargets(String playerId) {
                return List.of();
            }

            @Override
            public Vector3d entityPosition(Store<EntityStore> currentStore, Ref<EntityStore> ref) {
                return null;
            }

            @Override
            public void queueAbilityCast(String playerId,
                                         String abilityId,
                                         Ref<EntityStore> targetRef,
                                         Vector3i targetBlock,
                                         boolean notifyFailures) {
            }
        }, Logger.getLogger("test")).process(null);

        assertTrue(tasks.pendingSingleAbilityTests().isEmpty());
        assertEquals("skipped", events.get(1).get("phase"));
    }

    @Test
    void terraReviewProcessorCompletesUnavailablePlayerWithSkippedEvidence() {
        MotmRuntimeTasks tasks = new MotmRuntimeTasks();
        List<Map<String, Object>> events = captureEvents(tasks);
        tasks.requestTerraReviewKitGrant("player");

        new TerraReviewRuntimeTaskProcessor(tasks, new TerraReviewRuntimeTaskProcessor.Hooks() {
            @Override
            public Player runtimePlayer(String playerId) {
                return null;
            }

            @Override
            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                return false;
            }

            @Override
            public String grantReviewKit(Player player) {
                return "unused";
            }

            @Override
            public String cleanReviewInventory(Player player) {
                return "unused";
            }

            @Override
            public void sendMessage(Player player, String message) {
            }
        }).process(null);

        assertTrue(tasks.pendingTerraReviewKitGrants().isEmpty());
        assertEquals("skipped", events.get(1).get("phase"));
        assertEquals("terra-review-kit-grant", events.get(1).get("taskType"));
    }

    @Test
    void styleTestMobProcessorCompletesUnavailableSpawnWithSkippedEvidence() {
        MotmRuntimeTasks tasks = new MotmRuntimeTasks();
        List<Map<String, Object>> events = captureEvents(tasks);
        tasks.requestStyleTestMobSpawn("player", "standard");

        new StyleTestMobRuntimeTaskProcessor(tasks, new StyleTestMobRuntimeTaskProcessor.Hooks() {
            @Override
            public Player runtimePlayer(String playerId) {
                return null;
            }

            @Override
            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                return false;
            }

            @Override
            public String spawnStyleTestMobsNow(String playerId, Player runtimePlayer, String mode) {
                return "unused";
            }

            @Override
            public String clearStyleTestMobsNow(String playerId, Store<EntityStore> currentStore, Player player) {
                return "unused";
            }

            @Override
            public String countStyleTestMobsNow(String playerId, Store<EntityStore> currentStore, Player player) {
                return "unused";
            }

            @Override
            public int countTrackedStyleTestTargets(String playerId) {
                return 0;
            }

            @Override
            public void recordServerTruth(String type, Map<String, Object> data) {
            }

            @Override
            public void sendMessage(Player player, String message) {
            }
        }).process(null);

        assertTrue(tasks.pendingStyleTestMobSpawns().isEmpty());
        assertEquals("skipped", events.get(1).get("phase"));
        assertEquals("style-test-mob-spawn", events.get(1).get("taskType"));
    }

    private static List<Map<String, Object>> captureEvents(MotmRuntimeTasks tasks) {
        List<Map<String, Object>> events = new ArrayList<>();
        tasks.setEvidenceSink((phase, taskType, playerId, details) -> events.add(Map.of(
                "phase", phase,
                "taskType", taskType,
                "playerId", String.valueOf(playerId),
                "details", details
        )));
        return events;
    }
}
