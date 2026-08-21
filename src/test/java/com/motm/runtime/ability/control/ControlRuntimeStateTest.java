package com.motm.runtime.ability.control;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlRuntimeStateTest {

    @Test
    void tracksCountsPerOwner() {
        ControlRuntimeState state = new ControlRuntimeState();
        state.add("A", ally("mob-1", new TestRef()));
        state.add("A", ally("mob-2", new TestRef()));
        state.add("B", ally("mob-3", new TestRef()));

        assertEquals(2, state.activeOwnerCount());
        assertEquals(3, state.activeControlledCount());
        assertEquals(2, state.controlledCountForOwner("A"));
        assertEquals(1, state.controlledCountForOwner("B"));
        assertEquals(0, state.controlledCountForOwner("missing"));
    }

    @Test
    void ignoresBlankOwnerOrNullAlly() {
        ControlRuntimeState state = new ControlRuntimeState();
        state.add("", ally("mob-1", new TestRef()));
        state.add("A", null);
        assertEquals(0, state.activeControlledCount());
        assertTrue(state.alliesForOwner("A").isEmpty());
    }

    @Test
    void resolvesControlledEntityLookups() {
        ControlRuntimeState state = new ControlRuntimeState();
        ActiveControlledAlly a = ally("mob-1", new TestRef());
        state.add("A", a);

        assertTrue(state.isControlledEntity("mob-1"));
        assertFalse(state.isControlledEntity("mob-999"));
        assertSame(a, state.findByControlledEntityId("mob-1"));
        assertNull(state.findByControlledEntityId("mob-999"));
        assertNull(state.findByControlledEntityId(null));
        assertEquals(java.util.Set.of("mob-1"), state.controlledEntityIdsForOwner("A"));
    }

    @Test
    void resolvesByControlledRefIdentity() {
        ControlRuntimeState state = new ControlRuntimeState();
        Ref<EntityStore> ref = new TestRef();
        ActiveControlledAlly a = ally("mob-1", ref);
        state.add("A", a);

        assertSame(a, state.findByControlledRef(ref));
        assertNull(state.findByControlledRef(new TestRef()));
        assertNull(state.findByControlledRef(null));
    }

    @Test
    void removeForOwnerRunsCleanupPerEntry() {
        ControlRuntimeState state = new ControlRuntimeState();
        state.add("A", ally("mob-1", new TestRef()));
        state.add("A", ally("mob-2", new TestRef()));
        AtomicInteger cleaned = new AtomicInteger();

        int count = state.removeForOwner("A", ally -> {
            cleaned.incrementAndGet();
            return true;
        });

        assertEquals(2, count);
        assertEquals(2, cleaned.get());
        assertEquals(0, state.controlledCountForOwner("A"));
        assertEquals(0, state.activeOwnerCount());
    }

    @Test
    void removeProcessedRemovesEndedAndPrunesEmptyOwners() {
        ControlRuntimeState state = new ControlRuntimeState();
        ActiveControlledAlly keep = ally("keep", new TestRef());
        ActiveControlledAlly drop = ally("drop", new TestRef());
        state.add("A", keep);
        state.add("A", drop);
        state.add("B", ally("drop", new TestRef()));

        state.removeProcessed(ally -> "drop".equals(ally.controlledEntityId()));

        assertEquals(List.of(keep), state.alliesForOwner("A"));
        assertEquals(0, state.controlledCountForOwner("B"));
        assertEquals(1, state.activeOwnerCount());
    }

    private static ActiveControlledAlly ally(String controlledEntityId, Ref<EntityStore> controlledRef) {
        return new ActiveControlledAlly(
                "player",
                controlledRef,
                new TestRef(),
                controlledEntityId,
                "corruptus",
                "mentokinesis",
                null,
                6.0,
                16.0,
                800L,
                "control_strike",
                10_000L,
                0L,
                0L,
                null,
                0L,
                false
        );
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
}
