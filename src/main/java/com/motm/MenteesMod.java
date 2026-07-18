package com.motm;

import com.motm.command.MotmCommand;
import com.motm.command.MotmCommandAuth;
import com.motm.command.MotmDevCommandInbox;
import com.motm.command.MotmDevCommandInboxProcessor;
import com.motm.config.MotmPluginDataDirectories;
import com.motm.config.MotmServerConfig;
import com.motm.interaction.BlockDamageInteractionHandler;
import com.motm.interaction.SpellbookInputHandler;
import com.motm.lifecycle.MotmHydroRecipeRegistrar;
import com.motm.lifecycle.MotmLifecycleRegistrar;
import com.motm.lifecycle.MotmPacketObservabilityRegistrar;
import com.motm.lifecycle.MotmSpellbookCodecRegistrar;
import com.motm.manager.*;
import com.motm.model.AbilityData;
import com.motm.model.Perk;
import com.motm.model.PerkTriggerBinding;
import com.motm.model.PlayerData;
import com.motm.model.ScaledMobResult;
import com.motm.model.StatusEffect;
import com.motm.observability.MotmObservabilityActions;
import com.motm.observability.MotmObservabilityEvents;
import com.motm.observability.MotmObservabilitySnapshotBuilder;
import com.motm.proof.MotmProofActions;
import com.motm.proof.MotmProofCleanupProcessor;
import com.motm.proof.MotmProofRuntime;
import com.motm.resource.HydroContainerItems;
import com.motm.resource.HydroInventoryBridge;
import com.motm.resource.HydroContainerRefillHandler;
import com.motm.resource.SpellbookInventoryItems;
import com.motm.resource.SpellbookInventoryKit;
import com.motm.resource.TerraInventoryResourceBridge;
import com.motm.resource.TerraReviewInventoryKit;
import com.motm.runtime.MotmRuntimeTasks;
import com.motm.runtime.MotmRuntimeLoop;
import com.motm.runtime.PendingAbilityCast;
import com.motm.runtime.player.MobSpawnRuntimeActions;
import com.motm.runtime.player.PlayerCombatLifecycleActions;
import com.motm.runtime.player.PlayerProgressionRuntimeActions;
import com.motm.runtime.player.PerkTriggerRuntimeActions;
import com.motm.runtime.player.PlayerRuntimeRebuildActions;
import com.motm.runtime.player.PlayerSessionLifecycleActions;
import com.motm.runtime.player.RuntimePlayerView;
import com.motm.runtime.task.AbilityCastCommandActions;
import com.motm.runtime.task.AbilityCastRuntimeTaskProcessor;
import com.motm.runtime.task.AbilityTestRuntimeTaskProcessor;
import com.motm.runtime.task.DevRuntimeCommandActions;
import com.motm.runtime.task.DevPlayerTestActions;
import com.motm.runtime.task.DevRuntimeTaskProcessor;
import com.motm.runtime.task.FreeCastCommandActions;
import com.motm.runtime.task.FreeCastSafetyProcessor;
import com.motm.runtime.task.InventoryCommandActions;
import com.motm.runtime.task.InventoryRuntimeTaskProcessor;
import com.motm.runtime.task.PlayerMaintenanceRuntimeTaskProcessor;
import com.motm.runtime.task.ProofRuntimeTaskProcessor;
import com.motm.runtime.task.RuntimeTaskProcessorRegistry;
import com.motm.runtime.task.StyleReviewRuntimeTaskProcessor;
import com.motm.runtime.task.StatusHudRuntimeTaskProcessor;
import com.motm.runtime.task.StyleTestCommandActions;
import com.motm.runtime.task.StyleTestMobActions;
import com.motm.runtime.task.StyleTestMobRuntimeTaskProcessor;
import com.motm.runtime.task.StyleTestSequenceRuntimeTaskProcessor;
import com.motm.runtime.task.StyleTestTargetResolver;
import com.motm.runtime.task.TerraReviewRuntimeTaskProcessor;
import com.motm.runtime.state.FreeCastRuntimeState;
import com.motm.runtime.state.StyleTestRuntimeState;
import com.motm.runtime.state.ProofCleanupRuntimeState;
import com.motm.runtime.state.TargetHealthRuntimeState;
import com.motm.runtime.state.PerkTriggerRuntimeState;
import com.motm.runtime.state.RuntimePlayerRegistry;
import com.motm.runtime.state.StatusHudRuntimeState;
import com.motm.ui.MotmStatusHudActions;
import com.motm.ui.SpellbookPageActions;
import com.motm.util.DataLoader;
import com.motm.util.MotmObservability;
import com.motm.util.MotmPlayerInventory;
import com.motm.util.MotmPreflightAudit;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerInteractEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerMouseButtonEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.protocol.GameMode;
import com.hypixel.hytale.protocol.InteractionType;

import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
    private static final boolean CUSTOM_PAGE_UI_ENABLED = true;
    private static final boolean CUSTOM_HUD_ENABLED = true;
    private static final int HUD_REFRESH_INTERVAL_TICKS = 4;
    private static final int HUD_INSTALL_DELAY_TICKS = 4;
    private static final long SPELLBOOK_INPUT_DEBOUNCE_MS = 150L;
    private static final String BLACKSMITH_METADATA_KEY = "motm_blacksmith_armor";
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
    private HydroInventoryBridge hydroInventoryBridge;
    private HydroContainerRefillHandler hydroContainerRefillHandler;
    private ClassPassiveManager classPassiveManager;
    private RuntimePerkManager runtimePerkManager;
    private StyleManager styleManager;
    private StyleTestCommandActions styleTestCommandActions;
    private ElementalReactionManager elementalReactionManager;
    private SpellbookManager spellbookManager;
    private BookInteractionManager bookInteractionManager;
    private GameplayPlaybackManager gameplayPlaybackManager;
    private BlockDamageInteractionHandler blockDamageInteractionHandler;
    private SpellbookInputHandler spellbookInputHandler;
    private MobSpawnRuntimeActions mobSpawnActions;
    private MotmObservability observability;
    private final MotmObservabilityEvents observabilityEvents =
            new MotmObservabilityEvents(() -> observability);
    private final MotmLifecycleRegistrar lifecycleRegistrar = new MotmLifecycleRegistrar(this);
    private final MotmPacketObservabilityRegistrar packetObservabilityRegistrar = new MotmPacketObservabilityRegistrar();
    private final MotmHydroRecipeRegistrar hydroRecipeRegistrar = new MotmHydroRecipeRegistrar();
    private final MotmSpellbookCodecRegistrar spellbookCodecRegistrar = new MotmSpellbookCodecRegistrar();
    private final MotmDevCommandInbox devCommandInbox = new MotmDevCommandInbox();
    private final MotmDevCommandInboxProcessor devCommandInboxProcessor =
            new MotmDevCommandInboxProcessor(
                    devCommandInbox,
                    new MotmDevCommandInboxProcessor.Hooks() {
                        @Override
                        public boolean devToolsEnabled() {
                            return devToolsEnabled;
                        }

                        @Override
                        public boolean commandAvailable() {
                            return motmCommand != null;
                        }

                        @Override
                        public Path pluginDirectory() {
                            return pluginDirectory;
                        }

                        @Override
                        public Iterable<Player> runtimePlayers() {
                            return onlineRuntimePlayers.players();
                        }

                        @Override
                        public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                            return MenteesMod.this.isPlayerInStore(player, currentStore);
                        }

                        @Override
                        public String nextTraceId(String prefix) {
                            return observability != null ? observability.nextTraceId(prefix) : null;
                        }

                        @Override
                        public String enterTrace(String traceId) {
                            return enterObservabilityTrace(traceId);
                        }

                        @Override
                        public void restoreTrace(String previousTraceId) {
                            restoreObservabilityTrace(previousTraceId);
                        }

                        @Override
                        public String execute(Player player, String[] args) {
                            return motmCommand.execute(player, args);
                        }

                        @Override
                        public void recordControl(String type, String traceId, Map<String, Object> data) {
                            MenteesMod.this.recordControl(type, traceId, data);
                        }
                    },
                    LOG
            );
    private boolean devToolsEnabled = false;
    private final StatusHudRuntimeState statusHuds = new StatusHudRuntimeState();
    private final RuntimePlayerRegistry onlineRuntimePlayers = new RuntimePlayerRegistry();
    private final RuntimePlayerView runtimePlayerView = new RuntimePlayerView(onlineRuntimePlayers, LOG);
    private final PerkTriggerRuntimeActions perkTriggerActions =
            new PerkTriggerRuntimeActions(new PerkTriggerRuntimeState(), LOG);
    private final MotmRuntimeTasks runtimeTasks = new MotmRuntimeTasks();
    private final AbilityCastCommandActions abilityCastCommandActions =
            new AbilityCastCommandActions(runtimeTasks, LOG);
    private final PlayerCombatLifecycleActions playerCombatLifecycleActions =
            new PlayerCombatLifecycleActions(new PlayerCombatLifecycleActions.Hooks() {
                @Override
                public PlayerData playerData(String playerId) {
                    return playerDataManager != null ? playerDataManager.getOnlinePlayer(playerId) : null;
                }

                @Override
                public Player runtimePlayer(String playerId) {
                    return onlineRuntimePlayers.get(playerId);
                }

                @Override
                public void levelingMobKilled(PlayerData player, String mobType, int mobLevel, boolean isRare) {
                    if (levelingManager != null) {
                        levelingManager.onMobKilled(player, mobType, mobLevel, isRare);
                    }
                }

                @Override
                public void resourceMobKilled(String playerId, String playerClass) {
                    if (resourceManager != null) {
                        resourceManager.onMobKilled(playerId, playerClass);
                    }
                }

                @Override
                public void classPassiveMobKilled(PlayerData player, Player runtimePlayer, String mobEntityId) {
                    if (classPassiveManager != null) {
                        classPassiveManager.onMobKilled(player, runtimePlayer, mobEntityId);
                    }
                }
                @Override
                public void afterMobKilled(PlayerData player, Player runtimePlayer, String mobEntityId) {
                    LOG.info("[MOTM] afterMobKilled purge fired: mobEntityId=" + mobEntityId
                            + " killer=" + (player != null ? player.getPlayerId() : "unknown"));
                    if (mobEntityId != null && !mobEntityId.isBlank()) {
                        if (statusEffectManager != null) {
                            statusEffectManager.clearAllForEntity(mobEntityId);
                        }
                        if (elementalReactionManager != null) {
                            elementalReactionManager.clearAllForEntity(mobEntityId);
                        }
                        if (runtimePerkManager != null) {
                            runtimePerkManager.clearForEntity(mobEntityId);
                        }
                        Ref<EntityStore> killerRef = runtimePlayer != null ? runtimePlayer.getReference() : null;
                        Store<EntityStore> killerStore = killerRef != null && killerRef.isValid()
                                ? killerRef.getStore() : null;
                        if (gameplayPlaybackManager != null && killerStore != null) {
                            gameplayPlaybackManager.purgePulsesTargetingEntity(mobEntityId, killerStore);
                        }
                    }
                    if (runtimePerkManager != null) {
                        runtimePerkManager.afterMobKilled(player, runtimePlayer, mobEntityId);
                    }
                }


                @Override
                public void applyKillTriggers(String playerId, Player runtimePlayer) {
                    perkTriggerActions.applyKillTriggers(playerId, runtimePlayer);
                }

                @Override
                public void checkAchievements(PlayerData player, String event, Object context) {
                    if (playerDataManager != null) {
                        playerDataManager.checkAchievements(player, event, context);
                    }
                }

                @Override
                public void refreshPlayerProgressionBonuses(String playerId) {
                    MenteesMod.this.refreshPlayerProgressionBonuses(playerId);
                }

                @Override
                public void refreshStatusHud(String playerId) {
                    MenteesMod.this.refreshStatusHud(playerId);
                }

                @Override
                public void classPassivePlayerDeath(String playerId) {
                    if (classPassiveManager != null) {
                        classPassiveManager.onPlayerDeath(playerId);
                    }
                }

                @Override
                public void clearStatusEffects(String playerId) {
                    if (statusEffectManager != null) {
                        statusEffectManager.clearEffects(playerId);
                    }
                }

                @Override
                public void clearElementalMarks(String playerId) {
                    if (elementalReactionManager != null) {
                        elementalReactionManager.clearMarks(playerId);
                    }
                }

                @Override
                public void clearArmedStomp(String playerId) {
                    if (gameplayPlaybackManager != null) {
                        gameplayPlaybackManager.clearArmedStomp(playerId);
                    }
                }
            });
    private final PlayerSessionLifecycleActions playerSessionLifecycleActions =
            new PlayerSessionLifecycleActions(new PlayerSessionLifecycleActions.Hooks() {
                @Override
                public PlayerData playerDataOnJoin(String playerId, String playerName) {
                    return playerDataManager != null ? playerDataManager.onPlayerJoin(playerId, playerName) : null;
                }

                @Override
                public PlayerData playerData(String playerId) {
                    return playerDataManager != null ? playerDataManager.getOnlinePlayer(playerId) : null;
                }

                @Override
                public void playerDataOnDisconnect(String playerId) {
                    if (playerDataManager != null) {
                        playerDataManager.onPlayerDisconnect(playerId);
                    }
                }

                @Override
                public PlayerSessionLifecycleActions.RuntimePlayerIdentity runtimePlayerIdentity(Player runtimePlayer) {
                    PlayerRef playerRef = getUniversePlayerRef(runtimePlayer);
                    if (playerRef == null || playerRef.getUuid() == null) {
                        return null;
                    }
                    String worldName = runtimePlayer != null && runtimePlayer.getWorld() != null
                            ? runtimePlayer.getWorld().getName()
                            : "unknown";
                    return new PlayerSessionLifecycleActions.RuntimePlayerIdentity(
                            playerRef.getUuid().toString(),
                            playerRef.getUsername(),
                            worldName,
                            String.valueOf(runtimePlayer)
                    );
                }

                @Override
                public void putRuntimePlayer(String playerId, Player runtimePlayer) {
                    onlineRuntimePlayers.put(playerId, runtimePlayer);
                }

                @Override
                public boolean markRuntimePlayerInitialized(String playerId) {
                    return onlineRuntimePlayers.markInitialized(playerId);
                }

                @Override
                public boolean hasRuntimePlayer(String playerId) {
                    return onlineRuntimePlayers.contains(playerId);
                }

                @Override
                public void removeRuntimePlayer(String playerId) {
                    onlineRuntimePlayers.remove(playerId);
                }

                @Override
                public void updateRestedOnLogin(PlayerData player) {
                    if (levelingManager != null) {
                        levelingManager.updateRestedOnLogin(player);
                    }
                }

                @Override
                public void updateRestedOnLogout(PlayerData player) {
                    if (levelingManager != null) {
                        levelingManager.updateRestedOnLogout(player);
                    }
                }

                @Override
                public void reapplyPerks(PlayerData player) {
                    if (perkManager != null) {
                        perkManager.reapplyAllPerks(player, synergyEngine);
                    }
                }

                @Override
                public void synchronizePersistentResourceState(PlayerData player) {
                    if (resourceManager != null) {
                        resourceManager.synchronizePersistentState(player);
                    }
                }

                @Override
                public void initializeResources(PlayerData player) {
                    if (resourceManager != null) {
                        resourceManager.initializeForPlayer(player);
                    }
                }

                @Override
                public void queueHydroContainerSync(String playerId) {
                    MenteesMod.this.queueHydroContainerSync(playerId);
                }

                @Override
                public void onClassPassivePlayerJoin(PlayerData player) {
                    if (classPassiveManager != null) {
                        classPassiveManager.onPlayerJoin(player);
                    }
                }

                @Override
                public void clearClassPassiveState(String playerId) {
                    if (classPassiveManager != null) {
                        classPassiveManager.clearPlayerState(playerId);
                    }
                }

                @Override
                public boolean hasPendingPerkSelection(PlayerData player) {
                    return perkManager != null && perkManager.hasPendingPerkSelection(player);
                }

                @Override
                public int pendingPerkSelectionTier(PlayerData player) {
                    return perkManager != null ? perkManager.getPendingSelectionTier(player) : 0;
                }

                @Override
                public void rebuildPlayerRuntimeNow(PlayerData player) {
                    MenteesMod.this.rebuildPlayerRuntimeNow(player);
                }

                @Override
                public boolean ensureSpellbookItem(Player runtimePlayer) {
                    return MenteesMod.this.ensureSpellbookItem(runtimePlayer);
                }

                @Override
                public boolean playerHasSpellbook(Player runtimePlayer) {
                    return MenteesMod.this.playerHasSpellbook(runtimePlayer);
                }

                @Override
                public void queueSpellbookGrant(String playerId) {
                    MenteesMod.this.queueSpellbookGrant(playerId);
                }

                @Override
                public void refreshPlayerProgressionBonuses(String playerId) {
                    MenteesMod.this.refreshPlayerProgressionBonuses(playerId);
                }

                @Override
                public boolean devToolsEnabled() {
                    return MenteesMod.this.isDevToolsEnabled();
                }

                @Override
                public void clearStatusEffects(String playerId) {
                    if (statusEffectManager != null) {
                        statusEffectManager.clearEffects(playerId);
                    }
                }

                @Override
                public void clearElementalMarks(String playerId) {
                    if (elementalReactionManager != null) {
                        elementalReactionManager.clearMarks(playerId);
                    }
                }

                @Override
                public void setFreeCastEnabled(String playerId, boolean enabled) {
                    MenteesMod.this.setFreeCastEnabled(playerId, enabled);
                }

                @Override
                public void queueStatusHudInstall(String playerId) {
                    MenteesMod.this.queueStatusHudInstall(playerId);
                }

                @Override
                public void styleOnPlayerDisconnect(String playerId) {
                    if (styleManager != null) {
                        styleManager.onPlayerDisconnect(playerId);
                    }
                }

                @Override
                public void resourceOnPlayerDisconnect(String playerId) {
                    if (resourceManager != null) {
                        resourceManager.onPlayerDisconnect(playerId);
                    }
                }

                @Override
                public void clearStatModifiersOrPerkTriggers(String playerId) {
                    if (playerStatModifierManager != null) {
                        playerStatModifierManager.clearForPlayer(playerId);
                    } else {
                        clearPerkTriggers(playerId);
                    }
                }

                @Override
                public void clearStatusHud(String playerId) {
                    statusHudActions.clearPlayer(playerId);
                }

                @Override
                public void clearRuntimeTasks(String playerId) {
                    runtimeTasks.clearPlayer(playerId);
                }

                @Override
                public void clearStyleTestRuntime(String playerId) {
                    styleTestRuntimeState.clearPlayer(playerId);
                }

                @Override
                public void clearArmedStomp(String playerId) {
                    if (gameplayPlaybackManager != null) {
                        gameplayPlaybackManager.clearArmedStomp(playerId);
                    }
                }

                @Override
                public void clearPlayerProgression(String playerId) {
                    playerProgressionActions.clearPlayer(playerId);
                }

                @Override
                public void clearFreeCastState(String playerId) {
                    freeCastRuntimeState.clearPlayer(playerId);
                }

                @Override
                public void clearSpellbookInput(String playerId) {
                    if (spellbookInputHandler != null) {
                        spellbookInputHandler.clearPlayer(playerId);
                    }
                }

                @Override
                public void recordCausality(String type, Map<String, Object> data) {
                    MenteesMod.this.recordCausality(type, null, data);
                }

                @Override
                public long nowMs() {
                    return System.currentTimeMillis();
                }
            }, LOG);
    private final DevRuntimeCommandActions devRuntimeCommandActions = new DevRuntimeCommandActions(
            this::isDevToolsEnabled,
            this::devToolsDisabledMessage,
            onlineRuntimePlayers,
            runtimeTasks,
            LOG
    );
    private final InventoryCommandActions inventoryCommandActions = new InventoryCommandActions(
            this::isDevToolsEnabled,
            this::devToolsDisabledMessage,
            onlineRuntimePlayers,
            runtimePlayerView,
            runtimeTasks
    );
    private final SpellbookPageActions spellbookPageActions = new SpellbookPageActions(
            CUSTOM_PAGE_UI_ENABLED,
            this,
            new SpellbookPageActions.Hooks() {
                @Override
                public PlayerRef universePlayerRef(Player player) {
                    return getUniversePlayerRef(player);
                }

                @Override
                public void recordClientIntent(String type, String traceId, Map<String, Object> data) {
                    MenteesMod.this.recordClientIntent(type, traceId, data);
                }
            }
    );
    private final MotmStatusHudActions statusHudActions = new MotmStatusHudActions(
            CUSTOM_HUD_ENABLED,
            HUD_INSTALL_DELAY_TICKS,
            statusHuds,
            runtimeTasks,
            this,
            new MotmStatusHudActions.Hooks() {
                @Override
                public PlayerRef universePlayerRef(Player player) {
                    return getUniversePlayerRef(player);
                }

                @Override
                public PlayerData playerData(String playerId) {
                    return playerDataManager != null ? playerDataManager.getOnlinePlayer(playerId) : null;
                }

                @Override
                public Player runtimePlayer(String playerId) {
                    return getRuntimePlayer(playerId);
                }

                @Override
                public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                    return MenteesMod.this.isPlayerInStore(player, currentStore);
                }

                @Override
                public String currentOrNewClientIntentTraceId() {
                    return MenteesMod.this.currentOrNewClientIntentTraceId();
                }

                @Override
                public String enterTrace(String traceId) {
                    return enterObservabilityTrace(traceId);
                }

                @Override
                public void restoreTrace(String previousTraceId) {
                    restoreObservabilityTrace(previousTraceId);
                }

                @Override
                public void recordClientIntent(String type, String traceId, Map<String, Object> data) {
                    MenteesMod.this.recordClientIntent(type, traceId, data);
                }
            },
            LOG
    );
    private final StyleTestRuntimeState styleTestRuntimeState = new StyleTestRuntimeState();
    private final StyleTestTargetResolver styleTestTargetResolver = new StyleTestTargetResolver();
    private final StyleTestMobActions styleTestMobActions = new StyleTestMobActions(styleTestRuntimeState, LOG);
    private final SpellbookInventoryKit spellbookInventoryKit = new SpellbookInventoryKit(
            new SpellbookInventoryKit.Hooks() {
                @Override
                public CombinedItemContainer combinedInventory(Player player) {
                    return getCombinedPlayerInventory(player);
                }

                @Override
                public boolean devToolsEnabled() {
                    return devToolsEnabled;
                }

                @Override
                public void sendMessage(Player player, Message message) {
                    MenteesMod.this.sendPlayerMessage(player, message);
                }
            },
            LOG
    );
    private final TerraReviewInventoryKit terraReviewInventoryKit = new TerraReviewInventoryKit(
            LOG,
            new TerraReviewInventoryKit.Hooks() {
                @Override
                public boolean ensureSpellbookItem(Player player) {
                    return MenteesMod.this.ensureSpellbookItem(player);
                }

                @Override
                public boolean ensureDevBookItem(Player player) {
                    return MenteesMod.this.ensureDevBookItem(player);
                }

                @Override
                public String runtimePlayerId(Player player) {
                    return MenteesMod.this.getRuntimePlayerId(player);
                }
            }
    );
    private final MotmProofRuntime proofRuntime = new MotmProofRuntime();
    private final ProofCleanupRuntimeState proofCleanupRuntimeState = new ProofCleanupRuntimeState();
    private final MotmProofCleanupProcessor proofCleanupProcessor =
            new MotmProofCleanupProcessor(proofCleanupRuntimeState, LOG);
    private final MotmObservabilitySnapshotBuilder observabilitySnapshots = new MotmObservabilitySnapshotBuilder(
            new MotmObservabilitySnapshotBuilder.Hooks() {
                @Override
                public String buildChannel() {
                    return getBuildChannel();
                }

                @Override
                public boolean internalTestBuild() {
                    return isInternalTestBuild();
                }

                @Override
                public boolean devToolsEnabled() {
                    return isDevToolsEnabled();
                }

                @Override
                public String packetScope() {
                    return observability != null ? observability.getPacketScope() : null;
                }

                @Override
                public Path pluginDirectory() {
                    return pluginDirectory;
                }

                @Override
                public Map<String, Object> runtimeTasksSnapshot() {
                    return runtimeTasks.snapshot();
                }

                @Override
                public int onlineRuntimePlayerCount() {
                    return onlineRuntimePlayers.size();
                }

                @Override
                public int activeProofSelections() {
                    return proofCleanupRuntimeState.selectionCount();
                }

                @Override
                public int activeProofProxies() {
                    return proofCleanupRuntimeState.proxyCount();
                }

                @Override
                public int activeStyleTests() {
                    return styleTestRuntimeState.activeCount();
                }

                @Override
                public int freeCastPlayerCount() {
                    return freeCastRuntimeState.enabledCount();
                }

                @Override
                public Map<String, Object> activeRuntimeSnapshot(String playerId) {
                    return gameplayPlaybackManager != null
                            ? gameplayPlaybackManager.buildObservabilitySnapshot(playerId)
                            : Map.of();
                }

                @Override
                public Player runtimePlayer(String playerId) {
                    return getRuntimePlayer(playerId);
                }

                @Override
                public PlayerData playerData(String playerId) {
                    return playerDataManager != null ? playerDataManager.getOnlinePlayer(playerId) : null;
                }

                @Override
                public boolean freeCastEnabled(String playerId) {
                    return isFreeCastEnabled(playerId);
                }

                @Override
                public List<StatusEffect> statusEffects(String playerId) {
                    return statusEffectManager == null ? List.of() : statusEffectManager.getEffects(playerId);
                }

                @Override
                public CombinedItemContainer combinedInventory(Player player) {
                    return getCombinedPlayerInventory(player);
                }

                @Override
                public List<Ref<EntityStore>> trackedTargets(String playerId) {
                    return styleTestRuntimeState.targets(playerId);
                }

                @Override
                public String runtimePlayerId(Player player) {
                    return getRuntimePlayerId(player);
                }

                @Override
                public PlayerRef universePlayerRef(Player player) {
                    return getUniversePlayerRef(player);
                }
            }
    );
    private final MotmObservabilityActions observabilityActions = new MotmObservabilityActions(
            observabilitySnapshots,
            new MotmObservabilityActions.Hooks() {
                @Override
                public boolean devToolsEnabled() {
                    return MenteesMod.this.isDevToolsEnabled();
                }

                @Override
                public String devToolsDisabledMessage() {
                    return MenteesMod.this.devToolsDisabledMessage();
                }

                @Override
                public MotmObservability observability() {
                    return observability;
                }

                @Override
                public PlayerData playerData(String playerId) {
                    return playerDataManager != null ? playerDataManager.getOnlinePlayer(playerId) : null;
                }

                @Override
                public String buildChannel() {
                    return getBuildChannel();
                }

                @Override
                public boolean internalTestBuild() {
                    return isInternalTestBuild();
                }

                @Override
                public Path pluginDirectory() {
                    return pluginDirectory;
                }
            }
    );
    private final MotmProofActions proofActions = new MotmProofActions(
            proofCleanupRuntimeState,
            styleTestRuntimeState::targets,
            new MotmProofActions.Hooks() {
                @Override
                public void recordClientIntent(String type, Map<String, Object> data) {
                    MenteesMod.this.recordClientIntent(type, null, data);
                }

                @Override
                public void recordServerTruth(String type, Map<String, Object> data) {
                    MenteesMod.this.recordServerTruth(type, null, data);
                }

                @Override
                public List<Map<String, Object>> nativeEntityEffectsSnapshot(Store<EntityStore> store, Ref<EntityStore> ref) {
                    return observabilitySnapshots.nativeEntityEffectsSnapshot(store, ref);
                }
            },
            LOG
    );
    private final RuntimeTaskProcessorRegistry runtimeTaskProcessors = createRuntimeTaskProcessors();
    private final FreeCastRuntimeState freeCastRuntimeState = new FreeCastRuntimeState();
    private final FreeCastCommandActions freeCastCommandActions =
            new FreeCastCommandActions(freeCastRuntimeState, runtimeTasks);
    private final DevPlayerTestActions devPlayerTestActions = new DevPlayerTestActions(LOG);
    private final FreeCastSafetyProcessor freeCastSafetyProcessor = new FreeCastSafetyProcessor(
            freeCastRuntimeState,
            new FreeCastSafetyProcessor.Hooks() {
                @Override
                public Player runtimePlayer(String playerId) {
                    return onlineRuntimePlayers.get(playerId);
                }

                @Override
                public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                    return MenteesMod.this.isPlayerInStore(player, currentStore);
                }

                @Override
                public PlayerData playerData(String playerId) {
                    return playerDataManager != null ? playerDataManager.getOnlinePlayer(playerId) : null;
                }

                @Override
                public boolean hasStatusEffect(String playerId, StatusEffect.Type type) {
                    return statusEffectManager != null && statusEffectManager.hasEffect(playerId, type);
                }

                @Override
                public void removeStatusEffect(String playerId, StatusEffect.Type type) {
                    if (statusEffectManager != null) {
                        statusEffectManager.removeEffect(playerId, type);
                    }
                }

                @Override
                public void sendMessage(Player player, Message message) {
                    MenteesMod.this.sendPlayerMessage(player, message);
                }
            },
            LOG
    );
    private final PlayerRuntimeRebuildActions playerRuntimeRebuildActions = new PlayerRuntimeRebuildActions(
            new PlayerRuntimeRebuildActions.Hooks() {
                @Override
                public void resetCooldowns(String playerId) {
                    styleManager.resetCooldowns(playerId);
                }

                @Override
                public void clearClassPassiveState(String playerId) {
                    classPassiveManager.clearPlayerState(playerId);
                }

                @Override
                public void clearStatusEffects(String playerId) {
                    statusEffectManager.clearEffects(playerId);
                }

                @Override
                public void clearElementalMarks(String playerId) {
                    elementalReactionManager.clearMarks(playerId);
                }

                @Override
                public void clearArmedStomp(String playerId) {
                    gameplayPlaybackManager.clearArmedStomp(playerId);
                }

                @Override
                public void clearResourceState(String playerId) {
                    resourceManager.clearPlayerState(playerId);
                }

                @Override
                public void synchronizePersistentResourceState(PlayerData player) {
                    resourceManager.synchronizePersistentState(player);
                }

                @Override
                public void refreshProgressionBonusesNow(String playerId) {
                    MenteesMod.this.refreshPlayerProgressionBonusesNow(playerId);
                }

                @Override
                public boolean freeCastEnabled(String playerId) {
                    return MenteesMod.this.isFreeCastEnabled(playerId);
                }

                @Override
                public void clearFreeCastInvulnerability(String playerId) {
                    freeCastSafetyProcessor.clearInvulnerability(playerId);
                }

                @Override
                public void refreshStatusHudNow(String playerId) {
                    MenteesMod.this.refreshStatusHudNow(playerId);
                }

                @Override
                public void initializeResources(PlayerData player) {
                    resourceManager.initializeForPlayer(player);
                }

                @Override
                public void reapplyPerks(PlayerData player) {
                    perkManager.reapplyAllPerks(player, synergyEngine);
                }

                @Override
                public void queueHydroContainerSync(String playerId) {
                    MenteesMod.this.queueHydroContainerSync(playerId);
                }

                @Override
                public void onClassPassivePlayerJoin(PlayerData player) {
                    classPassiveManager.onPlayerJoin(player);
                }

                @Override
                public Player runtimePlayer(String playerId) {
                    return onlineRuntimePlayers.get(playerId);
                }

                @Override
                public void ensureFreeCastInvulnerability(Player runtimePlayer) {
                    freeCastSafetyProcessor.ensureInvulnerability(runtimePlayer);
                }
            },
            LOG
    );
    private final PlayerProgressionRuntimeActions playerProgressionActions = new PlayerProgressionRuntimeActions(
            new TargetHealthRuntimeState(),
            new PlayerProgressionRuntimeActions.Hooks() {
                @Override
                public Player runtimePlayer(String playerId) {
                    return onlineRuntimePlayers.get(playerId);
                }

                @Override
                public PlayerData playerData(String playerId) {
                    return playerDataManager != null ? playerDataManager.getOnlinePlayer(playerId) : null;
                }

                @Override
                public Iterable<Map.Entry<String, Player>> onlineRuntimePlayers() {
                    return onlineRuntimePlayers.entries();
                }

                @Override
                public java.util.Collection<PlayerData> allOnlinePlayers() {
                    return playerDataManager != null ? playerDataManager.getAllOnlinePlayers() : List.of();
                }

                @Override
                public int averageOnlineLevel(java.util.Collection<PlayerData> players) {
                    return levelingManager != null ? levelingManager.calculateAverageOnlineLevel(players) : 1;
                }

                @Override
                public com.motm.model.ClassData classData(String classId) {
                    return dataLoader != null ? dataLoader.getClassData(classId) : null;
                }

                @Override
                public boolean freeCastEnabled(String playerId) {
                    return isFreeCastEnabled(playerId);
                }

                @Override
                public boolean isScalingCategory(String category) {
                    return mobScalingManager != null && mobScalingManager.isScalingCategory(category);
                }

                @Override
                public boolean isBossCategory(String category) {
                    return mobScalingManager != null && mobScalingManager.isBossCategory(category);
                }

                @Override
                public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                    return MenteesMod.this.isPlayerInStore(player, currentStore);
                }
            },
            LOG
    );
    private final MotmRuntimeLoop runtimeLoop = new MotmRuntimeLoop(
            HUD_REFRESH_INTERVAL_TICKS,
            1000L,
            new MotmRuntimeLoop.Hooks() {
                @Override
                public Map<String, Double> tickStatusEffects() {
                    return statusEffectManager != null ? statusEffectManager.tickAll() : Map.of();
                }

                @Override
                public void tickElementalReactions() {
                    if (elementalReactionManager != null) {
                        elementalReactionManager.tickAll();
                    }
                }

                @Override
                public void tickStyleCooldowns() {
                    if (styleManager != null) {
                        styleManager.tickCooldowns();
                    }
                }

                @Override
                public void tickResources() {
                    if (resourceManager != null) {
                        resourceManager.tick();
                    }
                }

                @Override
                public void processRuntimeTask(String id, Store<EntityStore> currentStore) {
                    runtimeTaskProcessors.process(id, currentStore);
                }

                @Override
                public void processFreeCastSafety(Store<EntityStore> currentStore) {
                    freeCastSafetyProcessor.process(currentStore);
                }

                @Override
                public void tickClassPassives(Store<EntityStore> currentStore) {
                    if (classPassiveManager != null) {
                        classPassiveManager.tick(onlineRuntimePlayers.snapshot(), currentStore);
                    }
                }
                @Override
                public void tickRuntimePerks(Store<EntityStore> currentStore) {
                    if (runtimePerkManager == null) {
                        return;
                    }
                    for (Map.Entry<String, Player> entry : onlineRuntimePlayers.snapshot().entrySet()) {
                        Player runtimePlayer = entry.getValue();
                        Ref<EntityStore> playerRef = runtimePlayer != null ? runtimePlayer.getReference() : null;
                        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() != currentStore) {
                            continue;
                        }
                        PlayerData player = playerDataManager != null
                                ? playerDataManager.getOnlinePlayer(entry.getKey())
                                : null;
                        runtimePerkManager.onPlayerTick(
                                player,
                                runtimePlayer,
                                playerRef,
                                currentStore,
                                0L
                        );
                    }
                }


                @Override
                public void processDevCommandInbox(Store<EntityStore> currentStore) {
                    MenteesMod.this.processDevCommandInbox(currentStore);
                }

                @Override
                public void processActiveProofCleanups(Store<EntityStore> currentStore) {
                    MenteesMod.this.processActiveProofCleanups(currentStore);
                }

                @Override
                public void tickArmedStomps(Store<EntityStore> currentStore) {
                    if (gameplayPlaybackManager != null) {
                        gameplayPlaybackManager.tickArmedStomps(currentStore);
                    }
                }

                @Override
                public void tickGameplayPlayback(Store<EntityStore> currentStore) {
                    if (gameplayPlaybackManager != null) {
                        gameplayPlaybackManager.tick(currentStore);
                    }
                }

                @Override
                public void refreshAllStatusHuds(Store<EntityStore> currentStore) {
                    MenteesMod.this.refreshAllStatusHuds(currentStore);
                }

                @Override
                public boolean observabilityActive() {
                    return observability != null && observability.isActive();
                }

                @Override
                public int onlineRuntimePlayerCount() {
                    return onlineRuntimePlayers.size();
                }

                @Override
                public Map<String, Object> runtimeTasksSnapshot() {
                    return runtimeTasks.snapshot();
                }

                @Override
                public int activeProofSelections() {
                    return proofCleanupRuntimeState.selectionCount();
                }

                @Override
                public int activeProofProxies() {
                    return proofCleanupRuntimeState.proxyCount();
                }

                @Override
                public int activeStyleTests() {
                    return styleTestRuntimeState.activeCount();
                }

                @Override
                public int trackedStyleTargetOwners() {
                    return styleTestRuntimeState.targetOwnerCount();
                }

                @Override
                public void recordCausality(String type, Map<String, Object> data) {
                    MenteesMod.this.recordCausality(type, null, data);
                }

                @Override
                public void logFine(String message) {
                    LOG.fine(message);
                }

                @Override
                public void logInfo(String message) {
                    LOG.info(message);
                }

                @Override
                public long nowMs() {
                    return System.currentTimeMillis();
                }
            }
    );
    private volatile MotmPreflightAudit.AuditReport lastPreflightAudit;
    private final java.util.concurrent.atomic.AtomicBoolean preflightWorldRerunPending =
            new java.util.concurrent.atomic.AtomicBoolean(true);

    // Plugin data directory
    private Path pluginDirectory;

    private RuntimeTaskProcessorRegistry createRuntimeTaskProcessors() {
        return new RuntimeTaskProcessorRegistry()
                .register(new InventoryRuntimeTaskProcessor(
                        runtimeTasks,
                        new InventoryRuntimeTaskProcessor.Hooks() {
                            @Override
                            public Player runtimePlayer(String playerId) {
                                return onlineRuntimePlayers.get(playerId);
                            }

                            @Override
                            public PlayerData playerData(String playerId) {
                                return playerDataManager != null ? playerDataManager.getOnlinePlayer(playerId) : null;
                            }

                            @Override
                            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                                return MenteesMod.this.isPlayerInStore(player, currentStore);
                            }

                            @Override
                            public boolean ensureSpellbookItem(Player player) {
                                return MenteesMod.this.ensureSpellbookItem(player);
                            }

                            @Override
                            public boolean ensureDevBookItem(Player player) {
                                return MenteesMod.this.ensureDevBookItem(player);
                            }

                            @Override
                            public boolean playerHasSpellbook(Player player) {
                                return MenteesMod.this.playerHasSpellbook(player);
                            }

                            @Override
                            public boolean playerHasDevBook(Player player) {
                                return MenteesMod.this.playerHasDevBook(player);
                            }

                            @Override
                            public void syncHydroContainerItem(Player player, PlayerData playerData, boolean notify) {
                                if (hydroInventoryBridge != null) {
                                    hydroInventoryBridge.syncContainerItem(player, playerData, notify);
                                }
                            }

                            @Override
                            public void sendMessage(Player player, String message) {
                                if (player != null && message != null && !message.isBlank()) {
                                    sendPlayerMessage(player, Message.raw(message));
                                }
                            }
                        },
                        LOG
                ))
                .register(new StatusHudRuntimeTaskProcessor(
                        runtimeTasks,
                        new StatusHudRuntimeTaskProcessor.Hooks() {
                            @Override
                            public Player runtimePlayer(String playerId) {
                                return onlineRuntimePlayers.get(playerId);
                            }

                            @Override
                            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                                return MenteesMod.this.isPlayerInStore(player, currentStore);
                            }

                            @Override
                            public void installStatusHud(Player player) {
                                MenteesMod.this.installStatusHud(player);
                            }

                            @Override
                            public void refreshStatusHudNow(String playerId) {
                                MenteesMod.this.refreshStatusHudNow(playerId);
                            }
                        },
                        LOG
                ))
                .register(new DevRuntimeTaskProcessor(
                        runtimeTasks,
                        new DevRuntimeTaskProcessor.Hooks() {
                            @Override
                            public Player runtimePlayer(String playerId) {
                                return onlineRuntimePlayers.get(playerId);
                            }

                            @Override
                            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                                return MenteesMod.this.isPlayerInStore(player, currentStore);
                            }

                            @Override
                            public String relocateRuntimePlayerForTesting(String playerId, Player player, String target) {
                                return devPlayerTestActions.relocate(player, target);
                            }

                            @Override
                            public String applyDevGameModeChange(Player player, GameMode gameMode) {
                                return devPlayerTestActions.applyGameMode(player, gameMode);
                            }

                            @Override
                            public void sendMessage(Player player, String message) {
                                if (player != null && message != null && !message.isBlank()) {
                                    sendPlayerMessage(player, Message.raw(message));
                                }
                            }
                        },
                        LOG
                ))
                .register(new ProofRuntimeTaskProcessor(
                        runtimeTasks,
                        new ProofRuntimeTaskProcessor.Hooks() {
                            @Override
                            public Player runtimePlayer(String playerId) {
                                return onlineRuntimePlayers.get(playerId);
                            }

                            @Override
                            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                                return MenteesMod.this.isPlayerInStore(player, currentStore);
                            }

                            @Override
                            public String nextProofTraceId() {
                                return observability != null ? observability.nextTraceId("proof") : null;
                            }

                            @Override
                            public String enterTrace(String traceId) {
                                return MenteesMod.this.enterObservabilityTrace(traceId);
                            }

                            @Override
                            public void restoreTrace(String previousTraceId) {
                                MenteesMod.this.restoreObservabilityTrace(previousTraceId);
                            }

                            @Override
                            public String runProofNow(String playerId, Player player, Store<EntityStore> currentStore, String proofId) {
                                return MenteesMod.this.runProofNow(playerId, player, currentStore, proofId);
                            }

                            @Override
                            public void recordCausality(String event, String traceId, String playerId, String proofId, String result) {
                                Map<String, Object> payload = new LinkedHashMap<>();
                                payload.put("playerId", playerId);
                                payload.put("proofId", proofId);
                                if (result != null) {
                                    payload.put("result", result);
                                }
                                MenteesMod.this.recordCausality(event, traceId, payload);
                            }

                            @Override
                            public void sendMessage(Player player, String message) {
                                if (player != null && message != null && !message.isBlank()) {
                                    sendPlayerMessage(player, Message.raw(message));
                                }
                            }
                        },
                        LOG
                ))
                .register(new PlayerMaintenanceRuntimeTaskProcessor(
                        runtimeTasks,
                        new PlayerMaintenanceRuntimeTaskProcessor.Hooks() {
                            @Override
                            public Player runtimePlayer(String playerId) {
                                return onlineRuntimePlayers.get(playerId);
                            }

                            @Override
                            public PlayerData playerData(String playerId) {
                                return playerDataManager != null ? playerDataManager.getOnlinePlayer(playerId) : null;
                            }

                            @Override
                            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                                return MenteesMod.this.isPlayerInStore(player, currentStore);
                            }

                            @Override
                            public void clearFreeCastInvulnerability(String playerId) {
                                freeCastSafetyProcessor.clearInvulnerability(playerId);
                            }

                            @Override
                            public void rebuildPlayerRuntimeNow(PlayerData playerData) {
                                MenteesMod.this.rebuildPlayerRuntimeNow(playerData);
                            }

                            @Override
                            public void refreshPlayerProgressionBonusesNow(String playerId) {
                                MenteesMod.this.refreshPlayerProgressionBonusesNow(playerId);
                            }

                            @Override
                            public String clearRuntimeEntityEffectsForDev(String playerId, Store<EntityStore> currentStore) {
                                return MenteesMod.this.clearRuntimeEntityEffectsForDev(playerId, currentStore);
                            }
                        }
                ))
                .register(new AbilityTestRuntimeTaskProcessor(
                        runtimeTasks,
                        new AbilityTestRuntimeTaskProcessor.Hooks() {
                            @Override
                            public Player runtimePlayer(String playerId) {
                                return onlineRuntimePlayers.get(playerId);
                            }

                            @Override
                            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                                return MenteesMod.this.isPlayerInStore(player, currentStore);
                            }

                            @Override
                            public List<Ref<EntityStore>> styleTestTargets(String playerId) {
                                return styleTestRuntimeState.targets(playerId);
                            }

                            @Override
                            public Vector3d entityPosition(Store<EntityStore> currentStore, Ref<EntityStore> ref) {
                                return runtimePlayerView.entityPosition(currentStore, ref);
                            }

                            @Override
                            public void queueAbilityCast(String playerId,
                                                         String abilityId,
                                                         Ref<EntityStore> targetRef,
                                                         Vector3i targetBlock,
                                                         boolean notifyFailures) {
                                MenteesMod.this.queueAbilityCast(playerId, abilityId, targetRef, targetBlock, notifyFailures);
                            }
                        },
                        LOG
                ))
                .register(new StyleTestSequenceRuntimeTaskProcessor(
                        styleTestRuntimeState,
                        styleTestTargetResolver,
                        new StyleTestSequenceRuntimeTaskProcessor.Hooks() {
                            @Override
                            public Player runtimePlayer(String playerId) {
                                return onlineRuntimePlayers.get(playerId);
                            }

                            @Override
                            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                                return MenteesMod.this.isPlayerInStore(player, currentStore);
                            }

                            @Override
                            public PlayerData playerData(String playerId) {
                                return playerDataManager != null ? playerDataManager.getOnlinePlayer(playerId) : null;
                            }

                            @Override
                            public AbilityData findAbility(PlayerData playerData, String abilityId) {
                                return styleManager != null ? styleManager.findAbility(playerData, abilityId) : null;
                            }

                            @Override
                            public double castTimeSeconds(AbilityData ability) {
                                return styleManager != null ? styleManager.getCastTimeSeconds(ability) : 0.0;
                            }

                            @Override
                            public double recoveryTimeSeconds(AbilityData ability) {
                                return styleManager != null ? styleManager.getRecoveryTimeSeconds(ability) : 0.0;
                            }

                            @Override
                            public void queueAbilityCast(String playerId,
                                                         String abilityId,
                                                         Ref<EntityStore> targetRef,
                                                         Vector3i targetBlock,
                                                         boolean notifyFailures) {
                                MenteesMod.this.queueAbilityCast(playerId, abilityId, targetRef, targetBlock, notifyFailures);
                            }

                            @Override
                            public void recordServerTruth(String type, Map<String, Object> data) {
                                MenteesMod.this.recordServerTruth(type, null, data);
                            }

                            @Override
                            public void sendMessage(Player player, String message) {
                                if (player != null && message != null && !message.isBlank()) {
                                    sendPlayerMessage(player, Message.raw(message));
                                }
                            }
                        }
                ))
                .register(new AbilityCastRuntimeTaskProcessor(
                        runtimeTasks,
                        new AbilityCastRuntimeTaskProcessor.Hooks() {
                            @Override
                            public Player runtimePlayer(String playerId) {
                                return onlineRuntimePlayers.get(playerId);
                            }

                            @Override
                            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                                return MenteesMod.this.isPlayerInStore(player, currentStore);
                            }

                            @Override
                            public String executeQueuedAbilityCast(PendingAbilityCast request, Player player) {
                                return motmCommand.executeQueuedAbilityCast(
                                        request.playerId(),
                                        request.abilityId(),
                                        player,
                                        request.targetRef(),
                                        request.targetBlock()
                                );
                            }

                            @Override
                            public boolean devToolsEnabled() {
                                return MenteesMod.this.isDevToolsEnabled();
                            }

                            @Override
                            public void sendMessage(Player player, String message) {
                                if (player != null && message != null && !message.isBlank()) {
                                    sendPlayerMessage(player, Message.raw(message));
                                }
                            }
                        },
                        LOG
                ))
                .register(new StyleReviewRuntimeTaskProcessor(
                        runtimeTasks,
                        new StyleReviewRuntimeTaskProcessor.Hooks() {
                            @Override
                            public Player runtimePlayer(String playerId) {
                                return onlineRuntimePlayers.get(playerId);
                            }

                            @Override
                            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                                return MenteesMod.this.isPlayerInStore(player, currentStore);
                            }

                            @Override
                            public String clearStyleTestMobsNow(String playerId, Store<EntityStore> currentStore, Player player) {
                                return styleTestMobActions.clear(playerId, currentStore, player);
                            }

                            @Override
                            public String resetReviewRuntime(String playerId, Store<EntityStore> currentStore, Player player) {
                                return gameplayPlaybackManager.resetReviewRuntime(playerId, currentStore, player);
                            }

                            @Override
                            public String scrubStyleReviewArena(Player player) {
                                return styleTestMobActions.scrubArena(player);
                            }

                            @Override
                            public void clearStatusEffects(String playerId) {
                                statusEffectManager.clearEffects(playerId);
                            }

                            @Override
                            public void clearElementalMarks(String playerId) {
                                elementalReactionManager.clearMarks(playerId);
                            }

                            @Override
                            public void resetCooldowns(String playerId) {
                                styleManager.resetCooldowns(playerId);
                            }

                            @Override
                            public void setFreeCastEnabled(String playerId, boolean enabled) {
                                MenteesMod.this.setFreeCastEnabled(playerId, enabled);
                            }

                            @Override
                            public void clearActiveStyleTest(String playerId) {
                                styleTestRuntimeState.stop(playerId);
                            }

                            @Override
                            public void sendMessage(Player player, String message) {
                                if (player != null && message != null && !message.isBlank()) {
                                    sendPlayerMessage(player, Message.raw(message));
                                }
                            }
                        },
                        LOG
                ))
                .register(new StyleTestMobRuntimeTaskProcessor(
                        runtimeTasks,
                        new StyleTestMobRuntimeTaskProcessor.Hooks() {
                            @Override
                            public Player runtimePlayer(String playerId) {
                                return onlineRuntimePlayers.get(playerId);
                            }

                            @Override
                            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                                return MenteesMod.this.isPlayerInStore(player, currentStore);
                            }

                            @Override
                            public String spawnStyleTestMobsNow(String playerId, Player runtimePlayer, String mode) {
                                return styleTestMobActions.spawn(playerId, runtimePlayer, mode);
                            }

                            @Override
                            public String clearStyleTestMobsNow(String playerId, Store<EntityStore> currentStore, Player player) {
                                return styleTestMobActions.clear(playerId, currentStore, player);
                            }

                            @Override
                            public String countStyleTestMobsNow(String playerId, Store<EntityStore> currentStore, Player player) {
                                return styleTestMobActions.count(playerId, currentStore, player);
                            }

                            @Override
                            public int countTrackedStyleTestTargets(String playerId) {
                                return styleTestMobActions.countTracked(playerId);
                            }

                            @Override
                            public void recordServerTruth(String type, Map<String, Object> data) {
                                MenteesMod.this.recordServerTruth(type, null, data);
                            }

                            @Override
                            public void sendMessage(Player player, String message) {
                                if (player != null && message != null && !message.isBlank()) {
                                    sendPlayerMessage(player, Message.raw(message));
                                }
                            }
                        }
                ))
                .register(new TerraReviewRuntimeTaskProcessor(
                        runtimeTasks,
                        new TerraReviewRuntimeTaskProcessor.Hooks() {
                            @Override
                            public Player runtimePlayer(String playerId) {
                                return onlineRuntimePlayers.get(playerId);
                            }

                            @Override
                            public boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
                                return MenteesMod.this.isPlayerInStore(player, currentStore);
                            }

                            @Override
                            public String grantReviewKit(Player player) {
                                return terraReviewInventoryKit.grant(player);
                            }

                            @Override
                            public String cleanReviewInventory(Player player) {
                                return terraReviewInventoryKit.clean(player);
                            }

                            @Override
                            public void sendMessage(Player player, String message) {
                                if (player != null && message != null && !message.isBlank()) {
                                    sendPlayerMessage(player, Message.raw(message));
                                }
                            }
                        }
                ));
    }

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
                        .getAsset(SpellbookInventoryItems.DEFAULT_SPELLBOOK_ITEM_ID) != null;
        LOG.info("[MOTM] Spellbook item asset resolved=" + spellbookAssetResolved
                + " itemId=" + SpellbookInventoryItems.DEFAULT_SPELLBOOK_ITEM_ID);
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
        Path operationalDataDir = MotmPluginDataDirectories.resolveOperationalDataDirectory(dataDir, LOG);
        this.pluginDirectory = operationalDataDir;
        this.observability = new MotmObservability(operationalDataDir);
        runtimeTasks.setEvidenceSink(this::recordRuntimeTaskEvidence);
        loadServerConfig();

        LOG.info("========================================");
        LOG.info("  Mentees of the Mystical v1.0.0");
        LOG.info("  4 Classes | 40 Styles | 20 Shared Perks | Level 1-200");
        LOG.info("  Build Channel: " + MotmBuildInfo.BUILD_CHANNEL);
        LOG.info("========================================");

        // Initialize data loader and load all JSON data
        dataLoader = new DataLoader(operationalDataDir);
        dataLoader.loadAll();

        // Initialize managers (order matters â€” dependencies)
        synergyEngine = new SynergyEngine(dataLoader);
        playerStatModifierManager = new PlayerStatModifierManager(this);
        perkManager = new PerkManager(dataLoader, playerStatModifierManager);
        levelingManager = new LevelingManager(dataLoader, perkManager);
        mobScalingManager = new MobScalingManager(dataLoader);
        playerDataManager = new PlayerDataManager(operationalDataDir, dataLoader);
        mobSpawnActions = new MobSpawnRuntimeActions(
                playerDataManager,
                dataLoader,
                mobScalingManager,
                playerProgressionActions,
                LOG
        );

        // Phase 1 managers
        statusEffectManager = new StatusEffectManager();
        resourceManager = new ResourceManager();
        resourceManager.setTerraInventoryBridge(new TerraInventoryResourceBridge(
                this::getRuntimePlayer,
                resourceManager::add,
                LOG
        ));
        hydroInventoryBridge = new HydroInventoryBridge(
                this::getRuntimePlayer,
                resourceManager::getWaterContainerInfo,
                this::sendPlayerMessage,
                LOG
        );
        hydroContainerRefillHandler = new HydroContainerRefillHandler(
                resourceManager,
                playerDataManager,
                this::refreshStatusHud,
                this::sendPlayerMessage
        );
        resourceManager.setHydroInventoryBridge(hydroInventoryBridge);
        classPassiveManager = new ClassPassiveManager(
                dataLoader,
                playerDataManager,
                statusEffectManager,
                resourceManager
        );
        runtimePerkManager = new RuntimePerkManager(this);
        styleManager = new StyleManager(dataLoader, resourceManager, classPassiveManager, this::isFreeCastEnabled);
        elementalReactionManager = new ElementalReactionManager(dataLoader, statusEffectManager);
        spellbookManager = new SpellbookManager(
                dataLoader,
                levelingManager,
                styleManager,
                perkManager,
                classPassiveManager
        );
        bookInteractionManager = new BookInteractionManager(this);
        gameplayPlaybackManager = new GameplayPlaybackManager(this);
        styleTestCommandActions = new StyleTestCommandActions(
                this::isDevToolsEnabled,
                this::devToolsDisabledMessage,
                onlineRuntimePlayers,
                playerDataManager,
                dataLoader,
                styleManager,
                runtimeTasks,
                styleTestRuntimeState,
                styleTestTargetResolver,
                gameplayPlaybackManager,
                this::rebuildPlayerRuntime,
                this::refreshStatusHud,
                this::setFreeCastEnabled,
                LOG
        );
        blockDamageInteractionHandler = new BlockDamageInteractionHandler(
                new BlockDamageInteractionHandler.Support() {
                    @Override
                    public Iterable<Map.Entry<String, Player>> onlineRuntimePlayers() {
                        return onlineRuntimePlayers.entries();
                    }

                    @Override
                    public PlayerData playerData(String playerId) {
                        return playerDataManager != null ? playerDataManager.getOnlinePlayer(playerId) : null;
                    }

                    @Override
                    public String runtimePlayerId(Player player) {
                        return MenteesMod.this.getRuntimePlayerId(player);
                    }

                    @Override
                    public String handleAlloyToolUse(Player player, PlayerData playerData, String itemId) {
                        return gameplayPlaybackManager == null
                                ? ""
                                : gameplayPlaybackManager.handleAlloyToolUse(player, playerData, itemId);
                    }

                    @Override
                    public boolean handleBareHandBlockPunch(PlayerData playerData, Player player, DamageBlockEvent event) {
                        return runtimePerkManager != null
                                && runtimePerkManager.handleBareHandBlockPunch(playerData, player, event);
                    }

                    @Override
                    public void sendMessage(Player player, Message message) {
                        MenteesMod.this.sendPlayerMessage(player, message);
                    }
                },
                LOG
        );
        spellbookInputHandler = new SpellbookInputHandler(
                hydroContainerRefillHandler,
                SPELLBOOK_INPUT_DEBOUNCE_MS,
                new SpellbookInputHandler.Support() {
                    @Override
                    public String runtimePlayerId(Player player) {
                        return MenteesMod.this.getRuntimePlayerId(player);
                    }

                    @Override
                    public PlayerData playerData(String playerId) {
                        return playerDataManager != null ? playerDataManager.getOnlinePlayer(playerId) : null;
                    }

                    @Override
                    public boolean isSpellbookItem(ItemStack stack) {
                        return MenteesMod.this.isSpellbookItem(stack);
                    }

                    @Override
                    public boolean isDevBookItem(ItemStack stack) {
                        return MenteesMod.this.isDevBookItem(stack);
                    }

                    @Override
                    public boolean isSpellbookItemId(String itemId) {
                        return MenteesMod.this.isSpellbookItemId(itemId);
                    }

                    @Override
                    public boolean isDevBookItemId(String itemId) {
                        return MenteesMod.this.isDevBookItemId(itemId);
                    }

                    @Override
                    public boolean isPlayerCrouching(Player player) {
                        return MenteesMod.this.isPlayerCrouching(player);
                    }

                    @Override
                    public boolean devToolsEnabled() {
                        return MenteesMod.this.isDevToolsEnabled();
                    }

                    @Override
                    public String devToolsDisabledMessage() {
                        return MenteesMod.this.devToolsDisabledMessage();
                    }

                    @Override
                    public boolean openSpellbook(Player player, SpellbookManager.Section section) {
                        return MenteesMod.this.openSpellbook(player, section);
                    }

                    @Override
                    public String openSpellbookPage(PlayerData playerData) {
                        return bookInteractionManager.openSpellbook(playerData);
                    }

                    @Override
                    public String cycleDevPage(PlayerData playerData) {
                        return bookInteractionManager.cycleDevPage(playerData);
                    }

                    @Override
                    public String handleDevBookAction(PlayerData playerData, int slot) {
                        return bookInteractionManager.handleDevBookAction(playerData, slot);
                    }

                    @Override
                    public String handleSpellbookAction(PlayerData playerData, int slot) {
                        return bookInteractionManager.handleSpellbookAction(playerData, slot);
                    }

                    @Override
                    public String castAbilityBySlot(Player player,
                                                    int slot,
                                                    Ref<EntityStore> targetRef,
                                                    Vector3i targetBlock) {
                        return motmCommand.castAbilityBySlot(player, slot, targetRef, targetBlock);
                    }

                    @Override
                    public String handleWeaponFollowUpHit(Player player,
                                                          PlayerData playerData,
                                                          Ref<EntityStore> targetRef,
                                                          String itemId) {
                        return gameplayPlaybackManager == null
                                ? ""
                                : gameplayPlaybackManager.handleWeaponFollowUpHit(player, playerData, targetRef, itemId);
                    }

                    @Override
                    public void sendMessage(Player player, Message message) {
                        MenteesMod.this.sendPlayerMessage(player, message);
                    }
                },
                LOG
        );

        // Initialize command handler
        motmCommand = new MotmCommand(this);
        initializeProofRunners();
        registerSpellbookInteractionCodecs();
        lastPreflightAudit = MotmPreflightAudit.run(this);
    }

    private void loadServerConfig() {
        try {
            MotmServerConfig config = MotmServerConfig.loadOrCreate(pluginDirectory);
            devToolsEnabled = config.devToolsEnabled();
            if (observability != null) {
                observability.setPacketScope(config.observabilityPacketScope());
            }
            LOG.info("[MOTM] Dev tools " + (isDevToolsEnabled() ? "enabled" : "disabled")
                    + " via " + config.path().getFileName());
            LOG.info("[MOTM] Observability packet scope="
                    + (observability != null ? observability.getPacketScope() : "unavailable"));
        } catch (IOException e) {
            devToolsEnabled = false;
            if (observability != null) {
                observability.setPacketScope(MotmServerConfig.disabled(pluginDirectory).observabilityPacketScope());
            }
            LOG.warning("[MOTM] Failed to load server config. Dev tools disabled. " + e.getMessage());
        }
    }

    private void registerHytaleHooks() {
        lifecycleRegistrar.register(
                LOG,
                this::onPlayerConnect,
                this::onPlayerReady,
                this::onPlayerDisconnect,
                this::handleDamageBlock,
                this::handlePlayerInteract,
                this::handlePlayerMouseButton
        );
    }

    private void deregisterHytaleHooks() {
        lifecycleRegistrar.unregister(LOG);
    }

    private void registerObservabilityPacketWatchers() {
        packetObservabilityRegistrar.register(
                LOG,
                MotmBuildInfo.INTERNAL_TEST_BUILD,
                observability,
                this::currentObservabilityTraceId
        );
    }

    private void deregisterObservabilityPacketWatchers() {
        packetObservabilityRegistrar.unregister(LOG);
    }

    private void registerSpellbookInteractionCodecs() {
        spellbookCodecRegistrar.register(
                this,
                getCodecRegistry(com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.CODEC),
                LOG
        );
    }

    /**
     * Called when the plugin is disabled (server shutdown).
     */
    public void onDisable() {
        deregisterObservabilityPacketWatchers();
        deregisterHytaleHooks();
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
        playerSessionLifecycleActions.onPlayerJoin(playerId, playerName);
    }

    public void onPlayerConnect(Player runtimePlayer) {
        playerSessionLifecycleActions.onPlayerConnect(runtimePlayer);
    }

    public void onPlayerReady(Player runtimePlayer) {
        if (preflightWorldRerunPending.compareAndSet(true, false)) {
            // Setup-time preflight cannot see world asset maps; re-audit once
            // now that the registry is loaded so manifest validation is real.
            runPreflightAudit();
        }
        playerSessionLifecycleActions.onPlayerReady(runtimePlayer);
    }

    /**
     * Called when a player disconnects.
     */
    public void onPlayerDisconnect(String playerId) {
        playerSessionLifecycleActions.onPlayerDisconnect(playerId);
    }

    /**
     * Called when a mob is killed by a player.
     */
    public void onMobKilled(String playerId, String mobType, int mobLevel, boolean isRare) {
        onMobKilled(playerId, null, mobType, mobLevel, isRare);
    }

    public void onMobKilled(String playerId, String mobEntityId, String mobType, int mobLevel, boolean isRare) {
        playerCombatLifecycleActions.onMobKilled(playerId, mobEntityId, mobType, mobLevel, isRare);
    }

    public void registerPerkTrigger(String playerId, Perk perk, Perk.Effect effect) {
        perkTriggerActions.register(playerId, perk, effect);
    }

    public void clearPerkTriggers(String playerId) {
        perkTriggerActions.clear(playerId);
    }

    public List<PerkTriggerBinding> getPerkTriggers(String playerId, String type) {
        return perkTriggerActions.get(playerId, type);
    }

    /**
     * Called when a player dies.
     */
    public void onPlayerDeath(String playerId) {
        playerCombatLifecycleActions.onPlayerDeath(playerId);
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
        return mobSpawnActions == null
                ? null
                : mobSpawnActions.scale(mobType, playerId, zoneId, isNight, isBloodMoon, isDungeon);
    }

    /**
     * Called once per server tick.
     *
     * This keeps the runtime systems advancing even before we wire the actual
     * Hytale damage/entity APIs.
     */
    public void onServerTick(Store<EntityStore> currentStore) {
        runtimeLoop.tick(currentStore);
    }

    private void processDevCommandInbox(Store<EntityStore> currentStore) {
        devCommandInboxProcessor.process(currentStore);
    }

    public boolean openSpellbook(Player sender, SpellbookManager.Section section) {
        return spellbookPageActions.open(sender, section);
    }

    public boolean isSpellbookItem(ItemStack stack) {
        return SpellbookInventoryItems.isSpellbookItem(stack);
    }

    public boolean isSpellbookItemId(String itemId) {
        return SpellbookInventoryItems.isSpellbookItemId(itemId);
    }

    public boolean playerHasSpellbook(Player player) {
        return SpellbookInventoryItems.hasSpellbook(getCombinedPlayerInventory(player));
    }

    public boolean isDevBookItem(ItemStack stack) {
        return SpellbookInventoryItems.isDevBookItem(stack);
    }

    public boolean isDevBookItemId(String itemId) {
        return SpellbookInventoryItems.isDevBookItemId(itemId);
    }

    public boolean playerHasDevBook(Player player) {
        return SpellbookInventoryItems.hasDevBook(getCombinedPlayerInventory(player));
    }

    public boolean playerHasHydroContainer(Player player) {
        return HydroContainerItems.hasContainer(getCombinedPlayerInventory(player));
    }

    public boolean hasHydroContainerInInventory(String playerId) {
        return hydroInventoryBridge != null && hydroInventoryBridge.hasHydroContainer(playerId);
    }

    public boolean isHydroContainerItem(ItemStack stack) {
        return HydroContainerItems.isContainerItem(stack);
    }

    public boolean isHydroContainerItemId(String itemId) {
        return HydroContainerItems.isContainerItemId(itemId);
    }

    public String getHydroContainerItemId(int tier) {
        return HydroContainerItems.itemId(tier);
    }

    public int getHydroContainerTierFromInventory(String playerId) {
        return hydroInventoryBridge == null ? 0 : hydroInventoryBridge.getHydroContainerTier(playerId);
    }

    public void queueHydroContainerSync(String playerId) {
        inventoryCommandActions.queueHydroContainerSync(playerId);
    }

    public boolean ensureSpellbookItem(Player player) {
        return spellbookInventoryKit.ensureSpellbookItem(player);
    }

    public boolean ensureDevBookItem(Player player) {
        return spellbookInventoryKit.ensureDevBookItem(player);
    }

    public String queueSpellbookGrant(Player player) {
        return inventoryCommandActions.queueSpellbookGrant(player);
    }

    public String queueSpellbookGrant(String playerId) {
        return inventoryCommandActions.queueSpellbookGrant(playerId);
    }

    public String queueDevBookGrant(Player player) {
        return inventoryCommandActions.queueDevBookGrant(player);
    }

    public String queueDevBookGrant(String playerId) {
        return inventoryCommandActions.queueDevBookGrant(playerId);
    }

    public void queueAbilityCast(String playerId,
                                 String abilityId,
                                 com.hypixel.hytale.component.Ref<EntityStore> targetRef,
                                 Vector3i targetBlock,
                                 boolean notifyFailures) {
        abilityCastCommandActions.queue(playerId, abilityId, targetRef, targetBlock, notifyFailures);
    }

    public String startStyleTest(String playerId, String styleId) {
        return styleTestCommandActions.startStyleTest(playerId, styleId);
    }

    public String stopStyleTest(String playerId) {
        return styleTestCommandActions.stopStyleTest(playerId);
    }

    public String startSingleAbilityTest(String playerId, String abilityId) {
        return styleTestCommandActions.startSingleAbilityTest(playerId, abilityId);
    }

    public String getStyleTestStatus(String playerId) {
        return styleTestCommandActions.getStyleTestStatus(playerId);
    }

    public String spawnStyleTestMobs(String playerId) {
        return styleTestCommandActions.spawnStyleTestMobs(playerId);
    }

    public String clearStyleTestMobs(String playerId) {
        return styleTestCommandActions.clearStyleTestMobs(playerId);
    }

    public String resetStyleReviewArena(String playerId) {
        return styleTestCommandActions.resetStyleReviewArena(playerId);
    }

    public String countStyleTestMobs(String playerId) {
        return styleTestCommandActions.countStyleTestMobs(playerId);
    }

    public String spawnStyleTestMobs(String playerId, boolean closeGroundedTarget) {
        return styleTestCommandActions.spawnStyleTestMobs(playerId, closeGroundedTarget);
    }

    public String spawnStyleTestMobs(String playerId, String mode) {
        return styleTestCommandActions.spawnStyleTestMobs(playerId, mode);
    }

    public String queueDevProof(String playerId, String proofId) {
        return styleTestCommandActions.queueDevProof(playerId, proofId);
    }

    public String runStyleTestWeaponHit(String playerId) {
        return styleTestCommandActions.runStyleTestWeaponHit(playerId);
    }

    public String forceStyleTestStompLanding(String playerId) {
        return styleTestCommandActions.forceStyleTestStompLanding(playerId);
    }

    private void installStatusHud(Player player) {
        statusHudActions.install(player);
    }

    private void queueStatusHudInstall(String playerId) {
        statusHudActions.queueInstall(playerId);
    }

    private void processActiveProofCleanups(Store<EntityStore> currentStore) {
        proofCleanupProcessor.process(currentStore, System.currentTimeMillis());
    }

    private String runProofNow(String playerId, Player player, Store<EntityStore> currentStore, String proofId) {
        return proofActions.run(proofRuntime, playerId, player, currentStore, proofId);
    }

    private void initializeProofRunners() {
        proofRuntime.initialize(proofActions, LOG);
    }

    private void registerNativeHydroCraftingRecipe() {
        hydroRecipeRegistrar.registerLightWaterskinRecipe(
                LOG,
                HydroContainerItems.LIGHT_WATERSKIN_RECIPE_ID,
                HydroContainerItems.itemIds(),
                HydroContainerItems.CONTAINER_METADATA_KEY,
                HydroContainerItems.CONTAINER_TIER_METADATA_KEY,
                HydroContainerItems.LIGHT_WATERSKIN_INPUT_COUNT
        );
    }

    private void refreshAllStatusHuds(Store<EntityStore> currentStore) {
        statusHudActions.refreshAll(currentStore);
    }

    private void refreshAllPlayerProgressionBonuses(Store<EntityStore> currentStore) {
        playerProgressionActions.refreshAllPlayerProgressionBonuses(currentStore);
    }

    public void refreshStatusHud(String playerId) {
        statusHudActions.queueRefresh(playerId);
    }

    private void refreshStatusHudNow(String playerId) {
        statusHudActions.refreshNow(playerId);
    }

    public Player getRuntimePlayer(String playerId) {
        return runtimePlayerView.get(playerId);
    }

    public String describeRuntimePlayerPosition(String playerId) {
        return runtimePlayerView.describePosition(playerId);
    }

    public String queueRuntimePlayerRelocationForTesting(String playerId, String target) {
        return devRuntimeCommandActions.queueRuntimePlayerRelocationForTesting(playerId, target);
    }

    public String queueDaylightForTesting(String playerId) {
        return devRuntimeCommandActions.queueDaylightForTesting(playerId);
    }

    public String queueGameModeForTesting(String playerId, String mode) {
        return devRuntimeCommandActions.queueGameModeForTesting(playerId, mode);
    }

    public String queueTerraReviewKitGrant(String playerId) {
        return devRuntimeCommandActions.queueTerraReviewKitGrant(playerId);
    }

    public String queueTerraReviewInventoryClean(String playerId) {
        return devRuntimeCommandActions.queueTerraReviewInventoryClean(playerId);
    }

    public Player getRuntimePlayer(Ref<EntityStore> entityRef) {
        return runtimePlayerView.get(entityRef);
    }

    public String findOnlinePlayerId(Player runtimePlayer) {
        return runtimePlayerView.findOnlinePlayerId(runtimePlayer);
    }

    public PlayerRef getUniversePlayerRef(Player player) {
        return runtimePlayerView.universePlayerRef(player);
    }

    public String getRuntimePlayerId(Player player) {
        return runtimePlayerView.runtimePlayerId(player);
    }

    private boolean isPlayerInStore(Player player, Store<EntityStore> currentStore) {
        return runtimePlayerView.isPlayerInStore(player, currentStore);
    }

    private CombinedItemContainer getCombinedPlayerInventory(Player player) {
        Ref<EntityStore> playerRef = player != null ? player.getReference() : null;
        return MotmPlayerInventory.combined(playerRef, playerRef != null ? playerRef.getStore() : null);
    }

    public void rebuildPlayerRuntime(com.motm.model.PlayerData player) {
        if (player == null || player.getPlayerId() == null) {
            return;
        }
        LOG.info("[MOTM] Queue runtime rebuild: playerId=" + player.getPlayerId());
        runtimeTasks.requestRuntimeRebuild(player.getPlayerId());
    }

    private void rebuildPlayerRuntimeNow(com.motm.model.PlayerData player) {
        playerRuntimeRebuildActions.rebuildNow(player);
    }

    public int getAverageOnlinePlayerLevel() {
        return playerProgressionActions.averageOnlinePlayerLevel();
    }

    public int getAverageOnlinePlayerLevelForPlayer(String playerId) {
        return playerProgressionActions.averageOnlinePlayerLevelForPlayer(playerId);
    }

    public void refreshPlayerProgressionBonuses(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        runtimeTasks.requestProgressionBonusRefresh(playerId);
    }

    private void refreshPlayerProgressionBonusesNow(String playerId) {
        playerProgressionActions.refreshPlayerProgressionBonusesNow(playerId);
    }

    private boolean isPlayerCrouching(Player player) {
        return runtimePlayerView.isCrouching(player);
    }

    private void handlePlayerInteract(PlayerInteractEvent event) {
        if (spellbookInputHandler != null) {
            spellbookInputHandler.handleInteract(event);
        }
    }

    private void handlePlayerMouseButton(PlayerMouseButtonEvent event) {
        if (spellbookInputHandler != null) {
            spellbookInputHandler.handleMouseButton(event);
        }
    }

    /**
     * Entry point used by MotmSpellbookInteraction subclasses (custom item interaction codec).
     * Resolves player data and routes into the existing cast pipeline.
     * Phase 2 of CODEX_IMPLEMENTATION_PLAN_2026-05-13.md.
     */
    public void castSpellbookSlotFromInteraction(Player runtimePlayer, int slot) {
        if (spellbookInputHandler != null) {
            spellbookInputHandler.castSlotFromInteraction(runtimePlayer, slot);
        }
    }

    private void handleDamageBlock(DamageBlockEvent event) {
        if (blockDamageInteractionHandler != null) {
            blockDamageInteractionHandler.handle(event);
        }
    }

    public void handlePlayerCraft(CraftingRecipe recipe, int quantity, Player player) {
        if (runtimePerkManager != null) {
            runtimePerkManager.handlePlayerCraft(recipe, quantity, player);
        }
    }

    // --- Agent observability surface ---

    public String startObservabilityRun(String runId, String scenarioId, String playerId) {
        return observabilityActions.startRun(runId, scenarioId, playerId);
    }

    public String stopObservabilityRun(String reason) {
        return observabilityActions.stopRun(reason);
    }

    public String getObservabilityStatus() {
        return observabilityActions.status();
    }

    public String setObservabilityScenario(String scenarioId) {
        return observabilityActions.setScenario(scenarioId);
    }

    public String markObservabilityRun(String playerId, String label) {
        return observabilityActions.mark(playerId, label);
    }

    public String snapshotObservability(String playerId, String label) {
        return observabilityActions.snapshot(playerId, label);
    }

    public void recordControl(String type, String traceId, Map<String, Object> data) {
        observabilityEvents.recordControl(type, traceId, data);
    }

    public void recordCausality(String type, String traceId, Map<String, Object> data) {
        observabilityEvents.recordCausality(type, traceId, data);
    }

    private void recordRuntimeTaskEvidence(String phase,
                                           String taskType,
                                           String playerId,
                                           Map<String, Object> details) {
        Map<String, Object> payload = MotmObservability.mapOf(
                "phase", phase,
                "taskType", taskType,
                "playerId", playerId
        );
        if (details != null && !details.isEmpty()) {
            payload.putAll(details);
        }
        recordCausality("runtime_task_" + phase, null, payload);
    }

    public void recordServerTruth(String type, String traceId, Map<String, Object> data) {
        observabilityEvents.recordServerTruth(type, traceId, data);
    }

    public void recordClientIntent(String type, String traceId, Map<String, Object> data) {
        observabilityEvents.recordClientIntent(type, traceId, data);
    }

    public String enterObservabilityTrace(String traceId) {
        return observabilityEvents.enterTrace(traceId);
    }

    public void restoreObservabilityTrace(String previousTraceId) {
        observabilityEvents.restoreTrace(previousTraceId);
    }

    public String currentObservabilityTraceId() {
        return observabilityEvents.currentTraceId();
    }

    private String currentOrNewClientIntentTraceId() {
        return observabilityEvents.currentOrNewClientIntentTraceId();
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
    public RuntimePerkManager getRuntimePerkManager() { return runtimePerkManager; }
    public StyleManager getStyleManager() { return styleManager; }
    public ElementalReactionManager getElementalReactionManager() { return elementalReactionManager; }
    public SpellbookManager getSpellbookManager() { return spellbookManager; }
    public BookInteractionManager getBookInteractionManager() { return bookInteractionManager; }
    public GameplayPlaybackManager getGameplayPlaybackManager() { return gameplayPlaybackManager; }
    public MotmObservability getObservability() { return observability; }
    public Path getPluginDirectory() { return pluginDirectory; }
    public String getDefaultSpellbookItemId() { return SpellbookInventoryItems.DEFAULT_SPELLBOOK_ITEM_ID; }
    public Set<String> getRecognizedSpellbookItemIds() { return SpellbookInventoryItems.recognizedSpellbookItemIds(); }
    public Set<String> getRecognizedDevBookItemIds() { return SpellbookInventoryItems.recognizedDevBookItemIds(); }
    public boolean isCustomHudEnabled() { return CUSTOM_HUD_ENABLED; }

    public void sendPlayerMessage(Player player, Message message) {
        if (player == null || message == null) {
            return;
        }
        try {
            Ref<EntityStore> playerRef = player.getReference();
            Store<EntityStore> store = playerRef != null && playerRef.isValid() ? playerRef.getStore() : null;
            PlayerRef universePlayerRef = store != null && playerRef != null
                    ? store.getComponent(playerRef, PlayerRef.getComponentType())
                    : null;
            if (universePlayerRef != null) {
                universePlayerRef.sendMessage(message);
            }
        } catch (Exception e) {
            LOG.warning("[MOTM] Failed to send player message: " + e.getMessage());
        }
    }

    public String queueRuntimeEntityEffectsClearForDev(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return "no-player-id";
        }
        runtimeTasks.requestRuntimeEntityEffectClear(playerId);
        return "queued";
    }

    public String clearRuntimeEntityEffectsForDev(String playerId) {
        return clearRuntimeEntityEffectsForDev(playerId, null);
    }

    public String clearRuntimeEntityEffectsForDev(String playerId, Store<EntityStore> currentStore) {
        if (playerId == null || playerId.isBlank()) {
            return "no-player-id";
        }

        Player runtimePlayer = onlineRuntimePlayers.get(playerId);
        if (runtimePlayer == null) {
            return "no-runtime-player";
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return "no-player-store";
        }
        if (!isPlayerInStore(runtimePlayer, currentStore)) {
            return "wrong-store";
        }

        Store<EntityStore> store = currentStore != null ? currentStore : playerRef.getStore();
        if (store == null) {
            return "no-player-store";
        }
        EffectControllerComponent controller = store.getComponent(playerRef, EffectControllerComponent.getComponentType());
        if (controller == null) {
            return "no-effect-controller";
        }

        int before = controller.getActiveEffectIndexes() != null
                ? controller.getActiveEffectIndexes().length
                : controller.getAllActiveEntityEffects().length;
        controller.clearEffects(playerRef, store);
        LOG.info("[MOTM] Dev visual effects cleared: playerId=" + playerId
                + " nativeEffectsBefore=" + before);
        return "cleared " + before + " native effect" + (before == 1 ? "" : "s");
    }

    public void completeStartupSelection(String playerId) {
        if (playerId == null || playerId.isBlank() || playerDataManager == null) {
            return;
        }
        PlayerData player = playerDataManager.getOnlinePlayer(playerId);
        if (player != null
                && player.getPlayerClass() != null
                && player.getSelectedStyles() != null
                && !player.getSelectedStyles().isEmpty()) {
            player.setFirstJoin(false);
            player.setStartupSelectionComplete(true);
            player.setPendingStartupClass(null);
            playerDataManager.savePlayerData(player);
        }
    }

    public boolean isStartupSelectionProtected(String playerId) {
        if (playerId == null || playerId.isBlank() || playerDataManager == null) {
            return false;
        }
        PlayerData player = playerDataManager.getOnlinePlayer(playerId);
        return player != null
                && !player.isStartupSelectionComplete()
                && (player.isFirstJoin()
                || player.getPlayerClass() == null
                || player.getSelectedStyles() == null
                || player.getSelectedStyles().isEmpty());
    }

    public double getBlacksmithArmorDamageReduction(String playerId) {
        Player player = getRuntimePlayer(playerId);
        Ref<EntityStore> playerRef = player != null ? player.getReference() : null;
        Store<EntityStore> store = playerRef != null && playerRef.isValid() ? playerRef.getStore() : null;
        InventoryComponent.Armor armor = store != null && playerRef != null
                ? store.getComponent(playerRef, InventoryComponent.Armor.getComponentType())
                : null;
        if (armor == null || armor.getInventory() == null) {
            return 0.0;
        }
        final int[] enhancedPieces = {0};
        final double[] extraResistance = {0.0};
        armor.getInventory().forEach((slot, stack) -> {
            if (hasBooleanMetadata(stack, BLACKSMITH_METADATA_KEY)) {
                enhancedPieces[0]++;
                if (stack != null && stack.getItem() != null && stack.getItem().getArmor() != null) {
                    extraResistance[0] += Math.max(0.0, stack.getItem().getArmor().getBaseDamageResistance()) * 0.20;
                }
            }
        });
        double reduction = Math.min(0.20, extraResistance[0]);
        if (reduction > 0.0) {
            LOG.info("[MOTM] Runtime perk armor: blacksmith craftedPieces=" + enhancedPieces[0]
                    + " nativeArmorExtraResistance=" + String.format(Locale.ROOT, "%.3f", reduction)
                    + " player=" + playerId);
        }
        return reduction;
    }

    private boolean hasBooleanMetadata(ItemStack stack, String key) {
        if (stack == null || stack.getMetadata() == null || key == null) {
            return false;
        }
        Object value = stack.getMetadata().get(key);
        if (value == null) {
            return false;
        }
        try {
            Object isBoolean = value.getClass().getMethod("isBoolean").invoke(value);
            if (!Boolean.TRUE.equals(isBoolean)) {
                return false;
            }
            Object booleanValue = value.getClass().getMethod("asBoolean").invoke(value);
            Object rawValue = booleanValue.getClass().getMethod("getValue").invoke(booleanValue);
            return Boolean.TRUE.equals(rawValue);
        } catch (ReflectiveOperationException e) {
            LOG.warning("[MOTM] Failed to inspect item metadata flag " + key + ": " + e.getMessage());
            return false;
        }
    }

    public EcoFriendlyTreeResult applyEcoFriendlyTree(PlayerData playerData, Player player, DamageBlockEvent event) {
        return new EcoFriendlyTreeResult(false, "eco-friendly tree placement is not wired in this runtime baseline");
    }

    public record EcoFriendlyTreeResult(boolean success, String summary) {}

    public MotmPreflightAudit.AuditReport runPreflightAudit() {
        lastPreflightAudit = MotmPreflightAudit.run(this);
        return lastPreflightAudit;
    }
    public MotmPreflightAudit.AuditReport getLastPreflightAudit() { return lastPreflightAudit; }
    public boolean isDevToolsEnabled() {
        return MotmCommandAuth.canUseDevTools(MotmBuildInfo.INTERNAL_TEST_BUILD, devToolsEnabled);
    }
    public boolean isInternalTestBuild() { return MotmBuildInfo.INTERNAL_TEST_BUILD; }
    public String getBuildChannel() { return MotmBuildInfo.BUILD_CHANNEL; }
    public boolean isFreeCastEnabled(String playerId) {
        return freeCastCommandActions.isEnabled(playerId);
    }
    public void setFreeCastEnabled(String playerId, boolean enabled) {
        freeCastCommandActions.setEnabled(playerId, enabled);
    }
    public String devToolsDisabledMessage() {
        return MotmCommandAuth.devToolsDisabledMessage(
                MotmBuildInfo.INTERNAL_TEST_BUILD,
                MotmServerConfig.FILE_NAME
        );
    }

}
