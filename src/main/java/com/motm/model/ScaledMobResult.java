package com.motm.model;

/**
 * Result of resolving MOTM scaling for a live mob spawn.
 */
public record ScaledMobResult(
        MobStats stats,
        int level,
        String displayName,
        String levelColor
) {
}
