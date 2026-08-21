package com.motm.runtime.ability;

import com.motm.model.AbilityData;
import com.motm.model.StatusEffect;

import java.util.Locale;

/**
 * Builds status-effect runtime objects from normalized ability effect tokens.
 */
public final class AbilityStatusEffects {

    private static final int TICKS_PER_SECOND = 20;
    private static final int DEFAULT_STATUS_SECONDS = 4;
    private static final int ONE_SHOT_BUFF_SECONDS = 12;

    private AbilityStatusEffects() {
    }

    public static StatusEffect create(String token,
                                      AbilityData ability,
                                      String sourcePlayerId,
                                      String sourceAbilityId) {
        String normalized = lower(token);
        int durationTicks = durationTicks(ability, normalized);

        return switch (normalized) {
            case "burn", "self_burn" -> new StatusEffect(
                    StatusEffect.Type.BURN, durationTicks, mag(ability, normalized, 0.03), sourcePlayerId, sourceAbilityId);
            case "dot" -> new StatusEffect(
                    StatusEffect.Type.DOT, durationTicks, mag(ability, normalized, 0.05), sourcePlayerId, sourceAbilityId);
            case "toxic" -> new StatusEffect(
                    StatusEffect.Type.TOXIC_MARK, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "stun", "stun_if_wall" -> new StatusEffect(
                    StatusEffect.Type.STUN, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "slow" -> new StatusEffect(
                    StatusEffect.Type.SLOW, durationTicks, mag(ability, normalized, 0.20), sourcePlayerId, sourceAbilityId);
            case "slow_stack" -> new StatusEffect(
                    StatusEffect.Type.SLOW_STACK, durationTicks, mag(ability, normalized, 0.10), sourcePlayerId, sourceAbilityId);
            case "vulnerability", "curse" -> new StatusEffect(
                    StatusEffect.Type.VULNERABILITY, durationTicks, mag(ability, normalized, 0.25), sourcePlayerId, sourceAbilityId);
            case "freeze" -> new StatusEffect(
                    StatusEffect.Type.FREEZE, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "root" -> new StatusEffect(
                    StatusEffect.Type.ROOT, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "blind", "deafen" -> new StatusEffect(
                    StatusEffect.Type.BLIND, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "disoriented", "attack_slow" -> new StatusEffect(
                    StatusEffect.Type.DISORIENTED, durationTicks, mag(ability, normalized, 0.15), sourcePlayerId, sourceAbilityId);
            case "grounded" -> new StatusEffect(
                    StatusEffect.Type.GROUNDED, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "flying" -> new StatusEffect(
                    StatusEffect.Type.FLYING, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "shocked", "lightning" -> new StatusEffect(
                    StatusEffect.Type.SHOCKED, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "evasion", "evasion_zone" -> new StatusEffect(
                    StatusEffect.Type.EVASION, durationTicks, mag(ability, normalized, 0.30), sourcePlayerId, sourceAbilityId);
            case "evasion_buff" -> new StatusEffect(
                    StatusEffect.Type.EVASION, durationTicks, mag(ability, normalized, 0.40), sourcePlayerId, sourceAbilityId);
            case "speed" -> new StatusEffect(
                    StatusEffect.Type.SPEED_BUFF, durationTicks, mag(ability, normalized, 0.25), sourcePlayerId, sourceAbilityId);
            case "defense_buff" -> new StatusEffect(
                    StatusEffect.Type.DEFENSE_BUFF, durationTicks, mag(ability, normalized, 0.20), sourcePlayerId, sourceAbilityId);
            case "damage_reduction" -> new StatusEffect(
                    StatusEffect.Type.DEFENSE_BUFF, durationTicks, mag(ability, normalized, 0.35), sourcePlayerId, sourceAbilityId);
            case "attack_buff" -> new StatusEffect(
                    StatusEffect.Type.ATTACK_BUFF, durationTicks, mag(ability, normalized, 0.20), sourcePlayerId, sourceAbilityId);
            case "sand_empower" -> new StatusEffect(
                    StatusEffect.Type.ATTACK_BUFF, durationTicks, mag(ability, normalized, 0.15), sourcePlayerId, sourceAbilityId);
            case "damage_buff" -> new StatusEffect(
                    StatusEffect.Type.DAMAGE_BUFF, durationTicks, mag(ability, normalized, 0.35), sourcePlayerId, sourceAbilityId);
            case "stealth" -> new StatusEffect(
                    StatusEffect.Type.STEALTH, durationTicks, mag(ability, normalized, 0.40), sourcePlayerId, sourceAbilityId);
            case "lifesteal" -> new StatusEffect(
                    StatusEffect.Type.LIFESTEAL, durationTicks, mag(ability, normalized, 0.20), sourcePlayerId, sourceAbilityId);
            default -> null;
        };
    }

    public static int durationTicks(AbilityData ability, String token) {
        if ("toxic".equals(lower(token))) {
            return 10 * TICKS_PER_SECOND;
        }
        double seconds = ability != null && ability.getDurationSeconds() > 0
                ? ability.getDurationSeconds()
                : defaultDurationSeconds(token);
        return Math.max(1, (int) Math.round(seconds * TICKS_PER_SECOND));
    }

    public static double defaultDurationSeconds(String token) {
        return switch (lower(token)) {
            case "burn", "dot", "slow", "slow_stack" -> 4.0;
            case "stun", "stun_if_wall", "freeze", "root" -> 2.0;
            case "shield" -> 6.0;
            case "attack_buff", "defense_buff", "evasion", "evasion_buff", "evasion_zone",
                    "flying", "lifesteal", "vulnerability", "curse", "speed" -> 6.0;
            case "toxic" -> 10.0;
            case "damage_buff", "stealth" -> ONE_SHOT_BUFF_SECONDS;
            default -> DEFAULT_STATUS_SECONDS;
        };
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private static double mag(AbilityData ability, String token, double fallback) {
        return ability != null ? ability.getEffectMagnitude(token, fallback) : fallback;
    }
}
