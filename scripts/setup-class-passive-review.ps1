param(
    [ValidateSet("terra", "hydro", "aero", "corruptus")]
    [string]$ClassId = "terra",
    [string]$WorldName = "MOTM Creative Test",
    [ValidateSet("creative", "adventure")]
    [string]$ReviewMode = "creative",
    [switch]$SkipRelocate,
    [switch]$SkipThirdPerson,
    [switch]$SkipEntryCheck,
    [string]$RunId = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}

$phase = "manual-class-passive-review"
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $phase $RunId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

function Send-MotmCommand([string]$Text, [int]$DelayMilliseconds = 650) {
    $devCommand = Join-Path $PSScriptRoot "send-dev-command.ps1"
    & $devCommand -Command $Text -WorldName $WorldName -TimeoutMilliseconds ([Math]::Max(10000, $DelayMilliseconds + 4000)) -RunDir $outDir -ScenarioId "class-passive-$ClassId" | Out-Host
    Start-Sleep -Milliseconds $DelayMilliseconds
}

function Add-ReportLine([System.Collections.Generic.List[string]]$Report, [string]$Line) {
    $Report.Add($Line) | Out-Null
}

function Get-ClassPlan([string]$Id) {
    switch ($Id) {
        "terra" {
            return [pscustomobject]@{
                mode = "adventure"
                mobMode = "close"
                styleHint = "metal or quake"
                commands = @(
                    "motm dev kit terra",
                    "motm dev inventory clean terra-kit",
                    "motm dev test mobs close"
                )
                checklist = @(
                    "Immovable: take or simulate knockback and confirm it feels reduced, not amplified.",
                    "Miner's Affinity: use the granted pickaxe and compare mining feel against a non-Terra baseline later if needed.",
                    "Low-health regen: only test after environment is safe; confirm healing below 30 percent HP.",
                    "Cave vision: defer until an underground/cave lane exists."
                )
            }
        }
        "hydro" {
            return [pscustomobject]@{
                mode = "adventure"
                mobMode = "close"
                styleHint = "icicle for damage, any shield style for overlay ordering"
                commands = @(
                    "motm dev test mobs close"
                )
                checklist = @(
                    "Tidal Flow: damage a target with a Hydro ability and confirm small self-heal evidence.",
                    "Aqua Barrier: take damage and confirm the whole-body bubble shield is the first Hydro defensive layer depleted.",
                    "Swim speed and oxygen: defer to a water-lane setup unless a pool is already present."
                )
            }
        }
        "aero" {
            return [pscustomobject]@{
                mode = "creative"
                mobMode = "clear"
                styleHint = "wind_blade first, then vertical-movement styles"
                commands = @()
                checklist = @(
                    "Skybound speed: walk, sprint, and strafe; confirm the passive is fast without sideways slowdown.",
                    "Energy bonus: check HUD/status evidence for increased native Hytale energy.",
                    "Vertical movement: defer overlap checks to Aero style tests so Skybound does not duplicate jump/dive/hover logic."
                )
            }
        }
        "corruptus" {
            return [pscustomobject]@{
                mode = "adventure"
                mobMode = "cluster"
                styleHint = "flame for kill-stack setup"
                commands = @(
                    "motm dev test mobs cluster"
                )
                checklist = @(
                    "Dark Resurrection stacks: kill three hostile/test targets and confirm stack count reaches 3.",
                    "Resurrection: take lethal damage only after stacks are verified; confirm revive to half HP.",
                    "Lockout: after resurrection, confirm no new passive stacks are gained for the cooldown window."
                )
            }
        }
    }
}

$classPlan = Get-ClassPlan $ClassId
$modeToUse = if ($PSBoundParameters.ContainsKey("ReviewMode")) { $ReviewMode } else { $classPlan.mode }

$report = New-Object System.Collections.Generic.List[string]
Add-ReportLine $report "# Class Passive Review Prep"
Add-ReportLine $report ""
Add-ReportLine $report "- Run: $RunId"
Add-ReportLine $report "- Class: $ClassId"
Add-ReportLine $report "- World: $WorldName"
Add-ReportLine $report "- Mode: $modeToUse"
Add-ReportLine $report "- Output: $outDir"
Add-ReportLine $report ""

try {
    if (-not $SkipEntryCheck) {
        Add-ReportLine $report "## Entry State"
        & (Join-Path $PSScriptRoot "check-world-entry-state.ps1") -RunId $RunId | Out-Host
        Add-ReportLine $report "- PASS: entry-state screenshot did not look like death menu, pause menu, or void."
        Add-ReportLine $report ""
    }

    Write-Host "[setup-class-passive-review] Preparing $ClassId passive review."
    Send-MotmCommand "motm dev daylight" 450
    if (-not $SkipRelocate) {
        Send-MotmCommand "motm dev relocate lane" 1600
    }
    Send-MotmCommand "motm dev test reset" 1300
    Send-MotmCommand "motm dev freecast on" 700
    Send-MotmCommand "motm dev class set $ClassId"
    Send-MotmCommand "motm dev styles clear"
    Send-MotmCommand "motm dev mode $modeToUse" 1000
    Send-MotmCommand "motm dev test mobs clear" 700

    foreach ($command in @($classPlan.commands)) {
        Send-MotmCommand $command 1200
    }

    Send-MotmCommand "motm class" 500
    Send-MotmCommand "motm dev effects" 500
    Send-MotmCommand "motm dev test status" 500
    Send-MotmCommand "motm dev test mobs count" 450
    Send-MotmCommand "motm dev position" 450

    if (-not $SkipThirdPerson) {
        & (Join-Path $PSScriptRoot "verify-third-person.ps1") -Phase $phase -RunId $RunId -Name "$ClassId-passive" -TryToggle | Tee-Object -FilePath (Join-Path $outDir "third-person.txt") | Out-Host
    }

    & (Join-Path $PSScriptRoot "capture-evidence.ps1") -Phase $phase -RunId $RunId -Name "$ClassId-passive-ready" -WindowOnly | Out-Host
    & (Join-Path $PSScriptRoot "collect-observability-evidence.ps1") -WorldName $WorldName -RunId $RunId -Phase $phase -OutDir $outDir | Out-Host

    Add-ReportLine $report "## Checklist"
    foreach ($item in @($classPlan.checklist)) {
        Add-ReportLine $report "- [ ] $item"
    }
    Add-ReportLine $report ""
    Add-ReportLine $report "## Next Style Hint"
    Add-ReportLine $report "- Start ability review with: $($classPlan.styleHint)"
    Add-ReportLine $report ""
    Add-ReportLine $report "PASS"
    Write-Host "[setup-class-passive-review] Ready for $ClassId passive review."
} catch {
    Add-ReportLine $report ""
    Add-ReportLine $report "FAIL"
    Add-ReportLine $report ""
    Add-ReportLine $report "Error: $($_.Exception.Message)"
    throw
} finally {
    $reportPath = Join-Path $outDir "report.md"
    $report | Set-Content -LiteralPath $reportPath -Encoding UTF8
    Write-Host "[setup-class-passive-review] report: $reportPath"
}

Write-Host ""
Write-Host "[setup-class-passive-review] Manual test notes for ${ClassId}:"
foreach ($item in @($classPlan.checklist)) {
    Write-Host "  - $item"
}
