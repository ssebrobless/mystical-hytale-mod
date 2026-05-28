package com.motm.runtime.ability.projectile;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public record ProjectileVisualRuntime(Ref<EntityStore> visualRef,
                                      String travelEffectId,
                                      long nextRefreshAtMillis) {
    public static ProjectileVisualRuntime none() {
        return new ProjectileVisualRuntime(null, null, Long.MAX_VALUE);
    }
}
