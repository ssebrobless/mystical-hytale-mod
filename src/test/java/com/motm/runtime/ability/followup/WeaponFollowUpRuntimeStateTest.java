package com.motm.runtime.ability.followup;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WeaponFollowUpRuntimeStateTest {

    private static final Gson GSON = new Gson();

    @Test
    void ownsActiveFollowUpsByPlayer() {
        WeaponFollowUpRuntimeState state = new WeaponFollowUpRuntimeState();
        ActiveWeaponFollowUp followUp = followUp("alloy_enhancement");

        state.put("player", followUp);

        assertEquals(1, state.size());
        assertTrue(state.contains("player"));
        assertSame(followUp, state.get("player"));
        assertEquals("player", state.entries().getFirst().getKey());
        assertSame(followUp, state.remove("player"));
        assertNull(state.get("player"));
        assertEquals(0, state.size());
    }

    @Test
    void ignoresInvalidStateRequests() {
        WeaponFollowUpRuntimeState state = new WeaponFollowUpRuntimeState();

        state.put(null, followUp("alloy_enhancement"));
        state.put("player", null);

        assertEquals(0, state.size());
        assertNull(state.remove(""));
        assertNull(state.get(null));
    }

    private static ActiveWeaponFollowUp followUp(String abilityId) {
        AbilityData ability = GSON.fromJson("""
                {
                  "id": "%s",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """.formatted(abilityId), AbilityData.class);
        return ActiveWeaponFollowUp.create("player", ability, 1000L, WeaponFollowUpSpecs.resolve(ability));
    }
}
