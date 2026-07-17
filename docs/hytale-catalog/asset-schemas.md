# Asset Authoring Schemas (verified against Assets.zip, 0.5.6)

Universal ID rule (proven): asset references use the **filename stem** (no extension, no
path) — `SystemId: "Block_Break_Stone"`, `EffectId: "Immunity_Fire"`, `Config:
"Projectile_Config_Fireball"`, `ModelChange: "Corgi"`, sound `"SFX_Sword_T2_Impact"`.
Path-form may also resolve (MOTM probes both) but stem is canonical. 29 duplicate system
stems + 93 duplicate spawner stems exist (mostly `_Test` vs production) — ALWAYS prefix
your assets (`MOTM_*`, `MyMod_*`). Unreferenced JSON is inert; unknown SystemIds are
silent no-ops and can crash asset validation. `Parent` inheritance is pervasive
(effects, projectiles, items, fluids, roles, weather).

## 1. Particles

Two file types under `Server/Particles/**`:

**`.particlesystem`** = composition: `Spawners:[{SpawnerId, PositionOffset,
RotationOffset{Pitch,Roll,Yaw}, FixedRotation, EmitOffset{min/max}, WaveDelay,
MaxConcurrent, StartDelay, SpawnRate, TotalSpawners}]` + system-level `LifeSpan`,
`CullDistance`, `BoundingRadius`, `IsImportant`, `Scale`.

**`.particlespawner`** = emitter+renderer: `RenderMode` (Erosion|BlendAdd|BlendLinear|
Distortion), `Shape` (Sphere|Cube), `EmitOffset`, `ParticleRotationInfluence` (None|
Billboard|Velocity|BillboardVelocity|BillboardY), `ParticleRotateWithSpawner`,
`MaxConcurrentParticles`, `SpawnRate{Min,Max}`, `TotalParticles` (-1 = continuous),
`SpawnBurst`, `ParticleLifeSpan`, `InitialVelocity{Speed,Yaw,Pitch}`, `Attractors`
(Radial/Linear acceleration/impulse, DampingMultiplier), `ParticleCollision{BlockType:
Solid|Air|All, Action:Expire|Linger|LastFrame}`, `VelocityStretchMultiplier`,
`CameraOffset`, `IsLowRes`, `UVMotion{Texture,SpeedX/Y,Strength,Scale,curve}`, and
`Particle{Texture, FrameSize, ScaleRatioConstraint, SoftParticles, UVOption,
InitialAnimationFrame, Animation{"0".."100" keys: FrameIndex,Scale,Rotation,Opacity,
Color}, CollisionAnimationFrame}`.

No attachment/bone field in particle JSON — attachment is supplied by the runtime owner
(EntityEffect `Particles[].TargetNodeName/TargetEntityPart` or ParticleUtil position).
LOD = system CullDistance/BoundingRadius/IsImportant + spawner IsLowRes/soft particles.
Ship custom systems in your pack under `Server/Particles/<Family>/...` + spawners in a
`Spawners/` sibling; textures pack-relative (`Particles/Textures/...`).

## 2. Models & Animations

- **`.blockymodel`** = JSON node tree: `{nodes:[{id,name,children,position,
  orientation(quat), shape{type: none|box|quad, offset, stretch, settings(size|normal|
  isPiece), visible, doubleSided, shadingMode: flat|fullbright, unwrapMode,
  textureLayout per-side {offset px, mirror, angle}}}], lod:"auto"}`.
- **Wrapper JSON** (`Server/Models/**.json`) is the model ASSET: `{Model:"NPC/.../
  Model.blockymodel", Texture, EyeHeight, CrouchOffset, HitBox{Min,Max}, Min/MaxScale,
  DefaultAttachments:[{Model,Texture}], Camera, AnimationSets, Icon, Parent,
  PhysicsValues, Particles, Trails(on named nodes)}`. Wrapper stem = the id used by
  `ModelChange` / `NPCEntity.setAppearance` / resolvers.
- **`AnimationSets`**: `{<SetName>: {Animations:[{Animation:"NPC/.../X.blockyanim",
  Speed, BlendingDuration, Looping, SoundEventId, Weight}], NextAnimationDelay}}`.
  Set names are the runtime vocabulary (Idle/Walk/Run/Attack/Alerted/Sleep/Hurt/Death/
  Swim*/Fly*/...). "Missing animation X for model Y" = requested set absent from the
  wrapper. Clips are reusable across models IF node names match.
- **`.blockyanim`** = `{duration(ticks), holdLastKeyframe, nodeAnimations:{<NodeName>:
  {position[], orientation[], shapeStretch[], shapeVisible[], shapeUvOffset[]}},
  formatVersion:1}` — keyframes {time, delta, interpolationType}. `shapeUvOffset` is
  the texture-sheet frame-animation mechanism (e.g. campfire flames stepping y by -32).
- **Textures**: every vanilla NPC/item PNG dimension is divisible by 32 (1,602 checked);
  64-grid common but not universal; rectangular atlases normal. UV offsets are pixels.
- Attachments bind by matching anchor node names (`L-Eye-Attachment`, `Chest`, ...);
  wrapper `DefaultAttachments` lists model+texture pairs, no target field.

## 3. EntityEffects (`Server/Entity/Effects/**`)

Top-level keys (full observed vocabulary): `Parent, Duration, Infinite,
OverlapBehavior(Overwrite|Extend|Ignore — NOT Replace), Debuff, StatusEffectIcon,
Invulnerable, ModelChange, ModelOverride, RemovalBehavior, DeathMessageKey,
DamageCalculatorCooldown, DamageCalculator{BaseDamage{Physical,Fire,Poison,
Projectile,...}}, DamageEffects{WorldSoundEventId,PlayerSoundEventId},
StatModifiers{Health,Stamina,Mana,SignatureEnergy}, RawStatModifiers,
StatModifierEffects{WorldParticles,WorldSoundEventId}, DamageResistance{type:
[{Amount,CalculationType}]}, ApplyConditions[{Id,Tags,EffectId,Inverse,Conditions,
Operator}], WorldRemovalSoundEventId, LocalRemovalSoundEventId` and
`ApplicationEffects{EntityBottomTint, EntityTopTint, ScreenEffect, LocalSoundEventId,
WorldSoundEventId, Particles[{SystemId,TargetNodeName,TargetEntityPart,PositionOffset,
Scale,Color}], FirstPersonParticles, ModelVFXId, EntityAnimationId,
HorizontalSpeedMultiplier, KnockbackMultiplier, MovementEffects{DisableAll,
DisableSprint}, AbilityEffects{Disabled}, MouseSensitivityAdjustment*}`.

Reference example (`Status/Burn.json`): Parent template + tints + ScreenEffect +
`Particles:[{SystemId:"Effect_Fire"}]` + ModelVFXId Burn + Fire 5 dmg/1s + Duration 3.
Morph example: `{Duration:60, ModelChange:"Corgi", ApplicationEffects:{Particles:[...],
WorldSoundEventId:...}}`. Condition example: apply unless InFluid Water / HasEffect
Immunity_Fire (Inverse), with LogicCondition + Operator Or.

## 4. Projectiles

**ProjectileConfig** (`Server/ProjectileConfigs/**`, 112): `Parent, Model, LaunchForce,
SpawnOffset{X,Y,Z}, SpawnRotationOffset{Pitch,Yaw,Roll}, Launch(World|Local)SoundEventId,
Physics{Type:Standard, Gravity, TerminalVelocityAir/Water, RotationMode(VelocityDamped|
VelocityRoll|Velocity|None), Bounciness, BounceCount, SticksVertically, AllowRolling,
RollingFrictionFactor}, Interactions{ProjectileSpawn|Hit|Miss|Bounce: {Cooldown, Rules
{Interrupting:[...]}, Interactions:[refs or inline]}}`.

**Projectile appearance** (`Server/Projectiles/**`, 87): `Parent, Appearance,
MuzzleVelocity, TerminalVelocity, Gravity, Bounciness, ImpactSlowdown, TimeToLive,
DeadTime(Miss), Damage, Radius/Height, aim-assist fields (Horizontal/VerticalCenterShot,
DepthShot, PitchAdjustShot), Hit/Miss/Death/BounceSoundEventId,
Hit/Miss/Death/BounceParticles{SystemId,Color,PositionOffset},
ExplosionConfig{DamageEntities/Blocks, radii, falloff, Knockback}`.

## 5. Interactions (the data-driven gameplay verb set)

`Server/Item/Interactions/**` (1,341) + `Server/Item/RootInteractions/**` (611).
Root = entrypoint bound to item slot/click: `{RequireNewClick, ClickQueuingTimeout,
Cooldown, Interactions:["..."]}`. 70 distinct `Type` verbs observed (recursive scan):

Simple, Serial, Parallel, Selector (raycast/area targeting: Range, Length, LineOfSight,
extents), Charging, Chaining, Repeat, Condition, BlockCondition, EffectCondition,
StatsCondition(WithModifier), DurabilityCondition, MovementCondition, MemoriesCondition,
PlacementCountCondition, FirstClick, Wielding, DamageEntity, ApplyEffect,
ClearEntityEffect, ApplyForce, ChangeStat(WithModifier), ChangeState, ChangeBlock,
ChangeActiveSlot, PlaceBlock, PlaceFluid, BreakBlock, DestroyBlock, Projectile,
LaunchProjectile, Explode, RemoveEntity, SpawnNPC, SpawnPrefab,
SpawnDeployableFromRaycast, ModifyInventory, OpenContainer, OpenCustomUI,
OpenProcessingBench, OpenTreasureContainer, RefillContainer, Door, Seating,
TeleportInstance, UseBlock, UseEntity, UseWateringCan, FertilizeSoil, HarvestCrop,
SendMessage, TriggerCooldown, ResetCooldown, TriggerSpawnMarkers, ContextualUseNPC,
CancelChain, ChainFlag, Interrupt, Replace (Var substitution), Aoe, Point, Directional,
Absolute, Force, Vulnerable, CanBreakRespawnPoint, DestroyTreasureCondition, BuilderTool.

Chain wiring: `Next` / `Failed` / nested `Interactions`, `Effects{ItemAnimationId,
WorldSoundEventId, WorldParticles}`, `RunTime`, `Costs`, `InteractionVars` +
`{Type:"Replace", Var, DefaultValue}` for per-item parameterization.

## 6. Items, Blocks, Fluids

**Item** (`Server/Item/Items/**`, 3,641): `TranslationProperties, Parent, Quality, Icon,
Model/Texture, ItemLevel, MaxStack, Categories, Tags, Set, PlayerAnimationsId,
ItemSoundSetId, Recipe{Input,Output,BenchRequirement,TimeSeconds}, Interactions{Primary/
Secondary/Use->root ids or inline}, InteractionVars, MaxDurability, Weapon/Armor/Utility/
Consumable blocks, ItemAppearanceConditions` (conditional model by stat!), and optional
embedded **BlockType**.

**BlockType** (embedded in item JSON): `Material(Solid|Empty), DrawType(Cube|Model|
None), Group, Textures:[{All|per-face, Weight}] (weighted variants), ParticleColor,
BlockParticleSetId (built-in vocabulary: Stone/Wood/Dust/Grass/Metal/Crystal/Ice/...),
BlockSoundSetId, PhysicalMaterialId, Light{Color,Radius}, CustomModel/Texture/Scale/
Animation, HitboxType, VariantRotation, State.Definitions{<state>: CustomModelAnimation},
Gathering{Breaking{GatherType,ItemId}}, Flags, Aliases, Opacity, CubeShadingMode
(Fullbright = glow-block trick), Interactions{Use:...}, Support/placement fields`.

**Fluids** (`Server/Item/Block/Fluids/`, 14 JSONs / 7 families: Water(+Finite), Lava,
Poison, Slime, Slime_Red, Tar, Fire): `MaxFluidLevel, Effect:["Water"|"Lava"],
Opacity, Textures, FluidFXId, Light, Ticker{Type(Finite|Fire), CanDemote, SpreadFluid,
FlowRate, Collisions{<other fluid>: {BlockToPlace, SoundEvent}}}, Tags{Fluid}`.
Fire is DrawType None + particles only. Fluids are SIMULATION, not inert VFX —
spread/collide/replace blocks; prefer fluid textures on inert blocks for ephemeral
ability visuals unless simulation is intended.

## 7. Weather, Sounds, UI

**Weather** (`Server/Weathers/**`, 87): 24-hour keyed curves `{Hour,Color|Value}` for
sky/fog/sun/moon colors, FogDistance [near,far], scales/densities, `Clouds[{Texture,
Colors,Speeds}]`, `Particle{SystemId, OvergroundOnly, Color, Scale}` (weather = a
particle system + palette!), `Tags` (Zone1-4, Rain/Snow/Storm...), ScreenEffect(Colors),
ColorFilters. Tags select **AmbienceFX** (`Server/Audio/AmbienceFX/**`):
`Conditions{WeatherTagPattern{Op,Tag}, Shelter[Open|Partial|Sheltered]}` ->
`AmbientBed{Track,Volume}` or `Sounds[{SoundEventId,Frequency,Radius}]`.

**SoundEvents** (`Server/Audio/SoundEvents/**`, 1,177): `{Parent, Layers:[{Files[.ogg],
Volume, RandomSettings{MinVolume,Min/MaxPitch}, StartDelay, RoundRobinHistorySize}],
Volume, AudioCategory, PreventSoundInterruption, MaxInstance, SpatialBlend, Pitch,
MaxDistance, StartAttenuationDistance}`. Referenced by stem from effects/interactions/
projectiles (`WorldSoundEventId`, `LocalSoundEventId`, `PlayerSoundEventId`, launch/hit/
miss/death/bounce fields).

**UI** (`Common/UI/**`, 452 files: 135 .ui, 295 png): `.ui` = declarative widget tree
(`Group`, `Label`, `TextButton`, `Sprite`, `ProgressBar`, `CircularProgressBar`,
`TimerLabel`) with `Anchor`, `LayoutMode`, `Padding`, `Background(TexturePath,Border)`,
Style objects; `@Macro` definitions and `$file.@symbol` imports with `...@Style` spread
(see vanilla `Common/UI/Custom/Common.ui` = the macro/style library: @Panel,
@DefaultTextButtonStyle, @DefaultSpinner 72-frame, @ProgressBar). Vanilla @2x textures
resolve from logical paths (`Common/ProgressBar.png`).

## 8. Data-driven vs plugin-code boundary

Pure data (no Java): effect definitions, projectile configs+appearance, item+interaction
graphs (incl. ApplyEffect/LaunchProjectile/SpawnNPC on item use — see Food_Bread's
`InteractionVars.Effect` inline ApplyEffect), weather+ambience, roles/AI, recipes,
blocks/fluids. Java required for: novel triggers (commands, arbitrary events, ticks),
runtime selection (weather/time), state machines beyond the interaction verb set,
custom interaction Types (registered via codec like Spellbook/Boats do), persistence.
