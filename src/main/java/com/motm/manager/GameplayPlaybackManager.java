package com.motm.manager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Transform;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector3i;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.CollisionResultComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.collision.BlockCollisionData;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.motm.MenteesMod;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.model.StatusEffect;
import com.motm.model.StyleData;
import com.motm.runtime.ability.AbilityExecutionPolicy;
import com.motm.runtime.ability.AbilityRuntimeMath;
import com.motm.runtime.ability.AbilityRuntimeEffects;
import com.motm.runtime.ability.AbilityStatusEffects;
import com.motm.runtime.ability.channel.ChannelActivationRuntime;
import com.motm.runtime.ability.channel.ChannelHytaleAdapter;
import com.motm.runtime.ability.channel.ChannelRuntimeState;
import com.motm.runtime.ability.combat.CombatRuntimeState;
import com.motm.runtime.ability.field.ActiveField;
import com.motm.runtime.ability.field.FieldActivationHytaleAdapter;
import com.motm.runtime.ability.field.FieldActivationRuntime;
import com.motm.runtime.ability.field.FieldOwnerMobilityHytaleAdapter;
import com.motm.runtime.ability.field.FieldPulseHytaleAdapter;
import com.motm.runtime.ability.field.FieldRuntimeSpecs;
import com.motm.runtime.ability.field.FieldSinkholeHytaleAdapter;
import com.motm.runtime.ability.field.FieldRuntimeState;
import com.motm.runtime.ability.field.FieldSupportPulseHytaleAdapter;
import com.motm.runtime.ability.field.FieldTerrainHytaleAdapter;
import com.motm.runtime.ability.field.FieldTargetHytaleAdapter;
import com.motm.runtime.ability.field.FieldTickRuntime;
import com.motm.runtime.ability.field.FieldVisualHytaleAdapter;
import com.motm.runtime.ability.field.FieldVisualRuntime;
import com.motm.runtime.ability.field.FieldOriginRuntimeState;
import com.motm.runtime.ability.followup.ActiveWeaponFollowUp;
import com.motm.runtime.ability.followup.WeaponFollowUpDurabilityRestorer;
import com.motm.runtime.ability.followup.WeaponFollowUpHytaleAdapter;
import com.motm.runtime.ability.followup.WeaponFollowUpHitMath;
import com.motm.runtime.ability.followup.WeaponFollowUpLifecycleRuntime;
import com.motm.runtime.ability.followup.WeaponFollowUpNativeAlloyRuntime;
import com.motm.runtime.ability.followup.WeaponFollowUpRuntimeState;
import com.motm.runtime.ability.followup.WeaponFollowUpSpec;
import com.motm.runtime.ability.followup.WeaponFollowUpSpecs;
import com.motm.runtime.ability.projectile.ProjectileImpactHytaleAdapter;
import com.motm.runtime.ability.projectile.ProjectileLaunchHytaleAdapter;
import com.motm.runtime.ability.projectile.ProjectileLifecycleHytaleAdapter;
import com.motm.runtime.ability.projectile.ProjectileRuntimeFacade;
import com.motm.runtime.ability.self.SelfActivationRuntime;
import com.motm.runtime.ability.self.SelfHytaleAdapter;
import com.motm.runtime.ability.self.SelfRuntimeState;
import com.motm.runtime.ability.specific.AbilitySpecificHytaleAdapter;
import com.motm.runtime.ability.stomp.ArmedStomp;
import com.motm.runtime.ability.stomp.StompRuntimeState;
import com.motm.runtime.ability.summon.ActiveSummon;
import com.motm.runtime.ability.summon.SummonActivationRuntime;
import com.motm.runtime.ability.summon.SummonAttackEffectRuntime;
import com.motm.runtime.ability.summon.SummonAttackHytaleAdapter;
import com.motm.runtime.ability.summon.SummonAttackRuntime;
import com.motm.runtime.ability.summon.SummonBuffRuntime;
import com.motm.runtime.ability.summon.SummonControlHytaleAdapter;
import com.motm.runtime.ability.summon.SummonLifecycleHytaleAdapter;
import com.motm.runtime.ability.summon.SummonMovementRuntime;
import com.motm.runtime.ability.summon.SummonRuntimeState;
import com.motm.runtime.ability.summon.SummonSplashRuntime;
import com.motm.runtime.ability.summon.SummonTargetRuntime;
import com.motm.runtime.ability.summon.SummonTickRuntime;
import com.motm.runtime.ability.terrain.LapidaryGemRuntimeState;
import com.motm.runtime.ability.terrain.LavaHazardRuntimeState;
import com.motm.runtime.ability.terrain.TerrainAbilityHytaleAdapter;
import com.motm.runtime.ability.terrain.TerrainActivationRuntime;
import com.motm.runtime.ability.terrain.TerrainGemHytaleAdapter;
import com.motm.runtime.ability.terrain.TerrainHytaleAdapter;
import com.motm.runtime.ability.terrain.TerrainPlacementHytaleAdapter;
import com.motm.runtime.ability.terrain.TerrainSinkholeMarkerHytaleAdapter;
import com.motm.runtime.ability.terrain.TerrainSupplementalHytaleAdapter;
import com.motm.runtime.ability.terrain.TerrainTickRuntime;
import com.motm.runtime.ability.terrain.TerrainRuntimeSpecs;
import com.motm.runtime.ability.terrain.TerrainRuntimeState;
import com.motm.runtime.ability.transformation.ActiveTransformation;
import com.motm.runtime.ability.transformation.TransformationEffectRuntime;
import com.motm.runtime.ability.transformation.TransformationHytaleAdapter;
import com.motm.runtime.ability.transformation.TransformationRuntimeState;
import com.motm.runtime.ability.transformation.TransformationTickRuntime;
import com.motm.runtime.state.VisualProxyRuntimeState;
import com.motm.util.AbilityPresentation;
import com.motm.util.HytaleAssetResolver;
import com.motm.util.MotmObservability;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class GameplayPlaybackManager {

    private static final Logger LOG = Logger.getLogger("MOTM");
    private static final Set<String> AREA_CAST_TYPES = Set.of(
            "ground_burst", "ground_zone", "ground_target", "ground_strike",
            "support_zone", "self_burst", "barrier", "execute");
    private static final Set<String> CONE_CAST_TYPES = Set.of("cone", "gaze");
    private static final double DEFAULT_LINE_HALF_WIDTH = 1.75;
    private static final double DEFAULT_AREA_RADIUS = FieldRuntimeSpecs.DEFAULT_AREA_RADIUS;
    private static final double DEFAULT_CHAIN_RADIUS = 4.5;
    private static final int DEFAULT_CHAIN_TARGETS = 3;
    private static final String SUMMON_ROLE_NAME = "motm_summon";
    private static final String PROJECTILE_VISUAL_ROLE_NAME = "motm_projectile";
    private static final String FIELD_VISUAL_ROLE_NAME = "motm_field";
    private static final long CHANNEL_PULSE_INTERVAL_MS = 700L;
    private static final long FORM_PULSE_INTERVAL_MS = 850L;
    private static final double DEFAULT_LIGHTNING_ARC_RADIUS = 5.5;
    private static final long SHOCKED_DAMAGE_WINDOW_MS = 3_500L;
    private static final double DEFAULT_PULL_STOP_DISTANCE = 1.25;
    private static final long LINE_CONTROL_PULSE_INTERVAL_MS = 350L;
    private static final double BLIND_DAMAGE_PENALTY = 0.22;
    private static final double DISORIENTED_DAMAGE_PENALTY = 0.12;
    private static final long STOMP_ARM_TIMEOUT_MILLIS = 30_000L;
    private static final double STOMP_JUMP_THRESHOLD_BLOCKS = 0.45;
    private static final double STOMP_LAND_TOLERANCE_BLOCKS = 0.10;

    private final MenteesMod mod;
    private final SummonRuntimeState summonState = new SummonRuntimeState();
    private final SummonActivationRuntime summonActivationRuntime = new SummonActivationRuntime();
    private final SummonLifecycleHytaleAdapter summonLifecycleAdapter;
    private final SummonControlHytaleAdapter summonControlAdapter;
    private final SummonAttackHytaleAdapter summonAttackAdapter;
    private final TransformationRuntimeState transformationState = new TransformationRuntimeState();
    private final TransformationHytaleAdapter transformationAdapter;
    private final WeaponFollowUpRuntimeState weaponFollowUps = new WeaponFollowUpRuntimeState();
    private final WeaponFollowUpLifecycleRuntime weaponFollowUpLifecycleRuntime = new WeaponFollowUpLifecycleRuntime();
    private final WeaponFollowUpHytaleAdapter weaponFollowUpAdapter;
    private final FieldOriginRuntimeState fieldOriginState = new FieldOriginRuntimeState();
    private final LavaHazardRuntimeState lavaHazardState = new LavaHazardRuntimeState();
    private final StompRuntimeState stompState = new StompRuntimeState();
    private final ProjectileRuntimeFacade projectileRuntime;
    private final FieldRuntimeState fieldState = new FieldRuntimeState();
    private final FieldActivationRuntime fieldActivationRuntime = new FieldActivationRuntime();
    private final FieldActivationHytaleAdapter fieldActivationAdapter;
    private final FieldTargetHytaleAdapter fieldTargetAdapter;
    private final FieldVisualHytaleAdapter fieldVisualAdapter;
    private final FieldPulseHytaleAdapter fieldPulseAdapter = new FieldPulseHytaleAdapter();
    private final FieldPulseHytaleAdapter.Support fieldPulseSupport;
    private final FieldSupportPulseHytaleAdapter fieldSupportPulseAdapter = new FieldSupportPulseHytaleAdapter();
    private final FieldSupportPulseHytaleAdapter.Support fieldSupportPulseSupport;
    private final FieldSinkholeHytaleAdapter fieldSinkholeAdapter;
    private final FieldSinkholeHytaleAdapter.Support fieldSinkholeSupport;
    private final FieldTerrainHytaleAdapter fieldTerrainAdapter;
    private final FieldOwnerMobilityHytaleAdapter fieldOwnerMobilityAdapter;
    private final FieldTickRuntime fieldTickRuntime = new FieldTickRuntime();
    private final TerrainRuntimeState terrainState = new TerrainRuntimeState();
    private final TerrainActivationRuntime terrainActivationRuntime = new TerrainActivationRuntime();
    private final TerrainTickRuntime terrainTickRuntime = new TerrainTickRuntime();
    private final TerrainPlacementHytaleAdapter terrainPlacementAdapter;
    private final TerrainAbilityHytaleAdapter terrainAbilityAdapter;
    private final TerrainGemHytaleAdapter terrainGemAdapter;
    private final TerrainSinkholeMarkerHytaleAdapter terrainSinkholeMarkerAdapter;
    private final TerrainSupplementalHytaleAdapter terrainSupplementalAdapter;
    private final TerrainHytaleAdapter terrainHytaleAdapter;
    private final LapidaryGemRuntimeState lapidaryGemState = new LapidaryGemRuntimeState();
    private final ChannelRuntimeState channelState = new ChannelRuntimeState();
    private final ChannelHytaleAdapter channelAdapter;
    private final SelfRuntimeState selfState = new SelfRuntimeState();
    private final SelfHytaleAdapter selfAdapter;
    private final AbilitySpecificHytaleAdapter abilitySpecificAdapter;
    private final VisualProxyRuntimeState visualProxyState = new VisualProxyRuntimeState();
    private final CombatRuntimeState combatState = new CombatRuntimeState();
    private boolean warnedGroundedFallback;

    public GameplayPlaybackManager(MenteesMod mod) {
        this.mod = mod;
        this.projectileRuntime = new ProjectileRuntimeFacade(
                visualProxyState,
                this::applyEffectById,
                (type, data) -> this.mod.recordClientIntent(type, null, data),
                LOG,
                Set.of(SUMMON_ROLE_NAME, PROJECTILE_VISUAL_ROLE_NAME, FIELD_VISUAL_ROLE_NAME),
                DEFAULT_LIGHTNING_ARC_RADIUS,
                createProjectileLaunchSupport(),
                createProjectileImpactSupport(),
                ownerPlayerId -> this.mod.getPlayerDataManager().getOnlinePlayer(ownerPlayerId),
                new ProjectileLifecycleHytaleAdapter.TraceScope() {
                    @Override
                    public String enter(String traceId) {
                        return GameplayPlaybackManager.this.mod.enterObservabilityTrace(traceId);
                    }

                    @Override
                    public void restore(String previousTraceId) {
                        GameplayPlaybackManager.this.mod.restoreObservabilityTrace(previousTraceId);
                    }
                }
        );
        this.fieldTargetAdapter = new FieldTargetHytaleAdapter(Set.of(
                SUMMON_ROLE_NAME,
                PROJECTILE_VISUAL_ROLE_NAME,
                FIELD_VISUAL_ROLE_NAME
        ), this::isTargetGrounded);
        this.summonLifecycleAdapter = new SummonLifecycleHytaleAdapter(
                summonState,
                summonActivationRuntime,
                new SummonLifecycleHytaleAdapter.Support() {
                    @Override
                    public double abilityPowerMultiplier(int playerLevel) {
                        return mod.getLevelingManager().getPlayerAbilityPowerMultiplier(playerLevel);
                    }

                    @Override
                    public boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
                        return GameplayPlaybackManager.this.applyEffectById(ref, store, effectId);
                    }

                    @Override
                    public String resolveImpactEffectId(String classId, String styleId, AbilityData ability) {
                        return AbilityRuntimeEffects.impactEffectId(classId, styleId, ability);
                    }

                    @Override
                    public void logInfo(String message) {
                        LOG.info(message);
                    }

                    @Override
                    public void logWarning(String message) {
                        LOG.warning(message);
                    }
                }
        );
        this.summonControlAdapter = new SummonControlHytaleAdapter(
                summonState,
                summonLifecycleAdapter,
                new SummonTickRuntime(),
                new SummonBuffRuntime(),
                new SummonMovementRuntime(),
                new SummonTargetRuntime(),
                new SummonControlHytaleAdapter.Support() {
                    @Override
                    public PlayerData owner(String ownerPlayerId) {
                        return mod.getPlayerDataManager().getOnlinePlayer(ownerPlayerId);
                    }

                    @Override
                    public boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
                        return GameplayPlaybackManager.this.applyEffectById(ref, store, effectId);
                    }

                    @Override
                    public String resolveImpactEffectId(String classId, String styleId, AbilityData ability) {
                        return AbilityRuntimeEffects.impactEffectId(classId, styleId, ability);
                    }

                    @Override
                    public void attack(ActiveSummon summon,
                                       PlayerData owner,
                                       Ref<EntityStore> targetRef,
                                       Store<EntityStore> store,
                                       long now) {
                        GameplayPlaybackManager.this.summonAttackAdapter.performAttack(summon, owner, targetRef, store, now);
                    }

                    @Override
                    public boolean isMotmSummon(NPCEntity npc) {
                        return GameplayPlaybackManager.this.isMotmSummon(npc);
                    }
                }
        );
        this.summonAttackAdapter = new SummonAttackHytaleAdapter(
                new SummonAttackRuntime(),
                new SummonAttackEffectRuntime(),
                new SummonSplashRuntime(),
                new SummonAttackHytaleAdapter.Support() {
                    @Override
                    public void moveCloneBesideTarget(ActiveSummon summon,
                                                      Ref<EntityStore> targetRef,
                                                      Store<EntityStore> store) {
                        summonControlAdapter.moveSummonBesideTarget(summon, targetRef, store);
                    }

                    @Override
                    public String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store) {
                        return GameplayPlaybackManager.this.resolveEntityId(ref, store);
                    }

                    @Override
                    public double incomingDamageMultiplier(String targetEntityId) {
                        return GameplayPlaybackManager.this.resolveIncomingDamageMultiplier(targetEntityId);
                    }

                    @Override
                    public double absorbDamage(String targetEntityId, double damage) {
                        return mod.getStatusEffectManager().absorbDamage(targetEntityId, damage);
                    }

                    @Override
                    public void applyPostDamageClassPassives(PlayerData owner,
                                                             Ref<EntityStore> ownerRef,
                                                             String targetEntityId,
                                                             double damage,
                                                             boolean abilityDamage) {
                        GameplayPlaybackManager.this.applyPostDamageClassPassives(
                                owner,
                                ownerRef,
                                targetEntityId,
                                damage,
                                abilityDamage
                        );
                    }

                    @Override
                    public void applyLifesteal(Ref<EntityStore> ownerRef, String ownerPlayerId, double damage) {
                        GameplayPlaybackManager.this.applyLifesteal(ownerRef, ownerPlayerId, damage);
                    }

                    @Override
                    public boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
                        return GameplayPlaybackManager.this.applyEffectById(ref, store, effectId);
                    }

                    @Override
                    public String resolveImpactEffectId(String classId, String styleId, AbilityData ability) {
                        return AbilityRuntimeEffects.impactEffectId(classId, styleId, ability);
                    }

                    @Override
                    public boolean applyTargetToken(String token,
                                                    Ref<EntityStore> targetRef,
                                                    Store<EntityStore> store,
                                                    Ref<EntityStore> sourceRef,
                                                    String sourcePlayerId,
                                                    AbilityData ability) {
                        return GameplayPlaybackManager.this.applyTargetToken(
                                token,
                                targetRef,
                                store,
                                sourceRef,
                                sourcePlayerId,
                                ability
                        );
                    }

                    @Override
                    public double applyShield(String entityId,
                                              Ref<EntityStore> entityRef,
                                              Store<EntityStore> store,
                                              AbilityData ability,
                                              double shieldPercent) {
                        return GameplayPlaybackManager.this.applyShield(entityId, entityRef, store, ability, shieldPercent);
                    }

                    @Override
                    public boolean applyPullTowardsPoint(Ref<EntityStore> targetRef,
                                                         Store<EntityStore> store,
                                                         Vector3d point,
                                                         AbilityData ability,
                                                         double pullForce,
                                                         double liftForce,
                                                         double maxY) {
                        return GameplayPlaybackManager.this.applyPullTowardsPoint(
                                targetRef,
                                store,
                                point,
                                ability,
                                pullForce,
                                liftForce,
                                maxY
                        );
                    }

                    @Override
                    public Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
                        return GameplayPlaybackManager.this.getPosition(ref, store);
                    }

                    @Override
                    public List<Ref<EntityStore>> collectNearbyNpcTargets(Store<EntityStore> store,
                                                                          Vector3d center,
                                                                          double radius,
                                                                          int maxTargets) {
                        return GameplayPlaybackManager.this.collectNearbyNpcTargets(store, center, radius, maxTargets);
                    }

                    @Override
                    public void logInfo(String message) {
                        LOG.info(message);
                    }
                }
        );
        this.transformationAdapter = new TransformationHytaleAdapter(
                transformationState,
                new TransformationTickRuntime(),
                new TransformationEffectRuntime(),
                FORM_PULSE_INTERVAL_MS,
                new TransformationHytaleAdapter.Support() {
                    @Override
                    public PlayerData player(String playerId) {
                        return mod.getPlayerDataManager().getOnlinePlayer(playerId);
                    }

                    @Override
                    public boolean isIncapacitated(String playerId) {
                        return mod.getStatusEffectManager().isIncapacitated(playerId);
                    }

                    @Override
                    public boolean hasStatusEffect(String playerId, StatusEffect.Type type) {
                        return mod.getStatusEffectManager().hasEffect(playerId, type);
                    }

                    @Override
                    public StatusEffect createStatusEffect(String token,
                                                           AbilityData ability,
                                                           String sourcePlayerId,
                                                           String sourceAbilityId) {
                        return AbilityStatusEffects.create(token, ability, sourcePlayerId, sourceAbilityId);
                    }

                    @Override
                    public void applyOwnerStatusEffect(String playerId, StatusEffect effect) {
                        mod.getStatusEffectManager().applyEffect(playerId, effect);
                    }

                    @Override
                    public boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
                        return GameplayPlaybackManager.this.applyEffectById(ref, store, effectId);
                    }

                    @Override
                    public String resolveImpactEffectId(String classId, String styleId, AbilityData ability) {
                        return AbilityRuntimeEffects.impactEffectId(classId, styleId, ability);
                    }

                    @Override
                    public String currentStyleId(PlayerData player) {
                        return GameplayPlaybackManager.this.currentStyleId(player);
                    }

                    @Override
                    public Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
                        return GameplayPlaybackManager.this.getPosition(ref, store);
                    }

                    @Override
                    public String humanize(String value) {
                        return GameplayPlaybackManager.this.humanize(value);
                    }

                    @Override
                    public Ref<EntityStore> findNearestNpc(Store<EntityStore> store, Vector3d center, double radius) {
                        return GameplayPlaybackManager.this.findNearestNpc(store, center, radius);
                    }

                    @Override
                    public Iterable<Ref<EntityStore>> collectNearbyNpcTargets(Store<EntityStore> store,
                                                                              Vector3d center,
                                                                              double radius,
                                                                              int maxTargets) {
                        return GameplayPlaybackManager.this.collectNearbyNpcTargets(store, center, radius, maxTargets);
                    }

                    @Override
                    public Iterable<Ref<EntityStore>> collectTargetsAlongSegment(Store<EntityStore> store,
                                                                                Vector3d from,
                                                                                Vector3d to,
                                                                                double radius,
                                                                                int maxTargets) {
                        return GameplayPlaybackManager.this.collectTargetsAlongSegment(store, from, to, radius, maxTargets);
                    }

                    @Override
                    public boolean applyTargetToken(String token,
                                                    Ref<EntityStore> targetRef,
                                                    Store<EntityStore> store,
                                                    Ref<EntityStore> sourceRef,
                                                    String sourcePlayerId,
                                                    AbilityData ability) {
                        return GameplayPlaybackManager.this.applyTargetToken(
                                token,
                                targetRef,
                                store,
                                sourceRef,
                                sourcePlayerId,
                                ability
                        );
                    }

                    @Override
                    public void applyKnockback(Ref<EntityStore> targetRef,
                                               Store<EntityStore> store,
                                               Ref<EntityStore> sourceRef,
                                               AbilityData ability) {
                        GameplayPlaybackManager.this.applyKnockback(targetRef, store, sourceRef, ability);
                    }

                    @Override
                    public boolean applyKnockbackCollidedWithWall(Ref<EntityStore> targetRef,
                                                                  Store<EntityStore> store,
                                                                  Ref<EntityStore> sourceRef,
                                                                  AbilityData ability) {
                        return GameplayPlaybackManager.this.applyKnockbackResult(targetRef, store, sourceRef, ability).collidedWithWall();
                    }

                    @Override
                    public double applyShield(String entityId,
                                              Ref<EntityStore> entityRef,
                                              Store<EntityStore> store,
                                              AbilityData ability,
                                              double shieldPercent) {
                        return GameplayPlaybackManager.this.applyShield(entityId, entityRef, store, ability, shieldPercent);
                    }

                    @Override
                    public double resolveDamageAmount(PlayerData player, AbilityData ability) {
                        return AbilityRuntimeMath.damageAmount(player, ability, abilityPowerMultiplier(player));
                    }

                    @Override
                    public String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store) {
                        return GameplayPlaybackManager.this.resolveEntityId(ref, store);
                    }

                    @Override
                    public double incomingDamageMultiplier(String targetEntityId) {
                        return GameplayPlaybackManager.this.resolveIncomingDamageMultiplier(targetEntityId);
                    }

                    @Override
                    public double absorbDamage(String targetEntityId, double damage) {
                        return mod.getStatusEffectManager().absorbDamage(targetEntityId, damage);
                    }

                    @Override
                    public void applyPostDamageClassPassives(PlayerData player,
                                                             Ref<EntityStore> ownerRef,
                                                             String targetEntityId,
                                                             double damage,
                                                             boolean abilityDamage) {
                        GameplayPlaybackManager.this.applyPostDamageClassPassives(
                                player,
                                ownerRef,
                                targetEntityId,
                                damage,
                                abilityDamage
                        );
                    }

                    @Override
                    public void applyLifesteal(Ref<EntityStore> playerRef, String playerId, double damageDealt) {
                        GameplayPlaybackManager.this.applyLifesteal(playerRef, playerId, damageDealt);
                    }
                }
        );
        this.selfAdapter = new SelfHytaleAdapter(
                selfState,
                new SelfActivationRuntime(),
                new SelfHytaleAdapter.Support() {
                    @Override
                    public boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
                        return GameplayPlaybackManager.this.applyEffectById(ref, store, effectId);
                    }

                    @Override
                    public Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
                        return GameplayPlaybackManager.this.getPosition(ref, store);
                    }

                    @Override
                    public String formatVector(Vector3d vector) {
                        return GameplayPlaybackManager.this.formatVector(vector);
                    }

                    @Override
                    public void logInfo(String message) {
                        LOG.info(message);
                    }

                    @Override
                    public void logFine(String message) {
                        LOG.fine(message);
                    }

                    @Override
                    public void logWarning(String message) {
                        LOG.warning(message);
                    }
                }
        );
        this.channelAdapter = new ChannelHytaleAdapter(
                channelState,
                new ChannelActivationRuntime(),
                CHANNEL_PULSE_INTERVAL_MS,
                LINE_CONTROL_PULSE_INTERVAL_MS,
                new ChannelHytaleAdapter.Support() {
                    @Override
                    public PlayerData player(String playerId) {
                        return mod.getPlayerDataManager().getOnlinePlayer(playerId);
                    }

                    @Override
                    public Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
                        return GameplayPlaybackManager.this.getPosition(ref, store);
                    }

                    @Override
                    public double range(AbilityData ability) {
                        return AbilityRuntimeMath.range(ability);
                    }

                    @Override
                    public double resolveDamageAmount(PlayerData player, AbilityData ability) {
                        return AbilityRuntimeMath.damageAmount(player, ability, abilityPowerMultiplier(player));
                    }

                    @Override
                    public double outgoingDamageMultiplier(PlayerData player) {
                        return GameplayPlaybackManager.this.resolveOutgoingDamageMultiplier(player);
                    }

                    @Override
                    public String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store) {
                        return GameplayPlaybackManager.this.resolveEntityId(ref, store);
                    }

                    @Override
                    public double incomingDamageMultiplier(String targetEntityId) {
                        return GameplayPlaybackManager.this.resolveIncomingDamageMultiplier(targetEntityId);
                    }

                    @Override
                    public double absorbDamage(String targetEntityId, double damage) {
                        return mod.getStatusEffectManager().absorbDamage(targetEntityId, damage);
                    }

                    @Override
                    public void applyPostDamageClassPassives(PlayerData player,
                                                             Ref<EntityStore> ownerRef,
                                                             String targetEntityId,
                                                             double damage,
                                                             boolean abilityDamage) {
                        GameplayPlaybackManager.this.applyPostDamageClassPassives(
                                player,
                                ownerRef,
                                targetEntityId,
                                damage,
                                abilityDamage
                        );
                    }

                    @Override
                    public void applyLifesteal(Ref<EntityStore> playerRef, String playerId, double damageDealt) {
                        GameplayPlaybackManager.this.applyLifesteal(playerRef, playerId, damageDealt);
                    }

                    @Override
                    public double healEntityFlat(Ref<EntityStore> targetRef, Store<EntityStore> store, double amount) {
                        return GameplayPlaybackManager.this.healEntityFlat(targetRef, store, amount);
                    }

                    @Override
                    public boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
                        return GameplayPlaybackManager.this.applyEffectById(ref, store, effectId);
                    }

                    @Override
                    public String resolveImpactEffectId(String classId, String styleId, AbilityData ability) {
                        return AbilityRuntimeEffects.impactEffectId(classId, styleId, ability);
                    }

                    @Override
                    public String currentStyleId(PlayerData player) {
                        return GameplayPlaybackManager.this.currentStyleId(player);
                    }

                    @Override
                    public boolean applyLineControlPull(Ref<EntityStore> targetRef,
                                                        Store<EntityStore> store,
                                                        Ref<EntityStore> ownerRef,
                                                        AbilityData ability) {
                        return GameplayPlaybackManager.this.applyLineControlPull(targetRef, store, ownerRef, ability);
                    }

                    @Override
                    public List<String> effectTokens(AbilityData ability) {
                        return GameplayPlaybackManager.this.parseEffectTokens(ability == null ? null : ability.getEffect());
                    }

                    @Override
                    public boolean shouldApplyRepeatingLineControlToken(String token) {
                        return AbilityExecutionPolicy.shouldApplyRepeatingLineControlToken(token);
                    }

                    @Override
                    public boolean applyTargetToken(String token,
                                                    Ref<EntityStore> targetRef,
                                                    Store<EntityStore> store,
                                                    Ref<EntityStore> sourceRef,
                                                    String sourcePlayerId,
                                                    AbilityData ability) {
                        return GameplayPlaybackManager.this.applyTargetToken(
                                token,
                                targetRef,
                                store,
                                sourceRef,
                                sourcePlayerId,
                                ability
                        );
                    }

                    @Override
                    public String humanize(String value) {
                        return GameplayPlaybackManager.this.humanize(value);
                    }
                }
        );
        this.weaponFollowUpAdapter = new WeaponFollowUpHytaleAdapter(
                new WeaponFollowUpNativeAlloyRuntime(),
                new WeaponFollowUpHytaleAdapter.Support() {
                    @Override
                    public boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
                        return GameplayPlaybackManager.this.applyEffectById(ref, store, effectId);
                    }

                    @Override
                    public boolean removeEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
                        return GameplayPlaybackManager.this.removeEffectById(ref, store, effectId);
                    }

                    @Override
                    public String resolveImpactEffectId(String classId, String styleId, AbilityData ability) {
                        return AbilityRuntimeEffects.impactEffectId(classId, styleId, ability);
                    }

                    @Override
                    public String currentStyleId(PlayerData player) {
                        return GameplayPlaybackManager.this.currentStyleId(player);
                    }

                    @Override
                    public boolean applyTargetToken(String token,
                                                    Ref<EntityStore> targetRef,
                                                    Store<EntityStore> store,
                                                    Ref<EntityStore> sourceRef,
                                                    String sourcePlayerId,
                                                    AbilityData ability) {
                        return GameplayPlaybackManager.this.applyTargetToken(
                                token,
                                targetRef,
                                store,
                                sourceRef,
                                sourcePlayerId,
                                ability
                        );
                    }

                    @Override
                    public double applyShield(String entityId,
                                              Ref<EntityStore> entityRef,
                                              Store<EntityStore> store,
                                              AbilityData ability,
                                              double shieldPercent) {
                        return GameplayPlaybackManager.this.applyShield(entityId, entityRef, store, ability, shieldPercent);
                    }

                    @Override
                    public double healEntityFlat(Ref<EntityStore> targetRef, Store<EntityStore> store, double amount) {
                        return GameplayPlaybackManager.this.healEntityFlat(targetRef, store, amount);
                    }

                    @Override
                    public String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store) {
                        return GameplayPlaybackManager.this.resolveEntityId(ref, store);
                    }

                    @Override
                    public double incomingDamageMultiplier(String targetEntityId) {
                        return GameplayPlaybackManager.this.resolveIncomingDamageMultiplier(targetEntityId);
                    }

                    @Override
                    public double absorbDamage(String targetEntityId, double damage) {
                        return mod.getStatusEffectManager().absorbDamage(targetEntityId, damage);
                    }

                    @Override
                    public void applyPostDamageClassPassives(PlayerData player,
                                                             Ref<EntityStore> ownerRef,
                                                             String targetEntityId,
                                                             double damage,
                                                             boolean abilityDamage) {
                        GameplayPlaybackManager.this.applyPostDamageClassPassives(
                                player,
                                ownerRef,
                                targetEntityId,
                                damage,
                                abilityDamage
                        );
                    }

                    @Override
                    public void applyLifesteal(Ref<EntityStore> playerRef, String playerId, double damageDealt) {
                        GameplayPlaybackManager.this.applyLifesteal(playerRef, playerId, damageDealt);
                    }

                    @Override
                    public Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
                        return GameplayPlaybackManager.this.getPosition(ref, store);
                    }

                    @Override
                    public Iterable<Ref<EntityStore>> collectNearbyNpcTargets(Store<EntityStore> store,
                                                                              Vector3d center,
                                                                              double radius,
                                                                              int maxTargets) {
                        return GameplayPlaybackManager.this.collectNearbyNpcTargets(store, center, radius, maxTargets);
                    }

                    @Override
                    public boolean restoreHeldItemDurability(Player runtimePlayer, String itemId) {
                        return WeaponFollowUpDurabilityRestorer.restoreHeldItemDurability(runtimePlayer, itemId, LOG);
                    }

                    @Override
                    public void removeFollowUp(String playerId) {
                        weaponFollowUps.remove(playerId);
                    }

                    @Override
                    public void logInfo(String message) {
                        LOG.info(message);
                    }
                }
        );
        this.fieldVisualAdapter = new FieldVisualHytaleAdapter(
                visualProxyState,
                this::applyEffectById
        );
        this.fieldPulseSupport = createFieldPulseSupport();
        this.fieldSupportPulseSupport = createFieldSupportPulseSupport();
        this.fieldSinkholeSupport = createFieldSinkholeSupport();
        this.fieldSinkholeAdapter = new FieldSinkholeHytaleAdapter(
                fieldState,
                fieldTargetAdapter,
                fieldSinkholeSupport
        );
        this.terrainPlacementAdapter = new TerrainPlacementHytaleAdapter(
                terrainState,
                terrainActivationRuntime,
                new TerrainPlacementHytaleAdapter.Support() {
                    @Override
                    public boolean isMotmSummon(NPCEntity npc) {
                        return GameplayPlaybackManager.this.isMotmSummon(npc);
                    }

                    @Override
                    public void recordServerTruth(String type, Map<String, Object> data) {
                        GameplayPlaybackManager.this.mod.recordServerTruth(type, null, data);
                    }

                    @Override
                    public void logInfo(String message) {
                        LOG.info(message);
                    }

                    @Override
                    public void logWarning(String message) {
                        LOG.warning(message);
                    }
                }
        );
        this.terrainGemAdapter = new TerrainGemHytaleAdapter(
                lapidaryGemState,
                terrainActivationRuntime,
                visualProxyState,
                new TerrainGemHytaleAdapter.Support() {
                    @Override
                    public double resolvePlayerMaxHealth(String playerId) {
                        return GameplayPlaybackManager.this.resolvePlayerMaxHealth(playerId);
                    }

                    @Override
                    public boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
                        return GameplayPlaybackManager.this.applyEffectById(ref, store, effectId);
                    }

                    @Override
                    public void logInfo(String message) {
                        LOG.info(message);
                    }
                }
        );
        this.terrainSinkholeMarkerAdapter = new TerrainSinkholeMarkerHytaleAdapter(
                terrainPlacementAdapter,
                LOG::info
        );
        this.terrainSupplementalAdapter = new TerrainSupplementalHytaleAdapter(
                fieldVisualAdapter,
                terrainPlacementAdapter,
                this::registerFieldRuntime
        );
        this.terrainAbilityAdapter = new TerrainAbilityHytaleAdapter(
                terrainPlacementAdapter,
                new TerrainAbilityHytaleAdapter.Support() {
                    @Override
                    public Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
                        return GameplayPlaybackManager.this.getPosition(ref, store);
                    }

                    @Override
                    public Vector3d direction(Ref<EntityStore> ref, Store<EntityStore> store) {
                        return GameplayPlaybackManager.this.getDirection(ref, store);
                    }

                    @Override
                    public boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
                        return GameplayPlaybackManager.this.applyEffectById(ref, store, effectId);
                    }

                    @Override
                    public String spawnLapidaryGemProxy(World world,
                                                        PlayerData player,
                                                        AbilityData ability,
                                                        Vector3d center,
                                                        long expireAtMillis) {
                        return terrainGemAdapter.spawnLapidaryGemProxy(
                                world,
                                player,
                                ability,
                                center,
                                expireAtMillis
                        );
                    }

                    @Override
                    public Vector3d resolveActiveLapidaryGemCenter(PlayerData player, AbilityData ability, Store<EntityStore> store) {
                        return terrainGemAdapter.resolveActiveLapidaryGemCenter(player, ability, store);
                    }
                }
        );
        this.terrainHytaleAdapter = new TerrainHytaleAdapter(
                terrainTickRuntime,
                new TerrainHytaleAdapter.Support() {
                    @Override
                    public int resolveRuntimeBlockTypeId(String... blockIds) {
                        return terrainPlacementAdapter.resolveRuntimeBlockTypeId(blockIds);
                    }

                    @Override
                    public Vector3i surfaceOverlayAnchor(Vector3d center) {
                        return terrainPlacementAdapter.surfaceOverlayAnchor(center);
                    }

                    @Override
                    public String placeTemporarySelection(World world,
                                                          String reason,
                                                          Vector3i anchor,
                                                          BlockSelection selection,
                                                          long expireAtMillis,
                                                          String summary) {
                        return terrainPlacementAdapter.placeTemporarySelection(
                                world,
                                reason,
                                anchor,
                                selection,
                                expireAtMillis,
                                summary
                        );
                    }

                    @Override
                    public void logInfo(String message) {
                        LOG.info(message);
                    }

                    @Override
                    public void logWarning(String message) {
                        LOG.warning(message);
                    }
                }
        );
        this.abilitySpecificAdapter = new AbilitySpecificHytaleAdapter(
                lavaHazardState,
                terrainPlacementAdapter,
                terrainAbilityAdapter,
                terrainHytaleAdapter,
                selfAdapter,
                new AbilitySpecificHytaleAdapter.Support() {
                    @Override
                    public Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
                        return GameplayPlaybackManager.this.getPosition(ref, store);
                    }

                    @Override
                    public boolean applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
                        return GameplayPlaybackManager.this.applyEffectById(ref, store, effectId);
                    }

                    @Override
                    public void applyRoot(String playerId, AbilityData ability, double seconds) {
                        mod.getStatusEffectManager().applyEffect(playerId, new StatusEffect(
                                StatusEffect.Type.ROOT,
                                Math.max(1, (int) Math.round(seconds * StyleManager.TICKS_PER_SECOND)),
                                0.0,
                                playerId,
                                ability == null ? null : ability.getId()
                        ));
                    }
                }
        );
        this.fieldTerrainAdapter = new FieldTerrainHytaleAdapter(
                (world, reason) -> terrainHytaleAdapter.restoreActiveTemporarySelections(terrainState, world, reason));
        this.fieldOwnerMobilityAdapter = new FieldOwnerMobilityHytaleAdapter(
                lavaHazardState,
                new FieldOwnerMobilityHytaleAdapter.Support() {
                    @Override
                    public void removeEffect(String entityId, StatusEffect.Type type) {
                        mod.getStatusEffectManager().removeEffect(entityId, type);
                    }

                    @Override
                    public void logWarning(String message) {
                        LOG.warning(message);
                    }
                }
        );
        this.fieldActivationAdapter = new FieldActivationHytaleAdapter(
                fieldActivationRuntime,
                fieldState,
                fieldVisualAdapter,
                createFieldActivationSupport()
        );
    }

    private FieldActivationHytaleAdapter.Support createFieldActivationSupport() {
        return new FieldActivationHytaleAdapter.Support() {
            @Override
            public boolean isPersistentField(AbilityData ability) {
                return FieldRuntimeSpecs.isPersistentField(ability);
            }

            @Override
            public Vector3d resolveStableIronWallOrigin(String playerId, Vector3d origin) {
                return GameplayPlaybackManager.this.resolveStableIronWallOrigin(playerId, origin);
            }

            @Override
            public Vector3d resolveStableCasterCenteredOrigin(String playerId, Vector3d origin) {
                return GameplayPlaybackManager.this.resolveStableCasterCenteredOrigin(playerId, origin);
            }

            @Override
            public Vector3d resolveActiveLapidaryGemCenter(PlayerData player, AbilityData ability, Store<EntityStore> store) {
                return terrainGemAdapter.resolveActiveLapidaryGemCenter(player, ability, store);
            }

            @Override
            public Vector3d resolveIronWallForward(Vector3d forward) {
                return GameplayPlaybackManager.this.resolveIronWallForward(forward);
            }

            @Override
            public Vector3d resolveIronWallCenter(Vector3d origin, Vector3d ironWallForward) {
                return GameplayPlaybackManager.this.resolveIronWallCenter(origin, ironWallForward);
            }

            @Override
            public Vector3d resolveAreaCenter(Vector3d origin,
                                              Vector3d forward,
                                              Ref<EntityStore> explicitTargetRef,
                                              Vector3i targetBlock,
                                              double range,
                                              AbilityData ability) {
                return GameplayPlaybackManager.this.resolveAreaCenter(
                        origin,
                        forward,
                        new CastContext(explicitTargetRef, targetBlock),
                        range,
                        ability
                );
            }

            @Override
            public double range(AbilityData ability) {
                return AbilityRuntimeMath.range(ability);
            }

            @Override
            public String placePersistentTerrainSelection(Player runtimePlayer,
                                                          AbilityData ability,
                                                          Vector3d center,
                                                          Vector3d forward,
                                                          Vector3d lineDirection,
                                                          long expireAtMillis) {
                return terrainPlacementAdapter.placePersistentTerrainSelection(
                        runtimePlayer,
                        ability,
                        center,
                        forward,
                        lineDirection,
                        expireAtMillis,
                        terrainHytaleAdapter
                );
            }

            @Override
            public int pushTargetsOverlappingIronWall(Ref<EntityStore> playerRef,
                                                      Store<EntityStore> store,
                                                      AbilityData ability,
                                                      Vector3d center,
                                                      Vector3d forward,
                                                      Vector3d lineDirection) {
                return terrainPlacementAdapter.pushTargetsOverlappingIronWall(
                        playerRef,
                        store,
                        ability,
                        center,
                        forward,
                        lineDirection
                );
            }

            @Override
            public String resolveFieldVisualEffectId(String classId, String styleId, AbilityData ability) {
                return AbilityRuntimeEffects.fieldVisualEffectId(classId, styleId, ability);
            }

            @Override
            public String traceId() {
                return mod.currentObservabilityTraceId();
            }

            @Override
            public double pullStep(AbilityData ability) {
                return AbilityRuntimeMath.pullStep(ability, 0.55, 0.75);
            }
        };
    }

    private FieldSinkholeHytaleAdapter.Support createFieldSinkholeSupport() {
        return new FieldSinkholeHytaleAdapter.Support() {
            @Override
            public boolean applyEffect(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
                return applyEffectById(ref, store, effectId);
            }

            @Override
            public boolean applyTargetToken(String token,
                                            Ref<EntityStore> targetRef,
                                            Store<EntityStore> store,
                                            Ref<EntityStore> sourceRef,
                                            String sourcePlayerId,
                                            AbilityData ability) {
                return GameplayPlaybackManager.this.applyTargetToken(
                        token,
                        targetRef,
                        store,
                        sourceRef,
                        sourcePlayerId,
                        ability
                );
            }

            @Override
            public String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store) {
                return GameplayPlaybackManager.this.resolveEntityId(ref, store);
            }

            @Override
            public void placeSurfaceMarker(ActiveField field, long durationMillis) {
                terrainSinkholeMarkerAdapter.placeSinkholeSurfaceMarker(field, durationMillis);
            }

            @Override
            public void logInfo(String message) {
                LOG.info(message);
            }

            @Override
            public void logFine(String message) {
                LOG.fine(message);
            }

            @Override
            public void logWarning(String message) {
                LOG.warning(message);
            }
        };
    }

    private FieldSupportPulseHytaleAdapter.Support createFieldSupportPulseSupport() {
        return new FieldSupportPulseHytaleAdapter.Support() {
            @Override
            public double sustainMultiplier(PlayerData player) {
                return mod.getLevelingManager().getPlayerSustainMultiplier(player.getLevel());
            }

            @Override
            public double heal(Ref<EntityStore> entityRef, Store<EntityStore> store, double healPercent) {
                return healEntity(entityRef, store, healPercent);
            }

            @Override
            public double applyShield(String entityId,
                                      Ref<EntityStore> entityRef,
                                      Store<EntityStore> store,
                                      AbilityData ability,
                                      double shieldPercent) {
                return GameplayPlaybackManager.this.applyShield(entityId, entityRef, store, ability, shieldPercent);
            }

            @Override
            public int clearNegativeEffects(String entityId) {
                return GameplayPlaybackManager.this.clearNegativeEffects(entityId);
            }

            @Override
            public boolean isCasterEffectToken(String token) {
                return AbilityExecutionPolicy.isCasterEffectToken(token);
            }

            @Override
            public void applyOwnerStatusToken(String token, ActiveField field, PlayerData player) {
                GameplayPlaybackManager.this.applyOwnerStatusToken(token, player, field.ability());
            }
        };
    }

    private FieldPulseHytaleAdapter.Support createFieldPulseSupport() {
        return new FieldPulseHytaleAdapter.Support() {
            @Override
            public double pulseDamage(PlayerData player, AbilityData ability) {
                return AbilityRuntimeMath.fieldPulseDamage(player, ability, abilityPowerMultiplier(player));
            }

            @Override
            public String resolveImpactEffectId(String classId, String styleId, AbilityData ability) {
                return AbilityRuntimeEffects.impactEffectId(classId, styleId, ability);
            }

            @Override
            public String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store) {
                return GameplayPlaybackManager.this.resolveEntityId(ref, store);
            }

            @Override
            public double outgoingDamageMultiplier(PlayerData player) {
                return resolveOutgoingDamageMultiplier(player);
            }

            @Override
            public double incomingDamageMultiplier(String entityId) {
                return resolveIncomingDamageMultiplier(entityId);
            }

            @Override
            public double absorbDamage(String entityId, double damage) {
                return mod.getStatusEffectManager().absorbDamage(entityId, damage);
            }

            @Override
            public void applyPostDamageClassPassives(PlayerData player,
                                                     Ref<EntityStore> sourceRef,
                                                     String targetEntityId,
                                                     double damage,
                                                     boolean abilityDamage) {
                GameplayPlaybackManager.this.applyPostDamageClassPassives(
                        player,
                        sourceRef,
                        targetEntityId,
                        damage,
                        abilityDamage
                );
            }

            @Override
            public boolean applyEffect(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
                return applyEffectById(ref, store, effectId);
            }

            @Override
            public void applyLifesteal(Ref<EntityStore> ownerRef, String ownerPlayerId, double damage) {
                GameplayPlaybackManager.this.applyLifesteal(ownerRef, ownerPlayerId, damage);
            }

            @Override
            public boolean isTargetEffectToken(String token) {
                return AbilityExecutionPolicy.isTargetEffectToken(token);
            }

            @Override
            public boolean applyTargetToken(String token,
                                            Ref<EntityStore> targetRef,
                                            Store<EntityStore> store,
                                            Ref<EntityStore> sourceRef,
                                            String sourcePlayerId,
                                            AbilityData ability) {
                return GameplayPlaybackManager.this.applyTargetToken(
                        token,
                        targetRef,
                        store,
                        sourceRef,
                        sourcePlayerId,
                        ability
                );
            }

            @Override
            public boolean applyFieldPull(Ref<EntityStore> targetRef, Store<EntityStore> store, ActiveField field) {
                return GameplayPlaybackManager.this.applyFieldPull(targetRef, store, field);
            }

            @Override
            public boolean applyBarrierRepulsion(Ref<EntityStore> targetRef,
                                                 Store<EntityStore> store,
                                                 ActiveField field) {
                return GameplayPlaybackManager.this.applyBarrierRepulsion(targetRef, store, field);
            }

            @Override
            public void logInfo(String message) {
                LOG.info(message);
            }
        };
    }

    private ProjectileLaunchHytaleAdapter.Support createProjectileLaunchSupport() {
        return new ProjectileLaunchHytaleAdapter.Support() {
            @Override
            public double range(AbilityData ability) {
                return AbilityRuntimeMath.range(ability);
            }

            @Override
            public double damage(PlayerData player, AbilityData ability) {
                return AbilityRuntimeMath.damageAmount(player, ability, abilityPowerMultiplier(player));
            }

            @Override
            public String traceId() {
                return mod.currentObservabilityTraceId();
            }

            @Override
            public String visualEffectId(String classId, String styleId, AbilityData ability) {
                return AbilityRuntimeEffects.projectileVisualEffectId(classId, styleId, ability);
            }

            @Override
            public double ticksPerSecond() {
                return StyleManager.TICKS_PER_SECOND;
            }

            @Override
            public String lower(String value) {
                return GameplayPlaybackManager.this.lower(value);
            }
        };
    }

    private ProjectileImpactHytaleAdapter.Support createProjectileImpactSupport() {
        return new ProjectileImpactHytaleAdapter.Support() {
            @Override
            public String resolveImpactEffectId(String classId, String styleId, AbilityData ability) {
                return AbilityRuntimeEffects.impactEffectId(classId, styleId, ability);
            }

            @Override
            public double outgoingDamageMultiplier(PlayerData player) {
                return resolveOutgoingDamageMultiplier(player);
            }

            @Override
            public double applySpecialDamageModifiers(PlayerData player,
                                                      AbilityData ability,
                                                      Ref<EntityStore> targetRef,
                                                      Store<EntityStore> store,
                                                      String targetEntityId,
                                                      double damage) {
                return GameplayPlaybackManager.this.applySpecialDamageModifiers(
                        player,
                        ability,
                        targetRef,
                        store,
                        targetEntityId,
                        damage
                );
            }

            @Override
            public double incomingDamageMultiplier(String targetEntityId) {
                return resolveIncomingDamageMultiplier(targetEntityId);
            }

            @Override
            public double absorbDamage(String targetEntityId, double damage) {
                return mod.getStatusEffectManager().absorbDamage(targetEntityId, damage);
            }

            @Override
            public void reportAbilityKillIfDead(String ownerPlayerId,
                                                PlayerData player,
                                                Ref<EntityStore> targetRef,
                                                Store<EntityStore> store,
                                                String targetEntityId) {
                GameplayPlaybackManager.this.reportAbilityKillIfDead(ownerPlayerId, player, targetRef, store, targetEntityId);
            }

            @Override
            public void applyPostDamageClassPassives(PlayerData player,
                                                     Ref<EntityStore> ownerRef,
                                                     String targetEntityId,
                                                     double damage,
                                                     boolean abilityDamage) {
                GameplayPlaybackManager.this.applyPostDamageClassPassives(player, ownerRef, targetEntityId, damage, abilityDamage);
            }

            @Override
            public boolean applyEffect(Ref<EntityStore> targetRef, Store<EntityStore> store, String effectId) {
                return applyEffectById(targetRef, store, effectId);
            }

            @Override
            public void applyLifesteal(Ref<EntityStore> ownerRef, String ownerPlayerId, double damage) {
                GameplayPlaybackManager.this.applyLifesteal(ownerRef, ownerPlayerId, damage);
            }

            @Override
            public boolean applyTargetToken(String token,
                                            Ref<EntityStore> targetRef,
                                            Store<EntityStore> store,
                                            Ref<EntityStore> sourceRef,
                                            String sourcePlayerId,
                                            AbilityData ability) {
                return GameplayPlaybackManager.this.applyTargetToken(token, targetRef, store, sourceRef, sourcePlayerId, ability);
            }

            @Override
            public String resolveEntityId(Ref<EntityStore> ref, Store<EntityStore> store) {
                return GameplayPlaybackManager.this.resolveEntityId(ref, store);
            }

            @Override
            public double targetSequenceDamageMultiplier(AbilityData ability, String castType, int hitIndex) {
                return AbilityRuntimeMath.targetSequenceDamageMultiplier(ability, castType, hitIndex);
            }

            @Override
            public boolean isTargetEffectToken(String token) {
                return AbilityExecutionPolicy.isTargetEffectToken(token);
            }

            @Override
            public void logInfo(String message) {
                LOG.info(message);
            }
        };
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
                && AbilityExecutionPolicy.isGroundRestricted(ability)) {
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
        ProjectileLaunchHytaleAdapter.Result projectileLaunch = projectileRuntime.launch(
                runtimePlayer,
                player,
                style,
                ability,
                context.explicitTargetRef(),
                context.targetBlock()
        );
        FieldActivationHytaleAdapter.Result fieldRuntime = fieldActivationAdapter.activatePersistentField(
                runtimePlayer,
                player,
                style,
                ability,
                context == null ? null : context.explicitTargetRef(),
                context == null ? null : context.targetBlock()
        );
        TerrainSupplementalHytaleAdapter.Result supplementalTerrain = terrainSupplementalAdapter.activate(
                runtimePlayer,
                player,
                style,
                ability,
                playback.movementApplied(),
                playback.startPosition(),
                playback.endPosition());
        AbilitySpecificHytaleAdapter.Result specificRuntime = abilitySpecificAdapter.apply(
                runtimePlayer,
                player,
                ability,
                context == null ? null : context.explicitTargetRef(),
                context == null ? null : context.targetBlock(),
                playback.movementApplied()
        );
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

        logTerraAbilityEvent("cast.end", player, style, ability,
                "summary=" + summary
                        + " combatTargets=" + combat.targetsHit()
                        + " projectiles=" + projectileLaunch.launched()
                        + " field=" + fieldRuntime.activated()
                        + " terrain=" + supplementalTerrain.activated()
                        + " summons=" + summons.spawned()
                        + " form=" + form.applied());
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
                "summonsSpawned", summons.spawned(),
                "summonsBuffed", summons.buffed(),
                "formApplied", form.applied()
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
        stompState.arm(player.getPlayerId(), new ArmedStomp(
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
        String effectId = AbilityRuntimeEffects.castEffectId(player.getPlayerClass(), currentStyleId(player), ability);
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
        long now = System.currentTimeMillis();
        projectileRuntime.processForStore(currentStore, now);
        fieldState.removeProcessedFields(field ->
                belongsToCurrentStore(field.ownerRef(), currentStore) && processFieldTick(field, now));
        selfAdapter.processForStore(currentStore, now);
        terrainGemAdapter.processForStore(currentStore, now);
        terrainState.removeProcessedStackingColumns(column -> terrainHytaleAdapter.processStackingColumn(column, currentStore, now));
        terrainState.removeProcessedSelections(selection -> terrainHytaleAdapter.processTemporarySelection(selection, currentStore, now));
        terrainState.removeProcessedMovingTrails(trail -> terrainHytaleAdapter.processMovingTrail(trail, currentStore, now));
        channelAdapter.processForStore(currentStore, now);
        transformationAdapter.processForStore(currentStore, now);
        for (Map.Entry<String, ActiveWeaponFollowUp> entry : weaponFollowUps.entries()) {
            if (processWeaponFollowUpExpiry(entry.getKey(), entry.getValue(), currentStore, now)) {
                weaponFollowUps.remove(entry.getKey());
            }
        }
        summonControlAdapter.processForStore(currentStore, now);
    }

    public synchronized void tickArmedStomps(Store<EntityStore> currentStore) {
        if (currentStore == null || stompState.isEmpty()) {
            return;
        }

        long now = System.currentTimeMillis();
        for (ArmedStomp armed : stompState.armedStomps()) {
            String playerId = armed.playerId();
            if (armed.expired(now)) {
                LOG.info("[MOTM] Stomp arm expired: player=" + armed.player().getPlayerName());
                stompState.remove(playerId, armed);
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
                stompState.remove(playerId, armed);
                continue;
            }

            stompState.replace(playerId, armed, armed.withObservation(y, nowAirborne));
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

        String effectId = "MOTM_Terra_Quake_Impact";
        float despawnSeconds = 1.0f;
        int spawned = 0;
        String roleId = HytaleAssetResolver.resolveFieldRoleId("terra", "quake", ability);
        for (Vector3d position : positions) {
            NPCEntity proxy = new NPCEntity(world);
            proxy.setRoleName(roleId);
            proxy.setDespawnTime(despawnSeconds);
            world.spawnEntity(proxy, new Vector3d(position), new com.hypixel.hytale.math.vector.Rotation3f(0f, 0f, 0f));

            Ref<EntityStore> proxyRef = proxy.getReference();
            if (proxyRef != null && proxyRef.isValid() && proxyRef.getStore() != null) {
                visualProxyState.add(proxyRef);
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
            stompState.remove(playerId);
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

        int armed = stompState.remove(playerId) != null ? 1 : 0;
        int projectiles = projectileRuntime.removeForPlayer(playerId);
        int fields = removeFieldsForPlayer(playerId, currentStore);
        int anchors = removePlayerAnchorsForPlayer(playerId);
        int selfEffects = removeSelfEffectsForPlayer(playerId);
        int gems = removeLapidaryGemsForPlayer(playerId);
        int columns = removeStackingColumnsForWorld(currentWorld);
        int trails = removeMovingTerrainTrailsForWorld(currentWorld);
        int channels = removeChannelsForPlayer(playerId);
        int lineControls = removeLineControlsForPlayer(playerId);
        int transformations = transformationState.removeTransformation(playerId) != null ? 1 : 0;
        int followUps = weaponFollowUps.remove(playerId) != null ? 1 : 0;
        int summons = removeSummonsForPlayer(playerId);
        int terrain = terrainHytaleAdapter.restoreSelectionsForWorld(terrainState, currentWorld, "review reset");
        int proxies = despawnVisualProxiesForStore(currentStore);

        Ref<EntityStore> runtimeRef = runtimePlayer != null ? runtimePlayer.getReference() : null;
        Store<EntityStore> runtimeStore = currentStore != null
                ? currentStore
                : runtimeRef != null && runtimeRef.isValid() ? runtimeRef.getStore() : null;
        fieldOwnerMobilityAdapter.clearLavaPoolOwnerVelocityBoost(playerId, runtimeRef, runtimeStore);
        lavaHazardState.clearPlayer(playerId);
        fieldOriginState.clearCasterCenteredOrigin(playerId);

        String summary = "armed=" + armed
                + " projectiles=" + projectiles
                + " fields=" + fields
                + " anchors=" + anchors
                + " selfEffects=" + selfEffects
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
        snapshot.put("activeProjectiles", projectileRuntime.activeProjectileCount());
        snapshot.put("activeFields", fieldState.activeFieldCount());
        snapshot.put("activeTerrainSelections", terrainState.activeSelectionCount());
        snapshot.put("activeMovingTerrainTrails", terrainState.movingTrailCount());
        snapshot.put("activeStackingColumns", terrainState.stackingColumnCount());
        snapshot.put("activeLapidaryGems", terrainGemAdapter.activeGemCount());
        snapshot.put("activeChannels", channelState.activeChannelCount());
        snapshot.put("activeLineControls", channelState.activeLineControlCount());
        snapshot.put("activePlayerAnchors", selfState.activePlayerAnchorCount());
        snapshot.put("activeSelfEffects", selfState.activeSelfEffectCount());
        snapshot.put("visualProxyRefs", visualProxyState.size());
        snapshot.put("activeTransformations", transformationState.activeTransformationCount());
        snapshot.put("activeWeaponFollowUps", weaponFollowUps.size());
        snapshot.put("activeSummonOwners", summonState.activeOwnerCount());
        snapshot.put("activeSummons", summonState.activeSummonCount());
        if (playerId != null && !playerId.isBlank()) {
            snapshot.put("player", MotmObservability.mapOf(
                    "armedStomp", stompState.contains(playerId),
                    "activeTransformation", transformationState.containsTransformation(playerId),
                    "activeWeaponFollowUp", weaponFollowUps.contains(playerId),
                    "activeSummons", summonState.summonCountForOwner(playerId),
                    "lavaPoolMovementBoosted", lavaHazardState.isMovementBoosted(playerId),
                    "magmaHazardProtectionUntil", lavaHazardState.protectionUntil(playerId)
            ));
        }
        return snapshot;
    }

    private int removeFieldsForPlayer(String playerId, Store<EntityStore> currentStore) {
        return fieldState.removeFieldsForPlayer(playerId, field -> {
            fieldSinkholeAdapter.release(field);
            fieldVisualAdapter.despawn(field);
        });
    }

    private int removePlayerAnchorsForPlayer(String playerId) {
        return selfState.removePlayerAnchorsForPlayer(playerId);
    }

    private int removeSelfEffectsForPlayer(String playerId) {
        return selfState.removeSelfEffectsForPlayer(playerId);
    }

    private int removeLapidaryGemsForPlayer(String playerId) {
        return terrainGemAdapter.removeGemsForPlayer(playerId);
    }

    private int removeStackingColumnsForWorld(World world) {
        return terrainState.removeStackingColumnsForWorld(world);
    }

    private int removeMovingTerrainTrailsForWorld(World world) {
        return terrainState.removeMovingTrailsForWorld(world);
    }

    private int removeChannelsForPlayer(String playerId) {
        return channelState.removeChannelsForPlayer(playerId);
    }

    private int removeLineControlsForPlayer(String playerId) {
        return channelState.removeLineControlsForPlayer(playerId);
    }

    private int removeSummonsForPlayer(String playerId) {
        return summonLifecycleAdapter.removeSummonsForPlayer(playerId);
    }

    private int despawnVisualProxiesForStore(Store<EntityStore> currentStore) {
        return visualProxyState.despawnForStore(currentStore);
    }

    private boolean sameWorld(World left, World right) {
        return left != null && right != null && (left == right || left.equals(right));
    }


    public synchronized String forceArmedStompLanding(String playerId, Player runtimePlayer) {
        if (playerId == null || playerId.isBlank() || runtimePlayer == null) {
            return "[MOTM] Dev forced Stomp landing failed: runtime player unavailable.";
        }

        ArmedStomp armed = stompState.get(playerId);
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
        stompState.remove(playerId, armed);
        return "[MOTM] Dev forced Stomp landing resolved at " + formatVector(landingPosition) + ".";
    }

    public synchronized boolean isMagmaHazardProtected(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return false;
        }
        return lavaHazardState.isProtected(playerId, System.currentTimeMillis());
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

        int removedFields = fieldState.removeFieldsForAbility(playerId, normalizedAbilityId, field -> {
            Store<EntityStore> fieldStore = field.ownerRef() != null && field.ownerRef().isValid() ? field.ownerRef().getStore() : null;
            fieldOwnerMobilityAdapter.clearLavaPoolOwnerVelocityBoost(field.ownerPlayerId(), field.ownerRef(), fieldStore);
            fieldTerrainAdapter.restoreTemporaryTerrain(field, fieldStore);
            fieldSinkholeAdapter.release(field);
            fieldVisualAdapter.despawn(field);
        });
        if (removedFields > 0) {
            summaryParts.add("dismissed " + removedFields + " field" + (removedFields == 1 ? "" : "s"));
        }

        int removedChannels = channelState.removeChannelsForAbility(playerId, normalizedAbilityId);
        if (removedChannels > 0) {
            summaryParts.add("ended " + removedChannels + " channel" + (removedChannels == 1 ? "" : "s"));
        }

        int removedLineControls = channelState.removeLineControlsForAbility(playerId, normalizedAbilityId);
        if (removedLineControls > 0) {
            summaryParts.add("released " + removedLineControls + " control effect" + (removedLineControls == 1 ? "" : "s"));
        }

        ActiveTransformation transformation = transformationState.removeTransformationForAbility(playerId, normalizedAbilityId);
        if (transformation != null) {
            summaryParts.add("ended " + humanize(transformation.modelId()));
        }

        ActiveWeaponFollowUp followUp = weaponFollowUps.get(playerId);
        if (followUp != null && normalizedAbilityId.equals(lower(followUp.sourceAbilityId()))) {
            weaponFollowUps.remove(playerId);
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

        String effectId = AbilityExecutionPolicy.suppressGenericCasterVisual(ability)
                ? null
                : AbilityRuntimeEffects.castEffectId(player.getPlayerClass(), currentStyleId(player), ability);
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

    private boolean applyOwnerStatusToken(String token,
                                          PlayerData player,
                                          AbilityData ability) {
        if (token == null || token.isBlank() || player == null || ability == null || player.getPlayerId() == null) {
            return false;
        }

        StatusEffect effect = AbilityStatusEffects.create(token, ability, player.getPlayerId(), ability.getId());
        if (effect == null) {
            return false;
        }

        mod.getStatusEffectManager().applyEffect(player.getPlayerId(), effect);
        return true;
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

        FieldOriginRuntimeState.StableOrigin stableOrigin = fieldOriginState.resolveCasterCenteredOrigin(
                playerId,
                origin,
                System.currentTimeMillis());
        if (stableOrigin.reusedPrevious()) {
            LOG.warning("[MOTM] Caster-centered ability ignored implausible player-position jump: playerId=" + playerId
                    + " previous=" + formatVector(stableOrigin.origin())
                    + " current=" + formatVector(stableOrigin.rejectedOrigin()));
        }
        return stableOrigin.origin();
    }

    private Vector3d resolveStableIronWallOrigin(String playerId, Vector3d origin) {
        if (playerId == null || playerId.isBlank() || origin == null) {
            return origin;
        }

        FieldOriginRuntimeState.StableOrigin stableOrigin = fieldOriginState.resolveIronWallOrigin(
                playerId,
                origin,
                System.currentTimeMillis());
        if (stableOrigin.reusedPrevious()) {
            LOG.warning("[MOTM] Iron Wall ignored implausible player-position jump: playerId=" + playerId
                    + " previous=" + formatVector(stableOrigin.origin())
                    + " current=" + formatVector(stableOrigin.rejectedOrigin()));
        }
        return stableOrigin.origin();
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
        FieldActivationRuntime.Result activation = fieldActivationRuntime.activate(
                ownerPlayerId,
                ownerRef,
                classId,
                styleId,
                ability,
                ability == null ? "" : ability.getCastType(),
                center,
                forwardDirection,
                lineDirection,
                radius,
                halfWidth,
                thickness,
                activateAtMillis,
                Math.max(0L, expireAtMillis - activateAtMillis),
                followOwner,
                visual,
                mod.currentObservabilityTraceId(),
                "",
                0,
                0L,
                ability == null ? 0.0 : AbilityRuntimeMath.pullStep(ability, 0.55, 0.75)
        );
        fieldState.addFields(activation.fields());
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

    private boolean processFieldTick(ActiveField field, long now) {
        String previousTraceId = mod.enterObservabilityTrace(field.traceId());
        try {
            return fieldTickRuntime.process(field, now, new FieldTickRuntime.Hooks() {
                @Override
                public boolean hasOwnerStore(ActiveField field) {
                    return field.ownerRef() != null && field.ownerRef().getStore() != null;
                }

                @Override
                public void releaseSinkhole(ActiveField field) {
                    fieldSinkholeAdapter.release(field);
                }

                @Override
                public void despawnVisual(ActiveField field) {
                    fieldVisualAdapter.despawn(field);
                }

                @Override
                public void syncFollowOwnerAnchor(ActiveField field) {
                    fieldOwnerMobilityAdapter.syncFollowOwnerAnchor(field, field.ownerRef().getStore());
                }

                @Override
                public void clearOwnerMobility(ActiveField field) {
                    fieldOwnerMobilityAdapter.clearLavaPoolOwnerVelocityBoost(
                            field.ownerPlayerId(),
                            field.ownerRef(),
                            field.ownerRef().getStore()
                    );
                }

                @Override
                public void restoreTemporaryTerrain(ActiveField field) {
                    fieldTerrainAdapter.restoreTemporaryTerrain(field, field.ownerRef().getStore());
                }

                @Override
                public void applyOwnerMobility(ActiveField field) {
                    fieldOwnerMobilityAdapter.applyLavaPoolOwnerMobility(field, field.ownerRef().getStore());
                }

                @Override
                public void refreshVisual(ActiveField field, long now) {
                    fieldVisualAdapter.refresh(field, now);
                }

                @Override
                public boolean isSinkhole(AbilityData ability) {
                    return FieldSinkholeHytaleAdapter.isSinkhole(ability);
                }

                @Override
                public void engageSinkhole(ActiveField field) {
                    fieldSinkholeAdapter.engage(field, field.ownerRef().getStore());
                }

                @Override
                public void syncVisual(ActiveField field, long now) {
                    fieldVisualAdapter.sync(field, now);
                }

                @Override
                public PlayerData player(String ownerPlayerId) {
                    return mod.getPlayerDataManager().getOnlinePlayer(ownerPlayerId);
                }

                @Override
                public List<Ref<EntityStore>> collectTargets(ActiveField field) {
                    return fieldTargetAdapter.collectTargets(field, field.ownerRef().getStore());
                }

                @Override
                public void applyPulse(ActiveField field, PlayerData player, List<Ref<EntityStore>> targets) {
                    fieldPulseAdapter.applyPulse(field, player, field.ownerRef().getStore(), targets, fieldPulseSupport);
                }

                @Override
                public void applySupportPulse(ActiveField field, PlayerData player) {
                    fieldSupportPulseAdapter.applySupportPulse(field, player, fieldSupportPulseSupport);
                }

                @Override
                public void applySinkholeSuffocationPulse(ActiveField field) {
                    fieldSinkholeAdapter.applySuffocationPulse(field, field.ownerRef().getStore());
                }
            });
        } finally {
            mod.restoreObservabilityTrace(previousTraceId);
        }
    }

    private Ref<EntityStore> findNearestNpc(Store<EntityStore> store, Vector3d center, double radius) {
        AtomicReference<Ref<EntityStore>> nearest = new AtomicReference<>();
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
                    nearest.set(ref);
                }
            }
        });

        return nearest.get();
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
        Vector3d destination = com.motm.util.MotmVectors.addScaled(targetPosition, field.forwardDirection(), pushSign * 1.8)
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
                AbilityRuntimeMath.fieldPullLift(field != null ? field.ability() : null)
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

    private List<Vector3d> buildAreaVisualPositions(Vector3d center, AbilityData ability) {
        if (center == null || ability == null) {
            return List.of();
        }

        List<Vector3d> positions = new ArrayList<>();
        positions.add(new Vector3d(center));
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
        if (!AbilityExecutionPolicy.isMovementCastType(castType)) {
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

        double horizontalDistance = AbilityRuntimeMath.horizontalMovement(ability, castType);
        double verticalDistance = AbilityRuntimeMath.verticalMovement(ability, castType);
        String playerId = resolveEntityId(playerRef, store);
        if (playerId != null) {
            double speedBonus = mod.getStatusEffectManager().getSpeedBonus(playerId);
            if (speedBonus > 0.0) {
                horizontalDistance *= (1.0 + speedBonus);
            }

            ActiveTransformation activeForm = transformationState.getTransformation(playerId);
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
        Vector3d target = com.motm.util.MotmVectors.addScaled(start, horizontalDirection, horizontalDistance)
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

    private CombatResolution applyCombat(Player runtimePlayer,
                                         PlayerData player,
                                         AbilityData ability,
                                         CastContext context) {
        double baseDamage = AbilityRuntimeMath.damageAmount(player, ability, abilityPowerMultiplier(player));
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

        DamageCause cause = AbilityExecutionPolicy.directDamageCause(ability);
        String impactEffectId = AbilityRuntimeEffects.impactEffectId(player.getPlayerClass(), currentStyleId(player), ability);
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
            resolvedDamage *= AbilityRuntimeMath.targetSequenceDamageMultiplier(ability, castType, hitIndex);
            resolvedDamage = applySpecialDamageModifiers(player, ability, targetRef, store, targetEntityId, resolvedDamage);
            applyTempestImpactEffects(ability, targetRef, store, playerRef, player.getPlayerId(), targetEntityId);

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
            reportAbilityKillIfDead(player.getPlayerId(), player, targetRef, store, targetEntityId);
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
        double sustainMultiplier = mod.getLevelingManager().getPlayerSustainMultiplier(player.getLevel());

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
            if (!AbilityExecutionPolicy.shouldApplyCasterEffectToken(ability, token)) {
                continue;
            }

            StatusEffect effect = AbilityStatusEffects.create(token, ability, player.getPlayerId(), ability.getId());
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

        List<String> targetTokens = AbilityExecutionPolicy.targetEffectTokens(ability, tokens);
        if (targetTokens.isEmpty() && !appliesPull && !chainLightning) {
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

            for (String token : targetTokens) {
                if (applyTargetToken(token, targetRef, store, playerRef, player.getPlayerId(), ability)) {
                    appliedCount++;
                    affectedEntities.add(entityId);
                    appliedTokens.add(token);
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
        Ref<EntityStore> ownerRef = runtimePlayer == null ? null : runtimePlayer.getReference();
        Store<EntityStore> store = ownerRef != null && ownerRef.isValid() ? ownerRef.getStore() : null;
        Ref<EntityStore> targetRef = store == null
                ? null
                : resolveTargets(ownerRef, store, ability, context).stream().findFirst().orElse(null);
        ChannelHytaleAdapter.Result result = channelAdapter.startLineControl(ownerRef, player, ability, targetRef);
        if (!result.started()) {
            return LineControlRuntimeResult.none();
        }
        return new LineControlRuntimeResult(true, result.summary());
    }

    private FormRuntimeResult applyTransformation(Player runtimePlayer,
                                                  PlayerData player,
                                                  StyleData style,
                                                  AbilityData ability) {
        TransformationHytaleAdapter.ActivationResult result = transformationAdapter.activate(runtimePlayer, player, style, ability);
        return result.activated()
                ? new FormRuntimeResult(true, result.summary())
                : FormRuntimeResult.none();
    }

    private SummonRuntimeResult handleSummonRuntime(Player runtimePlayer,
                                                    PlayerData player,
                                                    StyleData style,
                                                    AbilityData ability,
                                                    CastContext context) {
        String castType = lower(ability.getCastType());
        if ("summon_buff".equals(castType)) {
            SummonControlHytaleAdapter.BuffResult result = summonControlAdapter.buffOwnedSummons(
                    runtimePlayer != null ? runtimePlayer.getReference() : null,
                    player,
                    ability
            );
            return new SummonRuntimeResult(0, result.buffed(), result.summary());
        }

        if (ability.getSummonName() == null || ability.getSummonName().isBlank()) {
            return SummonRuntimeResult.none();
        }

        SummonLifecycleHytaleAdapter.Result spawned = summonLifecycleAdapter.spawnSummon(
                runtimePlayer,
                player,
                style,
                ability,
                context != null ? context.targetBlock() : null
        );
        return new SummonRuntimeResult(spawned.spawned(), 0, spawned.summary());
    }

    private void applyTokenToTarget(String token,
                                    Ref<EntityStore> targetRef,
                                    Store<EntityStore> store,
                                    Ref<EntityStore> sourceRef,
                                    String sourcePlayerId,
                                    AbilityData ability) {
        applyTargetToken(token, targetRef, store, sourceRef, sourcePlayerId, ability);
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
                Vector3d nearestPoint = com.motm.util.MotmVectors.addScaled(from, segment, clampedProjection);
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
        Ref<EntityStore> playerRef = runtimePlayer == null ? null : runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null && playerRef.isValid() ? playerRef.getStore() : null;
        Ref<EntityStore> targetRef = store == null
                ? null
                : resolveTargets(playerRef, store, ability, context).stream().findFirst().orElse(null);
        ChannelHytaleAdapter.Result result = channelAdapter.startChannel(playerRef, player, ability, targetRef);
        if (!result.started()) {
            return result.summary().isBlank()
                    ? ChannelRuntimeResult.none()
                    : new ChannelRuntimeResult(false, result.summary());
        }
        return new ChannelRuntimeResult(true, result.summary());
    }

    private WeaponFollowUpResult armWeaponFollowUp(PlayerData player, AbilityData ability) {
        WeaponFollowUpSpec spec = WeaponFollowUpSpecs.resolve(ability);
        if (!spec.armed()) {
            return WeaponFollowUpResult.none();
        }

        long expireAt = System.currentTimeMillis() + (long) (Math.max(2.0, ability.getDurationSeconds()) * 1000);

        weaponFollowUps.put(
                player.getPlayerId(),
                ActiveWeaponFollowUp.create(
                        player.getPlayerId(),
                        ability,
                        expireAt,
                        spec
                )
        );

        return new WeaponFollowUpResult(true,
                "weapon follow-up ready x" + spec.uses() + " via " + humanize(ability.getName()));
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

        ActiveWeaponFollowUp followUp = weaponFollowUps.get(player.getPlayerId());
        if (isAlloyFollowUp(followUp)) {
            return null;
        }
        ActiveTransformation form = transformationState.getTransformation(player.getPlayerId());
        boolean hasClassPassiveWeaponAttack = mod.getClassPassiveManager().hasWeaponAttackPassive(player);
        boolean hasOneShot = mod.getStatusEffectManager().hasEffect(player.getPlayerId(), StatusEffect.Type.DAMAGE_BUFF)
                || mod.getStatusEffectManager().hasEffect(player.getPlayerId(), StatusEffect.Type.STEALTH);
        boolean hasAttackBuff = mod.getStatusEffectManager().hasEffect(player.getPlayerId(), StatusEffect.Type.ATTACK_BUFF);
        if (followUp == null && form == null && !hasOneShot && !hasAttackBuff && !hasClassPassiveWeaponAttack) {
            return null;
        }

        String targetEntityId = resolveEntityId(targetRef, store);
        double modifier = WeaponFollowUpHitMath.attackModifier(
                mod.getStatusEffectManager().getDamageIncrease(player.getPlayerId()),
                mod.getStatusEffectManager().consumeOneShot(player.getPlayerId(), StatusEffect.Type.DAMAGE_BUFF),
                mod.getStatusEffectManager().consumeOneShot(player.getPlayerId(), StatusEffect.Type.STEALTH),
                followUp,
                form != null ? form.weaponBonus() : 0.0
        );

        double baseDamage = WeaponFollowUpHitMath.baseWeaponDamage(
                player.getLevel(),
                mod.getLevelingManager().getPlayerAbilityPowerMultiplier(player.getLevel()),
                followUp
        );
        double resolvedDamage = baseDamage * modifier;
        ClassPassiveManager.WeaponAttackPassiveBonus passiveBonus =
                mod.getClassPassiveManager().consumeWeaponAttackBonus(player, playerRef, store, resolvedDamage);
        resolvedDamage = WeaponFollowUpHitMath.applyPassiveBonus(resolvedDamage, passiveBonus.bonusDamage());
        if (targetEntityId != null) {
            resolvedDamage = WeaponFollowUpHitMath.applyIncomingMultiplier(
                    resolvedDamage,
                    resolveIncomingDamageMultiplier(targetEntityId)
            );
            resolvedDamage = mod.getStatusEffectManager().absorbDamage(targetEntityId, resolvedDamage);
        }

        weaponFollowUpAdapter.applyPrimaryHit(
                followUp,
                form,
                resolvedDamage,
                player,
                playerRef,
                targetRef,
                store,
                targetEntityId
        );
        transformationAdapter.applyWeaponRider(form, targetRef, store, playerRef, player.getPlayerId());
        transformationAdapter.applyWeaponImpact(form, player, targetRef, store, playerRef, resolvedDamage);

        final double finalResolvedDamage = resolvedDamage;
        weaponFollowUpAdapter.applyPayoffs(playerRef, player, targetRef, store, followUp, finalResolvedDamage);

        if (followUp != null) {
            WeaponFollowUpDurabilityRestorer.restoreHeldItemDurability(runtimePlayer, itemId, LOG);
            if (followUp.decrementRemainingUses() <= 0) {
                weaponFollowUps.remove(player.getPlayerId());
            }
        }

        List<String> summaryParts = new ArrayList<>();
        summaryParts.add("[MOTM] Weapon follow-up: +" + AbilityPresentation.formatDecimal(resolvedDamage)
                + " damage" + (followUp != null ? " via " + humanize(followUp.sourceAbilityId()) : ""));
        if (WeaponFollowUpHitMath.hasSplash(followUp, resolvedDamage)) {
            summaryParts.add("splash ready");
        }
        if (followUp != null && followUp.healRatioOnHit() > 0.0) {
            summaryParts.add("healing payoff");
        }
        if (passiveBonus.applied() && !passiveBonus.summary().isBlank()) {
            summaryParts.add(passiveBonus.summary());
        }
        return String.join(" | ", summaryParts);
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

        ActiveWeaponFollowUp followUp = weaponFollowUps.get(player.getPlayerId());
        if (!isAlloyFollowUp(followUp)) {
            return null;
        }

        return weaponFollowUpAdapter.handleNativeWeaponDamage(
                runtimePlayer,
                player,
                playerRef,
                targetRef,
                store,
                followUp,
                itemId,
                damage
        );
    }

    public synchronized String handleAlloyToolUse(Player runtimePlayer,
                                                  PlayerData player,
                                                  String itemId) {
        if (runtimePlayer == null || player == null || itemId == null || itemId.isBlank()) {
            return null;
        }

        ActiveWeaponFollowUp followUp = weaponFollowUps.get(player.getPlayerId());
        if (!isAlloyFollowUp(followUp)) {
            return null;
        }

        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null && playerRef.isValid() ? playerRef.getStore() : null;
        return weaponFollowUpAdapter.handleToolUse(
                runtimePlayer,
                playerRef,
                store,
                followUp,
                itemId
        );
    }

    private boolean processWeaponFollowUpExpiry(String playerId,
                                                ActiveWeaponFollowUp followUp,
                                                Store<EntityStore> currentStore,
                                                long now) {
        final Player[] runtimePlayer = new Player[1];
        AtomicReference<Ref<EntityStore>> playerRef = new AtomicReference<>();
        return weaponFollowUpLifecycleRuntime.processExpiry(playerId, followUp, now, new WeaponFollowUpLifecycleRuntime.Hooks() {
            @Override
            public boolean playerAvailable(String playerId) {
                runtimePlayer[0] = mod.getRuntimePlayer(playerId);
                playerRef.set(runtimePlayer[0] != null ? runtimePlayer[0].getReference() : null);
                return playerRef.get() != null && playerRef.get().isValid();
            }

            @Override
            public boolean canMutateVisual(String playerId) {
                return canMutateInCurrentStore(playerRef.get(), currentStore);
            }

            @Override
            public void clearVisual(String playerId) {
                weaponFollowUpAdapter.clearAlloyHeldItemVisual(playerRef.get(), currentStore);
            }

            @Override
            public void logVisualClearSkipped(String playerId) {
                LOG.info("[MOTM] Alloy Enhancement visual clear skipped: player unavailable playerId=" + playerId);
            }

            @Override
            public void logEnded(String playerId, WeaponFollowUpLifecycleRuntime.EndReason reason) {
                LOG.info("[MOTM] Alloy Enhancement ended: "
                        + (reason == WeaponFollowUpLifecycleRuntime.EndReason.DURATION_EXPIRED
                        ? "duration expired"
                        : "uses exhausted")
                        + " playerId=" + playerId);
            }
        });
    }

    private double applySpecialDamageModifiers(PlayerData player,
                                               AbilityData ability,
                                               Ref<EntityStore> targetRef,
                                               Store<EntityStore> store,
                                               String targetEntityId,
                                               double damage) {
        if (targetEntityId == null) {
            return damage;
        }

        return switch (AbilityExecutionPolicy.specialDamagePolicy(ability)) {
            case COMBUST -> {
                if (mod.getStatusEffectManager().hasEffect(targetEntityId, StatusEffect.Type.BURN)) {
                    mod.getStatusEffectManager().removeEffect(targetEntityId, StatusEffect.Type.BURN);
                    yield damage * 1.75;
                }
                yield damage;
            }
            case LIGHTNING -> {
                boolean shocked = hasActiveOrRecentShock(targetEntityId);
                LOG.info("[MOTM] Lightning bonus check: ability=" + ability.getId()
                        + " target=" + targetEntityId
                        + " shocked=" + shocked);
                if (shocked) {
                    LOG.info("[MOTM] Lightning bonus applied: ability=" + ability.getId()
                            + " target=" + targetEntityId
                            + " multiplier=1.25");
                    yield damage * 1.25;
                }
                yield damage;
            }
            case CONSUME -> {
                double healthRatio = resolveHealthRatio(targetRef, store);
                double modifier = 1.0;
                if (healthRatio > 0.0 && healthRatio <= 0.35) {
                    modifier += healthRatio <= 0.18 ? 1.20 : 0.65;
                }
                if (mod.getStatusEffectManager().hasEffect(targetEntityId, StatusEffect.Type.VULNERABILITY)
                        || mod.getStatusEffectManager().hasEffect(targetEntityId, StatusEffect.Type.DOT)) {
                    modifier += 0.25;
                }
                yield damage * modifier;
            }
            case ANCHOR_TOXIC -> {
                if (mod.getStatusEffectManager().hasEffect(targetEntityId, StatusEffect.Type.TOXIC_MARK)) {
                    LOG.info("[MOTM] Anchor Haul Toxic follow-up applied: target="
                            + targetEntityId + " multiplier=1.10");
                    yield damage * 1.10;
                }
                yield damage;
            }
            case NONE -> damage;
        };
    }

    private boolean hasActiveOrRecentShock(String targetEntityId) {
        if (targetEntityId == null || targetEntityId.isBlank()) {
            return false;
        }

        if (mod.getStatusEffectManager().hasEffect(targetEntityId, StatusEffect.Type.SHOCKED)) {
            return true;
        }

        return combatState.hasActiveOrRecentShock(targetEntityId, System.currentTimeMillis(), SHOCKED_DAMAGE_WINDOW_MS);
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

        if (AbilityExecutionPolicy.isMultiTargetCastType(castType)) {
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

        if (AbilityExecutionPolicy.isLineCastType(castType)) {
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

        double range = AbilityRuntimeMath.range(ability);
        double radius = ability.getRadius() > 0 ? ability.getRadius() : DEFAULT_AREA_RADIUS;
        double halfWidth = ability.getWidth() > 0 ? ability.getWidth() / 2.0 : DEFAULT_LINE_HALF_WIDTH;
        String castType = lower(ability.getCastType());
        double coneThreshold = ability.getConeAngle() > 0
                ? Math.cos(Math.toRadians(ability.getConeAngle() / 2.0))
                : "gaze".equals(castType)
                ? Math.cos(Math.toRadians(12.0))
                : Math.cos(Math.toRadians(35.0));

        String ownerPlayerId = resolveEntityId(playerRef, store);
        Vector3d gemAnchor = terrainGemAdapter.resolveActiveLapidaryGemCenter(ownerPlayerId, ability, store);
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
        if (FieldRuntimeSpecs.isCasterCentered(ability)) {
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

    private String currentStyleId(PlayerData player) {
        if (player == null || player.getSelectedStyles() == null || player.getSelectedStyles().isEmpty()) {
            return null;
        }
        return player.getSelectedStyles().get(0);
    }

    private double abilityPowerMultiplier(PlayerData player) {
        return player == null
                ? 1.0
                : mod.getLevelingManager().getPlayerAbilityPowerMultiplier(player.getLevel());
    }

    private double resolveOutgoingDamageMultiplier(PlayerData player) {
        double modifier = 1.0;
        modifier += mod.getStatusEffectManager().getDamageIncrease(player.getPlayerId());
        modifier += mod.getStatusEffectManager().consumeOneShot(player.getPlayerId(), StatusEffect.Type.DAMAGE_BUFF);
        modifier += mod.getStatusEffectManager().consumeOneShot(player.getPlayerId(), StatusEffect.Type.STEALTH);
        modifier += player.getSynergyDamageIncrease().getOrDefault("all", 0.0);
        modifier += mod.getClassPassiveManager().getAbilityDamageModifier(player);
        ActiveTransformation activeForm = transformationState.getTransformation(player.getPlayerId());
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
                AbilityStatusEffects.durationTicks(ability, "shield"),
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
        if (!combatState.markAbilityKillReported(killKey)) {
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
        Vector3d destination = com.motm.util.MotmVectors.addScaled(targetPosition, direction, push)
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
            return AbilityExecutionPolicy.isAnchorDrag(ability)
                    ? applyAnchorDrag(targetRef, store, sourceRef, ability)
                    : applyKnockback(targetRef, store, sourceRef, ability);
        }

        if ("stun_if_wall".equals(normalized)) {
            KnockbackResult knockback = applyKnockbackResult(targetRef, store, sourceRef, ability);
            if (!knockback.applied()) {
                return false;
            }
            if (knockback.collidedWithWall()) {
                StatusEffect effect = AbilityStatusEffects.create("stun", ability, sourcePlayerId, ability.getId());
                if (effect != null) {
                    mod.getStatusEffectManager().applyEffect(entityId, effect);
                }
            }
            return true;
        }

        StatusEffect effect = AbilityStatusEffects.create(normalized, ability, sourcePlayerId, ability.getId());
        if (effect == null) {
            return false;
        }

        mod.getStatusEffectManager().applyEffect(entityId, effect);
        if (effect.getType() == StatusEffect.Type.SHOCKED) {
            combatState.markShocked(entityId, System.currentTimeMillis());
            LOG.info("[MOTM] Shocked token applied: ability=" + ability.getId()
                    + " target=" + entityId
                    + " durationTicks=" + effect.getInitialDurationTicks());
        }
        return true;
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
                ? clamp(ability.getKnockbackForce() * 0.9, 1.75, AbilityRuntimeMath.MAX_PULL_STEP_DISTANCE)
                : 2.5;
        dragStep = Math.min(dragStep, Math.max(0.0, remainingDistance - 1.1));
        if (dragStep <= 0.05) {
            return false;
        }

        Vector3d destination = com.motm.util.MotmVectors.addScaled(targetPosition, direction, dragStep)
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
        Vector3d ahead = com.motm.util.MotmVectors.addScaled(destination, direction, probeDistance);
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
                AbilityRuntimeMath.pullStep(ability, scale, 0.75),
                Math.max(0.0, remainingDistance - stopDistance)
        );
        if (step <= 0.05) {
            return false;
        }

        Vector3d destination = com.motm.util.MotmVectors.addScaled(targetPosition, direction, step)
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

    private boolean isMotmSummon(NPCEntity npc) {
        if (npc == null || npc.getRoleName() == null) {
            return false;
        }
        return SUMMON_ROLE_NAME.equalsIgnoreCase(npc.getRoleName())
                || PROJECTILE_VISUAL_ROLE_NAME.equalsIgnoreCase(npc.getRoleName())
                || FIELD_VISUAL_ROLE_NAME.equalsIgnoreCase(npc.getRoleName());
    }

    private boolean isMotmVisualProxy(Ref<EntityStore> ref) {
        return ref != null && visualProxyState.contains(ref);
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
        return followUp != null && followUp.alloyFollowUp();
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

    private record NearbyTargetCandidate(Ref<EntityStore> ref, double distance) { }

    private record SegmentTargetCandidate(Ref<EntityStore> ref, double alongDistance) { }

}
