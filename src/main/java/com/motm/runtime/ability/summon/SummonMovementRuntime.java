package com.motm.runtime.ability.summon;

import org.joml.Vector3d;

public final class SummonMovementRuntime {
    private static final double OWNER_FOLLOW_DISTANCE = 4.5;
    private static final double OWNER_FOLLOW_OFFSET = 2.0;
    private static final double MIN_APPROACH_TRAVEL = 0.4;
    private static final double MAX_APPROACH_TRAVEL = 4.0;
    private static final double MIN_RETREAT_TRAVEL = 0.5;
    private static final double MAX_RETREAT_TRAVEL = 3.4;
    private static final double BESIDE_TARGET_OFFSET = 1.15;

    public Vector3d ownerFollowDestination(Vector3d summonPosition, Vector3d ownerPosition) {
        if (summonPosition == null || ownerPosition == null || distance(summonPosition, ownerPosition) <= OWNER_FOLLOW_DISTANCE) {
            return null;
        }
        Vector3d direction = normalize(subtract(ownerPosition, summonPosition));
        return com.motm.util.MotmVectors.addScaled(ownerPosition, direction, -OWNER_FOLLOW_OFFSET);
    }

    public Vector3d targetApproachDestination(Vector3d summonPosition, Vector3d targetPosition, double desiredRange) {
        if (summonPosition == null || targetPosition == null) {
            return null;
        }
        Vector3d direction = normalize(subtract(targetPosition, summonPosition));
        double currentDistance = distance(summonPosition, targetPosition);
        double travel = Math.max(MIN_APPROACH_TRAVEL, Math.min(MAX_APPROACH_TRAVEL, currentDistance - desiredRange));
        return com.motm.util.MotmVectors.addScaled(summonPosition, direction, travel);
    }

    public Vector3d targetRetreatDestination(Vector3d summonPosition, Vector3d targetPosition, double desiredDistance) {
        if (summonPosition == null || targetPosition == null) {
            return null;
        }
        Vector3d direction = normalize(subtract(summonPosition, targetPosition));
        double currentDistance = distance(summonPosition, targetPosition);
        double retreat = Math.max(MIN_RETREAT_TRAVEL, Math.min(MAX_RETREAT_TRAVEL, desiredDistance - currentDistance));
        return com.motm.util.MotmVectors.addScaled(summonPosition, direction, retreat);
    }

    public Vector3d besideTargetDestination(Vector3d targetPosition, Vector3d ownerPosition) {
        if (targetPosition == null) {
            return null;
        }
        Vector3d approach = ownerPosition != null
                ? normalize(subtract(targetPosition, ownerPosition))
                : new Vector3d(0.0, 0.0, 1.0);
        return com.motm.util.MotmVectors.addScaled(targetPosition, approach, -BESIDE_TARGET_OFFSET);
    }

    private static Vector3d subtract(Vector3d left, Vector3d right) {
        return new Vector3d(left.x - right.x, left.y - right.y, left.z - right.z);
    }

    private static Vector3d normalize(Vector3d vector) {
        if (vector == null) {
            return new Vector3d();
        }
        Vector3d normalized = new Vector3d(vector);
        normalized.normalize();
        return normalized;
    }

    private static double distance(Vector3d left, Vector3d right) {
        double dx = left.x - right.x;
        double dy = left.y - right.y;
        double dz = left.z - right.z;
        return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    }
}
