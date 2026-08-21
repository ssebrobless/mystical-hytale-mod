package com.motm.runtime.player;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.motm.model.PlayerData;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Owns MOTM player session lifecycle policy while the plugin shell remains the
 * public Hytale callback facade.
 */
public final class PlayerSessionLifecycleActions {

    private final Hooks hooks;
    private final Logger log;

    public PlayerSessionLifecycleActions(Hooks hooks, Logger log) {
        this.hooks = hooks;
        this.log = log;
    }

    public void onPlayerJoin(String playerId, String playerName) {
        log.info("[MOTM] >>> onPlayerJoin: " + playerName + " id=" + playerId);
        PlayerData player = hooks.playerDataOnJoin(playerId, playerName);
        if (player == null) {
            return;
        }

        hooks.updateRestedOnLogin(player);

        if (player.getPlayerClass() != null) {
            hooks.reapplyPerks(player);
            hooks.synchronizePersistentResourceState(player);
            hooks.initializeResources(player);
            hooks.queueHydroContainerSync(playerId);
            hooks.onClassPassivePlayerJoin(player);
        } else {
            hooks.clearClassPassiveState(playerId);
        }

        if (hooks.hasPendingPerkSelection(player)) {
            int tier = hooks.pendingPerkSelectionTier(player);
            log.info("[MOTM] " + playerName + " has pending Tier " + tier + " perk selection");
        }

        if (player.isFirstJoin()) {
            log.info("[MOTM] " + playerName + " is a new player - first-join wizard runs on ready");
        }
    }

    public void onPlayerConnect(Player runtimePlayer) {
        onPlayerConnect(hooks.runtimePlayerIdentity(runtimePlayer), runtimePlayer);
    }

    void onPlayerConnect(RuntimePlayerIdentity identity, Player runtimePlayer) {
        log.info("[MOTM] >>> onPlayerConnect: " + runtimePlayer);
        if (identity == null || !identity.valid()) {
            return;
        }

        long startedAtMs = hooks.nowMs();
        String playerId = identity.playerId();
        hooks.putRuntimePlayer(playerId, runtimePlayer);
        hooks.recordCausality("player_connect", mapOf(
                "playerId", playerId,
                "username", identity.username(),
                "runtime", identity.runtimeDescription()
        ));

        PlayerData playerData = ensurePlayerDataInitialized(playerId, identity.username());
        rebuildSavedLoadoutIfPresent("onPlayerConnect", playerId, playerData, runtimePlayer);

        log.info("[MOTM] onPlayerConnect done dt=" + (hooks.nowMs() - startedAtMs)
                + "ms playerId=" + playerId);
    }

    public void onPlayerReady(Player runtimePlayer) {
        onPlayerReady(hooks.runtimePlayerIdentity(runtimePlayer), runtimePlayer);
    }

    void onPlayerReady(RuntimePlayerIdentity identity, Player runtimePlayer) {
        log.info("[MOTM] >>> onPlayerReady: " + runtimePlayer);
        if (identity == null || !identity.valid()) {
            return;
        }

        String playerId = identity.playerId();
        hooks.putRuntimePlayer(playerId, runtimePlayer);
        hooks.recordCausality("player_ready", mapOf(
                "playerId", playerId,
                "username", identity.username(),
                "world", identity.worldName()
        ));

        PlayerData playerData = ensurePlayerDataInitialized(playerId, identity.username());
        rebuildSavedLoadoutIfPresent("onPlayerReady", playerId, playerData, runtimePlayer);

        if (hooks.devToolsEnabled()) {
            hooks.clearStatusEffects(playerId);
            hooks.clearElementalMarks(playerId);
            hooks.setFreeCastEnabled(playerId, true);
            if (!hooks.ensureSpellbookItem(runtimePlayer)) {
                hooks.queueSpellbookGrant(playerId);
            }
        }

        // First-join wizard: a new or class-less player is protected (setup invincibility via
        // isStartupSelectionProtected) until they pick a class + an active style. Grant the spellbook so
        // they have the tool, and tell them what to do. Selection completion clears firstJoin
        // (completeStartupSelection), so this stops once they have chosen their path.
        if (playerData != null
                && (playerData.getPlayerClass() == null
                || playerData.getSelectedStyles() == null
                || playerData.getSelectedStyles().isEmpty())) {
            if (!hooks.ensureSpellbookItem(runtimePlayer)) {
                hooks.queueSpellbookGrant(playerId);
            }
            hooks.sendMessage(runtimePlayer, "[MOTM] Welcome to Mentees of the Mystical!");
            hooks.sendMessage(runtimePlayer,
                    "[MOTM] You are protected until you choose your path.");
            hooks.sendMessage(runtimePlayer,
                    "[MOTM] Open your spellbook (in your hotbar) to choose your elemental class and your active style (grants 3 abilities).");
            hooks.recordCausality("first_join_wizard", mapOf(
                    "playerId", playerId,
                    "username", identity.username()
            ));
        }

        hooks.queueStatusHudInstall(playerId);
        log.info("[MOTM] onPlayerReady done playerId=" + playerId);
    }

    private PlayerData ensurePlayerDataInitialized(String playerId, String username) {
        if (hooks.markRuntimePlayerInitialized(playerId) || hooks.playerData(playerId) == null) {
            onPlayerJoin(playerId, username);
        }
        return hooks.playerData(playerId);
    }

    private void rebuildSavedLoadoutIfPresent(String source,
                                             String playerId,
                                             PlayerData playerData,
                                             Player runtimePlayer) {
        boolean hasSavedLoadout = playerData != null
                && playerData.getPlayerClass() != null
                && playerData.getSelectedStyles() != null
                && !playerData.getSelectedStyles().isEmpty();
        log.info("[MOTM] " + source + " hasSavedLoadout=" + hasSavedLoadout + " playerId=" + playerId);

        if (!hasSavedLoadout) {
            return;
        }

        hooks.rebuildPlayerRuntimeNow(playerData);
        boolean ensured = hooks.ensureSpellbookItem(runtimePlayer);
        log.info("[MOTM] " + source + " ensureSpellbookItem=" + ensured
                + " hasSpellbook=" + hooks.playerHasSpellbook(runtimePlayer));
        if (!ensured && !hooks.playerHasSpellbook(runtimePlayer)) {
            hooks.queueSpellbookGrant(playerId);
        }
        hooks.refreshPlayerProgressionBonuses(playerId);
    }

    public void onPlayerDisconnect(String playerId) {
        hooks.recordCausality("player_disconnect", mapOf(
                "playerId", playerId,
                "hadRuntimePlayer", hooks.hasRuntimePlayer(playerId)
        ));

        PlayerData player = hooks.playerData(playerId);
        if (player != null) {
            hooks.updateRestedOnLogout(player);
        }

        hooks.playerDataOnDisconnect(playerId);
        hooks.styleOnPlayerDisconnect(playerId);
        hooks.resourceOnPlayerDisconnect(playerId);
        hooks.clearClassPassiveState(playerId);
        hooks.clearStatModifiersOrPerkTriggers(playerId);
        hooks.clearStatusEffects(playerId);
        hooks.clearElementalMarks(playerId);
        hooks.clearStatusHud(playerId);
        hooks.removeRuntimePlayer(playerId);
        hooks.clearRuntimeTasks(playerId);
        hooks.clearStyleTestRuntime(playerId);
        hooks.clearArmedStomp(playerId);
        hooks.clearPlayerProgression(playerId);
        hooks.clearFreeCastState(playerId);
        hooks.setFreeCastEnabled(playerId, false);
        hooks.clearSpellbookInput(playerId);
    }

    private static Map<String, Object> mapOf(Object... entries) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < entries.length; i += 2) {
            map.put(String.valueOf(entries[i]), entries[i + 1]);
        }
        return map;
    }

    public record RuntimePlayerIdentity(
            String playerId,
            String username,
            String worldName,
            String runtimeDescription
    ) {
        boolean valid() {
            return playerId != null && !playerId.isBlank();
        }
    }

    public interface Hooks {
        PlayerData playerDataOnJoin(String playerId, String playerName);

        PlayerData playerData(String playerId);

        void playerDataOnDisconnect(String playerId);

        RuntimePlayerIdentity runtimePlayerIdentity(Player runtimePlayer);

        void putRuntimePlayer(String playerId, Player runtimePlayer);

        boolean markRuntimePlayerInitialized(String playerId);

        boolean hasRuntimePlayer(String playerId);

        void removeRuntimePlayer(String playerId);

        void updateRestedOnLogin(PlayerData player);

        void updateRestedOnLogout(PlayerData player);

        void reapplyPerks(PlayerData player);

        void synchronizePersistentResourceState(PlayerData player);

        void initializeResources(PlayerData player);

        void queueHydroContainerSync(String playerId);

        void onClassPassivePlayerJoin(PlayerData player);

        void clearClassPassiveState(String playerId);

        boolean hasPendingPerkSelection(PlayerData player);

        int pendingPerkSelectionTier(PlayerData player);

        void rebuildPlayerRuntimeNow(PlayerData player);

        boolean ensureSpellbookItem(Player runtimePlayer);

        boolean playerHasSpellbook(Player runtimePlayer);

        void queueSpellbookGrant(String playerId);

        void sendMessage(Player runtimePlayer, String message);

        void refreshPlayerProgressionBonuses(String playerId);

        boolean devToolsEnabled();

        void clearStatusEffects(String playerId);

        void clearElementalMarks(String playerId);

        void setFreeCastEnabled(String playerId, boolean enabled);

        void queueStatusHudInstall(String playerId);

        void styleOnPlayerDisconnect(String playerId);

        void resourceOnPlayerDisconnect(String playerId);

        void clearStatModifiersOrPerkTriggers(String playerId);

        void clearStatusHud(String playerId);

        void clearRuntimeTasks(String playerId);

        void clearStyleTestRuntime(String playerId);

        void clearArmedStomp(String playerId);

        void clearPlayerProgression(String playerId);

        void clearFreeCastState(String playerId);

        void clearSpellbookInput(String playerId);

        void recordCausality(String type, Map<String, Object> data);

        long nowMs();
    }
}
