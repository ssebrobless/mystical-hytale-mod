package com.motm.runtime.ability;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import com.motm.model.StatusEffect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AbilityStatusEffectsTest {

    private static final Gson GSON = new Gson();

    @Test
    void createsKnownStatusEffectsFromTokens() {
        StatusEffect burn = AbilityStatusEffects.create("self_burn", ability("{}"), "player-1", "combust");
        assertEquals(StatusEffect.Type.BURN, burn.getType());
        assertEquals(0.03, burn.getValue());
        assertEquals("player-1", burn.getSourcePlayerId());
        assertEquals("combust", burn.getSourcePerkOrAbility());

        StatusEffect evasion = AbilityStatusEffects.create("evasion_buff", ability("{}"), "player-1", "form");
        assertEquals(StatusEffect.Type.EVASION, evasion.getType());
        assertEquals(0.40, evasion.getValue());

        assertNull(AbilityStatusEffects.create("unknown", ability("{}"), "player-1", "ability"));
    }

    @Test
    void resolvesConfiguredAndDefaultDurations() {
        AbilityData configured = ability("""
                {"duration_seconds":1.5}
                """);

        assertEquals(30, AbilityStatusEffects.durationTicks(configured, "slow"));
        assertEquals(40, AbilityStatusEffects.durationTicks(ability("{}"), "stun"));
        assertEquals(120, AbilityStatusEffects.durationTicks(ability("{}"), "shield"));
        assertEquals(240, AbilityStatusEffects.durationTicks(ability("{}"), "damage_buff"));
        assertEquals(80, AbilityStatusEffects.durationTicks(ability("{}"), "unknown"));
    }

    private static AbilityData ability(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
