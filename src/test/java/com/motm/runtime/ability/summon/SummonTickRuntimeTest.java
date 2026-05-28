package com.motm.runtime.ability.summon;

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

class SummonTickRuntimeTest {
    private static final Gson GSON = new Gson();
    private final SummonTickRuntime runtime = new SummonTickRuntime();

    @Test
    void removesInvalidSummonWithoutCallingHooks() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveSummon summon = summon(new InvalidRef(), new TestRef(), "snow_imp", 0L, 10_000L, 0L, 0L, true);

        assertTrue(runtime.process(summon, 1_000L, hooks));
        assertEquals(List.of(), hooks.events);
    }

    @Test
    void despawnsExpiredSummon() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveSummon summon = summon(new TestRef(), new TestRef(), "snow_imp", 0L, 1_000L, 0L, 0L, true);

        assertTrue(runtime.process(summon, 1_000L, hooks));
        assertEquals(List.of("despawn"), hooks.events);
    }

    @Test
    void waitsUntilNextThinkWithoutStoreLookup() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveSummon summon = summon(new TestRef(), new TestRef(), "snow_imp", 0L, 10_000L, 2_000L, 0L, true);

        assertFalse(runtime.process(summon, 1_000L, hooks));
        assertEquals(List.of(), hooks.events);
    }

    @Test
    void removesWhenStoreMissingOrOwnerUnavailable() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.storeAvailable = false;
        ActiveSummon summon = summon(new TestRef(), new TestRef(), "snow_imp", 0L, 10_000L, 0L, 0L, true);

        assertTrue(runtime.process(summon, 1_000L, hooks));
        assertEquals(List.of("store"), hooks.events);

        hooks = new RecordingHooks();
        hooks.ownerAvailable = false;
        assertTrue(runtime.process(summon, 1_000L, hooks));
        assertEquals(List.of("store", "owner", "despawn"), hooks.events);
    }

    @Test
    void hatchlingSchedulesThinkBeforeAwakening() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveSummon summon = summon(new TestRef(), new TestRef(), "scarak_egg", 3_000L, 10_000L, 0L, 0L, false);

        assertFalse(runtime.process(summon, 1_000L, hooks));
        assertEquals(1_000L + SummonRuntimeSpecs.THINK_INTERVAL_MS, summon.nextThinkAtMillis());
        assertEquals(List.of("store", "owner"), hooks.events);
    }

    @Test
    void awakensThenMovesTowardOwnerWhenNoTargetExists() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.target = null;
        ActiveSummon summon = summon(new TestRef(), new TestRef(), "snow_imp", 0L, 10_000L, 0L, 0L, false);

        assertFalse(runtime.process(summon, 1_000L, hooks));

        assertTrue(summon.awakened());
        assertEquals(0L, summon.targetLockExpireAtMillis());
        assertEquals(1_000L + SummonRuntimeSpecs.THINK_INTERVAL_MS, summon.nextThinkAtMillis());
        assertEquals(List.of("store", "owner", "awaken", "target", "moveOwner"), hooks.events);
    }

    @Test
    void despawnsWhenOwnerRefInvalidAfterAwakening() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveSummon summon = summon(new TestRef(), new InvalidRef(), "snow_imp", 0L, 10_000L, 0L, 0L, false);

        assertTrue(runtime.process(summon, 1_000L, hooks));
        assertEquals(List.of("store", "owner", "awaken", "despawn"), hooks.events);
    }

    @Test
    void movesTowardTargetWhenOutsideAttackRange() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.summonPosition = new Vector3d(0.0, 0.0, 0.0);
        hooks.targetPosition = new Vector3d(20.0, 0.0, 0.0);
        ActiveSummon summon = summon(new TestRef(), new TestRef(), "snow_imp", 0L, 10_000L, 0L, 0L, true);

        assertFalse(runtime.process(summon, 1_000L, hooks));

        assertEquals(1_000L + SummonRuntimeSpecs.THINK_INTERVAL_MS, summon.nextThinkAtMillis());
        assertEquals(List.of("store", "owner", "target", "summonPosition", "targetPosition", "moveTarget"),
                hooks.events);
    }

    @Test
    void rangedSummonRetreatsAndAttacksWhenTooCloseAndReady() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.summonPosition = new Vector3d(0.0, 0.0, 0.0);
        hooks.targetPosition = new Vector3d(1.0, 0.0, 0.0);
        ActiveSummon summon = summon(new TestRef(), new TestRef(), "snow_imp", 0L, 10_000L, 0L, 500L, true);

        assertFalse(runtime.process(summon, 1_000L, hooks));

        assertEquals(1_000L + SummonRuntimeSpecs.THINK_INTERVAL_MS, summon.nextThinkAtMillis());
        assertEquals(List.of("store", "owner", "target", "summonPosition", "targetPosition", "moveAway", "attack"),
                hooks.events);
    }

    @Test
    void skipsAttackWhenTargetPositionIsMissing() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.targetPosition = null;
        ActiveSummon summon = summon(new TestRef(), new TestRef(), "snow_imp", 0L, 10_000L, 0L, 0L, true);

        assertFalse(runtime.process(summon, 1_000L, hooks));

        assertEquals(1_000L + SummonRuntimeSpecs.THINK_INTERVAL_MS, summon.nextThinkAtMillis());
        assertEquals(List.of("store", "owner", "target", "summonPosition", "targetPosition"), hooks.events);
    }

    private static ActiveSummon summon(Ref<EntityStore> summonRef,
                                       Ref<EntityStore> ownerRef,
                                       String summonName,
                                       long hatchAtMillis,
                                       long expireAtMillis,
                                       long nextThinkAtMillis,
                                       long nextAttackAtMillis,
                                       boolean awakened) {
        AbilityData ability = ability(summonName);
        return new ActiveSummon(
                "player",
                summonRef,
                ownerRef,
                "hydro",
                "snow",
                ability,
                SummonRuntimeSpecs.resolve(ability),
                hatchAtMillis,
                expireAtMillis,
                nextThinkAtMillis,
                nextAttackAtMillis,
                0L,
                12.0,
                null,
                0L,
                awakened
        );
    }

    private static AbilityData ability(String summonName) {
        return GSON.fromJson("""
                {
                  "id": "%s",
                  "cast_type": "summon",
                  "summon_name": "%s"
                }
                """.formatted(summonName, summonName), AbilityData.class);
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

    private static final class RecordingHooks implements SummonTickRuntime.Hooks {
        private final List<String> events = new ArrayList<>();
        private boolean storeAvailable = true;
        private boolean ownerAvailable = true;
        private Ref<EntityStore> target = new TestRef();
        private Vector3d summonPosition = new Vector3d(0.0, 0.0, 0.0);
        private Vector3d targetPosition = new Vector3d(3.0, 0.0, 0.0);

        @Override
        public boolean despawn(ActiveSummon summon) {
            events.add("despawn");
            return true;
        }

        @Override
        public boolean hasStore(ActiveSummon summon) {
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
        public void awaken(ActiveSummon summon, long now) {
            events.add("awaken");
            summon.awaken(now);
        }

        @Override
        public Ref<EntityStore> resolveTarget(ActiveSummon summon, long now) {
            events.add("target");
            return target;
        }

        @Override
        public void moveTowardOwner(ActiveSummon summon) {
            events.add("moveOwner");
        }

        @Override
        public Vector3d position(Ref<EntityStore> ref) {
            if (ref == target) {
                events.add("targetPosition");
                return targetPosition;
            }
            events.add("summonPosition");
            return summonPosition;
        }

        @Override
        public void moveTowardTarget(ActiveSummon summon, Ref<EntityStore> targetRef, double desiredRange) {
            events.add("moveTarget");
        }

        @Override
        public void moveAwayFromTarget(ActiveSummon summon, Ref<EntityStore> targetRef, double desiredDistance) {
            events.add("moveAway");
        }

        @Override
        public void attack(ActiveSummon summon, PlayerData owner, Ref<EntityStore> targetRef, long now) {
            events.add("attack");
        }
    }
}
