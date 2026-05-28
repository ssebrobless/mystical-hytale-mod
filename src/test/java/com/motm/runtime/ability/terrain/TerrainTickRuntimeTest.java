package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.component.Ref;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainTickRuntimeTest {
    private final TerrainTickRuntime runtime = new TerrainTickRuntime();

    @Test
    void restoresExpiredTemporarySelectionOnlyWhenItBelongsToCurrentWorld() {
        RecordingHooks hooks = new RecordingHooks();
        TemporaryTerrainSelection selection =
                new TemporaryTerrainSelection("trail", null, new Vector3i(1, 2, 3), null, 1_000L);

        assertFalse(runtime.processTemporarySelection(selection, 999L, hooks));
        assertEquals(List.of(), hooks.events);

        assertTrue(runtime.processTemporarySelection(selection, 1_000L, hooks));
        assertEquals(List.of("selectionWorld", "restore"), hooks.events);
    }

    @Test
    void movingTrailWaitsUntilReadyAndCurrentOwnerStore() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveMovingTerrainTrail trail = movingTrail(5_000L, 2_000L);

        assertFalse(runtime.processMovingTrail(trail, 1_000L, hooks));
        assertEquals(List.of(), hooks.events);

        hooks.ownerBelongs = false;
        assertFalse(runtime.processMovingTrail(trail, 2_000L, hooks));
        assertEquals(List.of("ownerStore"), hooks.events);
    }

    @Test
    void movingTrailRemovesWhenOwnerPositionIsUnavailable() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.ownerPosition = null;
        ActiveMovingTerrainTrail trail = movingTrail(5_000L, 1_000L);

        assertTrue(runtime.processMovingTrail(trail, 1_000L, hooks));
        assertEquals(List.of("ownerStore", "position"), hooks.events);
    }

    @Test
    void movingTrailWarnsAndRemovesWhenBlockTypeCannotBeResolved() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.blockTypeId = 0;
        ActiveMovingTerrainTrail trail = movingTrail(5_000L, 1_000L);

        assertTrue(runtime.processMovingTrail(trail, 1_000L, hooks));
        assertEquals(List.of("ownerStore", "position", "block", "usable", "warn"), hooks.events);
    }

    @Test
    void movingTrailSchedulesStationaryRecheckWhenOwnerHasNotMovedEnough() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.ownerPosition = new Vector3d(0.2, 0.0, 0.2);
        ActiveMovingTerrainTrail trail = movingTrail(5_000L, 1_000L);
        trail.initializeLastPosition(new Vector3d(0.0, 0.0, 0.0));

        assertFalse(runtime.processMovingTrail(trail, 1_000L, hooks));
        assertEquals(1_000L + TerrainRuntimeSpecs.MOVING_TRAIL_STATIONARY_RECHECK_MS, trail.nextPlaceAtMillis());
        assertEquals(List.of("ownerStore", "position", "block", "usable"), hooks.events);
    }

    @Test
    void movingTrailPlacesSurfaceStampsAndSchedulesPlacedRecheck() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.ownerPosition = new Vector3d(0.0, 0.0, 5.0);
        ActiveMovingTerrainTrail trail = movingTrail(10_000L, 1_000L);
        trail.initializeLastPosition(new Vector3d(0.0, 0.0, 0.0));

        assertFalse(runtime.processMovingTrail(trail, 1_000L, hooks));

        assertTrue(hooks.stamps.size() > 0);
        assertTrue(hooks.stamps.size() <= TerrainRuntimeSpecs.MOVING_TRAIL_MAX_STAMPS);
        assertEquals(1_000L + TerrainRuntimeSpecs.MOVING_TRAIL_PLACED_RECHECK_MS, trail.nextPlaceAtMillis());
        assertEquals(5.0, trail.lastPosition().z, 0.0001);
        assertEquals(hooks.stamps.getLast().z, trail.lastAnchor().z);
    }

    @Test
    void stackingColumnStagesOneBlockAndCompletesWhenHeightReached() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveStackingColumn column = new ActiveStackingColumn(
                "pillar",
                null,
                new Vector3i(1, 2, 3),
                42,
                1,
                5_000L,
                1_000L
        );

        assertTrue(runtime.processStackingColumn(column, 1_000L, hooks));

        assertEquals(1, column.placedHeight());
        assertEquals(List.of("columnWorld", "columnStage"), hooks.events);
        assertEquals(1, hooks.stamps.getFirst().x);
        assertEquals(2, hooks.stamps.getFirst().y);
    }

    @Test
    void stackingColumnWaitsWhenWorldDoesNotMatchOrStageIsNotReady() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveStackingColumn column = new ActiveStackingColumn(
                "pillar",
                null,
                new Vector3i(1, 2, 3),
                42,
                2,
                5_000L,
                2_000L
        );

        assertFalse(runtime.processStackingColumn(column, 1_000L, hooks));
        assertEquals(List.of("columnWorld"), hooks.events);

        hooks.events.clear();
        hooks.columnBelongs = false;
        assertFalse(runtime.processStackingColumn(column, 2_000L, hooks));
        assertEquals(List.of("columnWorld"), hooks.events);
    }

    private static ActiveMovingTerrainTrail movingTrail(long expireAtMillis, long nextPlaceAtMillis) {
        return new ActiveMovingTerrainTrail(
                "trail",
                null,
                new TestRef(),
                List.of("Flower"),
                expireAtMillis,
                nextPlaceAtMillis
        );
    }

    private static final class TestRef extends Ref<EntityStore> {
        private TestRef() {
            super(null, 1);
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    private static final class RecordingHooks implements TerrainTickRuntime.Hooks {
        private final List<String> events = new ArrayList<>();
        private final List<Vector3i> stamps = new ArrayList<>();
        private boolean selectionBelongs = true;
        private boolean columnBelongs = true;
        private boolean ownerBelongs = true;
        private Vector3d ownerPosition = new Vector3d(0.0, 0.0, 0.0);
        private int blockTypeId = 42;

        @Override
        public boolean belongsToCurrentWorld(TemporaryTerrainSelection selection) {
            events.add("selectionWorld");
            return selectionBelongs;
        }

        @Override
        public boolean belongsToCurrentWorld(ActiveStackingColumn column) {
            events.add("columnWorld");
            return columnBelongs;
        }

        @Override
        public void restoreSelection(TemporaryTerrainSelection selection) {
            events.add("restore");
        }

        @Override
        public boolean ownerBelongsToCurrentStore(ActiveMovingTerrainTrail trail) {
            events.add("ownerStore");
            return ownerBelongs;
        }

        @Override
        public Vector3d ownerPosition(ActiveMovingTerrainTrail trail) {
            events.add("position");
            return ownerPosition;
        }

        @Override
        public int resolveBlockTypeId(ActiveMovingTerrainTrail trail) {
            events.add("block");
            return blockTypeId;
        }

        @Override
        public boolean usableBlockType(int blockTypeId) {
            events.add("usable");
            return blockTypeId > 0;
        }

        @Override
        public void warnMissingBlockType(ActiveMovingTerrainTrail trail) {
            events.add("warn");
        }

        @Override
        public Vector3i surfaceOverlayAnchor(Vector3d center) {
            events.add("anchor");
            return new Vector3i((int) Math.round(center.x), (int) Math.round(center.y), (int) Math.round(center.z));
        }

        @Override
        public void placeTrailStamp(ActiveMovingTerrainTrail trail,
                                    Vector3i anchor,
                                    Vector3i right,
                                    int blockTypeId,
                                    long expireAtMillis) {
            events.add("trailStamp");
            stamps.add(anchor);
        }

        @Override
        public void placeColumnStage(ActiveStackingColumn column, Vector3i block) {
            events.add("columnStage");
            stamps.add(block);
        }
    }
}
