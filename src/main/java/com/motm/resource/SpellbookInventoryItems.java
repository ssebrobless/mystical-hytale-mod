package com.motm.resource;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;

import java.util.Set;

public final class SpellbookInventoryItems {
    public static final String DEFAULT_SPELLBOOK_ITEM_ID = "MOTM_Spellbook_Focus";
    public static final String DEFAULT_DEV_GRIMOIRE_ITEM_ID = "Recipe_Book_Magic_Void";
    private static final Set<String> LEGACY_NONWEAPON_SPELLBOOK_ITEM_IDS = Set.of(
            "Recipe_Book_Magic_Air",
            "Weapon_Spellbook_Grimoire_Brown",
            "Weapon_Spellbook_Grimoire_Purple",
            "Weapon_Spellbook_Frost",
            "Weapon_Spellbook_Fire",
            "Weapon_Spellbook_Rekindle_Embers"
    );
    private static final Set<String> SPELLBOOK_ITEM_IDS = Set.of(
            DEFAULT_SPELLBOOK_ITEM_ID,
            "Weapon_Spellbook_Grimoire_Purple",
            "Weapon_Spellbook_Frost",
            "Weapon_Spellbook_Fire",
            "Weapon_Spellbook_Rekindle_Embers",
            "Weapon_Spellbook_Grimoire_Brown"
    );
    private static final Set<String> DEV_GRIMOIRE_ITEM_IDS = Set.of(
            DEFAULT_DEV_GRIMOIRE_ITEM_ID
    );

    private SpellbookInventoryItems() {
    }

    public static boolean isSpellbookItem(ItemStack stack) {
        return stack != null && isSpellbookItemId(stack.getItemId());
    }

    public static boolean isSpellbookItemId(String itemId) {
        return itemId != null && SPELLBOOK_ITEM_IDS.contains(itemId);
    }

    public static boolean isDevBookItem(ItemStack stack) {
        return stack != null && isDevBookItemId(stack.getItemId());
    }

    public static boolean isDevBookItemId(String itemId) {
        return itemId != null && DEV_GRIMOIRE_ITEM_IDS.contains(itemId);
    }

    public static boolean isLegacyNonweaponSpellbookItem(ItemStack stack) {
        return stack != null && LEGACY_NONWEAPON_SPELLBOOK_ITEM_IDS.contains(stack.getItemId());
    }

    public static boolean hasSpellbook(CombinedItemContainer inventory) {
        return inventory != null && inventory.countItemStacks(SpellbookInventoryItems::isSpellbookItem) > 0;
    }

    public static boolean hasDevBook(CombinedItemContainer inventory) {
        return inventory != null && inventory.countItemStacks(SpellbookInventoryItems::isDevBookItem) > 0;
    }

    public static boolean hasDefaultSpellbook(CombinedItemContainer inventory) {
        return inventory != null && inventory.countItemStacks(
                stack -> stack != null && DEFAULT_SPELLBOOK_ITEM_ID.equals(stack.getItemId())
        ) > 0;
    }

    public static Set<String> recognizedSpellbookItemIds() {
        return SPELLBOOK_ITEM_IDS;
    }

    public static Set<String> recognizedDevBookItemIds() {
        return DEV_GRIMOIRE_ITEM_IDS;
    }
}
