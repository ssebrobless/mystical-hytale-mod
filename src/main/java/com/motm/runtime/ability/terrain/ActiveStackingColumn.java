package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;

public final class ActiveStackingColumn {
    private final String reason;
    private final World world;
    private final Vector3i anchor;
    private final int blockTypeId;
    private final int height;
    private final long expireAtMillis;
    private long nextStageAtMillis;
    private int placedHeight;

    public ActiveStackingColumn(String reason,
                                World world,
                                Vector3i anchor,
                                int blockTypeId,
                                int height,
                                long expireAtMillis,
                                long nextStageAtMillis) {
        this.reason = reason;
        this.world = world;
        this.anchor = cloneOrNull(anchor);
        this.blockTypeId = blockTypeId;
        this.height = Math.max(1, height);
        this.expireAtMillis = expireAtMillis;
        this.nextStageAtMillis = nextStageAtMillis;
        this.placedHeight = 0;
    }

    public String reason() { return reason; }
    public World world() { return world; }
    public Vector3i anchor() { return anchor; }
    public int blockTypeId() { return blockTypeId; }
    public int height() { return height; }
    public long expireAtMillis() { return expireAtMillis; }
    public long nextStageAtMillis() { return nextStageAtMillis; }
    public int placedHeight() { return placedHeight; }

    public boolean expired(long now) {
        return now >= expireAtMillis;
    }

    public boolean complete() {
        return placedHeight >= height;
    }

    public boolean readyToStage(long now) {
        return now >= nextStageAtMillis;
    }

    public boolean belongsTo(World candidateWorld) {
        return world != null && candidateWorld != null && (world == candidateWorld || world.equals(candidateWorld));
    }

    public Vector3i nextBlockAnchor() {
        return new Vector3i(anchor.getX(), anchor.getY() + placedHeight, anchor.getZ());
    }

    public void markStagePlaced(long now) {
        placedHeight++;
        nextStageAtMillis = now + TerrainRuntimeSpecs.STACKING_COLUMN_STAGE_INTERVAL_MS;
    }

    private static Vector3i cloneOrNull(Vector3i vector) {
        return vector == null ? null : new Vector3i(vector.getX(), vector.getY(), vector.getZ());
    }
}
