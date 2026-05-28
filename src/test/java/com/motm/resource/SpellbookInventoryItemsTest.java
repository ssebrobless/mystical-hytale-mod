package com.motm.resource;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpellbookInventoryItemsTest {
    @Test
    void recognizesCurrentAndHistoricalSpellbookIds() {
        assertTrue(SpellbookInventoryItems.isSpellbookItemId("MOTM_Spellbook_Focus"));
        assertTrue(SpellbookInventoryItems.isSpellbookItemId("Weapon_Spellbook_Frost"));
        assertFalse(SpellbookInventoryItems.isSpellbookItemId("Weapon_Sword_Iron"));
        assertFalse(SpellbookInventoryItems.isSpellbookItemId(null));
    }

    @Test
    void recognizesDevBookIdsSeparately() {
        assertTrue(SpellbookInventoryItems.isDevBookItemId("Recipe_Book_Magic_Void"));
        assertFalse(SpellbookInventoryItems.isDevBookItemId("MOTM_Spellbook_Focus"));
        assertFalse(SpellbookInventoryItems.isDevBookItemId(null));
    }

    @Test
    void exposesStableReadOnlyIdSets() {
        assertEquals("MOTM_Spellbook_Focus", SpellbookInventoryItems.DEFAULT_SPELLBOOK_ITEM_ID);
        assertEquals("Recipe_Book_Magic_Void", SpellbookInventoryItems.DEFAULT_DEV_GRIMOIRE_ITEM_ID);
        assertThrows(UnsupportedOperationException.class,
                () -> SpellbookInventoryItems.recognizedSpellbookItemIds().add("Changed"));
        assertThrows(UnsupportedOperationException.class,
                () -> SpellbookInventoryItems.recognizedDevBookItemIds().add("Changed"));
    }
}
