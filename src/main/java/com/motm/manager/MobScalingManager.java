package com.motm.manager;

import com.motm.model.MobStats;
import com.motm.util.DataLoader;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

/**
 * Handles global mob scaling based on player level.
 * Translated from mob_scaling_logic.pseudo.
 */
public class MobScalingManager {

    private static final Logger LOG = Logger.getLogger("MOTM");

    // Scaling constants
    private static final double DIFFICULTY_INCREASE_PER_10_LEVELS = 0.20;
    private static final double MAX_DIFFICULTY_MULTIPLIER = 5.0;
    private static final double MAX_HEALTH_MULTIPLIER = 10.0;
    private static final double MAX_DAMAGE_MULTIPLIER = 5.0;
    private static final double MAX_SPEED_MULTIPLIER = 2.0;
    private static final double SPEED_SCALING_FACTOR = 0.25;
    private static final double BOSS_DIFFICULTY_INCREASE_PER_10_LEVELS = 0.12;
    private static final double WORLD_BOSS_DIFFICULTY_INCREASE_PER_10_LEVELS = 0.10;
    private static final double MAX_BOSS_DIFFICULTY_MULTIPLIER = 3.5;
    private static final double MAX_WORLD_BOSS_DIFFICULTY_MULTIPLIER = 3.0;

    private static final Set<String> SCALING_CATEGORIES = Set.of("hostile", "elite", "mini_boss");
    private static final Set<String> BOSS_SCALING_CATEGORIES = Set.of("boss", "world_boss");
    private static final Set<String> NON_SCALING_CATEGORIES = Set.of("passive", "neutral", "boss", "world_boss");

    // Night/event constants
    private static final double NIGHT_MULTIPLIER = 1.2;
    private static final double BLOOD_MOON_MULTIPLIER = 2.0;
    private static final double DUNGEON_MULTIPLIER = 1.5;

    private final DataLoader dataLoader;

    public MobScalingManager(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    // --- Difficulty Calculation ---

    public double getDifficultyMultiplier(int playerLevel) {
        int tier = playerLevel / 10;
        double multiplier = 1.0 + (tier * DIFFICULTY_INCREASE_PER_10_LEVELS);
        return Math.min(multiplier, MAX_DIFFICULTY_MULTIPLIER);
    }

    public boolean isScalingCategory(String mobCategory) {
        return SCALING_CATEGORIES.contains(mobCategory);
    }

    public boolean isBossCategory(String mobCategory) {
        return BOSS_SCALING_CATEGORIES.contains(mobCategory);
    }

    public String getDifficultyDescription(int playerLevel) {
        double multiplier = getDifficultyMultiplier(playerLevel);
        int percentIncrease = (int) ((multiplier - 1.0) * 100);
        return percentIncrease == 0 ? "Base Difficulty" : "+" + percentIncrease + "% Difficulty";
    }

    public double getBossDifficultyMultiplier(int playerLevel, String mobCategory) {
        double increasePerTier = "world_boss".equals(mobCategory)
                ? WORLD_BOSS_DIFFICULTY_INCREASE_PER_10_LEVELS
                : BOSS_DIFFICULTY_INCREASE_PER_10_LEVELS;
        double maxMultiplier = "world_boss".equals(mobCategory)
                ? MAX_WORLD_BOSS_DIFFICULTY_MULTIPLIER
                : MAX_BOSS_DIFFICULTY_MULTIPLIER;

        int tier = playerLevel / 10;
        double multiplier = 1.0 + (tier * increasePerTier);
        return Math.min(multiplier, maxMultiplier);
    }

    public String getBossDifficultyDescription(int playerLevel, String mobCategory) {
        double multiplier = getBossDifficultyMultiplier(playerLevel, mobCategory);
        int percentIncrease = (int) ((multiplier - 1.0) * 100);
        return percentIncrease == 0 ? "Base Boss Difficulty" : "+" + percentIncrease + "% Boss Difficulty";
    }

    // --- Mob Stat Scaling ---

    public MobStats scaleMobStats(MobStats baseStats, int playerLevel, String mobCategory) {
        if (!SCALING_CATEGORIES.contains(mobCategory)) {
            return baseStats; // Non-scaling mobs returned unmodified
        }

        double difficulty = getDifficultyMultiplier(playerLevel);
        MobStats scaled = new MobStats(baseStats);

        // Health: Linear scaling
        scaled.setHealth(Math.min(baseStats.getHealth() * difficulty,
                baseStats.getHealth() * MAX_HEALTH_MULTIPLIER));

        // Damage: Linear scaling
        scaled.setDamage(Math.min(baseStats.getDamage() * difficulty,
                baseStats.getDamage() * MAX_DAMAGE_MULTIPLIER));

        // Speed: Reduced scaling (25% of difficulty increase)
        double speedIncrease = (difficulty - 1) * SPEED_SCALING_FACTOR;
        scaled.setSpeed(Math.min(baseStats.getSpeed() * (1 + speedIncrease),
                baseStats.getSpeed() * MAX_SPEED_MULTIPLIER));

        // Armor: 50% of difficulty increase
        double armorIncrease = (difficulty - 1) * 0.5;
        scaled.setArmor(baseStats.getArmor() * (1 + armorIncrease));

        // Magic Resist: Same as armor
        double mrIncrease = (difficulty - 1) * 0.5;
        scaled.setMagicResist(baseStats.getMagicResist() * (1 + mrIncrease));

        // Attack Speed: Slight scaling
        double asIncrease = (difficulty - 1) * 0.15;
        scaled.setAttackSpeed(baseStats.getAttackSpeed() * (1 + asIncrease));

        // XP Reward: Linear scaling
        scaled.setXpReward(baseStats.getXpReward() * difficulty);

        return scaled;
    }

    public MobStats scaleBossStats(MobStats baseStats, int playerLevel, String mobCategory) {
        if (!BOSS_SCALING_CATEGORIES.contains(mobCategory)) {
            return baseStats;
        }

        double difficulty = getBossDifficultyMultiplier(playerLevel, mobCategory);
        double difficultyDelta = difficulty - 1.0;
        MobStats scaled = new MobStats(baseStats);

        double healthMultiplier = 1.0 + (difficultyDelta * 1.35);
        double damageMultiplier = 1.0 + (difficultyDelta * 0.85);
        double armorMultiplier = 1.0 + (difficultyDelta * 0.65);
        double speedMultiplier = 1.0 + (difficultyDelta * 0.10);
        double attackSpeedMultiplier = 1.0 + (difficultyDelta * 0.12);

        scaled.setHealth(Math.min(baseStats.getHealth() * healthMultiplier,
                baseStats.getHealth() * (MAX_HEALTH_MULTIPLIER * 1.25)));
        scaled.setDamage(Math.min(baseStats.getDamage() * damageMultiplier,
                baseStats.getDamage() * (MAX_DAMAGE_MULTIPLIER * 1.15)));
        scaled.setArmor(baseStats.getArmor() * armorMultiplier);
        scaled.setMagicResist(baseStats.getMagicResist() * armorMultiplier);
        scaled.setSpeed(Math.min(baseStats.getSpeed() * speedMultiplier,
                baseStats.getSpeed() * MAX_SPEED_MULTIPLIER));
        scaled.setAttackSpeed(baseStats.getAttackSpeed() * attackSpeedMultiplier);
        scaled.setXpReward(baseStats.getXpReward() * difficulty);

        return scaled;
    }

    // --- Mob Level Assignment ---

    public int assignMobLevel(int playerLevel) {
        int baseLevel = (int) (playerLevel * 0.9);
        int variation = ThreadLocalRandom.current().nextInt(-2, 3); // -2 to +2
        return Math.max(1, Math.min(200, baseLevel + variation));
    }

    public int assignMobLevelForArea(int playerLevel, int areaLevelModifier) {
        int base = assignMobLevel(playerLevel);
        return Math.max(1, Math.min(200, base + areaLevelModifier));
    }

    // --- Party Scaling ---

    public MobStats applyPartyScaling(MobStats stats, int partySize) {
        if (partySize <= 1) return stats;

        double partyMult = 1.0 + ((partySize - 1) * 0.15);
        MobStats scaled = new MobStats(stats);

        // Only scale health and XP reward for party
        scaled.setHealth(stats.getHealth() * partyMult);
        scaled.setXpReward(stats.getXpReward() * partyMult);

        return scaled;
    }

    // --- Environmental Modifiers ---

    public MobStats applyNightBonus(MobStats stats) {
        MobStats scaled = new MobStats(stats);
        scaled.setHealth(stats.getHealth() * NIGHT_MULTIPLIER);
        scaled.setDamage(stats.getDamage() * NIGHT_MULTIPLIER);
        return scaled;
    }

    public MobStats applyBloodMoonBonus(MobStats stats) {
        MobStats scaled = new MobStats(stats);
        scaled.setHealth(stats.getHealth() * BLOOD_MOON_MULTIPLIER);
        scaled.setDamage(stats.getDamage() * BLOOD_MOON_MULTIPLIER);
        scaled.setSpeed(stats.getSpeed() * 1.25);
        scaled.setXpReward(stats.getXpReward() * BLOOD_MOON_MULTIPLIER);
        return scaled;
    }

    public MobStats applyDungeonBonus(MobStats stats) {
        MobStats scaled = new MobStats(stats);
        scaled.setHealth(stats.getHealth() * DUNGEON_MULTIPLIER);
        scaled.setDamage(stats.getDamage() * DUNGEON_MULTIPLIER);
        scaled.setXpReward(stats.getXpReward() * DUNGEON_MULTIPLIER);
        return scaled;
    }

    public boolean canBecomeElite(String mobCategory) {
        return "hostile".equals(mobCategory) || "neutral".equals(mobCategory);
    }

    public MobStats tryMakeElite(MobStats stats, String zone, String mobType) {
        if (stats == null || zone == null || zone.isBlank() || stats.isElite()) {
            return stats;
        }

        if (ThreadLocalRandom.current().nextDouble() >= dataLoader.getEliteSpawnChance()) {
            return stats;
        }

        List<java.util.Map.Entry<String, String>> titlePool =
                new java.util.ArrayList<>(dataLoader.getEliteTitlesForZone(zone).entrySet());
        if (titlePool.isEmpty()) {
            LOG.fine("[MOTM] No elite titles configured for zone " + zone + "; skipping elite roll for " + mobType);
            return stats;
        }

        java.util.Map.Entry<String, String> titleEntry =
                titlePool.get(ThreadLocalRandom.current().nextInt(titlePool.size()));

        MobStats eliteStats = new MobStats(stats);
        eliteStats.setHealth(stats.getHealth() * dataLoader.getEliteHpMultiplier());
        eliteStats.setDamage(stats.getDamage() * dataLoader.getEliteDamageMultiplier());
        eliteStats.setXpReward(stats.getXpReward() * dataLoader.getEliteXpMultiplier());
        eliteStats.setElite(true);
        eliteStats.setEliteTitle(titleEntry.getValue());

        LOG.info("[MOTM] Spawned elite " + mobType + " in " + zone
                + " with title \"" + titleEntry.getValue() + "\" (style " + titleEntry.getKey() + ")");
        return eliteStats;
    }

    // --- Display Helpers ---

    public String formatMobName(String mobType, int level, String category) {
        String displayName = formatDisplayName(mobType);
        return switch (category) {
            case "boss" -> "\u2605 [Lv. " + level + "] " + displayName + " \u2605";
            case "mini_boss" -> "\u2B25 [Lv. " + level + "] " + displayName;
            case "elite" -> "\u25C6 [Lv. " + level + "] " + displayName;
            default -> "[Lv. " + level + "] " + displayName;
        };
    }

    public String formatEliteMobName(String mobType, int level, String eliteTitle) {
        return "\u25C6 [Lv. " + level + "] " + eliteTitle + " " + formatDisplayName(mobType);
    }

    /**
     * Returns a color hex code based on level difference for UI display.
     */
    public String getLevelColor(int mobLevel, int playerLevel) {
        int diff = mobLevel - playerLevel;
        if (diff <= -20) return "#808080"; // Grey - Trivial
        if (diff <= -10) return "#00FF00"; // Green - Easy
        if (diff <= 5)   return "#FFFF00"; // Yellow - Normal
        if (diff <= 10)  return "#FFA500"; // Orange - Hard
        if (diff <= 20)  return "#FF0000"; // Red - Dangerous
        return "#8B0000"; // Dark Red - Deadly
    }

    public boolean shouldShowSkullIcon(int mobLevel, int playerLevel) {
        return (mobLevel - playerLevel) > 20;
    }

    private String formatDisplayName(String mobType) {
        String[] words = mobType.split("_");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            if (i > 0) sb.append(' ');
            sb.append(Character.toUpperCase(words[i].charAt(0)));
            if (words[i].length() > 1) sb.append(words[i].substring(1).toLowerCase());
        }
        return sb.toString();
    }
}
