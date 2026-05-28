package com.motm.ui;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.HudComponent;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.MenteesMod;
import com.motm.model.PlayerData;
import com.motm.runtime.MotmRuntimeTasks;
import com.motm.runtime.state.StatusHudRuntimeState;
import com.motm.util.MotmObservability;

import java.util.List;
import java.util.logging.Logger;

/**
 * Owns MOTM custom HUD install/refresh behavior while task processors decide
 * when those actions should run.
 */
public final class MotmStatusHudActions {

    private final boolean enabled;
    private final int installDelayTicks;
    private final StatusHudRuntimeState statusHuds;
    private final MotmRuntimeTasks runtimeTasks;
    private final MenteesMod mod;
    private final Hooks hooks;
    private final Logger log;

    public MotmStatusHudActions(boolean enabled,
                                int installDelayTicks,
                                StatusHudRuntimeState statusHuds,
                                MotmRuntimeTasks runtimeTasks,
                                MenteesMod mod,
                                Hooks hooks,
                                Logger log) {
        this.enabled = enabled;
        this.installDelayTicks = installDelayTicks;
        this.statusHuds = statusHuds;
        this.runtimeTasks = runtimeTasks;
        this.mod = mod;
        this.hooks = hooks;
        this.log = log;
    }

    public void install(Player player) {
        if (!enabled || player == null) {
            return;
        }

        PlayerRef playerRef = hooks.universePlayerRef(player);
        if (playerRef == null || playerRef.getUuid() == null) {
            return;
        }

        String playerId = playerRef.getUuid().toString();
        if (statusHuds.contains(playerId)) {
            return;
        }

        MotmStatusHud hud = new MotmStatusHud(playerRef, mod);
        statusHuds.put(playerId, hud);
        String traceId = hooks.currentOrNewClientIntentTraceId();
        String previousTraceId = hooks.enterTrace(traceId);
        try {
            player.getHudManager().removeCustomHud(playerRef, hud.getKey());
            player.getHudManager().addCustomHud(playerRef, hud);
            hooks.recordClientIntent("custom_hud_set", traceId, MotmObservability.mapOf(
                    "playerId", playerId,
                    "username", playerRef.getUsername(),
                    "hud", "MOTM_StatusHud"
            ));
            try {
                player.getHudManager().hideHudComponents(
                        playerRef,
                        HudComponent.StatusIcons,
                        HudComponent.InputBindings,
                        HudComponent.AmmoIndicator,
                        HudComponent.UtilitySlotSelector);
                hooks.recordClientIntent("native_hud_components_hidden", traceId, MotmObservability.mapOf(
                        "playerId", playerId,
                        "components", List.of(
                                String.valueOf(HudComponent.StatusIcons),
                                String.valueOf(HudComponent.InputBindings),
                                String.valueOf(HudComponent.AmmoIndicator),
                                String.valueOf(HudComponent.UtilitySlotSelector)
                        )
                ));
            } catch (Exception e) {
                log.warning("[MOTM] Failed to hide native HUD components: " + e.getMessage());
            }
        } finally {
            hooks.restoreTrace(previousTraceId);
        }
    }

    public void queueInstall(String playerId) {
        if (!enabled || playerId == null || playerId.isBlank()) {
            return;
        }
        log.info("[MOTM] Queue HUD install: playerId=" + playerId
                + " delayTicks=" + installDelayTicks);
        runtimeTasks.requestStatusHudInstall(playerId, installDelayTicks);
    }

    public void refreshAll(Store<EntityStore> currentStore) {
        statusHuds.removeIfPlayer(playerId -> hooks.playerData(playerId) == null);
        statusHuds.forEach((playerId, hud) -> {
            Player runtimePlayer = hooks.runtimePlayer(playerId);
            if (runtimePlayer != null && hooks.isPlayerInStore(runtimePlayer, currentStore)) {
                hud.refresh();
            }
        });
    }

    public void queueRefresh(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }
        runtimeTasks.requestStatusHudRefresh(playerId);
    }

    public void clearPlayer(String playerId) {
        statusHuds.remove(playerId);
    }

    public void refreshNow(String playerId) {
        MotmStatusHud hud = statusHuds.get(playerId);
        if (hud != null) {
            String traceId = hooks.currentOrNewClientIntentTraceId();
            String previousTraceId = hooks.enterTrace(traceId);
            try {
                hooks.recordClientIntent("custom_hud_refresh", traceId, MotmObservability.mapOf(
                        "playerId", playerId,
                        "hud", "MOTM_StatusHud"
                ));
                hud.refresh();
            } finally {
                hooks.restoreTrace(previousTraceId);
            }
        }
    }

    public interface Hooks {
        PlayerRef universePlayerRef(Player player);

        PlayerData playerData(String playerId);

        Player runtimePlayer(String playerId);

        boolean isPlayerInStore(Player player, Store<EntityStore> currentStore);

        String currentOrNewClientIntentTraceId();

        String enterTrace(String traceId);

        void restoreTrace(String previousTraceId);

        void recordClientIntent(String type, String traceId, java.util.Map<String, Object> data);
    }
}
