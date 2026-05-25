param(
    [string]$AbilityId,
    [string]$Scenario,
    [string]$LogPath,
    [string]$SinceMarker,
    [string]$OutFile,
    [switch]$RequireMechanical,
    [switch]$RequireVisual
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AbilityId)) {
    throw "AbilityId is required."
}

if (-not $LogPath) {
    $defaultLogDir = Join-Path $env:APPDATA "Hytale\UserData\Saves\MOTM Creative Test\logs"
    $latest = Get-ChildItem -LiteralPath $defaultLogDir -Filter "*_server.log" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $latest) { throw "No server log found under $defaultLogDir." }
    $LogPath = $latest.FullName
}

if (-not (Test-Path -LiteralPath $LogPath)) {
    throw "LogPath not found: $LogPath"
}

$lines = @(Get-Content -LiteralPath $LogPath -ErrorAction Stop)
if ($SinceMarker) {
    $idx = -1
    for ($i = $lines.Count - 1; $i -ge 0; $i--) {
        if ($lines[$i] -match [regex]::Escape($SinceMarker)) {
            $idx = $i
            break
        }
    }
    if ($idx -ge 0) {
        $lines = @($lines | Select-Object -Skip $idx)
    }
}

$escaped = [regex]::Escape($AbilityId)
$resultIndex = -1
for ($i = $lines.Count - 1; $i -ge 0; $i--) {
    if ($lines[$i] -match "Queued ability cast result:.*abilityId=$escaped") {
        $resultIndex = $i
        break
    }
}

$proofLines = $lines
if (-not $SinceMarker -and $resultIndex -ge 0) {
    $start = [Math]::Max(0, $resultIndex - 12)
    $end = [Math]::Min($lines.Count - 1, $resultIndex + 60)
    $proofLines = @($lines[$start..$end])
}

$abilityLines = @($proofLines | Where-Object {
    $_ -match "abilityId=$escaped" -or
    $_ -match "Queued ability cast result:.*abilityId=$escaped" -or
    $_ -match "Cast .*Runtime:" -and $_ -match $escaped
})
$resultLine = @($proofLines | Where-Object { $_ -match "Queued ability cast result:.*abilityId=$escaped" } | Select-Object -Last 1)
$blocking = @($proofLines | Where-Object {
    $_ -match "NoClassDefFoundError|ClassNotFoundException|Exception|ERROR|Reloading nonexistent|Unmapped NPC type"
})
$noTarget = @($proofLines | Where-Object { $_ -match "No valid target" -and $_ -match $escaped })

function Test-MechanicalProof([string]$ScenarioName, $AllLines, $CastResultLine) {
    $resultText = [string]($CastResultLine -join "`n")
    switch ($ScenarioName) {
        "jump_land" {
            $landing = @($AllLines | Where-Object { $_ -match "landing resolved: targets=[1-9]" } | Select-Object -Last 1)
            if ($landing.Count -gt 0) { return @("PASS", $landing[0]) }
            return @("FAIL", "No landing-resolution line with targets>=1.")
        }
        "projectile_line" {
            if ($resultText -match "launched [1-9] projectile") {
                return @("REVIEW", "Projectile launched; target-side impact still needs hit/effect proof.")
            }
            if ($resultText -match "[1-9] hit|applied .* to [1-9] target") {
                return @("PASS", "Cast result includes target-side hit/effect.")
            }
            return @("FAIL", "No projectile launch or target-side hit/effect proof.")
        }
        "ground_zone" {
            if ($resultText -match "field active|field arms|radius .*m" -and $resultText -match "applied .* to [1-9] target|[1-9] hit") {
                return @("PASS", "Field result includes duration/radius and target-side effect.")
            }
            if ($resultText -match "field active|field arms|radius .*m") {
                return @("REVIEW", "Field exists; target-side tick/effect proof is weak.")
            }
            return @("FAIL", "No field active/radius proof.")
        }
        "ground_target" {
            if ($resultText -match "[1-9] hit|applied .* to [1-9] target|barrier active|hazard arms") {
                return @("PASS", "Ground target produced hit/effect/barrier/hazard proof.")
            }
            return @("REVIEW", "Ground target cast result exists but needs stronger target-side proof.")
        }
        "facing_cone" {
            if ($resultText -match "[1-9] hit|applied .* to [1-9] target") {
                return @("PASS", "Facing/cone target-side effect found.")
            }
            return @("FAIL", "No target-side hit/effect for facing/cone ability.")
        }
        "movement" {
            if ($resultText -match "[1-9] hit|applied .* to [1-9] target|dash|leap|teleport|movement") {
                return @("REVIEW", "Movement runtime fired; displacement still needs before/after proof.")
            }
            return @("FAIL", "No movement or target-side proof.")
        }
        "self_buff" {
            if ($resultText -match "self|buff|shield|heal|follow-up|form|evasion|defense") {
                return @("PASS", "Self-buff/status proof found in cast result.")
            }
            return @("REVIEW", "Self cast exists but status/HUD proof is weak.")
        }
        "support_heal" {
            if ($resultText -match "heal|shield|buff|aura") {
                return @("PASS", "Support/heal status proof found in cast result.")
            }
            return @("REVIEW", "Support cast exists but HP/stat proof is weak.")
        }
        "summon" {
            if ($resultText -match "summon|spawn") {
                return @("REVIEW", "Summon runtime fired; visual/action proof still needed.")
            }
            return @("FAIL", "No summon/spawn proof in cast result.")
        }
        "cleanse" {
            if ($resultText -match "cleanse|purge|purify|clear") {
                return @("REVIEW", "Cleanse runtime fired; pre-applied debuff removal proof still needed.")
            }
            return @("FAIL", "No cleanse proof in cast result.")
        }
        default {
            if ($resultText -match "[1-9] hit|applied .* to [1-9] target|Runtime:") {
                return @("PASS", "Generic cast/hit/status proof found.")
            }
            return @("REVIEW", "Runtime proof exists; concept-specific proof not classified.")
        }
    }
}

if (-not $Scenario) {
    $Scenario = "unknown"
}

$runtimeStatus = "FAIL"
$runtimeNote = "Queued cast result missing."
if ($resultLine.Count -gt 0 -and $blocking.Count -eq 0 -and $noTarget.Count -eq 0) {
    $runtimeStatus = "PASS"
    $runtimeNote = $resultLine[0]
} elseif ($resultLine.Count -gt 0) {
    $runtimeStatus = "FAIL"
    $runtimeNote = "Cast result exists but blocking/no-target evidence was found."
}

$mechanical = Test-MechanicalProof $Scenario $proofLines $resultLine
$mechanicalStatus = $mechanical[0]
$mechanicalNote = $mechanical[1]
$visualStatus = "REVIEW"
$visualNote = "Requires screenshot/video review against style palette and ability motion."

$report = @(
    "# Ability Proof: $AbilityId",
    "",
    "- Log: $LogPath",
    "- Scenario: $Scenario",
    "- Runtime: $runtimeStatus - $runtimeNote",
    "- Mechanical: $mechanicalStatus - $mechanicalNote",
    "- Visual: $visualStatus - $visualNote"
)

if ($blocking.Count -gt 0) {
    $report += "- Blocking lines:"
    $report += @($blocking | ForEach-Object { "  - $_" })
}
if ($noTarget.Count -gt 0) {
    $report += "- No-target lines:"
    $report += @($noTarget | ForEach-Object { "  - $_" })
}

if ($OutFile) {
    $parent = Split-Path -Parent $OutFile
    if ($parent) { New-Item -ItemType Directory -Path $parent -Force | Out-Null }
    $report | Set-Content -LiteralPath $OutFile -Encoding UTF8
    Write-Host "[assert-ability-proof] Wrote $OutFile"
} else {
    $report | Write-Output
}

$failed = $runtimeStatus -ne "PASS"
if ($RequireMechanical -and $mechanicalStatus -ne "PASS") { $failed = $true }
if ($RequireVisual -and $visualStatus -ne "PASS") { $failed = $true }
if ($failed) { exit 1 }
