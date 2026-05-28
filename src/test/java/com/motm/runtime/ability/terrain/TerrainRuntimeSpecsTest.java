package com.motm.runtime.ability.terrain;

import com.google.gson.Gson;
import org.joml.Vector3d;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerrainRuntimeSpecsTest {

    private static final Gson GSON = new Gson();

    @Test
    void identifiesMovementTrailsAndResolvesTrailGeometry() {
        AbilityData emberTrail = ability("""
                {
                  "id": "ember_dash",
                  "cast_type": "dash",
                  "terrain_effect": "ember_trail",
                  "radius": 9.0
                }
                """);

        assertTrue(TerrainRuntimeSpecs.shouldCreateMovementTrail(
                emberTrail, true, new Vector3d(0.0, 0.0, 0.0), new Vector3d(3.0, 0.0, 0.0)));
        assertEquals(4, TerrainRuntimeSpecs.trailNodeCount(emberTrail));
        assertEquals(2.4, TerrainRuntimeSpecs.trailRadius(emberTrail), 0.0001);
        assertFalse(TerrainRuntimeSpecs.shouldCreateMovementTrail(emberTrail, false,
                new Vector3d(0.0, 0.0, 0.0), new Vector3d(3.0, 0.0, 0.0)));
    }

    @Test
    void identifiesPersonalAuraFieldsAndResolvesAuraRadius() {
        AbilityData eye = ability("""
                {
                  "id": "eye_of_the_storm",
                  "cast_type": "self_buff",
                  "terrain_effect": "eye_of_the_storm",
                  "radius": 2.0
                }
                """);
        AbilityData flame = ability("""
                {
                  "id": "living_flame",
                  "cast_type": "self_burst",
                  "terrain_effect": "living_flame",
                  "radius": 5.25
                }
                """);

        assertTrue(TerrainRuntimeSpecs.shouldCreatePersonalAuraField(eye));
        assertEquals(4.5, TerrainRuntimeSpecs.auraRadius(eye), 0.0001);
        assertTrue(TerrainRuntimeSpecs.shouldCreatePersonalAuraField(flame));
        assertEquals(5.25, TerrainRuntimeSpecs.auraRadius(flame), 0.0001);
    }

    @Test
    void buildsDefensiveTrailCenterCopies() {
        Vector3d start = new Vector3d(0.0, 1.0, 0.0);
        Vector3d end = new Vector3d(3.0, 1.0, 0.0);

        List<Vector3d> centers = TerrainRuntimeSpecs.buildTrailCenters(start, end, 3);

        assertEquals(3, centers.size());
        assertEquals(0.0, centers.get(0).x, 0.0001);
        assertEquals(1.5, centers.get(1).x, 0.0001);
        assertEquals(3.0, centers.get(2).x, 0.0001);
        assertNotSame(start, centers.get(0));
        start.x = 99.0;
        assertEquals(0.0, centers.get(0).x, 0.0001);
    }

    @Test
    void appliesMinimumTemporarySelectionLifetime() {
        assertEquals(2_200L, TerrainRuntimeSpecs.temporarySelectionExpireAt(1_000L, 1_500L));
        assertEquals(5_000L, TerrainRuntimeSpecs.temporarySelectionExpireAt(1_000L, 5_000L));
    }

    private static AbilityData ability(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
