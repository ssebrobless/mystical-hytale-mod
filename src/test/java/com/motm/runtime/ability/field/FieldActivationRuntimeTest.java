package com.motm.runtime.ability.field;

import com.google.gson.Gson;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldActivationRuntimeTest {
    private static final Gson GSON = new Gson();

    private final FieldActivationRuntime runtime = new FieldActivationRuntime();

    @Test
    void createsDelayedBarrierFieldWithVisualsAndSummary() {
        TestRef ownerRef = new TestRef();
        TestRef visualRef = new TestRef();
        FieldActivationRuntime.Result result = runtime.activate(
                "player",
                ownerRef,
                "terra",
                "stone",
                ability("iron_wall", "barrier", 1.0),
                "barrier",
                new Vector3d(1.0, 2.0, 3.0),
                new Vector3d(0.0, 0.0, 1.0),
                new Vector3d(1.0, 0.0, 0.0),
                2.5,
                3.0,
                0.8,
                1_000L,
                2_000L,
                false,
                new FieldVisualRuntime(List.of(visualRef), "MOTM_Field", 1_080L),
                "trace-1",
                "stone wall",
                2,
                500L,
                0.75
        );

        assertTrue(result.activated());
        assertEquals("barrier arms in 0.5s | lasts 2s | width 6.0m | pull 0.8m pulse | stone wall | pushed 2 spawn-overlap target(s)",
                result.summary());
        ActiveField field = result.field();
        assertEquals("player", field.ownerPlayerId());
        assertEquals(ownerRef, field.ownerRef());
        assertEquals("terra", field.classId());
        assertEquals("stone", field.styleId());
        assertEquals(1_000L, field.activateAtMillis());
        assertEquals(3_000L, field.expireAtMillis());
        assertEquals(1_000L, field.nextPulseAtMillis());
        assertEquals(1_080L, field.nextVisualRefreshAtMillis());
        assertEquals(List.of(visualRef), field.visualRefs());
        assertEquals("MOTM_Field", field.loopEffectId());
        assertEquals("trace-1", field.traceId());
        assertEquals(1.0, field.center().x, 0.0001);
        assertEquals(1.0, field.lineDirection().x, 0.0001);
    }

    @Test
    void createsImmediateHazardWithNoVisualDefaults() {
        FieldActivationRuntime.Result result = runtime.activate(
                "player",
                new TestRef(),
                "terra",
                "magma",
                ability("lava_pool", "ground_target", 0.0),
                "ground_target",
                new Vector3d(0.0, 1.0, 0.0),
                new Vector3d(0.0, 0.0, 1.0),
                new Vector3d(1.0, 0.0, 0.0),
                4.0,
                4.0,
                1.0,
                2_000L,
                3_500L,
                true,
                null,
                null,
                "",
                0,
                0L,
                0.0
        );

        assertEquals("hazard active for 3.5s | radius 4.0m", result.summary());
        ActiveField field = result.field();
        assertTrue(field.followOwner());
        assertEquals(List.of(), field.visualRefs());
        assertEquals(Long.MAX_VALUE, field.nextVisualRefreshAtMillis());
        assertEquals(5_500L, field.expireAtMillis());
    }

    @Test
    void returnsNoneWhenRequiredInputsAreMissing() {
        AbilityData ability = ability("field", "area", 0.0);

        assertFalse(runtime.activate(null, new TestRef(), "terra", "stone", ability, "area",
                new Vector3d(0.0, 0.0, 0.0), new Vector3d(0.0, 0.0, 1.0), new Vector3d(1.0, 0.0, 0.0),
                1.0, 1.0, 1.0, 0L, 100L, false, null, null, "", 0, 0L, 0.0).activated());
        assertFalse(runtime.activate("player", new InvalidRef(), "terra", "stone", ability, "area",
                new Vector3d(0.0, 0.0, 0.0), new Vector3d(0.0, 0.0, 1.0), new Vector3d(1.0, 0.0, 0.0),
                1.0, 1.0, 1.0, 0L, 100L, false, null, null, "", 0, 0L, 0.0).activated());
        assertFalse(runtime.activate("player", new TestRef(), "terra", "stone", ability, "area",
                null, new Vector3d(0.0, 0.0, 1.0), new Vector3d(1.0, 0.0, 0.0),
                1.0, 1.0, 1.0, 0L, 100L, false, null, null, "", 0, 0L, 0.0).activated());
    }

    private static AbilityData ability(String id, String castType, double pullForce) {
        return GSON.fromJson("""
                {
                  "id": "%s",
                  "cast_type": "%s",
                  "pull_force": %s
                }
                """.formatted(id, castType, pullForce), AbilityData.class);
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
