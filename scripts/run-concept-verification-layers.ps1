param(
    [string]$WorldName = "MOTM Creative Test",
    [string]$RunId = "",
    [string[]]$Classes = @("terra", "hydro", "aero", "corruptus"),
    [string[]]$Styles = @(),
    [ValidateSet("PrimitiveProofs", "NormalControl")]
    [string[]]$Layers = @("PrimitiveProofs", "NormalControl"),
    [ValidateSet("PrimarySecondaryUse", "AbilityKeys", "Both")]
    [string]$ControlMode = "PrimarySecondaryUse",
    [switch]$SkipBuild,
    [switch]$StopOnFailure
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = "concept-layers-live-" + (Get-Date -Format "yyyyMMdd-HHmmss")
}

$Classes = @($Classes | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim().ToLowerInvariant() } | Where-Object { $_ })
$Styles = @($Styles | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim().ToLowerInvariant() } | Where-Object { $_ })
$Layers = @($Layers | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim() } | Where-Object { $_ })

$outDir = Join-Path $repoRoot (Join-Path "audits\concept-verification-live" $RunId)
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$report = New-Object System.Collections.Generic.List[string]
$rows = New-Object System.Collections.Generic.List[object]

function Add-Line([string]$Line) {
    $script:report.Add($Line)
}

function Resolve-PowerShellExecutable {
    $pwsh = Get-Command pwsh -ErrorAction SilentlyContinue
    if ($pwsh) { return $pwsh.Source }
    $powershell = Get-Command powershell -ErrorAction SilentlyContinue
    if ($powershell) { return $powershell.Source }
    throw "Could not locate pwsh or powershell."
}

function Get-StyleDefinitions {
    $stylesRoot = Join-Path $repoRoot "src\main\resources\data\styles"
    $docs = Get-ChildItem -LiteralPath $stylesRoot -Filter "*.json" |
        ForEach-Object { Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json }
    @($docs | ForEach-Object { $_.styles } | Where-Object {
        $Classes -contains ([string]$_.'class_id').ToLowerInvariant() -and
        ($Styles.Count -eq 0 -or $Styles -contains ([string]$_.id).ToLowerInvariant())
    })
}

function Get-ProofIds {
    $catalog = Join-Path $repoRoot "src\main\java\com\motm\proof\MotmProofCatalog.java"
    if (-not (Test-Path -LiteralPath $catalog)) {
        throw "Proof catalog not found: $catalog"
    }
    $text = Get-Content -LiteralPath $catalog -Raw
    $matches = [regex]::Matches($text, '"([^"]+)"')
    @($matches | ForEach-Object { $_.Groups[1].Value } | Where-Object {
        $_ -match "^(coating|tempblock|tempfluid|proxy|movement)-"
    })
}

function Invoke-LayerCommand {
    param(
        [string]$Label,
        [string[]]$Arguments,
        [string]$LogName
    )

    $logPath = Join-Path $outDir $LogName
    Add-Line("- START: $Label")
    $status = "PASS"
    $note = ""
    try {
        & $script:PowerShellExe @Arguments 2>&1 | Tee-Object -FilePath $logPath
        if ($LASTEXITCODE -ne 0) {
            throw "$Label exited with code $LASTEXITCODE"
        }
    } catch {
        $status = "FAIL"
        $note = $_.Exception.Message
        Add-Line("  - FAIL: $note")
        if ($StopOnFailure) {
            throw
        }
    }
    if ($status -eq "PASS") {
        Add-Line("  - PASS")
    }
    $rows.Add([PSCustomObject]@{
        layer = $Label
        status = $status
        log = $LogName
        note = $note
    })
}

$script:PowerShellExe = Resolve-PowerShellExecutable
$selectedStyles = Get-StyleDefinitions

Add-Line("# Concept Verification Live Layers")
Add-Line("")
Add-Line("- RunId: $RunId")
Add-Line("- World: $WorldName")
Add-Line("- Classes: $($Classes -join ', ')")
Add-Line("- Styles: " + $(if ($Styles.Count -gt 0) { $Styles -join ", " } else { "all selected classes" }))
Add-Line("- Layers: $($Layers -join ', ')")
Add-Line("")

if ($Layers -contains "PrimitiveProofs") {
    $proofs = Get-ProofIds
    $args = @(
        "-NoProfile",
        "-ExecutionPolicy", "Bypass",
        "-File", (Join-Path $PSScriptRoot "run-agent-observability-baseline.ps1"),
        "-WorldName", $WorldName,
        "-RunId", "$RunId-primitives",
        "-ScenarioId", "concept-primitive-proofs",
        "-Proofs", ($proofs -join ",")
    )
    if ($SkipBuild) {
        $args += "-SkipBuild"
    }
    Invoke-LayerCommand "PrimitiveProofs" $args "primitive-proofs.log"
}

if ($Layers -contains "NormalControl") {
    $modes = if ($ControlMode -eq "Both") { @("PrimarySecondaryUse", "AbilityKeys") } else { @($ControlMode) }
    foreach ($style in $selectedStyles) {
        $classId = ([string]$style.'class_id').ToLowerInvariant()
        $styleId = ([string]$style.id).ToLowerInvariant()
        foreach ($mode in $modes) {
            $label = "NormalControl:{0}/{1}:{2}" -f $classId, $styleId, $mode
            $safeLabel = $label -replace '[^A-Za-z0-9_.-]', '-'
            $args = @(
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", (Join-Path $PSScriptRoot "run-normal-control-probe.ps1"),
                "-WorldName", $WorldName,
                "-ClassId", $classId,
                "-StyleId", $styleId,
                "-RunId", "$RunId-normal-$classId-$styleId-$mode",
                "-ControlMode", $mode
            )
            Invoke-LayerCommand $label $args "$safeLabel.log"
        }
    }
}

$rows | Export-Csv -LiteralPath (Join-Path $outDir "summary.csv") -NoTypeInformation -Encoding UTF8
$rows | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $outDir "summary.json") -Encoding UTF8
$report | Set-Content -LiteralPath (Join-Path $outDir "report.md") -Encoding UTF8

$failures = @($rows | Where-Object { $_.status -ne "PASS" }).Count
Write-Host "[run-concept-verification-layers] report: $(Join-Path $outDir "report.md")"
if ($failures -gt 0) {
    Write-Host "[run-concept-verification-layers] FAILURES: $failures"
    exit 1
}

Write-Host "[run-concept-verification-layers] PASS"
