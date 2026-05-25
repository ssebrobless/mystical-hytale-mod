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

$motmDataDir = Resolve-MotmDataDir -WorldName $WorldName -DataDir $DataDir
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
    throw "Timed out acquiring dev command bridge lock: $lockPath"
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
throw "Timed out waiting for dev command result: /motm $normalized"
} finally {
    if ($lockStream) {
        $lockStream.Dispose()
    }
}
