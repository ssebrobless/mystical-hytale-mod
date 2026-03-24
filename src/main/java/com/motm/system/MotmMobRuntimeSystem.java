package com.motm.system;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.MenteesMod;
import com.motm.model.PlayerData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Tracks live NPC entities so the mod can apply scaling on first sight and
 * award progression when the runtime later reports a player-caused death.
 */
public class MotmMobRuntimeSystem extends TickingSystem<EntityStore> {

    private static final Logger LOG = Logger.getLogger("MOTM");
    private static final String HEALTH_MODIFIER_ID = "motm_scaling_health";

    private final MenteesMod mod;
    private final Map<UUID, TrackedMob> trackedMobs = new HashMap<>();
    private final Set<UUID> processedMobDeaths = new HashSet<>();
    private final Set<UUID> processedPlayerDeaths = new HashSet<>();
    private final Set<String> loggedUnknownMobTypes = new HashSet<>();

    public MotmMobRuntimeSystem(MenteesMod mod) {
        this.mod = mod;
    }

    @Override
    public synchronized void tick(float delta, int tick, Store<EntityStore> store) {
        Set<UUID> activeMobIds = new HashSet<>();
        Set<UUID> activePlayerIds = new HashSet<>();
        World world = store.getExternalData().getWorld();

        store.forEachChunk((chunk, ignoredCommandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);

                Player player = chunk.getComponent(entityIndex, Player.getComponentType());
                if (player != null) {
                    DeathComponent death = chunk.getComponent(entityIndex, DeathComponent.getComponentType());
                    UUIDComponent uuidComponent = chunk.getComponent(entityIndex, UUIDComponent.getComponentType());
                    handlePlayerLifecycle(getEntityId(uuidComponent), death, activePlayerIds);
                    continue;
                }

                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null) {
                    continue;
                }

                UUID entityId = getEntityId(chunk.getComponent(entityIndex, UUIDComponent.getComponentType()));
                if (entityId == null) {
                    continue;
                }

                activeMobIds.add(entityId);

                TrackedMob trackedMob = trackedMobs.get(entityId);
                if (trackedMob == null) {
                    ModelComponent modelComponent = chunk.getComponent(entityIndex, ModelComponent.getComponentType());
                    trackedMob = maybeTrackNewMob(entityId, ref, npc, modelComponent, store, world);
                }

                DeathComponent death = chunk.getComponent(entityIndex, DeathComponent.getComponentType());
                if (death == null) {
                    processedMobDeaths.remove(entityId);
                    continue;
                }

                if (trackedMob != null && processedMobDeaths.add(entityId)) {
                    handleMobDeath(store, death, trackedMob);
                }
            }
        });

        trackedMobs.keySet().retainAll(activeMobIds);
        processedMobDeaths.retainAll(activeMobIds);
        processedPlayerDeaths.retainAll(activePlayerIds);
    }

    private void handlePlayerLifecycle(UUID playerUuid, DeathComponent death, Set<UUID> activePlayerIds) {
        if (playerUuid == null) {
            return;
        }

        activePlayerIds.add(playerUuid);

        if (death == null) {
            processedPlayerDeaths.remove(playerUuid);
            return;
        }

        if (processedPlayerDeaths.add(playerUuid)) {
            mod.onPlayerDeath(playerUuid.toString());
        }
    }

    private TrackedMob maybeTrackNewMob(UUID entityId, Ref<EntityStore> ref, NPCEntity npc,
                                        ModelComponent modelComponent, Store<EntityStore> store,
                                        World world) {
        String mobType = resolveMobType(npc, modelComponent);
        if (mobType == null) {
            logUnknownMobType(npc, modelComponent);
            return null;
        }

        String scalingPlayerId = selectScalingPlayerId(world);
        if (scalingPlayerId == null) {
            return null;
        }

        var result = mod.onMobSpawn(mobType, scalingPlayerId, null, false, false, false);
        if (result == null || result.stats() == null) {
            return null;
        }

        applyScaledResult(ref, store, result);

        String category = mod.getDataLoader().getMobCategory(mobType);
        boolean isRare = result.stats().isElite()
                || "elite".equals(category)
                || "mini_boss".equals(category);

        TrackedMob trackedMob = new TrackedMob(mobType, result.level(), isRare);
        trackedMobs.put(entityId, trackedMob);

        LOG.fine("[MOTM] Tracked live mob " + entityId + " as " + mobType
                + " [Lv. " + result.level() + "]");
        return trackedMob;
    }

    private void applyScaledResult(Ref<EntityStore> ref, Store<EntityStore> store,
                                   MenteesMod.ScaledMobResult result) {
        EntityStatMap entityStatMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (entityStatMap != null) {
            applyHealthScaling(entityStatMap, result.stats().getHealth());
        }

        Message displayName = Message.raw(result.displayName());
        if (result.levelColor() != null && !result.levelColor().isBlank()) {
            displayName = displayName.color(result.levelColor());
        }

        store.putComponent(ref, DisplayNameComponent.getComponentType(),
                new DisplayNameComponent(displayName));
    }

    private void applyHealthScaling(EntityStatMap entityStatMap, double targetHealth) {
        if (targetHealth <= 0) {
            return;
        }

        EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null || health.getMax() <= 0) {
            return;
        }

        float multiplier = (float) (targetHealth / health.getMax());
        if (!Float.isFinite(multiplier) || multiplier <= 0) {
            return;
        }

        entityStatMap.putModifier(
                DefaultEntityStatTypes.getHealth(),
                HEALTH_MODIFIER_ID,
                new StaticModifier(
                        Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.MULTIPLICATIVE,
                        multiplier
                )
        );
        entityStatMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
    }

    private void handleMobDeath(Store<EntityStore> store, DeathComponent death, TrackedMob trackedMob) {
        String killerPlayerId = resolvePlayerId(store, death.getDeathInfo());
        if (killerPlayerId == null) {
            return;
        }

        mod.onMobKilled(killerPlayerId, trackedMob.mobType(), trackedMob.level(), trackedMob.rare());
    }

    private String resolvePlayerId(Store<EntityStore> store, Damage damage) {
        if (damage == null) {
            return null;
        }

        if (!(damage.getSource() instanceof Damage.EntitySource entitySource)) {
            return null;
        }

        Ref<EntityStore> sourceRef = entitySource.getRef();
        if (sourceRef == null || !sourceRef.isValid()) {
            return null;
        }

        Player player = store.getComponent(sourceRef, Player.getComponentType());
        if (player == null) {
            return null;
        }

        UUIDComponent uuidComponent = store.getComponent(sourceRef, UUIDComponent.getComponentType());
        UUID playerId = getEntityId(uuidComponent);
        return playerId != null ? playerId.toString() : null;
    }

    private String selectScalingPlayerId(World world) {
        String bestPlayerId = null;
        int bestLevel = Integer.MIN_VALUE;

        for (var playerRef : world.getPlayerRefs()) {
            if (playerRef == null || playerRef.getUuid() == null) {
                continue;
            }

            String playerId = playerRef.getUuid().toString();
            PlayerData playerData = mod.getPlayerDataManager().getOnlinePlayer(playerId);
            if (playerData == null || playerData.getLevel() < bestLevel) {
                continue;
            }

            bestPlayerId = playerId;
            bestLevel = playerData.getLevel();
        }

        return bestPlayerId;
    }

    private String resolveMobType(NPCEntity npc, ModelComponent modelComponent) {
        Set<String> candidates = new LinkedHashSet<>();
        addCandidateVariants(candidates, npc.getNPCTypeId());

        if (modelComponent != null && modelComponent.getModel() != null) {
            addCandidateVariants(candidates, modelComponent.getModel().getModelAssetId());
            addCandidateVariants(candidates, modelComponent.getModel().getModel());
        }

        for (String candidate : candidates) {
            if (mod.getDataLoader().getMobStats(candidate) != null) {
                return candidate;
            }
        }

        return null;
    }

    private void addCandidateVariants(Set<String> candidates, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return;
        }

        String normalized = rawValue.trim().replace('\\', '/');

        addCandidate(candidates, normalized);
        addCandidate(candidates, lastSegment(normalized, '/'));
        addCandidate(candidates, lastSegment(normalized, ':'));
        addCandidate(candidates, lastSegment(normalized, '.'));

        String slashSegment = lastSegment(normalized, '/');
        if (slashSegment != null) {
            addCandidate(candidates, lastSegment(slashSegment, ':'));
            addCandidate(candidates, lastSegment(slashSegment, '.'));
        }
    }

    private void addCandidate(Set<String> candidates, String rawValue) {
        String sanitized = sanitizeMobKey(rawValue);
        if (sanitized != null && !sanitized.isBlank()) {
            candidates.add(sanitized);
        }
    }

    private String sanitizeMobKey(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String sanitized = rawValue.trim().toLowerCase(Locale.ROOT)
                .replace(".json", "")
                .replace('-', '_')
                .replace(' ', '_');

        String[] prefixes = {"npc_", "entity_", "mob_", "creature_"};
        for (String prefix : prefixes) {
            if (sanitized.startsWith(prefix) && sanitized.length() > prefix.length()) {
                sanitized = sanitized.substring(prefix.length());
                break;
            }
        }

        String[] suffixes = {"_npc", "_entity", "_mob"};
        for (String suffix : suffixes) {
            if (sanitized.endsWith(suffix) && sanitized.length() > suffix.length()) {
                sanitized = sanitized.substring(0, sanitized.length() - suffix.length());
                break;
            }
        }

        return sanitized;
    }

    private String lastSegment(String value, char delimiter) {
        if (value == null) {
            return null;
        }

        int index = value.lastIndexOf(delimiter);
        if (index < 0 || index >= value.length() - 1) {
            return value;
        }
        return value.substring(index + 1);
    }

    private void logUnknownMobType(NPCEntity npc, ModelComponent modelComponent) {
        String modelAssetId = modelComponent != null && modelComponent.getModel() != null
                ? modelComponent.getModel().getModelAssetId()
                : "unknown";

        String logKey = npc.getNPCTypeId() + "|" + modelAssetId;
        if (loggedUnknownMobTypes.add(logKey)) {
            LOG.info("[MOTM] Unmapped NPC type encountered. npcTypeId="
                    + npc.getNPCTypeId() + ", modelAssetId=" + modelAssetId);
        }
    }

    private UUID getEntityId(UUIDComponent uuidComponent) {
        return uuidComponent != null ? uuidComponent.getUuid() : null;
    }

    private record TrackedMob(String mobType, int level, boolean rare) {}
}
