package com.motm.manager;

import com.motm.model.AbilityData;
import com.motm.model.ClassData;
import com.motm.model.Perk;
import com.motm.model.PlayerData;
import com.motm.model.StyleData;
import com.motm.util.AbilityPresentation;
import com.motm.util.DataLoader;
import com.motm.util.PassivePresentation;

import java.util.List;
import java.util.Locale;

/**
 * Centralizes the spellbook information architecture for class, style, ability,
 * level scaling, and perk information.
 */
public class SpellbookManager {

    public enum Section {
        OVERVIEW,
        CLASS,
        STYLE,
        ABILITIES,
        PERKS,
        PROGRESSION
    }

    private final DataLoader dataLoader;
    private final LevelingManager levelingManager;
    private final StyleManager styleManager;
    private final PerkManager perkManager;
    private final ClassPassiveManager classPassiveManager;

    public SpellbookManager(DataLoader dataLoader,
                            LevelingManager levelingManager,
                            StyleManager styleManager,
                            PerkManager perkManager,
                            ClassPassiveManager classPassiveManager) {
        this.dataLoader = dataLoader;
        this.levelingManager = levelingManager;
        this.styleManager = styleManager;
        this.perkManager = perkManager;
        this.classPassiveManager = classPassiveManager;
    }

    public String render(PlayerData player, Section section) {
        return switch (section) {
            case OVERVIEW -> renderOverview(player);
            case CLASS -> renderClass(player);
            case STYLE -> renderStyle(player);
            case ABILITIES -> renderAbilities(player);
            case PERKS -> renderPerks(player);
            case PROGRESSION -> renderProgression(player);
        };
    }

    public Section parseSection(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Section.OVERVIEW;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "overview", "home", "hub" -> Section.OVERVIEW;
            case "class", "identity", "path" -> Section.CLASS;
            case "style", "styles" -> Section.STYLE;
            case "abilities", "ability", "spells", "combat" -> Section.ABILITIES;
            case "perks", "perk", "web" -> Section.PERKS;
            case "stats", "stat", "progression", "progress", "level", "scaling" -> Section.PROGRESSION;
            default -> null;
        };
    }

    public String getSectionList() {
        return "overview, class, style, abilities, perks, stats";
    }

    private String renderOverview(PlayerData player) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM Spellbook] === Overview ===\n");
        sb.append("+--------------------------------------+\n");
        sb.append("| Build                                |\n");
        sb.append("+--------------------------------------+\n");
        sb.append("Class: ").append(displayClass(player)).append("\n");
        sb.append("Style: ").append(displayStyle(player)).append("\n");
        sb.append("Level: ").append(player.getLevel()).append(" | XP: ")
                .append(player.getCurrentXp()).append("/")
                .append(levelingManager.calculateXpRequired(player.getLevel())).append("\n");
        sb.append("Stats: ").append(levelingManager.describePlayerStatGrowth(player)).append("\n");
        sb.append("+--------------------------------------+\n");
        sb.append("| Next                                 |\n");
        sb.append("+--------------------------------------+\n");
        sb.append("Next Step: ").append(getNextStep(player)).append("\n");
        sb.append("Pending Perks: ").append(perkManager.hasPendingPerkSelection(player) ? "Yes" : "No").append("\n");
        sb.append("Active Synergies: ").append(player.getActiveSynergyBonuses().size()).append("\n");
        sb.append("Casting: cooldowns, durations, charges, and action timing\n");
        sb.append("+--------------------------------------+\n");
        sb.append("Sections: ").append(getSectionList()).append("\n");
        sb.append("Use: /motm spellbook <section>");
        return sb.toString();
    }

    private String renderClass(PlayerData player) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM Spellbook] === Class ===\n");
        sb.append("Class: ").append(displayClass(player)).append("\n");
        if (player.getPlayerClass() != null) {
            ClassData classData = dataLoader.getClassData(player.getPlayerClass());
            if (classData != null) {
                sb.append("Theme: ").append(classData.getTheme()).append("\n");
                sb.append("Element: ").append(classData.getElement()).append("\n");
                sb.append("Passive: ").append(classData.getPassiveAbility().getName()).append("\n");
                sb.append("  ").append(classData.getPassiveAbility().getDescription()).append("\n");
                String passiveSummary = PassivePresentation.buildPassiveSummary(classData.getPassiveAbility());
                if (!passiveSummary.isBlank()) {
                    sb.append("  ").append(passiveSummary).append("\n");
                }
                String passiveState = classPassiveManager.buildPassiveStateSummary(player);
                if (!passiveState.isBlank()) {
                    sb.append("  State: ").append(passiveState).append("\n");
                }
            }
        }
        sb.append("Style: ").append(displayStyle(player)).append("\n");
        sb.append("Level: ").append(player.getLevel()).append(" / ").append(LevelingManager.MAX_LEVEL).append("\n");
        sb.append("Return: /motm spellbook overview");
        return sb.toString();
    }

    private String renderStyle(PlayerData player) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM Spellbook] === Style ===\n");
        if (player.getPlayerClass() == null) {
            sb.append("Choose a class first.\n");
            sb.append("Use: /motm class <id>");
            return sb.toString();
        }

        StyleData selected = getSelectedStyle(player);
        sb.append("Current: ").append(selected != null ? selected.getName() : "Unchosen").append("\n");
        sb.append("Available Styles:\n");
        for (StyleData style : dataLoader.getStylesForClass(player.getPlayerClass())) {
            boolean current = selected != null && selected.getId().equals(style.getId());
            sb.append(current ? ">> " : "   ");
            sb.append(style.getId()).append(" - ").append(style.getName()).append("\n");
            sb.append("   ").append(compact(style.getTheme(), 72)).append("\n");
        }
        sb.append("Use: /motm style <styleId>");
        return sb.toString();
    }

    private String renderAbilities(PlayerData player) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM Spellbook] === Abilities ===\n");
        if (player.getPlayerClass() == null) {
            sb.append("Choose a class first.\n");
            sb.append("Use: /motm class <id>");
            return sb.toString();
        }

        StyleData style = getSelectedStyle(player);
        if (style == null) {
            sb.append("Choose a style to define your active abilities.\n");
            sb.append("Use: /motm style <styleId>");
            return sb.toString();
        }

        sb.append("Style: ").append(style.getName()).append("\n");
        sb.append("Theme: ").append(style.getTheme()).append("\n");
        sb.append("Casting: cooldowns, durations, charges, and action timing\n");
        sb.append("Abilities:\n");
        for (AbilityData ability : style.getAbilities()) {
            double cooldown = styleManager.getRemainingCooldownSeconds(player.getPlayerId(), ability.getId());
            sb.append("  ").append(ability.getName()).append(" [").append(ability.getId()).append("]\n");
            sb.append("    ").append(AbilityPresentation.buildEffectSummary(ability)).append("\n");
            String profile = AbilityPresentation.buildSpatialSummary(ability);
            if (!profile.isBlank()) {
                sb.append("    ").append(profile).append("\n");
            }
            String visuals = AbilityPresentation.buildVisualSummary(player.getPlayerClass(), style.getId(), ability);
            if (!visuals.isBlank()) {
                sb.append("    ").append(visuals).append("\n");
            }
            sb.append("    Cooldown ").append(AbilityPresentation.formatDecimal(ability.getCooldownSeconds())).append("s");
            if (cooldown > 0) {
                sb.append(" | Ready in ").append(AbilityPresentation.formatDecimal(cooldown)).append("s");
            }
            sb.append("\n");
        }
        sb.append("Use: /motm abilities | /motm cast <abilityId>");
        return sb.toString();
    }

    private String renderPerks(PlayerData player) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM Spellbook] === Perks ===\n");
        sb.append("Design: styles grant active abilities; perks modify your build.\n");

        if (player.getPlayerClass() == null) {
            sb.append("Choose a class first.\n");
            sb.append("Use: /motm class <id>");
            return sb.toString();
        }

        int currentTier = perkManager.getCurrentTier(player.getLevel());
        sb.append("Unlocked Tiers: ").append(currentTier).append(" / ").append(PerkManager.TOTAL_TIERS).append("\n");
        sb.append("Owned Perks: ").append(player.getSelectedPerks().size()).append(" / ")
                .append(PerkManager.MAX_TOTAL_PERKS).append("\n");

        if (perkManager.hasPendingPerkSelection(player)) {
            int pendingTier = perkManager.getPendingSelectionTier(player);
            List<Perk> available = perkManager.getAvailablePerks(player);
            sb.append("Pending Choice: ").append(pendingTier).append(" (pick 1 from shared pool)\n");
            sb.append("Available:\n");
            for (int i = 0; i < available.size(); i++) {
                Perk perk = available.get(i);
                sb.append("  [").append(i + 1).append("] ").append(perk.getName()).append("\n");
            }
            sb.append("Use: /motm perks | /motm select 4\n");
        } else {
            int nextTier = currentTier + 1;
            if (nextTier <= PerkManager.TOTAL_TIERS) {
                sb.append("Next Tier Unlock: Level ").append(nextTier * LevelingManager.MILESTONE_INTERVAL).append("\n");
            } else {
                sb.append("All perk tiers unlocked.\n");
            }
        }

        sb.append("Categories:\n");
        sb.append("  Stats | Utility | Ability Mods | Triggers | Synergies\n");
        return sb.toString();
    }

    private String renderProgression(PlayerData player) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM Spellbook] === Stats ===\n");
        sb.append("Level: ").append(player.getLevel()).append(" / ").append(LevelingManager.MAX_LEVEL).append("\n");
        sb.append("XP: ").append(player.getCurrentXp()).append("/")
                .append(levelingManager.calculateXpRequired(player.getLevel())).append("\n");
        sb.append("Total XP Earned: ").append(player.getTotalXpEarned()).append("\n");
        appendStatTable(sb, player);
        sb.append("Perks Chosen: ").append(player.getSelectedPerks().size()).append(" / ")
                .append(PerkManager.MAX_TOTAL_PERKS).append("\n");
        sb.append("Achievements: ").append(player.getAchievements().size()).append("\n");
        sb.append("Next Step: ").append(getNextStep(player)).append("\n");
        sb.append("Mob Scaling: enemies use level title bands and internal stat presets; mobs never receive perks.\n");
        sb.append("Spend: /motm stats spend <vigor|tenacity|endurance|agility|luck> [points]");
        return sb.toString();
    }

    private void appendStatTable(StringBuilder sb, PlayerData player) {
        PlayerData.StatAllocation stats = player.getStatAllocation();
        sb.append("Unspent Stat Points: ").append(player.getUnspentStatPoints()).append("\n");
        sb.append("Vigor ").append(stats.getVigor()).append(" | HP +")
                .append(AbilityPresentation.formatDecimal((levelingManager.getVigorHealthMultiplier(player) - 1.0) * 100.0))
                .append("% | DR +")
                .append(AbilityPresentation.formatDecimal(levelingManager.getVigorDamageReduction(player) * 100.0))
                .append("%\n");
        sb.append("Tenacity ").append(stats.getTenacity()).append(" | Damage +")
                .append(AbilityPresentation.formatDecimal((levelingManager.getTenacityDamageMultiplier(player) - 1.0) * 100.0))
                .append("% | Crit damage +")
                .append(AbilityPresentation.formatDecimal(levelingManager.getTenacityCritDamageBonus(player) * 100.0))
                .append("%\n");
        sb.append("Endurance ").append(stats.getEndurance()).append(" | Stamina +")
                .append(AbilityPresentation.formatDecimal((levelingManager.getEnduranceStaminaMultiplier(player) - 1.0) * 100.0))
                .append("% | Regen +")
                .append(AbilityPresentation.formatDecimal(levelingManager.getEnduranceStaminaRegenBonus(player) * 100.0))
                .append("%\n");
        sb.append("Agility ").append(stats.getAgility()).append(" | Speed +")
                .append(AbilityPresentation.formatDecimal((levelingManager.getAgilitySpeedMultiplier(player) - 1.0) * 100.0))
                .append("% | Melee attack speed +")
                .append(AbilityPresentation.formatDecimal(levelingManager.getAgilityMeleeAttackSpeedBonus(player) * 100.0))
                .append("%\n");
        sb.append("Luck ").append(stats.getLuck()).append(" | Crit chance +")
                .append(AbilityPresentation.formatDecimal(levelingManager.getLuckCritChanceBonus(player) * 100.0))
                .append("% | XP +")
                .append(AbilityPresentation.formatDecimal((levelingManager.getLuckXpMultiplier(player) - 1.0) * 100.0))
                .append("%\n");
    }

    private String displayClass(PlayerData player) {
        if (player.getPlayerClass() == null) {
            return "Unchosen";
        }
        ClassData classData = dataLoader.getClassData(player.getPlayerClass());
        return classData != null ? classData.getDisplayName() : player.getPlayerClass();
    }

    private String displayStyle(PlayerData player) {
        StyleData style = getSelectedStyle(player);
        return style != null ? style.getName() : "Unchosen";
    }

    private StyleData getSelectedStyle(PlayerData player) {
        if (player.getPlayerClass() == null || player.getSelectedStyles().isEmpty()) {
            return null;
        }
        return dataLoader.getStyleById(player.getSelectedStyles().get(0), player.getPlayerClass());
    }

    private String getNextStep(PlayerData player) {
        if (player.getPlayerClass() == null) {
            return "Choose your class";
        }
        if (player.getSelectedStyles().isEmpty()) {
            return "Choose your style";
        }
        if (perkManager.hasPendingPerkSelection(player)) {
            return "Choose perk tier " + perkManager.getPendingSelectionTier(player);
        }

        int nextTier = perkManager.getCurrentTier(player.getLevel()) + 1;
        if (nextTier <= PerkManager.TOTAL_TIERS) {
            return "Reach level " + (nextTier * LevelingManager.MILESTONE_INTERVAL) + " for the next perk tier";
        }
        return "Spend stat points and refine your build";
    }

    private String compact(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }
}
