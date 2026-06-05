# Final Shared Perk Runtime Status

Updated: 2026-05-28

This document records the current gameplay contract for the 20 shared perk choices. Perks are passive choices unlocked every 10 levels through level 100. Any player can choose any perk, regardless of class.

```
+------------+-----------------------------------------------------------+
| Theme      | Runtime Perks                                             |
+------------+-----------------------------------------------------------+
| Aero       | Twinkletoes, Accelerate, Bunny Hop, Big Strides,          |
|            | Sharpshooter                                              |
| Hydro      | Neptune's Grace, Semiaquatic, Big Lungs, Rainy Day,       |
|            | Freezing Winds                                            |
| Corruptus  | Ignite, Desperation, Haunting, Vampirism, Terror          |
| Terra      | Heavyweight, Eco-friendly, Mole Man, Blacksmith, Toolsmith|
+------------+-----------------------------------------------------------+
```

Archived perk data note: the historical tier-2+ perk JSON entries are not part of current canon. Runtime loading filters them out and exposes only these 20 shared choices.

## Implemented Runtime Hooks

| Perk | Runtime behavior |
| --- | --- |
| Twinkletoes | Reduces fall damage by 20% in the incoming damage hook. |
| Accelerate | Ramps sprint movement speed up to +5% over 3 seconds while sprinting. |
| Bunny Hop | Watches sprint+jump transitions and applies a temporary momentum fallback for 2-5 hops. |
| Big Strides | Refills/compensates stamina during the first 3 seconds of sprinting. |
| Sharpshooter | Multiplies MOTM projectile speed by 1.15. Native Hytale projectile coverage remains an explicit proof target. |
| Neptune's Grace | When projected health drops below 10%, heals 40% max HP and starts a 25 second cooldown. |
| Semiaquatic | Ramps swim movement speed up to +20% over 5 seconds while swimming. |
| Big Lungs | Applies +10% max stamina and +10% max oxygen through native stat modifiers. |
| Rainy Day | Uses Hytale weather resources/tracker to detect rain and applies periodic regen while raining; `/motm dev passive rainy-day auto` forces a rain proof when the weather API accepts the asset. |
| Freezing Winds | Below 20% projected HP, slows nearby enemies by 50% for 5 seconds and applies a cold visual effect. |
| Ignite | On successful hit, burns enemies in a 6 block radius for 1% caster max HP each second for 5 seconds, then starts cooldown. |
| Desperation | Below 70% HP, outgoing native and MOTM damage is multiplied by 1.10. |
| Haunting | On kill, spawns up to 3 temporary ghost allies for 60 seconds. Ghosts attack nearby hostile NPCs every 2 seconds. |
| Vampirism | Heals the player for 10% of successful native or MOTM damage dealt to mobs. |
| Terror | Uses the closest proven native hook: if the player hits with a native weapon while signature energy is full, nearby enemies are stunned for 3 seconds and the perk starts a 20 second cooldown. |
| Heavyweight | Reduces incoming knockback by 15% and increases outgoing knockback by 4% when a native knockback component exists. |
| Eco-friendly | Bare-hand natural earth/grass block damage in open space grows a temporary no-drop tree structure, pushes nearby NPCs, grants 5% damage reduction for 5 seconds, then uses a 15 second cooldown window. |
| Mole Man | Adds +10% mining multiplier while Terra cave vision says the player is underground. |
| Blacksmith | Player craft events mark eligible crafted armor; marked equipped pieces add 20% of that piece's native armor resistance as extra reduction, capped at 20% total. The enhanced item can be used by others because the bonus is stored on item metadata. |
| Toolsmith | Player craft events mark eligible crafted native tools/weapons, label them `Toolsmith Perk +25% Durability`, and raise max/restored durability to 125%; the enhanced item can be used by others because the bonus is stored on item metadata. |

## Verification Entry Points

Run the static/no-resource gates:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/audit-no-resource.ps1
powershell -ExecutionPolicy Bypass -File scripts/build-install.ps1
```

Run the runtime perk proof harness in an already loaded world:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-agent-observability-baseline.ps1 -WorldName Main -RunId perk-runtime-YYYYMMDD -ScenarioId command-observability-smoke
```

The runtime proof writes:

```text
audits/perk-runtime/<run-id>/report.md
audits/perk-runtime/<run-id>/<proof-name>.log
```

Movement perks still need real sprint/swim lane stimuli for high-confidence live proof. Eco-friendly and crafting perks now have native hooks, but the strongest final proof is still an in-world bare-hand grass punch and a real crafted armor/tool item because those are UI/world actions, not chat-only simulations.
