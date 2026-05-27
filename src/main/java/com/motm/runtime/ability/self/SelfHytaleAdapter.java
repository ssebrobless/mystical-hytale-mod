package com.motm.runtime.ability.self;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.PlayerData;

public final class SelfHytaleAdapter {
    private static final long DEFAULT_SELF_EFFECT_INITIAL_DELAY_MS = 180L;
    private static final long SELF_EFFECT_REFRESH_INTERVAL_MS = 650L;
    private static final long ANCHOR_COMPLETION_EFFECT_DURATION_MS = 6_250L;

    private final SelfRuntimeState selfState;
    private final SelfActivationRuntime activationRuntime;
    private final Support support;

    public SelfHytaleAdapter(SelfRuntimeState selfState,
                             SelfActivationRuntime activationRuntime,
                             Support support) {
        this.selfState = selfState;
        this.activationRuntime = activationRuntime;
        this.support = support;
    }

    public void processForStore(Store<EntityStore> currentStore, long now) {
        selfState.removeProcessedPlayerAnchors(anchor ->
                belongsToCurrentStore(anchor.ownerRef(), currentStore) && processPlayerAnchor(anchor, currentStore, now));
        selfState.removeProcessedSelfEffects(effect ->
                belongsToCurrentStore(effect.ownerRef(), currentStore) && processSelfEffect(effect, currentStore, now));
    }

    public void startActiveSelfEffect(Ref<EntityStore> ownerRef,
                                      String ownerPlayerId,
                                      String effectId,
                                      long expireAtMillis) {
        startActiveSelfEffect(
                ownerRef,
                ownerPlayerId,
                effectId,
                expireAtMillis,
                System.currentTimeMillis() + DEFAULT_SELF_EFFECT_INITIAL_DELAY_MS
        );
    }

    public void startActiveSelfEffect(Ref<EntityStore> ownerRef,
                                      String ownerPlayerId,
                                      String effectId,
                                      long expireAtMillis,
                                      long nextApplyAtMillis) {
        if (ownerRef == null || !ownerRef.isValid() || effectId == null || effectId.isBlank()) {
            return;
        }
        ActiveSelfEffect effect = activationRuntime.createSelfEffect(
                ownerPlayerId, ownerRef, effectId, expireAtMillis, nextApplyAtMillis);
        if (effect != null) {
            selfState.replaceSelfEffect(ownerPlayerId, effectId, effect);
        }
    }

    public void startPlayerAnchor(PlayerData player,
                                  Ref<EntityStore> ownerRef,
                                  Store<EntityStore> store,
                                  long expireAtMillis,
                                  String completionEffectId) {
        if (player == null || player.getPlayerId() == null || ownerRef == null || !ownerRef.isValid() || store == null) {
            return;
        }
        Vector3d anchor = support.position(ownerRef, store);
        if (anchor == null) {
            return;
        }
        ActivePlayerAnchor playerAnchor = activationRuntime.createPlayerAnchor(
                "obsidian_skin",
                player.getPlayerId(),
                ownerRef,
                anchor,
                expireAtMillis,
                completionEffectId
        );
        if (playerAnchor == null) {
            return;
        }
        selfState.replacePlayerAnchor(player.getPlayerId(), playerAnchor);
        support.logInfo("[MOTM] Player anchor started: reason=obsidian_skin player=" + player.getPlayerName()
                + " anchor=" + support.formatVector(anchor));
    }

    private boolean processPlayerAnchor(ActivePlayerAnchor anchor,
                                        Store<EntityStore> currentStore,
                                        long now) {
        if (anchor == null || anchor.ownerRef() == null || !anchor.ownerRef().isValid()) {
            return true;
        }
        if (anchor.expired(now)) {
            setAnchorMovementFreeze(anchor.ownerRef(), currentStore, false);
            zeroVelocity(anchor.ownerRef(), currentStore);
            boolean applied = support.applyEffectById(anchor.ownerRef(), currentStore, anchor.completionEffectId());
            startActiveSelfEffect(anchor.ownerRef(), anchor.ownerPlayerId(),
                    anchor.completionEffectId(), now + ANCHOR_COMPLETION_EFFECT_DURATION_MS);
            support.logInfo("[MOTM] Player anchor released: reason=" + anchor.reason()
                    + " playerId=" + anchor.ownerPlayerId()
                    + " completionEffect=" + anchor.completionEffectId()
                    + " applied=" + applied);
            return true;
        }

        setAnchorMovementFreeze(anchor.ownerRef(), currentStore, true);
        zeroVelocity(anchor.ownerRef(), currentStore);
        return false;
    }

    private boolean processSelfEffect(ActiveSelfEffect effect,
                                      Store<EntityStore> currentStore,
                                      long now) {
        if (effect == null || effect.ownerRef() == null || !effect.ownerRef().isValid()) {
            return true;
        }
        if (effect.expired(now)) {
            return true;
        }
        if (!effect.readyToApply(now)) {
            return false;
        }
        boolean applied = support.applyEffectById(effect.ownerRef(), currentStore, effect.effectId());
        effect.scheduleNextApply(now, SELF_EFFECT_REFRESH_INTERVAL_MS);
        if (applied) {
            support.logFine("[MOTM] Active self effect refreshed: playerId=" + effect.ownerPlayerId()
                    + " effect=" + effect.effectId());
        }
        return false;
    }

    private void setAnchorMovementFreeze(Ref<EntityStore> ownerRef,
                                         Store<EntityStore> store,
                                         boolean frozen) {
        if (ownerRef == null || !ownerRef.isValid() || store == null) {
            return;
        }
        try {
            MovementManager movementManager = store.getComponent(ownerRef, MovementManager.getComponentType());
            if (movementManager == null || movementManager.getSettings() == null) {
                return;
            }
            if (frozen) {
                var settings = movementManager.getSettings();
                settings.forwardWalkSpeedMultiplier = 0.0f;
                settings.backwardWalkSpeedMultiplier = 0.0f;
                settings.strafeWalkSpeedMultiplier = 0.0f;
                settings.forwardRunSpeedMultiplier = 0.0f;
                settings.backwardRunSpeedMultiplier = 0.0f;
                settings.strafeRunSpeedMultiplier = 0.0f;
                settings.forwardCrouchSpeedMultiplier = 0.0f;
                settings.backwardCrouchSpeedMultiplier = 0.0f;
                settings.strafeCrouchSpeedMultiplier = 0.0f;
                settings.forwardSprintSpeedMultiplier = 0.0f;
                settings.acceleration = 0.01f;
                settings.maxSpeedMultiplier = 0.01f;
            } else {
                movementManager.applyDefaultSettings();
            }
            PlayerRef universePlayerRef = store.getComponent(ownerRef, PlayerRef.getComponentType());
            if (universePlayerRef != null && universePlayerRef.getPacketHandler() != null) {
                movementManager.update(universePlayerRef.getPacketHandler());
            }
        } catch (Exception e) {
            support.logWarning("[MOTM] Player anchor movement freeze update failed: " + e.getMessage());
        }
    }

    private void zeroVelocity(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        if (entityRef == null || !entityRef.isValid() || store == null) {
            return;
        }
        Velocity velocity = store.getComponent(entityRef, Velocity.getComponentType());
        if (velocity != null) {
            velocity.set(0.0, 0.0, 0.0);
        }
    }

    private static boolean belongsToCurrentStore(Ref<EntityStore> ref, Store<EntityStore> currentStore) {
        return ref != null && ref.isValid() && ref.getStore() == currentStore;
    }

    public interface Support {
        boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store);

        String formatVector(Vector3d vector);

        void logInfo(String message);

        void logFine(String message);

        void logWarning(String message);
    }
}
