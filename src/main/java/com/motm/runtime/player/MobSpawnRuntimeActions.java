package com.motm.runtime.player;

import com.motm.manager.MobScalingManager;
import com.motm.manager.PlayerDataManager;
import com.motm.model.MobStats;
import com.motm.model.PlayerData;
import com.motm.model.ScaledMobResult;
import com.motm.util.DataLoader;

import java.util.logging.Logger;

/**
 * Owns the player-relative mob spawn scaling pipeline for live Hytale mobs.
 */
public final class MobSpawnRuntimeActions {

    private final PlayerDataManager playerDataManager;
    private final DataLoader dataLoader;
    private final MobScalingManager mobScalingManager;
    private final PlayerProgressionRuntimeActions playerProgressionActions;
    private final Logger log;

    public MobSpawnRuntimeActions(PlayerDataManager playerDataManager,
                                  DataLoader dataLoader,
                                  MobScalingManager mobScalingManager,
                                  PlayerProgressionRuntimeActions playerProgressionActions,
                                  Logger log) {
        this.playerDataManager = playerDataManager;
        this.dataLoader = dataLoader;
        this.mobScalingManager = mobScalingManager;
        this.playerProgressionActions = playerProgressionActions;
        this.log = log;
    }

    public ScaledMobResult scale(String mobType,
                                 String playerId,
                                 String zoneId,
                                 boolean isNight,
                                 boolean isBloodMoon,
                                 boolean isDungeon) {
        if (playerDataManager == null || dataLoader == null || mobScalingManager == null
                || playerProgressionActions == null) {
            return null;
        }

        PlayerData player = playerDataManager.getOnlinePlayer(playerId);
        if (player == null) {
            return null;
        }

        String category = dataLoader.getMobCategory(mobType);
        int progressionAnchorLevel = playerProgressionActions.mobScalingAnchorLevel(category, playerId, player);
        int mobLevel = mobScalingManager.assignMobLevel(progressionAnchorLevel);

        MobStats baseStats = dataLoader.getMobStats(mobType);
        if (baseStats == null) {
            log.warning("[MOTM] Missing base stats for mob type " + mobType + "; using empty fallback.");
            baseStats = new MobStats();
            baseStats.setXpReward(dataLoader.getMobBaseXp(mobType));
        }

        MobStats scaled = mobScalingManager.isBossCategory(category)
                ? mobScalingManager.scaleBossStats(baseStats, progressionAnchorLevel, category)
                : mobScalingManager.scaleMobStats(baseStats, progressionAnchorLevel, category);

        if (player.getPartySize() > 1) {
            scaled = mobScalingManager.applyPartyScaling(scaled, player.getPartySize());
        }

        if (isNight) {
            scaled = mobScalingManager.applyNightBonus(scaled);
        }
        if (isBloodMoon) {
            scaled = mobScalingManager.applyBloodMoonBonus(scaled);
        }
        if (isDungeon) {
            scaled = mobScalingManager.applyDungeonBonus(scaled);
        }

        if (mobScalingManager.canBecomeElite(category)) {
            scaled = mobScalingManager.tryMakeElite(scaled, zoneId, mobType);
        }

        String displayName = scaled.isElite() && scaled.getEliteTitle() != null
                ? mobScalingManager.formatEliteMobName(mobType, mobLevel, scaled.getEliteTitle())
                : mobScalingManager.formatMobName(mobType, mobLevel, category);
        String levelColor = mobScalingManager.getLevelColor(mobLevel, player.getLevel());

        return new ScaledMobResult(scaled, mobLevel, displayName, levelColor);
    }
}
