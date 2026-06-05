package com.motm.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.SystemGroup;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageEventSystem;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageModule;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.MenteesMod;

public class MotmSummonFriendlyDamageFilterSystem extends DamageEventSystem {
    private final MenteesMod mod;
    private final Query<EntityStore> query = NPCEntity.getComponentType();

    public MotmSummonFriendlyDamageFilterSystem(MenteesMod mod) {
        this.mod = mod;
    }

    @Override
    public SystemGroup<EntityStore> getGroup() {
        return DamageModule.get().getFilterDamageGroup();
    }

    @Override
    public Query<EntityStore> getQuery() {
        return query;
    }

    @Override
    public void handle(int entityIndex,
                       ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer,
                       Damage damage) {
        if (damage == null || damage.isCancelled()) {
            return;
        }
        if (!(damage.getSource() instanceof Damage.EntitySource source)) {
            return;
        }

        Ref<EntityStore> sourceRef = source.getRef();
        Ref<EntityStore> targetRef = chunk.getReferenceTo(entityIndex);
        if (mod.getGameplayPlaybackManager().shouldCancelFriendlySummonDamage(sourceRef, targetRef, store)) {
            damage.setCancelled(true);
        }
    }
}
