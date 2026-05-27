package com.motm.runtime.task;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3i;
import com.motm.runtime.MotmRuntimeTasks;
import com.motm.runtime.PendingAbilityCast;

import java.util.logging.Logger;

/**
 * Command-facing policy for queueing ability casts into the runtime task loop.
 */
public final class AbilityCastCommandActions {

    private final MotmRuntimeTasks runtimeTasks;
    private final Logger log;

    public AbilityCastCommandActions(MotmRuntimeTasks runtimeTasks, Logger log) {
        this.runtimeTasks = runtimeTasks;
        this.log = log;
    }

    public void queue(String playerId,
                      String abilityId,
                      Ref<EntityStore> targetRef,
                      Vector3i targetBlock,
                      boolean notifyFailures) {
        if (playerId == null || playerId.isBlank() || abilityId == null || abilityId.isBlank()) {
            return;
        }

        log.info("[MOTM] Queue ability cast: playerId=" + playerId
                + " abilityId=" + abilityId
                + " notifyFailures=" + notifyFailures);
        runtimeTasks.enqueueAbilityCast(new PendingAbilityCast(
                playerId,
                abilityId,
                targetRef,
                targetBlock,
                notifyFailures
        ));
    }
}
