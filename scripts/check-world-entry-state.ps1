param(
    [string]$RunId = "",
    [switch]$NoThrow
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}

$phaseId = "harness/world-entry-state"
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $phaseId $RunId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms
Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class MotmEntryStateWin32 {
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
    $rect = New-Object MotmEntryStateWin32+RECT
    if (-not [MotmEntryStateWin32]::GetWindowRect($Window.MainWindowHandle, [ref]$rect)) {
        throw "Could not read Hytale window rectangle."
    }
    return $rect
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
    $stepX = [Math]::Max(1, [int](($right - $left) / 90))
    $stepY = [Math]::Max(1, [int](($bottom - $top) / 50))
    $total = 0
    $sky = 0
    $grass = 0
    $dark = 0
    $whiteText = 0
    for ($y = $top; $y -le $bottom; $y += $stepY) {
        for ($x = $left; $x -le $right; $x += $stepX) {
            $c = $Bitmap.GetPixel($x, $y)
            $total++
            if ($c.B -ge 135 -and $c.G -ge 110 -and $c.B -gt ($c.R + 20)) { $sky++ }
            if ($c.G -ge 35 -and $c.G -gt ($c.R + 4) -and $c.G -gt ($c.B + 2)) { $grass++ }
            if ($c.R -lt 75 -and $c.G -lt 75 -and $c.B -lt 75) { $dark++ }
            if ($c.R -gt 190 -and $c.G -gt 190 -and $c.B -gt 190) { $whiteText++ }
        }
    }
    return [PSCustomObject]@{
        total = $total
        sky_ratio = if ($total) { [Math]::Round($sky / $total, 4) } else { 0 }
        grass_ratio = if ($total) { [Math]::Round($grass / $total, 4) } else { 0 }
        dark_ratio = if ($total) { [Math]::Round($dark / $total, 4) } else { 0 }
        white_ratio = if ($total) { [Math]::Round($whiteText / $total, 4) } else { 0 }
    }
}

$report = New-Object System.Collections.Generic.List[string]
$status = "FAIL"
try {
    $window = Get-HytaleWindow
    [MotmEntryStateWin32]::SetForegroundWindow($window.MainWindowHandle) | Out-Null
    Start-Sleep -Milliseconds 300
    $rect = Get-HytaleRect $window
    $screenshot = Capture-Screen "entry-state"
    $bitmap = [System.Drawing.Bitmap]::FromFile($screenshot)
    try {
        $center = Get-RegionMetrics $bitmap $rect 0.30 0.28 0.70 0.72
        $lower = Get-RegionMetrics $bitmap $rect 0.25 0.58 0.85 0.92
        $upper = Get-RegionMetrics $bitmap $rect 0.18 0.06 0.86 0.45
    } finally {
        $bitmap.Dispose()
    }

    $looksLikeWorld = ($upper.sky_ratio -ge 0.08 -or $lower.grass_ratio -ge 0.25 -or $center.grass_ratio -ge 0.18)
    $darkOverlay = $center.dark_ratio -ge 0.62 -and $center.white_ratio -ge 0.005
    $voidRisk = -not $looksLikeWorld -and $center.dark_ratio -ge 0.45

    $report.Add("# World Entry State")
    $report.Add("")
    $report.Add("- Run: $RunId")
    $report.Add("- Screenshot: $screenshot")
    $report.Add("- Center: $(($center | ConvertTo-Json -Compress))")
    $report.Add("- Lower: $(($lower | ConvertTo-Json -Compress))")
    $report.Add("- Upper: $(($upper | ConvertTo-Json -Compress))")
    $report.Add("- Looks like world: $looksLikeWorld")
    $report.Add("- Dark overlay/death-menu risk: $darkOverlay")
    $report.Add("- Void/loading risk: $voidRisk")

    if ($darkOverlay -or $voidRisk) {
        throw "Entry-state screenshot looks unsafe; respawn/menu/void recovery may be needed before typing commands."
    }

    $report.Add("")
    $report.Add("PASS")
    $status = "PASS"
} catch {
    $report.Add("")
    $report.Add("FAIL")
    $report.Add("")
    $report.Add("Error: $($_.Exception.Message)")
    if (-not $NoThrow) {
        throw
    }
} finally {
    $reportPath = Join-Path $outDir "report.md"
    $report | Set-Content -LiteralPath $reportPath -Encoding UTF8
    Write-Host "[check-world-entry-state] $status report: $reportPath"
}
