param(
    [string]$WorldName = "MOTM Creative Test",
    [ValidateSet("Direct", "Launcher", "Auto")]
    [string]$Strategy = "Direct",
    [int]$LauncherReadyTimeoutSec = 60,
    [int]$WorldLoadTimeoutSec = 180,
    [string]$PlayerName = "CodexHarness",
    [string]$PlayerUuid = "00000000-0000-4000-8000-00000000c0de"
)

$ErrorActionPreference = "Stop"

Add-Type -AssemblyName System.Windows.Forms

$repoRoot = Split-Path -Parent $PSScriptRoot
$runId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
$auditDir = Join-Path $repoRoot (Join-Path "audits\harness\start-hytale" $runId)
New-Item -ItemType Directory -Path $auditDir -Force | Out-Null
$logPath = Join-Path $auditDir "start-hytale.log"
$reportPath = Join-Path $auditDir "report.md"
$cachePath = Join-Path $repoRoot ".tools\hytale-paths.json"

function Write-Step([string]$Message) {
    $line = "[" + (Get-Date -Format "HH:mm:ss") + "] " + $Message
    Write-Host $line
    Add-Content -Path $logPath -Value $line -Encoding UTF8
}

function Resolve-HytalePaths {
    $launcherCandidates = @(
        "C:\Program Files\Hypixel Studios\Hytale Launcher\hytale-launcher.exe",
        "$env:LOCALAPPDATA\Programs\Hytale\HytaleLauncher.exe",
        "$env:LOCALAPPDATA\Hytale\HytaleLauncher.exe",
        "$env:APPDATA\Hytale\HytaleLauncher.exe",
        "$env:ProgramFiles\Hytale\HytaleLauncher.exe",
        "${env:ProgramFiles(x86)}\Hytale\HytaleLauncher.exe"
    )
    $appDir = Join-Path $env:APPDATA "Hytale\install\release\package\game\latest"
    $clientCandidates = @(
        (Join-Path $appDir "Client\HytaleClient.exe"),
        (Join-Path $appDir "Hytale.exe")
    )

    $launcher = $launcherCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    if (-not $launcher) {
        $launcher = (Get-Process -ErrorAction SilentlyContinue |
            Where-Object { $_.ProcessName -like "*hytale*" -and $_.Path -like "*launcher*.exe" } |
            Select-Object -First 1).Path
    }
    $client = $clientCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
    $assets = Join-Path $appDir "Assets.zip"
    $java = Join-Path $repoRoot ".tools\jdk-25\bin\java.exe"

    if (-not $client) { throw "Hytale client binary not found under $appDir." }
    if (-not (Test-Path -LiteralPath $assets)) { throw "Assets.zip not found at $assets." }
    if (-not (Test-Path -LiteralPath $java)) { throw "JDK 25 java.exe not found at $java. Run scripts/build-install.ps1 first." }

    $paths = [PSCustomObject]@{
        Launcher = $launcher
        Client = $client
        AppDir = $appDir
        UserDir = (Join-Path $env:APPDATA "Hytale\UserData")
        AssetsPath = $assets
        JavaExec = $java
        DiscoveredAt = (Get-Date).ToString("o")
    }
    $cacheParent = Split-Path -Parent $cachePath
    if (-not (Test-Path -LiteralPath $cacheParent)) {
        New-Item -ItemType Directory -Path $cacheParent -Force | Out-Null
    }
    $paths | ConvertTo-Json | Set-Content -Path $cachePath -Encoding UTF8
    return $paths
}

Add-Type -Name Win32 -Namespace HytaleHarness -MemberDefinition @'
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

function Wait-ForProcessWindow([string[]]$Names, [int]$TimeoutSec) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    do {
        $proc = Get-Process -ErrorAction SilentlyContinue |
            Where-Object {
                $Names -contains $_.ProcessName -and
                $_.MainWindowHandle -ne 0
            } |
            Sort-Object StartTime -Descending -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($proc) { return $proc }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    return $null
}

function Wait-ForAnyWindow([scriptblock]$Predicate, [int]$TimeoutSec) {
    $deadline = (Get-Date).AddSeconds($TimeoutSec)
    do {
        $proc = Get-Process -ErrorAction SilentlyContinue |
            Where-Object { $_.MainWindowHandle -ne 0 -and (& $Predicate $_) } |
            Sort-Object StartTime -Descending -ErrorAction SilentlyContinue |
            Select-Object -First 1
        if ($proc) { return $proc }
        Start-Sleep -Milliseconds 500
    } while ((Get-Date) -lt $deadline)
    return $null
}

function Focus-ProcessWindow($Process) {
    if (-not $Process -or $Process.MainWindowHandle -eq 0) {
        throw "Cannot foreground Hytale; no valid window handle."
    }
    [HytaleHarness.Win32]::ShowWindow($Process.MainWindowHandle, [HytaleHarness.Win32]::SW_RESTORE) | Out-Null
    Start-Sleep -Milliseconds 200
    [HytaleHarness.Win32]::SetForegroundWindow($Process.MainWindowHandle) | Out-Null
    Start-Sleep -Milliseconds 400
}

function Start-LauncherAndPressEnter($LauncherPath) {
    if (-not $LauncherPath) { throw "Launcher path not found." }
    Write-Step "Starting launcher: $LauncherPath"
    Start-Process -FilePath $LauncherPath | Out-Null
    $launcherWindow = Wait-ForAnyWindow {
        param($p)
        $p.ProcessName -like "*hytale*launcher*" -or $p.MainWindowTitle -like "*Hytale Launcher*"
    } $LauncherReadyTimeoutSec
    if (-not $launcherWindow) {
        throw "Launcher window did not appear within $LauncherReadyTimeoutSec seconds."
    }
    Write-Step "Foregrounding launcher PID=$($launcherWindow.Id) title='$($launcherWindow.MainWindowTitle)'"
    Focus-ProcessWindow $launcherWindow
    Write-Step "Sending Enter to launcher for Play/default action."
    [System.Windows.Forms.SendKeys]::SendWait("{ENTER}")
    Start-Sleep -Seconds 2
    return $launcherWindow
}

function Click-LauncherPlayButton($LauncherWindow) {
    if (-not $LauncherWindow -or $LauncherWindow.MainWindowHandle -eq 0) {
        throw "Cannot click Play; launcher window handle is missing."
    }
    Focus-ProcessWindow $LauncherWindow
    $rect = [HytaleHarness.Win32+RECT]::new()
    if (-not [HytaleHarness.Win32]::GetWindowRect($LauncherWindow.MainWindowHandle, [ref]$rect)) {
        throw "GetWindowRect failed for launcher window."
    }
    $width = $rect.Right - $rect.Left
    $height = $rect.Bottom - $rect.Top
    # Based on the 2026-05-21 launcher screenshot: the Play button is at the
    # lower-left of the launcher content, roughly 17% from left and 93.5% down.
    $x = [int]($rect.Left + ($width * 0.17))
    $y = [int]($rect.Top + ($height * 0.935))
    Write-Step "Clicking launcher Play button at screen=($x,$y), window=${width}x$height"
    [HytaleHarness.Win32]::SetCursorPos($x, $y) | Out-Null
    Start-Sleep -Milliseconds 120
    [HytaleHarness.Win32]::mouse_event([HytaleHarness.Win32]::MOUSEEVENTF_LEFTDOWN, 0, 0, 0, [IntPtr]::Zero)
    Start-Sleep -Milliseconds 80
    [HytaleHarness.Win32]::mouse_event([HytaleHarness.Win32]::MOUSEEVENTF_LEFTUP, 0, 0, 0, [IntPtr]::Zero)
    Start-Sleep -Seconds 2
}

function Start-DirectClient($Paths) {
    $saveDir = Join-Path $Paths.UserDir (Join-Path "Saves" $WorldName)
    if (-not (Test-Path -LiteralPath $saveDir)) {
        throw "World '$WorldName' not found at $saveDir."
    }

    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $Paths.Client
    $psi.WorkingDirectory = Split-Path -Parent $Paths.Client
    $psi.UseShellExecute = $false
    $args = @(
        "--world", $WorldName,
        "--auth-mode", "offline",
        "--name", $PlayerName,
        "--uuid", $PlayerUuid,
        "--user-dir", $Paths.UserDir,
        "--app-dir", $Paths.AppDir,
        "--java-exec", $Paths.JavaExec,
        "--assets-path", $Paths.AssetsPath
    )
    $psi.Arguments = ($args | ForEach-Object {
        if ($_ -match '\s') {
            '"' + ($_ -replace '"', '\"') + '"'
        } else {
            $_
        }
    }) -join " "

    Write-Step "Starting direct client: $($Paths.Client) --world '$WorldName'"
    $proc = [System.Diagnostics.Process]::Start($psi)
    Write-Step "Started HytaleClient PID=$($proc.Id)"
    return $proc
}

$result = "FAIL"
$paths = Resolve-HytalePaths
Write-Step "Resolved client=$($paths.Client)"
Write-Step "Resolved launcher=$($paths.Launcher)"

try {
    $window = $null
    if ($Strategy -eq "Direct" -or $Strategy -eq "Auto") {
        $clientProcess = Start-DirectClient $paths
        $window = Wait-ForProcessWindow @("HytaleClient", "Hytale") $LauncherReadyTimeoutSec
    } else {
        $launcherWindow = Start-LauncherAndPressEnter $paths.Launcher
        $window = Wait-ForProcessWindow @("HytaleClient", "Hytale") 8
        if (-not $window) {
            Write-Step "Enter did not start the client; falling back to Play-button click."
            Click-LauncherPlayButton $launcherWindow
            $window = Wait-ForProcessWindow @("HytaleClient", "Hytale") $LauncherReadyTimeoutSec
        }
    }

    if (-not $window) {
        throw "No Hytale client window appeared within $LauncherReadyTimeoutSec seconds."
    }
    Write-Step "Foregrounding Hytale window PID=$($window.Id) title='$($window.MainWindowTitle)'"
    Focus-ProcessWindow $window

    & (Join-Path $PSScriptRoot "capture-evidence.ps1") -Phase "harness/start-hytale" -RunId $runId -Name "main-menu"

    $result = "PASS"
    Write-Step "PASS: Hytale client started and foregrounded."
} finally {
    @"
# H1 start-hytale run - $runId

## Result
$result

## World
$WorldName

## Paths
- Client: $($paths.Client)
- Launcher: $($paths.Launcher)
- UserDir: $($paths.UserDir)
- AppDir: $($paths.AppDir)

## Evidence
- main-menu.png
- start-hytale.log
"@ | Set-Content -Path $reportPath -Encoding UTF8
}

if ($result -ne "PASS") {
    throw "start-hytale failed; see $reportPath"
}
