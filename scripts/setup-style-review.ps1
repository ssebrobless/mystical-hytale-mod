param(
    [ValidateSet("terra", "hydro", "aero", "corruptus")]
    [string]$ClassId = "terra",
    [string]$StyleId = "quake",
    [string]$WorldName = "MOTM Creative Test",
    [ValidateSet("close", "stationary", "clear")]
    [string]$MobMode = "close",
    [switch]$SkipRelocate,
    [switch]$SkipThirdPerson
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$stylePath = Join-Path $repoRoot "src\main\resources\data\styles\${ClassId}_styles.json"
if (-not (Test-Path -LiteralPath $stylePath)) {
    throw "Style file not found: $stylePath"
}

$styleFile = Get-Content -LiteralPath $stylePath -Raw | ConvertFrom-Json
$style = @($styleFile.styles | Where-Object { $_.id -eq $StyleId }) | Select-Object -First 1
if (-not $style) {
    throw "Style not found: $ClassId/$StyleId"
}

function Send-MotmCommand([string]$Text, [int]$DelayMilliseconds = 650) {
    $devCommand = Join-Path $PSScriptRoot "send-dev-command.ps1"
    if (Test-Path -LiteralPath $devCommand) {
        & $devCommand -Command $Text -WorldName $WorldName -TimeoutMilliseconds ([Math]::Max(10000, $DelayMilliseconds + 4000)) | Out-Host
    } else {
        & (Join-Path $PSScriptRoot "send-input.ps1") -Action Command -Text $Text -DelayMilliseconds 120 | Out-Host
    }
    Start-Sleep -Milliseconds $DelayMilliseconds
}

Write-Host "[setup-style-review] Preparing $ClassId/$StyleId for manual real-control review."
Send-MotmCommand "motm dev freecast on"
Send-MotmCommand "motm dev daylight" 450
if (-not $SkipRelocate) {
    Send-MotmCommand "motm dev relocate lane" 1600
}
Send-MotmCommand "motm dev test reset" 1300
Send-MotmCommand "motm dev class set $ClassId"
Send-MotmCommand "motm dev styles clear"
Send-MotmCommand "motm style $StyleId" 1100
Send-MotmCommand "motm dev test reset" 1300
if ($MobMode -ne "clear") {
    Send-MotmCommand "motm dev test mobs $MobMode" 1350
}
Send-MotmCommand "motm dev test mobs count" 450
Send-MotmCommand "motm dev position" 450

if (-not $SkipThirdPerson) {
    & (Join-Path $PSScriptRoot "verify-third-person.ps1") -Phase "manual-style-review" -Name "$ClassId-$StyleId" -TryToggle | Out-Host
}

Write-Host ""
Write-Host "[setup-style-review] Ready for USER real-control review."
Write-Host "[setup-style-review] Hold the spellbook, then trigger the normal in-game controls for:"
foreach ($ability in @($style.abilities)) {
    Write-Host ("  - {0}: {1} - {2}" -f $ability.id, $ability.name, $ability.description)
}
Write-Host ""
Write-Host "[setup-style-review] Notes:"
Write-Host "  - Do not use /motm dev test ability for this review; this setup is for normal player inputs."
Write-Host "  - After each ability, tell Codex what you saw. Codex can then clear mobs and prep the next style."
