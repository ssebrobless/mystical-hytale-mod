package com.motm.runtime.ability.channel;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiveLineControlTest {

    private static final Gson GSON = new Gson();

    @Test
    void ownsLineControlPulseScheduling() {
        AbilityData ability = ability("""
                {
                  "id": "vines",
                  "cast_type": "line_control"
                }
                """);

        ActiveLineControl lineControl = new ActiveLineControl("player", null, null, ability, 4000L, 100L);
        lineControl.scheduleNextPulse(1000L, 350L);

        assertEquals("player", lineControl.ownerPlayerId());
        assertEquals(ability, lineControl.ability());
        assertEquals(4000L, lineControl.expireAtMillis());
        assertEquals(1350L, lineControl.nextPulseAtMillis());
    }

    private static AbilityData ability(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
