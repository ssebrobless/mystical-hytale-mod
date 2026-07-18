package com.motm.runtime.ability.projectile;

/** Deterministic five-shot wind-blade volley schedule. */
public final class RazorWindState {
    public static final int SHOT_COUNT = 5;
    private static final long SHOT_INTERVAL_MILLIS = 45L;

    public int shotCount() {
        return SHOT_COUNT;
    }

    public long launchDelayMillis(int shotIndex) {
        if (shotIndex < 0 || shotIndex >= SHOT_COUNT) {
            throw new IndexOutOfBoundsException(shotIndex);
        }
        return shotIndex * SHOT_INTERVAL_MILLIS;
    }

    public boolean isComplete(int launchedShots) {
        return launchedShots >= SHOT_COUNT;
    }
}
