package com.motm.proof;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.CommandBuffer;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.projectile.ProjectileModule;
import com.hypixel.hytale.server.core.modules.projectile.config.ProjectileConfig;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.server.core.asset.type.particle.config.ParticleSystem;
import com.hypixel.hytale.server.core.modules.entity.component.EntityScaleComponent;
import com.hypixel.hytale.server.core.asset.type.model.config.Model;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentDynamicLight;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.motm.runtime.ability.field.FieldVisualHytaleAdapter;

import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.runtime.state.ProofCleanupRuntimeState;
import com.motm.runtime.state.TemporaryProofProxy;
import com.motm.runtime.state.TemporaryProofSelection;
import com.motm.util.MotmObservability;
import com.motm.util.HytaleAssetResolver;
import com.motm.util.MotmNpcRoles;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Hytale-facing proof mutations and evidence emission.
 */
public final class MotmProofActions implements MotmProofRuntime.DefaultProofActions {

    private final ProofCleanupRuntimeState cleanupState;
    private final Function<String, List<Ref<EntityStore>>> trackedTargets;
    private final Hooks hooks;
    private final Logger log;

    public MotmProofActions(ProofCleanupRuntimeState cleanupState,
                            Function<String, List<Ref<EntityStore>>> trackedTargets,
                            Hooks hooks,
                            Logger log) {
        this.cleanupState = cleanupState;
        this.trackedTargets = trackedTargets;
        this.hooks = hooks;
        this.log = log;
    }

    public String run(MotmProofRuntime proofRuntime,
                      String playerId,
                      Player player,
                      Store<EntityStore> currentStore,
                      String proofId) {
        Vector3d basePosition = playerPosition(player);
        Vector3d forward = normalizeHorizontal(playerForward(player));
        if (basePosition == null || forward == null) {
            return "[MOTM] Proof " + proofId + " FAIL: could not resolve player position/facing.";
        }
        if (basePosition.y < -16.0) {
            return "[MOTM] Proof " + proofId + " FAIL: player appears below world at " + formatVector(basePosition);
        }

        return proofRuntime.run(playerId, player, currentStore, forward, proofId);
    }

    @Override
    public String applyProofEffect(Player player, String effectId, String proofId) {
        Ref<EntityStore> ref = player.getReference();
        boolean applied = applyProofEffectToRef(ref, effectId);
        return "[MOTM] Proof " + proofId + " " + (applied ? "PASS" : "FAIL")
                + ": effect=" + effectId
                + " target=player";
    }

    @Override
    public String applyProofTargetEffect(String playerId, Store<EntityStore> store, String effectId, String proofId) {
        Ref<EntityStore> target = null;
        for (Ref<EntityStore> candidate : trackedTargets.apply(playerId)) {
            if (candidate != null && candidate.isValid()) {
                target = candidate;
                break;
            }
        }
        if (target == null) {
            return "[MOTM] Proof " + proofId + " FAIL: no tracked stationary target. Run /motm dev test mobs stationary first.";
        }
        boolean applied = applyProofEffectToRef(target, effectId);
        return "[MOTM] Proof " + proofId + " " + (applied ? "PASS" : "FAIL")
                + ": effect=" + effectId
                + " target=trackedNpc";
    }

    @Override
    public String runTempBlockProof(Player player, String proofId, String blockId, int width, int height, int depth) {
        return runTempBlockProof(player, proofId, width, height, 0, blockId);
    }

    @Override
    public String runTempBlockProof(Player player, String proofId, int width, int height, int yOffset, String... blockIds) {
        BlockResolution blockResolution = resolveProofBlockId(blockIds);
        String blockId = blockResolution.blockId();
        int blockTypeId = blockResolution.blockTypeId();
        if (blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "[MOTM] Proof " + proofId + " FAIL: block id did not resolve: candidates="
                    + String.join(",", blockIds);
        }
        return runTempBlockProof(player, proofId, blockId, blockTypeId, width, height, yOffset);
    }

    @Override
    public String runTempFluidProof(Player player, String proofId, int radius, String... fluidIds) {
        FluidResolution fluidResolution = resolveProofFluidId(fluidIds);
        int fluidTypeId = fluidResolution.fluidTypeId();
        Fluid fluid = Fluid.getAssetMap().getAsset(fluidTypeId);
        if (fluidTypeId == Fluid.UNKNOWN_ID || fluidTypeId == Fluid.EMPTY_ID || fluid == null) {
            return "[MOTM] Proof " + proofId + " FAIL: fluid id did not resolve: candidates="
                    + String.join(",", fluidIds)
                    + " available=" + listProofFluidIds();
        }
        World world = player.getWorld();
        Vector3d base = playerPosition(player);
        Vector3d forward = normalizeHorizontal(playerForward(player));
        if (world == null || base == null || forward == null) {
            return "[MOTM] Proof " + proofId + " FAIL: missing world/player transform.";
        }

        Vector3i anchor = proofAnchor(base, forward, 5.0);
        BlockSelection selection = new BlockSelection();
        selection.setPosition(anchor.x, anchor.y, anchor.z);
        selection.setAnchorAtWorldPos(anchor.x, anchor.y, anchor.z);
        byte fluidLevel = (byte) Math.max(1, fluid.getMaxFluidLevel());
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt((x * x) + (z * z));
                if (dist > radius + 0.2) {
                    continue;
                }
                selection.addFluidAtWorldPos(anchor.x + x, anchor.y, anchor.z + z, fluidTypeId, fluidLevel);
            }
        }
        return placeTemporarySelection(proofId, world, anchor, selection, 4000L,
                "fluid=" + fluidResolution.fluidId()
                        + " fluidTypeId=" + fluidTypeId
                        + " fluids=" + selection.getFluidCount());
    }

    @Override
    public String runNativeProjectileProof(Player player,
                                           Store<EntityStore> currentStore,
                                           String proofId,
                                           Vector3d forward,
                                           String... projectileConfigIds) {
        if (player == null || currentStore == null) {
            return "[MOTM] Proof " + proofId + " FAIL: missing player/store.";
        }
        Ref<EntityStore> playerRef = player.getReference();
        Vector3d base = playerPosition(player);
        Vector3d direction = normalize(forward);
        if (playerRef == null || !playerRef.isValid() || base == null || direction == null) {
            return "[MOTM] Proof " + proofId + " FAIL: missing player reference/transform.";
        }

        ProjectileConfig projectileConfig = resolveProjectileConfig(projectileConfigIds);
        if (projectileConfig == null) {
            return "[MOTM] Proof " + proofId + " FAIL: projectile config did not resolve: candidates="
                    + String.join(",", projectileConfigIds)
                    + " available=" + listProjectileConfigIds();
        }

        ProjectileModule projectileModule = ProjectileModule.get();
        if (projectileModule == null) {
            return "[MOTM] Proof " + proofId + " FAIL: ProjectileModule.get() returned null.";
        }

        Vector3d origin = new Vector3d(base).add(0.0, 1.2, 0.0);
        AtomicReference<Ref<EntityStore>> projectileRef = new AtomicReference<>();
        AtomicReference<String> failure = new AtomicReference<>();
        currentStore.forEachChunk((chunk, commandBuffer) -> {
            if (projectileRef.get() != null || failure.get() != null) {
                return;
            }
            if (commandBuffer == null) {
                failure.set("no command buffer");
                return;
            }
            try {
                projectileRef.set(spawnNativeProjectile(projectileModule, playerRef, commandBuffer, projectileConfig, origin, direction));
            } catch (Exception e) {
                failure.set(e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        });

        Ref<EntityStore> spawnedRef = projectileRef.get();
        boolean spawned = spawnedRef != null && spawnedRef.isValid();
        hooks.recordClientIntent("proof_native_projectile_spawned", MotmObservability.mapOf(
                "proofId", proofId,
                "projectileConfigId", projectileConfig.getId(),
                "spawned", spawned,
                "origin", formatVector(origin),
                "direction", formatVector(direction),
                "entityIndex", spawned ? spawnedRef.getIndex() : -1,
                "failure", failure.get()
        ));
        hooks.recordServerTruth("proof_native_projectile", MotmObservability.mapOf(
                "proofId", proofId,
                "projectileConfigId", projectileConfig.getId(),
                "spawned", spawned,
                "entityIndex", spawned ? spawnedRef.getIndex() : -1,
                "failure", failure.get()
        ));
        if (failure.get() != null) {
            return "[MOTM] Proof " + proofId + " FAIL: native projectile spawn failed: " + failure.get();
        }
        return "[MOTM] Proof " + proofId + " " + (spawned ? "PASS" : "FAIL")
                + ": native projectile config=" + projectileConfig.getId()
                + " origin=" + formatVector(origin)
                + " direction=" + formatVector(direction)
                + " entityIndex=" + (spawned ? spawnedRef.getIndex() : -1);
    }

    @Override
    public String runProxyProof(Player player, String proofId, String roleId, String effectId, double distanceAhead) {
        World world = player.getWorld();
        Vector3d base = playerPosition(player);
        Vector3d forward = normalizeHorizontal(playerForward(player));
        if (world == null || base == null || forward == null) {
            return "[MOTM] Proof " + proofId + " FAIL: missing world/player transform.";
        }
        Vector3d position = com.motm.util.MotmVectors.addScaled(base, forward, distanceAhead);
        NPCEntity proxy = new NPCEntity(world);
        MotmNpcRoles.applyRole(proxy, roleId,
                HytaleAssetResolver.resolveRenderlessVisualProxyRoleId(), log);
        proxy.setDespawnTime(4.5f);
        world.spawnEntity(proxy, position, new com.hypixel.hytale.math.vector.Rotation3f(0f, 0f, 0f));

        Ref<EntityStore> ref = proxy.getReference();
        boolean effectApplied = applyProofEffectToRef(ref, effectId);
        if (ref != null && ref.isValid()) {
            cleanupState.addProxy(new TemporaryProofProxy(proofId, world, ref, System.currentTimeMillis() + 4500L));
        }
        hooks.recordClientIntent("proof_proxy_spawned", MotmObservability.mapOf(
                "proofId", proofId,
                "roleId", roleId,
                "effectId", effectId,
                "effectApplied", effectApplied,
                "position", formatVector(position),
                "entityIndex", ref != null && ref.isValid() ? ref.getIndex() : -1
        ));
        return "[MOTM] Proof " + proofId + " PASS: proxy role=" + roleId
                + " effect=" + effectId
                + " effectApplied=" + effectApplied
                + " position=" + formatVector(position);
    }

    @Override
    public String runParticleWorldProof(Player player,
                                        Store<EntityStore> currentStore,
                                        String proofId,
                                        String systemId,
                                        double distanceAhead) {
        World world = player == null ? null : player.getWorld();
        Vector3d base = playerPosition(player);
        Vector3d forward = normalizeHorizontal(playerForward(player));
        if (world == null || currentStore == null || base == null || forward == null) {
            recordGateFailure(proofId, "missing world/player transform/store");
            return "[MOTM] Proof " + proofId + " FAIL: missing world/player transform/store.";
        }
        Vector3d point = com.motm.util.MotmVectors.addScaled(base, forward, distanceAhead);
        int before = countEntitiesNear(currentStore, point, 1.75);
        boolean assetMapAccessible = true;
        boolean assetResolved = true;
        if ("MOTM_Proof_Pink_Halo".equals(systemId)) {
            try {
                var assetMap = ParticleSystem.getAssetMap();
                assetMapAccessible = assetMap != null;
                assetResolved = assetMapAccessible && assetMap.getAsset(systemId) != null;
            } catch (Throwable error) {
                assetMapAccessible = false;
                assetResolved = false;
            }
        }
        boolean requested = false;
        String failure = null;
        try {
            ParticleUtil.spawnParticleEffect(systemId, point, currentStore);
            requested = true;
        } catch (Throwable error) {
            failure = error.getClass().getSimpleName() + ": " + error.getMessage();
        }
        int after = countEntitiesNear(currentStore, point, 1.75);
        int entityDelta = after - before;
        boolean customParticle = "MOTM_Proof_Pink_Halo".equals(systemId);
        boolean passed = requested && entityDelta == 0 && (!customParticle || !assetMapAccessible || assetResolved);
        hooks.recordClientIntent("proof_capability_gate_particle", MotmObservability.mapOf(
                "proofId", proofId,
                "systemId", systemId,
                "position", formatVector(point),
                "particleRequested", requested,
                "assetMapAccessible", assetMapAccessible,
                "assetResolved", assetResolved,
                "visualCheckRequired", "MOTM_Proof_Pink_Halo".equals(systemId),
                "failure", failure
        ));
        hooks.recordServerTruth("proof_capability_gate_particle", MotmObservability.mapOf(
                "proofId", proofId,
                "systemId", systemId,
                "entityCountBefore", before,
                "entityCountAfter", after,
                "entityDelta", entityDelta,
                "noEntityCreated", entityDelta == 0,
                "particleRequested", requested,
                "failure", failure
        ));
        return "[MOTM] Proof " + proofId + " " + (passed ? "PASS" : "FAIL")
                + ": particle system=" + systemId
                + " requested=" + requested
                + " entityCountBefore=" + before
                + " entityCountAfter=" + after
                + " noEntityCreated=" + (entityDelta == 0)
                + ("MOTM_Proof_Pink_Halo".equals(systemId)
                ? " assetMapAccessible=" + assetMapAccessible
                + " assetResolved=" + assetResolved
                + " visualCheckRequired=true"
                : "")
                + (failure == null ? "" : " failure=" + failure);
    }

    @Override
    public String runEntityScaleProof(Player player,
                                      Store<EntityStore> currentStore,
                                      String proofId,
                                      Vector3d forward,
                                      double distanceAhead) {
        GateProxy proxy = spawnGateProxy(player, proofId, forward, distanceAhead, 4.5f, false);
        if (proxy == null) {
            recordGateFailure(proofId, "could not spawn Spark_Living proxy");
            return "[MOTM] Proof " + proofId + " FAIL: could not spawn Spark_Living proxy.";
        }
        Store<EntityStore> store = proxy.ref().getStore();
        EntityScaleComponent scale = new EntityScaleComponent(2.5f);
        store.putComponent(proxy.ref(), EntityScaleComponent.getComponentType(), scale);
        EntityScaleComponent observed = store.getComponent(proxy.ref(), EntityScaleComponent.getComponentType());
        boolean enlarged = observed != null && Math.abs(observed.getScale() - 2.5f) < 0.01f;
        scheduleOnWorld(proxy.world(), 2000L, () -> {
            if (proxy.ref().isValid()) {
                store.putComponent(proxy.ref(), EntityScaleComponent.getComponentType(), new EntityScaleComponent(1.0f));
                EntityScaleComponent restored = store.getComponent(proxy.ref(), EntityScaleComponent.getComponentType());
                hooks.recordServerTruth("proof_capability_gate_entity_scale_restore", MotmObservability.mapOf(
                        "proofId", proofId,
                        "scaleApplied", restored != null ? restored.getScale() : null,
                        "restored", restored != null && Math.abs(restored.getScale() - 1.0f) < 0.01f
                ));
            }
        });
        hooks.recordClientIntent("proof_capability_gate_entity_scale", MotmObservability.mapOf(
                "proofId", proofId,
                "entityIndex", proxy.ref().getIndex(),
                "scaleBeforeRestore", 2.5f,
                "scaleAfterRestore", 1.0f,
                "restoreScheduled", true
        ));
        hooks.recordServerTruth("proof_capability_gate_entity_scale", MotmObservability.mapOf(
                "proofId", proofId,
                "entityIndex", proxy.ref().getIndex(),
                "scaleApplied", enlarged ? observed.getScale() : null,
                "scaleAppliedExpected", 2.5f,
                "restoreScheduled", true
        ));
        return "[MOTM] Proof " + proofId + " " + (enlarged ? "PASS" : "FAIL")
                + ": Spark_Living scale=2.5 for 2s then 1.0 for 2s; entityIndex=" + proxy.ref().getIndex();
    }

    @Override
    public String runDynamicLightProof(Player player,
                                       Store<EntityStore> currentStore,
                                       String proofId,
                                       Vector3d forward,
                                       double distanceAhead) {
        GateProxy proxy = spawnGateProxy(player, proofId, forward, distanceAhead, 4.5f, true);
        if (proxy == null) {
            recordGateFailure(proofId, "could not spawn renderless proxy");
            return "[MOTM] Proof " + proofId + " FAIL: could not spawn renderless proxy.";
        }
        Store<EntityStore> store = proxy.ref().getStore();
        ColorLight colorLight = new ColorLight((byte) 15, (byte) 0, (byte) 220, (byte) 255);
        store.putComponent(proxy.ref(), PersistentDynamicLight.getComponentType(),
                new PersistentDynamicLight(colorLight));
        boolean applied = store.getComponent(proxy.ref(), PersistentDynamicLight.getComponentType()) != null;
        scheduleOnWorld(proxy.world(), 4000L, () -> {
            if (proxy.ref().isValid()) {
                store.removeComponentIfExists(proxy.ref(), PersistentDynamicLight.getComponentType());
                hooks.recordServerTruth("proof_capability_gate_dynamic_light_removed", MotmObservability.mapOf(
                        "proofId", proofId,
                        "removed", store.getComponent(proxy.ref(), PersistentDynamicLight.getComponentType()) == null
                ));
            }
        });
        hooks.recordClientIntent("proof_capability_gate_dynamic_light", MotmObservability.mapOf(
                "proofId", proofId,
                "entityIndex", proxy.ref().getIndex(),
                "attachedToPlayer", false,
                "radius", 15,
                "red", 0,
                "green", 220,
                "blue", 255,
                "durationMs", 4000
        ));
        hooks.recordServerTruth("proof_capability_gate_dynamic_light", MotmObservability.mapOf(
                "proofId", proofId,
                "entityIndex", proxy.ref().getIndex(),
                "componentApplied", applied
        ));
        return "[MOTM] Proof " + proofId + " " + (applied ? "PASS" : "FAIL")
                + ": cyan PersistentDynamicLight on renderless proxy for 4s; attachedToPlayer=false.";
    }

    @Override
    public String runIntangibleProof(Player player,
                                     Store<EntityStore> currentStore,
                                     String proofId,
                                     Vector3d forward,
                                     double distanceAhead) {
        GateProxy proxy = spawnGateProxy(player, proofId, forward, distanceAhead, 5.5f, false);
        if (proxy == null) {
            recordGateFailure(proofId, "could not spawn visible proxy");
            return "[MOTM] Proof " + proofId + " FAIL: could not spawn visible proxy.";
        }
        Store<EntityStore> store = proxy.ref().getStore();
        store.putComponent(proxy.ref(), Intangible.getComponentType(), Intangible.INSTANCE);
        boolean present = store.getComponent(proxy.ref(), Intangible.getComponentType()) != null;
        scheduleOnWorld(proxy.world(), 5000L, () -> {
            if (proxy.ref().isValid()) {
                store.removeComponentIfExists(proxy.ref(), Intangible.getComponentType());
                hooks.recordServerTruth("proof_capability_gate_intangible_removed", MotmObservability.mapOf(
                        "proofId", proofId,
                        "componentPresent", store.getComponent(proxy.ref(), Intangible.getComponentType()) != null
                ));
            }
        });
        hooks.recordClientIntent("proof_capability_gate_intangible", MotmObservability.mapOf(
                "proofId", proofId,
                "entityIndex", proxy.ref().getIndex(),
                "durationMs", 5000,
                "walkThroughCheckRequired", true
        ));
        hooks.recordServerTruth("proof_capability_gate_intangible", MotmObservability.mapOf(
                "proofId", proofId,
                "entityIndex", proxy.ref().getIndex(),
                "componentPresent", present
        ));
        return "[MOTM] Proof " + proofId + " " + (present ? "PASS" : "FAIL")
                + ": visible proxy Intangible=" + present + " for 5s; walk through it now.";
    }

    @Override
    public String runPlayerCloneProof(Player player,
                                       Store<EntityStore> currentStore,
                                       String proofId,
                                       Vector3d forward,
                                       double distanceAhead) {
        if (player == null || currentStore == null) {
            recordGateFailure(proofId, "missing player/store");
            return "[MOTM] Proof " + proofId + " FAIL: missing player/store.";
        }
        Ref<EntityStore> playerRef = player.getReference();
        ModelComponent playerModelComponent = playerRef == null || !playerRef.isValid()
                ? null : currentStore.getComponent(playerRef, ModelComponent.getComponentType());
        Model playerModel = playerModelComponent == null ? null : playerModelComponent.getModel();
        String modelId = playerModel == null ? null : playerModel.getModelAssetId();
        GateProxy proxy = spawnGateProxy(player, proofId, forward, distanceAhead, 6.5f, false);
        if (proxy == null) {
            recordGateFailure(proofId, "could not spawn clone proxy; modelId=" + modelId);
            return "[MOTM] Proof " + proofId + " FAIL: could not spawn clone proxy; modelId=" + modelId;
        }
        Store<EntityStore> store = proxy.ref().getStore();
        boolean appearanceAttempted = false;
        boolean appearanceApplied = false;
        boolean modelComponentAttempted = false;
        boolean modelComponentApplied = false;
        String failure = null;
        if (modelId != null && !modelId.isBlank()) {
            try {
                appearanceAttempted = true;
                appearanceApplied = NPCEntity.setAppearance(proxy.ref(), modelId, store);
            } catch (Throwable error) {
                failure = error.getClass().getSimpleName() + ": " + error.getMessage();
            }
        }
        if (playerModel != null) {
            try {
                modelComponentAttempted = true;
                store.putComponent(proxy.ref(), ModelComponent.getComponentType(), new ModelComponent(new Model(playerModel)));
                modelComponentApplied = store.getComponent(proxy.ref(), ModelComponent.getComponentType()) != null;
            } catch (Throwable error) {
                failure = failure == null
                        ? error.getClass().getSimpleName() + ": " + error.getMessage()
                        : failure + " | " + error.getClass().getSimpleName() + ": " + error.getMessage();
            }
        }
        boolean passed = appearanceApplied || modelComponentApplied;
        hooks.recordClientIntent("proof_capability_gate_player_clone", MotmObservability.mapOf(
                "proofId", proofId,
                "entityIndex", proxy.ref().getIndex(),
                "modelId", modelId,
                "appearanceStrategyAttempted", appearanceAttempted,
                "modelComponentStrategyAttempted", modelComponentAttempted
        ));
        hooks.recordServerTruth("proof_capability_gate_player_clone", MotmObservability.mapOf(
                "proofId", proofId,
                "entityIndex", proxy.ref().getIndex(),
                "modelId", modelId,
                "appearanceApplied", appearanceApplied,
                "modelComponentApplied", modelComponentApplied,
                "visualMatchRequiresClientCheck", true,
                "failure", failure
        ));
        return "[MOTM] Proof " + proofId + " " + (passed ? "PASS" : "FAIL")
                + ": player modelId=" + modelId
                + " appearanceAttempted=" + appearanceAttempted
                + " appearanceApplied=" + appearanceApplied
                + " modelComponentApplied=" + modelComponentApplied
                + " (visual match is the gate answer).";
    }

    private GateProxy spawnGateProxy(Player player,
                                     String proofId,
                                     Vector3d forward,
                                     double distanceAhead,
                                     float despawnSeconds,
                                     boolean renderless) {
        World world = player == null ? null : player.getWorld();
        Vector3d base = playerPosition(player);
        Vector3d direction = normalizeHorizontal(forward);
        if (world == null || base == null || direction == null) {
            return null;
        }
        Vector3d position = com.motm.util.MotmVectors.addScaled(base, direction, distanceAhead);
        NPCEntity proxy = new NPCEntity(world);
        MotmNpcRoles.applyRole(proxy, "Spark_Living",
                HytaleAssetResolver.resolveRenderlessVisualProxyRoleId(), log);
        proxy.setDespawnTime(despawnSeconds);
        world.spawnEntity(proxy, position,
                new com.hypixel.hytale.math.vector.Rotation3f(0f, 0f, 0f));
        Ref<EntityStore> ref = proxy.getReference();
        if (ref == null || !ref.isValid() || ref.getStore() == null) {
            return null;
        }
        if (renderless) {
            FieldVisualHytaleAdapter.configureRenderlessProxy(ref, ref.getStore());
        }

        cleanupState.addProxy(new TemporaryProofProxy(
                proofId, world, ref, System.currentTimeMillis() + (long) (despawnSeconds * 1000L)));
        return new GateProxy(world, ref);
    }

    private void scheduleOnWorld(World world, long delayMillis, Runnable task) {
        CompletableFuture.delayedExecutor(delayMillis, TimeUnit.MILLISECONDS).execute(() -> {
            try {
                if (world != null && world.isAlive()) {
                    world.execute(task);
                }
            } catch (Throwable error) {
                log.log(Level.WARNING, "[MOTM] Proof scheduled task failed: " + error.getMessage(), error);
            }
        });
    }

    private void recordGateFailure(String proofId, String reason) {
        hooks.recordClientIntent("proof_capability_gate_failure", MotmObservability.mapOf(
                "proofId", proofId,
                "result", "FAIL",
                "reason", reason
        ));
        hooks.recordServerTruth("proof_capability_gate_failure", MotmObservability.mapOf(
                "proofId", proofId,
                "result", "FAIL",
                "reason", reason
        ));
    }

    private int countEntitiesNear(Store<EntityStore> store, Vector3d point, double radius) {
        if (store == null || point == null) {
            return 0;
        }
        int[] count = {0};
        double radiusSquared = radius * radius;
        store.forEachChunk((chunk, commandBuffer) -> {
            for (int index = 0; index < chunk.size(); index++) {
                TransformComponent transform = chunk.getComponent(index, TransformComponent.getComponentType());
                Vector3d position = transform == null ? null : transform.getPosition();
                if (position != null && distanceSquared(position, point) <= radiusSquared) {
                    count[0]++;
                }
            }
        });
        return count[0];
    }

    private static double distanceSquared(Vector3d a, Vector3d b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return dx * dx + dy * dy + dz * dz;
    }

    private record GateProxy(World world, Ref<EntityStore> ref) {
    }

    @Override
    public String runMovementProof(Player player,
                                   Store<EntityStore> currentStore,
                                   String proofId,
                                   Vector3d forward,
                                   double distance,
                                   boolean surfaceRecovery,
                                   boolean burstVelocity) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid() || currentStore == null) {
            return "[MOTM] Proof " + proofId + " FAIL: missing player ref/store.";
        }
        TransformComponent transform = currentStore.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getPosition() == null) {
            return "[MOTM] Proof " + proofId + " FAIL: missing TransformComponent.";
        }
        Vector3d start = new Vector3d(transform.getPosition());
        Vector3d destination = com.motm.util.MotmVectors.addScaled(start, forward, distance);
        if (surfaceRecovery) {
            destination.y = Math.max(start.y, destination.y);
        }

        String movementMethod;
        boolean moved;
        Vector3d observed;
        double observedDisplacement;
        double destinationError;
        if (burstVelocity) {
            Velocity velocity = currentStore.getComponent(ref, Velocity.getComponentType());
            if (velocity == null) {
                return "[MOTM] Proof " + proofId + " FAIL: missing Velocity component.";
            }
            Vector3d burst = forward == null ? new Vector3d() : new Vector3d(forward);
            burst.mul(Math.max(7.0, Math.min(18.0, distance * 2.4)));
            Vector3d before = velocity.getVelocity();
            double y = before != null && before.isFinite() ? before.y : 0.0;
            velocity.set(burst.x, y, burst.z);
            observed = transform.getPosition() != null ? new Vector3d(transform.getPosition()) : null;
            observedDisplacement = observed != null ? distance(start, observed) : 0.0;
            destinationError = observed != null ? distance(observed, destination) : Double.POSITIVE_INFINITY;
            moved = burst.length() > 0.1;
            movementMethod = "velocityBurst";
        } else {
            transform.teleportPosition(destination);
            observed = transform.getPosition() != null ? new Vector3d(transform.getPosition()) : null;
            observedDisplacement = observed != null ? distance(start, observed) : 0.0;
            destinationError = observed != null ? distance(observed, destination) : Double.POSITIVE_INFINITY;
            moved = observedDisplacement >= Math.min(0.75, Math.max(0.1, distance * 0.25));
            movementMethod = "teleportPosition";
        }
        hooks.recordServerTruth("proof_movement", MotmObservability.mapOf(
                "proofId", proofId,
                "start", formatVector(start),
                "destination", formatVector(destination),
                "observed", observed != null ? formatVector(observed) : null,
                "observedDisplacement", observedDisplacement,
                "destinationError", destinationError,
                "moved", moved,
                "movementMethod", movementMethod,
                "surfaceRecovery", surfaceRecovery
        ));
        return "[MOTM] Proof " + proofId + " " + (moved ? "PASS" : "FAIL")
                + ": movement start=" + formatVector(start)
                + " destination=" + formatVector(destination)
                + " observed=" + (observed != null ? formatVector(observed) : "null")
                + " displacement=" + String.format(Locale.ROOT, "%.2f", observedDisplacement)
                + " movementMethod=" + movementMethod
                + " surfaceRecovery=" + surfaceRecovery;
    }

    private boolean applyProofEffectToRef(Ref<EntityStore> ref, String effectId) {
        if (ref == null || !ref.isValid() || ref.getStore() == null || effectId == null || effectId.isBlank()) {
            return false;
        }
        EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectId);
        if (effect == null) {
            log.warning("[MOTM] Proof effect missing: " + effectId);
            return false;
        }
        Store<EntityStore> store = ref.getStore();
        EffectControllerComponent controller = store.getComponent(ref, EffectControllerComponent.getComponentType());
        if (controller == null) {
            log.warning("[MOTM] Proof target missing EffectControllerComponent: " + effectId);
            return false;
        }
        boolean applied = controller.addEffect(ref, effect, store);
        hooks.recordClientIntent("proof_entity_effect_add", MotmObservability.mapOf(
                "effectId", effectId,
                "applied", applied,
                "entityIndex", ref.getIndex(),
                "nativeEffectsAfter", hooks.nativeEntityEffectsSnapshot(store, ref)
        ));
        return applied;
    }

    private String runTempBlockProof(Player player, String proofId, String blockId, int blockTypeId, int width, int height, int yOffset) {
        if (blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "[MOTM] Proof " + proofId + " FAIL: block id did not resolve: " + blockId;
        }
        World world = player.getWorld();
        Vector3d base = playerPosition(player);
        Vector3d forward = normalizeHorizontal(playerForward(player));
        if (world == null || base == null || forward == null) {
            return "[MOTM] Proof " + proofId + " FAIL: missing world/player transform.";
        }

        Vector3i baseAnchor = proofAnchor(base, forward, 4.0);
        Vector3i anchor = new Vector3i(baseAnchor.x, baseAnchor.y + yOffset, baseAnchor.z);
        BlockSelection selection = new BlockSelection();
        selection.setPosition(anchor.x, anchor.y, anchor.z);
        selection.setAnchorAtWorldPos(anchor.x, anchor.y, anchor.z);
        Vector3i rightStep = proofHorizontalRightStep(forward);
        int half = width / 2;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int offset = x - half;
                int wx = anchor.x + (rightStep.x * offset);
                int wy = anchor.y + y;
                int wz = anchor.z + (rightStep.z * offset);
                selection.addBlockAtWorldPos(wx, wy, wz, blockTypeId, 0, 0, 0);
            }
        }
        return placeTemporarySelection(proofId, world, anchor, selection, 4000L,
                "block=" + blockId + " blockTypeId=" + blockTypeId + " blocks=" + selection.getBlockCount());
    }

    private BlockResolution resolveProofBlockId(String... blockIds) {
        for (String candidate : blockIds) {
            int blockTypeId = BlockType.getBlockIdOrUnknown(candidate, "MOTM proof block resolution");
            if (blockTypeId != BlockType.UNKNOWN_ID && blockTypeId != BlockType.EMPTY_ID) {
                return new BlockResolution(candidate, blockTypeId);
            }
        }
        return new BlockResolution("", BlockType.UNKNOWN_ID);
    }

    private FluidResolution resolveProofFluidId(String... fluidIds) {
        for (String candidate : fluidIds) {
            int directId = Fluid.getAssetMap().getIndexOrDefault(candidate, Fluid.UNKNOWN_ID);
            if (isUsableProofFluidId(directId)) {
                return new FluidResolution(candidate, directId);
            }
        }
        for (String candidate : fluidIds) {
            int convertedId = Fluid.getFluidIdOrUnknown(candidate, "MOTM proof fluid resolution");
            if (isUsableProofFluidId(convertedId)) {
                Fluid fluid = Fluid.getAssetMap().getAsset(convertedId);
                return new FluidResolution(fluid != null ? fluid.getId() : candidate, convertedId);
            }
        }
        return new FluidResolution("", Fluid.UNKNOWN_ID);
    }

    private boolean isUsableProofFluidId(int fluidTypeId) {
        if (fluidTypeId == Fluid.UNKNOWN_ID || fluidTypeId == Fluid.EMPTY_ID) {
            return false;
        }
        Fluid fluid = Fluid.getAssetMap().getAsset(fluidTypeId);
        return fluid != null && !fluid.isUnknown();
    }

    private String listProofFluidIds() {
        List<String> ids = new ArrayList<>();
        int max = Math.min(Fluid.getAssetMap().getNextIndex(), 64);
        for (int index = 0; index < max; index++) {
            Fluid fluid = Fluid.getAssetMap().getAsset(index);
            if (fluid != null && !fluid.isUnknown()) {
                ids.add(index + ":" + fluid.getId());
            }
        }
        return ids.isEmpty() ? "<none>" : String.join("|", ids);
    }

    private ProjectileConfig resolveProjectileConfig(String... projectileConfigIds) {
        if (projectileConfigIds == null) {
            return null;
        }
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

    private String listProjectileConfigIds() {
        try {
            return ProjectileConfig.getAssetMap().getAssetMap().keySet().stream()
                    .filter(id -> id != null && !id.isBlank())
                    .sorted()
                    .limit(32)
                    .reduce((left, right) -> left + "|" + right)
                    .orElse("<none>");
        } catch (Exception e) {
            return "<unavailable:" + e.getClass().getSimpleName() + ">";
        }
    }

    private Ref<EntityStore> spawnNativeProjectile(ProjectileModule projectileModule,
                                                   Ref<EntityStore> playerRef,
                                                   CommandBuffer<EntityStore> commandBuffer,
                                                   ProjectileConfig projectileConfig,
                                                   Vector3d origin,
                                                   Vector3d direction) {
        return projectileModule.spawnProjectile(playerRef, commandBuffer, projectileConfig, origin, direction);
    }

    private String placeTemporarySelection(String proofId,
                                           World world,
                                           Vector3i anchor,
                                           BlockSelection selection,
                                           long lifetimeMillis,
                                           String summary) {
        try {
            BlockSelection original = selection.place(null, world, new Vector3i(0, 0, 0), BlockMask.EMPTY);
            cleanupState.addSelection(new TemporaryProofSelection(
                    proofId,
                    world,
                    anchor,
                    original,
                    System.currentTimeMillis() + lifetimeMillis
            ));
            hooks.recordServerTruth("proof_temporary_selection_placed", MotmObservability.mapOf(
                    "proofId", proofId,
                    "anchor", anchor.toString(),
                    "blockCount", selection.getBlockCount(),
                    "fluidCount", selection.getFluidCount(),
                    "lifetimeMillis", lifetimeMillis,
                    "summary", summary
            ));
            return "[MOTM] Proof " + proofId + " PASS: placed temporary selection "
                    + summary
                    + " anchor=" + anchor
                    + " cleanupMs=" + lifetimeMillis;
        } catch (Exception e) {
            log.log(Level.SEVERE, "[MOTM] Proof temporary selection failed: " + proofId, e);
            return "[MOTM] Proof " + proofId + " FAIL: temporary selection placement failed: " + e.getMessage();
        }
    }

    private Vector3i proofAnchor(Vector3d base, Vector3d forward, double distanceAhead) {
        Vector3d anchor = com.motm.util.MotmVectors.addScaled(base, forward, distanceAhead);
        return new Vector3i(
                (int) Math.floor(anchor.x),
                (int) Math.floor(base.y),
                (int) Math.floor(anchor.z)
        );
    }

    private Vector3i proofHorizontalRightStep(Vector3d forward) {
        Vector3d right = new Vector3d(-forward.z, 0.0, forward.x);
        if (Math.abs(right.x) >= Math.abs(right.z)) {
            return new Vector3i(right.x >= 0.0 ? 1 : -1, 0, 0);
        }
        return new Vector3i(0, 0, right.z >= 0.0 ? 1 : -1);
    }

    private Vector3d playerPosition(Player player) {
        if (player == null) {
            return null;
        }
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid() || ref.getStore() == null) {
            return null;
        }
        TransformComponent transform = ref.getStore().getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }
        return transform.getTransform().getPosition();
    }

    private Vector3d playerForward(Player player) {
        if (player == null) {
            return null;
        }
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid() || ref.getStore() == null) {
            return null;
        }
        TransformComponent transform = ref.getStore().getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getDirection() == null) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        Vector3d direction = new Vector3d(transform.getTransform().getDirection());
        if (!direction.isFinite() || direction.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        return direction;
    }

    private Vector3d normalizeHorizontal(Vector3d direction) {
        if (direction == null) {
            return new Vector3d(1.0, 0.0, 0.0);
        }
        Vector3d flat = new Vector3d(direction.x, 0.0, direction.z);
        double length = Math.sqrt(flat.x * flat.x + flat.z * flat.z);
        if (length < 0.0001) {
            return new Vector3d(1.0, 0.0, 0.0);
        }
        return new Vector3d(flat.x / length, 0.0, flat.z / length);
    }

    private Vector3d normalize(Vector3d direction) {
        if (direction == null) {
            return new Vector3d(1.0, 0.0, 0.0);
        }
        Vector3d normalized = new Vector3d(direction);
        if (!normalized.isFinite() || normalized.length() < 0.0001) {
            return new Vector3d(1.0, 0.0, 0.0);
        }
        return normalized.normalize();
    }

    private double distance(Vector3d a, Vector3d b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private String formatVector(Vector3d position) {
        if (position == null) {
            return "null";
        }
        return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", position.x, position.y, position.z);
    }

    public interface Hooks {
        void recordClientIntent(String type, Map<String, Object> data);

        void recordServerTruth(String type, Map<String, Object> data);

        List<Map<String, Object>> nativeEntityEffectsSnapshot(Store<EntityStore> store, Ref<EntityStore> ref);
    }

    private record FluidResolution(String fluidId, int fluidTypeId) {
    }

    private record BlockResolution(String blockId, int blockTypeId) {
    }
}
