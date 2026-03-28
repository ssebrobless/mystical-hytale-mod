package com.motm.util;

import com.motm.model.ClassData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared formatting helpers for class passive abilities.
 */
public final class PassivePresentation {

    private PassivePresentation() {
    }

    public static String buildPassiveSummary(ClassData.PassiveAbility passive) {
        if (passive == null || passive.getEffects() == null || passive.getEffects().isEmpty()) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        for (ClassData.PassiveEffect effect : passive.getEffects()) {
            String summary = summarizeEffect(effect);
            if (!summary.isBlank()) {
                parts.add(summary);
            }
        }
        return String.join(" | ", parts);
    }

    private static String summarizeEffect(ClassData.PassiveEffect effect) {
        if (effect == null || effect.getType() == null || effect.getType().isBlank()) {
            return "";
        }

        return switch (effect.getType()) {
            case "spell_vamp" -> "Ability damage heals " + percent(effect.getValue())
                    + " of damage dealt";
            case "swim_speed_bonus" -> "Swim " + signedPercent(effect.getValue()) + " faster";
            case "oxygen_capacity_bonus" -> "Breathe underwater " + percent(effect.getValue()) + " longer";
            case "movement_speed_bonus" -> "Movement speed " + signedPercent(effect.getValue());
            case "signature_energy_bonus" -> "Native Hytale energy +" + percent(effect.getValue());
            case "conditional_cost_reduction" -> humanizeCondition(effect.getCondition())
                    + ": ability costs " + signedPercent(-effect.getValue());
            case "conditional_damage_modifier" -> humanizeCondition(effect.getCondition())
                    + ": ability damage " + signedPercent(effect.getValue());
            case "conditional_shield" -> humanizeCondition(effect.getCondition())
                    + ": gain shield for " + percent(effect.getValue()) + " max HP";
            case "conditional_regen" -> humanizeCondition(effect.getCondition())
                    + ": regen " + percent(effect.getValue()) + " max HP/s";
            case "mining_speed_bonus" -> "Pickaxe mining speed " + signedPercent(effect.getValue());
            case "conditional_night_vision" -> humanizeCondition(effect.getCondition()) + ": gain night vision";
            case "resource_generation" -> summarizeResourceGeneration(effect);
            case "resource_cast_pool" -> humanize(effect.getResource()) + " fuel Corruptus abilities";
            case "conditional_bonus_damage" -> humanizeCondition(effect.getCondition())
                    + ": next attack deals +" + percent(effect.getValue()) + " attack damage as "
                    + humanize(effect.getDamageType())
                    + (effect.isConsumesResource() ? " and consumes the charge" : "");
            case "conditional_buff" -> humanizeCondition(effect.getCondition()) + ": "
                    + signedPercent(effect.getValue()) + " " + humanize(effect.getBuff())
                    + (effect.getDuration() > 0 ? " for " + AbilityPresentation.formatDecimal(effect.getDuration()) + "s" : "");
            case "on_damage_debuff" -> "Dealing damage applies " + humanize(effect.getDebuff())
                    + durationSuffix(effect.getDuration())
                    + stackSuffix(effect.getMaxStacks());
            case "debuff_damage_amplification" -> humanize(effect.getDebuff())
                    + " takes +" + percent(effect.getValue()) + " damage per stack";
            case "on_kill_restore" -> humanizeCondition(effect.getCondition()) + ": restore "
                    + percent(effect.getRestoreHealth()) + " HP and "
                    + percent(effect.getRestoreMana()) + " mana on kill";
            default -> fallbackSummary(effect);
        };
    }

    private static String summarizeResourceGeneration(ClassData.PassiveEffect effect) {
        String resourceName = humanize(effect.getResource());
        String amount = AbilityPresentation.formatDecimal(effect.getValue());
        String maxSuffix = effect.getMax() > 0
                ? " (max " + AbilityPresentation.formatDecimal(effect.getMax()) + ")"
                : "";

        return switch (normalize(effect.getTrigger())) {
            case "movement" -> "While moving: gain " + amount + " " + resourceName + "/s" + maxSuffix;
            case "mob_kill", "kill", "enemy_kill" -> "On hostile kill: gain " + amount + " " + resourceName + maxSuffix;
            default -> "On " + humanizeTrigger(effect.getTrigger()) + ": gain " + amount + " " + resourceName + maxSuffix;
        };
    }

    private static String fallbackSummary(ClassData.PassiveEffect effect) {
        StringBuilder builder = new StringBuilder(humanize(effect.getType()));
        if (effect.getCondition() != null && !effect.getCondition().isBlank()) {
            builder.append(" when ").append(humanizeCondition(effect.getCondition()));
        }
        if (effect.getValue() != 0) {
            builder.append(" ").append(signedPercent(effect.getValue()));
        }
        if (effect.getDuration() > 0) {
            builder.append(" for ").append(AbilityPresentation.formatDecimal(effect.getDuration())).append("s");
        }
        return builder.toString();
    }

    private static String humanizeCondition(String condition) {
        if (condition == null || condition.isBlank()) {
            return "Conditionally";
        }

        return switch (condition.trim()) {
            case "current_mana_percent < 0.50" -> "Below 50% mana";
            case "current_water_percent < 0.50" -> "Below 50% water";
            case "current_health_percent < 0.30" -> "Below 30% HP";
            case "stationary_duration >= 2" -> "After standing still 2s";
            case "underground_cave" -> "Underground in caves";
            case "storm_charge >= 100" -> "At 100 Storm Charge";
            case "storm_charge_consumed" -> "When Storm Charge is consumed";
            case "target_has_corruption_mark" -> "If the target has Corruption";
            default -> humanize(condition);
        };
    }

    private static String humanizeTrigger(String trigger) {
        if (trigger == null || trigger.isBlank()) {
            return "triggered";
        }
        return switch (trigger.trim()) {
            case "movement" -> "moving";
            case "mob_kill", "kill", "enemy_kill" -> "hostile kill";
            default -> humanize(trigger);
        };
    }

    private static String durationSuffix(double duration) {
        if (duration <= 0) {
            return "";
        }
        return " for " + AbilityPresentation.formatDecimal(duration) + "s";
    }

    private static String stackSuffix(int stacks) {
        if (stacks <= 0) {
            return "";
        }
        return " (max " + stacks + " stacks)";
    }

    private static String percent(double value) {
        return AbilityPresentation.formatDecimal(value * 100) + "%";
    }

    private static String signedPercent(double value) {
        String prefix = value > 0 ? "+" : "";
        return prefix + AbilityPresentation.formatDecimal(value * 100) + "%";
    }

    private static String humanize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }

        String normalized = rawValue
                .replace(">=", " ")
                .replace("<=", " ")
                .replace(">", " ")
                .replace("<", " ")
                .replace('-', '_');
        String[] parts = normalized.split("_+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return builder.toString();
    }

    private static String normalize(String rawValue) {
        return rawValue == null ? "" : rawValue.trim().toLowerCase(Locale.ROOT);
    }
}
