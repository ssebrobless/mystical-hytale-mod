package com.motm.manager;

import com.motm.model.Perk;
import com.motm.model.PlayerData;
import com.motm.model.PlayerData.ActiveSynergy;
import com.motm.util.DataLoader;

import java.util.*;
import java.util.logging.Logger;

/**
 * Handles synergy detection, calculation, and application between perks.
 * Translated from perk_synergy_engine.pseudo.
 */
public class SynergyEngine {

    private static final Logger LOG = Logger.getLogger("MOTM");

    private final DataLoader dataLoader;

    public SynergyEngine(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    // --- Main Synergy Calculation ---

    public void recalculateSynergies(PlayerData player) {
        if (player.getPlayerClass() == null) return;

        player.clearSynergyBonuses();

        Map<String, Integer> tagCounts = countAllTags(player);

        // Check each owned perk for synergy bonuses
        for (String perkId : player.getSelectedPerks()) {
            Perk perk = dataLoader.getPerkById(perkId, player.getPlayerClass());
            if (perk == null || perk.getSynergyBonuses() == null) continue;

            for (Perk.SynergyBonus synergy : perk.getSynergyBonuses()) {
                if (synergy.getRequiresTags() == null) continue;

                if (isSynergyMet(synergy.getRequiresTags(), tagCounts)) {
                    ActiveSynergy active = new ActiveSynergy(
                            perkId,
                            synergy.getBonus().getType(),
                            synergy.getBonus().getValue(),
                            synergy.getRequiresTags(),
                            findContributingPerks(player, synergy.getRequiresTags())
                    );
                    player.getActiveSynergyBonuses().add(active);
                    applySynergyBonus(player, synergy.getBonus());
                }
            }
        }

        // Process enhancement chains
        processEnhancementChains(player);

        logSynergySummary(player);
    }

    // --- Tag Counting ---

    public Map<String, Integer> countAllTags(PlayerData player) {
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

    // --- Synergy Matching ---

    public boolean isSynergyMet(List<String> requiredTags, Map<String, Integer> availableTags) {
        Map<String, Integer> requiredCounts = new HashMap<>();
        for (String tag : requiredTags) {
            requiredCounts.merge(tag, 1, Integer::sum);
        }

        for (var entry : requiredCounts.entrySet()) {
            int available = availableTags.getOrDefault(entry.getKey(), 0);
            if (available < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private List<String> findContributingPerks(PlayerData player, List<String> requiredTags) {
        List<String> contributing = new ArrayList<>();
        List<String> remaining = new ArrayList<>(requiredTags);

        for (String perkId : player.getSelectedPerks()) {
            Perk perk = dataLoader.getPerkById(perkId, player.getPlayerClass());
            if (perk == null || perk.getSynergyTags() == null) continue;

            boolean contributed = false;
            for (String tag : perk.getSynergyTags()) {
                if (remaining.remove(tag)) {
                    contributed = true;
                }
            }
            if (contributed) {
                contributing.add(perkId);
            }
            if (remaining.isEmpty()) break;
        }

        return contributing;
    }

    // --- Synergy Bonus Application ---

    private void applySynergyBonus(PlayerData player, Perk.Effect bonus) {
        String type = bonus.getType();
        double value = bonus.getValue();

        switch (type) {
            case "damage_reduction" -> {
                String element = bonus.getElement() != null ? bonus.getElement() : "all";
                player.getSynergyDamageReduction().merge(element, value, Double::sum);
            }
            case "damage_increase" -> {
                String element = bonus.getElement() != null ? bonus.getElement() : "all";
                player.getSynergyDamageIncrease().merge(element, value, Double::sum);
            }
            case "stat_increase" -> {
                String stat = bonus.getStat();
                if (stat != null) {
                    player.getSynergyStatBonuses().merge(stat, value, Double::sum);
                }
            }
            case "heal_increase" -> player.setSynergyHealingBonus(player.getSynergyHealingBonus() + value);
            case "crit_chance" -> player.setSynergyCritChance(player.getSynergyCritChance() + value);
            case "crit_damage" -> player.setSynergyCritDamage(player.getSynergyCritDamage() + value);
            case "cooldown_reduction" -> {
                String ability = bonus.getAbility() != null ? bonus.getAbility() : "all";
                player.getSynergyCooldownReduction().merge(ability, value, Double::sum);
            }
            case "duration_increase" -> {
                String effectType = bonus.getEffectType() != null ? bonus.getEffectType() : "all";
                player.getSynergyDurationBonus().merge(effectType, value, Double::sum);
            }
            case "chain_increase" -> player.setSynergyChainBonus(player.getSynergyChainBonus() + value);
            case "radius_increase" -> player.setSynergyRadiusBonus(player.getSynergyRadiusBonus() + value);
            default -> LOG.warning("[MOTM] Unknown synergy bonus type: " + type);
        }
    }

    // --- Enhancement Chains ---

    private void processEnhancementChains(PlayerData player) {
        for (String perkId : player.getSelectedPerks()) {
            Perk perk = dataLoader.getPerkById(perkId, player.getPlayerClass());
            if (perk == null || perk.getEnhancedBy() == null) continue;

            for (String enhancerId : perk.getEnhancedBy()) {
                if (player.getSelectedPerks().contains(enhancerId)) {
                    applyEnhancement(player, perk, enhancerId);
                }
            }
        }
    }

    private void applyEnhancement(PlayerData player, Perk basePerk, String enhancerId) {
        double enhancementMultiplier = 1.25;

        if (basePerk.getEffects() == null) return;

        for (Perk.Effect effect : basePerk.getEffects()) {
            if (effect.getValue() != 0) {
                // TODO: Register enhanced effect with increased value (effect.value * 1.25)
                // This requires hooking into Hytale's effect system to modify applied effects
                LOG.fine("[MOTM] Perk " + basePerk.getId() + " enhanced by " + enhancerId
                        + " (x" + enhancementMultiplier + ")");
            }
        }
    }

    // --- Synergy Preview (for UI) ---

    public SynergyPreview previewSynergyChanges(PlayerData player, List<String> candidatePerkIds) {
        SynergyPreview preview = new SynergyPreview();

        // Simulate having these perks
        Set<String> simulatedPerks = new HashSet<>(player.getSelectedPerks());
        simulatedPerks.addAll(candidatePerkIds);

        // Calculate simulated tag counts
        Map<String, Integer> simulatedTags = new HashMap<>();
        for (String perkId : simulatedPerks) {
            Perk perk = dataLoader.getPerkById(perkId, player.getPlayerClass());
            if (perk != null && perk.getSynergyTags() != null) {
                for (String tag : perk.getSynergyTags()) {
                    simulatedTags.merge(tag, 1, Integer::sum);
                }
            }
        }

        // Find new synergies that would activate
        for (String perkId : simulatedPerks) {
            Perk perk = dataLoader.getPerkById(perkId, player.getPlayerClass());
            if (perk == null || perk.getSynergyBonuses() == null) continue;

            for (Perk.SynergyBonus synergy : perk.getSynergyBonuses()) {
                if (synergy.getRequiresTags() == null) continue;
                if (!isSynergyMet(synergy.getRequiresTags(), simulatedTags)) continue;

                // Check if it wasn't already active
                boolean alreadyActive = player.getActiveSynergyBonuses().stream()
                        .anyMatch(a -> a.getSourcePerk().equals(perkId)
                                && a.getBonusType().equals(synergy.getBonus().getType())
                                && a.getBonusValue() == synergy.getBonus().getValue());

                if (!alreadyActive) {
                    preview.newSynergies.add(new PreviewSynergy(
                            perkId, synergy.getBonus(), synergy.getRequiresTags()
                    ));
                }
            }
        }

        return preview;
    }

    // --- Query Helpers ---

    public double getSynergyDamageModifier(PlayerData player, String damageType) {
        double modifier = 1.0;
        modifier += player.getSynergyDamageIncrease().getOrDefault(damageType, 0.0);
        modifier += player.getSynergyDamageIncrease().getOrDefault("all", 0.0);
        return modifier;
    }

    public double getSynergyDamageReduction(PlayerData player, String damageType) {
        double reduction = 0;
        reduction += player.getSynergyDamageReduction().getOrDefault(damageType, 0.0);
        reduction += player.getSynergyDamageReduction().getOrDefault("all", 0.0);
        return Math.min(reduction, 0.80); // Cap at 80%
    }

    // --- Logging ---

    private void logSynergySummary(PlayerData player) {
        LOG.fine("[MOTM] === Synergy Summary for " + player.getPlayerName() + " ===");
        LOG.fine("[MOTM] Active Synergies: " + player.getActiveSynergyBonuses().size());
        for (ActiveSynergy s : player.getActiveSynergyBonuses()) {
            LOG.fine("[MOTM]   " + s.getSourcePerk() + ": " + s.getBonusType() + " +" + s.getBonusValue());
        }
    }

    // --- Preview Data Classes ---

    public static class SynergyPreview {
        public final List<PreviewSynergy> newSynergies = new ArrayList<>();
    }

    public static class PreviewSynergy {
        public final String sourcePerk;
        public final Perk.Effect bonus;
        public final List<String> triggeredBy;

        public PreviewSynergy(String sourcePerk, Perk.Effect bonus, List<String> triggeredBy) {
            this.sourcePerk = sourcePerk;
            this.bonus = bonus;
            this.triggeredBy = triggeredBy;
        }
    }
}
