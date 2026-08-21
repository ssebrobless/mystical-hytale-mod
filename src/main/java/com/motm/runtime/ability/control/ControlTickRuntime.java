package com.motm.runtime.ability.control;

import com.hypixel.hytale.component.Ref;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.PlayerData;

/**
 * Pure per-tick state machine for a controlled ally, mirroring
 * {@code SummonTickRuntime}. All engine/world interaction is injected through
 * {@link Hooks} so this logic is unit-testable without a live server.
 *
 * <p>Encodes the G2 acceptance contract: a converted hostile follows the caster
 * when idle, acquires and attacks other hostiles, wears a visible marker, and
 * releases cleanly on expiry / owner loss / its own removal. Friendly-fire
 * safety (caster and allies not targetable, controlled ally not damageable by
 * owner) is enforced by the damage filter + {@code resolveHostileTarget}
 * exclusion on the adapter side.
 *
 * @return {@code true} when control has ended and the ally should be removed
 *         from {@link ControlRuntimeState}; {@code false} while control persists.
 */
public final class ControlTickRuntime {

    public boolean process(ActiveControlledAlly ally, long now, Hooks hooks) {
        if (ally == null || ally.controlledRef() == null || !ally.controlledRef().isValid()) {
            // Controlled entity is gone (died/despawned): stop tracking. Nothing to restore.
            return true;
        }

        if (ally.isExpired(now)) {
            return hooks != null && hooks.release(ally);
        }

        if (now < ally.nextThinkAtMillis()) {
            return false;
        }

        if (hooks == null || !hooks.hasStore(ally)) {
            return true;
        }

        PlayerData owner = hooks.owner(ally.ownerPlayerId());
        if (owner == null) {
            return hooks.release(ally);
        }

        if (ally.ownerRef() == null || !ally.ownerRef().isValid()) {
            return hooks.release(ally);
        }

        if (!ally.markerApplied()) {
            hooks.applyMarker(ally);
            ally.markMarkerApplied();
        }

        Ref<EntityStore> targetRef = hooks.resolveHostileTarget(ally, now);
        if (targetRef == null || !targetRef.isValid()) {
            hooks.followOwner(ally);
            ally.scheduleNextThink(now);
            return false;
        }

        Vector3d allyPosition = hooks.position(ally.controlledRef());
        Vector3d targetPosition = hooks.position(targetRef);
        if (allyPosition == null || targetPosition == null) {
            ally.scheduleNextThink(now);
            return false;
        }

        double distanceToTarget = distance(allyPosition, targetPosition);
        if (distanceToTarget > ally.attackRange()) {
            hooks.moveTowardTarget(ally, targetRef, ally.attackRange() * 0.9);
        } else if (now >= ally.nextAttackAtMillis()) {
            hooks.attack(ally, owner, targetRef, now);
            ally.scheduleNextAttack(now);
        }

        ally.scheduleNextThink(now);
        return false;
    }

    private static double distance(Vector3d a, Vector3d b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    }

    /** Engine-side operations for a controlled ally; implemented by the Hytale adapter. */
    public interface Hooks {
        /** Restores the NPC's original attitude, removes the marker, and untracks it. Returns {@code true}. */
        boolean release(ActiveControlledAlly ally);

        boolean hasStore(ActiveControlledAlly ally);

        PlayerData owner(String ownerPlayerId);

        /** Applies the pink control marker to the controlled ally (idempotent per control window). */
        void applyMarker(ActiveControlledAlly ally);

        /** Nearest valid hostile, excluding the owner, owner's allies, and other controlled allies. */
        Ref<EntityStore> resolveHostileTarget(ActiveControlledAlly ally, long now);

        void followOwner(ActiveControlledAlly ally);

        Vector3d position(Ref<EntityStore> ref);

        void moveTowardTarget(ActiveControlledAlly ally, Ref<EntityStore> targetRef, double desiredRange);

        void attack(ActiveControlledAlly ally, PlayerData owner, Ref<EntityStore> targetRef, long now);
    }
}
