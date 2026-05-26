# Ability Concept Runtime Evidence

Every class/style ability is now expected to emit concept-route evidence when it
casts. This is separate from the protected style JSON, which remains concise
runtime data and should not be treated as the full design authority.

## Evidence Shape

```
ability_cast_begin
  └─ ability executes runtime primitives
       ├─ projectile / field / terrain / summon / movement / combat evidence
       ├─ ability_concept_route
       │    ├─ route
       │    ├─ visualPlan
       │    ├─ safety
       │    ├─ stateMachine
       │    ├─ physicalVisual
       │    ├─ friendlySafe
       │    └─ summonOrObject
       └─ ability_cast_end
```

## Contract

- All 120 abilities are listed in `GameplayPlaybackManager` under
  `CONCEPT_RUNTIME_RECONCILED_ABILITIES`.
- Abilities that need toggle, charge, follow-up, or lifecycle behavior are also
  listed in `CONCEPT_STATE_MACHINE_ABILITIES`.
- Abilities whose concept depends on visible world objects, fluids, ground marks,
  proxies, projectiles, or other physical cues are listed in
  `CONCEPT_PHYSICAL_VISUAL_ABILITIES`.
- Abilities that must explicitly skip the caster, allies, or friendly summons are
  listed in `CONCEPT_FRIENDLY_SAFE_ABILITIES`.
- Abilities that create or control a summon/object lifecycle are listed in
  `CONCEPT_SUMMON_OBJECT_ABILITIES`.

## Current Audit

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-ability-concept-reconciliation.ps1 -NoTimestamp
```

Expected result:

```text
Major gap rows: 0
```

This means every ability has an explicit concept route in code. It does not
replace live review of visual taste, normal-control input timing, or user-facing
feel; it gives the harness a hard runtime signal to inspect for those reviews.
