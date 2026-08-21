package com.motm.manager;

import com.motm.model.PlayerData;
import com.motm.util.DataLoader;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * Handles XP calculation, level progression, rested bonus, and combo systems.
 * Translated from leveling_logic.pseudo.
 */
public class LevelingManager {

    private static final Logger LOG = Logger.getLogger("MOTM");

    public static final int MAX_LEVEL = 200;
    public static final int PERK_CAP_LEVEL = 100;
    public static final int STAT_CAP_LEVEL = 200;
    public static final int STAT_POINTS_PER_LEVEL = 2;
    public static final int BASE_XP = 100;
    public static final double SCALING_FACTOR = 1.5;
    public static final int MILESTONE_INTERVAL = 10;

    // XP Modifier Constants
    private static final double PARTY_BONUS_2P = 0.10;
    private static final double PARTY_BONUS_3P = 0.15;
    private static final double PARTY_BONUS_4P = 0.20;
    private static final double PARTY_BONUS_5P = 0.25;
    private static final double RESTED_BONUS_MAX = 0.50;
    private static final double RESTED_ACCUMULATION_RATE = 0.05; // per hour offline

    // XP Penalty Constants
    private static final int LEVEL_DIFF_PENALTY_THRESHOLD = 10;
    private static final double LEVEL_DIFF_PENALTY_PER_LEVEL = 0.05;
    private static final double MAX_LEVEL_PENALTY = 0.90;
    private static final int REPEAT_KILL_THRESHOLD = 10;
    private static final double REPEAT_KILL_PENALTY = 0.05;
    private static final double REPEAT_KILL_MAX_PENALTY = 0.50;
    private static final int REPEAT_KILL_RESET_MINUTES = 30;

    private final DataLoader dataLoader;
    private final PerkManager perkManager;

    // Fired on XP gain / level-up / milestone so the runtime layer (MenteesMod) can apply player
    // feedback (messages, stat recalculation) without this plain-Java manager touching runtime APIs.
    private ProgressionListener progressionListener = new ProgressionListener() {};

    public LevelingManager(DataLoader dataLoader, PerkManager perkManager) {
        this.dataLoader = dataLoader;
        this.perkManager = perkManager;
    }

    public void setProgressionListener(ProgressionListener listener) {
        this.progressionListener = (listener != null) ? listener : new ProgressionListener() {};
    }

    /** Runtime feedback hooks for progression events; default methods no-op for headless/tests. */
    public interface ProgressionListener {
        default void onXpGained(PlayerData player, int amount, String source) {}
        default void onLevelUp(PlayerData player, int oldLevel, int newLevel) {}
        default void onMilestoneReached(PlayerData player, int level, int tier) {}
    }

    // --- XP Formula ---

    public int calculateXpRequired(int level) {
        if (level < 0 || level >= MAX_LEVEL) return 0;
        return (int) Math.round(BASE_XP * Math.pow(level + 1, SCALING_FACTOR));
    }

    public int calculateTotalXpToLevel(int targetLevel) {
        int total = 0;
        for (int lvl = 0; lvl < targetLevel; lvl++) {
            total += calculateXpRequired(lvl);
        }
        return total;
    }

    // --- XP Gain Calculation ---

    public int calculateMobXp(PlayerData player, String mobType, int mobLevel, boolean isRare) {
        int baseXp = dataLoader.getMobBaseXp(mobType);
        double levelMultiplier = 1.0 + (mobLevel * 0.1);
        double scaledXp = baseXp * levelMultiplier;

        double difficultyMult = getDifficultyMultiplier(player.getLevel());
        scaledXp *= difficultyMult;

        // Apply bonuses
        scaledXp = applyXpModifiers(player, mobType, isRare, scaledXp);

        // Apply penalties
        scaledXp = applyXpPenalties(player, mobType, mobLevel, scaledXp);

        return Math.max((int) Math.round(scaledXp), 1);
    }

    public double getDifficultyMultiplier(int playerLevel) {
        int tier = playerLevel / 10;
        double multiplier = 1.0 + (tier * 0.20);
        return Math.min(multiplier, 5.0);
    }

    public double getPlayerMaxHealthMultiplier(int level) {
        return 1.0;
    }

    public double getPlayerAbilityPowerMultiplier(int level) {
        return 1.0;
    }

    public double getPlayerSustainMultiplier(int level) {
        return 1.0;
    }

    public double getVigorHealthMultiplier(PlayerData player) {
        return 1.0 + (points(player, "vigor") * 0.02);
    }

    public double getVigorDamageReduction(PlayerData player) {
        return points(player, "vigor") * 0.0025;
    }

    public double getTenacityDamageMultiplier(PlayerData player) {
        return 1.0 + (points(player, "tenacity") * 0.0075);
    }

    public double getTenacityCritDamageBonus(PlayerData player) {
        return points(player, "tenacity") * 0.005;
    }

    public double getEnduranceStaminaMultiplier(PlayerData player) {
        return 1.0 + (points(player, "endurance") * 0.02);
    }

    public double getEnduranceStaminaRegenBonus(PlayerData player) {
        return points(player, "endurance") * 0.01;
    }

    public double getAgilitySpeedMultiplier(PlayerData player) {
        return 1.0 + (points(player, "agility") * 0.0025);
    }

    public double getAgilityMeleeAttackSpeedBonus(PlayerData player) {
        return points(player, "agility") * 0.0005;
    }

    public double getLuckCritChanceBonus(PlayerData player) {
        return points(player, "luck") * 0.001;
    }

    public double getLuckXpMultiplier(PlayerData player) {
        return 1.0 + (points(player, "luck") * 0.005);
    }

    public double getPlayerAbilityDamageMultiplier(PlayerData player) {
        return getTenacityDamageMultiplier(player);
    }

    public double getPlayerSustainMultiplier(PlayerData player) {
        return 1.0;
    }

    public String describePlayerStatGrowth(PlayerData player) {
        if (player == null) {
            return "No stat table loaded";
        }
        return "Points " + player.getUnspentStatPoints()
                + " | Vigor HP +" + formatPercent(getVigorHealthMultiplier(player) - 1.0)
                + " DR +" + formatPercent(getVigorDamageReduction(player))
                + " | Tenacity DMG +" + formatPercent(getTenacityDamageMultiplier(player) - 1.0)
                + " | Endurance STA +" + formatPercent(getEnduranceStaminaMultiplier(player) - 1.0)
                + " | Agility SPD +" + formatPercent(getAgilitySpeedMultiplier(player) - 1.0)
                + " | Luck XP +" + formatPercent(getLuckXpMultiplier(player) - 1.0);
    }

    public boolean spendStatPoint(PlayerData player, String statId, int points) {
        if (player == null || points <= 0 || player.getUnspentStatPoints() < points) {
            return false;
        }
        if (!player.getStatAllocation().add(statId, points)) {
            return false;
        }
        player.setUnspentStatPoints(player.getUnspentStatPoints() - points);
        return true;
    }

    public void reconcileProgressionState(PlayerData player) {
        if (player == null) {
            return;
        }
        int expectedEarned = Math.max(0, Math.min(STAT_CAP_LEVEL, player.getLevel())) * STAT_POINTS_PER_LEVEL;
        if (player.getTotalStatPointsEarned() < expectedEarned) {
            int delta = expectedEarned - player.getTotalStatPointsEarned();
            player.setTotalStatPointsEarned(expectedEarned);
            player.setUnspentStatPoints(player.getUnspentStatPoints() + delta);
        }
        int maxPerkChoices = getPerkChoicesEarned(player.getLevel());
        if (player.getPerkSelectionPoints() < 0) {
            player.setPerkSelectionPoints(0);
        }
        int spentOrRecorded = Math.max(player.getSelectedPerks().size(), player.getPerkSelectionHistory().size());
        int pending = Math.max(0, maxPerkChoices - spentOrRecorded);
        player.setPerkSelectionPoints(Math.max(player.getPerkSelectionPoints(), pending));
        player.setPendingPerkTier(pending > 0 ? Math.min(maxPerkChoices, PerkManager.TOTAL_TIERS) : null);
    }

    public int getPerkChoicesEarned(int level) {
        return Math.max(0, Math.min(level, PERK_CAP_LEVEL) / MILESTONE_INTERVAL);
    }

    private int points(PlayerData player, String statId) {
        return player == null ? 0 : player.getStatAllocation().get(statId);
    }

    public int calculateAverageOnlineLevel(Collection<PlayerData> players) {
        if (players == null || players.isEmpty()) {
            return 0;
        }

        int totalLevels = 0;
        int count = 0;
        for (PlayerData player : players) {
            if (player == null) {
                continue;
            }
            totalLevels += Math.max(0, player.getLevel());
            count++;
        }

        if (count == 0) {
            return 0;
        }

        return Math.max(0, (int) Math.round(totalLevels / (double) count));
    }

    public String describePlayerStatGrowth(int level) {
        return "Stat table based; use /motm stats for allocated bonuses.";
    }

    // --- XP Modifiers ---

    private double applyXpModifiers(PlayerData player, String mobType, boolean isRare, double baseXp) {
        double modified = baseXp;

        // Party Bonus
        if (player.getPartySize() > 1) {
            modified *= (1 + getPartyBonus(player.getPartySize()));
        }

        // Rested Bonus
        if (player.getRestedBonus() > 0) {
            double restedMult = Math.min(player.getRestedBonus(), RESTED_BONUS_MAX);
            modified *= (1 + restedMult);
            consumeRestedBonus(player, baseXp);
        }

        // First Encounter Bonus (first time ever killing this mob type)
        if (!player.getStatistics().getMobsKilled().containsKey(mobType)) {
            modified *= 5.0;
        }

        // Rare Mob Bonus
        if (isRare) {
            modified *= 2.0;
        }

        // Combo Kill Bonus
        double comboBonus = getComboBonus(player);
        if (comboBonus > 0) {
            modified *= (1 + comboBonus);
        }

        return modified;
    }

    private double getPartyBonus(int partySize) {
        return switch (partySize) {
            case 2 -> PARTY_BONUS_2P;
            case 3 -> PARTY_BONUS_3P;
            case 4 -> PARTY_BONUS_4P;
            default -> PARTY_BONUS_5P;
        };
    }

    private double getComboBonus(PlayerData player) {
        if (player.getComboCount() < 5) return 0;
        int comboTier = Math.min(player.getComboCount() - 5, 10);
        return comboTier * 0.10;
    }

    // --- XP Penalties ---

    private double applyXpPenalties(PlayerData player, String mobType, int mobLevel, double baseXp) {
        double penalized = baseXp;

        // Level Difference Penalty
        int levelDiff = player.getLevel() - mobLevel;
        if (levelDiff > LEVEL_DIFF_PENALTY_THRESHOLD) {
            double penalty = (levelDiff - LEVEL_DIFF_PENALTY_THRESHOLD) * LEVEL_DIFF_PENALTY_PER_LEVEL;
            penalty = Math.min(penalty, MAX_LEVEL_PENALTY);
            penalized *= (1 - penalty);
        }

        // Repeat Kill Penalty
        int repeatCount = getRecentKillCount(player, mobType);
        if (repeatCount > REPEAT_KILL_THRESHOLD) {
            int excessKills = repeatCount - REPEAT_KILL_THRESHOLD;
            double penalty = Math.min(excessKills * REPEAT_KILL_PENALTY, REPEAT_KILL_MAX_PENALTY);
            penalized *= (1 - penalty);
        }

        return penalized;
    }

    private int getRecentKillCount(PlayerData player, String mobType) {
        long cutoff = System.currentTimeMillis() - (REPEAT_KILL_RESET_MINUTES * 60L * 1000L);
        int count = 0;
        for (var kill : player.getRecentKills()) {
            if (kill.getMobType().equals(mobType) && kill.getTimestamp() > cutoff) {
                count++;
            }
        }
        return count;
    }

    // --- Level Up Processing ---

    public void grantXp(PlayerData player, int xpAmount, String source) {
        if (player == null || xpAmount <= 0 || player.getLevel() >= MAX_LEVEL) {
            return;
        }
        int modifiedXp = Math.max(1, (int) Math.round(xpAmount * getLuckXpMultiplier(player)));
        player.setCurrentXp(player.getCurrentXp() + modifiedXp);
        player.setTotalXpEarned(player.getTotalXpEarned() + modifiedXp);

        while (player.getCurrentXp() >= calculateXpRequired(player.getLevel())
                && player.getLevel() < MAX_LEVEL) {
            processLevelUp(player);
        }

        LOG.fine("[MOTM] " + player.getPlayerName() + " gained " + modifiedXp + " XP from: " + source);
        progressionListener.onXpGained(player, modifiedXp, source);
    }

    private void processLevelUp(PlayerData player) {
        int xpRequired = calculateXpRequired(player.getLevel());
        player.setCurrentXp(player.getCurrentXp() - xpRequired);

        int oldLevel = player.getLevel();
        player.setLevel(oldLevel + 1);
        int newLevel = player.getLevel();

        if (newLevel <= STAT_CAP_LEVEL) {
            player.setTotalStatPointsEarned(player.getTotalStatPointsEarned() + STAT_POINTS_PER_LEVEL);
            player.setUnspentStatPoints(player.getUnspentStatPoints() + STAT_POINTS_PER_LEVEL);
        }

        // RecalculateStats + full restore + level-up feedback are applied by the runtime listener
        // (MenteesMod refreshes the entity's progression health bonus and messages the player).
        progressionListener.onLevelUp(player, oldLevel, newLevel);

        // Check for milestone (perk selection)
        if (newLevel <= PERK_CAP_LEVEL && newLevel % MILESTONE_INTERVAL == 0) {
            onMilestoneReached(player, newLevel);
        }
        LOG.info("[MOTM] " + player.getPlayerName() + " leveled up: " + oldLevel + " -> " + newLevel);
    }

    private void onMilestoneReached(PlayerData player, int level) {
        int tier = level / MILESTONE_INTERVAL;
        player.setPerkSelectionPoints(player.getPerkSelectionPoints() + 1);
        player.setPendingPerkTier(tier);

        LOG.info("[MOTM] " + player.getPlayerName() + " reached milestone level "
                + level + " (Tier " + tier + ")");
        // Milestone + perk-selection availability feedback is delivered by the runtime listener.
        progressionListener.onMilestoneReached(player, level, tier);
    }

    // --- Rested Bonus ---

    public void updateRestedOnLogin(PlayerData player) {
        Long lastLogout = player.getLastLogoutTimestamp();
        if (lastLogout == null) return;

        double hoursOffline = (System.currentTimeMillis() - lastLogout) / (1000.0 * 60 * 60);
        double accumulated = hoursOffline * RESTED_ACCUMULATION_RATE;
        double totalRested = player.getRestedBonus() + accumulated;
        player.setRestedBonus(Math.min(totalRested, RESTED_BONUS_MAX));

        LOG.fine("[MOTM] " + player.getPlayerName() + " has "
                + (int) (player.getRestedBonus() * 100) + "% rested bonus");
    }

    public void updateRestedOnLogout(PlayerData player) {
        player.setLastLogoutTimestamp(System.currentTimeMillis());
    }

    private void consumeRestedBonus(PlayerData player, double xpEarned) {
        double consumption = (xpEarned / 100.0) * 0.01;
        player.setRestedBonus(Math.max(0, player.getRestedBonus() - consumption));
    }

    private String formatPercent(double value) {
        return String.format("%.1f%%", Math.max(0.0, value) * 100.0);
    }

    // --- Combo System ---

    public void updateCombo(PlayerData player) {
        long currentTime = System.currentTimeMillis();

        if (player.getLastKillTime() == null) {
            player.setComboCount(1);
        } else {
            long timeSinceLast = currentTime - player.getLastKillTime();
            if (timeSinceLast <= 10000) { // 10 second window
                player.setComboCount(player.getComboCount() + 1);
            } else {
                player.setComboCount(1);
            }
        }

        player.setLastKillTime(currentTime);

        if (player.getComboCount() > player.getStatistics().getHighestCombo()) {
            player.getStatistics().setHighestCombo(player.getComboCount());
        }
    }

    // --- Mob Kill Handler ---

    public void onMobKilled(PlayerData player, String mobType, int mobLevel, boolean isRare) {
        updateCombo(player);
        int xpGained = calculateMobXp(player, mobType, mobLevel, isRare);
        grantXp(player, xpGained, "Killed " + mobType);
        recordKill(player, mobType, mobLevel);

        player.getStatistics().getMobsKilled().merge(mobType, 1, Integer::sum);
    }

    private void recordKill(PlayerData player, String mobType, int mobLevel) {
        player.getRecentKills().add(
            new PlayerData.KillRecord(mobType, mobLevel, System.currentTimeMillis())
        );

        // Prune old records
        long cutoff = System.currentTimeMillis() - (REPEAT_KILL_RESET_MINUTES * 60L * 1000L);
        player.getRecentKills().removeIf(k -> k.getTimestamp() < cutoff);
    }

    // --- XP Display Helpers ---

    public double getXpProgressPercent(PlayerData player) {
        int required = calculateXpRequired(player.getLevel());
        if (required == 0) return 100.0;
        return (player.getCurrentXp() / (double) required) * 100.0;
    }

    public int getXpToLevel(PlayerData player, int targetLevel) {
        if (targetLevel <= player.getLevel()) return 0;
        int totalNeeded = 0;
        for (int lvl = player.getLevel(); lvl < targetLevel; lvl++) {
            totalNeeded += calculateXpRequired(lvl);
        }
        return totalNeeded - player.getCurrentXp();
    }
}
