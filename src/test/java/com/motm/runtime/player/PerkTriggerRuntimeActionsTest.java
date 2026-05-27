package com.motm.runtime.player;

import com.google.gson.Gson;
import com.motm.model.Perk;
import com.motm.model.PerkTriggerBinding;
import com.motm.runtime.state.PerkTriggerRuntimeState;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerkTriggerRuntimeActionsTest {

    private static final Gson GSON = new Gson();

    @Test
    void buildsBindingFromValueHealAndHealAmountFields() {
        Perk valuePerk = perk("""
                {"id":"value_perk","effects":[{"type":"on_kill","value":0.2}]}
                """);
        Perk healPerk = perk("""
                {"id":"heal_perk","effects":[{"type":"on_kill","heal":0.3}]}
                """);
        Perk healAmountPerk = perk("""
                {"id":"heal_amount_perk","effects":[{"type":"on_kill","heal_amount":0.4}]}
                """);

        assertEquals(0.2, PerkTriggerRuntimeActions.binding(
                valuePerk,
                valuePerk.getEffects().getFirst()
        ).value());
        assertEquals(0.3, PerkTriggerRuntimeActions.binding(
                healPerk,
                healPerk.getEffects().getFirst()
        ).value());
        assertEquals(0.4, PerkTriggerRuntimeActions.binding(
                healAmountPerk,
                healAmountPerk.getEffects().getFirst()
        ).value());
        assertNull(PerkTriggerRuntimeActions.binding(null, healAmountPerk.getEffects().getFirst()));
    }

    @Test
    void registersClearsAndAppliesKillTriggersThroughStateOwner() {
        PerkTriggerRuntimeActions actions = new PerkTriggerRuntimeActions(
                new PerkTriggerRuntimeState(),
                Logger.getLogger("test")
        );
        Perk perk = perk("""
                {"id":"killer","effects":[{"type":"on_kill","value":0.25}]}
                """);

        actions.register("player", perk, perk.getEffects().getFirst());

        PerkTriggerBinding binding = actions.get("player", "ON_KILL").getFirst();
        assertEquals("killer", binding.perkId());
        assertEquals(0.25, binding.value());
        assertEquals(0, actions.applyKillTriggers("player", null));

        actions.clear("player");

        assertTrue(actions.get("player", "on_kill").isEmpty());
    }

    private static Perk perk(String json) {
        return GSON.fromJson(json, Perk.class);
    }
}
