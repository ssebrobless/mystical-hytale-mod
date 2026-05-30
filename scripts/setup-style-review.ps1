param(
    [string]$WorldName = "MOTM Creative Test",
    [Parameter(Mandatory = $true)]
    [ValidateSet("terra", "hydro", "aero", "corruptus")]
    [string]$ClassId,
    [string]$StyleId = "",
    [ValidateSet("none", "close", "stationary", "cluster", "line", "surround")]
    [string]$TargetMode = "none",
    [switch]$Creative,
    [switch]$Survival,
    [switch]$RelocateFlatlands,
    [string]$RunId = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($RunId)) {
    $stylePart = if ([string]::IsNullOrWhiteSpace($StyleId)) { "no-style" } else { $StyleId }
    $RunId = "style-review-$ClassId-$stylePart-" + (Get-Date -Format "yyyyMMdd-HHmmss")
}

$runDir = Join-Path (Join-Path (Resolve-Path ".").Path "audits/manual") $RunId
New-Item -ItemType Directory -Force -Path $runDir | Out-Null

function Invoke-MotmReviewCommand {
    param([string]$Command, [int]$TimeoutMilliseconds = 9000)
    $sendCommandScript = Join-Path $PSScriptRoot "send-dev-command.ps1"
    & $sendCommandScript `
        -WorldName $WorldName `
        -RunDir $runDir `
        -ScenarioId "manual-style-review" `
        -TimeoutMilliseconds $TimeoutMilliseconds `
        -Command $Command
}

$commands = New-Object System.Collections.Generic.List[string]
[void]$commands.Add("motm dev observe start $RunId manual-style-review")
[void]$commands.Add("motm dev effects clear")
[void]$commands.Add("motm dev test mobs clear")
[void]$commands.Add("motm dev test reset")
[void]$commands.Add("motm dev perks clear")
[void]$commands.Add("motm dev styles clear")
[void]$commands.Add("motm dev class set $ClassId")
if ($Creative) {
    [void]$commands.Add("motm dev mode creative")
}
if ($Survival) {
    [void]$commands.Add("motm dev mode survival")
}
[void]$commands.Add("motm dev daylight")
if ($RelocateFlatlands) {
    [void]$commands.Add("motm dev relocate flatlands")
}
[void]$commands.Add("motm dev kit $ClassId")
[void]$commands.Add("motm dev inventory clean $ClassId-kit")
if (-not [string]::IsNullOrWhiteSpace($StyleId)) {
    [void]$commands.Add("motm style $StyleId")
}

# Manual visual review should not show training dummies unless the reviewer
# explicitly asks for targets. This keeps public ability visuals separate from
# harness-only test mobs.
if ($TargetMode -ne "none") {
    [void]$commands.Add("motm dev test mobs clear")
    [void]$commands.Add("motm dev test mobs $TargetMode")
    [void]$commands.Add("motm dev test mobs count")
}

[void]$commands.Add("motm dev effects")
[void]$commands.Add("motm dev passive status")
[void]$commands.Add("motm dev observe snapshot ready")
[void]$commands.Add("motm dev observe stop setup-complete")

foreach ($command in $commands) {
    Invoke-MotmReviewCommand $command
    Start-Sleep -Milliseconds 650
}

Write-Host "[setup-style-review] ready: class=$ClassId style=$StyleId targets=$TargetMode runDir=$runDir"
