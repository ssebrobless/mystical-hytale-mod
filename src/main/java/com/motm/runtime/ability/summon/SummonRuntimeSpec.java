package com.motm.runtime.ability.summon;

public record SummonRuntimeSpec(
        String role,
        boolean ranged,
        double attackRange,
        double chaseRange,
        long attackIntervalMillis,
        long hatchDelayMillis,
        double baseDamageMultiplier,
        String attackToken,
        String modelId
) {
}
