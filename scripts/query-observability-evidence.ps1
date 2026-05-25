param(
    [ValidateSet("summary", "list", "sources", "events", "raw")]
    [string]$Action = "summary",
    [string]$RunId = "",
    [string]$Phase = "agent-observability",
    [string]$Source = "",
    [string]$Pattern = "",
    [string]$TraceId = "",
    [string]$ScenarioId = "",
    [string]$Type = "",
    [int]$Context = 4
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$phaseDir = Join-Path $repoRoot (Join-Path "audits" $Phase)

function Resolve-RunDir {
    param([string]$RequestedRunId)

    if (-not (Test-Path -LiteralPath $phaseDir)) {
        throw "No observability audit directory found: $phaseDir"
    }

    if ([string]::IsNullOrWhiteSpace($RequestedRunId) -or "latest".Equals($RequestedRunId, [System.StringComparison]::OrdinalIgnoreCase)) {
        $latest = Get-ChildItem -LiteralPath $phaseDir -Directory |
            Sort-Object LastWriteTime -Descending |
            Select-Object -First 1
        if (-not $latest) {
            throw "No runs found under $phaseDir"
        }
        return $latest.FullName
    }

    $dir = Join-Path $phaseDir $RequestedRunId
    if (-not (Test-Path -LiteralPath $dir)) {
        throw "Run not found: $dir"
    }
    return (Resolve-Path -LiteralPath $dir).Path
}

function Get-JsonlRows {
    param([string[]]$Paths)

    foreach ($path in $Paths) {
        if (-not (Test-Path -LiteralPath $path)) { continue }
        $lineNumber = 0
        foreach ($line in Get-Content -LiteralPath $path) {
            $lineNumber++
            if ([string]::IsNullOrWhiteSpace($line)) { continue }
            try {
                $row = $line | ConvertFrom-Json -ErrorAction Stop
                $row | Add-Member -NotePropertyName "_source" -NotePropertyValue $path -Force
                $row | Add-Member -NotePropertyName "_lineNumber" -NotePropertyValue $lineNumber -Force
                $row
            } catch {
                [PSCustomObject]@{
                    _source = $path
                    _lineNumber = $lineNumber
                    parseError = $_.Exception.Message
                    rawLine = $line
                }
            }
        }
    }
}

function Format-EventRow {
    param([object]$Row)

    $time = if ($Row.timestamp) { $Row.timestamp } elseif ($Row.epochMillis) { $Row.epochMillis } else { "" }
    $plane = if ($Row.plane) { $Row.plane } elseif ($Row.kind) { $Row.kind } else { "event" }
    $rowType = if ($Row.type) { $Row.type } elseif ($Row.eventName) { $Row.eventName } else { "" }
    $trace = if ($Row.traceId) { " trace=$($Row.traceId)" } else { "" }
    $scenario = if ($Row.scenarioId) { " scenario=$($Row.scenarioId)" } else { "" }
    $summary = if ($Row.data) {
        ($Row.data | ConvertTo-Json -Compress -Depth 12)
    } elseif ($Row.text) {
        $Row.text
    } elseif ($Row.rawLine) {
        $Row.rawLine
    } else {
        ($Row | ConvertTo-Json -Compress -Depth 6)
    }
    "$time [$plane/$rowType]$trace$scenario $summary"
}

if ("list" -eq $Action) {
    if (-not (Test-Path -LiteralPath $phaseDir)) {
        Write-Host "[query-observability-evidence] no runs: $phaseDir"
        exit 0
    }
    Get-ChildItem -LiteralPath $phaseDir -Directory |
        Sort-Object LastWriteTime -Descending |
        Select-Object Name, LastWriteTime, FullName
    exit 0
}

$runDir = Resolve-RunDir $RunId
$manifestPath = Join-Path $runDir "manifest.json"
$manifest = if (Test-Path -LiteralPath $manifestPath) {
    Get-Content -LiteralPath $manifestPath -Raw | ConvertFrom-Json
} else {
    $null
}

if ("summary" -eq $Action) {
    $jsonl = @(Get-ChildItem -LiteralPath $runDir -Recurse -Filter "*.jsonl" -File -ErrorAction SilentlyContinue)
    $rawFiles = @(Get-ChildItem -LiteralPath (Join-Path $runDir "raw") -Recurse -File -ErrorAction SilentlyContinue)
    Write-Host "Run: $([IO.Path]::GetFileName($runDir))"
    Write-Host "Dir: $runDir"
    if ($manifest) {
        Write-Host "World: $($manifest.worldName)"
        Write-Host "MOTM data: $($manifest.motmDataDir)"
        Write-Host "Installed jar: $($manifest.installedJar)"
        Write-Host "Git: $($manifest.gitHead)"
    }
    Write-Host "JSONL sources: $($jsonl.Count)"
    Write-Host "Raw files: $($rawFiles.Count)"
    foreach ($file in $jsonl | Sort-Object FullName) {
        $count = (Get-Content -LiteralPath $file.FullName -ErrorAction SilentlyContinue | Measure-Object -Line).Lines
        Write-Host "- $($file.FullName.Substring($runDir.Length + 1)): $count rows"
    }
    exit 0
}

if ("sources" -eq $Action) {
    Get-ChildItem -LiteralPath $runDir -Recurse -File |
        Sort-Object FullName |
        Select-Object @{Name="RelativePath";Expression={$_.FullName.Substring($runDir.Length + 1)}}, Length, LastWriteTime
    exit 0
}

if ("events" -eq $Action) {
    $eventSources = @()
    $runtimeDir = Join-Path $runDir "raw/motm-observability"
    if (Test-Path -LiteralPath $runtimeDir) {
        $eventSources += @(Get-ChildItem -LiteralPath $runtimeDir -Filter "*.jsonl" -File -ErrorAction SilentlyContinue | ForEach-Object FullName)
    }
    $eventSources += @(Get-ChildItem -LiteralPath (Join-Path $runDir "indexes") -Filter "*.jsonl" -File -ErrorAction SilentlyContinue | ForEach-Object FullName)
    if ($Source) {
        $eventSources = @($eventSources | Where-Object { $_ -match [regex]::Escape($Source) })
    }

    $rows = @(Get-JsonlRows $eventSources)
    if ($TraceId) { $rows = @($rows | Where-Object { $_.traceId -eq $TraceId -or $_.data.traceId -eq $TraceId }) }
    if ($ScenarioId) { $rows = @($rows | Where-Object { $_.scenarioId -eq $ScenarioId }) }
    if ($Type) { $rows = @($rows | Where-Object { $_.type -eq $Type -or $_.eventName -eq $Type -or $_.category -eq $Type }) }
    if ($Pattern) {
        $regex = [regex]::Escape($Pattern)
        $rows = @($rows | Where-Object { ($_ | ConvertTo-Json -Compress -Depth 20) -match $regex })
    }

    foreach ($row in $rows) {
        Format-EventRow $row
    }
    exit 0
}

if ("raw" -eq $Action) {
    if ([string]::IsNullOrWhiteSpace($Pattern)) {
        throw "-Pattern is required for raw search."
    }
    $root = Join-Path $runDir "raw"
    $files = @(Get-ChildItem -LiteralPath $root -Recurse -File -ErrorAction SilentlyContinue)
    if ($Source) {
        $files = @($files | Where-Object { $_.FullName -match [regex]::Escape($Source) })
    }
    foreach ($file in $files) {
        if ($file.Extension -eq ".gz") {
            continue
        }
        $matches = @(Select-String -LiteralPath $file.FullName -Pattern $Pattern -SimpleMatch -Context $Context,$Context -ErrorAction SilentlyContinue)
        foreach ($match in $matches) {
            Write-Host "==== $($file.FullName.Substring($runDir.Length + 1)):$($match.LineNumber)"
            foreach ($pre in $match.Context.PreContext) { Write-Host $pre }
            Write-Host $match.Line
            foreach ($post in $match.Context.PostContext) { Write-Host $post }
        }
    }
}
