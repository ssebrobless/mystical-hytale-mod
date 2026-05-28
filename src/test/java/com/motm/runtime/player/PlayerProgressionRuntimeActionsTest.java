package com.motm.runtime.player;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.motm.model.ClassData;
import com.motm.model.PlayerData;
import com.motm.runtime.state.TargetHealthRuntimeState;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlayerProgressionRuntimeActionsTest {

    @Test
    void mobScalingAnchorUsesPlayerLevelForOrdinaryCategories() {
        PlayerData player = player("player-a", 12);
        PlayerProgressionRuntimeActions actions = actions(false, false, 7);

        assertEquals(12, actions.mobScalingAnchorLevel("ambient", "player-a", player));
    }

    @Test
    void mobScalingAnchorUsesAverageLevelForScalingCategories() {
        PlayerData player = player("player-a", 12);
        PlayerProgressionRuntimeActions actions = actions(true, false, 7);

        assertEquals(7, actions.mobScalingAnchorLevel("hostile", "player-a", player));
    }

    @Test
    void nullPlayerAnchorFallsBackToOne() {
        PlayerProgressionRuntimeActions actions = actions(true, false, 7);

        assertEquals(1, actions.mobScalingAnchorLevel("hostile", "missing", null));
    }

    private static PlayerProgressionRuntimeActions actions(boolean scalingCategory,
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
                java.util.logging.Logger.getLogger("test")
        );
    }

    private static PlayerData player(String id, int level) {
        PlayerData player = new PlayerData();
        player.setPlayerId(id);
        player.setLevel(level);
        return player;
    }
}
