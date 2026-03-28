package com.motm.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.MenteesMod;
import com.motm.manager.LevelingManager;
import com.motm.manager.PerkManager;
import com.motm.manager.SpellbookManager;
import com.motm.manager.StyleManager;
import com.motm.manager.SynergyEngine;
import com.motm.model.AbilityData;
import com.motm.model.ClassData;
import com.motm.model.Perk;
import com.motm.model.PlayerData;
import com.motm.model.RaceData;
import com.motm.model.StyleData;
import com.motm.util.AbilityPresentation;
import com.motm.util.PassivePresentation;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main command handler for /motm commands.
 */
public class MotmCommand {

    private static final Logger LOG = Logger.getLogger("MOTM");
    private static final List<String> CLASS_ID_ORDER = List.of("terra", "hydro", "aero", "corruptus");
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

        return execute(player, args, null);
    }

    public String execute(Player runtimePlayer, String[] args) {
        if (runtimePlayer == null) {
            return "[MOTM] Error: Player runtime not found.";
        }

        String playerId = mod.findOnlinePlayerId(runtimePlayer);
        if (playerId == null) {
            playerId = mod.getRuntimePlayerId(runtimePlayer);
        }
        if (playerId == null) {
            return "[MOTM] Error: Player runtime not found.";
        }

        PlayerData player = mod.getPlayerDataManager().getOnlinePlayer(playerId);
        if (player == null) {
            return "[MOTM] Error: Player data not found.";
        }

        return execute(player, args, runtimePlayer);
    }

    private String execute(PlayerData player, String[] args, Player runtimePlayer) {
        if (args.length == 0) {
            return getHelpMessage();
        }

        return switch (args[0].toLowerCase()) {
            case "class" -> handleClass(player, args);
            case "perks" -> handlePerks(player);
            case "select" -> handleSelect(player, args);
            case "style" -> handleStyle(player, args);
            case "abilities" -> handleAbilities(player);
            case "cast" -> handleCast(player, args, runtimePlayer);
            case "spellbook", "book" -> handleSpellbook(player, args, runtimePlayer);
            case "controls" -> handleControls(player, args, runtimePlayer);
            case "race" -> handleRace(player, args);
            case "resources" -> handleResources(player);
            case "stats" -> handleStats(player);
            case "level" -> handleLevel(player, runtimePlayer);
            case "audit" -> handleAudit();
            case "dev" -> handleDev(player, args, runtimePlayer);
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
                    + classData.getPassiveAbility().getDescription()
                    + "\nPassive Flow: " + PassivePresentation.buildPassiveSummary(classData.getPassiveAbility())
                    + "\nPassive State: " + mod.getClassPassiveManager().buildPassiveStateSummary(player);
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
        sb.append("Styles grant abilities. Perks are passive augments and synergies.\n");

        if (pm.hasPendingPerkSelection(player)) {
            int pendingTier = pm.getPendingSelectionTier(player);
            sb.append("Tier ").append(pendingTier).append(" choices: pick 3 of 10\n\n");
            List<Perk> available = pm.getAvailablePerks(player);
            for (int i = 0; i < available.size(); i++) {
                Perk perk = available.get(i);
                sb.append("[").append(i + 1).append("] ").append(perk.getName()).append("\n");
                sb.append("  ").append(compactText(perk.getDescription(), 60)).append("\n");
            }
            sb.append("\nUse: /motm select <choice1> <choice2> <choice3>");
            sb.append("\nExample: /motm select 1 4 7");
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
            return "[MOTM] Usage: /motm select <choice1> <choice2> <choice3>";
        }

        List<Perk> available = pm.getAvailablePerks(player);
        SelectionResolution selectionResolution = resolvePerkSelections(available, List.of(args[1], args[2], args[3]));
        if (!selectionResolution.invalidSelections().isEmpty()) {
            return "[MOTM] Invalid perk choice(s): " + String.join(", ", selectionResolution.invalidSelections())
                    + "\nUse /motm perks to see the numbered options.";
        }

        List<String> selectedIds = selectionResolution.resolvedIds();

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

    private String handleLevel(PlayerData player, Player runtimePlayer) {
        LevelingManager lm = mod.getLevelingManager();
        int required = lm.calculateXpRequired(player.getLevel());
        double percent = lm.getXpProgressPercent(player);
        String playerId = runtimePlayer != null
                ? mod.findOnlinePlayerId(runtimePlayer)
                : player.getPlayerId();
        if (playerId == null && runtimePlayer != null) {
            playerId = mod.getRuntimePlayerId(runtimePlayer);
        }
        if (playerId == null) {
            playerId = player.getPlayerId();
        }
        int hostileAnchorLevel = mod.getAverageOnlinePlayerLevelForPlayer(playerId);
        String difficulty = mod.getMobScalingManager().getDifficultyDescription(hostileAnchorLevel);
        String bossDifficulty = mod.getMobScalingManager().getBossDifficultyDescription(hostileAnchorLevel, "boss");

        StringBuilder sb = new StringBuilder("[MOTM] === Level Progress ===\n");
        sb.append("Level: ").append(player.getLevel()).append(" / ").append(LevelingManager.MAX_LEVEL).append("\n");
        sb.append("XP: ").append(player.getCurrentXp()).append(" / ").append(required)
                .append(" (").append(String.format("%.1f", percent)).append("%)\n");
        sb.append("Total XP Earned: ").append(player.getTotalXpEarned()).append("\n");
        sb.append("Level Growth: ").append(lm.describePlayerStatGrowth(player.getLevel())).append("\n");
        sb.append("Hostile Mob Anchor: Lv ").append(hostileAnchorLevel)
                .append(" average level in this world").append("\n");
        sb.append("Hostile Scaling: ").append(difficulty).append("\n");
        sb.append("Boss Scaling: ").append(bossDifficulty).append("\n");

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
        if (args.length < 2) {
            return buildStyleOverview(player);
        }

        String styleId = args[1].toLowerCase();
        ResolvedStyleSelection resolvedStyle = resolveStyleSelection(styleId);
        if (resolvedStyle == null) {
            return "[MOTM] Invalid style. Use /motm style to see available options.";
        }

        boolean internalTestFlow = mod.isDevToolsEnabled();
        boolean autoClassSwap = internalTestFlow
                && (player.getPlayerClass() == null || !player.getPlayerClass().equals(resolvedStyle.classId()));

        if (!internalTestFlow) {
            if (player.getPlayerClass() == null) {
                return "[MOTM] Select a class first with /motm class <classId>.\n"
                        + "Then use /motm style <styleId> for that class.";
            }
            if (!player.getPlayerClass().equals(resolvedStyle.classId())) {
                return "[MOTM] " + resolvedStyle.style().getName()
                        + " belongs to " + resolvedStyle.classData().getDisplayName()
                        + ". Select that class first with /motm class " + resolvedStyle.classId() + ".";
            }
        } else {
            clearClassProgression(player);
            player.setPlayerClass(resolvedStyle.classId());
            player.setFirstJoin(false);
            updateDebugProgressionState(player);
        }

        boolean success = mod.getStyleManager().selectStyles(player, List.of(styleId));
        if (!success) {
            return "[MOTM] Invalid style selection. Use /motm style to see available styles.";
        }

        StringBuilder sb = new StringBuilder(internalTestFlow
                ? "[MOTM] Testing loadout ready!\n"
                : "[MOTM] Style selected!\n");
        if (internalTestFlow) {
            sb.append("Class: ").append(resolvedStyle.classData().getDisplayName()).append("\n");
            if (autoClassSwap) {
                sb.append("Flow: class auto-set from style id.\n");
            }
            sb.append("Reset: class perks, style, resources, and cooldowns cleared for a clean test swap.\n");
        }
        sb.append("Style: ").append(resolvedStyle.style().getName()).append("\n");
        sb.append("Theme: ").append(resolvedStyle.style().getTheme()).append("\n");
        sb.append("Abilities:\n");
        for (AbilityData ability : resolvedStyle.style().getAbilities()) {
            String profile = buildAbilityProfileSummary(ability);
            String visuals = buildAbilityVisualSummary(player.getPlayerClass(), resolvedStyle.style().getId(), ability);
            sb.append("  ").append(ability.getName())
                    .append(" (").append(ability.getCooldownSeconds()).append("s)")
                    .append(" - ").append(compactText(
                            profile.isBlank() ? ability.getDescription() : profile,
                            58))
                    .append("\n");
            if (!visuals.isBlank()) {
                sb.append("    Visuals: ").append(compactText(visuals, 64)).append("\n");
            }
        }
        mod.getPlayerDataManager().savePlayerData(player);
        rebuildPlayerRuntime(player);
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

    // --- /motm spellbook [section] ---

    private String handleSpellbook(PlayerData player, String[] args, Player runtimePlayer) {
        if (args.length >= 2 && "give".equalsIgnoreCase(args[1])) {
            if (runtimePlayer == null && mod.getRuntimePlayer(player.getPlayerId()) == null) {
                return "[MOTM] Join a world and run this in-game to receive the spellbook item.";
            }
            return mod.queueSpellbookGrant(player.getPlayerId());
        }

        SpellbookManager spellbookManager = mod.getSpellbookManager();
        SpellbookManager.Section section = args.length >= 2
                ? spellbookManager.parseSection(args[1])
                : SpellbookManager.Section.OVERVIEW;
        if (section == null) {
            return "[MOTM] Unknown spellbook section.\n"
                    + "Sections: " + spellbookManager.getSectionList();
        }
        return spellbookManager.render(player, section);
    }

    private String handleControls(PlayerData player, String[] args, Player runtimePlayer) {
        if (args.length >= 2 && "givebook".equalsIgnoreCase(args[1])) {
            if (runtimePlayer == null && mod.getRuntimePlayer(player.getPlayerId()) == null) {
                return "[MOTM] Join a world and run this in-game to receive the spellbook item.";
            }
            return mod.queueSpellbookGrant(player.getPlayerId());
        }

        StyleData style = getSelectedStyle(player);
        String slot1 = describeAbilitySlot(style, 0);
        String slot2 = describeAbilitySlot(style, 1);
        String slot3 = describeAbilitySlot(style, 2);
        String bookStatus = runtimePlayer != null && mod.playerHasSpellbook(runtimePlayer)
                ? "Present"
                : "Optional";

        return "[MOTM] === Ability Controls ===\n"
                + "Default spellbook controls while equipped:\n"
                + "Left Click -> Slot 1: " + slot1 + "\n"
                + "Right Click -> Slot 2: " + slot2 + "\n"
                + "Use -> Slot 3: " + slot3 + "\n"
                + "Alternate bindings: Ability 1 / 2 / 3 also cast the same three slots.\n"
                + "Spellbook management/readout: /motm spellbook overview\n"
                + "Book overview gesture: Crouch + Use\n"
                + "Spellbook Status: " + bookStatus + "\n"
                + "Weapon swaps are encouraged for follow-up attacks after casting.\n"
                + "Fallback: /motm cast <abilityId>\n"
                + "Optional: /motm spellbook give"
                + (mod.isDevToolsEnabled() ? " | /motm dev book" : "");
    }

    // --- /motm abilities ---

    private String handleAbilities(PlayerData player) {
        if (player.getPlayerClass() == null) {
            return "[MOTM] Select a class first with /motm class <classId>";
        }

        StyleData style = getSelectedStyle(player);
        if (style == null) {
            return "[MOTM] Choose your style first with /motm style <styleId>";
        }

        List<AbilityData> abilities = mod.getStyleManager().getAvailableAbilities(player);
        if (abilities.isEmpty()) {
            return "[MOTM] No abilities available for your current style.";
        }

        StringBuilder sb = new StringBuilder("[MOTM] === Abilities ===\n");
        sb.append("Style: ").append(style.getName())
                .append(" | Resource: ").append(displayStyleResource(style)).append("\n\n");

        for (AbilityData ability : abilities) {
            double remainingCooldown = mod.getStyleManager()
                    .getRemainingCooldownSeconds(player.getPlayerId(), ability.getId());

            sb.append(ability.getId()).append(" - ").append(ability.getName()).append("\n");
            sb.append("  ").append(buildAbilityEffectSummary(ability)).append("\n");
            String profile = buildAbilityProfileSummary(ability);
            if (!profile.isBlank()) {
                sb.append("  ").append(profile).append("\n");
            }
            String visuals = buildAbilityVisualSummary(player.getPlayerClass(), style.getId(), ability);
            if (!visuals.isBlank()) {
                sb.append("  Visuals: ").append(visuals).append("\n");
            }
            sb.append("  Cost: ").append(formatResourceCost(style, ability))
                    .append(" | CD: ").append(formatDecimal(ability.getCooldownSeconds())).append("s")
                    .append(" | Status: ")
                    .append(remainingCooldown > 0
                            ? "Cooldown " + formatDecimal(remainingCooldown) + "s"
                            : "Ready")
                    .append("\n");
            sb.append("  ").append(compactText(ability.getDescription(), 58)).append("\n\n");
        }

        sb.append("Use: /motm cast <abilityId>\n");
        sb.append("Equip the spellbook, then use Hytale Ability 1 / 2 / 3 for live in-world casts.");
        return sb.toString();
    }

    // --- /motm cast <abilityId> ---

    private String handleCast(PlayerData player, String[] args, Player runtimePlayer) {
        if (player.getPlayerClass() == null) {
            return "[MOTM] Select a class first with /motm class <classId>";
        }

        StyleData style = getSelectedStyle(player);
        if (style == null) {
            return "[MOTM] Choose your style first with /motm style <styleId>";
        }

        if (args.length < 2) {
            return "[MOTM] Usage: /motm cast <abilityId>";
        }

        String abilityId = args[1].toLowerCase();
        AbilityData ability = mod.getStyleManager().findAbility(player, abilityId);
        if (ability == null) {
            return "[MOTM] Unknown ability. Use /motm abilities to see valid IDs.";
        }

        return castResolvedAbility(player, style, ability, runtimePlayer, null, null, false);
    }

    public String castAbilityBySlot(Player runtimePlayer,
                                    int slot,
                                    Ref<EntityStore> targetRef,
                                    Vector3i targetBlock) {
        if (runtimePlayer == null) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        String playerId = mod.findOnlinePlayerId(runtimePlayer);
        if (playerId == null) {
            playerId = mod.getRuntimePlayerId(runtimePlayer);
        }
        if (playerId == null) {
            return "[MOTM] Runtime player context is unavailable.";
        }

        PlayerData player = mod.getPlayerDataManager().getOnlinePlayer(playerId);
        if (player == null) {
            return "[MOTM] Error: Player data not found.";
        }

        if (player.getPlayerClass() == null) {
            return "[MOTM] Select a class first with /motm class <classId>";
        }

        StyleData style = getSelectedStyle(player);
        if (style == null) {
            return "[MOTM] Choose your style first with /motm style <styleId>";
        }

        if (slot < 1 || slot > style.getAbilities().size()) {
            return "[MOTM] Slot " + slot + " is not bound for your current style.";
        }

        AbilityData ability = style.getAbilities().get(slot - 1);
        LOG.info("[MOTM] Slot cast resolved: player="
                + player.getPlayerName()
                + " style=" + style.getId()
                + " slot=" + slot
                + " ability=" + ability.getId());
        return castResolvedAbility(player, style, ability, runtimePlayer, targetRef, targetBlock, true);
    }

    private String castResolvedAbility(PlayerData player,
                                       StyleData style,
                                       AbilityData ability,
                                       Player runtimePlayer,
                                       Ref<EntityStore> targetRef,
                                       Vector3i targetBlock,
                                       boolean quietSuccess) {
        if (runtimePlayer == null) {
            return "[MOTM] Join a world and run this in-game to trigger live ability playback.";
        }

        mod.queueAbilityCast(player.getPlayerId(), ability.getId(), targetRef, targetBlock, !quietSuccess);
        return quietSuccess ? "" : "[MOTM] Cast queued: " + ability.getName() + ".";
    }

    public String executeQueuedAbilityCast(String playerId,
                                           String abilityId,
                                           Player runtimePlayer,
                                           Ref<EntityStore> targetRef,
                                           Vector3i targetBlock) {
        PlayerData player = mod.getPlayerDataManager().getOnlinePlayer(playerId);
        if (player == null) {
            return "[MOTM] Error: Player data not found.";
        }

        StyleData style = getSelectedStyle(player);
        if (style == null) {
            return "[MOTM] That ability is unavailable for your current style.";
        }

        AbilityData ability = mod.getStyleManager().findAbility(player, abilityId);
        if (ability == null) {
            return "[MOTM] That ability is unavailable.";
        }

        try {
            StyleManager styleManager = mod.getStyleManager();
            StyleManager.ActionState actionState = styleManager.getActionState(player.getPlayerId());
            if (actionState != null) {
                String phase = actionState.phase() == StyleManager.AbilityPhase.CASTING ? "casting" : "recovering";
                return "[MOTM] " + actionState.abilityName() + " is still " + phase + " for "
                        + formatDecimal(actionState.remainingSeconds()) + "s.";
            }

            String useFailureReason = styleManager.getUseFailureReason(player, ability);
            if (!useFailureReason.isBlank()) {
                return "[MOTM] " + useFailureReason;
            }

            boolean deactivatingToggle = styleManager.isToggleActive(player.getPlayerId(), abilityId);
            String castRestriction = deactivatingToggle ? "" : mod.getGameplayPlaybackManager().getCastRestriction(player, ability);
            if (!castRestriction.isBlank()) {
                return "[MOTM] " + castRestriction;
            }

            StyleManager.AbilityUseResult useResult = styleManager.useAbility(player, abilityId);
            if (!useResult.success()) {
                return "[MOTM] " + useResult.failureReason();
            }

            if (useResult.toggledOff()) {
                mod.refreshStatusHud(player.getPlayerId());
                String runtimeSummary = mod.getGameplayPlaybackManager().deactivateAbilityRuntime(player, abilityId);
                StringBuilder toggledOff = new StringBuilder("[MOTM] Toggled off ")
                        .append(ability.getName())
                        .append(".");
                if (useResult.cooldownSeconds() > 0) {
                    toggledOff.append(" Cooldown: ")
                            .append(formatDecimal(useResult.cooldownSeconds()))
                            .append("s.");
                }
                if (!runtimeSummary.isBlank()) {
                    toggledOff.append(" Runtime: ").append(runtimeSummary).append(".");
                }
                return toggledOff.toString();
            }

            AbilityData activated = useResult.ability();
            mod.refreshStatusHud(player.getPlayerId());

            StringBuilder sb = new StringBuilder("[MOTM] Cast ").append(activated.getName()).append("!");
            if (useResult.maxCharges() > 0) {
                sb.append(" Charges ")
                        .append(useResult.currentCharges()).append("/").append(useResult.maxCharges()).append(".");
            }
            if (useResult.toggleActive()) {
                sb.append(" Toggle ON.");
            }

            if (runtimePlayer != null) {
                var execution = mod.getGameplayPlaybackManager().executeAbility(
                        runtimePlayer,
                        player,
                        style,
                        activated,
                        new com.motm.manager.GameplayPlaybackManager.CastContext(targetRef, targetBlock)
                );
                if (!execution.summary().isBlank()) {
                    sb.append(" Runtime: ").append(execution.summary()).append(".");
                }
            }

            return sb.toString();
        } catch (Exception e) {
            LOG.log(Level.SEVERE,
                    "[MOTM] Cast failed safely for " + player.getPlayerName() + " ability=" + abilityId,
                    e);
            return "[MOTM] Cast failed safely. The error was logged instead of silently breaking the mod.";
        }
    }

    // --- /motm dev ... ---

    private String handleDev(PlayerData player, String[] args, Player runtimePlayer) {
        if (!mod.isDevToolsEnabled()) {
            return mod.devToolsDisabledMessage();
        }
        if (args.length < 2) {
            return getDevHelpMessage();
        }

        return switch (args[1].toLowerCase()) {
            case "help" -> getDevHelpMessage();
            case "book" -> handleDevBook(player, runtimePlayer);
            case "test" -> handleDevTest(player, args, runtimePlayer);
            case "freecast" -> handleDevFreeCast(player, args);
            case "clear" -> handleDevClear(player, args);
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

    private String handleDevBook(PlayerData player, Player runtimePlayer) {
        if (runtimePlayer == null && mod.getRuntimePlayer(player.getPlayerId()) == null) {
            return "[MOTM] Join a world and run this in-game to receive the Dev Grimoire.";
        }
        return mod.queueDevBookGrant(player.getPlayerId());
    }

    private String handleDevTest(PlayerData player, String[] args, Player runtimePlayer) {
        if (args.length < 3) {
            return "[MOTM] Usage: /motm dev test <style <styleId>|status|stop>";
        }
        if (runtimePlayer == null && mod.getRuntimePlayer(player.getPlayerId()) == null) {
            return "[MOTM] Join a world and run this in-game to start a live style test.";
        }

        return switch (args[2].toLowerCase()) {
            case "style" -> {
                if (args.length < 4) {
                    yield "[MOTM] Usage: /motm dev test style <styleId>";
                }
                yield mod.startStyleTest(player.getPlayerId(), args[3]);
            }
            case "status" -> mod.getStyleTestStatus(player.getPlayerId());
            case "stop" -> mod.stopStyleTest(player.getPlayerId());
            default -> "[MOTM] Usage: /motm dev test <style <styleId>|status|stop>";
        };
    }

    private String handleDevFreeCast(PlayerData player, String[] args) {
        if (args.length < 3) {
            return "[MOTM] Usage: /motm dev freecast <on|off>\n"
                    + "Current: " + (mod.isFreeCastEnabled(player.getPlayerId()) ? "ON" : "OFF");
        }

        return switch (args[2].toLowerCase()) {
            case "on", "enable", "enabled", "true" -> {
                mod.setFreeCastEnabled(player.getPlayerId(), true);
                mod.refreshStatusHud(player.getPlayerId());
                yield "[MOTM] Dev: free-cast enabled. Ability costs are ignored for testing.";
            }
            case "off", "disable", "disabled", "false" -> {
                mod.setFreeCastEnabled(player.getPlayerId(), false);
                mod.refreshStatusHud(player.getPlayerId());
                yield "[MOTM] Dev: free-cast disabled. Normal resource costs restored.";
            }
            default -> "[MOTM] Usage: /motm dev freecast <on|off>";
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
        mod.refreshPlayerProgressionBonuses(player.getPlayerId());
        mod.refreshStatusHud(player.getPlayerId());

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
        mod.refreshStatusHud(player.getPlayerId());

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
        mod.getPlayerDataManager().savePlayerData(player);
        rebuildPlayerRuntime(player);
        return "[MOTM] Dev: styles cleared.";
    }

    private String handleDevClear(PlayerData player, String[] args) {
        if (args.length >= 3 && !"player".equalsIgnoreCase(args[2]) && !"all".equalsIgnoreCase(args[2])) {
            return "[MOTM] Usage: /motm dev clear\n"
                    + "Optional: /motm dev clear player";
        }

        return performFullDevPlayerClear(player);
    }

    private String handleDevReset(PlayerData player, String[] args) {
        if (args.length >= 3 && !"player".equalsIgnoreCase(args[2]) && !"all".equalsIgnoreCase(args[2])) {
            return "[MOTM] Usage: /motm dev reset\n"
                    + "Optional: /motm dev reset player";
        }

        return performFullDevPlayerClear(player);
    }

    // --- /motm resources ---

    private String handleResources(PlayerData player) {
        if (player.getPlayerClass() == null) {
            return "[MOTM] Select a class first with /motm class <classId>";
        }
        StringBuilder sb = new StringBuilder("[MOTM] ")
                .append(mod.getResourceManager().getResourceDisplay(player.getPlayerId(), player.getPlayerClass()));
        if (mod.isFreeCastEnabled(player.getPlayerId())) {
            sb.append("\nDev Free-Cast: ON");
        }
        return sb.toString();
    }

    // --- /motm info ---

    private String handleInfo() {
        return "[MOTM] === Mentees of the Mystical ===\n"
                + "Version: 1.0.1\n"
                + "4 Classes | 40 Styles | 12 Races | 800 Perks | Level 1-200\n"
                + "Elemental Reactions | Dynamic Mob Scaling | Synergy System\n\n"
                + "Flow:\n"
                + "  1. /motm class <id>\n"
                + "  2. /motm style <id>\n"
                + "  3. /motm spellbook overview\n"
                + "  4. Equip the spellbook and use Hytale Ability 1 / 2 / 3\n"
                + "  5. /motm abilities and /motm cast <abilityId>\n"
                + "  6. /motm perks at Lv. 10+\n\n"
                + "Commands:\n"
                + "  /motm class [id]        - View/select class\n"
                + "  /motm race [id]         - View/select race\n"
                + "  /motm style [id]        - View/select combat style"
                + (mod.isDevToolsEnabled() ? " (auto-loads matching class in test builds)\n" : "\n")
                + "  /motm spellbook [page]  - Open the spellbook page in chat\n"
                + "  /motm spellbook give    - Spawn the normal spellbook\n"
                + "  /motm controls          - View ability input bindings\n"
                + "  /motm abilities         - View ability IDs and cooldowns\n"
                + "  /motm cast <abilityId>  - Test-cast a style ability\n"
                + "  /motm audit             - Run the preflight data/runtime audit\n"
                + "  /motm perks             - View perk choices (not styles)\n"
                + "  /motm select ...        - Select 3 perks by number\n"
                + "  /motm resources         - View class resources\n"
                + "  /motm stats             - View your statistics\n"
                + "  /motm level             - View XP progress\n"
                + buildDevHelpSummary()
                + "  /motm help              - Show this help";
    }

    private String getHelpMessage() {
        return handleInfo();
    }

    private String handleAudit() {
        return mod.runPreflightAudit().toChatSummary();
    }

    private String getDevHelpMessage() {
        if (!mod.isDevToolsEnabled()) {
            return mod.devToolsDisabledMessage();
        }
        return "[MOTM] === Dev Commands ===\n"
                + "  /motm dev book\n"
                + "  /motm dev test style <styleId>\n"
                + "  /motm dev test status\n"
                + "  /motm dev test stop\n"
                + "  /motm dev freecast <on|off>\n"
                + "  /motm dev clear\n"
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
                + "  /motm dev reset";
    }

    private String buildDevHelpSummary() {
        if (!mod.isDevToolsEnabled()) {
            return "  Dev tools              - Disabled in this build/server\n";
        }
        return "  /motm dev ...           - Testing/admin tools\n"
                + "  /motm dev book          - Spawn the Dev Grimoire\n"
                + "  /motm dev test ...      - Run a live style test sequence\n";
    }

    private void rebuildPlayerRuntime(PlayerData player) {
        mod.rebuildPlayerRuntime(player);
    }

    private String formatSelectedStyleSummary(PlayerData player) {
        StyleData style = getSelectedStyle(player);
        if (style == null) {
            return "None";
        }
        return style.getName() + " (" + style.getId() + ")";
    }

    private String buildStyleOverview(PlayerData player) {
        if (player.getPlayerClass() == null) {
            StringBuilder sb = new StringBuilder("[MOTM] === Styles ===\n");
            sb.append("Pick any style id with /motm style <styleId>\n\n");
            for (String classId : CLASS_ID_ORDER) {
                ClassData classData = mod.getDataLoader().getClassData(classId);
                List<StyleData> styles = mod.getDataLoader().getStylesForClass(classId);
                if (classData == null || styles.isEmpty()) {
                    continue;
                }
                sb.append(classData.getDisplayName()).append(": ");
                sb.append(styles.stream()
                        .map(StyleData::getId)
                        .reduce((left, right) -> left + ", " + right)
                        .orElse("none"));
                sb.append("\n");
            }
            if (mod.isDevToolsEnabled()) {
                sb.append("\nInternal test flow: /motm style <styleId> auto-loads the matching class")
                        .append(" and clears prior class/style state.");
            } else {
                sb.append("\nPublic flow: choose your class first with /motm class <classId>.");
            }
            return sb.toString();
        }

        List<StyleData> allStyles = mod.getDataLoader().getStylesForClass(player.getPlayerClass());
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
        if (mod.isDevToolsEnabled()) {
            sb.append("\nDev shortcut: /motm style <styleId> can auto-swap into any class for testing.");
        }
        return sb.toString();
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

    private ResolvedStyleSelection resolveStyleSelection(String styleId) {
        for (String classId : CLASS_ID_ORDER) {
            StyleData style = mod.getDataLoader().getStyleById(styleId, classId);
            if (style != null) {
                ClassData classData = mod.getDataLoader().getClassData(classId);
                if (classData != null) {
                    return new ResolvedStyleSelection(classId, classData, style);
                }
            }
        }
        return null;
    }

    private String formatAbilityNames(StyleData style) {
        return style.getAbilities().stream()
                .map(AbilityData::getName)
                .reduce((left, right) -> left + ", " + right)
                .orElse("None");
    }

    private String describeAbilitySlot(StyleData style, int index) {
        if (style == null || index < 0 || index >= style.getAbilities().size()) {
            return "Empty";
        }

        AbilityData ability = style.getAbilities().get(index);
        return ability.getName() + " [" + ability.getId() + "]";
    }

    private SelectionResolution resolvePerkSelections(List<Perk> available, List<String> selections) {
        List<String> resolvedIds = new java.util.ArrayList<>();
        List<String> invalidSelections = new java.util.ArrayList<>();

        for (String selection : selections) {
            Integer numericChoice = parseInteger(selection);
            if (numericChoice != null) {
                int index = numericChoice - 1;
                if (index >= 0 && index < available.size()) {
                    resolvedIds.add(available.get(index).getId());
                } else {
                    invalidSelections.add(selection);
                }
                continue;
            }

            Perk matchedPerk = available.stream()
                    .filter(perk -> perk.getId().equalsIgnoreCase(selection))
                    .findFirst()
                    .orElse(null);
            if (matchedPerk != null) {
                resolvedIds.add(matchedPerk.getId());
            } else {
                invalidSelections.add(selection);
            }
        }

        return new SelectionResolution(resolvedIds, invalidSelections);
    }

    private String buildAbilityEffectSummary(AbilityData ability) {
        return AbilityPresentation.buildEffectSummary(ability);
    }

    private String buildAbilityProfileSummary(AbilityData ability) {
        return AbilityPresentation.buildSpatialSummary(ability);
    }

    private String buildAbilityVisualSummary(String classId, String styleId, AbilityData ability) {
        return AbilityPresentation.buildVisualSummary(classId, styleId, ability);
    }

    private String buildAbilityVisualDetail(String classId, String styleId, AbilityData ability) {
        return AbilityPresentation.buildVisualDetail(classId, styleId, ability);
    }

    private String formatResourceCost(StyleData style, AbilityData ability) {
        if (ability.getResourceCost() <= 0 || style == null || style.getResourceType() == null || style.getResourceType().isBlank()) {
            return "none";
        }
        return ability.getResourceCost() + " " + mod.getResourceManager().getDisplayName(style.getResourceType());
    }

    private String displayStyleResource(StyleData style) {
        if (style == null || style.getResourceType() == null || style.getResourceType().isBlank()) {
            return "None";
        }
        return mod.getResourceManager().getDisplayName(style.getResourceType());
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
        mod.getStatusEffectManager().clearEffects(player.getPlayerId());
        mod.getElementalReactionManager().clearMarks(player.getPlayerId());
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
        mod.getStyleManager().resetCooldowns(player.getPlayerId());
        mod.getStatusEffectManager().clearEffects(player.getPlayerId());
        mod.getElementalReactionManager().clearMarks(player.getPlayerId());
    }

    private String performFullDevPlayerClear(PlayerData player) {
        resetPlayerForDev(player);
        rebuildPlayerRuntime(player);
        mod.getPlayerDataManager().savePlayerData(player);
        mod.refreshStatusHud(player.getPlayerId());
        return "[MOTM] Dev: player cleared to a fresh state.\n"
                + "Reset: class, race, style, perks, level, XP, resources, cooldowns, statuses, and marks.";
    }

    private Integer parseInteger(String rawValue) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String formatDecimal(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format("%.1f", value);
    }

    private int clampLevel(int level) {
        return Math.max(1, Math.min(LevelingManager.MAX_LEVEL, level));
    }

    private record SelectionResolution(List<String> resolvedIds, List<String> invalidSelections) {}

    private record ResolvedStyleSelection(String classId, ClassData classData, StyleData style) {}
}
