package com.motm.util;

import org.joml.Vector3d;
import org.joml.Vector3i;

public final class MotmVectors {
    private MotmVectors() {
    }

    public static Vector3d copy(Vector3d vector) {
        return vector == null ? null : new Vector3d(vector);
    }

    public static Vector3i copy(Vector3i vector) {
        return vector == null ? null : new Vector3i(vector);
    }

    public static Vector3d addScaled(Vector3d origin, Vector3d direction, double scale) {
        Vector3d result = origin == null ? new Vector3d() : new Vector3d(origin);
        if (direction != null) {
            result.x += direction.x * scale;
            result.y += direction.y * scale;
            result.z += direction.z * scale;
        }
        return result;
    }
}
