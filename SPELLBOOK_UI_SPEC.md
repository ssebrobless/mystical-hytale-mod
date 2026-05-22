# Spellbook UI Spec

## Role

The spellbook is the player-facing build and ability reference for the mod.

```text
Spellbook
+-- Class
|   +-- selected class
|   +-- class resource / passive summary
|   +-- class identity notes
+-- Style
|   +-- selected style
|   +-- style theme
|   +-- active ability slots
+-- Abilities
|   +-- left click ability
|   +-- right click ability
|   +-- use / third ability
|   +-- descriptions must match real runtime behavior
+-- Perks
    +-- unlocked perks
    +-- active perks
    +-- available selections
    +-- passive modifiers / triggers / synergies
```

No story sections belong in the spellbook. Do not add Journey, Codex, Journal,
Grimoire, lore, quest, reaction glossary, or world-story tabs.

## Variants

### Player Spellbook

```text
Player Spellbook
+-- Class / Style overview
+-- Active abilities with descriptions
+-- Perks
```

This is the normal survival/play experience. It explains what the player has and
what their selected build does. It does not expose test controls.

### Dev/Test Spellbook

```text
Dev/Test Spellbook
+-- Class / Style overview
+-- Active abilities with descriptions
+-- Perks
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

Dev/test controls may be opened by command while validating:

```text
/motm spellbook dev
```

## Design Rules

```text
styles = only source of active abilities
perks  = passive modifiers / triggers / synergies
ui     = must describe current runtime truth, not future intent
```

Perks should never function as standalone active abilities.

## Acceptance

- The player spellbook has only Class, Style/Abilities, and Perks sections.
- The dev/test spellbook contains every player section plus class/style test controls.
- No story/lore/codex/journey/grimoire/journal tabs are visible.
- Ability descriptions are sourced from the active class/style data and audited
  against runtime behavior during Phase 9.
