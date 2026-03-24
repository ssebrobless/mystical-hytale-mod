package com.motm.manager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.motm.model.PlayerData;
import com.motm.util.DataLoader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Handles player data storage, loading, saving, and management.
 * Translated from player_data_logic.pseudo.
 */
public class PlayerDataManager {

    private static final Logger LOG = Logger.getLogger("MOTM");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final String PLAYER_DATA_DIR = "saves/players/";
    private static final String BACKUP_DIR = "saves/backups/";
    private static final int MAX_BACKUPS = 5;
    private static final long AUTO_SAVE_INTERVAL_MS = 300_000; // 5 minutes

    private final Path pluginDirectory;
    private final DataLoader dataLoader;
    private final Map<String, PlayerData> onlinePlayers = new ConcurrentHashMap<>();
    private final ScheduledExecutorService autoSaveScheduler = Executors.newSingleThreadScheduledExecutor();

    public PlayerDataManager(Path pluginDirectory, DataLoader dataLoader) {
        this.pluginDirectory = pluginDirectory;
        this.dataLoader = dataLoader;

        // Ensure directories exist
        try {
            Files.createDirectories(pluginDirectory.resolve(PLAYER_DATA_DIR));
            Files.createDirectories(pluginDirectory.resolve(BACKUP_DIR));
        } catch (IOException e) {
            LOG.severe("[MOTM] Failed to create data directories: " + e.getMessage());
        }
    }

    // --- Player Data Creation ---

    public PlayerData createNewPlayerData(String playerId, String playerName) {
        PlayerData data = new PlayerData();
        data.setPlayerId(playerId);
        data.setPlayerName(playerName);
        data.setPlayerClass(null);
        data.setLevel(1);
        data.setCurrentXp(0);
        data.setTotalXpEarned(0);
        data.setFirstJoin(true);
        data.getMetadata().setCreatedAt(Instant.now().toString());
        data.getMetadata().setLastPlayed(Instant.now().toString());
        data.getMetadata().setModVersion("1.0.0");
        data.getMetadata().setSchemaVersion(1);
        data.initRuntimeFields();
        return data;
    }

    // --- Save/Load ---

    public boolean savePlayerData(PlayerData player) {
        try {
            player.getMetadata().setLastPlayed(Instant.now().toString());
            String json = GSON.toJson(player);

            createBackup(player.getPlayerId());

            Path filePath = getPlayerFilePath(player.getPlayerId());
            Files.writeString(filePath, json, StandardCharsets.UTF_8);

            LOG.fine("[MOTM] Saved player data for " + player.getPlayerName());
            return true;
        } catch (IOException e) {
            LOG.severe("[MOTM] Failed to save player data: " + e.getMessage());
            return false;
        }
    }

    public PlayerData loadPlayerData(String playerId) {
        Path filePath = getPlayerFilePath(playerId);
        if (!Files.exists(filePath)) {
            LOG.fine("[MOTM] No existing save for player " + playerId);
            return null;
        }

        try {
            String json = Files.readString(filePath, StandardCharsets.UTF_8);
            PlayerData data = GSON.fromJson(json, PlayerData.class);
            data = migratePlayerData(data);

            if (!validatePlayerData(data)) {
                LOG.warning("[MOTM] Invalid player data, attempting backup recovery");
                data = recoverFromBackup(playerId);
            }

            if (data != null) {
                data.initRuntimeFields();
                LOG.fine("[MOTM] Loaded player data for " + data.getPlayerName());
            }
            return data;
        } catch (Exception e) {
            LOG.severe("[MOTM] Failed to load player data: " + e.getMessage());
            return recoverFromBackup(playerId);
        }
    }

    private Path getPlayerFilePath(String playerId) {
        return pluginDirectory.resolve(PLAYER_DATA_DIR + playerId + ".json");
    }

    // --- Validation ---

    public boolean validatePlayerData(PlayerData data) {
        if (data == null) return false;
        List<String> errors = new ArrayList<>();

        if (data.getPlayerId() == null || data.getPlayerId().isEmpty()) {
            errors.add("Missing player_id");
        }
        if (data.getLevel() < 1 || data.getLevel() > 200) {
            errors.add("Invalid level: " + data.getLevel());
        }
        if (data.getCurrentXp() < 0) {
            errors.add("Negative current_xp: " + data.getCurrentXp());
        }

        Set<String> validClasses = Set.of("terra", "hydro", "aero", "corruptus");
        if (data.getPlayerClass() != null && !validClasses.contains(data.getPlayerClass())) {
            errors.add("Invalid class: " + data.getPlayerClass());
        }

        if (!errors.isEmpty()) {
            for (String error : errors) {
                LOG.warning("[MOTM] Validation error: " + error);
            }
            return false;
        }
        return true;
    }

    // --- Migration ---

    private PlayerData migratePlayerData(PlayerData data) {
        if (data.getMetadata() == null) {
            data.getMetadata().setSchemaVersion(1);
        }
        // Future migrations go here
        return data;
    }

    // --- Backup System ---

    private void createBackup(String playerId) {
        Path sourcePath = getPlayerFilePath(playerId);
        if (!Files.exists(sourcePath)) return;

        try {
            String timestamp = Instant.now().toString().replace(":", "-").replace(".", "-");
            Path backupPath = pluginDirectory.resolve(
                    BACKUP_DIR + playerId + "_" + timestamp + ".json");
            Files.copy(sourcePath, backupPath, StandardCopyOption.REPLACE_EXISTING);
            pruneOldBackups(playerId);
            LOG.fine("[MOTM] Created backup: " + backupPath.getFileName());
        } catch (IOException e) {
            LOG.warning("[MOTM] Failed to create backup: " + e.getMessage());
        }
    }

    private void pruneOldBackups(String playerId) {
        try {
            Path backupDir = pluginDirectory.resolve(BACKUP_DIR);
            List<Path> backups = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir,
                    playerId + "_*.json")) {
                for (Path p : stream) {
                    backups.add(p);
                }
            }

            // Sort newest first
            backups.sort((a, b) -> {
                try {
                    return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                } catch (IOException e) {
                    return 0;
                }
            });

            // Delete old backups beyond MAX_BACKUPS
            for (int i = MAX_BACKUPS; i < backups.size(); i++) {
                Files.deleteIfExists(backups.get(i));
            }
        } catch (IOException e) {
            LOG.warning("[MOTM] Failed to prune backups: " + e.getMessage());
        }
    }

    private PlayerData recoverFromBackup(String playerId) {
        try {
            Path backupDir = pluginDirectory.resolve(BACKUP_DIR);
            List<Path> backups = new ArrayList<>();

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(backupDir,
                    playerId + "_*.json")) {
                for (Path p : stream) {
                    backups.add(p);
                }
            }

            // Sort newest first
            backups.sort((a, b) -> {
                try {
                    return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                } catch (IOException e) {
                    return 0;
                }
            });

            for (Path backup : backups) {
                try {
                    String json = Files.readString(backup, StandardCharsets.UTF_8);
                    PlayerData data = GSON.fromJson(json, PlayerData.class);
                    if (validatePlayerData(data)) {
                        data.initRuntimeFields();
                        LOG.info("[MOTM] Recovered from backup: " + backup.getFileName());
                        return data;
                    }
                } catch (Exception ignored) {}
            }
        } catch (IOException e) {
            LOG.severe("[MOTM] Backup recovery failed: " + e.getMessage());
        }

        LOG.severe("[MOTM] All backups failed for player " + playerId);
        return null;
    }

    // --- Online Player Management ---

    public PlayerData onPlayerJoin(String playerId, String playerName) {
        PlayerData data = loadPlayerData(playerId);

        if (data == null) {
            data = createNewPlayerData(playerId, playerName);
            LOG.info("[MOTM] Created new player: " + playerName);
        } else {
            data.setPlayerName(playerName); // Update if changed
        }

        data.setOnline(true);
        data.setSessionStart(System.currentTimeMillis());
        onlinePlayers.put(playerId, data);

        return data;
    }

    public void onPlayerDisconnect(String playerId) {
        PlayerData player = onlinePlayers.remove(playerId);
        if (player == null) return;

        // Update playtime
        long sessionTime = System.currentTimeMillis() - player.getSessionStart();
        player.getStatistics().setPlaytimeSeconds(
                player.getStatistics().getPlaytimeSeconds() + (int) (sessionTime / 1000));

        player.setOnline(false);
        savePlayerData(player);

        LOG.info("[MOTM] " + player.getPlayerName() + " disconnected, data saved.");
    }

    public PlayerData getOnlinePlayer(String playerId) {
        return onlinePlayers.get(playerId);
    }

    public Collection<PlayerData> getAllOnlinePlayers() {
        return onlinePlayers.values();
    }

    // --- Class Selection ---

    public boolean selectClass(PlayerData player, String classId) {
        if (player.getPlayerClass() != null) {
            LOG.warning("[MOTM] Player already has a class");
            return false;
        }

        if (!dataLoader.isValidClass(classId)) {
            LOG.warning("[MOTM] Invalid class: " + classId);
            return false;
        }

        player.setPlayerClass(classId);
        player.setFirstJoin(false);
        savePlayerData(player);

        LOG.info("[MOTM] " + player.getPlayerName() + " selected class: " + classId);
        return true;
    }

    // --- Auto-Save ---

    public void startAutoSave() {
        autoSaveScheduler.scheduleAtFixedRate(() -> {
            for (PlayerData player : onlinePlayers.values()) {
                if (player.isOnline()) {
                    savePlayerData(player);
                }
            }
        }, AUTO_SAVE_INTERVAL_MS, AUTO_SAVE_INTERVAL_MS, TimeUnit.MILLISECONDS);
        LOG.info("[MOTM] Auto-save started (interval: " + (AUTO_SAVE_INTERVAL_MS / 1000) + "s)");
    }

    public void stopAutoSave() {
        autoSaveScheduler.shutdown();
    }

    // --- Save All (for shutdown) ---

    public void saveAll() {
        for (PlayerData player : onlinePlayers.values()) {
            savePlayerData(player);
        }
        LOG.info("[MOTM] Saved all online player data.");
    }

    // --- Statistics ---

    public void recordBossDefeat(PlayerData player, String bossId) {
        if (!player.getStatistics().getBossesDefeated().contains(bossId)) {
            player.getStatistics().getBossesDefeated().add(bossId);
        }
    }

    // --- Achievement System ---

    public boolean unlockAchievement(PlayerData player, String achievementId) {
        if (player.getAchievements().contains(achievementId)) return false;
        player.getAchievements().add(achievementId);
        savePlayerData(player);
        LOG.info("[MOTM] " + player.getPlayerName() + " unlocked: " + achievementId);
        return true;
    }

    public void checkAchievements(PlayerData player, String trigger, Object context) {
        switch (trigger) {
            case "level_up" -> {
                int level = (int) context;
                if (level == 10) unlockAchievement(player, "reach_level_10");
                if (level == 50) unlockAchievement(player, "reach_level_50");
                if (level == 100) unlockAchievement(player, "reach_level_100");
                if (level == 200) unlockAchievement(player, "reach_max_level");
            }
            case "mob_killed" -> {
                int totalKills = player.getStatistics().getMobsKilled().values().stream()
                        .mapToInt(Integer::intValue).sum();
                if (totalKills == 1) unlockAchievement(player, "first_blood");
                if (totalKills >= 100) unlockAchievement(player, "century_slayer");
                if (totalKills >= 1000) unlockAchievement(player, "thousand_souls");
            }
            case "boss_defeated" -> {
                int bossCount = player.getStatistics().getBossesDefeated().size();
                if (bossCount == 1) unlockAchievement(player, "boss_slayer");
                if (bossCount >= 10) unlockAchievement(player, "boss_hunter");
            }
            case "perks_selected" -> {
                int perkCount = player.getSelectedPerks().size();
                if (perkCount == 3) unlockAchievement(player, "first_perks");
                if (perkCount >= 30) unlockAchievement(player, "perk_collector");
                if (perkCount >= 60) unlockAchievement(player, "fully_perked");
            }
        }
    }
}
