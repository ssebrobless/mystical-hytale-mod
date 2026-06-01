param(
    [ValidateSet("terra", "hydro", "aero", "corruptus")]
    [string]$ClassId = "terra",
    [string]$WorldName = "MOTM Creative Test",
    [string[]]$Styles = @(),
    [switch]$SkipFlatlandsGate,
    [switch]$SkipThirdPerson,
    [switch]$UseThirdPerson,
    [switch]$RequireConceptProof,
    [int]$CommandDelayMilliseconds = 850,
    [int]$PostCastMilliseconds = 2400
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
$phaseId = "phase9-$ClassId-flatlands"
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $phaseId $runId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

$stylePath = Join-Path $repoRoot "src\main\resources\data\styles\${ClassId}_styles.json"
if (-not (Test-Path -LiteralPath $stylePath)) {
    throw "Style file not found: $stylePath"
}
$styleFile = Get-Content -LiteralPath $stylePath -Raw | ConvertFrom-Json
$allStyles = @($styleFile.styles)
if ($Styles.Count -gt 0) {
    $wanted = @{}
    $Styles | ForEach-Object { $wanted[$_.ToLowerInvariant()] = $true }
    $allStyles = @($allStyles | Where-Object { $wanted.ContainsKey($_.id.ToLowerInvariant()) })
}
if ($allStyles.Count -eq 0) {
    throw "No styles selected for class '$ClassId'."
}

$report = New-Object System.Collections.Generic.List[string]
function Add-Line([string]$Line) {
    $script:report.Add($Line)
}

function Add-RunMarker([string]$Marker) {
    Send-MotmCommand "motm dev audit marker $Marker" 350
}

function Get-AbilityScenario($Ability) {
    $castType = ([string]$Ability.cast_type).ToLowerInvariant()
    $targetType = ([string]$Ability.target_type).ToLowerInvariant()
    $trigger = ([string]$Ability.trigger).ToLowerInvariant()
    $effect = ([string]$Ability.effect).ToLowerInvariant()
    $description = ([string]$Ability.description).ToLowerInvariant()
    $kind = "single_target"
    $setup = "Spawn grounded target in range and face it before cast."
    $proof = "Cast result plus target-side hit/effect evidence."
    if ($trigger -eq "jump_land") {
        $kind = "jump_land"
        $setup = "Arm ability, jump and land within 3m of grounded target."
        $proof = "Landing resolution log with targets>=1 plus impact screenshot."
    } elseif ($castType -in @("dash", "dash_buff", "dash_strike", "leap", "dive_strike", "teleport", "air_stall")) {
        $kind = "movement"
        $setup = "Face target, start with clear lane, and move through the ability path."
        $proof = "Before/after displacement or target-side hit/effect after motion."
    } elseif ($castType -in @("cone", "gaze")) {
        $kind = "facing_cone"
        $setup = "Face target in a narrow camera cone before cast."
        $proof = "Target-side effect or hit; no 'No valid target' accepted."
    } elseif ($castType -in @("ground_zone", "support_zone")) {
        $kind = "ground_zone"
        $setup = "Grounded target inside radius; wait for persistence/tick window."
        $proof = "Field active/pulse line, target-side damage/status, persistent-area screenshot."
    } elseif ($castType -in @("ground_target", "ground_strike", "barrier")) {
        $kind = "ground_target"
        $setup = "Aim at target block or target feet; wait delay_seconds when present."
        $proof = "Telegraph/delay if present, then hit/status/impact visual."
    } elseif ($castType -like "projectile*" -or $castType -in @("line_control", "wave_line", "chain")) {
        $kind = "projectile_line"
        $setup = "Place grounded target in visible lane and keep aim steady through impact window."
        $proof = "Launch plus target-side hit/effect/impact evidence."
    } elseif ($castType -in @("summon", "summon_buff")) {
        $kind = "summon"
        $setup = "Clear arena before cast, then wait for summon appearance/action window."
        $proof = "Summon appears and survives or acts; no unmapped/nonexistent role warnings."
    } elseif ($castType -in @("cleanse") -or $effect -match "cleanse|purify" -or $description -match "cleanse|purge|purify") {
        $kind = "cleanse"
        $setup = "Pre-apply debuff/damage condition where available."
        $proof = "Debuff setup exists, then clear/remove log after cast."
    } elseif ($targetType -eq "self" -or $castType -match "self|transformation|form|buff") {
        $kind = "self_buff"
        $setup = "Use third-person camera and capture caster body/HUD/status after cast."
        $proof = "Buff/shield/heal/status log plus third-person visual screenshot."
    } elseif ($effect -match "heal" -or $description -match "heal") {
        $kind = "support_heal"
        $setup = "Use damaged or buffable caster/ally; capture HP/status proof."
        $proof = "HP/stat/status improvement in log or HUD screenshot."
    }
    return [pscustomobject]@{
        Kind = $kind
        Setup = $setup
        Proof = $proof
    }
}

function Get-MechanicalProof($Scenario, $Lines, [string]$ResultLine, [string]$AbilityId) {
    $resultText = [string]$ResultLine
    switch ($Scenario.Kind) {
        "jump_land" {
            $landing = $Lines |
                Where-Object { $_ -match "landing resolved: targets=[1-9]" } |
                Select-Object -Last 1
            if ($landing) { return [pscustomobject]@{ Status = "PASS"; Note = $landing } }
            return [pscustomobject]@{ Status = "FAIL"; Note = "No landing-resolution line with targets>=1." }
        }
        "projectile_line" {
            if ($resultText -match "[1-9] hit|applied .* to [1-9] target") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Target-side hit/effect is present in cast result." }
            }
            if ($resultText -match "launched [1-9] projectile") {
                return [pscustomobject]@{ Status = "REVIEW"; Note = "Projectile launched; target-side impact still needs proof." }
            }
            return [pscustomobject]@{ Status = "FAIL"; Note = "No projectile launch or target-side hit/effect proof." }
        }
        "ground_zone" {
            if ($resultText -match "field active|field arms|radius .*m" -and $resultText -match "[1-9] hit|applied .* to [1-9] target") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Field duration/radius and target-side effect are present." }
            }
            if ($resultText -match "field active|field arms|radius .*m") {
                return [pscustomobject]@{ Status = "REVIEW"; Note = "Field exists; tick/effect proof is weak." }
            }
            return [pscustomobject]@{ Status = "FAIL"; Note = "No field active/radius proof." }
        }
        "facing_cone" {
            if ($resultText -match "[1-9] hit|applied .* to [1-9] target") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Facing/cone target-side effect found." }
            }
            return [pscustomobject]@{ Status = "FAIL"; Note = "No target-side hit/effect for facing/cone ability." }
        }
        "movement" {
            if ($resultText -match "[1-9] hit|applied .* to [1-9] target|dash|leap|teleport|movement") {
                return [pscustomobject]@{ Status = "REVIEW"; Note = "Movement runtime fired; displacement needs before/after proof." }
            }
            return [pscustomobject]@{ Status = "FAIL"; Note = "No movement or target-side proof." }
        }
        "self_buff" {
            if ($resultText -match "self|buff|shield|heal|follow-up|form|evasion|defense") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Self-buff/status proof found in cast result." }
            }
            return [pscustomobject]@{ Status = "REVIEW"; Note = "Self cast exists but status/HUD proof is weak." }
        }
        "support_heal" {
            if ($resultText -match "heal|shield|buff|aura") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Support/heal status proof found in cast result." }
            }
            return [pscustomobject]@{ Status = "REVIEW"; Note = "Support cast exists but HP/stat proof is weak." }
        }
        "summon" {
            $escapedAbility = [regex]::Escape($AbilityId)
            $summonLines = @($Lines | Where-Object {
                $_ -match "summon combat .*ability=$escapedAbility"
            })
            $spawn = @($summonLines | Where-Object { $_ -match "summon combat spawn:" } | Select-Object -First 1)
            $target = @($summonLines | Where-Object { $_ -match "summon combat target:" } | Select-Object -First 1)
            $attack = @($summonLines | Where-Object { $_ -match "summon combat attack:.*damage=[1-9]" } | Select-Object -First 1)
            $despawn = @($summonLines | Where-Object { $_ -match "summon combat despawn:" } | Select-Object -First 1)
            if ($spawn.Count -gt 0 -and $target.Count -gt 0 -and $attack.Count -gt 0) {
                $cleanup = if ($despawn.Count -gt 0) { "despawn observed" } else { "despawn not observed in this window" }
                return [pscustomobject]@{ Status = "PASS"; Note = "Summon spawned, acquired a target, attacked with damage, and $cleanup." }
            }
            if ($spawn.Count -gt 0) {
                return [pscustomobject]@{ Status = "REVIEW"; Note = "Summon spawned, but target/attack proof is incomplete." }
            }
            if ($resultText -match "summon|spawn") {
                return [pscustomobject]@{ Status = "REVIEW"; Note = "Summon cast result exists; summon combat action proof still needed." }
            }
            return [pscustomobject]@{ Status = "FAIL"; Note = "No summon/spawn proof in cast result." }
        }
        default {
            if ($resultText -match "[1-9] hit|applied .* to [1-9] target|Runtime:") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Generic cast/hit/status proof found." }
            }
            return [pscustomobject]@{ Status = "REVIEW"; Note = "Runtime proof exists; concept-specific proof not classified." }
        }
    }
}

function Get-LatestServerLog {
    $logDir = Join-Path $env:APPDATA ("Hytale\UserData\Saves\" + $WorldName + "\logs")
    Get-ChildItem -LiteralPath $logDir -Filter "*_server.log" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Send-MotmCommand([string]$Text, [int]$DelayMilliseconds = $CommandDelayMilliseconds) {
    Write-Host "[audit-phase9-class] /$Text"
    & (Join-Path $PSScriptRoot "send-input.ps1") -Action Command -Text $Text -DelayMilliseconds 120 | Out-Host
    Start-Sleep -Milliseconds $DelayMilliseconds
}

function Read-NewLogLines([string]$Path, [long]$StartOffset) {
    $lines = New-Object System.Collections.Generic.List[string]
    $fs = [System.IO.File]::Open($Path, "Open", "Read", "ReadWrite")
    try {
        $fs.Position = [Math]::Min($StartOffset, $fs.Length)
        $reader = New-Object System.IO.StreamReader($fs)
        while (($line = $reader.ReadLine()) -ne $null) {
            $lines.Add($line)
        }
    } finally {
        if ($reader) { $reader.Dispose() }
        $fs.Dispose()
    }
    return $lines
}

function Read-LogLinesAfterMarker([string]$Path, [string]$Marker) {
    $all = Get-Content -LiteralPath $Path -ErrorAction Stop
    $markerIndex = -1
    for ($i = $all.Count - 1; $i -ge 0; $i--) {
        if ($all[$i] -match [regex]::Escape($Marker)) {
            $markerIndex = $i
            break
        }
    }
    if ($markerIndex -lt 0) {
        return $all
    }
    return @($all | Select-Object -Skip $markerIndex)
}

function Capture([string]$Name) {
    & (Join-Path $PSScriptRoot "capture-evidence.ps1") -Phase $phaseId -RunId $runId -Name $Name | Out-Host
}

function Get-AbilityEvidence($Lines, [string]$AbilityId) {
    $escaped = [regex]::Escape($AbilityId)
    $Lines | Where-Object {
        $_ -match "abilityId=$escaped" -or
        $_ -match "Cast .*!.*" -and $_ -match $escaped -or
        $_ -match "Queued ability cast result:.*abilityId=$escaped"
    }
}

function Get-AbilityResult($Lines, [string]$AbilityId) {
    $escaped = [regex]::Escape($AbilityId)
    $Lines |
        Where-Object { $_ -match "Queued ability cast result:.*abilityId=$escaped" } |
        Select-Object -Last 1
}

function Get-ResidualLines($Lines) {
    $Lines | Where-Object {
        $_ -match "No valid target|Reloading nonexistent|Unmapped NPC type|Exception|ERROR|NoClassDefFoundError|ClassNotFoundException"
    }
}

$status = "FAIL"
try {
    Add-Line("# Phase 9 $ClassId Flatlands Class Audit")
    Add-Line("")
    Add-Line("- Run: $runId")
    Add-Line("- World: $WorldName")
    Add-Line("- Style source: $stylePath")
    Add-Line("- Styles: " + (($allStyles | ForEach-Object { $_.id }) -join ", "))
    Add-Line("- Camera: " + ($(if ($UseThirdPerson -and -not $SkipThirdPerson) { "third-person requested with V" } else { "unchanged" })))
    Add-Line("- Facing aid: top compass/debug heading should remain visible in screenshots")
    Add-Line("- Mob hygiene: clear tracked test mobs before each ability, then assert tracked count from logs")
    Add-Line("")

    if (-not $SkipFlatlandsGate) {
        & (Join-Path $PSScriptRoot "ensure-flatlands.ps1") -VerifyOnly
    }

    if ($UseThirdPerson -and -not $SkipThirdPerson) {
        & (Join-Path $PSScriptRoot "send-input.ps1") -Action ThirdPerson -DelayMilliseconds 350 | Out-Host
    }

    $log = Get-LatestServerLog
    if (-not $log) {
        throw "No server log found for $WorldName."
    }
    $startOffset = $log.Length
    Add-Line("- Server log: $($log.FullName)")
    Add-Line("")

    Send-MotmCommand "motm dev freecast on"
    Send-MotmCommand "motm dev class set $ClassId"
    $marker = "phase9-$ClassId-$runId"
    Add-RunMarker $marker

    foreach ($style in $allStyles) {
        $styleId = [string]$style.id
        Add-Line("## $styleId")
        Add-Line("")
        if (-not $SkipFlatlandsGate) {
            & (Join-Path $PSScriptRoot "ensure-flatlands.ps1") -VerifyOnly
        }

        Send-MotmCommand "motm dev class set $ClassId"
        Send-MotmCommand "motm dev styles clear"
        Send-MotmCommand "motm style $styleId" 1250

        foreach ($ability in @($style.abilities)) {
            $abilityId = [string]$ability.id
            $scenario = Get-AbilityScenario $ability
            $abilityStartLog = Get-LatestServerLog
            $abilityStartOffset = if ($abilityStartLog) { $abilityStartLog.Length } else { 0 }

            Send-MotmCommand "motm dev test mobs clear" 500
            Send-MotmCommand "motm dev test mobs close" 1350
            Send-MotmCommand "motm dev test mobs count" 450

            $trigger = ([string]$ability.trigger).ToLowerInvariant()
            if ($trigger -eq "jump_land") {
                Send-MotmCommand "motm dev test ability $abilityId" 900
                & (Join-Path $PSScriptRoot "send-input.ps1") -Action Jump -DelayMilliseconds 1600 | Out-Host
                Start-Sleep -Milliseconds 900
            } else {
                Send-MotmCommand "motm dev test ability $abilityId" $PostCastMilliseconds
            }
            Capture "$styleId-$abilityId"

            $abilityLines = Read-NewLogLines $abilityStartLog.FullName $abilityStartOffset
            $resultLine = Get-AbilityResult $abilityLines $abilityId
            $spawnLine = $abilityLines |
                Where-Object { $_ -match "Style test mobs spawned: count=" } |
                Select-Object -Last 1
            $countLine = $abilityLines |
                Where-Object { $_ -match "Style test mobs tracked: count=" } |
                Select-Object -Last 1
            $stompLine = $abilityLines |
                Where-Object { $_ -match "Stomp landing resolved: targets=" } |
                Select-Object -Last 1
            if ($resultLine) {
                $mechanical = Get-MechanicalProof $scenario $abilityLines $resultLine $abilityId
                Add-Line("- ``$abilityId``")
                Add-Line("  - Scenario: $($scenario.Kind)")
                Add-Line("  - Setup: $($scenario.Setup)")
                Add-Line("  - Required proof: $($scenario.Proof)")
                Add-Line("  - Runtime: PASS")
                if ($spawnLine) { Add-Line("  - $spawnLine") }
                if ($countLine) { Add-Line("  - $countLine") }
                Add-Line("  - $resultLine")
                Add-Line("  - Mechanical: $($mechanical.Status) - $($mechanical.Note)")
                Add-Line("  - Visual: REVIEW - inspect screenshot against style palette and ability motion")
            } else {
                Add-Line("- ``$abilityId``")
                Add-Line("  - Scenario: $($scenario.Kind)")
                Add-Line("  - Setup: $($scenario.Setup)")
                Add-Line("  - Required proof: $($scenario.Proof)")
                Add-Line("  - Runtime: FAIL - cast result missing")
                Add-Line("  - Mechanical: FAIL - runtime proof missing")
                Add-Line("  - Visual: REVIEW - no screenshot can pass without runtime proof")
                if ($spawnLine) { Add-Line("  - $spawnLine") }
                if ($countLine) { Add-Line("  - $countLine") }
            }
            $residuals = Get-ResidualLines $abilityLines
            foreach ($residual in $residuals) {
                Add-Line("  - REVIEW: $residual")
            }
        }
        Add-Line("")
    }

    $log = Get-LatestServerLog
    $lines = Read-LogLinesAfterMarker $log.FullName $marker
    $lines | Set-Content -LiteralPath (Join-Path $outDir "server.log") -Encoding UTF8

    Add-Line("## Residual Scan")
    Add-Line("")
    $blocking = $lines | Where-Object { $_ -match "NoClassDefFoundError|ClassNotFoundException|Exception|ERROR" }
    if ($blocking.Count -eq 0) {
        Add-Line("- PASS: no blocking class/runtime errors in class audit slice.")
    } else {
        foreach ($line in $blocking) {
            Add-Line("- FAIL: $line")
        }
    }

    $missingCasts = $report | Where-Object { $_ -match "  - Runtime: FAIL" }
    $failedScenarios = $report | Where-Object { $_ -match "  - Mechanical: FAIL" }
    $conceptReviews = $report | Where-Object { $_ -match "  - Mechanical: REVIEW|  - Visual: REVIEW" }
    Add-Line("")
    if ($blocking.Count -eq 0 -and $missingCasts.Count -eq 0 -and ($failedScenarios.Count -eq 0 -or -not $RequireConceptProof) -and ($conceptReviews.Count -eq 0 -or -not $RequireConceptProof)) {
        if ($conceptReviews.Count -gt 0 -and -not $RequireConceptProof) {
            Add-Line("- REVIEW: concept proof is incomplete for $($conceptReviews.Count) row(s). Runtime smoke can pass, but this is not FULL PASS.")
        }
        Add-Line("PASS")
        $status = "PASS"
    } else {
        Add-Line("FAIL")
        throw "Phase 9 $ClassId audit failed: missing casts=$($missingCasts.Count), mechanical failures=$($failedScenarios.Count), concept reviews=$($conceptReviews.Count), blocking=$($blocking.Count)."
    }
} catch {
    Add-Line("")
    Add-Line("FAIL")
    Add-Line("")
    Add-Line("Error: $($_.Exception.Message)")
    throw
} finally {
    $reportPath = Join-Path $outDir "report.md"
    $report | Set-Content -LiteralPath $reportPath -Encoding UTF8
    Write-Host "[audit-phase9-class] $status report: $reportPath"
}
