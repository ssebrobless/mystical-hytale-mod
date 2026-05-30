package com.motm.command;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import org.joml.Vector3d;
import org.joml.Vector3i;
import com.hypixel.hytale.protocol.MovementSettings;
import com.hypixel.hytale.protocol.MovementStates;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.movement.MovementManager;
import com.hypixel.hytale.server.core.entity.movement.MovementStatesComponent;
import com.hypixel.hytale.server.core.modules.entity.component.ModelComponent;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatValue;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
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
import com.motm.model.StatusEffect;
import com.motm.model.StyleData;
import com.motm.proof.MotmProofCatalog;
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
    private final MotmDevCommandRouter devCommandRouter;

    public MotmCommand(MenteesMod mod) {
        this.mod = mod;
        this.devCommandRouter = new MotmDevCommandRouter(mod, this);
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
            case "style" -> handleStyle(player, args, runtimePlayer);
            case "abilities" -> handleAbilities(player);
            case "cast" -> handleCast(player, args, runtimePlayer);
            case "spellbook", "book" -> handleSpellbook(player, args, runtimePlayer);
            case "controls" -> handleControls(player, args, runtimePlayer);
            case "casting", "resources" -> handleCastingModel(player);
            case "stats" -> handleStats(player, args);
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
            sb.append("Perk choice ").append(pendingTier).append(": pick 1 from the shared class-themed pool\n\n");
            List<Perk> available = pm.getAvailablePerks(player);
            for (int i = 0; i < available.size(); i++) {
                Perk perk = available.get(i);
                sb.append("[").append(i + 1).append("] ").append(perk.getName()).append("\n");
                sb.append("  ").append(compactText(perk.getDescription(), 60)).append("\n");
            }
            sb.append("\nUse: /motm select <choice>");
            sb.append("\nExample: /motm select 4");
        } else {
            sb.append("Selected: ").append(player.getSelectedPerks().size()).append("/")
                    .append(PerkManager.MAX_TOTAL_PERKS).append(" perks\n");
            int nextMilestone = ((player.getLevel() / 10) + 1) * 10;
            if (nextMilestone <= LevelingManager.PERK_CAP_LEVEL) {
                sb.append("Next perk choice unlocks at Lv. ").append(nextMilestone).append("\n\n");
            } else {
                sb.append("Perk unlock cap reached.\n\n");
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

    // --- /motm select <choice> ---

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

        if (args.length < 2) {
            return "[MOTM] Usage: /motm select <choice>";
        }

        List<Perk> available = pm.getAvailablePerks(player);
        SelectionResolution selectionResolution = resolvePerkSelections(available, List.of(args[1]));
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
            Perk perk = mod.getDataLoader().getPerkByIdAnyClass(id);
            if (perk != null) {
                sb.append("  + ").append(perk.getName()).append("\n");
            }
        }
        if (!preview.newSynergies.isEmpty()) {
            sb.append("\nNew synergies activated: ").append(preview.newSynergies.size());
        }
        mod.getPlayerDataManager().savePlayerData(player);
        mod.getPlayerDataManager().checkAchievements(player, "perks_selected", null);
        mod.rebuildPlayerRuntime(player);
        return sb.toString();
    }

    // --- /motm stats ---

    private String handleStats(PlayerData player, String[] args) {
        if (args.length >= 3 && "spend".equalsIgnoreCase(args[1])) {
            int points = args.length >= 4 ? Math.max(1, parseInteger(args[3]) != null ? parseInteger(args[3]) : 1) : 1;
            boolean spent = mod.getLevelingManager().spendStatPoint(player, args[2], points);
            if (!spent) {
                return "[MOTM] Could not spend stat points. Check the stat name and unspent point total.";
            }
            mod.getPlayerDataManager().savePlayerData(player);
            mod.refreshPlayerProgressionBonuses(player.getPlayerId());
            mod.refreshStatusHud(player.getPlayerId());
            return "[MOTM] Spent " + points + " point(s) in " + args[2].toLowerCase(java.util.Locale.ROOT)
                    + ". Unspent: " + player.getUnspentStatPoints();
        }

        StringBuilder sb = new StringBuilder("[MOTM] === Player Summary ===\n");
        sb.append("Name: ").append(player.getPlayerName()).append("\n");
        sb.append("Class: ").append(player.getPlayerClass() != null ? player.getPlayerClass() : "None").append("\n");
        sb.append("Style: ").append(formatSelectedStyleSummary(player)).append("\n");
        sb.append("Level: ").append(player.getLevel()).append(" / ").append(LevelingManager.MAX_LEVEL).append("\n");
        sb.append("Unspent Stat Points: ").append(player.getUnspentStatPoints()).append("\n");
        sb.append("Perks: ").append(player.getSelectedPerks().size()).append("/")
                .append(PerkManager.MAX_TOTAL_PERKS).append("\n");
        sb.append("Synergies: ").append(player.getActiveSynergyBonuses().size()).append(" active\n");
        sb.append("Achievements: ").append(player.getAchievements().size()).append("\n\n");

        PlayerData.StatAllocation allocation = player.getStatAllocation();
        LevelingManager lm = mod.getLevelingManager();
        sb.append("--- Stat Table ---\n");
        sb.append("Vigor: ").append(allocation.getVigor())
                .append(" | HP +").append(formatPercent(lm.getVigorHealthMultiplier(player) - 1.0))
                .append(" | DR +").append(formatPercent(lm.getVigorDamageReduction(player))).append("\n");
        sb.append("Tenacity: ").append(allocation.getTenacity())
                .append(" | Damage +").append(formatPercent(lm.getTenacityDamageMultiplier(player) - 1.0))
                .append(" | Crit damage +").append(formatPercent(lm.getTenacityCritDamageBonus(player))).append("\n");
        sb.append("Endurance: ").append(allocation.getEndurance())
                .append(" | Stamina +").append(formatPercent(lm.getEnduranceStaminaMultiplier(player) - 1.0))
                .append(" | Regen +").append(formatPercent(lm.getEnduranceStaminaRegenBonus(player))).append("\n");
        sb.append("Agility: ").append(allocation.getAgility())
                .append(" | Speed +").append(formatPercent(lm.getAgilitySpeedMultiplier(player) - 1.0))
                .append(" | Melee attack speed +").append(formatPercent(lm.getAgilityMeleeAttackSpeedBonus(player))).append("\n");
        sb.append("Luck: ").append(allocation.getLuck())
                .append(" | Crit chance +").append(formatPercent(lm.getLuckCritChanceBonus(player)))
                .append(" | XP +").append(formatPercent(lm.getLuckXpMultiplier(player) - 1.0)).append("\n");
        sb.append("Spend: /motm stats spend <vigor|tenacity|endurance|agility|luck> [points]\n\n");

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
        sb.append("Stat Growth: ").append(lm.describePlayerStatGrowth(player)).append("\n");
        sb.append("Hostile Mob Anchor: Lv ").append(hostileAnchorLevel)
                .append(" average level in this world").append("\n");
        sb.append("Hostile Scaling: ").append(difficulty).append("\n");
        sb.append("Boss Scaling: ").append(bossDifficulty).append("\n");

        if (player.getRestedBonus() > 0) {
            sb.append("Rested Bonus: +").append((int) (player.getRestedBonus() * 100)).append("%\n");
        }

        int nextMilestone = ((player.getLevel() / 10) + 1) * 10;
        if (nextMilestone <= LevelingManager.PERK_CAP_LEVEL) {
            int xpToMilestone = lm.getXpToLevel(player, nextMilestone);
            sb.append("XP to next perk choice (Lv. ").append(nextMilestone).append("): ")
                    .append(xpToMilestone).append("\n");
        }

        return sb.toString();
    }

    // --- /motm style [styleId] ---

    private String handleStyle(PlayerData player, String[] args, Player runtimePlayer) {
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
            resetRuntimeForLoadoutSwap(player, runtimePlayer);
            clearClassProgression(player);
            player.setPlayerClass(resolvedStyle.classId());
            player.setFirstJoin(false);
            updateDebugProgressionState(player);
        }

        boolean success = mod.getStyleManager().selectStyles(player, List.of(styleId));
        if (!success) {
            return "[MOTM] Invalid style selection. Use /motm style to see available styles.";
        }

        if (internalTestFlow) {
            mod.setFreeCastEnabled(player.getPlayerId(), false);
            boolean grantedImmediately = runtimePlayer != null && mod.ensureSpellbookItem(runtimePlayer);
            if (!grantedImmediately) {
                mod.queueSpellbookGrant(player.getPlayerId());
            }
        }

        StringBuilder sb = new StringBuilder(internalTestFlow
                ? "[MOTM] Testing loadout ready!\n"
                : "[MOTM] Style selected!\n");
        if (internalTestFlow) {
            sb.append("Class: ").append(resolvedStyle.classData().getDisplayName()).append("\n");
            if (autoClassSwap) {
                sb.append("Flow: class auto-set from style id.\n");
            }
            sb.append("Reset: class perks, style, casting state, and cooldowns cleared for a clean test swap.\n");
            sb.append("Test Protection: disabled for player-visible review; /motm dev test enables it only for harness runs.\n");
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
        mod.completeStartupSelection(player.getPlayerId());
        return sb.toString();
    }

    // --- /motm spellbook [section] ---

    private String handleSpellbook(PlayerData player, String[] args, Player runtimePlayer) {
        if (args.length >= 2 && "give".equalsIgnoreCase(args[1])) {
            if (runtimePlayer == null && mod.getRuntimePlayer(player.getPlayerId()) == null) {
                return "[MOTM] Join a world and run this in-game to receive the spellbook item.";
            }
            if (runtimePlayer != null && mod.ensureSpellbookItem(runtimePlayer)) {
                return "[MOTM] Spellbook delivered.";
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
                .append(" | Casting: cooldown-based\n\n");

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
            sb.append("  CD: ").append(formatDecimal(ability.getCooldownSeconds())).append("s")
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
        String response = castResolvedAbility(player, style, ability, runtimePlayer, targetRef, targetBlock, true);
        if (!response.isBlank()) {
            return response;
        }
        if (mod.isDevToolsEnabled()) {
            return "[MOTM] Spellbook slot " + slot + " queued: " + ability.getName() + ".";
        }
        return "";
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
                if ("dust_devil".equalsIgnoreCase(activated.getId())) {
                    String sandstormRuntime = mod.getGameplayPlaybackManager().deactivateAbilityRuntime(player, "sandstorm");
                    if (!sandstormRuntime.isBlank()) {
                        sb.append(" Sandstorm ended: ").append(sandstormRuntime).append(".");
                    }
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
        return devCommandRouter.route(player, args, runtimePlayer);
    }

    String handleDevBook(PlayerData player, Player runtimePlayer) {
        if (runtimePlayer == null && mod.getRuntimePlayer(player.getPlayerId()) == null) {
            return "[MOTM] Join a world and run this in-game to receive the Dev Spellbook.";
        }
        return mod.queueDevBookGrant(player.getPlayerId());
    }

    String handleDevObserve(PlayerData player, String[] args, Player runtimePlayer) {
        if (args.length < 3) {
            return "[MOTM] Usage: /motm dev observe <start|stop|status|scenario|marker|snapshot|spellbook> ...";
        }

        return switch (args[2].toLowerCase(java.util.Locale.ROOT)) {
            case "start" -> {
                String runId = args.length >= 4 ? args[3] : "";
                String scenarioId = args.length >= 5 ? args[4] : "manual";
                yield mod.startObservabilityRun(runId, scenarioId, player.getPlayerId());
            }
            case "stop" -> mod.stopObservabilityRun(args.length >= 4 ? args[3] : "manual");
            case "status" -> mod.getObservabilityStatus();
            case "scenario" -> {
                if (args.length < 4) {
                    yield "[MOTM] Usage: /motm dev observe scenario <scenarioId>";
                }
                yield mod.setObservabilityScenario(args[3]);
            }
            case "marker" -> mod.markObservabilityRun(
                    player.getPlayerId(),
                    args.length >= 4 ? args[3] : "marker"
            );
            case "snapshot", "state" -> mod.snapshotObservability(
                    player.getPlayerId(),
                    args.length >= 4 ? args[3] : "snapshot"
            );
            case "spellbook", "page", "ui" -> {
                Player resolvedPlayer = runtimePlayer != null ? runtimePlayer : mod.getRuntimePlayer(player.getPlayerId());
                if (resolvedPlayer == null) {
                    yield "[MOTM] Runtime player context is unavailable.";
                }
                SpellbookManager.Section section = args.length >= 4
                        ? mod.getSpellbookManager().parseSection(args[3])
                        : SpellbookManager.Section.OVERVIEW;
                if (section == null) {
                    yield "[MOTM] Unknown spellbook section. Sections: " + mod.getSpellbookManager().getSectionList();
                }
                boolean opened = mod.openSpellbook(resolvedPlayer, section);
                yield opened
                        ? "[MOTM] Observability custom spellbook page opened: section=" + section
                        : "[MOTM] Observability custom spellbook page was not opened.";
            }
            default -> "[MOTM] Usage: /motm dev observe <start|stop|status|scenario|marker|snapshot|spellbook> ...";
        };
    }

    String handleDevAudit(PlayerData player, String[] args) {
        String marker = args.length >= 4 && "marker".equalsIgnoreCase(args[2])
                ? args[3]
                : Long.toString(System.currentTimeMillis());
        LOG.info("[MOTM] Dev audit marker: playerId=" + player.getPlayerId()
                + " marker=" + marker);
        return "[MOTM] Dev audit marker: " + marker;
    }

    String handleDevTest(PlayerData player, String[] args, Player runtimePlayer) {
        if (args.length < 3) {
            return "[MOTM] Usage: /motm dev test <style <styleId>|ability <abilityId>|mobs|reset|status|stop>";
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
            case "ability" -> {
                if (args.length < 4) {
                    yield "[MOTM] Usage: /motm dev test ability <abilityId>";
                }
                yield mod.startSingleAbilityTest(player.getPlayerId(), args[3]);
            }
            case "mobs" -> {
                if (args.length >= 4 && "clear".equalsIgnoreCase(args[3])) {
                    yield mod.clearStyleTestMobs(player.getPlayerId());
                }
                if (args.length >= 4 && "count".equalsIgnoreCase(args[3])) {
                    yield mod.countStyleTestMobs(player.getPlayerId());
                }
                yield mod.spawnStyleTestMobs(player.getPlayerId(), args.length >= 4 ? args[3] : "standard");
            }
            case "reset", "arena-reset" -> mod.resetStyleReviewArena(player.getPlayerId());
            case "weapon-hit", "weapon", "attack" -> mod.runStyleTestWeaponHit(player.getPlayerId());
            case "stomp-land", "stomp-landing", "jump-land", "jump-landing" -> mod.forceStyleTestStompLanding(player.getPlayerId());
            case "status" -> mod.getStyleTestStatus(player.getPlayerId());
            case "stop" -> mod.stopStyleTest(player.getPlayerId());
            default -> "[MOTM] Usage: /motm dev test <style <styleId>|ability <abilityId>|mobs|reset|status|stop>";
        };
    }

    String handleDevProof(PlayerData player, String[] args, Player runtimePlayer) {
        if (args.length < 3) {
            return MotmProofCatalog.usage();
        }
        if (runtimePlayer == null && mod.getRuntimePlayer(player.getPlayerId()) == null) {
            return "[MOTM] Join a world and run this in-game to run a proof.";
        }
        return mod.queueDevProof(player.getPlayerId(), args[2]);
    }

    String handleDevPassive(PlayerData player, String[] args, Player runtimePlayer) {
        if (args.length < 3) {
            return getDevPassiveUsage();
        }

        Player resolvedPlayer = runtimePlayer != null ? runtimePlayer : mod.getRuntimePlayer(player.getPlayerId());
        if (resolvedPlayer == null) {
            return "[MOTM] Join a world and run this in-game to inspect passive runtime state.";
        }

        return switch (args[2].toLowerCase(java.util.Locale.ROOT)) {
            case "status" -> buildDevPassiveStatus(player, resolvedPlayer);
            case "health", "hp" -> {
                if (args.length < 4) {
                    yield "[MOTM] Usage: /motm dev passive health <value>";
                }
                Double value = parseDouble(args[3]);
                if (value == null) {
                    yield "[MOTM] Health value must be numeric.";
                }
                yield setRuntimePlayerHealth(player, resolvedPlayer, value.floatValue());
            }
            case "incoming-damage", "damage-in" -> {
                if (args.length < 4) {
                    yield "[MOTM] Usage: /motm dev passive incoming-damage <amount> [physical|fall]";
                }
                Double value = parseDouble(args[3]);
                if (value == null || value <= 0.0) {
                    yield "[MOTM] Incoming damage must be a positive number.";
                }
                yield applyDevIncomingDamage(player, resolvedPlayer, value.floatValue(), args.length >= 5 ? args[4] : "physical");
            }
            case "outgoing-damage", "damage-out" -> {
                if (args.length < 4) {
                    yield "[MOTM] Usage: /motm dev passive outgoing-damage <amount> [ability|weapon]";
                }
                Double value = parseDouble(args[3]);
                if (value == null || value <= 0.0) {
                    yield "[MOTM] Outgoing damage must be a positive number.";
                }
                boolean abilityBased = args.length < 5 || !"weapon".equalsIgnoreCase(args[4]);
                yield applyDevOutgoingDamage(player, resolvedPlayer, value, abilityBased);
            }
            case "combat", "damage-perks" -> {
                if (mod.getRuntimePerkManager() == null) {
                    yield "[MOTM] Runtime perk manager unavailable.";
                }
                double value = 100.0;
                if (args.length >= 4) {
                    Double parsed = parseDouble(args[3]);
                    if (parsed == null || parsed <= 0.0) {
                        yield "[MOTM] Combat proof damage must be a positive number.";
                    }
                    value = parsed;
                }
                yield mod.getRuntimePerkManager().runCombatPerkProof(player, resolvedPlayer, value);
            }
            case "low-health", "lowhp" -> {
                if (mod.getRuntimePerkManager() == null) {
                    yield "[MOTM] Runtime perk manager unavailable.";
                }
                yield mod.getRuntimePerkManager().runLowHealthProof(player, resolvedPlayer);
            }
            case "corruptus-stack", "stack" -> {
                mod.getClassPassiveManager().onMobKilled(
                        player,
                        resolvedPlayer,
                        "dev-passive-proof-" + System.currentTimeMillis()
                );
                yield "[MOTM] Dev passive corruptus stack queued.\n" + buildDevPassiveStatus(player, resolvedPlayer);
            }
            case "mob-kill", "kill" -> {
                mod.onMobKilled(
                        player.getPlayerId(),
                        "dev-perk-proof-" + System.currentTimeMillis(),
                        "dev-proof",
                        Math.max(1, player.getLevel()),
                        false
                );
                yield "[MOTM] Dev passive mob-kill queued.\n" + buildDevPassiveStatus(player, resolvedPlayer);
            }
            case "knockback", "kb" -> {
                double multiplier = mod.getClassPassiveManager().getIncomingKnockbackMultiplier(player.getPlayerId());
                if (mod.getRuntimePerkManager() != null) {
                    multiplier *= mod.getRuntimePerkManager().getIncomingKnockbackMultiplier(player);
                }
                String result = "[MOTM] Dev passive knockback multiplier="
                        + String.format(java.util.Locale.ROOT, "%.3f", multiplier);
                LOG.info(result);
                yield result;
            }
            case "mining", "mine" -> {
                String itemId = args.length >= 4 ? args[3] : "Iron_Pickaxe";
                double multiplier = mod.getClassPassiveManager().getMiningDamageMultiplier(player, itemId);
                if (mod.getRuntimePerkManager() != null) {
                    multiplier = mod.getRuntimePerkManager().modifyMiningMultiplier(player, multiplier);
                }
                String result = "[MOTM] Dev passive mining multiplier="
                        + String.format(java.util.Locale.ROOT, "%.3f", multiplier)
                        + " class=" + (player.getPlayerClass() == null ? "none" : player.getPlayerClass())
                        + " item=" + itemId;
                LOG.info(result);
                yield result;
            }
            case "mole-man", "moleman" -> {
                if (mod.getRuntimePerkManager() == null) {
                    yield "[MOTM] Runtime perk manager unavailable.";
                }
                double base = mod.getClassPassiveManager().getMiningDamageMultiplier(player,
                        args.length >= 4 ? args[3] : "Tool_Pickaxe_Iron");
                yield mod.getRuntimePerkManager().runMoleManMiningProof(player, base);
            }
            case "movement-perks", "movement" -> {
                if (mod.getRuntimePerkManager() == null) {
                    yield "[MOTM] Runtime perk manager unavailable.";
                }
                yield mod.getRuntimePerkManager().runMovementPerkProof(player, resolvedPlayer);
            }
            case "projectile-speed" -> {
                double baseSpeed = 1.0;
                if (args.length >= 4) {
                    Double parsed = parseDouble(args[3]);
                    if (parsed == null || parsed <= 0.0) {
                        yield "[MOTM] Projectile speed must be a positive number.";
                    }
                    baseSpeed = parsed;
                }
                double adjusted = mod.getRuntimePerkManager() != null
                        ? mod.getRuntimePerkManager().modifyProjectileSpeed(player, baseSpeed)
                        : baseSpeed;
                String result = "[MOTM] Dev passive projectile-speed: base="
                        + String.format(java.util.Locale.ROOT, "%.3f", baseSpeed)
                        + " adjusted="
                        + String.format(java.util.Locale.ROOT, "%.3f", adjusted);
                LOG.info(result);
                yield result;
            }
            case "rainy-day", "rain" -> {
                if (mod.getRuntimePerkManager() == null) {
                    yield "[MOTM] Runtime perk manager unavailable.";
                }
                yield mod.getRuntimePerkManager().runRainyDayProof(
                        player,
                        resolvedPlayer,
                        args.length >= 4 ? args[3] : "auto"
                );
            }
            case "terror" -> {
                if (mod.getRuntimePerkManager() == null) {
                    yield "[MOTM] Runtime perk manager unavailable.";
                }
                yield mod.getRuntimePerkManager().runTerrorProof(player, resolvedPlayer);
            }
            case "eco-friendly", "eco" -> {
                if (mod.getRuntimePerkManager() == null) {
                    yield "[MOTM] Runtime perk manager unavailable.";
                }
                yield mod.getRuntimePerkManager().runEcoFriendlyProof(player, resolvedPlayer);
            }
            case "crafting", "craft" -> {
                if (mod.getRuntimePerkManager() == null) {
                    yield "[MOTM] Runtime perk manager unavailable.";
                }
                yield mod.getRuntimePerkManager().runCraftingProof(player, resolvedPlayer);
            }
            case "velocity" -> {
                if (args.length < 5) {
                    yield "[MOTM] Usage: /motm dev passive velocity <x> <z>";
                }
                Double x = parseDouble(args[3]);
                Double z = parseDouble(args[4]);
                if (x == null || z == null) {
                    yield "[MOTM] Velocity x/z values must be numeric.";
                }
                yield setRuntimePlayerHorizontalVelocity(player, resolvedPlayer, x, z);
            }
            default -> getDevPassiveUsage();
        };
    }

    private String getDevPassiveUsage() {
        return "[MOTM] Usage: /motm dev passive <status|health|incoming-damage|outgoing-damage|combat|low-health|corruptus-stack|mob-kill|knockback|mining|mole-man|movement-perks|projectile-speed|rainy-day|terror|eco-friendly|crafting|velocity>\n"
                + "Examples:\n"
                + "  /motm dev passive status\n"
                + "  /motm dev passive health 50\n"
                + "  /motm dev passive incoming-damage 20 physical\n"
                + "  /motm dev passive outgoing-damage 100 ability\n"
                + "  /motm dev passive combat 100\n"
                + "  /motm dev passive low-health\n"
                + "  /motm dev passive corruptus-stack\n"
                + "  /motm dev passive mob-kill\n"
                + "  /motm dev passive knockback\n"
                + "  /motm dev passive mining Iron_Pickaxe\n"
                + "  /motm dev passive mole-man Tool_Pickaxe_Iron\n"
                + "  /motm dev passive movement-perks\n"
                + "  /motm dev passive projectile-speed 1.0\n"
                + "  /motm dev passive rainy-day auto\n"
                + "  /motm dev passive terror\n"
                + "  /motm dev passive eco-friendly\n"
                + "  /motm dev passive crafting\n"
                + "  /motm dev passive velocity 1.0 0.0";
    }

    private String buildDevPassiveStatus(PlayerData player, Player runtimePlayer) {
        StringBuilder sb = new StringBuilder("[MOTM] Dev passive status:");
        sb.append(" class=").append(player.getPlayerClass() == null ? "none" : player.getPlayerClass());
        sb.append(" summary=").append(mod.getClassPassiveManager().buildPassiveStateSummary(player));
        sb.append(" knockbackMultiplier=")
                .append(String.format(java.util.Locale.ROOT, "%.3f",
                        mod.getClassPassiveManager().getIncomingKnockbackMultiplier(player.getPlayerId())));
        sb.append(" terraStationaryTicks=")
                .append(mod.getClassPassiveManager().getTerraStationaryTicks(player.getPlayerId()));
        sb.append(" terraCaveVision=")
                .append(mod.getClassPassiveManager().isTerraCaveVisionActive(player.getPlayerId()));
        sb.append(" hydroBarrierHp=")
                .append(String.format(java.util.Locale.ROOT, "%.1f",
                        mod.getClassPassiveManager().getHydroAquaBarrierShieldHp(player.getPlayerId())));
        sb.append(" hydroSwimming=")
                .append(mod.getClassPassiveManager().isHydroSwimming(player.getPlayerId()));
        sb.append(" hydroUnderwater=")
                .append(mod.getClassPassiveManager().isHydroUnderwater(player.getPlayerId()));
        sb.append(" corruptusStacks=")
                .append(mod.getClassPassiveManager().getCorruptusDarkResurrectionStacks(player.getPlayerId()));
        sb.append(" corruptusLockoutSeconds=")
                .append(String.format(java.util.Locale.ROOT, "%.1f",
                        mod.getClassPassiveManager().getCorruptusPassiveLockoutSecondsRemaining(player.getPlayerId())));
        appendNativeMovementDiagnostics(sb, player, runtimePlayer);

        String result = sb.toString();
        LOG.info(result);
        return result;
    }

    private String setRuntimePlayerHealth(PlayerData player, Player runtimePlayer, float requestedHealth) {
        EntityStatMap statMap = resolveRuntimeStatMap(player, runtimePlayer);
        if (statMap == null) {
            return "[MOTM] Dev passive health failed: EntityStatMap unavailable.";
        }
        EntityStatValue health = statMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null || health.getMax() <= 0.0f) {
            return "[MOTM] Dev passive health failed: health stat unavailable.";
        }

        float before = health.get();
        float target = Math.max(1.0f, Math.min(requestedHealth, health.getMax()));
        statMap.setStatValue(DefaultEntityStatTypes.getHealth(), target);
        String result = "[MOTM] Dev passive health set: before="
                + String.format(java.util.Locale.ROOT, "%.1f", before)
                + " after="
                + String.format(java.util.Locale.ROOT, "%.1f", target)
                + " max="
                + String.format(java.util.Locale.ROOT, "%.1f", health.getMax());
        LOG.info(result);
        return result;
    }

    private String applyDevIncomingDamage(PlayerData player, Player runtimePlayer, float incomingDamage, String rawCause) {
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null ? playerRef.getStore() : null;
        EntityStatMap statMap = resolveRuntimeStatMap(player, runtimePlayer);
        if (playerRef == null || !playerRef.isValid() || store == null || statMap == null) {
            return "[MOTM] Dev passive incoming-damage failed: runtime player store unavailable.";
        }
        EntityStatValue health = statMap.get(DefaultEntityStatTypes.getHealth());
        if (health == null || health.getMax() <= 0.0f) {
            return "[MOTM] Dev passive incoming-damage failed: health stat unavailable.";
        }

        float healthBefore = health.get();
        float afterPassives = mod.getClassPassiveManager().handleIncomingPlayerDamage(
                player.getPlayerId(),
                playerRef,
                store,
                incomingDamage
        );
        com.hypixel.hytale.server.core.modules.entity.damage.DamageCause cause =
                "fall".equalsIgnoreCase(rawCause)
                        ? com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.FALL
                        : com.hypixel.hytale.server.core.modules.entity.damage.DamageCause.PHYSICAL;
        if (mod.getRuntimePerkManager() != null) {
            com.hypixel.hytale.server.core.modules.entity.damage.Damage damage =
                    new com.hypixel.hytale.server.core.modules.entity.damage.Damage(
                            com.hypixel.hytale.server.core.modules.entity.damage.Damage.NULL_SOURCE,
                            cause,
                            afterPassives
                    );
            afterPassives = mod.getRuntimePerkManager().modifyIncomingDamage(
                    player,
                    playerRef,
                    store,
                    damage,
                    afterPassives
            );
        }
        EntityStatValue postPassiveHealth = statMap.get(DefaultEntityStatTypes.getHealth());
        float healthAfterPassive = postPassiveHealth != null ? postPassiveHealth.get() : healthBefore;
        if (afterPassives > 0.0f && postPassiveHealth != null && healthAfterPassive > 0.0f) {
            statMap.addStatValue(DefaultEntityStatTypes.getHealth(), -afterPassives);
        }
        EntityStatValue finalHealth = statMap.get(DefaultEntityStatTypes.getHealth());
        String result = "[MOTM] Dev passive incoming-damage: requested="
                + String.format(java.util.Locale.ROOT, "%.1f", incomingDamage)
                + " cause=" + cause.getId()
                + " afterPassives="
                + String.format(java.util.Locale.ROOT, "%.1f", afterPassives)
                + " healthBefore="
                + String.format(java.util.Locale.ROOT, "%.1f", healthBefore)
                + " healthAfterPassive="
                + String.format(java.util.Locale.ROOT, "%.1f", healthAfterPassive)
                + " healthFinal="
                + String.format(java.util.Locale.ROOT, "%.1f", finalHealth != null ? finalHealth.get() : -1.0f);
        LOG.info(result);
        return result;
    }

    private String applyDevOutgoingDamage(PlayerData player,
                                          Player runtimePlayer,
                                          double outgoingDamage,
                                          boolean abilityBased) {
        EntityStatMap statMap = resolveRuntimeStatMap(player, runtimePlayer);
        if (statMap == null) {
            return "[MOTM] Dev passive outgoing-damage failed: EntityStatMap unavailable.";
        }
        EntityStatValue healthBefore = statMap.get(DefaultEntityStatTypes.getHealth());
        float before = healthBefore != null ? healthBefore.get() : -1.0f;
        double adjustedDamage = outgoingDamage;
        if (mod.getRuntimePerkManager() != null) {
            adjustedDamage = mod.getRuntimePerkManager().modifyMotmAbilityDamage(player, outgoingDamage);
            mod.getRuntimePerkManager().afterSuccessfulHit(
                    player,
                    runtimePlayer.getReference(),
                    runtimePlayer.getReference() != null ? runtimePlayer.getReference().getStore() : null,
                    null,
                    adjustedDamage
            );
        }
        mod.getClassPassiveManager().onDamageDealt(
                player,
                runtimePlayer.getReference(),
                "dev-passive-proof-target",
                adjustedDamage,
                abilityBased
        );
        EntityStatValue healthAfter = statMap.get(DefaultEntityStatTypes.getHealth());
        float after = healthAfter != null ? healthAfter.get() : -1.0f;
        String result = "[MOTM] Dev passive outgoing-damage: amount="
                + String.format(java.util.Locale.ROOT, "%.1f", outgoingDamage)
                + " adjusted="
                + String.format(java.util.Locale.ROOT, "%.1f", adjustedDamage)
                + " abilityBased=" + abilityBased
                + " healthBefore="
                + String.format(java.util.Locale.ROOT, "%.1f", before)
                + " healthAfter="
                + String.format(java.util.Locale.ROOT, "%.1f", after);
        LOG.info(result);
        return result;
    }

    private String setRuntimePlayerHorizontalVelocity(PlayerData player,
                                                      Player runtimePlayer,
                                                      double x,
                                                      double z) {
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        Store<EntityStore> store = playerRef != null ? playerRef.getStore() : null;
        if (playerRef == null || !playerRef.isValid() || store == null) {
            return "[MOTM] Dev passive velocity failed: runtime player store unavailable.";
        }

        Velocity velocity = store.getComponent(playerRef, Velocity.getComponentType());
        if (velocity == null) {
            return "[MOTM] Dev passive velocity failed: Velocity component unavailable.";
        }

        Vector3d before = velocity.getVelocity();
        double y = before != null && before.isFinite() ? before.y : 0.0;
        velocity.set(x, y, z);
        String result = "[MOTM] Dev passive velocity set: before="
                + (before == null ? "null" : formatCompactTriple(before.x, before.y, before.z))
                + " after="
                + formatCompactTriple(x, y, z)
                + " class=" + (player.getPlayerClass() == null ? "none" : player.getPlayerClass());
        LOG.info(result);
        return result;
    }

    private EntityStatMap resolveRuntimeStatMap(PlayerData player, Player runtimePlayer) {
        Player resolvedPlayer = runtimePlayer != null ? runtimePlayer : mod.getRuntimePlayer(player.getPlayerId());
        if (resolvedPlayer == null) {
            return null;
        }
        Ref<EntityStore> playerRef = resolvedPlayer.getReference();
        Store<EntityStore> store = playerRef != null ? playerRef.getStore() : null;
        if (playerRef == null || !playerRef.isValid() || store == null) {
            return null;
        }
        return store.getComponent(playerRef, EntityStatMap.getComponentType());
    }

    String handleDevPosition(PlayerData player) {
        return mod.describeRuntimePlayerPosition(player.getPlayerId());
    }

    String handleDevEntities(PlayerData player, String[] args, Player runtimePlayer) {
        Player resolvedPlayer = runtimePlayer != null ? runtimePlayer : mod.getRuntimePlayer(player.getPlayerId());
        if (resolvedPlayer == null || resolvedPlayer.getReference() == null || !resolvedPlayer.getReference().isValid()) {
            return "[MOTM] Dev entities failed: runtime player unavailable.";
        }
        Store<EntityStore> store = resolvedPlayer.getReference().getStore();
        TransformComponent playerTransform = store != null
                ? store.getComponent(resolvedPlayer.getReference(), TransformComponent.getComponentType())
                : null;
        if (store == null || playerTransform == null || playerTransform.getTransform() == null) {
            return "[MOTM] Dev entities failed: player store/position unavailable.";
        }

        double radius = 64.0;
        if (args.length >= 3) {
            Double parsed = parseDouble(args[2]);
            if (parsed == null || parsed <= 0.0) {
                return "[MOTM] Usage: /motm dev entities [radius]";
            }
            radius = Math.min(256.0, parsed);
        }

        Vector3d playerPosition = playerTransform.getTransform().getPosition();
        final double scanRadius = radius;
        List<String> rows = new java.util.ArrayList<>();
        store.forEachChunk((chunk, commandBuffer) -> {
            for (int entityIndex = 0; entityIndex < chunk.size(); entityIndex++) {
                NPCEntity npc = chunk.getComponent(entityIndex, NPCEntity.getComponentType());
                if (npc == null || npc.isDespawning()) {
                    continue;
                }
                Ref<EntityStore> ref = chunk.getReferenceTo(entityIndex);
                TransformComponent transform = store.getComponent(ref, TransformComponent.getComponentType());
                Vector3d position = transform != null && transform.getTransform() != null
                        ? transform.getTransform().getPosition()
                        : null;
                if (position == null) {
                    continue;
                }
                double distance = playerPosition.distance(position);
                if (distance > scanRadius) {
                    continue;
                }
                ModelComponent model = chunk.getComponent(entityIndex, ModelComponent.getComponentType());
                String modelAssetId = model != null && model.getModel() != null
                        ? model.getModel().getModelAssetId()
                        : "none";
                String modelId = model != null && model.getModel() != null
                        ? model.getModel().getModel()
                        : "none";
                rows.add(String.format(java.util.Locale.ROOT,
                        "%.1fm role=%s type=%s modelAsset=%s model=%s pos=%s",
                        distance,
                        valueOrNone(npc.getRoleName()),
                        valueOrNone(npc.getNPCTypeId()),
                        valueOrNone(modelAssetId),
                        valueOrNone(modelId),
                        formatCompactTriple(position.x, position.y, position.z)));
            }
        });

        rows.sort(String::compareTo);
        int total = rows.size();
        int limit = Math.min(total, 12);
        String result = "[MOTM] Dev entities nearby: radius="
                + String.format(java.util.Locale.ROOT, "%.1f", radius)
                + " count=" + total;
        for (int i = 0; i < limit; i++) {
            result += "\n  " + rows.get(i);
        }
        if (total > limit) {
            result += "\n  ... " + (total - limit) + " more";
        }
        LOG.info(result.replace('\n', ' '));
        return result;
    }

    String handleDevRelocate(PlayerData player, String[] args) {
        String target = args.length >= 3 ? args[2] : "up";
        return mod.queueRuntimePlayerRelocationForTesting(player.getPlayerId(), target);
    }

    String handleDevMode(PlayerData player, String[] args) {
        if (args.length < 3) {
            return "[MOTM] Usage: /motm dev mode <creative|adventure>. "
                    + "Use adventure for survival-like Terra mining/damage/durability review.";
        }
        return mod.queueGameModeForTesting(player.getPlayerId(), args[2]);
    }

    String handleDevKit(PlayerData player, String[] args) {
        if (args.length < 3) {
            return "[MOTM] Usage: /motm dev kit <terra>";
        }
        String kit = args[2].toLowerCase(java.util.Locale.ROOT);
        if ("terra".equals(kit) || "terra-review".equals(kit)) {
            return mod.queueTerraReviewKitGrant(player.getPlayerId());
        }
        return "[MOTM] Unknown dev kit: " + args[2] + ". Available kits: terra.";
    }

    String handleDevInventory(PlayerData player, String[] args) {
        if (args.length < 3) {
            return "[MOTM] Usage: /motm dev inventory clean terra-kit\n"
                    + "Keeps only the MOTM spellbook, iron pickaxe, and iron sword.";
        }

        String action = args[2].toLowerCase(java.util.Locale.ROOT);
        String preset = args.length >= 4 ? args[3].toLowerCase(java.util.Locale.ROOT) : "terra-kit";
        if (!"clean".equals(action) || (!"terra-kit".equals(preset) && !"terra".equals(preset))) {
            return "[MOTM] Usage: /motm dev inventory clean terra-kit";
        }

        return mod.queueTerraReviewInventoryClean(player.getPlayerId());
    }

    String handleDevFreeCast(PlayerData player, String[] args) {
        if (args.length < 3) {
            return "[MOTM] Usage: /motm dev freecast <on|off>\n"
                    + "Ability resource costs are globally disabled; this toggle now only keeps legacy test protections active.\n"
                    + "Current: " + (mod.isFreeCastEnabled(player.getPlayerId()) ? "ON" : "OFF");
        }

        return switch (args[2].toLowerCase()) {
            case "on", "enable", "enabled", "true" -> {
                mod.setFreeCastEnabled(player.getPlayerId(), true);
                mod.refreshStatusHud(player.getPlayerId());
                yield "[MOTM] Dev: test protection enabled. Ability resource costs are already globally disabled.";
            }
            case "off", "disable", "disabled", "false" -> {
                mod.setFreeCastEnabled(player.getPlayerId(), false);
                mod.refreshStatusHud(player.getPlayerId());
                yield "[MOTM] Dev: test protection disabled. Ability resource costs remain globally disabled.";
            }
            default -> "[MOTM] Usage: /motm dev freecast <on|off>";
        };
    }

    String handleDevEffects(PlayerData player, String[] args, Player runtimePlayer) {
        if (args.length >= 3 && "clear".equalsIgnoreCase(args[2])) {
            mod.setFreeCastEnabled(player.getPlayerId(), false);
            mod.getStatusEffectManager().clearEffects(player.getPlayerId());
            String runtimeReset = resetRuntimeForLoadoutSwap(player, runtimePlayer);
            mod.getClassPassiveManager().suppressHydroAquaBarrierForDevCleanup(player.getPlayerId(), runtimePlayer);
            String nativeResult = mod.queueRuntimeEntityEffectsClearForDev(player.getPlayerId());
            mod.refreshStatusHud(player.getPlayerId());
            String result = "[MOTM] Dev effects cleared: test protection disabled, statuses cleared, runtime="
                    + runtimeReset + ", visuals=" + nativeResult + ".";
            LOG.info(result);
            return result;
        }

        List<StatusEffect> effects = mod.getStatusEffectManager().getEffects(player.getPlayerId());
        StringBuilder sb = new StringBuilder("[MOTM] Dev effects: ");
        sb.append("count=").append(effects.size());
        sb.append(" slowMultiplier=")
                .append(String.format(java.util.Locale.ROOT, "%.3f",
                        mod.getStatusEffectManager().getSlowMultiplier(player.getPlayerId())));
        sb.append(" speedBonus=")
                .append(String.format(java.util.Locale.ROOT, "%.3f",
                        mod.getStatusEffectManager().getSpeedBonus(player.getPlayerId())));
        sb.append(" immobilized=")
                .append(mod.getStatusEffectManager().isImmobilized(player.getPlayerId()));
        sb.append(" terraStationaryTicks=")
                .append(mod.getClassPassiveManager().getTerraStationaryTicks(player.getPlayerId()));
        appendNativeMovementDiagnostics(sb, player, runtimePlayer);

        if (effects.isEmpty()) {
            sb.append(" effects=[]");
        } else {
            sb.append(" effects=[");
            for (int i = 0; i < effects.size(); i++) {
                StatusEffect effect = effects.get(i);
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(effect.getType())
                        .append(":")
                        .append(effect.getRemainingTicks())
                        .append("t")
                        .append(":source=")
                        .append(effect.getSourcePerkOrAbility());
            }
            sb.append("]");
        }
        sb.append(" | use /motm dev effects clear to remove native model VFX during testing");

        String result = sb.toString();
        LOG.info(result);
        return result;
    }

    private void appendNativeMovementDiagnostics(StringBuilder sb, PlayerData player, Player runtimePlayer) {
        Player resolvedPlayer = runtimePlayer != null ? runtimePlayer : mod.getRuntimePlayer(player.getPlayerId());
        if (resolvedPlayer == null) {
            sb.append(" native=no-runtime-player");
            return;
        }

        Ref<EntityStore> playerRef = resolvedPlayer.getReference();
        Store<EntityStore> store = playerRef != null ? playerRef.getStore() : null;
        if (playerRef == null || !playerRef.isValid() || store == null) {
            sb.append(" native=no-player-ref");
            return;
        }

        TransformComponent transform = store.getComponent(playerRef, TransformComponent.getComponentType());
        Velocity velocity = store.getComponent(playerRef, Velocity.getComponentType());
        MovementStatesComponent movementStatesComponent = store.getComponent(playerRef, MovementStatesComponent.getComponentType());
        MovementManager movementManager = store.getComponent(playerRef, MovementManager.getComponentType());
        EntityStatMap statMap = store.getComponent(playerRef, EntityStatMap.getComponentType());

        if (transform != null && transform.getPosition() != null) {
            sb.append(" nativePos=").append(formatCompactTriple(
                    transform.getPosition().x,
                    transform.getPosition().y,
                    transform.getPosition().z));
        }
        if (velocity != null && velocity.getVelocity() != null) {
            sb.append(" nativeVel=").append(formatCompactTriple(
                    velocity.getVelocity().x,
                    velocity.getVelocity().y,
                    velocity.getVelocity().z));
        }
        if (movementStatesComponent != null && movementStatesComponent.getMovementStates() != null) {
            sb.append(" movementStates=").append(formatMovementStates(movementStatesComponent.getMovementStates()));
        }
        if (movementManager != null && movementManager.getSettings() != null) {
            sb.append(" movementSettings=").append(formatMovementSettings(movementManager.getSettings()));
        }
        if (statMap != null) {
            appendStat(sb, "health", statMap, DefaultEntityStatTypes.getHealth());
            appendStat(sb, "stamina", statMap, DefaultEntityStatTypes.getStamina());
            appendStat(sb, "mana", statMap, DefaultEntityStatTypes.getMana());
            appendStat(sb, "signature", statMap, DefaultEntityStatTypes.getSignatureEnergy());
            appendStat(sb, "oxygen", statMap, DefaultEntityStatTypes.getOxygen());
        }
    }

    private String formatMovementStates(MovementStates states) {
        List<String> active = new java.util.ArrayList<>();
        if (states.idle) active.add("idle");
        if (states.horizontalIdle) active.add("horizontalIdle");
        if (states.jumping) active.add("jumping");
        if (states.flying) active.add("flying");
        if (states.walking) active.add("walking");
        if (states.running) active.add("running");
        if (states.sprinting) active.add("sprinting");
        if (states.crouching) active.add("crouching");
        if (states.forcedCrouching) active.add("forcedCrouching");
        if (states.falling) active.add("falling");
        if (states.fallingFar) active.add("fallingFar");
        if (states.climbing) active.add("climbing");
        if (states.inFluid) active.add("inFluid");
        if (states.swimming) active.add("swimming");
        if (states.swimJumping) active.add("swimJumping");
        if (states.onGround) active.add("onGround");
        if (states.mantling) active.add("mantling");
        if (states.sliding) active.add("sliding");
        if (states.mounting) active.add("mounting");
        if (states.rolling) active.add("rolling");
        if (states.sitting) active.add("sitting");
        if (states.gliding) active.add("gliding");
        if (states.sleeping) active.add("sleeping");
        return active.isEmpty() ? "[]" : "[" + String.join(",", active) + "]";
    }

    private String formatMovementSettings(MovementSettings settings) {
        return "{base="
                + formatOneDecimal(settings.baseSpeed)
                + ",fwWalk=" + formatOneDecimal(settings.forwardWalkSpeedMultiplier)
                + ",strafeWalk=" + formatOneDecimal(settings.strafeWalkSpeedMultiplier)
                + ",fwRun=" + formatOneDecimal(settings.forwardRunSpeedMultiplier)
                + ",strafeRun=" + formatOneDecimal(settings.strafeRunSpeedMultiplier)
                + ",fwCrouch=" + formatOneDecimal(settings.forwardCrouchSpeedMultiplier)
                + ",strafeCrouch=" + formatOneDecimal(settings.strafeCrouchSpeedMultiplier)
                + ",sprint=" + formatOneDecimal(settings.forwardSprintSpeedMultiplier)
                + ",minMult=" + formatOneDecimal(settings.minSpeedMultiplier)
                + ",maxMult=" + formatOneDecimal(settings.maxSpeedMultiplier)
                + ",accel=" + formatOneDecimal(settings.acceleration)
                + ",canFly=" + settings.canFly
                + "}";
    }

    private void appendStat(StringBuilder sb, String label, EntityStatMap statMap, int statType) {
        EntityStatValue value = statMap.get(statType);
        if (value == null) {
            return;
        }
        sb.append(' ')
                .append(label)
                .append('=')
                .append(String.format(java.util.Locale.ROOT, "%.1f/%.1f", value.get(), value.getMax()));
    }

    private String formatCompactTriple(double x, double y, double z) {
        return "("
                + String.format(java.util.Locale.ROOT, "%.2f", x)
                + ","
                + String.format(java.util.Locale.ROOT, "%.2f", y)
                + ","
                + String.format(java.util.Locale.ROOT, "%.2f", z)
                + ")";
    }

    private String valueOrNone(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }

    private String formatOneDecimal(float value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private String formatPercent(double value) {
        return String.format(java.util.Locale.ROOT, "%.2f%%", Math.max(0.0, value) * 100.0);
    }


    String handleDevLevel(PlayerData player, String[] args) {
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

        if (newLevel < 0) {
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

    String handleDevXp(PlayerData player, String[] args) {
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

    String handleDevClass(PlayerData player, String[] args) {
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

    String handleDevPerks(PlayerData player, String[] args) {
        if (args.length < 3) {
            return "[MOTM] Usage: /motm dev perks <clear|set|grant> [perkId|all]...";
        }

        String action = args[2].toLowerCase(java.util.Locale.ROOT);
        switch (action) {
            case "clear" -> {
                clearPerkProgression(player);
                updateDebugProgressionState(player);
                rebuildPlayerRuntime(player);
                mod.getPlayerDataManager().savePlayerData(player);
                return "[MOTM] Dev: perks and perk history cleared.";
            }
            case "set", "grant" -> {
                if (args.length < 4) {
                    return "[MOTM] Usage: /motm dev perks " + action + " <perkId|all>...";
                }
                if ("set".equals(action)) {
                    clearPerkProgression(player);
                }
                java.util.LinkedHashSet<String> selected = new java.util.LinkedHashSet<>(player.getSelectedPerks());
                for (int i = 3; i < args.length; i++) {
                    String requested = args[i];
                    if ("all".equalsIgnoreCase(requested)) {
                        mod.getDataLoader().getSharedPerkPool().stream()
                                .map(Perk::getId)
                                .forEach(selected::add);
                        continue;
                    }
                    Perk perk = mod.getDataLoader().getPerkByIdAnyClass(requested);
                    if (perk == null) {
                        return "[MOTM] Invalid perk id for dev grant: " + requested;
                    }
                    selected.add(perk.getId());
                }
                player.setSelectedPerks(new java.util.ArrayList<>(selected));
                player.setPendingPerkTier(null);
                player.setPerkSelectionPoints(Math.max(0, player.getPerkSelectionPoints()));
                updateDebugProgressionState(player);
                rebuildPlayerRuntime(player);
                mod.getPlayerDataManager().savePlayerData(player);
                String result = "[MOTM] Dev: perks " + action + " -> " + String.join(", ", player.getSelectedPerks());
                LOG.info(result);
                return result;
            }
            default -> {
                return "[MOTM] Usage: /motm dev perks <clear|set|grant> [perkId|all]...";
            }
        }
    }

    String handleDevStyles(PlayerData player, String[] args) {
        if (args.length < 3 || !"clear".equalsIgnoreCase(args[2])) {
            return "[MOTM] Usage: /motm dev styles clear";
        }

        player.getSelectedStyles().clear();
        resetRuntimeForLoadoutSwap(player, mod.getRuntimePlayer(player.getPlayerId()));
        mod.getPlayerDataManager().savePlayerData(player);
        rebuildPlayerRuntime(player);
        return "[MOTM] Dev: styles cleared.";
    }

    String handleDevClear(PlayerData player, String[] args) {
        if (args.length >= 3 && !"player".equalsIgnoreCase(args[2]) && !"all".equalsIgnoreCase(args[2])) {
            return "[MOTM] Usage: /motm dev clear\n"
                    + "Optional: /motm dev clear player";
        }

        return performFullDevPlayerClear(player);
    }

    String handleDevReset(PlayerData player, String[] args) {
        if (args.length >= 3 && !"player".equalsIgnoreCase(args[2]) && !"all".equalsIgnoreCase(args[2])) {
            return "[MOTM] Usage: /motm dev reset\n"
                    + "Optional: /motm dev reset player";
        }

        return performFullDevPlayerClear(player);
    }

    // --- /motm casting ---

    private String handleCastingModel(PlayerData player) {
        if (player.getPlayerClass() == null) {
            return "[MOTM] Select a class first with /motm class <classId>";
        }
        StringBuilder sb = new StringBuilder("[MOTM] Casting Model: active abilities use cooldowns, durations, charges, positioning, and action timing.");
        if (mod.isFreeCastEnabled(player.getPlayerId())) {
            sb.append("\nDev Free-Cast: ON");
        }
        return sb.toString();
    }

    // --- /motm info ---

    private String handleInfo() {
        return "[MOTM] === Mentees of the Mystical ===\n"
                + "Version: 1.0.1\n"
                + "4 Classes | 40 Styles | 20 shared perk choices | Level 0-200\n"
                + "Elemental Reactions | Stat Scaling | Synergy System\n\n"
                + "Flow:\n"
                + "  1. /motm class <id>\n"
                + "  2. /motm style <id>\n"
                + "  3. /motm spellbook overview\n"
                + "  4. Equip the spellbook and use Hytale Ability 1 / 2 / 3\n"
                + "  5. /motm abilities and /motm cast <abilityId>\n"
                + "  6. /motm perks at Lv. 10+\n\n"
                + "Commands:\n"
                + "  /motm class [id]        - View/select class\n"
                + "  /motm style [id]        - View/select combat style"
                + (mod.isDevToolsEnabled() ? " (auto-loads matching class in test builds)\n" : "\n")
                + "  /motm spellbook [page]  - Open the spellbook page in chat\n"
                + "  /motm spellbook give    - Spawn the normal spellbook\n"
                + "  /motm controls          - View ability input bindings\n"
                + "  /motm abilities         - View ability IDs and cooldowns\n"
                + "  /motm cast <abilityId>  - Test-cast a style ability\n"
                + "  /motm audit             - Run the preflight data/runtime audit\n"
                + "  /motm perks             - View perk choices (not styles)\n"
                + "  /motm select <choice>   - Select 1 perk by number\n"
                + "  /motm casting           - View the no-resource casting model\n"
                + "  /motm stats             - View/spend stat points\n"
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

    String getDevHelpMessage() {
        String denied = MotmCommandAuth.deniedMessage(mod.isDevToolsEnabled(), mod.devToolsDisabledMessage());
        if (denied != null) {
            return denied;
        }
        return "[MOTM] === Dev Commands ===\n"
                + "  /motm dev book\n"
                + "  /motm dev observe start <runId> [scenarioId]\n"
                + "  /motm dev observe marker <label>\n"
                + "  /motm dev observe snapshot <label>\n"
                + "  /motm dev observe stop [reason]\n"
                + "  /motm dev test style <styleId>\n"
                + "  /motm dev test ability <abilityId>\n"
                + "  /motm dev test mobs [close|stationary|cluster|line|surround|clear|count]\n"
                + "  /motm dev test reset\n"
                + "  /motm dev test status\n"
                + "  /motm dev test stop\n"
                + "  /motm dev proof <proofId> (" + String.join(", ", MotmProofCatalog.ids()) + ")\n"
                + "  /motm dev passive <status|health|incoming-damage|outgoing-damage|corruptus-stack|knockback>\n"
                + "  /motm dev entities [radius]\n"
                + "  /motm dev position\n"
                + "  /motm dev relocate <up|flatlands|lane|cave>\n"
                + "  /motm dev mode <creative|adventure>\n"
                + "  /motm dev kit terra\n"
                + "  /motm dev inventory clean terra-kit\n"
                + "  /motm dev daylight\n"
                + "  /motm dev freecast <on|off>\n"
                + "  /motm dev effects\n"
                + "  /motm dev clear\n"
                + "  /motm dev level set <n>\n"
                + "  /motm dev level add <n>\n"
                + "  /motm dev xp set <n>\n"
                + "  /motm dev xp add <n>\n"
                + "  /motm dev class set <id>\n"
                + "  /motm dev class clear\n"
                + "  /motm dev perks clear\n"
                + "  /motm dev styles clear\n"
                + "  /motm dev reset";
    }

    private String buildDevHelpSummary() {
        if (!mod.isDevToolsEnabled()) {
            return "  Dev tools              - Disabled in this build/server\n";
        }
        return "  /motm dev ...           - Testing/admin tools\n"
                + "  /motm dev book          - Spawn the Dev Spellbook\n"
                + "  /motm dev test ...      - Run a live style test sequence\n";
    }

    private void rebuildPlayerRuntime(PlayerData player) {
        mod.rebuildPlayerRuntime(player);
    }

    private String resetRuntimeForLoadoutSwap(PlayerData player, Player runtimePlayer) {
        if (player == null || player.getPlayerId() == null || runtimePlayer == null
                || mod.getGameplayPlaybackManager() == null) {
            return "";
        }

        Ref<EntityStore> runtimeRef = runtimePlayer.getReference();
        Store<EntityStore> runtimeStore = runtimeRef != null && runtimeRef.isValid()
                ? runtimeRef.getStore()
                : null;
        String playbackReset = mod.getGameplayPlaybackManager().resetReviewRuntime(
                player.getPlayerId(),
                runtimeStore,
                runtimePlayer
        );
        String nativeReset = mod.clearRuntimeEntityEffectsForDev(player.getPlayerId(), runtimeStore);
        LOG.info("[MOTM] Loadout swap runtime reset: playerId=" + player.getPlayerId()
                + " playback=" + playbackReset
                + " nativeEffects=" + nativeReset);
        return playbackReset;
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
        mod.getLevelingManager().reconcileProgressionState(player);
    }

    private void resetPlayerForDev(PlayerData player) {
        player.setPlayerClass(null);
        player.setLevel(0);
        player.setCurrentXp(0);
        player.setTotalXpEarned(0);
        player.setFirstJoin(true);
        player.setStartupSelectionComplete(false);
        player.setPendingStartupClass(null);
        player.setStatAllocation(new PlayerData.StatAllocation());
        player.setUnspentStatPoints(0);
        player.setTotalStatPointsEarned(0);
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
        mod.getStyleManager().resetCooldowns(player.getPlayerId());
        mod.getStatusEffectManager().clearEffects(player.getPlayerId());
        mod.getElementalReactionManager().clearMarks(player.getPlayerId());
    }

    private String performFullDevPlayerClear(PlayerData player) {
        String runtimeReset = resetRuntimeForLoadoutSwap(player, mod.getRuntimePlayer(player.getPlayerId()));
        resetPlayerForDev(player);
        rebuildPlayerRuntime(player);
        mod.getPlayerDataManager().savePlayerData(player);
        mod.refreshStatusHud(player.getPlayerId());
        LOG.info("[MOTM] Dev clear runtime cleanup: playerId=" + player.getPlayerId()
                + " playback=" + runtimeReset);
        return "[MOTM] Dev: player cleared to a fresh state.\n"
                + "Reset: class, style, perks, level, XP, casting state, cooldowns, statuses, marks, and runtime visuals.";
    }

    private Integer parseInteger(String rawValue) {
        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double parseDouble(String rawValue) {
        try {
            return Double.parseDouble(rawValue);
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
        return Math.max(0, Math.min(LevelingManager.MAX_LEVEL, level));
    }

    private record SelectionResolution(List<String> resolvedIds, List<String> invalidSelections) {}

    private record ResolvedStyleSelection(String classId, ClassData classData, StyleData style) {}
}
