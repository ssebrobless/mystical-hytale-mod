package com.motm.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Typed owner for MOTM server-side plugin settings.
 */
public record MotmServerConfig(
        boolean devToolsEnabled,
        String observabilityPacketScope,
        Path path
) {

    public static final String FILE_NAME = "motm-server.properties";
    private static final String DEV_TOOLS_ENABLED = "dev_tools_enabled";
    private static final String OBSERVABILITY_PACKET_SCOPE = "observability_packet_scope";
    private static final String NOTES = "notes";
    private static final String DEFAULT_PACKET_SCOPE = "key";

    public static MotmServerConfig disabled(Path pluginDirectory) {
        return new MotmServerConfig(false, DEFAULT_PACKET_SCOPE, configPath(pluginDirectory));
    }

    public static MotmServerConfig loadOrCreate(Path pluginDirectory) throws IOException {
        if (pluginDirectory == null) {
            throw new IOException("plugin directory is unavailable");
        }
        Files.createDirectories(pluginDirectory);
        Path path = configPath(pluginDirectory);
        Properties properties = new Properties();

        boolean changed = false;
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path)) {
                properties.load(reader);
            }
        } else {
            properties.setProperty(DEV_TOOLS_ENABLED, "false");
            properties.setProperty(NOTES, "Set dev_tools_enabled=true to enable /motm dev and the Dev Grimoire.");
            changed = true;
        }

        if (!properties.containsKey(OBSERVABILITY_PACKET_SCOPE)) {
            properties.setProperty(OBSERVABILITY_PACKET_SCOPE, DEFAULT_PACKET_SCOPE);
            changed = true;
        }

        if (changed) {
            try (var writer = Files.newBufferedWriter(path)) {
                properties.store(writer, "Mentees of the Mystical server settings");
            }
        }

        return new MotmServerConfig(
                Boolean.parseBoolean(properties.getProperty(DEV_TOOLS_ENABLED, "false")),
                properties.getProperty(OBSERVABILITY_PACKET_SCOPE, DEFAULT_PACKET_SCOPE),
                path
        );
    }

    private static Path configPath(Path pluginDirectory) {
        return pluginDirectory != null ? pluginDirectory.resolve(FILE_NAME) : Path.of(FILE_NAME);
    }
}
