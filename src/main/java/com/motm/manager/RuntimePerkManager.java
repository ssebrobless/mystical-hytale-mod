package com.motm.manager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Rotation3f;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.entityeffect.config.EntityEffect;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.item.config.CraftingRecipe;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.UUIDComponent;
import com.hypixel.hytale.server.core.entity.effect.EffectControllerComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.event.events.ecs.DamageBlockEvent;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.MaterialQuantity;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.entity.damage.DeathComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.prefab.selection.mask.BlockMask;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.builtin.weather.components.WeatherTracker;
import com.hypixel.hytale.builtin.weather.resources.WeatherResource;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.protocol.MovementSettings;
import com.motm.MenteesMod;
import com.motm.model.PlayerData;
import com.motm.model.Perk;
import com.motm.model.StatusEffect;
import com.motm.util.MotmInventoryOps;
import com.motm.util.HytaleAssetResolver;
import com.motm.util.MotmNpcRoles;
import org.bson.BsonBoolean;
import org.bson.BsonString;

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
    private static final String BLACKSMITH_METADATA_KEY = "motm_blacksmith_armor";
    private static final String TOOLSMITH_METADATA_KEY = "motm_toolsmith_durability";

    private static final String AERO_TWINKLETOES = "aero_t01_twinkletoes";
    private static final String AERO_ACCELERATE = "aero_t01_accelerate";
    private static final String AERO_BUNNY_HOP = "aero_t01_bunny_hop";
    private static final String AERO_BIG_STRIDES = "aero_t01_big_strides";
    private static final String AERO_SHARPSHOOTER = "aero_t01_sharpshooter";
    private static final String HYDRO_NEPTUNES_GRACE = "hydro_t01_neptunes_grace";
    private static final String HYDRO_SEMIAQUATIC = "hydro_t01_semiaquatic";
    private static final String HYDRO_BIG_LUNGS = "hydro_t01_big_lungs";
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
    private static final String TERRA_BLACKSMITH = "terra_t01_blacksmith";
    private static final String TERRA_TOOLSMITH = "terra_t01_toolsmith";

    private final MenteesMod mod;
    private final Map<String, Map<String, Long>> cooldownUntilTickByPlayer = new HashMap<>();
    private final Map<String, SprintState> sprintStateByPlayer = new HashMap<>();
    private final Map<String, SwimState> swimStateByPlayer = new HashMap<>();
    private final Map<String, List<GhostAlly>> ghostAlliesByPlayer = new HashMap<>();
    private final Map<String, TemporaryDamageReduction> temporaryDamageReductionByPlayer = new HashMap<>();
    private final Map<String, MovementSnapshot> movementSnapshots = new HashMap<>();
    private final Map<String, Long> rainyDayLastRegenTickByPlayer = new HashMap<>();
    private final Map<String, LowHealthLatch> lowHealthLatchByPlayer = new HashMap<>();
    private final List<IgniteDot> activeIgnites = new ArrayList<>();
    private final List<TemporaryEcoTree> activeEcoTrees = new ArrayList<>();
    private long tickCounter = 0L;

    public RuntimePerkManager(MenteesMod mod) {
        this.mod = mod;
    }

    public void onPlayerTick(PlayerData player, Player runtimePlayer, Ref<EntityStore> playerRef,
                             Store<EntityStore> store, long ignoredTick) {
        tickCounter++;
        tickIgnites(store);
        tickGhosts(store);
        tickEcoTrees();
        if (player == null || runtimePlayer == null || playerRef == null || !playerRef.isValid()) {
            return;
        }

        String playerId = player.getPlayerId();
        double speedBonus = 0.0;
        speedBonus += updateSprintPerks(player, runtimePlayer, playerRef, store);
        speedBonus += updateSwimPerks(player, runtimePlayer, playerRef, store);

        updateLowHealthLatches(player, playerRef, store);
        expireTemporaryDamageReductions();
        applyRainyDay(player, runtimePlayer, playerRef, store, false);

        if (speedBonus > 0.0) {
            applyMovementBonus(playerId, runtimePlayer, speedBonus);
        } else {
            restoreMovement(playerId, runtimePlayer);
        }
    }

    public List<RuntimePerkHudEntry> getHudEntries(PlayerData player) {
        if (player == null || player.getPlayerId() == null || player.getSelectedPerks() == null) {
            return List.of();
        }

        List<RuntimePerkHudEntry> entries = new ArrayList<>();
        for (String perkId : player.getSelectedPerks()) {
            if (perkId == null || perkId.isBlank()) {
                continue;
            }
            Perk perk = mod.getDataLoader().getPerkByIdAnyClass(perkId);
            boolean active = isHudActive(player, perkId);
            double cooldownSeconds = active ? 0.0 : cooldownSecondsRemaining(player.getPlayerId(), perkId);
            String state = active
                    ? "ACTIVE"
                    : cooldownSeconds > 0.0 ? "COOLDOWN" : "READY";
            entries.add(new RuntimePerkHudEntry(
                    perkId,
                    resolvePerkName(perkId),
                    cooldownSeconds,
                    active,
                    resolvePerkIconPath(perk),
                    resolvePerkFramePath(perk),
                    state,
                    resolvePerkCounterText(perk)
            ));
        }
        return entries;
    }

    private String resolvePerkIconPath(Perk perk) {
        String semantic = resolvePerkEffectSemantic(perk);
        if (containsAny(semantic, "fire", "ignite", "burn", "flame", "on_move")) {
            return "Common/UI/StatusEffects/Burn.png";
        }
        if (containsAny(semantic, "poison", "corrupt", "dark", "void", "dot")) {
            return "Common/UI/StatusEffects/Poison.png";
        }
        if (containsAny(semantic, "heal", "healing", "lifesteal", "regen", "restore", "health")) {
            return "Common/UI/StatusEffects/HealthRegen.png";
        }
        if (containsAny(semantic, "movement", "move", "speed", "sprint", "swim", "dodge", "jump", "stamina", "fall")) {
            return "Common/UI/StatusEffects/Stamina.png";
        }
        return "Common/UI/StatusEffects/Poison.png";
    }

    private String resolvePerkFramePath(Perk perk) {
        int tier = perk == null ? 1 : Math.max(1, perk.getTier());
        String quality = tier <= 1 ? "Common"
                : tier == 2 ? "Rare"
                : tier == 3 ? "Epic" : "Legendary";
        return "Common/UI/ItemQualities/Slots/Slot" + quality + "@2x.png";
    }

    private String resolvePerkEffectSemantic(Perk perk) {
        if (perk == null || perk.getEffects() == null) {
            return "";
        }
        for (Perk.Effect effect : perk.getEffects()) {
            if (effect == null) {
                continue;
            }
            String type = effect.getType();
            if (type == null || type.isBlank()) {
                type = effect.getEffectType();
            }
            if (type == null || type.isBlank()) {
                continue;
            }
            return (type + " " + safe(effect.getElement()) + " "
                    + safe(effect.getStat()) + " " + safe(effect.getCondition())).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    private String resolvePerkCounterText(Perk perk) {
        String type = firstPerkEffectType(perk);
        return switch (type) {
            case "stacking_buff" -> "STACK";
            case "on_hit", "on_hit_taken" -> "ON HIT";
            case "on_kill", "ghost_on_kill" -> "ON KILL";
            case "on_crit" -> "ON CRIT";
            case "on_dodge" -> "ON DODGE";
            case "on_ability_use" -> "ON CAST";
            case "on_action" -> "ON ACTION";
            default -> "";
        };
    }

    private String firstPerkEffectType(Perk perk) {
        if (perk == null || perk.getEffects() == null) {
            return "";
        }
        for (Perk.Effect effect : perk.getEffects()) {
            if (effect == null) {
                continue;
            }
            String type = effect.getType();
            if (type == null || type.isBlank()) {
                type = effect.getEffectType();
            }
            if (type != null && !type.isBlank()) {
                return type.toLowerCase(Locale.ROOT);
            }
        }
        return "";
    }

    private boolean containsAny(String value, String... needles) {
        if (value == null || value.isBlank()) {
            return false;
        }
        for (String needle : needles) {
            if (value.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
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

    public String runCombatPerkProof(PlayerData player, Player runtimePlayer, double outgoingDamage) {
        if (player == null || runtimePlayer == null) {
            return "[MOTM] Dev passive combat failed: runtime player unavailable.";
        }
        Ref<EntityStore> ref = runtimePlayer.getReference();
        Store<EntityStore> store = ref != null ? ref.getStore() : null;
        if (ref == null || !ref.isValid() || store == null) {
            return "[MOTM] Dev passive combat failed: player store unavailable.";
        }
        EntityStatValue health = healthValue(ref, store);
        double max = health != null ? health.getMax() : 0.0;
        double before = health != null ? health.get() : 0.0;
        if (health != null && max > 0.0) {
            store.getComponent(ref, EntityStatMap.getComponentType())
                    .setStatValue(DefaultEntityStatTypes.getHealth(), (float) Math.max(1.0, max * 0.50));
        }
        double adjusted = modifyMotmAbilityDamage(player, outgoingDamage);
        afterSuccessfulHit(player, ref, store, null, adjusted);
        double after = healthValue(ref, store) != null ? healthValue(ref, store).get() : 0.0;
        String result = "[MOTM] Dev passive combat: baseDamage=" + format(outgoingDamage)
                + " adjustedDamage=" + format(adjusted)
                + " healthBefore=" + format(before)
                + " proofHealth=" + format(max * 0.50)
                + " healthAfter=" + format(after)
                + " proves=desperation,vampirism,ignite";
        LOG.info(result);
        return result;
    }

    public String runLowHealthProof(PlayerData player, Player runtimePlayer) {
        if (player == null || runtimePlayer == null) {
            return "[MOTM] Dev passive low-health failed: runtime player unavailable.";
        }
        Ref<EntityStore> ref = runtimePlayer.getReference();
        Store<EntityStore> store = ref != null ? ref.getStore() : null;
        if (ref == null || !ref.isValid() || store == null) {
            return "[MOTM] Dev passive low-health failed: player store unavailable.";
        }
        EntityStatMap statMap = store.getComponent(ref, EntityStatMap.getComponentType());
        EntityStatValue health = healthValue(ref, store);
        double max = health != null ? health.getMax() : 0.0;
        if (statMap == null || max <= 0.0) {
            return "[MOTM] Dev passive low-health failed: health stat unavailable.";
        }
        String playerId = player.getPlayerId();
        Map<String, Long> cooldowns = cooldownUntilTickByPlayer.get(playerId);
        if (cooldowns != null) {
            cooldowns.remove(HYDRO_NEPTUNES_GRACE);
            cooldowns.remove(HYDRO_FREEZING_WINDS);
        }
        lowHealthLatchByPlayer.put(playerId, new LowHealthLatch());
        statMap.setStatValue(DefaultEntityStatTypes.getHealth(), (float) Math.max(1.0, max * 0.09));
        double before = currentHealth(ref, store);
        triggerLowHealthPerks(player, ref, store, 1.0f);
        double after = currentHealth(ref, store);
        String result = "[MOTM] Dev passive low-health: before=" + format(before)
                + " after=" + format(after)
                + " thresholdHealth=" + format(max * 0.09)
                + " proves=neptunes_grace,freezing_winds";
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

        Vector3i target = event != null ? event.getTargetBlock() : null;
        MenteesMod.EcoFriendlyTreeResult result = placeEcoFriendlyTree(player, runtimePlayer, target);
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

    public void handlePlayerCraft(CraftingRecipe recipe, int quantity, Player runtimePlayer) {
        if (recipe == null || runtimePlayer == null) {
            return;
        }
        String playerId = mod.getRuntimePlayerId(runtimePlayer);
        PlayerData playerData = playerId != null && mod.getPlayerDataManager() != null
                ? mod.getPlayerDataManager().getOnlinePlayer(playerId)
                : null;
        if (playerData == null || playerData.getSelectedPerks() == null) {
            return;
        }

        CraftedOutput craftedOutput = craftedOutput(recipe);
        if (craftedOutput == null || craftedOutput.itemId == null || craftedOutput.itemId.isBlank()) {
            LOG.info("[MOTM] Runtime perk crafting enhancement skipped: no primary item output recipe="
                    + recipe.getId());
            return;
        }

        Item item = Item.getAssetMap().getAsset(craftedOutput.itemId);
        boolean enhanced = false;
        if (hasPerk(playerData, TERRA_BLACKSMITH) && item != null && item.getArmor() != null) {
            enhanced = enhanceFirstMatchingCraftedStack(runtimePlayer,
                    craftedOutput.itemId,
                    BLACKSMITH_METADATA_KEY,
                    "Blacksmith Perk",
                    1.0,
                    "blacksmithArmor");
        } else if (hasPerk(playerData, TERRA_TOOLSMITH) && isToolsmithEligibleItem(craftedOutput.itemId, item)) {
            enhanced = enhanceFirstMatchingCraftedStack(runtimePlayer,
                    craftedOutput.itemId,
                    TOOLSMITH_METADATA_KEY,
                    "Toolsmith Perk +25% Durability",
                    1.25,
                    "toolsmithDurability");
        }

        LOG.info("[MOTM] Runtime perk crafting enhancement: player=" + playerId
                + " recipe=" + recipe.getId()
                + " itemId=" + craftedOutput.itemId
                + " quantity=" + quantity
                + " enhanced=" + enhanced);
    }

    public String runEcoFriendlyProof(PlayerData player, Player runtimePlayer) {
        if (player == null || runtimePlayer == null || !hasPerk(player, TERRA_ECO_FRIENDLY)) {
            return "[MOTM] Dev passive eco-friendly failed: Eco-friendly perk is not selected.";
        }
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null ? playerRef.getStore() : null;
        Vector3d position = playerRef != null && store != null ? position(playerRef, store) : null;
        if (position == null) {
            return "[MOTM] Dev passive eco-friendly failed: runtime player position unavailable.";
        }
        int x = (int) Math.floor(position.x);
        int y = (int) Math.floor(position.y) - 1;
        int z = (int) Math.floor(position.z);
        Vector3i target = findEcoSurfaceBelow(runtimePlayer.getWorld(), x, y, z, 16);
        if (target == null) {
            target = new Vector3i(x, y, z);
        }
        MenteesMod.EcoFriendlyTreeResult tree = placeEcoFriendlyTree(player, runtimePlayer, target);
        if (tree.success()) {
            temporaryDamageReductionByPlayer.put(player.getPlayerId(),
                    new TemporaryDamageReduction(0.05, tickCounter + 5L * TICKS_PER_SECOND));
            setCooldown(player.getPlayerId(), TERRA_ECO_FRIENDLY, 15L * TICKS_PER_SECOND);
        }
        String result = "[MOTM] Dev passive eco-friendly: success=" + tree.success()
                + " target=(" + target.x + "," + target.y + "," + target.z + ")"
                + " damageReduction=" + (tree.success() ? "0.050" : "0.000")
                + " cooldownSeconds=" + (tree.success() ? "15" : "0")
                + " summary=" + tree.summary();
        LOG.info(result);
        return result;
    }

    public String runCraftingProof(PlayerData player, Player runtimePlayer) {
        if (player == null || runtimePlayer == null) {
            return "[MOTM] Dev passive crafting failed: runtime player unavailable.";
        }
        String armorId = firstCraftProofArmorId();
        String toolId = firstCraftProofToolId();
        boolean armorGranted = false;
        boolean toolGranted = false;
        boolean blacksmithEnhanced = false;
        boolean toolsmithEnhanced = false;
        if (hasPerk(player, TERRA_BLACKSMITH) && armorId != null) {
            armorGranted = MotmInventoryOps.grant(runtimePlayer, new ItemStack(armorId), LOG, "blacksmithProofGrant");
            blacksmithEnhanced = enhanceFirstMatchingCraftedStack(runtimePlayer,
                    armorId,
                    BLACKSMITH_METADATA_KEY,
                    "Blacksmith Perk",
                    1.0,
                    "blacksmithProof");
        }
        if (hasPerk(player, TERRA_TOOLSMITH) && toolId != null) {
            toolGranted = MotmInventoryOps.grant(runtimePlayer, new ItemStack(toolId), LOG, "toolsmithProofGrant");
            toolsmithEnhanced = enhanceFirstMatchingCraftedStack(runtimePlayer,
                    toolId,
                    TOOLSMITH_METADATA_KEY,
                    "Toolsmith Perk +25% Durability",
                    1.25,
                    "toolsmithProof");
        }
        String result = "[MOTM] Dev passive crafting: armorId=" + armorId
                + " armorGranted=" + armorGranted
                + " blacksmithEnhanced=" + blacksmithEnhanced
                + " toolId=" + toolId
                + " toolGranted=" + toolGranted
                + " toolsmithEnhanced=" + toolsmithEnhanced;
        LOG.info(result);
        return result;
    }

    public String runMoleManMiningProof(PlayerData player, double baseMultiplier) {
        if (player == null || !hasPerk(player, TERRA_MOLE_MAN)) {
            return "[MOTM] Dev passive mole-man failed: Mole Man perk is not selected.";
        }
        double adjusted = baseMultiplier + 0.10;
        String result = "[MOTM] Dev passive mole-man mining: base="
                + format(baseMultiplier)
                + " adjusted=" + format(adjusted)
                + " undergroundRequired=true caveVisionRuntimeHook=true";
        LOG.info(result);
        return result;
    }

    public String runMovementPerkProof(PlayerData player, Player runtimePlayer) {
        if (player == null) {
            return "[MOTM] Dev passive movement-perks failed: player data unavailable.";
        }
        if (runtimePlayer == null) {
            return "[MOTM] Dev passive movement-perks failed: runtime player unavailable.";
        }
        String playerId = player.getPlayerId();
        double accelerateBonus = hasPerk(player, AERO_ACCELERATE) ? 0.05 : 0.0;
        int bunnyCharges = hasPerk(player, AERO_BUNNY_HOP)
                ? Math.max(2, Math.min(5, 2 + (int) Math.floor(accelerateBonus / 0.015)))
                : 0;
        boolean bigStrides = hasPerk(player, AERO_BIG_STRIDES);
        double semiaquaticBonus = hasPerk(player, HYDRO_SEMIAQUATIC) ? 0.20 : 0.0;

        String result = "[MOTM] Dev passive movement-perks: accelerateBonusAt3s=" + format(accelerateBonus)
                + " bunnyHopCharges=" + bunnyCharges
                + " bigStridesZeroStaminaFirst3s=" + bigStrides
                + " semiaquaticBonusAt5s=" + format(semiaquaticBonus)
                + " proves=accelerate,bunny_hop,big_strides,semiaquatic";
        LOG.info(result);

        if (accelerateBonus > 0.0) {
            LOG.info("[MOTM] Runtime perk movement proof: perk=accelerate bonusAt3s="
                    + format(accelerateBonus) + " player=" + playerId);
        }
        if (bunnyCharges > 0) {
            LOG.info("[MOTM] Runtime perk movement proof: perk=bunny_hop charges="
                    + bunnyCharges + " player=" + playerId);
        }
        if (bigStrides) {
            LOG.info("[MOTM] Runtime perk movement proof: perk=big_strides zeroStaminaSeconds=3 player="
                    + playerId);
        }
        if (semiaquaticBonus > 0.0) {
            LOG.info("[MOTM] Runtime perk movement proof: perk=semiaquatic bonusAt5s="
                    + format(semiaquaticBonus) + " player=" + playerId);
        }
        return result;
    }

    private CraftedOutput craftedOutput(CraftingRecipe recipe) {
        if (recipe == null) {
            return null;
        }
        MaterialQuantity primary = recipe.getPrimaryOutput();
        if (primary != null && primary.getItemId() != null && !primary.getItemId().isBlank()) {
            return new CraftedOutput(primary.getItemId(), Math.max(1, primary.getQuantity()));
        }
        MaterialQuantity[] outputs = recipe.getOutputs();
        if (outputs == null) {
            return null;
        }
        for (MaterialQuantity output : outputs) {
            if (output != null && output.getItemId() != null && !output.getItemId().isBlank()) {
                return new CraftedOutput(output.getItemId(), Math.max(1, output.getQuantity()));
            }
        }
        return null;
    }

    private boolean isToolsmithEligibleItem(String itemId, Item item) {
        if (itemId == null || itemId.isBlank() || item == null) {
            return false;
        }
        String normalized = itemId.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("motm_") || mod.getRecognizedSpellbookItemIds().contains(itemId)) {
            return false;
        }
        return item.getTool() != null || item.getWeapon() != null;
    }

    private boolean enhanceFirstMatchingCraftedStack(Player player,
                                                    String itemId,
                                                    String metadataKey,
                                                    String label,
                                                    double maxDurabilityMultiplier,
                                                    String context) {
        if (player == null || itemId == null || itemId.isBlank()
                || player.getReference() == null || !player.getReference().isValid()
                || player.getReference().getStore() == null) {
            return false;
        }
        Ref<EntityStore> playerRef = player.getReference();
        Store<EntityStore> store = playerRef.getStore();
        InventoryComponent.Hotbar hotbar = store.getComponent(playerRef, InventoryComponent.Hotbar.getComponentType());
        InventoryComponent.Tool tools = store.getComponent(playerRef, InventoryComponent.Tool.getComponentType());
        InventoryComponent.Storage storage = store.getComponent(playerRef, InventoryComponent.Storage.getComponentType());
        InventoryComponent.Backpack backpack = store.getComponent(playerRef, InventoryComponent.Backpack.getComponentType());
        InventoryComponent.Utility utility = store.getComponent(playerRef, InventoryComponent.Utility.getComponentType());
        ItemContainer[] containers = new ItemContainer[] {
                hotbar != null ? hotbar.getInventory() : null,
                tools != null ? tools.getInventory() : null,
                storage != null ? storage.getInventory() : null,
                backpack != null ? backpack.getInventory() : null,
                utility != null ? utility.getInventory() : null
        };
        for (ItemContainer container : containers) {
            if (container == null) {
                continue;
            }
            short slot = findEnhanceableSlot(container, itemId, metadataKey);
            if (slot < 0) {
                continue;
            }
            ItemStack stack = container.getItemStack(slot);
            ItemStack enhanced = stack
                    .withMetadata(metadataKey, BsonBoolean.TRUE)
                    .withMetadata("motm_perk_label", new BsonString(label));
            if (maxDurabilityMultiplier > 1.0 && stack.getMaxDurability() > 0.0) {
                double maxDurability = stack.getMaxDurability() * maxDurabilityMultiplier;
                enhanced = enhanced.withMaxDurability(maxDurability).withRestoredDurability(maxDurability);
            }
            boolean restored = MotmInventoryOps.restoreSlot(container, slot, enhanced, LOG, context);
            if (restored) {
                LOG.info("[MOTM] Runtime perk crafting stack enhanced: context=" + context
                        + " itemId=" + itemId
                        + " slot=" + slot
                        + " label=\"" + label + "\""
                        + " maxDurabilityMultiplier=" + format(maxDurabilityMultiplier));
            }
            return restored;
        }
        return false;
    }

    private short findEnhanceableSlot(ItemContainer container, String itemId, String metadataKey) {
        if (container == null) {
            return -1;
        }
        short capacity = container.getCapacity();
        for (short slot = 0; slot < capacity; slot++) {
            ItemStack stack = container.getItemStack(slot);
            if (stack == null || stack.isEmpty() || !itemId.equals(stack.getItemId())) {
                continue;
            }
            if (hasMetadataFlag(stack, metadataKey)) {
                continue;
            }
            return slot;
        }
        return -1;
    }

    private boolean hasMetadataFlag(ItemStack stack, String key) {
        return stack != null
                && stack.getMetadata() != null
                && key != null
                && stack.getMetadata().containsKey(key);
    }

    private MenteesMod.EcoFriendlyTreeResult placeEcoFriendlyTree(PlayerData playerData, Player player, Vector3i target) {
        World world = player != null ? player.getWorld() : null;
        Ref<EntityStore> playerRef = player != null ? player.getReference() : null;
        Store<EntityStore> store = playerRef != null ? playerRef.getStore() : null;
        if (world == null || store == null || target == null) {
            return new MenteesMod.EcoFriendlyTreeResult(false, "missing world/store/target");
        }
        BlockType targetType = blockType(world, target.x, target.y, target.z);
        if (!isNaturalEcoSurface(targetType)) {
            return new MenteesMod.EcoFriendlyTreeResult(false, "target surface is not natural earth: "
                    + blockTypeId(targetType));
        }
        for (int y = 1; y <= 7; y++) {
            if (blockMaterial(world, target.x, target.y + y, target.z) == BlockMaterial.Solid) {
                return new MenteesMod.EcoFriendlyTreeResult(false, "not enough open space above target");
            }
        }

        int trunk = resolveBlockType(
                "Wood_Oak_Trunk", "Wood_Beech_Trunk", "Wood_Birch_Trunk",
                "Wood_Ash_Trunk", "Wood_Apple_Trunk", "Wood_Aspen_Trunk");
        int leaves = resolveBlockType(
                "Plant_Leaves_Oak", "Plant_Leaves_Beech", "Plant_Leaves_Birch",
                "Plant_Leaves_Ash", "Plant_Leaves_Apple", "Plant_Leaves_Aspen");
        if (!usableBlock(trunk) || !usableBlock(leaves)) {
            return new MenteesMod.EcoFriendlyTreeResult(false, "tree block assets unavailable trunk="
                    + trunk + " leaves=" + leaves);
        }

        int x = target.x;
        int baseY = target.y + 1;
        int z = target.z;
        BlockSelection tree = new BlockSelection();
        tree.setPosition(x, baseY, z);
        tree.setAnchorAtWorldPos(x, baseY, z);
        for (int y = 0; y < 5; y++) {
            tree.addBlockAtWorldPos(x, baseY + y, z, trunk, 0, 0, 0);
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                int distance = Math.abs(dx) + Math.abs(dz);
                if (distance <= 3) {
                    tree.addBlockAtWorldPos(x + dx, baseY + 4, z + dz, leaves, 0, 0, 0);
                }
                if (distance <= 2) {
                    tree.addBlockAtWorldPos(x + dx, baseY + 5, z + dz, leaves, 0, 0, 0);
                }
            }
        }
        tree.addBlockAtWorldPos(x, baseY + 6, z, leaves, 0, 0, 0);

        try {
            BlockSelection original = tree.place(null, world, new Vector3i(0, 0, 0), BlockMask.EMPTY);
            int pushed = pushNpcsOutward(store, new Vector3d(x + 0.5, baseY, z + 0.5), 4.0, 3.25);
            long clearAtTick = tickCounter + 10L * TICKS_PER_SECOND;
            activeEcoTrees.add(new TemporaryEcoTree(playerData.getPlayerId(), world, original, clearAtTick));
            return new MenteesMod.EcoFriendlyTreeResult(true,
                    "temporary tree blocks=" + tree.getBlockCount()
                            + " pushed=" + pushed
                            + " clearsAtTick=" + clearAtTick
                            + " noDrops=true");
        } catch (Throwable e) {
            LOG.warning("[MOTM] Runtime perk eco-friendly tree placement failed safely: " + e.getMessage());
            return new MenteesMod.EcoFriendlyTreeResult(false, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private void tickEcoTrees() {
        Iterator<TemporaryEcoTree> iterator = activeEcoTrees.iterator();
        while (iterator.hasNext()) {
            TemporaryEcoTree tree = iterator.next();
            if (tree.clearAtTick > tickCounter) {
                continue;
            }
            iterator.remove();
            try {
                tree.originalSelection.place(null, tree.world, new Vector3i(0, 0, 0), BlockMask.EMPTY);
                LOG.info("[MOTM] Runtime perk eco-friendly tree cleared: player="
                        + tree.playerId + " blockDrops=false");
            } catch (Throwable e) {
                LOG.warning("[MOTM] Runtime perk eco-friendly tree clear failed safely: " + e.getMessage());
            }
        }
    }

    private boolean isNaturalEcoSurface(BlockType blockType) {
        if (blockType == null || blockType.getMaterial() != BlockMaterial.Solid) {
            return false;
        }
        String id = blockTypeId(blockType).toLowerCase(Locale.ROOT);
        return id.contains("grass")
                || id.contains("dirt")
                || id.contains("soil")
                || id.contains("moss")
                || id.contains("mud")
                || id.contains("peat");
    }

    private Vector3i findEcoSurfaceBelow(World world, int x, int startY, int z, int maxDepth) {
        for (int y = startY; y >= startY - Math.max(0, maxDepth); y--) {
            if (isNaturalEcoSurface(blockType(world, x, y, z))) {
                return new Vector3i(x, y, z);
            }
        }
        return null;
    }

    private String blockTypeId(BlockType blockType) {
        if (blockType == null) {
            return "";
        }
        for (String methodName : List.of("getId", "getName")) {
            try {
                Object value = blockType.getClass().getMethod(methodName).invoke(blockType);
                if (value != null) {
                    return String.valueOf(value);
                }
            } catch (ReflectiveOperationException ignored) {
                // Try the next known accessor. The Hytale API has moved this name across builds.
            }
        }
        return blockType.toString();
    }

    private BlockType blockType(World world, int x, int y, int z) {
        if (world == null) {
            return null;
        }
        long chunkIndex = ChunkUtil.indexChunkFromBlock(x, z);
        WorldChunk chunk = world.getChunkIfLoaded(chunkIndex);
        if (chunk == null) {
            chunk = world.getChunkIfInMemory(chunkIndex);
        }
        if (chunk == null) {
            return null;
        }
        int localX = ChunkUtil.localCoordinate(x);
        int localZ = ChunkUtil.localCoordinate(z);
        return chunk.getBlockType(localX, y, localZ);
    }

    private BlockMaterial blockMaterial(World world, int x, int y, int z) {
        BlockType blockType = blockType(world, x, y, z);
        return blockType != null ? blockType.getMaterial() : BlockMaterial.Empty;
    }

    private int resolveBlockType(String... blockIds) {
        for (String blockId : blockIds) {
            try {
                int id = BlockType.getBlockIdOrUnknown(blockId, "MOTM Eco-friendly perk tree");
                if (usableBlock(id)) {
                    return id;
                }
            } catch (Throwable e) {
                LOG.warning("[MOTM] Eco-friendly block candidate skipped: id=" + blockId
                        + " error=" + e.getMessage());
            }
        }
        return BlockType.UNKNOWN_ID;
    }

    private boolean usableBlock(int blockTypeId) {
        return blockTypeId != BlockType.UNKNOWN_ID && blockTypeId != BlockType.EMPTY_ID;
    }

    private String firstCraftProofArmorId() {
        for (String candidate : List.of(
                "Armor_Iron_Chestplate",
                "Armor_Iron_Chest",
                "Armor_Iron_Helmet",
                "Armor_Leather_Chestplate",
                "Armor_Leather_Helmet")) {
            Item explicit = Item.getAssetMap().getAsset(candidate);
            if (explicit != null && explicit.getArmor() != null) {
                return candidate;
            }
        }
        return null;
    }

    private String firstCraftProofToolId() {
        for (String candidate : List.of("Tool_Pickaxe_Iron", "Weapon_Sword_Iron", "Iron_Pickaxe")) {
            Item explicit = Item.getAssetMap().getAsset(candidate);
            if (isToolsmithEligibleItem(candidate, explicit)) {
                return candidate;
            }
        }
        return null;
    }

    private int pushNpcsOutward(Store<EntityStore> store, Vector3d center, double radius, double distance) {
        int pushed = 0;
        for (Ref<EntityStore> target : nearbyNpcs(store, center, radius)) {
            Vector3d position = position(target, store);
            if (position == null) {
                continue;
            }
            Vector3d direction = new Vector3d(position.x - center.x, 0.0, position.z - center.z);
            if (!direction.isFinite() || direction.length() < 0.01) {
                direction.set(1.0, 0.0, 0.0);
            } else {
                direction.normalize();
            }
            Vector3d destination = new Vector3d(
                    center.x + (direction.x * distance),
                    position.y,
                    center.z + (direction.z * distance)
            );
            try {
                NPCEntity npc = store.getComponent(target, NPCEntity.getComponentType());
                if (npc != null) {
                    npc.moveTo(target, destination.x, destination.y, destination.z, store);
                    pushed++;
                }
            } catch (Throwable e) {
                LOG.warning("[MOTM] Eco-friendly tree push failed safely: " + e.getMessage());
            }
        }
        return pushed;
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
                PlayerRef universePlayerRef = store.getComponent(playerRef, PlayerRef.getComponentType());
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
        lowHealthLatchByPlayer.remove(playerId);
        ghostAlliesByPlayer.remove(playerId);
        activeIgnites.removeIf(dot -> playerId.equals(dot.ownerPlayerId));
        Iterator<TemporaryEcoTree> ecoIterator = activeEcoTrees.iterator();
        while (ecoIterator.hasNext()) {
            TemporaryEcoTree tree = ecoIterator.next();
            if (!playerId.equals(tree.playerId)) {
                continue;
            }
            ecoIterator.remove();
            try {
                tree.originalSelection.place(null, tree.world, new Vector3i(0, 0, 0), BlockMask.EMPTY);
                LOG.info("[MOTM] Runtime perk eco-friendly tree cleared by player reset: player="
                        + playerId + " blockDrops=false");
            } catch (Throwable e) {
                LOG.warning("[MOTM] Runtime perk eco-friendly reset clear failed safely: " + e.getMessage());
            }
        }
    }

    private void expireTemporaryDamageReductions() {
        temporaryDamageReductionByPlayer.entrySet().removeIf(entry -> entry.getValue().expireAtTick <= tickCounter);
    }

    private void updateLowHealthLatches(PlayerData player, Ref<EntityStore> playerRef, Store<EntityStore> store) {
        if (player == null || player.getPlayerId() == null) {
            return;
        }
        double maxHealth = maxHealth(playerRef, store);
        if (maxHealth <= 0.0) {
            return;
        }
        double fraction = currentHealth(playerRef, store) / maxHealth;
        LowHealthLatch latch = lowHealthLatchByPlayer.computeIfAbsent(player.getPlayerId(), ignored -> new LowHealthLatch());
        if (fraction > 0.10) {
            latch.neptuneArmed = true;
        }
        if (fraction > 0.20) {
            latch.freezingWindsArmed = true;
        }
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
            if (tickCounter - state.lastSwimmingTick > 10L) {
                state.swimStartTick = -1L;
            }
            return 0.0;
        }
        state.lastSwimmingTick = tickCounter;
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
        double projectedFraction = projected / maxHealth;
        LowHealthLatch latch = lowHealthLatchByPlayer.computeIfAbsent(playerId, ignored -> new LowHealthLatch());
        if (hasPerk(target, HYDRO_NEPTUNES_GRACE)
                && projectedFraction <= 0.10
                && latch.neptuneArmed
                && !onCooldown(playerId, HYDRO_NEPTUNES_GRACE)) {
            double healed = healEntity(targetRef, store, maxHealth * 0.40);
            latch.neptuneArmed = false;
            setCooldown(playerId, HYDRO_NEPTUNES_GRACE, 25L * TICKS_PER_SECOND);
            applyEffectById(targetRef, store, "MOTM_Hydro_Impact");
            LOG.info("[MOTM] Runtime perk proc: neptunes_grace heal=" + format(healed)
                    + " thresholdFresh=true visual=blue_bubble_pulse cooldownSeconds=25 player=" + playerId);
        } else if (hasPerk(target, HYDRO_NEPTUNES_GRACE) && projectedFraction <= 0.10) {
            latch.neptuneArmed = false;
        }
        if (hasPerk(target, HYDRO_FREEZING_WINDS)
                && projectedFraction <= 0.20
                && latch.freezingWindsArmed
                && !onCooldown(playerId, HYDRO_FREEZING_WINDS)) {
            int targets = applyFreezingWinds(playerId, targetRef, store);
            latch.freezingWindsArmed = false;
            setCooldown(playerId, HYDRO_FREEZING_WINDS, 15L * TICKS_PER_SECOND);
            LOG.info("[MOTM] Runtime perk proc: freezing_winds targets=" + targets
                    + " thresholdFresh=true visual=outward_ice_snow_burst cooldownSeconds=15 player=" + playerId);
        } else if (hasPerk(target, HYDRO_FREEZING_WINDS) && projectedFraction <= 0.20) {
            latch.freezingWindsArmed = false;
        }
    }

    private int applyFreezingWinds(String playerId, Ref<EntityStore> playerRef, Store<EntityStore> store) {
        Vector3d center = position(playerRef, store);
        if (center == null) {
            return 0;
        }
        applyEffectById(playerRef, store, FREEZING_WINDS_EFFECT_ID);
        int targets = 0;
        for (Ref<EntityStore> target : nearbyNpcs(store, center, 5.0)) {
            String entityId = entityId(target, store);
            if (entityId == null) {
                continue;
            }
            mod.getStatusEffectManager().applyEffect(entityId,
                    new StatusEffect(StatusEffect.Type.SLOW, 5 * TICKS_PER_SECOND, 0.50, playerId, HYDRO_FREEZING_WINDS));
            applyEffectById(target, store, FREEZING_WINDS_EFFECT_ID);
            pushNpcAway(target, store, center, 5.75);
            targets++;
        }
        return targets;
    }

    private void pushNpcAway(Ref<EntityStore> target, Store<EntityStore> store, Vector3d center, double distance) {
        Vector3d position = position(target, store);
        if (position == null || center == null) {
            return;
        }
        Vector3d direction = new Vector3d(position.x - center.x, 0.0, position.z - center.z);
        if (!direction.isFinite() || direction.length() < 0.01) {
            direction.set(1.0, 0.0, 0.0);
        } else {
            direction.normalize();
        }
        Vector3d destination = new Vector3d(
                center.x + (direction.x * distance),
                position.y,
                center.z + (direction.z * distance)
        );
        try {
            NPCEntity npc = store.getComponent(target, NPCEntity.getComponentType());
            if (npc != null) {
                npc.moveTo(target, destination.x, destination.y, destination.z, store);
            }
        } catch (Throwable e) {
            LOG.warning("[MOTM] Freezing Winds knockback failed safely: " + e.getMessage());
        }
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
        MotmNpcRoles.applyRole(ghost, GHOST_ROLE_ID,
                HytaleAssetResolver.resolveRenderlessVisualProxyRoleId(), LOG);
        ghost.setDespawnTime(60.0f);
        Vector3d spawn = new Vector3d(ownerPosition.x + 1.5, ownerPosition.y + 1.0, ownerPosition.z);
        world.spawnEntity(ghost, spawn, new Rotation3f(0f, 0f, 0f));
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

    private String resolvePerkName(String perkId) {
        var perk = mod.getDataLoader().getPerkByIdAnyClass(perkId);
        if (perk != null && perk.getName() != null && !perk.getName().isBlank()) {
            return perk.getName();
        }
        return titleize(perkId);
    }

    private boolean isHudActive(PlayerData player, String perkId) {
        if (player == null || perkId == null) {
            return false;
        }

        String playerId = player.getPlayerId();
        return switch (perkId) {
            case AERO_ACCELERATE -> {
                SprintState state = sprintStateByPlayer.get(playerId);
                yield state != null && state.sprintStartTick >= 0L;
            }
            case AERO_BUNNY_HOP -> {
                SprintState state = sprintStateByPlayer.get(playerId);
                yield state != null && state.bunnyCharges > 0;
            }
            case AERO_BIG_STRIDES -> {
                SprintState state = sprintStateByPlayer.get(playerId);
                yield state != null
                        && state.sprintStartTick >= 0L
                        && tickCounter - state.sprintStartTick <= 3L * TICKS_PER_SECOND;
            }
            case HYDRO_SEMIAQUATIC -> {
                SwimState state = swimStateByPlayer.get(playerId);
                yield state != null && state.swimStartTick >= 0L;
            }
            case HYDRO_RAINY_DAY -> {
                long lastRegenTick = rainyDayLastRegenTickByPlayer.getOrDefault(playerId, Long.MIN_VALUE);
                yield tickCounter - lastRegenTick <= TICKS_PER_SECOND + 5L;
            }
            case CORRUPTUS_IGNITE -> activeIgnites.stream()
                    .anyMatch(dot -> playerId.equals(dot.ownerPlayerId) && dot.expireAtTick > tickCounter);
            case CORRUPTUS_DESPERATION -> healthFraction(playerId) < 0.70;
            case TERRA_ECO_FRIENDLY -> {
                TemporaryDamageReduction reduction = temporaryDamageReductionByPlayer.get(playerId);
                yield reduction != null && reduction.expireAtTick > tickCounter;
            }
            case TERRA_MOLE_MAN -> mod.getClassPassiveManager().isTerraCaveVisionActive(playerId);
            case AERO_TWINKLETOES, AERO_SHARPSHOOTER, HYDRO_BIG_LUNGS,
                    CORRUPTUS_HAUNTING, CORRUPTUS_VAMPIRISM,
                    TERRA_HEAVYWEIGHT, TERRA_BLACKSMITH, TERRA_TOOLSMITH -> false;
            default -> false;
        };
    }

    private double cooldownSecondsRemaining(String playerId, String key) {
        long readyTick = cooldownUntilTickByPlayer.getOrDefault(playerId, Map.of()).getOrDefault(key, 0L);
        return Math.max(0.0, (readyTick - tickCounter) / (double) TICKS_PER_SECOND);
    }

    private String titleize(String raw) {
        String normalized = raw == null ? "" : raw.replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isBlank()) {
            return "Perk";
        }
        StringBuilder result = new StringBuilder();
        for (String part : normalized.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                result.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return result.toString();
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
        private long lastSwimmingTick = Long.MIN_VALUE;
    }

    private static final class TemporaryDamageReduction {
        private final double reduction;
        private final long expireAtTick;

        private TemporaryDamageReduction(double reduction, long expireAtTick) {
            this.reduction = reduction;
            this.expireAtTick = expireAtTick;
        }
    }

    private static final class LowHealthLatch {
        private boolean neptuneArmed = true;
        private boolean freezingWindsArmed = true;
    }

    private static final class TemporaryEcoTree {
        private final String playerId;
        private final World world;
        private final BlockSelection originalSelection;
        private final long clearAtTick;

        private TemporaryEcoTree(String playerId, World world, BlockSelection originalSelection, long clearAtTick) {
            this.playerId = playerId;
            this.world = world;
            this.originalSelection = originalSelection;
            this.clearAtTick = clearAtTick;
        }
    }

    private record RainState(boolean raining, int weatherIndex, String weatherId) {}

    public record RuntimePerkHudEntry(
            String id,
            String name,
            double cooldownSeconds,
            boolean active,
            String iconPath,
            String framePath,
            String state,
            String counterText
    ) {}

    private record CraftedOutput(String itemId, int quantity) {}

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
