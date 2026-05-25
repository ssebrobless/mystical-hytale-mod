# Leveling, Stats, Perks, and Startup Selection Plan

This captures the next roadmap slice after class passive verification. It keeps the mod focused on classes, styles, abilities, leveling/scaling, and perks. Races and story/codex/grimoire framing should be removed from the user-facing mod.

```
╔════════════════════════════════════════════════════════════════════╗
║ Player Progression                                                ║
╠══════════════╦═════════════════════════════════════════════════════╣
║ Level 0      ║ Native Hytale defaults; stat table adds nothing    ║
║ Levels 1-200 ║ +2 stat upgrade points per level                   ║
║ Levels 10-100║ +1 perk choice every 10 levels, max 10 perks       ║
║ Level 100    ║ Perk unlock cap                                    ║
║ Level 200    ║ Stat point and XP progression cap                  ║
╚══════════════╩═════════════════════════════════════════════════════╝
```

## Stat Table

Each level grants 2 upgrade points until level 200. Players spend points into:

| Stat | Per Point |
| --- | --- |
| Vigor | +2% max HP and +0.25% permanent damage reduction |
| Tenacity | +0.75% damage increase and +0.5% critical strike damage |
| Endurance | +2% native Hytale max stamina reserve and +1% stamina regen from zero |
| Agility | +0.25% movement speed and +0.05% attack speed for native Hytale melee weapons only |
| Luck | +0.1% critical strike chance and +0.5% XP gain bonus |

Implementation direction:

- Add a persistent player stat allocation object with unspent points.
- Apply native-stat modifiers for HP and stamina where Hytale exposes stable stat APIs.
- Apply damage, crit, XP, and melee attack-speed modifiers through MOTM combat/leveling hooks.
- Keep level 0 as a neutral baseline with no added stat bonuses.
- Cap unspent/allocated progression at level 200.

## Perks

```
╔════════════════════════════════════════════════════════════════════╗
║ Perk Model                                                        ║
╠════════════════════════════════════════════════════════════════════╣
║ 20 total perks planned                                            ║
║ 5 themed perks per class family                                   ║
║ Any class can choose any unlocked perk                            ║
║ Player earns one choice every 10 levels through level 100         ║
║ Maximum selected perks: 10                                        ║
║ Enemies and mobs never receive perks                              ║
╚════════════════════════════════════════════════════════════════════╝
```

Implementation direction:

- Replace any current class-locked perk selection assumptions with a shared 20-perk pool.
- Keep class theme tags for organization only; do not gate by current class.
- Build the UI so level 10/20/.../100 grants one pending perk choice.
- Prevent additional perk grants above level 100.

## First-Join Startup UI

```
New Player Join
      │
      ▼
Grant Spellbook + freeze/protect player
      │
      ▼
Class Selection
  click class ▶ read class description/passives ▶ Choose
      │
      ▼
Style Selection
  list styles for chosen class ▶ click style ▶ read 3 ability details ▶ Choose
      │
      ▼
Close UI + remove startup invincibility + normal movement begins
```

Requirements:

- Give the player the spellbook immediately.
- Open the selection UI immediately for first-time players.
- While the UI is active, the player must be invincible and unable to die.
- Class list shows the four classes. Clicking once selects and shows description/passives. A bottom `Choose` button confirms.
- Style list only shows styles for the chosen class. Clicking a style shows ability descriptions, cooldowns, durations, effects, function, and visual identity. A bottom `Choose` button confirms.
- After style selection, close the UI and allow normal play.
- Reuse spellbook/perk UI description components where possible.

## In-Game Reference UI

The spellbook should remain the player-facing reference surface:

- Show current class, selected style, and all ability descriptions.
- Show cooldowns, durations, target type, range/radius, effects, function, and concept visual notes.
- Show stat table, unspent stat points, allocated points, and current derived bonuses.
- Show perk choices and selected perks.
- Remove Journey, Codex, Grimoire, story, lore, races, or unrelated narrative tabs from MOTM UI.
- A separate dev/test variant may include class/style switching for review only.

## Enemy Scaling

```
Mob Level 0-200
      │
      ▼
Stat points derived from scaled level
      │
      ▼
Preset distribution
  60% priority stat
  40% split evenly across other 4 stats
      │
      ▼
No perks, no player-only selection UI
```

Mob stat presets:

- Vigor-priority
- Tenacity-priority
- Endurance-priority
- Agility-priority
- Luck-priority

Friendly summons:

- Match caster level.
- Match caster stat distribution.
- Do not inherit caster perks.

Mob titles:

| Level Range | Title |
| --- | --- |
| 0-50 | Intern |
| 51-100 | Apprentice |
| 101-150 | Journeyman |
| 151-200 | Master |

Mob health bar text should read top-to-bottom:

```text
Master
Level 172
```

Remove existing elite/variant titles from the displayed mob label model.

## Implementation Order

1. Data model migration: add player stat allocations, unspent points, perk cap fields, and first-join selection state.
2. Leveling manager: cap perks at 100, stats at 200, award 2 stat points per level.
3. Stat modifier manager: apply Vigor, Endurance, Agility native modifiers and MOTM combat/XP modifiers.
4. Perk manager: switch to shared 20-perk pool with class-theme tags only.
5. Startup UI: class choose screen, style choose screen, invincibility lock, spellbook grant.
6. Spellbook UI cleanup: remove story/race/codex/grimoire/journey tabs; add reference/stats/perks views.
7. Mob scaling: replace elite titles with level title bands and internal stat-table scaling.
8. Summon inheritance: copy caster level/stat allocation, exclude perks.
9. Harness scenarios: first-join flow, stat point spending, perk unlocks at 10/100/101, mob title bands, summon stat reflection.

## Acceptance Gates

- New player cannot take damage before finishing class/style selection.
- Class/style selection persists after reconnect.
- Level 10 grants one perk choice; level 100 grants tenth; level 110 grants no additional perk.
- Level 200 is the final stat-point cap.
- Stat allocation changes derived player stats or MOTM combat math exactly as listed.
- Mobs show only Intern/Apprentice/Journeyman/Master and exact level text.
- Mobs never receive perks.
- Friendly summons match caster level/stat distribution and ignore perks.
- No race UI/data path remains user-facing.
