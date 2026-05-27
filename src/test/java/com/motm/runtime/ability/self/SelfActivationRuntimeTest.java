package com.motm.runtime.ability.self;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SelfActivationRuntimeTest {
    private final SelfActivationRuntime runtime = new SelfActivationRuntime();

    @Test
    void createsSelfEffectsAndPlayerAnchors() {
        TestRef ownerRef = new TestRef();
        ActiveSelfEffect effect = runtime.createSelfEffect("player", ownerRef, "MOTM_Effect", 5_000L, 1_000L);

        assertEquals("player", effect.ownerPlayerId());
        assertEquals(ownerRef, effect.ownerRef());
        assertEquals("MOTM_Effect", effect.effectId());
        assertEquals(5_000L, effect.expireAtMillis());
        assertEquals(1_000L, effect.nextApplyAtMillis());

        Vector3d anchorPosition = new Vector3d(1.0, 2.0, 3.0);
        ActivePlayerAnchor anchor = runtime.createPlayerAnchor(
                "obsidian_skin",
                "player",
                ownerRef,
                anchorPosition,
                6_000L,
                "MOTM_Complete"
        );

        assertEquals("obsidian_skin", anchor.reason());
        assertEquals("player", anchor.ownerPlayerId());
        assertEquals(ownerRef, anchor.ownerRef());
        assertEquals(6_000L, anchor.expireAtMillis());
        assertEquals("MOTM_Complete", anchor.completionEffectId());
        anchorPosition.x = 99.0;
        assertEquals(1.0, anchor.anchor().x, 0.0001);
    }

    @Test
    void rejectsMissingInputs() {
        assertNull(runtime.createSelfEffect(null, new TestRef(), "MOTM_Effect", 5_000L, 1_000L));
        assertNull(runtime.createSelfEffect("player", new InvalidRef(), "MOTM_Effect", 5_000L, 1_000L));
        assertNull(runtime.createSelfEffect("player", new TestRef(), " ", 5_000L, 1_000L));

        assertNull(runtime.createPlayerAnchor(null, "player", new TestRef(), new Vector3d(), 6_000L, null));
        assertNull(runtime.createPlayerAnchor("obsidian_skin", null, new TestRef(), new Vector3d(), 6_000L, null));
        assertNull(runtime.createPlayerAnchor("obsidian_skin", "player", new InvalidRef(), new Vector3d(), 6_000L, null));
        assertNull(runtime.createPlayerAnchor("obsidian_skin", "player", new TestRef(), null, 6_000L, null));
    }

    private static final class TestRef extends Ref<EntityStore> {
        private TestRef() {
            super(null, 1);
        }

        @Override
        public boolean isValid() {
            return true;
        }
    }

    private static final class InvalidRef extends Ref<EntityStore> {
        private InvalidRef() {
            super(null, 1);
        }

        @Override
        public boolean isValid() {
            return false;
        }
    }
}
