package com.motm.runtime.ability.self;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record ActivePlayerAnchor(String reason,
                                 String ownerPlayerId,
                                 Ref<EntityStore> ownerRef,
                                 Vector3d anchor,
                                 long expireAtMillis,
                                 String completionEffectId) {
    public ActivePlayerAnchor {
        anchor = anchor == null ? null : anchor.clone();
    }

    public boolean expired(long now) {
        return now >= expireAtMillis;
    }
}
