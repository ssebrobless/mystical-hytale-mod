package com.motm.runtime.ability.field;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;

import java.util.List;

public final class FieldTickRuntime {

    public boolean process(ActiveField field, long now, Hooks hooks) {
        if (field == null || hooks == null) {
            return true;
        }
        if (field.ownerRef() == null || !field.ownerRef().isValid()) {
            hooks.releaseSinkhole(field);
            hooks.despawnVisual(field);
            return true;
        }
        if (!hooks.hasOwnerStore(field)) {
            hooks.releaseSinkhole(field);
            hooks.despawnVisual(field);
            return true;
        }

        hooks.syncFollowOwnerAnchor(field);

        if (now >= field.expireAtMillis()) {
            hooks.clearOwnerMobility(field);
            hooks.restoreTemporaryTerrain(field);
            hooks.releaseSinkhole(field);
            hooks.despawnVisual(field);
            return true;
        }

        hooks.applyOwnerMobility(field);

        if (now < field.activateAtMillis()) {
            hooks.refreshVisual(field, now);
            return false;
        }

        if (hooks.isSinkhole(field.ability())) {
            hooks.engageSinkhole(field);
        }

        hooks.syncVisual(field, now);
        if (now < field.nextPulseAtMillis()) {
            return false;
        }

        PlayerData player = hooks.player(field.ownerPlayerId());
        if (player == null) {
            hooks.releaseSinkhole(field);
            hooks.despawnVisual(field);
            return true;
        }

        List<Ref<EntityStore>> targets = hooks.collectTargets(field);
        if (targets != null && !targets.isEmpty()) {
            hooks.applyPulse(field, player, targets);
        }
        hooks.applySupportPulse(field, player);
        hooks.applySinkholeSuffocationPulse(field);
        field.scheduleNextPulse(now);
        return false;
    }

    public interface Hooks {
        boolean hasOwnerStore(ActiveField field);

        void releaseSinkhole(ActiveField field);

        void despawnVisual(ActiveField field);

        void syncFollowOwnerAnchor(ActiveField field);

        void clearOwnerMobility(ActiveField field);

        void restoreTemporaryTerrain(ActiveField field);

        void applyOwnerMobility(ActiveField field);

        void refreshVisual(ActiveField field, long now);

        boolean isSinkhole(AbilityData ability);

        void engageSinkhole(ActiveField field);

        void syncVisual(ActiveField field, long now);

        PlayerData player(String ownerPlayerId);

        List<Ref<EntityStore>> collectTargets(ActiveField field);

        void applyPulse(ActiveField field, PlayerData player, List<Ref<EntityStore>> targets);

        void applySupportPulse(ActiveField field, PlayerData player);

        void applySinkholeSuffocationPulse(ActiveField field);
    }
}
