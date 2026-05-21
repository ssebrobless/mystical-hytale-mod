package com.motm.manager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
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

    private final MenteesMod mod;
    private final Map<String, Map<String, Integer>> ownedModifiers = new HashMap<>();

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
        mod.clearPerkTriggers(playerId);
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
}
