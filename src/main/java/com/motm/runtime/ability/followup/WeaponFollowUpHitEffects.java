package com.motm.runtime.ability.followup;

public final class WeaponFollowUpHitEffects {

    private WeaponFollowUpHitEffects() {
    }

    public static void applyTargetRiders(ActiveWeaponFollowUp followUp, RiderHooks hooks) {
        if (followUp == null || hooks == null) {
            return;
        }
        if (followUp.riderToken() != null) {
            hooks.applyRiderToken(followUp.riderToken());
        }
        if (followUp.secondaryRiderToken() != null) {
            hooks.applySecondaryRiderToken(followUp.secondaryRiderToken());
        }
    }

    public static void applyNativeAlloyRiders(ActiveWeaponFollowUp followUp, RiderHooks hooks) {
        if (followUp == null || hooks == null || followUp.secondaryRiderToken() == null) {
            return;
        }
        hooks.applySecondaryRiderToken(followUp.secondaryRiderToken());
    }

    public static void applyPayoffs(ActiveWeaponFollowUp followUp, double resolvedDamage, PayoffHooks hooks) {
        if (followUp == null || hooks == null) {
            return;
        }
        if (followUp.shieldPercentOnHit() > 0.0) {
            hooks.applyShield(followUp.shieldPercentOnHit());
        }
        if (followUp.lifestealBonus() > 0.0 && resolvedDamage > 0.0) {
            hooks.heal(resolvedDamage * followUp.lifestealBonus());
        }
        if (followUp.healRatioOnHit() > 0.0 && resolvedDamage > 0.0) {
            hooks.heal(resolvedDamage * followUp.healRatioOnHit());
        }
        if (WeaponFollowUpHitMath.hasSplash(followUp, resolvedDamage)) {
            hooks.applySplash();
        }
    }

    public interface RiderHooks {
        void applyRiderToken(String token);

        void applySecondaryRiderToken(String token);
    }

    public interface PayoffHooks {
        void applyShield(double shieldPercent);

        void heal(double amount);

        void applySplash();
    }
}
