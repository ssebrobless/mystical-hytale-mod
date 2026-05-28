package com.motm.runtime.ability.channel;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ActiveChannelTest {

    private static final Gson GSON = new Gson();

    @Test
    void ownsChannelPulseScheduling() {
        AbilityData ability = ability("""
                {
                  "id": "life_drain",
                  "cast_type": "channel"
                }
                """);

        ActiveChannel channel = new ActiveChannel("player", null, null, ability, 5000L, 100L);
        channel.scheduleNextPulse(1000L, 700L);

        assertEquals("player", channel.ownerPlayerId());
        assertEquals(ability, channel.ability());
        assertEquals(5000L, channel.expireAtMillis());
        assertEquals(1700L, channel.nextPulseAtMillis());
    }

    private static AbilityData ability(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
