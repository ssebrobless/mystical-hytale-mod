package com.motm.runtime.ability.projectile;

import com.google.gson.Gson;
import com.hypixel.hytale.component.Ref;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectileTickRuntimeTest {
    private static final Gson GSON = new Gson();
    private final ProjectileTickRuntime runtime = new ProjectileTickRuntime();

    @Test
    void advancesProjectileAndWaitsWhenNoHitOrExpiry() {
        RecordingHooks hooks = new RecordingHooks(false, false, null);
        ActiveProjectile projectile = projectile(1000L, 5000L, 4.0, 20.0, 0.0);

        boolean remove = runtime.process(projectile, 1200L, 3.0, hooks);

        assertFalse(remove);
        assertEquals(3.0, projectile.travelledDistance(), 0.0001);
        assertEquals(List.of("store", "player", "sync", "resolve"), hooks.events);
    }

    @Test
    void appliesImpactAndDespawnsWhenExpiredNonPiercingProjectileHasNoHit() {
        RecordingHooks hooks = new RecordingHooks(false, false, null);
        ActiveProjectile projectile = projectile(1000L, 1200L, 4.0, 20.0, 0.0);

        boolean remove = runtime.process(projectile, 1300L, 3.0, hooks);

        assertTrue(remove);
        assertEquals(List.of("store", "player", "sync", "resolve", "impact", "despawn"), hooks.events);
    }

    @Test
    void appliesTraversalHitsBeforeResolvingPiercingExpiry() {
        RecordingHooks hooks = new RecordingHooks(true, false, null);
        ActiveProjectile projectile = projectile(1000L, 1200L, 4.0, 20.0, 0.0);

        boolean remove = runtime.process(projectile, 1300L, 3.0, hooks);

        assertTrue(remove);
        assertEquals(List.of("store", "player", "sync", "traversal", "resolve", "despawn"), hooks.events);
    }

    @Test
    void refreshesVisualBeforeActivationWithoutAdvancing() {
        RecordingHooks hooks = new RecordingHooks(false, false, null);
        ActiveProjectile projectile = projectile(2000L, 5000L, 4.0, 20.0, 0.0);

        boolean remove = runtime.process(projectile, 1200L, 3.0, hooks);

        assertFalse(remove);
        assertEquals(0.0, projectile.travelledDistance(), 0.0001);
        assertEquals(List.of("store", "player", "refresh"), hooks.events);
    }

    @Test
    void removesInvalidOwnerWithoutCallingHooks() {
        RecordingHooks hooks = new RecordingHooks(false, false, null);
        ActiveProjectile projectile = projectile(
                new InvalidRef(),
                1000L,
                5000L,
                4.0,
                20.0,
                0.0
        );

        boolean remove = runtime.process(projectile, 1200L, 3.0, hooks);

        assertTrue(remove);
        assertEquals(List.of(), hooks.events);
    }

    @Test
    void despawnsWhenOwnerStoreIsUnavailable() {
        RecordingHooks hooks = new RecordingHooks(false, false, null);
        hooks.ownerStoreAvailable = false;
        ActiveProjectile projectile = projectile(1000L, 5000L, 4.0, 20.0, 0.0);

        boolean remove = runtime.process(projectile, 1200L, 3.0, hooks);

        assertTrue(remove);
        assertEquals(List.of("store", "despawn"), hooks.events);
    }

    @Test
    void despawnsWhenOwnerPlayerIsUnavailable() {
        RecordingHooks hooks = new RecordingHooks(false, false, null);
        hooks.playerAvailable = false;
        ActiveProjectile projectile = projectile(1000L, 5000L, 4.0, 20.0, 0.0);

        boolean remove = runtime.process(projectile, 1200L, 3.0, hooks);

        assertTrue(remove);
        assertEquals(List.of("store", "player", "despawn"), hooks.events);
    }

    @Test
    void untracksVisualWhenImpactVisualShouldRemain() {
        RecordingHooks hooks = new RecordingHooks(false, true, new TestRef());
        ActiveProjectile projectile = projectile(1000L, 5000L, 4.0, 20.0, 0.0);

        boolean remove = runtime.process(projectile, 1200L, 3.0, hooks);

        assertTrue(remove);
        assertEquals(List.of("store", "player", "sync", "resolve", "impact", "untrack"), hooks.events);
    }

    private static ActiveProjectile projectile(long activateAtMillis,
                                               long expireAtMillis,
                                               double speedPerTick,
                                               double maxDistance,
                                               double selfClearance) {
        return projectile(new TestRef(), activateAtMillis, expireAtMillis, speedPerTick, maxDistance, selfClearance);
    }

    private static ActiveProjectile projectile(Ref<EntityStore> ownerRef,
                                               long activateAtMillis,
                                               long expireAtMillis,
                                               double speedPerTick,
                                               double maxDistance,
                                               double selfClearance) {
        return new ActiveProjectile(
                "player",
                ownerRef,
                "terra",
                "magma",
                ability(),
                new Vector3d(0.0, 1.0, 0.0),
                new Vector3d(0.0, 0.0, 1.0),
                speedPerTick,
                maxDistance,
                2.0,
                0.5,
                selfClearance,
                activateAtMillis,
                expireAtMillis,
                12.0,
                Set.of(),
                null,
                null,
                activateAtMillis,
                null
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

    private static final class RecordingHooks implements ProjectileTickRuntime.Hooks {
        private final boolean piercing;
        private final boolean leaveVisual;
        private final Ref<EntityStore> hit;
        private final List<String> events = new ArrayList<>();
        private boolean ownerStoreAvailable = true;
        private boolean playerAvailable = true;

        private RecordingHooks(boolean piercing, boolean leaveVisual, Ref<EntityStore> hit) {
            this.piercing = piercing;
            this.leaveVisual = leaveVisual;
            this.hit = hit;
        }

        @Override
        public boolean hasOwnerStore(ActiveProjectile projectile) {
            events.add("store");
            return ownerStoreAvailable;
        }

        @Override
        public PlayerData player(String ownerPlayerId) {
            events.add("player");
            if (!playerAvailable) {
                return null;
            }
            PlayerData player = new PlayerData();
            player.setPlayerId(ownerPlayerId);
            return player;
        }

        @Override
        public void refreshVisual(ActiveProjectile projectile, long now) {
            events.add("refresh");
        }

        @Override
        public void syncVisual(ActiveProjectile projectile, long now) {
            events.add("sync");
        }

        @Override
        public boolean isPiercing(AbilityData ability) {
            return piercing;
        }

        @Override
        public void applyTraversalHits(ActiveProjectile projectile,
                                       PlayerData player,
                                       Vector3d from,
                                       Vector3d to) {
            events.add("traversal");
        }

        @Override
        public Ref<EntityStore> resolveHit(ActiveProjectile projectile, Vector3d from, Vector3d to) {
            events.add("resolve");
            return hit;
        }

        @Override
        public void applyImpact(ActiveProjectile projectile,
                                PlayerData player,
                                Vector3d impactPosition,
                                Ref<EntityStore> directHit) {
            events.add("impact");
        }

        @Override
        public boolean shouldLeaveVisualOnImpact(AbilityData ability) {
            return leaveVisual;
        }

        @Override
        public void untrackVisual(Ref<EntityStore> visualRef) {
            events.add("untrack");
        }

        @Override
        public void despawnVisual(ActiveProjectile projectile) {
            events.add("despawn");
        }
    }
}
