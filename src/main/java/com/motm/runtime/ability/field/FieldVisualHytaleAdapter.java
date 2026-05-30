package com.motm.runtime.ability.field;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import org.joml.Vector3f;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.model.AbilityData;
import com.motm.runtime.state.VisualProxyRuntimeState;
import com.motm.util.HytaleAssetResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FieldVisualHytaleAdapter {
    private final VisualProxyRuntimeState visualProxyState;
    private final EffectApplier effectApplier;

    public FieldVisualHytaleAdapter(VisualProxyRuntimeState visualProxyState,
                                    EffectApplier effectApplier) {
        this.visualProxyState = visualProxyState;
        this.effectApplier = effectApplier;
    }

    public FieldVisualRuntime spawn(Player runtimePlayer,
                                    String classId,
                                    String styleId,
                                    AbilityData ability,
                                    Vector3d center,
                                    Vector3d lineDirection,
                                    double halfWidth,
                                    long activateAtMillis,
                                    long expireAtMillis,
                                    String effectId) {
        if (runtimePlayer == null || center == null || effectId == null || effectId.isBlank()) {
            return FieldVisualRuntime.none();
        }

        World world = runtimePlayer.getWorld();
        if (world == null) {
            return FieldVisualRuntime.none();
        }

        List<Vector3d> positions = buildFieldVisualPositions(center, lineDirection, ability, halfWidth);
        if (positions.isEmpty()) {
            return FieldVisualRuntime.none();
        }

        List<Ref<EntityStore>> refs = new ArrayList<>();
        String roleId = HytaleAssetResolver.resolveFieldRoleId(classId, styleId, ability);
        float despawnTimeSeconds = (float) Math.max(1.0, ((expireAtMillis - System.currentTimeMillis()) / 1000.0) + 0.75);
        for (Vector3d position : positions) {
            NPCEntity proxy = new NPCEntity(world);
            proxy.setRoleName(roleId);
            proxy.setDespawnTime(despawnTimeSeconds);
            world.spawnEntity(proxy, new Vector3d(position), new com.hypixel.hytale.math.vector.Rotation3f(0f, 0f, 0f));

            Ref<EntityStore> proxyRef = proxy.getReference();
            if (proxyRef != null && proxyRef.isValid() && proxyRef.getStore() != null) {
                visualProxyState.add(proxyRef);
                refs.add(proxyRef);
                applyEffect(proxyRef, proxyRef.getStore(), effectId);
            }
        }

        if (refs.isEmpty()) {
            return FieldVisualRuntime.none();
        }

        return new FieldVisualRuntime(List.copyOf(refs), effectId, activateAtMillis);
    }

    public void sync(ActiveField field, long now) {
        if (field == null || field.visualRefs() == null || field.visualRefs().isEmpty()) {
            return;
        }

        List<Vector3d> positions = buildFieldVisualPositions(
                field.center(),
                field.lineDirection(),
                field.ability(),
                field.halfWidth()
        );
        int limit = Math.min(positions.size(), field.visualRefs().size());
        for (int index = 0; index < limit; index++) {
            Ref<EntityStore> visualRef = field.visualRefs().get(index);
            if (visualRef == null || !visualRef.isValid()) {
                continue;
            }

            Store<EntityStore> visualStore = visualRef.getStore();
            if (visualStore == null) {
                continue;
            }

            NPCEntity npc = visualStore.getComponent(visualRef, NPCEntity.getComponentType());
            if (npc != null) {
                Vector3d position = positions.get(index);
                npc.moveTo(visualRef, position.x, position.y, position.z, visualStore);
            }
        }

        refresh(field, now);
    }

    public void refresh(ActiveField field, long now) {
        if (field == null
                || field.visualRefs() == null
                || field.visualRefs().isEmpty()
                || field.loopEffectId() == null
                || field.loopEffectId().isBlank()
                || now < field.nextVisualRefreshAtMillis()) {
            return;
        }

        boolean refreshed = false;
        for (Ref<EntityStore> visualRef : field.visualRefs()) {
            if (visualRef == null || !visualRef.isValid()) {
                continue;
            }

            Store<EntityStore> visualStore = visualRef.getStore();
            if (visualStore == null) {
                continue;
            }

            refreshed |= applyEffect(visualRef, visualStore, field.loopEffectId());
        }

        if (refreshed) {
            field.scheduleNextVisualRefresh(now);
        }
    }

    public void despawn(ActiveField field) {
        if (field == null || field.visualRefs() == null || field.visualRefs().isEmpty()) {
            return;
        }

        visualProxyState.despawnAll(field.visualRefs());
    }

    private boolean applyEffect(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
        return effectApplier != null && effectApplier.apply(ref, store, effectId);
    }

    static List<Vector3d> buildFieldVisualPositions(Vector3d center,
                                                    Vector3d lineDirection,
                                                    AbilityData ability,
                                                    double halfWidth) {
        if (center == null || ability == null) {
            return List.of();
        }

        List<Vector3d> positions = new ArrayList<>();
        positions.add(fieldVisualCenter(center, ability));
        String castType = lower(ability.getCastType());
        if ("barrier".equals(castType) && lineDirection != null && lineDirection.isFinite()) {
            double span = Math.max(2.0, Math.min(Math.max(halfWidth, 0.0), 7.0));
            Vector3d normalized = normalize(lineDirection);
            for (double offset = -span; offset <= span + 0.001; offset += Math.max(2.25, span / 2.0)) {
                if (Math.abs(offset) < 0.3) {
                    continue;
                }
                positions.add(com.motm.util.MotmVectors.addScaled(center, normalized, offset));
            }
            return positions;
        }

        if (!"ground_zone".equals(castType) && !"support_zone".equals(castType)) {
            return positions;
        }

        if (isQuakeGroundVisual(ability)) {
            return positions;
        }

        return buildAreaVisualPositions(center, ability);
    }

    private static Vector3d fieldVisualCenter(Vector3d center, AbilityData ability) {
        Vector3d resolved = new Vector3d(center);
        if ("sinkhole".equals(lower(ability == null ? null : ability.getId()))) {
            resolved.y -= 1.0;
        }
        return resolved;
    }

    private static boolean isQuakeGroundVisual(AbilityData ability) {
        String abilityId = lower(ability == null ? null : ability.getId());
        return "aftershock".equals(abilityId) || "sinkhole".equals(abilityId);
    }

    private static List<Vector3d> buildAreaVisualPositions(Vector3d center, AbilityData ability) {
        if (center == null || ability == null) {
            return List.of();
        }

        List<Vector3d> positions = new ArrayList<>();
        positions.add(new Vector3d(center));
        double radius = ability.getRadius() > 0 ? ability.getRadius() : FieldRuntimeSpecs.DEFAULT_AREA_RADIUS;
        double ringRadius = Math.max(1.8, Math.min(radius * 0.62, 5.5));
        positions.add(new Vector3d(center).add(ringRadius, 0.0, 0.0));
        positions.add(new Vector3d(center).add(-ringRadius, 0.0, 0.0));
        positions.add(new Vector3d(center).add(0.0, 0.0, ringRadius));
        positions.add(new Vector3d(center).add(0.0, 0.0, -ringRadius));
        if (radius >= 4.5) {
            double diagonal = ringRadius * 0.72;
            positions.add(new Vector3d(center).add(diagonal, 0.0, diagonal));
            positions.add(new Vector3d(center).add(-diagonal, 0.0, diagonal));
            positions.add(new Vector3d(center).add(diagonal, 0.0, -diagonal));
            positions.add(new Vector3d(center).add(-diagonal, 0.0, -diagonal));
        }
        return positions;
    }

    private static Vector3d normalize(Vector3d vector) {
        Vector3d normalized = vector == null ? new Vector3d(0.0, 0.0, 1.0) : new Vector3d(vector);
        if (normalized.length() < 0.0001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        normalized.normalize();
        return normalized;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    @FunctionalInterface
    public interface EffectApplier {
        boolean apply(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);
    }
}
