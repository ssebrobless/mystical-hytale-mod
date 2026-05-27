package com.motm.runtime.ability.followup;

import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertFalse;

class WeaponFollowUpDurabilityRestorerTest {

    @Test
    void ignoresMissingPlayerOrItem() {
        Logger logger = Logger.getLogger("test");

        assertFalse(WeaponFollowUpDurabilityRestorer.restoreHeldItemDurability(null, "hytale:pickaxe", logger));
        assertFalse(WeaponFollowUpDurabilityRestorer.restoreHeldItemDurability(null, "", logger));
        assertFalse(WeaponFollowUpDurabilityRestorer.restoreHeldItemDurability(null, null, logger));
    }
}
