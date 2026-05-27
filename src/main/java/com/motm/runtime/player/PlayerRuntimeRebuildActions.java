package com.motm.runtime.player;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.motm.model.PlayerData;

import java.util.logging.Logger;

/**
 * Owns the runtime rebuild sequence for player class/style/race state.
 */
public final class PlayerRuntimeRebuildActions {

    private final Hooks hooks;
    private final Logger log;

    public PlayerRuntimeRebuildActions(Hooks hooks, Logger log) {
        this.hooks = hooks;
        this.log = log;
    }

    public void rebuildNow(PlayerData player) {
        if (player == null || player.getPlayerId() == null) {
            return;
        }

        String playerId = player.getPlayerId();
        log.info("[MOTM] >>> rebuildPlayerRuntimeNow START playerId=" + playerId
                + " class=" + player.getPlayerClass()
                + " styles=" + player.getSelectedStyles());

        hooks.resetCooldowns(playerId);
        hooks.clearClassPassiveState(playerId);
        hooks.clearStatusEffects(playerId);
        hooks.clearElementalMarks(playerId);
        hooks.clearArmedStomp(playerId);
        hooks.clearResourceState(playerId);
        hooks.synchronizePersistentResourceState(player);

        player.clearSynergyBonuses();
        player.clearRaceBonuses();

        if (player.getPlayerClass() == null) {
            hooks.refreshProgressionBonusesNow(playerId);
            if (!hooks.freeCastEnabled(playerId)) {
                hooks.clearFreeCastInvulnerability(playerId);
            }
            hooks.refreshStatusHudNow(playerId);
            log.info("[MOTM] <<< rebuildPlayerRuntimeNow END playerId=" + playerId + " class=<none>");
            return;
        }

        hooks.initializeResources(player);
        hooks.reapplyPerks(player);
        hooks.queueHydroContainerSync(playerId);

        if (player.getRace() != null) {
            hooks.applyRaceBonuses(player);
        }
        hooks.refreshProgressionBonusesNow(playerId);
        hooks.onClassPassivePlayerJoin(player);
        if (hooks.freeCastEnabled(playerId)) {
            Player runtimePlayer = hooks.runtimePlayer(playerId);
            if (runtimePlayer != null) {
                hooks.ensureFreeCastInvulnerability(runtimePlayer);
            }
        }
        hooks.refreshStatusHudNow(playerId);
        log.info("[MOTM] <<< rebuildPlayerRuntimeNow END playerId=" + playerId
                + " class=" + player.getPlayerClass()
                + " styles=" + player.getSelectedStyles());
    }

    public interface Hooks {
        void resetCooldowns(String playerId);

        void clearClassPassiveState(String playerId);

        void clearStatusEffects(String playerId);

        void clearElementalMarks(String playerId);

        void clearArmedStomp(String playerId);

        void clearResourceState(String playerId);

        void synchronizePersistentResourceState(PlayerData player);

        void refreshProgressionBonusesNow(String playerId);

        boolean freeCastEnabled(String playerId);

        void clearFreeCastInvulnerability(String playerId);

        void refreshStatusHudNow(String playerId);

        void initializeResources(PlayerData player);

        void reapplyPerks(PlayerData player);

        void queueHydroContainerSync(String playerId);

        void applyRaceBonuses(PlayerData player);

        void onClassPassivePlayerJoin(PlayerData player);

        Player runtimePlayer(String playerId);

        void ensureFreeCastInvulnerability(Player runtimePlayer);
    }
}
