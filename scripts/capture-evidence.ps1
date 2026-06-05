param(
    [string]$Phase = "manual",
    [string]$RunId,
    [string]$Name = "screen",
    [string]$WindowTitle,
    [switch]$Video,
    [int]$VideoSeconds = 30
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $RunId) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $Phase $RunId))
if (-not (Test-Path -LiteralPath $outDir)) {
    New-Item -ItemType Directory -Path $outDir -Force | Out-Null
}

Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing

Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;

public static class MotmCaptureWin32 {
    [StructLayout(LayoutKind.Sequential)]
    public struct RECT {
        public int Left;
        public int Top;
        public int Right;
        public int Bottom;
    }

    [DllImport("user32.dll")]
    public static extern bool GetWindowRect(IntPtr hWnd, out RECT rect);

    [DllImport("user32.dll")]
    public static extern bool PrintWindow(IntPtr hWnd, IntPtr hdcBlt, int nFlags);
}
"@

$window = $null
if ($WindowTitle) {
    $window = Get-Process -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowTitle -eq $WindowTitle -and $_.MainWindowHandle -ne 0 } |
        Select-Object -First 1
    if (-not $window) {
        throw "No window found with title '$WindowTitle'."
    }
}

if ($window) {
    $rect = New-Object MotmCaptureWin32+RECT
    if (-not [MotmCaptureWin32]::GetWindowRect($window.MainWindowHandle, [ref]$rect)) {
        throw "Could not read window bounds for '$WindowTitle'."
    }
    $width = [Math]::Max(1, $rect.Right - $rect.Left)
    $height = [Math]::Max(1, $rect.Bottom - $rect.Top)
    $bitmap = New-Object System.Drawing.Bitmap $width, $height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
} else {
    $bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    $bitmap = New-Object System.Drawing.Bitmap $bounds.Width, $bounds.Height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
}

try {
    if ($window) {
        $hdc = $graphics.GetHdc()
        try {
            $printed = [MotmCaptureWin32]::PrintWindow($window.MainWindowHandle, $hdc, 0)
        } finally {
            $graphics.ReleaseHdc($hdc)
        }
        if (-not $printed) {
            $graphics.CopyFromScreen(
                [System.Drawing.Point]::new($rect.Left, $rect.Top),
                [System.Drawing.Point]::Empty,
                [System.Drawing.Size]::new($width, $height))
        }
    } else {
        $graphics.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size)
    }
    $path = Join-Path $outDir ($Name + ".png")
    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    Write-Host "[capture-evidence] Screenshot: $path"
} finally {
    $graphics.Dispose()
    $bitmap.Dispose()
}

if ($Video) {
    $ffmpeg = Get-Command ffmpeg.exe -ErrorAction SilentlyContinue
    if (-not $ffmpeg) {
        $candidate = Join-Path $repoRoot ".tools\ffmpeg\bin\ffmpeg.exe"
        if (Test-Path -LiteralPath $candidate) {
            $ffmpeg = Get-Item -LiteralPath $candidate
        }
    }
    if ($ffmpeg) {
        $videoPath = Join-Path $outDir "run.mp4"
        $inputTarget = if ($WindowTitle) { "title=$WindowTitle" } else { "desktop" }
        & $ffmpeg.Source -y -f gdigrab -framerate 30 -i $inputTarget -t $VideoSeconds -c:v libx264 -pix_fmt yuv420p -movflags +faststart -preset ultrafast $videoPath
        Write-Host "[capture-evidence] Video: $videoPath"
    } else {
        Write-Warning "[capture-evidence] ffmpeg not found; video skipped."
    }
}
