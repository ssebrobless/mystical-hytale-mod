param(
    [string]$StyleId = "metal",
    [string]$WorldName = "MOTM Creative Test",
    [ValidateSet("creative", "adventure")]
    [string]$Mode = "creative",
    [ValidateSet("stationary", "close", "cluster", "line", "surround", "clear")]
    [string]$MobMode = "stationary",
    [switch]$SkipRelocate,
    [switch]$SkipThirdPerson,
    [switch]$SkipFinalFocus
)

$ErrorActionPreference = "Stop"

function Send-MotmCommand([string]$Text, [int]$DelayMilliseconds = 650) {
    & (Join-Path $PSScriptRoot "send-dev-command.ps1") `
        -Command $Text `
        -WorldName $WorldName `
        -TimeoutMilliseconds ([Math]::Max(10000, $DelayMilliseconds + 4000)) | Out-Host
    Start-Sleep -Milliseconds $DelayMilliseconds
}

Write-Host "[setup-terra-review] Preparing Terra/$StyleId manual review."
Write-Host "[setup-terra-review] Mode=$Mode MobMode=$MobMode"

Send-MotmCommand "motm dev mode creative" 800
Send-MotmCommand "motm dev daylight" 450
if (-not $SkipRelocate) {
    Send-MotmCommand "motm dev relocate lane" 1600
}
Send-MotmCommand "motm dev test reset" 1300
Send-MotmCommand "motm dev kit terra" 1200
Send-MotmCommand "motm dev freecast on" 450
Send-MotmCommand "motm dev class set terra" 650
Send-MotmCommand "motm dev styles clear" 450
Send-MotmCommand "motm style $StyleId" 1100
Send-MotmCommand "motm dev test reset" 1300
Send-MotmCommand "motm dev test mobs clear" 900
if ($MobMode -ne "clear") {
    Send-MotmCommand "motm dev test mobs $MobMode" 1350
}
Send-MotmCommand "motm dev test mobs count" 450
Send-MotmCommand "motm dev position" 450

if (-not $SkipThirdPerson) {
    & (Join-Path $PSScriptRoot "verify-third-person.ps1") -Phase "terra-manual-review" -Name $StyleId -TryToggle | Out-Host
}

if ($Mode -eq "adventure") {
    Send-MotmCommand "motm dev mode adventure" 800
}
Send-MotmCommand "motm dev freecast on" 450

if (-not $SkipFinalFocus) {
    & (Join-Path $PSScriptRoot "send-input.ps1") -Action FaceRight -MouseDelta 0 -DelayMilliseconds 120 | Out-Host
}

Write-Host ""
Write-Host "[setup-terra-review] Ready."
Write-Host "[setup-terra-review] Use Adventure mode for mining/damage/durability checks."
Write-Host "[setup-terra-review] Use Creative mode for visual-only setup or cleanup."
Write-Host "[setup-terra-review] To switch later:"
Write-Host "  powershell -ExecutionPolicy Bypass -File scripts/send-dev-command.ps1 -Command `"motm dev mode adventure`""
Write-Host "  powershell -ExecutionPolicy Bypass -File scripts/send-dev-command.ps1 -Command `"motm dev mode creative`""
