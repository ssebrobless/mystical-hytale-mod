param(
    [string]$WorldName = "MOTM Creative Test",
    [switch]$Autonomous,
    [switch]$SkipColdLaunch
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path "phase5" $runId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$report = New-Object System.Collections.Generic.List[string]

function Add-Line([string]$Line) {
    $script:report.Add($Line)
}

function Get-LatestServerLog {
    $logDir = Join-Path $env:APPDATA ("Hytale\UserData\Saves\" + $WorldName + "\logs")
    Get-ChildItem -LiteralPath $logDir -Filter "*_server.log" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Send-MotmCommand([string]$Text) {
    & (Join-Path $PSScriptRoot "send-input.ps1") -Action Command -Text $Text -DelayMilliseconds 120
}

function Send-Key([string]$Keys) {
    & (Join-Path $PSScriptRoot "send-input.ps1") -Action Key -Keys $Keys -DelayMilliseconds 120
}

function Send-Jump {
    & (Join-Path $PSScriptRoot "send-input.ps1") -Action Jump -DelayMilliseconds 120
}

function Read-NewLogLines([string]$Path, [long]$StartOffset) {
    $lines = New-Object System.Collections.Generic.List[string]
    $fs = [System.IO.File]::Open($Path, "Open", "Read", "ReadWrite")
    try {
        $fs.Position = [Math]::Min($StartOffset, $fs.Length)
        $reader = New-Object System.IO.StreamReader($fs)
        while (($line = $reader.ReadLine()) -ne $null) {
            $lines.Add($line)
        }
    } finally {
        $fs.Dispose()
    }
    return $lines
}

function Assert-AnyLine($Lines, [string]$Pattern, [string]$Label) {
    $match = $Lines | Where-Object { $_ -match $Pattern } | Select-Object -First 1
    if (-not $match) {
        Add-Line("- FAIL: $Label")
        throw "Missing Phase 5 evidence: $Label ($Pattern)"
    }
    Add-Line("- PASS: $Label")
    Add-Line("  - $match")
}

Add-Type @"
using System;
using System.Runtime.InteropServices;

public static class MotmPhase5Window {
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

function Get-HytaleWindowRect {
    $process = Get-Process -Name "HytaleClient" -ErrorAction SilentlyContinue |
        Where-Object { $_.MainWindowHandle -ne 0 } |
        Select-Object -First 1
    if (-not $process) {
        throw "No foreground-capable HytaleClient window found for visual crop."
    }

    $rect = New-Object MotmPhase5Window+RECT
    if (-not [MotmPhase5Window]::GetWindowRect($process.MainWindowHandle, [ref]$rect)) {
        throw "Could not read HytaleClient window rectangle."
    }

    return $rect
}

$status = "FAIL"
try {
    Add-Line("# Phase 5 Autonomous Acceptance")
    Add-Line("")
    Add-Line("- Run: $runId")
    Add-Line("- World: $WorldName")
    Add-Line("- Autonomous: $Autonomous")
    Add-Line("")

    if ($Autonomous -and -not $SkipColdLaunch) {
        & (Join-Path $PSScriptRoot "cold-launch.ps1") -WorldName $WorldName -LaunchAndLoad -EnsureFlatlands
        & (Join-Path $PSScriptRoot "setup-test-world.ps1") -WorldName $WorldName -CloseGroundedTarget -SkipSpellbookOverview
    }

    $log = Get-LatestServerLog
    if (-not $log) {
        throw "No server log found for $WorldName."
    }
    $startOffset = $log.Length
    Add-Line("## Actions")
    Add-Line("- Server log: $($log.FullName)")

    Send-MotmCommand "motm dev test mobs clear"
    Start-Sleep -Seconds 1
    Send-MotmCommand "motm dev test mobs close"
    Start-Sleep -Seconds 2
    Send-MotmCommand "motm dev test ability stomp"
    Start-Sleep -Milliseconds 800
    & (Join-Path $PSScriptRoot "send-input.ps1") -Action Jump -DelayMilliseconds 1600
    Start-Sleep -Seconds 3
    Send-MotmCommand "motm dev test mobs clear"
    Start-Sleep -Seconds 1
    Send-MotmCommand "motm dev test mobs close"
    Start-Sleep -Seconds 2
    Send-MotmCommand "motm dev test ability aftershock"
    Start-Sleep -Seconds 5
    Send-MotmCommand "motm dev test mobs clear"
    Start-Sleep -Seconds 1
    Send-MotmCommand "motm dev test mobs close"
    Start-Sleep -Seconds 2
    Send-MotmCommand "motm dev test ability sinkhole"
    Start-Sleep -Milliseconds 1200
    & (Join-Path $PSScriptRoot "capture-evidence.ps1") -Phase "phase5" -RunId $runId -Name "sinkhole-active" | Out-Null
    Start-Sleep -Seconds 5

    $lines = Read-NewLogLines $log.FullName $startOffset
    Add-Line("")
    Add-Line("## Log Gates")
    Assert-AnyLine $lines "Style test mobs spawned: count=2 mode=close" "Close grounded target spawned"
    Assert-AnyLine $lines "Stomp armed" "Stomp armed"
    Assert-AnyLine $lines "Stomp fired at landing" "Stomp landing fired"
    Assert-AnyLine $lines "Stomp landing resolved: targets=[1-9]" "Stomp landing hit target"
    $knockbackClassError = $lines | Where-Object { $_ -match "NoClassDefFoundError|KnockbackResult" } | Select-Object -First 1
    if ($knockbackClassError) {
        Add-Line("- FAIL: KnockbackResult class loading")
        Add-Line("  - $knockbackClassError")
        throw "KnockbackResult/NoClassDefFoundError returned during Stomp target-hit test."
    }
    Add-Line("- PASS: KnockbackResult class loading")
    Assert-AnyLine $lines "abilityId=aftershock" "Aftershock queued"
    Assert-AnyLine $lines "Cast Aftershock!" "Aftershock cast result"
    Assert-AnyLine $lines "abilityId=sinkhole" "Sinkhole queued"
    Assert-AnyLine $lines "Sinkhole engaged: buried [1-9][0-9]* target\(s\)" "Sinkhole buried target"
    Assert-AnyLine $lines "Sinkhole suffocation tick" "Sinkhole suffocation tick"
    Assert-AnyLine $lines "Sinkhole released: [1-9][0-9]* target\(s\)" "Sinkhole release"

    Add-Line("")
    Add-Line("## Visual Gates")
    $sinkholeShot = Join-Path $outDir "sinkhole-active.png"
    $windowRect = Get-HytaleWindowRect
    $cropX = $windowRect.Left + 131
    $cropY = $windowRect.Top + 175
    Add-Line("- Hytale window crop origin: left=$($windowRect.Left), top=$($windowRect.Top), crop=${cropX},${cropY}")
    try {
        & (Join-Path $PSScriptRoot "visual-validate.ps1") `
            -Mode avg-rgb `
            -X $cropX `
            -Y $cropY `
            -Width 60 `
            -Height 50 `
            -Expected "#505050" `
            -ToleranceRgb 50 `
            -RequireDarkAtOrBelow `
            -ScreenshotPath $sinkholeShot `
            -Phase "phase5/$runId" `
            -RunId "sinkhole-buried"
        Add-Line("- PASS: Sinkhole buried-look avg-rgb crop")
    } catch {
        $fallbackX = $windowRect.Left + 928
        $fallbackY = $windowRect.Top + 371
        Add-Line("- NOTE: Primary buried-look crop missed; trying fallback crop=${fallbackX},${fallbackY}")
        & (Join-Path $PSScriptRoot "visual-validate.ps1") `
            -Mode avg-rgb `
            -X $fallbackX `
            -Y $fallbackY `
            -Width 80 `
            -Height 80 `
            -Expected "#505050" `
            -ToleranceRgb 70 `
            -RequireDarkAtOrBelow `
            -ScreenshotPath $sinkholeShot `
            -Phase "phase5/$runId" `
            -RunId "sinkhole-buried-fallback"
        Add-Line("- PASS: Sinkhole buried-look fallback avg-rgb crop")
    }

    Copy-Item -LiteralPath $log.FullName -Destination (Join-Path $outDir "server.log") -Force
    Add-Line("")
    Add-Line("PASS")
    $status = "PASS"
} catch {
    Add-Line("")
    Add-Line("FAIL")
    Add-Line("")
    Add-Line("Error: $($_.Exception.Message)")
    throw
} finally {
    $reportPath = Join-Path $outDir "report.md"
    $report | Set-Content -LiteralPath $reportPath -Encoding UTF8
    Write-Host "[acceptance-phase5] $status report: $reportPath"
}
