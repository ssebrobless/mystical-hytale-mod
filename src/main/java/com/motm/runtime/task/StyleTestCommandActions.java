package com.motm.runtime.task;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.manager.GameplayPlaybackManager;
import com.motm.manager.PlayerDataManager;
import com.motm.manager.StyleManager;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.model.StyleData;
import com.motm.proof.MotmProofCatalog;
import com.motm.runtime.MotmRuntimeTasks;
import com.motm.runtime.state.ActiveStyleTest;
import com.motm.runtime.state.RuntimePlayerRegistry;
import com.motm.runtime.state.StyleTestRuntimeState;
import com.motm.util.DataLoader;

import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Owns dev style-test command policy while runtime processors perform the
 * queued world mutations.
 */
public final class StyleTestCommandActions {

    private final BooleanSupplier devToolsEnabled;
    private final Supplier<String> devToolsDisabledMessage;
    private final RuntimePlayerRegistry runtimePlayers;
    private final PlayerDataManager playerDataManager;
    private final DataLoader dataLoader;
    private final StyleManager styleManager;
    private final MotmRuntimeTasks runtimeTasks;
    private final StyleTestRuntimeState styleTestRuntimeState;
    private final StyleTestTargetResolver targetResolver;
    private final GameplayPlaybackManager gameplayPlaybackManager;
    private final Consumer<PlayerData> rebuildRuntime;
    private final Consumer<String> refreshStatusHud;
    private final BiConsumer<String, Boolean> setFreeCastEnabled;
    private final Logger log;

    public StyleTestCommandActions(BooleanSupplier devToolsEnabled,
                                   Supplier<String> devToolsDisabledMessage,
                                   RuntimePlayerRegistry runtimePlayers,
                                   PlayerDataManager playerDataManager,
                                   DataLoader dataLoader,
                                   StyleManager styleManager,
                                   MotmRuntimeTasks runtimeTasks,
                                   StyleTestRuntimeState styleTestRuntimeState,
                                   StyleTestTargetResolver targetResolver,
                                   GameplayPlaybackManager gameplayPlaybackManager,
                                   Consumer<PlayerData> rebuildRuntime,
                                   Consumer<String> refreshStatusHud,
                                   BiConsumer<String, Boolean> setFreeCastEnabled,
                                   Logger log) {
        this.devToolsEnabled = devToolsEnabled;
        this.devToolsDisabledMessage = devToolsDisabledMessage;
        this.runtimePlayers = runtimePlayers;
        this.playerDataManager = playerDataManager;
        this.dataLoader = dataLoader;
        this.styleManager = styleManager;
        this.runtimeTasks = runtimeTasks;
        this.styleTestRuntimeState = styleTestRuntimeState;
        this.targetResolver = targetResolver;
        this.gameplayPlaybackManager = gameplayPlaybackManager;
        this.rebuildRuntime = rebuildRuntime;
        this.refreshStatusHud = refreshStatusHud;
        this.setFreeCastEnabled = setFreeCastEnabled;
        this.log = log;
    }

    public String startStyleTest(String playerId, String styleId) {
        if (!devToolsEnabled.getAsBoolean()) {
            return devToolsDisabledMessage.get();
        }
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        Player runtimePlayer = runtimePlayers.get(playerId);
        var playerData = playerDataManager.getOnlinePlayer(playerId);
        if (runtimePlayer == null || playerData == null) {
            return "[MOTM] Join a world and run this in-game to start a live style test.";
        }

        StyleLookup styleLookup = findStyleLookup(styleId);
        if (styleLookup == null) {
            return "[MOTM] Unknown style '" + styleId + "'.";
        }

        playerData.setPlayerClass(styleLookup.classId());
        playerData.setFirstJoin(false);
        boolean selected = styleManager.selectStyles(playerData, List.of(styleLookup.style().getId()));
        if (!selected) {
            return "[MOTM] Failed to prepare style test for " + styleLookup.style().getName() + ".";
        }

        setFreeCastEnabled.accept(playerId, true);
        playerDataManager.savePlayerData(playerData);
        rebuildRuntime.accept(playerData);
        refreshStatusHud.accept(playerId);

        List<String> abilityIds = styleLookup.style().getAbilities().stream()
                .map(AbilityData::getId)
                .toList();

        styleTestRuntimeState.start(new ActiveStyleTest(
                playerId,
                styleLookup.classId(),
                styleLookup.style().getId(),
                styleLookup.style().getName(),
                abilityIds,
                0,
                System.currentTimeMillis() + 1200L
        ));

        return "[MOTM] Live style test queued: "
                + humanize(styleLookup.classId()) + " > " + styleLookup.style().getName()
                + ". Free-cast ON. The mod will fire the style abilities in sequence against nearby targets.";
    }

    public String stopStyleTest(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        ActiveStyleTest removed = styleTestRuntimeState.stop(playerId);
        if (removed == null) {
            return "[MOTM] No active live style test is running.";
        }

        return "[MOTM] Stopped live style test for " + removed.styleName() + ".";
    }

    public String startSingleAbilityTest(String playerId, String abilityId) {
        if (!devToolsEnabled.getAsBoolean()) {
            return devToolsDisabledMessage.get();
        }
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        Player runtimePlayer = runtimePlayers.get(playerId);
        var playerData = playerDataManager.getOnlinePlayer(playerId);
        if (runtimePlayer == null || playerData == null) {
            return "[MOTM] Join a world and run this in-game to start a live ability test.";
        }

        StyleData style = null;
        if (playerData.getPlayerClass() != null) {
            for (String selectedStyleId : playerData.getSelectedStyles()) {
                style = dataLoader.getStyleById(selectedStyleId, playerData.getPlayerClass());
                if (style != null) {
                    break;
                }
            }
        }
        if (style == null) {
            return "[MOTM] Choose a style before running /motm dev test ability <abilityId>.";
        }

        AbilityData ability = styleManager.findAbility(playerData, abilityId);
        if (ability == null) {
            return "[MOTM] Unknown ability '" + abilityId + "' for current style.";
        }

        setFreeCastEnabled.accept(playerId, true);
        runtimeTasks.requestStyleAbilityTest(playerId, ability.getId());

        return "[MOTM] Live ability test queued: " + ability.getName()
                + ". Free-cast ON. The mod will target the nearest test NPC.";
    }

    public String getStyleTestStatus(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        ActiveStyleTest active = styleTestRuntimeState.get(playerId);
        if (active == null) {
            return "[MOTM] No active live style test is running.";
        }

        int total = active.abilityIds().size();
        int nextStep = Math.min(active.nextAbilityIndex() + 1, total);
        return "[MOTM] Live style test: "
                + humanize(active.classId()) + " > " + active.styleName()
                + " | step " + nextStep + "/" + total + ".";
    }

    public String spawnStyleTestMobs(String playerId) {
        return spawnStyleTestMobs(playerId, "standard");
    }

    public String clearStyleTestMobs(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }
        boolean added = runtimeTasks.requestStyleTestMobClear(playerId);
        log.info("[MOTM] Style test mob clear queued: playerId=" + playerId + " added=" + added);
        return added
                ? "[MOTM] Style test mob clear queued."
                : "[MOTM] Style test mob clear is already queued.";
    }

    public String resetStyleReviewArena(String playerId) {
        if (!devToolsEnabled.getAsBoolean()) {
            return devToolsDisabledMessage.get();
        }
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }
        Player runtimePlayer = runtimePlayers.get(playerId);
        if (runtimePlayer == null) {
            return "[MOTM] Join a world and run this in-game to reset the style review arena.";
        }
        boolean added = runtimeTasks.requestStyleReviewReset(playerId);
        log.info("[MOTM] Style review arena reset queued: playerId=" + playerId + " added=" + added);
        return added
                ? "[MOTM] Style review arena reset queued."
                : "[MOTM] Style review arena reset is already queued.";
    }

    public String countStyleTestMobs(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }
        boolean added = runtimeTasks.requestStyleTestMobCount(playerId);
        log.info("[MOTM] Style test mob count queued: playerId=" + playerId + " added=" + added);
        return added
                ? "[MOTM] Style test mob count queued."
                : "[MOTM] Style test mob count is already queued.";
    }

    public String spawnStyleTestMobs(String playerId, boolean closeGroundedTarget) {
        return spawnStyleTestMobs(playerId, closeGroundedTarget ? "close" : "standard");
    }

    public String spawnStyleTestMobs(String playerId, String mode) {
        if (!devToolsEnabled.getAsBoolean()) {
            return devToolsDisabledMessage.get();
        }
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        Player runtimePlayer = runtimePlayers.get(playerId);
        if (runtimePlayer == null) {
            return "[MOTM] Join a world and run this in-game to spawn style-test mobs.";
        }

        String normalizedMode = StyleTestMobActions.normalizeMode(mode);
        boolean added = runtimeTasks.requestStyleTestMobSpawn(playerId, normalizedMode);
        log.info("[MOTM] Style test mob spawn queued: playerId=" + playerId + " added=" + added);
        return added
                ? "[MOTM] Style test mob spawn queued mode=" + normalizedMode + "."
                : "[MOTM] Style test mob spawn is already queued.";
    }

    public String queueDevProof(String playerId, String proofId) {
        if (!devToolsEnabled.getAsBoolean()) {
            return devToolsDisabledMessage.get();
        }
        if (playerId == null || playerId.isBlank()) {
            return "[MOTM] Runtime player context is unavailable.";
        }
        Player runtimePlayer = runtimePlayers.get(playerId);
        if (runtimePlayer == null) {
            return "[MOTM] Join a world and run this in-game to run a proof.";
        }
        String normalizedProofId = MotmProofCatalog.normalize(proofId);
        if (normalizedProofId.isBlank()) {
            return MotmProofCatalog.usage();
        }
        if (!MotmProofCatalog.isKnown(normalizedProofId)) {
            return "[MOTM] Unknown proof id: " + normalizedProofId + "\n" + MotmProofCatalog.usage();
        }
        boolean added = runtimeTasks.requestProof(playerId, normalizedProofId);
        log.info("[MOTM] Proof request queued: playerId=" + playerId
                + " proofId=" + normalizedProofId
                + " added=" + added);
        return added
                ? "[MOTM] Proof queued: " + normalizedProofId + "."
                : "[MOTM] A proof request is already queued.";
    }

    public String runStyleTestWeaponHit(String playerId) {
        if (!devToolsEnabled.getAsBoolean()) {
            return devToolsDisabledMessage.get();
        }
        Player runtimePlayer = runtimePlayers.get(playerId);
        var playerData = playerDataManager.getOnlinePlayer(playerId);
        if (runtimePlayer == null || playerData == null) {
            return "[MOTM] Join a world and run this in-game to test a weapon follow-up.";
        }
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return "[MOTM] Style test weapon hit failed: player store missing.";
        }

        Ref<EntityStore> target = targetResolver.findNearestNpc(playerRef.getStore(), runtimePlayer, 8.0);
        if (target == null) {
            String summary = "[MOTM] Style test weapon hit failed: no style-test target within 8m.";
            log.warning(summary + " playerId=" + playerId);
            return summary;
        }

        String response = gameplayPlaybackManager.handleWeaponFollowUpHit(
                runtimePlayer,
                playerData,
                target,
                "Weapon_Sword_Iron"
        );
        if (response == null || response.isBlank()) {
            Damage simulatedNativeHit = new Damage(
                    new Damage.EntitySource(playerRef),
                    DamageCause.PHYSICAL,
                    10.0f
            );
            response = gameplayPlaybackManager.handleNativeWeaponDamage(
                    runtimePlayer,
                    playerData,
                    target,
                    "Weapon_Sword_Iron",
                    simulatedNativeHit
            );
        }
        if (response == null || response.isBlank()) {
            String summary = "[MOTM] Style test weapon hit: no follow-up/passive applied.";
            log.info(summary + " playerId=" + playerId);
            return summary;
        }

        log.info(response + " playerId=" + playerId);
        return response;
    }

    public String forceStyleTestStompLanding(String playerId) {
        if (!devToolsEnabled.getAsBoolean()) {
            return devToolsDisabledMessage.get();
        }
        Player runtimePlayer = runtimePlayers.get(playerId);
        if (runtimePlayer == null) {
            return "[MOTM] Join a world and run this in-game to force a Stomp landing.";
        }

        String response = gameplayPlaybackManager.forceArmedStompLanding(playerId, runtimePlayer);
        log.info(response + " playerId=" + playerId);
        return response;
    }

    private StyleLookup findStyleLookup(String styleId) {
        String normalizedStyleId = styleId == null ? "" : styleId.trim().toLowerCase(Locale.ROOT);
        if (normalizedStyleId.isBlank()) {
            return null;
        }

        for (String classId : List.of("terra", "hydro", "aero", "corruptus")) {
            StyleData style = dataLoader.getStyleById(normalizedStyleId, classId);
            if (style != null) {
                return new StyleLookup(classId, style);
            }
        }

        return null;
    }

    private String humanize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Unknown";
        }

        String[] parts = raw.replace('-', '_').split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                sb.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return sb.toString();
    }

    private record StyleLookup(String classId, StyleData style) {
    }
}
