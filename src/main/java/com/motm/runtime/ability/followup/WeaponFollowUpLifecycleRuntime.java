package com.motm.runtime.ability.followup;

public final class WeaponFollowUpLifecycleRuntime {

    public boolean processExpiry(String playerId, ActiveWeaponFollowUp followUp, long now, Hooks hooks) {
        if (followUp == null) {
            return true;
        }

        boolean expiredByTime = now >= followUp.expireAtMillis();
        boolean exhausted = followUp.remainingUses() <= 0;
        if (!expiredByTime && !exhausted) {
            return false;
        }

        if (followUp.alloyFollowUp() && hooks != null) {
            if (hooks.playerAvailable(playerId)) {
                if (!hooks.canMutateVisual(playerId)) {
                    return false;
                }
                hooks.clearVisual(playerId);
            } else {
                hooks.logVisualClearSkipped(playerId);
            }
            hooks.logEnded(playerId, expiredByTime ? EndReason.DURATION_EXPIRED : EndReason.USES_EXHAUSTED);
        }
        return true;
    }

    public enum EndReason {
        DURATION_EXPIRED,
        USES_EXHAUSTED
    }

    public interface Hooks {
        boolean playerAvailable(String playerId);

        boolean canMutateVisual(String playerId);

        void clearVisual(String playerId);

        void logVisualClearSkipped(String playerId);

        void logEnded(String playerId, EndReason reason);
    }
}
