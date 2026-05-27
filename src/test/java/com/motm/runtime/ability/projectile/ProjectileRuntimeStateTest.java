package com.motm.runtime.ability.projectile;

import com.google.gson.Gson;
import com.hypixel.hytale.math.vector.Vector3d;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectileRuntimeStateTest {
    private static final Gson GSON = new Gson();

    @Test
    void ownsActiveProjectileCollectionAndProcessedRemoval() {
        ProjectileRuntimeState state = new ProjectileRuntimeState();
        int added = state.addProjectiles(List.of(projectile("player-a"), projectile("player-b")));

        assertEquals(2, added);
        assertEquals(2, state.activeProjectileCount());

        state.removeProcessedProjectiles(projectile -> "player-a".equals(projectile.ownerPlayerId()));

        assertEquals(1, state.activeProjectileCount());
    }

    @Test
    void removesProjectilesForPlayerAndRunsCleanup() {
        ProjectileRuntimeState state = new ProjectileRuntimeState();
        List<String> cleaned = new ArrayList<>();
        state.addProjectile(projectile("player"));
        state.addProjectile(projectile("other"));

        int removed = state.removeProjectilesForPlayer("player", projectile -> cleaned.add(projectile.ownerPlayerId()));

        assertEquals(1, removed);
        assertEquals(List.of("player"), cleaned);
        assertEquals(1, state.activeProjectileCount());
    }

    private static ActiveProjectile projectile(String ownerPlayerId) {
        return new ActiveProjectile(
                ownerPlayerId,
                null,
                "terra",
                "magma",
                ability(),
                new Vector3d(0.0, 1.0, 0.0),
                new Vector3d(0.0, 0.0, 1.0),
                1.0,
                8.0,
                2.0,
                0.5,
                0.0,
                1000L,
                5000L,
                12.0,
                Set.of(),
                null,
                null,
                1000L,
                null
        );
    }

    private static AbilityData ability() {
        return GSON.fromJson("{\"id\":\"magma_sling\",\"cast_type\":\"projectile\"}", AbilityData.class);
    }
}
