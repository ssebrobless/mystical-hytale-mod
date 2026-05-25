param(
    [switch]$NoDownload,
    [string]$JavaHome = "",
    [string]$HytaleRoot = ""
)

$ErrorActionPreference = "Stop"

$ensureArgs = @{
    DiagnoseOnly = $true
}
if ($NoDownload) {
    $ensureArgs.NoDownload = $true
}
if (-not [string]::IsNullOrWhiteSpace($JavaHome)) {
    $ensureArgs.JavaHome = $JavaHome
}
if (-not [string]::IsNullOrWhiteSpace($HytaleRoot)) {
    $ensureArgs.HytaleRoot = $HytaleRoot
}

& (Join-Path $PSScriptRoot "ensure-dev-environment.ps1") @ensureArgs
exit $LASTEXITCODE
