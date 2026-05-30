package com.motm.runtime.ability.projectile;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.model.StyleData;

import java.util.Set;

public final class ProjectileLaunchHytaleAdapter {
    private static final Set<String> DELAYED_PROJECTILE_CAST_TYPES = Set.of(
            "projectile", "projectile_line", "wave_line", "projectile_burst", "projectile_volley");

    private final ProjectileLaunchRuntime launchRuntime;
    private final ProjectileRuntimeState projectileState;
    private final ProjectileVisualHytaleAdapter visualAdapter;
    private final NativeProjectileHytaleAdapter nativeProjectileAdapter;
    private final Support support;

    public ProjectileLaunchHytaleAdapter(ProjectileLaunchRuntime launchRuntime,
                                         ProjectileRuntimeState projectileState,
                                         ProjectileVisualHytaleAdapter visualAdapter,
                                         NativeProjectileHytaleAdapter nativeProjectileAdapter,
                                         Support support) {
        this.launchRuntime = launchRuntime;
        this.projectileState = projectileState;
        this.visualAdapter = visualAdapter;
        this.nativeProjectileAdapter = nativeProjectileAdapter;
        this.support = support;
    }

    public Result launch(Player runtimePlayer,
                         PlayerData player,
                         StyleData style,
                         AbilityData ability,
                         Ref<EntityStore> explicitTargetRef,
                         Vector3i targetBlock) {
        if (runtimePlayer == null || player == null || style == null || ability == null
                || launchRuntime == null || projectileState == null || visualAdapter == null || support == null) {
            return Result.none();
        }

        String castType = support.lower(ability.getCastType());
        if (!DELAYED_PROJECTILE_CAST_TYPES.contains(castType)) {
            return Result.none();
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return Result.none();
        }

        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            return Result.none();
        }

        ProjectileRuntimeSpec projectileSpec = ProjectileRuntimeSpecs.resolve(
                ability,
                castType,
                support.range(ability),
                support.ticksPerSecond()
        );
        Vector3d origin = resolveOrigin(playerRef, store, projectileSpec.trajectoryProfile());
        Vector3d direction = resolveDirection(
                playerRef,
                store,
                projectileSpec.trajectoryProfile(),
                explicitTargetRef,
                targetBlock,
                origin
        );
        if (origin == null || direction == null) {
            return Result.none();
        }

        double baseDamage = support.damage(player, ability);
        long launchBaseTime = System.currentTimeMillis();
        String traceId = support.traceId();
        ProjectileLaunchRuntime.Result launch = launchRuntime.launch(
                player.getPlayerId(),
                playerRef,
                player.getPlayerClass(),
                style.getId(),
                ability,
                castType,
                projectileSpec,
                origin,
                direction,
                baseDamage,
                launchBaseTime,
                support.ticksPerSecond(),
                traceId,
                (visualOrigin, visualDirection, activateAtMillis, expireAtMillis, hideIdentityComponents) -> {
                    if (projectileSpec.usesNativeProjectileVisual() && nativeProjectileAdapter != null) {
                        return nativeProjectileAdapter.spawn(
                                runtimePlayer,
                                playerRef,
                                player.getPlayerClass(),
                                style.getId(),
                                ability,
                                visualOrigin,
                                visualDirection,
                                projectileSpec.nativeProjectileConfigIds(),
                                activateAtMillis,
                                expireAtMillis
                        );
                    }
                    String visualEffectId = support.visualEffectId(player.getPlayerClass(), style.getId(), ability);
                    return visualAdapter.spawn(
                            runtimePlayer,
                            player.getPlayerClass(),
                            style.getId(),
                            ability,
                            visualOrigin,
                            activateAtMillis,
                            expireAtMillis,
                            hideIdentityComponents,
                            visualEffectId
                    );
                }
        );
        projectileState.addProjectiles(launch.projectiles());
        return new Result(launch.launched(), launch.summary());
    }

    public Vector3d resolveOrigin(Ref<EntityStore> playerRef,
                                  Store<EntityStore> store,
                                  ProjectileTrajectoryProfile trajectoryProfile) {
        Vector3d origin = getPosition(playerRef, store);
        if (origin == null) {
            return null;
        }
        if (trajectoryProfile == null || !trajectoryProfile.offsetsOrigin()) {
            return origin;
        }

        Vector3d forward = getDirection(playerRef, store);
        Vector3d raised = new Vector3d(origin).add(0.0, trajectoryProfile.originVerticalOffset(), 0.0);
        if (forward != null && trajectoryProfile.originForwardOffset() != 0.0) {
            Vector3d horizontalForward = normalizeHorizontal(forward);
            raised = com.motm.util.MotmVectors.addScaled(raised, horizontalForward, trajectoryProfile.originForwardOffset());
        }
        return raised;
    }

    public Vector3d resolveDirection(Ref<EntityStore> playerRef,
                                     Store<EntityStore> store,
                                     ProjectileTrajectoryProfile trajectoryProfile,
                                     Ref<EntityStore> explicitTargetRef,
                                     Vector3i targetBlock,
                                     Vector3d launchOrigin) {
        Vector3d origin = launchOrigin != null && launchOrigin.isFinite()
                ? launchOrigin
                : getPosition(playerRef, store);
        if (origin == null) {
            return null;
        }

        if (explicitTargetRef != null && explicitTargetRef.isValid()) {
            Vector3d targetPosition = getPosition(explicitTargetRef, store);
            if (targetPosition != null) {
                Vector3d aimPoint = trajectoryProfile != null && trajectoryProfile.offsetsExplicitTarget()
                        ? new Vector3d(targetPosition).add(0.0, trajectoryProfile.explicitTargetVerticalOffset(), 0.0)
                        : targetPosition;
                return normalize(subtract(aimPoint, origin));
            }
        }

        if (trajectoryProfile != null && trajectoryProfile.preferLookDirectionWhenUntargeted()) {
            Vector3d direction = getDirection(playerRef, store);
            if (direction == null || !direction.isFinite() || direction.length() < 0.001) {
                return null;
            }
            return normalize(direction);
        }

        if (targetBlock != null) {
            Vector3d targetPosition = new Vector3d(targetBlock.x + 0.5, targetBlock.y + 1.0, targetBlock.z + 0.5);
            return normalize(subtract(targetPosition, origin));
        }

        return getDirection(playerRef, store);
    }

    private static Vector3d getPosition(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }
        return transform.getTransform().getPosition();
    }

    private static Vector3d getDirection(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getDirection() == null) {
            return null;
        }

        Vector3d direction = new Vector3d(transform.getTransform().getDirection());
        if (!direction.isFinite()) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        if (direction.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        return normalize(direction);
    }

    private static Vector3d normalizeHorizontal(Vector3d vector) {
        if (vector == null) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        Vector3d horizontal = new Vector3d(vector.x, 0.0, vector.z);
        if (horizontal.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        horizontal.normalize();
        return horizontal;
    }

    private static Vector3d normalize(Vector3d vector) {
        if (vector == null || !vector.isFinite()) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        Vector3d normalized = new Vector3d(vector);
        if (!normalized.isFinite() || normalized.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        normalized.normalize();
        return normalized;
    }

    private static Vector3d subtract(Vector3d a, Vector3d b) {
        if (a == null || b == null) {
            return new Vector3d();
        }
        return new Vector3d(a.x - b.x, a.y - b.y, a.z - b.z);
    }

    public record Result(int launched, String summary) {
        public Result {
            summary = summary == null ? "" : summary;
        }

        public static Result none() {
            return new Result(0, "");
        }
    }

    public interface Support {
        double range(AbilityData ability);

        double damage(PlayerData player, AbilityData ability);

        String traceId();

        String visualEffectId(String classId, String styleId, AbilityData ability);

        double ticksPerSecond();

        String lower(String value);
    }
}
