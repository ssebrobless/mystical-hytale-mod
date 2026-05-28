package com.motm.runtime.ability.projectile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProjectileVisualRuntimeTest {

    @Test
    void noneRepresentsMissingProjectileVisual() {
        ProjectileVisualRuntime visual = ProjectileVisualRuntime.none();

        assertNull(visual.visualRef());
        assertNull(visual.travelEffectId());
        assertEquals(Long.MAX_VALUE, visual.nextRefreshAtMillis());
    }
}
