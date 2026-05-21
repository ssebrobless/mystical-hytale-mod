# Claude Handoff — Runtime / Spellbook / Join-State Triage

> **Project:** `C:\Users\fishe\Documents\projects\Mystical-Hytale-Mod`
>
> **Date:** 2026-03-30
>
> **Purpose:** factual handoff for planning, not a victory lap. This documents the specific runtime problems we have been hitting, the fixes already attempted, what partially helped, and what is still unreliable.

```text
Current theme
╔══════════════════════════════════════════════════════════════╗
║ The mod is not blocked by content volume anymore.           ║
║ It is blocked by runtime integration reliability.           ║
║                                                              ║
║ Main pain points:                                           ║
║ 1. saved class/style/loadout restore timing on join         ║
║ 2. spellbook item shell + input routing                     ║
║ 3. thread-safe world/runtime operations                     ║
║ 4. HUD/custom UI stability and readability                  ║
╚══════════════════════════════════════════════════════════════╝
```

---

## 1. Repo Snapshot

```text
Git
├ committed baseline  ▶ `e9e638b`
│                      `Stabilize spellbook testing and runtime restoration`
└ local uncommitted    ▶ yes
   ├ `src/main/java/com/motm/MenteesMod.java`
   └ `src/main/resources/Server/Item/Items/MOTM_Spellbook_Focus.json`
```

### Current known local-only changes

These are **not guaranteed committed/pushed** at the time of this handoff:

1. `PlayerConnectEvent` hook added in [MenteesMod.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/MenteesMod.java)
   - Goal: restore saved class/style/runtime earlier than `PlayerReadyEvent`.
2. `MOTM_Spellbook_Focus.json` switched to inline interaction objects
   - File: [MOTM_Spellbook_Focus.json](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/resources/Server/Item/Items/MOTM_Spellbook_Focus.json)
   - Goal: make the custom spellbook emit real interactions instead of acting dead.

---

## 2. What Is Actually Proven Right Now

```text
Proven
├ mod builds and installs                  ▶ yes
├ player save file persists class/style    ▶ yes
├ many worker-thread command asserts fixed ▶ yes
├ book no longer acts like a placeable block
│  when using the custom MOTM shell        ▶ yes
├ random damage while idling on join       ▶ improved in latest tests
└ internal/public build split exists       ▶ yes

Not proven
├ saved loadout restores instantly on join ▶ no
├ spellbook controls cast reliably         ▶ no
├ custom HUD is fully stable/settled       ▶ no
└ style swap / join runtime is “done”      ▶ no
```

### Hard evidence

The persisted player save already contains the expected loadout:

- [6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d.json](C:/Users/fishe/AppData/Roaming/Hytale/UserData/Saves/MOTM%20Creative%20Test/mods/com.motm_Mentees%20of%20the%20Mystical/saves/players/6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d.json)

It clearly shows:

```json
"class": "terra",
"selected_styles": ["quake"]
```

So when the user reports “it took 30 seconds to give me Quake back,” the bottleneck is **not persistence**. It is **runtime restoration timing**.

---

## 3. Issue Ledger

## 3.1 Join Restore Is Too Slow / Not Immediate

```text
Symptom
join world
   ▼
saved class/style exists on disk
   ▼
runtime/hud/spellbook restore is delayed
   ▼
user waits ~10–30 seconds before loadout feels present
```

### What was observed

- User repeatedly reported:
  - saved style did not feel available right away on join
  - in the latest report, `quake` returned after roughly 30 seconds
- Server-side save data already had the correct persisted values
- This suggests runtime restore is late, not save/load itself

### What was tried

1. Immediate restore in `onPlayerReady(...)`
   - File: [MenteesMod.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/MenteesMod.java)
   - Added:
     - `rebuildPlayerRuntimeNow(playerData);`
     - `ensureSpellbookItem(runtimePlayer);`
   - Result:
     - helped conceptually
     - did **not** fully eliminate delayed restore in practice

2. Immediate spellbook grant attempt before queue fallback
   - Files:
     - [MenteesMod.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/MenteesMod.java)
     - [MotmCommand.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/command/MotmCommand.java)
   - Result:
     - reduced some “queued forever” behavior
     - still not consistently immediate on rejoin

3. Reduced HUD install delay
   - `HUD_INSTALL_DELAY_TICKS` reduced to `4`
   - Result:
     - HUD installs faster once ready
     - does **not** solve the deeper restore-lifecycle issue

4. Latest local-only change: move restore earlier to `PlayerConnectEvent`
   - File: [MenteesMod.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/MenteesMod.java)
   - Current intent:
     - `PlayerConnectEvent` restores runtime/loadout
     - `PlayerReadyEvent` only handles HUD attach
   - Result:
     - **not yet verified live** at the time of this handoff

### Current assessment

```text
Likely true
├ save file is fine
├ PlayerReadyEvent alone is too late / too fragile
└ runtime restore should happen earlier

Still unknown
├ whether PlayerConnectEvent is the correct final lifecycle hook
└ whether some inventory/world readiness step still delays the spellbook
```

---

## 3.2 Spellbook Item Has Been the Biggest Runtime Failure Point

```text
Spellbook shells tried
├ native recipe book       ▶ acted like a block / placeable
├ native weapon spellbook  ▶ movement slowdown + native HUD baggage
└ custom MOTM spellbook    ▶ best current direction, but casts still unreliable
```

### What was observed

- The book has alternated between:
  - acting like a placeable block
  - acting like a weapon/tool and slowing movement
  - being in hand but not casting abilities
- User specifically reported:
  - accidental placement behavior
  - movement slowdown when equipping the book
  - abilities still not firing even when the book is in hand

### What was tried

1. Native recipe book shell
   - Result:
     - bad
     - placeable / block-like behavior

2. Native weapon spellbook shell
   - Result:
     - bad
     - movement slowdown
     - native weapon HUD confusion

3. Custom MOTM spellbook shell
   - File: [MOTM_Spellbook_Focus.json](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/resources/Server/Item/Items/MOTM_Spellbook_Focus.json)
   - Result:
     - good in one important way: no longer behaves like a block
     - still does **not** reliably cast

4. Legacy spellbook normalization
   - Code converts old/legacy spellbook items to the new MOTM spellbook shell
   - File: [MenteesMod.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/MenteesMod.java)
   - Result:
     - necessary cleanup
     - does not solve the live input issue

5. Switched spellbook interaction definition to inline `RunRootInteraction`
   - File: [MOTM_Spellbook_Focus.json](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/resources/Server/Item/Items/MOTM_Spellbook_Focus.json)
   - Motivation:
     - a working external mod used inline interaction objects rather than bare strings
   - Result:
     - **not yet proven to fix the cast path**

### Current assessment

```text
Avoid
├ going back to native recipe book
└ going back to weapon-shell spellbook

Best current direction
└ custom MOTM spellbook shell

But
└ the book being in hand still does not prove the input events are reaching MOTM
```

---

## 3.3 Spellbook Controls Still Do Not Reliably Cast

```text
Expected
style selected
   ▼
spellbook equipped
   ▼
Left Click / Right Click / Use
   ▼
slot 1 / slot 2 / slot 3

Actual
style selected
   ▼
spellbook equipped
   ▼
often no cast
```

### What was observed

- User repeatedly reported:
  - spellbook visible
  - style active
  - abilities UI present
  - but `Left Click`, `Right Click`, and `Use` did not fire spells

### What was tried

1. Added `PlayerInteractEvent` and `PlayerMouseButtonEvent` handlers
   - File: [MenteesMod.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/MenteesMod.java)
   - Result:
     - plumbing exists
     - not enough by itself

2. Added spellbook slot routing
   - `Left Click` / `Right Click` / `Use`
   - plus `Ability 1/2/3` as alternate bindings
   - Result:
     - mapping exists in code
     - not proven to fire in-world

3. Added world-thread cast queue
   - Earlier `/motm cast ...` path was failing with world-thread asserts
   - Fixed by queueing actual runtime execution to the world tick
   - Result:
     - command path got healthier
     - does **not** prove spellbook input works

4. Added extensive input tracing logs
   - logs for:
     - interact action
     - mouse button
     - held item id
     - spellbook recognition
     - resolved slot
   - Result:
     - important finding: recent logs still did **not** show spellbook input traces
     - meaning the problem may be **before** the cast handler

### Important conclusion

This is not yet a “cooldowns wrong” problem. It is more basic:

```text
unknown break point
spellbook equipped
   ▼
Hytale item interaction
   ▼
PlayerInteractEvent / PlayerMouseButtonEvent?
   ▼
MOTM spellbook recognition?
   ▼
slot cast queue?
   ▼
runtime playback?
```

The strongest current hypothesis is:

```text
The custom spellbook item is still not producing the exact live interaction events
we expect, or not in the form MOTM is currently listening for.
```

---

## 3.4 Worker-Thread vs World-Thread Bugs Were Real and Cost a Lot of Time

```text
Bad pattern
command worker thread
   ▼
touch world/runtime/player store directly
   ▼
asserts / silent failures

Better pattern
command worker thread
   ▼
queue request
   ▼
world tick executes live action
```

### What was broken earlier

- `/motm spellbook give`
- `/motm dev class clear`
- `/motm dev styles clear`
- `/motm cast ...`

All of these had variants that touched live player/world state from the wrong thread.

### What was tried / fixed

1. Queue spellbook grants by player id
2. Queue ability casts
3. Queue runtime rebuilds
4. Fail safe with message instead of exploding the command bridge

### Result

```text
This area is much better than before.
Not perfect, but no longer the main blocker.
```

This matters because Claude should **not** waste time re-solving the same command-thread crashes from scratch. A lot of those were already found and partially cleaned up.

---

## 3.5 Random Damage / Self-Damage Was Not One Single Bug

```text
Observed damage sources
├ test arena mobs hitting the player
├ weak “free-cast protection” that healed but did not truly protect
└ HP modifier refresh clamping current HP down
```

### What was observed

- User sometimes took “constant damage”:
  - on join
  - after style swap
  - even when it did not feel like enemies were directly causing it

### What was tried

1. Free-cast safety that topped health off every tick
   - Result:
     - insufficient
     - could still look/feel like constant damage

2. Native `Invulnerable` component during free-cast testing
   - Result:
     - helped, but not the full story

3. Clear lingering `BURN` / `DOT`
   - Result:
     - useful cleanup
     - not enough alone

4. Fixed health modifier refresh logic
   - File: [MenteesMod.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/MenteesMod.java)
   - Root cause:
     - class HP modifier was being removed/reapplied too often
     - current HP could clamp downward
   - Result:
     - very important improvement
     - this likely explains the “I stood still and took damage for no reason” reports better than Quake self-damaging

### Current assessment

```text
This area is partially stabilized.
Recent user report said idle join damage stopped.
Historically style swap could still reintroduce it.
```

This is much closer to “understood” than the spellbook cast problem.

---

## 3.6 HUD / Custom UI Has Been a Separate Source of Breakage

```text
HUD goals
├ no native junk overlay collisions
├ readable status text
├ resource bar above energy bar
└ spell UI in sane place

Reality
├ custom HUD path/casing issues
├ asset-pack manifest issues
├ disconnects: “Failed to apply CustomUI HUD commands”
└ lots of placement/readability iteration
```

### What was tried

1. Multiple HUD document path fixes
2. Asset pack / manifest changes
3. Delayed HUD install after join
4. Hiding native HUD components
5. Moving/reshaping custom overlay

### What definitely happened

- At one point the world was disconnecting with:
  - `Failed to apply CustomUI HUD commands`
- That was addressed enough to get back into the world again

### Current assessment

```text
HUD is not the main blocker now.
Spellbook casting and join restoration are higher priority.
```

Claude should not spend the first plan step on HUD cosmetics. The runtime and input path are still more fundamental.

---

## 3.7 Automation Was Useful for Regression, But Not for Feel Validation

### What happened

- We built dev automation / style test runner commands
- The user disliked the fully automated style-cast flow for judging feel
- Manual one-style-at-a-time testing is preferred

### Important product decision

```text
Testing mode
├ internal tester build  ▶ okay to keep dev helpers
└ public build           ▶ do not expose them
```

This split already exists conceptually and in build output. It should stay.

---

## 4. Things Already Tried That Claude Should Not Blindly Repeat

```text
Do not blindly retry
├ using native recipe books as spellbooks
├ using weapon-shell spellbooks as the final answer
├ relying on PlayerReadyEvent alone for saved loadout restore
├ using `/motm cast` as proof that real gameplay is solved
├ treating HUD cleanup as the main blocker
└ assuming “free-cast heals every tick” equals true test safety
```

---

## 5. Things That Look Like the Real Remaining Core Problems

```text
Priority stack
╔ 1. loadout restore lifecycle ══════════════════════╗
║ when should saved class/style/runtime be restored? ║
║ connect? ready? add-to-world?                      ║
╠ 2. spellbook interaction contract ════════════════╣
║ what exact item interaction definition is needed   ║
║ for Hytale to emit usable cast input events?       ║
╠ 3. cast observability ═════════════════════════════╣
║ can we prove where the cast path dies?             ║
╚ 4. only then: HUD finalization ════════════════════╝
```

### Strong recommendation

Do **not** continue broad feature work until the following tiny vertical slice is stable:

```text
Vertical slice target
join world
  ▼
saved style restored immediately
  ▼
spellbook in inventory immediately
  ▼
equip spellbook
  ▼
Left Click / Right Click / Use
  ▼
3 visible Quake abilities fire
```

If that slice is not solid, everything else will keep feeling broken no matter how many ability-specific tweaks are made.

---

## 6. Files Claude Should Read First

### Core runtime
- [MenteesMod.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/MenteesMod.java)
- [MotmCommand.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/command/MotmCommand.java)
- [GameplayPlaybackManager.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/manager/GameplayPlaybackManager.java)

### Player persistence
- [PlayerDataManager.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/manager/PlayerDataManager.java)
- [PlayerData.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/model/PlayerData.java)

### Spellbook shell
- [MOTM_Spellbook_Focus.json](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/resources/Server/Item/Items/MOTM_Spellbook_Focus.json)

### HUD
- [MotmStatusHud.java](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/java/com/motm/ui/MotmStatusHud.java)
- [MOTM_StatusHud.ui](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/resources/Common/UI/Custom/HUD/MOTM_StatusHud.ui)
- [manifest.json](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/src/main/resources/manifest.json)

### Existing correction material
- [CODEX_CORRECTIONS_PLAN.md](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/CODEX_CORRECTIONS_PLAN.md)
- [ABILITY_COMPLETION_CHECKLIST.md](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/ABILITY_COMPLETION_CHECKLIST.md)

### Live test evidence
- [2026-03-30_00-41-50_server.log](C:/Users/fishe/AppData/Roaming/Hytale/UserData/Saves/MOTM%20Creative%20Test/logs/2026-03-30_00-41-50_server.log)
- [6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d.json](C:/Users/fishe/AppData/Roaming/Hytale/UserData/Saves/MOTM%20Creative%20Test/mods/com.motm_Mentees%20of%20the%20Mystical/saves/players/6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d.json)

---

## 7. What I Would Ask Claude To Deliver

```text
Needed from Claude
├ a concrete implementation plan
├ ordered by dependency / risk
├ explicitly avoiding dead-end retries
├ centered on one reliable vertical slice first
└ only then broadening back out to all classes/styles
```

Specifically:

1. Identify the correct lifecycle event chain for:
   - saved loadout restore
   - spellbook restore
   - HUD install

2. Identify the correct Hytale item interaction contract for a custom spellbook item
   - ideally with a minimal repro path

3. Recommend whether to:
   - keep custom spellbook item
   - piggyback a native item differently
   - or create a thinner input bridge

4. Provide a staged implementation plan that gets:
   - `terra / quake`
   - immediate join restore
   - live spellbook casting
   - no random damage
   - before touching broader content polish

---

## 8. Bottom-Line Summary

```text
Big picture
╔══════════════════════════════════════════════════════╗
║ This mod is not failing because the design is vague.║
║ It is failing because the live runtime contract     ║
║ with Hytale is still unstable in a few key places.  ║
║                                                      ║
║ We need less “tweak another ability,” and more      ║
║ “lock one vertical slice until it is undeniably     ║
║ solid.”                                             ║
╚══════════════════════════════════════════════════════╝
```

