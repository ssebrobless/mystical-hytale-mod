package com.motm.runtime.ability.terrain;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LapidaryGemRuntimeStateTest {

    @Test
    void ownsActiveGemCollectionAndProcessedRemoval() {
        LapidaryGemRuntimeState state = new LapidaryGemRuntimeState();
        state.addGem(gem("player-a", 1.0));
        state.addGem(gem("player-b", 2.0));

        state.removeProcessedGems(gem -> "player-a".equals(gem.ownerPlayerId()));

        assertEquals(1, state.activeGemCount());
        assertEquals(2.0, state.firstCenterForPlayer("player-b", null).x, 0.0001);
    }

    @Test
    void removesPlayerGemsAndRunsCleanup() {
        LapidaryGemRuntimeState state = new LapidaryGemRuntimeState();
        List<String> cleaned = new ArrayList<>();
        state.addGem(gem("player", 1.0));
        state.addGem(gem("other", 2.0));

        int removed = state.removeGemsForPlayer("player", gem -> {
            cleaned.add(gem.ownerPlayerId());
            return true;
        });

        assertEquals(1, removed);
        assertEquals(List.of("player"), cleaned);
        assertEquals(1, state.activeGemCount());
    }

    @Test
    void resolvesFirstMatchingCenter() {
        LapidaryGemRuntimeState state = new LapidaryGemRuntimeState();
        state.addGem(gem("player", 1.0));
        state.addGem(gem("player", 2.0));

        assertEquals(2.0, state.firstCenterForPlayer("player", gem -> gem.center().x > 1.5).x, 0.0001);
        assertNull(state.firstCenterForPlayer("missing", null));
    }

    private static ActiveLapidaryGem gem(String ownerPlayerId, double x) {
        return new ActiveLapidaryGem(
                ownerPlayerId,
                null,
                new Vector3d(x, 2.0, 3.0),
                10.0,
                10.0,
                5000L,
                "label"
        );
    }
}
