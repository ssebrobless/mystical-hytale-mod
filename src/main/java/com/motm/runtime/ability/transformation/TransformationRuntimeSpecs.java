package com.motm.runtime.ability.transformation;

import com.motm.model.AbilityData;

import java.util.List;

public final class TransformationRuntimeSpecs {

    private TransformationRuntimeSpecs() {
    }

    public static TransformationRuntimeSpec resolve(AbilityData ability) {
        if (ability == null) {
            return TransformationRuntimeSpec.fallback();
        }
        return switch (lower(ability.getId())) {
            case "smoke_form" -> new TransformationRuntimeSpec(
                    TransformationRuntimeKind.SMOKE,
                    "MOTM_Aero_Smoke_Form",
                    0.05,
                    0.12,
                    1.22,
                    0.35,
                    "blind",
                    0.95,
                    1.75,
                    List.of("evasion_buff"),
                    0.0,
                    true,
                    "mist body + drift blinds"
            );
            case "pterodactyl_form" -> new TransformationRuntimeSpec(
                    TransformationRuntimeKind.PTERODACTYL,
                    "MOTM_Corruptus_Pterodactyl_Form",
                    0.15,
                    0.20,
                    1.42,
                    1.35,
                    "slow",
                    1.15,
                    2.10,
                    List.of("speed", "evasion"),
                    0.0,
                    true,
                    "flight mobility + aerial drive-bys"
            );
            case "triceratops_form" -> new TransformationRuntimeSpec(
                    TransformationRuntimeKind.TRICERATOPS,
                    "MOTM_Corruptus_Triceratops_Form",
                    0.12,
                    0.24,
                    1.28,
                    0.0,
                    "knockback",
                    1.05,
                    2.45,
                    List.of("defense_buff"),
                    3.0,
                    false,
                    "armored charge + impact stuns"
            );
            case "t_rex_form" -> new TransformationRuntimeSpec(
                    TransformationRuntimeKind.T_REX,
                    "MOTM_Corruptus_TRex_Form",
                    0.22,
                    0.34,
                    1.18,
                    0.0,
                    "stun",
                    1.00,
                    3.25,
                    List.of("attack_buff"),
                    0.0,
                    false,
                    "primal power + rampage pressure"
            );
            default -> TransformationRuntimeSpec.fallback();
        };
    }

    public static String visualEffectId(AbilityData ability) {
        return resolve(ability).visualEffectId();
    }

    private static String lower(String value) {
        return value == null ? "" : value.toLowerCase();
    }
}
