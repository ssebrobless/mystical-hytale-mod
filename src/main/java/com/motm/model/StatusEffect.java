package com.motm.model;

/**
 * Represents an active status effect on an entity.
 * Status effects tick each server tick and expire after their duration.
 */
public class StatusEffect {

    public enum Type {
        BURN,           // 3% max HP per tick, 3 ticks
        DOT,            // 5% max HP per tick, 3 ticks (poison/generic)
        STUN,           // Cannot act, 1 tick
        SLOW,           // Reduced movement speed, 2 ticks
        SLOW_STACK,     // Stacking slow (multiplies), variable
        KNOCKBACK,      // Physics push, instant
        VULNERABILITY,  // +25% damage taken, 3 ticks
        FREEZE,         // Cannot move or act, 2 ticks
        ROOT,           // Cannot move, can still act, 2 ticks
        BLIND,          // Reduced accuracy/vision, 2 ticks
        DISORIENTED,    // Reduced accuracy, attack slow, 2 ticks
        GROUNDED,       // Cannot use mobility abilities, 2 ticks
        FLYING,         // Immune to grounded, mobility bonus, 5 ticks
        SHOCKED,        // Lightning mark — bonus damage from lightning, 3 ticks
        SHIELD,         // Absorbs damage, variable duration
        EVASION,        // +50% dodge chance, 2 ticks
        DEFENSE_BUFF,   // +20% damage reduction, 3 ticks
        ATTACK_BUFF,    // +20% damage dealt, 3 ticks
        DAMAGE_BUFF,    // +35% next attack only, until consumed
        STEALTH,        // Invisible, +40% damage next hit, until consumed
        HEAL_OVER_TIME, // Regen % HP per tick
        LIFESTEAL,      // Heal for % of damage dealt
        SPEED_BUFF      // Faster movement / follow-up momentum
    }

    private final Type type;
    private final String sourcePlayerId;
    private final String sourcePerkOrAbility;
    private final int initialDurationTicks;
    private int remainingTicks;
    private double value;        // damage %, reduction %, shield HP, etc.
    private boolean consumed;    // for one-shot effects like DAMAGE_BUFF, STEALTH

    public StatusEffect(Type type, int durationTicks, double value,
                        String sourcePlayerId, String sourcePerkOrAbility) {
        this.type = type;
        this.initialDurationTicks = Math.max(0, durationTicks);
        this.remainingTicks = durationTicks;
        this.value = value;
        this.sourcePlayerId = sourcePlayerId;
        this.sourcePerkOrAbility = sourcePerkOrAbility;
        this.consumed = false;
    }

    public Type getType() { return type; }
    public String getSourcePlayerId() { return sourcePlayerId; }
    public String getSourcePerkOrAbility() { return sourcePerkOrAbility; }
    public int getInitialDurationTicks() { return initialDurationTicks; }
    public int getRemainingTicks() { return remainingTicks; }
    public double getValue() { return value; }
    public boolean isConsumed() { return consumed; }
    public void consume() { this.consumed = true; }

    public boolean isExpired() {
        return remainingTicks <= 0 || consumed;
    }

    public void tick() {
        if (remainingTicks > 0) {
            remainingTicks--;
        }
    }

    public void setValue(double value) {
        this.value = value;
    }

    /**
     * Whether this effect prevents the entity from acting.
     */
    public boolean preventsAction() {
        return type == Type.STUN || type == Type.FREEZE;
    }

    /**
     * Whether this effect prevents movement.
     */
    public boolean preventsMovement() {
        return type == Type.STUN || type == Type.FREEZE || type == Type.ROOT;
    }

    /**
     * Whether this is a damage-over-time effect that ticks.
     */
    public boolean isDamageOverTime() {
        return type == Type.BURN || type == Type.DOT;
    }

    /**
     * Get the damage per tick as a fraction of max HP.
     */
    public double getDamagePerTickPercent() {
        return switch (type) {
            case BURN -> 0.03;  // 3% max HP
            case DOT -> 0.05;   // 5% max HP
            default -> 0;
        };
    }
}
