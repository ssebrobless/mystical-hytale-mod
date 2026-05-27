package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;

public final class TerrainTickRuntime {

    public boolean processTemporarySelection(TemporaryTerrainSelection selection, long now, Hooks hooks) {
        if (selection == null || !selection.expired(now)) {
            return false;
        }
        if (hooks == null || !hooks.belongsToCurrentWorld(selection)) {
            return false;
        }
        hooks.restoreSelection(selection);
        return true;
    }

    public boolean processMovingTrail(ActiveMovingTerrainTrail trail, long now, Hooks hooks) {
        if (trail == null || trail.expired(now)) {
            return true;
        }
        if (!trail.readyToPlace(now)) {
            return false;
        }
        if (hooks == null || !hooks.ownerBelongsToCurrentStore(trail)) {
            return false;
        }

        Vector3d position = hooks.ownerPosition(trail);
        if (position == null) {
            return true;
        }
        trail.initializeLastPosition(position);

        int blockTypeId = hooks.resolveBlockTypeId(trail);
        if (!hooks.usableBlockType(blockTypeId)) {
            hooks.warnMissingBlockType(trail);
            return true;
        }

        Vector3d delta = subtract(position, trail.lastPosition());
        double distance = Math.sqrt((delta.x * delta.x) + (delta.z * delta.z));
        if (distance < TerrainRuntimeSpecs.MOVING_TRAIL_MIN_DISTANCE) {
            trail.scheduleNextPlacement(now + TerrainRuntimeSpecs.MOVING_TRAIL_STATIONARY_RECHECK_MS);
            return false;
        }

        Vector3d travel = normalizeHorizontal(delta);
        Vector3i right = horizontalRightStep(travel);
        int stamps = Math.max(1, Math.min(
                TerrainRuntimeSpecs.MOVING_TRAIL_MAX_STAMPS,
                (int) Math.ceil(distance / TerrainRuntimeSpecs.MOVING_TRAIL_STAMP_SPACING)));
        int placed = 0;
        for (int index = 1; index <= stamps; index++) {
            double factor = index / (double) (stamps + 1);
            Vector3d trailCenter = trail.lastPosition().clone().addScaled(delta, factor);
            Vector3i anchor = hooks.surfaceOverlayAnchor(trailCenter);
            if (sameBlock(trail.lastAnchor(), anchor)) {
                continue;
            }

            hooks.placeTrailStamp(trail, anchor, right, blockTypeId, Math.min(trail.expireAtMillis(), now + 4500L));
            trail.markPlaced(anchor);
            placed++;
        }
        trail.updateLastPosition(position);
        trail.scheduleNextPlacement(now + (placed > 0
                ? TerrainRuntimeSpecs.MOVING_TRAIL_PLACED_RECHECK_MS
                : TerrainRuntimeSpecs.MOVING_TRAIL_EMPTY_RECHECK_MS));
        return false;
    }

    public boolean processStackingColumn(ActiveStackingColumn column, long now, Hooks hooks) {
        if (column == null || column.expired(now) || column.complete()) {
            return true;
        }
        if (hooks == null || !hooks.belongsToCurrentWorld(column)) {
            return false;
        }
        if (!column.readyToStage(now)) {
            return false;
        }

        Vector3i block = column.nextBlockAnchor();
        hooks.placeColumnStage(column, block);
        column.markStagePlaced(now);
        return column.complete();
    }

    private static Vector3d subtract(Vector3d a, Vector3d b) {
        if (a == null || b == null) {
            return new Vector3d(0.0, 0.0, 0.0);
        }
        return new Vector3d(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    private static Vector3d normalizeHorizontal(Vector3d vector) {
        if (vector == null || !vector.isFinite()) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        Vector3d normalized = new Vector3d(vector.x, 0.0, vector.z);
        if (!normalized.isFinite() || normalized.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        normalized.normalize();
        return normalized;
    }

    private static Vector3i horizontalRightStep(Vector3d forward) {
        if (forward == null || !forward.isFinite()) {
            return new Vector3i(1, 0, 0);
        }
        double absX = Math.abs(forward.x);
        double absZ = Math.abs(forward.z);
        if (absX >= absZ) {
            return new Vector3i(0, 0, forward.x >= 0 ? -1 : 1);
        }
        return new Vector3i(forward.z >= 0 ? 1 : -1, 0, 0);
    }

    private static boolean sameBlock(Vector3i first, Vector3i second) {
        return first != null
                && second != null
                && first.getX() == second.getX()
                && first.getY() == second.getY()
                && first.getZ() == second.getZ();
    }

    public interface Hooks {
        boolean belongsToCurrentWorld(TemporaryTerrainSelection selection);

        boolean belongsToCurrentWorld(ActiveStackingColumn column);

        void restoreSelection(TemporaryTerrainSelection selection);

        boolean ownerBelongsToCurrentStore(ActiveMovingTerrainTrail trail);

        Vector3d ownerPosition(ActiveMovingTerrainTrail trail);

        int resolveBlockTypeId(ActiveMovingTerrainTrail trail);

        boolean usableBlockType(int blockTypeId);

        void warnMissingBlockType(ActiveMovingTerrainTrail trail);

        Vector3i surfaceOverlayAnchor(Vector3d center);

        void placeTrailStamp(ActiveMovingTerrainTrail trail,
                             Vector3i anchor,
                             Vector3i right,
                             int blockTypeId,
                             long expireAtMillis);

        void placeColumnStage(ActiveStackingColumn column, Vector3i block);
    }
}
