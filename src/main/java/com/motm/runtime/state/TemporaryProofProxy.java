package com.motm.runtime.state;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/**
 * Temporary proof proxy entity that should despawn after its lifetime.
 */
public record TemporaryProofProxy(
        String proofId,
        World world,
        Ref<EntityStore> ref,
        long cleanupAtMillis
) {}
