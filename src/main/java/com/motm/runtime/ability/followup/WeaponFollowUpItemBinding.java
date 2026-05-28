package com.motm.runtime.ability.followup;

public record WeaponFollowUpItemBinding(
        boolean accepted,
        boolean newlyBound,
        String message
) {
    public static WeaponFollowUpItemBinding accepted(boolean newlyBound) {
        return new WeaponFollowUpItemBinding(true, newlyBound, null);
    }

    public static WeaponFollowUpItemBinding rejected(String message) {
        return new WeaponFollowUpItemBinding(false, false, message);
    }
}
