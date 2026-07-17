# Architecture Patterns From Real Mods (23 jars inspected, 0.5.6)

License rule first: NO inspected mod grants code/asset reuse. Statuses: explicitly
all-rights-reserved (EndgameQoL, Spellbook/Darkhax), proprietary signals (MMOSkillTree,
EndlessLeveling license-gate classes), or no license = default all-rights-reserved
(everything else). Bundled dependency licenses (MySQL GPLv2+FOSS, SQLite/SnakeYAML
Apache-2.0) do NOT license the mods. Therefore: **reimplement patterns, never copy
code or assets.** Cite the pattern source in commit messages for provenance.

## 1. Persistence patterns (class/leveling mods)

- **Codec-registered ECS components** for per-player state (RPGLeveling:
  PlayerLevelData/PlayerClassData + buff/marker components; MMOSkillTree: Skill/Quest/
  Achievement/Statistics components) — persisted by the store, survives restarts.
- **External DB for cross-world/leaderboard data**: JDBC+Hikari snapshot/merge
  (RPGLeveling DatabaseSyncService + PlayerDataSnapshot + merge policies + versioned
  config migrations), SQLite (EndlessLeveling, Tamework), plain JSON repository +
  owner index (HyTame JsonTamingRepository, Gravestones, Pets+ hybrid).
- **Reward reconciliation** (MMOSkillTree): on join/load, revalidate+reapply+revoke
  stat/ability rewards so config changes can't leave stale player state. Adopt for any
  perk/stat system.
- Config: BuilderCodec + `withConfig` + versioned JSON migrations + template/default/
  override resolution layers.

## 2. Ability triggering

Dominant pattern: **RootInteraction JSON as ability entrypoint** (MMOSkillTree ability
cast slots, Zephyr Root_Zephyr_Dodge1-3/Grapple, WansWonderWeapon 178 interaction
chains, EliteMobs ability channels) -> Java interaction/system resolves state:
interaction enqueues ECS state, tick systems resolve cooldowns/physics/damage.
Custom interaction Types are registered via codec (`Spellbook` idPascal registration,
Boats `teste` block interaction extends SimpleBlockInteraction with codec fields).
Hotbar: prefer interaction overrides on native slots; MMOSkillTree ships a
HotbarAbilityPacketFilter + AbilityInteractionInjector (packet route = advanced/risky).
Public API surface: static API class + CopyOnWriteArrayList listener buses + mutable/
cancellable progression events with source context (RPGLeveling ExperienceGainedEvent
with BlockBreak/EntityKill contexts; Zephyr PerfectDodge/GrappleStart lifecycle events).

## 3. Tether / grapple (GrapplingHook — direct blueprint for pull primitives)

- Zero-gravity ProjectileConfig (`Gravity:0`, VelocityDamped, BounceCount 1) with hook
  model + spawn/bounce/hit interaction events.
- Central `GrappleStateService` keyed by player ref: multi-hook slots, deferred detach
  scheduling.
- `GrappleTickSystem extends EntityTickingSystem` owns pull/flight physics per tick;
  temp-flight helper toggles movement states.
- **Chain visual = budgeted chain of small model entities** (`Chain_Segment_*` models,
  SEGMENT_SCALE/BASE_SPACING/MAX_SEGMENTS/SPAWN_BUDGET_PER_CALL constants), reused and
  cleaned each tick — NOT particles. A second viable tether visual besides particle
  chains; segment budget prevents entity spam.
- Shared entity stat (`GrappleActive` 0/1, `Shared:true`) drives conditional item
  appearance (`ItemAppearanceConditions`).

## 4. Owned allied NPCs (pet/tame mods — controlled-ally adjacent)

Three maturity tiers, all separating **ownership persistence** from **live behavior**:
- **HyTame (minimum viable)**: immutable `TamedAnimal` record (id, ownerId, mode
  FOLLOW/STAY, home xyz, maxFollowDistance, customName) + JSON repo with owner index +
  TamingService API (startCalming/feed/completeTaming/toggleBehaviorMode/release) +
  typed events (tamed/lost/teleported/mode-changed). Trust-based taming config per
  species.
- **Pets+ (lifecycle hardening)**: indexes pets by petId/entityId/owner; JDBC tables;
  `PetValidationSystem` relocates missing/wrong-world/too-far pets, cleans orphans;
  `PetLifecycleListener` retries respawn on world-add/disconnect. Rarity/level/perk
  scaling config.
- **Tamework (full framework)**: profile identity DECOUPLED from live NPC UUID;
  policy APIs (isOwner, evaluateClaimAccess/Damage/PopulationCap); progression
  snapshots (happiness/needs/lifestage/talents/traits); instruction-component assets
  (Follow_Advanced/Defend/Hold/Command_Move/Breeding/Needs_Seek); subscribe-based
  event API returning AutoCloseable.
Adopt: owner UUID + mode + home + leash distance as the core record; validation/respawn
system; role/attitude patches for wild-vs-tamed behavior gates (Template_All_Tamed_*).

## 5. Mounts & vehicles

Role JSON alone suffices (see npc-roles-ai.md recipe). Boats adds: water `Dive`
controller, custom block interaction that detects fluid and spawns the boat NPC from an
item, `MovementConfig` assets per tier. MoreMounts adds: custom NPC builder action
(`BuilderActionTame extends BuilderActionBase` with readConfig/build) invoking
`RoleChangeSystem.requestRoleChange` — the registered-builder-action pattern for data-
driven role verbs.

## 6. UI beyond one HUD slot

- Full screens = `InteractiveCustomUIPage<T>` classes with `build` +
  `handleDataEvent`; per-player state storage (JET browser state/pinned items).
- Compact persistent info = separate `CustomUIHud`; multiple HUDs coexist via
  MultipleHUD optional dep, or visibility gating (BetterMap reflectively hides its
  location HUD when the vanilla map is open).
- ReviveMe: value-builder helpers for HUD controls (groups/labels/progress bars/grids);
  bidirectional relationship maps (Reviver<->Downed, Carrier<->Carried) + codec
  component for the downed state.
- JET: registry caches (ITEMS/RECIPES/DROP_LISTS + bidirectional item<->recipe maps)
  built once at startup; separate recipe HUD component + update system.

## 7. Shared libraries

- JLib = the minimal library contract: singleton plugin registers codec-backed custom
  types (JMultiRespawnBlock component, JFireFluidTicker, JConditionInteraction) and
  exposes ComponentType getters; other packs author JSON against the registered types.
- Serilum `hybrid` = array-backed phased event factory + utility function library
  behind own callback facade.
- Spellbook (Darkhax) = codec outputs (ItemOutput/DropListOutput), custom asset
  interactions, block states, root command, in-plugin test helper (TestHelper.runTests).
- Integration bridges: optional-dependency + reflective adapter with no-op fallback
  (MMOSkillTree IntegrationRegistry; EndgameQoL RPGLeveling/EndlessLeveling XP
  provider selection). Declare in OptionalDependencies, probe at runtime.

## 8. Misc durable patterns

- EliteMobs: inject tag+state components into ARBITRARY spawned entities (elite
  variants of any mob); deterministic rarity roll; one ticker + per-ability state
  records; deferred spawn queue (never mutate ECS during scan); visual scaler.
- Boss framework (EndgameQoL): BossHealth/phase/enrage systems + boss-bar HUD builder
  + domain GameEventBus decoupling tracking from achievements/bounties/combos.
- GoneFishing: ephemeral entity + `BoundBobberComponent` (owner UUID) + tick lifecycle
  + typed events — the minimal "temporary owned entity" pattern.
- Gravestones: UUID+location dual index, pending-removal queue, container fill from
  the dead player's inventory, per-player cap + despawn timers.
- SubPlugins: one jar can declare nested plugins in its manifest (EndlessLeveling's
  EndlessQuests) — useful for optional feature modules.
- Declarative content imports: EndlessLeveling `imports/builtin/*/manifest.json` with
  schema_version/category/world_pattern/instance_definition — a pattern for shipping
  dungeon/encounter definitions as data.
