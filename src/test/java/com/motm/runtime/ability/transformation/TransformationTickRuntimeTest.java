package com.motm.runtime.ability.transformation;

import com.google.gson.Gson;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformationTickRuntimeTest {
    private static final Gson GSON = new Gson();
    private final TransformationTickRuntime runtime = new TransformationTickRuntime();

    @Test
    void removesInvalidFormAndClearsPulse() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveTransformation form = form(new InvalidRef(), 10_000L, "smoke_form");

        assertTrue(runtime.process(form, 1_000L, 850L, hooks));
        assertEquals(List.of("clear:player"), hooks.events);
    }

    @Test
    void removesExpiredFormAndClearsPulse() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveTransformation form = form(new TestRef(), 1_000L, "smoke_form");

        assertTrue(runtime.process(form, 1_000L, 850L, hooks));
        assertEquals(List.of("store", "clear:player"), hooks.events);
    }

    @Test
    void waitsUntilNextPulseWithoutLoadingPlayer() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.nextPulseAt = 2_000L;
        ActiveTransformation form = form(new TestRef(), 10_000L, "smoke_form");

        assertFalse(runtime.process(form, 1_000L, 850L, hooks));
        assertEquals(List.of("store", "nextPulse"), hooks.events);
    }

    @Test
    void removesWhenPlayerMissingShouldEndOrPositionMissing() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.playerAvailable = false;
        hooks.nextPulseAt = 1_000L;
        ActiveTransformation form = form(new TestRef(), 10_000L, "smoke_form");

        assertTrue(runtime.process(form, 1_000L, 850L, hooks));
        assertEquals(List.of("store", "nextPulse", "player", "clear:player"), hooks.events);

        hooks = new RecordingHooks();
        hooks.nextPulseAt = 1_000L;
        hooks.shouldEnd = true;
        assertTrue(runtime.process(form, 1_000L, 850L, hooks));
        assertEquals(List.of("store", "nextPulse", "player", "shouldEnd", "clear:player"), hooks.events);

        hooks = new RecordingHooks();
        hooks.nextPulseAt = 1_000L;
        hooks.ownerPosition = null;
        assertTrue(runtime.process(form, 1_000L, 850L, hooks));
        assertEquals(List.of("store", "nextPulse", "player", "shouldEnd", "position", "clear:player"),
                hooks.events);
    }

    @Test
    void appliesRefreshLocomotionAndPulseBeforeSchedulingNextPulse() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.nextPulseAt = 1_000L;
        ActiveTransformation form = form(new TestRef(), 10_000L, "smoke_form");

        assertFalse(runtime.process(form, 1_000L, 850L, hooks));

        assertEquals(List.of(
                "store",
                "nextPulse",
                "player",
                "shouldEnd",
                "position",
                "refresh",
                "locomotion",
                "pulse",
                "schedule:1850"
        ), hooks.events);
    }

    private static ActiveTransformation form(Ref<EntityStore> ownerRef, long expireAtMillis, String abilityId) {
        return ActiveTransformation.create(
                "player",
                ownerRef,
                ability(abilityId),
                "model",
                expireAtMillis,
                new Vector3d(0.0, 0.0, 0.0)
        );
    }

    private static AbilityData ability(String abilityId) {
        return GSON.fromJson("""
                {
                  "id": "%s",
                  "cast_type": "transformation",
                  "duration_seconds": 10.0
                }
                """.formatted(abilityId), AbilityData.class);
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

    private static final class RecordingHooks implements TransformationTickRuntime.Hooks {
        private final List<String> events = new ArrayList<>();
        private boolean storeAvailable = true;
        private boolean playerAvailable = true;
        private boolean shouldEnd;
        private long nextPulseAt;
        private Vector3d ownerPosition = new Vector3d(1.0, 2.0, 3.0);

        @Override
        public boolean hasOwnerStore(ActiveTransformation form) {
            events.add("store");
            return storeAvailable;
        }

        @Override
        public void clearNextPulse(String playerId) {
            events.add("clear:" + playerId);
        }

        @Override
        public long nextPulseAt(String playerId, long defaultValue) {
            events.add("nextPulse");
            return nextPulseAt > 0 ? nextPulseAt : defaultValue;
        }

        @Override
        public PlayerData player(String playerId) {
            events.add("player");
            if (!playerAvailable) {
                return null;
            }
            PlayerData player = new PlayerData();
            player.setPlayerId(playerId);
            return player;
        }

        @Override
        public boolean shouldEnd(ActiveTransformation form, PlayerData player) {
            events.add("shouldEnd");
            return shouldEnd;
        }

        @Override
        public Vector3d ownerPosition(ActiveTransformation form) {
            events.add("position");
            return ownerPosition;
        }

        @Override
        public void refreshOwnerState(ActiveTransformation form, PlayerData player) {
            events.add("refresh");
        }

        @Override
        public void applyLocomotionPressure(ActiveTransformation form, PlayerData player, Vector3d origin) {
            events.add("locomotion");
        }

        @Override
        public void applyFormPulse(ActiveTransformation form, PlayerData player, Vector3d origin) {
            events.add("pulse");
        }

        @Override
        public void scheduleNextPulse(String playerId, long nextPulseAtMillis) {
            events.add("schedule:" + nextPulseAtMillis);
        }
    }
}
