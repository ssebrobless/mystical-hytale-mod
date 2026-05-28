package com.motm.runtime.ability.followup;

import com.google.gson.Gson;
import com.motm.model.AbilityData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ActiveWeaponFollowUpTest {

    private static final Gson GSON = new Gson();

    @Test
    void ownsUseCountdownAndBoundItemState() {
        AbilityData ability = GSON.fromJson("""
                {
                  "id": "alloy_enhancement",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """, AbilityData.class);
        WeaponFollowUpSpec spec = WeaponFollowUpSpecs.resolve(ability);
        ActiveWeaponFollowUp followUp =
                ActiveWeaponFollowUp.create("player", ability, 1234L, spec);

        assertEquals("player", followUp.playerId());
        assertEquals("alloy_enhancement", followUp.sourceAbilityId());
        assertEquals(1234L, followUp.expireAtMillis());
        assertEquals(3, followUp.remainingUses());
        assertNull(followUp.boundItemId());

        followUp.bindItemId("hytale:pickaxe");

        assertEquals("hytale:pickaxe", followUp.boundItemId());
        assertEquals(2, followUp.decrementRemainingUses());
        assertEquals(2, followUp.remainingUses());
    }

    @Test
    void bindsFirstAlloyItemAndRejectsSwitchedItems() {
        AbilityData ability = GSON.fromJson("""
                {
                  "id": "alloy_enhancement",
                  "cast_type": "self_buff",
                  "effect": "damage_buff"
                }
                """, AbilityData.class);
        ActiveWeaponFollowUp followUp =
                ActiveWeaponFollowUp.create("player", ability, 1000L, WeaponFollowUpSpecs.resolve(ability));

        WeaponFollowUpItemBinding first = followUp.bindOrRejectItem("hytale:pickaxe");
        WeaponFollowUpItemBinding repeat = followUp.bindOrRejectItem("HYTALE:PICKAXE");
        WeaponFollowUpItemBinding switched = followUp.bindOrRejectItem("hytale:axe");

        assertEquals("hytale:pickaxe", followUp.boundItemId());
        assertEquals(true, first.accepted());
        assertEquals(true, first.newlyBound());
        assertEquals(true, repeat.accepted());
        assertEquals(false, repeat.newlyBound());
        assertEquals(false, switched.accepted());
        assertEquals("[MOTM] Alloy Enhancement ended: switched from hytale:pickaxe to hytale:axe.", switched.message());
    }
}
