param(
    [ValidateSet("terra", "hydro", "aero", "corruptus")]
    [string]$ClassId = "terra",
    [ValidateSet(
        "terra-mining-baseline",
        "terra-mining-passive",
        "terra-knockback",
        "hydro-barrier",
        "hydro-spell-vamp",
        "aero-movement-baseline",
        "aero-movement-passive",
        "corruptus-stacks"
    )]
    [string]$TestId = "terra-mining-passive",
    [string]$WorldName = "MOTM Creative Test",
    [string]$RunId = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}

$phase = "manual-class-passive-proof"
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $phase $RunId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

function Send-MotmCommand([string]$Text, [int]$DelayMilliseconds = 650) {
    & (Join-Path $PSScriptRoot "send-dev-command.ps1") -Command $Text -WorldName $WorldName -TimeoutMilliseconds ([Math]::Max(12000, $DelayMilliseconds + 4000)) -RunDir $outDir -ScenarioId $TestId | Out-Host
    Start-Sleep -Milliseconds $DelayMilliseconds
}

function Add-Line([System.Collections.Generic.List[string]]$Lines, [string]$Text) {
    $Lines.Add($Text) | Out-Null
}

$instructions = New-Object System.Collections.Generic.List[string]
Add-Line $instructions "# Class Passive Proof Setup"
Add-Line $instructions ""
Add-Line $instructions "- Run: $RunId"
Add-Line $instructions "- Test: $TestId"
Add-Line $instructions "- World: $WorldName"
Add-Line $instructions ""

& (Join-Path $PSScriptRoot "check-world-entry-state.ps1") -RunId $RunId | Out-Host
Send-MotmCommand "motm dev daylight" 450
Send-MotmCommand "motm dev relocate lane" 1600
Send-MotmCommand "motm dev test reset" 1300

switch ($TestId) {
    "terra-mining-baseline" {
        Send-MotmCommand "motm dev freecast on" 700
        Send-MotmCommand "motm dev class clear" 700
        Send-MotmCommand "motm dev kit terra" 1200
        Send-MotmCommand "motm dev inventory clean terra-kit" 1200
        Send-MotmCommand "motm dev mode adventure" 1000
        Add-Line $instructions "## Manual Action"
        Add-Line $instructions "Mine one stone/solid block with the pickaxe. This is the no-class baseline."
        Add-Line $instructions ""
        Add-Line $instructions "## Expected Evidence"
        Add-Line $instructions "- No `[MOTM] Terra mining affinity applied` line should appear for this hit."
    }
    "terra-mining-passive" {
        Send-MotmCommand "motm dev freecast on" 700
        Send-MotmCommand "motm dev class set terra" 700
        Send-MotmCommand "motm dev styles clear" 700
        Send-MotmCommand "motm dev kit terra" 1200
        Send-MotmCommand "motm dev inventory clean terra-kit" 1200
        Send-MotmCommand "motm dev mode adventure" 1000
        Add-Line $instructions "## Manual Action"
        Add-Line $instructions "Mine one matching stone/solid block with the same pickaxe."
        Add-Line $instructions ""
        Add-Line $instructions "## Expected Evidence"
        Add-Line $instructions "- Server log contains `[MOTM] Terra mining affinity applied` with `multiplier=1.500`."
    }
    "terra-knockback" {
        Send-MotmCommand "motm dev freecast off" 700
        Send-MotmCommand "motm dev class set terra" 700
        Send-MotmCommand "motm dev styles clear" 700
        Send-MotmCommand "motm dev passive knockback" 500
        Send-MotmCommand "motm dev mode adventure" 1000
        Send-MotmCommand "motm dev test mobs clear" 900
        Send-MotmCommand "motm dev test mobs close" 1200
        Add-Line $instructions "## Manual Action"
        Add-Line $instructions "Let a nearby native enemy/source hit you or trigger a controlled hit source with knockback."
        Add-Line $instructions ""
        Add-Line $instructions "## Expected Evidence"
        Add-Line $instructions "- If the source carries native knockback, server log contains `[MOTM] Incoming knockback passive applied` with `multiplier=0.800`."
        Add-Line $instructions "- If it says `had no knockback component`, that source cannot prove the passive; use a different knockback source."
    }
    "hydro-barrier" {
        Send-MotmCommand "motm dev freecast off" 700
        Send-MotmCommand "motm dev class set hydro" 700
        Send-MotmCommand "motm dev styles clear" 700
        Send-MotmCommand "motm dev mode adventure" 1000
        Send-MotmCommand "motm dev passive status" 500
        Send-MotmCommand "motm dev passive incoming-damage 20" 500
        Send-MotmCommand "motm dev passive status" 500
        Add-Line $instructions "## Automated Action"
        Add-Line $instructions "The script simulated 20 incoming damage through the passive manager."
        Add-Line $instructions ""
        Add-Line $instructions "## Expected Evidence"
        Add-Line $instructions "- `Dev passive incoming-damage` shows damage reduced by Aqua Barrier first."
        Add-Line $instructions "- `Dev passive status` reports Hydro barrier HP/cooldown state."
    }
    "hydro-spell-vamp" {
        Send-MotmCommand "motm dev freecast off" 700
        Send-MotmCommand "motm dev class set hydro" 700
        Send-MotmCommand "motm dev styles clear" 700
        Send-MotmCommand "motm style icicle" 1000
        Send-MotmCommand "motm dev freecast off" 700
        Send-MotmCommand "motm dev mode adventure" 1000
        Send-MotmCommand "motm dev passive health 50" 500
        Send-MotmCommand "motm dev passive status" 500
        Send-MotmCommand "motm dev passive outgoing-damage 100 ability" 500
        Send-MotmCommand "motm dev passive status" 500
        Add-Line $instructions "## Automated Action"
        Add-Line $instructions "The script lowered HP and simulated 100 ability damage dealt through the passive manager."
        Add-Line $instructions ""
        Add-Line $instructions "## Expected Evidence"
        Add-Line $instructions "- `Dev passive outgoing-damage` shows health increasing from the pre-damage value."
    }
    "aero-movement-baseline" {
        Send-MotmCommand "motm dev freecast on" 700
        Send-MotmCommand "motm dev class clear" 700
        Send-MotmCommand "motm dev mode creative" 1000
        Send-MotmCommand "motm dev effects" 500
        Send-MotmCommand "motm dev position" 500
        Add-Line $instructions "## Manual Action"
        Add-Line $instructions "Hold the chosen movement input for exactly 3 seconds; Codex should capture `/motm dev position` after."
        Add-Line $instructions ""
        Add-Line $instructions "## Expected Evidence"
        Add-Line $instructions "- This run is the no-class displacement baseline."
    }
    "aero-movement-passive" {
        Send-MotmCommand "motm dev freecast on" 700
        Send-MotmCommand "motm dev class set aero" 700
        Send-MotmCommand "motm dev styles clear" 700
        Send-MotmCommand "motm dev mode creative" 1000
        Send-MotmCommand "motm dev effects" 500
        Send-MotmCommand "motm dev position" 500
        Add-Line $instructions "## Manual Action"
        Add-Line $instructions "Repeat the exact same 3 second movement input from the same lane."
        Add-Line $instructions ""
        Add-Line $instructions "## Expected Evidence"
        Add-Line $instructions "- Displacement should be about 25% higher than the baseline and `/motm dev effects` should show no slow effect."
    }
    "corruptus-stacks" {
        Send-MotmCommand "motm dev freecast on" 700
        Send-MotmCommand "motm dev class set corruptus" 700
        Send-MotmCommand "motm dev styles clear" 700
        Send-MotmCommand "motm style flame" 1000
        Send-MotmCommand "motm dev freecast off" 700
        Send-MotmCommand "motm dev mode adventure" 1000
        Send-MotmCommand "motm dev passive corruptus-stack" 500
        Send-MotmCommand "motm dev passive corruptus-stack" 500
        Send-MotmCommand "motm dev passive corruptus-stack" 500
        Send-MotmCommand "motm dev passive health 10" 500
        Send-MotmCommand "motm dev passive incoming-damage 999" 500
        Send-MotmCommand "motm dev passive status" 500
        Add-Line $instructions "## Automated Action"
        Add-Line $instructions "The script simulated three kill stacks, lowered HP, then simulated lethal incoming damage."
        Add-Line $instructions ""
        Add-Line $instructions "## Expected Evidence"
        Add-Line $instructions "- Soul Harvest stack state advances to 5/5."
        Add-Line $instructions "- Lethal incoming damage is canceled, health returns to the resurrection threshold, stacks reset, and lockout starts."
    }
}

Send-MotmCommand "motm dev effects" 500
Send-MotmCommand "motm dev position" 500
if ($TestId -notmatch "baseline") {
    Send-MotmCommand "motm class" 500
}
& (Join-Path $PSScriptRoot "verify-third-person.ps1") -Phase $phase -RunId $RunId -Name $TestId -TryToggle | Out-Host
& (Join-Path $PSScriptRoot "capture-evidence.ps1") -Phase $phase -RunId $RunId -Name "$TestId-ready" -WindowOnly | Out-Host

Add-Line $instructions ""
Add-Line $instructions "PASS"
$reportPath = Join-Path $outDir "report.md"
$instructions | Set-Content -LiteralPath $reportPath -Encoding UTF8
Write-Host "[setup-class-passive-proof] PASS report: $reportPath"
Write-Host ""
$instructions | ForEach-Object { Write-Host $_ }
