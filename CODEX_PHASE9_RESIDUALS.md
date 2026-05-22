# Phase 9 Residuals

Non-blocking findings discovered during successful style validation runs. These should be batched for Claude or a later polish pass after each class's style group, unless one starts causing crashes, failed casts, or failed acceptance gates.

## Terra

- ~~`magma`: Hytale logs `Reloading nonexistent role motm_projectile!` when `magma_sling` spawns the projectile visual proxy. The ability still casts and launches successfully. Likely a cross-cutting projectile proxy-role issue, not Magma-specific.~~ Resolved 2026-05-21: projectile proxy now uses resolved role `Slug_Magma`; audit `audits/blockers/2026-05-21/projectile-role/report.md` confirms `motm_projectile reload lines: 0`.
- `magma`: the projectile proxy now logs `Unmapped NPC type encountered. npcTypeId=Slug_Magma, modelAssetId=Slug_Magma`. This did not block `magma_sling`; classify with the existing summon/proxy type-mapping cleanup.
- `stone`: `rubble_rouser`, `pillar_strike`, and `rockslide` validate successfully in `audits/phase9-stone/2026-05-21T21-21-31/`, but the earlier clean runtime pass logged `Unmapped NPC type encountered. npcTypeId=Spark_Living, modelAssetId=Spark_Living` for the projectile visual proxy. Classify with the existing summon/proxy type-mapping cleanup.
- `stone`: `pillar_strike` casts, damages, and stuns targets, but the realignment plan calls for a delayed vertical pillar telegraph. Current runtime resolves immediately; schedule for a later Stone polish pass.
- `stone`: `rockslide` casts and damages targets, but the realignment plan calls for the falling-rocks field to apply slow per tick. Current `falling_rocks` terrain behavior is knockback-only; schedule for a later Stone polish pass.
- `arbor`: `rooted`, `vines`, and `sapling` validate successfully in `audits/phase9-arbor/2026-05-21T21-25-32/`, but `vines` still behaves as immediate damage plus stun. Realignment calls for root plus damage-over-time across `duration_seconds`.
- `arbor`: `sapling` summons successfully, but the server logs `Unmapped NPC type encountered. npcTypeId=Spirit_Root, modelAssetId=Spirit_Root`. Classify with the existing summon/proxy type-mapping cleanup.
- `bloom`: `nightshade`, `frolick`, and `cacti_cluster` validate successfully in `audits/phase9-bloom/2026-05-21T21-30-15/`; `frolick` now grants speed after the surgical data fix. `cacti_cluster` still logs `Unmapped NPC type encountered. npcTypeId=Spark_Living, modelAssetId=Spark_Living` for projectile proxy mapping cleanup.
- `self_petrification`: `gargoyle`, `glare`, and `tunnel` validate successfully in `audits/phase9-self-petrification/2026-05-21T21-33-46/`, but `gargoyle` still needs the stronger gray stone-form tint called out by the realignment plan.
- `self_petrification`: `glare` stuns targets successfully, but still needs the stronger attached eye/proxy cast visual called out by the realignment plan.
- `self_petrification`: `tunnel` moves and damages successfully, but still reads as a surface dash/trail. Realignment calls for an underground concealment visual during travel.
- `soil`: `burrow`, `mudpit`, and `debris` validate successfully in `audits/phase9-soil/2026-05-21T21-38-16/`; `debris` now includes blind after the surgical data fix. `burrow` still needs the underground concealment visual called out by the realignment plan.
- `soil`: `debris` launches its projectile volley successfully, but the cast log does not prove target-side blind/vulnerability application on impact. Keep this for a later projectile-impact audit, alongside the `Spark_Living` proxy mapping cleanup.
- `gem`: `lapidary`, `fracture`, and `refraction` validate successfully in `audits/phase9-gem/2026-05-21T21-47-00/`; `refraction` now grants speed and `fracture` impact routing now resolves to crystal sparks. The cast log proves projectile-line launch, but target-side impact visuals should be checked in a later projectile-impact audit, alongside the `Spark_Living` proxy mapping cleanup.

## Hydro

- `snow`: `frosty` spawns and casts successfully, but the server logs `Unmapped NPC type encountered. npcTypeId=Golem_Crystal_Frost, modelAssetId=Golem_Crystal_Frost`. This did not block summon creation or crash the world; later mob-XP/type mapping should recognize MOTM summon proxy types or explicitly ignore them.
- `surf`: `high_tide`, `waverider`, and `riptide` validate successfully in `audits/phase9-surf/2026-05-21T22-19-09/`; `high_tide` now grants `self speed` after the surgical data fix. The run logs `Unmapped NPC type encountered. npcTypeId=Spark_Living, modelAssetId=Spark_Living` for projectile proxy mapping cleanup.
- `rain`: `piercing_rain`, `rainbow`, and `splash` validate successfully in `audits/phase9-rain/2026-05-21T22-21-49/`, but field visuals for `piercing_rain` and `rainbow` log `Reloading nonexistent role motm_field!`. This is the field-proxy equivalent of the earlier projectile proxy-role issue; abilities still cast and the world stayed connected.
- `boiling`: `scald`, `geyser`, and `overheat` validate successfully in `audits/phase9-boiling/2026-05-21T22-24-51/`, but `geyser` still needs the delayed vertical telegraph/activation called out by the realignment plan. `overheat` also logs the known field-proxy role warning `Reloading nonexistent role motm_field!`.
- `vapor`: `vapor_vanish`, `dispersion`, and `hidrosis` validate successfully in `audits/phase9-vapor/2026-05-21T22-27-41/`, but `dispersion` still needs stronger start/end vapor reform visuals called out by the realignment plan.
- `iceberg`: `ice_cap`, `glacier`, and `ice_shelf` validate successfully in `audits/phase9-iceberg/2026-05-21T22-30-24/`, but `ice_cap` still needs a future on-hit freeze-attacker hook, `glacier` needs a more physically readable barrier visual, and `ice_shelf` needs delayed strike telegraph/activation.
- `saltwater`: `tide_pool`, `abyssal_assist`, and `rip_current` validate successfully in `audits/phase9-saltwater/2026-05-21T22-33-30/`, but `tide_pool` still needs caster speed while standing in the field. It also logs the known field-proxy role warning `Reloading nonexistent role motm_field!`.
- `freshwater`: `leap_frog`, `river_rapids`, and `swamp_monster` validate successfully in `audits/phase9-freshwater/2026-05-21T22-43-55/`; `river_rapids` now grants `self speed` after the surgical data fix. `swamp_monster` logs `Unmapped NPC type encountered. npcTypeId=Frog_Green, modelAssetId=Frog_Green` for summon proxy mapping cleanup.
- `bilgewater`: `bilge_dump`, `anchor_haul`, and `oil_spill` validate successfully in `audits/phase9-bilgewater/2026-05-21T22-47-02/`. `anchor_haul` logs `Unmapped NPC type encountered. npcTypeId=Spark_Living, modelAssetId=Spark_Living` for projectile proxy mapping cleanup.

## Aero

- `scream`: `shriek`, `sonic_boom`, and `battle_cry` validate successfully in `audits/phase9-scream/2026-05-21T22-57-54/`; `battle_cry` confirms both `self attack buff` and `self speed` at runtime. `sonic_boom` logs `Unmapped NPC type encountered. npcTypeId=Spark_Living, modelAssetId=Spark_Living`, and `battle_cry` logs the known field-proxy role warning `Reloading nonexistent role motm_field!`.
- `jet`: `jet_burst`, `afterburner`, and `mach_punch` validate successfully in `audits/phase9-jet/2026-05-21T23-00-09/`; `jet_burst` hits and applies knockback. `afterburner` proves dash plus ember trail but reports `No valid target in range`, and `mach_punch` proves dash-strike movement but also reports `No valid target in range`; keep both for a later dash hit-position audit.
- `thunder`: `thunderclap`, `smite`, and `chain_lightning` validate successfully in `audits/phase9-thunder/2026-05-21T23-20-44/`; `smite` now applies its 1.25x shocked-target bonus through the projectile-impact path. `smite` logs `Unmapped NPC type encountered. npcTypeId=Spirit_Thunder, modelAssetId=Spirit_Thunder` for projectile proxy mapping cleanup.

## Harness

- During Snow validation, sending `{ESC}` while no custom UI was open put Hytale in the pause menu, causing subsequent chat commands to be swallowed until returning to game. Acceptance recovered manually; future harness work should use a less ambiguous close-overlay primitive.
