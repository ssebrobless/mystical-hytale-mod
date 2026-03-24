package com.motm.manager;

import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.model.StyleData;
import com.motm.util.AbilityPresentation;
import com.motm.util.DataLoader;

import java.util.*;
import java.util.logging.Logger;

/**
 * Manages style selection and ability usage for players.
 * Each player selects 1 style from their class's 10, gaining that style's abilities.
 * Tracks cooldowns and resolves ability activation.
 */
public class StyleManager {

    private static final Logger LOG = Logger.getLogger("MOTM");
    public static final int MAX_SELECTED_STYLES = 1;
    public static final int ABILITIES_PER_STYLE = 3;
    public static final int TICKS_PER_SECOND = 20; // Hytale server tick rate

    private final DataLoader dataLoader;
    private final ResourceManager resourceManager;

    // playerId -> (abilityId -> remaining cooldown ticks)
    private final Map<String, Map<String, Integer>> cooldowns = new HashMap<>();

    public StyleManager(DataLoader dataLoader, ResourceManager resourceManager) {
        this.dataLoader = dataLoader;
        this.resourceManager = resourceManager;
    }

    /**
     * Select styles for a player. Validates they belong to the player's class.
     * @return true if selection was valid and applied
     */
    public boolean selectStyles(PlayerData player, List<String> styleIds) {
        if (styleIds.size() > MAX_SELECTED_STYLES) return false;
        if (player.getPlayerClass() == null) return false;

        List<StyleData> allStyles = dataLoader.getStylesForClass(player.getPlayerClass());
        List<String> validIds = new ArrayList<>();

        for (String styleId : styleIds) {
            boolean found = allStyles.stream().anyMatch(s -> s.getId().equals(styleId));
            if (!found) {
                LOG.warning("[MOTM] Style " + styleId + " not found for class " + player.getPlayerClass());
                return false;
            }
            validIds.add(styleId);
        }

        player.setSelectedStyles(validIds);
        // Clear cooldowns when changing styles
        cooldowns.remove(player.getPlayerId());
        LOG.info("[MOTM] " + player.getPlayerName() + " selected styles: " + validIds);
        return true;
    }

    /**
     * Get the StyleData object for a player's selected style.
     */
    public List<StyleData> getSelectedStyles(PlayerData player) {
        List<String> selected = player.getSelectedStyles();
        if (selected == null || selected.isEmpty()) return Collections.emptyList();

        List<StyleData> allStyles = dataLoader.getStylesForClass(player.getPlayerClass());
        List<StyleData> result = new ArrayList<>();
        for (StyleData style : allStyles) {
            if (selected.contains(style.getId())) {
                result.add(style);
            }
        }
        return result;
    }

    /**
     * Get all abilities available to a player (from their selected styles).
     */
    public List<AbilityData> getAvailableAbilities(PlayerData player) {
        List<AbilityData> abilities = new ArrayList<>();
        for (StyleData style : getSelectedStyles(player)) {
            abilities.addAll(style.getAbilities());
        }
        return abilities;
    }

    /**
     * Attempt to use an ability. Checks cooldown and resource cost.
     * @return the AbilityData if usable, null if on cooldown or insufficient resources
     */
    public AbilityData useAbility(PlayerData player, String abilityId) {
        AbilityData ability = findAbility(player, abilityId);
        if (ability == null) return null;

        // Check cooldown
        if (isOnCooldown(player.getPlayerId(), abilityId)) {
            return null;
        }

        // Check resource cost
        if (ability.getResourceCost() > 0) {
            StyleData style = findStyleForAbility(player, abilityId);
            if (style != null) {
                String resourceType = style.getResourceType();
                if (!resourceManager.spend(player.getPlayerId(), resourceType, ability.getResourceCost())) {
                    return null; // Not enough resources
                }
            }
        }

        // Start cooldown
        startCooldown(player.getPlayerId(), abilityId, ability.getCooldownSeconds());
        return ability;
    }

    /**
     * Find an ability by ID across the player's selected styles.
     */
    public AbilityData findAbility(PlayerData player, String abilityId) {
        for (StyleData style : getSelectedStyles(player)) {
            for (AbilityData ability : style.getAbilities()) {
                if (ability.getId().equals(abilityId)) {
                    return ability;
                }
            }
        }
        return null;
    }

    /**
     * Find which style contains a given ability.
     */
    private StyleData findStyleForAbility(PlayerData player, String abilityId) {
        for (StyleData style : getSelectedStyles(player)) {
            for (AbilityData ability : style.getAbilities()) {
                if (ability.getId().equals(abilityId)) {
                    return style;
                }
            }
        }
        return null;
    }

    // --- Cooldown Management ---

    public boolean isOnCooldown(String playerId, String abilityId) {
        Map<String, Integer> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return false;
        Integer remaining = playerCooldowns.get(abilityId);
        return remaining != null && remaining > 0;
    }

    public int getRemainingCooldown(String playerId, String abilityId) {
        Map<String, Integer> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return 0;
        return playerCooldowns.getOrDefault(abilityId, 0);
    }

    public double getRemainingCooldownSeconds(String playerId, String abilityId) {
        return getRemainingCooldown(playerId, abilityId) / (double) TICKS_PER_SECOND;
    }

    private void startCooldown(String playerId, String abilityId, double cooldownSeconds) {
        int ticks = (int) (cooldownSeconds * TICKS_PER_SECOND);
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>()).put(abilityId, ticks);
    }

    /**
     * Tick all cooldowns. Called each server tick.
     */
    public void tickCooldowns() {
        for (Map<String, Integer> playerCooldowns : cooldowns.values()) {
            playerCooldowns.replaceAll((id, ticks) -> Math.max(0, ticks - 1));
            playerCooldowns.values().removeIf(v -> v <= 0);
        }
    }

    /**
     * Apply cooldown reduction (from race/perks). Reduces by a flat number of seconds.
     */
    public void applyCooldownReduction(String playerId, int reductionSeconds) {
        Map<String, Integer> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return;
        int reductionTicks = reductionSeconds * TICKS_PER_SECOND;
        playerCooldowns.replaceAll((id, ticks) -> Math.max(0, ticks - reductionTicks));
    }

    /**
     * Reset all cooldowns for a player (on death, style change, etc.).
     */
    public void resetCooldowns(String playerId) {
        cooldowns.remove(playerId);
    }

    /**
     * Clean up when player disconnects.
     */
    public void onPlayerDisconnect(String playerId) {
        cooldowns.remove(playerId);
    }

    /**
     * Get a display summary of a style and its abilities.
     */
    public String getStyleSummary(StyleData style) {
        StringBuilder sb = new StringBuilder();
        sb.append(style.getName()).append(" (").append(style.getTheme()).append(")\n");
        sb.append("  Resource: ").append(style.getResourceType()).append("\n");
        for (AbilityData ability : style.getAbilities()) {
            sb.append("  - ").append(ability.getName());
            if (ability.getDamagePercent() > 0) {
                sb.append(" [").append((int) ability.getDamagePercent()).append("% dmg]");
            }
            if (ability.getHealPercent() > 0) {
                sb.append(" [").append((int) ability.getHealPercent()).append("% heal]");
            }
            if (ability.getShieldPercent() > 0) {
                sb.append(" [").append((int) ability.getShieldPercent()).append("% shield]");
            }
            sb.append(" (").append(ability.getCooldownSeconds()).append("s cd)");
            if (ability.getCharges() > 0) {
                sb.append(" x").append(ability.getCharges());
            }
            String profile = AbilityPresentation.buildSpatialSummary(ability);
            sb.append("\n    ").append(profile.isBlank() ? ability.getDescription() : profile).append("\n");
        }
        return sb.toString();
    }
}
