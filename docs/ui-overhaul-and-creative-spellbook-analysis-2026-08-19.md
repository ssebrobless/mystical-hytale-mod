# UI Overhaul + Creative-Mode Spellbook — Analysis & Specification (2026-08-19)

Status: **INVESTIGATION + DESIGN — no code written yet.** For review before build.
Sources: `local://hytale-ui-research.md` (engine capability report), scout UI inventory,
and firsthand reads of `MOTM_Spellbook.ui`, `MOTM_StatusHud.ui`, `SpellbookPage.java`,
`MotmCommand.java` dev handlers.

---

## 0. Executive summary

The four requested workstreams are all **feasible with the engine we already use**, and the
mechanical substrate for the hardest one (a creative spellbook) **already exists** in the
mod's `dev` command handlers. Nothing here requires new gameplay mechanics — it is almost
entirely a **UI layer** over proven mutations.

```
                         WHAT WE ARE BUILDING
┌───────────────────────────────────────────────────────────────────────┐
│ 1. CREATIVE SPELLBOOK  ── new InteractiveCustomUIPage over the dev layer │
│      browse 4 classes / 40 styles / 120 abilities                       │
│      equip · unequip · test-cast · reset · repeat  (no progression gate)│
│                                                                         │
│ 2. UI / NAVIGATION OVERHAUL ── refine the existing progression spellbook│
│      clearer upgrades (styles/perks) + a "Mod Info" surface             │
│                                                                         │
│ 3. 120-ABILITY VERIFICATION ── drive the creative spellbook through     │
│      every ability: mechanical (server snapshot) + visual (screenshot)  │
└───────────────────────────────────────────────────────────────────────┘
```

**Single most important finding:** the `dev` handlers in `MotmCommand.java` write
`PlayerData` directly and bypass every production gate:

| Primitive | Handler (MotmCommand.java) | Effect | Bypasses |
|---|---|---|---|
| Set class (any) | `handleDevClass` :1756 `setPlayerClass(id)` | equip class | one-shot `selectClass` guard |
| Clear class | `handleDevClass` :1767 `setPlayerClass(null)` | unequip class | — |
| Set styles (arbitrary) | `handleDevStyles` :1828 + `resetRuntimeForLoadoutSwap` :1834 | equip styles, clean swap | style cap + cooldown carryover |
| Set perks (arbitrary) | `handleDevPerks` :1812 `setSelectedPerks(...)` | equip perks | milestone gating |
| Full reset | `handleDevReset` :1849 → `resetPlayerForDev` :2179 (`setPlayerClass(null)`, `resetCooldowns` :2216) | wipe loadout | all gates |
| Free casting | `handleDevFreeCast` | cast with no cost/cooldown | resource + cooldown |
| Test-cast / dummies | `handleDevTest` (`test ability <id>` / `test style <id>` / `test mobs`) | cast an ability, spawn/reset targets | — |
| Teleport to clear lane | `handleDevRelocate` (`relocate`/`unstuck`) | move player to open ground | — |
| Cooldown/runtime reset | `resetRuntimeForLoadoutSwap` :1965, `resetCooldowns` :2216 | clean repeat loop | — |

The creative spellbook is therefore a **presentation problem**, not a mechanics problem.

**Decisions needed from you** are collected in §7. The biggest three:
persistence model (snapshot vs sandbox), access gating (dev-only vs creative-world), and
whether we prove `TopScrolling` or commit to button pagination.

---

## 1. Engine capability constraints (the rules we must design within)

From `local://hytale-ui-research.md` (signatures pulled from the installed
`HytaleServer.jar`; caveats noted where only community-confirmed).

### 1.1 Widgets we can safely use (locally CONFIRMED)
`Group` (container/panel, incl. `Background: PatchStyle(Color)` or PNG), `Label`,
`TextButton` (with `Default`/`Hovered`/`Pressed` style states), `ProgressBar`. Hover/press
states confirmed.

### 1.2 Widgets that are UNPROVEN on 0.5.9 (community catalog only — do NOT assume)
`ScrollView`/`ScrollPanel`/`Scrollbar`, `Grid`/`ItemGrid`, `TextInput`/`TextField`,
`CheckBox`/`ToggleButton`, `Slider`, native `TabButton`/`TabNavigation`, `Dropdown`,
`Tooltip`, `CharacterPreviewComponent`, `ItemSlot`. The supplied `Assets.zip` had **0 `.ui`
members**, so none of these could be locally verified. **Design rule: build with the
confirmed set; each unproven widget requires its own tiny runtime proof before we depend
on it.** Tabs are already solved with `TextButton` + `Visible` panels (proven in MOTM).

### 1.3 Dynamic content (CONFIRMED API, bounded recommendation)
`UICommandBuilder`: `append(doc)`, `appendInline(sel, inlineDoc)`, `insertBefore(...)`,
`remove(sel)`, `clear(sel)`, `set(sel, value)`, `setNull(sel)`, `setObject(...)`.
So we can populate lists at runtime. **No hard element-count limit was found, which is not
proof of none** — so: use a **bounded predeclared pool of rows/slots + deterministic
pagination**, not a 120-card blast. `appendInline` for large lists only after a small proof.

### 1.4 The crash rule (CONFIRMED, live defect)
Runtime `set` of `Sprite.TexturePath` or `Group.Background` **hard-disconnects the client**
("CustomUI is not allowed to change this property"). Safe pattern: **predeclare every image/
background variant in the `.ui` and toggle `Visible`.** Text, color, visibility, and
`ProgressBar.Value` are freely writable at runtime.

### 1.5 Page vs HUD
- **Page** (`InteractiveCustomUIPage<T>`): interactive, full-screen, has event callbacks
  (`build` → bind events → `handleDataEvent` → mutate → `render(new UICommandBuilder())` →
  `sendUpdate`). Opened via `Player.getPageManager().openCustomPage(...)`. Lifetimes:
  `CantClose` / `CanDismiss` / `CanDismissOrCloseThroughInteraction`. **All creative
  controls live here.**
- **HUD** (`CustomUIHud`): display-only, `show()`/`update(false, commands)`, **no event API**.
  Requires `IncludesAssetPack=true`, deferred install after join, exact-case paths.

### 1.6 Non-negotiables
Case-sensitive paths (wrong case = disconnect at join); `IncludesAssetPack=true`; defer HUD
install; never runtime-write asset-bearing properties; keep interactive controls on a Page.

---

## 2. Existing UI inventory (what we have today)

### 2.1 Two surfaces
| Surface | Document | Java | Opened |
|---|---|---|---|
| Progression spellbook (Page) | `Pages/MOTM_Spellbook.ui` (198 ln) | `SpellbookPage.java` (847 ln), `SpellbookPageActions`, `SpellbookPageEventData` | crouch+Use (`SpellbookInputHandler`) |
| Status HUD (display) | `HUD/MOTM_StatusHud.ui` (1574 ln) | `MotmStatusHud.java`, `MotmStatusHudActions` | player-ready + 4-tick deferred install |

Chat/text fallback exists (`BookInteractionManager`, `SpellbookManager`) when CustomUI fails.

### 2.2 Spellbook page structure (`MOTM_Spellbook.ui`)
1040×704 panel: left **NavRail** with 5 `TextButton` tabs (Overview / Class / Abilities /
Perks / Stats) each mirrored by a hidden "> selected" `Label`; a **HeroPanel** header; a
**ContentRoot** holding 5 mutually-exclusive panels toggled by `Visible`:
- **Overview**: identity + next-step labels.
- **Class**: 4 class `TextButton`s + Choose + 12 legacy hidden `RemovedOptionButton`s + info.
- **Abilities**: 10 `StyleButton` slots (`StyleButtonsContainer`) + Choose + 3 `AbilityCard`s.
- **Perks**: 10 fixed `PerkRow`s (index/name/desc + Queue button) + Confirm/Clear.
- **Stats**: labels only.

### 2.3 The render/event pattern (proven, reusable)
`build()` → `commands.append("Pages/MOTM_Spellbook.ui")` → bind each `#id` via
`events.addEventBinding(Activating, "#id", EventData.append("Action",..).append("Value",..))`
→ `render(commands)`. `handleDataEvent` switches on `data.action`, mutates, then rebuilds
via `commands.set("#id.Visible", bool)` / `commands.set("#id", text)` and `sendUpdate`.

### 2.4 Selection flows and their GATES (why a creative page is needed)
- **Class**: spellbook `confirmClass` → `runCommand("class", id)` → `PlayerDataManager.selectClass`
  — **one-shot** (guard :304-308). You cannot freely re-pick class in production.
- **Styles**: `confirmStyle` → `runCommand("style", id)` → `StyleManager.selectStyles`
  — capped/gated; clears cooldowns on swap.
- **Perks**: `confirmPerks` → `PerkManager.applyPerkSelection` — **milestone-gated**
  (`hasPendingPerkSelection`, every 10 levels).
- `MenteesMod.isStartupSelectionProtected` protects first-join until class + styles chosen.

The production spellbook is a **progression tool** (choose your one class, unlock styles as
you level, pick a perk each milestone). A creative tool must **not** reuse this gated path —
it must call the **dev layer** (§0 table), which is exactly why that layer already exists.

### 2.5 HUD structure (`MOTM_StatusHud.ui`)
`StatusRoot` (2 status lines + 3 buff/3 debuff slots), `XpRoot` (`ProgressBar` + labels),
`PassiveTrackerRoot` (12 fixed rows, `Visible`-toggled), `ResourceRoot` (4 elemental
`ProgressBar`s), `AbilitySlotsRoot` (3 ability slots). Refreshed every 4 ticks
(`MotmRuntimeLoop.tickHudRefresh`) + on-demand after loadout changes.

---

## 3. Gap map — creative-spellbook needs vs what exists

| Need | Mechanic exists? | UI exists? | Gap |
|---|---|---|---|
| Browse all 4 classes | yes (data) | partial (4 buttons, but for one-shot pick) | new cross-class browser |
| Browse all 40 styles | yes (data) | only current class's 10 | class filter + 10-slot reuse (paginate by class) |
| Browse all 120 abilities | yes (data) | only selected style's 3 | style filter + 3-card reuse (paginate by style) |
| Equip any class freely | **yes** (`handleDevClass`) | no | new button → dev call |
| Unequip class | **yes** (`setPlayerClass(null)`) | no | new button → dev call |
| Equip/unequip any style, any count | **yes** (`handleDevStyles`) | no (capped in prod) | new toggle → dev call |
| Equip/unequip any perk freely | **yes** (`handleDevPerks`) | no (milestone-gated) | new toggle → dev call |
| Test-cast an ability on demand | **yes** (`handleDevTest ability`) | no | new "Test" button per ability |
| Free casting (no cost/cd) | **yes** (`handleDevFreeCast`) | no | new toggle |
| Spawn/reset test dummies | **yes** (`handleDevTest mobs`/`reset`) | no | new buttons |
| Teleport to clear test lane | **yes** (`handleDevRelocate`) | no | new button |
| Clean repeat (reset cd/runtime) | **yes** (`resetRuntimeForLoadoutSwap`) | auto on swap | wire to every swap |
| Master reset player | **yes** (`handleDevReset`) | no | new button |
| Live equipped/unequipped state | n/a | pattern proven | render text/color/Visible |
| Protect real character | **partial** (save/load) | no | **DECISION: snapshot/restore or sandbox** |

**Conclusion: every mechanical need already has a proven dev primitive.** The work is a new
page + wiring + state rendering + persistence policy.

---

## 4. Creative-mode spellbook — full feature specification

Design goal (your words): *navigate, equip, test, unequip, and repeat without any
issues/bugs.* Below is the exhaustive feature + edge-case enumeration so the page is
correct by construction.

### 4.1 Access & safety gating
- Opened only when **dev/creative mode is enabled** for the player (reuse `handleDevMode` /
  `devToolsEnabled`, already present) — never in a normal survival session.
- Distinct entry from the progression spellbook: a **dev "Creative Spellbook" item/command**
  (`/motm dev book` already opens a dev book — extend or add `/motm dev creative`).
- On open, **capture a snapshot** of the player's real loadout (class, styles, perks, level)
  so any experimentation is reversible (see §4.7).

### 4.2 Layout (tabs via proven `TextButton` + `Visible` panels)
```
┌ NavRail ────────┬ Content ─────────────────────────────────────────┐
│ > Classes       │  [tab-specific panel; all others Visible:false]   │
│   Styles        │                                                   │
│   Abilities     │  Header: current loadout summary (class/styles/   │
│   Perks         │          perks/level) — live-updated              │
│   Test Lab      │                                                   │
│   Info / Help   │  Footer: status line (last action result)        │
└─────────────────┴───────────────────────────────────────────────────┘
```

### 4.3 Classes tab
- 4 class entries (Terra/Hydro/Aero/Corruptus): name, element, passive name+desc.
- Per entry: **[Equip]** (dev set class) / **[Unequip]** (clear) — equipped one shows a lit
  state (color + "> EQUIPPED" label; no texture swap — crash rule).
- **[Clear Class]** master button.
- Edge: equipping a new class must **reset dependent styles/cooldowns** via
  `resetRuntimeForLoadoutSwap` (else stale style runtime). Unequipping class disables the
  Styles/Abilities tabs (grey state, not crash).

### 4.4 Styles tab
- **Class filter row** (4 buttons) selects which class's 10 styles to show — reuses the
  existing 10-slot pattern; no scrolling needed (10 fits).
- Each style slot: name + equipped indicator + **[Equip]/[Unequip]** (dev styles add/remove).
- **[Equip all in class]**, **[Clear styles]** convenience.
- **Creative cap toggle**: "Canonical (max N)" vs "Unlimited" — creative default Unlimited;
  Canonical enforces the real cap for realistic testing. (Verify real cap N — scout flagged
  `selectStyles` max 1 which conflicts with the 5-style design; confirm before build.)
- Edge: every equip/unequip calls `resetRuntimeForLoadoutSwap` (clean cooldowns), so repeat
  equip/unequip never leaves ghost cooldowns or half-applied runtime.

### 4.5 Abilities tab
- **Style filter** (paginate the selected class's 10 styles: prev/next or the 10 slots) →
  shows that style's **3 ability cards** (reuse `AbilityCard1..3`).
- Each card: name, id, summary, meta (cast type, cooldown, cost), and:
  - **[Test]** → `handleDevTest ability <id>` (cast now).
  - **[Target dummy]** indicator (uses Test Lab dummies).
- Requires the ability's style to be equipped OR auto-equip-on-test (creative convenience —
  temporarily equip, cast, leave equipped). Decide (§7).
- Edge: casting with no target → still valid (self/aoe); casting a projectile needs the clear
  lane (Test Lab relocate). Free-cast toggle removes cost/cd so rapid re-test works.

### 4.6 Perks tab
- All **20 perks** in the fixed 10-row pool → **paginate 2 pages** (rows 1-10, 11-20) with
  prev/next (bounded pool, deterministic — no scroll dependency).
- Per row: index, name, desc, **[Equip]/[Unequip]** (dev perks set/clear arbitrary).
- **[Clear perks]**; live **synergy preview** label (reuse `SynergyEngine` preview).
- Edge: perk changes call `recalculateStats`/`refreshPlayerProgressionBonuses` so HUD + stats
  reflect immediately.

### 4.7 Test Lab tab (the verification cockpit)
- **[Spawn dummy]** / **[Spawn mob line]** / **[Reset arena]** (`handleDevTest mobs`/`reset`).
- **[Teleport to flat lane]** (`handleDevRelocate`) — solves the dash-measurement terrain
  problem from prior sessions.
- **[Free-cast: ON/OFF]** (`handleDevFreeCast`), **[God/creative mode: ON/OFF]**
  (`handleDevMode`), **[Reset cooldowns]**, **[Clear effects]** (`handleDevClear`/`effects`).
- **[Reset player]** (master, `handleDevReset`).
- **Snapshot controls**: **[Restore my real loadout]** (re-apply the §4.1 snapshot) and
  **[Save current as snapshot]**. This is the persistence-safety mechanism.

### 4.8 Info / Help tab (feeds the "mod info" ask, §5)
- What the mod is, class/style/perk counts, current version, how progression works, a legend
  for HUD icons, and a link/pointer to controls. Static labels — safe.

### 4.9 State, correctness & bug-avoidance rules
1. **Single source of truth**: every action → dev mutation on `PlayerData` → full `render()`
   → `sendUpdate`. Never mutate UI state without re-reading `PlayerData` (mirrors current
   `SpellbookPage` which never trusts local UI state).
2. **Bounded pools + pagination** only (Classes 4, Styles 10/class, Abilities 3/style,
   Perks 10/page×2). No unbounded trees; no reliance on unproven scroll widgets.
3. **No runtime asset writes** — equipped/selected state shown via text + color + `Visible`
   only.
4. **Every loadout change routes through `resetRuntimeForLoadoutSwap`** so repeat
   equip→test→unequip loops are always clean (no ghost cooldowns, no stale runtime).
5. **Idempotent renders**: `render()` fully reconstructs every panel's visible/enabled/text
   state from `PlayerData` each call — no incremental drift.
6. **Disabled, not hidden, when invalid** (e.g. Abilities with no class): show greyed state +
   reason, never a dead button that errors.
7. **Dev-gated**: page refuses to open / all mutations no-op if `devToolsEnabled` is false.

---

## 5. UI / navigation overhaul (progression spellbook + mod info)

Separate from the creative page; refines the **player-facing** experience.

### 5.1 Navigation pain points today
- Class tab still carries 12 legacy hidden `RemovedOptionButton`s (dead nodes) — clean up.
- Upgrade paths (styles unlocked by level, perks at milestones) are shown as static labels;
  no clear "what can I upgrade now / next" affordance.
- No in-game "what is this mod / how do abilities work" surface (only chat + external docs).

### 5.2 Proposed overhaul (conservative, confirmed widgets)
1. **Overview tab = actionable dashboard**: "Pending: 1 perk (open Perks)", "Next style unlock
   at Lv X", current build summary — buttons jump to the relevant tab.
2. **Upgrades affordance**: on Perks/Abilities, a clear "AVAILABLE NOW" vs "LOCKED (Lv X)"
   state via color + label (no new widgets).
3. **Mod Info tab** (new, static labels): identity, counts, progression rules, HUD legend,
   version. Shared content with the creative page's Info tab.
4. **Remove dead `RemovedOptionButton*` nodes** from `MOTM_Spellbook.ui`.
5. Optional: HUD legend/first-run tips (the first-join wizard already messages; add a
   persistent Info reference).

Scope is deliberately modest — the progression spellbook already works; this is polish +
the Info surface, not a rewrite.

---

## 6. 120-ability visual + mechanical verification strategy

The creative spellbook becomes the **driver** for exhaustive verification.

### 6.1 The matrix
120 rows = 4 classes × 10 styles × 3 abilities. Each row gets two verdicts:
- **Mechanical PASS**: server-truth snapshot shows the ability executed with expected
  effects (damage/impulse/effect/summon/projectile), 0 SEVERE, 0 NPE — reuse the existing
  agent-observability harness (`run-agent-observability-baseline.ps1`, scenario JSONs,
  baseline-report.md) that already proved reactions/dashes/leveling.
- **Visual PASS**: screenshot mid-cast shows the intended VFX (the gap flagged in the
  release-readiness review — currently only major abilities are screenshotted).

### 6.2 The loop (per ability), automated where possible
```
for class in [terra,hydro,aero,corruptus]:
  dev set class; dev relocate (flat lane); dev freecast on; spawn dummy
  for style in class.styles(10):
    dev styles set <style>            (creative equip, clean swap)
    for ability in style.abilities(3):
      dev test ability <id>           (cast)
      capture server snapshot         (mechanical)
      screenshot mid-cast             (visual)
      record row verdict -> matrix
  reset arena
restore real loadout
```
This is exactly what the creative spellbook's Test Lab automates interactively, and what a
scenario script can automate headlessly.

### 6.3 Output
A living `docs/ability-verification-matrix.md` (120 rows × mechanical/visual/notes), plus
screenshot artifacts per ability. Drives a punch-list of any ability with wrong/missing VFX
or mechanics — the concrete "is the mod actually complete" evidence the release review said
was missing.

### 6.4 Why this order
Build the creative spellbook **first** (it makes equip+test of any ability a 2-click loop),
then run the 120-matrix through it. Doing the matrix without the tool means fighting the
progression gates for every ability.

---

## 7. Open decisions (need your call before building)

1. **Persistence / safety model** — how do we protect a real character while testing?
   - (a) **Snapshot/restore** the player's real loadout on open/close *(recommended: simplest,
     reuses save/load)*.
   - (b) Dedicated **creative sandbox world/profile** (heavier; cleanest separation).
2. **Access gating** — dev-only (behind `devToolsEnabled`) *(recommended)*, or a player-facing
   "creative world" toggle so non-dev creative worlds can use it too?
3. **Large-list rendering** — commit to **button pagination** now *(recommended, fully
   proven)*, or first spend a proof on `TopScrolling`/`ScrollView` to allow smooth scrolling?
4. **Style cap in creative** — allow **Unlimited** equip with a "Canonical" toggle
   *(recommended)*; and please confirm the real canonical style count (design says 5 owned;
   code `selectStyles` appears to cap at 1 active — need to reconcile).
5. **Auto-equip on Test** — when testing an ability whose style isn't equipped, auto-equip it
   temporarily *(recommended for speed)* or require manual equip first?
6. **Overhaul scope** — just add the Info tab + upgrade affordances + dead-node cleanup
   *(recommended)*, or a broader progression-spellbook redesign?
7. **Build sequencing** — recommended: (i) creative spellbook, (ii) 120-ability matrix run,
   (iii) fix ability defects found, (iv) progression UI overhaul + Info tab. Agree?

---

## 8. Proposed build phases (after decisions)

```
Phase A  Creative spellbook page (new .ui + CreativeSpellbookPage.java)
         · tabs, bounded pools, dev-layer wiring, snapshot/restore, Test Lab
         · smoke: open in-world, equip/unequip/test each tab, repeat loop clean
Phase B  120-ability verification matrix
         · scenario automation + creative-page-driven capture
         · produce ability-verification-matrix.md + screenshots; punch-list defects
Phase C  Fix ability visual/mechanical defects surfaced by B
Phase D  Progression UI overhaul: Info tab, upgrade affordances, remove dead nodes
Phase E  Re-verify, docs, machine-at-rest
```

Each phase ends with an in-world smoke proof (the project's standing bar), not just unit
tests. No release is attempted (per your hold).
