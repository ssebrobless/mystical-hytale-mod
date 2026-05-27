package com.motm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotmPluginDataDirectoriesTest {

    @TempDir
    Path tempDir;

    @Test
    void nonModsDirectoryStaysOperationalDirectoryAndGetsScannerSafeManifest() throws Exception {
        Path dataDir = tempDir.resolve("plain-data");

        Path resolved = MotmPluginDataDirectories.resolveOperationalDataDirectory(dataDir, Logger.getLogger("test"));

        assertEquals(dataDir.toAbsolutePath().normalize(), resolved);
        String manifest = Files.readString(dataDir.resolve("manifest.json"));
        assertTrue(manifest.contains("\"DisabledByDefault\": true"));
        assertTrue(manifest.contains("\"IncludesAssetPack\": false"));
    }

    @Test
    void saveModsDirectoryMigratesToMotmDataOutsideAssetScannedMods() throws Exception {
        Path saveRoot = tempDir.resolve("Saves/Main");
        Path legacyDir = saveRoot.resolve("mods/com.motm_Mentees of the Mystical");
        Files.createDirectories(legacyDir.resolve("players"));
        Files.writeString(legacyDir.resolve("players/alice.json"), "legacy-player");
        Path existingOperationalFile = saveRoot.resolve("motm-data/com.motm_Mentees of the Mystical/players/alice.json");
        Files.createDirectories(existingOperationalFile.getParent());
        Files.writeString(existingOperationalFile, "current-player");

        Path resolved = MotmPluginDataDirectories.resolveOperationalDataDirectory(legacyDir, Logger.getLogger("test"));

        Path expected = saveRoot.resolve("motm-data/com.motm_Mentees of the Mystical");
        assertEquals(expected.toAbsolutePath().normalize(), resolved.toAbsolutePath().normalize());
        assertEquals("current-player", Files.readString(existingOperationalFile));
        assertTrue(Files.exists(legacyDir.resolve("manifest.json")));
    }
}
