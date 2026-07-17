package com.motm.system;

import com.hypixel.hytale.component.ArchetypeChunk;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.EntityEventSystem;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.CraftRecipeEvent;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.MenteesMod;

/** Routes native post-craft ECS events into MOTM runtime perk handling. */
public final class MotmCraftRecipeEventSystem extends EntityEventSystem<EntityStore, CraftRecipeEvent.Post> {

    private final MenteesMod mod;

    public MotmCraftRecipeEventSystem(MenteesMod mod) {
        super(CraftRecipeEvent.Post.class);
        this.mod = mod;
    }

    @Override
    public Query<EntityStore> getQuery() {
        return Query.any();
    }

    @Override
    public void handle(int entityIndex,
                       ArchetypeChunk<EntityStore> chunk,
                       Store<EntityStore> store,
                       CommandBuffer<EntityStore> commandBuffer,
                       CraftRecipeEvent.Post event) {
        if (mod == null || chunk == null || store == null || event == null) {
            return;
        }
        Ref<EntityStore> playerRef = chunk.getReferenceTo(entityIndex);
        if (playerRef == null || !playerRef.isValid()) {
            return;
        }
        Player player = store.getComponent(playerRef, Player.getComponentType());
        if (player == null) {
            return;
        }
        mod.handlePlayerCraft(event.getCraftedRecipe(), event.getQuantity(), player);
    }
}
