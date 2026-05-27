package com.motm.runtime.ability.transformation;

import java.util.List;

public record TransformationRuntimeSpec(
        TransformationRuntimeKind kind,
        String visualEffectId,
        double damageBonus,
        double weaponBonus,
        double movementMultiplier,
        double verticalBonus,
        String weaponRiderToken,
        double locomotionTriggerDistance,
        double collisionRadius,
        List<String> ownerRuntimeTokens,
        double ownerShieldAmount,
        boolean endsWhenGrounded,
        String summary
) {
    public TransformationRuntimeSpec {
        kind = kind == null ? TransformationRuntimeKind.GENERIC : kind;
        ownerRuntimeTokens = ownerRuntimeTokens == null ? List.of() : List.copyOf(ownerRuntimeTokens);
    }

    public static TransformationRuntimeSpec fallback() {
        return new TransformationRuntimeSpec(
                TransformationRuntimeKind.GENERIC,
                null,
                0.10,
                0.15,
                1.10,
                0.0,
                null,
                1.20,
                2.00,
                List.of(),
                0.0,
                false,
                "transformed combat state"
        );
    }
}
