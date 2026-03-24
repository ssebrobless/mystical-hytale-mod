# Spellbook UI Spec

## Role

The spellbook is the primary player-facing interface for the mod.

```text
styles  -> active combat kit
perks   -> passive build modifiers
races   -> passive identity bonuses
journal -> story and world integration
```

The spellbook should answer three questions quickly:

```text
Who am I?
What can I do right now?
What should I pursue next?
```

## Sections

```text
overview
├ identity snapshot
├ current progression state
└ next-step guidance

journey
├ class
├ race
├ style
├ level
└ milestone progress

grimoire
├ 3 active style abilities
├ class passive
├ cooldown / resource info
└ ability summaries

perks
├ unlocked perk tiers
├ pending selections
├ active synergies
└ future giant perk web entry point

resources
├ current class resource state
├ regen / refill rules
└ class-specific sustain notes

codex
├ reactions
├ mob scaling
├ elite variants
└ system glossary

journal
├ story chapters
├ lore entries
├ discoveries
└ future quest threads
```

## Interaction Model

### MVP

```text
/motm spellbook
/motm spellbook <section>
```

This command-backed version establishes the information architecture.

### Future UI

```text
join world
  └─ receive spellbook item
       └─ right click
            └─ open spellbook custom UI
```

Onboarding behavior:

```text
no class   -> open class selection
no race    -> open race selection
no style   -> open style selection
otherwise  -> open overview
```

## Design Rules

```text
styles = only source of active abilities
perks  = always passive modifiers / triggers / synergies
races  = passive identity bonuses
```

Perks should never function as standalone active abilities.

## Long-Term Goal

The `perks` section should eventually become a large organized visual web:

```text
center      = current style identity
inner ring  = core stat / survival / utility perks
mid ring    = ability modifiers / trigger perks
outer ring  = capstones / cross-effects / rare synergies
links       = prerequisites or synergy relationships
```
