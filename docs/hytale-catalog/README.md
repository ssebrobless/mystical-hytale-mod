# Hytale Modding Catalog

A reusable knowledge base for building Hytale mods (this one and future ones).
Synthesized 2026-07-16 from an 11-lane research wave: vanilla `Assets.zip` mining
(60,148 members), `HytaleServer.jar` javap sweeps (0.5.6), inspection of 23 installed
third-party mod jars, community documentation, and open-source repo research —
cross-examined against MOTM's pre-existing indexes.

```
╔═ TRUTH STACK (when sources disagree, lower wins) ═══════════════╗
║ 1. community docs / web        (directional, may be stale)      ║
║ 2. third-party mod patterns    (proven-by-someone, unlicensed)  ║
║ 3. local Assets.zip + jar      (authoritative for 0.5.6)        ║
║ 4. runtime proof in-game       (final gate)                     ║
╚══════════════════════════════════════════════════════════════════╝
```

## Chapters

| File | Contents |
|---|---|
| `plugin-api.md` | Plugin lifecycle, registries, 40+ capability->entry-point table, full event catalog, ECS patterns, deprecation ledger, hostile surfaces |
| `asset-schemas.md` | Authoring schemas: particles, models/animations, EntityEffects, projectiles, interactions, items, blocks/fluids, weather, sounds, UI — with ID-resolution rules |
| `asset-catalog.md` | What vanilla ships: counts, family tables, themed best-of indexes |
| `npc-roles-ai.md` | NPC role/AI authoring: templates, parameter/action/sensor vocabularies, attitude system, ally/pet/mount recipes |
| `mod-patterns.md` | Architecture patterns mined from 23 real mods: persistence, UI, ability triggering, tethers, ownership, libraries — with license rules |
| `community-knowledge.md` | Ecosystem, workflow, tooling, documented gotchas, roadmap risks |

## Pre-existing machine indexes (link, don't duplicate)

| Artifact | What | Staleness |
|---|---|---|
| `audits/hytale-asset-library/latest/` | 10 category .txt indexes + keyword-catalog.md (42 sections) + api-classes.txt | 2026-05-22 |
| `audits/hytale-runtime-capabilities/2026-05-23-p0-p4/` | runtime capability probes (521 class hits) | 2026-05-23 |
| `audits/ability-asset-plan/latest/` | MOTM's 120-ability asset plan, 0 missing refs | 2026-05-22 |
| `docs/hytale-capability-atlas/` | proven primitives + research gates R0-R11 | rolling |
| `docs/modding-research-2026-07-16.md` | 12-theme visual candidates + retheming recipes | 2026-07-16 |
| `C:/tmp/hytale-llms-full.txt` | full hytalemodding.dev docs dump (2MB, 45,198 lines) — third-party, NOT committed | fetched 2026-07-16 |

Regeneration: `scripts/discover-hytale-assets.ps1` rebuilds the asset-library indexes;
`scripts/probe-hytale-runtime-capabilities.ps1` re-probes the jar. Neither captures the
curated schema/pattern knowledge in this catalog — that requires re-running the research
wave against a new game version.

## Version pin

Everything here is verified against **Hytale 0.5.6** (`Implementation-Version` in the
server jar, revision `5ea7c263`). On each game update: re-run the regeneration scripts,
re-check the deprecation ledger in `plugin-api.md`, and re-verify anything marked
`_Test`-tier or UNPROVEN.
