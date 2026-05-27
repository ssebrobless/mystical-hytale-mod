package com.motm.runtime.ability.followup;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponFollowUpVisualEffectsTest {

    @Test
    void appliesAlloyVisualEffectContract() {
        List<String> calls = new ArrayList<>();

        WeaponFollowUpVisualEffects.applyAlloyVisual(new WeaponFollowUpVisualEffects.VisualHooks() {
            @Override
            public void applyEffect(String effectId) {
                calls.add("apply:" + effectId);
            }

            @Override
            public boolean removeEffect(String effectId) {
                calls.add("remove:" + effectId);
                return false;
            }
        });

        assertEquals(List.of("apply:" + WeaponFollowUpVisualEffects.ALLOY_VISUAL_EFFECT_ID), calls);
    }

    @Test
    void clearsAlloyVisualEffectContract() {
        List<String> calls = new ArrayList<>();

        boolean removed = WeaponFollowUpVisualEffects.clearAlloyVisual(new WeaponFollowUpVisualEffects.VisualHooks() {
            @Override
            public void applyEffect(String effectId) {
                calls.add("apply:" + effectId);
            }

            @Override
            public boolean removeEffect(String effectId) {
                calls.add("remove:" + effectId);
                return true;
            }
        });

        assertTrue(removed);
        assertEquals(List.of("remove:" + WeaponFollowUpVisualEffects.ALLOY_VISUAL_EFFECT_ID), calls);
    }

    @Test
    void returnsFalseWhenAlloyVisualIsNotRemoved() {
        boolean removed = WeaponFollowUpVisualEffects.clearAlloyVisual(new WeaponFollowUpVisualEffects.VisualHooks() {
            @Override
            public void applyEffect(String effectId) {
            }

            @Override
            public boolean removeEffect(String effectId) {
                return false;
            }
        });

        assertFalse(removed);
    }

    @Test
    void ignoresMissingHooksSafely() {
        assertDoesNotThrow(() -> WeaponFollowUpVisualEffects.applyAlloyVisual(null));
        assertFalse(WeaponFollowUpVisualEffects.clearAlloyVisual(null));
    }
}
