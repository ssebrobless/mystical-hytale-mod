package com.motm.runtime.ability.transformation;

import com.google.gson.Gson;
import org.joml.Vector3d;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class ActiveTransformationTest {

    private static final Gson GSON = new Gson();

    @Test
    void ownsTransformationStateFromResolvedProfile() {
        Vector3d start = new Vector3d(1.0, 2.0, 3.0);
        AbilityData ability = ability("""
                {
                  "id": "t_rex_form",
                  "cast_type": "transformation"
                }
                """);

        ActiveTransformation form = ActiveTransformation.create(
                "player",
                null,
                ability,
                "T Rex Form",
                1234L,
                start
        );

        assertEquals("player", form.playerId());
        assertEquals("t_rex_form", form.abilityId());
        assertEquals(TransformationRuntimeKind.T_REX, form.kind());
        assertEquals("T Rex Form", form.modelId());
        assertEquals(1234L, form.expireAtMillis());
        assertEquals(0.22, form.damageBonus(), 0.0001);
        assertEquals(0.34, form.weaponBonus(), 0.0001);
        assertEquals("stun", form.weaponRiderToken());
        assertEquals(List.of("attack_buff"), form.ownerRuntimeTokens());
        assertEquals(0.0, form.ownerShieldAmount(), 0.0001);
        assertFalse(form.endsWhenGrounded());
        assertEquals("primal power + rampage pressure", form.summary());
        assertEquals(1.0, form.lastOwnerPosition().x, 0.0001);
    }

    @Test
    void clonesLastOwnerPositionOnCreateAndUpdate() {
        Vector3d start = new Vector3d(1.0, 2.0, 3.0);
        ActiveTransformation form = ActiveTransformation.create(
                "player",
                null,
                ability("""
                        {
                          "id": "smoke_form",
                          "cast_type": "transformation"
                        }
                        """),
                "Smoke Form",
                1234L,
                start
        );

        assertNotSame(start, form.lastOwnerPosition());
        start.x = 99.0;
        assertEquals(1.0, form.lastOwnerPosition().x, 0.0001);

        Vector3d next = new Vector3d(4.0, 5.0, 6.0);
        form.updateLastOwnerPosition(next);

        assertNotSame(next, form.lastOwnerPosition());
        next.x = 88.0;
        assertEquals(4.0, form.lastOwnerPosition().x, 0.0001);
    }

    private static AbilityData ability(String json) {
        return GSON.fromJson(json, AbilityData.class);
    }
}
