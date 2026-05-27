package com.motm.runtime.ability.self;

import com.hypixel.hytale.math.vector.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActivePlayerAnchorTest {

    @Test
    void ownsAnchorStateAndDefensivePositionCopy() {
        Vector3d anchorPosition = new Vector3d(1.0, 2.0, 3.0);
        ActivePlayerAnchor anchor = new ActivePlayerAnchor(
                "obsidian_skin",
                "player",
                null,
                anchorPosition,
                5_000L,
                "effect"
        );

        assertEquals("obsidian_skin", anchor.reason());
        assertEquals("player", anchor.ownerPlayerId());
        assertEquals("effect", anchor.completionEffectId());
        assertTrue(anchor.expired(5_000L));
        assertNotSame(anchorPosition, anchor.anchor());
        anchorPosition.x = 99.0;
        assertEquals(1.0, anchor.anchor().x, 0.0001);
    }
}
