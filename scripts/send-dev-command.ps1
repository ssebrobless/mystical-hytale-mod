param(
    [Parameter(Mandatory = $true)]
    [string]$Command,
    [string]$WorldName = "MOTM Creative Test",
    [string]$DataDir = "",
    [int]$TimeoutMilliseconds = 3500,
    [string]$RunDir = "",
    [string]$TraceId = "",
    [string]$ScenarioId = ""
)

$ErrorActionPreference = "Stop"

if (($global:IsMacOS -eq $true) -and [string]::IsNullOrWhiteSpace($env:APPDATA)) {
    $env:APPDATA = Join-Path $HOME "Library/Application Support"
}

function Resolve-MotmDataDir {
    param([string]$WorldName, [string]$DataDir)

    if (-not [string]::IsNullOrWhiteSpace($DataDir)) {
        if (-not (Test-Path -LiteralPath $DataDir)) {
            throw "MOTM data directory not found: $DataDir"
        }
        return (Resolve-Path -LiteralPath $DataDir).Path
    }

    $worldRoot = Join-Path (Join-Path (Join-Path $env:APPDATA "Hytale") "UserData") (Join-Path "Saves" $WorldName)
    $motmRoot = Join-Path $worldRoot "motm-data"
    $config = Get-ChildItem -LiteralPath $motmRoot -Recurse -Filter "motm-server.properties" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($config) {
        return $config.Directory.FullName
    }

    $fallbackRoot = Join-Path (Join-Path $env:APPDATA "Hytale") "UserData"
    $config = Get-ChildItem -LiteralPath $fallbackRoot -Recurse -Filter "motm-server.properties" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($config) {
        return $config.Directory.FullName
    }

    throw "Could not locate motm-server.properties under $worldRoot or $fallbackRoot. Launch the world once after installing the mod."
}

function Normalize-CommandForMatch {
    param([string]$Text)

    $normalized = $Text.Trim()
    if ($normalized.StartsWith("/")) {
        $normalized = $normalized.Substring(1).Trim()
    }
    if ($normalized.StartsWith("motm ", [System.StringComparison]::OrdinalIgnoreCase)) {
        $normalized = $normalized.Substring(5).Trim()
    }
    return $normalized
}

function Write-ControlEvent {
    param(
        [string]$Type,
        [object]$Data
    )

    if ([string]::IsNullOrWhiteSpace($RunDir)) {
        return
    }
    New-Item -ItemType Directory -Path $RunDir -Force | Out-Null
    if ([string]::IsNullOrWhiteSpace($script:traceId)) {
        $script:traceId = "shell-" + ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())
    }
    $event = [ordered]@{
        timestamp = (Get-Date).ToUniversalTime().ToString("o")
        epochMillis = [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds()
        traceId = $script:traceId
        scenarioId = $ScenarioId
        plane = "control"
        type = $Type
        data = $Data
    }
    $event | ConvertTo-Json -Compress -Depth 20 |
        Add-Content -LiteralPath (Join-Path $RunDir "control-requests.jsonl") -Encoding UTF8
}

function Read-SharedFileTailLines {
    param(
        [string]$Path,
        [int]$MaxBytes = 32768,
        [int]$MaxLines = 40
    )

    if ([string]::IsNullOrWhiteSpace($Path) -or -not (Test-Path -LiteralPath $Path)) {
        return @()
    }

    $stream = $null
    try {
        $stream = [System.IO.File]::Open(
            $Path,
            [System.IO.FileMode]::Open,
            [System.IO.FileAccess]::Read,
            [System.IO.FileShare]::ReadWrite)
        $bytesToRead = [Math]::Min([int64]$MaxBytes, $stream.Length)
        if ($bytesToRead -le 0) {
            return @()
        }
        $buffer = New-Object byte[] ([int]$bytesToRead)
        $stream.Position = $stream.Length - $bytesToRead
        [void]$stream.Read($buffer, 0, [int]$bytesToRead)
        $text = [System.Text.Encoding]::UTF8.GetString($buffer)
        $lines = @($text -split "`r?`n" | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
        if ($lines.Count -gt $MaxLines) {
            return @($lines | Select-Object -Last $MaxLines)
        }
        return $lines
    } catch {
        return @("Could not read shared file tail: $($_.Exception.Message)")
    } finally {
        if ($stream) {
            $stream.Dispose()
        }
    }
}

function Resolve-HytaleRootForDiagnostic {
    if (-not [string]::IsNullOrWhiteSpace($env:HYTALE_ROOT)) {
        return $env:HYTALE_ROOT
    }
    if (-not [string]::IsNullOrWhiteSpace($env:APPDATA)) {
        $candidate = Join-Path $env:APPDATA "Hytale"
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }
    if ($global:IsMacOS -eq $true) {
        $candidate = Join-Path $HOME "Library/Application Support/Hytale"
        if (Test-Path -LiteralPath $candidate) {
            return $candidate
        }
    }
    return ""
}

function Get-RecentLogSummary {
    $hytaleRoot = Resolve-HytaleRootForDiagnostic
    if ([string]::IsNullOrWhiteSpace($hytaleRoot)) {
        return $null
    }

    $logsDir = Join-Path $hytaleRoot "UserData/Logs"
    if (-not (Test-Path -LiteralPath $logsDir)) {
        return [ordered]@{
            logsDir = $logsDir
            exists = $false
        }
    }

    $latest = Get-ChildItem -LiteralPath $logsDir -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $latest) {
        return [ordered]@{
            logsDir = $logsDir
            exists = $true
            latestLog = $null
        }
    }

    $tail = @(Read-SharedFileTailLines -Path $latest.FullName -MaxBytes 32768 -MaxLines 40)

    return [ordered]@{
        logsDir = $logsDir
        exists = $true
        latestLog = $latest.FullName
        latestLogLastWriteTime = $latest.LastWriteTime.ToString("o")
        latestLogTail = $tail
    }
}

function Get-HytaleProcessSummary {
    $matches = @()
    try {
        $matches = @(Get-Process -ErrorAction Stop | Where-Object {
            $_.ProcessName -match '(?i)hytale'
        } | Select-Object -First 10 | ForEach-Object {
            $startTime = $null
            $path = $null
            try { $startTime = $_.StartTime.ToString("o") } catch { }
            try { $path = $_.Path } catch { }
            [ordered]@{
                id = $_.Id
                processName = $_.ProcessName
                startTime = $startTime
                path = $path
            }
        })
    } catch {
        return [ordered]@{
            error = $_.Exception.Message
            matches = @()
        }
    }

    return [ordered]@{
        count = $matches.Count
        matches = $matches
    }
}

function New-DevCommandDiagnostic {
    param(
        [string]$FailureType,
        [string]$Message,
        [string]$MotmDataDir = "",
        [string]$Inbox = "",
        [string]$Outbox = "",
        [Int64]$BeforeLength = 0
    )

    $outboxTail = @()
    $outboxLength = $null
    $outboxLastWrite = $null
    if (-not [string]::IsNullOrWhiteSpace($Outbox) -and (Test-Path -LiteralPath $Outbox)) {
        try {
            $outboxItem = Get-Item -LiteralPath $Outbox
            $outboxLength = $outboxItem.Length
            $outboxLastWrite = $outboxItem.LastWriteTime.ToString("o")
            $outboxTail = @(Read-SharedFileTailLines -Path $Outbox -MaxBytes 32768 -MaxLines 30)
        } catch {
            $outboxTail = @("Could not read outbox tail: $($_.Exception.Message)")
        }
    }

    $diagnostic = [ordered]@{
        timestamp = (Get-Date).ToUniversalTime().ToString("o")
        failureType = $FailureType
        message = $Message
        command = "/motm $(Normalize-CommandForMatch $Command)"
        worldName = $WorldName
        scenarioId = $ScenarioId
        runDir = $RunDir
        motmDataDir = $MotmDataDir
        inbox = $Inbox
        outbox = $Outbox
        outboxBeforeLength = $BeforeLength
        outboxCurrentLength = $outboxLength
        outboxLastWriteTime = $outboxLastWrite
        appdata = $env:APPDATA
        hytaleRoot = Resolve-HytaleRootForDiagnostic
        hytaleProcesses = Get-HytaleProcessSummary
        latestClientLog = Get-RecentLogSummary
        recommendedNextSteps = @(
            "Launch or restart Hytale through the official launcher after installing the internal MOTM jar.",
            "Enter the requested world '$WorldName' and wait until MOTM writes motm-server.properties.",
            "If -SkipBuild was used, rerun without -SkipBuild or run scripts/build-install.ps1, then restart Hytale onto the new jar.",
            "Inspect dev-command-outbox.log and the latest Hytale client log tail in this diagnostic.",
            "If the wrong world/data folder is selected, rerun with -WorldName or -DataDir pointing at the active motm-data directory."
        )
        outboxTail = $outboxTail
    }

    if (-not [string]::IsNullOrWhiteSpace($RunDir)) {
        New-Item -ItemType Directory -Path $RunDir -Force | Out-Null
        $jsonPath = Join-Path $RunDir "dev-command-diagnostic.json"
        $mdPath = Join-Path $RunDir "dev-command-diagnostic.md"
        $diagnostic["diagnosticJson"] = $jsonPath
        $diagnostic["diagnosticMarkdown"] = $mdPath
        $diagnostic | ConvertTo-Json -Depth 30 | Set-Content -LiteralPath $jsonPath -Encoding UTF8
        @(
            "# Dev Command Diagnostic",
            "",
            "- FailureType: $FailureType",
            "- Command: $($diagnostic.command)",
            "- WorldName: $WorldName",
            "- ScenarioId: $ScenarioId",
            "- MOTM data dir: $MotmDataDir",
            "- Inbox: $Inbox",
            "- Outbox: $Outbox",
            "- Hytale process count: $($diagnostic.hytaleProcesses.count)",
            "- JSON: $jsonPath",
            "",
            "## Recommended Next Steps",
            ""
        ) + ($diagnostic.recommendedNextSteps | ForEach-Object { "- $_" }) + @(
            "",
            "## Error",
            "",
            $Message
        ) | Set-Content -LiteralPath $mdPath -Encoding UTF8
    }

    Write-ControlEvent -Type "dev_command_diagnostic" -Data $diagnostic
    return $diagnostic
}

try {
    $motmDataDir = Resolve-MotmDataDir -WorldName $WorldName -DataDir $DataDir
} catch {
    $diagnostic = New-DevCommandDiagnostic `
        -FailureType "data_dir_not_found" `
        -Message $_.Exception.Message
    $detail = if ($diagnostic.diagnosticMarkdown) { " Diagnostic: $($diagnostic.diagnosticMarkdown)" } else { "" }
    throw "$($_.Exception.Message)$detail"
}
$inbox = Join-Path $motmDataDir "dev-command-inbox.txt"
$outbox = Join-Path $motmDataDir "dev-command-outbox.log"
$normalized = Normalize-CommandForMatch $Command
$beforeLength = if (Test-Path -LiteralPath $outbox) { (Get-Item -LiteralPath $outbox).Length } else { 0L }
$script:traceId = if ([string]::IsNullOrWhiteSpace($TraceId)) {
    "shell-" + ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())
} else {
    $TraceId
}

$lockPath = Join-Path $motmDataDir ".dev-command-inbox.lock"
$lockStream = $null
$lockDeadline = (Get-Date).AddMilliseconds([Math]::Max($TimeoutMilliseconds, 5000))
while ($null -eq $lockStream -and (Get-Date) -lt $lockDeadline) {
    try {
        $lockStream = [System.IO.File]::Open(
            $lockPath,
            [System.IO.FileMode]::OpenOrCreate,
            [System.IO.FileAccess]::ReadWrite,
            [System.IO.FileShare]::None)
    } catch [System.IO.IOException] {
        Start-Sleep -Milliseconds 100
    }
}
if ($null -eq $lockStream) {
    $diagnostic = New-DevCommandDiagnostic `
        -FailureType "lock_timeout" `
        -Message "Timed out acquiring dev command bridge lock: $lockPath" `
        -MotmDataDir $motmDataDir `
        -Inbox $inbox `
        -Outbox $outbox `
        -BeforeLength $beforeLength
    $detail = if ($diagnostic.diagnosticMarkdown) { " Diagnostic: $($diagnostic.diagnosticMarkdown)" } else { "" }
    throw "Timed out acquiring dev command bridge lock: $lockPath.$detail"
}

try {
Set-Content -LiteralPath $inbox -Value $Command -Encoding UTF8
Write-ControlEvent -Type "dev_command_queued" -Data ([ordered]@{
    command = "/motm $normalized"
    inbox = $inbox
    outbox = $outbox
    beforeLength = $beforeLength
})
Write-Host "[send-dev-command] queued: /motm $normalized"
Write-Host "[send-dev-command] inbox: $inbox"

$deadline = (Get-Date).AddMilliseconds($TimeoutMilliseconds)
while ((Get-Date) -lt $deadline) {
    Start-Sleep -Milliseconds 150
    if (-not (Test-Path -LiteralPath $outbox)) {
        continue
    }

    $fs = [System.IO.File]::Open($outbox, "Open", "Read", "ReadWrite")
    try {
        $fs.Position = [Math]::Min($beforeLength, $fs.Length)
        $reader = New-Object System.IO.StreamReader($fs, [System.Text.Encoding]::UTF8)
        $tail = $reader.ReadToEnd()
    } finally {
        if ($reader) { $reader.Dispose() }
        $fs.Dispose()
    }

    $matchingLines = @($tail.Trim() -split "`r?`n" | Where-Object {
        $_ -and ($_ -match [regex]::Escape("command=/motm $normalized"))
    })
    if ($matchingLines.Count -gt 0) {
        $failed = @($matchingLines | Where-Object { $_ -match "Dev command inbox skipped|Dev command inbox failed" })
        Write-ControlEvent -Type "dev_command_result" -Data ([ordered]@{
            command = "/motm $normalized"
            matchedTail = ($matchingLines -join "`n")
            failed = $failed.Count -gt 0
        })
        $matchingLines | Select-Object -Last 3 | ForEach-Object {
            Write-Host "[send-dev-command] $_"
        }
        if ($failed.Count -gt 0) {
            exit 1
        }
        exit 0
    }
}

Write-ControlEvent -Type "dev_command_timeout" -Data ([ordered]@{
    command = "/motm $normalized"
    timeoutMilliseconds = $TimeoutMilliseconds
})
$diagnostic = New-DevCommandDiagnostic `
    -FailureType "command_timeout" `
    -Message "Timed out waiting for dev command result: /motm $normalized" `
    -MotmDataDir $motmDataDir `
    -Inbox $inbox `
    -Outbox $outbox `
    -BeforeLength $beforeLength
$detail = if ($diagnostic.diagnosticMarkdown) { " Diagnostic: $($diagnostic.diagnosticMarkdown)" } else { "" }
throw "Timed out waiting for dev command result: /motm $normalized.$detail"
} finally {
    if ($lockStream) {
        $lockStream.Dispose()
    }
}
