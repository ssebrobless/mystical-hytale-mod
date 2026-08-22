# MOTM Testing Guide — How to Self-Test All 120 Abilities, 4 Passives, 20 Perks

Audience: the mod owner playing the mod to verify content by eye and by feel.
Companion data: `_ability_catalog.json` (machine-readable 120-row catalog), the four
`CONTACT_<class>.png` sheets in `qa-captures/`, and `_crashes.md`.

---

## 0. TL;DR answers to the setup questions

- **Is testing simple?** Two paths. **Normal play** (equip the Spellbook, pick class/style,
  cast with mouse/ability keys) exercises the *real* player experience. **Dev fast-path**
  (`/motm dev ...`) casts any ability on demand with free-cast on — best for churning through
  all 120 quickly. Use both: dev-path for coverage, normal-path to confirm the UX is real.
- **Are the UIs functional?** Yes. The Spellbook page (class/style/perk pick) and the status
  HUD are wired. As of 2026-08-22 the buff/debuff status strip AND the passive tracker render
  **text-only** — color-coded labels (buffs green, debuffs red; e.g. `SHIELD 14HP`,
  `Aqua Barrier 34/34`). Icons are intentionally omitted (toggling icon Sprites at runtime
  NRE-crashed the client). One remaining gap: spending stat points has no buttons (command
  only). See §2.
- **Does the environment matter?** A lot. Training dummies stuck in the ground only prove
  ~1/3 of the design. Projectiles need a *moving* target; dashes/leaps need a target to
  close on and ledges; AoE needs *groups*; support needs an *ally*; reactions need a
  *second element*; scaling/TTK needs *real hostiles*, not a 1,000,000-HP dummy. See §3.
- **Better setup?** A built arena with: open lane + moving enemies, a 3–5 enemy cluster, a
  wall + a ledge/pit, a water pool and a lava pool, one ally NPC, and a scaled real-hostile
  pen. Plus `/motm dev passive reaction <a> <b>` to trigger reactions solo. See §3.3.

---

## 1. How the player reaches and triggers each thing (normal play)

Source: `PlayerSessionLifecycleActions`, `SpellbookPage`, `SpellbookInputHandler`,
`MotmSpellbookInteraction`, `MotmCommand`, `LevelingManager`, `PerkManager`.

1. **First join** grants the **Spellbook** item and installs the HUD.
2. **Open the Spellbook**: hold **crouch + Use** (or chat fallback `/motm spellbook`).
3. **Pick class → style** in the Spellbook page (buttons), or `/motm class <c>` + `/motm style <s>`.
4. **Abilities auto-bind** to the style's 3 slots (fixed order 1‑2‑3). **Cast**:
   - **Left-click / Right-click / Use** = slots 1 / 2 / 3, or the **Ability 1/2/3** keybinds.
5. **Perks**: unlock at levels 10, 20, … 100 (1 pick per milestone, 10 tiers). Choose in the
   Spellbook **Perks** tab or `/motm perks` then `/motm select <perkId>`.
6. **Stats**: +2 points per level; spend with **`/motm stats spend <stat> <n>`** (no UI buttons yet).
7. **Level/XP** for testing: `/motm dev level set <n>` and `/motm dev xp add <n>`.

### Dev fast-path (recommended for full-coverage review)
```
/motm dev class set <terra|hydro|aero|corruptus>
/motm style <styleId>
/motm dev test mobs <close|stationary|cluster|line|surround>
/motm dev arena build                   # spawn the full test arena (walls, ledge, water+lava pools, dummies)
/motm dev arena clear                   # tear the arena down
/motm dev test ability <abilityId>     # free-cast, targets nearest test NPC
/motm dev test reset                    # scrub arena between styles
/motm dev passive status                # dump passive/stack/resource state
/motm dev passive reaction <elemA> <elemB>   # trigger an elemental reaction SOLO
/motm dev perks grant <perkId|all>      # equip perks to test them
```
Free-cast (resource costs off) is on for dev tests, so cooldowns/resources won't block you.

---

## 2. UI functionality audit (what to trust, what's blind)

| UI | Wired? | Notes for testing |
|---|---|---|
| Spellbook page (class/style/perk pick) | Yes | Primary nav. No dedicated *Style* tab — style picked within class view. |
| Creative Spellbook (dev) | Yes (dev-only) | Sandbox page for review. |
| Status HUD (class:style line, XP bar, resource bar, 3 ability slots) | Yes | Ability slots show name + cooldown timer as text (icons omitted, same reason as the strip). |
| Buff/Debuff status strip | Yes (text-only) | Active buffs/debuffs show as color-coded text tags (green buff, red debuff), not icons — e.g. `SHIELD 14HP`. Abbreviated; confirm detail by behavior or dev status. |
| Passive tracker | Yes (text-only) | Passive name + timer/stack shows as color-coded text (top-right), not icons — e.g. `Aqua Barrier 34/34`, `Tidal Flow`. |
| **Stat-point spending** | **No buttons** | Command only: `/motm stats spend ...`. |
| Spellbook `.ui` casing preflight | Not validated | HUD casing IS validated; spellbook page is not — watch for a client disconnect if paths drift. |

**Testing implication:** the buff/debuff strip now shows applied effects as text tags, so
"did the status effect land?" can be read from the HUD strip, confirmed by mechanics (the
target slows/burns/roots), or the dev status dump. There is no icon art — judge by the text
label plus behavior.

---

## 3. Testing environment — why it matters and how to build it right

### 3.1 What the current dummy setup proves and doesn't
`/motm dev test mobs` spawns **`Test_Dummy_Stationary`** (+ a floating **Bat** in `standard`).
Modes: **close, stationary, cluster (5), line (3 @ 4/8/12m), surround (4)**.
Dummy HP ≈ **1,000,000** (vanilla role), so damage numbers look huge and **TTK is meaningless**.

Stuck grounded dummies CANNOT exercise:
- **Projectile accuracy/lead** (24 abilities) — need a *moving* target.
- **Dash/leap/dive gap-closers** (13) — need distance to close and ledges/height.
- **AoE fan-out** (28 field + cones) — need a *group*, not one dummy.
- **Support/heal/buff-ally** (15) — need a *friendly* target; dummies aren't allies.
- **Airborne** (10) — need elevation to launch from/into.
- **Knockback payoff** — need a pit/ledge/wall to knock enemies into.
- **Elemental reactions** (6) — need a *second element* on the target.
- **Real damage/TTK & scaling** — need real hostiles at a title band, not a 1M-HP dummy.

### 3.2 Elemental reactions (need two elements)
`data/reactions/elemental_reactions.json` — every cross-pair reacts:

| Reaction | A + B | Bonus dmg | Applies |
|---|---|---|---|
| Storm Surge | aero + hydro | 12% | stun, shocked |
| Mud Snare | hydro + terra | 10% | root, slow |
| Dust Cyclone | aero + terra | 10% | blind, knockback |
| Black Steam | corruptus + hydro | 11% | dot, slow |
| Gravebind | corruptus + terra | 12% | root, vulnerability |
| Hellstorm | aero + corruptus | 14% | burn, stun |

A single player is one element, so **solo reaction testing uses**
`/motm dev passive reaction <elemA> <elemB>` (element→mark: aero=SHOCKED, hydro=WET,
terra=COMBUSTIBLE, corruptus=CURSED). For real feel, a **second player** of another class.

### 3.3 Recommended arena layout (build once, reuse)
Map ability categories → the arena feature that proves them:

```
            ┌─────────────────────── TEST ARENA ───────────────────────┐
            │  [A] Open lane 20m  ── moving enemy runs the lane          │  projectiles, dashes, leaps
            │  [B] Cluster pen (5 mobs, 3m spacing)                      │  AoE fields, cones, volleys
            │  [C] Wall + 4m ledge over a 6m pit                         │  knockback, dive, launch, airborne
            │  [D] Water pool (4x4) next to Lava pool (4x4)              │  terrain fields, wet/burn, hazards
            │  [E] Ally NPC pedestal (friendly)                          │  heal/shield/buff-ally, summons guard
            │  [F] Scaled-hostile pen (real mob_base_stats, title band)  │  true damage/TTK, reactions, passives
            └────────────────────────────────────────────────────────────┘
```
- **[A] moving enemy:** spawn a real mobile hostile (vanilla creature) rather than a dummy.
- **[F] scaling:** title bands are Intern/Apprentice/Journeyman/Master (`MobScalingManager`);
  set your `/motm dev level` and fight `mob_base_stats` hostiles (goblin 25 HP, trork 80, …)
  to read honest damage and TTK instead of chipping a 1M-HP post.
- Keep `close`/`cluster`/`line`/`surround` dummy modes for pure *visual* checks; use [A]–[F]
  for *behavioral* truth.

---

## 4. Per-category observation checklist

For each ability, confirm BOTH: **(V)** the themed visual fires and looks on-theme, and
**(M)** the mechanic actually happens. Blind buff strip → judge (M) by behavior or dev status.

- **Projectiles / lines / volleys / chains (24):** does it *leave the caster*, travel, and hit a
  *moving* target? correct arc/speed? impact fx on hit? damage applied?
- **Dashes / leaps / dives / teleport (13):** does the *caster move* the stated distance/height?
  trail fx? on-hit/landing effect? lands you next to the target?
- **Fields / zones / ground effects (28):** ring/pool renders at target ground? persists the
  stated duration? pulses damage/effect on everything inside? (tide_pool now safe.)
- **Cones / self-bursts (9):** correct fan/radius around caster? hits everything in the arc?
- **Summons (8):** correct creature model spawns, is allied, attacks/guards, despawns on timer?
  (Snow Imp, Frosty, Swamp Monster, Raise Dead, Void Spawn, Scarak Egg, Locust Queen, Shadow Step.)
- **Transforms (4):** player model swaps to the creature (Smoke Form, Pterodactyl, Triceratops,
  T‑Rex)? transform abilities/stats active? reverts on timer?
- **Support / ally (15):** with an ally present — heal/shield/buff actually lands on the ally?
- **Self-buffs / toggles (26):** effect active for the duration? (behavior/dev-status, not HUD icon.)
- **DoT / status / reactions:** target burns/roots/slows over time; reaction bonus + effect fires
  when the second element lands.

---

## 5. Class passives — how to trigger and observe (4)

Source: `ClassPassiveManager` + `data/classes/*.json`.

| Class | Passive | Trigger to test | What you should observe |
|---|---|---|---|
| Terra | **Immovable** | take knockback; drop below 30% HP; mine with a pickaxe; go underground | ~20% less knockback; 1%/s regen under 30%; ~50% faster mining; cave vision. Verify KB/regen with `/motm dev passive knockback` / `health`. |
| Hydro | **Tidal Flow** | deal ability damage; swim; go underwater; take a hit with barrier up | heal 3% of damage dealt; +40% swim; +50% breath; whole-body Aqua Barrier (10% max HP) depletes first, 8s cd. |
| Aero | **Wind Walker** | just move; check energy bar | +25% move speed always; +80% native energy bar. (No duplicate jumps — vertical is per-ability.) |
| Corruptus | **Soul Harvest** | get hostile kills (build 5 stacks); take lethal at 5 stacks | +2% dmg & +1% DR per stack; at 5, lethal → heal to 50%, clears stacks, 10‑min lockout. Stacks best read via `/motm dev passive status` (HUD tracker is faint). |

Hard-to-see: Soul Harvest stacks and all silent stat modifiers → use `/motm dev passive status`.

## 6. Perks — how to trigger and observe (20 shared, 5 per class)

Grant with `/motm dev perks grant <perkId>`; equip in Perks tab at Lv10+ in normal play.

**Terra:** Heavyweight (−15% KB taken/+4% dealt — dev knockback), Eco-friendly (punch grass in
the open → tree + push + 5% DR), Mole Man (underground night vision + mining), Blacksmith (crafted
armor +20% — craft & compare), Toolsmith (crafted tools +25% durability, shareable).
**Hydro:** Neptune's Grace (drop <10% HP → heal 40%, 25s cd), Semiaquatic (swim speed ramps to
+20% over 5s), Big Lungs (+10% stamina & breath), Rainy Day (stand in rain → +100% regen),
Freezing Winds (<20% HP → KB + 50% slow nearby, 5s).
**Aero:** Twinkletoes (−20% fall dmg — take a fall), Accelerate (sprint 3s → +5%), Bunny Hop
(sprint-jumps chain 2–5 hops), Big Strides (first 3s sprint = no stamina), Sharpshooter (+15%
projectile speed — compare a projectile).
**Corruptus:** Ignite (on hit, enemies within 6 burn 1% max HP/s for 5s, 15s cd), Desperation
(<70% HP → +10% dmg), Haunting (kill → ghost ally 1 min, up to 3), Vampirism (heal 10% of
damage dealt to mobs), Terror (native weapon ultimate → stun within 7 for 3s, 20s cd).

Hard-to-see (stat-only / conditional): Heavyweight, Blacksmith, Toolsmith, Big Lungs, Big Strides,
Sharpshooter, Desperation → confirm with a **with/without damage or number comparison** or the
`/motm dev passive combat|crafting|velocity|knockback` readouts, since there's no buff HUD.

---

## 7. Full 120-ability catalog

> `See` = expected themed visual; `Do` = mechanic to confirm; `Test needs` = what the
> environment/target must provide. Conditions are heuristic — trust your eyes on the sheet.

### Terra
__omp_magic("", "TERRA%")

### Hydro
__omp_magic("", "HYDRO%")

### Aero
__omp_magic("", "AERO%")

### Corruptus
__omp_magic("", "CORRUPTUS%")
