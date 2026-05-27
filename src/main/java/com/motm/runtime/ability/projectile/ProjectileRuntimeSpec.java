package com.motm.runtime.ability.projectile;

import java.util.List;

/**
 * Pure runtime profile for projectile launch behavior.
 *
 * GameplayPlaybackManager still executes against Hytale refs while migration is
 * in progress; projectile-specific branching should live here instead of in the
 * generic playback flow.
 */
public record ProjectileRuntimeSpec(
        int projectileCount,
        double speedPerTick,
        double maxDistance,
        double impactRadius,
        double collisionRadius,
        double spreadDegrees,
        long lifetimeMillis,
        List<Long> launchDelaysMillis,
        ProjectileTrajectoryProfile trajectoryProfile,
        boolean hideVisualProxyIdentityComponents
) {

    public long launchDelayMillis(int index) {
        if (index < 0 || index >= launchDelaysMillis.size()) {
            return 0L;
        }
        return launchDelaysMillis.get(index);
    }
}
