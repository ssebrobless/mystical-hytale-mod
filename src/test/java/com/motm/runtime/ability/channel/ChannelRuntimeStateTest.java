package com.motm.runtime.ability.channel;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChannelRuntimeStateTest {
    private static final Gson GSON = new Gson();

    @Test
    void replacesChannelAndLineControlPerPlayer() {
        ChannelRuntimeState state = new ChannelRuntimeState();

        state.replaceChannelForPlayer("player", channel("player", "water_beam"));
        state.replaceChannelForPlayer("player", channel("player", "bubble_stream"));
        state.replaceLineControlForPlayer("player", lineControl("player", "vines"));
        state.replaceLineControlForPlayer("player", lineControl("player", "current_pull"));

        assertEquals(1, state.activeChannelCount());
        assertEquals(1, state.activeLineControlCount());
    }

    @Test
    void removesChannelsAndLineControlsForOwnerAndAbility() {
        ChannelRuntimeState state = new ChannelRuntimeState();
        state.replaceChannelForPlayer("player-a", channel("player-a", "water_beam"));
        state.replaceChannelForPlayer("player-b", channel("player-b", "water_beam"));
        state.replaceLineControlForPlayer("player-a", lineControl("player-a", "vines"));
        state.replaceLineControlForPlayer("player-b", lineControl("player-b", "current_pull"));

        assertEquals(1, state.removeChannelsForAbility("player-a", "water_beam"));
        assertEquals(1, state.removeLineControlsForAbility("player-a", "vines"));
        assertEquals(1, state.activeChannelCount());
        assertEquals(1, state.activeLineControlCount());

        assertEquals(1, state.removeChannelsForPlayer("player-b"));
        assertEquals(1, state.removeLineControlsForPlayer("player-b"));
        assertEquals(0, state.activeChannelCount());
        assertEquals(0, state.activeLineControlCount());
    }

    @Test
    void removesProcessedChannelsAndLineControls() {
        ChannelRuntimeState state = new ChannelRuntimeState();
        state.replaceChannelForPlayer("player", channel("player", "water_beam"));
        state.replaceLineControlForPlayer("player", lineControl("player", "vines"));

        state.removeProcessedChannels(channel -> "player".equals(channel.ownerPlayerId()));
        state.removeProcessedLineControls(lineControl -> "player".equals(lineControl.ownerPlayerId()));

        assertEquals(0, state.activeChannelCount());
        assertEquals(0, state.activeLineControlCount());
    }

    private static ActiveChannel channel(String ownerPlayerId, String abilityId) {
        return new ActiveChannel(ownerPlayerId, null, null, ability(abilityId), 5000L, 1000L);
    }

    private static ActiveLineControl lineControl(String ownerPlayerId, String abilityId) {
        return new ActiveLineControl(ownerPlayerId, null, null, ability(abilityId), 5000L, 1000L);
    }

    private static AbilityData ability(String id) {
        return GSON.fromJson("{\"id\":\"" + id + "\",\"cast_type\":\"channel\"}", AbilityData.class);
    }
}
