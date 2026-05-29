param(
    [string]$WorldName = "MOTM Creative Test",
    [string]$RunId = "",
    [string[]]$Classes = @("terra", "hydro", "aero", "corruptus"),
    [string[]]$Styles = @(),
    [switch]$SkipBuild,
    [switch]$StopOnFailure,
    [int]$CleanupDelayMilliseconds = 6500,
    [int]$PerStyleTimeoutSeconds = 300,
    [int]$MinimumFreeMemoryMB = 1024
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}
$Classes = @($Classes | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
$Styles = @($Styles | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim() } | Where-Object { $_ })

$outDir = Join-Path $repoRoot (Join-Path "audits\style-sweeps" $RunId)
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

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

function Get-AvailableMemoryMB {
    $os = Get-CimInstance Win32_OperatingSystem -ErrorAction SilentlyContinue
    if (-not $os) {
        return [int]::MaxValue
    }
    return [int]([double]$os.FreePhysicalMemory / 1024.0)
}

function Get-StaleHarnessPowerShellProcesses {
    $scriptPattern = "scripts[\\/](run-style-observability-sweep|run-agent-observability-baseline|send-dev-command)\.ps1"
    @(Get-CimInstance Win32_Process -Filter "Name = 'powershell.exe' OR Name = 'pwsh.exe'" -ErrorAction SilentlyContinue |
        Where-Object {
            $commandLine = [string]$_.CommandLine
            [int]$_.ProcessId -ne $PID -and
                $commandLine -match $scriptPattern
        })
}

function Wait-HarnessResourceBudget {
    param([string]$Description)

    $deadline = (Get-Date).AddSeconds(120)
    while ((Get-Date) -lt $deadline) {
        $freeMemory = Get-AvailableMemoryMB
        $stale = @(Get-StaleHarnessPowerShellProcesses)
        if ($freeMemory -ge $MinimumFreeMemoryMB -and $stale.Count -eq 0) {
            return
        }

        if ($stale.Count -gt 0) {
            Write-Host "[style-sweep] cleaning stale harness PowerShell processes before ${Description}: $($stale.ProcessId -join ', ')"
            foreach ($process in $stale) {
                Stop-ProcessTree -RootProcessId ([int]$process.ProcessId)
            }
        }

        if ($freeMemory -lt $MinimumFreeMemoryMB) {
            Write-Host "[style-sweep] waiting for memory budget before ${Description}: free=${freeMemory}MB required=${MinimumFreeMemoryMB}MB"
            Start-Sleep -Seconds 3
        }
    }

    throw "Timed out waiting for harness resource budget before $Description."
}

function Invoke-StyleBaselineRun {
    param(
        [string[]]$Arguments,
        [string]$LogPath,
        [string]$Description
    )

    New-Item -ItemType Directory -Path (Split-Path -Parent $LogPath) -Force | Out-Null
    $stdoutPath = "$LogPath.stdout.tmp"
    $stderrPath = "$LogPath.stderr.tmp"
    Remove-Item -LiteralPath $stdoutPath, $stderrPath -Force -ErrorAction SilentlyContinue

    $child = $null
    try {
        $child = Start-Process -FilePath $psExe `
            -ArgumentList $Arguments `
            -NoNewWindow `
            -PassThru `
            -RedirectStandardOutput $stdoutPath `
            -RedirectStandardError $stderrPath

        $timeoutMs = [Math]::Max($PerStyleTimeoutSeconds * 1000, 15000)
        if (-not $child.WaitForExit($timeoutMs)) {
            Stop-ProcessTree -RootProcessId $child.Id
            $message = "Timed out after ${timeoutMs}ms: $Description"
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

function Get-MobModeForStyle($style) {
    $text = (($style.abilities | ConvertTo-Json -Depth 8) + " " + [string]$style.id).ToLowerInvariant()
    if ($text -match "radius|aoe|field|storm|explosion|chain|pool|summon|spawn|lure|taunt") {
        return "cluster"
    }
    return "stationary"
}

$psExe = Resolve-PowerShellExecutable
$stylesRoot = Join-Path $repoRoot "src\main\resources\data\styles"
$styleDocs = Get-ChildItem -LiteralPath $stylesRoot -Filter "*.json" |
    ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json }
$allStyles = @($styleDocs | ForEach-Object { $_.styles })
$selectedStyles = @($allStyles | Where-Object {
    $Classes -contains [string]$_.class_id -and ($Styles.Count -eq 0 -or $Styles -contains [string]$_.id)
})
if ($selectedStyles.Count -eq 0) {
    throw "No styles selected. Classes=[$($Classes -join ',')] Styles=[$($Styles -join ',')]"
}

$summaryRows = New-Object System.Collections.Generic.List[object]
$report = New-Object System.Collections.Generic.List[string]
$report.Add("# Style Observability Sweep")
$report.Add("")
$report.Add("- RunId: $RunId")
$report.Add("- World: $WorldName")
$report.Add("- Classes: $($Classes -join ', ')")
$report.Add("- Styles: $($selectedStyles.Count)")
$report.Add("")
$report.Add("| Class | Style | Abilities | Mob mode | Status | Run | Notes |")
$report.Add("|---|---|---|---|---|---|---|")

foreach ($style in $selectedStyles) {
    $classId = [string]$style.class_id
    $styleId = [string]$style.id
    $abilities = @($style.abilities | ForEach-Object { [string]$_.id })
    $mobMode = Get-MobModeForStyle $style
    $styleRunId = "$RunId-$classId-$styleId"
    $args = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", (Join-Path $PSScriptRoot "run-agent-observability-baseline.ps1"),
        "-WorldName", $WorldName,
        "-RunId", $styleRunId,
        "-ScenarioId", "style-sweep-$classId-$styleId",
        "-ClassId", $classId,
        "-StyleId", $styleId,
        "-MobMode", $mobMode,
        "-Abilities", ($abilities -join ","),
        "-NoDefaultProofs"
    )
    $args += @("-CleanupDelayMilliseconds", $CleanupDelayMilliseconds)
    if ($SkipBuild) {
        $args += "-SkipBuild"
    }

    $logPath = Join-Path $outDir "$classId-$styleId.log"
    Write-Host "[style-sweep] START $classId/$styleId abilities=$($abilities -join ',') mobMode=$mobMode"
    $status = "PASS"
    $notes = ""
    try {
        Wait-HarnessResourceBudget -Description "$classId/$styleId"
        Invoke-StyleBaselineRun -Arguments $args -LogPath $logPath -Description "style baseline $classId/$styleId"
    } catch {
        $status = "FAIL"
        $notes = $_.Exception.Message -replace "\|", "/"
        Write-Host "[style-sweep] FAIL $classId/$styleId $notes"
        if ($StopOnFailure) {
            throw
        }
    }

    $runPath = "audits/agent-observability/$styleRunId"
    $report.Add("| $classId | $styleId | $($abilities -join ', ') | $mobMode | $status | $runPath | $notes |")
    $summaryRows.Add([PSCustomObject]@{
        class = $classId
        style = $styleId
        abilities = ($abilities -join ",")
        mob_mode = $mobMode
        status = $status
        run_id = $styleRunId
        run_path = $runPath
        notes = $notes
    })
}

$summaryRows | Export-Csv -LiteralPath (Join-Path $outDir "summary.csv") -NoTypeInformation -Encoding UTF8
$summaryRows | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $outDir "summary.json") -Encoding UTF8
$report | Set-Content -LiteralPath (Join-Path $outDir "report.md") -Encoding UTF8

$failCount = @($summaryRows | Where-Object { $_.status -ne "PASS" }).Count
Write-Host "[style-sweep] Report: $(Join-Path $outDir "report.md")"
if ($failCount -gt 0) {
    Write-Host "[style-sweep] FAILURES: $failCount"
    exit 1
}
Write-Host "[style-sweep] PASS: all selected styles passed baseline evidence checks"
