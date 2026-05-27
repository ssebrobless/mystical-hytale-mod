package com.motm.runtime.player;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.model.Perk;
import com.motm.model.PerkTriggerBinding;
import com.motm.runtime.state.PerkTriggerRuntimeState;

import java.util.List;
import java.util.logging.Logger;

/**
 * Owns runtime perk-trigger registration and direct trigger effects.
 */
public final class PerkTriggerRuntimeActions {

    private final PerkTriggerRuntimeState state;
    private final Logger log;

    public PerkTriggerRuntimeActions(PerkTriggerRuntimeState state, Logger log) {
        this.state = state;
        this.log = log;
    }

    public void register(String playerId, Perk perk, Perk.Effect effect) {
        PerkTriggerBinding binding = binding(perk, effect);
        if (playerId == null || playerId.isBlank() || binding == null) {
            return;
        }

        state.add(playerId, binding);
        log.info("[MOTM] perk trigger registered: player=" + playerId
                + " perk=" + binding.perkId()
                + " type=" + binding.type()
                + " value=" + binding.value());
    }

    public void clear(String playerId) {
        state.clear(playerId);
    }

    public List<PerkTriggerBinding> get(String playerId, String type) {
        return state.get(playerId, type);
    }

    public int applyKillTriggers(String playerId, Player runtimePlayer) {
        int applied = 0;
        for (PerkTriggerBinding trigger : get(playerId, "on_kill")) {
            log.info("[MOTM] perk on_kill trigger: perk=" + trigger.perkId()
                    + " value=" + trigger.value());
            if (runtimePlayer != null && applyHealFraction(runtimePlayer, trigger.value())) {
                applied++;
            }
        }
        return applied;
    }

    static PerkTriggerBinding binding(Perk perk, Perk.Effect effect) {
        if (perk == null || effect == null || effect.getType() == null) {
            return null;
        }

        double triggerValue = effect.getValue();
        if (triggerValue == 0.0) {
            triggerValue = effect.getHeal();
        }
        if (triggerValue == 0.0) {
            triggerValue = effect.getHealAmount();
        }

        return new PerkTriggerBinding(perk.getId(), effect.getType(), triggerValue);
    }

    private boolean applyHealFraction(Player runtimePlayer, double fraction) {
        if (runtimePlayer == null || fraction <= 0.0) {
            return false;
        }

        try {
            Ref<EntityStore> playerRef = runtimePlayer.getReference();
            if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
                return false;
            }
            EntityStatMap statMap = playerRef.getStore().getComponent(playerRef, EntityStatMap.getComponentType());
            if (statMap == null) {
                return false;
            }

            float amount = (float) Math.max(1.0, 100.0 * fraction);
            statMap.addStatValue(DefaultEntityStatTypes.getHealth(), amount);
            log.info("[MOTM] perk heal applied: fraction=" + fraction + " amount=" + amount);
            return true;
        } catch (RuntimeException ex) {
            log.warning("[MOTM] Failed to apply perk heal: " + ex.getMessage());
            return false;
        }
    }
}
