package com.motm.runtime.ability.self;

import com.hypixel.hytale.math.vector.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SelfRuntimeStateTest {

    @Test
    void replacesPlayerAnchorsPerOwner() {
        SelfRuntimeState state = new SelfRuntimeState();

        state.replacePlayerAnchor("player", anchor("player", 1000L));
        state.replacePlayerAnchor("player", anchor("player", 2000L));

        assertEquals(1, state.activePlayerAnchorCount());
    }

    @Test
    void replacesMatchingSelfEffectButKeepsDifferentEffects() {
        SelfRuntimeState state = new SelfRuntimeState();

        state.replaceSelfEffect("player", "effect-a", effect("player", "effect-a"));
        state.replaceSelfEffect("player", "effect-a", effect("player", "effect-a"));
        state.replaceSelfEffect("player", "effect-b", effect("player", "effect-b"));

        assertEquals(2, state.activeSelfEffectCount());
    }

    @Test
    void removesProcessedAndOwnerOwnedState() {
        SelfRuntimeState state = new SelfRuntimeState();
        state.replacePlayerAnchor("player-a", anchor("player-a", 1000L));
        state.replacePlayerAnchor("player-b", anchor("player-b", 1000L));
        state.replaceSelfEffect("player-a", "effect-a", effect("player-a", "effect-a"));
        state.replaceSelfEffect("player-b", "effect-b", effect("player-b", "effect-b"));

        state.removeProcessedPlayerAnchors(anchor -> "player-a".equals(anchor.ownerPlayerId()));
        state.removeProcessedSelfEffects(effect -> "player-a".equals(effect.ownerPlayerId()));

        assertEquals(1, state.activePlayerAnchorCount());
        assertEquals(1, state.activeSelfEffectCount());
        assertEquals(1, state.removePlayerAnchorsForPlayer("player-b"));
        assertEquals(1, state.removeSelfEffectsForPlayer("player-b"));
        assertEquals(0, state.activePlayerAnchorCount());
        assertEquals(0, state.activeSelfEffectCount());
    }

    private static ActivePlayerAnchor anchor(String ownerPlayerId, long expireAtMillis) {
        return new ActivePlayerAnchor(
                "test",
                ownerPlayerId,
                null,
                new Vector3d(1.0, 2.0, 3.0),
                expireAtMillis,
                null
        );
    }

    private static ActiveSelfEffect effect(String ownerPlayerId, String effectId) {
        return new ActiveSelfEffect(ownerPlayerId, null, effectId, 5000L, 1000L);
    }
}
