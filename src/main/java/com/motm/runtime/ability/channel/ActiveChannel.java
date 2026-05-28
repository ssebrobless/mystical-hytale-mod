package com.motm.runtime.ability.channel;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;

public final class ActiveChannel {
    private final String ownerPlayerId;
    private final Ref<EntityStore> ownerRef;
    private final Ref<EntityStore> targetRef;
    private final AbilityData ability;
    private final long expireAtMillis;
    private long nextPulseAtMillis;

    public ActiveChannel(String ownerPlayerId,
                         Ref<EntityStore> ownerRef,
                         Ref<EntityStore> targetRef,
                         AbilityData ability,
                         long expireAtMillis,
                         long nextPulseAtMillis) {
        this.ownerPlayerId = ownerPlayerId;
        this.ownerRef = ownerRef;
        this.targetRef = targetRef;
        this.ability = ability;
        this.expireAtMillis = expireAtMillis;
        this.nextPulseAtMillis = nextPulseAtMillis;
    }

    public String ownerPlayerId() { return ownerPlayerId; }
    public Ref<EntityStore> ownerRef() { return ownerRef; }
    public Ref<EntityStore> targetRef() { return targetRef; }
    public AbilityData ability() { return ability; }
    public long expireAtMillis() { return expireAtMillis; }
    public long nextPulseAtMillis() { return nextPulseAtMillis; }

    public void scheduleNextPulse(long now, long intervalMillis) {
        nextPulseAtMillis = now + Math.max(0L, intervalMillis);
    }
}
