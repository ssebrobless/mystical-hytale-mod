package com.motm.runtime.ability;

import com.motm.content.ability.AbilityShape;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Registry skeleton for routing normalized ability shapes to family runtimes.
 *
 * The current playback manager remains the behavior facade while runtime
 * families are migrated. New feature work should add handlers here instead of
 * adding ability-id checks to generic playback code.
 */
public final class AbilityRuntimeRegistry {

    private final Map<AbilityRuntimeFamily, AbilityRuntimeHandler> handlers =
            new EnumMap<>(AbilityRuntimeFamily.class);

    public void register(AbilityRuntimeFamily family, AbilityRuntimeHandler handler) {
        if (family == null || family == AbilityRuntimeFamily.UNKNOWN || handler == null) {
            throw new IllegalArgumentException("Ability runtime family and handler are required.");
        }
        handlers.put(family, handler);
    }

    public Optional<AbilityRuntimeHandler> resolve(AbilityShape shape) {
        if (shape == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(handlers.get(shape.runtimeFamily()));
    }

    public boolean supports(AbilityShape shape) {
        return resolve(shape).isPresent();
    }

    public interface AbilityRuntimeHandler {
        AbilityRuntimeFamily family();
    }
}
