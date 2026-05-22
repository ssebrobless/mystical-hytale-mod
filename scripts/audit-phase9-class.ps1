param(
    [ValidateSet("terra", "hydro", "aero", "corruptus")]
    [string]$ClassId = "terra",
    [string]$WorldName = "MOTM Creative Test",
    [string[]]$Styles = @(),
    [switch]$SkipFlatlandsGate,
    [int]$CommandDelayMilliseconds = 850,
    [int]$PostCastMilliseconds = 2400
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
$phaseId = "phase9-$ClassId-flatlands"
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $phaseId $runId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

$stylePath = Join-Path $repoRoot "src\main\resources\data\styles\${ClassId}_styles.json"
if (-not (Test-Path -LiteralPath $stylePath)) {
    throw "Style file not found: $stylePath"
}
$styleFile = Get-Content -LiteralPath $stylePath -Raw | ConvertFrom-Json
$allStyles = @($styleFile.styles)
if ($Styles.Count -gt 0) {
    $wanted = @{}
    $Styles | ForEach-Object { $wanted[$_.ToLowerInvariant()] = $true }
    $allStyles = @($allStyles | Where-Object { $wanted.ContainsKey($_.id.ToLowerInvariant()) })
}
if ($allStyles.Count -eq 0) {
    throw "No styles selected for class '$ClassId'."
}

$report = New-Object System.Collections.Generic.List[string]
function Add-Line([string]$Line) {
    $script:report.Add($Line)
}

function Add-RunMarker([string]$Marker) {
    Send-MotmCommand "motm dev audit marker $Marker" 350
}

function Get-LatestServerLog {
    $logDir = Join-Path $env:APPDATA ("Hytale\UserData\Saves\" + $WorldName + "\logs")
    Get-ChildItem -LiteralPath $logDir -Filter "*_server.log" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Send-MotmCommand([string]$Text, [int]$DelayMilliseconds = $CommandDelayMilliseconds) {
    Write-Host "[audit-phase9-class] /$Text"
    & (Join-Path $PSScriptRoot "send-input.ps1") -Action Command -Text $Text -DelayMilliseconds 120 | Out-Host
    Start-Sleep -Milliseconds $DelayMilliseconds
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

function Read-LogLinesAfterMarker([string]$Path, [string]$Marker) {
    $all = Get-Content -LiteralPath $Path -ErrorAction Stop
    $markerIndex = -1
    for ($i = $all.Count - 1; $i -ge 0; $i--) {
        if ($all[$i] -match [regex]::Escape($Marker)) {
            $markerIndex = $i
            break
        }
    }
    if ($markerIndex -lt 0) {
        return $all
    }
    return @($all | Select-Object -Skip $markerIndex)
}

function Capture([string]$Name) {
    & (Join-Path $PSScriptRoot "capture-evidence.ps1") -Phase $phaseId -RunId $runId -Name $Name | Out-Host
}

function Get-AbilityEvidence($Lines, [string]$AbilityId) {
    $escaped = [regex]::Escape($AbilityId)
    $Lines | Where-Object {
        $_ -match "abilityId=$escaped" -or
        $_ -match "Cast .*!.*" -and $_ -match $escaped -or
        $_ -match "Queued ability cast result:.*abilityId=$escaped"
    }
}

function Get-AbilityResult($Lines, [string]$AbilityId) {
    $escaped = [regex]::Escape($AbilityId)
    $Lines |
        Where-Object { $_ -match "Queued ability cast result:.*abilityId=$escaped" } |
        Select-Object -Last 1
}

function Get-ResidualLines($Lines) {
    $Lines | Where-Object {
        $_ -match "No valid target|Reloading nonexistent|Unmapped NPC type|Exception|ERROR|NoClassDefFoundError|ClassNotFoundException"
    }
}

$status = "FAIL"
try {
    Add-Line("# Phase 9 $ClassId Flatlands Class Audit")
    Add-Line("")
    Add-Line("- Run: $runId")
    Add-Line("- World: $WorldName")
    Add-Line("- Style source: $stylePath")
    Add-Line("- Styles: " + (($allStyles | ForEach-Object { $_.id }) -join ", "))
    Add-Line("")

    if (-not $SkipFlatlandsGate) {
        & (Join-Path $PSScriptRoot "ensure-flatlands.ps1") -VerifyOnly
    }

    $log = Get-LatestServerLog
    if (-not $log) {
        throw "No server log found for $WorldName."
    }
    $startOffset = $log.Length
    Add-Line("- Server log: $($log.FullName)")
    Add-Line("")

    Send-MotmCommand "motm dev freecast on"
    Send-MotmCommand "motm dev class set $ClassId"
    $marker = "phase9-$ClassId-$runId"
    Add-RunMarker $marker

    foreach ($style in $allStyles) {
        $styleId = [string]$style.id
        Add-Line("## $styleId")
        Add-Line("")
        if (-not $SkipFlatlandsGate) {
            & (Join-Path $PSScriptRoot "ensure-flatlands.ps1") -VerifyOnly
        }

        Send-MotmCommand "motm dev class set $ClassId"
        Send-MotmCommand "motm dev styles clear"
        Send-MotmCommand "motm style $styleId" 1250

        foreach ($ability in @($style.abilities)) {
            $abilityId = [string]$ability.id
            Send-MotmCommand "motm dev test mobs close" 1350
            $abilityStartLog = Get-LatestServerLog
            $abilityStartOffset = if ($abilityStartLog) { $abilityStartLog.Length } else { 0 }
            Send-MotmCommand "motm dev test ability $abilityId" $PostCastMilliseconds
            Capture "$styleId-$abilityId"

            $abilityLines = Read-NewLogLines $abilityStartLog.FullName $abilityStartOffset
            $resultLine = Get-AbilityResult $abilityLines $abilityId
            if ($resultLine) {
                Add-Line("- PASS: ``$abilityId`` cast")
                Add-Line("  - $resultLine")
            } else {
                Add-Line("- FAIL: ``$abilityId`` cast result missing")
            }
            $residuals = Get-ResidualLines $abilityLines
            foreach ($residual in $residuals) {
                Add-Line("  - REVIEW: $residual")
            }
        }
        Add-Line("")
    }

    $log = Get-LatestServerLog
    $lines = Read-LogLinesAfterMarker $log.FullName $marker
    $lines | Set-Content -LiteralPath (Join-Path $outDir "server.log") -Encoding UTF8

    Add-Line("## Residual Scan")
    Add-Line("")
    $blocking = $lines | Where-Object { $_ -match "NoClassDefFoundError|ClassNotFoundException|Exception|ERROR" }
    if ($blocking.Count -eq 0) {
        Add-Line("- PASS: no blocking class/runtime errors in class audit slice.")
    } else {
        foreach ($line in $blocking) {
            Add-Line("- FAIL: $line")
        }
    }

    $missingCasts = $report | Where-Object { $_ -match "^- FAIL: .* cast result missing" }
    Add-Line("")
    if ($blocking.Count -eq 0 -and $missingCasts.Count -eq 0) {
        Add-Line("PASS")
        $status = "PASS"
    } else {
        Add-Line("FAIL")
        throw "Phase 9 $ClassId audit failed: missing casts=$($missingCasts.Count), blocking=$($blocking.Count)."
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
    Write-Host "[audit-phase9-class] $status report: $reportPath"
}
