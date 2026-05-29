param(
    [string]$WorldName = "Main",
    [string]$RunId = "",
    [string]$ScenarioId = "baseline",
    [string]$DataDir = "",
    [string]$JavaHome = "",
    [switch]$SkipBuild,
    [switch]$SkipScreenshot,
    [int]$CleanupDelayMilliseconds = 5500,
    [string]$ClassId = "",
    [string]$StyleId = "",
    [string]$MobMode = "",
    [string[]]$Abilities = @(),
    [string[]]$Proofs = @(),
    [switch]$NoDefaultProofs,
    [int]$MinimumFreeMemoryMB = 512
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

function Test-IsMacOS {
    return ($global:IsMacOS -eq $true)
}

function Resolve-PowerShellExecutable {
    if ($env:OS -eq "Windows_NT") {
        $powershell = Get-Command powershell -ErrorAction SilentlyContinue
        if ($powershell) { return $powershell.Source }
    }

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

    throw "Could not locate powershell or pwsh for child script execution."
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

function Resolve-HytaleRoot {
    if (-not [string]::IsNullOrWhiteSpace($env:HYTALE_ROOT)) {
        return $env:HYTALE_ROOT
    }
    if (-not [string]::IsNullOrWhiteSpace($env:APPDATA)) {
        $candidate = Join-Path $env:APPDATA "Hytale"
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }
    if (Test-IsMacOS) {
        $candidate = Join-Path $HOME "Library/Application Support/Hytale"
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }
    return ""
}

function Resolve-JavapExecutable {
    if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
        $javapName = if ($env:OS -eq "Windows_NT") { "javap.exe" } else { "javap" }
        $candidate = Join-Path $JavaHome (Join-Path "bin" $javapName)
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }

    $command = Get-Command javap -ErrorAction SilentlyContinue
    if ($command) {
        return $command.Source
    }

    throw "Could not locate javap to verify the installed MOTM build channel. Use a JDK JavaHome, not a JRE."
}

function Assert-InstalledInternalTesterJar {
    $hytaleRoot = Resolve-HytaleRoot
    if ([string]::IsNullOrWhiteSpace($hytaleRoot)) {
        throw "Could not resolve Hytale root to verify installed MOTM build channel. Set HYTALE_ROOT or APPDATA."
    }

    $modsDir = Join-Path $hytaleRoot "UserData/Mods"
    if (-not (Test-Path -LiteralPath $modsDir)) {
        throw "Could not find Hytale mods directory at $modsDir."
    }

    $installedJars = @(Get-ChildItem -LiteralPath $modsDir -Filter "mentees_of_the_mystical-*.jar" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending)
    if ($installedJars.Count -eq 0) {
        throw "No MOTM jar is installed in $modsDir. Run the harness without -SkipBuild or run scripts/build-install.ps1."
    }
    if ($installedJars.Count -gt 1) {
        throw "Multiple MOTM jars are installed in ${modsDir}: $($installedJars.Name -join ', '). Remove stale jars before launching Hytale."
    }

    $installedJar = $installedJars[0].FullName
    $javap = Resolve-JavapExecutable
    $buildInfo = & $javap -classpath $installedJar -constants com.motm.MotmBuildInfo 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "Could not inspect MotmBuildInfo in installed jar $installedJar. javap output: $($buildInfo -join ' ')"
    }
    if (($buildInfo -join "`n") -notmatch 'INTERNAL_TEST_BUILD\s*=\s*true') {
        throw "Installed MOTM jar is not an internal tester build: $installedJar. Rebuild with -Pmotm_build_channel=internal and restart Hytale before running observability scenarios."
    }

    Add-Line("- PASS: installed jar is internal tester build: $installedJar")
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

function Resolve-ScenarioFile {
    param([string]$RequestedScenarioId)

    if ([string]::IsNullOrWhiteSpace($RequestedScenarioId)) {
        return ""
    }
    $scenarioName = $RequestedScenarioId.Trim().ToLowerInvariant()
    $scenarioPath = Join-Path $repoRoot (Join-Path "scripts/scenarios" ($scenarioName + ".json"))
    if (Test-Path -LiteralPath $scenarioPath) {
        return $scenarioPath
    }
    return ""
}

function Apply-ScenarioDefaults {
    $scenarioPath = Resolve-ScenarioFile $ScenarioId
    if ([string]::IsNullOrWhiteSpace($scenarioPath)) {
        if ($Proofs.Count -eq 0 -and -not $NoDefaultProofs) {
            $script:Proofs = @("coating-metal")
        }
        return
    }

    $scenario = Get-Content -LiteralPath $scenarioPath -Raw | ConvertFrom-Json -ErrorAction Stop
    if ([string]::IsNullOrWhiteSpace($StyleId) -and -not [string]::IsNullOrWhiteSpace([string]$scenario.styleId)) {
        $script:StyleId = [string]$scenario.styleId
    }
    if ($Abilities.Count -eq 0 -and $null -ne $scenario.abilities) {
        $script:Abilities = @($scenario.abilities | ForEach-Object { [string]$_ } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    }
    if ($Proofs.Count -eq 0 -and $null -ne $scenario.proofs) {
        $script:Proofs = @($scenario.proofs | ForEach-Object { [string]$_ } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    }
    $script:ScenarioSetupCommands = @($scenario.setupCommands | ForEach-Object { [string]$_ } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $script:ScenarioCommands = @($scenario.commands | ForEach-Object { [string]$_ } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $script:ScenarioCleanupCommands = @($scenario.cleanupCommands | ForEach-Object { [string]$_ } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $script:ScenarioExpectedEvidence = @($scenario.expectedEvidence | ForEach-Object { [string]$_ } | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $script:ScenarioDescription = [string]$scenario.description
    $script:ScenarioFile = $scenarioPath
}

$script:ScenarioSetupCommands = @()
$script:ScenarioCommands = @()
$script:ScenarioCleanupCommands = @()
$script:ScenarioExpectedEvidence = @()
$script:ScenarioDescription = ""
$script:ScenarioFile = ""
Apply-ScenarioDefaults

function Add-Line([string]$Line) {
    $script:report.Add($Line)
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

function Get-AvailableMemoryMB {
    $os = Get-CimInstance Win32_OperatingSystem -ErrorAction SilentlyContinue
    if (-not $os) {
        return [int]::MaxValue
    }
    return [int]([double]$os.FreePhysicalMemory / 1024.0)
}

function Wait-HarnessResourceBudget {
    param([string]$Description)

    $deadline = (Get-Date).AddSeconds(60)
    while ((Get-Date) -lt $deadline) {
        $freeMemory = Get-AvailableMemoryMB
        if ($freeMemory -ge $MinimumFreeMemoryMB) {
            return
        }
        Write-Host "[run-agent-observability-baseline] waiting for memory budget before ${Description}: free=${freeMemory}MB required=${MinimumFreeMemoryMB}MB"
        Start-Sleep -Seconds 2
    }

    throw "Timed out waiting for harness memory budget before $Description."
}

function Invoke-HarnessChildProcess {
    param(
        [Parameter(Mandatory = $true)]
        [string[]]$Arguments,
        [Parameter(Mandatory = $true)]
        [string]$LogPath,
        [Parameter(Mandatory = $true)]
        [int]$TimeoutMilliseconds,
        [Parameter(Mandatory = $true)]
        [string]$Description
    )

    New-Item -ItemType Directory -Path (Split-Path -Parent $LogPath) -Force | Out-Null
    $stdoutPath = "$LogPath.stdout.tmp"
    $stderrPath = "$LogPath.stderr.tmp"
    Remove-Item -LiteralPath $stdoutPath, $stderrPath -Force -ErrorAction SilentlyContinue

    Wait-HarnessResourceBudget -Description $Description

    $process = Start-Process -FilePath $script:PowerShellExe `
        -ArgumentList $Arguments `
        -NoNewWindow `
        -PassThru `
        -RedirectStandardOutput $stdoutPath `
        -RedirectStandardError $stderrPath

    $hardTimeout = [Math]::Max($TimeoutMilliseconds + 10000, 15000)
    if (-not $process.WaitForExit($hardTimeout)) {
        Stop-ProcessTree -RootProcessId $process.Id
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
    Remove-Item -LiteralPath $stdoutPath, $stderrPath -Force -ErrorAction SilentlyContinue

    if ($process.ExitCode -ne 0) {
        throw "$Description exited with code $($process.ExitCode). See command log: $LogPath"
    }
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
    $commandLog = Join-Path $outDir ("command-" + ($traceId -replace '[^A-Za-z0-9_.-]', '-') + ".log")
    Invoke-HarnessChildProcess `
        -Arguments $sendArgs `
        -LogPath $commandLog `
        -TimeoutMilliseconds $TimeoutMilliseconds `
        -Description "Command /motm $displayCommand"
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

function Invoke-ScenarioCommandList {
    param(
        [string]$Label,
        [string[]]$Commands,
        [int]$TimeoutMilliseconds = 9000,
        [int]$DelayMilliseconds = 900
    )

    if ($Commands.Count -eq 0) {
        Add-Line("- Scenario $Label commands: <none>")
        return
    }

    Add-Line("- Scenario $Label commands: $($Commands.Count)")
    foreach ($command in $Commands) {
        Invoke-ObservedCommand $command -TimeoutMilliseconds $TimeoutMilliseconds -DelayMilliseconds $DelayMilliseconds
    }
}

function Get-ScenarioRuntimeTaskExpectations {
    $expectedTaskTypes = New-Object System.Collections.Generic.HashSet[string]
    $allCommands = @($script:ScenarioSetupCommands) + @($script:ScenarioCommands) + @($script:ScenarioCleanupCommands)
    foreach ($command in $allCommands) {
        $normalized = ([string]$command).Trim().ToLowerInvariant()
        if ($normalized -match '^motm\s+dev\s+test\s+mobs\s+count\b') {
            [void]$expectedTaskTypes.Add("style-test-mob-count")
            continue
        }
        if ($normalized -match '^motm\s+dev\s+test\s+mobs\s+clear\b') {
            [void]$expectedTaskTypes.Add("style-test-mob-clear")
            continue
        }
        if ($normalized -match '^motm\s+dev\s+test\s+mobs\b') {
            [void]$expectedTaskTypes.Add("style-test-mob-spawn")
            continue
        }
    }
    return @($expectedTaskTypes)
}

function Test-ArmedJumpAbilityEvidence {
    param(
        [Parameter(Mandatory = $true)]
        [string]$AbilityId,
        [Parameter(Mandatory = $true)]
        [array]$CausalityEvents,
        [Parameter(Mandatory = $true)]
        [array]$ServerTruthEvents
    )

    $began = @($CausalityEvents | Where-Object {
        -not $_.parseError `
            -and [string]$_.type -eq "ability_cast_begin" `
            -and [string]$_.data.abilityId -eq $AbilityId
    }).Count -gt 0
    if (-not $began) {
        return $false
    }

    $expectedLabel = "ability-$AbilityId-after"
    $armedSnapshots = @($ServerTruthEvents | Where-Object {
        -not $_.parseError `
            -and [string]$_.type -eq "snapshot" `
            -and [string]$_.data.label -eq $expectedLabel `
            -and [bool]$_.data.activeRuntime.player.armedStomp
    })

    return $armedSnapshots.Count -gt 0
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
    $serverTruthEvents = Read-JsonlObjects $serverTruthPath
    if ($Abilities.Count -gt 0) {
        $abilityEnds = @($causalityEvents | Where-Object { $_.type -eq "ability_cast_end" })
        $endedAbilityIds = @($abilityEnds | ForEach-Object { [string]$_.data.abilityId })
        $missingAbilities = @($Abilities | Where-Object {
            $endedAbilityIds -notcontains $_ `
                -and -not (Test-ArmedJumpAbilityEvidence -AbilityId $_ -CausalityEvents $causalityEvents -ServerTruthEvents $serverTruthEvents)
        })
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

    $runtimeTaskExpectations = @(Get-ScenarioRuntimeTaskExpectations)
    foreach ($taskType in $runtimeTaskExpectations) {
        $matchingRuntimeTasks = @($causalityEvents | Where-Object {
            -not $_.parseError `
                -and [string]$_.type -eq "runtime_task_executed" `
                -and [string]$_.data.taskType -eq $taskType
        })
        if ($matchingRuntimeTasks.Count -eq 0) {
            throw "Missing runtime_task_executed evidence for scenario task '$taskType'."
        }
    }

    $clientIntentEvents = Read-JsonlObjects $clientIntentPath
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

    $cleanupSnapshots = @($serverTruthEvents | Where-Object {
        -not $_.parseError `
            -and [string]$_.type -eq "snapshot" `
            -and [string]$_.data.label -eq "post-test-protection-cleanup"
    })
    if ($cleanupSnapshots.Count -eq 0) {
        throw "Missing post-test-protection-cleanup snapshot."
    }
    $cleanup = $cleanupSnapshots[-1].data
    if ([bool]$cleanup.playerData.freeCast) {
        throw "Cleanup left test protection/freecast enabled."
    }
    if (@($cleanup.runtimePlayer.nativeEntityEffects).Count -gt 0) {
        throw "Cleanup left native entity effects active."
    }
    if (@($cleanup.statusEffects).Count -gt 0) {
        throw "Cleanup left MOTM status effects active."
    }
    $runtime = $cleanup.activeRuntime
    $runtimeResidue = New-Object System.Collections.Generic.List[string]
    foreach ($field in @(
            "activeProjectiles",
            "activeFields",
            "activeTerrainSelections",
            "activeMovingTerrainTrails",
            "activeStackingColumns",
            "activeLapidaryGems",
            "activeChannels",
            "activeLineControls",
            "activePlayerAnchors",
            "activeSelfEffects",
            "visualProxyRefs",
            "activeTransformations",
            "activeWeaponFollowUps",
            "activeSummonOwners",
            "activeSummons"
        )) {
        $value = 0
        try { $value = [int]$runtime.$field } catch { $value = 0 }
        if ($value -ne 0) {
            [void]$runtimeResidue.Add("$field=$value")
        }
    }
    if ($runtimeResidue.Count -gt 0) {
        throw "Cleanup left active runtime residue: $($runtimeResidue -join ', ')"
    }

    $packetEvents = Read-JsonlObjects $packetPath
    $eventsBySource = @{
        "control" = $controlEvents
        "causality" = $causalityEvents
        "client-intent" = $clientIntentEvents
        "server-truth" = $serverTruthEvents
        "packets" = $packetEvents
    }
    foreach ($expectation in $script:ScenarioExpectedEvidence) {
        $parts = $expectation.Split(":", 2)
        if ($parts.Count -ne 2) {
            throw "Invalid scenario expectedEvidence entry '$expectation'. Expected source:type."
        }
        $source = $parts[0]
        $eventType = $parts[1]
        if (-not $eventsBySource.ContainsKey($source)) {
            throw "Invalid scenario expectedEvidence source '$source' in '$expectation'."
        }
        $matchingEvents = @($eventsBySource[$source] | Where-Object {
            if ($_.parseError) { return $false }
            if ($source -eq "packets") {
                return [string]$_.data.packetSimpleName -eq $eventType
            }
            if ($source -eq "server-truth" -and $eventType -eq "activeWeaponFollowUp") {
                try {
                    return [Int32]$_.data.activeRuntime.activeWeaponFollowUps -gt 0 `
                        -or [bool]$_.data.activeRuntime.player.activeWeaponFollowUp
                } catch {
                    return $false
                }
            }
            return [string]$_.type -eq $eventType
        })
        if ($matchingEvents.Count -eq 0) {
            throw "Missing expected scenario evidence '$expectation'."
        }
    }

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
    if (-not [string]::IsNullOrWhiteSpace($script:ScenarioDescription)) {
        Add-Line("- ScenarioDescription: $script:ScenarioDescription")
    }
    if (-not [string]::IsNullOrWhiteSpace($script:ScenarioFile)) {
        Add-Line("- ScenarioFile: $script:ScenarioFile")
    }
    Add-Line("- World: $WorldName")
    if (-not [string]::IsNullOrWhiteSpace($StyleId)) {
        Add-Line("- StyleId: $StyleId")
    }
    if (-not [string]::IsNullOrWhiteSpace($MobMode)) {
        Add-Line("- MobMode: $MobMode")
    }
    if ($Abilities.Count -gt 0) {
        Add-Line("- Abilities: $($Abilities -join ', ')")
    }
    Add-Line("- Proofs: $($Proofs -join ', ')")
    if ($script:ScenarioExpectedEvidence.Count -gt 0) {
        Add-Line("- ExpectedEvidence: $($script:ScenarioExpectedEvidence -join ', ')")
    }
    Add-Line("- APPDATA: $env:APPDATA")
    Add-Line("- JAVA_HOME: $env:JAVA_HOME")
    Add-Line("- PowerShell: $script:PowerShellExe")
    Add-Line("")

    Add-Line("## Static Scenario Validation")
    Add-Line("")
    $validateScript = Join-Path $PSScriptRoot "validate-content-shape.ps1"
    if (Test-Path -LiteralPath $validateScript) {
        & $script:PowerShellExe -NoProfile -ExecutionPolicy Bypass -File $validateScript -ProjectRoot $repoRoot 2>&1 |
            Tee-Object -FilePath (Join-Path $outDir "validate-content-shape.log")
        if ($LASTEXITCODE -ne 0) {
            throw "Content/scenario validation failed with exit code $LASTEXITCODE."
        }
        Add-Line("- PASS: content and scenario catalog validated.")
        Add-Line("")
    }

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

    Add-Line("## Installed Build Verification")
    Add-Line("")
    Assert-InstalledInternalTesterJar
    Add-Line("")

    Add-Line("## Runtime Commands")
    Add-Line("")
    Invoke-ObservedCommand "motm dev observe start $RunId $ScenarioId" -TimeoutMilliseconds 8000
    Invoke-ScenarioCommandList "setup" $script:ScenarioSetupCommands

    if (-not [string]::IsNullOrWhiteSpace($ClassId)) {
        Invoke-ObservedCommand "motm dev observe marker class-$ClassId-before"
        Invoke-ObservedCommand "motm class $ClassId" -TimeoutMilliseconds 9000 -DelayMilliseconds 900
        Invoke-ObservedCommand "motm dev observe snapshot class-$ClassId-after"
    }

    if (-not [string]::IsNullOrWhiteSpace($StyleId)) {
        Invoke-ObservedCommand "motm dev observe marker style-$StyleId-before"
        Invoke-ObservedCommand "motm style $StyleId" -TimeoutMilliseconds 9000 -DelayMilliseconds 900
        Invoke-ObservedCommand "motm dev observe snapshot style-$StyleId-after"
    }

    if (-not [string]::IsNullOrWhiteSpace($MobMode)) {
        Invoke-ObservedCommand "motm dev freecast on" -TimeoutMilliseconds 9000 -DelayMilliseconds 450
        Invoke-ObservedCommand "motm dev test mobs clear" -TimeoutMilliseconds 9000 -DelayMilliseconds 900
        Invoke-ObservedCommand "motm dev test mobs $MobMode" -TimeoutMilliseconds 9000 -DelayMilliseconds 1600
        Invoke-ObservedCommand "motm dev test mobs count" -TimeoutMilliseconds 9000 -DelayMilliseconds 700
        Invoke-ObservedCommand "motm dev observe snapshot after-target-setup"
    }

    foreach ($command in $script:ScenarioCommands) {
        Invoke-ObservedCommand $command -TimeoutMilliseconds 9000 -DelayMilliseconds 900
    }

    foreach ($ability in $Abilities) {
        Invoke-ObservedCommand "motm dev observe marker ability-$ability-before"
        Invoke-ObservedCommand "motm dev test ability $ability" -TimeoutMilliseconds 9000 -DelayMilliseconds 2200
        if ($ability -in @("stomp", "leap_frog")) {
            Invoke-ObservedCommand "motm dev test jump-land" -TimeoutMilliseconds 9000 -DelayMilliseconds 900
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
    Invoke-ScenarioCommandList "cleanup" $script:ScenarioCleanupCommands
    if (-not [string]::IsNullOrWhiteSpace($MobMode)) {
        Invoke-ObservedCommand "motm dev test mobs clear" -TimeoutMilliseconds 9000 -DelayMilliseconds 900
        Invoke-ObservedCommand "motm dev observe snapshot post-target-cleanup"
    }
    Invoke-ObservedCommand "motm dev freecast off" -TimeoutMilliseconds 9000 -DelayMilliseconds 450
    Invoke-ObservedCommand "motm dev effects clear" -TimeoutMilliseconds 9000 -DelayMilliseconds 450
    Invoke-ObservedCommand "motm dev observe snapshot post-test-protection-cleanup"
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
    Invoke-HarnessChildProcess `
        -Arguments $collectArgs `
        -LogPath (Join-Path $outDir "collect-observability-evidence.log") `
        -TimeoutMilliseconds 30000 `
        -Description "Evidence collection"
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
