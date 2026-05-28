package com.motm.content.ability;

import com.google.gson.Gson;
import com.motm.model.StyleData;
import com.motm.runtime.ability.AbilityRuntimeFamily;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbilityContentValidatorTest {

    private static final Gson GSON = new Gson();

    @Test
    void validatesKnownAbilityShape() {
        StyleData style = parseStyle("""
                {
                  "id": "magma",
                  "name": "Magma",
                  "class_id": "terra",
                  "resource_type": "",
                  "abilities": [
                    {
                      "id": "magma_sling",
                      "name": "Magma Sling",
                      "effect": "burn+slow",
                      "resource_cost": 0,
                      "cast_type": "projectile",
                      "target_type": "enemy",
                      "projectile_speed": 12,
                      "travel_type": "arcing_shot"
                    },
                    {
                      "id": "lava_pool",
                      "name": "Lava Pool",
                      "effect": "burn",
                      "resource_cost": 0,
                      "cast_type": "ground_zone",
                      "target_type": "ground_target",
                      "terrain_effect": "lava_pool"
                    },
                    {
                      "id": "obsidian_skin",
                      "name": "Obsidian Skin",
                      "effect": "shield",
                      "resource_cost": 0,
                      "cast_type": "self_buff",
                      "target_type": "self"
                    }
                  ]
                }
                """);

        AbilityContentValidator.ValidationReport report =
                AbilityContentValidator.validateStyles(List.of(style));

        assertTrue(report.valid(), String.join("; ", report.errors()));
        assertEquals(3, report.shapes().size());
        assertEquals(AbilityRuntimeFamily.PROJECTILE, report.shapes().getFirst().runtimeFamily());
        assertEquals(List.of("burn", "slow"), report.shapes().getFirst().effectTokens());
    }

    @Test
    void rejectsUnsupportedTokensAndResourceCosts() {
        StyleData style = parseStyle("""
                {
                  "id": "bad_style",
                  "name": "Bad",
                  "class_id": "terra",
                  "resource_type": "",
                  "abilities": [
                    {
                      "id": "bad_one",
                      "name": "Bad One",
                      "effect": "mystery",
                      "resource_cost": 0,
                      "cast_type": "projectile",
                      "target_type": "enemy"
                    },
                    {
                      "id": "bad_two",
                      "name": "Bad Two",
                      "effect": "burn",
                      "resource_cost": 3,
                      "cast_type": "not_real",
                      "target_type": "enemy"
                    },
                    {
                      "id": "bad_three",
                      "name": "Bad Three",
                      "effect": "burn",
                      "resource_cost": 0,
                      "cast_type": "projectile",
                      "target_type": "somewhere"
                    }
                  ]
                }
                """);

        AbilityContentValidator.ValidationReport report =
                AbilityContentValidator.validateStyles(List.of(style));

        assertFalse(report.valid());
        String errors = String.join("; ", report.errors());
        assertTrue(errors.contains("unsupported effect token=mystery"));
        assertTrue(errors.contains("unsupported cast_type=not_real"));
        assertTrue(errors.contains("unsupported target_type=somewhere"));
        assertTrue(errors.contains("resource_cost must remain 0"));
    }

    private static StyleData parseStyle(String json) {
        return GSON.fromJson(json, StyleData.class);
    }
}
