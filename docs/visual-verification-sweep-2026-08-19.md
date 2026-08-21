# Visual Verification Sweep - 2026-08-19

Purpose: close the one residual the deterministic L1-L4 ability sweep could not
prove - that abilities RENDER their intended VFX on screen (color/shape/model),
i.e. the mod "appears as intended." The L1-L4 harness proved every ability is
wired, dispatched, client-rendered (particle spawn), and mechanically correct;
it could not prove the pixels look right. This sweep adds direct screenshot
evidence.

## Method

- In-world MOTM Creative Test, live client, driven headlessly.
- Per ability: reliable dev setup (free-cast on, `dev class set <c>`, `style <s>`,
  spawn `test mobs line` target), then a background cast loop firing the ability
  repeatedly while a dense screenshot burst (14-16 frames @ ~300 ms) is taken.
- Each frame inspected by a vision model against the ability's expected VFX from
  `ability-visual-manifest.json` (dominant color / shape / summoned model).

## Result: DIRECT VISUAL PASS across all 4 classes + major VFX families

| Class     | Ability        | Family            | Observed on-screen (matches intent)                       |
|-----------|----------------|-------------------|-----------------------------------------------------------|
| Terra     | Vines          | line-control+summon | green vines wrapping the target's legs + green spirit-root creature |
| Hydro     | Frozen Needles | projectile-volley | cyan/blue ice bolt particles                              |
| Hydro     | Snow Imp       | summon            | white snow-imp creature summoned by the target            |
| Aero      | Acid Rain      | ground-zone       | translucent green/yellow toxic mist area effect           |
| Corruptus | Raise Dead     | summon            | purple void wispy particles + summoned entity             |
| Corruptus | T-Rex Form     | transformation    | feathered rex dinosaur model + purple void sparks         |

Every observed effect matched its manifest color/family/model. No wrong-color,
no missing-model, no visual defect surfaced in any confirmed capture.

## Honest limitation (capture tooling, NOT a mod defect)

The dev-command inbox is a strict single-slot channel that fires ~1 cast / ~1.5 s,
so continuous VFX cannot be sustained, and the client runs first-person (the
player's own body is not visible). Consequently, headless screenshots cannot
reliably catch:
- sub-second flashes at a distant target (fast single projectiles, lightning
  bolts, ground-burst shockwaves), and
- player-centered transient effects (self-bursts, dashes, and player-model
  transformations in first person).

These families are still proven to RENDER by the deterministic sweep: `L3` in
`ability-verification-matrix.md` asserts each ability's particle system spawns
client-side with zero load/animation failures across all 120 abilities. What
this sweep adds is direct pixel confirmation for the catchable families above.

This is the same class of headless constraint documented for grounded-dash
measurement (client-authoritative + first-person + no continuous cast channel);
a frame-perfect aesthetic pass of every one of the 120 abilities genuinely needs
a human watching in-world (or a third-person / video capture), which automation
cannot fully substitute.

## Verdict

- Representative visual pass: PASS (4/4 classes, 6 abilities, families =
  projectile-volley, summon, transformation, line-control, ground-zone).
- Full-120 render: PROVEN by L1-L4 (particle spawn + runtime), 0 defects.
- Remaining aesthetic residual: a human-in-the-loop watch of the transient /
  self-centered effects. Cosmetic QA, not a functional gate.
