# Concept To Hytale Visual Bridge - 2026-05-22

## Why This Exists

The next quality gap is translating the user's official ability concepts into
Hytale-readable visuals. A cast log proves the code path ran, but it does not
prove the player sees Quake, Magma, Icicle, Smoke, Void, or any other style in a
recognizable way.

This bridge turns each ability into:

```text
official concept
  +-- style identity
  +-- Hytale asset family
  +-- motion shape
  +-- implementation bridge
  +-- proof requirement
```

## Source Order

```text
1. Protected style JSON
   +-- ability name, description, cast_type, target_type, trigger, terrain_effect

2. CODEX_REALIGNMENT_PLAN_2026-05-13.md
   +-- class/style palette
   +-- style feel
   +-- visual identity rule: color + particle family + motion pattern

3. CODEX_HYTALE_ASSET_API_KNOWLEDGE_2026-05-22.md
   +-- verified local assets
   +-- confirmed Hytale API surfaces
   +-- resolver assets missing from local Assets.zip: 0

4. Generated visual map
   +-- audits/concept-visual-map/latest/concept-visual-map.md
   +-- audits/concept-visual-map/latest/concept-visual-map.json
```

## Translation Shape

```text
Style identity
  +-- palette: what color family should dominate
  +-- asset family: which verified Hytale particles/models can carry it
  +-- feel: heavy, sharp, soft, toxic, divine, void, etc.

Ability shape
  +-- projectile_or_line       visible travel path + target impact
  +-- persistent_field         radius loop + tick pulses
  +-- ground_mark_or_barrier   telegraph + delayed hit or solid wall
  +-- movement_or_form         body trail + displacement/form proof
  +-- cone_or_gaze             forward fan/gaze from caster
  +-- summon_or_command        gate/nest/rise + persistent model
  +-- self_or_channel          third-person body aura/tint + status proof
  +-- jump_land                arm -> jump -> landing ring/cracks
```

## Immediate Rule

No future ability should be called visually accepted until its generated row has
been reviewed:

```text
1. Does the selected asset family match the style concept?
2. Does the motion shape match the ability description?
3. Does the implementation bridge exist in GameplayPlaybackManager / resolver?
4. Does the audit capture the readable moment?
```

If any answer is no, the result is `VISUAL REVIEW`, not `VISUAL PASS`.

## Examples

```text
Terra / Quake / Stomp
  concept: shockwave that damages and knocks back
  style feel: heavy tremor, cracked earth, dust shockwave
  asset family: mace ground-hit cracks + stone dust + Earth_Brazier_Glow
  motion: arm -> jump -> landing ring/cracks
  bridge: landing resolver + quake impact ring proxies

Hydro / Boiling / Geyser
  concept: erupt a geyser of scalding water
  style feel: steam pressure, scalding jets, hot water
  asset family: geyser water beams + smoke + fire/water mix
  motion: ground telegraph -> vertical eruption -> burn impact
  bridge: delayed ground_strike plus vertical field proxy

Aero / Jet / Afterburner
  concept: enhanced dash that leaves damage trail
  style feel: fast golden streaks, afterburn trails
  asset family: wind tail + sparks + fire smoke
  motion: body dash trail + lingering burn path
  bridge: movement proof + dash trail field ticks

Corruptus / Void / Rift
  concept: open a void rift that damages over time
  style feel: cosmic void, rift, consuming darkness
  asset family: Void_Sparks + VoidSmoke_Impact + VoidImpact
  motion: persistent field with pulling/tick pulses
  bridge: ActiveField tick + void field proxy
```

## Generated Map Script

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/generate-concept-visual-map.ps1 -NoTimestamp
```

The script reads all 40 styles and 120 abilities from protected JSON without
modifying them, then emits:

```text
audits/concept-visual-map/latest/
  +-- concept-visual-map.md
  +-- concept-visual-map.json
```

## How This Changes The Work

```text
Before
  +-- choose a generic class effect
  +-- cast ability
  +-- screenshot whatever happened

After
  +-- read concept row
  +-- choose verified asset family
  +-- implement motion shape
  +-- add proof log if missing
  +-- capture the readable moment
  +-- mark runtime / mechanical / visual separately
```

This should happen before the large visual identity pass. It can run in parallel
with Phase 6 spellbook work because it does not touch protected data.
