package com.motm.manager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.MenteesMod;
import com.motm.model.Perk;
import com.motm.model.PlayerData;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

public class PlayerStatModifierManager {

    private static final Logger LOG = Logger.getLogger("MOTM");
    private static final String MODIFIER_PREFIX = "motm_perk_";
    private static final String STAT_MODIFIER_PREFIX = "motm_stat_";

    private final MenteesMod mod;
    private final Map<String, Map<String, Integer>> ownedModifiers = new HashMap<>();
    private final Map<String, Map<String, Integer>> ownedProgressionModifiers = new HashMap<>();
    private final Map<String, MovementSettingsSnapshot> progressionMovementSnapshots = new HashMap<>();

    public PlayerStatModifierManager(MenteesMod mod) {
        this.mod = mod;
    }

    public synchronized void rebuildFromPerks(PlayerData player, List<Perk> perks) {
        if (player == null || player.getPlayerId() == null) {
            return;
        }

        String playerId = player.getPlayerId();
        clearOwnedModifiers(playerId);
        mod.clearPerkTriggers(playerId);

        Player runtimePlayer = mod.getRuntimePlayer(playerId);
        if (runtimePlayer == null) {
            LOG.info("[MOTM] Rebuilt perk stat modifiers: player=" + playerId
                    + " perks=" + (perks == null ? 0 : perks.size()) + " modifiersApplied=0 runtimePlayer=false");
            return;
        }

        EntityStatMap statMap = getStatMap(runtimePlayer);
        if (statMap == null) {
            LOG.warning("[MOTM] Could not rebuild perk stat modifiers: missing EntityStatMap for player=" + playerId);
            return;
        }

        int applied = 0;
        if (perks != null) {
            for (Perk perk : perks) {
                if (perk == null || perk.getEffects() == null) {
                    continue;
                }
                for (Perk.Effect effect : perk.getEffects()) {
                    if (applyOneEffect(playerId, statMap, perk, effect)) {
                        applied++;
                    }
                }
            }
        }

        LOG.info("[MOTM] Rebuilt perk stat modifiers: player=" + playerId
                + " perks=" + (perks == null ? 0 : perks.size())
                + " modifiersApplied=" + applied);
    }

    public synchronized void clearForPlayer(String playerId) {
        if (playerId == null) {
            return;
        }
        clearOwnedModifiers(playerId);
        clearProgressionModifiers(playerId);
        mod.clearPerkTriggers(playerId);
    }

    public synchronized void rebuildFromProgression(PlayerData player) {
        if (player == null || player.getPlayerId() == null) {
            return;
        }

        String playerId = player.getPlayerId();
        clearProgressionModifiers(playerId);

        Player runtimePlayer = mod.getRuntimePlayer(playerId);
        if (runtimePlayer == null) {
            LOG.info("[MOTM] Rebuilt progression stat modifiers: player=" + playerId
                    + " runtimePlayer=false");
            return;
        }

        EntityStatMap statMap = getStatMap(runtimePlayer);
        int applied = 0;
        if (statMap != null) {
            applied += applyProgressionStat(playerId, statMap, "vigor_health",
                    DefaultEntityStatTypes.getHealth(),
                    mod.getLevelingManager().getVigorHealthMultiplier(player));
            applied += applyProgressionStat(playerId, statMap, "endurance_stamina",
                    DefaultEntityStatTypes.getStamina(),
                    mod.getLevelingManager().getEnduranceStaminaMultiplier(player));
        }

        boolean speedApplied = applyProgressionMovement(playerId, runtimePlayer, player);
        LOG.info("[MOTM] Rebuilt progression stat modifiers: player=" + playerId
                + " allocated=" + player.getStatAllocation().totalAllocated()
                + " nativeModifiers=" + applied
                + " movementApplied=" + speedApplied
                + " unspent=" + player.getUnspentStatPoints());
    }

    public double getDamageReduction(PlayerData player) {
        return mod.getLevelingManager().getVigorDamageReduction(player);
    }

    public double getDamageMultiplier(PlayerData player) {
        return mod.getLevelingManager().getTenacityDamageMultiplier(player);
    }

    public double getXpMultiplier(PlayerData player) {
        return mod.getLevelingManager().getLuckXpMultiplier(player);
    }

    private boolean applyOneEffect(String playerId, EntityStatMap statMap, Perk perk, Perk.Effect effect) {
        if (effect == null || effect.getType() == null) {
            return false;
        }

        String type = normalize(effect.getType());
        return switch (type) {
            case "stat_increase" -> applyStat(playerId, statMap, perk, effect, effect.getStat(), effect.getValue(),
                    StaticModifier.CalculationType.ADDITIVE);
            case "stat_multiplier" -> applyStat(playerId, statMap, perk, effect, effect.getStat(), effect.getValue(),
                    StaticModifier.CalculationType.MULTIPLICATIVE);
            case "damage_reduction" -> applyStat(playerId, statMap, perk, effect, "health", 1.0 + effect.getValue(),
                    StaticModifier.CalculationType.MULTIPLICATIVE);
            case "damage_increase" -> {
                LOG.info("[MOTM] Perk damage_increase registered log-only: player=" + playerId
                        + " perk=" + perk.getId() + " value=" + effect.getValue());
                yield false;
            }
            case "stamina_and_breath" -> {
                boolean stamina = applyStat(playerId, statMap, perk, effect, "stamina", 1.10,
                        StaticModifier.CalculationType.MULTIPLICATIVE);
                boolean oxygen = applyStat(playerId, statMap, perk, effect, "oxygen", 1.10,
                        StaticModifier.CalculationType.MULTIPLICATIVE);
                yield stamina || oxygen;
            }
            case "on_hit", "on_kill" -> {
                mod.registerPerkTrigger(playerId, perk, effect);
                yield true;
            }
            default -> {
                LOG.fine("[MOTM] Perk effect registered log-only: player=" + playerId
                        + " perk=" + perk.getId() + " type=" + type);
                yield false;
            }
        };
    }

    private boolean applyStat(String playerId, EntityStatMap statMap, Perk perk, Perk.Effect effect,
                              String rawStatName, double rawValue, StaticModifier.CalculationType calculationType) {
        StatTarget target = resolveStatTarget(rawStatName);
        if (target == null) {
            LOG.info("[MOTM] Perk stat effect skipped: player=" + playerId
                    + " perk=" + perk.getId() + " stat=" + rawStatName + " value=" + rawValue);
            return false;
        }

        double value = rawValue;
        StaticModifier.CalculationType effectiveType = calculationType;
        if (effectiveType == StaticModifier.CalculationType.ADDITIVE
                && target.healthLike()
                && Math.abs(value) > 0.0
                && Math.abs(value) < 1.0) {
            value = 1.0 + value;
            effectiveType = StaticModifier.CalculationType.MULTIPLICATIVE;
        }

        String modifierId = buildModifierId(perk.getId(), effect.getType(), target.name());
        statMap.removeModifier(target.statType(), modifierId);
        statMap.putModifier(target.statType(), modifierId,
                new StaticModifier(Modifier.ModifierTarget.MAX, effectiveType, (float) value));
        ownedModifiers.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(modifierId, target.statType());

        LOG.info("[MOTM] Perk stat modifier applied: player=" + playerId
                + " perk=" + perk.getId()
                + " stat=" + target.name()
                + " type=" + effectiveType
                + " value=" + value);
        return true;
    }

    private void clearOwnedModifiers(String playerId) {
        Map<String, Integer> modifiers = ownedModifiers.remove(playerId);
        if (modifiers == null || modifiers.isEmpty()) {
            return;
        }

        Player runtimePlayer = mod.getRuntimePlayer(playerId);
        EntityStatMap statMap = getStatMap(runtimePlayer);
        if (statMap == null) {
            return;
        }

        for (Map.Entry<String, Integer> entry : modifiers.entrySet()) {
            statMap.removeModifier(entry.getValue(), entry.getKey());
        }
    }

    private void clearProgressionModifiers(String playerId) {
        restoreProgressionMovement(playerId);
        Map<String, Integer> modifiers = ownedProgressionModifiers.remove(playerId);
        if (modifiers == null || modifiers.isEmpty()) {
            return;
        }

        Player runtimePlayer = mod.getRuntimePlayer(playerId);
        EntityStatMap statMap = getStatMap(runtimePlayer);
        if (statMap == null) {
            return;
        }

        for (Map.Entry<String, Integer> entry : modifiers.entrySet()) {
            statMap.removeModifier(entry.getValue(), entry.getKey());
        }
    }

    private int applyProgressionStat(String playerId,
                                     EntityStatMap statMap,
                                     String id,
                                     int statType,
                                     double multiplier) {
        String modifierId = STAT_MODIFIER_PREFIX + id;
        statMap.removeModifier(statType, modifierId);
        if (!Double.isFinite(multiplier) || Math.abs(multiplier - 1.0) < 0.0001) {
            return 0;
        }
        statMap.putModifier(statType, modifierId,
                new StaticModifier(Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.MULTIPLICATIVE,
                        (float) multiplier));
        ownedProgressionModifiers.computeIfAbsent(playerId, ignored -> new HashMap<>()).put(modifierId, statType);
        return 1;
    }

    private boolean applyProgressionMovement(String playerId, Player runtimePlayer, PlayerData player) {
        if (runtimePlayer == null || player == null) {
            return false;
        }
        double multiplier = mod.getLevelingManager().getAgilitySpeedMultiplier(player);
        if (!Double.isFinite(multiplier) || Math.abs(multiplier - 1.0) < 0.0001) {
            return false;
        }
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return false;
        }
        MovementManager movementManager = playerRef.getStore().getComponent(playerRef, MovementManager.getComponentType());
        if (movementManager == null || movementManager.getSettings() == null) {
            return false;
        }
        var settings = movementManager.getSettings();
        MovementSettingsSnapshot snapshot = progressionMovementSnapshots.computeIfAbsent(
                playerId,
                ignored -> MovementSettingsSnapshot.from(settings)
        );
        float value = (float) multiplier;
        settings.baseSpeed = snapshot.baseSpeed() * value;
        settings.forwardWalkSpeedMultiplier = snapshot.forwardWalkSpeedMultiplier() * value;
        settings.backwardWalkSpeedMultiplier = snapshot.backwardWalkSpeedMultiplier() * value;
        settings.strafeWalkSpeedMultiplier = snapshot.strafeWalkSpeedMultiplier() * value;
        settings.forwardRunSpeedMultiplier = snapshot.forwardRunSpeedMultiplier() * value;
        settings.backwardRunSpeedMultiplier = snapshot.backwardRunSpeedMultiplier() * value;
        settings.strafeRunSpeedMultiplier = snapshot.strafeRunSpeedMultiplier() * value;
        settings.forwardSprintSpeedMultiplier = snapshot.forwardSprintSpeedMultiplier() * value;
        settings.acceleration = snapshot.acceleration() * value;
        updateMovementManager(playerRef, playerRef.getStore(), movementManager);
        return true;
    }

    private void restoreProgressionMovement(String playerId) {
        MovementSettingsSnapshot snapshot = progressionMovementSnapshots.remove(playerId);
        if (snapshot == null) {
            return;
        }
        Player runtimePlayer = mod.getRuntimePlayer(playerId);
        if (runtimePlayer == null) {
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
        var settings = movementManager.getSettings();
        settings.baseSpeed = snapshot.baseSpeed();
        settings.forwardWalkSpeedMultiplier = snapshot.forwardWalkSpeedMultiplier();
        settings.backwardWalkSpeedMultiplier = snapshot.backwardWalkSpeedMultiplier();
        settings.strafeWalkSpeedMultiplier = snapshot.strafeWalkSpeedMultiplier();
        settings.forwardRunSpeedMultiplier = snapshot.forwardRunSpeedMultiplier();
        settings.backwardRunSpeedMultiplier = snapshot.backwardRunSpeedMultiplier();
        settings.strafeRunSpeedMultiplier = snapshot.strafeRunSpeedMultiplier();
        settings.forwardSprintSpeedMultiplier = snapshot.forwardSprintSpeedMultiplier();
        settings.acceleration = snapshot.acceleration();
        updateMovementManager(playerRef, playerRef.getStore(), movementManager);
    }

    private void updateMovementManager(Ref<EntityStore> entityRef,
                                       com.hypixel.hytale.component.Store<EntityStore> store,
                                       MovementManager movementManager) {
        PlayerRef universePlayerRef = store.getComponent(entityRef, PlayerRef.getComponentType());
        if (universePlayerRef != null && universePlayerRef.getPacketHandler() != null) {
            movementManager.update(universePlayerRef.getPacketHandler());
        }
    }

    private EntityStatMap getStatMap(Player runtimePlayer) {
        if (runtimePlayer == null) {
            return null;
        }
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return null;
        }
        return playerRef.getStore().getComponent(playerRef, EntityStatMap.getComponentType());
    }

    private StatTarget resolveStatTarget(String statName) {
        String normalized = normalize(statName);
        return switch (normalized) {
            case "health", "hp", "max_health", "max_hp", "maxhealth" ->
                    new StatTarget("health", DefaultEntityStatTypes.getHealth(), true);
            case "oxygen", "air" ->
                    new StatTarget("oxygen", DefaultEntityStatTypes.getOxygen(), false);
            case "stamina" ->
                    new StatTarget("stamina", DefaultEntityStatTypes.getStamina(), false);
            case "mana" ->
                    new StatTarget("mana", DefaultEntityStatTypes.getMana(), false);
            case "signature_energy", "signatureenergy", "energy" ->
                    new StatTarget("signature_energy", DefaultEntityStatTypes.getSignatureEnergy(), false);
            default -> null;
        };
    }

    private String buildModifierId(String perkId, String effectType, String statName) {
        return MODIFIER_PREFIX
                + normalize(perkId).replace(' ', '_')
                + "_"
                + normalize(effectType).replace(' ', '_')
                + "_"
                + normalize(statName).replace(' ', '_');
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private record StatTarget(String name, int statType, boolean healthLike) {
    }

    private record MovementSettingsSnapshot(
            float baseSpeed,
            float forwardWalkSpeedMultiplier,
            float backwardWalkSpeedMultiplier,
            float strafeWalkSpeedMultiplier,
            float forwardRunSpeedMultiplier,
            float backwardRunSpeedMultiplier,
            float strafeRunSpeedMultiplier,
            float forwardSprintSpeedMultiplier,
            float acceleration
    ) {
        static MovementSettingsSnapshot from(com.hypixel.hytale.protocol.MovementSettings settings) {
            return new MovementSettingsSnapshot(
                    settings.baseSpeed,
                    settings.forwardWalkSpeedMultiplier,
                    settings.backwardWalkSpeedMultiplier,
                    settings.strafeWalkSpeedMultiplier,
                    settings.forwardRunSpeedMultiplier,
                    settings.backwardRunSpeedMultiplier,
                    settings.strafeRunSpeedMultiplier,
                    settings.forwardSprintSpeedMultiplier,
                    settings.acceleration
            );
        }
    }
}
