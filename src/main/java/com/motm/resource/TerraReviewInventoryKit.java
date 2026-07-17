package com.motm.resource;

import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.motm.util.MotmInventoryOps;
import com.motm.util.MotmPlayerInventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Owns Terra review kit inventory policy for agent/harness review sessions.
 */
public final class TerraReviewInventoryKit {

    private static final List<KitItem> KIT_ITEMS = List.of(
            new KitItem("Tool_Pickaxe_Iron", 1, "pickaxe mining affinity"),
            new KitItem("Tool_Pickaxe_Wood", 1, "baseline pickaxe mining comparison"),
            new KitItem("Tool_Shovel_Iron", 1, "non-pickaxe negative mining control"),
            new KitItem("Weapon_Sword_Iron", 1, "physical melee weapon tests"),
            new KitItem("Weapon_Shield_Iron", 1, "durability shield / blocking tests"),
            new KitItem("Rock_Stone", 64, "stone/terrain test material"),
            new KitItem("Soil_Dirt", 64, "dirt/terrain test material"),
            new KitItem("Soil_Sand", 64, "sand/terrain test material"),
            new KitItem("Ingredient_Bar_Iron", 32, "metal visual test material"),
            new KitItem("Rock_Gem_Emerald", 16, "gem visual test material"),
            new KitItem("Plant_Seeds_Wheat", 32, "plant visual test material"),
            new KitItem("Rock_Crystal_Green_Block", 16, "green crystal/gem visual blocks"),
            new KitItem("Build_GreyDark_Cube", 32, "dark stone/metal visual block"),
            new KitItem("Build_Grey_Cube", 32, "neutral review marker block")
    );

    private static final Set<String> ESSENTIAL_ITEM_IDS = Set.of(
            SpellbookInventoryItems.DEFAULT_SPELLBOOK_ITEM_ID,
            "Tool_Pickaxe_Iron",
            "Weapon_Sword_Iron"
    );

    private final Logger log;
    private final Hooks hooks;

    public TerraReviewInventoryKit(Logger log, Hooks hooks) {
        this.log = log;
        this.hooks = hooks;
    }

    public String clean(Player player) {
        if (!hasRuntimeStore(player)) {
            return "[MOTM] Terra inventory clean failed: player runtime/store missing.";
        }

        CombinedItemContainer inventory = MotmPlayerInventory.combined(player.getReference(), player.getReference().getStore());
        if (inventory == null) {
            return "[MOTM] Terra inventory clean failed: player inventory unavailable.";
        }

        Set<String> keptEssentials = new HashSet<>();
        List<Short> slotsToRemove = new ArrayList<>();
        inventory.forEach((slot, stack) -> {
            if (stack == null || stack.getItemId() == null || stack.getQuantity() <= 0) {
                return;
            }

            String itemId = stack.getItemId();
            if (ESSENTIAL_ITEM_IDS.contains(itemId) && keptEssentials.add(itemId)) {
                return;
            }

            slotsToRemove.add(slot);
        });

        int removed = 0;
        for (short slot : slotsToRemove) {
            if (MotmInventoryOps.removeSlot(inventory, slot, log, "cleanTerraReviewInventory")) {
                removed++;
            }
        }

        hooks.ensureSpellbookItem(player);
        int granted = 0;
        granted += ensureReviewItem(player, "Tool_Pickaxe_Iron");
        granted += ensureReviewItem(player, "Weapon_Sword_Iron");

        String summary = "[MOTM] Terra review inventory cleaned: removedSlots=" + removed
                + " kept=spellbook,pickaxe,sword"
                + " grantedMissing=" + granted;
        log.info(summary + " playerId=" + hooks.runtimePlayerId(player));
        return summary;
    }

    public String grant(Player player) {
        if (!hasRuntimeStore(player)) {
            return "[MOTM] Terra review kit failed: player runtime/store missing.";
        }

        hooks.ensureSpellbookItem(player);
        hooks.ensureDevBookItem(player);

        int granted = 0;
        List<String> missing = new ArrayList<>();
        List<String> grantedIds = new ArrayList<>();
        for (KitItem item : KIT_ITEMS) {
            if (!isItemAssetAvailable(item.itemId())) {
                missing.add(item.itemId());
                continue;
            }
            if (MotmInventoryOps.grant(player, new ItemStack(item.itemId(), item.quantity()), log, "grantTerraReviewKit")) {
                granted++;
                grantedIds.add(item.itemId() + "x" + item.quantity());
            } else {
                missing.add(item.itemId());
            }
        }

        String summary = "[MOTM] Terra review kit granted: itemStacks=" + granted
                + " missing=" + missing.size()
                + " items=" + String.join(",", grantedIds)
                + (missing.isEmpty() ? "" : " missingIds=" + String.join(",", missing));
        log.info(summary + " playerId=" + hooks.runtimePlayerId(player));
        return summary;
    }

    private int ensureReviewItem(Player player, String itemId) {
        if (player == null || itemId == null || itemId.isBlank()) {
            return 0;
        }

        CombinedItemContainer inventory = MotmPlayerInventory.combined(player.getReference(), player.getReference().getStore());
        if (inventory == null) {
            return 0;
        }

        if (inventory.countItemStacks(stack -> stack != null && itemId.equals(stack.getItemId())) > 0) {
            return 0;
        }

        if (!isItemAssetAvailable(itemId)) {
            log.warning("[MOTM] Review item missing from asset map: " + itemId);
            return 0;
        }

        return MotmInventoryOps.grant(player, new ItemStack(itemId), log, "ensureReviewItem") ? 1 : 0;
    }

    private boolean hasRuntimeStore(Player player) {
        return player != null
                && player.getReference() != null
                && player.getReference().isValid()
                && player.getReference().getStore() != null;
    }

    private boolean isItemAssetAvailable(String itemId) {
        return itemId != null && !itemId.isBlank()
                && Item.getAssetMap().getAsset(itemId) != null;
    }

    public interface Hooks {
        boolean ensureSpellbookItem(Player player);

        boolean ensureDevBookItem(Player player);

        String runtimePlayerId(Player player);
    }

    private record KitItem(String itemId, int quantity, String purpose) {
    }
}
