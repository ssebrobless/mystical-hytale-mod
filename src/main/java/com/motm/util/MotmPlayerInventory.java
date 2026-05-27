package com.motm.util;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;

import java.util.ArrayList;

public final class MotmPlayerInventory {
    private MotmPlayerInventory() {
    }

    public static CombinedItemContainer combined(Player player) {
        if (player == null || player.getInventory() == null) {
            return null;
        }

        var inventory = player.getInventory();
        var containers = new ArrayList<ItemContainer>(6);
        addInventoryContainer(containers, inventory.getHotbar());
        addInventoryContainer(containers, inventory.getStorage());
        addInventoryContainer(containers, inventory.getBackpack());
        addInventoryContainer(containers, inventory.getUtility());
        addInventoryContainer(containers, inventory.getTools());
        addInventoryContainer(containers, inventory.getArmor());
        if (containers.isEmpty()) {
            return null;
        }

        return new CombinedItemContainer(containers.toArray(ItemContainer[]::new));
    }

    private static void addInventoryContainer(ArrayList<ItemContainer> containers, ItemContainer container) {
        if (container != null) {
            containers.add(container);
        }
    }
}
