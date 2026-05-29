param(
    [switch]$BuildOnly,
    [switch]$PublicRelease,
    [string]$JavaHome = "",
    [string]$HytaleRoot = ""
)

$ErrorActionPreference = "Stop"

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$setupId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
$diagDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path "setup-diagnostics" "agent-bootstrap-$setupId"))
New-Item -ItemType Directory -Path $diagDir -Force | Out-Null
$transcriptPath = Join-Path $diagDir "transcript.txt"

function Resolve-CurrentPowerShell {
    if ($env:OS -eq "Windows_NT") {
        $powershell = Get-Command powershell -ErrorAction SilentlyContinue
        if ($powershell) { return $powershell.Source }
    }

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
    throw "Could not locate powershell or pwsh."
}

function Invoke-ChildScript {
    param(
        [string]$ScriptPath,
        [string[]]$Arguments = @()
    )

    $powerShell = Resolve-CurrentPowerShell
    Write-Host "[setup-agent-workstation] Running: $powerShell -NoProfile -ExecutionPolicy Bypass -File $ScriptPath $($Arguments -join ' ')"
    & $powerShell -NoProfile -ExecutionPolicy Bypass -File $ScriptPath @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "$ScriptPath failed with exit code $LASTEXITCODE."
    }
}

Start-Transcript -LiteralPath $transcriptPath -Force | Out-Null
try {
    Write-Host "[setup-agent-workstation] Repository: $repoRoot"
    Write-Host "[setup-agent-workstation] Diagnostics: $diagDir"

    $buildArgs = @()
    if ($BuildOnly) { $buildArgs += "-BuildOnly" }
    if ($PublicRelease) { $buildArgs += "-PublicRelease" }
    if (-not [string]::IsNullOrWhiteSpace($JavaHome)) { $buildArgs += @("-JavaHome", $JavaHome) }
    if (-not [string]::IsNullOrWhiteSpace($HytaleRoot)) { $buildArgs += @("-HytaleRoot", $HytaleRoot) }

    Invoke-ChildScript -ScriptPath (Join-Path $PSScriptRoot "build-install.ps1") -Arguments $buildArgs

    $diagArgs = @()
    if (-not [string]::IsNullOrWhiteSpace($JavaHome)) { $diagArgs += @("-JavaHome", $JavaHome) }
    if (-not [string]::IsNullOrWhiteSpace($HytaleRoot)) { $diagArgs += @("-HytaleRoot", $HytaleRoot) }
    Invoke-ChildScript -ScriptPath (Join-Path $PSScriptRoot "diagnose-dev-environment.ps1") -Arguments $diagArgs

    $successPath = Join-Path $diagDir "SETUP-SUCCESS.txt"
    @(
        "MOTM agent workstation setup succeeded.",
        "Next runtime step:",
        "1. Launch Hytale through the official launcher.",
        "2. Enter the target world.",
        "3. Run scripts/run-agent-observability-baseline.ps1 -WorldName Main."
    ) | Set-Content -LiteralPath $successPath -Encoding UTF8

    Write-Host ""
    Write-Host "[setup-agent-workstation] SUCCESS"
    Write-Host "[setup-agent-workstation] Wrote $successPath"
} catch {
    $failurePath = Join-Path $diagDir "SETUP-FAILURE.txt"
    @(
        "MOTM agent workstation setup failed.",
        "",
        "Error:",
        $_.Exception.Message,
        "",
        "What the agent should do next:",
        "1. Read this directory, especially transcript.txt and any nested setup-context.json/setup-error.txt files.",
        "2. Run scripts/diagnose-dev-environment.ps1 to confirm the current failure mode.",
        "3. Fix the concrete blocker, such as launching Hytale once, setting -HytaleRoot, clearing a bad JAVA_HOME, or retrying a failed download.",
        "4. Rerun scripts/setup-agent-workstation.ps1."
    ) | Set-Content -LiteralPath $failurePath -Encoding UTF8

    Write-Host ""
    Write-Host "[setup-agent-workstation] FAILED"
    Write-Host "[setup-agent-workstation] Diagnostics: $diagDir"
    Write-Host "[setup-agent-workstation] Wrote $failurePath"
    Write-Host "[setup-agent-workstation] Error: $($_.Exception.Message)"
    exit 1
} finally {
    Stop-Transcript | Out-Null
}
