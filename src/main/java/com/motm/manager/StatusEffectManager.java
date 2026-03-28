package com.motm.manager;

import com.motm.model.StatusEffect;

import java.util.*;
import java.util.logging.Logger;

/**
 * Manages active status effects on entities.
 * Each entity (identified by a string ID) can have multiple concurrent effects.
 * Effects tick each server tick and are removed when expired.
 */
public class StatusEffectManager {

    private static final Logger LOG = Logger.getLogger("MOTM");

    // entityId -> list of active effects
    private final Map<String, List<StatusEffect>> activeEffects = new HashMap<>();

    /**
     * Apply a status effect to an entity.
     * Some effects stack (SLOW_STACK), others refresh duration if already present.
     */
    public void applyEffect(String entityId, StatusEffect effect) {
        List<StatusEffect> effects = activeEffects.computeIfAbsent(entityId, k -> new ArrayList<>());

        if (effect.getType() == StatusEffect.Type.SLOW_STACK) {
            // Stacking slows always add
            effects.add(effect);
            return;
        }

        // For non-stacking effects, refresh if same type already present
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).getType() == effect.getType()) {
                effects.set(i, effect); // Replace with fresh instance
                return;
            }
        }

        effects.add(effect);
    }

    /**
     * Remove all effects of a given type from an entity.
     */
    public void removeEffect(String entityId, StatusEffect.Type type) {
        List<StatusEffect> effects = activeEffects.get(entityId);
        if (effects != null) {
            effects.removeIf(e -> e.getType() == type);
        }
    }

    /**
     * Remove all effects from an entity (on death, cleanse, etc.).
     */
    public void clearEffects(String entityId) {
        activeEffects.remove(entityId);
    }

    /**
     * Remove all effects coming from a specific source ability or perk.
     */
    public int clearEffectsFromSource(String entityId, String sourcePerkOrAbility) {
        if (entityId == null || sourcePerkOrAbility == null || sourcePerkOrAbility.isBlank()) {
            return 0;
        }

        List<StatusEffect> effects = activeEffects.get(entityId);
        if (effects == null || effects.isEmpty()) {
            return 0;
        }

        int before = effects.size();
        effects.removeIf(effect -> sourcePerkOrAbility.equalsIgnoreCase(effect.getSourcePerkOrAbility()));
        if (effects.isEmpty()) {
            activeEffects.remove(entityId);
        }
        return Math.max(0, before - effects.size());
    }

    /**
     * Tick all effects for a given entity. Returns damage-over-time total as % of max HP.
     */
    public double tickEffects(String entityId) {
        List<StatusEffect> effects = activeEffects.get(entityId);
        if (effects == null || effects.isEmpty()) return 0;

        double dotDamagePercent = 0;

        Iterator<StatusEffect> it = effects.iterator();
        while (it.hasNext()) {
            StatusEffect effect = it.next();

            // Accumulate DoT damage before ticking
            if (effect.isDamageOverTime() && !effect.isExpired()) {
                dotDamagePercent += effect.getDamagePerTickPercent();
            }

            effect.tick();

            if (effect.isExpired()) {
                it.remove();
            }
        }

        return dotDamagePercent;
    }

    /**
     * Tick all tracked entities. Returns a map of entityId -> DoT damage percent.
     */
    public Map<String, Double> tickAll() {
        Map<String, Double> dotDamage = new HashMap<>();
        for (String entityId : new ArrayList<>(activeEffects.keySet())) {
            double dmg = tickEffects(entityId);
            if (dmg > 0) {
                dotDamage.put(entityId, dmg);
            }
        }
        // Clean up empty entries
        activeEffects.entrySet().removeIf(e -> e.getValue().isEmpty());
        return dotDamage;
    }

    /**
     * Check if an entity has a specific effect type active.
     */
    public boolean hasEffect(String entityId, StatusEffect.Type type) {
        List<StatusEffect> effects = activeEffects.get(entityId);
        if (effects == null) return false;
        return effects.stream().anyMatch(e -> e.getType() == type && !e.isExpired());
    }

    /**
     * Get all active effects for an entity.
     */
    public List<StatusEffect> getEffects(String entityId) {
        return activeEffects.getOrDefault(entityId, Collections.emptyList());
    }

    /**
     * Whether the entity is prevented from acting (stunned/frozen).
     */
    public boolean isIncapacitated(String entityId) {
        return getEffects(entityId).stream().anyMatch(StatusEffect::preventsAction);
    }

    /**
     * Whether the entity is prevented from moving (stunned/frozen/rooted).
     */
    public boolean isImmobilized(String entityId) {
        return getEffects(entityId).stream().anyMatch(StatusEffect::preventsMovement);
    }

    /**
     * Calculate total damage reduction from defense buffs.
     */
    public double getDamageReduction(String entityId) {
        return getEffects(entityId).stream()
                .filter(e -> e.getType() == StatusEffect.Type.DEFENSE_BUFF)
                .mapToDouble(StatusEffect::getValue)
                .sum();
    }

    /**
     * Calculate total damage increase from attack buffs.
     */
    public double getDamageIncrease(String entityId) {
        return getEffects(entityId).stream()
                .filter(e -> e.getType() == StatusEffect.Type.ATTACK_BUFF)
                .mapToDouble(StatusEffect::getValue)
                .sum();
    }

    /**
     * Get movement / haste bonus from speed effects.
     */
    public double getSpeedBonus(String entityId) {
        return getEffects(entityId).stream()
                .filter(e -> e.getType() == StatusEffect.Type.SPEED_BUFF)
                .mapToDouble(StatusEffect::getValue)
                .sum();
    }

    /**
     * Get total shield HP remaining on an entity.
     */
    public double getShieldHp(String entityId) {
        return getEffects(entityId).stream()
                .filter(e -> e.getType() == StatusEffect.Type.SHIELD)
                .mapToDouble(StatusEffect::getValue)
                .sum();
    }

    /**
     * Absorb damage through shields. Returns remaining damage after shield absorption.
     */
    public double absorbDamage(String entityId, double damage) {
        List<StatusEffect> effects = activeEffects.get(entityId);
        if (effects == null) return damage;

        double remaining = damage;
        Iterator<StatusEffect> it = effects.iterator();
        while (it.hasNext() && remaining > 0) {
            StatusEffect effect = it.next();
            if (effect.getType() == StatusEffect.Type.SHIELD) {
                double shieldHp = effect.getValue();
                if (shieldHp <= remaining) {
                    remaining -= shieldHp;
                    it.remove();
                } else {
                    effect.setValue(shieldHp - remaining);
                    remaining = 0;
                }
            }
        }
        return remaining;
    }

    /**
     * Consume a one-shot buff (DAMAGE_BUFF, STEALTH) and return its value.
     * Returns 0 if no such effect is active.
     */
    public double consumeOneShot(String entityId, StatusEffect.Type type) {
        List<StatusEffect> effects = activeEffects.get(entityId);
        if (effects == null) return 0;

        for (StatusEffect effect : effects) {
            if (effect.getType() == type && !effect.isConsumed()) {
                double value = effect.getValue();
                effect.consume();
                return value;
            }
        }
        return 0;
    }

    /**
     * Get the cumulative slow multiplier (for SLOW_STACK effects).
     * Each stack multiplies, so 3 stacks of 20% slow = 0.8^3 = 0.512 speed multiplier.
     */
    public double getSlowMultiplier(String entityId) {
        List<StatusEffect> effects = activeEffects.get(entityId);
        if (effects == null) return 1.0;

        double multiplier = 1.0;
        for (StatusEffect effect : effects) {
            if (effect.getType() == StatusEffect.Type.SLOW
                    || effect.getType() == StatusEffect.Type.SLOW_STACK) {
                multiplier *= (1.0 - effect.getValue());
            }
        }
        return multiplier;
    }

    /**
     * Get evasion chance from evasion effects.
     */
    public double getEvasionChance(String entityId) {
        return getEffects(entityId).stream()
                .filter(e -> e.getType() == StatusEffect.Type.EVASION)
                .mapToDouble(StatusEffect::getValue)
                .sum();
    }

    /**
     * Get vulnerability multiplier (extra damage taken).
     */
    public double getVulnerabilityMultiplier(String entityId) {
        double vuln = getEffects(entityId).stream()
                .filter(e -> e.getType() == StatusEffect.Type.VULNERABILITY)
                .mapToDouble(StatusEffect::getValue)
                .sum();
        return 1.0 + vuln;
    }
}
