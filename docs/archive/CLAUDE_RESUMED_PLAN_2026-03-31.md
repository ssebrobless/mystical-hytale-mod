# Resumed Implementation Plan — 2026-03-31

> **Context**
>
> I could not recover an actual saved Claude conversation thread from local project files.
> What I **could** recover was the live repo state it appears Claude/Codex left behind:
>
> - the handoff docs created on 2026-03-30
> - current uncommitted code changes in:
>   - [MenteesMod.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/MenteesMod.java)
>   - [MOTM_Spellbook_Focus.json](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/resources/Server/Item/Items/MOTM_Spellbook_Focus.json)
>
> This plan picks up from that point.

---

## 1. Where The Planning Most Likely Left Off

```text
Recovered direction
╔ join restore too late ══════════════════════════════╗
║ save file is correct                                ║
║ runtime restore is late                             ║
║ attempted fix: move restore earlier than            ║
║ `PlayerReadyEvent`                                  ║
╠ spellbook still dead ═══════════════════════════════╣
║ custom MOTM spellbook shell is the right direction  ║
║ attempted fix: richer inline interaction config     ║
║ unresolved: inputs still do not reliably cast       ║
╚ next planning target ═══════════════════════════════╝
  lock one reliable vertical slice
```

### Evidence of that direction

The current uncommitted diff shows:

1. `PlayerConnectEvent` being introduced in [MenteesMod.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/MenteesMod.java)
   - This strongly suggests the planning had moved toward restoring saved runtime earlier than `PlayerReadyEvent`.

2. Inline interaction objects in [MOTM_Spellbook_Focus.json](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/resources/Server/Item/Items/MOTM_Spellbook_Focus.json)
   - This strongly suggests the planning had moved toward making the custom spellbook emit real Hytale interaction events rather than relying on dead string ids.

So the resumed plan should **start there**, not restart from old dead ends.

---

## 2. Current Facts To Anchor The Plan

```text
Confirmed facts
├ player save persistence works
│  └ saved file already contains `terra + quake`
├ custom spellbook shell no longer behaves like a block
├ random join damage was at least partly caused by HP modifier churn
├ many thread-assert command issues were already fixed
├ `/motm cast` is not good enough as proof of gameplay health
└ live spellbook casting is still not reliable
```

### Meaning

The remaining work is **not** “the mod forgot your class/style.”  
It is:

```text
runtime contract problem
saved state exists
    ▼
world join lifecycle
    ▼
runtime restore timing
    ▼
spellbook in hand
    ▼
item interaction event contract
    ▼
slot cast routing
```

---

## 3. Recommended Planning Principle

```text
Do not plan by surface symptom
╔ bad approach ═══════════════════════════════════════╗
║ tweak one HUD thing, then one ability thing,        ║
║ then one spellbook thing, then another command      ║
╠ good approach ══════════════════════════════════════╣
║ lock one end-to-end vertical slice until it is      ║
║ undeniably working                                  ║
╚══════════════════════════════════════════════════════╝
```

### Vertical slice to lock first

```text
Target slice
join world
  ▼
saved `terra / quake` restored in 1–2 seconds
  ▼
spellbook restored in 1–2 seconds
  ▼
equip spellbook
  ▼
Left Click   ▶ Stomp
Right Click  ▶ Aftershock
Use          ▶ Sinkhole
  ▼
visible cast confirmation in arena
  ▼
no random damage / no weird movement slowdown
```

Do **not** broaden to all 40 styles until this slice is solid.

---

## 4. Ordered Implementation Plan

## Phase A — Instrument The Lifecycle, Not The HUD

### Goal
Prove exactly when runtime restore happens on join, and which step is late.

### Why first
Because “it took 30 seconds for Quake to come back” is still anecdotal unless join timing is instrumented end-to-end.

### Files
- [MenteesMod.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/MenteesMod.java)
- possibly [PlayerDataManager.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/manager/PlayerDataManager.java)

### Add precise logs for:

```text
join timing markers
├ PlayerConnectEvent fired
├ PlayerReadyEvent fired
├ PlayerData loaded
├ hasSavedLoadout = true/false
├ rebuildPlayerRuntimeNow start/end
├ ensureSpellbookItem start/result
├ queueSpellbookGrant invoked
├ pending spellbook grant processed
└ HUD install start/end
```

### Success test

Join the world once and inspect the log.  
You should be able to answer:

```text
exactly when did:
├ save data load
├ runtime rebuild
├ spellbook restore
└ HUD attach
occur, relative to join?
```

If the timeline is still ambiguous, do not move to Phase B yet.

---

## Phase B — Finalize The Correct Join Lifecycle Owner

### Goal
Choose which lifecycle event owns which responsibility.

### Current best hypothesis

```text
Lifecycle split
╔ PlayerConnectEvent ════════════════════════════════╗
║ load player data                                   ║
║ restore class/style/runtime state                  ║
║ restore spellbook if possible                      ║
╠ PlayerReadyEvent ══════════════════════════════════╣
║ attach HUD only                                    ║
║ final cosmetic synchronization                     ║
╚═════════════════════════════════════════════════════╝
```

### Why this is the best current direction

- Save data is already correct on disk
- `PlayerReadyEvent` has proven too late/too slippery as the only restore hook
- HUD is naturally more sensitive to “player fully ready” than runtime state is

### Files
- [MenteesMod.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/MenteesMod.java)

### Decision gate

If Phase A logs show `PlayerConnectEvent` gives usable `Player` + inventory access, commit to this split.  
If not, evaluate `AddPlayerToWorldEvent` or a hybrid connect+ready strategy, but do **not** fall back to `PlayerReadyEvent` alone.

### Success test

On join:
- class/style runtime is live almost immediately
- spellbook is present almost immediately
- HUD may still appear slightly after, and that is acceptable

---

## Phase C — Make Spellbook Input Observable End-To-End

### Goal
Prove where spellbook input dies.

### Why third
We already know the book shell is a problem area, but we still do not have a hard proof of the failing step:

```text
possible breakpoints
book equipped
  ▼
Hytale item interaction emitted?
  ▼
PlayerInteractEvent / PlayerMouseButtonEvent reached?
  ▼
recognized as MOTM spellbook?
  ▼
slot resolved?
  ▼
cast queued?
  ▼
runtime executed?
```

### Files
- [MenteesMod.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/MenteesMod.java)
- [MotmCommand.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/command/MotmCommand.java)
- [MOTM_Spellbook_Focus.json](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/resources/Server/Item/Items/MOTM_Spellbook_Focus.json)

### Required logging

For each spellbook input:

```text
input trace
├ held item id
├ event source
│  ├ interact
│  └ mouse
├ interaction action / mouse button
├ recognizedSpellbook yes/no
├ resolved slot number
├ cast queue accepted yes/no
└ runtime execution result
```

### Success test

One click on the book should produce one clean trace path in logs.  
If the first trace never appears, the issue is the item interaction contract.  
If the trace appears but runtime never fires, the issue is slot routing/cast execution.

---

## Phase D — Validate The Custom Spellbook Item Contract

### Goal
Settle whether the current custom item definition is the right approach.

### Current best hypothesis

The custom MOTM item is still the best direction, but the exact interaction contract may still be wrong or incomplete.

### Files
- [MOTM_Spellbook_Focus.json](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/resources/Server/Item/Items/MOTM_Spellbook_Focus.json)

### Evaluation checklist

```text
Spellbook shell checklist
├ does equipping it avoid block placement?          ▶ yes
├ does equipping it avoid movement slowdown?        ▶ should
├ does it emit real input events?                   ▶ unknown
├ does it preserve desired model/icon?              ▶ yes
└ does it need a different category / sound / tags? ▶ maybe
```

### Important planning guardrail

Do **not** revert to:
- recipe book shell
- weapon shell

Instead, compare the custom item’s interaction contract to the closest known-working custom interactive item examples and refine the contract.

### Success test

With the custom spellbook equipped:
- no block placement
- no movement slowdown
- `Left Click / Right Click / Use` each generate expected server-side trace lines

---

## Phase E — Lock Quake As The Canonical Test Style

### Goal
Use one style as the integration benchmark.

### Why Quake

```text
Quake is good because it exercises
├ slot 1 burst
├ slot 2 zone
├ slot 3 targeted control
└ Terra resource/passive interactions
```

### Files
- [terra_styles.json](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/resources/data/styles/terra_styles.json)
- [GameplayPlaybackManager.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/manager/GameplayPlaybackManager.java)

### Success criteria

After join, without extra setup churn:
- `quake` is already restored if it was saved
- the spellbook is already present
- each control casts the right slot
- arena visuals are obviously visible enough to confirm
- player does not take unexplained damage while idle testing

Do **not** move to other styles until this is true twice in a row across fresh relaunches.

---

## Phase F — Only After Quake Works, Broaden Carefully

### Order

```text
Expansion ladder
1. Terra / Quake
2. Terra / one second style
3. Hydro / one style
4. Aero / one style
5. Corruptus / one style
6. only then broader style sweep
```

### Why

Because right now the mod’s blocker is integration reliability, not content scarcity.

---

## 5. Specific Things Claude Should Check Immediately

### A. Is `onPlayerJoin(...)` doing too much duplicate work?

`onPlayerReady(...)` currently calls `onPlayerJoin(...)`, which:
- rehydrates resources
- reapplies perks
- reapplies race bonuses
- calls `classPassiveManager.onPlayerJoin(player)`

Then `rebuildPlayerRuntimeNow(...)` does many overlapping runtime reset/rebuild operations again.

This overlap may be contributing to fragile state timing.

### B. Is immediate `ensureSpellbookItem(...)` racing inventory readiness?

If the inventory is not ready at connect time, immediate grant may silently fail and the queue fallback may still be the real restore path.

### C. Are the spellbook item interactions actually emitting the same event type MOTM is listening for?

This still looks like the single most likely reason the book is in hand but casts do not happen.

---

## 6. A Good Definition Of “Done” For The Next Milestone

```text
Milestone
╔══════════════════════════════════════════════════════╗
║ Fresh relaunch                                       ║
║   ├ world join                                       ║
║   ├ saved quake restore in <= 2s                     ║
║   ├ spellbook present in <= 2s                       ║
║   ├ no idle damage                                   ║
║   ├ no movement slowdown                             ║
║   ├ Left Click   casts Stomp                         ║
║   ├ Right Click  casts Aftershock                    ║
║   └ Use         casts Sinkhole                       ║
╚══════════════════════════════════════════════════════╝
```

If that is not true, the milestone is not done, even if the HUD looks better or `/motm cast` works.

---

## 7. Bottom Line

```text
The resumed plan should not restart from zero.

It should continue from:
├ earlier join restore hook exploration
├ custom spellbook shell exploration
└ one-style vertical slice stabilization
```

The most likely productive next move is:

```text
1. instrument join timing
2. finalize lifecycle split
3. instrument spellbook inputs end-to-end
4. validate custom spellbook interaction contract
5. lock Quake as the first reliable slice
```

