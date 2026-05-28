package com.motm.runtime.ability.transformation;

import org.joml.Vector3d;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;

import java.util.List;

public final class ActiveTransformation {
    private final String playerId;
    private final Ref<EntityStore> ownerRef;
    private final AbilityData sourceAbility;
    private final String modelId;
    private final long expireAtMillis;
    private final TransformationRuntimeSpec spec;
    private Vector3d lastOwnerPosition;

    private ActiveTransformation(String playerId,
                                 Ref<EntityStore> ownerRef,
                                 AbilityData sourceAbility,
                                 String modelId,
                                 long expireAtMillis,
                                 TransformationRuntimeSpec spec,
                                 Vector3d lastOwnerPosition) {
        this.playerId = playerId;
        this.ownerRef = ownerRef;
        this.sourceAbility = sourceAbility;
        this.modelId = modelId;
        this.expireAtMillis = expireAtMillis;
        this.spec = spec != null ? spec : TransformationRuntimeSpec.fallback();
        this.lastOwnerPosition = lastOwnerPosition != null ? new Vector3d(lastOwnerPosition) : null;
    }

    public static ActiveTransformation create(String playerId,
                                              Ref<EntityStore> ownerRef,
                                              AbilityData sourceAbility,
                                              String modelId,
                                              long expireAtMillis,
                                              Vector3d initialPosition) {
        return new ActiveTransformation(
                playerId,
                ownerRef,
                sourceAbility,
                modelId,
                expireAtMillis,
                TransformationRuntimeSpecs.resolve(sourceAbility),
                initialPosition
        );
    }

    public String playerId() { return playerId; }
    public Ref<EntityStore> ownerRef() { return ownerRef; }
    public AbilityData sourceAbility() { return sourceAbility; }
    public String modelId() { return modelId; }
    public long expireAtMillis() { return expireAtMillis; }
    public TransformationRuntimeKind kind() { return spec.kind(); }
    public double damageBonus() { return spec.damageBonus(); }
    public double weaponBonus() { return spec.weaponBonus(); }
    public double movementMultiplier() { return spec.movementMultiplier(); }
    public double verticalBonus() { return spec.verticalBonus(); }
    public String weaponRiderToken() { return spec.weaponRiderToken(); }
    public double locomotionTriggerDistance() { return spec.locomotionTriggerDistance(); }
    public double collisionRadius() { return spec.collisionRadius(); }
    public List<String> ownerRuntimeTokens() { return spec.ownerRuntimeTokens(); }
    public double ownerShieldAmount() { return spec.ownerShieldAmount(); }
    public boolean endsWhenGrounded() { return spec.endsWhenGrounded(); }
    public Vector3d lastOwnerPosition() { return lastOwnerPosition; }
    public void updateLastOwnerPosition(Vector3d position) {
        lastOwnerPosition = position != null ? new Vector3d(position) : null;
    }
    public String summary() { return spec.summary(); }
    public String abilityId() { return sourceAbility != null ? sourceAbility.getId() : ""; }
}
