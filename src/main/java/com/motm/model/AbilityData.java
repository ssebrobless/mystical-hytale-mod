package com.motm.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents an active ability granted by a style.
 * Abilities have cooldowns, resource costs, damage, and effects.
 */
public class AbilityData {

    private String id;
    private String name;
    @SerializedName("damage_percent")
    private double damagePercent;
    @SerializedName("cooldown_seconds")
    private double cooldownSeconds;
    private String effect;
    private List<String> categories;
    private int charges; // 0 = unlimited (cooldown-based), >0 = charge-based
    @SerializedName("heal_percent")
    private double healPercent;
    @SerializedName("shield_percent")
    private double shieldPercent;
    @SerializedName("resource_cost")
    private int resourceCost;
    private String description;

    public String getId() { return id; }
    public String getName() { return name; }
    public double getDamagePercent() { return damagePercent; }
    public double getCooldownSeconds() { return cooldownSeconds; }
    public String getEffect() { return effect; }
    public List<String> getCategories() { return categories; }
    public int getCharges() { return charges; }
    public double getHealPercent() { return healPercent; }
    public double getShieldPercent() { return shieldPercent; }
    public int getResourceCost() { return resourceCost; }
    public String getDescription() { return description; }
}
