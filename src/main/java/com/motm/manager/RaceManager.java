package com.motm.manager;

import com.motm.model.PlayerData;
import com.motm.model.RaceData;
import com.motm.model.StatusEffect;
import com.motm.util.DataLoader;

import java.util.Map;
import java.util.logging.Logger;

/**
 * Applies race passives and tracks race-specific mechanics on the player model.
 *
 * Hytale entity stat hooks are still pending, so this manager records the passive
 * state on PlayerData and mirrors the most important combat passives into the
 * StatusEffectManager for the current runtime session.
 */
public class RaceManager {

    private static final Logger LOG = Logger.getLogger("MOTM");
    private static final int PERMANENT_EFFECT_TICKS = Integer.MAX_VALUE;

    private final DataLoader dataLoader;

    public RaceManager(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }

    public void applyRaceBonuses(PlayerData player, StatusEffectManager statusEffectManager) {
        if (player == null) {
            return;
        }

        player.clearRaceBonuses();
        clearManagedEffects(player.getPlayerId(), statusEffectManager);

        if (player.getRace() == null) {
            return;
        }

        RaceData race = dataLoader.getRaceById(player.getRace());
        if (race == null) {
            LOG.warning("[MOTM] Could not apply race bonuses for unknown race: " + player.getRace());
            return;
        }

        player.setRaceHpBonus(race.getHpBonus());
        if (race.getHpBonus() != 0) {
            player.getRaceStatBonuses().put("max_health_flat", (double) race.getHpBonus());
        }

        addPercentBonus(player.getRaceDamageReduction(), "all", race.getDamageReductionPercent());
        addPercentBonus(player.getRaceDamageIncrease(), "all", race.getDamagePercent());
        addPercentBonus(player.getRaceDamageIncrease(), "fire", race.getPercentBonus("fire_damage"));
        addPercentBonus(player.getRaceDamageIncrease(), "stealth", race.getPercentBonus("stealth_damage"));

        addPercentBonus(player.getRaceStatBonuses(), "magic_resist", race.getPercentBonus("magic_resist"));
        addPercentBonus(player.getRaceStatBonuses(), "fire_resist", race.getPercentBonus("fire_resist"));
        addPercentBonus(player.getRaceStatBonuses(), "elemental_resist", race.getPercentBonus("elemental_resist"));
        addPercentBonus(player.getRaceStatBonuses(), "knockback_resist", race.getPercentBonus("knockback_resist"));
        addPercentBonus(player.getRaceStatBonuses(), "all_stats", race.getPercentBonus("all_stats"));

        double healingReceived = race.getHealingPercent();
        healingReceived += race.getPercentBonus("all_stats");
        healingReceived -= race.getPercentBonus("healing_reduction");
        player.setRaceHealingReceivedBonus(healingReceived);

        player.setRaceCritChanceBonus(race.getCritChancePercent());
        player.setRaceCooldownReductionSeconds(race.getCooldownReduction());

        if (race.getEvasionPercent() > 0) {
            player.getRaceStatBonuses().put("evasion", race.getEvasionPercent());
            statusEffectManager.applyEffect(
                    player.getPlayerId(),
                    new StatusEffect(StatusEffect.Type.EVASION, PERMANENT_EFFECT_TICKS,
                            race.getEvasionPercent(), "race:" + race.getId(), race.getPassive())
            );
        }

        if (race.getDamageReductionPercent() > 0) {
            statusEffectManager.applyEffect(
                    player.getPlayerId(),
                    new StatusEffect(StatusEffect.Type.DEFENSE_BUFF, PERMANENT_EFFECT_TICKS,
                            race.getDamageReductionPercent(), "race:" + race.getId(), race.getPassive())
            );
        }

        double attackBuff = race.getDamagePercent() + race.getPercentBonus("all_stats");
        if (attackBuff > 0) {
            statusEffectManager.applyEffect(
                    player.getPlayerId(),
                    new StatusEffect(StatusEffect.Type.ATTACK_BUFF, PERMANENT_EFFECT_TICKS,
                            attackBuff, "race:" + race.getId(), race.getPassive())
            );
        }

        if (race.isPoisonImmune()) {
            player.getImmunities().add("poison");
        }

        for (Map.Entry<String, Object> bonus : race.getBonuses().entrySet()) {
            if (Boolean.TRUE.equals(bonus.getValue())) {
                player.getRaceSpecialMechanics().add(bonus.getKey());
            }
        }

        if (race.getSpecial() != null && !race.getSpecial().isBlank()) {
            player.getRaceSpecialMechanics().add(race.getSpecial());
        }

        if (race.hasBreathWeapon()) {
            player.getRaceSpecialMechanics().add("breath_weapon");
        }

        LOG.info("[MOTM] Applied race bonuses for " + player.getPlayerName()
                + ": " + race.getName() + " (HP "
                + (race.getHpBonus() >= 0 ? "+" : "") + race.getHpBonus() + ")");
        LOG.fine("[MOTM] TODO: Mirror race HP/stat bonuses into Hytale entity stats once the API is confirmed.");
    }

    private void clearManagedEffects(String playerId, StatusEffectManager statusEffectManager) {
        statusEffectManager.removeEffect(playerId, StatusEffect.Type.EVASION);
        statusEffectManager.removeEffect(playerId, StatusEffect.Type.DEFENSE_BUFF);
        statusEffectManager.removeEffect(playerId, StatusEffect.Type.ATTACK_BUFF);
    }

    private void addPercentBonus(Map<String, Double> target, String key, double value) {
        if (value != 0) {
            target.put(key, value);
        }
    }
}
