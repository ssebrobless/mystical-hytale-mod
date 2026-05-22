# Phase 9 Residuals

Non-blocking findings discovered during successful style validation runs. These should be batched for Claude or a later polish pass after each class's style group, unless one starts causing crashes, failed casts, or failed acceptance gates.

## Terra

- `magma`: Hytale logs `Reloading nonexistent role motm_projectile!` when `magma_sling` spawns the projectile visual proxy. The ability still casts and launches successfully. Likely a cross-cutting projectile proxy-role issue, not Magma-specific.

## Hydro

- `snow`: `frosty` spawns and casts successfully, but the server logs `Unmapped NPC type encountered. npcTypeId=Golem_Crystal_Frost, modelAssetId=Golem_Crystal_Frost`. This did not block summon creation or crash the world; later mob-XP/type mapping should recognize MOTM summon proxy types or explicitly ignore them.

## Harness

- During Snow validation, sending `{ESC}` while no custom UI was open put Hytale in the pause menu, causing subsequent chat commands to be swallowed until returning to game. Acceptance recovered manually; future harness work should use a less ambiguous close-overlay primitive.
