package com.motm.util;

import com.motm.MenteesMod;
import com.motm.model.AbilityActionAssets;
import com.motm.model.AbilityData;
import com.motm.model.ClassData;
import com.motm.model.StyleData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Static preflight audit for MOTM content and runtime wiring.
 *
 * This is intentionally conservative: it catches missing/broken data and
 * concept mismatches implied by the authored names/descriptions, but it does
 * not claim to prove final in-game feel or visual polish.
 */
public final class MotmPreflightAudit {

    private static final Logger LOG = Logger.getLogger("MOTM");
    private static final Set<String> EXPECTED_CLASS_IDS = Set.of("terra", "hydro", "aero", "corruptus");
    private static final int EXPECTED_STYLES_PER_CLASS = 10;
    private static final int EXPECTED_ABILITIES_PER_STYLE = 3;
    private static final int EXPECTED_TOTAL_STYLES = 40;
    private static final int EXPECTED_TOTAL_ABILITIES = 120;
    private static final String HUD_DOC_RESOURCE = "Common/UI/Custom/Hud/MOTM_StatusHud.ui";
    private static final String REPORT_FILE_NAME = "motm-preflight-report.txt";

    private static final Set<String> SUPPORTED_CAST_TYPES = Set.of(
            "air_stall", "barrier", "chain", "channel", "cleanse", "cone",
            "curse", "dash", "dash_buff", "dash_strike", "dive_strike", "execute",
            "gaze", "ground_burst", "ground_strike", "ground_target", "ground_zone",
            "leap", "line_control", "projectile", "projectile_burst",
            "projectile_line", "projectile_volley", "self_buff", "self_burst",
            "summon", "summon_buff", "support_zone", "teleport", "transformation",
            "wave_line"
    );

    private static final Set<String> PROJECTILE_CAST_TYPES = Set.of(
            "projectile", "projectile_line", "projectile_burst", "projectile_volley", "wave_line", "chain"
    );
    private static final Set<String> PERSISTENT_CAST_TYPES = Set.of(
            "ground_zone", "support_zone", "barrier", "channel"
    );
    private static final Set<String> MOVEMENT_CAST_TYPES = Set.of(
            "dash", "dash_buff", "dash_strike", "leap", "dive_strike", "teleport", "air_stall"
    );
    private static final Set<String> GROUND_CAST_TYPES = Set.of(
            "ground_burst", "ground_strike", "ground_target", "ground_zone", "self_burst", "execute"
    );
    private static final Set<String> SUMMON_CAST_TYPES = Set.of("summon", "summon_buff");
    private static final Set<String> TOGGLE_FAMILY_CAST_TYPES = Set.of("transformation", "channel", "self_buff");

    private static final Map<String, Set<String>> ALLOWED_RESOURCE_TYPES = Map.of(
            "terra", Set.of("stone_blocks", "dirt_blocks", "sand_blocks", "seeds", "metal", "gems"),
            "hydro", Set.of("water"),
            "aero", Set.of(""),
            "corruptus", Set.of("souls")
    );

    private MotmPreflightAudit() {
    }

    public static AuditReport run(MenteesMod mod) {
        AuditBuilder audit = new AuditBuilder();

        if (mod == null) {
            audit.error("bootstrap", "Mod bootstrap reference is null.");
            AuditReport report = audit.build(null, null);
            logSummary(report);
            return report;
        }

        auditBootstrap(mod, audit);
        auditHudAndSpellbook(mod, audit);
        auditClassesAndAbilities(mod, audit);

        Path reportPath = writeReport(mod.getPluginDirectory(), audit);
        AuditReport report = audit.build(reportPath, mod.getPluginDirectory());
        logSummary(report);
        return report;
    }

    private static void auditBootstrap(MenteesMod mod, AuditBuilder audit) {
        if (mod.getDataLoader() == null) {
            audit.error("bootstrap", "DataLoader is missing.");
        }
        if (mod.getStyleManager() == null) {
            audit.error("bootstrap", "StyleManager is missing.");
        }
        if (mod.getGameplayPlaybackManager() == null) {
            audit.error("bootstrap", "GameplayPlaybackManager is missing.");
        }
        if (mod.getSpellbookManager() == null) {
            audit.error("bootstrap", "SpellbookManager is missing.");
        }
        if (mod.getPluginDirectory() == null) {
            audit.warning("bootstrap", "Plugin data directory is unavailable, so no audit file can be written.");
        }
    }

    private static void auditHudAndSpellbook(MenteesMod mod, AuditBuilder audit) {
        String defaultSpellbookItemId = safeLower(mod.getDefaultSpellbookItemId());
        Set<String> spellbookIds = new LinkedHashSet<>();
        for (String itemId : mod.getRecognizedSpellbookItemIds()) {
            spellbookIds.add(safeLower(itemId));
        }

        if (defaultSpellbookItemId.isBlank()) {
            audit.error("spellbook", "Default spellbook item id is blank.");
        } else if (!spellbookIds.contains(defaultSpellbookItemId)) {
            audit.error("spellbook", "Default spellbook item id is not present in the recognized spellbook item set.");
        }

        if (spellbookIds.isEmpty()) {
            audit.error("spellbook", "No recognized spellbook item ids are configured.");
        } else {
            audit.info("spellbook", "Recognized spellbook shells: " + spellbookIds.size());
        }

        if (mod.isCustomHudEnabled()) {
            var resource = mod.getClass().getClassLoader().getResource(HUD_DOC_RESOURCE);
            if (resource == null) {
                audit.error("hud", "Custom HUD document is missing from plugin resources: " + HUD_DOC_RESOURCE);
            } else {
                audit.info("hud", "Custom HUD document found: " + HUD_DOC_RESOURCE);
            }
        } else {
            audit.warning("hud", "Custom HUD is disabled. Spell/resource/status display will rely on fallback behavior.");
        }
    }

    private static void auditClassesAndAbilities(MenteesMod mod, AuditBuilder audit) {
        Collection<ClassData> classes = mod.getDataLoader() != null
                ? mod.getDataLoader().getAllClasses()
                : Collections.emptyList();
        audit.classCount = classes.size();

        Set<String> seenClasses = new HashSet<>();
        Set<String> seenStyles = new HashSet<>();
        Set<String> seenAbilities = new HashSet<>();

        for (String classId : EXPECTED_CLASS_IDS) {
            ClassData classData = mod.getDataLoader().getClassData(classId);
            if (classData == null) {
                audit.error("class:" + classId, "Class data is missing.");
                continue;
            }

            seenClasses.add(safeLower(classData.getId()));
            auditClassData(classId, classData, audit);

            List<StyleData> styles = mod.getDataLoader().getStylesForClass(classId);
            if (styles == null || styles.isEmpty()) {
                audit.error("class:" + classId, "No styles were loaded for this class.");
                continue;
            }

            if (styles.size() != EXPECTED_STYLES_PER_CLASS) {
                audit.error("class:" + classId,
                        "Expected " + EXPECTED_STYLES_PER_CLASS + " styles, found " + styles.size() + ".");
            }

            for (StyleData style : styles) {
                audit.styleCount++;
                auditStyleData(classId, style, seenStyles, audit);

                List<AbilityData> abilities = style.getAbilities();
                if (abilities == null || abilities.isEmpty()) {
                    audit.error("style:" + safeLabel(style.getId()), "Style has no abilities.");
                    continue;
                }

                if (abilities.size() != EXPECTED_ABILITIES_PER_STYLE) {
                    audit.error("style:" + safeLabel(style.getId()),
                            "Expected " + EXPECTED_ABILITIES_PER_STYLE + " abilities, found " + abilities.size() + ".");
                }

                for (AbilityData ability : abilities) {
                    audit.abilityCount++;
                    auditAbilityData(classId, style, ability, seenAbilities, audit);
                }
            }
        }

        for (String classId : EXPECTED_CLASS_IDS) {
            if (!seenClasses.contains(classId)) {
                audit.error("classes", "Expected class id '" + classId + "' was not loaded.");
            }
        }

        if (audit.styleCount != EXPECTED_TOTAL_STYLES) {
            audit.error("styles", "Expected " + EXPECTED_TOTAL_STYLES + " total styles, found " + audit.styleCount + ".");
        }
        if (audit.abilityCount != EXPECTED_TOTAL_ABILITIES) {
            audit.error("abilities",
                    "Expected " + EXPECTED_TOTAL_ABILITIES + " total abilities, found " + audit.abilityCount + ".");
        }
    }

    private static void auditClassData(String expectedClassId, ClassData classData, AuditBuilder audit) {
        String actualId = safeLower(classData.getId());
        if (!expectedClassId.equals(actualId)) {
            audit.error("class:" + expectedClassId, "Class id mismatch. Expected '" + expectedClassId + "', found '" + actualId + "'.");
        }
        if (isBlank(classData.getDisplayName())) {
            audit.error("class:" + expectedClassId, "Display name is blank.");
        }
        if (isBlank(classData.getTheme())) {
            audit.warning("class:" + expectedClassId, "Theme text is blank.");
        }
        if (isBlank(classData.getDescription())) {
            audit.warning("class:" + expectedClassId, "Description is blank.");
        }

        ClassData.PassiveAbility passiveAbility = classData.getPassiveAbility();
        if (passiveAbility == null) {
            audit.error("class:" + expectedClassId, "Passive ability is missing.");
            return;
        }

        if (isBlank(passiveAbility.getId())) {
            audit.error("class:" + expectedClassId, "Passive ability id is blank.");
        }
        if (isBlank(passiveAbility.getName())) {
            audit.error("class:" + expectedClassId, "Passive ability name is blank.");
        }
        if (isBlank(passiveAbility.getDescription())) {
            audit.warning("class:" + expectedClassId, "Passive ability description is blank.");
        }
        if (passiveAbility.getEffects() == null || passiveAbility.getEffects().isEmpty()) {
            audit.warning("class:" + expectedClassId, "Passive ability has no encoded effect entries.");
        }
    }

    private static void auditStyleData(String classId,
                                       StyleData style,
                                       Set<String> seenStyles,
                                       AuditBuilder audit) {
        String styleId = safeLower(style != null ? style.getId() : null);
        String scope = "style:" + safeLabel(styleId);

        if (style == null) {
            audit.error(scope, "Style entry is null.");
            return;
        }
        if (styleId.isBlank()) {
            audit.error(scope, "Style id is blank.");
        } else if (!seenStyles.add(styleId)) {
            audit.error(scope, "Duplicate style id detected.");
        }
        if (isBlank(style.getName())) {
            audit.error(scope, "Style name is blank.");
        }
        if (!classId.equals(safeLower(style.getClassId()))) {
            audit.error(scope, "Style class_id does not match owning class '" + classId + "'.");
        }
        if (isBlank(style.getTheme())) {
            audit.warning(scope, "Style theme is blank.");
        }

        String resourceType = safeLower(style.getResourceType());
        Set<String> allowed = ALLOWED_RESOURCE_TYPES.getOrDefault(classId, Set.of());
        if (!allowed.contains(resourceType)) {
            audit.error(scope, "Resource type '" + style.getResourceType() + "' is not valid for class '" + classId + "'.");
        }
    }

    private static void auditAbilityData(String classId,
                                         StyleData style,
                                         AbilityData ability,
                                         Set<String> seenAbilities,
                                         AuditBuilder audit) {
        String abilityId = safeLower(ability != null ? ability.getId() : null);
        String styleId = safeLower(style != null ? style.getId() : null);
        String scope = "ability:" + safeLabel(abilityId);

        if (ability == null) {
            audit.error(scope, "Ability entry is null.");
            return;
        }

        if (abilityId.isBlank()) {
            audit.error(scope, "Ability id is blank.");
        } else if (!seenAbilities.add(abilityId)) {
            audit.error(scope, "Duplicate ability id detected.");
        }

        if (isBlank(ability.getName())) {
            audit.error(scope, "Ability name is blank.");
        }
        if (isBlank(ability.getDescription())) {
            audit.warning(scope, "Ability description is blank.");
        }
        if (isBlank(ability.getEffect())) {
            audit.warning(scope, "Ability effect tag is blank.");
        }

        String castType = safeLower(ability.getCastType());
        String targetType = safeLower(ability.getTargetType());
        String travelType = safeLower(ability.getTravelType());
        String terrainEffect = safeLower(ability.getTerrainEffect());
        String summonName = safeLower(ability.getSummonName());

        if (!SUPPORTED_CAST_TYPES.contains(castType)) {
            audit.error(scope, "Unsupported cast_type '" + ability.getCastType() + "'.");
        }
        if (isBlank(targetType)) {
            audit.warning(scope, "Target type is blank.");
        }

        if (ability.getCooldownSeconds() < 0) {
            audit.error(scope, "Cooldown cannot be negative.");
        }
        if (ability.getCastTimeSeconds() < 0) {
            audit.error(scope, "Cast time cannot be negative.");
        }
        if (ability.getRecoverySeconds() < 0) {
            audit.error(scope, "Recovery time cannot be negative.");
        }
        if (ability.getDurationSeconds() < 0) {
            audit.error(scope, "Duration cannot be negative.");
        }
        if (ability.getDelaySeconds() < 0) {
            audit.error(scope, "Delay cannot be negative.");
        }
        if (ability.getCharges() < 0) {
            audit.error(scope, "Charges cannot be negative.");
        }

        if (ability.getCharges() > 0 && ability.getChargeRechargeSeconds() <= 0) {
            audit.warning(scope, "Charge-based ability is missing explicit charge_recharge_seconds.");
        }

        if (ability.isToggleable() && ability.getToggleCooldownSeconds() < 0) {
            audit.error(scope, "Toggle cooldown cannot be negative.");
        }

        if ("transformation".equals(castType) && !ability.isToggleable()) {
            audit.warning(scope, "Transformation is relying on runtime auto-toggle instead of explicit toggleable=true.");
        }

        if (PROJECTILE_CAST_TYPES.contains(castType) && ability.getProjectileSpeed() <= 0) {
            audit.warning(scope, "Projectile-family cast is missing projectile_speed.");
        }

        if ((PROJECTILE_CAST_TYPES.contains(castType)
                || GROUND_CAST_TYPES.contains(castType)
                || "cone".equals(castType)
                || "teleport".equals(castType)
                || "line_control".equals(castType))
                && ability.getRange() <= 0
                && ability.getMaxRange() <= 0) {
            audit.warning(scope, "Cast looks ranged but has no positive range/max_range.");
        }

        if ("cone".equals(castType) && ability.getConeAngle() <= 0) {
            audit.warning(scope, "Cone cast is missing cone_angle.");
        }

        if (PERSISTENT_CAST_TYPES.contains(castType) && ability.getDurationSeconds() <= 0) {
            audit.warning(scope, "Persistent cast type has no positive duration.");
        }

        if ("barrier".equals(castType) && (ability.getWidth() <= 0 || ability.getHeight() <= 0)) {
            audit.warning(scope, "Barrier is missing width/height dimensions.");
        }

        if (SUMMON_CAST_TYPES.contains(castType) && summonName.isBlank()) {
            audit.error(scope, "Summon cast is missing summon_name.");
        }
        if (!summonName.isBlank() && !SUMMON_CAST_TYPES.contains(castType)) {
            audit.warning(scope, "Ability declares summon_name but cast_type is '" + ability.getCastType() + "'.");
        }

        if (PROJECTILE_CAST_TYPES.contains(castType) && travelType.isBlank()) {
            audit.warning(scope, "Projectile-family cast has no travel_type, so concept fidelity may be flat.");
        }

        if ((PERSISTENT_CAST_TYPES.contains(castType) || MOVEMENT_CAST_TYPES.contains(castType) || "ground_target".equals(castType))
                && terrainEffect.isBlank()) {
            audit.warning(scope, "This cast family often wants a terrain_effect, but none is set.");
        }

        AbilityActionAssets assets = HytaleAssetResolver.resolve(classId, styleId, ability);
        if (allAssetFieldsBlank(assets)) {
            audit.error(scope, "No animation, effect, or model assets resolve for this ability.");
        }

        auditConceptFit(style, ability, castType, targetType, summonName, audit);
    }

    private static void auditConceptFit(StyleData style,
                                        AbilityData ability,
                                        String castType,
                                        String targetType,
                                        String summonName,
                                        AuditBuilder audit) {
        String abilityId = safeLower(ability.getId());
        String scope = "ability:" + safeLabel(abilityId);
        String concept = buildConceptText(style, ability);

        if (containsAny(concept, "form", "transform") && !"transformation".equals(castType)) {
            audit.warning(scope, "Concept text suggests a transformation, but cast_type is '" + ability.getCastType() + "'.");
        }

        if (containsAny(concept, "summon", "spawn", "raise dead") && summonName.isBlank()) {
            audit.warning(scope, "Concept text suggests a summon, but summon_name is missing.");
        }

        if (containsAny(concept, "wall", "barrier") && !"barrier".equals(castType)) {
            audit.warning(scope, "Concept text suggests a barrier/wall, but cast_type is '" + ability.getCastType() + "'.");
        }

        if (containsAny(concept, "dash", "leap", "teleport", "burrow", "tunnel", "skate")
                && !MOVEMENT_CAST_TYPES.contains(castType)
                && !"transformation".equals(castType)) {
            audit.warning(scope, "Concept text suggests movement utility, but cast_type is '" + ability.getCastType() + "'.");
        }

        if (containsAny(concept, "shot", "bolt", "ball", "needles", "slash", "cutter", "wave", "smite")
                && !PROJECTILE_CAST_TYPES.contains(castType)
                && !"cone".equals(castType)
                && !"line_control".equals(castType)) {
            audit.warning(scope, "Concept text suggests a projectile/line attack, but cast_type is '" + ability.getCastType() + "'.");
        }

        if (containsAny(concept, "storm", "pool", "ground", "zone", "cloud", "aura", "field")
                && !PERSISTENT_CAST_TYPES.contains(castType)
                && !"ground_target".equals(castType)
                && !"self_buff".equals(castType)
                && ability.getDurationSeconds() <= 0) {
            audit.warning(scope, "Concept text suggests a lasting field/aura, but no lasting runtime shape is encoded.");
        }

        if (containsAny(concept, "self", "yourself", "your attack", "your speed")
                && "enemy".equals(targetType)
                && !"self_buff".equals(castType)) {
            audit.warning(scope, "Concept text sounds self-targeted, but target_type is enemy.");
        }
    }

    private static boolean allAssetFieldsBlank(AbilityActionAssets assets) {
        if (assets == null) {
            return true;
        }
        return isBlank(assets.getAnimationAsset())
                && isBlank(assets.getCastEffectAsset())
                && isBlank(assets.getTravelEffectAsset())
                && isBlank(assets.getImpactEffectAsset())
                && isBlank(assets.getLoopEffectAsset())
                && isBlank(assets.getModelAsset());
    }

    private static Path writeReport(Path pluginDirectory, AuditBuilder audit) {
        if (pluginDirectory == null) {
            return null;
        }

        try {
            Files.createDirectories(pluginDirectory);
            Path reportPath = pluginDirectory.resolve(REPORT_FILE_NAME);
            Files.writeString(reportPath, audit.toDetailedText(), StandardCharsets.UTF_8);
            return reportPath;
        } catch (IOException e) {
            LOG.warning("[MOTM] Failed to write preflight audit report: " + e.getMessage());
            return null;
        }
    }

    private static void logSummary(AuditReport report) {
        String banner = report.isHealthy()
                ? "[MOTM] Preflight audit: READY for live gameplay validation."
                : "[MOTM] Preflight audit: ISSUES found before live gameplay validation.";
        LOG.info(banner);
        LOG.info("[MOTM] Classes=" + report.classCount()
                + " Styles=" + report.styleCount()
                + " Abilities=" + report.abilityCount()
                + " Errors=" + report.errorCount()
                + " Warnings=" + report.warningCount());

        for (Finding finding : report.topFindings(6)) {
            LOG.info("[MOTM] " + finding.severity() + " " + finding.scope() + " - " + finding.message());
        }

        if (report.reportPath() != null) {
            LOG.info("[MOTM] Preflight report written to " + report.reportPath());
        }
    }

    private static String buildConceptText(StyleData style, AbilityData ability) {
        List<String> parts = new ArrayList<>();
        if (style != null) {
            parts.add(style.getName());
            parts.add(style.getTheme());
        }
        if (ability != null) {
            parts.add(ability.getName());
            parts.add(ability.getDescription());
            parts.add(ability.getEffect());
            if (ability.getCategories() != null) {
                parts.add(String.join(" ", ability.getCategories()));
            }
        }
        return safeLower(String.join(" ", parts));
    }

    private static boolean containsAny(String text, String... needles) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String safeLower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safeLabel(String value) {
        return isBlank(value) ? "unknown" : value;
    }

    public record Finding(String severity, String scope, String message) {
    }

    public record AuditReport(
            int classCount,
            int styleCount,
            int abilityCount,
            int errorCount,
            int warningCount,
            List<Finding> errors,
            List<Finding> warnings,
            List<Finding> infos,
            Path reportPath,
            Path pluginDirectory
    ) {
        public boolean isHealthy() {
            return errorCount == 0;
        }

        public List<Finding> topFindings(int limit) {
            List<Finding> result = new ArrayList<>();
            for (Finding finding : errors) {
                if (result.size() >= limit) {
                    return result;
                }
                result.add(finding);
            }
            for (Finding finding : warnings) {
                if (result.size() >= limit) {
                    return result;
                }
                result.add(finding);
            }
            for (Finding finding : infos) {
                if (result.size() >= limit) {
                    return result;
                }
                result.add(finding);
            }
            return result;
        }

        public String toChatSummary() {
            StringBuilder sb = new StringBuilder("[MOTM] Preflight Audit\n");
            sb.append("Classes: ").append(classCount)
                    .append(" | Styles: ").append(styleCount)
                    .append(" | Abilities: ").append(abilityCount).append("\n");
            sb.append("Errors: ").append(errorCount)
                    .append(" | Warnings: ").append(warningCount).append("\n");
            sb.append("Status: ").append(isHealthy()
                    ? "Ready for live gameplay validation"
                    : "Needs fixes before confident live testing");
            if (reportPath != null) {
                sb.append("\nReport: ").append(reportPath.getFileName());
            }

            List<Finding> findings = topFindings(5);
            if (!findings.isEmpty()) {
                sb.append("\nTop findings:");
                for (Finding finding : findings) {
                    sb.append("\n  - ").append(finding.scope()).append(": ").append(finding.message());
                }
            }
            return sb.toString();
        }
    }

    private static final class AuditBuilder {
        private final List<Finding> errors = new ArrayList<>();
        private final List<Finding> warnings = new ArrayList<>();
        private final List<Finding> infos = new ArrayList<>();
        private int classCount;
        private int styleCount;
        private int abilityCount;

        private void error(String scope, String message) {
            errors.add(new Finding("ERROR", scope, message));
        }

        private void warning(String scope, String message) {
            warnings.add(new Finding("WARN", scope, message));
        }

        private void info(String scope, String message) {
            infos.add(new Finding("INFO", scope, message));
        }

        private AuditReport build(Path reportPath, Path pluginDirectory) {
            return new AuditReport(
                    classCount,
                    styleCount,
                    abilityCount,
                    errors.size(),
                    warnings.size(),
                    List.copyOf(errors),
                    List.copyOf(warnings),
                    List.copyOf(infos),
                    reportPath,
                    pluginDirectory
            );
        }

        private String toDetailedText() {
            StringBuilder sb = new StringBuilder();
            sb.append("MOTM PREFLIGHT AUDIT\n");
            sb.append("════════════════════\n");
            sb.append("Classes   : ").append(classCount).append("\n");
            sb.append("Styles    : ").append(styleCount).append("\n");
            sb.append("Abilities : ").append(abilityCount).append("\n");
            sb.append("Errors    : ").append(errors.size()).append("\n");
            sb.append("Warnings  : ").append(warnings.size()).append("\n");
            sb.append("Status    : ").append(errors.isEmpty()
                    ? "READY FOR LIVE GAMEPLAY VALIDATION"
                    : "FIX ISSUES BEFORE LIVE GAMEPLAY VALIDATION").append("\n");

            appendSection(sb, "Errors", errors);
            appendSection(sb, "Warnings", warnings);
            appendSection(sb, "Info", infos);
            return sb.toString();
        }

        private void appendSection(StringBuilder sb, String title, List<Finding> findings) {
            sb.append("\n").append(title).append("\n");
            sb.append("────────").append("\n");
            if (findings.isEmpty()) {
                sb.append("(none)\n");
                return;
            }

            for (Finding finding : findings) {
                sb.append("- [").append(finding.scope()).append("] ")
                        .append(finding.message()).append("\n");
            }
        }
    }
}
