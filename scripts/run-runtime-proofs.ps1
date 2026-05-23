param(
    [string]$WorldName = "MOTM Creative Test",
    [string]$RunId = "",
    [string[]]$Proofs = @(),
    [switch]$ColdLaunch,
    [switch]$SkipFlatlandsGate,
    [int]$CommandDelayMilliseconds = 900,
    [int]$ProofSettleMilliseconds = 1700
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}

$phaseId = "proofs/p0-p4"
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $phaseId $RunId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

if ($Proofs.Count -eq 0) {
    $Proofs = @(
        "coating-metal",
        "coating-obsidian",
        "coating-stone",
        "coating-poison-target",
        "tempblock-metal-wall",
        "tempblock-stone-pillar",
        "tempblock-flower",
        "tempblock-sapling",
        "tempfluid-lava-ring",
        "tempfluid-water-field",
        "proxy-magma-blob",
        "proxy-cactus-projectile",
        "proxy-gem",
        "proxy-glass-shards",
        "movement-burrow",
        "movement-tunnel",
        "movement-dust-devil"
    )
}

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
        if ($reader) { $reader.Dispose() }
        $fs.Dispose()
    }
    return $lines
}

function Send-MotmCommand([string]$Text, [int]$Delay = $CommandDelayMilliseconds) {
    Write-Host "[run-runtime-proofs] /$Text"
    & (Join-Path $PSScriptRoot "send-input.ps1") -Action Command -Text $Text -DelayMilliseconds 120 | Out-Host
    Start-Sleep -Milliseconds $Delay
}

function Capture([string]$Name) {
    & (Join-Path $PSScriptRoot "capture-evidence.ps1") -Phase $phaseId -RunId $RunId -Name $Name | Out-Host
}

function Invoke-EntryGate {
    Add-Line("## Entry Gate")
    Add-Line("")
    & (Join-Path $PSScriptRoot "capture-evidence.ps1") -Phase $phaseId -RunId $RunId -Name "entry-before-typing" | Out-Host
    & (Join-Path $PSScriptRoot "check-world-entry-state.ps1") -RunId $RunId -NoThrow | Out-Host
    Add-Line("- Captured entry-before-typing before sending any in-game commands.")
    Add-Line("- Entry-state classifier report: audits/harness/world-entry-state/$RunId/report.md")
    Add-Line("")
}

function Invoke-ThirdPersonGate {
    Add-Line("## Third-Person Gate")
    Add-Line("")
    & (Join-Path $PSScriptRoot "send-input.ps1") -Action ThirdPerson -DelayMilliseconds 550 | Out-Host
    Capture "third-person-confirmation"
    Add-Line("- Pressed V and captured third-person-confirmation.png before visual proofs.")
    Add-Line("")
}

function Get-MobCountFromLines($Lines) {
    $line = $Lines | Where-Object { $_ -match "Style test mobs tracked: count=" } | Select-Object -Last 1
    if (-not $line) { return $null }
    if ($line -match "count=(\d+).*nearbyCleanupRoles=(\d+)") {
        return [PSCustomObject]@{
            tracked = [int]$Matches[1]
            nearby = [int]$Matches[2]
            line = $line
        }
    }
    return $null
}

function Ensure-StationaryTarget {
    $log = Get-LatestServerLog
    if (-not $log) { throw "No server log found for $WorldName." }
    $offset = $log.Length
    Send-MotmCommand "motm dev test mobs count" 500
    $lines = Read-NewLogLines $log.FullName $offset
    $count = Get-MobCountFromLines $lines
    if ($count) {
        Add-Line("- Mob count before setup: $($count.line)")
    } else {
        Add-Line("- Mob count before setup: unavailable; will spawn one stationary target.")
    }

    if ($count -and $count.tracked -ge 1 -and $count.nearby -le 2) {
        return
    }

    if ($count -and $count.nearby -gt 2) {
        Send-MotmCommand "motm dev test mobs clear" 650
        Add-Line("- Cleared nearby/tracked test mobs because nearbyCleanupRoles=$($count.nearby).")
    }

    Send-MotmCommand "motm dev test mobs stationary" 1350
    Send-MotmCommand "motm dev test mobs count" 500
    Add-Line("- Spawned/reused one stationary grounded test dummy for target-dependent proofs.")
}

function Proof-NeedsTarget([string]$Proof) {
    return $Proof -in @(
        "coating-poison-target",
        "movement-dust-devil"
    )
}

function Invoke-Proof([string]$Proof) {
    Add-Line("## $Proof")
    Add-Line("")
    if (Proof-NeedsTarget $Proof) {
        Ensure-StationaryTarget
    }

    $log = Get-LatestServerLog
    if (-not $log) { throw "No server log found for $WorldName." }
    $offset = $log.Length

    if ($Proof -like "movement-*") {
        Capture "$Proof-before"
    }

    Send-MotmCommand "motm dev proof $Proof" $ProofSettleMilliseconds
    Capture $Proof

    $lines = Read-NewLogLines $log.FullName $offset
    $proofLines = @($lines | Where-Object { $_ -match "\[MOTM\] Proof $([regex]::Escape($Proof)) " })
    $resultLine = $proofLines | Select-Object -Last 1
    $blocking = @($lines | Where-Object {
        $_ -match "NoClassDefFoundError|ClassNotFoundException|Exception|ERROR|failed safely|Reloading nonexistent|Missing gameplay effect|Proof .* FAIL"
    })

    if ($resultLine) {
        Add-Line("- Result: $resultLine")
    } else {
        Add-Line("- Result: FAIL - no proof result line found.")
    }
    foreach ($line in $blocking) {
        Add-Line("- Residual: $line")
    }
    if ($resultLine -and $resultLine -match " PASS:" -and $blocking.Count -eq 0) {
        Add-Line("- Status: PASS")
    } else {
        Add-Line("- Status: FAIL")
    }
    Add-Line("")
}

$status = "FAIL"
try {
    Add-Line("# P0-P4 Runtime Proof Run")
    Add-Line("")
    Add-Line("- Run: $RunId")
    Add-Line("- World: $WorldName")
    Add-Line("- Cold launch: $ColdLaunch")
    Add-Line("- Proofs: " + ($Proofs -join ", "))
    Add-Line("")

    if ($ColdLaunch) {
        & (Join-Path $PSScriptRoot "cold-launch.ps1") -WorldName $WorldName -LaunchAndLoad -EnsureFlatlands
    }

    Invoke-EntryGate

    if (-not $SkipFlatlandsGate) {
        & (Join-Path $PSScriptRoot "ensure-flatlands.ps1") -VerifyOnly
        & (Join-Path $PSScriptRoot "move-test-lane.ps1") -RunId $RunId -ForwardMilliseconds 2600 -StrafeMilliseconds 700 | Out-Host
        Add-Line("- Flatlands verified and moved into a clearer test lane.")
        Add-Line("")
    }

    Invoke-ThirdPersonGate

    Send-MotmCommand "motm dev freecast on"
    Send-MotmCommand "motm dev test mobs count" 450

    foreach ($proof in $Proofs) {
        Invoke-Proof $proof
    }

    $log = Get-LatestServerLog
    if ($log) {
        Copy-Item -LiteralPath $log.FullName -Destination (Join-Path $outDir "server.log") -Force
    }

    $failures = @($report | Where-Object { $_ -match "^- Status: FAIL" })
    Add-Line("## Summary")
    Add-Line("")
    if ($failures.Count -eq 0) {
        Add-Line("PASS")
        $status = "PASS"
    } else {
        Add-Line("FAIL")
        throw "Runtime proof run had $($failures.Count) failed proof(s)."
    }
} catch {
    Add-Line("")
    Add-Line("FAIL")
    Add-Line("")
    Add-Line("Error: $($_.Exception.Message)")
    throw
} finally {
    $reportPath = Join-Path $outDir "report.md"
    $report | Set-Content -LiteralPath $reportPath -Encoding UTF8
    Write-Host "[run-runtime-proofs] $status report: $reportPath"
}
