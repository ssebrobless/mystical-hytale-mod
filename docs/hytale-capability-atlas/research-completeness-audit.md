# Hytale Research Completeness Audit

Created: 2026-05-24

Purpose: define how we can get as close as possible to "we know everything viable for this mod" without pretending Hytale's early-access modding surface is fully documented or stable.

## Bottom Line

We cannot honestly guarantee that any agent has discovered literally every possible trick in Hytale. Hytale's own modding status post says the tools and documentation are incomplete, some client behavior is not exposed server-side, and modding maturity is uneven.

What we can do is build a complete coverage process:

```
╔══════════════════════════════════════════════════════════════════════╗
║ 1. Public Research                                                  ║
║ official posts, community docs, API mirrors, examples, changelogs    ║
╚══════════════════════════════════════════════════════════════════════╝
          │
          ▼
╔══════════════════════════════════════════════════════════════════════╗
║ 2. Local Static Inventory                                           ║
║ enumerate Assets.zip + HytaleServer.jar from this installed build    ║
╚══════════════════════════════════════════════════════════════════════╝
          │
          ▼
╔══════════════════════════════════════════════════════════════════════╗
║ 3. Local API Signature Probes                                       ║
║ javap/decompile exact classes before using APIs                      ║
╚══════════════════════════════════════════════════════════════════════╝
          │
          ▼
╔══════════════════════════════════════════════════════════════════════╗
║ 4. Isolated Runtime Proofs                                          ║
║ prove each primitive in-game with cleanup and logs                   ║
╚══════════════════════════════════════════════════════════════════════╝
          │
          ▼
╔══════════════════════════════════════════════════════════════════════╗
║ 5. Ability Implementation                                           ║
║ only build abilities from proven primitives                          ║
╚══════════════════════════════════════════════════════════════════════╝
          │
          ▼
╔══════════════════════════════════════════════════════════════════════╗
║ 6. Evidence Review                                                  ║
║ JSONL logs + screenshots/video + user visual judgment                ║
╚══════════════════════════════════════════════════════════════════════╝
```

This is the practical standard: if a mechanic or visual has not passed steps 2-4, it is not ready for ability work.

## Fresh Research Sources Checked

### Official Hytale Sources

- Hytale Modding Strategy and Status: confirms server-side-first modding, Java plugins, data assets, art assets, prefabs/world files, incomplete docs, rough tool maturity, and missing client exposure.
- Pre-Release Update 5 notes: confirms active modding/API churn, custom HUD layer improvements, node editor changes, creative-tool changes, entity tool changes, projectile/NPC crash fixes, and asset-pack fixes.
- Hytale model-making post: confirms Blockbench workflow, cube/quad geometry constraints, texture density guidance, and dynamic tinting on player textures.
- New Worlds Modding Contest guidance: recommends using asset-driven NPC systems first and Java only for what data cannot do.

### Community / Unofficial Documentation

- Doctale asset docs: asset pack structure, all major asset categories, asset maps, validation, hot reload notes, model VFX, block texture/tint notes.
- Hytale docs pages: projectile system, entity effects, movement, interactions, UI, fluids, and server API pages.
- HytaleModding.dev server-jar browsing guide: recommends decompiling/browsing `HytaleServer.jar` for real answers while docs catch up.
- hytale-docs.dev API mirror: useful search/index source, generated from a server jar, but not guaranteed to match the user's exact local build.

### Local Sources Checked

- `%APPDATA%/Hytale/install/release/package/game/latest/Assets.zip`
- `%APPDATA%/Hytale/install/release/package/game/latest/Server/HytaleServer.jar`
- Existing MOTM source files and proof commands.
- Existing audit outputs under `audits/hytale-asset-library/`, `audits/hytale-runtime-capabilities/`, and `audits/harness/assets/`.

## Local Static Inventory

Installed asset archive:
- `Assets.zip`: 3,411,196,468 bytes.

High-volume asset areas found in the installed archive:

```
┌──────────────────────────────────────┬────────┐
│ Area                                 │ Count  │
├──────────────────────────────────────┼────────┤
│ Server/World/Default                 │ 12102  │
│ Common/Icons/ItemsGenerated          │ 4325   │
│ Server/Item/Items                    │ 3635   │
│ Common/Characters/Animations         │ 2290   │
│ Server/Prefabs/Rock_Formations       │ 1676   │
│ Common/NPC/Intelligent               │ 1654   │
│ Server/Item/Interactions             │ 1339   │
│ Server/Audio/SoundEvents             │ 1176   │
│ Server/NPC/Roles                     │ 952    │
│ Server/Item/RootInteractions         │ 610    │
│ Common/UI/Custom                     │ 561    │
│ Server/Particles/Combat              │ 468    │
│ Server/Particles/Block               │ 272    │
│ Server/Particles/Spell               │ 217    │
│ Server/Entity/Effects                │ 140    │
│ Server/ProjectileConfigs/Weapons     │ 103    │
│ Common/Sounds/Projectiles            │ 81     │
└──────────────────────────────────────┴────────┘
```

Local server asset-type packages found in `HytaleServer.jar`:

```
ambiencefx, attitude, audiocategory, blockbreakingdecal, blockhitbox,
blockparticle, blockset, blocksound, blocktick, blocktype, buildertool,
camera, entityeffect, environment, equalizereffect, fluid, fluidfx,
gamemode, gameplay, item, itemanimation, itemsound, model, modelvfx,
particle, portalworld, projectile, responsecurve, reverbeffect,
soundevent, soundset, tagpattern, trail, weather, wordlist
```

This expands our viable implementation palette. We should not only think in terms of "particles and mobs." Hytale exposes or packages assets for blocks, fluids, projectiles, trails, model VFX, block-breaking decals, cameras, weather, sound events, audio effects, item animations, root interactions, prefabs, NPC roles, and UI.

## Local API Coverage Areas

Major local API package clusters found in `HytaleServer.jar`:

```
┌────────────────────────────────────────────────────────────┬───────┐
│ Package cluster                                            │ Count │
├────────────────────────────────────────────────────────────┼───────┤
│ server/core/modules/entity                                 │ 341   │
│ server/core/universe/world                                 │ 304   │
│ server/core/asset/type                                     │ 289   │
│ server/core/modules/interaction                            │ 183   │
│ server/core/prefab/selection                               │ 52    │
│ server/core/event/events                                   │ 50    │
│ server/core/entity/entities                                │ 45    │
│ server/core/modules/collision                              │ 33    │
│ server/core/modules/entitystats                            │ 33    │
│ server/core/inventory/container                            │ 28    │
│ server/core/modules/physics                                │ 26    │
│ server/core/modules/block                                  │ 18    │
│ server/core/modules/projectile                             │ 18    │
│ server/core/ui/browser                                     │ 18    │
│ server/core/modules/entityui                               │ 15    │
│ server/core/modules/time                                   │ 15    │
│ server/core/modules/prefabspawner                          │ 10    │
│ server/core/modules/blockhealth                            │ 9     │
│ server/core/modules/item                                   │ 7     │
│ server/core/entity/movement                                │ 5     │
│ server/core/entity/knockback                               │ 5     │
└────────────────────────────────────────────────────────────┴───────┘
```

Important event classes found:

```
BootEvent
ShutdownEvent
BreakBlockEvent
DamageBlockEvent
PlaceBlockEvent
UseBlockEvent.Pre / UseBlockEvent.Post
SwitchActiveSlotEvent
PlayerChatEvent
PlayerConnectEvent
PlayerDisconnectEvent
PlayerInteractEvent
PlayerMouseButtonEvent
PlayerMouseMotionEvent
PlayerReadyEvent
EntityRemoveEvent
ChangeGameModeEvent
DropItemEvent
InteractivelyPickupItemEvent
```

This tells us which player actions can be proven through real gameplay instead of only dev commands.

## Capability Categories We Must Cover

```
╔══════════════════════╦═══════════════════════════════════════════════╗
║ Category             ║ Why MOTM Needs It                            ║
╠══════════════════════╬═══════════════════════════════════════════════╣
║ Input events          ║ spellbook slots, LMB/RMB, ability keys       ║
║ Item/slot events      ║ Alloy, tools, weapon buffs, swap cleanup     ║
║ Damage events         ║ hits, DoTs, shields, durability, passives    ║
║ Movement events       ║ stomp, frolic, dash, burrow, tunnel, flight  ║
║ Projectiles           ║ magma sling, cactus, sapling, nightshade     ║
║ Temporary blocks      ║ walls, pillars, roots, flowers, gems         ║
║ Temporary fluids      ║ lava pool, mud pit, hydro fields             ║
║ Entity effects        ║ coating, roots, poison, slow, invuln         ║
║ Model VFX             ║ stone skin, metal coat, obsidian skin        ║
║ Particles/trails      ║ impact rings, sandstorm, smoke, shards       ║
║ Decals                ║ cracks, ground scars, block-breaking marks   ║
║ Proxies/NPCs          ║ gems, summons, target dummies, visuals       ║
║ Prefabs/selections    ║ structures, arenas, complex temporary shapes ║
║ UI/HUD/custom pages   ║ spellbook, status, perks, dev test page      ║
║ Sounds/audio effects  ║ cast, impact, loop, warning, charge          ║
║ Camera/feedback       ║ shake, impact, direction, readability        ║
║ Weather/environment   ║ large aura styles, rain/mist/storm identity  ║
║ Asset packs           ║ custom models, textures, blocks, VFX         ║
║ Creative tools        ║ test arena, NPC freeze, prefabs, setup       ║
║ Logs/telemetry        ║ prove mechanics in text, not snapshots       ║
╚══════════════════════╩═══════════════════════════════════════════════╝
```

## Newly Identified Areas We Had Underweighted

### 1. ModelVFX As Its Own Asset Family

We had been treating coating mostly as `EntityEffect` tint. Local and public asset docs show `modelvfx` is its own visual asset category. That means coating work should search and test:
- `EntityEffect`
- `ApplicationEffects`
- `ModelVFXId`
- `modelvfx/` assets
- top/bottom tint fields
- status-effect style overlays.

### 2. Trails

The local asset package includes `trail`. This may be better than particles alone for:
- Frolic flower/path motion readability
- Wind Blade
- Magma Sling streaks
- Sand/Debris waves
- weapon buff swing trails.

### 3. BlockBreakingDecal

The local asset package includes `blockbreakingdecal`. This may help with:
- Quake cracks
- Sinkhole ground marks
- Aftershock impact scars
- Stone/earth rupture visuals

It needs a proof before use.

### 4. Camera Assets / Camera Effects

The local asset package includes `camera`, and gameplay assets include camera config categories. This may help with:
- Stomp impact shake
- Quake/Aftershock feedback
- heavy hit confirmation

Need proof that server-side plugins can trigger or influence this safely.

### 5. Sound And Audio Effects

The local asset package includes `soundevent`, `soundset`, `reverbeffect`, and `equalizereffect`. We should make sound part of ability identity, not a late polish pass:
- metal clang
- lava/fire whoosh
- stone crack
- hydro bubble
- wind rush
- corruptus whispers/low impact.

### 6. Asset Pack Path

The current mod has mostly avoided custom asset-pack authoring. Research confirms plugin asset packs can define assets, but Hytale's own docs say packaging/distribution is rough. We should treat custom asset packs as a separate gate:
- Use existing assets first.
- If impossible, create a tiny custom asset proof.
- Do not broad-convert the mod to custom assets until that proof passes.

### 7. Creative Tool / Entity Tool Use In Testing

Patch notes say the Entity Tool gained NPC freezing and scale options. This may be extremely useful for manual test setup:
- stationary targets
- scaled targets
- predictable lanes
- no runaway mobs.

We should document how to use it in the test-world runbook, even if the mod's automated harness continues to use commands.

### 8. Prefabs And Selections

The asset archive contains thousands of prefabs. For arena setup and some abilities, prefabs/selections may be better than ad hoc block placement:
- resettable test arena
- target lanes
- temporary structures
- 3x3 walls or multi-block shells.

Need a prefab placement/restoration proof before using in abilities.

## Remaining Uncertainties

### U1. First-Class Projectile Use

Known:
- `ProjectileModule` exists locally.
- Projectile asset category exists locally.
- `Server/ProjectileConfigs/Weapons` exists in `Assets.zip`.

Unknown:
- Exact easiest way to define or reuse a projectile config for MOTM.
- Whether model choice can cleanly show a lava blob without health bars or mob AI.
- Whether projectile interactions can trigger the MOTM damage/effect code exactly as needed.

Required proof:
- `motm dev proof projectile-magma-sling`.

### U2. Fluid Placement And Side Effects

Known:
- Fluid assets/classes exist.
- Lava/water-like visuals can appear.
- Real lava causes vanilla slow/damage/fire behavior unless compensated.

Unknown:
- Best safe API path for direct fluid placement/removal.
- Whether a custom harmless lava-like fluid is easier than compensating real lava.
- Whether large shells/rings can be spawned without camera/crash risks.

Required proof:
- `motm dev proof harmless-lava-pool`.
- `motm dev proof harmless-water-field`.

### U3. Slot Swap And Item-Bound Buff Cleanup

Known:
- `SwitchActiveSlotEvent` exists.
- `InteractionType` includes `SwapTo` and `SwapFrom`.
- Existing mod logic can detect damage and mining events.

Unknown:
- Best reliable way to tie slot swap to the owning player in all cases.
- Whether inventory/slot data is stable enough for Alloy-style "first valid item action binds buff to item."

Required proof:
- `motm dev proof item-bound-buff-cleanup`.

### U4. True Texture/Material Coating On Player And Items

Known:
- Dynamic tinting exists in Hytale's rendering model.
- `ModelVFX` and `EntityEffect` tints are viable directions.
- Metal Coat found a good visual route.

Unknown:
- Whether an existing block material texture can be used as a model coating.
- Whether item-only coating can be isolated without body coating.
- How far color tints can push the Stoneskin-like VFX before it stops looking right.

Required proof:
- `motm dev proof coating-palette`.

### U5. Decals And Ground Cracks

Known:
- `blockbreakingdecal` exists as an asset category.
- Existing particle composition can produce a readable crack/flash effect.

Unknown:
- Whether block-breaking decals can be spawned or controlled from a plugin for temporary ground markings.

Required proof:
- `motm dev proof ground-decal-crack`.

### U6. Client-Side UI And Input Limits

Known:
- Custom server-driven UI pages are supported.
- Official posts say some client behavior is not exposed to the server.
- Patch notes show input and UI systems are still changing.

Unknown:
- Whether every desired input/context can be intercepted cleanly without fighting the client.
- Whether inventory UI and creative tools can be automated beyond SendInput/screenshot workflows.

Required proof:
- `motm dev proof input-matrix`.
- Production spellbook and dev spellbook must remain separate.

## Research Completeness Gates

Before continuing broad Terra/Hydro/Aero/Corruptus implementation, complete these gates:

```
┌──────┬───────────────────────────────┬──────────────────────────────┐
│ Gate │ Name                          │ Pass Evidence                │
├──────┼───────────────────────────────┼──────────────────────────────┤
│ C0   │ Source map current            │ source-index updated         │
│ C1   │ Asset index exhaustive local  │ category files generated     │
│ C2   │ API index exhaustive local    │ class/method probes saved    │
│ C3   │ Projectile proof              │ spawn/hit/remove JSONL       │
│ C4   │ Fluid proof                   │ place/effect/remove JSONL    │
│ C5   │ Coating palette proof         │ screenshots + effect logs    │
│ C6   │ Decal/trail proof             │ visible + cleanup logs       │
│ C7   │ Input matrix proof            │ controls match dev commands  │
│ C8   │ Test arena proof              │ reset + stationary targets   │
│ C9   │ Friendly-filter proof         │ allies/summons unharmed      │
│ C10  │ Cleanup registry proof        │ no lingering temp objects    │
└──────┴───────────────────────────────┴──────────────────────────────┘
```

## What "Fully Manipulate Hytale" Really Means

Within the current public and local evidence, we can aim for deep server-side control:
- Java plugin logic.
- Commands.
- Events.
- Damage/stat/effect systems.
- Temporary world blocks/selections.
- Assets bundled in plugin jars.
- Existing base-game assets from `Assets.zip`.
- Server-driven UI pages and HUD layers.
- NPC/proxy/summon behavior.
- Projectiles, particles, trails, sounds, prefabs, and model VFX.

We should not assume unrestricted client manipulation:
- no memory editing
- no custom client mods
- no guaranteed access to every client UI/menu
- no guarantee that all desired camera/render behavior is exposed server-side
- no guarantee that early-access APIs are stable.

The correct bridge is:
- use server APIs for mechanics
- use existing/custom assets for visuals
- use structured telemetry for proof
- use screenshots/video for visual review
- keep fallback designs for client-side limits.

## Next Best Action

Implement the missing research harness, not another ability tweak:

1. Generate exhaustive UTF-8 asset indexes from `Assets.zip`.
2. Generate API signature indexes for the high-risk packages:
   - projectile
   - fluid
   - modelvfx/entityeffect
   - block/decal/trail
   - inventory/slot events
   - UI/HUD pages
   - camera effects
3. Add MOTM structured telemetry.
4. Add proof commands for:
   - projectile
   - harmless fluid
   - coating palette
   - decals/trails
   - item-bound buff cleanup
   - friendly filtering
5. Only then resume Magma/Terra fixes.

This is the strongest route to making ability implementation feel like engineering instead of trial-and-error art surgery.
