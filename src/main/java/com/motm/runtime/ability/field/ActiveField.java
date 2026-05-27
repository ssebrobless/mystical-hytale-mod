package com.motm.runtime.ability.field;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;

import java.util.List;

public final class ActiveField {
    private final String ownerPlayerId;
    private final Ref<EntityStore> ownerRef;
    private final String classId;
    private final String styleId;
    private final AbilityData ability;
    private Vector3d center;
    private final Vector3d forwardDirection;
    private final Vector3d lineDirection;
    private final double radius;
    private final double halfWidth;
    private final double thickness;
    private final long expireAtMillis;
    private final long activateAtMillis;
    private final boolean followOwner;
    private final List<Ref<EntityStore>> visualRefs;
    private final String loopEffectId;
    private final String traceId;
    private long nextPulseAtMillis;
    private long nextVisualRefreshAtMillis;

    public ActiveField(String ownerPlayerId,
                       Ref<EntityStore> ownerRef,
                       String classId,
                       String styleId,
                       AbilityData ability,
                       Vector3d center,
                       Vector3d forwardDirection,
                       Vector3d lineDirection,
                       double radius,
                       double halfWidth,
                       double thickness,
                       long expireAtMillis,
                       long activateAtMillis,
                       long nextPulseAtMillis,
                       boolean followOwner,
                       List<Ref<EntityStore>> visualRefs,
                       String loopEffectId,
                       long nextVisualRefreshAtMillis,
                       String traceId) {
        this.ownerPlayerId = ownerPlayerId;
        this.ownerRef = ownerRef;
        this.classId = classId;
        this.styleId = styleId;
        this.ability = ability;
        this.center = cloneOrNull(center);
        this.forwardDirection = cloneOrNull(forwardDirection);
        this.lineDirection = cloneOrNull(lineDirection);
        this.radius = radius;
        this.halfWidth = halfWidth;
        this.thickness = thickness;
        this.expireAtMillis = expireAtMillis;
        this.activateAtMillis = activateAtMillis;
        this.nextPulseAtMillis = nextPulseAtMillis;
        this.followOwner = followOwner;
        this.visualRefs = visualRefs == null ? List.of() : List.copyOf(visualRefs);
        this.loopEffectId = loopEffectId;
        this.traceId = traceId;
        this.nextVisualRefreshAtMillis = nextVisualRefreshAtMillis;
    }

    public String ownerPlayerId() { return ownerPlayerId; }
    public Ref<EntityStore> ownerRef() { return ownerRef; }
    public String classId() { return classId; }
    public String styleId() { return styleId; }
    public AbilityData ability() { return ability; }
    public Vector3d center() { return center; }
    public Vector3d forwardDirection() { return forwardDirection; }
    public Vector3d lineDirection() { return lineDirection; }
    public double radius() { return radius; }
    public double halfWidth() { return halfWidth; }
    public double thickness() { return thickness; }
    public long expireAtMillis() { return expireAtMillis; }
    public long activateAtMillis() { return activateAtMillis; }
    public long nextPulseAtMillis() { return nextPulseAtMillis; }
    public boolean followOwner() { return followOwner; }
    public List<Ref<EntityStore>> visualRefs() { return visualRefs; }
    public String loopEffectId() { return loopEffectId; }
    public String traceId() { return traceId; }
    public long nextVisualRefreshAtMillis() { return nextVisualRefreshAtMillis; }

    public void updateCenter(Vector3d center) {
        this.center = cloneOrNull(center);
    }

    public void scheduleNextPulse(long now) {
        this.nextPulseAtMillis = now + FieldRuntimeSpecs.FIELD_PULSE_INTERVAL_MS;
    }

    public void scheduleNextVisualRefresh(long now) {
        this.nextVisualRefreshAtMillis = now + FieldRuntimeSpecs.FIELD_VISUAL_REFRESH_MS;
    }

    private static Vector3d cloneOrNull(Vector3d vector) {
        return vector == null ? null : vector.clone();
    }
}
