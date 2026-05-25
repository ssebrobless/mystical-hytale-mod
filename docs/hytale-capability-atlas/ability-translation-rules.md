# Ability Translation Rules

Updated: 2026-05-24

## Core Rule

When the concept names a literal object, start object-first.

```
Concept says "wall / pillar / flower / sapling / lava / mud / gem"
  └─▶ try real block, fluid, prefab, or model object first
       └─▶ add particles only to sell motion, impact, and magic

Concept says "coating / skin / petrified / poisoned / rooted"
  └─▶ try tight EntityEffect model/VFX/tint first
       └─▶ add particles only if they hug the body

Concept says "projectile made of material"
  └─▶ try real projectile or inert model proxy first
       └─▶ add trail/impact particles second
```

## Decision Tree

```
╔══════════════════════════════════════════════════════════════════╗
║ Ability Concept                                                  ║
╚══════════════════════════════════════════════════════════════════╝
        │
        ▼
┌──────────────────────────────┐
│ Does it need collision?       │
└──────────────┬───────────────┘
               │yes
               ▼
       temporary block/fluid
               │
               ▼
        proof cleanup/safety

               no
               │
               ▼
┌──────────────────────────────┐
│ Does it attach to a body/item?│
└──────────────┬───────────────┘
               │yes
               ▼
       EntityEffect/model VFX
               │
               ▼
        proof tint/readability

               no
               │
               ▼
┌──────────────────────────────┐
│ Does it move through space?   │
└──────────────┬───────────────┘
               │yes
               ▼
     projectile/proxy + trail
               │
               ▼
        proof hit trajectory

               no
               │
               ▼
        particle/model field
```

## Terra Examples From Current Review

| Style | Ability | Primary Route | Fallback If Risk Fails |
| --- | --- | --- | --- |
| Metal | Iron Wall | 3x3 temporary iron block wall placed one block in front of player-facing direction | inert model/proxy wall plus knockback zone |
| Metal | Metal Coat | stone-skin style model/VFX coating recolored dark gray | body tint plus metallic sparks only if VFX breaks |
| Metal | Alloy Enhancement | same coating on held item/body plus dark impact/swing effect and three boosted uses | impact-frame-only visual if held-item coating cannot be isolated |
| Magma | Lava Pool | floor-level friendly lava field centered on caster, with speed/damage immunity guards | lava circle particles plus invisible gameplay field |
| Magma | Obsidian Skin | small guarded lava/obsidian shell, then dark purple-black coating | no real shell if client crash repeats; use blocky model shell proxy |
| Magma | Magma Sling | real projectile or inert lava-blob model proxy, no health bar or AI | fire/lava particles with visible travel line |
| Stone | Pillar Strike | stack one-by-one temporary stone pillar under target, then vertical launch | pillar model proxy if block placement under NPC is unstable |
| Arbor/Bloom | placed plants | place on top of support block, never replace floor | model/prefab proxy at ground point |
| Gem | Lapidary | controllable object with HP, ideally green 2x2x2 gem/block cube | model proxy with HUD/logged HP if block cube collision is awkward |

## Mechanics Must Match Input Reality

Do not accept `/motm dev test ability` as the only proof for gameplay abilities.
Use it for setup/debug, then prove the player-facing route:

| Ability Type | Required Proof |
| --- | --- |
| left-click/slot cast | spellbook/item input log and cast begin/end log |
| movement ability | movement.before/movement.after plus visual capture |
| jump/landing ability | armed state, jump/land detection, target hit count |
| buff that persists after swapping | active item id, swap event, charge consume/expire logs |
| weapon/tool buff | item category accepted/rejected, charge count, damage/durability result |
| ground field | center, radius, tick count, targets affected, cleanup |
| projectile | origin, aim vector, travel ticks, hit target/surface, cleanup |
| summon/object | entity id, role/model id, HP state, ownership, cleanup |

## Friendly Safety Rule

Every AoE, DoT, field, explosion, pull, stun, slow, root, and terrain hazard must
filter out:

- caster
- allies
- allied summons
- converted/charmed allied mobs
- friendly deployables

If the underlying Hytale object has natural harmful behavior, MOTM must either
cancel/offset it or use a visual fallback.

## Ability Acceptance Shape

```
PASS requires all four:

1. Concept match
   └─ user agrees it reads like the intended ability
2. Runtime proof
   └─ structured logs prove cast/effect/hit/cleanup
3. Safety proof
   └─ no caster/ally harm, no orphaned blocks/entities/effects
4. Stability proof
   └─ no client/server crash, no missing assets/roles/SystemIds
```

