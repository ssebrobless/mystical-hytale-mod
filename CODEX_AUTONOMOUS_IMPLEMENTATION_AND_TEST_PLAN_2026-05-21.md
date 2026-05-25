# Codex Autonomous Implementation + Test Plan — 2026-05-21

> **Superseded testing guidance:** This document is historical context for the
> earlier log-tail/screenshot/cold-launch strategy. For new feature work, follow
> `AGENTS.md` and `docs/agent-driven-verification-observability.md`; final
> acceptance should come from `scripts/run-agent-observability-baseline.ps1` and
> an `audits/agent-observability/<runId>/` evidence bundle.

> **Audience:** Medium-effort Codex running unattended in the Mystical-Hytale-Mod repo at
> `C:\Users\fishe\Documents\projects\Mystical-Hytale-Mod`.
>
> **Purpose:** Move the mod toward fully autonomous, self-audited, log-driven execution
> of the existing roadmap (Phase 5 → Realignment R1-R6 → Phase 6/7/8/9/10), with Codex
> controlling builds, log tailing, evidence capture, and as much of Hytale runtime
> validation as the OS/process model permits.
>
> **Read this together with — in priority order:**
> 1. `CODEX_PHASE5_FIX_2026-05-13.md`  (current blocker — Quake slice)
> 2. `CODEX_SINKHOLE_VISUALS_FIX_2026-05-13.md`  (Phase 5 visuals — Sinkhole + ground cracks)
> 3. `CODEX_IMPLEMENTATION_PLAN_2026-05-13.md`  (parent plan — Phases 1-10)
> 4. `CODEX_REALIGNMENT_PLAN_2026-05-13.md`  (R1-R6 — 40-style identity)
> 5. `ABILITY_COMPLETION_CHECKLIST.md`  (per-ability finish status)
> 6. `SPELLBOOK_UI_SPEC.md`  (Phase 6 UI architecture)
> 7. `CLAUDE.md`  (hard rules — protected data files, plugin lifecycle, build)
> 8. `PLAN.md`  (Phase 1/2 step list — mostly superseded; reference only)
>
> **This document does NOT duplicate the technical steps in those plans.** It indexes
> them, captures current-state truth as of 2026-05-21, and adds the operational layer
> (build/launch/test/evidence) Codex needs in order to grind through them autonomously.

---

## 0. TL;DR — what Codex does next, in order

1. **First**: clear the two open blockers (§7, §8) so Phase 5 can pass twice from cold launch.
   - 7.1 Verify Stomp `KnockbackResult` crash does not reproduce after a true cold Hytale launch. If it does, root-cause it per §7's decision table.
   - 7.2 Diagnose and fix the "idle slow after standing still" symptom per §8. Strong prior: Terra `Mountain Stance` perk (`stationary` condition) is leaking a slow modifier into outgoing damage / movement.
2. **Then**: run the Phase 5 vertical-slice acceptance script (§5.4) twice consecutively from cold launch.
3. **Then**: proceed phase-by-phase through the consolidated roadmap (§5) without skipping. Each phase ends with a green acceptance gate, a self-audit (§9), and an evidence bundle (§10) under `audits/<phase>/<timestamp>/`.
4. **Stop only on**: true blocker (decision table exhausted), data-protection violation, or build failure that doesn't have a documented recovery in the source plan.

This document does not relax the hard rules in `CLAUDE.md`. Surgical edits to `data/styles/*.json` only. No `--no-verify`. No regenerations. No stale-doc deletion (move to `docs/archive/` instead — see §11).

---

## 1. Summary of all existing plans and how they relate

```
 PLAN.md  (2026-Phase 1/2 step list)
   │   superseded by CODEX_IMPLEMENTATION_PLAN_2026-05-13.md; reference only.
   ▼
 CLAUDE_HANDOFF_2026-03-30.md
 CLAUDE_RECOVERED_PLAN_2026-03-31.md
 CLAUDE_RESUMED_PLAN_2026-03-31.md
 CLAUDE_HANDOFF_PROMPT_2026-03-30.txt
 CODEX_IMPLEMENTATION_PROMPT_2026-03-31.txt
 CODEX_CORRECTIONS_PLAN.md
   │   historical context for godclass refactor + recovery. Not active work.
   │   DO NOT delete — move to docs/archive/ at the very end (Phase 10).
   ▼
 CODEX_IMPLEMENTATION_PLAN_2026-05-13.md     ← parent, 10 phases (build → broaden → cleanup)
   ├── Phase 1  build/manifest sanity                 PASS
   ├── Phase 2  custom spellbook interactions         PASS
   ├── Phase 3  lifecycle split                       PASS
   ├── Phase 4  asset pack expansion (reference)      n/a — read when needed
   ├── Phase 5  Quake vertical slice                  IN PROGRESS — see overrides below
   │     ├── CODEX_PHASE5_FIX_2026-05-13.md           ← Phase 5 mechanical overrides
   │     └── CODEX_SINKHOLE_VISUALS_FIX_2026-05-13.md ← Phase 5 visual overrides (buried-look path)
   ├── Phase 6  custom Spellbook UI page              QUEUED (after Phase 5 passes twice)
   │     └── SPELLBOOK_UI_SPEC.md                     ← architecture for the UI sections
   ├── Phase 7  perk effect integration               QUEUED
   ├── Phase 8  PlayerInteractLib fallback (optional) QUEUED — conditional on lib in mods/
   ├── Phase 9  broaden to 39 other styles            QUEUED
   │     └── CODEX_REALIGNMENT_PLAN_2026-05-13.md     ← R1-R6 = the actual content roadmap
   │           └── ABILITY_COMPLETION_CHECKLIST.md    ← per-ability finish ledger
   └── Phase 10  cleanup + README + memory hygiene    QUEUED
```

**Relationship rule:** If two docs disagree, **CODEX_PHASE5_FIX_2026-05-13.md** and
**CODEX_SINKHOLE_VISUALS_FIX_2026-05-13.md** override the parent plan for Phase 5
specifically. **CODEX_REALIGNMENT_PLAN_2026-05-13.md** supersedes the parent's
Phase 9 (broadening). Everything else flows from the parent.

---

## 2. Current-state inventory (verified by reading the repo at 2026-05-21)

### 2.1 Implemented and confirmed working

| Layer | What's done | Confirmation |
| --- | --- | --- |
| Build/install | `scripts/build-install.ps1` downloads Gradle 9.1.0 + Temurin JDK 25, builds jar, installs to `%APPDATA%/Hytale/UserData/Mods/` | Script exists; build channel is `internal` by default. |
| Plugin bootstrap | `MenteesMod.java`, `manifest.json`, command bridge `/motm`, data loading | Per parent plan §0 "What is already wired"; existing logs show `[MOTM] Loaded ...`. |
| Spellbook interaction codecs | `MotmSpellbookInteraction.{Primary,Secondary,Use}` registered, JSON `MOTM_Spellbook_Focus.json` uses `{ "Type": "motm_spellbook_*" }` | Phase 2 of parent plan complete. |
| Lifecycle split | `onPlayerConnect` owns data + spellbook restore; `onPlayerReady` owns HUD | Phase 3 of parent plan complete. |
| Custom MOTM EntityEffect assets | 35 files in `src/main/resources/Server/Entity/Effects/MOTM/` including new `MOTM_Terra_Quake_Cast/Impact/Loop.json`, `MOTM_Terra_Sinkhole_Buried.json`, `MOTM_Terra_Ground_Cracks.json` | Confirmed present 2026-05-21. |
| Phase 5 mechanical primitives | `ground_targets_only` filter in `GameplayPlaybackManager.isTargetGrounded` (line 4668); Stomp arm + jump-land tick in same file (lines 280-358); Sinkhole buried-look via `MOTM_Terra_Sinkhole_Buried` tint + suffocation DoT + root | Code reads correctly; user reports the buried look + dust + tint + root + suffocation tick all work in-game. |
| Stomp impact ring (`spawnQuakeImpactRing`) | Lines 360-394 of `GameplayPlaybackManager.java`; uses `NPCEntity` proxies + `applyEffectById` | Builds green. Manual test crashed mid-run (see §7). |
| EntityEffect particle SystemId corrections | `MOTM_Terra_Quake_Impact.json` uses `Mace_Signature_Ground_Hit` + `Block_Break_Stone`; loop file uses `Earth_Brazier_Glow` (valid). Original `Mace_Signature_Shockwave` and `Block_Break_Stone_Dust` were rejected by this Hytale build. | Files confirmed at 2026-05-21. |
| `OverlapBehavior` correction | All MOTM JSONs use `Overwrite` / `Extend` instead of the rejected `Replace`. | Confirmed. |
| Class passives | `ClassPassiveManager` with Terra stationary shield (line 209, 365-372), `getTerraStationaryTicks` etc. | Wired. |
| ECS tick + mob spawn/death hooks | `MotmServerTickSystem`, `MotmMobRuntimeSystem` | Confirmed (parent plan §0). |

### 2.2 Implemented but failing or not-yet-validated

| Layer | What's done | Failure / blocker |
| --- | --- | --- |
| Stomp jump-land slice end-to-end | All code paths in place | World crashed mid-test with `NoClassDefFoundError: com/motm/manager/GameplayPlaybackManager$KnockbackResult` even though the jar contains that class. **Strong prior: stale server classloader.** See §7. |
| Idle "slow" after standing still | n/a | Reported user-facing symptom. Strong prior: a Terra perk with `condition: stationary` is mis-applying its buff (e.g. Mountain Stance `damage_reduction +5%/s`, Root Stance, Verdant Growth) and either the value is mis-signed or it's inadvertently registering as a movement-speed multiplier. See §8. |
| Phase 5 acceptance script (twice consecutively from cold launch) | n/a | Not yet completed. Both blockers above must clear first. |

### 2.3 Planned but not started

| Plan section | What's in flight | Trigger to start |
| --- | --- | --- |
| Phase 6 — Custom Spellbook UI page | `SpellbookPage.java` references `Pages/MOTM_Spellbook.ui` but that file doesn't exist. UI structure spec'd in `SPELLBOOK_UI_SPEC.md`. | After Phase 5 passes twice. |
| Phase 7 — Perk effect integration | `PerkManager.applyPerkEffects` is a log-only stub (per parent plan §0); 800 perks have no gameplay effect. New `PlayerStatModifierManager` and `PerkTriggerBinding` spec'd in parent plan §7. | After Phase 5 passes twice. **Strongly recommended to run before §8 idle-slow fix lands** — Phase 7's stat-modifier rebuild gives us a clean canonical place to handle conditional buffs, which is where Mountain Stance and friends should live. |
| Phase 8 — PlayerInteractLib fallback | Optional. Only proceed if `%APPDATA%/Hytale/UserData/Mods/PlayerInteractLib-*.jar` exists. | After Phase 5 passes twice. |
| Phase 9 / Realignment R1-R6 — 40-style content + identity | All 40 styles' visuals/abilities/feel coherence pass. R1 stamps palette into JSON; R2 reads it in `StyleData`; R3 routes the resolver by `(class, style, phase)`; R4 authors up to 160 EntityEffect files; R5 fixes mechanical gaps. | After Phase 5 passes twice. |
| Phase 10 — cleanup + memory hygiene | Move stale `CLAUDE_*` / `CODEX_*` to `docs/archive/`; README update; per-style EntityEffect convention documented. | Last. |

### 2.4 Data files protected from regeneration

These have **3 prior incidents of AI-driven loss**. Surgical edits only:

- `src/main/resources/data/styles/{terra,hydro,aero,corruptus}_styles.json`
- `src/main/resources/data/perks/{terra,hydro,aero,corruptus}_perks.json` (audit-style additions only — see realignment R5)
- `src/main/resources/data/classes/{terra,hydro,aero,corruptus}.json`

---

## 3. The cold-launch + classloader contract (non-optional)

Every "true cold launch" of the Hytale client/server is **prerequisite** to any
acceptance run. The earlier Stomp crash is the canonical example of why: a stale
server kept the previous jar's classes resident, then the new jar's class table
disagreed, then we observed a `NoClassDefFoundError` for a record that visibly
exists in the new jar.

### 3.1 The cold-launch ritual (Codex performs this before EVERY acceptance run)

```powershell
# 1. Kill every running Hytale process (client AND any embedded server).
$names = @("Hytale", "HytaleLauncher", "HytaleServer", "java")
foreach ($n in $names) {
    Get-Process -Name $n -ErrorAction SilentlyContinue | ForEach-Object {
        try {
            if ($_.MainModule.FileName -like "*Hytale*") {
                Write-Host "[cold] Stopping $($_.Name) PID=$($_.Id) Path=$($_.MainModule.FileName)"
                Stop-Process -Id $_.Id -Force
            }
        } catch { } # MainModule may be inaccessible on java.exe owned by other apps; skip those.
    }
}

# 2. Confirm no Hytale process survived.
Start-Sleep -Seconds 2  # only allowed sleep — for OS process cleanup. NEVER chain.
$leftover = Get-Process -Name "Hytale*" -ErrorAction SilentlyContinue
if ($leftover) { throw "Hytale process(es) still running: $($leftover.Name -join ', ')" }

# 3. (Optional but recommended) verify the installed jar is the newest build.
$mods = Join-Path $env:APPDATA "Hytale\UserData\Mods"
$installed = Get-ChildItem $mods -Filter "mentees_of_the_mystical-*.jar" |
             Sort-Object LastWriteTime -Descending | Select-Object -First 1
$built = Get-ChildItem "build\libs\mentees_of_the_mystical-*.jar" |
         Sort-Object LastWriteTime -Descending | Select-Object -First 1
if ($built -and $installed -and ($built.LastWriteTime -gt $installed.LastWriteTime)) {
    Copy-Item $built.FullName $mods -Force
    Write-Host "[cold] Re-installed jar: $($built.Name)"
}

# 4. Clear (or rotate) the mod's logs directory so each acceptance run gets a fresh file.
$logs = "logs"
$archive = Join-Path $logs "archive"
if (-not (Test-Path $archive)) { New-Item -ItemType Directory -Path $archive | Out-Null }
Get-ChildItem $logs -Filter "*_server.log" -ErrorAction SilentlyContinue |
    Move-Item -Destination $archive -Force
```

**Codex must run the ritual** before each acceptance run. Save it as
`scripts/cold-launch.ps1` (see §6.2). The user starts Hytale manually after the
ritual runs (Codex cannot click "Play" in the launcher — see §11 GUI strategy).

### 3.2 Class-table audit (stale classloader detector)

When ANY in-game crash mentions a class Codex believes exists, Codex runs this
**before** investigating any other cause:

```powershell
# Extract every class name from the built jar and the installed jar; diff them.
$jdk = Join-Path $PWD ".tools\jdk-25\bin"
$jarExe = Join-Path $jdk "jar.exe"
$mods = Join-Path $env:APPDATA "Hytale\UserData\Mods"
$built = (Get-ChildItem "build\libs\mentees_of_the_mystical-*.jar" |
          Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName
$installed = (Get-ChildItem $mods -Filter "mentees_of_the_mystical-*.jar" |
              Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName

& $jarExe tf $built | Sort-Object > "$env:TEMP\built_classes.txt"
& $jarExe tf $installed | Sort-Object > "$env:TEMP\installed_classes.txt"
Compare-Object (Get-Content "$env:TEMP\built_classes.txt") (Get-Content "$env:TEMP\installed_classes.txt") |
    Format-Table -AutoSize
```

If the diff is non-empty, **the installed jar lags the built jar**. Rerun
`build-install.ps1`, perform §3.1's cold-launch ritual, retry.

If the diff is empty AND the class definitely exists (via `jar tf | Select-String "<classname>"`)
AND the crash still reproduces from cold launch, then it is a genuine bug — proceed
to §7's KnockbackResult decision table.

---

## 4. The autonomous loop — how Codex executes a phase

Every phase, regardless of size, runs through the same loop. **Do not skip steps.**

```
┌──────────────────────────────────────────────────────────────────────────┐
│ PHASE LOOP                                                              │
├──────────────────────────────────────────────────────────────────────────┤
│ 1.  Read the source plan section for this phase (do not skim).         │
│ 2.  Self-audit prerequisites (§9.1).                                    │
│ 3.  Implement the changes (one logical commit boundary per step).      │
│ 4.  Build: `powershell -ExecutionPolicy Bypass -File scripts/build-install.ps1` │
│ 5.  Class-table audit (§3.2).                                          │
│ 6.  Cold-launch ritual (§3.1).                                          │
│ 7.  Run the phase's acceptance script (in-game; see §11 for what Codex │
│     can and can't do here without the user).                            │
│ 8.  Log-tail audit (§6.4): match required log patterns; capture        │
│     evidence (§10).                                                     │
│ 9.  Self-audit post-implementation (§9.2). If PASS → mark phase done   │
│     and proceed. If FAIL → walk the decision table; do not skip.       │
│ 10. Commit (no `--no-verify`). One commit per logical step.             │
└──────────────────────────────────────────────────────────────────────────┘
```

Codex stops only on:
- Decision-table exhaustion (every fork has been tried; the symptom persists).
- Build failure with no recovery path in the source plan.
- Any operation that would violate `CLAUDE.md` (regenerate styles JSON, etc.).
- An external resource missing (HytaleServer.jar gone, JDK 25 download fails).

---

## 5. Consolidated roadmap (the actual phase-by-phase work)

Phases 1-3 are done. The first work Codex does is **§7 + §8 (the two blockers)**,
then Phase 5, then everything after.

### 5.0 Blocker work (do FIRST, before Phase 5 acceptance)

| Step | Source | Goal |
| --- | --- | --- |
| **B1** | §7 of this doc | Reproduce / rule out the Stomp `KnockbackResult` crash after a true cold launch. If still present, walk §7's table. |
| **B2** | §8 of this doc | Find and fix the "idle slow after standing still" regression. |

### 5.1 Phase 5 — Quake vertical slice (the gate)

Source overrides:
- Mechanical: `CODEX_PHASE5_FIX_2026-05-13.md`
- Visuals: `CODEX_SINKHOLE_VISUALS_FIX_2026-05-13.md`

What's left after the blockers clear:
1. Run the acceptance script (§5.4 below) twice consecutively from cold launch.
2. Both runs must hit every required log line and every required visible behavior.
3. Capture evidence per §10.

### 5.2 Phase 5 acceptance script — exact, runnable, log-pattern-checked

Codex prepares the world (or the user does, see §11). Then for each click-step,
Codex tails the log (§6.4) and matches the required patterns.

```
Step  Action                                                              Required log pattern (regex)
────  ───────────────────────────────────────────────────────────────────  ───────────────────────────────────────────────────────────────────────
 0    Cold launch (§3.1). Run `/motm class terra`, `/motm race human`,    ^\[MOTM\] Loaded \d+ styles.*$
      `/motm style quake`. Place a grounded mob and a floating mob.       ^\[MOTM\] Player class set: .*terra.*$

 1    LMB once on the Mentees Spellbook.                                   ^\[MOTM\] Custom spellbook interaction fired: type=Primary slot=1
                                                                          ^\[MOTM\] Stomp armed: player=.*$

 2    Jump (Space). On landing:                                            ^\[MOTM\] Stomp fired at landing: player=.* pos=.*$
                                                                          ^\[MOTM\] Quake impact ring spawned at .* positions=\d+ applied=\d+$
                                                                          ^\[MOTM\] Stomp landing resolved: targets=\d+ damage=.* effects=\d+ visual=(applied|missing)$
       Grounded mob takes damage + knockback; floating mob is unaffected.

 3    Wait 2s. RMB once.                                                   ^\[MOTM\] Custom spellbook interaction fired: type=Secondary slot=2
                                                                          ^\[MOTM\] Queue ability cast: .* abilityId=aftershock$
       Ground-cracks ring at caster's feet, radius 5, persists 4s.

 4    Wait 5s. Use (E) once, aimed at the grounded mob at ~8m.             ^\[MOTM\] Custom spellbook interaction fired: type=Use slot=3
                                                                          ^\[MOTM\] Queue ability cast: .* abilityId=sinkhole$
                                                                          ^\[MOTM\] Sinkhole engaged: buried \d+ target\(s\) at center=.*$
       Within 0.6s: mob's bottom half goes near-black; dust billows; mob   ^\[MOTM\] Sinkhole released: \d+ target\(s\)$
       can't move; suffocation ticks. After 2.5s: tint clears, mob walks.

 5    Stand idle 10 seconds.                                               (no [MOTM] line about HP loss, slow, or movement)
       No HP loss. No movement slowdown.
```

**Both runs must produce every line.** If any line is missing or any visible
behavior is wrong, Codex walks the decision tables in `CODEX_PHASE5_FIX_2026-05-13.md`
§2.4-2.6 and `CODEX_SINKHOLE_VISUALS_FIX_2026-05-13.md` §8.

### 5.3 Phase 6 — Custom Spellbook UI page

Source: parent plan Phase 6 + `SPELLBOOK_UI_SPEC.md`.

Critical path: create `src/main/resources/Common/UI/Custom/Pages/MOTM_Spellbook.ui`
mirroring the dialect of `Common/UI/Custom/HUD/MOTM_StatusHud.ui`. Bindings
must match `com.motm.ui.SpellbookPage.java`'s event/value declarations exactly.
Do **not** modify `SpellbookPage.java` in this phase.

Acceptance: crouch+Use opens the custom UI (not chat fallback). All seven nav
sections respond. Ability rows show current style's three abilities.

### 5.4 Phase 7 — Perk effect integration

Source: parent plan Phase 7.

Adds `PlayerStatModifierManager` (owns Hytale-side EntityStatMap modifiers prefixed
`motm_perk_`), `PerkTriggerBinding` record, and rewires `PerkManager.applyPerkEffects`
+ new `applyAllOwnedPerks`. On-kill trigger heals caster a fraction of max HP.

**Cross-link to §8:** Phase 7's rebuild gives us the canonical site to apply
conditional buffs (`condition: stationary`, etc.) cleanly. If §8's root cause is
in `PerkManager` rather than in passives, Phase 7 may fix §8 as a side-effect.
**Do not rely on this** — fix §8 explicitly first.

### 5.5 Phase 8 — PlayerInteractLib fallback (conditional)

Skip unless `%APPDATA%/Hytale/UserData/Mods/PlayerInteractLib-*.jar` exists at
the start of the phase. Reflective subscription only (don't add a compile-time
dependency; the parent plan has the snippet).

### 5.6 Phase 9 (= Realignment R1-R6) — 40 styles, distinct identity

Source: `CODEX_REALIGNMENT_PLAN_2026-05-13.md`.

```
R0   Phase 5 must have passed twice. PRECONDITION.
R1   data: stamp palette block into all 40 style objects in *_styles.json     (no code change)
R2   code: read palette into StyleData                                        (model layer)
R3   code: resolver routes by (class, style, phase) before class-default      (asset layer)
R4   content: author up to 160 MOTM_<Class>_<Style>_<Phase>.json files        (content)
R5   code: mechanical compliance fixes (Section 3 table of realignment doc)   (gameplay layer)
R6   audit + cleanup pass                                                      (consolidation)
```

**Scoping rule for Codex per user constraint:** Phase 9 must respect the
user's per-style rotation order (terra/quake → hydro/icicle → aero/thunder →
corruptus/flame → terra/metal → hydro/snow → aero/jet → corruptus/necro → …)
and may NOT batch-author all 40 styles in one session unless explicitly told to.

After each style:
- Build + cold launch + run the per-style acceptance template (realignment doc §4.2.4).
- Self-audit (§9): visual identity check, mechanical compliance check.
- Capture evidence (§10) for that style.
- Commit. One style per commit.

### 5.7 Phase 10 — Cleanup + memory hygiene

Parent plan Phase 10. Move (don't delete) `CLAUDE_*.md`, `CLAUDE_*.txt`,
old `CODEX_*.md` to `docs/archive/`. Keep `CODEX_AUTONOMOUS_IMPLEMENTATION_AND_TEST_PLAN_2026-05-21.md`
and the three 2026-05-13 fix docs at the root until the user explicitly archives them.

Update README to reflect:
- Phase 5 passing
- 40-style identity in place
- Per-style EntityEffect convention documented
- Phase 7 wires stat-layer perks; remaining trigger types still TODO

---

## 6. Scripts Codex creates/uses for autonomous operation

All scripts live under `scripts/`. Codex creates the missing ones lazily before they're
needed. Each is idempotent (safe to rerun) and uses only PowerShell + .NET. **No third-party
dependencies** (no AutoHotkey unless the user has it installed and the user pre-approves
its use). All scripts are runnable directly from the repo root:

```
powershell -ExecutionPolicy Bypass -File scripts/<name>.ps1 [args...]
```

### 6.1 `scripts/build-install.ps1`  (exists)

Already present. Codex doesn't modify it without a reason; if the user asks for a
faster turnaround, Codex may add a `-Skip` flag that omits the `installMod` task.

### 6.2 `scripts/cold-launch.ps1`  (Codex creates)

The §3.1 cold-launch ritual, as a script. Codex creates this on first need.

### 6.3 `scripts/audit-classes.ps1`  (Codex creates)

The §3.2 class-table audit, as a script. Returns exit code 0 if installed == built,
nonzero if they diverge. Used as a guard inside `cold-launch.ps1`.

### 6.4 `scripts/tail-log.ps1`  (Codex creates)

Tail the newest `logs/<date>_server.log` and stream new lines to stdout. Optionally
match a regex; exit 0 on match, exit 1 on EOF without match within a timeout.

```powershell
# Usage:
#   .\scripts\tail-log.ps1 -Pattern "Stomp fired at landing" -TimeoutSeconds 15
# Returns: matched line (success) or throws (timeout)
param(
    [string]$Pattern,
    [int]$TimeoutSeconds = 30,
    [string]$LogDir = "logs"
)
$log = Get-ChildItem -Path $LogDir -Filter "*_server.log" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
if (-not $log) { throw "No server log under $LogDir" }
$deadline = (Get-Date).AddSeconds($TimeoutSeconds)
$start = $log.Length
$buffer = New-Object byte[] 65536
$fs = [System.IO.File]::Open($log.FullName, 'Open', 'Read', 'ReadWrite')
try {
    $fs.Position = $start
    $reader = New-Object System.IO.StreamReader($fs)
    while ((Get-Date) -lt $deadline) {
        $line = $reader.ReadLine()
        if ($null -eq $line) {
            Start-Sleep -Milliseconds 200
            continue
        }
        if ([string]::IsNullOrEmpty($Pattern) -or $line -match $Pattern) {
            Write-Output $line
            if (-not [string]::IsNullOrEmpty($Pattern)) { return }
        }
    }
    throw "Timeout waiting for pattern: $Pattern"
} finally {
    $fs.Dispose()
}
```

### 6.5 `scripts/acceptance-phase5.ps1`  (Codex creates)

Walks the §5.2 patterns against the current log. Used after a manual in-game run.
Reports PASS/FAIL per step and writes `audits/phase5/<timestamp>/report.md`.

### 6.6 `scripts/capture-evidence.ps1`  (Codex creates)

Takes a screenshot of the active window using `[System.Drawing.Graphics]::FromImage(...)`
+ `CopyFromScreen`. Saves to `audits/<phase>/<timestamp>/screen-<N>.png`.

For video, **only** if `ffmpeg.exe` is present in PATH or `.tools/`:
```powershell
& ffmpeg -y -f gdigrab -framerate 30 -i title="Hytale" -t 60 -c:v libx264 -preset ultrafast out.mp4
```
If ffmpeg isn't present, the script logs a clear warning and proceeds without video.
**Codex does NOT auto-install ffmpeg**; that's a user decision.

### 6.7 `scripts/probe-hytale-api.ps1`  (Codex creates as-needed)

Wraps the discovery commands from the realignment plan §7.2 — e.g.:
```powershell
param([string]$Symbol)
$jar = Join-Path $env:APPDATA "Hytale\install\release\package\game\latest\Server\HytaleServer.jar"
$jdkJar = Join-Path $PWD ".tools\jdk-25\bin\jar.exe"
& $jdkJar tf $jar | Select-String -Pattern $Symbol
```
Used when an import doesn't resolve or a method signature is uncertain.

---

## 7. Stomp `KnockbackResult` crash — diagnosis plan

**Symptom (observed once, 2026-05-21):** During manual test of Stomp landing,
the world crashed with `NoClassDefFoundError: com/motm/manager/GameplayPlaybackManager$KnockbackResult`.

**Verified state (2026-05-21):**
- `GameplayPlaybackManager.KnockbackResult` IS defined at line 6068 as a `private record`.
- It IS called from lines 3836, 4423, 5601 (`applyKnockbackResult`) and 5604-5657 (multiple call sites including `applyCombat` and `applyTargetEffects`).
- The installed jar at the time of the crash contained the class (the user verified `jar tf | Select-String KnockbackResult`).
- The Stomp impact ring path (`spawnQuakeImpactRing` → `applyEffectById`) **does not** itself reference `KnockbackResult`. The crash came from somewhere along the existing combat resolution path that fires as part of Stomp landing.

**Strong prior:** stale classloader — the Hytale server was running during the install,
caching old class bytecode that didn't have the record. The new jar wrote on top
but the JVM didn't reload classes mid-process. This is exactly the case §3 was
written to prevent going forward.

### 7.1 Decision table — KnockbackResult crash

| Sub-symptom (after cold launch from §3.1) | Action |
| --- | --- |
| Crash does NOT reproduce | Done. Move on to §8 + Phase 5 acceptance. Document in `audits/blockers/2026-05-21/knockback-result-resolved.md` that the crash was a stale-classloader artifact. |
| Crash DOES reproduce on the first Stomp-land of a cold launch | The crash is real. Continue down the table. |
| `audit-classes.ps1` shows a diff | The installed jar lagged. Rebuild + cold launch again. |
| `audit-classes.ps1` clean, crash still reproduces, AND log shows `NoClassDefFoundError: ...$KnockbackResult` | `record` is JDK 14+. Confirm `gradle.properties` has `org.gradle.java.installations.paths` pointing at the JDK 25 we use (`scripts/build-install.ps1` sets this). Inspect built jar with `javap -p com.motm.manager.GameplayPlaybackManager` and confirm the inner record class exists. If absent, gradle did not pick up JDK 25 — re-run after explicitly clearing `.gradle/` cache. |
| `record` desugared classfile differs from what runtime expects | Check `build.gradle` for a `sourceCompatibility` / `targetCompatibility` mismatch with Hytale's runtime (which the install ships its own JDK for). If Hytale runs a lower JDK than 14, replace the `record` with a final class with explicit getters. Keep the type and call sites identical. |
| Crash fires from a code path that loads `KnockbackResult` reflectively, not via direct reference | Search the codebase: `Grep -n "KnockbackResult" src` should show ONLY direct references. Confirm no reflective loaders. If a tool/lib is reflectively scanning classes, the class may be present but failing static init. Inspect `<clinit>` via `javap -c -p`. |
| `applyKnockbackResult` is called with a `Damage`/`KnockbackComponent` instance that doesn't exist on this Hytale build | Probe with `scripts/probe-hytale-api.ps1 KnockbackComponent`. If the Hytale-side API moved, adapt the call site without changing the `KnockbackResult` record. |

### 7.2 Self-audit after the fix lands

1. From cold launch, perform Stomp ten times in a row (arm + jump + land × 10).
2. No crash. All ten produce both required log lines per §5.2 step 2.
3. The mod's `logs/` for the session contain zero `NoClassDefFoundError` lines.

---

## 8. "Idle slow after standing still" — diagnosis plan

**Symptom (user-reported):** While standing still, the player accumulates some
slow or movement-impairment effect that is not part of the Phase 5 abilities.

**What I know from the code (2026-05-21):**
- `ClassPassiveManager` tracks `stationaryTicksByPlayer` (line 68) and increments
  while the player is stationary; clears on movement (line 365-372).
- Terra class passive (`data/classes/terra.json:39`) reads:
  *"Standing still for 2 seconds grants a shield equal to 5% of max health..."*
  That's a shield-on-stationary, not a slow.
- Terra perks with `condition: stationary` (and similar):
  - `terra_t01_root_stance` — +10% damage reduction at 1.5s
  - `terra_t04_deep_roots` — immunity to displacement
  - `terra_t05_verdant_growth` — 2× health regen
  - `terra_t11_fortress_master` — 2× armor
  - `terra_t12_mountain_stance` — +5%/s damage reduction up to 30%
- `PerkManager.applyPerkEffects` is currently a log-only stub (Phase 7 fixes this).
  So perks themselves should not be applying any runtime stat change today.
- That leaves: **a class-passive side effect**, **an unrelated status-effect tick**, or **a HUD-driven UI animation that visually reads as "slow" but isn't really a movement debuff**.

### 8.1 Decision table — idle slow

| Step | Hypothesis | Verification | Action |
| --- | --- | --- | --- |
| H1 | Terra class passive ("Earthen Resilience") applies more than just the shield while stationary — e.g. a movement modifier as a "rooted" feel. | Read `ClassPassiveManager.java:365-372` and the branches around `terraPassive.stationaryTicksRequired()`. Look for any `StatusEffectManager` calls keyed on `slow` / `root` / movement. | If found: remove or guard the slow-emit. Verify the description says nothing about slow. |
| H2 | A non-Phase-5 ability (Aftershock or Sinkhole) left a slow active on the player as the field expired with the player inside it. | Reproduce: cast Aftershock, stand inside it, wait it out, then stand still. Check `applyFieldTerrainEffects` and `processFieldTick` whether owner targets are excluded. | If owner is included: add `if (entity == field.ownerRef()) continue;` for slow tokens. The realignment plan §3.1 already flags this kind of gap. |
| H3 | HUD progress bar (`MotmStatusHud.java:129-138`) animates the stationary ticks → looks like a slow indicator but is actually shield charging. | Compare the in-game visual against the HUD ui markup. | If purely visual: clarify the HUD label (`Earthen Resilience charging`, not generic). |
| H4 | `Mountain Stance`-style stacking buff is *intended* to be defensive (damage reduction), but `applyPerkEffects` somewhere flipped sign and stored as a SLOW status effect on the player. | Search: `Grep "Mountain Stance\|terra_t12_mountain_stance\|stationary"` across the manager classes; confirm `PerkManager.applyPerkEffects` is still a stub (it is per parent plan §0). | Confirm zero perk runtime today → rules out H4 *until* Phase 7 lands. Phase 7 must NOT treat damage_reduction or armor as a slow. |
| H5 | The "slow" is actually a slow walk-back animation triggered by a Hytale-side default movement state when the spellbook is held. | Equip and unequip the spellbook; compare. | If only happens with the spellbook equipped: revisit the `MOTM_Spellbook_Focus.json` JSON for any stray movement-impairing field. (Phase 2 should already have removed `Block_Secondary` etc.) |
| H6 | A status effect that was applied during a Phase 5 test never expired. | Run `/motm status` (if such command exists) or inspect `StatusEffectManager` state via a debug log. | Add a one-shot `LOG.info` of the active status set on each tick for the test player and observe what's actually present. Remove any stale effect when found. |
| H7 | `applyFieldTerrainEffects` `lingering_tremor` branch (realignment R5) is silently re-applying slow to the caster as they sit in the Aftershock zone. | Check whether `lingering_tremor` is in the switch yet. Per realignment §3.1 this branch was a "gap" pre-R5. | If present: gate by owner. If absent: skip — H7 ruled out. |

### 8.2 Action sequence

1. Walk H1 first (highest prior: class passive is the only system definitely
   running today that keys on stationary state).
2. Then H6 (cheap to add a debug log).
3. Then H2 (reproduce specifically by walking inside an expiring field).
4. H3-H5 only if 1-3 don't surface the cause.

### 8.3 Self-audit after fix

1. From cold launch, no Phase 5 abilities cast. Stand still 60s. No HP/movement change.
2. Cast Stomp (arm + jump + land); wait 5s; stand still 30s. No persistent slow.
3. Cast Aftershock; let it expire; stand still 30s outside any field. No slow.
4. Cast Sinkhole on a mob; let it release; stand still 30s. No slow.

---

## 9. Self-audit checklists (Codex runs these unprompted)

### 9.1 Pre-implementation (each phase begins with these)

- [ ] Have I read the source plan section for THIS phase end-to-end?
- [ ] Are all prerequisite phases marked PASS in `audits/`?
- [ ] Are any of `data/styles/*.json`, `data/perks/*.json`, `data/classes/*.json`
      about to be modified? If yes, are the edits surgical (add fields / change
      single values; never reformat, reorder, or rewrite)?
- [ ] Are there any `// removed` / `// TODO` / `--no-verify` / empty catch blocks
      in the planned diff? If yes, restructure to avoid them.
- [ ] Is there a documented fallback path for every uncertain API call (`probe-hytale-api.ps1`)?

### 9.2 Post-implementation (each phase ends with these before the gate)

- [ ] `./scripts/build-install.ps1` is green.
- [ ] `./scripts/audit-classes.ps1` reports no diff.
- [ ] `./scripts/cold-launch.ps1` ran successfully.
- [ ] The phase's acceptance script (in-game) ran and all required log patterns matched.
- [ ] All required visible behaviors were observed (Codex prompts the user to confirm
      visuals it cannot programmatically verify — see §11).
- [ ] No regressions: re-run Phase 5 acceptance after every Phase ≥ 6 change.
      If Phase 5 fails, Codex stops and walks the Phase 5 decision tables before
      proceeding.
- [ ] Evidence bundle written to `audits/<phase>/<timestamp>/`.
- [ ] No new `WARN`/`ERROR` lines unrelated to known Hytale-side issues in this run's log.
- [ ] No data-file regenerations occurred (`git diff data/styles/`, `data/perks/`,
      `data/classes/` shows only additive surgical edits).

### 9.3 Phase 9 per-style audit (extra checks)

- [ ] The style's three abilities each fired cleanly via LMB / RMB / Use.
- [ ] Each ability's tint visibly matched the palette table (`CODEX_REALIGNMENT_PLAN_2026-05-13.md` §2).
- [ ] None of the three abilities looked identical to abilities of another style in the same class.
- [ ] The description in `*_styles.json` matches the visible behavior (cross-check realignment §3).
- [ ] Cooldowns enforced; resource cost deducted; no idle damage.

---

## 10. Evidence capture and acceptance reporting

Each phase writes:

```
audits/
  <phase-id>/                       e.g. "phase5", "phase9-terra-quake", "blocker-knockback"
    <timestamp>/                    e.g. "2026-05-21T14-30-00"
      report.md                     PASS/FAIL summary + log-line evidence
      server.log                    copy of the run's mod-side log
      screen-1.png                  pre-cast
      screen-2.png                  mid-cast
      screen-3.png                  post-cast (if visuals are subjective)
      run.mp4                       optional, only if ffmpeg present
```

`report.md` template:

```markdown
# Phase <id> acceptance run — <timestamp>

## Result
PASS | FAIL

## Acceptance script (verbatim from §5.2 or the phase's source plan)
<copy>

## Log evidence
| Step | Required pattern | Matched line |
| --- | --- | --- |
| ... | ... | ... |

## Visual evidence
- screen-1.png: <caption>
- ...

## Anomalies / WARN / ERROR lines
<grep WARN|ERROR from the run's server log>

## Next action
- if PASS: proceed to <next phase>
- if FAIL: walked decision table row(s) <X, Y, Z>; <result>
```

Codex generates this from `scripts/acceptance-<phase>.ps1` plus
`scripts/capture-evidence.ps1`.

---

## 11. GUI control of Hytale — realistic strategy + fallback

### 11.1 What Codex CAN do programmatically on Windows from PowerShell

| Capability | Mechanism | Limitations |
| --- | --- | --- |
| Kill / list Hytale processes | `Get-Process`, `Stop-Process` | Cannot start the Hytale Launcher and click through Play. |
| Read/tail log files | `[System.IO.File]::Open` (read-share) | Reliable. No GUI cost. |
| Capture screenshots of the active window | `[System.Drawing.Graphics]::CopyFromScreen` (System.Drawing) | Works fullscreen / windowed. Captures the desktop, not just Hytale — must crop manually if needed. |
| Capture screen video | `ffmpeg -f gdigrab` IF ffmpeg present | Skip if ffmpeg absent. |
| Focus a window by title | `[Win32.SetForegroundWindow]` via P/Invoke; UIAutomation `WindowPattern` | Brittle if there are multiple windows or the title localizes. |
| Send keyboard input | `[System.Windows.Forms.SendKeys]::SendWait("...")` or UIAutomation `Invoke` | Works for `/` chat, `E` (Use), `space` (jump), `LMB`/`RMB` clicks. Hytale must be the focused window. **Click events** require P/Invoke `mouse_event` or `INPUT` struct — possible but error-prone, especially in fullscreen exclusive mode. |
| Mouse-click at screen coords | P/Invoke `mouse_event(MOUSEEVENTF_LEFTDOWN, ...)` | Works but blind — no UI introspection. Must be paired with focused-window guarantee. |
| Discover Hytale UI elements | UIAutomation (DLL `UIAutomationClient.dll`) | Hytale's renderer is a custom game engine, not standard Win32/WPF/UWP — UIAutomation almost certainly returns "blank" for in-game widgets. Useless for in-game controls; may work for the launcher window's "Play" button. |

### 11.2 What Codex CANNOT do reliably without the user

- Click "Play" inside the Hytale Launcher (the launcher window controls are likely
  custom-rendered; UIAutomation will not see them). **The user starts Hytale.**
- Select a singleplayer world from the in-game menu (custom-rendered menu).
- Place test mobs via the creative tool palette (icon clicks).
- Confirm that a particle effect visually reads as "ground cracks" vs "generic dust"
  (subjective visual judgment).
- Distinguish "buried-look tint" from "generic dark texture" in a screenshot
  without a human eye.

### 11.3 The pragmatic split

| Action | Owner |
| --- | --- |
| Start Hytale; load into a creative singleplayer world; spawn test mobs in the right configuration; equip the Mentees Spellbook | **User** (one-time per acceptance run) |
| Run `/motm class terra`, `/motm style quake`, `/motm spellbook overview`, etc. | **Codex** via SendKeys after focusing Hytale, OR **user** via chat |
| LMB / RMB / Use clicks during acceptance | **Either**: Codex via `mouse_event` IF Hytale window is foregrounded AND not fullscreen-exclusive; otherwise **user** |
| Wait + observe log + capture screenshot at the right moment | **Codex** |
| Visual confirmation ("did the mob's lower body go near-black?") | **User**, via Codex's screenshot + a Yes/No question in the report |
| Cold-launch ritual (process kill, log rotate) | **Codex** |

### 11.4 Recommended primary mode: log-driven validation + user-driven inputs

Phase 5 specifically: the user performs the four click steps (LMB, jump, wait, RMB,
wait, Use). Codex tails the log, matches required patterns, captures screenshots
at known moments (right after each `Queue ability cast` log line), and produces
`report.md`. **This is reliable.**

Codex's stretch-goal: implement an opt-in `scripts/send-input.ps1` that uses
`SendKeys` + `mouse_event` to perform a scripted acceptance run when Hytale is
windowed and foregrounded. **Don't ship this without testing it on a throwaway
session — the click events can easily mis-target.**

### 11.5 Decision table — when GUI automation doesn't work

| Symptom | Action |
| --- | --- |
| `Stop-Process Hytale` returns "no such process" but the launcher is visible | The launcher binary may be named `HytaleLauncher.exe`. Check `Get-Process | Where-Object { $_.Path -like "*Hytale*" }`. |
| `SendKeys` to Hytale produces nothing | Hytale isn't the foreground window. `Add-Type` a P/Invoke for `SetForegroundWindow(hwnd)`; focus first; then SendKeys. |
| `SendKeys "{F5}"` works but `mouse_event` clicks register at desktop, not in-game | Hytale is fullscreen-exclusive. Switch to borderless-windowed in Hytale's options; SendInput should then route correctly. |
| Hytale doesn't accept programmatic input at all | Fall back to user-driven acceptance — Codex still owns log-tail, screenshot, report. Document the limitation in this doc's §11.3. |

---

## 12. Rollback strategy

### 12.1 Per-commit rollback

Codex commits **one logical step per commit**. If a step's acceptance fails AND
the decision table is exhausted:

```powershell
git status                                # confirm clean working tree
git log -1                                # confirm we're at the failing commit
git revert HEAD --no-edit                 # undo it as a NEW commit (do NOT reset --hard)
powershell -ExecutionPolicy Bypass -File scripts/build-install.ps1
```

Why revert not reset: parent plan rule + general safety. The reverted commit
stays in history for postmortem. **Never `git reset --hard` user-side branches
without explicit permission.**

### 12.2 Per-phase rollback

If an entire phase is unrecoverable:

1. Identify the merge-base with the previous PASS phase (`git log` finds it).
2. Branch off there: `git checkout -b rescue/<phase-id>-fallback <commit>`.
3. Cherry-pick non-controversial commits that survived the phase failure.
4. Document in `audits/<phase>/<timestamp>/rollback.md`.

### 12.3 Data-file safety net

Before any change touching `data/styles/`, `data/perks/`, `data/classes/`:

```powershell
git stash push -- data/styles data/perks data/classes -m "pre-edit snapshot $(Get-Date -Format yyyy-MM-ddTHH-mm-ss)"
# ... make the edit ...
git diff --stat data/styles data/perks data/classes      # confirm change is surgical
# if it looks wrong:
git checkout -- data/styles data/perks data/classes
git stash pop                                            # only if you want to keep the stash
```

`git stash` is non-destructive: even if Codex is wrong, the stash holds the
prior state. The three "Restore" commits in git history exist because earlier
runs lacked this discipline.

---

## 13. Cross-cutting decision tables (composite reference)

These reduce the cognitive overhead of which-doc-do-I-look-in. When a symptom hits
during ANY phase, Codex consults this index first:

| Symptom | Look in |
| --- | --- |
| Build error: HytaleServer.jar not found | `CODEX_IMPLEMENTATION_PLAN_2026-05-13.md` Phase 1 table |
| Build error: JDK 25 not found | parent plan Phase 1 table |
| Manifest tokens not expanded in built jar | parent plan Phase 1 table |
| `BuilderCodec` / `SimpleInstantInteraction` / `InteractionContext` doesn't resolve | parent plan Phase 2 table |
| Codec registration succeeds but interaction never fires in-game | parent plan Phase 2 table |
| `PlayerInteractEvent` not firing for the spellbook | parent plan Phase 2 + Phase 8 |
| HUD installs but no spellbook after relog | parent plan Phase 3 table |
| Phase 5 click registers but no AoE | `CODEX_PHASE5_FIX_2026-05-13.md` §2.4 / `CODEX_SINKHOLE_VISUALS_FIX_2026-05-13.md` §8 |
| Stomp arms but never fires on jump-land | `CODEX_PHASE5_FIX_2026-05-13.md` §2.4 |
| Sinkhole doesn't visibly bury the mob | `CODEX_SINKHOLE_VISUALS_FIX_2026-05-13.md` §8 |
| Idle slow / movement debuff appears after Phase 5 cast | THIS DOC §8 |
| `NoClassDefFoundError` for a class that does exist | THIS DOC §7 |
| Spellbook UI page does not open on crouch+Use | parent plan Phase 6 table |
| Perks select but visible HP/armor unchanged | parent plan Phase 7 table |
| New EntityEffect file not loaded | `CODEX_REALIGNMENT_PLAN_2026-05-13.md` §4.3 (R4) |
| Particles invisible despite tint applying | realignment §4.3 + §6 ("no invented SystemIds") |
| Two styles in the same class look identical | realignment §0.4 + §2 ("identity leakage"); fix routing in R3 |
| Ability description doesn't match visible behavior | realignment §3 (audit table — find the row) |

---

## 14. End-to-end final acceptance (Phase 5 + Phase 6 + Phase 7 + Phase 9 done)

When all prior phases pass, Codex runs the full end-to-end acceptance script ONCE
as a final integration audit. This proves that the mod's surfaces don't regress
each other.

### 14.1 The end-to-end acceptance script

```
0. Cold launch (§3.1). Open creative singleplayer world. Equip spellbook.

CLASSES — for each of {terra, hydro, aero, corruptus}:
  C1. /motm class <class>; /motm style <first style of that class>; equip spellbook
  C2. Cast LMB, RMB, Use. Verify three abilities fire with the class palette.
  C3. /motm spellbook overview                      → seven nav buttons visible (Phase 6)
  C4. /motm spellbook grimoire                      → three abilities show
  C5. /motm spellbook perks; pick one stat_increase perk; observe HP/armor change (Phase 7)
  C6. /motm spellbook resources                     → resource bar updates as abilities cast

STYLES — for each style validated in Phase 9 (40 total over time):
  S1. /motm style <styleId>; cast LMB, RMB, Use. Verify the style's palette renders, the
      three abilities visually differ in shape (cast/travel/impact/loop), and behavior
      matches the description.
  S2. Cooldowns enforced.
  S3. Resource cost deducted.
  S4. No idle damage or movement slowdown when standing still for 30s.

ABILITIES — for each ability ever flagged in the realignment §3 table as a "gap":
  A1. The fix from realignment §3 produces the described behavior.
  A2. Self-buffs visibly affect the caster (HUD pulse, particles).
  A3. Persistent fields visibly persist for `duration_seconds`.
  A4. Projectiles travel from caster to target along the right travel_type.
  A5. Summons spawn, fight, and despawn correctly.

HUD + CONTROLS:
  H1. Cooldowns render correctly on each slot.
  H2. Resource bar matches `/motm resources`.
  H3. Earthen Resilience HUD bar fills while stationary (Terra only).
  H4. Crouch+Use opens custom spellbook UI (Phase 6).

PERSISTENCE:
  P1. Relog; class + race + style + perks survive.
  P2. Relog; spellbook is restored to inventory.
  P3. Active status effects clear on death/respawn (no permanent debuffs).

TERRAIN / STATUS:
  T1. Aftershock + Sinkhole + every ground_zone field affects only ground targets when
      `ground_targets_only=true` is set.
  T2. Realignment §3 terrain_effect branches each apply their declared status tokens.

DAMAGE / KNOCKBACK:
  D1. Damage numbers visible (HUD).
  D2. Knockback fires for abilities that declare `knockback_force > 0`.
  D3. Lifesteal applies to abilities with `effect: lifesteal`.
```

Each row above resolves to a small log-tail pattern + screenshot. Codex's
`scripts/acceptance-final.ps1` walks them, producing `audits/final/<timestamp>/report.md`.

### 14.2 Final gate

Codex declares "the mod is acceptance-passing" only when:
- Every Phase 5/6/7/9/10 acceptance has a PASS report under `audits/`.
- The final integration acceptance (§14.1) produces a PASS report.
- `git status` is clean and the branch is on a single commit chain (no orphan
  reverts mid-stream).
- README and CLAUDE.md reflect current truth.
- `project_mystical_hytale_mod.md` memory note is updated (user does this — Codex
  cannot write into the user's Claude memory directly).

---

## 15. Hard rules and rails (the non-negotiables)

- **Surgical edits only** to `data/styles/*.json`, `data/perks/*.json`, `data/classes/*.json`.
  Never reformat, reorder, or rewrite. Three "Restore" commits in git history exist
  for this exact reason.
- **No invented particle SystemIds.** Only IDs already cited in
  `HytaleAssetResolver.java` or confirmed via `/showcase dump`. Unknown IDs cause
  silent visual no-ops AND can crash asset validation.
- **No `--no-verify` on commits.** If a hook fails, fix the underlying issue.
- **No `git reset --hard` / `git push --force`** without explicit user permission.
- **No deletion of `CLAUDE_HANDOFF_*` / `CLAUDE_RECOVERED_*` / `CLAUDE_RESUMED_*` /
  `CODEX_*` docs.** Move them to `docs/archive/` only when Phase 10 explicitly says so.
- **Phase 4 of the parent plan is reference-only.** Codex does not modify files in that phase.
- **Phase 8 is conditional.** Skip unless `%APPDATA%/Hytale/UserData/Mods/PlayerInteractLib-*.jar`
  exists at the start of the phase.
- **Phase 9 respects the per-style rotation order** (terra → hydro → aero → corruptus
  → repeat). One style per session. Do NOT explode to all 40 styles unless explicitly told.
- **Empty `catch (Throwable t) {}` blocks are banned.** Always log.
- **No half-implementations.** If a step's acceptance gate fails, fix it before moving on.
- **Cold launch before EVERY acceptance run.** (§3.1)
- **Class-table audit on EVERY in-game crash that names a class.** (§3.2)
- **One commit per logical step.** Easier to bisect.
- **Visual regression check after every commit ≥ Phase 6:** boot the server,
  cast Quake's three abilities, confirm Phase 5 still passes. Catching regressions
  early is cheaper than re-validating later.

---

## 16. Reference appendices

### 16.1 File / class cheat sheet (delta from parent plan)

| Concern | Location | Status 2026-05-21 |
| --- | --- | --- |
| Stomp arm + fire | `GameplayPlaybackManager.java` lines 280-358 | In place; suspected stale-classloader crash on landing — see §7 |
| Quake impact ring | `GameplayPlaybackManager.spawnQuakeImpactRing` lines 360-394 | In place |
| Sinkhole buried-look engage | (paths per `CODEX_SINKHOLE_VISUALS_FIX_2026-05-13.md` §5.2) | In place; user reports the visual loop works |
| Stomp suffocation DoT | `applySuffocationTick` (per fix doc §5.3) | In place |
| `MOTM_Terra_Quake_Cast.json` | `src/main/resources/Server/Entity/Effects/MOTM/` | Present |
| `MOTM_Terra_Quake_Impact.json` | same | Present, uses corrected SystemIds |
| `MOTM_Terra_Quake_Loop.json` | same | Present |
| `MOTM_Terra_Sinkhole_Buried.json` | same | Present, `OverlapBehavior: Overwrite` |
| `MOTM_Terra_Ground_Cracks.json` | same | Present (legacy — `MOTM_Terra_Quake_Impact` supersedes it for routing) |
| `KnockbackResult` record | `GameplayPlaybackManager.java` line 6068 | Present; called from 3836, 4423, 5601-5657 |
| Spellbook UI page asset | `Common/UI/Custom/Pages/MOTM_Spellbook.ui` | **MISSING** — Phase 6 creates |
| Perk effect stub | `PerkManager.applyPerkEffects` | Still a log-only stub — Phase 7 fixes |
| `PlayerStatModifierManager` | not yet created | Phase 7 creates |

### 16.2 Discovery commands

| Want | Command |
| --- | --- |
| Vanilla particle IDs (`/showcase dump`) | In-game, from Effect Showcase mod |
| Inspect built/installed jar | `.tools\jdk-25\bin\jar.exe tf <path>` |
| Find an API class location | `.tools\jdk-25\bin\jar.exe tf "$env:APPDATA\Hytale\install\release\package\game\latest\Server\HytaleServer.jar" \| Select-String -Pattern "<Class>"` |
| Inspect class methods | `.tools\jdk-25\bin\javap.exe -p -cp <jar> <fully.qualified.ClassName>` |
| Tail mod log | `scripts/tail-log.ps1 -Pattern "<regex>" -TimeoutSeconds <n>` (§6.4) |
| Audit class tables | `scripts/audit-classes.ps1` (§6.3) |
| Cold launch | `scripts/cold-launch.ps1` (§6.2) |
| Capture screenshot | `scripts/capture-evidence.ps1` (§6.6) |

### 16.3 Order of operations summary (one screen)

```
BLOCKERS (this doc)
  B1  §7  KnockbackResult cold-launch test → fix if reproduces
  B2  §8  idle-slow diagnosis → walk H1..H7

PHASE 5  (parent plan + the two 2026-05-13 fix docs)
  acceptance script (§5.2) twice consecutively from cold launch

PHASE 6  (parent plan §6 + SPELLBOOK_UI_SPEC.md)
  create Pages/MOTM_Spellbook.ui matching the HUD dialect + Java bindings

PHASE 7  (parent plan §7)
  PlayerStatModifierManager + PerkTriggerBinding + applyAllOwnedPerks

PHASE 8  (parent plan §8 — conditional)
  reflective PlayerInteractLib subscription if the lib jar is present

PHASE 9  (realignment plan R0..R6)
  R1 palette JSON  → R2 read in StyleData  → R3 resolver route  → R4 EntityEffects
  → R5 mechanical gaps (realignment §3 table)  → R6 audit/cleanup
  one style per session; rotate classes

PHASE 10  (parent plan §10)
  move stale plans to docs/archive/; README + memory hygiene

FINAL ACCEPTANCE
  §14 end-to-end integration audit

STOP → declare acceptance-passing
```

---

## 17. Notes specifically for Medium-effort Codex

- Read sections 0, 3, 7, 8, 11, 15 of this doc before doing anything else.
- The two existing 2026-05-13 fix docs are mandatory reading **before**
  attempting Phase 5. Do not re-derive their logic.
- Every uncertain API call gets `scripts/probe-hytale-api.ps1 <Symbol>` first.
- Every "this should work" intuition gets a class-table audit (§3.2) before
  you assume the runtime sees what your IDE sees.
- The user has experience with prior AI runs that regenerated style JSONs.
  If you find yourself about to write a whole `*_styles.json` file, **stop**
  and ask. The protected-data clause in `CLAUDE.md` exists because of three
  prior incidents.
- If a step here disagrees with the parent plan, the parent plan wins for
  Phase 1-4 and 7-10; the 2026-05-13 fix docs win for Phase 5; the
  realignment doc wins for Phase 9.
