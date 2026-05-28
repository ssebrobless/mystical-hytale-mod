package com.motm.runtime.ability;

import com.google.gson.Gson;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityExecutionPolicyTest {

    private static final Gson GSON = new Gson();

    @Test
    void suppressesGenericCasterVisualForMigratedRuntimeOwners() {
        assertTrue(AbilityExecutionPolicy.suppressGenericCasterVisual(ability("""
                {"id":"magma_sling","cast_type":"projectile"}
                """)));
        assertTrue(AbilityExecutionPolicy.suppressGenericCasterVisual(ability("""
                {"id":"alloy_enhancement","cast_type":"self_buff"}
                """)));
        assertFalse(AbilityExecutionPolicy.suppressGenericCasterVisual(ability("""
                {"id":"generic_cast","cast_type":"self_buff"}
                """)));
    }

    @Test
    void classifiesMovementAndGroundRestrictedProfiles() {
        assertTrue(AbilityExecutionPolicy.isMovementCast(ability("""
                {"id":"dashy","cast_type":"dash"}
                """)));
        assertTrue(AbilityExecutionPolicy.isGroundRestricted(ability("""
                {"id":"smoke_form","cast_type":"transformation"}
                """)));
        assertTrue(AbilityExecutionPolicy.isGroundRestricted(ability("""
                {"id":"sky_form","cast_type":"transformation","travel_type":"flight"}
                """)));
        assertFalse(AbilityExecutionPolicy.isGroundRestricted(ability("""
                {"id":"grounded_cast","cast_type":"self_buff"}
                """)));
    }

    @Test
    void resolvesCasterAndTargetTokenPolicies() {
        AbilityData alloy = ability("""
                {"id":"alloy_enhancement","effect":"damage_buff+speed"}
                """);
        assertFalse(AbilityExecutionPolicy.shouldApplyCasterEffectToken(alloy, "damage_buff"));
        assertTrue(AbilityExecutionPolicy.shouldApplyCasterEffectToken(alloy, "speed"));
        assertFalse(AbilityExecutionPolicy.shouldApplyCasterEffectToken(alloy, "heal"));

        AbilityData dominate = ability("""
                {"id":"dominate","effect":"curse"}
                """);
        assertEquals(
                List.of("curse", "root", "disoriented"),
                AbilityExecutionPolicy.targetEffectTokens(dominate, List.of("curse", "unknown"))
        );
    }

    @Test
    void resolvesCombatMovementAndSpecialDamagePolicies() {
        assertEquals(DamageCause.PROJECTILE, AbilityExecutionPolicy.directDamageCause(ability("""
                {"id":"line","cast_type":"projectile_line"}
                """)));
        assertEquals(DamageCause.PHYSICAL, AbilityExecutionPolicy.directDamageCause(ability("""
                {"id":"melee","cast_type":"ground_burst"}
                """)));
        assertEquals(AbilityExecutionPolicy.SpecialDamagePolicy.COMBUST, AbilityExecutionPolicy.specialDamagePolicy(ability("""
                {"id":"combust"}
                """)));
        assertEquals(AbilityExecutionPolicy.SpecialDamagePolicy.CONSUME, AbilityExecutionPolicy.specialDamagePolicy(ability("""
                {"id":"consume"}
                """)));
        assertEquals(AbilityExecutionPolicy.SpecialDamagePolicy.LIGHTNING, AbilityExecutionPolicy.specialDamagePolicy(ability("""
                {"id":"storm","effect":"lightning"}
                """)));
        assertTrue(AbilityExecutionPolicy.isAnchorDrag(ability("""
                {"id":"anchor_haul","travel_type":"pull"}
                """)));
    }

    private static AbilityData ability(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
