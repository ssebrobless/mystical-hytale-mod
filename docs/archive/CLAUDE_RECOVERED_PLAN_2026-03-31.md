# Claude Recovered Plan - 2026-03-31

> Source:
> recovered from the interrupted local Claude session and copied into the repo
> for execution continuity.
>
> Original local files:
> - `C:\Users\fishe\.claude\projects\C--Users-fishe-Documents-projects\d2ce24c2-f2ff-42c5-9d86-804aecf0cce1.jsonl`
> - `C:\Users\fishe\.claude\plans\zazzy-giggling-bonbon.md`

---

# Plan: MOTM Vertical Slice Stabilization

## Context

The Mentees of the Mystical Hytale mod builds and loads, but has three interconnected runtime failures that block gameplay:
1. Spellbook casting does not work - clicking with the spellbook in hand produces no ability casts
2. Join restore is delayed - saved class/style/loadout takes 10-30 seconds to feel present
3. Random damage occasionally occurs - on join or after style swap (partially stabilized)

The handoff ([CLAUDE_HANDOFF_2026-03-30.md](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/CLAUDE_HANDOFF_2026-03-30.md)) and corrections plan ([CODEX_CORRECTIONS_PLAN.md](C:/Users/fishe/Documents/projects/Mystical-Hytale-Mod/CODEX_CORRECTIONS_PLAN.md)) are the source of truth. The mod is blocked by runtime integration reliability, not content volume.

### Dead ends - DO NOT retry
- Native recipe books as spellbooks
- Weapon-shell spellbooks
- `PlayerReadyEvent` alone for restore
- `/motm cast` as proof that spellbook input works
- HUD cosmetics before runtime/input reliability
- "Free-cast heals every tick" as true test safety

---

## Confirmed Facts

| # | Fact | Evidence |
|---|------|----------|
| F1 | Mod loads as plugin | Log line 117, plugin manager |
| F2 | `start()` runs, hooks register | Log line 1028: "Plugin enabled successfully!" - `start()` calls `registerHytaleHooks()` then logs this |
| F3 | Data loading works (4 classes, 40 styles, 120 abilities, 800 perks) | Log lines 252-276 |
| F4 | Save file correctly persists `class: terra`, `selected_styles: [quake]` | Player JSON on disk |
| F5 | JAR has valid manifest and contains `Server/Item/Items/MOTM_Spellbook_Focus.json` | JAR inspection confirms template expansion and item file presence |
| F6 | Zero MOTM log entries after "Plugin enabled successfully!" for the entire session | Full log grep - no connect, ready, interact, or cast traces |
| F7 | The save-level `mods/com.motm_Mentees of the Mystical/` directory (used for save data) has no manifest.json - this causes the benign AssetModule warning on log line 150 | `ls` of that directory shows only `saves/`, properties, preflight report |
| F8 | Manifest format matches working mods (GoneFishing, WansWonderWeapon, etc.) | Compared JAR manifests - identical structure |
| F9 | `initializeRuntimePlayer()` called from BOTH `PlayerConnectEvent` AND `PlayerReadyEvent` | MenteesMod.java lines 403-409 - double initialization |
| F10 | Join handlers have NO entry-point logging - the lack of log output does NOT prove handlers aren't firing | `onPlayerConnect/Ready/initializeRuntimePlayer/rebuildPlayerRuntimeNow` all lack entry LOG statements |
| F11 | Interaction handlers DO have conditional logging (dev tools trace + spellbook trace) that SHOULD fire | Lines 2237-2250 and 2428-2444 - conditions `hasSelectedStyle && isDevToolsEnabled()` should be true |
| F12 | Dev tools are enabled | Log line 252: "Dev tools enabled via motm-server.properties" |
| F13 | `CODEX_CORRECTIONS_PLAN.md` Phase 1.1 says set `IncludesAssetPack=false` because "AssetModule rejects the pack structure" | But current source still has `true`, and the manifest format looks valid vs other mods |
| F14 | Worker-thread/world-thread queuing is in place | ConcurrentHashMap, ConcurrentLinkedQueue, synchronized GameplayPlaybackManager |
| F15 | GoneFishing mod (working reference) uses both bare string interactions (`"Primary": "Block_Primary"`) and inline typed interactions (`{ "Type": "GoneFishing_Spawn_Fish" }`) | JAR inspection |

## Strongest Hypotheses

| # | Hypothesis | Supporting evidence | How to verify |
|---|-----------|-------------------|---------------|
| H1 | Asset pack loads from JAR but `RunRootInteraction` does not produce `PlayerInteractEvent` - the root interaction chains execute natively without firing mod-hookable events | F11 says interaction traces should fire but F6 shows they never do. GoneFishing uses custom interaction types, not `RunRootInteraction`. | Add FIRST-LINE logging to `handlePlayerInteract` before any early returns, click spellbook, check log |
| H2 | Asset pack does NOT load from JAR - item has no registered interactions, Hytale ignores clicks on it entirely | No "Loaded pack:" log for MOTM. Corrections plan says to set `IncludesAssetPack=false`. | Try to resolve the item definition in `start()`, log result |
| H3 | `PlayerMouseButtonEvent` fires but `PlayerInteractEvent` does not - mouse events are raw input, interact events require valid item interactions | Mouse button handler also has conditional logging that does not appear. But maybe `resolveMouseButtonItemId()` returns null for the custom item | Add FIRST-LINE logging to mouse handler too |
| H4 | Double initialization (F9) causes race conditions - connect handler partially sets up state, ready handler runs the same code again | Both call `initializeRuntimePlayer()` with overlapping logic | Split responsibilities, add entry logging |

## Implementation Roadmap

### Stage 0: Diagnostic Instrumentation (MUST DO FIRST)
- Add first-line lifecycle logging in `start()`, `onPlayerConnect`, `onPlayerReady`, `initializeRuntimePlayer`, and `onPlayerJoin`
- Add first-line logging in `handlePlayerInteract`, `handlePlayerMouseButton`, and `handleDamageBlock`
- Dump `InteractionType.values()` in `start()`
- Log spellbook asset resolution and spellbook grant results

### Stage 1: Fix Spellbook Interaction Contract
- If interactions do not fire, simplify the custom item back to bare string interactions first
- If mouse fires but interact does not, make mouse the primary cast path
- If cast attempts log but do not execute, trace queue/debounce/store gating

### Stage 2: Fix Join Restore Lifecycle
- `onPlayerConnect`: register runtime player and load save/runtime state, but avoid inventory work
- `onPlayerReady`: rebuild runtime, ensure/queue spellbook, refresh bonuses, install HUD

### Stage 3: End-to-End Vertical Slice
- Join world
- Saved Terra/Quake restored within 2 seconds
- Spellbook present within 2 seconds
- Left Click / Right Click / Use cast the 3 Quake abilities
- No random damage while testing

### Stage 4: Damage Safety
- Only revisit if the vertical slice still shows unexplained damage
