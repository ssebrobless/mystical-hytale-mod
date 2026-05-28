package com.motm.runtime;

import java.util.Map;

/**
 * Emits runtime-task lifecycle events to the active evidence plane.
 */
@FunctionalInterface
public interface RuntimeTaskEvidenceSink {

    RuntimeTaskEvidenceSink NOOP = (phase, taskType, playerId, details) -> { };

    void record(String phase, String taskType, String playerId, Map<String, Object> details);
}
