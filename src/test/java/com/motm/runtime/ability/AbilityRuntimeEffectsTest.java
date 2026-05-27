package com.motm.runtime.ability;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AbilityRuntimeEffectsTest {

    private static final Gson GSON = new Gson();

    @Test
    void resolvesThemeOverridesBeforeClassFallbacks() {
        AbilityData ability = ability("""
                {"id":"scream_cast","cast_type":"projectile"}
                """);

        assertEquals("MOTM_Aero_Scream_Cast",
                AbilityRuntimeEffects.castEffectId("aero", "scream", ability));
        assertEquals("MOTM_Aero_Scream_Move",
                AbilityRuntimeEffects.projectileVisualEffectId("aero", "scream", ability));
        assertEquals("MOTM_Aero_Scream_Impact",
                AbilityRuntimeEffects.impactEffectId("aero", "scream", ability));
        assertEquals("MOTM_Aero_Scream_Field",
                AbilityRuntimeEffects.fieldVisualEffectId("aero", "scream", ability));
    }

    @Test
    void resolvesClassFallbacksForGenericEffects() {
        assertEquals("MOTM_Terra_Impact",
                AbilityRuntimeEffects.impactEffectId("terra", null, null));
        assertEquals("MOTM_Hydro_Move",
                AbilityRuntimeEffects.projectileVisualEffectId("hydro", null, null));
        assertEquals("MOTM_Corruptus_Cast",
                AbilityRuntimeEffects.castEffectId("corruptus", null, ability("""
                        {"id":"curse","cast_type":"self_buff"}
                        """)));
        assertEquals("MOTM_Aero_Move",
                AbilityRuntimeEffects.castEffectId("aero", null, ability("""
                        {"id":"dash","cast_type":"dash"}
                        """)));
        assertNull(AbilityRuntimeEffects.impactEffectId("unknown", null, null));
    }

    @Test
    void acceptsOnlyRuntimeEffectIdsFromAssetResolverCandidates() {
        assertEquals("MOTM_Custom",
                AbilityRuntimeEffects.asRuntimeEffectId("MOTM_Custom"));
        assertEquals("Server/Entity/Effects/Foo",
                AbilityRuntimeEffects.asRuntimeEffectId("Server/Entity/Effects/Foo"));
        assertNull(AbilityRuntimeEffects.asRuntimeEffectId("Server/Particles/Foo"));
        assertNull(AbilityRuntimeEffects.asRuntimeEffectId(""));
    }

    private static AbilityData ability(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
