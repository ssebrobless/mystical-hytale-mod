package com.motm.resource;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.motm.manager.ResourceManager;
import com.motm.model.PlayerData;
import com.motm.util.MotmInventoryOps;
import com.motm.util.MotmPlayerInventory;

import java.util.ArrayList;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Logger;

public final class HydroInventoryBridge implements ResourceManager.HydroInventoryBridge {
    private final Function<String, Player> playerLookup;
    private final Function<String, String> containerInfoLookup;
    private final BiConsumer<Player, Message> messenger;
    private final Logger log;

    public HydroInventoryBridge(Function<String, Player> playerLookup,
                                Function<String, String> containerInfoLookup,
                                BiConsumer<Player, Message> messenger,
                                Logger log) {
        this.playerLookup = playerLookup;
        this.containerInfoLookup = containerInfoLookup;
        this.messenger = messenger;
        this.log = log;
    }

    @Override
    public boolean hasHydroContainer(String playerId) {
        Player player = lookupPlayer(playerId);
        return player != null && HydroContainerItems.hasContainer(MotmPlayerInventory.combined(player));
    }

    @Override
    public int getHydroContainerTier(String playerId) {
        Player player = lookupPlayer(playerId);
        return player == null ? 0 : HydroContainerItems.detectTier(MotmPlayerInventory.combined(player));
    }

    public void syncContainerItem(Player player, PlayerData playerData, boolean notify) {
        if (player == null || playerData == null || player.getInventory() == null) {
            return;
        }

        CombinedItemContainer inventory = MotmPlayerInventory.combined(player);
        if (inventory == null) {
            return;
        }
        int containerCount = inventory.countItemStacks(HydroContainerItems::isContainerItem);
        boolean hydroClass = "hydro".equalsIgnoreCase(playerData.getPlayerClass());

        if (!hydroClass) {
            return;
        }

        int targetTier = HydroContainerItems.clampTier(playerData.getWaterContainerTier());
        if (containerCount == 0 && targetTier <= 0) {
            return;
        }
        int correctCount = inventory.countItemStacks(stack -> HydroContainerItems.isTier(stack, targetTier));
        if (containerCount == 1 && correctCount == 1) {
            return;
        }

        removeAllHydroContainerItems(inventory);

        var entityRef = player.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        if (!MotmInventoryOps.grant(player, HydroContainerItems.createStack(targetTier), log, "syncHydroContainerItem")) {
            return;
        }
        if (notify) {
            String containerInfo = containerInfoLookup == null
                    ? "Hydro waterskin"
                    : containerInfoLookup.apply(playerData.getPlayerId());
            if (messenger != null) {
                messenger.accept(player, Message.raw(
                    "[MOTM] Your Hydro waterskin is now "
                            + containerInfo
                            + ". Waterskins are no longer a casting cost, but they remain available for future Hydro utility tests."
                ));
            }
        }
    }

    private Player lookupPlayer(String playerId) {
        return playerLookup == null || playerId == null ? null : playerLookup.apply(playerId);
    }

    private void removeAllHydroContainerItems(CombinedItemContainer inventory) {
        var hydroSlots = new ArrayList<Short>();
        inventory.forEach((slot, stack) -> {
            if (HydroContainerItems.isContainerItem(stack)) {
                hydroSlots.add(slot);
            }
        });

        for (short slot : hydroSlots) {
            MotmInventoryOps.removeSlot(inventory, slot, log, "removeAllHydroContainerItems");
        }
    }
}
