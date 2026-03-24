package com.motm.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.motm.model.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Logger;

/**
 * Loads JSON data files from the plugin resources directory.
 * Falls back to classpath resources if the file doesn't exist on disk.
 */
public class DataLoader {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Logger LOG = Logger.getLogger("MOTM");

    private final Path dataDirectory;

    // Cached data
    private final Map<String, ClassData> classDataCache = new HashMap<>();
    private final Map<String, List<Perk>> perkCache = new HashMap<>();
    private final Map<String, List<StyleData>> styleCache = new HashMap<>();
    private List<ElementalReaction> reactionCache = new ArrayList<>();
    private List<RaceData> raceCache = new ArrayList<>();
    private JsonObject eliteTitles;
    private JsonObject mobXpTable;
    private JsonObject xpConfig;
    private JsonObject mobBaseStats;
    private JsonObject mobScalingConfig;

    public DataLoader(Path dataDirectory) {
        this.dataDirectory = dataDirectory;
    }

    public void loadAll() {
        loadClasses();
        loadPerks();
        loadStyles();
        loadReactions();
        loadRaces();
        loadLevelingData();
        loadMobData();
        loadEliteTitles();
        LOG.info("[MOTM] All data files loaded successfully.");
    }

    // --- Class Data ---

    private void loadClasses() {
        String[] classIds = {"terra", "hydro", "aero", "corruptus"};
        for (String classId : classIds) {
            ClassData data = loadJson("data/classes/" + classId + ".json", ClassData.class);
            if (data != null) {
                classDataCache.put(classId, data);
                LOG.info("[MOTM] Loaded class: " + classId);
            }
        }
    }

    public ClassData getClassData(String classId) {
        return classDataCache.get(classId);
    }

    public Collection<ClassData> getAllClasses() {
        return classDataCache.values();
    }

    public boolean isValidClass(String classId) {
        return classDataCache.containsKey(classId);
    }

    // --- Perk Data ---

    private void loadPerks() {
        String[] classIds = {"terra", "hydro", "aero", "corruptus"};
        for (String classId : classIds) {
            JsonObject wrapper = loadJson("data/perks/" + classId + "_perks.json", JsonObject.class);
            if (wrapper != null && wrapper.has("perks")) {
                Type listType = new TypeToken<List<Perk>>() {}.getType();
                List<Perk> perks = GSON.fromJson(wrapper.get("perks"), listType);
                perkCache.put(classId, perks);
                LOG.info("[MOTM] Loaded " + perks.size() + " perks for class: " + classId);
            }
        }
    }

    public List<Perk> getPerksForClass(String classId) {
        return perkCache.getOrDefault(classId, Collections.emptyList());
    }

    public Perk getPerkById(String perkId, String classId) {
        List<Perk> perks = getPerksForClass(classId);
        for (Perk perk : perks) {
            if (perk.getId().equals(perkId)) {
                return perk;
            }
        }
        return null;
    }

    public boolean perkExists(String perkId, String classId) {
        return getPerkById(perkId, classId) != null;
    }

    // --- Style Data ---

    private void loadStyles() {
        String[] classIds = {"terra", "hydro", "aero", "corruptus"};
        for (String classId : classIds) {
            JsonObject wrapper = loadJson("data/styles/" + classId + "_styles.json", JsonObject.class);
            if (wrapper != null && wrapper.has("styles")) {
                Type listType = new TypeToken<List<StyleData>>() {}.getType();
                List<StyleData> styles = GSON.fromJson(wrapper.get("styles"), listType);
                styleCache.put(classId, styles);
                int totalAbilities = styles.stream().mapToInt(s -> s.getAbilities().size()).sum();
                LOG.info("[MOTM] Loaded " + styles.size() + " styles (" + totalAbilities + " abilities) for class: " + classId);
            }
        }
    }

    public List<StyleData> getStylesForClass(String classId) {
        return styleCache.getOrDefault(classId, Collections.emptyList());
    }

    public StyleData getStyleById(String styleId, String classId) {
        for (StyleData style : getStylesForClass(classId)) {
            if (style.getId().equals(styleId)) return style;
        }
        return null;
    }

    // --- Elemental Reaction Data ---

    private void loadReactions() {
        JsonObject wrapper = loadJson("data/reactions/elemental_reactions.json", JsonObject.class);
        if (wrapper != null && wrapper.has("reactions")) {
            Type listType = new TypeToken<List<ElementalReaction>>() {}.getType();
            reactionCache = GSON.fromJson(wrapper.get("reactions"), listType);
            LOG.info("[MOTM] Loaded " + reactionCache.size() + " elemental reactions.");
        }
    }

    public List<ElementalReaction> getElementalReactions() {
        return reactionCache;
    }

    // --- Race Data ---

    private void loadRaces() {
        JsonObject wrapper = loadJson("data/races/races.json", JsonObject.class);
        if (wrapper != null && wrapper.has("races")) {
            Type listType = new TypeToken<List<RaceData>>() {}.getType();
            raceCache = GSON.fromJson(wrapper.get("races"), listType);
            LOG.info("[MOTM] Loaded " + raceCache.size() + " races.");
        }
    }

    public List<RaceData> getAllRaces() { return raceCache; }

    public RaceData getRaceById(String raceId) {
        for (RaceData race : raceCache) {
            if (race.getId().equals(raceId)) return race;
        }
        return null;
    }

    public boolean isValidRace(String raceId) {
        return getRaceById(raceId) != null;
    }

    // --- Elite Titles ---

    private void loadEliteTitles() {
        eliteTitles = loadJson("data/mobs/elite_titles.json", JsonObject.class);
        if (eliteTitles != null) {
            LOG.info("[MOTM] Loaded elite titles data.");
        }
    }

    public JsonObject getEliteTitles() { return eliteTitles; }

    public Map<String, String> getEliteTitlesForZone(String zone) {
        if (eliteTitles == null || !eliteTitles.has("biome_elite_titles")) {
            return Collections.emptyMap();
        }

        JsonObject zones = eliteTitles.getAsJsonObject("biome_elite_titles");
        if (!zones.has(zone)) {
            return Collections.emptyMap();
        }

        Map<String, String> titles = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : zones.getAsJsonObject(zone).entrySet()) {
            titles.put(entry.getKey(), entry.getValue().getAsString());
        }
        return titles;
    }

    public double getEliteSpawnChance() {
        return getEliteConfigValue("spawn_chance", 0.05);
    }

    public double getEliteHpMultiplier() {
        return getEliteConfigValue("hp_multiplier", 2.0);
    }

    public double getEliteDamageMultiplier() {
        return getEliteConfigValue("damage_multiplier", 1.5);
    }

    public double getEliteXpMultiplier() {
        return getEliteConfigValue("xp_multiplier", 3.0);
    }

    public String getEliteTitle(String biome, String style) {
        if (eliteTitles == null || !eliteTitles.has("biome_elite_titles")) return null;
        JsonObject biomes = eliteTitles.getAsJsonObject("biome_elite_titles");
        if (!biomes.has(biome)) return null;
        JsonObject biomeStyles = biomes.getAsJsonObject(biome);
        return biomeStyles.has(style) ? biomeStyles.get(style).getAsString() : null;
    }

    // --- Leveling Data ---

    private void loadLevelingData() {
        xpConfig = loadJson("data/leveling/xp_config.json", JsonObject.class);
        mobXpTable = loadJson("data/leveling/mob_xp_table.json", JsonObject.class);
        LOG.info("[MOTM] Loaded leveling data.");
    }

    public int getMobBaseXp(String mobType) {
        if (mobXpTable == null || !mobXpTable.has("mob_xp_table")) return 50;

        JsonObject table = mobXpTable.getAsJsonObject("mob_xp_table");
        String[] categories = {"passive", "neutral", "hostile", "elite", "mini_boss", "boss", "world_boss"};

        for (String category : categories) {
            if (table.has(category)) {
                JsonObject cat = table.getAsJsonObject(category);
                if (cat.has(mobType)) {
                    return cat.getAsJsonObject(mobType).get("base_xp").getAsInt();
                }
            }
        }

        LOG.warning("[MOTM] Unknown mob type for XP: " + mobType + ", using default 50");
        return 50;
    }

    // --- Mob Data ---

    private void loadMobData() {
        mobBaseStats = loadJson("data/mobs/mob_base_stats.json", JsonObject.class);
        mobScalingConfig = loadJson("data/mobs/mob_scaling_config.json", JsonObject.class);
        LOG.info("[MOTM] Loaded mob data.");
    }

    public JsonObject getMobBaseStats() { return mobBaseStats; }
    public JsonObject getMobScalingConfig() { return mobScalingConfig; }

    public MobStats getMobStats(String mobType) {
        JsonObject mobEntry = findMobEntry(mobType);
        if (mobEntry == null) {
            return null;
        }

        if (mobEntry.has("base_mob")) {
            return buildDerivedMobStats(mobType, mobEntry);
        }

        return buildMobStats(mobType, mobEntry);
    }

    public String getMobCategory(String mobType) {
        if (mobBaseStats == null) return "hostile";

        Map<String, String> categoryMap = Map.of(
            "passive_mobs", "passive",
            "neutral_mobs", "neutral",
            "hostile_mobs", "hostile",
            "elite_mobs", "elite",
            "mini_boss_mobs", "mini_boss"
        );

        for (var entry : categoryMap.entrySet()) {
            if (mobBaseStats.has(entry.getKey())) {
                JsonObject cat = mobBaseStats.getAsJsonObject(entry.getKey());
                if (cat.has(mobType)) {
                    return entry.getValue();
                }
            }
        }
        return "hostile";
    }

    private JsonObject findMobEntry(String mobType) {
        if (mobBaseStats == null) {
            return null;
        }

        String[] categories = {
                "passive_mobs",
                "neutral_mobs",
                "hostile_mobs",
                "elite_mobs",
                "mini_boss_mobs"
        };

        for (String category : categories) {
            if (!mobBaseStats.has(category)) {
                continue;
            }

            JsonObject group = mobBaseStats.getAsJsonObject(category);
            if (group.has(mobType)) {
                return group.getAsJsonObject(mobType);
            }
        }

        return null;
    }

    private MobStats buildMobStats(String mobType, JsonObject mobEntry) {
        MobStats stats = new MobStats();
        stats.setHealth(getDouble(mobEntry, "health", 0));
        stats.setDamage(getDouble(mobEntry, "damage", 0));
        stats.setArmor(getDouble(mobEntry, "armor", 0));
        stats.setMagicResist(getDouble(mobEntry, "magic_resist", 0));
        stats.setSpeed(getDouble(mobEntry, "speed", 1));
        stats.setAttackSpeed(getDouble(mobEntry, "attack_speed", 1));
        stats.setAggroRange(getDouble(mobEntry, "aggro_range", 0));
        stats.setAttackRange(getDouble(mobEntry, "attack_range", 0));
        stats.setBehavior(getString(mobEntry, "behavior"));
        stats.setAbilities(readStringList(mobEntry, "abilities"));
        stats.setDrops(readStringList(mobEntry, "drops"));
        stats.setXpReward(getMobBaseXp(mobType));
        return stats;
    }

    private MobStats buildDerivedMobStats(String mobType, JsonObject mobEntry) {
        String baseMobType = getString(mobEntry, "base_mob");
        MobStats baseStats = baseMobType != null ? getMobStats(baseMobType) : null;
        if (baseStats == null) {
            return buildMobStats(mobType, mobEntry);
        }

        MobStats derived = new MobStats(baseStats);
        derived.setHealth(baseStats.getHealth() * getDouble(mobEntry, "health_multiplier", 1));
        derived.setDamage(baseStats.getDamage() * getDouble(mobEntry, "damage_multiplier", 1));
        derived.setArmor(baseStats.getArmor() + getDouble(mobEntry, "armor_bonus", 0));
        derived.setMagicResist(baseStats.getMagicResist() + getDouble(mobEntry, "magic_resist_bonus", 0));
        if (mobEntry.has("behavior")) {
            derived.setBehavior(getString(mobEntry, "behavior"));
        }
        if (mobEntry.has("abilities")) {
            derived.setAbilities(readStringList(mobEntry, "abilities"));
        }
        if (mobEntry.has("drops")) {
            derived.setDrops(readStringList(mobEntry, "drops"));
        }
        derived.setXpReward(getMobBaseXp(mobType));
        return derived;
    }

    private double getEliteConfigValue(String key, double fallback) {
        if (eliteTitles == null || !eliteTitles.has("elite_config")) {
            return fallback;
        }

        JsonObject config = eliteTitles.getAsJsonObject("elite_config");
        return config.has(key) ? config.get(key).getAsDouble() : fallback;
    }

    private double getDouble(JsonObject source, String key, double fallback) {
        return source.has(key) ? source.get(key).getAsDouble() : fallback;
    }

    private String getString(JsonObject source, String key) {
        return source.has(key) ? source.get(key).getAsString() : null;
    }

    private List<String> readStringList(JsonObject source, String key) {
        if (!source.has(key)) {
            return Collections.emptyList();
        }

        List<String> values = new ArrayList<>();
        for (JsonElement value : source.getAsJsonArray(key)) {
            values.add(value.getAsString());
        }
        return values;
    }

    // --- Generic JSON loading ---

    private <T> T loadJson(String resourcePath, Class<T> clazz) {
        // Try disk first
        Path diskPath = dataDirectory.resolve(resourcePath);
        if (Files.exists(diskPath)) {
            try (Reader reader = Files.newBufferedReader(diskPath, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, clazz);
            } catch (IOException e) {
                LOG.warning("[MOTM] Failed to read from disk: " + diskPath + " - " + e.getMessage());
            }
        }

        // Fall back to classpath
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
            if (is != null) {
                try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                    return GSON.fromJson(reader, clazz);
                }
            }
        } catch (IOException e) {
            LOG.warning("[MOTM] Failed to read from classpath: " + resourcePath + " - " + e.getMessage());
        }

        LOG.severe("[MOTM] Could not load resource: " + resourcePath);
        return null;
    }

    public Gson getGson() { return GSON; }
}
