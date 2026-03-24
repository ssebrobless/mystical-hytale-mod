package com.motm.model;

import com.google.gson.annotations.SerializedName;
import java.util.*;

public class PlayerData {

    @SerializedName("player_id")
    private String playerId;
    @SerializedName("player_name")
    private String playerName;
    @SerializedName("class")
    private String playerClass;
    private int level = 1;
    @SerializedName("current_xp")
    private int currentXp = 0;
    @SerializedName("total_xp_earned")
    private int totalXpEarned = 0;
    @SerializedName("selected_perks")
    private List<String> selectedPerks = new ArrayList<>();
    @SerializedName("perk_selection_history")
    private List<PerkSelectionRecord> perkSelectionHistory = new ArrayList<>();
    @SerializedName("perk_selection_points")
    private int perkSelectionPoints = 0;
    @SerializedName("pending_perk_tier")
    private Integer pendingPerkTier = null;
    @SerializedName("first_join")
    private boolean firstJoin = true;
    private Statistics statistics = new Statistics();
    private List<String> achievements = new ArrayList<>();
    private Settings settings = new Settings();
    private Metadata metadata = new Metadata();

    // Phase 1 additions: race, styles, resources
    private String race;
    @SerializedName("selected_styles")
    private List<String> selectedStyles = new ArrayList<>();
    @SerializedName("class_resources")
    private Map<String, Integer> classResources = new HashMap<>();
    @SerializedName("water_container_tier")
    private int waterContainerTier = 0;

    // --- Runtime fields (not serialized) ---
    private transient boolean online = false;
    private transient long sessionStart = 0;
    private transient List<KillRecord> recentKills = new ArrayList<>();
    private transient int comboCount = 0;
    private transient Long lastKillTime = null;
    private transient int partySize = 1;
    private transient double restedBonus = 0;
    private transient Long lastLogoutTimestamp = null;
    private transient Map<String, Double> synergyDamageReduction = new HashMap<>();
    private transient Map<String, Double> synergyDamageIncrease = new HashMap<>();
    private transient Map<String, Double> synergyStatBonuses = new HashMap<>();
    private transient double synergyHealingBonus = 0;
    private transient double synergyCritChance = 0;
    private transient double synergyCritDamage = 0;
    private transient Map<String, Double> synergyCooldownReduction = new HashMap<>();
    private transient Map<String, Double> synergyDurationBonus = new HashMap<>();
    private transient double synergyChainBonus = 0;
    private transient double synergyRadiusBonus = 0;
    private transient List<ActiveSynergy> activeSynergyBonuses = new ArrayList<>();
    private transient int raceHpBonus = 0;
    private transient Map<String, Double> raceDamageReduction = new HashMap<>();
    private transient Map<String, Double> raceDamageIncrease = new HashMap<>();
    private transient Map<String, Double> raceStatBonuses = new HashMap<>();
    private transient double raceHealingReceivedBonus = 0;
    private transient double raceCritChanceBonus = 0;
    private transient int raceCooldownReductionSeconds = 0;
    private transient Set<String> immunities = new HashSet<>();
    private transient Set<String> raceSpecialMechanics = new HashSet<>();

    // --- Getters and Setters ---

    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public String getPlayerClass() { return playerClass; }
    public void setPlayerClass(String playerClass) { this.playerClass = playerClass; }
    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }
    public int getCurrentXp() { return currentXp; }
    public void setCurrentXp(int currentXp) { this.currentXp = currentXp; }
    public int getTotalXpEarned() { return totalXpEarned; }
    public void setTotalXpEarned(int totalXpEarned) { this.totalXpEarned = totalXpEarned; }
    public List<String> getSelectedPerks() { return selectedPerks; }
    public void setSelectedPerks(List<String> selectedPerks) { this.selectedPerks = selectedPerks; }
    public List<PerkSelectionRecord> getPerkSelectionHistory() { return perkSelectionHistory; }
    public int getPerkSelectionPoints() { return perkSelectionPoints; }
    public void setPerkSelectionPoints(int perkSelectionPoints) { this.perkSelectionPoints = perkSelectionPoints; }
    public Integer getPendingPerkTier() { return pendingPerkTier; }
    public void setPendingPerkTier(Integer pendingPerkTier) { this.pendingPerkTier = pendingPerkTier; }
    public boolean isFirstJoin() { return firstJoin; }
    public void setFirstJoin(boolean firstJoin) { this.firstJoin = firstJoin; }
    public Statistics getStatistics() { return statistics; }
    public List<String> getAchievements() { return achievements; }
    public Settings getSettings() { return settings; }
    public Metadata getMetadata() { return metadata; }

    // Phase 1 getters/setters
    public String getRace() { return race; }
    public void setRace(String race) { this.race = race; }
    public List<String> getSelectedStyles() { return selectedStyles; }
    public void setSelectedStyles(List<String> selectedStyles) { this.selectedStyles = selectedStyles; }
    public Map<String, Integer> getClassResources() { return classResources; }
    public void setClassResources(Map<String, Integer> classResources) { this.classResources = classResources; }
    public int getWaterContainerTier() { return waterContainerTier; }
    public void setWaterContainerTier(int waterContainerTier) { this.waterContainerTier = waterContainerTier; }

    // Runtime getters/setters
    public boolean isOnline() { return online; }
    public void setOnline(boolean online) { this.online = online; }
    public long getSessionStart() { return sessionStart; }
    public void setSessionStart(long sessionStart) { this.sessionStart = sessionStart; }
    public List<KillRecord> getRecentKills() { return recentKills; }
    public int getComboCount() { return comboCount; }
    public void setComboCount(int comboCount) { this.comboCount = comboCount; }
    public Long getLastKillTime() { return lastKillTime; }
    public void setLastKillTime(Long lastKillTime) { this.lastKillTime = lastKillTime; }
    public int getPartySize() { return partySize; }
    public void setPartySize(int partySize) { this.partySize = partySize; }
    public double getRestedBonus() { return restedBonus; }
    public void setRestedBonus(double restedBonus) { this.restedBonus = restedBonus; }
    public Long getLastLogoutTimestamp() { return lastLogoutTimestamp; }
    public void setLastLogoutTimestamp(Long lastLogoutTimestamp) { this.lastLogoutTimestamp = lastLogoutTimestamp; }
    public Map<String, Double> getSynergyDamageReduction() { return synergyDamageReduction; }
    public Map<String, Double> getSynergyDamageIncrease() { return synergyDamageIncrease; }
    public Map<String, Double> getSynergyStatBonuses() { return synergyStatBonuses; }
    public double getSynergyHealingBonus() { return synergyHealingBonus; }
    public void setSynergyHealingBonus(double v) { this.synergyHealingBonus = v; }
    public double getSynergyCritChance() { return synergyCritChance; }
    public void setSynergyCritChance(double v) { this.synergyCritChance = v; }
    public double getSynergyCritDamage() { return synergyCritDamage; }
    public void setSynergyCritDamage(double v) { this.synergyCritDamage = v; }
    public Map<String, Double> getSynergyCooldownReduction() { return synergyCooldownReduction; }
    public Map<String, Double> getSynergyDurationBonus() { return synergyDurationBonus; }
    public double getSynergyChainBonus() { return synergyChainBonus; }
    public void setSynergyChainBonus(double v) { this.synergyChainBonus = v; }
    public double getSynergyRadiusBonus() { return synergyRadiusBonus; }
    public void setSynergyRadiusBonus(double v) { this.synergyRadiusBonus = v; }
    public List<ActiveSynergy> getActiveSynergyBonuses() { return activeSynergyBonuses; }
    public void setActiveSynergyBonuses(List<ActiveSynergy> v) { this.activeSynergyBonuses = v; }
    public int getRaceHpBonus() { return raceHpBonus; }
    public void setRaceHpBonus(int raceHpBonus) { this.raceHpBonus = raceHpBonus; }
    public Map<String, Double> getRaceDamageReduction() { return raceDamageReduction; }
    public Map<String, Double> getRaceDamageIncrease() { return raceDamageIncrease; }
    public Map<String, Double> getRaceStatBonuses() { return raceStatBonuses; }
    public double getRaceHealingReceivedBonus() { return raceHealingReceivedBonus; }
    public void setRaceHealingReceivedBonus(double raceHealingReceivedBonus) { this.raceHealingReceivedBonus = raceHealingReceivedBonus; }
    public double getRaceCritChanceBonus() { return raceCritChanceBonus; }
    public void setRaceCritChanceBonus(double raceCritChanceBonus) { this.raceCritChanceBonus = raceCritChanceBonus; }
    public int getRaceCooldownReductionSeconds() { return raceCooldownReductionSeconds; }
    public void setRaceCooldownReductionSeconds(int raceCooldownReductionSeconds) { this.raceCooldownReductionSeconds = raceCooldownReductionSeconds; }
    public Set<String> getImmunities() { return immunities; }
    public Set<String> getRaceSpecialMechanics() { return raceSpecialMechanics; }

    public void clearSynergyBonuses() {
        synergyDamageReduction.clear();
        synergyDamageIncrease.clear();
        synergyStatBonuses.clear();
        synergyHealingBonus = 0;
        synergyCritChance = 0;
        synergyCritDamage = 0;
        synergyCooldownReduction.clear();
        synergyDurationBonus.clear();
        synergyChainBonus = 0;
        synergyRadiusBonus = 0;
        activeSynergyBonuses.clear();
    }

    public void clearRaceBonuses() {
        raceHpBonus = 0;
        raceDamageReduction.clear();
        raceDamageIncrease.clear();
        raceStatBonuses.clear();
        raceHealingReceivedBonus = 0;
        raceCritChanceBonus = 0;
        raceCooldownReductionSeconds = 0;
        immunities.clear();
        raceSpecialMechanics.clear();
    }

    public void initRuntimeFields() {
        if (recentKills == null) recentKills = new ArrayList<>();
        if (synergyDamageReduction == null) synergyDamageReduction = new HashMap<>();
        if (synergyDamageIncrease == null) synergyDamageIncrease = new HashMap<>();
        if (synergyStatBonuses == null) synergyStatBonuses = new HashMap<>();
        if (synergyCooldownReduction == null) synergyCooldownReduction = new HashMap<>();
        if (synergyDurationBonus == null) synergyDurationBonus = new HashMap<>();
        if (activeSynergyBonuses == null) activeSynergyBonuses = new ArrayList<>();
        if (raceDamageReduction == null) raceDamageReduction = new HashMap<>();
        if (raceDamageIncrease == null) raceDamageIncrease = new HashMap<>();
        if (raceStatBonuses == null) raceStatBonuses = new HashMap<>();
        if (immunities == null) immunities = new HashSet<>();
        if (raceSpecialMechanics == null) raceSpecialMechanics = new HashSet<>();
    }

    // --- Nested types ---

    public static class PerkSelectionRecord {
        private int tier;
        @SerializedName("perks_chosen")
        private List<String> perksChosen;
        private String timestamp;

        public PerkSelectionRecord(int tier, List<String> perksChosen, String timestamp) {
            this.tier = tier;
            this.perksChosen = perksChosen;
            this.timestamp = timestamp;
        }

        public int getTier() { return tier; }
        public List<String> getPerksChosen() { return perksChosen; }
        public String getTimestamp() { return timestamp; }
    }

    public static class KillRecord {
        private final String mobType;
        private final int mobLevel;
        private final long timestamp;

        public KillRecord(String mobType, int mobLevel, long timestamp) {
            this.mobType = mobType;
            this.mobLevel = mobLevel;
            this.timestamp = timestamp;
        }

        public String getMobType() { return mobType; }
        public int getMobLevel() { return mobLevel; }
        public long getTimestamp() { return timestamp; }
    }

    public static class ActiveSynergy {
        private String sourcePerk;
        private String bonusType;
        private double bonusValue;
        private List<String> triggeredByTags;
        private List<String> contributingPerks;

        public ActiveSynergy(String sourcePerk, String bonusType, double bonusValue,
                             List<String> triggeredByTags, List<String> contributingPerks) {
            this.sourcePerk = sourcePerk;
            this.bonusType = bonusType;
            this.bonusValue = bonusValue;
            this.triggeredByTags = triggeredByTags;
            this.contributingPerks = contributingPerks;
        }

        public String getSourcePerk() { return sourcePerk; }
        public String getBonusType() { return bonusType; }
        public double getBonusValue() { return bonusValue; }
        public List<String> getTriggeredByTags() { return triggeredByTags; }
        public List<String> getContributingPerks() { return contributingPerks; }
    }

    public static class Statistics {
        @SerializedName("mobs_killed")
        private Map<String, Integer> mobsKilled = new HashMap<>();
        @SerializedName("bosses_defeated")
        private List<String> bossesDefeated = new ArrayList<>();
        @SerializedName("total_damage_dealt")
        private double totalDamageDealt = 0;
        @SerializedName("total_damage_taken")
        private double totalDamageTaken = 0;
        @SerializedName("total_healing_done")
        private double totalHealingDone = 0;
        private int deaths = 0;
        @SerializedName("playtime_seconds")
        private int playtimeSeconds = 0;
        @SerializedName("highest_combo")
        private int highestCombo = 0;

        public Map<String, Integer> getMobsKilled() { return mobsKilled; }
        public List<String> getBossesDefeated() { return bossesDefeated; }
        public double getTotalDamageDealt() { return totalDamageDealt; }
        public void setTotalDamageDealt(double v) { this.totalDamageDealt = v; }
        public double getTotalDamageTaken() { return totalDamageTaken; }
        public void setTotalDamageTaken(double v) { this.totalDamageTaken = v; }
        public double getTotalHealingDone() { return totalHealingDone; }
        public void setTotalHealingDone(double v) { this.totalHealingDone = v; }
        public int getDeaths() { return deaths; }
        public void setDeaths(int deaths) { this.deaths = deaths; }
        public int getPlaytimeSeconds() { return playtimeSeconds; }
        public void setPlaytimeSeconds(int v) { this.playtimeSeconds = v; }
        public int getHighestCombo() { return highestCombo; }
        public void setHighestCombo(int v) { this.highestCombo = v; }
    }

    public static class Settings {
        @SerializedName("show_damage_numbers")
        private boolean showDamageNumbers = true;
        @SerializedName("show_mob_levels")
        private boolean showMobLevels = true;
        @SerializedName("show_xp_notifications")
        private boolean showXpNotifications = true;
        @SerializedName("auto_select_perks")
        private boolean autoSelectPerks = false;

        public boolean isShowDamageNumbers() { return showDamageNumbers; }
        public boolean isShowMobLevels() { return showMobLevels; }
        public boolean isShowXpNotifications() { return showXpNotifications; }
        public boolean isAutoSelectPerks() { return autoSelectPerks; }
    }

    public static class Metadata {
        @SerializedName("created_at")
        private String createdAt;
        @SerializedName("last_played")
        private String lastPlayed;
        @SerializedName("mod_version")
        private String modVersion = "1.0.0";
        @SerializedName("schema_version")
        private int schemaVersion = 1;

        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String v) { this.createdAt = v; }
        public String getLastPlayed() { return lastPlayed; }
        public void setLastPlayed(String v) { this.lastPlayed = v; }
        public String getModVersion() { return modVersion; }
        public void setModVersion(String v) { this.modVersion = v; }
        public int getSchemaVersion() { return schemaVersion; }
        public void setSchemaVersion(int v) { this.schemaVersion = v; }
    }
}
