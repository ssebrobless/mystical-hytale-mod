package com.motm.runtime.ability.projectile;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;

import java.util.HashSet;
import java.util.Set;

public final class ActiveProjectile {
    private final String ownerPlayerId;
    private final Ref<EntityStore> ownerRef;
    private final String classId;
    private final String styleId;
    private final AbilityData ability;
    private final Vector3d position;
    private final Vector3d direction;
    private final double speedPerTick;
    private final double maxDistance;
    private final double impactRadius;
    private final double collisionRadius;
    private final double ownerSelfClearanceDistance;
    private final long activateAtMillis;
    private final long expireAtMillis;
    private final double baseDamage;
    private final Set<String> hitEntityIds;
    private final Ref<EntityStore> visualRef;
    private final String travelEffectId;
    private final String traceId;
    private long nextVisualRefreshAtMillis;
    private double travelledDistance;

    public ActiveProjectile(String ownerPlayerId,
                            Ref<EntityStore> ownerRef,
                            String classId,
                            String styleId,
                            AbilityData ability,
                            Vector3d position,
                            Vector3d direction,
                            double speedPerTick,
                            double maxDistance,
                            double impactRadius,
                            double collisionRadius,
                            double ownerSelfClearanceDistance,
                            long activateAtMillis,
                            long expireAtMillis,
                            double baseDamage,
                            Set<String> hitEntityIds,
                            Ref<EntityStore> visualRef,
                            String travelEffectId,
                            long nextVisualRefreshAtMillis,
                            String traceId) {
        this.ownerPlayerId = ownerPlayerId;
        this.ownerRef = ownerRef;
        this.classId = classId;
        this.styleId = styleId;
        this.ability = ability;
        this.position = position == null ? null : position.clone();
        this.direction = direction == null ? null : direction.clone();
        this.speedPerTick = speedPerTick;
        this.maxDistance = maxDistance;
        this.impactRadius = impactRadius;
        this.collisionRadius = collisionRadius;
        this.ownerSelfClearanceDistance = ownerSelfClearanceDistance;
        this.activateAtMillis = activateAtMillis;
        this.expireAtMillis = expireAtMillis;
        this.baseDamage = baseDamage;
        this.hitEntityIds = hitEntityIds == null ? new HashSet<>() : hitEntityIds;
        this.visualRef = visualRef;
        this.travelEffectId = travelEffectId;
        this.traceId = traceId;
        this.nextVisualRefreshAtMillis = nextVisualRefreshAtMillis;
        this.travelledDistance = 0.0;
    }

    public String ownerPlayerId() { return ownerPlayerId; }
    public Ref<EntityStore> ownerRef() { return ownerRef; }
    public String classId() { return classId; }
    public String styleId() { return styleId; }
    public AbilityData ability() { return ability; }
    public Vector3d position() { return position; }
    public Vector3d direction() { return direction; }
    public double speedPerTick() { return speedPerTick; }
    public double maxDistance() { return maxDistance; }
    public double impactRadius() { return impactRadius; }
    public double collisionRadius() { return collisionRadius; }
    public double ownerSelfClearanceDistance() { return ownerSelfClearanceDistance; }
    public long activateAtMillis() { return activateAtMillis; }
    public long expireAtMillis() { return expireAtMillis; }
    public double baseDamage() { return baseDamage; }
    public Set<String> hitEntityIds() { return hitEntityIds; }
    public Ref<EntityStore> visualRef() { return visualRef; }
    public String travelEffectId() { return travelEffectId; }
    public String traceId() { return traceId; }
    public long nextVisualRefreshAtMillis() { return nextVisualRefreshAtMillis; }
    public double travelledDistance() { return travelledDistance; }

    public void advanceTo(Vector3d nextPosition, double distance) {
        if (position != null && nextPosition != null) {
            position.x = nextPosition.x;
            position.y = nextPosition.y;
            position.z = nextPosition.z;
        }
        travelledDistance += Math.max(0.0, distance);
    }

    public void scheduleNextVisualRefresh(long now, long refreshIntervalMillis) {
        nextVisualRefreshAtMillis = now + Math.max(0L, refreshIntervalMillis);
    }
}
