package com.motm.manager;

import com.motm.model.PlayerData;

import java.util.*;
import java.util.logging.Logger;

/**
 * Manages class-specific resource systems.
 *
 * Resource types by class:
 *   - Terra: Physical materials (stone blocks, metal, seeds, dirt blocks, sand blocks, gems)
 *   - Hydro: Water (stored in waterskins kept in inventory like ammo with tiered capacity: 18/30/48/70/100)
 *   - Aero: No class resource requirement
 *   - Corruptus: Souls (earned from kills, max 50)
 */
public class ResourceManager {

    public interface TerraInventoryBridge {
        int countInventoryResource(String playerId, String resourceType);
        boolean spendInventoryResource(String playerId, String resourceType, int amount);
    }

    public interface HydroInventoryBridge {
        boolean hasHydroContainer(String playerId);
        int getHydroContainerTier(String playerId);
    }

    private static final Logger LOG = Logger.getLogger("MOTM");
    private static final List<String> TERRA_RESOURCE_TYPES = List.of(
            "stone_blocks",
            "metal",
            "seeds",
            "dirt_blocks",
            "sand_blocks",
            "gems"
    );
    private static final int DEV_TERRA_FILL_AMOUNT = 50;

    // Max resource values
    public static final int MAX_SOULS = 50;
    public static final int SOULS_PER_KILL = 3;

    // Hydro waterskin capacities by tier.
    // Tuned upward so Hydro can stay in the fight longer between refills.
    public static final int[] WATER_CONTAINER_CAPACITY = {18, 30, 48, 70, 100};
    public static final String[] WATER_CONTAINER_NAMES = {
            "Light Hide Waterskin",
            "Soft Hide Waterskin",
            "Medium Hide Waterskin",
            "Heavy Hide Waterskin",
            "Dark Hide Waterskin"
    };

    // playerId -> (resourceType -> currentAmount)
    private final Map<String, Map<String, Integer>> playerResources = new HashMap<>();
    // playerId -> water container tier (0-4)
    private final Map<String, Integer> waterContainerTier = new HashMap<>();
    private TerraInventoryBridge terraInventoryBridge;
    private HydroInventoryBridge hydroInventoryBridge;

    public void setTerraInventoryBridge(TerraInventoryBridge terraInventoryBridge) {
        this.terraInventoryBridge = terraInventoryBridge;
    }

    public void setHydroInventoryBridge(HydroInventoryBridge hydroInventoryBridge) {
        this.hydroInventoryBridge = hydroInventoryBridge;
    }

    /**
     * Initialize resources for a player based on their class.
     */
    public void initializeForPlayer(String playerId, String playerClass) {
        Map<String, Integer> resources = createDefaultResources(playerId, playerClass);
        playerResources.put(playerId, resources);
    }

    public void initializeForPlayer(PlayerData player) {
        if (player == null || player.getPlayerId() == null || player.getPlayerClass() == null) {
            return;
        }

        synchronizePersistentState(player);
        Map<String, Integer> resources = createDefaultResources(player.getPlayerId(), player.getPlayerClass());
        if (player.getClassResources() != null && !player.getClassResources().isEmpty()) {
            Map<String, Integer> persisted = player.getClassResources();
            for (Map.Entry<String, Integer> entry : persisted.entrySet()) {
                String resourceType = normalizePersistedResourceType(player.getPlayerClass(), entry.getKey());
                if (resourceType == null || !resources.containsKey(resourceType)) {
                    continue;
                }
                int max = getMaxForResource(player.getPlayerId(), resourceType);
                int amount = Math.max(0, Math.min(entry.getValue(), max));
                resources.put(resourceType, amount);
            }
        }

        playerResources.put(player.getPlayerId(), resources);
    }

    private Map<String, Integer> createDefaultResources(String playerId, String playerClass) {
        Map<String, Integer> resources = new HashMap<>();

        switch (playerClass.toLowerCase(Locale.ROOT)) {
            case "hydro" -> {
                resources.put("water", getHydroCapacity(playerId));
            }
            case "corruptus" -> resources.put("souls", 0);
            case "terra" -> {
                resources.put("stone_blocks", 0);
                resources.put("metal", 0);
                resources.put("seeds", 0);
                resources.put("dirt_blocks", 0);
                resources.put("sand_blocks", 0);
                resources.put("gems", 0);
            }
            default -> {
            }
        }
        return resources;
    }

    /**
     * Rehydrate persistent resource-related state onto the runtime caches.
     */
    public void synchronizePersistentState(PlayerData player) {
        if (player == null || player.getPlayerId() == null) {
            return;
        }

        int tier = Math.max(0, Math.min(player.getWaterContainerTier(), WATER_CONTAINER_CAPACITY.length - 1));
        waterContainerTier.put(player.getPlayerId(), tier);
    }

    /**
     * Clear runtime-only resource state for a player.
     */
    public void clearPlayerState(String playerId) {
        playerResources.remove(playerId);
        waterContainerTier.remove(playerId);
    }

    /**
     * Get current amount of a resource.
     */
    public int getAmount(String playerId, String resourceType) {
        Map<String, Integer> resources = playerResources.get(playerId);
        int runtimeAmount = resources == null ? 0 : resources.getOrDefault(resourceType, 0);
        if ("water".equals(resourceType)) {
            return Math.max(0, Math.min(runtimeAmount, getHydroCapacity(playerId)));
        }
        if (isInventoryBackedTerraResource(resourceType) && terraInventoryBridge != null) {
            return runtimeAmount + Math.max(0, terraInventoryBridge.countInventoryResource(playerId, resourceType));
        }
        return runtimeAmount;
    }

    public int getMaxAmount(String playerId, String resourceType) {
        return getMaxForResource(playerId, resourceType);
    }

    public int getHudDisplayMax(String playerId, String resourceType) {
        int actualMax = getMaxForResource(playerId, resourceType);
        if (actualMax >= 999) {
            return 50;
        }
        return actualMax;
    }

    public String getDisplayName(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            return "Resource";
        }

        return switch (resourceType.toLowerCase(Locale.ROOT)) {
            case "stone_blocks" -> "Stone Blocks";
            case "dirt_blocks" -> "Dirt Blocks";
            case "sand_blocks" -> "Sand Blocks";
            case "metal" -> "Metal";
            case "gems" -> "Gems";
            default -> {
                String[] parts = resourceType.split("_");
                StringBuilder label = new StringBuilder();
                for (String part : parts) {
                    if (part.isBlank()) {
                        continue;
                    }
                    if (!label.isEmpty()) {
                        label.append(' ');
                    }
                    label.append(Character.toUpperCase(part.charAt(0)));
                    if (part.length() > 1) {
                        label.append(part.substring(1));
                    }
                }
                yield label.isEmpty() ? "Resource" : label.toString();
            }
        };
    }

    /**
     * Get all resources for a player.
     */
    public Map<String, Integer> getAllResources(String playerId) {
        return playerResources.getOrDefault(playerId, Collections.emptyMap());
    }

    /**
     * Try to spend a resource. Returns true if successful.
     */
    public boolean spend(String playerId, String resourceType, int amount) {
        if (resourceType == null || resourceType.isBlank() || amount <= 0) {
            return true;
        }

        Map<String, Integer> resources = playerResources.get(playerId);
        int runtimeAmount = resources == null ? 0 : resources.getOrDefault(resourceType, 0);

        if ("water".equals(resourceType)) {
            int available = getAmount(playerId, resourceType);
            if (resources == null || available < amount) {
                return false;
            }
            resources.put(resourceType, available - amount);
            return true;
        }

        if (isInventoryBackedTerraResource(resourceType) && terraInventoryBridge != null) {
            int total = getAmount(playerId, resourceType);
            if (total < amount) {
                return false;
            }

            int remaining = amount;
            if (runtimeAmount > 0) {
                int fromRuntime = Math.min(runtimeAmount, remaining);
                if (resources == null) {
                    resources = playerResources.computeIfAbsent(playerId, k -> new HashMap<>());
                }
                resources.put(resourceType, runtimeAmount - fromRuntime);
                remaining -= fromRuntime;
            }

            return remaining <= 0 || terraInventoryBridge.spendInventoryResource(playerId, resourceType, remaining);
        }

        if (resources == null) return false;

        if (runtimeAmount < amount) return false;

        resources.put(resourceType, runtimeAmount - amount);
        return true;
    }

    /**
     * Add a resource (from gathering, kills, etc.).
     */
    public void add(String playerId, String resourceType, int amount) {
        if (resourceType == null || resourceType.isBlank() || amount <= 0) {
            return;
        }

        Map<String, Integer> resources = playerResources.computeIfAbsent(playerId, k -> new HashMap<>());
        int current = resources.getOrDefault(resourceType, 0);
        int max = getMaxForResource(playerId, resourceType);
        resources.put(resourceType, Math.min(current + amount, max));
    }

    public void set(String playerId, String resourceType, int amount) {
        if (resourceType == null || resourceType.isBlank()) {
            return;
        }

        Map<String, Integer> resources = playerResources.computeIfAbsent(playerId, k -> new HashMap<>());
        int max = getMaxForResource(playerId, resourceType);
        resources.put(resourceType, Math.max(0, Math.min(amount, max)));
    }

    public List<String> getResourceTypesForClass(String playerClass) {
        if (playerClass == null || playerClass.isBlank()) {
            return Collections.emptyList();
        }

        return switch (playerClass.toLowerCase(Locale.ROOT)) {
            case "terra" -> TERRA_RESOURCE_TYPES;
            case "hydro" -> List.of("water");
            case "corruptus" -> List.of("souls");
            default -> Collections.emptyList();
        };
    }

    public void fillClassResources(String playerId, String playerClass) {
        if (playerClass == null || playerClass.isBlank()) {
            return;
        }

        switch (playerClass.toLowerCase(Locale.ROOT)) {
            case "hydro" -> set(playerId, "water", getHydroCapacity(playerId));
            case "corruptus" -> set(playerId, "souls", MAX_SOULS);
            case "terra" -> {
                for (String resourceType : TERRA_RESOURCE_TYPES) {
                    set(playerId, resourceType, DEV_TERRA_FILL_AMOUNT);
                }
            }
            default -> {
            }
        }
    }

    public void clearClassResources(String playerId, String playerClass) {
        for (String resourceType : getResourceTypesForClass(playerClass)) {
            set(playerId, resourceType, 0);
        }
    }

    public void syncToPersistentState(PlayerData player) {
        if (player == null || player.getPlayerId() == null) {
            return;
        }

        Map<String, Integer> runtimeResources = playerResources.get(player.getPlayerId());
        player.setClassResources(runtimeResources == null ? new HashMap<>() : new HashMap<>(runtimeResources));
        player.setWaterContainerTier(getWaterContainerTier(player.getPlayerId()));
    }

    /**
     * Called when a Corruptus player kills a mob — gains souls.
     */
    public void onMobKilled(String playerId, String playerClass) {
        if ("corruptus".equalsIgnoreCase(playerClass)) {
            add(playerId, "souls", SOULS_PER_KILL);
        }
    }

    /**
     * Refill water for a Hydro player (at water source, rest, etc.).
     */
    public void refillWater(String playerId) {
        int capacity = getHydroCapacity(playerId);
        Map<String, Integer> resources = playerResources.get(playerId);
        if (resources != null && capacity > 0) {
            resources.put("water", capacity);
        }
    }

    /**
     * Upgrade water container tier. Returns true if upgrade was possible.
     */
    public boolean upgradeWaterContainer(String playerId) {
        int tier = waterContainerTier.getOrDefault(playerId, 0);
        if (tier >= WATER_CONTAINER_CAPACITY.length - 1) return false;
        waterContainerTier.put(playerId, tier + 1);
        return true;
    }

    /**
     * Get current water container name and capacity.
     */
    public String getWaterContainerInfo(String playerId) {
        int capacity = getHydroCapacity(playerId);
        if (capacity <= 0) {
            return "No Waterskin";
        }
        int tier = getEffectiveWaterContainerTier(playerId);
        return WATER_CONTAINER_NAMES[tier] + " (" + capacity + " capacity)";
    }

    public int getWaterContainerTier(String playerId) {
        return getEffectiveWaterContainerTier(playerId);
    }

    private String normalizePersistedResourceType(String playerClass, String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            return null;
        }

        if (!"terra".equalsIgnoreCase(playerClass)) {
            return resourceType;
        }

        return switch (resourceType.toLowerCase(Locale.ROOT)) {
            case "rocks" -> "stone_blocks";
            case "iron_ore" -> "metal";
            case "wood" -> "seeds";
            case "dirt" -> "dirt_blocks";
            case "sand" -> "sand_blocks";
            case "emerald" -> "gems";
            default -> resourceType;
        };
    }

    /**
     * Get max capacity for a resource type.
     */
    private int getMaxForResource(String playerId, String resourceType) {
        return switch (resourceType) {
            case "water" -> getHydroCapacity(playerId);
            case "souls" -> MAX_SOULS;
            default -> 999;
        };
    }

    private int getHydroCapacity(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return 0;
        }
        if (hydroInventoryBridge == null || !hydroInventoryBridge.hasHydroContainer(playerId)) {
            return 0;
        }
        return WATER_CONTAINER_CAPACITY[getEffectiveWaterContainerTier(playerId)];
    }

    private int getEffectiveWaterContainerTier(String playerId) {
        int storedTier = waterContainerTier.getOrDefault(playerId, 0);
        if (hydroInventoryBridge != null && hydroInventoryBridge.hasHydroContainer(playerId)) {
            storedTier = hydroInventoryBridge.getHydroContainerTier(playerId);
        }
        return Math.max(0, Math.min(storedTier, WATER_CONTAINER_CAPACITY.length - 1));
    }

    /**
     * Tick resource regeneration (called each server tick).
     * Reserved for future class resource behaviors.
     */
    public void tick() {
    }

    /**
     * Clean up when player disconnects.
     */
    public void onPlayerDisconnect(String playerId) {
        playerResources.remove(playerId);
        // Note: waterContainerTier persists in PlayerData and is restored on login.
    }

    /**
     * Get resource display for a player.
     */
    public String getResourceDisplay(String playerId, String playerClass) {
        Map<String, Integer> resources = getAllResources(playerId);
        if (playerClass == null || playerClass.isBlank()) {
            return "No resources";
        }

        StringBuilder sb = new StringBuilder();
        switch (playerClass.toLowerCase(Locale.ROOT)) {
            case "hydro" -> {
                int water = getAmount(playerId, "water");
                String container = getWaterContainerInfo(playerId);
                sb.append("Water: ").append(water).append(" | Container: ").append(container);
            }
            case "aero" -> sb.append("No class resource | Aero casts through cooldowns and movement.");
            case "corruptus" -> sb.append("Souls: ").append(getAmount(playerId, "souls"))
                    .append("/").append(MAX_SOULS);
            case "terra" -> {
                sb.append("Materials: ");
                List<String> parts = new ArrayList<>();
                for (String resourceType : TERRA_RESOURCE_TYPES) {
                    int amount = getAmount(playerId, resourceType);
                    if (amount > 0) {
                        parts.add(getDisplayName(resourceType) + "=" + amount);
                    }
                }
                sb.append(parts.isEmpty() ? "none" : String.join(", ", parts));
            }
            default -> sb.append("No resources");
        }
        return sb.toString();
    }

    private boolean isInventoryBackedTerraResource(String resourceType) {
        return resourceType != null && TERRA_RESOURCE_TYPES.contains(resourceType);
    }
}
