package com.motm.runtime.ability.field;

import org.joml.Vector3d;

public record RecentFieldOrigin(Vector3d position, long recordedAtMillis) {
    public RecentFieldOrigin {
        position = position == null ? null : new Vector3d(position);
    }

    public boolean withinWindow(long now, long windowMillis) {
        return now - recordedAtMillis <= windowMillis;
    }
}
