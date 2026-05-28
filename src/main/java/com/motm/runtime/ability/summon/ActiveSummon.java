package com.motm.runtime.ability.summon;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;

public final class ActiveSummon {
    private final String ownerPlayerId;
    private final Ref<EntityStore> ownerRef;
    private final String classId;
    private final String styleId;
    private final AbilityData ability;
    private final SummonRuntimeSpec spec;
    private final long hatchAtMillis;
    private final Ref<EntityStore> ref;
    private long nextThinkAtMillis;
    private long nextAttackAtMillis;
    private long buffExpireAtMillis;
    private long expireAtMillis;
    private final double baseDamage;
    private Ref<EntityStore> currentTargetRef;
    private long targetLockExpireAtMillis;
    private boolean awakened;

    public ActiveSummon(String ownerPlayerId,
                        Ref<EntityStore> ref,
                        Ref<EntityStore> ownerRef,
                        String classId,
                        String styleId,
                        AbilityData ability,
                        SummonRuntimeSpec spec,
                        long hatchAtMillis,
                        long expireAtMillis,
                        long nextThinkAtMillis,
                        long nextAttackAtMillis,
                        long buffExpireAtMillis,
                        double baseDamage,
                        Ref<EntityStore> currentTargetRef,
                        long targetLockExpireAtMillis,
                        boolean awakened) {
        this.ownerPlayerId = ownerPlayerId;
        this.ref = ref;
        this.ownerRef = ownerRef;
        this.classId = classId;
        this.styleId = styleId;
        this.ability = ability;
        this.spec = spec;
        this.hatchAtMillis = hatchAtMillis;
        this.expireAtMillis = expireAtMillis;
        this.nextThinkAtMillis = nextThinkAtMillis;
        this.nextAttackAtMillis = nextAttackAtMillis;
        this.buffExpireAtMillis = buffExpireAtMillis;
        this.baseDamage = baseDamage;
        this.currentTargetRef = currentTargetRef;
        this.targetLockExpireAtMillis = targetLockExpireAtMillis;
        this.awakened = awakened;
    }

    public String ownerPlayerId() { return ownerPlayerId; }
    public Ref<EntityStore> ownerRef() { return ownerRef; }
    public Ref<EntityStore> ref() { return ref; }
    public String classId() { return classId; }
    public String styleId() { return styleId; }
    public AbilityData ability() { return ability; }
    public String role() { return spec.role(); }
    public boolean ranged() { return spec.ranged(); }
    public double attackRange() { return spec.attackRange(); }
    public double chaseRange() { return spec.chaseRange(); }
    public long attackIntervalMillis() { return spec.attackIntervalMillis(); }
    public String attackToken() { return spec.attackToken(); }
    public long hatchAtMillis() { return hatchAtMillis; }
    public long nextThinkAtMillis() { return nextThinkAtMillis; }
    public long nextAttackAtMillis() { return nextAttackAtMillis; }
    public long buffExpireAtMillis() { return buffExpireAtMillis; }
    public long expireAtMillis() { return expireAtMillis; }
    public double baseDamage() { return baseDamage; }
    public Ref<EntityStore> currentTargetRef() { return currentTargetRef; }
    public long targetLockExpireAtMillis() { return targetLockExpireAtMillis; }
    public boolean awakened() { return awakened; }

    public void extend(long extensionMillis) {
        expireAtMillis += extensionMillis;
    }

    public void extendBuffUntil(long expireAtMillis) {
        buffExpireAtMillis = Math.max(buffExpireAtMillis, expireAtMillis);
    }

    public void commandAttackSoon(long now, long delayMillis) {
        nextAttackAtMillis = Math.min(nextAttackAtMillis, now + delayMillis);
        clearTargetLock();
    }

    public void scheduleNextThink(long now) {
        nextThinkAtMillis = now + SummonRuntimeSpecs.THINK_INTERVAL_MS;
    }

    public void awaken(long now) {
        awakened = true;
        nextAttackAtMillis = Math.min(nextAttackAtMillis, now + 200L);
        buffExpireAtMillis = Math.max(buffExpireAtMillis, now + 1800L);
    }

    public void clearTargetLock() {
        currentTargetRef = null;
        targetLockExpireAtMillis = 0L;
    }

    public void setTargetLock(Ref<EntityStore> targetRef, long now) {
        currentTargetRef = targetRef;
        targetLockExpireAtMillis = targetRef == null ? 0L : now + ("tank".equals(role()) ? 2200L : 1400L);
    }

    public boolean nowWithinBuffWindow(long now) {
        return now < buffExpireAtMillis;
    }

    public void scheduleNextAttack(long now) {
        nextAttackAtMillis = now + Math.max(
                450L,
                nowWithinBuffWindow(now) ? (long) (attackIntervalMillis() * 0.75) : attackIntervalMillis()
        );
    }

    public void expireCloneAfterStrike(long now) {
        if ("clone".equals(role())) {
            expireAtMillis = Math.min(expireAtMillis, now + 150L);
        }
    }
}
