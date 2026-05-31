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

function Test-FileContains([string]$Description, [string]$RelativePath, [string]$Pattern) {
    $path = Join-Path $ProjectRoot $RelativePath
    if (-not (Test-Path -LiteralPath $path)) {
        Add-Fail "$Description`nMissing file: $RelativePath"
        return
    }
    $content = Get-Content -LiteralPath $path -Raw
    if ($content -match $Pattern) {
        Add-Pass $Description
        return
    }
    Add-Fail "$Description`n$RelativePath did not match: $Pattern"
}

function Test-ProductionNpcSpawnsAreOwned {
    $matches = New-Object System.Collections.Generic.List[object]
    Get-ChildItem -LiteralPath $srcMain -Recurse -File -Filter "*.java" -ErrorAction SilentlyContinue |
        Select-String -Pattern "new NPCEntity" -SimpleMatch -ErrorAction SilentlyContinue |
        ForEach-Object {
            $relative = Get-RelativeProjectPath $_.Path
            $allowed = $relative -in @(
                "src/main/java/com/motm/manager/GameplayPlaybackManager.java",
                "src/main/java/com/motm/manager/RuntimePerkManager.java",
                "src/main/java/com/motm/runtime/ability/field/FieldVisualHytaleAdapter.java",
                "src/main/java/com/motm/runtime/ability/projectile/ProjectileVisualHytaleAdapter.java",
                "src/main/java/com/motm/runtime/ability/summon/SummonLifecycleHytaleAdapter.java",
                "src/main/java/com/motm/runtime/ability/terrain/TerrainGemHytaleAdapter.java",
                "src/main/java/com/motm/runtime/task/StyleTestMobActions.java",
                "src/main/java/com/motm/proof/MotmProofActions.java"
            )
            if (-not $allowed) {
                [void]$matches.Add([PSCustomObject]@{
                    Path = $relative
                    Line = $_.LineNumber
                    Text = $_.Line.Trim()
                })
            }
        }

    if ($matches.Count -eq 0) {
        Add-Pass "Production NPC spawns are limited to audited owners"
        return
    }

    $sample = ($matches | Select-Object -First 8 | ForEach-Object {
        "$($_.Path):$($_.Line): $($_.Text)"
    }) -join "`n"
    Add-Fail "Production NPC spawns are limited to audited owners`n$sample"
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
    "Production code does not spawn visible Spark_Living as a proxy role" `
    @($srcMain) `
    'setRoleName("Spark_Living")' `
    {
        param($relative)
        $relative -like "src/main/java/com/motm/proof/*" `
            -or $relative -like "src/main/java/com/motm/runtime/task/StyleTest*"
    }

Test-ProductionNpcSpawnsAreOwned

Test-FileContains `
    "Field visual proxies strip visible model components" `
    "src/main/java/com/motm/runtime/ability/field/FieldVisualHytaleAdapter.java" `
    "removeComponentIfExists\(\s*proxyRef,\s*ModelComponent\.getComponentType\(\)\s*\)"

Test-FileContains `
    "Projectile particle-only visuals do not spawn moving NPC proxies" `
    "src/main/java/com/motm/runtime/ability/projectile/ProjectileVisualHytaleAdapter.java" `
    "projectile_visual_proxy_skipped"

Test-FileContains `
    "Snow Imp summon avoids item-only snowman role in live NPC spawning" `
    "src/main/java/com/motm/runtime/ability/summon/SummonRuntimeSpecs.java" `
    'case "snow_imp", "snowman_imp" -> "Spirit_Frost"'

Test-FileContains `
    "Locust Queen avoids crash-prone Scarak Broodmother NPC role" `
    "src/main/java/com/motm/runtime/ability/summon/SummonRuntimeSpecs.java" `
    'case "locust_queen" -> "Scarak_Fighter"'

Test-NoMatches `
    "Live Scarak summons avoid crash-prone large Scarak NPC roles" `
    @((Join-Path $ProjectRoot "src/main/java/com/motm/runtime/ability/summon/SummonRuntimeSpecs.java")) `
    'Scarak_Defender|Scarak_Broodmother' `
    { param($relative) $false }

Test-FileContains `
    "Lapidary gem proxy strips fallback creature model" `
    "src/main/java/com/motm/runtime/ability/terrain/TerrainGemHytaleAdapter.java" `
    "FieldVisualHytaleAdapter\.configureRenderlessProxy"

Test-FileContains `
    "Quake one-shot impact visual strips fallback creature model" `
    "src/main/java/com/motm/manager/GameplayPlaybackManager.java" `
    "FieldVisualHytaleAdapter\.configureRenderlessProxy"

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
