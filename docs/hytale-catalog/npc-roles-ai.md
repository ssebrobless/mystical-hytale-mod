# NPC Roles & AI Authoring (Server/NPC/**, 0.5.6)

452 leaf roles: Undead 140 (Skeleton 122) · Intelligent 123 (Aggressive 64) ·
Creature 120 (Livestock 70) · Aquatic 30 · Avian 20 · Elemental 11 · Void 6 · Boss 2 ·
Empty_Role. Role file forms: `Generic` (full object, e.g. Empty_Role, Kweebec_Merchant),
`Variant` (`Reference` + `Modify` overlay + `Parameters` `{Value,Description}` consumed
via `{Compute:"Param"}`), `Abstract` (non-standard roots). Filename stem = role id.

## Core templates (`_Core/Templates/`)

Template_Intelligent · Template_Livestock · Template_Animal_Neutral (adds IsTameable/
TameRequiredAttitudes/**TameRoleChange**) · Template_Predator (FleeRange, threat/leash)
· **Template_Summoned_Ally** (full combat params + AimingHitProbability/AimingSpread +
`DespawnTimer`=300 + `SummonerLeashRange`=30; used by Risen_Gunner/Risen_Knight) ·
Template_Swimming_Aggressive/Passive (Dive controller) · Template_Birds_Passive (Fly) ·
Template_Beasts_Passive_Critter/Cactee · Template_Edible_Critter · Template_Spirit ·
Template_Temple · Template_Placeholder (invulnerable animation shell — the Dragon bosses
are placeholders, not full combat roles).

Key parameter vocabulary (Intelligent, ~80 params): perception (ViewRange default 15,
ViewSector, HearingRange, AbsoluteDetectionRange 0.5-4, AlertedRange 8-50, AlertedTime,
InvestigateRange), combat (**Attack = interaction id string**, e.g.
`Root_NPC_Attack_Melee`; **AttackDistance** 1.5-15 [Intelligent] vs **AttackRange**
[Livestock/Swimming]; DesiredAttackDistanceRange, AttackPauseRange, Combat*Weight/
Strafe/BackOff/Pre-PostDelay, BlockAbility/Probability, ChaseRelativeSpeed,
TargetSwitchTimer, UseCombatActionEvaluator), movement (MaxSpeed, RunThreshold, Patrol +
PatrolPathName + FollowPatrolPath, WanderRadius, Leash{Distance,MinPlayerDistance,
Timer,Hard}), identity (Appearance, MaxHealth, DropList, IsMemory/MemoriesCategory,
NameTranslationKey), gear (Weapons, HotbarItems, OffHand slots). Livestock adds
feed/pet/harvest/produce/mount: AttractiveItemSet, IsPettable, PetRequiredAttitudes,
IsHarvestable + Harvest*, ProduceItem/Timeout, **IsMountable + MountAnchorX/Y/Z +
MountMovementConfig**, GrazingBlockSet, DayTimeNap*.

MotionControllers (only 3): `Walk` (MaxWalkSpeed, Gravity 15, JumpHeight, climb/descent/
fence keys), `Fly` (MaxHorizontalSpeed, climb/sink, min/max height over ground, turn/
roll), `Dive` (MaxSwim/DiveSpeed, SwimDepth, SinkRatio, depth bounds).

## Behavior authoring: Instructions

Nested `Instructions` arrays: `{Sensor, Actions|Instructions, Continue, Enabled,
ActionsBlocking/ActionsAtomic, BodyMotion, HeadMotion, Reference+Modify}`. State machine
= Sensor `{Type:State}` + Action `{Type:State|ParentState}`; `StateTransitions` at role
level. Flee/Patrol are BodyMotion/parameters, NOT actions.

**65 Action types**: Attack, ApplyEntityEffect, Appearance, DisplayName, Spawn (Kind,
CountRange, DelayRange, SpawnAngle, JoinFlock, FanOut), Despawn, Die, Remove, Role
(runtime role swap), OverrideAttitude (Attitude + Duration 60-600s, per-target,
transient), Mount, PlayAnimation, PlaySound, SpawnParticles, ModelAttachment, Inventory,
DropItem, PickUpItem, PlaceBlock, SetBlockToPlace, SetStat, SetFlag, SetMarkedTarget,
StorePosition/ReadPosition, SetLeashPosition, Beacon/FlockBeacon/FlockState/FlockTarget/
JoinFlock/LeaveFlock, Timer{Start,Restart,Continue}, SetAlarm, Timeout, Sequence,
Random, Test, Log, Notify, CompleteTask, StartObjective, LockOnInteractionTarget,
SetInteractable, OpenBarterShop, TriggerSpawnBeacon, TriggerSpawners, ReleaseTarget,
ResetInstructions/ResetPath/ResetBlockSensors/ResetSearchRays, RecomputePath, MakePath,
OverrideAltitude, Crouch, CombatAbility, AddToHostileTargetMemory, Block/EntityHit-
Interaction, ToggleStateEvaluator, Nothing, ParentState, State.

**48 Sensor types**: Target{Range,TargetSlot,Filters(LineOfSight...),Once}, Player,
Mob{ExcludeOwnType,Collector,Prioritiser}, Self, Block{Blocks,Reserve}, BlockChange,
Damage{Combat,Friendly,...}, InflictedDamage, State, Flag, Timer, Alarm, Time, Weather,
Light, InAir/InWater, Path, Nav, Leash, Beacon{Message,ConsumeMessage}, FlockLeader,
FlockCombatDamage, DroppedItem, CanInteract, HasInteracted, InteractionContext,
CombatActionEvaluator, Animation, MotionController, Random, Count, Eval, Switch,
SearchRay, Charge*, IsBusy, IsBackingAway, EntityEvent, AdjustPosition, ReadPosition,
CanPlaceBlock, And/Or/Not/Any.

Canonical combat instruction:
```json
{ "Sensor": {"Type":"Target","Range":{"Compute":"AttackDistance"},
             "Filters":[{"Type":"LineOfSight"}]},
  "Actions": [{"Type":"Attack","Attack":{"Compute":"Attack"}}] }
```

## Attitude system (faction layer)

`Server/NPC/Attitude/Roles/**` group JSONs map group -> {Hostile|Neutral|Friendly|
Ignore|Revered: [groups/Player]}. Graph highlights: Prey/PreyBig fear Predators+Void;
Void hostile to Feran/Kweebec/**Player**; Trork vs Kweebec war; Golem hostile to Trork/
Undead/Void. Template defaults: Summoned_Ally = NPC+Player Ignore, no group; Intelligent
= Empty group + Player Hostile; Livestock = Prey + Player Revered. Three DISTINCT
mechanisms — do not conflate:
1. **Static faction**: role's `AttitudeGroup` + group JSON (future target classification)
2. **Transient per-target**: `OverrideAttitude` action (Duration-bound)
3. **Runtime role swap**: data `Role` action, or Java
   `RoleChangeSystem.requestRoleChange(Ref, Role, int, boolean, [...], accessor)`

## Recipes

**Summoned ally**: Variant of Template_Summoned_Ally; Modify Appearance/MaxHealth/
Attack/AttackDistance/perception; set DespawnTimer + SummonerLeashRange; spawn via
world code or `Spawn` action (Broodmother pattern: `{Type:"Spawn", Kind:
"Scarak_Fighter", CountRange:[2,2], JoinFlock:true, FanOut:true}`); FlockArray drives
follower/leader behavior; template already handles hostile-override on summoner damage.

**Tameable pet**: Variant of Template_Animal_Neutral with `IsTameable:true`,
`TameRequiredAttitudes`, `TameRoleChange:"MyMod_TamedPet"`; template's
InteractionInstruction runs CanInteract -> HasInteracted -> `Role` action with
`DetachFromSpawning:true`. Author the tamed role as a second Variant (friendly
AttitudeGroup, follow via FlockArray/AttractiveItemSet).

**Mount**: any role with `IsMountable:true` + `MountAnchorX/Y/Z` +
`MountMovementConfig:"<id>"` + `Server/Entity/MovementConfig/<id>.json` (BaseSpeed,
Acceleration, JumpForce...) + InteractionInstruction `CanInteract -> SetInteractable ->
HasInteracted -> Mount`. Water mounts use Dive controller (Boats pattern).

**Guard rails**: always `NPCPlugin.get().hasRoleName(id)` before `setRoleName`/role
change (missing roles SEVERE at reload); reservation via sensor-level `Reserve:true`
(Block sensors) and `NPCEntity.addReservation(UUID)`; `DetachFromSpawning` frees a
spawned NPC from its spawner.
