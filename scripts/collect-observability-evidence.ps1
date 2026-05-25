param(
    [string]$WorldName = "Main",
    [string]$RunId = "",
    [string]$Phase = "agent-observability",
    [string]$OutDir = "",
    [string]$DataDir = "",
    [int]$MaxClientLogs = 3,
    [int]$MaxTelemetryFiles = 3,
    [int]$MaxServerLogs = 3
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot

function Test-IsMacOS {
    return ($global:IsMacOS -eq $true)
}

if ((Test-IsMacOS) -and [string]::IsNullOrWhiteSpace($env:APPDATA)) {
    $env:APPDATA = Join-Path $HOME "Library/Application Support"
}
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}
if ([string]::IsNullOrWhiteSpace($OutDir)) {
    $OutDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $Phase $RunId))
}

function New-Dir([string]$Path) {
    New-Item -ItemType Directory -Path $Path -Force | Out-Null
    return (Resolve-Path -LiteralPath $Path).Path
}

function Resolve-MotmDataDir {
    param([string]$ExplicitDataDir)

    if (-not [string]::IsNullOrWhiteSpace($ExplicitDataDir)) {
        if (-not (Test-Path -LiteralPath $ExplicitDataDir)) {
            throw "MOTM data directory not found: $ExplicitDataDir"
        }
        return (Resolve-Path -LiteralPath $ExplicitDataDir).Path
    }

    $hytaleUserData = Join-Path (Join-Path $env:APPDATA "Hytale") "UserData"
    $worldRoot = Join-Path $hytaleUserData (Join-Path "Saves" $WorldName)
    $motmRoot = Join-Path $worldRoot "motm-data"
    $config = Get-ChildItem -LiteralPath $motmRoot -Recurse -Filter "motm-server.properties" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($config) {
        return $config.Directory.FullName
    }

    $fallback = Get-ChildItem -LiteralPath $hytaleUserData -Recurse -Filter "motm-server.properties" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($fallback) {
        return $fallback.Directory.FullName
    }

    return ""
}

function Copy-NewestFiles {
    param(
        [string]$SourceDir,
        [string]$Filter,
        [string]$DestinationDir,
        [int]$MaxCount
    )

    $copied = @()
    if (-not (Test-Path -LiteralPath $SourceDir)) {
        return $copied
    }
    New-Dir $DestinationDir | Out-Null
    $files = Get-ChildItem -LiteralPath $SourceDir -Filter $Filter -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First $MaxCount
    foreach ($file in $files) {
        $dest = Join-Path $DestinationDir $file.Name
        Copy-Item -LiteralPath $file.FullName -Destination $dest -Force
        $copied += [PSCustomObject]@{
            source = $file.FullName
            copiedTo = $dest
            length = $file.Length
            lastWriteTime = $file.LastWriteTime.ToString("o")
        }
    }
    return $copied
}

function Copy-NewestTelemetryFiles {
    param(
        [string]$SourceDir,
        [string]$DestinationDir,
        [int]$MaxCount
    )

    $copied = @()
    if (-not (Test-Path -LiteralPath $SourceDir)) {
        return $copied
    }
    New-Dir $DestinationDir | Out-Null
    $files = Get-ChildItem -LiteralPath $SourceDir -File -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -like "*.jsonl" -or $_.Name -like "*.jsonl.gz" } |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First $MaxCount
    foreach ($file in $files) {
        $dest = Join-Path $DestinationDir $file.Name
        Copy-Item -LiteralPath $file.FullName -Destination $dest -Force
        $copied += [PSCustomObject]@{
            source = $file.FullName
            copiedTo = $dest
            length = $file.Length
            lastWriteTime = $file.LastWriteTime.ToString("o")
        }
    }
    return $copied
}

function Copy-IfPresent {
    param(
        [string]$Path,
        [string]$DestinationDir
    )

    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path)) {
        return $null
    }
    New-Dir $DestinationDir | Out-Null
    $item = Get-Item -LiteralPath $Path
    $dest = Join-Path $DestinationDir $item.Name
    Copy-Item -LiteralPath $item.FullName -Destination $dest -Force
    return [PSCustomObject]@{
        source = $item.FullName
        copiedTo = $dest
        length = $item.Length
        lastWriteTime = $item.LastWriteTime.ToString("o")
    }
}

function Read-TextFileShared {
    param([string]$Path)

    $fs = [System.IO.File]::Open($Path, "Open", "Read", "ReadWrite")
    try {
        $reader = [System.IO.StreamReader]::new($fs, [System.Text.Encoding]::UTF8, $true)
        try {
            return $reader.ReadToEnd()
        } finally {
            $reader.Dispose()
        }
    } finally {
        $fs.Dispose()
    }
}

function Write-Jsonl {
    param(
        [string]$Path,
        [object[]]$Rows
    )

    $parent = Split-Path -Parent $Path
    if ($parent) { New-Dir $parent | Out-Null }
    $lines = @($Rows | ForEach-Object { $_ | ConvertTo-Json -Compress -Depth 20 })
    $lines | Set-Content -LiteralPath $Path -Encoding UTF8
}

function New-LogIndex {
    param(
        [string[]]$Paths,
        [string]$Kind,
        [string]$IndexPath
    )

    $rows = New-Object System.Collections.Generic.List[object]
    foreach ($path in $Paths) {
        if (-not (Test-Path -LiteralPath $path)) { continue }
        $content = Read-TextFileShared $path
        $lines = $content -split "`r?`n"
        for ($i = 0; $i -lt $lines.Count; $i++) {
            $line = $lines[$i]
            if ([string]::IsNullOrWhiteSpace($line)) { continue }
            $interesting = $line -match "MOTM|Observability|Dev command|WARN|ERROR|Exception|failed|auth|telemetry|world|joined|server|$([regex]::Escape($RunId))"
            if (-not $interesting) { continue }
            $rows.Add([PSCustomObject]@{
                kind = $Kind
                file = $path
                lineNumber = $i + 1
                offsetKind = "line"
                category = if ($line -match "ERROR|Exception|failed") { "error" } elseif ($line -match "WARN") { "warning" } elseif ($line -match "MOTM|Observability|Dev command") { "motm" } else { "lifecycle" }
                text = $line
            })
        }
    }
    Write-Jsonl -Path $IndexPath -Rows $rows
    return $rows.Count
}

function Read-TelemetryLines {
    param([string]$Path)

    $lines = New-Object System.Collections.Generic.List[string]
    if ($Path -notmatch "\.gz$") {
        foreach ($line in ((Read-TextFileShared $Path) -split "`r?`n")) {
            if (-not [string]::IsNullOrWhiteSpace($line)) {
                $lines.Add($line)
            }
        }
        return $lines
    }

    $fs = [System.IO.File]::OpenRead($Path)
    try {
        $gz = [System.IO.Compression.GzipStream]::new($fs, [System.IO.Compression.CompressionMode]::Decompress)
        try {
            $reader = [System.IO.StreamReader]::new($gz)
            try {
                while (($line = $reader.ReadLine()) -ne $null) {
                    $lines.Add($line)
                }
            } finally {
                $reader.Dispose()
            }
        } finally {
            $gz.Dispose()
        }
    } finally {
        $fs.Dispose()
    }
    return $lines
}

function New-TelemetryIndex {
    param(
        [string[]]$Paths,
        [string]$IndexPath
    )

    $rows = New-Object System.Collections.Generic.List[object]
    foreach ($path in $Paths) {
        if (-not (Test-Path -LiteralPath $path)) { continue }
        $lineNumber = 0
        foreach ($line in (Read-TelemetryLines $path)) {
            $lineNumber++
            if ([string]::IsNullOrWhiteSpace($line)) { continue }
            try {
                $json = $line | ConvertFrom-Json -ErrorAction Stop
                $rows.Add([PSCustomObject]@{
                    kind = "telemetry"
                    file = $path
                    lineNumber = $lineNumber
                    timestamp = $json.timestamp
                    sequence = $json.sequence
                    type = $json.type
                    eventName = $json.event_name
                    currentState = $json.current_state
                    fpsAvg = $json.performance.fps_avg
                    entityCount = $json.game.entity_count
                    loadedChunks = $json.game.loaded_chunks
                    rawLine = $line
                })
            } catch {
                $rows.Add([PSCustomObject]@{
                    kind = "telemetry"
                    file = $path
                    lineNumber = $lineNumber
                    parseError = $_.Exception.Message
                    rawLine = $line
                })
            }
        }
    }
    Write-Jsonl -Path $IndexPath -Rows $rows
    return $rows.Count
}

function Get-HytaleClientMetadata {
    param([string]$LatestRoot)

    $clientDir = Join-Path $LatestRoot "Client"
    $macApp = Join-Path $clientDir "Hytale.app"
    $windowsExe = Join-Path $clientDir "HytaleClient.exe"
    $clientPath = if (Test-IsMacOS) { $macApp } else { $windowsExe }

    if ((Test-IsMacOS) -and (Test-Path -LiteralPath $macApp)) {
        $plist = Join-Path $macApp "Contents/Info.plist"
        $shortVersion = $null
        $bundleVersion = $null
        try {
            $shortVersion = (& /usr/libexec/PlistBuddy -c "Print :CFBundleShortVersionString" $plist 2>$null)
            $bundleVersion = (& /usr/libexec/PlistBuddy -c "Print :CFBundleVersion" $plist 2>$null)
        } catch {
        }
        return [ordered]@{
            platform = "macos"
            clientPath = $macApp
            present = $true
            bundleShortVersion = $shortVersion
            bundleVersion = $bundleVersion
            infoPlist = $plist
        }
    }

    if (Test-Path -LiteralPath $windowsExe) {
        $item = Get-Item -LiteralPath $windowsExe
        return [ordered]@{
            platform = "windows"
            clientPath = $windowsExe
            present = $true
            fileVersion = $item.VersionInfo.FileVersion
            productVersion = $item.VersionInfo.ProductVersion
        }
    }

    return [ordered]@{
        platform = if (Test-IsMacOS) { "macos" } else { "windows-or-other" }
        clientPath = $clientPath
        present = $false
    }
}

function Get-NewestBuiltJar {
    $libs = Join-Path $repoRoot "build/libs"
    if (-not (Test-Path -LiteralPath $libs)) {
        return $null
    }
    return Get-ChildItem -LiteralPath $libs -Filter "*.jar" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Read-JsonlObjects {
    param([string]$Path)

    $objects = New-Object System.Collections.Generic.List[object]
    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path)) {
        return $objects
    }
    foreach ($line in ((Read-TextFileShared $Path) -split "`r?`n")) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        try {
            $objects.Add(($line | ConvertFrom-Json -ErrorAction Stop))
        } catch {
            $objects.Add([PSCustomObject]@{
                parseError = $_.Exception.Message
                rawLine = $line
            })
        }
    }
    return $objects
}

function Copy-DirectoryIfPresent {
    param(
        [string]$Path,
        [string]$Destination
    )

    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path)) {
        return $null
    }
    if (Test-Path -LiteralPath $Destination) {
        Remove-Item -LiteralPath $Destination -Recurse -Force
    }
    New-Dir (Split-Path -Parent $Destination) | Out-Null
    Copy-Item -LiteralPath $Path -Destination $Destination -Recurse -Force
    return [PSCustomObject]@{
        source = $Path
        copiedTo = $Destination
    }
}

$outDirResolved = New-Dir $OutDir
$rawDir = New-Dir (Join-Path $outDirResolved "raw")
$indexDir = New-Dir (Join-Path $outDirResolved "indexes")
$externalDir = New-Dir (Join-Path $outDirResolved "external")

$hytaleRoot = Join-Path $env:APPDATA "Hytale"
$userData = Join-Path $hytaleRoot "UserData"
$latestRoot = Join-Path $hytaleRoot "install/release/package/game/latest"
$clientLogDir = Join-Path $userData "Logs"
$telemetryDir = Join-Path $userData "Telemetry"
$worldRoot = Join-Path $userData (Join-Path "Saves" $WorldName)
$serverLogDir = Join-Path $worldRoot "logs"
$settingsPath = Join-Path $userData "Settings.json"
$serverJar = Join-Path $latestRoot "Server/HytaleServer.jar"
$assetsZip = Join-Path $latestRoot "Assets.zip"
$clientMetadata = Get-HytaleClientMetadata $latestRoot
$modsDir = Join-Path $userData "Mods"
$installedJar = Get-ChildItem -LiteralPath $modsDir -Filter "mentees_of_the_mystical-*.jar" -File -ErrorAction SilentlyContinue |
    Sort-Object LastWriteTime -Descending |
    Select-Object -First 1

$motmDataDir = Resolve-MotmDataDir $DataDir
$copied = New-Object System.Collections.Generic.List[object]
$copied.AddRange(@(Copy-NewestFiles -SourceDir $clientLogDir -Filter "*_client.log" -DestinationDir (Join-Path $rawDir "client-logs") -MaxCount $MaxClientLogs))
$copied.AddRange(@(Copy-NewestFiles -SourceDir $serverLogDir -Filter "*_server.log" -DestinationDir (Join-Path $rawDir "server-logs") -MaxCount $MaxServerLogs))
$copied.AddRange(@(Copy-NewestTelemetryFiles -SourceDir $telemetryDir -DestinationDir (Join-Path $rawDir "telemetry") -MaxCount $MaxTelemetryFiles))
$settingsCopy = Copy-IfPresent -Path $settingsPath -DestinationDir (Join-Path $rawDir "settings")
if ($settingsCopy) { $copied.Add($settingsCopy) }

if (-not [string]::IsNullOrWhiteSpace($motmDataDir)) {
    foreach ($name in @("motm-server.properties", "motm-preflight-report.txt", "dev-command-outbox.log")) {
        $copy = Copy-IfPresent -Path (Join-Path $motmDataDir $name) -DestinationDir (Join-Path $rawDir "motm-data")
        if ($copy) { $copied.Add($copy) }
    }
    $motmRunDir = Join-Path $motmDataDir (Join-Path "observability/runs" $RunId)
    $motmRunCopy = Copy-DirectoryIfPresent -Path $motmRunDir -Destination (Join-Path $rawDir "motm-observability")
    if ($motmRunCopy) { $copied.Add($motmRunCopy) }
}

$clientLogCopies = @(Get-ChildItem -LiteralPath (Join-Path $rawDir "client-logs") -Filter "*.log" -File -ErrorAction SilentlyContinue | ForEach-Object FullName)
$serverLogCopies = @(Get-ChildItem -LiteralPath (Join-Path $rawDir "server-logs") -Filter "*.log" -File -ErrorAction SilentlyContinue | ForEach-Object FullName)
$telemetryCopies = @(Get-ChildItem -LiteralPath (Join-Path $rawDir "telemetry") -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Name -like "*.jsonl" -or $_.Name -like "*.jsonl.gz" } |
    ForEach-Object FullName)

$clientIndexCount = New-LogIndex -Paths $clientLogCopies -Kind "client-log" -IndexPath (Join-Path $indexDir "client-log-index.jsonl")
$serverIndexCount = New-LogIndex -Paths $serverLogCopies -Kind "server-log" -IndexPath (Join-Path $indexDir "server-log-index.jsonl")
$telemetryIndexCount = New-TelemetryIndex -Paths $telemetryCopies -IndexPath (Join-Path $indexDir "telemetry-index.jsonl")

$gitHead = (& git -C $repoRoot rev-parse HEAD 2>$null)
$gitStatus = @(& git -C $repoRoot status --short 2>$null)
$builtJar = Get-NewestBuiltJar
$hashes = [ordered]@{}
foreach ($path in @($serverJar, $assetsZip, ($installedJar ? $installedJar.FullName : ""), ($builtJar ? $builtJar.FullName : ""))) {
    if (-not [string]::IsNullOrWhiteSpace($path) -and (Test-Path -LiteralPath $path)) {
        $hashes[$path] = (Get-FileHash -LiteralPath $path -Algorithm SHA256).Hash
    }
}

$controlRequests = Read-JsonlObjects (Join-Path $outDirResolved "control-requests.jsonl")
$motmControl = Read-JsonlObjects (Join-Path (Join-Path $rawDir "motm-observability") "control.jsonl")
$commandEvents = @($controlRequests | Where-Object { $_.plane -eq "control" -and $_.data.command } | ForEach-Object {
    [PSCustomObject]@{
        timestamp = $_.timestamp
        traceId = $_.traceId
        scenarioId = $_.scenarioId
        type = $_.type
        command = $_.data.command
        failed = $_.data.failed
    }
})

$manifest = [ordered]@{
    runId = $RunId
    phase = $Phase
    createdAt = (Get-Date).ToUniversalTime().ToString("o")
    outDir = $outDirResolved
    worldName = $WorldName
    appData = $env:APPDATA
    hytaleRoot = $hytaleRoot
    motmDataDir = $motmDataDir
    hytale = $clientMetadata
    installedJar = if ($installedJar) { $installedJar.FullName } else { $null }
    builtJar = if ($builtJar) { $builtJar.FullName } else { $null }
    builtJarMatchesInstalled = if ($installedJar -and $builtJar -and $hashes.Contains($installedJar.FullName) -and $hashes.Contains($builtJar.FullName)) {
        $hashes[$installedJar.FullName] -eq $hashes[$builtJar.FullName]
    } else {
        $null
    }
    serverJar = $serverJar
    assetsZip = $assetsZip
    clientPath = $clientMetadata.clientPath
    gitHead = $gitHead
    gitStatus = $gitStatus
    hashes = $hashes
    copiedSources = $copied
    commands = [ordered]@{
        requestEventCount = $controlRequests.Count
        motmControlEventCount = $motmControl.Count
        events = $commandEvents
    }
    indexes = [ordered]@{
        clientLogRows = $clientIndexCount
        serverLogRows = $serverIndexCount
        telemetryRows = $telemetryIndexCount
    }
}
$manifestPath = Join-Path $outDirResolved "manifest.json"
$manifest | ConvertTo-Json -Depth 40 | Set-Content -LiteralPath $manifestPath -Encoding UTF8

$report = @(
    "# Agent Observability Evidence Bundle",
    "",
    "- RunId: $RunId",
    "- World: $WorldName",
    "- OutDir: $outDirResolved",
    "- MOTM data: $motmDataDir",
    "- Copied sources: $($copied.Count)",
    "- Client log index rows: $clientIndexCount",
    "- Server log index rows: $serverIndexCount",
    "- Telemetry index rows: $telemetryIndexCount",
    "",
    "PASS"
)
$reportPath = Join-Path $outDirResolved "report.md"
$report | Set-Content -LiteralPath $reportPath -Encoding UTF8

Write-Host "[collect-observability-evidence] PASS report: $reportPath"
