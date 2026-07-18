package com.motm.runtime.ability.projectile;

import org.joml.Vector3d;

/** Immutable geometry for the two mirrored native wind blades. */
public record GaleCutterState(Vector3d forward, double halfAngleDegrees) {
    public GaleCutterState {
        forward = forward == null ? new Vector3d(0.0, 0.0, 1.0) : new Vector3d(forward).normalize();
        halfAngleDegrees = Math.max(0.0, halfAngleDegrees);
    }

    public int launchCount() {
        return 2;
    }

    public double rotationOffset(int index) {
        if (index < 0 || index > 1) {
            throw new IndexOutOfBoundsException(index);
        }
        return index == 0 ? -halfAngleDegrees : halfAngleDegrees;
    }
}
