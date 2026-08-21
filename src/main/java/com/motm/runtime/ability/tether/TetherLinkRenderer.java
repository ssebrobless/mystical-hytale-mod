package com.motm.runtime.ability.tether;

import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared visual-link (tether) contract for the pull / root / drain tether family
 * (vines, riptide, rip_current, anchor_haul, life_drain, ...).
 *
 * <p>The link is a thin "beaded" particle chain sampled between the caster and
 * the target and re-emitted every tether tick, so it follows both endpoints as
 * they move (synced movement) and disappears on its own once emission stops
 * (cleanup). Every bead uses a burst-only (TotalParticles-capped) particle
 * system, so there is never a permanent-emitter leak (crash-saga engine rule,
 * live-verified 2026-07-18: uncapped world spawns emit FOREVER).
 */
public final class TetherLinkRenderer {

    public static final int DEFAULT_BEAD_COUNT = 6;
    public static final double DEFAULT_ANCHOR_OFFSET_Y = 0.9;

    // Class -> proven burst-only (capped) bead system, mirroring the dash trail
    // set (live-verified 2026-07-18). Canon per-ability skins (Plant_Vine +
    // Nature_Buff for vines, Water_Beam for riptide/rip_current, etc.) are a
    // follow-up gated on verifying each system's TotalParticles cap.
    private static final String TERRA_BEAD = "Block_Break_Stone";
    private static final String HYDRO_BEAD = "Bubbles_Breathing";
    private static final String AERO_BEAD = "Block_Break_Dust";
    private static final String CORRUPTUS_BEAD = "VoidImpact";

    private TetherLinkRenderer() {
    }

    public static String beadSystemId(String classId) {
        return switch (classId == null ? "" : classId.toLowerCase(Locale.ROOT)) {
            case "hydro" -> HYDRO_BEAD;
            case "aero" -> AERO_BEAD;
            case "corruptus" -> CORRUPTUS_BEAD;
            default -> TERRA_BEAD;
        };
    }

    public static List<Vector3d> sampleChain(Vector3d from, Vector3d to) {
        return sampleChain(from, to, DEFAULT_BEAD_COUNT, DEFAULT_ANCHOR_OFFSET_Y);
    }

    /**
     * Interpolates {@code beadCount} points (inclusive of both endpoints) between
     * the anchor-lifted caster and target positions, producing the bead chain.
     * The anchor offset lifts both endpoints to body height so the link reads as
     * a torso-to-torso line rather than a feet-to-feet one.
     */
    public static List<Vector3d> sampleChain(Vector3d from, Vector3d to, int beadCount, double anchorOffsetY) {
        List<Vector3d> points = new ArrayList<>();
        if (from == null || to == null) {
            return points;
        }
        int beads = Math.max(2, beadCount);
        double startX = from.x;
        double startY = from.y + anchorOffsetY;
        double startZ = from.z;
        double endX = to.x;
        double endY = to.y + anchorOffsetY;
        double endZ = to.z;
        for (int i = 0; i < beads; i++) {
            double t = (double) i / (double) (beads - 1);
            points.add(new Vector3d(
                    startX + (endX - startX) * t,
                    startY + (endY - startY) * t,
                    startZ + (endZ - startZ) * t));
        }
        return points;
    }
}
