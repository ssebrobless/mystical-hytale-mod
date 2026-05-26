package com.motm.runtime;

import com.hypixel.hytale.component.Ref;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record PendingAbilityCast(
        String playerId,
        String abilityId,
        Ref<EntityStore> targetRef,
        Vector3i targetBlock,
        boolean notifyFailures
) {
}
