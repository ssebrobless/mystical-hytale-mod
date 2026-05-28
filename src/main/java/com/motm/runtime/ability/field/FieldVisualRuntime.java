package com.motm.runtime.ability.field;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.List;

public record FieldVisualRuntime(List<Ref<EntityStore>> visualRefs,
                                 String loopEffectId,
                                 long nextRefreshAtMillis) {
    public FieldVisualRuntime {
        visualRefs = visualRefs == null ? List.of() : List.copyOf(visualRefs);
    }

    public static FieldVisualRuntime none() {
        return new FieldVisualRuntime(List.of(), null, Long.MAX_VALUE);
    }
}
