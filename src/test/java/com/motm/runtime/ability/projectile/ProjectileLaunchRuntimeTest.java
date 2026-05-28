package com.motm.runtime.ability.projectile;

import com.google.gson.Gson;
import com.hypixel.hytale.component.Ref;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectileLaunchRuntimeTest {
    private static final Gson GSON = new Gson();

    private final ProjectileLaunchRuntime runtime = new ProjectileLaunchRuntime();

    @Test
    void createsDelayedSpreadProjectilesWithVisualsAndSummary() {
        RecordingHooks hooks = new RecordingHooks(true);
        Vector3d origin = new Vector3d(1.0, 2.0, 3.0);
        ProjectileLaunchRuntime.Result result = runtime.launch(
                "player",
                new TestRef(),
                "terra",
                "magma",
                ability(),
                "projectile_volley",
                spec(3, 10.0, List.of(0L, 50L, 100L), true),
                origin,
                new Vector3d(0.0, 0.0, 1.0),
                12.0,
                1_000L,
                20.0,
                "trace-1",
                hooks
        );

        assertEquals(3, result.launched());
        assertEquals("launched 3 projectiles at 40.0m/s | volley cadence", result.summary());
        assertEquals(List.of(
                "visual:1.0:2.0:3.0:1000:2500:true",
                "visual:1.0:2.0:3.0:1050:2550:true",
                "visual:1.0:2.0:3.0:1100:2600:true"
        ), hooks.events);

        List<ActiveProjectile> projectiles = result.projectiles();
        assertEquals(1_000L, projectiles.get(0).activateAtMillis());
        assertEquals(1_050L, projectiles.get(1).activateAtMillis());
        assertEquals(1_100L, projectiles.get(2).activateAtMillis());
        assertEquals(2_500L, projectiles.get(0).expireAtMillis());
        assertEquals(12.0, projectiles.get(0).baseDamage(), 0.0001);
        assertEquals("trace-1", projectiles.get(0).traceId());
        assertEquals("travel-1000", projectiles.get(0).travelEffectId());
        assertEquals(1_080L, projectiles.get(0).nextVisualRefreshAtMillis());
        assertEquals(0.1736, projectiles.get(0).direction().x, 0.0001);
        assertEquals(0.9848, projectiles.get(0).direction().z, 0.0001);
        assertEquals(0.0, projectiles.get(1).direction().x, 0.0001);
        assertEquals(1.0, projectiles.get(1).direction().z, 0.0001);
        assertEquals(-0.1736, projectiles.get(2).direction().x, 0.0001);
        assertEquals(0.9848, projectiles.get(2).direction().z, 0.0001);

        origin.x = 99.0;
        assertEquals(1.0, projectiles.get(0).position().x, 0.0001);
        assertTrue(projectiles.get(0).hitEntityIds().isEmpty());
    }

    @Test
    void usesProjectileDefaultsWhenVisualHookReturnsNull() {
        ProjectileLaunchRuntime.Result result = runtime.launch(
                "player",
                new TestRef(),
                "terra",
                "magma",
                ability(),
                "projectile_burst",
                spec(1, 0.0, List.of(0L), false),
                new Vector3d(0.0, 1.0, 0.0),
                new Vector3d(0.0, 0.0, 0.0),
                8.0,
                2_000L,
                20.0,
                null,
                (origin, activateAtMillis, expireAtMillis, hideIdentityComponents) -> null
        );

        assertEquals(1, result.launched());
        assertEquals("launched 1 projectile at 40.0m/s | burst spread", result.summary());
        ActiveProjectile projectile = result.projectiles().get(0);
        assertNull(projectile.visualRef());
        assertNull(projectile.travelEffectId());
        assertEquals(2_000L, projectile.nextVisualRefreshAtMillis());
        assertEquals(0.0, projectile.direction().x, 0.0001);
        assertEquals(0.0, projectile.direction().y, 0.0001);
        assertEquals(1.0, projectile.direction().z, 0.0001);
    }

    @Test
    void returnsNoneWhenRequiredInputsAreMissing() {
        ProjectileRuntimeSpec spec = spec(1, 0.0, List.of(0L), false);

        assertEquals(0, runtime.launch(null, new TestRef(), "terra", "magma", ability(), "projectile",
                spec, new Vector3d(0.0, 0.0, 0.0), new Vector3d(0.0, 0.0, 1.0), 1.0,
                0L, 20.0, null, (origin, activateAtMillis, expireAtMillis, hideIdentityComponents) -> null).launched());
        assertEquals(0, runtime.launch("player", new InvalidRef(), "terra", "magma", ability(), "projectile",
                spec, new Vector3d(0.0, 0.0, 0.0), new Vector3d(0.0, 0.0, 1.0), 1.0,
                0L, 20.0, null, (origin, activateAtMillis, expireAtMillis, hideIdentityComponents) -> null).launched());
        assertEquals(0, runtime.launch("player", new TestRef(), "terra", "magma", ability(), "projectile",
                spec, null, new Vector3d(0.0, 0.0, 1.0), 1.0,
                0L, 20.0, null, (origin, activateAtMillis, expireAtMillis, hideIdentityComponents) -> null).launched());
    }

    private static ProjectileRuntimeSpec spec(int count,
                                              double spreadDegrees,
                                              List<Long> launchDelays,
                                              boolean hideIdentityComponents) {
        return new ProjectileRuntimeSpec(
                count,
                2.0,
                20.0,
                2.0,
                0.9,
                spreadDegrees,
                1_500L,
                launchDelays,
                ProjectileTrajectoryProfile.generic(),
                hideIdentityComponents
        );
    }

    private static AbilityData ability() {
        return GSON.fromJson("{\"id\":\"magma_sling\",\"cast_type\":\"projectile\"}", AbilityData.class);
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

    private static final class RecordingHooks implements ProjectileLaunchRuntime.Hooks {
        private final boolean includeVisual;
        private final List<String> events = new ArrayList<>();

        private RecordingHooks(boolean includeVisual) {
            this.includeVisual = includeVisual;
        }

        @Override
        public ProjectileVisualRuntime spawnVisual(Vector3d origin,
                                                   long activateAtMillis,
                                                   long expireAtMillis,
                                                   boolean hideIdentityComponents) {
            events.add("visual:" + origin.x + ":" + origin.y + ":" + origin.z + ":"
                    + activateAtMillis + ":" + expireAtMillis + ":" + hideIdentityComponents);
            return includeVisual
                    ? new ProjectileVisualRuntime(new TestRef(), "travel-" + activateAtMillis, activateAtMillis + 80L)
                    : null;
        }
    }
}
