param(
    [string]$SourcePath = "C:\Users\fishe\Documents\projects\motm-hytale-extract\original-concept\MOD_DESIGN.md",
    [string]$OutputRoot,
    [switch]$NoTimestamp
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if (-not $OutputRoot) {
    if ($NoTimestamp) {
        $OutputRoot = Join-Path $repoRoot "audits\concept-source-discovery\latest"
    } else {
        $stamp = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
        $OutputRoot = Join-Path $repoRoot (Join-Path "audits\concept-source-discovery" $stamp)
    }
}
New-Item -ItemType Directory -Path $OutputRoot -Force | Out-Null

if (-not (Test-Path -LiteralPath $SourcePath)) {
    throw "Source concept file not found: $SourcePath"
}

$rows = New-Object System.Collections.Generic.List[object]
$currentClass = $null
$currentStyle = $null
$inAbilityTable = $false

foreach ($line in Get-Content -LiteralPath $SourcePath) {
    if ($line -match '^##\s+(Terra|Hydro|Aero|Corruptus)\s+Base Styles') {
        $currentClass = $matches[1]
        $currentStyle = $null
        $inAbilityTable = $false
        continue
    }
    if ($line -match '^###\s+\d+\.\s+(.+?)\s+Style') {
        $currentStyle = $matches[1]
        $inAbilityTable = $false
        continue
    }
    if ($line -match '^\*\*Abilities:\*\*') {
        $inAbilityTable = $true
        continue
    }
    if ($line -match '^---\s*$') {
        $inAbilityTable = $false
        continue
    }
    if ($inAbilityTable -and $currentClass -and $currentStyle -and $line -match '^\|\s*([123])\s*\|\s*\*\*(.+?)\*\*\s*\|\s*(.+?)\s*\|$') {
        $description = $matches[3].Trim()
        if ($description -eq "Description") {
            continue
        }
        $rows.Add([pscustomobject]@{
            class = $currentClass
            style = $currentStyle
            slot = [int]$matches[1]
            ability = $matches[2].Trim()
            original_description = $description
            source = $SourcePath
        }) | Out-Null
    }
}

$currentRows = New-Object System.Collections.Generic.List[object]
Get-ChildItem -LiteralPath (Join-Path $repoRoot "src\main\resources\data\styles") -Filter "*_styles.json" |
    Sort-Object Name |
    ForEach-Object {
        $classId = $_.BaseName -replace "_styles$", ""
        $data = Get-Content -LiteralPath $_.FullName -Raw | ConvertFrom-Json
        foreach ($style in @($data.styles)) {
            $slot = 0
            foreach ($ability in @($style.abilities)) {
                $slot++
                $currentRows.Add([pscustomobject]@{
                    class = (Get-Culture).TextInfo.ToTitleCase($classId)
                    style = [string]$style.name
                    slot = $slot
                    ability = [string]$ability.name
                    current_description = [string]$ability.description
                }) | Out-Null
            }
        }
    }

$index = @{}
foreach ($row in $currentRows) {
    $key = "$($row.class)|$($row.style)|$($row.slot)"
    $index[$key] = $row
}

$comparison = foreach ($row in $rows) {
    $key = "$($row.class)|$($row.style)|$($row.slot)"
    $current = $index[$key]
    [pscustomobject]@{
        class = $row.class
        style = $row.style
        slot = $row.slot
        original_ability = $row.ability
        current_ability = if ($current) { $current.ability } else { "" }
        same_name = if ($current) { $row.ability -eq $current.ability } else { $false }
        original_description = $row.original_description
        current_description = if ($current) { $current.current_description } else { "" }
        current_is_compressed = if ($current) { $row.original_description.Length -gt ($current.current_description.Length * 2) } else { $false }
    }
}

$jsonPath = Join-Path $OutputRoot "original-concept-ability-comparison.json"
$mdPath = Join-Path $OutputRoot "original-concept-ability-comparison.md"

$comparison | ConvertTo-Json -Depth 6 | Set-Content -LiteralPath $jsonPath -Encoding UTF8

$md = New-Object System.Collections.Generic.List[string]
$md.Add("# Original Concept Ability Comparison")
$md.Add("")
$md.Add("- Generated: $(Get-Date -Format o)")
$md.Add("- Source: $SourcePath")
$md.Add("- Original ability rows found: $($rows.Count)")
$md.Add("- Current ability rows found: $($currentRows.Count)")
$md.Add("- Name mismatches: $(($comparison | Where-Object { -not $_.same_name }).Count)")
$md.Add("- Compressed current descriptions: $(($comparison | Where-Object { $_.current_is_compressed }).Count)")
$md.Add("")
$md.Add("| Class | Style | Slot | Original Ability | Current Ability | Original Description | Current Description |")
$md.Add("| --- | --- | ---: | --- | --- | --- | --- |")
foreach ($row in $comparison) {
    $original = $row.original_description.Replace("|", "/")
    $current = $row.current_description.Replace("|", "/")
    $md.Add("| $($row.class) | $($row.style) | $($row.slot) | $($row.original_ability) | $($row.current_ability) | $original | $current |")
}
$md | Set-Content -LiteralPath $mdPath -Encoding UTF8

Write-Host "[extract-original-concept-abilities] Wrote $mdPath"
Write-Host "[extract-original-concept-abilities] Wrote $jsonPath"
