package com.motm.runtime.ability.control;

import com.google.gson.Gson;
import com.hypixel.hytale.component.Ref;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlTickRuntimeTest {
    private static final Gson GSON = new Gson();
    private final ControlTickRuntime runtime = new ControlTickRuntime();

    @Test
    void endsWithoutHooksWhenControlledRefInvalid() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveControlledAlly ally = ally(new InvalidRef(), new TestRef(), 10_000L, 0L, 0L, false);
        assertTrue(runtime.process(ally, 1_000L, hooks));
        assertTrue(hooks.events.isEmpty());
    }

    @Test
    void releasesWhenControlExpired() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveControlledAlly ally = ally(new TestRef(), new TestRef(), 500L, 0L, 0L, false);
        assertTrue(runtime.process(ally, 1_000L, hooks));
        assertEquals(List.of("release"), hooks.events);
    }

    @Test
    void waitsUntilNextThinkWithoutStoreLookup() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveControlledAlly ally = ally(new TestRef(), new TestRef(), 10_000L, 5_000L, 0L, true);
        assertFalse(runtime.process(ally, 1_000L, hooks));
        assertTrue(hooks.events.isEmpty());
    }

    @Test
    void endsWhenStoreMissing() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.storeAvailable = false;
        ActiveControlledAlly ally = ally(new TestRef(), new TestRef(), 10_000L, 0L, 0L, true);
        assertTrue(runtime.process(ally, 1_000L, hooks));
        assertEquals(List.of("store"), hooks.events);
    }

    @Test
    void releasesWhenOwnerUnavailable() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.ownerAvailable = false;
        ActiveControlledAlly ally = ally(new TestRef(), new TestRef(), 10_000L, 0L, 0L, true);
        assertTrue(runtime.process(ally, 1_000L, hooks));
        assertEquals(List.of("store", "owner", "release"), hooks.events);
    }

    @Test
    void releasesWhenOwnerRefInvalid() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveControlledAlly ally = ally(new TestRef(), new InvalidRef(), 10_000L, 0L, 0L, true);
        assertTrue(runtime.process(ally, 1_000L, hooks));
        assertEquals(List.of("store", "owner", "release"), hooks.events);
    }

    @Test
    void appliesMarkerThenFollowsOwnerWhenNoTarget() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.target = null;
        ActiveControlledAlly ally = ally(new TestRef(), new TestRef(), 10_000L, 0L, 0L, false);
        assertFalse(runtime.process(ally, 1_000L, hooks));
        assertTrue(ally.markerApplied());
        assertTrue(hooks.events.contains("applyMarker"));
        assertTrue(hooks.events.contains("followOwner"));
        assertFalse(hooks.events.contains("attack"));
    }

    @Test
    void doesNotReapplyMarkerWhenAlreadyApplied() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.target = null;
        ActiveControlledAlly ally = ally(new TestRef(), new TestRef(), 10_000L, 0L, 0L, true);
        assertFalse(runtime.process(ally, 1_000L, hooks));
        assertFalse(hooks.events.contains("applyMarker"));
    }

    @Test
    void movesTowardTargetWhenOutOfRange() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.targetPosition = new Vector3d(20.0, 0.0, 0.0); // distance 20 > attackRange 6
        ActiveControlledAlly ally = ally(new TestRef(), new TestRef(), 10_000L, 0L, 0L, true);
        assertFalse(runtime.process(ally, 1_000L, hooks));
        assertTrue(hooks.events.contains("moveTowardTarget"));
        assertFalse(hooks.events.contains("attack"));
    }

    @Test
    void attacksWhenInRangeAndReady() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.targetPosition = new Vector3d(3.0, 0.0, 0.0); // distance 3 <= attackRange 6
        ActiveControlledAlly ally = ally(new TestRef(), new TestRef(), 10_000L, 0L, 0L, true);
        assertFalse(runtime.process(ally, 1_000L, hooks));
        assertTrue(hooks.events.contains("attack"));
        assertTrue(ally.nextAttackAtMillis() > 1_000L);
    }

    private static ActiveControlledAlly ally(Ref<EntityStore> controlledRef,
                                             Ref<EntityStore> ownerRef,
                                             long expireAtMillis,
                                             long nextThinkAtMillis,
                                             long nextAttackAtMillis,
                                             boolean markerApplied) {
        return new ActiveControlledAlly(
                "player",
                controlledRef,
                ownerRef,
                "mob-1",
                "corruptus",
                "mentokinesis",
                ability(),
                6.0,
                16.0,
                800L,
                "control_strike",
                expireAtMillis,
                nextThinkAtMillis,
                nextAttackAtMillis,
                null,
                0L,
                markerApplied
        );
    }

    private static AbilityData ability() {
        return GSON.fromJson("""
                {
                  "id": "dominate",
                  "cast_type": "gaze"
                }
                """, AbilityData.class);
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

    private static final class RecordingHooks implements ControlTickRuntime.Hooks {
        private final List<String> events = new ArrayList<>();
        private boolean storeAvailable = true;
        private boolean ownerAvailable = true;
        private Ref<EntityStore> target = new TestRef();
        private Vector3d allyPosition = new Vector3d(0.0, 0.0, 0.0);
        private Vector3d targetPosition = new Vector3d(3.0, 0.0, 0.0);

        @Override
        public boolean release(ActiveControlledAlly ally) {
            events.add("release");
            return true;
        }

        @Override
        public boolean hasStore(ActiveControlledAlly ally) {
            events.add("store");
            return storeAvailable;
        }

        @Override
        public PlayerData owner(String ownerPlayerId) {
            events.add("owner");
            if (!ownerAvailable) {
                return null;
            }
            PlayerData player = new PlayerData();
            player.setPlayerId(ownerPlayerId);
            return player;
        }

        @Override
        public void applyMarker(ActiveControlledAlly ally) {
            events.add("applyMarker");
        }

        @Override
        public Ref<EntityStore> resolveHostileTarget(ActiveControlledAlly ally, long now) {
            events.add("resolveHostileTarget");
            return target;
        }

        @Override
        public void followOwner(ActiveControlledAlly ally) {
            events.add("followOwner");
        }

        @Override
        public Vector3d position(Ref<EntityStore> ref) {
            if (ref == target) {
                events.add("targetPosition");
                return targetPosition;
            }
            events.add("allyPosition");
            return allyPosition;
        }

        @Override
        public void moveTowardTarget(ActiveControlledAlly ally, Ref<EntityStore> targetRef, double desiredRange) {
            events.add("moveTowardTarget");
        }

        @Override
        public void attack(ActiveControlledAlly ally, PlayerData owner, Ref<EntityStore> targetRef, long now) {
            events.add("attack");
        }
    }
}
