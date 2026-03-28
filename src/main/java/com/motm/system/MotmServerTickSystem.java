package com.motm.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.MenteesMod;

/**
 * Bridges the Hytale ECS tick loop into the mod's plain-Java runtime systems.
 */
public class MotmServerTickSystem extends TickingSystem<EntityStore> {

    private final MenteesMod mod;
    private long lastProcessedTick = Long.MIN_VALUE;

    public MotmServerTickSystem(MenteesMod mod) {
        this.mod = mod;
    }

    @Override
    public void tick(float delta, int tick, Store<EntityStore> store) {
        long worldTick = store.getExternalData().getWorld().getTick();

        synchronized (this) {
            if (worldTick <= lastProcessedTick) {
                return;
            }
            lastProcessedTick = worldTick;
        }

        mod.onServerTick(store);
    }
}
