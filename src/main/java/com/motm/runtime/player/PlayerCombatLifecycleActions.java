package com.motm.runtime.player;

import java.util.logging.Logger;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.motm.model.PlayerData;

/**
 * Owns player combat lifecycle side effects that are triggered by runtime
 * kill/death events.
 */
public final class PlayerCombatLifecycleActions {

    private static final Logger LOG = Logger.getLogger("MOTM");
    private boolean perkKillCreditObserved;

    private final Hooks hooks;

    public PlayerCombatLifecycleActions(Hooks hooks) {
        this.hooks = hooks;
    }

    public void onMobKilled(String playerId, String mobEntityId, String mobType, int mobLevel, boolean isRare) {
        PlayerData player = hooks.playerData(playerId);
        if (player == null || player.getPlayerClass() == null) {
            return;
        }

        hooks.levelingMobKilled(player, mobType, mobLevel, isRare);
        hooks.resourceMobKilled(playerId, player.getPlayerClass());
        Player runtimePlayer = hooks.runtimePlayer(playerId);
        if (runtimePlayer != null) {
            hooks.classPassiveMobKilled(player, runtimePlayer, mobEntityId);
            hooks.afterMobKilled(player, runtimePlayer, mobEntityId);
            if (!perkKillCreditObserved) {
                perkKillCreditObserved = true;
                LOG.info("[MOTM] event=perk_kill_credit playerId=" + playerId
                        + " mobEntityId=" + mobEntityId);
            }

        }
        hooks.applyKillTriggers(playerId, runtimePlayer);
        hooks.checkAchievements(player, "mob_killed", null);
        hooks.refreshPlayerProgressionBonuses(playerId);
        hooks.refreshStatusHud(playerId);
    }

    public void onPlayerDeath(String playerId) {
        PlayerData player = hooks.playerData(playerId);
        if (player == null) {
            return;
        }

        player.getStatistics().setDeaths(player.getStatistics().getDeaths() + 1);
        player.setComboCount(0);
        player.setLastKillTime(null);
        hooks.classPassivePlayerDeath(playerId);
        hooks.clearStatusEffects(playerId);
        hooks.clearElementalMarks(playerId);
        hooks.clearArmedStomp(playerId);
        hooks.refreshStatusHud(playerId);
    }

    public interface Hooks {
        PlayerData playerData(String playerId);

        Player runtimePlayer(String playerId);

        void levelingMobKilled(PlayerData player, String mobType, int mobLevel, boolean isRare);

        void resourceMobKilled(String playerId, String playerClass);

        void classPassiveMobKilled(PlayerData player, Player runtimePlayer, String mobEntityId);
        void afterMobKilled(PlayerData player, Player runtimePlayer, String mobEntityId);


        void applyKillTriggers(String playerId, Player runtimePlayer);

        void checkAchievements(PlayerData player, String event, Object context);

        void refreshPlayerProgressionBonuses(String playerId);

        void refreshStatusHud(String playerId);

        void classPassivePlayerDeath(String playerId);

        void clearStatusEffects(String playerId);

        void clearElementalMarks(String playerId);

        void clearArmedStomp(String playerId);
    }
}
