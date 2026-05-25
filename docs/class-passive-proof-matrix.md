# Class Passive Proof Matrix

Use this before style/ability review. The goal is to prove each class passive with text evidence first, then use visual review only for the parts that are inherently visual.

```
╔════════════╦════════════════════╦═════════════════════╦══════════════╗
║ Class      ║ Passive            ║ Best Proof Type     ║ Status       ║
╠════════════╬════════════════════╬═════════════════════╬══════════════╣
║ Terra      ║ Immovable          ║ A/B mining + damage ║ instrumented ║
║ Hydro      ║ Tidal Flow         ║ HP/shield deltas    ║ needs run    ║
║ Aero       ║ Skybound Tempo     ║ position/velocity   ║ needs run    ║
║ Corruptus  ║ Dark Resurrection  ║ stack/HP/lockout    ║ needs run    ║
╚════════════╩════════════════════╩═════════════════════╩══════════════╝
```

## Terra

### Miner's Affinity

Purpose: confirm pickaxe block damage is actually increased by 50%.

Proof:

1. Prepare no-class baseline in Adventure.
2. Mine one identical stone/solid block with the same pickaxe.
3. Prepare Terra in Adventure.
4. Mine one identical stone/solid block with the same pickaxe.
5. Compare server log lines.

Expected Terra line:

```text
[MOTM] Terra mining affinity applied: ... damageBefore=<n> damageAfter=<n*1.5> multiplier=1.500
```

The no-class baseline should not emit that line.

### Immovable Knockback

Purpose: confirm incoming knockback is reduced by 20%.

Proof:

1. Trigger a native hit/knockback source against a non-Terra class and capture movement/knockback evidence.
2. Trigger the same source against Terra.
3. Compare logs and displacement.

Expected Terra line:

```text
[MOTM] Incoming knockback passive applied: ... multiplier=0.800 ...
```

If Hytale damage does not include a knockback meta component, the log will say:

```text
[MOTM] Incoming knockback passive had no knockback component...
```

That means the hit source cannot prove knockback and we need a different native source.

### Low-Health Regen

Purpose: confirm Terra heals 1% max HP per second below 30% HP.

Proof:

1. Disable test protection or it will refill HP and invalidate the test.
2. Put the player below 30% HP in Adventure.
3. Wait at least two passive ticks.
4. Compare health before/after from `/motm dev effects`.

Expected: HP rises by roughly 1% max HP per second while below the threshold.

### Cave Vision

Purpose: confirm underground Terra gains the dynamic light.

Proof:

1. Move to an underground lane with at least 3 solid blocks above the player within the scan distance.
2. Set class Terra.
3. Run `/motm dev effects` and visual check.

Expected: `isTerraCaveVisionActive` should be true once exposed through a dev status/proof command; visually the player should get cave light. This still needs a dedicated underground lane for clean testing.

## Hydro

### Spell Vamp

Purpose: confirm ability damage heals for 3% of damage dealt.

Proof:

1. Turn off test protection for the HP delta or set HP below max by controlled damage.
2. Set class Hydro and a damaging Hydro style.
3. Capture HP before.
4. Cast a Hydro ability into a stationary target.
5. Capture HP after.

Expected: `healed ~= damageDealt * 0.03`. The server path is `GameplayPlaybackManager.applyPostDamageClassPassives -> ClassPassiveManager.onDamageDealt`.

### Aqua Barrier

Purpose: confirm Hydro shield exists and is depleted before later Hydro defensive overlays.

Proof:

1. Set class Hydro in Adventure.
2. Let the passive tick until `/motm class` reports Aqua Barrier HP.
3. Take controlled damage.
4. Confirm damage first reduces the `hydro_passive_aqua_barrier` shield HP.

Expected: `/motm class` reports `Aqua Barrier <hp> HP`; after damage, shield HP drops or cooldown starts.

### Swim Speed And Oxygen

Purpose: confirm water-only movement/oxygen behavior.

Proof:

1. Use a water test lane.
2. Capture `/motm dev effects` while not swimming.
3. Enter water and move forward.
4. Capture `/motm dev effects`.

Expected: `hydroSwimming=true` once exposed or movement/velocity evidence shows the 40% swimming boost; oxygen max modifier is applied.

## Aero

### Skybound Movement

Purpose: confirm Aero movement is faster without causing sideways slow.

Proof:

1. Set no class; record position.
2. Hold forward or strafe for a fixed duration and record displacement.
3. Set class Aero; repeat from the same lane.
4. Compare displacement and `/motm dev effects`.

Expected: Aero displacement is about 25% higher under the same input, with no `slowMultiplier` effect.

### Signature Energy

Purpose: confirm native Hytale energy max is increased by 80%.

Proof:

1. Capture `/motm dev effects` with no class.
2. Set class Aero and wait for a passive tick.
3. Capture `/motm dev effects`.

Expected: signature max is multiplied by 1.8 if the native stat exists for the current player state.

## Corruptus

### Dark Resurrection Stacks

Purpose: confirm hostile kills add stacks up to 3.

Proof:

1. Set class Corruptus.
2. Spawn controlled targets.
3. Kill three targets with Corruptus abilities.
4. Run `/motm class` after each kill.

Expected: `Dark Resurrection: 1/3`, `2/3`, then `3/3`.

### Resurrection And Lockout

Purpose: confirm lethal damage heals to half HP and starts the 10 minute passive lockout.

Proof:

1. Reach 3 stacks.
2. Disable test protection.
3. Take controlled lethal damage.
4. Capture `/motm class` and `/motm dev effects`.
5. Kill another target during lockout.

Expected: player survives at roughly 50% HP, stacks clear, lockout shows near 600 seconds, and new stacks are blocked during lockout.

## Immediate Harness Needs

```
┌──────────────────────┬──────────────────────────────────────────────┐
│ Need                 │ Why                                          │
├──────────────────────┼──────────────────────────────────────────────┤
│ controlled HP command│ regen, spell-vamp, barrier, resurrection     │
│ movement delta script│ Aero and Hydro swim should be numeric        │
│ water/underground lane│ Hydro swim/oxygen and Terra cave vision     │
│ passive status command│ expose cave/swim/underwater/shield/stacks   │
│ knockback source list│ some damage sources have no knockback meta   │
└──────────────────────┴──────────────────────────────────────────────┘
```
