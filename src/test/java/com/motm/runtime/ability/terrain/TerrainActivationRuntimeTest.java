package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

class TerrainActivationRuntimeTest {
    private final TerrainActivationRuntime runtime = new TerrainActivationRuntime();

    @Test
    void createsMovingTrailsGemsAndStackingColumns() {
        TestRef ownerRef = new TestRef();
        ActiveMovingTerrainTrail trail = runtime.createMovingTrail(
                "frolick",
                null,
                ownerRef,
                5_000L,
                1_000L,
                "Plant_Flower_Common_Purple",
                "Plant_Flower_Common_Yellow"
        );

        assertEquals("frolick", trail.reason());
        assertEquals(ownerRef, trail.ownerRef());
        assertEquals(5_000L, trail.expireAtMillis());
        assertEquals(1_000L, trail.nextPlaceAtMillis());
        assertArrayEquals(new String[] {"Plant_Flower_Common_Purple", "Plant_Flower_Common_Yellow"},
                trail.blockIdArray());

        Vector3d center = new Vector3d(1.0, 2.0, 3.0);
        ActiveLapidaryGem gem = runtime.createLapidaryGem("player", ownerRef, center, 10.0, 20.0, 6_000L, "label");
        assertEquals("player", gem.ownerPlayerId());
        assertEquals(ownerRef, gem.ref());
        assertEquals(10.0, gem.currentHp(), 0.0001);
        assertEquals(20.0, gem.maxHp(), 0.0001);
        assertEquals("label", gem.lastLabel());
        assertNotSame(center, gem.center());
        center.x = 99.0;
        assertEquals(1.0, gem.center().x, 0.0001);

        Vector3i anchor = new Vector3i(2, 3, 4);
        ActiveStackingColumn column = runtime.createStackingColumn("pillar", null, anchor, 42, 3, 7_000L, 2_000L);
        assertEquals("pillar", column.reason());
        assertEquals(42, column.blockTypeId());
        assertEquals(3, column.height());
        assertEquals(7_000L, column.expireAtMillis());
        assertEquals(2_000L, column.nextStageAtMillis());
        assertNotSame(anchor, column.anchor());
    }

    @Test
    void rejectsMissingInputs() {
        assertNull(runtime.createMovingTrail("", null, new TestRef(), 5_000L, 1_000L, "block"));
        assertNull(runtime.createMovingTrail("trail", null, new InvalidRef(), 5_000L, 1_000L, "block"));
        assertNull(runtime.createMovingTrail("trail", null, new TestRef(), 5_000L, 1_000L));

        assertNull(runtime.createLapidaryGem(null, new TestRef(), new Vector3d(), 10.0, 20.0, 6_000L, "label"));
        assertNull(runtime.createLapidaryGem("player", new InvalidRef(), new Vector3d(), 10.0, 20.0, 6_000L, "label"));
        assertNull(runtime.createLapidaryGem("player", new TestRef(), null, 10.0, 20.0, 6_000L, "label"));
        assertNull(runtime.createLapidaryGem("player", new TestRef(), new Vector3d(), 0.0, 20.0, 6_000L, "label"));

        assertNull(runtime.createStackingColumn("", null, new Vector3i(), 42, 3, 7_000L, 2_000L));
        assertNull(runtime.createStackingColumn("pillar", null, null, 42, 3, 7_000L, 2_000L));
        assertNull(runtime.createStackingColumn("pillar", null, new Vector3i(), 0, 3, 7_000L, 2_000L));
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

    private static final class InvalidRef extends Ref<EntityStore> {
        private InvalidRef() {
            super(null, 1);
        }

        @Override
        public boolean isValid() {
            return false;
        }
    }
}
