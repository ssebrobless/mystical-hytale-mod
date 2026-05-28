package com.motm.runtime.ability.transformation;

import com.google.gson.Gson;
import com.hypixel.hytale.component.Ref;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransformationEffectRuntimeTest {
    private static final Gson GSON = new Gson();

    private final TransformationEffectRuntime runtime = new TransformationEffectRuntime();

    @Test
    void appliesKindSpecificPulsePlans() {
        RecordingHooks hooks = new RecordingHooks();

        runtime.applyPulse(form("smoke_form", 0.0), new Vector3d(1.0, 0.0, 1.0), hooks);
        runtime.applyPulse(form("pterodactyl_form", 0.0), new Vector3d(2.0, 0.0, 2.0), hooks);
        runtime.applyPulse(form("triceratops_form", 0.0), new Vector3d(3.0, 0.0, 3.0), hooks);
        runtime.applyPulse(form("t_rex_form", 6.0), new Vector3d(4.0, 0.0, 4.0), hooks);

        assertEquals(List.of(
                "nearest:3.40",
                "impact:nearest:0.30:blind:false",
                "nearby:5.50:2",
                "impact:near-1:0.34:slow:false",
                "impact:near-2:0.34:slow:false",
                "nearby:3.60:3",
                "impact:near-1:0.46:null:true",
                "impact:near-2:0.46:null:true",
                "nearby:6.00:4",
                "impact:near-1:0.58:vulnerability:false",
                "impact:near-2:0.58:vulnerability:false"
        ), hooks.events);
    }

    @Test
    void appliesLocomotionPressureAndUpdatesOwnerPosition() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveTransformation smoke = form("smoke_form", 0.0);

        runtime.applyLocomotionPressure(smoke, new Vector3d(2.0, 0.0, 0.0), hooks);

        assertEquals(List.of(
                "along:1.75:2",
                "impact:path-1:0.28:blind:false",
                "token:path-1:disoriented",
                "impact:path-2:0.28:blind:false",
                "token:path-2:disoriented"
        ), hooks.events);
        assertEquals(2.0, smoke.lastOwnerPosition().x, 0.0001);
    }

    @Test
    void skipsLocomotionWhenMovementIsBelowTrigger() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveTransformation form = form("pterodactyl_form", 0.0);

        runtime.applyLocomotionPressure(form, new Vector3d(0.5, 0.0, 0.0), hooks);

        assertEquals(List.of(), hooks.events);
        assertEquals(0.5, form.lastOwnerPosition().x, 0.0001);
    }

    @Test
    void appliesPterodactylAndTRexLocomotionPlans() {
        RecordingHooks hooks = new RecordingHooks();

        runtime.applyLocomotionPressure(form("pterodactyl_form", 0.0), new Vector3d(3.0, 0.0, 0.0), hooks);
        runtime.applyLocomotionPressure(form("t_rex_form", 0.0), new Vector3d(3.0, 0.0, 0.0), hooks);

        assertEquals(List.of(
                "along:2.10:3",
                "impact:path-1:0.39:slow:false",
                "token:path-1:vulnerability",
                "knockback:path-1",
                "impact:path-2:0.39:slow:false",
                "token:path-2:vulnerability",
                "knockback:path-2",
                "nearby:3.80:4",
                "impact:near-1:0.60:vulnerability:false",
                "token:near-1:disoriented",
                "impact:near-2:0.60:vulnerability:false",
                "token:near-2:disoriented"
        ), hooks.events);
    }

    @Test
    void triceratopsChargeAppliesOwnerShieldOnlyAfterAHit() {
        RecordingHooks hooks = new RecordingHooks();

        runtime.applyLocomotionPressure(form("triceratops_form", 0.0), new Vector3d(3.0, 0.0, 0.0), hooks);

        assertEquals(List.of(
                "along:2.45:4",
                "charge:path-1:0.63",
                "charge:path-2:0.63",
                "shield:2.50"
        ), hooks.events);
    }

    private static ActiveTransformation form(String abilityId, double radius) {
        return ActiveTransformation.create(
                "player",
                new TestRef(),
                ability(abilityId, radius),
                "model",
                10_000L,
                new Vector3d(0.0, 0.0, 0.0)
        );
    }

    private static AbilityData ability(String abilityId, double radius) {
        return GSON.fromJson(String.format(Locale.US, """
                {
                  "id": "%s",
                  "cast_type": "transformation",
                  "duration_seconds": 10.0,
                  "radius": %.1f
                }
                """, abilityId, radius), AbilityData.class);
    }

    private static String ratio(double value) {
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

    private static final class RecordingHooks implements TransformationEffectRuntime.Hooks<String> {
        private final List<String> events = new ArrayList<>();

        @Override
        public String findNearest(Vector3d origin, double radius) {
            events.add("nearest:" + ratio(radius));
            return "nearest";
        }

        @Override
        public Iterable<String> collectNearby(Vector3d origin, double radius, int maxTargets) {
            events.add("nearby:" + ratio(radius) + ":" + maxTargets);
            return List.of("near-1", "near-2");
        }

        @Override
        public Iterable<String> collectAlong(Vector3d from, Vector3d to, double radius, int maxTargets) {
            events.add("along:" + ratio(radius) + ":" + maxTargets);
            return List.of("path-1", "path-2");
        }

        @Override
        public void applyImpact(String target, double damageRatio, String token, boolean knockback) {
            events.add("impact:" + target + ":" + ratio(damageRatio) + ":" + token + ":" + knockback);
        }

        @Override
        public boolean applyChargeImpact(String target, double damageRatio) {
            events.add("charge:" + target + ":" + ratio(damageRatio));
            return true;
        }

        @Override
        public void applyToken(String target, String token) {
            events.add("token:" + target + ":" + token);
        }

        @Override
        public void applyKnockback(String target) {
            events.add("knockback:" + target);
        }

        @Override
        public void applyOwnerShield(double shieldPercent) {
            events.add("shield:" + ratio(shieldPercent));
        }
    }
}
