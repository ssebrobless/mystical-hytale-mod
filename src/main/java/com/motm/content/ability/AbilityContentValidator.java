package com.motm.content.ability;

import com.motm.model.AbilityData;
import com.motm.model.StyleData;
import com.motm.runtime.ability.AbilityRuntimeFamily;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public final class AbilityContentValidator {

    private static final Set<String> CAST_TYPES = Set.of(
            "air_stall", "barrier", "chain", "channel", "cleanse", "cone", "curse",
            "dash", "dash_buff", "dash_strike", "dive_strike", "execute", "gaze",
            "ground_burst", "ground_strike", "ground_target", "ground_zone", "leap",
            "line_control", "projectile", "projectile_burst", "projectile_line",
            "projectile_volley", "self_buff", "self_burst", "summon", "summon_buff",
            "support_zone", "teleport", "transformation", "wave_line"
    );

    private static final Set<String> TARGET_TYPES = Set.of(
            "allied_summons", "cone", "enemy", "enemy_cluster", "ground_target",
            "line", "self", "self_centered"
    );

    private static final Set<String> EFFECT_TOKENS = Set.of(
            "aoe", "attack_buff", "attack_slow", "blind", "burn", "burst", "clone",
            "consume_burn", "damage_buff", "deafen", "defense_buff", "disoriented",
            "dot", "evasion", "evasion_buff", "evasion_zone", "heal", "knockback",
            "lifesteal", "lightning", "lure", "persistent_object", "root",
            "self_burn", "shield", "shocked", "slow", "slow_stack", "speed",
            "stealth", "stun", "stun_if_wall", "summon", "summon_tank",
            "vulnerability"
    );

    private AbilityContentValidator() {
    }

    public static ValidationReport validateStyles(Collection<StyleData> styles) {
        List<String> errors = new ArrayList<>();
        List<AbilityShape> shapes = new ArrayList<>();
        if (styles == null) {
            errors.add("style collection is null");
            return new ValidationReport(List.of(), errors);
        }

        for (StyleData style : styles) {
            if (style == null) {
                errors.add("style entry is null");
                continue;
            }
            String classId = AbilityShapeFactory.lower(style.getClassId());
            String styleId = AbilityShapeFactory.lower(style.getId());
            if (classId.isBlank()) {
                errors.add("style " + styleId + " has blank class_id");
            }
            if (styleId.isBlank()) {
                errors.add("style in class " + classId + " has blank id");
            }
            List<AbilityData> abilities = style.getAbilities();
            if (abilities == null || abilities.size() != 3) {
                errors.add(classId + "/" + styleId + " should have 3 abilities");
                continue;
            }
            for (AbilityData ability : abilities) {
                AbilityShape shape = AbilityShapeFactory.from(classId, styleId, ability);
                shapes.add(shape);
                validateShape(shape, errors);
                if (ability != null && ability.getResourceCost() != 0) {
                    errors.add(shapePath(shape) + " resource_cost must remain 0");
                }
                if (shape.runtimeFamily() == AbilityRuntimeFamily.UNKNOWN) {
                    errors.add(shapePath(shape) + " does not resolve to a runtime family");
                }
            }
        }

        return new ValidationReport(List.copyOf(shapes), List.copyOf(errors));
    }

    private static void validateShape(AbilityShape shape, List<String> errors) {
        if (shape.abilityId().isBlank()) {
            errors.add(shape.classId() + "/" + shape.styleId() + " has ability with blank id");
        }
        if (!CAST_TYPES.contains(shape.castType())) {
            errors.add(shapePath(shape) + " has unsupported cast_type=" + shape.castType());
        }
        if (!TARGET_TYPES.contains(shape.targetType())) {
            errors.add(shapePath(shape) + " has unsupported target_type=" + shape.targetType());
        }
        for (String token : shape.effectTokens()) {
            if (!EFFECT_TOKENS.contains(token)) {
                errors.add(shapePath(shape) + " has unsupported effect token=" + token);
            }
        }
    }

    private static String shapePath(AbilityShape shape) {
        return shape.classId() + "/" + shape.styleId() + "/" + shape.abilityId();
    }

    public record ValidationReport(List<AbilityShape> shapes, List<String> errors) {
        public boolean valid() {
            return errors.isEmpty();
        }

        public void throwIfInvalid() {
            if (!valid()) {
                throw new IllegalStateException("Ability content validation failed: " + String.join("; ", errors));
            }
        }
    }
}
