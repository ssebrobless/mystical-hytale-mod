package com.motm.runtime.ability.field;

import com.google.gson.Gson;
import org.joml.Vector3d;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldRuntimeStateTest {
    private static final Gson GSON = new Gson();

    @Test
    void ownsActiveFieldCollectionAndRemovalCleanup() {
        FieldRuntimeState state = new FieldRuntimeState();
        ActiveField first = field("player-a", "iron_wall", 1000L);
        ActiveField second = field("player-b", "lava_pool", 2000L);
        List<String> cleaned = new ArrayList<>();

        int added = state.addFields(List.of(first, second));

        assertEquals(2, added);
        assertEquals(2, state.activeFieldCount());
        int removed = state.removeFieldsForPlayer("player-a", field -> cleaned.add(field.ownerPlayerId()));

        assertEquals(1, removed);
        assertEquals(List.of("player-a"), cleaned);
        assertEquals(1, state.activeFieldCount());

        state.removeProcessedFields(field -> "player-b".equals(field.ownerPlayerId()));
        assertEquals(0, state.activeFieldCount());
    }

    @Test
    void removesFieldsForSpecificAbilityOnly() {
        FieldRuntimeState state = new FieldRuntimeState();
        state.addField(field("player", "iron_wall", 1000L));
        state.addField(field("player", "lava_pool", 2000L));

        int removed = state.removeFieldsForAbility("player", "iron_wall", null);

        assertEquals(1, removed);
        assertEquals(1, state.activeFieldCount());
    }

    @Test
    void ownsBuriedVictimEntriesIncludingEmptyEngagedFields() {
        FieldRuntimeState state = new FieldRuntimeState();
        ActiveField field = field("player", "sinkhole", 1000L);
        BuriedVictim victim = new BuriedVictim(null, null, 5000L);

        assertFalse(state.hasBurialEntry(field));
        state.putBuriedVictims(field, List.of());
        assertTrue(state.hasBurialEntry(field));
        assertTrue(state.buriedVictims(field).isEmpty());

        state.putBuriedVictims(field, List.of(victim));
        assertEquals(List.of(victim), state.buriedVictims(field));
        assertEquals(List.of(victim), state.removeBuriedVictims(field));
        assertFalse(state.hasBurialEntry(field));
    }

    private static ActiveField field(String ownerPlayerId, String abilityId, long activateAtMillis) {
        return new ActiveField(
                ownerPlayerId,
                null,
                "terra",
                "iron",
                ability(abilityId),
                new Vector3d(1.0, 2.0, 3.0),
                new Vector3d(0.0, 0.0, 1.0),
                new Vector3d(1.0, 0.0, 0.0),
                3.0,
                4.0,
                1.0,
                activateAtMillis + 4000L,
                activateAtMillis,
                activateAtMillis,
                false,
                List.of(),
                null,
                activateAtMillis,
                null
        );
    }

    private static AbilityData ability(String id) {
        return GSON.fromJson("{\"id\":\"" + id + "\",\"cast_type\":\"ground_target\"}", AbilityData.class);
    }
}
