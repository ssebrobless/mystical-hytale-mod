package com.motm.util;

import com.motm.model.AbilityData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared formatting helpers for ability summaries in chat and the spellbook UI.
 */
public final class AbilityPresentation {

    private AbilityPresentation() {
    }

    public static String buildEffectSummary(AbilityData ability) {
        if (ability == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        appendPercent(parts, ability.getDamagePercent(), "% dmg");
        appendPercent(parts, ability.getHealPercent(), "% heal");
        appendPercent(parts, ability.getShieldPercent(), "% shield");

        if (ability.getEffect() != null && !ability.getEffect().isBlank()) {
            parts.add("Effect: " + humanize(ability.getEffect()));
        }

        return parts.isEmpty() ? "Utility / special ability" : String.join(" | ", parts);
    }

    public static String buildSpatialSummary(AbilityData ability) {
        if (ability == null) {
            return "";
        }

        List<String> parts = new ArrayList<>();
        String castType = humanize(ability.getCastType());
        String targetType = humanize(ability.getTargetType());

        appendText(parts, castType);
        if (!targetType.isBlank() && !targetType.equalsIgnoreCase(castType)) {
            appendText(parts, targetType);
        }

        appendMetric(parts, ability.getRange(), "Range ", "");
        appendMetric(parts, ability.getMaxRange(), "Max ", "");
        appendMetric(parts, ability.getRadius(), "Radius ", "");
        appendMetric(parts, ability.getWidth(), "Width ", "");
        appendMetric(parts, ability.getLength(), "Length ", "");
        appendMetric(parts, ability.getHeight(), "Height ", "");
        appendMetric(parts, ability.getConeAngle(), "Cone ", "deg");
        appendMetric(parts, ability.getDashDistance(), "Dash ", "");
        appendMetric(parts, ability.getLaunchHeight(), "Launch ", "");
        appendMetric(parts, ability.getProjectileSpeed(), "Speed ", "");
        appendMetric(parts, ability.getDurationSeconds(), "Duration ", "s");
        appendMetric(parts, ability.getDelaySeconds(), "Delay ", "s");
        appendMetric(parts, ability.getKnockbackForce(), "Knockback ", "");
        appendMetric(parts, ability.getPullForce(), "Pull ", "");

        if (ability.isKnockup()) {
            parts.add("Knockup");
        }

        if (ability.getTravelType() != null && !ability.getTravelType().isBlank()) {
            parts.add("Travel " + humanize(ability.getTravelType()));
        }

        if (ability.getTerrainEffect() != null && !ability.getTerrainEffect().isBlank()) {
            parts.add("Terrain " + humanize(ability.getTerrainEffect()));
        }

        if (ability.getSummonName() != null && !ability.getSummonName().isBlank()) {
            parts.add("Summons " + humanize(ability.getSummonName()));
        }

        return String.join(" | ", parts);
    }

    public static String buildVisualSummary(String classId, String styleId, AbilityData ability) {
        return HytaleAssetResolver.buildCompactSummary(classId, styleId, ability);
    }

    public static String buildVisualDetail(String classId, String styleId, AbilityData ability) {
        return HytaleAssetResolver.buildDetailedSummary(classId, styleId, ability);
    }

    public static String compactText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    public static String formatDecimal(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }

    private static void appendPercent(List<String> parts, double value, String suffix) {
        if (value > 0) {
            parts.add(formatDecimal(value) + suffix);
        }
    }

    private static void appendMetric(List<String> parts, double value, String label, String suffix) {
        if (value > 0) {
            parts.add(label + formatDecimal(value) + suffix);
        }
    }

    private static void appendText(List<String> parts, String text) {
        if (text != null && !text.isBlank()) {
            parts.add(text);
        }
    }

    private static String humanize(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return "";
        }

        String[] parts = rawValue.trim().replace('-', '_').split("_+");
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
}
