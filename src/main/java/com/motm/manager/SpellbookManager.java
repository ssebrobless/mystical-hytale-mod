package com.motm.manager;

import com.motm.model.AbilityData;
import com.motm.model.ClassData;
import com.motm.model.Perk;
import com.motm.model.PlayerData;
import com.motm.model.RaceData;
import com.motm.model.StyleData;
import com.motm.util.DataLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Centralizes the spellbook information architecture so the same page model can
 * back chat output now and a custom Hytale UI later.
 */
public class SpellbookManager {

    public enum Section {
        OVERVIEW,
        JOURNEY,
        GRIMOIRE,
        PERKS,
        RESOURCES,
        CODEX,
        JOURNAL
    }

    private final DataLoader dataLoader;
    private final LevelingManager levelingManager;
    private final StyleManager styleManager;
    private final PerkManager perkManager;
    private final ResourceManager resourceManager;

    public SpellbookManager(DataLoader dataLoader,
                            LevelingManager levelingManager,
                            StyleManager styleManager,
                            PerkManager perkManager,
                            ResourceManager resourceManager) {
        this.dataLoader = dataLoader;
        this.levelingManager = levelingManager;
        this.styleManager = styleManager;
        this.perkManager = perkManager;
        this.resourceManager = resourceManager;
    }

    public String render(PlayerData player, Section section) {
        return switch (section) {
            case OVERVIEW -> renderOverview(player);
            case JOURNEY -> renderJourney(player);
            case GRIMOIRE -> renderGrimoire(player);
            case PERKS -> renderPerks(player);
            case RESOURCES -> renderResources(player);
            case CODEX -> renderCodex(player);
            case JOURNAL -> renderJournal(player);
        };
    }

    public Section parseSection(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return Section.OVERVIEW;
        }

        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "overview", "home", "hub" -> Section.OVERVIEW;
            case "journey", "identity", "path" -> Section.JOURNEY;
            case "grimoire", "abilities", "spells", "style" -> Section.GRIMOIRE;
            case "perks", "web" -> Section.PERKS;
            case "resources", "resource" -> Section.RESOURCES;
            case "codex", "guide", "reactions" -> Section.CODEX;
            case "journal", "story", "lore" -> Section.JOURNAL;
            default -> null;
        };
    }

    public String getSectionList() {
        return "overview, journey, grimoire, perks, resources, codex, journal";
    }

    private String renderOverview(PlayerData player) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM Spellbook] === Overview ===\n");
        sb.append("┌ Identity ─────────────────────────────┐\n");
        sb.append("Class: ").append(displayClass(player)).append("\n");
        sb.append("Race: ").append(displayRace(player)).append("\n");
        sb.append("Style: ").append(displayStyle(player)).append("\n");
        sb.append("Level: ").append(player.getLevel()).append(" | XP: ")
                .append(player.getCurrentXp()).append("/")
                .append(levelingManager.calculateXpRequired(player.getLevel())).append("\n");
        sb.append("└───────────────────────────────────────┘\n");
        sb.append("┌ Current Path ─────────────────────────┐\n");
        sb.append("Next Step: ").append(getNextStep(player)).append("\n");
        sb.append("Pending Perks: ").append(perkManager.hasPendingPerkSelection(player) ? "Yes" : "No").append("\n");
        sb.append("Active Synergies: ").append(player.getActiveSynergyBonuses().size()).append("\n");
        sb.append("Resources: ").append(player.getPlayerClass() != null
                ? resourceManager.getResourceDisplay(player.getPlayerId(), player.getPlayerClass())
                : "No class selected").append("\n");
        sb.append("└───────────────────────────────────────┘\n");
        sb.append("Sections: ").append(getSectionList()).append("\n");
        sb.append("Use: /motm spellbook <section>");
        return sb.toString();
    }

    private String renderJourney(PlayerData player) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM Spellbook] === Journey ===\n");
        sb.append("Class: ").append(displayClass(player)).append("\n");
        if (player.getPlayerClass() != null) {
            ClassData classData = dataLoader.getClassData(player.getPlayerClass());
            if (classData != null) {
                sb.append("Theme: ").append(classData.getTheme()).append("\n");
                sb.append("Element: ").append(classData.getElement()).append("\n");
                sb.append("Passive: ").append(classData.getPassiveAbility().getName()).append("\n");
            }
        }
        sb.append("Race: ").append(displayRace(player)).append("\n");
        if (player.getRace() != null) {
            RaceData race = dataLoader.getRaceById(player.getRace());
            if (race != null) {
                sb.append("Race Passive: ").append(race.getPassive()).append("\n");
            }
        }
        sb.append("Style: ").append(displayStyle(player)).append("\n");
        sb.append("Level: ").append(player.getLevel()).append(" / ").append(LevelingManager.MAX_LEVEL).append("\n");
        sb.append("Total XP Earned: ").append(player.getTotalXpEarned()).append("\n");
        sb.append("Perks Chosen: ").append(player.getSelectedPerks().size()).append(" / 60\n");
        sb.append("Achievements: ").append(player.getAchievements().size()).append("\n");
        sb.append("Next Step: ").append(getNextStep(player)).append("\n");
        sb.append("Return: /motm spellbook overview");
        return sb.toString();
    }

    private String renderGrimoire(PlayerData player) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM Spellbook] === Grimoire ===\n");
        if (player.getPlayerClass() == null) {
            sb.append("Choose a class to awaken your grimoire.\n");
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
        sb.append("Resource: ").append(style.getResourceType()).append("\n");
        sb.append("Abilities:\n");
        for (AbilityData ability : style.getAbilities()) {
            double cooldown = styleManager.getRemainingCooldownSeconds(player.getPlayerId(), ability.getId());
            sb.append("  ").append(ability.getName()).append(" [").append(ability.getId()).append("]\n");
            sb.append("    ").append(abilitySummary(ability)).append("\n");
            sb.append("    Cost ").append(ability.getResourceCost())
                    .append(" | Cooldown ").append(formatDecimal(ability.getCooldownSeconds())).append("s");
            if (cooldown > 0) {
                sb.append(" | Ready in ").append(formatDecimal(cooldown)).append("s");
            }
            sb.append("\n");
        }
        sb.append("Use: /motm abilities | /motm cast <abilityId>");
        return sb.toString();
    }

    private String renderPerks(PlayerData player) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM Spellbook] === Perk Web ===\n");
        sb.append("Design: styles grant active abilities; perks modify your build.\n");

        if (player.getPlayerClass() == null) {
            sb.append("Choose a class first.\n");
            sb.append("Use: /motm class <id>");
            return sb.toString();
        }

        int currentTier = perkManager.getCurrentTier(player.getLevel());
        sb.append("Unlocked Tiers: ").append(currentTier).append(" / ").append(PerkManager.TOTAL_TIERS).append("\n");
        sb.append("Owned Perks: ").append(player.getSelectedPerks().size()).append(" / ").append(PerkManager.MAX_TOTAL_PERKS).append("\n");

        if (perkManager.hasPendingPerkSelection(player)) {
            int pendingTier = perkManager.getPendingSelectionTier(player);
            List<Perk> available = perkManager.getAvailablePerks(player);
            sb.append("Pending Tier: ").append(pendingTier).append(" (pick 3 of 10)\n");
            sb.append("Available:\n");
            for (int i = 0; i < available.size(); i++) {
                Perk perk = available.get(i);
                sb.append("  [").append(i + 1).append("] ").append(perk.getName()).append("\n");
            }
            sb.append("Use: /motm perks | /motm select 1 4 7\n");
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
        sb.append("Future UI: this page becomes the giant organized perk web.\n");
        return sb.toString();
    }

    private String renderResources(PlayerData player) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM Spellbook] === Resources ===\n");
        if (player.getPlayerClass() == null) {
            sb.append("No class selected yet.\n");
            sb.append("Use: /motm class <id>");
            return sb.toString();
        }

        sb.append("Current: ")
                .append(resourceManager.getResourceDisplay(player.getPlayerId(), player.getPlayerClass()))
                .append("\n");
        sb.append("Class Resource Notes:\n");
        sb.append(resourceNotes(player.getPlayerClass())).append("\n");
        sb.append("Use: /motm resources");
        return sb.toString();
    }

    private String renderCodex(PlayerData player) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM Spellbook] === Codex ===\n");
        sb.append("Elemental Flow:\n");
        sb.append("  class -> style -> 3 abilities -> level -> perks -> synergies\n");
        sb.append("Reaction System:\n");
        sb.append("  marks, status effects, and elemental cross-effects live here.\n");
        sb.append("Mob Scaling:\n");
        sb.append("  enemies scale from player progression and may gain elite titles.\n");
        sb.append("Current Focus:\n");
        sb.append("  spellbook UI, real perk effect hooks, and stronger ability execution.\n");
        return sb.toString();
    }

    private String renderJournal(PlayerData player) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM Spellbook] === Journal ===\n");
        sb.append("This section is reserved for story and lore integration.\n");
        sb.append("Planned:\n");
        sb.append("  Chapter pages\n");
        sb.append("  Class-specific notes\n");
        sb.append("  Race-specific lore\n");
        sb.append("  Discovery log\n");
        sb.append("  Quest and faction threads\n");
        if (player.getPlayerClass() != null) {
            sb.append("Current Path: ").append(displayClass(player));
            if (player.getRace() != null) {
                sb.append(" / ").append(displayRace(player));
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String displayClass(PlayerData player) {
        if (player.getPlayerClass() == null) {
            return "Unchosen";
        }
        ClassData classData = dataLoader.getClassData(player.getPlayerClass());
        return classData != null ? classData.getDisplayName() : player.getPlayerClass();
    }

    private String displayRace(PlayerData player) {
        if (player.getRace() == null) {
            return "Unchosen";
        }
        RaceData race = dataLoader.getRaceById(player.getRace());
        return race != null ? race.getName() : player.getRace();
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
        if (player.getRace() == null) {
            return "Choose your race";
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
        return "Refine your build and chase synergies";
    }

    private String resourceNotes(String playerClass) {
        return switch (playerClass.toLowerCase(Locale.ROOT)) {
            case "terra" -> "Terra uses gathered materials and practical terrain tools.";
            case "hydro" -> "Hydro spends water and refills from containers and sources.";
            case "aero" -> "Aero regenerates TP over time.";
            case "corruptus" -> "Corruptus earns souls from kills and cashes them into power.";
            default -> "Unknown class resource.";
        };
    }

    private String abilitySummary(AbilityData ability) {
        List<String> parts = new ArrayList<>();
        if (ability.getDamagePercent() > 0) {
            parts.add(formatDecimal(ability.getDamagePercent()) + "% dmg");
        }
        if (ability.getHealPercent() > 0) {
            parts.add(formatDecimal(ability.getHealPercent()) + "% heal");
        }
        if (ability.getShieldPercent() > 0) {
            parts.add(formatDecimal(ability.getShieldPercent()) + "% shield");
        }
        if (ability.getEffect() != null && !ability.getEffect().isBlank()) {
            parts.add("effect: " + ability.getEffect());
        }
        return parts.isEmpty() ? "utility effect" : String.join(" | ", parts);
    }

    private String formatDecimal(double value) {
        if (Math.abs(value - Math.rint(value)) < 0.0001) {
            return String.valueOf((int) Math.rint(value));
        }
        return String.format(Locale.US, "%.1f", value);
    }
}
