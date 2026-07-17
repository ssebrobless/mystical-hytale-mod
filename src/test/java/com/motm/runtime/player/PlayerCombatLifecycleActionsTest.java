package com.motm.runtime.player;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.motm.model.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerCombatLifecycleActionsTest {

    @Test
    void mobKillAppliesProgressionAndRefreshesOnlyForClassedPlayers() {
        Hooks hooks = new Hooks();
        PlayerData player = player("player-a");
        player.setPlayerClass("terra");
        hooks.player = player;
        PlayerCombatLifecycleActions actions = new PlayerCombatLifecycleActions(hooks);

        actions.onMobKilled("player-a", "mob-1", "stone_imp", 7, true);

        assertEquals(List.of(
                "leveling:stone_imp:7:true",
                "resource:terra",
                "kill-triggers",
                "achievements:mob_killed",
                "progression-refresh",
                "hud-refresh"
        ), hooks.events);
    }

    @Test
    void mobKillIgnoresPlayersWithoutClass() {
        Hooks hooks = new Hooks();
        hooks.player = player("player-a");
        PlayerCombatLifecycleActions actions = new PlayerCombatLifecycleActions(hooks);

        actions.onMobKilled("player-a", "mob-1", "stone_imp", 7, true);

        assertEquals(List.of(), hooks.events);
    }

    @Test
    void playerDeathOwnsDeathCleanupSequence() {
        Hooks hooks = new Hooks();
        PlayerData player = player("player-a");
        player.getStatistics().setDeaths(2);
        player.setComboCount(5);
        player.setLastKillTime(123L);
        hooks.player = player;
        PlayerCombatLifecycleActions actions = new PlayerCombatLifecycleActions(hooks);

        actions.onPlayerDeath("player-a");

        assertEquals(3, player.getStatistics().getDeaths());
        assertEquals(0, player.getComboCount());
        assertEquals(null, player.getLastKillTime());
        assertEquals(List.of(
                "class-passive-death",
                "status-clear",
                "elemental-clear",
                "armed-stomp-clear",
                "hud-refresh"
        ), hooks.events);
    }

    private static PlayerData player(String playerId) {
        PlayerData player = new PlayerData();
        player.setPlayerId(playerId);
        return player;
    }

    private static final class Hooks implements PlayerCombatLifecycleActions.Hooks {
        final List<String> events = new ArrayList<>();
        PlayerData player;

        @Override
        public PlayerData playerData(String playerId) {
            return player;
        }

        @Override
        public Player runtimePlayer(String playerId) {
            return null;
        }

        @Override
        public void levelingMobKilled(PlayerData player, String mobType, int mobLevel, boolean isRare) {
            events.add("leveling:" + mobType + ":" + mobLevel + ":" + isRare);
        }

        @Override
        public void resourceMobKilled(String playerId, String playerClass) {
            events.add("resource:" + playerClass);
        }

        @Override
        public void classPassiveMobKilled(PlayerData player, Player runtimePlayer, String mobEntityId) {
            events.add("class-passive-kill");
        }

        @Override
        public void afterMobKilled(PlayerData player, Player runtimePlayer, String mobEntityId) {
            events.add("perk-kill");
        }

        @Override
        public void applyKillTriggers(String playerId, Player runtimePlayer) {
            events.add("kill-triggers");
        }

        @Override
        public void checkAchievements(PlayerData player, String event, Object context) {
            events.add("achievements:" + event);
        }

        @Override
        public void refreshPlayerProgressionBonuses(String playerId) {
            events.add("progression-refresh");
        }

        @Override
        public void refreshStatusHud(String playerId) {
            events.add("hud-refresh");
        }

        @Override
        public void classPassivePlayerDeath(String playerId) {
            events.add("class-passive-death");
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
        public void clearArmedStomp(String playerId) {
            events.add("armed-stomp-clear");
        }
    }
}
