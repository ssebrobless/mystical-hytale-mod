package com.motm.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.MenteesMod;
import com.motm.manager.LevelingManager;
import com.motm.manager.PerkManager;
import com.motm.manager.SpellbookManager;
import com.motm.model.AbilityData;
import com.motm.model.ClassData;
import com.motm.model.Perk;
import com.motm.model.PlayerData;
import com.motm.model.RaceData;
import com.motm.model.StyleData;
import com.motm.util.AbilityPresentation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Visual custom UI page for the spellbook.
 */
public class SpellbookPage extends InteractiveCustomUIPage<SpellbookPageEventData> {

    private static final String PAGE_DOCUMENT = "Pages/MOTM_Spellbook.ui";
    private static final int MAX_PERK_ROWS = 10;
    private static final int MAX_ABILITY_ROWS = 3;
    private static final int MAX_RACE_BUTTONS = 12;
    private static final int MAX_STYLE_BUTTONS = 10;
    private static final List<String> CLASS_ORDER = List.of("terra", "hydro", "aero", "corruptus");

    private final MenteesMod mod;
    private final List<Integer> queuedPerkChoices = new ArrayList<>();
    private SpellbookManager.Section currentSection;
    private String statusMessage = "";

    public SpellbookPage(PlayerRef playerRef, MenteesMod mod, SpellbookManager.Section initialSection) {
        super(playerRef, CustomPageLifetime.CanDismiss, SpellbookPageEventData.CODEC);
        this.mod = mod;
        this.currentSection = initialSection != null ? initialSection : SpellbookManager.Section.OVERVIEW;
    }

    @Override
    public void build(Ref<EntityStore> playerEntityRef,
                      UICommandBuilder commands,
                      UIEventBuilder events,
                      Store<EntityStore> store) {
        commands.append(PAGE_DOCUMENT);
        bindNavigation(events);
        bindJourneyActions(events);
        bindGrimoireActions(events);
        bindPerkActions(events);
        render(commands);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> playerEntityRef,
                                Store<EntityStore> store,
                                SpellbookPageEventData data) {
        if (data == null || data.action == null || data.action.isBlank()) {
            return;
        }

        switch (data.action) {
            case "Navigate" -> navigate(data.section);
            case "ChooseClass" -> chooseClass(data.value);
            case "ChooseRaceSlot" -> chooseRaceSlot(data.value);
            case "ChooseStyleSlot" -> chooseStyleSlot(data.value);
            case "TogglePerkSlot" -> togglePerkSlot(data.value);
            case "ConfirmPerks" -> confirmPerks();
            case "ClearPerks" -> clearQueuedPerks();
            default -> statusMessage = "Unknown spellbook action.";
        }

        UICommandBuilder commands = new UICommandBuilder();
        render(commands);
        sendUpdate(commands);
    }

    private void bindNavigation(UIEventBuilder events) {
        bindSection(events, "#NavOverviewButton", SpellbookManager.Section.OVERVIEW);
        bindSection(events, "#NavJourneyButton", SpellbookManager.Section.JOURNEY);
        bindSection(events, "#NavGrimoireButton", SpellbookManager.Section.GRIMOIRE);
        bindSection(events, "#NavPerksButton", SpellbookManager.Section.PERKS);
        bindSection(events, "#NavResourcesButton", SpellbookManager.Section.RESOURCES);
        bindSection(events, "#NavCodexButton", SpellbookManager.Section.CODEX);
        bindSection(events, "#NavJournalButton", SpellbookManager.Section.JOURNAL);
    }

    private void bindJourneyActions(UIEventBuilder events) {
        bindAction(events, "#ClassTerraButton", "ChooseClass", "terra");
        bindAction(events, "#ClassHydroButton", "ChooseClass", "hydro");
        bindAction(events, "#ClassAeroButton", "ChooseClass", "aero");
        bindAction(events, "#ClassCorruptusButton", "ChooseClass", "corruptus");

        for (int index = 1; index <= MAX_RACE_BUTTONS; index++) {
            bindAction(events, "#RaceButton" + index, "ChooseRaceSlot", String.valueOf(index));
        }
    }

    private void bindGrimoireActions(UIEventBuilder events) {
        for (int index = 1; index <= MAX_STYLE_BUTTONS; index++) {
            bindAction(events, "#StyleButton" + index, "ChooseStyleSlot", String.valueOf(index));
        }
    }

    private void bindPerkActions(UIEventBuilder events) {
        for (int index = 1; index <= MAX_PERK_ROWS; index++) {
            bindAction(events, "#PerkButton" + index, "TogglePerkSlot", String.valueOf(index));
        }
        bindAction(events, "#PerksConfirmButton", "ConfirmPerks", "confirm");
        bindAction(events, "#PerksClearButton", "ClearPerks", "clear");
    }

    private void bindSection(UIEventBuilder events, String selector, SpellbookManager.Section section) {
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                new EventData()
                        .append("Action", "Navigate")
                        .append("Section", sectionName(section)),
                false
        );
    }

    private void bindAction(UIEventBuilder events, String selector, String action, String value) {
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                new EventData()
                        .append("Action", action)
                        .append("Value", value != null ? value : ""),
                false
        );
    }

    private void navigate(String rawSection) {
        SpellbookManager.Section parsedSection = mod.getSpellbookManager().parseSection(rawSection);
        if (parsedSection != null) {
            currentSection = parsedSection;
        }
    }

    private void chooseClass(String classId) {
        if (classId == null || classId.isBlank()) {
            statusMessage = "Class selection is unavailable right now.";
            return;
        }

        runCommand("class", classId);

        PlayerData player = currentPlayer();
        if (player != null && classId.equalsIgnoreCase(player.getPlayerClass())) {
            queuedPerkChoices.clear();
            currentSection = player.getRace() == null
                    ? SpellbookManager.Section.JOURNEY
                    : SpellbookManager.Section.GRIMOIRE;
        }
    }

    private void chooseRaceSlot(String rawSlot) {
        PlayerData player = currentPlayer();
        int slot = parseSlot(rawSlot);
        List<RaceData> races = mod.getDataLoader().getAllRaces();
        if (player == null || slot < 1 || slot > races.size()) {
            statusMessage = "That race slot is not available.";
            return;
        }

        RaceData race = races.get(slot - 1);
        runCommand("race", race.getId());

        player = currentPlayer();
        if (player != null && race.getId().equalsIgnoreCase(player.getRace())) {
            currentSection = SpellbookManager.Section.GRIMOIRE;
        }
    }

    private void chooseStyleSlot(String rawSlot) {
        PlayerData player = currentPlayer();
        if (player == null || player.getPlayerClass() == null) {
            statusMessage = "Choose a class first to unlock style selection.";
            return;
        }

        int slot = parseSlot(rawSlot);
        List<StyleData> styles = mod.getDataLoader().getStylesForClass(player.getPlayerClass());
        if (slot < 1 || slot > styles.size()) {
            statusMessage = "That style slot is not available.";
            return;
        }

        StyleData style = styles.get(slot - 1);
        runCommand("style", style.getId());

        player = currentPlayer();
        if (player != null && player.getSelectedStyles().contains(style.getId())) {
            currentSection = SpellbookManager.Section.GRIMOIRE;
        }
    }

    private void togglePerkSlot(String rawSlot) {
        PlayerData player = currentPlayer();
        List<Perk> available = player != null && hasPendingPerks(player)
                ? perkManager().getAvailablePerks(player)
                : Collections.emptyList();
        int slot = parseSlot(rawSlot);
        if (slot < 1 || slot > available.size()) {
            statusMessage = "That perk slot is not available.";
            return;
        }

        if (queuedPerkChoices.contains(slot)) {
            queuedPerkChoices.remove(Integer.valueOf(slot));
            statusMessage = "Removed perk [" + slot + "] from the selection queue.";
            return;
        }

        if (queuedPerkChoices.size() >= PerkManager.PERKS_TO_SELECT) {
            statusMessage = "You can queue only 3 perks at a time. Confirm or clear first.";
            return;
        }

        queuedPerkChoices.add(slot);
        queuedPerkChoices.sort(Integer::compareTo);
        statusMessage = "Queued [" + slot + "] " + safe(available.get(slot - 1).getName()) + ".";
    }

    private void confirmPerks() {
        PlayerData player = currentPlayer();
        if (player == null || !hasPendingPerks(player)) {
            queuedPerkChoices.clear();
            statusMessage = "No pending perk tier is ready right now.";
            return;
        }

        if (queuedPerkChoices.size() != PerkManager.PERKS_TO_SELECT) {
            statusMessage = "Queue exactly 3 perks before confirming.";
            return;
        }

        int beforeCount = player.getSelectedPerks().size();
        int beforeHistory = player.getPerkSelectionHistory().size();

        runCommand(
                "select",
                String.valueOf(queuedPerkChoices.get(0)),
                String.valueOf(queuedPerkChoices.get(1)),
                String.valueOf(queuedPerkChoices.get(2))
        );

        player = currentPlayer();
        if (player != null
                && player.getSelectedPerks().size() > beforeCount
                && player.getPerkSelectionHistory().size() > beforeHistory) {
            queuedPerkChoices.clear();
            currentSection = SpellbookManager.Section.PERKS;
        }
    }

    private void clearQueuedPerks() {
        queuedPerkChoices.clear();
        statusMessage = "Cleared queued perk choices.";
    }

    private String runCommand(String... args) {
        String response = mod.getMotmCommand().execute(playerRef.getUuid().toString(), args);
        statusMessage = summarizeStatus(response);
        return response;
    }

    private void render(UICommandBuilder commands) {
        PlayerData player = currentPlayer();
        syncTransientUiState(player);

        applyNavigationState(commands);
        applyHero(commands, player);
        applyOverview(commands, player);
        applyJourney(commands, player);
        applyGrimoire(commands, player);
        applyPerks(commands, player);
        applyResources(commands, player);
        applyCodex(commands, player);
        applyJournal(commands, player);
        applySectionVisibility(commands);
    }

    private PlayerData currentPlayer() {
        return mod.getPlayerDataManager().getOnlinePlayer(playerRef.getUuid().toString());
    }

    private void syncTransientUiState(PlayerData player) {
        if (player == null || !hasPendingPerks(player)) {
            queuedPerkChoices.clear();
            return;
        }

        List<Perk> available = perkManager().getAvailablePerks(player);
        queuedPerkChoices.removeIf(choice -> choice < 1 || choice > available.size());
    }

    private void applyNavigationState(UICommandBuilder commands) {
        setNavState(commands, SpellbookManager.Section.OVERVIEW, "#NavOverviewButton", "#NavOverviewSelected");
        setNavState(commands, SpellbookManager.Section.JOURNEY, "#NavJourneyButton", "#NavJourneySelected");
        setNavState(commands, SpellbookManager.Section.GRIMOIRE, "#NavGrimoireButton", "#NavGrimoireSelected");
        setNavState(commands, SpellbookManager.Section.PERKS, "#NavPerksButton", "#NavPerksSelected");
        setNavState(commands, SpellbookManager.Section.RESOURCES, "#NavResourcesButton", "#NavResourcesSelected");
        setNavState(commands, SpellbookManager.Section.CODEX, "#NavCodexButton", "#NavCodexSelected");
        setNavState(commands, SpellbookManager.Section.JOURNAL, "#NavJournalButton", "#NavJournalSelected");
    }

    private void setNavState(UICommandBuilder commands,
                             SpellbookManager.Section section,
                             String buttonSelector,
                             String selectedSelector) {
        boolean selected = currentSection == section;
        commands.set(buttonSelector + ".Visible", !selected);
        commands.set(selectedSelector + ".Visible", selected);
        if (selected) {
            setText(commands, selectedSelector + ".Text", "> " + sectionTitle(section));
        }
    }

    private void applySectionVisibility(UICommandBuilder commands) {
        commands.set("#OverviewPanel.Visible", currentSection == SpellbookManager.Section.OVERVIEW);
        commands.set("#JourneyPanel.Visible", currentSection == SpellbookManager.Section.JOURNEY);
        commands.set("#GrimoirePanel.Visible", currentSection == SpellbookManager.Section.GRIMOIRE);
        commands.set("#PerksPanel.Visible", currentSection == SpellbookManager.Section.PERKS);
        commands.set("#ResourcesPanel.Visible", currentSection == SpellbookManager.Section.RESOURCES);
        commands.set("#CodexPanel.Visible", currentSection == SpellbookManager.Section.CODEX);
        commands.set("#JournalPanel.Visible", currentSection == SpellbookManager.Section.JOURNAL);
    }

    private void applyHero(UICommandBuilder commands, PlayerData player) {
        setText(commands, "#SectionTitle.Text", sectionTitle(currentSection));
        setText(commands, "#SectionSubtitle.Text", sectionSubtitle(currentSection));
        setText(commands, "#HeroClassValue.Text", displayClass(player));
        setText(commands, "#HeroRaceValue.Text", displayRace(player));
        setText(commands, "#HeroStyleValue.Text", displayStyle(player));
        setText(commands, "#HeroLevelValue.Text", player != null
                ? player.getLevel() + " / " + LevelingManager.MAX_LEVEL
                : "Unknown");
        setText(commands, "#HeroProgress.Text", buildXpLine(player));
        setText(commands, "#HeroNextStep.Text", "Next Step: " + getNextStep(player));
        setText(commands, "#ActionStatus.Text", statusMessage.isBlank() ? buildDefaultStatus(player) : statusMessage);
    }

    private void applyOverview(UICommandBuilder commands, PlayerData player) {
        setText(commands, "#OverviewIdentityClassValue.Text", displayClass(player));
        setText(commands, "#OverviewIdentityRaceValue.Text", displayRace(player));
        setText(commands, "#OverviewIdentityStyleValue.Text", displayStyle(player));
        setText(commands, "#OverviewIdentityLevelValue.Text", buildXpLine(player));
        setText(commands, "#OverviewPathNextStepValue.Text", getNextStep(player));
        setText(commands, "#OverviewPathPendingPerksValue.Text", hasPendingPerks(player) ? "Yes" : "No");
        setText(commands, "#OverviewPathSynergyValue.Text", player != null
                ? String.valueOf(player.getActiveSynergyBonuses().size())
                : "0");
        setText(commands, "#OverviewPathResourceValue.Text", currentResourceLine(player));
        setText(commands, "#OverviewPathTipValue.Text", "Journey chooses class and race. Grimoire chooses style. Perk Web confirms 3 passive picks.");
    }

    private void applyJourney(UICommandBuilder commands, PlayerData player) {
        ClassData classData = getClassData(player);
        RaceData raceData = getRaceData(player);

        setText(commands, "#JourneyClassValue.Text", displayClass(player));
        setText(commands, "#JourneyThemeValue.Text", classData != null ? safe(classData.getTheme()) : "Choose a class to define your path.");
        setText(commands, "#JourneyElementValue.Text", classData != null ? safe(classData.getElement()) : "Unchosen");
        setText(commands, "#JourneyPassiveNameValue.Text", classData != null && classData.getPassiveAbility() != null
                ? safe(classData.getPassiveAbility().getName())
                : "Unawakened");
        setText(commands, "#JourneyPassiveDescValue.Text", classData != null && classData.getPassiveAbility() != null
                ? compactText(classData.getPassiveAbility().getDescription(), 150)
                : "Your class passive will appear here.");
        setText(commands, "#JourneyClassActionValue.Text", player != null && player.getPlayerClass() != null
                ? "Class is currently locked to " + displayClass(player) + ". Use dev reset or dev class clear if you need to change it while testing."
                : "Choose your class here. This becomes your permanent path in normal play.");
        applyClassButtons(commands, player);

        setText(commands, "#JourneyRaceValue.Text", displayRace(player));
        setText(commands, "#JourneyRacePassiveValue.Text", raceData != null
                ? compactText(raceData.getPassive(), 120)
                : "Choose a race to gain identity bonuses.");
        setText(commands, "#JourneyRaceSpecialValue.Text", raceData != null
                ? compactText(raceData.getSpecial(), 120)
                : "Race specialties appear here.");
        setText(commands, "#JourneyRaceActionValue.Text", player != null && player.getRace() != null
                ? "Race is currently locked to " + displayRace(player) + ". Use dev race clear if you want to retest another one."
                : "Choose your race here. This is your long-term identity bonus.");
        applyRaceButtons(commands, player);

        setText(commands, "#JourneyStyleValue.Text", displayStyle(player));
        setText(commands, "#JourneyProgressValue.Text", player != null
                ? "Total XP: " + player.getTotalXpEarned() + " | Achievements: " + player.getAchievements().size()
                : "No journey data");
        setText(commands, "#JourneyMilestoneValue.Text", milestoneLine(player));
        setText(commands, "#JourneyPromptValue.Text", getNextStep(player));
    }

    private void applyClassButtons(UICommandBuilder commands, PlayerData player) {
        for (String classId : CLASS_ORDER) {
            String selector = "#Class" + toPascalCase(classId) + "Button.Text";
            ClassData classData = mod.getDataLoader().getClassData(classId);
            String label = classData != null ? safe(classData.getDisplayName()) : toPascalCase(classId);
            if (player != null && classId.equalsIgnoreCase(player.getPlayerClass())) {
                label += " *";
            }
            setText(commands, selector, label);
        }
    }

    private void applyRaceButtons(UICommandBuilder commands, PlayerData player) {
        List<RaceData> races = mod.getDataLoader().getAllRaces();
        for (int index = 0; index < MAX_RACE_BUTTONS; index++) {
            String selector = "#RaceButton" + (index + 1);
            boolean visible = index < races.size();
            commands.set(selector + ".Visible", visible);
            if (!visible) {
                continue;
            }

            RaceData race = races.get(index);
            String label = safe(race.getName());
            if (player != null && race.getId().equalsIgnoreCase(player.getRace())) {
                label += " *";
            }
            setText(commands, selector + ".Text", label);
        }
    }

    private void applyGrimoire(UICommandBuilder commands, PlayerData player) {
        StyleData style = getSelectedStyle(player);
        boolean hasClass = player != null && player.getPlayerClass() != null;
        boolean hasStyle = style != null;

        commands.set("#GrimoireEmpty.Visible", !hasClass);
        commands.set("#GrimoireDetails.Visible", hasClass);
        commands.set("#StyleButtonsContainer.Visible", hasClass);

        setText(commands, "#GrimoireEmpty.Text", "Choose a class in Journey to awaken your grimoire.");
        setText(commands, "#GrimoireStyleActionValue.Text", !hasClass
                ? "Choose a class first."
                : hasStyle
                ? "Choose a different style here if you want to reshuffle your 3 active abilities."
                : "Choose a style here. Styles grant all 3 active abilities immediately.");

        if (hasClass) {
            applyStyleButtons(commands, player);
        } else {
            hideStyleButtons(commands);
        }

        setText(commands, "#GrimoireStyleValue.Text", hasStyle ? safe(style.getName()) : "Unchosen");
        setText(commands, "#GrimoireThemeValue.Text", hasStyle ? safe(style.getTheme()) : "No theme yet");
        setText(commands, "#GrimoireResourceValue.Text", hasStyle ? safe(style.getResourceType()) : "No class resource");
        setText(commands, "#GrimoireAbilityRule.Text", "Styles grant all 3 active abilities immediately. Perks stay passive and modify those abilities later.");

        List<AbilityData> abilities = hasStyle && style.getAbilities() != null ? style.getAbilities() : Collections.emptyList();
        for (int index = 0; index < MAX_ABILITY_ROWS; index++) {
            String prefix = "#AbilityCard" + (index + 1);
            boolean visible = index < abilities.size();
            commands.set(prefix + ".Visible", visible);

            if (!visible) {
                continue;
            }

            AbilityData ability = abilities.get(index);
            setText(commands, "#Ability" + (index + 1) + "Name.Text", safe(ability.getName()));
            setText(commands, "#Ability" + (index + 1) + "Id.Text", safe(ability.getId()));
            setText(commands, "#Ability" + (index + 1) + "Summary.Text",
                    compactText(buildAbilitySummary(ability), 110));
            setText(commands, "#Ability" + (index + 1) + "Meta.Text",
                    compactText(buildAbilityMeta(player, ability), 140));
        }
    }

    private void applyStyleButtons(UICommandBuilder commands, PlayerData player) {
        List<StyleData> styles = mod.getDataLoader().getStylesForClass(player.getPlayerClass());
        for (int index = 0; index < MAX_STYLE_BUTTONS; index++) {
            String selector = "#StyleButton" + (index + 1);
            boolean visible = index < styles.size();
            commands.set(selector + ".Visible", visible);
            if (!visible) {
                continue;
            }

            StyleData style = styles.get(index);
            String label = safe(style.getName());
            if (player.getSelectedStyles().contains(style.getId())) {
                label += " *";
            }
            setText(commands, selector + ".Text", label);
        }
    }

    private void hideStyleButtons(UICommandBuilder commands) {
        for (int index = 1; index <= MAX_STYLE_BUTTONS; index++) {
            commands.set("#StyleButton" + index + ".Visible", false);
        }
    }

    private void applyPerks(UICommandBuilder commands, PlayerData player) {
        boolean hasClass = player != null && player.getPlayerClass() != null;
        boolean hasStyle = player != null && !player.getSelectedStyles().isEmpty();
        boolean pending = hasPendingPerks(player);

        setText(commands, "#PerksDesignValue.Text", "Perks are passive modifiers, triggers, and synergies. They never grant active abilities.");
        setText(commands, "#PerksUnlockedValue.Text", player != null
                ? perkManager().getCurrentTier(player.getLevel()) + " / " + PerkManager.TOTAL_TIERS
                : "0 / " + PerkManager.TOTAL_TIERS);
        setText(commands, "#PerksOwnedValue.Text", player != null
                ? player.getSelectedPerks().size() + " / " + PerkManager.MAX_TOTAL_PERKS
                : "0 / " + PerkManager.MAX_TOTAL_PERKS);
        setText(commands, "#PerksSynergyValue.Text", player != null
                ? String.valueOf(player.getActiveSynergyBonuses().size())
                : "0");

        String statusText;
        if (!hasClass) {
            statusText = "Choose a class first to begin your build.";
        } else if (!hasStyle) {
            statusText = "Choose a style first. Styles give abilities; perks reshape them.";
        } else if (pending) {
            statusText = "Tier " + perkManager().getPendingSelectionTier(player) + " is ready. Queue 3 perks, then confirm them here.";
        } else {
            statusText = nextPerkUnlockLine(player);
        }
        setText(commands, "#PerksStatusValue.Text", statusText);

        List<Perk> available = pending ? perkManager().getAvailablePerks(player) : Collections.emptyList();
        boolean hasVisibleRows = false;
        for (int index = 0; index < MAX_PERK_ROWS; index++) {
            String prefix = "#PerkRow" + (index + 1);
            boolean visible = index < available.size();
            commands.set(prefix + ".Visible", visible);
            if (!visible) {
                continue;
            }

            hasVisibleRows = true;
            Perk perk = available.get(index);
            boolean queued = queuedPerkChoices.contains(index + 1);
            setText(commands, "#Perk" + (index + 1) + "Index.Text", "[" + (index + 1) + "]");
            setText(commands, "#Perk" + (index + 1) + "Name.Text", safe(perk.getName()) + (queued ? " *" : ""));
            setText(commands, "#Perk" + (index + 1) + "Desc.Text", compactText(perk.getDescription(), 140));
            setText(commands, "#PerkButton" + (index + 1) + ".Text", queued ? "Queued" : "Queue");
        }

        commands.set("#PerksChoicesContainer.Visible", hasVisibleRows);
        commands.set("#PerksControls.Visible", hasVisibleRows);
        commands.set("#PerksEmpty.Visible", !hasVisibleRows);
        setText(commands, "#PerksEmpty.Text", hasVisibleRows
                ? ""
                : "No pending perk choices right now. Reach the next milestone to open another tier.");
        setText(commands, "#PerksSelectionValue.Text", queuedChoiceLine());
    }

    private void applyResources(UICommandBuilder commands, PlayerData player) {
        setText(commands, "#ResourcesCurrentValue.Text", currentResourceLine(player));
        setText(commands, "#ResourcesRuleValue.Text", player != null && player.getPlayerClass() != null
                ? resourceRule(player.getPlayerClass())
                : "Choose a class to unlock its resource system.");
        setText(commands, "#ResourcesPracticalValue.Text",
                "Practical perks can improve sustain, movement, gathering, healing, and survival while playing Hytale normally.");
    }

    private void applyCodex(UICommandBuilder commands, PlayerData player) {
        setText(commands, "#CodexFlowValue.Text", "Class -> Style -> 3 abilities -> Level milestones -> Perk tiers -> Synergies");
        setText(commands, "#CodexReactionsValue.Text", "Elemental reactions and status marks will live here as the combat execution layer grows.");
        setText(commands, "#CodexScalingValue.Text", "Enemies scale with progression and may gain elite titles based on zone and conditions.");
        setText(commands, "#CodexFocusValue.Text", "Current focus: spellbook UI, passive perk execution, and stronger ability runtime hooks.");
    }

    private void applyJournal(UICommandBuilder commands, PlayerData player) {
        setText(commands, "#JournalStatusValue.Text", "Story pages are reserved for Mentees lore, discoveries, factions, and future quests.");
        setText(commands, "#JournalPathValue.Text", "Current Path: " + displayClass(player) + " / " + displayRace(player) + " / " + displayStyle(player));
        setText(commands, "#JournalFutureValue.Text", "Planned chapters: origin notes, class-specific story beats, race lore, and a discovery log.");
    }

    private String displayClass(PlayerData player) {
        if (player == null || player.getPlayerClass() == null) {
            return "Unchosen";
        }
        ClassData classData = getClassData(player);
        return classData != null ? safe(classData.getDisplayName()) : safe(player.getPlayerClass());
    }

    private String displayRace(PlayerData player) {
        if (player == null || player.getRace() == null) {
            return "Unchosen";
        }
        RaceData raceData = getRaceData(player);
        return raceData != null ? safe(raceData.getName()) : safe(player.getRace());
    }

    private String displayStyle(PlayerData player) {
        StyleData style = getSelectedStyle(player);
        return style != null ? safe(style.getName()) : "Unchosen";
    }

    private String queuedChoiceLine() {
        if (queuedPerkChoices.isEmpty()) {
            return "Queued Choices: none";
        }

        StringBuilder line = new StringBuilder("Queued Choices: ");
        for (int index = 0; index < queuedPerkChoices.size(); index++) {
            if (index > 0) {
                line.append(", ");
            }
            line.append("[").append(queuedPerkChoices.get(index)).append("]");
        }
        line.append(" / ").append(PerkManager.PERKS_TO_SELECT);
        return line.toString();
    }

    private String buildDefaultStatus(PlayerData player) {
        return "Spellbook ready. " + getNextStep(player);
    }

    private String summarizeStatus(String rawMessage) {
        String cleaned = safe(rawMessage)
                .replace("[MOTM]", "")
                .replaceAll("\\s*\\n\\s*", " | ")
                .replaceAll("\\s{2,}", " ")
                .trim();
        return cleaned.isEmpty() ? "Action complete." : compactText(cleaned, 240);
    }

    private int parseSlot(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return -1;
        }

        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private ClassData getClassData(PlayerData player) {
        if (player == null || player.getPlayerClass() == null) {
            return null;
        }
        return mod.getDataLoader().getClassData(player.getPlayerClass());
    }

    private RaceData getRaceData(PlayerData player) {
        if (player == null || player.getRace() == null) {
            return null;
        }
        return mod.getDataLoader().getRaceById(player.getRace());
    }

    private StyleData getSelectedStyle(PlayerData player) {
        if (player == null || player.getPlayerClass() == null || player.getSelectedStyles().isEmpty()) {
            return null;
        }
        return mod.getDataLoader().getStyleById(player.getSelectedStyles().get(0), player.getPlayerClass());
    }

    private String buildXpLine(PlayerData player) {
        if (player == null) {
            return "No player data loaded";
        }
        int required = mod.getLevelingManager().calculateXpRequired(player.getLevel());
        return "Lv " + player.getLevel() + " | XP " + player.getCurrentXp() + " / " + required;
    }

    private String getNextStep(PlayerData player) {
        if (player == null) {
            return "Player data unavailable.";
        }
        if (player.getPlayerClass() == null) {
            return "Choose your class.";
        }
        if (player.getRace() == null) {
            return "Choose your race.";
        }
        if (player.getSelectedStyles().isEmpty()) {
            return "Choose your style.";
        }
        if (hasPendingPerks(player)) {
            return "Choose perk tier " + perkManager().getPendingSelectionTier(player) + ".";
        }

        int nextTier = perkManager().getCurrentTier(player.getLevel()) + 1;
        if (nextTier <= PerkManager.TOTAL_TIERS) {
            return "Reach level " + (nextTier * LevelingManager.MILESTONE_INTERVAL) + " for the next perk tier.";
        }
        return "Refine your build and chase synergies.";
    }

    private String milestoneLine(PlayerData player) {
        if (player == null) {
            return "No milestone data";
        }
        if (hasPendingPerks(player)) {
            return "Pending perk tier: " + perkManager().getPendingSelectionTier(player);
        }
        return nextPerkUnlockLine(player);
    }

    private String nextPerkUnlockLine(PlayerData player) {
        if (player == null) {
            return "No perk milestone data";
        }
        int currentTier = perkManager().getCurrentTier(player.getLevel());
        int nextTier = currentTier + 1;
        if (nextTier > PerkManager.TOTAL_TIERS) {
            return "All perk tiers unlocked.";
        }
        return "Next perk tier unlocks at level " + (nextTier * LevelingManager.MILESTONE_INTERVAL) + ".";
    }

    private boolean hasPendingPerks(PlayerData player) {
        return player != null && perkManager().hasPendingPerkSelection(player);
    }

    private String currentResourceLine(PlayerData player) {
        if (player == null || player.getPlayerClass() == null) {
            return "No class resource yet.";
        }
        return mod.getResourceManager().getResourceDisplay(player.getPlayerId(), player.getPlayerClass());
    }

    private String resourceRule(String playerClass) {
        if (playerClass == null) {
            return "Choose a class to unlock a resource rule.";
        }
        return switch (playerClass.toLowerCase(Locale.ROOT)) {
            case "terra" -> "Terra uses gathered materials and practical terrain tools.";
            case "hydro" -> "Hydro spends water and refills from containers and natural sources.";
            case "aero" -> "Aero regenerates TP over time for fast, mobile casting.";
            case "corruptus" -> "Corruptus earns souls from kills and burns them for power.";
            default -> "Unknown class resource.";
        };
    }

    private String buildAbilitySummary(AbilityData ability) {
        String summary = AbilityPresentation.buildEffectSummary(ability);
        return summary.isBlank() ? compactText(ability.getDescription(), 90) : summary;
    }

    private String buildAbilityMeta(PlayerData player, AbilityData ability) {
        StringBuilder meta = new StringBuilder();
        String profile = AbilityPresentation.buildSpatialSummary(ability);
        if (!profile.isBlank()) {
            meta.append(profile).append(" | ");
        }
        meta.append("Cost ").append(ability.getResourceCost())
                .append(" | Cooldown ").append(formatDecimal(ability.getCooldownSeconds())).append("s")
                .append(" | Ready in ").append(formatDecimal(getRemainingCooldown(player, ability))).append("s");
        return meta.toString();
    }

    private double getRemainingCooldown(PlayerData player, AbilityData ability) {
        if (player == null || ability == null) {
            return 0;
        }
        return mod.getStyleManager().getRemainingCooldownSeconds(player.getPlayerId(), ability.getId());
    }

    private PerkManager perkManager() {
        return mod.getPerkManager();
    }

    private String sectionTitle(SpellbookManager.Section section) {
        return switch (section) {
            case OVERVIEW -> "Overview";
            case JOURNEY -> "Journey";
            case GRIMOIRE -> "Grimoire";
            case PERKS -> "Perk Web";
            case RESOURCES -> "Resources";
            case CODEX -> "Codex";
            case JOURNAL -> "Journal";
        };
    }

    private String sectionSubtitle(SpellbookManager.Section section) {
        return switch (section) {
            case OVERVIEW -> "Who you are, what you can do, and what comes next.";
            case JOURNEY -> "Your class, race, style, and progression milestones.";
            case GRIMOIRE -> "The three active abilities granted by your chosen style.";
            case PERKS -> "Passive modifiers, triggers, and synergies that shape your build.";
            case RESOURCES -> "How your class sustains spellcasting and practical power.";
            case CODEX -> "Core system notes for reactions, scaling, and combat flow.";
            case JOURNAL -> "Reserved space for lore, chapters, quests, and discoveries.";
        };
    }

    private String sectionName(SpellbookManager.Section section) {
        return switch (section) {
            case OVERVIEW -> "overview";
            case JOURNEY -> "journey";
            case GRIMOIRE -> "grimoire";
            case PERKS -> "perks";
            case RESOURCES -> "resources";
            case CODEX -> "codex";
            case JOURNAL -> "journal";
        };
    }

    private void setText(UICommandBuilder commands, String selector, String value) {
        commands.set(selector, safe(value));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String compactText(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, Math.max(0, maxLength - 3)).trim() + "...";
    }

    private String formatDecimal(double value) {
        return AbilityPresentation.formatDecimal(value);
    }

    private String toPascalCase(String rawValue) {
        StringBuilder builder = new StringBuilder();
        for (String part : rawValue.split("_")) {
            if (part.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                builder.append(part.substring(1));
            }
        }
        return builder.toString();
    }
}
