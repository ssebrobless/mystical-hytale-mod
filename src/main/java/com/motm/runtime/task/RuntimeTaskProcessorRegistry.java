package com.motm.runtime.task;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Ordered processor list for deferred server-tick work.
 */
public final class RuntimeTaskProcessorRegistry {

    private final List<RuntimeTaskProcessor> processors = new ArrayList<>();
    private final Map<String, RuntimeTaskProcessor> processorsById = new LinkedHashMap<>();

    public RuntimeTaskProcessorRegistry register(RuntimeTaskProcessor processor) {
        if (processor == null) {
            throw new IllegalArgumentException("Runtime task processor is required.");
        }
        if (processor.id() == null || processor.id().isBlank()) {
            throw new IllegalArgumentException("Runtime task processor id is required.");
        }
        if (processorsById.containsKey(processor.id())) {
            throw new IllegalArgumentException("Duplicate runtime task processor id: " + processor.id());
        }
        processors.add(processor);
        processorsById.put(processor.id(), processor);
        return this;
    }

    public void processAll(Store<EntityStore> currentStore) {
        for (RuntimeTaskProcessor processor : processors) {
            processor.process(currentStore);
        }
    }

    public List<String> ids() {
        return processors.stream().map(RuntimeTaskProcessor::id).toList();
    }

    public void process(String id, Store<EntityStore> currentStore) {
        RuntimeTaskProcessor processor = processorsById.get(id);
        if (processor == null) {
            throw new IllegalArgumentException("Unknown runtime task processor id: " + id);
        }
        processor.process(currentStore);
    }
}
