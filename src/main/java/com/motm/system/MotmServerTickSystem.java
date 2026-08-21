package com.motm.system;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.MenteesMod;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Bridges the Hytale ECS tick loop into the mod's plain-Java runtime systems.
 */
public class MotmServerTickSystem extends TickingSystem<EntityStore> {

    private final MenteesMod mod;
    private final Map<Store<EntityStore>, Long> lastProcessedTicks =
            Collections.synchronizedMap(new WeakHashMap<>());

    public MotmServerTickSystem(MenteesMod mod) {
        this.mod = mod;
    }

    @Override
    public void tick(float delta, int tick, Store<EntityStore> store) {
        long worldTick = store.getExternalData().getWorld().getTick();

        synchronized (this) {
            long lastProcessedTick = lastProcessedTicks.getOrDefault(store, Long.MIN_VALUE);
            if (worldTick <= lastProcessedTick) {
                return;
            }
            lastProcessedTicks.put(store, worldTick);
        }

        mod.onServerTick(store, tick);
    }
}
