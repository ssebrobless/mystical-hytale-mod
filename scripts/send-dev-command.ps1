param(
    [Parameter(Mandatory = $true)]
    [string]$Command,
    [string]$WorldName = "MOTM Creative Test",
    [string]$DataDir = "",
    [int]$TimeoutMilliseconds = 3500
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

    $worldRoot = Join-Path $env:APPDATA ("Hytale\UserData\Saves\" + $WorldName)
    $motmRoot = Join-Path $worldRoot "motm-data"
    $config = Get-ChildItem -LiteralPath $motmRoot -Recurse -Filter "motm-server.properties" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if ($config) {
        return $config.Directory.FullName
    }

    $fallbackRoot = Join-Path $env:APPDATA "Hytale\UserData"
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

$motmDataDir = Resolve-MotmDataDir -WorldName $WorldName -DataDir $DataDir
$inbox = Join-Path $motmDataDir "dev-command-inbox.txt"
$outbox = Join-Path $motmDataDir "dev-command-outbox.log"
$normalized = Normalize-CommandForMatch $Command
$beforeLength = if (Test-Path -LiteralPath $outbox) { (Get-Item -LiteralPath $outbox).Length } else { 0L }

Set-Content -LiteralPath $inbox -Value $Command -Encoding UTF8
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

    if ($tail -match [regex]::Escape("command=/motm $normalized") -or $tail -match "Dev command inbox skipped|Dev command inbox failed") {
        $tail.Trim() -split "`r?`n" | Where-Object { $_ } | Select-Object -Last 3 | ForEach-Object {
            Write-Host "[send-dev-command] $_"
        }
        exit 0
    }
}

throw "Timed out waiting for dev command result: /motm $normalized"
