package com.motm.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
import com.hypixel.hytale.server.core.inventory.InventoryComponent;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.motm.MenteesMod;
import com.motm.model.AbilityData;
import com.motm.model.PlayerData;
import com.motm.model.StatusEffect;
import com.motm.model.StyleData;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Persistent in-game HUD overlay for MOTM progression, class resources, and
 * always-on passive/buff/debuff state.
 */
public class MotmStatusHud extends CustomUIHud {

    private static final String HUD_DOCUMENT = "HUD/MOTM_StatusHud.ui";
    private static final int MAX_BUFF_SLOTS = 3;
    private static final int MAX_DEBUFF_SLOTS = 3;
    private static final int MAX_TRACKER_ROWS = 12;
    private static final int TICKS_PER_SECOND = 20;
    private static final String TERRA_REGEN_ICON = "Common/UI/StatusEffects/HealthRegen.png";
    private static final String MOVEMENT_ICON = "Common/UI/StatusEffects/Stamina.png";
    private static final String CORRUPTUS_ICON = "Common/UI/StatusEffects/Poison.png";
    private static final String HYDRO_BARRIER_ICON = "Assets/StatusEffects/Icons/ShieldAbility@2x.png";

    private final MenteesMod mod;

    public MotmStatusHud(PlayerRef playerRef, MenteesMod mod) {
        super(playerRef, "motm_status_hud");
        this.mod = mod;
    }

    @Override
    protected void build(UICommandBuilder commands) {
        commands.append(HUD_DOCUMENT);
        render(commands);
    }

    public void refresh() {
        UICommandBuilder commands = new UICommandBuilder();
        render(commands);
        update(false, commands);
    }

    private void render(UICommandBuilder commands) {
        PlayerData player = currentPlayer();
        renderStatusStrip(commands, player);
        renderPassiveTracker(commands, player);
        renderXp(commands, player);
        renderResource(commands, player);
        renderAbilitySlots(commands, player);
    }

    private void renderStatusStrip(UICommandBuilder commands, PlayerData player) {
        String primaryLine = buildClassStyleLine(player);
        boolean visible = !primaryLine.isBlank();

        commands.set("#StatusRoot.Visible", visible);
        setText(commands, "#StatusLine1.Text", primaryLine);
        setText(commands, "#StatusLine2.Text", "");
        commands.set("#StatusLine1.Visible", !primaryLine.isBlank());
        commands.set("#StatusLine2.Visible", false);

        // Hide legacy icon-slot widgets; passive/perk readiness now lives in the top-right tracker.
        renderStatusSlots(commands, "BuffStatus", List.of(), MAX_BUFF_SLOTS);
        renderStatusSlots(commands, "DebuffStatus", List.of(), MAX_DEBUFF_SLOTS);
    }

    private String buildClassStyleLine(PlayerData player) {
        if (player == null || player.getPlayerClass() == null || player.getPlayerClass().isBlank()) {
            return "";
        }
        return displayClassName(player.getPlayerClass()) + ": " + displaySelectedStyleName(player);
    }

    private String displayClassName(String classId) {
        if (classId == null || classId.isBlank()) {
            return "No Class";
        }
        return titleCase(classId);
    }

    private String displaySelectedStyleName(PlayerData player) {
        StyleData style = getSelectedStyle(player);
        if (style == null) {
            return "No Style";
        }
        if (style.getName() != null && !style.getName().isBlank()) {
            return style.getName();
        }
        return titleCase(style.getId());
    }

    private void renderPassiveTracker(UICommandBuilder commands, PlayerData player) {
        List<TrackerEntry> entries = buildPassiveTrackerEntries(player);
        commands.set("#PassiveTrackerRoot.Visible", !entries.isEmpty());

        for (int index = 1; index <= MAX_TRACKER_ROWS; index++) {
            TrackerEntry entry = index <= entries.size() ? entries.get(index - 1) : null;
            renderTrackerRow(commands, index, entry);
        }
    }

    private List<TrackerEntry> buildPassiveTrackerEntries(PlayerData player) {
        if (player == null || player.getPlayerId() == null || player.getPlayerClass() == null) {
            return List.of();
        }

        List<TrackerEntry> entries = new ArrayList<>();
        appendClassPassiveTrackerEntries(player, entries);

        var runtimePerks = mod.getRuntimePerkManager();
        if (runtimePerks != null) {
            for (var perkEntry : runtimePerks.getHudEntries(player)) {
                entries.add(new TrackerEntry(
                        perkEntry.name(),
                        perkEntry.cooldownSeconds(),
                        perkEntry.active(),
                        perkEntry.iconPath(),
                        perkEntry.framePath(),
                        perkEntry.state(),
                        perkEntry.counterText(),
                        true
                ));
            }
        }
        return entries;
    }

    private void appendClassPassiveTrackerEntries(PlayerData player, List<TrackerEntry> entries) {
        String playerId = player.getPlayerId();
        String classId = safeLower(player.getPlayerClass());
        var passiveManager = mod.getClassPassiveManager();

        switch (classId) {
            case "terra" -> {
                entries.add(new TrackerEntry(
                        "Immovable",
                        0.0,
                        passiveManager.isTerraShieldPrimed(playerId),
                        TERRA_REGEN_ICON,
                        null,
                        passiveManager.isTerraShieldPrimed(playerId) ? "ACTIVE" : "READY",
                        "",
                        false
                ));
                entries.add(new TrackerEntry(
                        "Miner's Affinity",
                        0.0,
                        passiveManager.isTerraMiningAffinityActive(playerId),
                        MOVEMENT_ICON,
                        null,
                        passiveManager.isTerraMiningAffinityActive(playerId) ? "ACTIVE" : "READY",
                        "",
                        false
                ));
                entries.add(new TrackerEntry("Cave Vision", 0.0, passiveManager.isTerraCaveVisionActive(playerId)));
            }
            case "hydro" -> {
                boolean swimming = passiveManager.isHydroSwimming(playerId);
                boolean underwater = passiveManager.isHydroUnderwater(playerId);
                double barrierHp = passiveManager.getHydroAquaBarrierShieldHp(playerId);
                double barrierCooldown = barrierHp > 0.0
                        ? 0.0
                        : passiveManager.getHydroAquaBarrierCooldownSecondsRemaining(playerId);
                entries.add(new TrackerEntry(
                        "Tidal Flow",
                        0.0,
                        swimming || underwater,
                        MOVEMENT_ICON,
                        null,
                        swimming || underwater ? "ACTIVE" : "READY",
                        "",
                        false
                ));
                entries.add(new TrackerEntry(
                        "Aqua Barrier",
                        barrierCooldown,
                        barrierHp > 0.0,
                        HYDRO_BARRIER_ICON,
                        null,
                        barrierHp > 0.0 ? "ACTIVE" : barrierCooldown > 0.0 ? "COOLDOWN" : "READY",
                        barrierHp > 0.0 ? formatTrackerNumber(barrierHp) + "/" + formatTrackerNumber(barrierHp) : "",
                        false
                ));
            }
            case "aero" -> entries.add(new TrackerEntry(
                    "Wind Walker",
                    0.0,
                    false,
                    MOVEMENT_ICON,
                    null,
                    "READY",
                    "",
                    false
            ));
            case "corruptus" -> {
                int stacks = passiveManager.getCorruptusDarkResurrectionStacks(playerId);
                int maxStacks = passiveManager.getCorruptusSoulHarvestMaxStacks();
                double lockout = passiveManager.getCorruptusPassiveLockoutSecondsRemaining(playerId);
                entries.add(new TrackerEntry(
                        "Soul Harvest " + stacks + "/" + maxStacks,
                        lockout,
                        stacks > 0 && lockout <= 0.0,
                        CORRUPTUS_ICON,
                        null,
                        stacks > 0 && lockout <= 0.0 ? "ACTIVE" : lockout > 0.0 ? "COOLDOWN" : "READY",
                        lockout > 0.0 ? "LOCKOUT " + formatTrackerTimer(lockout) : "",
                        false
                ));
            }
            default -> {
            }
        }
    }

    private void renderTrackerRow(UICommandBuilder commands, int index, TrackerEntry entry) {
        String prefix = "#TrackerRow" + index;
        commands.set(prefix + "Root.Visible", entry != null);
        if (entry == null) {
            setText(commands, prefix + "Name.Text", "");
            setText(commands, prefix + "NameShadow.Text", "");
            setText(commands, prefix + "Timer.Text", "");
            setText(commands, prefix + "TimerShadow.Text", "");
            commands.set(prefix + "Timer.Visible", false);
            commands.set(prefix + "TimerShadow.Visible", false);
            commands.set(prefix + "Icon.Visible", false);
            commands.set(prefix + "Frame.Visible", false);
            return;
        }

        boolean coolingDown = entry.cooldownSeconds() > 0.0;
        // DEFECT (2026-07-18, live-verified): CustomUI FORBIDS changing Sprite
        // TexturePath / Group Background via runtime commands - the client hard-
        // disconnects on the first such command ("CustomUI is not allowed to
        // change this property. Selector: #TrackerRow2Icon"), blocking world
        // entry entirely. Icons stay hidden until the vanilla pattern lands:
        // pre-declared per-family icon nodes in the .ui toggled via Visible.
        boolean iconVisible = false;
        boolean frameVisible = false;
        String nameColor = entry.active() ? "#a8ff9a" : coolingDown ? "#a6a6a6" : "#ffffff";
        String timerColor = coolingDown && !entry.active() ? "#a6a6a6" : "#ffffff";
        String timerText = entry.counterText() != null && !entry.counterText().isBlank()
                ? entry.counterText()
                : coolingDown ? formatTrackerTimer(entry.cooldownSeconds()) : "";

        setText(commands, prefix + "Name.Text", fitTrackerName(entry.name()));
        setText(commands, prefix + "NameShadow.Text", fitTrackerName(entry.name()));
        setText(commands, prefix + "Timer.Text", timerText);
        setText(commands, prefix + "TimerShadow.Text", timerText);
        commands.set(prefix + "Name.Style.TextColor", nameColor);
        commands.set(prefix + "Name.Style.RenderBold", entry.active());
        commands.set(prefix + "NameShadow.Style.RenderBold", entry.active());
        commands.set(prefix + "Timer.Style.TextColor", timerColor);
        commands.set(prefix + "Timer.Style.RenderBold", false);
        commands.set(prefix + "TimerShadow.Style.RenderBold", false);
        commands.set(prefix + "Timer.Visible", !timerText.isBlank());
        commands.set(prefix + "TimerShadow.Visible", !timerText.isBlank());
        // TexturePath/Background writes removed - see defect note above.
        commands.set(prefix + "Icon.Visible", iconVisible);
        commands.set(prefix + "Frame.Visible", frameVisible);
    }

    private String fitTrackerName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        String normalized = name.trim();
        return normalized.length() <= 34 ? normalized : normalized.substring(0, 33).trim() + ".";
    }

    private String formatTrackerTimer(double seconds) {
        return Math.max(0, (int) Math.ceil(seconds)) + "s";
    }

    private String formatTrackerNumber(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        return String.format(Locale.ROOT, "%.0f", Math.max(0.0, value));
    }

    private String titleCase(String raw) {
        String normalized = raw == null ? "" : raw.replace('_', ' ').replace('-', ' ').trim();
        if (normalized.isBlank()) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        for (String part : normalized.split("\\s+")) {
            if (part.isBlank()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(' ');
            }
            result.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                result.append(part.substring(1).toLowerCase(Locale.ROOT));
            }
        }
        return result.toString();
    }

    private void renderStatusSlots(UICommandBuilder commands,
                                   String prefix,
                                   List<HudStatusEntry> entries,
                                   int maxSlots) {
        for (int slot = 1; slot <= maxSlots; slot++) {
            HudStatusEntry entry = slot <= entries.size() ? entries.get(slot - 1) : null;
            renderStatusSlot(commands, prefix + slot, entry);
        }
    }

    private void renderStatusSlot(UICommandBuilder commands, String slotId, HudStatusEntry entry) {
        String selector = "#" + slotId;
        commands.set(selector + "Root.Visible", entry != null);

        if (entry == null) {
            return;
        }

        commands.set(selector + "BuffBg.Visible", false);
        commands.set(selector + "DebuffBg.Visible", false);
        commands.set(selector + "DisabledBg.Visible", false);

        commands.set(selector + "ArrowBuff.Visible", false);
        commands.set(selector + "ArrowDebuff.Visible", false);
        commands.set(selector + "ArrowBuffDisabled.Visible", false);

        commands.set(selector + "CooldownBuff.Visible", false);
        commands.set(selector + "CooldownDebuff.Visible", false);
        commands.set(selector + "CooldownPassive.Visible", false);
        commands.set(selector + "CooldownBuff.Value", 0.0);
        commands.set(selector + "CooldownDebuff.Value", 0.0);
        commands.set(selector + "CooldownPassive.Value", 0.0);

        commands.set(selector + "IconAttack.Visible", false);
        commands.set(selector + "IconDefense.Visible", false);
        commands.set(selector + "IconHealth.Visible", false);
        commands.set(selector + "IconSpeed.Visible", false);
        commands.set(selector + "IconStamina.Visible", false);
        commands.set(selector + "IconShield.Visible", false);
        commands.set(selector + "IconMagic.Visible", false);
        commands.set(selector + "IconSword.Visible", false);

        setText(commands, selector + "Tag.Text", abbreviateStatusTag(entry.tag()));
        setText(commands, selector + "Detail.Text", abbreviateStatusDetail(entry.label()));
        setText(commands, selector + "Counter.Text", entry.counter());
        commands.set(selector + "Counter.Visible", entry.counter() != null && !entry.counter().isBlank());
    }

    private String abbreviateStatusDetail(String detail) {
        if (detail == null || detail.isBlank()) {
            return "";
        }

        String normalized = detail.trim();
        if (normalized.length() <= 9) {
            return normalized;
        }

        String[] words = normalized.split("\\s+");
        if (words.length >= 2) {
            String combined = fitWord(words[0], 4) + " " + fitWord(words[1], 4);
            if (combined.length() <= 9) {
                return combined;
            }
        }

        return fitWord(normalized, 8) + ".";
    }

    private String abbreviateStatusTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return "";
        }

        String normalized = tag.trim();
        if (normalized.length() <= 7) {
            return normalized;
        }

        return fitWord(normalized, 6) + ".";
    }

    private String fitWord(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "";
        }

        String normalized = value.trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }

        return normalized.substring(0, Math.max(1, maxLength)).trim();
    }

    private void renderXp(UICommandBuilder commands, PlayerData player) {
        if (player == null) {
            commands.set("#XpBar.Value", 0.0);
            setText(commands, "#XpLabel.Text", "Lv 1 | XP 0 / 100");
            setText(commands, "#XpMilestone.Text", "");
            commands.set("#XpMilestone.Visible", false);
            return;
        }

        int xpRequired = Math.max(1, mod.getLevelingManager().calculateXpRequired(player.getLevel()));
        double progress = Math.max(
                0.0,
                Math.min(mod.getLevelingManager().getXpProgressPercent(player) / 100.0, 1.0)
        );
        int nextMilestone = ((player.getLevel() / 10) + 1) * 10;

        commands.set("#XpBar.Value", progress);
        setText(
                commands,
                "#XpLabel.Text",
                "Lv " + player.getLevel() + " | XP " + player.getCurrentXp() + " / " + xpRequired
        );
        setText(commands, "#XpMilestone.Text", "");
        commands.set("#XpMilestone.Visible", false);
    }

    private void renderResource(UICommandBuilder commands, PlayerData player) {
        ResourceSnapshot snapshot = buildResourceSnapshot(player);

        commands.set("#ResourceRoot.Visible", snapshot.visible);
        commands.set("#ResourceTerraBar.Visible", snapshot.visible && "terra".equals(snapshot.classId));
        commands.set("#ResourceHydroBar.Visible", snapshot.visible && "hydro".equals(snapshot.classId));
        commands.set("#ResourceAeroBar.Visible", snapshot.visible && "aero".equals(snapshot.classId));
        commands.set("#ResourceCorruptusBar.Visible", snapshot.visible && "corruptus".equals(snapshot.classId));

        commands.set("#ResourceTerraBar.Value", snapshot.progress);
        commands.set("#ResourceHydroBar.Value", snapshot.progress);
        commands.set("#ResourceAeroBar.Value", snapshot.progress);
        commands.set("#ResourceCorruptusBar.Value", snapshot.progress);

        setText(commands, "#ResourceTitle.Text", snapshot.title);
        setText(commands, "#ResourceLabel.Text", snapshot.label);
    }

    private void renderAbilitySlots(UICommandBuilder commands, PlayerData player) {
        boolean visible = player != null
                && player.getPlayerClass() != null
                && player.getSelectedStyles() != null
                && !player.getSelectedStyles().isEmpty()
                && isSpellbookEquipped(player);
        commands.set("#AbilitySlotsRoot.Visible", visible);

        for (int slot = 1; slot <= 3; slot++) {
            renderAbilitySlot(commands, player, slot, visible);
        }
    }

    private void renderAbilitySlot(UICommandBuilder commands, PlayerData player, int slot, boolean rootVisible) {
        String prefix = "#Ability" + slot;
        var slotStatus = rootVisible
                ? mod.getStyleManager().getAbilitySlotStatus(player, slot)
                : com.motm.manager.StyleManager.AbilitySlotStatus.unavailable();

        commands.set(prefix + "Root.Visible", slotStatus.available());
        if (!slotStatus.available()) {
            setText(commands, prefix + "Name.Text", "");
            setText(commands, prefix + "Timer.Text", "");
            commands.set(prefix + "Timer.Visible", false);
            commands.set(prefix + "ReadyBg.Visible", false);
            commands.set(prefix + "CooldownBg.Visible", false);
            renderAbilityIcon(commands, prefix, null);
            return;
        }

        boolean ready = slotStatus.phase() == com.motm.manager.StyleManager.AbilityPhase.READY && !slotStatus.toggleActive();
        String timerText = buildAbilityTimerText(slotStatus);

        setText(commands, prefix + "Name.Text", abbreviateAbilityName(slotStatus.abilityName()));
        setText(commands, prefix + "Timer.Text", timerText);
        commands.set(prefix + "Timer.Visible", !timerText.isBlank());
        commands.set(prefix + "ReadyBg.Visible", false);
        commands.set(prefix + "CooldownBg.Visible", false);
        renderAbilityIcon(commands, prefix, resolveAbilityHudIcon(player, slotStatus.abilityId()));
    }

    private String abbreviateAbilityName(String abilityName) {
        if (abilityName == null || abilityName.isBlank()) {
            return "";
        }

        String normalized = abilityName.trim();
        if (normalized.length() <= 9) {
            return normalized;
        }

        String[] words = normalized.split("\\s+");
        if (words.length >= 2) {
            String combined = fitWord(words[0], 4) + " " + fitWord(words[1], 4);
            if (combined.length() <= 9) {
                return combined;
            }
        }

        return fitWord(normalized, 8) + ".";
    }

    private String buildAbilityTimerText(com.motm.manager.StyleManager.AbilitySlotStatus slotStatus) {
        if (slotStatus == null || !slotStatus.available()) {
            return "";
        }

        if (slotStatus.toggleActive()) {
            return slotStatus.remainingSeconds() > 0
                    ? formatCompactSeconds(slotStatus.remainingSeconds())
                    : "ON";
        }

        if (slotStatus.maxCharges() > 0) {
            return switch (slotStatus.phase()) {
                case READY -> slotStatus.maxCharges() > 1
                        ? slotStatus.currentCharges() + "/" + slotStatus.maxCharges()
                        : "";
                case ACTIVE -> slotStatus.remainingSeconds() > 0
                        ? formatCompactSeconds(slotStatus.remainingSeconds())
                        : "ON";
                case CASTING, RECOVERY, COOLDOWN -> formatCompactSeconds(slotStatus.remainingSeconds());
            };
        }

        return switch (slotStatus.phase()) {
            case READY -> "";
            case ACTIVE -> slotStatus.remainingSeconds() > 0
                    ? formatCompactSeconds(slotStatus.remainingSeconds())
                    : "ON";
            case CASTING, RECOVERY, COOLDOWN -> formatCompactSeconds(slotStatus.remainingSeconds());
        };
    }

    private String formatCompactSeconds(double seconds) {
        double safeSeconds = Math.max(0.0, seconds);
        if (safeSeconds >= 10.0) {
            return Integer.toString((int) Math.ceil(safeSeconds));
        }
        return String.format(Locale.US, "%.1f", safeSeconds);
    }

    private StatusIcon resolveAbilityHudIcon(PlayerData player, String abilityId) {
        if (player == null || abilityId == null || abilityId.isBlank()) {
            return StatusIcon.MAGIC;
        }

        AbilityData ability = mod.getStyleManager().findAbility(player, abilityId);
        if (ability == null) {
            return StatusIcon.MAGIC;
        }

        Set<String> categories = new HashSet<>();
        if (ability.getCategories() != null) {
            for (String category : ability.getCategories()) {
                if (category != null && !category.isBlank()) {
                    categories.add(category.toLowerCase(Locale.ROOT));
                }
            }
        }

        String castType = safeLower(ability.getCastType());
        String effect = safeLower(ability.getEffect());
        String travelType = safeLower(ability.getTravelType());
        String terrainEffect = safeLower(ability.getTerrainEffect());

        if (ability.getShieldPercent() > 0 || castType.contains("barrier") || effect.contains("shield")) {
            return StatusIcon.SHIELD;
        }

        if (ability.getHealPercent() > 0
                || categories.contains("healing")
                || effect.contains("heal")
                || effect.contains("regen")
                || effect.contains("purify")
                || effect.contains("absorb")) {
            return StatusIcon.HEALTH;
        }

        if (castType.contains("dash")
                || castType.contains("teleport")
                || castType.contains("jump")
                || categories.contains("dash")
                || categories.contains("mobility")
                || effect.contains("speed")
                || travelType.contains("skate")) {
            return StatusIcon.SPEED;
        }

        if (categories.contains("buff")) {
            if (effect.contains("defense") || effect.contains("fortitude") || effect.contains("armor")) {
                return StatusIcon.DEFENSE;
            }
            if (effect.contains("attack")
                    || effect.contains("power")
                    || effect.contains("imbue")
                    || effect.contains("alloy")
                    || effect.contains("metal_coat")) {
                return StatusIcon.ATTACK;
            }
            return StatusIcon.SWORD;
        }

        if (categories.contains("crowd_control")
                || categories.contains("debuff")
                || effect.contains("stun")
                || effect.contains("slow")
                || effect.contains("freeze")
                || effect.contains("root")
                || effect.contains("blind")
                || effect.contains("grounded")
                || effect.contains("disorient")
                || effect.contains("dominate")) {
            return StatusIcon.STAMINA;
        }

        if (categories.contains("summon")
                || castType.contains("summon")
                || castType.contains("transform")
                || ability.getSummonName() != null
                || terrainEffect.contains("rift")
                || effect.contains("void")) {
            return StatusIcon.MAGIC;
        }

        if (categories.contains("damage") || categories.contains("dot")) {
            if (castType.contains("projectile")
                    || castType.contains("wave")
                    || castType.contains("beam")
                    || castType.contains("cone")
                    || castType.contains("ground")
                    || castType.contains("zone")
                    || travelType.contains("wave")
                    || travelType.contains("shot")) {
                return StatusIcon.MAGIC;
            }
            return StatusIcon.SWORD;
        }

        return StatusIcon.MAGIC;
    }

    private void renderAbilityIcon(UICommandBuilder commands, String prefix, StatusIcon icon) {
        commands.set(prefix + "IconAttack.Visible", icon == StatusIcon.ATTACK);
        commands.set(prefix + "IconDefense.Visible", icon == StatusIcon.DEFENSE);
        commands.set(prefix + "IconHealth.Visible", icon == StatusIcon.HEALTH);
        commands.set(prefix + "IconSpeed.Visible", icon == StatusIcon.SPEED);
        commands.set(prefix + "IconStamina.Visible", icon == StatusIcon.STAMINA);
        commands.set(prefix + "IconShield.Visible", icon == StatusIcon.SHIELD);
        commands.set(prefix + "IconMagic.Visible", icon == StatusIcon.MAGIC);
        commands.set(prefix + "IconSword.Visible", icon == StatusIcon.SWORD);
    }

    private String safeLower(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private ResourceSnapshot buildResourceSnapshot(PlayerData player) {
        if (player == null || player.getPlayerClass() == null) {
            return ResourceSnapshot.hidden();
        }
        if (!mod.getResourceManager().areAbilityResourceCostsEnabled()) {
            return ResourceSnapshot.hidden();
        }

        String classId = player.getPlayerClass().toLowerCase(Locale.ROOT);
        if ("aero".equals(classId)) {
            return ResourceSnapshot.hidden();
        }

        StyleData selectedStyle = getSelectedStyle(player);
        if (selectedStyle != null && !styleUsesResources(selectedStyle)) {
            return ResourceSnapshot.hidden();
        }

        String resourceType = resolveResourceType(classId, selectedStyle);

        if (resourceType == null) {
            return new ResourceSnapshot(
                    true,
                    classId,
                    "Style Resource",
                    "Choose a style to track its ability resource.",
                    0.0
            );
        }

        int current = mod.getResourceManager().getAmount(player.getPlayerId(), resourceType);
        int hudMax = Math.max(1, mod.getResourceManager().getHudDisplayMax(player.getPlayerId(), resourceType));
        int actualMax = Math.max(hudMax, mod.getResourceManager().getMaxAmount(player.getPlayerId(), resourceType));
        double progress = Math.max(0.0, Math.min(current / (double) hudMax, 1.0));

        String displayName = mod.getResourceManager().getDisplayName(resourceType);
        String title;
        if ("hydro".equals(classId)) {
            title = "Hydro Waterskin";
        } else {
            title = selectedStyle != null
                    ? selectedStyle.getName() + " | " + displayName
                    : displayName + " Resource";
        }
        String label = actualMax >= 999
                ? displayName + ": " + current + " / " + hudMax + "+"
                : displayName + ": " + current + " / " + actualMax;

        return new ResourceSnapshot(true, classId, title, label, progress);
    }

    private boolean styleUsesResources(StyleData style) {
        if (style == null || style.getAbilities() == null || style.getAbilities().isEmpty()) {
            return false;
        }

        for (AbilityData ability : style.getAbilities()) {
            if (ability != null && ability.getResourceCost() > 0) {
                return true;
            }
        }
        return false;
    }

    private String resolveResourceType(String classId, StyleData selectedStyle) {
        if (selectedStyle != null && selectedStyle.getResourceType() != null && !selectedStyle.getResourceType().isBlank()) {
            return selectedStyle.getResourceType();
        }

        return switch (classId) {
            case "hydro" -> "water";
            case "corruptus" -> "souls";
            default -> null;
        };
    }

    private StyleData getSelectedStyle(PlayerData player) {
        if (player.getPlayerClass() == null || player.getSelectedStyles() == null || player.getSelectedStyles().isEmpty()) {
            return null;
        }
        return mod.getDataLoader().getStyleById(player.getSelectedStyles().get(0), player.getPlayerClass());
    }

    private PlayerData currentPlayer() {
        return mod.getPlayerDataManager().getOnlinePlayer(getPlayerRef().getUuid().toString());
    }

    private boolean isSpellbookEquipped(PlayerData player) {
        if (player == null || player.getPlayerId() == null) {
            return false;
        }

        var runtimePlayer = mod.getRuntimePlayer(player.getPlayerId());
        if (runtimePlayer == null || runtimePlayer.getReference() == null || !runtimePlayer.getReference().isValid()
                || runtimePlayer.getReference().getStore() == null) {
            return false;
        }

        return mod.isSpellbookItem(InventoryComponent.getItemInHand(
                runtimePlayer.getReference().getStore(),
                runtimePlayer.getReference()
        ));
    }

    private void setText(UICommandBuilder commands, String selector, String value) {
        commands.set(selector, value != null ? value : "");
    }

    private record ResourceSnapshot(
            boolean visible,
            String classId,
            String title,
            String label,
            double progress
    ) {
        private static ResourceSnapshot hidden() {
            return new ResourceSnapshot(false, "", "", "", 0.0);
        }
    }

    private record HudStatusEntry(
            String tag,
            String label,
            StatusTone tone,
            StatusIcon icon,
            double progress,
            String counter,
            int priority
    ) {}

    private record TrackerEntry(
            String name,
            double cooldownSeconds,
            boolean active,
            String iconPath,
            String framePath,
            String state,
            String counterText,
            boolean showIconWhenInactive
    ) {
        private TrackerEntry(String name, double cooldownSeconds, boolean active) {
            this(name, cooldownSeconds, active, null, null,
                    active ? "ACTIVE" : cooldownSeconds > 0.0 ? "COOLDOWN" : "READY",
                    "", false);
        }
    }

    private enum StatusTone {
        BUFF,
        PASSIVE,
        DEBUFF
    }

    private enum StatusIcon {
        ATTACK,
        DEFENSE,
        HEALTH,
        SPEED,
        STAMINA,
        SHIELD,
        MAGIC,
        SWORD
    }
}
