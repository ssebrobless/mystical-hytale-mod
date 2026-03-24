package com.motm.model;

import com.google.gson.annotations.SerializedName;
import java.util.Collections;
import java.util.Map;

/**
 * Represents a playable race with stat bonuses and special mechanics.
 * 12 races total.
 */
public class RaceData {

    private String id;
    private String name;
    private String description;
    @SerializedName("hp_bonus")
    private int hpBonus;
    @SerializedName("ability_bonuses")
    private Map<String, Integer> abilityBonuses;
    private Map<String, Object> bonuses;
    private String passive;
    private String special;

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public int getHpBonus() { return hpBonus; }
    public Map<String, Integer> getAbilityBonuses() {
        return abilityBonuses != null ? abilityBonuses : Collections.emptyMap();
    }
    public Map<String, Object> getBonuses() {
        return bonuses != null ? bonuses : Collections.emptyMap();
    }
    public String getPassive() { return passive; }
    public String getSpecial() { return special; }

    // Typed accessors for common bonuses
    public double getDamageReductionPercent() {
        return getPercentBonus("damage_reduction");
    }

    public double getEvasionPercent() {
        return getPercentBonus("evasion");
    }

    public double getDamagePercent() {
        return getPercentBonus("damage");
    }

    public double getCritChancePercent() {
        return getPercentBonus("crit_chance");
    }

    public double getHealingPercent() {
        return getPercentBonus("healing");
    }

    public int getCooldownReduction() {
        Object val = getBonuses().get("cooldown_reduction");
        return val instanceof Number ? ((Number) val).intValue() : 0;
    }

    public boolean isPoisonImmune() {
        return hasFlagBonus("poison_immune");
    }

    public boolean hasBreathWeapon() {
        return hasFlagBonus("breath_weapon");
    }

    public double getPercentBonus(String key) {
        Object val = getBonuses().get(key);
        return val instanceof Number ? ((Number) val).doubleValue() / 100.0 : 0;
    }

    public boolean hasFlagBonus(String key) {
        return Boolean.TRUE.equals(getBonuses().get(key));
    }
}
