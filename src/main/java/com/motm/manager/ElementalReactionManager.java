package com.motm.manager;

import com.motm.model.ElementalMark;
import com.motm.model.ElementalReaction;
import com.motm.util.DataLoader;

import java.util.*;
import java.util.logging.Logger;

/**
 * Manages elemental marks on entities and detects/resolves elemental reactions.
 *
 * Flow:
 *   1. Player hits enemy with an elemental ability -> applyMark()
 *   2. If enemy already has a different synergy mark -> reaction triggers
 *   3. Reaction deals bonus damage and applies status effects
 *
 * Mark types:
 *   Synergy marks: WET, COLD, SHOCKED, COMBUSTIBLE, TARRED, CURSED
 *   Exception marks: GROUNDED (blocks flying), FLYING (blocks grounded)
 *
 * Reactions (6 total):
 *   Aero + Hydro    = Storm Surge  (+12%, stun+shocked)
 *   Hydro + Terra   = Mud Snare    (+10%, root+slow)
 *   Aero + Terra    = Dust Cyclone (+10%, blind+knockback)
 *   Corruptus+Hydro = Black Steam  (+11%, dot+slow)
 *   Corruptus+Terra = Gravebind    (+12%, root+vulnerability)
 *   Aero+Corruptus  = Hellstorm    (+14%, burn+stun)
 */
public class ElementalReactionManager {

    private static final Logger LOG = Logger.getLogger("MOTM");

    private final DataLoader dataLoader;
    private final StatusEffectManager statusEffectManager;

    // entityId -> list of active marks
    private final Map<String, List<ElementalMark>> activeMarks = new HashMap<>();

    public ElementalReactionManager(DataLoader dataLoader, StatusEffectManager statusEffectManager) {
        this.dataLoader = dataLoader;
        this.statusEffectManager = statusEffectManager;
    }

    /**
     * Apply an elemental mark to an entity.
     * If a reaction is triggered, returns the reaction; otherwise null.
     */
    public ReactionResult applyMark(String entityId, ElementalMark newMark) {
        List<ElementalMark> marks = activeMarks.computeIfAbsent(entityId, k -> new ArrayList<>());

        // Check cooldown — can't reapply same mark type too quickly
        for (ElementalMark existing : marks) {
            if (existing.getType() == newMark.getType() && existing.isOnCooldown()) {
                return null; // Same mark type still on cooldown
            }
        }

        // Handle exception marks (grounded/flying) — they don't trigger reactions
        if (newMark.isExceptionMark()) {
            handleExceptionMark(entityId, marks, newMark);
            return null;
        }

        // Check for reaction with existing synergy marks
        ReactionResult reaction = checkForReaction(entityId, marks, newMark);
        if (reaction != null) {
            return reaction;
        }

        // No reaction — just apply the mark
        // Remove existing mark of same type (refresh)
        marks.removeIf(m -> m.getType() == newMark.getType());
        marks.add(newMark);
        return null;
    }

    /**
     * Check if adding this mark triggers a reaction with existing marks.
     */
    private ReactionResult checkForReaction(String entityId, List<ElementalMark> marks, ElementalMark newMark) {
        for (ElementalMark existing : marks) {
            if (existing.isExpired() || existing.isExceptionMark()) continue;
            if (existing.getAppliedByElement().equals(newMark.getAppliedByElement())) continue;

            // Two different elements present — check for a matching reaction
            List<ElementalReaction> reactions = dataLoader.getElementalReactions();
            for (ElementalReaction reaction : reactions) {
                if (reaction.matches(existing.getAppliedByElement(), newMark.getAppliedByElement())) {
                    // Reaction found! Consume both marks and trigger effects
                    return triggerReaction(entityId, existing, newMark, reaction);
                }
            }
        }
        return null;
    }

    /**
     * Trigger a reaction — consume marks, apply bonus damage and effects.
     */
    private ReactionResult triggerReaction(String entityId, ElementalMark existingMark,
                                           ElementalMark newMark, ElementalReaction reaction) {
        // Consume marks
        if (existingMark.isConsumedOnHit()) {
            existingMark.consume();
        }

        // Remove consumed marks
        List<ElementalMark> marks = activeMarks.get(entityId);
        if (marks != null) {
            marks.removeIf(ElementalMark::isExpired);
        }

        // Apply status effects from the reaction
        String triggerPlayerId = newMark.getAppliedByPlayerId();
        for (String effectName : reaction.getAppliedEffects()) {
            applyReactionEffect(entityId, effectName, triggerPlayerId, reaction.getName());
        }

        LOG.info("[MOTM] Elemental Reaction: " + reaction.getName() + " triggered on " + entityId
                + " by " + triggerPlayerId + " (+"
                + (int)(reaction.getBonusDamagePercent()) + "% bonus damage)");

        return new ReactionResult(
                reaction.getName(),
                reaction.getBonusDamagePercent(),
                reaction.getAppliedEffects(),
                reaction.getColor(),
                existingMark.getAppliedByPlayerId(),
                newMark.getAppliedByPlayerId()
        );
    }

    /**
     * Apply a status effect from a reaction.
     */
    private void applyReactionEffect(String entityId, String effectName,
                                      String sourcePlayerId, String reactionName) {
        var effect = switch (effectName.toLowerCase()) {
            case "stun" -> new com.motm.model.StatusEffect(
                    com.motm.model.StatusEffect.Type.STUN, 1, 0, sourcePlayerId, reactionName);
            case "root" -> new com.motm.model.StatusEffect(
                    com.motm.model.StatusEffect.Type.ROOT, 2, 0, sourcePlayerId, reactionName);
            case "slow" -> new com.motm.model.StatusEffect(
                    com.motm.model.StatusEffect.Type.SLOW, 2, 0.2, sourcePlayerId, reactionName);
            case "burn" -> new com.motm.model.StatusEffect(
                    com.motm.model.StatusEffect.Type.BURN, 3, 0.03, sourcePlayerId, reactionName);
            case "dot" -> new com.motm.model.StatusEffect(
                    com.motm.model.StatusEffect.Type.DOT, 3, 0.05, sourcePlayerId, reactionName);
            case "blind" -> new com.motm.model.StatusEffect(
                    com.motm.model.StatusEffect.Type.BLIND, 2, 0, sourcePlayerId, reactionName);
            case "shocked" -> new com.motm.model.StatusEffect(
                    com.motm.model.StatusEffect.Type.SHOCKED, 3, 0, sourcePlayerId, reactionName);
            case "knockback" -> new com.motm.model.StatusEffect(
                    com.motm.model.StatusEffect.Type.KNOCKBACK, 1, 0, sourcePlayerId, reactionName);
            case "vulnerability" -> new com.motm.model.StatusEffect(
                    com.motm.model.StatusEffect.Type.VULNERABILITY, 3, 0.25, sourcePlayerId, reactionName);
            default -> null;
        };

        if (effect != null) {
            statusEffectManager.applyEffect(entityId, effect);
        }
    }

    /**
     * Handle exception marks (grounded/flying). They cancel each other.
     */
    private void handleExceptionMark(String entityId, List<ElementalMark> marks, ElementalMark newMark) {
        ElementalMark.MarkType oppositeType = newMark.getType() == ElementalMark.MarkType.GROUNDED
                ? ElementalMark.MarkType.FLYING
                : ElementalMark.MarkType.GROUNDED;

        // If the opposite mark is present, cancel both
        boolean cancelled = marks.removeIf(m -> m.getType() == oppositeType);
        if (cancelled) {
            LOG.info("[MOTM] Exception marks cancelled: " + newMark.getType() + " vs " + oppositeType);
            return;
        }

        // Otherwise apply the exception mark
        marks.removeIf(m -> m.getType() == newMark.getType()); // Remove existing same type
        marks.add(newMark);
    }

    /**
     * Get all active marks on an entity.
     */
    public List<ElementalMark> getMarks(String entityId) {
        return activeMarks.getOrDefault(entityId, Collections.emptyList());
    }

    /**
     * Check if an entity has a specific mark type.
     */
    public boolean hasMark(String entityId, ElementalMark.MarkType type) {
        return getMarks(entityId).stream().anyMatch(m -> m.getType() == type && !m.isExpired());
    }

    /**
     * Tick all marks — decrement durations and cooldowns, remove expired.
     */
    public void tickAll() {
        for (var entry : new ArrayList<>(activeMarks.entrySet())) {
            List<ElementalMark> marks = entry.getValue();
            marks.forEach(ElementalMark::tick);
            marks.removeIf(m -> m.isExpired() && !m.isOnCooldown());
        }
        activeMarks.entrySet().removeIf(e -> e.getValue().isEmpty());
    }

    /**
     * Clear all marks on an entity (on death, cleanse).
     */
    public void clearMarks(String entityId) {
        activeMarks.remove(entityId);
    }

    /**
     * Determine which mark type an ability applies based on the element and ability.
     */
    public static ElementalMark.MarkType getMarkTypeForAbility(String element, String abilityEffect) {
        if (abilityEffect == null || abilityEffect.isEmpty()) return null;

        return switch (element.toLowerCase()) {
            case "hydro" -> {
                // Frost abilities apply COLD, water abilities apply WET
                // Boiling abilities (Scald, Geyser, Overheat) don't apply combustible
                if (abilityEffect.contains("slow_stack") || abilityEffect.contains("freeze")) {
                    yield ElementalMark.MarkType.COLD;
                }
                yield ElementalMark.MarkType.WET;
            }
            case "aero" -> {
                if (abilityEffect.contains("lightning") || abilityEffect.contains("shocked")) {
                    yield ElementalMark.MarkType.SHOCKED;
                }
                if (abilityEffect.contains("dot") && abilityEffect.contains("slow")) {
                    yield ElementalMark.MarkType.TARRED; // Smoke/Pollution abilities
                }
                yield null; // Pure wind abilities don't apply marks
            }
            case "terra" -> {
                if (abilityEffect.contains("burn")) {
                    yield ElementalMark.MarkType.COMBUSTIBLE;
                }
                yield null; // Most terra abilities don't apply marks
            }
            case "corruptus" -> {
                yield ElementalMark.MarkType.CURSED;
            }
            default -> null;
        };
    }

    // --- Result record ---

    public record ReactionResult(
            String reactionName,
            double bonusDamagePercent,
            List<String> appliedEffects,
            String color,
            String firstPlayerContributor,
            String secondPlayerContributor
    ) {
        public boolean isCrossPlayerCombo() {
            return !firstPlayerContributor.equals(secondPlayerContributor);
        }
    }
}
