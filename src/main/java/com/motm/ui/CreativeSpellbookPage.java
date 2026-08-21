package com.motm.ui;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.MenteesMod;
import com.motm.model.AbilityData;
import com.motm.model.ClassData;
import com.motm.model.Perk;
import com.motm.model.PlayerData;
import com.motm.model.StyleData;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dev-only creative sandbox spellbook. Lets a developer freely equip/unequip any class,
 * style, or perk, test-cast any ability, prepare a clean test arena, and restore the real
 * loadout afterward. All mutations route through the proven MotmCommand dev layer (which
 * bypasses production progression gating); this page is a UI over that layer and never
 * mutates PlayerData directly. Reuses {@link SpellbookPageEventData} (Action/Section/Value).
 *
 * Correctness rules honored:
 *  - bounded, predeclared element pools only (no runtime element creation);
 *  - no runtime writes to asset-bearing properties (Sprite.TexturePath / Group.Background);
 *    equipped/active state is shown via Label text + Visible only;
 *  - render() fully reconstructs visible state from PlayerData each call (idempotent).
 */
public class CreativeSpellbookPage extends InteractiveCustomUIPage<SpellbookPageEventData> {

    private static final String PAGE_DOCUMENT = "Pages/MOTM_CreativeSpellbook.ui";
    private static final List<String> CLASS_ORDER = List.of("terra", "hydro", "aero", "corruptus");
    private static final int STYLE_SLOTS = 10;
    private static final int ABILITY_CARDS = 3;
    private static final int PERK_ROWS = 10;

    private final MenteesMod mod;
    private final PlayerRef ref;

    private String currentTab = "classes";
    private String filterClassId = null;
    private String selectedStyleId = null;
    private int perksPage = 0;
    private boolean modeCreative = true;
    private String statusMessage = "";

    // In-memory snapshot of the real loadout, captured on open.
    private boolean hasSnapshot = false;
    private String snapClass = null;
    private List<String> snapStyles = new ArrayList<>();
    private List<String> snapPerks = new ArrayList<>();
    private int snapLevel = 0;

    public CreativeSpellbookPage(PlayerRef playerRef, MenteesMod mod) {
        super(playerRef, CustomPageLifetime.CanDismiss, SpellbookPageEventData.CODEC);
        this.mod = mod;
        this.ref = playerRef;
        captureSnapshot(currentPlayer());
    }

    @Override
    public void build(Ref<EntityStore> playerEntityRef,
                      UICommandBuilder commands,
                      UIEventBuilder events,
                      Store<EntityStore> store) {
        commands.append(PAGE_DOCUMENT);
        bindEvents(events);
        render(commands);
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> playerEntityRef,
                                Store<EntityStore> store,
                                SpellbookPageEventData data) {
        if (data == null || data.action == null || data.action.isBlank()) {
            return;
        }
        String value = data.value != null ? data.value : "";
        switch (data.action) {
            case "Navigate" -> currentTab = value.isBlank() ? currentTab : value;
            case "EquipClass" -> equipClass(value);
            case "ClearClass" -> { runCommand("dev", "class", "clear"); }
            case "FilterClass" -> { filterClassId = value; selectedStyleId = null; currentTab = "styles"; }
            case "EquipStyleSlot" -> equipStyleSlot(value);
            case "ClearStyles" -> { runCommand("dev", "styles", "clear"); }
            case "SelectStyleSlot" -> selectStyleSlot(value);
            case "TestAbilitySlot" -> testAbilitySlot(value);
            case "TogglePerkRow" -> togglePerkRow(value);
            case "PerksPage" -> changePerksPage(value);
            case "ClearPerks" -> { runCommand("dev", "perks", "clear"); }
            case "SpawnMobs" -> { runCommand("dev", "test", "mobs"); }
            case "ResetArena" -> { runCommand("dev", "test", "reset"); }
            case "TeleportFlat" -> { runCommand("dev", "relocate", "flatlands"); }
            case "ToggleFreeCast" -> toggleFreeCast();
            case "ToggleMode" -> toggleMode();
            case "ClearEffects" -> { runCommand("dev", "effects", "clear"); }
            case "ResetPlayer" -> { runCommand("dev", "reset"); }
            case "SaveSnapshot" -> { captureSnapshot(currentPlayer()); statusMessage = "Snapshot saved."; }
            case "RestoreLoadout" -> restoreLoadout();
            default -> statusMessage = "Unknown action: " + data.action;
        }
        UICommandBuilder commands = new UICommandBuilder();
        render(commands);
        sendUpdate(commands);
    }

    // ---- event bindings (registered once) ----

    private void bindEvents(UIEventBuilder events) {
        bind(events, "#NavClassesButton", "Navigate", "classes");
        bind(events, "#NavStylesButton", "Navigate", "styles");
        bind(events, "#NavAbilitiesButton", "Navigate", "abilities");
        bind(events, "#NavPerksButton", "Navigate", "perks");
        bind(events, "#NavTestButton", "Navigate", "test");
        bind(events, "#NavInfoButton", "Navigate", "info");

        for (int i = 0; i < CLASS_ORDER.size(); i++) {
            bind(events, "#Class" + (i + 1) + "EquipButton", "EquipClass", CLASS_ORDER.get(i));
        }
        bind(events, "#ClearClassButton", "ClearClass", "");

        bind(events, "#StyleFilterTerraButton", "FilterClass", "terra");
        bind(events, "#StyleFilterHydroButton", "FilterClass", "hydro");
        bind(events, "#StyleFilterAeroButton", "FilterClass", "aero");
        bind(events, "#StyleFilterCorruptusButton", "FilterClass", "corruptus");
        for (int i = 1; i <= STYLE_SLOTS; i++) {
            bind(events, "#Style" + i + "EquipButton", "EquipStyleSlot", String.valueOf(i));
        }
        bind(events, "#ClearStylesButton", "ClearStyles", "");

        for (int i = 1; i <= STYLE_SLOTS; i++) {
            bind(events, "#AbilStyle" + i + "Button", "SelectStyleSlot", String.valueOf(i));
        }
        for (int i = 1; i <= ABILITY_CARDS; i++) {
            bind(events, "#Abil" + i + "TestButton", "TestAbilitySlot", String.valueOf(i));
        }

        for (int i = 1; i <= PERK_ROWS; i++) {
            bind(events, "#Perk" + i + "ToggleButton", "TogglePerkRow", String.valueOf(i));
        }
        bind(events, "#PerksPrevButton", "PerksPage", "prev");
        bind(events, "#PerksNextButton", "PerksPage", "next");
        bind(events, "#ClearPerksButton", "ClearPerks", "");

        bind(events, "#SpawnMobsButton", "SpawnMobs", "");
        bind(events, "#ResetArenaButton", "ResetArena", "");
        bind(events, "#TeleportFlatButton", "TeleportFlat", "");
        bind(events, "#FreeCastButton", "ToggleFreeCast", "");
        bind(events, "#ModeButton", "ToggleMode", "");
        bind(events, "#ClearEffectsButton", "ClearEffects", "");
        bind(events, "#ResetPlayerButton", "ResetPlayer", "");
        bind(events, "#SaveSnapshotButton", "SaveSnapshot", "");
        bind(events, "#RestoreLoadoutButton", "RestoreLoadout", "");
    }

    private void bind(UIEventBuilder events, String selector, String action, String value) {
        events.addEventBinding(
                CustomUIEventBindingType.Activating,
                selector,
                new EventData().append("Action", action).append("Value", value != null ? value : ""),
                false
        );
    }

    // ---- actions ----

    private void equipClass(String classId) {
        if (classId == null || classId.isBlank() || !mod.getDataLoader().isValidClass(classId)) {
            statusMessage = "Invalid class.";
            return;
        }
        runCommand("dev", "class", "set", classId);
        filterClassId = classId;
        selectedStyleId = null;
    }

    private void equipStyleSlot(String rawSlot) {
        StyleData style = styleAtSlot(rawSlot);
        if (style == null) {
            statusMessage = "No style in that slot.";
            return;
        }
        runCommand("style", style.getId());
        selectedStyleId = style.getId();
    }

    private void selectStyleSlot(String rawSlot) {
        StyleData style = styleAtSlot(rawSlot);
        if (style != null) {
            selectedStyleId = style.getId();
            currentTab = "abilities";
        }
    }

    private void testAbilitySlot(String rawCard) {
        AbilityData ability = abilityAtCard(rawCard);
        if (ability == null) {
            statusMessage = "No ability in that slot.";
            return;
        }
        runCommand("dev", "test", "ability", ability.getId());
    }

    private void togglePerkRow(String rawRow) {
        Perk perk = perkAtRow(rawRow);
        if (perk == null) {
            statusMessage = "No perk in that row.";
            return;
        }
        PlayerData player = currentPlayer();
        List<String> owned = player != null ? new ArrayList<>(player.getSelectedPerks()) : new ArrayList<>();
        if (owned.contains(perk.getId())) {
            owned.remove(perk.getId());
            if (owned.isEmpty()) {
                runCommand("dev", "perks", "clear");
            } else {
                List<String> args = new ArrayList<>(List.of("dev", "perks", "set"));
                args.addAll(owned);
                runCommand(args.toArray(new String[0]));
            }
        } else {
            runCommand("dev", "perks", "grant", perk.getId());
        }
    }

    private void changePerksPage(String direction) {
        int pages = perkPageCount();
        if ("next".equalsIgnoreCase(direction)) {
            perksPage = Math.min(pages - 1, perksPage + 1);
        } else if ("prev".equalsIgnoreCase(direction)) {
            perksPage = Math.max(0, perksPage - 1);
        }
    }

    private void toggleFreeCast() {
        boolean enable = !mod.isFreeCastEnabled(ref.getUuid().toString());
        runCommand("dev", "freecast", enable ? "on" : "off");
    }

    private void toggleMode() {
        modeCreative = !modeCreative;
        runCommand("dev", "mode", modeCreative ? "creative" : "adventure");
    }

    private void restoreLoadout() {
        if (!hasSnapshot) {
            statusMessage = "No snapshot to restore.";
            return;
        }
        if (snapClass == null || snapClass.isBlank()) {
            runCommand("dev", "class", "clear");
        } else {
            runCommand("dev", "class", "set", snapClass);
        }
        if (snapStyles.isEmpty()) {
            runCommand("dev", "styles", "clear");
        } else {
            runCommand("style", snapStyles.get(0));
        }
        if (snapPerks.isEmpty()) {
            runCommand("dev", "perks", "clear");
        } else {
            List<String> args = new ArrayList<>(List.of("dev", "perks", "set"));
            args.addAll(snapPerks);
            runCommand(args.toArray(new String[0]));
        }
        runCommand("dev", "level", "set", String.valueOf(snapLevel));
        statusMessage = "Restored real loadout.";
    }

    private void captureSnapshot(PlayerData player) {
        if (player == null) {
            return;
        }
        snapClass = player.getPlayerClass();
        snapStyles = new ArrayList<>(player.getSelectedStyles());
        snapPerks = new ArrayList<>(player.getSelectedPerks());
        snapLevel = player.getLevel();
        hasSnapshot = true;
    }

    // ---- render ----

    private void render(UICommandBuilder commands) {
        PlayerData player = currentPlayer();
        if (filterClassId == null) {
            filterClassId = player != null && player.getPlayerClass() != null
                    ? player.getPlayerClass() : "terra";
        }
        renderNav(commands);
        renderHeader(commands, player);
        renderClasses(commands, player);
        renderStyles(commands, player);
        renderAbilities(commands, player);
        renderPerks(commands, player);
        renderTest(commands, player);
        renderInfo(commands);
        renderVisibility(commands);
    }

    private void renderNav(UICommandBuilder commands) {
        navState(commands, "classes", "#NavClassesButton", "#NavClassesSelected");
        navState(commands, "styles", "#NavStylesButton", "#NavStylesSelected");
        navState(commands, "abilities", "#NavAbilitiesButton", "#NavAbilitiesSelected");
        navState(commands, "perks", "#NavPerksButton", "#NavPerksSelected");
        navState(commands, "test", "#NavTestButton", "#NavTestSelected");
        navState(commands, "info", "#NavInfoButton", "#NavInfoSelected");
        setText(commands, "#ActionStatus.Text", statusMessage);
    }

    private void navState(UICommandBuilder commands, String tab, String button, String selected) {
        boolean active = currentTab.equals(tab);
        commands.set(button + ".Visible", !active);
        commands.set(selected + ".Visible", active);
    }

    private void renderHeader(UICommandBuilder commands, PlayerData player) {
        setText(commands, "#SectionTitle.Text", tabTitle(currentTab));
        setText(commands, "#SectionHint.Text", tabHint(currentTab));
        setText(commands, "#LoadoutClass.Text", "Class: " + classDisplay(player));
        setText(commands, "#LoadoutStyle.Text", "Style: " + styleDisplay(player));
        setText(commands, "#LoadoutPerks.Text", "Perks: " + (player != null ? player.getSelectedPerks().size() : 0));
        setText(commands, "#LoadoutLevel.Text", "Level: " + (player != null ? player.getLevel() : 0));
    }

    private void renderClasses(UICommandBuilder commands, PlayerData player) {
        String equipped = player != null ? player.getPlayerClass() : null;
        for (int i = 0; i < CLASS_ORDER.size(); i++) {
            String classId = CLASS_ORDER.get(i);
            ClassData data = mod.getDataLoader().getClassData(classId);
            String n = String.valueOf(i + 1);
            boolean isEquipped = classId.equalsIgnoreCase(equipped);
            setText(commands, "#Class" + n + "Name.Text", data != null ? data.getDisplayName() : classId);
            setText(commands, "#Class" + n + "Desc.Text", data != null
                    ? (safe(data.getElement()) + "  -  " + safe(data.getTheme())) : "");
            commands.set("#Class" + n + "Active.Visible", isEquipped);
            commands.set("#Class" + n + "EquipButton.Text", isEquipped ? "Re-equip" : "Equip");
        }
    }

    private void renderStyles(UICommandBuilder commands, PlayerData player) {
        List<StyleData> styles = stylesForFilter();
        String activeStyle = activeStyleId(player);
        setText(commands, "#StylesFilterLabel.Text", "Class filter: " + classDisplayId(filterClassId)
                + "  (10 styles)");
        for (int i = 1; i <= STYLE_SLOTS; i++) {
            String slot = "#Style" + i;
            if (i <= styles.size()) {
                StyleData s = styles.get(i - 1);
                boolean on = s.getId().equalsIgnoreCase(activeStyle);
                commands.set(slot + "Name.Visible", true);
                setText(commands, slot + "Name.Text", s.getName());
                commands.set(slot + "Active.Visible", on);
                commands.set(slot + "EquipButton.Visible", true);
                commands.set(slot + "EquipButton.Text", on ? "Active" : "Equip");
            } else {
                commands.set(slot + "Name.Visible", false);
                commands.set(slot + "Active.Visible", false);
                commands.set(slot + "EquipButton.Visible", false);
            }
        }
    }

    private void renderAbilities(UICommandBuilder commands, PlayerData player) {
        List<StyleData> styles = stylesForFilter();
        String viewStyle = viewStyleId(player, styles);
        setText(commands, "#AbilitiesStyleLabel.Text", "Styles in " + classDisplayId(filterClassId)
                + " (tap to inspect):");
        for (int i = 1; i <= STYLE_SLOTS; i++) {
            String sel = "#AbilStyle" + i + "Button";
            if (i <= styles.size()) {
                StyleData s = styles.get(i - 1);
                boolean selected = s.getId().equalsIgnoreCase(viewStyle);
                commands.set(sel + ".Visible", true);
                commands.set(sel + ".Text", (selected ? "> " : "") + s.getName());
            } else {
                commands.set(sel + ".Visible", false);
            }
        }
        StyleData style = viewStyle != null
                ? mod.getDataLoader().getStyleById(viewStyle, filterClassId) : null;
        List<AbilityData> abilities = style != null && style.getAbilities() != null
                ? style.getAbilities() : List.of();
        boolean hasStyle = style != null && !abilities.isEmpty();
        setText(commands, "#AbilitiesSelectedStyle.Text", style != null
                ? "Abilities of " + style.getName() : "");
        commands.set("#AbilitiesEmpty.Visible", !hasStyle);
        for (int i = 1; i <= ABILITY_CARDS; i++) {
            String card = "#AbilCard" + i;
            if (hasStyle && i <= abilities.size()) {
                AbilityData a = abilities.get(i - 1);
                commands.set(card + ".Visible", true);
                setText(commands, "#Abil" + i + "Name.Text", a.getName());
                setText(commands, "#Abil" + i + "Id.Text", a.getId());
                setText(commands, "#Abil" + i + "Desc.Text", safe(a.getDescription()));
                setText(commands, "#Abil" + i + "Meta.Text", "Cast: " + safe(a.getCastType()));
            } else {
                commands.set(card + ".Visible", false);
            }
        }
    }

    private void renderPerks(UICommandBuilder commands, PlayerData player) {
        List<Perk> pool = mod.getDataLoader().getSharedPerkPool();
        int pages = perkPageCount();
        if (perksPage >= pages) {
            perksPage = Math.max(0, pages - 1);
        }
        List<String> owned = player != null ? player.getSelectedPerks() : List.of();
        setText(commands, "#PerksHeader.Text", "Equip/unequip any perk freely (" + owned.size()
                + " equipped of " + pool.size() + ").");
        setText(commands, "#PerksPageLabel.Text", "Page " + (perksPage + 1) + "/" + pages);
        int base = perksPage * PERK_ROWS;
        for (int i = 1; i <= PERK_ROWS; i++) {
            String row = "#PerkRow" + i;
            int idx = base + (i - 1);
            if (idx < pool.size()) {
                Perk perk = pool.get(idx);
                boolean has = owned.contains(perk.getId());
                commands.set(row + ".Visible", true);
                setText(commands, "#Perk" + i + "Name.Text", perk.getName());
                setText(commands, "#Perk" + i + "Desc.Text", safe(perk.getDescription()));
                commands.set("#Perk" + i + "ToggleButton.Text", has ? "Unequip" : "Equip");
            } else {
                commands.set(row + ".Visible", false);
            }
        }
        setText(commands, "#PerksSynergy.Text", owned.isEmpty()
                ? "No perks equipped." : "Equipped: " + String.join(", ", owned));
    }

    private void renderTest(UICommandBuilder commands, PlayerData player) {
        boolean freecast = player != null && mod.isFreeCastEnabled(ref.getUuid().toString());
        commands.set("#FreeCastButton.Text", "Free-Cast: " + (freecast ? "ON" : "OFF"));
        commands.set("#ModeButton.Text", "Game Mode: " + (modeCreative ? "Creative" : "Adventure"));
        setText(commands, "#SnapshotLabel.Text", hasSnapshot
                ? "Snapshot: class=" + safe(snapClass) + ", styles=" + snapStyles.size()
                        + ", perks=" + snapPerks.size() + ", level=" + snapLevel
                : "No snapshot captured.");
        setText(commands, "#TestStatus.Text", statusMessage.isBlank()
                ? "Ready. Equip a style, spawn mobs, and Test-Cast abilities. Restore your loadout when done."
                : statusMessage);
    }

    private void renderInfo(UICommandBuilder commands) {
        int classes = mod.getDataLoader().getAllClasses().size();
        int perks = mod.getDataLoader().getSharedPerkPool().size();
        int styles = 0;
        int abilities = 0;
        for (String c : CLASS_ORDER) {
            List<StyleData> cs = mod.getDataLoader().getStylesForClass(c);
            styles += cs.size();
            for (StyleData s : cs) {
                abilities += s.getAbilities() != null ? s.getAbilities().size() : 0;
            }
        }
        setText(commands, "#InfoBody.Text",
                "A pure RPG-overlay mod: elemental classes, combat styles, active abilities, "
                        + "shared perks, and title-band mob scaling on top of Hytale's native systems. "
                        + "This creative sandbox is a dev tool for equipping and verifying content.");
        setText(commands, "#InfoCounts.Text", classes + " classes  -  " + styles + " styles  -  "
                + abilities + " abilities  -  " + perks + " perks");
        setText(commands, "#InfoLegend.Text",
                "HUD legend: elemental resource bars (bottom), XP bar + milestone, active ability "
                        + "slots (1-3), passive tracker rows, and buff/debuff status icons.");
        setText(commands, "#InfoVersion.Text", "Mentees of the Mystical - internal build");
    }

    private void renderVisibility(UICommandBuilder commands) {
        commands.set("#ClassesPanel.Visible", currentTab.equals("classes"));
        commands.set("#StylesPanel.Visible", currentTab.equals("styles"));
        commands.set("#AbilitiesPanel.Visible", currentTab.equals("abilities"));
        commands.set("#PerksPanel.Visible", currentTab.equals("perks"));
        commands.set("#TestPanel.Visible", currentTab.equals("test"));
        commands.set("#InfoPanel.Visible", currentTab.equals("info"));
    }

    // ---- helpers ----

    private PlayerData currentPlayer() {
        return mod.getPlayerDataManager().getOnlinePlayer(ref.getUuid().toString());
    }

    private String runCommand(String... args) {
        String playerId = ref.getUuid().toString();
        Player runtime = mod.getRuntimePlayer(playerId);
        String response = runtime != null
                ? mod.getMotmCommand().execute(runtime, args)
                : mod.getMotmCommand().execute(playerId, args);
        statusMessage = summarize(response);
        return response;
    }

    private List<StyleData> stylesForFilter() {
        List<StyleData> styles = mod.getDataLoader().getStylesForClass(filterClassId);
        return styles != null ? styles : List.of();
    }

    private StyleData styleAtSlot(String rawSlot) {
        int slot = parseInt(rawSlot);
        List<StyleData> styles = stylesForFilter();
        if (slot >= 1 && slot <= styles.size()) {
            return styles.get(slot - 1);
        }
        return null;
    }

    private AbilityData abilityAtCard(String rawCard) {
        int card = parseInt(rawCard);
        List<StyleData> styles = stylesForFilter();
        String viewStyle = viewStyleId(currentPlayer(), styles);
        StyleData style = viewStyle != null ? mod.getDataLoader().getStyleById(viewStyle, filterClassId) : null;
        if (style != null && style.getAbilities() != null
                && card >= 1 && card <= style.getAbilities().size()) {
            return style.getAbilities().get(card - 1);
        }
        return null;
    }

    private Perk perkAtRow(String rawRow) {
        int row = parseInt(rawRow);
        List<Perk> pool = mod.getDataLoader().getSharedPerkPool();
        int idx = perksPage * PERK_ROWS + (row - 1);
        if (row >= 1 && idx >= 0 && idx < pool.size()) {
            return pool.get(idx);
        }
        return null;
    }

    private String activeStyleId(PlayerData player) {
        if (player == null) {
            return null;
        }
        List<String> selected = player.getSelectedStyles();
        return selected != null && !selected.isEmpty() ? selected.get(0) : null;
    }

    private String viewStyleId(PlayerData player, List<StyleData> styles) {
        if (selectedStyleId != null) {
            for (StyleData s : styles) {
                if (s.getId().equalsIgnoreCase(selectedStyleId)) {
                    return selectedStyleId;
                }
            }
        }
        String active = activeStyleId(player);
        if (active != null) {
            for (StyleData s : styles) {
                if (s.getId().equalsIgnoreCase(active)) {
                    return active;
                }
            }
        }
        return styles.isEmpty() ? null : styles.get(0).getId();
    }

    private int perkPageCount() {
        int total = mod.getDataLoader().getSharedPerkPool().size();
        return Math.max(1, (total + PERK_ROWS - 1) / PERK_ROWS);
    }

    private String classDisplay(PlayerData player) {
        if (player == null || player.getPlayerClass() == null) {
            return "None";
        }
        return classDisplayId(player.getPlayerClass());
    }

    private String classDisplayId(String classId) {
        if (classId == null) {
            return "None";
        }
        ClassData data = mod.getDataLoader().getClassData(classId);
        return data != null ? data.getDisplayName() : classId;
    }

    private String styleDisplay(PlayerData player) {
        String active = activeStyleId(player);
        if (active == null) {
            return "None";
        }
        StyleData style = mod.getDataLoader().getStyleById(active,
                player != null ? player.getPlayerClass() : null);
        return style != null ? style.getName() : active;
    }

    private String tabTitle(String tab) {
        return switch (tab) {
            case "styles" -> "Styles";
            case "abilities" -> "Abilities";
            case "perks" -> "Perks";
            case "test" -> "Test Lab";
            case "info" -> "Info";
            default -> "Classes";
        };
    }

    private String tabHint(String tab) {
        return switch (tab) {
            case "styles" -> "Pick a class filter, then equip one active style (3 abilities).";
            case "abilities" -> "Inspect a style's 3 abilities and test-cast them.";
            case "perks" -> "Freely equip/unequip any shared perk.";
            case "test" -> "Prepare a clean arena and manage the test session.";
            case "info" -> "Mod overview, content counts, and HUD legend.";
            default -> "Equip any class instantly (bypasses progression gating).";
        };
    }

    private void setText(UICommandBuilder commands, String selector, String value) {
        commands.set(selector, safe(value));
    }

    private static String safe(String value) {
        return value != null ? value : "";
    }

    private static int parseInt(String raw) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException | NullPointerException e) {
            return -1;
        }
    }

    private static String summarize(String response) {
        if (response == null || response.isBlank()) {
            return "";
        }
        String line = response.split("\n", 2)[0];
        return line.replace("[MOTM]", "").trim();
    }
}
