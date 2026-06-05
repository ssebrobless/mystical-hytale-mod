param(
    [ValidateSet("terra", "hydro", "aero", "corruptus")]
    [string]$ClassId = "hydro",
    [string]$StyleId = "snow",
    [string]$AbilityId = "snow_imp",
    [string]$WorldName = "MOTM Creative Test",
    [int]$ObserveSeconds = 24,
    [switch]$SkipVideo
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
$phase = "summon-acceptance"
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $phase $runId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

function Get-LatestServerLog {
    $logDir = Join-Path $env:APPDATA ("Hytale\UserData\Saves\" + $WorldName + "\logs")
    Get-ChildItem -LiteralPath $logDir -Filter "*_server.log" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Send-MotmCommand([string]$Text, [int]$DelayMilliseconds = 700) {
    Write-Host "[run-summon-acceptance] /$Text"
    & (Join-Path $PSScriptRoot "send-input.ps1") -Action Command -Text $Text -DelayMilliseconds 120 | Out-Host
    Start-Sleep -Milliseconds $DelayMilliseconds
}

function Capture([string]$Name) {
    & (Join-Path $PSScriptRoot "capture-evidence.ps1") -Phase $phase -RunId $runId -Name $Name -WindowTitle "Hytale" | Out-Host
}

function Read-NewLogLines([string]$Path, [long]$StartOffset) {
    $lines = New-Object System.Collections.Generic.List[string]
    $fs = [System.IO.File]::Open($Path, "Open", "Read", "ReadWrite")
    try {
        $fs.Position = [Math]::Min($StartOffset, $fs.Length)
        $reader = New-Object System.IO.StreamReader($fs)
        while (($line = $reader.ReadLine()) -ne $null) {
            $lines.Add($line)
        }
    } finally {
        if ($reader) { $reader.Dispose() }
        $fs.Dispose()
    }
    return @($lines)
}

$log = Get-LatestServerLog
if (-not $log) {
    throw "No server log found for world '$WorldName'. Launch and load the world first."
}
$startOffset = $log.Length

$marker = "summon-acceptance-$runId-$AbilityId"
Send-MotmCommand "motm dev audit marker $marker" 450
Send-MotmCommand "motm dev test stop" 450
Send-MotmCommand "motm dev test mobs clear" 900
Send-MotmCommand "motm dev freecast on" 450
Send-MotmCommand "motm dev class set $ClassId" 650
Send-MotmCommand "motm dev styles clear" 450
Send-MotmCommand "motm style $StyleId" 1100
Send-MotmCommand "motm dev test mobs stationary" 1300
Send-MotmCommand "motm dev test mobs count" 450

& (Join-Path $PSScriptRoot "send-input.ps1") -Action ThirdPerson -DelayMilliseconds 350 | Out-Host
& (Join-Path $PSScriptRoot "send-input.ps1") -Action Back -HoldMilliseconds 450 -DelayMilliseconds 250 | Out-Host
Capture "before-cast"

$videoProcess = $null
if (-not $SkipVideo) {
    $captureScript = Join-Path $PSScriptRoot "capture-evidence.ps1"
    $videoArgs = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", $captureScript,
        "-Phase", $phase,
        "-RunId", $runId,
        "-Name", "cast-window",
        "-WindowTitle", "Hytale",
        "-Video",
        "-VideoSeconds", ([string]([Math]::Max(8, $ObserveSeconds)))
    )
    $videoProcess = Start-Process -FilePath "powershell.exe" -ArgumentList $videoArgs -PassThru -WindowStyle Hidden
    Start-Sleep -Seconds 2
}

Send-MotmCommand "motm dev test ability $AbilityId" 1200
Start-Sleep -Seconds 6
Capture "after-cast-6s"
Start-Sleep -Seconds ([Math]::Max(0, $ObserveSeconds - 6))
Capture "after-cast-${ObserveSeconds}s"

if ($videoProcess) {
    Wait-Process -Id $videoProcess.Id -Timeout ([Math]::Max(10, $ObserveSeconds + 10)) -ErrorAction SilentlyContinue
}

Send-MotmCommand "motm dev clear" 900
Capture "after-dev-clear"

$lines = Read-NewLogLines $log.FullName $startOffset
$markerIndex = -1
for ($i = 0; $i -lt $lines.Count; $i++) {
    if ($lines[$i] -match [regex]::Escape($marker)) {
        $markerIndex = $i
        break
    }
}
if ($markerIndex -ge 0) {
    $lines = @($lines | Select-Object -Skip $markerIndex)
}
$proofLog = Join-Path $outDir "server-window.log"
$lines | Set-Content -LiteralPath $proofLog -Encoding UTF8

$escaped = [regex]::Escape($AbilityId)
$blocking = @($lines | Where-Object { $_ -match "NoClassDefFoundError|ClassNotFoundException|Exception|ERROR|Reloading nonexistent|Cannot find model" })
$warnings = @($lines | Where-Object { $_ -match "Unmapped NPC type" })
$spawn = @($lines | Where-Object { $_ -match "summon combat spawn:.*ability=$escaped" })
$target = @($lines | Where-Object { $_ -match "summon combat target:.*ability=$escaped" })
$attack = @($lines | Where-Object { $_ -match "summon combat attack:.*ability=$escaped.*damage=[1-9]" })
$despawn = @($lines | Where-Object { $_ -match "summon combat despawn:.*ability=$escaped" })
$cleanup = @($lines | Where-Object { $_ -match "summon combat despawn:.*ability=$escaped|Dev: player cleared|player cleared to a fresh state" })
$castResult = @($lines | Where-Object { $_ -match "Queued ability cast result:.*abilityId=$escaped" } | Select-Object -Last 1)

$mechanicalStatus = if ($blocking.Count -eq 0 -and $spawn.Count -gt 0 -and $target.Count -gt 0 -and $attack.Count -gt 0) {
    "PASS"
} else {
    "FAIL"
}
$cleanupStatus = if ($cleanup.Count -gt 0) { "PASS" } else { "FAIL" }
$visualStatus = "REVIEW"

$report = @(
    "# Summon Acceptance: $ClassId/$StyleId/$AbilityId",
    "",
    '```text',
    "+----------------------+--------------------------------------------+",
    "| Gate                 | Result                                     |",
    "+----------------------+--------------------------------------------+",
    ("| Runtime window       | {0,-42} |" -f "captured"),
    ("| Spawn marker         | {0,-42} |" -f ($(if ($spawn.Count -gt 0) { "PASS x$($spawn.Count)" } else { "FAIL" }))),
    ("| Target marker        | {0,-42} |" -f ($(if ($target.Count -gt 0) { "PASS x$($target.Count)" } else { "FAIL" }))),
    ("| Attack marker        | {0,-42} |" -f ($(if ($attack.Count -gt 0) { "PASS x$($attack.Count)" } else { "FAIL" }))),
    ("| Despawn marker       | {0,-42} |" -f ($(if ($despawn.Count -gt 0) { "PASS x$($despawn.Count)" } else { "REVIEW" }))),
    ("| Cleanup marker       | {0,-42} |" -f ($(if ($cleanupStatus -eq "PASS") { "PASS x$($cleanup.Count)" } else { "FAIL" }))),
    ("| Warning markers      | {0,-42} |" -f ($(if ($warnings.Count -gt 0) { "REVIEW x$($warnings.Count)" } else { "PASS none" }))),
    ("| Blocking errors      | {0,-42} |" -f ($(if ($blocking.Count -eq 0) { "PASS none" } else { "FAIL x$($blocking.Count)" }))),
    ("| Mechanical verdict   | {0,-42} |" -f $mechanicalStatus),
    ("| Visual verdict       | {0,-42} |" -f $visualStatus),
    "+----------------------+--------------------------------------------+",
    '```',
    "",
    "- Source log: $($log.FullName)",
    "- Window log: $proofLog",
    "- Evidence folder: $outDir",
    "- Cast result: $([string]($castResult -join ' '))",
    "- Visual note: screenshots/video still require human review for actual model readability, framing, and concept match."
)

if ($blocking.Count -gt 0) {
    $report += ""
    $report += "## Blocking Lines"
    $report += @($blocking | ForEach-Object { "- $_" })
}
if ($warnings.Count -gt 0) {
    $report += ""
    $report += "## Warning Lines"
    $report += @($warnings | ForEach-Object { "- $_" })
}

$reportPath = Join-Path $outDir "report.md"
$report | Set-Content -LiteralPath $reportPath -Encoding UTF8
Write-Host "[run-summon-acceptance] Wrote $reportPath"

if ($mechanicalStatus -ne "PASS" -or $cleanupStatus -ne "PASS") {
    exit 1
}
