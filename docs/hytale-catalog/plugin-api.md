# Plugin API Atlas (Hytale 0.5.6, javap-verified)

Jar: 39,618 members. Namespaces by class count: `server` 4,107 · `builtin` 2,142 ·
`protocol` 900 · `component` (ECS) 108 · `codec` 167 · `assetstore` 37 · `plugin` 3.

## 1. Lifecycle

```java
public final class MyPlugin extends JavaPlugin {          // extends PluginBase
    public MyPlugin(JavaPluginInit init) { super(init); }
    @Override protected void setup()    { /* data load, registrations */ }
    @Override protected void start()    { /* hooks after server ready */ }
    @Override protected void shutdown() { }
}
```

`PluginBase` accessors: `getName/getLogger/getIdentifier/getManifest/getDataDirectory/
getState`, registries `getEventRegistry / getCommandRegistry / getEntityRegistry /
getTaskRegistry / getEntityStoreRegistry / getChunkStoreRegistry / getAssetRegistry /
getClientFeatureRegistry / getCodecRegistry(...)`, config `withConfig(BuilderCodec)` and
`withConfig(String, BuilderCodec)`.

`PluginManifest` (codec-backed): Group, Name, Version, Description, Authors, Website,
Main, ServerVersion (semver range), Dependencies, OptionalDependencies, LoadBefore,
DisabledByDefault, IncludesAssetPack, SubPlugins.

## 2. Events

`EventRegistry` overloads: `register(Class, Consumer)`, prioritized
(`EventPriority` or `short`), keyed, `registerAsync(...)` (CompletableFuture pipeline),
`registerGlobal(...)`, unhandled. Returns `EventRegistration` — keep for cleanup.
Ordinary cancellables implement `ICancellable`; ECS cancellables extend
`CancellableEcsEvent`.

Full event catalog under `server/core/event/events/` (68 classes):

- **player**: PlayerConnectEvent, PlayerReadyEvent, PlayerDisconnectEvent,
  PlayerChatEvent (async, mutable content/targets/formatter), PlayerInteractEvent,
  PlayerMouseButtonEvent, PlayerMouseMotionEvent, PlayerCraftEvent (forRemoval),
  PlayerSetupConnect/DisconnectEvent, AddPlayerToWorldEvent,
  RemovedPlayerFromWorldEvent, DrainPlayerFromWorldEvent, PlayerRefEvent, PlayerEvent
- **ecs**: BreakBlockEvent, PlaceBlockEvent, DamageBlockEvent, UseBlockEvent$Pre/$Post,
  CraftRecipeEvent$Pre/$Post, InventoryChangeEvent, InventoryActiveSlotRequestEvent,
  InventorySetActiveSlotEvent, DropItemEvent($Drop/$PlayerRequest),
  InteractivelyPickupItemEvent, ChangeGameModeEvent, BreathingCheckEvent,
  DiscoverZoneEvent($Display), ChunkSaveEvent
- **entity**: EntityRemoveEvent, LivingEntityUseBlockEvent, EntityEvent
- **world**: StartWorldEvent, AddWorldEvent, RemoveWorldEvent, AllWorldsLoadedEvent,
  ChunkPreLoadProcessEvent, ChunkUnloadEvent, WorldGenChunksClearedEvent,
  MoonPhaseChangeEvent, ChunkEvent, WorldEvent
- **lifecycle**: BootEvent, ShutdownEvent, PrepareUniverseEvent
- **permissions**: PlayerGroupEvent($Added/$Removed), PlayerPermissionChangeEvent(+4
  nested), GroupPermissionChangeEvent($Added/$Removed)
- adjacent: PluginEvent/PluginSetupEvent, SendCommonAssetsEvent, PrefabPasteEvent,
  PrefabPlaceEntityEvent, DiscoverInstanceEvent, TreasureChestOpeningEvent

Rule: prefer `events.ecs.*` variants over deprecated legacy events.

## 3. ECS

Types: `Store, Ref, Holder, Archetype, ArchetypeChunk, ComponentType, ResourceType,
CommandBuffer, ComponentRegistryProxy, Query`. Registration via
`getEntityStoreRegistry()`:

```java
ComponentType<EntityStore,C> ct = reg.registerComponent(C.class, "id", CODEC); // persistent
reg.registerResource(R.class, R::new);
reg.registerSystemType(MySystem.class);  reg.registerSystem(new MySystem());
reg.registerEntityEventType(MyEcsEvent.class); reg.registerWorldEventType(...);
```

Store ops: `addEntity(Archetype|Holder, AddReason)`, `removeEntity(Ref, RemoveReason)`,
`get/ensureAndGet/put/replace/removeComponent`, `get/replaceResource`,
`forEachChunk(Query,...)`, `forEachEntityParallel`, `invoke(Ref|worldType, Event)`.
System hooks: `EntityEventSystem.handle(...)`, `WorldEventSystem.handle(...)`,
`TickingSystem.tick(float,int,Store)`, `EntityTickingSystem.tick(...,ArchetypeChunk,
Store,CommandBuffer)`, `DelayedSystem(float intervalSec)`.
Threading: no general thread-safety; mutate on owning thread or via `World.execute` /
command buffers. Use deferred queues when mutating during scans.

## 4. Capability -> Entry Point (selected; full 40-row table verified)

| Capability | Entry point |
|---|---|
| World lookup | `Universe.get().getWorld(name/uuid)`, `getDefaultWorld()` |
| Create/load world | `Universe.addWorld / makeWorld / loadWorld` (async futures) |
| World config | `World.getWorldConfig()` — seed, gamemode, PvP, time, forced weather, `getPluginConfig()` (PLUGIN_CODEC map = world-scoped plugin config hook) |
| Blocks | `World.getChunkIfLoaded/getChunkAsync`; `BlockAccessor.getBlock/setBlock/breakBlock/placeBlock/getBlockType`; `setBlockInteractionState` |
| Spawn entity | `World.spawnEntity(Entity, Vector3d, Rotation3f)`; `addEntity(..., AddReason)` |
| Custom entity type | `getEntityRegistry().registerEntity(String, Class, Function<World,T>, DirectDecodeCodec)` |
| Teleport | `Teleport.createForPlayer(...)` / `createExact(...)` component (prefer over legacy moveTo) |
| Velocity | `Velocity` component: `set/addVelocity/setClient/addInstruction(Vector3d, VelocityConfig, ChangeVelocityType)` |
| World time | `WorldTimeResource.getGameTime/setGameTime/setDayTime/getCurrentHour` |
| Weather | `WeatherResource.setForcedWeather` / `WorldConfig.setForcedWeather` |
| Sound | `SoundUtil.playSoundEvent2d/3d(...ToPlayer)` (numeric id + SoundCategory) |
| Particles (world-space) | `ParticleUtil.spawnParticleEffect(systemId, pos, store)` |
| Effects | `EffectControllerComponent.addEffect(ref, EntityEffect, store)`; asset via `EntityEffect.getAssetMap().getAsset(id)` |
| Projectiles | `ProjectileModule.spawnProjectile(ref, buffer, config, origin, direction)`; config via `ProjectileConfig.getAssetMap()` |
| NPC | `NPCEntity` (`setRoleName`, `setAppearance`, `setDespawnTime`, `addReservation(UUID)`), `NPCPlugin.get().hasRoleName/validateSpawnableRole`, `RoleChangeSystem.requestRoleChange(Ref, Role, int, boolean, [String,String,[boolean]], accessor)` |
| Page UI | `Player.getPageManager().openCustomPage(...)`; `CustomUIPage.build(Ref, UICommandBuilder, UIEventBuilder, Store)`; `BasicCustomUIPage`; `InteractiveCustomUIPage<T>` + `handleDataEvent` |
| HUD | `CustomUIHud(PlayerRef, String[, zOrder])`; `show()`; `update(boolean, UICommandBuilder)` |
| UI commands | `UICommandBuilder.append/appendInline/insertBefore/remove/clear/set/setNull`; `UIEventBuilder.addEventBinding(type, selector, EventData, bool)` |
| Commands | `getCommandRegistry().registerCommand(...)`; `CommandBase` + `withRequiredArg/withOptionalArg/withDefaultArg/withFlagArg`; `AbstractPlayerCommand`; custom `ArgumentType.parse` |
| Config | `withConfig(BuilderCodec)`; `Config.load/get/save` (futures) |
| KV persistence | `new DiskDataStore<>(String, BuilderCodec)` — `load/save/remove/list/loadAll` |
| Scheduling | `HytaleServer.SCHEDULED_EXECUTOR` + `getTaskRegistry().registerTask(future)` |
| Packets | `PlayerRef.getPacketHandler().write(ToClientPacket)`; `Universe.broadcastPacket`; intercept via `PacketAdapters.registerInbound/outbound` (host-sensitive) |
| Chat | `PlayerRef.sendMessage(Message)` |
| Assets | `getAssetRegistry().register(AssetStore)`; codecs via `getCodecRegistry` |

## 5. Deprecation ledger (`forRemoval=true` on 0.5.6)

- `Inventory` container getters + `getItemInHand` → `getSectionById(InventoryComponent.
  *_SECTION_ID)` (HOTBAR -1, STORAGE -2, ARMOR -3, UTILITY -5, TOOLS -8, BACKPACK -9)
  or ECS `InventoryComponent.*`; `InventoryComponent.getItemInHand(accessor, ref)`
- `PlayerCraftEvent` → `CraftRecipeEvent.Pre/Post` via `EntityEventSystem`
- `Player.getPlayerConnection()`, `Player.getPlayerRef()` → entity-store `PlayerRef`
  component; `PlayerRef.getPacketHandler()` / `sendMessage`
- `World.getPlayers()`, `World.addPlayer(...)`
- `WorldChunk.getFiller/getRotationIndex/getFluidId/getFluidLevel/getSupportValue(int,int,int)`
- `Entity.markNeedsSave/getNetworkId/getLegacyDisplayName/getUuid`
- `ComponentRegistry(Proxy).registerComponent(..., boolean)` / `registerSystem(..., boolean)`
- `Config.preloadedConfig(...)`

## 6. Hostile / unstable surfaces

`server.core.modules.*`, `server.npc.*` internals (1,003 classes), `builtin.*`,
`server.core.io.handlers.*`, generated `protocol.*`, static `PacketAdapters`,
`PluginManager.load/reload/unload`. Wrap uses behind a plugin-local compatibility layer;
expect migration on version bumps. Stale doc class names that do NOT exist:
`ParticleModule`, `SoundModule`, `TintComponent`, `NameplateComponent` — use
`ParticleUtil`, `SoundUtil`, EntityEffect tints, `Nameplate`.
