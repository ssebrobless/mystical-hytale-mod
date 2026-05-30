param(
    [switch]$KeepLauncher,
    [int]$GraceSeconds = 5,
    [string]$RunId = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}

$outDir = Join-Path $repoRoot (Join-Path "audits\harness\window-cleanup" $RunId)
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

function Write-ProcessSnapshot([string]$Name) {
    Get-HytaleProcesses |
        Select-Object ProcessName,Id,StartTime,MainWindowTitle,Responding,Path |
        ConvertTo-Json -Depth 4 |
        Set-Content -LiteralPath (Join-Path $outDir "$Name-processes.json") -Encoding UTF8
}

function Get-HytaleProcesses {
    $hytaleRoot = Join-Path $env:APPDATA "Hytale"
    @(Get-Process -ErrorAction SilentlyContinue |
        Where-Object {
            $_.Id -ne $PID -and (
                $_.ProcessName -like "*Hytale*" -or
                $_.ProcessName -like "*hytale*" -or
                ($_.Path -and ($_.Path -like "$hytaleRoot*"))
            )
        })
}

function Stop-HytaleProcesses {
    param(
        [switch]$LauncherOnly
    )

    $targets = @(Get-HytaleProcesses | Where-Object {
        -not $LauncherOnly -or $_.ProcessName -like "*launcher*"
    })

    foreach ($process in $targets) {
        try {
            if ($process.MainWindowHandle -ne 0) {
                [void]$process.CloseMainWindow()
            }
        } catch {
            Write-Warning "[reset-hytale-clients] CloseMainWindow failed for PID=$($process.Id): $($_.Exception.Message)"
        }
    }
}

function Copy-RecentClientLogs {
    $logRoot = Join-Path $env:APPDATA "Hytale\UserData\Logs"
    if (-not (Test-Path -LiteralPath $logRoot)) {
        return
    }
    Get-ChildItem -LiteralPath $logRoot -Filter "*_client.log" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 5 |
        ForEach-Object {
            Copy-Item -LiteralPath $_.FullName -Destination (Join-Path $outDir $_.Name) -Force
        }
}

Write-ProcessSnapshot "before"
Copy-RecentClientLogs

Stop-HytaleProcesses

if ($GraceSeconds -gt 0) {
    Start-Sleep -Seconds $GraceSeconds
}

$remainingClients = @(Get-HytaleProcesses | Where-Object {
    -not $KeepLauncher -or $_.ProcessName -notlike "*launcher*"
})
foreach ($process in $remainingClients) {
    try {
        Stop-Process -Id $process.Id -Force -ErrorAction Stop
    } catch {
        Write-Warning "[reset-hytale-clients] Stop-Process failed for PID=$($process.Id): $($_.Exception.Message)"
    }
}

if (-not $KeepLauncher) {
    Stop-HytaleProcesses -LauncherOnly
    Start-Sleep -Milliseconds 500
    foreach ($process in @(Get-HytaleProcesses)) {
        Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
    }
}

Start-Sleep -Milliseconds 500
Write-ProcessSnapshot "after"

$remaining = @(Get-HytaleProcesses | Where-Object {
    -not $KeepLauncher -or $_.ProcessName -notlike "*launcher*"
})
if ($remaining.Count -gt 0) {
    throw "Hytale cleanup failed; $($remaining.Count) Hytale process(es) remain. Evidence: $outDir"
}

Write-Host "[reset-hytale-clients] PASS evidence: $outDir"
