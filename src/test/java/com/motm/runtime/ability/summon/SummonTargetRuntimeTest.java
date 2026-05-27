package com.motm.runtime.ability.summon;

import com.google.gson.Gson;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SummonTargetRuntimeTest {
    private static final Gson GSON = new Gson();

    private final SummonTargetRuntime runtime = new SummonTargetRuntime();

    @Test
    void reusesValidLockedTargetWithoutSearching() {
        RecordingHooks hooks = new RecordingHooks();
        TestRef locked = new TestRef("locked");
        ActiveSummon summon = summon("snow_imp", new TestRef("summon"), new TestRef("owner"), locked, 2_500L);

        Ref<EntityStore> result = runtime.resolveTarget(summon, 1_000L, hooks);

        assertSame(locked, result);
        assertEquals(List.of(
                "position:summon",
                "position:owner",
                "valid:locked:14.00"
        ), hooks.events);
    }

    @Test
    void searchesDefaultFromSummonAnchorAndUpdatesLock() {
        RecordingHooks hooks = new RecordingHooks();
        TestRef found = new TestRef("found");
        hooks.nearest = found;
        ActiveSummon summon = summon("snow_imp", new TestRef("summon"), new TestRef("owner"), new TestRef("old"), 500L);

        Ref<EntityStore> result = runtime.resolveTarget(summon, 1_000L, hooks);

        assertSame(found, result);
        assertSame(found, summon.currentTargetRef());
        assertEquals(2_400L, summon.targetLockExpireAtMillis());
        assertEquals(List.of(
                "position:summon",
                "position:owner",
                "find:summon-pos:12.00"
        ), hooks.events);
    }

    @Test
    void tankAndCloneSearchFromOwnerAnchorWithRoleRanges() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.nearest = new TestRef("found");

        runtime.resolveTarget(summon("frosty_golem", new TestRef("summon"), new TestRef("owner"), null, 0L), 1_000L, hooks);
        runtime.resolveTarget(summon("shadow_clone", new TestRef("summon"), new TestRef("owner"), null, 0L), 1_000L, hooks);

        assertEquals(List.of(
                "position:summon",
                "position:owner",
                "find:owner-pos:12.00",
                "position:summon",
                "position:owner",
                "find:owner-pos:10.50"
        ), hooks.events);
    }

    @Test
    void fallsBackToOwnerPositionWhenSummonPositionMissing() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.summonPosition = null;
        hooks.nearest = new TestRef("found");

        runtime.resolveTarget(summon("snow_imp", new TestRef("summon"), new TestRef("owner"), null, 0L), 1_000L, hooks);

        assertEquals(List.of(
                "position:summon",
                "position:owner",
                "find:owner-pos:12.00"
        ), hooks.events);
    }

    @Test
    void clearsLockWhenNoTargetIsFound() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.nearest = null;
        ActiveSummon summon = summon("snow_imp", new TestRef("summon"), new TestRef("owner"), new TestRef("old"), 500L);

        Ref<EntityStore> result = runtime.resolveTarget(summon, 1_000L, hooks);

        assertEquals(null, result);
        assertEquals(null, summon.currentTargetRef());
        assertEquals(0L, summon.targetLockExpireAtMillis());
    }

    private static ActiveSummon summon(String summonName,
                                       Ref<EntityStore> summonRef,
                                       Ref<EntityStore> ownerRef,
                                       Ref<EntityStore> currentTargetRef,
                                       long targetLockExpireAtMillis) {
        AbilityData ability = GSON.fromJson("""
                {
                  "id": "%s",
                  "cast_type": "summon",
                  "summon_name": "%s"
                }
                """.formatted(summonName, summonName), AbilityData.class);
        return new ActiveSummon(
                "player",
                summonRef,
                ownerRef,
                "hydro",
                "snow",
                ability,
                SummonRuntimeSpecs.resolve(ability),
                0L,
                10_000L,
                0L,
                0L,
                0L,
                12.0,
                currentTargetRef,
                targetLockExpireAtMillis,
                true
        );
    }

    private static String ratio(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static final class TestRef extends Ref<EntityStore> {
        private final String name;

        private TestRef(String name) {
            super(null, 1);
            this.name = name;
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    private static final class RecordingHooks implements SummonTargetRuntime.Hooks {
        private final List<String> events = new ArrayList<>();
        private Vector3d summonPosition = new Vector3d(1.0, 0.0, 0.0);
        private Vector3d ownerPosition = new Vector3d(2.0, 0.0, 0.0);
        private Ref<EntityStore> nearest = new TestRef("nearest");

        @Override
        public Vector3d position(Ref<EntityStore> ref) {
            String name = ref instanceof TestRef testRef ? testRef.name : "unknown";
            events.add("position:" + name);
            if ("summon".equals(name)) {
                return summonPosition;
            }
            if ("owner".equals(name)) {
                return ownerPosition;
            }
            return null;
        }

        @Override
        public boolean isValidTarget(Ref<EntityStore> targetRef, Vector3d anchor, double radius) {
            String name = targetRef instanceof TestRef testRef ? testRef.name : "unknown";
            events.add("valid:" + name + ":" + ratio(radius));
            return true;
        }

        @Override
        public Ref<EntityStore> findNearest(Vector3d anchor, double radius) {
            String anchorName = anchor == ownerPosition ? "owner-pos" : "summon-pos";
            events.add("find:" + anchorName + ":" + ratio(radius));
            return nearest;
        }
    }
}
