package com.motm.command;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.util.MotmObservability;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Owns the file-backed `/motm dev` command bridge used by the agent harness.
 */
public final class MotmDevCommandInbox {

    public static final String INBOX_FILE_NAME = "dev-command-inbox.txt";
    public static final String OUTBOX_FILE_NAME = "dev-command-outbox.log";
    private static final long POLL_INTERVAL_MS = 250L;

    private long lastPollAtMs = 0L;

    public void process(Store<EntityStore> currentStore, Hooks hooks, Logger log) {
        if (hooks == null || !hooks.devToolsEnabled() || hooks.pluginDirectory() == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastPollAtMs < POLL_INTERVAL_MS) {
            return;
        }
        lastPollAtMs = now;

        Path pluginDirectory = hooks.pluginDirectory();
        Path inbox = pluginDirectory.resolve(INBOX_FILE_NAME);
        if (!Files.exists(inbox)) {
            return;
        }

        Player runtimePlayer = hooks.findRuntimePlayer(currentStore);
        if (runtimePlayer == null) {
            return;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(inbox, StandardCharsets.UTF_8);
            Files.deleteIfExists(inbox);
        } catch (IOException e) {
            log.warning("[MOTM] Dev command inbox read failed: " + e.getMessage());
            return;
        }

        for (String rawLine : lines) {
            processLine(rawLine, runtimePlayer, pluginDirectory, hooks, log);
        }
    }

    static String normalizeCommand(String rawLine) {
        if (rawLine == null) {
            return "";
        }
        String command = rawLine.replace("\uFEFF", "").trim();
        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (command.regionMatches(true, 0, "motm", 0, 4)) {
            command = command.substring(4).trim();
        }
        return command;
    }

    private void processLine(String rawLine,
                             Player runtimePlayer,
                             Path pluginDirectory,
                             Hooks hooks,
                             Logger log) {
        String command = normalizeCommand(rawLine);
        if (command.isBlank()) {
            return;
        }

        String traceId = hooks.nextTraceId();
        hooks.recordControl("dev_command_received", traceId, MotmObservability.mapOf(
                "command", "/motm " + command,
                "rawLine", rawLine
        ));
        String previousTraceId = hooks.enterTrace(traceId);
        try {
            String result = hooks.execute(runtimePlayer, command.split("\\s+"));
            String safeResult = result == null ? "" : result.replace('\n', ' ');
            String out = "[MOTM] Dev command inbox executed: command=/motm " + command
                    + " traceId=" + traceId
                    + " result=" + safeResult;
            log.info(out);
            hooks.recordControl("dev_command_executed", traceId, MotmObservability.mapOf(
                    "command", "/motm " + command,
                    "result", safeResult
            ));
            appendOutbox(pluginDirectory, out, log);
        } catch (Throwable t) {
            String out = "[MOTM] Dev command inbox failed: command=/motm " + command
                    + " traceId=" + traceId
                    + " error=" + t.getClass().getSimpleName() + ": " + t.getMessage();
            log.severe(out);
            hooks.recordControl("dev_command_failed", traceId, MotmObservability.mapOf(
                    "command", "/motm " + command,
                    "errorType", t.getClass().getSimpleName(),
                    "error", t.getMessage()
            ));
            appendOutbox(pluginDirectory, out, log);
        } finally {
            hooks.restoreTrace(previousTraceId);
        }
    }

    private void appendOutbox(Path pluginDirectory, String line, Logger log) {
        try {
            Files.createDirectories(pluginDirectory);
            Files.writeString(
                    pluginDirectory.resolve(OUTBOX_FILE_NAME),
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            log.warning("[MOTM] Dev command outbox write failed: " + e.getMessage());
        }
    }

    public interface Hooks {
        boolean devToolsEnabled();

        Path pluginDirectory();

        Player findRuntimePlayer(Store<EntityStore> currentStore);

        String nextTraceId();

        String enterTrace(String traceId);

        void restoreTrace(String previousTraceId);

        String execute(Player player, String[] args);

        void recordControl(String type, String traceId, Map<String, Object> data);
    }
}
