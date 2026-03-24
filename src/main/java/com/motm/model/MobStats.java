package com.motm.model;

import java.util.List;

public class MobStats {

    private double health;
    private double damage;
    private double armor;
    private double magicResist;
    private double speed;
    private double attackSpeed;
    private double aggroRange;
    private double attackRange;
    private String behavior;
    private List<String> abilities;
    private List<String> drops;
    private double xpReward;
    private String eliteTitle;
    private boolean elite;

    public MobStats() {}

    public MobStats(MobStats other) {
        this.health = other.health;
        this.damage = other.damage;
        this.armor = other.armor;
        this.magicResist = other.magicResist;
        this.speed = other.speed;
        this.attackSpeed = other.attackSpeed;
        this.aggroRange = other.aggroRange;
        this.attackRange = other.attackRange;
        this.behavior = other.behavior;
        this.abilities = other.abilities;
        this.drops = other.drops;
        this.xpReward = other.xpReward;
        this.eliteTitle = other.eliteTitle;
        this.elite = other.elite;
    }

    public double getHealth() { return health; }
    public void setHealth(double health) { this.health = health; }
    public double getDamage() { return damage; }
    public void setDamage(double damage) { this.damage = damage; }
    public double getArmor() { return armor; }
    public void setArmor(double armor) { this.armor = armor; }
    public double getMagicResist() { return magicResist; }
    public void setMagicResist(double magicResist) { this.magicResist = magicResist; }
    public double getSpeed() { return speed; }
    public void setSpeed(double speed) { this.speed = speed; }
    public double getAttackSpeed() { return attackSpeed; }
    public void setAttackSpeed(double attackSpeed) { this.attackSpeed = attackSpeed; }
    public double getAggroRange() { return aggroRange; }
    public void setAggroRange(double aggroRange) { this.aggroRange = aggroRange; }
    public double getAttackRange() { return attackRange; }
    public void setAttackRange(double attackRange) { this.attackRange = attackRange; }
    public String getBehavior() { return behavior; }
    public void setBehavior(String behavior) { this.behavior = behavior; }
    public List<String> getAbilities() { return abilities; }
    public void setAbilities(List<String> abilities) { this.abilities = abilities; }
    public List<String> getDrops() { return drops; }
    public void setDrops(List<String> drops) { this.drops = drops; }
    public double getXpReward() { return xpReward; }
    public void setXpReward(double xpReward) { this.xpReward = xpReward; }
    public String getEliteTitle() { return eliteTitle; }
    public void setEliteTitle(String eliteTitle) { this.eliteTitle = eliteTitle; }
    public boolean isElite() { return elite; }
    public void setElite(boolean elite) { this.elite = elite; }
}
