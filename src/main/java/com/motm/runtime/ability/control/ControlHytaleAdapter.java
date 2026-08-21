package com.motm.runtime.ability.control;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.runtime.ability.summon.SummonMovementRuntime;
import com.motm.util.MotmEntityLiveness;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Hytale-side glue for the Controlled Ally primitive (Corruptus mentokinesis:
 * dominate / hivemind). Mirrors {@code SummonControlHytaleAdapter}: it drives an
 * existing hostile NPC as a MOTM puppet rather than swapping its persisted role
 * (which caused SEVERE {@code RoleChangeSystem} failures). Ownership is bound with
 * {@code NPCEntity.addReservation(ownerUuid)}; friendly-fire safety is enforced by
 * {@code GameplayPlaybackManager.shouldSuppressFriendlySummonDamage} via
 * {@link ControlRuntimeState}. The tick logic lives in the unit-tested
 * {@link ControlTickRuntime}; this class only supplies its {@link ControlTickRuntime.Hooks}.
 */
public final class ControlHytaleAdapter {
    public static final String MARKER_EFFECT_ID = "MOTM_Corruptus_Control_Marker";

    private static final double DEFAULT_ATTACK_RANGE = 3.0;
    private static final double DEFAULT_CHASE_RANGE = 18.0;
    private static final long DEFAULT_ATTACK_INTERVAL_MS = 900L;
    private static final double CONTROL_STRIKE_DAMAGE = 10.0;

    private final ControlRuntimeState controlState;
    private final ControlTickRuntime tickRuntime;
    private final SummonMovementRuntime movementRuntime;
    private final Support support;

    public ControlHytaleAdapter(ControlRuntimeState controlState,
                                ControlTickRuntime tickRuntime,
                                SummonMovementRuntime movementRuntime,
                                Support support) {
        this.controlState = controlState;
        this.tickRuntime = tickRuntime;
        this.movementRuntime = movementRuntime;
        this.support = support;
    }

    /**
     * Converts a live hostile NPC into a controlled ally of {@code owner} for
     * {@code durationMillis}. Recast on an already-controlled entity refreshes the
     * release clock. Returns {@code true} when a (new or refreshed) control is active.
     */
    public boolean convert(Ref<EntityStore> ownerRef,
                           PlayerData owner,
                           Ref<EntityStore> controlledRef,
                           Store<EntityStore> store,
                           String classId,
                           String styleId,
                           AbilityData ability,
                           long durationMillis,
                           long now) {
        if (owner == null || owner.getPlayerId() == null || store == null
                || controlledRef == null || !controlledRef.isValid()
                || ownerRef == null || !ownerRef.isValid()) {
            return false;
        }

        NPCEntity npc = store.getComponent(controlledRef, NPCEntity.getComponentType());
        if (npc == null || npc.isDespawning() || support.isMotmSummon(npc)) {
            return false;
        }
        if (store.getComponent(controlledRef, DeathComponent.getComponentType()) != null) {
            return false;
        }

        String entityId = support.resolveEntityId(controlledRef, store);
        long expireAt = now + Math.max(1L, durationMillis);

        ActiveControlledAlly existing = controlState.findByControlledRef(controlledRef);
        if (existing != null) {
            existing.refreshControlUntil(expireAt);
            return true;
        }

        UUID ownerUuid = parseUuid(owner.getPlayerId());
        if (ownerUuid != null) {
            npc.addReservation(ownerUuid);
        }

        ActiveControlledAlly ally = new ActiveControlledAlly(
                owner.getPlayerId(),
                controlledRef,
                ownerRef,
                entityId,
                classId,
                styleId,
                ability,
                DEFAULT_ATTACK_RANGE,
                DEFAULT_CHASE_RANGE,
                DEFAULT_ATTACK_INTERVAL_MS,
                "control_strike",
                expireAt,
                now,
                now,
                null,
                0L,
                false
        );
        controlState.add(owner.getPlayerId(), ally);
        support.logInfo("[MOTM] control_acquired: owner=" + owner.getPlayerId()
                + " controlled=" + entityId + " durationMs=" + durationMillis);
        return true;
    }

    /** Drives every controlled ally whose entity lives in {@code currentStore} for one tick. */
    public void processForStore(Store<EntityStore> currentStore, long now) {
        if (controlState == null || tickRuntime == null) {
            return;
        }
        controlState.removeProcessed(ally ->
                belongsToCurrentStore(ally.controlledRef(), currentStore) && processTick(ally, now));
    }

    /** Releases (and untracks) every controlled ally for a player, e.g. on logout/death. */
    public int releaseForOwner(String ownerPlayerId) {
        if (controlState == null) {
            return 0;
        }
        return controlState.removeForOwner(ownerPlayerId, this::releaseAlly);
    }

    private boolean processTick(ActiveControlledAlly ally, long now) {
        return tickRuntime.process(ally, now, new ControlTickRuntime.Hooks() {
            @Override
            public boolean release(ActiveControlledAlly ally) {
                return releaseAlly(ally);
            }

            @Override
            public boolean hasStore(ActiveControlledAlly ally) {
                return ally.controlledRef() != null && ally.controlledRef().getStore() != null;
            }

            @Override
            public PlayerData owner(String ownerPlayerId) {
                return support.owner(ownerPlayerId);
            }

            @Override
            public void applyMarker(ActiveControlledAlly ally) {
                support.applyEffectById(ally.controlledRef(), ally.controlledRef().getStore(), MARKER_EFFECT_ID);
            }

            @Override
            public Ref<EntityStore> resolveHostileTarget(ActiveControlledAlly ally, long now) {
                return ControlHytaleAdapter.this.resolveHostileTarget(ally, ally.controlledRef().getStore());
            }

            @Override
            public void followOwner(ActiveControlledAlly ally) {
                ControlHytaleAdapter.this.moveTowardOwner(ally, ally.controlledRef().getStore());
            }

            @Override
            public Vector3d position(Ref<EntityStore> ref) {
                return ControlHytaleAdapter.this.position(ref, ally.controlledRef().getStore());
            }

            @Override
            public void moveTowardTarget(ActiveControlledAlly ally, Ref<EntityStore> targetRef, double desiredRange) {
                ControlHytaleAdapter.this.moveTowardTarget(ally, targetRef, ally.controlledRef().getStore(), desiredRange);
            }

            @Override
            public void attack(ActiveControlledAlly ally, PlayerData owner, Ref<EntityStore> targetRef, long now) {
                ControlHytaleAdapter.this.attack(ally, targetRef, ally.controlledRef().getStore());
            }
        });
    }

    private boolean releaseAlly(ActiveControlledAlly ally) {
        if (ally == null || ally.controlledRef() == null) {
            return true;
        }
        Store<EntityStore> store = ally.controlledRef().getStore();
        if (store != null && ally.controlledRef().isValid()) {
            NPCEntity npc = store.getComponent(ally.controlledRef(), NPCEntity.getComponentType());
            if (npc != null) {
                UUID ownerUuid = parseUuid(ally.ownerPlayerId());
                if (ownerUuid != null) {
                    npc.removeReservation(ownerUuid);
                }
            }
        }
        support.logInfo("[MOTM] control_released: owner=" + ally.ownerPlayerId()
                + " controlled=" + ally.controlledEntityId());
        return true;
    }

    private void attack(ActiveControlledAlly ally, Ref<EntityStore> targetRef, Store<EntityStore> store) {
        if (store == null || targetRef == null || !MotmEntityLiveness.isLiveTarget(targetRef, store)) {
            return;
        }
        // Command the puppet's native AI to engage (best-effort visual) ...
        NPCEntity npc = store.getComponent(ally.controlledRef(), NPCEntity.getComponentType());
        if (npc != null) {
            npc.onFlockSetTarget(ally.controlledEntityId(), targetRef);
        }
        // ... and apply deterministic owner-attributed damage so the fight resolves.
        Damage damage = new Damage(new Damage.EntitySource(ally.controlledRef()),
                DamageCause.PHYSICAL,
                (float) CONTROL_STRIKE_DAMAGE);
        DamageSystems.executeDamage(targetRef, store, damage);
        support.logInfo("[MOTM] control_attack: owner=" + ally.ownerPlayerId()
                + " controlled=" + ally.controlledEntityId()
                + " target=" + support.resolveEntityId(targetRef, store)
                + " damage=" + CONTROL_STRIKE_DAMAGE);
    }

    private void moveTowardOwner(ActiveControlledAlly ally, Store<EntityStore> store) {
        Vector3d allyPosition = position(ally.controlledRef(), store);
        Vector3d ownerPosition = position(ally.ownerRef(), store);
        if (allyPosition == null || ownerPosition == null || movementRuntime == null) {
            return;
        }
        NPCEntity npc = store.getComponent(ally.controlledRef(), NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }
        Vector3d destination = movementRuntime.ownerFollowDestination(allyPosition, ownerPosition);
        if (destination != null) {
            npc.moveTo(ally.controlledRef(), destination.x, destination.y, destination.z, store);
        }
    }

    private void moveTowardTarget(ActiveControlledAlly ally,
                                  Ref<EntityStore> targetRef,
                                  Store<EntityStore> store,
                                  double desiredRange) {
        Vector3d allyPosition = position(ally.controlledRef(), store);
        Vector3d targetPosition = position(targetRef, store);
        if (allyPosition == null || targetPosition == null || movementRuntime == null) {
            return;
        }
        NPCEntity npc = store.getComponent(ally.controlledRef(), NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }
        Vector3d destination = movementRuntime.targetApproachDestination(allyPosition, targetPosition, desiredRange);
        if (destination != null) {
            npc.moveTo(ally.controlledRef(), destination.x, destination.y, destination.z, store);
        }
    }

    private Ref<EntityStore> resolveHostileTarget(ActiveControlledAlly ally, Store<EntityStore> store) {
        Vector3d anchor = position(ally.controlledRef(), store);
        if (anchor == null || store == null) {
            return null;
        }
        List<NearbyTargetCandidate> candidates = new ArrayList<>();
        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid() || ref.equals(ally.controlledRef())) {
                    continue;
                }
                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning() || support.isMotmSummon(npc)) {
                    continue;
                }
                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }
                // Do not target the owner's own controlled allies.
                String candidateId = support.resolveEntityId(ref, store);
                if (candidateId != null && controlState.isControlledEntity(candidateId)) {
                    continue;
                }
                TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
                if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                    continue;
                }
                double candidateDistance = distance(anchor, transform.getTransform().getPosition());
                if (candidateDistance <= ally.chaseRange()) {
                    candidates.add(new NearbyTargetCandidate(ref, candidateDistance));
                }
            }
        });
        return candidates.stream()
                .min((left, right) -> Double.compare(left.distance(), right.distance()))
                .map(NearbyTargetCandidate::ref)
                .orElse(null);
    }

    private Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
        Transform transform = transform(ref, store);
        return transform == null ? null : transform.getPosition();
    }

    private static Transform transform(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }
        return transform.getTransform();
    }

    private static boolean belongsToCurrentStore(Ref<EntityStore> ref, Store<EntityStore> currentStore) {
        return ref != null && ref.isValid() && ref.getStore() == currentStore;
    }

    private static double distance(Vector3d left, Vector3d right) {
        if (left == null || right == null) {
            return Double.MAX_VALUE;
        }
        double dx = left.x - right.x;
        double dy = left.y - right.y;
        double dz = left.z - right.z;
        return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
    }

    private static UUID parseUuid(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public interface Support {
        PlayerData owner(String ownerPlayerId);

        boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store);

        boolean isMotmSummon(NPCEntity npc);

        void logInfo(String message);
    }

    private record NearbyTargetCandidate(Ref<EntityStore> ref, double distance) {
    }
}
