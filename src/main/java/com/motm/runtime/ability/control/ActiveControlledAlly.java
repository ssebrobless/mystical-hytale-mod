package com.motm.runtime.ability.control;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;

/**
 * Runtime state for a single hostile NPC that a Corruptus mentokinesis ability
 * (dominate / hivemind) has converted into a temporary controlled ally.
 *
 * <p>Control is modelled MOTM-side rather than by swapping the NPC's persisted
 * role, because runtime role swaps on persisted NPCs caused SEVERE
 * {@code RoleChangeSystem} failures (see completion plan Sec. 3 / defect #1). The
 * adapter flips the NPC's live attitude to friendly and applies a pink marker for
 * the control window; this state object owns the ownership binding, the
 * release clock, and the target/attack scheduling that the tick runtime drives.
 */
public final class ActiveControlledAlly {
    /** Cadence at which a controlled ally re-evaluates its target/movement. */
    public static final long THINK_INTERVAL_MS = 250L;
    /** How long a resolved target stays locked before re-acquisition. */
    public static final long TARGET_LOCK_MS = 1500L;

    private final String ownerPlayerId;
    private final Ref<EntityStore> controlledRef;
    private final Ref<EntityStore> ownerRef;
    private final String controlledEntityId;
    private final String classId;
    private final String styleId;
    private final AbilityData ability;
    private final double attackRange;
    private final double chaseRange;
    private final long attackIntervalMillis;
    private final String attackToken;

    private long expireAtMillis;
    private long nextThinkAtMillis;
    private long nextAttackAtMillis;
    private Ref<EntityStore> currentTargetRef;
    private long targetLockExpireAtMillis;
    private boolean markerApplied;

    public ActiveControlledAlly(String ownerPlayerId,
                                Ref<EntityStore> controlledRef,
                                Ref<EntityStore> ownerRef,
                                String controlledEntityId,
                                String classId,
                                String styleId,
                                AbilityData ability,
                                double attackRange,
                                double chaseRange,
                                long attackIntervalMillis,
                                String attackToken,
                                long expireAtMillis,
                                long nextThinkAtMillis,
                                long nextAttackAtMillis,
                                Ref<EntityStore> currentTargetRef,
                                long targetLockExpireAtMillis,
                                boolean markerApplied) {
        this.ownerPlayerId = ownerPlayerId;
        this.controlledRef = controlledRef;
        this.ownerRef = ownerRef;
        this.controlledEntityId = controlledEntityId;
        this.classId = classId;
        this.styleId = styleId;
        this.ability = ability;
        this.attackRange = attackRange;
        this.chaseRange = chaseRange;
        this.attackIntervalMillis = attackIntervalMillis;
        this.attackToken = attackToken;
        this.expireAtMillis = expireAtMillis;
        this.nextThinkAtMillis = nextThinkAtMillis;
        this.nextAttackAtMillis = nextAttackAtMillis;
        this.currentTargetRef = currentTargetRef;
        this.targetLockExpireAtMillis = targetLockExpireAtMillis;
        this.markerApplied = markerApplied;
    }

    public String ownerPlayerId() { return ownerPlayerId; }
    public Ref<EntityStore> controlledRef() { return controlledRef; }
    public Ref<EntityStore> ownerRef() { return ownerRef; }
    public String controlledEntityId() { return controlledEntityId; }
    public String classId() { return classId; }
    public String styleId() { return styleId; }
    public AbilityData ability() { return ability; }
    public double attackRange() { return attackRange; }
    public double chaseRange() { return chaseRange; }
    public long attackIntervalMillis() { return attackIntervalMillis; }
    public String attackToken() { return attackToken; }
    public long expireAtMillis() { return expireAtMillis; }
    public long nextThinkAtMillis() { return nextThinkAtMillis; }
    public long nextAttackAtMillis() { return nextAttackAtMillis; }
    public Ref<EntityStore> currentTargetRef() { return currentTargetRef; }
    public long targetLockExpireAtMillis() { return targetLockExpireAtMillis; }
    public boolean markerApplied() { return markerApplied; }

    public boolean isExpired(long now) {
        return now >= expireAtMillis;
    }

    /** Recast on an already-controlled ally refreshes (never shortens) the release clock. */
    public void refreshControlUntil(long expireAtMillis) {
        this.expireAtMillis = Math.max(this.expireAtMillis, expireAtMillis);
    }

    public void scheduleNextThink(long now) {
        this.nextThinkAtMillis = now + THINK_INTERVAL_MS;
    }

    public void scheduleNextAttack(long now) {
        this.nextAttackAtMillis = now + Math.max(1L, attackIntervalMillis);
    }

    public boolean targetLockExpired(long now) {
        return now >= targetLockExpireAtMillis;
    }

    public void setTargetLock(Ref<EntityStore> targetRef, long now) {
        this.currentTargetRef = targetRef;
        this.targetLockExpireAtMillis = now + TARGET_LOCK_MS;
    }

    public void clearTargetLock() {
        this.currentTargetRef = null;
        this.targetLockExpireAtMillis = 0L;
    }

    public void markMarkerApplied() {
        this.markerApplied = true;
    }
}
