package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;

public final class TerrainHytaleAdapter {
    private final TerrainTickRuntime tickRuntime;
    private final Support support;

    public TerrainHytaleAdapter(TerrainTickRuntime tickRuntime, Support support) {
        this.tickRuntime = tickRuntime;
        this.support = support;
    }

    public int restoreSelectionsForWorld(TerrainRuntimeState terrainState, World world, String context) {
        if (terrainState == null || world == null) {
            return 0;
        }
        return terrainState.restoreSelectionsForWorld(world, selection -> restoreTemporarySelection(selection, context));
    }

    public void restoreActiveTemporarySelections(TerrainRuntimeState terrainState, World world, String reason) {
        if (terrainState == null || world == null || reason == null || reason.isBlank()) {
            return;
        }
        terrainState.restoreSelectionsByReason(world, reason, selection -> {
            try {
                selection.originalSelection().place(null, selection.world(), Vector3i.ZERO, BlockMask.EMPTY);
                logInfo("[MOTM] Temporary Terra terrain restored before replacement: reason=" + selection.reason()
                        + " anchor=" + selection.anchor());
                return true;
            } catch (Throwable e) {
                logWarning("[MOTM] Temporary Terra terrain replacement restore failed: reason=" + selection.reason()
                        + " anchor=" + selection.anchor()
                        + " error=" + e.getMessage());
                return false;
            }
        });
    }

    public boolean processTemporarySelection(TemporaryTerrainSelection selection,
                                             Store<EntityStore> currentStore,
                                             long now) {
        return tickRuntime.processTemporarySelection(selection, now, hooks(currentStore));
    }

    public boolean processMovingTrail(ActiveMovingTerrainTrail trail,
                                      Store<EntityStore> currentStore,
                                      long now) {
        return tickRuntime.processMovingTrail(trail, now, hooks(currentStore));
    }

    public boolean processStackingColumn(ActiveStackingColumn column,
                                         Store<EntityStore> currentStore,
                                         long now) {
        return tickRuntime.processStackingColumn(column, now, hooks(currentStore));
    }

    public boolean restoreTemporarySelection(TemporaryTerrainSelection selection, String context) {
        if (selection == null || selection.originalSelection() == null) {
            return false;
        }
        try {
            selection.originalSelection().place(null, selection.world(), Vector3i.ZERO, BlockMask.EMPTY);
            logInfo("[MOTM] Temporary Terra terrain restored: reason=" + selection.reason()
                    + " anchor=" + selection.anchor()
                    + (context == null || context.isBlank() ? "" : " context=" + context));
            return true;
        } catch (Throwable e) {
            logWarning("[MOTM] Temporary Terra terrain restore failed: reason=" + selection.reason()
                    + " anchor=" + selection.anchor()
                    + (context == null || context.isBlank() ? "" : " context=" + context)
                    + " error=" + e.getMessage());
            return false;
        }
    }

    private TerrainTickRuntime.Hooks hooks(Store<EntityStore> currentStore) {
        return new TerrainTickRuntime.Hooks() {
            @Override
            public boolean belongsToCurrentWorld(TemporaryTerrainSelection selection) {
                World currentWorld = currentWorld(currentStore);
                return selection != null && selection.belongsTo(currentWorld);
            }

            @Override
            public boolean belongsToCurrentWorld(ActiveStackingColumn column) {
                World currentWorld = currentWorld(currentStore);
                return column != null && column.belongsTo(currentWorld);
            }

            @Override
            public void restoreSelection(TemporaryTerrainSelection selection) {
                restoreTemporarySelection(selection, "");
            }

            @Override
            public boolean ownerBelongsToCurrentStore(ActiveMovingTerrainTrail trail) {
                return trail != null && belongsToCurrentStore(trail.ownerRef(), currentStore);
            }

            @Override
            public Vector3d ownerPosition(ActiveMovingTerrainTrail trail) {
                return trail == null ? null : position(trail.ownerRef(), currentStore);
            }

            @Override
            public int resolveBlockTypeId(ActiveMovingTerrainTrail trail) {
                return trail == null || support == null
                        ? BlockType.UNKNOWN_ID
                        : support.resolveRuntimeBlockTypeId(trail.blockIdArray());
            }

            @Override
            public boolean usableBlockType(int blockTypeId) {
                return blockTypeId != BlockType.UNKNOWN_ID && blockTypeId != BlockType.EMPTY_ID;
            }

            @Override
            public void warnMissingBlockType(ActiveMovingTerrainTrail trail) {
                logWarning("[MOTM] Moving Terra terrain trail skipped: reason=" + trail.reason()
                        + " no block id resolved.");
            }

            @Override
            public Vector3i surfaceOverlayAnchor(Vector3d center) {
                return support == null ? new Vector3i() : support.surfaceOverlayAnchor(center);
            }

            @Override
            public void placeTrailStamp(ActiveMovingTerrainTrail trail,
                                        Vector3i anchor,
                                        Vector3i right,
                                        int blockTypeId,
                                        long expireAtMillis) {
                BlockSelection selection = baseSelection(anchor);
                selection.addBlockAtWorldPos(anchor.getX(), anchor.getY(), anchor.getZ(), blockTypeId, 0, 0, 0);
                selection.addBlockAtWorldPos(anchor.getX() + right.getX(), anchor.getY(), anchor.getZ() + right.getZ(), blockTypeId, 0, 0, 0);
                selection.addBlockAtWorldPos(anchor.getX() - right.getX(), anchor.getY(), anchor.getZ() - right.getZ(), blockTypeId, 0, 0, 0);
                if (support != null) {
                    support.placeTemporarySelection(trail.world(), trail.reason(), anchor, selection,
                            expireAtMillis, "3 surface trail flowers on movement path");
                }
            }

            @Override
            public void placeColumnStage(ActiveStackingColumn column, Vector3i block) {
                BlockSelection selection = baseSelection(block);
                selection.addBlockAtWorldPos(block.getX(), block.getY(), block.getZ(), column.blockTypeId(), 0, 0, 0);
                if (support != null) {
                    support.placeTemporarySelection(column.world(), column.reason(), block, selection, column.expireAtMillis(),
                            "pillar stage " + (column.placedHeight() + 1) + "/" + column.height());
                }
            }
        };
    }

    private static World currentWorld(Store<EntityStore> store) {
        return store != null && store.getExternalData() != null
                ? store.getExternalData().getWorld()
                : null;
    }

    private static boolean belongsToCurrentStore(Ref<EntityStore> ref, Store<EntityStore> currentStore) {
        return ref != null && ref.isValid() && ref.getStore() == currentStore;
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

    private static BlockSelection baseSelection(Vector3i anchor) {
        BlockSelection selection = new BlockSelection();
        selection.setPosition(anchor.getX(), anchor.getY(), anchor.getZ());
        selection.setAnchorAtWorldPos(anchor.getX(), anchor.getY(), anchor.getZ());
        return selection;
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
        int resolveRuntimeBlockTypeId(String... blockIds);

        Vector3i surfaceOverlayAnchor(Vector3d center);

        String placeTemporarySelection(World world,
                                       String reason,
                                       Vector3i anchor,
                                       BlockSelection selection,
                                       long expireAtMillis,
                                       String summary);

        void logInfo(String message);

        void logWarning(String message);
    }
}
