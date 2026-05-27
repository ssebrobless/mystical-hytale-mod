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

        long expireAt = System.currentTimeMillis() + Math.max(1200L, durationMillis);
        String cracks = placementAdapter.placeSurfacePatchSelection(world, "sinkhole_cracks", field.center(), 2, expireAt,
                "Rock_Stone_Brick_Pillar_Middle", "Rock_Stone_Brick");
        String dust = placementAdapter.placeRingBlockSelection(world, "sinkhole_dust_ring", field.center(), 3.0, expireAt,
                "Soil_Dirt", "Soil_Grass");
        if (!cracks.isBlank() || !dust.isBlank()) {
            support.logInfo("[MOTM] Sinkhole surface marker placed: cracks=" + !cracks.isBlank()
                    + " dustRing=" + !dust.isBlank()
                    + " center=" + field.center());
        }
    }

    public interface Support {
        void logInfo(String message);
    }
}
