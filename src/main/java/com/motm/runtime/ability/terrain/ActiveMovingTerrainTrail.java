package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.component.Ref;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

public final class ActiveMovingTerrainTrail {
    private final String reason;
    private final World world;
    private final Ref<EntityStore> ownerRef;
    private final List<String> blockIds;
    private final long expireAtMillis;
    private long nextPlaceAtMillis;
    private Vector3i lastAnchor;
    private Vector3d lastPosition;

    public ActiveMovingTerrainTrail(String reason,
                                    World world,
                                    Ref<EntityStore> ownerRef,
                                    List<String> blockIds,
                                    long expireAtMillis,
                                    long nextPlaceAtMillis) {
        this.reason = reason;
        this.world = world;
        this.ownerRef = ownerRef;
        this.blockIds = blockIds == null ? List.of() : List.copyOf(blockIds);
        this.expireAtMillis = expireAtMillis;
        this.nextPlaceAtMillis = nextPlaceAtMillis;
    }

    public String reason() { return reason; }
    public World world() { return world; }
    public Ref<EntityStore> ownerRef() { return ownerRef; }
    public List<String> blockIds() { return blockIds; }
    public long expireAtMillis() { return expireAtMillis; }
    public long nextPlaceAtMillis() { return nextPlaceAtMillis; }
    public Vector3i lastAnchor() { return lastAnchor; }
    public Vector3d lastPosition() { return lastPosition; }

    public boolean expired(long now) {
        return now >= expireAtMillis;
    }

    public boolean readyToPlace(long now) {
        return now >= nextPlaceAtMillis;
    }

    public void initializeLastPosition(Vector3d position) {
        if (lastPosition == null) {
            lastPosition = cloneOrNull(position);
        }
    }

    public void updateLastPosition(Vector3d position) {
        lastPosition = cloneOrNull(position);
    }

    public void markPlaced(Vector3i anchor) {
        lastAnchor = cloneOrNull(anchor);
    }

    public void scheduleNextPlacement(long nextPlaceAtMillis) {
        this.nextPlaceAtMillis = nextPlaceAtMillis;
    }

    public String[] blockIdArray() {
        return blockIds.toArray(String[]::new);
    }

    private static Vector3d cloneOrNull(Vector3d vector) {
        return vector == null ? null : new Vector3d(vector);
    }

    private static Vector3i cloneOrNull(Vector3i vector) {
        return vector == null ? null : new Vector3i(vector.x, vector.y, vector.z);
    }
}
