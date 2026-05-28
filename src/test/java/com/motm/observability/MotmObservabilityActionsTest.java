package com.motm.observability;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.PlayerData;
import com.motm.model.StatusEffect;
import com.motm.util.MotmObservability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MotmObservabilityActionsTest {

    @TempDir
    Path tempDir;

    @Test
    void delegatesRunControlAndSnapshotsToObservabilityOwner() throws Exception {
        PlayerData player = new PlayerData();
        player.setPlayerId("player-1");
        player.setPlayerName("Tester");
        MotmObservability observability = new MotmObservability(tempDir);
        MotmObservabilityActions actions = new MotmObservabilityActions(
                snapshotBuilder(),
                hooks(observability, player)
        );

        String start = actions.startRun("run-a", "scenario-a", "player-1");
        assertTrue(start.contains("runId=run-a"));
        assertTrue(actions.setScenario("scenario-b").contains("scenario-b"));
        assertTrue(actions.mark("player-1", "ready").contains("ready"));
        assertTrue(actions.snapshot("player-1", "snap").contains("run-a"));
        assertTrue(actions.status().contains("run-a"));
        assertTrue(actions.stopRun("done").contains("run-a"));
        assertTrue(Files.exists(tempDir.resolve("observability/runs/run-a/manifest.runtime.json")));
        assertTrue(Files.exists(tempDir.resolve("observability/runs/run-a/server-truth.jsonl")));
    }

    @Test
    void reportsUnavailableWhenNoObservabilityExists() {
        MotmObservabilityActions actions = new MotmObservabilityActions(null, hooks(null, null));

        assertTrue(actions.startRun("run", "scenario", "player").contains("unavailable"));
        assertTrue(actions.status().contains("unavailable"));
    }

    private MotmObservabilityActions.Hooks hooks(MotmObservability observability, PlayerData playerData) {
        return new MotmObservabilityActions.Hooks() {
            @Override
            public boolean devToolsEnabled() {
                return true;
            }

            @Override
            public String devToolsDisabledMessage() {
                return "disabled";
            }

            @Override
            public MotmObservability observability() {
                return observability;
            }

            @Override
            public PlayerData playerData(String playerId) {
                return playerData;
            }

            @Override
            public String buildChannel() {
                return "internal";
            }

            @Override
            public boolean internalTestBuild() {
                return true;
            }

            @Override
            public Path pluginDirectory() {
                return tempDir;
            }
        };
    }

    private static MotmObservabilitySnapshotBuilder snapshotBuilder() {
        return new MotmObservabilitySnapshotBuilder(new MotmObservabilitySnapshotBuilder.Hooks() {
            @Override
            public String buildChannel() {
                return "internal";
            }

            @Override
            public boolean internalTestBuild() {
                return true;
            }

            @Override
            public boolean devToolsEnabled() {
                return true;
            }

            @Override
            public String packetScope() {
                return "key";
            }

            @Override
            public Path pluginDirectory() {
                return null;
            }

            @Override
            public Map<String, Object> runtimeTasksSnapshot() {
                return Map.of();
            }

            @Override
            public int onlineRuntimePlayerCount() {
                return 0;
            }

            @Override
            public int activeProofSelections() {
                return 0;
            }

            @Override
            public int activeProofProxies() {
                return 0;
            }

            @Override
            public int activeStyleTests() {
                return 0;
            }

            @Override
            public int freeCastPlayerCount() {
                return 0;
            }

            @Override
            public Map<String, Object> activeRuntimeSnapshot(String playerId) {
                return Map.of();
            }

            @Override
            public Player runtimePlayer(String playerId) {
                return null;
            }

            @Override
            public PlayerData playerData(String playerId) {
                return null;
            }

            @Override
            public boolean freeCastEnabled(String playerId) {
                return false;
            }

            @Override
            public List<StatusEffect> statusEffects(String playerId) {
                return List.of();
            }

            @Override
            public CombinedItemContainer combinedInventory(Player player) {
                return null;
            }

            @Override
            public List<Ref<EntityStore>> trackedTargets(String playerId) {
                return List.of();
            }

            @Override
            public String runtimePlayerId(Player player) {
                return null;
            }

            @Override
            public PlayerRef universePlayerRef(Player player) {
                return null;
            }
        });
    }
}
