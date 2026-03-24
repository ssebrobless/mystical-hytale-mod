package com.motm.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Represents an active ability granted by a style.
 * Abilities have cooldowns, resource costs, damage, effects, and
 * richer spatial metadata for Hytale's 3D combat runtime.
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
    @SerializedName("cast_type")
    private String castType;
    @SerializedName("target_type")
    private String targetType;
    private double range;
    @SerializedName("max_range")
    private double maxRange;
    private double radius;
    private double width;
    private double length;
    private double height;
    @SerializedName("cone_angle")
    private double coneAngle;
    @SerializedName("duration_seconds")
    private double durationSeconds;
    @SerializedName("delay_seconds")
    private double delaySeconds;
    @SerializedName("projectile_speed")
    private double projectileSpeed;
    @SerializedName("dash_distance")
    private double dashDistance;
    @SerializedName("launch_height")
    private double launchHeight;
    @SerializedName("knockback_force")
    private double knockbackForce;
    @SerializedName("pull_force")
    private double pullForce;
    private boolean knockup;
    @SerializedName("travel_type")
    private String travelType;
    @SerializedName("terrain_effect")
    private String terrainEffect;
    @SerializedName("summon_name")
    private String summonName;

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
    public String getCastType() { return castType; }
    public String getTargetType() { return targetType; }
    public double getRange() { return range; }
    public double getMaxRange() { return maxRange; }
    public double getRadius() { return radius; }
    public double getWidth() { return width; }
    public double getLength() { return length; }
    public double getHeight() { return height; }
    public double getConeAngle() { return coneAngle; }
    public double getDurationSeconds() { return durationSeconds; }
    public double getDelaySeconds() { return delaySeconds; }
    public double getProjectileSpeed() { return projectileSpeed; }
    public double getDashDistance() { return dashDistance; }
    public double getLaunchHeight() { return launchHeight; }
    public double getKnockbackForce() { return knockbackForce; }
    public double getPullForce() { return pullForce; }
    public boolean isKnockup() { return knockup; }
    public String getTravelType() { return travelType; }
    public String getTerrainEffect() { return terrainEffect; }
    public String getSummonName() { return summonName; }
}
