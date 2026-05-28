package com.motm.runtime.ability.followup;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeaponFollowUpSplashRuntimeTest {

    private static final Gson GSON = new Gson();

    @Test
    void appliesSplashTargetsInRuntimeOwnedOrder() {
        ActiveWeaponFollowUp refraction = followUp("""
                {
                  "id": "refraction",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """);
        List<String> calls = new ArrayList<>();

        WeaponFollowUpSplashRuntime.applySplash(refraction, 100.0, new WeaponFollowUpSplashRuntime.SplashHooks<String>() {
            @Override
            public Iterable<String> collectTargets(double radius, int maxTargets) {
                calls.add("collect:" + radius + ":" + maxTargets);
                return List.of("primary", "invalid", "shielded", "open");
            }

            @Override
            public boolean isValidTarget(String target) {
                return !"invalid".equals(target);
            }

            @Override
            public boolean isPrimaryTarget(String target) {
                return "primary".equals(target);
            }

            @Override
            public String entityId(String target) {
                return target;
            }

            @Override
            public double incomingMultiplier(String entityId) {
                return "shielded".equals(entityId) ? 0.5 : 1.0;
            }

            @Override
            public double absorbDamage(String entityId, double damage) {
                calls.add("absorb:" + entityId + ":" + format(damage));
                return "shielded".equals(entityId) ? 0.0 : damage;
            }

            @Override
            public void applyDamage(String target, double damage) {
                calls.add("damage:" + target + ":" + format(damage));
            }

            @Override
            public void applyPostDamage(String target, String entityId, double damage) {
                calls.add("post:" + entityId + ":" + format(damage));
            }

            @Override
            public void applySecondaryRiderToken(String target, String token) {
                calls.add("rider:" + target + ":" + token);
            }

            @Override
            public void applyImpact(String target) {
                calls.add("impact:" + target);
            }
        });

        assertEquals(List.of(
                "collect:4.5:4",
                "absorb:shielded:27.50",
                "absorb:open:55.00",
                "damage:open:55.00",
                "post:open:55.00",
                "rider:open:slow",
                "impact:open"
        ), calls);
    }

    @Test
    void ignoresMissingSplashInputs() {
        List<String> calls = new ArrayList<>();

        WeaponFollowUpSplashRuntime.applySplash(null, 100.0, collectingHooks(calls));
        WeaponFollowUpSplashRuntime.applySplash(followUp("""
                {
                  "id": "alloy_enhancement",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """), 100.0, collectingHooks(calls));
        WeaponFollowUpSplashRuntime.applySplash(followUp("""
                {
                  "id": "refraction",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """), 0.0, collectingHooks(calls));
        WeaponFollowUpSplashRuntime.applySplash(followUp("""
                {
                  "id": "refraction",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """), 100.0, null);

        assertEquals(List.of(), calls);
    }

    private static WeaponFollowUpSplashRuntime.SplashHooks<String> collectingHooks(List<String> calls) {
        return new WeaponFollowUpSplashRuntime.SplashHooks<>() {
            @Override
            public Iterable<String> collectTargets(double radius, int maxTargets) {
                calls.add("collect");
                return List.of("target");
            }

            @Override
            public boolean isValidTarget(String target) {
                return true;
            }

            @Override
            public boolean isPrimaryTarget(String target) {
                return false;
            }

            @Override
            public String entityId(String target) {
                return target;
            }

            @Override
            public double incomingMultiplier(String entityId) {
                return 1.0;
            }

            @Override
            public double absorbDamage(String entityId, double damage) {
                return damage;
            }

            @Override
            public void applyDamage(String target, double damage) {
            }

            @Override
            public void applyPostDamage(String target, String entityId, double damage) {
            }

            @Override
            public void applySecondaryRiderToken(String target, String token) {
            }

            @Override
            public void applyImpact(String target) {
            }
        };
    }

    private static ActiveWeaponFollowUp followUp(String json) {
        AbilityData ability = GSON.fromJson(json, AbilityData.class);
        return ActiveWeaponFollowUp.create("player", ability, 1000L, WeaponFollowUpSpecs.resolve(ability));
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
