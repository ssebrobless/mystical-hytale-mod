param(
    [Parameter(Mandatory)]
    [ValidateSet("avg-rgb", "variance", "palette-match")]
    [string]$Mode,

    [Parameter(Mandatory)] [int]$X,
    [Parameter(Mandatory)] [int]$Y,
    [int]$Width = 20,
    [int]$Height = 20,
    [string]$Expected,
    [int]$ToleranceRgb = 25,
    [string]$ScreenshotPath,
    [string]$Phase = "harness/visual-validate",
    [string]$RunId,
    [switch]$RequireDarkAtOrBelow
)

$ErrorActionPreference = "Stop"

if ($Width -le 0 -or $Height -le 0) {
    throw "Width and Height must be positive."
}

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $RunId) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $Phase $RunId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

Add-Type -AssemblyName System.Drawing
Add-Type -AssemblyName System.Windows.Forms

function Convert-HexColor([string]$Hex) {
    if ([string]::IsNullOrWhiteSpace($Hex)) {
        return $null
    }
    $value = $Hex.Trim()
    if ($value.StartsWith("#")) {
        $value = $value.Substring(1)
    }
    if ($value.Length -ne 6) {
        throw "Expected color must be #RRGGBB."
    }

    return [PSCustomObject]@{
        R = [Convert]::ToInt32($value.Substring(0, 2), 16)
        G = [Convert]::ToInt32($value.Substring(2, 2), 16)
        B = [Convert]::ToInt32($value.Substring(4, 2), 16)
    }
}

function Get-PaletteColor([string]$Name) {
    $palettes = @{
        "terra" = "#8A5A2B"
        "quake" = "#5A351F"
        "metal" = "#8E989F"
        "hydro" = "#3A86C8"
        "icicle" = "#A9E4FF"
        "snow" = "#E7F5FF"
        "aero" = "#DCE8D6"
        "wind_blade" = "#B9D7C6"
        "thunder" = "#E7D04A"
        "corruptus" = "#7A2D9A"
        "flame" = "#E25A24"
        "buried" = "#282828"
    }
    $key = if ($Name) { $Name.Trim().ToLowerInvariant() } else { "" }
    if (-not $palettes.ContainsKey($key)) {
        throw "Unknown palette '$Name'. Use a known style/class key or #RRGGBB."
    }
    return Convert-HexColor $palettes[$key]
}

function Capture-ScreenBitmap {
    $bounds = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    $bitmap = New-Object System.Drawing.Bitmap $bounds.Width, $bounds.Height
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    try {
        $graphics.CopyFromScreen($bounds.Location, [System.Drawing.Point]::Empty, $bounds.Size)
        return $bitmap
    } finally {
        $graphics.Dispose()
    }
}

if ($ScreenshotPath) {
    if (-not (Test-Path -LiteralPath $ScreenshotPath)) {
        throw "ScreenshotPath not found: $ScreenshotPath"
    }
    $bitmap = [System.Drawing.Bitmap]::FromFile((Resolve-Path $ScreenshotPath))
} else {
    $bitmap = Capture-ScreenBitmap
}

try {
    if ($X -lt 0 -or $Y -lt 0 -or ($X + $Width) -gt $bitmap.Width -or ($Y + $Height) -gt $bitmap.Height) {
        throw "Region ${X},${Y} ${Width}x${Height} is outside screenshot ${($bitmap.Width)}x${($bitmap.Height)}."
    }

    $crop = New-Object System.Drawing.Bitmap $Width, $Height
    $graphics = [System.Drawing.Graphics]::FromImage($crop)
    try {
        $graphics.DrawImage($bitmap, 0, 0, (New-Object System.Drawing.Rectangle $X, $Y, $Width, $Height), [System.Drawing.GraphicsUnit]::Pixel)
        $cropPath = Join-Path $outDir ("visual-{0}x{1}-{2}x{3}.png" -f $X, $Y, $Width, $Height)
        $crop.Save($cropPath, [System.Drawing.Imaging.ImageFormat]::Png)
    } finally {
        $graphics.Dispose()
        $crop.Dispose()
    }

    [double]$sumR = 0
    [double]$sumG = 0
    [double]$sumB = 0
    [double[]]$rs = New-Object double[] ($Width * $Height)
    [double[]]$gs = New-Object double[] ($Width * $Height)
    [double[]]$bs = New-Object double[] ($Width * $Height)
    $i = 0
    for ($yy = $Y; $yy -lt ($Y + $Height); $yy++) {
        for ($xx = $X; $xx -lt ($X + $Width); $xx++) {
            $pixel = $bitmap.GetPixel($xx, $yy)
            $sumR += $pixel.R
            $sumG += $pixel.G
            $sumB += $pixel.B
            $rs[$i] = $pixel.R
            $gs[$i] = $pixel.G
            $bs[$i] = $pixel.B
            $i++
        }
    }

    $count = [double]($Width * $Height)
    $avgR = $sumR / $count
    $avgG = $sumG / $count
    $avgB = $sumB / $count

    function Get-StDev([double[]]$Values, [double]$Average) {
        [double]$sum = 0
        foreach ($value in $Values) {
            $delta = $value - $Average
            $sum += $delta * $delta
        }
        return [Math]::Sqrt($sum / [Math]::Max(1, $Values.Length))
    }

    $stdevR = Get-StDev $rs $avgR
    $stdevG = Get-StDev $gs $avgG
    $stdevB = Get-StDev $bs $avgB
    $overallVarianceMetric = ($stdevR + $stdevG + $stdevB) / 3.0

    $pass = $false
    $expectedColor = $null
    $threshold = 0.0

    switch ($Mode) {
        "avg-rgb" {
            $expectedColor = Convert-HexColor $Expected
            if ($null -eq $expectedColor) {
                throw "Expected #RRGGBB is required for avg-rgb."
            }
            if ($RequireDarkAtOrBelow) {
                $pass = ($avgR -le ($expectedColor.R + $ToleranceRgb)) -and
                    ($avgG -le ($expectedColor.G + $ToleranceRgb)) -and
                    ($avgB -le ($expectedColor.B + $ToleranceRgb))
            } else {
                $pass = ([Math]::Abs($avgR - $expectedColor.R) -le $ToleranceRgb) -and
                    ([Math]::Abs($avgG - $expectedColor.G) -le $ToleranceRgb) -and
                    ([Math]::Abs($avgB - $expectedColor.B) -le $ToleranceRgb)
            }
        }
        "variance" {
            if ([string]::IsNullOrWhiteSpace($Expected)) {
                throw "Expected numeric threshold is required for variance."
            }
            $threshold = [double]::Parse($Expected, [Globalization.CultureInfo]::InvariantCulture)
            $pass = $overallVarianceMetric -ge $threshold
        }
        "palette-match" {
            if ([string]::IsNullOrWhiteSpace($Expected)) {
                throw "Expected palette name or #RRGGBB is required for palette-match."
            }
            if ($Expected.Trim().StartsWith("#")) {
                $expectedColor = Convert-HexColor $Expected
            } else {
                $expectedColor = Get-PaletteColor $Expected
            }
            $pass = ([Math]::Abs($avgR - $expectedColor.R) -le $ToleranceRgb) -and
                ([Math]::Abs($avgG - $expectedColor.G) -le $ToleranceRgb) -and
                ([Math]::Abs($avgB - $expectedColor.B) -le $ToleranceRgb)
        }
    }

    $metric = [PSCustomObject]@{
        mode = $Mode
        pass = $pass
        screenshotPath = $ScreenshotPath
        cropPath = $cropPath
        region = [PSCustomObject]@{ x = $X; y = $Y; width = $Width; height = $Height }
        avgRgb = [PSCustomObject]@{
            r = [Math]::Round($avgR, 2)
            g = [Math]::Round($avgG, 2)
            b = [Math]::Round($avgB, 2)
        }
        stdevRgb = [PSCustomObject]@{
            r = [Math]::Round($stdevR, 2)
            g = [Math]::Round($stdevG, 2)
            b = [Math]::Round($stdevB, 2)
            mean = [Math]::Round($overallVarianceMetric, 2)
        }
        expected = $Expected
        toleranceRgb = $ToleranceRgb
        requireDarkAtOrBelow = [bool]$RequireDarkAtOrBelow
    }

    $metricPath = Join-Path $outDir "metric.json"
    $metric | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $metricPath -Encoding UTF8
    Write-Host "[visual-validate] mode=$Mode pass=$pass avg=($([Math]::Round($avgR, 1)),$([Math]::Round($avgG, 1)),$([Math]::Round($avgB, 1))) stdevMean=$([Math]::Round($overallVarianceMetric, 1))"
    Write-Host "[visual-validate] crop=$cropPath"
    Write-Host "[visual-validate] metric=$metricPath"

    if (-not $pass) {
        throw "visual validation failed for mode=$Mode. See $metricPath"
    }
} finally {
    $bitmap.Dispose()
}
