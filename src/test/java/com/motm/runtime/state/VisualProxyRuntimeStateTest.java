package com.motm.runtime.state;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisualProxyRuntimeStateTest {

    @Test
    void tracksVisualProxyRefsAndExposesImmutableSnapshot() {
        VisualProxyRuntimeState state = new VisualProxyRuntimeState();
        Ref<EntityStore> ref = new Ref<>(null, 1);

        state.add(null);
        state.add(ref);

        assertEquals(1, state.size());
        assertFalse(state.isEmpty());
        assertTrue(state.contains(ref));
        assertThrows(UnsupportedOperationException.class, () -> state.snapshot().add(new Ref<>(null, 2)));

        assertTrue(state.remove(ref));
        assertFalse(state.contains(ref));
        assertTrue(state.isEmpty());
    }

    @Test
    void despawnUntracksRefsEvenWithoutLiveNpcStore() {
        VisualProxyRuntimeState state = new VisualProxyRuntimeState();
        Ref<EntityStore> ref = new Ref<>(null, 1);
        state.add(ref);

        assertTrue(state.despawn(ref));

        assertFalse(state.contains(ref));
        assertTrue(state.isEmpty());
    }
}
