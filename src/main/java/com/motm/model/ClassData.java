package com.motm.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

public class ClassData {

    private String id;
    private String name;
    @SerializedName("display_name")
    private String displayName;
    private String theme;
    private String element;
    private String description;
    private String lore;
    @SerializedName("color_primary")
    private String colorPrimary;
    @SerializedName("color_secondary")
    private String colorSecondary;
    @SerializedName("color_accent")
    private String colorAccent;
    private String icon;
    @SerializedName("starting_stats")
    private Map<String, Double> startingStats;
    @SerializedName("stat_growth_per_level")
    private Map<String, Double> statGrowthPerLevel;
    @SerializedName("passive_ability")
    private PassiveAbility passiveAbility;
    @SerializedName("innate_resistances")
    private Map<String, Double> innateResistances;
    @SerializedName("playstyle_tags")
    private List<String> playstyleTags;
    @SerializedName("recommended_for")
    private String recommendedFor;
    private String difficulty;
    @SerializedName("synergy_elements")
    private List<String> synergyElements;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public String getTheme() { return theme; }
    public String getElement() { return element; }
    public String getDescription() { return description; }
    public String getLore() { return lore; }
    public String getIcon() { return icon; }
    public Map<String, Double> getStartingStats() { return startingStats; }
    public Map<String, Double> getStatGrowthPerLevel() { return statGrowthPerLevel; }
    public PassiveAbility getPassiveAbility() { return passiveAbility; }
    public Map<String, Double> getInnateResistances() { return innateResistances; }
    public List<String> getPlaystyleTags() { return playstyleTags; }
    public String getDifficulty() { return difficulty; }
    public List<String> getSynergyElements() { return synergyElements; }

    public static class PassiveAbility {
        private String id;
        private String name;
        private String description;
        private List<PassiveEffect> effects;

        public String getId() { return id; }
        public String getName() { return name; }
        public String getDescription() { return description; }
        public List<PassiveEffect> getEffects() { return effects; }
    }

    public static class PassiveEffect {
        private String type;
        private String condition;
        private double value;
        @SerializedName("value_type")
        private String valueType;
        private String resource;
        private String trigger;
        private double max;
        private double duration;
        private String buff;
        @SerializedName("damage_type")
        private String damageType;
        private String debuff;
        @SerializedName("max_stacks")
        private int maxStacks;
        @SerializedName("restore_health")
        private double restoreHealth;
        @SerializedName("restore_mana")
        private double restoreMana;
        @SerializedName("consumes_resource")
        private boolean consumesResource;

        public String getType() { return type; }
        public String getCondition() { return condition; }
        public double getValue() { return value; }
        public String getValueType() { return valueType; }
        public String getResource() { return resource; }
        public String getTrigger() { return trigger; }
        public double getMax() { return max; }
        public double getDuration() { return duration; }
        public String getBuff() { return buff; }
        public String getDamageType() { return damageType; }
        public String getDebuff() { return debuff; }
        public int getMaxStacks() { return maxStacks; }
        public double getRestoreHealth() { return restoreHealth; }
        public double getRestoreMana() { return restoreMana; }
        public boolean isConsumesResource() { return consumesResource; }
    }
}
