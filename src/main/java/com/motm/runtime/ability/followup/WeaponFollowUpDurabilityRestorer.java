package com.motm.runtime.ability.followup;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.util.MotmInventoryOps;

import java.util.Locale;
import java.util.logging.Logger;

public final class WeaponFollowUpDurabilityRestorer {

    private WeaponFollowUpDurabilityRestorer() {
    }

    public static boolean restoreHeldItemDurability(Player runtimePlayer, String itemId, Logger logger) {
        if (runtimePlayer == null || itemId == null || itemId.isBlank()
                || runtimePlayer.getReference() == null || !runtimePlayer.getReference().isValid()
                || runtimePlayer.getReference().getStore() == null) {
            return false;
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef.getStore();
        InventoryComponent.Tool tools = store.getComponent(playerRef, InventoryComponent.Tool.getComponentType());
        if (tools != null && restoreActiveContainerDurability(
                tools.getInventory(),
                tools.getActiveSlot(),
                itemId,
                logger)) {
            return true;
        }
        InventoryComponent.Hotbar hotbar = store.getComponent(playerRef, InventoryComponent.Hotbar.getComponentType());
        return hotbar != null && restoreActiveContainerDurability(
                hotbar.getInventory(),
                hotbar.getActiveSlot(),
                itemId,
                logger
        );
    }

    static boolean restoreActiveContainerDurability(ItemContainer container,
                                                   byte slot,
                                                   String itemId,
                                                   Logger logger) {
        if (container == null || slot < 0) {
            return false;
        }

        ItemStack stack = container.getItemStack(slot);
        if (stack == null || stack.isEmpty() || stack.getItemId() == null || !stack.getItemId().equalsIgnoreCase(itemId)) {
            return false;
        }
        if (stack.getMaxDurability() <= 0 || stack.getDurability() >= stack.getMaxDurability()) {
            return true;
        }

        ItemStack restored = stack.withRestoredDurability(stack.getMaxDurability());
        if (!MotmInventoryOps.restoreSlot(container, slot, restored, logger, "weaponFollowUpDurabilityRestore")) {
            return false;
        }
        if (logger != null) {
            logger.info("[MOTM] Alloy Enhancement restored durability: item=" + itemId
                    + " slot=" + slot
                    + " durability=" + formatNumber(stack.getDurability())
                    + "/" + formatNumber(stack.getMaxDurability()));
        }
        return true;
    }

    private static String formatNumber(double value) {
        if (!Double.isFinite(value)) {
            return String.valueOf(value);
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }
}
