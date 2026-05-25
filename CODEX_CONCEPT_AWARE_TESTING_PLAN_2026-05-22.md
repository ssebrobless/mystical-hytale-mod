# Concept-Aware Ability Testing Plan - 2026-05-22

> **Superseded testing guidance:** Keep the concept-aware acceptance ideas, but
> do not use this as the operational harness. New feature verification should be
> driven through `AGENTS.md`, `docs/agent-driven-verification-observability.md`,
> and `scripts/run-agent-observability-baseline.ps1`.

## Goal

Future ability audits must prove that an ability performs its actual concept, not
only that `/motm dev test ability <id>` produced a cast log.

This plan sits between `CODEX_TEST_AUDIT_OPTIMIZATION_2026-05-22.md` and
`CODEX_REALIGNMENT_PLAN_2026-05-13.md`. It applies to every future Phase 9
polish pass and final acceptance run.

## Test Shape

```text
Ability source of truth
+-- style JSON
|   +-- name / description
|   +-- cast_type / target_type / trigger
|   +-- range / radius / duration / delay
|   +-- effect / terrain_effect / travel_type
+-- realignment plan
|   +-- style palette / feel
|   +-- mechanical gap table
+-- runtime implementation
    +-- GameplayPlaybackManager path
    +-- StatusEffectManager path
    +-- HytaleAssetResolver visual path

Audit decision
+-- Runtime smoke: cast line exists, no crash
+-- Mechanical proof: hit/status/movement/field/buff happened
+-- Visual proof: screenshot/video reads like the style concept
```

## Required Pre-Audit Pass

Before testing a style, create a small per-style note in the audit report:

```text
Style: <class>/<style>
Concept: <plain English style identity>
Palette/feel: <from CODEX_REALIGNMENT_PLAN section 2>
Abilities:
  1. <id> - expected setup - expected proof
  2. <id> - expected setup - expected proof
  3. <id> - expected setup - expected proof
```

If this note is missing, the audit cannot claim gameplay acceptance. It can only
claim runtime smoke.

## Scenario Matrix

```text
Ability kind                  Required setup                         Required proof
------------------------------------------------------------------------------------------------
jump_land                     arm ability, jump, land near target     landing log targets>=1 plus impact screenshot
movement / dash / leap        start with target in path, record pos   before/after displacement and target-side result
dive / aerial                 face target, airborne timing if needed  hit/effect after motion, not just cast log
projectile / volley / line    target in visible lane                  launch plus target-side hit/effect/impact visual
cone / gaze / breath          face target, narrow camera cone         target-side effect; no "No valid target" accepted
ground_zone / field           target inside radius for duration       field persists and pulses expected status/damage
ground_strike delayed         target at ground mark, wait delay       telegraph first, then delayed hit/visual
self_buff / form              third-person camera                     body tint/model/status/stat change
support / heal                damaged or buffable caster/ally         HP/stat/status changes visibly or in log
summon                        clear arena, summon then wait           summon appears, survives, acts or buffs as described
cleanse / absorb / reactive   pre-apply debuff/damage condition       effect removes/absorbs/reacts; setup condition logged
```

## PASS Language

Use these words consistently:

```text
RUNTIME PASS
  The command/cast path works and the game did not crash.

MECHANICAL PASS
  The described gameplay effect occurred under the required setup.

VISUAL PASS
  The screenshot/video clearly reads as the intended style/ability.

FULL PASS
  Runtime + mechanical + visual all passed.
```

Do not write "validated" or "done" for an ability that only has a runtime pass.

## Harness Changes To Make Next

1. Update `scripts/audit-phase9-class.ps1` so every ability row includes
   `runtime`, `mechanical`, and `visual` fields separately.
2. Add scenario-specific helpers:
   - `scripts/setup-ability-scenario.ps1 -ClassId <id> -StyleId <id> -AbilityId <id>`
   - `scripts/assert-ability-proof.ps1 -AbilityId <id> -Scenario <kind>`
3. Add movement primitives to `scripts/send-input.ps1` if missing:
   - forward hold
   - strafe hold
   - jump plus forward
   - face target using mouse nudge or logged heading
4. Add a generated audit matrix from all four `data/styles/*.json` files:
   - `audits/ability-matrix/<timestamp>/ability-scenarios.md`
   - each row: class, style, ability, description, cast_type, target_type,
     trigger, required setup, required proof.
5. For every future style polish pass, read that style's row from the matrix
   before launching Hytale.

## Immediate Next Use

After Phase 6 spellbook work, use this plan to create the first real
concept-aware polish batch. Recommended order:

```text
1. Cross-cutting target/proxy cleanup
   +-- unmapped MOTM summon/proxy roles
   +-- projectile target-side impact proof
   +-- cone/facing target acquisition

2. Mechanical gap batch from realignment section 3
   +-- delayed ground_strike telegraphs
   +-- field tick effects
   +-- movement ability target-path setup
   +-- self/form/buff proof

3. Visual identity batch
   +-- one class at a time
   +-- one style at a time
   +-- screenshots/video judged against palette + feel
```

## Stop Conditions

Stop and re-plan instead of improvising if:

- a required Hytale API path is unknown;
- a particle/SystemId is not proven in `HytaleAssetResolver`, `Assets.zip`, or
  `/showcase dump`;
- a test would require editing protected style JSON wholesale;
- an audit setup cannot prove the ability concept.
