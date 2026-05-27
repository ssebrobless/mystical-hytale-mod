package com.motm.runtime.task;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.protocol.GameMode;
import com.motm.runtime.MotmRuntimeTasks;
import com.motm.runtime.state.RuntimePlayerRegistry;

import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * Owns dev command validation and runtime task queueing for player/world review
 * actions.
 */
public final class DevRuntimeCommandActions {

    private final BooleanSupplier devToolsEnabled;
    private final Supplier<String> devToolsDisabledMessage;
    private final RuntimePlayerRegistry runtimePlayers;
    private final MotmRuntimeTasks runtimeTasks;
    private final Logger log;

    public DevRuntimeCommandActions(BooleanSupplier devToolsEnabled,
                                    Supplier<String> devToolsDisabledMessage,
                                    RuntimePlayerRegistry runtimePlayers,
                                    MotmRuntimeTasks runtimeTasks,
                                    Logger log) {
        this.devToolsEnabled = devToolsEnabled;
        this.devToolsDisabledMessage = devToolsDisabledMessage;
        this.runtimePlayers = runtimePlayers;
        this.runtimeTasks = runtimeTasks;
        this.log = log;
    }

    public String queueRuntimePlayerRelocationForTesting(String playerId, String target) {
        String normalizedTarget = target == null ? "up" : target.toLowerCase(Locale.ROOT);
        if (!List.of("up", "flatlands", "lane").contains(normalizedTarget)) {
            return "[MOTM] Dev relocate usage: /motm dev relocate <up|flatlands|lane>";
        }
        boolean added = runtimeTasks.requestDevRelocation(playerId, normalizedTarget);
        log.info("[MOTM] Dev relocate queued: playerId=" + playerId
                + " target=" + normalizedTarget
                + " added=" + added);
        return added
                ? "[MOTM] Dev relocate queued: " + normalizedTarget + "."
                : "[MOTM] A dev relocate request is already queued.";
    }

    public String queueDaylightForTesting(String playerId) {
        if (playerId == null || playerId.isBlank() || runtimePlayers.get(playerId) == null) {
            return "[MOTM] Join a world and run this in-game to force daylight.";
        }
        boolean added = runtimeTasks.requestDaylight(playerId);
        log.info("[MOTM] Dev daylight queued: playerId=" + playerId + " added=" + added);
        return added
                ? "[MOTM] Dev daylight queued."
                : "[MOTM] Dev daylight is already queued.";
    }

    public String queueGameModeForTesting(String playerId, String mode) {
        if (!devToolsEnabled.getAsBoolean()) {
            return devToolsDisabledMessage.get();
        }
        if (playerId == null || playerId.isBlank() || runtimePlayers.get(playerId) == null) {
            return "[MOTM] Join a world and run this in-game to change review mode.";
        }

        GameMode gameMode = parseReviewGameMode(mode);
        if (gameMode == null) {
            return "[MOTM] Dev mode usage: /motm dev mode <creative|adventure>. "
                    + "This Hytale build exposes Adventure and Creative; Survival is not present in the protocol enum.";
        }

        runtimeTasks.requestGameModeChange(playerId, gameMode);
        log.info("[MOTM] Dev game mode queued: playerId=" + playerId + " mode=" + gameMode);
        return "[MOTM] Dev game mode queued: " + gameMode + ".";
    }

    public String queueTerraReviewKitGrant(String playerId) {
        if (!devToolsEnabled.getAsBoolean()) {
            return devToolsDisabledMessage.get();
        }
        if (playerId == null || playerId.isBlank() || runtimePlayers.get(playerId) == null) {
            return "[MOTM] Join a world and run this in-game to receive the Terra review kit.";
        }

        boolean added = runtimeTasks.requestTerraReviewKitGrant(playerId);
        log.info("[MOTM] Terra review kit queued: playerId=" + playerId + " added=" + added);
        return added
                ? "[MOTM] Terra review kit queued."
                : "[MOTM] Terra review kit is already queued.";
    }

    public String queueTerraReviewInventoryClean(String playerId) {
        if (!devToolsEnabled.getAsBoolean()) {
            return devToolsDisabledMessage.get();
        }
        if (playerId == null || playerId.isBlank() || runtimePlayers.get(playerId) == null) {
            return "[MOTM] Join a world and run this in-game to clean the Terra review inventory.";
        }

        boolean added = runtimeTasks.requestTerraReviewInventoryClean(playerId);
        log.info("[MOTM] Terra review inventory clean queued: playerId=" + playerId + " added=" + added);
        return added
                ? "[MOTM] Terra review inventory clean queued."
                : "[MOTM] Terra review inventory clean is already queued.";
    }

    private GameMode parseReviewGameMode(String mode) {
        String normalized = mode == null ? "" : mode.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "creative", "c" -> GameMode.Creative;
            case "adventure", "survival", "s", "a" -> GameMode.Adventure;
            default -> null;
        };
    }
}
