# Mentees of the Mystical - Project Rules

## Data Protection
- `src/main/resources/data/styles/*.json` are PROTECTED. These contain carefully authored ability profiles. NEVER regenerate these files wholesale. Only make surgical, targeted edits to specific fields.
- If you need to modify ability data, change only the specific field(s) requested. Do not reformat, reorder, or rewrite surrounding content.
- 3 "Restore" commits in git history show ability data was previously lost and had to be manually recovered.

## Design Principles
- This is a PURE RPG OVERLAY mod. It adds class/style/ability identity, perk progression, and mob scaling on top of Hytale's native systems.
- NEVER create custom biomes, weapons, armor, or economy systems - Hytale provides all of those natively.
- Styles = the ONLY source of active abilities (3 per style)
- Perks = ALWAYS passive bonuses (never active abilities)
- Races = passive identity bonuses

## Plugin Lifecycle
- `setup()` = data loading (JSON -> model objects). Runs before hooks are available.
- `start()` = hook registration (Hytale event listeners). Runs after the server is ready.
- NEVER register hooks in the constructor or in `setup()`.

## Build
- Build: `powershell -ExecutionPolicy Bypass -File scripts/build-install.ps1`
- JDK 25 + Gradle 9.1 (auto-downloaded to `.tools/`)
- Internal build installs to `%APPDATA%/Hytale/UserData/Mods/`

## Known Issues
- `MenteesMod.java` and `GameplayPlaybackManager.java` are god classes. Extraction direction is documented in `CODEX_CORRECTIONS_PLAN.md`.
- Custom HUD documents require `IncludesAssetPack=true`, and HUD install should be deferred briefly after join so the client can resolve the UI safely.
