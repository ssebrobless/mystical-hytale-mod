package com.motm.runtime.ability.field;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldRuntimeSpecsTest {

    private static final Gson GSON = new Gson();

    @Test
    void identifiesPersistentFieldAbilities() {
        assertTrue(FieldRuntimeSpecs.isPersistentField(ability("""
                {
                  "id": "iron_wall",
                  "cast_type": "barrier"
                }
                """)));
        assertTrue(FieldRuntimeSpecs.isPersistentField(ability("""
                {
                  "id": "sinkhole",
                  "cast_type": "ground_target",
                  "terrain_effect": "sinkhole",
                  "duration_seconds": 5.0
                }
                """)));
        assertFalse(FieldRuntimeSpecs.isPersistentField(ability("""
                {
                  "id": "terra_quake",
                  "cast_type": "ground_burst"
                }
                """)));
        assertFalse(FieldRuntimeSpecs.isPersistentField(null));
    }

    @Test
    void resolvesPersistentFieldTerrainSpecsAndRestoreReasons() {
        FieldTerrainRuntimeSpec ironWall = FieldRuntimeSpecs.terrainSpec(ability("""
                {
                  "id": "iron_wall",
                  "cast_type": "barrier",
                  "terrain_effect": "iron_wall"
                }
                """));
        FieldTerrainRuntimeSpec lavaPool = FieldRuntimeSpecs.terrainSpec(ability("""
                {
                  "id": "lava_pool",
                  "cast_type": "ground_zone",
                  "terrain_effect": "lava_pool"
                }
                """));
        FieldTerrainRuntimeSpec mudpit = FieldRuntimeSpecs.terrainSpec(ability("""
                {
                  "id": "mudpit",
                  "cast_type": "ground_zone",
                  "terrain_effect": "mudpit"
                }
                """));
        FieldTerrainRuntimeSpec pillar = FieldRuntimeSpecs.terrainSpec(ability("""
                {
                  "id": "stone_pillar",
                  "cast_type": "ground_zone",
                  "terrain_effect": "stone_pillar"
                }
                """));

        assertEquals(FieldTerrainRuntimeKind.IRON_WALL, ironWall.kind());
        assertEquals("iron_wall", ironWall.reason());
        assertEquals(List.of("Metal_Iron"), ironWall.primaryAssetIds());
        assertEquals(List.of("iron_wall"), FieldRuntimeSpecs.terrainRestoreReasons(ironWallAbility()));

        assertEquals(FieldTerrainRuntimeKind.LAVA_POOL, lavaPool.kind());
        assertTrue(lavaPool.restoreBeforePlace());
        assertEquals(List.of("Fluid_Lava", "Lava", "lava"), lavaPool.primaryAssetIds());
        assertEquals(List.of("lava_pool"), FieldRuntimeSpecs.terrainRestoreReasons(lavaPoolAbility()));

        assertEquals(FieldTerrainRuntimeKind.MUDPIT, mudpit.kind());
        assertTrue(mudpit.groundedFluid());
        assertTrue(mudpit.appendBrownDebrisVisual());
        assertEquals(List.of("mudpit"), FieldRuntimeSpecs.terrainRestoreReasons(mudpitAbility()));

        assertEquals(FieldTerrainRuntimeKind.STONE_PILLAR, pillar.kind());
        assertEquals(3, pillar.columnHeight());
        assertEquals(List.of("Rock_Stone_Brick_Pillar_Middle", "Rock_Stone_Brick"), pillar.primaryAssetIds());
    }

    @Test
    void resolvesFieldOriginPolicies() {
        assertTrue(FieldRuntimeSpecs.isIronWall(ironWallAbility()));
        assertTrue(FieldRuntimeSpecs.isCasterCentered(lavaPoolAbility()));
        assertFalse(FieldRuntimeSpecs.isCasterCentered(ironWallAbility()));
        assertEquals(FieldTerrainRuntimeKind.NONE, FieldRuntimeSpecs.terrainSpec(ability("""
                {
                  "id": "plain_field",
                  "cast_type": "ground_zone"
                }
                """)).kind());
    }

    @Test
    void resolvesFieldDimensionsAndTiming() {
        AbilityData barrier = ability("""
                {
                  "id": "iron_wall",
                  "cast_type": "barrier",
                  "radius": 4.0,
                  "width": 10.0,
                  "delay_seconds": 0.5,
                  "duration_seconds": 6.0
                }
                """);
        AbilityData defaultZone = ability("""
                {
                  "id": "mist_zone",
                  "cast_type": "ground_zone"
                }
                """);

        assertEquals(4.0, FieldRuntimeSpecs.radius(barrier), 0.0001);
        assertEquals(5.0, FieldRuntimeSpecs.halfWidth(barrier, 4.0), 0.0001);
        assertEquals(FieldRuntimeSpecs.DEFAULT_FIELD_THICKNESS, FieldRuntimeSpecs.thickness(barrier, 4.0), 0.0001);
        assertEquals(500L, FieldRuntimeSpecs.delayMillis(barrier));
        assertEquals(6000L, FieldRuntimeSpecs.durationMillis(barrier));

        assertEquals(FieldRuntimeSpecs.DEFAULT_AREA_RADIUS, FieldRuntimeSpecs.radius(defaultZone), 0.0001);
        assertEquals(FieldRuntimeSpecs.DEFAULT_AREA_RADIUS, FieldRuntimeSpecs.halfWidth(defaultZone, FieldRuntimeSpecs.DEFAULT_AREA_RADIUS), 0.0001);
        assertEquals(FieldRuntimeSpecs.DEFAULT_AREA_RADIUS, FieldRuntimeSpecs.thickness(defaultZone, FieldRuntimeSpecs.DEFAULT_AREA_RADIUS), 0.0001);
        assertEquals(4000L, FieldRuntimeSpecs.durationMillis(defaultZone));
    }

    @Test
    void resolvesPulseDamageRatios() {
        assertEquals(0.0, FieldRuntimeSpecs.pulseDamage(100.0, ability("""
                {
                  "id": "healing_rain",
                  "cast_type": "support_zone"
                }
                """)), 0.0001);
        assertEquals(18.0, FieldRuntimeSpecs.pulseDamage(100.0, ability("""
                {
                  "id": "iron_wall",
                  "cast_type": "barrier"
                }
                """)), 0.0001);
        assertEquals(34.0, FieldRuntimeSpecs.pulseDamage(100.0, ability("""
                {
                  "id": "sinkhole",
                  "cast_type": "ground_target",
                  "terrain_effect": "sinkhole"
                }
                """)), 0.0001);
        assertEquals(22.0, FieldRuntimeSpecs.pulseDamage(100.0, ability("""
                {
                  "id": "smog",
                  "cast_type": "ground_zone",
                  "terrain_effect": "smog"
                }
                """)), 0.0001);
        assertEquals(28.0, FieldRuntimeSpecs.pulseDamage(100.0, ability("""
                {
                  "id": "field",
                  "cast_type": "ground_zone"
                }
                """)), 0.0001);
    }

    @Test
    void resolvesPullLiftForVerticalFieldEffects() {
        assertEquals(0.35, FieldRuntimeSpecs.pullLift(ability("""
                {
                  "id": "tempest_field",
                  "cast_type": "ground_zone",
                  "terrain_effect": "wind_tempest"
                }
                """)), 0.0001);
        assertEquals(0.35, FieldRuntimeSpecs.pullLift(ability("""
                {
                  "id": "field",
                  "cast_type": "ground_zone",
                  "travel_type": "funnel"
                }
                """)), 0.0001);
        assertEquals(0.0, FieldRuntimeSpecs.pullLift(ability("""
                {
                  "id": "mudpit",
                  "cast_type": "ground_zone"
                }
                """)), 0.0001);
    }

    private static AbilityData ability(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }

    private static AbilityData ironWallAbility() {
        return ability("""
                {
                  "id": "iron_wall",
                  "cast_type": "barrier",
                  "terrain_effect": "iron_wall"
                }
                """);
    }

    private static AbilityData lavaPoolAbility() {
        return ability("""
                {
                  "id": "lava_pool",
                  "cast_type": "ground_zone",
                  "terrain_effect": "lava_pool"
                }
                """);
    }

    private static AbilityData mudpitAbility() {
        return ability("""
                {
                  "id": "mudpit",
                  "cast_type": "ground_zone",
                  "terrain_effect": "mudpit"
                }
                """);
    }
}
