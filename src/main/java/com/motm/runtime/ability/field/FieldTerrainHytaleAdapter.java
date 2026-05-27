package com.motm.runtime.ability.field;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

public final class FieldTerrainHytaleAdapter {
    private final Support support;

    public FieldTerrainHytaleAdapter(Support support) {
        this.support = support;
    }

    public void restoreTemporaryTerrain(ActiveField field, Store<EntityStore> store) {
        if (field == null || field.ability() == null || store == null || store.getExternalData() == null
                || support == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }
        for (String reason : FieldRuntimeSpecs.terrainRestoreReasons(field.ability())) {
            support.restoreTemporarySelections(world, reason);
        }
    }

    @FunctionalInterface
    public interface Support {
        void restoreTemporarySelections(World world, String reason);
    }
}
