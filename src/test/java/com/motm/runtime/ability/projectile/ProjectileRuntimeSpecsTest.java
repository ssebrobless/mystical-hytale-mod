package com.motm.runtime.ability.projectile;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectileRuntimeSpecsTest {

    private static final Gson GSON = new Gson();
    private static final double TICKS_PER_SECOND = 20.0;

    @Test
    void resolvesMagmaSlingProfileWithoutGenericPlaybackBranching() {
        AbilityData ability = parseAbility("""
                {
                  "id": "magma_sling",
                  "cast_type": "projectile",
                  "target_type": "enemy",
                  "projectile_speed": 12
                }
                """);

        ProjectileRuntimeSpec spec =
                ProjectileRuntimeSpecs.resolve(ability, "projectile", 8.0, TICKS_PER_SECOND);

        assertEquals(1, spec.projectileCount());
        assertEquals(0.6, spec.speedPerTick(), 0.0001);
        assertEquals(2.0, spec.impactRadius(), 0.0001);
        assertEquals(1.8, spec.collisionRadius(), 0.0001);
        assertEquals(1.15, spec.trajectoryProfile().originVerticalOffset(), 0.0001);
        assertEquals(0.9, spec.trajectoryProfile().originForwardOffset(), 0.0001);
        assertEquals(1.0, spec.trajectoryProfile().explicitTargetVerticalOffset(), 0.0001);
        assertTrue(spec.trajectoryProfile().preferLookDirectionWhenUntargeted());
        assertEquals(0.75, spec.trajectoryProfile().ownerSelfClearanceDistance(), 0.0001);
        assertTrue(spec.hideVisualProxyIdentityComponents());
        assertTrue(spec.usesNativeProjectileVisual());
        assertEquals("Projectile_Config_MOTM_Magma_Sling_Visual", spec.nativeProjectileConfigIds().getFirst());
    }

    @Test
    void resolvesKnownVolleyCadenceAndSpread() {
        AbilityData ability = parseAbility("""
                {
                  "id": "bullet_storm",
                  "cast_type": "projectile_volley",
                  "target_type": "enemy",
                  "projectile_speed": 30
                }
                """);

        ProjectileRuntimeSpec spec =
                ProjectileRuntimeSpecs.resolve(ability, "projectile_volley", 12.0, TICKS_PER_SECOND);

        assertEquals(6, spec.projectileCount());
        assertEquals(1.5, spec.speedPerTick(), 0.0001);
        assertEquals(4.5, spec.spreadDegrees(), 0.0001);
        assertEquals(0L, spec.launchDelayMillis(0));
        assertEquals(65L, spec.launchDelayMillis(1));
        assertEquals(325L, spec.launchDelayMillis(5));
    }

    @Test
    void clampsSpeedAndUsesBurstDefaults() {
        AbilityData ability = parseAbility("""
                {
                  "id": "generic_burst",
                  "cast_type": "projectile_burst",
                  "target_type": "enemy",
                  "projectile_speed": 99
                }
                """);

        ProjectileRuntimeSpec spec =
                ProjectileRuntimeSpecs.resolve(ability, "projectile_burst", 3.0, TICKS_PER_SECOND);

        assertEquals(3, spec.projectileCount());
        assertEquals(1.9, spec.speedPerTick(), 0.0001);
        assertEquals(4.0, spec.maxDistance(), 0.0001);
        assertEquals(2.25, spec.impactRadius(), 0.0001);
        assertEquals(1.0, spec.collisionRadius(), 0.0001);
        assertEquals(12.0, spec.spreadDegrees(), 0.0001);
        assertEquals(22L, spec.launchDelayMillis(1));
    }

    private static AbilityData parseAbility(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
