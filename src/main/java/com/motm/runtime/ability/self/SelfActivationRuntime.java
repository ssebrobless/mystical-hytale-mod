package com.motm.runtime.ability.self;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class SelfActivationRuntime {

    public ActiveSelfEffect createSelfEffect(String ownerPlayerId,
                                             Ref<EntityStore> ownerRef,
                                             String effectId,
                                             long expireAtMillis,
                                             long nextApplyAtMillis) {
        if (ownerPlayerId == null || ownerRef == null || !ownerRef.isValid()
                || effectId == null || effectId.isBlank()) {
            return null;
        }
        return new ActiveSelfEffect(ownerPlayerId, ownerRef, effectId, expireAtMillis, nextApplyAtMillis);
    }

    public ActivePlayerAnchor createPlayerAnchor(String reason,
                                                 String ownerPlayerId,
                                                 Ref<EntityStore> ownerRef,
                                                 Vector3d anchor,
                                                 long expireAtMillis,
                                                 String completionEffectId) {
        if (reason == null || reason.isBlank()
                || ownerPlayerId == null || ownerRef == null || !ownerRef.isValid()
                || anchor == null) {
            return null;
        }
        return new ActivePlayerAnchor(reason, ownerPlayerId, ownerRef, anchor, expireAtMillis, completionEffectId);
    }
}
