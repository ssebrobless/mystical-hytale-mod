package com.motm.runtime.ability.channel;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;

public final class ChannelActivationRuntime {

    public ActiveChannel createChannel(String ownerPlayerId,
                                       Ref<EntityStore> ownerRef,
                                       Ref<EntityStore> targetRef,
                                       AbilityData ability,
                                       long expireAtMillis,
                                       long nextPulseAtMillis) {
        if (ownerPlayerId == null || ownerRef == null || !ownerRef.isValid()
                || targetRef == null || !targetRef.isValid() || ability == null) {
            return null;
        }
        return new ActiveChannel(ownerPlayerId, ownerRef, targetRef, ability, expireAtMillis, nextPulseAtMillis);
    }

    public ActiveLineControl createLineControl(String ownerPlayerId,
                                               Ref<EntityStore> ownerRef,
                                               Ref<EntityStore> targetRef,
                                               AbilityData ability,
                                               long expireAtMillis,
                                               long nextPulseAtMillis) {
        if (ownerPlayerId == null || ownerRef == null || !ownerRef.isValid()
                || targetRef == null || !targetRef.isValid() || ability == null) {
            return null;
        }
        return new ActiveLineControl(ownerPlayerId, ownerRef, targetRef, ability, expireAtMillis, nextPulseAtMillis);
    }
}
