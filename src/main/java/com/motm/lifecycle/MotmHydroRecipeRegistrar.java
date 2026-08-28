package com.motm.lifecycle;

import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonInt32;

import java.lang.reflect.Field;
import java.util.List;
import java.util.logging.Logger;

/**
 * Owns native Hydro recipe registration and the reflection needed to set ids on
 * Hytale recipe assets.
 */
public final class MotmHydroRecipeRegistrar {

    public void registerLightWaterskinRecipe(Logger log,
                                             String recipeId,
                                             String[] containerItemIds,
                                             String containerMetadataKey,
                                             String containerTierMetadataKey,
                                             int inputCount) {
        try {
            if (CraftingRecipe.getAssetMap().getAsset(recipeId) != null) {
                log.info("[MOTM] Native Hydro waterskin recipe already registered.");
                return;
            }

            CraftingRecipe recipe = createHydroWaterskinRecipe(
                    recipeId,
                    containerItemIds,
                    containerMetadataKey,
                    containerTierMetadataKey,
                    inputCount
            );
            CraftingRecipe.getAssetStore().loadAssets("MOTM:MOTM", List.of(recipe));

            if (CraftingRecipe.getAssetMap().getAsset(recipeId) != null) {
                log.info("[MOTM] Registered native Hydro waterskin fieldcraft recipe.");
            } else {
                log.warning("[MOTM] Hydro waterskin recipe load finished, but the recipe is not visible in the asset map.");
            }
        } catch (Exception e) {
            log.warning("[MOTM] Failed to register native Hydro waterskin recipe: " + e.getMessage());
        }
    }

    private CraftingRecipe createHydroWaterskinRecipe(String recipeId,
                                                      String[] containerItemIds,
                                                      String containerMetadataKey,
                                                      String containerTierMetadataKey,
                                                      int inputCount) throws ReflectiveOperationException {
        MaterialQuantity input = new MaterialQuantity(
                containerItemIds[0],
                null,
                null,
                inputCount,
                null
        );
        MaterialQuantity primaryOutput = new MaterialQuantity(
                containerItemIds[0],
                null,
                null,
                1,
                createHydroContainerMetadata(containerItemIds, containerMetadataKey, containerTierMetadataKey, 0)
        );
        BenchRequirement fieldcraft = new BenchRequirement(
                BenchType.Crafting,
                CraftingRecipe.FIELDCRAFT_REQUIREMENT,
                null,
                0,
                null
        );

        CraftingRecipe recipe = new CraftingRecipe(
                new MaterialQuantity[]{input},
                primaryOutput,
                MaterialQuantity.EMPTY_ARRAY,
                1,
                new BenchRequirement[]{fieldcraft},
                0f,
                false,
                0
        );
        setCraftingRecipeId(recipe, recipeId);
        return recipe;
    }

    private BsonDocument createHydroContainerMetadata(String[] containerItemIds,
                                                      String containerMetadataKey,
                                                      String containerTierMetadataKey,
                                                      int tier) {
        int clampedTier = Math.max(0, Math.min(tier, containerItemIds.length - 1));
        BsonDocument metadata = new BsonDocument();
        metadata.put(containerMetadataKey, BsonBoolean.TRUE);
        metadata.put(containerTierMetadataKey, new BsonInt32(clampedTier));
        return metadata;
    }

    private void setCraftingRecipeId(CraftingRecipe recipe, String recipeId) throws ReflectiveOperationException {
        Field idField = CraftingRecipe.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(recipe, recipeId);
    }
}
