# Community Knowledge & Ecosystem (July 2026)

Primary sources: hytalemodding.dev (community hub; full docs dump preserved locally at
`C:/tmp/hytale-llms-full.txt`, 45,198 lines, fetched 2026-07-16), hytale-docs.com
(community; includes decompilation-derived API pages), official
hytale.com/news/2025/11/hytale-modding-strategy-and-status. Neither docs site is
official; the local jar/Assets.zip outrank them on conflicts.

## Ecosystem facts

- Server-first: Java plugins + streamed asset packs; **the client cannot be modded**
  (community FAQ). Assets cleared on disconnect. No shaders/post-processing.
- Distribution: CurseForge is the main hub; **Modtale** is a community host that
  inspects plugin `manifest.json` on upload. Asset-only packs upload as zips.
- Open-source ecosystem is thin: most published mods ship compiled-only with no
  license. Treat GitHub repos (HytaleModding/site, marggx/ModelCreator) as the
  exceptions; check LICENSE per repo before lifting code.

## Dev workflow (community-documented)

- Env: JDK 25 + Gradle; guides at hytalemodding.dev/en/docs/guides/plugin/
  setting-up-env and build-and-test. IntelliJ plugin: project setup, API completion,
  server management.
- Models: Blockbench + Hytale plugin (replaces the old "Hytale Model Maker");
  ModelCreator (github.com/marggx/ModelCreator) for .blockymodel export.
- In-game Asset Editor edits data assets live; NPC editor keeps the state-tree
  paradigm (dev Q&A) — biggest admitted pain point is no live debugging.
- Manifest deps control load order: add `Hytale:EntityModule` / `Hytale:BlockModule`
  etc. to `Dependencies` when touching their systems.
- Translations: `Server/Languages/en-US/*.lang` (`key.name = Value`).
- UI: reference vanilla macros via relative traversal to `Common/UI/Custom/Common.ui`
  when your .ui lives in a subfolder.

## Documented gotchas (community's hard-won list)

1. Client is unmoddable; everything ships from the server.
2. One custom HUD slot per client — coexist via MultipleHUD or visibility gating.
3. Unknown particle SystemIds: silent no-op, can crash asset validation.
4. EntityEffect OverlapBehavior rejects `Replace` (use Overwrite/Extend/Ignore).
5. Custom ability bars fighting the vanilla hotbar cause ghost items — override
   interactions on native slots instead of packet games.
6. Asset/UI paths are case-sensitive in current builds (MOTM verified: client
   hard-disconnects on wrong-case UI document paths).
7. Heavy networked particle spam has real perf cost — short bursts, caps, culling.
8. ECS mutation during scans is unsafe — deferred queues/command buffers.
9. Early Access API churn is expected — keep logic thin in Java, heavy in JSON;
   wrap internal surfaces behind your own compatibility layer.
10. `_Test/` assets work today but are prototype-tier; re-verify each patch.

## Roadmap signals (affect knowledge durability)

- **NoesisGUI (XAML) is the new UI system** — already shipped for machinima; the
  legacy custom UI "will eventually be retired" (dev Q&A). Expect a HUD/page rewrite
  eventually; keep UI code isolated. (The 2 `.xaml` files in vanilla `Common/UI` are
  the first sign.)
- Server **source access** is planned per the official modding strategy post —
  decompilation-based knowledge may become legitimate reference.
- Blockbench integration, machinima tools, and creative-tools reveals are on the
  public roadmap; NPC editor migrates into the Asset Editor.
- Update 6+ mentioned as the NoesisGUI migration window in community notes.

## Starter checklist for a brand-new Hytale mod

1. `manifest.json` (Group/Name/Version/Main/ServerVersion range/IncludesAssetPack) at
   jar root; Gradle processResources expands placeholders.
2. `JavaPlugin` subclass: data in `setup()`, hooks in `start()` — never in ctor.
3. Asset pack trees: `Common/` (models/textures/UI/anims) + `Server/` (roles, effects,
   particles, interactions, items, sounds, languages) — prefix EVERY asset id.
4. Events via `getEventRegistry()`; state via codec ECS components; config via
   `withConfig`; commands via `getCommandRegistry()`.
5. Visuals: compose vanilla first (see asset-catalog.md), custom assets only per the
   decision rule in `docs/modding-research-2026-07-16.md` §4.
6. Prove each novel capability with an isolated command/proof before wiring it wide.
7. Test with multiple HUD mods installed early; declare optional integrations via
   OptionalDependencies + reflective adapters.
