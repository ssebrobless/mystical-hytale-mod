package com.motm.manager;

import com.motm.model.PlayerData;

import java.util.*;
import java.util.logging.Logger;

/**
 * Manages class-specific resource systems.
 *
 * Resource types by class:
 *   - Terra: Physical materials (rocks, iron_ore, wood, seeds, dirt, sand, emerald)
 *   - Hydro: Water (stored in containers with tiered capacity: 10/20/35/50/75)
 *   - Aero: TP (technique points, regenerates over time, max 100)
 *   - Corruptus: Souls (earned from kills, max 50)
 */
public class ResourceManager {

    private static final Logger LOG = Logger.getLogger("MOTM");

    // Max resource values
    public static final int MAX_SOULS = 50;
    public static final int MAX_TP = 100;
    public static final int TP_REGEN_PER_TICK = 1; // Regenerate 1 TP per tick
    public static final int TP_REGEN_INTERVAL = 20; // Every 20 ticks (1 second)
    public static final int SOULS_PER_KILL = 3;

    // Water container capacities by tier
    public static final int[] WATER_CONTAINER_CAPACITY = {10, 20, 35, 50, 75};
    public static final String[] WATER_CONTAINER_NAMES = {
            "Waterskin", "Canteen", "Water Flask", "Reservoir Pouch", "Elemental Ewer"
    };

    // playerId -> (resourceType -> currentAmount)
    private final Map<String, Map<String, Integer>> playerResources = new HashMap<>();
    // playerId -> water container tier (0-4)
    private final Map<String, Integer> waterContainerTier = new HashMap<>();
    // Tick counter for TP regen
    private int tickCounter = 0;

    /**
     * Initialize resources for a player based on their class.
     */
    public void initializeForPlayer(String playerId, String playerClass) {
        Map<String, Integer> resources = new HashMap<>();

        switch (playerClass.toLowerCase()) {
            case "hydro" -> {
                int tier = waterContainerTier.getOrDefault(playerId, 0);
                int capacity = WATER_CONTAINER_CAPACITY[tier];
                resources.put("water", capacity); // Start full
            }
            case "aero" -> resources.put("tp", MAX_TP); // Start full
            case "corruptus" -> resources.put("souls", 0); // Start empty, earn from kills
            case "terra" -> {
                // Terra uses multiple material types, all start at 0
                resources.put("rocks", 0);
                resources.put("iron_ore", 0);
                resources.put("wood", 0);
                resources.put("seeds", 0);
                resources.put("dirt", 0);
                resources.put("sand", 0);
                resources.put("emerald", 0);
            }
        }

        playerResources.put(playerId, resources);
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
        if (resources == null) return 0;
        return resources.getOrDefault(resourceType, 0);
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
        Map<String, Integer> resources = playerResources.get(playerId);
        if (resources == null) return false;

        int current = resources.getOrDefault(resourceType, 0);
        if (current < amount) return false;

        resources.put(resourceType, current - amount);
        return true;
    }

    /**
     * Add a resource (from gathering, kills, etc.).
     */
    public void add(String playerId, String resourceType, int amount) {
        Map<String, Integer> resources = playerResources.computeIfAbsent(playerId, k -> new HashMap<>());
        int current = resources.getOrDefault(resourceType, 0);
        int max = getMaxForResource(playerId, resourceType);
        resources.put(resourceType, Math.min(current + amount, max));
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
        int tier = waterContainerTier.getOrDefault(playerId, 0);
        int capacity = WATER_CONTAINER_CAPACITY[tier];
        Map<String, Integer> resources = playerResources.get(playerId);
        if (resources != null) {
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
        int tier = waterContainerTier.getOrDefault(playerId, 0);
        return WATER_CONTAINER_NAMES[tier] + " (" + WATER_CONTAINER_CAPACITY[tier] + " capacity)";
    }

    public int getWaterContainerTier(String playerId) {
        return waterContainerTier.getOrDefault(playerId, 0);
    }

    /**
     * Get max capacity for a resource type.
     */
    private int getMaxForResource(String playerId, String resourceType) {
        return switch (resourceType) {
            case "water" -> {
                int tier = waterContainerTier.getOrDefault(playerId, 0);
                yield WATER_CONTAINER_CAPACITY[tier];
            }
            case "tp" -> MAX_TP;
            case "souls" -> MAX_SOULS;
            // Terra materials have no hard cap, but practical limit
            default -> 999;
        };
    }

    /**
     * Tick resource regeneration (called each server tick).
     * Currently only Aero TP regenerates over time.
     */
    public void tick() {
        tickCounter++;
        if (tickCounter < TP_REGEN_INTERVAL) return;
        tickCounter = 0;

        for (Map.Entry<String, Map<String, Integer>> entry : playerResources.entrySet()) {
            Map<String, Integer> resources = entry.getValue();
            if (resources.containsKey("tp")) {
                int current = resources.get("tp");
                if (current < MAX_TP) {
                    resources.put("tp", Math.min(current + TP_REGEN_PER_TICK, MAX_TP));
                }
            }
        }
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
        if (resources.isEmpty()) return "No resources";

        StringBuilder sb = new StringBuilder();
        switch (playerClass.toLowerCase()) {
            case "hydro" -> {
                int water = resources.getOrDefault("water", 0);
                String container = getWaterContainerInfo(playerId);
                sb.append("Water: ").append(water).append(" | Container: ").append(container);
            }
            case "aero" -> sb.append("TP: ").append(resources.getOrDefault("tp", 0))
                    .append("/").append(MAX_TP);
            case "corruptus" -> sb.append("Souls: ").append(resources.getOrDefault("souls", 0))
                    .append("/").append(MAX_SOULS);
            case "terra" -> {
                sb.append("Materials: ");
                List<String> parts = new ArrayList<>();
                for (var entry : resources.entrySet()) {
                    if (entry.getValue() > 0) {
                        parts.add(entry.getKey() + "=" + entry.getValue());
                    }
                }
                sb.append(parts.isEmpty() ? "none" : String.join(", ", parts));
            }
        }
        return sb.toString();
    }
}
