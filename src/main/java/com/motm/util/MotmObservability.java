package com.motm.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.server.core.universe.PlayerRef;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.logging.Logger;

/**
 * Loss-preserving runtime evidence writer for agent-driven verification runs.
 *
 * This class intentionally writes simple JSONL files instead of deciding pass/fail.
 * The agent can then inspect raw streams, indexes, and snapshots together.
 */
public class MotmObservability {

    private static final Logger LOG = Logger.getLogger("MOTM");
    private static final Pattern SAFE_ID = Pattern.compile("[^A-Za-z0-9_.-]+");
    private static final int MAX_PACKET_STRING_LENGTH = 2_000;

    private final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private final Gson lineGson = new GsonBuilder().disableHtmlEscaping().create();
    private final Object writeLock = new Object();
    private final Path rootDirectory;
    private final Path runsDirectory;

    private volatile String activeRunId;
    private volatile String activeScenarioId;
    private volatile long activeRunStartedAtMillis;
    private volatile String packetScope = "key";

    public MotmObservability(Path pluginDirectory) {
        this.rootDirectory = pluginDirectory == null
                ? null
                : pluginDirectory.resolve("observability").toAbsolutePath().normalize();
        this.runsDirectory = rootDirectory == null ? null : rootDirectory.resolve("runs");
    }

    public synchronized String startRun(String requestedRunId,
                                        String requestedScenarioId,
                                        String actor,
                                        Map<String, Object> metadata) {
        if (runsDirectory == null) {
            return "[MOTM] Observability unavailable: plugin data directory is not initialized.";
        }

        String runId = sanitizeId(requestedRunId, "run-" + Instant.now().toString().replace(':', '-'));
        String scenarioId = sanitizeId(requestedScenarioId, "manual");

        activeRunId = runId;
        activeScenarioId = scenarioId;
        activeRunStartedAtMillis = System.currentTimeMillis();

        try {
            Files.createDirectories(getActiveRunDirectory());
            Files.createDirectories(getActiveRunDirectory().resolve("raw"));
            Files.createDirectories(getActiveRunDirectory().resolve("indexes"));

            Map<String, Object> manifest = baseEnvelope("manifest", "run_start", null);
            manifest.put("actor", actor == null || actor.isBlank() ? "unknown" : actor);
            manifest.put("metadata", metadata == null ? Map.of() : metadata);
            manifest.put("referenceDocuments", List.of(
                    "docs/agent-driven-verification-observability.md",
                    "docs/hytale-capability-atlas/README.md",
                    "docs/hytale-capability-atlas/source-index.md",
                    "docs/hytale-capability-atlas/research-completeness-audit.md"
            ));
            Files.writeString(
                    getActiveRunDirectory().resolve("manifest.runtime.json"),
                    gson.toJson(manifest) + System.lineSeparator(),
                    StandardCharsets.UTF_8
            );
            record("causality", "run_start", null, mapOf(
                    "actor", actor,
                    "packetScope", packetScope
            ));
        } catch (IOException e) {
            LOG.warning("[MOTM] Observability run start failed: " + e.getMessage());
            return "[MOTM] Observability failed to start run " + runId + ": " + e.getMessage();
        }

        return "[MOTM] Observability run active: runId=" + runId
                + " scenario=" + scenarioId
                + " dir=" + getActiveRunDirectory();
    }

    public synchronized String stopRun(String reason) {
        if (!isActive()) {
            return "[MOTM] Observability is not active.";
        }

        String stoppedRunId = activeRunId;
        record("causality", "run_stop", null, mapOf(
                "reason", reason == null || reason.isBlank() ? "manual" : reason,
                "durationMillis", System.currentTimeMillis() - activeRunStartedAtMillis
        ));
        activeRunId = null;
        activeScenarioId = null;
        activeRunStartedAtMillis = 0L;
        return "[MOTM] Observability run stopped: runId=" + stoppedRunId;
    }

    public String status() {
        if (!isActive()) {
            return "[MOTM] Observability inactive. Packet scope=" + packetScope + ".";
        }
        return "[MOTM] Observability active: runId=" + activeRunId
                + " scenario=" + activeScenarioId
                + " packetScope=" + packetScope
                + " dir=" + getActiveRunDirectory();
    }

    public boolean isActive() {
        return activeRunId != null && !activeRunId.isBlank() && runsDirectory != null;
    }

    public Path getActiveRunDirectory() {
        if (!isActive()) {
            return null;
        }
        return runsDirectory.resolve(activeRunId);
    }

    public String getActiveRunId() {
        return activeRunId;
    }

    public String getActiveScenarioId() {
        return activeScenarioId;
    }

    public String getPacketScope() {
        return packetScope;
    }

    public void setPacketScope(String packetScope) {
        String normalized = packetScope == null ? "key" : packetScope.trim().toLowerCase(Locale.ROOT);
        this.packetScope = switch (normalized) {
            case "off", "none", "disabled" -> "off";
            case "all", "full" -> "all";
            default -> "key";
        };
    }

    public void setScenario(String scenarioId) {
        if (!isActive()) {
            return;
        }
        String previous = activeScenarioId;
        activeScenarioId = sanitizeId(scenarioId, "manual");
        record("causality", "scenario_set", null, mapOf(
                "previousScenarioId", previous,
                "scenarioId", activeScenarioId
        ));
    }

    public String nextTraceId(String prefix) {
        String cleanPrefix = sanitizeId(prefix, "trace");
        return cleanPrefix + "-" + Long.toUnsignedString(System.currentTimeMillis(), 36);
    }

    public void recordControl(String type, String traceId, Map<String, Object> data) {
        record("control", type, traceId, data);
    }

    public void recordCausality(String type, String traceId, Map<String, Object> data) {
        record("causality", type, traceId, data);
    }

    public void recordServerTruth(String type, String traceId, Map<String, Object> data) {
        record("server-truth", type, traceId, data);
    }

    public void recordClientIntent(String type, String traceId, Map<String, Object> data) {
        record("client-intent", type, traceId, data);
    }

    public void recordExternalArtifact(String type, String traceId, Map<String, Object> data) {
        record("external-artifacts", type, traceId, data);
    }

    public void recordPacket(String direction, PlayerRef playerRef, Packet packet) {
        recordPacket(direction, null, playerRef, packet);
    }

    public void recordPacket(String direction, String traceId, PlayerRef playerRef, Packet packet) {
        if (!isActive() || packet == null || "off".equals(packetScope)) {
            return;
        }

        String packetClass = packet.getClass().getName();
        String packetSimpleName = packet.getClass().getSimpleName();
        if ("key".equals(packetScope) && !isKeyPacket(packetClass, packetSimpleName)) {
            return;
        }

        String packetString = String.valueOf(packet);
        boolean truncated = packetString.length() > MAX_PACKET_STRING_LENGTH;
        if (truncated) {
            packetString = packetString.substring(0, MAX_PACKET_STRING_LENGTH);
        }

        Map<String, Object> player = new LinkedHashMap<>();
        if (playerRef != null) {
            if (playerRef.getUuid() != null) {
                player.put("uuid", playerRef.getUuid().toString());
            }
            player.put("username", playerRef.getUsername());
        }

        record("packets", direction == null ? "packet" : direction, traceId, mapOf(
                "direction", direction,
                "packetId", packet.getId(),
                "channel", String.valueOf(packet.getChannel()),
                "packetClass", packetClass,
                "packetSimpleName", packetSimpleName,
                "player", player,
                "packetString", packetString,
                "packetStringTruncated", truncated
        ));
    }

    public void record(String plane, String type, String traceId, Map<String, Object> data) {
        if (!isActive()) {
            return;
        }

        Map<String, Object> envelope = baseEnvelope(plane, type, traceId);
        envelope.put("data", data == null ? Map.of() : data);
        writeJsonl(plane + ".jsonl", envelope);
    }

    public static Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (keyValues == null) {
            return result;
        }
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            Object key = keyValues[index];
            if (key == null) {
                continue;
            }
            result.put(String.valueOf(key), keyValues[index + 1]);
        }
        return result;
    }

    public static String sanitizeId(String requested, String fallback) {
        String value = requested == null || requested.isBlank() ? fallback : requested.trim();
        value = SAFE_ID.matcher(value).replaceAll("-");
        value = value.replaceAll("-+", "-");
        if (value.isBlank() || ".".equals(value) || "..".equals(value)) {
            value = fallback;
        }
        return value;
    }

    private Map<String, Object> baseEnvelope(String plane, String type, String traceId) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("timestamp", Instant.now().toString());
        envelope.put("epochMillis", System.currentTimeMillis());
        envelope.put("runId", activeRunId);
        envelope.put("scenarioId", activeScenarioId);
        envelope.put("traceId", traceId);
        envelope.put("plane", plane);
        envelope.put("type", type);
        return envelope;
    }

    private void writeJsonl(String fileName, Map<String, Object> envelope) {
        try {
            synchronized (writeLock) {
                Path runDirectory = getActiveRunDirectory();
                if (runDirectory == null) {
                    return;
                }
                Files.createDirectories(runDirectory);
                Files.writeString(
                        runDirectory.resolve(fileName),
                        lineGson.toJson(envelope) + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND
                );
            }
        } catch (IOException e) {
            LOG.warning("[MOTM] Observability write failed: " + fileName + " " + e.getMessage());
        }
    }

    private boolean isKeyPacket(String packetClass, String packetSimpleName) {
        String name = ((packetClass == null ? "" : packetClass)
                + "."
                + (packetSimpleName == null ? "" : packetSimpleName)).toLowerCase(Locale.ROOT);
        if (name.contains("entityupdates") || name.contains("cachedpacket") || name.contains("chunk")) {
            return false;
        }
        return name.contains("syncinteraction")
                || name.contains("clientmovement")
                || name.contains("mouseinteraction")
                || name.contains("playerchat")
                || name.contains("setservercamera")
                || name.contains("customui")
                || name.contains("custompage")
                || name.contains("hud")
                || name.contains("effect")
                || name.contains("sound")
                || name.contains("particle")
                || name.contains("projectile");
    }
}
