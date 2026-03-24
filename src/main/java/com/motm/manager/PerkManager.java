package com.motm.manager;

import com.motm.model.Perk;
import com.motm.model.PlayerData;
import com.motm.util.DataLoader;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Handles perk selection, validation, and application.
 * Translated from perk_logic.pseudo.
 */
public class PerkManager {

    private static final Logger LOG = Logger.getLogger("MOTM");

    public static final int PERKS_PER_TIER = 10;
    public static final int PERKS_TO_SELECT = 3;
    public static final int TOTAL_TIERS = 20;
    public static final int MILESTONE_INTERVAL = 10;
    public static final int MAX_TOTAL_PERKS = 60;

    private final DataLoader dataLoader;

    public PerkManager(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    // --- Tier Calculation ---

    public int getCurrentTier(int playerLevel) {
        if (playerLevel < 10) return 0;
        return Math.min(playerLevel / MILESTONE_INTERVAL, TOTAL_TIERS);
    }

    public int getMilestoneLevel(int tier) {
        return tier * MILESTONE_INTERVAL;
    }

    public boolean hasPendingPerkSelection(PlayerData player) {
        int currentTier = getCurrentTier(player.getLevel());
        int completedSelections = player.getPerkSelectionHistory().size();
        return currentTier > completedSelections;
    }

    public int getPendingSelectionTier(PlayerData player) {
        int completedSelections = player.getPerkSelectionHistory().size();
        int nextTier = completedSelections + 1;
        if (nextTier <= getCurrentTier(player.getLevel())) {
            return nextTier;
        }
        return 0;
    }

    // --- Perk Availability ---

    public List<Perk> getAvailablePerks(PlayerData player) {
        int pendingTier = getPendingSelectionTier(player);
        if (pendingTier == 0) return Collections.emptyList();

        List<Perk> classPerks = dataLoader.getPerksForClass(player.getPlayerClass());
        return classPerks.stream()
                .filter(p -> p.getTier() == pendingTier)
                .collect(Collectors.toList());
    }

    // --- Validation ---

    public ValidationResult validatePerkSelection(PlayerData player, List<String> selectedPerkIds) {
        ValidationResult result = new ValidationResult();

        // Check 1: Exactly 3 perks
        if (selectedPerkIds.size() != PERKS_TO_SELECT) {
            result.addError("Must select exactly " + PERKS_TO_SELECT + " perks");
            return result;
        }

        // Check 2: Pending selection available
        int pendingTier = getPendingSelectionTier(player);
        if (pendingTier == 0) {
            result.addError("No perk selection available at this time");
            return result;
        }

        // Check 3: All perks from correct tier
        List<Perk> available = getAvailablePerks(player);
        Set<String> availableIds = available.stream()
                .map(Perk::getId)
                .collect(Collectors.toSet());

        for (String perkId : selectedPerkIds) {
            if (!availableIds.contains(perkId)) {
                result.addError("Invalid perk selection: " + perkId);
            }
        }

        // Check 4: No duplicates
        if (new HashSet<>(selectedPerkIds).size() != selectedPerkIds.size()) {
            result.addError("Cannot select the same perk multiple times");
        }

        // Check 5: Not already owned
        for (String perkId : selectedPerkIds) {
            if (player.getSelectedPerks().contains(perkId)) {
                result.addError("Already own perk: " + perkId);
            }
        }

        return result;
    }

    // --- Perk Application ---

    public boolean applyPerkSelection(PlayerData player, List<String> selectedPerkIds,
                                      SynergyEngine synergyEngine) {
        ValidationResult validation = validatePerkSelection(player, selectedPerkIds);
        if (!validation.isValid()) {
            LOG.warning("[MOTM] Perk selection failed: " + validation.getErrors());
            return false;
        }

        int pendingTier = getPendingSelectionTier(player);

        // Add perks to player's collection
        player.getSelectedPerks().addAll(selectedPerkIds);

        // Record selection in history
        player.getPerkSelectionHistory().add(new PlayerData.PerkSelectionRecord(
                pendingTier, new ArrayList<>(selectedPerkIds), Instant.now().toString()
        ));

        // Apply perk effects
        for (String perkId : selectedPerkIds) {
            Perk perk = dataLoader.getPerkById(perkId, player.getPlayerClass());
            if (perk != null) {
                applyPerkEffects(player, perk);
            }
        }

        // Recalculate synergies
        synergyEngine.recalculateSynergies(player);

        LOG.info("[MOTM] " + player.getPlayerName() + " selected Tier " + pendingTier
                + " perks: " + selectedPerkIds);
        return true;
    }

    public void applyPerkEffects(PlayerData player, Perk perk) {
        if (perk.getEffects() == null) return;

        for (Perk.Effect effect : perk.getEffects()) {
            // Effect application depends on Hytale's entity/stat API.
            // The effect types from the pseudocode are:
            //   stat_increase, stat_multiplier, damage_increase, damage_reduction,
            //   ability, summon, passive, on_hit, on_kill, aura, transformation,
            //   conditional_buff, immunity
            //
            // TODO: Hook each effect type into Hytale's actual player stat/ability system.
            // For now, we log what would be applied.
            LOG.fine("[MOTM] Applying effect: " + effect.getType()
                    + " (value=" + effect.getValue() + ") from perk " + perk.getId());
        }
    }

    // --- Reapply on Load ---

    public void reapplyAllPerks(PlayerData player, SynergyEngine synergyEngine) {
        for (String perkId : player.getSelectedPerks()) {
            Perk perk = dataLoader.getPerkById(perkId, player.getPlayerClass());
            if (perk != null) {
                applyPerkEffects(player, perk);
            }
        }
        synergyEngine.recalculateSynergies(player);
    }

    // --- Query Helpers ---

    public List<Perk> getPlayerPerksForTier(PlayerData player, int tier) {
        List<Perk> result = new ArrayList<>();
        for (String perkId : player.getSelectedPerks()) {
            Perk perk = dataLoader.getPerkById(perkId, player.getPlayerClass());
            if (perk != null && perk.getTier() == tier) {
                result.add(perk);
            }
        }
        return result;
    }

    public List<Perk> getPerksByTag(PlayerData player, String tag) {
        List<Perk> result = new ArrayList<>();
        for (String perkId : player.getSelectedPerks()) {
            Perk perk = dataLoader.getPerkById(perkId, player.getPlayerClass());
            if (perk != null && perk.getSynergyTags() != null && perk.getSynergyTags().contains(tag)) {
                result.add(perk);
            }
        }
        return result;
    }

    public Map<String, Integer> countPerkTags(PlayerData player) {
        Map<String, Integer> tagCounts = new HashMap<>();
        for (String perkId : player.getSelectedPerks()) {
            Perk perk = dataLoader.getPerkById(perkId, player.getPlayerClass());
            if (perk != null && perk.getSynergyTags() != null) {
                for (String tag : perk.getSynergyTags()) {
                    tagCounts.merge(tag, 1, Integer::sum);
                }
            }
        }
        return tagCounts;
    }

    public int getRemainingPerkSlots(PlayerData player) {
        int maxPerks = getCurrentTier(player.getLevel()) * PERKS_TO_SELECT;
        return maxPerks - player.getSelectedPerks().size();
    }

    // --- Validation Result ---

    public static class ValidationResult {
        private boolean valid = true;
        private final List<String> errors = new ArrayList<>();

        public void addError(String error) {
            valid = false;
            errors.add(error);
        }

        public boolean isValid() { return valid; }
        public List<String> getErrors() { return errors; }
    }
}
