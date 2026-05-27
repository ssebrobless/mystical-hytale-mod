package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class ActiveLapidaryGem {
    private final String ownerPlayerId;
    private final Ref<EntityStore> ref;
    private final Vector3d center;
    private double currentHp;
    private final double maxHp;
    private final long expireAtMillis;
    private String lastLabel;

    public ActiveLapidaryGem(String ownerPlayerId,
                             Ref<EntityStore> ref,
                             Vector3d center,
                             double currentHp,
                             double maxHp,
                             long expireAtMillis,
                             String lastLabel) {
        this.ownerPlayerId = ownerPlayerId;
        this.ref = ref;
        this.center = center == null ? null : center.clone();
        this.currentHp = currentHp;
        this.maxHp = maxHp;
        this.expireAtMillis = expireAtMillis;
        this.lastLabel = lastLabel;
    }

    public String ownerPlayerId() { return ownerPlayerId; }
    public Ref<EntityStore> ref() { return ref; }
    public Vector3d center() { return center == null ? null : center.clone(); }
    public double currentHp() { return currentHp; }
    public double maxHp() { return maxHp; }
    public long expireAtMillis() { return expireAtMillis; }
    public String lastLabel() { return lastLabel; }

    public boolean expired(long now) {
        return now >= expireAtMillis;
    }

    public boolean updateHealthLabel(double currentHp, String label) {
        if (label == null || label.equals(lastLabel)) {
            return false;
        }
        this.currentHp = currentHp;
        this.lastLabel = label;
        return true;
    }
}
