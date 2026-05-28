package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.model.AbilityData;
import com.motm.runtime.ability.field.FieldRuntimeSpecs;
import com.motm.runtime.ability.field.FieldTerrainRuntimeSpec;
import com.motm.util.MotmObservability;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TerrainPlacementHytaleAdapter {
    private final TerrainRuntimeState terrainState;
    private final TerrainActivationRuntime activationRuntime;
    private final Support support;

    public TerrainPlacementHytaleAdapter(TerrainRuntimeState terrainState,
                                         TerrainActivationRuntime activationRuntime,
                                         Support support) {
        this.terrainState = terrainState;
        this.activationRuntime = activationRuntime;
        this.support = support;
    }

    public String placePersistentTerrainSelection(Player runtimePlayer,
                                                  AbilityData ability,
                                                  Vector3d center,
                                                  Vector3d forward,
                                                  Vector3d lineDirection,
                                                  long expireAtMillis,
                                                  TerrainHytaleAdapter restoreAdapter) {
        if (runtimePlayer == null || ability == null || center == null) {
            return "";
        }

        FieldTerrainRuntimeSpec terrain = FieldRuntimeSpecs.terrainSpec(ability);
        if (terrain.restoreBeforePlace() && restoreAdapter != null) {
            restoreAdapter.restoreActiveTemporarySelections(terrainState, runtimePlayer.getWorld(), terrain.reason());
        }

        return switch (terrain.kind()) {
            case IRON_WALL -> placeIronWallSelection(
                    runtimePlayer.getWorld(),
                    terrain.reason(),
                    center,
                    lineDirection,
                    expireAtMillis,
                    terrain.primaryAssetIdArray(),
                    terrain.secondaryAssetIdArray(),
                    restoreAdapter);
            case LAVA_POOL -> placeFluidDiscSelection(
                    runtimePlayer.getWorld(),
                    terrain.reason(),
                    center,
                    ability.getRadius(),
                    expireAtMillis,
                    terrain.primaryAssetIdArray());
            case MUDPIT -> {
                String fluid = placeGroundedFluidDiscSelection(
                        runtimePlayer.getWorld(),
                        terrain.reason(),
                        center,
                        ability.getRadius(),
                        expireAtMillis,
                        terrain.primaryAssetIdArray());
                yield fluid.isBlank() || !terrain.appendBrownDebrisVisual()
                        ? fluid
                        : fluid + " + brown debris visual";
            }
            case STONE_PILLAR -> placeStackingColumnSelection(
                    runtimePlayer.getWorld(),
                    terrain.reason(),
                    center,
                    terrain.columnHeight(),
                    expireAtMillis,
                    terrain.primaryAssetIdArray());
            case TIDE_POOL, OIL_SPILL -> placeGroundedFluidCylinderSelection(
                    runtimePlayer.getWorld(),
                    terrain.reason(),
                    center,
                    ability.getRadius(),
                    Math.max(1, (int) Math.round(ability.getHeight())),
                    expireAtMillis,
                    terrain.primaryAssetIdArray());
            case ICE_CAP_TUBE -> placeIceCapTubeSelection(
                    runtimePlayer.getWorld(),
                    terrain.reason(),
                    center,
                    Math.max(1, terrain.columnHeight()),
                    expireAtMillis,
                    terrain.primaryAssetIdArray());
            case NONE -> "";
        };
    }

    public void placeSupplementalSurfaceCue(World world,
                                            AbilityData ability,
                                            Vector3d center,
                                            long expireAtMillis) {
        if (world == null || ability == null || center == null) {
            return;
        }
        String abilityId = lower(ability.getId());
        String terrainEffect = lower(ability.getTerrainEffect());
        if (terrainEffect.contains("dust_devil")) {
            return;
        }
        if (terrainEffect.contains("tunnel_path") || terrainEffect.contains("ruptured_earth")) {
            placeSurfacePatchSelection(world, abilityId.isBlank() ? "earth_movement" : abilityId,
                    center, 1, Math.min(expireAtMillis, System.currentTimeMillis() + 2600L),
                    "Soil_Dirt", "Rock_Stone", "Rock_Stone_Brick");
        }
    }

    public boolean startMovingTerrainTrail(World world,
                                           Ref<EntityStore> ownerRef,
                                           String reason,
                                           long expireAtMillis,
                                           String... blockIds) {
        if (world == null || ownerRef == null || !ownerRef.isValid()
                || blockIds == null || blockIds.length == 0) {
            return false;
        }
        if (resolveRuntimeBlockTypeId(blockIds) == BlockType.UNKNOWN_ID) {
            return false;
        }

        ActiveMovingTerrainTrail trail = activationRuntime.createMovingTrail(
                reason,
                world,
                ownerRef,
                expireAtMillis,
                System.currentTimeMillis(),
                blockIds
        );
        if (trail == null) {
            return false;
        }
        terrainState.addMovingTrail(trail);
        logInfo("[MOTM] Moving Terra terrain trail started: reason=" + reason
                + " expiresAt=" + expireAtMillis);
        return true;
    }

    public int pushTargetsOverlappingIronWall(Ref<EntityStore> playerRef,
                                              Store<EntityStore> store,
                                              AbilityData ability,
                                              Vector3d center,
                                              Vector3d forward,
                                              Vector3d lineDirection) {
        if (playerRef == null || store == null || ability == null || center == null || forward == null) {
            return 0;
        }

        Vector3d pushDirection = normalizeHorizontal(forward);
        Vector3d wallRight = normalizeHorizontal(lineDirection);
        double halfWidth = Math.max(1.5, ability.getWidth() > 0 ? ability.getWidth() / 2.0 : 1.5);
        int pushed = 0;

        for (Ref<EntityStore> targetRef : collectNearbyNpcTargets(store, center, halfWidth + 2.0, 8)) {
            Vector3d targetPosition = position(targetRef, store);
            if (targetPosition == null) {
                continue;
            }
            Vector3d fromCenter = subtract(targetPosition, center);
            double axial = dot(fromCenter, pushDirection);
            double lateral = dot(fromCenter, wallRight);
            double vertical = Math.abs(targetPosition.y - center.y);
            if (Math.abs(axial) > 1.25 || Math.abs(lateral) > halfWidth + 0.75 || vertical > 3.25) {
                continue;
            }

            NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
            if (npc == null) {
                continue;
            }
            Vector3d destination = com.motm.util.MotmVectors.addScaled(targetPosition, pushDirection, ability.getKnockbackForce() > 0 ? Math.min(ability.getKnockbackForce(), 4.0) : 3.0);
            npc.moveTo(targetRef, destination.x, destination.y, destination.z, store);
            pushed++;
        }

        if (pushed > 0) {
            logInfo("[MOTM] Iron Wall spawn-overlap push: pushed=" + pushed
                    + " center=" + formatVector(center));
        }
        return pushed;
    }

    public String placeWallSelection(World world,
                                     String reason,
                                     Vector3d center,
                                     Vector3d lineDirection,
                                     int width,
                                     int height,
                                     long expireAtMillis,
                                     String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = surfaceOverlayAnchor(center);
        Vector3i rightStep = horizontalStep(lineDirection != null ? lineDirection : new Vector3d(1.0, 0.0, 0.0));
        BlockSelection selection = baseSelection(anchor);
        int half = width / 2;
        for (int x = 0; x < width; x++) {
            int offset = x - half;
            for (int y = 0; y < height; y++) {
                selection.addBlockAtWorldPos(
                        anchor.x + (rightStep.x * offset),
                        anchor.y + y,
                        anchor.z + (rightStep.z * offset),
                        blockTypeId, 0, 0, 0);
            }
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " blocks");
    }

    public String placeIronWallSelection(World world,
                                         String reason,
                                         Vector3d center,
                                         Vector3d lineDirection,
                                         long expireAtMillis,
                                         String[] primaryBlockIds,
                                         String[] secondaryBlockIds,
                                         TerrainHytaleAdapter restoreAdapter) {
        int primaryBlockTypeId = resolveRuntimeBlockTypeId(primaryBlockIds);
        int secondaryBlockTypeId = resolveRuntimeBlockTypeId(secondaryBlockIds);
        if (world == null || center == null
                || primaryBlockTypeId == BlockType.UNKNOWN_ID || primaryBlockTypeId == BlockType.EMPTY_ID) {
            return "";
        }
        if (secondaryBlockTypeId == BlockType.UNKNOWN_ID || secondaryBlockTypeId == BlockType.EMPTY_ID) {
            secondaryBlockTypeId = primaryBlockTypeId;
        }

        Vector3i anchor = surfaceDecorationAnchor(center);
        if (restoreAdapter != null) {
            restoreAdapter.restoreActiveTemporarySelections(terrainState, world, reason);
        }
        Vector3i rightStep = horizontalStep(lineDirection != null ? lineDirection : new Vector3d(1.0, 0.0, 0.0));
        BlockSelection selection = baseSelection(anchor);
        for (int x = 0; x < 3; x++) {
            int offset = x - 1;
            for (int y = 0; y < 4; y++) {
                int blockTypeId = ((x + y) % 2 == 0) ? primaryBlockTypeId : secondaryBlockTypeId;
                selection.addBlockAtWorldPos(
                        anchor.x + (rightStep.x * offset),
                        anchor.y + y,
                        anchor.z + (rightStep.z * offset),
                        blockTypeId, 0, 0, 0);
            }
        }
        String summary = "12 grounded iron blocks";
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis, summary);
    }

    public String placeSurfacePatchSelection(World world,
                                             String reason,
                                             Vector3d center,
                                             int radius,
                                             long expireAtMillis,
                                             String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = surfaceOverlayAnchor(center);
        BlockSelection selection = baseSelection(anchor);
        int r = Math.max(0, radius);
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double dist = Math.sqrt((x * x) + (z * z));
                if (dist > r + 0.25) {
                    continue;
                }
                selection.addBlockAtWorldPos(anchor.x + x, anchor.y, anchor.z + z, blockTypeId, 0, 0, 0);
            }
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " surface decoration blocks");
    }

    public String placeFloatingClusterSelection(World world,
                                                String reason,
                                                Vector3d center,
                                                int width,
                                                int height,
                                                int depth,
                                                long expireAtMillis,
                                                String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = new Vector3i(
                (int) Math.floor(center.x),
                (int) Math.floor(center.y) + 1,
                (int) Math.floor(center.z)
        );
        BlockSelection selection = baseSelection(anchor);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    selection.addBlockAtWorldPos(anchor.x + x, anchor.y + y, anchor.z + z, blockTypeId, 0, 0, 0);
                }
            }
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " floating green gem cube blocks");
    }

    public String placeSurfaceColumnSelection(World world,
                                              String reason,
                                              Vector3d center,
                                              int height,
                                              long expireAtMillis,
                                              String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = surfaceOverlayAnchor(center);
        BlockSelection selection = baseSelection(anchor);
        int h = Math.max(1, height);
        for (int y = 0; y < h; y++) {
            selection.addBlockAtWorldPos(anchor.x, anchor.y + y, anchor.z, blockTypeId, 0, 0, 0);
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " surface column blocks");
    }

    public String placeStackingColumnSelection(World world,
                                               String reason,
                                               Vector3d center,
                                               int height,
                                               long expireAtMillis,
                                               String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = surfaceDecorationAnchor(center);
        ActiveStackingColumn column = activationRuntime.createStackingColumn(
                reason,
                world,
                anchor,
                blockTypeId,
                Math.max(1, height),
                expireAtMillis,
                System.currentTimeMillis()
        );
        if (column == null) {
            return "";
        }
        terrainState.addStackingColumn(column);
        logInfo("[MOTM] Temporary Terra stacking column started: reason=" + reason
                + " anchor=" + anchor
                + " height=" + column.height());
        return "terrain staged " + column.height() + " stone pillar blocks";
    }

    public String placeColumnSelection(World world,
                                       String reason,
                                       Vector3d center,
                                       int width,
                                       int height,
                                       long expireAtMillis,
                                       String... blockIds) {
        return placeWallSelection(world, reason, center, new Vector3d(1.0, 0.0, 0.0),
                width, height, expireAtMillis, blockIds);
    }

    public String placeIceCapTubeSelection(World world,
                                           String reason,
                                           Vector3d center,
                                           int height,
                                           long expireAtMillis,
                                           String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = surfaceDecorationAnchor(center);
        BlockSelection selection = baseSelection(anchor);
        int h = Math.max(1, height);
        for (int y = 0; y < h; y++) {
            selection.addBlockAtWorldPos(anchor.x - 1, anchor.y + y, anchor.z, blockTypeId, 0, 0, 0);
            selection.addBlockAtWorldPos(anchor.x + 1, anchor.y + y, anchor.z, blockTypeId, 0, 0, 0);
            selection.addBlockAtWorldPos(anchor.x, anchor.y + y, anchor.z - 1, blockTypeId, 0, 0, 0);
            selection.addBlockAtWorldPos(anchor.x, anchor.y + y, anchor.z + 1, blockTypeId, 0, 0, 0);
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " ice cap tube blocks");
    }

    public String placeRingBlockSelection(World world,
                                          String reason,
                                          Vector3d center,
                                          double radius,
                                          long expireAtMillis,
                                          String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = surfaceOverlayAnchor(center);
        BlockSelection selection = baseSelection(anchor);
        int ring = Math.max(1, (int) Math.round(radius));
        for (int x = -ring; x <= ring; x++) {
            for (int z = -ring; z <= ring; z++) {
                double dist = Math.sqrt((x * x) + (z * z));
                if (dist < ring - 0.4 || dist > ring + 0.4) {
                    continue;
                }
                selection.addBlockAtWorldPos(anchor.x + x, anchor.y, anchor.z + z, blockTypeId, 0, 0, 0);
            }
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " ring blocks");
    }

    public String placeTrailSelection(World world,
                                      String reason,
                                      Vector3d origin,
                                      Vector3d forward,
                                      long expireAtMillis,
                                      String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || origin == null || forward == null
                || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = surfaceOverlayAnchor(origin);
        BlockSelection selection = baseSelection(anchor);
        Vector3d back = new Vector3d(-forward.x, 0.0, -forward.z);
        if (!back.isFinite() || back.length() < 0.001) {
            back = new Vector3d(0.0, 0.0, -1.0);
        } else {
            back.normalize();
        }
        for (int i = 1; i <= 4; i++) {
            Vector3d pos = com.motm.util.MotmVectors.addScaled(origin, back, i);
            Vector3i block = surfaceOverlayAnchor(pos);
            selection.addBlockAtWorldPos(block.x, block.y, block.z, blockTypeId, 0, 0, 0);
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " trail flowers");
    }

    public String placeObsidianBlockShellSelection(World world,
                                                   String reason,
                                                   Vector3d center,
                                                   long expireAtMillis,
                                                   TerrainHytaleAdapter restoreAdapter,
                                                   String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null
                || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        if (restoreAdapter != null) {
            restoreAdapter.restoreActiveTemporarySelections(terrainState, world, reason);
        }
        Vector3i anchor = blockAnchor(center);
        BlockSelection selection = baseSelection(anchor);
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = -1; z <= 1; z++) {
                    boolean side = Math.abs(x) == 1 || Math.abs(z) == 1;
                    if (!side) {
                        continue;
                    }
                    selection.addBlockAtWorldPos(
                            anchor.x + x,
                            anchor.y + y,
                            anchor.z + z,
                            blockTypeId,
                            0,
                            0,
                            0);
                }
            }
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " offset obsidian shell blocks");
    }

    public String placeFluidDiscSelection(World world,
                                          String reason,
                                          Vector3d center,
                                          double radius,
                                          long expireAtMillis,
                                          String... fluidIds) {
        int fluidTypeId = resolveRuntimeFluidTypeId(fluidIds);
        Fluid fluid = fluidTypeId != Fluid.UNKNOWN_ID && fluidTypeId != Fluid.EMPTY_ID
                ? Fluid.getAssetMap().getAsset(fluidTypeId)
                : null;
        if (world == null || center == null || fluid == null || fluid.isUnknown()) {
            return "";
        }

        Vector3i anchor = blockAnchor(center);
        BlockSelection selection = baseSelection(anchor);
        int r = Math.max(1, (int) Math.round(radius));
        byte fluidLevel = (byte) Math.max(1, fluid.getMaxFluidLevel());
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double dist = Math.sqrt((x * x) + (z * z));
                if (dist > r + 0.2) {
                    continue;
                }
                selection.addFluidAtWorldPos(anchor.x + x, anchor.y, anchor.z + z, fluidTypeId, fluidLevel);
            }
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getFluidCount() + " fluids");
    }

    public String placeGroundedFluidDiscSelection(World world,
                                                  String reason,
                                                  Vector3d center,
                                                  double radius,
                                                  long expireAtMillis,
                                                  String... fluidIds) {
        if (center == null) {
            return "";
        }
        Vector3d grounded = new Vector3d(center.x, center.y - 1.0, center.z);
        return placeFluidDiscSelection(world, reason, grounded, radius, expireAtMillis, fluidIds);
    }

    public String placeGroundedFluidCylinderSelection(World world,
                                                      String reason,
                                                      Vector3d center,
                                                      double radius,
                                                      int height,
                                                      long expireAtMillis,
                                                      String... fluidIds) {
        int fluidTypeId = resolveRuntimeFluidTypeId(fluidIds);
        Fluid fluid = fluidTypeId != Fluid.UNKNOWN_ID && fluidTypeId != Fluid.EMPTY_ID
                ? Fluid.getAssetMap().getAsset(fluidTypeId)
                : null;
        if (world == null || center == null || fluid == null || fluid.isUnknown()) {
            return "";
        }

        Vector3d grounded = new Vector3d(center.x, center.y - 1.0, center.z);
        Vector3i anchor = blockAnchor(grounded);
        BlockSelection selection = baseSelection(anchor);
        int r = Math.max(1, (int) Math.round(radius));
        int h = Math.max(1, height);
        byte fluidLevel = (byte) Math.max(1, fluid.getMaxFluidLevel());
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double dist = Math.sqrt((x * x) + (z * z));
                if (dist > r + 0.2) {
                    continue;
                }
                for (int y = 0; y < h; y++) {
                    selection.addFluidAtWorldPos(anchor.x + x, anchor.y + y, anchor.z + z, fluidTypeId, fluidLevel);
                }
            }
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getFluidCount() + " grounded fluid cylinder fluids");
    }

    public String placeTemporarySelection(World world,
                                          String reason,
                                          Vector3i anchor,
                                          BlockSelection selection,
                                          long expireAtMillis,
                                          String summary) {
        if (world == null || anchor == null || selection == null) {
            return "";
        }
        try {
            BlockSelection original = selection.place(null, world, new Vector3i(0, 0, 0), BlockMask.EMPTY);
            terrainState.addSelection(new TemporaryTerrainSelection(
                    reason,
                    world,
                    anchor,
                    original,
                    TerrainRuntimeSpecs.temporarySelectionExpireAt(System.currentTimeMillis(), expireAtMillis)
            ));
            logInfo("[MOTM] Temporary Terra terrain placed: reason=" + reason
                    + " anchor=" + anchor
                    + " summary=" + summary);
            if (support != null) {
                support.recordServerTruth("temporary_selection_placed", MotmObservability.mapOf(
                        "reason", reason,
                        "anchor", "(" + anchor.x + "," + anchor.y + "," + anchor.z + ")",
                        "blockCount", selection.getBlockCount(),
                        "fluidCount", selection.getFluidCount(),
                        "expireAtMillis", expireAtMillis,
                        "summary", summary
                ));
            }
            return "terrain " + summary;
        } catch (Throwable e) {
            logWarning("[MOTM] Temporary Terra terrain placement failed: reason=" + reason
                    + " anchor=" + anchor
                    + " error=" + e.getMessage());
            return "";
        }
    }

    public int resolveRuntimeBlockTypeId(String... blockIds) {
        for (String blockId : blockIds) {
            try {
                int id = BlockType.getBlockIdOrUnknown(blockId, "MOTM Terra runtime terrain");
                if (id != BlockType.UNKNOWN_ID && id != BlockType.EMPTY_ID) {
                    return id;
                }
            } catch (Throwable e) {
                logWarning("[MOTM] Terra runtime block candidate skipped: id=" + blockId
                        + " error=" + e.getMessage());
            }
        }
        return BlockType.UNKNOWN_ID;
    }

    public int resolveRuntimeFluidTypeId(String... fluidIds) {
        for (String fluidId : fluidIds) {
            int id = Fluid.getAssetMap().getIndexOrDefault(fluidId, Fluid.UNKNOWN_ID);
            if (id != Fluid.UNKNOWN_ID && id != Fluid.EMPTY_ID) {
                return id;
            }
        }
        for (String fluidId : fluidIds) {
            int id = Fluid.getFluidIdOrUnknown(fluidId, "MOTM Terra runtime fluid");
            if (id != Fluid.UNKNOWN_ID && id != Fluid.EMPTY_ID) {
                return id;
            }
        }
        return Fluid.UNKNOWN_ID;
    }

    public Vector3i surfaceOverlayAnchor(Vector3d center) {
        Vector3i anchor = surfaceDecorationAnchor(center);
        return new Vector3i(anchor.x, anchor.y + 1, anchor.z);
    }

    public Vector3i blockAnchor(Vector3d center) {
        return new Vector3i(
                (int) Math.floor(center.x),
                (int) Math.floor(center.y),
                (int) Math.floor(center.z)
        );
    }

    public Vector3i surfaceDecorationAnchor(Vector3d center) {
        return blockAnchor(center);
    }

    private List<Ref<EntityStore>> collectNearbyNpcTargets(Store<EntityStore> store,
                                                           Vector3d center,
                                                           double radius,
                                                           int maxTargets) {
        if (store == null || center == null || support == null) {
            return List.of();
        }
        List<NearbyTargetCandidate> candidates = new ArrayList<>();

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid()) {
                    continue;
                }

                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning() || support.isMotmSummon(npc)) {
                    continue;
                }

                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }

                TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
                if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                    continue;
                }

                double candidateDistance = distance(center, transform.getTransform().getPosition());
                if (candidateDistance <= radius) {
                    candidates.add(new NearbyTargetCandidate(ref, candidateDistance));
                }
            }
        });

        candidates.sort((left, right) -> Double.compare(left.distance(), right.distance()));
        List<Ref<EntityStore>> targets = new ArrayList<>();
        for (NearbyTargetCandidate candidate : candidates) {
            targets.add(candidate.ref());
            if (maxTargets > 0 && targets.size() >= maxTargets) {
                break;
            }
        }
        return List.copyOf(targets);
    }

    private static BlockSelection baseSelection(Vector3i anchor) {
        BlockSelection selection = new BlockSelection();
        selection.setPosition(anchor.x, anchor.y, anchor.z);
        selection.setAnchorAtWorldPos(anchor.x, anchor.y, anchor.z);
        return selection;
    }

    private static Vector3i horizontalStep(Vector3d direction) {
        Vector3d step = direction != null ? new Vector3d(direction) : new Vector3d(1.0, 0.0, 0.0);
        step.y = 0.0;
        if (!step.isFinite() || step.length() < 0.001) {
            step = new Vector3d(1.0, 0.0, 0.0);
        } else {
            step.normalize();
        }
        if (Math.abs(step.x) >= Math.abs(step.z)) {
            return new Vector3i(step.x >= 0.0 ? 1 : -1, 0, 0);
        }
        return new Vector3i(0, 0, step.z >= 0.0 ? 1 : -1);
    }

    private static Vector3d normalizeHorizontal(Vector3d vector) {
        Vector3d horizontal = vector == null ? new Vector3d(0.0, 0.0, -1.0) : new Vector3d(vector.x, 0.0, vector.z);
        if (!horizontal.isFinite() || horizontal.length() < 0.001) {
            return new Vector3d(0.0, 0.0, -1.0);
        }
        horizontal.normalize();
        return horizontal;
    }

    private static Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }
        return transform.getTransform().getPosition();
    }

    private static Vector3d subtract(Vector3d left, Vector3d right) {
        return new Vector3d(left.x - right.x, left.y - right.y, left.z - right.z);
    }

    private static double dot(Vector3d left, Vector3d right) {
        return (left.x * right.x) + (left.y * right.y) + (left.z * right.z);
    }

    private static double distance(Vector3d left, Vector3d right) {
        Vector3d delta = subtract(left, right);
        return Math.sqrt((delta.x * delta.x) + (delta.y * delta.y) + (delta.z * delta.z));
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static String formatVector(Vector3d vector) {
        if (vector == null) {
            return "(null)";
        }
        return "("
                + String.format(Locale.US, "%.2f", vector.x)
                + ","
                + String.format(Locale.US, "%.2f", vector.y)
                + ","
                + String.format(Locale.US, "%.2f", vector.z)
                + ")";
    }

    private void logInfo(String message) {
        if (support != null) {
            support.logInfo(message);
        }
    }

    private void logWarning(String message) {
        if (support != null) {
            support.logWarning(message);
        }
    }

    public interface Support {
        boolean isMotmSummon(NPCEntity npc);

        void recordServerTruth(String type, java.util.Map<String, Object> data);

        void logInfo(String message);

        void logWarning(String message);
    }

    private record NearbyTargetCandidate(Ref<EntityStore> ref, double distance) {
    }
}
