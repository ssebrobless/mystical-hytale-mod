package com.motm.runtime.ability.field;

import java.util.List;

public record FieldTerrainRuntimeSpec(
        FieldTerrainRuntimeKind kind,
        String reason,
        boolean restoreBeforePlace,
        boolean groundedFluid,
        boolean appendBrownDebrisVisual,
        int columnHeight,
        List<String> primaryAssetIds,
        List<String> secondaryAssetIds
) {
    public FieldTerrainRuntimeSpec {
        kind = kind == null ? FieldTerrainRuntimeKind.NONE : kind;
        reason = reason == null ? "" : reason;
        columnHeight = Math.max(0, columnHeight);
        primaryAssetIds = primaryAssetIds == null ? List.of() : List.copyOf(primaryAssetIds);
        secondaryAssetIds = secondaryAssetIds == null ? List.of() : List.copyOf(secondaryAssetIds);
    }

    public static FieldTerrainRuntimeSpec none() {
        return new FieldTerrainRuntimeSpec(
                FieldTerrainRuntimeKind.NONE,
                "",
                false,
                false,
                false,
                0,
                List.of(),
                List.of()
        );
    }

    public String[] primaryAssetIdArray() {
        return primaryAssetIds.toArray(String[]::new);
    }

    public String[] secondaryAssetIdArray() {
        return secondaryAssetIds.toArray(String[]::new);
    }
}
