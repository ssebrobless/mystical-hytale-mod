package com.motm.resource;

import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonValue;

import java.util.Set;

public final class HydroContainerItems {
    public static final String CONTAINER_METADATA_KEY = "motm_hydro_container";
    public static final String CONTAINER_TIER_METADATA_KEY = "motm_hydro_container_tier";
    public static final String LIGHT_WATERSKIN_RECIPE_ID = "MOTM_Hydro_Waterskin_Light";
    public static final int LIGHT_WATERSKIN_INPUT_COUNT = 2;
    private static final String[] CONTAINER_ITEM_IDS = {
            "Ingredient_Hide_Light",
            "Ingredient_Hide_Soft",
            "Ingredient_Hide_Medium",
            "Ingredient_Hide_Heavy",
            "Ingredient_Hide_Dark"
    };
    private static final Set<String> CONTAINER_ID_SET = Set.of(CONTAINER_ITEM_IDS);

    private HydroContainerItems() {
    }

    public static String[] itemIds() {
        return CONTAINER_ITEM_IDS.clone();
    }

    public static boolean isContainerItem(ItemStack stack) {
        if (stack == null || stack.getItemId() == null || !isContainerItemId(stack.getItemId())) {
            return false;
        }
        BsonDocument metadata = stack.getMetadata();
        if (metadata == null || !metadata.containsKey(CONTAINER_METADATA_KEY)) {
            return false;
        }
        BsonValue value = metadata.get(CONTAINER_METADATA_KEY);
        return value != null && value.isBoolean() && value.asBoolean().getValue();
    }

    public static boolean isContainerItemId(String itemId) {
        return itemId != null && CONTAINER_ID_SET.contains(itemId);
    }

    public static String itemId(int tier) {
        return CONTAINER_ITEM_IDS[clampTier(tier)];
    }

    public static ItemStack createStack(int tier) {
        int clampedTier = clampTier(tier);
        return new ItemStack(CONTAINER_ITEM_IDS[clampedTier])
                .withMetadata(CONTAINER_METADATA_KEY, BsonBoolean.TRUE)
                .withMetadata(CONTAINER_TIER_METADATA_KEY, new BsonInt32(clampedTier));
    }

    public static boolean isTier(ItemStack stack, int tier) {
        if (!isContainerItem(stack)) {
            return false;
        }
        BsonDocument metadata = stack.getMetadata();
        if (metadata == null || !metadata.containsKey(CONTAINER_TIER_METADATA_KEY)) {
            return false;
        }
        BsonValue value = metadata.get(CONTAINER_TIER_METADATA_KEY);
        return value != null && value.isInt32() && value.asInt32().getValue() == clampTier(tier);
    }

    public static boolean hasContainer(CombinedItemContainer inventory) {
        return inventory != null && inventory.countItemStacks(HydroContainerItems::isContainerItem) > 0;
    }

    public static int detectTier(CombinedItemContainer inventory) {
        if (inventory == null) {
            return 0;
        }

        final int[] detectedTier = {0};
        inventory.forEach((slot, stack) -> {
            if (!isContainerItem(stack)) {
                return;
            }
            BsonDocument metadata = stack.getMetadata();
            if (metadata == null) {
                return;
            }
            BsonValue value = metadata.get(CONTAINER_TIER_METADATA_KEY);
            if (value != null && value.isInt32()) {
                detectedTier[0] = clampTier(value.asInt32().getValue());
            }
        });
        return detectedTier[0];
    }

    public static int clampTier(int tier) {
        return Math.max(0, Math.min(tier, CONTAINER_ITEM_IDS.length - 1));
    }
}
