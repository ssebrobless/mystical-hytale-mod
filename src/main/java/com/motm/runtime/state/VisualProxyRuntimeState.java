package com.motm.runtime.state;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class VisualProxyRuntimeState {
    private final Set<Ref<EntityStore>> visualProxyRefs = ConcurrentHashMap.newKeySet();

    public void add(Ref<EntityStore> ref) {
        if (ref != null) {
            visualProxyRefs.add(ref);
        }
    }

    public boolean remove(Ref<EntityStore> ref) {
        return ref != null && visualProxyRefs.remove(ref);
    }

    public boolean contains(Ref<EntityStore> ref) {
        return ref != null && visualProxyRefs.contains(ref);
    }

    public boolean isEmpty() {
        pruneInvalidRefs();
        return visualProxyRefs.isEmpty();
    }

    public int size() {
        pruneInvalidRefs();
        return visualProxyRefs.size();
    }

    public List<Ref<EntityStore>> snapshot() {
        pruneInvalidRefs();
        return List.copyOf(visualProxyRefs);
    }

    public boolean despawn(Ref<EntityStore> ref) {
        if (ref == null) {
            return false;
        }
        try {
            Store<EntityStore> store = isValidRef(ref) ? ref.getStore() : null;
            NPCEntity npc = store != null ? store.getComponent(ref, NPCEntity.getComponentType()) : null;
            if (npc != null && !npc.isDespawning()) {
                npc.setToDespawn();
            }
        } catch (RuntimeException ignored) {
            // Invalid refs can surface while stores are shutting down; untrack them either way.
        }
        return remove(ref);
    }

    public void despawnAll(List<Ref<EntityStore>> refs) {
        if (refs == null || refs.isEmpty()) {
            return;
        }
        for (Ref<EntityStore> ref : refs) {
            despawn(ref);
        }
    }

    public int despawnForStore(Store<EntityStore> store) {
        if (store == null) {
            return 0;
        }
        int removed = 0;
        for (Ref<EntityStore> ref : snapshot()) {
            if (ref == null || !isValidRef(ref)) {
                if (remove(ref)) {
                    removed++;
                }
                continue;
            }
            if (ref.getStore() != store) {
                continue;
            }
            if (despawn(ref)) {
                removed++;
            }
        }
        return removed;
    }

    private void pruneInvalidRefs() {
        for (Ref<EntityStore> ref : visualProxyRefs) {
            if (ref == null || !isValidRef(ref)) {
                remove(ref);
            }
        }
    }

    private static boolean isValidRef(Ref<EntityStore> ref) {
        try {
            return ref != null && ref.isValid();
        } catch (RuntimeException ignored) {
            return false;
        }
    }
}
