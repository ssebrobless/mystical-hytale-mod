param(
    [string]$RunId = "",
    [string[]]$StyleSweepRunIds = @(),
    [string]$OutDir = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}
if ([string]::IsNullOrWhiteSpace($OutDir)) {
    $OutDir = Join-Path $repoRoot (Join-Path "audits\concept-verification-layers" $RunId)
}
New-Item -ItemType Directory -Path $OutDir -Force | Out-Null

function Read-JsonlObjects {
    param([string]$Path)

    $items = New-Object System.Collections.Generic.List[object]
    if (-not (Test-Path -LiteralPath $Path)) {
        return $items
    }
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        try {
            $items.Add(($line | ConvertFrom-Json -ErrorAction Stop))
        } catch {
            $items.Add([PSCustomObject]@{
                parseError = $_.Exception.Message
                rawLine = $line
            })
        }
    }
    return $items
}

function Get-LatestStyleSweepRunIds {
    $sweepRoot = Join-Path $repoRoot "audits\style-sweeps"
    if (-not (Test-Path -LiteralPath $sweepRoot)) {
        return @()
    }
    $dirs = @(Get-ChildItem -LiteralPath $sweepRoot -Directory |
        Sort-Object LastWriteTime -Descending)
    $selected = New-Object System.Collections.Generic.List[string]
    foreach ($dir in $dirs) {
        $summaryPath = Join-Path $dir.FullName "summary.json"
        if (-not (Test-Path -LiteralPath $summaryPath)) { continue }
        $summary = @(Get-Content -LiteralPath $summaryPath -Raw | ConvertFrom-Json)
        if ($summary.Count -eq 0) { continue }
        $selected.Add($dir.Name)
        $classes = @($summary | ForEach-Object { [string]$_.class } | Select-Object -Unique)
        if ($selected.Count -ge 2 -and @($classes | Where-Object { $_ -in @("terra", "hydro", "aero", "corruptus") }).Count -gt 0) {
            $covered = New-Object System.Collections.Generic.HashSet[string]
            foreach ($name in $selected) {
                $path = Join-Path $sweepRoot (Join-Path $name "summary.json")
                if (-not (Test-Path -LiteralPath $path)) { continue }
                foreach ($row in @(Get-Content -LiteralPath $path -Raw | ConvertFrom-Json)) {
                    [void]$covered.Add([string]$row.class)
                }
            }
            if (@("terra", "hydro", "aero", "corruptus") | Where-Object { -not $covered.Contains($_) }) {
                continue
            }
            return @($selected)
        }
    }
    return @($selected)
}

function Get-PrimitiveBucket($ability) {
    $cast = ([string]$ability.cast_type).ToLowerInvariant()
    $terrain = ([string]$ability.terrain_effect).ToLowerInvariant()
    $summon = ([string]$ability.summon_name).ToLowerInvariant()
    $effect = ([string]$ability.effect).ToLowerInvariant()

    if (-not [string]::IsNullOrWhiteSpace($summon) -or $cast -match "summon") { return "summon" }
    if ($cast -match "dash|leap|teleport|air_stall|dive|transformation|form") { return "movement/form" }
    if ($cast -match "projectile|wave|line|chain|volley") { return "projectile/aim" }
    if ($terrain -match "wall|pillar|sapling|flower|roots|gem|lava|mud|ice|rift|gate|pool|storm|field|trail|earth|sand|smoke") { return "temporary object/field" }
    if ($cast -match "ground|support|barrier|zone|strike") { return "ground/field" }
    if ($effect -match "stun|slow|root|burn|dot|vulnerability|blind|knockback|freeze|grounded") { return "combat/status" }
    return "self/support"
}

function Test-HarmfulAbility($ability) {
    $effect = ([string]$ability.effect).ToLowerInvariant()
    $target = ([string]$ability.target_type).ToLowerInvariant()
    $description = ([string]$ability.description).ToLowerInvariant()
    $cast = ([string]$ability.cast_type).ToLowerInvariant()
    return ($target -match "enemy|ground_target|line|cone|self_centered" -or $cast -match "zone|field|strike|projectile|wave|chain") -and
        ($effect -match "slow|root|stun|burn|dot|knockback|vulnerability|damage|freeze|grounded|blind" -or
            $description -match "damage|burn|poison|slow|root|stun|knock|explode|pull|drag|vulnerab")
}

function Test-NeedsAimProof($ability) {
    $cast = ([string]$ability.cast_type).ToLowerInvariant()
    $target = ([string]$ability.target_type).ToLowerInvariant()
    return $cast -match "projectile|ground_target|ground_strike|line|wave|chain|cone|gaze" -or
        $target -match "ground_target|enemy|line|cone"
}

function Test-NeedsMovementProof($ability) {
    $cast = ([string]$ability.cast_type).ToLowerInvariant()
    $trigger = ([string]$ability.trigger).ToLowerInvariant()
    $description = ([string]$ability.description).ToLowerInvariant()
    return $cast -match "dash|leap|teleport|air_stall|dive|transformation|form" -or
        $trigger -match "jump|sprint|move|land" -or
        $description -match "dash|jump|leap|move|sprint|swim|skate|burrow|tunnel|fly|hover|ride"
}

function Convert-Gate($status, $note) {
    [PSCustomObject]@{
        status = $status
        note = $note
    }
}

function Test-MechanicalSignal($ability, $abilityEnd) {
    if (-not $abilityEnd) {
        return Convert-Gate "FAIL" "missing ability_cast_end"
    }
    $bucket = Get-PrimitiveBucket $ability
    $summary = ([string]$abilityEnd.data.summary).ToLowerInvariant()
    $combatTargets = [int]($abilityEnd.data.combatTargets)
    $totalDamage = [double]($abilityEnd.data.totalDamage)
    $projectiles = [int]($abilityEnd.data.projectiles)
    $fieldActivated = [bool]($abilityEnd.data.fieldActivated)
    $terrainActivated = [bool]($abilityEnd.data.terrainActivated)
    $summonsSpawned = [int]($abilityEnd.data.summonsSpawned)
    $summonsBuffed = [int]($abilityEnd.data.summonsBuffed)
    $formApplied = [bool]($abilityEnd.data.formApplied)

    switch ($bucket) {
        "summon" {
            if ($summonsSpawned -gt 0 -or $summonsBuffed -gt 0 -or $summary -match "summon|minion|spawn") {
                return Convert-Gate "PASS" $summary
            }
            return Convert-Gate "REVIEW" "summon-like ability ended but summon count was 0"
        }
        "movement/form" {
            if ($formApplied -or $summary -match "dash|teleport|movement|moved|leap|dive|burrow|tunnel|skate|form|transformed|velocity|slide|hover") {
                return Convert-Gate "PASS" $summary
            }
            return Convert-Gate "REVIEW" "movement/form ability needs displacement or form evidence"
        }
        "projectile/aim" {
            if ($projectiles -gt 0 -or $combatTargets -gt 0 -or $totalDamage -gt 0.0 -or $summary -match "projectile|launched|chain|wave|volley|hit") {
                return Convert-Gate "PASS" $summary
            }
            return Convert-Gate "REVIEW" "projectile-like ability needs launch or hit evidence"
        }
        "temporary object/field" {
            if ($fieldActivated -or $terrainActivated -or $summary -match "field|terrain|block|selection|fluid|pool|wall|pillar|sapling|flower|root|trail|ring|storm|aura|gem") {
                return Convert-Gate "PASS" $summary
            }
            return Convert-Gate "REVIEW" "object/field ability needs terrain, field, or proxy evidence"
        }
        "ground/field" {
            if ($fieldActivated -or $terrainActivated -or $combatTargets -gt 0 -or $summary -match "field|terrain|ground|zone|strike|impact|pool|storm|heal|shield") {
                return Convert-Gate "PASS" $summary
            }
            return Convert-Gate "REVIEW" "ground/field ability needs impact, field, or target evidence"
        }
        default {
            if ($formApplied -or $fieldActivated -or $terrainActivated -or $combatTargets -gt 0 -or $totalDamage -gt 0.0 -or $summary -match "buff|shield|heal|status|effect|coating|aura|cleanse") {
                return Convert-Gate "PASS" $summary
            }
            return Convert-Gate "REVIEW" "generic runtime ended but concept-specific signal is weak"
        }
    }
}

function Test-VisualSignal($ability, $abilityEnd, $serverTruthEvents, $clientIntentEvents) {
    if (-not $abilityEnd) {
        return Convert-Gate "FAIL" "missing ability_cast_end"
    }
    $traceId = [string]$abilityEnd.traceId
    $summary = ([string]$abilityEnd.data.summary).ToLowerInvariant()
    $visualEvents = @($serverTruthEvents + $clientIntentEvents | Where-Object {
        -not $_.parseError -and (
            [string]$_.traceId -eq $traceId -or
            [string]$_.data.abilityId -eq [string]$ability.id -or
            [string]$_.data.reason -eq [string]$ability.id -or
            [string]$_.data.proofId -match [regex]::Escape([string]$ability.id)
        )
    })
    if ($visualEvents.Count -gt 0) {
        return Convert-Gate "PASS" "$($visualEvents.Count) visual/proxy/server-truth event(s)"
    }
    if ($summary -match "visual|coating|effect|terrain|block|fluid|field|proxy|projectile|aura|trail|ring|storm|smoke|spark|gem|pillar|wall|sapling|flower|root|lava|mud|ice") {
        return Convert-Gate "REVIEW" "summary contains visual terms but no dedicated visual/proxy event was linked"
    }
    return Convert-Gate "UNKNOWN" "no dedicated visual/proxy event in baseline evidence"
}

function Get-WorstStatus($statuses) {
    if ($statuses -contains "FAIL") { return "FAIL" }
    if ($statuses -contains "UNKNOWN") { return "UNKNOWN" }
    if ($statuses -contains "REVIEW") { return "REVIEW" }
    return "PASS"
}

function Count-Status($Rows, [string]$Property, [string]$Status) {
    return @($Rows | Where-Object { [string]$_.$Property -eq $Status }).Count
}

if ($StyleSweepRunIds.Count -eq 0) {
    $StyleSweepRunIds = Get-LatestStyleSweepRunIds
}
$StyleSweepRunIds = @($StyleSweepRunIds | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
if ($StyleSweepRunIds.Count -eq 0) {
    throw "No style sweep run ids supplied and none were discovered under audits/style-sweeps."
}

$sweepRows = New-Object System.Collections.Generic.List[object]
foreach ($sweepRunId in $StyleSweepRunIds) {
    $summaryPath = Join-Path $repoRoot (Join-Path "audits\style-sweeps" (Join-Path $sweepRunId "summary.json"))
    if (-not (Test-Path -LiteralPath $summaryPath)) {
        throw "Style sweep summary not found: $summaryPath"
    }
    $summaryRows = Get-Content -LiteralPath $summaryPath -Raw | ConvertFrom-Json
    foreach ($row in $summaryRows) {
        $sweepRows.Add($row)
    }
}

$stylesRoot = Join-Path $repoRoot "src\main\resources\data\styles"
$styleDocs = Get-ChildItem -LiteralPath $stylesRoot -Filter "*.json" |
    ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json }
$allStyles = @($styleDocs | ForEach-Object { $_.styles })

$results = New-Object System.Collections.Generic.List[object]
foreach ($style in $allStyles) {
    $classId = [string]$style.class_id
    $styleId = [string]$style.id
    $sweep = @($sweepRows | Where-Object { [string]$_.'class' -eq $classId -and [string]$_.style -eq $styleId } | Select-Object -First 1)
    $runIdForStyle = if ($sweep) { [string]$sweep.run_id } else { "" }
    $rawDir = if (-not [string]::IsNullOrWhiteSpace($runIdForStyle)) {
        Join-Path $repoRoot (Join-Path "audits\agent-observability" (Join-Path $runIdForStyle "raw\motm-observability"))
    } else {
        ""
    }
    if ([string]::IsNullOrWhiteSpace($rawDir)) {
        $causalityEvents = @()
        $serverTruthEvents = @()
        $clientIntentEvents = @()
    } else {
        $causalityEvents = Read-JsonlObjects (Join-Path $rawDir "causality.jsonl")
        $serverTruthEvents = Read-JsonlObjects (Join-Path $rawDir "server-truth.jsonl")
        $clientIntentEvents = Read-JsonlObjects (Join-Path $rawDir "client-intent.jsonl")
    }
    $abilityEnds = @($causalityEvents | Where-Object { -not $_.parseError -and [string]$_.type -eq "ability_cast_end" })

    foreach ($ability in @($style.abilities)) {
        $abilityId = [string]$ability.id
        $abilityEnd = @($abilityEnds | Where-Object { [string]$_.data.abilityId -eq $abilityId } | Select-Object -Last 1)
        $runtimeGate = if ($abilityEnd) { Convert-Gate "PASS" "ability_cast_end present" } else { Convert-Gate "FAIL" "missing ability_cast_end" }
        $mechanicalGate = Test-MechanicalSignal $ability $abilityEnd
        $visualGate = Test-VisualSignal $ability $abilityEnd $serverTruthEvents $clientIntentEvents
        $safetyGate = if (Test-HarmfulAbility $ability) {
            Convert-Gate "UNKNOWN" "needs allied-player/summon/caster-safe probe"
        } else {
            Convert-Gate "PASS" "not classified as hostile AoE/debuff"
        }
        $normalControlGate = Convert-Gate "UNKNOWN" "style sweep used /motm dev test ability; normal spellbook input still needs live or packet proof"
        $aimMovementGate = if (Test-NeedsMovementProof $ability) {
            Convert-Gate "REVIEW" "needs displacement/path proof for movement-sensitive behavior"
        } elseif (Test-NeedsAimProof $ability) {
            Convert-Gate "REVIEW" "needs target-position/crosshair trajectory proof"
        } else {
            Convert-Gate "PASS" "no aim or movement-specific condition detected"
        }
        $overall = Get-WorstStatus @(
            $runtimeGate.status,
            $mechanicalGate.status,
            $visualGate.status,
            $safetyGate.status,
            $normalControlGate.status,
            $aimMovementGate.status
        )

        $results.Add([PSCustomObject]@{
            class = $classId
            style = $styleId
            ability = $abilityId
            name = [string]$ability.name
            bucket = Get-PrimitiveBucket $ability
            run_id = $runIdForStyle
            runtime_cast = $runtimeGate.status
            mechanical_signal = $mechanicalGate.status
            visual_signal = $visualGate.status
            safety_signal = $safetyGate.status
            normal_control_signal = $normalControlGate.status
            aim_movement_signal = $aimMovementGate.status
            overall = $overall
            notes = @(
                "mechanical=$($mechanicalGate.note)",
                "visual=$($visualGate.note)",
                "safety=$($safetyGate.note)",
                "control=$($normalControlGate.note)",
                "aimMovement=$($aimMovementGate.note)"
            ) -join " | "
        })
    }
}

$csvPath = Join-Path $OutDir "concept-verification-layers.csv"
$jsonPath = Join-Path $OutDir "concept-verification-layers.json"
$reportPath = Join-Path $OutDir "report.md"
$results | Sort-Object class, style, ability | Export-Csv -LiteralPath $csvPath -NoTypeInformation -Encoding UTF8
$results | Sort-Object class, style, ability | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

$report = New-Object System.Collections.Generic.List[string]
$report.Add("# Concept Verification Layers")
$report.Add("")
$report.Add("- RunId: $RunId")
$report.Add("- Style sweeps: $($StyleSweepRunIds -join ', ')")
$report.Add("- Abilities inspected: $($results.Count)")
$report.Add("")
$report.Add("## Status Shape")
$report.Add("")
$report.Add('```')
$report.Add("runtime cast        -> ability reached ability_cast_end")
$report.Add("mechanical signal   -> damage/heal/status/field/projectile/summon/move summary matches concept shape")
$report.Add("visual signal       -> linked client-intent/server-truth visual, proxy, block, fluid, or effect evidence")
$report.Add("safety signal       -> caster/allies/summons are proven not harmed or debuffed")
$report.Add("normal control      -> intended spellbook/hotbar input, not only dev command")
$report.Add("aim/movement signal -> crosshair/trajectory or movement path proved where relevant")
$report.Add('```')
$report.Add("")
$report.Add("## Summary")
$report.Add("")
$report.Add("| Layer | PASS | REVIEW | UNKNOWN | FAIL |")
$report.Add("|---|---:|---:|---:|---:|")
foreach ($layer in @("runtime_cast", "mechanical_signal", "visual_signal", "safety_signal", "normal_control_signal", "aim_movement_signal", "overall")) {
    $report.Add("| $layer | $(Count-Status $results $layer 'PASS') | $(Count-Status $results $layer 'REVIEW') | $(Count-Status $results $layer 'UNKNOWN') | $(Count-Status $results $layer 'FAIL') |")
}
$report.Add("")
$report.Add("## Class Summary")
$report.Add("")
$report.Add("| Class | Abilities | Overall PASS | REVIEW | UNKNOWN | FAIL |")
$report.Add("|---|---:|---:|---:|---:|---:|")
foreach ($classGroup in ($results | Group-Object class | Sort-Object Name)) {
    $report.Add("| $($classGroup.Name) | $($classGroup.Count) | $(Count-Status $classGroup.Group 'overall' 'PASS') | $(Count-Status $classGroup.Group 'overall' 'REVIEW') | $(Count-Status $classGroup.Group 'overall' 'UNKNOWN') | $(Count-Status $classGroup.Group 'overall' 'FAIL') |")
}
$report.Add("")
$report.Add("## Next Probe Queue")
$report.Add("")
$probeRows = @($results | Where-Object { $_.overall -ne "PASS" } | Sort-Object class, style, ability)
if ($probeRows.Count -eq 0) {
    $report.Add("None. All concept layers are PASS.")
} else {
    $report.Add("| Class | Style | Ability | Bucket | Needed |")
    $report.Add("|---|---|---|---|---|")
    foreach ($row in $probeRows) {
        $needed = New-Object System.Collections.Generic.List[string]
        foreach ($layer in @("runtime_cast", "mechanical_signal", "visual_signal", "safety_signal", "normal_control_signal", "aim_movement_signal")) {
            if ($row.$layer -ne "PASS") { $needed.Add($layer + "=" + $row.$layer) }
        }
        $report.Add("| $($row.class) | $($row.style) | $($row.ability) | $($row.bucket) | $($needed -join '; ') |")
    }
}
$report.Add("")
$report.Add("## Full Matrix")
$report.Add("")
$report.Add("| Class | Style | Ability | Runtime | Mechanical | Visual | Safety | Normal Control | Aim/Movement | Overall |")
$report.Add("|---|---|---|---|---|---|---|---|---|---|")
foreach ($row in ($results | Sort-Object class, style, ability)) {
    $report.Add("| $($row.class) | $($row.style) | $($row.ability) | $($row.runtime_cast) | $($row.mechanical_signal) | $($row.visual_signal) | $($row.safety_signal) | $($row.normal_control_signal) | $($row.aim_movement_signal) | $($row.overall) |")
}
$report | Set-Content -LiteralPath $reportPath -Encoding UTF8

$failCount = @($results | Where-Object { $_.runtime_cast -eq "FAIL" -or $_.mechanical_signal -eq "FAIL" -or $_.visual_signal -eq "FAIL" }).Count
Write-Host "Concept verification layer audit wrote: $reportPath"
if ($failCount -gt 0) {
    Write-Host "Concept verification layer audit: FAIL ($failCount hard failure(s))"
    exit 1
}
Write-Host "Concept verification layer audit: PASS with review/unknown items captured"
