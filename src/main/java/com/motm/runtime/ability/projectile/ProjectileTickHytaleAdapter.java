package com.motm.runtime.ability.projectile;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;

import java.util.Locale;
import java.util.function.Function;

public final class ProjectileTickHytaleAdapter {
    private static final double MAX_STEP_DISTANCE = 2.6;

    private final ProjectileTickRuntime tickRuntime;
    private final ProjectileVisualHytaleAdapter visualAdapter;
    private final ProjectileHitHytaleAdapter hitAdapter;
    private final ProjectileImpactHytaleAdapter impactAdapter;
    private final ProjectileImpactHytaleAdapter.Support impactSupport;
    private final Function<String, PlayerData> playerLookup;

    public ProjectileTickHytaleAdapter(ProjectileTickRuntime tickRuntime,
                                       ProjectileVisualHytaleAdapter visualAdapter,
                                       ProjectileHitHytaleAdapter hitAdapter,
                                       ProjectileImpactHytaleAdapter impactAdapter,
                                       ProjectileImpactHytaleAdapter.Support impactSupport,
                                       Function<String, PlayerData> playerLookup) {
        this.tickRuntime = tickRuntime;
        this.visualAdapter = visualAdapter;
        this.hitAdapter = hitAdapter;
        this.impactAdapter = impactAdapter;
        this.impactSupport = impactSupport;
        this.playerLookup = playerLookup;
    }

    public boolean process(ActiveProjectile projectile, long now) {
        if (tickRuntime == null) {
            return true;
        }

        return tickRuntime.process(projectile, now, MAX_STEP_DISTANCE, new ProjectileTickRuntime.Hooks() {
            @Override
            public boolean hasOwnerStore(ActiveProjectile projectile) {
                return projectile.ownerRef() != null && projectile.ownerRef().getStore() != null;
            }

            @Override
            public PlayerData player(String ownerPlayerId) {
                return playerLookup == null ? null : playerLookup.apply(ownerPlayerId);
            }

            @Override
            public void refreshVisual(ActiveProjectile projectile, long now) {
                visualAdapter.refresh(projectile, now);
            }

            @Override
            public void syncVisual(ActiveProjectile projectile, long now) {
                visualAdapter.sync(projectile, now);
            }

            @Override
            public boolean isPiercing(AbilityData ability) {
                return isPiercingProjectile(ability);
            }

            @Override
            public void applyTraversalHits(ActiveProjectile projectile,
                                           PlayerData player,
                                           Vector3d from,
                                           Vector3d to) {
                impactAdapter.applyTraversalHits(
                        projectile,
                        player,
                        projectile.ownerRef().getStore(),
                        from,
                        to,
                        impactSupport
                );
            }

            @Override
            public Ref<EntityStore> resolveHit(ActiveProjectile projectile,
                                               Vector3d from,
                                               Vector3d to) {
                return hitAdapter.resolveHit(projectile, projectile.ownerRef().getStore(), from, to);
            }

            @Override
            public void applyImpact(ActiveProjectile projectile,
                                    PlayerData player,
                                    Vector3d impactPosition,
                                    Ref<EntityStore> directHit) {
                impactAdapter.applyImpact(
                        projectile,
                        player,
                        projectile.ownerRef().getStore(),
                        impactPosition,
                        directHit,
                        impactSupport
                );
            }

            @Override
            public boolean shouldLeaveVisualOnImpact(AbilityData ability) {
                return false;
            }

            @Override
            public void untrackVisual(Ref<EntityStore> visualRef) {
                visualAdapter.untrack(visualRef);
            }

            @Override
            public void despawnVisual(ActiveProjectile projectile) {
                visualAdapter.despawn(projectile);
            }
        });
    }

    private static boolean isPiercingProjectile(AbilityData ability) {
        if (ability == null) {
            return false;
        }

        String castType = lower(ability.getCastType());
        if ("wave_line".equals(castType) || "projectile_line".equals(castType)) {
            return true;
        }

        String travelType = lower(ability.getTravelType());
        return travelType.contains("wave")
                || travelType.contains("slash")
                || travelType.contains("cutter")
                || travelType.contains("tide")
                || travelType.contains("shard")
                || travelType.contains("gust");
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }
}
