package com.motm.runtime.ability.followup;

public final class WeaponFollowUpSplashRuntime {

    public static final int DEFAULT_MAX_SPLASH_TARGETS = 4;

    private WeaponFollowUpSplashRuntime() {
    }

    public static <T> void applySplash(ActiveWeaponFollowUp followUp,
                                       double resolvedDamage,
                                       SplashHooks<T> hooks) {
        if (!WeaponFollowUpHitMath.hasSplash(followUp, resolvedDamage) || hooks == null) {
            return;
        }

        Iterable<T> targets = hooks.collectTargets(followUp.splashRadius(), DEFAULT_MAX_SPLASH_TARGETS);
        if (targets == null) {
            return;
        }

        for (T target : targets) {
            if (target == null || !hooks.isValidTarget(target) || hooks.isPrimaryTarget(target)) {
                continue;
            }

            String entityId = hooks.entityId(target);
            double splashDamage = WeaponFollowUpHitMath.splashDamage(resolvedDamage, followUp);
            if (entityId != null) {
                splashDamage = WeaponFollowUpHitMath.applyIncomingMultiplier(
                        splashDamage,
                        hooks.incomingMultiplier(entityId)
                );
                splashDamage = hooks.absorbDamage(entityId, splashDamage);
            }

            if (splashDamage <= 0.0) {
                continue;
            }

            hooks.applyDamage(target, splashDamage);
            hooks.applyPostDamage(target, entityId, splashDamage);
            if (followUp.secondaryRiderToken() != null) {
                hooks.applySecondaryRiderToken(target, followUp.secondaryRiderToken());
            }
            hooks.applyImpact(target);
        }
    }

    public interface SplashHooks<T> {
        Iterable<T> collectTargets(double radius, int maxTargets);

        boolean isValidTarget(T target);

        boolean isPrimaryTarget(T target);

        String entityId(T target);

        double incomingMultiplier(String entityId);

        double absorbDamage(String entityId, double damage);

        void applyDamage(T target, double damage);

        void applyPostDamage(T target, String entityId, double damage);

        void applySecondaryRiderToken(T target, String token);

        void applyImpact(T target);
    }
}
