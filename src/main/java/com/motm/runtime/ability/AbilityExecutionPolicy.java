package com.motm.runtime.ability;

import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.motm.model.AbilityData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Normalizes authored ability fields into coarse execution policy used by the
 * temporary playback facade. Ability-id checks are allowed here because this is
 * the profile edge, not the generic manager execution loop.
 */
public final class AbilityExecutionPolicy {

    private static final Set<String> MOVEMENT_CAST_TYPES = Set.of(
            "dash", "dash_buff", "dash_strike", "leap", "dive_strike", "teleport", "air_stall");
    private static final Set<String> LINE_CAST_TYPES = Set.of(
            "projectile", "projectile_line", "line_control", "wave_line", "projectile_burst");
    private static final Set<String> MULTI_TARGET_CAST_TYPES = Set.of("projectile_volley", "chain");
    private static final Set<String> GENERIC_CASTER_VISUAL_SUPPRESSED_IDS = Set.of(
            "iron_wall", "metal_coat", "alloy_enhancement", "obsidian_skin", "magma_sling");
    private static final Set<String> CASTER_EFFECT_TOKENS = Set.of(
            "attack_buff", "defense_buff", "evasion", "evasion_buff", "evasion_zone",
            "stealth", "damage_buff", "lifesteal", "flying", "self_burn", "speed");
    private static final Set<String> TARGET_EFFECT_TOKENS = Set.of(
            "burn", "dot", "stun", "stun_if_wall", "slow", "slow_stack", "vulnerability",
            "freeze", "root", "blind", "deafen", "disoriented", "attack_slow",
            "grounded", "shocked", "lightning", "knockback", "curse", "toxic");

    private AbilityExecutionPolicy() {
    }

    public static boolean isMovementCast(AbilityData ability) {
        return ability != null && isMovementCastType(ability.getCastType());
    }

    public static boolean isMovementCastType(String castType) {
        return MOVEMENT_CAST_TYPES.contains(lower(castType));
    }

    public static boolean isLineCastType(String castType) {
        return LINE_CAST_TYPES.contains(lower(castType));
    }

    public static boolean isMultiTargetCastType(String castType) {
        return MULTI_TARGET_CAST_TYPES.contains(lower(castType));
    }

    public static boolean isTargetEffectToken(String token) {
        return TARGET_EFFECT_TOKENS.contains(lower(token));
    }

    public static boolean isCasterEffectToken(String token) {
        return CASTER_EFFECT_TOKENS.contains(lower(token));
    }

    public static boolean shouldApplyRepeatingLineControlToken(String token) {
        String normalized = lower(token);
        return isTargetEffectToken(normalized)
                && !"knockback".equals(normalized)
                && !"stun_if_wall".equals(normalized);
    }

    public static boolean shouldApplyCasterEffectToken(AbilityData ability, String token) {
        String normalized = lower(token);
        if ("heal".equals(normalized) || "shield".equals(normalized)) {
            return false;
        }
        if ("alloy_enhancement".equals(abilityId(ability)) && "damage_buff".equals(normalized)) {
            return false;
        }
        return isCasterEffectToken(normalized);
    }

    public static List<String> targetEffectTokens(AbilityData ability, List<String> parsedTokens) {
        List<String> tokens = new ArrayList<>();
        if (parsedTokens != null) {
            for (String token : parsedTokens) {
                String normalized = lower(token);
                if (isTargetEffectToken(normalized)) {
                    tokens.add(normalized);
                }
            }
        }
        if ("dominate".equals(abilityId(ability))) {
            tokens.add("root");
            tokens.add("disoriented");
        }
        return tokens;
    }

    public static boolean suppressGenericCasterVisual(AbilityData ability) {
        return GENERIC_CASTER_VISUAL_SUPPRESSED_IDS.contains(abilityId(ability));
    }

    public static boolean isGroundRestricted(AbilityData ability) {
        if (ability == null) {
            return false;
        }

        String castType = lower(ability.getCastType());
        if (MOVEMENT_CAST_TYPES.contains(castType)) {
            return true;
        }

        if (!"transformation".equals(castType)) {
            return false;
        }

        String travelType = lower(ability.getTravelType());
        String abilityId = abilityId(ability);
        return travelType.contains("flight")
                || "smoke_form".equals(abilityId)
                || "pterodactyl_form".equals(abilityId);
    }

    public static boolean isAnchorDrag(AbilityData ability) {
        String abilityId = abilityId(ability);
        String travelType = lower(ability != null ? ability.getTravelType() : null);
        return "anchor_haul".equals(abilityId) || travelType.contains("anchor_drag");
    }

    public static DamageCause directDamageCause(AbilityData ability) {
        String castType = lower(ability != null ? ability.getCastType() : null);
        return isLineCastType(castType) || isMultiTargetCastType(castType)
                ? DamageCause.PROJECTILE
                : DamageCause.PHYSICAL;
    }

    public static SpecialDamagePolicy specialDamagePolicy(AbilityData ability) {
        String abilityId = abilityId(ability);
        if ("combust".equals(abilityId)) {
            return SpecialDamagePolicy.COMBUST;
        }
        if ("consume".equals(abilityId)) {
            return SpecialDamagePolicy.CONSUME;
        }
        if ("anchor_haul".equals(abilityId)) {
            return SpecialDamagePolicy.ANCHOR_TOXIC;
        }
        if (lower(ability != null ? ability.getEffect() : null).contains("lightning")) {
            return SpecialDamagePolicy.LIGHTNING;
        }
        return SpecialDamagePolicy.NONE;
    }

    private static String abilityId(AbilityData ability) {
        return lower(ability != null ? ability.getId() : null);
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    public enum SpecialDamagePolicy {
        NONE,
        COMBUST,
        CONSUME,
        LIGHTNING,
        ANCHOR_TOXIC
    }
}
