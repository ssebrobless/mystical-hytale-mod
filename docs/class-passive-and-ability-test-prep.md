# Class Passive And Ability Test Prep

This is the manual-plus-agent lane for verifying MOTM gameplay. Use it when the goal is to test class passives first, then review each style ability through normal player controls.

```
╔══════════════════════════════════════════════════════════════════════╗
║ Review Order                                                        ║
╠══════════════╦════════════════════════╦══════════════════════════════╣
║ 1. Passives  ║ class identity systems ║ status, movement, damage     ║
║ 2. Styles    ║ three abilities each   ║ normal controls, visuals     ║
║ 3. Residuals ║ bugs or weak visuals   ║ patch, rebuild, retest       ║
╚══════════════╩════════════════════════╩══════════════════════════════╝
```

## Launch Boundary

Direct world launch is not reliable on this machine because Hytale offline direct launch requires `HYTALE_OFFLINE_TOKEN`. `scripts/start-hytale.ps1` and `scripts/cold-launch.ps1` therefore default to the official launcher/auth path. If world menu navigation misses, start Hytale through the official launcher, enter `MOTM Creative Test`, and then let Codex use the dev-command bridge.

Before launching or loading a world, clear stale clients so the harness cannot drive a crash dialog or older client window:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/reset-hytale-clients.ps1 -KeepLauncher
```

Before typing commands or staging a test, run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/check-world-entry-state.ps1
```

If it fails, fix the in-game state first: close crash dialogs, respawn if dead, or enter the flatlands lane.

## Passive Prep

Use this command for each class:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-class-passive-review.ps1 -ClassId terra
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-class-passive-review.ps1 -ClassId hydro
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-class-passive-review.ps1 -ClassId aero
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-class-passive-review.ps1 -ClassId corruptus
```

The script clears tracked mobs, relocates to the test lane, sets the class, switches to the right review mode, verifies third person, captures a screenshot, and collects observability evidence.

For actual A/B proof instead of a feel check, use:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-class-passive-proof.ps1 -TestId terra-mining-baseline
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-class-passive-proof.ps1 -TestId terra-mining-passive
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-class-passive-proof.ps1 -TestId terra-knockback
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-class-passive-proof.ps1 -TestId hydro-barrier
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-class-passive-proof.ps1 -TestId hydro-spell-vamp
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-class-passive-proof.ps1 -TestId aero-movement-baseline
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-class-passive-proof.ps1 -TestId aero-movement-passive
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-class-passive-proof.ps1 -TestId corruptus-stacks
```

The detailed proof matrix lives in [class-passive-proof-matrix.md](class-passive-proof-matrix.md).

```
┌───────────┬───────────┬─────────────────────────────────────────────┐
│ Class     │ Mode      │ Must Verify                                 │
├───────────┼───────────┼─────────────────────────────────────────────┤
│ Terra     │ Adventure │ -20% knockback taken, +50% pickaxe mining,  │
│           │           │ low-health regen, cave vision later         │
│ Hydro     │ Adventure │ 3% spell-vamp, Aqua Barrier first layer,    │
│           │           │ swim and oxygen in water-lane test          │
│ Aero      │ Creative  │ +25% movement, +80% energy, no strafe slow, │
│           │           │ no duplicate vertical boosts                │
│ Corruptus │ Adventure │ 3 kill stacks, revive to half HP, 10 minute │
│           │           │ passive lockout after resurrection          │
└───────────┴───────────┴─────────────────────────────────────────────┘
```

## Style Prep

Use this command when moving to a specific style:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/setup-style-review.ps1 -ClassId terra -StyleId metal -MobMode close
```

Pick `-MobMode` based on the ability:

```
┌────────────┬────────────────────────────────────────────────────────┐
│ Mob Mode   │ Use For                                                │
├────────────┼────────────────────────────────────────────────────────┤
│ clear      │ self buffs, movement visuals, held-item visuals        │
│ close      │ single target, melee, landing AoE, shield contact      │
│ stationary │ precise target placement, projectiles, sinkhole/vines  │
│ cluster    │ explosions, spreading damage, gem/cactus/lava fields   │
│ line       │ beams, waves, forward lanes, dash-through tests        │
│ surround   │ radial AoE, pull/expel, defensive aura checks          │
└────────────┴────────────────────────────────────────────────────────┘
```

Before every ability review:

1. Clear tracked mobs.
2. Confirm the player is not dead, paused, or falling.
3. Confirm third person when the body, held item, or ground visuals matter.
4. Spawn only the mob layout needed for that ability.
5. Test with the normal intended control, not `/motm dev test ability`, unless the goal is internal observability only.
6. Capture evidence and write residuals immediately if visuals or function are weak.

## Terra First Pass

Terra is the proving ground for reusable tricks: temporary blocks, body coatings, ground placement, movement/rooting, mob clusters, and held-item effects. Finish Terra to visual and functional quality before trusting the pattern for Hydro, Aero, and Corruptus.

```
Terra review loop

Passive prep
  └── style prep
        ├── ability 1 normal-control test
        ├── ability 2 normal-control test
        ├── ability 3 normal-control test
        └── residual patch + rebuild if needed
```

For current Terra order, start with `metal`, then continue style by style once the passive review is acceptable.
