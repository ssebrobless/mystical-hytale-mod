package com.motm.runtime.task;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.runtime.state.StyleTestRuntimeState;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hytale-facing world actions for live style-test mobs and review arenas.
 */
public final class StyleTestMobActions {

    private static final Set<String> CLEANUP_ROLES = Set.of(
            "Goblin_Scrapper",
            "Test_Dummy_Stationary",
            "Bat",
            "magma_sheep",
            "HytaleWolf",
            "Crawler"
    );
    private static final Set<String> GLOBAL_CLEANUP_ROLES = Set.of(
            "Test_Dummy_Stationary",
            "Mannequin",
            "Spark_Living"
    );

    private final StyleTestRuntimeState state;
    private final Logger log;

    public StyleTestMobActions(StyleTestRuntimeState state, Logger log) {
        this.state = state;
        this.log = log;
    }

    public static String normalizeMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "close", "stationary", "standard", "cluster", "line", "surround" -> normalized;
            default -> "standard";
        };
    }

    public String spawn(String playerId, Player runtimePlayer, String mode) {
        if (runtimePlayer == null) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        Vector3d basePosition = playerPosition(runtimePlayer);
        Vector3d forward = playerForward(runtimePlayer);
        if (basePosition == null || forward == null) {
            return "[MOTM] Could not resolve player position/direction for style-test mobs.";
        }
        if (basePosition.y < -16.0) {
            String summary = "[MOTM] Style test mob spawn blocked: player appears below world at "
                    + formatVector(basePosition)
                    + ". Respawn before running setup.";
            log.warning(summary);
            return summary;
        }

        Vector3d horizontalForward = normalizeHorizontal(forward);
        Vector3d right = new Vector3d(-horizontalForward.z, 0.0, horizontalForward.x);
        String normalizedMode = normalizeMode(mode);
        boolean closeGroundedTarget = "close".equals(normalizedMode) || "stationary".equals(normalizedMode);
        Vector3d groundPosition = closeGroundedTarget
                ? com.motm.util.MotmVectors.addScaled(basePosition, horizontalForward, 1.6)
                : com.motm.util.MotmVectors.addScaled(
                        com.motm.util.MotmVectors.addScaled(basePosition, horizontalForward, 5.0),
                        right,
                        -8.0);
        Vector3d floatingPosition = com.motm.util.MotmVectors.addScaled(
                com.motm.util.MotmVectors.addScaled(basePosition, horizontalForward, 5.0),
                right,
                -5.0);
        floatingPosition.y += 3.0;

        World world = runtimePlayer.getWorld();
        if (world == null) {
            return "[MOTM] Runtime world is unavailable for style-test mobs.";
        }

        Store<EntityStore> currentStore = playerRefStore(runtimePlayer);
        int cleared = clearTracked(playerId);
        int staleCleared = visitCleanupRoles(currentStore, runtimePlayer, true, true);
        List<Ref<EntityStore>> targets = new ArrayList<>();
        if ("cluster".equals(normalizedMode)) {
            addNpc(targets, world, com.motm.util.MotmVectors.addScaled(basePosition, horizontalForward, 4.0), "Test_Dummy_Stationary");
            addNpc(targets, world, com.motm.util.MotmVectors.addScaled(
                    com.motm.util.MotmVectors.addScaled(basePosition, horizontalForward, 4.0), right, 3.0), "Test_Dummy_Stationary");
            addNpc(targets, world, com.motm.util.MotmVectors.addScaled(
                    com.motm.util.MotmVectors.addScaled(basePosition, horizontalForward, 4.0), right, -3.0), "Test_Dummy_Stationary");
            addNpc(targets, world, com.motm.util.MotmVectors.addScaled(basePosition, horizontalForward, 7.0), "Test_Dummy_Stationary");
            addNpc(targets, world, com.motm.util.MotmVectors.addScaled(basePosition, horizontalForward, 2.0), "Test_Dummy_Stationary");
        } else if ("line".equals(normalizedMode)) {
            addNpc(targets, world, com.motm.util.MotmVectors.addScaled(basePosition, horizontalForward, 4.0), "Test_Dummy_Stationary");
            addNpc(targets, world, com.motm.util.MotmVectors.addScaled(basePosition, horizontalForward, 8.0), "Test_Dummy_Stationary");
            addNpc(targets, world, com.motm.util.MotmVectors.addScaled(basePosition, horizontalForward, 12.0), "Test_Dummy_Stationary");
        } else if ("surround".equals(normalizedMode)) {
            addNpc(targets, world, com.motm.util.MotmVectors.addScaled(basePosition, horizontalForward, 3.0), "Test_Dummy_Stationary");
            addNpc(targets, world, com.motm.util.MotmVectors.addScaled(basePosition, horizontalForward, -3.0), "Test_Dummy_Stationary");
            addNpc(targets, world, com.motm.util.MotmVectors.addScaled(basePosition, right, 3.0), "Test_Dummy_Stationary");
            addNpc(targets, world, com.motm.util.MotmVectors.addScaled(basePosition, right, -3.0), "Test_Dummy_Stationary");
        } else {
            addNpc(targets, world, groundPosition, "Test_Dummy_Stationary");
            if (!"stationary".equals(normalizedMode)) {
                addNpc(targets, world, floatingPosition, "Bat");
            }
        }
        state.putTargets(playerId, targets);
        int spawned = targets.size();

        String summary = "[MOTM] Style test mobs spawned: count=" + spawned
                + " mode=" + normalizedMode
                + " clearedPrevious=" + (cleared + staleCleared)
                + " tracked=" + countValidRefs(targets)
                + " grounded=" + formatVector(groundPosition)
                + " floating=" + formatVector(floatingPosition);
        log.info(summary);
        return summary;
    }

    public String clear(String playerId, Store<EntityStore> currentStore, Player player) {
        int cleared = clearTracked(playerId);
        int staleCleared = visitCleanupRoles(currentStore, player, true, true);
        String summary = "[MOTM] Style test mobs cleared: count=" + (cleared + staleCleared)
                + " tracked=" + cleared
                + " staleNearby=" + staleCleared;
        log.info(summary + " playerId=" + playerId);
        return summary;
    }

    public String count(String playerId, Store<EntityStore> currentStore, Player player) {
        int count = countTracked(playerId);
        int nearby = visitCleanupRoles(currentStore, player, false, true);
        String summary = "[MOTM] Style test mobs tracked: count=" + count
                + " nearbyCleanupRoles=" + nearby;
        log.info(summary + " playerId=" + playerId);
        return summary;
    }

    public String scrubArena(Player player) {
        World world = player != null ? player.getWorld() : null;
        Vector3d center = player != null ? playerPosition(player) : null;
        if (world == null || center == null) {
            return "skipped missing world/player";
        }

        int grassBlockTypeId = BlockType.getBlockIdOrUnknown("Soil_Grass", "MOTM style review arena scrub");
        if (grassBlockTypeId == BlockType.UNKNOWN_ID || grassBlockTypeId == BlockType.EMPTY_ID) {
            grassBlockTypeId = BlockType.getBlockIdOrUnknown("Rock_Stone_Brick_Pillar_Middle", "MOTM style review arena scrub");
        }
        if (grassBlockTypeId == BlockType.UNKNOWN_ID || grassBlockTypeId == BlockType.EMPTY_ID) {
            return "skipped no floor block";
        }

        int floorY = (int) Math.floor(center.y) - 1;
        int centerX = (int) Math.floor(center.x);
        int centerZ = (int) Math.floor(center.z);
        int radius = 28;
        BlockSelection scrub = new BlockSelection();
        scrub.setPosition(centerX, floorY, centerZ);
        scrub.setAnchorAtWorldPos(centerX, floorY, centerZ);
        int floorBlocks = 0;
        int clearedBlocks = 0;
        int clearedFluids = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int wx = centerX + x;
                int wz = centerZ + z;
                scrub.addBlockAtWorldPos(wx, floorY, wz, grassBlockTypeId, 0, 0, 0);
                floorBlocks++;
                for (int y = floorY + 1; y <= floorY + 5; y++) {
                    scrub.addBlockAtWorldPos(wx, y, wz, BlockType.EMPTY_ID, 0, 0, 0);
                    scrub.addFluidAtWorldPos(wx, y, wz, Fluid.EMPTY_ID, (byte) 0);
                    clearedBlocks++;
                    clearedFluids++;
                }
            }
        }

        try {
            scrub.place(null, world, new Vector3i(0, 0, 0), BlockMask.EMPTY);
            String summary = "scrubbed center=(" + centerX + "," + floorY + "," + centerZ + ")"
                    + " radius=" + radius
                    + " floorBlocks=" + floorBlocks
                    + " clearedBlocks=" + clearedBlocks
                    + " clearedFluids=" + clearedFluids;
            log.info("[MOTM] Style review arena scrub: " + summary);
            return summary;
        } catch (Throwable e) {
            log.log(Level.WARNING, "[MOTM] Style review arena scrub failed safely.", e);
            return "failed " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    public int countTracked(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return 0;
        }
        return countValidRefs(state.targets(playerId));
    }

    private int clearTracked(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return 0;
        }
        List<Ref<EntityStore>> targets = state.removeTargets(playerId);
        if (targets == null || targets.isEmpty()) {
            return 0;
        }

        int cleared = 0;
        for (Ref<EntityStore> target : targets) {
            if (target == null || !target.isValid()) {
                continue;
            }
            Store<EntityStore> store = target.getStore();
            NPCEntity npc = store != null ? store.getComponent(target, NPCEntity.getComponentType()) : null;
            if (npc != null && !npc.isDespawning()) {
                npc.setToDespawn();
                cleared++;
            }
        }
        return cleared;
    }

    private Store<EntityStore> playerRefStore(Player player) {
        if (player == null || player.getReference() == null || !player.getReference().isValid()) {
            return null;
        }
        return player.getReference().getStore();
    }

    private int visitCleanupRoles(Store<EntityStore> currentStore, Player player, boolean despawn, boolean includeGlobalDummies) {
        if (currentStore == null || player == null) {
            return 0;
        }

        Vector3d playerPosition = playerPosition(player);
        if (playerPosition == null) {
            return 0;
        }

        int[] visited = {0};
        currentStore.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                ModelComponent model = chunk.getComponent(entityIndex, ModelComponent.getComponentType());
                boolean cleanupRole = isCleanupRole(npc, model);
                boolean globalCleanupRole = includeGlobalDummies && isGlobalCleanupRole(npc, model);
                if (npc == null || npc.isDespawning() || (!cleanupRole && !globalCleanupRole)) {
                    continue;
                }

                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                Vector3d position = entityPosition(currentStore, ref);
                if (!globalCleanupRole && (position == null || distance(playerPosition, position) > 28.0)) {
                    continue;
                }

                if (despawn) {
                    npc.setToDespawn();
                }
                visited[0]++;
            }
        });
        return visited[0];
    }

    private boolean isCleanupRole(NPCEntity npc, ModelComponent model) {
        if (npc == null) {
            return false;
        }
        return isCleanupName(npc.getRoleName())
                || isCleanupName(npc.getNPCTypeId())
                || isCleanupModel(model);
    }

    private boolean isGlobalCleanupRole(NPCEntity npc, ModelComponent model) {
        if (npc == null) {
            return false;
        }
        return isGlobalCleanupName(npc.getRoleName())
                || isGlobalCleanupName(npc.getNPCTypeId())
                || isCleanupModel(model);
    }

    private boolean isCleanupModel(ModelComponent model) {
        if (model == null || model.getModel() == null) {
            return false;
        }
        return isCleanupName(model.getModel().getModelAssetId())
                || isCleanupName(model.getModel().getModel());
    }

    private boolean isCleanupName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return false;
        }
        String name = rawName.trim();
        return CLEANUP_ROLES.contains(name)
                || isDummyLikeName(name);
    }

    private boolean isGlobalCleanupName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return false;
        }
        String name = rawName.trim();
        return GLOBAL_CLEANUP_ROLES.contains(name)
                || isDummyLikeName(name);
    }

    private boolean isDummyLikeName(String rawName) {
        String normalized = rawName.toLowerCase(Locale.ROOT);
        return normalized.contains("test_dummy")
                || normalized.contains("training_dummy")
                || normalized.contains("dummy")
                || normalized.contains("mannequin");
    }

    private int countValidRefs(List<Ref<EntityStore>> refs) {
        if (refs == null || refs.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Ref<EntityStore> ref : refs) {
            if (ref == null || !ref.isValid()) {
                continue;
            }
            Store<EntityStore> store = ref.getStore();
            NPCEntity npc = store != null ? store.getComponent(ref, NPCEntity.getComponentType()) : null;
            if (npc != null && !npc.isDespawning() && store.getComponent(ref, DeathComponent.getComponentType()) == null) {
                count++;
            }
        }
        return count;
    }

    private void addNpc(List<Ref<EntityStore>> targets, World world, Vector3d position, String roleName) {
        Ref<EntityStore> ref = spawnNpc(world, position, roleName);
        if (ref != null) {
            targets.add(ref);
        }
    }

    private Ref<EntityStore> spawnNpc(World world, Vector3d position, String roleName) {
        try {
            NPCEntity npc = new NPCEntity(world);
            npc.setRoleName(roleName);
            npc.setDespawnTime(240.0f);
            world.spawnEntity(npc, new Vector3d(position), new com.hypixel.hytale.math.vector.Rotation3f(0f, 0f, 0f));

            Ref<EntityStore> ref = npc.getReference();
            if (ref == null || !ref.isValid() || ref.getStore() == null) {
                log.warning("[MOTM] Style test NPC spawned without a valid entity reference: role=" + roleName);
                return null;
            }
            return ref;
        } catch (Exception e) {
            log.warning("[MOTM] Failed to spawn style test NPC role=" + roleName
                    + " at " + formatVector(position) + ": " + e.getMessage());
            return null;
        }
    }

    private Vector3d playerPosition(Player player) {
        if (player == null) {
            return null;
        }
        var playerRef = player.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return null;
        }
        return entityPosition(playerRef.getStore(), playerRef);
    }

    private Vector3d playerForward(Player player) {
        if (player == null) {
            return null;
        }
        var playerRef = player.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return null;
        }
        TransformComponent transform = playerRef.getStore().getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getDirection() == null) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        Vector3d direction = new Vector3d(transform.getTransform().getDirection());
        if (!direction.isFinite() || direction.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        return direction;
    }

    private Vector3d entityPosition(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return null;
        }
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }
        return transform.getTransform().getPosition();
    }

    private Vector3d normalizeHorizontal(Vector3d direction) {
        if (direction == null) {
            return new Vector3d(1.0, 0.0, 0.0);
        }
        Vector3d flat = new Vector3d(direction.x, 0.0, direction.z);
        double length = Math.sqrt(flat.x * flat.x + flat.z * flat.z);
        if (length < 0.0001) {
            return new Vector3d(1.0, 0.0, 0.0);
        }
        return new Vector3d(flat.x / length, 0.0, flat.z / length);
    }

    private double distance(Vector3d a, Vector3d b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private String formatVector(Vector3d position) {
        if (position == null) {
            return "null";
        }
        return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", position.x, position.y, position.z);
    }
}
