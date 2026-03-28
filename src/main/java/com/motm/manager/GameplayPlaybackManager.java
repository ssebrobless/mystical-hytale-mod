package com.motm.manager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
    private static final Set<String> CONE_CAST_TYPES = Set.of("cone");
    private static final Set<String> MULTI_TARGET_CAST_TYPES = Set.of("projectile_volley", "chain");
    private static final Set<String> CASTER_EFFECT_TOKENS = Set.of(
            "attack_buff", "defense_buff", "evasion", "evasion_buff", "evasion_zone",
            "stealth", "damage_buff", "lifesteal", "flying", "self_burn", "speed");
    private static final Set<String> TARGET_EFFECT_TOKENS = Set.of(
            "burn", "dot", "stun", "stun_if_wall", "slow", "slow_stack", "vulnerability",
            "freeze", "root", "blind", "deafen", "disoriented", "attack_slow",
            "grounded", "shocked", "lightning", "knockback", "curse");
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

    private final MenteesMod mod;
    private final Map<String, List<ActiveSummon>> activeSummonsByOwner = new HashMap<>();
    private final Map<String, ActiveTransformation> activeTransformationsByPlayer = new HashMap<>();
    private final Map<String, Long> nextTransformationPulseAtByPlayer = new HashMap<>();
    private final Map<String, ActiveWeaponFollowUp> activeWeaponFollowUpsByPlayer = new HashMap<>();
    private final List<ActiveProjectile> activeProjectiles = new ArrayList<>();
    private final List<ActiveField> activeFields = new ArrayList<>();
    private final List<ActiveChannel> activeChannels = new ArrayList<>();
    private final List<ActiveLineControl> activeLineControls = new ArrayList<>();

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

    public synchronized ExecutionResult executeAbility(Player runtimePlayer,
                                                       PlayerData player,
                                                       StyleData style,
                                                       AbilityData ability,
                                                       CastContext context) {
        if (runtimePlayer == null || player == null || style == null || ability == null) {
            return ExecutionResult.none("Runtime player context is unavailable.");
        }

        PlaybackResult playback = playAbility(runtimePlayer, player, style, ability);
        ProjectileLaunchResult projectileLaunch = launchProjectiles(runtimePlayer, player, style, ability, context);
        FieldRuntimeResult fieldRuntime = activatePersistentField(runtimePlayer, player, style, ability, context);
        SupplementalTerrainRuntimeResult supplementalTerrain = activateSupplementalTerrainRuntime(
                runtimePlayer, player, style, ability, playback);
        AbilitySpecificRuntimeResult specificRuntime = applySpecificCastRuntime(player, ability);
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
        if (!support.summary().isBlank()) summaryParts.add(support.summary());
        if (!combat.summary().isBlank()) summaryParts.add(combat.summary());
        if (lifestealHealing > 0) summaryParts.add("lifesteal " + AbilityPresentation.formatDecimal(lifestealHealing));
        if (!targetEffects.summary().isBlank()) summaryParts.add(targetEffects.summary());
        if (!lineControl.summary().isBlank()) summaryParts.add(lineControl.summary());
        if (!channel.summary().isBlank()) summaryParts.add(channel.summary());
        if (!form.summary().isBlank()) summaryParts.add(form.summary());
        if (!summons.summary().isBlank()) summaryParts.add(summons.summary());
        if (!followUp.summary().isBlank()) summaryParts.add(followUp.summary());

        String summary = summaryParts.isEmpty()
                ? "No live runtime was applied."
                : String.join(" | ", summaryParts);

        return new ExecutionResult(
                playback, combat.targetsHit(), combat.totalDamage(),
                summons.spawned(), summons.buffed(), form.applied(), summary);
    }

    public synchronized void tick() {
        long now = System.currentTimeMillis();
        activeProjectiles.removeIf(projectile -> processProjectileTick(projectile, now));
        activeFields.removeIf(field -> processFieldTick(field, now));
        activeLineControls.removeIf(lineControl -> processLineControlTick(lineControl, now));
        activeChannels.removeIf(channel -> processChannelTick(channel, now));
        activeTransformationsByPlayer.entrySet().removeIf(entry -> processTransformationTick(entry.getValue(), now));
        activeWeaponFollowUpsByPlayer.entrySet().removeIf(entry -> now >= entry.getValue().expireAtMillis() || entry.getValue().remainingUses() <= 0);
        activeSummonsByOwner.values().removeIf(List::isEmpty);
        activeSummonsByOwner.values().forEach(summons ->
                summons.removeIf(summon -> processSummonTick(summon, now)));
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

        String effectId = resolveEffectId(player.getPlayerClass(), currentStyleId(player), ability);
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

        Vector3d origin = getPosition(playerRef, store);
        Vector3d direction = resolveLaunchDirection(playerRef, store, context);
        if (origin == null || direction == null) {
            return ProjectileLaunchResult.none();
        }

        int projectileCount = resolveProjectileCount(castType, ability);
        double speedPerTick = resolveProjectileSpeedPerTick(ability);
        double maxDistance = Math.max(resolveRange(ability), 4.0);
        double impactRadius = resolveProjectileImpactRadius(ability, castType);
        double collisionRadius = resolveProjectileCollisionRadius(ability, castType);
        double spreadDegrees = resolveProjectileSpreadDegrees(castType, ability, projectileCount);
        long lifetimeMillis = resolveProjectileLifetimeMillis(ability, speedPerTick, maxDistance);
        double baseDamage = resolveDamageAmount(player, ability);
        long launchBaseTime = System.currentTimeMillis();

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
                    origin.clone(),
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
                    visual.nextRefreshAtMillis()
            ));
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

        Vector3d center = resolveAreaCenter(origin, forward, context, resolveRange(ability));
        if (center == null) {
            return FieldRuntimeResult.none();
        }

        double radius = ability.getRadius() > 0 ? ability.getRadius() : DEFAULT_AREA_RADIUS;
        double halfWidth = ability.getWidth() > 0 ? ability.getWidth() / 2.0 : Math.max(radius, 3.0);
        double thickness = ability.getCastType().equalsIgnoreCase("barrier")
                ? DEFAULT_FIELD_THICKNESS
                : Math.max(1.25, radius);
        Vector3d lineDirection = rotateAroundY(new Vector3d(forward.x, 0.0, forward.z), 90.0);
        long now = System.currentTimeMillis();
        long delayMillis = (long) (Math.max(0.0, ability.getDelaySeconds()) * 1000);
        long activateAtMillis = now + delayMillis;
        long durationMillis = (long) (Math.max(1.5, ability.getDurationSeconds() > 0 ? ability.getDurationSeconds() : 4.0) * 1000);
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
                normalize(forward),
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
        return new FieldRuntimeResult(true,
                fieldLabel + " " + timingLabel
                        + " | " + sizeLabel
                        + controlLabel);
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
            centers.add(transform.getTransform().getPosition().clone());
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

    private AbilitySpecificRuntimeResult applySpecificCastRuntime(PlayerData player,
                                                                  AbilityData ability) {
        if (player == null || ability == null || player.getPlayerId() == null) {
            return AbilitySpecificRuntimeResult.none();
        }

        List<String> granted = new ArrayList<>();
        String abilityId = lower(ability.getId());
        if (Set.of("high_tide", "river_rapids", "frolick", "refraction").contains(abilityId)
                && applyOwnerStatusToken("speed", player, ability)) {
            granted.add("speed");
        }

        if (granted.isEmpty()) {
            return AbilitySpecificRuntimeResult.none();
        }

        return new AbilitySpecificRuntimeResult(
                String.join(" | ", granted.stream().map(this::humanize).toList())
        );
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
                visual.nextRefreshAtMillis()
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
            centers.add(start.clone().addScaled(segment, factor));
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

        Vector3d from = projectile.position().clone();
        Vector3d stepDirection = normalize(projectile.direction());
        double stepDistance = Math.min(projectile.speedPerTick(), MAX_PROJECTILE_STEP_DISTANCE);
        Vector3d to = from.clone().addScaled(stepDirection, stepDistance);

        projectile.position().x = to.x;
        projectile.position().y = to.y;
        projectile.position().z = to.z;
        projectile.travelledDistance += stepDistance;
        syncProjectileVisual(projectile, now);

        if (isPiercingProjectile(projectile.ability())) {
            applyProjectileTraversalHits(projectile, player, store, from, to);
        }

        Ref<EntityStore> directHit = resolveProjectileHit(projectile, store, from, to);
        boolean expired = now >= projectile.expireAtMillis() || projectile.travelledDistance() >= projectile.maxDistance();
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
        despawnProjectileVisual(projectile);
        return true;
    }

    private boolean processFieldTick(ActiveField field, long now) {
        if (field.ownerRef() == null || !field.ownerRef().isValid()) {
            despawnFieldVisual(field);
            return true;
        }

        Store<EntityStore> store = field.ownerRef().getStore();
        if (store == null) {
            despawnFieldVisual(field);
            return true;
        }

        syncFollowOwnerFieldAnchor(field, store);

        if (now >= field.expireAtMillis()) {
            despawnFieldVisual(field);
            return true;
        }

        if (now < field.activateAtMillis()) {
            refreshFieldVisual(field, now);
            return false;
        }

        syncFieldVisual(field, now);
        if (now < field.nextPulseAtMillis()) {
            return false;
        }

        PlayerData player = mod.getPlayerDataManager().getOnlinePlayer(field.ownerPlayerId());
        if (player == null) {
            despawnFieldVisual(field);
            return true;
        }

        List<Ref<EntityStore>> targets = collectFieldTargets(field, store);
        if (!targets.isEmpty()) {
            applyFieldPulse(field, player, store, targets);
        }
        applyFieldSupportPulse(field, player);
        field.nextPulseAtMillis = now + FIELD_PULSE_INTERVAL_MS;
        return false;
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

        field.center = ownerPosition.clone();
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
                Vector3d nearestPoint = from.clone().addScaled(segment, clampedProjection);
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
                resolvedDamage *= resolveIncomingDamageMultiplier(targetEntityId);
                resolvedDamage = mod.getStatusEffectManager().absorbDamage(targetEntityId, resolvedDamage);
            }

            if (resolvedDamage > 0.0) {
                Damage damage = new Damage(new Damage.EntitySource(projectile.ownerRef()), cause, (float) resolvedDamage);
                DamageSystems.executeDamage(targetRef, store, damage);
                applyPostDamageClassPassives(player, projectile.ownerRef(), targetEntityId, resolvedDamage, true);
                totalDamage += resolvedDamage;
            }

            applyEffectById(targetRef, store, impactEffectId);
            applyProjectileTravelTypeEffects(projectile, player, store, targetRef, impactPosition, true);
        }

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
            }

            applyEffectById(targetRef, store, impactEffectId);
            applyProjectileTargetEffects(projectile, player, store, List.of(targetRef));
            applyProjectileTravelTypeEffects(projectile, player, store, targetRef, to, false);
            projectile.hitEntityIds().add(targetEntityId);
            hitIndex++;

            if (isLightningProjectile(projectile.ability())) {
                applyLightningArcSplash(projectile, player, store, targetRef);
            }
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
                Vector3d nearestPoint = from.clone().addScaled(segment, clampedProjection);
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

            applyEffectById(targetRef, store, impactEffectId);
            applyFieldTargetEffects(field, player, targetRef, store);
        }

        if (totalDamage > 0.0) {
            player.getStatistics().setTotalDamageDealt(
                    player.getStatistics().getTotalDamageDealt() + totalDamage);
            applyLifesteal(field.ownerRef(), player.getPlayerId(), totalDamage);
        }
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
        double sustainMultiplier = mod.getLevelingManager().getPlayerSustainMultiplier(player.getLevel());

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

        if (terrainEffect.contains("mudpit")) {
            applyTargetToken("root", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            applyTargetToken("slow", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
            return;
        }

        if (terrainEffect.contains("falling_rocks")) {
            applyTargetToken("knockback", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
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
            applyTargetToken("blind", targetRef, store, field.ownerRef(), player.getPlayerId(), field.ability());
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
        Vector3d destination = targetPosition.clone()
                .addScaled(field.forwardDirection(), pushSign * 1.8)
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

        return controller.addEffect(entityRef, effect, store);
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

        NPCEntity proxy = new NPCEntity(world);
        proxy.setRoleName(PROJECTILE_VISUAL_ROLE_NAME);
        proxy.setDespawnTime((float) Math.max(0.6, ((expireAtMillis - System.currentTimeMillis()) / 1000.0) + 0.5));
        world.spawnEntity(proxy, position.clone(), new Vector3f(0f, 0f, 0f));

        Ref<EntityStore> proxyRef = proxy.getReference();
        if (proxyRef == null || !proxyRef.isValid() || proxyRef.getStore() == null) {
            return ProjectileVisualRuntime.none();
        }

        return new ProjectileVisualRuntime(proxyRef, effectId, activateAtMillis);
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
        float despawnTimeSeconds = (float) Math.max(1.0, ((expireAtMillis - System.currentTimeMillis()) / 1000.0) + 0.75);
        for (Vector3d position : positions) {
            NPCEntity proxy = new NPCEntity(world);
            proxy.setRoleName(FIELD_VISUAL_ROLE_NAME);
            proxy.setDespawnTime(despawnTimeSeconds);
            world.spawnEntity(proxy, position.clone(), new Vector3f(0f, 0f, 0f));

            Ref<EntityStore> proxyRef = proxy.getReference();
            if (proxyRef != null && proxyRef.isValid() && proxyRef.getStore() != null) {
                refs.add(proxyRef);
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
        positions.add(center.clone());
        String castType = lower(ability.getCastType());
        if ("barrier".equals(castType) && lineDirection != null && lineDirection.isFinite()) {
            double span = Math.max(2.0, Math.min(Math.max(halfWidth, 0.0), 7.0));
            Vector3d normalized = normalize(lineDirection);
            for (double offset = -span; offset <= span + 0.001; offset += Math.max(2.25, span / 2.0)) {
                if (Math.abs(offset) < 0.3) {
                    continue;
                }
                positions.add(center.clone().addScaled(normalized, offset));
            }
            return positions;
        }

        if (!"ground_zone".equals(castType) && !"support_zone".equals(castType)) {
            return positions;
        }

        double radius = ability.getRadius() > 0 ? ability.getRadius() : DEFAULT_AREA_RADIUS;
        double ringRadius = Math.max(1.8, Math.min(radius * 0.62, 5.5));
        positions.add(center.clone().add(ringRadius, 0.0, 0.0));
        positions.add(center.clone().add(-ringRadius, 0.0, 0.0));
        positions.add(center.clone().add(0.0, 0.0, ringRadius));
        positions.add(center.clone().add(0.0, 0.0, -ringRadius));
        if (radius >= 4.5) {
            double diagonal = ringRadius * 0.72;
            positions.add(center.clone().add(diagonal, 0.0, diagonal));
            positions.add(center.clone().add(-diagonal, 0.0, diagonal));
            positions.add(center.clone().add(diagonal, 0.0, -diagonal));
            positions.add(center.clone().add(-diagonal, 0.0, -diagonal));
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
            direction = direction.clone();
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

        Vector3d start = currentTransform.getPosition().clone();
        Vector3d target = start.clone()
                .addScaled(horizontalDirection, horizontalDistance)
                .add(0.0, verticalDistance, 0.0);
        runtimePlayer.moveTo(playerRef, target.x, target.y, target.z, store);

        return new MovementResult(
                true,
                horizontalDistance,
                verticalDistance,
                start,
                target.clone(),
                buildMovementSummary(castType, horizontalDistance, verticalDistance)
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

            if (targetEntityId != null) {
                resolvedDamage *= resolveIncomingDamageMultiplier(targetEntityId);
                resolvedDamage = mod.getStatusEffectManager().absorbDamage(targetEntityId, resolvedDamage);
            }

            if (resolvedDamage <= 0.0) {
                applyEffectById(targetRef, store, impactEffectId);
                continue;
            }

            Damage damage = new Damage(new Damage.EntitySource(playerRef), cause, (float) resolvedDamage);
            DamageSystems.executeDamage(targetRef, store, damage);
            applyPostDamageClassPassives(player, playerRef, targetEntityId, resolvedDamage, true);
            applyEffectById(targetRef, store, impactEffectId);
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
        double sustainMultiplier = mod.getLevelingManager().getPlayerSustainMultiplier(player.getLevel());

        if (ability.getHealPercent() > 0) {
            healed = healEntity(playerRef, store, ability.getHealPercent() * sustainMultiplier);
            if (healed > 0) {
                summaryParts.add("healed " + AbilityPresentation.formatDecimal(healed));
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

                if (applyTargetToken(token, targetRef, store, playerRef, player.getPlayerId(), ability)) {
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
        if (!"line_control".equals(lower(ability.getCastType())) || ability.getPullForce() <= 0.0) {
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
        activeLineControls.add(new ActiveLineControl(
                player.getPlayerId(),
                ownerRef,
                targetRef,
                ability,
                now + (long) (durationSeconds * 1000),
                now + LINE_CONTROL_PULSE_INTERVAL_MS
        ));
        return new LineControlRuntimeResult(
                true,
                "current pull "
                        + AbilityPresentation.formatDecimal(durationSeconds)
                        + "s"
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

        applyLineControlPull(lineControl.targetRef(), store, lineControl.ownerRef(), lineControl.ability());
        applyRepeatingLineControlEffects(lineControl, player, store);
        lineControl.nextPulseAtMillis = now + LINE_CONTROL_PULSE_INTERVAL_MS;
        return false;
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
        if (effectId == null || !applyEffectById(playerRef, store, effectId)) {
            return FormRuntimeResult.none();
        }

        String modelId = HytaleAssetResolver.resolveModelId(player.getPlayerClass(), style.getId(), ability);
        if (modelId == null || modelId.isBlank()) {
            modelId = ability.getName();
        }

        Vector3d origin = getPosition(playerRef, store);
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
        summon.setRoleName(SUMMON_ROLE_NAME);
        summon.setDespawnTime((float) Math.max(2.0, ability.getDurationSeconds()));
        world.spawnEntity(summon, spawnPosition, new Vector3f(0f, 0f, 0f));

        Ref<EntityStore> summonRef = summon.getReference();
        if (summonRef == null || !summonRef.isValid()) {
            return SummonRuntimeResult.none();
        }

        NPCEntity.setAppearance(summonRef, modelId, summonRef.getStore());
        applyEffectById(summonRef, summonRef.getStore(), resolveImpactEffectId(player.getPlayerClass(), style.getId(), ability));

        long expireAt = System.currentTimeMillis() + (long) (Math.max(2.0, ability.getDurationSeconds()) * 1000);
        activeSummonsByOwner.computeIfAbsent(player.getPlayerId(), ignored -> new ArrayList<>())
                .add(createActiveSummon(player, playerRef, summonRef, player.getPlayerClass(), style.getId(), ability, expireAt));

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
        long hatchAtMillis = "hatchling".equals(role) ? now + 2000L : now;
        double baseDamage = resolveSummonBaseDamage(player, ability, role);

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
                null,
                0L,
                !"hatchling".equals(role)
        );
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
        damage *= mod.getLevelingManager().getPlayerAbilityPowerMultiplier(player.getLevel());
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
        Vector3d destination = ownerPosition.clone().addScaled(direction, -2.0);
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
        Vector3d destination = summonPosition.clone().addScaled(direction, travel);
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
        Vector3d destination = summonPosition.clone().addScaled(direction, retreat);
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
        Vector3d destination = targetPosition.clone().addScaled(approach, -1.15);
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
        form.lastOwnerPosition = origin.clone();
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
                Vector3d nearestPoint = from.clone().addScaled(segment, clampedProjection);
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
                        secondaryRiderToken
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
        ActiveTransformation form = activeTransformationsByPlayer.get(player.getPlayerId());
        boolean hasClassPassiveWeaponAttack = mod.getClassPassiveManager().hasWeaponAttackPassive(player);
        boolean hasOneShot = mod.getStatusEffectManager().hasEffect(player.getPlayerId(), StatusEffect.Type.DAMAGE_BUFF)
                || mod.getStatusEffectManager().hasEffect(player.getPlayerId(), StatusEffect.Type.STEALTH);
        boolean hasAttackBuff = mod.getStatusEffectManager().hasEffect(player.getPlayerId(), StatusEffect.Type.ATTACK_BUFF);
        if (followUp == null && form == null && !hasOneShot && !hasAttackBuff && !hasClassPassiveWeaponAttack) {
            return null;
        }

        String targetEntityId = resolveEntityId(targetRef, store);
        double modifier = 1.0
                + mod.getStatusEffectManager().getDamageIncrease(player.getPlayerId())
                + mod.getStatusEffectManager().consumeOneShot(player.getPlayerId(), StatusEffect.Type.DAMAGE_BUFF)
                + mod.getStatusEffectManager().consumeOneShot(player.getPlayerId(), StatusEffect.Type.STEALTH);
        if (form != null) {
            modifier += form.weaponBonus();
        }

        double baseDamage = ((4.0 + (player.getLevel() * 0.9))
                * mod.getLevelingManager().getPlayerAbilityPowerMultiplier(player.getLevel()))
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

    private boolean shouldArmWeaponFollowUp(AbilityData ability) {
        if (!"self_buff".equals(lower(ability.getCastType()))) {
            return false;
        }

        List<String> tokens = parseEffectTokens(ability.getEffect());
        return tokens.stream().anyMatch(token -> switch (token) {
            case "attack_buff", "damage_buff", "stealth", "lifesteal", "defense_buff", "shield", "evasion", "speed", "self_burn" -> true;
            default -> false;
        }) || ability.getShieldPercent() > 0;
    }

    private int resolveFollowUpUses(AbilityData ability, List<String> tokens) {
        return switch (lower(ability.getId())) {
            case "alloy_enhancement", "umbral_veil" -> 1;
            case "metal_coat", "lapidary", "imbue_fortitude", "absorb" -> 2;
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

    private double resolveFollowUpFlatDamageBonus(AbilityData ability, List<String> tokens) {
        double bonus = 4.0
                + (ability.getDamagePercent() * 0.20)
                + (tokens.contains("attack_buff") ? 4.0 : 0.0)
                + (tokens.contains("damage_buff") ? 7.0 : 0.0);

        return switch (lower(ability.getId())) {
            case "alloy_enhancement" -> bonus + 9.0;
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
            case "battle_cry" -> 2.5;
            case "overheat" -> 2.6;
            case "river_rapids" -> 2.8;
            case "refraction" -> 4.5;
            default -> 0.0;
        };
    }

    private double resolveFollowUpSplashDamageRatio(AbilityData ability) {
        return switch (lower(ability.getId())) {
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
            case "overheat" -> "burn";
            case "hidrosis", "smoke_form" -> "blind";
            case "battle_cry", "metal_coat", "triceratops_form" -> "knockback";
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

        if (CONE_CAST_TYPES.contains(castType)) {
            for (TargetCandidate candidate : frame.candidates()) {
                if (candidate.distance() <= frame.range() && candidate.forwardDot() >= frame.coneThreshold()) {
                    targets.add(candidate.ref());
                }
            }
            return List.copyOf(targets);
        }

        if (AREA_CAST_TYPES.contains(castType)) {
            Vector3d center = frame.areaCenter();
            for (TargetCandidate candidate : frame.candidates()) {
                if (distance(center, candidate.position()) <= frame.areaRadius()) {
                    targets.add(candidate.ref());
                }
            }
            return List.copyOf(targets);
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
            return List.copyOf(targets);
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
            return List.copyOf(targets);
        }

        if (frame.explicitTarget() != null) {
            return List.of(frame.explicitTarget());
        }

        TargetCandidate nearestForward = frame.candidates().stream()
                .filter(candidate -> candidate.forwardDot() > 0.2 && candidate.distance() <= frame.range())
                .min((left, right) -> Double.compare(left.distance(), right.distance()))
                .orElse(null);
        return nearestForward != null ? List.of(nearestForward.ref()) : List.of();
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
        double coneThreshold = ability.getConeAngle() > 0
                ? Math.cos(Math.toRadians(ability.getConeAngle() / 2.0))
                : Math.cos(Math.toRadians(35.0));

        Vector3d areaCenter = resolveAreaCenter(origin, forward, context, range);
        Ref<EntityStore> explicitTarget = resolveExplicitTarget(store, context.explicitTargetRef(), range, origin);
        List<TargetCandidate> candidates = new ArrayList<>();

        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                if (ref == null || !ref.isValid() || ref.equals(playerRef)) {
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
                                       double range) {
        if (context.targetBlock() != null) {
            Vector3i block = context.targetBlock();
            return new Vector3d(block.x + 0.5, block.y + 1.0, block.z + 0.5);
        }

        return new Vector3d(
                origin.x + (forward.x * Math.min(range, 5.0)),
                origin.y + (forward.y * Math.min(range, 5.0)),
                origin.z + (forward.z * Math.min(range, 5.0))
        );
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
                                            CastContext context) {
        Vector3d origin = getPosition(playerRef, store);
        if (origin == null) {
            return null;
        }

        if (context.explicitTargetRef() != null && context.explicitTargetRef().isValid()) {
            Vector3d targetPosition = getPosition(context.explicitTargetRef(), store);
            if (targetPosition != null) {
                return normalize(subtract(targetPosition, origin));
            }
        }

        if (context.targetBlock() != null) {
            Vector3i block = context.targetBlock();
            Vector3d targetPosition = new Vector3d(block.x + 0.5, block.y + 1.0, block.z + 0.5);
            return normalize(subtract(targetPosition, origin));
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

        Vector3d direction = transform.getTransform().getDirection().clone();
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
                case "frozen_needles", "cacti_cluster" -> 5;
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
        double speedPerSecond = ability.getProjectileSpeed() > 0
                ? ability.getProjectileSpeed()
                : DEFAULT_PROJECTILE_SPEED;
        return clamp(speedPerSecond, 6.0, MAX_PROJECTILE_SPEED) / StyleManager.TICKS_PER_SECOND;
    }

    private double resolveProjectileImpactRadius(AbilityData ability, String castType) {
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
        damage *= mod.getLevelingManager().getPlayerAbilityPowerMultiplier(player.getLevel());
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
        modifier += player.getRaceDamageIncrease().getOrDefault("all", 0.0);
        modifier += player.getSynergyDamageIncrease().getOrDefault("all", 0.0);
        modifier += mod.getClassPassiveManager().getAbilityDamageModifier(player);
        ActiveTransformation activeForm = activeTransformationsByPlayer.get(player.getPlayerId());
        if (activeForm != null) {
            modifier += activeForm.damageBonus();
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
        Vector3d destination = targetPosition.clone()
                .addScaled(direction, push)
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

        if ("knockback".equals(normalized)) {
            return isAnchorDragAbility(ability)
                    ? applyAnchorDrag(targetRef, store, sourceRef, ability)
                    : applyKnockback(targetRef, store, sourceRef, ability);
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

        Vector3d destination = targetPosition.clone()
                .addScaled(direction, dragStep)
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
        Vector3d ahead = destination.clone().addScaled(direction, probeDistance);
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

        Vector3d destination = targetPosition.clone()
                .addScaled(direction, step)
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

    private Vector3d subtract(Vector3d left, Vector3d right) {
        return new Vector3d(left.x - right.x, left.y - right.y, left.z - right.z);
    }

    private Vector3d normalize(Vector3d vector) {
        Vector3d normalized = vector.clone();
        if (normalized.length() < 0.0001) {
            return new Vector3d(0.0, 0.0, 1.0);
        }
        normalized.normalize();
        return normalized;
    }

    private double dot(Vector3d left, Vector3d right) {
        return (left.x * right.x) + (left.y * right.y) + (left.z * right.z);
    }

    private double length(Vector3d value) {
        return Math.sqrt((value.x * value.x) + (value.y * value.y) + (value.z * value.z));
    }

    private double distance(Vector3d left, Vector3d right) {
        return length(subtract(left, right));
    }

    public record CastContext(Ref<EntityStore> explicitTargetRef, Vector3i targetBlock) {
        public static CastContext empty() {
            return new CastContext(null, null);
        }
    }

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
        private final long expireAtMillis;
        private long nextPulseAtMillis;

        private ActiveLineControl(String ownerPlayerId,
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
            this.lastOwnerPosition = lastOwnerPosition != null ? lastOwnerPosition.clone() : null;
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
                                     String secondaryRiderToken) {
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
                                 long nextVisualRefreshAtMillis) {
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
                            long nextVisualRefreshAtMillis) {
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
        public long nextVisualRefreshAtMillis() { return nextVisualRefreshAtMillis; }
    }
}
