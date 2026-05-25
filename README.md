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
- `docs/FRIEND_REVIEW_GUIDE.md` - orientation guide for outside reviewers and review tools
- `docs/ABILITY_REFERENCE.md` - GitHub-facing catalog of all 40 styles and 120 active abilities
- `docs/hytale-capability-atlas/` - Hytale capability research, primitive choices, and implementation gates
- `PLAN.md` - original implementation plan used to start the project
- `scripts/build-install.ps1` - local build and install helper
- `scripts/audit-no-resource.ps1` - verifies the no-resource casting model across all classes/styles/abilities

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
- custom spellbook and development review surfaces
- Phase 7 perk runtime layer for stat modifiers and on-kill healing
- Phase 9 live validation for Terra/Metal, Hydro/Icicle, Aero/Wind Blade, Corruptus/Flame, Terra/Magma, and Hydro/Snow
- no-resource active ability model: all 40 styles and 120 abilities cast through cooldowns, durations, charges, action timing, positioning, and item conditions instead of class resource spending

## Ability Reference

The current class/style/ability catalog is documented in [`docs/ABILITY_REFERENCE.md`](docs/ABILITY_REFERENCE.md).

For the intended ability fantasy, exact recovered function details, visual read,
and review priorities, start with [`docs/FRIEND_REVIEW_GUIDE.md`](docs/FRIEND_REVIEW_GUIDE.md), then read
[`CODEX_CLASS_STYLE_ABILITY_REVIEW_MOCKUP_2026-05-22.md`](CODEX_CLASS_STYLE_ABILITY_REVIEW_MOCKUP_2026-05-22.md) and
[`CODEX_CONCEPT_REVIEW_DECISIONS_2026-05-22.md`](CODEX_CONCEPT_REVIEW_DECISIONS_2026-05-22.md).

Current shape:

```text
MOTM
+-- 4 classes
+-- 40 styles
+-- 120 active abilities
+-- 0 active ability resource costs
```

Use this check before claiming ability data and docs are still aligned:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/audit-no-resource.ps1
```

Still in progress:

- deeper perk effect integration for damage triggers, transformations, auras, and advanced conditions
- broader in-game validation for the remaining 33 styles
- public release polish
