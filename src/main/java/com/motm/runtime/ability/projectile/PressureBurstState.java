package com.motm.runtime.ability.projectile;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

/** Hold-charge state for pressure_burst; scale belongs to the projectile actor. */
public final class PressureBurstState {
    public static final long MAX_CHARGE_MILLIS = 4_000L;
    private final long startedAtMillis;

    public PressureBurstState(long startedAtMillis) {
        this.startedAtMillis = startedAtMillis;
    }

    public float charge(long nowMillis) {
        return (float) Math.max(0.0, Math.min(1.0,
                (nowMillis - startedAtMillis) / (double) MAX_CHARGE_MILLIS));
    }

    public double speedMultiplier(long nowMillis) {
        return 1.0 + charge(nowMillis);
    }

    public boolean applyProjectileScale(Ref<EntityStore> projectileRef,
                                         Store<EntityStore> store,
                                         long nowMillis) {
        if (projectileRef == null || !projectileRef.isValid() || store == null) {
            return false;
        }
        float scale = 1.0f + charge(nowMillis);
        store.putComponent(projectileRef, EntityScaleComponent.getComponentType(), new EntityScaleComponent(scale));
        return true;
    }
}
