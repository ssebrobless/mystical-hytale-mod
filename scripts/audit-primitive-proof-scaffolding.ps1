param()

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$summonAcceptancePath = Join-Path $repoRoot "scripts\run-summon-acceptance.ps1"
$assertAbilityProofPath = Join-Path $repoRoot "scripts\assert-ability-proof.ps1"
$commandPath = Join-Path $repoRoot "src\main\java\com\motm\command\MotmCommand.java"
$captureEvidencePath = Join-Path $repoRoot "scripts\capture-evidence.ps1"

$summonAcceptance = Get-Content -LiteralPath $summonAcceptancePath -Raw
$assertAbilityProof = Get-Content -LiteralPath $assertAbilityProofPath -Raw
$command = Get-Content -LiteralPath $commandPath -Raw
$captureEvidence = Get-Content -LiteralPath $captureEvidencePath -Raw

$failures = New-Object System.Collections.Generic.List[string]

if ($summonAcceptance -notmatch 'Send-MotmCommand\s+"motm dev clear"') {
    $failures.Add("Summon acceptance must run /motm dev clear after observation to prove temporary summon cleanup.")
}

if ($summonAcceptance -notmatch '\$cleanup\s*=') {
    $failures.Add("Summon acceptance must collect cleanup log lines after /motm dev clear.")
}

if ($summonAcceptance -notmatch 'Cleanup marker') {
    $failures.Add("Summon acceptance report must include a cleanup marker gate.")
}

if ($summonAcceptance -notmatch '\$cleanupStatus\s*=') {
    $failures.Add("Summon acceptance cleanup gate must compute a cleanup verdict from despawn and/or dev-clear evidence.")
}

if ($command -notmatch 'clearActiveSummonsForOwner\(player\.getPlayerId\(\)\)') {
    $failures.Add("/motm dev clear must clear active summons owned by the player.")
}

if ($captureEvidence -notmatch '\[string\]\$WindowTitle') {
    $failures.Add("Evidence capture must support a WindowTitle target so Hytale proofs are not blocked by foreground desktop windows.")
}

if ($summonAcceptance -notmatch '-WindowTitle\s+"Hytale"') {
    $failures.Add("Summon acceptance must capture the Hytale window, not the whole desktop.")
}

if ($summonAcceptance -match '-Action\s+Key\s+-Keys\s+"2"') {
    $failures.Add("Summon acceptance must not press hotbar/ability key 2 before commands; that can fire an unrelated ability and invalidate the run.")
}

if ($summonAcceptance -match 'NoClassDefFoundError\|ClassNotFoundException\|Exception\|ERROR\|Reloading nonexistent\|Unmapped NPC type') {
    $failures.Add("Summon acceptance must not treat informational Unmapped NPC type lines as blocking when summon combat markers prove behavior.")
}

if ($assertAbilityProof -notmatch '"summon"\s*\{[\s\S]*summon combat spawn:[\s\S]*summon combat target:[\s\S]*summon combat attack:.*damage=\[1-9\]') {
    $failures.Add("Generic summon proof must require spawn, target acquisition, and damaging attack markers.")
}

if ($failures.Count -gt 0) {
    Write-Host "[primitive-proof-scaffolding] FAIL"
    foreach ($failure in $failures) {
        Write-Host " - $failure"
    }
    exit 1
}

Write-Host "[primitive-proof-scaffolding] PASS"
