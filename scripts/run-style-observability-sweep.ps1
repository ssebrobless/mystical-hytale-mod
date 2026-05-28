param(
    [string]$WorldName = "MOTM Creative Test",
    [string]$RunId = "",
    [string[]]$Classes = @("terra", "hydro", "aero", "corruptus"),
    [string[]]$Styles = @(),
    [switch]$SkipBuild,
    [switch]$StopOnFailure,
    [int]$CleanupDelayMilliseconds = 6500
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}
$Classes = @($Classes | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
$Styles = @($Styles | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim() } | Where-Object { $_ })

$outDir = Join-Path $repoRoot (Join-Path "audits\style-sweeps" $RunId)
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

function Resolve-PowerShellExecutable {
    try {
        $current = (Get-Process -Id $PID).Path
        if (-not [string]::IsNullOrWhiteSpace($current) -and (Test-Path -LiteralPath $current)) {
            return $current
        }
    } catch {
    }
    $pwsh = Get-Command pwsh -ErrorAction SilentlyContinue
    if ($pwsh) { return $pwsh.Source }
    $powershell = Get-Command powershell -ErrorAction SilentlyContinue
    if ($powershell) { return $powershell.Source }
    throw "Could not locate pwsh or powershell."
}

function Get-MobModeForStyle($style) {
    $text = (($style.abilities | ConvertTo-Json -Depth 8) + " " + [string]$style.id).ToLowerInvariant()
    if ($text -match "radius|aoe|field|storm|explosion|chain|pool|summon|spawn|lure|taunt") {
        return "cluster"
    }
    return "stationary"
}

$psExe = Resolve-PowerShellExecutable
$stylesRoot = Join-Path $repoRoot "src\main\resources\data\styles"
$styleDocs = Get-ChildItem -LiteralPath $stylesRoot -Filter "*.json" |
    ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json }
$allStyles = @($styleDocs | ForEach-Object { $_.styles })
$selectedStyles = @($allStyles | Where-Object {
    $Classes -contains [string]$_.class_id -and ($Styles.Count -eq 0 -or $Styles -contains [string]$_.id)
})
if ($selectedStyles.Count -eq 0) {
    throw "No styles selected. Classes=[$($Classes -join ',')] Styles=[$($Styles -join ',')]"
}

$summaryRows = New-Object System.Collections.Generic.List[object]
$report = New-Object System.Collections.Generic.List[string]
$report.Add("# Style Observability Sweep")
$report.Add("")
$report.Add("- RunId: $RunId")
$report.Add("- World: $WorldName")
$report.Add("- Classes: $($Classes -join ', ')")
$report.Add("- Styles: $($selectedStyles.Count)")
$report.Add("")
$report.Add("| Class | Style | Abilities | Mob mode | Status | Run | Notes |")
$report.Add("|---|---|---|---|---|---|---|")

foreach ($style in $selectedStyles) {
    $classId = [string]$style.class_id
    $styleId = [string]$style.id
    $abilities = @($style.abilities | ForEach-Object { [string]$_.id })
    $mobMode = Get-MobModeForStyle $style
    $styleRunId = "$RunId-$classId-$styleId"
    $args = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", (Join-Path $PSScriptRoot "run-agent-observability-baseline.ps1"),
        "-WorldName", $WorldName,
        "-RunId", $styleRunId,
        "-ScenarioId", "style-sweep-$classId-$styleId",
        "-ClassId", $classId,
        "-StyleId", $styleId,
        "-MobMode", $mobMode,
        "-Abilities", ($abilities -join ","),
        "-NoDefaultProofs"
    )
    $args += @("-CleanupDelayMilliseconds", $CleanupDelayMilliseconds)
    if ($SkipBuild) {
        $args += "-SkipBuild"
    }

    $logPath = Join-Path $outDir "$classId-$styleId.log"
    Write-Host "[style-sweep] START $classId/$styleId abilities=$($abilities -join ',') mobMode=$mobMode"
    $status = "PASS"
    $notes = ""
    try {
        & $psExe @args 2>&1 | Tee-Object -FilePath $logPath
        if ($LASTEXITCODE -ne 0) {
            throw "baseline exited with code $LASTEXITCODE"
        }
    } catch {
        $status = "FAIL"
        $notes = $_.Exception.Message -replace "\|", "/"
        Write-Host "[style-sweep] FAIL $classId/$styleId $notes"
        if ($StopOnFailure) {
            throw
        }
    }

    $runPath = "audits/agent-observability/$styleRunId"
    $report.Add("| $classId | $styleId | $($abilities -join ', ') | $mobMode | $status | $runPath | $notes |")
    $summaryRows.Add([PSCustomObject]@{
        class = $classId
        style = $styleId
        abilities = ($abilities -join ",")
        mob_mode = $mobMode
        status = $status
        run_id = $styleRunId
        run_path = $runPath
        notes = $notes
    })
}

$summaryRows | Export-Csv -LiteralPath (Join-Path $outDir "summary.csv") -NoTypeInformation -Encoding UTF8
$summaryRows | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $outDir "summary.json") -Encoding UTF8
$report | Set-Content -LiteralPath (Join-Path $outDir "report.md") -Encoding UTF8

$failCount = @($summaryRows | Where-Object { $_.status -ne "PASS" }).Count
Write-Host "[style-sweep] Report: $(Join-Path $outDir "report.md")"
if ($failCount -gt 0) {
    Write-Host "[style-sweep] FAILURES: $failCount"
    exit 1
}
Write-Host "[style-sweep] PASS: all selected styles passed baseline evidence checks"
