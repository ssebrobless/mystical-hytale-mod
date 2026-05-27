package com.motm.runtime.task;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Resolves live style-test targets from server truth.
 */
public final class StyleTestTargetResolver {

    public Ref<EntityStore> findNearestNpc(Store<EntityStore> store, Player player, double radius) {
        if (store == null || radius <= 0.0) {
            return null;
        }
        Vector3d playerPosition = playerPosition(player);
        if (playerPosition == null) {
            return null;
        }

        AtomicReference<Ref<EntityStore>> nearest = new AtomicReference<>();
        final double[] bestDistance = {Double.MAX_VALUE};

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid()) {
                    continue;
                }

                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning()) {
                    continue;
                }
                if ("motm_summon".equalsIgnoreCase(npc.getRoleName())) {
                    continue;
                }
                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }

                Vector3d candidatePosition = entityPosition(store, ref);
                if (candidatePosition == null) {
                    continue;
                }

                double candidateDistance = distance(playerPosition, candidatePosition);
                if (candidateDistance <= radius && candidateDistance < bestDistance[0]) {
                    bestDistance[0] = candidateDistance;
                    nearest.set(ref);
                }
            }
        });

        return nearest.get();
    }

    public Vector3i resolveTargetBlock(Store<EntityStore> store, Player player, Ref<EntityStore> targetRef) {
        Vector3d targetPosition = entityPosition(store, targetRef);
        if (targetPosition != null) {
            return floorBlock(targetPosition);
        }

        Vector3d playerPosition = playerPosition(player);
        if (playerPosition == null) {
            return null;
        }

        return floorBlock(playerPosition);
    }

    private Vector3d playerPosition(Player player) {
        if (player == null) {
            return null;
        }
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid() || ref.getStore() == null) {
            return null;
        }
        return entityPosition(ref.getStore(), ref);
    }

    private Vector3d entityPosition(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return null;
        }
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }
        return transform.getTransform().getPosition();
    }

    private Vector3i floorBlock(Vector3d position) {
        return new Vector3i(
                (int) Math.floor(position.x),
                (int) Math.floor(position.y),
                (int) Math.floor(position.z)
        );
    }

    private double distance(Vector3d left, Vector3d right) {
        if (left == null || right == null) {
            return Double.MAX_VALUE;
        }
        double dx = left.x - right.x;
        double dy = left.y - right.y;
        double dz = left.z - right.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
