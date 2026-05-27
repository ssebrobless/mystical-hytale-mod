package com.motm.manager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Rotation3f;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.component.CollisionResultComponent;
import com.hypixel.hytale.server.core.modules.entity.component.DisplayNameComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Interactable;
import com.hypixel.hytale.server.core.modules.entity.component.RespondToHit;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.collision.BlockCollisionData;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.entity.nameplate.Nameplate;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.fluid.Fluid;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.MenteesMod;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.model.StatusEffect;
import com.motm.model.StyleData;
import com.motm.util.AbilityPresentation;
import com.motm.util.HytaleAssetResolver;
import com.motm.util.MotmInventoryOps;
import com.motm.util.MotmObservability;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class GameplayPlaybackManager {

    private static final Logger LOG = Logger.getLogger("MOTM");
    private static final Set<String> MOVEMENT_CAST_TYPES = Set.of(
            "dash", "dash_buff", "dash_strike", "leap", "dive_strike", "teleport", "air_stall");
    private static final Set<String> LINE_CAST_TYPES = Set.of(
            "projectile", "projectile_line", "line_control", "wave_line", "projectile_burst");
    private static final Set<String> DELAYED_PROJECTILE_CAST_TYPES = Set.of(
            "projectile", "projectile_line", "wave_line", "projectile_burst", "projectile_volley");
    private static final Set<String> PERSISTENT_FIELD_CAST_TYPES = Set.of(
            "ground_zone", "support_zone", "barrier");
    private static final Set<String> AREA_CAST_TYPES = Set.of(
            "ground_burst", "ground_zone", "ground_target", "ground_strike",
            "support_zone", "self_burst", "barrier", "execute");
    private static final Set<String> CONE_CAST_TYPES = Set.of("cone", "gaze");
    private static final Set<String> MULTI_TARGET_CAST_TYPES = Set.of("projectile_volley", "chain");
    private static final Set<String> CASTER_EFFECT_TOKENS = Set.of(
            "attack_buff", "defense_buff", "evasion", "evasion_buff", "evasion_zone",
            "stealth", "damage_buff", "lifesteal", "flying", "self_burn", "speed");
    private static final Set<String> TARGET_EFFECT_TOKENS = Set.of(
            "burn", "dot", "stun", "stun_if_wall", "slow", "slow_stack", "vulnerability",
            "freeze", "root", "blind", "deafen", "disoriented", "attack_slow",
            "grounded", "shocked", "lightning", "knockback", "curse");
    private static final Set<String> CONCEPT_RUNTIME_RECONCILED_ABILITIES = Set.of(
            "stomp", "aftershock", "sinkhole",
            "iron_wall", "metal_coat", "alloy_enhancement",
            "lava_pool", "obsidian_skin", "magma_sling",
            "rubble_rouser", "pillar_strike", "rockslide",
            "rooted", "vines", "sapling",
            "nightshade", "frolick", "cacti_cluster",
            "gargoyle", "glare", "tunnel",
            "burrow", "mudpit", "debris",
            "sandstorm", "dust_devil", "vitrification",
            "lapidary", "fracture", "refraction",
            "frozen_needles", "stalactite_crash", "skate",
            "snow_imp", "snowstorm", "frosty",
            "high_tide", "waverider", "riptide",
            "piercing_rain", "rainbow", "splash",
            "scald", "geyser", "overheat",
            "vapor_vanish", "dispersion", "hidrosis",
            "ice_cap", "glacier", "ice_shelf",
            "tide_pool", "abyssal_assist", "rip_current",
            "leap_frog", "river_rapids", "swamp_monster",
            "bilge_dump", "anchor_haul", "oil_spill",
            "shriek", "sonic_boom", "battle_cry",
            "jet_burst", "afterburner", "mach_punch",
            "thunderclap", "smite", "chain_lightning",
            "twister", "funnel_cloud", "eye_of_the_storm",
            "leap", "divebomb", "hang_time",
            "air_slash", "gale_cutter", "razor_wind",
            "smoke_bomb", "vanish", "smoke_form",
            "gust", "cyclone_shield", "tempest",
            "air_shot", "bullet_storm", "pressure_burst",
            "smog", "toxic_breath", "acid_rain",
            "fireball", "ignite", "combust",
            "raise_dead", "life_drain", "death_mark",
            "shadow_step", "umbral_veil", "dark_embrace",
            "hellfire", "infernal_ground", "soul_scorch",
            "dominate", "mind_shatter", "hivemind",
            "imbue_power", "imbue_fortitude", "imbue_swiftness",
            "sanctuary", "absorb", "purify",
            "rift", "void_spawn", "consume",
            "scarak_egg", "brood_surge", "locust_queen",
            "pterodactyl_form", "triceratops_form", "t_rex_form");
    private static final Set<String> CONCEPT_STATE_MACHINE_ABILITIES = Set.of(
            "alloy_enhancement", "obsidian_skin", "rubble_rouser", "vines", "sapling",
            "gargoyle", "glare", "tunnel", "sandstorm", "dust_devil", "vitrification",
            "lapidary", "fracture", "refraction", "snowstorm", "waverider", "riptide",
            "piercing_rain", "vapor_vanish", "dispersion", "ice_cap", "ice_shelf",
            "abyssal_assist", "rip_current", "leap_frog", "river_rapids", "bilge_dump",
            "oil_spill", "sonic_boom", "jet_burst", "afterburner", "mach_punch",
            "divebomb", "hang_time", "razor_wind", "air_shot", "infernal_ground",
            "soul_scorch", "shadow_step", "umbral_veil", "death_mark", "mind_shatter",
            "hivemind");
    private static final Set<String> CONCEPT_PHYSICAL_VISUAL_ABILITIES = Set.of(
            "stomp", "aftershock", "sinkhole", "iron_wall", "lava_pool", "obsidian_skin",
            "magma_sling", "pillar_strike", "rockslide", "rooted", "vines", "sapling",
            "nightshade", "frolick", "cacti_cluster", "glare", "tunnel", "burrow",
            "mudpit", "debris", "sandstorm", "dust_devil", "lapidary", "fracture",
            "refraction", "stalactite_crash", "snowstorm", "waverider", "riptide",
            "scald", "ice_cap", "ice_shelf", "river_rapids", "anchor_haul",
            "fireball", "life_drain", "consume", "hellfire", "infernal_ground",
            "triceratops_form", "shriek", "sonic_boom", "twister", "funnel_cloud",
            "mach_punch", "divebomb", "hang_time", "air_slash", "gale_cutter",
            "air_shot", "bullet_storm", "rip_current", "leap_frog");
    private static final Set<String> CONCEPT_FRIENDLY_SAFE_ABILITIES = Set.of(
            "lava_pool", "mudpit", "rockslide", "frolick", "life_drain", "soul_scorch",
            "smoke_bomb", "toxic_breath", "oil_spill");
    private static final Set<String> CONCEPT_SUMMON_OBJECT_ABILITIES = Set.of(
            "sapling", "lapidary", "snow_imp", "frosty", "swamp_monster", "funnel_cloud",
            "raise_dead", "shadow_step", "void_spawn", "scarak_egg", "locust_queen");
    private static final double MAX_HORIZONTAL_MOVEMENT = 12.0;
    private static final double MAX_VERTICAL_MOVEMENT = 6.0;
    private static final double DEFAULT_LINE_HALF_WIDTH = 1.75;
    private static final double DEFAULT_AREA_RADIUS = 3.5;
    private static final double DEFAULT_CHAIN_RADIUS = 4.5;
    private static final int DEFAULT_CHAIN_TARGETS = 3;
    private static final int DEFAULT_PROJECTILE_CLUSTER_COUNT = 3;
    private static final String SUMMON_ROLE_NAME = "motm_summon";
    private static final String PROJECTILE_VISUAL_ROLE_NAME = "motm_projectile";
    private static final String FIELD_VISUAL_ROLE_NAME = "motm_field";
    private static final String EMPTY_VISUAL_ROLE_NAME = "Empty_Role";
    private static final String SAPLING_MARKER_BLOCK_ID = "Furniture_Temple_Emerald_Statue";
    private static final String SAPLING_MARKER_GLOW_EFFECT_ID = "MOTM_Arbor_Sapling_Pink_Glow";
    private static final long SUMMON_THINK_INTERVAL_MS = 450L;
    private static final long CHANNEL_PULSE_INTERVAL_MS = 700L;
    private static final long FORM_PULSE_INTERVAL_MS = 850L;
    private static final int DEFAULT_STATUS_SECONDS = 4;
    private static final int ONE_SHOT_BUFF_SECONDS = 12;
    private static final double DEFAULT_PROJECTILE_COLLISION_RADIUS = 0.9;
    private static final double DEFAULT_PROJECTILE_SPEED = 20.0;
    private static final double MAX_PROJECTILE_SPEED = 38.0;
    private static final double DEFAULT_PROJECTILE_TTL_SECONDS = 2.5;
    private static final double MAX_PROJECTILE_STEP_DISTANCE = 2.6;
    private static final double DEFAULT_LIGHTNING_ARC_RADIUS = 5.5;
    private static final long SHOCKED_DAMAGE_WINDOW_MS = 3_500L;
    private static final double DEFAULT_IMPACT_RADIUS = 0.0;
    private static final long DEFAULT_VOLLEY_STAGGER_MS = 80L;
    private static final long DEFAULT_BURST_STAGGER_MS = 22L;
    private static final long PROJECTILE_VISUAL_REFRESH_MS = 220L;
    private static final long FIELD_VISUAL_REFRESH_MS = 900L;
    private static final long FIELD_PULSE_INTERVAL_MS = 900L;
    private static final double DEFAULT_FIELD_THICKNESS = 1.35;
    private static final double DEFAULT_PULL_STOP_DISTANCE = 1.25;
    private static final double MAX_PULL_STEP_DISTANCE = 5.5;
    private static final double DEFAULT_FIELD_DAMAGE_RATIO = 0.28;
    private static final double DEFAULT_SUPPORT_HEAL_RATIO = 0.16;
    private static final double DEFAULT_SUPPORT_SHIELD_RATIO = 0.12;
    private static final double VOLLEY_SPREAD_DEGREES = 8.0;
    private static final double BURST_SPREAD_DEGREES = 12.0;
    private static final long LINE_CONTROL_PULSE_INTERVAL_MS = 350L;
    private static final double BLIND_DAMAGE_PENALTY = 0.22;
    private static final double DISORIENTED_DAMAGE_PENALTY = 0.12;
    private static final long STOMP_ARM_TIMEOUT_MILLIS = 30_000L;
    private static final double STOMP_JUMP_THRESHOLD_BLOCKS = 0.45;
    private static final double STOMP_LAND_TOLERANCE_BLOCKS = 0.10;

    private final MenteesMod mod;
    private final Map<String, List<ActiveSummon>> activeSummonsByOwner = new HashMap<>();
    private final Map<String, ActiveTransformation> activeTransformationsByPlayer = new HashMap<>();
    private final Map<String, Long> nextTransformationPulseAtByPlayer = new HashMap<>();
    private final Map<String, ActiveWeaponFollowUp> activeWeaponFollowUpsByPlayer = new HashMap<>();
    private final Map<String, RecentPosition> recentIronWallOriginByPlayer = new HashMap<>();
    private final Map<String, RecentPosition> recentCasterCenteredOriginByPlayer = new HashMap<>();
    private final Map<String, Vector3d> lavaPoolVelocityBoostByPlayer = new HashMap<>();
    private final Set<String> lavaPoolMovementBoostedPlayers = new LinkedHashSet<>();
    private final Map<String, Long> magmaHazardProtectionUntilByPlayer = new HashMap<>();
    private final Map<String, ArmedStomp> armedStompByPlayer = new ConcurrentHashMap<>();
    private final List<ActiveProjectile> activeProjectiles = new ArrayList<>();
    private final List<ActiveField> activeFields = new ArrayList<>();
    private final List<TemporaryTerrainSelection> activeTerrainSelections = new ArrayList<>();
    private final Set<String> activeTemporaryTerrainBlockKeys = ConcurrentHashMap.newKeySet();
    private final List<ActiveMovingTerrainTrail> activeMovingTerrainTrails = new ArrayList<>();
    private final List<ActiveStackingColumn> activeStackingColumns = new ArrayList<>();
    private final List<ActiveLapidaryGem> activeLapidaryGems = new ArrayList<>();
    private final List<ActiveChannel> activeChannels = new ArrayList<>();
    private final List<ActiveLineControl> activeLineControls = new ArrayList<>();
    private final List<ActivePlayerAnchor> activePlayerAnchors = new ArrayList<>();
    private final List<ActiveSelfEffect> activeSelfEffects = new ArrayList<>();
    private final List<ActiveDelayedBurst> activeDelayedBursts = new ArrayList<>();
    private final Set<Ref<EntityStore>> visualProxyRefs = ConcurrentHashMap.newKeySet();
    private final Map<String, List<BuriedVictim>> buriedVictimsByField = new HashMap<>();
    private final Set<String> reportedAbilityKillEntityIds = ConcurrentHashMap.newKeySet();
    private final Map<String, Long> recentShockedTargets = new ConcurrentHashMap<>();
    private boolean warnedGroundedFallback;

    public GameplayPlaybackManager(MenteesMod mod) {
        this.mod = mod;
    }

    public synchronized String getCastRestriction(PlayerData player, AbilityData ability) {
        if (player == null || ability == null || player.getPlayerId() == null) {
            return "";
        }

        String playerId = player.getPlayerId();
        if (mod.getStatusEffectManager().isIncapacitated(playerId)) {
            return "You are incapacitated and cannot cast right now.";
        }

        if (mod.getStatusEffectManager().hasEffect(playerId, StatusEffect.Type.GROUNDED)
                && isGroundRestrictedAbility(ability)) {
            return ability.getName() + " is blocked while you are grounded.";
        }

        return "";
    }

    private void logTerraAbilityEvent(String event,
                                      PlayerData player,
                                      StyleData style,
                                      AbilityData ability,
                                      String details) {
        if (player == null || ability == null || !"terra".equals(lower(player.getPlayerClass()))) {
            return;
        }
        LOG.info("[MOTM][terra-audit] event=" + event
                + " playerId=" + safe(player.getPlayerId())
                + " styleId=" + safe(style != null ? style.getId() : currentStyleId(player))
                + " abilityId=" + safe(ability.getId())
                + (details == null || details.isBlank() ? "" : " " + details));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public synchronized ExecutionResult executeAbility(Player runtimePlayer,
                                                       PlayerData player,
                                                       StyleData style,
                                                       AbilityData ability,
                                                       CastContext context) {
        if (runtimePlayer == null || player == null || style == null || ability == null) {
            return ExecutionResult.none("Runtime player context is unavailable.");
        }

        logTerraAbilityEvent("cast.begin", player, style, ability,
                "castType=" + lower(ability.getCastType())
                        + " targetType=" + lower(ability.getTargetType()));
        String traceId = mod.getObservability() != null
                ? mod.getObservability().nextTraceId("ability")
                : null;
        mod.recordCausality("ability_cast_begin", traceId, MotmObservability.mapOf(
                "playerId", player.getPlayerId(),
                "classId", safe(player.getPlayerClass()),
                "styleId", safe(style.getId()),
                "abilityId", safe(ability.getId()),
                "abilityName", safe(ability.getName()),
                "castType", safe(ability.getCastType()),
                "targetType", safe(ability.getTargetType())
        ));

        String previousTraceId = mod.enterObservabilityTrace(traceId);
        try {
        if ("jump_land".equalsIgnoreCase(ability.getTrigger())) {
            return armJumpLandAbility(runtimePlayer, player, style, ability);
        }

        PlaybackResult playback = playAbility(runtimePlayer, player, style, ability);
        ProjectileLaunchResult projectileLaunch = launchProjectiles(runtimePlayer, player, style, ability, context);
        FieldRuntimeResult fieldRuntime = activatePersistentField(runtimePlayer, player, style, ability, context);
        SupplementalTerrainRuntimeResult supplementalTerrain = activateSupplementalTerrainRuntime(
                runtimePlayer, player, style, ability, playback);
        AbilitySpecificRuntimeResult specificRuntime = applySpecificCastRuntime(
                runtimePlayer, player, style, ability, context, playback);
        MovementContactRuntimeResult movementContact = applyMovementContactRuntime(
                runtimePlayer, player, style, ability, playback);
        SupportResolution support = applyCasterRuntime(runtimePlayer, player, ability);
        CombatResolution combat = projectileLaunch.launched() > 0
                ? CombatResolution.none()
                : applyCombat(runtimePlayer, player, ability, context);
        double lifestealHealing = projectileLaunch.launched() > 0
                ? 0.0
                : applyLifesteal(runtimePlayer, player, combat.totalDamage());
        EffectResolution targetEffects = projectileLaunch.launched() > 0
                ? EffectResolution.none()
                : applyTargetEffects(runtimePlayer, player, ability, context);
        LineControlRuntimeResult lineControl = startLineControlRuntime(runtimePlayer, player, ability, context);
        ChannelRuntimeResult channel = startChannelRuntime(runtimePlayer, player, ability, context);
        FormRuntimeResult form = applyTransformation(runtimePlayer, player, style, ability);
        SummonRuntimeResult summons = handleSummonRuntime(runtimePlayer, player, style, ability, context);
        WeaponFollowUpResult followUp = armWeaponFollowUp(player, ability);
        AbilitySpecificRuntimeResult conceptRuntime = applyConceptRuntimeProfile(
                player, style, ability, playback, projectileLaunch, fieldRuntime, supplementalTerrain,
                specificRuntime, movementContact, combat, targetEffects, lineControl, channel, form, summons, followUp);

        if (combat.totalDamage() > 0) {
            player.getStatistics().setTotalDamageDealt(
                    player.getStatistics().getTotalDamageDealt() + combat.totalDamage());
        }
        if (support.healed() > 0 || lifestealHealing > 0) {
            player.getStatistics().setTotalHealingDone(
                    player.getStatistics().getTotalHealingDone() + support.healed() + lifestealHealing);
        }

        List<String> summaryParts = new ArrayList<>();
        if (!playback.summary().isBlank()) summaryParts.add(playback.summary());
        if (!projectileLaunch.summary().isBlank()) summaryParts.add(projectileLaunch.summary());
        if (!fieldRuntime.summary().isBlank()) summaryParts.add(fieldRuntime.summary());
        if (!supplementalTerrain.summary().isBlank()) summaryParts.add(supplementalTerrain.summary());
        if (!specificRuntime.summary().isBlank()) summaryParts.add(specificRuntime.summary());
        if (!movementContact.summary().isBlank()) summaryParts.add(movementContact.summary());
        if (!support.summary().isBlank()) summaryParts.add(support.summary());
        if (!combat.summary().isBlank()) summaryParts.add(combat.summary());
        if (lifestealHealing > 0) summaryParts.add("lifesteal " + AbilityPresentation.formatDecimal(lifestealHealing));
        if (!targetEffects.summary().isBlank()) summaryParts.add(targetEffects.summary());
        if (!lineControl.summary().isBlank()) summaryParts.add(lineControl.summary());
        if (!channel.summary().isBlank()) summaryParts.add(channel.summary());
        if (!form.summary().isBlank()) summaryParts.add(form.summary());
        if (!summons.summary().isBlank()) summaryParts.add(summons.summary());
        if (!followUp.summary().isBlank()) summaryParts.add(followUp.summary());
        if (!conceptRuntime.summary().isBlank()) summaryParts.add(conceptRuntime.summary());

        String summary = summaryParts.isEmpty()
                ? "No live runtime was applied."
                : String.join(" | ", summaryParts);

        logTerraAbilityEvent("cast.end", player, style, ability,
                "summary=" + summary
                        + " combatTargets=" + combat.targetsHit()
                        + " projectiles=" + projectileLaunch.launched()
                        + " field=" + fieldRuntime.activated()
                        + " terrain=" + supplementalTerrain.activated()
                        + " movementContact=" + movementContact.targetsHit()
                        + " summons=" + summons.spawned()
                        + " form=" + form.applied()
                        + " concept=" + !conceptRuntime.summary().isBlank());
        mod.recordCausality("ability_cast_end", traceId, MotmObservability.mapOf(
                "playerId", player.getPlayerId(),
                "styleId", safe(style.getId()),
                "abilityId", safe(ability.getId()),
                "summary", summary,
                "combatTargets", combat.targetsHit(),
                "totalDamage", combat.totalDamage(),
                "projectiles", projectileLaunch.launched(),
                "fieldActivated", fieldRuntime.activated(),
                "terrainActivated", supplementalTerrain.activated(),
                "movementContactTargets", movementContact.targetsHit(),
                "summonsSpawned", summons.spawned(),
                "summonsBuffed", summons.buffed(),
                "formApplied", form.applied(),
                "conceptRuntime", !conceptRuntime.summary().isBlank(),
                "conceptRoute", conceptRoute(ability),
                "conceptVisualPlan", conceptVisualPlan(ability),
                "conceptSafety", conceptSafetyPlan(ability)
        ));

        return new ExecutionResult(
                playback, combat.targetsHit(), combat.totalDamage(),
                summons.spawned(), summons.buffed(), form.applied(), summary);
        } finally {
            mod.restoreObservabilityTrace(previousTraceId);
        }
    }

    private ExecutionResult armJumpLandAbility(Player runtimePlayer,
                                               PlayerData player,
                                               StyleData style,
                                               AbilityData ability) {
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return ExecutionResult.none("Cannot arm: player reference invalid.");
        }

        Store<EntityStore> store = playerRef.getStore();
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
            return ExecutionResult.none("Cannot arm: player position unavailable.");
        }

        long now = System.currentTimeMillis();
        armedStompByPlayer.put(player.getPlayerId(), new ArmedStomp(
                player.getPlayerId(),
                player,
                style,
                ability,
                mod.currentObservabilityTraceId(),
                now,
                now + STOMP_ARM_TIMEOUT_MILLIS,
                transform.getTransform().getPosition().y,
                false
        ));

        logTerraAbilityEvent("stomp.armed", player, style, ability,
                "startY=" + AbilityPresentation.formatDecimal(transform.getTransform().getPosition().y)
                        + " timeoutMs=" + STOMP_ARM_TIMEOUT_MILLIS);
        LOG.info("[MOTM] Stomp armed: player=" + player.getPlayerName()
                + " - next jump's landing will trigger the shockwave");
        String effectId = resolveEffectId(player.getPlayerClass(), currentStyleId(player), ability);
        applyEffectById(playerRef, store, effectId);

        return new ExecutionResult(
                PlaybackResult.none("Stomp armed - jump and land to release."),
                0, 0.0, 0, 0, false,
                "Stomp armed (jump -> land to release the shockwave)");
    }

    public synchronized void tick(Store<EntityStore> currentStore) {
        if (currentStore == null) {
            return;
        }
        pruneVisualProxyRefs(currentStore);
        long now = System.currentTimeMillis();
        activeProjectiles.removeIf(projectile ->
                belongsToCurrentStore(projectile.ownerRef(), currentStore) && processProjectileTick(projectile, now));
        activeFields.removeIf(field ->
                belongsToCurrentStore(field.ownerRef(), currentStore) && processFieldTick(field, now));
        activePlayerAnchors.removeIf(anchor ->
                belongsToCurrentStore(anchor.ownerRef(), currentStore) && processPlayerAnchor(anchor, currentStore, now));
        activeSelfEffects.removeIf(effect ->
                belongsToCurrentStore(effect.ownerRef(), currentStore) && processActiveSelfEffect(effect, currentStore, now));
        activeDelayedBursts.removeIf(burst ->
                belongsToCurrentStore(burst.ownerRef(), currentStore) && processDelayedBurstTick(burst, currentStore, now));
        activeLapidaryGems.removeIf(gem -> processLapidaryGem(gem, currentStore, now));
        activeStackingColumns.removeIf(column -> processStackingColumn(column, currentStore, now));
        activeTerrainSelections.removeIf(selection -> processTemporaryTerrainSelection(selection, currentStore, now));
        activeMovingTerrainTrails.removeIf(trail -> processMovingTerrainTrail(trail, currentStore, now));
        activeLineControls.removeIf(lineControl ->
                belongsToCurrentStore(lineControl.ownerRef(), currentStore) && processLineControlTick(lineControl, now));
        activeChannels.removeIf(channel ->
                belongsToCurrentStore(channel.ownerRef(), currentStore) && processChannelTick(channel, now));
        activeTransformationsByPlayer.entrySet().removeIf(entry ->
                belongsToCurrentStore(entry.getValue().ownerRef(), currentStore)
                        && processTransformationTick(entry.getValue(), now));
        activeWeaponFollowUpsByPlayer.entrySet().removeIf(entry ->
                processWeaponFollowUpExpiry(entry.getKey(), entry.getValue(), currentStore, now));
        activeSummonsByOwner.values().removeIf(List::isEmpty);
        activeSummonsByOwner.values().forEach(summons ->
                summons.removeIf(summon ->
                        belongsToCurrentStore(summon.ref(), currentStore) && processSummonTick(summon, now)));
    }

    public synchronized void tickArmedStomps(Store<EntityStore> currentStore) {
        if (currentStore == null || armedStompByPlayer.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        for (Map.Entry<String, ArmedStomp> entry : new ArrayList<>(armedStompByPlayer.entrySet())) {
            String playerId = entry.getKey();
            ArmedStomp armed = entry.getValue();
            if (now >= armed.expireAtMillis()) {
                LOG.info("[MOTM] Stomp arm expired: player=" + armed.player().getPlayerName());
                armedStompByPlayer.remove(playerId, armed);
                continue;
            }

            Player runtimePlayer = mod.getRuntimePlayer(armed.playerId());
            if (runtimePlayer == null) {
                continue;
            }

            Ref<EntityStore> playerRef = runtimePlayer.getReference();
            if (playerRef == null || !playerRef.isValid() || !belongsToCurrentStore(playerRef, currentStore)) {
                continue;
            }

            TransformComponent transform = currentStore.getComponent(playerRef, TransformComponent.getComponentType());
            if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                continue;
            }

            double y = transform.getTransform().getPosition().y;
            double dy = y - armed.previousY();
            boolean nowAirborne = armed.wasAirborne()
                    || dy > STOMP_JUMP_THRESHOLD_BLOCKS / StyleManager.TICKS_PER_SECOND;
            double fractionalY = Math.abs(y - Math.floor(y));
            boolean landed = armed.wasAirborne()
                    && fractionalY < STOMP_LAND_TOLERANCE_BLOCKS
                    && dy <= 0.0;
            if (landed) {
                fireArmedStomp(runtimePlayer, armed, new Vector3d(transform.getTransform().getPosition()));
                armedStompByPlayer.remove(playerId, armed);
                continue;
            }

            armedStompByPlayer.replace(playerId, armed, new ArmedStomp(
                    armed.playerId(),
                    armed.player(),
                    armed.style(),
                    armed.ability(),
                    armed.traceId(),
                    armed.armedAtMillis(),
                    armed.expireAtMillis(),
                    y,
                    nowAirborne
            ));
        }
    }

    private void fireArmedStomp(Player runtimePlayer, ArmedStomp armed, Vector3d landingPosition) {
        LOG.info("[MOTM] Stomp fired at landing: player=" + armed.player().getPlayerName()
                + " pos=" + landingPosition);
        CastContext landingContext = CastContext.atPosition(landingPosition);
        AbilityData ability = armed.ability();
        PlaybackResult playback = playAbility(runtimePlayer, armed.player(), armed.style(), ability);
        CombatResolution combat = applyCombat(runtimePlayer, armed.player(), ability, landingContext);
        EffectResolution effects = applyTargetEffects(runtimePlayer, armed.player(), ability, landingContext);
        spawnQuakeImpactRing(runtimePlayer, ability, landingPosition);
        double lifestealHealing = applyLifesteal(runtimePlayer, armed.player(), combat.totalDamage());

        if (combat.totalDamage() > 0.0) {
            armed.player().getStatistics().setTotalDamageDealt(
                    armed.player().getStatistics().getTotalDamageDealt() + combat.totalDamage());
        }
        if (lifestealHealing > 0.0) {
            armed.player().getStatistics().setTotalHealingDone(
                    armed.player().getStatistics().getTotalHealingDone() + lifestealHealing);
        }

        LOG.info("[MOTM] Stomp landing resolved: targets=" + combat.targetsHit()
                + " damage=" + AbilityPresentation.formatDecimal(combat.totalDamage())
                + " effects=" + effects.effectsApplied()
                + (playback.effectApplied() ? " visual=applied" : " visual=missing"));
        logTerraAbilityEvent("cast.end", armed.player(), armed.style(), ability,
                "summary=Stomp landing resolved"
                        + " combatTargets=" + combat.targetsHit()
                        + " projectiles=0"
                        + " field=false"
                        + " terrain=false"
                        + " summons=0"
                        + " form=false");
        mod.recordCausality("ability_cast_end", armed.traceId(), MotmObservability.mapOf(
                "playerId", armed.player().getPlayerId(),
                "styleId", safe(armed.style() != null ? armed.style().getId() : currentStyleId(armed.player())),
                "abilityId", safe(ability.getId()),
                "summary", "Stomp landing resolved: targets=" + combat.targetsHit()
                        + " damage=" + AbilityPresentation.formatDecimal(combat.totalDamage())
                        + " effects=" + effects.effectsApplied()
                        + (playback.effectApplied() ? " visual=applied" : " visual=missing"),
                "combatTargets", combat.targetsHit(),
                "totalDamage", combat.totalDamage(),
                "projectiles", 0,
                "fieldActivated", false,
                "terrainActivated", false,
                "summonsSpawned", 0,
                "summonsBuffed", 0,
                "formApplied", false
        ));
    }

    private void spawnQuakeImpactRing(Player runtimePlayer, AbilityData ability, Vector3d center) {
        if (runtimePlayer == null || ability == null || center == null) {
            return;
        }

        World world = runtimePlayer.getWorld();
        if (world == null) {
            return;
        }

        List<Vector3d> positions = buildAreaVisualPositions(center, ability);
        if (positions.isEmpty()) {
            positions = List.of(new Vector3d(center));
        }

        String effectId = isSinkhole(ability) ? "MOTM_Terra_Sinkhole_Cracks" : "MOTM_Terra_Quake_Impact";
        float despawnSeconds = 1.0f;
        int spawned = 0;
        String roleId = HytaleAssetResolver.resolveFieldRoleId("terra", "quake", ability);
        for (Vector3d position : isQuakeGroundImpactAbility(ability) ? List.of(new Vector3d(center)) : positions) {
            NPCEntity proxy = new NPCEntity(world);
            proxy.setRoleName(roleId);
            proxy.setDespawnTime(despawnSeconds);
            world.spawnEntity(proxy, new Vector3d(position), new Rotation3f(0f, 0f, 0f));

            Ref<EntityStore> proxyRef = proxy.getReference();
            if (proxyRef != null && proxyRef.isValid() && proxyRef.getStore() != null) {
                visualProxyRefs.add(proxyRef);
                applyEffectById(proxyRef, proxyRef.getStore(), effectId);
                spawned++;
            }
        }

        LOG.info("[MOTM] Quake impact ring spawned at " + center
                + " positions=" + positions.size()
                + " applied=" + spawned);
    }

    public synchronized void clearArmedStomp(String playerId) {
        if (playerId != null) {
            armedStompByPlayer.remove(playerId);
        }
    }

    public synchronized String resetReviewRuntime(String playerId,
                                                  Store<EntityStore> currentStore,
                                                  Player runtimePlayer) {
        if (playerId == null || playerId.isBlank()) {
            return "runtime player unavailable";
        }

        World currentWorld = currentStore != null && currentStore.getExternalData() != null
                ? currentStore.getExternalData().getWorld()
                : runtimePlayer != null ? runtimePlayer.getWorld() : null;

        int armed = armedStompByPlayer.remove(playerId) != null ? 1 : 0;
        int projectiles = removeProjectilesForPlayer(playerId);
        int fields = removeFieldsForPlayer(playerId, currentStore);
        int anchors = removePlayerAnchorsForPlayer(playerId);
        int selfEffects = removeSelfEffectsForPlayer(playerId);
        int delayedBursts = removeDelayedBurstsForPlayer(playerId);
        int gems = removeLapidaryGemsForPlayer(playerId);
        int columns = removeStackingColumnsForWorld(currentWorld);
        int trails = removeMovingTerrainTrailsForWorld(currentWorld);
        int channels = removeChannelsForPlayer(playerId);
        int lineControls = removeLineControlsForPlayer(playerId);
        int transformations = activeTransformationsByPlayer.remove(playerId) != null ? 1 : 0;
        int followUps = activeWeaponFollowUpsByPlayer.remove(playerId) != null ? 1 : 0;
        int summons = removeSummonsForPlayer(playerId);
        int terrain = restoreTemporarySelectionsForWorld(currentWorld);
        int proxies = despawnVisualProxiesForStore(currentStore);

        Ref<EntityStore> runtimeRef = runtimePlayer != null ? runtimePlayer.getReference() : null;
        Store<EntityStore> runtimeStore = currentStore != null
                ? currentStore
                : runtimeRef != null && runtimeRef.isValid() ? runtimeRef.getStore() : null;
        clearLavaPoolOwnerVelocityBoost(playerId, runtimeRef, runtimeStore);
        lavaPoolVelocityBoostByPlayer.remove(playerId);
        lavaPoolMovementBoostedPlayers.remove(playerId);
        magmaHazardProtectionUntilByPlayer.remove(playerId);
        nextTransformationPulseAtByPlayer.remove(playerId);
        recentCasterCenteredOriginByPlayer.remove(playerId);

        String summary = "armed=" + armed
                + " projectiles=" + projectiles
                + " fields=" + fields
                + " anchors=" + anchors
                + " selfEffects=" + selfEffects
                + " delayedBursts=" + delayedBursts
                + " gems=" + gems
                + " columns=" + columns
                + " trails=" + trails
                + " channels=" + channels
                + " lineControls=" + lineControls
                + " transformations=" + transformations
                + " followUps=" + followUps
                + " summons=" + summons
                + " terrain=" + terrain
                + " proxies=" + proxies;
        LOG.info("[MOTM] Style review runtime reset: playerId=" + playerId + " " + summary);
        return summary;
    }

    public synchronized Map<String, Object> buildObservabilitySnapshot(String playerId) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("activeProjectiles", activeProjectiles.size());
        snapshot.put("activeFields", activeFields.size());
        snapshot.put("activeTerrainSelections", activeTerrainSelections.size());
        snapshot.put("activeMovingTerrainTrails", activeMovingTerrainTrails.size());
        snapshot.put("activeStackingColumns", activeStackingColumns.size());
        snapshot.put("activeLapidaryGems", activeLapidaryGems.size());
        snapshot.put("activeChannels", activeChannels.size());
        snapshot.put("activeLineControls", activeLineControls.size());
        snapshot.put("activePlayerAnchors", activePlayerAnchors.size());
        snapshot.put("activeSelfEffects", activeSelfEffects.size());
        snapshot.put("activeDelayedBursts", activeDelayedBursts.size());
        snapshot.put("visualProxyRefs", visualProxyRefs.size());
        snapshot.put("activeTransformations", activeTransformationsByPlayer.size());
        snapshot.put("activeWeaponFollowUps", activeWeaponFollowUpsByPlayer.size());
        snapshot.put("activeSummonOwners", activeSummonsByOwner.size());
        snapshot.put("activeSummons", activeSummonsByOwner.values().stream().mapToInt(List::size).sum());
        if (playerId != null && !playerId.isBlank()) {
            snapshot.put("player", MotmObservability.mapOf(
                    "armedStomp", armedStompByPlayer.containsKey(playerId),
                    "activeTransformation", activeTransformationsByPlayer.containsKey(playerId),
                    "activeWeaponFollowUp", activeWeaponFollowUpsByPlayer.containsKey(playerId),
                    "activeSummons", activeSummonsByOwner.getOrDefault(playerId, List.of()).size(),
                    "lavaPoolMovementBoosted", lavaPoolMovementBoostedPlayers.contains(playerId),
                    "magmaHazardProtectionUntil", magmaHazardProtectionUntilByPlayer.get(playerId)
            ));
        }
        return snapshot;
    }

    private int removeProjectilesForPlayer(String playerId) {
        int removed = 0;
        for (int index = activeProjectiles.size() - 1; index >= 0; index--) {
            ActiveProjectile projectile = activeProjectiles.get(index);
            if (!playerId.equals(projectile.ownerPlayerId())) {
                continue;
            }
            despawnProjectileVisual(projectile);
            activeProjectiles.remove(index);
            removed++;
        }
        return removed;
    }

    private int removeFieldsForPlayer(String playerId, Store<EntityStore> currentStore) {
        int removed = 0;
        for (int index = activeFields.size() - 1; index >= 0; index--) {
            ActiveField field = activeFields.get(index);
            if (!playerId.equals(field.ownerPlayerId())) {
                continue;
            }
            releaseSinkholeField(field, currentStore);
            restoreFieldTemporaryTerrain(field, currentStore);
            despawnFieldVisual(field);
            activeFields.remove(index);
            removed++;
        }
        return removed;
    }

    private int removePlayerAnchorsForPlayer(String playerId) {
        int removed = 0;
        for (int index = activePlayerAnchors.size() - 1; index >= 0; index--) {
            ActivePlayerAnchor anchor = activePlayerAnchors.get(index);
            if (!playerId.equals(anchor.ownerPlayerId())) {
                continue;
            }
            Store<EntityStore> store = anchor.ownerRef() != null && anchor.ownerRef().isValid()
                    ? anchor.ownerRef().getStore()
                    : null;
            setAnchorMovementFreeze(anchor.ownerRef(), store, false);
            zeroVelocity(anchor.ownerRef(), store);
            activePlayerAnchors.remove(index);
            removed++;
        }
        return removed;
    }

    private int removeSelfEffectsForPlayer(String playerId) {
        int removed = 0;
        for (int index = activeSelfEffects.size() - 1; index >= 0; index--) {
            ActiveSelfEffect effect = activeSelfEffects.get(index);
            if (!playerId.equals(effect.ownerPlayerId())) {
                continue;
            }
            Store<EntityStore> store = effect.ownerRef() != null && effect.ownerRef().isValid()
                    ? effect.ownerRef().getStore()
                    : null;
            removeEffectById(effect.ownerRef(), store, effect.effectId());
            activeSelfEffects.remove(index);
            removed++;
        }
        return removed;
    }

    private int removeDelayedBurstsForPlayer(String playerId) {
        int before = activeDelayedBursts.size();
        activeDelayedBursts.removeIf(burst -> playerId.equals(burst.ownerPlayerId()));
        return before - activeDelayedBursts.size();
    }

    private int removeLapidaryGemsForPlayer(String playerId) {
        int removed = 0;
        for (int index = activeLapidaryGems.size() - 1; index >= 0; index--) {
            ActiveLapidaryGem gem = activeLapidaryGems.get(index);
            if (!playerId.equals(gem.ownerPlayerId)) {
                continue;
            }
            despawnLapidaryGem(gem);
            activeLapidaryGems.remove(index);
            removed++;
        }
        return removed;
    }

    private int removeStackingColumnsForWorld(World world) {
        if (world == null) {
            return 0;
        }
        int before = activeStackingColumns.size();
        activeStackingColumns.removeIf(column -> sameWorld(column.world, world));
        return before - activeStackingColumns.size();
    }

    private int removeMovingTerrainTrailsForWorld(World world) {
        if (world == null) {
            return 0;
        }
        int before = activeMovingTerrainTrails.size();
        activeMovingTerrainTrails.removeIf(trail -> trail == null || sameWorld(trail.world, world));
        return before - activeMovingTerrainTrails.size();
    }

    private int removeChannelsForPlayer(String playerId) {
        int before = activeChannels.size();
        activeChannels.removeIf(channel -> playerId.equals(channel.ownerPlayerId()));
        return before - activeChannels.size();
    }

    private int removeLineControlsForPlayer(String playerId) {
        int before = activeLineControls.size();
        activeLineControls.removeIf(lineControl -> playerId.equals(lineControl.ownerPlayerId()));
        return before - activeLineControls.size();
    }

    private int removeSummonsForPlayer(String playerId) {
        List<ActiveSummon> summons = activeSummonsByOwner.remove(playerId);
        if (summons == null || summons.isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (ActiveSummon summon : summons) {
            if (despawnSummon(summon)) {
                removed++;
            }
        }
        return removed;
    }

    private int restoreTemporarySelectionsForWorld(World world) {
        if (world == null) {
            return 0;
        }
        int[] restored = {0};
        activeTerrainSelections.removeIf(selection -> {
            if (selection == null || !sameWorld(selection.world(), world)) {
                return false;
            }
            if (restoreTemporarySelection(selection, "review reset")) {
                restored[0]++;
            }
            return true;
        });
        return restored[0];
    }

    private boolean restoreTemporarySelection(TemporaryTerrainSelection selection, String context) {
        try {
            selection.originalSelection().place(null, selection.world(), new Vector3i(0, 0, 0), BlockMask.EMPTY);
            LOG.info("[MOTM] Temporary Terra terrain restored: reason=" + selection.reason()
                    + " anchor=" + selection.anchor()
                    + " context=" + context);
            return true;
        } catch (Throwable e) {
            LOG.warning("[MOTM] Temporary Terra terrain restore failed: reason=" + selection.reason()
                    + " anchor=" + selection.anchor()
                    + " context=" + context
                    + " error=" + e.getMessage());
            return false;
        } finally {
            activeTemporaryTerrainBlockKeys.removeAll(selection.protectedBlockKeys());
        }
    }

    private int despawnVisualProxiesForStore(Store<EntityStore> currentStore) {
        if (currentStore == null || visualProxyRefs.isEmpty()) {
            return 0;
        }
        int removed = 0;
        for (Ref<EntityStore> visualRef : List.copyOf(visualProxyRefs)) {
            if (visualRef == null || !visualRef.isValid()) {
                visualProxyRefs.remove(visualRef);
                removed++;
                continue;
            }
            if (visualRef.getStore() != currentStore) {
                continue;
            }
            NPCEntity npc = currentStore.getComponent(visualRef, NPCEntity.getComponentType());
            if (npc != null) {
                npc.setToDespawn();
            }
            visualProxyRefs.remove(visualRef);
            removed++;
        }
        return removed;
    }

    private int pruneVisualProxyRefs(Store<EntityStore> currentStore) {
        if (visualProxyRefs.isEmpty()) {
            return 0;
        }
        int before = visualProxyRefs.size();
        visualProxyRefs.removeIf(visualRef ->
                visualRef == null
                        || !visualRef.isValid()
                        || (currentStore != null && visualRef.getStore() != currentStore));
        return before - visualProxyRefs.size();
    }

    private boolean sameWorld(World left, World right) {
        return left != null && right != null && (left == right || left.equals(right));
    }


    public synchronized String forceArmedStompLanding(String playerId, Player runtimePlayer) {
        if (playerId == null || playerId.isBlank() || runtimePlayer == null) {
            return "[MOTM] Dev forced Stomp landing failed: runtime player unavailable.";
        }

        ArmedStomp armed = armedStompByPlayer.get(playerId);
        if (armed == null) {
            return "[MOTM] Dev forced Stomp landing skipped: no armed Stomp.";
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return "[MOTM] Dev forced Stomp landing failed: player store unavailable.";
        }

        Store<EntityStore> store = playerRef.getStore();
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
            return "[MOTM] Dev forced Stomp landing failed: player position unavailable.";
        }

        Vector3d landingPosition = new Vector3d(transform.getTransform().getPosition());
        LOG.info("[MOTM] Dev forced Stomp landing: player=" + armed.player().getPlayerName()
                + " pos=" + formatVector(landingPosition));
        fireArmedStomp(runtimePlayer, armed, landingPosition);
        armedStompByPlayer.remove(playerId, armed);
        return "[MOTM] Dev forced Stomp landing resolved at " + formatVector(landingPosition) + ".";
    }

    public synchronized boolean isMagmaHazardProtected(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return false;
        }
        Long until = magmaHazardProtectionUntilByPlayer.get(playerId);
        if (until == null) {
            return false;
        }
        long now = System.currentTimeMillis();
        if (now > until) {
            magmaHazardProtectionUntilByPlayer.remove(playerId);
            return false;
        }
        return true;
    }

    private boolean belongsToCurrentStore(Ref<EntityStore> ownerRef, Store<EntityStore> currentStore) {
        if (ownerRef == null || !ownerRef.isValid()) {
            return true;
        }

        try {
            return ownerRef.getStore() == currentStore;
        } catch (IllegalStateException ignored) {
            return true;
        }
    }

    private boolean canMutateInCurrentStore(Ref<EntityStore> ownerRef, Store<EntityStore> currentStore) {
        if (ownerRef == null || !ownerRef.isValid() || currentStore == null) {
            return false;
        }

        try {
            return ownerRef.getStore() == currentStore;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public synchronized String deactivateAbilityRuntime(PlayerData player, String abilityId) {
        if (player == null || abilityId == null || abilityId.isBlank()) {
            return "";
        }

        String playerId = player.getPlayerId();
        String normalizedAbilityId = lower(abilityId);
        List<String> summaryParts = new ArrayList<>();

        int removedFields = 0;
        for (int index = activeFields.size() - 1; index >= 0; index--) {
            ActiveField field = activeFields.get(index);
            if (!playerId.equals(field.ownerPlayerId()) || !normalizedAbilityId.equals(lower(field.ability().getId()))) {
                continue;
            }
            Store<EntityStore> fieldStore = field.ownerRef() != null && field.ownerRef().isValid() ? field.ownerRef().getStore() : null;
            clearLavaPoolOwnerVelocityBoost(field.ownerPlayerId(), field.ownerRef(), fieldStore);
            restoreFieldTemporaryTerrain(field, fieldStore);
            releaseSinkholeField(field, fieldStore);
            despawnFieldVisual(field);
            activeFields.remove(index);
            removedFields++;
        }
        if (removedFields > 0) {
            summaryParts.add("dismissed " + removedFields + " field" + (removedFields == 1 ? "" : "s"));
        }

        int removedChannels = 0;
        for (int index = activeChannels.size() - 1; index >= 0; index--) {
            ActiveChannel channel = activeChannels.get(index);
            if (!playerId.equals(channel.ownerPlayerId()) || !normalizedAbilityId.equals(lower(channel.ability().getId()))) {
                continue;
            }
            activeChannels.remove(index);
            removedChannels++;
        }
        if (removedChannels > 0) {
            summaryParts.add("ended " + removedChannels + " channel" + (removedChannels == 1 ? "" : "s"));
        }

        int removedLineControls = 0;
        for (int index = activeLineControls.size() - 1; index >= 0; index--) {
            ActiveLineControl lineControl = activeLineControls.get(index);
            if (!playerId.equals(lineControl.ownerPlayerId()) || !normalizedAbilityId.equals(lower(lineControl.ability().getId()))) {
                continue;
            }
            activeLineControls.remove(index);
            removedLineControls++;
        }
        if (removedLineControls > 0) {
            summaryParts.add("released " + removedLineControls + " control effect" + (removedLineControls == 1 ? "" : "s"));
        }

        ActiveTransformation transformation = activeTransformationsByPlayer.get(playerId);
        if (transformation != null && normalizedAbilityId.equals(lower(transformation.abilityId()))) {
            activeTransformationsByPlayer.remove(playerId);
            nextTransformationPulseAtByPlayer.remove(playerId);
            summaryParts.add("ended " + humanize(transformation.modelId()));
        }

        ActiveWeaponFollowUp followUp = activeWeaponFollowUpsByPlayer.get(playerId);
        if (followUp != null && normalizedAbilityId.equals(lower(followUp.sourceAbilityId()))) {
            activeWeaponFollowUpsByPlayer.remove(playerId);
            summaryParts.add("cleared weapon follow-up");
        }

        int clearedStatusEffects = mod.getStatusEffectManager().clearEffectsFromSource(playerId, abilityId);
        if (clearedStatusEffects > 0) {
            summaryParts.add("cleared " + clearedStatusEffects + " status effect" + (clearedStatusEffects == 1 ? "" : "s"));
        }

        return summaryParts.isEmpty() ? "" : String.join(" | ", summaryParts);
    }

    public PlaybackResult playAbility(Player runtimePlayer,
                                      PlayerData player,
                                      StyleData style,
                                      AbilityData ability) {
        if (runtimePlayer == null || player == null || style == null || ability == null) {
            return PlaybackResult.none("Runtime player context is unavailable.");
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return PlaybackResult.none("Player entity reference is unavailable.");
        }

        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            return PlaybackResult.none("Player entity store is unavailable.");
        }

        String effectId = suppressGenericCasterVisual(ability)
                ? null
                : resolveEffectId(player.getPlayerClass(), currentStyleId(player), ability);
        boolean effectApplied = applyEffectById(playerRef, store, effectId);
        MovementResult movementResult = applyMovement(runtimePlayer, playerRef, store, ability);

        List<String> summaryParts = new ArrayList<>();
        if (effectApplied) summaryParts.add(formatEffectLabel(effectId) + " visuals");
        if (movementResult.applied()) summaryParts.add(movementResult.summary());

        if (summaryParts.isEmpty()) {
            return PlaybackResult.none("No live playback was applied.");
        }

        return new PlaybackResult(
                effectApplied,
                effectId,
                movementResult.applied(),
                movementResult.horizontalDistance(),
                movementResult.verticalDistance(),
                movementResult.startPosition(),
                movementResult.endPosition(),
                String.join(" | ", summaryParts)
        );
    }

    private ProjectileLaunchResult launchProjectiles(Player runtimePlayer,
                                                     PlayerData player,
                                                     StyleData style,
                                                     AbilityData ability,
                                                     CastContext context) {
        String castType = lower(ability.getCastType());
        if (!DELAYED_PROJECTILE_CAST_TYPES.contains(castType)) {
            return ProjectileLaunchResult.none();
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return ProjectileLaunchResult.none();
        }

        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            return ProjectileLaunchResult.none();
        }

        Vector3d origin = resolveProjectileOrigin(playerRef, store, ability);
        Vector3d direction = resolveLaunchDirection(playerRef, store, ability, context);
        if (origin == null || direction == null) {
            return ProjectileLaunchResult.none();
        }

        int projectileCount = resolveProjectileCount(castType, ability);
        double speedPerTick = resolveProjectileSpeedPerTick(ability);
        if (mod.getRuntimePerkManager() != null) {
            speedPerTick = mod.getRuntimePerkManager().modifyProjectileSpeed(player, speedPerTick);
        }
        double maxDistance = Math.max(resolveRange(ability), 4.0);
        if (isGroundMarkerProjectile(ability) && context != null && context.targetBlock() != null) {
            Vector3d markerTarget = new Vector3d(
                    context.targetBlock().x + 0.5,
                    context.targetBlock().y + 1.0,
                    context.targetBlock().z + 0.5
            );
            maxDistance = Math.max(1.0, Math.min(maxDistance, distance(origin, markerTarget)));
        }
        double impactRadius = resolveProjectileImpactRadius(ability, castType);
        double collisionRadius = resolveProjectileCollisionRadius(ability, castType);
        double spreadDegrees = resolveProjectileSpreadDegrees(castType, ability, projectileCount);
        long lifetimeMillis = resolveProjectileLifetimeMillis(ability, speedPerTick, maxDistance);
        double baseDamage = resolveDamageAmount(player, ability);
        long launchBaseTime = System.currentTimeMillis();
        String traceId = mod.currentObservabilityTraceId();

        for (int index = 0; index < projectileCount; index++) {
            double angleOffset = projectileCount == 1
                    ? 0.0
                    : (index - ((projectileCount - 1) / 2.0)) * spreadDegrees;
            Vector3d projectileDirection = rotateAroundY(direction, angleOffset);
            long activateAtMillis = launchBaseTime + resolveProjectileLaunchDelayMillis(castType, ability, index);
            ProjectileVisualRuntime visual = spawnProjectileVisualProxy(
                    runtimePlayer,
                    player.getPlayerClass(),
                    style.getId(),
                    ability,
                    origin,
                    activateAtMillis,
                    activateAtMillis + lifetimeMillis
            );
            activeProjectiles.add(new ActiveProjectile(
                    player.getPlayerId(),
                    playerRef,
                    player.getPlayerClass(),
                    style.getId(),
                    ability,
                    new Vector3d(origin),
                    projectileDirection,
                    speedPerTick,
                    maxDistance,
                    impactRadius,
                    collisionRadius,
                    activateAtMillis,
                    activateAtMillis + lifetimeMillis,
                    baseDamage,
                    new LinkedHashSet<>(),
                    visual.visualRef(),
                    visual.travelEffectId(),
                    visual.nextRefreshAtMillis(),
                    traceId
            ));
            LOG.info("[MOTM] projectile_launch abilityId=" + safe(ability.getId())
                    + " index=" + (index + 1) + "/" + projectileCount
                    + " origin=" + formatVector(origin)
                    + " direction=" + formatVector(projectileDirection)
                    + " speedPerTick=" + AbilityPresentation.formatDecimal(speedPerTick)
                    + " maxDistance=" + AbilityPresentation.formatDecimal(maxDistance)
                    + " collisionRadius=" + AbilityPresentation.formatDecimal(collisionRadius)
                    + " impactRadius=" + AbilityPresentation.formatDecimal(impactRadius)
                    + " visualProxy=" + (visual.visualRef() != null ? visual.visualRef().getIndex() : "none")
                    + " traceId=" + safe(traceId));
        }

        String label = projectileCount == 1 ? "projectile" : "projectiles";
        return new ProjectileLaunchResult(
                projectileCount,
                "launched " + projectileCount + " " + label + " at "
                        + formatDistance(speedPerTick * StyleManager.TICKS_PER_SECOND) + "m/s"
                        + switch (castType) {
                            case "projectile_volley" -> " | volley cadence";
                            case "projectile_burst" -> " | burst spread";
                            default -> "";
                        }
        );
    }

    private FieldRuntimeResult activatePersistentField(Player runtimePlayer,
                                                       PlayerData player,
                                                       StyleData style,
                                                       AbilityData ability,
                                                       CastContext context) {
        String castType = lower(ability.getCastType());
        if (!isPersistentFieldAbility(ability)) {
            return FieldRuntimeResult.none();
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return FieldRuntimeResult.none();
        }

        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            return FieldRuntimeResult.none();
        }

        Vector3d origin = getPosition(playerRef, store);
        Vector3d forward = getDirection(playerRef, store);
        if (origin == null || forward == null) {
            return FieldRuntimeResult.none();
        }

        boolean ironWall = isIronWallAbility(ability);
        if (ironWall) {
            origin = resolveStableIronWallOrigin(player.getPlayerId(), origin);
        } else if (isCasterCenteredAreaAbility(ability)) {
            origin = resolveStableCasterCenteredOrigin(player.getPlayerId(), origin);
        }
        Vector3d gemAnchor = resolveActiveLapidaryGemCenter(player, ability, store);
        Vector3d ironWallForward = ironWall ? resolveIronWallForward(forward) : null;
        Vector3d center = gemAnchor != null
                ? gemAnchor
                : ironWallForward != null
                ? resolveIronWallCenter(origin, ironWallForward)
                : resolveAreaCenter(origin, forward, context, resolveRange(ability), ability);
        if (center == null) {
            return FieldRuntimeResult.none();
        }

        double radius = ability.getRadius() > 0 ? ability.getRadius() : DEFAULT_AREA_RADIUS;
        double halfWidth = ability.getWidth() > 0 ? ability.getWidth() / 2.0 : Math.max(radius, 3.0);
        double thickness = ability.getCastType().equalsIgnoreCase("barrier")
                ? DEFAULT_FIELD_THICKNESS
                : Math.max(1.25, radius);
        Vector3d lineDirection = ironWallForward != null
                ? new Vector3d(-ironWallForward.z, 0.0, ironWallForward.x)
                : rotateAroundY(new Vector3d(forward.x, 0.0, forward.z), 90.0);
        long now = System.currentTimeMillis();
        long delayMillis = (long) (Math.max(0.0, ability.getDelaySeconds()) * 1000);
        long activateAtMillis = now + delayMillis;
        long durationMillis = (long) (Math.max(1.5, ability.getDurationSeconds() > 0 ? ability.getDurationSeconds() : 4.0) * 1000);
        String terrainSummary = placePersistentTerrainSelection(
                runtimePlayer,
                ability,
                center,
                normalize(ironWallForward != null ? ironWallForward : forward),
                normalize(lineDirection),
                activateAtMillis + durationMillis
        );
        int immediatePushes = ironWall
                ? pushTargetsOverlappingIronWall(playerRef, store, ability, center,
                ironWallForward != null ? ironWallForward : forward, lineDirection)
                : 0;
        FieldVisualRuntime visual = spawnFieldVisualProxy(
                runtimePlayer,
                player.getPlayerClass(),
                style.getId(),
                ability,
                center,
                normalize(lineDirection),
                halfWidth,
                activateAtMillis,
                activateAtMillis + durationMillis
        );
        registerFieldRuntime(
                player.getPlayerId(),
                playerRef,
                player.getPlayerClass(),
                style.getId(),
                ability,
                center,
                normalize(ironWallForward != null ? ironWallForward : forward),
                normalize(lineDirection),
                radius,
                halfWidth,
                thickness,
                activateAtMillis,
                activateAtMillis + durationMillis,
                false,
                visual
        );

        String fieldLabel = switch (castType) {
            case "barrier" -> "barrier";
            case "ground_target" -> "hazard";
            default -> "field";
        };
        String sizeLabel = "barrier".equals(castType)
                ? "width " + formatDistance(halfWidth * 2.0) + "m"
                : "radius " + formatDistance(radius) + "m";
        String controlLabel = ability.getPullForce() > 0
                ? " | pull " + formatDistance(resolvePullStep(ability, 0.55, 0.75)) + "m pulse"
                : "";
        String timingLabel = delayMillis > 0
                ? "arms in " + AbilityPresentation.formatDecimal(delayMillis / 1000.0) + "s"
                + " | lasts " + AbilityPresentation.formatDecimal(durationMillis / 1000.0) + "s"
                : "active for " + AbilityPresentation.formatDecimal(durationMillis / 1000.0) + "s";
        String terrainLabel = terrainSummary.isBlank() ? "" : " | " + terrainSummary;
        String pushLabel = immediatePushes > 0 ? " | pushed " + immediatePushes + " spawn-overlap target(s)" : "";
        return new FieldRuntimeResult(true,
                fieldLabel + " " + timingLabel
                        + " | " + sizeLabel
                        + controlLabel
                        + terrainLabel
                        + pushLabel);
    }

    private SupplementalTerrainRuntimeResult activateSupplementalTerrainRuntime(Player runtimePlayer,
                                                                                PlayerData player,
                                                                                StyleData style,
                                                                                AbilityData ability,
                                                                                PlaybackResult playback) {
        if (runtimePlayer == null || player == null || style == null || ability == null) {
            return SupplementalTerrainRuntimeResult.none();
        }

        if (PERSISTENT_FIELD_CAST_TYPES.contains(lower(ability.getCastType())) || isPersistentFieldAbility(ability)) {
            return SupplementalTerrainRuntimeResult.none();
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return SupplementalTerrainRuntimeResult.none();
        }

        Store<EntityStore> store = playerRef.getStore();
        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
            return SupplementalTerrainRuntimeResult.none();
        }

        String terrainEffect = lower(ability.getTerrainEffect());
        String abilityId = lower(ability.getId());
        String castType = lower(ability.getCastType());

        List<Vector3d> centers = new ArrayList<>();
        Vector3d forward = currentForward(transform.getTransform().getDirection());
        Vector3d lineDirection = rotateAroundY(new Vector3d(forward.x, 0.0, forward.z), 90.0);
        double radius = Math.max(1.8, ability.getRadius() > 0 ? ability.getRadius() : 2.75);
        double halfWidth = Math.max(1.2, ability.getWidth() > 0 ? ability.getWidth() / 2.0 : radius);
        double thickness = Math.max(1.1, Math.min(radius, 2.5));
        double durationSeconds = Math.max(2.0, ability.getDurationSeconds() > 0 ? ability.getDurationSeconds() : 3.0);
        boolean followOwner = false;
        boolean created = false;
        String summary;

        if (shouldCreateMovementTrail(ability, playback)) {
            centers.addAll(buildTrailCenters(playback.startPosition(), playback.endPosition(), resolveTrailNodeCount(ability)));
            radius = resolveTrailRadius(ability);
            halfWidth = radius;
            thickness = Math.max(1.0, radius * 0.8);
            summary = humanize(terrainEffect.isBlank() ? abilityId : terrainEffect) + " trail";
        } else if (shouldCreatePersonalAuraField(ability)) {
            centers.add(new Vector3d(transform.getTransform().getPosition()));
            radius = resolveAuraRadius(ability);
            halfWidth = radius;
            thickness = Math.max(1.1, radius * 0.9);
            followOwner = true;
            summary = humanize(terrainEffect.isBlank() ? abilityId : terrainEffect) + " aura";
        } else {
            return SupplementalTerrainRuntimeResult.none();
        }

        long now = System.currentTimeMillis();
        long activateAtMillis = now;
        long expireAtMillis = now + (long) (durationSeconds * 1000);
        for (Vector3d center : centers) {
            FieldVisualRuntime visual = spawnFieldVisualProxy(
                    runtimePlayer,
                    player.getPlayerClass(),
                    style.getId(),
                    ability,
                    center,
                    normalize(lineDirection),
                    halfWidth,
                    activateAtMillis,
                    expireAtMillis
            );
            registerFieldRuntime(
                    player.getPlayerId(),
                    playerRef,
                    player.getPlayerClass(),
                    style.getId(),
                    ability,
                    center,
                    normalize(forward),
                    normalize(lineDirection),
                    radius,
                    halfWidth,
                    thickness,
                    activateAtMillis,
                    expireAtMillis,
                    followOwner,
                    visual
            );
            placeSupplementalSurfaceCue(runtimePlayer.getWorld(), ability, center, expireAtMillis);
            created = true;
        }

        if (!created) {
            return SupplementalTerrainRuntimeResult.none();
        }

        String detail = centers.size() > 1
                ? centers.size() + " nodes"
                : "radius " + formatDistance(radius) + "m";
        return new SupplementalTerrainRuntimeResult(true,
                summary + " | " + detail + " | "
                        + AbilityPresentation.formatDecimal(durationSeconds) + "s");
    }

    private AbilitySpecificRuntimeResult applySpecificCastRuntime(Player runtimePlayer,
                                                                  PlayerData player,
                                                                  StyleData style,
                                                                  AbilityData ability,
                                                                  CastContext context,
                                                                  PlaybackResult playback) {
        if (runtimePlayer == null || player == null || style == null || ability == null) {
            return AbilitySpecificRuntimeResult.none();
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null && playerRef.isValid() ? playerRef.getStore() : null;
        if (playerRef == null || store == null) {
            return AbilitySpecificRuntimeResult.none();
        }

        String abilityId = lower(ability.getId());
        List<String> parts = new ArrayList<>();

        switch (abilityId) {
            case "metal_coat" -> {
                if (applyEffectById(playerRef, store, "MOTM_Proof_Coating_Metal")) {
                    parts.add("metal coating");
                }
            }
            case "alloy_enhancement" -> {
                if (applyEffectById(playerRef, store, "MOTM_Proof_Alloy_Enhancement")) {
                    parts.add("alloy coating");
                }
            }
            case "rubble_rouser" -> {
                if (applyEffectById(playerRef, store, "MOTM_Proof_Coating_Stone")) {
                    parts.add("stone-arm coating");
                }
                parts.add("next unarmed strike throws rubble");
                logTerraAbilityEvent("rubble_rouser.armed", player, style, ability,
                        "uses=1 splashRadius=2.8");
            }
            case "obsidian_skin" -> {
                long nowMillis = System.currentTimeMillis();
                long lavaExpireAt = nowMillis + 1_800L;
                long guardExpireAt = nowMillis + 7_500L;
                magmaHazardProtectionUntilByPlayer.put(player.getPlayerId(), guardExpireAt);
                String lavaShell = placeObsidianBlockShellSelection(
                        runtimePlayer.getWorld(),
                        "obsidian_skin",
                        getPosition(playerRef, store),
                        lavaExpireAt,
                        "Rock_Volcanic_Cracked_Lava",
                        "Rock_Volcanic_Cracked_Incandescent",
                        "Rock_Magma_Cooled"
                );
                if (!lavaShell.isBlank()) {
                    parts.add(lavaShell.replace("terrain ", "lava shell "));
                } else if (applyEffectById(playerRef, store, "MOTM_Proof_Obsidian_Lava_Wrap")) {
                    parts.add("lava wrap");
                    startActiveSelfEffect(playerRef, player.getPlayerId(), "MOTM_Proof_Obsidian_Lava_Wrap", lavaExpireAt);
                }
                startPlayerAnchor(player, playerRef, store, lavaExpireAt, "MOTM_Proof_Coating_Obsidian");
                startActiveSelfEffect(
                        playerRef,
                        player.getPlayerId(),
                        "MOTM_Proof_Coating_Obsidian",
                        guardExpireAt,
                        lavaExpireAt
                );
                mod.getStatusEffectManager().applyEffect(player.getPlayerId(), new StatusEffect(
                        StatusEffect.Type.ROOT,
                        Math.max(1, (int) Math.round(1.8 * StyleManager.TICKS_PER_SECOND)),
                        0.0,
                        player.getPlayerId(),
                        ability.getId()
                ));
                parts.add("obsidian root");
                parts.add("queued obsidian coating");
            }
            case "rooted" -> {
                String terrain = placeAbilityTerrainSelection(runtimePlayer, player, ability, context, "rooted");
                if (!terrain.isBlank()) {
                    parts.add(terrain);
                }
                long expireAt = System.currentTimeMillis()
                        + (long) (Math.max(1.0, ability.getDurationSeconds() > 0
                        ? ability.getDurationSeconds()
                        : 5.0) * 1000);
                startPlayerAnchor(player, playerRef, store, "rooted", expireAt, "");
                if (applyOwnerStatusToken("root", player, ability)) {
                    parts.add("self rooted");
                }
                parts.add("root anchor");
            }
            case "pillar_strike" -> {
                Vector3d center = resolveContextTargetOrBlockPosition(context, playerRef, store);
                int pillarHeight = Math.max(1, (int) Math.round(ability.getHeight() > 0 ? ability.getHeight() : 4.0));
                String terrain = placeStackingColumnSelection(
                        runtimePlayer.getWorld(),
                        "stone_pillar",
                        center,
                        pillarHeight,
                        System.currentTimeMillis() + (pillarHeight * 90L) + 600L,
                        "Rock_Stone_Brick_Pillar_Middle",
                        "Rock_Stone_Brick");
                if (!terrain.isBlank()) {
                    parts.add(terrain);
                }
                LOG.info("[MOTM][terra-audit] event=pillar_strike.column target="
                        + (context != null && context.explicitTargetRef() != null
                        ? resolveEntityId(context.explicitTargetRef(), store)
                        : "block")
                        + " center=" + formatVector(center)
                        + " terrain=" + terrain);
                int launched = launchTargetsFromPoint(playerRef, store, ability, center, Math.max(2.5, ability.getRadius()), true);
                parts.add("pillar launched " + launched + " target" + (launched == 1 ? "" : "s"));
            }
            case "frolick", "cacti_cluster", "lapidary", "glare", "debris",
                 "fracture", "refraction" -> {
                String terrain = placeAbilityTerrainSelection(runtimePlayer, player, ability, context, abilityId);
                if (!terrain.isBlank()) {
                    parts.add(terrain);
                }
            }
            case "gargoyle" -> {
                if (applyEffectById(playerRef, store, "MOTM_Proof_Coating_Stone")) {
                    parts.add("stone coating");
                }
            }
            case "sandstorm" -> {
                String terrain = placeAbilityTerrainSelection(runtimePlayer, player, ability, context, abilityId);
                if (!terrain.isBlank()) {
                    parts.add("sand surface ring");
                    parts.add(terrain);
                }
            }
            case "tunnel" -> {
                if (applyEffectById(playerRef, store, "MOTM_Proof_Coating_Stone")) {
                    parts.add("stone block form cue");
                }
                String terrain = placeAbilityTerrainSelection(runtimePlayer, player, ability, context, abilityId);
                if (!terrain.isBlank()) {
                    parts.add(terrain);
                }
                if (playback != null && playback.movementApplied()) {
                    parts.add("surface-safe tunnel move");
                }
            }
            default -> {
                return AbilitySpecificRuntimeResult.none();
            }
        }

        return parts.isEmpty()
                ? AbilitySpecificRuntimeResult.none()
                : new AbilitySpecificRuntimeResult(String.join(" | ", parts));
    }

    private AbilitySpecificRuntimeResult applyConceptRuntimeProfile(PlayerData player,
                                                                    StyleData style,
                                                                    AbilityData ability,
                                                                    PlaybackResult playback,
                                                                    ProjectileLaunchResult projectileLaunch,
                                                                    FieldRuntimeResult fieldRuntime,
                                                                    SupplementalTerrainRuntimeResult supplementalTerrain,
                                                                    AbilitySpecificRuntimeResult specificRuntime,
                                                                    MovementContactRuntimeResult movementContact,
                                                                    CombatResolution combat,
                                                                    EffectResolution targetEffects,
                                                                    LineControlRuntimeResult lineControl,
                                                                    ChannelRuntimeResult channel,
                                                                    FormRuntimeResult form,
                                                                    SummonRuntimeResult summons,
                                                                    WeaponFollowUpResult followUp) {
        String abilityId = lower(ability != null ? ability.getId() : null);
        if (!CONCEPT_RUNTIME_RECONCILED_ABILITIES.contains(abilityId)) {
            return AbilitySpecificRuntimeResult.none();
        }

        String route = conceptRoute(ability);
        String visualPlan = conceptVisualPlan(ability);
        String safety = conceptSafetyPlan(ability);
        boolean mechanicalSignal = hasConceptMechanicalSignal(
                playback, projectileLaunch, fieldRuntime, supplementalTerrain, specificRuntime,
                movementContact, combat, targetEffects, lineControl, channel, form, summons, followUp);
        boolean visualSignal = hasConceptVisualSignal(
                ability, projectileLaunch, fieldRuntime, supplementalTerrain, specificRuntime, form, summons);

        mod.recordCausality("ability_concept_route", mod.currentObservabilityTraceId(), MotmObservability.mapOf(
                "playerId", player != null ? player.getPlayerId() : "",
                "classId", player != null ? safe(player.getPlayerClass()) : "",
                "styleId", style != null ? safe(style.getId()) : "",
                "abilityId", safe(ability != null ? ability.getId() : ""),
                "route", route,
                "visualPlan", visualPlan,
                "safety", safety,
                "mechanicalSignal", mechanicalSignal,
                "visualSignal", visualSignal,
                "stateMachine", CONCEPT_STATE_MACHINE_ABILITIES.contains(abilityId),
                "physicalVisual", CONCEPT_PHYSICAL_VISUAL_ABILITIES.contains(abilityId),
                "friendlySafe", CONCEPT_FRIENDLY_SAFE_ABILITIES.contains(abilityId),
                "summonOrObject", CONCEPT_SUMMON_OBJECT_ABILITIES.contains(abilityId)
        ));

        StringBuilder summary = new StringBuilder("concept route: ").append(route);
        if (!visualPlan.isBlank()) {
            summary.append(" | visual: ").append(visualPlan);
        }
        if (!safety.isBlank()) {
            summary.append(" | safety: ").append(safety);
        }
        summary.append(" | mechanical ").append(mechanicalSignal ? "ok" : "needs-live-proof");
        summary.append(" | visual ").append(visualSignal ? "ok" : "needs-live-proof");
        return new AbilitySpecificRuntimeResult(summary.toString());
    }

    private boolean hasConceptMechanicalSignal(PlaybackResult playback,
                                               ProjectileLaunchResult projectileLaunch,
                                               FieldRuntimeResult fieldRuntime,
                                               SupplementalTerrainRuntimeResult supplementalTerrain,
                                               AbilitySpecificRuntimeResult specificRuntime,
                                               MovementContactRuntimeResult movementContact,
                                               CombatResolution combat,
                                               EffectResolution targetEffects,
                                               LineControlRuntimeResult lineControl,
                                               ChannelRuntimeResult channel,
                                               FormRuntimeResult form,
                                               SummonRuntimeResult summons,
                                               WeaponFollowUpResult followUp) {
        return (playback != null && !playback.summary().isBlank())
                || (projectileLaunch != null && projectileLaunch.launched() > 0)
                || (fieldRuntime != null && fieldRuntime.activated())
                || (supplementalTerrain != null && supplementalTerrain.activated())
                || (specificRuntime != null && !specificRuntime.summary().isBlank())
                || (movementContact != null && movementContact.targetsHit() > 0)
                || (combat != null && (combat.targetsHit() > 0 || combat.totalDamage() > 0.0))
                || (targetEffects != null && !targetEffects.summary().isBlank())
                || (lineControl != null && lineControl.started())
                || (channel != null && channel.started())
                || (form != null && form.applied())
                || (summons != null && (summons.spawned() > 0 || summons.buffed() > 0))
                || (followUp != null && !followUp.summary().isBlank());
    }

    private boolean hasConceptVisualSignal(AbilityData ability,
                                           ProjectileLaunchResult projectileLaunch,
                                           FieldRuntimeResult fieldRuntime,
                                           SupplementalTerrainRuntimeResult supplementalTerrain,
                                           AbilitySpecificRuntimeResult specificRuntime,
                                           FormRuntimeResult form,
                                           SummonRuntimeResult summons) {
        String abilityId = lower(ability != null ? ability.getId() : null);
        if (!CONCEPT_PHYSICAL_VISUAL_ABILITIES.contains(abilityId)) {
            return true;
        }
        return (projectileLaunch != null && projectileLaunch.launched() > 0)
                || (fieldRuntime != null && fieldRuntime.activated())
                || (supplementalTerrain != null && supplementalTerrain.activated())
                || (specificRuntime != null && !specificRuntime.summary().isBlank())
                || (form != null && form.applied())
                || (summons != null && summons.spawned() > 0);
    }

    private String conceptRoute(AbilityData ability) {
        if (ability == null) {
            return "";
        }
        String abilityId = lower(ability.getId());
        if (CONCEPT_SUMMON_OBJECT_ABILITIES.contains(abilityId)) {
            return "owned summon/object lifecycle";
        }
        if (CONCEPT_STATE_MACHINE_ABILITIES.contains(abilityId)) {
            return "explicit state machine";
        }
        String castType = lower(ability.getCastType());
        if (DELAYED_PROJECTILE_CAST_TYPES.contains(castType)) {
            return "aimed projectile";
        }
        if (PERSISTENT_FIELD_CAST_TYPES.contains(castType) || isPersistentFieldAbility(ability)) {
            return "persistent field";
        }
        if (MOVEMENT_CAST_TYPES.contains(castType)) {
            return "movement/form";
        }
        if (AREA_CAST_TYPES.contains(castType) || CONE_CAST_TYPES.contains(castType)) {
            return "area/status";
        }
        return "self/support";
    }

    private String conceptVisualPlan(AbilityData ability) {
        if (ability == null) {
            return "";
        }
        String abilityId = lower(ability.getId());
        if (CONCEPT_PHYSICAL_VISUAL_ABILITIES.contains(abilityId)) {
            return "physical/proxy world visual";
        }
        String terrain = lower(ability.getTerrainEffect());
        if (!terrain.isBlank()) {
            return "terrain/effect: " + terrain;
        }
        if (CONCEPT_SUMMON_OBJECT_ABILITIES.contains(abilityId)) {
            return "owned visible entity/object";
        }
        return "effect/status visual";
    }

    private String conceptSafetyPlan(AbilityData ability) {
        String abilityId = lower(ability != null ? ability.getId() : null);
        return CONCEPT_FRIENDLY_SAFE_ABILITIES.contains(abilityId)
                ? "hostile-only; caster/allies/summons skipped"
                : "standard hostile targeting";
    }

    private Vector3d resolveContextTargetOrBlockPosition(CastContext context,
                                                         Ref<EntityStore> fallbackRef,
                                                         Store<EntityStore> store) {
        if (context != null && context.explicitTargetRef() != null && context.explicitTargetRef().isValid()) {
            Vector3d targetPosition = getPosition(context.explicitTargetRef(), store);
            if (targetPosition != null) {
                return new Vector3d(targetPosition);
            }
        }
        return resolveCastContextPosition(context, fallbackRef, store);
    }

    private boolean applyOwnerStatusToken(String token,
                                          PlayerData player,
                                          AbilityData ability) {
        if (token == null || token.isBlank() || player == null || ability == null || player.getPlayerId() == null) {
            return false;
        }

        StatusEffect effect = createStatusEffect(token, ability, player.getPlayerId(), ability.getId());
        if (effect == null) {
            return false;
        }

        mod.getStatusEffectManager().applyEffect(player.getPlayerId(), effect);
        return true;
    }

    private boolean processTemporaryTerrainSelection(TemporaryTerrainSelection selection,
                                                     Store<EntityStore> currentStore,
                                                     long now) {
        if (selection == null || now < selection.expireAtMillis()) {
            return false;
        }

        World currentWorld = currentStore != null && currentStore.getExternalData() != null
                ? currentStore.getExternalData().getWorld()
                : null;
        if (currentWorld == null || (selection.world() != currentWorld && !selection.world().equals(currentWorld))) {
            return false;
        }

        try {
            selection.originalSelection().place(null, selection.world(), new Vector3i(0, 0, 0), BlockMask.EMPTY);
            LOG.info("[MOTM] Temporary Terra terrain restored: reason=" + selection.reason()
                    + " anchor=" + selection.anchor());
        } catch (Throwable e) {
            LOG.warning("[MOTM] Temporary Terra terrain restore failed: reason=" + selection.reason()
                    + " anchor=" + selection.anchor()
                    + " error=" + e.getMessage());
        } finally {
            activeTemporaryTerrainBlockKeys.removeAll(selection.protectedBlockKeys());
        }
        return true;
    }

    private void applyLavaPoolOwnerMobility(ActiveField field, Store<EntityStore> store) {
        if (field == null || store == null || field.ownerPlayerId() == null
                || !"lava_pool".equals(lower(field.ability().getId()))) {
            return;
        }

        magmaHazardProtectionUntilByPlayer.put(field.ownerPlayerId(), field.expireAtMillis() + 1250L);
        Vector3d ownerPosition = getPosition(field.ownerRef(), store);
        if (ownerPosition == null || distance(ownerPosition, field.center()) > Math.max(1.5, field.radius() + 0.5)) {
            clearLavaPoolOwnerVelocityBoost(field.ownerPlayerId(), field.ownerRef(), store);
            return;
        }

        mod.getStatusEffectManager().removeEffect(field.ownerPlayerId(), StatusEffect.Type.SLOW);
        mod.getStatusEffectManager().removeEffect(field.ownerPlayerId(), StatusEffect.Type.SLOW_STACK);
        mod.getStatusEffectManager().removeEffect(field.ownerPlayerId(), StatusEffect.Type.BURN);
        applyLavaPoolOwnerMovementBoost(field.ownerPlayerId(), field.ownerRef(), store);
    }

    private void clearLavaPoolOwnerVelocityBoost(String playerId,
                                                 Ref<EntityStore> ownerRef,
                                                 Store<EntityStore> store) {
        clearLavaPoolOwnerMovementBoost(playerId, ownerRef, store);
        Vector3d previousBoost = lavaPoolVelocityBoostByPlayer.remove(playerId);
        if (previousBoost == null || ownerRef == null || !ownerRef.isValid() || store == null) {
            return;
        }

        Velocity velocity = store.getComponent(ownerRef, Velocity.getComponentType());
        if (velocity == null) {
            return;
        }

        Vector3d currentVelocity = velocity.getVelocity();
        if (currentVelocity == null || !currentVelocity.isFinite()) {
            return;
        }

        velocity.set(
                currentVelocity.x - previousBoost.x,
                currentVelocity.y,
                currentVelocity.z - previousBoost.z
        );
    }

    private void applyLavaPoolOwnerMovementBoost(String playerId,
                                                 Ref<EntityStore> ownerRef,
                                                 Store<EntityStore> store) {
        if (playerId == null || playerId.isBlank() || ownerRef == null || !ownerRef.isValid() || store == null) {
            return;
        }
        try {
            MovementManager movementManager = store.getComponent(ownerRef, MovementManager.getComponentType());
            if (movementManager == null || movementManager.getSettings() == null) {
                return;
            }
            var settings = movementManager.getSettings();
            settings.baseSpeed = Math.max(settings.baseSpeed, 11.0f);
            settings.forwardWalkSpeedMultiplier = Math.max(settings.forwardWalkSpeedMultiplier, 1.15f);
            settings.backwardWalkSpeedMultiplier = Math.max(settings.backwardWalkSpeedMultiplier, 1.00f);
            settings.strafeWalkSpeedMultiplier = Math.max(settings.strafeWalkSpeedMultiplier, 1.15f);
            settings.forwardRunSpeedMultiplier = Math.max(settings.forwardRunSpeedMultiplier, 1.65f);
            settings.backwardRunSpeedMultiplier = Math.max(settings.backwardRunSpeedMultiplier, 1.25f);
            settings.strafeRunSpeedMultiplier = Math.max(settings.strafeRunSpeedMultiplier, 1.65f);
            settings.forwardSprintSpeedMultiplier = Math.max(settings.forwardSprintSpeedMultiplier, 1.85f);
            settings.acceleration = Math.max(settings.acceleration, 0.22f);
            settings.maxSpeedMultiplier = Math.max(settings.maxSpeedMultiplier, 20.0f);
            PlayerRef universePlayerRef = store.getComponent(ownerRef, PlayerRef.getComponentType());
            if (universePlayerRef != null && universePlayerRef.getPacketHandler() != null) {
                movementManager.update(universePlayerRef.getPacketHandler());
            }
            lavaPoolMovementBoostedPlayers.add(playerId);
        } catch (Exception e) {
            LOG.warning("[MOTM] Lava Pool movement compensation failed: playerId=" + playerId
                    + " error=" + e.getMessage());
        }
    }

    private void clearLavaPoolOwnerMovementBoost(String playerId,
                                                 Ref<EntityStore> ownerRef,
                                                 Store<EntityStore> store) {
        if (playerId == null || !lavaPoolMovementBoostedPlayers.remove(playerId)
                || ownerRef == null || !ownerRef.isValid() || store == null) {
            return;
        }
        try {
            MovementManager movementManager = store.getComponent(ownerRef, MovementManager.getComponentType());
            if (movementManager == null) {
                return;
            }
            movementManager.applyDefaultSettings();
            PlayerRef universePlayerRef = store.getComponent(ownerRef, PlayerRef.getComponentType());
            if (universePlayerRef != null && universePlayerRef.getPacketHandler() != null) {
                movementManager.update(universePlayerRef.getPacketHandler());
            }
        } catch (Exception e) {
            LOG.warning("[MOTM] Lava Pool movement compensation reset failed: playerId=" + playerId
                    + " error=" + e.getMessage());
        }
    }

    private void restoreFieldTemporaryTerrain(ActiveField field, Store<EntityStore> store) {
        if (field == null || field.ability() == null || store == null || store.getExternalData() == null) {
            return;
        }
        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }
        String abilityId = lower(field.ability().getId());
        String terrainEffect = lower(field.ability().getTerrainEffect());
        if ("lava_pool".equals(abilityId) || terrainEffect.contains("lava_pool")) {
            restoreActiveTemporarySelections(world, "lava_pool");
        } else if ("mudpit".equals(abilityId) || terrainEffect.contains("mudpit")) {
            restoreActiveTemporarySelections(world, "mudpit");
        } else if ("iron_wall".equals(abilityId) || terrainEffect.contains("iron_wall")) {
            restoreActiveTemporarySelections(world, "iron_wall");
        }
    }

    private boolean processPlayerAnchor(ActivePlayerAnchor anchor,
                                        Store<EntityStore> currentStore,
                                        long now) {
        if (anchor == null || anchor.ownerRef() == null || !anchor.ownerRef().isValid()) {
            return true;
        }
        if (now >= anchor.expireAtMillis()) {
            setAnchorMovementFreeze(anchor.ownerRef(), currentStore, false);
            zeroVelocity(anchor.ownerRef(), currentStore);
            boolean applied = false;
            if (anchor.completionEffectId() != null && !anchor.completionEffectId().isBlank()) {
                applied = applyEffectById(anchor.ownerRef(), currentStore, anchor.completionEffectId());
                startActiveSelfEffect(anchor.ownerRef(), anchor.ownerPlayerId(), anchor.completionEffectId(), now + 6_250L);
            }
            LOG.info("[MOTM] Player anchor released: reason=" + anchor.reason()
                    + " playerId=" + anchor.ownerPlayerId()
                    + " completionEffect=" + (anchor.completionEffectId() == null || anchor.completionEffectId().isBlank()
                    ? "none"
                    : anchor.completionEffectId())
                    + " applied=" + applied);
            return true;
        }

        setAnchorMovementFreeze(anchor.ownerRef(), currentStore, true);
        snapPlayerToAnchor(anchor, currentStore);
        zeroVelocity(anchor.ownerRef(), currentStore);
        return false;
    }

    private void snapPlayerToAnchor(ActivePlayerAnchor anchor, Store<EntityStore> store) {
        if (anchor == null || anchor.ownerRef() == null || !anchor.ownerRef().isValid()
                || anchor.anchor() == null || store == null) {
            return;
        }
        Vector3d current = getPosition(anchor.ownerRef(), store);
        if (current == null || distance(current, anchor.anchor()) < 0.08) {
            return;
        }
        Player runtimePlayer = mod.getRuntimePlayer(anchor.ownerPlayerId());
        if (runtimePlayer == null) {
            return;
        }
        try {
            runtimePlayer.moveTo(anchor.ownerRef(), anchor.anchor().x, anchor.anchor().y, anchor.anchor().z, store);
        } catch (Exception e) {
            LOG.warning("[MOTM] Player anchor snap failed: reason=" + anchor.reason()
                    + " playerId=" + anchor.ownerPlayerId()
                    + " error=" + e.getMessage());
        }
    }

    private boolean processActiveSelfEffect(ActiveSelfEffect effect,
                                            Store<EntityStore> currentStore,
                                            long now) {
        if (effect == null || effect.ownerRef() == null || !effect.ownerRef().isValid()) {
            return true;
        }
        if (now >= effect.expireAtMillis()) {
            return true;
        }
        if (now < effect.nextApplyAtMillis()) {
            return false;
        }
        boolean applied = applyEffectById(effect.ownerRef(), currentStore, effect.effectId());
        effect.nextApplyAtMillis = now + 650L;
        if (applied) {
            LOG.fine("[MOTM] Active self effect refreshed: playerId=" + effect.ownerPlayerId()
                    + " effect=" + effect.effectId());
        }
        return false;
    }

    private void startActiveSelfEffect(Ref<EntityStore> ownerRef,
                                       String ownerPlayerId,
                                       String effectId,
                                       long expireAtMillis) {
        startActiveSelfEffect(ownerRef, ownerPlayerId, effectId, expireAtMillis, System.currentTimeMillis() + 180L);
    }

    private void startActiveSelfEffect(Ref<EntityStore> ownerRef,
                                       String ownerPlayerId,
                                       String effectId,
                                       long expireAtMillis,
                                       long nextApplyAtMillis) {
        if (ownerRef == null || !ownerRef.isValid() || effectId == null || effectId.isBlank()) {
            return;
        }
        activeSelfEffects.removeIf(existing -> ownerPlayerId != null
                && ownerPlayerId.equals(existing.ownerPlayerId())
                && effectId.equals(existing.effectId()));
        activeSelfEffects.add(new ActiveSelfEffect(ownerPlayerId, ownerRef, effectId, expireAtMillis, nextApplyAtMillis));
    }

    private void setAnchorMovementFreeze(Ref<EntityStore> ownerRef,
                                         Store<EntityStore> store,
                                         boolean frozen) {
        if (ownerRef == null || !ownerRef.isValid() || store == null) {
            return;
        }
        try {
            MovementManager movementManager = store.getComponent(ownerRef, MovementManager.getComponentType());
            if (movementManager == null || movementManager.getSettings() == null) {
                return;
            }
            if (frozen) {
                var settings = movementManager.getSettings();
                settings.forwardWalkSpeedMultiplier = 0.0f;
                settings.backwardWalkSpeedMultiplier = 0.0f;
                settings.strafeWalkSpeedMultiplier = 0.0f;
                settings.forwardRunSpeedMultiplier = 0.0f;
                settings.backwardRunSpeedMultiplier = 0.0f;
                settings.strafeRunSpeedMultiplier = 0.0f;
                settings.forwardCrouchSpeedMultiplier = 0.0f;
                settings.backwardCrouchSpeedMultiplier = 0.0f;
                settings.strafeCrouchSpeedMultiplier = 0.0f;
                settings.forwardSprintSpeedMultiplier = 0.0f;
                settings.acceleration = 0.01f;
                settings.maxSpeedMultiplier = 0.01f;
            } else {
                movementManager.applyDefaultSettings();
            }
            PlayerRef universePlayerRef = store.getComponent(ownerRef, PlayerRef.getComponentType());
            if (universePlayerRef != null && universePlayerRef.getPacketHandler() != null) {
                movementManager.update(universePlayerRef.getPacketHandler());
            }
        } catch (Exception e) {
            LOG.warning("[MOTM] Player anchor movement freeze update failed: " + e.getMessage());
        }
    }

    private void zeroVelocity(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        if (entityRef == null || !entityRef.isValid() || store == null) {
            return;
        }
        Velocity velocity = store.getComponent(entityRef, Velocity.getComponentType());
        if (velocity != null) {
            velocity.set(0.0, 0.0, 0.0);
        }
    }

    private boolean processMovingTerrainTrail(ActiveMovingTerrainTrail trail,
                                              Store<EntityStore> currentStore,
                                              long now) {
        if (trail == null || now >= trail.expireAtMillis) {
            return true;
        }
        if (now < trail.nextPlaceAtMillis) {
            return false;
        }
        if (!belongsToCurrentStore(trail.ownerRef, currentStore)) {
            return false;
        }

        Vector3d position = getPosition(trail.ownerRef, currentStore);
        if (position == null) {
            return true;
        }
        if (trail.lastPosition == null) {
            trail.lastPosition = new Vector3d(position);
        }

        int blockTypeId = resolveRuntimeBlockTypeId(trail.blockIds);
        if (blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            LOG.warning("[MOTM] Moving Terra terrain trail skipped: reason=" + trail.reason
                    + " no block id resolved.");
            return true;
        }

        Vector3d delta = subtract(position, trail.lastPosition);
        double distance = Math.sqrt((delta.x * delta.x) + (delta.z * delta.z));
        if (distance < 0.35) {
            trail.nextPlaceAtMillis = now + 180L;
            return false;
        }

        Vector3d travel = normalizeHorizontal(delta);
        Vector3i right = horizontalRightStep(travel);
        int stamps = Math.max(1, Math.min(5, (int) Math.ceil(distance / 0.75)));
        int placed = 0;
        for (int index = 1; index <= stamps; index++) {
            double factor = index / (double) (stamps + 1);
            Vector3d trailCenter = new Vector3d(trail.lastPosition).fma(factor, delta);
            Vector3i anchor = surfaceOverlayAnchor(trailCenter);
            if (sameBlock(trail.lastAnchor, anchor)) {
                continue;
            }

            BlockSelection selection = baseSelection(anchor);
            Set<String> protectedKeys = new LinkedHashSet<>();
            selection.addBlockAtWorldPos(anchor.x, anchor.y, anchor.z, blockTypeId, 0, 0, 0);
            protectedKeys.add(blockKey(anchor));
            selection.addBlockAtWorldPos(anchor.x + right.x, anchor.y, anchor.z + right.z, blockTypeId, 0, 0, 0);
            protectedKeys.add(blockKey(anchor.x + right.x, anchor.y, anchor.z + right.z));
            selection.addBlockAtWorldPos(anchor.x - right.x, anchor.y, anchor.z - right.z, blockTypeId, 0, 0, 0);
            protectedKeys.add(blockKey(anchor.x - right.x, anchor.y, anchor.z - right.z));
            placeTemporarySelection(trail.world, trail.reason, anchor, selection,
                    Math.min(trail.expireAtMillis, now + 4500L),
                    "3 surface trail flowers on movement path", protectedKeys);
            trail.lastAnchor = anchor;
            placed++;
        }
        trail.lastPosition = new Vector3d(position);
        trail.nextPlaceAtMillis = now + (placed > 0 ? 180L : 260L);
        return false;
    }

    private boolean processStackingColumn(ActiveStackingColumn column,
                                          Store<EntityStore> currentStore,
                                          long now) {
        if (column == null || now >= column.expireAtMillis || column.placedHeight >= column.height) {
            return true;
        }
        World currentWorld = currentStore != null && currentStore.getExternalData() != null
                ? currentStore.getExternalData().getWorld()
                : null;
        if (currentWorld == null || (column.world != currentWorld && !column.world.equals(currentWorld))) {
            return false;
        }
        if (now < column.nextStageAtMillis) {
            return false;
        }

        Vector3i block = new Vector3i(
                column.anchor.x,
                column.anchor.y + column.placedHeight,
                column.anchor.z
        );
        BlockSelection selection = baseSelection(block);
        selection.addBlockAtWorldPos(block.x, block.y, block.z, column.blockTypeId, 0, 0, 0);
        placeTemporarySelection(column.world, column.reason, block, selection, column.expireAtMillis,
                "pillar stage " + (column.placedHeight + 1) + "/" + column.height,
                Set.of(blockKey(block)));
        column.placedHeight++;
        column.nextStageAtMillis = now + 90L;
        return column.placedHeight >= column.height;
    }

    private boolean startMovingTerrainTrail(World world,
                                            Ref<EntityStore> ownerRef,
                                            String reason,
                                            long expireAtMillis,
                                            String... blockIds) {
        if (world == null || ownerRef == null || !ownerRef.isValid()
                || blockIds == null || blockIds.length == 0) {
            return false;
        }
        if (resolveRuntimeBlockTypeId(blockIds) == BlockType.UNKNOWN_ID) {
            return false;
        }

        activeMovingTerrainTrails.add(new ActiveMovingTerrainTrail(
                reason,
                world,
                ownerRef,
                blockIds,
                expireAtMillis,
                System.currentTimeMillis()
        ));
        LOG.info("[MOTM] Moving Terra terrain trail started: reason=" + reason
                + " expiresAt=" + expireAtMillis);
        return true;
    }

    private void startPlayerAnchor(PlayerData player,
                                   Ref<EntityStore> ownerRef,
                                   Store<EntityStore> store,
                                   long expireAtMillis,
                                   String completionEffectId) {
        startPlayerAnchor(player, ownerRef, store, "obsidian_skin", expireAtMillis, completionEffectId);
    }

    private void startPlayerAnchor(PlayerData player,
                                   Ref<EntityStore> ownerRef,
                                   Store<EntityStore> store,
                                   String reason,
                                   long expireAtMillis,
                                   String completionEffectId) {
        if (player == null || player.getPlayerId() == null || ownerRef == null || !ownerRef.isValid() || store == null) {
            return;
        }
        Vector3d anchor = getPosition(ownerRef, store);
        if (anchor == null) {
            return;
        }
        activePlayerAnchors.removeIf(existing -> player.getPlayerId().equals(existing.ownerPlayerId()));
        activePlayerAnchors.add(new ActivePlayerAnchor(
                reason == null || reason.isBlank() ? "player_anchor" : reason,
                player.getPlayerId(),
                ownerRef,
                new Vector3d(anchor),
                expireAtMillis,
                completionEffectId
        ));
        setAnchorMovementFreeze(ownerRef, store, true);
        zeroVelocity(ownerRef, store);
        LOG.info("[MOTM] Player anchor started: reason=" + (reason == null || reason.isBlank() ? "player_anchor" : reason)
                + " player=" + player.getPlayerName()
                + " anchor=" + formatVector(anchor));
    }

    private String placePersistentTerrainSelection(Player runtimePlayer,
                                                   AbilityData ability,
                                                   Vector3d center,
                                                   Vector3d forward,
                                                   Vector3d lineDirection,
                                                   long expireAtMillis) {
        if (runtimePlayer == null || ability == null || center == null) {
            return "";
        }

        String terrainEffect = lower(ability.getTerrainEffect());
        if (terrainEffect.contains("iron_wall")) {
            return placeIronWallSelection(runtimePlayer.getWorld(), "iron_wall", center, lineDirection,
                    Math.max(1, (int) Math.round(ability.getHeight() > 0 ? ability.getHeight() : 4.0)),
                    expireAtMillis);
        }
        if (terrainEffect.contains("lava_pool")) {
            restoreActiveTemporarySelections(runtimePlayer.getWorld(), "lava_pool");
            return placeFluidDiscSelection(runtimePlayer.getWorld(), "lava_pool", center, ability.getRadius(),
                    expireAtMillis, "Fluid_Lava", "Lava", "lava");
        }
        if (terrainEffect.contains("mudpit")) {
            String fluid = placeGroundedFluidDiscSelection(runtimePlayer.getWorld(), "mudpit", center, ability.getRadius(),
                    expireAtMillis, "Fluid_Water", "Water", "water");
            return fluid.isBlank() ? "" : fluid + " + brown debris visual";
        }
        if (terrainEffect.contains("stone_pillar")) {
            return placeStackingColumnSelection(runtimePlayer.getWorld(), "stone_pillar", center,
                    Math.max(1, (int) Math.round(ability.getHeight() > 0 ? ability.getHeight() : 4.0)),
                    expireAtMillis, "Rock_Stone_Brick_Pillar_Middle", "Rock_Stone_Brick");
        }
        return "";
    }

    private void placeSupplementalSurfaceCue(World world,
                                             AbilityData ability,
                                             Vector3d center,
                                             long expireAtMillis) {
        if (world == null || ability == null || center == null) {
            return;
        }
        String abilityId = lower(ability.getId());
        String terrainEffect = lower(ability.getTerrainEffect());
        if (terrainEffect.contains("dust_devil")) {
            placeRingBlockSelection(world, "dust_devil_sand", center, Math.max(1.6, ability.getRadius()),
                    Math.min(expireAtMillis, System.currentTimeMillis() + 3200L),
                    "Soil_Sand", "Rock_Sandstone", "Rock_Sandstone_White");
            return;
        }
        if (terrainEffect.contains("tunnel_path") || terrainEffect.contains("ruptured_earth")) {
            placeSurfacePatchSelection(world, abilityId.isBlank() ? "earth_movement" : abilityId,
                    center, 1, Math.min(expireAtMillis, System.currentTimeMillis() + 2600L),
                    "Soil_Dirt", "Rock_Stone", "Rock_Stone_Brick");
        }
    }

    private String placeAbilityTerrainSelection(Player runtimePlayer,
                                                PlayerData player,
                                                AbilityData ability,
                                                CastContext context,
                                                String reason) {
        return placeAbilityTerrainSelection(runtimePlayer, player, ability, context, reason, 0L);
    }

    private String placeAbilityTerrainSelection(Player runtimePlayer,
                                                PlayerData player,
                                                AbilityData ability,
                                                CastContext context,
                                                String reason,
                                                long forcedExpireAtMillis) {
        World world = runtimePlayer.getWorld();
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null && playerRef.isValid() ? playerRef.getStore() : null;
        Vector3d origin = playerRef != null && store != null ? getPosition(playerRef, store) : null;
        Vector3d forward = playerRef != null && store != null ? getDirection(playerRef, store) : null;
        if (world == null || origin == null || forward == null) {
            return "";
        }

        long expireAt = forcedExpireAtMillis > 0
                ? forcedExpireAtMillis
                : System.currentTimeMillis()
                + (long) (Math.max(2.0, ability.getDurationSeconds() > 0 ? ability.getDurationSeconds() : 4.0) * 1000);
        Vector3d center = context != null && context.targetBlock() != null
                ? new Vector3d(context.targetBlock().x + 0.5,
                context.targetBlock().y + 1.0,
                context.targetBlock().z + 0.5)
                : new Vector3d(origin).fma(3.5, new Vector3d(forward.x, 0.0, forward.z));

        return switch (reason) {
            case "obsidian_skin" -> "";
            case "rooted" -> placeSurfacePatchSelection(world, reason, origin, 1, expireAt,
                    "Plant_Roots_Leafy", "Plant_Roots_Cave", "Plant_Vine_Thick_Roots");
            case "sapling" -> placeSaplingMarkerSelection(world, reason, center, expireAt);
            case "nightshade" -> placeSurfaceColumnSelection(world, reason, center, 1, expireAt,
                    "Plant_Flower_Common_Purple", "Plant_Flower_Common_Blue");
            case "frolick" -> {
                boolean started = startMovingTerrainTrail(world, playerRef, reason, expireAt,
                        "Plant_Flower_Common_Purple", "Plant_Flower_Common_Yellow", "Plant_Flower_Common_Blue");
                yield started ? "moving flower trail" : "";
            }
            case "cacti_cluster" -> placeSurfaceColumnSelection(world, reason, center, 2, expireAt,
                    "Plant_Cactus_1", "Prototype_Cactus_Kit_Tall_Base", "Prototype_Cactus_One");
            case "lapidary" -> {
                String placed = placeFloatingClusterSelection(world, reason, center,
                        2, 2, 2, expireAt,
                        "Rock_Crystal_Green_Block", "Rock_Crystal_Green_Large", "Plant_Bush_Crystal");
                applyEffectById(playerRef, store, "MOTM_Proof_Gem_Green");
                String hpProxy = spawnLapidaryGemProxy(world, player, ability, center, expireAt);
                yield placed.isBlank()
                        ? "green gem aura" + hpProxy
                        : placed + " + green aura" + hpProxy;
            }
            case "fracture" -> {
                Vector3d gemCenter = resolveActiveLapidaryGemCenter(player, ability, store);
                Vector3d burstCenter = gemCenter != null ? gemCenter : center;
                String terrain = placeRingBlockSelection(world, reason, burstCenter, Math.max(2.0, ability.getRadius()),
                        Math.min(expireAt, System.currentTimeMillis() + 1200L),
                        "Rock_Crystal_Green_Block", "Rock_Crystal_Green_Large", "Plant_Bush_Crystal");
                yield terrain.isBlank() ? "green fracture burst" : "green fracture burst + " + terrain;
            }
            case "refraction" -> {
                Vector3d gemCenter = resolveActiveLapidaryGemCenter(player, ability, store);
                Vector3d auraCenter = gemCenter != null ? gemCenter : center;
                String terrain = placeRingBlockSelection(world, reason, auraCenter, Math.max(2.0, ability.getRadius()),
                        expireAt, "Rock_Crystal_Green_Block", "Rock_Crystal_Green_Large", "Plant_Bush_Crystal");
                yield terrain.isBlank() ? "green refraction aura" : "green refraction aura + " + terrain;
            }
            case "glare" -> {
                if (context != null && context.explicitTargetRef() != null) {
                    applyEffectById(context.explicitTargetRef(), store, "MOTM_Proof_Coating_Stone");
                    String terrain = placeSurfacePatchSelection(world, reason, center, 1, expireAt,
                            "Rock_Stone_Brick_Pillar_Middle", "Rock_Stone_Brick");
                    yield terrain.isBlank() ? "target stone coating" : "target stone coating + " + terrain;
                }
                yield "";
            }
            case "debris" -> placeTrailSelection(world, reason, origin, forward, System.currentTimeMillis() + 2400L,
                    "Soil_Dirt", "Rock_Stone", "Rock_Stone_Brick");
            case "sandstorm" -> placeRingBlockSelection(world, reason, origin, Math.max(2.0, ability.getRadius()),
                    System.currentTimeMillis() + 2200L, "Soil_Sand", "Rock_Sandstone", "Rock_Sandstone_White");
            case "tunnel" -> placeTrailSelection(world, reason, origin, forward, System.currentTimeMillis() + 2400L,
                    "Rock_Stone_Brick_Pillar_Middle", "Rock_Stone_Brick");
            default -> "";
        };
    }

    private String placeWallSelection(World world,
                                      String reason,
                                      Vector3d center,
                                      Vector3d lineDirection,
                                      int width,
                                      int height,
                                      long expireAtMillis,
                                      String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = surfaceOverlayAnchor(center);
        Vector3i rightStep = horizontalStep(lineDirection != null ? lineDirection : new Vector3d(1.0, 0.0, 0.0));
        BlockSelection selection = baseSelection(anchor);
        int half = width / 2;
        for (int x = 0; x < width; x++) {
            int offset = x - half;
            for (int y = 0; y < height; y++) {
                selection.addBlockAtWorldPos(
                        anchor.x + (rightStep.x * offset),
                        anchor.y + y,
                        anchor.z + (rightStep.z * offset),
                        blockTypeId, 0, 0, 0);
            }
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " blocks");
    }

    private String placeIronWallSelection(World world,
                                          String reason,
                                          Vector3d center,
                                          Vector3d lineDirection,
                                          int height,
                                          long expireAtMillis) {
        int primaryBlockTypeId = resolveRuntimeBlockTypeId("Metal_Iron");
        int secondaryBlockTypeId = resolveRuntimeBlockTypeId("Metal_Iron_Decorative", "Metal_Iron_Smooth", "Metal_Iron");
        if (world == null || center == null
                || primaryBlockTypeId == BlockType.UNKNOWN_ID || primaryBlockTypeId == BlockType.EMPTY_ID) {
            return "";
        }
        if (secondaryBlockTypeId == BlockType.UNKNOWN_ID || secondaryBlockTypeId == BlockType.EMPTY_ID) {
            secondaryBlockTypeId = primaryBlockTypeId;
        }

        Vector3i anchor = surfaceDecorationAnchor(center);
        restoreActiveTemporarySelections(world, reason);
        Vector3i rightStep = horizontalStep(lineDirection != null ? lineDirection : new Vector3d(1.0, 0.0, 0.0));
        BlockSelection selection = baseSelection(anchor);
        int wallHeight = Math.max(1, height);
        for (int x = 0; x < 3; x++) {
            int offset = x - 1;
            for (int y = 0; y < wallHeight; y++) {
                int blockTypeId = ((x + y) % 2 == 0) ? primaryBlockTypeId : secondaryBlockTypeId;
                selection.addBlockAtWorldPos(
                        anchor.x + (rightStep.x * offset),
                        anchor.y + y,
                        anchor.z + (rightStep.z * offset),
                        blockTypeId, 0, 0, 0);
            }
        }
        String summary = selection.getBlockCount() + " grounded mixed iron blocks";
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis, summary);
    }

    private boolean isIronWallAbility(AbilityData ability) {
        return ability != null
                && ("iron_wall".equals(lower(ability.getId())) || lower(ability.getTerrainEffect()).contains("iron_wall"));
    }

    private Vector3d resolveIronWallForward(Vector3d forward) {
        Vector3i step = horizontalStep(forward);
        return new Vector3d(step.x, 0.0, step.z);
    }

    private Vector3d resolveIronWallCenter(Vector3d origin, Vector3d forward) {
        if (origin == null || forward == null) {
            return null;
        }
        Vector3d horizontalForward = normalizeHorizontal(forward);
        return new Vector3d(
                origin.x + (horizontalForward.x * 1.75),
                origin.y,
                origin.z + (horizontalForward.z * 1.75)
        );
    }

    private Vector3d resolveStableCasterCenteredOrigin(String playerId, Vector3d origin) {
        if (playerId == null || playerId.isBlank() || origin == null) {
            return origin;
        }

        long now = System.currentTimeMillis();
        RecentPosition previous = recentCasterCenteredOriginByPlayer.get(playerId);
        if (previous != null
                && now - previous.recordedAtMillis() <= 10_000L
                && distance(previous.position(), origin) > 24.0) {
            LOG.warning("[MOTM] Caster-centered ability ignored implausible player-position jump: playerId=" + playerId
                    + " previous=" + formatVector(previous.position())
                    + " current=" + formatVector(origin));
            return new Vector3d(previous.position());
        }

        recentCasterCenteredOriginByPlayer.put(playerId, new RecentPosition(new Vector3d(origin), now));
        return origin;
    }

    private Vector3d resolveStableIronWallOrigin(String playerId, Vector3d origin) {
        if (playerId == null || playerId.isBlank() || origin == null) {
            return origin;
        }

        long now = System.currentTimeMillis();
        RecentPosition previous = recentIronWallOriginByPlayer.get(playerId);
        if (previous != null
                && now - previous.recordedAtMillis() <= 4_000L
                && distance(previous.position(), origin) > 24.0) {
            LOG.warning("[MOTM] Iron Wall ignored implausible player-position jump: playerId=" + playerId
                    + " previous=" + formatVector(previous.position())
                    + " current=" + formatVector(origin));
            return new Vector3d(previous.position());
        }

        recentIronWallOriginByPlayer.put(playerId, new RecentPosition(new Vector3d(origin), now));
        return origin;
    }

    private int pushTargetsOverlappingIronWall(Ref<EntityStore> playerRef,
                                               Store<EntityStore> store,
                                               AbilityData ability,
                                               Vector3d center,
                                               Vector3d forward,
                                               Vector3d lineDirection) {
        if (playerRef == null || store == null || ability == null || center == null || forward == null) {
            return 0;
        }

        Vector3d pushDirection = normalizeHorizontal(forward);
        Vector3d wallRight = normalizeHorizontal(lineDirection);
        double halfWidth = Math.max(1.5, ability.getWidth() > 0 ? ability.getWidth() / 2.0 : 1.5);
        int pushed = 0;

        for (Ref<EntityStore> targetRef : collectNearbyNpcTargets(store, center, halfWidth + 2.0, 8)) {
            Vector3d targetPosition = getPosition(targetRef, store);
            if (targetPosition == null) {
                continue;
            }
            Vector3d fromCenter = subtract(targetPosition, center);
            double axial = dot(fromCenter, pushDirection);
            double lateral = dot(fromCenter, wallRight);
            double vertical = Math.abs(targetPosition.y - center.y);
            if (Math.abs(axial) > 1.25 || Math.abs(lateral) > halfWidth + 0.75 || vertical > 3.25) {
                continue;
            }

            NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
            if (npc == null) {
                continue;
            }
            Vector3d destination = new Vector3d(targetPosition).fma(
                    ability.getKnockbackForce() > 0 ? Math.min(ability.getKnockbackForce(), 4.0) : 3.0,
                    pushDirection);
            npc.moveTo(targetRef, destination.x, destination.y, destination.z, store);
            pushed++;
        }

        if (pushed > 0) {
            LOG.info("[MOTM] Iron Wall spawn-overlap push: pushed=" + pushed
                    + " center=" + formatVector(center));
        }
        return pushed;
    }

    private String placeSurfacePatchSelection(World world,
                                              String reason,
                                              Vector3d center,
                                              int radius,
                                              long expireAtMillis,
                                              String... blockIds) {
        return placeSurfacePatchSelection(world, reason, center, radius, expireAtMillis, true, blockIds);
    }

    private String placeSurfacePatchSelection(World world,
                                              String reason,
                                              Vector3d center,
                                              int radius,
                                              long expireAtMillis,
                                              boolean elevated,
                                              String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = elevated ? surfaceOverlayAnchor(center) : fluidGroundAnchor(center);
        BlockSelection selection = baseSelection(anchor);
        Set<String> protectedKeys = new LinkedHashSet<>();
        int r = Math.max(0, radius);
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double dist = Math.sqrt((x * x) + (z * z));
                if (dist > r + 0.25) {
                    continue;
                }
                int worldX = anchor.x + x;
                int worldZ = anchor.z + z;
                selection.addBlockAtWorldPos(worldX, anchor.y, worldZ, blockTypeId, 0, 0, 0);
                protectedKeys.add(blockKey(worldX, anchor.y, worldZ));
            }
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " surface decoration blocks", protectedKeys);
    }

    private String placeFloatingClusterSelection(World world,
                                                 String reason,
                                                 Vector3d center,
                                                 int width,
                                                 int height,
                                                 int depth,
                                                 long expireAtMillis,
                                                 String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = new Vector3i(
                (int) Math.floor(center.x),
                (int) Math.floor(center.y) + 1,
                (int) Math.floor(center.z)
        );
        BlockSelection selection = baseSelection(anchor);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                for (int z = 0; z < depth; z++) {
                    selection.addBlockAtWorldPos(anchor.x + x, anchor.y + y, anchor.z + z, blockTypeId, 0, 0, 0);
                }
            }
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " floating green gem cube blocks");
    }

    private String spawnLapidaryGemProxy(World world,
                                         PlayerData player,
                                         AbilityData ability,
                                         Vector3d center,
                                         long expireAtMillis) {
        if (world == null || player == null || ability == null || center == null) {
            return "";
        }

        activeLapidaryGems.removeIf(gem -> player.getPlayerId().equals(gem.ownerPlayerId)
                && despawnLapidaryGem(gem));

        double gemHealth = Math.max(1.0, resolvePlayerMaxHealth(player.getPlayerId()) * Math.max(0.10, ability.getShieldPercent() / 100.0));
        Vector3d proxyPosition = new Vector3d(center).add(1.0, 2.35, 1.0);
        NPCEntity proxy = new NPCEntity(world);
        proxy.setRoleName("Spark_Living");
        proxy.setDespawnTime((float) Math.max(1.0, ((expireAtMillis - System.currentTimeMillis()) / 1000.0) + 0.5));
        world.spawnEntity(proxy, proxyPosition, new Rotation3f(0f, 0f, 0f));

        Ref<EntityStore> proxyRef = proxy.getReference();
        if (proxyRef == null || !proxyRef.isValid() || proxyRef.getStore() == null) {
            return "";
        }

        Store<EntityStore> store = proxyRef.getStore();
        String label = lapidaryGemLabel(gemHealth, gemHealth);
        applyLapidaryGemLabel(proxyRef, store, label);
        applyEffectById(proxyRef, store, "MOTM_Proof_Gem_Green");
        visualProxyRefs.add(proxyRef);
        activeLapidaryGems.add(new ActiveLapidaryGem(
                player.getPlayerId(),
                proxyRef,
                new Vector3d(center),
                gemHealth,
                gemHealth,
                expireAtMillis,
                label
        ));
        LOG.info("[MOTM] Lapidary gem HP proxy spawned: owner=" + player.getPlayerName()
                + " hp=" + AbilityPresentation.formatDecimal(gemHealth)
                + " position=" + formatVector(proxyPosition));
        return " + HP nameplate";
    }

    private boolean processLapidaryGem(ActiveLapidaryGem gem,
                                       Store<EntityStore> currentStore,
                                       long now) {
        if (gem == null || gem.ref == null || !gem.ref.isValid()) {
            return true;
        }
        if (!belongsToCurrentStore(gem.ref, currentStore)) {
            return false;
        }
        Store<EntityStore> store = gem.ref.getStore();
        if (store == null || now >= gem.expireAtMillis) {
            return despawnLapidaryGem(gem);
        }

        double current = resolveCurrentHealth(gem.ref, store);
        if (current <= 0.0) {
            current = gem.currentHp;
        }
        current = clamp(current, 0.0, gem.maxHp);
        String label = lapidaryGemLabel(current, gem.maxHp);
        if (!label.equals(gem.lastLabel)) {
            applyLapidaryGemLabel(gem.ref, store, label);
            gem.lastLabel = label;
            gem.currentHp = current;
        }
        return false;
    }

    private void applyLapidaryGemLabel(Ref<EntityStore> ref, Store<EntityStore> store, String label) {
        if (ref == null || !ref.isValid() || store == null || label == null) {
            return;
        }
        store.putComponent(ref, Nameplate.getComponentType(), new Nameplate(label));
        store.putComponent(ref, DisplayNameComponent.getComponentType(),
                new DisplayNameComponent(Message.raw(label).color("#6CFF8C")));
    }

    private boolean despawnLapidaryGem(ActiveLapidaryGem gem) {
        if (gem == null || gem.ref == null || !gem.ref.isValid()) {
            return true;
        }
        Store<EntityStore> store = gem.ref.getStore();
        if (store != null) {
            NPCEntity npc = store.getComponent(gem.ref, NPCEntity.getComponentType());
            if (npc != null && !npc.isDespawning()) {
                npc.setToDespawn();
            }
        }
        visualProxyRefs.remove(gem.ref);
        return true;
    }

    private String lapidaryGemLabel(double currentHp, double maxHp) {
        return "Lapidary HP "
                + Math.max(0, (int) Math.ceil(currentHp))
                + "/"
                + Math.max(1, (int) Math.ceil(maxHp));
    }

    private String placeSurfaceColumnSelection(World world,
                                               String reason,
                                               Vector3d center,
                                               int height,
                                               long expireAtMillis,
                                               String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = surfaceOverlayAnchor(center);
        BlockSelection selection = baseSelection(anchor);
        Set<String> protectedKeys = new LinkedHashSet<>();
        int h = Math.max(1, height);
        for (int y = 0; y < h; y++) {
            int worldY = anchor.y + y;
            selection.addBlockAtWorldPos(anchor.x, worldY, anchor.z, blockTypeId, 0, 0, 0);
            protectedKeys.add(blockKey(anchor.x, worldY, anchor.z));
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " surface column blocks", protectedKeys);
    }

    private String placeSaplingMarkerSelection(World world,
                                               String reason,
                                               Vector3d center,
                                               long expireAtMillis) {
        int blockTypeId = resolveRuntimeBlockTypeId(SAPLING_MARKER_BLOCK_ID, "Plant_Sapling_Oak");
        if (world == null || center == null || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = surfaceOverlayAnchor(center);
        BlockSelection selection = baseSelection(anchor);
        selection.addBlockAtWorldPos(anchor.x, anchor.y, anchor.z, blockTypeId, 0, 0, 0);
        Set<String> protectedKeys = Set.of(blockKey(anchor.x, anchor.y, anchor.z));
        String terrain = placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                "1 emerald temple statue marker", protectedKeys);
        if (!terrain.isBlank()) {
            spawnStaticMarkerGlow(world,
                    new Vector3d(anchor.x + 0.5, anchor.y + 0.65, anchor.z + 0.5),
                    SAPLING_MARKER_GLOW_EFFECT_ID,
                    expireAtMillis);
        }
        return terrain;
    }

    private String placeStackingColumnSelection(World world,
                                                String reason,
                                                Vector3d center,
                                                int height,
                                                long expireAtMillis,
                                                String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = surfaceDecorationAnchor(center);
        ActiveStackingColumn column = new ActiveStackingColumn(
                reason,
                world,
                anchor,
                blockTypeId,
                Math.max(1, height),
                expireAtMillis,
                System.currentTimeMillis()
        );
        activeStackingColumns.add(column);
        LOG.info("[MOTM] Temporary Terra stacking column started: reason=" + reason
                + " anchor=" + anchor
                + " height=" + column.height);
        return "terrain staged " + column.height + " stone pillar blocks";
    }

    private String placeColumnSelection(World world,
                                        String reason,
                                        Vector3d center,
                                        int width,
                                        int height,
                                        long expireAtMillis,
                                        String... blockIds) {
        return placeWallSelection(world, reason, center, new Vector3d(1.0, 0.0, 0.0),
                width, height, expireAtMillis, blockIds);
    }

    private String placeRingBlockSelection(World world,
                                           String reason,
                                           Vector3d center,
                                           double radius,
                                           long expireAtMillis,
                                           String... blockIds) {
        return placeRingBlockSelection(world, reason, center, radius, expireAtMillis, true, blockIds);
    }

    private String placeRingBlockSelection(World world,
                                           String reason,
                                           Vector3d center,
                                           double radius,
                                           long expireAtMillis,
                                           boolean elevated,
                                           String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = elevated ? surfaceOverlayAnchor(center) : fluidGroundAnchor(center);
        BlockSelection selection = baseSelection(anchor);
        Set<String> protectedKeys = new LinkedHashSet<>();
        int ring = Math.max(1, (int) Math.round(radius));
        for (int x = -ring; x <= ring; x++) {
            for (int z = -ring; z <= ring; z++) {
                double dist = Math.sqrt((x * x) + (z * z));
                if (dist < ring - 0.4 || dist > ring + 0.4) {
                    continue;
                }
                int worldX = anchor.x + x;
                int worldZ = anchor.z + z;
                selection.addBlockAtWorldPos(worldX, anchor.y, worldZ, blockTypeId, 0, 0, 0);
                protectedKeys.add(blockKey(worldX, anchor.y, worldZ));
            }
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " ring blocks", protectedKeys);
    }

    private String placeTrailSelection(World world,
                                       String reason,
                                       Vector3d origin,
                                       Vector3d forward,
                                       long expireAtMillis,
                                       String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || origin == null || forward == null
                || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        Vector3i anchor = surfaceOverlayAnchor(origin);
        BlockSelection selection = baseSelection(anchor);
        Set<String> protectedKeys = new LinkedHashSet<>();
        Vector3d back = new Vector3d(-forward.x, 0.0, -forward.z);
        if (!back.isFinite() || back.length() < 0.001) {
            back = new Vector3d(0.0, 0.0, -1.0);
        } else {
            back.normalize();
        }
        for (int i = 1; i <= 4; i++) {
            Vector3d pos = new Vector3d(origin).fma(i, back);
            Vector3i block = surfaceOverlayAnchor(pos);
            selection.addBlockAtWorldPos(block.x, block.y, block.z, blockTypeId, 0, 0, 0);
            protectedKeys.add(blockKey(block));
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " trail flowers", protectedKeys);
    }

    private String placeObsidianBlockShellSelection(World world,
                                                    String reason,
                                                    Vector3d center,
                                                    long expireAtMillis,
                                                    String... blockIds) {
        int blockTypeId = resolveRuntimeBlockTypeId(blockIds);
        if (world == null || center == null
                || blockTypeId == BlockType.UNKNOWN_ID || blockTypeId == BlockType.EMPTY_ID) {
            return "";
        }

        restoreActiveTemporarySelections(world, reason);
        Vector3i anchor = blockAnchor(center);
        BlockSelection selection = baseSelection(anchor);
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y < 4; y++) {
                for (int z = -1; z <= 1; z++) {
                    boolean side = Math.abs(x) == 1 || Math.abs(z) == 1;
                    if (!side) {
                        continue;
                    }
                    selection.addBlockAtWorldPos(
                            anchor.x + x,
                            anchor.y + y,
                            anchor.z + z,
                            blockTypeId,
                            0,
                            0,
                            0);
                }
            }
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getBlockCount() + " offset obsidian shell blocks");
    }

    private String placeFluidDiscSelection(World world,
                                           String reason,
                                           Vector3d center,
                                           double radius,
                                           long expireAtMillis,
                                           String... fluidIds) {
        int fluidTypeId = resolveRuntimeFluidTypeId(fluidIds);
        Fluid fluid = fluidTypeId != Fluid.UNKNOWN_ID && fluidTypeId != Fluid.EMPTY_ID
                ? Fluid.getAssetMap().getAsset(fluidTypeId)
                : null;
        if (world == null || center == null || fluid == null || fluid.isUnknown()) {
            return "";
        }

        Vector3i anchor = blockAnchor(center);
        BlockSelection selection = baseSelection(anchor);
        int r = Math.max(1, (int) Math.round(radius));
        byte fluidLevel = (byte) Math.max(1, fluid.getMaxFluidLevel());
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                double dist = Math.sqrt((x * x) + (z * z));
                if (dist > r + 0.2) {
                    continue;
                }
                selection.addFluidAtWorldPos(anchor.x + x, anchor.y, anchor.z + z, fluidTypeId, fluidLevel);
            }
        }
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis,
                selection.getFluidCount() + " fluids");
    }

    private String placeGroundedFluidDiscSelection(World world,
                                                   String reason,
                                                   Vector3d center,
                                                   double radius,
                                                   long expireAtMillis,
                                                   String... fluidIds) {
        if (center == null) {
            return "";
        }
        Vector3d grounded = new Vector3d(center.x, center.y - 1.0, center.z);
        return placeFluidDiscSelection(world, reason, grounded, radius, expireAtMillis, fluidIds);
    }

    private String placeTemporarySelection(World world,
                                           String reason,
                                           Vector3i anchor,
                                           BlockSelection selection,
                                           long expireAtMillis,
                                           String summary) {
        return placeTemporarySelection(world, reason, anchor, selection, expireAtMillis, summary, Set.of());
    }

    private String placeTemporarySelection(World world,
                                           String reason,
                                           Vector3i anchor,
                                           BlockSelection selection,
                                           long expireAtMillis,
                                           String summary,
                                           Set<String> protectedBlockKeys) {
        if (world == null || anchor == null || selection == null) {
            return "";
        }
        try {
            BlockSelection original = selection.place(null, world, new Vector3i(0, 0, 0), BlockMask.EMPTY);
            Set<String> protectedKeys = protectedBlockKeys == null || protectedBlockKeys.isEmpty()
                    ? Set.of()
                    : Set.copyOf(protectedBlockKeys);
            activeTemporaryTerrainBlockKeys.addAll(protectedKeys);
            activeTerrainSelections.add(new TemporaryTerrainSelection(
                    reason,
                    world,
                    anchor,
                    original,
                    protectedKeys,
                    Math.max(System.currentTimeMillis() + 1200L, expireAtMillis)
            ));
            LOG.info("[MOTM] Temporary Terra terrain placed: reason=" + reason
                    + " anchor=" + anchor
                    + " summary=" + summary);
            if (isSurfaceDecorationReason(reason)) {
                LOG.info("[MOTM] surface_place_result ability=" + reason
                        + " placedOnTop=true"
                        + " replacedOriginalBlock=false"
                        + " protectedBlocks=" + protectedKeys.size());
            }
            mod.recordServerTruth("temporary_selection_placed", null, MotmObservability.mapOf(
                    "reason", reason,
                    "anchor", "(" + anchor.x + "," + anchor.y + "," + anchor.z + ")",
                    "blockCount", selection.getBlockCount(),
                    "fluidCount", selection.getFluidCount(),
                    "expireAtMillis", expireAtMillis,
                    "summary", summary
            ));
            return "terrain " + summary;
        } catch (Throwable e) {
            LOG.warning("[MOTM] Temporary Terra terrain placement failed: reason=" + reason
                    + " anchor=" + anchor
                    + " error=" + e.getMessage());
            return "";
        }
    }

    private boolean isSurfaceDecorationReason(String reason) {
        String normalized = lower(reason);
        return normalized.contains("frolick")
                || normalized.contains("sapling")
                || normalized.contains("nightshade")
                || normalized.contains("rooted")
                || normalized.contains("vines")
                || normalized.contains("sinkhole_cracks");
    }

    public boolean isTemporaryAbilityTerrainBlock(Vector3i block) {
        return block != null && activeTemporaryTerrainBlockKeys.contains(blockKey(block));
    }

    private static String blockKey(Vector3i block) {
        return block == null ? "" : blockKey(block.x, block.y, block.z);
    }

    private static String blockKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private BlockSelection baseSelection(Vector3i anchor) {
        BlockSelection selection = new BlockSelection();
        selection.setPosition(anchor.x, anchor.y, anchor.z);
        selection.setAnchorAtWorldPos(anchor.x, anchor.y, anchor.z);
        return selection;
    }

    private Vector3i blockAnchor(Vector3d center) {
        return MotmPlaybackGeometry.blockAnchor(center);
    }

    private Vector3i fluidGroundAnchor(Vector3d center) {
        return MotmPlaybackGeometry.fluidGroundAnchor(center);
    }

    private Vector3i surfaceDecorationAnchor(Vector3d center) {
        return MotmPlaybackGeometry.surfaceDecorationAnchor(center);
    }

    private Vector3i surfaceOverlayAnchor(Vector3d center) {
        Vector3i anchor = surfaceDecorationAnchor(center);
        return new Vector3i(anchor.x, anchor.y + 1, anchor.z);
    }

    private boolean sameBlock(Vector3i first, Vector3i second) {
        return MotmPlaybackGeometry.sameBlock(first, second);
    }

    private Vector3d normalizeHorizontal(Vector3d vector) {
        return MotmPlaybackGeometry.normalizeHorizontal(vector);
    }

    private Vector3i horizontalRightStep(Vector3d direction) {
        return MotmPlaybackGeometry.horizontalRightStep(direction);
    }

    private Vector3i horizontalStep(Vector3d direction) {
        return MotmPlaybackGeometry.horizontalStep(direction);
    }

    private int resolveRuntimeBlockTypeId(String... blockIds) {
        for (String blockId : blockIds) {
            try {
                int id = BlockType.getBlockIdOrUnknown(blockId, "MOTM Terra runtime terrain");
                if (id != BlockType.UNKNOWN_ID && id != BlockType.EMPTY_ID) {
                    return id;
                }
            } catch (Throwable e) {
                LOG.warning("[MOTM] Terra runtime block candidate skipped: id=" + blockId
                        + " error=" + e.getMessage());
            }
        }
        return BlockType.UNKNOWN_ID;
    }

    private int resolveRuntimeFluidTypeId(String... fluidIds) {
        for (String fluidId : fluidIds) {
            int id = Fluid.getAssetMap().getIndexOrDefault(fluidId, Fluid.UNKNOWN_ID);
            if (id != Fluid.UNKNOWN_ID && id != Fluid.EMPTY_ID) {
                return id;
            }
        }
        for (String fluidId : fluidIds) {
            int id = Fluid.getFluidIdOrUnknown(fluidId, "MOTM Terra runtime fluid");
            if (id != Fluid.UNKNOWN_ID && id != Fluid.EMPTY_ID) {
                return id;
            }
        }
        return Fluid.UNKNOWN_ID;
    }

    private void registerFieldRuntime(String ownerPlayerId,
                                      Ref<EntityStore> ownerRef,
                                      String classId,
                                      String styleId,
                                      AbilityData ability,
                                      Vector3d center,
                                      Vector3d forwardDirection,
                                      Vector3d lineDirection,
                                      double radius,
                                      double halfWidth,
                                      double thickness,
                                      long activateAtMillis,
                                      long expireAtMillis,
                                      boolean followOwner,
                                      FieldVisualRuntime visual) {
        activeFields.add(new ActiveField(
                ownerPlayerId,
                ownerRef,
                classId,
                styleId,
                ability,
                center,
                forwardDirection,
                lineDirection,
                radius,
                halfWidth,
                thickness,
                expireAtMillis,
                activateAtMillis,
                activateAtMillis,
                followOwner,
                visual.visualRefs(),
                visual.loopEffectId(),
                visual.nextRefreshAtMillis(),
                mod.currentObservabilityTraceId()
        ));
    }

    private boolean shouldCreateMovementTrail(AbilityData ability, PlaybackResult playback) {
        if (ability == null || playback == null || !playback.movementApplied()
                || playback.startPosition() == null || playback.endPosition() == null) {
            return false;
        }
        String terrainEffect = lower(ability.getTerrainEffect());
        return terrainEffect.contains("ember_trail")
                || terrainEffect.contains("ice_skate_trail")
                || terrainEffect.contains("dust_devil")
                || terrainEffect.contains("tunnel_path")
                || terrainEffect.contains("ruptured_earth");
    }

    private boolean shouldCreatePersonalAuraField(AbilityData ability) {
        if (ability == null) {
            return false;
        }
        String terrainEffect = lower(ability.getTerrainEffect());
        String castType = lower(ability.getCastType());
        return ("self_burst".equals(castType) && (
                terrainEffect.contains("living_flame")
                        || terrainEffect.contains("pressure_burst")
        ))
                || ("self_buff".equals(castType) && (
                terrainEffect.contains("cyclone_shield")
                        || terrainEffect.contains("eye_of_the_storm")
                        || terrainEffect.contains("root_circle")
                        || terrainEffect.contains("ice_shell")
                        || terrainEffect.contains("mist_shroud")
                        || terrainEffect.contains("condensation_veil")
                        || terrainEffect.contains("vanish")
                        || terrainEffect.contains("umbral_shroud")
                        || terrainEffect.contains("resonant_aura")
                        || terrainEffect.contains("purifying_aura")
                        || terrainEffect.contains("psychic_link")
                        || terrainEffect.contains("steam_pressure")
                        || terrainEffect.contains("sandstorm")
        ));
    }

    private int resolveTrailNodeCount(AbilityData ability) {
        String terrainEffect = lower(ability.getTerrainEffect());
        if (terrainEffect.contains("ember_trail")) {
            return 4;
        }
        if (terrainEffect.contains("ice_skate_trail")) {
            return 3;
        }
        if (terrainEffect.contains("dust_devil")) {
            return 4;
        }
        return 3;
    }

    private double resolveTrailRadius(AbilityData ability) {
        String terrainEffect = lower(ability.getTerrainEffect());
        if (terrainEffect.contains("ember_trail")) {
            return 2.4;
        }
        if (terrainEffect.contains("ice_skate_trail")) {
            return 2.1;
        }
        if (terrainEffect.contains("dust_devil")) {
            return Math.max(2.6, ability.getRadius());
        }
        return 2.2;
    }

    private double resolveAuraRadius(AbilityData ability) {
        String terrainEffect = lower(ability.getTerrainEffect());
        if (terrainEffect.contains("living_flame")) {
            return Math.max(3.8, ability.getRadius() > 0 ? ability.getRadius() : 4.0);
        }
        if (terrainEffect.contains("pressure_burst")) {
            return 4.6;
        }
        if (terrainEffect.contains("eye_of_the_storm")) {
            return 4.5;
        }
        if (terrainEffect.contains("cyclone_shield")) {
            return 3.8;
        }
        if (terrainEffect.contains("resonant_aura")) {
            return 4.2;
        }
        if (terrainEffect.contains("ice_shell")) {
            return 3.4;
        }
        if (terrainEffect.contains("mist_shroud")
                || terrainEffect.contains("condensation_veil")
                || terrainEffect.contains("vanish")
                || terrainEffect.contains("umbral_shroud")) {
            return 3.6;
        }
        if (terrainEffect.contains("purifying_aura")) {
            return 3.7;
        }
        if (terrainEffect.contains("psychic_link")) {
            return 4.0;
        }
        if (terrainEffect.contains("steam_pressure")) {
            return 3.5;
        }
        if (terrainEffect.contains("root_circle")) {
            return 3.4;
        }
        if (terrainEffect.contains("sandstorm")) {
            return Math.max(4.0, ability.getRadius());
        }
        return Math.max(2.4, ability.getRadius());
    }

    private List<Vector3d> buildTrailCenters(Vector3d start, Vector3d end, int nodes) {
        if (start == null || end == null || nodes <= 0) {
            return List.of();
        }

        List<Vector3d> centers = new ArrayList<>();
        Vector3d segment = subtract(end, start);
        int count = Math.max(2, nodes);
        for (int index = 0; index < count; index++) {
            double factor = count == 1 ? 1.0 : index / (double) (count - 1);
            centers.add(new Vector3d(start).fma(factor, segment));
        }
        return List.copyOf(centers);
    }

    private Vector3d currentForward(Vector3d direction) {
        if (direction == null || !direction.isFinite()) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        Vector3d forward = new Vector3d(direction.x, 0.0, direction.z);
        if (!forward.isFinite() || forward.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        forward.normalize();
        return forward;
    }

    private boolean processProjectileTick(ActiveProjectile projectile, long now) {
        String previousTraceId = mod.enterObservabilityTrace(projectile.traceId());
        try {
        if (projectile.ownerRef() == null || !projectile.ownerRef().isValid()) {
            return true;
        }

        Store<EntityStore> store = projectile.ownerRef().getStore();
        if (store == null) {
            despawnProjectileVisual(projectile);
            return true;
        }

        PlayerData player = mod.getPlayerDataManager().getOnlinePlayer(projectile.ownerPlayerId());
        if (player == null) {
            despawnProjectileVisual(projectile);
            return true;
        }

        if (now < projectile.activateAtMillis()) {
            refreshProjectileVisual(projectile, now);
            return false;
        }

        Vector3d from = new Vector3d(projectile.position());
        Vector3d stepDirection = normalize(projectile.direction());
        double stepDistance = Math.min(projectile.speedPerTick(), MAX_PROJECTILE_STEP_DISTANCE);
        Vector3d to = new Vector3d(from).fma(stepDistance, stepDirection);

        projectile.position().x = to.x;
        projectile.position().y = to.y;
        projectile.position().z = to.z;
        projectile.travelledDistance += stepDistance;
        syncProjectileVisual(projectile, now);

        if (isMagmaSlingAbility(projectile.ability()) && projectile.travelledDistance() < 2.25 && !expiredByTimeOrRange(projectile, now)) {
            return false;
        }

        boolean expired = now >= projectile.expireAtMillis() || projectile.travelledDistance() >= projectile.maxDistance();
        boolean groundMarkerLanded = isGroundMarkerProjectile(projectile.ability())
                && hasGroundMarkerReachedSurface(projectile, store, to);
        if (isGroundMarkerProjectile(projectile.ability())) {
            if (expired || groundMarkerLanded) {
                placeProjectileGroundMarkerImpact(projectile, player, store, to);
                LOG.info("[MOTM] projectile_despawn abilityId=" + safe(projectile.ability().getId())
                        + " reason=" + (groundMarkerLanded ? "ground_marker_surface" : "ground_marker_expired")
                        + " flightTicks=" + Math.max(0, Math.round((now - projectile.activateAtMillis()) / 50.0))
                        + " travelled=" + AbilityPresentation.formatDecimal(projectile.travelledDistance())
                        + " position=" + formatVector(to));
                despawnProjectileVisual(projectile);
            }
            return expired || groundMarkerLanded;
        }

        if (isPiercingProjectile(projectile.ability())) {
            applyProjectileTraversalHits(projectile, player, store, from, to);
        }

        Ref<EntityStore> directHit = resolveProjectileHit(projectile, store, from, to);
        if (isPiercingProjectile(projectile.ability())) {
            if (expired) {
                despawnProjectileVisual(projectile);
            }
            return expired;
        }
        if (directHit == null && !expired) {
            return false;
        }

        applyProjectileImpact(projectile, player, store, to, directHit);
        LOG.info("[MOTM] projectile_despawn abilityId=" + safe(projectile.ability().getId())
                + " reason=" + (directHit != null ? "direct_hit" : "expired")
                + " hitTarget=" + (directHit != null && directHit.isValid() ? resolveEntityId(directHit, store) : "none")
                + " flightTicks=" + Math.max(0, Math.round((now - projectile.activateAtMillis()) / 50.0))
                + " travelled=" + AbilityPresentation.formatDecimal(projectile.travelledDistance())
                + " position=" + formatVector(to));
        if (shouldLeaveProjectileVisualOnImpact(projectile.ability())) {
            visualProxyRefs.remove(projectile.visualRef());
        } else {
            despawnProjectileVisual(projectile);
        }
        return true;
        } finally {
            mod.restoreObservabilityTrace(previousTraceId);
        }
    }

    private boolean shouldLeaveProjectileVisualOnImpact(AbilityData ability) {
        return false;
    }

    private boolean expiredByTimeOrRange(ActiveProjectile projectile, long now) {
        return projectile != null
                && (now >= projectile.expireAtMillis()
                || projectile.travelledDistance() >= projectile.maxDistance());
    }

    private boolean isMagmaSlingAbility(AbilityData ability) {
        return ability != null && "magma_sling".equals(lower(ability.getId()));
    }

    private boolean isVinesAbility(AbilityData ability) {
        return ability != null && "vines".equals(lower(ability.getId()));
    }

    private boolean isGroundMarkerProjectile(AbilityData ability) {
        if (ability == null) {
            return false;
        }
        String abilityId = lower(ability.getId());
        return "sapling".equals(abilityId) || "nightshade".equals(abilityId);
    }

    private boolean hasGroundMarkerReachedSurface(ActiveProjectile projectile,
                                                  Store<EntityStore> store,
                                                  Vector3d position) {
        if (projectile == null || store == null || position == null) {
            return false;
        }
        Vector3d ownerPosition = getPosition(projectile.ownerRef(), store);
        if (ownerPosition == null) {
            return false;
        }
        return projectile.direction().y < -0.05
                && projectile.travelledDistance() >= 1.5
                && position.y <= ownerPosition.y + 0.15;
    }

    private void placeProjectileGroundMarkerImpact(ActiveProjectile projectile,
                                                   PlayerData player,
                                                   Store<EntityStore> store,
                                                   Vector3d impactPosition) {
        if (projectile == null || projectile.ability() == null || store == null
                || store.getExternalData() == null || impactPosition == null) {
            return;
        }

        World world = store.getExternalData().getWorld();
        if (world == null) {
            return;
        }

        long expireAt = System.currentTimeMillis()
                + (long) (Math.max(4.0, projectile.ability().getDurationSeconds()) * 1000);
        Vector3d groundedImpact = groundMarkerImpactPosition(projectile, store, impactPosition);
        String abilityId = lower(projectile.ability().getId());
        String terrain;
        if ("sapling".equals(abilityId)) {
            terrain = placeSaplingMarkerSelection(world, "sapling", groundedImpact, expireAt);
        } else {
            terrain = placeSurfaceColumnSelection(world, "nightshade", groundedImpact, 1, expireAt,
                    "Plant_Flower_Common_Purple", "Plant_Flower_Common_Blue");
        }

        int lured = applyMarkerLure(projectile, player, store, groundedImpact);
        LOG.info("[MOTM][terra-audit] event=ground_marker_projectile.impact abilityId=" + abilityId
                + " position=" + formatVector(groundedImpact)
                + " terrain=" + terrain
                + " lured=" + lured);
    }

    private Vector3d groundMarkerImpactPosition(ActiveProjectile projectile,
                                                Store<EntityStore> store,
                                                Vector3d impactPosition) {
        Vector3d ownerPosition = projectile != null ? getPosition(projectile.ownerRef(), store) : null;
        if (ownerPosition == null || impactPosition == null) {
            return impactPosition;
        }
        return new Vector3d(impactPosition.x, ownerPosition.y, impactPosition.z);
    }

    private int applyMarkerLure(ActiveProjectile projectile,
                                PlayerData player,
                                Store<EntityStore> store,
                                Vector3d impactPosition) {
        if (projectile == null || player == null || store == null || impactPosition == null) {
            return 0;
        }
        int applied = 0;
        double radius = Math.max(4.0, projectile.ability().getRadius() > 0 ? projectile.ability().getRadius() : 5.0);
        for (Ref<EntityStore> targetRef : collectNearbyNpcTargets(store, impactPosition, radius, 8)) {
            if (targetRef == null || !targetRef.isValid()) {
                continue;
            }
            NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
            Vector3d targetPosition = getPosition(targetRef, store);
            if (npc == null || npc.isDespawning() || targetPosition == null) {
                continue;
            }
            Vector3d toMarker = subtract(impactPosition, targetPosition);
            toMarker.y = 0.0;
            if (!toMarker.isFinite() || toMarker.length() < 0.001) {
                continue;
            }
            toMarker.normalize();
            Vector3d destination = new Vector3d(targetPosition).fma(
                    Math.min(2.25, distance(targetPosition, impactPosition)),
                    toMarker);
            npc.moveTo(targetRef, destination.x, destination.y, destination.z, store);
            applyTargetToken("disoriented", targetRef, store, projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            applied++;
        }
        return applied;
    }

    private boolean processFieldTick(ActiveField field, long now) {
        String previousTraceId = mod.enterObservabilityTrace(field.traceId());
        try {
        if (field.ownerRef() == null || !field.ownerRef().isValid()) {
            releaseSinkholeField(field, null);
            despawnFieldVisual(field);
            return true;
        }

        Store<EntityStore> store = field.ownerRef().getStore();
        if (store == null) {
            releaseSinkholeField(field, null);
            despawnFieldVisual(field);
            return true;
        }

        syncFollowOwnerFieldAnchor(field, store);

        if (now >= field.expireAtMillis()) {
            clearLavaPoolOwnerVelocityBoost(field.ownerPlayerId(), field.ownerRef(), store);
            restoreFieldTemporaryTerrain(field, store);
            releaseSinkholeField(field, store);
            despawnFieldVisual(field);
            return true;
        }

        applyLavaPoolOwnerMobility(field, store);

        if (now < field.activateAtMillis()) {
            refreshFieldVisual(field, now);
            return false;
        }

        if (isSinkhole(field.ability())) {
            engageSinkholeField(field, store);
        }

        syncFieldVisual(field, now);
        if (now < field.nextPulseAtMillis()) {
            return false;
        }

        PlayerData player = mod.getPlayerDataManager().getOnlinePlayer(field.ownerPlayerId());
        if (player == null) {
            releaseSinkholeField(field, store);
            despawnFieldVisual(field);
            return true;
        }

        List<Ref<EntityStore>> targets = collectFieldTargets(field, store);
        if (!targets.isEmpty()) {
            applyFieldPulse(field, player, store, targets);
        }
        applyFieldSupportPulse(field, player);
        applySinkholeSuffocationPulse(field, store);
        field.nextPulseAtMillis = now + FIELD_PULSE_INTERVAL_MS;
        return false;
        } finally {
            mod.restoreObservabilityTrace(previousTraceId);
        }
    }

    private void syncFollowOwnerFieldAnchor(ActiveField field,
                                            Store<EntityStore> store) {
        if (field == null || !field.followOwner()) {
            return;
        }

        Vector3d ownerPosition = getPosition(field.ownerRef(), store);
        if (ownerPosition == null) {
            return;
        }

        field.center = new Vector3d(ownerPosition);
    }

    private Ref<EntityStore> resolveProjectileHit(ActiveProjectile projectile,
                                                  Store<EntityStore> store,
                                                  Vector3d from,
                                                  Vector3d to) {
        final Ref<EntityStore>[] hit = new Ref[]{null};
        final double[] bestDistance = {Double.MAX_VALUE};
        Vector3d segment = subtract(to, from);
        double segmentLengthSquared = Math.max(0.0001, dot(segment, segment));

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid()) {
                    continue;
                }

                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning() || isMotmSummon(npc)) {
                    continue;
                }

                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }

                TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
                if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                    continue;
                }

                Vector3d targetPosition = transform.getTransform().getPosition();
                double normalizedProjection = dot(subtract(targetPosition, from), segment) / segmentLengthSquared;
                double clampedProjection = clamp(normalizedProjection, 0.0, 1.0);
                Vector3d nearestPoint = new Vector3d(from).fma(clampedProjection, segment);
                double distanceToSegment = distance(nearestPoint, targetPosition);
                if (distanceToSegment > projectile.collisionRadius()) {
                    continue;
                }

                double alongSegment = distance(from, nearestPoint);
                if (alongSegment < bestDistance[0]) {
                    bestDistance[0] = alongSegment;
                    hit[0] = ref;
                }
            }
        });

        return hit[0];
    }

    private void applyProjectileImpact(ActiveProjectile projectile,
                                       PlayerData player,
                                       Store<EntityStore> store,
                                       Vector3d impactPosition,
                                       Ref<EntityStore> directHit) {
        List<Ref<EntityStore>> targets = collectProjectileImpactTargets(projectile, store, impactPosition, directHit);
        if (targets.isEmpty()) {
            return;
        }

        DamageCause cause = DamageCause.PROJECTILE;
        String impactEffectId = resolveImpactEffectId(projectile.classId(), projectile.styleId(), projectile.ability());
        double castBuffMultiplier = resolveOutgoingDamageMultiplier(player);
        double totalDamage = 0.0;

        for (Ref<EntityStore> targetRef : targets) {
            if (targetRef == null || !targetRef.isValid()) {
                continue;
            }

            String targetEntityId = resolveEntityId(targetRef, store);
            double resolvedDamage = projectile.baseDamage() * castBuffMultiplier;
            if (targetEntityId != null) {
                resolvedDamage = applySpecialDamageModifiers(player, projectile.ability(), targetRef, store, targetEntityId, resolvedDamage);
                resolvedDamage *= resolveIncomingDamageMultiplier(targetEntityId);
                resolvedDamage = mod.getStatusEffectManager().absorbDamage(targetEntityId, resolvedDamage);
            }

            if (resolvedDamage > 0.0) {
                Damage damage = new Damage(new Damage.EntitySource(projectile.ownerRef()), cause, (float) resolvedDamage);
                DamageSystems.executeDamage(targetRef, store, damage);
                reportAbilityKillIfDead(projectile.ownerPlayerId(), player, targetRef, store, targetEntityId);
                applyPostDamageClassPassives(player, projectile.ownerRef(), targetEntityId, resolvedDamage, true);
                totalDamage += resolvedDamage;
            }

            applyEffectById(targetRef, store, impactEffectId);
            applyProjectileTravelTypeEffects(projectile, player, store, targetRef, impactPosition, true);
        }

        LOG.info("[MOTM] Projectile impact resolved: abilityId=" + projectile.ability().getId()
                + " targets=" + targets.size()
                + " damage=" + AbilityPresentation.formatDecimal(totalDamage)
                + " effect=" + (projectile.ability().getEffect() == null
                ? ""
                : projectile.ability().getEffect())
                + " impact=" + formatVector(impactPosition));

        if (totalDamage > 0.0) {
            player.getStatistics().setTotalDamageDealt(
                    player.getStatistics().getTotalDamageDealt() + totalDamage);
            applyLifesteal(projectile.ownerRef(), projectile.ownerPlayerId(), totalDamage);
        }

        applyProjectileTargetEffects(projectile, player, store, targets);
        if (isLightningProjectile(projectile.ability()) && directHit != null && directHit.isValid()) {
            String directEntityId = resolveEntityId(directHit, store);
            if (directEntityId != null) {
                projectile.hitEntityIds().add(directEntityId);
            }
            applyLightningArcSplash(projectile, player, store, directHit);
        }
    }

    private void applyProjectileTravelTypeEffects(ActiveProjectile projectile,
                                                  PlayerData player,
                                                  Store<EntityStore> store,
                                                  Ref<EntityStore> primaryTarget,
                                                  Vector3d impactPosition,
                                                  boolean allowSplash) {
        if (projectile == null || player == null || store == null || primaryTarget == null || !primaryTarget.isValid()) {
            return;
        }

        String travelType = lower(projectile.ability().getTravelType());
        if (travelType.isBlank()) {
            return;
        }

        if (travelType.contains("gust")) {
            applyTokenToTarget("disoriented", primaryTarget, store, projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            if (allowSplash) {
                applyProjectileSplashToken(projectile, player, store, impactPosition, primaryTarget, "knockback", 2.4, 1);
            }
            return;
        }

        if (travelType.contains("compressed_air")) {
            applyTokenToTarget("knockback", primaryTarget, store, projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            applyTokenToTarget("grounded", primaryTarget, store, projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            return;
        }

        if (travelType.contains("psychic")) {
            applyTokenToTarget("disoriented", primaryTarget, store, projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            if (allowSplash) {
                applyProjectileSplashToken(projectile, player, store, impactPosition, primaryTarget, "vulnerability", 2.6, 2);
            }
            return;
        }

        if (travelType.contains("boiling_jet")) {
            if (allowSplash) {
                applyProjectileSplashToken(projectile, player, store, impactPosition, primaryTarget, "burn", 2.1, 2);
            }
            return;
        }

        if (travelType.contains("arcing_shot") && allowSplash) {
            applyProjectileSplashToken(projectile, player, store, impactPosition, primaryTarget, "slow", 1.8, 1);
        }

        if (isCactiClusterProjectile(projectile)) {
            applyTokenToTarget("slow", primaryTarget, store, projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            applyTokenToTarget("dot", primaryTarget, store, projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            scheduleCactiClusterBurst(projectile, primaryTarget, store, impactPosition);
            LOG.info("[MOTM][terra-audit] event=cacti_cluster.attached target="
                    + resolveEntityId(primaryTarget, store)
                    + " impact=" + formatVector(impactPosition)
                    + " burstDelaySeconds=" + AbilityPresentation.formatDecimal(
                    Math.max(1.0, projectile.ability().getDurationSeconds())));
        }
    }

    private void scheduleCactiClusterBurst(ActiveProjectile projectile,
                                           Ref<EntityStore> primaryTarget,
                                           Store<EntityStore> store,
                                           Vector3d impactPosition) {
        if (projectile == null || projectile.ability() == null || primaryTarget == null
                || !primaryTarget.isValid() || store == null || impactPosition == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long burstAt = now + (long) (Math.max(1.0, projectile.ability().getDurationSeconds()) * 1000);
        String primaryEntityId = resolveEntityId(primaryTarget, store);
        activeDelayedBursts.add(new ActiveDelayedBurst(
                projectile.ownerPlayerId(),
                projectile.ownerRef(),
                projectile.classId(),
                projectile.styleId(),
                projectile.ability(),
                new Vector3d(impactPosition),
                primaryEntityId,
                burstAt,
                Math.max(4.0, projectile.ability().getRadius() > 0 ? projectile.ability().getRadius() : 4.0),
                mod.currentObservabilityTraceId()
        ));

        World world = store.getExternalData() != null ? store.getExternalData().getWorld() : null;
        if (world != null) {
            placeSurfaceColumnSelection(world, "cacti_cluster", impactPosition, 2, burstAt + 350L,
                    "Plant_Cactus_1", "Prototype_Cactus_Kit_Tall_Base", "Prototype_Cactus_One");
        }
    }

    private boolean processDelayedBurstTick(ActiveDelayedBurst burst,
                                            Store<EntityStore> currentStore,
                                            long now) {
        if (burst == null || burst.ownerRef() == null || !burst.ownerRef().isValid()) {
            return true;
        }
        if (now < burst.burstAtMillis()) {
            return false;
        }
        if (currentStore == null) {
            return false;
        }

        PlayerData player = mod.getPlayerDataManager().getOnlinePlayer(burst.ownerPlayerId());
        if (player == null) {
            return true;
        }

        int affected = 0;
        for (Ref<EntityStore> targetRef : collectNearbyNpcTargets(currentStore, burst.center(), burst.radius(), 10)) {
            if (targetRef == null || !targetRef.isValid()) {
                continue;
            }
            String entityId = resolveEntityId(targetRef, currentStore);
            if (entityId == null || entityId.equals(burst.primaryEntityId())) {
                continue;
            }
            applyTokenToTarget("slow", targetRef, currentStore, burst.ownerRef(), player.getPlayerId(), burst.ability());
            applyTokenToTarget("dot", targetRef, currentStore, burst.ownerRef(), player.getPlayerId(), burst.ability());
            applyEffectById(targetRef, currentStore, resolveImpactEffectId(burst.classId(), burst.styleId(), burst.ability()));
            affected++;
        }

        LOG.info("[MOTM][terra-audit] event=cacti_cluster.burst center=" + formatVector(burst.center())
                + " primaryTarget=" + safe(burst.primaryEntityId())
                + " radius=" + AbilityPresentation.formatDecimal(burst.radius())
                + " affectedSplashTargets=" + affected);
        return true;
    }

    private boolean isCactiClusterProjectile(ActiveProjectile projectile) {
        return projectile != null
                && projectile.ability() != null
                && "cacti_cluster".equals(lower(projectile.ability().getId()));
    }

    private void applyProjectileSplashToken(ActiveProjectile projectile,
                                            PlayerData player,
                                            Store<EntityStore> store,
                                            Vector3d impactPosition,
                                            Ref<EntityStore> primaryTarget,
                                            String token,
                                            double radius,
                                            int maxTargets) {
        if (projectile == null || player == null || store == null || impactPosition == null
                || token == null || token.isBlank() || radius <= 0.0 || maxTargets <= 0) {
            return;
        }

        int applied = 0;
        for (Ref<EntityStore> splashTarget : collectNearbyNpcTargets(store, impactPosition, radius, maxTargets + 1)) {
            if (splashTarget == null || !splashTarget.isValid() || splashTarget.equals(primaryTarget)) {
                continue;
            }
            applyTokenToTarget(token, splashTarget, store, projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            applied++;
            if (applied >= maxTargets) {
                return;
            }
        }
    }

    private void applyProjectileTraversalHits(ActiveProjectile projectile,
                                              PlayerData player,
                                              Store<EntityStore> store,
                                              Vector3d from,
                                              Vector3d to) {
        List<Ref<EntityStore>> targets = collectProjectileTraversalTargets(projectile, store, from, to);
        if (targets.isEmpty()) {
            return;
        }

        String impactEffectId = resolveImpactEffectId(projectile.classId(), projectile.styleId(), projectile.ability());
        DamageCause cause = DamageCause.PROJECTILE;
        double castBuffMultiplier = resolveOutgoingDamageMultiplier(player);
        int hitIndex = projectile.hitEntityIds().size();
        int resolvedTargets = 0;
        double totalDamage = 0.0;

        for (Ref<EntityStore> targetRef : targets) {
            if (targetRef == null || !targetRef.isValid()) {
                continue;
            }

            String targetEntityId = resolveEntityId(targetRef, store);
            if (targetEntityId == null || targetEntityId.equals(projectile.ownerPlayerId())) {
                continue;
            }

            double resolvedDamage = projectile.baseDamage() * castBuffMultiplier;
            resolvedDamage *= resolveTargetSequenceDamageMultiplier(projectile.ability(), lower(projectile.ability().getCastType()), hitIndex);
            resolvedDamage = applySpecialDamageModifiers(player, projectile.ability(), targetRef, store, targetEntityId, resolvedDamage);
            resolvedDamage *= resolveIncomingDamageMultiplier(targetEntityId);
            resolvedDamage = mod.getStatusEffectManager().absorbDamage(targetEntityId, resolvedDamage);

            if (resolvedDamage > 0.0) {
                Damage damage = new Damage(new Damage.EntitySource(projectile.ownerRef()), cause, (float) resolvedDamage);
                DamageSystems.executeDamage(targetRef, store, damage);
                applyPostDamageClassPassives(player, projectile.ownerRef(), targetEntityId, resolvedDamage, true);
                player.getStatistics().setTotalDamageDealt(
                        player.getStatistics().getTotalDamageDealt() + resolvedDamage);
                applyLifesteal(projectile.ownerRef(), projectile.ownerPlayerId(), resolvedDamage);
                totalDamage += resolvedDamage;
            }

            applyEffectById(targetRef, store, impactEffectId);
            applyProjectileTargetEffects(projectile, player, store, List.of(targetRef));
            applyProjectileTravelTypeEffects(projectile, player, store, targetRef, to, false);
            projectile.hitEntityIds().add(targetEntityId);
            hitIndex++;
            resolvedTargets++;

            if (isLightningProjectile(projectile.ability())) {
                applyLightningArcSplash(projectile, player, store, targetRef);
            }
        }

        if (resolvedTargets > 0) {
            LOG.info("[MOTM] Projectile traversal resolved: abilityId=" + projectile.ability().getId()
                    + " targets=" + resolvedTargets
                    + " damage=" + AbilityPresentation.formatDecimal(totalDamage)
                    + " effect=" + (projectile.ability().getEffect() == null
                    ? ""
                    : projectile.ability().getEffect())
                    + " position=" + formatVector(to));
        }
    }

    private List<Ref<EntityStore>> collectProjectileImpactTargets(ActiveProjectile projectile,
                                                                  Store<EntityStore> store,
                                                                  Vector3d impactPosition,
                                                                  Ref<EntityStore> directHit) {
        LinkedHashSet<Ref<EntityStore>> targets = new LinkedHashSet<>();
        if (directHit != null && directHit.isValid()) {
            targets.add(directHit);
        }

        double radius = projectile.impactRadius();
        if (radius <= 0.01) {
            if (!targets.isEmpty()) {
                return List.copyOf(targets);
            }
            Ref<EntityStore> splashHit = findNearestNpc(store, impactPosition, projectile.collisionRadius());
            return splashHit != null ? List.of(splashHit) : List.of();
        }

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid()) {
                    continue;
                }

                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning() || isMotmSummon(npc)) {
                    continue;
                }

                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }

                TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
                if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                    continue;
                }

                if (distance(impactPosition, transform.getTransform().getPosition()) <= radius) {
                    targets.add(ref);
                }
            }
        });

        return List.copyOf(targets);
    }

    private List<Ref<EntityStore>> collectProjectileTraversalTargets(ActiveProjectile projectile,
                                                                     Store<EntityStore> store,
                                                                     Vector3d from,
                                                                     Vector3d to) {
        LinkedHashSet<Ref<EntityStore>> targets = new LinkedHashSet<>();
        Vector3d segment = subtract(to, from);
        double segmentLengthSquared = Math.max(0.0001, dot(segment, segment));

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid()) {
                    continue;
                }

                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning() || isMotmSummon(npc)) {
                    continue;
                }

                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }

                TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
                if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                    continue;
                }

                String entityId = resolveEntityId(ref, store);
                if (entityId == null || projectile.hitEntityIds().contains(entityId)) {
                    continue;
                }

                Vector3d targetPosition = transform.getTransform().getPosition();
                double normalizedProjection = dot(subtract(targetPosition, from), segment) / segmentLengthSquared;
                double clampedProjection = clamp(normalizedProjection, 0.0, 1.0);
                Vector3d nearestPoint = new Vector3d(from).fma(clampedProjection, segment);
                if (distance(nearestPoint, targetPosition) <= projectile.collisionRadius()) {
                    targets.add(ref);
                }
            }
        });

        return List.copyOf(targets);
    }

    private void applyLightningArcSplash(ActiveProjectile projectile,
                                         PlayerData player,
                                         Store<EntityStore> store,
                                         Ref<EntityStore> directTargetRef) {
        Vector3d center = getPosition(directTargetRef, store);
        if (center == null) {
            return;
        }

        String impactEffectId = resolveImpactEffectId(projectile.classId(), projectile.styleId(), projectile.ability());
        double radius = projectile.ability().getRadius() > 0
                ? Math.max(DEFAULT_LIGHTNING_ARC_RADIUS, projectile.ability().getRadius())
                : DEFAULT_LIGHTNING_ARC_RADIUS;
        List<Ref<EntityStore>> arcTargets = collectNearbyNpcTargets(store, center, radius, 2);
        double castBuffMultiplier = resolveOutgoingDamageMultiplier(player);

        for (Ref<EntityStore> arcTarget : arcTargets) {
            if (arcTarget == null || !arcTarget.isValid() || arcTarget.equals(directTargetRef)) {
                continue;
            }

            String entityId = resolveEntityId(arcTarget, store);
            if (entityId == null || projectile.hitEntityIds().contains(entityId)) {
                continue;
            }

            double resolvedDamage = projectile.baseDamage() * 0.55 * castBuffMultiplier;
            resolvedDamage *= resolveIncomingDamageMultiplier(entityId);
            resolvedDamage = mod.getStatusEffectManager().absorbDamage(entityId, resolvedDamage);
            if (resolvedDamage > 0.0) {
                Damage arcDamage = new Damage(new Damage.EntitySource(projectile.ownerRef()), DamageCause.PROJECTILE, (float) resolvedDamage);
                DamageSystems.executeDamage(arcTarget, store, arcDamage);
                applyPostDamageClassPassives(player, projectile.ownerRef(), entityId, resolvedDamage, true);
                player.getStatistics().setTotalDamageDealt(
                        player.getStatistics().getTotalDamageDealt() + resolvedDamage);
                applyLifesteal(projectile.ownerRef(), projectile.ownerPlayerId(), resolvedDamage);
            }

            applyEffectById(arcTarget, store, impactEffectId);
            applyTokenToTarget("shocked", arcTarget, store, projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            projectile.hitEntityIds().add(entityId);
        }
    }

    private Ref<EntityStore> findNearestNpc(Store<EntityStore> store, Vector3d center, double radius) {
        final Ref<EntityStore>[] nearest = new Ref[]{null};
        final double[] bestDistance = {Double.MAX_VALUE};

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid()) {
                    continue;
                }

                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning() || isMotmSummon(npc)) {
                    continue;
                }

                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }

                TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
                if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                    continue;
                }

                double candidateDistance = distance(center, transform.getTransform().getPosition());
                if (candidateDistance <= radius && candidateDistance < bestDistance[0]) {
                    bestDistance[0] = candidateDistance;
                    nearest[0] = ref;
                }
            }
        });

        return nearest[0];
    }

    private void applyProjectileTargetEffects(ActiveProjectile projectile,
                                              PlayerData player,
                                              Store<EntityStore> store,
                                              List<Ref<EntityStore>> targets) {
        List<String> tokens = parseEffectTokens(projectile.ability().getEffect());
        if (tokens.isEmpty()) {
            return;
        }

        for (Ref<EntityStore> targetRef : targets) {
            String entityId = resolveEntityId(targetRef, store);
            if (entityId == null || entityId.equals(player.getPlayerId())) {
                continue;
            }

            for (String token : tokens) {
                if (!TARGET_EFFECT_TOKENS.contains(token)) {
                    continue;
                }

                applyTargetToken(token, targetRef, store, projectile.ownerRef(), player.getPlayerId(), projectile.ability());
            }
        }
    }

    private boolean isPiercingProjectile(AbilityData ability) {
        if (ability == null) {
            return false;
        }

        String castType = lower(ability.getCastType());
        if ("wave_line".equals(castType) || "projectile_line".equals(castType)) {
            return true;
        }

        String travelType = lower(ability.getTravelType());
        return travelType.contains("wave")
                || travelType.contains("slash")
                || travelType.contains("cutter")
                || travelType.contains("tide")
                || travelType.contains("shard")
                || travelType.contains("gust");
    }

    private boolean isLightningProjectile(AbilityData ability) {
        if (ability == null) {
            return false;
        }

        String abilityId = lower(ability.getId());
        String travelType = lower(ability.getTravelType());
        return abilityId.contains("smite")
                || abilityId.contains("lightning")
                || travelType.contains("lightning")
                || travelType.contains("thunder");
    }

    private List<Ref<EntityStore>> collectFieldTargets(ActiveField field, Store<EntityStore> store) {
        LinkedHashSet<Ref<EntityStore>> targets = new LinkedHashSet<>();
        String castType = lower(field.ability().getCastType());

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid()) {
                    continue;
                }

                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning() || isMotmSummon(npc)) {
                    continue;
                }

                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }

                TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
                if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                    continue;
                }

                Vector3d position = transform.getTransform().getPosition();
                if (field.ability().isGroundTargetsOnly() && !isTargetGrounded(ref, store)) {
                    continue;
                }

                if ("barrier".equals(castType)) {
                    if (isInsideBarrier(field, position)) {
                        targets.add(ref);
                    }
                    continue;
                }

                if (distance(field.center(), position) <= field.radius()) {
                    targets.add(ref);
                }
            }
        });

        return List.copyOf(targets);
    }

    private boolean isInsideBarrier(ActiveField field, Vector3d position) {
        Vector3d relative = subtract(position, field.center());
        double lateral = Math.abs(dot(relative, field.lineDirection()));
        double depth = Math.abs(dot(relative, field.forwardDirection()));
        return lateral <= field.halfWidth() && depth <= field.thickness();
    }

    private void applyFieldPulse(ActiveField field,
                                 PlayerData player,
                                 Store<EntityStore> store,
                                 List<Ref<EntityStore>> targets) {
        double totalDamage = 0.0;
        double pulseDamage = resolveFieldPulseDamage(player, field.ability());
        String impactEffectId = resolveImpactEffectId(field.classId(), field.styleId(), field.ability());

        for (Ref<EntityStore> targetRef : targets) {
            if (targetRef == null || !targetRef.isValid()) {
                continue;
            }

            String entityId = resolveEntityId(targetRef, store);
            if (entityId == null || entityId.equals(player.getPlayerId())) {
                continue;
            }

            if (pulseDamage > 0.0) {
                double resolvedDamage = pulseDamage * resolveOutgoingDamageMultiplier(player);
                resolvedDamage *= resolveIncomingDamageMultiplier(entityId);
                resolvedDamage = mod.getStatusEffectManager().absorbDamage(entityId, resolvedDamage);
                if (resolvedDamage > 0.0) {
                    Damage damage = new Damage(new Damage.EntitySource(field.ownerRef()), DamageCause.PHYSICAL, (float) resolvedDamage);
                    DamageSystems.executeDamage(targetRef, store, damage);
                    applyPostDamageClassPassives(player, field.ownerRef(), entityId, resolvedDamage, true);
                    totalDamage += resolvedDamage;
                }
            }

            if (!isQuakeGroundImpactAbility(field.ability())) {
                applyEffectById(targetRef, store, impactEffectId);
            } else {
                applyAerialQuakeClarityEffect(field.ability(), targetRef, store, impactEffectId);
            }
            applyFieldTargetEffects(field, player, targetRef, store);
        }

        if (totalDamage > 0.0) {
            player.getStatistics().setTotalDamageDealt(
                    player.getStatistics().getTotalDamageDealt() + totalDamage);
            applyLifesteal(field.ownerRef(), player.getPlayerId(), totalDamage);
        }
    }

    private void engageSinkholeField(ActiveField field, Store<EntityStore> store) {
        if (!isSinkhole(field.ability())) {
            return;
        }

        String key = buriedFieldKey(field);
        if (buriedVictimsByField.containsKey(key)) {
            return;
        }

        List<Ref<EntityStore>> caught = collectFieldTargets(field, store);
        if (caught.isEmpty()) {
            placeSinkholeSurfaceMarker(field, 1800L);
            buriedVictimsByField.put(key, new ArrayList<>());
            LOG.info("[MOTM] Sinkhole engaged: no targets in radius="
                    + AbilityPresentation.formatDecimal(field.ability().getRadius() > 0 ? field.ability().getRadius() : DEFAULT_AREA_RADIUS)
                    + " at center=" + field.center());
            return;
        }

        List<BuriedVictim> victims = new ArrayList<>();
        for (Ref<EntityStore> targetRef : caught) {
            if (targetRef == null || !targetRef.isValid()) {
                continue;
            }

            applyEffectById(targetRef, store, "MOTM_Terra_Sinkhole_Buried");
            applyTargetToken("root", targetRef, store, field.ownerRef(), field.ownerPlayerId(), field.ability());
            victims.add(new BuriedVictim(targetRef, null, field.expireAtMillis()));
        }

        placeSinkholeSurfaceMarker(field, Math.max(1800L, field.expireAtMillis() - System.currentTimeMillis()));
        buriedVictimsByField.put(key, victims);
        LOG.info("[MOTM] Sinkhole engaged: buried " + victims.size()
                + " target(s) at center=" + field.center());
    }

    private void placeSinkholeSurfaceMarker(ActiveField field, long durationMillis) {
        if (field == null || field.ownerRef() == null || !field.ownerRef().isValid()) {
            return;
        }
        Player runtimePlayer = mod.getRuntimePlayer(field.ownerPlayerId());
        if (runtimePlayer != null) {
            Vector3d groundedCenter = new Vector3d(field.center());
            groundedCenter.y = groundedCenter.y - 2.15;
            spawnQuakeImpactRing(runtimePlayer, field.ability(), groundedCenter);
            LOG.info("[MOTM] Sinkhole surface marker placed: crack particles only center=" + groundedCenter
                    + " durationMillis=" + Math.max(900L, durationMillis)
                    + " blocksPlaced=0");
        } else {
            LOG.info("[MOTM] Sinkhole surface marker skipped: runtime player unavailable center=" + field.center());
        }
    }

    private void applySinkholeSuffocationPulse(ActiveField field, Store<EntityStore> store) {
        if (!isSinkhole(field.ability())) {
            return;
        }

        double dotPercent = Math.max(0.0, field.ability().getDotPercentPerSecond());
        if (dotPercent <= 0.0) {
            return;
        }

        List<BuriedVictim> victims = buriedVictimsByField.get(buriedFieldKey(field));
        if (victims == null || victims.isEmpty()) {
            return;
        }

        double maxHpFraction = dotPercent * (FIELD_PULSE_INTERVAL_MS / 1000.0) / 100.0;
        for (BuriedVictim victim : victims) {
            if (victim.targetRef() != null && victim.targetRef().isValid()) {
                applySuffocationTick(victim.targetRef(), store, field, maxHpFraction);
            }
        }
    }

    private void applySuffocationTick(Ref<EntityStore> targetRef,
                                      Store<EntityStore> store,
                                      ActiveField field,
                                      double maxHpFraction) {
        if (targetRef == null || !targetRef.isValid() || store == null || maxHpFraction <= 0.0) {
            return;
        }

        EntityStatMap entityStatMap = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return;
        }

        EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null || health.getMax() <= 0.0) {
            return;
        }

        double resolvedDamage = health.getMax() * maxHpFraction;
        if (resolvedDamage <= 0.0) {
            return;
        }

        try {
            DamageCause cause = DamageCause.getAssetMap().getAsset("Suffocation");
            Damage damage = new Damage(new Damage.EntitySource(field.ownerRef()), cause, (float) resolvedDamage);
            DamageSystems.executeDamage(targetRef, store, damage);
            LOG.info("[MOTM] Sinkhole suffocation tick: target="
                    + (resolveEntityId(targetRef, store) == null ? "<unknown>" : resolveEntityId(targetRef, store))
                    + " damage=" + AbilityPresentation.formatDecimal(resolvedDamage));
        } catch (RuntimeException e) {
            LOG.warning("[MOTM] Sinkhole DoT failed: " + e.getMessage());
        }
    }

    private void releaseSinkholeField(ActiveField field, Store<EntityStore> store) {
        if (!isSinkhole(field.ability())) {
            return;
        }

        List<BuriedVictim> victims = buriedVictimsByField.remove(buriedFieldKey(field));
        if (victims == null || victims.isEmpty()) {
            return;
        }

        for (BuriedVictim victim : victims) {
            if (victim.targetRef() == null || !victim.targetRef().isValid() || victim.originalScale() == null) {
                continue;
            }
            LOG.fine("[MOTM] Sinkhole scale restore skipped; EntityScaleComponent support is not enabled.");
        }
        LOG.info("[MOTM] Sinkhole released: " + victims.size() + " target(s)");
    }

    private boolean isSinkhole(AbilityData ability) {
        return ability != null
                && ("sinkhole".equalsIgnoreCase(ability.getId())
                || lower(ability.getTerrainEffect()).contains("sinkhole"));
    }

    private String buriedFieldKey(ActiveField field) {
        if (field == null) {
            return "";
        }
        return field.ownerPlayerId() + "::" + lower(field.ability().getId()) + "::" + field.activateAtMillis();
    }

    private void applyFieldTargetEffects(ActiveField field,
                                         PlayerData player,
                                         Ref<EntityStore> targetRef,
                                         Store<EntityStore> store) {
        String entityId = resolveEntityId(targetRef, store);
        if (entityId == null || entityId.equals(player.getPlayerId())) {
            return;
        }

        for (String token : parseEffectTokens(field.ability().getEffect())) {
            if (!TARGET_EFFECT_TOKENS.contains(token)) {
                continue;
            }

            applyTargetToken(token, targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
        }

        applyFieldTerrainEffects(field, player, targetRef, store);

        if (field.ability().getPullForce() > 0 && !"barrier".equals(lower(field.ability().getCastType()))) {
            applyFieldPull(targetRef, store, field);
        }

        if ("barrier".equals(lower(field.ability().getCastType()))) {
            applyBarrierRepulsion(targetRef, store, field);
        }
    }

    private void applyFieldSupportPulse(ActiveField field, PlayerData player) {
        Ref<EntityStore> ownerRef = field.ownerRef();
        if (ownerRef == null || !ownerRef.isValid() || ownerRef.getStore() == null) {
            return;
        }

        Store<EntityStore> store = ownerRef.getStore();

        Vector3d ownerPosition = getPosition(ownerRef, store);
        if (ownerPosition == null) {
            return;
        }

        boolean inField = "barrier".equals(lower(field.ability().getCastType()))
                ? isInsideBarrier(field, ownerPosition)
                : distance(field.center(), ownerPosition) <= field.radius();
        if (!inField) {
            return;
        }

        applyFieldOwnerEffects(field, player);
        double sustainMultiplier = mod.getLevelingManager().getPlayerSustainMultiplier(player);

        double pulseHealPercent = field.ability().getHealPercent() * DEFAULT_SUPPORT_HEAL_RATIO * sustainMultiplier;
        if (pulseHealPercent > 0.0) {
            double healed = healEntity(ownerRef, store, pulseHealPercent);
            if (healed > 0.0) {
                player.getStatistics().setTotalHealingDone(player.getStatistics().getTotalHealingDone() + healed);
            }
        }

        double pulseShieldPercent = field.ability().getShieldPercent() * DEFAULT_SUPPORT_SHIELD_RATIO * sustainMultiplier;
        if (pulseShieldPercent > 0.0) {
            applyShield(player.getPlayerId(), ownerRef, store, field.ability(), pulseShieldPercent);
        }

        applyFieldOwnerTerrainEffects(field, player, ownerRef, store, sustainMultiplier);
    }

    private void applyFieldOwnerEffects(ActiveField field, PlayerData player) {
        for (String token : parseEffectTokens(field.ability().getEffect())) {
            if (!shouldPulseOwnerEffectToken(field, token)) {
                continue;
            }

            StatusEffect effect = createStatusEffect(token, field.ability(), player.getPlayerId(), field.ability().getId());
            if (effect != null) {
                mod.getStatusEffectManager().applyEffect(player.getPlayerId(), effect);
            }
        }

        String terrainEffect = lower(field.ability().getTerrainEffect());
        if (terrainEffect.contains("shadow") || terrainEffect.contains("smoke")) {
            applyStatusToOwner("evasion", field, player);
        }
        if (terrainEffect.contains("mist_shroud")
                || terrainEffect.contains("condensation_veil")
                || terrainEffect.contains("vanish")
                || terrainEffect.contains("umbral_shroud")) {
            applyStatusToOwner("evasion", field, player);
        }
        if (terrainEffect.contains("tide_pool") || terrainEffect.contains("rainbow")) {
            applyStatusToOwner("speed", field, player);
        }
        if (terrainEffect.contains("sanctuary") || terrainEffect.contains("glacier") || terrainEffect.contains("purifying")) {
            applyStatusToOwner("defense_buff", field, player);
        }
        if (terrainEffect.contains("ice_shell")) {
            applyStatusToOwner("defense_buff", field, player);
        }
    }

    private boolean shouldPulseOwnerEffectToken(ActiveField field, String token) {
        if (field == null || token == null || token.isBlank()) {
            return false;
        }
        if (!CASTER_EFFECT_TOKENS.contains(token)) {
            return false;
        }

        String terrainEffect = lower(field.ability().getTerrainEffect());
        return !"stealth".equals(lower(token))
                || (!terrainEffect.contains("vanish") && !terrainEffect.contains("umbral_shroud"));
    }

    private void applyFieldTerrainEffects(ActiveField field,
                                          PlayerData player,
                                          Ref<EntityStore> targetRef,
                                          Store<EntityStore> store) {
        String terrainEffect = lower(field.ability().getTerrainEffect());
        if (terrainEffect.isBlank()) {
            return;
        }

        if (terrainEffect.contains("sinkhole")) {
            applyTargetToken("root", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("lingering_tremor") || terrainEffect.contains("seismic_shockwave")) {
            applyKnockbackFromPoint(targetRef, store, field.center(), field.ability());
            return;
        }

        if (terrainEffect.contains("mudpit")) {
            applyTargetToken("root", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("falling_rocks")) {
            applyTargetToken("knockback", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("living_flame") || terrainEffect.contains("ember_trail")) {
            applyTargetToken("burn", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("ice_skate_trail")) {
            applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("grounded", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("tunnel_path") || terrainEffect.contains("ruptured_earth")) {
            applyTargetToken("knockback", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("grounded", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("cyclone_shield")) {
            applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("knockback", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("pressure_burst")) {
            applyTargetToken("knockback", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("grounded", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("twister") || terrainEffect.contains("dust_devil")) {
            applyTargetToken("knockback", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("tempest")) {
            String entityId = resolveEntityId(targetRef, store);
            boolean stunned = applyTargetToken("stun", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            boolean slowed = applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            LOG.info("[MOTM] Tempest field tick applied: target=" + entityId
                    + " stun=" + stunned
                    + " slow=" + slowed);
            return;
        }

        if (terrainEffect.contains("funnel_cloud")) {
            applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("snowstorm")) {
            applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("attack_slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("sandstorm")) {
            applyTargetToken("blind", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("smog")) {
            String entityId = resolveEntityId(targetRef, store);
            boolean blinded = applyTargetToken("blind", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            boolean slowed = applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            boolean dotted = applyTargetToken("dot", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            LOG.info("[MOTM] Smog field tick applied: target=" + entityId
                    + " blind=" + blinded
                    + " slow=" + slowed
                    + " dot=" + dotted);
            return;
        }

        if (terrainEffect.contains("acid")) {
            applyTargetToken("attack_slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("piercing_rain")) {
            applyTargetToken("attack_slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("dot", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("glacier")) {
            applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("ice_shell")) {
            applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("grounded", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("void_rift")) {
            applyTargetToken("vulnerability", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("infernal_ground")) {
            applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("shadow_zone")) {
            applyTargetToken("blind", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("smoke_bomb")) {
            applyTargetToken("blind", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("mist_shroud")
                || terrainEffect.contains("vanish")
                || terrainEffect.contains("umbral_shroud")) {
            applyTargetToken("blind", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("resonant_aura")) {
            applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("psychic_link")) {
            applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("vulnerability", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("steam_pressure")) {
            applyTargetToken("knockback", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("disoriented", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
        }
    }

    private void applyFieldOwnerTerrainEffects(ActiveField field,
                                               PlayerData player,
                                               Ref<EntityStore> ownerRef,
                                               Store<EntityStore> store,
                                               double sustainMultiplier) {
        String terrainEffect = lower(field.ability().getTerrainEffect());
        if (terrainEffect.isBlank()) {
            return;
        }

        if (terrainEffect.contains("sanctuary") || terrainEffect.contains("purifying")) {
            clearNegativeEffects(player.getPlayerId());
            applyShield(player.getPlayerId(), ownerRef, store, field.ability(), 4.0 * sustainMultiplier);
            return;
        }

        if (terrainEffect.contains("rainbow")) {
            applyShield(player.getPlayerId(), ownerRef, store, field.ability(), 3.5 * sustainMultiplier);
            applyStatusToOwner("speed", field, player);
            return;
        }

        if (terrainEffect.contains("root_circle")) {
            healEntity(ownerRef, store, 2.5 * sustainMultiplier);
            applyStatusToOwner("defense_buff", field, player);
            return;
        }

        if (terrainEffect.contains("eye_of_the_storm")) {
            healEntity(ownerRef, store, 2.0 * sustainMultiplier);
            applyShield(player.getPlayerId(), ownerRef, store, field.ability(), 2.5 * sustainMultiplier);
            applyStatusToOwner("evasion", field, player);
            return;
        }

        if (terrainEffect.contains("cyclone_shield")) {
            applyShield(player.getPlayerId(), ownerRef, store, field.ability(), 2.0 * sustainMultiplier);
            applyStatusToOwner("defense_buff", field, player);
            return;
        }

        if (terrainEffect.contains("ice_shell")) {
            applyShield(player.getPlayerId(), ownerRef, store, field.ability(), 2.0 * sustainMultiplier);
            applyStatusToOwner("defense_buff", field, player);
            return;
        }

        if (terrainEffect.contains("tide_pool")) {
            applyStatusToOwner("speed", field, player);
            return;
        }

        if (terrainEffect.contains("glacier")) {
            applyShield(player.getPlayerId(), ownerRef, store, field.ability(), 3.0 * sustainMultiplier);
            return;
        }

        if (terrainEffect.contains("shadow") || terrainEffect.contains("smoke")) {
            applyStatusToOwner("evasion", field, player);
            return;
        }

        if (terrainEffect.contains("mist_shroud")
                || terrainEffect.contains("condensation_veil")) {
            applyStatusToOwner("evasion", field, player);
            return;
        }

        if (terrainEffect.contains("resonant_aura")) {
            applyStatusToOwner("attack_buff", field, player);
            applyStatusToOwner("speed", field, player);
            return;
        }

        if (terrainEffect.contains("psychic_link")) {
            applyStatusToOwner("attack_buff", field, player);
            return;
        }

        if (terrainEffect.contains("steam_pressure")) {
            applyStatusToOwner("attack_buff", field, player);
            applyStatusToOwner("speed", field, player);
        }
    }

    private void applyStatusToOwner(String token, ActiveField field, PlayerData player) {
        StatusEffect effect = createStatusEffect(token, field.ability(), player.getPlayerId(), field.ability().getId());
        if (effect != null) {
            mod.getStatusEffectManager().applyEffect(player.getPlayerId(), effect);
        }
    }

    private boolean applyBarrierRepulsion(Ref<EntityStore> targetRef,
                                          Store<EntityStore> store,
                                          ActiveField field) {
        Vector3d targetPosition = getPosition(targetRef, store);
        if (targetPosition == null) {
            return false;
        }

        Vector3d relative = subtract(targetPosition, field.center());
        double pushSign = dot(relative, field.forwardDirection()) >= 0 ? 1.0 : -1.0;
        Vector3d destination = new Vector3d(targetPosition)
                .fma(pushSign * 1.8, field.forwardDirection())
                .add(0.0, 0.2, 0.0);

        NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
        if (npc == null) {
            return false;
        }

        npc.moveTo(targetRef, destination.x, destination.y, destination.z, store);
        return true;
    }

    private boolean applyFieldPull(Ref<EntityStore> targetRef,
                                   Store<EntityStore> store,
                                   ActiveField field) {
        return applyPullTowardsPoint(
                targetRef,
                store,
                field.center(),
                field.ability(),
                DEFAULT_PULL_STOP_DISTANCE,
                0.55,
                resolveFieldPullLift(field)
        );
    }

    private boolean applyEffectById(Ref<EntityStore> entityRef,
                                    Store<EntityStore> store,
                                    String effectId) {
        if (entityRef == null || !entityRef.isValid() || store == null || effectId == null || effectId.isBlank()) {
            return false;
        }

        EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectId);
        if (effect == null) {
            LOG.warning("[MOTM] Missing gameplay effect asset: " + effectId);
            return false;
        }

        EffectControllerComponent controller = store.getComponent(entityRef, EffectControllerComponent.getComponentType());
        if (controller == null) {
            LOG.warning("[MOTM] Entity is missing EffectControllerComponent; skipping effect " + effectId);
            return false;
        }

        boolean applied = controller.addEffect(entityRef, effect, store);
        mod.recordClientIntent("entity_effect_add", null, MotmObservability.mapOf(
                "effectId", effectId,
                "applied", applied,
                "entityIndex", entityRef.getIndex()
        ));
        return applied;
    }

    private boolean removeEffectById(Ref<EntityStore> entityRef,
                                     Store<EntityStore> store,
                                     String effectId) {
        if (entityRef == null || !entityRef.isValid() || store == null || effectId == null || effectId.isBlank()) {
            return false;
        }

        int effectIndex = EntityEffect.getAssetMap().getIndexOrDefault(effectId, -1);
        if (effectIndex < 0) {
            LOG.warning("[MOTM] Missing gameplay effect asset for removal: " + effectId);
            return false;
        }

        EffectControllerComponent controller = store.getComponent(entityRef, EffectControllerComponent.getComponentType());
        if (controller == null) {
            LOG.warning("[MOTM] Entity is missing EffectControllerComponent; skipping effect removal " + effectId);
            return false;
        }
        if (!controller.hasEffect(effectIndex)) {
            return false;
        }

        try {
            controller.removeEffect(entityRef, effectIndex, store);
            return true;
        } catch (Throwable e) {
            LOG.warning("[MOTM] Entity effect removal skipped safely: effect=" + effectId
                    + " error=" + e.getMessage());
            return false;
        }
    }

    private void spawnStaticMarkerGlow(World world,
                                       Vector3d position,
                                       String effectId,
                                       long expireAtMillis) {
        if (world == null || position == null || effectId == null || effectId.isBlank()) {
            return;
        }

        NPCEntity proxy = new NPCEntity(world);
        proxy.setRoleName(EMPTY_VISUAL_ROLE_NAME);
        proxy.setDespawnTime((float) Math.max(1.0, ((expireAtMillis - System.currentTimeMillis()) / 1000.0) + 0.75));
        world.spawnEntity(proxy, new Vector3d(position), new Rotation3f(0f, 0f, 0f));

        Ref<EntityStore> proxyRef = proxy.getReference();
        if (proxyRef == null || !proxyRef.isValid() || proxyRef.getStore() == null) {
            return;
        }

        visualProxyRefs.add(proxyRef);
        boolean applied = applyEffectById(proxyRef, proxyRef.getStore(), effectId);
        LOG.info("[MOTM] Static marker glow spawned: effect=" + effectId
                + " position=" + formatVector(position)
                + " applied=" + applied);
    }

    private ProjectileVisualRuntime spawnProjectileVisualProxy(Player runtimePlayer,
                                                               String classId,
                                                               String styleId,
                                                               AbilityData ability,
                                                               Vector3d position,
                                                               long activateAtMillis,
                                                               long expireAtMillis) {
        String effectId = resolveProjectileVisualEffectId(classId, styleId, ability);
        if (runtimePlayer == null || position == null || effectId == null || effectId.isBlank()) {
            return ProjectileVisualRuntime.none();
        }

        World world = runtimePlayer.getWorld();
        if (world == null) {
            return ProjectileVisualRuntime.none();
        }

        String roleId = HytaleAssetResolver.resolveProjectileRoleId(classId, styleId, ability);
        NPCEntity proxy = new NPCEntity(world);
        proxy.setRoleName(roleId);
        proxy.setDespawnTime((float) Math.max(0.6, ((expireAtMillis - System.currentTimeMillis()) / 1000.0) + 0.5));
        world.spawnEntity(proxy, new Vector3d(position), new Rotation3f(0f, 0f, 0f));

        Ref<EntityStore> proxyRef = proxy.getReference();
        if (proxyRef == null || !proxyRef.isValid() || proxyRef.getStore() == null) {
            return ProjectileVisualRuntime.none();
        }

        visualProxyRefs.add(proxyRef);
        String modelId = HytaleAssetResolver.resolveModelId(classId, styleId, ability);
        if (modelId != null && !modelId.isBlank()) {
            NPCEntity.setAppearance(proxyRef, modelId, proxyRef.getStore());
        }
        configureProjectileVisualProxy(proxyRef, proxyRef.getStore(), ability);
        applyEffectById(proxyRef, proxyRef.getStore(), effectId);
        mod.recordClientIntent("projectile_visual_proxy_spawned", null, MotmObservability.mapOf(
                "classId", classId,
                "styleId", styleId,
                "abilityId", ability != null ? ability.getId() : null,
                "roleId", roleId,
                "modelId", modelId,
                "effectId", effectId,
                "position", formatVector(position),
                "entityIndex", proxyRef.getIndex(),
                "activateAtMillis", activateAtMillis,
                "expireAtMillis", expireAtMillis
        ));
        return new ProjectileVisualRuntime(proxyRef, effectId, activateAtMillis + 80L);
    }

    private void configureProjectileVisualProxy(Ref<EntityStore> proxyRef,
                                                Store<EntityStore> store,
                                                AbilityData ability) {
        if (proxyRef == null || !proxyRef.isValid() || store == null || ability == null) {
            return;
        }
        try {
            store.removeComponentIfExists(proxyRef, Nameplate.getComponentType());
            store.removeComponentIfExists(proxyRef, DisplayNameComponent.getComponentType());
            store.removeComponentIfExists(proxyRef, Interactable.getComponentType());
            store.removeComponentIfExists(proxyRef, RespondToHit.getComponentType());
            store.removeComponentIfExists(proxyRef, CollisionResultComponent.getComponentType());
        } catch (Exception e) {
            LOG.warning("[MOTM] Projectile visual proxy cleanup failed safely: abilityId="
                    + lower(ability.getId()) + " error=" + e.getMessage());
        }
    }

    private FieldVisualRuntime spawnFieldVisualProxy(Player runtimePlayer,
                                                     String classId,
                                                     String styleId,
                                                     AbilityData ability,
                                                     Vector3d center,
                                                     Vector3d lineDirection,
                                                     double halfWidth,
                                                     long activateAtMillis,
                                                     long expireAtMillis) {
        String effectId = resolveFieldVisualEffectId(classId, styleId, ability);
        if (runtimePlayer == null || center == null || effectId == null || effectId.isBlank()) {
            return FieldVisualRuntime.none();
        }

        World world = runtimePlayer.getWorld();
        if (world == null) {
            return FieldVisualRuntime.none();
        }

        List<Vector3d> positions = buildFieldVisualPositions(center, lineDirection, ability, halfWidth);
        if (positions.isEmpty()) {
            return FieldVisualRuntime.none();
        }

        List<Ref<EntityStore>> refs = new ArrayList<>();
        String roleId = HytaleAssetResolver.resolveFieldRoleId(classId, styleId, ability);
        float despawnTimeSeconds = (float) Math.max(1.0, ((expireAtMillis - System.currentTimeMillis()) / 1000.0) + 0.75);
        for (Vector3d position : positions) {
            NPCEntity proxy = new NPCEntity(world);
            proxy.setRoleName(roleId);
            proxy.setDespawnTime(despawnTimeSeconds);
            world.spawnEntity(proxy, new Vector3d(position), new Rotation3f(0f, 0f, 0f));

            Ref<EntityStore> proxyRef = proxy.getReference();
            if (proxyRef != null && proxyRef.isValid() && proxyRef.getStore() != null) {
                visualProxyRefs.add(proxyRef);
                refs.add(proxyRef);
                applyEffectById(proxyRef, proxyRef.getStore(), effectId);
            }
        }

        if (refs.isEmpty()) {
            return FieldVisualRuntime.none();
        }

        return new FieldVisualRuntime(List.copyOf(refs), effectId, activateAtMillis);
    }

    private void syncProjectileVisual(ActiveProjectile projectile, long now) {
        if (projectile == null || projectile.visualRef() == null || !projectile.visualRef().isValid()) {
            return;
        }

        Store<EntityStore> visualStore = projectile.visualRef().getStore();
        if (visualStore == null) {
            return;
        }

        NPCEntity npc = visualStore.getComponent(projectile.visualRef(), NPCEntity.getComponentType());
        if (npc != null) {
            npc.moveTo(projectile.visualRef(),
                    projectile.position().x,
                    projectile.position().y,
                    projectile.position().z,
                    visualStore);
        }

        refreshProjectileVisual(projectile, now);
    }

    private void refreshProjectileVisual(ActiveProjectile projectile, long now) {
        if (projectile == null
                || projectile.visualRef() == null
                || !projectile.visualRef().isValid()
                || projectile.travelEffectId() == null
                || projectile.travelEffectId().isBlank()
                || now < projectile.nextVisualRefreshAtMillis()) {
            return;
        }

        Store<EntityStore> visualStore = projectile.visualRef().getStore();
        if (visualStore == null) {
            return;
        }

        if (applyEffectById(projectile.visualRef(), visualStore, projectile.travelEffectId())) {
            projectile.nextVisualRefreshAtMillis = now + PROJECTILE_VISUAL_REFRESH_MS;
        }
    }

    private void despawnProjectileVisual(ActiveProjectile projectile) {
        if (projectile == null || projectile.visualRef() == null || !projectile.visualRef().isValid()) {
            return;
        }

        Store<EntityStore> visualStore = projectile.visualRef().getStore();
        NPCEntity npc = visualStore != null
                ? visualStore.getComponent(projectile.visualRef(), NPCEntity.getComponentType())
                : null;
        if (npc != null) {
            npc.setToDespawn();
        }
        visualProxyRefs.remove(projectile.visualRef());
    }

    private void syncFieldVisual(ActiveField field, long now) {
        if (field == null || field.visualRefs() == null || field.visualRefs().isEmpty()) {
            return;
        }

        List<Vector3d> positions = buildFieldVisualPositions(
                field.center(),
                field.lineDirection(),
                field.ability(),
                field.halfWidth()
        );
        int limit = Math.min(positions.size(), field.visualRefs().size());
        for (int index = 0; index < limit; index++) {
            Ref<EntityStore> visualRef = field.visualRefs().get(index);
            if (visualRef == null || !visualRef.isValid()) {
                continue;
            }

            Store<EntityStore> visualStore = visualRef.getStore();
            if (visualStore == null) {
                continue;
            }

            NPCEntity npc = visualStore.getComponent(visualRef, NPCEntity.getComponentType());
            if (npc != null) {
                Vector3d position = positions.get(index);
                npc.moveTo(visualRef, position.x, position.y, position.z, visualStore);
            }
        }

        refreshFieldVisual(field, now);
    }

    private void refreshFieldVisual(ActiveField field, long now) {
        if (field == null
                || field.visualRefs() == null
                || field.visualRefs().isEmpty()
                || field.loopEffectId() == null
                || field.loopEffectId().isBlank()
                || now < field.nextVisualRefreshAtMillis()) {
            return;
        }

        boolean refreshed = false;
        for (Ref<EntityStore> visualRef : field.visualRefs()) {
            if (visualRef == null || !visualRef.isValid()) {
                continue;
            }

            Store<EntityStore> visualStore = visualRef.getStore();
            if (visualStore == null) {
                continue;
            }

            refreshed |= applyEffectById(visualRef, visualStore, field.loopEffectId());
        }

        if (refreshed) {
            field.nextVisualRefreshAtMillis = now + FIELD_VISUAL_REFRESH_MS;
        }
    }

    private void despawnFieldVisual(ActiveField field) {
        if (field == null || field.visualRefs() == null || field.visualRefs().isEmpty()) {
            return;
        }

        for (Ref<EntityStore> visualRef : field.visualRefs()) {
            if (visualRef == null || !visualRef.isValid()) {
                continue;
            }

            Store<EntityStore> visualStore = visualRef.getStore();
            NPCEntity npc = visualStore != null
                    ? visualStore.getComponent(visualRef, NPCEntity.getComponentType())
                    : null;
            if (npc != null) {
                npc.setToDespawn();
            }
            visualProxyRefs.remove(visualRef);
        }
    }

    private List<Vector3d> buildFieldVisualPositions(Vector3d center,
                                                     Vector3d lineDirection,
                                                     AbilityData ability,
                                                     double halfWidth) {
        if (center == null || ability == null) {
            return List.of();
        }

        List<Vector3d> positions = new ArrayList<>();
        Vector3d visualCenter = fieldVisualCenter(center, ability);
        positions.add(visualCenter);
        String castType = lower(ability.getCastType());
        if ("barrier".equals(castType) && lineDirection != null && lineDirection.isFinite()) {
            double span = Math.max(2.0, Math.min(Math.max(halfWidth, 0.0), 7.0));
            Vector3d normalized = normalize(lineDirection);
            for (double offset = -span; offset <= span + 0.001; offset += Math.max(2.25, span / 2.0)) {
                if (Math.abs(offset) < 0.3) {
                    continue;
                }
                positions.add(new Vector3d(visualCenter).fma(offset, normalized));
            }
            return positions;
        }

        if (!"ground_zone".equals(castType) && !"support_zone".equals(castType)) {
            return positions;
        }

        return buildAreaVisualPositions(visualCenter, ability);
    }

    private Vector3d fieldVisualCenter(Vector3d center, AbilityData ability) {
        Vector3d visualCenter = new Vector3d(center);
        if ("sinkhole".equals(lower(ability != null ? ability.getId() : null))) {
            visualCenter.y -= 1.0;
        }
        return visualCenter;
    }

    private List<Vector3d> buildAreaVisualPositions(Vector3d center, AbilityData ability) {
        if (center == null || ability == null) {
            return List.of();
        }

        List<Vector3d> positions = new ArrayList<>();
        positions.add(new Vector3d(center));
        if (isQuakeGroundImpactAbility(ability)) {
            return List.copyOf(positions);
        }
        double radius = ability.getRadius() > 0 ? ability.getRadius() : DEFAULT_AREA_RADIUS;
        double ringRadius = Math.max(1.8, Math.min(radius * 0.62, 5.5));
        positions.add(new Vector3d(center).add(ringRadius, 0.0, 0.0));
        positions.add(new Vector3d(center).add(-ringRadius, 0.0, 0.0));
        positions.add(new Vector3d(center).add(0.0, 0.0, ringRadius));
        positions.add(new Vector3d(center).add(0.0, 0.0, -ringRadius));
        if (radius >= 4.5) {
            double diagonal = ringRadius * 0.72;
            positions.add(new Vector3d(center).add(diagonal, 0.0, diagonal));
            positions.add(new Vector3d(center).add(-diagonal, 0.0, diagonal));
            positions.add(new Vector3d(center).add(diagonal, 0.0, -diagonal));
            positions.add(new Vector3d(center).add(-diagonal, 0.0, -diagonal));
        }
        return positions;
    }

    private MovementResult applyMovement(Player runtimePlayer,
                                         Ref<EntityStore> playerRef,
                                         Store<EntityStore> store,
                                         AbilityData ability) {
        String castType = lower(ability.getCastType());
        if (!MOVEMENT_CAST_TYPES.contains(castType)) {
            return MovementResult.none();
        }

        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        if (transform == null) {
            return MovementResult.none();
        }

        Transform currentTransform = transform.getTransform();
        if (currentTransform == null || currentTransform.getPosition() == null) {
            return MovementResult.none();
        }

        Vector3d direction = currentTransform.getDirection();
        if (direction == null || !direction.isFinite()) {
            direction = new Vector3d(0.0, 0.0, 1.0);
        } else {
            direction = new Vector3d(direction);
        }

        double horizontalDistance = resolveHorizontalMovement(ability, castType);
        double verticalDistance = resolveVerticalMovement(ability, castType);
        String playerId = resolveEntityId(playerRef, store);
        if (playerId != null) {
            double speedBonus = mod.getStatusEffectManager().getSpeedBonus(playerId);
            if (speedBonus > 0.0) {
                horizontalDistance *= (1.0 + speedBonus);
            }

            ActiveTransformation activeForm = activeTransformationsByPlayer.get(playerId);
            if (activeForm != null) {
                horizontalDistance *= activeForm.movementMultiplier();
                verticalDistance += activeForm.verticalBonus();
            }
        }
        if (horizontalDistance <= 0.0 && verticalDistance <= 0.0) {
            return MovementResult.none();
        }

        Vector3d horizontalDirection = new Vector3d(direction.x, 0.0, direction.z);
        if (!horizontalDirection.isFinite() || horizontalDirection.length() < 0.001) {
            horizontalDirection = new Vector3d(0.0, 0.0, 1.0);
        } else {
            horizontalDirection.normalize();
        }

        Vector3d start = new Vector3d(currentTransform.getPosition());
        Vector3d target = new Vector3d(start)
                .fma(horizontalDistance, horizontalDirection)
                .add(0.0, verticalDistance, 0.0);
        runtimePlayer.moveTo(playerRef, target.x, target.y, target.z, store);

        return new MovementResult(
                true,
                horizontalDistance,
                verticalDistance,
                start,
                new Vector3d(target),
                buildMovementSummary(castType, horizontalDistance, verticalDistance)
        );
    }

    private MovementContactRuntimeResult applyMovementContactRuntime(Player runtimePlayer,
                                                                     PlayerData player,
                                                                     StyleData style,
                                                                     AbilityData ability,
                                                                     PlaybackResult playback) {
        if (runtimePlayer == null || player == null || ability == null || playback == null
                || !playback.movementApplied() || playback.startPosition() == null || playback.endPosition() == null) {
            return MovementContactRuntimeResult.none();
        }

        String abilityId = lower(ability.getId());
        if (!"rockslide".equals(abilityId) && !"dust_devil".equals(abilityId)) {
            return MovementContactRuntimeResult.none();
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null && playerRef.isValid() ? playerRef.getStore() : null;
        if (playerRef == null || store == null) {
            return MovementContactRuntimeResult.none();
        }

        double radius = Math.max("rockslide".equals(abilityId) ? 2.35 : 2.75, ability.getRadius());
        int targetsHit = 0;
        double totalDamage = 0.0;
        for (Ref<EntityStore> targetRef : collectTargetsAlongSegment(store,
                playback.startPosition(),
                playback.endPosition(),
                radius,
                8)) {
            String entityId = resolveEntityId(targetRef, store);
            if (entityId == null || entityId.equals(player.getPlayerId())) {
                continue;
            }
            double damage = Math.max(1.0, resolveDamageAmount(player, ability) * 0.75);
            damage *= resolveIncomingDamageMultiplier(entityId);
            damage = mod.getStatusEffectManager().absorbDamage(entityId, damage);
            if (damage > 0.0) {
                Damage hitDamage = new Damage(new Damage.EntitySource(playerRef), DamageCause.PHYSICAL, (float) damage);
                DamageSystems.executeDamage(targetRef, store, hitDamage);
                applyPostDamageClassPassives(player, playerRef, entityId, damage, false);
                totalDamage += damage;
            }
            applyKnockbackFromPoint(targetRef, store, playback.startPosition(), ability);
            applyEffectById(targetRef, store, resolveImpactEffectId(player.getPlayerClass(), style != null ? style.getId() : currentStyleId(player), ability));
            targetsHit++;
        }

        if (targetsHit <= 0) {
            logTerraAbilityEvent(abilityId + ".dash", player, style, ability,
                    "targets=0 from=" + formatVector(playback.startPosition())
                            + " to=" + formatVector(playback.endPosition()));
            return MovementContactRuntimeResult.none();
        }

        player.getStatistics().setTotalDamageDealt(player.getStatistics().getTotalDamageDealt() + totalDamage);
        logTerraAbilityEvent(abilityId + ".dash", player, style, ability,
                "targets=" + targetsHit
                        + " damage=" + AbilityPresentation.formatDecimal(totalDamage)
                        + " from=" + formatVector(playback.startPosition())
                        + " to=" + formatVector(playback.endPosition()));
        return new MovementContactRuntimeResult(
                targetsHit,
                totalDamage,
                humanize(abilityId) + " contact pushed " + targetsHit + " target" + (targetsHit == 1 ? "" : "s")
        );
    }

    private CombatResolution applyCombat(Player runtimePlayer,
                                         PlayerData player,
                                         AbilityData ability,
                                         CastContext context) {
        double baseDamage = resolveDamageAmount(player, ability);
        if (baseDamage <= 0) {
            return CombatResolution.none();
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return CombatResolution.none();
        }

        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            return CombatResolution.none();
        }

        List<Ref<EntityStore>> targets = resolveTargets(playerRef, store, ability, context);
        if (targets.isEmpty()) {
            return new CombatResolution(0, 0.0, "No valid target in range");
        }

        DamageCause cause = isProjectileLike(ability) ? DamageCause.PROJECTILE : DamageCause.PHYSICAL;
        String impactEffectId = resolveImpactEffectId(player.getPlayerClass(), currentStyleId(player), ability);
        double castBuffMultiplier = resolveOutgoingDamageMultiplier(player);
        String castType = lower(ability.getCastType());
        String travelType = lower(ability.getTravelType());
        int hits = 0;
        double totalDamage = 0.0;
        int hitIndex = 0;

        for (Ref<EntityStore> targetRef : targets) {
            if (targetRef == null || !targetRef.isValid()) {
                continue;
            }

            String targetEntityId = resolveEntityId(targetRef, store);
            double resolvedDamage = baseDamage * castBuffMultiplier;
            resolvedDamage *= resolveTargetSequenceDamageMultiplier(ability, castType, hitIndex);
            resolvedDamage = applySpecialDamageModifiers(player, ability, targetRef, store, targetEntityId, resolvedDamage);
            applyTempestImpactEffects(ability, targetRef, store, playerRef, player.getPlayerId(), targetEntityId);

            if (targetEntityId != null) {
                resolvedDamage *= resolveIncomingDamageMultiplier(targetEntityId);
                resolvedDamage = mod.getStatusEffectManager().absorbDamage(targetEntityId, resolvedDamage);
            }

            if (resolvedDamage <= 0.0) {
                if (!isQuakeGroundImpactAbility(ability)) {
                    applyEffectById(targetRef, store, impactEffectId);
                } else {
                    applyAerialQuakeClarityEffect(ability, targetRef, store, impactEffectId);
                }
                continue;
            }

            Damage damage = new Damage(new Damage.EntitySource(playerRef), cause, (float) resolvedDamage);
            DamageSystems.executeDamage(targetRef, store, damage);
            reportAbilityKillIfDead(player.getPlayerId(), player, targetRef, store, targetEntityId);
            applyPostDamageClassPassives(player, playerRef, targetEntityId, resolvedDamage, true);
            if (!isQuakeGroundImpactAbility(ability)) {
                applyEffectById(targetRef, store, impactEffectId);
            } else {
                applyAerialQuakeClarityEffect(ability, targetRef, store, impactEffectId);
            }
            if ("chain".equals(castType) && travelType.contains("chain_lightning")) {
                applyTokenToTarget("shocked", targetRef, store, playerRef, player.getPlayerId(), ability);
            }
            hits++;
            hitIndex++;
            totalDamage += resolvedDamage;
        }

        if (hits == 0) {
            return CombatResolution.none();
        }

        String summary = hits == 1
                ? "1 hit for " + AbilityPresentation.formatDecimal(totalDamage) + " damage"
                : hits + " hits for " + AbilityPresentation.formatDecimal(totalDamage) + " damage";
        return new CombatResolution(hits, totalDamage, summary);
    }

    private void applyTempestImpactEffects(AbilityData ability,
                                           Ref<EntityStore> targetRef,
                                           Store<EntityStore> store,
                                           Ref<EntityStore> playerRef,
                                           String playerId,
                                           String targetEntityId) {
        if (ability == null || !lower(ability.getTerrainEffect()).contains("tempest")) {
            return;
        }

        boolean stunned = applyTargetToken("stun", targetRef, store, playerRef, playerId, ability);
        boolean slowed = applyTargetToken("slow", targetRef, store, playerRef, playerId, ability);
        LOG.info("[MOTM] Tempest impact effects applied: target=" + targetEntityId
                + " stun=" + stunned
                + " slow=" + slowed);
    }

    private SupportResolution applyCasterRuntime(Player runtimePlayer,
                                                 PlayerData player,
                                                 AbilityData ability) {
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return SupportResolution.none();
        }

        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            return SupportResolution.none();
        }

        List<String> summaryParts = new ArrayList<>();
        double healed = 0.0;
        double shielded = 0.0;
        int effectsApplied = 0;
        double sustainMultiplier = mod.getLevelingManager().getPlayerSustainMultiplier(player);

        if (ability.getHealPercent() > 0) {
            healed = healEntity(playerRef, store, ability.getHealPercent() * sustainMultiplier);
            if (healed > 0) {
                summaryParts.add("healed " + AbilityPresentation.formatDecimal(healed));
            } else {
                summaryParts.add("heal ready");
            }
        }

        if (ability.getShieldPercent() > 0) {
            shielded = applyShield(player.getPlayerId(), playerRef, store, ability, ability.getShieldPercent() * sustainMultiplier);
            if (shielded > 0) {
                summaryParts.add("shield " + AbilityPresentation.formatDecimal(shielded));
            }
        }

        if ("cleanse".equals(lower(ability.getCastType()))) {
            int removed = clearNegativeEffects(player.getPlayerId());
            if (removed > 0) {
                summaryParts.add("cleansed " + removed + " effect" + (removed == 1 ? "" : "s"));
            }
        }

        for (String token : parseEffectTokens(ability.getEffect())) {
            if ("heal".equals(token) || "shield".equals(token)) {
                continue;
            }
            if ("alloy_enhancement".equals(lower(ability.getId())) && "damage_buff".equals(token)) {
                continue;
            }

            if (!CASTER_EFFECT_TOKENS.contains(token)) {
                continue;
            }

            StatusEffect effect = createStatusEffect(token, ability, player.getPlayerId(), ability.getId());
            if (effect == null) {
                continue;
            }

            mod.getStatusEffectManager().applyEffect(player.getPlayerId(), effect);
            effectsApplied++;
            summaryParts.add("self " + humanize(token));
        }

        if (summaryParts.isEmpty()) {
            return SupportResolution.none();
        }

        return new SupportResolution(
                healed,
                shielded,
                effectsApplied,
                String.join(" | ", dedupeSummaryParts(summaryParts))
        );
    }

    private EffectResolution applyTargetEffects(Player runtimePlayer,
                                                PlayerData player,
                                                AbilityData ability,
                                                CastContext context) {
        List<String> tokens = parseEffectTokens(ability.getEffect());
        String castType = lower(ability.getCastType());
        String travelType = lower(ability.getTravelType());
        boolean appliesPull = ability.getPullForce() > 0 && "line_control".equals(castType);
        boolean chainLightning = "chain".equals(castType) && travelType.contains("chain_lightning");
        if (tokens.isEmpty() && !appliesPull && !chainLightning) {
            return EffectResolution.none();
        }

        boolean hasTargetEffect = tokens.stream().anyMatch(TARGET_EFFECT_TOKENS::contains);
        if (!hasTargetEffect && !appliesPull && !chainLightning) {
            return EffectResolution.none();
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return EffectResolution.none();
        }

        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            return EffectResolution.none();
        }

        List<Ref<EntityStore>> targets = resolveTargets(playerRef, store, ability, context);
        if (targets.isEmpty()) {
            return EffectResolution.none();
        }

        Set<String> affectedEntities = new LinkedHashSet<>();
        Set<String> appliedTokens = new LinkedHashSet<>();
        int appliedCount = 0;
        int pulledTargets = 0;

        for (Ref<EntityStore> targetRef : targets) {
            String entityId = resolveEntityId(targetRef, store);
            if (entityId == null || entityId.equals(player.getPlayerId())) {
                continue;
            }

            if (appliesPull && applyLineControlPull(targetRef, store, playerRef, ability)) {
                pulledTargets++;
                affectedEntities.add(entityId);
            }

            for (String token : tokens) {
                if (!TARGET_EFFECT_TOKENS.contains(token)) {
                    continue;
                }

                boolean applied = false;
                if (isQuakeGroundImpactAbility(ability) && "knockback".equals(token)) {
                    Vector3d origin = resolveCastContextPosition(context, playerRef, store);
                    applied = applyKnockbackFromPoint(targetRef, store, origin, ability);
                } else {
                    applied = applyTargetToken(token, targetRef, store, playerRef, player.getPlayerId(), ability);
                }
                if (applied) {
                    appliedCount++;
                    affectedEntities.add(entityId);
                    appliedTokens.add(token);
                }
            }

            if ("dominate".equals(lower(ability.getId()))) {
                for (String extraToken : List.of("root", "disoriented")) {
                    if (applyTargetToken(extraToken, targetRef, store, playerRef, player.getPlayerId(), ability)) {
                        appliedCount++;
                        affectedEntities.add(entityId);
                        appliedTokens.add(extraToken);
                    }
                }
            }

            if (chainLightning
                    && applyTargetToken("shocked", targetRef, store, playerRef, player.getPlayerId(), ability)) {
                appliedCount++;
                affectedEntities.add(entityId);
                appliedTokens.add("shocked");
            }
        }

        if (appliedCount <= 0 && pulledTargets <= 0) {
            return EffectResolution.none();
        }

        List<String> summaryParts = new ArrayList<>();
        if (!appliedTokens.isEmpty()) {
            summaryParts.add("applied "
                    + String.join(", ", humanizeTokens(appliedTokens))
                    + " to " + affectedEntities.size()
                    + " target" + (affectedEntities.size() == 1 ? "" : "s"));
        }
        if (pulledTargets > 0) {
            summaryParts.add("pulled " + pulledTargets + " target" + (pulledTargets == 1 ? "" : "s"));
        }

        return new EffectResolution(
                affectedEntities.size(),
                appliedCount + pulledTargets,
                String.join(" | ", summaryParts)
        );
    }

    private LineControlRuntimeResult startLineControlRuntime(Player runtimePlayer,
                                                             PlayerData player,
                                                             AbilityData ability,
                                                             CastContext context) {
        boolean vines = isVinesAbility(ability);
        if (!"line_control".equals(lower(ability.getCastType())) || (!vines && ability.getPullForce() <= 0.0)) {
            return LineControlRuntimeResult.none();
        }

        Ref<EntityStore> ownerRef = runtimePlayer.getReference();
        if (ownerRef == null || !ownerRef.isValid() || ownerRef.getStore() == null) {
            return LineControlRuntimeResult.none();
        }

        Store<EntityStore> store = ownerRef.getStore();
        Ref<EntityStore> targetRef = resolveTargets(ownerRef, store, ability, context).stream().findFirst().orElse(null);
        if (targetRef == null || !targetRef.isValid()) {
            return LineControlRuntimeResult.none();
        }

        double durationSeconds = inferLineControlDurationSeconds(ability);
        if (durationSeconds <= 0.0) {
            return LineControlRuntimeResult.none();
        }

        long now = System.currentTimeMillis();
        activeLineControls.removeIf(lineControl -> lineControl.ownerPlayerId().equals(player.getPlayerId()));
        Vector3d targetAnchor = vines ? getPosition(targetRef, store) : null;
        String vinesTerrain = "";
        if (vines && targetAnchor != null && runtimePlayer.getWorld() != null) {
            long expireAt = now + (long) (durationSeconds * 1000);
            vinesTerrain = placeSurfacePatchSelection(runtimePlayer.getWorld(), "vines", targetAnchor, 1, expireAt,
                    "Plant_Roots_Leafy", "Plant_Roots_Cave", "Plant_Vine_Thick_Roots");
        }
        activeLineControls.add(new ActiveLineControl(
                player.getPlayerId(),
                ownerRef,
                targetRef,
                ability,
                targetAnchor != null ? new Vector3d(targetAnchor) : null,
                now + (long) (durationSeconds * 1000),
                now + LINE_CONTROL_PULSE_INTERVAL_MS
        ));
        return new LineControlRuntimeResult(
                true,
                (vines ? "vines root/dot " : "current pull ")
                        + AbilityPresentation.formatDecimal(durationSeconds)
                        + "s"
                        + (vinesTerrain.isBlank() ? "" : " | " + vinesTerrain)
        );
    }

    private boolean processLineControlTick(ActiveLineControl lineControl, long now) {
        if (lineControl.ownerRef() == null || !lineControl.ownerRef().isValid()
                || lineControl.targetRef() == null || !lineControl.targetRef().isValid()) {
            return true;
        }

        if (now >= lineControl.expireAtMillis()) {
            return true;
        }

        if (now < lineControl.nextPulseAtMillis()) {
            return false;
        }

        Store<EntityStore> store = lineControl.ownerRef().getStore();
        if (store == null) {
            return true;
        }

        Vector3d ownerPosition = getPosition(lineControl.ownerRef(), store);
        Vector3d targetPosition = getPosition(lineControl.targetRef(), store);
        if (ownerPosition == null || targetPosition == null
                || distance(ownerPosition, targetPosition) > resolveRange(lineControl.ability()) + 3.0) {
            return true;
        }

        PlayerData player = mod.getPlayerDataManager().getOnlinePlayer(lineControl.ownerPlayerId());
        if (player == null) {
            return true;
        }

        if (lineControl.ability().getPullForce() > 0.0) {
            applyLineControlPull(lineControl.targetRef(), store, lineControl.ownerRef(), lineControl.ability());
        }
        if (isVinesAbility(lineControl.ability())) {
            applyVinesRootHold(lineControl, player, store);
        }
        applyRepeatingLineControlEffects(lineControl, player, store);
        lineControl.nextPulseAtMillis = now + LINE_CONTROL_PULSE_INTERVAL_MS;
        return false;
    }

    private void applyVinesRootHold(ActiveLineControl lineControl,
                                    PlayerData player,
                                    Store<EntityStore> store) {
        if (lineControl == null || player == null || store == null
                || lineControl.targetRef() == null || !lineControl.targetRef().isValid()) {
            return;
        }

        Vector3d anchor = lineControl.anchorPosition();
        if (anchor != null) {
            NPCEntity npc = store.getComponent(lineControl.targetRef(), NPCEntity.getComponentType());
            if (npc != null && !npc.isDespawning()) {
                npc.moveTo(lineControl.targetRef(), anchor.x, anchor.y, anchor.z, store);
                zeroVelocity(lineControl.targetRef(), store);
            }
        }

        String entityId = resolveEntityId(lineControl.targetRef(), store);
        if (entityId != null && !entityId.equals(player.getPlayerId())) {
            applyTargetToken("root", lineControl.targetRef(), store,
                    lineControl.ownerRef(), player.getPlayerId(), lineControl.ability());
            applyEffectById(lineControl.targetRef(), store, resolveImpactEffectId("terra", "arbor", lineControl.ability()));
            LOG.info("[MOTM][terra-audit] event=vines.hold target=" + entityId
                    + " anchor=" + formatVector(anchor));
        }
    }

    private FormRuntimeResult applyTransformation(Player runtimePlayer,
                                                  PlayerData player,
                                                  StyleData style,
                                                  AbilityData ability) {
        if (!"transformation".equals(lower(ability.getCastType()))) {
            return FormRuntimeResult.none();
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return FormRuntimeResult.none();
        }

        Store<EntityStore> store = playerRef.getStore();
        String effectId = resolveTransformationEffectId(ability.getId());
        if (effectId != null) {
            applyEffectById(playerRef, store, effectId);
        }

        String modelId = HytaleAssetResolver.resolveModelId(player.getPlayerClass(), style.getId(), ability);
        if (modelId == null || modelId.isBlank()) {
            modelId = ability.getName();
        }

        Vector3d origin = isMagmaSlingAbility(ability)
                ? resolveProjectileOrigin(playerRef, store, ability)
                : getPosition(playerRef, store);
        ActiveTransformation form = createTransformationState(player.getPlayerId(), playerRef, ability, modelId, origin);
        activeTransformationsByPlayer.put(player.getPlayerId(), form);
        nextTransformationPulseAtByPlayer.put(player.getPlayerId(), System.currentTimeMillis() + FORM_PULSE_INTERVAL_MS);

        return new FormRuntimeResult(true,
                "form " + humanize(modelId)
                        + " | " + form.summary());
    }

    private SummonRuntimeResult handleSummonRuntime(Player runtimePlayer,
                                                    PlayerData player,
                                                    StyleData style,
                                                    AbilityData ability,
                                                    CastContext context) {
        String castType = lower(ability.getCastType());
        if ("summon_buff".equals(castType)) {
            return buffOwnedSummons(runtimePlayer, player, ability);
        }

        if (ability.getSummonName() == null || ability.getSummonName().isBlank()) {
            return SummonRuntimeResult.none();
        }

        return spawnSummon(runtimePlayer, player, style, ability, context);
    }

    private SummonRuntimeResult buffOwnedSummons(Player runtimePlayer,
                                                 PlayerData player,
                                                 AbilityData ability) {
        List<ActiveSummon> summons = activeSummonsByOwner.getOrDefault(player.getPlayerId(), List.of());
        if (summons.isEmpty()) {
            return new SummonRuntimeResult(0, 0, "no active summons");
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null ? playerRef.getStore() : null;
        if (playerRef == null || store == null) {
            return SummonRuntimeResult.none();
        }

        double radius = ability.getRadius() > 0 ? ability.getRadius() : 12.0;
        Vector3d origin = getPosition(playerRef, store);
        if (origin == null) {
            return SummonRuntimeResult.none();
        }

        long now = System.currentTimeMillis();
        int buffed = 0;
        int commanded = 0;
        for (ActiveSummon summon : summons) {
            if (!summon.ref().isValid()) {
                continue;
            }

            Vector3d position = getPosition(summon.ref(), store);
            if (position == null || distance(origin, position) > radius) {
                continue;
            }

            applyEffectById(summon.ref(), store, resolveImpactEffectId(player.getPlayerClass(), summon.styleId, summon.ability));
            summon.extend((long) (Math.max(2.0, ability.getDurationSeconds()) * 1000));
            summon.buffExpireAtMillis = Math.max(
                    summon.buffExpireAtMillis,
                    now + (long) (Math.max(2.0, ability.getDurationSeconds()) * 1000)
            );
            summon.nextAttackAtMillis = Math.min(summon.nextAttackAtMillis, now + 150L);
            summon.targetLockExpireAtMillis = 0L;
            if (summon.awakened || now >= summon.hatchAtMillis) {
                Ref<EntityStore> targetRef = resolveSummonTarget(summon, store, now);
                if (targetRef != null && targetRef.isValid()) {
                    performSummonAttack(summon, player, targetRef, store, now);
                    commanded++;
                }
            }
            buffed++;
        }

        return buffed > 0
                ? new SummonRuntimeResult(0, buffed,
                "buffed " + buffed + " summon" + (buffed == 1 ? "" : "s")
                        + (commanded > 0 ? " | commanded " + commanded + " strike" + (commanded == 1 ? "" : "s") : ""))
                : new SummonRuntimeResult(0, 0, "no summons in range");
    }

    private SummonRuntimeResult spawnSummon(Player runtimePlayer,
                                            PlayerData player,
                                            StyleData style,
                                            AbilityData ability,
                                            CastContext context) {
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return SummonRuntimeResult.none();
        }

        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            return SummonRuntimeResult.none();
        }

        String modelId = resolveSummonModelId(player.getPlayerClass(), style.getId(), ability);
        if (modelId == null || modelId.isBlank()) {
            LOG.warning("[MOTM] No summon model mapping for " + ability.getId());
            return SummonRuntimeResult.none();
        }

        Vector3d spawnPosition = resolveSummonPosition(playerRef, store, ability, context);
        if (spawnPosition == null) {
            return SummonRuntimeResult.none();
        }

        World world = runtimePlayer.getWorld();
        NPCEntity summon = new NPCEntity(world);
        summon.setRoleName(modelId);
        summon.setDespawnTime((float) Math.max(2.0, ability.getDurationSeconds()));
        world.spawnEntity(summon, spawnPosition, new Rotation3f(0f, 0f, 0f));

        Ref<EntityStore> summonRef = summon.getReference();
        if (summonRef == null || !summonRef.isValid()) {
            return SummonRuntimeResult.none();
        }

        NPCEntity.setAppearance(summonRef, modelId, summonRef.getStore());
        applyEffectById(summonRef, summonRef.getStore(), resolveImpactEffectId(player.getPlayerClass(), style.getId(), ability));

        long expireAt = System.currentTimeMillis() + (long) (Math.max(2.0, ability.getDurationSeconds()) * 1000);
        activeSummonsByOwner.computeIfAbsent(player.getPlayerId(), ignored -> new ArrayList<>())
                .add(createActiveSummon(player, playerRef, summonRef, player.getPlayerClass(), style.getId(), ability, expireAt));

        LOG.info("[MOTM] Summon spawned: abilityId=" + ability.getId()
                + " model=" + modelId
                + " position=" + formatVector(spawnPosition)
                + " duration=" + AbilityPresentation.formatDecimal(Math.max(2.0, ability.getDurationSeconds())) + "s");

        return new SummonRuntimeResult(1, 0, "summoned " + humanize(modelId));
    }

    private boolean processSummonTick(ActiveSummon summon, long now) {
        if (summon.ref() == null || !summon.ref().isValid()) {
            return true;
        }

        if (now >= summon.expireAtMillis()) {
            return despawnSummon(summon);
        }

        if (now < summon.nextThinkAtMillis) {
            return false;
        }

        Store<EntityStore> store = summon.ref().getStore();
        if (store == null) {
            return true;
        }

        PlayerData owner = mod.getPlayerDataManager().getOnlinePlayer(summon.ownerPlayerId);
        if (owner == null) {
            return despawnSummon(summon);
        }

        if (now < summon.hatchAtMillis) {
            summon.nextThinkAtMillis = now + SUMMON_THINK_INTERVAL_MS;
            return false;
        }

        if (!summon.awakened) {
            awakenSummon(summon, store, now);
        }

        if (summon.ownerRef() == null || !summon.ownerRef().isValid()) {
            return despawnSummon(summon);
        }

        Ref<EntityStore> targetRef = resolveSummonTarget(summon, store, now);

        if (targetRef == null || !targetRef.isValid()) {
            summon.currentTargetRef = null;
            summon.targetLockExpireAtMillis = 0L;
            moveSummonTowardOwner(summon, store);
            summon.nextThinkAtMillis = now + SUMMON_THINK_INTERVAL_MS;
            return false;
        }

        Vector3d summonPosition = getPosition(summon.ref(), store);
        Vector3d targetPosition = getPosition(targetRef, store);
        if (summonPosition == null || targetPosition == null) {
            summon.nextThinkAtMillis = now + SUMMON_THINK_INTERVAL_MS;
            return false;
        }

        double distanceToTarget = distance(summonPosition, targetPosition);
        if (distanceToTarget > summon.attackRange) {
            moveSummonTowardTarget(summon, targetRef, store, summon.attackRange * 0.8);
            summon.nextThinkAtMillis = now + SUMMON_THINK_INTERVAL_MS;
            return false;
        }

        if (summon.ranged && !"clone".equals(summon.role) && distanceToTarget < summon.attackRange * 0.45) {
            moveSummonAwayFromTarget(summon, targetRef, store, summon.attackRange * 0.72);
        }

        if (now >= summon.nextAttackAtMillis) {
            performSummonAttack(summon, owner, targetRef, store, now);
        }

        summon.nextThinkAtMillis = now + SUMMON_THINK_INTERVAL_MS;
        return false;
    }

    private boolean despawnSummon(ActiveSummon summon) {
        Store<EntityStore> store = summon.ref() != null ? summon.ref().getStore() : null;
        NPCEntity npc = store != null ? store.getComponent(summon.ref(), NPCEntity.getComponentType()) : null;
        if (npc != null) {
            npc.setToDespawn();
        }
        return true;
    }

    private ActiveSummon createActiveSummon(PlayerData player,
                                            Ref<EntityStore> ownerRef,
                                            Ref<EntityStore> summonRef,
                                            String classId,
                                            String styleId,
                                            AbilityData ability,
                                            long expireAtMillis) {
        String summonName = lower(ability.getSummonName());
        long now = System.currentTimeMillis();
        String role = resolveSummonRole(summonName);
        boolean ranged = switch (role) {
            case "skirmisher", "artillery", "caster", "swarm", "clone" -> true;
            default -> false;
        };
        double attackRange = switch (role) {
            case "tank" -> 2.8;
            case "skirmisher", "clone" -> 7.5;
            case "artillery", "caster", "swarm" -> 9.5;
            default -> 3.2;
        };
        double chaseRange = Math.max(10.0, ability.getRange() > 0 ? ability.getRange() + 4.0 : 12.0);
        long attackIntervalMillis = switch (role) {
            case "tank" -> 1700L;
            case "clone" -> 900L;
            case "swarm" -> 1100L;
            case "artillery", "caster" -> 1400L;
            default -> 1250L;
        };
        double summonAttackSpeedBonus = mod.getLevelingManager().getAgilityMeleeAttackSpeedBonus(player);
        if (summonAttackSpeedBonus > 0.0) {
            attackIntervalMillis = Math.max(
                    450L,
                    Math.round(attackIntervalMillis / (1.0 + summonAttackSpeedBonus))
            );
        }
        long hatchAtMillis = "hatchling".equals(role) ? now + 2000L : now;
        double baseDamage = resolveSummonBaseDamage(player, ability, role);
        String statSnapshot = formatSummonStatSnapshot(player);

        return new ActiveSummon(
                player.getPlayerId(),
                summonRef,
                ownerRef,
                classId,
                styleId,
                ability,
                role,
                ranged,
                attackRange,
                chaseRange,
                attackIntervalMillis,
                hatchAtMillis,
                expireAtMillis,
                now,
                now,
                0L,
                baseDamage,
                player.getLevel(),
                statSnapshot,
                null,
                0L,
                !"hatchling".equals(role)
        );
    }

    private String formatSummonStatSnapshot(PlayerData player) {
        PlayerData.StatAllocation stats = player != null ? player.getStatAllocation() : null;
        if (stats == null) {
            return "vigor=0,tenacity=0,endurance=0,agility=0,luck=0";
        }
        return "vigor=" + stats.getVigor()
                + ",tenacity=" + stats.getTenacity()
                + ",endurance=" + stats.getEndurance()
                + ",agility=" + stats.getAgility()
                + ",luck=" + stats.getLuck();
    }

    private String resolveSummonRole(String summonName) {
        return switch (summonName) {
            case "frosty_golem" -> "tank";
            case "snow_imp", "skeleton_minion" -> "skirmisher";
            case "void_spawn" -> "caster";
            case "swamp_monster", "treant_sapling" -> "bruiser";
            case "locust_queen" -> "swarm";
            case "shadow_clone" -> "clone";
            case "scarak_egg" -> "hatchling";
            default -> "bruiser";
        };
    }

    private double resolveSummonBaseDamage(PlayerData player, AbilityData ability, String role) {
        double damage = ability.getDamagePercent() > 0
                ? ability.getDamagePercent() * (0.55 + (player.getLevel() * 0.035))
                : 5.0 + (player.getLevel() * 0.75);
        damage *= mod.getLevelingManager().getPlayerAbilityDamageMultiplier(player);
        return switch (role) {
            case "tank" -> damage * 0.75;
            case "clone" -> damage * 1.25;
            case "swarm" -> damage * 0.9;
            case "caster" -> damage * 1.1;
            default -> damage;
        };
    }

    private void moveSummonTowardOwner(ActiveSummon summon, Store<EntityStore> store) {
        Vector3d summonPosition = getPosition(summon.ref(), store);
        Vector3d ownerPosition = getPosition(summon.ownerRef(), store);
        if (summonPosition == null || ownerPosition == null || distance(summonPosition, ownerPosition) <= 4.5) {
            return;
        }

        NPCEntity npc = store.getComponent(summon.ref(), NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }

        Vector3d direction = normalize(subtract(ownerPosition, summonPosition));
        Vector3d destination = new Vector3d(ownerPosition).fma(-2.0, direction);
        npc.moveTo(summon.ref(), destination.x, destination.y, destination.z, store);
    }

    private void moveSummonTowardTarget(ActiveSummon summon,
                                        Ref<EntityStore> targetRef,
                                        Store<EntityStore> store,
                                        double desiredRange) {
        Vector3d summonPosition = getPosition(summon.ref(), store);
        Vector3d targetPosition = getPosition(targetRef, store);
        if (summonPosition == null || targetPosition == null) {
            return;
        }

        NPCEntity npc = store.getComponent(summon.ref(), NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }

        Vector3d direction = normalize(subtract(targetPosition, summonPosition));
        double distance = distance(summonPosition, targetPosition);
        double travel = Math.max(0.4, Math.min(4.0, distance - desiredRange));
        Vector3d destination = new Vector3d(summonPosition).fma(travel, direction);
        npc.moveTo(summon.ref(), destination.x, destination.y, destination.z, store);
    }

    private void moveSummonAwayFromTarget(ActiveSummon summon,
                                          Ref<EntityStore> targetRef,
                                          Store<EntityStore> store,
                                          double desiredDistance) {
        Vector3d summonPosition = getPosition(summon.ref(), store);
        Vector3d targetPosition = getPosition(targetRef, store);
        if (summonPosition == null || targetPosition == null) {
            return;
        }

        NPCEntity npc = store.getComponent(summon.ref(), NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }

        Vector3d direction = normalize(subtract(summonPosition, targetPosition));
        double distance = distance(summonPosition, targetPosition);
        double retreat = Math.max(0.5, Math.min(3.4, desiredDistance - distance));
        Vector3d destination = new Vector3d(summonPosition).fma(retreat, direction);
        npc.moveTo(summon.ref(), destination.x, destination.y, destination.z, store);
    }

    private void awakenSummon(ActiveSummon summon,
                              Store<EntityStore> store,
                              long now) {
        summon.awakened = true;
        summon.nextAttackAtMillis = Math.min(summon.nextAttackAtMillis, now + 200L);
        summon.buffExpireAtMillis = Math.max(summon.buffExpireAtMillis, now + 1800L);
        applyEffectById(summon.ref(), store, resolveImpactEffectId(summon.classId, summon.styleId, summon.ability));
    }

    private Ref<EntityStore> resolveSummonTarget(ActiveSummon summon,
                                                 Store<EntityStore> store,
                                                 long now) {
        Vector3d summonPosition = getPosition(summon.ref(), store);
        Vector3d ownerPosition = getPosition(summon.ownerRef(), store);
        Vector3d summonAnchor = summonPosition != null ? summonPosition : ownerPosition;

        if (summon.currentTargetRef != null
                && now < summon.targetLockExpireAtMillis
                && isValidNpcTarget(summon.currentTargetRef, store, summonAnchor, summon.chaseRange + 2.0)) {
            return summon.currentTargetRef;
        }

        Ref<EntityStore> targetRef = switch (summon.role) {
            case "tank" -> findNearestNpc(store,
                    ownerPosition != null ? ownerPosition : summonAnchor,
                    Math.max(8.0, summon.chaseRange));
            case "clone" -> findNearestNpc(store,
                    ownerPosition != null ? ownerPosition : summonAnchor,
                    Math.max(8.0, summon.attackRange + 3.0));
            default -> findNearestNpc(store, summonAnchor, summon.chaseRange);
        };

        summon.currentTargetRef = targetRef;
        summon.targetLockExpireAtMillis = targetRef == null
                ? 0L
                : now + ("tank".equals(summon.role) ? 2200L : 1400L);
        return targetRef;
    }

    private boolean isValidNpcTarget(Ref<EntityStore> targetRef,
                                     Store<EntityStore> store,
                                     Vector3d anchor,
                                     double radius) {
        if (targetRef == null || !targetRef.isValid() || anchor == null || store == null) {
            return false;
        }

        NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
        if (npc == null || npc.isDespawning() || isMotmSummon(npc)) {
            return false;
        }

        if (store.getComponent(targetRef, DeathComponent.getComponentType()) != null) {
            return false;
        }

        Vector3d targetPosition = getPosition(targetRef, store);
        return targetPosition != null && distance(anchor, targetPosition) <= radius;
    }

    private void moveSummonBesideTarget(ActiveSummon summon,
                                        Ref<EntityStore> targetRef,
                                        Store<EntityStore> store) {
        Vector3d targetPosition = getPosition(targetRef, store);
        if (targetPosition == null) {
            return;
        }

        NPCEntity npc = store.getComponent(summon.ref(), NPCEntity.getComponentType());
        if (npc == null) {
            return;
        }

        Vector3d ownerPosition = getPosition(summon.ownerRef(), store);
        Vector3d approach = ownerPosition != null
                ? normalize(subtract(targetPosition, ownerPosition))
                : new Vector3d(0.0, 0.0, 1.0);
        Vector3d destination = new Vector3d(targetPosition).fma(-1.15, approach);
        npc.moveTo(summon.ref(), destination.x, destination.y, destination.z, store);
    }

    private void performSummonAttack(ActiveSummon summon,
                                     PlayerData owner,
                                     Ref<EntityStore> targetRef,
                                     Store<EntityStore> store,
                                     long now) {
        if (targetRef == null || !targetRef.isValid()) {
            return;
        }

        if ("clone".equals(summon.role)) {
            moveSummonBesideTarget(summon, targetRef, store);
        }

        String targetEntityId = resolveEntityId(targetRef, store);
        double resolvedDamage = summon.baseDamage;
        if (nowWithinBuffWindow(summon, now)) {
            resolvedDamage *= 1.35;
        }
        if (targetEntityId != null) {
            resolvedDamage *= resolveIncomingDamageMultiplier(targetEntityId);
            resolvedDamage = mod.getStatusEffectManager().absorbDamage(targetEntityId, resolvedDamage);
        }

        if (resolvedDamage > 0.0) {
            DamageCause cause = summon.ranged ? DamageCause.PROJECTILE : DamageCause.PHYSICAL;
            Damage damage = new Damage(new Damage.EntitySource(summon.ref()), cause, (float) resolvedDamage);
            DamageSystems.executeDamage(targetRef, store, damage);
            applyPostDamageClassPassives(owner, summon.ownerRef(), targetEntityId, resolvedDamage, true);
            owner.getStatistics().setTotalDamageDealt(owner.getStatistics().getTotalDamageDealt() + resolvedDamage);
            applyLifesteal(summon.ownerRef(), owner.getPlayerId(), resolvedDamage);
        }

        applyEffectById(targetRef, store, resolveImpactEffectId(summon.classId, summon.styleId, summon.ability));
        applySummonAttackEffects(summon, owner, targetRef, store, now);
        summon.nextAttackAtMillis = now + Math.max(450L, nowWithinBuffWindow(summon, now)
                ? (long) (summon.attackIntervalMillis * 0.75)
                : summon.attackIntervalMillis);

        LOG.info("[MOTM] Summon attack resolved: abilityId=" + summon.ability.getId()
                + " summonRole=" + summon.role
                + " casterLevel=" + summon.casterLevel
                + " casterStats={" + summon.casterStatSnapshot + "}"
                + " target=" + (targetEntityId == null ? "<unknown>" : targetEntityId)
                + " damage=" + AbilityPresentation.formatDecimal(resolvedDamage));

        if ("clone".equals(summon.role)) {
            summon.expireAtMillis = Math.min(summon.expireAtMillis, now + 150L);
        }
    }

    private void applySummonAttackEffects(ActiveSummon summon,
                                          PlayerData owner,
                                          Ref<EntityStore> targetRef,
                                          Store<EntityStore> store,
                                          long now) {
        String token = resolveSummonAttackToken(summon);

        applyTokenToTarget(token, targetRef, store, summon.ref(), summon.ownerPlayerId, summon.ability);

        if ("tank".equals(summon.role)) {
            String summonEntityId = resolveEntityId(summon.ref(), store);
            if (summonEntityId != null) {
                applyShield(summonEntityId, summon.ref(), store, summon.ability, 4.0);
            }
            Vector3d summonPosition = getPosition(summon.ref(), store);
            if (summonPosition != null) {
                applyPullTowardsPoint(targetRef, store, summonPosition, summon.ability, 1.0, 0.55, 0.0);
            }
        }

        if (nowWithinBuffWindow(summon, now)
                && ("swarm".equals(summon.role) || "hatchling".equals(summon.role))) {
            applyTokenToTarget("dot", targetRef, store, summon.ref(), summon.ownerPlayerId, summon.ability);
        }

        applySpecificSummonAttackEffects(summon, owner, targetRef, store, now);
    }

    private String resolveSummonAttackToken(ActiveSummon summon) {
        String summonName = lower(summon.ability.getSummonName());
        if (summonName.isBlank()) {
            summonName = lower(summon.ability.getId());
        }

        return switch (summonName) {
            case "frosty_golem" -> "root";
            case "snow_imp" -> "slow";
            case "swamp_monster", "treant_sapling" -> "root";
            case "void_spawn" -> "vulnerability";
            case "locust_queen", "scarak_egg" -> "dot";
            case "shadow_clone" -> "vulnerability";
            default -> switch (summon.role) {
                case "tank", "skirmisher" -> "slow";
                case "caster" -> "curse";
                case "swarm", "hatchling" -> "dot";
                case "clone" -> "vulnerability";
                default -> "root";
            };
        };
    }

    private boolean nowWithinBuffWindow(ActiveSummon summon, long now) {
        return now < summon.buffExpireAtMillis;
    }

    private void applySpecificSummonAttackEffects(ActiveSummon summon,
                                                  PlayerData owner,
                                                  Ref<EntityStore> targetRef,
                                                  Store<EntityStore> store,
                                                  long now) {
        String summonName = lower(summon.ability.getSummonName());
        switch (summonName) {
            case "skeleton_minion" -> applyTokenToTarget("dot", targetRef, store, summon.ref(), summon.ownerPlayerId, summon.ability);
            case "snow_imp" -> {
                applyTokenToTarget("attack_slow", targetRef, store, summon.ref(), summon.ownerPlayerId, summon.ability);
                applySummonSplashToken(summon, targetRef, store, "slow", 2.6, 1);
            }
            case "frosty_golem" -> {
                applySummonSplashToken(summon, targetRef, store, "slow", 3.4, 2);
                applySummonSplashToken(summon, targetRef, store, "root", 2.0, 1);
            }
            case "swamp_monster" -> {
                applySummonSplashToken(summon, targetRef, store, "dot", 3.2, 2);
                applySummonSplashToken(summon, targetRef, store, "slow", 3.2, 2);
            }
            case "treant_sapling" -> {
                applySummonSplashToken(summon, targetRef, store, "root", 2.8, 2);
                if (summon.ownerRef() != null && summon.ownerRef().isValid()) {
                    applyShield(owner.getPlayerId(), summon.ownerRef(), store, summon.ability, 4.5);
                }
            }
            case "void_spawn" -> {
                applySummonSplashToken(summon, targetRef, store, "vulnerability", 3.6, 2);
                applySummonSplashDamage(summon, owner, targetRef, store, 0.35, 3.4, 2);
            }
            case "scarak_egg" -> {
                if (summon.awakened) {
                    applyTokenToTarget("vulnerability", targetRef, store, summon.ref(), summon.ownerPlayerId, summon.ability);
                    applySummonSplashToken(summon, targetRef, store, "dot", 2.8, 2);
                }
            }
            case "locust_queen" -> {
                applySummonSplashToken(summon, targetRef, store, "dot", 3.8, 3);
                if (nowWithinBuffWindow(summon, now)) {
                    applySummonSplashToken(summon, targetRef, store, "vulnerability", 3.8, 2);
                }
            }
            case "shadow_clone" -> applyTokenToTarget("blind", targetRef, store, summon.ref(), summon.ownerPlayerId, summon.ability);
            default -> {
            }
        }
    }

    private void applySummonSplashToken(ActiveSummon summon,
                                        Ref<EntityStore> primaryTargetRef,
                                        Store<EntityStore> store,
                                        String token,
                                        double radius,
                                        int maxTargets) {
        Vector3d center = getPosition(primaryTargetRef, store);
        if (center == null) {
            return;
        }

        for (Ref<EntityStore> splashTarget : collectNearbyNpcTargets(store, center, radius, maxTargets + 1)) {
            if (splashTarget == null || !splashTarget.isValid() || splashTarget.equals(primaryTargetRef)) {
                continue;
            }
            applyTokenToTarget(token, splashTarget, store, summon.ref(), summon.ownerPlayerId, summon.ability);
        }
    }

    private void applySummonSplashDamage(ActiveSummon summon,
                                         PlayerData owner,
                                         Ref<EntityStore> primaryTargetRef,
                                         Store<EntityStore> store,
                                         double damageRatio,
                                         double radius,
                                         int maxTargets) {
        Vector3d center = getPosition(primaryTargetRef, store);
        if (center == null || damageRatio <= 0.0) {
            return;
        }

        for (Ref<EntityStore> splashTarget : collectNearbyNpcTargets(store, center, radius, maxTargets + 1)) {
            if (splashTarget == null || !splashTarget.isValid() || splashTarget.equals(primaryTargetRef)) {
                continue;
            }

            String targetEntityId = resolveEntityId(splashTarget, store);
            double damageAmount = summon.baseDamage * damageRatio;
            if (targetEntityId != null) {
                damageAmount *= resolveIncomingDamageMultiplier(targetEntityId);
                damageAmount = mod.getStatusEffectManager().absorbDamage(targetEntityId, damageAmount);
            }
            if (damageAmount <= 0.0) {
                continue;
            }

            Damage splash = new Damage(new Damage.EntitySource(summon.ref()),
                    summon.ranged ? DamageCause.PROJECTILE : DamageCause.PHYSICAL,
                    (float) damageAmount);
            DamageSystems.executeDamage(splashTarget, store, splash);
            applyPostDamageClassPassives(owner, summon.ownerRef(), targetEntityId, damageAmount, true);
            owner.getStatistics().setTotalDamageDealt(owner.getStatistics().getTotalDamageDealt() + damageAmount);
        applyEffectById(splashTarget, store, resolveImpactEffectId(summon.classId, summon.styleId, summon.ability));
        }
    }

    private void applyTokenToTarget(String token,
                                    Ref<EntityStore> targetRef,
                                    Store<EntityStore> store,
                                    Ref<EntityStore> sourceRef,
                                    String sourcePlayerId,
                                    AbilityData ability) {
        applyTargetToken(token, targetRef, store, sourceRef, sourcePlayerId, ability);
    }

    private ActiveTransformation createTransformationState(String playerId,
                                                           Ref<EntityStore> ownerRef,
                                                           AbilityData ability,
                                                           String modelId,
                                                           Vector3d initialPosition) {
        long expireAt = System.currentTimeMillis() + (long) (Math.max(2.0, ability.getDurationSeconds()) * 1000);
        return switch (lower(ability.getId())) {
            case "smoke_form" -> new ActiveTransformation(
                    playerId, ownerRef, ability, modelId, expireAt,
                    0.05, 0.12, 1.22, 0.35, "blind",
                    0.95, 1.75, initialPosition,
                    "mist body + drift blinds");
            case "pterodactyl_form" -> new ActiveTransformation(
                    playerId, ownerRef, ability, modelId, expireAt,
                    0.15, 0.20, 1.42, 1.35, "slow",
                    1.15, 2.10, initialPosition,
                    "flight mobility + aerial drive-bys");
            case "triceratops_form" -> new ActiveTransformation(
                    playerId, ownerRef, ability, modelId, expireAt,
                    0.12, 0.24, 1.28, 0.0, "knockback",
                    1.05, 2.45, initialPosition,
                    "armored charge + impact stuns");
            case "t_rex_form" -> new ActiveTransformation(
                    playerId, ownerRef, ability, modelId, expireAt,
                    0.22, 0.34, 1.18, 0.0, "stun",
                    1.00, 3.25, initialPosition,
                    "primal power + rampage pressure");
            default -> new ActiveTransformation(
                    playerId, ownerRef, ability, modelId, expireAt,
                    0.10, 0.15, 1.10, 0.0, null,
                    1.20, 2.00, initialPosition,
                    "transformed combat state");
        };
    }

    private boolean processTransformationTick(ActiveTransformation form, long now) {
        if (form == null || form.ownerRef() == null || !form.ownerRef().isValid() || form.ownerRef().getStore() == null) {
            nextTransformationPulseAtByPlayer.remove(form != null ? form.playerId() : null);
            return true;
        }

        if (now >= form.expireAtMillis()) {
            nextTransformationPulseAtByPlayer.remove(form.playerId());
            return true;
        }

        long nextPulseAt = nextTransformationPulseAtByPlayer.getOrDefault(form.playerId(), now + FORM_PULSE_INTERVAL_MS);
        if (now < nextPulseAt) {
            return false;
        }

        PlayerData player = mod.getPlayerDataManager().getOnlinePlayer(form.playerId());
        if (player == null) {
            nextTransformationPulseAtByPlayer.remove(form.playerId());
            return true;
        }

        if (shouldEndTransformation(form, player)) {
            nextTransformationPulseAtByPlayer.remove(form.playerId());
            return true;
        }

        Store<EntityStore> store = form.ownerRef().getStore();
        Vector3d origin = getPosition(form.ownerRef(), store);
        if (origin == null) {
            nextTransformationPulseAtByPlayer.remove(form.playerId());
            return true;
        }

        refreshTransformationOwnerState(form, player, store);
        applyTransformationLocomotionPressure(form, player, store, origin);

        switch (lower(form.abilityId())) {
            case "smoke_form" -> applySmokeFormPulse(form, player, store, origin);
            case "pterodactyl_form" -> applyPterodactylFormPulse(form, player, store, origin);
            case "triceratops_form" -> applyTriceratopsFormPulse(form, player, store, origin);
            case "t_rex_form" -> applyTRexFormPulse(form, player, store, origin);
            default -> { }
        }

        nextTransformationPulseAtByPlayer.put(form.playerId(), now + FORM_PULSE_INTERVAL_MS);
        return false;
    }

    private boolean shouldEndTransformation(ActiveTransformation form, PlayerData player) {
        if (form == null || player == null) {
            return true;
        }

        String playerId = form.playerId();
        if (playerId == null || mod.getStatusEffectManager().isIncapacitated(playerId)) {
            return true;
        }

        String abilityId = lower(form.abilityId());
        if (("smoke_form".equals(abilityId) || "pterodactyl_form".equals(abilityId))
                && mod.getStatusEffectManager().hasEffect(playerId, StatusEffect.Type.GROUNDED)) {
            return true;
        }

        return !"corruptus".equalsIgnoreCase(player.getPlayerClass());
    }

    private void refreshTransformationOwnerState(ActiveTransformation form,
                                                 PlayerData player,
                                                 Store<EntityStore> store) {
        switch (lower(form.abilityId())) {
            case "smoke_form" -> applyOwnerRuntimeToken("evasion_buff", form, player);
            case "pterodactyl_form" -> {
                applyOwnerRuntimeToken("speed", form, player);
                applyOwnerRuntimeToken("evasion", form, player);
            }
            case "triceratops_form" -> {
                applyOwnerRuntimeToken("defense_buff", form, player);
                applyShield(player.getPlayerId(), form.ownerRef(), store, form.sourceAbility(), 3.0);
            }
            case "t_rex_form" -> applyOwnerRuntimeToken("attack_buff", form, player);
            default -> {
            }
        }
    }

    private void applyOwnerRuntimeToken(String token,
                                        ActiveTransformation form,
                                        PlayerData player) {
        StatusEffect effect = createStatusEffect(token, form.sourceAbility(), player.getPlayerId(), form.abilityId());
        if (effect != null) {
            mod.getStatusEffectManager().applyEffect(player.getPlayerId(), effect);
        }
    }

    private void applySmokeFormPulse(ActiveTransformation form,
                                     PlayerData player,
                                     Store<EntityStore> store,
                                     Vector3d origin) {
        Ref<EntityStore> target = findNearestNpc(store, origin, 3.4);
        if (target == null) {
            return;
        }

        applyTransformationPulseImpact(form, player, target, store, 0.30, "blind", false);
    }

    private void applyPterodactylFormPulse(ActiveTransformation form,
                                           PlayerData player,
                                           Store<EntityStore> store,
                                           Vector3d origin) {
        for (Ref<EntityStore> target : collectNearbyNpcTargets(store, origin, 5.5, 2)) {
            applyTransformationPulseImpact(form, player, target, store, 0.34, "slow", false);
        }
    }

    private void applyTriceratopsFormPulse(ActiveTransformation form,
                                           PlayerData player,
                                           Store<EntityStore> store,
                                           Vector3d origin) {
        for (Ref<EntityStore> target : collectNearbyNpcTargets(store, origin, 3.6, 3)) {
            applyTransformationPulseImpact(form, player, target, store, 0.46, null, true);
        }
    }

    private void applyTRexFormPulse(ActiveTransformation form,
                                    PlayerData player,
                                    Store<EntityStore> store,
                                    Vector3d origin) {
        double radius = Math.max(3.8, form.sourceAbility().getRadius() > 0 ? form.sourceAbility().getRadius() : 4.0);
        for (Ref<EntityStore> target : collectNearbyNpcTargets(store, origin, radius, 4)) {
            applyTransformationPulseImpact(form, player, target, store, 0.58, "vulnerability", false);
        }
    }

    private void applyTransformationPulseImpact(ActiveTransformation form,
                                                PlayerData player,
                                                Ref<EntityStore> targetRef,
                                                Store<EntityStore> store,
                                                double damageRatio,
                                                String token,
                                                boolean knockback) {
        if (targetRef == null || !targetRef.isValid()) {
            return;
        }

        String targetEntityId = resolveEntityId(targetRef, store);
        double damage = Math.max(3.0, resolveDamageAmount(player, form.sourceAbility()) * damageRatio);
        if (targetEntityId != null) {
            damage *= resolveIncomingDamageMultiplier(targetEntityId);
            damage = mod.getStatusEffectManager().absorbDamage(targetEntityId, damage);
        }

        if (damage > 0.0) {
            Damage pulseDamage = new Damage(new Damage.EntitySource(form.ownerRef()), DamageCause.PHYSICAL, (float) damage);
            DamageSystems.executeDamage(targetRef, store, pulseDamage);
            applyPostDamageClassPassives(player, form.ownerRef(), targetEntityId, damage, true);
            player.getStatistics().setTotalDamageDealt(player.getStatistics().getTotalDamageDealt() + damage);
            applyLifesteal(form.ownerRef(), player.getPlayerId(), damage);
        }

        applyEffectById(targetRef, store, resolveImpactEffectId(player.getPlayerClass(), currentStyleId(player), form.sourceAbility()));
        if (token != null && !token.isBlank()) {
            applyTokenToTarget(token, targetRef, store, form.ownerRef(), player.getPlayerId(), form.sourceAbility());
        }
        if (knockback) {
            applyKnockback(targetRef, store, form.ownerRef(), form.sourceAbility());
        }
    }

    private void applyTransformationLocomotionPressure(ActiveTransformation form,
                                                       PlayerData player,
                                                       Store<EntityStore> store,
                                                       Vector3d origin) {
        if (form == null || origin == null) {
            return;
        }

        Vector3d previous = form.lastOwnerPosition();
        form.lastOwnerPosition = new Vector3d(origin);
        if (previous == null) {
            return;
        }

        double movedDistance = distance(previous, origin);
        if (movedDistance < form.locomotionTriggerDistance()) {
            return;
        }

        double movementFactor = clamp(
                movedDistance / Math.max(0.75, form.locomotionTriggerDistance()),
                1.0,
                1.75
        );

        switch (lower(form.abilityId())) {
            case "smoke_form" -> applySmokeFormDriftImpact(form, player, store, previous, origin, movementFactor);
            case "pterodactyl_form" -> applyPterodactylGlideImpact(form, player, store, previous, origin, movementFactor);
            case "triceratops_form" -> applyTriceratopsChargeImpact(form, player, store, previous, origin, movementFactor);
            case "t_rex_form" -> applyTRexRampageImpact(form, player, store, origin, movementFactor);
            default -> {
            }
        }
    }

    private void applySmokeFormDriftImpact(ActiveTransformation form,
                                           PlayerData player,
                                           Store<EntityStore> store,
                                           Vector3d from,
                                           Vector3d to,
                                           double movementFactor) {
        for (Ref<EntityStore> target : collectTargetsAlongSegment(store, from, to, form.collisionRadius(), 2)) {
            applyTransformationPulseImpact(form, player, target, store, 0.16 * movementFactor, "blind", false);
            applyTokenToTarget("disoriented", target, store, form.ownerRef(), player.getPlayerId(), form.sourceAbility());
        }
    }

    private void applyPterodactylGlideImpact(ActiveTransformation form,
                                             PlayerData player,
                                             Store<EntityStore> store,
                                             Vector3d from,
                                             Vector3d to,
                                             double movementFactor) {
        for (Ref<EntityStore> target : collectTargetsAlongSegment(store, from, to, form.collisionRadius(), 3)) {
            applyTransformationPulseImpact(form, player, target, store, 0.22 * movementFactor, "slow", false);
            applyTokenToTarget("vulnerability", target, store, form.ownerRef(), player.getPlayerId(), form.sourceAbility());
            applyKnockback(target, store, form.ownerRef(), form.sourceAbility());
        }
    }

    private void applyTriceratopsChargeImpact(ActiveTransformation form,
                                              PlayerData player,
                                              Store<EntityStore> store,
                                              Vector3d from,
                                              Vector3d to,
                                              double movementFactor) {
        boolean hitAny = false;
        for (Ref<EntityStore> target : collectTargetsAlongSegment(store, from, to, form.collisionRadius(), 4)) {
            if (target == null || !target.isValid()) {
                continue;
            }

            String targetEntityId = resolveEntityId(target, store);
            double damage = Math.max(4.0, resolveDamageAmount(player, form.sourceAbility()) * 0.36 * movementFactor);
            if (targetEntityId != null) {
                damage *= resolveIncomingDamageMultiplier(targetEntityId);
                damage = mod.getStatusEffectManager().absorbDamage(targetEntityId, damage);
            }

            if (damage > 0.0) {
                Damage impactDamage = new Damage(new Damage.EntitySource(form.ownerRef()), DamageCause.PHYSICAL, (float) damage);
                DamageSystems.executeDamage(target, store, impactDamage);
                applyPostDamageClassPassives(player, form.ownerRef(), targetEntityId, damage, true);
                player.getStatistics().setTotalDamageDealt(player.getStatistics().getTotalDamageDealt() + damage);
                applyLifesteal(form.ownerRef(), player.getPlayerId(), damage);
            }

            applyEffectById(target, store, resolveImpactEffectId(player.getPlayerClass(), currentStyleId(player), form.sourceAbility()));
            KnockbackResult result = applyKnockbackResult(target, store, form.ownerRef(), form.sourceAbility());
            if (result.collidedWithWall()) {
                applyTokenToTarget("stun", target, store, form.ownerRef(), player.getPlayerId(), form.sourceAbility());
            }
            hitAny = true;
        }

        if (hitAny) {
            applyShield(player.getPlayerId(), form.ownerRef(), store, form.sourceAbility(), 2.5);
        }
    }

    private void applyTRexRampageImpact(ActiveTransformation form,
                                        PlayerData player,
                                        Store<EntityStore> store,
                                        Vector3d origin,
                                        double movementFactor) {
        double radius = Math.max(3.8, form.collisionRadius());
        for (Ref<EntityStore> target : collectNearbyNpcTargets(store, origin, radius, 4)) {
            applyTransformationPulseImpact(form, player, target, store, 0.34 * movementFactor, "vulnerability", false);
            applyTokenToTarget("disoriented", target, store, form.ownerRef(), player.getPlayerId(), form.sourceAbility());
        }
    }

    private List<Ref<EntityStore>> collectNearbyNpcTargets(Store<EntityStore> store,
                                                           Vector3d center,
                                                           double radius,
                                                           int maxTargets) {
        List<NearbyTargetCandidate> candidates = new ArrayList<>();

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid()) {
                    continue;
                }

                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning() || isMotmSummon(npc)) {
                    continue;
                }

                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }

                TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
                if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                    continue;
                }

                double candidateDistance = distance(center, transform.getTransform().getPosition());
                if (candidateDistance <= radius) {
                    candidates.add(new NearbyTargetCandidate(ref, candidateDistance));
                }
            }
        });

        candidates.sort((left, right) -> Double.compare(left.distance(), right.distance()));
        List<Ref<EntityStore>> targets = new ArrayList<>();
        for (NearbyTargetCandidate candidate : candidates) {
            targets.add(candidate.ref());
            if (maxTargets > 0 && targets.size() >= maxTargets) {
                break;
            }
        }
        return List.copyOf(targets);
    }

    private List<Ref<EntityStore>> collectTargetsAlongSegment(Store<EntityStore> store,
                                                              Vector3d from,
                                                              Vector3d to,
                                                              double radius,
                                                              int maxTargets) {
        if (store == null || from == null || to == null) {
            return List.of();
        }

        List<SegmentTargetCandidate> candidates = new ArrayList<>();
        Vector3d segment = subtract(to, from);
        double segmentLengthSquared = Math.max(0.0001, dot(segment, segment));

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid()) {
                    continue;
                }

                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning() || isMotmSummon(npc)) {
                    continue;
                }

                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }

                TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
                if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                    continue;
                }

                Vector3d targetPosition = transform.getTransform().getPosition();
                double normalizedProjection = dot(subtract(targetPosition, from), segment) / segmentLengthSquared;
                double clampedProjection = clamp(normalizedProjection, 0.0, 1.0);
                Vector3d nearestPoint = new Vector3d(from).fma(clampedProjection, segment);
                double candidateDistance = distance(nearestPoint, targetPosition);
                if (candidateDistance <= radius) {
                    candidates.add(new SegmentTargetCandidate(ref, distance(from, nearestPoint)));
                }
            }
        });

        candidates.sort((left, right) -> Double.compare(left.alongDistance(), right.alongDistance()));
        List<Ref<EntityStore>> targets = new ArrayList<>();
        for (SegmentTargetCandidate candidate : candidates) {
            targets.add(candidate.ref());
            if (maxTargets > 0 && targets.size() >= maxTargets) {
                break;
            }
        }
        return List.copyOf(new LinkedHashSet<>(targets));
    }

    private ChannelRuntimeResult startChannelRuntime(Player runtimePlayer,
                                                     PlayerData player,
                                                     AbilityData ability,
                                                     CastContext context) {
        if (!"channel".equals(lower(ability.getCastType()))) {
            return ChannelRuntimeResult.none();
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid()) {
            return ChannelRuntimeResult.none();
        }

        Store<EntityStore> store = playerRef.getStore();
        if (store == null) {
            return ChannelRuntimeResult.none();
        }

        Ref<EntityStore> targetRef = resolveTargets(playerRef, store, ability, context).stream().findFirst().orElse(null);
        if (targetRef == null || !targetRef.isValid()) {
            return new ChannelRuntimeResult(false, "channel failed: no target");
        }

        long now = System.currentTimeMillis();
        long expireAt = now + (long) (Math.max(1.5, ability.getDurationSeconds()) * 1000);
        activeChannels.removeIf(channel -> channel.ownerPlayerId().equals(player.getPlayerId()));
        activeChannels.add(new ActiveChannel(
                player.getPlayerId(),
                playerRef,
                targetRef,
                ability,
                expireAt,
                now + CHANNEL_PULSE_INTERVAL_MS
        ));
        return new ChannelRuntimeResult(true,
                "channeling " + humanize(ability.getName()) + " for "
                        + AbilityPresentation.formatDecimal((expireAt - now) / 1000.0) + "s");
    }

    private double inferLineControlDurationSeconds(AbilityData ability) {
        if (ability == null) {
            return 0.0;
        }

        if (isVinesAbility(ability)) {
            return 30.0;
        }

        if (ability.getDurationSeconds() > 0.0) {
            return Math.max(1.0, ability.getDurationSeconds());
        }

        String travelType = lower(ability.getTravelType());
        if (travelType.contains("current") || travelType.contains("undertow")) {
            return 1.8;
        }
        return 1.2;
    }

    private void applyRepeatingLineControlEffects(ActiveLineControl lineControl,
                                                  PlayerData player,
                                                  Store<EntityStore> store) {
        String targetEntityId = resolveEntityId(lineControl.targetRef(), store);
        if (targetEntityId == null || targetEntityId.equals(player.getPlayerId())) {
            return;
        }

        for (String token : parseEffectTokens(lineControl.ability().getEffect())) {
            if (!TARGET_EFFECT_TOKENS.contains(token)
                    || "knockback".equals(token)
                    || "stun_if_wall".equals(token)) {
                continue;
            }
            applyTargetToken(token, lineControl.targetRef(), store,
                    lineControl.ownerRef(), player.getPlayerId(), lineControl.ability());
        }
    }

    private boolean processChannelTick(ActiveChannel channel, long now) {
        if (channel.ownerRef() == null || !channel.ownerRef().isValid()
                || channel.targetRef() == null || !channel.targetRef().isValid()) {
            return true;
        }

        if (now >= channel.expireAtMillis()) {
            return true;
        }

        if (now < channel.nextPulseAtMillis()) {
            return false;
        }

        Store<EntityStore> store = channel.ownerRef().getStore();
        if (store == null) {
            return true;
        }

        PlayerData player = mod.getPlayerDataManager().getOnlinePlayer(channel.ownerPlayerId());
        if (player == null) {
            return true;
        }

        Vector3d ownerPosition = getPosition(channel.ownerRef(), store);
        Vector3d targetPosition = getPosition(channel.targetRef(), store);
        if (ownerPosition == null || targetPosition == null || distance(ownerPosition, targetPosition) > resolveRange(channel.ability()) + 2.0) {
            return true;
        }

        String targetEntityId = resolveEntityId(channel.targetRef(), store);
        double damage = resolveDamageAmount(player, channel.ability()) * 0.55 * resolveOutgoingDamageMultiplier(player);
        if (targetEntityId != null) {
            damage *= resolveIncomingDamageMultiplier(targetEntityId);
            damage = mod.getStatusEffectManager().absorbDamage(targetEntityId, damage);
        }

        if (damage > 0.0) {
            Damage pulseDamage = new Damage(new Damage.EntitySource(channel.ownerRef()), DamageCause.PHYSICAL, (float) damage);
            DamageSystems.executeDamage(channel.targetRef(), store, pulseDamage);
            applyPostDamageClassPassives(player, channel.ownerRef(), targetEntityId, damage, true);
            player.getStatistics().setTotalDamageDealt(player.getStatistics().getTotalDamageDealt() + damage);
            applyLifesteal(channel.ownerRef(), player.getPlayerId(), damage);
            if ("life_drain".equals(lower(channel.ability().getId()))) {
                double siphoned = healEntityFlat(channel.ownerRef(), store, damage * 0.45);
                if (siphoned > 0.0) {
                    player.getStatistics().setTotalHealingDone(player.getStatistics().getTotalHealingDone() + siphoned);
                }
            }
        }

        applyEffectById(channel.targetRef(), store, resolveImpactEffectId(player.getPlayerClass(), currentStyleId(player), channel.ability()));
        channel.nextPulseAtMillis = now + CHANNEL_PULSE_INTERVAL_MS;
        return false;
    }

    private WeaponFollowUpResult armWeaponFollowUp(PlayerData player, AbilityData ability) {
        if (!shouldArmWeaponFollowUp(ability)) {
            return WeaponFollowUpResult.none();
        }

        List<String> tokens = parseEffectTokens(ability.getEffect());
        int uses = resolveFollowUpUses(ability, tokens);
        long expireAt = System.currentTimeMillis() + (long) (Math.max(2.0, ability.getDurationSeconds()) * 1000);
        String riderToken = resolveFollowUpRiderToken(ability);
        double flatDamageBonus = resolveFollowUpFlatDamageBonus(ability, tokens);
        double lifestealBonus = tokens.contains("lifesteal") ? 0.18 : 0.0;
        double shieldPercentOnHit = resolveFollowUpShieldPercentOnHit(ability);
        double healRatioOnHit = resolveFollowUpHealRatioOnHit(ability, tokens);
        double splashRadius = resolveFollowUpSplashRadius(ability);
        double splashDamageRatio = resolveFollowUpSplashDamageRatio(ability);
        String secondaryRiderToken = resolveFollowUpSecondaryRiderToken(ability);

        activeWeaponFollowUpsByPlayer.put(
                player.getPlayerId(),
                new ActiveWeaponFollowUp(
                        player.getPlayerId(),
                        ability,
                        expireAt,
                        uses,
                        flatDamageBonus,
                        riderToken,
                        lifestealBonus,
                        shieldPercentOnHit,
                        healRatioOnHit,
                        splashRadius,
                        splashDamageRatio,
                        secondaryRiderToken,
                        resolveFollowUpDamageMultiplierBonus(ability),
                        null
                )
        );

        return new WeaponFollowUpResult(true,
                "weapon follow-up ready x" + uses + " via " + humanize(ability.getName()));
    }

    public synchronized String handleWeaponFollowUpHit(Player runtimePlayer,
                                                       PlayerData player,
                                                       Ref<EntityStore> targetRef,
                                                       String itemId) {
        if (runtimePlayer == null || player == null || targetRef == null || !targetRef.isValid()) {
            return null;
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return null;
        }

        Store<EntityStore> store = playerRef.getStore();
        NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
        if (npc == null || npc.isDespawning() || isMotmSummon(npc) || store.getComponent(targetRef, DeathComponent.getComponentType()) != null) {
            return null;
        }

        ActiveWeaponFollowUp followUp = activeWeaponFollowUpsByPlayer.get(player.getPlayerId());
        if (isAlloyFollowUp(followUp)) {
            return null;
        }
        ActiveTransformation form = activeTransformationsByPlayer.get(player.getPlayerId());
        boolean hasClassPassiveWeaponAttack = mod.getClassPassiveManager().hasWeaponAttackPassive(player);
        boolean hasOneShot = mod.getStatusEffectManager().hasEffect(player.getPlayerId(), StatusEffect.Type.DAMAGE_BUFF)
                || mod.getStatusEffectManager().hasEffect(player.getPlayerId(), StatusEffect.Type.STEALTH);
        boolean hasAttackBuff = mod.getStatusEffectManager().hasEffect(player.getPlayerId(), StatusEffect.Type.ATTACK_BUFF);
        if (followUp == null && form == null && !hasOneShot && !hasAttackBuff && !hasClassPassiveWeaponAttack) {
            return null;
        }

        String bindFailure = validateOrBindFollowUpItem(player.getPlayerId(), followUp, itemId);
        if (bindFailure != null) {
            clearAlloyHeldItemVisual(playerRef, store);
            return bindFailure;
        }

        String targetEntityId = resolveEntityId(targetRef, store);
        double modifier = 1.0
                + mod.getStatusEffectManager().getDamageIncrease(player.getPlayerId())
                + mod.getStatusEffectManager().consumeOneShot(player.getPlayerId(), StatusEffect.Type.DAMAGE_BUFF)
                + mod.getStatusEffectManager().consumeOneShot(player.getPlayerId(), StatusEffect.Type.STEALTH);
        if (followUp != null) {
            modifier += followUp.damageMultiplierBonus;
        }
        if (form != null) {
            modifier += form.weaponBonus();
        }

        double baseDamage = ((4.0 + (player.getLevel() * 0.9))
                * mod.getLevelingManager().getPlayerAbilityDamageMultiplier(player))
                + (followUp != null ? followUp.flatDamageBonus : 0.0);
        double resolvedDamage = baseDamage * modifier;
        ClassPassiveManager.WeaponAttackPassiveBonus passiveBonus =
                mod.getClassPassiveManager().consumeWeaponAttackBonus(player, playerRef, store, resolvedDamage);
        resolvedDamage += passiveBonus.bonusDamage();
        if (targetEntityId != null) {
            resolvedDamage *= resolveIncomingDamageMultiplier(targetEntityId);
            resolvedDamage = mod.getStatusEffectManager().absorbDamage(targetEntityId, resolvedDamage);
        }

        if (resolvedDamage > 0.0) {
            Damage damage = new Damage(new Damage.EntitySource(playerRef), DamageCause.PHYSICAL, (float) resolvedDamage);
            DamageSystems.executeDamage(targetRef, store, damage);
            applyPostDamageClassPassives(player, playerRef, targetEntityId, resolvedDamage, false);
            player.getStatistics().setTotalDamageDealt(player.getStatistics().getTotalDamageDealt() + resolvedDamage);
            applyLifesteal(playerRef, player.getPlayerId(), resolvedDamage);
        }

        applyEffectById(targetRef, store, resolveImpactEffectId(player.getPlayerClass(), currentStyleId(player), followUp != null ? followUp.sourceAbility() : (form != null ? form.sourceAbility() : null)));
        if (followUp != null && followUp.riderToken != null) {
            applyTokenToTarget(followUp.riderToken, targetRef, store, playerRef, player.getPlayerId(), followUp.sourceAbility());
        }
        if (followUp != null && followUp.secondaryRiderToken != null) {
            applyTokenToTarget(followUp.secondaryRiderToken, targetRef, store, playerRef, player.getPlayerId(), followUp.sourceAbility());
        }
        applyTransformationWeaponRider(form, targetRef, store, playerRef, player.getPlayerId());
        applyTransformationWeaponImpact(form, player, targetRef, store, playerRef, resolvedDamage);

        if (followUp != null && followUp.shieldPercentOnHit > 0.0) {
            applyShield(player.getPlayerId(), playerRef, store, followUp.sourceAbility(), followUp.shieldPercentOnHit);
        }
        if (followUp != null && followUp.lifestealBonus > 0.0 && resolvedDamage > 0.0) {
            healEntityFlat(playerRef, store, resolvedDamage * followUp.lifestealBonus);
        }
        if (followUp != null && followUp.healRatioOnHit > 0.0 && resolvedDamage > 0.0) {
            healEntityFlat(playerRef, store, resolvedDamage * followUp.healRatioOnHit);
        }
        if (followUp != null && followUp.splashRadius > 0.0 && followUp.splashDamageRatio > 0.0 && resolvedDamage > 0.0) {
            applyWeaponFollowUpSplash(playerRef, player, targetRef, store, followUp, resolvedDamage);
        }

        if (followUp != null) {
            restoreHeldItemDurability(runtimePlayer, itemId);
            followUp.remainingUses--;
            if (followUp.remainingUses <= 0) {
                activeWeaponFollowUpsByPlayer.remove(player.getPlayerId());
            }
        }

        List<String> summaryParts = new ArrayList<>();
        summaryParts.add("[MOTM] Weapon follow-up: +" + AbilityPresentation.formatDecimal(resolvedDamage)
                + " damage" + (followUp != null ? " via " + humanize(followUp.sourceAbilityId()) : ""));
        if (followUp != null && followUp.splashRadius > 0.0 && followUp.splashDamageRatio > 0.0) {
            summaryParts.add("splash ready");
        }
        if (followUp != null && followUp.healRatioOnHit > 0.0) {
            summaryParts.add("healing payoff");
        }
        if (passiveBonus.applied() && !passiveBonus.summary().isBlank()) {
            summaryParts.add(passiveBonus.summary());
        }
        return String.join(" | ", summaryParts);
    }

    public synchronized boolean hasActiveWeaponFollowUp(String playerId, String abilityId) {
        if (playerId == null || playerId.isBlank() || abilityId == null || abilityId.isBlank()) {
            return false;
        }
        ActiveWeaponFollowUp followUp = activeWeaponFollowUpsByPlayer.get(playerId);
        return followUp != null && abilityId.equalsIgnoreCase(followUp.sourceAbilityId());
    }

    public synchronized String handleNativeWeaponDamage(Player runtimePlayer,
                                                        PlayerData player,
                                                        Ref<EntityStore> targetRef,
                                                        String itemId,
                                                        Damage damage) {
        if (runtimePlayer == null || player == null || targetRef == null || !targetRef.isValid()
                || itemId == null || itemId.isBlank() || damage == null) {
            return null;
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null && playerRef.isValid() ? playerRef.getStore() : null;
        if (store == null) {
            return null;
        }

        ActiveWeaponFollowUp followUp = activeWeaponFollowUpsByPlayer.get(player.getPlayerId());
        if (!isAlloyFollowUp(followUp)) {
            return null;
        }

        String bindFailure = validateOrBindFollowUpItem(player.getPlayerId(), followUp, itemId);
        if (bindFailure != null) {
            return bindFailure;
        }

        applyAlloyHeldItemVisual(playerRef, store);
        float before = damage.getAmount();
        float after = (float) (before * (1.0 + followUp.damageMultiplierBonus));
        damage.setAmount(after);
        applyEffectById(targetRef, store, resolveImpactEffectId(player.getPlayerClass(), currentStyleId(player), followUp.sourceAbility()));
        if (followUp.secondaryRiderToken != null) {
            applyTokenToTarget(followUp.secondaryRiderToken, targetRef, store, playerRef, player.getPlayerId(), followUp.sourceAbility());
        }
        boolean restored = restoreHeldItemDurability(runtimePlayer, itemId);

        followUp.remainingUses--;
        if (followUp.remainingUses <= 0) {
            activeWeaponFollowUpsByPlayer.remove(player.getPlayerId());
            clearAlloyHeldItemVisual(playerRef, store);
        }

        String message = "[MOTM] Alloy Enhancement hit: "
                + AbilityPresentation.formatDecimal(before)
                + " -> "
                + AbilityPresentation.formatDecimal(after)
                + " damage"
                + (restored ? " | durability protected" : "")
                + (followUp.remainingUses > 0 ? " | " + followUp.remainingUses + " use(s) left" : " | Alloy finished");
        LOG.info(message + " playerId=" + player.getPlayerId() + " item=" + itemId);
        return message;
    }

    public synchronized String handleAlloyToolUse(Player runtimePlayer,
                                                  PlayerData player,
                                                  String itemId) {
        if (runtimePlayer == null || player == null || itemId == null || itemId.isBlank()) {
            return null;
        }

        ActiveWeaponFollowUp followUp = activeWeaponFollowUpsByPlayer.get(player.getPlayerId());
        if (!isAlloyFollowUp(followUp)) {
            return null;
        }

        String bindFailure = validateOrBindFollowUpItem(player.getPlayerId(), followUp, itemId);
        if (bindFailure != null) {
            clearAlloyHeldItemVisual(runtimePlayer);
            return bindFailure;
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null && playerRef.isValid() ? playerRef.getStore() : null;
        applyAlloyHeldItemVisual(playerRef, store);
        boolean restored = restoreHeldItemDurability(runtimePlayer, itemId);
        followUp.remainingUses--;
        if (followUp.remainingUses <= 0) {
            activeWeaponFollowUpsByPlayer.remove(player.getPlayerId());
            clearAlloyHeldItemVisual(runtimePlayer);
        }

        return "[MOTM] Alloy durability shield: "
                + (restored ? "protected " : "tracked ")
                + itemId
                + " use"
                + (followUp.remainingUses > 0 ? " | " + followUp.remainingUses + " Alloy use(s) left" : " | Alloy finished");
    }

    private void applyAlloyHeldItemVisual(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        if (playerRef == null || store == null) {
            return;
        }
        applyEffectById(playerRef, store, "MOTM_Proof_Alloy_Enhancement");
    }

    private void clearAlloyHeldItemVisual(Player runtimePlayer) {
        if (runtimePlayer == null) {
            return;
        }
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null && playerRef.isValid() ? playerRef.getStore() : null;
        clearAlloyHeldItemVisual(playerRef, store);
    }

    private void clearAlloyHeldItemVisual(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        if (removeEffectById(playerRef, store, "MOTM_Proof_Alloy_Enhancement")) {
            LOG.info("[MOTM] Alloy Enhancement visual cleared.");
        }
    }

    private boolean processWeaponFollowUpExpiry(String playerId,
                                                ActiveWeaponFollowUp followUp,
                                                Store<EntityStore> currentStore,
                                                long now) {
        if (followUp == null) {
            return true;
        }
        boolean expired = now >= followUp.expireAtMillis() || followUp.remainingUses() <= 0;
        if (expired && isAlloyFollowUp(followUp)) {
            Player runtimePlayer = mod.getRuntimePlayer(playerId);
            Ref<EntityStore> playerRef = runtimePlayer != null ? runtimePlayer.getReference() : null;
            if (playerRef != null && playerRef.isValid()) {
                if (!canMutateInCurrentStore(playerRef, currentStore)) {
                    return false;
                }
                clearAlloyHeldItemVisual(playerRef, currentStore);
            } else {
                LOG.info("[MOTM] Alloy Enhancement visual clear skipped: player unavailable playerId=" + playerId);
            }
            LOG.info("[MOTM] Alloy Enhancement ended: "
                    + (now >= followUp.expireAtMillis() ? "duration expired" : "uses exhausted")
                    + " playerId=" + playerId);
        }
        return expired;
    }

    private boolean shouldArmWeaponFollowUp(AbilityData ability) {
        String castType = lower(ability.getCastType());
        if (!"self_buff".equals(castType) && !"dash_buff".equals(castType)) {
            return false;
        }
        if ("metal_coat".equals(lower(ability.getId())) || "obsidian_skin".equals(lower(ability.getId()))) {
            return false;
        }

        List<String> tokens = parseEffectTokens(ability.getEffect());
        return tokens.stream().anyMatch(token -> switch (token) {
            case "attack_buff", "damage_buff", "stealth", "lifesteal", "shield", "evasion", "speed", "self_burn" -> true;
            default -> false;
        }) || ability.getShieldPercent() > 0;
    }

    private int resolveFollowUpUses(AbilityData ability, List<String> tokens) {
        return switch (lower(ability.getId())) {
            case "alloy_enhancement" -> 3;
            case "rubble_rouser" -> 1;
            case "umbral_veil" -> 1;
            case "lapidary", "imbue_fortitude", "absorb" -> 2;
            case "battle_cry", "waverider", "river_rapids", "frolick", "refraction", "imbue_swiftness" -> 3;
            default -> {
                if (tokens.contains("damage_buff") || tokens.contains("stealth")) {
                    yield 1;
                }
                if (tokens.contains("attack_buff") || tokens.contains("speed")) {
                    yield 3;
                }
                yield 2;
            }
        };
    }

    private double resolveFollowUpDamageMultiplierBonus(AbilityData ability) {
        return switch (lower(ability.getId())) {
            case "alloy_enhancement" -> 0.35;
            default -> 0.0;
        };
    }

    private double resolveFollowUpFlatDamageBonus(AbilityData ability, List<String> tokens) {
        double bonus = 4.0
                + (ability.getDamagePercent() * 0.20)
                + (tokens.contains("attack_buff") ? 4.0 : 0.0)
                + (tokens.contains("damage_buff") ? 7.0 : 0.0);

        return switch (lower(ability.getId())) {
            case "alloy_enhancement" -> bonus + 9.0;
            case "rubble_rouser" -> bonus + 6.0;
            case "imbue_power" -> bonus + 8.0;
            case "battle_cry", "overheat", "river_rapids", "refraction" -> bonus + 4.0;
            case "waverider", "frolick", "imbue_swiftness" -> bonus + 2.0;
            default -> bonus;
        };
    }

    private double resolveFollowUpShieldPercentOnHit(AbilityData ability) {
        double base = ability.getShieldPercent() > 0 ? Math.min(ability.getShieldPercent() * 0.35, 12.0) : 0.0;
        return switch (lower(ability.getId())) {
            case "metal_coat" -> Math.max(base, 8.0);
            case "lapidary" -> Math.max(base, 14.0);
            case "imbue_fortitude", "absorb" -> Math.max(base, 10.0);
            case "waverider" -> Math.max(base, 8.0);
            default -> base;
        };
    }

    private double resolveFollowUpHealRatioOnHit(AbilityData ability, List<String> tokens) {
        double base = tokens.contains("heal") ? 0.20 : 0.0;
        return switch (lower(ability.getId())) {
            case "imbue_fortitude", "absorb" -> Math.max(base, 0.38);
            case "frolick" -> Math.max(base, 0.30);
            default -> base;
        };
    }

    private double resolveFollowUpSplashRadius(AbilityData ability) {
        return switch (lower(ability.getId())) {
            case "rubble_rouser" -> 2.8;
            case "battle_cry" -> 2.5;
            case "overheat" -> 2.6;
            case "river_rapids" -> 2.8;
            case "refraction" -> 4.5;
            default -> 0.0;
        };
    }

    private double resolveFollowUpSplashDamageRatio(AbilityData ability) {
        return switch (lower(ability.getId())) {
            case "rubble_rouser" -> 0.45;
            case "battle_cry" -> 0.35;
            case "overheat" -> 0.45;
            case "river_rapids" -> 0.30;
            case "refraction" -> 0.55;
            default -> 0.0;
        };
    }

    private String resolveFollowUpSecondaryRiderToken(AbilityData ability) {
        return switch (lower(ability.getId())) {
            case "alloy_enhancement" -> "vulnerability";
            case "imbue_swiftness" -> "disoriented";
            case "refraction" -> "slow";
            case "frolick" -> "root";
            default -> null;
        };
    }

    private int resolveFollowUpUses(List<String> tokens) {
        if (tokens.contains("damage_buff") || tokens.contains("stealth")) {
            return 1;
        }
        if (tokens.contains("attack_buff") || tokens.contains("speed")) {
            return 3;
        }
        return 2;
    }

    private String resolveFollowUpRiderToken(AbilityData ability) {
        return switch (lower(ability.getId())) {
            case "rubble_rouser" -> "knockback";
            case "overheat" -> "burn";
            case "hidrosis", "smoke_form" -> "blind";
            case "battle_cry", "triceratops_form" -> "knockback";
            case "waverider" -> "slow";
            case "imbue_power", "refraction" -> "vulnerability";
            case "t_rex_form" -> "stun";
            default -> null;
        };
    }

    private void applyWeaponFollowUpSplash(Ref<EntityStore> playerRef,
                                           PlayerData player,
                                           Ref<EntityStore> primaryTargetRef,
                                           Store<EntityStore> store,
                                           ActiveWeaponFollowUp followUp,
                                           double resolvedDamage) {
        Vector3d center = getPosition(primaryTargetRef, store);
        if (center == null) {
            return;
        }

        for (Ref<EntityStore> splashTarget : collectNearbyNpcTargets(store, center, followUp.splashRadius, 4)) {
            if (splashTarget == null || !splashTarget.isValid() || splashTarget.equals(primaryTargetRef)) {
                continue;
            }

            String splashEntityId = resolveEntityId(splashTarget, store);
            double splashDamage = resolvedDamage * followUp.splashDamageRatio;
            if (splashEntityId != null) {
                splashDamage *= resolveIncomingDamageMultiplier(splashEntityId);
                splashDamage = mod.getStatusEffectManager().absorbDamage(splashEntityId, splashDamage);
            }

            if (splashDamage <= 0.0) {
                continue;
            }

            Damage splash = new Damage(new Damage.EntitySource(playerRef), DamageCause.PHYSICAL, (float) splashDamage);
            DamageSystems.executeDamage(splashTarget, store, splash);
            applyPostDamageClassPassives(player, playerRef, splashEntityId, splashDamage, false);
            player.getStatistics().setTotalDamageDealt(player.getStatistics().getTotalDamageDealt() + splashDamage);
            if (followUp.secondaryRiderToken != null) {
                applyTokenToTarget(followUp.secondaryRiderToken, splashTarget, store, playerRef, player.getPlayerId(), followUp.sourceAbility());
            }
            applyEffectById(splashTarget, store, resolveImpactEffectId(player.getPlayerClass(), currentStyleId(player), followUp.sourceAbility()));
        }
    }

    private void applyTransformationWeaponRider(ActiveTransformation form,
                                                Ref<EntityStore> targetRef,
                                                Store<EntityStore> store,
                                                Ref<EntityStore> playerRef,
                                                String playerId) {
        if (form == null || form.weaponRiderToken() == null) {
            return;
        }
        applyTokenToTarget(form.weaponRiderToken(), targetRef, store, playerRef, playerId, form.sourceAbility());
    }

    private void applyTransformationWeaponImpact(ActiveTransformation form,
                                                 PlayerData player,
                                                 Ref<EntityStore> targetRef,
                                                 Store<EntityStore> store,
                                                 Ref<EntityStore> playerRef,
                                                 double resolvedDamage) {
        if (form == null || player == null || targetRef == null || !targetRef.isValid()) {
            return;
        }

        switch (lower(form.abilityId())) {
            case "smoke_form" -> {
                StatusEffect evasion = createStatusEffect("evasion", form.sourceAbility(), player.getPlayerId(), form.abilityId());
                if (evasion != null) {
                    mod.getStatusEffectManager().applyEffect(player.getPlayerId(), evasion);
                }
                applyTokenToTarget("blind", targetRef, store, playerRef, player.getPlayerId(), form.sourceAbility());
            }
            case "pterodactyl_form" -> {
                applyTokenToTarget("slow", targetRef, store, playerRef, player.getPlayerId(), form.sourceAbility());
                applyTokenToTarget("vulnerability", targetRef, store, playerRef, player.getPlayerId(), form.sourceAbility());
                applyKnockback(targetRef, store, playerRef, form.sourceAbility());
            }
            case "triceratops_form" -> {
                KnockbackResult result = applyKnockbackResult(targetRef, store, playerRef, form.sourceAbility());
                if (result.collidedWithWall()) {
                    applyTokenToTarget("stun", targetRef, store, playerRef, player.getPlayerId(), form.sourceAbility());
                }
                if (resolvedDamage > 0.0) {
                    applyShield(player.getPlayerId(), playerRef, store, form.sourceAbility(), 6.0);
                }
            }
            case "t_rex_form" -> {
                applyTransformationCleave(form, player, targetRef, store, playerRef, resolvedDamage * 0.45, "vulnerability");
            }
            default -> {
            }
        }
    }

    private void applyTransformationCleave(ActiveTransformation form,
                                           PlayerData player,
                                           Ref<EntityStore> primaryTargetRef,
                                           Store<EntityStore> store,
                                           Ref<EntityStore> playerRef,
                                           double splashDamage,
                                           String token) {
        if (splashDamage <= 0.0) {
            return;
        }

        Vector3d center = getPosition(primaryTargetRef, store);
        if (center == null) {
            return;
        }

        for (Ref<EntityStore> splashTarget : collectNearbyNpcTargets(store, center, 3.4, 3)) {
            if (splashTarget == null || !splashTarget.isValid() || splashTarget.equals(primaryTargetRef)) {
                continue;
            }

            String targetEntityId = resolveEntityId(splashTarget, store);
            double resolvedSplash = splashDamage;
            if (targetEntityId != null) {
                resolvedSplash *= resolveIncomingDamageMultiplier(targetEntityId);
                resolvedSplash = mod.getStatusEffectManager().absorbDamage(targetEntityId, resolvedSplash);
            }
            if (resolvedSplash <= 0.0) {
                continue;
            }

            Damage cleave = new Damage(new Damage.EntitySource(playerRef), DamageCause.PHYSICAL, (float) resolvedSplash);
            DamageSystems.executeDamage(splashTarget, store, cleave);
            applyPostDamageClassPassives(player, playerRef, targetEntityId, resolvedSplash, false);
            player.getStatistics().setTotalDamageDealt(player.getStatistics().getTotalDamageDealt() + resolvedSplash);
            applyEffectById(splashTarget, store, resolveImpactEffectId(player.getPlayerClass(), currentStyleId(player), form.sourceAbility()));
            if (token != null && !token.isBlank()) {
                applyTokenToTarget(token, splashTarget, store, playerRef, player.getPlayerId(), form.sourceAbility());
            }
        }
    }

    private double applySpecialDamageModifiers(PlayerData player,
                                               AbilityData ability,
                                               Ref<EntityStore> targetRef,
                                               Store<EntityStore> store,
                                               String targetEntityId,
                                               double damage) {
        String abilityId = lower(ability.getId());
        if (targetEntityId == null || abilityId.isBlank()) {
            return damage;
        }

        if ("combust".equals(abilityId) && mod.getStatusEffectManager().hasEffect(targetEntityId, StatusEffect.Type.BURN)) {
            mod.getStatusEffectManager().removeEffect(targetEntityId, StatusEffect.Type.BURN);
            return damage * 1.75;
        }

        if (lower(ability.getEffect()).contains("lightning")) {
            boolean shocked = hasActiveOrRecentShock(targetEntityId);
            LOG.info("[MOTM] Lightning bonus check: ability=" + ability.getId()
                    + " target=" + targetEntityId
                    + " shocked=" + shocked);
            if (shocked) {
                LOG.info("[MOTM] Lightning bonus applied: ability=" + ability.getId()
                        + " target=" + targetEntityId
                        + " multiplier=1.25");
                return damage * 1.25;
            }
        }

        if ("consume".equals(abilityId)) {
            double healthRatio = resolveHealthRatio(targetRef, store);
            double modifier = 1.0;
            if (healthRatio > 0.0 && healthRatio <= 0.35) {
                modifier += healthRatio <= 0.18 ? 1.20 : 0.65;
            }
            if (mod.getStatusEffectManager().hasEffect(targetEntityId, StatusEffect.Type.VULNERABILITY)
                    || mod.getStatusEffectManager().hasEffect(targetEntityId, StatusEffect.Type.DOT)) {
                modifier += 0.25;
            }
            return damage * modifier;
        }

        return damage;
    }

    private boolean hasActiveOrRecentShock(String targetEntityId) {
        if (targetEntityId == null || targetEntityId.isBlank()) {
            return false;
        }

        if (mod.getStatusEffectManager().hasEffect(targetEntityId, StatusEffect.Type.SHOCKED)) {
            return true;
        }

        Long appliedAt = recentShockedTargets.get(targetEntityId);
        if (appliedAt == null) {
            return false;
        }

        long age = System.currentTimeMillis() - appliedAt;
        if (age <= SHOCKED_DAMAGE_WINDOW_MS) {
            return true;
        }

        recentShockedTargets.remove(targetEntityId, appliedAt);
        return false;
    }

    private double resolveTargetSequenceDamageMultiplier(AbilityData ability, String castType, int hitIndex) {
        if (ability == null || hitIndex <= 0) {
            return 1.0;
        }

        if ("chain".equals(castType)) {
            return switch (hitIndex) {
                case 1 -> 0.82;
                case 2 -> 0.67;
                default -> 0.55;
            };
        }

        if ("projectile_volley".equals(castType)) {
            return Math.max(0.7, 1.0 - (0.12 * hitIndex));
        }

        return 1.0;
    }

    private double resolveHealthRatio(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        EntityStatMap entityStatMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return 0.0;
        }
        EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null || health.getMax() <= 0) {
            return 0.0;
        }
        return health.get() / health.getMax();
    }

    private List<Ref<EntityStore>> resolveTargets(Ref<EntityStore> playerRef,
                                                  Store<EntityStore> store,
                                                  AbilityData ability,
                                                  CastContext context) {
        TargetingFrame frame = createTargetingFrame(playerRef, store, ability, context);
        if (frame == null) {
            return List.of();
        }

        LinkedHashSet<Ref<EntityStore>> targets = new LinkedHashSet<>();
        String castType = lower(ability.getCastType());
        if ("ground_burst".equals(castType) && "self_centered".equals(lower(ability.getTargetType()))) {
            return filterGroundTargetsIfNeeded(
                    collectTargetsAroundPoint(playerRef, store, frame.areaCenter(), frame.areaRadius(), 16.0),
                    store,
                    ability
            );
        }

        if (CONE_CAST_TYPES.contains(castType)) {
            for (TargetCandidate candidate : frame.candidates()) {
                if (candidate.distance() <= frame.range() && candidate.forwardDot() >= frame.coneThreshold()) {
                    targets.add(candidate.ref());
                }
            }
            return filterGroundTargetsIfNeeded(List.copyOf(targets), store, ability);
        }

        if (AREA_CAST_TYPES.contains(castType)) {
            Vector3d center = frame.areaCenter();
            for (TargetCandidate candidate : frame.candidates()) {
                if (distance(center, candidate.position()) <= frame.areaRadius()) {
                    targets.add(candidate.ref());
                }
            }
            return filterGroundTargetsIfNeeded(List.copyOf(targets), store, ability);
        }

        if (MULTI_TARGET_CAST_TYPES.contains(castType)) {
            List<TargetCandidate> sorted = new ArrayList<>(frame.candidates());
            sorted.sort((left, right) -> Double.compare(left.distance(), right.distance()));

            int maxTargets = "chain".equals(castType) ? DEFAULT_CHAIN_TARGETS : 3;
            if (frame.explicitTarget() != null) {
                targets.add(frame.explicitTarget());
            }

            for (TargetCandidate candidate : sorted) {
                if (targets.size() >= maxTargets) {
                    break;
                }
                if (candidate.forwardDot() <= 0.15 || candidate.distance() > frame.range()) {
                    continue;
                }
                if ("chain".equals(castType) && !targets.isEmpty()) {
                    Ref<EntityStore> anchor = targets.stream().reduce((first, second) -> second).orElse(null);
                    Vector3d anchorPosition = anchor != null ? getPosition(anchor, store) : null;
                    if (anchorPosition != null && distance(anchorPosition, candidate.position()) > DEFAULT_CHAIN_RADIUS) {
                        continue;
                    }
                }
                targets.add(candidate.ref());
            }
            return filterGroundTargetsIfNeeded(List.copyOf(targets), store, ability);
        }

        if (LINE_CAST_TYPES.contains(castType)) {
            if (frame.explicitTarget() != null) {
                targets.add(frame.explicitTarget());
            }

            for (TargetCandidate candidate : frame.candidates()) {
                if (candidate.distance() > frame.range()) {
                    continue;
                }
                if (candidate.axialDistance() < 0.0 || candidate.axialDistance() > frame.range()) {
                    continue;
                }
                if (candidate.lateralDistance() <= frame.lineHalfWidth()) {
                    targets.add(candidate.ref());
                }
            }

            if (targets.isEmpty()) {
                TargetCandidate nearestForward = frame.candidates().stream()
                        .filter(candidate -> candidate.forwardDot() > 0.2 && candidate.distance() <= frame.range())
                        .min((left, right) -> Double.compare(left.distance(), right.distance()))
                        .orElse(null);
                if (nearestForward != null) {
                    targets.add(nearestForward.ref());
                }
            }
            return filterGroundTargetsIfNeeded(List.copyOf(targets), store, ability);
        }

        if ("curse".equals(castType)) {
            TargetCandidate nearestLooseFacing = frame.candidates().stream()
                    .filter(candidate -> candidate.forwardDot() > -0.5 && candidate.distance() <= frame.range())
                    .min((left, right) -> Double.compare(left.distance(), right.distance()))
                    .orElse(null);
            return filterGroundTargetsIfNeeded(nearestLooseFacing != null ? List.of(nearestLooseFacing.ref()) : List.of(), store, ability);
        }

        if (frame.explicitTarget() != null) {
            return filterGroundTargetsIfNeeded(List.of(frame.explicitTarget()), store, ability);
        }

        TargetCandidate nearestForward = frame.candidates().stream()
                .filter(candidate -> candidate.forwardDot() > 0.2 && candidate.distance() <= frame.range())
                .min((left, right) -> Double.compare(left.distance(), right.distance()))
                .orElse(null);
        return filterGroundTargetsIfNeeded(nearestForward != null ? List.of(nearestForward.ref()) : List.of(), store, ability);
    }

    private List<Ref<EntityStore>> collectTargetsAroundPoint(Ref<EntityStore> playerRef,
                                                             Store<EntityStore> store,
                                                             Vector3d center,
                                                             double radius,
                                                             double verticalTolerance) {
        if (playerRef == null || store == null || center == null) {
            return List.of();
        }

        LinkedHashSet<Ref<EntityStore>> targets = new LinkedHashSet<>();
        int[] scanned = {0};
        double[] nearest = {Double.MAX_VALUE};
        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid() || ref.equals(playerRef) || isMotmVisualProxy(ref)) {
                    continue;
                }

                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning() || isMotmSummon(npc)) {
                    continue;
                }

                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }

                TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
                if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                    continue;
                }

                scanned[0]++;
                Vector3d position = transform.getTransform().getPosition();
                double horizontal = Math.hypot(position.x - center.x, position.z - center.z);
                double vertical = Math.abs(position.y - center.y);
                nearest[0] = Math.min(nearest[0], horizontal);
                if (horizontal <= radius && vertical <= verticalTolerance) {
                    targets.add(ref);
                }
            }
        });

        if (targets.isEmpty()) {
            String nearestLabel = nearest[0] == Double.MAX_VALUE ? "none" : AbilityPresentation.formatDecimal(nearest[0]);
            LOG.info("[MOTM] Landing AoE target scan found no targets: center=" + center
                    + " radius=" + AbilityPresentation.formatDecimal(radius)
                    + " verticalTolerance=" + AbilityPresentation.formatDecimal(verticalTolerance)
                    + " scanned=" + scanned[0]
                    + " nearestHorizontal=" + nearestLabel);
        }
        return List.copyOf(targets);
    }

    private List<Ref<EntityStore>> filterGroundTargetsIfNeeded(List<Ref<EntityStore>> targets,
                                                               Store<EntityStore> store,
                                                               AbilityData ability) {
        if (targets.isEmpty() || ability == null || !ability.isGroundTargetsOnly()) {
            return targets;
        }

        List<Ref<EntityStore>> groundedTargets = new ArrayList<>();
        for (Ref<EntityStore> target : targets) {
            if (isTargetGrounded(target, store)) {
                groundedTargets.add(target);
            }
        }
        return List.copyOf(groundedTargets);
    }

    private boolean isTargetGrounded(Ref<EntityStore> targetRef, Store<EntityStore> store) {
        if (targetRef == null || !targetRef.isValid() || store == null) {
            return false;
        }

        NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
        String roleName = npc != null ? npc.getRoleName() : "";
        if (roleName != null) {
            String normalizedRole = roleName.toLowerCase(Locale.ROOT);
            if (normalizedRole.contains("dummy") || normalizedRole.contains("stationary")) {
                return true;
            }
        }

        CollisionResultComponent collision = store.getComponent(targetRef, CollisionResultComponent.getComponentType());
        if (collision != null && collision.getCollisionResult() != null) {
            for (int i = 0; i < collision.getCollisionResult().getBlockCollisionCount(); i++) {
                BlockCollisionData blockCollision = collision.getCollisionResult().getBlockCollision(i);
                if (blockCollision != null
                        && blockCollision.collisionNormal != null
                        && blockCollision.collisionNormal.y > 0.45) {
                    return true;
                }
            }
        }

        if (!warnedGroundedFallback) {
            warnedGroundedFallback = true;
            LOG.warning("[MOTM] Ground-target filtering is using the fractional-Y fallback because no active ground collision was available.");
        }

        Vector3d position = getPosition(targetRef, store);
        if (position == null) {
            return false;
        }
        double fractionalY = Math.abs(position.y - Math.floor(position.y));
        return fractionalY <= 0.15;
    }

    private TargetingFrame createTargetingFrame(Ref<EntityStore> playerRef,
                                                Store<EntityStore> store,
                                                AbilityData ability,
                                                CastContext context) {
        Vector3d origin = getPosition(playerRef, store);
        Vector3d forward = getDirection(playerRef, store);
        if (origin == null || forward == null) {
            return null;
        }

        double range = resolveRange(ability);
        double radius = ability.getRadius() > 0 ? ability.getRadius() : DEFAULT_AREA_RADIUS;
        double halfWidth = ability.getWidth() > 0 ? ability.getWidth() / 2.0 : DEFAULT_LINE_HALF_WIDTH;
        String castType = lower(ability.getCastType());
        double coneThreshold = ability.getConeAngle() > 0
                ? Math.cos(Math.toRadians(ability.getConeAngle() / 2.0))
                : "gaze".equals(castType)
                ? Math.cos(Math.toRadians(12.0))
                : Math.cos(Math.toRadians(35.0));

        String ownerPlayerId = resolveEntityId(playerRef, store);
        Vector3d gemAnchor = resolveActiveLapidaryGemCenter(ownerPlayerId, ability, store);
        Vector3d areaCenter = gemAnchor != null
                ? gemAnchor
                : resolveAreaCenter(origin, forward, context, range, ability);
        Ref<EntityStore> explicitTarget = resolveExplicitTarget(store, context.explicitTargetRef(), range, origin);
        List<TargetCandidate> candidates = new ArrayList<>();

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid() || ref.equals(playerRef) || isMotmVisualProxy(ref)) {
                    continue;
                }

                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning() || isMotmSummon(npc)) {
                    continue;
                }

                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }

                TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
                if (transform == null || transform.getTransform() == null || transform.getTransform().getPosition() == null) {
                    continue;
                }

                Vector3d position = transform.getTransform().getPosition();
                Vector3d toTarget = subtract(position, origin);
                double distance = length(toTarget);
                if (distance <= 0.01) {
                    continue;
                }

                double axial = dot(toTarget, forward);
                Vector3d projected = new Vector3d(
                        origin.x + (forward.x * axial),
                        origin.y + (forward.y * axial),
                        origin.z + (forward.z * axial)
                );
                double lateral = distance(projected, position);
                double forwardDot = dot(normalize(toTarget), forward);
                candidates.add(new TargetCandidate(ref, position, distance, axial, lateral, forwardDot));
            }
        });

        return new TargetingFrame(explicitTarget, areaCenter, range, radius, halfWidth, coneThreshold, candidates);
    }

    private Ref<EntityStore> resolveExplicitTarget(Store<EntityStore> store,
                                                   Ref<EntityStore> explicitTargetRef,
                                                   double range,
                                                   Vector3d origin) {
        if (explicitTargetRef == null || !explicitTargetRef.isValid()) {
            return null;
        }

        NPCEntity npc = store.getComponent(explicitTargetRef, NPCEntity.getComponentType());
        if (npc == null || npc.isDespawning() || isMotmSummon(npc)) {
            return null;
        }

        if (store.getComponent(explicitTargetRef, DeathComponent.getComponentType()) != null) {
            return null;
        }

        Vector3d targetPosition = getPosition(explicitTargetRef, store);
        if (targetPosition == null || distance(origin, targetPosition) > Math.max(range, DEFAULT_AREA_RADIUS * 2.0)) {
            return null;
        }
        return explicitTargetRef;
    }

    private Vector3d resolveAreaCenter(Vector3d origin,
                                       Vector3d forward,
                                       CastContext context,
                                       double range,
                                       AbilityData ability) {
        if (isCasterCenteredAreaAbility(ability)) {
            return new Vector3d(origin);
        }
        if (context != null && context.targetBlock() != null) {
            Vector3i block = context.targetBlock();
            return new Vector3d(block.x + 0.5, block.y + 1.0, block.z + 0.5);
        }

        return new Vector3d(
                origin.x + (forward.x * Math.min(range, 5.0)),
                origin.y + (forward.y * Math.min(range, 5.0)),
                origin.z + (forward.z * Math.min(range, 5.0))
        );
    }

    private boolean isCasterCenteredAreaAbility(AbilityData ability) {
        return ability != null && "lava_pool".equals(lower(ability.getId()));
    }

    private Vector3d resolveActiveLapidaryGemCenter(PlayerData player, AbilityData ability, Store<EntityStore> store) {
        return player == null ? null : resolveActiveLapidaryGemCenter(player.getPlayerId(), ability, store);
    }

    private Vector3d resolveActiveLapidaryGemCenter(String ownerPlayerId, AbilityData ability, Store<EntityStore> store) {
        if (ownerPlayerId == null || ownerPlayerId.isBlank() || ability == null || store == null
                || !isGemAnchoredAbility(ability)) {
            return null;
        }
        for (ActiveLapidaryGem gem : activeLapidaryGems) {
            if (gem == null || !ownerPlayerId.equals(gem.ownerPlayerId)
                    || gem.ref == null || !gem.ref.isValid() || !belongsToCurrentStore(gem.ref, store)) {
                continue;
            }
            return new Vector3d(gem.center);
        }
        LOG.info("[MOTM][terra-audit] event=gem.anchor.missing playerId=" + safe(ownerPlayerId)
                + " abilityId=" + safe(ability.getId()));
        return null;
    }

    private boolean isGemAnchoredAbility(AbilityData ability) {
        String abilityId = lower(ability != null ? ability.getId() : null);
        return "fracture".equals(abilityId) || "refraction".equals(abilityId);
    }

    private Vector3d resolveProjectileOrigin(Ref<EntityStore> playerRef,
                                             Store<EntityStore> store,
                                             AbilityData ability) {
        Vector3d origin = getPosition(playerRef, store);
        if (origin == null) {
            return null;
        }
        boolean raisedProjectile = "magma_sling".equals(lower(ability != null ? ability.getId() : null))
                || isGroundMarkerProjectile(ability);
        if (!raisedProjectile) {
            return origin;
        }

        Vector3d forward = getDirection(playerRef, store);
        Vector3d raised = new Vector3d(origin).add(0.0, 1.15, 0.0);
        if (forward != null) {
            Vector3d horizontalForward = normalizeHorizontal(forward);
            raised.fma(0.9, horizontalForward);
        }
        return raised;
    }

    private Vector3d resolveSummonPosition(Ref<EntityStore> playerRef,
                                           Store<EntityStore> store,
                                           AbilityData ability,
                                           CastContext context) {
        Vector3d origin = getPosition(playerRef, store);
        Vector3d forward = getDirection(playerRef, store);
        if (origin == null || forward == null) {
            return null;
        }

        if (context.targetBlock() != null) {
            Vector3i block = context.targetBlock();
            return new Vector3d(block.x + 0.5, block.y + 1.0, block.z + 0.5);
        }

        double distance = ability.getRange() > 0 ? Math.min(ability.getRange(), 4.0) : 2.5;
        return new Vector3d(origin.x + (forward.x * distance), origin.y, origin.z + (forward.z * distance));
    }

    private Vector3d resolveLaunchDirection(Ref<EntityStore> playerRef,
                                            Store<EntityStore> store,
                                            AbilityData ability,
                                            CastContext context) {
        Vector3d origin = getPosition(playerRef, store);
        if (origin == null) {
            return null;
        }

        if (!isGroundMarkerProjectile(ability)
                && context.explicitTargetRef() != null
                && context.explicitTargetRef().isValid()) {
            Vector3d targetPosition = getPosition(context.explicitTargetRef(), store);
            if (targetPosition != null) {
                Vector3d aimPoint = isMagmaSlingAbility(ability)
                        ? new Vector3d(targetPosition).add(0.0, 1.0, 0.0)
                        : targetPosition;
                return normalize(subtract(aimPoint, origin));
            }
        }

        if (isMagmaSlingAbility(ability)) {
            Vector3d direction = getDirection(playerRef, store);
            if (direction == null || !direction.isFinite() || direction.length() < 0.001) {
                return null;
            }
            return normalize(direction);
        }

        if (context.targetBlock() != null) {
            Vector3i block = context.targetBlock();
            Vector3d targetPosition = new Vector3d(block.x + 0.5, block.y + 1.0, block.z + 0.5);
            Vector3d launchOrigin = resolveProjectileOrigin(playerRef, store, ability);
            return normalize(subtract(targetPosition, launchOrigin != null ? launchOrigin : origin));
        }

        return getDirection(playerRef, store);
    }

    private Vector3d getPosition(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null) {
            return null;
        }
        return transform.getTransform().getPosition();
    }

    private Vector3d getDirection(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }

        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        if (transform == null || transform.getTransform() == null || transform.getTransform().getDirection() == null) {
            return null;
        }

        Vector3d direction = new Vector3d(transform.getTransform().getDirection());
        if (!direction.isFinite()) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        if (direction.length() < 0.001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        return normalize(direction);
    }

    private String resolveSummonModelId(String classId, String styleId, AbilityData ability) {
        String summonName = lower(ability.getSummonName());
        if (!summonName.isBlank()) {
            return switch (summonName) {
                case "treant_sapling" -> "Spirit_Root";
                case "snow_imp" -> "Spirit_Frost";
                case "frosty_golem" -> "Golem_Crystal_Frost";
                case "swamp_monster" -> "Frog_Green";
                case "skeleton_minion", "shadow_clone" -> "Shadow_Knight";
                case "void_spawn" -> "Spawn_Void";
                case "scarak_egg" -> "Scarak_Fighter";
                case "locust_queen" -> "Scarak_Broodmother";
                default -> null;
            };
        }

        return HytaleAssetResolver.resolveModelId(classId, styleId, ability);
    }

    private String resolveTransformationEffectId(String abilityId) {
        return switch (lower(abilityId)) {
            case "smoke_form" -> "MOTM_Aero_Smoke_Form";
            case "pterodactyl_form" -> "MOTM_Corruptus_Pterodactyl_Form";
            case "triceratops_form" -> "MOTM_Corruptus_Triceratops_Form";
            case "t_rex_form" -> "MOTM_Corruptus_TRex_Form";
            default -> null;
        };
    }

    private String resolveImpactEffectId(String classId,
                                         String styleId,
                                         AbilityData ability) {
        String themed = resolveThemedEffectId(classId, styleId, ability, RuntimeEffectKind.IMPACT);
        if (themed != null) {
            return themed;
        }
        if (ability != null) {
            String impact = asRuntimeEffectId(HytaleAssetResolver.resolve(classId, styleId, ability).getImpactEffectAsset());
            if (impact != null) {
                return impact;
            }
        }
        return switch (lower(classId)) {
            case "terra" -> "MOTM_Terra_Impact";
            case "hydro" -> "MOTM_Hydro_Impact";
            case "aero" -> "MOTM_Aero_Impact";
            case "corruptus" -> "MOTM_Corruptus_Impact";
            default -> null;
        };
    }

    private String resolveProjectileVisualEffectId(String classId,
                                                   String styleId,
                                                   AbilityData ability) {
        String themed = resolveThemedEffectId(classId, styleId, ability, RuntimeEffectKind.MOVE);
        if (themed != null) {
            return themed;
        }
        if (ability != null) {
            var assets = HytaleAssetResolver.resolve(classId, styleId, ability);
            String travel = asRuntimeEffectId(assets.getTravelEffectAsset());
            if (travel != null) {
                return travel;
            }
            String impact = asRuntimeEffectId(assets.getImpactEffectAsset());
            if (impact != null) {
                return impact;
            }
            String cast = asRuntimeEffectId(assets.getCastEffectAsset());
            if (cast != null) {
                return cast;
            }
        }

        return switch (lower(classId)) {
            case "terra" -> "MOTM_Terra_Move";
            case "hydro" -> "MOTM_Hydro_Move";
            case "aero" -> "MOTM_Aero_Move";
            case "corruptus" -> "MOTM_Corruptus_Move";
            default -> null;
        };
    }

    private String resolveFieldVisualEffectId(String classId,
                                              String styleId,
                                              AbilityData ability) {
        if (isSinkhole(ability)) {
            return null;
        }
        String themed = resolveThemedEffectId(classId, styleId, ability, RuntimeEffectKind.FIELD);
        if (themed != null) {
            return themed;
        }
        if (ability == null) {
            return null;
        }

        var assets = HytaleAssetResolver.resolve(classId, styleId, ability);
        String loop = asRuntimeEffectId(assets.getLoopEffectAsset());
        if (loop != null) {
            return loop;
        }
        String impact = asRuntimeEffectId(assets.getImpactEffectAsset());
        if (impact != null) {
            return impact;
        }
        String travel = asRuntimeEffectId(assets.getTravelEffectAsset());
        if (travel != null) {
            return travel;
        }
        return null;
    }

    private String resolveEffectId(String classId,
                                   String styleId,
                                   AbilityData ability) {
        String themed = resolveThemedEffectId(classId, styleId, ability, RuntimeEffectKind.CAST);
        if (themed != null) {
            return themed;
        }
        String prefix = switch (lower(classId)) {
            case "terra" -> "MOTM_Terra";
            case "hydro" -> "MOTM_Hydro";
            case "aero" -> "MOTM_Aero";
            case "corruptus" -> "MOTM_Corruptus";
            default -> null;
        };
        if (prefix == null) {
            return null;
        }

        return MOVEMENT_CAST_TYPES.contains(lower(ability.getCastType()))
                ? prefix + "_Move"
                : prefix + "_Cast";
    }

    private boolean suppressGenericCasterVisual(AbilityData ability) {
        return switch (lower(ability != null ? ability.getId() : null)) {
            case "iron_wall", "metal_coat", "alloy_enhancement", "obsidian_skin", "magma_sling",
                    "rubble_rouser" -> true;
            default -> false;
        };
    }

    private String currentStyleId(PlayerData player) {
        if (player == null || player.getSelectedStyles() == null || player.getSelectedStyles().isEmpty()) {
            return null;
        }
        return player.getSelectedStyles().get(0);
    }

    private String asRuntimeEffectId(String candidate) {
        if (candidate == null || candidate.isBlank()) {
            return null;
        }
        if (candidate.startsWith("MOTM_") || candidate.contains("/Entity/Effects/")) {
            return candidate;
        }
        return null;
    }

    private String resolveThemedEffectId(String classId,
                                         String styleId,
                                         AbilityData ability,
                                         RuntimeEffectKind kind) {
        String lowerClassId = lower(classId);
        String lowerStyleId = lower(styleId);
        if (ability == null) {
            return null;
        }

        if ("aero".equals(lowerClassId) && "scream".equals(lowerStyleId)) {
            return switch (kind) {
                case CAST -> "MOTM_Aero_Scream_Cast";
                case MOVE -> "MOTM_Aero_Scream_Move";
                case IMPACT -> "MOTM_Aero_Scream_Impact";
                case FIELD -> "MOTM_Aero_Scream_Field";
            };
        }

        if ("hydro".equals(lowerClassId)
                && ("surf".equals(lowerStyleId)
                || "rain".equals(lowerStyleId)
                || "saltwater".equals(lowerStyleId)
                || "freshwater".equals(lowerStyleId)
                || "bilgewater".equals(lowerStyleId)
                || "boiling".equals(lowerStyleId))) {
            return switch (kind) {
                case CAST -> "MOTM_Hydro_Wave_Cast";
                case MOVE -> "MOTM_Hydro_Wave_Move";
                case IMPACT -> "MOTM_Hydro_Wave_Impact";
                case FIELD -> "MOTM_Hydro_Wave_Field";
            };
        }

        if ("terra".equals(lowerClassId) && "gem".equals(lowerStyleId)) {
            return switch (kind) {
                case CAST -> "MOTM_Terra_Gem_Cast";
                case MOVE, IMPACT -> "MOTM_Terra_Gem_Impact";
                case FIELD -> "MOTM_Terra_Gem_Field";
            };
        }

        if ("corruptus".equals(lowerClassId)
                && ("void".equals(lowerStyleId) || "shadow".equals(lowerStyleId))) {
            return switch (kind) {
                case CAST -> "MOTM_Corruptus_Void_Cast";
                case MOVE -> "MOTM_Corruptus_Void_Move";
                case IMPACT -> "MOTM_Corruptus_Void_Impact";
                case FIELD -> "MOTM_Corruptus_Void_Field";
            };
        }

        return null;
    }

    private enum RuntimeEffectKind {
        CAST,
        MOVE,
        IMPACT,
        FIELD
    }

    private double resolveHorizontalMovement(AbilityData ability, String castType) {
        double configured = ability.getDashDistance() > 0 ? ability.getDashDistance() : ability.getRange();
        if ("air_stall".equals(castType)) {
            return 0.0;
        }

        double fallback = switch (castType) {
            case "teleport" -> 8.0;
            case "leap", "dive_strike" -> 6.0;
            case "dash_strike" -> 5.5;
            default -> 4.5;
        };

        double resolved = configured > 0 ? configured : fallback;
        return clamp(resolved, 0.0, MAX_HORIZONTAL_MOVEMENT);
    }

    private double resolveVerticalMovement(AbilityData ability, String castType) {
        double configured = ability.getLaunchHeight();
        double fallback = switch (castType) {
            case "air_stall" -> 2.5;
            case "leap", "dive_strike" -> 1.75;
            default -> 0.0;
        };

        double resolved = configured > 0 ? configured : fallback;
        return clamp(resolved, 0.0, MAX_VERTICAL_MOVEMENT);
    }

    private double resolveRange(AbilityData ability) {
        if (ability.getRange() > 0) return ability.getRange();
        if (ability.getMaxRange() > 0) return ability.getMaxRange();
        if (ability.getDashDistance() > 0) return ability.getDashDistance();
        return 8.0;
    }

    private int resolveProjectileCount(String castType, AbilityData ability) {
        String abilityId = lower(ability.getId());
        String travelType = lower(ability.getTravelType());
        return switch (castType) {
            case "projectile_volley" -> switch (abilityId) {
                case "bullet_storm" -> 6;
                case "frozen_needles" -> 5;
                case "cacti_cluster" -> 1;
                case "debris" -> 4;
                default -> travelType.contains("storm") ? 5 : DEFAULT_PROJECTILE_CLUSTER_COUNT + 1;
            };
            case "projectile_burst" -> switch (abilityId) {
                case "splash", "scald", "hellfire" -> 4;
                default -> DEFAULT_PROJECTILE_CLUSTER_COUNT;
            };
            default -> 1;
        };
    }

    private double resolveProjectileSpeedPerTick(AbilityData ability) {
        if ("cacti_cluster".equals(lower(ability != null ? ability.getId() : null))) {
            return 9.0 / StyleManager.TICKS_PER_SECOND;
        }
        double speedPerSecond = ability.getProjectileSpeed() > 0
                ? ability.getProjectileSpeed()
                : DEFAULT_PROJECTILE_SPEED;
        return clamp(speedPerSecond, 6.0, MAX_PROJECTILE_SPEED) / StyleManager.TICKS_PER_SECOND;
    }

    private double resolveProjectileImpactRadius(AbilityData ability, String castType) {
        if (isMagmaSlingAbility(ability)) {
            return 2.0;
        }
        if (ability.getRadius() > 0) {
            return ability.getRadius();
        }

        return switch (castType) {
            case "projectile_burst", "wave_line" -> 2.25;
            case "projectile_volley" -> 0.0;
            default -> DEFAULT_IMPACT_RADIUS;
        };
    }

    private double resolveProjectileCollisionRadius(AbilityData ability, String castType) {
        if (isMagmaSlingAbility(ability)) {
            return 1.8;
        }
        if ("cacti_cluster".equals(lower(ability != null ? ability.getId() : null))) {
            return 1.65;
        }
        if (ability.getWidth() > 0) {
            return Math.max(DEFAULT_PROJECTILE_COLLISION_RADIUS, ability.getWidth() / 3.5);
        }

        return switch (castType) {
            case "wave_line" -> 1.4;
            case "projectile_burst", "projectile_volley" -> 1.0;
            default -> DEFAULT_PROJECTILE_COLLISION_RADIUS;
        };
    }

    private double resolveProjectileSpreadDegrees(String castType, AbilityData ability, int projectileCount) {
        if (projectileCount <= 1) {
            return 0.0;
        }

        String abilityId = lower(ability.getId());
        return switch (castType) {
            case "projectile_burst" -> switch (abilityId) {
                case "splash" -> 13.0;
                case "scald" -> 11.5;
                case "hellfire" -> 12.5;
                default -> BURST_SPREAD_DEGREES;
            };
            case "projectile_volley" -> switch (abilityId) {
                case "bullet_storm" -> 4.5;
                case "frozen_needles" -> 5.0;
                case "cacti_cluster" -> 6.5;
                case "debris" -> 7.5;
                default -> VOLLEY_SPREAD_DEGREES;
            };
            default -> 0.0;
        };
    }

    private long resolveProjectileLaunchDelayMillis(String castType,
                                                    AbilityData ability,
                                                    int index) {
        String abilityId = lower(ability.getId());
        return switch (castType) {
            case "projectile_volley" -> switch (abilityId) {
                case "bullet_storm" -> index * 65L;
                case "frozen_needles" -> index * 55L;
                case "debris" -> index * 90L;
                default -> index * DEFAULT_VOLLEY_STAGGER_MS;
            };
            case "projectile_burst" -> switch (abilityId) {
                case "hellfire" -> index * 35L;
                case "splash" -> index * 28L;
                default -> index * DEFAULT_BURST_STAGGER_MS;
            };
            default -> 0L;
        };
    }

    private long resolveProjectileLifetimeMillis(AbilityData ability,
                                                 double speedPerTick,
                                                 double maxDistance) {
        double travelSeconds = Math.max(
                DEFAULT_PROJECTILE_TTL_SECONDS,
                maxDistance / Math.max(0.1, speedPerTick * StyleManager.TICKS_PER_SECOND)
        );
        if (ability.getDurationSeconds() > 0) {
            travelSeconds = Math.max(travelSeconds, Math.min(ability.getDurationSeconds(), 8.0));
        }
        return (long) (travelSeconds * 1000);
    }

    private double resolveFieldPulseDamage(PlayerData player, AbilityData ability) {
        double baseDamage = resolveDamageAmount(player, ability);
        if (baseDamage <= 0.0) {
            return 0.0;
        }

        String terrainEffect = lower(ability.getTerrainEffect());
        return switch (lower(ability.getCastType())) {
            case "support_zone" -> 0.0;
            case "barrier" -> baseDamage * 0.18;
            default -> {
                double ratio = DEFAULT_FIELD_DAMAGE_RATIO;
                if (terrainEffect.contains("sinkhole")) {
                    ratio = 0.34;
                } else if (terrainEffect.contains("falling_rocks")) {
                    ratio = 0.36;
                } else if (terrainEffect.contains("acid")) {
                    ratio = 0.30;
                } else if (terrainEffect.contains("smog")) {
                    ratio = 0.22;
                }
                yield baseDamage * ratio;
            }
        };
    }

    private boolean isPersistentFieldAbility(AbilityData ability) {
        String castType = lower(ability.getCastType());
        if (PERSISTENT_FIELD_CAST_TYPES.contains(castType)) {
            return true;
        }

        if (!"ground_target".equals(castType)) {
            return false;
        }

        String terrainEffect = lower(ability.getTerrainEffect());
        String abilityId = lower(ability.getId());
        return ability.getDurationSeconds() > 0.0
                && (ability.getDelaySeconds() > 0.0
                || terrainEffect.contains("sinkhole")
                || terrainEffect.contains("hazard")
                || "sinkhole".equals(abilityId));
    }

    private double resolvePullStep(AbilityData ability, double scale, double minimumStep) {
        double configured = ability.getPullForce() > 0 ? ability.getPullForce() : minimumStep;
        return clamp(Math.max(minimumStep, configured * scale), minimumStep, MAX_PULL_STEP_DISTANCE);
    }

    private double resolveFieldPullLift(ActiveField field) {
        String travelType = lower(field.ability().getTravelType());
        String terrainEffect = lower(field.ability().getTerrainEffect());
        String abilityId = lower(field.ability().getId());
        if (travelType.contains("funnel")
                || travelType.contains("twister")
                || terrainEffect.contains("funnel")
                || terrainEffect.contains("tempest")
                || abilityId.contains("tempest")) {
            return 0.35;
        }
        return 0.0;
    }

    private double resolveDamageAmount(PlayerData player, AbilityData ability) {
        if (ability.getDamagePercent() <= 0) {
            return 0.0;
        }

        double damage = ability.getDamagePercent() * (0.9 + (player.getLevel() * 0.06));
        damage *= mod.getLevelingManager().getPlayerAbilityDamageMultiplier(player);
        return switch (lower(ability.getCastType())) {
            case "execute" -> damage * 1.3;
            case "projectile_volley" -> damage * 0.75;
            case "chain" -> damage * 0.85;
            default -> damage;
        };
    }

    private double resolveOutgoingDamageMultiplier(PlayerData player) {
        double modifier = 1.0;
        modifier += mod.getStatusEffectManager().getDamageIncrease(player.getPlayerId());
        modifier += mod.getStatusEffectManager().consumeOneShot(player.getPlayerId(), StatusEffect.Type.DAMAGE_BUFF);
        modifier += mod.getStatusEffectManager().consumeOneShot(player.getPlayerId(), StatusEffect.Type.STEALTH);
        modifier += player.getSynergyDamageIncrease().getOrDefault("all", 0.0);
        modifier += mod.getClassPassiveManager().getAbilityDamageModifier(player);
        ActiveTransformation activeForm = activeTransformationsByPlayer.get(player.getPlayerId());
        if (activeForm != null) {
            modifier += activeForm.damageBonus();
        }
        if (mod.getRuntimePerkManager() != null) {
            modifier *= mod.getRuntimePerkManager().getOutgoingDamageMultiplier(player);
        }
        if (mod.getStatusEffectManager().hasEffect(player.getPlayerId(), StatusEffect.Type.BLIND)) {
            modifier *= (1.0 - BLIND_DAMAGE_PENALTY);
        }
        if (mod.getStatusEffectManager().hasEffect(player.getPlayerId(), StatusEffect.Type.DISORIENTED)) {
            modifier *= (1.0 - DISORIENTED_DAMAGE_PENALTY);
        }
        return Math.max(0.1, modifier);
    }

    private boolean isGroundRestrictedAbility(AbilityData ability) {
        if (ability == null) {
            return false;
        }

        String castType = lower(ability.getCastType());
        if (MOVEMENT_CAST_TYPES.contains(castType)) {
            return true;
        }

        if (!"transformation".equals(castType)) {
            return false;
        }

        String travelType = lower(ability.getTravelType());
        String abilityId = lower(ability.getId());
        return travelType.contains("flight")
                || "smoke_form".equals(abilityId)
                || "pterodactyl_form".equals(abilityId);
    }

    private double resolveIncomingDamageMultiplier(String entityId) {
        double modifier = mod.getStatusEffectManager().getVulnerabilityMultiplier(entityId);
        modifier *= Math.max(0.1, 1.0 - mod.getStatusEffectManager().getDamageReduction(entityId));
        return Math.max(0.1, modifier);
    }

    private void applyPostDamageClassPassives(PlayerData player,
                                              Ref<EntityStore> sourceRef,
                                              String targetEntityId,
                                              double damageAmount,
                                              boolean abilityBased) {
        if (player == null || damageAmount <= 0.0) {
            return;
        }

        mod.getClassPassiveManager().onDamageDealt(
                player,
                sourceRef,
                targetEntityId,
                damageAmount,
                abilityBased
        );
    }

    private double healEntity(Ref<EntityStore> entityRef, Store<EntityStore> store, double healPercent) {
        if (healPercent <= 0) {
            return 0.0;
        }

        EntityStatMap entityStatMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return 0.0;
        }

        EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null || health.getMax() <= 0) {
            return 0.0;
        }

        float current = health.get();
        float max = health.getMax();
        float healAmount = (float) (max * (healPercent / 100.0));
        float applied = Math.max(0f, Math.min(healAmount, max - current));
        if (applied <= 0f) {
            return 0.0;
        }

        entityStatMap.addStatValue(DefaultEntityStatTypes.getHealth(), applied);
        return applied;
    }

    private double healEntityFlat(Ref<EntityStore> entityRef, Store<EntityStore> store, double healAmount) {
        if (healAmount <= 0) {
            return 0.0;
        }

        EntityStatMap entityStatMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return 0.0;
        }

        EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null || health.getMax() <= 0) {
            return 0.0;
        }

        float current = health.get();
        float max = health.getMax();
        float applied = Math.max(0f, Math.min((float) healAmount, max - current));
        if (applied <= 0f) {
            return 0.0;
        }

        entityStatMap.addStatValue(DefaultEntityStatTypes.getHealth(), applied);
        return applied;
    }

    private double applyLifesteal(Player runtimePlayer, PlayerData player, double damageDealt) {
        if (damageDealt <= 0 || runtimePlayer == null || player == null) {
            return 0.0;
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        return applyLifesteal(playerRef, player.getPlayerId(), damageDealt);
    }

    private double applyLifesteal(Ref<EntityStore> playerRef, String playerId, double damageDealt) {
        if (damageDealt <= 0 || playerRef == null || !playerRef.isValid() || playerId == null) {
            return 0.0;
        }

        PlayerData player = mod.getPlayerDataManager().getOnlinePlayer(playerId);
        if (player != null && mod.getRuntimePerkManager() != null) {
            mod.getRuntimePerkManager().afterSuccessfulHit(
                    player,
                    playerRef,
                    playerRef.getStore(),
                    null,
                    damageDealt
            );
        }

        double lifestealRatio = mod.getStatusEffectManager().getEffects(playerId).stream()
                .filter(effect -> effect.getType() == StatusEffect.Type.LIFESTEAL && !effect.isExpired())
                .mapToDouble(StatusEffect::getValue)
                .sum();
        if (lifestealRatio <= 0) {
            return 0.0;
        }

        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return 0.0;
        }

        return healEntityFlat(playerRef, playerRef.getStore(), damageDealt * lifestealRatio);
    }

    private double applyShield(String entityId,
                               Ref<EntityStore> entityRef,
                               Store<EntityStore> store,
                               AbilityData ability,
                               double shieldPercent) {
        double maxHealth = resolveMaxHealth(entityRef, store);
        if (maxHealth <= 0 || shieldPercent <= 0) {
            return 0.0;
        }

        double shieldAmount = maxHealth * (shieldPercent / 100.0);
        if (shieldAmount <= 0) {
            return 0.0;
        }

        StatusEffect shield = new StatusEffect(
                StatusEffect.Type.SHIELD,
                resolveDurationTicks(ability, "shield"),
                shieldAmount,
                entityId,
                ability.getId()
        );
        mod.getStatusEffectManager().applyEffect(entityId, shield);
        return shieldAmount;
    }

    private double resolveMaxHealth(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        EntityStatMap entityStatMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return 0.0;
        }

        EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null || health.getMax() <= 0) {
            return 0.0;
        }
        return health.getMax();
    }

    private void restoreActiveTemporarySelections(World world, String reason) {
        if (world == null || reason == null || reason.isBlank()) {
            return;
        }
        activeTerrainSelections.removeIf(selection -> {
            if (selection == null || !reason.equals(selection.reason())) {
                return false;
            }
            if (selection.world() != world && !selection.world().equals(world)) {
                return false;
            }
            try {
                selection.originalSelection().place(null, selection.world(), new Vector3i(0, 0, 0), BlockMask.EMPTY);
                LOG.info("[MOTM] Temporary Terra terrain restored before replacement: reason=" + selection.reason()
                        + " anchor=" + selection.anchor());
            } catch (Throwable e) {
                LOG.warning("[MOTM] Temporary Terra terrain replacement restore failed: reason=" + selection.reason()
                        + " anchor=" + selection.anchor()
                        + " error=" + e.getMessage());
            } finally {
                activeTemporaryTerrainBlockKeys.removeAll(selection.protectedBlockKeys());
            }
            return true;
        });
    }

    private double resolveCurrentHealth(Ref<EntityStore> entityRef, Store<EntityStore> store) {
        EntityStatMap entityStatMap = store.getComponent(entityRef, EntityStatMap.getComponentType());
        if (entityStatMap == null) {
            return 0.0;
        }

        EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null) {
            return 0.0;
        }
        return health.get();
    }

    private double resolvePlayerMaxHealth(String playerId) {
        Player runtimePlayer = mod.getRuntimePlayer(playerId);
        if (runtimePlayer == null || runtimePlayer.getReference() == null || !runtimePlayer.getReference().isValid()
                || runtimePlayer.getReference().getStore() == null) {
            return 100.0;
        }
        double maxHealth = resolveMaxHealth(runtimePlayer.getReference(), runtimePlayer.getReference().getStore());
        return maxHealth > 0.0 ? maxHealth : 100.0;
    }

    private void reportAbilityKillIfDead(String ownerPlayerId,
                                         PlayerData player,
                                         Ref<EntityStore> targetRef,
                                         Store<EntityStore> store,
                                         String targetEntityId) {
        if (ownerPlayerId == null || targetRef == null || !targetRef.isValid() || store == null) {
            return;
        }

        boolean dead = store.getComponent(targetRef, DeathComponent.getComponentType()) != null;
        EntityStatMap entityStatMap = store.getComponent(targetRef, EntityStatMap.getComponentType());
        if (!dead && entityStatMap != null) {
            EntityStatValue health = entityStatMap.get(DefaultEntityStatTypes.getHealth());
            dead = health != null && health.get() <= 0.0f;
        }
        if (!dead) {
            return;
        }

        String killKey = targetEntityId != null ? targetEntityId : String.valueOf(targetRef.getIndex());
        if (!reportedAbilityKillEntityIds.add(killKey)) {
            return;
        }

        NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
        String mobType = npc != null && npc.getRoleName() != null && !npc.getRoleName().isBlank()
                ? npc.getRoleName()
                : "ability_target";
        int mobLevel = player != null ? Math.max(1, player.getLevel()) : 1;
        LOG.info("[MOTM] ability kill detected: player=" + ownerPlayerId
                + " target=" + killKey
                + " mobType=" + mobType);
        mod.onMobKilled(ownerPlayerId, killKey, mobType, mobLevel, false);
    }

    private int clearNegativeEffects(String entityId) {
        int removed = 0;
        for (StatusEffect.Type type : List.of(
                StatusEffect.Type.BURN,
                StatusEffect.Type.DOT,
                StatusEffect.Type.STUN,
                StatusEffect.Type.SLOW,
                StatusEffect.Type.SLOW_STACK,
                StatusEffect.Type.VULNERABILITY,
                StatusEffect.Type.FREEZE,
                StatusEffect.Type.ROOT,
                StatusEffect.Type.BLIND,
                StatusEffect.Type.DISORIENTED,
                StatusEffect.Type.GROUNDED,
                StatusEffect.Type.SHOCKED
        )) {
            if (mod.getStatusEffectManager().hasEffect(entityId, type)) {
                mod.getStatusEffectManager().removeEffect(entityId, type);
                removed++;
            }
        }
        return removed;
    }

    private StatusEffect createStatusEffect(String token,
                                            AbilityData ability,
                                            String sourcePlayerId,
                                            String sourceAbilityId) {
        String normalized = lower(token);
        int durationTicks = resolveDurationTicks(ability, normalized);

        return switch (normalized) {
            case "burn", "self_burn" -> new StatusEffect(
                    StatusEffect.Type.BURN, durationTicks, 0.03, sourcePlayerId, sourceAbilityId);
            case "dot" -> new StatusEffect(
                    StatusEffect.Type.DOT, durationTicks, 0.05, sourcePlayerId, sourceAbilityId);
            case "stun", "stun_if_wall" -> new StatusEffect(
                    StatusEffect.Type.STUN, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "slow" -> new StatusEffect(
                    StatusEffect.Type.SLOW, durationTicks, 0.20, sourcePlayerId, sourceAbilityId);
            case "slow_stack" -> new StatusEffect(
                    StatusEffect.Type.SLOW_STACK, durationTicks, 0.10, sourcePlayerId, sourceAbilityId);
            case "vulnerability", "curse" -> new StatusEffect(
                    StatusEffect.Type.VULNERABILITY, durationTicks, 0.25, sourcePlayerId, sourceAbilityId);
            case "freeze" -> new StatusEffect(
                    StatusEffect.Type.FREEZE, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "root" -> new StatusEffect(
                    StatusEffect.Type.ROOT, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "blind", "deafen" -> new StatusEffect(
                    StatusEffect.Type.BLIND, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "disoriented", "attack_slow" -> new StatusEffect(
                    StatusEffect.Type.DISORIENTED, durationTicks, 0.15, sourcePlayerId, sourceAbilityId);
            case "grounded" -> new StatusEffect(
                    StatusEffect.Type.GROUNDED, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "flying" -> new StatusEffect(
                    StatusEffect.Type.FLYING, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "shocked", "lightning" -> new StatusEffect(
                    StatusEffect.Type.SHOCKED, durationTicks, 0.0, sourcePlayerId, sourceAbilityId);
            case "evasion", "evasion_zone" -> new StatusEffect(
                    StatusEffect.Type.EVASION, durationTicks, 0.30, sourcePlayerId, sourceAbilityId);
            case "evasion_buff" -> new StatusEffect(
                    StatusEffect.Type.EVASION, durationTicks, 0.40, sourcePlayerId, sourceAbilityId);
            case "speed" -> new StatusEffect(
                    StatusEffect.Type.SPEED_BUFF, durationTicks, 0.25, sourcePlayerId, sourceAbilityId);
            case "defense_buff" -> new StatusEffect(
                    StatusEffect.Type.DEFENSE_BUFF, durationTicks, 0.20, sourcePlayerId, sourceAbilityId);
            case "attack_buff" -> new StatusEffect(
                    StatusEffect.Type.ATTACK_BUFF, durationTicks, 0.20, sourcePlayerId, sourceAbilityId);
            case "damage_buff" -> new StatusEffect(
                    StatusEffect.Type.DAMAGE_BUFF, durationTicks, 0.35, sourcePlayerId, sourceAbilityId);
            case "stealth" -> new StatusEffect(
                    StatusEffect.Type.STEALTH, durationTicks, 0.40, sourcePlayerId, sourceAbilityId);
            case "lifesteal" -> new StatusEffect(
                    StatusEffect.Type.LIFESTEAL, durationTicks, 0.20, sourcePlayerId, sourceAbilityId);
            default -> null;
        };
    }

    private int resolveDurationTicks(AbilityData ability, String token) {
        double seconds = ability.getDurationSeconds() > 0
                ? ability.getDurationSeconds()
                : defaultDurationSeconds(token);
        return Math.max(1, (int) Math.round(seconds * StyleManager.TICKS_PER_SECOND));
    }

    private double defaultDurationSeconds(String token) {
        return switch (lower(token)) {
            case "burn", "dot", "slow", "slow_stack" -> 4.0;
            case "stun", "stun_if_wall", "freeze", "root" -> 2.0;
            case "shield" -> 6.0;
            case "attack_buff", "defense_buff", "evasion", "evasion_buff", "evasion_zone",
                    "flying", "lifesteal", "vulnerability", "curse", "speed" -> 6.0;
            case "damage_buff", "stealth" -> ONE_SHOT_BUFF_SECONDS;
            default -> DEFAULT_STATUS_SECONDS;
        };
    }

    private boolean applyKnockback(Ref<EntityStore> targetRef,
                                   Store<EntityStore> store,
                                   Ref<EntityStore> sourceRef,
                                   AbilityData ability) {
        return applyKnockbackResult(targetRef, store, sourceRef, ability).applied();
    }

    private boolean applyKnockbackFromPoint(Ref<EntityStore> targetRef,
                                            Store<EntityStore> store,
                                            Vector3d origin,
                                            AbilityData ability) {
        Vector3d targetPosition = getPosition(targetRef, store);
        if (targetPosition == null || origin == null) {
            return false;
        }

        Vector3d direction = subtract(targetPosition, origin);
        direction.y = 0.0;
        if (!direction.isFinite() || direction.length() < 0.001) {
            direction = new Vector3d(0.0, 0.0, 1.0);
        } else {
            direction.normalize();
        }

        double push = ability != null && ability.getKnockbackForce() > 0
                ? Math.min(ability.getKnockbackForce(), 5.0)
                : 2.5;
        double lift = ability != null && ability.isKnockup() ? Math.max(0.6, ability.getLaunchHeight()) : 0.0;
        Vector3d destination = new Vector3d(targetPosition)
                .fma(push, direction)
                .add(0.0, lift, 0.0);

        NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
        if (npc == null) {
            return false;
        }

        npc.moveTo(targetRef, destination.x, destination.y, destination.z, store);
        return true;
    }

    private int launchTargetsFromPoint(Ref<EntityStore> sourceRef,
                                       Store<EntityStore> store,
                                       AbilityData ability,
                                       Vector3d center,
                                       double radius,
                                       boolean applyStun) {
        if (sourceRef == null || store == null || ability == null || center == null) {
            return 0;
        }

        int launched = 0;
        for (Ref<EntityStore> targetRef : collectNearbyNpcTargets(store, center, radius, 8)) {
            if (targetRef == null || !targetRef.isValid()) {
                continue;
            }
            boolean moved = applyKnockbackFromPoint(targetRef, store, center, ability);
            if (!moved) {
                continue;
            }
            if (applyStun) {
                applyTargetToken("stun", targetRef, store, sourceRef, resolveEntityId(sourceRef, store), ability);
            }
            applyEffectById(targetRef, store, resolveImpactEffectId("terra", "stone", ability));
            launched++;
        }
        return launched;
    }

    private Vector3d resolveCastContextPosition(CastContext context,
                                                Ref<EntityStore> fallbackRef,
                                                Store<EntityStore> store) {
        if (context != null && context.targetBlock() != null) {
            Vector3i block = context.targetBlock();
            return new Vector3d(block.x + 0.5, block.y, block.z + 0.5);
        }
        return getPosition(fallbackRef, store);
    }

    private void applyAerialQuakeClarityEffect(AbilityData ability,
                                               Ref<EntityStore> targetRef,
                                               Store<EntityStore> store,
                                               String effectId) {
        if (!isQuakeGroundImpactAbility(ability)
                || targetRef == null
                || !targetRef.isValid()
                || store == null
                || isTargetGrounded(targetRef, store)) {
            return;
        }
        applyEffectById(targetRef, store, effectId);
    }

    private boolean isQuakeGroundImpactAbility(AbilityData ability) {
        if (ability == null) {
            return false;
        }
        String abilityId = lower(ability.getId());
        String terrainEffect = lower(ability.getTerrainEffect());
        return "stomp".equals(abilityId)
                || "aftershock".equals(abilityId)
                || "sinkhole".equals(abilityId)
                || terrainEffect.contains("seismic_shockwave")
                || terrainEffect.contains("lingering_tremor");
    }

    private KnockbackResult applyKnockbackResult(Ref<EntityStore> targetRef,
                                                 Store<EntityStore> store,
                                                 Ref<EntityStore> sourceRef,
                                                 AbilityData ability) {
        Vector3d targetPosition = getPosition(targetRef, store);
        Vector3d sourcePosition = getPosition(sourceRef, store);
        if (targetPosition == null || sourcePosition == null) {
            return KnockbackResult.none();
        }

        Vector3d direction = subtract(targetPosition, sourcePosition);
        direction.y = 0.0;
        direction = normalize(direction);

        double push = ability.getKnockbackForce() > 0 ? Math.min(ability.getKnockbackForce(), 5.0) : 2.5;
        double lift = ability.isKnockup() ? Math.max(0.6, ability.getLaunchHeight()) : 0.0;
        Vector3d destination = new Vector3d(targetPosition)
                .fma(push, direction)
                .add(0.0, lift, 0.0);
        boolean wallImpact = isSolidTerrainImpact(store, destination, direction);

        NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
        if (npc == null) {
            return KnockbackResult.none();
        }

        npc.moveTo(targetRef, destination.x, destination.y, destination.z, store);
        return new KnockbackResult(true, wallImpact);
    }

    private boolean applyTargetToken(String token,
                                     Ref<EntityStore> targetRef,
                                     Store<EntityStore> store,
                                     Ref<EntityStore> sourceRef,
                                     String sourcePlayerId,
                                     AbilityData ability) {
        if (token == null || token.isBlank()) {
            return false;
        }

        String normalized = lower(token);
        String entityId = resolveEntityId(targetRef, store);
        if (entityId == null) {
            return false;
        }
        if (sourcePlayerId != null && sourcePlayerId.equals(entityId)) {
            LOG.info("[MOTM] target_skipped reason=caster ability=" + safe(ability != null ? ability.getId() : "")
                    + " token=" + normalized
                    + " target=" + entityId);
            return false;
        }
        NPCEntity npc = store != null && targetRef != null && targetRef.isValid()
                ? store.getComponent(targetRef, NPCEntity.getComponentType())
                : null;
        if (npc != null && isMotmSummon(npc)) {
            LOG.info("[MOTM] target_skipped reason=allied_summon ability=" + safe(ability != null ? ability.getId() : "")
                    + " token=" + normalized
                    + " target=" + entityId);
            return false;
        }

        if ("knockback".equals(normalized)) {
            boolean applied = isAnchorDragAbility(ability)
                    ? applyAnchorDrag(targetRef, store, sourceRef, ability)
                    : applyKnockback(targetRef, store, sourceRef, ability);
            if (applied) {
                LOG.info("[MOTM] target_hit reason=hostile ability=" + safe(ability != null ? ability.getId() : "")
                        + " token=" + normalized
                        + " target=" + entityId);
            }
            return applied;
        }

        if ("stun_if_wall".equals(normalized)) {
            KnockbackResult knockback = applyKnockbackResult(targetRef, store, sourceRef, ability);
            if (!knockback.applied()) {
                return false;
            }
            if (knockback.collidedWithWall()) {
                StatusEffect effect = createStatusEffect("stun", ability, sourcePlayerId, ability.getId());
                if (effect != null) {
                    mod.getStatusEffectManager().applyEffect(entityId, effect);
                }
            }
            return true;
        }

        StatusEffect effect = createStatusEffect(normalized, ability, sourcePlayerId, ability.getId());
        if (effect == null) {
            return false;
        }

        mod.getStatusEffectManager().applyEffect(entityId, effect);
        LOG.info("[MOTM] target_hit reason=hostile ability=" + safe(ability != null ? ability.getId() : "")
                + " token=" + normalized
                + " target=" + entityId);
        if (effect.getType() == StatusEffect.Type.SHOCKED) {
            recentShockedTargets.put(entityId, System.currentTimeMillis());
            LOG.info("[MOTM] Shocked token applied: ability=" + ability.getId()
                    + " target=" + entityId
                    + " durationTicks=" + effect.getInitialDurationTicks());
        }
        return true;
    }

    private boolean isAnchorDragAbility(AbilityData ability) {
        if (ability == null) {
            return false;
        }
        String abilityId = lower(ability.getId());
        String travelType = lower(ability.getTravelType());
        return "anchor_haul".equals(abilityId) || travelType.contains("anchor_drag");
    }

    private boolean applyAnchorDrag(Ref<EntityStore> targetRef,
                                    Store<EntityStore> store,
                                    Ref<EntityStore> sourceRef,
                                    AbilityData ability) {
        Vector3d anchor = getPosition(sourceRef, store);
        Vector3d targetPosition = getPosition(targetRef, store);
        if (anchor == null || targetPosition == null) {
            return false;
        }

        Vector3d direction = subtract(anchor, targetPosition);
        direction.y = 0.0;
        double remainingDistance = length(direction);
        if (remainingDistance <= 1.25) {
            return false;
        }

        direction = normalize(direction);
        double dragStep = ability.getKnockbackForce() > 0
                ? clamp(ability.getKnockbackForce() * 0.9, 1.75, MAX_PULL_STEP_DISTANCE)
                : 2.5;
        dragStep = Math.min(dragStep, Math.max(0.0, remainingDistance - 1.1));
        if (dragStep <= 0.05) {
            return false;
        }

        Vector3d destination = new Vector3d(targetPosition)
                .fma(dragStep, direction)
                .add(0.0, 0.15, 0.0);

        NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
        if (npc == null) {
            return false;
        }

        npc.moveTo(targetRef, destination.x, destination.y, destination.z, store);
        return true;
    }

    private boolean isSolidTerrainImpact(Store<EntityStore> store,
                                         Vector3d destination,
                                         Vector3d direction) {
        if (store == null || store.getExternalData() == null || store.getExternalData().getWorld() == null) {
            return false;
        }

        World world = store.getExternalData().getWorld();
        double probeDistance = 0.9;
        Vector3d ahead = new Vector3d(destination).fma(probeDistance, direction);
        return isSolidBlock(world, destination) || isSolidBlock(world, ahead);
    }

    private boolean isSolidBlock(World world, Vector3d position) {
        if (world == null || position == null) {
            return false;
        }

        int blockX = (int) Math.floor(position.x);
        int blockY = (int) Math.floor(position.y);
        int blockZ = (int) Math.floor(position.z);
        for (int yOffset = 0; yOffset <= 1; yOffset++) {
            BlockType blockType = world.getBlockType(blockX, blockY + yOffset, blockZ);
            if (blockType != null && blockType.getMaterial() == BlockMaterial.Solid) {
                return true;
            }
        }

        return false;
    }

    private boolean applyLineControlPull(Ref<EntityStore> targetRef,
                                         Store<EntityStore> store,
                                         Ref<EntityStore> sourceRef,
                                         AbilityData ability) {
        Vector3d anchor = getPosition(sourceRef, store);
        if (anchor == null) {
            return false;
        }

        return applyPullTowardsPoint(
                targetRef,
                store,
                anchor,
                ability,
                DEFAULT_PULL_STOP_DISTANCE,
                1.0,
                0.0
        );
    }

    private boolean applyPullTowardsPoint(Ref<EntityStore> targetRef,
                                          Store<EntityStore> store,
                                          Vector3d anchor,
                                          AbilityData ability,
                                          double stopDistance,
                                          double scale,
                                          double verticalLift) {
        Vector3d targetPosition = getPosition(targetRef, store);
        if (targetPosition == null || anchor == null) {
            return false;
        }

        Vector3d direction = subtract(anchor, targetPosition);
        direction.y = 0.0;
        double remainingDistance = length(direction);
        if (remainingDistance <= stopDistance + 0.05) {
            return false;
        }

        direction = normalize(direction);
        double step = Math.min(
                resolvePullStep(ability, scale, 0.75),
                Math.max(0.0, remainingDistance - stopDistance)
        );
        if (step <= 0.05) {
            return false;
        }

        Vector3d destination = new Vector3d(targetPosition)
                .fma(step, direction)
                .add(0.0, verticalLift, 0.0);

        NPCEntity npc = store.getComponent(targetRef, NPCEntity.getComponentType());
        if (npc == null) {
            return false;
        }

        npc.moveTo(targetRef, destination.x, destination.y, destination.z, store);
        return true;
    }

    private String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player != null) {
            String playerId = mod.getRuntimePlayerId(player);
            if (playerId != null) {
                return playerId;
            }
        }

        UUIDComponent uuidComponent = store.getComponent(ref, UUIDComponent.getComponentType());
        UUID uuid = uuidComponent != null ? uuidComponent.getUuid() : null;
        return uuid != null ? uuid.toString() : null;
    }

    private List<String> parseEffectTokens(String effect) {
        if (effect == null || effect.isBlank()) {
            return List.of();
        }

        List<String> tokens = new ArrayList<>();
        for (String part : effect.toLowerCase(Locale.ROOT).split("\\+")) {
            String token = part.trim();
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private List<String> humanizeTokens(Set<String> tokens) {
        List<String> labels = new ArrayList<>();
        for (String token : tokens) {
            labels.add(humanize(token));
        }
        return labels;
    }

    private List<String> dedupeSummaryParts(List<String> summaryParts) {
        return new ArrayList<>(new LinkedHashSet<>(summaryParts));
    }

    private boolean isProjectileLike(AbilityData ability) {
        String castType = lower(ability.getCastType());
        return LINE_CAST_TYPES.contains(castType) || MULTI_TARGET_CAST_TYPES.contains(castType);
    }

    private boolean isMotmSummon(NPCEntity npc) {
        if (npc == null || npc.getRoleName() == null) {
            return false;
        }
        return SUMMON_ROLE_NAME.equalsIgnoreCase(npc.getRoleName())
                || PROJECTILE_VISUAL_ROLE_NAME.equalsIgnoreCase(npc.getRoleName())
                || FIELD_VISUAL_ROLE_NAME.equalsIgnoreCase(npc.getRoleName());
    }

    private boolean isMotmVisualProxy(Ref<EntityStore> ref) {
        return ref != null && visualProxyRefs.contains(ref);
    }

    private String buildMovementSummary(String castType, double horizontalDistance, double verticalDistance) {
        List<String> parts = new ArrayList<>();
        parts.add(castType.replace('_', ' '));
        if (horizontalDistance > 0.0) parts.add(formatDistance(horizontalDistance) + "m forward");
        if (verticalDistance > 0.0) parts.add("+" + formatDistance(verticalDistance) + "m vertical");
        return String.join(" ", parts);
    }

    private String formatEffectLabel(String effectId) {
        if (effectId == null || effectId.isBlank()) {
            return "effect";
        }
        return effectId.replace("MOTM_", "").replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private String humanize(String rawValue) {
        return rawValue == null ? "" : rawValue.replace('_', ' ');
    }

    private String lower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private Vector3d rotateAroundY(Vector3d vector, double degrees) {
        if (Math.abs(degrees) < 0.001) {
            return normalize(vector);
        }

        double radians = Math.toRadians(degrees);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        Vector3d rotated = new Vector3d(
                (vector.x * cos) - (vector.z * sin),
                vector.y,
                (vector.x * sin) + (vector.z * cos)
        );
        return normalize(rotated);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatDistance(double distance) {
        return String.format(Locale.US, "%.1f", distance);
    }

    private String formatVector(Vector3d vector) {
        if (vector == null) {
            return "(unknown)";
        }
        return "("
                + formatDistance(vector.x) + ", "
                + formatDistance(vector.y) + ", "
                + formatDistance(vector.z) + ")";
    }

    private Vector3d subtract(Vector3d left, Vector3d right) {
        return MotmPlaybackGeometry.subtract(left, right);
    }

    private Vector3d normalize(Vector3d vector) {
        return MotmPlaybackGeometry.normalize(vector);
    }

    private double dot(Vector3d left, Vector3d right) {
        return MotmPlaybackGeometry.dot(left, right);
    }

    private double length(Vector3d value) {
        return MotmPlaybackGeometry.length(value);
    }

    private double distance(Vector3d left, Vector3d right) {
        return MotmPlaybackGeometry.distance(left, right);
    }

    private boolean isAlloyFollowUp(ActiveWeaponFollowUp followUp) {
        return followUp != null && "alloy_enhancement".equals(lower(followUp.sourceAbilityId()));
    }

    private String validateOrBindFollowUpItem(String playerId, ActiveWeaponFollowUp followUp, String itemId) {
        if (followUp == null || itemId == null || itemId.isBlank()) {
            return null;
        }
        if (!isAlloyFollowUp(followUp)) {
            return null;
        }

        if (followUp.boundItemId == null || followUp.boundItemId.isBlank()) {
            followUp.boundItemId = itemId;
            LOG.info("[MOTM] Alloy Enhancement bound: playerId=" + playerId
                    + " item=" + itemId
                    + " uses=" + followUp.remainingUses);
            return null;
        }

        if (!followUp.boundItemId.equalsIgnoreCase(itemId)) {
            activeWeaponFollowUpsByPlayer.remove(playerId);
            String message = "[MOTM] Alloy Enhancement ended: switched from "
                    + followUp.boundItemId + " to " + itemId + ".";
            LOG.info(message + " playerId=" + playerId);
            return message;
        }
        return null;
    }

    private boolean restoreHeldItemDurability(Player runtimePlayer, String itemId) {
        if (runtimePlayer == null || runtimePlayer.getInventory() == null || itemId == null || itemId.isBlank()) {
            return false;
        }

        Inventory inventory = runtimePlayer.getInventory();
        if (restoreActiveContainerDurability(inventory.getTools(), inventory.getActiveToolsSlot(), itemId)) {
            return true;
        }
        return restoreActiveContainerDurability(inventory.getHotbar(), inventory.getActiveHotbarSlot(), itemId);
    }

    private boolean restoreActiveContainerDurability(ItemContainer container, byte slot, String itemId) {
        if (container == null || slot < 0) {
            return false;
        }

        ItemStack stack = container.getItemStack(slot);
        if (stack == null || stack.isEmpty() || stack.getItemId() == null || !stack.getItemId().equalsIgnoreCase(itemId)) {
            return false;
        }
        if (stack.getMaxDurability() <= 0 || stack.getDurability() >= stack.getMaxDurability()) {
            return true;
        }

        ItemStack restored = stack.withRestoredDurability(stack.getMaxDurability());
        if (!MotmInventoryOps.restoreSlot(container, slot, restored, LOG, "restoreActiveContainerDurability")) {
            return false;
        }
        LOG.info("[MOTM] Alloy Enhancement restored durability: item=" + itemId
                + " slot=" + slot
                + " durability=" + formatDistance(stack.getDurability())
                + "/" + formatDistance(stack.getMaxDurability()));
        return true;
    }

    public record CastContext(Ref<EntityStore> explicitTargetRef, Vector3i targetBlock) {
        public static CastContext empty() {
            return new CastContext(null, null);
        }

        public static CastContext atPosition(Vector3d position) {
            if (position == null) {
                return empty();
            }
            return new CastContext(null, new Vector3i(
                    (int) Math.floor(position.x),
                    (int) Math.floor(position.y),
                    (int) Math.floor(position.z)
            ));
        }
    }

    private record ArmedStomp(String playerId,
                              PlayerData player,
                              StyleData style,
                              AbilityData ability,
                              String traceId,
                              long armedAtMillis,
                              long expireAtMillis,
                              double previousY,
                              boolean wasAirborne) {}

    private record BuriedVictim(Ref<EntityStore> targetRef,
                                Float originalScale,
                                long expireAtMillis) {}

    private record RecentPosition(Vector3d position, long recordedAtMillis) {}

    private record ActivePlayerAnchor(String reason,
                                      String ownerPlayerId,
                                      Ref<EntityStore> ownerRef,
                                      Vector3d anchor,
                                      long expireAtMillis,
                                      String completionEffectId) {}

    private static final class ActiveSelfEffect {
        private final String ownerPlayerId;
        private final Ref<EntityStore> ownerRef;
        private final String effectId;
        private final long expireAtMillis;
        private long nextApplyAtMillis;

        private ActiveSelfEffect(String ownerPlayerId,
                                 Ref<EntityStore> ownerRef,
                                 String effectId,
                                 long expireAtMillis,
                                 long nextApplyAtMillis) {
            this.ownerPlayerId = ownerPlayerId;
            this.ownerRef = ownerRef;
            this.effectId = effectId;
            this.expireAtMillis = expireAtMillis;
            this.nextApplyAtMillis = nextApplyAtMillis;
        }

        public String ownerPlayerId() { return ownerPlayerId; }
        public Ref<EntityStore> ownerRef() { return ownerRef; }
        public String effectId() { return effectId; }
        public long expireAtMillis() { return expireAtMillis; }
        public long nextApplyAtMillis() { return nextApplyAtMillis; }
    }

    private record ActiveDelayedBurst(String ownerPlayerId,
                                      Ref<EntityStore> ownerRef,
                                      String classId,
                                      String styleId,
                                      AbilityData ability,
                                      Vector3d center,
                                      String primaryEntityId,
                                      long burstAtMillis,
                                      double radius,
                                      String traceId) {}

    public record ExecutionResult(
            PlaybackResult playback,
            int targetsHit,
            double totalDamage,
            int summonsCreated,
            int summonsBuffed,
            boolean formApplied,
            String summary
    ) {
        public static ExecutionResult none(String summary) {
            return new ExecutionResult(PlaybackResult.none(""), 0, 0.0, 0, 0, false, summary);
        }
    }

    public record PlaybackResult(
            boolean effectApplied,
            String effectId,
            boolean movementApplied,
            double movementDistance,
            double verticalDistance,
            Vector3d startPosition,
            Vector3d endPosition,
            String summary
    ) {
        public static PlaybackResult none(String summary) {
            return new PlaybackResult(false, null, false, 0.0, 0.0, null, null, summary);
        }
    }

    private record MovementResult(boolean applied,
                                  double horizontalDistance,
                                  double verticalDistance,
                                  Vector3d startPosition,
                                  Vector3d endPosition,
                                  String summary) {
        private static MovementResult none() {
            return new MovementResult(false, 0.0, 0.0, null, null, "");
        }
    }

    private record CombatResolution(int targetsHit, double totalDamage, String summary) {
        private static CombatResolution none() {
            return new CombatResolution(0, 0.0, "");
        }
    }

    private record SupportResolution(double healed, double shielded, int effectsApplied, String summary) {
        private static SupportResolution none() {
            return new SupportResolution(0.0, 0.0, 0, "");
        }
    }

    private record EffectResolution(int targetsAffected, int effectsApplied, String summary) {
        private static EffectResolution none() {
            return new EffectResolution(0, 0, "");
        }
    }

    private record FormRuntimeResult(boolean applied, String summary) {
        private static FormRuntimeResult none() {
            return new FormRuntimeResult(false, "");
        }
    }

    private record SummonRuntimeResult(int spawned, int buffed, String summary) {
        private static SummonRuntimeResult none() {
            return new SummonRuntimeResult(0, 0, "");
        }
    }

    private record ChannelRuntimeResult(boolean started, String summary) {
        private static ChannelRuntimeResult none() {
            return new ChannelRuntimeResult(false, "");
        }
    }

    private record LineControlRuntimeResult(boolean started, String summary) {
        private static LineControlRuntimeResult none() {
            return new LineControlRuntimeResult(false, "");
        }
    }

    private record MovementContactRuntimeResult(int targetsHit, double damage, String summary) {
        private static MovementContactRuntimeResult none() {
            return new MovementContactRuntimeResult(0, 0.0, "");
        }
    }

    private record WeaponFollowUpResult(boolean armed, String summary) {
        private static WeaponFollowUpResult none() {
            return new WeaponFollowUpResult(false, "");
        }
    }

    private record KnockbackResult(boolean applied, boolean collidedWithWall) {
        private static KnockbackResult none() {
            return new KnockbackResult(false, false);
        }
    }

    private record ProjectileLaunchResult(int launched, String summary) {
        private static ProjectileLaunchResult none() {
            return new ProjectileLaunchResult(0, "");
        }
    }

    private record FieldRuntimeResult(boolean activated, String summary) {
        private static FieldRuntimeResult none() {
            return new FieldRuntimeResult(false, "");
        }
    }

    private record SupplementalTerrainRuntimeResult(boolean activated, String summary) {
        private static SupplementalTerrainRuntimeResult none() {
            return new SupplementalTerrainRuntimeResult(false, "");
        }
    }

    private record AbilitySpecificRuntimeResult(String summary) {
        private static AbilitySpecificRuntimeResult none() {
            return new AbilitySpecificRuntimeResult("");
        }
    }

    private record TargetCandidate(
            Ref<EntityStore> ref,
            Vector3d position,
            double distance,
            double axialDistance,
            double lateralDistance,
            double forwardDot
    ) {}

    private record TargetingFrame(
            Ref<EntityStore> explicitTarget,
            Vector3d areaCenter,
            double range,
            double areaRadius,
            double lineHalfWidth,
            double coneThreshold,
            List<TargetCandidate> candidates
    ) {}

    private static final class ActiveSummon {
        private final String ownerPlayerId;
        private final Ref<EntityStore> ownerRef;
        private final String classId;
        private final String styleId;
        private final AbilityData ability;
        private final String role;
        private final boolean ranged;
        private final double attackRange;
        private final double chaseRange;
        private final long attackIntervalMillis;
        private final long hatchAtMillis;
        private final Ref<EntityStore> ref;
        private long nextThinkAtMillis;
        private long nextAttackAtMillis;
        private long buffExpireAtMillis;
        private long expireAtMillis;
        private final double baseDamage;
        private Ref<EntityStore> currentTargetRef;
        private long targetLockExpireAtMillis;
        private boolean awakened;
        private final int casterLevel;
        private final String casterStatSnapshot;

        private ActiveSummon(String ownerPlayerId,
                             Ref<EntityStore> ref,
                             Ref<EntityStore> ownerRef,
                             String classId,
                             String styleId,
                             AbilityData ability,
                             String role,
                             boolean ranged,
                             double attackRange,
                             double chaseRange,
                             long attackIntervalMillis,
                             long hatchAtMillis,
                             long expireAtMillis,
                             long nextThinkAtMillis,
                             long nextAttackAtMillis,
                             long buffExpireAtMillis,
                             double baseDamage,
                             int casterLevel,
                             String casterStatSnapshot,
                             Ref<EntityStore> currentTargetRef,
                             long targetLockExpireAtMillis,
                             boolean awakened) {
            this.ownerPlayerId = ownerPlayerId;
            this.ref = ref;
            this.ownerRef = ownerRef;
            this.classId = classId;
            this.styleId = styleId;
            this.ability = ability;
            this.role = role;
            this.ranged = ranged;
            this.attackRange = attackRange;
            this.chaseRange = chaseRange;
            this.attackIntervalMillis = attackIntervalMillis;
            this.hatchAtMillis = hatchAtMillis;
            this.expireAtMillis = expireAtMillis;
            this.nextThinkAtMillis = nextThinkAtMillis;
            this.nextAttackAtMillis = nextAttackAtMillis;
            this.buffExpireAtMillis = buffExpireAtMillis;
            this.baseDamage = baseDamage;
            this.casterLevel = casterLevel;
            this.casterStatSnapshot = casterStatSnapshot;
            this.currentTargetRef = currentTargetRef;
            this.targetLockExpireAtMillis = targetLockExpireAtMillis;
            this.awakened = awakened;
        }

        public Ref<EntityStore> ownerRef() { return ownerRef; }
        public Ref<EntityStore> ref() { return ref; }
        public long expireAtMillis() { return expireAtMillis; }
        public void extend(long extensionMillis) { expireAtMillis += extensionMillis; }
    }

    private static final class ActiveChannel {
        private final String ownerPlayerId;
        private final Ref<EntityStore> ownerRef;
        private final Ref<EntityStore> targetRef;
        private final AbilityData ability;
        private final long expireAtMillis;
        private long nextPulseAtMillis;

        private ActiveChannel(String ownerPlayerId,
                              Ref<EntityStore> ownerRef,
                              Ref<EntityStore> targetRef,
                              AbilityData ability,
                              long expireAtMillis,
                              long nextPulseAtMillis) {
            this.ownerPlayerId = ownerPlayerId;
            this.ownerRef = ownerRef;
            this.targetRef = targetRef;
            this.ability = ability;
            this.expireAtMillis = expireAtMillis;
            this.nextPulseAtMillis = nextPulseAtMillis;
        }

        public String ownerPlayerId() { return ownerPlayerId; }
        public Ref<EntityStore> ownerRef() { return ownerRef; }
        public Ref<EntityStore> targetRef() { return targetRef; }
        public AbilityData ability() { return ability; }
        public long expireAtMillis() { return expireAtMillis; }
        public long nextPulseAtMillis() { return nextPulseAtMillis; }
    }

    private static final class ActiveLineControl {
        private final String ownerPlayerId;
        private final Ref<EntityStore> ownerRef;
        private final Ref<EntityStore> targetRef;
        private final AbilityData ability;
        private final Vector3d anchorPosition;
        private final long expireAtMillis;
        private long nextPulseAtMillis;

        private ActiveLineControl(String ownerPlayerId,
                                  Ref<EntityStore> ownerRef,
                                  Ref<EntityStore> targetRef,
                                  AbilityData ability,
                                  Vector3d anchorPosition,
                                  long expireAtMillis,
                                  long nextPulseAtMillis) {
            this.ownerPlayerId = ownerPlayerId;
            this.ownerRef = ownerRef;
            this.targetRef = targetRef;
            this.ability = ability;
            this.anchorPosition = anchorPosition;
            this.expireAtMillis = expireAtMillis;
            this.nextPulseAtMillis = nextPulseAtMillis;
        }

        public String ownerPlayerId() { return ownerPlayerId; }
        public Ref<EntityStore> ownerRef() { return ownerRef; }
        public Ref<EntityStore> targetRef() { return targetRef; }
        public AbilityData ability() { return ability; }
        public Vector3d anchorPosition() { return anchorPosition; }
        public long expireAtMillis() { return expireAtMillis; }
        public long nextPulseAtMillis() { return nextPulseAtMillis; }
    }

    private static final class ActiveTransformation {
        private final String playerId;
        private final Ref<EntityStore> ownerRef;
        private final AbilityData sourceAbility;
        private final String modelId;
        private final long expireAtMillis;
        private final double damageBonus;
        private final double weaponBonus;
        private final double movementMultiplier;
        private final double verticalBonus;
        private final String weaponRiderToken;
        private final double locomotionTriggerDistance;
        private final double collisionRadius;
        private Vector3d lastOwnerPosition;
        private final String summary;

        private ActiveTransformation(String playerId,
                                     Ref<EntityStore> ownerRef,
                                     AbilityData sourceAbility,
                                     String modelId,
                                     long expireAtMillis,
                                     double damageBonus,
                                     double weaponBonus,
                                     double movementMultiplier,
                                     double verticalBonus,
                                     String weaponRiderToken,
                                     double locomotionTriggerDistance,
                                     double collisionRadius,
                                     Vector3d lastOwnerPosition,
                                     String summary) {
            this.playerId = playerId;
            this.ownerRef = ownerRef;
            this.sourceAbility = sourceAbility;
            this.modelId = modelId;
            this.expireAtMillis = expireAtMillis;
            this.damageBonus = damageBonus;
            this.weaponBonus = weaponBonus;
            this.movementMultiplier = movementMultiplier;
            this.verticalBonus = verticalBonus;
            this.weaponRiderToken = weaponRiderToken;
            this.locomotionTriggerDistance = locomotionTriggerDistance;
            this.collisionRadius = collisionRadius;
            this.lastOwnerPosition = lastOwnerPosition != null ? new Vector3d(lastOwnerPosition) : null;
            this.summary = summary;
        }

        public String playerId() { return playerId; }
        public Ref<EntityStore> ownerRef() { return ownerRef; }
        public AbilityData sourceAbility() { return sourceAbility; }
        public String modelId() { return modelId; }
        public long expireAtMillis() { return expireAtMillis; }
        public double damageBonus() { return damageBonus; }
        public double weaponBonus() { return weaponBonus; }
        public double movementMultiplier() { return movementMultiplier; }
        public double verticalBonus() { return verticalBonus; }
        public String weaponRiderToken() { return weaponRiderToken; }
        public double locomotionTriggerDistance() { return locomotionTriggerDistance; }
        public double collisionRadius() { return collisionRadius; }
        public Vector3d lastOwnerPosition() { return lastOwnerPosition; }
        public String summary() { return summary; }
        public String abilityId() { return sourceAbility != null ? sourceAbility.getId() : ""; }
    }

    private record NearbyTargetCandidate(Ref<EntityStore> ref, double distance) { }

    private record SegmentTargetCandidate(Ref<EntityStore> ref, double alongDistance) { }

    private record ProjectileVisualRuntime(Ref<EntityStore> visualRef,
                                           String travelEffectId,
                                           long nextRefreshAtMillis) {
        private static ProjectileVisualRuntime none() {
            return new ProjectileVisualRuntime(null, null, Long.MAX_VALUE);
        }
    }

    private record FieldVisualRuntime(List<Ref<EntityStore>> visualRefs,
                                      String loopEffectId,
                                      long nextRefreshAtMillis) {
        private static FieldVisualRuntime none() {
            return new FieldVisualRuntime(List.of(), null, Long.MAX_VALUE);
        }
    }

    private record TemporaryTerrainSelection(String reason,
                                             World world,
                                             Vector3i anchor,
                                             BlockSelection originalSelection,
                                             Set<String> protectedBlockKeys,
                                             long expireAtMillis) { }

    private static final class ActiveMovingTerrainTrail {
        private final String reason;
        private final World world;
        private final Ref<EntityStore> ownerRef;
        private final String[] blockIds;
        private final long expireAtMillis;
        private long nextPlaceAtMillis;
        private Vector3i lastAnchor;
        private Vector3d lastPosition;

        private ActiveMovingTerrainTrail(String reason,
                                         World world,
                                         Ref<EntityStore> ownerRef,
                                         String[] blockIds,
                                         long expireAtMillis,
                                         long nextPlaceAtMillis) {
            this.reason = reason;
            this.world = world;
            this.ownerRef = ownerRef;
            this.blockIds = blockIds;
            this.expireAtMillis = expireAtMillis;
            this.nextPlaceAtMillis = nextPlaceAtMillis;
        }
    }

    private static final class ActiveStackingColumn {
        private final String reason;
        private final World world;
        private final Vector3i anchor;
        private final int blockTypeId;
        private final int height;
        private final long expireAtMillis;
        private long nextStageAtMillis;
        private int placedHeight;

        private ActiveStackingColumn(String reason,
                                     World world,
                                     Vector3i anchor,
                                     int blockTypeId,
                                     int height,
                                     long expireAtMillis,
                                     long nextStageAtMillis) {
            this.reason = reason;
            this.world = world;
            this.anchor = anchor;
            this.blockTypeId = blockTypeId;
            this.height = height;
            this.expireAtMillis = expireAtMillis;
            this.nextStageAtMillis = nextStageAtMillis;
            this.placedHeight = 0;
        }
    }

    private static final class ActiveLapidaryGem {
        private final String ownerPlayerId;
        private final Ref<EntityStore> ref;
        private final Vector3d center;
        private double currentHp;
        private final double maxHp;
        private final long expireAtMillis;
        private String lastLabel;

        private ActiveLapidaryGem(String ownerPlayerId,
                                  Ref<EntityStore> ref,
                                  Vector3d center,
                                  double currentHp,
                                  double maxHp,
                                  long expireAtMillis,
                                  String lastLabel) {
            this.ownerPlayerId = ownerPlayerId;
            this.ref = ref;
            this.center = center;
            this.currentHp = currentHp;
            this.maxHp = maxHp;
            this.expireAtMillis = expireAtMillis;
            this.lastLabel = lastLabel;
        }
    }

    private static final class ActiveWeaponFollowUp {
        private final String playerId;
        private final AbilityData sourceAbility;
        private final long expireAtMillis;
        private int remainingUses;
        private final double flatDamageBonus;
        private final String riderToken;
        private final double lifestealBonus;
        private final double shieldPercentOnHit;
        private final double healRatioOnHit;
        private final double splashRadius;
        private final double splashDamageRatio;
        private final String secondaryRiderToken;
        private final double damageMultiplierBonus;
        private String boundItemId;

        private ActiveWeaponFollowUp(String playerId,
                                     AbilityData sourceAbility,
                                     long expireAtMillis,
                                     int remainingUses,
                                     double flatDamageBonus,
                                     String riderToken,
                                     double lifestealBonus,
                                     double shieldPercentOnHit,
                                     double healRatioOnHit,
                                     double splashRadius,
                                     double splashDamageRatio,
                                     String secondaryRiderToken,
                                     double damageMultiplierBonus,
                                     String boundItemId) {
            this.playerId = playerId;
            this.sourceAbility = sourceAbility;
            this.expireAtMillis = expireAtMillis;
            this.remainingUses = remainingUses;
            this.flatDamageBonus = flatDamageBonus;
            this.riderToken = riderToken;
            this.lifestealBonus = lifestealBonus;
            this.shieldPercentOnHit = shieldPercentOnHit;
            this.healRatioOnHit = healRatioOnHit;
            this.splashRadius = splashRadius;
            this.splashDamageRatio = splashDamageRatio;
            this.secondaryRiderToken = secondaryRiderToken;
            this.damageMultiplierBonus = damageMultiplierBonus;
            this.boundItemId = boundItemId;
        }

        public String playerId() { return playerId; }
        public String sourceAbilityId() { return sourceAbility != null ? sourceAbility.getId() : ""; }
        public AbilityData sourceAbility() { return sourceAbility; }
        public long expireAtMillis() { return expireAtMillis; }
        public int remainingUses() { return remainingUses; }
    }

    private static final class ActiveProjectile {
        private final String ownerPlayerId;
        private final Ref<EntityStore> ownerRef;
        private final String classId;
        private final String styleId;
        private final AbilityData ability;
        private final Vector3d position;
        private final Vector3d direction;
        private final double speedPerTick;
        private final double maxDistance;
        private final double impactRadius;
        private final double collisionRadius;
        private final long activateAtMillis;
        private final long expireAtMillis;
        private final double baseDamage;
        private final Set<String> hitEntityIds;
        private final Ref<EntityStore> visualRef;
        private final String travelEffectId;
        private final String traceId;
        private long nextVisualRefreshAtMillis;
        private double travelledDistance;

        private ActiveProjectile(String ownerPlayerId,
                                 Ref<EntityStore> ownerRef,
                                 String classId,
                                 String styleId,
                                 AbilityData ability,
                                 Vector3d position,
                                 Vector3d direction,
                                 double speedPerTick,
                                 double maxDistance,
                                 double impactRadius,
                                 double collisionRadius,
                                 long activateAtMillis,
                                 long expireAtMillis,
                                 double baseDamage,
                                 Set<String> hitEntityIds,
                                 Ref<EntityStore> visualRef,
                                 String travelEffectId,
                                 long nextVisualRefreshAtMillis,
                                 String traceId) {
            this.ownerPlayerId = ownerPlayerId;
            this.ownerRef = ownerRef;
            this.classId = classId;
            this.styleId = styleId;
            this.ability = ability;
            this.position = position;
            this.direction = direction;
            this.speedPerTick = speedPerTick;
            this.maxDistance = maxDistance;
            this.impactRadius = impactRadius;
            this.collisionRadius = collisionRadius;
            this.activateAtMillis = activateAtMillis;
            this.expireAtMillis = expireAtMillis;
            this.baseDamage = baseDamage;
            this.hitEntityIds = hitEntityIds;
            this.visualRef = visualRef;
            this.travelEffectId = travelEffectId;
            this.traceId = traceId;
            this.nextVisualRefreshAtMillis = nextVisualRefreshAtMillis;
            this.travelledDistance = 0.0;
        }

        public String ownerPlayerId() { return ownerPlayerId; }
        public Ref<EntityStore> ownerRef() { return ownerRef; }
        public String classId() { return classId; }
        public String styleId() { return styleId; }
        public AbilityData ability() { return ability; }
        public Vector3d position() { return position; }
        public Vector3d direction() { return direction; }
        public double speedPerTick() { return speedPerTick; }
        public double maxDistance() { return maxDistance; }
        public double impactRadius() { return impactRadius; }
        public double collisionRadius() { return collisionRadius; }
        public long activateAtMillis() { return activateAtMillis; }
        public long expireAtMillis() { return expireAtMillis; }
        public double baseDamage() { return baseDamage; }
        public Set<String> hitEntityIds() { return hitEntityIds; }
        public Ref<EntityStore> visualRef() { return visualRef; }
        public String travelEffectId() { return travelEffectId; }
        public String traceId() { return traceId; }
        public long nextVisualRefreshAtMillis() { return nextVisualRefreshAtMillis; }
        public double travelledDistance() { return travelledDistance; }
    }

    private static final class ActiveField {
        private final String ownerPlayerId;
        private final Ref<EntityStore> ownerRef;
        private final String classId;
        private final String styleId;
        private final AbilityData ability;
        private Vector3d center;
        private final Vector3d forwardDirection;
        private final Vector3d lineDirection;
        private final double radius;
        private final double halfWidth;
        private final double thickness;
        private final long expireAtMillis;
        private final long activateAtMillis;
        private final boolean followOwner;
        private final List<Ref<EntityStore>> visualRefs;
        private final String loopEffectId;
        private final String traceId;
        private long nextPulseAtMillis;
        private long nextVisualRefreshAtMillis;

        private ActiveField(String ownerPlayerId,
                            Ref<EntityStore> ownerRef,
                            String classId,
                            String styleId,
                            AbilityData ability,
                            Vector3d center,
                            Vector3d forwardDirection,
                            Vector3d lineDirection,
                            double radius,
                            double halfWidth,
                            double thickness,
                            long expireAtMillis,
                            long activateAtMillis,
                            long nextPulseAtMillis,
                            boolean followOwner,
                            List<Ref<EntityStore>> visualRefs,
                            String loopEffectId,
                            long nextVisualRefreshAtMillis,
                            String traceId) {
            this.ownerPlayerId = ownerPlayerId;
            this.ownerRef = ownerRef;
            this.classId = classId;
            this.styleId = styleId;
            this.ability = ability;
            this.center = center;
            this.forwardDirection = forwardDirection;
            this.lineDirection = lineDirection;
            this.radius = radius;
            this.halfWidth = halfWidth;
            this.thickness = thickness;
            this.expireAtMillis = expireAtMillis;
            this.activateAtMillis = activateAtMillis;
            this.nextPulseAtMillis = nextPulseAtMillis;
            this.followOwner = followOwner;
            this.visualRefs = visualRefs;
            this.loopEffectId = loopEffectId;
            this.traceId = traceId;
            this.nextVisualRefreshAtMillis = nextVisualRefreshAtMillis;
        }

        public String ownerPlayerId() { return ownerPlayerId; }
        public Ref<EntityStore> ownerRef() { return ownerRef; }
        public String classId() { return classId; }
        public String styleId() { return styleId; }
        public AbilityData ability() { return ability; }
        public Vector3d center() { return center; }
        public Vector3d forwardDirection() { return forwardDirection; }
        public Vector3d lineDirection() { return lineDirection; }
        public double radius() { return radius; }
        public double halfWidth() { return halfWidth; }
        public double thickness() { return thickness; }
        public long expireAtMillis() { return expireAtMillis; }
        public long activateAtMillis() { return activateAtMillis; }
        public long nextPulseAtMillis() { return nextPulseAtMillis; }
        public boolean followOwner() { return followOwner; }
        public List<Ref<EntityStore>> visualRefs() { return visualRefs; }
        public String loopEffectId() { return loopEffectId; }
        public String traceId() { return traceId; }
        public long nextVisualRefreshAtMillis() { return nextVisualRefreshAtMillis; }
    }
}
