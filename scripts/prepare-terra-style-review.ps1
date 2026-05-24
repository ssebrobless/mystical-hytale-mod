param(
    [Parameter(Mandatory = $true)]
    [ValidateSet("metal", "magma", "stone", "arbor", "bloom", "self_petrification", "soil", "sand", "gem", "quake")]
    [string]$StyleId,
    [string]$WorldName = "MOTM Creative Test",
    [ValidateSet("creative", "adventure")]
    [string]$Mode = "creative",
    [switch]$SkipRelocate
)

$ErrorActionPreference = "Stop"

$stylePlans = @{
    metal = @{
        MobMode = "stationary"
        Summary = "Iron Wall, Metal Coat, Alloy Enhancement"
        UserActions = @(
            "Hold spellbook and cast Iron Wall facing the dummy.",
            "Cast Metal Coat and rotate in third person.",
            "Cast Alloy Enhancement, then switch to sword or pickaxe and attack/mine."
        )
    }
    magma = @{
        MobMode = "cluster"
        Summary = "Lava Pool, Obsidian Skin, Magma Sling"
        UserActions = @(
            "Cast Lava Pool into the clustered dummies.",
            "Cast Obsidian Skin and rotate in third person.",
            "Aim Magma Sling at the central dummy."
        )
    }
    stone = @{
        MobMode = "line"
        Summary = "Rubble Rouser, Pillar Strike, Rockslide"
        UserActions = @(
            "Cast Rubble Rouser down the line.",
            "Cast Pillar Strike on the closest dummy.",
            "Cast Rockslide down the target lane."
        )
    }
    arbor = @{
        MobMode = "stationary"
        Summary = "Rooted, Vines, Sapling"
        UserActions = @(
            "Cast Rooted and stay still while checking root placement.",
            "Cast Vines on the dummy.",
            "Aim Sapling at the ground near the dummy."
        )
    }
    bloom = @{
        MobMode = "cluster"
        Summary = "Nightshade, Frolick, Cacti Cluster"
        UserActions = @(
            "Fire Nightshade toward a surface beyond or near the dummy cluster.",
            "Cast Frolick, then move forward/sideways along the safe lane.",
            "Fire Cacti Cluster at the central dummy and watch the surrounding dummies."
        )
    }
    self_petrification = @{
        MobMode = "stationary"
        Summary = "Gargoyle, Glare, Tunnel"
        UserActions = @(
            "Cast Gargoyle and check the stone coating.",
            "Face the dummy and cast Glare.",
            "Cast Tunnel normally, then test Tunnel after Gargoyle if available."
        )
    }
    soil = @{
        MobMode = "line"
        Summary = "Burrow, Mudpit, Debris"
        UserActions = @(
            "Face the lane and cast Burrow.",
            "Cast Mudpit into the target area.",
            "Cast Debris down the line."
        )
    }
    sand = @{
        MobMode = "surround"
        Summary = "Sandstorm, Dust Devil, Vitrification"
        UserActions = @(
            "Cast Sandstorm while surrounded by dummies.",
            "With Sandstorm active, move forward and cast Dust Devil.",
            "Cast Vitrification during or after the Sandstorm combo."
        )
    }
    gem = @{
        MobMode = "cluster"
        Summary = "Lapidary, Fracture, Refraction"
        UserActions = @(
            "Cast Lapidary and inspect the floating gem object/HP display.",
            "Cast Fracture from the gem into the cluster.",
            "Cast Refraction and check the aura around the gem."
        )
    }
    quake = @{
        MobMode = "surround"
        Summary = "Stomp, Aftershock, Sinkhole"
        UserActions = @(
            "Use Stomp by arming it, jumping, and landing near targets.",
            "Test Aftershock by intentionally standing still/activating it.",
            "Cast Sinkhole on a target and watch for buried/crack/dust visuals."
        )
    }
}

$plan = $stylePlans[$StyleId]
if (-not $plan) {
    throw "No Terra review plan found for style: $StyleId"
}

Write-Host "[prepare-terra-style-review] Terra/$StyleId"
Write-Host "[prepare-terra-style-review] Abilities: $($plan.Summary)"
Write-Host "[prepare-terra-style-review] Layout: $($plan.MobMode)"
Write-Host "[prepare-terra-style-review] Mode: $Mode"

$setupArgs = @{
    StyleId = $StyleId
    WorldName = $WorldName
    Mode = $Mode
    MobMode = $plan.MobMode
}
if ($SkipRelocate) {
    $setupArgs.SkipRelocate = $true
}

& (Join-Path $PSScriptRoot "setup-terra-review.ps1") @setupArgs

Write-Host ""
Write-Host "[prepare-terra-style-review] USER ONLY:"
foreach ($action in $plan.UserActions) {
    Write-Host "  - $action"
}
Write-Host ""
Write-Host "[prepare-terra-style-review] Codex will handle cleanup, next style setup, mob count, mode switching, and screenshots/logs."
