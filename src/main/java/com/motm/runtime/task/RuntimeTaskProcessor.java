package com.motm.runtime.task;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public interface RuntimeTaskProcessor {

    String id();

    void process(Store<EntityStore> currentStore);
}
