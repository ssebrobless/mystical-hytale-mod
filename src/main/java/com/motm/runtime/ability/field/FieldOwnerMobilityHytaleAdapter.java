package com.motm.runtime.ability.field;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.StatusEffect;
import com.motm.runtime.ability.terrain.LavaHazardRuntimeState;

import java.util.Locale;

public final class FieldOwnerMobilityHytaleAdapter {
    private final LavaHazardRuntimeState lavaHazardState;
    private final Support support;

    public FieldOwnerMobilityHytaleAdapter(LavaHazardRuntimeState lavaHazardState, Support support) {
        this.lavaHazardState = lavaHazardState;
        this.support = support;
    }

    public void syncFollowOwnerAnchor(ActiveField field, Store<EntityStore> store) {
        if (field == null || !field.followOwner()) {
            return;
        }

        Vector3d ownerPosition = position(field.ownerRef(), store);
        if (ownerPosition != null) {
            field.updateCenter(ownerPosition);
        }
    }

    public void applyLavaPoolOwnerMobility(ActiveField field, Store<EntityStore> store) {
        if (field == null || store == null || field.ownerPlayerId() == null
                || !"lava_pool".equals(lower(field.ability().getId()))
                || lavaHazardState == null) {
            return;
        }

        lavaHazardState.protectUntil(field.ownerPlayerId(), field.expireAtMillis() + 1250L);
        Vector3d ownerPosition = position(field.ownerRef(), store);
        if (ownerPosition == null || distance(ownerPosition, field.center()) > Math.max(1.5, field.radius() + 0.5)) {
            clearLavaPoolOwnerVelocityBoost(field.ownerPlayerId(), field.ownerRef(), store);
            return;
        }

        if (support != null) {
            support.removeEffect(field.ownerPlayerId(), StatusEffect.Type.SLOW);
            support.removeEffect(field.ownerPlayerId(), StatusEffect.Type.SLOW_STACK);
            support.removeEffect(field.ownerPlayerId(), StatusEffect.Type.BURN);
        }
        applyLavaPoolOwnerMovementBoost(field.ownerPlayerId(), field.ownerRef(), store);
    }

    public void clearLavaPoolOwnerVelocityBoost(String playerId,
                                                Ref<EntityStore> ownerRef,
                                                Store<EntityStore> store) {
        clearLavaPoolOwnerMovementBoost(playerId, ownerRef, store);
        Vector3d previousBoost = lavaHazardState == null ? null : lavaHazardState.removeVelocityBoost(playerId);
        if (previousBoost == null || ownerRef == null || !ownerRef.isValid() || store == null) {
            return;
        }

        Velocity velocity = store.getComponent(ownerRef, Velocity.getComponentType());
        if (velocity == null) {
            return;
        }

        Vector3d currentVelocity = velocity.getVelocity();
        if (currentVelocity == null || !currentVelocity.isFinite()) {
            return;
        }

        velocity.set(
                currentVelocity.x - previousBoost.x,
                currentVelocity.y,
                currentVelocity.z - previousBoost.z
        );
    }

    private void applyLavaPoolOwnerMovementBoost(String playerId,
                                                 Ref<EntityStore> ownerRef,
                                                 Store<EntityStore> store) {
        if (playerId == null || playerId.isBlank() || ownerRef == null || !ownerRef.isValid() || store == null
                || lavaHazardState == null) {
            return;
        }
        try {
            MovementManager movementManager = store.getComponent(ownerRef, MovementManager.getComponentType());
            if (movementManager == null || movementManager.getSettings() == null) {
                return;
            }
            var settings = movementManager.getSettings();
            settings.baseSpeed = Math.max(settings.baseSpeed, 11.0f);
            settings.forwardWalkSpeedMultiplier = Math.max(settings.forwardWalkSpeedMultiplier, 1.15f);
            settings.backwardWalkSpeedMultiplier = Math.max(settings.backwardWalkSpeedMultiplier, 1.00f);
            settings.strafeWalkSpeedMultiplier = Math.max(settings.strafeWalkSpeedMultiplier, 1.15f);
            settings.forwardRunSpeedMultiplier = Math.max(settings.forwardRunSpeedMultiplier, 1.65f);
            settings.backwardRunSpeedMultiplier = Math.max(settings.backwardRunSpeedMultiplier, 1.25f);
            settings.strafeRunSpeedMultiplier = Math.max(settings.strafeRunSpeedMultiplier, 1.65f);
            settings.forwardSprintSpeedMultiplier = Math.max(settings.forwardSprintSpeedMultiplier, 1.85f);
            settings.acceleration = Math.max(settings.acceleration, 0.22f);
            settings.maxSpeedMultiplier = Math.max(settings.maxSpeedMultiplier, 20.0f);
            PlayerRef universePlayerRef = store.getComponent(ownerRef, PlayerRef.getComponentType());
            if (universePlayerRef != null && universePlayerRef.getPacketHandler() != null) {
                movementManager.update(universePlayerRef.getPacketHandler());
            }
            lavaHazardState.markMovementBoosted(playerId);
        } catch (Exception e) {
            if (support != null) {
                support.logWarning("[MOTM] Lava Pool movement compensation failed: playerId=" + playerId
                        + " error=" + e.getMessage());
            }
        }
    }

    private void clearLavaPoolOwnerMovementBoost(String playerId,
                                                 Ref<EntityStore> ownerRef,
                                                 Store<EntityStore> store) {
        if (playerId == null || lavaHazardState == null || !lavaHazardState.consumeMovementBoosted(playerId)
                || ownerRef == null || !ownerRef.isValid() || store == null) {
            return;
        }
        try {
            MovementManager movementManager = store.getComponent(ownerRef, MovementManager.getComponentType());
            if (movementManager == null) {
                return;
            }
            movementManager.applyDefaultSettings();
            PlayerRef universePlayerRef = store.getComponent(ownerRef, PlayerRef.getComponentType());
            if (universePlayerRef != null && universePlayerRef.getPacketHandler() != null) {
                movementManager.update(universePlayerRef.getPacketHandler());
            }
        } catch (Exception e) {
            if (support != null) {
                support.logWarning("[MOTM] Lava Pool movement compensation reset failed: playerId=" + playerId
                        + " error=" + e.getMessage());
            }
        }
    }

    private static Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }
        return transform.getTransform().getPosition();
    }

    private static double distance(Vector3d left, Vector3d right) {
        if (left == null || right == null) {
            return Double.MAX_VALUE;
        }
        Vector3d delta = new Vector3d(left.x - right.x, left.y - right.y, left.z - right.z);
        return Math.sqrt((delta.x * delta.x) + (delta.y * delta.y) + (delta.z * delta.z));
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public interface Support {
        void removeEffect(String entityId, StatusEffect.Type type);

        void logWarning(String message);
    }
}
