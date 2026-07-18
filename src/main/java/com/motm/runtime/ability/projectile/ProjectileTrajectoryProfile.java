package com.motm.runtime.ability.projectile;

/**
 * Data-only launch/aim profile for projectile abilities that need spatial
 * offsets beyond the generic caster-center launch.
 */
public record ProjectileTrajectoryProfile(
        double originVerticalOffset,
        double originForwardOffset,
        double explicitTargetVerticalOffset,
        boolean preferLookDirectionWhenUntargeted,
        double ownerSelfClearanceDistance
) {

    public static ProjectileTrajectoryProfile generic() {
        return new ProjectileTrajectoryProfile(0.0, 0.0, 0.0, false, 0.0);
    }
    public static ProjectileTrajectoryProfile nativeProjectile() {
        // Eye-height launch with forward clearance so the projectile visibly
        // leaves the player model (magma template feel) while aim stays on the
        // head look vector.
        return new ProjectileTrajectoryProfile(1.5, 0.9, 0.0, true, 0.75);
    }

    public static ProjectileTrajectoryProfile magmaSling() {
        return new ProjectileTrajectoryProfile(1.15, 0.9, 1.0, true, 0.75);
    }

    public boolean offsetsOrigin() {
        return originVerticalOffset != 0.0 || originForwardOffset != 0.0;
    }

    public boolean offsetsExplicitTarget() {
        return explicitTargetVerticalOffset != 0.0;
    }
}
