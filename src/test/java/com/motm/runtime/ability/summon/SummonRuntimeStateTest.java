package com.motm.runtime.ability.summon;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SummonRuntimeStateTest {
    private static final Gson GSON = new Gson();

    @Test
    void ownsSummonOwnerIndexAndCounts() {
        SummonRuntimeState state = new SummonRuntimeState();

        state.addSummon("player-a", summon("player-a", "snow_imp"));
        state.addSummon("player-a", summon("player-a", "snow_imp"));
        state.addSummon("player-b", summon("player-b", "shadow_clone"));

        assertEquals(2, state.activeOwnerCount());
        assertEquals(3, state.activeSummonCount());
        assertEquals(2, state.summonCountForOwner("player-a"));
        assertEquals(0, state.summonCountForOwner("missing"));
    }

    @Test
    void returnsImmutableOwnerSnapshot() {
        SummonRuntimeState state = new SummonRuntimeState();
        state.addSummon("player", summon("player", "snow_imp"));

        List<ActiveSummon> snapshot = state.summonsForOwner("player");

        assertEquals(1, snapshot.size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(summon("player", "snow_imp")));
    }

    @Test
    void removesOwnerSummonsAndRunsCleanup() {
        SummonRuntimeState state = new SummonRuntimeState();
        List<String> cleaned = new ArrayList<>();
        state.addSummon("player", summon("player", "snow_imp"));
        state.addSummon("player", summon("player", "shadow_clone"));

        int removed = state.removeSummonsForPlayer("player", summon -> {
            cleaned.add(summon.ownerPlayerId());
            return true;
        });

        assertEquals(2, removed);
        assertEquals(List.of("player", "player"), cleaned);
        assertEquals(0, state.activeOwnerCount());
        assertEquals(0, state.activeSummonCount());
    }

    @Test
    void removesProcessedSummonsAndDropsEmptyOwners() {
        SummonRuntimeState state = new SummonRuntimeState();
        state.addSummon("player-a", summon("player-a", "snow_imp"));
        state.addSummon("player-b", summon("player-b", "shadow_clone"));

        state.removeProcessedSummons(summon -> "player-a".equals(summon.ownerPlayerId()));

        assertEquals(1, state.activeOwnerCount());
        assertEquals(1, state.activeSummonCount());
        assertEquals(0, state.summonCountForOwner("player-a"));
        assertEquals(1, state.summonCountForOwner("player-b"));
    }

    private static ActiveSummon summon(String ownerPlayerId, String summonName) {
        AbilityData ability = ability(summonName);
        return new ActiveSummon(
                ownerPlayerId,
                null,
                null,
                "hydro",
                "snow",
                ability,
                SummonRuntimeSpecs.resolve(ability),
                1000L,
                10_000L,
                1200L,
                2000L,
                0L,
                12.0,
                null,
                0L,
                false
        );
    }

    private static AbilityData ability(String summonName) {
        return GSON.fromJson(
                "{\"id\":\"" + summonName + "\",\"cast_type\":\"summon\",\"summon_name\":\"" + summonName + "\"}",
                AbilityData.class);
    }
}
