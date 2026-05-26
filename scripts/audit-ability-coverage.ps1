param(
    [string]$RunId = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}

$outDir = Join-Path $repoRoot (Join-Path "audits\ability-coverage" $RunId)
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

$expectedClasses = @("terra", "hydro", "aero", "corruptus")
$expectedStylesPerClass = 10
$expectedAbilitiesPerStyle = 3

function Get-PrimitiveBucket($ability) {
    $cast = [string]$ability.cast_type
    $terrain = [string]$ability.terrain_effect
    $summon = [string]$ability.summon_name
    $effect = [string]$ability.effect

    if (-not [string]::IsNullOrWhiteSpace($summon) -or $cast -in @("summon", "summon_buff")) { return "summon/proxy" }
    if ($terrain -match "wall|pillar|sapling|flower|roots|gem|lava|mud|ice|rift|gate|pool|storm|field|trail|earth|sand|smoke") { return "temporary terrain/field" }
    if ($cast -match "projectile|wave|chain") { return "projectile" }
    if ($cast -match "dash|leap|teleport|air_stall|transformation") { return "movement/form" }
    if ($cast -match "self_buff|support|barrier") { return "self/support buff" }
    if ($effect -match "stun|slow|root|burn|dot|vulnerability|blind|knockback|freeze|grounded") { return "status/combat" }
    return "generic runtime"
}

function Get-RiskNotes($classId, $styleId, $ability) {
    $notes = New-Object System.Collections.Generic.List[string]
    $id = [string]$ability.id
    $cast = [string]$ability.cast_type
    $target = [string]$ability.target_type
    $terrain = [string]$ability.terrain_effect
    $effect = [string]$ability.effect

    if ($cast -match "projectile" -and [string]::IsNullOrWhiteSpace([string]$ability.projectile_speed)) {
        $notes.Add("projectile-speed-default")
    }
    if ($cast -match "ground|barrier|support" -and [string]::IsNullOrWhiteSpace($terrain)) {
        $notes.Add("field-without-terrain-effect")
    }
    if ($target -match "enemy|ground_target|line|cone|self_centered" -and $effect -match "slow|root|stun|burn|dot|knockback|vulnerability") {
        $notes.Add("needs-friendly-safety-proof")
    }
    if ($cast -match "dash|leap|teleport|air_stall") {
        $notes.Add("needs-movement-control-proof")
    }
    if ($cast -match "summon" -or -not [string]::IsNullOrWhiteSpace([string]$ability.summon_name)) {
        $notes.Add("needs-summon-ai-cleanup-proof")
    }
    if ($terrain -match "lava|mud|water|ice|wall|pillar|sapling|roots|flower|gem|pool|trail") {
        $notes.Add("needs-temp-object-cleanup-proof")
    }
    if ($classId -eq "terra" -and $styleId -eq "stone" -and $id -in @("rubble_rouser", "pillar_strike", "rockslide")) {
        $notes.Add("live-fix-under-review")
    }
    if ($classId -eq "terra" -and $styleId -eq "arbor" -and $id -in @("rooted", "vines", "sapling")) {
        $notes.Add("live-fix-under-review")
    }
    if ($classId -eq "terra" -and $styleId -eq "quake" -and $id -in @("stomp", "aftershock", "sinkhole")) {
        $notes.Add("live-ground-vfx-under-review")
    }

    if ($notes.Count -eq 0) { return "none" }
    return ($notes -join "; ")
}

$styles = @()
foreach ($path in Get-ChildItem -LiteralPath (Join-Path $repoRoot "src\main\resources\data\styles") -Filter "*.json") {
    $doc = Get-Content -LiteralPath $path.FullName -Raw | ConvertFrom-Json
    $styles += @($doc.styles)
}

$rows = New-Object System.Collections.Generic.List[object]
$failures = New-Object System.Collections.Generic.List[string]

foreach ($classId in $expectedClasses) {
    $classStyles = @($styles | Where-Object { $_.class_id -eq $classId })
    if ($classStyles.Count -ne $expectedStylesPerClass) {
        $failures.Add("$classId expected $expectedStylesPerClass styles, found $($classStyles.Count)")
    }
    foreach ($style in $classStyles) {
        $abilities = @($style.abilities)
        if ($abilities.Count -ne $expectedAbilitiesPerStyle) {
            $failures.Add("$classId/$($style.id) expected $expectedAbilitiesPerStyle abilities, found $($abilities.Count)")
        }
        for ($index = 0; $index -lt $abilities.Count; $index++) {
            $ability = $abilities[$index]
            if ($ability.resource_cost -ne 0) {
                $failures.Add("$classId/$($style.id)/$($ability.id) resource_cost is $($ability.resource_cost)")
            }
            $rows.Add([PSCustomObject]@{
                class = $classId
                style = $style.id
                style_name = $style.name
                slot = $index + 1
                ability = $ability.id
                name = $ability.name
                description = $ability.description
                cast_type = $ability.cast_type
                target_type = $ability.target_type
                cooldown_seconds = $ability.cooldown_seconds
                duration_seconds = $ability.duration_seconds
                radius = $ability.radius
                range = $ability.range
                effect = $ability.effect
                terrain_effect = $ability.terrain_effect
                visual_overlay = $ability.visual_overlay
                trigger = $ability.trigger
                summon_name = $ability.summon_name
                primitive_bucket = Get-PrimitiveBucket $ability
                risk_notes = Get-RiskNotes $classId $style.id $ability
            })
        }
    }
}

$csvPath = Join-Path $outDir "ability-coverage.csv"
$jsonPath = Join-Path $outDir "ability-coverage.json"
$reportPath = Join-Path $outDir "report.md"

$rows | Sort-Object class, style, slot | Export-Csv -Path $csvPath -NoTypeInformation -Encoding UTF8
$rows | Sort-Object class, style, slot | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

$classSummary = $rows |
    Group-Object class |
    ForEach-Object {
        [PSCustomObject]@{
            Class = $_.Name
            Styles = (@($_.Group | Select-Object -ExpandProperty style -Unique)).Count
            Abilities = $_.Count
        }
    }

$bucketSummary = $rows |
    Group-Object primitive_bucket |
    Sort-Object Name |
    ForEach-Object { "| $($_.Name) | $($_.Count) |" }

$status = if ($failures.Count -eq 0 -and $rows.Count -eq 120) { "PASS" } else { "FAIL" }
$report = New-Object System.Collections.Generic.List[string]
$report.Add("# Ability Coverage Audit")
$report.Add("")
$report.Add("- RunId: $RunId")
$report.Add("- Status: $status")
$report.Add("- Styles: $((@($styles)).Count)")
$report.Add("- Abilities: $($rows.Count)")
$report.Add("- CSV: ability-coverage.csv")
$report.Add("- JSON: ability-coverage.json")
$report.Add("")
$report.Add("## Class Summary")
$report.Add("")
$report.Add("| Class | Styles | Abilities |")
$report.Add("|---|---:|---:|")
foreach ($entry in $classSummary) {
    $report.Add("| $($entry.Class) | $($entry.Styles) | $($entry.Abilities) |")
}
$report.Add("")
$report.Add("## Primitive Buckets")
$report.Add("")
$report.Add("| Bucket | Abilities |")
$report.Add("|---|---:|")
foreach ($line in $bucketSummary) {
    $report.Add($line)
}
$report.Add("")
$report.Add("## Failures")
$report.Add("")
if ($failures.Count -eq 0) {
    $report.Add("None.")
} else {
    foreach ($failure in $failures) {
        $report.Add("- $failure")
    }
}

$report.Add("")
$report.Add("## Full Ability Matrix")
$report.Add("")
$report.Add("| Class | Style | Slot | Ability | Concept | Cast/Target | Visual/Primitive | Proof Risks |")
$report.Add("|---|---|---:|---|---|---|---|---|")
foreach ($row in ($rows | Sort-Object class, style, slot)) {
    $concept = ([string]$row.description) -replace "\|", "/"
    $visualParts = @()
    if (-not [string]::IsNullOrWhiteSpace([string]$row.terrain_effect)) { $visualParts += "terrain=$($row.terrain_effect)" }
    if (-not [string]::IsNullOrWhiteSpace([string]$row.visual_overlay)) { $visualParts += "overlay=$($row.visual_overlay)" }
    if (-not [string]::IsNullOrWhiteSpace([string]$row.summon_name)) { $visualParts += "summon=$($row.summon_name)" }
    $visual = ($visualParts -join "; ")
    if ([string]::IsNullOrWhiteSpace($visual)) { $visual = $row.primitive_bucket }
    $cast = "$($row.cast_type)/$($row.target_type)"
    $report.Add("| $($row.class) | $($row.style) | $($row.slot) | $($row.ability) | $concept | $cast | $visual | $($row.risk_notes) |")
}

$report | Set-Content -LiteralPath $reportPath -Encoding UTF8

Write-Host "Ability coverage audit: $status"
Write-Host "Report: $reportPath"
if ($status -ne "PASS") {
    exit 1
}
