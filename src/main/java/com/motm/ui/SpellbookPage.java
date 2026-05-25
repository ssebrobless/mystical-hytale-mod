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
import com.motm.model.StyleData;
import com.motm.util.AbilityPresentation;
import com.motm.util.PassivePresentation;

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
    private static final int MAX_STYLE_BUTTONS = 10;
    private static final List<String> CLASS_ORDER = List.of("terra", "hydro", "aero", "corruptus");

    private final MenteesMod mod;
    private final List<Integer> queuedPerkChoices = new ArrayList<>();
    private SpellbookManager.Section currentSection;
    private String statusMessage = "";
    private String pendingClassId = null;
    private Integer pendingStyleSlot = null;

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
        bindClassActions(events);
        bindStyleActions(events);
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
            case "ConfirmClass" -> confirmClass();
            case "ChooseStyleSlot" -> chooseStyleSlot(data.value);
            case "ConfirmStyle" -> confirmStyle();
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
        bindSection(events, "#NavJourneyButton", SpellbookManager.Section.CLASS);
        bindSection(events, "#NavGrimoireButton", SpellbookManager.Section.ABILITIES);
        bindSection(events, "#NavPerksButton", SpellbookManager.Section.PERKS);
        bindSection(events, "#NavResourcesButton", SpellbookManager.Section.PROGRESSION);
    }

    private void bindClassActions(UIEventBuilder events) {
        bindAction(events, "#ClassTerraButton", "ChooseClass", "terra");
        bindAction(events, "#ClassHydroButton", "ChooseClass", "hydro");
        bindAction(events, "#ClassAeroButton", "ChooseClass", "aero");
        bindAction(events, "#ClassCorruptusButton", "ChooseClass", "corruptus");
        bindAction(events, "#ClassChooseButton", "ConfirmClass", "confirm");
    }

    private void bindStyleActions(UIEventBuilder events) {
        for (int index = 1; index <= MAX_STYLE_BUTTONS; index++) {
            bindAction(events, "#StyleButton" + index, "ChooseStyleSlot", String.valueOf(index));
        }
        bindAction(events, "#StyleChooseButton", "ConfirmStyle", "confirm");
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

        pendingClassId = classId.toLowerCase(Locale.ROOT);
        pendingStyleSlot = null;
        statusMessage = "Selected " + displayClassName(pendingClassId) + ". Press Choose to confirm.";
    }

    private void confirmClass() {
        if (pendingClassId == null || pendingClassId.isBlank()) {
            statusMessage = "Click a class first, then press Choose.";
            return;
        }

        runCommand("class", pendingClassId);

        PlayerData player = currentPlayer();
        if (player != null && pendingClassId.equalsIgnoreCase(player.getPlayerClass())) {
            queuedPerkChoices.clear();
            pendingClassId = null;
            currentSection = SpellbookManager.Section.STYLE;
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
        pendingStyleSlot = slot;
        statusMessage = "Selected " + safe(style.getName()) + ". Press Choose to confirm.";
    }

    private void confirmStyle() {
        PlayerData player = currentPlayer();
        if (player == null || player.getPlayerClass() == null) {
            statusMessage = "Choose a class first to unlock style selection.";
            return;
        }

        int slot = pendingStyleSlot != null ? pendingStyleSlot : -1;
        List<StyleData> styles = mod.getDataLoader().getStylesForClass(player.getPlayerClass());
        if (slot < 1 || slot > styles.size()) {
            statusMessage = "Click a style first, then press Choose.";
            return;
        }

        StyleData style = styles.get(slot - 1);
        runCommand("style", style.getId());

        player = currentPlayer();
        if (player != null && player.getSelectedStyles().contains(style.getId())) {
            pendingStyleSlot = null;
            currentSection = SpellbookManager.Section.ABILITIES;
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
            statusMessage = "You can queue only " + PerkManager.PERKS_TO_SELECT + " perk at a time. Confirm or clear first.";
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
            statusMessage = "Queue exactly " + PerkManager.PERKS_TO_SELECT + " perk before confirming.";
            return;
        }

        int beforeCount = player.getSelectedPerks().size();
        int beforeHistory = player.getPerkSelectionHistory().size();

        runCommand(
                "select",
                String.valueOf(queuedPerkChoices.get(0))
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
        applyClass(commands, player);
        applyAbilities(commands, player);
        applyPerks(commands, player);
        applyProgression(commands, player);
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
        setNavState(commands, SpellbookManager.Section.CLASS, "#NavJourneyButton", "#NavJourneySelected");
        setNavState(commands, SpellbookManager.Section.ABILITIES, "#NavGrimoireButton", "#NavGrimoireSelected");
        setNavState(commands, SpellbookManager.Section.PERKS, "#NavPerksButton", "#NavPerksSelected");
        setNavState(commands, SpellbookManager.Section.PROGRESSION, "#NavResourcesButton", "#NavResourcesSelected");
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
        commands.set("#JourneyPanel.Visible", currentSection == SpellbookManager.Section.CLASS);
        commands.set("#GrimoirePanel.Visible", currentSection == SpellbookManager.Section.STYLE
                || currentSection == SpellbookManager.Section.ABILITIES);
        commands.set("#PerksPanel.Visible", currentSection == SpellbookManager.Section.PERKS);
        commands.set("#ResourcesPanel.Visible", currentSection == SpellbookManager.Section.PROGRESSION);
    }

    private void applyHero(UICommandBuilder commands, PlayerData player) {
        setText(commands, "#SectionTitle.Text", sectionTitle(currentSection));
        setText(commands, "#SectionSubtitle.Text", sectionSubtitle(currentSection));
        setText(commands, "#HeroClassValue.Text", displayClass(player));
        setText(commands, "#HeroBuildValue.Text", "Style: " + displayStyle(player));
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
        setText(commands, "#OverviewIdentityBuildValue.Text", "Build identity is class + style.");
        setText(commands, "#OverviewIdentityStyleValue.Text", displayStyle(player));
        setText(commands, "#OverviewIdentityLevelValue.Text", buildXpLine(player));
        setText(commands, "#OverviewPathNextStepValue.Text", getNextStep(player));
        setText(commands, "#OverviewPathPendingPerksValue.Text", hasPendingPerks(player) ? "Yes" : "No");
        setText(commands, "#OverviewPathSynergyValue.Text", player != null
                ? String.valueOf(player.getActiveSynergyBonuses().size())
                : "0");
        setText(commands, "#OverviewPathResourceValue.Text", buildCastingAndStatsLine(player));
        setText(commands, "#OverviewPathTipValue.Text", "Choose a class, choose one style, use its three abilities, then shape the build with perks.");
    }

    private void applyClass(UICommandBuilder commands, PlayerData player) {
        ClassData classData = pendingClassId != null
                ? mod.getDataLoader().getClassData(pendingClassId)
                : getClassData(player);

        setText(commands, "#JourneyClassValue.Text", classData != null ? safe(classData.getDisplayName()) : displayClass(player));
        setText(commands, "#JourneyThemeValue.Text", classData != null ? safe(classData.getTheme()) : "Choose a class to define your path.");
        setText(commands, "#JourneyElementValue.Text", classData != null ? safe(classData.getElement()) : "Unchosen");
        setText(commands, "#JourneyPassiveNameValue.Text", classData != null && classData.getPassiveAbility() != null
                ? safe(classData.getPassiveAbility().getName())
                : "Unawakened");
        setText(commands, "#JourneyPassiveDescValue.Text", classData != null && classData.getPassiveAbility() != null
                ? compactText(buildPassiveDetails(classData), 220)
                : "Your class passive will appear here.");
        setText(commands, "#JourneyClassActionValue.Text", player != null && player.getPlayerClass() != null
                ? "Class is currently locked to " + displayClass(player) + ". Use dev reset or dev class clear if you need to change it while testing."
                : "Click once to read a class, then press Choose to confirm.");
        commands.set("#ClassChooseButton.Visible", player == null || player.getPlayerClass() == null);
        setText(commands, "#ClassChooseButton.Text", "Choose");
        applyClassButtons(commands, player);

        setText(commands, "#JourneyStyleIntroValue.Text", "Style");
        setText(commands, "#JourneyStyleRuleValue.Text", "Styles are the only source of active abilities.");
        setText(commands, "#JourneyAbilityRuleValue.Text", "Each style grants three cooldown-based abilities.");
        setText(commands, "#JourneyStyleActionValue.Text", "Choose a style on the Style page after selecting a class.");
        hideRemovedOptionButtons(commands);

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

    private void hideRemovedOptionButtons(UICommandBuilder commands) {
        for (int index = 0; index < 12; index++) {
            String selector = "#RemovedOptionButton" + (index + 1);
            commands.set(selector + ".Visible", false);
        }
    }

    private void applyAbilities(UICommandBuilder commands, PlayerData player) {
        StyleData style = getPreviewStyle(player);
        boolean hasClass = player != null && player.getPlayerClass() != null;
        boolean hasStyle = style != null;

        commands.set("#GrimoireEmpty.Visible", !hasClass);
        commands.set("#GrimoireDetails.Visible", hasClass);
        commands.set("#StyleButtonsContainer.Visible", hasClass);

        setText(commands, "#GrimoireEmpty.Text", "Choose a class first to unlock style abilities.");
        setText(commands, "#GrimoireStyleActionValue.Text", !hasClass
                ? "Choose a class first."
                : hasStyle
                ? "Click once to preview a style, then press Choose to confirm."
                : "Click once to read a style, then press Choose to confirm.");
        commands.set("#StyleChooseButton.Visible", hasClass);
        setText(commands, "#StyleChooseButton.Text", "Choose");

        if (hasClass) {
            applyStyleButtons(commands, player);
        } else {
            hideStyleButtons(commands);
        }

        setText(commands, "#GrimoireStyleValue.Text", hasStyle ? safe(style.getName()) : "Unchosen");
        setText(commands, "#GrimoireThemeValue.Text", hasStyle ? safe(style.getTheme()) : "No theme yet");
        setText(commands, "#GrimoireResourceValue.Text", hasStyle ? "Cooldown-based" : "Choose a style");
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
                    compactText(buildAbilityMeta(player, style, ability), 160));
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

        setText(commands, "#PerksDesignValue.Text", "Perks are passive modifiers, triggers, and synergies. Active abilities come only from your style.");
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
            statusText = "Perk choice " + perkManager().getPendingSelectionTier(player) + " is ready. Queue 1 perk, then confirm it here.";
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

    private void applyProgression(UICommandBuilder commands, PlayerData player) {
        setText(commands, "#ResourcesCurrentValue.Text", buildXpLine(player));
        setText(commands, "#ResourcesRuleValue.Text", buildStatTableLine(player));
        setText(commands, "#ResourcesPracticalValue.Text", buildStatBonusLine(player));
    }

    private String displayClass(PlayerData player) {
        if (player == null || player.getPlayerClass() == null) {
            return "Unchosen";
        }
        ClassData classData = getClassData(player);
        return classData != null ? safe(classData.getDisplayName()) : safe(player.getPlayerClass());
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

    private StyleData getSelectedStyle(PlayerData player) {
        if (player == null || player.getPlayerClass() == null || player.getSelectedStyles().isEmpty()) {
            return null;
        }
        return mod.getDataLoader().getStyleById(player.getSelectedStyles().get(0), player.getPlayerClass());
    }

    private StyleData getPreviewStyle(PlayerData player) {
        if (player == null || player.getPlayerClass() == null) {
            return null;
        }
        if (pendingStyleSlot != null) {
            List<StyleData> styles = mod.getDataLoader().getStylesForClass(player.getPlayerClass());
            int slot = pendingStyleSlot;
            if (slot >= 1 && slot <= styles.size()) {
                return styles.get(slot - 1);
            }
        }
        return getSelectedStyle(player);
    }

    private String displayClassName(String classId) {
        ClassData classData = classId == null ? null : mod.getDataLoader().getClassData(classId);
        return classData != null ? safe(classData.getDisplayName()) : safe(classId);
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
        return "Spend stat points and refine your build.";
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

    private String buildCastingAndStatsLine(PlayerData player) {
        if (player == null || player.getPlayerClass() == null) {
            return "Choose a class and style to unlock cooldown-based casting.";
        }
        return "Casting: no class resources. " + mod.getLevelingManager().describePlayerStatGrowth(player);
    }

    private String buildStatTableLine(PlayerData player) {
        if (player == null) {
            return "No stat table loaded.";
        }
        PlayerData.StatAllocation stats = player.getStatAllocation();
        return "Unspent " + player.getUnspentStatPoints()
                + " | Vigor " + stats.getVigor()
                + " | Tenacity " + stats.getTenacity()
                + " | Endurance " + stats.getEndurance()
                + " | Agility " + stats.getAgility()
                + " | Luck " + stats.getLuck();
    }

    private String buildStatBonusLine(PlayerData player) {
        if (player == null) {
            return "Each level grants 2 stat points through level 200. Perks unlock every 10 levels through level 100.";
        }
        return mod.getLevelingManager().describePlayerStatGrowth(player)
                + " | Spend with /motm stats spend <stat> [points].";
    }

    private String buildAbilitySummary(AbilityData ability) {
        String summary = AbilityPresentation.buildEffectSummary(ability);
        return summary.isBlank() ? compactText(ability.getDescription(), 90) : summary;
    }

    private String buildPassiveDetails(ClassData classData) {
        if (classData == null || classData.getPassiveAbility() == null) {
            return "Your class passive will appear here.";
        }
        String summary = PassivePresentation.buildPassiveSummary(classData.getPassiveAbility());
        if (summary.isBlank()) {
            return classData.getPassiveAbility().getDescription();
        }
        return classData.getPassiveAbility().getDescription() + " | " + summary;
    }

    private String buildAbilityMeta(PlayerData player, StyleData style, AbilityData ability) {
        StringBuilder meta = new StringBuilder();
        String profile = AbilityPresentation.buildSpatialSummary(ability);
        if (!profile.isBlank()) {
            meta.append(profile).append(" | ");
        }
        if (player != null && style != null && player.getPlayerClass() != null) {
            String visuals = AbilityPresentation.buildVisualSummary(player.getPlayerClass(), style.getId(), ability);
            if (!visuals.isBlank()) {
                meta.append(visuals).append(" | ");
            }
        }
        meta.append("Cooldown ").append(formatDecimal(ability.getCooldownSeconds())).append("s")
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
            case CLASS -> "Class";
            case STYLE -> "Style";
            case ABILITIES -> "Abilities";
            case PERKS -> "Perks";
            case PROGRESSION -> "Stats";
        };
    }

    private String sectionSubtitle(SpellbookManager.Section section) {
        return switch (section) {
            case OVERVIEW -> "Who you are, what you can do, and what comes next.";
            case CLASS -> "Your class passive and identity.";
            case STYLE -> "Choose the style that grants your three active abilities.";
            case ABILITIES -> "The three active abilities granted by your chosen style.";
            case PERKS -> "Passive modifiers, triggers, and synergies that shape your build.";
            case PROGRESSION -> "Spend stat points and review leveling bonuses.";
        };
    }

    private String sectionName(SpellbookManager.Section section) {
        return switch (section) {
            case OVERVIEW -> "overview";
            case CLASS -> "class";
            case STYLE -> "style";
            case ABILITIES -> "abilities";
            case PERKS -> "perks";
            case PROGRESSION -> "stats";
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
