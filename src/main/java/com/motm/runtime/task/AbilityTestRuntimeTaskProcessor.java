package com.motm.runtime.task;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.runtime.MotmRuntimeTasks;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Resolves queued single-ability test requests into normal queued ability casts.
 */
public final class AbilityTestRuntimeTaskProcessor implements RuntimeTaskProcessor {

    private final MotmRuntimeTasks tasks;
    private final Hooks hooks;
    private final Logger log;

    public AbilityTestRuntimeTaskProcessor(MotmRuntimeTasks tasks, Hooks hooks, Logger log) {
        this.tasks = tasks;
        this.hooks = hooks;
        this.log = log;
    }

    @Override
    public String id() {
        return "ability-test";
    }

    @Override
    public void process(Store<EntityStore> currentStore) {
        for (Map.Entry<String, String> entry : tasks.pendingSingleAbilityTests().entrySet()) {
            String playerId = entry.getKey();
            String abilityId = entry.getValue();
            Player player = hooks.runtimePlayer(playerId);
            if (player == null) {
                tasks.recordTaskSkipped("style-ability-test", playerId, Map.of(
                        "abilityId", abilityId,
                        "reason", "player_unavailable"
                ));
                tasks.completeStyleAbilityTest(playerId);
                continue;
            }
            if (!hooks.isPlayerInStore(player, currentStore)) {
                tasks.recordTaskSkipped("style-ability-test", playerId, Map.of(
                        "abilityId", abilityId,
                        "reason", "wrong_store"
                ));
                continue;
            }

            Ref<EntityStore> targetRef = null;
            Vector3i targetBlock = null;
            for (Ref<EntityStore> candidate : hooks.styleTestTargets(playerId)) {
                Vector3d position = hooks.entityPosition(currentStore, candidate);
                if (position == null) {
                    continue;
                }
                targetRef = candidate;
                targetBlock = new Vector3i(
                        (int) Math.floor(position.x),
                        (int) Math.floor(position.y),
                        (int) Math.floor(position.z)
                );
                break;
            }

            log.info("[MOTM] Live ability test target: playerId=" + playerId
                    + " abilityId=" + abilityId
                    + " hasTarget=" + (targetRef != null)
                    + " targetBlock=" + targetBlock);
            hooks.queueAbilityCast(playerId, abilityId, targetRef, targetBlock, true);
            tasks.recordTaskExecuted("style-ability-test", playerId, Map.of(
                    "abilityId", abilityId,
                    "hasTarget", targetRef != null,
                    "targetBlock", String.valueOf(targetBlock)
            ));
            tasks.completeStyleAbilityTest(playerId);
        }
    }

    public interface Hooks {
        Player runtimePlayer(String playerId);

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);

        List<Ref<EntityStore>> styleTestTargets(String playerId);

        Vector3d entityPosition(Store<EntityStore> currentStore, Ref<EntityStore> ref);

        void queueAbilityCast(String playerId,
                              String abilityId,
                              Ref<EntityStore> targetRef,
                              Vector3i targetBlock,
                              boolean notifyFailures);
    }
}
