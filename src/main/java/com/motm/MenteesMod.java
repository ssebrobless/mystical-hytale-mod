package com.motm;

import com.motm.command.MotmCommand;
import com.motm.command.MotmCommandBase;
import com.motm.interaction.MotmSpellbookInteraction;
import com.motm.manager.*;
import com.motm.model.AbilityData;
import com.motm.model.Perk;
import com.motm.model.PerkTriggerBinding;
import com.motm.model.PlayerData;
import com.motm.model.StatusEffect;
import com.motm.model.StyleData;
import com.motm.system.MotmDamageEventSystem;
import com.motm.system.MotmMobRuntimeSystem;
import com.motm.system.MotmServerTickSystem;
import com.motm.ui.MotmStatusHud;
import com.motm.ui.SpellbookPage;
import com.motm.util.DataLoader;
import com.motm.util.MotmObservability;
import com.motm.util.MotmPreflightAudit;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.ActiveEntityEffect;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerConnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.io.adapter.PacketAdapters;
import com.hypixel.hytale.server.core.io.adapter.PacketFilter;
import com.hypixel.hytale.server.core.io.adapter.PlayerPacketWatcher;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.modules.entity.component.Invulnerable;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.modules.time.WorldTimeResource;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BenchRequirement;
import com.hypixel.hytale.protocol.BenchType;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.protocol.MouseButtonState;
import com.hypixel.hytale.protocol.MouseButtonType;
import com.hypixel.hytale.protocol.Packet;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonInt32;
import org.bson.BsonValue;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardOpenOption;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private static final String DEFAULT_SPELLBOOK_ITEM_ID = "MOTM_Spellbook_Focus";
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
    private static final List<TerraReviewKitItem> TERRA_REVIEW_KIT_ITEMS = List.of(
            new TerraReviewKitItem("Tool_Pickaxe_Iron", 1, "pickaxe mining affinity"),
            new TerraReviewKitItem("Tool_Pickaxe_Wood", 1, "baseline pickaxe mining comparison"),
            new TerraReviewKitItem("Tool_Shovel_Iron", 1, "non-pickaxe negative mining control"),
            new TerraReviewKitItem("Weapon_Sword_Iron", 1, "physical melee weapon tests"),
            new TerraReviewKitItem("Weapon_Shield_Iron", 1, "durability shield / blocking tests"),
            new TerraReviewKitItem("Rock_Stone", 64, "stone/terrain test material"),
            new TerraReviewKitItem("Soil_Dirt", 64, "dirt/terrain test material"),
            new TerraReviewKitItem("Soil_Sand", 64, "sand/terrain test material"),
            new TerraReviewKitItem("Ingredient_Bar_Iron", 32, "metal visual test material"),
            new TerraReviewKitItem("Rock_Gem_Emerald", 16, "gem visual test material"),
            new TerraReviewKitItem("Plant_Seeds_Wheat", 32, "plant visual test material"),
            new TerraReviewKitItem("Rock_Crystal_Green_Block", 16, "green crystal/gem visual blocks"),
            new TerraReviewKitItem("Build_GreyDark_Cube", 32, "dark stone/metal visual block"),
            new TerraReviewKitItem("Build_Grey_Cube", 32, "neutral review marker block")
    );
    private static final String PLAYER_LEVEL_HEALTH_MODIFIER_ID = "motm_player_level_health";
    private static final boolean CUSTOM_PAGE_UI_ENABLED = true;
    private static final boolean CUSTOM_HUD_ENABLED = true;
    private static final String SERVER_CONFIG_FILE_NAME = "motm-server.properties";
    private static final String DEV_COMMAND_INBOX_FILE_NAME = "dev-command-inbox.txt";
    private static final String DEV_COMMAND_OUTBOX_FILE_NAME = "dev-command-outbox.log";
    private static final long DEV_COMMAND_INBOX_POLL_INTERVAL_MS = 250L;
    private static final int HUD_REFRESH_INTERVAL_TICKS = 4;
    private static final int HUD_INSTALL_DELAY_TICKS = 4;
    private static final long SPELLBOOK_INPUT_DEBOUNCE_MS = 150L;
    private static final Set<String> LEGACY_NONWEAPON_SPELLBOOK_ITEM_IDS = Set.of(
            "Recipe_Book_Magic_Air",
            "Weapon_Spellbook_Grimoire_Brown",
            "Weapon_Spellbook_Grimoire_Purple",
            "Weapon_Spellbook_Frost",
            "Weapon_Spellbook_Fire",
            "Weapon_Spellbook_Rekindle_Embers"
    );
    private static final Set<String> SPELLBOOK_ITEM_IDS = Set.of(
            DEFAULT_SPELLBOOK_ITEM_ID,
            "Weapon_Spellbook_Grimoire_Purple",
            "Weapon_Spellbook_Frost",
            "Weapon_Spellbook_Fire",
            "Weapon_Spellbook_Rekindle_Embers",
            "Weapon_Spellbook_Grimoire_Brown"
    );
    private static final Set<String> DEV_GRIMOIRE_ITEM_IDS = Set.of(
            DEFAULT_DEV_GRIMOIRE_ITEM_ID
    );
    private static final Set<String> TERRA_REVIEW_ESSENTIAL_ITEM_IDS = Set.of(
            DEFAULT_SPELLBOOK_ITEM_ID,
            "Tool_Pickaxe_Iron",
            "Weapon_Sword_Iron"
    );
    private static final Set<String> HYDRO_CONTAINER_ID_SET = Set.of(HYDRO_CONTAINER_ITEM_IDS);
    private static final Set<String> STYLE_TEST_CLEANUP_ROLES = Set.of(
            "Goblin_Scrapper",
            "Test_Dummy_Stationary",
            "Bat",
            "Empty_Role",
            "Slug_Magma",
            "Spark_Living"
    );

    // Core systems
    private DataLoader dataLoader;
    private PerkManager perkManager;
    private PlayerStatModifierManager playerStatModifierManager;
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
    private MotmObservability observability;
    private final ThreadLocal<String> observabilityTraceContext = new ThreadLocal<>();
    private PacketFilter observabilityInboundPacketFilter;
    private PacketFilter observabilityOutboundPacketFilter;
    private boolean devToolsEnabled = false;
    private final Map<String, MotmStatusHud> statusHuds = new ConcurrentHashMap<>();
    private final Map<String, Player> onlineRuntimePlayers = new ConcurrentHashMap<>();
    private final Map<String, List<PerkTriggerBinding>> perkTriggersByPlayer = new ConcurrentHashMap<>();
    private final Set<String> pendingSpellbookGrants = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingDevBookGrants = ConcurrentHashMap.newKeySet();
    private final Map<String, String> pendingStyleTestMobSpawns = new ConcurrentHashMap<>();
    private final Set<String> pendingStyleTestMobClears = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingStyleTestMobCounts = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingStyleReviewResets = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingDaylightRequests = ConcurrentHashMap.newKeySet();
    private final Map<String, GameMode> pendingDevGameModeChanges = new ConcurrentHashMap<>();
    private final Set<String> pendingTerraReviewKitGrants = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingTerraReviewInventoryCleans = ConcurrentHashMap.newKeySet();
    private final Map<String, List<Ref<EntityStore>>> styleTestTargetsByPlayer = new ConcurrentHashMap<>();
    private final Map<String, String> pendingSingleAbilityTests = new ConcurrentHashMap<>();
    private final Map<String, String> pendingProofRequests = new ConcurrentHashMap<>();
    private final Map<String, String> pendingDevRelocations = new ConcurrentHashMap<>();
    private final Queue<TemporaryProofSelection> activeProofSelections = new ConcurrentLinkedQueue<>();
    private final Queue<TemporaryProofProxy> activeProofProxies = new ConcurrentLinkedQueue<>();
    private final Set<String> pendingHydroContainerSyncs = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingRuntimeRebuilds = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingStatusHudRefreshs = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> pendingStatusHudInstalls = new ConcurrentHashMap<>();
    private final Set<String> pendingProgressionBonusRefreshs = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingFreeCastInvulnerabilityClears = ConcurrentHashMap.newKeySet();
    private final Queue<PendingAbilityCast> pendingAbilityCasts = new ConcurrentLinkedQueue<>();
    private final Map<String, ActiveStyleTest> activeStyleTests = new ConcurrentHashMap<>();
    private final Set<String> freeCastPlayers = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> recentSpellbookSlotInputs = new ConcurrentHashMap<>();
    private final Map<String, Double> lastAppliedTargetHealthByPlayer = new ConcurrentHashMap<>();
    private final Map<String, Float> lastObservedFreeCastHealthByPlayer = new ConcurrentHashMap<>();
    private final Set<String> initializedRuntimePlayers = ConcurrentHashMap.newKeySet();
    private long lastDevCommandInboxPollAtMs = 0L;
    private long lastObservabilityHeartbeatAtMs = 0L;
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
        LOG.info("[MOTM] >>> start() called");
        registerHytaleHooks();
        registerObservabilityPacketWatchers();
        for (InteractionType value : InteractionType.values()) {
            LOG.info("[MOTM] InteractionType enum: "
                    + value.name()
                    + " / toString=" + value
                    + " / valueOf=" + String.valueOf(value));
        }
        boolean spellbookAssetResolved =
                com.hypixel.hytale.server.core.asset.type.item.config.Item.getAssetMap()
                        .getAsset(DEFAULT_SPELLBOOK_ITEM_ID) != null;
        LOG.info("[MOTM] Spellbook item asset resolved=" + spellbookAssetResolved
                + " itemId=" + DEFAULT_SPELLBOOK_ITEM_ID);
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
        Path operationalDataDir = resolveOperationalDataDirectory(dataDir);
        this.pluginDirectory = operationalDataDir;
        this.observability = new MotmObservability(operationalDataDir);
        loadServerConfig();

        LOG.info("========================================");
        LOG.info("  Mentees of the Mystical v1.0.0");
        LOG.info("  4 Classes | 40 Styles | 800 Perks | Level 1-200");
        LOG.info("  Build Channel: " + MotmBuildInfo.BUILD_CHANNEL);
        LOG.info("========================================");

        // Initialize data loader and load all JSON data
        dataLoader = new DataLoader(operationalDataDir);
        dataLoader.loadAll();

        // Initialize managers (order matters — dependencies)
        synergyEngine = new SynergyEngine(dataLoader);
        playerStatModifierManager = new PlayerStatModifierManager(this);
        perkManager = new PerkManager(dataLoader, playerStatModifierManager);
        levelingManager = new LevelingManager(dataLoader, perkManager);
        mobScalingManager = new MobScalingManager(dataLoader);
        playerDataManager = new PlayerDataManager(operationalDataDir, dataLoader);

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
        registerSpellbookInteractionCodecs();
        lastPreflightAudit = MotmPreflightAudit.run(this);
    }

    private Path resolveOperationalDataDirectory(Path hytaleDataDir) {
        if (hytaleDataDir == null) {
            return null;
        }

        hytaleDataDir = hytaleDataDir.toAbsolutePath().normalize();
        writeScannerSafeLegacyManifest(hytaleDataDir);

        Path parent = hytaleDataDir.getParent();
        if (parent == null
                || parent.getFileName() == null
                || !"mods".equalsIgnoreCase(parent.getFileName().toString())) {
            return hytaleDataDir;
        }

        Path saveRoot = parent.getParent();
        if (saveRoot == null || hytaleDataDir.getFileName() == null) {
            return hytaleDataDir;
        }

        Path operationalDataDir = saveRoot.resolve("motm-data").resolve(hytaleDataDir.getFileName().toString());
        migrateLegacyPluginDataDirectory(hytaleDataDir, operationalDataDir);
        LOG.info("[MOTM] Using operational data directory outside asset-scanned mods folder: "
                + operationalDataDir);
        return operationalDataDir;
    }

    private void migrateLegacyPluginDataDirectory(Path legacyDataDir, Path operationalDataDir) {
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
            LOG.info("[MOTM] Migrated legacy plugin data from asset-scanned mods folder: " + legacyDataDir);
        } catch (RuntimeException | IOException e) {
            LOG.warning("[MOTM] Failed to migrate legacy plugin data from " + legacyDataDir
                    + " to " + operationalDataDir + ": " + e.getMessage());
        }
    }

    private void writeScannerSafeLegacyManifest(Path hytaleDataDir) {
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
            LOG.warning("[MOTM] Failed to write scanner-safe manifest for legacy data directory "
                    + hytaleDataDir + ": " + e.getMessage());
        }
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
                boolean changed = false;
                if (!properties.containsKey("observability_packet_scope")) {
                    properties.setProperty("observability_packet_scope", "key");
                    changed = true;
                }
                if (changed) {
                    try (var writer = Files.newBufferedWriter(configPath)) {
                        properties.store(writer, "Mentees of the Mystical server settings");
                    }
                }
            } else {
                properties.setProperty("dev_tools_enabled", "false");
                properties.setProperty("observability_packet_scope", "key");
                properties.setProperty("notes", "Set dev_tools_enabled=true to enable /motm dev and the Dev Grimoire.");
                try (var writer = Files.newBufferedWriter(configPath)) {
                    properties.store(writer, "Mentees of the Mystical server settings");
                }
            }

            devToolsEnabled = Boolean.parseBoolean(properties.getProperty("dev_tools_enabled", "false"));
            if (observability != null) {
                observability.setPacketScope(properties.getProperty("observability_packet_scope", "key"));
            }
            LOG.info("[MOTM] Dev tools " + (isDevToolsEnabled() ? "enabled" : "disabled")
                    + " via " + configPath.getFileName());
            LOG.info("[MOTM] Observability packet scope="
                    + (observability != null ? observability.getPacketScope() : "unavailable"));
        } catch (IOException e) {
            devToolsEnabled = false;
            LOG.warning("[MOTM] Failed to load server config. Dev tools disabled. " + e.getMessage());
        }
    }

    private void registerHytaleHooks() {
        getEventRegistry().registerGlobal(PlayerConnectEvent.class, event -> onPlayerConnect(event.getPlayer()));
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
        getEntityStoreRegistry().registerSystem(new MotmDamageEventSystem(this));
    }

    private void registerObservabilityPacketWatchers() {
        if (!MotmBuildInfo.INTERNAL_TEST_BUILD || observability == null || observabilityInboundPacketFilter != null) {
            return;
        }

        try {
            observabilityInboundPacketFilter = PacketAdapters.registerInbound((PlayerPacketWatcher) (playerRef, packet) ->
                    observability.recordPacket("inbound", currentObservabilityTraceId(), playerRef, packet));
            observabilityOutboundPacketFilter = PacketAdapters.registerOutbound((PlayerPacketWatcher) (playerRef, packet) ->
                    observability.recordPacket("outbound", currentObservabilityTraceId(), playerRef, packet));
            LOG.info("[MOTM] Observability packet watchers registered: scope="
                    + observability.getPacketScope());
        } catch (Throwable e) {
            LOG.warning("[MOTM] Observability packet watcher registration failed: " + e.getMessage());
            observabilityInboundPacketFilter = null;
            observabilityOutboundPacketFilter = null;
        }
    }

    private void deregisterObservabilityPacketWatchers() {
        try {
            if (observabilityInboundPacketFilter != null) {
                PacketAdapters.deregisterInbound(observabilityInboundPacketFilter);
                observabilityInboundPacketFilter = null;
            }
            if (observabilityOutboundPacketFilter != null) {
                PacketAdapters.deregisterOutbound(observabilityOutboundPacketFilter);
                observabilityOutboundPacketFilter = null;
            }
        } catch (Throwable e) {
            LOG.warning("[MOTM] Observability packet watcher deregistration failed: " + e.getMessage());
        }
    }

    private void registerSpellbookInteractionCodecs() {
        MotmSpellbookInteraction.setMod(this);
        var interactionCodecRegistry = getCodecRegistry(
                com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.CODEC);
        interactionCodecRegistry.register(
                "motm_spellbook_primary",
                MotmSpellbookInteraction.Primary.class,
                MotmSpellbookInteraction.Primary.CODEC);
        interactionCodecRegistry.register(
                "motm_spellbook_secondary",
                MotmSpellbookInteraction.Secondary.class,
                MotmSpellbookInteraction.Secondary.CODEC);
        interactionCodecRegistry.register(
                "motm_spellbook_use",
                MotmSpellbookInteraction.Use.class,
                MotmSpellbookInteraction.Use.CODEC);
        LOG.info("[MOTM] Registered MOTM spellbook custom interactions: primary/secondary/use");
    }

    /**
     * Called when the plugin is disabled (server shutdown).
     */
    public void onDisable() {
        deregisterObservabilityPacketWatchers();
        if (playerDataManager != null) {
            playerDataManager.saveAll();
            playerDataManager.stopAutoSave();
        }
        if (observability != null && observability.isActive()) {
            observability.stopRun("plugin-disable");
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
        LOG.info("[MOTM] >>> onPlayerJoin: " + playerName + " id=" + playerId);
        var player = playerDataManager.onPlayerJoin(playerId, playerName);

        // Update rested bonus
        levelingManager.updateRestedOnLogin(player);

        // Reapply perks and synergies if class is set
        if (player.getPlayerClass() != null) {
            perkManager.reapplyAllPerks(player, synergyEngine);
            // Initialize legacy resource state for save compatibility; ability casting no longer spends it.
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

    public void onPlayerConnect(Player runtimePlayer) {
        LOG.info("[MOTM] >>> onPlayerConnect: " + runtimePlayer);
        var playerRef = getUniversePlayerRef(runtimePlayer);
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }
        String playerId = playerRef.getUuid().toString();
        long t0 = System.currentTimeMillis();

        onlineRuntimePlayers.put(playerId, runtimePlayer);
        recordCausality("player_connect", null, MotmObservability.mapOf(
                "playerId", playerId,
                "username", playerRef.getUsername(),
                "runtime", String.valueOf(runtimePlayer)
        ));
        if (initializedRuntimePlayers.add(playerId) || playerDataManager.getOnlinePlayer(playerId) == null) {
            onPlayerJoin(playerId, playerRef.getUsername());
        }

        var playerData = playerDataManager.getOnlinePlayer(playerId);
        boolean hasSavedLoadout = playerData != null
                && playerData.getPlayerClass() != null
                && playerData.getSelectedStyles() != null
                && !playerData.getSelectedStyles().isEmpty();
        LOG.info("[MOTM] onPlayerConnect hasSavedLoadout=" + hasSavedLoadout + " playerId=" + playerId);

        if (hasSavedLoadout) {
            rebuildPlayerRuntimeNow(playerData);
            boolean ensured = ensureSpellbookItem(runtimePlayer);
            LOG.info("[MOTM] onPlayerConnect ensureSpellbookItem=" + ensured
                    + " hasSpellbook=" + playerHasSpellbook(runtimePlayer));
            if (!ensured && !playerHasSpellbook(runtimePlayer)) {
                queueSpellbookGrant(playerId);
            }
            refreshPlayerProgressionBonuses(playerId);
        }

        LOG.info("[MOTM] onPlayerConnect done dt=" + (System.currentTimeMillis() - t0)
                + "ms playerId=" + playerId);
    }

    public void onPlayerReady(Player runtimePlayer) {
        LOG.info("[MOTM] >>> onPlayerReady: " + runtimePlayer);
        var playerRef = getUniversePlayerRef(runtimePlayer);
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }
        String playerId = playerRef.getUuid().toString();
        onlineRuntimePlayers.put(playerId, runtimePlayer);
        recordCausality("player_ready", null, MotmObservability.mapOf(
                "playerId", playerId,
                "username", playerRef.getUsername(),
                "world", runtimePlayer.getWorld() != null ? runtimePlayer.getWorld().getName() : "unknown"
        ));

        if (isDevToolsEnabled()) {
            statusEffectManager.clearEffects(playerId);
            elementalReactionManager.clearMarks(playerId);
            setFreeCastEnabled(playerId, true);
            if (!ensureSpellbookItem(runtimePlayer)) {
                queueSpellbookGrant(playerId);
            }
        }

        queueStatusHudInstall(playerId);
        LOG.info("[MOTM] onPlayerReady done playerId=" + playerId);
    }

    /**
     * Called when a player disconnects.
     */
    public void onPlayerDisconnect(String playerId) {
        recordCausality("player_disconnect", null, MotmObservability.mapOf(
                "playerId", playerId,
                "hadRuntimePlayer", onlineRuntimePlayers.containsKey(playerId)
        ));
        var player = playerDataManager.getOnlinePlayer(playerId);
        if (player != null) {
            levelingManager.updateRestedOnLogout(player);
        }
        playerDataManager.onPlayerDisconnect(playerId);
        styleManager.onPlayerDisconnect(playerId);
        resourceManager.onPlayerDisconnect(playerId);
        classPassiveManager.clearPlayerState(playerId);
        if (playerStatModifierManager != null) {
            playerStatModifierManager.clearForPlayer(playerId);
        } else {
            clearPerkTriggers(playerId);
        }
        statusEffectManager.clearEffects(playerId);
        elementalReactionManager.clearMarks(playerId);
        statusHuds.remove(playerId);
        onlineRuntimePlayers.remove(playerId);
        initializedRuntimePlayers.remove(playerId);
        pendingSpellbookGrants.remove(playerId);
        pendingDevBookGrants.remove(playerId);
        pendingStyleTestMobSpawns.remove(playerId);
        styleTestTargetsByPlayer.remove(playerId);
        pendingSingleAbilityTests.remove(playerId);
        pendingHydroContainerSyncs.remove(playerId);
        pendingRuntimeRebuilds.remove(playerId);
        pendingStatusHudRefreshs.remove(playerId);
        pendingStatusHudInstalls.remove(playerId);
        pendingProgressionBonusRefreshs.remove(playerId);
        pendingFreeCastInvulnerabilityClears.remove(playerId);
        pendingAbilityCasts.removeIf(request -> playerId.equals(request.playerId()));
        gameplayPlaybackManager.clearArmedStomp(playerId);
        activeStyleTests.remove(playerId);
        lastAppliedTargetHealthByPlayer.remove(playerId);
        lastObservedFreeCastHealthByPlayer.remove(playerId);
        setFreeCastEnabled(playerId, false);
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
        for (PerkTriggerBinding trigger : getPerkTriggers(playerId, "on_kill")) {
            LOG.info("[MOTM] perk on_kill trigger: perk=" + trigger.perkId()
                    + " value=" + trigger.value());
            if (runtimePlayer != null) {
                applyHealFraction(runtimePlayer, trigger.value());
            }
        }
        playerDataManager.checkAchievements(player, "mob_killed", null);
        refreshPlayerProgressionBonuses(playerId);
        refreshStatusHud(playerId);
    }

    public void registerPerkTrigger(String playerId, Perk perk, Perk.Effect effect) {
        if (playerId == null || perk == null || effect == null || effect.getType() == null) {
            return;
        }

        double triggerValue = effect.getValue();
        if (triggerValue == 0.0) {
            triggerValue = effect.getHeal();
        }
        if (triggerValue == 0.0) {
            triggerValue = effect.getHealAmount();
        }

        PerkTriggerBinding binding = new PerkTriggerBinding(perk.getId(), effect.getType(), triggerValue);
        perkTriggersByPlayer.computeIfAbsent(playerId, ignored -> new ArrayList<>()).add(binding);
        LOG.info("[MOTM] perk trigger registered: player=" + playerId
                + " perk=" + binding.perkId()
                + " type=" + binding.type()
                + " value=" + binding.value());
    }

    public void clearPerkTriggers(String playerId) {
        if (playerId != null) {
            perkTriggersByPlayer.remove(playerId);
        }
    }

    public List<PerkTriggerBinding> getPerkTriggers(String playerId, String type) {
        if (playerId == null || type == null) {
            return List.of();
        }
        List<PerkTriggerBinding> bindings = perkTriggersByPlayer.get(playerId);
        if (bindings == null || bindings.isEmpty()) {
            return List.of();
        }
        String normalizedType = type.trim().toLowerCase(Locale.ROOT);
        return bindings.stream()
                .filter(binding -> binding.type() != null
                        && binding.type().trim().toLowerCase(Locale.ROOT).equals(normalizedType))
                .toList();
    }

    private void applyHealFraction(Player runtimePlayer, double fraction) {
        if (runtimePlayer == null || fraction <= 0.0) {
            return;
        }

        try {
            Ref<EntityStore> playerRef = runtimePlayer.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                return;
            }
            EntityStatMap statMap = playerRef.getStore().getComponent(playerRef, EntityStatMap.getComponentType());
            if (statMap == null) {
                return;
            }

            float amount = (float) Math.max(1.0, 100.0 * fraction);
            statMap.addStatValue(DefaultEntityStatTypes.getHealth(), amount);
            LOG.info("[MOTM] perk heal applied: fraction=" + fraction + " amount=" + amount);
        } catch (RuntimeException ex) {
            LOG.warning("[MOTM] Failed to apply perk heal: " + ex.getMessage());
        }
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
        gameplayPlaybackManager.clearArmedStomp(playerId);
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
        processPendingFreeCastInvulnerabilityClears(currentStore);
        processPendingRuntimeRebuilds(currentStore);
        processFreeCastTestSafety(currentStore);
        classPassiveManager.tick(onlineRuntimePlayers, currentStore);
        processActiveStyleTests(currentStore);
        processPendingSingleAbilityTests(currentStore);
        processPendingDevRelocations(currentStore);
        processPendingDaylightRequests(currentStore);
        processPendingDevGameModeChanges(currentStore);
        processDevCommandInbox(currentStore);
        processPendingStyleReviewResets(currentStore);
        processPendingProofRequests(currentStore);
        processActiveProofCleanups(currentStore);
        processPendingStyleTestMobClears(currentStore);
        processPendingStyleTestMobSpawns(currentStore);
        processPendingStyleTestMobCounts(currentStore);
        processPendingAbilityCasts(currentStore);
        gameplayPlaybackManager.tickArmedStomps(currentStore);
        gameplayPlaybackManager.tick(currentStore);
        processPendingTerraReviewKitGrants(currentStore);
        processPendingTerraReviewInventoryCleans(currentStore);
        processPendingInventoryGrants(currentStore);
        processPendingProgressionBonusRefreshs(currentStore);
        processPendingStatusHudInstalls(currentStore);
        processPendingStatusHudRefreshs(currentStore);
        hudRefreshTickCounter++;
        if (hudRefreshTickCounter >= HUD_REFRESH_INTERVAL_TICKS) {
            hudRefreshTickCounter = 0;
            refreshAllStatusHuds(currentStore);
        }
        recordObservabilityHeartbeat(currentStore);

        dotDamageByEntity.forEach((entityId, dotPercent) ->
                LOG.fine("[MOTM] TODO: Apply " + (dotPercent * 100)
                        + "% max HP DoT to entity " + entityId + " via Hytale's damage API."));
    }

    private void processDevCommandInbox(Store<EntityStore> currentStore) {
        if (!devToolsEnabled || pluginDirectory == null || motmCommand == null) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastDevCommandInboxPollAtMs < DEV_COMMAND_INBOX_POLL_INTERVAL_MS) {
            return;
        }
        lastDevCommandInboxPollAtMs = now;

        Path inbox = pluginDirectory.resolve(DEV_COMMAND_INBOX_FILE_NAME);
        if (!Files.exists(inbox)) {
            return;
        }

        Player runtimePlayer = findFirstOnlineRuntimePlayer(currentStore);
        if (runtimePlayer == null) {
            return;
        }

        List<String> lines;
        try {
            lines = Files.readAllLines(inbox, StandardCharsets.UTF_8);
            Files.deleteIfExists(inbox);
        } catch (IOException e) {
            LOG.warning("[MOTM] Dev command inbox read failed: " + e.getMessage());
            return;
        }

        for (String rawLine : lines) {
            String command = normalizeDevInboxCommand(rawLine);
            if (command.isBlank()) {
                continue;
            }

            String traceId = observability != null
                    ? observability.nextTraceId("cmd")
                    : "cmd-" + Long.toUnsignedString(System.currentTimeMillis(), 36);
            recordControl("dev_command_received", traceId, MotmObservability.mapOf(
                    "command", "/motm " + command,
                    "rawLine", rawLine
            ));
            String previousTraceId = enterObservabilityTrace(traceId);
            try {
                String result = motmCommand.execute(runtimePlayer, command.split("\\s+"));
                String safeResult = result == null ? "" : result.replace('\n', ' ');
                String out = "[MOTM] Dev command inbox executed: command=/motm " + command
                        + " traceId=" + traceId
                        + " result=" + safeResult;
                LOG.info(out);
                recordControl("dev_command_executed", traceId, MotmObservability.mapOf(
                        "command", "/motm " + command,
                        "result", safeResult
                ));
                appendDevCommandOutbox(out);
            } catch (Throwable t) {
                String out = "[MOTM] Dev command inbox failed: command=/motm " + command
                        + " traceId=" + traceId
                        + " error=" + t.getClass().getSimpleName() + ": " + t.getMessage();
                LOG.severe(out);
                recordControl("dev_command_failed", traceId, MotmObservability.mapOf(
                        "command", "/motm " + command,
                        "errorType", t.getClass().getSimpleName(),
                    "error", t.getMessage()
                ));
                appendDevCommandOutbox(out);
            } finally {
                restoreObservabilityTrace(previousTraceId);
            }
        }
    }

    private void recordObservabilityHeartbeat(Store<EntityStore> currentStore) {
        if (observability == null || !observability.isActive()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (now - lastObservabilityHeartbeatAtMs < 1000L) {
            return;
        }
        lastObservabilityHeartbeatAtMs = now;

        String worldName = "unknown";
        if (currentStore != null && currentStore.getExternalData() != null
                && currentStore.getExternalData().getWorld() != null) {
            worldName = currentStore.getExternalData().getWorld().getName();
        }

        recordCausality("server_tick_heartbeat", null, MotmObservability.mapOf(
                "world", worldName,
                "onlinePlayers", onlineRuntimePlayers.size(),
                "pendingAbilityCasts", pendingAbilityCasts.size(),
                "pendingProofRequests", pendingProofRequests.size(),
                "pendingStyleTestMobSpawns", pendingStyleTestMobSpawns.size(),
                "pendingStyleTestMobClears", pendingStyleTestMobClears.size(),
                "activeProofSelections", activeProofSelections.size(),
                "activeProofProxies", activeProofProxies.size(),
                "activeStyleTests", activeStyleTests.size(),
                "trackedStyleTargetOwners", styleTestTargetsByPlayer.size()
        ));
    }

    private Player findFirstOnlineRuntimePlayer(Store<EntityStore> currentStore) {
        for (Player player : onlineRuntimePlayers.values()) {
            if (isPlayerInStore(player, currentStore)) {
                return player;
            }
        }
        return null;
    }

    private String normalizeDevInboxCommand(String rawLine) {
        if (rawLine == null) {
            return "";
        }
        String command = rawLine.replace("\uFEFF", "").trim();
        if (command.startsWith("/")) {
            command = command.substring(1).trim();
        }
        if (command.regionMatches(true, 0, "motm", 0, 4)) {
            command = command.substring(4).trim();
        }
        return command;
    }

    private void appendDevCommandOutbox(String line) {
        try {
            Files.createDirectories(pluginDirectory);
            Files.writeString(
                    pluginDirectory.resolve(DEV_COMMAND_OUTBOX_FILE_NAME),
                    line + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            LOG.warning("[MOTM] Dev command outbox write failed: " + e.getMessage());
        }
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

        recordClientIntent("custom_page_open", null, MotmObservability.mapOf(
                "playerId", playerRef.getUuid() != null ? playerRef.getUuid().toString() : null,
                "username", playerRef.getUsername(),
                "page", "MOTM_Spellbook",
                "section", String.valueOf(section)
        ));
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
        LOG.info("[MOTM] Granted spellbook item: " + DEFAULT_SPELLBOOK_ITEM_ID);
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
        LOG.info("[MOTM] Queue ability cast: playerId=" + playerId
                + " abilityId=" + abilityId
                + " notifyFailures=" + notifyFailures);
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

    public String startSingleAbilityTest(String playerId, String abilityId) {
        if (!devToolsEnabled) {
            return devToolsDisabledMessage();
        }
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        Player runtimePlayer = onlineRuntimePlayers.get(playerId);
        var playerData = playerDataManager.getOnlinePlayer(playerId);
        if (runtimePlayer == null || playerData == null) {
            return "[MOTM] Join a world and run this in-game to start a live ability test.";
        }

        StyleData style = null;
        if (playerData.getPlayerClass() != null) {
            for (String selectedStyleId : playerData.getSelectedStyles()) {
                style = dataLoader.getStyleById(selectedStyleId, playerData.getPlayerClass());
                if (style != null) {
                    break;
                }
            }
        }
        if (style == null) {
            return "[MOTM] Choose a style before running /motm dev test ability <abilityId>.";
        }

        AbilityData ability = styleManager.findAbility(playerData, abilityId);
        if (ability == null) {
            return "[MOTM] Unknown ability '" + abilityId + "' for current style.";
        }

        setFreeCastEnabled(playerId, true);
        pendingSingleAbilityTests.put(playerId, ability.getId());

        return "[MOTM] Live ability test queued: " + ability.getName()
                + ". Free-cast ON. The mod will target the nearest test NPC.";
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

    public String spawnStyleTestMobs(String playerId) {
        return spawnStyleTestMobs(playerId, "standard");
    }

    public String clearStyleTestMobs(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }
        boolean added = pendingStyleTestMobClears.add(playerId);
        LOG.info("[MOTM] Style test mob clear queued: playerId=" + playerId + " added=" + added);
        return added
                ? "[MOTM] Style test mob clear queued."
                : "[MOTM] Style test mob clear is already queued.";
    }

    public String resetStyleReviewArena(String playerId) {
        if (!devToolsEnabled) {
            return devToolsDisabledMessage();
        }
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }
        Player runtimePlayer = onlineRuntimePlayers.get(playerId);
        if (runtimePlayer == null) {
            return "[MOTM] Join a world and run this in-game to reset the style review arena.";
        }
        boolean added = pendingStyleReviewResets.add(playerId);
        LOG.info("[MOTM] Style review arena reset queued: playerId=" + playerId + " added=" + added);
        return added
                ? "[MOTM] Style review arena reset queued."
                : "[MOTM] Style review arena reset is already queued.";
    }

    public String countStyleTestMobs(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }
        boolean added = pendingStyleTestMobCounts.add(playerId);
        LOG.info("[MOTM] Style test mob count queued: playerId=" + playerId + " added=" + added);
        return added
                ? "[MOTM] Style test mob count queued."
                : "[MOTM] Style test mob count is already queued.";
    }

    public String spawnStyleTestMobs(String playerId, boolean closeGroundedTarget) {
        return spawnStyleTestMobs(playerId, closeGroundedTarget ? "close" : "standard");
    }

    public String spawnStyleTestMobs(String playerId, String mode) {
        if (!devToolsEnabled) {
            return devToolsDisabledMessage();
        }
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        Player runtimePlayer = onlineRuntimePlayers.get(playerId);
        if (runtimePlayer == null) {
            return "[MOTM] Join a world and run this in-game to spawn style-test mobs.";
        }

        String normalizedMode = normalizeStyleTestMobMode(mode);
        boolean added = pendingStyleTestMobSpawns.put(playerId, normalizedMode) == null;
        LOG.info("[MOTM] Style test mob spawn queued: playerId=" + playerId + " added=" + added);
        return added
                ? "[MOTM] Style test mob spawn queued mode=" + normalizedMode + "."
                : "[MOTM] Style test mob spawn is already queued.";
    }

    public String queueDevProof(String playerId, String proofId) {
        if (!devToolsEnabled) {
            return devToolsDisabledMessage();
        }
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }
        Player runtimePlayer = onlineRuntimePlayers.get(playerId);
        if (runtimePlayer == null) {
            return "[MOTM] Join a world and run this in-game to run a proof.";
        }
        String normalizedProofId = proofId == null ? "" : proofId.trim().toLowerCase(Locale.ROOT);
        if (normalizedProofId.isBlank()) {
            return "[MOTM] Usage: /motm dev proof <coating-metal|tempblock-metal-wall|tempblock-gem-cluster|tempfluid-lava-ring|proxy-magma-blob|movement-burrow|...>";
        }
        boolean added = pendingProofRequests.put(playerId, normalizedProofId) == null;
        LOG.info("[MOTM] Proof request queued: playerId=" + playerId
                + " proofId=" + normalizedProofId
                + " added=" + added);
        return added
                ? "[MOTM] Proof queued: " + normalizedProofId + "."
                : "[MOTM] A proof request is already queued.";
    }

    private String normalizeStyleTestMobMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "close", "stationary", "standard", "cluster", "line", "surround" -> normalized;
            default -> "standard";
        };
    }

    private String spawnStyleTestMobsNow(String playerId, Player runtimePlayer, String mode) {
        if (runtimePlayer == null) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        Vector3d basePosition = getPlayerPosition(runtimePlayer);
        Vector3d forward = getPlayerForward(runtimePlayer);
        if (basePosition == null || forward == null) {
            return "[MOTM] Could not resolve player position/direction for style-test mobs.";
        }
        if (basePosition.y < -16.0) {
            String summary = "[MOTM] Style test mob spawn blocked: player appears below world at "
                    + formatVector(basePosition)
                    + ". Respawn before running setup.";
            LOG.warning(summary);
            return summary;
        }

        Vector3d horizontalForward = normalizeHorizontal(forward);
        Vector3d right = new Vector3d(-horizontalForward.z, 0.0, horizontalForward.x);
        String normalizedMode = normalizeStyleTestMobMode(mode);
        boolean closeGroundedTarget = "close".equals(normalizedMode) || "stationary".equals(normalizedMode);
        Vector3d groundPosition = closeGroundedTarget
                ? basePosition.clone().addScaled(horizontalForward, 1.6)
                : basePosition.clone()
                        .addScaled(horizontalForward, 5.0)
                        .addScaled(right, -8.0);
        Vector3d floatingPosition = basePosition.clone()
                .addScaled(horizontalForward, 5.0)
                .addScaled(right, -5.0);
        floatingPosition.y += 3.0;

        World world = runtimePlayer.getWorld();
        if (world == null) {
            return "[MOTM] Runtime world is unavailable for style-test mobs.";
        }

        int cleared = clearTrackedStyleTestTargets(playerId);
        List<Ref<EntityStore>> targets = new ArrayList<>();
        if ("cluster".equals(normalizedMode)) {
            addStyleTestNpc(targets, world, basePosition.clone().addScaled(horizontalForward, 4.0), "Test_Dummy_Stationary");
            addStyleTestNpc(targets, world, basePosition.clone().addScaled(horizontalForward, 4.0).addScaled(right, 3.0), "Test_Dummy_Stationary");
            addStyleTestNpc(targets, world, basePosition.clone().addScaled(horizontalForward, 4.0).addScaled(right, -3.0), "Test_Dummy_Stationary");
            addStyleTestNpc(targets, world, basePosition.clone().addScaled(horizontalForward, 7.0), "Test_Dummy_Stationary");
            addStyleTestNpc(targets, world, basePosition.clone().addScaled(horizontalForward, 2.0), "Test_Dummy_Stationary");
        } else if ("line".equals(normalizedMode)) {
            addStyleTestNpc(targets, world, basePosition.clone().addScaled(horizontalForward, 4.0), "Test_Dummy_Stationary");
            addStyleTestNpc(targets, world, basePosition.clone().addScaled(horizontalForward, 8.0), "Test_Dummy_Stationary");
            addStyleTestNpc(targets, world, basePosition.clone().addScaled(horizontalForward, 12.0), "Test_Dummy_Stationary");
        } else if ("surround".equals(normalizedMode)) {
            addStyleTestNpc(targets, world, basePosition.clone().addScaled(horizontalForward, 3.0), "Test_Dummy_Stationary");
            addStyleTestNpc(targets, world, basePosition.clone().addScaled(horizontalForward, -3.0), "Test_Dummy_Stationary");
            addStyleTestNpc(targets, world, basePosition.clone().addScaled(right, 3.0), "Test_Dummy_Stationary");
            addStyleTestNpc(targets, world, basePosition.clone().addScaled(right, -3.0), "Test_Dummy_Stationary");
        } else {
            addStyleTestNpc(targets, world, groundPosition, "Test_Dummy_Stationary");
            if (!"stationary".equals(normalizedMode)) {
                addStyleTestNpc(targets, world, floatingPosition, "Bat");
            }
        }
        styleTestTargetsByPlayer.put(playerId, targets);
        int spawned = targets.size();

        String summary = "[MOTM] Style test mobs spawned: count=" + spawned
                + " mode=" + normalizedMode
                + " clearedPrevious=" + cleared
                + " tracked=" + countValidRefs(targets)
                + " grounded=" + formatVector(groundPosition)
                + " floating=" + formatVector(floatingPosition);
        LOG.info(summary);
        return summary;
    }

    private void addStyleTestNpc(List<Ref<EntityStore>> targets, World world, Vector3d position, String roleName) {
        Ref<EntityStore> ref = spawnStyleTestNpc(world, position, roleName);
        if (ref != null) {
            targets.add(ref);
        }
    }

    private String clearStyleTestMobsNow(String playerId, Store<EntityStore> currentStore, Player player) {
        int cleared = clearTrackedStyleTestTargets(playerId);
        int staleCleared = clearNearbyStyleTestTargets(currentStore, player);
        String summary = "[MOTM] Style test mobs cleared: count=" + (cleared + staleCleared)
                + " tracked=" + cleared
                + " staleNearby=" + staleCleared;
        LOG.info(summary + " playerId=" + playerId);
        return summary;
    }

    private String countStyleTestMobsNow(String playerId, Store<EntityStore> currentStore, Player player) {
        int count = countTrackedStyleTestTargets(playerId);
        int nearby = countNearbyStyleTestTargets(currentStore, player);
        String summary = "[MOTM] Style test mobs tracked: count=" + count
                + " nearbyCleanupRoles=" + nearby;
        LOG.info(summary + " playerId=" + playerId);
        return summary;
    }

    private String scrubStyleReviewArena(Player player) {
        World world = player != null ? player.getWorld() : null;
        Vector3d center = player != null ? getPlayerPosition(player) : null;
        if (world == null || center == null) {
            return "skipped missing world/player";
        }

        int grassBlockTypeId = BlockType.getBlockIdOrUnknown("Soil_Grass", "MOTM style review arena scrub");
        if (grassBlockTypeId == BlockType.UNKNOWN_ID || grassBlockTypeId == BlockType.EMPTY_ID) {
            grassBlockTypeId = BlockType.getBlockIdOrUnknown("Rock_Stone_Brick_Pillar_Middle", "MOTM style review arena scrub");
        }
        if (grassBlockTypeId == BlockType.UNKNOWN_ID || grassBlockTypeId == BlockType.EMPTY_ID) {
            return "skipped no floor block";
        }

        int floorY = (int) Math.floor(center.y) - 1;
        int centerX = (int) Math.floor(center.x);
        int centerZ = (int) Math.floor(center.z);
        int radius = 28;
        BlockSelection scrub = new BlockSelection();
        scrub.setPosition(centerX, floorY, centerZ);
        scrub.setAnchorAtWorldPos(centerX, floorY, centerZ);
        int floorBlocks = 0;
        int clearedBlocks = 0;
        int clearedFluids = 0;

        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                int wx = centerX + x;
                int wz = centerZ + z;
                scrub.addBlockAtWorldPos(wx, floorY, wz, grassBlockTypeId, 0, 0, 0);
                floorBlocks++;
                for (int y = floorY + 1; y <= floorY + 5; y++) {
                    scrub.addBlockAtWorldPos(wx, y, wz, BlockType.EMPTY_ID, 0, 0, 0);
                    scrub.addFluidAtWorldPos(wx, y, wz, Fluid.EMPTY_ID, (byte) 0);
                    clearedBlocks++;
                    clearedFluids++;
                }
            }
        }

        try {
            scrub.place(null, world, Vector3i.ZERO, BlockMask.EMPTY);
            String summary = "scrubbed center=(" + centerX + "," + floorY + "," + centerZ + ")"
                    + " radius=" + radius
                    + " floorBlocks=" + floorBlocks
                    + " clearedBlocks=" + clearedBlocks
                    + " clearedFluids=" + clearedFluids;
            LOG.info("[MOTM] Style review arena scrub: " + summary);
            return summary;
        } catch (Throwable e) {
            LOG.log(java.util.logging.Level.WARNING, "[MOTM] Style review arena scrub failed safely.", e);
            return "failed " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    public String runStyleTestWeaponHit(String playerId) {
        if (!devToolsEnabled) {
            return devToolsDisabledMessage();
        }
        Player runtimePlayer = getRuntimePlayer(playerId);
        var playerData = playerDataManager.getOnlinePlayer(playerId);
        if (runtimePlayer == null || playerData == null) {
            return "[MOTM] Join a world and run this in-game to test a weapon follow-up.";
        }
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return "[MOTM] Style test weapon hit failed: player store missing.";
        }

        Ref<EntityStore> target = findNearestStyleTestNpc(playerRef.getStore(), runtimePlayer, 8.0);
        if (target == null) {
            String summary = "[MOTM] Style test weapon hit failed: no style-test target within 8m.";
            LOG.warning(summary + " playerId=" + playerId);
            return summary;
        }

        String response = gameplayPlaybackManager.handleWeaponFollowUpHit(
                runtimePlayer,
                playerData,
                target,
                "Weapon_Sword_Iron"
        );
        if (response == null || response.isBlank()) {
            Damage simulatedNativeHit = new Damage(
                    new Damage.EntitySource(playerRef),
                    DamageCause.PHYSICAL,
                    10.0f
            );
            response = gameplayPlaybackManager.handleNativeWeaponDamage(
                    runtimePlayer,
                    playerData,
                    target,
                    "Weapon_Sword_Iron",
                    simulatedNativeHit
            );
        }
        if (response == null || response.isBlank()) {
            String summary = "[MOTM] Style test weapon hit: no follow-up/passive applied.";
            LOG.info(summary + " playerId=" + playerId);
            return summary;
        }

        LOG.info(response + " playerId=" + playerId);
        return response;
    }

    public String forceStyleTestStompLanding(String playerId) {
        if (!devToolsEnabled) {
            return devToolsDisabledMessage();
        }
        Player runtimePlayer = getRuntimePlayer(playerId);
        if (runtimePlayer == null) {
            return "[MOTM] Join a world and run this in-game to force a Stomp landing.";
        }

        String response = gameplayPlaybackManager.forceArmedStompLanding(playerId, runtimePlayer);
        LOG.info(response + " playerId=" + playerId);
        return response;
    }

    private int clearNearbyStyleTestTargets(Store<EntityStore> currentStore, Player player) {
        return visitNearbyStyleTestTargets(currentStore, player, true);
    }

    private int countNearbyStyleTestTargets(Store<EntityStore> currentStore, Player player) {
        return visitNearbyStyleTestTargets(currentStore, player, false);
    }

    private int visitNearbyStyleTestTargets(Store<EntityStore> currentStore, Player player, boolean despawn) {
        if (currentStore == null || player == null) {
            return 0;
        }

        Vector3d playerPosition = getPlayerPosition(player);
        if (playerPosition == null) {
            return 0;
        }

        int[] visited = {0};
        currentStore.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning() || !isStyleTestCleanupRole(npc)) {
                    continue;
                }

                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                Vector3d position = getEntityPosition(currentStore, ref);
                if (position == null || distance(playerPosition, position) > 28.0) {
                    continue;
                }

                if (despawn) {
                    npc.setToDespawn();
                }
                visited[0]++;
            }
        });
        return visited[0];
    }

    private boolean isStyleTestCleanupRole(NPCEntity npc) {
        if (npc == null) {
            return false;
        }
        return STYLE_TEST_CLEANUP_ROLES.contains(npc.getRoleName())
                || STYLE_TEST_CLEANUP_ROLES.contains(npc.getNPCTypeId());
    }

    private int clearTrackedStyleTestTargets(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return 0;
        }
        List<Ref<EntityStore>> targets = styleTestTargetsByPlayer.remove(playerId);
        if (targets == null || targets.isEmpty()) {
            return 0;
        }

        int cleared = 0;
        for (Ref<EntityStore> target : targets) {
            if (target == null || !target.isValid()) {
                continue;
            }
            Store<EntityStore> store = target.getStore();
            NPCEntity npc = store != null ? store.getComponent(target, NPCEntity.getComponentType()) : null;
            if (npc != null && !npc.isDespawning()) {
                npc.setToDespawn();
                cleared++;
            }
        }
        return cleared;
    }

    private int countTrackedStyleTestTargets(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return 0;
        }
        return countValidRefs(styleTestTargetsByPlayer.get(playerId));
    }

    private int countValidRefs(List<Ref<EntityStore>> refs) {
        if (refs == null || refs.isEmpty()) {
            return 0;
        }
        int count = 0;
        for (Ref<EntityStore> ref : refs) {
            if (ref == null || !ref.isValid()) {
                continue;
            }
            Store<EntityStore> store = ref.getStore();
            NPCEntity npc = store != null ? store.getComponent(ref, NPCEntity.getComponentType()) : null;
            if (npc != null && !npc.isDespawning() && store.getComponent(ref, DeathComponent.getComponentType()) == null) {
                count++;
            }
        }
        return count;
    }

    private Ref<EntityStore> spawnStyleTestNpc(World world, Vector3d position, String roleName) {
        try {
            NPCEntity npc = new NPCEntity(world);
            npc.setRoleName(roleName);
            npc.setDespawnTime(240.0f);
            world.spawnEntity(npc, position.clone(), new Vector3f(0f, 0f, 0f));

            Ref<EntityStore> ref = npc.getReference();
            if (ref == null || !ref.isValid() || ref.getStore() == null) {
                LOG.warning("[MOTM] Style test NPC spawned without a valid entity reference: role=" + roleName);
                return null;
            }
            return ref;
        } catch (Exception e) {
            LOG.warning("[MOTM] Failed to spawn style test NPC role=" + roleName
                    + " at " + formatVector(position) + ": " + e.getMessage());
            return null;
        }
    }

    private void installStatusHud(Player player) {
        if (!CUSTOM_HUD_ENABLED || player == null) {
            return;
        }

        var playerRef = getUniversePlayerRef(player);
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }

        String playerId = playerRef.getUuid().toString();
        if (statusHuds.containsKey(playerId)) {
            return;
        }

        MotmStatusHud hud = new MotmStatusHud(playerRef, this);
        statusHuds.put(playerId, hud);
        String traceId = currentOrNewClientIntentTraceId();
        String previousTraceId = enterObservabilityTrace(traceId);
        try {
            player.getHudManager().setCustomHud(playerRef, hud);
            recordClientIntent("custom_hud_set", traceId, MotmObservability.mapOf(
                    "playerId", playerId,
                    "username", playerRef.getUsername(),
                    "hud", "MOTM_StatusHud"
            ));
            try {
                // Keep the native hotbar, but let the MOTM HUD own the right-side spell lane.
                player.getHudManager().hideHudComponents(
                        playerRef,
                        HudComponent.StatusIcons,
                        HudComponent.InputBindings,
                        HudComponent.AmmoIndicator,
                        HudComponent.UtilitySlotSelector);
                recordClientIntent("native_hud_components_hidden", traceId, MotmObservability.mapOf(
                        "playerId", playerId,
                        "components", List.of(
                                String.valueOf(HudComponent.StatusIcons),
                                String.valueOf(HudComponent.InputBindings),
                                String.valueOf(HudComponent.AmmoIndicator),
                                String.valueOf(HudComponent.UtilitySlotSelector)
                        )
                ));
            } catch (Exception e) {
                LOG.warning("[MOTM] Failed to hide native HUD components: " + e.getMessage());
            }
        } finally {
            restoreObservabilityTrace(previousTraceId);
        }
    }

    private void queueStatusHudInstall(String playerId) {
        if (!CUSTOM_HUD_ENABLED || playerId == null || playerId.isBlank()) {
            return;
        }
        LOG.info("[MOTM] Queue HUD install: playerId=" + playerId
                + " delayTicks=" + HUD_INSTALL_DELAY_TICKS);
        pendingStatusHudInstalls.put(playerId, HUD_INSTALL_DELAY_TICKS);
    }

    private void processPendingInventoryGrants(Store<EntityStore> currentStore) {
        processPendingSpellbookGrants(currentStore);
        processPendingDevBookGrants(currentStore);
        processPendingHydroContainerSyncs(currentStore);
    }

    private void processPendingStatusHudInstalls(Store<EntityStore> currentStore) {
        for (Map.Entry<String, Integer> entry : Map.copyOf(pendingStatusHudInstalls).entrySet()) {
            String playerId = entry.getKey();
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingStatusHudInstalls.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            int ticksRemaining = entry.getValue() - 1;
            if (ticksRemaining > 0) {
                pendingStatusHudInstalls.put(playerId, ticksRemaining);
                continue;
            }

            LOG.info("[MOTM] Installing HUD: playerId=" + playerId);
            installStatusHud(player);
            pendingStatusHudInstalls.remove(playerId);
            refreshStatusHud(playerId);
        }
    }

    private void processFreeCastTestSafety(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(freeCastPlayers)) {
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            try {
                var playerRef = player.getReference();
                if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                    continue;
                }

                EntityStatMap entityStatMap = playerRef.getStore().getComponent(playerRef, EntityStatMap.getComponentType());
                if (entityStatMap == null) {
                    continue;
                }

                EntityStatValue healthBeforeSafety = entityStatMap.get(DefaultEntityStatTypes.getHealth());
                if (healthBeforeSafety != null) {
                    float currentHealth = healthBeforeSafety.get();
                    Float previousHealth = lastObservedFreeCastHealthByPlayer.put(playerId, currentHealth);
                    if (previousHealth != null && currentHealth < previousHealth - 0.5f) {
                        var playerData = playerDataManager.getOnlinePlayer(playerId);
                        LOG.info("[MOTM] Free-cast health drop detected: player="
                                + (playerData != null ? playerData.getPlayerName() : playerId)
                                + " class=" + (playerData != null ? playerData.getPlayerClass() : "unknown")
                                + " styles=" + (playerData != null ? playerData.getSelectedStyles() : List.of())
                                + " from=" + previousHealth
                                + " to=" + currentHealth
                                + " burn=" + statusEffectManager.hasEffect(playerId, com.motm.model.StatusEffect.Type.BURN)
                                + " dot=" + statusEffectManager.hasEffect(playerId, com.motm.model.StatusEffect.Type.DOT));
                    }
                }

                entityStatMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
                maximizeStatIfPresent(entityStatMap, DefaultEntityStatTypes.getStamina());
                maximizeStatIfPresent(entityStatMap, DefaultEntityStatTypes.getMana());
                maximizeStatIfPresent(entityStatMap, DefaultEntityStatTypes.getSignatureEnergy());
                statusEffectManager.removeEffect(playerId, com.motm.model.StatusEffect.Type.BURN);
                statusEffectManager.removeEffect(playerId, com.motm.model.StatusEffect.Type.DOT);
                applyFreeCastMovementNormalization(player);
                ensureFreeCastInvulnerability(player);
            } catch (IllegalStateException e) {
                LOG.fine("[MOTM] Skipped free-cast safety tick on the wrong store for " + playerId + ": " + e.getMessage());
            }
        }
    }

    private void applyFreeCastMovementNormalization(Player player) {
        if (player == null) {
            return;
        }
        try {
            var playerRef = player.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                return;
            }
            MovementManager movementManager = playerRef.getStore().getComponent(playerRef, MovementManager.getComponentType());
            if (movementManager == null || movementManager.getSettings() == null) {
                return;
            }
            var settings = movementManager.getSettings();
            settings.baseSpeed = Math.max(settings.baseSpeed, 6.25f);
            settings.forwardWalkSpeedMultiplier = Math.max(settings.forwardWalkSpeedMultiplier, 1.10f);
            settings.backwardWalkSpeedMultiplier = Math.max(settings.backwardWalkSpeedMultiplier, 1.05f);
            settings.strafeWalkSpeedMultiplier = Math.max(settings.strafeWalkSpeedMultiplier, 1.10f);
            settings.forwardRunSpeedMultiplier = Math.max(settings.forwardRunSpeedMultiplier, 1.25f);
            settings.backwardRunSpeedMultiplier = Math.max(settings.backwardRunSpeedMultiplier, 1.10f);
            settings.strafeRunSpeedMultiplier = Math.max(settings.strafeRunSpeedMultiplier, 1.25f);
            settings.forwardSprintSpeedMultiplier = Math.max(settings.forwardSprintSpeedMultiplier, 1.45f);
            settings.acceleration = Math.max(settings.acceleration, 0.16f);
            PlayerRef universePlayerRef = playerRef.getStore().getComponent(playerRef, PlayerRef.getComponentType());
            if (universePlayerRef != null && universePlayerRef.getPacketHandler() != null) {
                movementManager.update(universePlayerRef.getPacketHandler());
            }
        } catch (IllegalStateException e) {
            LOG.fine("[MOTM] Skipped free-cast movement normalization on the wrong store: " + e.getMessage());
        } catch (Exception e) {
            LOG.warning("[MOTM] Failed to normalize free-cast movement: " + e.getMessage());
        }
    }

    private void maximizeStatIfPresent(EntityStatMap entityStatMap, int statType) {
        if (entityStatMap == null) {
            return;
        }
        EntityStatValue stat = entityStatMap.get(statType);
        if (stat == null || stat.getMax() <= 0.0f) {
            return;
        }
        entityStatMap.maximizeStatValue(statType);
    }

    private void processPendingFreeCastInvulnerabilityClears(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(pendingFreeCastInvulnerabilityClears)) {
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingFreeCastInvulnerabilityClears.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            clearFreeCastInvulnerability(playerId);
            pendingFreeCastInvulnerabilityClears.remove(playerId);
        }
    }

    private void ensureFreeCastInvulnerability(Player player) {
        if (!setRuntimeInvulnerability(player, true) && player != null) {
            player.sendMessage(Message.raw("[MOTM] Test Protection warning: native invulnerability did not attach. "
                    + "Free-cast is still on, but arena mobs may still hit you."));
        }
    }

    private void clearFreeCastInvulnerability(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        Player player = onlineRuntimePlayers.get(playerId);
        if (player != null) {
            setRuntimeInvulnerability(player, false);
            resetFreeCastMovementNormalization(player);
        }
    }

    private void resetFreeCastMovementNormalization(Player player) {
        if (player == null) {
            return;
        }
        try {
            var playerRef = player.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                return;
            }
            MovementManager movementManager = playerRef.getStore().getComponent(playerRef, MovementManager.getComponentType());
            if (movementManager != null) {
                movementManager.applyDefaultSettings();
                PlayerRef universePlayerRef = playerRef.getStore().getComponent(playerRef, PlayerRef.getComponentType());
                if (universePlayerRef != null && universePlayerRef.getPacketHandler() != null) {
                    movementManager.update(universePlayerRef.getPacketHandler());
                }
            }
        } catch (IllegalStateException e) {
            LOG.fine("[MOTM] Skipped free-cast movement reset on the wrong store: " + e.getMessage());
        } catch (Exception e) {
            LOG.warning("[MOTM] Failed to reset free-cast movement normalization: " + e.getMessage());
        }
    }

    private boolean setRuntimeInvulnerability(Player player, boolean enabled) {
        if (player == null) {
            return false;
        }

        try {
            var playerRef = player.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                return false;
            }

            Store<EntityStore> store = playerRef.getStore();
            var componentType = Invulnerable.getComponentType();
            var existing = store.getComponent(playerRef, componentType);

            if (enabled) {
                if (existing != null) {
                    return true;
                }
                store.addComponent(playerRef, componentType);
                return true;
            }

            if (existing == null) {
                return true;
            }
            store.removeComponent(playerRef, componentType);
            return true;
        } catch (Exception e) {
            String playerLabel = "unknown";
            try {
                var playerRef = getUniversePlayerRef(player);
                if (playerRef != null && playerRef.getUsername() != null && !playerRef.getUsername().isBlank()) {
                    playerLabel = playerRef.getUsername();
                }
            } catch (Exception ignored) {
                // Keep fallback label.
            }
            LOG.warning("[MOTM] Failed to toggle dev invulnerability for "
                    + playerLabel + ": " + e.getMessage());
            return false;
        }
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

            String failureMessage = null;
            try {
                LOG.info("[MOTM] Processing queued ability cast: playerId="
                        + request.playerId()
                        + " abilityId=" + request.abilityId());
                failureMessage = motmCommand.executeQueuedAbilityCast(
                        request.playerId(),
                        request.abilityId(),
                        player,
                        request.targetRef(),
                        request.targetBlock()
                );
                LOG.info("[MOTM] Queued ability cast result: playerId="
                        + request.playerId()
                        + " abilityId=" + request.abilityId()
                        + " result=" + (failureMessage == null || failureMessage.isBlank() ? "<success>" : failureMessage));
            } catch (Throwable e) {
                failureMessage = "[MOTM] Queued ability cast failed safely for "
                        + request.abilityId() + ": " + e.getMessage();
                LOG.log(java.util.logging.Level.SEVERE, failureMessage, e);
            } finally {
                pendingAbilityCasts.remove(request);
            }
            if ((request.notifyFailures() || isDevToolsEnabled())
                    && failureMessage != null
                    && !failureMessage.isBlank()) {
                player.sendMessage(Message.raw(failureMessage));
            }
        }
    }

    private void processPendingSingleAbilityTests(Store<EntityStore> currentStore) {
        for (Map.Entry<String, String> entry : Map.copyOf(pendingSingleAbilityTests).entrySet()) {
            String playerId = entry.getKey();
            String abilityId = entry.getValue();
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingSingleAbilityTests.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            Ref<EntityStore> targetRef = null;
            Vector3i targetBlock = null;
            for (Ref<EntityStore> candidate : styleTestTargetsByPlayer.getOrDefault(playerId, List.of())) {
                Vector3d position = getEntityPosition(currentStore, candidate);
                if (position == null) {
                    continue;
                }
                targetRef = candidate;
                targetBlock = new Vector3i(
                        (int) Math.floor(position.x),
                        (int) Math.floor(position.y),
                        (int) Math.floor(position.z)
                );
                break;
            }

            LOG.info("[MOTM] Live ability test target: playerId=" + playerId
                    + " abilityId=" + abilityId
                    + " hasTarget=" + (targetRef != null)
                    + " targetBlock=" + targetBlock);
            queueAbilityCast(playerId, abilityId, targetRef, targetBlock, true);
            pendingSingleAbilityTests.remove(playerId);
        }
    }

    private void processPendingStyleTestMobSpawns(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(pendingStyleTestMobSpawns.keySet())) {
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingStyleTestMobSpawns.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            String mode = pendingStyleTestMobSpawns.get(playerId);
            String result = spawnStyleTestMobsNow(playerId, player, mode);
            recordServerTruth("style_test_mobs_spawned", null, MotmObservability.mapOf(
                    "playerId", playerId,
                    "mode", mode,
                    "result", result,
                    "trackedCount", countTrackedStyleTestTargets(playerId)
            ));
            player.sendMessage(Message.raw(result));
            pendingStyleTestMobSpawns.remove(playerId);
        }
    }

    private void processPendingStyleTestMobClears(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(pendingStyleTestMobClears)) {
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingStyleTestMobClears.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            String result = clearStyleTestMobsNow(playerId, currentStore, player);
            recordServerTruth("style_test_mobs_cleared", null, MotmObservability.mapOf(
                    "playerId", playerId,
                    "result", result,
                    "trackedCount", countTrackedStyleTestTargets(playerId)
            ));
            player.sendMessage(Message.raw(result));
            pendingStyleTestMobClears.remove(playerId);
        }
    }

    private void processPendingStyleReviewResets(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(pendingStyleReviewResets)) {
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingStyleReviewResets.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            String mobResult = clearStyleTestMobsNow(playerId, currentStore, player);
            String runtimeResult = gameplayPlaybackManager.resetReviewRuntime(playerId, currentStore, player);
            String arenaResult = scrubStyleReviewArena(player);
            statusEffectManager.clearEffects(playerId);
            elementalReactionManager.clearMarks(playerId);
            styleManager.resetCooldowns(playerId);
            setFreeCastEnabled(playerId, false);
            activeStyleTests.remove(playerId);
            pendingSingleAbilityTests.remove(playerId);
            pendingAbilityCasts.removeIf(request -> playerId.equals(request.playerId()));
            pendingStyleReviewResets.remove(playerId);

            String summary = "[MOTM] Style review arena reset: " + mobResult
                    + " | runtime=" + runtimeResult
                    + " | arena=" + arenaResult;
            LOG.info(summary + " playerId=" + playerId);
            player.sendMessage(Message.raw(summary));
        }
    }

    private void processPendingStyleTestMobCounts(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(pendingStyleTestMobCounts)) {
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingStyleTestMobCounts.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            String result = countStyleTestMobsNow(playerId, currentStore, player);
            recordServerTruth("style_test_mobs_counted", null, MotmObservability.mapOf(
                    "playerId", playerId,
                    "result", result,
                    "trackedCount", countTrackedStyleTestTargets(playerId)
            ));
            player.sendMessage(Message.raw(result));
            pendingStyleTestMobCounts.remove(playerId);
        }
    }

    private void processPendingDevGameModeChanges(Store<EntityStore> currentStore) {
        for (Map.Entry<String, GameMode> entry : Map.copyOf(pendingDevGameModeChanges).entrySet()) {
            String playerId = entry.getKey();
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingDevGameModeChanges.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            String result = applyDevGameModeChange(player, entry.getValue());
            player.sendMessage(Message.raw(result));
            pendingDevGameModeChanges.remove(playerId);
        }
    }

    private String applyDevGameModeChange(Player player, GameMode gameMode) {
        if (player == null || gameMode == null || player.getReference() == null || !player.getReference().isValid()
                || player.getReference().getStore() == null) {
            return "[MOTM] Dev game mode failed: player runtime/store missing.";
        }

        try {
            GameMode before = player.getGameMode();
            Player.setGameMode(player.getReference(), gameMode, player.getReference().getStore());
            String summary = "[MOTM] Dev game mode changed: before=" + before + " after=" + gameMode;
            LOG.info(summary + " playerId=" + getRuntimePlayerId(player));
            return summary;
        } catch (Throwable e) {
            String summary = "[MOTM] Dev game mode failed safely: " + e.getMessage();
            LOG.log(java.util.logging.Level.WARNING, summary, e);
            return summary;
        }
    }

    private void processPendingProofRequests(Store<EntityStore> currentStore) {
        for (Map.Entry<String, String> entry : Map.copyOf(pendingProofRequests).entrySet()) {
            String playerId = entry.getKey();
            String proofId = entry.getValue();
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingProofRequests.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            String result = null;
            String traceId = observability != null ? observability.nextTraceId("proof") : null;
            recordCausality("proof_begin", traceId, MotmObservability.mapOf(
                    "playerId", playerId,
                    "proofId", proofId
            ));
            String previousTraceId = enterObservabilityTrace(traceId);
            try {
                result = runProofNow(playerId, player, currentStore, proofId);
            } catch (Throwable e) {
                result = "[MOTM] Proof " + proofId + " failed safely: " + e.getMessage();
                LOG.log(java.util.logging.Level.SEVERE, result, e);
            } finally {
                restoreObservabilityTrace(previousTraceId);
                pendingProofRequests.remove(playerId);
            }
            LOG.info(result);
            recordCausality("proof_end", traceId, MotmObservability.mapOf(
                    "playerId", playerId,
                    "proofId", proofId,
                    "result", result
            ));
            player.sendMessage(Message.raw(result));
        }
    }

    private void processPendingDevRelocations(Store<EntityStore> currentStore) {
        for (Map.Entry<String, String> entry : Map.copyOf(pendingDevRelocations).entrySet()) {
            String playerId = entry.getKey();
            String target = entry.getValue();
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingDevRelocations.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            String result;
            try {
                result = relocateRuntimePlayerForTesting(playerId, target);
            } catch (Throwable e) {
                result = "[MOTM] Dev relocate failed safely: " + e.getMessage();
                LOG.log(java.util.logging.Level.SEVERE, result, e);
            } finally {
                pendingDevRelocations.remove(playerId);
            }
            LOG.info(result);
            player.sendMessage(Message.raw(result));
        }
    }

    private void processPendingDaylightRequests(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(pendingDaylightRequests)) {
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingDaylightRequests.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            String result;
            try {
                World world = currentStore != null && currentStore.getExternalData() != null
                        ? currentStore.getExternalData().getWorld()
                        : player.getWorld();
                WorldTimeResource time = currentStore == null
                        ? null
                        : currentStore.getResource(WorldTimeResource.getResourceType());
                if (world == null || time == null) {
                    result = "[MOTM] Dev daylight failed: world time resource unavailable.";
                } else {
                    time.setDayTime(0.5d, world, currentStore);
                    result = "[MOTM] Dev daylight applied: dayTime=0.5 sunlight="
                            + String.format(Locale.ROOT, "%.2f", time.getSunlightFactor());
                }
            } catch (Throwable e) {
                result = "[MOTM] Dev daylight failed safely: " + e.getMessage();
                LOG.log(java.util.logging.Level.SEVERE, result, e);
            } finally {
                pendingDaylightRequests.remove(playerId);
            }
            LOG.info(result);
            player.sendMessage(Message.raw(result));
        }
    }

    private void processActiveProofCleanups(Store<EntityStore> currentStore) {
        long now = System.currentTimeMillis();
        World currentWorld = currentStore != null && currentStore.getExternalData() != null
                ? currentStore.getExternalData().getWorld()
                : null;
        for (TemporaryProofSelection proof : List.copyOf(activeProofSelections)) {
            if (now < proof.cleanupAtMillis()) {
                continue;
            }
            if (currentWorld == null || (currentWorld != proof.world() && !currentWorld.equals(proof.world()))) {
                continue;
            }
            try {
                proof.originalSelection().place(null, proof.world(), Vector3i.ZERO, BlockMask.EMPTY);
                LOG.info("[MOTM] Proof cleanup restored selection: proofId=" + proof.proofId()
                        + " anchor=" + proof.anchor());
            } catch (Throwable e) {
                LOG.warning("[MOTM] Proof cleanup failed for " + proof.proofId()
                        + " anchor=" + proof.anchor()
                        + ": " + e.getMessage());
            }
            activeProofSelections.remove(proof);
        }

        for (TemporaryProofProxy proof : List.copyOf(activeProofProxies)) {
            if (now < proof.cleanupAtMillis()) {
                continue;
            }
            if (currentWorld == null || (currentWorld != proof.world() && !currentWorld.equals(proof.world()))) {
                continue;
            }
            try {
                if (proof.ref() != null && proof.ref().isValid() && proof.ref().getStore() != null) {
                    NPCEntity npc = proof.ref().getStore().getComponent(proof.ref(), NPCEntity.getComponentType());
                    if (npc != null) {
                        npc.setToDespawn();
                    }
                }
                LOG.info("[MOTM] Proof cleanup despawned proxy: proofId=" + proof.proofId());
            } catch (Exception e) {
                LOG.warning("[MOTM] Proof proxy cleanup failed for " + proof.proofId()
                        + ": " + e.getMessage());
            }
            activeProofProxies.remove(proof);
        }
    }

    private String runProofNow(String playerId, Player player, Store<EntityStore> currentStore, String proofId) {
        Vector3d basePosition = getPlayerPosition(player);
        Vector3d forward = normalizeHorizontal(getPlayerForward(player));
        if (basePosition == null || forward == null) {
            return "[MOTM] Proof " + proofId + " FAIL: could not resolve player position/facing.";
        }
        if (basePosition.y < -16.0) {
            return "[MOTM] Proof " + proofId + " FAIL: player appears below world at " + formatVector(basePosition);
        }

        return switch (proofId) {
            case "coating-metal" -> applyProofEffect(player, "MOTM_Proof_Coating_Metal", proofId);
            case "coating-obsidian" -> applyProofEffect(player, "MOTM_Proof_Coating_Obsidian", proofId);
            case "coating-stone" -> applyProofEffect(player, "MOTM_Proof_Coating_Stone", proofId);
            case "coating-poison-target" -> applyProofTargetEffect(playerId, currentStore, "MOTM_Proof_Coating_Poison", proofId);
            case "tempblock-metal-wall" -> runTempBlockProof(player, proofId, "Metal_Iron", 2, 2, 0);
            case "tempblock-stone-pillar" -> runTempBlockProof(player, proofId, "Rock_Stone_Brick_Pillar_Middle", 1, 3, 0);
            case "tempblock-flower" -> runTempBlockProof(player, proofId, "Plant_Flower_Common_Purple", 1, 1, 0);
            case "tempblock-sapling" -> runTempBlockProof(player, proofId, "Plant_Sapling_Oak", 1, 1, 0);
            case "tempblock-gem-cluster" -> runTempBlockProof(player, proofId, 2, 2, 1,
                    "Rock_Crystal_Green_Block",
                    "Rock_Crystal_Green_Large",
                    "Plant_Bush_Crystal",
                    "Plant_Leaves_Crystal",
                    "Plant_Sapling_Crystal");
            case "tempblock-cactus" -> runTempBlockProof(player, proofId, 1, 2, 0,
                    "Plant_Cactus_1",
                    "Prototype_Cactus_Kit_Tall_Base",
                    "Prototype_Cactus_One",
                    "Plant_Cactus_Ball_1");
            case "tempblock-roots" -> runTempBlockProof(player, proofId, 2, 1, 0,
                    "Plant_Roots_Leafy",
                    "Plant_Roots_Cave",
                    "Plant_Roots_Cave_Small",
                    "Plant_Vine_Thick_Roots");
            case "tempfluid-lava-ring" -> runTempFluidProof(player, proofId, 2, "Fluid_Lava", "Lava", "lava");
            case "tempfluid-water-field" -> runTempFluidProof(player, proofId, 2, "Fluid_Water", "Water", "water");
            case "tempfluid-mud-field" -> runTempFluidProof(player, proofId, 3, "Fluid_Water", "Water", "water");
            case "proxy-magma-blob" -> runProxyProof(player, proofId, "Slug_Magma", "MOTM_Terra_Impact", 2.5);
            case "proxy-cactus-projectile" -> runProxyProof(player, proofId, "Test_Dummy_Stationary", "MOTM_Terra_Cast", 2.5);
            case "proxy-gem" -> runProxyProof(player, proofId, "Spark_Living", "MOTM_Terra_Gem_Field", 3.0);
            case "proxy-gem-aura" -> runProxyProof(player, proofId, "Spark_Living", "MOTM_Proof_Gem_Green", 3.0);
            case "proxy-glass-shards" -> runProxyProof(player, proofId, "Spark_Living", "MOTM_Terra_Gem_Cast", 2.5);
            case "proxy-sand-cloud" -> runProxyProof(player, proofId, "Spark_Living", "MOTM_Proof_Sand_Cloud", 3.0);
            case "proxy-debris-wave" -> runProxyProof(player, proofId, "Spark_Living", "MOTM_Proof_Debris_Wave", 3.0);
            case "movement-burrow" -> runMovementProof(player, currentStore, proofId, forward, 4.0, false);
            case "movement-tunnel" -> runMovementProof(player, currentStore, proofId, forward, 2.0, true);
            case "movement-dust-devil" -> runMovementProof(player, currentStore, proofId, forward, 5.0, false);
            default -> "[MOTM] Proof " + proofId + " FAIL: unknown proof id.";
        };
    }

    private String applyProofEffect(Player player, String effectId, String proofId) {
        Ref<EntityStore> ref = player.getReference();
        boolean applied = applyProofEffectToRef(ref, effectId);
        return "[MOTM] Proof " + proofId + " " + (applied ? "PASS" : "FAIL")
                + ": effect=" + effectId
                + " target=player";
    }

    private String applyProofTargetEffect(String playerId, Store<EntityStore> store, String effectId, String proofId) {
        Ref<EntityStore> target = null;
        for (Ref<EntityStore> candidate : styleTestTargetsByPlayer.getOrDefault(playerId, List.of())) {
            if (candidate != null && candidate.isValid()) {
                target = candidate;
                break;
            }
        }
        if (target == null) {
            return "[MOTM] Proof " + proofId + " FAIL: no tracked stationary target. Run /motm dev test mobs stationary first.";
        }
        boolean applied = applyProofEffectToRef(target, effectId);
        return "[MOTM] Proof " + proofId + " " + (applied ? "PASS" : "FAIL")
                + ": effect=" + effectId
                + " target=trackedNpc";
    }

    private boolean applyProofEffectToRef(Ref<EntityStore> ref, String effectId) {
        if (ref == null || !ref.isValid() || ref.getStore() == null || effectId == null || effectId.isBlank()) {
            return false;
        }
        EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectId);
        if (effect == null) {
            LOG.warning("[MOTM] Proof effect missing: " + effectId);
            return false;
        }
        Store<EntityStore> store = ref.getStore();
        EffectControllerComponent controller = store.getComponent(ref, EffectControllerComponent.getComponentType());
        if (controller == null) {
            LOG.warning("[MOTM] Proof target missing EffectControllerComponent: " + effectId);
            return false;
        }
        boolean applied = controller.addEffect(ref, effect, store);
        recordClientIntent("proof_entity_effect_add", null, MotmObservability.mapOf(
                "effectId", effectId,
                "applied", applied,
                "entityIndex", ref.getIndex(),
                "nativeEffectsAfter", buildNativeEntityEffectsSnapshot(store, ref)
        ));
        return applied;
    }

    private String runTempBlockProof(Player player, String proofId, String blockId, int width, int height, int depth) {
        return runTempBlockProof(player, proofId, width, height, 0, blockId);
    }

    private String runTempBlockProof(Player player, String proofId, int width, int height, int yOffset, String... blockIds) {
        BlockResolution blockResolution = resolveProofBlockId(blockIds);
        String blockId = blockResolution.blockId();
        int blockTypeId = blockResolution.blockTypeId();
        if (blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "[MOTM] Proof " + proofId + " FAIL: block id did not resolve: candidates="
                    + String.join(",", blockIds);
        }
        return runTempBlockProof(player, proofId, blockId, blockTypeId, width, height, yOffset);
    }

    private String runTempBlockProof(Player player, String proofId, String blockId, int blockTypeId, int width, int height, int yOffset) {
        if (blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "[MOTM] Proof " + proofId + " FAIL: block id did not resolve: " + blockId;
        }
        World world = player.getWorld();
        Vector3d base = getPlayerPosition(player);
        Vector3d forward = normalizeHorizontal(getPlayerForward(player));
        if (world == null || base == null || forward == null) {
            return "[MOTM] Proof " + proofId + " FAIL: missing world/player transform.";
        }

        Vector3i baseAnchor = proofAnchor(base, forward, 4.0);
        Vector3i anchor = new Vector3i(baseAnchor.getX(), baseAnchor.getY() + yOffset, baseAnchor.getZ());
        BlockSelection selection = new BlockSelection();
        selection.setPosition(anchor.getX(), anchor.getY(), anchor.getZ());
        selection.setAnchorAtWorldPos(anchor.getX(), anchor.getY(), anchor.getZ());
        Vector3i rightStep = proofHorizontalRightStep(forward);
        int half = width / 2;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int offset = x - half;
                int wx = anchor.getX() + (rightStep.getX() * offset);
                int wy = anchor.getY() + y;
                int wz = anchor.getZ() + (rightStep.getZ() * offset);
                selection.addBlockAtWorldPos(wx, wy, wz, blockTypeId, 0, 0, 0);
            }
        }
        return placeTemporarySelection(proofId, world, anchor, selection, 4000L,
                "block=" + blockId + " blockTypeId=" + blockTypeId + " blocks=" + selection.getBlockCount());
    }

    private BlockResolution resolveProofBlockId(String... blockIds) {
        for (String candidate : blockIds) {
            int blockTypeId = BlockType.getBlockIdOrUnknown(candidate, "MOTM proof block resolution");
            if (blockTypeId != BlockType.UNKNOWN_ID && blockTypeId != BlockType.EMPTY_ID) {
                return new BlockResolution(candidate, blockTypeId);
            }
        }
        return new BlockResolution("", BlockType.UNKNOWN_ID);
    }

    private Vector3i proofHorizontalRightStep(Vector3d forward) {
        Vector3d right = new Vector3d(-forward.z, 0.0, forward.x);
        if (Math.abs(right.x) >= Math.abs(right.z)) {
            return new Vector3i(right.x >= 0.0 ? 1 : -1, 0, 0);
        }
        return new Vector3i(0, 0, right.z >= 0.0 ? 1 : -1);
    }

    private String runTempFluidProof(Player player, String proofId, int radius, String... fluidIds) {
        FluidResolution fluidResolution = resolveProofFluidId(fluidIds);
        int fluidTypeId = fluidResolution.fluidTypeId();
        Fluid fluid = Fluid.getAssetMap().getAsset(fluidTypeId);
        if (fluidTypeId == Fluid.UNKNOWN_ID || fluidTypeId == Fluid.EMPTY_ID || fluid == null) {
            return "[MOTM] Proof " + proofId + " FAIL: fluid id did not resolve: candidates="
                    + String.join(",", fluidIds)
                    + " available=" + listProofFluidIds();
        }
        World world = player.getWorld();
        Vector3d base = getPlayerPosition(player);
        Vector3d forward = normalizeHorizontal(getPlayerForward(player));
        if (world == null || base == null || forward == null) {
            return "[MOTM] Proof " + proofId + " FAIL: missing world/player transform.";
        }

        Vector3i anchor = proofAnchor(base, forward, 5.0);
        BlockSelection selection = new BlockSelection();
        selection.setPosition(anchor.getX(), anchor.getY(), anchor.getZ());
        selection.setAnchorAtWorldPos(anchor.getX(), anchor.getY(), anchor.getZ());
        byte fluidLevel = (byte) Math.max(1, fluid.getMaxFluidLevel());
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                double dist = Math.sqrt((x * x) + (z * z));
                if (dist > radius + 0.2) {
                    continue;
                }
                selection.addFluidAtWorldPos(anchor.getX() + x, anchor.getY(), anchor.getZ() + z, fluidTypeId, fluidLevel);
            }
        }
        return placeTemporarySelection(proofId, world, anchor, selection, 4000L,
                "fluid=" + fluidResolution.fluidId()
                        + " fluidTypeId=" + fluidTypeId
                        + " fluids=" + selection.getFluidCount());
    }

    private FluidResolution resolveProofFluidId(String... fluidIds) {
        for (String candidate : fluidIds) {
            int directId = Fluid.getAssetMap().getIndexOrDefault(candidate, Fluid.UNKNOWN_ID);
            if (isUsableProofFluidId(directId)) {
                return new FluidResolution(candidate, directId);
            }
        }
        for (String candidate : fluidIds) {
            int convertedId = Fluid.getFluidIdOrUnknown(candidate, "MOTM proof fluid resolution");
            if (isUsableProofFluidId(convertedId)) {
                Fluid fluid = Fluid.getAssetMap().getAsset(convertedId);
                return new FluidResolution(fluid != null ? fluid.getId() : candidate, convertedId);
            }
        }
        return new FluidResolution("", Fluid.UNKNOWN_ID);
    }

    private boolean isUsableProofFluidId(int fluidTypeId) {
        if (fluidTypeId == Fluid.UNKNOWN_ID || fluidTypeId == Fluid.EMPTY_ID) {
            return false;
        }
        Fluid fluid = Fluid.getAssetMap().getAsset(fluidTypeId);
        return fluid != null && !fluid.isUnknown();
    }

    private String listProofFluidIds() {
        List<String> ids = new ArrayList<>();
        int max = Math.min(Fluid.getAssetMap().getNextIndex(), 64);
        for (int index = 0; index < max; index++) {
            Fluid fluid = Fluid.getAssetMap().getAsset(index);
            if (fluid != null && !fluid.isUnknown()) {
                ids.add(index + ":" + fluid.getId());
            }
        }
        return ids.isEmpty() ? "<none>" : String.join("|", ids);
    }

    private String placeTemporarySelection(String proofId,
                                           World world,
                                           Vector3i anchor,
                                           BlockSelection selection,
                                           long lifetimeMillis,
                                           String summary) {
        try {
            BlockSelection original = selection.place(null, world, Vector3i.ZERO, BlockMask.EMPTY);
            activeProofSelections.add(new TemporaryProofSelection(
                    proofId,
                    world,
                    anchor,
                    original,
                    System.currentTimeMillis() + lifetimeMillis
            ));
            recordServerTruth("proof_temporary_selection_placed", null, MotmObservability.mapOf(
                    "proofId", proofId,
                    "anchor", anchor.toString(),
                    "blockCount", selection.getBlockCount(),
                    "fluidCount", selection.getFluidCount(),
                    "lifetimeMillis", lifetimeMillis,
                    "summary", summary
            ));
            return "[MOTM] Proof " + proofId + " PASS: placed temporary selection "
                    + summary
                    + " anchor=" + anchor
                    + " cleanupMs=" + lifetimeMillis;
        } catch (Exception e) {
            LOG.log(java.util.logging.Level.SEVERE, "[MOTM] Proof temporary selection failed: " + proofId, e);
            return "[MOTM] Proof " + proofId + " FAIL: temporary selection placement failed: " + e.getMessage();
        }
    }

    private String runProxyProof(Player player, String proofId, String roleId, String effectId, double distanceAhead) {
        World world = player.getWorld();
        Vector3d base = getPlayerPosition(player);
        Vector3d forward = normalizeHorizontal(getPlayerForward(player));
        if (world == null || base == null || forward == null) {
            return "[MOTM] Proof " + proofId + " FAIL: missing world/player transform.";
        }
        Vector3d position = base.clone().addScaled(forward, distanceAhead);
        NPCEntity proxy = new NPCEntity(world);
        proxy.setRoleName(roleId);
        proxy.setDespawnTime(4.5f);
        world.spawnEntity(proxy, position, new Vector3f(0f, 0f, 0f));

        Ref<EntityStore> ref = proxy.getReference();
        boolean effectApplied = applyProofEffectToRef(ref, effectId);
        if (ref != null && ref.isValid()) {
            activeProofProxies.add(new TemporaryProofProxy(proofId, world, ref, System.currentTimeMillis() + 4500L));
        }
        recordClientIntent("proof_proxy_spawned", null, MotmObservability.mapOf(
                "proofId", proofId,
                "roleId", roleId,
                "effectId", effectId,
                "effectApplied", effectApplied,
                "position", formatVector(position),
                "entityIndex", ref != null && ref.isValid() ? ref.getIndex() : -1
        ));
        return "[MOTM] Proof " + proofId + " PASS: proxy role=" + roleId
                + " effect=" + effectId
                + " effectApplied=" + effectApplied
                + " position=" + formatVector(position);
    }

    private String runMovementProof(Player player,
                                    Store<EntityStore> currentStore,
                                    String proofId,
                                    Vector3d forward,
                                    double distance,
                                    boolean surfaceRecovery) {
        Ref<EntityStore> ref = player.getReference();
        if (ref == null || !ref.isValid() || currentStore == null) {
            return "[MOTM] Proof " + proofId + " FAIL: missing player ref/store.";
        }
        TransformComponent transform = currentStore.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getPosition() == null) {
            return "[MOTM] Proof " + proofId + " FAIL: missing TransformComponent.";
        }
        Vector3d start = transform.getPosition().clone();
        Vector3d destination = start.clone().addScaled(forward, distance);
        if (surfaceRecovery) {
            destination.y = Math.max(start.y, destination.y);
        }
        transform.teleportPosition(destination);
        Vector3d observed = transform.getPosition() != null ? transform.getPosition().clone() : null;
        double observedDisplacement = observed != null ? distance(start, observed) : 0.0;
        double destinationError = observed != null ? distance(observed, destination) : Double.POSITIVE_INFINITY;
        boolean moved = observedDisplacement >= Math.min(0.75, Math.max(0.1, distance * 0.25));
        recordServerTruth("proof_movement", null, MotmObservability.mapOf(
                "proofId", proofId,
                "start", formatVector(start),
                "destination", formatVector(destination),
                "observed", observed != null ? formatVector(observed) : null,
                "observedDisplacement", observedDisplacement,
                "destinationError", destinationError,
                "moved", moved,
                "movementMethod", "teleportPosition",
                "surfaceRecovery", surfaceRecovery
        ));
        return "[MOTM] Proof " + proofId + " " + (moved ? "PASS" : "FAIL")
                + ": movement start=" + formatVector(start)
                + " destination=" + formatVector(destination)
                + " observed=" + (observed != null ? formatVector(observed) : "null")
                + " displacement=" + String.format(Locale.ROOT, "%.2f", observedDisplacement)
                + " movementMethod=teleportPosition"
                + " surfaceRecovery=" + surfaceRecovery;
    }

    private Vector3i proofAnchor(Vector3d base, Vector3d forward, double distanceAhead) {
        Vector3d anchor = base.clone().addScaled(forward, distanceAhead);
        return new Vector3i(
                (int) Math.floor(anchor.x),
                (int) Math.floor(base.y),
                (int) Math.floor(anchor.z)
        );
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
            LOG.info("[MOTM] Pending spellbook grant processed: playerId=" + playerId
                    + " granted=" + granted
                    + " nowHasSpellbook=" + playerHasSpellbook(player));
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

    private void processPendingTerraReviewKitGrants(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(pendingTerraReviewKitGrants)) {
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingTerraReviewKitGrants.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            String result = grantTerraReviewKit(player);
            player.sendMessage(Message.raw(result));
            pendingTerraReviewKitGrants.remove(playerId);
        }
    }

    private void processPendingTerraReviewInventoryCleans(Store<EntityStore> currentStore) {
        for (String playerId : Set.copyOf(pendingTerraReviewInventoryCleans)) {
            Player player = onlineRuntimePlayers.get(playerId);
            if (player == null) {
                pendingTerraReviewInventoryCleans.remove(playerId);
                continue;
            }
            if (!isPlayerInStore(player, currentStore)) {
                continue;
            }

            String result = cleanTerraReviewInventory(player);
            player.sendMessage(Message.raw(result));
            pendingTerraReviewInventoryCleans.remove(playerId);
        }
    }

    private String cleanTerraReviewInventory(Player player) {
        if (player == null || player.getReference() == null || !player.getReference().isValid()
                || player.getReference().getStore() == null) {
            return "[MOTM] Terra inventory clean failed: player runtime/store missing.";
        }

        CombinedItemContainer inventory = getCombinedPlayerInventory(player);
        if (inventory == null) {
            return "[MOTM] Terra inventory clean failed: player inventory unavailable.";
        }

        Set<String> keptEssentials = new java.util.HashSet<>();
        List<Short> slotsToRemove = new ArrayList<>();
        inventory.forEach((slot, stack) -> {
            if (stack == null || stack.getItemId() == null || stack.getQuantity() <= 0) {
                return;
            }

            String itemId = stack.getItemId();
            if (TERRA_REVIEW_ESSENTIAL_ITEM_IDS.contains(itemId) && keptEssentials.add(itemId)) {
                return;
            }

            slotsToRemove.add(slot);
        });

        int removed = 0;
        for (short slot : slotsToRemove) {
            try {
                inventory.removeItemStackFromSlot(slot);
                removed++;
            } catch (Throwable e) {
                LOG.log(java.util.logging.Level.WARNING,
                        "[MOTM] Terra inventory clean slot removal failed: slot=" + slot,
                        e);
            }
        }

        ensureSpellbookItem(player);
        int granted = 0;
        granted += ensureReviewItem(player, "Tool_Pickaxe_Iron");
        granted += ensureReviewItem(player, "Weapon_Sword_Iron");

        String summary = "[MOTM] Terra review inventory cleaned: removedSlots=" + removed
                + " kept=spellbook,pickaxe,sword"
                + " grantedMissing=" + granted;
        LOG.info(summary + " playerId=" + getRuntimePlayerId(player));
        return summary;
    }

    private String grantTerraReviewKit(Player player) {
        if (player == null || player.getReference() == null || !player.getReference().isValid()
                || player.getReference().getStore() == null) {
            return "[MOTM] Terra review kit failed: player runtime/store missing.";
        }

        ensureSpellbookItem(player);
        ensureDevBookItem(player);

        int granted = 0;
        List<String> missing = new ArrayList<>();
        List<String> grantedIds = new ArrayList<>();
        for (TerraReviewKitItem item : TERRA_REVIEW_KIT_ITEMS) {
            if (!isItemAssetAvailable(item.itemId())) {
                missing.add(item.itemId());
                continue;
            }
            try {
                player.giveItem(new ItemStack(item.itemId(), item.quantity()), player.getReference(), player.getReference().getStore());
                granted++;
                grantedIds.add(item.itemId() + "x" + item.quantity());
            } catch (Throwable e) {
                LOG.log(java.util.logging.Level.WARNING,
                        "[MOTM] Terra review kit item grant failed: itemId=" + item.itemId()
                                + " reason=" + e.getMessage(),
                        e);
                missing.add(item.itemId());
            }
        }

        String summary = "[MOTM] Terra review kit granted: itemStacks=" + granted
                + " missing=" + missing.size()
                + " items=" + String.join(",", grantedIds)
                + (missing.isEmpty() ? "" : " missingIds=" + String.join(",", missing));
        LOG.info(summary + " playerId=" + getRuntimePlayerId(player));
        return summary;
    }

    private int ensureReviewItem(Player player, String itemId) {
        if (player == null || itemId == null || itemId.isBlank()) {
            return 0;
        }

        CombinedItemContainer inventory = getCombinedPlayerInventory(player);
        if (inventory == null) {
            return 0;
        }

        if (inventory.countItemStacks(stack -> stack != null && itemId.equals(stack.getItemId())) > 0) {
            return 0;
        }

        if (!isItemAssetAvailable(itemId)) {
            LOG.warning("[MOTM] Review item missing from asset map: " + itemId);
            return 0;
        }

        try {
            player.giveItem(new ItemStack(itemId), player.getReference(), player.getReference().getStore());
            return 1;
        } catch (Throwable e) {
            LOG.log(java.util.logging.Level.WARNING,
                    "[MOTM] Review item grant failed: itemId=" + itemId,
                    e);
            return 0;
        }
    }

    private boolean isItemAssetAvailable(String itemId) {
        return itemId != null && !itemId.isBlank()
                && com.hypixel.hytale.server.core.asset.type.item.config.Item.getAssetMap().getAsset(itemId) != null;
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
                            + ". Waterskins are no longer a casting cost, but they remain available for future Hydro utility tests."
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
            String traceId = currentOrNewClientIntentTraceId();
            String previousTraceId = enterObservabilityTrace(traceId);
            try {
                recordClientIntent("custom_hud_refresh", traceId, MotmObservability.mapOf(
                        "playerId", playerId,
                        "hud", "MOTM_StatusHud"
                ));
                hud.refresh();
            } finally {
                restoreObservabilityTrace(previousTraceId);
            }
        }
    }

    public Player getRuntimePlayer(String playerId) {
        return playerId == null ? null : onlineRuntimePlayers.get(playerId);
    }

    public String describeRuntimePlayerPosition(String playerId) {
        Player player = getRuntimePlayer(playerId);
        try {
            Vector3d position = getPlayerPosition(player);
            Vector3d forward = normalizeHorizontal(getPlayerForward(player));
            String worldId = player != null && player.getWorld() != null ? player.getWorld().getName() : "unknown";
            String summary = "[MOTM] Dev position: world=" + worldId
                    + " position=" + formatVector(position)
                    + " forward=" + formatVector(forward);
            LOG.info(summary);
            return summary;
        } catch (Throwable e) {
            String summary = "[MOTM] Dev position failed safely: " + e.getMessage();
            LOG.log(java.util.logging.Level.WARNING, summary, e);
            return summary;
        }
    }

    public String queueRuntimePlayerRelocationForTesting(String playerId, String target) {
        String normalizedTarget = target == null ? "up" : target.toLowerCase(Locale.ROOT);
        if (!List.of("up", "flatlands", "lane").contains(normalizedTarget)) {
            return "[MOTM] Dev relocate usage: /motm dev relocate <up|flatlands|lane>";
        }
        boolean added = pendingDevRelocations.put(playerId, normalizedTarget) == null;
        LOG.info("[MOTM] Dev relocate queued: playerId=" + playerId
                + " target=" + normalizedTarget
                + " added=" + added);
        return added
                ? "[MOTM] Dev relocate queued: " + normalizedTarget + "."
                : "[MOTM] A dev relocate request is already queued.";
    }

    public String queueDaylightForTesting(String playerId) {
        if (playerId == null || playerId.isBlank() || onlineRuntimePlayers.get(playerId) == null) {
            return "[MOTM] Join a world and run this in-game to force daylight.";
        }
        boolean added = pendingDaylightRequests.add(playerId);
        LOG.info("[MOTM] Dev daylight queued: playerId=" + playerId + " added=" + added);
        return added
                ? "[MOTM] Dev daylight queued."
                : "[MOTM] Dev daylight is already queued.";
    }

    public String queueGameModeForTesting(String playerId, String mode) {
        if (!devToolsEnabled) {
            return devToolsDisabledMessage();
        }
        if (playerId == null || playerId.isBlank() || onlineRuntimePlayers.get(playerId) == null) {
            return "[MOTM] Join a world and run this in-game to change review mode.";
        }

        GameMode gameMode = parseReviewGameMode(mode);
        if (gameMode == null) {
            return "[MOTM] Dev mode usage: /motm dev mode <creative|adventure>. "
                    + "This Hytale build exposes Adventure and Creative; Survival is not present in the protocol enum.";
        }

        pendingDevGameModeChanges.put(playerId, gameMode);
        LOG.info("[MOTM] Dev game mode queued: playerId=" + playerId + " mode=" + gameMode);
        return "[MOTM] Dev game mode queued: " + gameMode + ".";
    }

    private GameMode parseReviewGameMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "creative", "c" -> GameMode.Creative;
            case "adventure", "survival", "s", "a" -> GameMode.Adventure;
            default -> null;
        };
    }

    public String queueTerraReviewKitGrant(String playerId) {
        if (!devToolsEnabled) {
            return devToolsDisabledMessage();
        }
        if (playerId == null || playerId.isBlank() || onlineRuntimePlayers.get(playerId) == null) {
            return "[MOTM] Join a world and run this in-game to receive the Terra review kit.";
        }

        boolean added = pendingTerraReviewKitGrants.add(playerId);
        LOG.info("[MOTM] Terra review kit queued: playerId=" + playerId + " added=" + added);
        return added
                ? "[MOTM] Terra review kit queued."
                : "[MOTM] Terra review kit is already queued.";
    }

    public String queueTerraReviewInventoryClean(String playerId) {
        if (!devToolsEnabled) {
            return devToolsDisabledMessage();
        }
        if (playerId == null || playerId.isBlank() || onlineRuntimePlayers.get(playerId) == null) {
            return "[MOTM] Join a world and run this in-game to clean the Terra review inventory.";
        }

        boolean added = pendingTerraReviewInventoryCleans.add(playerId);
        LOG.info("[MOTM] Terra review inventory clean queued: playerId=" + playerId + " added=" + added);
        return added
                ? "[MOTM] Terra review inventory clean queued."
                : "[MOTM] Terra review inventory clean is already queued.";
    }

    private String relocateRuntimePlayerForTesting(String playerId, String target) {
        Player player = getRuntimePlayer(playerId);
        if (player == null || player.getReference() == null || !player.getReference().isValid()
                || player.getReference().getStore() == null) {
            return "[MOTM] Dev relocate failed: player runtime/store missing.";
        }
        TransformComponent transform = player.getReference().getStore()
                .getComponent(player.getReference(), TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
            return "[MOTM] Dev relocate failed: TransformComponent missing.";
        }

        Vector3d start = transform.getTransform().getPosition().clone();
        String normalizedTarget = target == null ? "up" : target.toLowerCase(Locale.ROOT);
        Vector3d destination = switch (normalizedTarget) {
            case "flatlands" -> new Vector3d(start.x + 96.0, Math.max(start.y + 40.0, 160.0), start.z + 96.0);
            case "lane" -> new Vector3d(start.x + 96.0, resolveTestingLaneY(player, start), start.z + 96.0);
            case "up" -> new Vector3d(start.x, start.y + 12.0, start.z);
            default -> null;
        };
        if (destination == null) {
            return "[MOTM] Dev relocate usage: /motm dev relocate <up|flatlands|lane>";
        }

        try {
            if ("flatlands".equals(normalizedTarget) || "lane".equals(normalizedTarget)) {
                placeRelocationPlatform(player, destination, normalizedTarget);
            }
            transform.teleportPosition(destination);
            String summary = "[MOTM] Dev relocate " + normalizedTarget
                    + ": start=" + formatVector(start)
                    + " destination=" + formatVector(destination);
            LOG.info(summary);
            return summary;
        } catch (Throwable e) {
            String summary = "[MOTM] Dev relocate failed safely: " + e.getMessage();
            LOG.log(java.util.logging.Level.SEVERE, summary, e);
            return summary;
        }
    }

    private double resolveTestingLaneY(Player player, Vector3d start) {
        String worldName = player != null && player.getWorld() != null
                ? player.getWorld().getName().toLowerCase(Locale.ROOT)
                : "";
        if (worldName.contains("flat")) {
            return 80.0;
        }
        return start != null ? start.y : 80.0;
    }

    private void placeRelocationPlatform(Player player, Vector3d destination, String target) {
        World world = player.getWorld();
        if (world == null || destination == null) {
            return;
        }
        int blockTypeId = BlockType.getBlockIdOrUnknown("Soil_Grass", "MOTM dev relocation platform");
        if (blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            blockTypeId = BlockType.getBlockIdOrUnknown("Rock_Stone_Brick_Pillar_Middle", "MOTM dev relocation platform");
        }
        if (blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            LOG.warning("[MOTM] Dev relocate platform skipped: no platform block resolved.");
            return;
        }

        int floorY = (int) Math.floor(destination.y) - 1;
        int centerX = (int) Math.floor(destination.x);
        int centerZ = (int) Math.floor(destination.z);
        BlockSelection platform = new BlockSelection();
        platform.setPosition(centerX, floorY, centerZ);
        platform.setAnchorAtWorldPos(centerX, floorY, centerZ);
        int radius = "lane".equals(target) ? 60 : 30;
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                platform.addBlockAtWorldPos(centerX + x, floorY, centerZ + z, blockTypeId, 0, 0, 0);
            }
        }
        try {
            platform.place(null, world, Vector3i.ZERO, BlockMask.EMPTY);
            LOG.info("[MOTM] Dev relocate platform placed: target=" + target
                    + " center=(" + centerX + "," + floorY + "," + centerZ + ")"
                    + " blocks=" + platform.getBlockCount()
                    + " blockTypeId=" + blockTypeId);
        } catch (Throwable e) {
            LOG.log(java.util.logging.Level.WARNING, "[MOTM] Dev relocate platform failed safely.", e);
        }
    }

    public Player getRuntimePlayer(Ref<EntityStore> entityRef) {
        if (entityRef == null || !entityRef.isValid()) {
            return null;
        }
        for (Player player : onlineRuntimePlayers.values()) {
            if (player == null) {
                continue;
            }
            Ref<EntityStore> playerRef = player.getReference();
            if (playerRef != null
                    && playerRef.isValid()
                    && playerRef.getStore() == entityRef.getStore()
                    && playerRef.getIndex() == entityRef.getIndex()) {
                return player;
            }
        }
        return null;
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
            PlayerRef playerRef = player.getPlayerRef();
            if (playerRef != null && playerRef.getUuid() != null) {
                return playerRef;
            }
        } catch (IllegalStateException ignored) {
            // Fall back to the entity store lookup below.
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
        if (player == null) {
            return false;
        }

        var playerRef = player.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return false;
        }

        if (currentStore == null) {
            return true;
        }

        Store<EntityStore> playerStore = playerRef.getStore();
        if (playerStore == currentStore || playerStore.equals(currentStore)) {
            return true;
        }

        World playerWorld = player.getWorld();
        World currentWorld = currentStore.getExternalData() != null
                ? currentStore.getExternalData().getWorld()
                : null;

        if (playerWorld != null && currentWorld != null) {
            return playerWorld == currentWorld || playerWorld.equals(currentWorld);
        }

        return false;
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
        LOG.info("[MOTM] Queue runtime rebuild: playerId=" + player.getPlayerId());
        pendingRuntimeRebuilds.add(player.getPlayerId());
    }

    private void rebuildPlayerRuntimeNow(com.motm.model.PlayerData player) {
        if (player == null || player.getPlayerId() == null) {
            return;
        }

        String playerId = player.getPlayerId();
        LOG.info("[MOTM] >>> rebuildPlayerRuntimeNow START playerId=" + playerId
                + " class=" + player.getPlayerClass()
                + " styles=" + player.getSelectedStyles());

        styleManager.resetCooldowns(playerId);
        classPassiveManager.clearPlayerState(playerId);
        statusEffectManager.clearEffects(playerId);
        elementalReactionManager.clearMarks(playerId);
        gameplayPlaybackManager.clearArmedStomp(playerId);
        resourceManager.clearPlayerState(playerId);
        resourceManager.synchronizePersistentState(player);

        player.clearSynergyBonuses();
        player.clearRaceBonuses();

        if (player.getPlayerClass() == null) {
            refreshPlayerProgressionBonusesNow(playerId);
            if (!isFreeCastEnabled(playerId)) {
                clearFreeCastInvulnerability(playerId);
            }
            refreshStatusHudNow(playerId);
            LOG.info("[MOTM] <<< rebuildPlayerRuntimeNow END playerId=" + playerId + " class=<none>");
            return;
        }

        resourceManager.initializeForPlayer(player);
        perkManager.reapplyAllPerks(player, synergyEngine);
        queueHydroContainerSync(playerId);

        if (player.getRace() != null) {
            raceManager.applyRaceBonuses(player, statusEffectManager);
        }
        refreshPlayerProgressionBonusesNow(playerId);
        classPassiveManager.onPlayerJoin(player);
        if (isFreeCastEnabled(playerId)) {
            Player runtimePlayer = onlineRuntimePlayers.get(playerId);
            if (runtimePlayer != null) {
                ensureFreeCastInvulnerability(runtimePlayer);
            }
        }
        refreshStatusHudNow(playerId);
        LOG.info("[MOTM] <<< rebuildPlayerRuntimeNow END playerId=" + playerId
                + " class=" + player.getPlayerClass()
                + " styles=" + player.getSelectedStyles());
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
        if (runtimePlayer == null) {
            lastAppliedTargetHealthByPlayer.remove(playerId);
            return;
        }
        if (playerData == null || playerData.getPlayerClass() == null) {
            clearPlayerLevelHealthBonus(runtimePlayer, playerId);
            return;
        }

        applyPlayerLevelHealthBonus(runtimePlayer, playerData);
    }

    private void clearPlayerLevelHealthBonus(Player runtimePlayer, String playerId) {
        if (runtimePlayer == null) {
            if (playerId != null) {
                lastAppliedTargetHealthByPlayer.remove(playerId);
            }
            return;
        }

        try {
            var playerRef = runtimePlayer.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                return;
            }

            EntityStatMap entityStatMap = playerRef.getStore().getComponent(playerRef, EntityStatMap.getComponentType());
            if (entityStatMap == null) {
                return;
            }

            entityStatMap.removeModifier(DefaultEntityStatTypes.getHealth(), PLAYER_LEVEL_HEALTH_MODIFIER_ID);
            if (playerId != null) {
                lastAppliedTargetHealthByPlayer.remove(playerId);
            }
        } catch (IllegalStateException e) {
            LOG.warning("[MOTM] Skipped clearing progression health bonus on the wrong store for "
                    + (playerId == null ? "unknown" : playerId) + ": " + e.getMessage());
        }
    }

    private void applyPlayerLevelHealthBonus(Player runtimePlayer, com.motm.model.PlayerData playerData) {
        if (runtimePlayer == null || playerData == null) {
            return;
        }

        try {
            var playerRef = runtimePlayer.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                return;
            }

            EntityStatMap entityStatMap = playerRef.getStore().getComponent(playerRef, EntityStatMap.getComponentType());
            if (entityStatMap == null) {
                return;
            }

            String classId = playerData.getPlayerClass();
            var classData = classId == null ? null : dataLoader.getClassData(classId);
            if (classData == null || classData.getStartingStats() == null) {
                return;
            }

            double baseHealth = classData.getStartingStats().getOrDefault("health", 100.0);
            double healthGrowth = classData.getStatGrowthPerLevel() == null
                    ? 0.0
                    : classData.getStatGrowthPerLevel().getOrDefault("health", 0.0);
            double targetHealth = baseHealth + (Math.max(1, playerData.getLevel()) - 1) * healthGrowth;
            if (!Double.isFinite(targetHealth) || targetHealth <= 0.0) {
                return;
            }

            String playerId = playerData.getPlayerId();
            Double lastAppliedTargetHealth = playerId == null ? null : lastAppliedTargetHealthByPlayer.get(playerId);
            if (lastAppliedTargetHealth != null && Math.abs(lastAppliedTargetHealth - targetHealth) < 0.01) {
                return;
            }

            EntityStatValue healthBefore = entityStatMap.get(DefaultEntityStatTypes.getHealth());
            float currentHealthBefore = healthBefore != null ? healthBefore.get() : 0.0f;
            float maxHealthBefore = healthBefore != null ? healthBefore.getMax() : 0.0f;
            boolean shouldMaximizeAfterApply = playerId != null
                    && isFreeCastEnabled(playerId)
                    || (maxHealthBefore > 0.0f && currentHealthBefore >= maxHealthBefore - 0.5f);
            float previousHealthRatio = maxHealthBefore > 0.0f
                    ? Math.max(0.0f, Math.min(1.0f, currentHealthBefore / maxHealthBefore))
                    : 1.0f;

            entityStatMap.removeModifier(DefaultEntityStatTypes.getHealth(), PLAYER_LEVEL_HEALTH_MODIFIER_ID);

            EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
            if (health == null || health.getMax() <= 0.0f) {
                return;
            }

            float healthMultiplier = (float) (targetHealth / health.getMax());
            if (!Float.isFinite(healthMultiplier) || healthMultiplier <= 0f) {
                return;
            }

            if (Math.abs(healthMultiplier - 1.0f) > 0.0001f) {
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

            EntityStatValue updatedHealth = entityStatMap.get(DefaultEntityStatTypes.getHealth());
            if (updatedHealth != null && updatedHealth.getMax() > 0.0f) {
                if (shouldMaximizeAfterApply) {
                    entityStatMap.maximizeStatValue(DefaultEntityStatTypes.getHealth());
                } else {
                    float desiredCurrentHealth = updatedHealth.getMax() * previousHealthRatio;
                    float missingHealth = desiredCurrentHealth - updatedHealth.get();
                    if (missingHealth > 0.05f) {
                        entityStatMap.addStatValue(DefaultEntityStatTypes.getHealth(), missingHealth);
                    }
                }
            }

            if (playerId != null) {
                lastAppliedTargetHealthByPlayer.put(playerId, targetHealth);
            }
        } catch (IllegalStateException e) {
            LOG.warning("[MOTM] Skipped progression health bonus refresh on the wrong store for "
                    + playerData.getPlayerName() + ": " + e.getMessage());
        }
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

    private Vector3d getPlayerForward(Player player) {
        if (player == null) {
            return null;
        }

        var playerRef = player.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return null;
        }

        TransformComponent transform = playerRef.getStore().getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getDirection() == null) {
            return new Vector3d(0.0, 0.0, 1.0);
        }

        Vector3d direction = transform.getTransform().getDirection().clone();
        if (!direction.isFinite() || direction.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        return direction;
    }

    private Vector3d normalizeHorizontal(Vector3d direction) {
        if (direction == null) {
            return new Vector3d(0.0, 0.0, 1.0);
        }

        Vector3d horizontal = new Vector3d(direction.x, 0.0, direction.z);
        if (!horizontal.isFinite() || horizontal.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        horizontal.normalize();
        return horizontal;
    }

    private String formatVector(Vector3d position) {
        if (position == null) {
            return "(unknown)";
        }
        return String.format(Locale.ROOT, "(%.2f, %.2f, %.2f)", position.x, position.y, position.z);
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
            LOG.info("[MOTM] >>> handlePlayerInteract ENTERED action="
                    + (event != null ? event.getActionType() : "<null>"));
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
            int bookSlot = resolveSpellbookInteractSlot(actionType);
            String heldItemId = itemInHand != null ? itemInHand.getItemId() : "<none>";
            boolean hasSelectedStyle = playerData.getSelectedStyles() != null && !playerData.getSelectedStyles().isEmpty();
            if (hasSelectedStyle && isDevToolsEnabled()) {
                LOG.info("[MOTM] Input trace(interact): player="
                        + playerData.getPlayerName()
                        + " action=" + actionType
                        + " item=" + heldItemId
                        + " recognizedSpellbook=" + holdingSpellbook);
            }
            if (holdingSpellbook) {
                LOG.info("[MOTM] Spellbook interact input: player="
                        + playerData.getPlayerName()
                        + " action=" + actionType
                        + " slot=" + bookSlot
                        + " item=" + heldItemId);
            }
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
        if (!resourceManager.areAbilityResourceCostsEnabled()) {
            return false;
        }
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
            LOG.info("[MOTM] >>> handlePlayerMouseButton ENTERED button="
                    + (event != null && event.getMouseButton() != null
                    ? event.getMouseButton().mouseButtonType + "/" + event.getMouseButton().state
                    : "<null>"));
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
            boolean hasSelectedStyle = playerData.getSelectedStyles() != null && !playerData.getSelectedStyles().isEmpty();
            String loggedItemId = (itemId == null || itemId.isBlank()) ? "<none>" : itemId;
            if (hasSelectedStyle && isDevToolsEnabled()) {
                LOG.info("[MOTM] Input trace(mouse): player="
                        + playerData.getPlayerName()
                        + " button=" + event.getMouseButton().mouseButtonType
                        + " item=" + loggedItemId
                        + " recognizedSpellbook=" + isSpellbookItemId(loggedItemId));
            }
            if (itemId == null || itemId.isBlank()) {
                return;
            }
            if (isSpellbookItemId(itemId)) {
                int slot = resolveSpellbookMouseSlot(event.getMouseButton().mouseButtonType);
                LOG.info("[MOTM] Spellbook mouse input: player="
                        + playerData.getPlayerName()
                        + " button=" + event.getMouseButton().mouseButtonType
                        + " slot=" + slot
                        + " item=" + itemId);
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

    /**
     * Entry point used by MotmSpellbookInteraction subclasses (custom item interaction codec).
     * Resolves player data and routes into the existing cast pipeline.
     * Phase 2 of CODEX_IMPLEMENTATION_PLAN_2026-05-13.md.
     */
    public void castSpellbookSlotFromInteraction(Player runtimePlayer, int slot) {
        if (runtimePlayer == null || slot <= 0) {
            return;
        }
        String playerId = getRuntimePlayerId(runtimePlayer);
        if (playerId == null) {
            return;
        }
        var playerData = playerDataManager.getOnlinePlayer(playerId);
        if (playerData == null) {
            return;
        }
        String response = tryCastSpellbookSlot(
                runtimePlayer,
                playerData,
                slot,
                "interaction:custom",
                null,
                null
        );
        if (response != null && !response.isBlank()) {
            runtimePlayer.sendMessage(com.hypixel.hytale.server.core.Message.raw(response));
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

    private int resolveSpellbookInteractSlot(InteractionType actionType) {
        if (actionType == null) {
            return 0;
        }

        String normalized = String.valueOf(actionType).toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "primary", "ability1", "ability_1" -> 1;
            case "secondary", "ability2", "ability_2" -> 2;
            case "use", "ability3", "ability_3" -> 3;
            default -> 0;
        };
    }

    private int resolveSpellbookMouseSlot(MouseButtonType mouseButtonType) {
        if (mouseButtonType == null) {
            return 0;
        }

        return switch (String.valueOf(mouseButtonType).toLowerCase(Locale.ROOT)) {
            case "left" -> 1;
            case "right" -> 2;
            default -> 0;
        };
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
        LOG.info("[MOTM] >>> handleDamageBlock ENTERED");
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
        String playerId = getRuntimePlayerId(terraMiner);
        var playerData = playerId != null ? playerDataManager.getOnlinePlayer(playerId) : null;
        String alloyResponse = gameplayPlaybackManager.handleAlloyToolUse(terraMiner, playerData, itemId);
        if (alloyResponse != null && !alloyResponse.isBlank()) {
            LOG.info(alloyResponse + " playerId=" + playerId);
            terraMiner.sendMessage(Message.raw(alloyResponse));
        }
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

    // --- Agent observability surface ---

    public String startObservabilityRun(String runId, String scenarioId, String playerId) {
        if (!devToolsEnabled) {
            return devToolsDisabledMessage();
        }
        if (observability == null) {
            return "[MOTM] Observability unavailable.";
        }
        var playerData = playerId == null ? null : playerDataManager.getOnlinePlayer(playerId);
        Map<String, Object> metadata = MotmObservability.mapOf(
                "buildChannel", getBuildChannel(),
                "internalTestBuild", isInternalTestBuild(),
                "devToolsEnabled", isDevToolsEnabled(),
                "pluginDirectory", pluginDirectory != null ? pluginDirectory.toString() : null,
                "playerId", playerId,
                "playerName", playerData != null ? playerData.getPlayerName() : null
        );
        return observability.startRun(runId, scenarioId, "motm-dev-command", metadata);
    }

    public String stopObservabilityRun(String reason) {
        if (observability == null) {
            return "[MOTM] Observability unavailable.";
        }
        return observability.stopRun(reason);
    }

    public String getObservabilityStatus() {
        if (observability == null) {
            return "[MOTM] Observability unavailable.";
        }
        return observability.status();
    }

    public String setObservabilityScenario(String scenarioId) {
        if (observability == null || !observability.isActive()) {
            return "[MOTM] Observability is not active.";
        }
        observability.setScenario(scenarioId);
        return "[MOTM] Observability scenario set: " + observability.getActiveScenarioId();
    }

    public String markObservabilityRun(String playerId, String label) {
        if (observability == null || !observability.isActive()) {
            return "[MOTM] Observability is not active.";
        }
        String traceId = observability.nextTraceId("marker");
        recordCausality("marker", traceId, MotmObservability.mapOf(
                "playerId", playerId,
                "label", label == null || label.isBlank() ? "marker" : label
        ));
        return "[MOTM] Observability marker: label="
                + (label == null || label.isBlank() ? "marker" : label)
                + " traceId=" + traceId;
    }

    public String snapshotObservability(String playerId, String label) {
        if (observability == null || !observability.isActive()) {
            return "[MOTM] Observability is not active.";
        }
        String traceId = observability.nextTraceId("snapshot");
        Map<String, Object> snapshot = buildObservabilitySnapshot(playerId, label);
        observability.recordServerTruth("snapshot", traceId, snapshot);
        return "[MOTM] Observability snapshot captured: label="
                + snapshot.getOrDefault("label", "snapshot")
                + " traceId=" + traceId
                + " runId=" + observability.getActiveRunId();
    }

    public void recordControl(String type, String traceId, Map<String, Object> data) {
        if (observability != null) {
            observability.recordControl(type, effectiveObservabilityTraceId(traceId), data);
        }
    }

    public void recordCausality(String type, String traceId, Map<String, Object> data) {
        if (observability != null) {
            observability.recordCausality(type, effectiveObservabilityTraceId(traceId), data);
        }
    }

    public void recordServerTruth(String type, String traceId, Map<String, Object> data) {
        if (observability != null) {
            observability.recordServerTruth(type, effectiveObservabilityTraceId(traceId), data);
        }
    }

    public void recordClientIntent(String type, String traceId, Map<String, Object> data) {
        if (observability != null) {
            String effectiveTraceId = effectiveObservabilityTraceId(traceId);
            if ((effectiveTraceId == null || effectiveTraceId.isBlank()) && observability.isActive()) {
                effectiveTraceId = observability.nextTraceId("client");
            }
            observability.recordClientIntent(type, effectiveTraceId, data);
        }
    }

    public String enterObservabilityTrace(String traceId) {
        String previous = observabilityTraceContext.get();
        if (traceId == null || traceId.isBlank()) {
            observabilityTraceContext.remove();
        } else {
            observabilityTraceContext.set(traceId);
        }
        return previous;
    }

    public void restoreObservabilityTrace(String previousTraceId) {
        if (previousTraceId == null || previousTraceId.isBlank()) {
            observabilityTraceContext.remove();
        } else {
            observabilityTraceContext.set(previousTraceId);
        }
    }

    public String currentObservabilityTraceId() {
        return effectiveObservabilityTraceId(null);
    }

    private String currentOrNewClientIntentTraceId() {
        String traceId = currentObservabilityTraceId();
        if ((traceId == null || traceId.isBlank()) && observability != null && observability.isActive()) {
            return observability.nextTraceId("client");
        }
        return traceId;
    }

    private String effectiveObservabilityTraceId(String traceId) {
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        return observabilityTraceContext.get();
    }

    private Map<String, Object> buildObservabilitySnapshot(String playerId, String label) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("label", label == null || label.isBlank() ? "snapshot" : label);
        snapshot.put("build", MotmObservability.mapOf(
                "buildChannel", getBuildChannel(),
                "internalTestBuild", isInternalTestBuild(),
                "devToolsEnabled", isDevToolsEnabled(),
                "packetScope", observability != null ? observability.getPacketScope() : null
        ));
        snapshot.put("pluginDirectory", pluginDirectory != null ? pluginDirectory.toString() : null);
        snapshot.put("pending", buildPendingSnapshot());
        snapshot.put("activeRuntime", gameplayPlaybackManager != null
                ? gameplayPlaybackManager.buildObservabilitySnapshot(playerId)
                : Map.of());

        Player runtimePlayer = getRuntimePlayer(playerId);
        PlayerData playerData = playerId == null ? null : playerDataManager.getOnlinePlayer(playerId);
        snapshot.put("playerData", buildPlayerDataSnapshot(playerData));
        snapshot.put("runtimePlayer", buildRuntimePlayerSnapshot(runtimePlayer));
        snapshot.put("statusEffects", buildStatusEffectsSnapshot(playerId));
        snapshot.put("inventory", buildInventorySnapshot(runtimePlayer));
        snapshot.put("trackedTargets", buildTrackedTargetsSnapshot(playerId));
        return snapshot;
    }

    private Map<String, Object> buildPendingSnapshot() {
        return MotmObservability.mapOf(
                "onlineRuntimePlayers", onlineRuntimePlayers.size(),
                "pendingSpellbookGrants", pendingSpellbookGrants.size(),
                "pendingDevBookGrants", pendingDevBookGrants.size(),
                "pendingStyleTestMobSpawns", pendingStyleTestMobSpawns.size(),
                "pendingStyleTestMobClears", pendingStyleTestMobClears.size(),
                "pendingStyleTestMobCounts", pendingStyleTestMobCounts.size(),
                "pendingStyleReviewResets", pendingStyleReviewResets.size(),
                "pendingProofRequests", pendingProofRequests.size(),
                "pendingDevRelocations", pendingDevRelocations.size(),
                "pendingAbilityCasts", pendingAbilityCasts.size(),
                "activeProofSelections", activeProofSelections.size(),
                "activeProofProxies", activeProofProxies.size(),
                "activeStyleTests", activeStyleTests.size(),
                "freeCastPlayers", freeCastPlayers.size()
        );
    }

    private Map<String, Object> buildPlayerDataSnapshot(PlayerData player) {
        if (player == null) {
            return Map.of("present", false);
        }
        return MotmObservability.mapOf(
                "present", true,
                "playerId", player.getPlayerId(),
                "playerName", player.getPlayerName(),
                "classId", player.getPlayerClass(),
                "raceId", player.getRace(),
                "selectedStyles", new ArrayList<>(player.getSelectedStyles()),
                "level", player.getLevel(),
                "currentXp", player.getCurrentXp(),
                "totalXpEarned", player.getTotalXpEarned(),
                "selectedPerkCount", player.getSelectedPerks().size(),
                "activeSynergyCount", player.getActiveSynergyBonuses().size(),
                "freeCast", isFreeCastEnabled(player.getPlayerId())
        );
    }

    private Map<String, Object> buildRuntimePlayerSnapshot(Player player) {
        if (player == null) {
            return Map.of("present", false);
        }

        Map<String, Object> runtime = new LinkedHashMap<>();
        runtime.put("present", true);
        runtime.put("playerId", getRuntimePlayerId(player));
        PlayerRef playerRef = getUniversePlayerRef(player);
        runtime.put("username", playerRef != null ? playerRef.getUsername() : null);
        runtime.put("uuid", playerRef != null && playerRef.getUuid() != null ? playerRef.getUuid().toString() : null);
        runtime.put("world", player.getWorld() != null ? player.getWorld().getName() : "unknown");
        runtime.put("gameMode", String.valueOf(player.getGameMode()));

        Ref<EntityStore> ref = player.getReference();
        runtime.put("ref", buildRefSnapshot(ref));
        Store<EntityStore> store = ref != null && ref.isValid() ? ref.getStore() : null;
        runtime.put("position", vectorSnapshot(getPlayerPosition(player)));
        runtime.put("forward", vectorSnapshot(normalizeHorizontal(getPlayerForward(player))));
        runtime.put("velocity", buildVelocitySnapshot(store, ref));
        runtime.put("movement", buildMovementSnapshot(store, ref));
        runtime.put("stats", buildStatsSnapshot(store, ref));
        runtime.put("nativeEntityEffects", buildNativeEntityEffectsSnapshot(store, ref));
        return runtime;
    }

    private Map<String, Object> buildRefSnapshot(Ref<EntityStore> ref) {
        if (ref == null) {
            return Map.of("present", false);
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("present", true);
        snapshot.put("valid", ref.isValid());
        snapshot.put("index", ref.isValid() ? ref.getIndex() : -1);
        try {
            Store<EntityStore> store = ref.isValid() ? ref.getStore() : null;
            UUIDComponent uuid = store != null ? store.getComponent(ref, UUIDComponent.getComponentType()) : null;
            snapshot.put("uuid", uuid != null && uuid.getUuid() != null ? uuid.getUuid().toString() : null);
        } catch (Throwable e) {
            snapshot.put("uuidError", e.getMessage());
        }
        return snapshot;
    }

    private Map<String, Object> buildVelocitySnapshot(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return Map.of("present", false);
        }
        Velocity velocity = store.getComponent(ref, Velocity.getComponentType());
        if (velocity == null || velocity.getVelocity() == null) {
            return Map.of("present", false);
        }
        Map<String, Object> snapshot = vectorSnapshot(velocity.getVelocity());
        snapshot.put("present", true);
        return snapshot;
    }

    private Map<String, Object> buildMovementSnapshot(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return Map.of("present", false);
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("present", true);

        MovementStatesComponent statesComponent = store.getComponent(ref, MovementStatesComponent.getComponentType());
        List<String> activeStates = new ArrayList<>();
        if (statesComponent != null && statesComponent.getMovementStates() != null) {
            var states = statesComponent.getMovementStates();
            if (states.idle) activeStates.add("idle");
            if (states.horizontalIdle) activeStates.add("horizontalIdle");
            if (states.jumping) activeStates.add("jumping");
            if (states.flying) activeStates.add("flying");
            if (states.walking) activeStates.add("walking");
            if (states.running) activeStates.add("running");
            if (states.sprinting) activeStates.add("sprinting");
            if (states.crouching) activeStates.add("crouching");
            if (states.falling) activeStates.add("falling");
            if (states.onGround) activeStates.add("onGround");
            if (states.swimming) activeStates.add("swimming");
            if (states.gliding) activeStates.add("gliding");
        }
        snapshot.put("states", activeStates);

        MovementManager movementManager = store.getComponent(ref, MovementManager.getComponentType());
        if (movementManager != null && movementManager.getSettings() != null) {
            var settings = movementManager.getSettings();
            snapshot.put("settings", MotmObservability.mapOf(
                    "baseSpeed", settings.baseSpeed,
                    "forwardWalkSpeedMultiplier", settings.forwardWalkSpeedMultiplier,
                    "strafeWalkSpeedMultiplier", settings.strafeWalkSpeedMultiplier,
                    "forwardRunSpeedMultiplier", settings.forwardRunSpeedMultiplier,
                    "strafeRunSpeedMultiplier", settings.strafeRunSpeedMultiplier,
                    "forwardSprintSpeedMultiplier", settings.forwardSprintSpeedMultiplier,
                    "minSpeedMultiplier", settings.minSpeedMultiplier,
                    "maxSpeedMultiplier", settings.maxSpeedMultiplier,
                    "acceleration", settings.acceleration,
                    "canFly", settings.canFly
            ));
        }
        return snapshot;
    }

    private Map<String, Object> buildStatsSnapshot(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return Map.of("present", false);
        }
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap == null) {
            return Map.of("present", false);
        }
        return MotmObservability.mapOf(
                "present", true,
                "health", statSnapshot(statMap, DefaultEntityStatTypes.getHealth()),
                "stamina", statSnapshot(statMap, DefaultEntityStatTypes.getStamina()),
                "mana", statSnapshot(statMap, DefaultEntityStatTypes.getMana()),
                "signatureEnergy", statSnapshot(statMap, DefaultEntityStatTypes.getSignatureEnergy())
        );
    }

    private Map<String, Object> statSnapshot(EntityStatMap statMap, int statType) {
        EntityStatValue value = statMap != null ? statMap.get(statType) : null;
        if (value == null) {
            return Map.of("present", false);
        }
        return MotmObservability.mapOf(
                "present", true,
                "id", value.getId(),
                "index", value.getIndex(),
                "current", value.get(),
                "min", value.getMin(),
                "max", value.getMax(),
                "modifierCount", value.getModifiers() != null ? value.getModifiers().size() : 0
        );
    }

    private List<Map<String, Object>> buildStatusEffectsSnapshot(String playerId) {
        if (playerId == null || statusEffectManager == null) {
            return List.of();
        }
        List<Map<String, Object>> effects = new ArrayList<>();
        for (StatusEffect effect : statusEffectManager.getEffects(playerId)) {
            if (effect == null) {
                continue;
            }
            effects.add(MotmObservability.mapOf(
                    "type", String.valueOf(effect.getType()),
                    "remainingTicks", effect.getRemainingTicks(),
                    "initialDurationTicks", effect.getInitialDurationTicks(),
                    "value", effect.getValue(),
                    "source", effect.getSourcePerkOrAbility(),
                    "expired", effect.isExpired()
            ));
        }
        return effects;
    }

    private List<Map<String, Object>> buildNativeEntityEffectsSnapshot(Store<EntityStore> store, Ref<EntityStore> ref) {
        if (store == null || ref == null || !ref.isValid()) {
            return List.of();
        }
        EffectControllerComponent controller = store.getComponent(ref, EffectControllerComponent.getComponentType());
        if (controller == null) {
            return List.of();
        }
        List<Map<String, Object>> effects = new ArrayList<>();
        ActiveEntityEffect[] activeEffects = controller.getAllActiveEntityEffects();
        if (activeEffects == null) {
            return effects;
        }
        for (ActiveEntityEffect effect : activeEffects) {
            if (effect == null) {
                continue;
            }
            EntityEffect asset = EntityEffect.getAssetMap().getAsset(effect.getEntityEffectIndex());
            effects.add(MotmObservability.mapOf(
                    "entityEffectIndex", effect.getEntityEffectIndex(),
                    "entityEffectId", asset != null ? asset.getId() : null,
                    "name", asset != null ? asset.getName() : null,
                    "initialDuration", effect.getInitialDuration(),
                    "remainingDuration", effect.getRemainingDuration(),
                    "infinite", effect.isInfinite(),
                    "debuff", effect.isDebuff(),
                    "invulnerable", effect.isInvulnerable()
            ));
        }
        return effects;
    }

    private Map<String, Object> buildInventorySnapshot(Player player) {
        CombinedItemContainer inventory = getCombinedPlayerInventory(player);
        if (inventory == null) {
            return Map.of("present", false);
        }
        Map<String, Integer> itemCounts = new LinkedHashMap<>();
        inventory.forEach((slot, stack) -> {
            if (stack == null || stack.getItemId() == null || stack.getItemId().isBlank()) {
                return;
            }
            itemCounts.merge(stack.getItemId(), Math.max(0, stack.getQuantity()), Integer::sum);
        });
        return MotmObservability.mapOf(
                "present", true,
                "uniqueItemIds", itemCounts.size(),
                "itemCounts", itemCounts
        );
    }

    private List<Map<String, Object>> buildTrackedTargetsSnapshot(String playerId) {
        if (playerId == null) {
            return List.of();
        }
        List<Ref<EntityStore>> targets = styleTestTargetsByPlayer.get(playerId);
        if (targets == null || targets.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Ref<EntityStore> target : targets) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("ref", buildRefSnapshot(target));
            Store<EntityStore> store = target != null && target.isValid() ? target.getStore() : null;
            NPCEntity npc = store != null ? store.getComponent(target, NPCEntity.getComponentType()) : null;
            row.put("npc", npc == null
                    ? Map.of("present", false)
                    : MotmObservability.mapOf(
                    "present", true,
                    "roleName", npc.getRoleName(),
                    "npcTypeId", npc.getNPCTypeId(),
                    "despawning", npc.isDespawning(),
                    "despawnTime", npc.getDespawnTime()
            ));
            row.put("position", vectorSnapshot(getEntityPosition(store, target)));
            row.put("stats", buildStatsSnapshot(store, target));
            rows.add(row);
        }
        return rows;
    }

    private Map<String, Object> vectorSnapshot(Vector3d vector) {
        if (vector == null) {
            return Map.of("present", false);
        }
        return MotmObservability.mapOf(
                "present", true,
                "x", vector.x,
                "y", vector.y,
                "z", vector.z
        );
    }

    // --- Getters for inter-manager access ---

    public DataLoader getDataLoader() { return dataLoader; }
    public PerkManager getPerkManager() { return perkManager; }
    public PlayerStatModifierManager getPlayerStatModifierManager() { return playerStatModifierManager; }
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
    public MotmObservability getObservability() { return observability; }
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
            pendingFreeCastInvulnerabilityClears.remove(playerId);
        } else {
            freeCastPlayers.remove(playerId);
            lastObservedFreeCastHealthByPlayer.remove(playerId);
            pendingFreeCastInvulnerabilityClears.add(playerId);
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

    private record TemporaryProofSelection(
            String proofId,
            World world,
            Vector3i anchor,
            BlockSelection originalSelection,
            long cleanupAtMillis
    ) {}

    private record TemporaryProofProxy(
            String proofId,
            World world,
            Ref<EntityStore> ref,
            long cleanupAtMillis
    ) {}

    private record FluidResolution(
            String fluidId,
            int fluidTypeId
    ) {}

    private record BlockResolution(
            String blockId,
            int blockTypeId
    ) {}

    private record StyleLookup(
            String classId,
            StyleData style
    ) {}

    private record TerraReviewKitItem(
            String itemId,
            int quantity,
            String purpose
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
