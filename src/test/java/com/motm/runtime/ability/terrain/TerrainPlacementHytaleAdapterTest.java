package com.motm.runtime.ability.terrain;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TerrainPlacementHytaleAdapterTest {

    @Test
    void surfaceOverlayAnchorStaysGroundedForTemporaryAbilityMarkers() {
        TerrainPlacementHytaleAdapter adapter = new TerrainPlacementHytaleAdapter(null, null, null);

        assertEquals(adapter.surfaceDecorationAnchor(new Vector3d(10.6, 64.0, -3.2)),
                adapter.surfaceOverlayAnchor(new Vector3d(10.6, 64.0, -3.2)));
    }
}
