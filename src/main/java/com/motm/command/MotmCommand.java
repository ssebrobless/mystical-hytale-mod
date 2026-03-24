package com.motm.command;

import com.motm.MenteesMod;
import com.motm.manager.*;
import com.motm.model.*;

import java.util.List;

// TODO: These imports will need to match Hytale's actual API packages.
// Based on the CurseForge docs, commands extend CommandBase.
// import com.hypixel.hytale.server.command.CommandBase;
// import com.hypixel.hytale.server.command.CommandContext;
// import com.hypixel.hytale.server.text.Message;

/**
 * Main command handler for /motm commands.
 *
 * Subcommands:
 *   /motm class [classId]    - View classes or select one
 *   /motm perks              - Open perk selection UI / view current perks
 *   /motm select <id1> <id2> <id3> - Select 3 perks
 *   /motm stats              - View your stats and progress
 *   /motm level              - View XP progress
 *   /motm info               - View mod info
 *
 * NOTE: This is structured as a standalone class that can be adapted
 * to extend Hytale's CommandBase once the API is confirmed.
 * For now, it contains the logic that CommandBase.executeSync() would call.
 */
public class MotmCommand {

    private final MenteesMod mod;

    public MotmCommand(MenteesMod mod) {
        this.mod = mod;
    }

    /**
     * Entry point — called from Hytale's command system.
     * @param playerId The UUID of the command sender
     * @param args     The command arguments (everything after "/motm")
     * @return Response message to send back to the player
     */
    public String execute(String playerId, String[] args) {
        PlayerData player = mod.getPlayerDataManager().getOnlinePlayer(playerId);
        if (player == null) {
            return "[MOTM] Error: Player data not found.";
        }

        if (args.length == 0) {
            return getHelpMessage();
        }

        return switch (args[0].toLowerCase()) {
            case "class" -> handleClass(player, args);
            case "perks" -> handlePerks(player);
            case "select" -> handleSelect(player, args);
            case "style" -> handleStyle(player, args);
            case "race" -> handleRace(player, args);
            case "resources" -> handleResources(player);
            case "stats" -> handleStats(player);
            case "level" -> handleLevel(player);
            case "info" -> handleInfo();
            case "help" -> getHelpMessage();
            default -> "[MOTM] Unknown subcommand. Use /motm help";
        };
    }

    // --- /motm class [classId] ---

    private String handleClass(PlayerData player, String[] args) {
        if (player.getPlayerClass() != null) {
            ClassData classData = mod.getDataLoader().getClassData(player.getPlayerClass());
            return "[MOTM] You are a " + classData.getDisplayName() + "\n"
                    + "Theme: " + classData.getTheme() + " | Element: " + classData.getElement() + "\n"
                    + "Passive: " + classData.getPassiveAbility().getName() + " - "
                    + classData.getPassiveAbility().getDescription();
        }

        if (args.length < 2) {
            StringBuilder sb = new StringBuilder("[MOTM] Choose Your Elemental Path:\n");
            for (ClassData c : mod.getDataLoader().getAllClasses()) {
                sb.append("  ").append(c.getId()).append(" - ").append(c.getDisplayName())
                        .append(" (").append(c.getDifficulty()).append(")\n")
                        .append("    ").append(c.getDescription()).append("\n");
            }
            sb.append("\nUse: /motm class <terra|hydro|aero|corruptus>");
            return sb.toString();
        }

        String classId = args[1].toLowerCase();
        boolean success = mod.getPlayerDataManager().selectClass(player, classId);
        if (success) {
            ClassData classData = mod.getDataLoader().getClassData(classId);
            mod.getResourceManager().initializeForPlayer(player.getPlayerId(), classId);
            mod.getPerkManager().reapplyAllPerks(player, mod.getSynergyEngine());
            if (player.getRace() != null) {
                mod.getRaceManager().applyRaceBonuses(player, mod.getStatusEffectManager());
            }
            return "[MOTM] You have chosen " + classData.getDisplayName() + "!\n"
                    + "Your journey begins. Reach level 10 to unlock your first perks.";
        } else {
            return "[MOTM] Invalid class or you already have a class. Valid: terra, hydro, aero, corruptus";
        }
    }

    // --- /motm perks ---

    private String handlePerks(PlayerData player) {
        if (player.getPlayerClass() == null) {
            return "[MOTM] Select a class first with /motm class <classId>";
        }

        PerkManager pm = mod.getPerkManager();

        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM] === Your Perks ===\n");
        sb.append("Class: ").append(player.getPlayerClass()).append(" | Level: ")
                .append(player.getLevel()).append("\n");
        sb.append("Perks: ").append(player.getSelectedPerks().size()).append("/")
                .append(pm.getCurrentTier(player.getLevel()) * PerkManager.PERKS_TO_SELECT)
                .append(" (").append(pm.getRemainingPerkSlots(player)).append(" slots remaining)\n\n");

        if (pm.hasPendingPerkSelection(player)) {
            int pendingTier = pm.getPendingSelectionTier(player);
            sb.append(">>> PERK SELECTION AVAILABLE - Tier ").append(pendingTier).append(" <<<\n");
            List<Perk> available = pm.getAvailablePerks(player);
            for (int i = 0; i < available.size(); i++) {
                Perk p = available.get(i);
                sb.append("  [").append(i + 1).append("] ").append(p.getName())
                        .append(" - ").append(p.getDescription()).append("\n")
                        .append("      ID: ").append(p.getId()).append("\n")
                        .append("      Tags: ").append(String.join(", ", p.getSynergyTags())).append("\n");
            }
            sb.append("\nUse: /motm select <id1> <id2> <id3>");
        } else {
            // Show current perks by tier
            for (int tier = 1; tier <= pm.getCurrentTier(player.getLevel()); tier++) {
                List<Perk> tierPerks = pm.getPlayerPerksForTier(player, tier);
                if (!tierPerks.isEmpty()) {
                    sb.append("Tier ").append(tier).append(":\n");
                    for (Perk p : tierPerks) {
                        sb.append("  - ").append(p.getName()).append("\n");
                    }
                }
            }
        }

        // Show active synergies
        if (!player.getActiveSynergyBonuses().isEmpty()) {
            sb.append("\n=== Active Synergies ===\n");
            for (var syn : player.getActiveSynergyBonuses()) {
                sb.append("  ").append(syn.getBonusType()).append(" +")
                        .append(syn.getBonusValue()).append(" (from ").append(syn.getSourcePerk()).append(")\n");
            }
        }

        return sb.toString();
    }

    // --- /motm select <id1> <id2> <id3> ---

    private String handleSelect(PlayerData player, String[] args) {
        if (player.getPlayerClass() == null) {
            return "[MOTM] Select a class first with /motm class <classId>";
        }

        PerkManager pm = mod.getPerkManager();
        if (!pm.hasPendingPerkSelection(player)) {
            return "[MOTM] No perk selection available right now.";
        }

        if (args.length < 4) {
            return "[MOTM] Usage: /motm select <perkId1> <perkId2> <perkId3>";
        }

        List<String> selectedIds = List.of(args[1], args[2], args[3]);

        // Validate
        PerkManager.ValidationResult validation = pm.validatePerkSelection(player, selectedIds);
        if (!validation.isValid()) {
            return "[MOTM] Selection failed:\n" + String.join("\n", validation.getErrors());
        }

        // Show synergy preview
        SynergyEngine.SynergyPreview preview = mod.getSynergyEngine()
                .previewSynergyChanges(player, selectedIds);

        // Apply
        boolean success = pm.applyPerkSelection(player, selectedIds, mod.getSynergyEngine());
        if (success) {
            StringBuilder sb = new StringBuilder("[MOTM] Perks selected!\n");
            for (String id : selectedIds) {
                Perk perk = mod.getDataLoader().getPerkById(id, player.getPlayerClass());
                if (perk != null) {
                    sb.append("  + ").append(perk.getName()).append("\n");
                }
            }
            if (!preview.newSynergies.isEmpty()) {
                sb.append("\nNew synergies activated: ").append(preview.newSynergies.size());
            }
            mod.getPlayerDataManager().savePlayerData(player);
            mod.getPlayerDataManager().checkAchievements(player, "perks_selected", null);
            return sb.toString();
        }
        return "[MOTM] Perk selection failed.";
    }

    // --- /motm stats ---

    private String handleStats(PlayerData player) {
        StringBuilder sb = new StringBuilder("[MOTM] === Player Summary ===\n");
        sb.append("Name: ").append(player.getPlayerName()).append("\n");
        sb.append("Class: ").append(player.getPlayerClass() != null ? player.getPlayerClass() : "None").append("\n");
        sb.append("Level: ").append(player.getLevel()).append("\n");
        sb.append("Perks: ").append(player.getSelectedPerks().size()).append("/60\n");
        sb.append("Synergies: ").append(player.getActiveSynergyBonuses().size()).append(" active\n");
        sb.append("Achievements: ").append(player.getAchievements().size()).append("\n\n");

        sb.append("--- Combat Stats ---\n");
        var stats = player.getStatistics();
        int totalKills = stats.getMobsKilled().values().stream().mapToInt(Integer::intValue).sum();
        sb.append("Mobs Killed: ").append(totalKills).append("\n");
        sb.append("Bosses Defeated: ").append(stats.getBossesDefeated().size()).append("\n");
        sb.append("Deaths: ").append(stats.getDeaths()).append("\n");
        sb.append("Highest Combo: ").append(stats.getHighestCombo()).append("\n");
        sb.append("Damage Dealt: ").append(String.format("%.0f", stats.getTotalDamageDealt())).append("\n");
        sb.append("Damage Taken: ").append(String.format("%.0f", stats.getTotalDamageTaken())).append("\n");
        sb.append("Healing Done: ").append(String.format("%.0f", stats.getTotalHealingDone())).append("\n");

        int seconds = stats.getPlaytimeSeconds();
        int hours = seconds / 3600;
        int minutes = (seconds % 3600) / 60;
        sb.append("Playtime: ").append(hours).append("h ").append(minutes).append("m\n");

        return sb.toString();
    }

    // --- /motm level ---

    private String handleLevel(PlayerData player) {
        LevelingManager lm = mod.getLevelingManager();
        int required = lm.calculateXpRequired(player.getLevel());
        double percent = lm.getXpProgressPercent(player);
        String difficulty = mod.getMobScalingManager().getDifficultyDescription(player.getLevel());

        StringBuilder sb = new StringBuilder("[MOTM] === Level Progress ===\n");
        sb.append("Level: ").append(player.getLevel()).append(" / ").append(LevelingManager.MAX_LEVEL).append("\n");
        sb.append("XP: ").append(player.getCurrentXp()).append(" / ").append(required)
                .append(" (").append(String.format("%.1f", percent)).append("%)\n");
        sb.append("Total XP Earned: ").append(player.getTotalXpEarned()).append("\n");
        sb.append("World Difficulty: ").append(difficulty).append("\n");

        if (player.getRestedBonus() > 0) {
            sb.append("Rested Bonus: +").append((int) (player.getRestedBonus() * 100)).append("%\n");
        }

        int nextMilestone = ((player.getLevel() / 10) + 1) * 10;
        if (nextMilestone <= 200) {
            int xpToMilestone = lm.getXpToLevel(player, nextMilestone);
            sb.append("XP to next perk tier (Lv. ").append(nextMilestone).append("): ")
                    .append(xpToMilestone).append("\n");
        }

        return sb.toString();
    }

    // --- /motm style [styleId1] [styleId2] ---

    private String handleStyle(PlayerData player, String[] args) {
        if (player.getPlayerClass() == null) {
            return "[MOTM] Select a class first with /motm class <classId>";
        }

        var sm = mod.getStyleManager();

        // No args — show available styles for the player's class
        if (args.length < 2) {
            List<StyleData> allStyles = mod.getDataLoader().getStylesForClass(player.getPlayerClass());
            StringBuilder sb = new StringBuilder("[MOTM] === " + player.getPlayerClass().toUpperCase() + " Styles ===\n");
            sb.append("Choose 2 styles (6 abilities total)\n\n");

            List<String> selected = player.getSelectedStyles();
            for (StyleData style : allStyles) {
                boolean isSelected = selected.contains(style.getId());
                sb.append(isSelected ? ">> " : "   ");
                sb.append(sm.getStyleSummary(style)).append("\n");
            }

            if (selected.isEmpty()) {
                sb.append("Use: /motm style <styleId1> <styleId2>");
            } else {
                sb.append("Current styles: ").append(String.join(", ", selected));
                sb.append("\nTo change: /motm style <styleId1> <styleId2>");
            }
            return sb.toString();
        }

        // Select styles
        if (args.length < 3) {
            return "[MOTM] Select exactly 2 styles: /motm style <styleId1> <styleId2>";
        }

        List<String> styleIds = List.of(args[1].toLowerCase(), args[2].toLowerCase());
        if (styleIds.get(0).equals(styleIds.get(1))) {
            return "[MOTM] You must select two different styles.";
        }

        boolean success = sm.selectStyles(player, styleIds);
        if (success) {
            StringBuilder sb = new StringBuilder("[MOTM] Styles selected!\n");
            for (StyleData style : sm.getSelectedStyles(player)) {
                sb.append("  ").append(style.getName()).append(" - ").append(style.getTheme()).append("\n");
                for (AbilityData ability : style.getAbilities()) {
                    sb.append("    - ").append(ability.getName()).append(": ")
                            .append(ability.getDescription()).append("\n");
                }
            }
            // Initialize resources for the player's class
            mod.getResourceManager().initializeForPlayer(player.getPlayerId(), player.getPlayerClass());
            mod.getPlayerDataManager().savePlayerData(player);
            return sb.toString();
        }
        return "[MOTM] Invalid style selection. Use /motm style to see available styles.";
    }

    // --- /motm race [raceId] ---

    private String handleRace(PlayerData player, String[] args) {
        if (player.getRace() != null && args.length < 2) {
            RaceData race = mod.getDataLoader().getRaceById(player.getRace());
            if (race != null) {
                return "[MOTM] Your race: " + race.getName() + "\n"
                        + "  " + race.getDescription() + "\n"
                        + "  HP Bonus: " + (race.getHpBonus() >= 0 ? "+" : "") + race.getHpBonus() + "\n"
                        + "  Passive: " + race.getPassive();
            }
        }

        if (args.length < 2) {
            StringBuilder sb = new StringBuilder("[MOTM] === Choose Your Race ===\n\n");
            for (RaceData race : mod.getDataLoader().getAllRaces()) {
                sb.append("  ").append(race.getId()).append(" - ").append(race.getName())
                        .append(" (HP: ").append(race.getHpBonus() >= 0 ? "+" : "")
                        .append(race.getHpBonus()).append(")\n");
                sb.append("    ").append(race.getPassive()).append("\n\n");
            }
            sb.append("Use: /motm race <raceId>");
            return sb.toString();
        }

        // Race is permanent — can only set once
        if (player.getRace() != null) {
            return "[MOTM] You are already a " + player.getRace() + ". Race cannot be changed.";
        }

        String raceId = args[1].toLowerCase();
        if (!mod.getDataLoader().isValidRace(raceId)) {
            return "[MOTM] Invalid race. Use /motm race to see options.";
        }

        player.setRace(raceId);
        RaceData race = mod.getDataLoader().getRaceById(raceId);
        if (player.getPlayerClass() != null) {
            mod.getRaceManager().applyRaceBonuses(player, mod.getStatusEffectManager());
        }
        mod.getPlayerDataManager().savePlayerData(player);
        return "[MOTM] You are now a " + race.getName() + "!\n"
                + "  " + race.getPassive() + "\n"
                + "  Special: " + race.getSpecial();
    }

    // --- /motm resources ---

    private String handleResources(PlayerData player) {
        if (player.getPlayerClass() == null) {
            return "[MOTM] Select a class first with /motm class <classId>";
        }
        return "[MOTM] " + mod.getResourceManager()
                .getResourceDisplay(player.getPlayerId(), player.getPlayerClass());
    }

    // --- /motm info ---

    private String handleInfo() {
        return "[MOTM] === Mentees of the Mystical ===\n"
                + "Version: 1.0.0\n"
                + "4 Classes | 40 Styles | 12 Races | 800 Perks | Level 1-200\n"
                + "Elemental Reactions | Dynamic Mob Scaling | Synergy System\n\n"
                + "Commands:\n"
                + "  /motm class [id]        - View/select class\n"
                + "  /motm race [id]         - View/select race\n"
                + "  /motm style [id1] [id2] - View/select combat styles\n"
                + "  /motm perks             - View perks & available selections\n"
                + "  /motm select ...        - Select 3 perks\n"
                + "  /motm resources         - View class resources\n"
                + "  /motm stats             - View your statistics\n"
                + "  /motm level             - View XP progress\n"
                + "  /motm help              - Show this help";
    }

    private String getHelpMessage() {
        return handleInfo();
    }
}
