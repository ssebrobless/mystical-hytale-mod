package com.motm.runtime.ability.field;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldVisualRuntimeTest {

    @Test
    void noneRepresentsMissingFieldVisual() {
        FieldVisualRuntime visual = FieldVisualRuntime.none();

        assertTrue(visual.visualRefs().isEmpty());
        assertNull(visual.loopEffectId());
        assertEquals(Long.MAX_VALUE, visual.nextRefreshAtMillis());
    }

    @Test
    void visualRefsAreDefensiveAndImmutable() {
        ArrayList<com.hypixel.hytale.component.Ref<
                com.hypixel.hytale.server.core.universe.world.storage.EntityStore>> refs = new ArrayList<>();

        FieldVisualRuntime visual = new FieldVisualRuntime(refs, "loop", 123L);
        refs.add(null);

        assertTrue(visual.visualRefs().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> visual.visualRefs().add(null));
    }
}
