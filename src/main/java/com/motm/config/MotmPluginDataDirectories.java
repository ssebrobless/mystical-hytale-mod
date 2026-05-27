package com.motm.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.logging.Logger;

/**
 * Resolves MOTM runtime data outside Hytale's asset-scanned mod directory.
 */
public final class MotmPluginDataDirectories {

    private MotmPluginDataDirectories() {
    }

    public static Path resolveOperationalDataDirectory(Path hytaleDataDir, Logger log) {
        if (hytaleDataDir == null) {
            return null;
        }

        Path normalizedDataDir = hytaleDataDir.toAbsolutePath().normalize();
        writeScannerSafeLegacyManifest(normalizedDataDir, log);

        Path parent = normalizedDataDir.getParent();
        if (parent == null
                || parent.getFileName() == null
                || !"mods".equalsIgnoreCase(parent.getFileName().toString())) {
            return normalizedDataDir;
        }

        Path saveRoot = parent.getParent();
        if (saveRoot == null || normalizedDataDir.getFileName() == null) {
            return normalizedDataDir;
        }

        Path operationalDataDir = saveRoot.resolve("motm-data").resolve(normalizedDataDir.getFileName().toString());
        migrateLegacyPluginDataDirectory(normalizedDataDir, operationalDataDir, log);
        if (log != null) {
            log.info("[MOTM] Using operational data directory outside asset-scanned mods folder: "
                    + operationalDataDir);
        }
        return operationalDataDir;
    }

    static void migrateLegacyPluginDataDirectory(Path legacyDataDir, Path operationalDataDir, Logger log) {
        if (legacyDataDir == null || operationalDataDir == null || !Files.exists(legacyDataDir)) {
            return;
        }

        try {
            Files.createDirectories(operationalDataDir);
            try (var paths = Files.walk(legacyDataDir)) {
                paths.sorted(Comparator.naturalOrder()).forEach(source -> {
                    Path target = operationalDataDir.resolve(legacyDataDir.relativize(source));
                    try {
                        if (Files.isDirectory(source)) {
                            Files.createDirectories(target);
                        } else {
                            Files.createDirectories(target.getParent());
                            if (!Files.exists(target)) {
                                Files.copy(source, target);
                            }
                        }
                    } catch (IOException e) {
                        throw new IllegalStateException(e);
                    }
                });
            }
            if (log != null) {
                log.info("[MOTM] Migrated legacy plugin data from asset-scanned mods folder: " + legacyDataDir);
            }
        } catch (RuntimeException | IOException e) {
            if (log != null) {
                log.warning("[MOTM] Failed to migrate legacy plugin data from " + legacyDataDir
                        + " to " + operationalDataDir + ": " + e.getMessage());
            }
        }
    }

    static void writeScannerSafeLegacyManifest(Path hytaleDataDir, Logger log) {
        if (hytaleDataDir == null) {
            return;
        }

        try {
            Files.createDirectories(hytaleDataDir);
            Path manifestPath = hytaleDataDir.resolve("manifest.json");
            Files.writeString(manifestPath, """
                    {
                      "Group": "com.motm.runtime",
                      "Name": "MOTM Runtime Data",
                      "Version": "1.0.1",
                      "Description": "Scanner-safe runtime data folder for Mentees of the Mystical.",
                      "Authors": [
                        {
                          "Name": "fishe"
                        }
                      ],
                      "Website": "",
                      "ServerVersion": "*",
                      "Dependencies": {},
                      "OptionalDependencies": {},
                      "DisabledByDefault": true,
                      "IncludesAssetPack": false
                    }
                    """);
        } catch (IOException e) {
            if (log != null) {
                log.warning("[MOTM] Failed to write scanner-safe manifest for legacy data directory "
                        + hytaleDataDir + ": " + e.getMessage());
            }
        }
    }
}
