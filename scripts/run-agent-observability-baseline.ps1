param(
    [string]$WorldName = "Main",
    [string]$RunId = "",
    [string]$ScenarioId = "baseline",
    [string]$DataDir = "",
    [string]$JavaHome = "",
    [switch]$SkipBuild,
    [switch]$SkipScreenshot,
    [int]$CleanupDelayMilliseconds = 5500,
    [string]$StyleId = "",
    [string[]]$Abilities = @(),
    [string]$MobMode = "stationary",
    [string[]]$Proofs = @("coating-metal")
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

function Test-IsMacOS {
    return ($global:IsMacOS -eq $true)
}

function Resolve-PowerShellExecutable {
    try {
        $current = (Get-Process -Id $PID).Path
        if (-not [string]::IsNullOrWhiteSpace($current) -and (Test-Path -LiteralPath $current)) {
            return $current
        }
    } catch {
    }

    $pwsh = Get-Command pwsh -ErrorAction SilentlyContinue
    if ($pwsh) { return $pwsh.Source }

    $powershell = Get-Command powershell -ErrorAction SilentlyContinue
    if ($powershell) { return $powershell.Source }

    throw "Could not locate pwsh or powershell for child script execution."
}

function Resolve-JavaHome {
    param([string]$RequestedJavaHome)

    if (-not [string]::IsNullOrWhiteSpace($RequestedJavaHome)) {
        return $RequestedJavaHome
    }

    $candidates = New-Object System.Collections.Generic.List[string]
    if (Test-IsMacOS) {
        $candidates.Add("/opt/homebrew/opt/openjdk@25")
    }
    if (-not [string]::IsNullOrWhiteSpace($env:JAVA_HOME)) {
        $candidates.Add($env:JAVA_HOME)
    }
    $candidates.Add((Join-Path $repoRoot ".tools/jdk-25"))

    foreach ($candidate in $candidates) {
        if (-not [string]::IsNullOrWhiteSpace($candidate) -and (Test-Path -LiteralPath $candidate)) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
    }
    return ""
}

function Resolve-GradleExecutable {
    $wrapperName = if ($env:OS -eq "Windows_NT") { "gradlew.bat" } else { "gradlew" }
    $wrapper = Join-Path $repoRoot $wrapperName
    if (Test-Path -LiteralPath $wrapper) {
        return $wrapper
    }

    $command = Get-Command gradle -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    $gradleName = if ($env:OS -eq "Windows_NT") { "gradle.bat" } else { "gradle" }
    $toolsDir = Join-Path $repoRoot ".tools"
    if (Test-Path -LiteralPath $toolsDir) {
        $candidate = Get-ChildItem -LiteralPath $toolsDir -Directory -Filter "gradle-*" -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending |
            ForEach-Object { Join-Path $_.FullName (Join-Path "bin" $gradleName) } |
            Where-Object { Test-Path -LiteralPath $_ } |
            Select-Object -First 1
        if ($candidate) {
            return $candidate
        }
    }

    throw "Could not locate the Gradle wrapper or system Gradle. Run scripts/build-install.ps1 to bootstrap the project."
}

if ((Test-IsMacOS) -and [string]::IsNullOrWhiteSpace($env:APPDATA)) {
    $env:APPDATA = Join-Path $HOME "Library/Application Support"
}
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}
$Proofs = @($Proofs | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
$Abilities = @($Abilities | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
$JavaHome = Resolve-JavaHome $JavaHome
if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
    $env:JAVA_HOME = $JavaHome
}

$script:PowerShellExe = Resolve-PowerShellExecutable
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path "agent-observability" $RunId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$report = New-Object System.Collections.Generic.List[string]

function Add-Line([string]$Line) {
    $script:report.Add($Line)
}

function Invoke-ObservedCommand {
    param(
        [string]$Command,
        [int]$TimeoutMilliseconds = 7000,
        [int]$DelayMilliseconds = 450
    )

    $traceId = "shell-" + ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())
    $displayCommand = $Command -replace '^\s*motm\s+', ''
    Add-Line("- Command: `/motm $displayCommand` traceId=$traceId")
    $sendArgs = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", (Join-Path $PSScriptRoot "send-dev-command.ps1"),
        "-Command", $Command,
        "-WorldName", $WorldName,
        "-TimeoutMilliseconds", $TimeoutMilliseconds,
        "-RunDir", $outDir,
        "-TraceId", $traceId,
        "-ScenarioId", $ScenarioId
    )
    if (-not [string]::IsNullOrWhiteSpace($DataDir)) {
        $sendArgs += @("-DataDir", $DataDir)
    }
    & $script:PowerShellExe @sendArgs 2>&1 |
        Tee-Object -FilePath (Join-Path $outDir ("command-" + ($traceId -replace '[^A-Za-z0-9_.-]', '-') + ".log"))
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed: /motm $Command"
    }
    if ($DelayMilliseconds -gt 0) {
        Start-Sleep -Milliseconds $DelayMilliseconds
    }
}

function Read-JsonlObjects {
    param([string]$Path)

    $objects = New-Object System.Collections.Generic.List[object]
    if (-not (Test-Path -LiteralPath $Path)) {
        return $objects
    }
    foreach ($line in (Get-Content -LiteralPath $Path -ErrorAction Stop)) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        try {
            $objects.Add(($line | ConvertFrom-Json -ErrorAction Stop))
        } catch {
            $objects.Add([PSCustomObject]@{
                parseError = $_.Exception.Message
                rawLine = $line
            })
        }
    }
    return $objects
}

function Assert-RunEvidence {
    $motmRawDir = Join-Path $outDir "raw/motm-observability"
    $controlPath = Join-Path $motmRawDir "control.jsonl"
    $causalityPath = Join-Path $motmRawDir "causality.jsonl"
    $clientIntentPath = Join-Path $motmRawDir "client-intent.jsonl"
    $serverTruthPath = Join-Path $motmRawDir "server-truth.jsonl"
    $packetPath = Join-Path $motmRawDir "packets.jsonl"

    $controlEvents = Read-JsonlObjects $controlPath
    $commandFailures = @($controlEvents | Where-Object { $_.type -eq "dev_command_failed" })
    if ($commandFailures.Count -gt 0) {
        $sample = ($commandFailures | Select-Object -First 3 | ForEach-Object { $_.data.command + ": " + $_.data.error }) -join "; "
        throw "Observed dev command failures in run evidence: $sample"
    }

    $causalityEvents = Read-JsonlObjects $causalityPath
    if ($Abilities.Count -gt 0) {
        $abilityEnds = @($causalityEvents | Where-Object { $_.type -eq "ability_cast_end" })
        $endedAbilityIds = @($abilityEnds | ForEach-Object { [string]$_.data.abilityId })
        $missingAbilities = @($Abilities | Where-Object { $endedAbilityIds -notcontains $_ })
        if ($missingAbilities.Count -gt 0) {
            throw "Missing ability_cast_end events for expected ability/abilities: $($missingAbilities -join ', ')"
        }
    }

    $proofEnds = @($causalityEvents | Where-Object { $_.type -eq "proof_end" })
    $proofFailures = @($proofEnds | Where-Object {
        $result = [string]$_.data.result
        $result -match "\bFAIL\b|failed safely"
    })
    if ($proofFailures.Count -gt 0) {
        $sample = ($proofFailures | Select-Object -First 3 | ForEach-Object { $_.data.proofId + ": " + $_.data.result }) -join "; "
        throw "Observed proof failures in run evidence: $sample"
    }

    $endedProofIds = @($proofEnds | ForEach-Object { [string]$_.data.proofId })
    $missingProofs = @($Proofs | Where-Object { $endedProofIds -notcontains $_ })
    if ($missingProofs.Count -gt 0) {
        throw "Missing proof_end events for expected proof(s): $($missingProofs -join ', ')"
    }

    $clientIntentEvents = Read-JsonlObjects $clientIntentPath
    $serverTruthEvents = Read-JsonlObjects $serverTruthPath
    $untracedClientIntent = @($clientIntentEvents | Where-Object {
        -not $_.parseError -and [string]::IsNullOrWhiteSpace([string]$_.traceId)
    })
    if ($untracedClientIntent.Count -gt 0) {
        $sample = ($untracedClientIntent | Select-Object -First 5 | ForEach-Object { $_.type }) -join ", "
        throw "Observed untraced client-intent entries: $sample"
    }

    if ($Abilities.Count -gt 0) {
        foreach ($ability in $Abilities) {
            $abilityEndsForAbility = @($causalityEvents | Where-Object {
                $_.type -eq "ability_cast_end" -and [string]$_.data.abilityId -eq $ability
            })
            foreach ($abilityEnd in $abilityEndsForAbility) {
                $abilityTraceId = [string]$abilityEnd.traceId
                if ([string]::IsNullOrWhiteSpace($abilityTraceId)) {
                    continue
                }
                $matchingClientIntent = @($clientIntentEvents | Where-Object { [string]$_.traceId -eq $abilityTraceId })
                if ($matchingClientIntent.Count -eq 0) {
                    $corroboratingEffectId = switch ($ability) {
                        "obsidian_skin" { "MOTM_Proof_Coating_Obsidian" }
                        default { "" }
                    }
                    $abilityEpoch = 0L
                    try { $abilityEpoch = [Int64]$abilityEnd.epochMillis } catch { $abilityEpoch = 0L }
                    $hasCorroboratingClientIntent = $false
                    if (-not [string]::IsNullOrWhiteSpace($corroboratingEffectId) -and $abilityEpoch -gt 0) {
                        $windowEnd = $abilityEpoch + 12000L
                        $hasCorroboratingClientIntent = @($clientIntentEvents | Where-Object {
                            -not $_.parseError `
                                -and [string]$_.data.effectId -eq $corroboratingEffectId `
                                -and [Int64]$_.epochMillis -ge $abilityEpoch `
                                -and [Int64]$_.epochMillis -le $windowEnd
                        }).Count -gt 0
                    }
                    $hasCorroboratingServerTruth = $false
                    if ($ability -eq "iron_wall") {
                        $hasCorroboratingServerTruth = @($serverTruthEvents | Where-Object {
                            -not $_.parseError `
                                -and [string]$_.traceId -eq $abilityTraceId `
                                -and [string]$_.type -eq "temporary_selection_placed" `
                                -and [string]$_.data.reason -eq "iron_wall" `
                                -and [Int32]$_.data.blockCount -gt 0
                        }).Count -gt 0
                    }
                    if (-not $hasCorroboratingClientIntent -and -not $hasCorroboratingServerTruth) {
                        throw "Ability $ability trace $abilityTraceId has no matching client-intent evidence."
                    }
                }
            }
        }
    }

    $packetEvents = Read-JsonlObjects $packetPath
    $tracedHudPackets = @($packetEvents | Where-Object {
        -not $_.parseError `
            -and -not [string]::IsNullOrWhiteSpace([string]$_.traceId) `
            -and [string]$_.data.packetSimpleName -eq "CustomHud"
    })
    if ($tracedHudPackets.Count -eq 0) {
        throw "No traced outbound CustomHud packet evidence was captured."
    }
}

$status = "FAIL"
try {
    Add-Line("# Agent Observability Baseline")
    Add-Line("")
    Add-Line("- RunId: $RunId")
    Add-Line("- ScenarioId: $ScenarioId")
    Add-Line("- World: $WorldName")
    if (-not [string]::IsNullOrWhiteSpace($StyleId)) {
        Add-Line("- StyleId: $StyleId")
    }
    if ($Abilities.Count -gt 0) {
        Add-Line("- Abilities: $($Abilities -join ', ')")
    }
    Add-Line("- Proofs: $($Proofs -join ', ')")
    Add-Line("- APPDATA: $env:APPDATA")
    Add-Line("- JAVA_HOME: $env:JAVA_HOME")
    Add-Line("- PowerShell: $script:PowerShellExe")
    Add-Line("")

    if (-not $SkipBuild) {
        $ensureScript = Join-Path $PSScriptRoot "ensure-dev-environment.ps1"
        if (Test-Path -LiteralPath $ensureScript) {
            $ensureArgs = @()
            if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
                $ensureArgs += @("-JavaHome", $JavaHome)
            }
            Add-Line("## Environment Bootstrap")
            Add-Line("")
            Add-Line("- Command: $script:PowerShellExe -NoProfile -ExecutionPolicy Bypass -File $ensureScript $($ensureArgs -join ' ')")
            & $script:PowerShellExe -NoProfile -ExecutionPolicy Bypass -File $ensureScript @ensureArgs 2>&1 |
                Tee-Object -FilePath (Join-Path $outDir "ensure-dev-environment.log")
            if ($LASTEXITCODE -ne 0) {
                throw "Development environment bootstrap failed with exit code $LASTEXITCODE."
            }
            $JavaHome = Resolve-JavaHome $JavaHome
            if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
                $env:JAVA_HOME = $JavaHome
            }
            Add-Line("- PASS: development environment resolved.")
            Add-Line("")
        }

        $gradle = Resolve-GradleExecutable
        $gradleArgs = @()
        if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
            $gradleArgs += "-Dorg.gradle.java.installations.paths=$JavaHome"
        }
        $gradleArgs += "-Pmotm_build_channel=internal"
        $gradleArgs += "build"
        $gradleArgs += "installMod"
        Add-Line("## Build/Install")
        Add-Line("")
        Add-Line("- Command: $gradle $($gradleArgs -join ' ')")
        & $gradle @gradleArgs 2>&1 | Tee-Object -FilePath (Join-Path $outDir "gradle-build-install.log")
        if ($LASTEXITCODE -ne 0) {
            throw "Gradle build/install failed with exit code $LASTEXITCODE."
        }
        Add-Line("- PASS: internal build installed.")
        Add-Line("")
    }

    Add-Line("## Runtime Commands")
    Add-Line("")
    Invoke-ObservedCommand "motm dev observe start $RunId $ScenarioId" -TimeoutMilliseconds 8000
    Invoke-ObservedCommand "motm dev observe marker baseline-start"
    Invoke-ObservedCommand "motm dev observe snapshot initial"
    Invoke-ObservedCommand "motm dev position"
    Invoke-ObservedCommand "motm dev effects"
    Invoke-ObservedCommand "motm dev freecast on"
    Invoke-ObservedCommand "motm dev test mobs $MobMode" -TimeoutMilliseconds 9000 -DelayMilliseconds 900
    Invoke-ObservedCommand "motm dev test mobs count"
    Invoke-ObservedCommand "motm dev observe snapshot after-target-setup"

    if (-not [string]::IsNullOrWhiteSpace($StyleId)) {
        Invoke-ObservedCommand "motm dev observe marker style-$StyleId-before"
        Invoke-ObservedCommand "motm style $StyleId" -TimeoutMilliseconds 9000 -DelayMilliseconds 900
        Invoke-ObservedCommand "motm dev observe snapshot style-$StyleId-after"
    }

    foreach ($ability in $Abilities) {
        Invoke-ObservedCommand "motm dev observe marker ability-$ability-before"
        Invoke-ObservedCommand "motm dev test ability $ability" -TimeoutMilliseconds 9000 -DelayMilliseconds 2200
        if ($ability -eq "stomp") {
            Invoke-ObservedCommand "motm dev test stomp-land" -TimeoutMilliseconds 9000 -DelayMilliseconds 900
        }
        Invoke-ObservedCommand "motm dev observe snapshot ability-$ability-after"
    }

    foreach ($proof in $Proofs) {
        Invoke-ObservedCommand "motm dev observe marker proof-$proof-before"
        Invoke-ObservedCommand "motm dev proof $proof" -TimeoutMilliseconds 9000 -DelayMilliseconds 1800
        Invoke-ObservedCommand "motm dev observe snapshot proof-$proof-after"
    }

    if ($CleanupDelayMilliseconds -gt 0) {
        Add-Line("- Waiting $CleanupDelayMilliseconds ms for proof cleanup windows.")
        Start-Sleep -Milliseconds $CleanupDelayMilliseconds
    }
    Invoke-ObservedCommand "motm dev observe snapshot post-proof-cleanup"
    Invoke-ObservedCommand "motm dev test mobs clear" -TimeoutMilliseconds 9000 -DelayMilliseconds 900
    Invoke-ObservedCommand "motm dev observe snapshot post-target-cleanup"

    Invoke-ObservedCommand "motm dev observe marker baseline-end"
    Invoke-ObservedCommand "motm dev observe snapshot final"
    Invoke-ObservedCommand "motm dev observe stop baseline-complete"

    if (-not $SkipScreenshot -and (Test-IsMacOS)) {
        $externalDir = Join-Path $outDir "external"
        New-Item -ItemType Directory -Path $externalDir -Force | Out-Null
        $screenshot = Join-Path $externalDir "hytale-screen.png"
        & screencapture -x $screenshot
        if ($LASTEXITCODE -eq 0 -and (Test-Path -LiteralPath $screenshot)) {
            Add-Line("- PASS: captured screenshot: $screenshot")
        } else {
            Add-Line("- WARN: screenshot capture failed or was blocked.")
        }
    }

    Add-Line("")
    Add-Line("## Evidence Collection")
    Add-Line("")
    $collectArgs = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", (Join-Path $PSScriptRoot "collect-observability-evidence.ps1"),
        "-WorldName", $WorldName,
        "-RunId", $RunId,
        "-OutDir", $outDir
    )
    if (-not [string]::IsNullOrWhiteSpace($DataDir)) {
        $collectArgs += @("-DataDir", $DataDir)
    }
    & $script:PowerShellExe @collectArgs 2>&1 |
        Tee-Object -FilePath (Join-Path $outDir "collect-observability-evidence.log")
    if ($LASTEXITCODE -ne 0) {
        throw "Evidence collection failed with exit code $LASTEXITCODE."
    }
    Assert-RunEvidence

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
    $reportPath = Join-Path $outDir "baseline-report.md"
    $report | Set-Content -LiteralPath $reportPath -Encoding UTF8
    Write-Host "[run-agent-observability-baseline] $status report: $reportPath"
}
