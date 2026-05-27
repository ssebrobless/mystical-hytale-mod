package com.motm.runtime.player;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.ClassData;
import com.motm.model.PlayerData;
import com.motm.runtime.state.TargetHealthRuntimeState;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Applies runtime stat mutations derived from player progression and resolves
 * world-scoped level anchors for gameplay systems.
 */
public final class PlayerProgressionRuntimeActions {

    private static final String PLAYER_LEVEL_HEALTH_MODIFIER_ID = "motm_player_level_health";

    private final TargetHealthRuntimeState targetHealthState;
    private final Hooks hooks;
    private final Logger log;

    public PlayerProgressionRuntimeActions(TargetHealthRuntimeState targetHealthState,
                                           Hooks hooks,
                                           Logger log) {
        this.targetHealthState = targetHealthState;
        this.hooks = hooks;
        this.log = log;
    }

    public int averageOnlinePlayerLevel() {
        return hooks.averageOnlineLevel(hooks.allOnlinePlayers());
    }

    public int averageOnlinePlayerLevelForPlayer(String playerId) {
        if (playerId == null) {
            return averageOnlinePlayerLevel();
        }

        Player runtimePlayer = hooks.runtimePlayer(playerId);
        if (runtimePlayer == null) {
            return averageOnlinePlayerLevel();
        }

        World world = runtimePlayer.getWorld();
        if (world == null) {
            return averageOnlinePlayerLevel();
        }

        return averageOnlinePlayerLevelForWorld(world);
    }

    public void refreshPlayerProgressionBonusesNow(String playerId) {
        if (playerId == null) {
            return;
        }

        Player runtimePlayer = hooks.runtimePlayer(playerId);
        PlayerData playerData = hooks.playerData(playerId);
        if (runtimePlayer == null) {
            targetHealthState.clear(playerId);
            return;
        }
        if (playerData == null || playerData.getPlayerClass() == null) {
            clearPlayerLevelHealthBonus(runtimePlayer, playerId);
            return;
        }

        applyPlayerLevelHealthBonus(runtimePlayer, playerData);
    }

    public void refreshAllPlayerProgressionBonuses(Store<EntityStore> currentStore) {
        for (Map.Entry<String, Player> entry : hooks.onlineRuntimePlayers()) {
            Player player = entry.getValue();
            if (hooks.isPlayerInStore(player, currentStore)) {
                refreshPlayerProgressionBonusesNow(entry.getKey());
            }
        }
    }

    public void clearPlayer(String playerId) {
        if (playerId != null) {
            targetHealthState.clear(playerId);
        }
    }

    public int mobScalingAnchorLevel(String category, String playerId, PlayerData player) {
        if (player == null) {
            return 1;
        }

        if (!hooks.isScalingCategory(category) && !hooks.isBossCategory(category)) {
            return player.getLevel();
        }

        return Math.max(1, averageOnlinePlayerLevelForPlayer(playerId));
    }

    private void clearPlayerLevelHealthBonus(Player runtimePlayer, String playerId) {
        if (runtimePlayer == null) {
            if (playerId != null) {
                targetHealthState.clear(playerId);
            }
            return;
        }

        try {
            var playerRef = runtimePlayer.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                return;
            }

            EntityStatMap entityStatMap = playerRef.getStore().getComponent(playerRef, EntityStatMap.getComponentType());
            if (entityStatMap == null) {
                return;
            }

            entityStatMap.removeModifier(DefaultEntityStatTypes.getHealth(), PLAYER_LEVEL_HEALTH_MODIFIER_ID);
            if (playerId != null) {
                targetHealthState.clear(playerId);
            }
        } catch (IllegalStateException e) {
            log.warning("[MOTM] Skipped clearing progression health bonus on the wrong store for "
                    + (playerId == null ? "unknown" : playerId) + ": " + e.getMessage());
        }
    }

    private void applyPlayerLevelHealthBonus(Player runtimePlayer, PlayerData playerData) {
        if (runtimePlayer == null || playerData == null) {
            return;
        }

        try {
            var playerRef = runtimePlayer.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                return;
            }

            EntityStatMap entityStatMap = playerRef.getStore().getComponent(playerRef, EntityStatMap.getComponentType());
            if (entityStatMap == null) {
                return;
            }

            String classId = playerData.getPlayerClass();
            ClassData classData = classId == null ? null : hooks.classData(classId);
            if (classData == null || classData.getStartingStats() == null) {
                return;
            }

            double baseHealth = classData.getStartingStats().getOrDefault("health", 100.0);
            Map<String, Double> growth = classData.getStatGrowthPerLevel();
            double healthGrowth = growth == null ? 0.0 : growth.getOrDefault("health", 0.0);
            double targetHealth = baseHealth + (Math.max(1, playerData.getLevel()) - 1) * healthGrowth;
            if (!Double.isFinite(targetHealth) || targetHealth <= 0.0) {
                return;
            }

            String playerId = playerData.getPlayerId();
            Double lastAppliedTargetHealth = targetHealthState.get(playerId);
            if (lastAppliedTargetHealth != null && Math.abs(lastAppliedTargetHealth - targetHealth) < 0.01) {
                return;
            }

            EntityStatValue healthBefore = entityStatMap.get(DefaultEntityStatTypes.getHealth());
            float currentHealthBefore = healthBefore != null ? healthBefore.get() : 0.0f;
            float maxHealthBefore = healthBefore != null ? healthBefore.getMax() : 0.0f;
            boolean shouldMaximizeAfterApply = playerId != null && hooks.freeCastEnabled(playerId)
                    || (maxHealthBefore > 0.0f && currentHealthBefore >= maxHealthBefore - 0.5f);
            float previousHealthRatio = maxHealthBefore > 0.0f
                    ? Math.max(0.0f, Math.min(1.0f, currentHealthBefore / maxHealthBefore))
                    : 1.0f;

            entityStatMap.removeModifier(DefaultEntityStatTypes.getHealth(), PLAYER_LEVEL_HEALTH_MODIFIER_ID);

            EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
            if (health == null || health.getMax() <= 0.0f) {
                return;
            }

            float healthMultiplier = (float) (targetHealth / health.getMax());
            if (!Float.isFinite(healthMultiplier) || healthMultiplier <= 0f) {
                return;
            }

            if (Math.abs(healthMultiplier - 1.0f) > 0.0001f) {
                entityStatMap.putModifier(
                        DefaultEntityStatTypes.getHealth(),
                        PLAYER_LEVEL_HEALTH_MODIFIER_ID,
                        new StaticModifier(
                                Modifier.ModifierTarget.MAX,
                                StaticModifier.CalculationType.MULTIPLICATIVE,
                                healthMultiplier
                        )
                );
            }

            EntityStatValue updatedHealth = entityStatMap.get(DefaultEntityStatTypes.getHealth());
            if (updatedHealth != null && updatedHealth.getMax() > 0.0f) {
                if (shouldMaximizeAfterApply) {
                    entityStatMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
                } else {
                    float desiredCurrentHealth = updatedHealth.getMax() * previousHealthRatio;
                    float missingHealth = desiredCurrentHealth - updatedHealth.get();
                    if (missingHealth > 0.05f) {
                        entityStatMap.addStatValue(DefaultEntityStatTypes.getHealth(), missingHealth);
                    }
                }
            }

            if (playerId != null) {
                targetHealthState.remember(playerId, targetHealth);
            }
        } catch (IllegalStateException e) {
            log.warning("[MOTM] Skipped progression health bonus refresh on the wrong store for "
                    + playerData.getPlayerName() + ": " + e.getMessage());
        }
    }

    private int averageOnlinePlayerLevelForWorld(World world) {
        if (world == null) {
            return 1;
        }

        int totalLevels = 0;
        int count = 0;
        for (Map.Entry<String, Player> entry : hooks.onlineRuntimePlayers()) {
            Player candidate = entry.getValue();
            if (candidate == null || candidate.getWorld() != world) {
                continue;
            }

            PlayerData playerData = hooks.playerData(entry.getKey());
            if (playerData == null) {
                continue;
            }

            totalLevels += Math.max(1, playerData.getLevel());
            count++;
        }

        if (count == 0) {
            return averageOnlinePlayerLevel();
        }

        return Math.max(1, (int) Math.round(totalLevels / (double) count));
    }

    public interface Hooks {
        Player runtimePlayer(String playerId);

        PlayerData playerData(String playerId);

        Iterable<Map.Entry<String, Player>> onlineRuntimePlayers();

        Collection<PlayerData> allOnlinePlayers();

        int averageOnlineLevel(Collection<PlayerData> players);

        ClassData classData(String classId);

        boolean freeCastEnabled(String playerId);

        boolean isScalingCategory(String category);

        boolean isBossCategory(String category);

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);
    }
}
