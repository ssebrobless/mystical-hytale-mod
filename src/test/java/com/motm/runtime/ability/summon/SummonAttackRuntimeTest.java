package com.motm.runtime.ability.summon;

import com.google.gson.Gson;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummonAttackRuntimeTest {
    private static final Gson GSON = new Gson();

    private final SummonAttackRuntime runtime = new SummonAttackRuntime();

    @Test
    void ignoresMissingOrInvalidTargets() {
        RecordingHooks hooks = new RecordingHooks();

        assertFalse(runtime.performAttack(summon("snow_imp", false, 0L, 10_000L), null, 1_000L, hooks).attacked());
        assertFalse(runtime.performAttack(summon("snow_imp", false, 0L, 10_000L), new InvalidRef(), 1_000L, hooks).attacked());
        assertEquals(List.of(), hooks.events);
    }

    @Test
    void appliesDamagePipelineAndSchedulesNextAttack() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.targetEntityId = "npc-1";
        hooks.incomingMultiplier = 0.5;
        hooks.absorbFlat = 2.0;
        ActiveSummon summon = summon("snow_imp", false, 0L, 10_000L);

        SummonAttackRuntime.Result result = runtime.performAttack(summon, new TestRef(), 1_000L, hooks);

        assertTrue(result.attacked());
        assertEquals("npc-1", result.targetEntityId());
        assertEquals(4.0, result.resolvedDamage(), 0.0001);
        assertEquals(2_850L, summon.nextAttackAtMillis());
        assertEquals(List.of(
                "entity",
                "incoming:npc-1",
                "absorb:npc-1:6.0",
                "damage:4.0:false",
                "post:npc-1:4.0",
                "lifesteal:4.0",
                "impact",
                "effects:1000",
                "log:npc-1:4.0"
        ), hooks.events);
    }

    @Test
    void appliesBuffedDamageAndSkipsDamageCallbacksWhenAbsorbed() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.targetEntityId = "npc-1";
        hooks.absorbFlat = 100.0;
        ActiveSummon summon = summon("snow_imp", false, 5_000L, 10_000L);

        SummonAttackRuntime.Result result = runtime.performAttack(summon, new TestRef(), 1_000L, hooks);

        assertEquals(-83.8, result.resolvedDamage(), 0.0001);
        assertEquals(List.of(
                "entity",
                "incoming:npc-1",
                "absorb:npc-1:16.200000000000003",
                "impact",
                "effects:1000",
                "log:npc-1:-83.8"
        ), hooks.events);
    }

    @Test
    void movesCloneBeforeDamageAndExpiresAfterStrike() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.targetEntityId = null;
        ActiveSummon clone = summon("shadow_clone", true, 0L, 10_000L);

        runtime.performAttack(clone, new TestRef(), 2_000L, hooks);

        assertEquals(2_150L, clone.expireAtMillis());
        assertEquals(List.of(
                "moveClone",
                "entity",
                "damage:12.0:true",
                "post:null:12.0",
                "lifesteal:12.0",
                "impact",
                "effects:2000",
                "log:null:12.0"
        ), hooks.events);
    }

    private static ActiveSummon summon(String summonName,
                                       boolean awakened,
                                       long buffExpireAtMillis,
                                       long expireAtMillis) {
        AbilityData ability = GSON.fromJson("""
                {
                  "id": "%s",
                  "cast_type": "summon",
                  "summon_name": "%s"
                }
                """.formatted(summonName, summonName), AbilityData.class);
        return new ActiveSummon(
                "player",
                new TestRef(),
                new TestRef(),
                "hydro",
                "snow",
                ability,
                SummonRuntimeSpecs.resolve(ability),
                0L,
                expireAtMillis,
                0L,
                0L,
                buffExpireAtMillis,
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

    private static final class RecordingHooks implements SummonAttackRuntime.Hooks {
        private final List<String> events = new ArrayList<>();
        private String targetEntityId = "npc";
        private double incomingMultiplier = 1.0;
        private double absorbFlat;

        @Override
        public void moveCloneBesideTarget(Ref<EntityStore> targetRef) {
            events.add("moveClone");
        }

        @Override
        public String targetEntityId(Ref<EntityStore> targetRef) {
            events.add("entity");
            return targetEntityId;
        }

        @Override
        public double incomingDamageMultiplier(String targetEntityId) {
            events.add("incoming:" + targetEntityId);
            return incomingMultiplier;
        }

        @Override
        public double absorbDamage(String targetEntityId, double damage) {
            events.add("absorb:" + targetEntityId + ":" + damage);
            return damage - absorbFlat;
        }

        @Override
        public void applyDamage(Ref<EntityStore> targetRef, double damage, boolean ranged) {
            events.add("damage:" + damage + ":" + ranged);
        }

        @Override
        public void applyPostDamage(Ref<EntityStore> targetRef, String targetEntityId, double damage) {
            events.add("post:" + targetEntityId + ":" + damage);
        }

        @Override
        public void applyLifesteal(double damage) {
            events.add("lifesteal:" + damage);
        }

        @Override
        public void applyImpact(Ref<EntityStore> targetRef) {
            events.add("impact");
        }

        @Override
        public void applyAttackEffects(Ref<EntityStore> targetRef, long now) {
            events.add("effects:" + now);
        }

        @Override
        public void logResolved(String targetEntityId, double resolvedDamage) {
            events.add("log:" + targetEntityId + ":" + resolvedDamage);
        }
    }
}
