package com.motm.resource;

import java.util.Map;

public final class TerraInventoryResourcePolicy {
    private static final Map<String, ResourceDefinition> DEFINITIONS = Map.of(
            "stone_blocks", new ResourceDefinition(1,
                    "Rock_Stone",
                    "Rock_Slate",
                    "Rock_Shale",
                    "Rock_Calcite",
                    "Rock_Quartzite",
                    "Rock_Marble",
                    "Rock_Lime",
                    "Rock_Basalt",
                    "Rock_Volcanic"),
            "dirt_blocks", new ResourceDefinition(1,
                    "Soil_Dirt",
                    "Soil_Grass"),
            "sand_blocks", new ResourceDefinition(1,
                    "Soil_Sand",
                    "Rock_Sandstone",
                    "Rock_Sandstone_Red",
                    "Rock_Sandstone_White"),
            "metal", new ResourceDefinition(4,
                    "Ore_",
                    "Ingredient_Bar_"),
            "gems", new ResourceDefinition(6,
                    "Rock_Gem_",
                    "Ingredient_Crystal_",
                    "Rock_Crystal_"),
            "seeds", new ResourceDefinition(2,
                    "Plant_Seeds_")
    );

    private TerraInventoryResourcePolicy() {
    }

    public static boolean matchesItemId(String itemId, String resourceType) {
        if (itemId == null || resourceType == null || resourceType.isBlank()) {
            return false;
        }
        ResourceDefinition definition = DEFINITIONS.get(resourceType);
        return definition != null && definition.matches(itemId);
    }

    public static int unitsPerItem(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            return 1;
        }
        ResourceDefinition definition = DEFINITIONS.get(resourceType);
        return definition == null ? 1 : definition.unitsPerItem();
    }

    private record ResourceDefinition(int unitsPerItem, String... itemIdPrefixes) {
        boolean matches(String itemId) {
            if (itemId == null || itemIdPrefixes == null) {
                return false;
            }
            for (String prefix : itemIdPrefixes) {
                if (prefix != null && itemId.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }
    }
}
