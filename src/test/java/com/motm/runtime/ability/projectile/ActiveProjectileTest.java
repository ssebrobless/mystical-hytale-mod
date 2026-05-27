package com.motm.runtime.ability.projectile;

import com.google.gson.Gson;
import com.hypixel.hytale.math.vector.Vector3d;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActiveProjectileTest {

    private static final Gson GSON = new Gson();

    @Test
    void ownsProjectileStateAndRuntimeMutationIntents() {
        AbilityData ability = GSON.fromJson("""
                {
                  "id": "magma_sling",
                  "cast_type": "projectile"
                }
                """, AbilityData.class);
        Vector3d position = new Vector3d(1.0, 2.0, 3.0);
        Vector3d direction = new Vector3d(0.0, 0.0, 1.0);

        ActiveProjectile projectile = new ActiveProjectile(
                "player",
                null,
                "terra",
                "magma",
                ability,
                position,
                direction,
                1.5,
                12.0,
                2.0,
                1.8,
                1.0,
                100L,
                5000L,
                24.0,
                new HashSet<>(),
                null,
                "travel",
                200L,
                "trace"
        );

        assertEquals("player", projectile.ownerPlayerId());
        assertEquals("terra", projectile.classId());
        assertEquals("magma", projectile.styleId());
        assertEquals(ability, projectile.ability());
        assertEquals(1.5, projectile.speedPerTick(), 0.0001);
        assertEquals(12.0, projectile.maxDistance(), 0.0001);
        assertEquals("travel", projectile.travelEffectId());
        assertEquals("trace", projectile.traceId());
        assertNotSame(position, projectile.position());
        assertNotSame(direction, projectile.direction());

        position.x = 99.0;
        assertEquals(1.0, projectile.position().x, 0.0001);

        projectile.advanceTo(new Vector3d(2.0, 3.0, 4.0), 1.25);
        projectile.scheduleNextVisualRefresh(1000L, 220L);
        projectile.hitEntityIds().add("target");

        assertEquals(2.0, projectile.position().x, 0.0001);
        assertEquals(1.25, projectile.travelledDistance(), 0.0001);
        assertEquals(1220L, projectile.nextVisualRefreshAtMillis());
        assertTrue(projectile.hitEntityIds().contains("target"));
    }
}
