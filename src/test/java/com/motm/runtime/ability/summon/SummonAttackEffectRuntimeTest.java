package com.motm.runtime.ability.summon;

import com.google.gson.Gson;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SummonAttackEffectRuntimeTest {
    private static final Gson GSON = new Gson();

    private final SummonAttackEffectRuntime runtime = new SummonAttackEffectRuntime();

    @Test
    void appliesTankTokenShieldAndPull() {
        RecordingHooks hooks = new RecordingHooks();

        runtime.applyAttackEffects(summon("frosty_golem", false, 0L), "target", 1_000L, hooks);

        assertEquals(List.of(
                "token:target:root",
                "summonShield:4.00",
                "pull:target:1.00:0.55:0.00",
                "splashToken:target:slow:3.40:2",
                "splashToken:target:root:2.00:1"
        ), hooks.events);
    }

    @Test
    void appliesSnowImpAndSkeletonSpecials() {
        RecordingHooks hooks = new RecordingHooks();

        runtime.applyAttackEffects(summon("snow_imp", false, 0L), "snow-target", 1_000L, hooks);
        runtime.applyAttackEffects(summon("skeleton_minion", false, 0L), "skeleton-target", 1_000L, hooks);

        assertEquals(List.of(
                "token:snow-target:slow",
                "token:snow-target:attack_slow",
                "splashToken:snow-target:slow:2.60:1",
                "token:skeleton-target:slow",
                "token:skeleton-target:dot"
        ), hooks.events);
    }

    @Test
    void appliesOwnerShieldAndSplashDamageSpecials() {
        RecordingHooks hooks = new RecordingHooks();

        runtime.applyAttackEffects(summon("treant_sapling", false, 0L), "treant-target", 1_000L, hooks);
        runtime.applyAttackEffects(summon("void_spawn", false, 0L), "void-target", 1_000L, hooks);

        assertEquals(List.of(
                "token:treant-target:root",
                "splashToken:treant-target:root:2.80:2",
                "ownerShield:4.50",
                "token:void-target:vulnerability",
                "splashToken:void-target:vulnerability:3.60:2",
                "splashDamage:void-target:0.35:3.40:2"
        ), hooks.events);
    }

    @Test
    void appliesBuffWindowAndAwakenedHatchlingEffects() {
        RecordingHooks hooks = new RecordingHooks();

        runtime.applyAttackEffects(summon("locust_queen", false, 5_000L), "locust-target", 1_000L, hooks);
        runtime.applyAttackEffects(summon("scarak_egg", true, 5_000L), "egg-target", 1_000L, hooks);

        assertEquals(List.of(
                "token:locust-target:dot",
                "token:locust-target:dot",
                "splashToken:locust-target:dot:3.80:3",
                "splashToken:locust-target:vulnerability:3.80:2",
                "token:egg-target:dot",
                "token:egg-target:dot",
                "token:egg-target:vulnerability",
                "splashToken:egg-target:dot:2.80:2"
        ), hooks.events);
    }

    @Test
    void appliesCloneBlindAndSwampSplash() {
        RecordingHooks hooks = new RecordingHooks();

        runtime.applyAttackEffects(summon("shadow_clone", false, 0L), "clone-target", 1_000L, hooks);
        runtime.applyAttackEffects(summon("swamp_monster", false, 0L), "swamp-target", 1_000L, hooks);

        assertEquals(List.of(
                "token:clone-target:vulnerability",
                "token:clone-target:blind",
                "token:swamp-target:root",
                "splashToken:swamp-target:dot:3.20:2",
                "splashToken:swamp-target:slow:3.20:2"
        ), hooks.events);
    }

    private static ActiveSummon summon(String summonName, boolean awakened, long buffExpireAtMillis) {
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
                10_000L,
                0L,
                0L,
                buffExpireAtMillis,
                12.0,
                null,
                0L,
                awakened
        );
    }

    private static String decimal(double value) {
        return String.format(Locale.US, "%.2f", value);
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

    private static final class RecordingHooks implements SummonAttackEffectRuntime.Hooks<String> {
        private final List<String> events = new ArrayList<>();

        @Override
        public void applyToken(String target, String token) {
            events.add("token:" + target + ":" + token);
        }

        @Override
        public void applySplashToken(String primaryTarget, String token, double radius, int maxTargets) {
            events.add("splashToken:" + primaryTarget + ":" + token + ":" + decimal(radius) + ":" + maxTargets);
        }

        @Override
        public void applySplashDamage(String primaryTarget, double damageRatio, double radius, int maxTargets) {
            events.add("splashDamage:" + primaryTarget + ":" + decimal(damageRatio) + ":" + decimal(radius) + ":" + maxTargets);
        }

        @Override
        public void applySummonShield(double shieldPercent) {
            events.add("summonShield:" + decimal(shieldPercent));
        }

        @Override
        public void applyOwnerShield(double shieldPercent) {
            events.add("ownerShield:" + decimal(shieldPercent));
        }

        @Override
        public void pullTargetTowardSummon(String target, double pullForce, double liftForce, double maxY) {
            events.add("pull:" + target + ":" + decimal(pullForce) + ":" + decimal(liftForce) + ":" + decimal(maxY));
        }
    }
}
