package com.motm.runtime.ability.followup;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponFollowUpSpecsTest {

    private static final Gson GSON = new Gson();

    @Test
    void resolvesAlloyEnhancementAsNativeWeaponFollowUpProfile() {
        AbilityData ability = parseAbility("""
                {
                  "id": "alloy_enhancement",
                  "name": "Alloy Enhancement",
                  "cast_type": "self_buff",
                  "effect": "damage_buff",
                  "damage_percent": 35,
                  "duration_seconds": 8
                }
                """);

        WeaponFollowUpSpec spec = WeaponFollowUpSpecs.resolve(ability);

        assertTrue(spec.armed());
        assertTrue(spec.alloyFollowUp());
        assertEquals(3, spec.uses());
        assertEquals(27.0, spec.flatDamageBonus(), 0.0001);
        assertEquals(0.35, spec.damageMultiplierBonus(), 0.0001);
        assertEquals("vulnerability", spec.secondaryRiderToken());
        assertNull(spec.riderToken());
    }

    @Test
    void excludesBuffsThatAreNotWeaponFollowUps() {
        AbilityData metalCoat = parseAbility("""
                {
                  "id": "metal_coat",
                  "cast_type": "self_buff",
                  "effect": "shield",
                  "shield_percent": 30
                }
                """);
        AbilityData obsidianSkin = parseAbility("""
                {
                  "id": "obsidian_skin",
                  "cast_type": "self_buff",
                  "effect": "shield",
                  "shield_percent": 40
                }
                """);

        assertFalse(WeaponFollowUpSpecs.resolve(metalCoat).armed());
        assertFalse(WeaponFollowUpSpecs.resolve(obsidianSkin).armed());
    }

    @Test
    void resolvesGenericAttackBuffWithoutAbilitySpecificBranchInManager() {
        AbilityData ability = parseAbility("""
                {
                  "id": "generic_attack",
                  "cast_type": "dash_buff",
                  "effect": "attack_buff",
                  "damage_percent": 10
                }
                """);

        WeaponFollowUpSpec spec = WeaponFollowUpSpecs.resolve(ability);

        assertTrue(spec.armed());
        assertFalse(spec.alloyFollowUp());
        assertEquals(3, spec.uses());
        assertEquals(10.0, spec.flatDamageBonus(), 0.0001);
        assertEquals(0.0, spec.damageMultiplierBonus(), 0.0001);
    }

    @Test
    void resolvesRefractionSplashAndRiders() {
        AbilityData ability = parseAbility("""
                {
                  "id": "refraction",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """);

        WeaponFollowUpSpec spec = WeaponFollowUpSpecs.resolve(ability);

        assertTrue(spec.armed());
        assertEquals(3, spec.uses());
        assertEquals(15.0, spec.flatDamageBonus(), 0.0001);
        assertEquals("vulnerability", spec.riderToken());
        assertEquals("slow", spec.secondaryRiderToken());
        assertEquals(4.5, spec.splashRadius(), 0.0001);
        assertEquals(0.55, spec.splashDamageRatio(), 0.0001);
    }

    @Test
    void ignoresNonBuffCastTypes() {
        AbilityData ability = parseAbility("""
                {
                  "id": "not_a_follow_up",
                  "cast_type": "projectile",
                  "effect": "damage_buff"
                }
                """);

        assertFalse(WeaponFollowUpSpecs.resolve(ability).armed());
        assertFalse(WeaponFollowUpSpecs.resolve(null).armed());
    }

    private static AbilityData parseAbility(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
