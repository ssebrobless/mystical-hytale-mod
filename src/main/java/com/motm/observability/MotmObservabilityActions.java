package com.motm.observability;

import com.motm.model.PlayerData;
import com.motm.util.MotmObservability;

import java.nio.file.Path;
import java.util.Map;

/**
 * Owns command-facing observability run control and snapshot capture.
 */
public final class MotmObservabilityActions {

    private final MotmObservabilitySnapshotBuilder snapshots;
    private final Hooks hooks;

    public MotmObservabilityActions(MotmObservabilitySnapshotBuilder snapshots, Hooks hooks) {
        this.snapshots = snapshots;
        this.hooks = hooks;
    }

    public String startRun(String runId, String scenarioId, String playerId) {
        if (!hooks.devToolsEnabled()) {
            return hooks.devToolsDisabledMessage();
        }
        MotmObservability observability = hooks.observability();
        if (observability == null) {
            return "[MOTM] Observability unavailable.";
        }

        PlayerData playerData = playerId == null ? null : hooks.playerData(playerId);
        Path pluginDirectory = hooks.pluginDirectory();
        Map<String, Object> metadata = MotmObservability.mapOf(
                "buildChannel", hooks.buildChannel(),
                "internalTestBuild", hooks.internalTestBuild(),
                "devToolsEnabled", hooks.devToolsEnabled(),
                "pluginDirectory", pluginDirectory != null ? pluginDirectory.toString() : null,
                "playerId", playerId,
                "playerName", playerData != null ? playerData.getPlayerName() : null
        );
        return observability.startRun(runId, scenarioId, "motm-dev-command", metadata);
    }

    public String stopRun(String reason) {
        MotmObservability observability = hooks.observability();
        if (observability == null) {
            return "[MOTM] Observability unavailable.";
        }
        return observability.stopRun(reason);
    }

    public String status() {
        MotmObservability observability = hooks.observability();
        if (observability == null) {
            return "[MOTM] Observability unavailable.";
        }
        return observability.status();
    }

    public String setScenario(String scenarioId) {
        MotmObservability observability = hooks.observability();
        if (observability == null || !observability.isActive()) {
            return "[MOTM] Observability is not active.";
        }
        observability.setScenario(scenarioId);
        return "[MOTM] Observability scenario set: " + observability.getActiveScenarioId();
    }

    public String mark(String playerId, String label) {
        MotmObservability observability = hooks.observability();
        if (observability == null || !observability.isActive()) {
            return "[MOTM] Observability is not active.";
        }
        String effectiveLabel = label == null || label.isBlank() ? "marker" : label;
        String traceId = observability.nextTraceId("marker");
        observability.recordCausality("marker", traceId, MotmObservability.mapOf(
                "playerId", playerId,
                "label", effectiveLabel
        ));
        return "[MOTM] Observability marker: label=" + effectiveLabel + " traceId=" + traceId;
    }

    public String snapshot(String playerId, String label) {
        MotmObservability observability = hooks.observability();
        if (observability == null || !observability.isActive()) {
            return "[MOTM] Observability is not active.";
        }
        if (snapshots == null) {
            return "[MOTM] Observability snapshot unavailable.";
        }
        String traceId = observability.nextTraceId("snapshot");
        Map<String, Object> snapshot = snapshots.build(playerId, label);
        observability.recordServerTruth("snapshot", traceId, snapshot);
        return "[MOTM] Observability snapshot captured: label="
                + snapshot.getOrDefault("label", "snapshot")
                + " traceId=" + traceId
                + " runId=" + observability.getActiveRunId();
    }

    public interface Hooks {
        boolean devToolsEnabled();

        String devToolsDisabledMessage();

        MotmObservability observability();

        PlayerData playerData(String playerId);

        String buildChannel();

        boolean internalTestBuild();

        Path pluginDirectory();
    }
}
