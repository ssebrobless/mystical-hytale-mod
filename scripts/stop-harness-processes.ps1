param(
    [int]$OlderThanSeconds = 15,
    [switch]$WhatIfOnly
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

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

function Get-HarnessProcesses {
    $scriptPattern = "scripts[\\/](run-style-observability-sweep|run-agent-observability-baseline|send-dev-command)\.ps1"
    $selfPattern = "scripts[\\/]stop-harness-processes\.ps1"
    $cutoff = (Get-Date).AddSeconds(-[Math]::Max($OlderThanSeconds, 0))

    @(Get-CimInstance Win32_Process -Filter "Name = 'powershell.exe' OR Name = 'pwsh.exe'" -ErrorAction SilentlyContinue |
        Where-Object {
            $commandLine = [string]$_.CommandLine
            [int]$_.ProcessId -ne $PID -and
                $commandLine -match $scriptPattern -and
                $commandLine -notmatch $selfPattern
        } |
        ForEach-Object {
            $startTime = $null
            try {
                $startTime = [Management.ManagementDateTimeConverter]::ToDateTime($_.CreationDate)
            } catch {
                $startTime = Get-Date "1970-01-01"
            }

            [PSCustomObject]@{
                ProcessId = [int]$_.ProcessId
                ParentProcessId = [int]$_.ParentProcessId
                Name = [string]$_.Name
                CreationTime = $startTime
                AgeSeconds = [int]((Get-Date) - $startTime).TotalSeconds
                CommandLine = $commandLine
                IsOldEnough = $startTime -le $cutoff
            }
        } |
        Where-Object { $_.IsOldEnough })
}

$targets = @(Get-HarnessProcesses)
if ($targets.Count -eq 0) {
    Write-Host "[stop-harness-processes] No stale MOTM harness PowerShell processes found."
    exit 0
}

Write-Host "[stop-harness-processes] Found stale MOTM harness PowerShell processes:"
$targets |
    Select-Object ProcessId, ParentProcessId, Name, AgeSeconds, CommandLine |
    Format-Table -AutoSize -Wrap

if ($WhatIfOnly) {
    Write-Host "[stop-harness-processes] WhatIfOnly set; no processes stopped."
    exit 0
}

foreach ($target in $targets | Sort-Object AgeSeconds -Descending) {
    Write-Host "[stop-harness-processes] Stopping process tree rooted at PID $($target.ProcessId)"
    Stop-ProcessTree -RootProcessId $target.ProcessId
}

Start-Sleep -Milliseconds 300
$remaining = @(Get-HarnessProcesses)
if ($remaining.Count -gt 0) {
    Write-Host "[stop-harness-processes] WARN: remaining matching processes: $($remaining.ProcessId -join ', ')"
    exit 1
}

Write-Host "[stop-harness-processes] Cleanup complete."
