param(
    [string]$WorldName = "MOTM Creative Test",
    [string]$RunId = "",
    [switch]$ColdLaunch,
    [switch]$SkipFlatlandsGate
)

$ErrorActionPreference = "Stop"

$proofs = @(
    "tempblock-gem-cluster",
    "tempblock-cactus",
    "tempblock-roots",
    "tempfluid-mud-field",
    "proxy-gem-aura",
    "proxy-sand-cloud",
    "proxy-debris-wave",
    "movement-burrow",
    "movement-tunnel",
    "movement-dust-devil"
)

$params = @{
    WorldName = $WorldName
    PhaseId = "proofs/terra-readiness"
    Proofs = $proofs
}

if (-not [string]::IsNullOrWhiteSpace($RunId)) {
    $params.RunId = $RunId
}
if ($ColdLaunch) {
    $params.ColdLaunch = $true
}
if ($SkipFlatlandsGate) {
    $params.SkipFlatlandsGate = $true
}

& (Join-Path $PSScriptRoot "run-runtime-proofs.ps1") @params
