package com.motm.runtime.task;

import com.motm.runtime.MotmRuntimeTasks;
import org.junit.jupiter.api.Test;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbilityCastCommandActionsTest {

    @Test
    void ignoresInvalidQueueRequests() {
        MotmRuntimeTasks tasks = new MotmRuntimeTasks();
        AbilityCastCommandActions actions = new AbilityCastCommandActions(tasks, Logger.getLogger("test"));

        actions.queue("", "stone_shot", null, null, true);
        actions.queue("player-a", "", null, null, true);

        assertEquals(0, tasks.pendingAbilityCasts().size());
    }

    @Test
    void queuesValidAbilityCastRequests() {
        MotmRuntimeTasks tasks = new MotmRuntimeTasks();
        AbilityCastCommandActions actions = new AbilityCastCommandActions(tasks, Logger.getLogger("test"));

        actions.queue("player-a", "stone_shot", null, null, true);

        assertEquals(1, tasks.pendingAbilityCasts().size());
        assertEquals("player-a", tasks.pendingAbilityCasts().get(0).playerId());
        assertEquals("stone_shot", tasks.pendingAbilityCasts().get(0).abilityId());
    }
}
