package com.motm.runtime.ability.projectile;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ProjectileLifecycleHytaleAdapter {
    private final ProjectileRuntimeState projectileState;
    private final ProjectileTickHytaleAdapter tickAdapter;
    private final ProjectileVisualHytaleAdapter visualAdapter;
    private final TraceScope traceScope;

    public ProjectileLifecycleHytaleAdapter(ProjectileRuntimeState projectileState,
                                            ProjectileTickHytaleAdapter tickAdapter,
                                            ProjectileVisualHytaleAdapter visualAdapter,
                                            TraceScope traceScope) {
        this.projectileState = projectileState;
        this.tickAdapter = tickAdapter;
        this.visualAdapter = visualAdapter;
        this.traceScope = traceScope;
    }

    public void processForStore(Store<EntityStore> currentStore, long now) {
        if (projectileState == null || tickAdapter == null) {
            return;
        }
        projectileState.removeProcessedProjectiles(projectile ->
                belongsToCurrentStore(projectile.ownerRef(), currentStore) && processWithTrace(projectile, now));
    }

    public int removeForPlayer(String playerId) {
        if (projectileState == null) {
            return 0;
        }
        return projectileState.removeProjectilesForPlayer(playerId,
                visualAdapter == null ? null : visualAdapter::despawn);
    }

    public int activeProjectileCount() {
        return projectileState == null ? 0 : projectileState.activeProjectileCount();
    }

    private boolean processWithTrace(ActiveProjectile projectile, long now) {
        if (traceScope == null) {
            return tickAdapter.process(projectile, now);
        }
        String previousTraceId = traceScope.enter(projectile.traceId());
        try {
            return tickAdapter.process(projectile, now);
        } finally {
            traceScope.restore(previousTraceId);
        }
    }

    private static boolean belongsToCurrentStore(Ref<EntityStore> ownerRef, Store<EntityStore> currentStore) {
        if (ownerRef == null || !ownerRef.isValid()) {
            return true;
        }

        try {
            return ownerRef.getStore() == currentStore;
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    public interface TraceScope {
        String enter(String traceId);

        void restore(String previousTraceId);
    }
}
