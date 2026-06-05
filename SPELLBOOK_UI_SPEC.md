# Spellbook UI Spec

## Role

The spellbook is the player-facing build and ability reference for the mod. The
canonical UI contract lives in `docs/ui-canon-2026-06-01.md`.

```text
Spellbook
+-- Class
|   +-- selected class
|   +-- collapsible class descriptions
|   +-- passive ability descriptions
+-- Styles / Abilities
|   +-- selected style
|   +-- style theme
|   +-- active ability slots
|   +-- left click ability
|   +-- right click ability
|   +-- use / third ability
|   +-- descriptions must match real runtime behavior
+-- Perks
|   +-- token count
|   +-- selected perks
|   +-- available perk choices
|   +-- passive modifiers / triggers / synergies
+-- Stats
    +-- stat point selection
    +-- current percent buffs
```

No story sections belong in the spellbook. Do not add Journey, Codex, Journal,
Grimoire, Resources, lore, quest, reaction glossary, or world-story tabs.

## Variants

### Player Spellbook

```text
Player Spellbook
+-- Class
+-- Styles / Abilities
+-- Perks
+-- Stats
```

This is the normal survival/play experience. It explains what the player has and
what their selected build does. It does not expose test controls.

### Dev/Test Spellbook

```text
Dev/Test Spellbook
+-- Class
+-- Styles / Abilities
+-- Perks
+-- Stats
+-- Test controls
    +-- change class
    +-- change style
    +-- refresh current spellbook
    +-- trigger or inspect current abilities
```

The dev/test variant exists to speed validation and should be gated behind the
mod's internal/dev build path or dev commands. It must not become the default
player experience.

## Interaction Model

```text
/motm spellbook
/motm spellbook player
/motm spellbook dev
/motm spellbook perks
```

Long-term item flow:

```text
join world
  +-- receive spellbook item
      +-- right click
          +-- open Player Spellbook
```

Startup flow for a new player with no class/style:

```text
join world
  +-- receive spellbook item
  +-- open startup UI
      +-- choose class
      +-- choose style
      +-- close UI and release movement
```

The player must be invincible while the startup UI is open.

## Design Rules

```text
styles = only source of active abilities
perks  = passive modifiers / triggers / synergies
ui     = must describe current runtime truth, not future intent
```

Perks should never function as standalone active abilities.

## Acceptance

- The player spellbook has only Class, Styles/Abilities, Perks, and Stats sections.
- The dev/test spellbook contains every player section plus class/style test controls.
- No story/lore/codex/journey/grimoire/journal/resource tabs are visible.
- Ability descriptions are sourced from the active class/style data and audited
  against runtime behavior during Phase 9.
