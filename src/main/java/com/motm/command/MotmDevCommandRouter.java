package com.motm.command;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.motm.MenteesMod;
import com.motm.model.PlayerData;

final class MotmDevCommandRouter {

    private final MenteesMod mod;
    private final MotmCommand command;

    MotmDevCommandRouter(MenteesMod mod, MotmCommand command) {
        this.mod = mod;
        this.command = command;
    }

    String route(PlayerData player, String[] args, Player runtimePlayer) {
        String denied = MotmCommandAuth.deniedMessage(mod.isDevToolsEnabled(), mod.devToolsDisabledMessage());
        if (denied != null) {
            return denied;
        }
        if (args.length < 2) {
            return command.getDevHelpMessage();
        }

        return switch (args[1].toLowerCase()) {
            case "help" -> command.getDevHelpMessage();
            case "book" -> command.handleDevBook(player, runtimePlayer);
            case "observe", "observability" -> command.handleDevObserve(player, args, runtimePlayer);
            case "audit" -> command.handleDevAudit(player, args);
            case "test" -> command.handleDevTest(player, args, runtimePlayer);
            case "proof" -> command.handleDevProof(player, args, runtimePlayer);
            case "passive", "passives" -> command.handleDevPassive(player, args, runtimePlayer);
            case "position", "where" -> command.handleDevPosition(player);
            case "relocate", "unstuck" -> command.handleDevRelocate(player, args);
            case "mode", "gamemode" -> command.handleDevMode(player, args);
            case "kit" -> command.handleDevKit(player, args);
            case "inventory", "inv" -> command.handleDevInventory(player, args);
            case "daylight", "noon" -> mod.queueDaylightForTesting(player.getPlayerId());
            case "freecast" -> command.handleDevFreeCast(player, args);
            case "effects" -> command.handleDevEffects(player, runtimePlayer);
            case "clear" -> command.handleDevClear(player, args);
            case "level" -> command.handleDevLevel(player, args);
            case "xp" -> command.handleDevXp(player, args);
            case "class" -> command.handleDevClass(player, args);
            case "perks" -> command.handleDevPerks(player, args);
            case "styles" -> command.handleDevStyles(player, args);
            case "reset" -> command.handleDevReset(player, args);
            default -> "[MOTM] Unknown dev subcommand.\n" + command.getDevHelpMessage();
        };
    }
}
