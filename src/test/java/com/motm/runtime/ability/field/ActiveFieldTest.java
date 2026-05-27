package com.motm.runtime.ability.field;

import com.google.gson.Gson;
import com.hypixel.hytale.math.vector.Vector3d;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveFieldTest {

    private static final Gson GSON = new Gson();

    @Test
    void ownsFieldStateAndSchedulesRuntimeTicks() {
        AbilityData ability = ability("""
                {
                  "id": "iron_wall",
                  "cast_type": "barrier"
                }
                """);
        ActiveField field = new ActiveField(
                "player",
                null,
                "terra",
                "iron",
                ability,
                new Vector3d(1.0, 2.0, 3.0),
                new Vector3d(0.0, 0.0, 1.0),
                new Vector3d(1.0, 0.0, 0.0),
                3.5,
                5.0,
                1.35,
                9000L,
                1000L,
                1000L,
                true,
                List.of(),
                "loop",
                1200L,
                "trace"
        );

        assertEquals("player", field.ownerPlayerId());
        assertEquals("terra", field.classId());
        assertEquals("iron", field.styleId());
        assertEquals(ability, field.ability());
        assertTrue(field.followOwner());
        assertEquals(3.5, field.radius(), 0.0001);
        assertEquals(5.0, field.halfWidth(), 0.0001);
        assertEquals(1.35, field.thickness(), 0.0001);
        assertEquals("loop", field.loopEffectId());
        assertEquals("trace", field.traceId());

        field.scheduleNextPulse(2000L);
        field.scheduleNextVisualRefresh(3000L);

        assertEquals(2000L + FieldRuntimeSpecs.FIELD_PULSE_INTERVAL_MS, field.nextPulseAtMillis());
        assertEquals(3000L + FieldRuntimeSpecs.FIELD_VISUAL_REFRESH_MS, field.nextVisualRefreshAtMillis());
    }

    @Test
    void clonesMutableVectorsOnCreateAndCenterUpdate() {
        Vector3d center = new Vector3d(1.0, 2.0, 3.0);
        Vector3d forward = new Vector3d(0.0, 0.0, 1.0);
        Vector3d line = new Vector3d(1.0, 0.0, 0.0);

        ActiveField field = new ActiveField(
                "player",
                null,
                "terra",
                "iron",
                ability("""
                        {
                          "id": "iron_wall",
                          "cast_type": "barrier"
                        }
                        """),
                center,
                forward,
                line,
                3.5,
                5.0,
                1.35,
                9000L,
                1000L,
                1000L,
                false,
                null,
                null,
                1200L,
                null
        );

        assertNotSame(center, field.center());
        assertNotSame(forward, field.forwardDirection());
        assertNotSame(line, field.lineDirection());
        center.x = 99.0;
        assertEquals(1.0, field.center().x, 0.0001);

        Vector3d nextCenter = new Vector3d(4.0, 5.0, 6.0);
        field.updateCenter(nextCenter);

        assertNotSame(nextCenter, field.center());
        nextCenter.x = 88.0;
        assertEquals(4.0, field.center().x, 0.0001);
        assertTrue(field.visualRefs().isEmpty());
    }

    private static AbilityData ability(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
