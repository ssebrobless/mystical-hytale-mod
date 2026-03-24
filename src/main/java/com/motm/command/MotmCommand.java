package com.motm.command;

import com.motm.MenteesMod;
import com.motm.manager.LevelingManager;
import com.motm.manager.PerkManager;
import com.motm.manager.StyleManager;
import com.motm.manager.SynergyEngine;
import com.motm.model.AbilityData;
import com.motm.model.ClassData;
import com.motm.model.Perk;
import com.motm.model.PlayerData;
import com.motm.model.RaceData;
import com.motm.model.StyleData;

import java.util.List;

/**
 * Main command handler for /motm commands.
 */
public class MotmCommand {

    private final MenteesMod mod;

    public MotmCommand(MenteesMod mod) {
        this.mod = mod;
    }

    /**
     * Entry point called from Hytale's command bridge.
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
            case "dev" -> handleDev(player, args);
            case "info" -> handleInfo();
            case "help" -> getHelpMessage();
            default -> "[MOTM] Unknown subcommand. Use /motm help";
        };
    }

    // --- /motm class [classId] ---

    private String handleClass(PlayerData player, String[] args) {
        if (player.getPlayerClass() != null) {
            ClassData classData = mod.getDataLoader().getClassData(player.getPlayerClass());
            String styleSummary = player.getSelectedStyles().isEmpty()
                    ? "None selected yet. Use /motm style <styleId>."
                    : formatSelectedStyleSummary(player);
            return "[MOTM] You are a " + classData.getDisplayName() + "\n"
                    + "Theme: " + classData.getTheme() + " | Element: " + classData.getElement() + "\n"
                    + "Style: " + styleSummary + "\n"
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
        if (!success) {
            return "[MOTM] Invalid class or you already have a class. Valid: terra, hydro, aero, corruptus";
        }

        ClassData classData = mod.getDataLoader().getClassData(classId);
        rebuildPlayerRuntime(player);
        return "[MOTM] You have chosen " + classData.getDisplayName() + "!\n"
                + "Next: choose your combat style with /motm style <styleId>\n"
                + "Perks unlock starting at level 10.";
    }

    // --- /motm perks ---

    private String handlePerks(PlayerData player) {
        if (player.getPlayerClass() == null) {
            return "[MOTM] Select a class first with /motm class <classId>";
        }

        if (player.getSelectedStyles().isEmpty()) {
            return "[MOTM] Choose your style first with /motm style <styleId>\n"
                    + "Flow: class -> style -> abilities -> perks";
        }

        PerkManager pm = mod.getPerkManager();

        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM] === Perks ===\n");
        sb.append("Class: ").append(player.getPlayerClass())
                .append(" | Style: ").append(getSelectedStyleName(player))
                .append(" | Level: ").append(player.getLevel()).append("\n");
        sb.append("Perks are separate from styles.\n");

        if (pm.hasPendingPerkSelection(player)) {
            int pendingTier = pm.getPendingSelectionTier(player);
            sb.append("Tier ").append(pendingTier).append(" choices: pick 3 of 10\n\n");
            List<Perk> available = pm.getAvailablePerks(player);
            for (int i = 0; i < available.size(); i++) {
                Perk perk = available.get(i);
                sb.append("[").append(i + 1).append("] ").append(perk.getName()).append("\n");
                sb.append("  ").append(compactText(perk.getDescription(), 60)).append("\n");
                sb.append("  ID: ").append(perk.getId()).append("\n");
            }
            sb.append("\nUse: /motm select <id1> <id2> <id3>");
        } else {
            sb.append("Selected: ").append(player.getSelectedPerks().size()).append(" perks\n");
            int nextMilestone = ((player.getLevel() / 10) + 1) * 10;
            if (nextMilestone <= LevelingManager.MAX_LEVEL) {
                sb.append("Next perk tier unlocks at Lv. ").append(nextMilestone).append("\n\n");
            } else {
                sb.append("All perk tiers unlocked.\n\n");
            }

            for (int tier = 1; tier <= pm.getCurrentTier(player.getLevel()); tier++) {
                List<Perk> tierPerks = pm.getPlayerPerksForTier(player, tier);
                if (!tierPerks.isEmpty()) {
                    sb.append("Tier ").append(tier).append(":\n");
                    for (Perk perk : tierPerks) {
                        sb.append("  ").append(perk.getName()).append("\n");
                    }
                }
            }
        }

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

        if (player.getSelectedStyles().isEmpty()) {
            return "[MOTM] Choose your style first with /motm style <styleId>";
        }

        PerkManager pm = mod.getPerkManager();
        if (!pm.hasPendingPerkSelection(player)) {
            return "[MOTM] No perk selection available right now.";
        }

        if (args.length < 4) {
            return "[MOTM] Usage: /motm select <perkId1> <perkId2> <perkId3>";
        }

        List<String> selectedIds = List.of(args[1], args[2], args[3]);

        PerkManager.ValidationResult validation = pm.validatePerkSelection(player, selectedIds);
        if (!validation.isValid()) {
            return "[MOTM] Selection failed:\n" + String.join("\n", validation.getErrors());
        }

        SynergyEngine.SynergyPreview preview = mod.getSynergyEngine()
                .previewSynergyChanges(player, selectedIds);

        boolean success = pm.applyPerkSelection(player, selectedIds, mod.getSynergyEngine());
        if (!success) {
            return "[MOTM] Perk selection failed.";
        }

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

    // --- /motm stats ---

    private String handleStats(PlayerData player) {
        StringBuilder sb = new StringBuilder("[MOTM] === Player Summary ===\n");
        sb.append("Name: ").append(player.getPlayerName()).append("\n");
        sb.append("Class: ").append(player.getPlayerClass() != null ? player.getPlayerClass() : "None").append("\n");
        sb.append("Style: ").append(formatSelectedStyleSummary(player)).append("\n");
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

    // --- /motm style [styleId] ---

    private String handleStyle(PlayerData player, String[] args) {
        if (player.getPlayerClass() == null) {
            return "[MOTM] Select a class first with /motm class <classId>";
        }

        List<StyleData> allStyles = mod.getDataLoader().getStylesForClass(player.getPlayerClass());

        if (args.length < 2) {
            StringBuilder sb = new StringBuilder("[MOTM] === " + player.getPlayerClass().toUpperCase() + " Styles ===\n");
            sb.append("Choose 1 style. Your style determines your abilities.\n\n");

            List<String> selected = player.getSelectedStyles();
            for (StyleData style : allStyles) {
                boolean isSelected = selected.contains(style.getId());
                sb.append(isSelected ? ">> " : "   ");
                sb.append(style.getId()).append(" - ").append(style.getName()).append("\n");
                sb.append("   ").append(compactText(style.getTheme(), 54)).append("\n");
                sb.append("   Abilities: ").append(formatAbilityNames(style)).append("\n");
            }

            if (selected.isEmpty()) {
                sb.append("\nUse: /motm style <styleId>");
            } else {
                sb.append("\nCurrent style: ").append(formatSelectedStyleSummary(player));
                sb.append("\nTo change: /motm style <styleId>");
            }
            return sb.toString();
        }

        String styleId = args[1].toLowerCase();
        StyleData selectedStyle = allStyles.stream()
                .filter(style -> style.getId().equals(styleId))
                .findFirst()
                .orElse(null);
        if (selectedStyle == null) {
            return "[MOTM] Invalid style. Use /motm style to see available options.";
        }

        boolean success = mod.getStyleManager().selectStyles(player, List.of(styleId));
        if (!success) {
            return "[MOTM] Invalid style selection. Use /motm style to see available styles.";
        }

        StringBuilder sb = new StringBuilder("[MOTM] Style selected!\n");
        sb.append("Style: ").append(selectedStyle.getName()).append("\n");
        sb.append("Theme: ").append(selectedStyle.getTheme()).append("\n");
        sb.append("Abilities:\n");
        for (AbilityData ability : selectedStyle.getAbilities()) {
            sb.append("  ").append(ability.getName())
                    .append(" (").append(ability.getCooldownSeconds()).append("s)")
                    .append(" - ").append(compactText(ability.getDescription(), 46)).append("\n");
        }
        mod.getResourceManager().synchronizePersistentState(player);
        mod.getResourceManager().initializeForPlayer(player.getPlayerId(), player.getPlayerClass());
        mod.getPlayerDataManager().savePlayerData(player);
        return sb.toString();
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
            rebuildPlayerRuntime(player);
        }
        mod.getPlayerDataManager().savePlayerData(player);
        return "[MOTM] You are now a " + race.getName() + "!\n"
                + "  " + race.getPassive() + "\n"
                + "  Special: " + race.getSpecial();
    }

    // --- /motm dev ... ---

    private String handleDev(PlayerData player, String[] args) {
        if (args.length < 2) {
            return getDevHelpMessage();
        }

        return switch (args[1].toLowerCase()) {
            case "help" -> getDevHelpMessage();
            case "level" -> handleDevLevel(player, args);
            case "xp" -> handleDevXp(player, args);
            case "class" -> handleDevClass(player, args);
            case "race" -> handleDevRace(player, args);
            case "perks" -> handleDevPerks(player, args);
            case "styles" -> handleDevStyles(player, args);
            case "reset" -> handleDevReset(player, args);
            default -> "[MOTM] Unknown dev subcommand.\n" + getDevHelpMessage();
        };
    }

    private String handleDevLevel(PlayerData player, String[] args) {
        if (args.length < 4) {
            return "[MOTM] Usage: /motm dev level <set|add> <value>";
        }

        Integer value = parseInteger(args[3]);
        if (value == null) {
            return "[MOTM] Level value must be a whole number.";
        }

        int oldLevel = player.getLevel();
        int newLevel = switch (args[2].toLowerCase()) {
            case "set" -> clampLevel(value);
            case "add" -> clampLevel(player.getLevel() + value);
            default -> -1;
        };

        if (newLevel < 1) {
            return "[MOTM] Usage: /motm dev level <set|add> <value>";
        }

        player.setLevel(newLevel);
        player.setCurrentXp(0);
        player.setTotalXpEarned(mod.getLevelingManager().calculateTotalXpToLevel(newLevel));
        updateDebugProgressionState(player);
        mod.getPlayerDataManager().savePlayerData(player);

        return "[MOTM] Dev: level changed " + oldLevel + " -> " + newLevel + ".";
    }

    private String handleDevXp(PlayerData player, String[] args) {
        if (args.length < 4) {
            return "[MOTM] Usage: /motm dev xp <set|add> <value>";
        }

        Integer value = parseInteger(args[3]);
        if (value == null) {
            return "[MOTM] XP value must be a whole number.";
        }

        int oldXp = player.getCurrentXp();
        int newXp = switch (args[2].toLowerCase()) {
            case "set" -> Math.max(0, value);
            case "add" -> Math.max(0, player.getCurrentXp() + value);
            default -> -1;
        };

        if (newXp < 0) {
            return "[MOTM] Usage: /motm dev xp <set|add> <value>";
        }

        int xpRequired = mod.getLevelingManager().calculateXpRequired(player.getLevel());
        if (xpRequired > 0) {
            newXp = Math.min(newXp, Math.max(0, xpRequired - 1));
        } else {
            newXp = 0;
        }

        player.setCurrentXp(newXp);
        int floorTotalXp = mod.getLevelingManager().calculateTotalXpToLevel(player.getLevel());
        player.setTotalXpEarned(Math.max(player.getTotalXpEarned(), floorTotalXp + newXp));
        mod.getPlayerDataManager().savePlayerData(player);

        return "[MOTM] Dev: XP changed " + oldXp + " -> " + newXp + ".";
    }

    private String handleDevClass(PlayerData player, String[] args) {
        if (args.length < 3) {
            return "[MOTM] Usage: /motm dev class <set|clear> [classId]";
        }

        return switch (args[2].toLowerCase()) {
            case "set" -> {
                if (args.length < 4) {
                    yield "[MOTM] Usage: /motm dev class set <terra|hydro|aero|corruptus>";
                }

                String classId = args[3].toLowerCase();
                if (!mod.getDataLoader().isValidClass(classId)) {
                    yield "[MOTM] Invalid class. Valid: terra, hydro, aero, corruptus.";
                }

                clearClassProgression(player);
                player.setPlayerClass(classId);
                player.setFirstJoin(false);
                updateDebugProgressionState(player);
                rebuildPlayerRuntime(player);
                mod.getPlayerDataManager().savePlayerData(player);

                ClassData classData = mod.getDataLoader().getClassData(classId);
                yield "[MOTM] Dev: class set to " + classData.getDisplayName() + ".";
            }
            case "clear" -> {
                clearClassProgression(player);
                player.setPlayerClass(null);
                rebuildPlayerRuntime(player);
                mod.getPlayerDataManager().savePlayerData(player);
                yield "[MOTM] Dev: class cleared.";
            }
            default -> "[MOTM] Usage: /motm dev class <set|clear> [classId]";
        };
    }

    private String handleDevRace(PlayerData player, String[] args) {
        if (args.length < 3) {
            return "[MOTM] Usage: /motm dev race <set|clear> [raceId]";
        }

        return switch (args[2].toLowerCase()) {
            case "set" -> {
                if (args.length < 4) {
                    yield "[MOTM] Usage: /motm dev race set <raceId>";
                }

                String raceId = args[3].toLowerCase();
                if (!mod.getDataLoader().isValidRace(raceId)) {
                    yield "[MOTM] Invalid race. Use /motm race to see valid options.";
                }

                player.setRace(raceId);
                rebuildPlayerRuntime(player);
                mod.getPlayerDataManager().savePlayerData(player);

                RaceData race = mod.getDataLoader().getRaceById(raceId);
                yield "[MOTM] Dev: race set to " + race.getName() + ".";
            }
            case "clear" -> {
                player.setRace(null);
                rebuildPlayerRuntime(player);
                mod.getPlayerDataManager().savePlayerData(player);
                yield "[MOTM] Dev: race cleared.";
            }
            default -> "[MOTM] Usage: /motm dev race <set|clear> [raceId]";
        };
    }

    private String handleDevPerks(PlayerData player, String[] args) {
        if (args.length < 3 || !"clear".equalsIgnoreCase(args[2])) {
            return "[MOTM] Usage: /motm dev perks clear";
        }

        clearPerkProgression(player);
        updateDebugProgressionState(player);
        rebuildPlayerRuntime(player);
        mod.getPlayerDataManager().savePlayerData(player);
        return "[MOTM] Dev: perks and perk history cleared.";
    }

    private String handleDevStyles(PlayerData player, String[] args) {
        if (args.length < 3 || !"clear".equalsIgnoreCase(args[2])) {
            return "[MOTM] Usage: /motm dev styles clear";
        }

        player.getSelectedStyles().clear();
        mod.getStyleManager().resetCooldowns(player.getPlayerId());
        mod.getPlayerDataManager().savePlayerData(player);
        return "[MOTM] Dev: styles cleared.";
    }

    private String handleDevReset(PlayerData player, String[] args) {
        if (args.length < 3 || !"player".equalsIgnoreCase(args[2])) {
            return "[MOTM] Usage: /motm dev reset player";
        }

        resetPlayerForDev(player);
        rebuildPlayerRuntime(player);
        mod.getPlayerDataManager().savePlayerData(player);
        return "[MOTM] Dev: player progression reset to a fresh state.";
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
                + "Flow:\n"
                + "  1. /motm class <id>\n"
                + "  2. /motm style <id>\n"
                + "  3. Use style abilities and level up\n"
                + "  4. /motm perks at Lv. 10+\n\n"
                + "Commands:\n"
                + "  /motm class [id]        - View/select class\n"
                + "  /motm race [id]         - View/select race\n"
                + "  /motm style [id]        - View/select your combat style\n"
                + "  /motm perks             - View perk choices (not styles)\n"
                + "  /motm select ...        - Select 3 perks\n"
                + "  /motm resources         - View class resources\n"
                + "  /motm stats             - View your statistics\n"
                + "  /motm level             - View XP progress\n"
                + "  /motm dev ...           - Testing/admin tools\n"
                + "  /motm help              - Show this help";
    }

    private String getHelpMessage() {
        return handleInfo();
    }

    private String getDevHelpMessage() {
        return "[MOTM] === Dev Commands ===\n"
                + "  /motm dev level set <n>\n"
                + "  /motm dev level add <n>\n"
                + "  /motm dev xp set <n>\n"
                + "  /motm dev xp add <n>\n"
                + "  /motm dev class set <id>\n"
                + "  /motm dev class clear\n"
                + "  /motm dev race set <id>\n"
                + "  /motm dev race clear\n"
                + "  /motm dev perks clear\n"
                + "  /motm dev styles clear\n"
                + "  /motm dev reset player";
    }

    private void rebuildPlayerRuntime(PlayerData player) {
        String playerId = player.getPlayerId();

        mod.getStyleManager().resetCooldowns(playerId);
        mod.getStatusEffectManager().clearEffects(playerId);
        mod.getElementalReactionManager().clearMarks(playerId);
        mod.getResourceManager().clearPlayerState(playerId);
        mod.getResourceManager().synchronizePersistentState(player);

        player.clearSynergyBonuses();
        player.clearRaceBonuses();

        if (player.getPlayerClass() == null) {
            return;
        }

        mod.getResourceManager().initializeForPlayer(playerId, player.getPlayerClass());
        mod.getPerkManager().reapplyAllPerks(player, mod.getSynergyEngine());

        if (player.getRace() != null) {
            mod.getRaceManager().applyRaceBonuses(player, mod.getStatusEffectManager());
        }
    }

    private String formatSelectedStyleSummary(PlayerData player) {
        StyleData style = getSelectedStyle(player);
        if (style == null) {
            return "None";
        }
        return style.getName() + " (" + style.getId() + ")";
    }

    private String getSelectedStyleName(PlayerData player) {
        StyleData style = getSelectedStyle(player);
        return style != null ? style.getName() : "None";
    }

    private StyleData getSelectedStyle(PlayerData player) {
        if (player.getPlayerClass() == null || player.getSelectedStyles().isEmpty()) {
            return null;
        }

        return mod.getDataLoader().getStyleById(player.getSelectedStyles().get(0), player.getPlayerClass());
    }

    private String formatAbilityNames(StyleData style) {
        return style.getAbilities().stream()
                .map(AbilityData::getName)
                .reduce((left, right) -> left + ", " + right)
                .orElse("None");
    }

    private String compactText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    private void clearClassProgression(PlayerData player) {
        clearPerkProgression(player);
        player.getSelectedStyles().clear();
        player.getClassResources().clear();
        mod.getStyleManager().resetCooldowns(player.getPlayerId());
    }

    private void clearPerkProgression(PlayerData player) {
        player.getSelectedPerks().clear();
        player.getPerkSelectionHistory().clear();
        player.setPerkSelectionPoints(0);
        player.setPendingPerkTier(null);
        player.clearSynergyBonuses();
    }

    private void updateDebugProgressionState(PlayerData player) {
        int availableSelections = Math.max(0,
                mod.getPerkManager().getCurrentTier(player.getLevel()) - player.getPerkSelectionHistory().size());
        player.setPerkSelectionPoints(availableSelections);
        player.setPendingPerkTier(availableSelections > 0
                ? player.getPerkSelectionHistory().size() + 1
                : null);
    }

    private void resetPlayerForDev(PlayerData player) {
        player.setPlayerClass(null);
        player.setRace(null);
        player.setLevel(1);
        player.setCurrentXp(0);
        player.setTotalXpEarned(0);
        player.setFirstJoin(true);
        player.getSelectedPerks().clear();
        player.getPerkSelectionHistory().clear();
        player.setPerkSelectionPoints(0);
        player.setPendingPerkTier(null);
        player.getSelectedStyles().clear();
        player.getClassResources().clear();
        player.setWaterContainerTier(0);
        player.getAchievements().clear();

        player.getStatistics().getMobsKilled().clear();
        player.getStatistics().getBossesDefeated().clear();
        player.getStatistics().setTotalDamageDealt(0);
        player.getStatistics().setTotalDamageTaken(0);
        player.getStatistics().setTotalHealingDone(0);
        player.getStatistics().setDeaths(0);
        player.getStatistics().setPlaytimeSeconds(0);
        player.getStatistics().setHighestCombo(0);

        player.initRuntimeFields();
        player.getRecentKills().clear();
        player.setComboCount(0);
        player.setLastKillTime(null);
        player.setPartySize(1);
        player.setRestedBonus(0);
        player.setLastLogoutTimestamp(null);
        player.clearSynergyBonuses();
        player.clearRaceBonuses();
    }

    private Integer parseInteger(String rawValue) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private int clampLevel(int level) {
        return Math.max(1, Math.min(LevelingManager.MAX_LEVEL, level));
    }
}
