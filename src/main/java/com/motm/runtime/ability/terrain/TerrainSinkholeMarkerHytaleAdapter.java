package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.runtime.ability.field.ActiveField;

public final class TerrainSinkholeMarkerHytaleAdapter {
    private final TerrainPlacementHytaleAdapter placementAdapter;
    private final Support support;

    public TerrainSinkholeMarkerHytaleAdapter(TerrainPlacementHytaleAdapter placementAdapter, Support support) {
        this.placementAdapter = placementAdapter;
        this.support = support;
    }

    public void placeSinkholeSurfaceMarker(ActiveField field, long durationMillis) {
        if (field == null || field.ownerRef() == null || !field.ownerRef().isValid() || placementAdapter == null) {
            return;
        }
        Store<EntityStore> store = field.ownerRef().getStore();
        World world = store != null && store.getExternalData() != null
                ? store.getExternalData().getWorld()
                : null;
        if (world == null) {
            return;
        }

        support.logInfo("[MOTM] Sinkhole surface marker uses particle/effect cue only: center="
                + field.center()
                + " durationMillis=" + Math.max(1200L, durationMillis));
    }

    public interface Support {
        void logInfo(String message);
    }
}
