package com.motm.interaction;

import org.joml.Vector3i;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.motm.model.PlayerData;

import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public final class BlockDamageInteractionHandler {
    private static final double MAX_MATCH_DISTANCE = 7.5;

    private final Support support;
    private final Logger log;

    public BlockDamageInteractionHandler(Support support, Logger log) {
        this.support = support;
        this.log = log;
    }

    public void handle(DamageBlockEvent event) {
        if (log != null) {
            log.info("[MOTM] >>> handleDamageBlock ENTERED");
        }
        if (event == null || event.getTargetBlock() == null || support == null) {
            return;
        }

        Player nearbyPlayer = resolvePlayerForBlockDamage(event, false);
        if (nearbyPlayer != null) {
            String playerId = support.runtimePlayerId(nearbyPlayer);
            PlayerData playerData = playerId != null ? support.playerData(playerId) : null;
            if (isBareHanded(nearbyPlayer, event)
                    && support.handleBareHandBlockPunch(playerData, nearbyPlayer, event)) {
                return;
            }
        }

        ItemStack eventItem = event.getItemInHand();
        String itemId = eventItem != null ? eventItem.getItemId() : null;
        if (!isPickaxeItemId(itemId)) {
            return;
        }

        Player terraMiner = resolvePlayerForBlockDamage(event, true);
        if (terraMiner == null) {
            return;
        }

        event.setDamage(event.getDamage() * 1.5f);
        String playerId = support.runtimePlayerId(terraMiner);
        PlayerData playerData = playerId != null ? support.playerData(playerId) : null;
        String alloyResponse = support.handleAlloyToolUse(terraMiner, playerData, itemId);
        if (alloyResponse != null && !alloyResponse.isBlank()) {
            if (log != null) {
                log.info(alloyResponse + " playerId=" + playerId);
            }
            support.sendMessage(terraMiner, Message.raw(alloyResponse));
        }
    }

    private Player resolvePlayerForBlockDamage(DamageBlockEvent event, boolean requireTerraPickaxe) {
        if (event == null || event.getTargetBlock() == null) {
            return null;
        }

        String eventItemId = event.getItemInHand() != null ? event.getItemInHand().getItemId() : null;
        if (requireTerraPickaxe && (eventItemId == null || eventItemId.isBlank())) {
            return null;
        }

        Vector3i targetBlock = event.getTargetBlock();
        double targetX = targetBlock.x + 0.5;
        double targetY = targetBlock.y + 0.5;
        double targetZ = targetBlock.z + 0.5;

        Player bestMatch = null;
        double bestDistance = Double.MAX_VALUE;

        for (Map.Entry<String, Player> entry : support.onlineRuntimePlayers()) {
            Player candidate = entry.getValue();
            if (candidate == null || candidate.getInventory() == null) {
                continue;
            }

            PlayerData playerData = support.playerData(entry.getKey());
            if (requireTerraPickaxe && (playerData == null || !"terra".equalsIgnoreCase(playerData.getPlayerClass()))) {
                continue;
            }

            ItemStack itemInHand = candidate.getInventory().getItemInHand();
            if (requireTerraPickaxe
                    && (itemInHand == null || itemInHand.isEmpty() || !eventItemId.equalsIgnoreCase(itemInHand.getItemId()))) {
                continue;
            }

            var playerRef = candidate.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                continue;
            }

            TransformComponent transform = playerRef.getStore().getComponent(
                    playerRef,
                    TransformComponent.getComponentType()
            );
            if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                continue;
            }

            var position = transform.getTransform().getPosition();
            double dx = position.x - targetX;
            double dy = position.y - targetY;
            double dz = position.z - targetZ;
            double distance = Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
            if (distance > MAX_MATCH_DISTANCE || distance >= bestDistance) {
                continue;
            }

            bestDistance = distance;
            bestMatch = candidate;
        }

        return bestMatch;
    }

    private static boolean isBareHanded(Player player, DamageBlockEvent event) {
        ItemStack eventItem = event != null ? event.getItemInHand() : null;
        if (eventItem != null && !eventItem.isEmpty()) {
            return false;
        }
        ItemStack held = player != null && player.getInventory() != null
                ? player.getInventory().getItemInHand()
                : null;
        return held == null || held.isEmpty();
    }

    private static boolean isPickaxeItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }

        String normalized = itemId.toLowerCase(Locale.ROOT);
        return normalized.contains("pickaxe") || normalized.contains("_pick");
    }

    public interface Support {
        Iterable<Map.Entry<String, Player>> onlineRuntimePlayers();

        PlayerData playerData(String playerId);

        String runtimePlayerId(Player player);

        String handleAlloyToolUse(Player player, PlayerData playerData, String itemId);

        boolean handleBareHandBlockPunch(PlayerData playerData, Player player, DamageBlockEvent event);

        void sendMessage(Player player, Message message);
    }
}
