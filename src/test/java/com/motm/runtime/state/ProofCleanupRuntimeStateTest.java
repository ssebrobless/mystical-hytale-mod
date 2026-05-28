package com.motm.runtime.state;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProofCleanupRuntimeStateTest {

    @Test
    void tracksAndRemovesTemporaryProofArtifacts() {
        ProofCleanupRuntimeState state = new ProofCleanupRuntimeState();
        TemporaryProofSelection selection = new TemporaryProofSelection("proof", null, null, null, 1000L);
        TemporaryProofProxy proxy = new TemporaryProofProxy("proof", null, null, 1000L);

        state.addSelection(selection);
        state.addProxy(proxy);

        assertEquals(1, state.selectionCount());
        assertEquals(1, state.proxyCount());
        assertEquals(selection, state.selections().getFirst());
        assertEquals(proxy, state.proxies().getFirst());
        assertTrue(state.removeSelection(selection));
        assertTrue(state.removeProxy(proxy));
        assertEquals(0, state.selectionCount());
        assertEquals(0, state.proxyCount());
    }
}
