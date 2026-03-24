package com.motm.model;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.List;
import java.util.Locale;

public class Perk {

    private String id;
    private String name;
    private int tier;
    private String description;
    private List<Effect> effects;
    @SerializedName("synergy_tags")
    private List<String> synergyTags;
    @SerializedName("synergy_bonuses")
    private List<SynergyBonus> synergyBonuses;
    @SerializedName("enhanced_by")
    private List<String> enhancedBy;
    private List<String> enhances;

    public String getId() { return id; }
    public String getName() { return name; }
    public int getTier() { return tier; }
    public String getDescription() { return description; }
    public List<Effect> getEffects() { return effects; }
    public List<String> getSynergyTags() { return synergyTags; }
    public List<SynergyBonus> getSynergyBonuses() { return synergyBonuses; }
    public List<String> getEnhancedBy() { return enhancedBy; }
    public List<String> getEnhances() { return enhances; }

    public static class Effect {
        private String type;
        private String element;
        private String stat;
        private String name;
        private String creature;
        @SerializedName("effect_type")
        private String effectType;
        private String ability;
        private JsonElement value;
        private JsonElement duration;
        private String condition;

        public String getType() { return type; }
        public String getElement() { return element; }
        public String getStat() { return stat; }
        public String getName() { return name; }
        public String getCreature() { return creature; }
        public String getEffectType() { return effectType; }
        public String getAbility() { return ability; }
        public double getValue() { return parseNumberish(value); }
        public double getDuration() { return parseNumberish(duration); }
        public String getValueRaw() { return toRawString(value); }
        public String getDurationRaw() { return toRawString(duration); }
        public String getCondition() { return condition; }

        private double parseNumberish(JsonElement element) {
            if (element == null || element.isJsonNull()) {
                return 0;
            }

            if (!element.isJsonPrimitive()) {
                return 0;
            }

            var primitive = element.getAsJsonPrimitive();
            if (primitive.isNumber()) {
                return primitive.getAsDouble();
            }

            if (primitive.isBoolean()) {
                return primitive.getAsBoolean() ? 1.0 : 0.0;
            }

            if (!primitive.isString()) {
                return 0;
            }

            String raw = primitive.getAsString().trim();
            if (raw.isEmpty()) {
                return 0;
            }

            String normalized = raw.toLowerCase(Locale.ROOT);
            if (normalized.equals("infinite") || normalized.equals("infinity") || normalized.equals("unlimited")) {
                return Double.POSITIVE_INFINITY;
            }

            try {
                return Double.parseDouble(raw);
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        private String toRawString(JsonElement element) {
            if (element == null || element.isJsonNull()) {
                return null;
            }

            if (element.isJsonPrimitive()) {
                return element.getAsJsonPrimitive().getAsString();
            }

            return element.toString();
        }
    }

    public static class SynergyBonus {
        @SerializedName("requires_tags")
        private List<String> requiresTags;
        private Effect bonus;

        public List<String> getRequiresTags() { return requiresTags; }
        public Effect getBonus() { return bonus; }
    }
}
