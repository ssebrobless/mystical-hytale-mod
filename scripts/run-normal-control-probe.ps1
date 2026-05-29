param(
    [string]$WorldName = "MOTM Creative Test",
    [ValidateSet("terra", "hydro", "aero", "corruptus")]
    [string]$ClassId = "terra",
    [string]$StyleId = "quake",
    [string]$RunId = "",
    [ValidateSet("PrimarySecondaryUse", "AbilityKeys")]
    [string]$ControlMode = "PrimarySecondaryUse",
    [int]$SpellbookHotbarSlot = 1,
    [int]$DelayMilliseconds = 1500,
    [switch]$SkipCollect
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = "normal-control-" + (Get-Date -Format "yyyy-MM-ddTHH-mm-ss")
}
$outDir = Join-Path $repoRoot (Join-Path "audits\agent-observability" $RunId)
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$report = New-Object System.Collections.Generic.List[string]

function Add-Line([string]$Line) {
    $script:report.Add($Line)
}

function Resolve-PowerShellExecutable {
    if ($env:OS -eq "Windows_NT") {
        $powershell = Get-Command powershell -ErrorAction SilentlyContinue
        if ($powershell) { return $powershell.Source }
    }

    $pwsh = Get-Command pwsh -ErrorAction SilentlyContinue
    if ($pwsh) { return $pwsh.Source }
    $powershell = Get-Command powershell -ErrorAction SilentlyContinue
    if ($powershell) { return $powershell.Source }
    throw "Could not locate powershell or pwsh."
}

function Stop-ProcessTree {
    param([int]$RootProcessId)

    if ($RootProcessId -le 0) {
        return
    }

    $allProcesses = @(Get-CimInstance Win32_Process -ErrorAction SilentlyContinue)
    $processIds = New-Object System.Collections.Generic.HashSet[int]
    [void]$processIds.Add($RootProcessId)
    $changed = $true
    while ($changed) {
        $changed = $false
        foreach ($process in $allProcesses) {
            $processId = [int]$process.ProcessId
            $parentId = [int]$process.ParentProcessId
            if ($processIds.Contains($parentId) -and -not $processIds.Contains($processId)) {
                [void]$processIds.Add($processId)
                $changed = $true
            }
        }
    }

    $processIds |
        Sort-Object -Descending |
        Where-Object { $_ -ne $PID } |
        ForEach-Object { Stop-Process -Id $_ -Force -ErrorAction SilentlyContinue }
}

function Invoke-HarnessChildProcess {
    param(
        [string[]]$Arguments,
        [string]$LogPath,
        [int]$TimeoutMilliseconds,
        [string]$Description
    )

    New-Item -ItemType Directory -Path (Split-Path -Parent $LogPath) -Force | Out-Null
    $stdoutPath = "$LogPath.stdout.tmp"
    $stderrPath = "$LogPath.stderr.tmp"
    Remove-Item -LiteralPath $stdoutPath, $stderrPath -Force -ErrorAction SilentlyContinue

    $child = $null
    try {
        $child = Start-Process -FilePath $script:PowerShellExe `
            -ArgumentList $Arguments `
            -NoNewWindow `
            -PassThru `
            -RedirectStandardOutput $stdoutPath `
            -RedirectStandardError $stderrPath
        $hardTimeout = [Math]::Max($TimeoutMilliseconds + 10000, 15000)
        if (-not $child.WaitForExit($hardTimeout)) {
            Stop-ProcessTree -RootProcessId $child.Id
            $message = "Timed out after ${hardTimeout}ms: $Description"
            $message | Set-Content -LiteralPath $LogPath -Encoding UTF8
            throw $message
        }

        $output = New-Object System.Collections.Generic.List[string]
        if (Test-Path -LiteralPath $stdoutPath) {
            Get-Content -LiteralPath $stdoutPath -ErrorAction SilentlyContinue | ForEach-Object { $output.Add($_) }
        }
        if (Test-Path -LiteralPath $stderrPath) {
            Get-Content -LiteralPath $stderrPath -ErrorAction SilentlyContinue | ForEach-Object { $output.Add($_) }
        }
        $output | Set-Content -LiteralPath $LogPath -Encoding UTF8
        $output | ForEach-Object { Write-Host $_ }
        if ($child.ExitCode -ne 0) {
            throw "$Description exited with code $($child.ExitCode). See command log: $LogPath"
        }
    } finally {
        if ($child -and -not $child.HasExited) {
            Stop-ProcessTree -RootProcessId $child.Id
        }
        Remove-Item -LiteralPath $stdoutPath, $stderrPath -Force -ErrorAction SilentlyContinue
    }
}

function Invoke-ObservedCommand {
    param(
        [string]$Command,
        [int]$TimeoutMilliseconds = 8000,
        [int]$Delay = 450
    )

    $traceId = "normal-control-" + ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())
    Add-Line("- Command: `/motm $($Command -replace '^\s*motm\s+', '')` traceId=$traceId")
    $args = @(
        "-NoProfile", "-ExecutionPolicy", "Bypass",
        "-File", (Join-Path $PSScriptRoot "send-dev-command.ps1"),
        "-Command", $Command,
        "-WorldName", $WorldName,
        "-TimeoutMilliseconds", $TimeoutMilliseconds,
        "-RunDir", $outDir,
        "-TraceId", $traceId,
        "-ScenarioId", "normal-control-$ClassId-$StyleId"
    )
    Invoke-HarnessChildProcess `
        -Arguments $args `
        -LogPath (Join-Path $outDir ("command-" + ($traceId -replace '[^A-Za-z0-9_.-]', '-') + ".log")) `
        -TimeoutMilliseconds $TimeoutMilliseconds `
        -Description "Command /motm $Command"
    if ($Delay -gt 0) {
        Start-Sleep -Milliseconds $Delay
    }
}

function Invoke-InputAction {
    param(
        [string]$Action,
        [string]$Label
    )

    Add-Line("- Input: $Label via scripts/send-input.ps1 -Action $Action")
    $args = @(
        "-NoProfile", "-ExecutionPolicy", "Bypass",
        "-File", (Join-Path $PSScriptRoot "send-input.ps1"),
        "-Action", $Action,
        "-DelayMilliseconds", 250
    )
    Invoke-HarnessChildProcess `
        -Arguments $args `
        -LogPath (Join-Path $outDir ("input-" + $Label + ".log")) `
        -TimeoutMilliseconds 5000 `
        -Description "Input action $Action"
    Start-Sleep -Milliseconds $DelayMilliseconds
}

function Read-JsonlObjects {
    param([string]$Path)

    $items = New-Object System.Collections.Generic.List[object]
    if (-not (Test-Path -LiteralPath $Path)) { return $items }
    foreach ($line in Get-Content -LiteralPath $Path) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        try {
            $items.Add(($line | ConvertFrom-Json -ErrorAction Stop))
        } catch {
            $items.Add([PSCustomObject]@{ parseError = $_.Exception.Message; rawLine = $line })
        }
    }
    return $items
}

function Get-StyleDefinition {
    $path = Join-Path $repoRoot "src\main\resources\data\styles\${ClassId}_styles.json"
    if (-not (Test-Path -LiteralPath $path)) {
        throw "Style file not found: $path"
    }
    $doc = Get-Content -LiteralPath $path -Raw | ConvertFrom-Json
    $style = $doc.styles | Where-Object { [string]$_.id -eq $StyleId } | Select-Object -First 1
    if (-not $style) {
        throw "Style not found: $ClassId/$StyleId"
    }
    return $style
}

function Get-SlotActions {
    if ($ControlMode -eq "AbilityKeys") {
        return @(
            [PSCustomObject]@{ slot = 1; action = "Ability1"; label = "slot-1-ability-key" },
            [PSCustomObject]@{ slot = 2; action = "Ability2"; label = "slot-2-ability-key" },
            [PSCustomObject]@{ slot = 3; action = "Ability3"; label = "slot-3-ability-key" }
        )
    }
    return @(
        [PSCustomObject]@{ slot = 1; action = "LeftClick"; label = "slot-1-primary-left-click" },
        [PSCustomObject]@{ slot = 2; action = "RightClick"; label = "slot-2-secondary-right-click" },
        [PSCustomObject]@{ slot = 3; action = "Ability3"; label = "slot-3-use-key" }
    )
}

function New-InputStep {
    param(
        [string]$Action,
        [string]$Label
    )

    [PSCustomObject]@{
        action = $Action
        label = ($Label -replace '[^A-Za-z0-9_.-]', '-')
    }
}

function Get-AbilityExercisePlan {
    param($Slot)

    $abilityId = ([string]$Slot.ability).ToLowerInvariant()
    $castType = ([string]$Slot.cast).ToLowerInvariant()
    $steps = New-Object System.Collections.Generic.List[object]

    # Stomp is armed by the cast and proved by a follow-up jump/landing.
    if ($abilityId -eq "stomp" -and $ControlMode -eq "PrimarySecondaryUse") {
        $steps.Add((New-InputStep "Stomp" "$($Slot.label)-arm-and-land"))
        return $steps
    }

    $steps.Add((New-InputStep $Slot.action $Slot.label))

    $movementAbilities = @(
        "rockslide", "burrow", "tunnel", "dust_devil", "jet_burst", "afterburner",
        "mach_punch", "dispersion", "leap_frog", "skate", "shadow_step"
    )
    $jumpAbilities = @("leap", "divebomb", "hang_time")
    $trailAbilities = @("frolick", "sandstorm", "smoke_form", "waverider", "river_rapids")
    $weaponFollowUps = @("alloy_enhancement", "rubble_rouser", "razor_wind", "oil_spill")
    $aimNudges = @(
        "sapling", "nightshade", "magma_sling", "vitrification", "rubble_rouser",
        "air_slash", "gale_cutter", "air_shot", "bullet_storm", "smite", "fireball",
        "hellfire", "mind_shatter", "scald", "frozen_needles", "anchor_haul"
    )

    if ($aimNudges -contains $abilityId -or $castType -match "projectile|line|gaze|cone|chain") {
        $steps.Add((New-InputStep "FaceRight" "$($Slot.label)-aim-nudge"))
    }
    if ($movementAbilities -contains $abilityId -or $castType -match "dash|teleport|transformation") {
        $steps.Add((New-InputStep "Forward" "$($Slot.label)-movement-followup"))
    }
    if ($jumpAbilities -contains $abilityId -or $castType -match "leap|dive|air") {
        $steps.Add((New-InputStep "ForwardJump" "$($Slot.label)-jump-followup"))
    }
    if ($trailAbilities -contains $abilityId) {
        $steps.Add((New-InputStep "Forward" "$($Slot.label)-trail-followup"))
        $steps.Add((New-InputStep "StrafeRight" "$($Slot.label)-trail-strafe"))
    }
    if ($weaponFollowUps -contains $abilityId) {
        $steps.Add((New-InputStep "LeftClick" "$($Slot.label)-weapon-followup-1"))
        $steps.Add((New-InputStep "LeftClick" "$($Slot.label)-weapon-followup-2"))
    }

    return $steps
}

function Assert-NormalControlEvidence {
    param($Expected)

    $rawDir = Join-Path $outDir "raw\motm-observability"
    $causalityEvents = Read-JsonlObjects (Join-Path $rawDir "causality.jsonl")
    $slotMarkers = @($causalityEvents | Where-Object {
        -not $_.parseError -and [string]$_.type -eq "marker" -and [string]$_.data.label -match "^normal-control-slot-"
    } | Sort-Object epochMillis)
    $abilityEnds = @($causalityEvents | Where-Object {
        -not $_.parseError -and [string]$_.type -eq "ability_cast_end"
    } | Sort-Object epochMillis)

    $failures = New-Object System.Collections.Generic.List[string]
    foreach ($expectedSlot in $Expected) {
        $marker = $slotMarkers | Where-Object { [string]$_.data.label -eq "normal-control-slot-$($expectedSlot.slot)-before" } | Select-Object -First 1
        if (-not $marker) {
            $failures.Add("slot $($expectedSlot.slot) missing before marker")
            continue
        }
        $nextMarker = $slotMarkers | Where-Object { [Int64]$_.epochMillis -gt [Int64]$marker.epochMillis } | Select-Object -First 1
        $windowEnd = if ($nextMarker) { [Int64]$nextMarker.epochMillis } else { [Int64]$marker.epochMillis + 12000L }
        $match = $abilityEnds | Where-Object {
            [Int64]$_.epochMillis -ge [Int64]$marker.epochMillis -and
            [Int64]$_.epochMillis -le $windowEnd -and
            [string]$_.data.abilityId -eq [string]$expectedSlot.ability
        } | Select-Object -First 1
        if (-not $match) {
            $failures.Add("slot $($expectedSlot.slot) expected $($expectedSlot.ability) but no matching ability_cast_end appeared after input")
        }
    }
    return $failures
}

$script:PowerShellExe = Resolve-PowerShellExecutable
$style = Get-StyleDefinition
$abilities = @($style.abilities | ForEach-Object { [string]$_.id })
if ($abilities.Count -lt 3) {
    throw "$ClassId/$StyleId has fewer than 3 abilities."
}
$slotActions = Get-SlotActions
$expected = @($slotActions | ForEach-Object {
    [PSCustomObject]@{
        slot = $_.slot
        action = $_.action
        label = $_.label
        ability = $abilities[$_.slot - 1]
        cast = [string]$style.abilities[$_.slot - 1].cast_type
    }
})

$status = "FAIL"
try {
    Add-Line("# Normal Control Probe")
    Add-Line("")
    Add-Line("- RunId: $RunId")
    Add-Line("- World: $WorldName")
    Add-Line("- Class/style: $ClassId/$StyleId")
    Add-Line("- Control mode: $ControlMode")
    Add-Line("- Expected slots: " + (($expected | ForEach-Object { "$($_.slot)=$($_.ability)" }) -join ", "))
    Add-Line("")

    Invoke-ObservedCommand "motm dev observe start $RunId normal-control-$ClassId-$StyleId" -TimeoutMilliseconds 9000
    Invoke-ObservedCommand "motm dev freecast on"
    Invoke-ObservedCommand "motm dev class set $ClassId" -Delay 900
    Invoke-ObservedCommand "motm dev styles clear" -Delay 700
    Invoke-ObservedCommand "motm style $StyleId" -Delay 1200
    Invoke-ObservedCommand "motm dev test mobs clear" -Delay 600
    Invoke-ObservedCommand "motm dev test mobs close" -TimeoutMilliseconds 9000 -Delay 1200
    Invoke-ObservedCommand "motm dev observe snapshot normal-control-ready"

    Invoke-HarnessChildProcess `
        -Arguments @(
            "-NoProfile", "-ExecutionPolicy", "Bypass",
            "-File", (Join-Path $PSScriptRoot "send-input.ps1"),
            "-Action", "Key",
            "-Keys", ([string]$SpellbookHotbarSlot),
            "-DelayMilliseconds", 450
        ) `
        -LogPath (Join-Path $outDir "input-select-spellbook.log") `
        -TimeoutMilliseconds 5000 `
        -Description "Select spellbook hotbar slot"

    foreach ($slot in $expected) {
        Invoke-ObservedCommand "motm dev observe marker normal-control-slot-$($slot.slot)-before" -Delay 150
        $exercisePlan = @(Get-AbilityExercisePlan $slot)
        Add-Line("- Exercise plan for slot $($slot.slot) / $($slot.ability): " + (($exercisePlan | ForEach-Object { $_.action }) -join " -> "))
        foreach ($step in $exercisePlan) {
            Invoke-InputAction $step.action $step.label
        }
        Invoke-ObservedCommand "motm dev observe snapshot normal-control-slot-$($slot.slot)-after" -Delay 250
    }

    Invoke-ObservedCommand "motm dev observe marker normal-control-end"
    Invoke-ObservedCommand "motm dev observe snapshot normal-control-final"
    Invoke-ObservedCommand "motm dev observe stop normal-control-complete"

    if (-not $SkipCollect) {
        Invoke-HarnessChildProcess `
            -Arguments @(
                "-NoProfile", "-ExecutionPolicy", "Bypass",
                "-File", (Join-Path $PSScriptRoot "collect-observability-evidence.ps1"),
                "-WorldName", $WorldName,
                "-RunId", $RunId,
                "-OutDir", $outDir
            ) `
            -LogPath (Join-Path $outDir "collect-observability-evidence.log") `
            -TimeoutMilliseconds 30000 `
            -Description "Evidence collection"
        $failures = Assert-NormalControlEvidence $expected
        if ($failures.Count -gt 0) {
            Add-Line("")
            Add-Line("## Failures")
            foreach ($failure in $failures) {
                Add-Line("- $failure")
            }
            throw "Normal control probe failed: $($failures -join '; ')"
        }
    }

    Add-Line("")
    Add-Line("## Summary")
    Add-Line("")
    Add-Line("PASS")
    $status = "PASS"
} catch {
    Add-Line("")
    Add-Line("## Summary")
    Add-Line("")
    Add-Line("FAIL")
    Add-Line("")
    Add-Line("Error: $($_.Exception.Message)")
    throw
} finally {
    $reportPath = Join-Path $outDir "normal-control-report.md"
    $report | Set-Content -LiteralPath $reportPath -Encoding UTF8
    Write-Host "[run-normal-control-probe] $status report: $reportPath"
}
