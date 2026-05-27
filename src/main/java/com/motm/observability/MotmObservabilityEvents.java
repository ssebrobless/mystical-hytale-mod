package com.motm.observability;

import com.motm.util.MotmObservability;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Owns observability event emission and trace-context policy.
 */
public final class MotmObservabilityEvents {

    private final Supplier<MotmObservability> observabilitySupplier;
    private final ThreadLocal<String> traceContext = new ThreadLocal<>();

    public MotmObservabilityEvents(Supplier<MotmObservability> observabilitySupplier) {
        this.observabilitySupplier = observabilitySupplier;
    }

    public void recordControl(String type, String traceId, Map<String, Object> data) {
        MotmObservability observability = observability();
        if (observability != null) {
            observability.recordControl(type, effectiveTraceId(traceId), data);
        }
    }

    public void recordCausality(String type, String traceId, Map<String, Object> data) {
        MotmObservability observability = observability();
        if (observability != null) {
            observability.recordCausality(type, effectiveTraceId(traceId), data);
        }
    }

    public void recordServerTruth(String type, String traceId, Map<String, Object> data) {
        MotmObservability observability = observability();
        if (observability != null) {
            observability.recordServerTruth(type, effectiveTraceId(traceId), data);
        }
    }

    public void recordClientIntent(String type, String traceId, Map<String, Object> data) {
        MotmObservability observability = observability();
        if (observability == null) {
            return;
        }

        String effectiveTraceId = effectiveTraceId(traceId);
        if ((effectiveTraceId == null || effectiveTraceId.isBlank()) && observability.isActive()) {
            effectiveTraceId = observability.nextTraceId("client");
        }
        observability.recordClientIntent(type, effectiveTraceId, data);
    }

    public String enterTrace(String traceId) {
        String previous = traceContext.get();
        if (traceId == null || traceId.isBlank()) {
            traceContext.remove();
        } else {
            traceContext.set(traceId);
        }
        return previous;
    }

    public void restoreTrace(String previousTraceId) {
        if (previousTraceId == null || previousTraceId.isBlank()) {
            traceContext.remove();
        } else {
            traceContext.set(previousTraceId);
        }
    }

    public String currentTraceId() {
        return effectiveTraceId(null);
    }

    public String currentOrNewClientIntentTraceId() {
        String traceId = currentTraceId();
        MotmObservability observability = observability();
        if ((traceId == null || traceId.isBlank()) && observability != null && observability.isActive()) {
            return observability.nextTraceId("client");
        }
        return traceId;
    }

    private String effectiveTraceId(String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return traceContext.get();
    }

    private MotmObservability observability() {
        return observabilitySupplier == null ? null : observabilitySupplier.get();
    }
}
