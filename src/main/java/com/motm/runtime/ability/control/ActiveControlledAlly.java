package com.motm.runtime.ability.control;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;

public final class ActiveControlledAlly {
    private final String ownerPlayerId;
    private final Ref<EntityStore> ownerRef;
    private final Ref<EntityStore> ref;
    private final AbilityData ability;
    private long expireAtMillis;
    private long nextThinkAtMillis;
    private long nextAttackAtMillis;
    private Ref<EntityStore> currentTargetRef;
    private long targetLockExpireAtMillis;

    public ActiveControlledAlly(String ownerPlayerId,
                                Ref<EntityStore> ownerRef,
                                Ref<EntityStore> ref,
                                AbilityData ability,
                                long expireAtMillis,
                                long nextThinkAtMillis,
                                long nextAttackAtMillis) {
        this.ownerPlayerId = ownerPlayerId;
        this.ownerRef = ownerRef;
        this.ref = ref;
        this.ability = ability;
        this.expireAtMillis = expireAtMillis;
        this.nextThinkAtMillis = nextThinkAtMillis;
        this.nextAttackAtMillis = nextAttackAtMillis;
    }

    public String ownerPlayerId() { return ownerPlayerId; }
    public Ref<EntityStore> ownerRef() { return ownerRef; }
    public Ref<EntityStore> ref() { return ref; }
    public AbilityData ability() { return ability; }
    public long expireAtMillis() { return expireAtMillis; }
    public long nextThinkAtMillis() { return nextThinkAtMillis; }
    public long nextAttackAtMillis() { return nextAttackAtMillis; }
    public Ref<EntityStore> currentTargetRef() { return currentTargetRef; }
    public long targetLockExpireAtMillis() { return targetLockExpireAtMillis; }

    public void extendUntil(long newExpireAtMillis) {
        expireAtMillis = Math.max(expireAtMillis, newExpireAtMillis);
    }

    public void scheduleNextThink(long now, long intervalMillis) {
        nextThinkAtMillis = now + Math.max(150L, intervalMillis);
    }

    public void scheduleNextAttack(long now, long intervalMillis) {
        nextAttackAtMillis = now + Math.max(450L, intervalMillis);
    }

    public void clearTargetLock() {
        currentTargetRef = null;
        targetLockExpireAtMillis = 0L;
    }

    public void setTargetLock(Ref<EntityStore> targetRef, long now) {
        currentTargetRef = targetRef;
        targetLockExpireAtMillis = targetRef == null ? 0L : now + 1400L;
    }
}
