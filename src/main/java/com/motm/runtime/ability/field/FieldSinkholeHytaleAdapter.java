package com.motm.runtime.ability.field;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.util.AbilityPresentation;
import com.motm.util.MotmEntityLiveness;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class FieldSinkholeHytaleAdapter {
    private final FieldRuntimeState fieldState;
    private final FieldTargetHytaleAdapter targetAdapter;
    private final Support support;

    public FieldSinkholeHytaleAdapter(FieldRuntimeState fieldState,
                                      FieldTargetHytaleAdapter targetAdapter,
                                      Support support) {
        this.fieldState = fieldState;
        this.targetAdapter = targetAdapter;
        this.support = support;
    }

    public void engage(ActiveField field, Store<EntityStore> store) {
        if (!isSinkhole(field != null ? field.ability() : null) || fieldState == null || support == null) {
            return;
        }

        if (fieldState.hasBurialEntry(field)) {
            return;
        }

        List<Ref<EntityStore>> caught = targetAdapter == null ? List.of() : targetAdapter.collectTargets(field, store);
        if (caught.isEmpty()) {
            support.placeSurfaceMarker(field, 1800L);
            fieldState.putBuriedVictims(field, List.of());
            support.logInfo("[MOTM] Sinkhole engaged: no targets in radius="
                    + AbilityPresentation.formatDecimal(FieldRuntimeSpecs.radius(field.ability()))
                    + " at center=" + field.center());
            return;
        }

        List<BuriedVictim> victims = new ArrayList<>();
        for (Ref<EntityStore> targetRef : caught) {
            if (targetRef == null || !targetRef.isValid()) {
                continue;
            }
            if (!MotmEntityLiveness.isLiveTarget(targetRef, store)) {
                continue;
            }

            support.applyEffect(targetRef, store, "MOTM_Terra_Sinkhole_Buried");
            support.applyTargetToken("root", targetRef, store, field.ownerRef(), field.ownerPlayerId(), field.ability());
            victims.add(new BuriedVictim(targetRef, null, field.expireAtMillis()));
        }

        support.placeSurfaceMarker(field, Math.max(1800L, field.expireAtMillis() - System.currentTimeMillis()));
        fieldState.putBuriedVictims(field, victims);
        support.logInfo("[MOTM] Sinkhole engaged: buried " + victims.size()
                + " target(s) at center=" + field.center());
    }

    public void applySuffocationPulse(ActiveField field, Store<EntityStore> store) {
        if (!isSinkhole(field != null ? field.ability() : null) || fieldState == null || support == null) {
            return;
        }

        double dotPercent = Math.max(0.0, field.ability().getDotPercentPerSecond());
        if (dotPercent <= 0.0) {
            return;
        }

        List<BuriedVictim> victims = fieldState.buriedVictims(field);
        if (victims.isEmpty()) {
            return;
        }
        for (BuriedVictim victim : victims) {
            if (victim.targetRef() != null && MotmEntityLiveness.isLiveTarget(victim.targetRef(), store)) {
                applySuffocationTick(victim.targetRef(), store, field, dotPercent);
            }
        }
    }

    public void release(ActiveField field) {
        if (!isSinkhole(field != null ? field.ability() : null) || fieldState == null || support == null) {
            return;
        }

        List<BuriedVictim> victims = fieldState.removeBuriedVictims(field);
        if (victims.isEmpty()) {
            return;
        }

        for (BuriedVictim victim : victims) {
            if (victim.targetRef() == null || !victim.targetRef().isValid() || victim.originalScale() == null) {
                continue;
            }
            support.logFine("[MOTM] Sinkhole scale restore skipped; EntityScaleComponent support is not enabled.");
        }
        support.logInfo("[MOTM] Sinkhole released: " + victims.size() + " target(s)");
    }

    public static boolean isSinkhole(AbilityData ability) {
        return ability != null
                && ("sinkhole".equalsIgnoreCase(ability.getId())
                || lower(ability.getTerrainEffect()).contains("sinkhole"));
    }

    private void applySuffocationTick(Ref<EntityStore> targetRef,
                                      Store<EntityStore> store,
                                      ActiveField field,
                                      double maxHpFraction) {
        if (!MotmEntityLiveness.isLiveTarget(targetRef, store) || maxHpFraction <= 0.0) {
            return;
        }

        EntityStatMap entityStatMap = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return;
        }

        EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null || health.getMax() <= 0.0) {
            return;
        }

        double resolvedDamage = health.getMax() * maxHpFraction;
        if (resolvedDamage <= 0.0) {
            return;
        }

        try {
            DamageCause cause = DamageCause.getAssetMap().getAsset("Suffocation");
            Damage damage = new Damage(new Damage.EntitySource(field.ownerRef()), cause, (float) resolvedDamage);
            DamageSystems.executeDamage(targetRef, store, damage);
            String entityId = support.resolveEntityId(targetRef, store);
            support.logInfo("[MOTM] Sinkhole suffocation tick: target="
                    + (entityId == null ? "<unknown>" : entityId)
                    + " damage=" + AbilityPresentation.formatDecimal(resolvedDamage));
        } catch (RuntimeException e) {
            support.logWarning("[MOTM] Sinkhole DoT failed: " + e.getMessage());
        }
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public interface Support {
        boolean applyEffect(Ref<EntityStore> ref, Store<EntityStore> store, String effectId);

        boolean applyTargetToken(String token,
                                 Ref<EntityStore> targetRef,
                                 Store<EntityStore> store,
                                 Ref<EntityStore> sourceRef,
                                 String sourcePlayerId,
                                 AbilityData ability);

        String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store);

        void placeSurfaceMarker(ActiveField field, long durationMillis);

        void logInfo(String message);

        void logFine(String message);

        void logWarning(String message);
    }
}
