package com.motm.content.ability;

import com.motm.runtime.ability.AbilityRuntimeFamily;

import java.util.List;

/**
 * Normalized runtime-facing view of authored ability data.
 *
 * Authored JSON remains the content source. Runtime code should prefer this
 * shape over repeatedly parsing string fields from AbilityData.
 */
public record AbilityShape(
        String classId,
        String styleId,
        String abilityId,
        String castType,
        String targetType,
        List<String> effectTokens,
        String terrainEffect,
        String travelType,
        AbilityRuntimeFamily runtimeFamily
) {
}
