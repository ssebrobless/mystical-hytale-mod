package com.motm.runtime.player;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.motm.model.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerRuntimeRebuildActionsTest {

    @Test
    void rebuildWithoutClassClearsRuntimeAndRefreshesShellState() {
        RecordingHooks hooks = new RecordingHooks(false);
        PlayerRuntimeRebuildActions actions = new PlayerRuntimeRebuildActions(hooks, Logger.getLogger("test"));
        PlayerData player = player("player-1", null);

        actions.rebuildNow(player);

        assertEquals(List.of(
                "resetCooldowns",
                "clearClassPassiveState",
                "clearStatusEffects",
                "clearElementalMarks",
                "clearArmedStomp",
                "clearResourceState",
                "synchronizePersistentResourceState",
                "refreshProgressionBonusesNow",
                "clearFreeCastInvulnerability",
                "refreshStatusHudNow"
        ), hooks.calls);
    }

    @Test
    void rebuildWithClassReappliesResourcesPerksAndHud() {
        RecordingHooks hooks = new RecordingHooks(true);
        PlayerRuntimeRebuildActions actions = new PlayerRuntimeRebuildActions(hooks, Logger.getLogger("test"));
        PlayerData player = player("player-1", "terra");

        actions.rebuildNow(player);

        assertEquals(List.of(
                "resetCooldowns",
                "clearClassPassiveState",
                "clearStatusEffects",
                "clearElementalMarks",
                "clearArmedStomp",
                "clearResourceState",
                "synchronizePersistentResourceState",
                "initializeResources",
                "reapplyPerks",
                "queueHydroContainerSync",
                "refreshProgressionBonusesNow",
                "onClassPassivePlayerJoin",
                "runtimePlayer",
                "refreshStatusHudNow"
        ), hooks.calls);
    }

    private static PlayerData player(String playerId, String classId) {
        PlayerData player = new PlayerData();
        player.setPlayerId(playerId);
        player.setPlayerClass(classId);
        return player;
    }

    private static final class RecordingHooks implements PlayerRuntimeRebuildActions.Hooks {
        private final boolean freeCastEnabled;
        private final List<String> calls = new ArrayList<>();

        private RecordingHooks(boolean freeCastEnabled) {
            this.freeCastEnabled = freeCastEnabled;
        }

        @Override
        public void resetCooldowns(String playerId) {
            calls.add("resetCooldowns");
        }

        @Override
        public void clearClassPassiveState(String playerId) {
            calls.add("clearClassPassiveState");
        }

        @Override
        public void clearStatusEffects(String playerId) {
            calls.add("clearStatusEffects");
        }

        @Override
        public void clearElementalMarks(String playerId) {
            calls.add("clearElementalMarks");
        }

        @Override
        public void clearArmedStomp(String playerId) {
            calls.add("clearArmedStomp");
        }

        @Override
        public void clearResourceState(String playerId) {
            calls.add("clearResourceState");
        }

        @Override
        public void synchronizePersistentResourceState(PlayerData player) {
            calls.add("synchronizePersistentResourceState");
        }

        @Override
        public void refreshProgressionBonusesNow(String playerId) {
            calls.add("refreshProgressionBonusesNow");
        }

        @Override
        public boolean freeCastEnabled(String playerId) {
            return freeCastEnabled;
        }

        @Override
        public void clearFreeCastInvulnerability(String playerId) {
            calls.add("clearFreeCastInvulnerability");
        }

        @Override
        public void refreshStatusHudNow(String playerId) {
            calls.add("refreshStatusHudNow");
        }

        @Override
        public void initializeResources(PlayerData player) {
            calls.add("initializeResources");
        }

        @Override
        public void reapplyPerks(PlayerData player) {
            calls.add("reapplyPerks");
        }

        @Override
        public void queueHydroContainerSync(String playerId) {
            calls.add("queueHydroContainerSync");
        }

        @Override
        public void onClassPassivePlayerJoin(PlayerData player) {
            calls.add("onClassPassivePlayerJoin");
        }

        @Override
        public Player runtimePlayer(String playerId) {
            calls.add("runtimePlayer");
            return null;
        }

        @Override
        public void ensureFreeCastInvulnerability(Player runtimePlayer) {
            calls.add("ensureFreeCastInvulnerability");
        }
    }
}
