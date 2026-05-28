package com.motm.runtime.ability.projectile;

import com.hypixel.hytale.component.Store;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.model.StyleData;
import com.motm.runtime.state.VisualProxyRuntimeState;

import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

/**
 * Owns projectile-family runtime wiring so the playback manager retains a
 * single projectile facade dependency.
 */
public final class ProjectileRuntimeFacade {

    private final ProjectileLaunchHytaleAdapter launchAdapter;
    private final ProjectileLifecycleHytaleAdapter lifecycleAdapter;

    public ProjectileRuntimeFacade(VisualProxyRuntimeState visualProxyState,
                                   ProjectileVisualHytaleAdapter.EffectApplier effectApplier,
                                   ProjectileVisualHytaleAdapter.IntentRecorder intentRecorder,
                                   Logger log,
                                   Set<String> ignoredTargetRoles,
                                   double defaultLightningArcRadius,
                                   ProjectileLaunchHytaleAdapter.Support launchSupport,
                                   ProjectileImpactHytaleAdapter.Support impactSupport,
                                   Function<String, PlayerData> playerLookup,
                                   ProjectileLifecycleHytaleAdapter.TraceScope traceScope) {
        ProjectileRuntimeState projectileState = new ProjectileRuntimeState();
        ProjectileHitHytaleAdapter hitAdapter = new ProjectileHitHytaleAdapter(ignoredTargetRoles);
        ProjectileImpactHytaleAdapter impactAdapter = new ProjectileImpactHytaleAdapter(
                hitAdapter,
                defaultLightningArcRadius
        );
        ProjectileVisualHytaleAdapter visualAdapter = new ProjectileVisualHytaleAdapter(
                visualProxyState,
                effectApplier,
                intentRecorder,
                log
        );
        this.launchAdapter = new ProjectileLaunchHytaleAdapter(
                new ProjectileLaunchRuntime(),
                projectileState,
                visualAdapter,
                launchSupport
        );
        ProjectileTickHytaleAdapter tickAdapter = new ProjectileTickHytaleAdapter(
                new ProjectileTickRuntime(),
                visualAdapter,
                hitAdapter,
                impactAdapter,
                impactSupport,
                playerLookup
        );
        this.lifecycleAdapter = new ProjectileLifecycleHytaleAdapter(
                projectileState,
                tickAdapter,
                visualAdapter,
                traceScope
        );
    }

    public ProjectileLaunchHytaleAdapter.Result launch(Player runtimePlayer,
                                                       PlayerData player,
                                                       StyleData style,
                                                       AbilityData ability,
                                                       com.hypixel.hytale.component.Ref<EntityStore> explicitTargetRef,
                                                       Vector3i targetBlock) {
        return launchAdapter.launch(runtimePlayer, player, style, ability, explicitTargetRef, targetBlock);
    }

    public void processForStore(Store<EntityStore> currentStore, long now) {
        lifecycleAdapter.processForStore(currentStore, now);
    }

    public int removeForPlayer(String playerId) {
        return lifecycleAdapter.removeForPlayer(playerId);
    }

    public int activeProjectileCount() {
        return lifecycleAdapter.activeProjectileCount();
    }
}
