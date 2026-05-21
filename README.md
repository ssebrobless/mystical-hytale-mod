# Mystical Hytale Mod

`Mentees of the Mystical` is a Hytale mod prototype centered around:

- 4 elemental classes
- 40 combat styles
- 12 races
- 800 perks
- level progression to 200
- dynamic mob scaling
- elemental reactions and runtime combat systems

## Project Layout

- `src/main/java/com/motm` - mod source
- `src/main/resources/data` - gameplay JSON data
- `PLAN.md` - original implementation plan used to start the project
- `scripts/build-install.ps1` - local build and install helper

## Build And Install

The project is set up to work with an installed Hytale client on Windows.

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-install.ps1
```

That script will:

- download a portable Gradle distribution into `.tools/`
- download a portable JDK 25 into `.tools/`
- build the mod jar
- install the jar into `%APPDATA%/Hytale/UserData/Mods`

## Current Status

Implemented so far:

- Hytale Java plugin bootstrap and manifest wiring
- local build/install flow against the installed Hytale server jar
- command bridge for `/motm`
- data loading for classes, perks, styles, races, leveling, mobs, and elite titles
- race manager runtime bonuses
- mob stat loading and elite title support
- plugin lifecycle alignment with Hytale's `setup/start/shutdown`
- real server tick registration
- real mob spawn/death event hooks
- Terra/Quake vertical slice validated across two cold launches
- custom spellbook UI page with seven working navigation tabs
- Phase 7 perk runtime layer for stat modifiers and on-kill healing
- Phase 9 live validation for Terra/Metal, Hydro/Icicle, Aero/Wind Blade, and Corruptus/Flame

Still in progress:

- deeper perk effect integration for damage triggers, transformations, auras, and advanced conditions
- broader in-game validation for the remaining 35 styles
- public release polish
