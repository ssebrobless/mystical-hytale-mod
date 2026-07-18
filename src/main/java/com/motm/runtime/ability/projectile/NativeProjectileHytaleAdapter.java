package com.motm.runtime.ability.projectile;

import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.util.MotmObservability;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Spawns first-class Hytale projectile actors for MOTM projectile abilities.
 *
 * MOTM keeps the ActiveProjectile simulation as the owner-attributed damage,
 * status, reaction, and timeout path while Hytale owns actor motion/rendering.
 */
public final class NativeProjectileHytaleAdapter {
    private final ProjectileVisualHytaleAdapter.IntentRecorder intentRecorder;
    private final Logger log;

    public NativeProjectileHytaleAdapter(ProjectileVisualHytaleAdapter.IntentRecorder intentRecorder,
                                         Logger log) {
        this.intentRecorder = intentRecorder;
        this.log = log;
    }

    public ProjectileVisualRuntime spawn(Player runtimePlayer,
                                         Ref<EntityStore> ownerRef,
                                         String classId,
                                         String styleId,
                                         AbilityData ability,
                                         Vector3d origin,
                                         Vector3d direction,
                                         List<String> projectileConfigIds,
                                         long activateAtMillis,
                                         long expireAtMillis) {
        if (runtimePlayer == null || ownerRef == null || !ownerRef.isValid()
                || origin == null || direction == null || projectileConfigIds == null || projectileConfigIds.isEmpty()) {
            return ProjectileVisualRuntime.none();
        }
        Store<EntityStore> store = ownerRef.getStore();
        if (store == null) {
            return ProjectileVisualRuntime.none();
        }

        ProjectileConfig projectileConfig = resolveProjectileConfig(projectileConfigIds);
        if (projectileConfig == null) {
            record(classId, styleId, ability, null, false, origin, direction, -1, "config_not_found", activateAtMillis, expireAtMillis);
            return ProjectileVisualRuntime.none();
        }

        ProjectileModule projectileModule = ProjectileModule.get();
        if (projectileModule == null) {
            record(classId, styleId, ability, projectileConfig.getId(), false, origin, direction, -1, "module_missing", activateAtMillis, expireAtMillis);
            return ProjectileVisualRuntime.none();
        }

        AtomicReference<Ref<EntityStore>> projectileRef = new AtomicReference<>();
        AtomicReference<String> failure = new AtomicReference<>();
        store.forEachChunk((chunk, commandBuffer) -> {
            if (projectileRef.get() != null || failure.get() != null) {
                return;
            }
            if (commandBuffer == null) {
                failure.set("command_buffer_missing");
                return;
            }
            try {
                projectileRef.set(spawn(projectileModule, ownerRef, commandBuffer, projectileConfig, origin, normalize(direction)));
            } catch (Exception e) {
                failure.set(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });

        Ref<EntityStore> spawnedRef = projectileRef.get();
        boolean spawned = spawnedRef != null && spawnedRef.isValid();
        if (spawnedRef != null && spawnedRef.isValid()
                && ability != null && "pressure_burst".equalsIgnoreCase(ability.getId())) {
            new PressureBurstState(activateAtMillis).applyProjectileScale(
                    spawnedRef, store, activateAtMillis + PressureBurstState.MAX_CHARGE_MILLIS);
        }
        String failureReason = failure.get();
        record(
                classId,
                styleId,
                ability,
                projectileConfig.getId(),
                spawned,
                origin,
                direction,
                spawned ? spawnedRef.getIndex() : -1,
                failureReason,
                activateAtMillis,
                expireAtMillis
        );
        if (failureReason != null && log != null) {
            log.warning("[MOTM] Native projectile visual spawn failed: abilityId="
                    + (ability == null ? "" : ability.getId())
                    + " config=" + projectileConfig.getId()
                    + " failure=" + failureReason);
        }
        return spawned
                ? new ProjectileVisualRuntime(spawnedRef, null, Long.MAX_VALUE)
                : ProjectileVisualRuntime.none();
    }

    private ProjectileConfig resolveProjectileConfig(List<String> projectileConfigIds) {
        for (String candidate : projectileConfigIds) {
            if (candidate == null || candidate.isBlank()) {
                continue;
            }
            ProjectileConfig projectileConfig = ProjectileConfig.getAssetMap().getAsset(candidate);
            if (projectileConfig != null) {
                return projectileConfig;
            }
        }
        return null;
    }

    private Ref<EntityStore> spawn(ProjectileModule projectileModule,
                                   Ref<EntityStore> ownerRef,
                                   CommandBuffer<EntityStore> commandBuffer,
                                   ProjectileConfig projectileConfig,
                                   Vector3d origin,
                                   Vector3d direction) {
        return projectileModule.spawnProjectile(ownerRef, commandBuffer, projectileConfig, origin, direction);
    }

    private void record(String classId,
                        String styleId,
                        AbilityData ability,
                        String projectileConfigId,
                        boolean spawned,
                        Vector3d origin,
                        Vector3d direction,
                        int entityIndex,
                        String failure,
                        long activateAtMillis,
                        long expireAtMillis) {
        if (intentRecorder == null) {
            return;
        }
        intentRecorder.record("native_projectile_visual_spawned", MotmObservability.mapOf(
                "classId", classId,
                "styleId", styleId,
                "abilityId", ability == null ? null : ability.getId(),
                "projectileConfigId", projectileConfigId,
                "spawned", spawned,
                "origin", formatVector(origin),
                "direction", formatVector(direction),
                "entityIndex", entityIndex,
                "failure", failure,
                "activateAtMillis", activateAtMillis,
                "expireAtMillis", expireAtMillis
        ));
    }

    private static Vector3d normalize(Vector3d vector) {
        Vector3d normalized = vector == null ? new Vector3d(0.0, 0.0, 1.0) : new Vector3d(vector);
        if (!normalized.isFinite() || normalized.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        return normalized.normalize();
    }

    private static String formatVector(Vector3d vector) {
        if (vector == null) {
            return "null";
        }
        return String.format(Locale.US, "%.2f,%.2f,%.2f", vector.x, vector.y, vector.z);
    }
}
