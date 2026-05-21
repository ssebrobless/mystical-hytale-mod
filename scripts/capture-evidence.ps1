param(
    [string]$Phase = "manual",
    [string]$RunId,
    [string]$Name = "screen",
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
$bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
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
        & $ffmpeg.Source -y -f gdigrab -framerate 30 -i desktop -t $VideoSeconds -c:v libx264 -preset ultrafast $videoPath
        Write-Host "[capture-evidence] Video: $videoPath"
    } else {
        Write-Warning "[capture-evidence] ffmpeg not found; video skipped."
    }
}
