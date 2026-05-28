package com.motm.runtime.ability.followup;

public final class WeaponFollowUpVisualEffects {

    public static final String ALLOY_VISUAL_EFFECT_ID = "MOTM_Proof_Alloy_Enhancement";

    private WeaponFollowUpVisualEffects() {
    }

    public static void applyAlloyVisual(VisualHooks hooks) {
        if (hooks != null) {
            hooks.applyEffect(ALLOY_VISUAL_EFFECT_ID);
        }
    }

    public static boolean clearAlloyVisual(VisualHooks hooks) {
        return hooks != null && hooks.removeEffect(ALLOY_VISUAL_EFFECT_ID);
    }

    public interface VisualHooks {
        void applyEffect(String effectId);

        boolean removeEffect(String effectId);
    }
}
