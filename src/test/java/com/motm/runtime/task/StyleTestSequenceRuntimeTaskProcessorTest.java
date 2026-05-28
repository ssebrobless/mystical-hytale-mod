package com.motm.runtime.task;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.runtime.state.ActiveStyleTest;
import com.motm.runtime.state.StyleTestRuntimeState;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class StyleTestSequenceRuntimeTaskProcessorTest {

    @Test
    void stopsUnavailablePlayerAndRecordsServerTruth() {
        StyleTestRuntimeState state = new StyleTestRuntimeState();
        state.start(new ActiveStyleTest(
                "player",
                "terra",
                "stoneguard",
                "Stoneguard",
                List.of("ability"),
                0,
                0L
        ));
        List<Map<String, Object>> events = new ArrayList<>();

        new StyleTestSequenceRuntimeTaskProcessor(
                state,
                new StyleTestTargetResolver(),
                new TestHooks(events)
        ).process(null);

        assertNull(state.get("player"));
        assertEquals(1, events.size());
        assertEquals("style_test_sequence_stopped", events.get(0).get("type"));
        assertEquals("player_unavailable", events.get(0).get("reason"));
    }

    private static final class TestHooks implements StyleTestSequenceRuntimeTaskProcessor.Hooks {
        private final List<Map<String, Object>> events;

        private TestHooks(List<Map<String, Object>> events) {
            this.events = events;
        }

        @Override
        public Player runtimePlayer(String playerId) {
            return null;
        }

        @Override
        public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
            return false;
        }

        @Override
        public PlayerData playerData(String playerId) {
            return null;
        }

        @Override
        public AbilityData findAbility(PlayerData playerData, String abilityId) {
            return null;
        }

        @Override
        public double castTimeSeconds(AbilityData ability) {
            return 0.0;
        }

        @Override
        public double recoveryTimeSeconds(AbilityData ability) {
            return 0.0;
        }

        @Override
        public void queueAbilityCast(String playerId,
                                     String abilityId,
                                     Ref<EntityStore> targetRef,
                                     Vector3i targetBlock,
                                     boolean notifyFailures) {
        }

        @Override
        public void recordServerTruth(String type, Map<String, Object> data) {
            java.util.LinkedHashMap<String, Object> event = new java.util.LinkedHashMap<>(data);
            event.put("type", type);
            events.add(event);
        }

        @Override
        public void sendMessage(Player player, String message) {
        }
    }
}
