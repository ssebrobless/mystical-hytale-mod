package com.motm.runtime.ability.dash;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import org.joml.Vector3d;

import java.util.HashSet;
import java.util.Set;

/** One server-side dash window; movement itself is owned by Hytale velocity. */
public final class ActiveDash {
    private final String ownerPlayerId;
    private final String classId;
    private final String styleId;
    private final AbilityData ability;
    private final Ref<EntityStore> ownerRef;
    private final Vector3d startPosition;
    private final Vector3d direction;
    private final long startedAtMillis;
    private final long expiresAtMillis;
    private final double distance;
    private final Set<Integer> sweptTargetIndices = new HashSet<>();
    private long lastTrailAtMillis;
    private boolean endCueApplied;

    public ActiveDash(String ownerPlayerId,
                      String classId,
                      String styleId,
                      AbilityData ability,
                      Ref<EntityStore> ownerRef,
                      Vector3d startPosition,
                      Vector3d direction,
                      long startedAtMillis,
                      long expiresAtMillis,
                      double distance) {
        this.ownerPlayerId = ownerPlayerId;
        this.classId = classId;
        this.styleId = styleId;
        this.ability = ability;
        this.ownerRef = ownerRef;
        this.startPosition = new Vector3d(startPosition);
        this.direction = new Vector3d(direction);
        this.startedAtMillis = startedAtMillis;
        this.expiresAtMillis = expiresAtMillis;
        this.distance = distance;
    }

    public String ownerPlayerId() { return ownerPlayerId; }
    public String classId() { return classId; }
    public String styleId() { return styleId; }
    public AbilityData ability() { return ability; }
    public Ref<EntityStore> ownerRef() { return ownerRef; }
    public Vector3d startPosition() { return new Vector3d(startPosition); }
    public Vector3d direction() { return new Vector3d(direction); }
    public long startedAtMillis() { return startedAtMillis; }
    public long expiresAtMillis() { return expiresAtMillis; }
    public double distance() { return distance; }
    public long lastTrailAtMillis() { return lastTrailAtMillis; }
    public void markTrail(long now) { lastTrailAtMillis = now; }
    public boolean endCueApplied() { return endCueApplied; }
    public void markEndCueApplied() { endCueApplied = true; }
    public boolean hasSweptTarget(int index) { return sweptTargetIndices.contains(index); }
    public void markSweptTarget(int index) { sweptTargetIndices.add(index); }
}
