package com.motm.runtime.ability.summon;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SummonMovementRuntimeTest {
    private final SummonMovementRuntime runtime = new SummonMovementRuntime();

    @Test
    void plansOwnerFollowDestinationBehindOwnerWhenFarAway() {
        Vector3d destination = runtime.ownerFollowDestination(
                new Vector3d(0.0, 0.0, 0.0),
                new Vector3d(10.0, 0.0, 0.0)
        );

        assertVector(destination, 8.0, 0.0, 0.0);
        assertNull(runtime.ownerFollowDestination(new Vector3d(0.0, 0.0, 0.0), new Vector3d(4.5, 0.0, 0.0)));
        assertNull(runtime.ownerFollowDestination(null, new Vector3d(1.0, 0.0, 0.0)));
    }

    @Test
    void plansApproachDestinationWithTravelClampedToAllowedStep() {
        Vector3d maxTravel = runtime.targetApproachDestination(
                new Vector3d(0.0, 0.0, 0.0),
                new Vector3d(10.0, 0.0, 0.0),
                2.0
        );
        Vector3d minTravel = runtime.targetApproachDestination(
                new Vector3d(0.0, 0.0, 0.0),
                new Vector3d(2.1, 0.0, 0.0),
                2.0
        );

        assertVector(maxTravel, 4.0, 0.0, 0.0);
        assertVector(minTravel, 0.4, 0.0, 0.0);
        assertNull(runtime.targetApproachDestination(new Vector3d(0.0, 0.0, 0.0), null, 2.0));
    }

    @Test
    void plansRetreatDestinationAwayFromTargetWithClamp() {
        Vector3d maxRetreat = runtime.targetRetreatDestination(
                new Vector3d(1.0, 0.0, 0.0),
                new Vector3d(0.0, 0.0, 0.0),
                10.0
        );
        Vector3d minRetreat = runtime.targetRetreatDestination(
                new Vector3d(1.0, 0.0, 0.0),
                new Vector3d(0.0, 0.0, 0.0),
                1.1
        );

        assertVector(maxRetreat, 4.4, 0.0, 0.0);
        assertVector(minRetreat, 1.5, 0.0, 0.0);
        assertNull(runtime.targetRetreatDestination(null, new Vector3d(0.0, 0.0, 0.0), 2.0));
    }

    @Test
    void plansCloneBesideTargetUsingOwnerApproachOrFallbackDirection() {
        Vector3d withOwner = runtime.besideTargetDestination(
                new Vector3d(10.0, 0.0, 0.0),
                new Vector3d(0.0, 0.0, 0.0)
        );
        Vector3d fallback = runtime.besideTargetDestination(
                new Vector3d(10.0, 0.0, 10.0),
                null
        );

        assertVector(withOwner, 8.85, 0.0, 0.0);
        assertVector(fallback, 10.0, 0.0, 8.85);
        assertNull(runtime.besideTargetDestination(null, new Vector3d(0.0, 0.0, 0.0)));
    }

    private static void assertVector(Vector3d actual, double x, double y, double z) {
        assertEquals(x, actual.x, 0.0001);
        assertEquals(y, actual.y, 0.0001);
        assertEquals(z, actual.z, 0.0001);
    }
}
