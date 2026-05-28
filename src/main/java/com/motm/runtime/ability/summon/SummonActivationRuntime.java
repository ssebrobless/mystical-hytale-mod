package com.motm.runtime.ability.summon;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.AbilityData;

public final class SummonActivationRuntime {

    public ActiveSummon create(String ownerPlayerId,
                               Ref<EntityStore> summonRef,
                               Ref<EntityStore> ownerRef,
                               String classId,
                               String styleId,
                               AbilityData ability,
                               long now,
                               long expireAtMillis,
                               double rawBaseDamage) {
        if (ownerPlayerId == null || summonRef == null || !summonRef.isValid()
                || ownerRef == null || !ownerRef.isValid() || ability == null) {
            return null;
        }

        SummonRuntimeSpec spec = SummonRuntimeSpecs.resolve(ability);
        long hatchAtMillis = now + spec.hatchDelayMillis();
        double baseDamage = SummonRuntimeSpecs.baseDamage(rawBaseDamage, spec);

        return new ActiveSummon(
                ownerPlayerId,
                summonRef,
                ownerRef,
                classId,
                styleId,
                ability,
                spec,
                hatchAtMillis,
                expireAtMillis,
                now,
                now,
                0L,
                baseDamage,
                null,
                0L,
                spec.hatchDelayMillis() <= 0L
        );
    }
}
