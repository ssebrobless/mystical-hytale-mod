package com.motm.runtime.player;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.motm.manager.MobScalingManager;
import com.motm.manager.PlayerDataManager;
import com.motm.model.ClassData;
import com.motm.model.MobStats;
import com.motm.model.PlayerData;
import com.motm.model.ScaledMobResult;
import com.motm.runtime.state.TargetHealthRuntimeState;
import com.motm.util.DataLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobSpawnRuntimeActionsTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsNullWhenScalingPlayerIsNotOnline() {
        TestDataLoader dataLoader = new TestDataLoader(tempDir);
        MobSpawnRuntimeActions actions = new MobSpawnRuntimeActions(
                new PlayerDataManager(tempDir, dataLoader),
                dataLoader,
                new TestMobScalingManager(dataLoader),
                progressionActions(true, false, 10),
                Logger.getLogger("test")
        );

        assertNull(actions.scale("stone_imp", "missing", null, false, false, false));
    }

    @Test
    void scalesMobSpawnThroughRuntimeOwnerPipeline() {
        TestDataLoader dataLoader = new TestDataLoader(tempDir);
        PlayerDataManager playerDataManager = new PlayerDataManager(tempDir, dataLoader);
        PlayerData player = playerDataManager.onPlayerJoin("player-a", "Ada");
        player.setLevel(22);
        player.setPartySize(3);

        MobSpawnRuntimeActions actions = new MobSpawnRuntimeActions(
                playerDataManager,
                dataLoader,
                new TestMobScalingManager(dataLoader),
                progressionActions(true, false, 10),
                Logger.getLogger("test")
        );

        ScaledMobResult result = actions.scale("stone_imp", "player-a", "cave", true, true, true);

        assertEquals(12, result.level());
        assertEquals("ELITE-12-Jagged", result.displayName());
        assertEquals("color-12-vs-22", result.levelColor());
        assertTrue(result.stats().isElite());
        assertEquals("Jagged", result.stats().getEliteTitle());
        assertEquals(141.0, result.stats().getHealth(), 0.001);
        assertEquals(50.0, result.stats().getXpReward(), 0.001);
    }

    private static PlayerProgressionRuntimeActions progressionActions(boolean scalingCategory,
                                                                      boolean bossCategory,
                                                                      int averageLevel) {
        return new PlayerProgressionRuntimeActions(
                new TargetHealthRuntimeState(),
                new PlayerProgressionRuntimeActions.Hooks() {
                    @Override
                    public Player runtimePlayer(String playerId) {
                        return null;
                    }

                    @Override
                    public PlayerData playerData(String playerId) {
                        return null;
                    }

                    @Override
                    public Iterable<Map.Entry<String, Player>> onlineRuntimePlayers() {
                        return List.of();
                    }

                    @Override
                    public Collection<PlayerData> allOnlinePlayers() {
                        return List.of();
                    }

                    @Override
                    public int averageOnlineLevel(Collection<PlayerData> players) {
                        return averageLevel;
                    }

                    @Override
                    public ClassData classData(String classId) {
                        return null;
                    }

                    @Override
                    public boolean freeCastEnabled(String playerId) {
                        return false;
                    }

                    @Override
                    public boolean isScalingCategory(String category) {
                        return scalingCategory;
                    }

                    @Override
                    public boolean isBossCategory(String category) {
                        return bossCategory;
                    }

                    @Override
                    public boolean isPlayerInStore(Player player, com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> currentStore) {
                        return true;
                    }
                },
                Logger.getLogger("test")
        );
    }

    private static final class TestDataLoader extends DataLoader {
        TestDataLoader(Path dataDirectory) {
            super(dataDirectory);
        }

        @Override
        public String getMobCategory(String mobType) {
            return "hostile";
        }

        @Override
        public MobStats getMobStats(String mobType) {
            return null;
        }

        @Override
        public int getMobBaseXp(String mobType) {
            return 50;
        }
    }

    private static final class TestMobScalingManager extends MobScalingManager {
        TestMobScalingManager(DataLoader dataLoader) {
            super(dataLoader);
        }

        @Override
        public int assignMobLevel(int playerLevel) {
            return playerLevel + 2;
        }

        @Override
        public MobStats scaleMobStats(MobStats baseStats, int playerLevel, String mobCategory) {
            MobStats scaled = new MobStats(baseStats);
            scaled.setHealth(baseStats.getXpReward() + playerLevel);
            return scaled;
        }

        @Override
        public MobStats applyPartyScaling(MobStats stats, int partySize) {
            MobStats scaled = new MobStats(stats);
            scaled.setHealth(stats.getHealth() + partySize);
            return scaled;
        }

        @Override
        public MobStats applyNightBonus(MobStats stats) {
            MobStats scaled = new MobStats(stats);
            scaled.setHealth(stats.getHealth() * 2.0);
            return scaled;
        }

        @Override
        public MobStats applyBloodMoonBonus(MobStats stats) {
            MobStats scaled = new MobStats(stats);
            scaled.setHealth(stats.getHealth() + 5.0);
            return scaled;
        }

        @Override
        public MobStats applyDungeonBonus(MobStats stats) {
            MobStats scaled = new MobStats(stats);
            scaled.setHealth(stats.getHealth() + 10.0);
            return scaled;
        }

        @Override
        public boolean canBecomeElite(String mobCategory) {
            return true;
        }

        @Override
        public MobStats tryMakeElite(MobStats stats, String zone, String mobType) {
            MobStats elite = new MobStats(stats);
            elite.setElite(true);
            elite.setEliteTitle("Jagged");
            return elite;
        }

        @Override
        public String formatEliteMobName(String mobType, int level, String eliteTitle) {
            return "ELITE-" + level + "-" + eliteTitle;
        }

        @Override
        public String getLevelColor(int mobLevel, int playerLevel) {
            return "color-" + mobLevel + "-vs-" + playerLevel;
        }
    }
}
