package com.motm.runtime.ability.self;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ActiveSelfEffect {
    private final String ownerPlayerId;
    private final Ref<EntityStore> ownerRef;
    private final String effectId;
    private final long expireAtMillis;
    private final boolean pulse;
    private final String sourceAbilityId;
    private long nextApplyAtMillis;

    public ActiveSelfEffect(String ownerPlayerId,
                            Ref<EntityStore> ownerRef,
                            String effectId,
                            long expireAtMillis,
                            long nextApplyAtMillis) {
        this(ownerPlayerId, ownerRef, effectId, expireAtMillis, nextApplyAtMillis, false, null);
    }

    public ActiveSelfEffect(String ownerPlayerId,
                            Ref<EntityStore> ownerRef,
                            String effectId,
                            long expireAtMillis,
                            long nextApplyAtMillis,
                            boolean pulse) {
        this(ownerPlayerId, ownerRef, effectId, expireAtMillis, nextApplyAtMillis, pulse, null);
    }

    public ActiveSelfEffect(String ownerPlayerId,
                            Ref<EntityStore> ownerRef,
                            String effectId,
                            long expireAtMillis,
                            long nextApplyAtMillis,
                            boolean pulse,
                            String sourceAbilityId) {
        this.ownerPlayerId = ownerPlayerId;
        this.ownerRef = ownerRef;
        this.effectId = effectId;
        this.expireAtMillis = expireAtMillis;
        this.nextApplyAtMillis = nextApplyAtMillis;
        this.pulse = pulse;
        this.sourceAbilityId = sourceAbilityId;
    }

    public String ownerPlayerId() { return ownerPlayerId; }
    public Ref<EntityStore> ownerRef() { return ownerRef; }
    public String effectId() { return effectId; }
    public long expireAtMillis() { return expireAtMillis; }
    public long nextApplyAtMillis() { return nextApplyAtMillis; }
    /** Pulse loops re-fire application particles by removing before re-applying. */
    public boolean pulse() { return pulse; }
    /** Ability that started this effect, or null for system-owned effects. */
    public String sourceAbilityId() { return sourceAbilityId; }

    public boolean expired(long now) {
        return now >= expireAtMillis;
    }

    public boolean readyToApply(long now) {
        return now >= nextApplyAtMillis;
    }

    public void scheduleNextApply(long now, long intervalMillis) {
        nextApplyAtMillis = now + Math.max(0L, intervalMillis);
    }
}
