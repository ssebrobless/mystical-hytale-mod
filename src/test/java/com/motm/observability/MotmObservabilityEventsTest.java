package com.motm.observability;

import com.motm.util.MotmObservability;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotmObservabilityEventsTest {

    @TempDir
    Path tempDir;

    @Test
    void traceContextCanBeEnteredRestoredAndCleared() {
        MotmObservabilityEvents events = new MotmObservabilityEvents(() -> null);

        assertNull(events.currentTraceId());
        assertNull(events.enterTrace("trace-a"));
        assertEquals("trace-a", events.currentTraceId());
        assertEquals("trace-a", events.enterTrace("trace-b"));
        assertEquals("trace-b", events.currentTraceId());
        events.restoreTrace("trace-a");
        assertEquals("trace-a", events.currentTraceId());
        events.restoreTrace(null);
        assertNull(events.currentTraceId());
    }

    @Test
    void eventRecordingUsesCurrentTraceWhenExplicitTraceIsMissing() throws Exception {
        MotmObservability observability = new MotmObservability(tempDir);
        observability.startRun("run-a", "scenario-a", "test", Map.of());
        MotmObservabilityEvents events = new MotmObservabilityEvents(() -> observability);

        events.enterTrace("trace-a");
        events.recordCausality("event-a", null, Map.of("ok", true));

        Path causalityLog = tempDir.resolve("observability/runs/run-a/causality.jsonl");
        String log = Files.readString(causalityLog);
        assertTrue(log.contains("\"type\":\"event-a\""));
        assertTrue(log.contains("\"traceId\":\"trace-a\""));
    }

    @Test
    void clientIntentTraceCanBeAllocatedWhenRunIsActive() {
        MotmObservability observability = new MotmObservability(tempDir);
        observability.startRun("run-b", "scenario-b", "test", Map.of());
        MotmObservabilityEvents events = new MotmObservabilityEvents(() -> observability);

        assertTrue(events.currentOrNewClientIntentTraceId().startsWith("client-"));
    }
}
