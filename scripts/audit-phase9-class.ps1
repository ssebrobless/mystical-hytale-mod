param(
    [ValidateSet("terra", "hydro", "aero", "corruptus")]
    [string]$ClassId = "terra",
    [string]$WorldName = "MOTM Creative Test",
    [string[]]$Styles = @(),
    [switch]$SkipFlatlandsGate,
    [switch]$SkipThirdPerson,
    [switch]$UseThirdPerson,
    [switch]$RequireThirdPersonProof,
    [switch]$RequireConceptProof,
    [switch]$RecordVideo,
    [switch]$SkipSafeLaneRelocate,
    [int]$VideoSeconds = 900,
    [int]$CommandDelayMilliseconds = 850,
    [int]$PostCastMilliseconds = 2400
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runId = Get-Date -Format "yyyy-MM-ddTHH-mm-ss"
$phaseId = "phase9-$ClassId-flatlands"
$outDir = Join-Path $repoRoot (Join-Path "audits" (Join-Path $phaseId $runId))
New-Item -ItemType Directory -Path $outDir -Force | Out-Null

$stylePath = Join-Path $repoRoot "src\main\resources\data\styles\${ClassId}_styles.json"
if (-not (Test-Path -LiteralPath $stylePath)) {
    throw "Style file not found: $stylePath"
}
$styleFile = Get-Content -LiteralPath $stylePath -Raw | ConvertFrom-Json
$allStyles = @($styleFile.styles)
if ($Styles.Count -gt 0) {
    $Styles = @($Styles | ForEach-Object { $_ -split "," } | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    $wanted = @{}
    $Styles | ForEach-Object { $wanted[$_.ToLowerInvariant()] = $true }
    $allStyles = @($allStyles | Where-Object { $wanted.ContainsKey($_.id.ToLowerInvariant()) })
}
if ($allStyles.Count -eq 0) {
    throw "No styles selected for class '$ClassId'."
}

$report = New-Object System.Collections.Generic.List[string]
function Add-Line([string]$Line) {
    $script:report.Add($Line)
}

function Start-HytaleRecording {
    if (-not $RecordVideo) {
        return $null
    }

    $ffmpeg = Get-Command ffmpeg.exe -ErrorAction SilentlyContinue
    if (-not $ffmpeg) {
        Add-Line("- Video: SKIP - ffmpeg not found.")
        return $null
    }

    $videoPath = Join-Path $outDir "run.mkv"
    $args = @(
        "-y",
        "-f", "gdigrab",
        "-framerate", "30",
        "-i", "title=Hytale",
        "-t", [string]$VideoSeconds,
        "-c:v", "libx264",
        "-preset", "ultrafast",
        "-pix_fmt", "yuv420p",
        $videoPath
    )
    Add-Line("- Video: recording Hytale window to $videoPath")
    return Start-Process -FilePath $ffmpeg.Source -ArgumentList $args -PassThru -WindowStyle Hidden
}

function Add-RunMarker([string]$Marker) {
    Send-MotmCommand "motm dev audit marker $Marker" 350
}

function Get-AbilityScenario($Ability) {
    $castType = ([string]$Ability.cast_type).ToLowerInvariant()
    $targetType = ([string]$Ability.target_type).ToLowerInvariant()
    $trigger = ([string]$Ability.trigger).ToLowerInvariant()
    $effect = ([string]$Ability.effect).ToLowerInvariant()
    $description = ([string]$Ability.description).ToLowerInvariant()
    $kind = "single_target"
    $setup = "Spawn grounded target in range and face it before cast."
    $proof = "Cast result plus target-side hit/effect evidence."
    if ($trigger -eq "jump_land") {
        $kind = "jump_land"
        $setup = "Arm ability, jump and land within 3m of grounded target."
        $proof = "Landing resolution log with targets>=1 plus impact screenshot."
    } elseif ($castType -in @("dash", "dash_buff", "dash_strike", "leap", "dive_strike", "teleport", "air_stall")) {
        $kind = "movement"
        $setup = "Face target, start with clear lane, and move through the ability path."
        $proof = "Before/after displacement or target-side hit/effect after motion."
    } elseif ($castType -in @("cone", "gaze")) {
        $kind = "facing_cone"
        $setup = "Face target in a narrow camera cone before cast."
        $proof = "Target-side effect or hit; no 'No valid target' accepted."
    } elseif ($castType -in @("ground_zone", "support_zone")) {
        $kind = "ground_zone"
        $setup = "Grounded target inside radius; wait for persistence/tick window."
        $proof = "Field active/pulse line, target-side damage/status, persistent-area screenshot."
    } elseif ($castType -in @("ground_target", "ground_strike", "barrier")) {
        $kind = "ground_target"
        $setup = "Aim at target block or target feet; wait delay_seconds when present."
        $proof = "Telegraph/delay if present, then hit/status/impact visual."
    } elseif ($castType -like "projectile*" -or $castType -in @("line_control", "wave_line", "chain")) {
        $kind = "projectile_line"
        $setup = "Place grounded target in visible lane and keep aim steady through impact window."
        $proof = "Launch plus target-side hit/effect/impact evidence."
    } elseif ($castType -in @("summon", "summon_buff")) {
        $kind = "summon"
        $setup = "Clear arena before cast, then wait for summon appearance/action window."
        $proof = "Summon appears and survives or acts; no unmapped/nonexistent role warnings."
    } elseif ($castType -in @("cleanse") -or $effect -match "cleanse|purify" -or $description -match "cleanse|purge|purify") {
        $kind = "cleanse"
        $setup = "Pre-apply debuff/damage condition where available."
        $proof = "Debuff setup exists, then clear/remove log after cast."
    } elseif ($targetType -eq "self" -or $castType -match "self|transformation|form|buff") {
        $kind = "self_buff"
        $setup = "Use third-person camera and capture caster body/HUD/status after cast."
        $proof = "Buff/shield/heal/status log plus third-person visual screenshot."
    } elseif ($effect -match "heal" -or $description -match "heal") {
        $kind = "support_heal"
        $setup = "Use damaged or buffable caster/ally; capture HP/status proof."
        $proof = "HP/stat/status improvement in log or HUD screenshot."
    }
    return [pscustomobject]@{
        Kind = $kind
        Setup = $setup
        Proof = $proof
    }
}

function New-MotionStep([string]$Action, [int]$HoldMilliseconds = 650, [int]$DelayMilliseconds = 150) {
    return [pscustomobject]@{
        Action = $Action
        HoldMilliseconds = $HoldMilliseconds
        DelayMilliseconds = $DelayMilliseconds
    }
}

function Get-AbilityAuditPlan([string]$StyleId, $Ability, $Scenario) {
    $abilityId = ([string]$Ability.id).ToLowerInvariant()
    $styleKey = $StyleId.ToLowerInvariant()
    $spawnMode = "stationary"
    $postActions = New-Object System.Collections.Generic.List[object]
    $waitMs = $PostCastMilliseconds
    $note = "Default stationary-target cast with third-person evidence."
    $requiresSafeLane = $false
    $requiresWeaponHit = $false
    $weaponProofAfterCapture = $false

    switch ("$styleKey/$abilityId") {
        "quake/stomp" {
            $postActions.Add((New-MotionStep "Jump" 650 1700))
            $postActions.Add((New-MotionStep "DevCommand:motm dev test stomp-land" 0 650))
            $waitMs = 900
            $note = "Arm Stomp, attempt a real jump/land, then use the dev-only forced landing fallback if creative-world input does not produce a server landing."
        }
        "quake/aftershock" {
            $waitMs = 4200
            $note = "Wait through the 4s field window to catch crack pulse/status ticks."
        }
        "quake/sinkhole" {
            $waitMs = 5200
            $note = "Wait through buried-look engage, suffocation ticks, and release."
        }
        "metal/metal_coat" {
            $note = "Self metallic coating and damage-reduction buff; no weapon follow-up is expected."
        }
        "metal/alloy_enhancement" {
            $requiresWeaponHit = $true
            $note = "Weapon/tool enhancement proof: simulate a melee weapon hit after the buff is armed."
        }
        "magma/obsidian_skin" {
            $note = "Obsidian shell/coating, immobilize window, and shield proof; no weapon follow-up is expected."
        }
        "bloom/frolick" {
            $requiresWeaponHit = $true
            $postActions.Add((New-MotionStep "Forward" 900 250))
            $postActions.Add((New-MotionStep "Back" 500 250))
            $postActions.Add((New-MotionStep "StrafeLeft" 500 250))
            $waitMs = 1000
            $requiresSafeLane = $true
            $note = "Cast Frolick, immediately prove weapon payoff inside the buff window, then move while recording flower trail generation."
        }
        "sand/dust_devil" {
            $postActions.Add((New-MotionStep "Forward" 900 250))
            $waitMs = 1200
            $requiresSafeLane = $true
            $note = "Prime Sandstorm, then move with the dash/tornado window so drag and expel can be observed."
        }
        "sand/sandstorm" {
            $postActions.Add((New-MotionStep "Forward" 650 250))
            $postActions.Add((New-MotionStep "Back" 450 250))
            $postActions.Add((New-MotionStep "StrafeRight" 450 250))
            $waitMs = 1200
            $requiresSafeLane = $true
            $note = "Move inside the sand cloud to prove the aura follows the player."
        }
        "sand/vitrification" {
            $postActions.Add((New-MotionStep "Forward" 550 250))
            $postActions.Add((New-MotionStep "Back" 350 250))
            $waitMs = 1200
            $requiresSafeLane = $true
            $note = "Self-buff check while moving, since it should combine with Sandstorm/Dust Devil."
        }
        "self_petrification/tunnel" {
            $postActions.Add((New-MotionStep "Forward" 900 250))
            $postActions.Add((New-MotionStep "Back" 500 250))
            $postActions.Add((New-MotionStep "StrafeRight" 500 250))
            $waitMs = 1600
            $requiresSafeLane = $true
            $note = "Controlled movement proof while transformed into a stone block."
        }
        "soil/burrow" {
            $postActions.Add((New-MotionStep "Forward" 800 250))
            $waitMs = 1600
            $requiresSafeLane = $true
            $note = "Whack-a-mole dash proof: disappear, travel forward, re-emerge, hit/knockback."
        }
        "soil/debris" {
            $postActions.Add((New-MotionStep "Forward" 600 200))
            $waitMs = 1700
            $note = "Dust/debris wave travels forward from player; keep lane clear and visible."
        }
        "soil/mudpit" {
            $postActions.Add((New-MotionStep "Forward" 550 250))
            $postActions.Add((New-MotionStep "Back" 350 250))
            $waitMs = 3600
            $requiresSafeLane = $true
            $note = "Expanding muddy water field proof, including caster movement through the pool."
        }
        "arbor/sapling" {
            $spawnMode = "clear-only"
            $waitMs = 2600
            $note = "Ground-marking projectile proof; no target needed because sapling should land on terrain."
        }
        "bloom/nightshade" {
            $spawnMode = "stationary"
            $waitMs = 3600
            $note = "Projectile should pass through mobs and bloom on terrain/object, then lure/explode."
        }
        "bloom/cacti_cluster" {
            $waitMs = 5200
            $note = "Slow cactus projectile sticks, ticks for 4s, then visually bursts with AoE DoT."
        }
        "gem/lapidary" {
            $spawnMode = "clear-only"
            $waitMs = 2600
            $note = "Persistent gem object proof with block cube, aura, and HP nameplate."
        }
        "gem/refraction" {
            $requiresWeaponHit = $true
            $note = "Buff aura proof plus simulated weapon follow-up hit for attack/speed payoff."
        }
        "scarak/brood_surge" {
            $waitMs = [Math]::Max($waitMs, 3200)
            $note = "Summon-buff proof: create an active Scarak Egg summon first, then verify Brood Surge buffs or commands it."
        }
        default {
            if ($Scenario.Kind -eq "movement") {
                $postActions.Add((New-MotionStep "Forward" 800 250))
                $requiresSafeLane = $true
                $note = "Movement-classified ability gets active movement proof."
            } elseif ($Scenario.Kind -eq "projectile_line") {
                $postActions.Add((New-MotionStep "FaceRight" 180 150))
                $waitMs = [Math]::Max($waitMs, 2600)
                $note = "Projectile lane proof; small camera nudge helps show launch path."
            } elseif ($Scenario.Kind -eq "self_buff") {
                $postActions.Add((New-MotionStep "Forward" 450 200))
                $postActions.Add((New-MotionStep "Back" 300 200))
                $note = "Self-buff proof with slight movement to show body-bound coating/follow behavior."
            } elseif ($Scenario.Kind -eq "ground_zone") {
                $waitMs = [Math]::Max($waitMs, 3600)
                $note = "Ground-zone proof with persistence/tick wait."
            }
        }
    }

    return [pscustomobject]@{
        SpawnMode = $spawnMode
        PostActions = $postActions.ToArray()
        WaitMilliseconds = $waitMs
        RequiresSafeLane = $requiresSafeLane
        RequiresWeaponHit = $requiresWeaponHit
        WeaponProofAfterCapture = $weaponProofAfterCapture
        Note = $note
    }
}

function Invoke-MotionPlan($Plan) {
    foreach ($step in @($Plan.PostActions)) {
        if ($step.Action -like "DevCommand:*") {
            $command = $step.Action.Substring("DevCommand:".Length)
            Send-MotmCommand $command ([Math]::Max(250, $step.DelayMilliseconds))
            continue
        }
        & (Join-Path $PSScriptRoot "send-input.ps1") `
            -Action $step.Action `
            -HoldMilliseconds $step.HoldMilliseconds `
            -DelayMilliseconds $step.DelayMilliseconds | Out-Host
    }
}

function Get-PositionSamples($Lines) {
    $samples = New-Object System.Collections.Generic.List[object]
    foreach ($line in @($Lines)) {
        if ($line -match "Dev position:.*position=\(([-0-9.]+), ([-0-9.]+), ([-0-9.]+)\)") {
            $samples.Add([pscustomobject]@{
                X = [double]$matches[1]
                Y = [double]$matches[2]
                Z = [double]$matches[3]
                Line = $line
            })
        }
    }
    return $samples
}

function Get-MotionProof($Lines) {
    $samples = @(Get-PositionSamples $Lines)
    if ($samples.Count -lt 2) {
        return [pscustomobject]@{
            Status = "REVIEW"
            Distance = 0.0
            Note = "Before/after position proof missing."
        }
    }

    $before = $samples[0]
    $after = $samples[$samples.Count - 1]
    $dx = $after.X - $before.X
    $dz = $after.Z - $before.Z
    $distance = [Math]::Sqrt(($dx * $dx) + ($dz * $dz))
    return [pscustomobject]@{
        Status = $(if ($distance -ge 0.75) { "PASS" } else { "REVIEW" })
        Distance = $distance
        Note = ("before={0} after={1} horizontal={2:N2}m" -f $before.Line, $after.Line, $distance)
    }
}

function Invoke-WeaponProof([string]$StyleId, [string]$AbilityId, $Plan) {
    if (-not $Plan.RequiresWeaponHit) {
        return
    }
    $needsFreshTarget = $Plan.SpawnMode -eq "clear-only" `
        -or $Plan.WeaponProofAfterCapture `
        -or @($Plan.PostActions).Count -gt 0
    if ($needsFreshTarget) {
        Add-Line("  - Weapon proof setup: reacquiring a stationary target at current player position")
        Send-MotmCommand "motm dev test mobs stationary" 1450
        Send-MotmCommand "motm dev test mobs count" 450
    } else {
        Send-MotmCommand "motm dev test mobs count" 250
    }
    Send-MotmCommand "motm dev test weapon-hit" 650
    Add-Line("  - Action proof: simulated melee weapon hit after ability cast")
}

function Prepare-AbilityEnvironment([string]$StyleId, [string]$AbilityId, $Plan) {
    if ($Plan.RequiresSafeLane) {
        if (-not $SkipSafeLaneRelocate) {
            Send-MotmCommand "motm dev relocate lane" 1700
        }
        Send-MotmCommand "motm dev position" 350
        Ensure-ThirdPerson "$StyleId-$AbilityId-safe-lane" | Out-Null
    }

    Send-MotmCommand "motm dev test stop" 250
    Send-MotmCommand "motm dev test reset" 1700
    Send-MotmCommand "motm dev freecast on" 450
    Send-MotmCommand "motm dev class set $ClassId" 650
    Send-MotmCommand "motm dev styles clear" 450
    Send-MotmCommand "motm style $StyleId" 1250
    Send-MotmCommand "motm dev test mobs count" 350

    if ($StyleId.ToLowerInvariant() -eq "gem" -and $AbilityId.ToLowerInvariant() -in @("fracture", "refraction")) {
        Send-MotmCommand "motm dev test ability lapidary" 1800
        Add-Line("  - Precondition: refreshed active Lapidary gem before $AbilityId")
    }

    if ($Plan.SpawnMode -eq "clear-only") {
        return
    }

    Send-MotmCommand "motm dev test mobs $($Plan.SpawnMode)" 1450
    Send-MotmCommand "motm dev test mobs count" 450

    if ($StyleId.ToLowerInvariant() -eq "scarak" -and $AbilityId.ToLowerInvariant() -eq "brood_surge") {
        Send-MotmCommand "motm dev test ability scarak_egg" 2200
        Add-Line("  - Precondition: refreshed active Scarak Egg summon before Brood Surge")
    }
}

function Get-VisualProof([string]$StyleId, [string]$AbilityId, $Scenario, $Plan, $Lines, [string]$ResultLine, [string]$ScreenshotPath) {
    if (-not (Test-Path -LiteralPath $ScreenshotPath)) {
        return [pscustomobject]@{ Status = "REVIEW"; Note = "Screenshot missing." }
    }

    $abilityKey = "$($StyleId.ToLowerInvariant())/$($AbilityId.ToLowerInvariant())"
    $terrainLine = $Lines |
        Where-Object {
            $_ -match "Temporary Terra terrain placed: reason=$([regex]::Escape($AbilityId))\b" -or
            $_ -match "Temporary Terra terrain placed: reason=$([regex]::Escape($StyleId))\b" -or
            $_ -match "Temporary Terra terrain placed: reason=iron_wall\b" -or
            $_ -match "Temporary Terra terrain placed: reason=stone_pillar\b" -or
            $_ -match "Temporary Terra terrain placed: reason=sinkhole_(cracks|dust_ring)\b" -or
            $_ -match "Temporary Terra terrain placed: reason=dust_devil_sand\b" -or
            $_ -match "Moving Terra terrain trail started: reason=$([regex]::Escape($AbilityId))\b"
        } |
        Select-Object -Last 1
    $effectLine = $Lines |
        Where-Object {
            $_ -match "Effect applied:.*MOTM_" -or
            $_ -match "cast visuals|impact visuals|field visuals|aura|coating|trail"
        } |
        Select-Object -Last 1

    switch ($abilityKey) {
        { $_ -in @(
            "metal/iron_wall", "magma/lava_pool", "soil/mudpit",
            "arbor/sapling", "bloom/nightshade", "bloom/frolick",
            "bloom/cacti_cluster", "gem/lapidary"
        ) } {
            if ($terrainLine) {
                return [pscustomobject]@{ Status = "PASS-CANDIDATE"; Note = "Terrain proof captured: $terrainLine" }
            }
        }
        default {
            if ($terrainLine) {
                return [pscustomobject]@{ Status = "PASS-CANDIDATE"; Note = "Terrain proof captured: $terrainLine" }
            }
            if ($effectLine -or $ResultLine -match "Runtime:|field active|launched|self|buff|shield|hit|applied") {
                return [pscustomobject]@{ Status = "PASS-CANDIDATE"; Note = "Runtime visual proof captured in screenshot; still needs aesthetic review." }
            }
        }
    }

    return [pscustomobject]@{ Status = "REVIEW"; Note = "Inspect screenshot against style palette and ability motion." }
}

function Get-MechanicalProof($Scenario, $Lines, [string]$ResultLine, [string]$AbilityId, $MotionProof) {
    $resultText = [string]$ResultLine
    switch ($Scenario.Kind) {
        "jump_land" {
            $landing = $Lines |
                Where-Object { $_ -match "landing resolved: targets=[1-9]" } |
                Select-Object -Last 1
            if ($landing) { return [pscustomobject]@{ Status = "PASS"; Note = $landing } }
            return [pscustomobject]@{ Status = "FAIL"; Note = "No landing-resolution line with targets>=1." }
        }
        "projectile_line" {
            $impactLine = $Lines |
                Where-Object { $_ -match "Projectile (impact|traversal) resolved: abilityId=$([regex]::Escape($AbilityId)).*targets=[1-9]" } |
                Select-Object -Last 1
            if ($impactLine) {
                return [pscustomobject]@{ Status = "PASS"; Note = $impactLine }
            }
            if ($resultText -match "[1-9] hit|applied .* to [1-9] target") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Target-side hit/effect is present in cast result." }
            }
            if ($resultText -match "launched [1-9] projectile") {
                return [pscustomobject]@{ Status = "REVIEW"; Note = "Projectile launched; target-side impact still needs proof." }
            }
            return [pscustomobject]@{ Status = "FAIL"; Note = "No projectile launch or target-side hit/effect proof." }
        }
        "ground_zone" {
            if ($resultText -match "field active|field arms|radius .*m" -and $resultText -match "[1-9] hit|applied .* to [1-9] target") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Field duration/radius and target-side effect are present." }
            }
            if ($resultText -match "field active|field arms|radius .*m" -and $resultText -match "heal ready|self defense buff|self attack buff|cleanse|purify") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Field duration/radius plus support effect proof are present." }
            }
            if ($resultText -match "field active|field arms|radius .*m") {
                return [pscustomobject]@{ Status = "REVIEW"; Note = "Field exists; tick/effect proof is weak." }
            }
            return [pscustomobject]@{ Status = "FAIL"; Note = "No field active/radius proof." }
        }
        "facing_cone" {
            if ($resultText -match "[1-9] hit|applied .* to [1-9] target") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Facing/cone target-side effect found." }
            }
            return [pscustomobject]@{ Status = "FAIL"; Note = "No target-side hit/effect for facing/cone ability." }
        }
        "movement" {
            if ($MotionProof -and $MotionProof.Distance -ge 0.75) {
                return [pscustomobject]@{ Status = "PASS"; Note = "Movement displacement proved: $($MotionProof.Note)" }
            }
            if ($resultText -match "[1-9] hit|applied .* to [1-9] target|dash|leap|teleport|movement") {
                return [pscustomobject]@{ Status = "REVIEW"; Note = "Movement runtime fired; displacement needs before/after proof." }
            }
            return [pscustomobject]@{ Status = "FAIL"; Note = "No movement or target-side proof." }
        }
        "self_buff" {
            if ($resultText -match "[1-9] hit|applied .* to [1-9] target") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Self/aura cast also produced target-side proof." }
            }
            if ($resultText -match "self|buff|shield|heal|follow-up|form|evasion|defense") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Self-buff/status proof found in cast result." }
            }
            return [pscustomobject]@{ Status = "REVIEW"; Note = "Self cast exists but status/HUD proof is weak." }
        }
        "support_heal" {
            if ($resultText -match "heal|shield|buff|aura|lifesteal|channeling") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Support/heal status proof found in cast result." }
            }
            return [pscustomobject]@{ Status = "REVIEW"; Note = "Support cast exists but HP/stat proof is weak." }
        }
        "summon" {
            if ($resultText -match "buffed [1-9] summon|commanded [1-9] strike") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Active summon buff/command proof found in cast result." }
            }
            $summonAttackLine = $Lines |
                Where-Object { $_ -match "Summon attack resolved: abilityId=$([regex]::Escape($AbilityId))" } |
                Select-Object -Last 1
            if ($summonAttackLine) {
                return [pscustomobject]@{ Status = "PASS"; Note = $summonAttackLine }
            }
            $summonSpawnLine = $Lines |
                Where-Object { $_ -match "Summon spawned: abilityId=$([regex]::Escape($AbilityId))" } |
                Select-Object -Last 1
            if ($summonSpawnLine -and $resultText -match "summon") {
                return [pscustomobject]@{ Status = "PASS"; Note = $summonSpawnLine }
            }
            if ($resultText -match "summon|spawn") {
                return [pscustomobject]@{ Status = "REVIEW"; Note = "Summon runtime fired; visual/action proof still needed." }
            }
            return [pscustomobject]@{ Status = "FAIL"; Note = "No summon/spawn proof in cast result." }
        }
        default {
            if ($resultText -match "[1-9] hit|applied .* to [1-9] target|Runtime:") {
                return [pscustomobject]@{ Status = "PASS"; Note = "Generic cast/hit/status proof found." }
            }
            return [pscustomobject]@{ Status = "REVIEW"; Note = "Runtime proof exists; concept-specific proof not classified." }
        }
    }
}

function Get-LatestServerLog {
    $logDir = Join-Path $env:APPDATA ("Hytale\UserData\Saves\" + $WorldName + "\logs")
    Get-ChildItem -LiteralPath $logDir -Filter "*_server.log" -File -ErrorAction SilentlyContinue |
        Sort-Object LastWriteTime -Descending |
        Select-Object -First 1
}

function Send-MotmCommand([string]$Text, [int]$DelayMilliseconds = $CommandDelayMilliseconds) {
    Write-Host "[audit-phase9-class] /$Text"
    $devCommand = Join-Path $PSScriptRoot "send-dev-command.ps1"
    if (Test-Path -LiteralPath $devCommand) {
        & $devCommand -Command $Text -WorldName $WorldName -TimeoutMilliseconds ([Math]::Max(10000, $DelayMilliseconds + 4000)) | Out-Host
    } else {
        & (Join-Path $PSScriptRoot "send-input.ps1") -Action Command -Text $Text -DelayMilliseconds 120 | Out-Host
    }
    Start-Sleep -Milliseconds $DelayMilliseconds
}

function Read-NewLogLines([string]$Path, [long]$StartOffset) {
    $lines = New-Object System.Collections.Generic.List[string]
    $fs = [System.IO.File]::Open($Path, "Open", "Read", "ReadWrite")
    try {
        $fs.Position = [Math]::Min($StartOffset, $fs.Length)
        $reader = New-Object System.IO.StreamReader($fs)
        while (($line = $reader.ReadLine()) -ne $null) {
            $lines.Add($line)
        }
    } finally {
        if ($reader) { $reader.Dispose() }
        $fs.Dispose()
    }
    return $lines
}

function Read-LogLinesAfterMarker([string]$Path, [string]$Marker) {
    $all = Get-Content -LiteralPath $Path -ErrorAction Stop
    $markerIndex = -1
    for ($i = $all.Count - 1; $i -ge 0; $i--) {
        if ($all[$i] -match [regex]::Escape($Marker)) {
            $markerIndex = $i
            break
        }
    }
    if ($markerIndex -lt 0) {
        return $all
    }
    return @($all | Select-Object -Skip $markerIndex)
}

function Capture([string]$Name) {
    & (Join-Path $PSScriptRoot "capture-evidence.ps1") -Phase $phaseId -RunId $runId -Name $Name -WindowOnly | Out-Host
}

function Ensure-ThirdPerson([string]$Name) {
    if (-not $UseThirdPerson -or $SkipThirdPerson) {
        return "THIRD_PERSON=SKIP"
    }
    $check = & (Join-Path $PSScriptRoot "verify-third-person.ps1") -Phase $phaseId -RunId $runId -Name $Name -TryToggle
    $checkText = ($check | Out-String).Trim()
    if ([string]::IsNullOrWhiteSpace($checkText)) {
        $checkText = "THIRD_PERSON=FAIL no output"
    }
    Add-Line("  - Camera: $checkText")
    if ($RequireThirdPersonProof -and $checkText -notmatch "THIRD_PERSON=PASS") {
        throw "Third-person verification failed for $Name`: $checkText"
    }
    return $checkText
}

function Get-AbilityEvidence($Lines, [string]$AbilityId) {
    $escaped = [regex]::Escape($AbilityId)
    $Lines | Where-Object {
        $_ -match "abilityId=$escaped" -or
        $_ -match "Cast .*!.*" -and $_ -match $escaped -or
        $_ -match "Queued ability cast result:.*abilityId=$escaped"
    }
}

function Get-AbilityResult($Lines, [string]$AbilityId) {
    $escaped = [regex]::Escape($AbilityId)
    $Lines |
        Where-Object { $_ -match "Queued ability cast result:.*abilityId=$escaped" } |
        Select-Object -Last 1
}

function Get-ResidualLines($Lines) {
    $Lines | Where-Object {
        $_ -match "No valid target|Reloading nonexistent|Unmapped NPC type|Exception|ERROR|NoClassDefFoundError|ClassNotFoundException"
    } | Where-Object {
        $_ -notmatch "WorldThread - (default|default_world)" -and
        $_ -notmatch "Removing world exceptionally: default" -and
        $_ -notmatch "Store is shutdown!"
    }
}

function Get-BlockingResidualLines($Lines) {
    $Lines | Where-Object {
        $_ -match "NoClassDefFoundError|ClassNotFoundException|Exception|ERROR"
    } | Where-Object {
        $_ -notmatch "WorldThread - (default|default_world)" -and
        $_ -notmatch "Removing world exceptionally: default" -and
        $_ -notmatch "Store is shutdown!"
    }
}

$status = "FAIL"
$recordingProcess = $null
try {
    Add-Line("# Phase 9 $ClassId Flatlands Class Audit")
    Add-Line("")
    Add-Line("- Run: $runId")
    Add-Line("- World: $WorldName")
    Add-Line("- Style source: $stylePath")
    Add-Line("- Styles: " + (($allStyles | ForEach-Object { $_.id }) -join ", "))
    Add-Line("- Camera: " + ($(if ($UseThirdPerson -and -not $SkipThirdPerson) { "third-person verified before every ability" } else { "unchanged" })))
    Add-Line("- Facing aid: top compass/debug heading should remain visible in screenshots")
    Add-Line("- Mob hygiene: stop prior tests, clear tracked test mobs, assert count, then spawn ability-specific grounds")
    Add-Line("")
    $recordingProcess = Start-HytaleRecording

    if (-not $SkipFlatlandsGate) {
        & (Join-Path $PSScriptRoot "ensure-flatlands.ps1") -VerifyOnly
    }

    $log = Get-LatestServerLog
    if (-not $log) {
        throw "No server log found for $WorldName."
    }
    $startOffset = $log.Length
    Add-Line("- Server log: $($log.FullName)")
    Add-Line("")

    Send-MotmCommand "motm dev freecast on"
    Send-MotmCommand "motm dev class set $ClassId"
    Send-MotmCommand "motm dev daylight" 500
    $marker = "phase9-$ClassId-$runId"
    Add-RunMarker $marker

    foreach ($style in $allStyles) {
        $styleId = [string]$style.id
        Add-Line("## $styleId")
        Add-Line("")
        if (-not $SkipFlatlandsGate) {
            & (Join-Path $PSScriptRoot "ensure-flatlands.ps1") -VerifyOnly
        }

        Send-MotmCommand "motm dev class set $ClassId"
        Send-MotmCommand "motm dev styles clear"
        Send-MotmCommand "motm style $styleId" 1250

        foreach ($ability in @($style.abilities)) {
            $abilityId = [string]$ability.id
            $scenario = Get-AbilityScenario $ability
            $plan = Get-AbilityAuditPlan $styleId $ability $scenario
            $abilityStartLog = Get-LatestServerLog
            $abilityStartOffset = if ($abilityStartLog) { $abilityStartLog.Length } else { 0 }

            Add-Line("- ``$abilityId``")
            Add-Line("  - Scenario: $($scenario.Kind)")
            Add-Line("  - Setup: $($scenario.Setup)")
            Add-Line("  - Required proof: $($scenario.Proof)")
            Add-Line("  - Audit plan: $($plan.Note)")
            Ensure-ThirdPerson "$styleId-$abilityId-before" | Out-Null
            Prepare-AbilityEnvironment $styleId $abilityId $plan

            $trigger = ([string]$ability.trigger).ToLowerInvariant()
            if (@($plan.PostActions).Count -gt 0) {
                Send-MotmCommand "motm dev position" 350
            }
            if ($styleId.ToLowerInvariant() -eq "sand" -and $abilityId.ToLowerInvariant() -eq "dust_devil") {
                Send-MotmCommand "motm dev test ability sandstorm" 950
                Add-Line("  - Precondition: activated Sandstorm before Dust Devil")
            }
            if ($trigger -eq "jump_land") {
                Send-MotmCommand "motm dev test ability $abilityId" 900
                Invoke-MotionPlan $plan
                if (@($plan.PostActions).Count -gt 0) {
                    Send-MotmCommand "motm dev position" 350
                }
                Start-Sleep -Milliseconds $plan.WaitMilliseconds
            } else {
                $castDelay = $plan.WaitMilliseconds
                if ($plan.RequiresWeaponHit -and -not $plan.WeaponProofAfterCapture) {
                    $castDelay = [Math]::Min($plan.WaitMilliseconds, 700)
                }
                Send-MotmCommand "motm dev test ability $abilityId" $castDelay
                if (-not $plan.WeaponProofAfterCapture) {
                    Invoke-WeaponProof $styleId $abilityId $plan
                }
                $remainingWait = $plan.WaitMilliseconds - $castDelay
                if ($remainingWait -gt 0) {
                    Start-Sleep -Milliseconds $remainingWait
                }
                Invoke-MotionPlan $plan
                if (@($plan.PostActions).Count -gt 0) {
                    Send-MotmCommand "motm dev position" 350
                }
            }
            Ensure-ThirdPerson "$styleId-$abilityId-after" | Out-Null
            Capture "$styleId-$abilityId"
            if ($plan.WeaponProofAfterCapture) {
                Invoke-WeaponProof $styleId $abilityId $plan
            }

            $abilityLines = Read-NewLogLines $abilityStartLog.FullName $abilityStartOffset
            $resultLine = Get-AbilityResult $abilityLines $abilityId
            $spawnLine = $abilityLines |
                Where-Object { $_ -match "Style test mobs spawned: count=" } |
                Select-Object -Last 1
            $countLine = $abilityLines |
                Where-Object { $_ -match "Style test mobs tracked: count=" } |
                Select-Object -Last 1
            $stompLine = $abilityLines |
                Where-Object { $_ -match "Stomp landing resolved: targets=" } |
                Select-Object -Last 1
            $weaponLine = $abilityLines |
                Where-Object { $_ -match "Weapon follow-up:|Alloy Enhancement hit:|Style test weapon hit:" } |
                Select-Object -Last 1
            if ($resultLine) {
                $motionProof = Get-MotionProof $abilityLines
                $mechanical = Get-MechanicalProof $scenario $abilityLines $resultLine $abilityId $motionProof
                if ($plan.RequiresWeaponHit) {
                    if ($weaponLine -match "Weapon follow-up:|Alloy Enhancement hit:") {
                        $mechanical = [pscustomobject]@{ Status = "PASS"; Note = $weaponLine }
                    } else {
                        $mechanical = [pscustomobject]@{ Status = "FAIL"; Note = "Weapon follow-up was required but not applied." }
                    }
                }
                $screenshotPath = Join-Path $outDir "$styleId-$abilityId.png"
                $visual = Get-VisualProof $styleId $abilityId $scenario $plan $abilityLines $resultLine $screenshotPath
                Add-Line("  - Runtime: PASS")
                if ($spawnLine) { Add-Line("  - $spawnLine") }
                if ($countLine) { Add-Line("  - $countLine") }
                Add-Line("  - $resultLine")
                if ($weaponLine) { Add-Line("  - $weaponLine") }
                if (@($plan.PostActions).Count -gt 0) { Add-Line("  - Motion: $($motionProof.Status) - $($motionProof.Note)") }
                Add-Line("  - Mechanical: $($mechanical.Status) - $($mechanical.Note)")
                Add-Line("  - Visual: $($visual.Status) - $($visual.Note)")
            } else {
                Add-Line("  - Runtime: FAIL - cast result missing")
                Add-Line("  - Mechanical: FAIL - runtime proof missing")
                Add-Line("  - Visual: REVIEW - no screenshot can pass without runtime proof")
                if ($spawnLine) { Add-Line("  - $spawnLine") }
                if ($countLine) { Add-Line("  - $countLine") }
            }
            $residuals = Get-ResidualLines $abilityLines
            foreach ($residual in $residuals) {
                Add-Line("  - REVIEW: $residual")
            }
        }
        Add-Line("")
    }

    $log = Get-LatestServerLog
    $lines = Read-LogLinesAfterMarker $log.FullName $marker
    $lines | Set-Content -LiteralPath (Join-Path $outDir "server.log") -Encoding UTF8

    Add-Line("## Residual Scan")
    Add-Line("")
    $blocking = Get-BlockingResidualLines $lines
    if ($blocking.Count -eq 0) {
        Add-Line("- PASS: no blocking class/runtime errors in class audit slice.")
    } else {
        foreach ($line in $blocking) {
            Add-Line("- FAIL: $line")
        }
    }

    $missingCasts = $report | Where-Object { $_ -match "  - Runtime: FAIL" }
    $failedScenarios = $report | Where-Object { $_ -match "  - Mechanical: FAIL" }
    $conceptReviews = $report | Where-Object { $_ -match "  - Mechanical: REVIEW|  - Visual: REVIEW" }
    Add-Line("")
    if ($blocking.Count -eq 0 -and $missingCasts.Count -eq 0 -and $failedScenarios.Count -eq 0 -and ($conceptReviews.Count -eq 0 -or -not $RequireConceptProof)) {
        if ($conceptReviews.Count -gt 0 -and -not $RequireConceptProof) {
            Add-Line("- REVIEW: concept proof is incomplete for $($conceptReviews.Count) row(s). Runtime smoke can pass, but this is not FULL PASS.")
        }
        Add-Line("PASS")
        $status = "PASS"
    } else {
        Add-Line("FAIL")
        throw "Phase 9 $ClassId audit failed: missing casts=$($missingCasts.Count), mechanical failures=$($failedScenarios.Count), concept reviews=$($conceptReviews.Count), blocking=$($blocking.Count)."
    }
} catch {
    Add-Line("")
    Add-Line("FAIL")
    Add-Line("")
    Add-Line("Error: $($_.Exception.Message)")
    throw
} finally {
    if ($recordingProcess -and -not $recordingProcess.HasExited) {
        try {
            Stop-Process -Id $recordingProcess.Id -Force -ErrorAction Stop
            Add-Line("")
            Add-Line("- Video: recording stopped at audit end.")
        } catch {
            Add-Line("")
            Add-Line("- Video: could not stop recorder PID=$($recordingProcess.Id): $($_.Exception.Message)")
        }
    }
    $reportPath = Join-Path $outDir "report.md"
    $report | Set-Content -LiteralPath $reportPath -Encoding UTF8
    Write-Host "[audit-phase9-class] $status report: $reportPath"
}
