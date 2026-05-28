package com.motm.runtime.ability.followup;

public record WeaponFollowUpSpec(
        boolean armed,
        int uses,
        double flatDamageBonus,
        String riderToken,
        double lifestealBonus,
        double shieldPercentOnHit,
        double healRatioOnHit,
        double splashRadius,
        double splashDamageRatio,
        String secondaryRiderToken,
        double damageMultiplierBonus,
        boolean alloyFollowUp
) {
    public static WeaponFollowUpSpec none() {
        return new WeaponFollowUpSpec(
                false,
                0,
                0.0,
                null,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                null,
                0.0,
                false
        );
    }
}
