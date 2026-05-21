param(
    [string]$WorldName = "MOTM Creative Test",
    [int]$LoadTimeoutSec = 180
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
    Get-Process -Name "HytaleClient" -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowHandle -ne 0 } |
        Sort-Object StartTime -Descending |
        Select-Object -First 1
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

function Get-LatestServerLog {
    $saveLogDir = Join-Path $env:APPDATA ("Hytale\UserData\Saves\" + $WorldName + "\logs")
    Get-ChildItem -Path $saveLogDir -File -Filter "*.log" -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Wait-OnPlayerConnect([datetime]$After, [int]$TimeoutSec) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    $threshold = if ($After -le ([datetime]::MinValue).AddDays(1)) { [datetime]::MinValue } else { $After.AddSeconds(-2) }
    do {
        $latest = Get-LatestServerLog
        if ($latest -and $latest.LastWriteTime -ge $threshold) {
            $match = Select-String -Path $latest.FullName -Pattern "\[MOTM\].*>>> onPlayerConnect" -ErrorAction SilentlyContinue |
                Select-Object -First 1
            if ($match) {
                return [PSCustomObject]@{ Log = $latest.FullName; Line = $match.Line }
            }
        }
        Start-Sleep -Seconds 2
    } while ((Get-Date) -lt $deadline)
    return $null
}

$result = "FAIL"
$matched = $null
$startedAt = Get-Date
try {
    $existing = Wait-OnPlayerConnect ([datetime]::MinValue) 1
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
        Start-Sleep -Seconds 2
        $firstWorld = $coords.'1280x720'.worlds_first_card
        Click-Relative $proc ([double]$firstWorld.x_ratio) ([double]$firstWorld.y_ratio) "first world card"
        Start-Sleep -Milliseconds 400
        Click-Relative $proc ([double]$firstWorld.x_ratio) ([double]$firstWorld.y_ratio) "first world card confirm"
        $matched = Wait-OnPlayerConnect $startedAt $LoadTimeoutSec
        if (-not $matched) {
            throw "Timed out waiting for [MOTM] >>> onPlayerConnect."
        }
    }

    $proc = Get-HytaleWindow
    if ($proc) {
        Start-Sleep -Seconds 8
        Click-Relative $proc 0.508 0.596 "respawn recovery button"
        Start-Sleep -Seconds 2
        Click-Relative $proc 0.508 0.596 "respawn recovery button confirm"
        Start-Sleep -Seconds 8
    }

    & (Join-Path $PSScriptRoot "capture-evidence.ps1") -Phase "harness/load-world" -RunId $runId -Name "in-world"
    Copy-Item -LiteralPath $matched.Log -Destination (Join-Path $auditDir "server.log") -Force
    $result = "PASS"
    Write-Step "PASS: onPlayerConnect matched in $($matched.Log)"
} finally {
    @"
# H2 load-world run - $runId

## Result
$result

## World
$WorldName

## Log evidence
$($matched.Line)

## Evidence
- in-world.png
- server.log
- load-world.log
"@ | Set-Content -Path $reportPath -Encoding UTF8
}

if ($result -ne "PASS") {
    throw "load-world failed; see $reportPath"
}
