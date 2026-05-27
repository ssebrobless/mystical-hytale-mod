package com.motm.observability;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.math.vector.Vector3d;
import com.motm.model.PlayerData;
import com.motm.model.StatusEffect;
import com.motm.util.MotmObservability;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds raw agent-facing runtime snapshots without making the plugin shell own
 * the evidence shape. The hooks are intentionally simple read accessors so new
 * probes can be added here without growing MenteesMod.
 */
public final class MotmObservabilitySnapshotBuilder {

    private final Hooks hooks;

    public MotmObservabilitySnapshotBuilder(Hooks hooks) {
        this.hooks = hooks;
    }

    public Map<String, Object> build(String playerId, String label) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("label", label == null || label.isBlank() ? "snapshot" : label);
        snapshot.put("build", MotmObservability.mapOf(
                "buildChannel", hooks.buildChannel(),
                "internalTestBuild", hooks.internalTestBuild(),
                "devToolsEnabled", hooks.devToolsEnabled(),
                "packetScope", hooks.packetScope()
        ));
        Path pluginDirectory = hooks.pluginDirectory();
        snapshot.put("pluginDirectory", pluginDirectory != null ? pluginDirectory.toString() : null);
        snapshot.put("pending", buildPendingSnapshot());
        Map<String, Object> activeRuntime = hooks.activeRuntimeSnapshot(playerId);
        snapshot.put("activeRuntime", activeRuntime == null ? Map.of() : activeRuntime);

        Player runtimePlayer = hooks.runtimePlayer(playerId);
        PlayerData playerData = playerId == null ? null : hooks.playerData(playerId);
        snapshot.put("playerData", buildPlayerDataSnapshot(playerData));
        snapshot.put("runtimePlayer", buildRuntimePlayerSnapshot(runtimePlayer));
        snapshot.put("statusEffects", buildStatusEffectsSnapshot(playerId));
        snapshot.put("inventory", buildInventorySnapshot(runtimePlayer));
        snapshot.put("trackedTargets", buildTrackedTargetsSnapshot(playerId));
        return snapshot;
    }

    public List<Map<String, Object>> nativeEntityEffectsSnapshot(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return List.of();
        }
        EffectControllerComponent controller = store.getComponent(ref, EffectControllerComponent.getComponentType());
        if (controller == null) {
            return List.of();
        }
        List<Map<String, Object>> effects = new ArrayList<>();
        ActiveEntityEffect[] activeEffects = controller.getAllActiveEntityEffects();
        if (activeEffects == null) {
            return effects;
        }
        for (ActiveEntityEffect effect : activeEffects) {
            if (effect == null) {
                continue;
            }
            EntityEffect asset = EntityEffect.getAssetMap().getAsset(effect.getEntityEffectIndex());
            effects.add(MotmObservability.mapOf(
                    "entityEffectIndex", effect.getEntityEffectIndex(),
                    "entityEffectId", asset != null ? asset.getId() : null,
                    "name", asset != null ? asset.getName() : null,
                    "initialDuration", effect.getInitialDuration(),
                    "remainingDuration", effect.getRemainingDuration(),
                    "infinite", effect.isInfinite(),
                    "debuff", effect.isDebuff(),
                    "invulnerable", effect.isInvulnerable()
            ));
        }
        return effects;
    }

    private Map<String, Object> buildPendingSnapshot() {
        Map<String, Object> pending = new LinkedHashMap<>();
        Map<String, Object> runtimeTasks = hooks.runtimeTasksSnapshot();
        if (runtimeTasks != null) {
            pending.putAll(runtimeTasks);
        }
        pending.put("onlineRuntimePlayers", hooks.onlineRuntimePlayerCount());
        pending.put("activeProofSelections", hooks.activeProofSelections());
        pending.put("activeProofProxies", hooks.activeProofProxies());
        pending.put("activeStyleTests", hooks.activeStyleTests());
        pending.put("freeCastPlayers", hooks.freeCastPlayerCount());
        return pending;
    }

    private Map<String, Object> buildPlayerDataSnapshot(PlayerData player) {
        if (player == null) {
            return Map.of("present", false);
        }
        return MotmObservability.mapOf(
                "present", true,
                "playerId", player.getPlayerId(),
                "playerName", player.getPlayerName(),
                "classId", player.getPlayerClass(),
                "raceId", player.getRace(),
                "selectedStyles", new ArrayList<>(player.getSelectedStyles()),
                "level", player.getLevel(),
                "currentXp", player.getCurrentXp(),
                "totalXpEarned", player.getTotalXpEarned(),
                "selectedPerkCount", player.getSelectedPerks().size(),
                "activeSynergyCount", player.getActiveSynergyBonuses().size(),
                "freeCast", hooks.freeCastEnabled(player.getPlayerId())
        );
    }

    private Map<String, Object> buildRuntimePlayerSnapshot(Player player) {
        if (player == null) {
            return Map.of("present", false);
        }

        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("present", true);
        runtime.put("playerId", hooks.runtimePlayerId(player));
        PlayerRef playerRef = hooks.universePlayerRef(player);
        runtime.put("username", playerRef != null ? playerRef.getUsername() : null);
        runtime.put("uuid", playerRef != null && playerRef.getUuid() != null ? playerRef.getUuid().toString() : null);
        runtime.put("world", player.getWorld() != null ? player.getWorld().getName() : "unknown");
        runtime.put("gameMode", String.valueOf(player.getGameMode()));

        Ref<EntityStore> ref = player.getReference();
        runtime.put("ref", buildRefSnapshot(ref));
        Store<EntityStore> store = ref != null && ref.isValid() ? ref.getStore() : null;
        runtime.put("position", vectorSnapshot(playerPosition(player)));
        runtime.put("forward", vectorSnapshot(normalizeHorizontal(playerForward(player))));
        runtime.put("velocity", buildVelocitySnapshot(store, ref));
        runtime.put("movement", buildMovementSnapshot(store, ref));
        runtime.put("stats", buildStatsSnapshot(store, ref));
        runtime.put("nativeEntityEffects", nativeEntityEffectsSnapshot(store, ref));
        return runtime;
    }

    private Map<String, Object> buildRefSnapshot(Ref<EntityStore> ref) {
        if (ref == null) {
            return Map.of("present", false);
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("present", true);
        snapshot.put("valid", ref.isValid());
        snapshot.put("index", ref.isValid() ? ref.getIndex() : -1);
        try {
            Store<EntityStore> store = ref.isValid() ? ref.getStore() : null;
            UUIDComponent uuid = store != null ? store.getComponent(ref, UUIDComponent.getComponentType()) : null;
            snapshot.put("uuid", uuid != null && uuid.getUuid() != null ? uuid.getUuid().toString() : null);
        } catch (Throwable e) {
            snapshot.put("uuidError", e.getMessage());
        }
        return snapshot;
    }

    private Map<String, Object> buildVelocitySnapshot(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return Map.of("present", false);
        }
        Velocity velocity = store.getComponent(ref, Velocity.getComponentType());
        if (velocity == null || velocity.getVelocity() == null) {
            return Map.of("present", false);
        }
        Map<String, Object> snapshot = vectorSnapshot(velocity.getVelocity());
        snapshot.put("present", true);
        return snapshot;
    }

    private Map<String, Object> buildMovementSnapshot(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return Map.of("present", false);
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("present", true);

        MovementStatesComponent statesComponent = store.getComponent(ref, MovementStatesComponent.getComponentType());
        List<String> activeStates = new ArrayList<>();
        if (statesComponent != null && statesComponent.getMovementStates() != null) {
            var states = statesComponent.getMovementStates();
            if (states.idle) activeStates.add("idle");
            if (states.horizontalIdle) activeStates.add("horizontalIdle");
            if (states.jumping) activeStates.add("jumping");
            if (states.flying) activeStates.add("flying");
            if (states.walking) activeStates.add("walking");
            if (states.running) activeStates.add("running");
            if (states.sprinting) activeStates.add("sprinting");
            if (states.crouching) activeStates.add("crouching");
            if (states.falling) activeStates.add("falling");
            if (states.onGround) activeStates.add("onGround");
            if (states.swimming) activeStates.add("swimming");
            if (states.gliding) activeStates.add("gliding");
        }
        snapshot.put("states", activeStates);

        MovementManager movementManager = store.getComponent(ref, MovementManager.getComponentType());
        if (movementManager != null && movementManager.getSettings() != null) {
            var settings = movementManager.getSettings();
            snapshot.put("settings", MotmObservability.mapOf(
                    "baseSpeed", settings.baseSpeed,
                    "forwardWalkSpeedMultiplier", settings.forwardWalkSpeedMultiplier,
                    "strafeWalkSpeedMultiplier", settings.strafeWalkSpeedMultiplier,
                    "forwardRunSpeedMultiplier", settings.forwardRunSpeedMultiplier,
                    "strafeRunSpeedMultiplier", settings.strafeRunSpeedMultiplier,
                    "forwardSprintSpeedMultiplier", settings.forwardSprintSpeedMultiplier,
                    "minSpeedMultiplier", settings.minSpeedMultiplier,
                    "maxSpeedMultiplier", settings.maxSpeedMultiplier,
                    "acceleration", settings.acceleration,
                    "canFly", settings.canFly
            ));
        }
        return snapshot;
    }

    private Map<String, Object> buildStatsSnapshot(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return Map.of("present", false);
        }
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            return Map.of("present", false);
        }
        return MotmObservability.mapOf(
                "present", true,
                "health", statSnapshot(statMap, DefaultEntityStatTypes.getHealth()),
                "stamina", statSnapshot(statMap, DefaultEntityStatTypes.getStamina()),
                "mana", statSnapshot(statMap, DefaultEntityStatTypes.getMana()),
                "signatureEnergy", statSnapshot(statMap, DefaultEntityStatTypes.getSignatureEnergy())
        );
    }

    private Map<String, Object> statSnapshot(EntityStatMap statMap, int statType) {
        EntityStatValue value = statMap != null ? statMap.get(statType) : null;
        if (value == null) {
            return Map.of("present", false);
        }
        return MotmObservability.mapOf(
                "present", true,
                "id", value.getId(),
                "index", value.getIndex(),
                "current", value.get(),
                "min", value.getMin(),
                "max", value.getMax(),
                "modifierCount", value.getModifiers() != null ? value.getModifiers().size() : 0
        );
    }

    private List<Map<String, Object>> buildStatusEffectsSnapshot(String playerId) {
        if (playerId == null) {
            return List.of();
        }
        List<StatusEffect> currentEffects = hooks.statusEffects(playerId);
        if (currentEffects == null || currentEffects.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> effects = new ArrayList<>();
        for (StatusEffect effect : currentEffects) {
            if (effect == null) {
                continue;
            }
            effects.add(MotmObservability.mapOf(
                    "type", String.valueOf(effect.getType()),
                    "remainingTicks", effect.getRemainingTicks(),
                    "initialDurationTicks", effect.getInitialDurationTicks(),
                    "value", effect.getValue(),
                    "source", effect.getSourcePerkOrAbility(),
                    "expired", effect.isExpired()
            ));
        }
        return effects;
    }

    private Map<String, Object> buildInventorySnapshot(Player player) {
        CombinedItemContainer inventory = hooks.combinedInventory(player);
        if (inventory == null) {
            return Map.of("present", false);
        }
        Map<String, Integer> itemCounts = new LinkedHashMap<>();
        inventory.forEach((slot, stack) -> {
            if (stack == null || stack.getItemId() == null || stack.getItemId().isBlank()) {
                return;
            }
            itemCounts.merge(stack.getItemId(), Math.max(0, stack.getQuantity()), Integer::sum);
        });
        return MotmObservability.mapOf(
                "present", true,
                "uniqueItemIds", itemCounts.size(),
                "itemCounts", itemCounts
        );
    }

    private List<Map<String, Object>> buildTrackedTargetsSnapshot(String playerId) {
        if (playerId == null) {
            return List.of();
        }
        List<Ref<EntityStore>> targets = hooks.trackedTargets(playerId);
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Ref<EntityStore> target : targets) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ref", buildRefSnapshot(target));
            Store<EntityStore> store = target != null && target.isValid() ? target.getStore() : null;
            NPCEntity npc = store != null ? store.getComponent(target, NPCEntity.getComponentType()) : null;
            row.put("npc", npc == null
                    ? Map.of("present", false)
                    : MotmObservability.mapOf(
                    "present", true,
                    "roleName", npc.getRoleName(),
                    "npcTypeId", npc.getNPCTypeId(),
                    "despawning", npc.isDespawning(),
                    "despawnTime", npc.getDespawnTime()
            ));
            row.put("position", vectorSnapshot(entityPosition(store, target)));
            row.put("stats", buildStatsSnapshot(store, target));
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> vectorSnapshot(Vector3d vector) {
        if (vector == null) {
            return Map.of("present", false);
        }
        return MotmObservability.mapOf(
                "present", true,
                "x", vector.x,
                "y", vector.y,
                "z", vector.z
        );
    }

    private Vector3d playerPosition(Player player) {
        if (player == null) {
            return null;
        }
        var playerRef = player.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return null;
        }
        return entityPosition(playerRef.getStore(), playerRef);
    }

    private Vector3d playerForward(Player player) {
        if (player == null) {
            return null;
        }

        var playerRef = player.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return null;
        }

        TransformComponent transform = playerRef.getStore().getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getDirection() == null) {
            return new Vector3d(0.0, 0.0, 1.0);
        }

        Vector3d direction = transform.getTransform().getDirection().clone();
        if (!direction.isFinite() || direction.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        return direction;
    }

    private Vector3d normalizeHorizontal(Vector3d direction) {
        if (direction == null) {
            return new Vector3d(0.0, 0.0, 1.0);
        }

        Vector3d horizontal = new Vector3d(direction.x, 0.0, direction.z);
        if (!horizontal.isFinite() || horizontal.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        horizontal.normalize();
        return horizontal;
    }

    private Vector3d entityPosition(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return null;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }

        return transform.getTransform().getPosition();
    }

    public interface Hooks {
        String buildChannel();

        boolean internalTestBuild();

        boolean devToolsEnabled();

        String packetScope();

        Path pluginDirectory();

        Map<String, Object> runtimeTasksSnapshot();

        int onlineRuntimePlayerCount();

        int activeProofSelections();

        int activeProofProxies();

        int activeStyleTests();

        int freeCastPlayerCount();

        Map<String, Object> activeRuntimeSnapshot(String playerId);

        Player runtimePlayer(String playerId);

        PlayerData playerData(String playerId);

        boolean freeCastEnabled(String playerId);

        List<StatusEffect> statusEffects(String playerId);

        CombinedItemContainer combinedInventory(Player player);

        List<Ref<EntityStore>> trackedTargets(String playerId);

        String runtimePlayerId(Player player);

        PlayerRef universePlayerRef(Player player);
    }
}
