package com.motm.runtime.ability.summon;

import com.hypixel.hytale.component.Ref;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.PlayerData;

public final class SummonTickRuntime {

    public boolean process(ActiveSummon summon, long now, Hooks hooks) {
        if (summon == null || summon.ref() == null || !summon.ref().isValid()) {
            return true;
        }

        if (now >= summon.expireAtMillis()) {
            return hooks != null && hooks.despawn(summon);
        }

        if (now < summon.nextThinkAtMillis()) {
            return false;
        }

        if (hooks == null || !hooks.hasStore(summon)) {
            return true;
        }

        PlayerData owner = hooks.owner(summon.ownerPlayerId());
        if (owner == null) {
            return hooks.despawn(summon);
        }

        if (now < summon.hatchAtMillis()) {
            summon.scheduleNextThink(now);
            return false;
        }

        if (!summon.awakened()) {
            hooks.awaken(summon, now);
        }

        if (summon.ownerRef() == null || !summon.ownerRef().isValid()) {
            return hooks.despawn(summon);
        }

        Ref<EntityStore> targetRef = hooks.resolveTarget(summon, now);

        if (targetRef == null || !targetRef.isValid()) {
            summon.clearTargetLock();
            hooks.moveTowardOwner(summon);
            summon.scheduleNextThink(now);
            return false;
        }

        Vector3d summonPosition = hooks.position(summon.ref());
        Vector3d targetPosition = hooks.position(targetRef);
        if (summonPosition == null || targetPosition == null) {
            summon.scheduleNextThink(now);
            return false;
        }

        double distanceToTarget = distance(summonPosition, targetPosition);
        if (distanceToTarget > summon.attackRange()) {
            hooks.moveTowardTarget(summon, targetRef, summon.attackRange() * 0.8);
            summon.scheduleNextThink(now);
            return false;
        }

        if (summon.ranged() && !"clone".equals(summon.role()) && distanceToTarget < summon.attackRange() * 0.45) {
            hooks.moveAwayFromTarget(summon, targetRef, summon.attackRange() * 0.72);
        }

        if (now >= summon.nextAttackAtMillis()) {
            hooks.attack(summon, owner, targetRef, now);
        }

        summon.scheduleNextThink(now);
        return false;
    }

    private static double distance(Vector3d a, Vector3d b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    }

    public interface Hooks {
        boolean despawn(ActiveSummon summon);

        boolean hasStore(ActiveSummon summon);

        PlayerData owner(String ownerPlayerId);

        void awaken(ActiveSummon summon, long now);

        Ref<EntityStore> resolveTarget(ActiveSummon summon, long now);

        void moveTowardOwner(ActiveSummon summon);

        Vector3d position(Ref<EntityStore> ref);

        void moveTowardTarget(ActiveSummon summon, Ref<EntityStore> targetRef, double desiredRange);

        void moveAwayFromTarget(ActiveSummon summon, Ref<EntityStore> targetRef, double desiredDistance);

        void attack(ActiveSummon summon, PlayerData owner, Ref<EntityStore> targetRef, long now);
    }
}
