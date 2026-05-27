package com.motm.runtime.ability.field;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.model.StyleData;

import java.util.Locale;

public final class FieldActivationHytaleAdapter {
    private final FieldActivationRuntime activationRuntime;
    private final FieldRuntimeState fieldState;
    private final FieldVisualHytaleAdapter visualAdapter;
    private final Support support;

    public FieldActivationHytaleAdapter(FieldActivationRuntime activationRuntime,
                                        FieldRuntimeState fieldState,
                                        FieldVisualHytaleAdapter visualAdapter,
                                        Support support) {
        this.activationRuntime = activationRuntime;
        this.fieldState = fieldState;
        this.visualAdapter = visualAdapter;
        this.support = support;
    }

    public Result activatePersistentField(Player runtimePlayer,
                                          PlayerData player,
                                          StyleData style,
                                          AbilityData ability,
                                          Ref<EntityStore> explicitTargetRef,
                                          Vector3i targetBlock) {
        if (runtimePlayer == null || player == null || style == null || ability == null
                || support == null || activationRuntime == null || fieldState == null) {
            return Result.none();
        }

        String castType = lower(ability.getCastType());
        if (!support.isPersistentField(ability)) {
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

        Vector3d origin = position(playerRef, store);
        Vector3d forward = direction(playerRef, store);
        if (origin == null || forward == null) {
            return Result.none();
        }

        boolean ironWall = FieldRuntimeSpecs.isIronWall(ability);
        if (ironWall) {
            origin = support.resolveStableIronWallOrigin(player.getPlayerId(), origin);
        } else if (FieldRuntimeSpecs.isCasterCentered(ability)) {
            origin = support.resolveStableCasterCenteredOrigin(player.getPlayerId(), origin);
        }

        Vector3d gemAnchor = support.resolveActiveLapidaryGemCenter(player, ability, store);
        Vector3d ironWallForward = ironWall ? support.resolveIronWallForward(forward) : null;
        Vector3d center = gemAnchor != null
                ? gemAnchor
                : ironWallForward != null
                ? support.resolveIronWallCenter(origin, ironWallForward)
                : support.resolveAreaCenter(origin, forward, explicitTargetRef, targetBlock, support.range(ability), ability);
        if (center == null) {
            return Result.none();
        }

        double radius = FieldRuntimeSpecs.radius(ability);
        double halfWidth = FieldRuntimeSpecs.halfWidth(ability, radius);
        double thickness = FieldRuntimeSpecs.thickness(ability, radius);
        Vector3d lineDirection = ironWallForward != null
                ? new Vector3d(-ironWallForward.z, 0.0, ironWallForward.x)
                : rotateAroundY(new Vector3d(forward.x, 0.0, forward.z), 90.0);
        long now = System.currentTimeMillis();
        long delayMillis = FieldRuntimeSpecs.delayMillis(ability);
        long activateAtMillis = now + delayMillis;
        long durationMillis = FieldRuntimeSpecs.durationMillis(ability);
        String terrainSummary = support.placePersistentTerrainSelection(
                runtimePlayer,
                ability,
                center,
                normalize(ironWallForward != null ? ironWallForward : forward),
                normalize(lineDirection),
                activateAtMillis + durationMillis
        );
        int immediatePushes = ironWall
                ? support.pushTargetsOverlappingIronWall(
                playerRef,
                store,
                ability,
                center,
                ironWallForward != null ? ironWallForward : forward,
                lineDirection)
                : 0;
        FieldVisualRuntime visual = visualAdapter == null
                ? FieldVisualRuntime.none()
                : visualAdapter.spawn(
                runtimePlayer,
                player.getPlayerClass(),
                style.getId(),
                ability,
                center,
                normalize(lineDirection),
                halfWidth,
                activateAtMillis,
                activateAtMillis + durationMillis,
                support.resolveFieldVisualEffectId(player.getPlayerClass(), style.getId(), ability)
        );
        FieldActivationRuntime.Result activation = activationRuntime.activate(
                player.getPlayerId(),
                playerRef,
                player.getPlayerClass(),
                style.getId(),
                ability,
                castType,
                center,
                normalize(ironWallForward != null ? ironWallForward : forward),
                normalize(lineDirection),
                radius,
                halfWidth,
                thickness,
                activateAtMillis,
                durationMillis,
                false,
                visual,
                support.traceId(),
                terrainSummary,
                immediatePushes,
                delayMillis,
                support.pullStep(ability)
        );
        fieldState.addFields(activation.fields());
        return new Result(activation.activated(), activation.summary());
    }

    private static Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }
        return transform.getTransform().getPosition();
    }

    private static Vector3d direction(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getDirection() == null) {
            return null;
        }

        Vector3d resolvedDirection = transform.getTransform().getDirection().clone();
        if (!resolvedDirection.isFinite() || resolvedDirection.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        return normalize(resolvedDirection);
    }

    private static Vector3d normalize(Vector3d vector) {
        Vector3d normalized = vector == null ? new Vector3d(0.0, 0.0, 1.0) : vector.clone();
        if (normalized.length() < 0.0001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        normalized.normalize();
        return normalized;
    }

    private static Vector3d rotateAroundY(Vector3d vector, double degrees) {
        if (vector == null) {
            return new Vector3d();
        }
        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        return new Vector3d(
                vector.x * cos - vector.z * sin,
                vector.y,
                vector.x * sin + vector.z * cos
        );
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public interface Support {
        boolean isPersistentField(AbilityData ability);

        Vector3d resolveStableIronWallOrigin(String playerId, Vector3d origin);

        Vector3d resolveStableCasterCenteredOrigin(String playerId, Vector3d origin);

        Vector3d resolveActiveLapidaryGemCenter(PlayerData player, AbilityData ability, Store<EntityStore> store);

        Vector3d resolveIronWallForward(Vector3d forward);

        Vector3d resolveIronWallCenter(Vector3d origin, Vector3d ironWallForward);

        Vector3d resolveAreaCenter(Vector3d origin,
                                   Vector3d forward,
                                   Ref<EntityStore> explicitTargetRef,
                                   Vector3i targetBlock,
                                   double range,
                                   AbilityData ability);

        double range(AbilityData ability);

        String placePersistentTerrainSelection(Player runtimePlayer,
                                               AbilityData ability,
                                               Vector3d center,
                                               Vector3d forward,
                                               Vector3d lineDirection,
                                               long expireAtMillis);

        int pushTargetsOverlappingIronWall(Ref<EntityStore> playerRef,
                                           Store<EntityStore> store,
                                           AbilityData ability,
                                           Vector3d center,
                                           Vector3d forward,
                                           Vector3d lineDirection);

        String resolveFieldVisualEffectId(String classId, String styleId, AbilityData ability);

        String traceId();

        double pullStep(AbilityData ability);
    }

    public record Result(boolean activated, String summary) {
        public Result {
            summary = summary == null ? "" : summary;
        }

        public static Result none() {
            return new Result(false, "");
        }
    }
}
