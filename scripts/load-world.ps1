param(
    [string]$WorldName = "MOTM Creative Test",
    [int]$LoadTimeoutSec = 180,
    [string]$DataDir = "",
    [switch]$AttemptRespawnRecovery,
    [switch]$SkipPreClickProbe
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
$auditDir = Join-Path $repoRoot (Join-Path "audits\harness\load-world" $runId)
New-Item -ItemType Directory -Path $auditDir -Force | Out-Null
$logPath = Join-Path $auditDir "load-world.log"
$reportPath = Join-Path $auditDir "report.md"

function Write-Step([string]$Message) {
    $line = "[" + (Get-Date -Format "HH:mm:ss") + "] " + $Message
    Write-Host $line
    Add-Content -Path $logPath -Value $line -Encoding UTF8
}

Add-Type -AssemblyName System.Windows.Forms
Add-Type -Name Win32 -Namespace HytaleHarnessLoad -MemberDefinition @'
[DllImport("user32.dll", SetLastError = true)]
public static extern bool SetForegroundWindow(IntPtr hWnd);

[DllImport("user32.dll")]
public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

[DllImport("user32.dll", SetLastError = true)]
public static extern bool GetWindowRect(IntPtr hWnd, out RECT lpRect);

[DllImport("user32.dll")]
public static extern bool SetCursorPos(int X, int Y);

[DllImport("user32.dll")]
public static extern void mouse_event(uint dwFlags, uint dx, uint dy, uint dwData, IntPtr dwExtraInfo);

public struct RECT {
    public int Left;
    public int Top;
    public int Right;
    public int Bottom;
}

public const int SW_RESTORE = 9;
public const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
public const uint MOUSEEVENTF_LEFTUP = 0x0004;
'@

function Get-HytaleWindow {
    $windows = @(Get-Process -Name "HytaleClient" -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowHandle -ne 0 } |
        Sort-Object StartTime -Descending)
    if ($windows.Count -gt 1) {
        $details = ($windows | ForEach-Object {
            "$($_.Id):$($_.MainWindowTitle)"
        }) -join ", "
        throw "Multiple Hytale client windows are open ($details). Run scripts/reset-hytale-clients.ps1 before load-world."
    }
    $windows | Select-Object -First 1
}

function Focus-Hytale($Process) {
    if (-not $Process) { throw "Hytale client window not found." }
    [HytaleHarnessLoad.Win32]::ShowWindow($Process.MainWindowHandle, [HytaleHarnessLoad.Win32]::SW_RESTORE) | Out-Null
    Start-Sleep -Milliseconds 200
    [HytaleHarnessLoad.Win32]::SetForegroundWindow($Process.MainWindowHandle) | Out-Null
    Start-Sleep -Milliseconds 300
}

function Get-WindowRect($Process) {
    $rect = [HytaleHarnessLoad.Win32+RECT]::new()
    if (-not [HytaleHarnessLoad.Win32]::GetWindowRect($Process.MainWindowHandle, [ref]$rect)) {
        throw "GetWindowRect failed for Hytale."
    }
    return $rect
}

function Click-Relative($Process, [double]$XRatio, [double]$YRatio, [string]$Label) {
    Focus-Hytale $Process
    $rect = Get-WindowRect $Process
    $width = $rect.Right - $rect.Left
    $height = $rect.Bottom - $rect.Top
    $x = [int]($rect.Left + ($width * $XRatio))
    $y = [int]($rect.Top + ($height * $YRatio))
    Write-Step "Clicking $Label at screen=($x,$y), window=${width}x$height"
    [HytaleHarnessLoad.Win32]::SetCursorPos($x, $y) | Out-Null
    Start-Sleep -Milliseconds 120
    [HytaleHarnessLoad.Win32]::mouse_event([HytaleHarnessLoad.Win32]::MOUSEEVENTF_LEFTDOWN, 0, 0, 0, [IntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [HytaleHarnessLoad.Win32]::mouse_event([HytaleHarnessLoad.Win32]::MOUSEEVENTF_LEFTUP, 0, 0, 0, [IntPtr]::Zero)
}

function Get-CandidateServerLogs {
    $logs = New-Object System.Collections.Generic.List[object]
    $saveRoot = Join-Path $env:APPDATA "Hytale\UserData\Saves"
    $namedLogDir = Join-Path $saveRoot (Join-Path $WorldName "logs")
    if (Test-Path -LiteralPath $namedLogDir) {
        Get-ChildItem -LiteralPath $namedLogDir -File -Filter "*.log" -ErrorAction SilentlyContinue |
            ForEach-Object { $logs.Add($_) }
    }

    if (Test-Path -LiteralPath $saveRoot) {
        Get-ChildItem -LiteralPath $saveRoot -Directory -ErrorAction SilentlyContinue |
            ForEach-Object {
                $dir = Join-Path $_.FullName "logs"
                if (Test-Path -LiteralPath $dir) {
                    Get-ChildItem -LiteralPath $dir -File -Filter "*.log" -ErrorAction SilentlyContinue |
                        ForEach-Object { $logs.Add($_) }
                }
            }
    }

    $logs |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 8
}

function Wait-OnPlayerConnect([datetime]$After, [int]$TimeoutSec) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    $threshold = if ($After -le ([datetime]::MinValue).AddDays(1)) { [datetime]::MinValue } else { $After.AddSeconds(-2) }
    do {
        foreach ($latest in @(Get-CandidateServerLogs)) {
            if ($latest -and $latest.LastWriteTime -ge $threshold) {
                $match = Select-String -Path $latest.FullName -Pattern "\[MOTM\].*>>> onPlayerConnect" -ErrorAction SilentlyContinue |
                    Select-Object -First 1
                if ($match) {
                    return [PSCustomObject]@{ Kind = "server-log"; Log = $latest.FullName; Line = $match.Line }
                }
            }
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    return $null
}

function Invoke-MotmPositionProbe([string]$Label, [int]$TimeoutMilliseconds = 3500) {
    $traceId = "load-world-" + $Label + "-" + ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())
    $commandLog = Join-Path $auditDir ("dev-position-" + $Label + ".log")
    $args = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", (Join-Path $PSScriptRoot "send-dev-command.ps1"),
        "-Command", "/motm dev position",
        "-WorldName", $WorldName,
        "-TimeoutMilliseconds", $TimeoutMilliseconds,
        "-RunDir", $auditDir,
        "-TraceId", $traceId,
        "-ScenarioId", "load-world"
    )
    if (-not [string]::IsNullOrWhiteSpace($DataDir)) {
        $args += @("-DataDir", $DataDir)
    }

    $hardTimeoutSec = [Math]::Max(5, [int][Math]::Ceiling($TimeoutMilliseconds / 1000.0) + 2)
    $probe = Start-Job -ScriptBlock {
        param([string[]]$ProbeArgs)
        $output = @(& powershell @ProbeArgs 2>&1)
        [PSCustomObject]@{
            ExitCode = $LASTEXITCODE
            Output = $output
        }
    } -ArgumentList (, $args)

    try {
        if (-not (Wait-Job -Job $probe -Timeout $hardTimeoutSec)) {
            Stop-Job -Job $probe -ErrorAction SilentlyContinue
            "Timed out after ${hardTimeoutSec}s running /motm dev position probe." |
                Set-Content -LiteralPath $commandLog -Encoding UTF8
            Write-Step "Dev position probe '$Label' timed out after ${hardTimeoutSec}s. See $commandLog"
            return $null
        }
        $jobResult = Receive-Job -Job $probe
        $output = @($jobResult.Output)
        $output | Set-Content -LiteralPath $commandLog -Encoding UTF8
        $joined = $output -join "`n"
        if ([int]$jobResult.ExitCode -eq 0 -and $joined -match "\[MOTM\] Dev position:") {
            $line = ($output | Where-Object { $_ -match "\[MOTM\] Dev position:" } | Select-Object -Last 1)
            return [PSCustomObject]@{
                Kind = "dev-position"
                Log = $commandLog
                Line = [string]$line
            }
        }
        Write-Step "Dev position probe '$Label' did not confirm in-world state. See $commandLog"
    } catch {
        $_.Exception.Message | Set-Content -LiteralPath $commandLog -Encoding UTF8
        Write-Step "Dev position probe '$Label' failed: $($_.Exception.Message)"
    } finally {
        Remove-Job -Job $probe -Force -ErrorAction SilentlyContinue
    }
    return $null
}

$result = "FAIL"
$matched = $null
$startedAt = Get-Date
try {
    $existing = $null
    if (-not $SkipPreClickProbe) {
        $existing = Invoke-MotmPositionProbe "pre-click" 2500
    } else {
        Write-Step "Skipping pre-click /motm dev position probe; launch flow is expected to be at menu."
    }
    if (-not $existing) {
        $existing = Wait-OnPlayerConnect $startedAt 1
    }
    if ($existing) {
        $matched = $existing
        Write-Step "Already in world: $($matched.Line)"
    } else {
        $proc = Get-HytaleWindow
        if (-not $proc) { throw "Hytale client is not running." }
        $coordsPath = Join-Path $PSScriptRoot "templates\menu-coords.json"
        $coords = Get-Content -Path $coordsPath -Raw | ConvertFrom-Json
        $main = $coords.'1280x720'.main_menu_worlds
        Click-Relative $proc ([double]$main.x_ratio) ([double]$main.y_ratio) "main menu Worlds"
        Start-Sleep -Milliseconds 900
        Click-Relative $proc ([double]$main.x_ratio) ([double]$main.y_ratio) "main menu Worlds confirm"
        Start-Sleep -Seconds 2
        $firstWorld = $coords.'1280x720'.worlds_first_card
        $firstWorldX = if ($firstWorld.x_ratio) { [double]$firstWorld.x_ratio } else { 0.1505 }
        $firstWorldY = if ($firstWorld.y_ratio) { [double]$firstWorld.y_ratio } else { 0.4743 }
        Click-Relative $proc $firstWorldX $firstWorldY "first world card"
        Start-Sleep -Milliseconds 400
        Click-Relative $proc $firstWorldX $firstWorldY "first world card confirm"
        Start-Sleep -Milliseconds 900
        Click-Relative $proc $firstWorldX $firstWorldY "first world card final confirm"
        $matched = Invoke-MotmPositionProbe "post-click" 6000
        if (-not $matched) {
            $matched = Wait-OnPlayerConnect $startedAt $LoadTimeoutSec
        }
        if (-not $matched) {
            throw "Timed out waiting for MOTM in-world readiness. Tried /motm dev position and [MOTM] >>> onPlayerConnect."
        }
    }

    $proc = Get-HytaleWindow
    if ($AttemptRespawnRecovery -and $proc) {
        Start-Sleep -Seconds 8
        Click-Relative $proc 0.508 0.596 "respawn recovery button"
        Start-Sleep -Seconds 2
        Click-Relative $proc 0.508 0.596 "respawn recovery button confirm"
        Start-Sleep -Seconds 8
    } elseif ($proc) {
        Write-Step "Skipping respawn recovery clicks; use -AttemptRespawnRecovery if the loaded world is on a death screen."
    }

    & (Join-Path $PSScriptRoot "capture-evidence.ps1") -Phase "harness/load-world" -RunId $runId -Name "in-world"
    if ($matched.Log -and (Test-Path -LiteralPath $matched.Log)) {
        $destName = if ($matched.Kind -eq "server-log") { "server.log" } else { "motm-position-probe.log" }
        Copy-Item -LiteralPath $matched.Log -Destination (Join-Path $auditDir $destName) -Force
    }
    $result = "PASS"
    Write-Step "PASS: MOTM in-world readiness confirmed via $($matched.Kind) in $($matched.Log)"
} finally {
    @"
# H2 load-world run - $runId

## Result
$result

## World
$WorldName

## Log evidence
$($matched.Line)

## Readiness kind
$($matched.Kind)

## Evidence
- in-world.png
- server.log or motm-position-probe.log
- load-world.log
"@ | Set-Content -Path $reportPath -Encoding UTF8
}

if ($result -ne "PASS") {
    throw "load-world failed; see $reportPath"
}
