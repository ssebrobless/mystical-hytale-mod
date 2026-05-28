package com.motm.runtime.ability.followup;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WeaponFollowUpNativeAlloyRuntimeTest {
    private static final Gson GSON = new Gson();

    private final WeaponFollowUpNativeAlloyRuntime runtime = new WeaponFollowUpNativeAlloyRuntime();

    @Test
    void appliesNativeDamageOrderingAndLogsMessage() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveWeaponFollowUp followUp = alloyFollowUp();

        String message = runtime.applyNativeDamage(followUp, "hytale:pickaxe", 10.0f, hooks);

        assertEquals("[MOTM] Alloy Enhancement hit: 10 -> 13 damage | durability protected | 2 use(s) left", message);
        assertEquals(13.0f, hooks.damageAmount, 0.0001f);
        assertEquals(List.of(
                "bound:player:hytale:pickaxe:3",
                "applyVisual",
                "setDamage:13.0",
                "impact",
                "secondary:vulnerability",
                "restore:hytale:pickaxe",
                "log:[MOTM] Alloy Enhancement hit: 10 -> 13 damage | durability protected | 2 use(s) left:player:hytale:pickaxe"
        ), hooks.events);
    }

    @Test
    void removesAndClearsVisualWhenNativeUsesAreExhausted() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveWeaponFollowUp followUp = alloyFollowUp();
        followUp.decrementRemainingUses();
        followUp.decrementRemainingUses();

        String message = runtime.applyNativeDamage(followUp, "hytale:pickaxe", 8.0f, hooks);

        assertEquals("[MOTM] Alloy Enhancement hit: 8 -> 10.4 damage | durability protected | Alloy finished", message);
        assertEquals(List.of(
                "bound:player:hytale:pickaxe:1",
                "applyVisual",
                "setDamage:10.4",
                "impact",
                "secondary:vulnerability",
                "restore:hytale:pickaxe",
                "remove:player",
                "clearVisual",
                "log:[MOTM] Alloy Enhancement hit: 8 -> 10.4 damage | durability protected | Alloy finished:player:hytale:pickaxe"
        ), hooks.events);
    }

    @Test
    void rejectsSwitchedNativeItemWithoutMutatingVisuals() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveWeaponFollowUp followUp = alloyFollowUp();
        followUp.bindItemId("hytale:pickaxe");

        String message = runtime.applyNativeDamage(followUp, "hytale:axe", 10.0f, hooks);

        assertEquals("[MOTM] Alloy Enhancement ended: switched from hytale:pickaxe to hytale:axe.", message);
        assertEquals(List.of(
                "remove:player",
                "rejected:[MOTM] Alloy Enhancement ended: switched from hytale:pickaxe to hytale:axe.:player"
        ), hooks.events);
    }

    @Test
    void toolUseClearsVisualWhenSwitchedItemIsRejected() {
        RecordingHooks hooks = new RecordingHooks();
        ActiveWeaponFollowUp followUp = alloyFollowUp();
        followUp.bindItemId("hytale:pickaxe");

        String message = runtime.applyToolUse(followUp, "hytale:axe", hooks);

        assertEquals("[MOTM] Alloy Enhancement ended: switched from hytale:pickaxe to hytale:axe.", message);
        assertEquals(List.of(
                "remove:player",
                "rejected:[MOTM] Alloy Enhancement ended: switched from hytale:pickaxe to hytale:axe.:player",
                "clearVisual"
        ), hooks.events);
    }

    @Test
    void toolUseTracksDurabilityAndFinishesAfterLastUse() {
        RecordingHooks hooks = new RecordingHooks();
        hooks.restored = false;
        ActiveWeaponFollowUp followUp = alloyFollowUp();
        followUp.decrementRemainingUses();
        followUp.decrementRemainingUses();

        String message = runtime.applyToolUse(followUp, "hytale:pickaxe", hooks);

        assertEquals("[MOTM] Alloy durability shield: tracked hytale:pickaxe use | Alloy finished", message);
        assertEquals(List.of(
                "bound:player:hytale:pickaxe:1",
                "applyVisual",
                "restore:hytale:pickaxe",
                "remove:player",
                "clearVisual"
        ), hooks.events);
    }

    @Test
    void ignoresNonAlloyOrMissingHooks() {
        RecordingHooks hooks = new RecordingHooks();

        assertNull(runtime.applyNativeDamage(null, "hytale:pickaxe", 10.0f, hooks));
        assertNull(runtime.applyNativeDamage(alloyFollowUp(), "", 10.0f, hooks));
        assertNull(runtime.applyNativeDamage(alloyFollowUp(), "hytale:pickaxe", 10.0f, null));
        assertNull(runtime.applyToolUse(null, "hytale:pickaxe", hooks));
        assertEquals(List.of(), hooks.events);
    }

    private static ActiveWeaponFollowUp alloyFollowUp() {
        AbilityData ability = GSON.fromJson("""
                {
                  "id": "alloy_enhancement",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """, AbilityData.class);
        return ActiveWeaponFollowUp.create("player", ability, 1000L, WeaponFollowUpSpecs.resolve(ability));
    }

    private static final class RecordingHooks implements WeaponFollowUpNativeAlloyRuntime.Hooks {
        private final List<String> events = new ArrayList<>();
        private boolean restored = true;
        private float damageAmount;

        @Override
        public void applyVisual() {
            events.add("applyVisual");
        }

        @Override
        public void clearVisual() {
            events.add("clearVisual");
        }

        @Override
        public void setDamage(float amount) {
            damageAmount = amount;
            events.add("setDamage:" + amount);
        }

        @Override
        public void applyImpact() {
            events.add("impact");
        }

        @Override
        public void applySecondaryRiderToken(String token) {
            events.add("secondary:" + token);
        }

        @Override
        public boolean restoreDurability(String itemId) {
            events.add("restore:" + itemId);
            return restored;
        }

        @Override
        public void removeFollowUp(String playerId) {
            events.add("remove:" + playerId);
        }

        @Override
        public void logNativeDamage(String message, String playerId, String itemId) {
            events.add("log:" + message + ":" + playerId + ":" + itemId);
        }

        @Override
        public void logBound(String playerId, String itemId, int remainingUses) {
            events.add("bound:" + playerId + ":" + itemId + ":" + remainingUses);
        }

        @Override
        public void logRejected(String message, String playerId) {
            events.add("rejected:" + message + ":" + playerId);
        }
    }
}
