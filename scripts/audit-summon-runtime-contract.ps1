param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$playbackPath = Join-Path $repoRoot "src\main\java\com\motm\manager\GameplayPlaybackManager.java"
$mobRuntimePath = Join-Path $repoRoot "src\main\java\com\motm\system\MotmMobRuntimeSystem.java"
$modPath = Join-Path $repoRoot "src\main\java\com\motm\MenteesMod.java"

$playback = Get-Content $playbackPath -Raw
$mobRuntime = Get-Content $mobRuntimePath -Raw
$mod = Get-Content $modPath -Raw

$failures = New-Object System.Collections.Generic.List[string]

if ($playback -notmatch 'summon\.setRoleName\(modelId\)') {
    $failures.Add("GameplayPlaybackManager must spawn summons with a real Hytale NPC role/model id; fake roles fail to reload at runtime.")
}

if ($playback -notmatch 'NPCEntity\.setAppearance\(summonRef,\s*modelId,\s*summonRef\.getStore\(\)\)') {
    $failures.Add("GameplayPlaybackManager must keep summon appearance/model separate from the allied role marker.")
}

if ($playback -notmatch 'boolean isActiveSummonRef\(Ref<EntityStore> ref\)') {
    $failures.Add("GameplayPlaybackManager must expose active summon ref tracking so allied summons can be ignored by hostile targeting without a fake NPC role.")
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

if ($failures.Count -gt 0) {
    Write-Host "[summon-contract] FAIL"
    foreach ($failure in $failures) {
        Write-Host " - $failure"
    }
    exit 1
}

Write-Host "[summon-contract] PASS"
