param(
    [string]$Phase = "manual",
    [string]$RunId,
    [string]$Name = "camera-check",
    [switch]$TryToggle,
    [double]$PassThreshold = 0.025
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $RunId) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $Phase $RunId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms

Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class MotmCameraCheckWin32 {
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

function Get-HytaleBounds {
    $window = Get-Process -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowTitle -eq "Hytale" -and $_.MainWindowHandle -ne 0 } |
        Select-Object -First 1
    if (-not $window) {
        throw "No Hytale window found for third-person verification."
    }

    $rect = New-Object MotmCameraCheckWin32+RECT
    if (-not [MotmCameraCheckWin32]::GetWindowRect($window.MainWindowHandle, [ref]$rect)) {
        throw "Could not read Hytale window bounds."
    }
    $width = [Math]::Max(1, $rect.Right - $rect.Left)
    $height = [Math]::Max(1, $rect.Bottom - $rect.Top)
    return New-Object System.Drawing.Rectangle $rect.Left, $rect.Top, $width, $height
}

function Capture-HytaleWindow([string]$Suffix) {
    $bounds = Get-HytaleBounds
    $bitmap = New-Object System.Drawing.Bitmap $bounds.Width, $bounds.Height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size)
        $path = Join-Path $outDir ($Name + "-" + $Suffix + ".png")
        $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
        return [pscustomobject]@{
            Path = $path
            Bitmap = $bitmap.Clone()
        }
    } finally {
        $graphics.Dispose()
        $bitmap.Dispose()
    }
}

function Get-ModelVisibilityScore([System.Drawing.Bitmap]$Bitmap) {
    $x0 = [int]($Bitmap.Width * 0.38)
    $x1 = [int]($Bitmap.Width * 0.62)
    $y0 = [int]($Bitmap.Height * 0.43)
    $y1 = [int]($Bitmap.Height * 0.82)
    $samples = 0
    $modelish = 0

    for ($y = $y0; $y -lt $y1; $y += 4) {
        for ($x = $x0; $x -lt $x1; $x += 4) {
            $pixel = $Bitmap.GetPixel($x, $y)
            $max = [Math]::Max($pixel.R, [Math]::Max($pixel.G, $pixel.B))
            $min = [Math]::Min($pixel.R, [Math]::Min($pixel.G, $pixel.B))
            $brightness = ($pixel.R + $pixel.G + $pixel.B) / 3.0
            $saturation = if ($max -eq 0) { 0.0 } else { ($max - $min) / [double]$max }
            $isSkyOrGrass = ($pixel.B -gt 135 -and $pixel.G -gt 120 -and $pixel.R -lt 150) -or
                ($pixel.G -gt 95 -and $pixel.R -lt 130 -and $pixel.B -lt 115)
            if ((($brightness -lt 92) -or ($saturation -gt 0.48 -and $brightness -lt 180)) -and -not $isSkyOrGrass) {
                $modelish++
            }
            $samples++
        }
    }

    if ($samples -eq 0) {
        return 0.0
    }
    return $modelish / [double]$samples
}

function New-CheckResult([string]$Status, [double]$Score, [string]$Image, [string]$Action) {
    return [pscustomobject]@{
        status = $Status
        score = [Math]::Round($Score, 4)
        threshold = $PassThreshold
        image = $Image
        action = $Action
    }
}

$before = Capture-HytaleWindow "before"
try {
    $beforeScore = Get-ModelVisibilityScore $before.Bitmap
} finally {
    $before.Bitmap.Dispose()
}

if ($beforeScore -ge $PassThreshold) {
    $result = New-CheckResult "PASS" $beforeScore $before.Path "kept"
} elseif ($TryToggle) {
    $bestScore = $beforeScore
    $bestPath = $before.Path
    $bestAction = "kept"

    for ($attempt = 1; $attempt -le 3; $attempt++) {
        & (Join-Path $PSScriptRoot "send-input.ps1") -Action ThirdPerson -DelayMilliseconds 450 | Out-Null
        Start-Sleep -Milliseconds 450
        $after = Capture-HytaleWindow ("after-toggle-" + $attempt)
        try {
            $afterScore = Get-ModelVisibilityScore $after.Bitmap
        } finally {
            $after.Bitmap.Dispose()
        }

        if ($afterScore -gt $bestScore) {
            $bestScore = $afterScore
            $bestPath = $after.Path
            $bestAction = "toggled-$attempt"
        }
        if ($afterScore -ge $PassThreshold) {
            $result = New-CheckResult "PASS" $afterScore $after.Path ("toggled-$attempt")
            break
        }
    }

    if ($null -eq $result) {
        $status = if ($bestScore -ge ($PassThreshold * 0.65)) { "REVIEW" } else { "FAIL" }
        $result = New-CheckResult $status $bestScore $bestPath $bestAction
    }
} else {
    $status = if ($beforeScore -ge ($PassThreshold * 0.65)) { "REVIEW" } else { "FAIL" }
    $result = New-CheckResult $status $beforeScore $before.Path "unchecked"
}

$line = "THIRD_PERSON={0} score={1} threshold={2} action={3} image={4}" -f `
    $result.status, $result.score, $result.threshold, $result.action, $result.image
Write-Output $line
$result | ConvertTo-Json -Depth 3 | Set-Content -LiteralPath (Join-Path $outDir ($Name + ".json")) -Encoding UTF8
