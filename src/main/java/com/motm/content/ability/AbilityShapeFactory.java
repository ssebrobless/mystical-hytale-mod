package com.motm.content.ability;

import com.motm.model.AbilityData;
import com.motm.runtime.ability.AbilityRuntimeFamily;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class AbilityShapeFactory {

    private AbilityShapeFactory() {
    }

    public static AbilityShape from(String classId, String styleId, AbilityData ability) {
        String castType = lower(ability != null ? ability.getCastType() : null);
        return new AbilityShape(
                lower(classId),
                lower(styleId),
                lower(ability != null ? ability.getId() : null),
                castType,
                lower(ability != null ? ability.getTargetType() : null),
                parseEffectTokens(ability != null ? ability.getEffect() : null),
                lower(ability != null ? ability.getTerrainEffect() : null),
                lower(ability != null ? ability.getTravelType() : null),
                AbilityRuntimeFamily.fromCastType(castType)
        );
    }

    public static List<String> parseEffectTokens(String effect) {
        List<String> tokens = new ArrayList<>();
        if (effect == null || effect.isBlank()) {
            return tokens;
        }
        for (String rawToken : effect.split("\\+")) {
            String token = lower(rawToken);
            if (!token.isBlank()) {
                tokens.add(token);
            }
        }
        return List.copyOf(tokens);
    }

    public static String lower(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
