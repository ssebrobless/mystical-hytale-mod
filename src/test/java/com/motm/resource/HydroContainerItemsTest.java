package com.motm.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HydroContainerItemsTest {
    @Test
    void clampsContainerTierForIdsAndStacks() {
        assertEquals("Ingredient_Hide_Light", HydroContainerItems.itemId(-10));
        assertEquals("Ingredient_Hide_Dark", HydroContainerItems.itemId(100));
    }

    @Test
    void detectsKnownContainerItemIdsWithoutRuntimeMetadata() {
        assertTrue(HydroContainerItems.isContainerItemId("Ingredient_Hide_Light"));
        assertTrue(HydroContainerItems.isContainerItemId("Ingredient_Hide_Dark"));
        assertFalse(HydroContainerItems.isContainerItemId(null));
        assertFalse(HydroContainerItems.isContainerItemId("Weapon_Sword_Iron"));
        assertEquals("Ingredient_Hide_Light", HydroContainerItems.itemId(0));
        assertEquals("Ingredient_Hide_Medium", HydroContainerItems.itemId(2));
    }

    @Test
    void exposesDefensiveItemIdCopy() {
        String[] ids = HydroContainerItems.itemIds();
        ids[0] = "Changed";

        assertEquals("Ingredient_Hide_Light", HydroContainerItems.itemId(0));
    }
}
