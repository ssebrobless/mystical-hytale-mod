package com.motm.runtime.ability.terrain;

import com.hypixel.hytale.component.Ref;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

public final class TerrainActivationRuntime {

    public ActiveMovingTerrainTrail createMovingTrail(String reason,
                                                      World world,
                                                      Ref<EntityStore> ownerRef,
                                                      long expireAtMillis,
                                                      long nextPlaceAtMillis,
                                                      String... blockIds) {
        if (reason == null || reason.isBlank()
                || ownerRef == null || !ownerRef.isValid()
                || blockIds == null || blockIds.length == 0) {
            return null;
        }
        return new ActiveMovingTerrainTrail(
                reason,
                world,
                ownerRef,
                List.of(blockIds),
                expireAtMillis,
                nextPlaceAtMillis
        );
    }

    public ActiveLapidaryGem createLapidaryGem(String ownerPlayerId,
                                               Ref<EntityStore> gemRef,
                                               Vector3d center,
                                               double currentHp,
                                               double maxHp,
                                               long expireAtMillis,
                                               String label) {
        if (ownerPlayerId == null || gemRef == null || !gemRef.isValid()
                || center == null || maxHp <= 0.0 || currentHp <= 0.0) {
            return null;
        }
        return new ActiveLapidaryGem(
                ownerPlayerId,
                gemRef,
                center,
                currentHp,
                maxHp,
                expireAtMillis,
                label
        );
    }

    public ActiveStackingColumn createStackingColumn(String reason,
                                                     World world,
                                                     Vector3i anchor,
                                                     int blockTypeId,
                                                     int height,
                                                     long expireAtMillis,
                                                     long nextStageAtMillis) {
        if (reason == null || reason.isBlank() || anchor == null || blockTypeId <= 0) {
            return null;
        }
        return new ActiveStackingColumn(
                reason,
                world,
                anchor,
                blockTypeId,
                Math.max(1, height),
                expireAtMillis,
                nextStageAtMillis
        );
    }
}
