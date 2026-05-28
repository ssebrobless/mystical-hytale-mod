package com.motm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotmServerConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void createsDefaultConfigWhenMissing() throws Exception {
        MotmServerConfig config = MotmServerConfig.loadOrCreate(tempDir);

        assertFalse(config.devToolsEnabled());
        assertEquals("key", config.observabilityPacketScope());
        assertTrue(Files.exists(tempDir.resolve(MotmServerConfig.FILE_NAME)));
    }

    @Test
    void preservesExistingValues() throws Exception {
        Path configPath = tempDir.resolve(MotmServerConfig.FILE_NAME);
        Files.writeString(configPath, """
                dev_tools_enabled=true
                observability_packet_scope=full
                """);

        MotmServerConfig config = MotmServerConfig.loadOrCreate(tempDir);

        assertTrue(config.devToolsEnabled());
        assertEquals("full", config.observabilityPacketScope());
    }

    @Test
    void backfillsPacketScopeForOlderConfig() throws Exception {
        Path configPath = tempDir.resolve(MotmServerConfig.FILE_NAME);
        Files.writeString(configPath, "dev_tools_enabled=true\n");

        MotmServerConfig config = MotmServerConfig.loadOrCreate(tempDir);

        assertTrue(config.devToolsEnabled());
        assertEquals("key", config.observabilityPacketScope());
        assertTrue(Files.readString(configPath).contains("observability_packet_scope=key"));
    }
}
