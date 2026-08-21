# Ability Review — Consolidated Index (2026-08-21)

Concept ↔ data ↔ visual-asset ↔ code audit of all 4 classes, 40 styles, 120 abilities.
Each ability answered six questions: what visual assets, why chosen, appropriate for the
action, what the code was supposed to do, does behavior match concept, do visuals match
concept. Per-class detail lives in the sibling reports.

- [Terra](terra-ability-review-2026-08-21.md)
- [Hydro](hydro-ability-review-2026-08-21.md)
- [Aero](aero-ability-review-2026-08-21.md)
- [Corruptus](corruptus-ability-review-2026-08-21.md)

## Scorecard (120 abilities)

| Class     | MATCH | PARTIAL | MISMATCH | Authored visual | Legacy (unauthored) |
|-----------|------:|--------:|---------:|----------------:|--------------------:|
| Terra     |     3 |      16 |       11 |               6 |                  24 |
| Hydro     |     1 |      22 |        7 |               3 |                  27 |
| Aero      |     6 |      13 |       11 |              14 |                  16 |
| Corruptus |     1 |      14 |       15 |               5 |                  25 |
| **TOTAL** | **11**| **65**  | **44**   | **28**          | **92**              |

Only ~9% of abilities are full concept/visual/behavior matches; 77% still render
unauthored vanilla-fallback visuals.

## Systemic findings (all 4 classes)

1. **Visual gap is the dominant issue — 92/120 (77%) manifest rows are `legacy:true`.**
   Those rows carry raw vanilla spawner paths. `AbilityRuntimeEffects.asRuntimeEffectId`
   accepts only `MOTM_*`/EntityEffect ids, so raw paths are dropped: the runtime falls
   back to a generic class/family effect or renders no themed visual. The manifest
   documents an intent, but no authored per-style VFX/color/sound/cleanup exists.
   Exception: Aero's **Scream** style has a runtime themed override that rescues its
   legacy rows.

2. **Mechanics silently unimplemented (root of the 44 mismatches).** Concept tokens are
   dropped by the runtime token router: `knockback` on many non-dash abilities,
   `damage_reduction`, `untargetable`, `sand_empower`, `lure`, execute thresholds, and
   ground-strike delay / physical-object spawns. The ability "works" but its defining
   behavior is missing.

3. **Numeric drift between data and canon/code.**
   - **Terra Sinkhole** — `dot_percent_per_second=2` is applied as a *fraction of max HP*,
     producing up to **200% max-HP suffocation pulses** instead of 2%. **(gameplay bug)**
   - **Terra Metal Coat** — 20% defense in code vs 50% promised.
   - **Aero Battle Cry** — +20% ATK / +25% speed, caster-only, vs canon +15%/+10% aura.
   - **Corruptus Life Drain** — 45% channel pulse + 20% lifesteal vs stated 50%.

4. **Even authored visuals reuse semantically-weak assets.** Terra Debris uses a Dash cast
   cue; Terra Magma travel uses `PrototypeBlockPlaceSuccess`; Aero Wind Blade uses the
   blue-white Tornado model instead of the pale-yellow arc canon; Corruptus Fire/Hell
   Flame authored cast effects are bypassed by generic Corruptus cast routing.

5. **Passive/visual wiring holes.** Terra cave-vision has no visible light path (the
   `TERRA_CAVE_VISION_LIGHT_ID` asset id was never wired — it was removed as dead code in
   the 2026-08-21 cleanup), and the stationary-shield loader has no matching Terra grant.

## Recommended priority order

- **P0 — gameplay bug:** fix Terra Sinkhole max-HP-fraction DoT (2% not 200%).
- **P1 — cheap data/code truth-ups:** Metal Coat 50%, Battle Cry values+aura, Life Drain
  50%; either implement or delete the unrecognized effect tokens so data matches behavior.
- **P1 — missing signature mechanics:** wire knockback routing, ground-strike delay/objects
  (Hydro), dash contact/launch/trail (Aero), execute thresholds (Corruptus), ally-support
  zones (Terra Lava Pool/Refraction, Hydro Rainbow, Corruptus Atonement).
- **P2 — the big content lane:** author the 92 legacy manifest rows into themed `MOTM_*`
  EntityEffects (per-class color identity already defined in `data/classes/*.json`). This
  is the largest remaining effort and the root cause of "visuals don't match intent."

## Method / integrity

Read-only investigation (no source/data edits, no build, no game launch during the audit).
Every verdict cites concrete evidence: manifest rows, EntityEffect ids, particle SystemIds,
hex tints, models, sound events, cast_types, and runtime families.
