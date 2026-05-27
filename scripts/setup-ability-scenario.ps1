param(
    [ValidateSet("terra", "hydro", "aero", "corruptus")]
    [string]$ClassId,
    [string]$StyleId,
    [string]$AbilityId,
    [switch]$DryRun,
    [int]$DelayMilliseconds = 650
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$stylePath = Join-Path $repoRoot "src\main\resources\data\styles\${ClassId}_styles.json"
if (-not (Test-Path -LiteralPath $stylePath)) {
    throw "Style file not found: $stylePath"
}

$styleFile = Get-Content -LiteralPath $stylePath -Raw | ConvertFrom-Json
$style = @($styleFile.styles | Where-Object { $_.id -eq $StyleId }) | Select-Object -First 1
if (-not $style) { throw "Style not found: $ClassId/$StyleId" }
$ability = @($style.abilities | Where-Object { $_.id -eq $AbilityId }) | Select-Object -First 1
if (-not $ability) { throw "Ability not found: $ClassId/$StyleId/$AbilityId" }

function Get-ScenarioKind($Ability) {
    $castType = ([string]$Ability.cast_type).ToLowerInvariant()
    $targetType = ([string]$Ability.target_type).ToLowerInvariant()
    $trigger = ([string]$Ability.trigger).ToLowerInvariant()
    $effect = ([string]$Ability.effect).ToLowerInvariant()
    $description = ([string]$Ability.description).ToLowerInvariant()

    if ($trigger -eq "jump_land") { return "jump_land" }
    if ($castType -in @("dash", "dash_buff", "dash_strike", "leap", "dive_strike", "teleport", "air_stall")) { return "movement" }
    if ($castType -in @("cone", "gaze")) { return "facing_cone" }
    if ($castType -in @("ground_zone", "support_zone")) { return "ground_zone" }
    if ($castType -in @("ground_target", "ground_strike", "barrier")) { return "ground_target" }
    if ($castType -like "projectile*" -or $castType -in @("line_control", "wave_line", "chain")) { return "projectile_line" }
    if ($castType -in @("summon", "summon_buff")) { return "summon" }
    if ($castType -in @("cleanse") -or $effect -match "cleanse|purify" -or $description -match "cleanse|purge|purify") { return "cleanse" }
    if ($targetType -eq "self" -or $castType -match "self|transformation|form|buff") { return "self_buff" }
    if ($effect -match "heal" -or $description -match "heal") { return "support_heal" }
    return "single_target"
}

function Invoke-Step([string]$Description, [scriptblock]$Action) {
    Write-Host "[setup-ability-scenario] $Description"
    if (-not $DryRun) { & $Action }
}

function Send-MotmCommand([string]$Text, [int]$Delay = $DelayMilliseconds) {
    $command = $Text -replace '^\s*/?motm\s+', ''
    $timeout = [Math]::Max(3500, $Delay + 2500)
    & (Join-Path $PSScriptRoot "send-dev-command.ps1") -Command $command -TimeoutMilliseconds $timeout | Out-Host
    if ($Delay -gt 0) {
        Start-Sleep -Milliseconds $Delay
    }
}

$scenario = Get-ScenarioKind $ability
Write-Host "[setup-ability-scenario] $ClassId/$StyleId/$AbilityId scenario=$scenario"
Write-Host "[setup-ability-scenario] description=$($ability.description)"

Invoke-Step "focus flatlands-facing camera context" {
    & (Join-Path $PSScriptRoot "send-input.ps1") -Action FaceRight -MouseDelta 0 -DelayMilliseconds 100 | Out-Host
}

Invoke-Step "set class/style and freecast" {
    Send-MotmCommand "motm dev freecast on"
    Send-MotmCommand "motm dev class set $ClassId"
    Send-MotmCommand "motm dev styles clear"
    Send-MotmCommand "motm style $StyleId" 1100
}

Invoke-Step "reset tracked test mobs" {
    Send-MotmCommand "motm dev test mobs clear" 450
}

switch ($scenario) {
    "self_buff" {
        Invoke-Step "switch third-person for caster-body visual proof" {
            & (Join-Path $PSScriptRoot "send-input.ps1") -Action ThirdPerson -DelayMilliseconds 350 | Out-Host
        }
    }
    "support_heal" {
        Invoke-Step "spawn close target and keep third-person for HUD/body proof" {
            Send-MotmCommand "motm dev test mobs close" 1200
            & (Join-Path $PSScriptRoot "send-input.ps1") -Action ThirdPerson -DelayMilliseconds 350 | Out-Host
        }
    }
    "movement" {
        Invoke-Step "spawn close target, step back to create a movement lane, then face target" {
            Send-MotmCommand "motm dev test mobs close" 1200
            & (Join-Path $PSScriptRoot "send-input.ps1") -Action Back -HoldMilliseconds 700 -DelayMilliseconds 250 | Out-Host
        }
    }
    "jump_land" {
        Invoke-Step "spawn grounded target close enough for landing AoE" {
            Send-MotmCommand "motm dev test mobs close" 1200
        }
    }
    "summon" {
        Invoke-Step "leave arena clear enough to see summon, then use a ground target lane" {
            Send-MotmCommand "motm dev test mobs close" 1200
            & (Join-Path $PSScriptRoot "send-input.ps1") -Action Back -HoldMilliseconds 450 -DelayMilliseconds 250 | Out-Host
        }
    }
    default {
        Invoke-Step "spawn grounded and floating targets for target-side proof" {
            Send-MotmCommand "motm dev test mobs close" 1200
        }
    }
}

Invoke-Step "log tracked mob count" {
    Send-MotmCommand "motm dev test mobs count" 400
}

Write-Host "[setup-ability-scenario] Ready: run /motm dev test ability $AbilityId, then assert with scripts/assert-ability-proof.ps1."
