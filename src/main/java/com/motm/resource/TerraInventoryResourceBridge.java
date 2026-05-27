package com.motm.resource;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.motm.manager.ResourceManager;
import com.motm.util.MotmInventoryOps;
import com.motm.util.MotmPlayerInventory;

import java.util.ArrayList;
import java.util.function.Function;
import java.util.logging.Logger;

public final class TerraInventoryResourceBridge implements ResourceManager.TerraInventoryBridge {
    private final Function<String, Player> playerLookup;
    private final ResourceOverflowSink overflowSink;
    private final Logger log;

    public TerraInventoryResourceBridge(Function<String, Player> playerLookup,
                                        ResourceOverflowSink overflowSink,
                                        Logger log) {
        this.playerLookup = playerLookup;
        this.overflowSink = overflowSink;
        this.log = log;
    }

    @Override
    public int countInventoryResource(String playerId, String resourceType) {
        Player player = lookupPlayer(playerId);
        if (player == null || player.getInventory() == null || resourceType == null || resourceType.isBlank()) {
            return 0;
        }

        CombinedItemContainer inventory = MotmPlayerInventory.combined(player);
        return inventory == null ? 0 : countInventoryResource(inventory, resourceType);
    }

    @Override
    public boolean spendInventoryResource(String playerId, String resourceType, int amount) {
        if (amount <= 0) {
            return true;
        }

        Player player = lookupPlayer(playerId);
        if (player == null || player.getInventory() == null || resourceType == null || resourceType.isBlank()) {
            return false;
        }

        CombinedItemContainer inventory = MotmPlayerInventory.combined(player);
        if (inventory == null || countInventoryResource(inventory, resourceType) < amount) {
            return false;
        }

        int remaining = amount;
        int unitsPerItem = TerraInventoryResourcePolicy.unitsPerItem(resourceType);
        var matchingSlots = new ArrayList<Short>();
        inventory.forEach((slot, stack) -> {
            if (matchesResourceItem(stack, resourceType)) {
                matchingSlots.add(slot);
            }
        });

        for (short slot : matchingSlots) {
            if (remaining <= 0) {
                break;
            }

            ItemStack stack = inventory.getItemStack(slot);
            if (!matchesResourceItem(stack, resourceType)) {
                continue;
            }

            int stackQuantity = Math.max(0, stack.getQuantity());
            if (stackQuantity <= 0) {
                continue;
            }

            int itemsNeeded = (int) Math.ceil(remaining / (double) unitsPerItem);
            int removeAmount = Math.min(itemsNeeded, stackQuantity);
            var removal = MotmInventoryOps.removeFromSlot(
                    inventory,
                    slot,
                    removeAmount,
                    log,
                    "TerraInventoryResourceBridge playerId=" + playerId + " resourceType=" + resourceType
            );
            if (removal.succeeded()) {
                int removedUnits = removal.removedItems() * unitsPerItem;
                remaining -= removedUnits;
                if (remaining < 0) {
                    if (overflowSink != null) {
                        overflowSink.add(playerId, resourceType, Math.abs(remaining));
                    }
                    remaining = 0;
                }
            }
        }

        return remaining <= 0;
    }

    private Player lookupPlayer(String playerId) {
        return playerLookup == null || playerId == null ? null : playerLookup.apply(playerId);
    }

    private static int countInventoryResource(CombinedItemContainer inventory, String resourceType) {
        int unitsPerItem = TerraInventoryResourcePolicy.unitsPerItem(resourceType);
        final int[] total = {0};
        inventory.forEach((slot, stack) -> {
            if (matchesResourceItem(stack, resourceType)) {
                total[0] += Math.max(0, stack.getQuantity()) * unitsPerItem;
            }
        });
        return total[0];
    }

    private static boolean matchesResourceItem(ItemStack stack, String resourceType) {
        return stack != null
                && stack.getItemId() != null
                && TerraInventoryResourcePolicy.matchesItemId(stack.getItemId(), resourceType);
    }

    @FunctionalInterface
    public interface ResourceOverflowSink {
        void add(String playerId, String resourceType, int amount);
    }
}
