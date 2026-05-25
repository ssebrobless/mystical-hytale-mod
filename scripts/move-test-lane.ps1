param(
    [int]$ForwardMilliseconds = 4500,
    [int]$StrafeMilliseconds = 900,
    [ValidateSet("W", "S")]
    [string]$ForwardKey = "W",
    [ValidateSet("A", "D")]
    [string]$StrafeKey = "D",
    [int]$LookDx = 0,
    [int]$LookDy = 0,
    [string]$RunId = "",
    [switch]$SkipScreenshot
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}

$phaseId = "harness/move-test-lane"
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $phaseId $RunId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

Add-Type -AssemblyName System.Windows.Forms

Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class MotmMoveInput {
    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);

    [DllImport("user32.dll")]
    public static extern void mouse_event(uint dwFlags, int dx, int dy, uint dwData, UIntPtr dwExtraInfo);

    public const uint KEYEVENTF_KEYUP = 0x0002;
    public const uint MOUSEEVENTF_MOVE = 0x0001;
}
"@

function Focus-Hytale {
    $window = Get-Process -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowTitle -eq "Hytale" -and $_.MainWindowHandle -ne 0 } |
        Select-Object -First 1
    if (-not $window) {
        throw "No foregroundable Hytale window found. Load Hytale into the world first."
    }

    [MotmMoveInput]::SetForegroundWindow($window.MainWindowHandle) | Out-Null
    Start-Sleep -Milliseconds 250
    return $window
}

function Key-Down([byte]$VirtualKey) {
    [MotmMoveInput]::keybd_event($VirtualKey, 0, 0, [UIntPtr]::Zero)
}

function Key-Up([byte]$VirtualKey) {
    [MotmMoveInput]::keybd_event($VirtualKey, 0, [MotmMoveInput]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
}

function Hold-Key([byte]$VirtualKey, [int]$Milliseconds) {
    if ($Milliseconds -le 0) {
        return
    }

    Key-Down $VirtualKey
    try {
        Start-Sleep -Milliseconds $Milliseconds
    } finally {
        Key-Up $VirtualKey
    }
}

$report = New-Object System.Collections.Generic.List[string]
$status = "FAIL"

try {
    $window = Focus-Hytale
    $report.Add("# Move Test Lane")
    $report.Add("")
    $report.Add("- Run: $RunId")
    $report.Add("- Hytale PID: $($window.Id)")
    $report.Add("- Forward milliseconds: $ForwardMilliseconds")
    $report.Add("- Strafe milliseconds: $StrafeMilliseconds")
    $report.Add("- Forward key: $ForwardKey")
    $report.Add("- Strafe key: $StrafeKey")
    $report.Add("- Look delta: $LookDx,$LookDy")
    $report.Add("")

    if ($LookDx -ne 0 -or $LookDy -ne 0) {
        [MotmMoveInput]::mouse_event([MotmMoveInput]::MOUSEEVENTF_MOVE, $LookDx, $LookDy, 0, [UIntPtr]::Zero)
        Start-Sleep -Milliseconds 150
    }

    $forwardVk = if ($ForwardKey -eq "S") { [byte]0x53 } else { [byte]0x57 }
    $strafeVk = if ($StrafeKey -eq "A") { [byte]0x41 } else { [byte]0x44 }

    # Move in a shallow dogleg so the player leaves portal/spawn clutter instead of walking into one fixed obstacle.
    Hold-Key $forwardVk $ForwardMilliseconds
    Hold-Key $strafeVk $StrafeMilliseconds
    Hold-Key $forwardVk ([Math]::Max(800, [int]($ForwardMilliseconds / 3)))

    if (-not $SkipScreenshot) {
        & (Join-Path $PSScriptRoot "capture-evidence.ps1") -Phase $phaseId -RunId $RunId -Name "after-move" |
            Tee-Object -FilePath (Join-Path $outDir "capture.txt")
    }

    $report.Add("PASS")
    $status = "PASS"
} catch {
    $report.Add("")
    $report.Add("FAIL")
    $report.Add("")
    $report.Add("Error: $($_.Exception.Message)")
    throw
} finally {
    $reportPath = Join-Path $outDir "report.md"
    $report | Set-Content -LiteralPath $reportPath -Encoding UTF8
    Write-Host "[move-test-lane] $status report: $reportPath"
}
