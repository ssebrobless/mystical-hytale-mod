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
    private final ClassPassiveManager classPassiveManager;
    private final java.util.function.Predicate<String> freeCastChecker;

    // playerId -> (abilityId -> remaining cooldown ticks)
    private final Map<String, Map<String, Integer>> cooldowns = new HashMap<>();
    // playerId -> (abilityId -> recharge timers for spent charges)
    private final Map<String, Map<String, List<Integer>>> chargeRecharges = new HashMap<>();
    // playerId -> currently active cast / recovery window
    private final Map<String, ActionWindow> actionWindows = new HashMap<>();
    // playerId -> (abilityId -> active toggle state)
    private final Map<String, Map<String, ToggleState>> activeToggles = new HashMap<>();

    public StyleManager(DataLoader dataLoader,
                        ResourceManager resourceManager,
                        ClassPassiveManager classPassiveManager,
                        java.util.function.Predicate<String> freeCastChecker) {
        this.dataLoader = dataLoader;
        this.resourceManager = resourceManager;
        this.classPassiveManager = classPassiveManager;
        this.freeCastChecker = freeCastChecker != null ? freeCastChecker : ignored -> false;
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
        chargeRecharges.remove(player.getPlayerId());
        actionWindows.remove(player.getPlayerId());
        activeToggles.remove(player.getPlayerId());
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
    public AbilityUseResult useAbility(PlayerData player, String abilityId) {
        AbilityData ability = findAbility(player, abilityId);
        if (ability == null) {
            return AbilityUseResult.failed("That ability is unavailable.");
        }

        String playerId = player.getPlayerId();
        StyleData style = findStyleForAbility(player, abilityId);
        String failureReason = buildUseFailureReason(player, ability, style);
        if (!failureReason.isBlank()) {
            return AbilityUseResult.failed(failureReason);
        }

        if (isToggleActive(playerId, abilityId)) {
            deactivateToggle(playerId, abilityId);
            double toggleCooldown = resolveToggleCooldownSeconds(ability);
            if (toggleCooldown > 0) {
                startCooldown(playerId, abilityId, toggleCooldown);
            }
            startActionWindow(playerId, ability, 0.0, getRecoveryTimeSeconds(ability));
            return AbilityUseResult.toggledOff(
                    ability,
                    getCurrentCharges(playerId, ability),
                    getMaxCharges(ability),
                    getRemainingCooldownSeconds(playerId, ability)
            );
        }

        if (!isFreeCastEnabled(playerId) && ability.getResourceCost() > 0 && style != null) {
            String resourceType = style.getResourceType();
            int resolvedCost = classPassiveManager != null
                    ? classPassiveManager.resolveAbilityResourceCost(player, style, ability)
                    : ability.getResourceCost();
            if (resolvedCost > 0 && !resourceManager.spend(playerId, resourceType, resolvedCost)) {
                return AbilityUseResult.failed("Not enough " + resourceManager.getDisplayName(resourceType) + ".");
            }
        }

        if (usesCharges(ability)) {
            spendCharge(playerId, ability);
        } else if (!isToggleable(ability)) {
            startCooldown(playerId, abilityId, ability.getCooldownSeconds());
        }

        if (isToggleable(ability)) {
            activateToggle(playerId, ability);
        }

        startActionWindow(playerId, ability);
        return AbilityUseResult.activated(
                ability,
                getCurrentCharges(playerId, ability),
                getMaxCharges(ability),
                isToggleActive(playerId, abilityId)
        );
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

    public boolean isOnCooldown(String playerId, AbilityData ability) {
        if (ability == null) {
            return false;
        }

        if (isOnCooldown(playerId, ability.getId())) {
            return true;
        }

        return usesCharges(ability)
                && getCurrentCharges(playerId, ability) <= 0
                && getNextChargeSeconds(playerId, ability) > 0;
    }

    public int getRemainingCooldown(String playerId, String abilityId) {
        Map<String, Integer> playerCooldowns = cooldowns.get(playerId);
        if (playerCooldowns == null) return 0;
        return playerCooldowns.getOrDefault(abilityId, 0);
    }

    public double getRemainingCooldownSeconds(String playerId, String abilityId) {
        return getRemainingCooldown(playerId, abilityId) / (double) TICKS_PER_SECOND;
    }

    public double getRemainingCooldownSeconds(String playerId, AbilityData ability) {
        if (ability == null) {
            return 0.0;
        }

        double standardCooldown = getRemainingCooldownSeconds(playerId, ability.getId());
        if (standardCooldown > 0) {
            return standardCooldown;
        }

        if (usesCharges(ability) && getCurrentCharges(playerId, ability) <= 0) {
            return getNextChargeSeconds(playerId, ability);
        }

        return 0.0;
    }

    public boolean isActionLocked(String playerId) {
        ActionWindow window = actionWindows.get(playerId);
        return window != null && !window.isFinished();
    }

    public boolean isToggleActive(String playerId, String abilityId) {
        Map<String, ToggleState> playerToggles = activeToggles.get(playerId);
        if (playerToggles == null) {
            return false;
        }

        ToggleState state = playerToggles.get(abilityId);
        return state != null && !state.isExpired();
    }

    public int getCurrentCharges(String playerId, AbilityData ability) {
        if (!usesCharges(ability)) {
            return 0;
        }

        int maxCharges = Math.max(0, ability.getCharges());
        int missingCharges = getRechargeTimers(playerId, ability.getId()).size();
        return Math.max(0, maxCharges - missingCharges);
    }

    public int getMaxCharges(AbilityData ability) {
        return ability == null ? 0 : Math.max(0, ability.getCharges());
    }

    public double getCastTimeSeconds(AbilityData ability) {
        if (ability == null) {
            return 0.0;
        }

        if (ability.getCastTimeSeconds() > 0) {
            return ability.getCastTimeSeconds();
        }

        if (ability.getDelaySeconds() > 0) {
            return Math.max(0.1, ability.getDelaySeconds());
        }

        String castType = safeLower(ability.getCastType());
        Set<String> categories = categorySet(ability);
        double base;

        if (categories.contains("summon") || categories.contains("transform") || categories.contains("ultimate")) {
            base = 0.9;
        } else if ("summon".equals(castType) || "transform".equals(castType) || "transformation".equals(castType)) {
            base = 0.85;
        } else if ("ground".equals(castType) || "area".equals(castType) || categories.contains("zone")) {
            base = 0.55;
        } else if ("projectile".equals(castType) || "beam".equals(castType) || categories.contains("ranged")) {
            base = 0.35;
        } else if ("dash".equals(castType) || "teleport".equals(castType) || categories.contains("mobility")) {
            base = 0.2;
        } else if (categories.contains("buff") || categories.contains("heal") || categories.contains("shield")) {
            base = 0.28;
        } else {
            base = 0.25;
        }

        return Math.min(base, Math.max(0.1, ability.getCooldownSeconds() * 0.4));
    }

    public double getRecoveryTimeSeconds(AbilityData ability) {
        if (ability == null) {
            return 0.0;
        }

        if (ability.getRecoverySeconds() > 0) {
            return ability.getRecoverySeconds();
        }

        String castType = safeLower(ability.getCastType());
        Set<String> categories = categorySet(ability);
        double base;

        if (categories.contains("summon") || categories.contains("transform") || categories.contains("ultimate")) {
            base = 0.5;
        } else if ("ground".equals(castType) || "area".equals(castType) || categories.contains("zone")) {
            base = 0.32;
        } else if ("projectile".equals(castType) || "beam".equals(castType) || categories.contains("ranged")) {
            base = 0.24;
        } else if ("dash".equals(castType) || "teleport".equals(castType) || categories.contains("mobility")) {
            base = 0.16;
        } else if (categories.contains("buff") || categories.contains("heal") || categories.contains("shield")) {
            base = 0.2;
        } else {
            base = 0.18;
        }

        return Math.min(base, Math.max(0.08, ability.getCooldownSeconds() * 0.25));
    }

    public ActionState getActionState(String playerId) {
        ActionWindow window = actionWindows.get(playerId);
        if (window == null || window.isFinished()) {
            return null;
        }

        return new ActionState(
                window.abilityId,
                window.abilityName,
                window.phase(),
                window.getRemainingSeconds(),
                window.getProgress()
        );
    }

    public AbilitySlotStatus getAbilitySlotStatus(PlayerData player, int slot) {
        if (player == null || slot < 1) {
            return AbilitySlotStatus.unavailable();
        }

        List<StyleData> styles = getSelectedStyles(player);
        if (styles.isEmpty()) {
            return AbilitySlotStatus.unavailable();
        }

        StyleData style = styles.get(0);
        if (style.getAbilities() == null || slot > style.getAbilities().size()) {
            return AbilitySlotStatus.unavailable();
        }

        AbilityData ability = style.getAbilities().get(slot - 1);
        int currentCharges = getCurrentCharges(player.getPlayerId(), ability);
        int maxCharges = getMaxCharges(ability);
        ActionWindow window = actionWindows.get(player.getPlayerId());
        if (window != null && !window.isFinished() && window.abilityId.equals(ability.getId())) {
            AbilityPhase phase = window.phase();
            String stateLabel = phase == AbilityPhase.CASTING
                    ? "Casting " + formatSeconds(window.getRemainingSeconds()) + "s"
                    : "Recover " + formatSeconds(window.getRemainingSeconds()) + "s";
            return new AbilitySlotStatus(
                    true,
                    ability.getId(),
                    ability.getName(),
                    phase,
                    window.getProgress(),
                    stateLabel,
                    window.getRemainingSeconds(),
                    currentCharges,
                    maxCharges,
                    false
            );
        }

        ToggleState toggleState = getToggleState(player.getPlayerId(), ability.getId());
        if (toggleState != null && !toggleState.isExpired()) {
            return new AbilitySlotStatus(
                    true,
                    ability.getId(),
                    ability.getName(),
                    AbilityPhase.ACTIVE,
                    toggleState.getProgress(),
                    toggleState.hasFiniteDuration()
                            ? "Active " + formatSeconds(toggleState.getRemainingSeconds()) + "s"
                            : "Active",
                    toggleState.getRemainingSeconds(),
                    currentCharges,
                    maxCharges,
                    true
            );
        }

        double cooldownRemaining = getRemainingCooldownSeconds(player.getPlayerId(), ability);
        if (cooldownRemaining > 0) {
            double progress = usesCharges(ability) && currentCharges <= 0
                    ? getChargeRechargeProgress(player.getPlayerId(), ability)
                    : getStandardCooldownProgress(player.getPlayerId(), ability);
            return new AbilitySlotStatus(
                    true,
                    ability.getId(),
                    ability.getName(),
                    AbilityPhase.COOLDOWN,
                    progress,
                    usesCharges(ability)
                            ? "Recharge " + formatSeconds(cooldownRemaining) + "s"
                            : "Cooldown " + formatSeconds(cooldownRemaining) + "s",
                    cooldownRemaining,
                    currentCharges,
                    maxCharges,
                    false
            );
        }

        return new AbilitySlotStatus(
                true,
                ability.getId(),
                ability.getName(),
                AbilityPhase.READY,
                maxCharges > 0 ? Math.max(0.0, Math.min(currentCharges / (double) maxCharges, 1.0)) : 1.0,
                maxCharges > 0 ? currentCharges + "/" + maxCharges : "Ready",
                0.0,
                currentCharges,
                maxCharges,
                false
        );
    }

    private void startCooldown(String playerId, String abilityId, double cooldownSeconds) {
        int ticks = secondsToTicks(cooldownSeconds);
        if (ticks <= 0) {
            Map<String, Integer> playerCooldowns = cooldowns.get(playerId);
            if (playerCooldowns != null) {
                playerCooldowns.remove(abilityId);
                if (playerCooldowns.isEmpty()) {
                    cooldowns.remove(playerId);
                }
            }
            return;
        }
        cooldowns.computeIfAbsent(playerId, k -> new HashMap<>()).put(abilityId, ticks);
    }

    private void startActionWindow(String playerId, AbilityData ability) {
        startActionWindow(playerId, ability, getCastTimeSeconds(ability), getRecoveryTimeSeconds(ability));
    }

    private void startActionWindow(String playerId, AbilityData ability, double castSeconds, double recoverySeconds) {
        int castTicks = secondsToTicks(castSeconds);
        int recoveryTicks = secondsToTicks(recoverySeconds);
        if (castTicks <= 0 && recoveryTicks <= 0) {
            actionWindows.remove(playerId);
            return;
        }

        actionWindows.put(playerId, new ActionWindow(
                ability.getId(),
                ability.getName(),
                castTicks,
                recoveryTicks
        ));
    }

    private int secondsToTicks(double seconds) {
        return Math.max(0, (int) Math.ceil(seconds * TICKS_PER_SECOND));
    }

    public double estimateCastTimeSeconds(AbilityData ability) {
        return getCastTimeSeconds(ability);
    }

    public double estimateRecoveryTimeSeconds(AbilityData ability) {
        return getRecoveryTimeSeconds(ability);
    }

    private Set<String> categorySet(AbilityData ability) {
        if (ability == null || ability.getCategories() == null || ability.getCategories().isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> categories = new HashSet<>();
        for (String category : ability.getCategories()) {
            if (category != null && !category.isBlank()) {
                categories.add(category.toLowerCase(Locale.ROOT));
            }
        }
        return categories;
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private String formatSeconds(double seconds) {
        return String.format(Locale.US, "%.1f", Math.max(0.0, seconds));
    }

    /**
     * Tick all cooldowns. Called each server tick.
     */
    public void tickCooldowns() {
        for (Map<String, Integer> playerCooldowns : cooldowns.values()) {
            playerCooldowns.replaceAll((id, ticks) -> Math.max(0, ticks - 1));
            playerCooldowns.values().removeIf(v -> v <= 0);
        }

        for (Map<String, List<Integer>> playerChargeMap : chargeRecharges.values()) {
            for (List<Integer> rechargeTimers : playerChargeMap.values()) {
                for (int index = 0; index < rechargeTimers.size(); index++) {
                    rechargeTimers.set(index, Math.max(0, rechargeTimers.get(index) - 1));
                }
                rechargeTimers.removeIf(ticks -> ticks <= 0);
            }
            playerChargeMap.values().removeIf(List::isEmpty);
        }
        chargeRecharges.values().removeIf(Map::isEmpty);

        Iterator<Map.Entry<String, ActionWindow>> iterator = actionWindows.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ActionWindow> entry = iterator.next();
            entry.getValue().tick();
            if (entry.getValue().isFinished()) {
                iterator.remove();
            }
        }

        Iterator<Map.Entry<String, Map<String, ToggleState>>> toggleIterator = activeToggles.entrySet().iterator();
        while (toggleIterator.hasNext()) {
            Map.Entry<String, Map<String, ToggleState>> entry = toggleIterator.next();
            String playerId = entry.getKey();
            Map<String, ToggleState> playerToggles = entry.getValue();
            Iterator<Map.Entry<String, ToggleState>> stateIterator = playerToggles.entrySet().iterator();
            while (stateIterator.hasNext()) {
                Map.Entry<String, ToggleState> stateEntry = stateIterator.next();
                ToggleState state = stateEntry.getValue();
                state.tick();
                if (state.isExpired()) {
                    stateIterator.remove();
                    double toggleCooldown = resolveToggleCooldownSeconds(state.ability());
                    if (toggleCooldown > 0) {
                        startCooldown(playerId, stateEntry.getKey(), toggleCooldown);
                    }
                }
            }

            if (playerToggles.isEmpty()) {
                toggleIterator.remove();
            }
        }
    }

    /**
     * Apply cooldown reduction (from race/perks). Reduces by a flat number of seconds.
     */
    public void applyCooldownReduction(String playerId, int reductionSeconds) {
        Map<String, Integer> playerCooldowns = cooldowns.get(playerId);
        int reductionTicks = reductionSeconds * TICKS_PER_SECOND;
        if (playerCooldowns != null) {
            playerCooldowns.replaceAll((id, ticks) -> Math.max(0, ticks - reductionTicks));
        }

        Map<String, List<Integer>> playerChargeMap = chargeRecharges.get(playerId);
        if (playerChargeMap != null) {
            for (List<Integer> rechargeTimers : playerChargeMap.values()) {
                for (int index = 0; index < rechargeTimers.size(); index++) {
                    rechargeTimers.set(index, Math.max(0, rechargeTimers.get(index) - reductionTicks));
                }
                rechargeTimers.removeIf(ticks -> ticks <= 0);
            }
            playerChargeMap.values().removeIf(List::isEmpty);
            if (playerChargeMap.isEmpty()) {
                chargeRecharges.remove(playerId);
            }
        }
    }

    /**
     * Reset all cooldowns for a player (on death, style change, etc.).
     */
    public void resetCooldowns(String playerId) {
        cooldowns.remove(playerId);
        chargeRecharges.remove(playerId);
        actionWindows.remove(playerId);
        activeToggles.remove(playerId);
    }

    /**
     * Clean up when player disconnects.
     */
    public void onPlayerDisconnect(String playerId) {
        cooldowns.remove(playerId);
        chargeRecharges.remove(playerId);
        actionWindows.remove(playerId);
        activeToggles.remove(playerId);
    }

    /**
     * Get a display summary of a style and its abilities.
     */
    public String getStyleSummary(StyleData style) {
        StringBuilder sb = new StringBuilder();
        sb.append(style.getName()).append(" (").append(style.getTheme()).append(")\n");
        sb.append("  Resource: ")
                .append(style.getResourceType() == null || style.getResourceType().isBlank()
                        ? "None"
                        : resourceManager.getDisplayName(style.getResourceType()))
                .append("\n");
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
                sb.append(" x").append(ability.getCharges())
                        .append(" @").append(formatSeconds(resolveChargeRechargeSeconds(ability))).append("s");
            }
            if (isToggleable(ability)) {
                sb.append(" [toggle]");
            }
            sb.append(" [cast ").append(formatSeconds(getCastTimeSeconds(ability)))
                    .append("s | rec ").append(formatSeconds(getRecoveryTimeSeconds(ability))).append("s]");
            String profile = AbilityPresentation.buildSpatialSummary(ability);
            sb.append("\n    ").append(profile.isBlank() ? ability.getDescription() : profile).append("\n");
        }
        return sb.toString();
    }

    public String getUseFailureReason(PlayerData player, AbilityData ability) {
        return buildUseFailureReason(player, ability, player != null && ability != null
                ? findStyleForAbility(player, ability.getId())
                : null);
    }

    private String buildUseFailureReason(PlayerData player, AbilityData ability, StyleData style) {
        if (player == null || ability == null) {
            return "Runtime player context is unavailable.";
        }

        String playerId = player.getPlayerId();
        if (isActionLocked(playerId)) {
            ActionState state = getActionState(playerId);
            if (state != null) {
                String phase = state.phase() == AbilityPhase.CASTING ? "casting" : "recovering";
                return state.abilityName() + " is still " + phase + " for "
                        + formatSeconds(state.remainingSeconds()) + "s.";
            }
            return "You are still locked into another action.";
        }

        if (isToggleActive(playerId, ability.getId())) {
            return "";
        }

        if (isOnCooldown(playerId, ability.getId())) {
            return ability.getName() + " is on cooldown for "
                    + formatSeconds(getRemainingCooldownSeconds(playerId, ability.getId())) + "s.";
        }

        if (usesCharges(ability) && getCurrentCharges(playerId, ability) <= 0) {
            return ability.getName() + " is recharging. Next charge in "
                    + formatSeconds(getNextChargeSeconds(playerId, ability)) + "s.";
        }

        if (!isFreeCastEnabled(playerId) && ability.getResourceCost() > 0 && style != null) {
            String resourceType = style.getResourceType();
            int currentResource = resourceManager.getAmount(playerId, resourceType);
            int resolvedCost = classPassiveManager != null
                    ? classPassiveManager.resolveAbilityResourceCost(player, style, ability)
                    : ability.getResourceCost();
            if (currentResource < resolvedCost) {
                return "Not enough " + resourceManager.getDisplayName(resourceType)
                        + ". Need " + resolvedCost + ", have " + currentResource + ".";
            }
        }

        return "";
    }

    private boolean usesCharges(AbilityData ability) {
        return ability != null && ability.getCharges() > 0;
    }

    private boolean isFreeCastEnabled(String playerId) {
        return playerId != null && freeCastChecker.test(playerId);
    }

    private boolean isToggleable(AbilityData ability) {
        if (ability == null) {
            return false;
        }

        if (ability.isToggleable()) {
            return true;
        }

        if ("transformation".equals(safeLower(ability.getCastType()))) {
            return true;
        }

        return categorySet(ability).contains("toggle");
    }

    private double resolveToggleCooldownSeconds(AbilityData ability) {
        if (ability == null) {
            return 0.0;
        }

        if (ability.getToggleCooldownSeconds() > 0) {
            return ability.getToggleCooldownSeconds();
        }

        return usesCharges(ability) ? 0.0 : ability.getCooldownSeconds();
    }

    private double resolveChargeRechargeSeconds(AbilityData ability) {
        if (ability == null) {
            return 0.0;
        }

        if (ability.getChargeRechargeSeconds() > 0) {
            return ability.getChargeRechargeSeconds();
        }

        return Math.max(0.1, ability.getCooldownSeconds());
    }

    private void spendCharge(String playerId, AbilityData ability) {
        if (!usesCharges(ability)) {
            return;
        }

        chargeRecharges
                .computeIfAbsent(playerId, ignored -> new HashMap<>())
                .computeIfAbsent(ability.getId(), ignored -> new ArrayList<>())
                .add(secondsToTicks(resolveChargeRechargeSeconds(ability)));
    }

    private List<Integer> getRechargeTimers(String playerId, String abilityId) {
        Map<String, List<Integer>> playerChargeMap = chargeRecharges.get(playerId);
        if (playerChargeMap == null) {
            return Collections.emptyList();
        }

        List<Integer> timers = playerChargeMap.get(abilityId);
        return timers != null ? timers : Collections.emptyList();
    }

    private double getNextChargeSeconds(String playerId, AbilityData ability) {
        if (!usesCharges(ability)) {
            return 0.0;
        }

        return getRechargeTimers(playerId, ability.getId()).stream()
                .min(Integer::compareTo)
                .map(ticks -> ticks / (double) TICKS_PER_SECOND)
                .orElse(0.0);
    }

    private double getChargeRechargeProgress(String playerId, AbilityData ability) {
        double nextChargeSeconds = getNextChargeSeconds(playerId, ability);
        double totalRechargeSeconds = Math.max(resolveChargeRechargeSeconds(ability), 0.1);
        return Math.max(0.0, Math.min(1.0 - (nextChargeSeconds / totalRechargeSeconds), 1.0));
    }

    private double getStandardCooldownProgress(String playerId, AbilityData ability) {
        double cooldownRemaining = getRemainingCooldownSeconds(playerId, ability.getId());
        double totalCooldown = Math.max(ability.getCooldownSeconds(), 0.1);
        return Math.max(0.0, Math.min(1.0 - (cooldownRemaining / totalCooldown), 1.0));
    }

    private ToggleState getToggleState(String playerId, String abilityId) {
        Map<String, ToggleState> playerToggles = activeToggles.get(playerId);
        if (playerToggles == null) {
            return null;
        }

        return playerToggles.get(abilityId);
    }

    private void activateToggle(String playerId, AbilityData ability) {
        if ("transformation".equals(safeLower(ability.getCastType()))) {
            clearOtherTransformationToggles(playerId, ability.getId());
        }

        int totalTicks = ability.getDurationSeconds() > 0
                ? secondsToTicks(ability.getDurationSeconds())
                : -1;

        activeToggles
                .computeIfAbsent(playerId, ignored -> new HashMap<>())
                .put(ability.getId(), new ToggleState(ability, totalTicks));
    }

    private void clearOtherTransformationToggles(String playerId, String activatedAbilityId) {
        Map<String, ToggleState> playerToggles = activeToggles.get(playerId);
        if (playerToggles == null || playerToggles.isEmpty()) {
            return;
        }

        Iterator<Map.Entry<String, ToggleState>> iterator = playerToggles.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, ToggleState> entry = iterator.next();
            if (activatedAbilityId.equals(entry.getKey())) {
                continue;
            }

            ToggleState state = entry.getValue();
            if (!"transformation".equals(safeLower(state.ability().getCastType()))) {
                continue;
            }

            iterator.remove();
            double toggleCooldown = resolveToggleCooldownSeconds(state.ability());
            if (toggleCooldown > 0) {
                startCooldown(playerId, entry.getKey(), toggleCooldown);
            }
        }

        if (playerToggles.isEmpty()) {
            activeToggles.remove(playerId);
        }
    }

    private void deactivateToggle(String playerId, String abilityId) {
        Map<String, ToggleState> playerToggles = activeToggles.get(playerId);
        if (playerToggles == null) {
            return;
        }

        playerToggles.remove(abilityId);
        if (playerToggles.isEmpty()) {
            activeToggles.remove(playerId);
        }
    }

    public enum AbilityPhase {
        READY,
        ACTIVE,
        CASTING,
        RECOVERY,
        COOLDOWN
    }

    public record ActionState(
            String abilityId,
            String abilityName,
            AbilityPhase phase,
            double remainingSeconds,
            double progress
    ) {
    }

    public record AbilitySlotStatus(
            boolean available,
            String abilityId,
            String abilityName,
            AbilityPhase phase,
            double progress,
            String stateLabel,
            double remainingSeconds,
            int currentCharges,
            int maxCharges,
            boolean toggleActive
    ) {
        public static AbilitySlotStatus unavailable() {
            return new AbilitySlotStatus(false, "", "", AbilityPhase.READY, 0.0, "Unavailable", 0.0, 0, 0, false);
        }
    }

    public record AbilityUseResult(
            boolean success,
            boolean toggledOff,
            AbilityData ability,
            String failureReason,
            int currentCharges,
            int maxCharges,
            boolean toggleActive,
            double cooldownSeconds
    ) {
        public static AbilityUseResult failed(String failureReason) {
            return new AbilityUseResult(false, false, null, failureReason, 0, 0, false, 0.0);
        }

        public static AbilityUseResult activated(AbilityData ability,
                                                 int currentCharges,
                                                 int maxCharges,
                                                 boolean toggleActive) {
            return new AbilityUseResult(true, false, ability, "", currentCharges, maxCharges, toggleActive, 0.0);
        }

        public static AbilityUseResult toggledOff(AbilityData ability,
                                                  int currentCharges,
                                                  int maxCharges,
                                                  double cooldownSeconds) {
            return new AbilityUseResult(true, true, ability, "", currentCharges, maxCharges, false, cooldownSeconds);
        }
    }

    private static final class ActionWindow {
        private final String abilityId;
        private final String abilityName;
        private final int castTicksTotal;
        private final int recoveryTicksTotal;
        private int castTicksRemaining;
        private int recoveryTicksRemaining;

        private ActionWindow(String abilityId, String abilityName, int castTicks, int recoveryTicks) {
            this.abilityId = abilityId;
            this.abilityName = abilityName;
            this.castTicksTotal = castTicks;
            this.recoveryTicksTotal = recoveryTicks;
            this.castTicksRemaining = castTicks;
            this.recoveryTicksRemaining = recoveryTicks;
        }

        private void tick() {
            if (castTicksRemaining > 0) {
                castTicksRemaining = Math.max(0, castTicksRemaining - 1);
            } else if (recoveryTicksRemaining > 0) {
                recoveryTicksRemaining = Math.max(0, recoveryTicksRemaining - 1);
            }
        }

        private boolean isFinished() {
            return castTicksRemaining <= 0 && recoveryTicksRemaining <= 0;
        }

        private AbilityPhase phase() {
            if (castTicksRemaining > 0) {
                return AbilityPhase.CASTING;
            }
            if (recoveryTicksRemaining > 0) {
                return AbilityPhase.RECOVERY;
            }
            return AbilityPhase.READY;
        }

        private double getRemainingSeconds() {
            int ticks = castTicksRemaining > 0 ? castTicksRemaining : recoveryTicksRemaining;
            return ticks / (double) TICKS_PER_SECOND;
        }

        private double getProgress() {
            if (castTicksRemaining > 0) {
                if (castTicksTotal <= 0) {
                    return 1.0;
                }
                return Math.max(0.0, Math.min(1.0 - (castTicksRemaining / (double) castTicksTotal), 1.0));
            }
            if (recoveryTicksRemaining > 0) {
                if (recoveryTicksTotal <= 0) {
                    return 1.0;
                }
                return Math.max(0.0, Math.min(1.0 - (recoveryTicksRemaining / (double) recoveryTicksTotal), 1.0));
            }
            return 1.0;
        }
    }

    private static final class ToggleState {
        private final AbilityData ability;
        private final int totalTicks;
        private int remainingTicks;

        private ToggleState(AbilityData ability, int totalTicks) {
            this.ability = ability;
            this.totalTicks = totalTicks;
            this.remainingTicks = totalTicks;
        }

        private AbilityData ability() {
            return ability;
        }

        private void tick() {
            if (remainingTicks > 0) {
                remainingTicks = Math.max(0, remainingTicks - 1);
            }
        }

        private boolean isExpired() {
            return totalTicks >= 0 && remainingTicks <= 0;
        }

        private boolean hasFiniteDuration() {
            return totalTicks >= 0;
        }

        private double getRemainingSeconds() {
            return hasFiniteDuration() ? remainingTicks / (double) TICKS_PER_SECOND : 0.0;
        }

        private double getProgress() {
            if (!hasFiniteDuration() || totalTicks <= 0) {
                return 1.0;
            }

            return Math.max(0.0, Math.min(remainingTicks / (double) totalTicks, 1.0));
        }
    }
}
