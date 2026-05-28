package com.motm.runtime.ability.terrain;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerrainRuntimeStateTest {

    @Test
    void ownsTerrainCollectionsBehindIntentMethods() {
        TerrainRuntimeState state = new TerrainRuntimeState();
        state.addSelection(new TemporaryTerrainSelection("one", null, null, null, 1000L));
        state.addMovingTrail(new ActiveMovingTerrainTrail("trail", null, null, null, 2000L, 0L));
        state.addStackingColumn(new ActiveStackingColumn("column", null, null, 1, 2, 3000L, 0L));

        assertEquals(1, state.activeSelectionCount());
        assertEquals(1, state.movingTrailCount());
        assertEquals(1, state.stackingColumnCount());

        AtomicInteger processed = new AtomicInteger();
        state.removeProcessedSelections(selection -> {
            processed.incrementAndGet();
            return true;
        });
        state.removeProcessedMovingTrails(trail -> true);
        state.removeProcessedStackingColumns(column -> true);

        assertEquals(1, processed.get());
        assertEquals(0, state.activeSelectionCount());
        assertEquals(0, state.movingTrailCount());
        assertEquals(0, state.stackingColumnCount());
    }
}
