# Terra Implementation Feasibility Review - 2026-05-22

## Purpose

This checkpoint reviews every Terra style approved in the concept pass and
separates:

```text
Can implement with already-proven MOTM runtime
Can likely implement, but needs a proof harness first
Needs fallback if Hytale does not expose the exact manipulation safely
Still has concept or safety gaps
```

Use this before implementing Terra or before continuing other class reviews. The
main lesson is that Terra needs object-first implementation, but object-first
does not mean "skip proof." Temporary block/fluid/prefab work must be proven in
a tiny isolated harness before it becomes ability runtime.

## Global Terra Implementation Shape

```text
Terra runtime families
+-- Temporary block/fluid placement
|   +-- Iron Wall
|   +-- Lava Pool
|   +-- Obsidian Skin shell
|   +-- Pillar Strike
|   +-- Mudpit
|   +-- Frolick flower trail
|   +-- Sapling / Nightshade markers
+-- Tight EntityEffect coating
|   +-- Metal Coat
|   +-- Alloy Enhancement fallback
|   +-- Obsidian Skin coating
|   +-- Rubble Rouser arms fallback
|   +-- Gargoyle / Glare
|   +-- poison / buried / rooted target states
+-- Model/proxy objects
|   +-- Magma Sling blob
|   +-- Cacti Cluster projectile
|   +-- Lapidary gem
|   +-- Vitrification shards
|   +-- Tunnel stone-block player proxy
+-- Movement/status runtimes
    +-- Burrow pop-down/pop-up dash
    +-- Rockslide dash
    +-- Dust Devil dash/drag/expel
    +-- Sinkhole buried status
    +-- Stomp/Aftershock/Quake field effects
```

## Proof Harness Required First

Before claiming any object-heavy Terra ability can be implemented exactly, add
isolated dev commands:

```text
/motm dev visual tempblock <itemAssetId> <seconds>
/motm dev visual tempfluid <fluidAssetId> <seconds>
/motm dev visual prefab <prefabPath> <seconds>
/motm dev visual modelproxy <modelOrRoleId> <seconds>
/motm dev visual coating <effectId> <seconds>
```

Acceptance for each:

```text
PASS
  +-- object/effect appears at controlled target position
  +-- screenshot confirms visual
  +-- collision works when the concept needs collision
  +-- owner/ally safety can be enforced
  +-- cleanup restores world/player state
  +-- logs contain no missing asset, missing role, or API errors
```

If a proof fails, the ability should use the fallback listed below and record the
residual in `CODEX_PHASE9_RESIDUALS.md`.

## Terra Style Feasibility Matrix

| Style | Ability | Feasibility | Concrete route | Gaps / proof needed |
| --- | --- | --- | --- | --- |
| Metal | Iron Wall | Likely exact, proof needed | Place four temporary `Metal_Iron` or `Metal_Iron_Smooth` blocks as a 2x2 barricade; track lifetime; apply knockback from wall contact zone; cooldown starts on cleanup. | Prove temporary block placement/removal and collision. Need confirm wall does not overwrite important blocks or trap allies. |
| Metal | Metal Coat | High | MOTM EntityEffect with tight gray top/bottom tint and no particles. | Need screenshot tuning for "solid metallic shiny" because tint may not create actual shine. |
| Metal | Alloy Enhancement | Partial exact, fallback likely | Restrict to physical melee/tools; first try held item tint/model VFX if Hytale supports it; fallback to tight hand/held-item proxy effect. | Need prove held-item-specific visual. If impossible, use hand/item-adjacent proxy and document limitation. |
| Magma | Lava Pool | Likely, high-risk proof | Expanding ring of temporary lava/fluid blocks; remove old ring as new ring expands; use lava particles as support; enforce friendly immunity. | Prove fluid placement, cleanup, no world grief, and owner/ally immunity. Fallback: animated lava particle/model ring. |
| Magma | Obsidian Skin | Likely exact enough | Brief temporary lava blocks around caster, then dark purple/black tight EntityEffect coating. | Prove shell placement does not trap/kill caster; coating screenshot. |
| Magma | Magma Sling | Likely with proxy | Use projectile visual proxy with lava/magma model or role if found; add lava bubble/smoke trail. | Need identify valid role/model. Fallback: particles plus model proxy with no block collision. |
| Stone | Rubble Rouser | Partial exact | Apply stone-arm visual if model override supports limb granularity; otherwise tight upper-body/arm EntityEffect plus stone impact particles on punches. | Need prove arm-only coating. If not possible, document upper-body fallback. |
| Stone | Pillar Strike | Likely exact, proof needed | Place temporary `Rock_Stone_Brick_Pillar_Base/Middle` blocks beneath grounded targets, apply vertical launch, cleanup after short duration. | Prove block placement under NPCs and cleanup. Need avoid placing through ceilings or protected blocks. |
| Stone | Rockslide | High | Use existing dash/velocity runtime, stone/rubble trail, path knockback, temporary immunity flag. | Need verify immunity window and path hit detection while moving. |
| Arbor | Rooted | Medium | Apply root/vine lower-body EntityEffect or prefab/model proxy at feet; root caster; heal and reflect damage. | Need prove lower-body/ground attachment. Fallback: root prefab at player feet plus body tint. |
| Arbor | Vines | Medium | Persistent single-target root/vine effect; clear old target on retarget or death; no cooldown. | Need death/retarget cleanup proof. Need target-friendly filtering. |
| Arbor | Sapling | Likely, proof needed | Projectile ray ignores enemies and resolves to ground; place/spawn `Plant_Sapling_Oak` or sapling model at impact; object taunts enemies. | Need ground-hit raycast behavior and taunt object state/HP. |
| Bloom | Nightshade | Likely, proof needed | Projectile passes through enemies, lands on surface, places purple/poison flower marker, lures enemies within 5 blocks, then poison burst. | Need pass-through projectile + surface impact proof; lure AI may need threat/taunt workaround. |
| Bloom | Frolick | Likely, proof needed | While moving, place temporary existing flower assets behind caster; allies get speed, enemies get stun. | Need temporary flower placement density/cleanup and ally/enemy filtering. |
| Bloom | Cacti Cluster | Medium | Large cactus model projectile proxy sticks to first enemy/surface; attached target receives DoT/slow; delayed visual explosion spreads DoT/slow to nearby enemies. | Need sticky projectile sync to moving target. Fallback: stationary cactus at hit position with attached-target status. |
| Self Petrification | Gargoyle | High | Tight gray stone EntityEffect coating; invulnerable, immobile, action-locked, untargetable; 6s cooldown after end/cancel. | Need prove untargetable can be represented; if not, use invulnerable + AI target filtering. |
| Self Petrification | Glare | High | Forward cone target select; same stone coating on mobs; release clears stone, then 2s slow. | Need cone targeting and cancel/release cleanup. |
| Self Petrification | Tunnel | Hardest | Player becomes singular stone-block visual/proxy; controlled terrain traversal consuming stone; can start from Gargoyle; auto-surface recovery if resource ends underground. | Needs dedicated tunnel safety harness. True through-terrain movement may be constrained; fallback is phased/teleport stepping with collision suppression and forced surface exit. |
| Soil | Burrow | Medium-high | Short scripted pop-down/pop-up dash: hide/stone-dust caster briefly, move 4 blocks forward, reappear, apply exit AoE damage/knockback. | Need safe destination selection; avoid emerging inside blocks or off cliffs. |
| Soil | Mudpit | Likely, high-risk proof | Expanding brown-tinted water/fluid block field; enemy debuffs; caster/allies/summons walk normally; counts as water for beneficial interactions. | Prove water tinting and no movement tax for allies. If block tint unavailable, use water field + brown smoke overlay. |
| Soil | Debris | High | Forward-moving brown smoke/debris wave made from smoke + dirt/stone break particles; applies confuse/untargetable debuff and vulnerability. | Need create/readable wave shape, not just scattered dust. |
| Sand | Sandstorm | High | Player-attached beige-yellow smoke/sand cloud radius using Sand_Storm and tinted smoke; tick damage/slow/damage-down to enemies only. | Need visibility tuning so cloud reads radius but does not blind testing. |
| Sand | Dust Devil | Medium-high | While Sandstorm active, dash forward with same cloud, drag enemies caught in cloud, expel at dash end, deactivate Sandstorm. | Need drag/expel proof and ensure Vitrification can remain layered. |
| Sand | Vitrification | Medium | Track five shard charges; show floating glass/crystal shard proxies; primary attacks fire one shard each; can overlap Sandstorm/Dust Devil. | Need primary attack interception and persistent floating shard visuals. |
| Gem | Lapidary | Medium | Persistent controllable object with HP; preferred green gem/crystal; fallback floating 2x2 `Rock_Crystal_Green_Block`/`Rock_Gem_Emerald` cluster one block above ground; recall vs destroyed resource accounting. | Need HP bar/display and recall command/input. Need decide if 2x2 block cluster has collision or model proxy only. |
| Gem | Fracture | Medium-high | Expanding sphere/ring from Lapidary epicenter; damage falloff; no caster/allied damage; consumes gem investment and starts Lapidary cooldown. | Need staged expanding visual and ticked damage wave. |
| Gem | Refraction | Medium | Bright radius sphere/aura around Lapidary, green preferred; buff caster; doubled effects if Lapidary active. | Need radius-sphere visual around gem object, not caster. |
| Quake | Stomp | Already partially proven | Use existing armed jump -> landing runtime; keep flash + crack impact ring; enemy knock-up/away and damage. | Re-test with target hits after any refactor. |
| Quake | Aftershock | Medium-high | Idle/still caster; 8-block spherical radius; quake flash/crack/tremor field; staged slow/disoriented/vulnerability. | Need distinguish intentional idle from input bug; prove spherical radius. |
| Quake | Sinkhole | Already partially proven | Use buried-look EntityEffect/status, suffocation ticks, ground crack markers from repeated block-break/crack effects plus brown dust. | Need verify cleanup/release and visual marker per buried target. |

## Cross-Cutting Gaps To Close Before Terra Implementation

```text
1. Temporary block/fluid lifecycle
   +-- Needed by Metal, Magma, Stone, Soil, Arbor, Bloom, Gem
   +-- Must preserve original block state and restore it
   +-- Must avoid overwriting important blocks

2. Friendly filtering
   +-- Needed by every AoE/field/explosion
   +-- Caster, allies, allied summons, converted mobs are never harmed/debuffed

3. Ownership and cleanup
   +-- Every temporary object must know owner, style, ability, expiry, and cleanup action
   +-- Cleanup on logout/death/world change/recast/cancel

4. EntityEffect granularity
   +-- Need prove whether we can tint specific limbs or held items
   +-- If not, use model/proxy fallback and document limitation

5. Visual proof standards
   +-- Third-person screenshots for coatings and self states
   +-- Movement video/screenshot sequence for dashes/tunnel/burrow
   +-- Log proof for mechanics plus screenshot proof for visual read
```

## Recommended Next Engineering Step

Do not start full Terra implementation yet. First implement and test the proof
harness in this exact order:

```text
P0. EntityEffect coating proof
    +-- stone body
    +-- metal body
    +-- dark obsidian body
    +-- target poison smoke

P1. Temporary block proof
    +-- place/remove Metal_Iron 2x2 wall
    +-- place/remove Rock_Stone_Brick_Pillar_Base/Middle
    +-- place/remove Plant_Flower_Common_Purple
    +-- place/remove Plant_Sapling_Oak

P2. Temporary fluid proof
    +-- place/remove Fluid_Lava or Lava_Source
    +-- place/remove water field
    +-- test brown tint or brown overlay fallback

P3. Model/proxy proof
    +-- cactus projectile proxy
    +-- magma blob proxy
    +-- floating crystal/gem proxy
    +-- glass shard charge proxy

P4. Movement safety proof
    +-- Burrow pop-down/pop-up safe destination
    +-- Tunnel underground traversal and forced surface recovery
    +-- Dust Devil drag/expel
```

After P0-P4, we can mark each Terra ability as:

```text
Exact route confirmed
Exact route impossible, fallback confirmed
Blocked pending user decision
```

That result should then guide Hydro/Aero/Corruptus reviews so we do not keep
approving visuals that the current Hytale build cannot faithfully support.
