package com.motm.runtime.ability.field;

import com.hypixel.hytale.math.vector.Vector3d;

public record RecentFieldOrigin(Vector3d position, long recordedAtMillis) {
    public RecentFieldOrigin {
        position = position == null ? null : position.clone();
    }

    public boolean withinWindow(long now, long windowMillis) {
        return now - recordedAtMillis <= windowMillis;
    }
}
