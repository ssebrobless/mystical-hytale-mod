package com.motm.runtime;

import com.hypixel.hytale.protocol.GameMode;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MotmRuntimeTasksTest {

    @Test
    void requestMethodsCoalesceSetAndMapTasks() {
        MotmRuntimeTasks tasks = new MotmRuntimeTasks();

        assertTrue(tasks.requestSpellbookGrant("player"));
        assertFalse(tasks.requestSpellbookGrant("player"));
        assertTrue(tasks.requestProof("player", "coating-metal"));
        assertFalse(tasks.requestProof("player", "coating-obsidian"));
        tasks.requestGameModeChange("player", GameMode.Creative);
        tasks.requestStatusHudInstall("player", 4);
        tasks.updateStatusHudInstallDelay("player", 2);

        assertEquals(1, tasks.pendingSpellbookGrants().size());
        assertEquals("coating-obsidian", tasks.pendingProofRequests().get("player"));
        assertEquals(GameMode.Creative, tasks.pendingDevGameModeChanges().get("player"));
        assertEquals(2, tasks.pendingStatusHudInstalls().get("player"));
    }

    @Test
    void taskLifecycleEvidenceRecordsAcceptedSkippedExecutedAndFailed() {
        MotmRuntimeTasks tasks = new MotmRuntimeTasks();
        List<Map<String, Object>> events = new ArrayList<>();
        tasks.setEvidenceSink((phase, taskType, playerId, details) -> events.add(Map.of(
                "phase", phase,
                "taskType", taskType,
                "playerId", String.valueOf(playerId),
                "details", details
        )));

        assertTrue(tasks.requestSpellbookGrant("player"));
        assertFalse(tasks.requestSpellbookGrant("player"));
        tasks.recordTaskExecuted("spellbook-grant", "player", Map.of("granted", true));
        tasks.recordTaskFailed("proof", "player", Map.of("error", "boom"));

        assertEquals("accepted", events.get(0).get("phase"));
        assertEquals("spellbook-grant", events.get(0).get("taskType"));
        assertEquals("skipped", events.get(1).get("phase"));
        assertEquals(Map.of("reason", "duplicate"), events.get(1).get("details"));
        assertEquals("executed", events.get(2).get("phase"));
        assertEquals("failed", events.get(3).get("phase"));
    }

    @Test
    void clearPlayerRemovesEveryPendingFamily() {
        MotmRuntimeTasks tasks = new MotmRuntimeTasks();
        tasks.requestSpellbookGrant("player");
        tasks.requestDevBookGrant("player");
        tasks.enqueueAbilityCast(new PendingAbilityCast("player", "ability", null, null, true));
        tasks.requestStyleTestMobSpawn("player", "standard");
        tasks.requestStyleTestMobClear("player");
        tasks.requestStyleTestMobCount("player");
        tasks.requestStyleReviewReset("player");
        tasks.requestProof("player", "coating-metal");
        tasks.requestDevRelocation("player", "up");
        tasks.requestDaylight("player");
        tasks.requestGameModeChange("player", GameMode.Creative);
        tasks.requestTerraReviewKitGrant("player");
        tasks.requestTerraReviewInventoryClean("player");
        tasks.requestHydroContainerSync("player");
        tasks.requestRuntimeRebuild("player");
        tasks.requestStatusHudRefresh("player");
        tasks.requestStatusHudInstall("player", 4);
        tasks.requestProgressionBonusRefresh("player");
        tasks.requestFreeCastInvulnerabilityClear("player");

        tasks.clearPlayer("player");

        tasks.snapshot().forEach((key, value) -> assertEquals(0, value, key));
    }

    @Test
    void invalidRequestsAreSkippedWithoutMutatingPendingState() {
        MotmRuntimeTasks tasks = new MotmRuntimeTasks();
        List<Map<String, Object>> events = new ArrayList<>();
        tasks.setEvidenceSink((phase, taskType, playerId, details) -> events.add(Map.of(
                "phase", phase,
                "taskType", taskType,
                "playerId", String.valueOf(playerId),
                "details", details
        )));

        assertFalse(tasks.requestSpellbookGrant(""));
        tasks.enqueueAbilityCast(null);
        tasks.enqueueAbilityCast(new PendingAbilityCast("", "ability", null, null, true));
        tasks.requestStyleAbilityTest("player", "");
        assertFalse(tasks.requestProof("player", null));
        tasks.requestStatusHudInstall("", 4);

        tasks.snapshot().forEach((key, value) -> assertEquals(0, value, key));
        assertEquals(6, events.size());
        assertTrue(events.stream().allMatch(event -> "skipped".equals(event.get("phase"))));
    }

    @Test
    void pendingViewsAreImmutableAndCompletionIsExplicit() {
        MotmRuntimeTasks tasks = new MotmRuntimeTasks();
        tasks.requestSpellbookGrant("player");
        tasks.requestProof("player", "coating-metal");
        tasks.enqueueAbilityCast(new PendingAbilityCast("player", "ability", null, null, true));

        assertThrows(UnsupportedOperationException.class, () -> tasks.pendingSpellbookGrants().add("other"));
        assertThrows(UnsupportedOperationException.class, () -> tasks.pendingProofRequests().put("other", "proof"));
        assertThrows(UnsupportedOperationException.class, () -> tasks.pendingAbilityCasts().clear());

        assertEquals(1, tasks.pendingSpellbookGrants().size());
        assertEquals(1, tasks.pendingProofRequests().size());
        assertEquals(1, tasks.pendingAbilityCasts().size());

        assertTrue(tasks.completeSpellbookGrant("player"));
        assertTrue(tasks.completeProofRequest("player"));
        assertTrue(tasks.completeAbilityCast(tasks.pendingAbilityCasts().get(0)));

        tasks.snapshot().forEach((key, value) -> assertEquals(0, value, key));
    }
}
