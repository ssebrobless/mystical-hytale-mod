package com.motm.runtime.ability.terrain;

import org.joml.Vector3i;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;

public record TemporaryTerrainSelection(String reason,
                                        World world,
                                        Vector3i anchor,
                                        BlockSelection originalSelection,
                                        long expireAtMillis) {

    public TemporaryTerrainSelection {
        anchor = cloneOrNull(anchor);
    }

    public boolean expired(long now) {
        return now >= expireAtMillis;
    }

    public boolean belongsTo(World candidateWorld) {
        return sameWorld(world, candidateWorld);
    }

    public boolean matches(World candidateWorld, String candidateReason) {
        return reason != null
                && reason.equals(candidateReason)
                && belongsTo(candidateWorld);
    }

    private static boolean sameWorld(World first, World second) {
        return first != null && second != null && (first == second || first.equals(second));
    }

    private static Vector3i cloneOrNull(Vector3i vector) {
        return vector == null ? null : new Vector3i(vector.x, vector.y, vector.z);
    }
}
