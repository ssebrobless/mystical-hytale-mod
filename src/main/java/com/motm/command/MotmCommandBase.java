package com.motm.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.motm.MenteesMod;

/**
 * Thin Hytale command bridge that forwards to the plain Java command logic.
 */
public class MotmCommandBase extends CommandBase {

    private final MenteesMod mod;

    public MotmCommandBase(MenteesMod mod) {
        super("motm", "Mentees of the Mystical main command");
        this.mod = mod;
        setAllowsExtraArguments(true);
    }

    @Override
    protected boolean canGeneratePermission() {
        return false;
    }

    @Override
    public void executeSync(CommandContext context) {
        if (!context.isPlayer()) {
            context.sendMessage(Message.raw("[MOTM] This command can only be used by an in-game player."));
            return;
        }

        Player sender = context.senderAs(Player.class);
        String input = context.getInputString();
        String[] args = parseArgs(input);

        String response = mod.getMotmCommand().execute(sender.getPlayerRef().getUuid().toString(), args);
        context.sendMessage(Message.raw(response));
    }

    private String[] parseArgs(String input) {
        if (input == null || input.isBlank()) {
            return new String[0];
        }

        String[] tokens = input.trim().split("\\s+");
        if (tokens.length > 0 && (tokens[0].equalsIgnoreCase("motm") || tokens[0].equalsIgnoreCase("/motm"))) {
            String[] args = new String[tokens.length - 1];
            System.arraycopy(tokens, 1, args, 0, args.length);
            return args;
        }

        return tokens;
    }
}
