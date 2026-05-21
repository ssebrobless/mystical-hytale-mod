param(
    [string]$Pattern,
    [int]$TimeoutSeconds = 30,
    [string]$SaveName = "MOTM Creative Test",
    [string]$LogDir
)

$ErrorActionPreference = "Stop"

if (-not $LogDir) {
    $LogDir = Join-Path $env:APPDATA ("Hytale\UserData\Saves\" + $SaveName + "\logs")
}

$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$log = $null
while ((Get-Date) -lt $deadline -and -not $log) {
    $log = Get-ChildItem -Path $LogDir -Filter "*_server.log" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
    if (-not $log) { Start-Sleep -Milliseconds 250 }
}
if (-not $log) { throw "No server log under $LogDir" }

Write-Host "[tail-log] Watching $($log.FullName)"
$start = $log.Length
$fs = [System.IO.File]::Open($log.FullName, "Open", "Read", "ReadWrite")
try {
    $fs.Position = $start
    $reader = New-Object System.IO.StreamReader($fs)
    while ((Get-Date) -lt $deadline) {
        $line = $reader.ReadLine()
        if ($null -eq $line) {
            Start-Sleep -Milliseconds 200
            continue
        }
        if ([string]::IsNullOrEmpty($Pattern) -or $line -match $Pattern) {
            Write-Output $line
            if (-not [string]::IsNullOrEmpty($Pattern)) { exit 0 }
        }
    }
    if ([string]::IsNullOrEmpty($Pattern)) { exit 0 }
    throw "Timeout waiting for pattern: $Pattern"
} finally {
    $fs.Dispose()
}
