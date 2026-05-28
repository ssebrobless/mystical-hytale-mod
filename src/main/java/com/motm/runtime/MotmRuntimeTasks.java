package com.motm.runtime;

import com.hypixel.hytale.protocol.GameMode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Owns deferred player work that must be processed from the server tick/store
 * context. MenteesMod still executes the tasks today, but this keeps the
 * scheduling state in one named place instead of scattering ad hoc collections
 * across the plugin class.
 */
public final class MotmRuntimeTasks {

    private final Set<String> spellbookGrants = ConcurrentHashMap.newKeySet();
    private final Set<String> devBookGrants = ConcurrentHashMap.newKeySet();
    private final ConcurrentLinkedQueue<PendingAbilityCast> abilityCasts = new ConcurrentLinkedQueue<>();
    private final Map<String, String> styleTestMobSpawns = new ConcurrentHashMap<>();
    private final Set<String> styleTestMobClears = ConcurrentHashMap.newKeySet();
    private final Set<String> styleTestMobCounts = ConcurrentHashMap.newKeySet();
    private final Set<String> styleReviewResets = ConcurrentHashMap.newKeySet();
    private final Set<String> daylightRequests = ConcurrentHashMap.newKeySet();
    private final Map<String, GameMode> devGameModeChanges = new ConcurrentHashMap<>();
    private final Set<String> terraReviewKitGrants = ConcurrentHashMap.newKeySet();
    private final Set<String> terraReviewInventoryCleans = ConcurrentHashMap.newKeySet();
    private final Map<String, String> singleAbilityTests = new ConcurrentHashMap<>();
    private final Map<String, String> proofRequests = new ConcurrentHashMap<>();
    private final Map<String, String> devRelocations = new ConcurrentHashMap<>();
    private final Set<String> hydroContainerSyncs = ConcurrentHashMap.newKeySet();
    private final Set<String> runtimeRebuilds = ConcurrentHashMap.newKeySet();
    private final Set<String> statusHudRefreshes = ConcurrentHashMap.newKeySet();
    private final Map<String, Integer> statusHudInstalls = new ConcurrentHashMap<>();
    private final Set<String> progressionBonusRefreshes = ConcurrentHashMap.newKeySet();
    private final Set<String> freeCastInvulnerabilityClears = ConcurrentHashMap.newKeySet();
    private final Set<String> runtimeEntityEffectClears = ConcurrentHashMap.newKeySet();
    private volatile RuntimeTaskEvidenceSink evidenceSink = RuntimeTaskEvidenceSink.NOOP;

    public void setEvidenceSink(RuntimeTaskEvidenceSink evidenceSink) {
        this.evidenceSink = evidenceSink == null ? RuntimeTaskEvidenceSink.NOOP : evidenceSink;
    }

    public List<String> pendingSpellbookGrants() { return List.copyOf(spellbookGrants); }
    public List<String> pendingDevBookGrants() { return List.copyOf(devBookGrants); }
    public List<PendingAbilityCast> pendingAbilityCasts() { return List.copyOf(abilityCasts); }
    public Map<String, String> pendingStyleTestMobSpawns() { return Map.copyOf(styleTestMobSpawns); }
    public List<String> pendingStyleTestMobClears() { return List.copyOf(styleTestMobClears); }
    public List<String> pendingStyleTestMobCounts() { return List.copyOf(styleTestMobCounts); }
    public List<String> pendingStyleReviewResets() { return List.copyOf(styleReviewResets); }
    public List<String> pendingDaylightRequests() { return List.copyOf(daylightRequests); }
    public Map<String, GameMode> pendingDevGameModeChanges() { return Map.copyOf(devGameModeChanges); }
    public List<String> pendingTerraReviewKitGrants() { return List.copyOf(terraReviewKitGrants); }
    public List<String> pendingTerraReviewInventoryCleans() { return List.copyOf(terraReviewInventoryCleans); }
    public Map<String, String> pendingSingleAbilityTests() { return Map.copyOf(singleAbilityTests); }
    public Map<String, String> pendingProofRequests() { return Map.copyOf(proofRequests); }
    public Map<String, String> pendingDevRelocations() { return Map.copyOf(devRelocations); }
    public List<String> pendingHydroContainerSyncs() { return List.copyOf(hydroContainerSyncs); }
    public List<String> pendingRuntimeRebuilds() { return List.copyOf(runtimeRebuilds); }
    public List<String> pendingStatusHudRefreshes() { return List.copyOf(statusHudRefreshes); }
    public Map<String, Integer> pendingStatusHudInstalls() { return Map.copyOf(statusHudInstalls); }
    public List<String> pendingProgressionBonusRefreshes() { return List.copyOf(progressionBonusRefreshes); }
    public List<String> pendingFreeCastInvulnerabilityClears() { return List.copyOf(freeCastInvulnerabilityClears); }
    public List<String> pendingRuntimeEntityEffectClears() { return List.copyOf(runtimeEntityEffectClears); }

    public boolean requestSpellbookGrant(String playerId) {
        return requestSet("spellbook-grant", spellbookGrants, playerId);
    }

    public boolean requestDevBookGrant(String playerId) {
        return requestSet("dev-book-grant", devBookGrants, playerId);
    }

    public void enqueueAbilityCast(PendingAbilityCast request) {
        if (request != null && validPlayerId(request.playerId())) {
            abilityCasts.add(request);
            recordAccepted("ability-cast", request.playerId(), Map.of("abilityId", String.valueOf(request.abilityId())));
        } else {
            recordSkipped("ability-cast", request != null ? request.playerId() : null, Map.of("reason", "invalid_request"));
        }
    }

    public void requestStyleAbilityTest(String playerId, String abilityId) {
        if (validPlayerId(playerId) && abilityId != null && !abilityId.isBlank()) {
            singleAbilityTests.put(playerId, abilityId);
            recordAccepted("style-ability-test", playerId, Map.of("abilityId", abilityId));
        } else {
            recordSkipped("style-ability-test", playerId, Map.of("reason", "invalid_request"));
        }
    }

    public boolean requestStyleTestMobSpawn(String playerId, String mode) {
        return requestMapPutIfAbsent("style-test-mob-spawn", styleTestMobSpawns, playerId, mode);
    }

    public boolean requestStyleTestMobClear(String playerId) {
        return requestSet("style-test-mob-clear", styleTestMobClears, playerId);
    }

    public boolean requestStyleTestMobCount(String playerId) {
        return requestSet("style-test-mob-count", styleTestMobCounts, playerId);
    }

    public boolean requestStyleReviewReset(String playerId) {
        return requestSet("style-review-reset", styleReviewResets, playerId);
    }

    public boolean requestProof(String playerId, String proofId) {
        return requestMapPutIfAbsent("proof", proofRequests, playerId, proofId);
    }

    public boolean requestDevRelocation(String playerId, String target) {
        return requestMapPutIfAbsent("dev-relocation", devRelocations, playerId, target);
    }

    public boolean requestDaylight(String playerId) {
        return requestSet("daylight", daylightRequests, playerId);
    }

    public void requestGameModeChange(String playerId, GameMode gameMode) {
        if (validPlayerId(playerId) && gameMode != null) {
            devGameModeChanges.put(playerId, gameMode);
            recordAccepted("game-mode-change", playerId, Map.of("gameMode", String.valueOf(gameMode)));
        } else {
            recordSkipped("game-mode-change", playerId, Map.of("reason", "invalid_request"));
        }
    }

    public boolean requestTerraReviewKitGrant(String playerId) {
        return requestSet("terra-review-kit-grant", terraReviewKitGrants, playerId);
    }

    public boolean requestTerraReviewInventoryClean(String playerId) {
        return requestSet("terra-review-inventory-clean", terraReviewInventoryCleans, playerId);
    }

    public void requestHydroContainerSync(String playerId) {
        requestSet("hydro-container-sync", hydroContainerSyncs, playerId);
    }

    public void requestRuntimeRebuild(String playerId) {
        requestSet("runtime-rebuild", runtimeRebuilds, playerId);
    }

    public void requestStatusHudRefresh(String playerId) {
        requestSet("status-hud-refresh", statusHudRefreshes, playerId);
    }

    public void requestStatusHudInstall(String playerId, int delayTicks) {
        if (validPlayerId(playerId)) {
            statusHudInstalls.put(playerId, Math.max(0, delayTicks));
            recordAccepted("status-hud-install", playerId, Map.of("delayTicks", Math.max(0, delayTicks)));
        } else {
            recordSkipped("status-hud-install", playerId, Map.of("reason", "invalid_player"));
        }
    }

    public void updateStatusHudInstallDelay(String playerId, int delayTicks) {
        if (validPlayerId(playerId)) {
            statusHudInstalls.put(playerId, Math.max(0, delayTicks));
        }
    }

    public void completeStatusHudInstall(String playerId) {
        if (validPlayerId(playerId)) {
            statusHudInstalls.remove(playerId);
        }
    }

    public void requestProgressionBonusRefresh(String playerId) {
        requestSet("progression-bonus-refresh", progressionBonusRefreshes, playerId);
    }

    public void requestFreeCastInvulnerabilityClear(String playerId) {
        requestSet("free-cast-invulnerability-clear", freeCastInvulnerabilityClears, playerId);
    }

    public void requestRuntimeEntityEffectClear(String playerId) {
        requestSet("runtime-entity-effect-clear", runtimeEntityEffectClears, playerId);
    }

    public void cancelFreeCastInvulnerabilityClear(String playerId) {
        if (validPlayerId(playerId)) {
            freeCastInvulnerabilityClears.remove(playerId);
        }
    }

    public void clearPlayer(String playerId) {
        if (playerId == null || playerId.isBlank()) {
            return;
        }

        spellbookGrants.remove(playerId);
        devBookGrants.remove(playerId);
        abilityCasts.removeIf(request -> playerId.equals(request.playerId()));
        styleTestMobSpawns.remove(playerId);
        styleTestMobClears.remove(playerId);
        styleTestMobCounts.remove(playerId);
        styleReviewResets.remove(playerId);
        daylightRequests.remove(playerId);
        devGameModeChanges.remove(playerId);
        terraReviewKitGrants.remove(playerId);
        terraReviewInventoryCleans.remove(playerId);
        singleAbilityTests.remove(playerId);
        proofRequests.remove(playerId);
        devRelocations.remove(playerId);
        hydroContainerSyncs.remove(playerId);
        runtimeRebuilds.remove(playerId);
        statusHudRefreshes.remove(playerId);
        statusHudInstalls.remove(playerId);
        progressionBonusRefreshes.remove(playerId);
        freeCastInvulnerabilityClears.remove(playerId);
        runtimeEntityEffectClears.remove(playerId);
    }

    public boolean completeSpellbookGrant(String playerId) { return removeSet(spellbookGrants, playerId); }
    public boolean completeDevBookGrant(String playerId) { return removeSet(devBookGrants, playerId); }
    public boolean completeAbilityCast(PendingAbilityCast request) { return request != null && abilityCasts.remove(request); }
    public boolean completeStyleTestMobSpawn(String playerId) { return removeMap(styleTestMobSpawns, playerId); }
    public boolean completeStyleTestMobClear(String playerId) { return removeSet(styleTestMobClears, playerId); }
    public boolean completeStyleTestMobCount(String playerId) { return removeSet(styleTestMobCounts, playerId); }
    public boolean completeStyleReviewReset(String playerId) { return removeSet(styleReviewResets, playerId); }
    public boolean completeDaylightRequest(String playerId) { return removeSet(daylightRequests, playerId); }
    public boolean completeDevGameModeChange(String playerId) { return removeMap(devGameModeChanges, playerId); }
    public boolean completeTerraReviewKitGrant(String playerId) { return removeSet(terraReviewKitGrants, playerId); }
    public boolean completeTerraReviewInventoryClean(String playerId) { return removeSet(terraReviewInventoryCleans, playerId); }
    public boolean completeStyleAbilityTest(String playerId) { return removeMap(singleAbilityTests, playerId); }
    public boolean completeProofRequest(String playerId) { return removeMap(proofRequests, playerId); }
    public boolean completeDevRelocation(String playerId) { return removeMap(devRelocations, playerId); }
    public boolean completeHydroContainerSync(String playerId) { return removeSet(hydroContainerSyncs, playerId); }
    public boolean completeRuntimeRebuild(String playerId) { return removeSet(runtimeRebuilds, playerId); }
    public boolean completeStatusHudRefresh(String playerId) { return removeSet(statusHudRefreshes, playerId); }
    public boolean completeProgressionBonusRefresh(String playerId) { return removeSet(progressionBonusRefreshes, playerId); }
    public boolean completeFreeCastInvulnerabilityClear(String playerId) { return removeSet(freeCastInvulnerabilityClears, playerId); }
    public boolean completeRuntimeEntityEffectClear(String playerId) { return removeSet(runtimeEntityEffectClears, playerId); }

    public void cancelAbilityCastsForPlayer(String playerId) {
        if (validPlayerId(playerId)) {
            abilityCasts.removeIf(request -> playerId.equals(request.playerId()));
        }
    }

    public void recordTaskExecuted(String taskType, String playerId, Map<String, Object> details) {
        record("executed", taskType, playerId, details);
    }

    public void recordTaskSkipped(String taskType, String playerId, Map<String, Object> details) {
        recordSkipped(taskType, playerId, details);
    }

    public void recordTaskFailed(String taskType, String playerId, Map<String, Object> details) {
        record("failed", taskType, playerId, details);
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> pending = new LinkedHashMap<>();
        pending.put("pendingSpellbookGrants", spellbookGrants.size());
        pending.put("pendingDevBookGrants", devBookGrants.size());
        pending.put("pendingAbilityCasts", abilityCasts.size());
        pending.put("pendingStyleTestMobSpawns", styleTestMobSpawns.size());
        pending.put("pendingStyleTestMobClears", styleTestMobClears.size());
        pending.put("pendingStyleTestMobCounts", styleTestMobCounts.size());
        pending.put("pendingStyleReviewResets", styleReviewResets.size());
        pending.put("pendingProofRequests", proofRequests.size());
        pending.put("pendingDevRelocations", devRelocations.size());
        pending.put("pendingDaylightRequests", daylightRequests.size());
        pending.put("pendingDevGameModeChanges", devGameModeChanges.size());
        pending.put("pendingTerraReviewKitGrants", terraReviewKitGrants.size());
        pending.put("pendingTerraReviewInventoryCleans", terraReviewInventoryCleans.size());
        pending.put("pendingHydroContainerSyncs", hydroContainerSyncs.size());
        pending.put("pendingRuntimeRebuilds", runtimeRebuilds.size());
        pending.put("pendingStatusHudRefreshes", statusHudRefreshes.size());
        pending.put("pendingStatusHudInstalls", statusHudInstalls.size());
        pending.put("pendingProgressionBonusRefreshes", progressionBonusRefreshes.size());
        pending.put("pendingFreeCastInvulnerabilityClears", freeCastInvulnerabilityClears.size());
        pending.put("pendingRuntimeEntityEffectClears", runtimeEntityEffectClears.size());
        return pending;
    }

    private boolean validPlayerId(String playerId) {
        return playerId != null && !playerId.isBlank();
    }

    private boolean requestSet(String taskType, Set<String> tasks, String playerId) {
        if (!validPlayerId(playerId)) {
            recordSkipped(taskType, playerId, Map.of("reason", "invalid_player"));
            return false;
        }
        boolean added = tasks.add(playerId);
        if (added) {
            recordAccepted(taskType, playerId, Map.of());
        } else {
            recordSkipped(taskType, playerId, Map.of("reason", "duplicate"));
        }
        return added;
    }

    private <T> boolean requestMapPutIfAbsent(String taskType, Map<String, T> tasks, String playerId, T value) {
        if (!validPlayerId(playerId) || value == null) {
            recordSkipped(taskType, playerId, Map.of("reason", "invalid_request"));
            return false;
        }
        boolean added = tasks.put(playerId, value) == null;
        if (added) {
            recordAccepted(taskType, playerId, Map.of("value", String.valueOf(value)));
        } else {
            recordSkipped(taskType, playerId, Map.of("reason", "duplicate"));
        }
        return added;
    }

    private boolean removeSet(Set<String> tasks, String playerId) {
        return validPlayerId(playerId) && tasks.remove(playerId);
    }

    private boolean removeMap(Map<String, ?> tasks, String playerId) {
        return validPlayerId(playerId) && tasks.remove(playerId) != null;
    }

    private void recordAccepted(String taskType, String playerId, Map<String, Object> details) {
        record("accepted", taskType, playerId, details);
    }

    private void recordSkipped(String taskType, String playerId, Map<String, Object> details) {
        record("skipped", taskType, playerId, details);
    }

    private void record(String phase, String taskType, String playerId, Map<String, Object> details) {
        evidenceSink.record(phase, taskType, playerId, details == null ? Map.of() : details);
    }
}
