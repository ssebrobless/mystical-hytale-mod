package com.motm.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

/** Shared guard for deferred MOTM target updates. */
public final class MotmEntityLiveness {
    private MotmEntityLiveness() {
    }

    public static boolean isLiveTarget(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null || ref.getStore() != store) {
            return false;
        }

        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc != null && (npc.wasRemoved() || npc.isDespawning())) {
            return false;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null && player.wasRemoved()) {
            return false;
        }

        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            return npc != null || player != null;
        }
        EntityStatValue health = statMap.get(DefaultEntityStatTypes.getHealth());
        return health == null || health.get() > 0.0f;
    }
}
