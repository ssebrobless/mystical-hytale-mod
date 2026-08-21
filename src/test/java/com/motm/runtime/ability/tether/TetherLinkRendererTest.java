package com.motm.runtime.ability.tether;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TetherLinkRendererTest {

    @Test
    void sampleChainIncludesBothAnchorsAtBodyHeightAndInterpolatesEvenly() {
        Vector3d from = new Vector3d(0, 64, 0);
        Vector3d to = new Vector3d(10, 64, 0);
        List<Vector3d> beads = TetherLinkRenderer.sampleChain(from, to, 6, 0.9);
        assertEquals(6, beads.size());
        // First/last bead are the caster/target anchors, lifted to body height.
        assertEquals(0.0, beads.get(0).x, 1e-9);
        assertEquals(64.9, beads.get(0).y, 1e-9);
        assertEquals(10.0, beads.get(5).x, 1e-9);
        assertEquals(64.9, beads.get(5).y, 1e-9);
        // Even spacing along the segment: x = 2 * i for i in 0..5.
        for (int i = 0; i < beads.size(); i++) {
            assertEquals(2.0 * i, beads.get(i).x, 1e-9);
        }
    }

    @Test
    void sampleChainClampsToMinimumTwoBeads() {
        List<Vector3d> beads = TetherLinkRenderer.sampleChain(new Vector3d(0, 0, 0), new Vector3d(1, 0, 0), 1, 0.0);
        assertEquals(2, beads.size());
    }

    @Test
    void sampleChainIsNullSafe() {
        assertTrue(TetherLinkRenderer.sampleChain(null, new Vector3d(1, 0, 0)).isEmpty());
        assertTrue(TetherLinkRenderer.sampleChain(new Vector3d(0, 0, 0), null).isEmpty());
    }

    @Test
    void beadSystemMapsClassToProvenCappedSystem() {
        // These four ids are the dash trail set (live-verified TotalParticles-capped, 2026-07-18).
        assertEquals("Bubbles_Breathing", TetherLinkRenderer.beadSystemId("hydro"));
        assertEquals("Block_Break_Dust", TetherLinkRenderer.beadSystemId("aero"));
        assertEquals("VoidImpact", TetherLinkRenderer.beadSystemId("corruptus"));
        assertEquals("Block_Break_Stone", TetherLinkRenderer.beadSystemId("terra"));
        assertEquals("Block_Break_Stone", TetherLinkRenderer.beadSystemId(null));
        assertEquals("Block_Break_Stone", TetherLinkRenderer.beadSystemId("UNKNOWN"));
    }
}
