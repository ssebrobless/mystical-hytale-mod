package com.motm;

import com.motm.command.MotmCommand;
import com.motm.command.MotmCommandBase;
import com.motm.manager.*;
import com.motm.system.MotmMobRuntimeSystem;
import com.motm.system.MotmServerTickSystem;
import com.motm.util.DataLoader;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;

import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Main plugin entry point for Mentees of the Mystical.
 *
 * Hytale Plugin Structure (from CurseForge docs):
 *   - Extends JavaPlugin
 *   - Constructor receives JavaPluginInit
 *   - Config registered via withConfig()
 *   - Deployed to %appdata%/Hytale/UserData/Mods/
 *
 * This class initializes all managers and wires them together.
 * Event listeners and command registration use Hytale's API.
 */
public class MenteesMod extends JavaPlugin {

    private static final Logger LOG = Logger.getLogger("MOTM");

    // Core systems
    private DataLoader dataLoader;
    private PerkManager perkManager;
    private SynergyEngine synergyEngine;
    private LevelingManager levelingManager;
    private MobScalingManager mobScalingManager;
    private PlayerDataManager playerDataManager;
    private MotmCommand motmCommand;

    // Phase 1 systems
    private StatusEffectManager statusEffectManager;
    private ResourceManager resourceManager;
    private StyleManager styleManager;
    private ElementalReactionManager elementalReactionManager;
    private RaceManager raceManager;

    // Plugin data directory
    private Path pluginDirectory;

    public MenteesMod(JavaPluginInit init) {
        super(init);
    }

    @Override
    protected void setup() {
        onEnable(getDataDirectory());
    }

    @Override
    protected void start() {
        registerHytaleHooks();
        playerDataManager.startAutoSave();
        LOG.info("[MOTM] Plugin enabled successfully!");
    }

    @Override
    protected void shutdown() {
        onDisable();
    }

    /**
     * Internal plugin bootstrap invoked from the real Hytale constructor.
     */
    public void onEnable(Path dataDir) {
        this.pluginDirectory = dataDir;

        LOG.info("========================================");
        LOG.info("  Mentees of the Mystical v1.0.0");
        LOG.info("  4 Classes | 40 Styles | 800 Perks | Level 1-200");
        LOG.info("========================================");

        // Initialize data loader and load all JSON data
        dataLoader = new DataLoader(dataDir);
        dataLoader.loadAll();

        // Initialize managers (order matters — dependencies)
        synergyEngine = new SynergyEngine(dataLoader);
        perkManager = new PerkManager(dataLoader);
        levelingManager = new LevelingManager(dataLoader, perkManager);
        mobScalingManager = new MobScalingManager(dataLoader);
        playerDataManager = new PlayerDataManager(dataDir, dataLoader);

        // Phase 1 managers
        statusEffectManager = new StatusEffectManager();
        resourceManager = new ResourceManager();
        styleManager = new StyleManager(dataLoader, resourceManager);
        elementalReactionManager = new ElementalReactionManager(dataLoader, statusEffectManager);
        raceManager = new RaceManager(dataLoader);

        // Initialize command handler
        motmCommand = new MotmCommand(this);
    }

    private void registerHytaleHooks() {
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, event -> {
            var playerRef = event.getPlayer().getPlayerRef();
            onPlayerJoin(playerRef.getUuid().toString(), playerRef.getUsername());
        });
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event ->
                onPlayerDisconnect(event.getPlayerRef().getUuid().toString())
        );

        getCommandRegistry().registerCommand(new MotmCommandBase(this));
        getEntityStoreRegistry().registerSystem(new MotmServerTickSystem(this));
        getEntityStoreRegistry().registerSystem(new MotmMobRuntimeSystem(this));
    }

    /**
     * Called when the plugin is disabled (server shutdown).
     */
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.saveAll();
            playerDataManager.stopAutoSave();
        }
        LOG.info("[MOTM] Plugin disabled. All data saved.");
    }

    // --- Event Handlers ---
    // These methods are called by Hytale event listeners (see listener/ package).
    // The actual listener registration depends on Hytale's event API.

    /**
     * Called when a player joins the server.
     */
    public void onPlayerJoin(String playerId, String playerName) {
        var player = playerDataManager.onPlayerJoin(playerId, playerName);

        // Update rested bonus
        levelingManager.updateRestedOnLogin(player);

        // Reapply perks and synergies if class is set
        if (player.getPlayerClass() != null) {
            perkManager.reapplyAllPerks(player, synergyEngine);
            // Initialize class resources
            resourceManager.initializeForPlayer(playerId, player.getPlayerClass());
            if (player.getRace() != null) {
                raceManager.applyRaceBonuses(player, statusEffectManager);
            }
        }

        // Check for pending perk selections
        if (perkManager.hasPendingPerkSelection(player)) {
            int tier = perkManager.getPendingSelectionTier(player);
            // TODO: Send message to player about pending perk selection
            LOG.info("[MOTM] " + playerName + " has pending Tier " + tier + " perk selection");
        }

        // First join — prompt class selection
        if (player.isFirstJoin()) {
            // TODO: Open class selection UI via Hytale's UI system
            LOG.info("[MOTM] " + playerName + " is a new player — showing class selection");
        }
    }

    /**
     * Called when a player disconnects.
     */
    public void onPlayerDisconnect(String playerId) {
        var player = playerDataManager.getOnlinePlayer(playerId);
        if (player != null) {
            levelingManager.updateRestedOnLogout(player);
        }
        playerDataManager.onPlayerDisconnect(playerId);
        styleManager.onPlayerDisconnect(playerId);
        resourceManager.onPlayerDisconnect(playerId);
        statusEffectManager.clearEffects(playerId);
        elementalReactionManager.clearMarks(playerId);
    }

    /**
     * Called when a mob is killed by a player.
     */
    public void onMobKilled(String playerId, String mobType, int mobLevel, boolean isRare) {
        var player = playerDataManager.getOnlinePlayer(playerId);
        if (player == null || player.getPlayerClass() == null) return;

        levelingManager.onMobKilled(player, mobType, mobLevel, isRare);
        resourceManager.onMobKilled(playerId, player.getPlayerClass());
        playerDataManager.checkAchievements(player, "mob_killed", null);
    }

    /**
     * Called when a player dies.
     */
    public void onPlayerDeath(String playerId) {
        var player = playerDataManager.getOnlinePlayer(playerId);
        if (player == null) return;

        player.getStatistics().setDeaths(player.getStatistics().getDeaths() + 1);
        // Reset combo on death
        player.setComboCount(0);
        player.setLastKillTime(null);
        statusEffectManager.clearEffects(playerId);
        elementalReactionManager.clearMarks(playerId);
    }

    /**
     * Called when a mob spawns, to apply level scaling.
     * Returns the scaled stats that should be applied to the mob entity.
     */
    public ScaledMobResult onMobSpawn(String mobType, String playerId,
                                      boolean isNight, boolean isBloodMoon, boolean isDungeon) {
        return onMobSpawn(mobType, playerId, null, isNight, isBloodMoon, isDungeon);
    }

    public ScaledMobResult onMobSpawn(String mobType, String playerId, String zoneId,
                                      boolean isNight, boolean isBloodMoon, boolean isDungeon) {
        var player = playerDataManager.getOnlinePlayer(playerId);
        if (player == null) return null;

        String category = dataLoader.getMobCategory(mobType);
        int mobLevel = mobScalingManager.assignMobLevel(player.getLevel());

        // Build base stats from data
        var baseStats = dataLoader.getMobStats(mobType);
        if (baseStats == null) {
            LOG.warning("[MOTM] Missing base stats for mob type " + mobType + "; using empty fallback.");
            baseStats = new com.motm.model.MobStats();
            baseStats.setXpReward(dataLoader.getMobBaseXp(mobType));
        }

        // Scale stats
        var scaled = mobScalingManager.scaleMobStats(baseStats, player.getLevel(), category);

        // Apply party scaling
        if (player.getPartySize() > 1) {
            scaled = mobScalingManager.applyPartyScaling(scaled, player.getPartySize());
        }

        // Apply environmental modifiers
        if (isNight) scaled = mobScalingManager.applyNightBonus(scaled);
        if (isBloodMoon) scaled = mobScalingManager.applyBloodMoonBonus(scaled);
        if (isDungeon) scaled = mobScalingManager.applyDungeonBonus(scaled);

        if (mobScalingManager.canBecomeElite(category)) {
            scaled = mobScalingManager.tryMakeElite(scaled, zoneId, mobType);
        }

        String displayName = scaled.isElite() && scaled.getEliteTitle() != null
                ? mobScalingManager.formatEliteMobName(mobType, mobLevel, scaled.getEliteTitle())
                : mobScalingManager.formatMobName(mobType, mobLevel, category);
        String levelColor = mobScalingManager.getLevelColor(mobLevel, player.getLevel());

        return new ScaledMobResult(scaled, mobLevel, displayName, levelColor);
    }

    /**
     * Called once per server tick.
     *
     * This keeps the runtime systems advancing even before we wire the actual
     * Hytale damage/entity APIs.
     */
    public void onServerTick() {
        var dotDamageByEntity = statusEffectManager.tickAll();
        elementalReactionManager.tickAll();
        styleManager.tickCooldowns();
        resourceManager.tick();

        dotDamageByEntity.forEach((entityId, dotPercent) ->
                LOG.fine("[MOTM] TODO: Apply " + (dotPercent * 100)
                        + "% max HP DoT to entity " + entityId + " via Hytale's damage API."));
    }

    // --- Getters for inter-manager access ---

    public DataLoader getDataLoader() { return dataLoader; }
    public PerkManager getPerkManager() { return perkManager; }
    public SynergyEngine getSynergyEngine() { return synergyEngine; }
    public LevelingManager getLevelingManager() { return levelingManager; }
    public MobScalingManager getMobScalingManager() { return mobScalingManager; }
    public PlayerDataManager getPlayerDataManager() { return playerDataManager; }
    public MotmCommand getMotmCommand() { return motmCommand; }
    public StatusEffectManager getStatusEffectManager() { return statusEffectManager; }
    public ResourceManager getResourceManager() { return resourceManager; }
    public StyleManager getStyleManager() { return styleManager; }
    public ElementalReactionManager getElementalReactionManager() { return elementalReactionManager; }
    public RaceManager getRaceManager() { return raceManager; }

    // --- Result wrapper for mob spawn scaling ---

    public record ScaledMobResult(
            com.motm.model.MobStats stats,
            int level,
            String displayName,
            String levelColor
    ) {}
}
