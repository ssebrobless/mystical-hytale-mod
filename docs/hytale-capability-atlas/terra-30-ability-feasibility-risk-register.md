# Terra 30-Ability Feasibility And Risk Register

Date: 2026-05-24

Purpose: cross-check every Terra ability plan against known Hytale capability,
known MOTM code paths, public documentation, local asset/API research, and the
bugs we should expect before implementation. This is the "plan ahead before
coding" register.

## Authority Stack

```
╔══════════════════════════════════════════════════════════════════════╗
║                     TERRA FEASIBILITY DECISION STACK                ║
╠══════════════════════╦════════════════════════════════╦══════════════╣
║ User concept review  ║ what the ability must be       ║ design truth ║
║ terra_styles.json    ║ current data contract          ║ repo truth   ║
║ Public Hytale docs   ║ intended plugin/asset model    ║ directional  ║
║ Local Assets.zip     ║ exact assets on this machine   ║ hard source  ║
║ Local server jar     ║ exact classes/methods          ║ hard source  ║
║ Runtime proof        ║ actual in-game behavior        ║ final gate   ║
╚══════════════════════╩════════════════════════════════╩══════════════╝
```

Public references checked again during this pass:

- Hytale modding strategy and status:
  `https://hytale.com/news/2025/11/hytale-modding-strategy-and-status`
- Hytale projectile docs:
  `https://hytale-docs.pages.dev/modding/systems/projectiles/`
- Hytale entity effect docs:
  `https://hytale-docs.pages.dev/modding/systems/entity-effects/`
- Doctale model effects docs:
  `https://doctale.dev/asset-development/vfx/model-effects/`
- Doctale trails docs:
  `https://doctale.dev/asset-development/vfx/trails/`

Public docs confirm the direction, but local jar/assets and runtime proofs decide
whether a given route is allowed in this installed Hytale build.

## Status Key

| Status | Meaning |
|---|---|
| READY | Existing Hytale/MOTM primitives make the plan straightforward |
| PROOF-FIRST | Likely possible, but a focused proof must pass before final wiring |
| RISKY | Possible only with guarded fallback because prior tests already exposed crash/visual/control risks |
| DATA-DRIFT | Current JSON/data still describes older behavior and must be surgically aligned |

## Shared Hazards

```
╔══════════════════════╦═══════════════════════════════════════════════╗
║ Hazard               ║ Abilities Most Affected                       ║
╠══════════════════════╬═══════════════════════════════════════════════╣
║ Friendly harm        ║ all negative AoEs, fields, dashes, projectiles║
║ Lingering terrain    ║ wall, pillar, flowers, sapling, fluids, gems  ║
║ Living visual proxy  ║ Magma Sling, cactus, gem, lure objects        ║
║ Weak tint/read       ║ Metal, Stone, Obsidian, Sand, poison          ║
║ Native fluid effects ║ Lava Pool, Obsidian shell, Mudpit             ║
║ Player movement bugs ║ Stomp, Burrow, Dust Devil, Tunnel             ║
║ Data/concept drift   ║ Vines, Sapling, Pillar, Gem, Lava Pool        ║
║ Snapshot-only proof  ║ all visually subtle abilities                 ║
╚══════════════════════╩═══════════════════════════════════════════════╝
```

The fix is not to tune each ability in isolation. The fix is to build reusable
proofed primitives, then wire ability-specific behavior onto them.

## Per-Ability Feasibility Matrix

### 1. Quake / Stomp

Status: PROOF-FIRST.

Can Hytale do it: yes. MOTM already has spellbook input, armed-stomp state, tick
polling, velocity/position access, AoE resolution, damage/status code, and Quake
visual particles.

Expected complications:
- no confirmed `PlayerJumpEvent`/`PlayerLandEvent`, so landing must remain
  tick/velocity/ground-state based;
- test can falsely pass if no target is actually inside the landing AoE;
- knockback class/path crashes only appear when a real target is hit;
- edge/platform tests can make movement evidence misleading.

Plan adjustment:
- require `stomp.armed`, `stomp.land`, `stomp.resolve`, `targets`, `friendlySkips`,
  and `cleanup` logs;
- autonomous/user test must jump and land near a grounded target;
- third-person visual review must see the landing ring/crack.

Proof before complete:
- 10 normal-control Stomp casts from cold launch with at least one
  `targets>=1` line and no class/table crash.

### 2. Quake / Aftershock

Status: READY with optional decal proof.

Can Hytale do it: yes. This is a self-centered AoE/field plus status effects and
particle visuals. The current data radius is already 8.

Expected complications:
- visual could be too subtle without a clear ground mark;
- spherical radius must not accidentally behave like flat radius if the code uses
  horizontal-only distance;
- allies/summons need explicit skip logs.

Plan adjustment:
- implement with server-side target query, enemy-only slow/disorient, and the
  existing flash/crack particle composition;
- add BlockBreakingDecal only if `ground-decal` proof passes.

Proof before complete:
- one target inside 8 blocks hit, one outside skipped, friendly skip logged.

### 3. Quake / Sinkhole

Status: READY for buried-look, PROOF-FIRST for stronger crack decals.

Can Hytale do it: yes by visual/status trick, not by physically lowering NPCs.
EntityEffect, root/status, DoT ticks, and dust/crack particles are supported.

Expected complications:
- if code chases NPC position/teleport, release bugs return;
- buried target can be hard to track visually;
- multiple simultaneous fields can release the wrong target if keyed weakly;
- data still contains `vertical_displace_blocks`, which could tempt the wrong
  implementation path.

Plan adjustment:
- ignore `vertical_displace_blocks` for runtime;
- key buried victim state by caster/field/target id;
- use repeated ground crack plus brown dust marker at target anchor.

Proof before complete:
- engage/tick/release for same target id, visible marker during full duration.

### 4. Metal / Iron Wall

Status: READY with alignment hardening.

Can Hytale do it: yes. Temporary block placement/restoration is already the right
primitive and was visually accepted after iteration.

Expected complications:
- forward vector/camera yaw can rotate wall sideways;
- target/cursor-based fallback can place wall too far away;
- cooldown timing can start at cast instead of wall disappearance;
- cleanup failure can leave blocks behind.

Plan adjustment:
- never use cursor target for final placement;
- compute near-caster anchor from player forward vector and one-block gap;
- snap wall plane to dominant horizontal axis to prevent diagonal/sideways walls;
- store original blocks and restore all on expiry/recast/disconnect.

Proof before complete:
- cast facing north/east/south/west; wall stays one block forward and restores.

### 5. Metal / Metal Coat

Status: READY, but visual route must not be simplified.

Can Hytale do it: yes. The approved route is EntityEffect + ModelVFX/Stoneskin +
top/bottom tint. It may include held item coating if that preserves the look.

Expected complications:
- particle-only effect reads as faint white smoke;
- trying to isolate body from held item may lose the good visual;
- effect duration can outlive/underlive gameplay state if not cleaned manually.

Plan adjustment:
- preserve the strong coating effect family;
- use explicit effect remove on expiry/death/style swap;
- do not attach Alloy gameplay text/state to Metal Coat.

Proof before complete:
- third-person pass: visible dark gray coating for full duration.

### 6. Metal / Alloy Enhancement

Status: READY for mechanics, PROOF-FIRST for item-only visual isolation.

Can Hytale do it: mostly yes. Existing code can handle damage follow-up and tool
use; SwitchActiveSlotEvent exists, though slot ownership needs careful proof.

Expected complications:
- body coating instead of item coating;
- buff not clearing on swap;
- damage logs show bonus but floating damage numbers do not;
- ranged/magic items accidentally consuming charges;
- durability events may not expose every tool use path cleanly.

Plan adjustment:
- bind on first eligible melee/tool action, not on cast;
- store bound item id and remaining charges;
- reject spellbook/ranged/magic item ids;
- clear on slot swap, item mismatch, three charges spent, expiry, death.

Proof before complete:
- three boosted hits/tool uses, fourth normal, swap clears, invalid item rejected.

### 7. Magma / Lava Pool

Status: RISKY because native lava behavior fights the design.

Can Hytale do it: likely yes, but not safely by blindly placing real lava. Fluids
exist, temporary terrain exists, and visual fields exist, but real lava has native
slow/damage/fire behavior.

Expected complications:
- caster gets slowed, damaged, or lit on fire;
- speed compensation can fail sideways/strafe or stack badly;
- field can become invisible if visual route changes;
- cleanup can leave lava on targets/ground;
- cursor targeting can spawn away from player.

Plan adjustment:
- final ability should be caster-centered;
- run a harmless-fluid proof before using real lava;
- if real lava cannot be made friendly-safe, use lava-like visual blocks/field
  plus server-side enemy-only burn ticks.

Proof before complete:
- caster walks/strafe-walks through own pool normally with no damage/fire; enemy
  burns; cleanup registry reports zero leftover terrain.

### 8. Magma / Obsidian Skin

Status: RISKY because prior shell geometry caused client crashes.

Can Hytale do it: yes in concept, but the shell must be camera-safe. Temporary
blocks and coating effects exist; large/close lava shells need guarded geometry.

Expected complications:
- client "index outside bounds" crash from shell/camera geometry;
- root/immobility may not persist through the whole lava phase;
- shell visuals can block camera too aggressively;
- purple-black coating may look like faint tint unless Stoneskin/ModelVFX route
  is applied correctly;
- cleanup can leave shell blocks.

Plan adjustment:
- build shell proof as a bounded ring/cage with no block occupying camera center;
- cap placement count per tick;
- root with explicit movement/velocity zero during lava phase;
- apply Obsidian coating only after shell cleanup.

Proof before complete:
- 5 cold repeated casts, no crash, root holds, shell cleans, coating visibly
  midnight purple-black.

### 9. Magma / Magma Sling

Status: PROOF-FIRST.

Can Hytale do it: yes, likely through ProjectileModule and projectile configs.
The proxy-mob route is not acceptable as final.

Expected complications:
- visual accidentally spawns a living mob/nameplate;
- projectile may not align with aim if direction comes from the wrong vector;
- arc/speed can be too fast to see;
- impact callback may be hard to route into MOTM damage/status;
- projectile can despawn without visible hit.

Plan adjustment:
- use first-class projectile proof before ability wiring;
- log origin, direction, config id, speed, hit block/entity, expiry reason;
- add trail/impact VFX for readability.

Proof before complete:
- normal cast fires a visible lava/fire blob along aim, hits/despawns, no mob UI.

### 10. Stone / Rubble Rouser

Status: PROOF-FIRST for arm-only visuals, READY for fallback.

Can Hytale do it: yes as a gameplay buff/attack with stone VFX. Arm-only coating
is not proven.

Expected complications:
- full-body coating may be the only reliable route;
- item/hand-only visual may not be separable;
- if treated as projectile, it may drift from the intended melee/arm identity.

Plan adjustment:
- implement mechanics as melee/unarmed stone buff first;
- test attachment-specific/held-item coating as a proof;
- fallback to full body/held-item stone coating plus impact frames.

Proof before complete:
- melee hit shows stone effect and correct damage/knockback; visual judged close
  enough if arm-only is impossible.

### 11. Stone / Pillar Strike

Status: DATA-DRIFT, then READY.

Can Hytale do it: yes. Temporary block columns are a strong fit.

Expected complications:
- current data height is 5, concept wants 3;
- target anchor may place pillar offset or under invalid terrain;
- staged placement must be fast enough to read without being slow/clunky;
- pillar can trap target or leave blocks behind.

Plan adjustment:
- surgically align data/runtime to 1x1x3;
- use staged column: bottom, middle, top over short intervals;
- apply launch/stun at first or final stage;
- restore original blocks after short duration.

Proof before complete:
- target gets lifted/stunned and 3-block pillar appears/restores.

### 12. Stone / Rockslide

Status: PROOF-FIRST for visual quality.

Can Hytale do it: yes as a line/zone sweep with particles/trails/temporary nodes.

Expected complications:
- can look like invisible AoE if no moving front is rendered;
- too many blocks/particles can be noisy;
- terrain slope can make path nodes float/sink.

Plan adjustment:
- use server-side line/zone mechanics;
- use surface-sampled visual nodes with stone/dust trail;
- avoid replacing terrain unless a rubble-node proof passes.

Proof before complete:
- user sees forward rocky wave and logs prove path hits/skips.

### 13. Arbor / Rooted

Status: READY after surface-anchor primitive.

Can Hytale do it: yes. Temporary surface decorations/effects are a good match.

Expected complications:
- previous floor replacement behavior can return if anchor code writes into
  support block;
- self-root can make player feel bugged if duration/cancel unclear;
- heal/status proof needs logs, not just visual.

Plan adjustment:
- all root/vine/flower placements use `surfaceDecorationAnchor`;
- place visuals on top of support block only;
- emit heal/root start/end logs.

Proof before complete:
- visual sits above ground and the block under player is unchanged.

### 14. Arbor / Vines

Status: DATA-DRIFT, then PROOF-FIRST.

Can Hytale do it: yes. Target state plus EntityRemoveEvent/death cleanup is the
right route.

Expected complications:
- current data still has cooldown 4, concept wants no cooldown;
- old target can remain rooted when retargeting;
- death/removal cleanup may miss if target id changes or event lacks owner info;
- friendly targets must be rejected.

Plan adjustment:
- surgically set cooldown to 0 or runtime override to no cooldown;
- maintain one active target per caster;
- always release previous target before applying new one;
- add death/removal cleanup sweep as backup.

Proof before complete:
- target A releases when B is rooted; target death clears visuals/status.

### 15. Arbor / Sapling

Status: DATA-DRIFT and PROOF-FIRST.

Can Hytale do it: yes, but current data says treant summon while the concept now
wants ground marker sapling/lure.

Expected complications:
- projectile can hit enemies instead of passing through to ground;
- actual sapling block placement can fail on invalid support;
- lure/taunt AI may not be directly exposed and may need movement/targeting
  workaround;
- marker can be treated as a living summon by old code.

Plan adjustment:
- convert runtime from combat summon to ground-marker object;
- use pass-through projectile/ray to surface;
- use sapling block/proxy plus enemy lure field around it.

Proof before complete:
- shot through enemy lands on ground, sapling appears, nearby enemies move/target
  toward lure or logs prove lure fallback.

### 16. Bloom / Nightshade

Status: PROOF-FIRST.

Can Hytale do it: yes with pass-through projectile or server ray and surface
marker.

Expected complications:
- native projectile collision may stop on first enemy, conflicting with concept;
- flower may replace blocks if surface anchor is wrong;
- lure before explosion may be hard if AI targeting hooks are limited;
- poison visual can be too subtle.

Plan adjustment:
- use server-sim pass-through until surface impact if first-class projectile
  cannot ignore entities;
- flower marker owns lure/explosion timing;
- apply poison as body-hugging purple EntityEffect/status.

Proof before complete:
- enemy in path does not block flower placement; lure/explosion/poison logs fire.

### 17. Bloom / Frolick

Status: READY after surface-anchor and movement sampling fix.

Can Hytale do it: yes. Movement sampling and temporary decorations are sufficient.

Expected complications:
- flowers can replace floor and pull player downward if anchor is wrong;
- trail may appear under/inside player instead of behind;
- too many placements can cause spam/cleanup debt;
- stationary tests prove nothing.

Plan adjustment:
- sample movement only when displacement exceeds spacing threshold;
- compute behind-player anchor and place above support block;
- clear trail on expiry/recast/death/disconnect.

Proof before complete:
- third-person movement test shows flowers behind player with no terrain damage.

### 18. Bloom / Cacti Cluster

Status: PROOF-FIRST.

Can Hytale do it: yes, but projectile collision and attached-state logic must be
customized.

Expected complications:
- cactus projectile may need proxy visual if no first-class cactus projectile
  config exists;
- attached target can get double-damaged by explosion;
- secondary targets can accidentally get attached markers;
- slow/DoT may affect allies unless filtered.

Plan adjustment:
- create `AttachedCactus` state with target/surface anchor and attached target id;
- delayed explosion excludes attached target from extra burst;
- secondary AoE applies DoT/slow without marker attach.

Proof before complete:
- direct target receives initial DoT only; surrounding targets receive delayed
  secondary DoT/slow.

### 19. Self Petrification / Gargoyle

Status: READY.

Can Hytale do it: yes. Toggle/status plus stone coating is a known primitive.

Expected complications:
- cooldown can start at cast instead of end/cancel;
- manual cancel may not call the same cleanup path as expiry;
- Tunnel combo can reject or clear Gargoyle by accident.

Plan adjustment:
- single `endGargoyle` path starts 6s cooldown;
- allow Tunnel state to coexist with Gargoyle coating/state;
- log natural end vs manual cancel.

Proof before complete:
- cancel and natural expiry both remove coating and start 6s cooldown.

### 20. Self Petrification / Glare

Status: READY with target cleanup proof.

Can Hytale do it: yes. Target EntityEffect and status phases fit.

Expected complications:
- coating can remain after stun ends;
- slow tail can start too early or overlap with petrify;
- target death/removal can leave state entries;
- allies/summons must be skipped.

Plan adjustment:
- target state has petrify phase and slow-tail phase;
- remove stone coating before applying 2s slow tail;
- cleanup on death/removal.

Proof before complete:
- stone target releases visually, then remains slowed for 2s.

### 21. Self Petrification / Tunnel

Status: RISKY.

Can Hytale do it: probably, but only with a guarded movement/safe-exit system.
True free underground traversal is the hardest Terra movement requirement.

Expected complications:
- player can get stuck in terrain;
- camera can clip or become unreadable;
- teleport/position loops can destabilize client;
- cave-air-pocket exit may be ambiguous;
- overlap with Gargoyle can break form visuals or movement.

Plan adjustment:
- start with duration-based form and forced safe-surface exit;
- only add cave-air-pocket exit after proof;
- log every sampled position, rejected position, and exit reason;
- emergency unstuck command/path must exist.

Proof before complete:
- repeated Tunnel casts near terrain never leave player stuck; Gargoyle+Tunnel
  combo works; exit reason is logged.

### 22. Soil / Burrow

Status: PROOF-FIRST.

Can Hytale do it: yes as short guarded dash/teleport plus entry/exit visuals.

Expected complications:
- player can dash off floating test platform;
- ability can look like ordinary teleport if entry/exit effects are weak;
- target hit at exit can miss if position/direction is wrong;
- Burrow can blur with Tunnel if visuals are too similar.

Plan adjustment:
- use 4-block forward dash with ground/edge safety check;
- entry dust, hidden/dip cue, exit eruption cue;
- exit AoE resolves after final position.

Proof before complete:
- controlled lane test shows 4-block dash and exit AoE hit/knockback.

### 23. Soil / Mudpit

Status: RISKY for real water, READY for visual field fallback.

Can Hytale do it: yes as a field. Real water/fluid route depends on harmless
fluid proof.

Expected complications:
- water may slow caster/allies;
- brown tint on water may not be exposed;
- Hydro "counts as water" interaction may need server-side tag rather than real
  fluid;
- cleanup can leave fluid/blocks.

Plan adjustment:
- represent mud as server-side water-tagged field first;
- use brown dust/debris overlay and/or safe water blocks only after proof;
- enemy-only debuff ticks.

Proof before complete:
- caster/allies walk normally, enemies debuffed, Hydro water tag logs true.

### 24. Soil / Debris

Status: READY with trail visual proof.

Can Hytale do it: yes. Line sweep plus particles/trails is a good fit.

Expected complications:
- if tinting fails, it reads as white smoke;
- projectile-volley code may imply thrown dirt blocks, conflicting with concept;
- line width can hit targets outside the visual wave.

Plan adjustment:
- use brown-tinted dust/debris trail nodes;
- synchronize visual sweep width with hit sweep;
- avoid block projectile as primary visual.

Proof before complete:
- forward brown debris wave visibly matches hit path.

### 25. Sand / Sandstorm

Status: PROOF-FIRST for visual identity, READY for state rules.

Can Hytale do it: yes. Toggle duration/cooldown and follow-owner fields are
implementable. Sand-colored visuals need proof.

Expected complications:
- smoke can remain white/gray despite tint;
- follow field can lag behind moving player;
- manual deactivate and natural expiry can double-start cooldown;
- Dust Devil consume can leave orphan field/effects;
- caster/allies must be skipped.

Plan adjustment:
- use toggle state with one end path;
- test trails/sand particles/block-ring support instead of smoke only;
- active field follows player and logs tick center.

Proof before complete:
- activate/deactivate/expire/Dust Devil consume all cleanly end Sandstorm with
  2s cooldown and no orphan visuals.

### 26. Sand / Dust Devil

Status: RISKY until movement/field combo proof.

Can Hytale do it: yes in principle with dash movement, pull/knockback, and active
Sandstorm state.

Expected complications:
- inactive cast can still fire if precondition only exists in one command path;
- dash can move player off safe terrain;
- enemy drag can fail if mobs move/run away;
- consume Sandstorm can leave visual field active;
- Vitrification/Sandstorm layering can conflict.

Plan adjustment:
- precondition in StyleManager and runtime command path;
- use safe-lane dash checks;
- pull targets by field during dash and expel at end;
- call one Sandstorm cleanup function after expel.

Proof before complete:
- inactive fail message, active dash/drag/expel, Sandstorm ends and cooldown
  starts.

### 27. Sand / Vitrification

Status: PROOF-FIRST for projectile visual, READY for combo state.

Can Hytale do it: yes as projectile/status with glass/spark/burn effect.

Expected complications:
- projectile visuals can be hidden inside Sandstorm;
- state code can accidentally clear Sandstorm/Dust Devil flags;
- charges/cooldowns can conflict with no-resource design if old logic remains.

Plan adjustment:
- use visually thinner heated-glass trail/impact;
- keep independent status/projectile state;
- add combo test while Sandstorm active and after Dust Devil.

Proof before complete:
- burn applies and Sandstorm/Dust Devil state remains correct.

### 28. Gem / Lapidary

Status: DATA-DRIFT and PROOF-FIRST.

Can Hytale do it: yes, but current data still describes self shield while concept
wants persistent recallable object with HP.

Expected complications:
- block cluster alone has no HP bar;
- proxy HP marker can become a living target/nameplate problem;
- 2x2x2 floating cube must not leave blocks behind;
- one-active-gem rule can fail on recast/disconnect;
- enemies may not target/attack block cluster naturally.

Plan adjustment:
- implement active gem state separate from self shield;
- use temporary green crystal block cluster plus non-hostile HP/nameplate proxy or
  entity UI;
- recast recalls/replaces old gem before placing new one.

Proof before complete:
- visible 2x2x2 green cube, HP/readout, one active only, recall/despawn clean.

### 29. Gem / Fracture

Status: DATA-DRIFT and PROOF-FIRST.

Can Hytale do it: yes as gem-centered expanding pulse, not as current generic
projectile line.

Expected complications:
- no active gem case needs clear behavior;
- expanding visual can desync from damage radius;
- allies/summons can get caught if filter not centralized;
- if gem moves/recalls during cast, pulse anchor must be stable.

Plan adjustment:
- require active Lapidary or approved fallback;
- snapshot gem location at cast;
- staged radius ticks apply damage when radius reaches targets;
- green expanding ring/sphere visual follows same tick schedule.

Proof before complete:
- visual originates at gem and hit timing matches expanding radius.

### 30. Gem / Refraction

Status: DATA-DRIFT and PROOF-FIRST.

Can Hytale do it: yes as a gem-anchored field/aura after Lapidary exists.

Expected complications:
- current data says self buff, not gem aura;
- aura can remain after gem despawns;
- if HP proxy/block cluster split exists, aura may anchor to wrong thing;
- shield/reflect mechanics need exact priority with other defenses.

Plan adjustment:
- tie aura lifetime to active Lapidary state;
- cleanup on gem recall/despawn/death/disconnect;
- log aura attach/refresh/remove and affected friendly/enemy entities.

Proof before complete:
- aura surrounds gem, not player; recall/despawn removes aura instantly.

## Shared Proof Tasks Required Before Full Terra Implementation

```
╔════╦══════════════════════════════╦══════════════════════════════════╗
║ ID ║ Proof Task                   ║ Unlocks                          ║
╠════╬══════════════════════════════╬══════════════════════════════════╣
║ P1 ║ Structured telemetry registry║ all 30                           ║
║ P2 ║ Friendly filter proof        ║ all negative effects             ║
║ P3 ║ Temporary terrain cleanup    ║ wall, pillar, flowers, fields    ║
║ P4 ║ Surface decoration anchor    ║ Rooted, Vines, Frolick, flowers  ║
║ P5 ║ First-class projectile proof ║ Magma, Bloom, Arbor, Sand        ║
║ P6 ║ Coating palette proof        ║ Metal, Stone, Magma, Petrify     ║
║ P7 ║ Harmless field/fluid proof   ║ Lava Pool, Obsidian, Mudpit      ║
║ P8 ║ Movement safety proof        ║ Stomp, Burrow, Tunnel, Dust Devil║
║ P9 ║ Persistent object + HP proof ║ Gem, sapling/flower lure objects ║
╚════╩══════════════════════════════╩══════════════════════════════════╝
```

## Implementation Warnings

1. Do not mark projectile abilities complete if they use living mobs as visual
   projectiles.
2. Do not mark fluid abilities complete if the caster is slowed, damaged, burned,
   or visually lit on fire by their own field.
3. Do not mark placement abilities complete if they replace support/floor blocks
   instead of placing on top.
4. Do not mark movement abilities complete from a stationary screenshot.
5. Do not mark Gem complete until Lapidary is a persistent object, not a self
   shield.
6. Do not mark Vines complete until recast and target-death cleanup are proven.
7. Do not mark Sand complete until Dust Devil consumes Sandstorm through the same
   cleanup path as manual deactivate/expiry.
8. Do not mark Obsidian Skin complete until repeated cold casts prove no client
   crash.
9. Do not rely on `/motm dev test ability` as the only proof for any ability that
   depends on movement, aiming, item use, toggles, or hit timing.

## Bottom Line

Every Terra ability is achievable or has a plausible Hytale-supported fallback.
The impossible-looking parts are not impossible; they are proof-gated:

- exact material coatings become tinted ModelVFX/EntityEffects;
- underground movement becomes guarded movement plus safe-exit resolver;
- lava/water fields become harmless-field proofs or visual fields with server
  mechanics;
- gem/flower/sapling objects become temporary block clusters plus non-hostile
  state/proxy/HP layers;
- projectiles must move from mob proxies to ProjectileModule or server-simulated
  non-living visuals.

The next coding move should be the proof layer, not another one-off ability fix.
