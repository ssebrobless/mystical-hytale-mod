package com.motm.runtime.ability.terrain;

import org.joml.Vector3d;
import com.motm.model.AbilityData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TerrainRuntimeSpecs {
    public static final long TEMPORARY_SELECTION_MIN_LIFETIME_MS = 1_200L;
    public static final long STACKING_COLUMN_STAGE_INTERVAL_MS = 90L;
    public static final long MOVING_TRAIL_STATIONARY_RECHECK_MS = 180L;
    public static final long MOVING_TRAIL_PLACED_RECHECK_MS = 180L;
    public static final long MOVING_TRAIL_EMPTY_RECHECK_MS = 260L;
    public static final double MOVING_TRAIL_MIN_DISTANCE = 0.35;
    public static final double MOVING_TRAIL_STAMP_SPACING = 0.75;
    public static final int MOVING_TRAIL_MAX_STAMPS = 5;

    private TerrainRuntimeSpecs() {
    }

    public static boolean shouldCreateMovementTrail(AbilityData ability,
                                                    boolean movementApplied,
                                                    Vector3d startPosition,
                                                    Vector3d endPosition) {
        if (ability == null || !movementApplied || startPosition == null || endPosition == null) {
            return false;
        }
        String terrainEffect = lower(ability.getTerrainEffect());
        return terrainEffect.contains("ember_trail")
                || terrainEffect.contains("ice_skate_trail")
                || terrainEffect.contains("dust_devil")
                || terrainEffect.contains("tunnel_path")
                || terrainEffect.contains("ruptured_earth");
    }

    public static boolean shouldCreatePersonalAuraField(AbilityData ability) {
        if (ability == null) {
            return false;
        }
        String terrainEffect = lower(ability.getTerrainEffect());
        String castType = lower(ability.getCastType());
        return ("self_burst".equals(castType) && (
                terrainEffect.contains("living_flame")
                        || terrainEffect.contains("pressure_burst")
        ))
                || ("self_buff".equals(castType) && (
                terrainEffect.contains("cyclone_shield")
                        || terrainEffect.contains("eye_of_the_storm")
                        || terrainEffect.contains("root_circle")
                        || terrainEffect.contains("ice_shell")
                        || terrainEffect.contains("mist_shroud")
                        || terrainEffect.contains("condensation_veil")
                        || terrainEffect.contains("vanish")
                        || terrainEffect.contains("umbral_shroud")
                        || terrainEffect.contains("resonant_aura")
                        || terrainEffect.contains("purifying_aura")
                        || terrainEffect.contains("psychic_link")
                        || terrainEffect.contains("steam_pressure")
                        || terrainEffect.contains("sandstorm")
        ));
    }

    public static int trailNodeCount(AbilityData ability) {
        String terrainEffect = lower(ability == null ? null : ability.getTerrainEffect());
        if (terrainEffect.contains("ember_trail")) {
            return 4;
        }
        if (terrainEffect.contains("ice_skate_trail")) {
            return 3;
        }
        if (terrainEffect.contains("dust_devil")) {
            return 4;
        }
        return 3;
    }

    public static double trailRadius(AbilityData ability) {
        String terrainEffect = lower(ability == null ? null : ability.getTerrainEffect());
        if (terrainEffect.contains("ember_trail")) {
            return 2.4;
        }
        if (terrainEffect.contains("ice_skate_trail")) {
            return 2.1;
        }
        if (terrainEffect.contains("dust_devil")) {
            return Math.max(2.6, ability == null ? 0.0 : ability.getRadius());
        }
        return 2.2;
    }

    public static double auraRadius(AbilityData ability) {
        String terrainEffect = lower(ability == null ? null : ability.getTerrainEffect());
        double authoredRadius = ability == null ? 0.0 : ability.getRadius();
        if (terrainEffect.contains("living_flame")) {
            return Math.max(3.8, authoredRadius > 0 ? authoredRadius : 4.0);
        }
        if (terrainEffect.contains("pressure_burst")) {
            return 4.6;
        }
        if (terrainEffect.contains("eye_of_the_storm")) {
            return 4.5;
        }
        if (terrainEffect.contains("cyclone_shield")) {
            return 3.8;
        }
        if (terrainEffect.contains("resonant_aura")) {
            return 4.2;
        }
        if (terrainEffect.contains("ice_shell")) {
            return 3.4;
        }
        if (terrainEffect.contains("mist_shroud")
                || terrainEffect.contains("condensation_veil")
                || terrainEffect.contains("vanish")
                || terrainEffect.contains("umbral_shroud")) {
            return 3.6;
        }
        if (terrainEffect.contains("purifying_aura")) {
            return 3.7;
        }
        if (terrainEffect.contains("psychic_link")) {
            return 4.0;
        }
        if (terrainEffect.contains("steam_pressure")) {
            return 3.5;
        }
        if (terrainEffect.contains("root_circle")) {
            return 3.4;
        }
        if (terrainEffect.contains("sandstorm")) {
            return Math.max(4.0, authoredRadius);
        }
        return Math.max(2.4, authoredRadius);
    }

    public static List<Vector3d> buildTrailCenters(Vector3d start, Vector3d end, int nodes) {
        if (start == null || end == null || nodes <= 0) {
            return List.of();
        }

        List<Vector3d> centers = new ArrayList<>();
        Vector3d segment = com.motm.util.MotmVectors.addScaled(end, start, -1.0);
        int count = Math.max(2, nodes);
        for (int index = 0; index < count; index++) {
            double factor = count == 1 ? 1.0 : index / (double) (count - 1);
            centers.add(com.motm.util.MotmVectors.addScaled(start, segment, factor));
        }
        return List.copyOf(centers);
    }

    public static long temporarySelectionExpireAt(long now, long requestedExpireAtMillis) {
        return Math.max(now + TEMPORARY_SELECTION_MIN_LIFETIME_MS, requestedExpireAtMillis);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
