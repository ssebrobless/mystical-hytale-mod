package com.motm.runtime.task;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuntimeTaskProcessorRegistryTest {

    @Test
    void processesRegisteredProcessorsInOrder() {
        List<String> processed = new ArrayList<>();
        RuntimeTaskProcessorRegistry registry = new RuntimeTaskProcessorRegistry()
                .register(testProcessor("first", processed))
                .register(testProcessor("second", processed));

        registry.processAll(null);
        registry.process("second", null);

        assertEquals(List.of("first", "second", "second"), processed);
        assertEquals(List.of("first", "second"), registry.ids());
    }

    @Test
    void rejectsNullProcessor() {
        RuntimeTaskProcessorRegistry registry = new RuntimeTaskProcessorRegistry();

        assertThrows(IllegalArgumentException.class, () -> registry.register(null));
    }

    @Test
    void rejectsDuplicateProcessorIdsAndUnknownProcessRequests() {
        RuntimeTaskProcessorRegistry registry = new RuntimeTaskProcessorRegistry()
                .register(testProcessor("same", new ArrayList<>()));

        assertThrows(IllegalArgumentException.class, () -> registry.register(testProcessor("same", new ArrayList<>())));
        assertThrows(IllegalArgumentException.class, () -> registry.process("missing", null));
    }

    private static RuntimeTaskProcessor testProcessor(String id, List<String> processed) {
        return new RuntimeTaskProcessor() {
            @Override
            public String id() {
                return id;
            }

            @Override
            public void process(com.hypixel.hytale.component.Store<com.hypixel.hytale.server.core.universe.world.storage.EntityStore> currentStore) {
                processed.add(id);
            }
        };
    }
}
