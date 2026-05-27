package com.motm.runtime.ability.transformation;

import com.hypixel.hytale.math.vector.Vector3d;

public final class TransformationEffectRuntime {

    public <T> void applyPulse(ActiveTransformation form, Vector3d origin, Hooks<T> hooks) {
        if (form == null || origin == null || hooks == null) {
            return;
        }

        switch (form.kind()) {
            case SMOKE -> {
                T target = hooks.findNearest(origin, 3.4);
                if (target != null) {
                    hooks.applyImpact(target, 0.30, "blind", false);
                }
            }
            case PTERODACTYL -> {
                for (T target : hooks.collectNearby(origin, 5.5, 2)) {
                    hooks.applyImpact(target, 0.34, "slow", false);
                }
            }
            case TRICERATOPS -> {
                for (T target : hooks.collectNearby(origin, 3.6, 3)) {
                    hooks.applyImpact(target, 0.46, null, true);
                }
            }
            case T_REX -> {
                double radius = Math.max(3.8, form.sourceAbility().getRadius() > 0 ? form.sourceAbility().getRadius() : 4.0);
                for (T target : hooks.collectNearby(origin, radius, 4)) {
                    hooks.applyImpact(target, 0.58, "vulnerability", false);
                }
            }
            default -> {
            }
        }
    }

    public <T> void applyLocomotionPressure(ActiveTransformation form, Vector3d origin, Hooks<T> hooks) {
        if (form == null || origin == null || hooks == null) {
            return;
        }

        Vector3d previous = form.lastOwnerPosition();
        form.updateLastOwnerPosition(origin);
        if (previous == null) {
            return;
        }

        double movedDistance = distance(previous, origin);
        if (movedDistance < form.locomotionTriggerDistance()) {
            return;
        }

        double movementFactor = clamp(
                movedDistance / Math.max(0.75, form.locomotionTriggerDistance()),
                1.0,
                1.75
        );

        switch (form.kind()) {
            case SMOKE -> {
                for (T target : hooks.collectAlong(previous, origin, form.collisionRadius(), 2)) {
                    hooks.applyImpact(target, 0.16 * movementFactor, "blind", false);
                    hooks.applyToken(target, "disoriented");
                }
            }
            case PTERODACTYL -> {
                for (T target : hooks.collectAlong(previous, origin, form.collisionRadius(), 3)) {
                    hooks.applyImpact(target, 0.22 * movementFactor, "slow", false);
                    hooks.applyToken(target, "vulnerability");
                    hooks.applyKnockback(target);
                }
            }
            case TRICERATOPS -> {
                boolean hitAny = false;
                for (T target : hooks.collectAlong(previous, origin, form.collisionRadius(), 4)) {
                    if (hooks.applyChargeImpact(target, 0.36 * movementFactor)) {
                        hitAny = true;
                    }
                }
                if (hitAny) {
                    hooks.applyOwnerShield(2.5);
                }
            }
            case T_REX -> {
                double radius = Math.max(3.8, form.collisionRadius());
                for (T target : hooks.collectNearby(origin, radius, 4)) {
                    hooks.applyImpact(target, 0.34 * movementFactor, "vulnerability", false);
                    hooks.applyToken(target, "disoriented");
                }
            }
            default -> {
            }
        }
    }

    private static double distance(Vector3d left, Vector3d right) {
        double dx = left.getX() - right.getX();
        double dy = left.getY() - right.getY();
        double dz = left.getZ() - right.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    public interface Hooks<T> {
        T findNearest(Vector3d origin, double radius);

        Iterable<T> collectNearby(Vector3d origin, double radius, int maxTargets);

        Iterable<T> collectAlong(Vector3d from, Vector3d to, double radius, int maxTargets);

        void applyImpact(T target, double damageRatio, String token, boolean knockback);

        boolean applyChargeImpact(T target, double damageRatio);

        void applyToken(T target, String token);

        void applyKnockback(T target);

        void applyOwnerShield(double shieldPercent);
    }
}
