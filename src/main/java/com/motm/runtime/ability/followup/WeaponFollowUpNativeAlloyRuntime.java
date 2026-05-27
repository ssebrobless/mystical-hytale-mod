package com.motm.runtime.ability.followup;

import com.motm.util.AbilityPresentation;

public final class WeaponFollowUpNativeAlloyRuntime {

    public String applyNativeDamage(ActiveWeaponFollowUp followUp,
                                    String itemId,
                                    float beforeDamage,
                                    Hooks hooks) {
        if (!isUsableAlloyFollowUp(followUp, itemId) || hooks == null) {
            return null;
        }

        WeaponFollowUpItemBinding binding = followUp.bindOrRejectItem(itemId);
        if (!binding.accepted()) {
            hooks.removeFollowUp(followUp.playerId());
            hooks.logRejected(binding.message(), followUp.playerId());
            return binding.message();
        }
        if (binding.newlyBound()) {
            hooks.logBound(followUp.playerId(), itemId, followUp.remainingUses());
        }

        hooks.applyVisual();
        float afterDamage = WeaponFollowUpHitMath.alloyNativeDamageAfter(beforeDamage, followUp);
        hooks.setDamage(afterDamage);
        hooks.applyImpact();
        WeaponFollowUpHitEffects.applyNativeAlloyRiders(followUp, new WeaponFollowUpHitEffects.RiderHooks() {
            @Override
            public void applyRiderToken(String token) {
            }

            @Override
            public void applySecondaryRiderToken(String token) {
                hooks.applySecondaryRiderToken(token);
            }
        });
        boolean restored = hooks.restoreDurability(itemId);

        int remainingUses = followUp.decrementRemainingUses();
        if (remainingUses <= 0) {
            hooks.removeFollowUp(followUp.playerId());
            hooks.clearVisual();
        }

        String message = "[MOTM] Alloy Enhancement hit: "
                + AbilityPresentation.formatDecimal(beforeDamage)
                + " -> "
                + AbilityPresentation.formatDecimal(afterDamage)
                + " damage"
                + (restored ? " | durability protected" : "")
                + (remainingUses > 0 ? " | " + remainingUses + " use(s) left" : " | Alloy finished");
        hooks.logNativeDamage(message, followUp.playerId(), itemId);
        return message;
    }

    public String applyToolUse(ActiveWeaponFollowUp followUp,
                               String itemId,
                               Hooks hooks) {
        if (!isUsableAlloyFollowUp(followUp, itemId) || hooks == null) {
            return null;
        }

        WeaponFollowUpItemBinding binding = followUp.bindOrRejectItem(itemId);
        if (!binding.accepted()) {
            hooks.removeFollowUp(followUp.playerId());
            hooks.logRejected(binding.message(), followUp.playerId());
            hooks.clearVisual();
            return binding.message();
        }
        if (binding.newlyBound()) {
            hooks.logBound(followUp.playerId(), itemId, followUp.remainingUses());
        }

        hooks.applyVisual();
        boolean restored = hooks.restoreDurability(itemId);
        int remainingUses = followUp.decrementRemainingUses();
        if (remainingUses <= 0) {
            hooks.removeFollowUp(followUp.playerId());
            hooks.clearVisual();
        }

        return "[MOTM] Alloy durability shield: "
                + (restored ? "protected " : "tracked ")
                + itemId
                + " use"
                + (remainingUses > 0 ? " | " + remainingUses + " Alloy use(s) left" : " | Alloy finished");
    }

    private static boolean isUsableAlloyFollowUp(ActiveWeaponFollowUp followUp, String itemId) {
        return followUp != null
                && followUp.alloyFollowUp()
                && itemId != null
                && !itemId.isBlank();
    }

    public interface Hooks {
        void applyVisual();

        void clearVisual();

        void setDamage(float amount);

        void applyImpact();

        void applySecondaryRiderToken(String token);

        boolean restoreDurability(String itemId);

        void removeFollowUp(String playerId);

        void logNativeDamage(String message, String playerId, String itemId);

        void logBound(String playerId, String itemId, int remainingUses);

        void logRejected(String message, String playerId);
    }
}
