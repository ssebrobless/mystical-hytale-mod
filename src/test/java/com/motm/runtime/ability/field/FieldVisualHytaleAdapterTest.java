package com.motm.runtime.ability.field;

import com.motm.model.AbilityData;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FieldVisualHytaleAdapterTest {

    @Test
    void quakeGroundVisualsUseSingleCenterCue() {
        AbilityData aftershock = new AbilityData();
        set(aftershock, "id", "aftershock");
        set(aftershock, "castType", "ground_zone");
        set(aftershock, "radius", 8.0);

        List<Vector3d> positions = FieldVisualHytaleAdapter.buildFieldVisualPositions(
                new Vector3d(10.0, 80.0, 20.0),
                new Vector3d(1.0, 0.0, 0.0),
                aftershock,
                4.0);

        assertEquals(1, positions.size());
        assertEquals(new Vector3d(10.0, 80.0, 20.0), positions.get(0));
    }

    @Test
    void sinkholeVisualIsLoweredToReadAsGroundCracksInsteadOfFloating() {
        AbilityData sinkhole = new AbilityData();
        set(sinkhole, "id", "sinkhole");
        set(sinkhole, "castType", "ground_target");
        set(sinkhole, "radius", 3.0);

        List<Vector3d> positions = FieldVisualHytaleAdapter.buildFieldVisualPositions(
                new Vector3d(10.0, 80.0, 20.0),
                new Vector3d(1.0, 0.0, 0.0),
                sinkhole,
                1.5);

        assertEquals(1, positions.size());
        assertEquals(new Vector3d(10.0, 79.0, 20.0), positions.get(0));
    }

    private static void set(AbilityData ability, String fieldName, Object value) {
        try {
            Field field = AbilityData.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(ability, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to set AbilityData." + fieldName, e);
        }
    }
}
