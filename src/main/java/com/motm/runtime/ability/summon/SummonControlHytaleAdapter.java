package com.motm.runtime.ability.summon;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;

import java.util.ArrayList;
import java.util.List;

public final class SummonControlHytaleAdapter {
    private final SummonRuntimeState summonState;
    private final SummonLifecycleHytaleAdapter lifecycleAdapter;
    private final SummonTickRuntime tickRuntime;
    private final SummonBuffRuntime buffRuntime;
    private final SummonMovementRuntime movementRuntime;
    private final SummonTargetRuntime targetRuntime;
    private final Support support;

    public SummonControlHytaleAdapter(SummonRuntimeState summonState,
                                      SummonLifecycleHytaleAdapter lifecycleAdapter,
                                      SummonTickRuntime tickRuntime,
                                      SummonBuffRuntime buffRuntime,
                                      SummonMovementRuntime movementRuntime,
                                      SummonTargetRuntime targetRuntime,
                                      Support support) {
        this.summonState = summonState;
        this.lifecycleAdapter = lifecycleAdapter;
        this.tickRuntime = tickRuntime;
        this.buffRuntime = buffRuntime;
        this.movementRuntime = movementRuntime;
        this.targetRuntime = targetRuntime;
        this.support = support;
    }

    public void processForStore(Store<EntityStore> currentStore, long now) {
        if (summonState == null || tickRuntime == null) {
            return;
        }
        summonState.removeProcessedSummons(summon ->
                belongsToCurrentStore(summon.ref(), currentStore) && processSummonTick(summon, now));
    }

    public BuffResult buffOwnedSummons(Ref<EntityStore> playerRef,
                                       PlayerData player,
                                       AbilityData ability) {
        if (summonState == null || buffRuntime == null || player == null) {
            return BuffResult.none();
        }
        List<ActiveSummon> summons = summonState.summonsForOwner(player.getPlayerId());
        if (summons.isEmpty()) {
            return buffResult(buffRuntime.apply(
                    summons,
                    ability,
                    null,
                    System.currentTimeMillis(),
                    null
            ));
        }

        Store<EntityStore> store = playerRef != null ? playerRef.getStore() : null;
        if (playerRef == null || store == null) {
            return BuffResult.none();
        }

        Vector3d origin = position(playerRef, store);
        if (origin == null) {
            return BuffResult.none();
        }

        long now = System.currentTimeMillis();
        SummonBuffRuntime.Result result = buffRuntime.apply(summons, ability, origin, now, new SummonBuffRuntime.Hooks() {
            @Override
            public Vector3d position(ActiveSummon summon) {
                return SummonControlHytaleAdapter.this.position(summon.ref(), store);
            }

            @Override
            public void applyBuffVisual(ActiveSummon summon) {
                support.applyEffectById(summon.ref(), store,
                        support.resolveImpactEffectId(player.getPlayerClass(), summon.styleId(), summon.ability()));
            }

            @Override
            public Ref<EntityStore> resolveTarget(ActiveSummon summon, long now) {
                return SummonControlHytaleAdapter.this.resolveSummonTarget(summon, store, now);
            }

            @Override
            public void attack(ActiveSummon summon, Ref<EntityStore> targetRef, long now) {
                support.attack(summon, player, targetRef, store, now);
            }
        });
        return buffResult(result);
    }

    public void moveSummonBesideTarget(ActiveSummon summon,
                                       Ref<EntityStore> targetRef,
                                       Store<EntityStore> store) {
        if (movementRuntime == null || summon == null || summon.ref() == null || store == null) {
            return;
        }
        Vector3d targetPosition = position(targetRef, store);
        if (targetPosition == null) {
            return;
        }

        NPCEntity npc = store.getComponent(summon.ref(), NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }

        Vector3d destination = movementRuntime.besideTargetDestination(targetPosition, position(summon.ownerRef(), store));
        if (destination != null) {
            npc.moveTo(summon.ref(), destination.x, destination.y, destination.z, store);
        }
    }

    private boolean processSummonTick(ActiveSummon summon, long now) {
        return tickRuntime.process(summon, now, new SummonTickRuntime.Hooks() {
            @Override
            public boolean despawn(ActiveSummon summon) {
                return lifecycleAdapter.despawnSummon(summon);
            }

            @Override
            public boolean hasStore(ActiveSummon summon) {
                return summon.ref() != null && summon.ref().getStore() != null;
            }

            @Override
            public PlayerData owner(String ownerPlayerId) {
                return support.owner(ownerPlayerId);
            }

            @Override
            public void awaken(ActiveSummon summon, long now) {
                awakenSummon(summon, summon.ref().getStore(), now);
            }

            @Override
            public Ref<EntityStore> resolveTarget(ActiveSummon summon, long now) {
                return SummonControlHytaleAdapter.this.resolveSummonTarget(summon, summon.ref().getStore(), now);
            }

            @Override
            public void moveTowardOwner(ActiveSummon summon) {
                SummonControlHytaleAdapter.this.moveSummonTowardOwner(summon, summon.ref().getStore());
            }

            @Override
            public Vector3d position(Ref<EntityStore> ref) {
                return SummonControlHytaleAdapter.this.position(ref, summon.ref().getStore());
            }

            @Override
            public void moveTowardTarget(ActiveSummon summon, Ref<EntityStore> targetRef, double desiredRange) {
                SummonControlHytaleAdapter.this.moveSummonTowardTarget(summon, targetRef, summon.ref().getStore(), desiredRange);
            }

            @Override
            public void moveAwayFromTarget(ActiveSummon summon, Ref<EntityStore> targetRef, double desiredDistance) {
                SummonControlHytaleAdapter.this.moveSummonAwayFromTarget(summon, targetRef, summon.ref().getStore(), desiredDistance);
            }

            @Override
            public void attack(ActiveSummon summon, PlayerData owner, Ref<EntityStore> targetRef, long now) {
                support.attack(summon, owner, targetRef, summon.ref().getStore(), now);
            }
        });
    }

    private void moveSummonTowardOwner(ActiveSummon summon, Store<EntityStore> store) {
        Vector3d summonPosition = position(summon.ref(), store);
        Vector3d ownerPosition = position(summon.ownerRef(), store);
        if (summonPosition == null || ownerPosition == null || movementRuntime == null) {
            return;
        }

        NPCEntity npc = store.getComponent(summon.ref(), NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }

        Vector3d destination = movementRuntime.ownerFollowDestination(summonPosition, ownerPosition);
        if (destination != null) {
            npc.moveTo(summon.ref(), destination.x, destination.y, destination.z, store);
        }
    }

    private void moveSummonTowardTarget(ActiveSummon summon,
                                        Ref<EntityStore> targetRef,
                                        Store<EntityStore> store,
                                        double desiredRange) {
        Vector3d summonPosition = position(summon.ref(), store);
        Vector3d targetPosition = position(targetRef, store);
        if (summonPosition == null || targetPosition == null || movementRuntime == null) {
            return;
        }

        NPCEntity npc = store.getComponent(summon.ref(), NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }

        Vector3d destination = movementRuntime.targetApproachDestination(summonPosition, targetPosition, desiredRange);
        if (destination != null) {
            npc.moveTo(summon.ref(), destination.x, destination.y, destination.z, store);
        }
    }

    private void moveSummonAwayFromTarget(ActiveSummon summon,
                                          Ref<EntityStore> targetRef,
                                          Store<EntityStore> store,
                                          double desiredDistance) {
        Vector3d summonPosition = position(summon.ref(), store);
        Vector3d targetPosition = position(targetRef, store);
        if (summonPosition == null || targetPosition == null || movementRuntime == null) {
            return;
        }

        NPCEntity npc = store.getComponent(summon.ref(), NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }

        Vector3d destination = movementRuntime.targetRetreatDestination(summonPosition, targetPosition, desiredDistance);
        if (destination != null) {
            npc.moveTo(summon.ref(), destination.x, destination.y, destination.z, store);
        }
    }

    private void awakenSummon(ActiveSummon summon,
                              Store<EntityStore> store,
                              long now) {
        summon.awaken(now);
        support.applyEffectById(summon.ref(), store,
                support.resolveImpactEffectId(summon.classId(), summon.styleId(), summon.ability()));
    }

    private Ref<EntityStore> resolveSummonTarget(ActiveSummon summon,
                                                 Store<EntityStore> store,
                                                 long now) {
        if (targetRuntime == null) {
            return null;
        }
        return targetRuntime.resolveTarget(summon, now, new SummonTargetRuntime.Hooks() {
            @Override
            public Vector3d position(Ref<EntityStore> ref) {
                return SummonControlHytaleAdapter.this.position(ref, store);
            }

            @Override
            public boolean isValidTarget(Ref<EntityStore> targetRef, Vector3d anchor, double radius) {
                return isValidNpcTarget(targetRef, store, anchor, radius);
            }

            @Override
            public Ref<EntityStore> findNearest(Vector3d anchor, double radius) {
                return findNearestNpc(store, anchor, radius);
            }
        });
    }

    private boolean isValidNpcTarget(Ref<EntityStore> targetRef,
                                     Store<EntityStore> store,
                                     Vector3d anchor,
                                     double radius) {
        if (targetRef == null || !targetRef.isValid() || anchor == null || store == null) {
            return false;
        }

        NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
        if (npc == null || npc.isDespawning() || support.isFriendlyOwned(targetRef, store)) {
            return false;
        }

        if (store.getComponent(targetRef, DeathComponent.getComponentType()) != null) {
            return false;
        }

        Vector3d targetPosition = position(targetRef, store);
        return targetPosition != null && distance(anchor, targetPosition) <= radius;
    }

    private Ref<EntityStore> findNearestNpc(Store<EntityStore> store, Vector3d center, double radius) {
        if (store == null || center == null) {
            return null;
        }

        List<NearbyTargetCandidate> candidates = new ArrayList<>();
        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid()) {
                    continue;
                }

                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning() || support.isFriendlyOwned(ref, store)) {
                    continue;
                }

                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }

                TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
                if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                    continue;
                }

                double candidateDistance = distance(center, transform.getTransform().getPosition());
                if (candidateDistance <= radius) {
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

    private static BuffResult buffResult(SummonBuffRuntime.Result result) {
        return result == null
                ? BuffResult.none()
                : new BuffResult(result.buffed(), result.summary());
    }

    public interface Support {
        PlayerData owner(String ownerPlayerId);

        boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        String resolveImpactEffectId(String classId, String styleId, AbilityData ability);

        void attack(ActiveSummon summon,
                    PlayerData owner,
                    Ref<EntityStore> targetRef,
                    Store<EntityStore> store,
                    long now);

        boolean isFriendlyOwned(Ref<EntityStore> ref, Store<EntityStore> store);
    }

    public record BuffResult(int buffed, String summary) {
        public BuffResult {
            summary = summary == null ? "" : summary;
        }

        public static BuffResult none() {
            return new BuffResult(0, "");
        }
    }

    private record NearbyTargetCandidate(Ref<EntityStore> ref, double distance) {
    }
}
