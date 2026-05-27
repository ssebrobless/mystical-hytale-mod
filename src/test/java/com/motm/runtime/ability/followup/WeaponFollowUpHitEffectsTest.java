package com.motm.runtime.ability.followup;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeaponFollowUpHitEffectsTest {

    private static final Gson GSON = new Gson();

    @Test
    void appliesRidersInRuntimeOwnedOrder() {
        ActiveWeaponFollowUp refraction = followUp("""
                {
                  "id": "refraction",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """);
        List<String> calls = new ArrayList<>();

        WeaponFollowUpHitEffects.applyTargetRiders(refraction, new WeaponFollowUpHitEffects.RiderHooks() {
            @Override
            public void applyRiderToken(String token) {
                calls.add("rider:" + token);
            }

            @Override
            public void applySecondaryRiderToken(String token) {
                calls.add("secondary:" + token);
            }
        });

        assertEquals(List.of("rider:vulnerability", "secondary:slow"), calls);
    }

    @Test
    void appliesPayoffsInRuntimeOwnedOrder() {
        ActiveWeaponFollowUp absorb = followUp("""
                {
                  "id": "absorb",
                  "cast_type": "self_buff",
                  "effect": "shield+heal",
                  "shield_percent": 30
                }
                """);
        List<String> calls = new ArrayList<>();

        WeaponFollowUpHitEffects.applyPayoffs(absorb, 100.0, new WeaponFollowUpHitEffects.PayoffHooks() {
            @Override
            public void applyShield(double shieldPercent) {
                calls.add("shield:" + shieldPercent);
            }

            @Override
            public void heal(double amount) {
                calls.add("heal:" + amount);
            }

            @Override
            public void applySplash() {
                calls.add("splash");
            }
        });

        assertEquals(List.of("shield:10.5", "heal:38.0"), calls);
    }

    @Test
    void appliesNativeAlloySecondaryRiderOnly() {
        ActiveWeaponFollowUp alloy = followUp("""
                {
                  "id": "alloy_enhancement",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """);
        List<String> calls = new ArrayList<>();

        WeaponFollowUpHitEffects.applyNativeAlloyRiders(alloy, new WeaponFollowUpHitEffects.RiderHooks() {
            @Override
            public void applyRiderToken(String token) {
                calls.add("rider:" + token);
            }

            @Override
            public void applySecondaryRiderToken(String token) {
                calls.add("secondary:" + token);
            }
        });

        assertEquals(List.of("secondary:vulnerability"), calls);
    }

    private static ActiveWeaponFollowUp followUp(String json) {
        AbilityData ability = GSON.fromJson(json, AbilityData.class);
        return ActiveWeaponFollowUp.create("player", ability, 1000L, WeaponFollowUpSpecs.resolve(ability));
    }
}
