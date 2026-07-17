package com.motm.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Fail-closed, immutable lookup table for the per-ability visual contract.
 * A present row is authoritative, including explicit nulls; callers must not
 * silently merge it with branch-derived values.
 */
public final class AbilityVisualManifest {
    private static volatile AbilityVisualManifest current = empty();

    private final Map<String, Row> rows;

    private AbilityVisualManifest(Map<String, Row> rows) {
        this.rows = Collections.unmodifiableMap(new LinkedHashMap<>(rows));
    }

    public static AbilityVisualManifest empty() {
        return new AbilityVisualManifest(Map.of());
    }

    public static AbilityVisualManifest fromJson(JsonObject root) {
        if (root == null
                || !root.has("schemaVersion")
                || root.get("schemaVersion").getAsInt() != 1
                || !root.has("abilities")
                || !root.get("abilities").isJsonObject()) {
            throw new IllegalArgumentException("Manifest schemaVersion 1 with an abilities object is required");
        }

        Map<String, Row> parsed = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> entry : root.getAsJsonObject("abilities").entrySet()) {
            String abilityId = normalize(entry.getKey());
            if (abilityId.isBlank() || !entry.getValue().isJsonObject()) {
                throw new IllegalArgumentException("Manifest ability row is invalid: " + entry.getKey());
            }
            if (parsed.put(abilityId, Row.fromJson(entry.getValue().getAsJsonObject())) != null) {
                throw new IllegalArgumentException("Duplicate manifest ability id: " + abilityId);
            }
        }
        return new AbilityVisualManifest(parsed);
    }

    public static void install(AbilityVisualManifest manifest) {
        current = manifest == null ? empty() : manifest;
    }

    public static AbilityVisualManifest current() {
        return current;
    }

    public Row lookup(String abilityId) {
        return rows.get(normalize(abilityId));
    }

    public Map<String, Row> rows() {
        return rows;
    }

    public int size() {
        return rows.size();
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record Row(String cast,
                      String travel,
                      String impact,
                      String loop,
                      String model,
                      String role,
                      String projectileConfig,
                      boolean legacy) {
        private static Row fromJson(JsonObject object) {
            for (String key : new String[]{"cast", "travel", "impact", "loop",
                    "model", "role", "projectileConfig"}) {
                if (!object.has(key)) {
                    throw new IllegalArgumentException("Manifest row is missing field: " + key);
                }
            }
            return new Row(value(object, "cast"),
                    value(object, "travel"),
                    value(object, "impact"),
                    value(object, "loop"),
                    value(object, "model"),
                    value(object, "role"),
                    value(object, "projectileConfig"),
                    object.has("legacy") && !object.get("legacy").isJsonNull()
                            && object.get("legacy").getAsBoolean());
        }

        private static String value(JsonObject object, String key) {
            JsonElement value = object.get(key);
            return value == null || value.isJsonNull() ? null : value.getAsString();
        }
    }
}
