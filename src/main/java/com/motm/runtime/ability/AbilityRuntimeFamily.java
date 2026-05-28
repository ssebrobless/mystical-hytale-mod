package com.motm.runtime.ability;

import java.util.Set;

public enum AbilityRuntimeFamily {
    PROJECTILE,
    FIELD,
    TERRAIN,
    SUMMON,
    TRANSFORMATION,
    FOLLOW_UP,
    MOVEMENT,
    INSTANT,
    UNKNOWN;

    private static final Set<String> PROJECTILE_CAST_TYPES = Set.of(
            "projectile", "projectile_line", "projectile_burst", "projectile_volley", "wave_line", "chain"
    );
    private static final Set<String> FIELD_CAST_TYPES = Set.of(
            "ground_zone", "support_zone", "barrier", "channel"
    );
    private static final Set<String> TERRAIN_CAST_TYPES = Set.of(
            "ground_burst", "ground_strike", "ground_target", "self_burst"
    );
    private static final Set<String> MOVEMENT_CAST_TYPES = Set.of(
            "air_stall", "dash", "dash_buff", "dash_strike", "dive_strike", "leap", "teleport"
    );
    private static final Set<String> FOLLOW_UP_CAST_TYPES = Set.of(
            "self_buff", "summon_buff", "cleanse", "curse", "execute", "cone", "gaze", "line_control"
    );

    public static AbilityRuntimeFamily fromCastType(String castType) {
        if (castType == null || castType.isBlank()) {
            return UNKNOWN;
        }
        if (PROJECTILE_CAST_TYPES.contains(castType)) {
            return PROJECTILE;
        }
        if (FIELD_CAST_TYPES.contains(castType)) {
            return FIELD;
        }
        if (TERRAIN_CAST_TYPES.contains(castType)) {
            return TERRAIN;
        }
        if ("summon".equals(castType)) {
            return SUMMON;
        }
        if ("transformation".equals(castType)) {
            return TRANSFORMATION;
        }
        if (MOVEMENT_CAST_TYPES.contains(castType)) {
            return MOVEMENT;
        }
        if (FOLLOW_UP_CAST_TYPES.contains(castType)) {
            return FOLLOW_UP;
        }
        return INSTANT;
    }
}
