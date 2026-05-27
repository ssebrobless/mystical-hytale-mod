package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveTerrainStateTest {

    @Test
    void movingTrailOwnsTimingAndDefensivePositionCopies() {
        ActiveMovingTerrainTrail trail = new ActiveMovingTerrainTrail(
                "frolick",
                null,
                null,
                List.of("Plant_Flower_Common_Purple", "Plant_Flower_Common_Yellow"),
                5_000L,
                1_000L
        );

        assertTrue(trail.readyToPlace(1_000L));
        assertTrue(trail.expired(5_000L));
        assertArrayEquals(
                new String[] {"Plant_Flower_Common_Purple", "Plant_Flower_Common_Yellow"},
                trail.blockIdArray());

        Vector3d position = new Vector3d(1.0, 2.0, 3.0);
        trail.initializeLastPosition(position);
        assertNotSame(position, trail.lastPosition());
        position.x = 99.0;
        assertEquals(1.0, trail.lastPosition().x, 0.0001);

        trail.scheduleNextPlacement(1_250L);
        assertEquals(1_250L, trail.nextPlaceAtMillis());
    }

    @Test
    void stackingColumnStagesFromRuntimeOwnedState() {
        Vector3i anchor = new Vector3i(2, 3, 4);
        ActiveStackingColumn column = new ActiveStackingColumn(
                "stone_pillar",
                null,
                anchor,
                42,
                3,
                5_000L,
                1_000L
        );

        assertEquals(3, column.height());
        assertEquals(0, column.placedHeight());
        assertEquals(2, column.nextBlockAnchor().getX());
        assertEquals(3, column.nextBlockAnchor().getY());

        column.markStagePlaced(1_000L);

        assertEquals(1, column.placedHeight());
        assertEquals(1_000L + TerrainRuntimeSpecs.STACKING_COLUMN_STAGE_INTERVAL_MS, column.nextStageAtMillis());
        assertEquals(4, column.nextBlockAnchor().getY());
        assertNotSame(anchor, column.anchor());
    }
}
