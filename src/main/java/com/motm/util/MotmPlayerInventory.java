package com.motm.util;

import com.hypixel.hytale.component.ComponentAccessor;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class MotmPlayerInventory {
    private MotmPlayerInventory() {
    }

    public static CombinedItemContainer combined(Ref<EntityStore> playerRef,
                                                  ComponentAccessor<EntityStore> accessor) {
        if (playerRef == null || !playerRef.isValid() || accessor == null) {
            return null;
        }
        return InventoryComponent.getCombined(accessor, playerRef, InventoryComponent.EVERYTHING);
    }
}
