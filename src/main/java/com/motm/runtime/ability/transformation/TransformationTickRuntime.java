package com.motm.runtime.ability.transformation;

import com.hypixel.hytale.math.vector.Vector3d;
import com.motm.model.PlayerData;

public final class TransformationTickRuntime {

    public boolean process(ActiveTransformation form, long now, long pulseIntervalMillis, Hooks hooks) {
        if (form == null || form.ownerRef() == null || !form.ownerRef().isValid() || hooks == null || !hooks.hasOwnerStore(form)) {
            if (hooks != null) {
                hooks.clearNextPulse(form != null ? form.playerId() : null);
            }
            return true;
        }

        if (now >= form.expireAtMillis()) {
            hooks.clearNextPulse(form.playerId());
            return true;
        }

        long nextPulseAt = hooks.nextPulseAt(form.playerId(), now + pulseIntervalMillis);
        if (now < nextPulseAt) {
            return false;
        }

        PlayerData player = hooks.player(form.playerId());
        if (player == null) {
            hooks.clearNextPulse(form.playerId());
            return true;
        }

        if (hooks.shouldEnd(form, player)) {
            hooks.clearNextPulse(form.playerId());
            return true;
        }

        Vector3d origin = hooks.ownerPosition(form);
        if (origin == null) {
            hooks.clearNextPulse(form.playerId());
            return true;
        }

        hooks.refreshOwnerState(form, player);
        hooks.applyLocomotionPressure(form, player, origin);
        hooks.applyFormPulse(form, player, origin);
        hooks.scheduleNextPulse(form.playerId(), now + pulseIntervalMillis);
        return false;
    }

    public interface Hooks {
        boolean hasOwnerStore(ActiveTransformation form);

        void clearNextPulse(String playerId);

        long nextPulseAt(String playerId, long defaultValue);

        PlayerData player(String playerId);

        boolean shouldEnd(ActiveTransformation form, PlayerData player);

        Vector3d ownerPosition(ActiveTransformation form);

        void refreshOwnerState(ActiveTransformation form, PlayerData player);

        void applyLocomotionPressure(ActiveTransformation form, PlayerData player, Vector3d origin);

        void applyFormPulse(ActiveTransformation form, PlayerData player, Vector3d origin);

        void scheduleNextPulse(String playerId, long nextPulseAtMillis);
    }
}
