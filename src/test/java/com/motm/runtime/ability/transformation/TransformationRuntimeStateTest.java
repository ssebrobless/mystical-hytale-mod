package com.motm.runtime.ability.transformation;

import com.google.gson.Gson;
import org.joml.Vector3d;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransformationRuntimeStateTest {
    private static final Gson GSON = new Gson();

    @Test
    void ownsTransformationMapAndPulseSchedule() {
        TransformationRuntimeState state = new TransformationRuntimeState();
        ActiveTransformation form = transformation("player", "smoke_form");

        state.putTransformation("player", form, 1200L);

        assertTrue(state.containsTransformation("player"));
        assertEquals(1, state.activeTransformationCount());
        assertEquals(form, state.getTransformation("player"));
        assertEquals(1200L, state.nextPulseAt("player", 0L));

        state.scheduleNextPulse("player", 1800L);
        assertEquals(1800L, state.nextPulseAt("player", 0L));
    }

    @Test
    void removesTransformationAndAssociatedPulseState() {
        TransformationRuntimeState state = new TransformationRuntimeState();
        state.putTransformation("player", transformation("player", "smoke_form"), 1200L);

        assertNotNull(state.removeTransformation("player"));

        assertFalse(state.containsTransformation("player"));
        assertEquals(99L, state.nextPulseAt("player", 99L));
    }

    @Test
    void removesByAbilityOnlyWhenAbilityMatches() {
        TransformationRuntimeState state = new TransformationRuntimeState();
        state.putTransformation("player", transformation("player", "smoke_form"), 1200L);

        assertNull(state.removeTransformationForAbility("player", "t_rex_form"));
        assertTrue(state.containsTransformation("player"));
        assertNotNull(state.removeTransformationForAbility("player", "smoke_form"));
        assertFalse(state.containsTransformation("player"));
    }

    @Test
    void removesProcessedTransformationsAndClearsPulse() {
        TransformationRuntimeState state = new TransformationRuntimeState();
        state.putTransformation("player-a", transformation("player-a", "smoke_form"), 1200L);
        state.putTransformation("player-b", transformation("player-b", "t_rex_form"), 1400L);

        state.removeProcessedTransformations(form -> "player-a".equals(form.playerId()));

        assertFalse(state.containsTransformation("player-a"));
        assertTrue(state.containsTransformation("player-b"));
        assertEquals(99L, state.nextPulseAt("player-a", 99L));
        assertEquals(1400L, state.nextPulseAt("player-b", 99L));
    }

    private static ActiveTransformation transformation(String playerId, String abilityId) {
        return ActiveTransformation.create(
                playerId,
                null,
                ability(abilityId),
                abilityId,
                5000L,
                new Vector3d(1.0, 2.0, 3.0)
        );
    }

    private static AbilityData ability(String id) {
        return GSON.fromJson("{\"id\":\"" + id + "\",\"cast_type\":\"transformation\"}", AbilityData.class);
    }
}
