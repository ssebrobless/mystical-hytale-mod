package com.motm.runtime.ability.field;

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

class FieldTickRuntimeTest {
    private static final Gson GSON = new Gson();
    private final FieldTickRuntime runtime = new FieldTickRuntime();

    @Test
    void removesInvalidOwnerAndCleansVisualsWithoutStoreCallbacks() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveField field = field(new InvalidRef(), 1000L, 5000L, 1000L);

        boolean remove = runtime.process(field, 1200L, hooks);

        assertTrue(remove);
        assertEquals(List.of("release", "despawn"), hooks.events);
    }

    @Test
    void removesExpiredFieldAfterTerrainAndSinkholeCleanup() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveField field = field(new TestRef(), 1000L, 1200L, 1000L);

        boolean remove = runtime.process(field, 1300L, hooks);

        assertTrue(remove);
        assertEquals(List.of("store", "anchor", "clearMobility", "restoreTerrain", "release", "despawn"), hooks.events);
    }

    @Test
    void refreshesVisualBeforeActivationWithoutPulsing() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveField field = field(new TestRef(), 2000L, 5000L, 2000L);

        boolean remove = runtime.process(field, 1200L, hooks);

        assertFalse(remove);
        assertEquals(List.of("store", "anchor", "ownerMobility", "refresh"), hooks.events);
    }

    @Test
    void syncsActiveFieldAndWaitsUntilNextPulse() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveField field = field(new TestRef(), 1000L, 5000L, 2000L);

        boolean remove = runtime.process(field, 1200L, hooks);

        assertFalse(remove);
        assertEquals(List.of("store", "anchor", "ownerMobility", "sync"), hooks.events);
    }

    @Test
    void appliesPulseSupportAndSinkholeSuffocationInOrder() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.sinkhole = true;
        hooks.targets = List.of(new TestRef());
        ActiveField field = field(new TestRef(), 1000L, 5000L, 1100L);

        boolean remove = runtime.process(field, 1200L, hooks);

        assertFalse(remove);
        assertEquals(List.of(
                "store",
                "anchor",
                "ownerMobility",
                "engage",
                "sync",
                "player",
                "collect",
                "pulse",
                "support",
                "suffocation"
        ), hooks.events);
        assertTrue(field.nextPulseAtMillis() > 1200L);
    }

    @Test
    void removesWhenOwnerPlayerIsUnavailableAtPulseTime() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.playerAvailable = false;
        ActiveField field = field(new TestRef(), 1000L, 5000L, 1100L);

        boolean remove = runtime.process(field, 1200L, hooks);

        assertTrue(remove);
        assertEquals(List.of("store", "anchor", "ownerMobility", "sync", "player", "release", "despawn"), hooks.events);
    }

    private static ActiveField field(Ref<EntityStore> ownerRef,
                                     long activateAtMillis,
                                     long expireAtMillis,
                                     long nextPulseAtMillis) {
        return new ActiveField(
                "player",
                ownerRef,
                "terra",
                "quake",
                ability(),
                new Vector3d(1.0, 2.0, 3.0),
                new Vector3d(0.0, 0.0, 1.0),
                new Vector3d(1.0, 0.0, 0.0),
                4.0,
                2.0,
                1.0,
                expireAtMillis,
                activateAtMillis,
                nextPulseAtMillis,
                false,
                List.of(),
                "loop",
                activateAtMillis,
                null
        );
    }

    private static AbilityData ability() {
        return GSON.fromJson("{\"id\":\"quake\",\"cast_type\":\"field\"}", AbilityData.class);
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

    private static final class RecordingHooks implements FieldTickRuntime.Hooks {
        private final List<String> events = new ArrayList<>();
        private boolean ownerStoreAvailable = true;
        private boolean playerAvailable = true;
        private boolean sinkhole;
        private List<Ref<EntityStore>> targets = List.of();

        @Override
        public boolean hasOwnerStore(ActiveField field) {
            events.add("store");
            return ownerStoreAvailable;
        }

        @Override
        public void releaseSinkhole(ActiveField field) {
            events.add("release");
        }

        @Override
        public void despawnVisual(ActiveField field) {
            events.add("despawn");
        }

        @Override
        public void syncFollowOwnerAnchor(ActiveField field) {
            events.add("anchor");
        }

        @Override
        public void clearOwnerMobility(ActiveField field) {
            events.add("clearMobility");
        }

        @Override
        public void restoreTemporaryTerrain(ActiveField field) {
            events.add("restoreTerrain");
        }

        @Override
        public void applyOwnerMobility(ActiveField field) {
            events.add("ownerMobility");
        }

        @Override
        public void refreshVisual(ActiveField field, long now) {
            events.add("refresh");
        }

        @Override
        public boolean isSinkhole(AbilityData ability) {
            return sinkhole;
        }

        @Override
        public void engageSinkhole(ActiveField field) {
            events.add("engage");
        }

        @Override
        public void syncVisual(ActiveField field, long now) {
            events.add("sync");
        }

        @Override
        public PlayerData player(String ownerPlayerId) {
            events.add("player");
            if (!playerAvailable) {
                return null;
            }
            PlayerData player = new PlayerData();
            player.setPlayerId(ownerPlayerId);
            return player;
        }

        @Override
        public List<Ref<EntityStore>> collectTargets(ActiveField field) {
            events.add("collect");
            return targets;
        }

        @Override
        public void applyPulse(ActiveField field, PlayerData player, List<Ref<EntityStore>> targets) {
            events.add("pulse");
        }

        @Override
        public void applySupportPulse(ActiveField field, PlayerData player) {
            events.add("support");
        }

        @Override
        public void applySinkholeSuffocationPulse(ActiveField field) {
            events.add("suffocation");
        }
    }
}
