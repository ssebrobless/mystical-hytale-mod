package com.motm.util;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Narrow wrapper around Hytale inventory mutation APIs. The installed Hytale jar
 * remains authoritative; this class exists so transaction/result handling and
 * failure logging stay in one place as more item mechanics are added.
 */
public final class MotmInventoryOps {

    private MotmInventoryOps() {
    }

    public static boolean grant(Player player, ItemStack stack, Logger log, String context) {
        if (player == null || stack == null) {
            return false;
        }
        try {
            var entityRef = player.getReference();
            if (entityRef == null || !entityRef.isValid()) {
                return false;
            }
            var store = entityRef.getStore();
            if (store == null) {
                return false;
            }
            var transaction = player.giveItem(stack, entityRef, store);
            return transaction != null && transaction.succeeded();
        } catch (Throwable e) {
            logFailure(log, "grant", context, e);
            return false;
        }
    }

    public static boolean removeSlot(CombinedItemContainer inventory, short slot, Logger log, String context) {
        if (inventory == null) {
            return false;
        }
        try {
            inventory.removeItemStackFromSlot(slot);
            return true;
        } catch (Throwable e) {
            logFailure(log, "removeSlot", context + " slot=" + slot, e);
            return false;
        }
    }

    public static SlotRemoval removeFromSlot(CombinedItemContainer inventory,
                                             short slot,
                                             int amount,
                                             Logger log,
                                             String context) {
        if (inventory == null || amount <= 0) {
            return SlotRemoval.failed();
        }
        try {
            var transaction = inventory.removeItemStackFromSlot(slot, amount);
            if (transaction == null || !transaction.succeeded()) {
                return SlotRemoval.failed();
            }

            ItemStack before = transaction.getSlotBefore();
            ItemStack after = transaction.getSlotAfter();
            int beforeQuantity = before == null ? 0 : Math.max(0, before.getQuantity());
            int afterQuantity = after == null ? 0 : Math.max(0, after.getQuantity());
            return new SlotRemoval(true, Math.max(0, beforeQuantity - afterQuantity), before, after);
        } catch (Throwable e) {
            logFailure(log, "removeFromSlot", context + " slot=" + slot + " amount=" + amount, e);
            return SlotRemoval.failed();
        }
    }

    public static boolean restoreSlot(ItemContainer container, short slot, ItemStack stack, Logger log, String context) {
        if (container == null || stack == null) {
            return false;
        }
        try {
            container.setItemStackForSlot(slot, stack);
            return true;
        } catch (Throwable e) {
            logFailure(log, "restoreSlot", context + " slot=" + slot, e);
            return false;
        }
    }

    private static void logFailure(Logger log, String operation, String context, Throwable e) {
        if (log == null) {
            return;
        }
        log.log(Level.WARNING,
                "[MOTM] Inventory operation failed: operation=" + operation
                        + " context=" + (context == null ? "" : context)
                        + " error=" + e.getMessage(),
                e);
    }

    public record SlotRemoval(boolean succeeded, int removedItems, ItemStack before, ItemStack after) {
        private static SlotRemoval failed() {
            return new SlotRemoval(false, 0, null, null);
        }
    }
}
