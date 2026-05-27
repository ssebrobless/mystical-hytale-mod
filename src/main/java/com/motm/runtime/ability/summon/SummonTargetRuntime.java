package com.motm.runtime.ability.summon;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class SummonTargetRuntime {

    public Ref<EntityStore> resolveTarget(ActiveSummon summon, long now, Hooks hooks) {
        if (summon == null || hooks == null) {
            return null;
        }

        Vector3d summonPosition = hooks.position(summon.ref());
        Vector3d ownerPosition = hooks.position(summon.ownerRef());
        Vector3d anchor = summonPosition != null ? summonPosition : ownerPosition;

        if (summon.currentTargetRef() != null
                && now < summon.targetLockExpireAtMillis()
                && hooks.isValidTarget(summon.currentTargetRef(), anchor, summon.chaseRange() + 2.0)) {
            return summon.currentTargetRef();
        }

        Ref<EntityStore> targetRef = switch (summon.role()) {
            case "tank" -> hooks.findNearest(
                    ownerPosition != null ? ownerPosition : anchor,
                    Math.max(8.0, summon.chaseRange()));
            case "clone" -> hooks.findNearest(
                    ownerPosition != null ? ownerPosition : anchor,
                    Math.max(8.0, summon.attackRange() + 3.0));
            default -> hooks.findNearest(anchor, summon.chaseRange());
        };

        summon.setTargetLock(targetRef, now);
        return targetRef;
    }

    public interface Hooks {
        Vector3d position(Ref<EntityStore> ref);

        boolean isValidTarget(Ref<EntityStore> targetRef, Vector3d anchor, double radius);

        Ref<EntityStore> findNearest(Vector3d anchor, double radius);
    }
}
