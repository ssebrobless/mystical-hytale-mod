# Full Style Ability Alignment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every MOTM class, style, and ability match the user's original concept in both gameplay behavior and visible Hytale presentation.

**Architecture:** Treat the style JSON descriptions as the concept source, the generated ability asset plan as the visual bridge, and in-game evidence as the only final proof. Implementation proceeds by shared scenario bridges first, then per-style validation, so the same class of ability does not get fixed forty different ways.

**Tech Stack:** Java Hytale server plugin, Hytale EntityEffect JSON, local `Assets.zip`/`HytaleServer.jar` discovery, PowerShell harness scripts, Custom UI `.ui` pages.

---

## Operating Shape

```text
User concept
  │
  ├─ 40 styles / 120 abilities from protected style JSON
  │
  ├─ verified Hytale assets
  │    ├─ 492 built-in particle/model paths
  │    └─ 30 local MOTM EntityEffects
  │
  ├─ scenario bridge
  │    ├─ self/channel
  │    ├─ projectile/line
  │    ├─ persistent field
  │    ├─ movement/form
  │    ├─ summon/command
  │    ├─ ground mark/barrier
  │    ├─ cone/gaze
  │    ├─ single target
  │    └─ jump/land
  │
  └─ evidence gate
       ├─ log proof
       ├─ target/caster state proof
       ├─ third-person screenshot/video when visual
       └─ PASS only when runtime + mechanics + visuals agree
```

## Current Analysis

What is solid:

- Every style and ability now has a concrete asset recipe in `audits/ability-asset-plan/latest/ability-asset-plan.json`.
- The generated plan covers all `120` abilities: `30` per class.
- Asset validation is clean: `0` missing references across `492` local Hytale asset paths and `30` local MOTM effects.
- Public Hytale docs support the direction: server-side plugins, data/art assets, custom UI, and EntityEffects are the right surfaces.

What still needs correction:

- `HytaleAssetResolver.java` still routes many visuals by broad class fallback. That is useful as a safety net, but not enough for style identity.
- Runtime plumbing exists for projectiles, fields, summons, forms, and status effects, but a green cast log does not prove the advertised concept happened.
- Some earlier audit language says styles are "playable" or "partial"; treat that as code-path status, not final concept acceptance.
- The test harness must move, jump, face targets, use third-person view with `V`, count/clean mobs, and leave the spawn zone/portal area before visual claims.
- Spellbook UI must stay gameplay-only: Class, Style/Abilities, Perks, plus a separate dev/test variant with class/style switching. No Journey, Codex, Grimoire, Journal, lore, or story tabs.

## Scenario Counts

| Scenario | Abilities | Implementation bar |
| --- | ---: | --- |
| `self_or_channel` | 31 | Caster aura/status in third-person; outgoing/incoming effect proven in logs. |
| `projectile_or_line` | 25 | Visible travel path, resolved role, target-side impact and status/damage proof. |
| `persistent_field` | 20 | Radius visible, tick/release logs, inside/outside target behavior proven. |
| `movement_or_form` | 16 | Start/end position proof, actual movement/form state, third-person capture. |
| `summon_or_command` | 9 | Resolved role/model, spawn visible, behavior/targeting log proof. |
| `ground_mark_or_barrier` | 7 | Telegraph or barrier appears before effect, delayed hit/solid block behavior proven. |
| `cone_or_gaze` | 6 | Facing direction and target cone proven, over-shoulder capture. |
| `single_target` | 5 | Target status/damage/finisher proof, visual impact on target. |
| `jump_land` | 1 | Stomp arms, player jumps, landing AoE resolves targets, cracks visible. |

## Style Contract Matrix

| Class | Style | Abilities | Concept bar |
| --- | --- | --- | --- |
| Terra | Quake | `stomp`, `aftershock`, `sinkhole` | Heavy earth control: jump-land cracks, lingering tremor, buried-look suffocation. |
| Terra | Metal | `iron_wall`, `metal_coat`, `alloy_enhancement` | Steel defense: solid barrier, armor plating, next-hit weapon enhancement. |
| Terra | Magma | `lava_pool`, `obsidian_skin`, `magma_sling` | Molten area denial: viscous fire/smoke, burn ticks, hot defense. |
| Terra | Stone | `rubble_rouser`, `pillar_strike`, `rockslide` | Heavy rubble: moving debris, delayed vertical pillar, falling-rock field. |
| Terra | Arbor | `rooted`, `vines`, `sapling` | Living roots: healing growth, entangling line/root, treant summon. |
| Terra | Bloom | `nightshade`, `frolick`, `cacti_cluster` | Toxic floral: spores, flower dance buff, needle volley poison/slow. |
| Terra | Self Petrification | `gargoyle`, `glare`, `tunnel` | Statue/stone body: gray form, petrifying gaze, underground strike illusion. |
| Terra | Soil | `burrow`, `mudpit`, `debris` | Dirt/mud control: underground emerge, sticky root/slow, blinding debris. |
| Terra | Sand | `sandstorm`, `dust_devil`, `vitrification` | Desert motion: sand cloud volume, dust vortex, superheated glass burn. |
| Terra | Gem | `lapidary`, `fracture`, `refraction` | Crystal defense/offense: shield facets, shard line, refractive self buff. |
| Hydro | Icicle | `frozen_needles`, `stalactite_crash`, `skate` | Sharp ice: crystal volley, delayed falling ice, icy movement/evasion. |
| Hydro | Snow | `snow_imp`, `snowstorm`, `frosty` | Soft frost: snow minion, attack-slow snow field, tanky frost summon. |
| Hydro | Surf | `high_tide`, `waverider`, `riptide` | Wave momentum: push, self speed/shield, pull current. |
| Hydro | Rain | `piercing_rain`, `rainbow`, `splash` | Weather support: damaging rain, healing rainbow, water burst shield. |
| Hydro | Boiling | `scald`, `geyser`, `overheat` | Steam pressure: scald projectile, vertical geyser, self-burn power buff. |
| Hydro | Vapor | `vapor_vanish`, `dispersion`, `hidrosis` | Mist form: vanish, reform strike, evasive moisture shell. |
| Hydro | Iceberg | `ice_cap`, `glacier`, `ice_shelf` | Heavy ice: armor retaliation, large barrier, crushing shelf. |
| Hydro | Saltwater | `tide_pool`, `abyssal_assist`, `rip_current` | Ocean pressure: slow/speed pool, abyssal stun/weaken, dragging current. |
| Hydro | Freshwater | `leap_frog`, `river_rapids`, `swamp_monster` | River life: leap strike, speed/momentum buff, swamp creature summon. |
| Hydro | Bilgewater | `bilge_dump`, `anchor_haul`, `oil_spill` | Dirty water: toxic cone, hooked anchor drag, oily defensive buff. |
| Aero | Scream | `shriek`, `sonic_boom`, `battle_cry` | Sonic force: deafen cone, shockwave projectile, team/self cry buff. |
| Aero | Jet | `jet_burst`, `afterburner`, `mach_punch` | Speed combat: dash launch, damaging trail, strike after dash. |
| Aero | Thunder | `thunderclap`, `smite`, `chain_lightning` | Lightning: AoE shock, bolt, chained arcs against shocked targets. |
| Aero | Tornado | `twister`, `funnel_cloud`, `eye_of_the_storm` | Vortex control: knockback spiral, sustained funnel, calm healing center. |
| Aero | Jump | `leap`, `divebomb`, `hang_time` | Airborne gameplay: launch, dive impact, dodge while airborne. |
| Aero | Wind Blade | `air_slash`, `gale_cutter`, `razor_wind` | Cutting air: visible slash line, knockback blade, self sharpening aura. |
| Aero | Smoke | `smoke_bomb`, `vanish`, `smoke_form` | Smoke stealth: obscuring field, vanish next-hit, body/form evasion. |
| Aero | Gale Wizard | `gust`, `cyclone_shield`, `tempest` | Refined wind magic: shaped gust, defensive cyclone, stunning storm. |
| Aero | Pressure | `air_shot`, `bullet_storm`, `pressure_burst` | Compressed air: hard projectile, rapid bullets, explosive pressure ring. |
| Aero | Pollution | `smog`, `toxic_breath`, `acid_rain` | Toxic air: haze field, poison cone, corrosive rain. |
| Corruptus | Flame | `fireball`, `ignite`, `combust` | Fire loop: projectile burn, self fire aura, consume existing burns. |
| Corruptus | Necro | `raise_dead`, `life_drain`, `death_mark` | Undeath: minion rise, drain channel, vulnerability mark. |
| Corruptus | Shadow | `shadow_step`, `umbral_veil`, `dark_embrace` | Darkness: teleport clone, invis next-hit, shadow evasion zone. |
| Corruptus | Hell Flame | `hellfire`, `infernal_ground`, `soul_scorch` | Harsher fire: hell projectile, infernal field, self-cost finisher. |
| Corruptus | Mentokinesis | `dominate`, `mind_shatter`, `hivemind` | Psychic control: gaze/control, mind projectile, collective self buff. |
| Corruptus | Imbuement | `imbue_power`, `imbue_fortitude`, `imbue_swiftness` | Body enchantment: attack, defense, and speed auras that affect follow-up combat. |
| Corruptus | Attonement | `sanctuary`, `absorb`, `purify` | Cleansing corruption: holy field, absorb-to-heal, cleanse/protect. |
| Corruptus | Void | `rift`, `void_spawn`, `consume` | Cosmic void: pulling rift, void summon, consuming finisher. |
| Corruptus | Scarak | `scarak_egg`, `brood_surge`, `locust_queen` | Insect brood: egg hatch, commander buff, queen summon. |
| Corruptus | Primordial | `pterodactyl_form`, `triceratops_form`, `t_rex_form` | Beast forms: flight/evasion, armored charge, heavy predator damage. |

## Implementation Tasks

### Task 1: Lock The Concept Contract

**Files:**
- Read: `src/main/resources/data/styles/*_styles.json`
- Read: `audits/ability-asset-plan/latest/ability-asset-plan.json`
- Modify: `ABILITY_COMPLETION_CHECKLIST.md`

- [ ] Add a note that `Playable` and `Partial` are runtime-plumbing states, not final concept acceptance.
- [ ] Add a final acceptance column: `Runtime`, `Mechanical`, `Visual`, `Harness`.
- [ ] Run `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/generate-ability-asset-plan.ps1 -NoTimestamp`.
- [ ] Commit: `docs(abilities): lock concept acceptance ledger`.

### Task 2: Make Resolver Routing Style-First

**Files:**
- Modify: `src/main/java/com/motm/util/HytaleAssetResolver.java`
- Test: `scripts/generate-ability-asset-plan.ps1`

- [ ] Replace broad class fallback as the first decision with a `(classId, styleId)` style profile table.
- [ ] Keep class fallback only after style lookup fails.
- [ ] Route cast/travel/impact/loop/model using the assets from `ability-asset-plan.json`.
- [ ] Preserve local MOTM effects for Quake, Scream, Gem, and other bespoke effects that already exist.
- [ ] Build with `powershell -ExecutionPolicy Bypass -File scripts/build-install.ps1`.
- [ ] Commit: `feat(resolver): route visuals by class and style`.

### Task 3: Harden Shared Runtime Bridges

**Files:**
- Modify: `src/main/java/com/motm/manager/GameplayPlaybackManager.java`
- Modify only if needed: `src/main/java/com/motm/manager/StatusEffectManager.java`

- [ ] `projectile_or_line`: prove launch role, travel proxy, hit, impact effect, target damage/status.
- [ ] `persistent_field`: prove field engage, visual radius, tick, target status, release cleanup.
- [ ] `movement_or_form`: prove start/end position, movement vector, form/aura state, target result.
- [ ] `summon_or_command`: prove role/model resolution, spawn, targeting, behavior, despawn/expiry.
- [ ] `ground_mark_or_barrier`: prove telegraph before delayed impact, or barrier volume before effect.
- [ ] `cone_or_gaze`: prove facing direction, cone target list, target impact.
- [ ] `self_or_channel`: prove caster status and follow-up weapon/defense effect where applicable.
- [ ] `jump_land`: preserve Phase 5 Stomp as the regression sample.
- [ ] Build after each scenario family and commit one family at a time.

### Task 4: Upgrade The Harness To Test The Actual Concept

**Files:**
- Modify: `scripts/setup-ability-scenario.ps1`
- Modify: `scripts/audit-phase9-class.ps1`
- Modify: `scripts/assert-ability-proof.ps1`
- Modify: `scripts/send-input.ps1`

- [ ] Add scenario movement scripts: walk, dash, jump, face target, switch third-person with `V`.
- [ ] Add flatlands navigation support after the user enters the world, using the creative-world portal route or cached flatlands coordinates.
- [ ] Add mob-count and cleanup checks so repeated tests do not leave crowded worlds.
- [ ] Capture one screenshot/video frame at the readable moment for each scenario.
- [ ] Fail visual proof if the camera is facing a wall, sky-only frame, spawn zone, or no target region.
- [ ] Commit: `feat(harness): test ability concepts instead of static casts`.

### Task 5: Spellbook UI Must Reflect Runtime Truth

**Files:**
- Create/modify: `src/main/resources/Common/UI/Custom/Pages/MOTM_Spellbook.ui`
- Modify: `src/main/java/com/motm/ui/SpellbookPage.java`
- Modify: `src/main/java/com/motm/command/MotmCommandManager.java`

- [ ] Player spellbook tabs: Class, Style/Abilities, Perks only.
- [ ] Dev spellbook tabs: Class, Style/Abilities, Perks, Test Controls only.
- [ ] Remove or avoid Journey, Codex, Grimoire, Journal, lore, and story tabs.
- [ ] Ability descriptions must come from active runtime data and must not promise mechanics that are not implemented.
- [ ] Dev controls may change class/style and refresh the spellbook for testing.
- [ ] Commit: `feat(ui): add gameplay-only spellbook variants`.

### Task 6: Execute Per-Class Implementation Passes

**Files:**
- Modify as needed: `HytaleAssetResolver.java`, `GameplayPlaybackManager.java`, MOTM EntityEffect JSON files.
- Protected JSON: surgical edits only when a description/effect mismatch is proven.

- [ ] Terra pass: all 10 styles, starting at Quake and moving through Gem.
- [ ] Hydro pass: all 10 styles, starting at Icicle and moving through Bilgewater.
- [ ] Aero pass: all 10 styles, starting at Scream and moving through Pollution.
- [ ] Corruptus pass: all 10 styles, starting at Flame and moving through Primordial.
- [ ] For each style, test all three abilities from flatlands with correct scenario setup.
- [ ] Save evidence under `audits/phase9-<class>-<style>/<timestamp>/`.
- [ ] Commit each style or small style group without exceeding the current commit hygiene cap.

## Acceptance Gates

An ability is not complete until all four are true:

```text
Runtime    ──▶ it casts from the spellbook/dev control without crash
Mechanical ──▶ the advertised effect actually changes target/caster state
Visual     ──▶ the player can recognize the style and action within ~0.5s
Harness    ──▶ logs + screenshot/video prove the correct scenario was tested
```

A style is not complete until all three abilities pass and the style is visually distinct from the other nine styles in the same class.

A class is not complete until all ten styles pass and Phase 5 still passes as a regression check from a cold launch.

## Research Notes

- Hytale's own modding status post says modding is server-side first and built around plugins plus data/art assets, but documentation/tooling is still rough. That supports local jar/asset discovery as the final authority.
- The custom UI docs confirm server Java builds UI commands against `.ui` markup, which matches the Spellbook path.
- EntityEffect docs confirm the correct visual layer for tint, duration, particles, model changes, and status-like visuals.

Sources:

- https://hytale.com/news/2025/11/hytale-modding-strategy-and-status
- https://hytalemodding.dev/pl-PL/docs/official-documentation/custom-ui
- https://release.server.docs.hytale.com/com/hypixel/hytale/server/core/ui/builder/UICommandBuilder.html
- https://hytale-docs.pages.dev/modding/systems/entity-effects/

## Decision

The next best implementation move is not to blindly resume style testing. The next move is:

```text
1. Update the checklist to separate runtime/mechanical/visual/harness truth.
2. Make the resolver style-first using the verified asset plan.
3. Upgrade harness scenario movement/third-person/flatlands proof.
4. Then restart Terra from the beginning and validate all 10 Terra styles.
```

That keeps the build aligned with the user's concept instead of drifting into generic "ability casted" proof.
