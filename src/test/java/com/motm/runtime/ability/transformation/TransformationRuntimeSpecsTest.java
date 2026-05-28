package com.motm.runtime.ability.transformation;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformationRuntimeSpecsTest {

    private static final Gson GSON = new Gson();

    @Test
    void resolvesSmokeFormProfile() {
        TransformationRuntimeSpec spec = TransformationRuntimeSpecs.resolve(ability("""
                {
                  "id": "smoke_form",
                  "cast_type": "transformation"
                }
                """));

        assertEquals(TransformationRuntimeKind.SMOKE, spec.kind());
        assertEquals("MOTM_Aero_Smoke_Form", spec.visualEffectId());
        assertEquals(0.05, spec.damageBonus(), 0.0001);
        assertEquals(0.12, spec.weaponBonus(), 0.0001);
        assertEquals(1.22, spec.movementMultiplier(), 0.0001);
        assertEquals(0.35, spec.verticalBonus(), 0.0001);
        assertEquals("blind", spec.weaponRiderToken());
        assertEquals(0.95, spec.locomotionTriggerDistance(), 0.0001);
        assertEquals(1.75, spec.collisionRadius(), 0.0001);
        assertEquals(List.of("evasion_buff"), spec.ownerRuntimeTokens());
        assertEquals(0.0, spec.ownerShieldAmount(), 0.0001);
        assertTrue(spec.endsWhenGrounded());
        assertEquals("mist body + drift blinds", spec.summary());
    }

    @Test
    void resolvesCorruptusBeastProfiles() {
        TransformationRuntimeSpec pterodactyl = TransformationRuntimeSpecs.resolve(ability("""
                {
                  "id": "pterodactyl_form",
                  "cast_type": "transformation"
                }
                """));
        TransformationRuntimeSpec triceratops = TransformationRuntimeSpecs.resolve(ability("""
                {
                  "id": "triceratops_form",
                  "cast_type": "transformation"
                }
                """));
        TransformationRuntimeSpec tRex = TransformationRuntimeSpecs.resolve(ability("""
                {
                  "id": "t_rex_form",
                  "cast_type": "transformation"
                }
                """));

        assertEquals(TransformationRuntimeKind.PTERODACTYL, pterodactyl.kind());
        assertEquals("MOTM_Corruptus_Pterodactyl_Form", pterodactyl.visualEffectId());
        assertEquals("slow", pterodactyl.weaponRiderToken());
        assertEquals(1.42, pterodactyl.movementMultiplier(), 0.0001);
        assertEquals(List.of("speed", "evasion"), pterodactyl.ownerRuntimeTokens());
        assertTrue(pterodactyl.endsWhenGrounded());
        assertEquals(TransformationRuntimeKind.TRICERATOPS, triceratops.kind());
        assertEquals("MOTM_Corruptus_Triceratops_Form", triceratops.visualEffectId());
        assertEquals("knockback", triceratops.weaponRiderToken());
        assertEquals(2.45, triceratops.collisionRadius(), 0.0001);
        assertEquals(List.of("defense_buff"), triceratops.ownerRuntimeTokens());
        assertEquals(3.0, triceratops.ownerShieldAmount(), 0.0001);
        assertEquals(TransformationRuntimeKind.T_REX, tRex.kind());
        assertEquals("MOTM_Corruptus_TRex_Form", tRex.visualEffectId());
        assertEquals("stun", tRex.weaponRiderToken());
        assertEquals(0.34, tRex.weaponBonus(), 0.0001);
        assertEquals(List.of("attack_buff"), tRex.ownerRuntimeTokens());
    }

    @Test
    void fallbackKeepsUnknownTransformationsMechanicallyActiveWithoutVisualEffect() {
        TransformationRuntimeSpec spec = TransformationRuntimeSpecs.resolve(ability("""
                {
                  "id": "unknown_form",
                  "cast_type": "transformation"
                }
                """));

        assertEquals(TransformationRuntimeKind.GENERIC, spec.kind());
        assertNull(spec.visualEffectId());
        assertEquals(0.10, spec.damageBonus(), 0.0001);
        assertEquals(0.15, spec.weaponBonus(), 0.0001);
        assertEquals(1.10, spec.movementMultiplier(), 0.0001);
        assertEquals(2.00, spec.collisionRadius(), 0.0001);
        assertEquals(List.of(), spec.ownerRuntimeTokens());
        assertFalse(spec.endsWhenGrounded());
        assertEquals("transformed combat state", spec.summary());
    }

    @Test
    void visualEffectLookupUsesResolvedSpec() {
        assertEquals("MOTM_Aero_Smoke_Form", TransformationRuntimeSpecs.visualEffectId(ability("""
                {
                  "id": "smoke_form",
                  "cast_type": "transformation"
                }
                """)));
        assertNull(TransformationRuntimeSpecs.visualEffectId(null));
    }

    private static AbilityData ability(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
