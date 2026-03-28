package com.motm;

import com.motm.command.MotmCommand;
import com.motm.command.MotmCommandBase;
import com.motm.manager.*;
import com.motm.model.AbilityData;
import com.motm.model.StyleData;
import com.motm.system.MotmMobRuntimeSystem;
import com.motm.system.MotmServerTickSystem;
import com.motm.ui.MotmStatusHud;
import com.motm.ui.SpellbookPage;
import com.motm.util.DataLoader;
import com.motm.util.MotmPreflightAudit;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonValue;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
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
    private static final String DEFAULT_SPELLBOOK_ITEM_ID = "Recipe_Book_Magic_Air";
    private static final String DEFAULT_DEV_GRIMOIRE_ITEM_ID = "Recipe_Book_Magic_Void";
    private static final String HYDRO_CONTAINER_METADATA_KEY = "motm_hydro_container";
    private static final String HYDRO_CONTAINER_TIER_METADATA_KEY = "motm_hydro_container_tier";
    private static final String HYDRO_LIGHT_WATERSKIN_RECIPE_ID = "MOTM_Hydro_Waterskin_Light";
    private static final int HYDRO_LIGHT_WATERSKIN_INPUT_COUNT = 2;
    private static final String[] HYDRO_CONTAINER_ITEM_IDS = {
            "Ingredient_Hide_Light",
            "Ingredient_Hide_Soft",
            "Ingredient_Hide_Medium",
            "Ingredient_Hide_Heavy",
            "Ingredient_Hide_Dark"
    };
    private static final String[] TERRA_STONE_ITEM_PREFIXES = {
            "Rock_Stone",
            "Rock_Slate",
            "Rock_Shale",
            "Rock_Calcite",
            "Rock_Quartzite",
            "Rock_Marble",
            "Rock_Lime",
            "Rock_Basalt",
            "Rock_Volcanic"
    };
    private static final String[] TERRA_DIRT_ITEM_PREFIXES = {
            "Soil_Dirt",
            "Soil_Grass"
    };
    private static final String[] TERRA_SAND_ITEM_PREFIXES = {
            "Soil_Sand",
            "Rock_Sandstone",
            "Rock_Sandstone_Red",
            "Rock_Sandstone_White"
    };
    private static final String[] TERRA_METAL_ITEM_PREFIXES = {
            "Ore_",
            "Ingredient_Bar_"
    };
    private static final String[] TERRA_GEM_ITEM_PREFIXES = {
            "Rock_Gem_",
            "Ingredient_Crystal_",
            "Rock_Crystal_"
    };
    private static final String[] TERRA_SEED_ITEM_PREFIXES = {
            "Plant_Seeds_"
    };
    private static final int TERRA_STONE_UNITS_PER_ITEM = 1;
    private static final int TERRA_DIRT_UNITS_PER_ITEM = 1;
    private static final int TERRA_SAND_UNITS_PER_ITEM = 1;
    private static final int TERRA_SEED_UNITS_PER_ITEM = 2;
    private static final int TERRA_METAL_UNITS_PER_ITEM = 4;
    private static final int TERRA_GEM_UNITS_PER_ITEM = 6;
    private static final String PLAYER_LEVEL_HEALTH_MODIFIER_ID = "motm_player_level_health";
    private static final boolean CUSTOM_PAGE_UI_ENABLED = false;
    private static final boolean CUSTOM_HUD_ENABLED = true;
    private static final String SERVER_CONFIG_FILE_NAME = "motm-server.properties";
    private static final int HUD_REFRESH_INTERVAL_TICKS = 4;
    private static final long SPELLBOOK_INPUT_DEBOUNCE_MS = 150L;
    private static final Set<String> LEGACY_NONWEAPON_SPELLBOOK_ITEM_IDS = Set.of(
            "Weapon_Spellbook_Grimoire_Brown",
            "Weapon_Spellbook_Grimoire_Purple",
            "Weapon_Spellbook_Frost",
            "Weapon_Spellbook_Fire",
            "Weapon_Spellbook_Rekindle_Embers"
    );
    private static final Set<String> SPELLBOOK_ITEM_IDS = Set.of(
            DEFAULT_SPELLBOOK_ITEM_ID,
            "Weapon_Spellbook_Grimoire_Brown",
            "Weapon_Spellbook_Grimoire_Purple",
            "Weapon_Spellbook_Frost",
            "Weapon_Spellbook_Fire",
            "Weapon_Spellbook_Rekindle_Embers"
    );
    private static final Set<String> DEV_GRIMOIRE_ITEM_IDS = Set.of(
            DEFAULT_DEV_GRIMOIRE_ITEM_ID
    );
    private static final Set<String> HYDRO_CONTAINER_ID_SET = Set.of(HYDRO_CONTAINER_ITEM_IDS);

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
    private ClassPassiveManager classPassiveManager;
    private StyleManager styleManager;
    private ElementalReactionManager elementalReactionManager;
    private RaceManager raceManager;
    private SpellbookManager spellbookManager;
    private BookInteractionManager bookInteractionManager;
    private GameplayPlaybackManager gameplayPlaybackManager;
    private boolean devToolsEnabled = false;
    private final Map<String, MotmStatusHud> statusHuds = new ConcurrentHashMap<>();
    private final Map<String, Player> onlineRuntimePlayers = new ConcurrentHashMap<>();
    private final Set<String> pendingSpellbookGrants = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingDevBookGrants = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingHydroContainerSyncs = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingRuntimeRebuilds = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingStatusHudRefreshs = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingProgressionBonusRefreshs = ConcurrentHashMap.newKeySet();
    private final Queue<PendingAbilityCast> pendingAbilityCasts = new ConcurrentLinkedQueue<>();
    private final Map<String, ActiveStyleTest> activeStyleTests = new ConcurrentHashMap<>();
    private final Set<String> freeCastPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> recentSpellbookSlotInputs = new ConcurrentHashMap<>();
    private int hudRefreshTickCounter = 0;
    private volatile MotmPreflightAudit.AuditReport lastPreflightAudit;

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
        registerNativeHydroCraftingRecipe();
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
        loadServerConfig();

        LOG.info("========================================");
        LOG.info("  Mentees of the Mystical v1.0.0");
        LOG.info("  4 Classes | 40 Styles | 800 Perks | Level 1-200");
        LOG.info("  Build Channel: " + MotmBuildInfo.BUILD_CHANNEL);
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
        resourceManager.setTerraInventoryBridge(new ResourceManager.TerraInventoryBridge() {
            @Override
            public int countInventoryResource(String playerId, String resourceType) {
                return countTerraInventoryResource(playerId, resourceType);
            }

            @Override
            public boolean spendInventoryResource(String playerId, String resourceType, int amount) {
                return spendTerraInventoryResource(playerId, resourceType, amount);
            }
        });
        resourceManager.setHydroInventoryBridge(new ResourceManager.HydroInventoryBridge() {
            @Override
            public boolean hasHydroContainer(String playerId) {
                return hasHydroContainerInInventory(playerId);
            }

            @Override
            public int getHydroContainerTier(String playerId) {
                return getHydroContainerTierFromInventory(playerId);
            }
        });
        classPassiveManager = new ClassPassiveManager(
                dataLoader,
                playerDataManager,
                statusEffectManager,
                resourceManager
        );
        styleManager = new StyleManager(dataLoader, resourceManager, classPassiveManager, this::isFreeCastEnabled);
        elementalReactionManager = new ElementalReactionManager(dataLoader, statusEffectManager);
        raceManager = new RaceManager(dataLoader);
        spellbookManager = new SpellbookManager(
                dataLoader,
                levelingManager,
                styleManager,
                perkManager,
                resourceManager,
                classPassiveManager
        );
        bookInteractionManager = new BookInteractionManager(this);
        gameplayPlaybackManager = new GameplayPlaybackManager(this);

        // Initialize command handler
        motmCommand = new MotmCommand(this);
        lastPreflightAudit = MotmPreflightAudit.run(this);
    }

    private void loadServerConfig() {
        try {
            Files.createDirectories(pluginDirectory);
            Path configPath = pluginDirectory.resolve(SERVER_CONFIG_FILE_NAME);
            Properties properties = new Properties();

            if (Files.exists(configPath)) {
                try (var reader = Files.newBufferedReader(configPath)) {
                    properties.load(reader);
                }
            } else {
                properties.setProperty("dev_tools_enabled", "false");
                properties.setProperty("notes", "Set dev_tools_enabled=true to enable /motm dev and the Dev Grimoire.");
                try (var writer = Files.newBufferedWriter(configPath)) {
                    properties.store(writer, "Mentees of the Mystical server settings");
                }
            }

            devToolsEnabled = Boolean.parseBoolean(properties.getProperty("dev_tools_enabled", "false"));
            LOG.info("[MOTM] Dev tools " + (isDevToolsEnabled() ? "enabled" : "disabled")
                    + " via " + configPath.getFileName());
        } catch (IOException e) {
            devToolsEnabled = false;
            LOG.warning("[MOTM] Failed to load server config. Dev tools disabled. " + e.getMessage());
        }
    }

    private void registerHytaleHooks() {
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, event -> onPlayerReady(event.getPlayer()));
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, event ->
                onPlayerDisconnect(event.getPlayerRef().getUuid().toString())
        );
        getEventRegistry().registerGlobal(DamageBlockEvent.class, this::handleDamageBlock);
        getEventRegistry().registerGlobal(PlayerInteractEvent.class, this::handlePlayerInteract);
        getEventRegistry().registerGlobal(PlayerMouseButtonEvent.class, this::handlePlayerMouseButton);

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
            resourceManager.synchronizePersistentState(player);
            resourceManager.initializeForPlayer(player);
            queueHydroContainerSync(playerId);
            if (player.getRace() != null) {
                raceManager.applyRaceBonuses(player, statusEffectManager);
            }
            classPassiveManager.onPlayerJoin(player);
        } else {
            classPassiveManager.clearPlayerState(playerId);
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

    public void onPlayerReady(Player runtimePlayer) {
        var playerRef = getUniversePlayerRef(runtimePlayer);
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }
        onlineRuntimePlayers.put(playerRef.getUuid().toString(), runtimePlayer);
        onPlayerJoin(playerRef.getUuid().toString(), playerRef.getUsername());
        refreshPlayerProgressionBonuses(playerRef.getUuid().toString());
        installStatusHud(runtimePlayer);
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
        classPassiveManager.clearPlayerState(playerId);
        statusEffectManager.clearEffects(playerId);
        elementalReactionManager.clearMarks(playerId);
        statusHuds.remove(playerId);
        onlineRuntimePlayers.remove(playerId);
        pendingSpellbookGrants.remove(playerId);
        pendingDevBookGrants.remove(playerId);
        pendingHydroContainerSyncs.remove(playerId);
        pendingRuntimeRebuilds.remove(playerId);
        pendingStatusHudRefreshs.remove(playerId);
        pendingProgressionBonusRefreshs.remove(playerId);
        pendingAbilityCasts.removeIf(request -> playerId.equals(request.playerId()));
        activeStyleTests.remove(playerId);
        freeCastPlayers.remove(playerId);
        recentSpellbookSlotInputs.keySet().removeIf(key -> key.startsWith(playerId + ":"));
    }

    /**
     * Called when a mob is killed by a player.
     */
    public void onMobKilled(String playerId, String mobType, int mobLevel, boolean isRare) {
        onMobKilled(playerId, null, mobType, mobLevel, isRare);
    }

    public void onMobKilled(String playerId, String mobEntityId, String mobType, int mobLevel, boolean isRare) {
        var player = playerDataManager.getOnlinePlayer(playerId);
        if (player == null || player.getPlayerClass() == null) return;

        levelingManager.onMobKilled(player, mobType, mobLevel, isRare);
        resourceManager.onMobKilled(playerId, player.getPlayerClass());
        Player runtimePlayer = onlineRuntimePlayers.get(playerId);
        if (runtimePlayer != null) {
            classPassiveManager.onMobKilled(player, runtimePlayer, mobEntityId);
        }
        playerDataManager.checkAchievements(player, "mob_killed", null);
        refreshPlayerProgressionBonuses(playerId);
        refreshStatusHud(playerId);
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
        classPassiveManager.onPlayerDeath(playerId);
        statusEffectManager.clearEffects(playerId);
        elementalReactionManager.clearMarks(playerId);
        refreshStatusHud(playerId);
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
        int progressionAnchorLevel = resolveMobScalingAnchorLevel(category, playerId, player);
        int mobLevel = mobScalingManager.assignMobLevel(progressionAnchorLevel);

        // Build base stats from data
        var baseStats = dataLoader.getMobStats(mobType);
        if (baseStats == null) {
            LOG.warning("[MOTM] Missing base stats for mob type " + mobType + "; using empty fallback.");
            baseStats = new com.motm.model.MobStats();
            baseStats.setXpReward(dataLoader.getMobBaseXp(mobType));
        }

        // Scale stats
        var scaled = mobScalingManager.isBossCategory(category)
                ? mobScalingManager.scaleBossStats(baseStats, progressionAnchorLevel, category)
                : mobScalingManager.scaleMobStats(baseStats, progressionAnchorLevel, category);

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
    public void onServerTick(Store<EntityStore> currentStore) {
        var dotDamageByEntity = statusEffectManager.tickAll();
        elementalReactionManager.tickAll();
        styleManager.tickCooldowns();
        resourceManager.tick();
        processPendingRuntimeRebuilds(currentStore);
        classPassiveManager.tick(onlineRuntimePlayers, currentStore);
        processActiveStyleTests(currentStore);
        processPendingAbilityCasts(currentStore);
        gameplayPlaybackManager.tick();
        processPendingInventoryGrants(currentStore);
        processPendingProgressionBonusRefreshs(currentStore);
        processPendingStatusHudRefreshs(currentStore);
        hudRefreshTickCounter++;
        if (hudRefreshTickCounter >= HUD_REFRESH_INTERVAL_TICKS) {
            hudRefreshTickCounter = 0;
            refreshAllPlayerProgressionBonuses(currentStore);
            refreshAllStatusHuds(currentStore);
        }

        dotDamageByEntity.forEach((entityId, dotPercent) ->
                LOG.fine("[MOTM] TODO: Apply " + (dotPercent * 100)
                        + "% max HP DoT to entity " + entityId + " via Hytale's damage API."));
    }

    public boolean openSpellbook(Player sender, SpellbookManager.Section section) {
        if (!CUSTOM_PAGE_UI_ENABLED) {
            return false;
        }

        var entityRef = sender.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        PlayerRef playerRef = getUniversePlayerRef(sender);
        if (playerRef == null) {
            return false;
        }

        sender.getPageManager().openCustomPage(
                entityRef,
                entityRef.getStore(),
                new SpellbookPage(playerRef, this, section)
        );
        return true;
    }

    public boolean isSpellbookItem(ItemStack stack) {
        return stack != null && stack.getItemId() != null && SPELLBOOK_ITEM_IDS.contains(stack.getItemId());
    }

    public boolean isSpellbookItemId(String itemId) {
        return itemId != null && SPELLBOOK_ITEM_IDS.contains(itemId);
    }

    public boolean playerHasSpellbook(Player player) {
        CombinedItemContainer inventory = getCombinedPlayerInventory(player);
        if (inventory == null) {
            return false;
        }

        return inventory.countItemStacks(this::isSpellbookItem) > 0;
    }

    public boolean isDevBookItem(ItemStack stack) {
        return stack != null && stack.getItemId() != null && DEV_GRIMOIRE_ITEM_IDS.contains(stack.getItemId());
    }

    public boolean isDevBookItemId(String itemId) {
        return itemId != null && DEV_GRIMOIRE_ITEM_IDS.contains(itemId);
    }

    public boolean playerHasDevBook(Player player) {
        CombinedItemContainer inventory = getCombinedPlayerInventory(player);
        if (inventory == null) {
            return false;
        }

        return inventory.countItemStacks(this::isDevBookItem) > 0;
    }

    public boolean playerHasHydroContainer(Player player) {
        CombinedItemContainer inventory = getCombinedPlayerInventory(player);
        if (inventory == null) {
            return false;
        }

        return inventory.countItemStacks(this::isHydroContainerItem) > 0;
    }

    public boolean hasHydroContainerInInventory(String playerId) {
        Player player = getRuntimePlayer(playerId);
        return player != null && playerHasHydroContainer(player);
    }

    public boolean isHydroContainerItem(ItemStack stack) {
        if (stack == null || stack.getItemId() == null || !HYDRO_CONTAINER_ID_SET.contains(stack.getItemId())) {
            return false;
        }
        BsonDocument metadata = stack.getMetadata();
        if (metadata == null || !metadata.containsKey(HYDRO_CONTAINER_METADATA_KEY)) {
            return false;
        }
        BsonValue value = metadata.get(HYDRO_CONTAINER_METADATA_KEY);
        return value != null && value.isBoolean() && value.asBoolean().getValue();
    }

    public boolean isHydroContainerItemId(String itemId) {
        return itemId != null && HYDRO_CONTAINER_ID_SET.contains(itemId);
    }

    public String getHydroContainerItemId(int tier) {
        int clampedTier = Math.max(0, Math.min(tier, HYDRO_CONTAINER_ITEM_IDS.length - 1));
        return HYDRO_CONTAINER_ITEM_IDS[clampedTier];
    }

    private ItemStack createHydroContainerStack(int tier) {
        int clampedTier = Math.max(0, Math.min(tier, HYDRO_CONTAINER_ITEM_IDS.length - 1));
        return new ItemStack(HYDRO_CONTAINER_ITEM_IDS[clampedTier])
                .withMetadata(HYDRO_CONTAINER_METADATA_KEY, BsonBoolean.TRUE)
                .withMetadata(HYDRO_CONTAINER_TIER_METADATA_KEY, new BsonInt32(clampedTier));
    }

    private boolean isHydroContainerTier(ItemStack stack, int tier) {
        if (!isHydroContainerItem(stack)) {
            return false;
        }
        BsonDocument metadata = stack.getMetadata();
        if (metadata == null || !metadata.containsKey(HYDRO_CONTAINER_TIER_METADATA_KEY)) {
            return false;
        }
        BsonValue value = metadata.get(HYDRO_CONTAINER_TIER_METADATA_KEY);
        return value != null && value.isInt32() && value.asInt32().getValue() == tier;
    }

    public int getHydroContainerTierFromInventory(String playerId) {
        Player player = getRuntimePlayer(playerId);
        if (player == null) {
            return 0;
        }

        CombinedItemContainer inventory = getCombinedPlayerInventory(player);
        if (inventory == null) {
            return 0;
        }

        final int[] detectedTier = {0};
        inventory.forEach((slot, stack) -> {
            if (!isHydroContainerItem(stack)) {
                return;
            }
            BsonDocument metadata = stack.getMetadata();
            if (metadata == null) {
                return;
            }
            BsonValue value = metadata.get(HYDRO_CONTAINER_TIER_METADATA_KEY);
            if (value != null && value.isInt32()) {
                detectedTier[0] = Math.max(0, Math.min(
                        value.asInt32().getValue(),
                        HYDRO_CONTAINER_ITEM_IDS.length - 1
                ));
            }
        });
        return detectedTier[0];
    }

    public void queueHydroContainerSync(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        pendingHydroContainerSyncs.add(playerId);
    }

    public boolean ensureSpellbookItem(Player player) {
        if (player == null) {
            return false;
        }

        var entityRef = player.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        if (normalizeLegacySpellbookItem(player)) {
            return true;
        }

        if (playerHasSpellbook(player)) {
            return false;
        }

        player.giveItem(new ItemStack(DEFAULT_SPELLBOOK_ITEM_ID), entityRef, entityRef.getStore());
        player.sendMessage(Message.raw(
                "[MOTM] A Mentees spellbook has been placed in your inventory. "
                        + "Cast with Left Click / Right Click / Use while equipped. "
                        + "Ability 1 / 2 / 3 still work as alternate bindings. "
                        + "For the management/readout view, use /motm spellbook overview. "
                        + "Crouch + Use opens the spellbook overview."
        ));
        return true;
    }

    private boolean normalizeLegacySpellbookItem(Player player) {
        CombinedItemContainer inventory = getCombinedPlayerInventory(player);
        if (inventory == null) {
            return false;
        }

        List<Short> legacySlots = new ArrayList<>();
        inventory.forEach((slot, stack) -> {
            if (stack != null && LEGACY_NONWEAPON_SPELLBOOK_ITEM_IDS.contains(stack.getItemId())) {
                legacySlots.add(slot);
            }
        });

        if (legacySlots.isEmpty()) {
            return false;
        }

        boolean hasModernSpellbook = inventory.countItemStacks(
                stack -> stack != null && DEFAULT_SPELLBOOK_ITEM_ID.equals(stack.getItemId())
        ) > 0;

        var entityRef = player.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        for (short slot : legacySlots) {
            inventory.removeItemStackFromSlot(slot);
        }

        if (!hasModernSpellbook) {
            player.giveItem(new ItemStack(DEFAULT_SPELLBOOK_ITEM_ID), entityRef, entityRef.getStore());
        }

        player.sendMessage(Message.raw(
                "[MOTM] Your legacy spellbook has been updated to the new casting focus. "
                        + "Cast with Left Click / Right Click / Use while equipped."
        ));
        return true;
    }

    public boolean ensureDevBookItem(Player player) {
        if (!devToolsEnabled) {
            return false;
        }
        if (player == null) {
            return false;
        }

        var entityRef = player.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        if (playerHasDevBook(player)) {
            return false;
        }

        player.giveItem(new ItemStack(DEFAULT_DEV_GRIMOIRE_ITEM_ID), entityRef, entityRef.getStore());
        player.sendMessage(Message.raw(
                "[MOTM] A Dev Grimoire has been placed in your inventory. "
                        + "Use to open it, then Ability 1 / 2 / 3 to navigate."
        ));
        return true;
    }

    public String queueSpellbookGrant(Player player) {
        String playerId = findOnlinePlayerId(player);
        if (playerId == null) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        onlineRuntimePlayers.put(playerId, player);
        return queueSpellbookGrant(playerId);
    }

    public String queueSpellbookGrant(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        boolean added = pendingSpellbookGrants.add(playerId);
        return added
                ? "[MOTM] Spellbook delivery queued."
                : "[MOTM] Spellbook delivery is already queued.";
    }

    public String queueDevBookGrant(Player player) {
        if (!devToolsEnabled) {
            return devToolsDisabledMessage();
        }
        String playerId = findOnlinePlayerId(player);
        if (playerId == null) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        onlineRuntimePlayers.put(playerId, player);
        return queueDevBookGrant(playerId);
    }

    public String queueDevBookGrant(String playerId) {
        if (!devToolsEnabled) {
            return devToolsDisabledMessage();
        }
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        boolean added = pendingDevBookGrants.add(playerId);
        return added
                ? "[MOTM] Dev Grimoire delivery queued."
                : "[MOTM] Dev Grimoire delivery is already queued.";
    }

    public void queueAbilityCast(String playerId,
                                 String abilityId,
                                 com.hypixel.hytale.component.Ref<EntityStore> targetRef,
                                 Vector3i targetBlock,
                                 boolean notifyFailures) {
        if (playerId == null || playerId.isBlank() || abilityId == null || abilityId.isBlank()) {
            return;
        }
        pendingAbilityCasts.add(new PendingAbilityCast(playerId, abilityId, targetRef, targetBlock, notifyFailures));
    }

    public String startStyleTest(String playerId, String styleId) {
        if (!devToolsEnabled) {
            return devToolsDisabledMessage();
        }
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        Player runtimePlayer = onlineRuntimePlayers.get(playerId);
        var playerData = playerDataManager.getOnlinePlayer(playerId);
        if (runtimePlayer == null || playerData == null) {
            return "[MOTM] Join a world and run this in-game to start a live style test.";
        }

        StyleLookup styleLookup = findStyleLookup(styleId);
        if (styleLookup == null) {
            return "[MOTM] Unknown style '" + styleId + "'.";
        }

        playerData.setPlayerClass(styleLookup.classId());
        playerData.setFirstJoin(false);
        boolean selected = styleManager.selectStyles(playerData, List.of(styleLookup.style().getId()));
        if (!selected) {
            return "[MOTM] Failed to prepare style test for " + styleLookup.style().getName() + ".";
        }

        setFreeCastEnabled(playerId, true);
        playerDataManager.savePlayerData(playerData);
        rebuildPlayerRuntime(playerData);
        refreshStatusHud(playerId);

        List<String> abilityIds = styleLookup.style().getAbilities().stream()
                .map(AbilityData::getId)
                .toList();

        activeStyleTests.put(playerId, new ActiveStyleTest(
                playerId,
                styleLookup.classId(),
                styleLookup.style().getId(),
                styleLookup.style().getName(),
                abilityIds,
                0,
                System.currentTimeMillis() + 1200L
        ));

        return "[MOTM] Live style test queued: "
                + humanize(styleLookup.classId()) + " > " + styleLookup.style().getName()
                + ". Free-cast ON. The mod will fire the style abilities in sequence against nearby targets.";
    }

    public String stopStyleTest(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        ActiveStyleTest removed = activeStyleTests.remove(playerId);
        if (removed == null) {
            return "[MOTM] No active live style test is running.";
        }

        return "[MOTM] Stopped live style test for " + removed.styleName() + ".";
    }

    public String getStyleTestStatus(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        ActiveStyleTest active = activeStyleTests.get(playerId);
        if (active == null) {
            return "[MOTM] No active live style test is running.";
        }

        int total = active.abilityIds().size();
        int nextStep = Math.min(active.nextAbilityIndex() + 1, total);
        return "[MOTM] Live style test: "
                + humanize(active.classId()) + " > " + active.styleName()
                + " | step " + nextStep + "/" + total + ".";
    }

    private void installStatusHud(Player player) {
        if (!CUSTOM_HUD_ENABLED || player == null) {
            return;
        }

        var playerRef = getUniversePlayerRef(player);
        if (playerRef == null) {
            return;
        }

        MotmStatusHud hud = new MotmStatusHud(playerRef, this);
        statusHuds.put(playerRef.getUuid().toString(), hud);
        player.getHudManager().setCustomHud(playerRef, hud);
        try {
            // Keep the native hotbar, but let the MOTM HUD own the right-side spell lane.
            player.getHudManager().hideHudComponents(
                    playerRef,
                    HudComponent.StatusIcons,
                    HudComponent.InputBindings,
                    HudComponent.AmmoIndicator,
                    HudComponent.UtilitySlotSelector);
        } catch (Exception e) {
            LOG.warning("[MOTM] Failed to hide native HUD components: " + e.getMessage());
        }
    }

    private void processPendingInventoryGrants(Store<EntityStore> currentStore) {
        processPendingSpellbookGrants(currentStore);
        processPendingDevBookGrants(currentStore);
        processPendingHydroContainerSyncs(currentStore);
    }

    private void processPendingAbilityCasts(Store<EntityStore> currentStore) {
        for (PendingAbilityCast request : List.copyOf(pendingAbilityCasts)) {
            Player player = onlineRuntimePlayers.get(request.playerId());
            if (player == null) {
                pendingAbilityCasts.remove(request);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            String failureMessage = motmCommand.executeQueuedAbilityCast(
                    request.playerId(),
                    request.abilityId(),
                    player,
                    request.targetRef(),
                    request.targetBlock()
            );
            if (request.notifyFailures() && failureMessage != null && !failureMessage.isBlank()) {
                player.sendMessage(Message.raw(failureMessage));
            }
            pendingAbilityCasts.remove(request);
        }
    }

    private void processActiveStyleTests(Store<EntityStore> currentStore) {
        long now = System.currentTimeMillis();
        for (ActiveStyleTest test : List.copyOf(activeStyleTests.values())) {
            Player player = onlineRuntimePlayers.get(test.playerId());
            if (player == null) {
                activeStyleTests.remove(test.playerId());
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }
            if (now < test.nextActionAtMs()) {
                continue;
            }

            var playerData = playerDataManager.getOnlinePlayer(test.playerId());
            if (playerData == null) {
                activeStyleTests.remove(test.playerId());
                continue;
            }

            if (test.nextAbilityIndex() >= test.abilityIds().size()) {
                player.sendMessage(Message.raw("[MOTM] Live style test complete: "
                        + humanize(test.classId()) + " > " + test.styleName() + "."));
                activeStyleTests.remove(test.playerId());
                continue;
            }

            AbilityData ability = styleManager.findAbility(playerData, test.abilityIds().get(test.nextAbilityIndex()));
            if (ability == null) {
                player.sendMessage(Message.raw("[MOTM] Live style test skipped a missing ability at step "
                        + (test.nextAbilityIndex() + 1) + "."));
                activeStyleTests.put(test.playerId(), test.advance(now + 1200L));
                continue;
            }

            Ref<EntityStore> targetRef = findNearestStyleTestNpc(currentStore, player, 28.0);
            Vector3i targetBlock = resolveStyleTestTargetBlock(currentStore, player, targetRef);
            player.sendMessage(Message.raw("[MOTM] Live test step "
                    + (test.nextAbilityIndex() + 1) + "/" + test.abilityIds().size()
                    + ": " + ability.getName()));

            queueAbilityCast(test.playerId(), ability.getId(), targetRef, targetBlock, true);

            activeStyleTests.put(test.playerId(), test.advance(now + resolveStyleTestDelayMs(ability)));
        }
    }

    private void processPendingSpellbookGrants(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(pendingSpellbookGrants)) {
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingSpellbookGrants.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            boolean granted = ensureSpellbookItem(player);
            if (!granted && playerHasSpellbook(player)) {
                player.sendMessage(Message.raw("[MOTM] You already have a spellbook in your inventory."));
            }
            pendingSpellbookGrants.remove(playerId);
        }
    }

    private void processPendingDevBookGrants(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(pendingDevBookGrants)) {
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingDevBookGrants.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            boolean granted = ensureDevBookItem(player);
            if (!granted && playerHasDevBook(player)) {
                player.sendMessage(Message.raw("[MOTM] You already have a Dev Grimoire in your inventory."));
            }
            pendingDevBookGrants.remove(playerId);
        }
    }

    private void processPendingHydroContainerSyncs(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(pendingHydroContainerSyncs)) {
            Player player = onlineRuntimePlayers.get(playerId);
            var playerData = playerDataManager.getOnlinePlayer(playerId);
            if (player == null || playerData == null) {
                pendingHydroContainerSyncs.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            syncHydroContainerItem(player, playerData, false);
            pendingHydroContainerSyncs.remove(playerId);
        }
    }

    private void processPendingRuntimeRebuilds(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(pendingRuntimeRebuilds)) {
            Player player = onlineRuntimePlayers.get(playerId);
            var playerData = playerDataManager.getOnlinePlayer(playerId);
            if (player == null || playerData == null) {
                pendingRuntimeRebuilds.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            rebuildPlayerRuntimeNow(playerData);
            pendingRuntimeRebuilds.remove(playerId);
        }
    }

    private void processPendingStatusHudRefreshs(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(pendingStatusHudRefreshs)) {
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingStatusHudRefreshs.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            refreshStatusHudNow(playerId);
            pendingStatusHudRefreshs.remove(playerId);
        }
    }

    private void processPendingProgressionBonusRefreshs(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(pendingProgressionBonusRefreshs)) {
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingProgressionBonusRefreshs.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            refreshPlayerProgressionBonusesNow(playerId);
            pendingProgressionBonusRefreshs.remove(playerId);
        }
    }

    private void syncHydroContainerItem(Player player, com.motm.model.PlayerData playerData, boolean notify) {
        if (player == null || playerData == null || player.getInventory() == null) {
            return;
        }

        CombinedItemContainer inventory = getCombinedPlayerInventory(player);
        if (inventory == null) {
            return;
        }
        int containerCount = inventory.countItemStacks(this::isHydroContainerItem);
        boolean hydroClass = "hydro".equalsIgnoreCase(playerData.getPlayerClass());

        if (!hydroClass) {
            return;
        }

        int targetTier = Math.max(0, Math.min(
                playerData.getWaterContainerTier(),
                HYDRO_CONTAINER_ITEM_IDS.length - 1
        ));
        if (containerCount == 0 && targetTier <= 0) {
            return;
        }
        int correctCount = inventory.countItemStacks(stack -> isHydroContainerTier(stack, targetTier));
        if (containerCount == 1 && correctCount == 1) {
            return;
        }

        removeAllHydroContainerItems(inventory);

        var entityRef = player.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return;
        }

        player.giveItem(createHydroContainerStack(targetTier), entityRef, entityRef.getStore());
        if (notify) {
            player.sendMessage(Message.raw(
                    "[MOTM] Your Hydro waterskin is now "
                            + resourceManager.getWaterContainerInfo(playerData.getPlayerId())
                            + ". Keep it in your inventory like ammo while casting with your spellbook, then right-click a water source with your empty hand, spellbook, or waterskin to refill."
            ));
        }
    }

    private void removeAllHydroContainerItems(CombinedItemContainer inventory) {
        var hydroSlots = new ArrayList<Short>();
        inventory.forEach((slot, stack) -> {
            if (isHydroContainerItem(stack)) {
                hydroSlots.add(slot);
            }
        });

        for (short slot : hydroSlots) {
            inventory.removeItemStackFromSlot(slot);
        }
    }

    private void registerNativeHydroCraftingRecipe() {
        try {
            if (CraftingRecipe.getAssetMap().getAsset(HYDRO_LIGHT_WATERSKIN_RECIPE_ID) != null) {
                LOG.info("[MOTM] Native Hydro waterskin recipe already registered.");
                return;
            }

            CraftingRecipe recipe = createHydroWaterskinRecipe();
            CraftingRecipe.getAssetStore().loadAssets("MOTM:MOTM", List.of(recipe));

            if (CraftingRecipe.getAssetMap().getAsset(HYDRO_LIGHT_WATERSKIN_RECIPE_ID) != null) {
                LOG.info("[MOTM] Registered native Hydro waterskin fieldcraft recipe.");
            } else {
                LOG.warning("[MOTM] Hydro waterskin recipe load finished, but the recipe is not visible in the asset map.");
            }
        } catch (Exception e) {
            LOG.warning("[MOTM] Failed to register native Hydro waterskin recipe: " + e.getMessage());
        }
    }

    private CraftingRecipe createHydroWaterskinRecipe() throws ReflectiveOperationException {
        MaterialQuantity input = new MaterialQuantity(
                HYDRO_CONTAINER_ITEM_IDS[0],
                null,
                null,
                HYDRO_LIGHT_WATERSKIN_INPUT_COUNT,
                null
        );
        MaterialQuantity primaryOutput = new MaterialQuantity(
                HYDRO_CONTAINER_ITEM_IDS[0],
                null,
                null,
                1,
                createHydroContainerMetadata(0)
        );
        BenchRequirement fieldcraft = new BenchRequirement(
                BenchType.Crafting,
                CraftingRecipe.FIELDCRAFT_REQUIREMENT,
                null,
                0
        );

        CraftingRecipe recipe = new CraftingRecipe(
                new MaterialQuantity[]{input},
                primaryOutput,
                MaterialQuantity.EMPTY_ARRAY,
                1,
                new BenchRequirement[]{fieldcraft},
                0f,
                false,
                0
        );
        setCraftingRecipeId(recipe, HYDRO_LIGHT_WATERSKIN_RECIPE_ID);
        return recipe;
    }

    private BsonDocument createHydroContainerMetadata(int tier) {
        int clampedTier = Math.max(0, Math.min(tier, HYDRO_CONTAINER_ITEM_IDS.length - 1));
        BsonDocument metadata = new BsonDocument();
        metadata.put(HYDRO_CONTAINER_METADATA_KEY, BsonBoolean.TRUE);
        metadata.put(HYDRO_CONTAINER_TIER_METADATA_KEY, new BsonInt32(clampedTier));
        return metadata;
    }

    private void setCraftingRecipeId(CraftingRecipe recipe, String recipeId) throws ReflectiveOperationException {
        Field idField = CraftingRecipe.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(recipe, recipeId);
    }

    private void refreshAllStatusHuds(Store<EntityStore> currentStore) {
        statusHuds.entrySet().removeIf(entry -> playerDataManager.getOnlinePlayer(entry.getKey()) == null);
        statusHuds.forEach((playerId, hud) -> {
            Player runtimePlayer = onlineRuntimePlayers.get(playerId);
            if (runtimePlayer != null && isPlayerInStore(runtimePlayer, currentStore)) {
                hud.refresh();
            }
        });
    }

    private void refreshAllPlayerProgressionBonuses(Store<EntityStore> currentStore) {
        onlineRuntimePlayers.forEach((playerId, player) -> {
            if (isPlayerInStore(player, currentStore)) {
                refreshPlayerProgressionBonusesNow(playerId);
            }
        });
    }

    private int countTerraInventoryResource(String playerId, String resourceType) {
        Player player = getRuntimePlayer(playerId);
        if (player == null || player.getInventory() == null || resourceType == null || resourceType.isBlank()) {
            return 0;
        }

        int unitsPerItem = getTerraResourceUnitsPerItem(resourceType);
        CombinedItemContainer inventory = getCombinedPlayerInventory(player);
        if (inventory == null) {
            return 0;
        }
        final int[] total = {0};
        inventory.forEach((slot, stack) -> {
            if (matchesTerraResourceItem(stack, resourceType)) {
                total[0] += Math.max(0, stack.getQuantity()) * unitsPerItem;
            }
        });
        return total[0];
    }

    private boolean spendTerraInventoryResource(String playerId, String resourceType, int amount) {
        if (amount <= 0) {
            return true;
        }

        Player player = getRuntimePlayer(playerId);
        if (player == null || player.getInventory() == null || resourceType == null || resourceType.isBlank()) {
            return false;
        }

        CombinedItemContainer inventory = getCombinedPlayerInventory(player);
        if (inventory == null) {
            return false;
        }
        int unitsPerItem = getTerraResourceUnitsPerItem(resourceType);
        if (countTerraInventoryResource(playerId, resourceType) < amount) {
            return false;
        }

        int remaining = amount;
        var matchingSlots = new ArrayList<Short>();
        inventory.forEach((slot, stack) -> {
            if (matchesTerraResourceItem(stack, resourceType)) {
                matchingSlots.add(slot);
            }
        });

        for (short slot : matchingSlots) {
            if (remaining <= 0) {
                break;
            }

            ItemStack stack = inventory.getItemStack(slot);
            if (!matchesTerraResourceItem(stack, resourceType)) {
                continue;
            }

            int stackQuantity = Math.max(0, stack.getQuantity());
            if (stackQuantity <= 0) {
                continue;
            }

            int itemsNeeded = (int) Math.ceil(remaining / (double) unitsPerItem);
            int removeAmount = Math.min(itemsNeeded, stackQuantity);
            var transaction = inventory.removeItemStackFromSlot(slot, removeAmount);
            if (transaction != null && transaction.succeeded()) {
                ItemStack before = transaction.getSlotBefore();
                ItemStack after = transaction.getSlotAfter();
                int beforeQuantity = before == null ? 0 : Math.max(0, before.getQuantity());
                int afterQuantity = after == null ? 0 : Math.max(0, after.getQuantity());
                int removedItems = Math.max(0, beforeQuantity - afterQuantity);
                int removedUnits = removedItems * unitsPerItem;
                remaining -= removedUnits;
                if (remaining < 0) {
                    resourceManager.add(playerId, resourceType, Math.abs(remaining));
                    remaining = 0;
                }
            }
        }

        return remaining <= 0;
    }

    private boolean matchesTerraResourceItem(ItemStack stack, String resourceType) {
        if (stack == null || stack.getItemId() == null || resourceType == null || resourceType.isBlank()) {
            return false;
        }
        return matchesTerraResourceItemId(stack.getItemId(), resourceType);
    }

    private boolean matchesTerraResourceItemId(String itemId, String resourceType) {
        if (itemId == null || resourceType == null || resourceType.isBlank()) {
            return false;
        }

        return switch (resourceType) {
            case "stone_blocks" -> hasAnyPrefix(itemId, TERRA_STONE_ITEM_PREFIXES);
            case "dirt_blocks" -> hasAnyPrefix(itemId, TERRA_DIRT_ITEM_PREFIXES);
            case "sand_blocks" -> hasAnyPrefix(itemId, TERRA_SAND_ITEM_PREFIXES);
            case "metal" -> hasAnyPrefix(itemId, TERRA_METAL_ITEM_PREFIXES);
            case "gems" -> hasAnyPrefix(itemId, TERRA_GEM_ITEM_PREFIXES);
            case "seeds" -> hasAnyPrefix(itemId, TERRA_SEED_ITEM_PREFIXES);
            default -> false;
        };
    }

    private boolean hasAnyPrefix(String itemId, String[] prefixes) {
        if (itemId == null || prefixes == null) {
            return false;
        }
        for (String prefix : prefixes) {
            if (prefix != null && itemId.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    private int getTerraResourceUnitsPerItem(String resourceType) {
        if (resourceType == null || resourceType.isBlank()) {
            return 1;
        }

        return switch (resourceType) {
            case "stone_blocks" -> TERRA_STONE_UNITS_PER_ITEM;
            case "dirt_blocks" -> TERRA_DIRT_UNITS_PER_ITEM;
            case "sand_blocks" -> TERRA_SAND_UNITS_PER_ITEM;
            case "seeds" -> TERRA_SEED_UNITS_PER_ITEM;
            case "metal" -> TERRA_METAL_UNITS_PER_ITEM;
            case "gems" -> TERRA_GEM_UNITS_PER_ITEM;
            default -> 1;
        };
    }

    public void refreshStatusHud(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        pendingStatusHudRefreshs.add(playerId);
    }

    private void refreshStatusHudNow(String playerId) {
        MotmStatusHud hud = statusHuds.get(playerId);
        if (hud != null) {
            hud.refresh();
        }
    }

    public Player getRuntimePlayer(String playerId) {
        return playerId == null ? null : onlineRuntimePlayers.get(playerId);
    }

    public String findOnlinePlayerId(Player runtimePlayer) {
        if (runtimePlayer == null) {
            return null;
        }

        for (Map.Entry<String, Player> entry : onlineRuntimePlayers.entrySet()) {
            if (entry.getValue() == runtimePlayer) {
                return entry.getKey();
            }
        }
        return null;
    }

    public PlayerRef getUniversePlayerRef(Player player) {
        if (player == null) {
            return null;
        }

        try {
            var entityRef = player.getReference();
            if (entityRef == null || !entityRef.isValid()) {
                return null;
            }

            return entityRef.getStore().getComponent(entityRef, PlayerRef.getComponentType());
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    public String getRuntimePlayerId(Player player) {
        String cachedPlayerId = findOnlinePlayerId(player);
        if (cachedPlayerId != null) {
            return cachedPlayerId;
        }

        try {
            PlayerRef playerRef = getUniversePlayerRef(player);
            return playerRef != null && playerRef.getUuid() != null
                    ? playerRef.getUuid().toString()
                    : null;
        } catch (IllegalStateException ignored) {
            return null;
        }
    }

    private boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
        if (player == null || currentStore == null) {
            return false;
        }

        var playerRef = player.getReference();
        return playerRef != null && playerRef.isValid() && playerRef.getStore() == currentStore;
    }

    private CombinedItemContainer getCombinedPlayerInventory(Player player) {
        if (player == null || player.getInventory() == null) {
            return null;
        }

        var inventory = player.getInventory();
        var containers = new ArrayList<ItemContainer>(6);
        addInventoryContainer(containers, inventory.getHotbar());
        addInventoryContainer(containers, inventory.getStorage());
        addInventoryContainer(containers, inventory.getBackpack());
        addInventoryContainer(containers, inventory.getUtility());
        addInventoryContainer(containers, inventory.getTools());
        addInventoryContainer(containers, inventory.getArmor());
        if (containers.isEmpty()) {
            return null;
        }

        return new CombinedItemContainer(containers.toArray(ItemContainer[]::new));
    }

    private void addInventoryContainer(ArrayList<ItemContainer> containers, ItemContainer container) {
        if (container != null) {
            containers.add(container);
        }
    }

    public void rebuildPlayerRuntime(com.motm.model.PlayerData player) {
        if (player == null || player.getPlayerId() == null) {
            return;
        }
        pendingRuntimeRebuilds.add(player.getPlayerId());
    }

    private void rebuildPlayerRuntimeNow(com.motm.model.PlayerData player) {
        if (player == null || player.getPlayerId() == null) {
            return;
        }

        String playerId = player.getPlayerId();

        styleManager.resetCooldowns(playerId);
        classPassiveManager.clearPlayerState(playerId);
        statusEffectManager.clearEffects(playerId);
        elementalReactionManager.clearMarks(playerId);
        resourceManager.clearPlayerState(playerId);
        resourceManager.synchronizePersistentState(player);

        player.clearSynergyBonuses();
        player.clearRaceBonuses();

        if (player.getPlayerClass() == null) {
            refreshStatusHudNow(playerId);
            return;
        }

        resourceManager.initializeForPlayer(player);
        perkManager.reapplyAllPerks(player, synergyEngine);
        queueHydroContainerSync(playerId);

        if (player.getRace() != null) {
            raceManager.applyRaceBonuses(player, statusEffectManager);
        }
        classPassiveManager.onPlayerJoin(player);
        refreshStatusHudNow(playerId);
    }

    public int getAverageOnlinePlayerLevel() {
        return levelingManager.calculateAverageOnlineLevel(playerDataManager.getAllOnlinePlayers());
    }

    public int getAverageOnlinePlayerLevelForPlayer(String playerId) {
        if (playerId == null) {
            return getAverageOnlinePlayerLevel();
        }

        Player runtimePlayer = onlineRuntimePlayers.get(playerId);
        if (runtimePlayer == null) {
            return getAverageOnlinePlayerLevel();
        }

        World world = runtimePlayer.getWorld();
        if (world == null) {
            return getAverageOnlinePlayerLevel();
        }

        return calculateAverageOnlinePlayerLevelForWorld(world);
    }

    public void refreshPlayerProgressionBonuses(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        pendingProgressionBonusRefreshs.add(playerId);
    }

    private void refreshPlayerProgressionBonusesNow(String playerId) {
        if (playerId == null) {
            return;
        }

        Player runtimePlayer = onlineRuntimePlayers.get(playerId);
        var playerData = playerDataManager.getOnlinePlayer(playerId);
        if (runtimePlayer == null || playerData == null) {
            return;
        }

        applyPlayerLevelHealthBonus(runtimePlayer, playerData);
    }

    private void applyPlayerLevelHealthBonus(Player runtimePlayer, com.motm.model.PlayerData playerData) {
        if (runtimePlayer == null || playerData == null) {
            return;
        }

        var playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return;
        }

        EntityStatMap entityStatMap = playerRef.getStore().getComponent(playerRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return;
        }

        float healthMultiplier = (float) levelingManager.getPlayerMaxHealthMultiplier(playerData.getLevel());
        if (!Float.isFinite(healthMultiplier) || healthMultiplier <= 0f) {
            return;
        }

        entityStatMap.putModifier(
                DefaultEntityStatTypes.getHealth(),
                PLAYER_LEVEL_HEALTH_MODIFIER_ID,
                new StaticModifier(
                        Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.MULTIPLICATIVE,
                        healthMultiplier
                )
        );
    }

    private int calculateAverageOnlinePlayerLevelForWorld(World world) {
        if (world == null) {
            return 1;
        }

        int totalLevels = 0;
        int count = 0;
        for (var entry : onlineRuntimePlayers.entrySet()) {
            Player candidate = entry.getValue();
            if (candidate == null || candidate.getWorld() != world) {
                continue;
            }

            var playerData = playerDataManager.getOnlinePlayer(entry.getKey());
            if (playerData == null) {
                continue;
            }

            totalLevels += Math.max(1, playerData.getLevel());
            count++;
        }

        if (count == 0) {
            return getAverageOnlinePlayerLevel();
        }

        return Math.max(1, (int) Math.round(totalLevels / (double) count));
    }

    private long resolveStyleTestDelayMs(AbilityData ability) {
        if (ability == null) {
            return 1500L;
        }

        double seconds = Math.max(
                1.2,
                styleManager.getCastTimeSeconds(ability)
                        + styleManager.getRecoveryTimeSeconds(ability)
                        + Math.max(ability.getDurationSeconds(), ability.getDelaySeconds()) * 0.6
        );
        return Math.min(5000L, Math.round(seconds * 1000.0));
    }

    private Ref<EntityStore> findNearestStyleTestNpc(Store<EntityStore> store, Player player, double radius) {
        Vector3d playerPosition = getPlayerPosition(player);
        if (playerPosition == null) {
            return null;
        }

        final Ref<EntityStore>[] nearest = new Ref[]{null};
        final double[] bestDistance = {Double.MAX_VALUE};

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid()) {
                    continue;
                }

                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning()) {
                    continue;
                }
                if ("motm_summon".equalsIgnoreCase(npc.getRoleName())) {
                    continue;
                }
                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }

                TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
                if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                    continue;
                }

                double candidateDistance = distance(playerPosition, transform.getTransform().getPosition());
                if (candidateDistance <= radius && candidateDistance < bestDistance[0]) {
                    bestDistance[0] = candidateDistance;
                    nearest[0] = ref;
                }
            }
        });

        return nearest[0];
    }

    private Vector3i resolveStyleTestTargetBlock(Store<EntityStore> store,
                                                 Player player,
                                                 Ref<EntityStore> targetRef) {
        Vector3d targetPosition = getEntityPosition(store, targetRef);
        if (targetPosition != null) {
            return new Vector3i(
                    (int) Math.floor(targetPosition.x),
                    (int) Math.floor(targetPosition.y),
                    (int) Math.floor(targetPosition.z)
            );
        }

        Vector3d playerPosition = getPlayerPosition(player);
        if (playerPosition == null) {
            return null;
        }

        return new Vector3i(
                (int) Math.floor(playerPosition.x),
                (int) Math.floor(playerPosition.y),
                (int) Math.floor(playerPosition.z)
        );
    }

    private Vector3d getPlayerPosition(Player player) {
        if (player == null) {
            return null;
        }

        var playerRef = player.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return null;
        }

        TransformComponent transform = playerRef.getStore().getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }

        return transform.getTransform().getPosition();
    }

    private Vector3d getEntityPosition(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return null;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }

        return transform.getTransform().getPosition();
    }

    private double distance(Vector3d a, Vector3d b) {
        if (a == null || b == null) {
            return Double.MAX_VALUE;
        }

        double dx = a.x - b.x;
        double dy = a.y - b.y;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private int resolveMobScalingAnchorLevel(String category, String playerId, com.motm.model.PlayerData player) {
        if (player == null) {
            return 1;
        }

        if (!mobScalingManager.isScalingCategory(category) && !mobScalingManager.isBossCategory(category)) {
            return player.getLevel();
        }

        return Math.max(1, getAverageOnlinePlayerLevelForPlayer(playerId));
    }

    private boolean isPlayerCrouching(Player player) {
        if (player == null) {
            return false;
        }

        var entityRef = player.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            return false;
        }

        var store = entityRef.getStore();
        if (store == null) {
            return false;
        }

        MovementStatesComponent movementStates = store.getComponent(
                entityRef,
                MovementStatesComponent.getComponentType()
        );
        return movementStates != null
                && movementStates.getMovementStates() != null
                && movementStates.getMovementStates().crouching;
    }

    private StyleLookup findStyleLookup(String styleId) {
        String normalizedStyleId = styleId == null ? "" : styleId.trim().toLowerCase(Locale.ROOT);
        if (normalizedStyleId.isBlank() || dataLoader == null) {
            return null;
        }

        for (String classId : List.of("terra", "hydro", "aero", "corruptus")) {
            StyleData style = dataLoader.getStyleById(normalizedStyleId, classId);
            if (style != null) {
                return new StyleLookup(classId, style);
            }
        }

        return null;
    }

    private String humanize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Unknown";
        }

        String[] parts = raw.replace('-', '_').split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    private void handlePlayerInteract(PlayerInteractEvent event) {
        try {
            Player player = event.getPlayer();
            if (player == null) {
                return;
            }

            String playerId = getRuntimePlayerId(player);
            var playerData = playerId != null ? playerDataManager.getOnlinePlayer(playerId) : null;
            if (playerData == null) {
                return;
            }

            ItemStack eventItemInHand = event.getItemInHand();
            ItemStack inventoryItemInHand = player.getInventory() != null ? player.getInventory().getItemInHand() : null;
            ItemStack itemInHand = eventItemInHand != null && !eventItemInHand.isEmpty()
                    ? eventItemInHand
                    : inventoryItemInHand;
            boolean holdingSpellbook = isSpellbookItem(itemInHand);
            boolean holdingDevBook = isDevBookItem(itemInHand);
            boolean crouching = isPlayerCrouching(player);
            InteractionType actionType = event.getActionType();
            int bookSlot = switch (actionType) {
                case Ability1 -> 1;
                case Ability2 -> 2;
                case Ability3 -> 3;
                case Use -> 3;
                default -> 0;
            };
            boolean openSpellbookGesture = holdingSpellbook && crouching && actionType == InteractionType.Use;
            boolean navigateSpellbookGesture = holdingSpellbook && crouching && bookSlot > 0;
            boolean openDevBookGesture = holdingDevBook && actionType == InteractionType.Use;
            boolean navigateDevBookGesture = holdingDevBook && bookSlot > 0;

            if (openSpellbookGesture || navigateSpellbookGesture || openDevBookGesture || navigateDevBookGesture) {
                event.setCancelled(true);

                String response = null;
                if (openDevBookGesture) {
                    response = devToolsEnabled
                            ? bookInteractionManager.cycleDevPage(playerData)
                            : devToolsDisabledMessage();
                } else if (openSpellbookGesture) {
                    if (!openSpellbook(player, SpellbookManager.Section.OVERVIEW)) {
                        response = bookInteractionManager.openSpellbook(playerData);
                    }
                } else {
                    response = holdingDevBook
                            ? (devToolsEnabled
                            ? bookInteractionManager.handleDevBookAction(playerData, bookSlot)
                            : devToolsDisabledMessage())
                            : bookInteractionManager.handleSpellbookAction(playerData, bookSlot);
                }

                if (response != null && !response.isBlank()) {
                    player.sendMessage(Message.raw(response));
                }
                return;
            }

            if (tryHandleHydroContainerRefill(event, player, playerData, itemInHand, holdingSpellbook)) {
                return;
            }

            int slot = bookSlot;
            if (slot <= 0) {
                return;
            }

            if (!holdingSpellbook) {
                return;
            }

            // Never let the spellbook fall through into native place/use behavior.
            event.setCancelled(true);

            if (playerData.getPlayerClass() == null
                    || playerData.getSelectedStyles() == null
                    || playerData.getSelectedStyles().isEmpty()) {
                player.sendMessage(Message.raw("[MOTM] Select a style first with /motm style <styleId>."));
                return;
            }

            String response = tryCastSpellbookSlot(
                    player,
                    playerData,
                    slot,
                    "interact:" + actionType,
                    event.getTargetRef(),
                    event.getTargetBlock()
            );
            if (response != null && !response.isBlank()) {
                player.sendMessage(Message.raw(response));
            }
        } catch (Exception e) {
            LOG.severe("[MOTM] PlayerInteract handling failed safely: " + e.getMessage());
        }
    }

    private boolean tryHandleHydroContainerRefill(
            PlayerInteractEvent event,
            Player player,
            com.motm.model.PlayerData playerData,
            ItemStack itemInHand,
            boolean holdingSpellbook
    ) {
        if (event.getActionType() != InteractionType.Use) {
            return false;
        }
        if (!"hydro".equalsIgnoreCase(playerData.getPlayerClass())) {
            return false;
        }
        if (!playerHasHydroContainer(player)) {
            return false;
        }
        if (!canAttemptHydroContainerRefill(itemInHand, holdingSpellbook)) {
            return false;
        }

        Vector3i targetBlock = event.getTargetBlock();
        if (!isWaterSourceBlock(player.getWorld(), targetBlock)) {
            return false;
        }

        String playerId = playerData.getPlayerId();
        int currentWater = resourceManager.getAmount(playerId, "water");
        int maxWater = resourceManager.getMaxAmount(playerId, "water");
        event.setCancelled(true);

        if (currentWater >= maxWater) {
            player.sendMessage(Message.raw(
                    "[MOTM] " + resourceManager.getWaterContainerInfo(playerId)
                            + " is already full (" + currentWater + "/" + maxWater + ")."
            ));
            return true;
        }

        resourceManager.refillWater(playerId);
        resourceManager.syncToPersistentState(playerData);
        playerDataManager.savePlayerData(playerData);
        refreshStatusHud(playerId);
        player.sendMessage(Message.raw(
                "[MOTM] Refilled " + resourceManager.getWaterContainerInfo(playerId)
                        + " from the water source."
        ));
        return true;
    }

    private boolean canAttemptHydroContainerRefill(ItemStack itemInHand, boolean holdingSpellbook) {
        return holdingSpellbook || itemInHand == null || isHydroContainerItem(itemInHand);
    }

    @SuppressWarnings("removal")
    private boolean isWaterSourceBlock(World world, Vector3i targetBlock) {
        if (world == null || targetBlock == null) {
            return false;
        }

        WorldChunk chunk = world.getChunkIfLoaded(ChunkUtil.indexChunkFromBlock(targetBlock.getX(), targetBlock.getZ()));
        if (chunk == null) {
            chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(targetBlock.getX(), targetBlock.getZ()));
        }
        if (chunk == null) {
            return false;
        }

        int localX = ChunkUtil.localCoordinate(targetBlock.getX());
        int localZ = ChunkUtil.localCoordinate(targetBlock.getZ());
        int y = targetBlock.getY();
        var blockType = chunk.getBlockType(localX, y, localZ);
        String blockId = blockType != null ? blockType.getId() : null;
        if (blockId != null) {
            String normalized = blockId.toLowerCase(Locale.ROOT);
            if (normalized.contains("water")) {
                return true;
            }
            if (normalized.contains("lava")) {
                return false;
            }
        }

        return world.getFluidId(targetBlock.getX(), y, targetBlock.getZ()) != 0;
    }

    private void handlePlayerMouseButton(PlayerMouseButtonEvent event) {
        try {
            Player player = event.getPlayer();
            if (player == null || event.getMouseButton() == null) {
                return;
            }

            if (event.getMouseButton().state != MouseButtonState.Pressed) {
                return;
            }

            String playerId = getRuntimePlayerId(player);
            var playerData = playerId != null ? playerDataManager.getOnlinePlayer(playerId) : null;
            if (playerData == null) {
                return;
            }

            var eventItemInHand = event.getItemInHand();
            ItemStack inventoryItemInHand = player.getInventory() != null ? player.getInventory().getItemInHand() : null;
            String itemId = resolveMouseButtonItemId(eventItemInHand, inventoryItemInHand);
            if (itemId == null || itemId.isBlank()) {
                return;
            }
            if (isSpellbookItemId(itemId)) {
                int slot = switch (event.getMouseButton().mouseButtonType) {
                    case Left -> 1;
                    case Right -> 2;
                    default -> 0;
                };
                if (slot > 0) {
                    event.setCancelled(true);
                    String response = tryCastSpellbookSlot(
                            player,
                            playerData,
                            slot,
                            "mouse:" + event.getMouseButton().mouseButtonType,
                            event.getTargetEntity() != null ? event.getTargetEntity().getReference() : null,
                            null
                    );
                    if (response != null && !response.isBlank()) {
                        player.sendMessage(Message.raw(response));
                    }
                }
                return;
            }

            if (event.getMouseButton().mouseButtonType != MouseButtonType.Left) {
                return;
            }

            if (isDevBookItemId(itemId)) {
                return;
            }

            if (eventItemInHand == null || eventItemInHand.getWeapon() == null) {
                return;
            }

            if (event.getTargetEntity() == null || event.getTargetEntity().getReference() == null) {
                return;
            }

            String response = gameplayPlaybackManager.handleWeaponFollowUpHit(
                    player,
                    playerData,
                    event.getTargetEntity().getReference(),
                    itemId
            );
            if (response != null && !response.isBlank()) {
                player.sendMessage(Message.raw(response));
            }
        } catch (Exception e) {
            LOG.severe("[MOTM] PlayerMouseButton handling failed safely: " + e.getMessage());
        }
    }

    private String tryCastSpellbookSlot(Player player,
                                        com.motm.model.PlayerData playerData,
                                        int slot,
                                        String source,
                                        com.hypixel.hytale.component.Ref<EntityStore> targetRef,
                                        Vector3i targetBlock) {
        if (player == null || playerData == null || slot <= 0) {
            return "";
        }

        String playerId = playerData.getPlayerId();
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        if (isDuplicateSpellbookInput(playerId, slot)) {
            return "";
        }

        LOG.info("[MOTM] Spellbook cast attempt: player="
                + playerData.getPlayerName()
                + " slot=" + slot
                + " source=" + source);

        return motmCommand.castAbilityBySlot(player, slot, targetRef, targetBlock);
    }

    private boolean isDuplicateSpellbookInput(String playerId, int slot) {
        if (playerId == null || playerId.isBlank() || slot <= 0) {
            return false;
        }

        long now = System.currentTimeMillis();
        String key = playerId + ":" + slot;
        Long previous = recentSpellbookSlotInputs.put(key, now);
        recentSpellbookSlotInputs.entrySet().removeIf(entry -> now - entry.getValue() > 1000L);
        return previous != null && now - previous < SPELLBOOK_INPUT_DEBOUNCE_MS;
    }

    private String resolveMouseButtonItemId(
            com.hypixel.hytale.server.core.asset.type.item.config.Item eventItemInHand,
            ItemStack inventoryItemInHand
    ) {
        if (eventItemInHand != null && eventItemInHand.getId() != null && !eventItemInHand.getId().isBlank()) {
            return eventItemInHand.getId();
        }
        return inventoryItemInHand != null ? inventoryItemInHand.getItemId() : null;
    }

    private void handleDamageBlock(DamageBlockEvent event) {
        if (event == null || event.getItemInHand() == null || event.getTargetBlock() == null) {
            return;
        }

        String itemId = event.getItemInHand().getItemId();
        if (!isPickaxeItemId(itemId)) {
            return;
        }

        Player terraMiner = resolveTerraMinerForBlockDamage(event);
        if (terraMiner == null) {
            return;
        }

        event.setDamage(event.getDamage() * 1.5f);
    }

    private Player resolveTerraMinerForBlockDamage(DamageBlockEvent event) {
        if (event == null || event.getTargetBlock() == null || event.getItemInHand() == null) {
            return null;
        }

        String eventItemId = event.getItemInHand().getItemId();
        if (eventItemId == null || eventItemId.isBlank()) {
            return null;
        }

        Vector3i targetBlock = event.getTargetBlock();
        double targetX = targetBlock.getX() + 0.5;
        double targetY = targetBlock.getY() + 0.5;
        double targetZ = targetBlock.getZ() + 0.5;

        Player bestMatch = null;
        double bestDistance = Double.MAX_VALUE;

        for (Map.Entry<String, Player> entry : onlineRuntimePlayers.entrySet()) {
            Player candidate = entry.getValue();
            if (candidate == null || candidate.getInventory() == null) {
                continue;
            }

            var playerData = playerDataManager.getOnlinePlayer(entry.getKey());
            if (playerData == null || !"terra".equalsIgnoreCase(playerData.getPlayerClass())) {
                continue;
            }

            ItemStack itemInHand = candidate.getInventory().getItemInHand();
            if (itemInHand == null || itemInHand.isEmpty() || !eventItemId.equalsIgnoreCase(itemInHand.getItemId())) {
                continue;
            }

            var playerRef = candidate.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                continue;
            }

            TransformComponent transform = playerRef.getStore().getComponent(
                    playerRef,
                    TransformComponent.getComponentType()
            );
            if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                continue;
            }

            var position = transform.getTransform().getPosition();
            double dx = position.x - targetX;
            double dy = position.y - targetY;
            double dz = position.z - targetZ;
            double distance = Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
            if (distance > 7.5 || distance >= bestDistance) {
                continue;
            }

            bestDistance = distance;
            bestMatch = candidate;
        }

        return bestMatch;
    }

    private boolean isPickaxeItemId(String itemId) {
        if (itemId == null || itemId.isBlank()) {
            return false;
        }

        String normalized = itemId.toLowerCase(Locale.ROOT);
        return normalized.contains("pickaxe") || normalized.contains("_pick");
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
    public ClassPassiveManager getClassPassiveManager() { return classPassiveManager; }
    public StyleManager getStyleManager() { return styleManager; }
    public ElementalReactionManager getElementalReactionManager() { return elementalReactionManager; }
    public RaceManager getRaceManager() { return raceManager; }
    public SpellbookManager getSpellbookManager() { return spellbookManager; }
    public BookInteractionManager getBookInteractionManager() { return bookInteractionManager; }
    public GameplayPlaybackManager getGameplayPlaybackManager() { return gameplayPlaybackManager; }
    public Path getPluginDirectory() { return pluginDirectory; }
    public String getDefaultSpellbookItemId() { return DEFAULT_SPELLBOOK_ITEM_ID; }
    public Set<String> getRecognizedSpellbookItemIds() { return SPELLBOOK_ITEM_IDS; }
    public Set<String> getRecognizedDevBookItemIds() { return DEV_GRIMOIRE_ITEM_IDS; }
    public boolean isCustomHudEnabled() { return CUSTOM_HUD_ENABLED; }
    public MotmPreflightAudit.AuditReport runPreflightAudit() {
        lastPreflightAudit = MotmPreflightAudit.run(this);
        return lastPreflightAudit;
    }
    public MotmPreflightAudit.AuditReport getLastPreflightAudit() { return lastPreflightAudit; }
    public boolean isDevToolsEnabled() { return MotmBuildInfo.INTERNAL_TEST_BUILD && devToolsEnabled; }
    public boolean isInternalTestBuild() { return MotmBuildInfo.INTERNAL_TEST_BUILD; }
    public String getBuildChannel() { return MotmBuildInfo.BUILD_CHANNEL; }
    public boolean isFreeCastEnabled(String playerId) {
        return playerId != null && freeCastPlayers.contains(playerId);
    }
    public void setFreeCastEnabled(String playerId, boolean enabled) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        if (enabled) {
            freeCastPlayers.add(playerId);
        } else {
            freeCastPlayers.remove(playerId);
        }
    }
    public String devToolsDisabledMessage() {
        if (!MotmBuildInfo.INTERNAL_TEST_BUILD) {
            return "[MOTM] Dev tools are not included in this public release build.\n"
                    + "Use an internal tester build to access /motm dev and live automation commands.";
        }
        return "[MOTM] Dev tools are disabled on this build/server.\n"
                + "To enable them, set dev_tools_enabled=true in "
                + SERVER_CONFIG_FILE_NAME + " inside the mod data folder and restart Hytale.";
    }

    // --- Result wrapper for mob spawn scaling ---

    public record ScaledMobResult(
            com.motm.model.MobStats stats,
            int level,
            String displayName,
            String levelColor
    ) {}

    private record PendingAbilityCast(
            String playerId,
            String abilityId,
            com.hypixel.hytale.component.Ref<EntityStore> targetRef,
            Vector3i targetBlock,
            boolean notifyFailures
    ) {}

    private record StyleLookup(
            String classId,
            StyleData style
    ) {}

    private record ActiveStyleTest(
            String playerId,
            String classId,
            String styleId,
            String styleName,
            List<String> abilityIds,
            int nextAbilityIndex,
            long nextActionAtMs
    ) {
        private ActiveStyleTest advance(long nextActionAtMs) {
            return new ActiveStyleTest(
                    playerId,
                    classId,
                    styleId,
                    styleName,
                    abilityIds,
                    nextAbilityIndex + 1,
                    nextActionAtMs
            );
        }
    }
}
