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
    Get-Process -Name HytaleClient,hytale-launcher -ErrorAction SilentlyContinue |
        Select-Object ProcessName,Id,StartTime,MainWindowTitle,Responding,Path |
        ConvertTo-Json -Depth 4 |
        Set-Content -LiteralPath (Join-Path $outDir "$Name-processes.json") -Encoding UTF8
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

$clients = @(Get-Process -Name HytaleClient -ErrorAction SilentlyContinue)
foreach ($process in $clients) {
    try {
        if ($process.MainWindowHandle -ne 0) {
            [void]$process.CloseMainWindow()
        }
    } catch {
        Write-Warning "[reset-hytale-clients] CloseMainWindow failed for PID=$($process.Id): $($_.Exception.Message)"
    }
}

if ($GraceSeconds -gt 0) {
    Start-Sleep -Seconds $GraceSeconds
}

$remainingClients = @(Get-Process -Name HytaleClient -ErrorAction SilentlyContinue)
foreach ($process in $remainingClients) {
    try {
        Stop-Process -Id $process.Id -Force -ErrorAction Stop
    } catch {
        Write-Warning "[reset-hytale-clients] Stop-Process failed for PID=$($process.Id): $($_.Exception.Message)"
    }
}

if (-not $KeepLauncher) {
    foreach ($process in @(Get-Process -Name hytale-launcher -ErrorAction SilentlyContinue)) {
        try {
            if ($process.MainWindowHandle -ne 0) {
                [void]$process.CloseMainWindow()
            }
        } catch {
            Write-Warning "[reset-hytale-clients] Launcher CloseMainWindow failed for PID=$($process.Id): $($_.Exception.Message)"
        }
    }
}

Start-Sleep -Milliseconds 500
Write-ProcessSnapshot "after"

$remaining = @(Get-Process -Name HytaleClient -ErrorAction SilentlyContinue)
if ($remaining.Count -gt 0) {
    throw "Hytale client cleanup failed; $($remaining.Count) HytaleClient process(es) remain. Evidence: $outDir"
}

Write-Host "[reset-hytale-clients] PASS evidence: $outDir"
