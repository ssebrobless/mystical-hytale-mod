param(
    [string]$ProjectRoot = ""
)

$ErrorActionPreference = "Stop"

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
} else {
    $ProjectRoot = (Resolve-Path $ProjectRoot).Path
}

$failures = New-Object System.Collections.Generic.List[string]

function Add-Pass([string]$Message) {
    Write-Host "PASS: $Message"
}

function Add-Fail([string]$Message) {
    Write-Host "FAIL: $Message"
    [void]$failures.Add($Message)
}

function Get-RelativeProjectPath([string]$Path) {
    $full = (Resolve-Path -LiteralPath $Path).Path
    if ($full.StartsWith($ProjectRoot, [System.StringComparison]::OrdinalIgnoreCase)) {
        return $full.Substring($ProjectRoot.Length).TrimStart('\', '/').Replace('\', '/')
    }
    return $full.Replace('\', '/')
}

function Test-NoMatches([string]$Description, [string[]]$Paths, [string]$Pattern, [scriptblock]$Allowed) {
    $matches = New-Object System.Collections.Generic.List[object]
    foreach ($path in $Paths) {
        if (-not (Test-Path -LiteralPath $path)) { continue }
        Get-ChildItem -LiteralPath $path -Recurse -File -ErrorAction SilentlyContinue |
            Where-Object {
                $_.Extension -in @(".java", ".json", ".ui", ".properties", ".md", ".ps1")
            } |
            Select-String -Pattern $Pattern -SimpleMatch -ErrorAction SilentlyContinue |
            ForEach-Object {
                $relative = Get-RelativeProjectPath $_.Path
                if (-not (& $Allowed $relative)) {
                    [void]$matches.Add([PSCustomObject]@{
                        Path = $relative
                        Line = $_.LineNumber
                        Text = $_.Line.Trim()
                    })
                }
            }
    }

    if ($matches.Count -eq 0) {
        Add-Pass $Description
        return
    }

    $sample = ($matches | Select-Object -First 8 | ForEach-Object {
        "$($_.Path):$($_.Line): $($_.Text)"
    }) -join "`n"
    Add-Fail "$Description`n$sample"
}

$srcMain = Join-Path $ProjectRoot "src/main"
$styleData = Join-Path $ProjectRoot "src/main/resources/data/styles"
$scenarioDir = Join-Path $ProjectRoot "scripts/scenarios"
$scriptDir = Join-Path $ProjectRoot "scripts"

Test-NoMatches `
    "Style data does not reference dev/test dummy roles" `
    @($styleData) `
    "Test_Dummy" `
    { param($relative) $false }

Test-NoMatches `
    "Production ability runtime does not use dev/test dummy roles" `
    @($srcMain) `
    "Test_Dummy" `
    {
        param($relative)
        $relative -like "src/main/java/com/motm/runtime/task/StyleTest*" `
            -or $relative -like "src/main/java/com/motm/proof/*" `
            -or $relative -like "src/main/java/com/motm/command/*"
    }

Test-NoMatches `
    "Production visual proxies do not route through Empty_Role mannequins" `
    @(Join-Path $ProjectRoot "src/main/java/com/motm/util/HytaleAssetResolver.java") `
    "return ROLE_EMPTY" `
    { param($relative) $false }

Test-NoMatches `
    "Normal style selection does not claim test protection is automatically enabled" `
    @(Join-Path $ProjectRoot "src/main/java") `
    "Test Protection: legacy free-cast flag enabled" `
    { param($relative) $false }

Test-NoMatches `
    "Harness scenarios clean up any spawned style-test mobs" `
    @($scenarioDir) `
    "motm dev test mobs stationary" `
    {
        param($relative)
        $content = Get-Content -LiteralPath (Join-Path $ProjectRoot $relative) -Raw
        return $content -match "motm dev test mobs clear"
    }

Test-NoMatches `
    "Harness scenarios clean up any cluster style-test mobs" `
    @($scenarioDir) `
    "motm dev test mobs cluster" `
    {
        param($relative)
        $content = Get-Content -LiteralPath (Join-Path $ProjectRoot $relative) -Raw
        return $content -match "motm dev test mobs clear"
    }

Test-NoMatches `
    "Review/setup scripts clean up style-test mobs before spawning" `
    @($scriptDir) `
    "motm dev test mobs " `
    {
        param($relative)
        $content = Get-Content -LiteralPath (Join-Path $ProjectRoot $relative) -Raw
        return $content -match "motm dev test mobs clear" `
            -or $relative -eq "scripts/audit-public-readiness.ps1" `
            -or $relative -eq "scripts/send-dev-command.ps1"
    }

if ($failures.Count -gt 0) {
    throw "Public readiness audit failed with $($failures.Count) issue(s)."
}

Write-Host "Public readiness audit: PASS"
