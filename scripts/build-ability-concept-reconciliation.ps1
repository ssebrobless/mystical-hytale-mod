param(
    [string]$ConceptPath = "C:\Users\fishe\Documents\projects\motm-hytale-extract\original-concept\MOD_DESIGN.md",
    [string]$OutputRoot,
    [switch]$NoTimestamp
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $OutputRoot) {
    if ($NoTimestamp) {
        $OutputRoot = Join-Path $repoRoot "audits\concept-reconciliation\latest"
    } else {
        $stamp = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
        $OutputRoot = Join-Path $repoRoot (Join-Path "audits\concept-reconciliation" $stamp)
    }
}
New-Item -ItemType Directory -Path $OutputRoot -Force | Out-Null

if (-not (Test-Path -LiteralPath $ConceptPath)) {
    throw "Concept source not found: $ConceptPath"
}

$javaFiles = @(Get-ChildItem -LiteralPath (Join-Path $repoRoot "src\main\java") -Recurse -Filter "*.java")

function ConvertTo-Id([string]$value) {
    if (-not $value) { return "" }
    return ($value.ToLowerInvariant() -replace "[^a-z0-9]+", "_").Trim("_")
}

function Get-ConceptRows([string]$path) {
    $rows = New-Object System.Collections.Generic.List[object]
    $currentClass = $null
    $currentStyle = $null
    $inAbilityTable = $false

    foreach ($line in Get-Content -LiteralPath $path) {
        if ($line -match "^##\s+(Terra|Hydro|Aero|Corruptus)\s+Base Styles") {
            $currentClass = $matches[1].ToLowerInvariant()
            $currentStyle = $null
            $inAbilityTable = $false
            continue
        }
        if ($line -match "^###\s+\d+\.\s+(.+?)\s+Style") {
            $currentStyle = ConvertTo-Id $matches[1]
            $inAbilityTable = $false
            continue
        }
        if ($line -match "^\*\*Abilities:\*\*") {
            $inAbilityTable = $true
            continue
        }
        if ($line -match "^---\s*$") {
            $inAbilityTable = $false
            continue
        }
        if ($inAbilityTable -and $currentClass -and $currentStyle -and $line -match "^\|\s*([123])\s*\|\s*\*\*(.+?)\*\*\s*\|\s*(.+?)\s*\|$") {
            $description = $matches[3].Trim()
            if ($description -eq "Description") { continue }
            $rows.Add([pscustomobject]@{
                class = $currentClass
                style = $currentStyle
                slot = [int]$matches[1]
                originalAbility = $matches[2].Trim()
                originalId = ConvertTo-Id $matches[2]
                originalDescription = $description
            }) | Out-Null
        }
    }
    return $rows
}

function Get-CurrentRows {
    $rows = New-Object System.Collections.Generic.List[object]
    Get-ChildItem -LiteralPath (Join-Path $repoRoot "src\main\resources\data\styles") -Filter "*_styles.json" |
        Sort-Object Name |
        ForEach-Object {
            $classId = $_.BaseName -replace "_styles$", ""
            $data = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
            foreach ($style in @($data.styles)) {
                $slot = 0
                foreach ($ability in @($style.abilities)) {
                    $slot++
                    $rows.Add([pscustomobject]@{
                        class = $classId
                        style = [string]$style.id
                        styleName = [string]$style.name
                        slot = $slot
                        abilityId = [string]$ability.id
                        abilityName = [string]$ability.name
                        description = [string]$ability.description
                        castType = [string]$ability.cast_type
                        targetType = [string]$ability.target_type
                        range = $ability.range
                        radius = $ability.radius
                        duration = $ability.duration_seconds
                        cooldown = $ability.cooldown_seconds
                        effect = [string]$ability.effect
                        terrainEffect = [string]$ability.terrain_effect
                        projectileSpeed = $ability.projectile_speed
                        raw = $ability
                    }) | Out-Null
                }
            }
        }
    return $rows
}

function Get-Tags([string]$text) {
    $tags = New-Object System.Collections.Generic.List[string]
    $checks = [ordered]@{
        "requirement" = "\brequires?\b|\bcannot\b|\bonly\b|\bmust\b"
        "toggle_recast" = "\btoggle\b|\breactivat|\bpress again\b|\bcancel\b|\bdeactivate\b|\bend early\b"
        "charges_recharge" = "\bcharges?\b|\brecharge\b|\bregenerate\b|\brefund\b"
        "followup_chain" = "\bnext\b|\bafter using\b|\bwhile active\b|\bwhen .* ends\b|\bif .* active\b|\bdepends\b"
        "summon_object" = "\bsummon\b|\bminion\b|\btree\b|\bgem\b|\begg\b|\bmount\b|\bclone\b|\bdecoy\b|\bdominated\b|\bfriendly\b"
        "projectile_aim" = "\bprojectile\b|\barrow speed\b|\bfire\b|\bthrow\b|\bbeam\b|\bline\b|\bcrosshair\b"
        "spatial_radius" = "\bradius\b|\bwithin\b|\bblocks?\b|\bcone\b|\brange\b|\bAoE\b|\barea\b"
        "movement_form" = "\bdash\b|\bjump\b|\bairborne\b|\bflight\b|\bfly\b|\bhover\b|\bteleport\b|\btransform\b|\bform\b|\bride\b"
        "terrain_physical" = "\bblock\b|\bwall\b|\bpillar\b|\blava\b|\bwater\b|\bflower\b|\bsapling\b|\bcactus\b|\bice\b|\brift\b|\bpool\b"
        "status_debuff" = "\bburn\b|\bslow\b|\bstun\b|\bpoison\b|\bmark\b|\bcurse\b|\bDoT\b|\bfrozen\b|\broot\b|\bblind\b"
        "friendly_safety" = "\bfriendlies\b|\ballies\b|\bcaster\b|\bself-damage\b|\bdoes not hurt\b|\bimmune\b|\bheal\b"
    }
    foreach ($key in $checks.Keys) {
        if ($text -match $checks[$key]) {
            $tags.Add($key) | Out-Null
        }
    }
    return @($tags)
}

function Get-CodeHits([string]$abilityId) {
    if (-not $abilityId) { return @() }
    $hits = New-Object System.Collections.Generic.List[string]
    foreach ($file in $javaFiles) {
        $matches = Select-String -LiteralPath $file.FullName -SimpleMatch $abilityId -ErrorAction SilentlyContinue
        foreach ($match in @($matches)) {
            $relative = $file.FullName.Substring($repoRoot.Length + 1)
            $hits.Add("${relative}:$($match.LineNumber)") | Out-Null
        }
    }
    return @($hits)
}

function Has-ConceptSetEntry([string]$setName, [string]$abilityId) {
    if (-not $abilityId) { return $false }
    foreach ($file in $javaFiles) {
        $text = Get-Content -LiteralPath $file.FullName -Raw -ErrorAction SilentlyContinue
        if ([string]::IsNullOrWhiteSpace($text)) { continue }
        $setIndex = $text.IndexOf($setName, [StringComparison]::Ordinal)
        if ($setIndex -lt 0) { continue }
        $tail = $text.Substring($setIndex, [Math]::Min(5000, $text.Length - $setIndex))
        if ($tail.Contains('"' + $abilityId + '"')) { return $true }
    }
    return $false
}

function Get-Gaps($concept, $current, [string[]]$tags, [string[]]$codeHits) {
    $gaps = New-Object System.Collections.Generic.List[string]
    if (-not $current) {
        $gaps.Add("missing-current-ability-row") | Out-Null
        return @($gaps)
    }
    $conceptProfile = Has-ConceptSetEntry "CONCEPT_RUNTIME_RECONCILED_ABILITIES" $current.abilityId
    $stateProfile = Has-ConceptSetEntry "CONCEPT_STATE_MACHINE_ABILITIES" $current.abilityId
    $physicalProfile = Has-ConceptSetEntry "CONCEPT_PHYSICAL_VISUAL_ABILITIES" $current.abilityId
    $friendlyProfile = Has-ConceptSetEntry "CONCEPT_FRIENDLY_SAFE_ABILITIES" $current.abilityId
    $summonProfile = Has-ConceptSetEntry "CONCEPT_SUMMON_OBJECT_ABILITIES" $current.abilityId

    if ($concept.originalDescription.Length -gt (($current.description.Length + 1) * 2) -and -not $conceptProfile) {
        $gaps.Add("current-json-compresses-original-concept") | Out-Null
    }
    if ($codeHits.Count -eq 0 -and -not $conceptProfile) {
        $gaps.Add("no-direct-runtime-specialization-found") | Out-Null
    }
    if (($tags -contains "toggle_recast" -or $tags -contains "charges_recharge" -or $tags -contains "followup_chain") -and $codeHits.Count -lt 2 -and -not $stateProfile) {
        $gaps.Add("needs-explicit-state-machine") | Out-Null
    }
    if (($tags -contains "summon_object") -and $codeHits.Count -lt 2 -and -not $summonProfile) {
        $gaps.Add("needs-owned-summon-or-object-runtime") | Out-Null
    }
    if (($tags -contains "terrain_physical") -and -not $current.terrainEffect -and -not $physicalProfile) {
        $gaps.Add("needs-physical-world-visual-plan") | Out-Null
    }
    if (($tags -contains "projectile_aim") -and ($current.castType -notmatch "projectile|line|cone|wave|gaze") -and -not $conceptProfile) {
        $gaps.Add("current-cast-type-does-not-read-as-aimed-projectile") | Out-Null
    }
    if (($tags -contains "friendly_safety") -and $codeHits.Count -lt 2 -and -not $friendlyProfile) {
        $gaps.Add("needs-friendly-safety-proof") | Out-Null
    }
    if (($tags -contains "spatial_radius") -and -not $current.radius -and -not $current.range) {
        $gaps.Add("missing-range-or-radius-data") | Out-Null
    }
    return @($gaps)
}

$conceptRows = @(Get-ConceptRows $ConceptPath)
$currentRows = @(Get-CurrentRows)
$currentIndex = @{}
foreach ($row in $currentRows) {
    $currentIndex["$($row.class)|$($row.style)|$($row.slot)"] = $row
}

$matrix = New-Object System.Collections.Generic.List[object]
foreach ($concept in $conceptRows) {
    $key = "$($concept.class)|$($concept.style)|$($concept.slot)"
    $current = $currentIndex[$key]
    $tags = @(Get-Tags $concept.originalDescription)
    $codeHits = if ($current) { @(Get-CodeHits $current.abilityId) } else { @() }
    $gaps = @(Get-Gaps $concept $current $tags $codeHits)
    $matrix.Add([pscustomobject]@{
        class = $concept.class
        style = $concept.style
        slot = $concept.slot
        ability = if ($current) { $current.abilityName } else { $concept.originalAbility }
        abilityId = if ($current) { $current.abilityId } else { $concept.originalId }
        originalConcept = $concept.originalDescription
        currentProfile = if ($current) {
            "cast=$($current.castType); target=$($current.targetType); range=$($current.range); radius=$($current.radius); duration=$($current.duration); cooldown=$($current.cooldown); effect=$($current.effect); terrain=$($current.terrainEffect)"
        } else {
            ""
        }
        conceptTags = ($tags -join ", ")
        directRuntimeHits = $codeHits.Count
        runtimeHitRefs = ($codeHits | Select-Object -First 8) -join "; "
        missingWork = if ($gaps.Count -gt 0) { $gaps -join ", " } else { "needs-live-visual-functional-proof" }
    }) | Out-Null
}

$jsonPath = Join-Path $OutputRoot "ability-concept-gap-matrix.json"
$mdPath = Join-Path $OutputRoot "ability-concept-gap-matrix.md"
$matrix | ConvertTo-Json -Depth 8 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

$highRisk = @($matrix | Where-Object {
    $_.missingWork -match "no-direct-runtime|state-machine|owned-summon|physical-world|friendly-safety|cast-type"
})

$md = New-Object System.Collections.Generic.List[string]
$md.Add("# Ability Concept Gap Matrix")
$md.Add("")
$md.Add("- Generated: $(Get-Date -Format o)")
$md.Add("- Concept source: $ConceptPath")
$md.Add("- Current rows: $($currentRows.Count)")
$md.Add("- Concept rows: $($conceptRows.Count)")
$md.Add("- Rows with major implementation gaps: $($highRisk.Count)")
$md.Add("")
$md.Add("## Source Authority")
$md.Add("")
$md.Add("1. User's current corrections and review decisions")
$md.Add("2. Original detailed Hytale concept in MOD_DESIGN.md")
$md.Add("3. Local Hytale API/assets and proven primitives")
$md.Add("4. Current protected style JSON as implementation state, not concept authority")
$md.Add("")
$md.Add("## Highest-Risk Rows")
$md.Add("")
$md.Add("| Class | Style | Ability | Missing Work |")
$md.Add("| --- | --- | --- | --- |")
foreach ($row in $highRisk) {
    $md.Add("| $($row.class) | $($row.style) | $($row.ability) | $($row.missingWork.Replace('|','/')) |")
}
$md.Add("")
$md.Add("## Full 120-Ability Matrix")
$md.Add("")
$md.Add("| Class | Style | Slot | Ability | Original Concept | Current Profile | Concept Tags | Runtime Hits | Missing Work |")
$md.Add("| --- | --- | ---: | --- | --- | --- | --- | ---: | --- |")
foreach ($row in $matrix) {
    $concept = $row.originalConcept.Replace("|", "/")
    $profile = $row.currentProfile.Replace("|", "/")
    $tags = $row.conceptTags.Replace("|", "/")
    $missing = $row.missingWork.Replace("|", "/")
    $md.Add("| $($row.class) | $($row.style) | $($row.slot) | $($row.ability) | $concept | $profile | $tags | $($row.directRuntimeHits) | $missing |")
}
$md | Set-Content -LiteralPath $mdPath -Encoding UTF8

Write-Host "[build-ability-concept-reconciliation] Wrote $mdPath"
Write-Host "[build-ability-concept-reconciliation] Wrote $jsonPath"
Write-Host "[build-ability-concept-reconciliation] Major gap rows: $($highRisk.Count)"
