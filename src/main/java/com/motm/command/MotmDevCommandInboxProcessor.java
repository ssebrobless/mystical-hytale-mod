package com.motm.command;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;

import java.nio.file.Path;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Owns the server-tick processing adapter for the file-backed dev command
 * inbox. The lower-level inbox still owns file I/O and command execution
 * wrapping; this class owns runtime-player selection and trace fallback.
 */
public final class MotmDevCommandInboxProcessor {

    private final MotmDevCommandInbox inbox;
    private final Hooks hooks;
    private final Logger log;

    public MotmDevCommandInboxProcessor(MotmDevCommandInbox inbox, Hooks hooks, Logger log) {
        this.inbox = inbox;
        this.hooks = hooks;
        this.log = log;
    }

    public void process(Store<EntityStore> currentStore) {
        inbox.process(currentStore, new MotmDevCommandInbox.Hooks() {
            @Override
            public boolean devToolsEnabled() {
                return hooks.devToolsEnabled() && hooks.commandAvailable();
            }

            @Override
            public Path pluginDirectory() {
                return hooks.pluginDirectory();
            }

            @Override
            public Player findRuntimePlayer(Store<EntityStore> currentStore) {
                for (Player player : hooks.runtimePlayers()) {
                    if (hooks.isPlayerInStore(player, currentStore)) {
                        return player;
                    }
                }
                return null;
            }

            @Override
            public String nextTraceId() {
                String traceId = hooks.nextTraceId("cmd");
                return traceId == null || traceId.isBlank()
                        ? "cmd-" + Long.toUnsignedString(System.currentTimeMillis(), 36)
                        : traceId;
            }

            @Override
            public String enterTrace(String traceId) {
                return hooks.enterTrace(traceId);
            }

            @Override
            public void restoreTrace(String previousTraceId) {
                hooks.restoreTrace(previousTraceId);
            }

            @Override
            public String execute(Player player, String[] args) {
                return hooks.execute(player, args);
            }

            @Override
            public void recordControl(String type, String traceId, Map<String, Object> data) {
                hooks.recordControl(type, traceId, data);
            }
        }, log);
    }

    public interface Hooks {
        boolean devToolsEnabled();

        boolean commandAvailable();

        Path pluginDirectory();

        Iterable<Player> runtimePlayers();

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);

        String nextTraceId(String prefix);

        String enterTrace(String traceId);

        void restoreTrace(String previousTraceId);

        String execute(Player player, String[] args);

        void recordControl(String type, String traceId, Map<String, Object> data);
    }
}
