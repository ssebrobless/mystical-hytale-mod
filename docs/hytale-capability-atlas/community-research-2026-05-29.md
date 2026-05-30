# Community Research and Capability Gates - 2026-05-29

This note turns the current community research pass into practical MOTM
implementation gates. Community repos are useful as maps; the installed
`HytaleServer.jar`, local assets, and MOTM observability evidence remain the
authority before any gameplay feature is called done.

## Source Shape

```text
+-------------------------+----------------------------+---------------------------+
| Source                  | Best use                   | Trust rule                |
+-------------------------+----------------------------+---------------------------+
| vulpeslab/hytale-docs   | Concepts, package names,   | Verify every class and    |
|                         | system overview            | signature in local jar    |
| HytaleModding/template  | Java 25, Gradle wrapper,   | Compare with MOTM scripts |
|                         | plugin packaging sanity    | and current manifest      |
| HytaleModding/patcher   | Searchable/decompiled jar  | Discovery only; local jar |
|                         | workflow                   | and harness still decide  |
| Installed server jar    | Class existence and API    | Local authoritative       |
| MOTM observability      | Runtime behavior proof     | Acceptance authority      |
+-------------------------+----------------------------+---------------------------+
```

Sources checked:

- [vulpeslab/hytale-docs](https://github.com/vulpeslab/hytale-docs)
- [HytaleModding/plugin-template](https://github.com/HytaleModding/plugin-template)
- [HytaleModding/patcher](https://github.com/HytaleModding/patcher)

## Installed Jar Confirmation

The local jar at
`%APPDATA%/Hytale/install/release/package/game/latest/Server/HytaleServer.jar`
confirms the core primitives we need for public-use class/style work:

```text
Gameplay need              Local classes confirmed
-------------------------  ----------------------------------------------------
Projectiles                ProjectileModule, ProjectileConfig,
                           projectile components, predicted projectile systems
Temporary blocks           BlockTypeModule, BlockType, placement/settings,
                           DamageBlockEvent
Temporary fluids           Fluid, FluidTicker, FluidFX, ServerSetFluid(s)
Entity visuals/coatings    EffectControllerComponent, ModelComponent,
                           EntityScaleComponent, DynamicLight
Movement/control           MovementManager, MovementSettings,
                           MovementStates, PlayerInput movement systems
NPC roles/summons          NPC role/component packages and movement components
Crafting hooks             PlayerCraftEvent, PlayerCraftingSystems
Custom UI/HUD              UICommandBuilder, UIEventBuilder, CustomHud packet
```

Current local evidence:

- `audits/hytale-runtime-capabilities/community-research-2026-05-29/report.md`
- `audits/hytale-runtime-capabilities/community-research-2026-05-29/api-public-signatures.txt`
- `audits/hytale-asset-library/community-research-2026-05-29/report.md`
- `audits/hytale-asset-library/community-research-2026-05-29/keyword-catalog.md`

This means the remaining ability repair should not default to training dummy or
living-mob visual hacks where a first-class projectile, block/fluid placement,
model/effect component, or summon route can be proven.

## MOTM Translation Gates

Before broad class/style implementation, build or reuse these small proof gates.
Each gate should produce raw observability under `audits/agent-observability/`
or a targeted capability audit folder, not just screenshots.

```text
P1 Projectile proof
  -> spawn first-class projectile
  -> aim from crosshair/player view
  -> hit entity and block
  -> despawn cleanly
  -> current proof id: native-projectile-fireball
  -> current scenario id: projectile-native-fireball-proof

P2 Temporary block proof
  -> place real ability-owned blocks
  -> support orientation/stacking/walls
  -> prevent drops/material gain
  -> cleanup on expire, cancel, death, clear, reload

P3 Temporary fluid proof
  -> place lava/water/tar style fields at caster origin
  -> protect caster/allies from native negative fluid effects
  -> cleanup all cells with ownership id

P4 Summon/role proof
  -> spawn existing NPC/mob as friendly summon
  -> target hostiles only
  -> follow/despawn/kill/cancel correctly
  -> never persist test dummies into public gameplay

P5 Model/effect proof
  -> apply coating, statue transform, item/model visual, scale/tint
  -> remove on class/style swap and /motm dev clear
  -> avoid passive visuals where concepts say mechanical only

P6 Movement/control proof
  -> dash, root, jump-arm, glide, knockback, pull/leash
  -> verify state through server truth and player intent events
  -> guard fall damage and stuck/underground recovery where needed

P7 UI/HUD proof
  -> spellbook tabs and collapsible descriptions
  -> top-right passive/perk tracker fits all resolutions
  -> cooldown timers hide unless actively counting down

P8 Asset palette proof
  -> extract candidate particles/models/items/blocks per style
  -> map each ability to one canonical visual recipe
  -> no invented SystemIds, roles, or item ids
```

## Practical Class/Style Repair Order

Use this order before continuing the remaining style rebuild:

1. Asset palette extraction for Terra/Hydro/Aero/Corruptus using
   `scripts/generate-hytale-asset-manipulation-library.ps1`.
2. API signature capture using `scripts/probe-hytale-runtime-capabilities.ps1`
   plus targeted `javap` on projectile, block/fluid, movement, NPC, and UI
   classes.
3. Replace ability hacks by primitive family:
   - projectile abilities first,
   - real block/fluid abilities second,
   - summon/transformation abilities third,
   - movement/control abilities fourth,
   - HUD/UI cleanup last.
4. Add or extend an observability scenario for every primitive family before
   doing the full 40-style pass.
5. Run a class pass only when the primitive family gates it depends on are
   already green.

## Direct Implications For Current MOTM Issues

| Issue pattern | Better route indicated by research |
| --- | --- |
| Magma Sling or Sapling behaving like a mob | Use first-class projectile proof, then route projectile abilities through that helper. |
| Floating/block-dropping ability structures | Use ownership-tagged temporary block proof with ground anchoring and no-drop cleanup. |
| Lava pool/caster damage/slow conflicts | Use temporary fluid proof plus caster/ally protection and compensation effects. |
| Aqua Barrier residual on Terra | Class swap and `/motm dev clear` must clear effect family state, not only current style state. |
| Cave Vision unreliable | Treat as a lighting/effect proof. If dynamic light is local-radius only, record that limit and avoid claiming fullbright. |
| Dummies/test assets in public gameplay | Move them behind dev-only scenarios and make public ability paths use real targets/summons/projectiles. |
| Weapon/held-item coatings leaking | Model/effect proof must verify body-only, item-only, or both explicitly per ability. |

## Ready For Item 2

Item 1 produces enough confidence to move into item 2: build reusable primitive
owners and probes instead of patching each ability ad hoc. The first concrete
owner to build should be the projectile primitive, because it unblocks many
visible failures and removes the biggest "mob pretending to be a projectile"
problem.

## Item 2 Progress

`native-projectile-fireball` is now registered in the proof catalog and wired to
`ProjectileModule.spawnProjectile(...)` through the installed Hytale API. The
scenario `projectile-native-fireball-proof` drives it through the observability
harness and expects `proof_native_projectile_spawned`,
`proof_native_projectile`, and `proof_end` evidence.

The code/static/build rails passed on 2026-05-29. The first harness run
`native-projectile-proof-20260529` did not reach the proof because Hytale was
not running in-world; the diagnostic reported `Hytale process count: 0`.

The rerun `native-projectile-proof-20260529b` passed in-world. Raw evidence:

- `raw/motm-observability/client-intent.jsonl` contains
  `proof_native_projectile_spawned` with `spawned=true`.
- `raw/motm-observability/server-truth.jsonl` contains
  `proof_native_projectile` with `spawned=true`.
- `raw/motm-observability/causality.jsonl` contains `proof_end` with
  `Proof native-projectile-fireball PASS`.

This proves a first-class Hytale projectile can be spawned through
`ProjectileModule.spawnProjectile(...)`; the next implementation step is to
route MOTM projectile-style abilities through a reusable projectile owner rather
than NPC/mob visual proxies.
