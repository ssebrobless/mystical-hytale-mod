package com.motm.runtime.ability.field;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record BuriedVictim(Ref<EntityStore> targetRef,
                           Float originalScale,
                           long expireAtMillis) {
    public boolean expired(long now) {
        return now >= expireAtMillis;
    }
}
