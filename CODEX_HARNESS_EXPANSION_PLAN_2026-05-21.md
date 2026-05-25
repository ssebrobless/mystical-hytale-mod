# Codex Harness Expansion Plan — 2026-05-21

> **Superseded testing guidance:** This launcher/UI/screenshot harness plan is
> historical. The current canonical path is the in-mod observability harness in
> `AGENTS.md` and `docs/agent-driven-verification-observability.md`, driven by
> `scripts/run-agent-observability-baseline.ps1`.

> **Audience:** Medium-effort Codex running unattended in
> `C:\Users\fishe\Documents\projects\Mystical-Hytale-Mod`.
>
> **Purpose:** Extend the test/audit harness so Codex can drive Hytale end-to-end
> without the user babysitting it — launch the Hytale Launcher, press Play,
> navigate to the creative test world, spawn/position mobs, and resolve as many
> visual yes/no acceptance calls as possible programmatically. **Once this
> harness is in place, Codex returns to the main roadmap (blocker B1 → Phase 5
> → onward) and runs it autonomously.**
>
> **Read together with:** `CODEX_AUTONOMOUS_IMPLEMENTATION_AND_TEST_PLAN_2026-05-21.md`
> (the master plan — operational loop, evidence layout, hard rules).

---

## 0. Why this exists

The master plan §11 split the test harness into "Codex owns" vs "User owns":

```
Codex Owns                    User Owns (today)
────────────                  ──────────────
build/install                 launch Hytale
cold cleanup                  load creative world
class-table audit             spawn/position mobs
log tail/audit                visual yes/no calls
screenshots/reports
foreground input
```

The "User Owns" column is the bottleneck. Every Phase 5 / Phase 9 acceptance
run currently requires the user to perform 4-5 manual GUI steps. The user has
asked us to push that boundary as far as physically possible while keeping
fall-backs for the parts we cannot reliably automate.

The four targets, in difficulty order:

1. **Mob spawning** — easy. Hytale has chat commands; we already send chat via SendKeys.
2. **Launch Hytale** — medium. Start-Process or a CLI direct-launch path.
3. **Click Play / navigate Worlds menu** — hard. Hytale's UI is custom-rendered; UIAutomation cannot see it. Image-matching or fixed-coords are the realistic options.
4. **Visual yes/no calls** — variable. Pixel-region color checks are tractable for many cases; subjective judgments ("do these look like cracks?") still need a fallback.

---

## 1. Investigation Codex performs before writing any code

The harness depends on facts about this specific machine. Codex must verify these
**before** writing automation. Save outputs under
`audits/harness/2026-05-21/discovery/`.

### 1.1 Locate the Hytale Launcher executable and Hytale game binary

```powershell
# Common install locations — check all, report first hit
$candidates = @(
    "$env:LOCALAPPDATA\Programs\Hytale\HytaleLauncher.exe",
    "$env:LOCALAPPDATA\Hytale\HytaleLauncher.exe",
    "$env:APPDATA\Hytale\HytaleLauncher.exe",
    "$env:APPDATA\Hytale\install\release\package\game\latest\Hytale.exe",
    "$env:ProgramFiles\Hytale\HytaleLauncher.exe",
    "${env:ProgramFiles(x86)}\Hytale\HytaleLauncher.exe"
)
foreach ($c in $candidates) {
    if (Test-Path $c) {
        Write-Host "[discovery] found: $c (size=$((Get-Item $c).Length))"
    }
}

# Also enumerate the installed package directory tree
$pkg = "$env:APPDATA\Hytale\install\release\package\game\latest"
if (Test-Path $pkg) {
    Get-ChildItem $pkg -Filter "*.exe" -Recurse -ErrorAction SilentlyContinue |
        Select-Object FullName, Length
}

# Check for any uninstall-registry record that includes Hytale (gives canonical path)
Get-ChildItem "HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall",
              "HKCU:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall" |
    ForEach-Object {
        $disp = (Get-ItemProperty $_.PSPath).DisplayName
        $loc  = (Get-ItemProperty $_.PSPath).InstallLocation
        if ($disp -like "*Hytale*") { [PSCustomObject]@{ Name = $disp; Path = $loc } }
    }
```

**Record:** the exact path of (a) `HytaleLauncher.exe`, (b) the game client
executable if it has its own (e.g. `Hytale.exe`), (c) any CLI flags exposed.

### 1.2 Discover whether Hytale supports a direct-launch CLI

```powershell
# Try --help / -h / -? on whichever binary you found.
& "<path>\HytaleLauncher.exe" --help     2>&1 | Tee-Object "audits/harness/2026-05-21/discovery/launcher-help.txt"
& "<path>\HytaleLauncher.exe" -h         2>&1 | Tee-Object "audits/harness/2026-05-21/discovery/launcher-help-h.txt"
& "<path>\Hytale.exe" --help             2>&1 | Tee-Object "audits/harness/2026-05-21/discovery/game-help.txt"

# Also inspect any shipped scripts or .ini next to the binary
Get-ChildItem (Split-Path "<path>\HytaleLauncher.exe") -Filter "*.json" -Recurse |
    Select-Object FullName, Length
```

**Goal:** find any flag that lets us bypass the launcher's "Play" button — e.g.
`--play`, `--skip-launcher`, `--world <name>`, `--singleplayer`. If found,
**that is the preferred path** (eliminates Phase H2 in one go).

### 1.3 Discover whether Hytale's save files reveal a "test world" identifier

```powershell
$saves = "$env:APPDATA\Hytale\UserData\Worlds"
if (Test-Path $saves) {
    Get-ChildItem $saves -Directory | Select-Object Name, LastWriteTime, FullName
}
```

**Record:** which folder is the user's mod-testing world. We'll either pass it
to a CLI flag (1.2) or use it as the match target during menu navigation.

### 1.4 Discover screen geometry + Hytale window state

```powershell
Add-Type -AssemblyName System.Windows.Forms
$primary = [System.Windows.Forms.Screen]::PrimaryScreen
[PSCustomObject]@{
    Bounds  = $primary.Bounds
    Working = $primary.WorkingArea
    DPI     = (Get-CimInstance -ClassName Win32_VideoController).CurrentHorizontalResolution
}

# After Hytale is up, capture its window handle/title so we can foreground it
$proc = Get-Process -Name "Hytale*" -ErrorAction SilentlyContinue | Select-Object -First 1
if ($proc) { Write-Host "Window title: $($proc.MainWindowTitle); hwnd: $($proc.MainWindowHandle)" }
```

**Goal:** know whether the user runs fullscreen-exclusive (we cannot
`mouse_event` into that) vs borderless-windowed (we can). If fullscreen-exclusive,
Phase H2's image-matching falls back to SendKeys-only or to the user.

### 1.5 Discover the chat-command vocabulary for mob spawning

In-game (user does this once, dumps result to clipboard for Codex):

```
/help
/spawn ?         (try variants until something useful prints)
/summon ?
/give ?
/creative ?
/tp ?
```

Record which commands actually work and their argument grammars. Codex's
mob-spawn helper depends on this.

### 1.6 Sanity-check the existing harness scripts (already in place)

```powershell
foreach ($s in @(
    "scripts/cold-launch.ps1",
    "scripts/audit-classes.ps1",
    "scripts/tail-log.ps1",
    "scripts/capture-evidence.ps1",
    "scripts/build-install.ps1"
)) {
    Write-Host "--- $s ---"
    powershell -NoProfile -Command "& { . '$s'; }"  # parse-only smoke
}
```

**Goal:** confirm none of them regressed since the user wired them up today.

---

## 2. Phase H1 — Launch Hytale autonomously

### 2.1 Goal

`scripts/start-hytale.ps1` brings Hytale from cold (no processes running) to
"in a creative test world, foregrounded, ready for input" with **no user click**.

### 2.2 Strategy ranked by reliability

**A. Direct-launch CLI (preferred — if §1.2 found a flag).**
Skip the launcher entirely. Pass `--world <id>` (or whatever the flag is) to
the game binary; on launch, the client lands directly in the world. **This is
the cleanest path.** If §1.2 finds it, build H1 around it and skip H2 entirely.

**B. Launcher → SendKeys-driven Play (medium reliability).**
`Start-Process HytaleLauncher.exe`; wait for the launcher window to be ready
(poll `Get-Process` for `MainWindowTitle` matching the launcher); foreground it
via `SetForegroundWindow`; press Enter (the "Play" button is often the default
focused control in Electron-style launchers). Fall back to image-matching on the
Play button rectangle if Enter does nothing.

**C. Image-matching click on launcher's Play button (fallback).**
Capture the launcher window via the same screenshot path as
`scripts/capture-evidence.ps1`. Match the "Play" button using template
matching:

- Save a reference template at `scripts/templates/play-button.png` (the user
  captures this once; Codex can prompt for it or fall back to gracefully asking
  the user to provide it on first run).
- Use OpenCV via `python -m cv2.matchTemplate` IF Python + OpenCV are present.
- If not, fall back to a pure-.NET pixel-search: scan the screenshot for a
  contiguous region whose dominant color matches the Play button's primary
  color within a tolerance band.
- Click via P/Invoke `SendInput` at the matched center.

### 2.3 Implementation

Create `scripts/start-hytale.ps1`:

```
param(
    [string]$WorldName = $null,
    [switch]$DirectLaunch = $false,
    [int]$LauncherReadyTimeoutSec = 60,
    [int]$WorldLoadTimeoutSec = 180
)

# 1. Discover launcher path. Cache it under .tools/hytale-paths.json on first
#    success so future runs are instant. Re-discover if cache is stale (>30d)
#    or the cached path no longer exists.

# 2. If $DirectLaunch and a CLI flag is known (from §1.2), use it. Done.

# 3. Otherwise:
#    a. Start-Process HytaleLauncher.exe -PassThru
#    b. Poll for $proc.MainWindowTitle non-empty within $LauncherReadyTimeoutSec
#    c. Add-Type the SetForegroundWindow P/Invoke; foreground the launcher
#    d. Add-Type the SendInput P/Invoke (more reliable than SendKeys for games)
#    e. Try: SendKeys "{ENTER}" first. Watch for the game-client process to
#       spawn (poll Get-Process for the game .exe).
#    f. If after 5s no game .exe appeared, fall back to image-match-then-click
#       on the Play button.

# 4. Wait for the game client window to appear. Foreground it.

# 5. Phase H2 handles the world-pick.

# 6. Return $true on success; throw on timeout. Log every step to
#    audits/harness/start-hytale-<timestamp>.log
```

**SendInput / SetForegroundWindow / mouse_event P/Invoke pattern** (paste-able):

```
Add-Type -Name Win32 -Namespace Hytale -MemberDefinition @'
[DllImport("user32.dll", SetLastError = true)]
public static extern bool SetForegroundWindow(IntPtr hWnd);

[DllImport("user32.dll")]
public static extern bool ShowWindow(IntPtr hWnd, int nCmdShow);

[DllImport("user32.dll")]
public static extern void mouse_event(uint dwFlags, uint dx, uint dy, uint dwData, IntPtr dwExtraInfo);

[DllImport("user32.dll")]
public static extern bool SetCursorPos(int X, int Y);

public const int SW_RESTORE = 9;
public const uint MOUSEEVENTF_LEFTDOWN = 0x0002;
public const uint MOUSEEVENTF_LEFTUP   = 0x0004;
public const uint MOUSEEVENTF_RIGHTDOWN= 0x0008;
public const uint MOUSEEVENTF_RIGHTUP  = 0x0010;
'@
```

### 2.4 Acceptance gate

- `scripts/start-hytale.ps1` from a cold state ends with: the Hytale game client
  in the foreground, no leftover launcher window blocking input. Verified by:
  `Get-Process` shows the game-client process running, no `HytaleLauncher` process.
- A screenshot saved to `audits/harness/start-hytale-<timestamp>/main-menu.png`
  shows Hytale's main menu (or, with DirectLaunch, the world load screen).
- The script exits 0.

### 2.5 Decision table — Phase H1 forks

| Symptom | Action |
| --- | --- |
| `HytaleLauncher.exe` not found by §1.1 candidates | Re-run §1.1 with the user's `Get-ChildItem -Recurse -ErrorAction SilentlyContinue C:\ -Filter HytaleLauncher.exe` (slow, last resort). Update the candidate list. |
| Launcher starts but `MainWindowTitle` stays empty | Some launchers use a splash-screen window before the main one. Poll for any window owned by the launcher process whose title starts with "Hytale". |
| `SetForegroundWindow` returns false | The window is minimized OR Windows is blocking activation. Call `ShowWindow(hwnd, SW_RESTORE)` first. If still false, send Alt+Tab via SendInput as a focus-steal workaround. |
| Enter on launcher does nothing | Play button isn't keyboard-focused by default. Fall back to image-match (path C). If image-match also fails, document the obstacle and prompt the user once; cache the working sequence. |
| Game client window appears but stays black for >60s | Normal — Hytale loads slowly. Don't time-out on appearance alone; time-out on the actual main-menu visual or a known log line. |
| `mouse_event` clicks register on desktop, not Hytale | Hytale is fullscreen-exclusive. Document this in audits/. Prompt the user to switch to borderless-windowed in Hytale options. Phase H2 will need this. |

---

## 3. Phase H2 — Navigate to the creative test world

### 3.1 Goal

From the Hytale main menu, reach the user's mod-testing creative world.

### 3.2 Strategy ranked by reliability

**A. CLI direct-load (if Phase H1's CLI flag includes world selection).** Skip H2 entirely.

**B. Coordinate-relative clicks (low effort, low robustness).** If the user's
screen resolution is fixed and the menu layout doesn't change between sessions,
record the on-screen coords of "Singleplayer" → "Worlds" → the world tile, save
them to `scripts/templates/menu-coords.json`. Replay via `SetCursorPos` +
`mouse_event`.

**C. Image-matching clicks (more robust).** Same template-match flow as H1.C
but with `play.png`, `singleplayer.png`, `worlds-tab.png`, and a per-world
template (the user supplies on first run, named by the world folder from §1.3).
The template files live in `scripts/templates/menu/`.

**D. User-driven, harness-resumed.** If A/B/C all fail or are too brittle,
Phase H2 prompts the user once: "navigate to creative world `<name>` and press
F12; the harness will resume when it sees the F12 key." Codex registers a
global hotkey via `RegisterHotKey` P/Invoke; on press it captures a confirmation
screenshot and continues.

### 3.3 Implementation

Create `scripts/load-world.ps1`:

```
param(
    [string]$WorldName,
    [int]$LoadTimeoutSec = 180
)

# 1. Assert Hytale game client is foregrounded (from H1).
# 2. Try strategy A (CLI) if H1 used it.
# 3. Try strategy B (coord clicks) if a menu-coords.json template exists for
#    the current screen resolution. Validate each click by waiting for a
#    visual change at the expected next-screen template.
# 4. Try strategy C (image-matching).
# 5. Fallback: strategy D — prompt user, register F12 global hotkey, wait.

# 6. Wait for "in world" signal:
#    - the mod's log shows `[MOTM] >>> onPlayerConnect` after the user enters
#      the world; tail until match (re-uses scripts/tail-log.ps1)
#    - capture a screenshot for evidence

# 7. Return $true; throw on timeout.
```

### 3.4 Acceptance gate

- After `start-hytale.ps1` + `load-world.ps1` ran, `logs/<latest>_server.log`
  contains `[MOTM] >>> onPlayerConnect: ...` within `$LoadTimeoutSec`.
- A screenshot at `audits/harness/load-world-<timestamp>/in-world.png` shows
  the in-game first-person view.

### 3.5 Decision table — Phase H2 forks

| Symptom | Action |
| --- | --- |
| Image template doesn't exist yet | First run: capture full screenshot, prompt user to identify the bounding box of the relevant button. Crop and save under `scripts/templates/menu/<name>.png`. Cache. Future runs are auto. |
| Template matches multiple regions | Tighten the template's distinctive pixels OR add a second template-confirmation step. |
| Hytale changes menu layout in an update | Templates go stale; auto-detect by validating each click leads to the expected next-screen template within 5s. If not, void all templates for that menu and fall back to user-driven. |
| Multiple creative worlds visible; ambiguous which is "test world" | Tag the world in §1.3 by folder name; the per-world template must encode the world's tile name visually OR we click by stable list order. Save the resolved choice to `scripts/templates/menu/world-choice.txt`. |
| World loads but `onPlayerConnect` never logs | The mod jar didn't load. Re-run `audit-classes.ps1` and `build-install.ps1`. |

---

## 4. Phase H3 — Spawn and position test mobs

### 4.1 Goal

From in-world, set up the mob configuration Phase 5 acceptance requires:
- one grounded mob at ~8m in front of the player
- one floating mob within Stomp radius

### 4.2 Strategy

Use the Hytale chat commands discovered in §1.5. Codex sends them via SendKeys
to the focused Hytale window. The dominant commands across creative-style games
are some variant of:

- `/summon <entity>` — spawn an entity at the player position
- `/tp <x> <y> <z>` — teleport an entity (often the player; sometimes targetable)
- `/give @s <item>` — give an item (e.g. a creative spawn-egg)
- `/gamemode creative` — ensure free-cam / no damage etc.

**Codex does NOT assume any specific command works.** §1.5's discovery output
is the source of truth. If `/summon` doesn't exist, fall back to whatever does
(`/spawnEntity`, `/give @s spawn-egg`, etc.).

### 4.3 Implementation

Create `scripts/setup-test-world.ps1`:

```
param(
    [string]$Configuration = "phase5"
)

# 1. Assert in-world (logs show recent onPlayerConnect).
# 2. Foreground Hytale.
# 3. Send commands matching the requested configuration:
#
#    phase5:
#      /motm class terra
#      /motm style quake
#      /motm spellbook overview      (queues spellbook grant)
#      <commands from §1.5 to spawn 1 grounded mob 8m ahead>
#      <commands to spawn 1 floating mob 4m ahead, 3m up>
#
# 4. Wait for confirmation log lines (mod logs `[MOTM] Player class set: terra`,
#    `[MOTM] Player style set: quake`, etc.).
# 5. Capture a screenshot; verify two mobs visible via pixel-color check at
#    expected screen positions (see §5 for color-region validation).
# 6. Return $true.
```

### 4.4 Sending commands — the chat-open primitive

```
# Open chat via the key Hytale binds to chat (usually Enter or T). Verify in
# Hytale settings; record in scripts/templates/keybinds.json.
# Then type the command via SendKeys, then Enter.

function Send-MotmCommand {
    param([string]$Command)
    [System.Windows.Forms.SendKeys]::SendWait("t")      # or whatever opens chat
    Start-Sleep -Milliseconds 200
    [System.Windows.Forms.SendKeys]::SendWait($Command)
    Start-Sleep -Milliseconds 100
    [System.Windows.Forms.SendKeys]::SendWait("{ENTER}")
}
```

### 4.5 Acceptance gate

- The mod log shows `[MOTM] Player class set: ...` and `[MOTM] Player style set: ...`
- Two mobs appear (color-region check at expected screen positions OR the user
  confirms once on first run; Codex caches the working command sequence).

### 4.6 Decision table — Phase H3 forks

| Symptom | Action |
| --- | --- |
| Chat key isn't `T` | Read Hytale options; default may be Enter or `/`. Record once. |
| `/summon` doesn't exist | §1.5 discovery was incomplete. Open Hytale's command help in-game (user does this on first run); dump the list to clipboard. Codex updates the command vocabulary. |
| Mob spawns at unexpected position | The teleport command's coord system may be world-absolute, not relative. Compute correctly using player position from the mod log (the mod logs player pos on cast events). |
| Floating mob falls due to gravity | Use whatever entity has natural flight (bat, ghast equivalent, drone) OR apply a no-gravity tag if Hytale supports it. Discovery-driven; do not assume. |

---

## 5. Phase H4 — Auto-validate visual yes/no calls

### 5.1 Goal

Replace as many "user-judged visuals" with programmatic pixel/region checks
as possible. The user only confirms the genuinely subjective ones.

### 5.2 What is auto-validatable vs not

| Check | Approach | Tractable? |
| --- | --- | --- |
| "Mob's lower body went near-black" (Sinkhole buried-look) | Sample a 20×20 pixel region at the mob's lower-half screen position; assert average RGB ≤ (40,40,40). | YES — high-confidence. |
| "Dust particles billow around the mob" | Sample a wider region (60×60) around mob's feet; assert RGB variance increases vs pre-cast baseline. | YES — moderate confidence; tune threshold per first run. |
| "Mob can't move (rooted)" | Log-driven, not visual. Mod logs the root token application. | YES — log-only, no vision. |
| "Suffocation damage ticks" | Log-driven. `[MOTM] Sinkhole DoT:` (or equivalent). | YES — log-only. |
| "Ring of ground-cracks at landing point" | Hardest visual. Approach: sample a ring of pixels around the player's screen-projected position at radius 4 blocks; assert pixel hue is in earth-brown range and texture variance > flat-ground baseline. | PARTIAL — works as a "something happened here" check, NOT as a "looks like cracks" judgment. |
| "Aftershock ring persists 4s" | Log: field activation + field expiry timestamps; compute delta. | YES — log-only. |
| "Color identity by style" (Phase 9) | Sample the EntityEffect's tint on the caster; assert RGB is within the palette table tolerance (`CODEX_REALIGNMENT_PLAN_2026-05-13.md` §2). | YES — high-confidence. |
| "Does this look like wind / fire / ice?" (artistic identity) | Subjective. | NO — user. |
| "Does this visually feel different from the other styles?" | Subjective. | NO — user. |

### 5.3 Implementation: pixel-region helpers

Create `scripts/visual-validate.ps1`:

```
param(
    [Parameter(Mandatory)] [string]$Mode,    # 'avg-rgb' | 'variance' | 'palette-match'
    [Parameter(Mandatory)] [int]$X,
    [Parameter(Mandatory)] [int]$Y,
    [int]$Width = 20,
    [int]$Height = 20,
    [string]$Expected = $null,               # for avg-rgb: "#RRGGBB"; for palette-match: hex; for variance: numeric threshold
    [int]$ToleranceRgb = 25,
    [string]$ScreenshotPath = $null
)

Add-Type -AssemblyName System.Drawing

# 1. Take screenshot OR load existing one. Crop the region.
# 2. Per mode:
#    avg-rgb       → assert average RGB within tolerance of $Expected hex
#    variance      → assert per-channel stdev > $Expected threshold
#    palette-match → assert avg RGB falls within the named style's palette band
# 3. Write the cropped region image + the computed metric to
#    audits/<phase>/<timestamp>/visual-<X>x<Y>.png + metric.json
# 4. Exit 0 on pass; throw on fail.
```

### 5.4 The "mob screen position" problem

Pixel checks require knowing where the mob is on screen. Options:

**Option 1 — Anchor by the player's view-center.** The user always faces the
test mob during acceptance. The mob's center is at screen-center, lower body
~50px below center on a 1080p screen. Codex computes from the screen geometry
discovered in §1.4.

**Option 2 — Use the mod's debug log.** Have the mod print the mob's world
position on Sinkhole engage (it already does — `[MOTM] Sinkhole engaged: ... at
center=...`). The mod could *optionally* compute the screen-projection itself,
but that requires reading Hytale's camera state — not currently exposed. Stick
to Option 1.

**Option 3 — Color-find the mob.** Mobs have distinctive textures. Template
matching as in H2.C. Heavier, but works for any framing.

**Default:** Option 1. Codex requires the user to face the mob (a one-time
acceptance precondition logged in the script's docstring).

### 5.5 The user-fallback pattern

When a visual check is non-tractable, Codex writes to the report:

```
Step 4 — visual check "ring of ground-cracks visible at landing point"
  Method: pixel-variance around player feet, radius 4 blocks
  Auto-result: PASS (variance=82.4, threshold=50)
  User confirmation: needed (artistic identity check)
  Action: see audits/.../screen-2.png; please confirm Y/N in report.md
```

User opens `report.md`, writes Y or N at the placeholder, saves. Codex's
next-step gate consumes that. Specific file path:
`audits/<phase>/<timestamp>/user-confirmations.txt` with lines like:

```
step4-cracks: Y
step5-buried: Y
step7-aftershock-identity: N — looks like generic dust, not cracks
```

### 5.6 Acceptance gate

- `scripts/visual-validate.ps1` exists and handles all three modes.
- One end-to-end Phase 5 run uses it for the buried-look check (the easiest
  auto-validation) and produces a PASS report without user judgment on that line.

---

## 6. Phase H5 — Integration and roadmap re-entry

### 6.1 Wire the new scripts into the master plan's autonomous loop

Update `scripts/cold-launch.ps1` to optionally chain into start+load+setup:

```
param(
    [switch]$LaunchAndLoad,
    [string]$WorldName = $null,
    [string]$Setup = $null     # e.g. "phase5"
)

# existing cold-cleanup logic stays as-is.

if ($LaunchAndLoad) {
    & "$PSScriptRoot\start-hytale.ps1" -WorldName $WorldName
    & "$PSScriptRoot\load-world.ps1" -WorldName $WorldName
    if ($Setup) {
        & "$PSScriptRoot\setup-test-world.ps1" -Configuration $Setup
    }
}
```

### 6.2 Wire the new scripts into the acceptance scripts

`scripts/acceptance-phase5.ps1` (master plan §6.5) gains a `-Autonomous` switch.
With it, the acceptance run is:

```
1. cold-launch -LaunchAndLoad -WorldName <user's world> -Setup phase5
2. send each ability input via Send-MotmCommand / SendInput LMB/RMB/Use
3. tail log for the patterns in master plan §5.2
4. visual-validate the buried-look and any palette-match checks
5. capture screenshots at each step
6. write report.md with PASS/FAIL + any user-confirm placeholders
```

### 6.3 Re-entry into the main roadmap

**Once Phase H1-H4 acceptance gates have all passed once, Codex returns to the
master plan and runs the remaining roadmap autonomously:**

```
Step  Action
────  ──────────────────────────────────────────────────────────────────────
 1    Master plan B1 (§7 KnockbackResult cold-launch retest)
        - run `cold-launch -LaunchAndLoad -WorldName ... -Setup phase5`
        - perform Stomp 10× via acceptance-phase5.ps1
        - if no crash: B1 PASS, write audit, continue
        - if crash: walk §7 decision table

 2    Master plan B2 (§8 idle-slow diagnosis)
        - reproduce per §8.2 action sequence, all via the harness
        - fix; verify with §8.3 self-audit

 3    Master plan Phase 5 acceptance script — twice consecutively from cold launch
        - both runs use acceptance-phase5.ps1 -Autonomous
        - both must PASS

 4    Master plan Phase 6 — Spellbook UI page

 5    Master plan Phase 7 — Perk effect integration

 6    Master plan Phase 8 — PlayerInteractLib (conditional)

 7    Master plan Phase 9 — one style per session, rotating classes
        - per-style acceptance uses acceptance-phase5.ps1 pattern adapted

 8    Master plan Phase 10 — cleanup

 9    Master plan §14 end-to-end final acceptance
```

**Codex does not pause for the user between phases unless:**
- A decision table is exhausted (master plan + this doc + the per-phase plan docs).
- A user-confirmation visual check returns N or is missing.
- A new GUI obstacle appears that the harness can't handle (e.g. Hytale shipped
  an update that broke the templates).

---

## 7. Hard rules and rails (additions over master plan §15)

- **No sandbox bypass.** The harness uses normal Windows APIs (P/Invoke
  `SetForegroundWindow`, `mouse_event`, `SendInput`). It does NOT inject into
  Hytale's process, hook the renderer, or modify the game files. Anything
  resembling cheating or memory-editing is out of scope.
- **No third-party automation deps installed without explicit user permission.**
  OpenCV / ffmpeg / AutoHotkey / Python are used only if already present on
  the machine. Codex never silently installs them.
- **Templates live in version control.** `scripts/templates/*.png` and
  `scripts/templates/*.json` are checked in. They are regenerated only when
  Hytale's UI demonstrably changes; document the regeneration trigger in the
  commit message.
- **Coords cache is per-resolution.** `menu-coords.json` keys by
  `1920x1080`, `2560x1440`, etc. Loading at a different resolution requires
  recapture.
- **First-run friction is acceptable.** If the user has to provide one
  screenshot/template/F12-tap on the very first run for Codex to learn the
  layout, that is fine — subsequent runs must be hands-off.
- **Auto-validation never overrides user N.** If a pixel check says PASS but
  the user writes N in `user-confirmations.txt`, the step is FAIL.
- **Logs and screenshots from every run are committed to `audits/`.** Even
  failed runs — postmortem evidence is more valuable than disk space.
- **Visual checks always save the cropped region image** even on PASS, so the
  user can spot-check the validator's tuning.

---

## 8. Order of operations summary (one screen)

```
HARNESS EXPANSION (this doc)
  §1  Discovery       — find launcher, CLI flags, save dir, screen geometry,
                        chat-command vocabulary; sanity-check existing scripts
  H1  start-hytale.ps1     — launch + (if needed) image-match Play button
  H2  load-world.ps1       — main menu → creative test world
  H3  setup-test-world.ps1 — class/style/spellbook + spawn/position mobs
  H4  visual-validate.ps1  — pixel/variance/palette checks; user-fallback
  H5  Integration          — wire into cold-launch/acceptance scripts

RE-ENTRY (master plan + the per-phase plans)
  B1  KnockbackResult cold-launch retest                 (master plan §7)
  B2  idle-slow diagnosis                                (master plan §8)
  P5  Phase 5 acceptance script ×2                       (master plan §5.2)
  P6  Phase 6 — Spellbook UI page                        (parent plan §6)
  P7  Phase 7 — Perk effect integration                  (parent plan §7)
  P8  Phase 8 — PlayerInteractLib (conditional)          (parent plan §8)
  P9  Phase 9 — realignment R1..R6, one style/session    (realignment plan)
  P10 Phase 10 — cleanup + memory hygiene                (parent plan §10)
  FA  Final acceptance integration audit                  (master plan §14)
```

Each harness phase ends with PASS or FAIL in `audits/harness/<phase>/<timestamp>/report.md`.
Each main-plan phase resumes the master plan's autonomous loop (§4) using the new
scripts in place of user-driven inputs.

---

## 9. Reference appendices

### 9.1 Reliable Windows API patterns we rely on

| Need | API | Notes |
| --- | --- | --- |
| Focus a window | `SetForegroundWindow(hwnd)` + `ShowWindow(hwnd, SW_RESTORE)` first | Sometimes blocked by Windows focus-steal protection; Alt-Tab workaround. |
| Get a window handle | `Process.MainWindowHandle` | Empty during splash screens; poll until non-zero. |
| Type into the focused window | `[System.Windows.Forms.SendKeys]::SendWait($text)` | Slow for long input; reliable for chat commands. |
| Click at coords | `SetCursorPos(x, y)` + `mouse_event(...)` | Fails on fullscreen-exclusive games; use borderless-windowed. |
| Screenshot | `[System.Drawing.Graphics]::FromImage($bmp).CopyFromScreen(...)` | Captures the whole desktop; crop after. |
| Register a global hotkey | `RegisterHotKey(hwnd, id, modifiers, vk)` via P/Invoke | Lets the user signal Codex (F12 to resume). |

### 9.2 What Codex still cannot do, even with this harness

- Click controls inside Hytale's launcher if the launcher is a fullscreen
  always-on-top window without an Enter-focused Play button AND no CLI flag.
  In that case: §1.2's discovery output should record this and Phase H1 falls
  back to the user permanently for launch.
- Distinguish artistic identity ("does this look like wind vs. fire").
- React to in-game events that produce no log line (mod-side logging gaps).
- Recover from Hytale crashes that leave a zombie window — `Stop-Process` is
  still required.

### 9.3 Source pointers

- Master plan: `CODEX_AUTONOMOUS_IMPLEMENTATION_AND_TEST_PLAN_2026-05-21.md` —
  operational loop, evidence layout, hard rules, GUI strategy split (§11).
- Per-phase plans: `CODEX_PHASE5_FIX_2026-05-13.md`,
  `CODEX_SINKHOLE_VISUALS_FIX_2026-05-13.md`,
  `CODEX_IMPLEMENTATION_PLAN_2026-05-13.md`,
  `CODEX_REALIGNMENT_PLAN_2026-05-13.md`.
- Project rules: `CLAUDE.md` — never violated by harness work.
- Existing harness: `scripts/cold-launch.ps1`, `scripts/audit-classes.ps1`,
  `scripts/tail-log.ps1`, `scripts/capture-evidence.ps1`,
  `scripts/build-install.ps1`.
