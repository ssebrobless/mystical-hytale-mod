param(
    [string]$WorldName = "MOTM Creative Test",
    [string]$ClassId = "terra",
    [string]$StyleId = "quake",
    [switch]$CloseGroundedTarget,
    [switch]$SkipSpellbookOverview,
    [int]$CommandDelayMilliseconds = 650,
    [int]$TimeoutSeconds = 45
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
$phaseId = "harness/setup-test-world"
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $phaseId $runId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

$report = New-Object System.Collections.Generic.List[string]
$report.Add("# Setup Test World")
$report.Add("")
$report.Add("- Run: $runId")
$report.Add("- World: $WorldName")
$report.Add("- Class: $ClassId")
$report.Add("- Style: $StyleId")
$report.Add("- Close grounded target: $CloseGroundedTarget")
$report.Add("- Spellbook overview: " + ($(if ($SkipSpellbookOverview) { "skipped" } else { "requested" })))
$report.Add("")

function Add-ReportLine([string]$Line) {
    $script:report.Add($Line)
}

function Get-LatestServerLog {
    $logDir = Join-Path $env:APPDATA ("Hytale\UserData\Saves\" + $WorldName + "\logs")
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        $log = Get-ChildItem -LiteralPath $logDir -Filter "*_server.log" -File -ErrorAction SilentlyContinue |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if ($log) { return $log }
        Start-Sleep -Milliseconds 250
    } while ((Get-Date) -lt $deadline)

    throw "No server log found under $logDir"
}

function Send-MotmCommand([string]$Text) {
    Write-Host "[setup-test-world] /$Text"
    & (Join-Path $PSScriptRoot "send-dev-command.ps1") `
        -Command $Text `
        -WorldName $WorldName `
        -TimeoutMilliseconds ($TimeoutSeconds * 1000)
    if ($LASTEXITCODE -ne 0) {
        throw "Dev command bridge failed: /$Text"
    }
    Start-Sleep -Milliseconds $CommandDelayMilliseconds
}

function Wait-LogPattern([string]$Pattern, [long]$StartOffset) {
    $log = Get-LatestServerLog
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $fs = [System.IO.File]::Open($log.FullName, "Open", "Read", "ReadWrite")
    try {
        $fs.Position = [Math]::Min($StartOffset, $fs.Length)
        $reader = New-Object System.IO.StreamReader($fs)
        while ((Get-Date) -lt $deadline) {
            $line = $reader.ReadLine()
            if ($null -eq $line) {
                Start-Sleep -Milliseconds 200
                continue
            }
            if ($line -match $Pattern) {
                return $line
            }
        }
    } finally {
        $fs.Dispose()
    }

    throw "Timeout waiting for server log pattern: $Pattern"
}

$status = "FAIL"
try {
    $log = Get-LatestServerLog
    $startOffset = $log.Length
    Add-ReportLine("- Server log: $($log.FullName)")

    Send-MotmCommand "motm dev freecast on"
    Send-MotmCommand "motm dev class set $ClassId"
    Send-MotmCommand "motm dev styles clear"
    Send-MotmCommand "motm style $StyleId"
    if ($CloseGroundedTarget) {
        Send-MotmCommand "motm dev test mobs close"
    } else {
        Send-MotmCommand "motm dev test mobs"
    }
    if (-not $SkipSpellbookOverview) {
        Send-MotmCommand "motm spellbook overview"
    }

    $styleLine = Wait-LogPattern "\[MOTM\].*selected styles: \[$StyleId\]" $startOffset
    $mobLine = Wait-LogPattern "\[MOTM\] Style test mobs spawned: count=2" $startOffset

    Add-ReportLine("- Class command: confirmed by dev-command bridge")
    Add-ReportLine("- Style command: $styleLine")
    Add-ReportLine("- Mob spawn: $mobLine")

    Copy-Item -LiteralPath $log.FullName -Destination (Join-Path $outDir "server.log") -Force
    & (Join-Path $PSScriptRoot "capture-evidence.ps1") -Phase $phaseId -RunId $runId -Name "test-mobs" | Tee-Object -FilePath (Join-Path $outDir "capture.txt")

    Add-ReportLine("")
    Add-ReportLine("PASS")
    $status = "PASS"
} catch {
    Add-ReportLine("")
    Add-ReportLine("FAIL")
    Add-ReportLine("")
    Add-ReportLine("Error: $($_.Exception.Message)")
    throw
} finally {
    $reportPath = Join-Path $outDir "report.md"
    $report | Set-Content -LiteralPath $reportPath -Encoding UTF8
    Write-Host "[setup-test-world] $status report: $reportPath"
}
