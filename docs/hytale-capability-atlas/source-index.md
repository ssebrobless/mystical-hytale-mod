# Source Index

Updated: 2026-05-24

## Source Classes

```
╔══════════════════════╦══════════════════════════════════════════════╗
║ Source Class          ║ How To Use It                               ║
╠══════════════════════╬══════════════════════════════════════════════╣
║ official-current      ║ Trust for direction and stated support       ║
║ official-api          ║ Trust names, still compare to local jar       ║
║ official-directional  ║ Useful design intent, may lag Early Access   ║
║ community-reference   ║ Useful examples, must verify locally          ║
║ community-experience  ║ Good warning signal, never sole source        ║
║ local-authoritative   ║ Actual installed assets/classes/signatures    ║
║ runtime-proven        ║ Final proof for MOTM gameplay acceptance      ║
╚══════════════════════╩══════════════════════════════════════════════╝
```

## Public Sources Checked

| Source | Class | Useful Findings |
| --- | --- | --- |
| https://hytale.com/news/2025/11/hytale-modding-strategy-and-status | official-directional | Hytale modding is server-side first. Major content surfaces are Java plugins, data assets, art assets, save files, worlds, and prefabs. The team explicitly says docs/tools are uneven and server source/decompilation is part of the short-term path. |
| https://hytale.com/news/2026/4/hytale-pre-release-patch-notes-update-5 | official-current | Modding tools and runtime are changing quickly. Relevant notes include multiple custom HUD layers, command autocomplete, entity tool changes, projectile crash fixes, and NPC movement crash fixes. |
| https://hytalemodding.dev/en/docs/official-documentation/worldgen/pack-tutorial/asset-packs | official-directional mirror | Asset packs override/add base-game assets and are stored in `UserData/Mods` or world-local mods. Useful for future custom effects/assets, but MOTM currently keeps `IncludesAssetPack=false` unless explicitly revisited. |
| https://hytalemodding.dev/en/docs/official-documentation/custom-ui | official-directional mirror | Server-controlled custom UI is possible through Java code plus `.ui` markup assets. Client UI such as main menu/inventory is not moddable. |
| https://release.server.docs.hytale.com/com/hypixel/hytale/server/core/modules/entity/damage/DamageSystems | official-api | Damage should be queued through `DamageSystems.executeDamage(...)` in the correct damage phase; damage has gather/filter/application/inspect phases. |
| https://release.server.docs.hytale.com/com/hypixel/hytale/server/core/modules/entity/component/package-summary | official-api | Confirms useful ECS components: `TransformComponent`, `ModelComponent`, `EntityScaleComponent`, `BoundingBox`, `Intangible`, `Invulnerable`, `PersistentModel`, `PositionDataComponent`, `DynamicLight`. |
| https://hytalemodding.dev/en/docs/guides/plugin/browsing-serverjar | community-reference | Recommends decompiling/browsing `HytaleServer.jar` because class-by-class docs are not enough for real plugin work. This validates our local jar-first workflow. |
| https://doctale.dev/plugin-development/blocks/block-types/ | community-reference | Block definitions expose visual, physics, behavior, tint, light, particles, sound, support, and movement settings. Useful for deciding when a real block/asset pack route is better than particles. |
| https://doctale.dev/plugin-development/entities/overview/ | community-reference | Explains the ECS shape: entities are components, `PlayerRef` bridges network/player state, and entity components are accessed through world/entity stores. |
| https://hytale-docs.pages.dev/modding/systems/entity-effects/ | community-reference | Entity effects can apply durations, overlap behavior, stat modifiers, application effects, model changes, tint, particles, and removal behavior. Verify every concrete field against local jar/assets. |
| https://hytale-docs.pages.dev/modding/systems/projectiles/ | community-reference | Documents the intended `ProjectileModule.spawnProjectile(...)` route. Useful for fixing Magma Sling/Cactus/Shards away from living-mob proxies, but signatures must be checked locally. |
| https://hytale-docs.pages.dev/modding/ecs/components/ | community-reference | Reinforces component/ticking-system/command-buffer concepts. Mutating world/entities is safer through tick/system-compatible paths. |
| https://hytale-docs.dev/ | community-reference | Generated API documentation from an older server jar. Useful as search index only; local jar wins if signatures differ. |
| https://hytalecharts.com/server-api | community-reference | Another API index. Useful for package discovery, not final authority. |
| https://hytalemodding.dev/de-DE/docs/guides/plugin/ui | community-reference | Practical custom UI guide with examples. Notes that `.ui` files currently remain the practical route, that UI resources need asset-pack inclusion, and that Diagnostic Mode can improve UI errors. |
| https://www.curseforge.com/hytale/mods/vuetale | community-experience | Shows community is building higher-level UI bridges. Useful as proof of direction, but adding new third-party UI dependencies should be a separate explicit decision. |
| https://www.curseforge.com/hytale/mods/htdevlib | community-experience | General utility library for plugin development. Useful for ideas, not a silent dependency. |
| https://www.reddit.com/r/HytaleModding/comments/1qkt5cg/custom_projectile_removes_player_entity/ | community-experience | A warning that projectile work can go wrong in entity ownership/removal paths. Treat projectile implementation as a proof gate, not a simple visual swap. |

## Local Sources Checked

| Local Source | Class | Use |
| --- | --- | --- |
| `%APPDATA%/Hytale/install/release/package/game/latest/Assets.zip` | local-authoritative | Actual asset ids, particles, models, prefabs, UI, block/item JSON. |
| `%APPDATA%/Hytale/install/release/package/game/latest/Server/HytaleServer.jar` | local-authoritative | Actual Java classes and method signatures for this installed build. |
| `audits/hytale-asset-library/2026-05-24-capability-atlas/` | local-authoritative | Full local asset/API indexes generated from current installed package. |
| `audits/hytale-runtime-capabilities/2026-05-24-capability-atlas/` | local-authoritative | Focused API signatures for P0-P4 proof buckets. |
| `audits/harness/assets/2026-05-24-capability-atlas/` | local-authoritative | Resolver asset validation and keyword catalogs. |
| `src/main/java/com/motm/util/HytaleAssetResolver.java` | repo truth | MOTM's current allowed particle/model/role routing. |
| `src/main/java/com/motm/manager/GameplayPlaybackManager.java` | repo truth | MOTM's actual ability runtime/playback implementation. |
| `src/main/resources/Server/Entity/Effects/MOTM/*.json` | repo truth | Current custom EntityEffect assets. |

## Research Rule

No new ability implementation should depend on a public-doc claim alone.

```
Public docs say "possible"
  └─▶ find asset/class locally
       └─▶ prove in a tiny command or isolated ability sandbox
            └─▶ then use in real style runtime
```
