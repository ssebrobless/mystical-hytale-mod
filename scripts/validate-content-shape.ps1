param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path,
    [string]$OutDir = ""
)

$ErrorActionPreference = "Stop"
$script:Failed = $false
$script:Failures = New-Object System.Collections.Generic.List[string]

function Add-Failure([string]$Message) {
    $script:Failed = $true
    $script:Failures.Add($Message)
    Write-Host "FAIL: $Message" -ForegroundColor Red
}

function Add-Pass([string]$Message) {
    Write-Host "PASS: $Message" -ForegroundColor Green
}

function Read-Json([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) {
        Add-Failure "Missing JSON file: $Path"
        return $null
    }
    try {
        return Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json -ErrorAction Stop
    } catch {
        Add-Failure "Invalid JSON in $Path`: $($_.Exception.Message)"
        return $null
    }
}

function Normalize-Id([object]$Value) {
    return ([string]$Value).Trim().ToLowerInvariant()
}

function Split-EffectTokens([object]$Value) {
    return @(([string]$Value).Split("+", [System.StringSplitOptions]::RemoveEmptyEntries) |
        ForEach-Object { $_.Trim().ToLowerInvariant() } |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
}

function Test-InSet([string]$Value, [string[]]$Allowed, [string]$Context) {
    if ([string]::IsNullOrWhiteSpace($Value)) {
        Add-Failure "$Context is blank"
        return
    }
    if ($Allowed -notcontains $Value) {
        Add-Failure "$Context has unsupported value '$Value'"
    }
}

if ([string]::IsNullOrWhiteSpace($OutDir)) {
    $OutDir = Join-Path $ProjectRoot "build/reports/motm/content-shape"
}
New-Item -ItemType Directory -Path $OutDir -Force | Out-Null

$expectedClasses = @("terra", "hydro", "aero", "corruptus")
$allowedCastTypes = @(
    "air_stall", "barrier", "chain", "channel", "cleanse", "cone", "curse",
    "dash", "dash_buff", "dash_strike", "dive_strike", "execute", "gaze",
    "ground_burst", "ground_strike", "ground_target", "ground_zone", "leap",
    "line_control", "projectile", "projectile_burst", "projectile_line",
    "projectile_volley", "self_buff", "self_burst", "summon", "summon_buff",
    "support_zone", "teleport", "transformation", "wave_line"
)
$allowedTargetTypes = @(
    "allied_summons", "cone", "enemy", "enemy_cluster", "ground_target",
    "line", "self", "self_centered"
)
$allowedEffectTokens = @(
    "aoe", "attack_buff", "attack_slow", "blind", "burn", "burst", "clone",
    "consume_burn", "damage_buff", "deafen", "defense_buff", "disoriented",
    "dot", "damage_reduction", "evasion", "evasion_buff", "evasion_zone", "grounded", "heal", "knockback",
    "lifesteal", "lightning", "lure", "persistent_object", "root",
    "sand_empower", "self_burn", "shield", "shocked", "slow", "slow_stack", "speed",
    "stealth", "stun", "stun_if_wall", "summon", "summon_tank",
    "toxic", "untargetable", "vulnerability"
)

$stylesDir = Join-Path $ProjectRoot "src/main/resources/data/styles"
$proofCatalogPath = Join-Path $ProjectRoot "src/main/java/com/motm/proof/MotmProofCatalog.java"
$scenarioDir = Join-Path $ProjectRoot "scripts/scenarios"

$styleCatalog = New-Object System.Collections.Generic.List[object]
$abilityCatalog = New-Object System.Collections.Generic.List[object]
$abilityIds = New-Object System.Collections.Generic.HashSet[string]
$styleIds = New-Object System.Collections.Generic.HashSet[string]
$effectTokensSeen = New-Object System.Collections.Generic.HashSet[string]
$castTypesSeen = New-Object System.Collections.Generic.HashSet[string]
$terrainEffectsSeen = New-Object System.Collections.Generic.HashSet[string]
$travelTypesSeen = New-Object System.Collections.Generic.HashSet[string]

foreach ($classId in $expectedClasses) {
    $stylePath = Join-Path $stylesDir ($classId + "_styles.json")
    $styleData = Read-Json $stylePath
    if ($null -eq $styleData) { continue }

    $styles = @($styleData.styles | Where-Object { (Normalize-Id $_.class_id) -eq $classId })
    if ($styles.Count -ne 10) {
        Add-Failure "$classId should have 10 styles, found $($styles.Count)"
    }

    foreach ($style in $styles) {
        $styleId = Normalize-Id $style.id
        if ([string]::IsNullOrWhiteSpace($styleId)) {
            Add-Failure "$classId style has blank id"
            continue
        }
        if (-not $styleIds.Add($styleId)) {
            Add-Failure "Duplicate style id '$styleId'"
        }

        $abilities = @($style.abilities)
        if ($abilities.Count -ne 3) {
            Add-Failure "$classId/$styleId should have 3 abilities, found $($abilities.Count)"
        }

        $styleCatalog.Add([PSCustomObject]@{
            classId = $classId
            styleId = $styleId
            name = [string]$style.name
            abilityIds = @($abilities | ForEach-Object { Normalize-Id $_.id })
        })

        foreach ($ability in $abilities) {
            $abilityId = Normalize-Id $ability.id
            $castType = Normalize-Id $ability.cast_type
            $targetType = Normalize-Id $ability.target_type
            $terrainEffect = Normalize-Id $ability.terrain_effect
            $travelType = Normalize-Id $ability.travel_type
            $effectTokens = Split-EffectTokens $ability.effect

            if ([string]::IsNullOrWhiteSpace($abilityId)) {
                Add-Failure "$classId/$styleId has ability with blank id"
                continue
            }
            if (-not $abilityIds.Add($abilityId)) {
                Add-Failure "Duplicate ability id '$abilityId'"
            }
            Test-InSet $castType $allowedCastTypes "$classId/$styleId/$abilityId cast_type"
            Test-InSet $targetType $allowedTargetTypes "$classId/$styleId/$abilityId target_type"

            foreach ($token in $effectTokens) {
                [void]$effectTokensSeen.Add($token)
                if ($allowedEffectTokens -notcontains $token) {
                    Add-Failure "$classId/$styleId/$abilityId effect token '$token' is unsupported"
                }
            }
            [void]$castTypesSeen.Add($castType)
            if (-not [string]::IsNullOrWhiteSpace($terrainEffect)) {
                [void]$terrainEffectsSeen.Add($terrainEffect)
            }
            if (-not [string]::IsNullOrWhiteSpace($travelType)) {
                [void]$travelTypesSeen.Add($travelType)
            }

            if ($castType -in @("projectile", "projectile_line", "projectile_burst", "projectile_volley", "wave_line") `
                    -and [double]$ability.projectile_speed -lt 0) {
                Add-Failure "$classId/$styleId/$abilityId projectile_speed cannot be negative"
            }
            if ($castType -in @("ground_zone", "support_zone", "barrier", "channel", "transformation", "summon") `
                    -and [double]$ability.duration_seconds -lt 0) {
                Add-Failure "$classId/$styleId/$abilityId duration_seconds cannot be negative"
            }
            if ([int]$ability.resource_cost -ne 0) {
                Add-Failure "$classId/$styleId/$abilityId resource_cost must remain 0"
            }

            $abilityCatalog.Add([PSCustomObject]@{
                classId = $classId
                styleId = $styleId
                abilityId = $abilityId
                name = [string]$ability.name
                castType = $castType
                targetType = $targetType
                effectTokens = $effectTokens
                terrainEffect = $terrainEffect
                travelType = $travelType
            })
        }
    }
}

if ($styleCatalog.Count -eq 40) { Add-Pass "Catalog contains all 40 styles" } else { Add-Failure "Catalog contains $($styleCatalog.Count) styles, expected 40" }
if ($abilityCatalog.Count -eq 120) { Add-Pass "Catalog contains all 120 abilities" } else { Add-Failure "Catalog contains $($abilityCatalog.Count) abilities, expected 120" }

$proofIds = New-Object System.Collections.Generic.HashSet[string]
if (Test-Path -LiteralPath $proofCatalogPath) {
    $proofSource = Get-Content -LiteralPath $proofCatalogPath -Raw
    $matches = [regex]::Matches($proofSource, 'ProofDefinition\(\s*"([a-z0-9][a-z0-9_-]+)"')
    if ($matches.Count -eq 0) {
        $matches = [regex]::Matches($proofSource, '"([a-z0-9][a-z0-9_-]+)"')
    }
    foreach ($match in $matches) {
        $id = $match.Groups[1].Value
        if ($id -notin @("proofid")) {
            [void]$proofIds.Add($id)
        }
    }
} else {
    Add-Failure "Missing proof catalog source: $proofCatalogPath"
}

$scenarioCatalog = New-Object System.Collections.Generic.List[object]
$validEvidenceSources = @("control", "causality", "client-intent", "server-truth", "packets")
if (Test-Path -LiteralPath $scenarioDir) {
    foreach ($scenarioPath in Get-ChildItem -LiteralPath $scenarioDir -Filter "*.json" | Sort-Object Name) {
        $scenario = Read-Json $scenarioPath.FullName
        if ($null -eq $scenario) { continue }
        $scenarioId = Normalize-Id $scenario.id
        if ([string]::IsNullOrWhiteSpace($scenarioId)) {
            Add-Failure "$($scenarioPath.Name) has blank scenario id"
            continue
        }
        if ((Normalize-Id $scenario.styleId) -and -not $styleIds.Contains((Normalize-Id $scenario.styleId))) {
            Add-Failure "$scenarioId references unknown styleId '$($scenario.styleId)'"
        }
        foreach ($abilityId in @($scenario.abilities)) {
            $normalized = Normalize-Id $abilityId
            if (-not $abilityIds.Contains($normalized)) {
                Add-Failure "$scenarioId references unknown ability '$abilityId'"
            }
        }
        foreach ($proofId in @($scenario.proofs)) {
            $normalized = Normalize-Id $proofId
            if (-not $proofIds.Contains($normalized)) {
                Add-Failure "$scenarioId references unknown proof '$proofId'"
            }
        }
        foreach ($commandField in @("setupCommands", "commands", "cleanupCommands")) {
            foreach ($command in @($scenario.$commandField)) {
                $commandText = [string]$command
                if ([string]::IsNullOrWhiteSpace($commandText)) {
                    Add-Failure "$scenarioId contains a blank $commandField entry"
                    continue
                }
                if ($commandText.Trim() -notmatch '^motm\s+') {
                    Add-Failure "$scenarioId $commandField entry must start with 'motm ': $commandText"
                }
                if ($commandText.Trim() -match '^motm\s+dev\s+observe\s+stop\b') {
                    Add-Failure "$scenarioId $commandField must not stop observability; the runner owns run finalization"
                }
            }
        }
        foreach ($expectation in @($scenario.expectedEvidence)) {
            $expectationText = [string]$expectation
            if ([string]::IsNullOrWhiteSpace($expectationText)) {
                Add-Failure "$scenarioId contains a blank expectedEvidence entry"
                continue
            }
            $parts = $expectationText.Split(":", 2)
            if ($parts.Count -ne 2 -or [string]::IsNullOrWhiteSpace($parts[0]) -or [string]::IsNullOrWhiteSpace($parts[1])) {
                Add-Failure "$scenarioId expectedEvidence must use source:type format: $expectationText"
                continue
            }
            if ($validEvidenceSources -notcontains $parts[0]) {
                Add-Failure "$scenarioId expectedEvidence has unknown source '$($parts[0])': $expectationText"
            }
        }
        $scenarioCatalog.Add([PSCustomObject]@{
            id = $scenarioId
            description = [string]$scenario.description
            styleId = Normalize-Id $scenario.styleId
            abilities = @($scenario.abilities | ForEach-Object { Normalize-Id $_ })
            proofs = @($scenario.proofs | ForEach-Object { Normalize-Id $_ })
            setupCommands = @($scenario.setupCommands | ForEach-Object { [string]$_ })
            commands = @($scenario.commands | ForEach-Object { [string]$_ })
            cleanupCommands = @($scenario.cleanupCommands | ForEach-Object { [string]$_ })
            expectedEvidence = @($scenario.expectedEvidence | ForEach-Object { [string]$_ })
        })
    }
} else {
    Add-Failure "Missing scenario catalog directory: $scenarioDir"
}

$proofIdList = @($proofIds.GetEnumerator() | Sort-Object)
$castTypeList = @($castTypesSeen.GetEnumerator() | Sort-Object)
$effectTokenList = @($effectTokensSeen.GetEnumerator() | Sort-Object)
$terrainEffectList = @($terrainEffectsSeen.GetEnumerator() | Sort-Object)
$travelTypeList = @($travelTypesSeen.GetEnumerator() | Sort-Object)

$catalog = [PSCustomObject]@{
    generatedAt = (Get-Date).ToUniversalTime().ToString("o")
    classes = $expectedClasses
    styles = @($styleCatalog.ToArray())
    abilities = @($abilityCatalog.ToArray())
    proofIds = $proofIdList
    scenarios = @($scenarioCatalog.ToArray())
    knownTokens = [PSCustomObject]@{
        castTypes = $castTypeList
        effectTokens = $effectTokenList
        terrainEffects = $terrainEffectList
        travelTypes = $travelTypeList
    }
}

$catalogJson = Join-Path $OutDir "content-catalog.json"
$catalogMd = Join-Path $OutDir "content-catalog.md"
$catalog | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $catalogJson

$markdown = New-Object System.Collections.Generic.List[string]
$markdown.Add("# MOTM Content Catalog")
$markdown.Add("")
$markdown.Add("- Styles: $($styleCatalog.Count)")
$markdown.Add("- Abilities: $($abilityCatalog.Count)")
$markdown.Add("- Proofs: $($proofIds.Count)")
$markdown.Add("- Scenarios: $($scenarioCatalog.Count)")
$markdown.Add("")
$markdown.Add("## Scenarios")
foreach ($scenario in $scenarioCatalog) {
    $abilityList = @($scenario.abilities) -join ", "
    $proofList = @($scenario.proofs) -join ", "
    $commandCount = @($scenario.setupCommands).Count + @($scenario.commands).Count + @($scenario.cleanupCommands).Count
    $markdown.Add(('- `{0}` style=`{1}` abilities=`{2}` proofs=`{3}` commands=`{4}`' -f $scenario.id, $scenario.styleId, $abilityList, $proofList, $commandCount))
}
$markdown.Add("")
$markdown.Add("## Abilities")
foreach ($ability in $abilityCatalog) {
    $markdown.Add(('- `{0}` class=`{1}` style=`{2}` cast=`{3}` target=`{4}`' -f $ability.abilityId, $ability.classId, $ability.styleId, $ability.castType, $ability.targetType))
}
$markdown | Set-Content -LiteralPath $catalogMd

Write-Host "Catalog written: $catalogJson"
Write-Host "Catalog written: $catalogMd"

if ($script:Failed) {
    Write-Host "Content shape validation failed with $($script:Failures.Count) issue(s)." -ForegroundColor Red
    exit 1
}

Write-Host "Content shape validation: PASS" -ForegroundColor Green
