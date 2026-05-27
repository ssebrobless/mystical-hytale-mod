package com.motm.runtime.ability.followup;

public final class WeaponFollowUpHitMath {

    private WeaponFollowUpHitMath() {
    }

    public static double baseWeaponDamage(int playerLevel,
                                          double abilityPowerMultiplier,
                                          ActiveWeaponFollowUp followUp) {
        return ((4.0 + (playerLevel * 0.9)) * abilityPowerMultiplier)
                + (followUp != null ? followUp.flatDamageBonus() : 0.0);
    }

    public static double attackModifier(double statusDamageIncrease,
                                        double consumedDamageBuff,
                                        double consumedStealthBuff,
                                        ActiveWeaponFollowUp followUp,
                                        double transformationWeaponBonus) {
        return 1.0
                + statusDamageIncrease
                + consumedDamageBuff
                + consumedStealthBuff
                + (followUp != null ? followUp.damageMultiplierBonus() : 0.0)
                + transformationWeaponBonus;
    }

    public static double applyPassiveBonus(double damage, double passiveBonusDamage) {
        return damage + passiveBonusDamage;
    }

    public static double applyIncomingMultiplier(double damage, double incomingMultiplier) {
        return damage * incomingMultiplier;
    }

    public static float alloyNativeDamageAfter(float before, ActiveWeaponFollowUp followUp) {
        return (float) (before * (1.0 + (followUp != null ? followUp.damageMultiplierBonus() : 0.0)));
    }

    public static double splashDamage(double resolvedDamage, ActiveWeaponFollowUp followUp) {
        if (followUp == null || followUp.splashDamageRatio() <= 0.0 || resolvedDamage <= 0.0) {
            return 0.0;
        }
        return resolvedDamage * followUp.splashDamageRatio();
    }

    public static boolean hasSplash(ActiveWeaponFollowUp followUp, double resolvedDamage) {
        return followUp != null
                && followUp.splashRadius() > 0.0
                && followUp.splashDamageRatio() > 0.0
                && resolvedDamage > 0.0;
    }
}
