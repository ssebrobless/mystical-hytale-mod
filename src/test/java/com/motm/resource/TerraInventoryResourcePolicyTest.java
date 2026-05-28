package com.motm.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TerraInventoryResourcePolicyTest {
    @Test
    void matchesKnownResourcePrefixes() {
        assertTrue(TerraInventoryResourcePolicy.matchesItemId("Rock_Stone", "stone_blocks"));
        assertTrue(TerraInventoryResourcePolicy.matchesItemId("Soil_Grass_Temperate", "dirt_blocks"));
        assertTrue(TerraInventoryResourcePolicy.matchesItemId("Rock_Sandstone_Red_Block", "sand_blocks"));
        assertTrue(TerraInventoryResourcePolicy.matchesItemId("Ore_Copper", "metal"));
        assertTrue(TerraInventoryResourcePolicy.matchesItemId("Ingredient_Crystal_Blue", "gems"));
        assertTrue(TerraInventoryResourcePolicy.matchesItemId("Plant_Seeds_Wheat", "seeds"));
    }

    @Test
    void rejectsWrongResourceTypeAndUnknownItems() {
        assertFalse(TerraInventoryResourcePolicy.matchesItemId("Rock_Stone", "gems"));
        assertFalse(TerraInventoryResourcePolicy.matchesItemId("Weapon_Sword_Iron", "metal"));
        assertFalse(TerraInventoryResourcePolicy.matchesItemId("Rock_Stone", "unknown"));
        assertFalse(TerraInventoryResourcePolicy.matchesItemId(null, "stone_blocks"));
        assertFalse(TerraInventoryResourcePolicy.matchesItemId("Rock_Stone", null));
    }

    @Test
    void preservesCaseSensitiveItemIdMatching() {
        assertFalse(TerraInventoryResourcePolicy.matchesItemId("rock_stone", "stone_blocks"));
        assertFalse(TerraInventoryResourcePolicy.matchesItemId("Rock_Stone", "STONE_BLOCKS"));
    }

    @Test
    void returnsExpectedUnitsPerItem() {
        assertEquals(1, TerraInventoryResourcePolicy.unitsPerItem("stone_blocks"));
        assertEquals(1, TerraInventoryResourcePolicy.unitsPerItem("dirt_blocks"));
        assertEquals(1, TerraInventoryResourcePolicy.unitsPerItem("sand_blocks"));
        assertEquals(2, TerraInventoryResourcePolicy.unitsPerItem("seeds"));
        assertEquals(4, TerraInventoryResourcePolicy.unitsPerItem("metal"));
        assertEquals(6, TerraInventoryResourcePolicy.unitsPerItem("gems"));
    }

    @Test
    void unknownResourceTypesUseNeutralUnitFallback() {
        assertEquals(1, TerraInventoryResourcePolicy.unitsPerItem("unknown"));
        assertEquals(1, TerraInventoryResourcePolicy.unitsPerItem(null));
        assertEquals(1, TerraInventoryResourcePolicy.unitsPerItem(""));
    }
}
