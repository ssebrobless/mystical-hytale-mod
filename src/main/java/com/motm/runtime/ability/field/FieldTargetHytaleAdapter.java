package com.motm.runtime.ability.field;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class FieldTargetHytaleAdapter {
    private final Set<String> ignoredNpcRoleNames;
    private final Support support;

    public FieldTargetHytaleAdapter(Set<String> ignoredNpcRoleNames, Support support) {
        this.ignoredNpcRoleNames = ignoredNpcRoleNames == null
                ? Set.of()
                : ignoredNpcRoleNames.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.support = support;
    }

    public List<Ref<EntityStore>> collectTargets(ActiveField field, Store<EntityStore> store) {
        if (field == null || field.ability() == null || store == null) {
            return List.of();
        }

        LinkedHashSet<Ref<EntityStore>> targets = new LinkedHashSet<>();
        String castType = lower(field.ability().getCastType());

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                TransformComponent transform = validNpcTransform(chunk, entityIndex);
                if (ref == null || !ref.isValid() || transform == null
                        || (support != null && support.isNonTargetableProxy(ref))) {
                    continue;
                }

                Vector3d position = transform.getTransform().getPosition();
                if (field.ability().isGroundTargetsOnly()
                        && (support == null || !support.isTargetGrounded(ref, store))) {
                    continue;
                }

                if ("barrier".equals(castType)) {
                    if (isInsideBarrier(field, position)) {
                        targets.add(ref);
                    }
                    continue;
                }

                if (distance(field.center(), position) <= field.radius()) {
                    targets.add(ref);
                }
            }
        });

        return List.copyOf(targets);
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
        return ignoredNpcRoleNames.contains(npc.getRoleName().toLowerCase(Locale.ROOT));
    }

    private static boolean isInsideBarrier(ActiveField field, Vector3d position) {
        Vector3d relative = subtract(position, field.center());
        double lateral = Math.abs(dot(relative, field.lineDirection()));
        double depth = Math.abs(dot(relative, field.forwardDirection()));
        return lateral <= field.halfWidth() && depth <= field.thickness();
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

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public interface Support {
        boolean isTargetGrounded(Ref<EntityStore> targetRef, Store<EntityStore> store);

        /**
         * Visual proxies wear vanilla roles (Spark_Living) since the role
         * migration, so identity via the proxy registry is the only reliable
         * exclusion (2026-07-18 crash: dismissed sandstorm proxy pulsed by the
         * dust devil trail fields while its removal was queued).
         */
        boolean isNonTargetableProxy(Ref<EntityStore> targetRef);
    }
}
