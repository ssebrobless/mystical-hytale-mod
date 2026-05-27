package com.motm.runtime.player;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.motm.model.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerSessionLifecycleActionsTest {

    @Test
    void joinRehydratesClassedPlayersAndQueuesResourceSync() {
        Hooks hooks = new Hooks();
        PlayerData player = player("player-a");
        player.setPlayerClass("terra");
        player.setRace("dryad");
        hooks.player = player;
        PlayerSessionLifecycleActions actions = new PlayerSessionLifecycleActions(hooks, Logger.getLogger("test"));

        actions.onPlayerJoin("player-a", "Ada");

        assertEquals(List.of(
                "player-join",
                "rested-login",
                "reapply-perks",
                "resource-sync-persistent",
                "resource-init",
                "hydro-sync",
                "race-bonuses",
                "class-passive-join"
        ), hooks.events);
    }

    @Test
    void connectInitializesPlayerAndRebuildsSavedLoadout() {
        Hooks hooks = new Hooks();
        PlayerData player = player("player-a");
        player.setPlayerClass("terra");
        player.setSelectedStyles(List.of("stone_shot"));
        hooks.player = player;
        hooks.markInitializedResult = true;
        PlayerSessionLifecycleActions actions = new PlayerSessionLifecycleActions(hooks, Logger.getLogger("test"));

        actions.onPlayerConnect(identity(), null);

        assertEquals(List.of(
                "put-runtime",
                "causality:player_connect",
                "mark-initialized",
                "player-join",
                "rested-login",
                "reapply-perks",
                "resource-sync-persistent",
                "resource-init",
                "hydro-sync",
                "class-passive-join",
                "rebuild-runtime",
                "ensure-spellbook",
                "player-has-spellbook",
                "progression-refresh"
        ), hooks.events);
    }

    @Test
    void readyAppliesDevReadinessAndQueuesHudInstall() {
        Hooks hooks = new Hooks();
        hooks.devToolsEnabled = true;
        hooks.ensureSpellbookResult = false;
        PlayerSessionLifecycleActions actions = new PlayerSessionLifecycleActions(hooks, Logger.getLogger("test"));

        actions.onPlayerReady(identity(), null);

        assertEquals(List.of(
                "put-runtime",
                "causality:player_ready",
                "mark-initialized",
                "player-join",
                "status-clear",
                "elemental-clear",
                "free-cast:true",
                "ensure-spellbook",
                "spellbook-grant",
                "hud-install"
        ), hooks.events);
    }

    @Test
    void readyBackfillsPlayerDataWhenConnectCouldNotInitialize() {
        Hooks hooks = new Hooks();
        PlayerData player = player("player-a");
        player.setPlayerClass("terra");
        player.setSelectedStyles(List.of("stone_shot"));
        hooks.playerOnJoin = player;
        hooks.markInitializedResult = true;
        PlayerSessionLifecycleActions actions = new PlayerSessionLifecycleActions(hooks, Logger.getLogger("test"));

        actions.onPlayerReady(identity(), null);

        assertEquals(List.of(
                "put-runtime",
                "causality:player_ready",
                "mark-initialized",
                "player-join",
                "rested-login",
                "reapply-perks",
                "resource-sync-persistent",
                "resource-init",
                "hydro-sync",
                "class-passive-join",
                "rebuild-runtime",
                "ensure-spellbook",
                "player-has-spellbook",
                "progression-refresh",
                "hud-install"
        ), hooks.events);
    }

    @Test
    void disconnectClearsAllPlayerSessionState() {
        Hooks hooks = new Hooks();
        hooks.player = player("player-a");
        hooks.hasRuntimePlayer = true;
        PlayerSessionLifecycleActions actions = new PlayerSessionLifecycleActions(hooks, Logger.getLogger("test"));

        actions.onPlayerDisconnect("player-a");

        assertEquals(List.of(
                "causality:player_disconnect",
                "rested-logout",
                "player-disconnect",
                "style-disconnect",
                "resource-disconnect",
                "class-passive-clear",
                "stat-or-perk-clear",
                "status-clear",
                "elemental-clear",
                "hud-clear",
                "remove-runtime",
                "runtime-tasks-clear",
                "style-test-clear",
                "armed-stomp-clear",
                "progression-clear",
                "free-cast-state-clear",
                "free-cast:false",
                "spellbook-input-clear"
        ), hooks.events);
    }

    private static PlayerSessionLifecycleActions.RuntimePlayerIdentity identity() {
        return new PlayerSessionLifecycleActions.RuntimePlayerIdentity(
                "player-a",
                "Ada",
                "test-world",
                "runtime-player"
        );
    }

    private static PlayerData player(String playerId) {
        PlayerData player = new PlayerData();
        player.setPlayerId(playerId);
        return player;
    }

    private static final class Hooks implements PlayerSessionLifecycleActions.Hooks {
        final List<String> events = new ArrayList<>();
        PlayerData player;
        PlayerData playerOnJoin;
        boolean devToolsEnabled;
        boolean hasRuntimePlayer;
        boolean markInitializedResult;
        boolean ensureSpellbookResult = true;

        @Override
        public PlayerData playerDataOnJoin(String playerId, String playerName) {
            events.add("player-join");
            if (playerOnJoin != null) {
                player = playerOnJoin;
            }
            return player;
        }

        @Override
        public PlayerData playerData(String playerId) {
            return player;
        }

        @Override
        public void playerDataOnDisconnect(String playerId) {
            events.add("player-disconnect");
        }

        @Override
        public PlayerSessionLifecycleActions.RuntimePlayerIdentity runtimePlayerIdentity(Player runtimePlayer) {
            return identity();
        }

        @Override
        public void putRuntimePlayer(String playerId, Player runtimePlayer) {
            events.add("put-runtime");
        }

        @Override
        public boolean markRuntimePlayerInitialized(String playerId) {
            events.add("mark-initialized");
            return markInitializedResult;
        }

        @Override
        public boolean hasRuntimePlayer(String playerId) {
            return hasRuntimePlayer;
        }

        @Override
        public void removeRuntimePlayer(String playerId) {
            events.add("remove-runtime");
        }

        @Override
        public void updateRestedOnLogin(PlayerData player) {
            events.add("rested-login");
        }

        @Override
        public void updateRestedOnLogout(PlayerData player) {
            events.add("rested-logout");
        }

        @Override
        public void reapplyPerks(PlayerData player) {
            events.add("reapply-perks");
        }

        @Override
        public void synchronizePersistentResourceState(PlayerData player) {
            events.add("resource-sync-persistent");
        }

        @Override
        public void initializeResources(PlayerData player) {
            events.add("resource-init");
        }

        @Override
        public void queueHydroContainerSync(String playerId) {
            events.add("hydro-sync");
        }

        @Override
        public void applyRaceBonuses(PlayerData player) {
            events.add("race-bonuses");
        }

        @Override
        public void onClassPassivePlayerJoin(PlayerData player) {
            events.add("class-passive-join");
        }

        @Override
        public void clearClassPassiveState(String playerId) {
            events.add("class-passive-clear");
        }

        @Override
        public boolean hasPendingPerkSelection(PlayerData player) {
            return false;
        }

        @Override
        public int pendingPerkSelectionTier(PlayerData player) {
            return 0;
        }

        @Override
        public void rebuildPlayerRuntimeNow(PlayerData player) {
            events.add("rebuild-runtime");
        }

        @Override
        public boolean ensureSpellbookItem(Player runtimePlayer) {
            events.add("ensure-spellbook");
            return ensureSpellbookResult;
        }

        @Override
        public boolean playerHasSpellbook(Player runtimePlayer) {
            events.add("player-has-spellbook");
            return true;
        }

        @Override
        public void queueSpellbookGrant(String playerId) {
            events.add("spellbook-grant");
        }

        @Override
        public void refreshPlayerProgressionBonuses(String playerId) {
            events.add("progression-refresh");
        }

        @Override
        public boolean devToolsEnabled() {
            return devToolsEnabled;
        }

        @Override
        public void clearStatusEffects(String playerId) {
            events.add("status-clear");
        }

        @Override
        public void clearElementalMarks(String playerId) {
            events.add("elemental-clear");
        }

        @Override
        public void setFreeCastEnabled(String playerId, boolean enabled) {
            events.add("free-cast:" + enabled);
        }

        @Override
        public void queueStatusHudInstall(String playerId) {
            events.add("hud-install");
        }

        @Override
        public void styleOnPlayerDisconnect(String playerId) {
            events.add("style-disconnect");
        }

        @Override
        public void resourceOnPlayerDisconnect(String playerId) {
            events.add("resource-disconnect");
        }

        @Override
        public void clearStatModifiersOrPerkTriggers(String playerId) {
            events.add("stat-or-perk-clear");
        }

        @Override
        public void clearStatusHud(String playerId) {
            events.add("hud-clear");
        }

        @Override
        public void clearRuntimeTasks(String playerId) {
            events.add("runtime-tasks-clear");
        }

        @Override
        public void clearStyleTestRuntime(String playerId) {
            events.add("style-test-clear");
        }

        @Override
        public void clearArmedStomp(String playerId) {
            events.add("armed-stomp-clear");
        }

        @Override
        public void clearPlayerProgression(String playerId) {
            events.add("progression-clear");
        }

        @Override
        public void clearFreeCastState(String playerId) {
            events.add("free-cast-state-clear");
        }

        @Override
        public void clearSpellbookInput(String playerId) {
            events.add("spellbook-input-clear");
        }

        @Override
        public void recordCausality(String type, Map<String, Object> data) {
            events.add("causality:" + type);
        }

        @Override
        public long nowMs() {
            return 1000L;
        }
    }
}
