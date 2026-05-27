package com.motm.runtime.ability;

import com.motm.model.AbilityData;
import com.motm.util.HytaleAssetResolver;

import java.util.Locale;

/**
 * Resolves runtime entity-effect ids from ability descriptors and class/style
 * themes.
 */
public final class AbilityRuntimeEffects {

    private AbilityRuntimeEffects() {
    }

    public static String impactEffectId(String classId, String styleId, AbilityData ability) {
        String themed = themedEffectId(classId, styleId, ability, RuntimeEffectKind.IMPACT);
        if (themed != null) {
            return themed;
        }
        if (ability != null) {
            String impact = asRuntimeEffectId(HytaleAssetResolver.resolve(classId, styleId, ability).getImpactEffectAsset());
            if (impact != null) {
                return impact;
            }
        }
        return switch (lower(classId)) {
            case "terra" -> "MOTM_Terra_Impact";
            case "hydro" -> "MOTM_Hydro_Impact";
            case "aero" -> "MOTM_Aero_Impact";
            case "corruptus" -> "MOTM_Corruptus_Impact";
            default -> null;
        };
    }

    public static String projectileVisualEffectId(String classId, String styleId, AbilityData ability) {
        String themed = themedEffectId(classId, styleId, ability, RuntimeEffectKind.MOVE);
        if (themed != null) {
            return themed;
        }
        if (ability != null) {
            var assets = HytaleAssetResolver.resolve(classId, styleId, ability);
            String travel = asRuntimeEffectId(assets.getTravelEffectAsset());
            if (travel != null) {
                return travel;
            }
            String impact = asRuntimeEffectId(assets.getImpactEffectAsset());
            if (impact != null) {
                return impact;
            }
            String cast = asRuntimeEffectId(assets.getCastEffectAsset());
            if (cast != null) {
                return cast;
            }
        }

        return switch (lower(classId)) {
            case "terra" -> "MOTM_Terra_Move";
            case "hydro" -> "MOTM_Hydro_Move";
            case "aero" -> "MOTM_Aero_Move";
            case "corruptus" -> "MOTM_Corruptus_Move";
            default -> null;
        };
    }

    public static String fieldVisualEffectId(String classId, String styleId, AbilityData ability) {
        String themed = themedEffectId(classId, styleId, ability, RuntimeEffectKind.FIELD);
        if (themed != null) {
            return themed;
        }
        if (ability == null) {
            return null;
        }

        var assets = HytaleAssetResolver.resolve(classId, styleId, ability);
        String loop = asRuntimeEffectId(assets.getLoopEffectAsset());
        if (loop != null) {
            return loop;
        }
        String impact = asRuntimeEffectId(assets.getImpactEffectAsset());
        if (impact != null) {
            return impact;
        }
        String travel = asRuntimeEffectId(assets.getTravelEffectAsset());
        if (travel != null) {
            return travel;
        }
        return null;
    }

    public static String castEffectId(String classId, String styleId, AbilityData ability) {
        String themed = themedEffectId(classId, styleId, ability, RuntimeEffectKind.CAST);
        if (themed != null) {
            return themed;
        }
        String prefix = switch (lower(classId)) {
            case "terra" -> "MOTM_Terra";
            case "hydro" -> "MOTM_Hydro";
            case "aero" -> "MOTM_Aero";
            case "corruptus" -> "MOTM_Corruptus";
            default -> null;
        };
        if (prefix == null) {
            return null;
        }

        return AbilityExecutionPolicy.isMovementCast(ability)
                ? prefix + "_Move"
                : prefix + "_Cast";
    }

    static String asRuntimeEffectId(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        if (candidate.startsWith("MOTM_") || candidate.contains("/Entity/Effects/")) {
            return candidate;
        }
        return null;
    }

    private static String themedEffectId(String classId,
                                         String styleId,
                                         AbilityData ability,
                                         RuntimeEffectKind kind) {
        String lowerClassId = lower(classId);
        String lowerStyleId = lower(styleId);
        if (ability == null) {
            return null;
        }

        if ("aero".equals(lowerClassId) && "scream".equals(lowerStyleId)) {
            return switch (kind) {
                case CAST -> "MOTM_Aero_Scream_Cast";
                case MOVE -> "MOTM_Aero_Scream_Move";
                case IMPACT -> "MOTM_Aero_Scream_Impact";
                case FIELD -> "MOTM_Aero_Scream_Field";
            };
        }

        if ("hydro".equals(lowerClassId)
                && ("surf".equals(lowerStyleId)
                || "rain".equals(lowerStyleId)
                || "saltwater".equals(lowerStyleId)
                || "freshwater".equals(lowerStyleId)
                || "bilgewater".equals(lowerStyleId)
                || "boiling".equals(lowerStyleId))) {
            return switch (kind) {
                case CAST -> "MOTM_Hydro_Wave_Cast";
                case MOVE -> "MOTM_Hydro_Wave_Move";
                case IMPACT -> "MOTM_Hydro_Wave_Impact";
                case FIELD -> "MOTM_Hydro_Wave_Field";
            };
        }

        if ("terra".equals(lowerClassId) && "gem".equals(lowerStyleId)) {
            return switch (kind) {
                case CAST -> "MOTM_Terra_Gem_Cast";
                case MOVE, IMPACT -> "MOTM_Terra_Gem_Impact";
                case FIELD -> "MOTM_Terra_Gem_Field";
            };
        }

        if ("corruptus".equals(lowerClassId)
                && ("void".equals(lowerStyleId) || "shadow".equals(lowerStyleId))) {
            return switch (kind) {
                case CAST -> "MOTM_Corruptus_Void_Cast";
                case MOVE -> "MOTM_Corruptus_Void_Move";
                case IMPACT -> "MOTM_Corruptus_Void_Impact";
                case FIELD -> "MOTM_Corruptus_Void_Field";
            };
        }

        return null;
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private enum RuntimeEffectKind {
        CAST,
        MOVE,
        IMPACT,
        FIELD
    }
}
