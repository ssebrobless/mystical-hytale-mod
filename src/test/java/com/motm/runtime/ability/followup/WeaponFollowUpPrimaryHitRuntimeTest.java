package com.motm.runtime.ability.followup;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeaponFollowUpPrimaryHitRuntimeTest {

    private static final Gson GSON = new Gson();

    @Test
    void appliesPrimaryHitInRuntimeOwnedOrder() {
        ActiveWeaponFollowUp refraction = followUp("""
                {
                  "id": "refraction",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """);
        List<String> calls = new ArrayList<>();

        WeaponFollowUpPrimaryHitRuntime.applyPrimaryHit(refraction, 42.0, hooks(calls));

        assertEquals(List.of(
                "damage:42.0",
                "post:42.0",
                "lifesteal:42.0",
                "impact",
                "rider:vulnerability",
                "secondary:slow"
        ), calls);
    }

    @Test
    void skipsDamageHooksWhenResolvedDamageIsNotPositiveButStillAppliesImpact() {
        List<String> calls = new ArrayList<>();

        WeaponFollowUpPrimaryHitRuntime.applyPrimaryHit(null, 0.0, hooks(calls));

        assertEquals(List.of("impact"), calls);
    }

    @Test
    void ignoresMissingHooksSafely() {
        WeaponFollowUpPrimaryHitRuntime.applyPrimaryHit(followUp("""
                {
                  "id": "refraction",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """), 42.0, null);
    }

    private static WeaponFollowUpPrimaryHitRuntime.PrimaryHitHooks hooks(List<String> calls) {
        return new WeaponFollowUpPrimaryHitRuntime.PrimaryHitHooks() {
            @Override
            public void applyDamage(double damage) {
                calls.add("damage:" + damage);
            }

            @Override
            public void applyPostDamage(double damage) {
                calls.add("post:" + damage);
            }

            @Override
            public void applyLifesteal(double damage) {
                calls.add("lifesteal:" + damage);
            }

            @Override
            public void applyImpact() {
                calls.add("impact");
            }

            @Override
            public void applyRiderToken(String token) {
                calls.add("rider:" + token);
            }

            @Override
            public void applySecondaryRiderToken(String token) {
                calls.add("secondary:" + token);
            }
        };
    }

    private static ActiveWeaponFollowUp followUp(String json) {
        AbilityData ability = GSON.fromJson(json, AbilityData.class);
        return ActiveWeaponFollowUp.create("player", ability, 1000L, WeaponFollowUpSpecs.resolve(ability));
    }
}
