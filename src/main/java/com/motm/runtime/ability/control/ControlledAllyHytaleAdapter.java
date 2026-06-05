package com.motm.runtime.ability.control;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ControlledAllyHytaleAdapter {
    public static final String CONTROL_MARKER_EFFECT_ID = "MOTM_Arbor_Sapling_Pink_Glow";
    private static final long THINK_INTERVAL_MS = 450L;
    private static final long ATTACK_INTERVAL_MS = 1250L;
    private static final double ATTACK_RANGE = 2.7;
    private static final double FOLLOW_RANGE = 5.0;
    private static final double MAX_OWNER_RANGE = 18.0;
    private static final double TARGET_SCAN_RADIUS = 10.0;

    private final ControlledAllyRuntimeState state;
    private final Support support;

    public ControlledAllyHytaleAdapter(ControlledAllyRuntimeState state, Support support) {
        this.state = state;
        this.support = support;
    }

    public Result activate(PlayerData owner,
                           Ref<EntityStore> ownerRef,
                           AbilityData ability,
                           List<Ref<EntityStore>> targets) {
        if (state == null || support == null || owner == null || ownerRef == null || !ownerRef.isValid()
                || ability == null || !isControlAbility(ability)) {
            return Result.none();
        }
        Store<EntityStore> store = ownerRef.getStore();
        if (store == null || targets == null || targets.isEmpty()) {
            return Result.none();
        }

        long now = System.currentTimeMillis();
        long expireAt = now + durationMillis(ability);
        int controlled = 0;
        int maxTargets = "dominate".equals(abilityId(ability)) ? 1 : targets.size();
        for (Ref<EntityStore> targetRef : targets) {
            if (controlled >= maxTargets) {
                break;
            }
            if (!isValidControlledTarget(targetRef, store, ownerRef)) {
                continue;
            }
            ActiveControlledAlly ally = new ActiveControlledAlly(
                    owner.getPlayerId(),
                    ownerRef,
                    targetRef,
                    ability,
                    expireAt,
                    now + 150L,
                    now + 500L
            );
            state.addOrRefresh(owner.getPlayerId(), ally);
            support.applyEffectById(targetRef, store, CONTROL_MARKER_EFFECT_ID);
            controlled++;
        }

        if (controlled <= 0) {
            return Result.none();
        }
        return new Result(controlled, "controlled " + controlled + " hostile" + (controlled == 1 ? "" : "s"));
    }

    public Result releaseOwnedTarget(PlayerData owner, Ref<EntityStore> targetRef) {
        if (state == null || owner == null || targetRef == null || !targetRef.isValid()) {
            return Result.none();
        }
        boolean released = state.removeOwnedRef(owner.getPlayerId(), targetRef, ally -> cleanup(ally, null));
        return released ? new Result(0, "released controlled ally") : Result.none();
    }

    public void processForStore(Store<EntityStore> currentStore, long now) {
        if (state == null) {
            return;
        }
        state.removeProcessedAllies(ally ->
                belongsToCurrentStore(ally.ref(), currentStore) && processAllyTick(ally, currentStore, now),
                ally -> cleanup(ally, currentStore));
    }

    public Vector3d firstControlledPosition(String ownerPlayerId, Store<EntityStore> store) {
        if (state == null || store == null) {
            return null;
        }
        for (ActiveControlledAlly ally : state.alliesForOwner(ownerPlayerId)) {
            if (ally != null && belongsToCurrentStore(ally.ref(), store)) {
                Vector3d position = position(ally.ref(), store);
                if (position != null) {
                    return position;
                }
            }
        }
        return null;
    }

    public int removeAlliesForPlayer(String ownerPlayerId) {
        return state == null ? 0 : state.removeAlliesForPlayer(ownerPlayerId, ally -> cleanup(ally, null));
    }

    private boolean processAllyTick(ActiveControlledAlly ally, Store<EntityStore> store, long now) {
        if (ally == null || ally.ref() == null || !ally.ref().isValid()) {
            return true;
        }
        if (store == null || store.getComponent(ally.ref(), DeathComponent.getComponentType()) != null) {
            return true;
        }
        if (now >= ally.expireAtMillis()) {
            return true;
        }
        if (now < ally.nextThinkAtMillis()) {
            return false;
        }
        if (ally.ownerRef() == null || !ally.ownerRef().isValid()) {
            return true;
        }

        Vector3d allyPosition = position(ally.ref(), store);
        Vector3d ownerPosition = position(ally.ownerRef(), store);
        if (allyPosition == null || ownerPosition == null) {
            ally.scheduleNextThink(now, THINK_INTERVAL_MS);
            return false;
        }

        Ref<EntityStore> targetRef = resolveTarget(ally, store, now, allyPosition, ownerPosition);
        if (targetRef == null || !targetRef.isValid()) {
            ally.clearTargetLock();
            moveTowardOwnerIfNeeded(ally, store, allyPosition, ownerPosition);
            ally.scheduleNextThink(now, THINK_INTERVAL_MS);
            return false;
        }

        Vector3d targetPosition = position(targetRef, store);
        if (targetPosition == null) {
            ally.clearTargetLock();
            ally.scheduleNextThink(now, THINK_INTERVAL_MS);
            return false;
        }

        double distanceToTarget = distance(allyPosition, targetPosition);
        if (distanceToTarget > ATTACK_RANGE) {
            moveTowardPoint(ally.ref(), store, allyPosition, targetPosition, ATTACK_RANGE * 0.75);
        } else if (now >= ally.nextAttackAtMillis()) {
            attack(ally, targetRef, store, now);
        }

        ally.scheduleNextThink(now, THINK_INTERVAL_MS);
        return false;
    }

    private Ref<EntityStore> resolveTarget(ActiveControlledAlly ally,
                                           Store<EntityStore> store,
                                           long now,
                                           Vector3d allyPosition,
                                           Vector3d ownerPosition) {
        if (ally.currentTargetRef() != null
                && now < ally.targetLockExpireAtMillis()
                && isValidHostileTarget(ally.currentTargetRef(), store, ownerPosition, MAX_OWNER_RANGE)) {
            return ally.currentTargetRef();
        }

        Ref<EntityStore> target = findNearestHostile(store, ownerPosition, TARGET_SCAN_RADIUS);
        if (target == null) {
            target = findNearestHostile(store, allyPosition, TARGET_SCAN_RADIUS);
        }
        ally.setTargetLock(target, now);
        return target;
    }

    private void attack(ActiveControlledAlly ally, Ref<EntityStore> targetRef, Store<EntityStore> store, long now) {
        double damageAmount = support.controlledAllyDamage(ally.ability());
        if (damageAmount <= 0.0) {
            damageAmount = 4.0;
        }
        Damage damage = new Damage(new Damage.EntitySource(ally.ref()), DamageCause.PHYSICAL, (float) damageAmount);
        DamageSystems.executeDamage(targetRef, store, damage);
        support.applyEffectById(targetRef, store, CONTROL_MARKER_EFFECT_ID);
        ally.scheduleNextAttack(now, ATTACK_INTERVAL_MS);
        support.logInfo("[MOTM] Controlled ally attack resolved: owner=" + ally.ownerPlayerId()
                + " ability=" + abilityId(ally.ability())
                + " damage=" + String.format(Locale.ROOT, "%.2f", damageAmount));
    }

    private void moveTowardOwnerIfNeeded(ActiveControlledAlly ally,
                                         Store<EntityStore> store,
                                         Vector3d allyPosition,
                                         Vector3d ownerPosition) {
        double ownerDistance = distance(allyPosition, ownerPosition);
        if (ownerDistance <= FOLLOW_RANGE) {
            return;
        }
        moveTowardPoint(ally.ref(), store, allyPosition, ownerPosition, FOLLOW_RANGE * 0.8);
    }

    private void moveTowardPoint(Ref<EntityStore> ref,
                                 Store<EntityStore> store,
                                 Vector3d current,
                                 Vector3d target,
                                 double stopDistance) {
        NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
        if (npc == null || current == null || target == null) {
            return;
        }
        Vector3d direction = new Vector3d(target).sub(current);
        direction.y = 0.0;
        double remaining = direction.length();
        if (remaining <= stopDistance + 0.05) {
            return;
        }
        direction.normalize();
        double step = Math.min(2.2, Math.max(0.0, remaining - stopDistance));
        Vector3d destination = new Vector3d(current).add(direction.mul(step));
        npc.moveTo(ref, destination.x, destination.y, destination.z, store);
    }

    private Ref<EntityStore> findNearestHostile(Store<EntityStore> store, Vector3d center, double radius) {
        if (store == null || center == null) {
            return null;
        }
        List<Candidate> candidates = new ArrayList<>();
        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (!isValidHostileTarget(ref, store, center, radius)) {
                    continue;
                }
                Vector3d position = position(ref, store);
                candidates.add(new Candidate(ref, distance(center, position)));
            }
        });
        return candidates.stream()
                .min((left, right) -> Double.compare(left.distance(), right.distance()))
                .map(Candidate::ref)
                .orElse(null);
    }

    private boolean isValidControlledTarget(Ref<EntityStore> targetRef, Store<EntityStore> store, Ref<EntityStore> ownerRef) {
        return isValidHostileTarget(targetRef, store, position(ownerRef, store), 18.0);
    }

    private boolean isValidHostileTarget(Ref<EntityStore> targetRef, Store<EntityStore> store, Vector3d anchor, double radius) {
        if (targetRef == null || !targetRef.isValid() || store == null || anchor == null) {
            return false;
        }
        if (state != null && state.isControlled(targetRef)) {
            return false;
        }
        if (support.isFriendlyOwned(targetRef, store)) {
            return false;
        }
        NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
        if (npc == null || npc.isDespawning()) {
            return false;
        }
        if (store.getComponent(targetRef, DeathComponent.getComponentType()) != null) {
            return false;
        }
        Vector3d position = position(targetRef, store);
        return position != null && distance(anchor, position) <= radius;
    }

    private void cleanup(ActiveControlledAlly ally, Store<EntityStore> fallbackStore) {
        if (ally == null || support == null) {
            return;
        }
        Store<EntityStore> store = ally.ref() != null && ally.ref().isValid() ? ally.ref().getStore() : fallbackStore;
        if (store != null) {
            support.removeEffectById(ally.ref(), store, CONTROL_MARKER_EFFECT_ID);
        }
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
        return transform == null ? null : transform.getTransform();
    }

    private static boolean belongsToCurrentStore(Ref<EntityStore> ref, Store<EntityStore> currentStore) {
        return ref != null && ref.isValid() && ref.getStore() == currentStore;
    }

    private static boolean isControlAbility(AbilityData ability) {
        String abilityId = abilityId(ability);
        return "dominate".equals(abilityId) || "hivemind".equals(abilityId);
    }

    private static String abilityId(AbilityData ability) {
        return ability == null || ability.getId() == null ? "" : ability.getId().toLowerCase(Locale.ROOT);
    }

    private static long durationMillis(AbilityData ability) {
        double seconds = ability != null && ability.getDurationSeconds() > 0 ? ability.getDurationSeconds() : 6.0;
        if ("dominate".equals(abilityId(ability))) {
            seconds = Math.min(seconds, 300.0);
        }
        return Math.max(1_000L, Math.round(seconds * 1000.0));
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

    public interface Support {
        boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        boolean removeEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        boolean isFriendlyOwned(Ref<EntityStore> ref, Store<EntityStore> store);

        double controlledAllyDamage(AbilityData ability);

        void logInfo(String message);
    }

    public record Result(int controlled, String summary) {
        public Result {
            summary = summary == null ? "" : summary;
        }

        public static Result none() {
            return new Result(0, "");
        }
    }

    private record Candidate(Ref<EntityStore> ref, double distance) {
    }
}
