package com.motm.manager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.builtin.weather.components.WeatherTracker;
import com.hypixel.hytale.builtin.weather.resources.WeatherResource;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.protocol.MovementSettings;
import com.motm.MenteesMod;
import com.motm.model.PlayerData;
import com.motm.model.StatusEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Owns runtime behavior for the final shared perk pool.
 *
 * JSON data describes the perks for UI/selection. This manager handles the
 * eventful pieces that need cooldowns, movement sampling, combat hooks, and
 * observable proof logs.
 */
public class RuntimePerkManager {

    private static final Logger LOG = Logger.getLogger("MOTM");
    private static final int TICKS_PER_SECOND = 20;
    private static final String FREEZING_WINDS_EFFECT_ID = "MOTM_Hydro_Impact";
    private static final String IGNITE_EFFECT_ID = "MOTM_Corruptus_Impact";
    private static final String GHOST_ROLE_ID = "Empty_Role";
    private static final String GHOST_MODEL_ID = "Common/NPC/Void/Spawn_Void/Models/Model.blockymodel";

    private static final String AERO_TWINKLETOES = "aero_t01_twinkletoes";
    private static final String AERO_ACCELERATE = "aero_t01_accelerate";
    private static final String AERO_BUNNY_HOP = "aero_t01_bunny_hop";
    private static final String AERO_BIG_STRIDES = "aero_t01_big_strides";
    private static final String AERO_SHARPSHOOTER = "aero_t01_sharpshooter";
    private static final String HYDRO_NEPTUNES_GRACE = "hydro_t01_neptunes_grace";
    private static final String HYDRO_SEMIAQUATIC = "hydro_t01_semiaquatic";
    private static final String HYDRO_RAINY_DAY = "hydro_t01_rainy_day";
    private static final String HYDRO_FREEZING_WINDS = "hydro_t01_freezing_winds";
    private static final String CORRUPTUS_IGNITE = "corruptus_t01_ignite";
    private static final String CORRUPTUS_DESPERATION = "corruptus_t01_desperation";
    private static final String CORRUPTUS_HAUNTING = "corruptus_t01_haunting";
    private static final String CORRUPTUS_VAMPIRISM = "corruptus_t01_vampirism";
    private static final String CORRUPTUS_TERROR = "corruptus_t01_terror";
    private static final String TERRA_HEAVYWEIGHT = "terra_t01_heavyweight";
    private static final String TERRA_ECO_FRIENDLY = "terra_t01_eco_friendly";
    private static final String TERRA_MOLE_MAN = "terra_t01_mole_man";

    private final MenteesMod mod;
    private final Map<String, Map<String, Long>> cooldownUntilTickByPlayer = new HashMap<>();
    private final Map<String, SprintState> sprintStateByPlayer = new HashMap<>();
    private final Map<String, SwimState> swimStateByPlayer = new HashMap<>();
    private final Map<String, List<GhostAlly>> ghostAlliesByPlayer = new HashMap<>();
    private final Map<String, TemporaryDamageReduction> temporaryDamageReductionByPlayer = new HashMap<>();
    private final Map<String, MovementSnapshot> movementSnapshots = new HashMap<>();
    private final Map<String, Long> rainyDayLastRegenTickByPlayer = new HashMap<>();
    private final List<IgniteDot> activeIgnites = new ArrayList<>();
    private long tickCounter = 0L;

    public RuntimePerkManager(MenteesMod mod) {
        this.mod = mod;
    }

    public void onPlayerTick(PlayerData player, Player runtimePlayer, Ref<EntityStore> playerRef,
                             Store<EntityStore> store, long ignoredTick) {
        tickCounter++;
        tickIgnites(store);
        tickGhosts(store);
        if (player == null || runtimePlayer == null || playerRef == null || !playerRef.isValid()) {
            return;
        }

        String playerId = player.getPlayerId();
        double speedBonus = 0.0;
        speedBonus += updateSprintPerks(player, runtimePlayer, playerRef, store);
        speedBonus += updateSwimPerks(player, runtimePlayer, playerRef, store);

        expireTemporaryDamageReductions();
        applyRainyDay(player, runtimePlayer, playerRef, store, false);

        if (speedBonus > 0.0) {
            applyMovementBonus(playerId, runtimePlayer, speedBonus);
        } else {
            restoreMovement(playerId, runtimePlayer);
        }
    }

    public float modifyIncomingDamage(PlayerData target, Ref<EntityStore> targetRef, Store<EntityStore> store,
                                      Damage damage, float amount) {
        if (target == null || amount <= 0.0f) {
            return amount;
        }

        float adjusted = amount;
        if (hasPerk(target, AERO_TWINKLETOES) && damage != null && damage.getCause() == DamageCause.FALL) {
            adjusted *= 0.80f;
            LOG.info("[MOTM] Runtime perk damage: twinkletoes fallReduction=0.200 before=" + amount
                    + " after=" + adjusted + " player=" + target.getPlayerId());
        }

        TemporaryDamageReduction temporaryReduction = temporaryDamageReductionByPlayer.get(target.getPlayerId());
        double reduction = temporaryReduction != null && temporaryReduction.expireAtTick > tickCounter
                ? temporaryReduction.reduction
                : 0.0;
        if (reduction > 0.0) {
            adjusted *= (float) Math.max(0.0, 1.0 - reduction);
            LOG.info("[MOTM] Runtime perk damage: temporaryReduction=" + format(reduction)
                    + " after=" + adjusted + " player=" + target.getPlayerId());
        } else if (temporaryReduction != null) {
            temporaryDamageReductionByPlayer.remove(target.getPlayerId());
        }

        triggerLowHealthPerks(target, targetRef, store, adjusted);
        return adjusted;
    }

    public float modifyOutgoingDamage(PlayerData attacker, Ref<EntityStore> attackerRef, Store<EntityStore> store,
                                      Ref<EntityStore> targetRef, Damage damage, float amount) {
        if (attacker == null || amount <= 0.0f) {
            return amount;
        }
        double adjusted = modifyMotmAbilityDamage(attacker, amount);
        if (damage != null && hasPerk(attacker, TERRA_HEAVYWEIGHT)) {
            var knockback = damage.getIfPresentMetaObject(Damage.KNOCKBACK_COMPONENT);
            if (knockback != null) {
                knockback.addModifier(1.04);
                LOG.info("[MOTM] Runtime perk knockback dealt: perk=heavyweight multiplier=1.040 player="
                        + attacker.getPlayerId());
            }
        }
        return (float) adjusted;
    }

    public double modifyMotmAbilityDamage(PlayerData attacker, double amount) {
        if (attacker == null || amount <= 0.0) {
            return amount;
        }
        if (hasPerk(attacker, CORRUPTUS_DESPERATION) && healthFraction(attacker.getPlayerId()) < 0.70) {
            double adjusted = amount * 1.10;
            LOG.info("[MOTM] Runtime perk damage: desperation multiplier=1.100 before="
                    + format(amount) + " after=" + format(adjusted) + " player=" + attacker.getPlayerId());
            return adjusted;
        }
        return amount;
    }

    public double modifyProjectileSpeed(PlayerData attacker, double speedPerTick) {
        if (attacker != null && hasPerk(attacker, AERO_SHARPSHOOTER)) {
            double adjusted = speedPerTick * 1.15;
            LOG.info("[MOTM] Runtime perk projectile speed: sharpshooter multiplier=1.150 before="
                    + format(speedPerTick) + " after=" + format(adjusted) + " player=" + attacker.getPlayerId()
                    + " nativeProjectileResidual=true");
            return adjusted;
        }
        return speedPerTick;
    }

    public double getOutgoingDamageMultiplier(PlayerData attacker) {
        if (attacker != null && hasPerk(attacker, CORRUPTUS_DESPERATION)
                && healthFraction(attacker.getPlayerId()) < 0.70) {
            return 1.10;
        }
        return 1.0;
    }

    public double modifyMiningMultiplier(PlayerData player, double multiplier) {
        if (player != null && hasPerk(player, TERRA_MOLE_MAN)
                && mod.getClassPassiveManager().isTerraCaveVisionActive(player.getPlayerId())) {
            double adjusted = multiplier + 0.10;
            LOG.info("[MOTM] Runtime perk mining applied: perk=mole_man multiplier=1.100 base="
                    + format(multiplier) + " adjusted=" + format(adjusted) + " player=" + player.getPlayerId());
            return adjusted;
        }
        return multiplier;
    }

    public double getIncomingKnockbackMultiplier(PlayerData target) {
        if (target != null && hasPerk(target, TERRA_HEAVYWEIGHT)) {
            return 0.85;
        }
        return 1.0;
    }

    public void afterSuccessfulHit(PlayerData attacker, Ref<EntityStore> attackerRef, Store<EntityStore> store,
                                   Ref<EntityStore> targetRef, double damage) {
        if (attacker == null || attackerRef == null || !attackerRef.isValid() || store == null || damage <= 0.0) {
            return;
        }
        if (hasPerk(attacker, CORRUPTUS_VAMPIRISM)) {
            double healed = healEntity(attackerRef, store, damage * 0.10);
            LOG.info("[MOTM] Runtime perk lifesteal: vampirism heal=" + format(healed)
                    + " damage=" + format(damage) + " player=" + attacker.getPlayerId());
        }
        if (hasPerk(attacker, CORRUPTUS_IGNITE) && !onCooldown(attacker.getPlayerId(), CORRUPTUS_IGNITE)) {
            int targets = applyIgnite(attacker, attackerRef, store, targetRef);
            if (targets > 0) {
                setCooldown(attacker.getPlayerId(), CORRUPTUS_IGNITE, TICKS_PER_SECOND * 20L);
            }
            LOG.info("[MOTM] Runtime perk proc: ignite targets=" + targets + " player=" + attacker.getPlayerId());
        }
    }

    public int tryTriggerTerror(PlayerData attacker, Ref<EntityStore> attackerRef, Store<EntityStore> store,
                                ItemStack heldItem) {
        if (attacker == null || attackerRef == null || !attackerRef.isValid() || store == null
                || !hasPerk(attacker, CORRUPTUS_TERROR) || onCooldown(attacker.getPlayerId(), CORRUPTUS_TERROR)
                || !isNativeWeapon(heldItem) || !hasFullSignatureEnergy(attackerRef, store)) {
            return 0;
        }
        Vector3d center = position(attackerRef, store);
        if (center == null) {
            return 0;
        }
        int targets = 0;
        for (Ref<EntityStore> target : nearbyNpcs(store, center, 7.0)) {
            String entityId = entityId(target, store);
            if (entityId == null) {
                continue;
            }
            mod.getStatusEffectManager().applyEffect(entityId,
                    new StatusEffect(StatusEffect.Type.STUN, 3 * TICKS_PER_SECOND, 1.0,
                            attacker.getPlayerId(), CORRUPTUS_TERROR));
            applyEffectById(target, store, IGNITE_EFFECT_ID);
            targets++;
        }
        setCooldown(attacker.getPlayerId(), CORRUPTUS_TERROR, 20L * TICKS_PER_SECOND);
        LOG.info("[MOTM] Runtime perk proc: terror targets=" + targets
                + " radius=7 cooldownSeconds=20 nativeUltimateProxy=signatureEnergyFullOnWeaponHit player="
                + attacker.getPlayerId());
        return targets;
    }

    public String runTerrorProof(PlayerData player, Player runtimePlayer) {
        if (player == null || runtimePlayer == null) {
            return "[MOTM] Dev passive terror failed: runtime player unavailable.";
        }
        Ref<EntityStore> ref = runtimePlayer.getReference();
        Store<EntityStore> store = ref != null ? ref.getStore() : null;
        if (ref == null || !ref.isValid() || store == null) {
            return "[MOTM] Dev passive terror failed: player store unavailable.";
        }
        int targets = 0;
        Vector3d center = position(ref, store);
        if (center != null) {
            for (Ref<EntityStore> target : nearbyNpcs(store, center, 7.0)) {
                String entityId = entityId(target, store);
                if (entityId == null) {
                    continue;
                }
                mod.getStatusEffectManager().applyEffect(entityId,
                        new StatusEffect(StatusEffect.Type.STUN, 3 * TICKS_PER_SECOND, 1.0,
                                player.getPlayerId(), CORRUPTUS_TERROR));
                applyEffectById(target, store, IGNITE_EFFECT_ID);
                targets++;
            }
        }
        String result = "[MOTM] Dev passive terror: targets=" + targets
                + " nativeUltimateHook=signatureEnergyFullOnWeaponHit";
        LOG.info(result);
        return result;
    }

    public void afterMobKilled(PlayerData killer, Player runtimePlayer, String mobEntityId) {
        if (killer == null || runtimePlayer == null || !hasPerk(killer, CORRUPTUS_HAUNTING)) {
            return;
        }
        List<GhostAlly> allies = ghostAlliesByPlayer.computeIfAbsent(killer.getPlayerId(), ignored -> new ArrayList<>());
        allies.removeIf(ghost -> ghost.expireAtTick <= tickCounter || ghost.ref == null || !ghost.ref.isValid());
        if (allies.size() >= 3) {
            LOG.info("[MOTM] Runtime perk ghost skipped: owner=" + killer.getPlayerId() + " active=3");
            return;
        }
        GhostAlly ghost = spawnGhost(killer, runtimePlayer);
        if (ghost != null) {
            allies.add(ghost);
            LOG.info("[MOTM] Runtime perk ghost spawned: owner=" + killer.getPlayerId()
                    + " active=" + allies.size() + " sourceKill=" + mobEntityId);
        }
    }

    public boolean handleBareHandBlockPunch(PlayerData player, Player runtimePlayer, DamageBlockEvent event) {
        if (player == null || runtimePlayer == null || !hasPerk(player, TERRA_ECO_FRIENDLY)) {
            return false;
        }
        String playerId = player.getPlayerId();
        if (onCooldown(playerId, TERRA_ECO_FRIENDLY)) {
            LOG.info("[MOTM] Runtime perk eco-friendly skipped: cooldownActive=true player=" + playerId);
            return true;
        }

        MenteesMod.EcoFriendlyTreeResult result = mod.applyEcoFriendlyTree(player, runtimePlayer, event);
        LOG.info("[MOTM] Runtime perk eco-friendly tree proof: player=" + playerId
                + " success=" + result.success()
                + " summary=" + result.summary());
        if (!result.success()) {
            return false;
        }
        temporaryDamageReductionByPlayer.put(playerId,
                new TemporaryDamageReduction(0.05, tickCounter + 5L * TICKS_PER_SECOND));
        setCooldown(playerId, TERRA_ECO_FRIENDLY, 20L * TICKS_PER_SECOND);
        LOG.info("[MOTM] Runtime perk proc: eco_friendly damageReduction=0.050 durationSeconds=5 cooldownSeconds=15 player="
                + playerId);
        return true;
    }

    public String runRainyDayProof(PlayerData player, Player runtimePlayer, String requestedWeatherId) {
        if (player == null || runtimePlayer == null || !hasPerk(player, HYDRO_RAINY_DAY)) {
            return "[MOTM] Dev passive rainy-day failed: Rainy Day perk is not selected.";
        }
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null ? playerRef.getStore() : null;
        if (playerRef == null || !playerRef.isValid() || store == null) {
            return "[MOTM] Dev passive rainy-day failed: runtime player store unavailable.";
        }
        String weatherId = resolveRainWeatherId(requestedWeatherId);
        WeatherResource weatherResource = store.getResource(WeatherResource.getResourceType());
        boolean forced = false;
        if (weatherResource != null && weatherId != null && !weatherId.isBlank()) {
            try {
                weatherResource.setForcedWeather(weatherId);
                forced = true;
            } catch (Throwable e) {
                LOG.warning("[MOTM] Rainy Day proof weather force failed safely: " + e.getMessage());
            }
        }
        RainState rainState = resolveRainState(playerRef, store);
        int forcedWeatherIndex = weatherIndexForId(weatherId);
        if (forced && forcedWeatherIndex >= 0) {
            WeatherTracker tracker = store.getComponent(playerRef, WeatherTracker.getComponentType());
            try {
                PlayerRef universePlayerRef = runtimePlayer.getPlayerRef();
                if (tracker != null && universePlayerRef != null) {
                    tracker.setWeatherIndex(universePlayerRef, forcedWeatherIndex);
                    rainState = resolveRainState(playerRef, store);
                }
            } catch (Throwable e) {
                LOG.warning("[MOTM] Rainy Day proof tracker update failed safely: " + e.getMessage());
            }
        }
        if (!rainState.raining && forced && isRainWeatherId(weatherId)) {
            rainState = new RainState(true, forcedWeatherIndex, weatherId);
        }
        double healed = applyRainyDay(player, runtimePlayer, playerRef, store, rainState, true);
        String result = "[MOTM] Dev passive rainy-day: requestedWeather=" + requestedWeatherId
                + " resolvedWeather=" + weatherId
                + " forced=" + forced
                + " raining=" + rainState.raining
                + " trackerWeatherId=" + rainState.weatherId
                + " heal=" + format(healed);
        LOG.info(result);
        return result;
    }

    public void clearForPlayer(String playerId) {
        if (playerId == null) {
            return;
        }
        cooldownUntilTickByPlayer.remove(playerId);
        sprintStateByPlayer.remove(playerId);
        swimStateByPlayer.remove(playerId);
        temporaryDamageReductionByPlayer.remove(playerId);
        movementSnapshots.remove(playerId);
        rainyDayLastRegenTickByPlayer.remove(playerId);
        ghostAlliesByPlayer.remove(playerId);
        activeIgnites.removeIf(dot -> playerId.equals(dot.ownerPlayerId));
    }

    private void expireTemporaryDamageReductions() {
        temporaryDamageReductionByPlayer.entrySet().removeIf(entry -> entry.getValue().expireAtTick <= tickCounter);
    }

    private double updateSprintPerks(PlayerData player, Player runtimePlayer, Ref<EntityStore> playerRef, Store<EntityStore> store) {
        boolean sprinting = isMovementState(playerRef, store, "sprinting");
        SprintState state = sprintStateByPlayer.computeIfAbsent(player.getPlayerId(), ignored -> new SprintState());
        if (!sprinting) {
            state.sprintStartTick = -1L;
            state.lastJumping = false;
            return 0.0;
        }
        if (state.sprintStartTick < 0L) {
            state.sprintStartTick = tickCounter;
        }

        double bonus = 0.0;
        if (hasPerk(player, AERO_ACCELERATE)) {
            double progress = Math.min(1.0, (tickCounter - state.sprintStartTick) / (3.0 * TICKS_PER_SECOND));
            bonus += progress * 0.05;
        }
        if (hasPerk(player, AERO_BIG_STRIDES)
                && tickCounter - state.sprintStartTick <= 3L * TICKS_PER_SECOND
                && tickCounter % TICKS_PER_SECOND == 0) {
            maximizeStat(playerRef, store, DefaultEntityStatTypes.getStamina());
            LOG.info("[MOTM] Runtime perk stamina: big_strides compensation=true player=" + player.getPlayerId());
        }
        boolean jumping = isMovementState(playerRef, store, "jumping");
        if (hasPerk(player, AERO_BUNNY_HOP) && jumping && !state.lastJumping) {
            state.bunnyCharges = Math.max(2, Math.min(5, 2 + (int) Math.floor(bonus / 0.015)));
            LOG.info("[MOTM] Runtime perk movement applied: perk=bunny_hop charges="
                    + state.bunnyCharges + " fallbackSpeedBuff=true player=" + player.getPlayerId());
        }
        state.lastJumping = jumping;
        if (state.bunnyCharges > 0) {
            bonus += 0.035;
            if (!jumping) {
                state.bunnyCharges--;
            }
        }
        if (bonus > 0.0 && tickCounter % TICKS_PER_SECOND == 0) {
            LOG.info("[MOTM] Runtime perk movement applied: perk=accelerate bonus=" + format(bonus)
                    + " player=" + player.getPlayerId());
        }
        return bonus;
    }

    private double updateSwimPerks(PlayerData player, Player runtimePlayer, Ref<EntityStore> playerRef, Store<EntityStore> store) {
        boolean swimming = isMovementState(playerRef, store, "swimming");
        SwimState state = swimStateByPlayer.computeIfAbsent(player.getPlayerId(), ignored -> new SwimState());
        if (!swimming) {
            state.swimStartTick = -1L;
            return 0.0;
        }
        if (state.swimStartTick < 0L) {
            state.swimStartTick = tickCounter;
        }
        if (!hasPerk(player, HYDRO_SEMIAQUATIC)) {
            return 0.0;
        }
        double progress = Math.min(1.0, (tickCounter - state.swimStartTick) / (5.0 * TICKS_PER_SECOND));
        double bonus = progress * 0.20;
        if (tickCounter % TICKS_PER_SECOND == 0) {
            LOG.info("[MOTM] Runtime perk movement applied: perk=semiaquatic bonus=" + format(bonus)
                    + " player=" + player.getPlayerId());
        }
        return bonus;
    }

    private double applyRainyDay(PlayerData player, Player runtimePlayer, Ref<EntityStore> playerRef,
                                 Store<EntityStore> store, boolean forceNow) {
        return applyRainyDay(player, runtimePlayer, playerRef, store, resolveRainState(playerRef, store), forceNow);
    }

    private double applyRainyDay(PlayerData player, Player runtimePlayer, Ref<EntityStore> playerRef,
                                 Store<EntityStore> store, RainState rainState, boolean forceNow) {
        if (player == null || runtimePlayer == null || !hasPerk(player, HYDRO_RAINY_DAY)) {
            return 0.0;
        }
        if (!rainState.raining) {
            return 0.0;
        }
        String playerId = player.getPlayerId();
        long lastTick = rainyDayLastRegenTickByPlayer.getOrDefault(playerId, Long.MIN_VALUE);
        if (!forceNow && tickCounter - lastTick < TICKS_PER_SECOND) {
            return 0.0;
        }
        rainyDayLastRegenTickByPlayer.put(playerId, tickCounter);
        double amount = Math.max(1.0, maxHealth(playerRef, store) * 0.01);
        double healed = healEntity(playerRef, store, amount);
        LOG.info("[MOTM] Runtime perk regen: rainy_day active=true weatherId=" + rainState.weatherId
                + " weatherIndex=" + rainState.weatherIndex
                + " heal=" + format(healed)
                + " player=" + playerId);
        return healed;
    }

    private RainState resolveRainState(Ref<EntityStore> playerRef, Store<EntityStore> store) {
        if (playerRef == null || !playerRef.isValid()) {
            return new RainState(false, -1, "");
        }
        Store<EntityStore> effectiveStore = playerRef.getStore() != null ? playerRef.getStore() : store;
        if (effectiveStore == null) {
            return new RainState(false, -1, "");
        }
        WeatherTracker tracker = effectiveStore.getComponent(playerRef, WeatherTracker.getComponentType());
        int weatherIndex = tracker != null ? tracker.getWeatherIndex() : -1;
        String weatherId = weatherIdForIndex(weatherIndex);
        if ((weatherId == null || weatherId.isBlank()) && effectiveStore.getResource(WeatherResource.getResourceType()) != null) {
            WeatherResource resource = effectiveStore.getResource(WeatherResource.getResourceType());
            weatherIndex = resource.getForcedWeatherIndex();
            weatherId = weatherIdForIndex(weatherIndex);
        }
        return new RainState(isRainWeatherId(weatherId), weatherIndex, weatherId == null ? "" : weatherId);
    }

    private String resolveRainWeatherId(String requestedWeatherId) {
        if (requestedWeatherId != null && !requestedWeatherId.isBlank() && !"auto".equalsIgnoreCase(requestedWeatherId)) {
            return requestedWeatherId.trim();
        }
        var assetMap = com.hypixel.hytale.server.core.asset.type.weather.config.Weather.getAssetMap();
        int max = Math.max(0, assetMap.getNextIndex());
        for (int i = 0; i < max; i++) {
            var weather = assetMap.getAsset(i);
            String id = weather != null ? weather.getId() : null;
            if (isRainWeatherId(id)) {
                return id;
            }
        }
        return "Rain";
    }

    private int weatherIndexForId(String weatherId) {
        if (weatherId == null || weatherId.isBlank()) {
            return -1;
        }
        var assetMap = com.hypixel.hytale.server.core.asset.type.weather.config.Weather.getAssetMap();
        int max = Math.max(0, assetMap.getNextIndex());
        for (int i = 0; i < max; i++) {
            var weather = assetMap.getAsset(i);
            if (weather != null && weatherId.equals(weather.getId())) {
                return i;
            }
        }
        return -1;
    }

    private String weatherIdForIndex(int weatherIndex) {
        if (weatherIndex < 0) {
            return "";
        }
        var weather = com.hypixel.hytale.server.core.asset.type.weather.config.Weather.getAssetMap().getAsset(weatherIndex);
        return weather != null ? weather.getId() : "";
    }

    private boolean isRainWeatherId(String weatherId) {
        if (weatherId == null || weatherId.isBlank()) {
            return false;
        }
        String normalized = weatherId.toLowerCase(Locale.ROOT);
        return normalized.contains("rain") || normalized.contains("storm") || normalized.contains("drizzle");
    }

    private void triggerLowHealthPerks(PlayerData target, Ref<EntityStore> targetRef, Store<EntityStore> store, float incomingDamage) {
        String playerId = target.getPlayerId();
        double maxHealth = maxHealth(targetRef, store);
        double currentHealth = currentHealth(targetRef, store);
        if (maxHealth <= 0.0) {
            return;
        }
        double projected = Math.max(0.0, currentHealth - incomingDamage);
        if (hasPerk(target, HYDRO_NEPTUNES_GRACE)
                && projected / maxHealth <= 0.10
                && !onCooldown(playerId, HYDRO_NEPTUNES_GRACE)) {
            double healed = healEntity(targetRef, store, maxHealth * 0.40);
            setCooldown(playerId, HYDRO_NEPTUNES_GRACE, 25L * TICKS_PER_SECOND);
            LOG.info("[MOTM] Runtime perk proc: neptunes_grace heal=" + format(healed)
                    + " cooldownSeconds=25 player=" + playerId);
        }
        if (hasPerk(target, HYDRO_FREEZING_WINDS)
                && projected / maxHealth <= 0.20
                && !onCooldown(playerId, HYDRO_FREEZING_WINDS)) {
            int targets = applyFreezingWinds(playerId, targetRef, store);
            setCooldown(playerId, HYDRO_FREEZING_WINDS, 15L * TICKS_PER_SECOND);
            LOG.info("[MOTM] Runtime perk proc: freezing_winds targets=" + targets
                    + " cooldownSeconds=15 player=" + playerId);
        }
    }

    private int applyFreezingWinds(String playerId, Ref<EntityStore> playerRef, Store<EntityStore> store) {
        Vector3d center = position(playerRef, store);
        if (center == null) {
            return 0;
        }
        int targets = 0;
        for (Ref<EntityStore> target : nearbyNpcs(store, center, 5.0)) {
            String entityId = entityId(target, store);
            if (entityId == null) {
                continue;
            }
            mod.getStatusEffectManager().applyEffect(entityId,
                    new StatusEffect(StatusEffect.Type.SLOW, 5 * TICKS_PER_SECOND, 0.50, playerId, HYDRO_FREEZING_WINDS));
            applyEffectById(target, store, FREEZING_WINDS_EFFECT_ID);
            targets++;
        }
        return targets;
    }

    private int applyIgnite(PlayerData attacker, Ref<EntityStore> attackerRef, Store<EntityStore> store, Ref<EntityStore> targetRef) {
        Vector3d center = targetRef != null && targetRef.isValid()
                ? position(targetRef, store)
                : position(attackerRef, store);
        if (center == null) {
            return 0;
        }
        int targets = 0;
        double tickDamage = Math.max(1.0, maxHealth(attackerRef, store) * 0.01);
        for (Ref<EntityStore> target : nearbyNpcs(store, center, 6.0)) {
            activeIgnites.add(new IgniteDot(attacker.getPlayerId(), attackerRef, target, tickDamage, tickCounter + 5L * TICKS_PER_SECOND, tickCounter));
            applyEffectById(target, store, IGNITE_EFFECT_ID);
            targets++;
        }
        return targets;
    }

    private GhostAlly spawnGhost(PlayerData owner, Player runtimePlayer) {
        Ref<EntityStore> ownerRef = runtimePlayer.getReference();
        if (ownerRef == null || !ownerRef.isValid()) {
            return null;
        }
        World world = runtimePlayer.getWorld();
        Store<EntityStore> store = ownerRef.getStore();
        Vector3d ownerPosition = position(ownerRef, store);
        if (world == null || store == null || ownerPosition == null) {
            return null;
        }
        NPCEntity ghost = new NPCEntity(world);
        ghost.setRoleName(GHOST_ROLE_ID);
        ghost.setDespawnTime(60.0f);
        Vector3d spawn = new Vector3d(ownerPosition.x + 1.5, ownerPosition.y + 1.0, ownerPosition.z);
        world.spawnEntity(ghost, spawn, new Vector3f(0f, 0f, 0f));
        Ref<EntityStore> ghostRef = ghost.getReference();
        if (ghostRef == null || !ghostRef.isValid()) {
            return null;
        }
        NPCEntity.setAppearance(ghostRef, GHOST_MODEL_ID, ghostRef.getStore());
        return new GhostAlly(owner.getPlayerId(), ownerRef, ghostRef,
                Math.max(1.0, maxHealth(ownerRef, store) * 0.05),
                tickCounter + 60L * TICKS_PER_SECOND,
                tickCounter);
    }

    private void tickIgnites(Store<EntityStore> store) {
        Iterator<IgniteDot> iterator = activeIgnites.iterator();
        while (iterator.hasNext()) {
            IgniteDot dot = iterator.next();
            if (tickCounter >= dot.expireAtTick || dot.targetRef == null || !dot.targetRef.isValid()) {
                iterator.remove();
                continue;
            }
            if (tickCounter - dot.lastTick < TICKS_PER_SECOND) {
                continue;
            }
            dot.lastTick = tickCounter;
            Store<EntityStore> targetStore = dot.targetRef.getStore() != null ? dot.targetRef.getStore() : store;
            Damage damage = new Damage(new Damage.EntitySource(dot.ownerRef), DamageCause.ENVIRONMENT, (float) dot.damagePerSecond);
            DamageSystems.executeDamage(dot.targetRef, targetStore, damage);
            LOG.info("[MOTM] Runtime perk dot: ignite damage=" + format(dot.damagePerSecond)
                    + " owner=" + dot.ownerPlayerId + " target=" + entityId(dot.targetRef, targetStore));
        }
    }

    private void tickGhosts(Store<EntityStore> store) {
        for (List<GhostAlly> allies : ghostAlliesByPlayer.values()) {
            Iterator<GhostAlly> iterator = allies.iterator();
            while (iterator.hasNext()) {
                GhostAlly ghost = iterator.next();
                if (tickCounter >= ghost.expireAtTick || ghost.ref == null || !ghost.ref.isValid()) {
                    iterator.remove();
                    continue;
                }
                if (tickCounter - ghost.lastAttackTick < 2L * TICKS_PER_SECOND) {
                    continue;
                }
                Store<EntityStore> ghostStore = ghost.ref.getStore() != null ? ghost.ref.getStore() : store;
                Ref<EntityStore> target = nearestNpc(ghostStore, position(ghost.ownerRef, ghostStore), 12.0);
                if (target == null) {
                    continue;
                }
                ghost.lastAttackTick = tickCounter;
                Damage damage = new Damage(new Damage.EntitySource(ghost.ref), DamageCause.PHYSICAL, (float) ghost.damage);
                DamageSystems.executeDamage(target, ghostStore, damage);
                LOG.info("[MOTM] Runtime perk ghost attack: owner=" + ghost.ownerPlayerId
                        + " target=" + entityId(target, ghostStore)
                        + " damage=" + format(ghost.damage));
            }
        }
    }

    private void applyMovementBonus(String playerId, Player runtimePlayer, double bonus) {
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return;
        }
        MovementManager movementManager = playerRef.getStore().getComponent(playerRef, MovementManager.getComponentType());
        if (movementManager == null || movementManager.getSettings() == null) {
            return;
        }
        var settings = movementManager.getSettings();
        MovementSnapshot snapshot = movementSnapshots.computeIfAbsent(playerId, ignored -> MovementSnapshot.capture(settings));
        double multiplier = 1.0 + Math.max(0.0, bonus);
        settings.forwardSprintSpeedMultiplier = (float) (snapshot.forwardSprintSpeedMultiplier * multiplier);
        settings.strafeRunSpeedMultiplier = (float) (snapshot.strafeRunSpeedMultiplier * multiplier);
        settings.forwardRunSpeedMultiplier = (float) (snapshot.forwardRunSpeedMultiplier * multiplier);
        updateMovement(playerRef, movementManager);
    }

    private void restoreMovement(String playerId, Player runtimePlayer) {
        MovementSnapshot snapshot = movementSnapshots.remove(playerId);
        if (snapshot == null || runtimePlayer == null) {
            return;
        }
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return;
        }
        MovementManager movementManager = playerRef.getStore().getComponent(playerRef, MovementManager.getComponentType());
        if (movementManager == null || movementManager.getSettings() == null) {
            return;
        }
        snapshot.restore(movementManager.getSettings());
        updateMovement(playerRef, movementManager);
    }

    private void updateMovement(Ref<EntityStore> playerRef, MovementManager movementManager) {
        PlayerRef universePlayerRef = playerRef.getStore().getComponent(playerRef, PlayerRef.getComponentType());
        if (universePlayerRef != null && universePlayerRef.getPacketHandler() != null) {
            movementManager.update(universePlayerRef.getPacketHandler());
        }
    }

    private boolean hasPerk(PlayerData player, String perkId) {
        return player != null && player.getSelectedPerks() != null && player.getSelectedPerks().contains(perkId);
    }

    private boolean isNativeWeapon(ItemStack heldItem) {
        return heldItem != null && heldItem.getItem() != null && heldItem.getItem().getWeapon() != null;
    }

    private boolean hasFullSignatureEnergy(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatValue signature = statValue(ref, store, DefaultEntityStatTypes.getSignatureEnergy());
        return signature != null && signature.getMax() > 0.0f && signature.get() >= signature.getMax() * 0.99f;
    }

    private boolean onCooldown(String playerId, String key) {
        return cooldownUntilTickByPlayer.getOrDefault(playerId, Map.of()).getOrDefault(key, 0L) > tickCounter;
    }

    private void setCooldown(String playerId, String key, long ticks) {
        cooldownUntilTickByPlayer.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(key, tickCounter + ticks);
    }

    private boolean isMovementState(Ref<EntityStore> playerRef, Store<EntityStore> store, String stateName) {
        if (playerRef == null || !playerRef.isValid()) {
            return false;
        }
        Store<EntityStore> effectiveStore = playerRef.getStore() != null ? playerRef.getStore() : store;
        if (effectiveStore == null) {
            return false;
        }
        MovementStatesComponent statesComponent = effectiveStore.getComponent(playerRef, MovementStatesComponent.getComponentType());
        if (statesComponent == null || statesComponent.getMovementStates() == null) {
            return false;
        }
        var states = statesComponent.getMovementStates();
        return switch (stateName) {
            case "sprinting" -> states.sprinting;
            case "swimming" -> states.swimming;
            case "jumping" -> states.jumping;
            default -> false;
        };
    }

    private double healthFraction(String playerId) {
        Player runtimePlayer = mod.getRuntimePlayer(playerId);
        if (runtimePlayer == null) {
            return 1.0;
        }
        Ref<EntityStore> ref = runtimePlayer.getReference();
        double max = maxHealth(ref, ref != null ? ref.getStore() : null);
        return max <= 0.0 ? 1.0 : currentHealth(ref, ref.getStore()) / max;
    }

    private double currentHealth(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatValue health = healthValue(ref, store);
        return health != null ? health.get() : 0.0;
    }

    private double maxHealth(Ref<EntityStore> ref, Store<EntityStore> store) {
        EntityStatValue health = healthValue(ref, store);
        return health != null ? health.getMax() : 0.0;
    }

    private double healEntity(Ref<EntityStore> ref, Store<EntityStore> store, double amount) {
        if (amount <= 0.0 || ref == null || !ref.isValid()) {
            return 0.0;
        }
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        EntityStatValue health = statMap != null ? statMap.get(DefaultEntityStatTypes.getHealth()) : null;
        if (health == null || health.getMax() <= 0.0f) {
            return 0.0;
        }
        float applied = (float) Math.max(0.0, Math.min(amount, health.getMax() - health.get()));
        if (applied > 0.0f) {
            statMap.addStatValue(DefaultEntityStatTypes.getHealth(), applied);
        }
        return applied;
    }

    private EntityStatValue healthValue(Ref<EntityStore> ref, Store<EntityStore> store) {
        return statValue(ref, store, DefaultEntityStatTypes.getHealth());
    }

    private EntityStatValue statValue(Ref<EntityStore> ref, Store<EntityStore> store, int statType) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        return statMap != null ? statMap.get(statType) : null;
    }

    private void maximizeStat(Ref<EntityStore> ref, Store<EntityStore> store, int statType) {
        if (ref == null || !ref.isValid() || store == null) {
            return;
        }
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        if (statMap != null) {
            statMap.maximizeStatValue(statType);
        }
    }

    private List<Ref<EntityStore>> nearbyNpcs(Store<EntityStore> store, Vector3d center, double radius) {
        List<Ref<EntityStore>> targets = new ArrayList<>();
        if (store == null || center == null) {
            return targets;
        }
        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (ref == null || !ref.isValid() || npc == null || npc.isDespawning()) {
                    continue;
                }
                if (chunk.getComponent(entityIndex, DeathComponent.getComponentType()) != null) {
                    continue;
                }
                TransformComponent transform = chunk.getComponent(entityIndex, TransformComponent.getComponentType());
                Vector3d position = transform != null && transform.getTransform() != null
                        ? transform.getTransform().getPosition()
                        : null;
                if (position != null && distance(center, position) <= radius) {
                    targets.add(ref);
                }
            }
        });
        return targets;
    }

    private Ref<EntityStore> nearestNpc(Store<EntityStore> store, Vector3d center, double radius) {
        Ref<EntityStore> best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Ref<EntityStore> target : nearbyNpcs(store, center, radius)) {
            double distance = distance(center, position(target, store));
            if (distance < bestDistance) {
                bestDistance = distance;
                best = target;
            }
        }
        return best;
    }

    private Vector3d position(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }
        TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
        return transform != null && transform.getTransform() != null ? transform.getTransform().getPosition() : null;
    }

    private String entityId(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (ref == null || !ref.isValid() || store == null) {
            return null;
        }
        UUIDComponent uuid = store.getComponent(ref, UUIDComponent.getComponentType());
        return uuid != null && uuid.getUuid() != null ? uuid.getUuid().toString() : ref.toString();
    }

    private void applyEffectById(Ref<EntityStore> ref, Store<EntityStore> store, String effectId) {
        if (ref == null || !ref.isValid() || store == null || effectId == null || effectId.isBlank()) {
            return;
        }
        EntityEffect effect = EntityEffect.getAssetMap().getAsset(effectId);
        EffectControllerComponent controller = store.getComponent(ref, EffectControllerComponent.getComponentType());
        if (effect != null && controller != null) {
            controller.addEffect(ref, effect, store);
        }
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

    private String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static final class SprintState {
        private long sprintStartTick = -1L;
        private boolean lastJumping;
        private int bunnyCharges;
    }

    private static final class SwimState {
        private long swimStartTick = -1L;
    }

    private static final class TemporaryDamageReduction {
        private final double reduction;
        private final long expireAtTick;

        private TemporaryDamageReduction(double reduction, long expireAtTick) {
            this.reduction = reduction;
            this.expireAtTick = expireAtTick;
        }
    }

    private record RainState(boolean raining, int weatherIndex, String weatherId) {}

    private static final class IgniteDot {
        private final String ownerPlayerId;
        private final Ref<EntityStore> ownerRef;
        private final Ref<EntityStore> targetRef;
        private final double damagePerSecond;
        private final long expireAtTick;
        private long lastTick;

        private IgniteDot(String ownerPlayerId, Ref<EntityStore> ownerRef, Ref<EntityStore> targetRef,
                          double damagePerSecond, long expireAtTick, long lastTick) {
            this.ownerPlayerId = ownerPlayerId;
            this.ownerRef = ownerRef;
            this.targetRef = targetRef;
            this.damagePerSecond = damagePerSecond;
            this.expireAtTick = expireAtTick;
            this.lastTick = lastTick;
        }
    }

    private static final class GhostAlly {
        private final String ownerPlayerId;
        private final Ref<EntityStore> ownerRef;
        private final Ref<EntityStore> ref;
        private final double damage;
        private final long expireAtTick;
        private long lastAttackTick;

        private GhostAlly(String ownerPlayerId, Ref<EntityStore> ownerRef, Ref<EntityStore> ref,
                          double damage, long expireAtTick, long lastAttackTick) {
            this.ownerPlayerId = ownerPlayerId;
            this.ownerRef = ownerRef;
            this.ref = ref;
            this.damage = damage;
            this.expireAtTick = expireAtTick;
            this.lastAttackTick = lastAttackTick;
        }
    }

    private record MovementSnapshot(
            float forwardRunSpeedMultiplier,
            float strafeRunSpeedMultiplier,
            float forwardSprintSpeedMultiplier
    ) {
        private static MovementSnapshot capture(MovementSettings settings) {
            return new MovementSnapshot(
                    settings.forwardRunSpeedMultiplier,
                    settings.strafeRunSpeedMultiplier,
                    settings.forwardSprintSpeedMultiplier
            );
        }

        private void restore(MovementSettings settings) {
            settings.forwardRunSpeedMultiplier = forwardRunSpeedMultiplier;
            settings.strafeRunSpeedMultiplier = strafeRunSpeedMultiplier;
            settings.forwardSprintSpeedMultiplier = forwardSprintSpeedMultiplier;
        }
    }
}
