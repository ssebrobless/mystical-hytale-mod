param(
    [string]$Phase = "manual",
    [string]$RunId,
    [string]$Name = "screen",
    [switch]$Video,
    [int]$VideoSeconds = 30,
    [switch]$WindowOnly,
    [string]$WindowTitle = "Hytale"
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

Add-Type @"
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
}
"@

function Get-CaptureBounds {
    if (-not $WindowOnly) {
        return [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    }

    $window = Get-Process -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowTitle -eq $WindowTitle -and $_.MainWindowHandle -ne 0 } |
        Select-Object -First 1
    if (-not $window) {
        Write-Warning "[capture-evidence] Window '$WindowTitle' not found; falling back to full desktop."
        return [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    }

    $rect = New-Object MotmCaptureWin32+RECT
    if (-not [MotmCaptureWin32]::GetWindowRect($window.MainWindowHandle, [ref]$rect)) {
        Write-Warning "[capture-evidence] Could not read window bounds for '$WindowTitle'; falling back to full desktop."
        return [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    }

    $width = [Math]::Max(1, $rect.Right - $rect.Left)
    $height = [Math]::Max(1, $rect.Bottom - $rect.Top)
    return New-Object System.Drawing.Rectangle $rect.Left, $rect.Top, $width, $height
}

$bounds = Get-CaptureBounds
$bitmap = New-Object System.Drawing.Bitmap $bounds.Width, $bounds.Height
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
try {
    $graphics.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size)
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
        $inputName = "desktop"
        if ($WindowOnly) {
            $inputName = "title=$WindowTitle"
        }
        & $ffmpeg.Source -y -f gdigrab -framerate 30 -i $inputName -t $VideoSeconds -c:v libx264 -preset ultrafast -pix_fmt yuv420p $videoPath
        Write-Host "[capture-evidence] Video: $videoPath"
    } else {
        Write-Warning "[capture-evidence] ffmpeg not found; video skipped."
    }
}
