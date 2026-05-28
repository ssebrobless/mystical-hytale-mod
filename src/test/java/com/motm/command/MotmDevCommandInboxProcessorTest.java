package com.motm.command;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MotmDevCommandInboxProcessorTest {

    @TempDir
    Path tempDir;

    @Test
    void leavesInboxUntouchedWhenCommandHandlerUnavailable() throws Exception {
        Path inbox = tempDir.resolve(MotmDevCommandInbox.INBOX_FILE_NAME);
        Files.writeString(inbox, "status\n");

        MotmDevCommandInboxProcessor processor = new MotmDevCommandInboxProcessor(
                new MotmDevCommandInbox(),
                new Hooks(false, tempDir),
                Logger.getLogger("test")
        );

        processor.process(null);

        assertTrue(Files.exists(inbox));
    }

    private record Hooks(boolean commandAvailable, Path pluginDirectory)
            implements MotmDevCommandInboxProcessor.Hooks {
        @Override
        public boolean devToolsEnabled() {
            return true;
        }

        @Override
        public Iterable<Player> runtimePlayers() {
            return List.of();
        }

        @Override
        public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
            return false;
        }

        @Override
        public String nextTraceId(String prefix) {
            return prefix + "-test";
        }

        @Override
        public String enterTrace(String traceId) {
            return null;
        }

        @Override
        public void restoreTrace(String previousTraceId) {
        }

        @Override
        public String execute(Player player, String[] args) {
            return "ok";
        }

        @Override
        public void recordControl(String type, String traceId, Map<String, Object> data) {
        }
    }
}
