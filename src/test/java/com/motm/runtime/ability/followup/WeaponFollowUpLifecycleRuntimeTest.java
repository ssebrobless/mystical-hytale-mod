package com.motm.runtime.ability.followup;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WeaponFollowUpLifecycleRuntimeTest {
    private static final Gson GSON = new Gson();
    private final WeaponFollowUpLifecycleRuntime runtime = new WeaponFollowUpLifecycleRuntime();

    @Test
    void removesNullOrExpiredNonAlloyWithoutCleanupHooks() {
        RecordingHooks hooks = new RecordingHooks();

        assertTrue(runtime.processExpiry("player", null, 1_000L, hooks));
        assertEquals(List.of(), hooks.events);

        ActiveWeaponFollowUp followUp = followUp("refraction", 1_000L);
        assertTrue(runtime.processExpiry("player", followUp, 1_000L, hooks));
        assertEquals(List.of(), hooks.events);
    }

    @Test
    void keepsActiveFollowUp() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveWeaponFollowUp followUp = followUp("alloy_enhancement", 5_000L);

        assertFalse(runtime.processExpiry("player", followUp, 1_000L, hooks));
        assertEquals(List.of(), hooks.events);
    }

    @Test
    void defersAlloyExpiryWhenPlayerIsInDifferentStore() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.canMutate = false;
        ActiveWeaponFollowUp followUp = followUp("alloy_enhancement", 1_000L);

        assertFalse(runtime.processExpiry("player", followUp, 1_000L, hooks));
        assertEquals(List.of("available", "canMutate"), hooks.events);
    }

    @Test
    void clearsVisualAndLogsDurationExpiryForAlloy() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveWeaponFollowUp followUp = followUp("alloy_enhancement", 1_000L);

        assertTrue(runtime.processExpiry("player", followUp, 1_000L, hooks));
        assertEquals(List.of("available", "canMutate", "clear", "ended:DURATION_EXPIRED"), hooks.events);
    }

    @Test
    void logsUnavailablePlayerAndRemovesAlloy() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.playerAvailable = false;
        ActiveWeaponFollowUp followUp = followUp("alloy_enhancement", 1_000L);

        assertTrue(runtime.processExpiry("player", followUp, 1_000L, hooks));
        assertEquals(List.of("available", "skipped", "ended:DURATION_EXPIRED"), hooks.events);
    }

    @Test
    void logsUsesExhaustedForAlloy() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveWeaponFollowUp followUp = followUp("alloy_enhancement", 5_000L);
        followUp.decrementRemainingUses();
        followUp.decrementRemainingUses();
        followUp.decrementRemainingUses();

        assertTrue(runtime.processExpiry("player", followUp, 1_000L, hooks));
        assertEquals(List.of("available", "canMutate", "clear", "ended:USES_EXHAUSTED"), hooks.events);
    }

    private static ActiveWeaponFollowUp followUp(String abilityId, long expireAtMillis) {
        AbilityData ability = GSON.fromJson("""
                {
                  "id": "%s",
                  "name": "%s",
                  "cast_type": "self_buff",
                  "effect": "damage_buff",
                  "duration_seconds": 5.0
                }
                """.formatted(abilityId, abilityId), AbilityData.class);
        return ActiveWeaponFollowUp.create("player", ability, expireAtMillis, WeaponFollowUpSpecs.resolve(ability));
    }

    private static final class RecordingHooks implements WeaponFollowUpLifecycleRuntime.Hooks {
        private final List<String> events = new ArrayList<>();
        private boolean playerAvailable = true;
        private boolean canMutate = true;

        @Override
        public boolean playerAvailable(String playerId) {
            events.add("available");
            return playerAvailable;
        }

        @Override
        public boolean canMutateVisual(String playerId) {
            events.add("canMutate");
            return canMutate;
        }

        @Override
        public void clearVisual(String playerId) {
            events.add("clear");
        }

        @Override
        public void logVisualClearSkipped(String playerId) {
            events.add("skipped");
        }

        @Override
        public void logEnded(String playerId, WeaponFollowUpLifecycleRuntime.EndReason reason) {
            events.add("ended:" + reason.name());
        }
    }
}
