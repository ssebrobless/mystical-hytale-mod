package com.motm.ui;

import com.hypixel.hytale.server.core.entity.entities.player.hud.CustomUIHud;
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

    private static final String HUD_DOCUMENT = "Hud/MOTM_StatusHud.ui";
    private static final int MAX_BUFF_SLOTS = 3;
    private static final int MAX_DEBUFF_SLOTS = 3;
    private static final int TICKS_PER_SECOND = 20;

    private final MenteesMod mod;

    public MotmStatusHud(PlayerRef playerRef, MenteesMod mod) {
        super(playerRef);
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
        renderXp(commands, player);
        renderResource(commands, player);
        renderAbilitySlots(commands, player);
    }

    private void renderStatusStrip(UICommandBuilder commands, PlayerData player) {
        List<HudStatusEntry> buffs = new ArrayList<>();
        List<HudStatusEntry> debuffs = new ArrayList<>();

        if (player != null && player.getPlayerId() != null) {
            appendPassiveEntry(player, buffs);
            appendStatusEffectEntries(player, buffs, debuffs);
        }

        buffs.sort(Comparator.comparingInt(HudStatusEntry::priority).reversed());
        debuffs.sort(Comparator.comparingInt(HudStatusEntry::priority).reversed());

        String primaryLine = buildStatusSummaryLine(buffs, 3);
        String secondaryLine = buildStatusSummaryLine(debuffs, 3);
        boolean visible = !primaryLine.isBlank() || !secondaryLine.isBlank();

        commands.set("#StatusRoot.Visible", visible);
        setText(commands, "#StatusLine1.Text", primaryLine);
        setText(commands, "#StatusLine2.Text", secondaryLine);
        commands.set("#StatusLine1.Visible", !primaryLine.isBlank());
        commands.set("#StatusLine2.Visible", !secondaryLine.isBlank());

        // Hide the legacy icon-slot widgets; we now render a text-first status summary.
        renderStatusSlots(commands, "BuffStatus", List.of(), MAX_BUFF_SLOTS);
        renderStatusSlots(commands, "DebuffStatus", List.of(), MAX_DEBUFF_SLOTS);
    }

    private String buildStatusSummaryLine(List<HudStatusEntry> entries, int maxEntries) {
        if (entries == null || entries.isEmpty() || maxEntries <= 0) {
            return "";
        }

        StringBuilder summary = new StringBuilder();
        int count = 0;
        for (HudStatusEntry entry : entries) {
            if (entry == null) {
                continue;
            }

            if (summary.length() > 0) {
                summary.append("  |  ");
            }
            summary.append(entry.tag());
            if (entry.label() != null && !entry.label().isBlank()) {
                summary.append(": ").append(entry.label());
            }
            if (entry.counter() != null && !entry.counter().isBlank()) {
                summary.append(" ").append(entry.counter());
            }

            count++;
            if (count >= maxEntries || summary.length() >= 72) {
                break;
            }
        }

        return summary.toString();
    }

    private void appendPassiveEntry(PlayerData player, List<HudStatusEntry> buffs) {
        if (player.getPlayerClass() == null) {
            return;
        }

        String playerId = player.getPlayerId();
        String classId = player.getPlayerClass().toLowerCase(Locale.ROOT);
        var passiveManager = mod.getClassPassiveManager();

        switch (classId) {
            case "terra" -> {
                int stationaryTicks = passiveManager.getTerraStationaryTicks(playerId);
                int requiredTicks = Math.max(1, passiveManager.getTerraStationaryTicksRequired());
                boolean primed = passiveManager.isTerraShieldPrimed(playerId);
                boolean caveVision = passiveManager.isTerraCaveVisionActive(playerId);
                buffs.add(new HudStatusEntry(
                        "Terra",
                        primed ? "Shield Up" : caveVision ? "Cave Vision" : "Charging",
                        primed ? StatusTone.BUFF : StatusTone.PASSIVE,
                        StatusIcon.SHIELD,
                        Math.max(0.0, Math.min(stationaryTicks / (double) requiredTicks, 1.0)),
                        caveVision ? "NV" : "",
                        primed ? 100 : 90
                ));
            }
            case "hydro" -> {
                int water = mod.getResourceManager().getAmount(playerId, "water");
                int maxWater = Math.max(1, mod.getResourceManager().getMaxAmount(playerId, "water"));
                boolean lowWaterMode = passiveManager.isHydroLowResourceMode(playerId);
                boolean swimming = passiveManager.isHydroSwimming(playerId);
                boolean underwater = passiveManager.isHydroUnderwater(playerId);
                buffs.add(new HudStatusEntry(
                        "Hydro",
                        lowWaterMode
                                ? "Low Water"
                                : underwater
                                ? "Underwater"
                                : swimming
                                ? "Swimming"
                                : "Tidal Flow",
                        lowWaterMode ? StatusTone.BUFF : StatusTone.PASSIVE,
                        StatusIcon.HEALTH,
                        1.0 - Math.max(0.0, Math.min(water / (double) maxWater, 1.0)),
                        underwater ? "O2+" : swimming ? "SW" : "",
                        lowWaterMode ? 100 : 85
                ));
            }
            case "aero" -> {
                buffs.add(new HudStatusEntry(
                        "Aero",
                        "Tempo Surge",
                        StatusTone.PASSIVE,
                        StatusIcon.SPEED,
                        1.0,
                        "EN+",
                        88
                ));
            }
            case "corruptus" -> {
                int souls = mod.getResourceManager().getAmount(playerId, "souls");
                int maxSouls = Math.max(1, mod.getResourceManager().getMaxAmount(playerId, "souls"));
                buffs.add(new HudStatusEntry(
                        "Souls",
                        souls > 0 ? "Harvest" : "Empty",
                        souls > 0 ? StatusTone.BUFF : StatusTone.PASSIVE,
                        StatusIcon.MAGIC,
                        Math.max(0.0, Math.min(souls / (double) maxSouls, 1.0)),
                        souls > 0 ? Integer.toString(souls) : "",
                        souls > 0 ? 100 : 84
                ));
            }
            default -> {
            }
        }
    }

    private void appendStatusEffectEntries(PlayerData player,
                                           List<HudStatusEntry> buffs,
                                           List<HudStatusEntry> debuffs) {
        Map<StatusEffect.Type, AggregatedEffect> aggregated = new EnumMap<>(StatusEffect.Type.class);

        for (StatusEffect effect : mod.getStatusEffectManager().getEffects(player.getPlayerId())) {
            if (effect == null || effect.isExpired()) {
                continue;
            }

            StatusEffect.Type normalized = normalizeType(effect.getType());
            AggregatedEffect snapshot = aggregated.get(normalized);
            if (snapshot == null) {
                snapshot = new AggregatedEffect(normalized);
                aggregated.put(normalized, snapshot);
            }
            snapshot.count++;
            snapshot.remainingTicks = Math.max(snapshot.remainingTicks, effect.getRemainingTicks());
            snapshot.initialTicks = Math.max(snapshot.initialTicks, effect.getInitialDurationTicks());
        }

        for (AggregatedEffect effect : aggregated.values()) {
            HudStatusEntry entry = toHudStatusEntry(effect);
            if (entry == null) {
                continue;
            }
            if (entry.tone() == StatusTone.DEBUFF) {
                debuffs.add(entry);
            } else {
                buffs.add(entry);
            }
        }
    }

    private StatusEffect.Type normalizeType(StatusEffect.Type type) {
        return type == StatusEffect.Type.SLOW_STACK ? StatusEffect.Type.SLOW : type;
    }

    private HudStatusEntry toHudStatusEntry(AggregatedEffect effect) {
        double durationProgress = effect.initialTicks <= 0
                ? 1.0
                : Math.max(0.0, Math.min(effect.remainingTicks / (double) effect.initialTicks, 1.0));
        String counter = effect.count > 1
                ? Integer.toString(effect.count)
                : formatDurationCounter(effect.remainingTicks);

        return switch (effect.type) {
            case SHIELD -> new HudStatusEntry("Shield", "Shield", StatusTone.BUFF, StatusIcon.SHIELD, durationProgress, counter, 95);
            case DEFENSE_BUFF -> new HudStatusEntry("Guard", "Defense Buff", StatusTone.BUFF, StatusIcon.DEFENSE, durationProgress, counter, 82);
            case ATTACK_BUFF -> new HudStatusEntry("Power", "Attack Buff", StatusTone.BUFF, StatusIcon.ATTACK, durationProgress, counter, 81);
            case DAMAGE_BUFF -> new HudStatusEntry("Blade", "Damage Buff", StatusTone.BUFF, StatusIcon.SWORD, durationProgress, counter, 80);
            case STEALTH -> new HudStatusEntry("Stealth", "Stealth", StatusTone.BUFF, StatusIcon.MAGIC, durationProgress, counter, 78);
            case HEAL_OVER_TIME -> new HudStatusEntry("Regen", "Regeneration", StatusTone.BUFF, StatusIcon.HEALTH, durationProgress, counter, 77);
            case LIFESTEAL -> new HudStatusEntry("Leech", "Lifesteal", StatusTone.BUFF, StatusIcon.HEALTH, durationProgress, counter, 76);
            case SPEED_BUFF -> new HudStatusEntry("Speed", "Speed Buff", StatusTone.BUFF, StatusIcon.SPEED, durationProgress, counter, 75);
            case EVASION -> new HudStatusEntry("Evade", "Evasion", StatusTone.BUFF, StatusIcon.SPEED, durationProgress, counter, 74);
            case FLYING -> new HudStatusEntry("Flight", "Flying", StatusTone.BUFF, StatusIcon.STAMINA, durationProgress, counter, 73);
            case BURN -> new HudStatusEntry("Burn", "Burn", StatusTone.DEBUFF, StatusIcon.MAGIC, durationProgress, counter, 96);
            case DOT -> new HudStatusEntry("DOT", "Damage Over Time", StatusTone.DEBUFF, StatusIcon.HEALTH, durationProgress, counter, 95);
            case STUN -> new HudStatusEntry("Stun", "Stun", StatusTone.DEBUFF, StatusIcon.MAGIC, durationProgress, counter, 100);
            case FREEZE -> new HudStatusEntry("Freeze", "Freeze", StatusTone.DEBUFF, StatusIcon.STAMINA, durationProgress, counter, 99);
            case SLOW -> new HudStatusEntry("Slow", "Slow", StatusTone.DEBUFF, StatusIcon.SPEED, durationProgress, counter, 84);
            case ROOT -> new HudStatusEntry("Root", "Root", StatusTone.DEBUFF, StatusIcon.STAMINA, durationProgress, counter, 88);
            case BLIND -> new HudStatusEntry("Blind", "Blind", StatusTone.DEBUFF, StatusIcon.MAGIC, durationProgress, counter, 79);
            case DISORIENTED -> new HudStatusEntry("Daze", "Disoriented", StatusTone.DEBUFF, StatusIcon.MAGIC, durationProgress, counter, 78);
            case GROUNDED -> new HudStatusEntry("Ground", "Grounded", StatusTone.DEBUFF, StatusIcon.STAMINA, durationProgress, counter, 85);
            case SHOCKED -> new HudStatusEntry("Shock", "Shocked", StatusTone.DEBUFF, StatusIcon.MAGIC, durationProgress, counter, 83);
            case VULNERABILITY -> new HudStatusEntry("Vuln", "Vulnerability", StatusTone.DEBUFF, StatusIcon.ATTACK, durationProgress, counter, 87);
            case KNOCKBACK -> null;
            default -> null;
        };
    }

    private String formatDurationCounter(int remainingTicks) {
        if (remainingTicks <= 0) {
            return "";
        }

        int seconds = (int) Math.ceil(remainingTicks / (double) TICKS_PER_SECOND);
        return seconds > 1 ? Integer.toString(seconds) : "";
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

        String classId = player.getPlayerClass().toLowerCase(Locale.ROOT);
        if ("aero".equals(classId)) {
            return ResourceSnapshot.hidden();
        }

        StyleData selectedStyle = getSelectedStyle(player);
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
        if (runtimePlayer == null || runtimePlayer.getInventory() == null) {
            return false;
        }

        return mod.isSpellbookItem(runtimePlayer.getInventory().getItemInHand());
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

    private static final class AggregatedEffect {
        private final StatusEffect.Type type;
        private int remainingTicks;
        private int initialTicks;
        private int count;

        private AggregatedEffect(StatusEffect.Type type) {
            this.type = type;
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
