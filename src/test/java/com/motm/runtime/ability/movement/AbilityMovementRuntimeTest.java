package com.motm.runtime.ability.movement;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityMovementRuntimeTest {
    private static final Gson GSON = new Gson();
    private final AbilityMovementRuntime runtime = new AbilityMovementRuntime();

    @Test
    void normalDashPlansBurstVelocityInsteadOfReposition() {
        AbilityMovementRuntime.MovementPlan plan = runtime.plan(
                ability("""
                        {"id":"dust_devil","cast_type":"dash","dash_distance":5}
                        """),
                "dash",
                new Vector3d(10.0, 80.0, 10.0),
                new Vector3d(0.0, 0.0, -1.0),
                5.0,
                0.0
        );

        assertEquals(AbilityMovementRuntime.MovementMode.BURST, plan.mode());
        assertNotNull(plan.velocity());
        assertEquals(0.0, plan.velocity().x, 0.0001);
        assertEquals(-12.0, plan.velocity().z, 0.0001);
        assertTrue(Double.isNaN(plan.velocity().y));
        assertEquals(5.0, plan.horizontalDistance(), 0.0001);
    }

    @Test
    void burrowKeepsWhackAMoleRepositionSemantics() {
        AbilityMovementRuntime.MovementPlan plan = runtime.plan(
                ability("""
                        {"id":"burrow","cast_type":"dash","dash_distance":4}
                        """),
                "dash",
                new Vector3d(10.0, 80.0, 10.0),
                new Vector3d(1.0, 0.0, 0.0),
                4.0,
                0.0
        );

        assertEquals(AbilityMovementRuntime.MovementMode.REPOSITION, plan.mode());
        assertEquals(14.0, plan.target().x, 0.0001);
        assertEquals(80.0, plan.target().y, 0.0001);
        assertEquals(10.0, plan.target().z, 0.0001);
    }

    @Test
    void teleportKeepsRepositionSemantics() {
        AbilityMovementRuntime.MovementPlan plan = runtime.plan(
                ability("""
                        {"id":"shadow_step","cast_type":"teleport","dash_distance":8}
                        """),
                "teleport",
                new Vector3d(0.0, 0.0, 0.0),
                new Vector3d(0.0, 0.0, 1.0),
                8.0,
                0.0
        );

        assertEquals(AbilityMovementRuntime.MovementMode.REPOSITION, plan.mode());
        assertEquals(8.0, plan.target().z, 0.0001);
    }

    private static AbilityData ability(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
