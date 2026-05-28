package com.motm.runtime.ability.followup;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponFollowUpHitMathTest {

    private static final Gson GSON = new Gson();

    @Test
    void computesBaseDamageAndModifierFromFollowUpInputs() {
        ActiveWeaponFollowUp followUp = followUp("""
                {
                  "id": "alloy_enhancement",
                  "cast_type": "self_buff",
                  "effect": "damage_buff",
                  "damage_percent": 35
                }
                """);

        assertEquals(60.0, WeaponFollowUpHitMath.baseWeaponDamage(20, 1.5, followUp), 0.0001);
        assertEquals(1.55, WeaponFollowUpHitMath.attackModifier(0.10, 0.05, 0.0, followUp, 0.10), 0.0001);
        assertEquals(65.0, WeaponFollowUpHitMath.applyPassiveBonus(60.0, 5.0), 0.0001);
        assertEquals(48.75, WeaponFollowUpHitMath.applyIncomingMultiplier(65.0, 0.75), 0.0001);
    }

    @Test
    void computesNativeAlloyAndSplashDamage() {
        ActiveWeaponFollowUp alloy = followUp("""
                {
                  "id": "alloy_enhancement",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """);
        ActiveWeaponFollowUp refraction = followUp("""
                {
                  "id": "refraction",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """);

        assertEquals(13.0f, WeaponFollowUpHitMath.alloyNativeDamageAfter(10.0f, alloy), 0.0001f);
        assertEquals(55.0, WeaponFollowUpHitMath.splashDamage(100.0, refraction), 0.0001);
        assertTrue(WeaponFollowUpHitMath.hasSplash(refraction, 100.0));
        assertFalse(WeaponFollowUpHitMath.hasSplash(alloy, 100.0));
        assertFalse(WeaponFollowUpHitMath.hasSplash(refraction, 0.0));
    }

    private static ActiveWeaponFollowUp followUp(String json) {
        AbilityData ability = GSON.fromJson(json, AbilityData.class);
        return ActiveWeaponFollowUp.create("player", ability, 1000L, WeaponFollowUpSpecs.resolve(ability));
    }
}
