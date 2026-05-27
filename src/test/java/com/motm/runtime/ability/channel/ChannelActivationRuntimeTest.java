package com.motm.runtime.ability.channel;

import com.google.gson.Gson;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChannelActivationRuntimeTest {
    private static final Gson GSON = new Gson();

    private final ChannelActivationRuntime runtime = new ChannelActivationRuntime();

    @Test
    void createsChannelsAndLineControls() {
        TestRef ownerRef = new TestRef();
        TestRef targetRef = new TestRef();
        AbilityData channelAbility = ability("life_drain", "channel");
        ActiveChannel channel = runtime.createChannel("player", ownerRef, targetRef, channelAbility, 5_000L, 1_000L);

        assertEquals("player", channel.ownerPlayerId());
        assertEquals(ownerRef, channel.ownerRef());
        assertEquals(targetRef, channel.targetRef());
        assertEquals(channelAbility, channel.ability());
        assertEquals(5_000L, channel.expireAtMillis());
        assertEquals(1_000L, channel.nextPulseAtMillis());

        AbilityData lineAbility = ability("vines", "line_control");
        ActiveLineControl lineControl = runtime.createLineControl("player", ownerRef, targetRef, lineAbility, 6_000L, 1_200L);

        assertEquals("player", lineControl.ownerPlayerId());
        assertEquals(ownerRef, lineControl.ownerRef());
        assertEquals(targetRef, lineControl.targetRef());
        assertEquals(lineAbility, lineControl.ability());
        assertEquals(6_000L, lineControl.expireAtMillis());
        assertEquals(1_200L, lineControl.nextPulseAtMillis());
    }

    @Test
    void rejectsMissingInputs() {
        AbilityData ability = ability("life_drain", "channel");

        assertNull(runtime.createChannel(null, new TestRef(), new TestRef(), ability, 5_000L, 1_000L));
        assertNull(runtime.createChannel("player", new InvalidRef(), new TestRef(), ability, 5_000L, 1_000L));
        assertNull(runtime.createChannel("player", new TestRef(), new InvalidRef(), ability, 5_000L, 1_000L));
        assertNull(runtime.createChannel("player", new TestRef(), new TestRef(), null, 5_000L, 1_000L));

        assertNull(runtime.createLineControl(null, new TestRef(), new TestRef(), ability, 5_000L, 1_000L));
        assertNull(runtime.createLineControl("player", new InvalidRef(), new TestRef(), ability, 5_000L, 1_000L));
        assertNull(runtime.createLineControl("player", new TestRef(), new InvalidRef(), ability, 5_000L, 1_000L));
        assertNull(runtime.createLineControl("player", new TestRef(), new TestRef(), null, 5_000L, 1_000L));
    }

    private static AbilityData ability(String id, String castType) {
        return GSON.fromJson("{\"id\":\"" + id + "\",\"cast_type\":\"" + castType + "\"}", AbilityData.class);
    }

    private static final class TestRef extends Ref<EntityStore> {
        private TestRef() {
            super(null, 1);
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    private static final class InvalidRef extends Ref<EntityStore> {
        private InvalidRef() {
            super(null, 1);
        }

        @Override
        public boolean isValid() {
            return false;
        }
    }
}
