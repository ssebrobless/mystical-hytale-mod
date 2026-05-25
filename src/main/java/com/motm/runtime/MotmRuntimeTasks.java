package com.motm.runtime;

import com.hypixel.hytale.protocol.GameMode;

import java.util.LinkedHashMap;
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

    public Set<String> spellbookGrants() { return spellbookGrants; }
    public Set<String> devBookGrants() { return devBookGrants; }
    public ConcurrentLinkedQueue<PendingAbilityCast> abilityCasts() { return abilityCasts; }
    public Map<String, String> styleTestMobSpawns() { return styleTestMobSpawns; }
    public Set<String> styleTestMobClears() { return styleTestMobClears; }
    public Set<String> styleTestMobCounts() { return styleTestMobCounts; }
    public Set<String> styleReviewResets() { return styleReviewResets; }
    public Set<String> daylightRequests() { return daylightRequests; }
    public Map<String, GameMode> devGameModeChanges() { return devGameModeChanges; }
    public Set<String> terraReviewKitGrants() { return terraReviewKitGrants; }
    public Set<String> terraReviewInventoryCleans() { return terraReviewInventoryCleans; }
    public Map<String, String> singleAbilityTests() { return singleAbilityTests; }
    public Map<String, String> proofRequests() { return proofRequests; }
    public Map<String, String> devRelocations() { return devRelocations; }
    public Set<String> hydroContainerSyncs() { return hydroContainerSyncs; }
    public Set<String> runtimeRebuilds() { return runtimeRebuilds; }
    public Set<String> statusHudRefreshes() { return statusHudRefreshes; }
    public Map<String, Integer> statusHudInstalls() { return statusHudInstalls; }
    public Set<String> progressionBonusRefreshes() { return progressionBonusRefreshes; }
    public Set<String> freeCastInvulnerabilityClears() { return freeCastInvulnerabilityClears; }

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
        return pending;
    }
}
