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

class SummonBuffRuntimeTest {
    private static final Gson GSON = new Gson();

    private final SummonBuffRuntime runtime = new SummonBuffRuntime();

    @Test
    void reportsNoActiveSummonsBeforeCallingHooks() {
        RecordingHooks hooks = new RecordingHooks();

        SummonBuffRuntime.Result result = runtime.apply(List.of(), buffAbility(0.0, 4.0), new Vector3d(), 1_000L, hooks);

        assertEquals(new SummonBuffRuntime.Result(0, 0, "no active summons"), result);
        assertEquals(List.of(), hooks.events);
    }

    @Test
    void buffsInRangeSummonsAndCommandsAwakenedStrike() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveSummon close = summon("snow_imp", new TestRef(), true, 0L, 10_000L, 3_000L);
        ActiveSummon far = summon("snow_imp", new TestRef(), true, 0L, 10_000L, 3_000L);
        hooks.positions.put(close, new Vector3d(2.0, 0.0, 0.0));
        hooks.positions.put(far, new Vector3d(20.0, 0.0, 0.0));

        SummonBuffRuntime.Result result = runtime.apply(
                List.of(close, far),
                buffAbility(5.0, 3.0),
                new Vector3d(0.0, 0.0, 0.0),
                1_000L,
                hooks
        );

        assertEquals(new SummonBuffRuntime.Result(1, 1, "buffed 1 summon | commanded 1 strike"), result);
        assertEquals(13_000L, close.expireAtMillis());
        assertEquals(4_000L, close.buffExpireAtMillis());
        assertEquals(1_150L, close.nextAttackAtMillis());
        assertEquals(List.of(
                "position:snow_imp",
                "visual:snow_imp",
                "target:snow_imp",
                "attack:snow_imp:1000",
                "position:snow_imp"
        ), hooks.events);
    }

    @Test
    void skipsCommandForUnhatchedSummonButStillBuffsIt() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveSummon hatchling = summon("scarak_egg", new TestRef(), false, 5_000L, 10_000L, 8_000L);
        hooks.positions.put(hatchling, new Vector3d(1.0, 0.0, 0.0));

        SummonBuffRuntime.Result result = runtime.apply(
                List.of(hatchling),
                buffAbility(0.0, 1.0),
                new Vector3d(0.0, 0.0, 0.0),
                1_000L,
                hooks
        );

        assertEquals(new SummonBuffRuntime.Result(1, 0, "buffed 1 summon"), result);
        assertEquals(12_000L, hatchling.expireAtMillis());
        assertEquals(3_000L, hatchling.buffExpireAtMillis());
        assertEquals(1_150L, hatchling.nextAttackAtMillis());
        assertEquals(List.of("position:scarak_egg", "visual:scarak_egg"), hooks.events);
    }

    @Test
    void reportsNoSummonsInRangeForInvalidMissingOrDistantSummons() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveSummon invalid = summon("snow_imp", new InvalidRef(), true, 0L, 10_000L, 3_000L);
        ActiveSummon missingPosition = summon("snow_imp", new TestRef(), true, 0L, 10_000L, 3_000L);
        ActiveSummon distant = summon("snow_imp", new TestRef(), true, 0L, 10_000L, 3_000L);
        hooks.positions.put(distant, new Vector3d(30.0, 0.0, 0.0));

        SummonBuffRuntime.Result result = runtime.apply(
                List.of(invalid, missingPosition, distant),
                buffAbility(4.0, 2.0),
                new Vector3d(0.0, 0.0, 0.0),
                1_000L,
                hooks
        );

        assertEquals(new SummonBuffRuntime.Result(0, 0, "no summons in range"), result);
        assertEquals(List.of("position:snow_imp", "position:snow_imp"), hooks.events);
    }

    @Test
    void doesNotCommandWhenResolvedTargetIsInvalid() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.target = new InvalidRef();
        ActiveSummon summon = summon("snow_imp", new TestRef(), true, 0L, 10_000L, 3_000L);
        hooks.positions.put(summon, new Vector3d(1.0, 0.0, 0.0));

        SummonBuffRuntime.Result result = runtime.apply(
                List.of(summon),
                buffAbility(4.0, 2.0),
                new Vector3d(0.0, 0.0, 0.0),
                1_000L,
                hooks
        );

        assertEquals(new SummonBuffRuntime.Result(1, 0, "buffed 1 summon"), result);
        assertEquals(List.of("position:snow_imp", "visual:snow_imp", "target:snow_imp"), hooks.events);
    }

    private static AbilityData buffAbility(double radius, double durationSeconds) {
        return GSON.fromJson(String.format(Locale.US, """
                {
                  "id": "summon_command",
                  "cast_type": "summon_buff",
                  "radius": %.1f,
                  "duration_seconds": %.1f
                }
                """, radius, durationSeconds), AbilityData.class);
    }

    private static ActiveSummon summon(String summonName,
                                       Ref<EntityStore> ref,
                                       boolean awakened,
                                       long hatchAtMillis,
                                       long expireAtMillis,
                                       long nextAttackAtMillis) {
        AbilityData ability = GSON.fromJson("""
                {
                  "id": "%s",
                  "cast_type": "summon",
                  "summon_name": "%s"
                }
                """.formatted(summonName, summonName), AbilityData.class);
        return new ActiveSummon(
                "player",
                ref,
                new TestRef(),
                "hydro",
                "snow",
                ability,
                SummonRuntimeSpecs.resolve(ability),
                hatchAtMillis,
                expireAtMillis,
                0L,
                nextAttackAtMillis,
                0L,
                12.0,
                null,
                0L,
                awakened
        );
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

    private static final class RecordingHooks implements SummonBuffRuntime.Hooks {
        private final List<String> events = new ArrayList<>();
        private final java.util.Map<ActiveSummon, Vector3d> positions = new java.util.HashMap<>();
        private Ref<EntityStore> target = new TestRef();

        @Override
        public Vector3d position(ActiveSummon summon) {
            events.add("position:" + summon.ability().getSummonName());
            return positions.get(summon);
        }

        @Override
        public void applyBuffVisual(ActiveSummon summon) {
            events.add("visual:" + summon.ability().getSummonName());
        }

        @Override
        public Ref<EntityStore> resolveTarget(ActiveSummon summon, long now) {
            events.add("target:" + summon.ability().getSummonName());
            return target;
        }

        @Override
        public void attack(ActiveSummon summon, Ref<EntityStore> targetRef, long now) {
            events.add("attack:" + summon.ability().getSummonName() + ":" + now);
        }
    }
}
