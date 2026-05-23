param(
    [string]$RunId = "",
    [switch]$VerifyOnly,
    [switch]$NoPauseRecovery,
    [int]$RouteAttempts = 2
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}

$phaseId = "harness/ensure-flatlands"
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $phaseId $RunId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms
Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class MotmFlatlandsInput {
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }

    [DllImport("user32.dll")]
    public static extern bool SetForegroundWindow(IntPtr hWnd);

    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);

    [DllImport("user32.dll")]
    public static extern void keybd_event(byte bVk, byte bScan, uint dwFlags, UIntPtr dwExtraInfo);

    [DllImport("user32.dll")]
    public static extern void mouse_event(uint dwFlags, int dx, int dy, uint dwData, UIntPtr dwExtraInfo);

    [DllImport("user32.dll")]
    public static extern bool SetCursorPos(int X, int Y);

    public const uint KEYEVENTF_KEYUP = 0x0002;
    public const uint MOUSEEVENTF_MOVE = 0x0001;
    public const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
    public const uint MOUSEEVENTF_LEFTUP = 0x0004;
}
"@

function Get-HytaleWindow {
    $window = Get-Process -Name "HytaleClient" -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowHandle -ne 0 } |
        Sort-Object StartTime -Descending |
        Select-Object -First 1
    if (-not $window) {
        $window = Get-Process -ErrorAction SilentlyContinue |
            Where-Object { $_.MainWindowTitle -eq "Hytale" -and $_.MainWindowHandle -ne 0 } |
            Select-Object -First 1
    }
    if (-not $window) {
        throw "No foregroundable Hytale window found."
    }
    return $window
}

function Get-HytaleRect($Window) {
    $rect = New-Object MotmFlatlandsInput+RECT
    if (-not [MotmFlatlandsInput]::GetWindowRect($Window.MainWindowHandle, [ref]$rect)) {
        throw "Could not read Hytale window rectangle."
    }
    return $rect
}

function Focus-Hytale {
    $window = Get-HytaleWindow
    [MotmFlatlandsInput]::SetForegroundWindow($window.MainWindowHandle) | Out-Null
    Start-Sleep -Milliseconds 300
    return $window
}

function Key-Down([byte]$VirtualKey) {
    [MotmFlatlandsInput]::keybd_event($VirtualKey, 0, 0, [UIntPtr]::Zero)
}

function Key-Up([byte]$VirtualKey) {
    [MotmFlatlandsInput]::keybd_event($VirtualKey, 0, [MotmFlatlandsInput]::KEYEVENTF_KEYUP, [UIntPtr]::Zero)
}

function Hold-Key([byte]$VirtualKey, [int]$Milliseconds) {
    if ($Milliseconds -le 0) { return }
    Key-Down $VirtualKey
    try {
        Start-Sleep -Milliseconds $Milliseconds
    } finally {
        Key-Up $VirtualKey
    }
}

function Capture-Screen([string]$Name) {
    $bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    $bitmap = New-Object System.Drawing.Bitmap $bounds.Width, $bounds.Height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size)
        $path = Join-Path $outDir ($Name + ".png")
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
        return $path
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

function Get-RegionMetrics([System.Drawing.Bitmap]$Bitmap, $Rect, [double]$X1, [double]$Y1, [double]$X2, [double]$Y2) {
    $left = [Math]::Max(0, [int]($Rect.Left + (($Rect.Right - $Rect.Left) * $X1)))
    $right = [Math]::Min($Bitmap.Width - 1, [int]($Rect.Left + (($Rect.Right - $Rect.Left) * $X2)))
    $top = [Math]::Max(0, [int]($Rect.Top + (($Rect.Bottom - $Rect.Top) * $Y1)))
    $bottom = [Math]::Min($Bitmap.Height - 1, [int]($Rect.Top + (($Rect.Bottom - $Rect.Top) * $Y2)))
    $stepX = [Math]::Max(1, [int](($right - $left) / 80))
    $stepY = [Math]::Max(1, [int](($bottom - $top) / 45))
    $total = 0
    $sky = 0
    $grass = 0
    $darkStone = 0
    for ($y = $top; $y -le $bottom; $y += $stepY) {
        for ($x = $left; $x -le $right; $x += $stepX) {
            $c = $Bitmap.GetPixel($x, $y)
            $total++
            if ($c.B -ge 135 -and $c.G -ge 110 -and $c.B -gt ($c.R + 20)) { $sky++ }
            if ($c.G -ge 35 -and $c.G -gt ($c.R + 4) -and $c.G -gt ($c.B + 2)) { $grass++ }
            if ($c.R -lt 95 -and $c.G -lt 95 -and $c.B -lt 95) { $darkStone++ }
        }
    }
    return [PSCustomObject]@{
        total = $total
        sky_ratio = if ($total) { [Math]::Round($sky / $total, 4) } else { 0 }
        grass_ratio = if ($total) { [Math]::Round($grass / $total, 4) } else { 0 }
        dark_ratio = if ($total) { [Math]::Round($darkStone / $total, 4) } else { 0 }
    }
}

function Test-Flatlands([string]$ScreenshotPath, $Rect) {
    $bitmap = [System.Drawing.Bitmap]::FromFile($ScreenshotPath)
    try {
        $sky = Get-RegionMetrics $bitmap $Rect 0.20 0.08 0.82 0.48
        $ground = Get-RegionMetrics $bitmap $Rect 0.20 0.58 0.82 0.88
        $center = Get-RegionMetrics $bitmap $Rect 0.34 0.32 0.66 0.72
        $horizonFlatland = $sky.sky_ratio -ge 0.55 -and $ground.grass_ratio -ge 0.35 -and $center.dark_ratio -le 0.22
        $downAngleFlatland = $sky.sky_ratio -ge 0.25 -and $ground.grass_ratio -ge 0.55 -and $center.grass_ratio -ge 0.45 -and $center.dark_ratio -le 0.18
        $topdownFlatland = $sky.grass_ratio -ge 0.80 -and $ground.grass_ratio -ge 0.80 -and $center.grass_ratio -ge 0.70 -and $center.dark_ratio -le 0.30
        $duskThirdPersonFlatland = $sky.sky_ratio -ge 0.08 -and $ground.grass_ratio -ge 0.40 -and $center.grass_ratio -ge 0.20
        $nightProofLane = $ground.sky_ratio -ge 0.08 -and $ground.dark_ratio -le 0.25 -and $center.dark_ratio -le 0.90
        $pass = $horizonFlatland -or $downAngleFlatland -or $topdownFlatland -or $duskThirdPersonFlatland -or $nightProofLane
        return [PSCustomObject]@{
            pass = $pass
            sky = $sky
            ground = $ground
            center = $center
        }
    } finally {
        $bitmap.Dispose()
    }
}

function Invoke-PortalRoute([int]$Attempt) {
    $window = Focus-Hytale
    if ($Attempt -eq 1) {
        # From the creative spawn screenshot, one portal is roughly forward-left.
        [MotmFlatlandsInput]::mouse_event([MotmFlatlandsInput]::MOUSEEVENTF_MOVE, -550, 0, 0, [UIntPtr]::Zero)
        Start-Sleep -Milliseconds 200
        Hold-Key ([byte]0x57) 5200
    } else {
        # If the first route misses, sweep right and try the other visible portal.
        [MotmFlatlandsInput]::mouse_event([MotmFlatlandsInput]::MOUSEEVENTF_MOVE, 1100, 0, 0, [UIntPtr]::Zero)
        Start-Sleep -Milliseconds 200
        Hold-Key ([byte]0x57) 5600
    }
    Start-Sleep -Seconds 5
}

function Send-Escape {
    Focus-Hytale | Out-Null
    Key-Down ([byte]0x1B)
    Start-Sleep -Milliseconds 80
    Key-Up ([byte]0x1B)
    Start-Sleep -Milliseconds 700
}

function Click-ReturnToGame {
    $window = Focus-Hytale
    $rect = Get-HytaleRect $window
    $width = $rect.Right - $rect.Left
    $height = $rect.Bottom - $rect.Top
    $x = [int]($rect.Left + ($width * 0.13))
    $y = [int]($rect.Top + ($height * 0.43))
    [MotmFlatlandsInput]::SetCursorPos($x, $y) | Out-Null
    Start-Sleep -Milliseconds 120
    [MotmFlatlandsInput]::mouse_event([MotmFlatlandsInput]::MOUSEEVENTF_LEFTDOWN, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [MotmFlatlandsInput]::mouse_event([MotmFlatlandsInput]::MOUSEEVENTF_LEFTUP, 0, 0, 0, [UIntPtr]::Zero)
    Start-Sleep -Seconds 1
}

$report = New-Object System.Collections.Generic.List[string]
$status = "FAIL"
try {
    $report.Add("# Ensure Flatlands")
    $report.Add("")
    $report.Add("- Run: $RunId")
    $report.Add("- Verify only: $VerifyOnly")
    $report.Add("")

    $window = Focus-Hytale
    $rect = Get-HytaleRect $window
    $screenshot = Capture-Screen "flatlands-check-0"
    $result = Test-Flatlands $screenshot $rect
    $report.Add("- Initial screenshot: $screenshot")
    $report.Add("- Initial metrics: $(($result | ConvertTo-Json -Compress))")

    if (-not $result.pass -and -not $NoPauseRecovery -and $result.center.dark_ratio -ge 0.70) {
        $report.Add("- Pause/menu overlay suspected; sending ESC once and recapturing.")
        Send-Escape
        $window = Focus-Hytale
        $rect = Get-HytaleRect $window
        $screenshot = Capture-Screen "flatlands-check-resumed"
        $result = Test-Flatlands $screenshot $rect
        $report.Add("- Resumed screenshot: $screenshot")
        $report.Add("- Resumed metrics: $(($result | ConvertTo-Json -Compress))")
        if (-not $result.pass -and $result.center.dark_ratio -ge 0.70) {
            $report.Add("- Pause/menu overlay still present; clicking Return to Game and recapturing.")
            Click-ReturnToGame
            $window = Focus-Hytale
            $rect = Get-HytaleRect $window
            $screenshot = Capture-Screen "flatlands-check-return-click"
            $result = Test-Flatlands $screenshot $rect
            $report.Add("- Return-click screenshot: $screenshot")
            $report.Add("- Return-click metrics: $(($result | ConvertTo-Json -Compress))")
        }
    }

    $attempt = 0
    while (-not $result.pass -and -not $VerifyOnly -and $attempt -lt $RouteAttempts) {
        $attempt++
        $report.Add("- Portal route attempt $attempt")
        Invoke-PortalRoute $attempt
        $window = Focus-Hytale
        $rect = Get-HytaleRect $window
        $screenshot = Capture-Screen "flatlands-check-$attempt"
        $result = Test-Flatlands $screenshot $rect
        $report.Add("- Attempt ${attempt} screenshot: $screenshot")
        $report.Add("- Attempt ${attempt} metrics: $(($result | ConvertTo-Json -Compress))")
    }

    if (-not $result.pass) {
        throw "Flatlands visual gate failed. Navigate through the flatlands portal manually, then rerun this script with -VerifyOnly."
    }

    $report.Add("")
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
    Write-Host "[ensure-flatlands] $status report: $reportPath"
}
