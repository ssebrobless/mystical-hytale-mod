package com.motm.runtime.ability.projectile;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.LinkedHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class ProjectileHitHytaleAdapter {
    private final Set<String> ignoredNpcRoleNames;

    public ProjectileHitHytaleAdapter(Set<String> ignoredNpcRoleNames) {
        this.ignoredNpcRoleNames = ignoredNpcRoleNames == null
                ? Set.of()
                : ignoredNpcRoleNames.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    public Ref<EntityStore> resolveHit(ActiveProjectile projectile,
                                       Store<EntityStore> store,
                                       Vector3d from,
                                       Vector3d to) {
        if (projectile == null || store == null || from == null || to == null) {
            return null;
        }

        AtomicReference<Ref<EntityStore>> hit = new AtomicReference<>();
        final double[] bestDistance = {Double.MAX_VALUE};
        Vector3d segment = subtract(to, from);
        double segmentLengthSquared = Math.max(0.0001, dot(segment, segment));

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                TransformComponent transform = validNpcTransform(chunk, entityIndex);
                if (ref == null || !ref.isValid() || transform == null) {
                    continue;
                }

                Vector3d targetPosition = transform.getTransform().getPosition();
                double normalizedProjection = dot(subtract(targetPosition, from), segment) / segmentLengthSquared;
                double clampedProjection = clamp(normalizedProjection, 0.0, 1.0);
                Vector3d nearestPoint = from.clone().addScaled(segment, clampedProjection);
                double distanceToSegment = distance(nearestPoint, targetPosition);
                if (distanceToSegment > projectile.collisionRadius()) {
                    continue;
                }

                double alongSegment = distance(from, nearestPoint);
                if (alongSegment < bestDistance[0]) {
                    bestDistance[0] = alongSegment;
                    hit.set(ref);
                }
            }
        });

        return hit.get();
    }

    public List<Ref<EntityStore>> collectImpactTargets(ActiveProjectile projectile,
                                                       Store<EntityStore> store,
                                                       Vector3d impactPosition,
                                                       Ref<EntityStore> directHit) {
        if (projectile == null || store == null || impactPosition == null) {
            return List.of();
        }

        LinkedHashSet<Ref<EntityStore>> targets = new LinkedHashSet<>();
        if (directHit != null && directHit.isValid()) {
            targets.add(directHit);
        }

        double radius = projectile.impactRadius();
        if (radius <= 0.01) {
            if (!targets.isEmpty()) {
                return List.copyOf(targets);
            }
            Ref<EntityStore> splashHit = findNearestNpc(store, impactPosition, projectile.collisionRadius());
            return splashHit != null ? List.of(splashHit) : List.of();
        }

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                TransformComponent transform = validNpcTransform(chunk, entityIndex);
                if (ref == null || !ref.isValid() || transform == null) {
                    continue;
                }

                if (distance(impactPosition, transform.getTransform().getPosition()) <= radius) {
                    targets.add(ref);
                }
            }
        });

        return List.copyOf(targets);
    }

    public List<Ref<EntityStore>> collectTraversalTargets(ActiveProjectile projectile,
                                                          Store<EntityStore> store,
                                                          Vector3d from,
                                                          Vector3d to,
                                                          EntityIdResolver entityIdResolver) {
        if (projectile == null || store == null || from == null || to == null) {
            return List.of();
        }

        LinkedHashSet<Ref<EntityStore>> targets = new LinkedHashSet<>();
        Vector3d segment = subtract(to, from);
        double segmentLengthSquared = Math.max(0.0001, dot(segment, segment));

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                TransformComponent transform = validNpcTransform(chunk, entityIndex);
                if (ref == null || !ref.isValid() || transform == null) {
                    continue;
                }

                String entityId = entityIdResolver == null ? null : entityIdResolver.resolve(ref, store);
                if (entityId == null || projectile.hitEntityIds().contains(entityId)) {
                    continue;
                }

                Vector3d targetPosition = transform.getTransform().getPosition();
                double normalizedProjection = dot(subtract(targetPosition, from), segment) / segmentLengthSquared;
                double clampedProjection = clamp(normalizedProjection, 0.0, 1.0);
                Vector3d nearestPoint = from.clone().addScaled(segment, clampedProjection);
                if (distance(nearestPoint, targetPosition) <= projectile.collisionRadius()) {
                    targets.add(ref);
                }
            }
        });

        return List.copyOf(targets);
    }

    public List<Ref<EntityStore>> collectNearbyTargets(Store<EntityStore> store,
                                                       Vector3d center,
                                                       double radius,
                                                       int maxTargets) {
        if (store == null || center == null || radius <= 0.0) {
            return List.of();
        }

        List<NearbyTargetCandidate> candidates = new ArrayList<>();
        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                TransformComponent transform = validNpcTransform(chunk, entityIndex);
                if (ref == null || !ref.isValid() || transform == null) {
                    continue;
                }

                double candidateDistance = distance(center, transform.getTransform().getPosition());
                if (candidateDistance <= radius) {
                    candidates.add(new NearbyTargetCandidate(ref, candidateDistance));
                }
            }
        });

        candidates.sort((left, right) -> Double.compare(left.distance(), right.distance()));
        List<Ref<EntityStore>> targets = new ArrayList<>();
        for (NearbyTargetCandidate candidate : candidates) {
            targets.add(candidate.ref());
            if (maxTargets > 0 && targets.size() >= maxTargets) {
                break;
            }
        }
        return List.copyOf(targets);
    }

    private Ref<EntityStore> findNearestNpc(Store<EntityStore> store, Vector3d center, double radius) {
        AtomicReference<Ref<EntityStore>> nearest = new AtomicReference<>();
        final double[] bestDistance = {Double.MAX_VALUE};

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                TransformComponent transform = validNpcTransform(chunk, entityIndex);
                if (ref == null || !ref.isValid() || transform == null) {
                    continue;
                }

                double candidateDistance = distance(center, transform.getTransform().getPosition());
                if (candidateDistance <= radius && candidateDistance < bestDistance[0]) {
                    bestDistance[0] = candidateDistance;
                    nearest.set(ref);
                }
            }
        });

        return nearest.get();
    }

    private TransformComponent validNpcTransform(ArchetypeChunk<EntityStore> chunk, int entityIndex) {
        NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
        if (npc == null || npc.isDespawning() || isIgnoredNpc(npc)) {
            return null;
        }

        if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
            return null;
        }

        TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
            return null;
        }
        return transform;
    }

    private boolean isIgnoredNpc(NPCEntity npc) {
        if (npc == null || npc.getRoleName() == null) {
            return false;
        }
        String roleName = npc.getRoleName().toLowerCase(Locale.ROOT);
        return ignoredNpcRoleNames.contains(roleName);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static Vector3d subtract(Vector3d left, Vector3d right) {
        if (left == null || right == null) {
            return new Vector3d();
        }
        return new Vector3d(left.x - right.x, left.y - right.y, left.z - right.z);
    }

    private static double dot(Vector3d left, Vector3d right) {
        if (left == null || right == null) {
            return 0.0;
        }
        return (left.x * right.x) + (left.y * right.y) + (left.z * right.z);
    }

    private static double distance(Vector3d left, Vector3d right) {
        Vector3d delta = subtract(left, right);
        return Math.sqrt(dot(delta, delta));
    }

    public interface EntityIdResolver {
        String resolve(Ref<EntityStore> ref, Store<EntityStore> store);
    }

    private record NearbyTargetCandidate(Ref<EntityStore> ref, double distance) { }
}
