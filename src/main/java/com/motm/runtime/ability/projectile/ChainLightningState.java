package com.motm.runtime.ability.projectile;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.util.MotmEntityLiveness;
import org.joml.Vector3d;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Per-cast chain-hop state: nearest live target, bounded to six hops and 3 blocks. */
public final class ChainLightningState {
    public static final int MAX_HOPS = 6;
    public static final double HOP_RADIUS = 3.0;
    private final Set<Ref<EntityStore>> visited = new HashSet<>();

    public boolean visit(Ref<EntityStore> target, Store<EntityStore> store) {
        if (!MotmEntityLiveness.isLiveTarget(target, store) || visited.size() >= MAX_HOPS) {
            return false;
        }
        return visited.add(target);
    }

    public boolean complete() {
        return visited.size() >= MAX_HOPS;
    }

    public int hopCount() {
        return visited.size();
    }

    public Ref<EntityStore> nearestNext(ProjectileHitHytaleAdapter hitAdapter,
                                        Store<EntityStore> store,
                                        Vector3d center) {
        if (hitAdapter == null || store == null || center == null || complete()) {
            return null;
        }
        List<Ref<EntityStore>> candidates = hitAdapter.collectNearbyTargets(store, center, HOP_RADIUS, MAX_HOPS + 1);
        for (Ref<EntityStore> candidate : candidates) {
            if (MotmEntityLiveness.isLiveTarget(candidate, store) && !visited.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
