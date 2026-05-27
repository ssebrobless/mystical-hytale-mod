package com.motm.runtime.ability.followup;

public final class WeaponFollowUpPrimaryHitRuntime {

    private WeaponFollowUpPrimaryHitRuntime() {
    }

    public static void applyPrimaryHit(ActiveWeaponFollowUp followUp,
                                       double resolvedDamage,
                                       PrimaryHitHooks hooks) {
        if (hooks == null) {
            return;
        }

        if (resolvedDamage > 0.0) {
            hooks.applyDamage(resolvedDamage);
            hooks.applyPostDamage(resolvedDamage);
            hooks.applyLifesteal(resolvedDamage);
        }

        hooks.applyImpact();
        WeaponFollowUpHitEffects.applyTargetRiders(followUp, new WeaponFollowUpHitEffects.RiderHooks() {
            @Override
            public void applyRiderToken(String token) {
                hooks.applyRiderToken(token);
            }

            @Override
            public void applySecondaryRiderToken(String token) {
                hooks.applySecondaryRiderToken(token);
            }
        });
    }

    public interface PrimaryHitHooks {
        void applyDamage(double damage);

        void applyPostDamage(double damage);

        void applyLifesteal(double damage);

        void applyImpact();

        void applyRiderToken(String token);

        void applySecondaryRiderToken(String token);
    }
}
