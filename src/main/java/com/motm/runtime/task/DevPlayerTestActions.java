package com.motm.runtime.task;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.GameMode;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hytale-facing player/world mutations for dev harness convenience commands.
 */
public final class DevPlayerTestActions {

    private final Logger log;

    public DevPlayerTestActions(Logger log) {
        this.log = log;
    }

    public String relocate(Player player, String target) {
        if (player == null || player.getReference() == null || !player.getReference().isValid()
                || player.getReference().getStore() == null) {
            return "[MOTM] Dev relocate failed: player runtime/store missing.";
        }
        TransformComponent transform = player.getReference().getStore()
                .getComponent(player.getReference(), TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
            return "[MOTM] Dev relocate failed: TransformComponent missing.";
        }

        Vector3d start = transform.getTransform().getPosition().clone();
        String normalizedTarget = target == null ? "up" : target.toLowerCase(Locale.ROOT);
        Vector3d destination = switch (normalizedTarget) {
            case "flatlands" -> new Vector3d(start.x + 96.0, Math.max(start.y + 40.0, 160.0), start.z + 96.0);
            case "lane" -> new Vector3d(start.x + 96.0, resolveTestingLaneY(player, start), start.z + 96.0);
            case "up" -> new Vector3d(start.x, start.y + 12.0, start.z);
            default -> null;
        };
        if (destination == null) {
            return "[MOTM] Dev relocate usage: /motm dev relocate <up|flatlands|lane>";
        }

        try {
            if ("flatlands".equals(normalizedTarget) || "lane".equals(normalizedTarget)) {
                placeRelocationPlatform(player, destination, normalizedTarget);
            }
            transform.teleportPosition(destination);
            String summary = "[MOTM] Dev relocate " + normalizedTarget
                    + ": start=" + formatVector(start)
                    + " destination=" + formatVector(destination);
            log.info(summary);
            return summary;
        } catch (Throwable e) {
            String summary = "[MOTM] Dev relocate failed safely: " + e.getMessage();
            log.log(Level.SEVERE, summary, e);
            return summary;
        }
    }

    public String applyGameMode(Player player, GameMode gameMode) {
        if (player == null || gameMode == null || player.getReference() == null || !player.getReference().isValid()
                || player.getReference().getStore() == null) {
            return "[MOTM] Dev game mode failed: player runtime/store missing.";
        }

        try {
            GameMode before = player.getGameMode();
            Player.setGameMode(player.getReference(), gameMode, player.getReference().getStore());
            String summary = "[MOTM] Dev game mode changed: before=" + before + " after=" + gameMode;
            log.info(summary);
            return summary;
        } catch (Throwable e) {
            String summary = "[MOTM] Dev game mode failed safely: " + e.getMessage();
            log.log(Level.SEVERE, summary, e);
            return summary;
        }
    }

    private double resolveTestingLaneY(Player player, Vector3d start) {
        String worldName = player != null && player.getWorld() != null
                ? player.getWorld().getName().toLowerCase(Locale.ROOT)
                : "";
        if (worldName.contains("flat")) {
            return 80.0;
        }
        return start != null ? start.y : 80.0;
    }

    private void placeRelocationPlatform(Player player, Vector3d destination, String target) {
        World world = player.getWorld();
        if (world == null || destination == null) {
            return;
        }
        int blockTypeId = BlockType.getBlockIdOrUnknown("Soil_Grass", "MOTM dev relocation platform");
        if (blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            blockTypeId = BlockType.getBlockIdOrUnknown("Rock_Stone_Brick_Pillar_Middle", "MOTM dev relocation platform");
        }
        if (blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            log.warning("[MOTM] Dev relocate platform skipped: no platform block resolved.");
            return;
        }

        int floorY = (int) Math.floor(destination.y) - 1;
        int centerX = (int) Math.floor(destination.x);
        int centerZ = (int) Math.floor(destination.z);
        BlockSelection platform = new BlockSelection();
        platform.setPosition(centerX, floorY, centerZ);
        platform.setAnchorAtWorldPos(centerX, floorY, centerZ);
        int radius = "lane".equals(target) ? 60 : 30;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                platform.addBlockAtWorldPos(centerX + x, floorY, centerZ + z, blockTypeId, 0, 0, 0);
            }
        }
        try {
            platform.place(null, world, Vector3i.ZERO, BlockMask.EMPTY);
            log.info("[MOTM] Dev relocate platform placed: target=" + target
                    + " center=(" + centerX + "," + floorY + "," + centerZ + ")"
                    + " blocks=" + platform.getBlockCount()
                    + " blockTypeId=" + blockTypeId);
        } catch (Throwable e) {
            log.log(Level.WARNING, "[MOTM] Dev relocate platform failed safely.", e);
        }
    }

    private String formatVector(Vector3d vector) {
        if (vector == null) {
            return "null";
        }
        return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", vector.x, vector.y, vector.z);
    }
}
