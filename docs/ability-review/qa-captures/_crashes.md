# Client-crash findings during visual QA harness (2026-08-21)

## tide_pool (hydro / saltwater) — CLIENT CRASH — FIXED
- Symptom: client `NullReferenceException` (renderer), crash dialog, client process dies.
- Reproduced cleanly: baseline 0 NRE -> `dev test ability tide_pool` -> 1 NRE + crash window.

### Root cause (confirmed by isolation builds)
- The crash is tide_pool's **field visual proxy** (the persistent field-ring aura),
  NOT the fluid terrain and NOT the `Water_Run` particle.
- Isolation evidence:
  1. Terrain hypothesis DISPROVEN: `mudpit` places 89 `Fluid_Water` disc blocks with no
     crash; `tide_pool` placed 57 and crashed. Rebuilding tide_pool to skip terrain
     entirely STILL crashed -> terrain exonerated.
  2. Particle hypothesis DISPROVEN: `Water_Run` is used by geyser/hidrosis/overheat/
     piercing_rain/rainbow/vapor_vanish/waverider, all of which cast earlier in the same
     run with no crash -> particle exonerated.
  3. Proxy hypothesis CONFIRMED: excluding tide_pool from `shouldUseFieldVisualProxy`
     (so it does not spawn the renderless field-ring proxy) -> baseline 0 NRE, client
     survives, crash window 0. This matches the pre-existing crash-avoidance for
     `lava_pool` (also excluded from the proxy for the same client-NRE reason).

### Fix
- `FieldRuntimeSpecs.shouldUseFieldVisualProxy`: also exclude `tide_pool` and `oil_spill`
  (id + terrain_effect) so they do not spawn the client-crashing field-ring proxy.
- `TerrainPlacementHytaleAdapter`: routed `TIDE_POOL`/`OIL_SPILL` temporary terrain from
  the tall stacked `placeGroundedFluidCylinderSelection` to the shallow, mudpit-proven
  `placeGroundedFluidDiscSelection`, and removed the now-unused cylinder method. (Terrain
  was not the crash cause, but the shallow disc is client-safe and more thematically
  correct for a tide pool; it preserves the water-pool look now that the aura proxy is
  suppressed.)
- Net gameplay: tide_pool still runs its 6s field, radius 4, slow + self buff. Visuals:
  cast splash (player), impact splash (target), shallow water disc terrain. Only the
  crashing persistent field-ring aura proxy is gone.
- Verified: 317/317 tests pass; in-world cast -> 0 NRE, client alive.

## swamp_monster (hydro / freshwater) — NOT A CRASH (earlier mis-attribution, corrected)
- The client had already crashed on `tide_pool` ~39s earlier; the harness kept sending
  casts (incl. swamp_monster) into a dead client whose server relay kept logging. No
  summon-specific crash exists; Snow Imp / Frosty summons captured fine.

## Follow-up risk to watch (not yet a confirmed bug)
- Any other ability that spawns the field-ring proxy with a water/fluid loop near existing
  water could theoretically hit the same renderer NRE. None observed crashing in the 120-
  ability run except tide_pool. Re-check if new field abilities are added.

## HUD CustomUI icon Sprites — CLIENT CRASH on world entry — FIXED (2026-08-22)
- Symptom: on world join the client logs `Failed to apply CustomUI HUD commands
  (CustomUI Set command couldn't set value. Selector: #<node>.Visible -> ...
  Object reference not set to an instance of an object.)` and hard-disconnects
  ("Disconnecting with error during stage InGame", `left with reason: Crash`).
- Root cause (confirmed by isolation builds): toggling any pre-declared **icon**
  Sprite/Group node's `.Visible` at runtime NREs the client renderer. Proven by
  progressively repointing the offending `#TrackerRow2Icon*` node to known-good
  Common textures (`Stamina.png`, `Health_Potion.png`) and renaming it
  (`IconShield` -> `IconGuard`): the NRE persisted on the *same* node position
  regardless of texture or name, while text/Label writes on the same rows applied
  fine. Node id collision, missing texture, and lazy-row instantiation were all
  ruled out (row 1 and row 2 nodes are byte-identical; only the row whose active
  variant is set `Visible=true` crashes).
- Fix: render all three icon surfaces **text-only** in `MotmStatusHud`:
  - `renderStatusSlot` (buff/debuff strip): color-coded Tag/Detail/Counter labels
    (`#a8ff9a` buff, `#ff9a9a` debuff); tone-bg / arrow / cooldown Sprites left at
    their `.ui` default (hidden), never toggled.
  - `renderTrackerRow` (passive tracker): color-coded Name + Timer labels; the
    per-family `#TrackerRowNIcon*` Sprites are never toggled.
  - `renderAbilityIcon` (ability slots): no-op; the `#AbilityNIcon*` Sprites/Groups
    (incl. `IconShield` whose Background is `ShieldAbility@2x.png`) are never toggled.
  Only `.Visible`/`.Text`/`.Style.TextColor`/`.Value` writes remain — all proven-safe.
- Verified in-world (2026-08-22): 0 CustomUI Set errors, 0 disconnects, client alive;
  HUD shows XP bar, class:style line, live buff strip (`SHIELD 14HP`), and passive
  tracker (`Tidal Flow`, `Aqua Barrier 34/34`) as color-coded text.
