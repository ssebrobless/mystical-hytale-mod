# Final Shared Perk Runtime Status

Updated: 2026-05-25

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
| Rainy Day | Data and runtime residual are recorded; current weather/rain exposure API still needs proof before regen is applied. |
| Freezing Winds | Below 20% projected HP, slows nearby enemies by 50% for 5 seconds and applies a cold visual effect. |
| Ignite | On successful hit, burns enemies in a 6 block radius for 1% caster max HP each second for 5 seconds, then starts cooldown. |
| Desperation | Below 70% HP, outgoing native and MOTM damage is multiplied by 1.10. |
| Haunting | On kill, spawns up to 3 temporary ghost allies for 60 seconds. Ghosts attack nearby hostile NPCs every 2 seconds. |
| Vampirism | Heals the player for 10% of successful native or MOTM damage dealt to mobs. |
| Terror | Data-defined residual; needs a proven native weapon ultimate-ready hook before it can stun on ultimate use. |
| Heavyweight | Reduces incoming knockback by 15% and increases outgoing knockback by 4% when a native knockback component exists. |
| Eco-friendly | Data-defined residual; needs a proven bare-hand grass block event with player mapping before tree growth can be safe. |
| Mole Man | Adds +10% mining multiplier while Terra cave vision says the player is underground. |
| Blacksmith | Data-defined residual; needs native crafted-armor event/item metadata proof. |
| Toolsmith | Data-defined residual; needs native crafted-tool/weapon event/item metadata proof. |

## Verification Entry Points

Run the static/no-resource gates:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/audit-no-resource.ps1
powershell -ExecutionPolicy Bypass -File scripts/build-install.ps1
```

Run the runtime perk proof harness in an already loaded world:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/run-perk-runtime-proofs.ps1 -WorldName "MOTM Creative Test" -RunId perk-runtime-YYYYMMDD
```

The runtime proof writes:

```text
audits/perk-runtime/<run-id>/report.md
audits/perk-runtime/<run-id>/<proof-name>.log
```

Movement, weather, crafting, and native-ultimate perks still need specific Hytale stimuli. The harness records those as residuals instead of pretending screenshots are proof.
