package com.motm.runtime.ability.projectile;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import org.joml.Vector3f;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.CollisionResultComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.RespondToHit;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.modules.projectile.component.Projectile;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.model.AbilityData;
import com.motm.runtime.state.VisualProxyRuntimeState;
import com.motm.util.HytaleAssetResolver;
import com.motm.util.MotmObservability;
import com.motm.util.MotmNpcRoles;

import java.util.Map;
import java.util.Locale;
import java.util.logging.Logger;

public final class ProjectileVisualHytaleAdapter {
    public static final long VISUAL_REFRESH_MS = 220L;

    private final VisualProxyRuntimeState visualProxyState;
    private final EffectApplier effectApplier;
    private final IntentRecorder intentRecorder;
    private final Logger log;

    public ProjectileVisualHytaleAdapter(VisualProxyRuntimeState visualProxyState,
                                         EffectApplier effectApplier,
                                         IntentRecorder intentRecorder,
                                         Logger log) {
        this.visualProxyState = visualProxyState;
        this.effectApplier = effectApplier;
        this.intentRecorder = intentRecorder;
        this.log = log;
    }

    public ProjectileVisualRuntime spawn(Player runtimePlayer,
                                         String classId,
                                         String styleId,
                                         AbilityData ability,
                                         Vector3d position,
                                         long activateAtMillis,
                                         long expireAtMillis,
                                         boolean hideIdentityComponents,
                                         String effectId) {
        if (runtimePlayer == null || position == null || effectId == null || effectId.isBlank()) {
            return ProjectileVisualRuntime.none();
        }

        World world = runtimePlayer.getWorld();
        if (world == null) {
            return ProjectileVisualRuntime.none();
        }

        String modelId = HytaleAssetResolver.resolveModelId(classId, styleId, ability);
        boolean hasExplicitModel = modelId != null && !modelId.isBlank();
        if (!hasExplicitModel) {
            recordSkippedSpawn(classId, styleId, ability, effectId, position, activateAtMillis, expireAtMillis);
            return ProjectileVisualRuntime.none();
        }

        String roleId = HytaleAssetResolver.resolveProjectileRoleId(classId, styleId, ability);
        NPCEntity proxy = new NPCEntity(world);
        MotmNpcRoles.applyRole(proxy, roleId,
                HytaleAssetResolver.resolveRenderlessVisualProxyRoleId(), log);
        proxy.setDespawnTime((float) Math.max(0.6, ((expireAtMillis - System.currentTimeMillis()) / 1000.0) + 0.5));
        world.spawnEntity(proxy, new Vector3d(position), new com.hypixel.hytale.math.vector.Rotation3f(0f, 0f, 0f));

        Ref<EntityStore> proxyRef = proxy.getReference();
        if (proxyRef == null || !proxyRef.isValid() || proxyRef.getStore() == null) {
            return ProjectileVisualRuntime.none();
        }

        visualProxyState.add(proxyRef);
        NPCEntity.setAppearance(proxyRef, modelId, proxyRef.getStore());
        configureProxy(proxyRef, proxyRef.getStore(), hideIdentityComponents, false);
        applyEffect(proxyRef, proxyRef.getStore(), effectId);
        recordSpawn(classId, styleId, ability, roleId, modelId, effectId, position, proxyRef, activateAtMillis, expireAtMillis);
        return new ProjectileVisualRuntime(proxyRef, effectId, activateAtMillis + 80L);
    }

    public void sync(ActiveProjectile projectile, long now) {
        if (projectile == null || projectile.visualRef() == null || !projectile.visualRef().isValid()) {
            return;
        }

        Store<EntityStore> visualStore = projectile.visualRef().getStore();
        if (visualStore == null) {
            return;
        }

        NPCEntity npc = visualStore.getComponent(projectile.visualRef(), NPCEntity.getComponentType());
        if (npc != null) {
            npc.moveTo(projectile.visualRef(),
                    projectile.position().x,
                    projectile.position().y,
                    projectile.position().z,
                    visualStore);
        }

        refresh(projectile, now);
    }

    public void refresh(ActiveProjectile projectile, long now) {
        if (projectile == null
                || projectile.visualRef() == null
                || !projectile.visualRef().isValid()
                || projectile.travelEffectId() == null
                || projectile.travelEffectId().isBlank()
                || now < projectile.nextVisualRefreshAtMillis()) {
            return;
        }

        Store<EntityStore> visualStore = projectile.visualRef().getStore();
        if (visualStore == null) {
            return;
        }

        if (applyEffect(projectile.visualRef(), visualStore, projectile.travelEffectId())) {
            projectile.scheduleNextVisualRefresh(now, VISUAL_REFRESH_MS);
        }
    }

    public void despawn(ActiveProjectile projectile) {
        if (projectile == null) {
            return;
        }
        Ref<EntityStore> ref = projectile.visualRef();
        if (ref == null || !ref.isValid()) {
            return;
        }
        Store<EntityStore> store = ref.getStore();
        if (store == null) {
            return;
        }
        try {
            if (store.getComponent(ref, Projectile.getComponentType()) != null) {
                store.removeEntity(ref, RemoveReason.REMOVE);
            } else {
                visualProxyState.despawn(ref);
            }
        } catch (RuntimeException e) {
            if (log != null) {
                log.warning("[MOTM] Projectile cleanup failed safely: " + e.getMessage());
            }
            visualProxyState.remove(ref);
        }


    }
    public void untrack(Ref<EntityStore> visualRef) {
        visualProxyState.remove(visualRef);
    }

    private void configureProxy(Ref<EntityStore> proxyRef,
                                Store<EntityStore> store,
                                boolean hideIdentityComponents,
                                boolean hideModel) {
        if (proxyRef == null || !proxyRef.isValid() || store == null || (!hideIdentityComponents && !hideModel)) {
            return;
        }
        try {
            if (hideModel) {
                store.removeComponentIfExists(proxyRef, ModelComponent.getComponentType());
            }
            if (hideIdentityComponents) {
                store.removeComponentIfExists(proxyRef, Nameplate.getComponentType());
                store.removeComponentIfExists(proxyRef, DisplayNameComponent.getComponentType());
                store.removeComponentIfExists(proxyRef, Interactable.getComponentType());
                store.removeComponentIfExists(proxyRef, RespondToHit.getComponentType());
                store.removeComponentIfExists(proxyRef, CollisionResultComponent.getComponentType());
            }
        } catch (Exception e) {
            if (log != null) {
                log.warning("[MOTM] Projectile visual proxy cleanup failed safely: " + e.getMessage());
            }
        }
    }

    private boolean applyEffect(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
        return effectApplier != null && effectApplier.apply(ref, store, effectId);
    }

    private void recordSkippedSpawn(String classId,
                                    String styleId,
                                    AbilityData ability,
                                    String effectId,
                                    Vector3d position,
                                    long activateAtMillis,
                                    long expireAtMillis) {
        if (intentRecorder == null) {
            return;
        }
        intentRecorder.record("projectile_visual_proxy_skipped", MotmObservability.mapOf(
                "reason", "particle_only_projectile",
                "classId", classId,
                "styleId", styleId,
                "abilityId", ability != null ? ability.getId() : null,
                "effectId", effectId,
                "position", formatVector(position),
                "activateAtMillis", activateAtMillis,
                "expireAtMillis", expireAtMillis
        ));
    }

    private void recordSpawn(String classId,
                             String styleId,
                             AbilityData ability,
                             String roleId,
                             String modelId,
                             String effectId,
                             Vector3d position,
                             Ref<EntityStore> proxyRef,
                             long activateAtMillis,
                             long expireAtMillis) {
        if (intentRecorder == null) {
            return;
        }
        intentRecorder.record("projectile_visual_proxy_spawned", MotmObservability.mapOf(
                "classId", classId,
                "styleId", styleId,
                "abilityId", ability != null ? ability.getId() : null,
                "roleId", roleId,
                "modelId", modelId,
                "effectId", effectId,
                "position", formatVector(position),
                "entityIndex", proxyRef.getIndex(),
                "activateAtMillis", activateAtMillis,
                "expireAtMillis", expireAtMillis
        ));
    }

    private static String formatVector(Vector3d vector) {
        if (vector == null) {
            return "null";
        }
        return String.format(Locale.US, "%.2f,%.2f,%.2f", vector.x, vector.y, vector.z);
    }

    @FunctionalInterface
    public interface EffectApplier {
        boolean apply(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);
    }

    @FunctionalInterface
    public interface IntentRecorder {
        void record(String type, Map<String, Object> data);
    }
}
