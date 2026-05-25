param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
)

$ErrorActionPreference = "Stop"

function Fail([string]$Message) {
    Write-Host "FAIL: $Message" -ForegroundColor Red
    exit 1
}

function Pass([string]$Message) {
    Write-Host "PASS: $Message" -ForegroundColor Green
}

$stylePath = Join-Path $ProjectRoot "src/main/resources/data/styles/terra_styles.json"
$styleManagerPath = Join-Path $ProjectRoot "src/main/java/com/motm/manager/StyleManager.java"

if (-not (Test-Path $stylePath)) {
    Fail "Missing terra style data at $stylePath"
}
if (-not (Test-Path $styleManagerPath)) {
    Fail "Missing StyleManager at $styleManagerPath"
}

$data = Get-Content $stylePath -Raw | ConvertFrom-Json
$terraAbilities = @{}
foreach ($style in $data.styles) {
    if ($style.class_id -ne "terra") {
        continue
    }
    foreach ($ability in $style.abilities) {
        $terraAbilities[$ability.id] = $ability
    }
}

foreach ($ability in $terraAbilities.GetEnumerator()) {
    if ($ability.Value.resource_cost -ne 0) {
        Fail "$($ability.Key) has resource_cost=$($ability.Value.resource_cost); expected 0"
    }
}
Pass "All Terra ability resource costs are 0"

$sandstorm = $terraAbilities["sandstorm"]
if ($null -eq $sandstorm) {
    Fail "Sandstorm ability is missing"
}
if ([double]$sandstorm.duration_seconds -ne 10.0) {
    Fail "Sandstorm duration_seconds=$($sandstorm.duration_seconds); expected 10"
}
if ($sandstorm.toggleable -ne $true) {
    Fail "Sandstorm toggleable=$($sandstorm.toggleable); expected true"
}
if ([double]$sandstorm.toggle_cooldown_seconds -ne 2.0) {
    Fail "Sandstorm toggle_cooldown_seconds=$($sandstorm.toggle_cooldown_seconds); expected 2"
}
Pass "Sandstorm is a 10s toggle with a 2s post-end cooldown"

$dustDevil = $terraAbilities["dust_devil"]
if ($null -eq $dustDevil) {
    Fail "Dust Devil ability is missing"
}

$styleManager = Get-Content $styleManagerPath -Raw
if ($styleManager -notmatch "requiresActiveToggle\(ability,\s*""sandstorm""\)") {
    Fail "StyleManager does not enforce Dust Devil's active Sandstorm prerequisite"
}
if ($styleManager -notmatch "requiredActiveToggleToConsume\(ability\)" -or
        $styleManager -notmatch "consumeActiveToggle\(playerId,\s*consumedToggle\)") {
    Fail "StyleManager does not consume/end Sandstorm when Dust Devil is used"
}
Pass "Dust Devil requires and consumes active Sandstorm"

$tunnel = $terraAbilities["tunnel"]
if ($null -eq $tunnel) {
    Fail "Tunnel ability is missing"
}
if ([double]$tunnel.duration_seconds -le 0.0) {
    Fail "Tunnel duration_seconds is missing or <= 0; expected a finite no-resource timer"
}
Pass "Tunnel has a finite no-resource duration timer"

Write-Host "Terra no-resource contract: PASS"
