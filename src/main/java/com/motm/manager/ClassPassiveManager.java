package com.motm.manager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.ColorLight;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.PersistentDynamicLight;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.ClassData;
import com.motm.model.PlayerData;
import com.motm.model.StatusEffect;
import com.motm.model.StyleData;
import com.motm.util.DataLoader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runs the always-on class passive layer for Terra, Hydro, Aero, and Corruptus.
 *
 * The abilities/styles own active combat, while this manager owns the baseline
 * class identity that should always be working in the background.
 */
public class ClassPassiveManager {

    private static final int TICKS_PER_SECOND = 20;
    private static final int TERRA_REGEN_INTERVAL_TICKS = TICKS_PER_SECOND;
    private static final double MOVEMENT_EPSILON = 0.08;
    private static final String TERRA_CAVE_VISION_LIGHT_ID = "motm_terra_cave_vision";
    private static final String HYDRO_OXYGEN_MODIFIER_ID = "motm_hydro_passive_oxygen";
    private static final String AERO_SIGNATURE_ENERGY_MODIFIER_ID = "motm_aero_passive_signature_energy";
    private static final ColorLight TERRA_CAVE_LIGHT = new ColorLight((byte) 9, (byte) 120, (byte) 110, (byte) 90);
    private static final int TERRA_CAVE_MAX_Y = 80;
    private static final int TERRA_CAVE_SCAN_DISTANCE = 10;
    private static final int TERRA_CAVE_REQUIRED_SOLID_BLOCKS = 3;
    private static final Pattern DECIMAL_PATTERN = Pattern.compile("([0-9]+(?:\\.[0-9]+)?)");

    private final DataLoader dataLoader;
    private final PlayerDataManager playerDataManager;
    private final StatusEffectManager statusEffectManager;
    private final ResourceManager resourceManager;

    private final TerraPassive terraPassive;
    private final HydroPassive hydroPassive;
    private final AeroPassive aeroPassive;

    private final Map<String, Vector3d> lastPositionsByPlayer = new HashMap<>();
    private final Map<String, Integer> stationaryTicksByPlayer = new HashMap<>();
    private final Set<String> terraShieldPrimedPlayers = new HashSet<>();
    private final Set<String> terraCaveVisionPlayers = new HashSet<>();
    private final Map<String, Vector3d> hydroSwimBoostByPlayer = new HashMap<>();
    private final Set<String> hydroSwimmingPlayers = new HashSet<>();
    private final Set<String> hydroUnderwaterPlayers = new HashSet<>();
    private final Map<String, Integer> stormChargeByPlayer = new HashMap<>();
    private final Map<String, Vector3d> aeroMoveBoostByPlayer = new HashMap<>();

    private long tickCounter = 0L;

    public ClassPassiveManager(DataLoader dataLoader,
                               PlayerDataManager playerDataManager,
                               StatusEffectManager statusEffectManager,
                               ResourceManager resourceManager) {
        this.dataLoader = dataLoader;
        this.playerDataManager = playerDataManager;
        this.statusEffectManager = statusEffectManager;
        this.resourceManager = resourceManager;
        this.terraPassive = loadTerraPassive();
        this.hydroPassive = loadHydroPassive();
        this.aeroPassive = loadAeroPassive();
    }

    public synchronized void onPlayerJoin(PlayerData player) {
        if (player == null || player.getPlayerId() == null) {
            return;
        }

        clearPlayerState(player.getPlayerId());
    }

    public synchronized void onPlayerDeath(String playerId) {
        clearPlayerState(playerId);
    }

    public synchronized void clearPlayerState(String playerId) {
        if (playerId == null) {
            return;
        }

        lastPositionsByPlayer.remove(playerId);
        stationaryTicksByPlayer.remove(playerId);
        terraShieldPrimedPlayers.remove(playerId);
        terraCaveVisionPlayers.remove(playerId);
        hydroSwimBoostByPlayer.remove(playerId);
        hydroSwimmingPlayers.remove(playerId);
        hydroUnderwaterPlayers.remove(playerId);
        stormChargeByPlayer.remove(playerId);
        aeroMoveBoostByPlayer.remove(playerId);
    }

    public synchronized void tick(Map<String, Player> runtimePlayers, Store<EntityStore> currentStore) {
        tickCounter++;

        if (runtimePlayers == null || runtimePlayers.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Player> entry : runtimePlayers.entrySet()) {
            String playerId = entry.getKey();
            Player runtimePlayer = entry.getValue();
            if (playerId == null || runtimePlayer == null) {
                continue;
            }
            Ref<EntityStore> playerRef = runtimePlayer.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() != currentStore) {
                continue;
            }

            PlayerData player = playerDataManager.getOnlinePlayer(playerId);
            if (player == null || player.getPlayerClass() == null) {
                clearTerraPassiveRuntime(playerId, runtimePlayer);
                clearHydroPassiveRuntime(playerId, runtimePlayer);
                clearAeroPassiveRuntime(playerId, runtimePlayer);
                clearPlayerState(playerId);
                continue;
            }

            switch (player.getPlayerClass().toLowerCase(Locale.ROOT)) {
                case "terra" -> {
                    clearHydroPassiveRuntime(playerId, runtimePlayer);
                    clearAeroPassiveRuntime(playerId, runtimePlayer);
                    tickTerraPassive(runtimePlayer, player);
                }
                case "hydro" -> {
                    clearTerraPassiveRuntime(playerId, runtimePlayer);
                    clearAeroPassiveRuntime(playerId, runtimePlayer);
                    tickHydroPassive(runtimePlayer, player);
                }
                case "aero" -> {
                    clearTerraPassiveRuntime(playerId, runtimePlayer);
                    clearHydroPassiveRuntime(playerId, runtimePlayer);
                    tickAeroPassive(runtimePlayer, player);
                }
                default -> {
                    clearTerraPassiveRuntime(playerId, runtimePlayer);
                    clearHydroPassiveRuntime(playerId, runtimePlayer);
                    clearAeroPassiveRuntime(playerId, runtimePlayer);
                    updateTrackedPosition(playerId, runtimePlayer);
                }
            }
        }
    }

    public synchronized int resolveAbilityResourceCost(PlayerData player, StyleData style, AbilityData ability) {
        if (player == null || style == null || ability == null) {
            return 0;
        }

        int baseCost = Math.max(0, ability.getResourceCost());
        if (baseCost == 0) {
            return 0;
        }

        if (!"hydro".equalsIgnoreCase(player.getPlayerClass())
                || !"water".equalsIgnoreCase(style.getResourceType())
                || !isHydroLowResource(player.getPlayerId())) {
            return baseCost;
        }

        int reducedCost = (int) Math.floor(baseCost * (1.0 - hydroPassive.costReduction()));
        return Math.max(1, reducedCost);
    }

    public synchronized double getAbilityDamageModifier(PlayerData player) {
        if (player == null || player.getPlayerClass() == null) {
            return 0.0;
        }

        if ("hydro".equalsIgnoreCase(player.getPlayerClass()) && isHydroLowResource(player.getPlayerId())) {
            return hydroPassive.lowResourceDamageModifier();
        }

        return 0.0;
    }

    public synchronized boolean isTerraShieldPrimed(String playerId) {
        return playerId != null && terraShieldPrimedPlayers.contains(playerId);
    }

    public synchronized int getTerraStationaryTicks(String playerId) {
        return playerId == null ? 0 : stationaryTicksByPlayer.getOrDefault(playerId, 0);
    }

    public int getTerraStationaryTicksRequired() {
        return terraPassive.stationaryTicksRequired();
    }

    public synchronized boolean isTerraCaveVisionActive(String playerId) {
        return playerId != null && terraCaveVisionPlayers.contains(playerId);
    }

    public synchronized boolean isHydroLowResourceMode(String playerId) {
        return isHydroLowResource(playerId);
    }

    public synchronized boolean isHydroSwimming(String playerId) {
        return playerId != null && hydroSwimmingPlayers.contains(playerId);
    }

    public synchronized boolean isHydroUnderwater(String playerId) {
        return playerId != null && hydroUnderwaterPlayers.contains(playerId);
    }

    public synchronized void onDamageDealt(PlayerData player,
                                           Ref<EntityStore> playerRef,
                                           String targetEntityId,
                                           double damageAmount,
                                           boolean abilityBased) {
        if (player == null || player.getPlayerClass() == null || damageAmount <= 0.0) {
            return;
        }

        String classId = player.getPlayerClass().toLowerCase(Locale.ROOT);
        if ("hydro".equals(classId) && abilityBased) {
            double healed = healFlat(playerRef, damageAmount * hydroPassive.spellVampRatio());
            if (healed > 0.0) {
                player.getStatistics().setTotalHealingDone(player.getStatistics().getTotalHealingDone() + healed);
            }
        }
    }

    public synchronized boolean hasWeaponAttackPassive(PlayerData player) {
        return false;
    }

    public synchronized WeaponAttackPassiveBonus consumeWeaponAttackBonus(PlayerData player,
                                                                          Ref<EntityStore> playerRef,
                                                                          Store<EntityStore> store,
                                                                          double currentAttackDamage) {
        return WeaponAttackPassiveBonus.none();
    }

    public synchronized void onMobKilled(PlayerData player, Player runtimePlayer, String mobEntityId) {
        // Corruptus soul income is handled by ResourceManager.onMobKilled(...).
        // Class passive runtime no longer adds extra kill-restores or corruption-mark bonuses.
    }

    public synchronized String buildPassiveStateSummary(PlayerData player) {
        if (player == null || player.getPlayerClass() == null) {
            return "";
        }

        if ("aero".equalsIgnoreCase(player.getPlayerClass())) {
            return "Tempo Surge: +25% movement speed | +80% native Hytale energy";
        }

        return switch (player.getPlayerClass().toLowerCase(Locale.ROOT)) {
            case "terra" -> {
                int stationaryTicks = stationaryTicksByPlayer.getOrDefault(player.getPlayerId(), 0);
                double stationarySeconds = stationaryTicks / (double) TICKS_PER_SECOND;
                String shieldState = terraShieldPrimedPlayers.contains(player.getPlayerId())
                        ? "shield primed"
                        : "shield in " + Math.max(0.0, (terraPassive.stationaryTicksRequired() - stationaryTicks) / (double) TICKS_PER_SECOND) + "s";
                yield "Earthen Resilience: " + formatDecimal(stationarySeconds) + "s still · " + shieldState;
            }
            case "hydro" -> {
                int water = resourceManager.getAmount(player.getPlayerId(), "water");
                int maxWater = resourceManager.getMaxAmount(player.getPlayerId(), "water");
                String mode = isHydroLowResource(player.getPlayerId()) ? "low-water flow active" : "stable flow";
                yield "Tidal Flow: " + water + "/" + maxWater + " water · " + mode;
            }
            case "aero" -> {
                int charge = stormChargeByPlayer.getOrDefault(player.getPlayerId(), 0);
                String mode = charge >= aeroPassive.maxCharge() ? "charged" : "building";
                yield "Storm Surge: " + charge + "/" + aeroPassive.maxCharge() + " storm charge · " + mode;
            }
            case "corruptus" -> {
                int souls = resourceManager.getAmount(player.getPlayerId(), "souls");
                int maxSouls = Math.max(1, resourceManager.getMaxAmount(player.getPlayerId(), "souls"));
                String mode = souls > 0 ? "abilities fueled" : "harvest souls from kills";
                yield "Soul Harvest: " + souls + "/" + maxSouls + " souls · " + mode;
            }
            default -> "";
        };
    }

    private void tickHydroPassive(Player runtimePlayer, PlayerData player) {
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return;
        }

        Store<EntityStore> store = playerRef.getStore();
        String playerId = player.getPlayerId();

        applyHydroOxygenModifier(playerRef, store);

        MovementStatesComponent movementStatesComponent = store.getComponent(
                playerRef,
                MovementStatesComponent.getComponentType()
        );
        boolean swimming = movementStatesComponent != null
                && movementStatesComponent.getMovementStates() != null
                && movementStatesComponent.getMovementStates().swimming;

        boolean canBreathe = canBreatheAtCurrentHeight(runtimePlayer, playerRef, store);
        if (canBreathe) {
            maximizeOxygen(playerRef, store);
        }

        if (swimming) {
            hydroSwimmingPlayers.add(playerId);
        } else {
            hydroSwimmingPlayers.remove(playerId);
        }

        boolean underwater = swimming && !canBreathe;
        if (underwater) {
            hydroUnderwaterPlayers.add(playerId);
        } else {
            hydroUnderwaterPlayers.remove(playerId);
        }

        applyHydroSwimSpeedBonus(playerId, playerRef, store, swimming);
        updateTrackedPosition(playerId, runtimePlayer);
    }

    private void tickTerraPassive(Player runtimePlayer, PlayerData player) {
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return;
        }

        Store<EntityStore> store = playerRef.getStore();
        Vector3d position = getPosition(playerRef, store);
        if (position == null) {
            return;
        }

        String playerId = player.getPlayerId();
        Vector3d previousPosition = lastPositionsByPlayer.get(playerId);
        boolean moved = hasMoved(previousPosition, position);

        lastPositionsByPlayer.put(playerId, position.clone());
        if (moved) {
            stationaryTicksByPlayer.put(playerId, 0);
            terraShieldPrimedPlayers.remove(playerId);
        } else {
            stationaryTicksByPlayer.merge(playerId, 1, Integer::sum);
        }

        if (stationaryTicksByPlayer.getOrDefault(playerId, 0) >= terraPassive.stationaryTicksRequired()
                && terraShieldPrimedPlayers.add(playerId)) {
            applyShieldFraction(playerId, playerRef, store, terraPassive.shieldFraction(), "terra_passive_earthen_resilience");
        }

        if (tickCounter % TERRA_REGEN_INTERVAL_TICKS == 0
                && getHealthRatio(playerRef, store) < terraPassive.lowHealthThreshold()) {
            double healed = healPercentOfMax(playerRef, store, terraPassive.regenPercentPerSecond());
            if (healed > 0.0) {
                player.getStatistics().setTotalHealingDone(player.getStatistics().getTotalHealingDone() + healed);
            }
        }

        updateTerraCaveVision(runtimePlayer, playerRef, store, playerId, position);
    }

    private void tickAeroPassive(Player runtimePlayer, PlayerData player) {
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return;
        }

        Store<EntityStore> store = playerRef.getStore();
        String playerId = player.getPlayerId();
        applyAeroSignatureEnergyModifier(playerRef, store);
        applyAeroMovementSpeedBonus(playerId, playerRef, store);

        Vector3d position = getPosition(playerRef, store);
        if (position != null) {
            lastPositionsByPlayer.put(playerId, position.clone());
        }
    }

    private void updateTrackedPosition(String playerId, Player runtimePlayer) {
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return;
        }

        Vector3d position = getPosition(playerRef, playerRef.getStore());
        if (position != null) {
            lastPositionsByPlayer.put(playerId, position.clone());
        }
    }

    private boolean isHydroLowResource(String playerId) {
        if (playerId == null) {
            return false;
        }

        int current = resourceManager.getAmount(playerId, "water");
        int max = resourceManager.getMaxAmount(playerId, "water");
        return max > 0 && (current / (double) max) < hydroPassive.lowResourceThreshold();
    }

    private void clearHydroPassiveRuntime(String playerId, Player runtimePlayer) {
        hydroSwimmingPlayers.remove(playerId);
        hydroUnderwaterPlayers.remove(playerId);

        if (playerId == null || runtimePlayer == null) {
            hydroSwimBoostByPlayer.remove(playerId);
            return;
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            hydroSwimBoostByPlayer.remove(playerId);
            return;
        }

        Store<EntityStore> store = playerRef.getStore();
        EntityStatMap entityStatMap = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (entityStatMap != null) {
            entityStatMap.removeModifier(DefaultEntityStatTypes.getOxygen(), HYDRO_OXYGEN_MODIFIER_ID);
        }

        clearHydroSwimBoost(playerId, playerRef, store);
    }

    private void clearAeroPassiveRuntime(String playerId, Player runtimePlayer) {
        if (runtimePlayer == null) {
            aeroMoveBoostByPlayer.remove(playerId);
            return;
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            aeroMoveBoostByPlayer.remove(playerId);
            return;
        }

        Store<EntityStore> store = playerRef.getStore();
        EntityStatMap entityStatMap = store.getComponent(playerRef, EntityStatMap.getComponentType());
        if (entityStatMap != null) {
            entityStatMap.removeModifier(DefaultEntityStatTypes.getSignatureEnergy(), AERO_SIGNATURE_ENERGY_MODIFIER_ID);
        }

        clearAeroMovementBoost(playerId, playerRef, store);
    }

    private void clearTerraPassiveRuntime(String playerId, Player runtimePlayer) {
        terraCaveVisionPlayers.remove(playerId);

        if (runtimePlayer == null) {
            return;
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return;
        }

        playerRef.getStore().removeComponentIfExists(playerRef, PersistentDynamicLight.getComponentType());
    }

    private void updateTerraCaveVision(Player runtimePlayer,
                                       Ref<EntityStore> playerRef,
                                       Store<EntityStore> store,
                                       String playerId,
                                       Vector3d position) {
        boolean active = isUndergroundCave(runtimePlayer, position);
        if (active) {
            if (terraCaveVisionPlayers.add(playerId)) {
                store.putComponent(
                        playerRef,
                        PersistentDynamicLight.getComponentType(),
                        new PersistentDynamicLight(new ColorLight(TERRA_CAVE_LIGHT))
                );
            }
        } else if (terraCaveVisionPlayers.remove(playerId)) {
            store.removeComponentIfExists(playerRef, PersistentDynamicLight.getComponentType());
        }
    }

    private boolean isUndergroundCave(Player runtimePlayer, Vector3d position) {
        if (runtimePlayer == null || position == null || position.y > TERRA_CAVE_MAX_Y) {
            return false;
        }

        World world = runtimePlayer.getWorld();
        if (world == null) {
            return false;
        }

        int blockX = (int) Math.floor(position.x);
        int blockY = (int) Math.floor(position.y);
        int blockZ = (int) Math.floor(position.z);

        int solidAbove = 0;
        for (int offset = 2; offset <= TERRA_CAVE_SCAN_DISTANCE; offset++) {
            BlockMaterial material = getBlockMaterial(world, new Vector3i(blockX, blockY + offset, blockZ));
            if (material == BlockMaterial.Solid) {
                solidAbove++;
            }
        }

        return solidAbove >= TERRA_CAVE_REQUIRED_SOLID_BLOCKS;
    }

    private BlockMaterial getBlockMaterial(World world, Vector3i targetBlock) {
        if (world == null || targetBlock == null) {
            return BlockMaterial.Empty;
        }

        long chunkIndex = ChunkUtil.indexChunkFromBlock(targetBlock.getX(), targetBlock.getZ());
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) {
            chunk = world.getChunkIfInMemory(chunkIndex);
        }
        if (chunk == null) {
            return BlockMaterial.Empty;
        }

        int localX = ChunkUtil.localCoordinate(targetBlock.getX());
        int localZ = ChunkUtil.localCoordinate(targetBlock.getZ());
        int y = targetBlock.getY();
        var blockType = chunk.getBlockType(localX, y, localZ);
        if (blockType == null) {
            return BlockMaterial.Empty;
        }

        return blockType.getMaterial();
    }

    private String resolvePrimaryResourceType(String classId) {
        if (classId == null) {
            return null;
        }

        return switch (classId.toLowerCase(Locale.ROOT)) {
            case "hydro" -> "water";
            case "corruptus" -> "souls";
            default -> null;
        };
    }

    private Vector3d getPosition(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        TransformComponent transform = store.getComponent(entityRef, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }
        return transform.getTransform().getPosition();
    }

    private boolean hasMoved(Vector3d previous, Vector3d current) {
        if (previous == null || current == null) {
            return false;
        }

        return distance(previous, current) > MOVEMENT_EPSILON;
    }

    private double distance(Vector3d a, Vector3d b) {
        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    }

    private double getHealthRatio(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        EntityStatMap entityStatMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return 1.0;
        }

        EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null || health.getMax() <= 0.0f) {
            return 1.0;
        }

        return health.get() / health.getMax();
    }

    private void applyHydroOxygenModifier(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        EntityStatMap entityStatMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return;
        }

        entityStatMap.putModifier(
                DefaultEntityStatTypes.getOxygen(),
                HYDRO_OXYGEN_MODIFIER_ID,
                new StaticModifier(
                        Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.MULTIPLICATIVE,
                        (float) (1.0 + hydroPassive.oxygenCapacityBonus())
                )
        );
    }

    private boolean canBreatheAtCurrentHeight(Player runtimePlayer,
                                              Ref<EntityStore> entityRef,
                                              Store<EntityStore> store) {
        long packed = Player.getPackedMaterialAndFluidAtBreathingHeight(entityRef, store);
        BlockMaterial material = BlockMaterial.fromValue((int) (packed >>> 32));
        int fluidId = (int) packed;
        return runtimePlayer.canBreathe(entityRef, material, fluidId, store);
    }

    private void maximizeOxygen(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        EntityStatMap entityStatMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return;
        }

        EntityStatValue oxygen = entityStatMap.get(DefaultEntityStatTypes.getOxygen());
        if (oxygen == null || oxygen.getMax() <= 0.0f) {
            return;
        }

        entityStatMap.maximizeStatValue(DefaultEntityStatTypes.getOxygen());
    }

    private void applyAeroSignatureEnergyModifier(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        EntityStatMap entityStatMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return;
        }

        entityStatMap.putModifier(
                DefaultEntityStatTypes.getSignatureEnergy(),
                AERO_SIGNATURE_ENERGY_MODIFIER_ID,
                new StaticModifier(
                        Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.MULTIPLICATIVE,
                        (float) (1.0 + aeroPassive.signatureEnergyBonus())
                )
        );
    }

    private void applyAeroMovementSpeedBonus(String playerId,
                                             Ref<EntityStore> entityRef,
                                             Store<EntityStore> store) {
        if (playerId == null) {
            return;
        }

        Velocity velocity = store.getComponent(entityRef, Velocity.getComponentType());
        if (velocity == null) {
            aeroMoveBoostByPlayer.remove(playerId);
            return;
        }

        Vector3d lastBoost = aeroMoveBoostByPlayer.getOrDefault(playerId, new Vector3d(0.0, 0.0, 0.0));
        Vector3d currentVelocity = velocity.getVelocity();
        if (currentVelocity == null || !currentVelocity.isFinite()) {
            aeroMoveBoostByPlayer.remove(playerId);
            return;
        }

        Vector3d baseVelocity = new Vector3d(
                currentVelocity.x - lastBoost.x,
                currentVelocity.y,
                currentVelocity.z - lastBoost.z
        );
        Vector3d horizontalBase = new Vector3d(baseVelocity.x, 0.0, baseVelocity.z);
        if (!horizontalBase.isFinite() || horizontalBase.length() < 0.01) {
            clearAeroMovementBoost(playerId, entityRef, store);
            return;
        }

        Vector3d newBoost = new Vector3d(
                horizontalBase.x * aeroPassive.movementSpeedBonus(),
                0.0,
                horizontalBase.z * aeroPassive.movementSpeedBonus()
        );
        aeroMoveBoostByPlayer.put(playerId, newBoost);
        velocity.set(
                baseVelocity.x + newBoost.x,
                baseVelocity.y,
                baseVelocity.z + newBoost.z
        );
    }

    private void applyHydroSwimSpeedBonus(String playerId,
                                          Ref<EntityStore> entityRef,
                                          Store<EntityStore> store,
                                          boolean swimming) {
        if (playerId == null) {
            return;
        }

        Velocity velocity = store.getComponent(entityRef, Velocity.getComponentType());
        if (velocity == null) {
            hydroSwimBoostByPlayer.remove(playerId);
            return;
        }

        if (!swimming) {
            clearHydroSwimBoost(playerId, entityRef, store);
            return;
        }

        Vector3d lastBoost = hydroSwimBoostByPlayer.getOrDefault(playerId, new Vector3d(0.0, 0.0, 0.0));
        Vector3d currentVelocity = velocity.getVelocity();
        if (currentVelocity == null || !currentVelocity.isFinite()) {
            hydroSwimBoostByPlayer.remove(playerId);
            return;
        }

        Vector3d baseVelocity = new Vector3d(
                currentVelocity.x - lastBoost.x,
                currentVelocity.y,
                currentVelocity.z - lastBoost.z
        );
        Vector3d horizontalBase = new Vector3d(baseVelocity.x, 0.0, baseVelocity.z);
        if (!horizontalBase.isFinite() || horizontalBase.length() < 0.01) {
            hydroSwimBoostByPlayer.put(playerId, new Vector3d(0.0, 0.0, 0.0));
            return;
        }

        Vector3d newBoost = new Vector3d(
                horizontalBase.x * hydroPassive.swimSpeedBonus(),
                0.0,
                horizontalBase.z * hydroPassive.swimSpeedBonus()
        );
        hydroSwimBoostByPlayer.put(playerId, newBoost);
        velocity.set(
                baseVelocity.x + newBoost.x,
                baseVelocity.y,
                baseVelocity.z + newBoost.z
        );
    }

    private void clearHydroSwimBoost(String playerId,
                                     Ref<EntityStore> entityRef,
                                     Store<EntityStore> store) {
        Vector3d lastBoost = hydroSwimBoostByPlayer.remove(playerId);
        if (lastBoost == null || entityRef == null || store == null) {
            return;
        }

        Velocity velocity = store.getComponent(entityRef, Velocity.getComponentType());
        if (velocity == null) {
            return;
        }

        Vector3d currentVelocity = velocity.getVelocity();
        if (currentVelocity == null || !currentVelocity.isFinite()) {
            return;
        }

        velocity.set(
                currentVelocity.x - lastBoost.x,
                currentVelocity.y,
                currentVelocity.z - lastBoost.z
        );
    }

    private void clearAeroMovementBoost(String playerId,
                                        Ref<EntityStore> entityRef,
                                        Store<EntityStore> store) {
        Vector3d lastBoost = aeroMoveBoostByPlayer.remove(playerId);
        if (lastBoost == null || entityRef == null || store == null) {
            return;
        }

        Velocity velocity = store.getComponent(entityRef, Velocity.getComponentType());
        if (velocity == null) {
            return;
        }

        Vector3d currentVelocity = velocity.getVelocity();
        if (currentVelocity == null || !currentVelocity.isFinite()) {
            return;
        }

        velocity.set(
                currentVelocity.x - lastBoost.x,
                currentVelocity.y,
                currentVelocity.z - lastBoost.z
        );
    }

    private double healPercentOfMax(Ref<EntityStore> entityRef,
                                    Store<EntityStore> store,
                                    double fractionOfMaxHealth) {
        if (fractionOfMaxHealth <= 0.0) {
            return 0.0;
        }

        EntityStatMap entityStatMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return 0.0;
        }

        EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null || health.getMax() <= 0.0f) {
            return 0.0;
        }

        float healAmount = (float) (health.getMax() * fractionOfMaxHealth);
        float applied = Math.max(0f, Math.min(healAmount, health.getMax() - health.get()));
        if (applied <= 0.0f) {
            return 0.0;
        }

        entityStatMap.addStatValue(DefaultEntityStatTypes.getHealth(), applied);
        return applied;
    }

    private double healFlat(Ref<EntityStore> entityRef, double healAmount) {
        if (healAmount <= 0.0 || entityRef == null || !entityRef.isValid() || entityRef.getStore() == null) {
            return 0.0;
        }

        Store<EntityStore> store = entityRef.getStore();
        EntityStatMap entityStatMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return 0.0;
        }

        EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null || health.getMax() <= 0.0f) {
            return 0.0;
        }

        float applied = Math.max(0f, Math.min((float) healAmount, health.getMax() - health.get()));
        if (applied <= 0.0f) {
            return 0.0;
        }

        entityStatMap.addStatValue(DefaultEntityStatTypes.getHealth(), applied);
        return applied;
    }

    private void applyShieldFraction(String entityId,
                                     Ref<EntityStore> entityRef,
                                     Store<EntityStore> store,
                                     double fractionOfMaxHealth,
                                     String sourceId) {
        if (fractionOfMaxHealth <= 0.0) {
            return;
        }

        EntityStatMap entityStatMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return;
        }

        EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null || health.getMax() <= 0.0f) {
            return;
        }

        double shieldAmount = health.getMax() * fractionOfMaxHealth;
        if (shieldAmount <= 0.0) {
            return;
        }

        statusEffectManager.applyEffect(
                entityId,
                new StatusEffect(
                        StatusEffect.Type.SHIELD,
                        Math.max(TICKS_PER_SECOND * 3, terraPassive.stationaryTicksRequired()),
                        shieldAmount,
                        entityId,
                        sourceId
                )
        );
    }

    private TerraPassive loadTerraPassive() {
        ClassData.PassiveAbility passive = getPassiveAbility("terra");
        double shieldFraction = getPassiveValue(passive, "conditional_shield", 0.05);
        double regenFraction = getPassiveValue(passive, "conditional_regen", 0.01);
        double lowHealthThreshold = getConditionValue(passive, "conditional_regen", 0.30);
        int stationaryTicks = Math.max(TICKS_PER_SECOND, (int) Math.round(
                getConditionValue(passive, "conditional_shield", 2.0) * TICKS_PER_SECOND
        ));
        return new TerraPassive(shieldFraction, stationaryTicks, regenFraction, lowHealthThreshold);
    }

    private HydroPassive loadHydroPassive() {
        ClassData.PassiveAbility passive = getPassiveAbility("hydro");
        double spellVamp = getPassiveValue(passive, "spell_vamp", 0.03);
        double swimSpeedBonus = getPassiveValue(passive, "swim_speed_bonus", 0.40);
        double oxygenCapacityBonus = getPassiveValue(passive, "oxygen_capacity_bonus", 0.50);
        double threshold = getConditionValue(passive, "conditional_cost_reduction", 0.50);
        double costReduction = getPassiveValue(passive, "conditional_cost_reduction", 0.15);
        double damageModifier = getPassiveValue(passive, "conditional_damage_modifier", -0.10);
        return new HydroPassive(
                spellVamp,
                swimSpeedBonus,
                oxygenCapacityBonus,
                threshold,
                costReduction,
                damageModifier
        );
    }

    private AeroPassive loadAeroPassive() {
        ClassData.PassiveAbility passive = getPassiveAbility("aero");
        double movementSpeedBonus = getPassiveValue(passive, "movement_speed_bonus", 0.25);
        double signatureEnergyBonus = getPassiveValue(passive, "signature_energy_bonus", 0.80);
        return new AeroPassive(movementSpeedBonus, signatureEnergyBonus, 100);
    }

    private ClassData.PassiveAbility getPassiveAbility(String classId) {
        ClassData classData = dataLoader.getClassData(classId);
        return classData != null ? classData.getPassiveAbility() : null;
    }

    private ClassData.PassiveEffect findPassiveEffect(ClassData.PassiveAbility passiveAbility, String type) {
        if (passiveAbility == null || passiveAbility.getEffects() == null) {
            return null;
        }

        for (ClassData.PassiveEffect effect : passiveAbility.getEffects()) {
            if (effect != null && type.equalsIgnoreCase(effect.getType())) {
                return effect;
            }
        }

        return null;
    }

    private double getPassiveValue(ClassData.PassiveAbility passiveAbility, String type, double fallback) {
        ClassData.PassiveEffect effect = findPassiveEffect(passiveAbility, type);
        return effect != null ? effect.getValue() : fallback;
    }

    private double getConditionValue(ClassData.PassiveAbility passiveAbility, String type, double fallback) {
        ClassData.PassiveEffect effect = findPassiveEffect(passiveAbility, type);
        if (effect == null || effect.getCondition() == null || effect.getCondition().isBlank()) {
            return fallback;
        }

        Matcher matcher = DECIMAL_PATTERN.matcher(effect.getCondition());
        if (!matcher.find()) {
            return fallback;
        }

        try {
            return Double.parseDouble(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private String formatDecimal(double value) {
        return String.format(Locale.US, "%.1f", value);
    }

    private record TerraPassive(
            double shieldFraction,
            int stationaryTicksRequired,
            double regenPercentPerSecond,
            double lowHealthThreshold
    ) {}

    private record HydroPassive(
            double spellVampRatio,
            double swimSpeedBonus,
            double oxygenCapacityBonus,
            double lowResourceThreshold,
            double costReduction,
            double lowResourceDamageModifier
    ) {}

    private record AeroPassive(
            double movementSpeedBonus,
            double signatureEnergyBonus,
            int maxCharge
    ) {}

    public record WeaponAttackPassiveBonus(
            boolean applied,
            double bonusDamage,
            String summary
    ) {
        public static WeaponAttackPassiveBonus none() {
            return new WeaponAttackPassiveBonus(false, 0.0, "");
        }
    }
}
