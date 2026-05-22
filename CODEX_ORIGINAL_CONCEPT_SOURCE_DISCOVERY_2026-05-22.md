# Original Concept Source Discovery - 2026-05-22

## Source Shape

```text
Original Hytale concept
  ├─ motm-hytale-extract/original-concept/MOD_DESIGN.md
  │    └─ richest known source for class/style/ability mechanics
  │
  ├─ motm-hytale-extract/original-concept/CLASS_STYLES_SUMMARY.md
  │    └─ compact roster/index of 40 styles and 120 abilities
  │
  ├─ MOTM_CONCEPT_ARCHIVE.zip
  │    ├─ original-concept/
  │    └─ hytale-mod/
  │
  ├─ mentees-of-the-mystical-terminal/
  │    ├─ CONCEPT_ARCHIVE.md
  │    ├─ godot-terminal/data/parity/classes.json
  │    └─ mentees_of_the_mystical/data/classes.py
  │
  ├─ mentees-of-the-mystical-godot/
  │    ├─ SPECIFICATION.md
  │    └─ data/classes.json
  │
  └─ current Mystical-Hytale-Mod/
       └─ src/main/resources/data/styles/*_styles.json
```

## Key Finding

The original Hytale concept is much richer than the current Hytale style JSON.

```text
Original MOD_DESIGN.md
  └─ exact range, radius, duration, cooldown, charges, requirements,
     resource costs, conditional interactions, toggles, recasts,
     summon behavior, form behavior, and style synergies

Current style JSON
  └─ playable compressed profiles: short description, damage/effect,
     cast_type, target_type, rough range/radius/duration
```

Generated comparison:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/extract-original-concept-abilities.ps1 -NoTimestamp
```

Output:

```text
audits/concept-source-discovery/latest/
  ├─ original-concept-ability-comparison.md
  └─ original-concept-ability-comparison.json
```

Current comparison result:

```text
Original ability rows found:       120
Current ability rows found:        120
Name mismatches:                     2
Compressed current descriptions:   120
```

The two name mismatches are not concept conflicts:

```text
Hydro / Vapor / slot 3
  Original: Hidroses
  Current:  Hidrosis
  Meaning:  spelling/normalization difference

Corruptus / Primordial / slot 2
  Original: Triceratops Form / Mosasaurus Form
  Current:  Triceratops Form
  Meaning:  current Hytale scope lost the water-form branch
```

## What We Would Lose If Current JSON Wins

Examples of concept detail currently compressed or missing:

| Style | Original concept detail at risk |
| --- | --- |
| Terra/Metal | Iron Wall is a short-lived physical wall 2 blocks taller than player; Metal Coat consumes matching ingots; Alloy Enhancement enlarges held tool/weapon. |
| Terra/Magma | Lava Pool is an expanding ring from the player that pulls enemies with it; Obsidian Skin is a temporary lava cube with self-burn, strong shield, movement/jump penalties. |
| Terra/Sand | Sandstorm is a toggle attached to the player and consumes sand; Dust Devil requires active Sandstorm; Vitrification turns sand into five glass shards fired by future primary attacks. |
| Terra/Gem | Lapidary requires emeralds, summons a taunting floating gem, and can be recalled; Fracture depends on Lapidary; Refraction doubles if Lapidary is active. |
| Hydro/Rain | Rainbow automatically triggers when Piercing Rain ends; it is not simply another manually cast support field. |
| Hydro/Saltwater | Tide Pool is a mount-like water orb; Abyssal Assist and Rip Current require Tide Pool active and chain with it. |
| Aero/Tornado | Twister absorbs projectiles and powers Funnel Cloud debris count. |
| Aero/Jet | Afterburner modifies the next Jet Burst; Mach Punch only exists after Jet Burst and has wall-collision logic. |
| Corruptus/Flame | Fireball/Ignite create stacking burns; Combust consumes remaining burn damage and can spread ignition on kill. |
| Corruptus/Mentokinesis | Dominate converts enemies; Mind Shatter depends on a dominated/friendly minion; Hivemind links dominated enemies and minions. |
| Corruptus/Primordial | Slot 2 originally branches between Triceratops on land and Mosasaurus in water. |

## What We Gain By Using Original MOD_DESIGN As Concept Authority

- Ability implementation targets become much more precise.
- Harness scenarios become obvious: some abilities require movement, follow-up attacks, active previous ability state, summons, toggles, recasts, or environmental conditions.
- Spellbook descriptions can be made honest to the real intended fantasy.
- We stop treating simplified 2D/Godot/current JSON descriptions as the full concept.

## What It Costs

- More mechanics are incomplete than earlier plans implied.
- Some current code and JSON values are approximations and must be reconciled.
- Some original mechanics may need Hytale-friendly approximations rather than literal block manipulation.
- Per-style implementation becomes deeper: many styles require state machines, not one-off casts.

## Recommended Concept Authority Update

```text
1. Original detailed Hytale concept in MOD_DESIGN.md
   └─ wins for class/style/ability fantasy and mechanics

2. User current corrections
   └─ win when the original doc is stale, unwanted, or impractical

3. Local Hytale API/assets
   └─ decide how we express the fantasy safely

4. Current protected style JSON
   └─ current implementation data, protected from regeneration,
      but not the full concept authority

5. 2D/Godot data
   └─ useful parity/history, but often compressed from the original Hytale design

6. Existing Java behavior
   └─ current state only; never overrides concept
```

## Immediate Plan Impact

Before implementing all styles:

```text
1. Upgrade the concept-alignment plan to read from original MOD_DESIGN.
2. Generate an "original concept delta" for every ability.
3. For each style, decide:
   ├─ literal implementation
   ├─ Hytale-safe approximation
   └─ deferred advanced mechanic
4. Only then start Terra from the beginning.
```

This avoids building a polished version of the compressed data while accidentally leaving the original fantasy behind.
