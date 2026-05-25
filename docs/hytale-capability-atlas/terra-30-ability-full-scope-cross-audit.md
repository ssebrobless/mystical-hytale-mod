# Terra 30-Ability Full-Scope Cross-Audit

Date: 2026-05-24

Purpose: cross-check the Terra implementation map against the concept decisions,
current `terra_styles.json`, local Hytale research, and the in-game failures we
already observed. This document is the implementation guardrail before the next
Terra pass.

## Verdict

The current Terra map is directionally correct: abilities should be built from
Hytale's real primitives instead of particle-only approximations. The path needs
one correction: shared proofs must happen before style polish.

```
╔══════════════════════════════════════════════════════════════════════╗
║                    IDEAL TERRA IMPLEMENTATION ORDER                 ║
╠═════════════════════════════════╦════════════════════════════════════╣
║ 1. Shared proof layer           ║ telemetry, cleanup, friendly skip  ║
║ 2. Placement layer              ║ surface objects, blocks, fields    ║
║ 3. Motion layer                 ║ projectiles, dashes, safe exits    ║
║ 4. Visual identity layer        ║ coatings, decals, trails, sounds   ║
║ 5. Ability layer                ║ 30 abilities consume the above     ║
║ 6. Acceptance layer             ║ normal controls + logs + review    ║
╚═════════════════════════════════╩════════════════════════════════════╝
```

The most important implementation rule is now:

```
Concept intent
  └─▶ Hytale primitive
      └─▶ local asset/API proof
          └─▶ small runtime proof
              └─▶ ability implementation
                  └─▶ structured evidence + user visual pass
```

## Global Gates

Every Terra ability must pass these gates unless it is pure self-only benefit.

| Gate | Requirement | Why it matters |
|---|---|---|
| G0 Telemetry | Emit cast, target, hit, skip, visual, cleanup, and state logs | Screenshots cannot prove mechanics |
| G1 Friendly safety | Caster, allies, allied summons/minions are never harmed, slowed, rooted, debuffed, displaced, or burned | User-approved global rule |
| G2 Cleanup registry | Every block, fluid, proxy, effect, field, and toggle has deterministic cleanup | Prevents lingering lava/walls/trails |
| G3 Normal controls | Test with intended spellbook/hotbar/LMB/RMB/player actions, not only dev commands | Proves real playability |
| G4 Third-person review | Visual abilities get third-person screenshots/video or live review | User must judge style read |
| G5 Cold crash proof | High-risk geometry/projectiles run repeated cold tests | Prevents client index crashes |

## Primitive Fit Audit

| Primitive | Fit | Use For | Risk | Decision |
|---|---:|---|---|---|
| Temporary blocks | High | Iron Wall, Pillar Strike, flowers, saplings, gem cube, cactus marker | Medium cleanup risk | Use as primary for physical objects |
| Surface placement | High | Rooted, Vines, Frolick, Sapling, Nightshade | Medium anchoring risk | Centralize before style work |
| EntityEffect + ModelVFX + tint | High | Metal Coat, Alloy, Gargoyle, Glare, Obsidian | Medium color/isolation risk | Use for coatings; no smoke-only coatings |
| ProjectileModule / first-class projectile | Medium-High | Magma Sling, Cacti, Sapling, Nightshade, Vitrification | API/config proof needed | Prove before final projectile abilities |
| Server-sim projectile + visual | Medium | Pass-through seeds, slow cactus, debris wave | Hit/visual drift risk | Fallback only with strong logs |
| Temporary fluids | High visual, high risk | Lava Pool, Mudpit, Obsidian shell | Native slow/damage/fire/crash risk | Proof-gate; fallback to lava-like blocks/visual field |
| Trails/particles | Medium | Sandstorm, Debris, Rockslide, projectile streaks | Tint/readability varies | Support the main object/action |
| BlockBreakingDecal | Candidate | Quake cracks, Sinkhole marks | Spawn API unproven | Optional proof; particle cracks remain fallback |
| Velocity/status movement | Medium | Burrow, Dust Devil, Stomp, knockbacks | Player control edge cases | Use with before/after logs |
| Teleport/position movement | High risk | Tunnel exit, emergency unstuck | Client stability risk | Only guarded with safe-exit resolver |
| Proxy/NPC object | Medium | Lapidary HP marker, lure markers, summons | Health bar/AI mistakes | Must be non-hostile or intentional |

## Data Drift To Fix Surgically

These are not implementation failures; they are current data/plan differences
that must be reconciled with surgical edits when coding resumes.

| Area | Current Drift | Required Direction |
|---|---|---|
| Quake / Sinkhole | Data still includes `vertical_displace_blocks` | Use buried-look, not physical lowering |
| Metal / Iron Wall | Data `target_type` still says `line`/`range` | Runtime should be player-forward with one block gap |
| Magma / Lava Pool | Data says `ground_target` | Runtime should originate from caster body |
| Stone / Pillar Strike | Data height is `5` | Concept wants 1x1x3 staged stone pillar |
| Arbor / Vines | Data cooldown is `4` | Concept wants no cooldown, one target at a time |
| Arbor / Sapling | Data says summon treant | Concept wants ground marker sapling/lure object |
| Gem / Lapidary | Data says self shield | Concept wants persistent recallable gem object |
| Gem / Fracture | Data says projectile line | Concept wants gem-centered expanding explosion |
| Gem / Refraction | Data says self buff | Concept wants gem-centered aura/shield field |

## Per-Ability Cross-Audit

### 1. Quake / Stomp

Visual: armed jump followed by landing flash, shockwave, crack/dust ring.

Function: LMB arms the next jump/landing. Landing near enemies fires AoE,
damages/knocks enemies, skips caster/allies/summons, then clears armed state.

State/cancel: armed state expires or clears after landing. It must not fire while
standing still.

Implementation route: keep armed-stomp state, use landing event/ground check,
spawn Quake impact ring, query radius at landing position, log `targets>=1`.

Proof: normal LMB + jump + land near a grounded target, not dev-only forced hit.

### 2. Quake / Aftershock

Visual: self-centered 8-block spherical tremor with the same flash/crack family
as Stomp.

Function: enemies inside radius receive slow/disorient; enemies outside do not.
Caster/allies/summons are skipped.

State/cancel: short cast then field or pulse duration; cleanup visual after end.

Implementation route: immediate self-centered AoE plus optional short field
visual; keep radius at 8.

Proof: one target inside, one outside, structured hit/skip logs.

### 3. Quake / Sinkhole

Visual: target looks buried by dark/lower-body effect, root, stone dust, crack
marker, and brown dust at the target location.

Function: targeted enemy is rooted/controlled, suffers suffocation ticks if
specified, then releases cleanly. Do not lower the target through the ground.

State/cancel: one buried-victim state per cast target; release on expiry, death,
disconnect, or cleanup sweep.

Implementation route: buried-look EntityEffect plus status/root and ground crack
marker. Use BlockBreakingDecal only after proof; otherwise particle cracks.

Proof: engage/tick/release logs for same target id and visible location marker.

### 4. Metal / Iron Wall

Visual: 3x3 wall of metal/iron blocks one block in front of player, full width
facing the player.

Function: wall blocks line/pathing, lasts 4 seconds, heals if intended, pushes
enemies away if the spawn would overlap or they are too close. No body coating.

State/cancel: cooldown begins when wall disappears; cleanup restores all blocks.

Implementation route: player-forward vector placement, clamp near caster, save
original block states, push enemy overlap opposite the wall/caster direction.

Proof: cast while facing multiple compass directions; wall always spawns near,
aligned, restores, and push logs match observed movement.

### 5. Metal / Metal Coat

Visual: strong, visible dark gray metallic coating hugging body; held item may
share the effect if that preserves the approved look.

Function: self defensive buff only. It should not apply weapon follow-up text or
Alloy-only damage behavior.

State/cancel: apply on cast, remove on duration end/death/style swap/disconnect.

Implementation route: approved coating route with EntityEffect/ModelVFX/tint,
using the Metal palette and no particle-only smoke as the main visual.

Proof: third-person visual remains dark and present for full duration; logs show
apply/remove and no unintended Alloy state.

### 6. Metal / Alloy Enhancement

Visual: eligible weapon/tool gets metal coating; swing/impact frames flash dark
gray/metallic enough to read.

Function: physical melee weapons/tools only. First eligible action binds the
buff to that item. Next 3 uses are boosted/no durability. Swap after binding
ends the buff. Ranged/magic/spellbook actions are rejected.

State/cancel: clear on three charges spent, item swap, expiry, death, or style
swap.

Implementation route: item-bound buff state plus SwitchActiveSlotEvent cleanup,
hit/use hooks for melee/tool actions, and impact-frame VFX.

Proof: damage numbers show exactly 3 boosted uses, fourth normal, swap clears.

### 7. Magma / Lava Pool

Visual: visible lava pool/ring on the ground from the caster body.

Function: enemies burn/are affected. Caster/allies/summons are not slowed,
damaged, burned, or lit on fire. Cleanup is reliable.

State/cancel: field state owns all placed visuals/effects and ends after
duration.

Implementation route: prove harmless fluid first. If real lava cannot be made
safe, use lava-looking temporary blocks/visuals plus server-side enemy field.

Proof: caster walks through own pool normally with no damage/fire; enemy takes
field effect; logs show all terrain restored.

### 8. Magma / Obsidian Skin

Visual: first phase is a lava shell/box around player; second phase is a dark
midnight purple-black stone-skin-like coating.

Function: lava phase immobilizes player, then grants shield/reduction. It must
not crash the client.

State/cancel: root during lava phase only, shell cleanup before coating phase,
coating cleanup on duration end.

Implementation route: bounded camera-safe shell proof. If full shell keeps
crashing, use safer partial shell/ring with the same read. Apply Obsidian palette
coating after shell cleanup.

Proof: five repeated casts from cold launch; no index crash; distinct lava and
obsidian phases.

### 9. Magma / Magma Sling

Visual: aimed lava blob projectile with fire/lava trail, no mob health bar.

Function: projectile travels along aim, hits enemy/surface, applies burn/slow,
then despawns.

State/cancel: projectile expires at range/time and cleans visual proxy/trail.

Implementation route: first-class ProjectileModule/config first. If visuals are
not enough, attach non-living visual/trail to projectile path; do not spawn a
living NPC as the projectile.

Proof: visible path aligns with aim, hit logs match visual, no nameplate/AI.

### 10. Stone / Rubble Rouser

Visual: stone-coated arms if possible; fallback is strong stone body/held-item
coating plus stone impact frames.

Function: melee/unarmed/stone strike buff or attack deals damage/knockback as
specified, enemies only.

State/cancel: buff charges/duration clear on expiry, action spend, swap if
bound, death, or style change.

Implementation route: gameplay buff with stone coating; research arm-only VFX
only if local API exposes attachment-specific effects.

Proof: target is hit/knocked, visual reads stone not metal/obsidian.

### 11. Stone / Pillar Strike

Visual: 1x1x3 stone block pillar appears under target in rapid upward stages.

Function: target at pillar location is damaged/launched/stunned. Allies/summons
skipped.

State/cancel: staged block placement and timed cleanup restore all blocks.

Implementation route: target ground anchor, staged temporary stone column, pulse
launch/stun at start or completion.

Proof: user sees three-block pillar stack quickly and target launches/stuns.

### 12. Stone / Rockslide

Visual: forward-moving rocky debris wave, not invisible damage.

Function: hits enemies in path/area with damage/slow/control; outside targets
skipped.

State/cancel: line/field sweep owns all temporary markers and expires.

Implementation route: server-side line sweep with stone dust, block-break
particles, optional rubble nodes/trail.

Proof: visible forward wave and path-based hit logs.

### 13. Arbor / Rooted

Visual: vines/roots at player's legs/lower body placed on top of the ground.
No text label and no replacement of floor block.

Function: self root/heal/regeneration/defense as designed; no negative spillover.

State/cancel: rooted state can end on expiry/cancel/death; surface markers clean.

Implementation route: centralized surface-decoration anchor plus self status and
optional root EntityEffect.

Proof: ground remains intact and roots sit above support block.

### 14. Arbor / Vines

Visual: vine/root effect on the current target.

Function: no cooldown; one target at a time. Recasting on target B releases A.
Target death clears state. Enemy takes intended control/damage.

State/cancel: active target table keyed by caster; cleanup on recast/death/expiry.

Implementation route: target status/effect plus `activeVinesByPlayer` state and
EntityRemoveEvent/death cleanup.

Proof: A releases when B is rooted; death clears state and visuals.

### 15. Arbor / Sapling

Visual: projectile marks ground, then a visible tree sapling appears on top of
that ground point.

Function: projectile should not directly damage enemies. Sapling lures/taunts
nearby enemies as a ground object.

State/cancel: marker/lure field expires or cleans on recast/death/world cleanup.

Implementation route: pass-through/ground-impact projectile or server ray to
surface; surface-place sapling block/proxy; lure field around marker.

Proof: enemy in the shot path does not stop it; sapling appears on ground.

### 16. Bloom / Nightshade

Visual: seed projectile passes through enemies, creates flower on surface, then
light purple body-hugging poison effect on afflicted targets.

Function: flower lures enemies within 5 blocks before poison explosion. Damage
moment is explosion, not direct projectile hit.

State/cancel: flower/lure/explosion pipeline owns marker and poison states.

Implementation route: server-sim pass-through projectile until surface impact,
surface flower marker, lure field, delayed explosion and poison DoT.

Proof: projectile passes through an enemy, flower anchors to surface, lure and
poison logs fire.

### 17. Bloom / Frolick

Visual: flowers appear behind the moving player on top of ground, never replacing
the block under the player.

Function: self heal/speed/attack buff while moving. Trail is visual/supporting
identity and must not push player underground.

State/cancel: active movement sampler, spacing throttle, owned flower cleanup.

Implementation route: sample movement, compute behind-player surface anchors,
place flower decorations above support block.

Proof: movement test in a safe lane leaves flowers behind player and no terrain
damage.

### 18. Bloom / Cacti Cluster

Visual: slow large cactus-like projectile sticks to enemy or surface, then
visually bursts after 4 seconds.

Function: attached enemy takes 4s DoT equal to 5% caster max HP and 20% slow.
Explosion does not double-hit attached target but applies same DoT/slow to
nearby enemies within 4 blocks without attaching more cacti.

State/cancel: attached cactus state stores target/surface anchor, expiry, and
secondary targets.

Implementation route: first-class or server-sim projectile with cactus visual,
attached marker/effect, delayed AoE excluding the attached target from extra
burst.

Proof: one direct target and surrounding targets prove initial and secondary
DoTs separately.

### 19. Self Petrification / Gargoyle

Visual: tight stone coating hugging the player model.

Function: self heal/shield/stone form as designed. Can end naturally or manually.
Cooldown is 6 seconds and starts only after end/cancel.

State/cancel: toggle/duration state with explicit `endGargoyle` cleanup.

Implementation route: StyleManager toggle plus stone ModelVFX/EntityEffect and
post-end cooldown start.

Proof: manual cancel and natural expiry both start 6s cooldown and remove coating.

### 20. Self Petrification / Glare

Visual: target receives same tight stone coating while petrified, then coating
vanishes.

Function: target is petrified/rooted/stunned for duration. After release, target
remains slowed for 2 seconds. Allies/summons skipped.

State/cancel: target state tracks petrify phase and slow-tail phase.

Implementation route: target EntityEffect/status, scheduled release, delayed
slow cleanup.

Proof: target visibly stone, then normal but slowed; logs show phases.

### 21. Self Petrification / Tunnel

Visual: player reads as a singular stone block/form while moving through/under
terrain.

Function: no resource cost. Duration-based. Can be used with Gargoyle. On end,
player exits to safe surface or valid cave air pocket and never remains stuck.

State/cancel: duration state with safe-exit resolver; manual cancel if supported.

Implementation route: implement surface-recovery first. Add underground/cave
movement only after a proof shows safe position sampling and exit.

Proof: repeated casts in lanes and near terrain, Gargoyle combo, exit reason log.

### 22. Soil / Burrow

Visual: whack-a-mole dash: player dips/down cue, travels forward 4 blocks, then
erupts from ground with dirt/stone burst.

Function: evasion/dash movement; damage/knockback happens at re-emerge point.

State/cancel: short movement state with edge/platform safety and exit AoE.

Implementation route: velocity/teleport guarded dash with entry/exit visual cues
and before/after position logs.

Proof: moves about 4 blocks forward, exit AoE hits, no platform fall.

### 23. Soil / Mudpit

Visual: expanding muddy brown water-like ground pool/field.

Function: enemy debuff field; counts as water for future Hydro interaction.
Caster/allies/summons move normally and are not slowed/damaged/debuffed.

State/cancel: field state owns visuals, debuff ticks, and cleanup.

Implementation route: prove safe water/fluid first. If fluid slows allies,
server-side field plus visual muddy overlay is preferred.

Proof: caster walks normally, enemy is debuffed, visual reads mud.

### 24. Soil / Debris

Visual: brown dust/debris wave traveling forward, using dirt/stone break effects
and possibly trails; not a thrown dirt block.

Function: enemies in path receive vulnerability/blind/damage; allies skipped.

State/cancel: timed line sweep with visual node cleanup.

Implementation route: line sweep mechanics plus particles/trails for a moving
debris front.

Proof: path hit logs, outside target skip, visible forward dust wave.

### 25. Sand / Sandstorm

Visual: beige-yellow sand cloud radius around/following the player, not plain
white smoke.

Function: no resources. Toggle lasts 10 seconds, manual deactivate allowed, 2s
cooldown after expiry/deactivate. Enemy-only ticks.

State/cancel: active follow-field toggle; deactivate on duration, manual input,
Dust Devil consume, death, style swap.

Implementation route: StyleManager toggle plus owner-following field. Prefer
trail/sand particles/block markers if smoke tint remains weak.

Proof: 10s expiry, manual deactivate, 2s cooldown, enemy hits, friendly skips,
visual sand color.

### 26. Sand / Dust Devil

Visual: player dashes with Sandstorm/tornado cloud, dragging enemies, then
expelling them at the end.

Function: only usable while Sandstorm is active. Fails clearly if inactive.
Consumes/ends Sandstorm after dash/expel.

State/cancel: precondition checks active Sandstorm; dash owns pull/expel and
Sandstorm cleanup.

Implementation route: active-toggle precondition, velocity/controlled dash, pull
field during dash, final knockback and `endSandstorm`.

Proof: inactive failure, active dash, target drag/expel, Sandstorm ends.

### 27. Sand / Vitrification

Visual: superheated sand/glass/burn effect that layers with Sandstorm instead
of hiding it.

Function: enemy burn/damage projectile or targeted effect; does not cancel
Sandstorm or Dust Devil combo flow.

State/cancel: projectile/status cleanup independent from Sandstorm state.

Implementation route: glass/spark projectile/trail with enemy burn status.

Proof: cast while Sandstorm active and during combo tests without state conflict.

### 28. Gem / Lapidary

Visual: persistent floating 2x2x2 green gem/crystal cube one block above ground,
unless a better local green gem asset is proven. Needs readable HP.

Function: controllable/recallable object, one active gem per player. Recast
recalls/replaces old gem. It can be damaged and tracked.

State/cancel: active gem state stores location, HP, owner, proxy/block ids, and
dependent Fracture/Refraction state.

Implementation route: temporary green crystal block cluster plus proxy/nameplate
or entity UI for HP. Recast performs cleanup before spawn.

Proof: visible cube, HP readout/bar, recall/despawn logs, one active only.

### 29. Gem / Fracture

Visual: fast expanding green sphere/ring from the Lapidary gem epicenter.

Function: damage comes from gem-centered expanding pulse. Caster/allies/summons
are skipped. No gem should fail clearly or use an approved fallback.

State/cancel: requires active gem; staged pulse owns radius ticks and visuals.

Implementation route: expanding AoE tick from gem location with green ring/sphere
visual and distance-based hit timing.

Proof: visual starts at gem, not player; hits occur as radius reaches enemies.

### 30. Gem / Refraction

Visual: bright green aura/sphere around the Lapidary gem showing active radius.

Function: gem-anchored shield/reflect/buff field according to final tuning. It
cleans when gem disappears.

State/cancel: aura tied to active gem state and removed on recall/despawn/expiry.

Implementation route: gem-anchored field visual plus server-side shield/refraction
state.

Proof: aura surrounds gem, not player; recall/despawn removes aura.

## Best Implementation Path From Here

```
╔═══════════════════╦══════════════════════════════════════════════════╗
║ Step              ║ Why It Comes First                              ║
╠═══════════════════╬══════════════════════════════════════════════════╣
║ Telemetry/cleanup ║ Every later proof needs reliable text evidence   ║
║ Friendly filters  ║ Prevents repeating caster/ally harm bugs         ║
║ Surface anchors   ║ Fixes Rooted, Frolick, flowers, saplings, gems   ║
║ Projectile proof  ║ Fixes Magma Sling and unlocks four more styles   ║
║ Coating palette   ║ Locks Metal, Stone, Obsidian, Petrification      ║
║ Safe fields       ║ Resolves Lava Pool, Obsidian shell, Mudpit       ║
║ Movement safety   ║ Resolves Stomp, Burrow, Tunnel, Dust Devil       ║
║ Persistent object ║ Resolves Gem and lure objects                    ║
╚═══════════════════╩══════════════════════════════════════════════════╝
```

After this shared layer, the style implementation order should be:

1. Metal, because it proves coating, item-bound buffs, and temporary wall blocks.
2. Quake, because it reuses movement/hit telemetry and crack effects.
3. Arbor and Bloom, because they stress-test surface placement and projectiles.
4. Magma and Soil, because they need the safe-field decision.
5. Sand, because it needs the movement and trail/sand visual decisions.
6. Self Petrification, because Tunnel should wait for movement safety.
7. Gem, because it depends on persistent object and HP UI behavior.

## Remaining Research/Proof Questions

| Question | Blocking Abilities | Proof Needed |
|---|---|---|
| Which local projectile config gives a real lava/blob path without mob behavior? | Magma Sling, Vitrification, Cacti, Sapling, Nightshade | `motm dev proof projectile` |
| Can real fluids be made harmless to caster/allies? | Lava Pool, Obsidian Skin, Mudpit | `motm dev proof harmless-fluid` |
| Can BlockBreakingDecal be spawned from plugin code? | Stomp, Aftershock, Sinkhole | `motm dev proof ground-decal` |
| Can arm-only/item-only coatings be isolated? | Rubble Rouser, Alloy | `motm dev proof coating-targets` |
| Can Tunnel safely exit cave air pockets, not only surface? | Tunnel | `motm dev proof tunnel-exit` |
| What HP UI/proxy path works for a block-cluster object? | Lapidary, Refraction, Fracture | `motm dev proof persistent-object-hp` |

## Completion Standard

Terra is not complete when each ability merely "does something." Terra is
complete when each ability:

- matches the concept above;
- uses the intended player controls;
- has no resource cost;
- skips caster/allies/allied summons for all negative effects;
- emits structured proof logs;
- cleans all temporary state;
- uses object/block/coating/projectile visuals where the concept calls for them;
- passes third-person visual review.
