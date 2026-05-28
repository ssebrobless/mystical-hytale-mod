param(
    [string]$WorldName = "MOTM Creative Test",
    [string]$RunId = "",
    [int]$DelayMilliseconds = 750
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($RunId)) {
    $RunId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
}

$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path "perk-runtime" $RunId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null
$report = New-Object System.Collections.Generic.List[string]

function Add-Line {
    param([string]$Line)
    $script:report.Add($Line) | Out-Null
}

function Get-LatestServerLog {
    $logDir = Join-Path $env:APPDATA ("Hytale\UserData\Saves\" + $WorldName + "\logs")
    Get-ChildItem -LiteralPath $logDir -Filter "*_server.log" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Read-NewLogLines {
    param([string]$Path, [long]$StartOffset)
    $lines = New-Object System.Collections.Generic.List[string]
    $fs = [System.IO.File]::Open($Path, "Open", "Read", "ReadWrite")
    try {
        $fs.Position = [Math]::Min($StartOffset, $fs.Length)
        $reader = New-Object System.IO.StreamReader($fs)
        while (($line = $reader.ReadLine()) -ne $null) {
            $lines.Add($line) | Out-Null
        }
    } finally {
        if ($reader) { $reader.Dispose() }
        $fs.Dispose()
    }
    return $lines
}

function Send-MotmCommand {
    param([string]$Command, [int]$Delay = $DelayMilliseconds)
    $trace = "perk-" + ([DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())
    & (Join-Path $PSScriptRoot "send-dev-command.ps1") `
        -Command $Command `
        -WorldName $WorldName `
        -RunDir $outDir `
        -TraceId $trace `
        -ScenarioId "perk-runtime-proof" `
        -TimeoutMilliseconds ([Math]::Max(12000, $Delay + 5000)) | Out-Host
    Start-Sleep -Milliseconds $Delay
}

function Invoke-Proof {
    param(
        [string]$Name,
        [string]$PerkId,
        [string[]]$Commands,
        [string[]]$RequiredPatterns,
        [string[]]$ResidualPatterns = @()
    )

    $RequiredPatterns = @($RequiredPatterns | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })
    $ResidualPatterns = @($ResidualPatterns | Where-Object { -not [string]::IsNullOrWhiteSpace($_) })

    Add-Line "## $Name"
    Add-Line ""
    Add-Line ("- Perk: " + $PerkId)
    $log = Get-LatestServerLog
    if (-not $log) {
        Add-Line "- Status: FAIL"
        Add-Line ("- Reason: no server log found for world " + $WorldName + ".")
        Add-Line ""
        return $false
    }

    $offset = $log.Length
    Send-MotmCommand "motm dev perks set $PerkId" 1000
    foreach ($command in $Commands) {
        Send-MotmCommand $command
    }
    $lines = Read-NewLogLines $log.FullName $offset
    $lines | Set-Content -LiteralPath (Join-Path $outDir "$Name.log") -Encoding UTF8

    $missing = @()
    foreach ($pattern in $RequiredPatterns) {
        if (-not @($lines | Where-Object { $_ -match $pattern })) {
            $missing += $pattern
        }
    }

    $residuals = @()
    foreach ($pattern in $ResidualPatterns) {
        $residuals += @($lines | Where-Object { $_ -match $pattern })
    }

    foreach ($line in ($lines | Where-Object { $_ -match "Runtime perk|Dev passive|Dev: perks" } | Select-Object -First 20)) {
        Add-Line "- Evidence: $line"
    }
    foreach ($line in $residuals) {
        Add-Line "- Residual: $line"
    }

    if ($missing.Count -eq 0) {
        Add-Line "- Status: PASS"
        Add-Line ""
        return $true
    }

    foreach ($pattern in $missing) {
        Add-Line ("- Missing: " + $pattern)
    }
    Add-Line "- Status: FAIL"
    Add-Line ""
    return $false
}

$status = "FAIL"
try {
    Add-Line "# Runtime Perk Proof Run"
    Add-Line ""
    Add-Line "- Run: $RunId"
    Add-Line "- World: $WorldName"
    Add-Line ""

    Send-MotmCommand "motm dev freecast off" 700
    Send-MotmCommand "motm dev class set terra" 900
    Send-MotmCommand "motm dev styles clear" 700
    Send-MotmCommand "motm dev mode adventure" 1000

    $passes = 0
    $total = 0
    $proofs = @(
        @{ Name = "twinkletoes"; Perk = "aero_t01_twinkletoes"; Commands = @("motm dev passive health 100", "motm dev passive incoming-damage 10 fall"); Required = @("Runtime perk damage: twinkletoes", "cause=Fall") },
        @{ Name = "neptunes-grace"; Perk = "hydro_t01_neptunes_grace"; Commands = @("motm dev passive health 5", "motm dev passive incoming-damage 1 physical"); Required = @("Runtime perk proc: neptunes_grace", "heal=[1-9]") },
        @{ Name = "desperation"; Perk = "corruptus_t01_desperation"; Commands = @("motm dev passive health 50", "motm dev passive outgoing-damage 100 ability"); Required = @("Runtime perk damage: desperation", "adjusted=110.0") },
        @{ Name = "vampirism"; Perk = "corruptus_t01_vampirism"; Commands = @("motm dev passive health 50", "motm dev passive outgoing-damage 100 ability"); Required = @("Runtime perk lifesteal: vampirism heal=[1-9]") },
        @{ Name = "heavyweight"; Perk = "terra_t01_heavyweight"; Commands = @("motm dev passive knockback"); Required = @("Dev passive knockback multiplier=0.680") },
        @{ Name = "big-lungs"; Perk = "hydro_t01_big_lungs"; Commands = @("motm dev passive status"); Required = @("Perk stat modifier applied: player=.*perk=hydro_t01_big_lungs.*stat=stamina", "Perk stat modifier applied: player=.*perk=hydro_t01_big_lungs.*stat=oxygen") },
        @{ Name = "ignite"; Perk = "corruptus_t01_ignite"; Commands = @("motm dev test mobs clear", "motm dev test mobs close", "motm dev passive outgoing-damage 100 ability"); Required = @("Runtime perk proc: ignite targets=[1-9]") },
        @{ Name = "haunting"; Perk = "corruptus_t01_haunting"; Commands = @("motm dev passive mob-kill"); Required = @("Runtime perk ghost spawned") },
        @{ Name = "sharpshooter"; Perk = "aero_t01_sharpshooter"; Commands = @("motm dev passive projectile-speed 1.0"); Required = @("Runtime perk projectile speed: sharpshooter", "adjusted=1.150") },
        @{ Name = "mole-man"; Perk = "terra_t01_mole_man"; Commands = @("motm dev relocate cave", "motm dev passive status", "motm dev passive mining Tool_Pickaxe_Iron"); Required = @("terraCaveVision=true", "Runtime perk mining applied: perk=mole_man", "Dev passive mining multiplier=1.600") },
        @{ Name = "rainy-day"; Perk = "hydro_t01_rainy_day"; Commands = @("motm dev passive health 50", "motm dev passive rainy-day auto"); Required = @("Dev passive rainy-day:.*forced=true", "Runtime perk regen: rainy_day active=true") },
        @{ Name = "terror"; Perk = "corruptus_t01_terror"; Commands = @("motm dev test mobs clear", "motm dev test mobs close", "motm dev passive terror"); Required = @("Dev passive terror: targets=[1-9]") }
    )

    foreach ($proof in $proofs) {
        $total++
        $ok = Invoke-Proof `
            -Name $proof.Name `
            -PerkId $proof.Perk `
            -Commands $proof.Commands `
            -RequiredPatterns $proof.Required `
            -ResidualPatterns @($proof.Residual)
        if ($ok) { $passes++ }
    }

    Add-Line "## Untestable Without Specific World Stimulus"
    Add-Line ""
    Add-Line "- Accelerate, Bunny Hop, Big Strides, and Semiaquatic require live sprint/swim movement-state transitions; verify with `/motm dev perks set <perk>` plus movement lane/water-lane captures."
    Add-Line "- Eco-friendly requires a native bare-hand grass punch in open space; expected log: `Runtime perk proc: eco_friendly`."
    Add-Line "- Blacksmith and Toolsmith require native crafting; expected log: `Runtime perk crafting enhancement` plus metadata/damage or durability proof."
    Add-Line "- Terror is implemented on the closest available hook: native weapon hit while signature energy is full. If Hytale exposes a future explicit ultimate-use event, move the trigger there."
    Add-Line ""

    if ($passes -eq $total) {
        $status = "PASS"
    }
    Add-Line "## Summary"
    Add-Line ""
    Add-Line "- Result: $status"
    Add-Line "- Passed: $passes / $total automated runtime proofs"
} catch {
    Add-Line "## Failure"
    Add-Line ""
    Add-Line "- Result: FAIL"
    Add-Line "- Error: $($_.Exception.Message)"
    throw
} finally {
    $reportPath = Join-Path $outDir "report.md"
    $report | Set-Content -LiteralPath $reportPath -Encoding UTF8
    Write-Host "[run-perk-runtime-proofs] $status report: $reportPath"
}

if ($status -ne "PASS") {
    exit 1
}
