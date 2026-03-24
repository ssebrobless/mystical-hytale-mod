package com.motm.command;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.motm.MenteesMod;
import com.motm.manager.SpellbookManager;
import com.motm.ui.SpellbookPage;

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

        if (tryOpenSpellbook(context, sender, args)) {
            return;
        }

        String response = mod.getMotmCommand().execute(sender.getPlayerRef().getUuid().toString(), args);
        context.sendMessage(Message.raw(response));
    }

    private boolean tryOpenSpellbook(CommandContext context, Player sender, String[] args) {
        if (args.length == 0) {
            return false;
        }

        String subcommand = args[0].toLowerCase();
        if (!subcommand.equals("spellbook") && !subcommand.equals("book")) {
            return false;
        }

        SpellbookManager.Section section = args.length >= 2
                ? mod.getSpellbookManager().parseSection(args[1])
                : SpellbookManager.Section.OVERVIEW;
        if (section == null) {
            context.sendMessage(Message.raw("[MOTM] Unknown spellbook section.\nSections: "
                    + mod.getSpellbookManager().getSectionList()));
            return true;
        }

        var entityRef = sender.getReference();
        if (entityRef == null || !entityRef.isValid()) {
            String fallback = mod.getMotmCommand().execute(sender.getPlayerRef().getUuid().toString(), args);
            context.sendMessage(Message.raw(fallback));
            return true;
        }

        sender.getPageManager().openCustomPage(
                entityRef,
                entityRef.getStore(),
                new SpellbookPage(sender.getPlayerRef(), mod, section)
        );
        return true;
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
