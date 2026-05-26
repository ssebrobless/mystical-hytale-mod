package com.motm.manager;

import org.joml.Vector3d;
import org.joml.Vector3i;

final class MotmPlaybackGeometry {

    private MotmPlaybackGeometry() {
    }

    static Vector3i blockAnchor(Vector3d center) {
        return new Vector3i(
                (int) Math.floor(center.x),
                (int) Math.floor(center.y),
                (int) Math.floor(center.z)
        );
    }

    static Vector3i fluidGroundAnchor(Vector3d center) {
        return new Vector3i(
                (int) Math.floor(center.x),
                (int) Math.floor(center.y) - 1,
                (int) Math.floor(center.z)
        );
    }

    static Vector3i surfaceDecorationAnchor(Vector3d center) {
        return blockAnchor(center);
    }

    static boolean sameBlock(Vector3i first, Vector3i second) {
        return first != null
                && second != null
                && first.x == second.x
                && first.y == second.y
                && first.z == second.z;
    }

    static Vector3d normalizeHorizontal(Vector3d vector) {
        Vector3d horizontal = vector == null ? new Vector3d(0.0, 0.0, -1.0) : new Vector3d(vector.x, 0.0, vector.z);
        if (!horizontal.isFinite() || horizontal.length() < 0.001) {
            return new Vector3d(0.0, 0.0, -1.0);
        }
        horizontal.normalize();
        return horizontal;
    }

    static Vector3i horizontalRightStep(Vector3d direction) {
        Vector3d right = new Vector3d(-direction.z, 0.0, direction.x);
        if (Math.abs(right.x) >= Math.abs(right.z)) {
            return new Vector3i(right.x >= 0.0 ? 1 : -1, 0, 0);
        }
        return new Vector3i(0, 0, right.z >= 0.0 ? 1 : -1);
    }

    static Vector3i horizontalStep(Vector3d direction) {
        Vector3d step = direction != null ? new Vector3d(direction) : new Vector3d(1.0, 0.0, 0.0);
        step.y = 0.0;
        if (!step.isFinite() || step.length() < 0.001) {
            step = new Vector3d(1.0, 0.0, 0.0);
        } else {
            step.normalize();
        }
        if (Math.abs(step.x) >= Math.abs(step.z)) {
            return new Vector3i(step.x >= 0.0 ? 1 : -1, 0, 0);
        }
        return new Vector3i(0, 0, step.z >= 0.0 ? 1 : -1);
    }

    static Vector3d subtract(Vector3d left, Vector3d right) {
        return new Vector3d(left.x - right.x, left.y - right.y, left.z - right.z);
    }

    static Vector3d normalize(Vector3d vector) {
        Vector3d normalized = new Vector3d(vector);
        if (normalized.length() < 0.0001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        normalized.normalize();
        return normalized;
    }

    static double dot(Vector3d left, Vector3d right) {
        return (left.x * right.x) + (left.y * right.y) + (left.z * right.z);
    }

    static double length(Vector3d value) {
        return Math.sqrt((value.x * value.x) + (value.y * value.y) + (value.z * value.z));
    }

    static double distance(Vector3d left, Vector3d right) {
        return length(subtract(left, right));
    }
}
