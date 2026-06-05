package com.motm.runtime.ability.summon;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SummonSplashRuntimeTest {
    private final SummonSplashRuntime runtime = new SummonSplashRuntime();

    @Test
    void appliesSplashTokensToNearbyTargetsExceptPrimaryAndNulls() {
        RecordingHooks hooks = new RecordingHooks(Arrays.asList("primary", null, "a", "b"), Map.of(), Map.of(), Map.of());

        runtime.applySplashToken("primary", "slow", 2.6, 2, hooks);

        assertEquals(List.of(
                "collect:primary:2.60:3",
                "token:a:slow",
                "token:b:slow"
        ), hooks.events);
    }

    @Test
    void appliesSplashDamageWithIncomingAbsorbPostDamageAndImpactOrdering() {
        ActiveSummon summon = summon(true, 20.0);
        RecordingHooks hooks = new RecordingHooks(
                List.of("primary", "shielded", "plain", "blocked", "unknown"),
                Map.of("shielded", "shielded-id", "plain", "plain-id", "blocked", "blocked-id"),
                Map.of("shielded-id", 0.5, "plain-id", 1.0, "blocked-id", 1.0),
                Map.of("shielded-id", 2.0, "blocked-id", 10.0)
        );

        runtime.applySplashDamage(summon, "primary", 0.5, 3.4, 3, hooks);

        assertEquals(List.of(
                "collect:primary:3.40:4",
                "id:shielded",
                "incoming:shielded-id",
                "absorb:shielded-id:5.00",
                "damage:shielded:3.00:true",
                "post:shielded:shielded-id:3.00",
                "impact:shielded",
                "id:plain",
                "incoming:plain-id",
                "absorb:plain-id:10.00",
                "damage:plain:10.00:true",
                "post:plain:plain-id:10.00",
                "impact:plain",
                "id:blocked",
                "incoming:blocked-id",
                "absorb:blocked-id:10.00",
                "id:unknown",
                "damage:unknown:10.00:true",
                "post:unknown:<null>:10.00",
                "impact:unknown"
        ), hooks.events);
    }

    @Test
    void skipsSplashDamageWhenDamageRatioIsNotPositive() {
        RecordingHooks hooks = new RecordingHooks(List.of("target"), Map.of(), Map.of(), Map.of());

        runtime.applySplashDamage(summon(false, 20.0), "primary", 0.0, 3.4, 3, hooks);

        assertEquals(List.of(), hooks.events);
    }

    @Test
    void treatsNullCollectedTargetsAsNoTargets() {
        RecordingHooks hooks = new RecordingHooks(null, Map.of(), Map.of(), Map.of());

        runtime.applySplashToken("primary", "slow", 2.6, 2, hooks);

        assertEquals(List.of("collect:primary:2.60:3"), hooks.events);
    }

    private static ActiveSummon summon(boolean ranged, double baseDamage) {
        SummonRuntimeSpec spec = new SummonRuntimeSpec(
                "test",
                ranged,
                3.0,
                8.0,
                1000L,
                0L,
                1.0,
                "slow",
                null,
                null
        );
        return new ActiveSummon(
                "player",
                null,
                null,
                "hydro",
                "snow",
                null,
                spec,
                0L,
                10_000L,
                0L,
                0L,
                0L,
                baseDamage,
                null,
                0L,
                false
        );
    }

    private static String decimal(double value) {
        return String.format(Locale.US, "%.2f", value);
    }

    private static final class RecordingHooks implements SummonSplashRuntime.TokenHooks<String>, SummonSplashRuntime.DamageHooks<String> {
        private final List<String> targets;
        private final Map<String, String> entityIds;
        private final Map<String, Double> incomingMultipliers;
        private final Map<String, Double> absorption;
        private final List<String> events = new ArrayList<>();

        private RecordingHooks(List<String> targets,
                               Map<String, String> entityIds,
                               Map<String, Double> incomingMultipliers,
                               Map<String, Double> absorption) {
            this.targets = targets;
            this.entityIds = entityIds;
            this.incomingMultipliers = incomingMultipliers;
            this.absorption = absorption;
        }

        @Override
        public List<String> collectNearbyTargets(String primaryTarget, double radius, int maxCandidates) {
            events.add("collect:" + primaryTarget + ":" + decimal(radius) + ":" + maxCandidates);
            return targets;
        }

        @Override
        public void applyToken(String target, String token) {
            events.add("token:" + target + ":" + token);
        }

        @Override
        public String targetEntityId(String target) {
            events.add("id:" + target);
            return entityIds.get(target);
        }

        @Override
        public double incomingDamageMultiplier(String targetEntityId) {
            events.add("incoming:" + targetEntityId);
            return incomingMultipliers.getOrDefault(targetEntityId, 1.0);
        }

        @Override
        public double absorbDamage(String targetEntityId, double damage) {
            events.add("absorb:" + targetEntityId + ":" + decimal(damage));
            return damage - absorption.getOrDefault(targetEntityId, 0.0);
        }

        @Override
        public void applyDamage(String target, double damageAmount, boolean ranged) {
            events.add("damage:" + target + ":" + decimal(damageAmount) + ":" + ranged);
        }

        @Override
        public void applyPostDamage(String target, String targetEntityId, double damage) {
            events.add("post:" + target + ":" + (targetEntityId == null ? "<null>" : targetEntityId) + ":" + decimal(damage));
        }

        @Override
        public void applyImpact(String target) {
            events.add("impact:" + target);
        }
    }
}
