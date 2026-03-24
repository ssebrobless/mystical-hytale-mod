package com.motm.model;

/**
 * Represents an elemental mark applied to a target entity.
 * When two different marks are present, an elemental reaction triggers.
 */
public class ElementalMark {

    public enum MarkType {
        WET,         // Applied by Hydro water abilities
        COLD,        // Applied by Hydro frost abilities (Icicle, Snow, Iceberg)
        SHOCKED,     // Applied by lightning abilities (Aero Thunder)
        COMBUSTIBLE, // Applied by burn effects (except Scald, Geyser, Overheat)
        TARRED,      // Applied by smoke/toxic abilities (Smoke Bomb, Smog, Toxic Breath)
        CURSED,      // Applied by Corruptus dark abilities (Shadow, Void, Hell Flame)
        GROUNDED,    // Exception mark: knockdown/root, blocks flying
        FLYING       // Exception mark: mobility, blocks grounded
    }

    private final MarkType type;
    private final String appliedByPlayerId;
    private final String appliedByAbility;
    private final String appliedByElement; // terra, hydro, aero, corruptus
    private int remainingTicks;
    private int cooldownTicks; // Cannot reapply same mark type during cooldown

    public static final int DEFAULT_MARK_DURATION = 3;  // ticks
    public static final int GROUNDED_DURATION = 2;
    public static final int FLYING_DURATION = 5;
    public static final int MARK_COOLDOWN = 2;

    public ElementalMark(MarkType type, String appliedByPlayerId,
                         String appliedByAbility, String appliedByElement) {
        this.type = type;
        this.appliedByPlayerId = appliedByPlayerId;
        this.appliedByAbility = appliedByAbility;
        this.appliedByElement = appliedByElement;
        this.cooldownTicks = 0;

        this.remainingTicks = switch (type) {
            case GROUNDED -> GROUNDED_DURATION;
            case FLYING -> FLYING_DURATION;
            default -> DEFAULT_MARK_DURATION;
        };
    }

    public MarkType getType() { return type; }
    public String getAppliedByPlayerId() { return appliedByPlayerId; }
    public String getAppliedByAbility() { return appliedByAbility; }
    public String getAppliedByElement() { return appliedByElement; }
    public int getRemainingTicks() { return remainingTicks; }
    public int getCooldownTicks() { return cooldownTicks; }

    public boolean isExpired() { return remainingTicks <= 0; }
    public boolean isOnCooldown() { return cooldownTicks > 0; }

    /**
     * Whether this mark is consumed when hit (synergy marks are, exception marks are not).
     */
    public boolean isConsumedOnHit() {
        return type != MarkType.FLYING; // Flying is never consumed
    }

    /**
     * Whether this is an exception mark (grounded/flying) with special rules.
     */
    public boolean isExceptionMark() {
        return type == MarkType.GROUNDED || type == MarkType.FLYING;
    }

    public void tick() {
        if (remainingTicks > 0) remainingTicks--;
        if (cooldownTicks > 0) cooldownTicks--;
    }

    /**
     * Consume this mark and start cooldown.
     */
    public void consume() {
        remainingTicks = 0;
        cooldownTicks = MARK_COOLDOWN;
    }
}
