package com.motm.runtime.state;

import org.joml.Vector3i;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;

/**
 * Temporary proof placement that should be restored after its lifetime.
 */
public record TemporaryProofSelection(
        String proofId,
        World world,
        Vector3i anchor,
        BlockSelection originalSelection,
        long cleanupAtMillis
) {}
