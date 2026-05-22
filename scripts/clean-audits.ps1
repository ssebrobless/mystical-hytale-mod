param(
    [string]$AuditRoot,
    [int]$KeepLatest = 3,
    [switch]$WhatIf
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($AuditRoot)) {
    $repoRoot = Split-Path -Parent $PSScriptRoot
    $AuditRoot = Join-Path $repoRoot "audits"
}

function Remove-Path([string]$Path, [string]$Reason) {
    if ($WhatIf) {
        Write-Host "[clean-audits] WOULD remove: $Path ($Reason)"
        return
    }
    Write-Host "[clean-audits] Removing: $Path ($Reason)"
    Remove-Item -LiteralPath $Path -Recurse -Force -ErrorAction SilentlyContinue
}

function Test-Utf16Likely([string]$Path) {
    $bytes = [System.IO.File]::ReadAllBytes($Path)
    $sampleLen = [Math]::Min($bytes.Length, 200000)
    $nulCount = 0
    for ($i = 0; $i -lt $sampleLen; $i++) {
        if ($bytes[$i] -eq 0) {
            $nulCount++
        }
    }
    return $nulCount -gt [Math]::Max(10, [int]($sampleLen / 10))
}

if (-not (Test-Path -LiteralPath $AuditRoot)) {
    Write-Host "[clean-audits] No audits directory at $AuditRoot"
    exit 0
}

$harnessRoot = Join-Path $AuditRoot "harness"
if (Test-Path -LiteralPath $harnessRoot) {
    Get-ChildItem -LiteralPath $harnessRoot -Directory | ForEach-Object {
        $timestamped = Get-ChildItem -LiteralPath $_.FullName -Directory |
            Where-Object { $_.Name -match '^\d{4}-\d{2}-\d{2}T\d{2}-\d{2}-\d{2}$' } |
            Sort-Object Name -Descending

        $timestamped |
            Select-Object -Skip $KeepLatest |
            ForEach-Object { Remove-Path $_.FullName "retention keep latest $KeepLatest under $($_.Parent.FullName)" }
    }
}

$textExtensions = @(".txt", ".md", ".log", ".json", ".csv", ".ps1")
Get-ChildItem -LiteralPath $AuditRoot -Recurse -File -ErrorAction SilentlyContinue |
    Where-Object { $_.Length -gt 100KB -and $textExtensions -contains $_.Extension.ToLowerInvariant() } |
    ForEach-Object {
        if (Test-Utf16Likely $_.FullName) {
            Remove-Path $_.FullName "UTF-16/NUL-interleaved text dump >100KB"
        }
    }

Write-Host "[clean-audits] PASS"
