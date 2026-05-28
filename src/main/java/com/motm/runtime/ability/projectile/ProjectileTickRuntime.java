package com.motm.runtime.ability.projectile;

import com.hypixel.hytale.component.Ref;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;

public final class ProjectileTickRuntime {

    public boolean process(ActiveProjectile projectile, long now, double maxStepDistance, Hooks hooks) {
        if (projectile == null || hooks == null) {
            return true;
        }
        if (projectile.ownerRef() == null || !projectile.ownerRef().isValid()) {
            return true;
        }

        if (!hooks.hasOwnerStore(projectile)) {
            hooks.despawnVisual(projectile);
            return true;
        }

        PlayerData player = hooks.player(projectile.ownerPlayerId());
        if (player == null) {
            hooks.despawnVisual(projectile);
            return true;
        }

        if (now < projectile.activateAtMillis()) {
            hooks.refreshVisual(projectile, now);
            return false;
        }

        Vector3d from = new Vector3d(projectile.position());
        Vector3d stepDirection = normalize(projectile.direction());
        double stepDistance = Math.min(projectile.speedPerTick(), maxStepDistance);
        Vector3d to = com.motm.util.MotmVectors.addScaled(from, stepDirection, stepDistance);

        projectile.advanceTo(to, stepDistance);
        hooks.syncVisual(projectile, now);

        if (projectile.ownerSelfClearanceDistance() > 0.0
                && projectile.travelledDistance() < projectile.ownerSelfClearanceDistance()
                && !expiredByTimeOrRange(projectile, now)) {
            return false;
        }

        boolean piercing = hooks.isPiercing(projectile.ability());
        if (piercing) {
            hooks.applyTraversalHits(projectile, player, from, to);
        }

        Ref<EntityStore> directHit = hooks.resolveHit(projectile, from, to);
        boolean expired = expiredByTimeOrRange(projectile, now);
        if (piercing) {
            if (expired) {
                hooks.despawnVisual(projectile);
            }
            return expired;
        }
        if (directHit == null && !expired) {
            return false;
        }

        hooks.applyImpact(projectile, player, to, directHit);
        if (hooks.shouldLeaveVisualOnImpact(projectile.ability())) {
            hooks.untrackVisual(projectile.visualRef());
        } else {
            hooks.despawnVisual(projectile);
        }
        return true;
    }

    static boolean expiredByTimeOrRange(ActiveProjectile projectile, long now) {
        return projectile != null
                && (now >= projectile.expireAtMillis()
                || projectile.travelledDistance() >= projectile.maxDistance());
    }

    private static Vector3d normalize(Vector3d vector) {
        if (vector == null || !vector.isFinite()) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        Vector3d normalized = new Vector3d(vector);
        if (!normalized.isFinite() || normalized.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        normalized.normalize();
        return normalized;
    }

    public interface Hooks {
        boolean hasOwnerStore(ActiveProjectile projectile);

        PlayerData player(String ownerPlayerId);

        void refreshVisual(ActiveProjectile projectile, long now);

        void syncVisual(ActiveProjectile projectile, long now);

        boolean isPiercing(AbilityData ability);

        void applyTraversalHits(ActiveProjectile projectile,
                                PlayerData player,
                                Vector3d from,
                                Vector3d to);

        Ref<EntityStore> resolveHit(ActiveProjectile projectile,
                                    Vector3d from,
                                    Vector3d to);

        void applyImpact(ActiveProjectile projectile,
                         PlayerData player,
                         Vector3d impactPosition,
                         Ref<EntityStore> directHit);

        boolean shouldLeaveVisualOnImpact(AbilityData ability);

        void untrackVisual(Ref<EntityStore> visualRef);

        void despawnVisual(ActiveProjectile projectile);
    }
}
