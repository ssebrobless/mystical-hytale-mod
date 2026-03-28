package com.motm.manager;

import com.motm.MenteesMod;
import com.motm.model.AbilityData;
import com.motm.model.Perk;
import com.motm.model.PlayerData;
import com.motm.model.RaceData;
import com.motm.model.StyleData;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Chat-driven fallback interaction layer for the player spellbook and dev grimoire.
 *
 * This keeps class, race, style, and test flows usable while the visual custom UI
 * layer is paused for stability.
 */
public class BookInteractionManager {

    private static final List<SpellbookManager.Section> SPELLBOOK_SECTIONS = List.of(
            SpellbookManager.Section.OVERVIEW,
            SpellbookManager.Section.JOURNEY,
            SpellbookManager.Section.GRIMOIRE,
            SpellbookManager.Section.PERKS,
            SpellbookManager.Section.RESOURCES,
            SpellbookManager.Section.CODEX,
            SpellbookManager.Section.JOURNAL
    );

    private static final List<DevPage> DEV_PAGES = List.of(
            DevPage.CLASS,
            DevPage.RACE,
            DevPage.STYLE,
            DevPage.AUTO_TEST,
            DevPage.ABILITIES,
            DevPage.RESOURCES,
            DevPage.LEVEL,
            DevPage.XP,
            DevPage.PERKS,
            DevPage.UTILITY,
            DevPage.RESET
    );

    private static final List<Integer> LEVEL_PRESETS = List.of(1, 10, 25, 50, 75, 100, 150, 200);
    private static final List<Integer> XP_PERCENT_PRESETS = List.of(0, 25, 50, 75, 95);
    private static final List<Integer> PERK_TIER_LEVELS = List.of(10, 20, 30, 40, 50);
    private static final List<String> CLASS_IDS = List.of("terra", "hydro", "aero", "corruptus", "clear");
    private static final List<ResetAction> RESET_ACTIONS = List.of(
            new ResetAction("Reset Player", new String[]{"dev", "reset", "player"}),
            new ResetAction("Clear Class", new String[]{"dev", "class", "clear"}),
            new ResetAction("Clear Race", new String[]{"dev", "race", "clear"}),
            new ResetAction("Clear Styles", new String[]{"dev", "styles", "clear"}),
            new ResetAction("Clear Perks", new String[]{"dev", "perks", "clear"}),
            new ResetAction("Give Spellbook", new String[]{"spellbook", "give"}),
            new ResetAction("Give Dev Grimoire", new String[]{"dev", "book"})
    );
    private static final List<String> UTILITY_ACTIONS = List.of(
            "Give Spellbook",
            "Give Dev Grimoire",
            "Clear Cooldowns",
            "Clear Status Effects",
            "Clear Reaction Marks",
            "Refresh HUD",
            "Rebuild Runtime"
    );
    private static final List<AutoTestPreset> AUTO_TEST_PRESETS = List.of(
            new AutoTestPreset("Ready Current Build", "Clear cooldowns/effects and refill current build", null, null, -1, false),
            new AutoTestPreset("Perk Tier Snapshot", "Set current build to Lv 10 for perk testing", null, null, 10, false),
            new AutoTestPreset("Terra Quake Snapshot", "Terra + Quake at Lv 25", "terra", "quake", 25, true),
            new AutoTestPreset("Hydro Surf Snapshot", "Hydro + Surf at Lv 25", "hydro", "surf", 25, true),
            new AutoTestPreset("Aero Thunder Snapshot", "Aero + Thunder at Lv 25", "aero", "thunder", 25, true),
            new AutoTestPreset("Corruptus Flame Snapshot", "Corruptus + Flame at Lv 25", "corruptus", "flame", 25, true),
            new AutoTestPreset("Primordial Form Snapshot", "Corruptus + Primordial at Lv 50", "corruptus", "primordial", 50, true)
    );

    private final MenteesMod mod;
    private final Map<String, SpellbookState> spellbookStates = new ConcurrentHashMap<>();
    private final Map<String, DevBookState> devBookStates = new ConcurrentHashMap<>();

    public BookInteractionManager(MenteesMod mod) {
        this.mod = mod;
    }

    public String openSpellbook(PlayerData player) {
        SpellbookState state = spellbookStates.computeIfAbsent(player.getPlayerId(), ignored -> new SpellbookState());
        syncSpellbookState(player, state, true);
        return renderSpellbook(player, state);
    }

    public String handleSpellbookAction(PlayerData player, int slot) {
        SpellbookState state = spellbookStates.computeIfAbsent(player.getPlayerId(), ignored -> new SpellbookState());
        syncSpellbookState(player, state, false);

        return switch (state.mode) {
            case CLASS_SELECT -> handleClassSelection(player, state, slot);
            case RACE_SELECT -> handleRaceSelection(player, state, slot);
            case STYLE_SELECT -> handleStyleSelection(player, state, slot);
            case SECTION_VIEW -> handleSectionNavigation(player, state, slot);
        };
    }

    public String openDevBook(PlayerData player) {
        DevBookState state = devBookStates.computeIfAbsent(player.getPlayerId(), ignored -> new DevBookState());
        normalizeDevState(player, state);
        return renderDevBook(player, state);
    }

    public String cycleDevPage(PlayerData player) {
        DevBookState state = devBookStates.computeIfAbsent(player.getPlayerId(), ignored -> new DevBookState());
        normalizeDevState(player, state);

        if (!state.openedOnce) {
            state.openedOnce = true;
            return renderDevBook(player, state);
        }

        if (currentDevPage(state) == DevPage.PERKS
                && hasPendingPerks(player)
                && state.queuedPerkChoices.size() == PerkManager.PERKS_TO_SELECT) {
            return confirmQueuedPerks(player, state);
        }

        state.pageIndex = (state.pageIndex + 1) % DEV_PAGES.size();
        state.optionIndex = 0;
        state.lastMessage = "Switched to " + currentDevPage(state).displayName + ".";
        normalizeDevState(player, state);
        return renderDevBook(player, state);
    }

    public String handleDevBookAction(PlayerData player, int slot) {
        DevBookState state = devBookStates.computeIfAbsent(player.getPlayerId(), ignored -> new DevBookState());
        normalizeDevState(player, state);

        switch (slot) {
            case 1 -> moveDevSelection(player, state, -1);
            case 2 -> applyDevSelection(player, state);
            case 3 -> moveDevSelection(player, state, 1);
            default -> state.lastMessage = "Unknown dev input.";
        }

        normalizeDevState(player, state);
        return renderDevBook(player, state);
    }

    private String handleClassSelection(PlayerData player, SpellbookState state, int slot) {
        List<String> classIds = CLASS_IDS.subList(0, CLASS_IDS.size() - 1);
        switch (slot) {
            case 1 -> state.selectionIndex = wrap(state.selectionIndex - 1, classIds.size());
            case 2 -> {
                String selectedClass = classIds.get(state.selectionIndex);
                state.lastMessage = summarize(run(player, "class", selectedClass));
                syncSpellbookState(player, state, true);
            }
            case 3 -> state.selectionIndex = wrap(state.selectionIndex + 1, classIds.size());
            default -> state.lastMessage = "Use Ability 1 / 2 / 3 to navigate the spellbook.";
        }
        return renderSpellbook(player, state);
    }

    private String handleRaceSelection(PlayerData player, SpellbookState state, int slot) {
        List<RaceData> races = mod.getDataLoader().getAllRaces();
        if (races.isEmpty()) {
            state.lastMessage = "No races are loaded.";
            return renderSpellbook(player, state);
        }

        switch (slot) {
            case 1 -> state.selectionIndex = wrap(state.selectionIndex - 1, races.size());
            case 2 -> {
                RaceData race = races.get(state.selectionIndex);
                state.lastMessage = summarize(run(player, "race", race.getId()));
                syncSpellbookState(player, state, true);
            }
            case 3 -> state.selectionIndex = wrap(state.selectionIndex + 1, races.size());
            default -> state.lastMessage = "Use Ability 1 / 2 / 3 to navigate the spellbook.";
        }
        return renderSpellbook(player, state);
    }

    private String handleStyleSelection(PlayerData player, SpellbookState state, int slot) {
        List<StyleData> styles = getStyles(player);
        if (styles.isEmpty()) {
            state.lastMessage = "Choose a class first to unlock styles.";
            syncSpellbookState(player, state, true);
            return renderSpellbook(player, state);
        }

        switch (slot) {
            case 1 -> state.selectionIndex = wrap(state.selectionIndex - 1, styles.size());
            case 2 -> {
                StyleData style = styles.get(state.selectionIndex);
                state.lastMessage = summarize(run(player, "style", style.getId()));
                syncSpellbookState(player, state, true);
                if (state.mode == SpellbookMode.SECTION_VIEW) {
                    state.sectionIndex = SPELLBOOK_SECTIONS.indexOf(SpellbookManager.Section.GRIMOIRE);
                    if (state.sectionIndex < 0) {
                        state.sectionIndex = 0;
                    }
                }
            }
            case 3 -> state.selectionIndex = wrap(state.selectionIndex + 1, styles.size());
            default -> state.lastMessage = "Use Ability 1 / 2 / 3 to navigate the spellbook.";
        }
        return renderSpellbook(player, state);
    }

    private String handleSectionNavigation(PlayerData player, SpellbookState state, int slot) {
        switch (slot) {
            case 1 -> state.sectionIndex = wrap(state.sectionIndex - 1, SPELLBOOK_SECTIONS.size());
            case 2 -> state.lastMessage = "Viewing " + currentSection(state).name().toLowerCase(Locale.ROOT) + ".";
            case 3 -> state.sectionIndex = wrap(state.sectionIndex + 1, SPELLBOOK_SECTIONS.size());
            default -> state.lastMessage = "Use Ability 1 / 2 / 3 to move between spellbook sections.";
        }
        return renderSpellbook(player, state);
    }

    private void moveDevSelection(PlayerData player, DevBookState state, int delta) {
        int optionCount = devOptionCount(player, state);
        if (optionCount <= 0) {
            state.optionIndex = 0;
            state.lastMessage = "No options on this page yet.";
            return;
        }

        state.optionIndex = wrap(state.optionIndex + delta, optionCount);
    }

    private void applyDevSelection(PlayerData player, DevBookState state) {
        DevPage page = currentDevPage(state);
        switch (page) {
            case CLASS -> applyDevClass(player, state);
            case RACE -> applyDevRace(player, state);
            case STYLE -> applyDevStyle(player, state);
            case AUTO_TEST -> applyAutoTestPreset(player, state);
            case ABILITIES -> applyDevAbility(player, state);
            case RESOURCES -> applyDevResource(player, state);
            case LEVEL -> applyDevLevel(player, state);
            case XP -> applyDevXp(player, state);
            case PERKS -> applyDevPerkSelection(player, state);
            case UTILITY -> applyUtilityAction(player, state);
            case RESET -> applyResetAction(player, state);
        }
    }

    private void applyDevClass(PlayerData player, DevBookState state) {
        String classId = CLASS_IDS.get(state.optionIndex);
        state.lastMessage = summarize("clear".equals(classId)
                ? run(player, "dev", "class", "clear")
                : run(player, "dev", "class", "set", classId));
        state.optionIndex = 0;
    }

    private void applyDevRace(PlayerData player, DevBookState state) {
        List<RaceData> races = mod.getDataLoader().getAllRaces();
        int optionCount = races.size() + 1;
        if (optionCount <= 0) {
            state.lastMessage = "No race options available.";
            return;
        }

        if (state.optionIndex >= races.size()) {
            state.lastMessage = summarize(run(player, "dev", "race", "clear"));
            return;
        }

        RaceData race = races.get(state.optionIndex);
        state.lastMessage = summarize(run(player, "dev", "race", "set", race.getId()));
    }

    private void applyDevStyle(PlayerData player, DevBookState state) {
        List<StyleData> styles = getStyles(player);
        if (styles.isEmpty()) {
            state.lastMessage = "Choose a class first to load its styles.";
            return;
        }

        if (state.optionIndex >= styles.size()) {
            state.lastMessage = summarize(run(player, "dev", "styles", "clear"));
            return;
        }

        StyleData style = styles.get(state.optionIndex);
        state.lastMessage = summarize(run(player, "style", style.getId()));
    }

    private void applyAutoTestPreset(PlayerData player, DevBookState state) {
        AutoTestPreset preset = AUTO_TEST_PRESETS.get(state.optionIndex);

        if (preset.classId() == null) {
            if (preset.level() > 0) {
                if (player.getPlayerClass() == null) {
                    state.lastMessage = "Choose a class first, or use a class snapshot preset.";
                    return;
                }
                state.lastMessage = summarize(run(player, "dev", "level", "set", String.valueOf(preset.level())));
                return;
            }

            if (player.getPlayerClass() == null) {
                state.lastMessage = "Choose a class first, or use a class snapshot preset.";
                return;
            }

            mod.getStyleManager().resetCooldowns(player.getPlayerId());
            fillResourcesForTesting(player);
            clearTransientEffects(player);
            mod.rebuildPlayerRuntime(player);
            mod.getPlayerDataManager().savePlayerData(player);
            state.lastMessage = "Current build is ready for testing.";
            return;
        }

        state.lastMessage = applyBuildSnapshot(player, preset.classId(), preset.styleId(), preset.level(), preset.fillResources());
    }

    private void applyDevAbility(PlayerData player, DevBookState state) {
        List<AbilityData> abilities = getSelectedAbilities(player);
        if (abilities.isEmpty()) {
            state.lastMessage = "Choose a style first to test abilities.";
            return;
        }

        int actionOffset = abilities.size();
        if (state.optionIndex < abilities.size()) {
            AbilityData ability = abilities.get(state.optionIndex);
            var runtimePlayer = mod.getRuntimePlayer(player.getPlayerId());
            if (runtimePlayer == null) {
                state.lastMessage = "Live runtime player not found. Rejoin the world and try again.";
                return;
            }
            state.lastMessage = summarize(mod.getMotmCommand().execute(runtimePlayer, new String[]{"cast", ability.getId()}));
            return;
        }

        int actionIndex = state.optionIndex - actionOffset;
        switch (actionIndex) {
            case 0 -> {
                mod.getStyleManager().resetCooldowns(player.getPlayerId());
                mod.refreshStatusHud(player.getPlayerId());
                state.lastMessage = "Cooldowns reset.";
            }
            case 1 -> {
                fillResourcesForTesting(player);
                state.lastMessage = "Active class resources refilled.";
            }
            case 2 -> {
                clearTransientEffects(player);
                state.lastMessage = "Status effects and marks cleared.";
            }
            case 3 -> {
                mod.rebuildPlayerRuntime(player);
                mod.getPlayerDataManager().savePlayerData(player);
                state.lastMessage = "Player runtime rebuilt.";
            }
            default -> state.lastMessage = "No ability action selected.";
        }
    }

    private void applyDevResource(PlayerData player, DevBookState state) {
        List<String> options = getResourceOptionLabels(player);
        if (options.isEmpty()) {
            state.lastMessage = "aero".equalsIgnoreCase(player.getPlayerClass())
                    ? "Aero has no class resource to manage."
                    : "Choose a class first to unlock resource tools.";
            return;
        }

        String playerClass = player.getPlayerClass();
        String playerId = player.getPlayerId();
        int index = state.optionIndex;

        switch (safe(playerClass, "").toLowerCase(Locale.ROOT)) {
            case "hydro" -> {
                if (index == 0) {
                    mod.getResourceManager().fillClassResources(playerId, playerClass);
                    state.lastMessage = "Water refilled.";
                } else if (index == 1) {
                    mod.getResourceManager().clearClassResources(playerId, playerClass);
                    state.lastMessage = "Water emptied.";
                } else {
                    int newTier = index - 2;
                    player.setWaterContainerTier(Math.max(0, Math.min(newTier, ResourceManager.WATER_CONTAINER_CAPACITY.length - 1)));
                    mod.getResourceManager().synchronizePersistentState(player);
                    mod.getResourceManager().initializeForPlayer(player);
                    mod.queueHydroContainerSync(playerId);
                    state.lastMessage = "Hydro waterskin set to " + mod.getResourceManager().getWaterContainerInfo(playerId) + ".";
                }
            }
            case "corruptus" -> {
                if (index == 0) {
                    mod.getResourceManager().fillClassResources(playerId, playerClass);
                    state.lastMessage = "Souls filled.";
                } else if (index == 1) {
                    mod.getResourceManager().clearClassResources(playerId, playerClass);
                    state.lastMessage = "Souls emptied.";
                } else if (index == 2) {
                    mod.getResourceManager().add(playerId, "souls", 10);
                    state.lastMessage = "Added 10 souls.";
                }
            }
            case "terra" -> {
                String activeResource = getActiveStyleResourceType(player);
                if (index == 0) {
                    mod.getResourceManager().fillClassResources(playerId, playerClass);
                    state.lastMessage = "Terra materials filled for testing.";
                } else if (index == 1) {
                    mod.getResourceManager().clearClassResources(playerId, playerClass);
                    state.lastMessage = "Terra materials emptied.";
                } else if (index == 2 && activeResource != null) {
                    mod.getResourceManager().add(playerId, activeResource, 25);
                    state.lastMessage = "Added 25 " + mod.getResourceManager().getDisplayName(activeResource) + ".";
                } else if (index == 3 && activeResource != null) {
                    mod.getResourceManager().set(playerId, activeResource, 0);
                    state.lastMessage = "Cleared " + mod.getResourceManager().getDisplayName(activeResource) + ".";
                } else {
                    state.lastMessage = "No Terra resource action available.";
                }
            }
            default -> state.lastMessage = "No resource tools available.";
        }

        syncResourceState(player);
    }

    private void applyDevLevel(PlayerData player, DevBookState state) {
        int level = LEVEL_PRESETS.get(state.optionIndex);
        state.lastMessage = summarize(run(player, "dev", "level", "set", String.valueOf(level)));
    }

    private void applyDevXp(PlayerData player, DevBookState state) {
        int percent = XP_PERCENT_PRESETS.get(state.optionIndex);
        int xpRequired = Math.max(1, mod.getLevelingManager().calculateXpRequired(player.getLevel()));
        int xpValue = Math.max(0, Math.min(xpRequired - 1, (int) Math.round((xpRequired - 1) * (percent / 100.0))));
        state.lastMessage = summarize(run(player, "dev", "xp", "set", String.valueOf(xpValue)));
    }

    private void applyDevPerkSelection(PlayerData player, DevBookState state) {
        if (!hasPendingPerks(player)) {
            int level = PERK_TIER_LEVELS.get(state.optionIndex);
            state.lastMessage = summarize(run(player, "dev", "level", "set", String.valueOf(level)));
            return;
        }

        List<Perk> available = mod.getPerkManager().getAvailablePerks(player);
        if (available.isEmpty()) {
            state.lastMessage = "No perk options are available.";
            return;
        }

        int perkChoice = state.optionIndex + 1;
        if (state.queuedPerkChoices.contains(perkChoice)) {
            state.queuedPerkChoices.remove(Integer.valueOf(perkChoice));
            state.lastMessage = "Removed perk [" + perkChoice + "] from the queue.";
            return;
        }

        if (state.queuedPerkChoices.size() >= PerkManager.PERKS_TO_SELECT) {
            state.lastMessage = "Queue only 3 perks, then press Use to confirm them.";
            return;
        }

        state.queuedPerkChoices.add(perkChoice);
        Collections.sort(state.queuedPerkChoices);
        state.lastMessage = "Queued perk [" + perkChoice + "] " + available.get(perkChoice - 1).getName() + ".";
    }

    private String confirmQueuedPerks(PlayerData player, DevBookState state) {
        if (!hasPendingPerks(player) || state.queuedPerkChoices.size() != PerkManager.PERKS_TO_SELECT) {
            state.lastMessage = "Queue exactly 3 perks before confirming.";
            return renderDevBook(player, state);
        }

        state.lastMessage = summarize(run(
                player,
                "select",
                String.valueOf(state.queuedPerkChoices.get(0)),
                String.valueOf(state.queuedPerkChoices.get(1)),
                String.valueOf(state.queuedPerkChoices.get(2))
        ));
        state.queuedPerkChoices.clear();
        normalizeDevState(player, state);
        return renderDevBook(player, state);
    }

    private void applyResetAction(PlayerData player, DevBookState state) {
        ResetAction action = RESET_ACTIONS.get(state.optionIndex);
        state.lastMessage = summarize(run(player, action.command()));
    }

    private void applyUtilityAction(PlayerData player, DevBookState state) {
        switch (state.optionIndex) {
            case 0 -> {
                var runtimePlayer = mod.getRuntimePlayer(player.getPlayerId());
                state.lastMessage = runtimePlayer == null
                        ? "Live runtime player not found. Rejoin the world and try again."
                        : summarize(mod.getMotmCommand().execute(runtimePlayer, new String[]{"spellbook", "give"}));
            }
            case 1 -> {
                var runtimePlayer = mod.getRuntimePlayer(player.getPlayerId());
                state.lastMessage = runtimePlayer == null
                        ? "Live runtime player not found. Rejoin the world and try again."
                        : summarize(mod.getMotmCommand().execute(runtimePlayer, new String[]{"dev", "book"}));
            }
            case 2 -> {
                mod.getStyleManager().resetCooldowns(player.getPlayerId());
                mod.refreshStatusHud(player.getPlayerId());
                state.lastMessage = "Cooldowns cleared.";
            }
            case 3 -> {
                mod.getStatusEffectManager().clearEffects(player.getPlayerId());
                mod.refreshStatusHud(player.getPlayerId());
                state.lastMessage = "Status effects cleared.";
            }
            case 4 -> {
                mod.getElementalReactionManager().clearMarks(player.getPlayerId());
                mod.refreshStatusHud(player.getPlayerId());
                state.lastMessage = "Elemental marks cleared.";
            }
            case 5 -> {
                mod.refreshStatusHud(player.getPlayerId());
                state.lastMessage = "HUD refreshed.";
            }
            case 6 -> {
                mod.rebuildPlayerRuntime(player);
                mod.getPlayerDataManager().savePlayerData(player);
                state.lastMessage = "Runtime rebuilt.";
            }
            default -> state.lastMessage = "No utility action selected.";
        }
    }

    private void syncSpellbookState(PlayerData player, SpellbookState state, boolean resetSelection) {
        SpellbookMode targetMode = determineSpellbookMode(player);
        if (state.mode != targetMode) {
            state.mode = targetMode;
            resetSelection = true;
        }

        if (targetMode == SpellbookMode.SECTION_VIEW) {
            if (resetSelection || state.sectionIndex >= SPELLBOOK_SECTIONS.size()) {
                state.sectionIndex = Math.max(0, SPELLBOOK_SECTIONS.indexOf(SpellbookManager.Section.OVERVIEW));
            }
            return;
        }

        int optionCount = switch (targetMode) {
            case CLASS_SELECT -> 4;
            case RACE_SELECT -> mod.getDataLoader().getAllRaces().size();
            case STYLE_SELECT -> getStyles(player).size();
            case SECTION_VIEW -> SPELLBOOK_SECTIONS.size();
        };
        if (optionCount <= 0) {
            state.selectionIndex = 0;
        } else if (resetSelection || state.selectionIndex >= optionCount) {
            state.selectionIndex = 0;
        } else {
            state.selectionIndex = wrap(state.selectionIndex, optionCount);
        }
    }

    private void normalizeDevState(PlayerData player, DevBookState state) {
        if (state.pageIndex < 0 || state.pageIndex >= DEV_PAGES.size()) {
            state.pageIndex = 0;
        }

        int optionCount = devOptionCount(player, state);
        if (optionCount <= 0) {
            state.optionIndex = 0;
        } else if (state.optionIndex >= optionCount) {
            state.optionIndex = 0;
        } else {
            state.optionIndex = wrap(state.optionIndex, optionCount);
        }

        if (!hasPendingPerks(player)) {
            state.queuedPerkChoices.clear();
            return;
        }

        List<Perk> available = mod.getPerkManager().getAvailablePerks(player);
        state.queuedPerkChoices.removeIf(choice -> choice < 1 || choice > available.size());
    }

    private int devOptionCount(PlayerData player, DevBookState state) {
        return switch (currentDevPage(state)) {
            case CLASS -> CLASS_IDS.size();
            case RACE -> mod.getDataLoader().getAllRaces().size() + 1;
            case STYLE -> getStyles(player).isEmpty() ? 0 : getStyles(player).size() + 1;
            case AUTO_TEST -> AUTO_TEST_PRESETS.size();
            case ABILITIES -> devAbilityOptionCount(player);
            case RESOURCES -> devResourceOptionCount(player);
            case LEVEL -> LEVEL_PRESETS.size();
            case XP -> XP_PERCENT_PRESETS.size();
            case PERKS -> hasPendingPerks(player)
                    ? mod.getPerkManager().getAvailablePerks(player).size()
                    : PERK_TIER_LEVELS.size();
            case UTILITY -> UTILITY_ACTIONS.size();
            case RESET -> RESET_ACTIONS.size();
        };
    }

    private String renderSpellbook(PlayerData player, SpellbookState state) {
        return switch (state.mode) {
            case CLASS_SELECT -> renderClassPage(state);
            case RACE_SELECT -> renderRacePage(state);
            case STYLE_SELECT -> renderStylePage(player, state);
            case SECTION_VIEW -> renderSectionPage(player, state);
        };
    }

    private String renderClassPage(SpellbookState state) {
        StringBuilder sb = header("Spellbook", "Class Selection", state.lastMessage);
        List<String> classIds = CLASS_IDS.subList(0, CLASS_IDS.size() - 1);
        String selectedClass = classIds.get(state.selectionIndex);
        var classData = mod.getDataLoader().getClassData(selectedClass);

        appendChoiceBox(sb, classData.getDisplayName());
        sb.append("Theme: ").append(classData.getTheme()).append("\n");
        sb.append("Passive: ").append(classData.getPassiveAbility().getName()).append("\n");
        sb.append("Controls: Ability1 prev | Ability2 choose | Ability3 next\n\n");
        sb.append("Classes:\n");
        for (int i = 0; i < classIds.size(); i++) {
            var data = mod.getDataLoader().getClassData(classIds.get(i));
            sb.append(i == state.selectionIndex ? "> " : "  ")
                    .append(data.getId()).append(" - ")
                    .append(data.getDisplayName()).append("\n");
        }
        return sb.toString();
    }

    private String renderRacePage(SpellbookState state) {
        StringBuilder sb = header("Spellbook", "Race Selection", state.lastMessage);
        List<RaceData> races = mod.getDataLoader().getAllRaces();
        if (races.isEmpty()) {
            sb.append("No races are available.\n");
            return sb.toString();
        }

        RaceData race = races.get(state.selectionIndex);
        appendChoiceBox(sb, race.getName());
        sb.append("Passive: ").append(compact(race.getPassive(), 72)).append("\n");
        sb.append("Special: ").append(compact(race.getSpecial(), 72)).append("\n");
        sb.append("Controls: Ability1 prev | Ability2 choose | Ability3 next\n\n");
        sb.append("Races:\n");
        for (int i = 0; i < races.size(); i++) {
            RaceData option = races.get(i);
            sb.append(i == state.selectionIndex ? "> " : "  ")
                    .append(option.getId()).append(" - ")
                    .append(option.getName()).append("\n");
        }
        return sb.toString();
    }

    private String renderStylePage(PlayerData player, SpellbookState state) {
        StringBuilder sb = header("Spellbook", "Style Selection", state.lastMessage);
        List<StyleData> styles = getStyles(player);
        if (styles.isEmpty()) {
            sb.append("Choose a class first to unlock styles.\n");
            return sb.toString();
        }

        StyleData style = styles.get(state.selectionIndex);
        appendChoiceBox(sb, style.getName());
        sb.append("Theme: ").append(compact(style.getTheme(), 72)).append("\n");
        sb.append("Abilities: ").append(joinAbilityNames(style)).append("\n");
        sb.append("Controls: Ability1 prev | Ability2 choose | Ability3 next\n\n");
        sb.append("Styles:\n");
        for (int i = 0; i < styles.size(); i++) {
            StyleData option = styles.get(i);
            sb.append(i == state.selectionIndex ? "> " : "  ")
                    .append(option.getId()).append(" - ")
                    .append(option.getName()).append("\n");
        }
        return sb.toString();
    }

    private String renderSectionPage(PlayerData player, SpellbookState state) {
        StringBuilder sb = new StringBuilder();
        sb.append(mod.getSpellbookManager().render(player, currentSection(state)));
        sb.append("\n\n");
        if (!state.lastMessage.isBlank()) {
            sb.append("Last Action: ").append(state.lastMessage).append("\n");
        }
        sb.append("Book Controls: Ability1 prev page | Ability2 refresh | Ability3 next page");
        return sb.toString();
    }

    private String renderDevBook(PlayerData player, DevBookState state) {
        DevPage page = currentDevPage(state);
        StringBuilder sb = header("Dev Grimoire", page.displayName, state.lastMessage);
        sb.append("Page ").append(state.pageIndex + 1).append(" / ").append(DEV_PAGES.size())
                .append(" | Use -> next page\n");
        sb.append("Player: ").append(safe(player.getPlayerClass(), "No class"))
                .append(" / ").append(safe(player.getRace(), "No race"))
                .append(" / ").append(currentStyleId(player))
                .append(" | Lv ").append(player.getLevel()).append("\n");
        sb.append("Slots: ").append(currentAbilitySummary(player)).append("\n");
        if (player.getPlayerClass() != null) {
            sb.append("Resources: ")
                    .append(mod.getResourceManager().getResourceDisplay(player.getPlayerId(), player.getPlayerClass()))
                    .append("\n");
        }
        sb.append("Controls: Ability1 prev | Ability2 apply/toggle | Ability3 next\n\n");

        switch (page) {
            case CLASS -> renderClassDevOptions(sb, state);
            case RACE -> renderRaceDevOptions(sb, state);
            case STYLE -> renderStyleDevOptions(player, sb, state);
            case AUTO_TEST -> renderAutoTestOptions(sb, state);
            case ABILITIES -> renderAbilityDevOptions(player, sb, state);
            case RESOURCES -> renderResourceDevOptions(player, sb, state);
            case LEVEL -> renderLevelDevOptions(sb, state);
            case XP -> renderXpDevOptions(player, sb, state);
            case PERKS -> renderPerkDevOptions(player, sb, state);
            case UTILITY -> renderUtilityDevOptions(sb, state);
            case RESET -> renderResetDevOptions(sb, state);
        }

        return sb.toString();
    }

    private void renderClassDevOptions(StringBuilder sb, DevBookState state) {
        sb.append("Class tools:\n");
        for (int i = 0; i < CLASS_IDS.size(); i++) {
            String classId = CLASS_IDS.get(i);
            String label = "clear".equals(classId)
                    ? "Clear class"
                    : mod.getDataLoader().getClassData(classId).getDisplayName();
            appendOption(sb, i == state.optionIndex, label);
        }
    }

    private void renderRaceDevOptions(StringBuilder sb, DevBookState state) {
        sb.append("Race tools:\n");
        List<RaceData> races = mod.getDataLoader().getAllRaces();
        for (int i = 0; i < races.size(); i++) {
            appendOption(sb, i == state.optionIndex, races.get(i).getName());
        }
        appendOption(sb, state.optionIndex == races.size(), "Clear race");
    }

    private void renderStyleDevOptions(PlayerData player, StringBuilder sb, DevBookState state) {
        List<StyleData> styles = getStyles(player);
        if (styles.isEmpty()) {
            sb.append("Choose a class first to load style options.\n");
            return;
        }

        sb.append("Style tools:\n");
        for (int i = 0; i < styles.size(); i++) {
            appendOption(sb, i == state.optionIndex, styles.get(i).getName());
        }
        appendOption(sb, state.optionIndex == styles.size(), "Clear styles");
    }

    private void renderAutoTestOptions(StringBuilder sb, DevBookState state) {
        sb.append("One-press test presets:\n");
        for (int i = 0; i < AUTO_TEST_PRESETS.size(); i++) {
            AutoTestPreset preset = AUTO_TEST_PRESETS.get(i);
            appendOption(sb, i == state.optionIndex, preset.label());
        }

        AutoTestPreset selected = AUTO_TEST_PRESETS.get(state.optionIndex);
        sb.append("\nSelected:\n");
        sb.append("  ").append(selected.description()).append("\n");
        if (selected.classId() != null) {
            sb.append("  Class: ").append(selected.classId())
                    .append(" | Style: ").append(selected.styleId())
                    .append(" | Level: ").append(selected.level()).append("\n");
        }
    }

    private void renderAbilityDevOptions(PlayerData player, StringBuilder sb, DevBookState state) {
        List<AbilityData> abilities = getSelectedAbilities(player);
        if (abilities.isEmpty()) {
            sb.append("Choose a style first to unlock ability testing.\n");
            return;
        }

        sb.append("Current style: ").append(currentStyleId(player)).append("\n");
        sb.append("Resource: ").append(mod.getResourceManager().getResourceDisplay(
                player.getPlayerId(),
                safe(player.getPlayerClass(), "")
        )).append("\n");
        sb.append("Actions:\n");

        for (int i = 0; i < abilities.size(); i++) {
            AbilityData ability = abilities.get(i);
            appendOption(sb, i == state.optionIndex, "Cast [" + (i + 1) + "] " + ability.getName());
        }

        int actionOffset = abilities.size();
        appendOption(sb, state.optionIndex == actionOffset, "Reset cooldowns");
        appendOption(sb, state.optionIndex == actionOffset + 1, "Refill class resources");
        appendOption(sb, state.optionIndex == actionOffset + 2, "Clear status effects + marks");
        appendOption(sb, state.optionIndex == actionOffset + 3, "Rebuild runtime");

        int selectedIndex = Math.min(state.optionIndex, abilities.size() - 1);
        if (state.optionIndex < abilities.size()) {
            AbilityData selectedAbility = abilities.get(selectedIndex);
            sb.append("\nSelected: ").append(selectedAbility.getName()).append("\n");
            sb.append("  ").append(compact(selectedAbility.getDescription(), 76)).append("\n");
        }
    }

    private void renderResourceDevOptions(PlayerData player, StringBuilder sb, DevBookState state) {
        List<String> options = getResourceOptionLabels(player);
        if (options.isEmpty()) {
            if ("aero".equalsIgnoreCase(player.getPlayerClass())) {
                sb.append("Aero has no class resource to test.\n");
                sb.append("Use Ability Lab for cooldown, movement, and momentum testing.\n");
            } else {
                sb.append("Choose a class first to unlock resource testing.\n");
            }
            return;
        }

        sb.append("Current resources: ")
                .append(mod.getResourceManager().getResourceDisplay(player.getPlayerId(), safe(player.getPlayerClass(), "")))
                .append("\n");
        if ("hydro".equalsIgnoreCase(player.getPlayerClass())) {
            sb.append("Container: ").append(mod.getResourceManager().getWaterContainerInfo(player.getPlayerId())).append("\n");
        }
        sb.append("Resource tools:\n");
        for (int i = 0; i < options.size(); i++) {
            appendOption(sb, i == state.optionIndex, options.get(i));
        }
    }

    private void renderLevelDevOptions(StringBuilder sb, DevBookState state) {
        sb.append("Level presets:\n");
        for (int i = 0; i < LEVEL_PRESETS.size(); i++) {
            appendOption(sb, i == state.optionIndex, "Set level " + LEVEL_PRESETS.get(i));
        }
    }

    private void renderXpDevOptions(PlayerData player, StringBuilder sb, DevBookState state) {
        int xpRequired = Math.max(1, mod.getLevelingManager().calculateXpRequired(player.getLevel()));
        sb.append("XP presets for current level (").append(xpRequired).append(" XP cap):\n");
        for (int i = 0; i < XP_PERCENT_PRESETS.size(); i++) {
            appendOption(sb, i == state.optionIndex, "Set XP to " + XP_PERCENT_PRESETS.get(i) + "%");
        }
    }

    private void renderPerkDevOptions(PlayerData player, StringBuilder sb, DevBookState state) {
        if (!hasPendingPerks(player)) {
            sb.append("No pending perk tier right now.\n");
            sb.append("Quick unlocks:\n");
            for (int i = 0; i < PERK_TIER_LEVELS.size(); i++) {
                appendOption(sb, i == state.optionIndex, "Set level " + PERK_TIER_LEVELS.get(i) + " for perk testing");
            }
            return;
        }

        List<Perk> available = mod.getPerkManager().getAvailablePerks(player);
        sb.append("Queued: ").append(queuedChoicesLine(state)).append("\n");
        sb.append("Use -> confirm once 3 perks are queued\n");
        for (int i = 0; i < available.size(); i++) {
            Perk perk = available.get(i);
            boolean queued = state.queuedPerkChoices.contains(i + 1);
            appendOption(
                    sb,
                    i == state.optionIndex,
                    "[" + (i + 1) + "] " + perk.getName() + (queued ? " *" : "")
            );
        }
    }

    private void renderUtilityDevOptions(StringBuilder sb, DevBookState state) {
        sb.append("Utility tools:\n");
        for (int i = 0; i < UTILITY_ACTIONS.size(); i++) {
            appendOption(sb, i == state.optionIndex, UTILITY_ACTIONS.get(i));
        }
    }

    private void renderResetDevOptions(StringBuilder sb, DevBookState state) {
        sb.append("Reset and utility tools:\n");
        for (int i = 0; i < RESET_ACTIONS.size(); i++) {
            appendOption(sb, i == state.optionIndex, RESET_ACTIONS.get(i).label());
        }
    }

    private void appendOption(StringBuilder sb, boolean selected, String label) {
        sb.append(selected ? "> " : "  ").append(label).append("\n");
    }

    private StringBuilder header(String bookName, String pageName, String statusMessage) {
        StringBuilder sb = new StringBuilder();
        sb.append("[MOTM ").append(bookName).append("] === ").append(pageName).append(" ===\n");
        if (statusMessage != null && !statusMessage.isBlank()) {
            sb.append("Last Action: ").append(statusMessage).append("\n");
        }
        if ("Spellbook".equals(bookName)) {
            sb.append("Open: /motm spellbook overview\n");
            sb.append("Combat: Left/Right/Use cast your 3 equipped spell slots\n");
            sb.append("Overview: Crouch + Use\n");
        } else if ("Dev Grimoire".equals(bookName)) {
            sb.append("Gesture: Use to open | Ability1/2/3 to navigate\n");
        }
        return sb;
    }

    private void appendChoiceBox(StringBuilder sb, String value) {
        sb.append("+--------------------------------------+\n");
        sb.append("| Current: ").append(padRight(value, 28)).append(" |\n");
        sb.append("+--------------------------------------+\n");
    }

    private SpellbookMode determineSpellbookMode(PlayerData player) {
        if (player.getPlayerClass() == null) {
            return SpellbookMode.CLASS_SELECT;
        }
        if (player.getRace() == null) {
            return SpellbookMode.RACE_SELECT;
        }
        if (player.getSelectedStyles() == null || player.getSelectedStyles().isEmpty()) {
            return SpellbookMode.STYLE_SELECT;
        }
        return SpellbookMode.SECTION_VIEW;
    }

    private SpellbookManager.Section currentSection(SpellbookState state) {
        return SPELLBOOK_SECTIONS.get(state.sectionIndex);
    }

    private DevPage currentDevPage(DevBookState state) {
        return DEV_PAGES.get(state.pageIndex);
    }

    private int devAbilityOptionCount(PlayerData player) {
        List<AbilityData> abilities = getSelectedAbilities(player);
        return abilities.isEmpty() ? 0 : abilities.size() + 4;
    }

    private int devResourceOptionCount(PlayerData player) {
        return getResourceOptionLabels(player).size();
    }

    private List<StyleData> getStyles(PlayerData player) {
        if (player.getPlayerClass() == null) {
            return Collections.emptyList();
        }
        return mod.getDataLoader().getStylesForClass(player.getPlayerClass());
    }

    private List<AbilityData> getSelectedAbilities(PlayerData player) {
        StyleData selectedStyle = getSelectedStyle(player);
        if (selectedStyle == null || selectedStyle.getAbilities() == null) {
            return Collections.emptyList();
        }
        return selectedStyle.getAbilities();
    }

    private List<String> getResourceOptionLabels(PlayerData player) {
        if (player.getPlayerClass() == null) {
            return Collections.emptyList();
        }

        String classId = player.getPlayerClass().toLowerCase(Locale.ROOT);
        List<String> options = new ArrayList<>();
        switch (classId) {
            case "hydro" -> {
                options.add("Fill water");
                options.add("Empty water");
                for (int tier = 0; tier < ResourceManager.WATER_CONTAINER_CAPACITY.length; tier++) {
                    options.add("Set container: " + ResourceManager.WATER_CONTAINER_NAMES[tier]);
                }
            }
            case "corruptus" -> {
                options.add("Fill souls");
                options.add("Empty souls");
                options.add("Add 10 souls");
            }
            case "terra" -> {
                String activeResource = getActiveStyleResourceType(player);
                options.add("Fill all Terra materials");
                options.add("Empty all Terra materials");
                if (activeResource != null) {
                    String label = mod.getResourceManager().getDisplayName(activeResource);
                    options.add("Add 25 " + label);
                    options.add("Clear " + label);
                }
            }
            default -> {
            }
        }
        return options;
    }

    private String getActiveStyleResourceType(PlayerData player) {
        StyleData style = getSelectedStyle(player);
        if (style != null && style.getResourceType() != null && !style.getResourceType().isBlank()) {
            return style.getResourceType();
        }
        return null;
    }

    private boolean hasPendingPerks(PlayerData player) {
        return mod.getPerkManager().hasPendingPerkSelection(player);
    }

    private String run(PlayerData player, String... commandArgs) {
        return mod.getMotmCommand().execute(player.getPlayerId(), commandArgs);
    }

    private String summarize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Action complete.";
        }

        String cleaned = raw.replace("[MOTM]", "").replace("\r", "");
        int newline = cleaned.indexOf('\n');
        if (newline >= 0) {
            cleaned = cleaned.substring(0, newline);
        }
        cleaned = cleaned.trim();
        return cleaned.isBlank() ? "Action complete." : cleaned;
    }

    private String queuedChoicesLine(DevBookState state) {
        if (state.queuedPerkChoices.isEmpty()) {
            return "none";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < state.queuedPerkChoices.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("[").append(state.queuedPerkChoices.get(i)).append("]");
        }
        return sb.toString();
    }

    private String joinAbilityNames(StyleData style) {
        if (style.getAbilities() == null || style.getAbilities().isEmpty()) {
            return "No abilities";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < style.getAbilities().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(style.getAbilities().get(i).getName());
        }
        return sb.toString();
    }

    private void fillResourcesForTesting(PlayerData player) {
        if (player.getPlayerClass() == null) {
            return;
        }
        mod.getResourceManager().fillClassResources(player.getPlayerId(), player.getPlayerClass());
        syncResourceState(player);
    }

    private void clearTransientEffects(PlayerData player) {
        mod.getStatusEffectManager().clearEffects(player.getPlayerId());
        mod.getElementalReactionManager().clearMarks(player.getPlayerId());
        mod.refreshStatusHud(player.getPlayerId());
    }

    private void syncResourceState(PlayerData player) {
        mod.getResourceManager().syncToPersistentState(player);
        mod.getPlayerDataManager().savePlayerData(player);
        mod.refreshStatusHud(player.getPlayerId());
    }

    private String applyBuildSnapshot(PlayerData player,
                                      String classId,
                                      String styleId,
                                      int level,
                                      boolean fillResources) {
        String result = summarize(run(player, "dev", "class", "set", classId));
        if (!result.toLowerCase(Locale.ROOT).contains("class set")) {
            return result;
        }

        result = summarize(run(player, "style", styleId));
        if (result.toLowerCase(Locale.ROOT).contains("invalid") || result.toLowerCase(Locale.ROOT).contains("failed")) {
            return result;
        }

        if (level > 0) {
            result = summarize(run(player, "dev", "level", "set", String.valueOf(level)));
        }

        if (fillResources) {
            fillResourcesForTesting(player);
        }

        clearTransientEffects(player);
        mod.getStyleManager().resetCooldowns(player.getPlayerId());
        mod.rebuildPlayerRuntime(player);
        mod.getPlayerDataManager().savePlayerData(player);
        return "Loaded " + classId + " / " + styleId + " at Lv " + Math.max(level, player.getLevel()) + ".";
    }

    private String currentStyleId(PlayerData player) {
        if (player.getSelectedStyles() == null || player.getSelectedStyles().isEmpty()) {
            return "No style";
        }
        return player.getSelectedStyles().get(0);
    }

    private StyleData getSelectedStyle(PlayerData player) {
        if (player.getPlayerClass() == null || player.getSelectedStyles() == null || player.getSelectedStyles().isEmpty()) {
            return null;
        }
        return mod.getDataLoader().getStyleById(player.getSelectedStyles().get(0), player.getPlayerClass());
    }

    private String currentAbilitySummary(PlayerData player) {
        List<AbilityData> abilities = getSelectedAbilities(player);
        if (abilities.isEmpty()) {
            return "No abilities";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < abilities.size(); i++) {
            if (i > 0) {
                sb.append(" | ");
            }
            sb.append(i + 1).append(":").append(compact(abilities.get(i).getName(), 12));
        }
        return sb.toString();
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String compact(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    private String padRight(String value, int width) {
        String safeValue = value == null ? "" : value;
        if (safeValue.length() >= width) {
            return safeValue.substring(0, width);
        }
        return safeValue + " ".repeat(width - safeValue.length());
    }

    private int wrap(int index, int size) {
        if (size <= 0) {
            return 0;
        }
        int wrapped = index % size;
        return wrapped < 0 ? wrapped + size : wrapped;
    }

    private enum SpellbookMode {
        CLASS_SELECT,
        RACE_SELECT,
        STYLE_SELECT,
        SECTION_VIEW
    }

    private enum DevPage {
        CLASS("Class Lab"),
        RACE("Race Lab"),
        STYLE("Style Lab"),
        AUTO_TEST("Auto Test"),
        ABILITIES("Ability Lab"),
        RESOURCES("Resource Lab"),
        LEVEL("Level Lab"),
        XP("XP Lab"),
        PERKS("Perk Lab"),
        UTILITY("Utility Lab"),
        RESET("Reset Lab");

        private final String displayName;

        DevPage(String displayName) {
            this.displayName = displayName;
        }
    }

    private static final class SpellbookState {
        private SpellbookMode mode = SpellbookMode.CLASS_SELECT;
        private int selectionIndex = 0;
        private int sectionIndex = 0;
        private String lastMessage = "";
    }

    private static final class DevBookState {
        private int pageIndex = 0;
        private int optionIndex = 0;
        private final List<Integer> queuedPerkChoices = new ArrayList<>();
        private String lastMessage = "";
        private boolean openedOnce = false;
    }

    private record ResetAction(String label, String[] command) {}
    private record AutoTestPreset(String label, String description, String classId, String styleId, int level, boolean fillResources) {}
}
