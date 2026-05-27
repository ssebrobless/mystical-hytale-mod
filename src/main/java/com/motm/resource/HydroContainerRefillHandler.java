package com.motm.resource;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.protocol.InteractionType;
import com.motm.manager.PlayerDataManager;
import com.motm.manager.ResourceManager;
import com.motm.model.PlayerData;

public final class HydroContainerRefillHandler {
    private final ResourceManager resourceManager;
    private final PlayerDataManager playerDataManager;
    private final StatusHudRefresher statusHudRefresher;

    public HydroContainerRefillHandler(ResourceManager resourceManager,
                                       PlayerDataManager playerDataManager,
                                       StatusHudRefresher statusHudRefresher) {
        this.resourceManager = resourceManager;
        this.playerDataManager = playerDataManager;
        this.statusHudRefresher = statusHudRefresher;
    }

    public boolean tryHandle(PlayerInteractEvent event,
                             Player player,
                             PlayerData playerData,
                             ItemStack itemInHand,
                             boolean holdingSpellbook) {
        if (event == null || player == null || playerData == null || resourceManager == null) {
            return false;
        }
        if (!resourceManager.areAbilityResourceCostsEnabled()) {
            return false;
        }
        if (event.getActionType() != InteractionType.Use) {
            return false;
        }
        if (!"hydro".equalsIgnoreCase(playerData.getPlayerClass())) {
            return false;
        }
        if (!HydroContainerItems.hasContainer(com.motm.util.MotmPlayerInventory.combined(player))) {
            return false;
        }
        if (!canAttemptRefill(itemInHand, holdingSpellbook)) {
            return false;
        }

        Vector3i targetBlock = event.getTargetBlock();
        if (!isWaterSourceBlock(player.getWorld(), targetBlock)) {
            return false;
        }

        String playerId = playerData.getPlayerId();
        int currentWater = resourceManager.getAmount(playerId, "water");
        int maxWater = resourceManager.getMaxAmount(playerId, "water");
        event.setCancelled(true);

        if (currentWater >= maxWater) {
            player.sendMessage(Message.raw(
                    "[MOTM] " + resourceManager.getWaterContainerInfo(playerId)
                            + " is already full (" + currentWater + "/" + maxWater + ")."
            ));
            return true;
        }

        resourceManager.refillWater(playerId);
        resourceManager.syncToPersistentState(playerData);
        if (playerDataManager != null) {
            playerDataManager.savePlayerData(playerData);
        }
        if (statusHudRefresher != null) {
            statusHudRefresher.refresh(playerId);
        }
        player.sendMessage(Message.raw(
                "[MOTM] Refilled " + resourceManager.getWaterContainerInfo(playerId)
                        + " from the water source."
        ));
        return true;
    }

    private static boolean canAttemptRefill(ItemStack itemInHand, boolean holdingSpellbook) {
        return holdingSpellbook || itemInHand == null || HydroContainerItems.isContainerItem(itemInHand);
    }

    @SuppressWarnings("removal")
    private static boolean isWaterSourceBlock(World world, Vector3i targetBlock) {
        if (world == null || targetBlock == null) {
            return false;
        }

        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(targetBlock.getX(), targetBlock.getZ()));
        if (chunk == null) {
            chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(targetBlock.getX(), targetBlock.getZ()));
        }
        if (chunk == null) {
            return false;
        }

        int localX = ChunkUtil.localCoordinate(targetBlock.getX());
        int localZ = ChunkUtil.localCoordinate(targetBlock.getZ());
        int y = targetBlock.getY();
        var blockType = chunk.getBlockType(localX, y, localZ);
        String blockId = blockType != null ? blockType.getId() : null;
        if (blockId != null) {
            String normalized = blockId.toLowerCase(java.util.Locale.ROOT);
            if (normalized.contains("water")) {
                return true;
            }
            if (normalized.contains("lava")) {
                return false;
            }
        }

        return world.getFluidId(targetBlock.getX(), y, targetBlock.getZ()) != 0;
    }

    @FunctionalInterface
    public interface StatusHudRefresher {
        void refresh(String playerId);
    }
}
