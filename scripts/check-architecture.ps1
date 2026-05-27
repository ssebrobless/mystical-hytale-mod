param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"
$script:Failed = $false

function Fail([string]$Message) {
    $script:Failed = $true
    Write-Host "FAIL: $Message" -ForegroundColor Red
}

function Pass([string]$Message) {
    Write-Host "PASS: $Message" -ForegroundColor Green
}

function Get-RelativePath([string]$Path) {
    return [System.IO.Path]::GetRelativePath($ProjectRoot, $Path).Replace("\", "/")
}

$javaFiles = Get-ChildItem -LiteralPath (Join-Path $ProjectRoot "src/main/java") -Recurse -Filter "*.java"
$genericRuntimeFiles = @($javaFiles | Where-Object {
    $relative = Get-RelativePath $_.FullName
    $relative -match '^src/main/java/com/motm/(manager|runtime|system)/' `
        -and $relative -notmatch '^src/main/java/com/motm/runtime/ability/' `
        -and $relative -notmatch '^src/main/java/com/motm/content/'
})

$abilityBranchPattern = 'is[A-Z][A-Za-z0-9]+Ability\s*\(|switch\s*\(\s*lower\s*\(\s*ability\.getId\s*\(\s*\)\s*\)\s*\)|"[^"]+"\s*\.equals\s*\(\s*lower\s*\(\s*ability\.getId\s*\(\s*\)\s*\)\s*\)'
$allowedExistingAbilityBranchFiles = @(
    "src/main/java/com/motm/manager/GameplayPlaybackManager.java",
    "src/main/java/com/motm/manager/StyleManager.java",
    "src/main/java/com/motm/system/MotmDamageEventSystem.java"
)

foreach ($file in $genericRuntimeFiles) {
    $relative = Get-RelativePath $file.FullName
    if ($allowedExistingAbilityBranchFiles -contains $relative) {
        continue
    }
    $lineNumber = 0
    foreach ($line in Get-Content -LiteralPath $file.FullName) {
        $lineNumber++
        if ($line -match $abilityBranchPattern) {
            Fail "$relative`:$lineNumber adds ability-id branching outside the ability runtime/profile layer. Suggested owner: src/main/java/com/motm/runtime/ability/<family>/ or content ability descriptors."
        }
    }
}

$menteesMod = Join-Path $ProjectRoot "src/main/java/com/motm/MenteesMod.java"
if (Test-Path -LiteralPath $menteesMod) {
    $lineNumber = 0
    $mutableRuntimeStateCount = 0
    $allowedLegacyMutableRuntimeStateCount = 0
    $pendingProcessorMethodCount = 0
    $allowedPendingProcessorMethodCount = 0
    foreach ($line in Get-Content -LiteralPath $menteesMod) {
        $lineNumber++
        if ($line -match 'new\s+ConcurrentHashMap\s*<|new\s+ConcurrentLinkedQueue\s*<|ConcurrentHashMap\.newKeySet\s*\(') {
            $mutableRuntimeStateCount++
        }
        if ($line -match 'private\s+void\s+processPending[A-Z]') {
            $pendingProcessorMethodCount++
        }
        if ($line -match 'runtimeTasks\.[A-Za-z0-9_]+\(\)\.(add|put)\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber mutates runtime task collections directly. Suggested owner: MotmRuntimeTasks intent method, then a runtime task processor."
        }
        if ($line -match 'dev-command-(inbox|outbox)|Files\.(readAllLines|writeString|deleteIfExists)\s*\([^)]*(inbox|outbox)') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces file-backed dev command bridge ownership. Suggested owner: src/main/java/com/motm/command/MotmDevCommandInbox.java."
        }
        if ($line -match 'new\s+MotmDevCommandInbox\.Hooks\s*\(|cmd-\s*"\s*\+\s*Long\.toUnsignedString|findRuntimePlayer\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces dev command inbox processing ownership. Suggested owner: src/main/java/com/motm/command/MotmDevCommandInboxProcessor.java."
        }
        if ($line -match 'scanner-safe|asset-scanned|migrateLegacyPluginDataDirectory|writeScannerSafeLegacyManifest') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces plugin data migration compatibility ownership. Suggested owner: src/main/java/com/motm/config/MotmPluginDataDirectories.java."
        }
        if ($line -match 'TERRA_[A-Z_]+_(ITEM_PREFIXES|UNITS_PER_ITEM)|hasAnyPrefix\s*\(|countTerraInventoryResource\s*\(|spendTerraInventoryResource\s*\(|matchesTerraResourceItem\s*\(|getTerraResourceUnitsPerItem\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces Terra inventory resource ownership. Suggested owner: src/main/java/com/motm/resource/TerraInventoryResourceBridge.java and TerraInventoryResourcePolicy.java."
        }
        if ($line -match 'TERRA_REVIEW_(KIT_ITEMS|ESSENTIAL_ITEM_IDS)|TerraReviewKitItem|private\s+String\s+(cleanTerraReviewInventory|grantTerraReviewKit)\s*\(|ensureReviewItem\s*\(|isItemAssetAvailable\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces Terra review inventory kit ownership. Suggested owner: src/main/java/com/motm/resource/TerraReviewInventoryKit.java."
        }
        if ($line -match 'new\s+CombinedItemContainer\s*\(|addInventoryContainer\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces player inventory container assembly. Suggested owner: src/main/java/com/motm/util/MotmPlayerInventory.java."
        }
        if ($line -match 'HYDRO_[A-Z_]+|Bson(Boolean|Document|Int32|Value)|private\s+ItemStack\s+createHydroContainerStack\s*\(|private\s+boolean\s+isHydroContainerTier\s*\(|private\s+void\s+removeAllHydroContainerItems\s*\(|private\s+void\s+syncHydroContainerItem\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces Hydro container inventory ownership. Suggested owner: src/main/java/com/motm/resource/HydroContainerItems.java and HydroInventoryBridge.java."
        }
        if ($line -match 'tryHandleHydroContainerRefill\s*\(|canAttemptHydroContainerRefill\s*\(|isWaterSourceBlock\s*\(|resourceManager\.refillWater\s*\(|resourceManager\.syncToPersistentState\s*\([^)]*playerData') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces Hydro refill behavior ownership. Suggested owner: src/main/java/com/motm/resource/HydroContainerRefillHandler.java."
        }
        if ($line -match 'resolveTerraMinerForBlockDamage\s*\(|isPickaxeItemId\s*\(|event\.setDamage\s*\(\s*event\.getDamage\s*\(\s*\)\s*\*\s*1\.5f') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces block-damage interaction behavior ownership. Suggested owner: src/main/java/com/motm/interaction/BlockDamageInteractionHandler.java."
        }
        if ($line -match 'tryCastSpellbookSlot\s*\(|resolveSpellbook(Interact|Mouse)Slot\s*\(|isDuplicateSpellbookInput\s*\(|resolveMouseButtonItemId\s*\(|spellbookInputDebouncer|MouseButtonState|MouseButtonType') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces spellbook input routing ownership. Suggested owner: src/main/java/com/motm/interaction/SpellbookInputHandler.java."
        }
        if ($line -match 'proofCleanupRuntimeState\.(selections|proxies|removeSelection|removeProxy)\s*\(|Proof cleanup (restored|failed|despawned)|Proof proxy cleanup failed') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces proof cleanup processing ownership. Suggested owner: src/main/java/com/motm/proof/MotmProofCleanupProcessor.java."
        }
        if ($line -match 'private\s+.*\s+(applyProofEffect|applyProofTargetEffect|applyProofEffectToRef|runTempBlockProof|resolveProofBlockId|proofHorizontalRightStep|runTempFluidProof|resolveProofFluidId|isUsableProofFluidId|listProofFluidIds|placeTemporarySelection|runProxyProof|runMovementProof|proofAnchor)\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces proof world/effect mutation ownership. Suggested owner: src/main/java/com/motm/proof/MotmProofActions.java."
        }
        if ($line -match 'processActiveStyleTests\s*\(|resolveStyleTestDelayMs\s*\(|findNearestStyleTestNpc\s*\(|resolveStyleTestTargetBlock\s*\(|styleTestRuntimeState\.activeTests\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces live style-test sequence ownership. Suggested owner: src/main/java/com/motm/runtime/task/StyleTestSequenceRuntimeTaskProcessor.java and StyleTestTargetResolver.java."
        }
        if ($line -match 'processFreeCastTestSafety\s*\(|applyFreeCastMovementNormalization\s*\(|resetFreeCastMovementNormalization\s*\(|setRuntimeInvulnerability\s*\(|maximizeStatIfPresent\s*\(|freeCastRuntimeState\.(enabledPlayers|rememberObservedHealth)\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces free-cast safety mutation ownership. Suggested owner: src/main/java/com/motm/runtime/task/FreeCastSafetyProcessor.java."
        }
        if ($line -match 'private\s+void\s+(ensureFreeCastInvulnerability|clearFreeCastInvulnerability)\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces free-cast invulnerability ownership. Suggested owner: src/main/java/com/motm/runtime/task/FreeCastSafetyProcessor.java."
        }
        if ($line -match 'private\s+String\s+(relocateRuntimePlayerForTesting|applyDevGameModeChange)\s*\(|resolveTestingLaneY\s*\(|placeRelocationPlatform\s*\(|Player\.setGameMode\s*\(|MOTM dev relocation platform') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces dev player/world mutation ownership. Suggested owner: src/main/java/com/motm/runtime/task/DevPlayerTestActions.java."
        }
        if ($line -match 'STYLE_TEST_CLEANUP_ROLES|normalizeStyleTestMobMode\s*\(|private\s+String\s+(spawnStyleTestMobsNow|clearStyleTestMobsNow|countStyleTestMobsNow|scrubStyleReviewArena)\s*\(|private\s+(void|int|boolean|Ref<[^>]+>)\s+(addStyleTestNpc|clearNearbyStyleTestTargets|countNearbyStyleTestTargets|visitNearbyStyleTestTargets|isStyleTestCleanupRole|clearTrackedStyleTestTargets|countTrackedStyleTestTargets|countValidRefs|spawnStyleTestNpc)\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces style-test mob world action ownership. Suggested owner: src/main/java/com/motm/runtime/task/StyleTestMobActions.java."
        }
        if ($line -match 'findStyleLookup\s*\(|StyleLookup|MotmProofCatalog\.|requestStyleAbilityTest\s*\(|requestStyle(TestMob(Spawn|Clear|Count)|ReviewReset)\s*\(|handleNativeWeaponDamage\s*\(|forceArmedStompLanding\s*\(|styleTestRuntimeState\.(start|get)\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces dev style-test command policy. Suggested owner: src/main/java/com/motm/runtime/task/StyleTestCommandActions.java."
        }
        if ($line -match 'parseReviewGameMode\s*\(|request(DevRelocation|Daylight|GameModeChange|TerraReviewKitGrant|TerraReviewInventoryClean)\s*\(|Dev relocate usage|Dev daylight queued|Dev game mode queued|Terra review (kit|inventory clean) queued') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces dev runtime command queue policy. Suggested owner: src/main/java/com/motm/runtime/task/DevRuntimeCommandActions.java."
        }
        if ($line -match 'new\s+SpellbookPage\s*\(|openCustomPage\s*\(|custom_page_open') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces spellbook custom-page opening behavior. Suggested owner: src/main/java/com/motm/ui/SpellbookPageActions.java."
        }
        if ($line -match 'request(HydroContainerSync|SpellbookGrant|DevBookGrant)\s*\(|Spellbook delivery queued|Dev Grimoire delivery queued|onlineRuntimePlayers\.put\s*\(\s*playerId\s*,\s*player\s*\)') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces inventory command queue policy. Suggested owner: src/main/java/com/motm/runtime/task/InventoryCommandActions.java."
        }
        if ($line -match 'private\s+.*\s+(buildObservabilitySnapshot|buildPendingSnapshot|buildPlayerDataSnapshot|buildRuntimePlayerSnapshot|buildRefSnapshot|buildVelocitySnapshot|buildMovementSnapshot|buildStatsSnapshot|statSnapshot|buildStatusEffectsSnapshot|buildNativeEntityEffectsSnapshot|buildInventorySnapshot|buildTrackedTargetsSnapshot|vectorSnapshot)\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces observability snapshot evidence-shape ownership. Suggested owner: src/main/java/com/motm/observability/MotmObservabilitySnapshotBuilder.java."
        }
        if ($line -match 'new\s+ThreadLocal\s*<\s*String\s*>\s*\(|effectiveObservabilityTraceId\s*\(|observability\.record(Control|Causality|ServerTruth|ClientIntent)\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces observability event/trace ownership. Suggested owner: src/main/java/com/motm/observability/MotmObservabilityEvents.java."
        }
        if ($line -match 'PLAYER_LEVEL_HEALTH_MODIFIER_ID|private\s+.*\s+(clearPlayerLevelHealthBonus|applyPlayerLevelHealthBonus|calculateAverageOnlinePlayerLevelForWorld)\s*\(|new\s+StaticModifier\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces progression stat mutation ownership. Suggested owner: src/main/java/com/motm/runtime/player/PlayerProgressionRuntimeActions.java."
        }
        if ($line -match 'assignMobLevel\s*\(|scale(Boss|Mob)Stats\s*\(|apply(PartyScaling|NightBonus|BloodMoonBonus|DungeonBonus)\s*\(|tryMakeElite\s*\(|format(Elite)?MobName\s*\(|getMobStats\s*\(|getMobBaseXp\s*\(|getLevelColor\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces mob spawn scaling ownership. Suggested owner: src/main/java/com/motm/runtime/player/MobSpawnRuntimeActions.java."
        }
        if ($line -match 'runtimeTaskProcessors\.process\s*\(\s*"[^"]+"|hudRefreshTickCounter|lastObservabilityHeartbeatAtMs|server_tick_heartbeat|dotDamageByEntity|private\s+void\s+recordObservabilityHeartbeat\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces server-tick runtime loop ownership. Suggested owner: src/main/java/com/motm/runtime/MotmRuntimeLoop.java."
        }
        if ($line -match 'new\s+PendingAbilityCast\s*\(|runtimeTasks\.enqueueAbilityCast\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces ability-cast queue ownership. Suggested owner: src/main/java/com/motm/runtime/task/AbilityCastCommandActions.java."
        }
        if ($line -match 'new\s+PerkTriggerBinding\s*\(|private\s+.*\s+applyHealFraction\s*\(|perk (on_kill trigger|trigger registered)|DefaultEntityStatTypes\.getHealth\s*\(\)|EntityStatMap') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces perk-trigger runtime effect ownership. Suggested owner: src/main/java/com/motm/runtime/player/PerkTriggerRuntimeActions.java."
        }
        if ($line -match 'private\s+.*\s+(getPlayerPosition|getPlayerForward|normalizeHorizontal|formatVector|getEntityPosition|distance|resolveMobScalingAnchorLevel)\s*\(|TransformComponent|getComponent\s*\([^)]*TransformComponent\.getComponentType') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces runtime-player lookup/geometry ownership. Suggested owner: src/main/java/com/motm/runtime/player/RuntimePlayerView.java or PlayerProgressionRuntimeActions.java."
        }
        if ($line -match 'MovementStatesComponent|\.crouching') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces runtime-player movement-state readback. Suggested owner: src/main/java/com/motm/runtime/player/RuntimePlayerView.java."
        }
        if ($line -match 'private\s+static\s+final\s+.*(DEFAULT_SPELLBOOK_ITEM_ID|DEFAULT_DEV_GRIMOIRE_ITEM_ID|LEGACY_NONWEAPON_SPELLBOOK_ITEM_IDS|SPELLBOOK_ITEM_IDS|DEV_GRIMOIRE_ITEM_IDS)') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces spellbook/dev-book item identity tables. Suggested owner: src/main/java/com/motm/resource/SpellbookInventoryItems.java."
        }
        if ($line -match 'normalizeLegacySpellbookItem\s*\(|new\s+ItemStack\s*\(\s*SpellbookInventoryItems\.(DEFAULT_SPELLBOOK_ITEM_ID|DEFAULT_DEV_GRIMOIRE_ITEM_ID)|MotmInventoryOps\.(grant|removeSlot)\s*\([^)]*(ensureSpellbookItem|ensureDevBookItem|normalizeLegacySpellbookItem)|legacy spellbook has been updated|A Mentees spellbook has been placed|A Dev Grimoire has been placed') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces spellbook/dev-book delivery or migration behavior. Suggested owner: src/main/java/com/motm/resource/SpellbookInventoryKit.java."
        }
        if ($line -match 'new\s+MotmStatusHud\s*\(|setCustomHud\s*\(|hideHudComponents\s*\(|native_hud_components_hidden|custom_hud_set|statusHuds\.(put|get|forEach|removeIfPlayer|remove)\s*\(') {
            Fail "src/main/java/com/motm/MenteesMod.java`:$lineNumber reintroduces custom HUD install/refresh behavior. Suggested owner: src/main/java/com/motm/ui/MotmStatusHudActions.java."
        }
    }
    if ($mutableRuntimeStateCount -gt $allowedLegacyMutableRuntimeStateCount) {
        Fail "src/main/java/com/motm/MenteesMod.java declares $mutableRuntimeStateCount mutable runtime collections; allowed legacy ratchet is $allowedLegacyMutableRuntimeStateCount. Suggested owner: src/main/java/com/motm/runtime/state or runtime/task typed ownership."
    } else {
        Pass "MenteesMod mutable runtime collection ratchet: $mutableRuntimeStateCount/$allowedLegacyMutableRuntimeStateCount"
    }
    if ($pendingProcessorMethodCount -gt $allowedPendingProcessorMethodCount) {
        Fail "src/main/java/com/motm/MenteesMod.java declares $pendingProcessorMethodCount processPending methods; allowed migration ratchet is $allowedPendingProcessorMethodCount. Suggested owner: src/main/java/com/motm/runtime/task RuntimeTaskProcessor."
    } else {
        Pass "MenteesMod processPending migration ratchet: $pendingProcessorMethodCount/$allowedPendingProcessorMethodCount"
    }

    $menteesSource = Get-Content -LiteralPath $menteesMod -Raw
    $lifecycleSmokeChecks = @{
        "start registers Hytale hooks" = 'protected\s+void\s+start\s*\(\s*\)[\s\S]*?registerHytaleHooks\s*\('
        "shutdown delegates to onDisable" = 'protected\s+void\s+shutdown\s*\(\s*\)[\s\S]*?onDisable\s*\('
        "onDisable removes packet watchers" = 'public\s+void\s+onDisable\s*\(\s*\)[\s\S]*?deregisterObservabilityPacketWatchers\s*\('
        "onDisable removes Hytale hooks" = 'public\s+void\s+onDisable\s*\(\s*\)[\s\S]*?deregisterHytaleHooks\s*\('
    }
    foreach ($check in $lifecycleSmokeChecks.GetEnumerator()) {
        if ($menteesSource -notmatch $check.Value) {
            Fail "src/main/java/com/motm/MenteesMod.java lifecycle smoke check failed: $($check.Key)."
        }
    }
    if ($menteesSource -notmatch 'private\s+void\s+rebuildPlayerRuntimeNow\s*\([^)]*\)\s*\{[\s\S]*?playerRuntimeRebuildActions\.rebuildNow\s*\(') {
        Fail "src/main/java/com/motm/MenteesMod.java rebuildPlayerRuntimeNow must stay delegation-only. Suggested owner: src/main/java/com/motm/runtime/player/PlayerRuntimeRebuildActions.java."
    }
    if ($menteesSource -notmatch 'public\s+ScaledMobResult\s+onMobSpawn\s*\([^)]*zoneId[\s\S]*?\)\s*\{[\s\S]*?mobSpawnActions\.scale\s*\(') {
        Fail "src/main/java/com/motm/MenteesMod.java onMobSpawn must stay delegation-only. Suggested owner: src/main/java/com/motm/runtime/player/MobSpawnRuntimeActions.java."
    }
    if ($menteesSource -notmatch 'public\s+void\s+onServerTick\s*\([^)]*\)\s*\{[\s\S]*?runtimeLoop\.tick\s*\(') {
        Fail "src/main/java/com/motm/MenteesMod.java onServerTick must stay delegation-only. Suggested owner: src/main/java/com/motm/runtime/MotmRuntimeLoop.java."
    }
    if ($menteesSource -notmatch 'private\s+void\s+processDevCommandInbox\s*\([^)]*\)\s*\{[\s\S]*?devCommandInboxProcessor\.process\s*\(') {
        Fail "src/main/java/com/motm/MenteesMod.java processDevCommandInbox must stay delegation-only. Suggested owner: src/main/java/com/motm/command/MotmDevCommandInboxProcessor.java."
    }
    if ($menteesSource -notmatch 'public\s+void\s+queueAbilityCast\s*\([^)]*\)\s*\{[\s\S]*?abilityCastCommandActions\.queue\s*\(') {
        Fail "src/main/java/com/motm/MenteesMod.java queueAbilityCast must stay delegation-only. Suggested owner: src/main/java/com/motm/runtime/task/AbilityCastCommandActions.java."
    }
    if ($menteesSource -notmatch 'public\s+boolean\s+isFreeCastEnabled\s*\([^)]*\)\s*\{[\s\S]*?freeCastCommandActions\.isEnabled\s*\(' `
            -or $menteesSource -notmatch 'public\s+void\s+setFreeCastEnabled\s*\([^)]*\)\s*\{[\s\S]*?freeCastCommandActions\.setEnabled\s*\(') {
        Fail "src/main/java/com/motm/MenteesMod.java free-cast command access must stay delegation-only. Suggested owner: src/main/java/com/motm/runtime/task/FreeCastCommandActions.java."
    }
    if ($menteesSource -notmatch 'public\s+void\s+onMobKilled\s*\([^)]*mobEntityId[\s\S]*?\)\s*\{[\s\S]*?playerCombatLifecycleActions\.onMobKilled\s*\(') {
        Fail "src/main/java/com/motm/MenteesMod.java onMobKilled must stay delegation-only. Suggested owner: src/main/java/com/motm/runtime/player/PlayerCombatLifecycleActions.java."
    }
    if ($menteesSource -notmatch 'public\s+void\s+onPlayerDeath\s*\([^)]*\)\s*\{[\s\S]*?playerCombatLifecycleActions\.onPlayerDeath\s*\(') {
        Fail "src/main/java/com/motm/MenteesMod.java onPlayerDeath must stay delegation-only. Suggested owner: src/main/java/com/motm/runtime/player/PlayerCombatLifecycleActions.java."
    }
    foreach ($methodName in @("onPlayerJoin", "onPlayerConnect", "onPlayerReady", "onPlayerDisconnect")) {
        if ($menteesSource -notmatch "public\s+void\s+$methodName\s*\([^)]*\)\s*\{[\s\S]*?playerSessionLifecycleActions\.$methodName\s*\(") {
            Fail "src/main/java/com/motm/MenteesMod.java $methodName must stay delegation-only. Suggested owner: src/main/java/com/motm/runtime/player/PlayerSessionLifecycleActions.java."
        }
    }
    foreach ($methodName in @("startObservabilityRun", "stopObservabilityRun", "getObservabilityStatus", "setObservabilityScenario", "markObservabilityRun", "snapshotObservability")) {
        if ($menteesSource -notmatch "public\s+String\s+$methodName\s*\([^)]*\)\s*\{[\s\S]*?observabilityActions\.") {
            Fail "src/main/java/com/motm/MenteesMod.java $methodName must stay delegation-only. Suggested owner: src/main/java/com/motm/observability/MotmObservabilityActions.java."
        }
    }
    foreach ($methodName in @("recordControl", "recordCausality", "recordServerTruth", "recordClientIntent")) {
        if ($menteesSource -notmatch "public\s+void\s+$methodName\s*\([^)]*\)\s*\{[\s\S]*?observabilityEvents\.") {
            Fail "src/main/java/com/motm/MenteesMod.java $methodName must stay delegation-only. Suggested owner: src/main/java/com/motm/observability/MotmObservabilityEvents.java."
        }
    }
    foreach ($methodName in @("enterObservabilityTrace", "restoreObservabilityTrace", "currentObservabilityTraceId")) {
        if ($menteesSource -notmatch "(public|private)\s+(String|void)\s+$methodName\s*\([^)]*\)\s*\{[\s\S]*?observabilityEvents\.") {
            Fail "src/main/java/com/motm/MenteesMod.java $methodName must stay delegation-only. Suggested owner: src/main/java/com/motm/observability/MotmObservabilityEvents.java."
        }
    }
}

$gameplayPlaybackManager = Join-Path $ProjectRoot "src/main/java/com/motm/manager/GameplayPlaybackManager.java"
if (Test-Path -LiteralPath $gameplayPlaybackManager) {
    $lineNumber = 0
    $rawRuntimeCollectionCount = 0
    $allowedRawRuntimeCollectionCount = 0
    $directAbilityStateConstructionCount = 0
    $allowedDirectAbilityStateConstructionCount = 0
    foreach ($line in Get-Content -LiteralPath $gameplayPlaybackManager) {
        $lineNumber++
        if ($line -match '^\s*private\s+final\s+(List|Map|Set)\s*<') {
            $rawRuntimeCollectionCount++
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber declares raw runtime collection state. Suggested owner: a runtime/ability family state class or src/main/java/com/motm/runtime/state."
        }
        if ($line -match 'new\s+(ActiveProjectile|ActiveField|ActiveSummon|ActiveSelfEffect|ActivePlayerAnchor|ActiveMovingTerrainTrail|ActiveLapidaryGem|ActiveStackingColumn|ActiveChannel|ActiveLineControl)\s*\(') {
            $directAbilityStateConstructionCount++
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber constructs migrated ability runtime state directly. Suggested owner: the matching runtime activation/launch owner."
        }
        if ($line -match 'PROJECTILE_VISUAL_REFRESH_MS|configureProjectileVisualProxy|projectile_visual_proxy_spawned|spawnProjectileVisualProxy\s*\(|syncProjectileVisual\s*\(|refreshProjectileVisual\s*\(|despawnProjectileVisual\s*\(|removeComponentIfExists\s*\([^)]*(Nameplate|DisplayNameComponent|Interactable|RespondToHit|CollisionResultComponent)') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces projectile visual proxy mechanics. Suggested owner: src/main/java/com/motm/runtime/ability/projectile/ProjectileVisualHytaleAdapter.java."
        }
        if ($line -match 'private\s+Vector3d\s+resolveProjectileOrigin\s*\(|private\s+Vector3d\s+resolveLaunchDirection\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces projectile launch origin/direction lookup. Suggested owner: src/main/java/com/motm/runtime/ability/projectile/ProjectileLaunchHytaleAdapter.java."
        }
        if ($line -match 'DELAYED_PROJECTILE_CAST_TYPES|ProjectileRuntimeSpecs\.resolve|projectileLaunchRuntime\.launch\s*\(|projectileState\.addProjectiles\s*\(|projectileLaunchAdapter\.resolve(Origin|Direction)\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces projectile launch execution. Suggested owner: src/main/java/com/motm/runtime/ability/projectile/ProjectileLaunchHytaleAdapter.java."
        }
        if ($line -match 'private\s+ProjectileLaunchHytaleAdapter\.Result\s+launchProjectiles\s*\(|private\s+final\s+ProjectileLaunchHytaleAdapter\.Support\s+projectileLaunchSupport|private\s+final\s+ProjectileLaunchRuntime\s+projectileLaunchRuntime') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces projectile launch facade plumbing. Suggested owner: construct ProjectileLaunchHytaleAdapter with its runtime, state, visual adapter, and support."
        }
        if ($line -match 'private\s+final\s+Projectile(RuntimeState|HitHytaleAdapter|ImpactHytaleAdapter|TickHytaleAdapter|LifecycleHytaleAdapter|VisualHytaleAdapter)\b') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces projectile family adapter/state fields. Suggested owner: src/main/java/com/motm/runtime/ability/projectile/ProjectileRuntimeFacade.java."
        }
        if ($line -match 'private\s+Ref\s*<\s*EntityStore\s*>\s+resolveProjectileHit\s*\(|private\s+List\s*<\s*Ref\s*<\s*EntityStore\s*>\s*>\s+collectProjectile(Impact|Traversal)Targets\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces projectile hit scanning or target collection. Suggested owner: src/main/java/com/motm/runtime/ability/projectile/ProjectileHitHytaleAdapter.java."
        }
        if ($line -match 'launch\.projectiles\s*\(\s*\)\.forEach\s*\(\s*projectileState::addProjectile\s*\)|private\s+int\s+removeProjectilesForPlayer\s*\(|projectileState\.(removeProcessedProjectiles|removeProjectilesForPlayer|activeProjectileCount)\s*\(|private\s+boolean\s+processProjectileTick\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces projectile state registration/cleanup/lifecycle wrapper logic. Suggested owner: src/main/java/com/motm/runtime/ability/projectile/ProjectileRuntimeState.java or ProjectileLifecycleHytaleAdapter.java."
        }
        if ($line -match 'private\s+void\s+applyProjectile(Impact|TraversalHits|TravelTypeEffects|SplashToken|TargetEffects)\s*\(|private\s+void\s+applyLightningArcSplash\s*\(|private\s+boolean\s+isLightningProjectile\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces projectile impact damage/effect mutation. Suggested owner: src/main/java/com/motm/runtime/ability/projectile/ProjectileImpactHytaleAdapter.java."
        }
        if ($line -match 'ProjectileTickRuntime\.Hooks|MAX_PROJECTILE_STEP_DISTANCE|private\s+boolean\s+isPiercingProjectile\s*\(|private\s+boolean\s+shouldLeaveProjectileVisualOnImpact\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces projectile tick hook wiring. Suggested owner: src/main/java/com/motm/runtime/ability/projectile/ProjectileTickHytaleAdapter.java."
        }
        if ($line -match 'private\s+List\s*<\s*Ref\s*<\s*EntityStore\s*>\s*>\s+collectFieldTargets\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces field target collection. Suggested owner: src/main/java/com/motm/runtime/ability/field/FieldTargetHytaleAdapter.java."
        }
        if ($line -match 'spawnFieldVisualProxy\s*\(|syncFieldVisual\s*\(|refreshFieldVisual\s*\(|despawnFieldVisual\s*\(|private\s+List\s*<\s*Vector3d\s*>\s+buildFieldVisualPositions\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces field visual proxy mutation. Suggested owner: src/main/java/com/motm/runtime/ability/field/FieldVisualHytaleAdapter.java."
        }
        if ($line -match 'private\s+void\s+applyField(Pulse|TargetEffects|TerrainEffects)\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces field pulse target mutation. Suggested owner: src/main/java/com/motm/runtime/ability/field/FieldPulseHytaleAdapter.java."
        }
        if ($line -match 'private\s+void\s+applyField(SupportPulse|OwnerEffects|OwnerTerrainEffects)\s*\(|private\s+void\s+applyStatusToOwner\s*\(|private\s+boolean\s+shouldPulseOwnerEffectToken\s*\(|private\s+boolean\s+isInsideBarrier\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces field support/owner pulse mutation. Suggested owner: src/main/java/com/motm/runtime/ability/field/FieldSupportPulseHytaleAdapter.java."
        }
        if ($line -match 'private\s+void\s+(engageSinkholeField|applySinkholeSuffocationPulse|applySuffocationTick|releaseSinkholeField)\s*\(|private\s+boolean\s+isSinkhole\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces sinkhole mutation logic. Suggested owner: src/main/java/com/motm/runtime/ability/field/FieldSinkholeHytaleAdapter.java."
        }
        if ($line -match 'private\s+void\s+restoreFieldTemporaryTerrain\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces field terrain restoration. Suggested owner: src/main/java/com/motm/runtime/ability/field/FieldTerrainHytaleAdapter.java."
        }
        if ($line -match 'private\s+void\s+(applyLavaPoolOwnerMobility|clearLavaPoolOwnerVelocityBoost|applyLavaPoolOwnerMovementBoost|clearLavaPoolOwnerMovementBoost|syncFollowOwnerFieldAnchor)\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces field owner-mobility mutation. Suggested owner: src/main/java/com/motm/runtime/ability/field/FieldOwnerMobilityHytaleAdapter.java."
        }
        if ($line -match 'private\s+FieldRuntimeResult\s+activatePersistentField\s*\(|FieldRuntimeResult') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces persistent field activation facade logic. Suggested owner: src/main/java/com/motm/runtime/ability/field/FieldActivationHytaleAdapter.java."
        }
        if ($line -match 'private\s+TerrainTickRuntime\.Hooks\s+terrainHooks\s*\(|private\s+boolean\s+process(TemporaryTerrainSelection|MovingTerrainTrail|StackingColumn)\s*\(|private\s+int\s+restoreTemporarySelectionsForWorld\s*\(|private\s+boolean\s+restoreTemporarySelection\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces terrain tick/restoration hook ownership. Suggested owner: src/main/java/com/motm/runtime/ability/terrain/TerrainHytaleAdapter.java."
        }
        if ($line -match 'private\s+.*\s+(placePersistentTerrainSelection|placeSupplementalSurfaceCue|startMovingTerrainTrail|pushTargetsOverlappingIronWall|placeWallSelection|placeIronWallSelection|placeSurfacePatchSelection|placeFloatingClusterSelection|placeSurfaceColumnSelection|placeStackingColumnSelection|placeColumnSelection|placeRingBlockSelection|placeTrailSelection|placeObsidianBlockShellSelection|placeFluidDiscSelection|placeGroundedFluidDiscSelection|placeTemporarySelection|baseSelection|resolveRuntimeBlockTypeId|resolveRuntimeFluidTypeId)\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces terrain placement/world-mutation ownership. Suggested owner: src/main/java/com/motm/runtime/ability/terrain/TerrainPlacementHytaleAdapter.java."
        }
        if ($line -match 'private\s+String\s+placeAbilityTerrainSelection\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces ability-specific terrain routing. Suggested owner: src/main/java/com/motm/runtime/ability/terrain/TerrainAbilityHytaleAdapter.java."
        }
        if ($line -match 'private\s+SupplementalTerrainRuntimeResult\s+activateSupplementalTerrainRuntime\s*\(|private\s+record\s+SupplementalTerrainRuntimeResult') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces supplemental terrain runtime ownership. Suggested owner: src/main/java/com/motm/runtime/ability/terrain/TerrainSupplementalHytaleAdapter.java."
        }
        if ($line -match 'private\s+.*\s+(spawnLapidaryGemProxy|processLapidaryGem|applyLapidaryGemLabel|despawnLapidaryGem|lapidaryGemLabel|resolveActiveLapidaryGemCenter|isGemAnchoredAbility)\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces Lapidary gem proxy ownership. Suggested owner: src/main/java/com/motm/runtime/ability/terrain/TerrainGemHytaleAdapter.java."
        }
        if ($line -match 'private\s+void\s+placeSinkholeSurfaceMarker\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces sinkhole marker terrain orchestration. Suggested owner: src/main/java/com/motm/runtime/ability/terrain/TerrainSinkholeMarkerHytaleAdapter.java."
        }
        if ($line -match 'private\s+.*\s+(spawnSummon|despawnSummon|createActiveSummon|resolveSummonRawBaseDamage|resolveSummonPosition|resolveSummonModelId)\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces summon spawn/lifecycle ownership. Suggested owner: src/main/java/com/motm/runtime/ability/summon/SummonLifecycleHytaleAdapter.java."
        }
        if ($line -match 'private\s+.*\s+(buffOwnedSummons|summonBuffResult|processSummonTick|moveSummonTowardOwner|moveSummonTowardTarget|moveSummonAwayFromTarget|awakenSummon|resolveSummonTarget|isValidNpcTarget|moveSummonBesideTarget)\s*\(|Summon(Tick|Buff|Target)Runtime\.Hooks|summonState\.removeProcessedSummons\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces summon buff/tick/control ownership. Suggested owner: src/main/java/com/motm/runtime/ability/summon/SummonControlHytaleAdapter.java."
        }
        if ($line -match 'private\s+.*\s+(performSummonAttack|applySummonAttackEffects|applySummonSplashToken|applySummonSplashDamage)\s*\(|SummonAttackRuntime\.Hooks|SummonAttackEffectRuntime\.Hooks|SummonSplashRuntime\.(TokenHooks|DamageHooks)') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces summon attack/effect/splash ownership. Suggested owner: src/main/java/com/motm/runtime/ability/summon/SummonAttackHytaleAdapter.java."
        }
        if ($line -match 'private\s+.*\s+(applyAlloyHeldItemVisual|clearAlloyHeldItemVisual|applyWeaponFollowUpSplash)\s*\(|WeaponFollowUpPrimaryHitRuntime\.PrimaryHitHooks|WeaponFollowUpHitEffects\.PayoffHooks|WeaponFollowUpNativeAlloyRuntime\.Hooks|WeaponFollowUpSplashRuntime\.SplashHooks|WeaponFollowUpVisualEffects\.VisualHooks') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces weapon follow-up Hytale mutation ownership. Suggested owner: src/main/java/com/motm/runtime/ability/followup/WeaponFollowUpHytaleAdapter.java."
        }
        if ($line -match 'private\s+.*\s+(processTransformationTick|createTransformationState|shouldEndTransformation|refreshTransformationOwnerState|applyOwnerRuntimeToken|transformationEffectHooks|applyTransformationPulseImpact|applyTransformationChargeImpact|applyTransformationWeaponRider|applyTransformationWeaponImpact|applyTransformationCleave)\s*\(|TransformationTickRuntime\.Hooks|TransformationEffectRuntime\.Hooks|transformationState\.removeProcessedTransformations\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces transformation Hytale mutation ownership. Suggested owner: src/main/java/com/motm/runtime/ability/transformation/TransformationHytaleAdapter.java."
        }
        if ($line -match 'private\s+.*\s+(processPlayerAnchor|processActiveSelfEffect|startActiveSelfEffect|startPlayerAnchor|setAnchorMovementFreeze|zeroVelocity)\s*\(|selfState\.removeProcessed(PlayerAnchors|SelfEffects)\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces self/anchor Hytale mutation ownership. Suggested owner: src/main/java/com/motm/runtime/ability/self/SelfHytaleAdapter.java."
        }
        if ($line -match 'private\s+.*\s+(inferLineControlDurationSeconds|applyRepeatingLineControlEffects|processChannelTick|processLineControlTick)\s*\(|channelState\.removeProcessed(LineControls|Channels)\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces channel/line-control Hytale mutation ownership. Suggested owner: src/main/java/com/motm/runtime/ability/channel/ChannelHytaleAdapter.java."
        }
        if ($line -match 'activation\.fields\s*\(\s*\)\.forEach\s*\(\s*fieldState::addField\s*\)') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces field batch registration loop. Suggested owner: src/main/java/com/motm/runtime/ability/field/FieldRuntimeState.java."
        }
        if ($line -match 'private\s+AbilitySpecificRuntimeResult\s+applySpecificCastRuntime\s*\(|case\s+"(metal_coat|alloy_enhancement|obsidian_skin|rooted|sapling|nightshade|frolick|cacti_cluster|lapidary|glare|debris|fracture|refraction|gargoyle|sandstorm|tunnel)"') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces manager-local ability-specific runtime routing. Suggested owner: src/main/java/com/motm/runtime/ability/specific/AbilitySpecificHytaleAdapter.java or a narrower runtime-family adapter."
        }
        if ($line -match 'private\s+static\s+final\s+Set<String>\s+(MOVEMENT_CAST_TYPES|LINE_CAST_TYPES|MULTI_TARGET_CAST_TYPES|CASTER_EFFECT_TOKENS|TARGET_EFFECT_TOKENS)\s*=|private\s+boolean\s+(suppressGenericCasterVisual|isGroundRestrictedAbility|isAnchorDragAbility|isProjectileLike)\s*\(|"(alloy_enhancement|dominate|combust|consume)"\.equals\s*\(\s*lower\s*\(\s*ability\.getId\s*\(\s*\)\s*\)\s*\)') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces generic ability execution policy ownership. Suggested owner: src/main/java/com/motm/runtime/ability/AbilityExecutionPolicy.java."
        }
        if ($line -match 'private\s+String\s+(resolveImpactEffectId|resolveProjectileVisualEffectId|resolveFieldVisualEffectId|resolveEffectId|resolveThemedEffectId|asRuntimeEffectId)\s*\(|private\s+enum\s+RuntimeEffectKind') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces runtime effect-id resolver ownership. Suggested owner: src/main/java/com/motm/runtime/ability/AbilityRuntimeEffects.java."
        }
        if ($line -match 'private\s+(StatusEffect|int|double)\s+(createStatusEffect|resolveDurationTicks|defaultDurationSeconds)\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces ability status-effect construction policy. Suggested owner: src/main/java/com/motm/runtime/ability/AbilityStatusEffects.java."
        }
        if ($line -match 'private\s+(double|boolean)\s+(resolveTargetSequenceDamageMultiplier|resolveHorizontalMovement|resolveVerticalMovement|resolveRange|resolveFieldPulseDamage|isPersistentFieldAbility|resolvePullStep|resolveFieldPullLift|resolveDamageAmount)\s*\(') {
            Fail "src/main/java/com/motm/manager/GameplayPlaybackManager.java`:$lineNumber reintroduces ability runtime math policy. Suggested owner: src/main/java/com/motm/runtime/ability/AbilityRuntimeMath.java or a narrower runtime-family spec."
        }
    }
    if ($rawRuntimeCollectionCount -le $allowedRawRuntimeCollectionCount) {
        Pass "GameplayPlaybackManager raw runtime collection ratchet: $rawRuntimeCollectionCount/$allowedRawRuntimeCollectionCount"
    }
    if ($directAbilityStateConstructionCount -le $allowedDirectAbilityStateConstructionCount) {
        Pass "GameplayPlaybackManager migrated ability state construction ratchet: $directAbilityStateConstructionCount/$allowedDirectAbilityStateConstructionCount"
    }
}

$runtimeTasks = Join-Path $ProjectRoot "src/main/java/com/motm/runtime/MotmRuntimeTasks.java"
if (Test-Path -LiteralPath $runtimeTasks) {
    $lineNumber = 0
    foreach ($line in Get-Content -LiteralPath $runtimeTasks) {
        $lineNumber++
        if ($line -match 'public\s+(Set|ConcurrentLinkedQueue)\s*<[^>]+>\s+[A-Za-z0-9_]+\s*\(\s*\)') {
            Fail "src/main/java/com/motm/runtime/MotmRuntimeTasks.java`:$lineNumber exposes a mutable task collection. Use immutable pending views plus complete/cancel intent methods."
        }
        if ($line -match 'public\s+Map\s*<[^>]+>\s+([A-Za-z0-9_]+)\s*\(\s*\)') {
            $methodName = $Matches[1]
            if ($methodName -notmatch '^pending' -and $methodName -ne "snapshot") {
                Fail "src/main/java/com/motm/runtime/MotmRuntimeTasks.java`:$lineNumber exposes a mutable task map-style accessor '$methodName'. Use immutable pending views plus complete/cancel intent methods."
            }
        }
    }
}

$inventoryMutationPattern = '\.(setItem|removeItem|removeAt|removeSlot|setStack|removeStack)\s*\('
$allowedInventoryFiles = @(
    "src/main/java/com/motm/util/MotmInventoryOps.java"
)
foreach ($file in $javaFiles) {
    $relative = Get-RelativePath $file.FullName
    if ($allowedInventoryFiles -contains $relative) {
        continue
    }
    $lineNumber = 0
    foreach ($line in Get-Content -LiteralPath $file.FullName) {
        $lineNumber++
        if ($line -match 'MotmInventoryOps\.') {
            continue
        }
        if ($line -match $inventoryMutationPattern) {
            Fail "$relative`:$lineNumber appears to mutate inventory directly. Suggested owner: src/main/java/com/motm/util/MotmInventoryOps.java."
        }
    }
}

foreach ($file in $javaFiles) {
    $relative = Get-RelativePath $file.FullName
    $lineNumber = 0
    foreach ($line in Get-Content -LiteralPath $file.FullName) {
        $lineNumber++
        if ($line -match '\b(player|runtimePlayer|sender|candidate)\.getPlayerRef\s*\(') {
            Fail "$relative`:$lineNumber calls deprecated Player.getPlayerRef(). Suggested owner: resolve PlayerRef through the entity store component via player.getReference()."
        }
    }
}

$scenarioDir = Join-Path $ProjectRoot "scripts/scenarios"
if (-not (Test-Path -LiteralPath $scenarioDir)) {
    Fail "scripts/scenarios is missing. Suggested owner: scripts/scenarios/<scenario-id>.json for agent-driven harness extension."
} else {
    $scenarioCount = @(Get-ChildItem -LiteralPath $scenarioDir -Filter "*.json").Count
    if ($scenarioCount -lt 1) {
        Fail "scripts/scenarios contains no scenario JSON files."
    }
    foreach ($scenarioPath in Get-ChildItem -LiteralPath $scenarioDir -Filter "*.json") {
        $scenarioText = Get-Content -LiteralPath $scenarioPath.FullName -Raw
        foreach ($requiredField in @('"setupCommands"', '"cleanupCommands"', '"expectedEvidence"')) {
            if ($scenarioText -notmatch [regex]::Escape($requiredField)) {
                Fail "$(Get-RelativePath $scenarioPath.FullName) is missing $requiredField. Scenario choreography and evidence expectations belong in the catalog, not hard-coded runner branches."
            }
        }
    }
}

$stalePathFiles = @(
    Join-Path $ProjectRoot "AGENTS.md",
    Join-Path $ProjectRoot "README.md"
) + @($javaFiles | ForEach-Object { $_.FullName }) + @(
    Get-ChildItem -LiteralPath (Join-Path $ProjectRoot "scripts") -Recurse -File -Include "*.ps1" | ForEach-Object { $_.FullName }
)
$staleGuidancePattern = '(?i)(extend|add|put|wire).{0,40}(legacy|deprecated|old path|processPending|MenteesMod pending|screenshot-only acceptance)'
foreach ($path in $stalePathFiles) {
    if (-not (Test-Path -LiteralPath $path)) {
        continue
    }
    $relative = Get-RelativePath $path
    if ($relative -eq "scripts/check-architecture.ps1") {
        continue
    }
    $lineNumber = 0
    foreach ($line in Get-Content -LiteralPath $path) {
        $lineNumber++
        if ($line -match $staleGuidancePattern) {
            Fail "$relative`:$lineNumber appears to direct agents toward a stale or deprecated extension path. Prefer the named owner and compatibility register."
        }
    }
}

$compatibilityRegister = Join-Path $ProjectRoot "docs/compatibility-register.md"
if (-not (Test-Path -LiteralPath $compatibilityRegister)) {
    Fail "docs/compatibility-register.md is missing. Legacy or compatibility exceptions must be explicit, owned, and removable."
} else {
    $registerText = Get-Content -LiteralPath $compatibilityRegister -Raw
    foreach ($requiredSection in @("## Removal Rules", "## Active Compatibility Exceptions")) {
        if ($registerText -notmatch [regex]::Escape($requiredSection)) {
            Fail "docs/compatibility-register.md is missing required section '$requiredSection'."
        }
    }
    foreach ($requiredTerm in @("Owner:", "Implementation boundary:", "Consumer:", "Preferred replacement:", "Verification evidence:", "Removal gate:")) {
        if ($registerText -notmatch [regex]::Escape($requiredTerm)) {
            Fail "docs/compatibility-register.md must describe compatibility entries with '$requiredTerm'."
        }
    }

    $legacyCodePattern = '(?i)\b(legacy|compatibility|deprecated|old path)\b'
    foreach ($file in $javaFiles) {
        $relative = Get-RelativePath $file.FullName
        $lineNumber = 0
        foreach ($line in Get-Content -LiteralPath $file.FullName) {
            $lineNumber++
            if ($line -match $legacyCodePattern -and $registerText -notmatch [regex]::Escape($relative)) {
                Fail "$relative`:$lineNumber mentions legacy/compatibility code but the file is not listed in docs/compatibility-register.md. Delete the old path or register an explicit implementation boundary with an active consumer and removal gate."
            }
        }
    }
}

if ($script:Failed) {
    Write-Host "Architecture check: FAIL" -ForegroundColor Red
    exit 1
}

Pass "Architecture check passed"
