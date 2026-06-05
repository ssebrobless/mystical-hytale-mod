param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$playbackPath = Join-Path $repoRoot "src\main\java\com\motm\manager\GameplayPlaybackManager.java"
$mobRuntimePath = Join-Path $repoRoot "src\main\java\com\motm\system\MotmMobRuntimeSystem.java"
$modPath = Join-Path $repoRoot "src\main\java\com\motm\MenteesMod.java"
$snowImpModelPath = Join-Path $repoRoot "src\main\resources\Server\Models\MOTM\Summons\Snow_Imp_Snowman.json"

$playback = Get-Content $playbackPath -Raw
$mobRuntime = Get-Content $mobRuntimePath -Raw
$mod = Get-Content $modPath -Raw

$failures = New-Object System.Collections.Generic.List[string]

if ($playback -notmatch 'summon\.setRoleName\(roleNameId\)') {
    $failures.Add("GameplayPlaybackManager must spawn summons with a stable real Hytale NPC role id; fake item/model roles fail to reload at runtime.")
}

if ($playback -notmatch 'NPCEntity\.setAppearance\(summonRef,\s*appearanceModelId,\s*summonRef\.getStore\(\)\)') {
    $failures.Add("GameplayPlaybackManager must keep summon appearance/model separate from the stable NPC role id.")
}

if (-not (Test-Path $snowImpModelPath)) {
    $failures.Add("Snow Imp must register a MOTM ModelAsset wrapper for WinterHoliday_Snowman so the summon can move as the NPC body instead of only as a block proxy.")
} else {
    $snowImpModel = Get-Content $snowImpModelPath -Raw
    if ($snowImpModel -notmatch 'WinterHoliday_Snowman\.blockymodel') {
        $failures.Add("Snow Imp ModelAsset wrapper must be backed by the WinterHoliday_Snowman block model requested by the user.")
    }
    if ($snowImpModel -match 'Icons/ItemsGenerated/') {
        $failures.Add("Snow Imp ModelAsset must not use item icon roots; Hytale only accepts model icon roots for Server/Models assets.")
    }
}

if ($playback -notmatch 'MOTM_SNOW_IMP_MODEL_ASSET_ID' -or $playback -notmatch 'Snow_Imp_Snowman') {
    $failures.Add("Snow Imp must prefer the MOTM snowman ModelAsset before falling back to item/block proxy primitives.")
}

if ($playback -notmatch 'case "snow_imp" -> "ground_snowman"') {
    $failures.Add("Snow Imp must use a grounded melee snowman behavior role, not a ranged skirmisher profile.")
}

if ($playback -notmatch 'case "snow_imp" -> "MOTM_Summon_Driver"') {
    $failures.Add("Snow Imp must use the stable MOTM summon driver role for grounded movement instead of Spirit_Frost/Template_Spirit flying-style movement.")
}

if ($playback -notmatch 'case "frosty_golem" -> "Yeti"') {
    $failures.Add("Frosty must use the real existing Yeti NPC role, not the crystal golem fallback.")
}

if ($playback -notmatch 'resolveSummonDurationSeconds' -or $playback -notmatch '"frosty_golem"\.equals\(summonName\)' -or $playback -notmatch 'Math\.max\(240\.0, summonDurationSeconds \+ 30\.0\)') {
    $failures.Add("Frosty/Yeti duration must be mod-controlled and long enough for visual testing; native NPC despawn must be a safety fallback.")
}

if ($playback -notmatch 'resolveSummonThinkIntervalMillis') {
    $failures.Add("Summon runtime must support role-specific think intervals so grounded item bodies do not snap in large jumps.")
}

if ($playback -match 'Math\.floor\(summonPosition\.y\)\s*-\s*1') {
    $failures.Add("Grounded item-summon block proxies must not anchor one block below the summon and replace real terrain.")
}

if ($playback -notmatch 'visualBlockAnchor\.x == x' -or $playback -notmatch 'visualBlockAnchor\.z == z') {
    $failures.Add("Grounded item-summon block proxies must keep a sticky Y anchor while staying in the same horizontal block.")
}

if ($playback -notmatch 'SNOWMAN_MODEL_ASSET_CANDIDATES' -or $playback -notmatch 'getAssetMap\(\)\.getAssetMap\(\)') {
    $failures.Add("Snow Imp item-model summons must probe the loaded ModelAsset key map, not assume the .blockymodel path is the runtime key.")
}

if ($playback -notmatch 'itemModelProxyRequired') {
    $failures.Add("Summon runtime must explicitly mark item-model proxy fallback requirements when an item model cannot be attached to an NPC.")
}

if ($playback -notmatch 'suppressSummonDrops' -or $playback -notmatch 'setDeathItemsDropped\(\)') {
    $failures.Add("Summons must suppress NPC death drops at spawn/death/despawn so temporary allied summons never drop items when killed.")
}

foreach ($required in @(
        'ItemComponent.generateItemDrop',
        'PreventPickup.INSTANCE',
        'PreventItemMerging.INSTANCE',
        'syncSummonVisualProxy',
        'removeSummonVisualProxy',
        'spawnSummonBlockVisualProxy',
        'EntityScaleComponent',
        'HiddenFromAdventurePlayers.INSTANCE',
        'summon visual block proxy'
    )) {
    if ($playback -notmatch [regex]::Escape($required)) {
        $failures.Add("Item-based summon proxy primitive must include '$required'.")
    }
}

if ($playback -notmatch 'boolean isActiveSummonRef\(Ref<EntityStore> ref\)') {
    $failures.Add("GameplayPlaybackManager must expose active summon ref tracking so allied summons can be ignored by hostile targeting without a fake NPC role.")
}

if ($playback -notmatch 'clearSummonFriendlyMarkedTargets' -or $playback -notmatch 'summon friendly target blocked') {
    $failures.Add("Summons must not target, damage, or keep native marked-target locks on other active MOTM summons.")
}

foreach ($marker in @(
        'summon combat spawn',
        'summon combat target',
        'summon combat attack',
        'summon combat despawn'
    )) {
    if ($playback -notmatch [regex]::Escape($marker)) {
        $failures.Add("GameplayPlaybackManager must emit '$marker' evidence for summon acceptance review.")
    }
}

if ($mobRuntime -notmatch 'isActiveSummonRef\(ref\)') {
    $failures.Add("MotmMobRuntimeSystem must ignore active MOTM summon refs when tracking hostile mobs.")
}

if ($mod -notmatch 'isActiveSummonRef\(ref\)') {
    $failures.Add("Live style-test target selection must ignore active MOTM summon refs.")
}

if ($mod -notmatch 'suppressStyleTestNpcDrops' -or $mod -notmatch 'Style test NPC no-drop guard') {
    $failures.Add("Style-test mobs must suppress death drops so summon aggro tests do not litter items or confuse summon no-drop evidence.")
}

if ($mod -notmatch '"Yeti"' -or $mod -notmatch '"Golem_Crystal_Frost"' -or $mod -notmatch '"Skeleton_Fighter"') {
    $failures.Add("Style-test cleanup must remove stale summon driver mobs, including old Frosty, Yeti Frosty, and Snow Imp bodies.")
}

if ($failures.Count -gt 0) {
    Write-Host "[summon-contract] FAIL"
    foreach ($failure in $failures) {
        Write-Host " - $failure"
    }
    exit 1
}

Write-Host "[summon-contract] PASS"
