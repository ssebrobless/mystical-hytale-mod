# Terra 30-Ability Implementation Map

Date: 2026-05-24

Purpose: record the exact implementation understanding for all 30 Terra abilities: what each ability must do, what Hytale primitives can achieve it, the ideal route, and what proof is needed before the ability can be marked complete.

This map assumes Terra has no ability resource costs. Ability limits come from cooldowns, durations, charges, toggles, target caps, and cleanup rules.

## Shared Implementation Backbone

```
╔══════════════════════════════════════════════════════════════════════╗
║ TERRA ABILITY PIPELINE                                              ║
╠══════════════╦═══════════════════════════════════════════════════════╣
║ Input        ║ normal spellbook/hotbar/LMB/RMB controls             ║
║ State        ║ StyleManager cooldowns, toggles, charges, follow-ups  ║
║ Runtime      ║ GameplayPlaybackManager ability-specific primitives   ║
║ Visual       ║ HytaleAssetResolver + effects + blocks + projectiles  ║
║ Safety       ║ friendly/caster/summon filters before negative impact ║
║ Telemetry    ║ structured terra.cast/hit/visual/cleanup log lines    ║
║ Acceptance   ║ log proof + third-person visual user review           ║
╚══════════════╩═══════════════════════════════════════════════════════╝
```

## Primitive Priority

| Concept Type | First Choice | Fallback |
|---|---|---|
| Physical wall/pillar/object | temporary block selection | block-like proxy model |
| Ground flower/root/sapling | surface decoration block above support | proxy marker above ground |
| Projectile | `ProjectileModule.spawnProjectile` with existing `ProjectileConfig` | custom server-simulated projectile with non-living visual proxy |
| Body/item coating | `EntityEffect` + `ModelVFX` + top/bottom tint | short repeated effects if full coating cannot isolate body part |
| Lava/water/mud field | safe temporary fluid/block selection | visual-only blocks/particles with server-side field |
| Dust/sand/debris motion | trail + particles + line/field sweep | block-ring markers plus particles |
| Movement through/under ground | explicit movement state + safe exit resolver | visual-only dip/dash with surface reappear |
| Persistent object | proxy/temporary blocks + owner state + HP | temporary block cluster with log-only HP |

## Style: Quake

### 1. Stomp

What it must do:

- Left-click arms the next jump/landing.
- The player must actually jump/land for the AoE to fire.
- On landing, enemies in radius take the intended hit/knock/control.
- The crash path must be exercised with at least one real target.
- Friendly/caster targets are skipped.

How to achieve it:

- Keep the existing armed-stomp state machine.
- Use landing detection or the existing forced/dev landing only for tests.
- Use the proven Quake impact ring: `Mace_Signature_Shockwave`, `Block_Break_Stone_Dust`, `Earth_Brazier_Glow`.
- Add structured logs for arm, landing, center, radius, target count, friendly skips, and cleanup.

Ideal route:

- `StyleManager` records/permits cast.
- `GameplayPlaybackManager` stores `ArmedStomp`.
- On landing, run an AoE query at landing position and spawn the quake impact ring.

Proof gate:

- User presses the normal Stomp control, jumps, lands near targets.
- Logs show `targets>=1`.
- User sees ground flash/crack effect at the landing position.

### 2. Aftershock

What it must do:

- Apply an 8 block spherical AoE.
- Hit enemies only.
- Use the same visual language as Stomp: flash, crack, tremor.

How to achieve it:

- Use a self-centered AoE/field or immediate AoE pulse.
- Set radius to 8 in data/runtime.
- Reuse the Quake ring/particle composition.
- Log radius, center, targets, and skips.

Ideal route:

- Immediate AoE pulse with a single strong visual burst.
- Optional short-lived field visual if readability needs it.

Proof gate:

- One target inside 8 blocks is hit; one target outside is not.
- Friendly/caster skips are logged.
- User sees a readable crack/flash.

### 3. Sinkhole

What it must do:

- Target is made to look buried, not physically lowered.
- Target is rooted/controlled for the duration.
- Suffocation/damage ticks occur if specified.
- Target releases cleanly.
- Ground marker is visible: cracks plus brown dust around the buried location.

How to achieve it:

- Keep buried-look approach: lower-body/dark effect, root, dust, suffocation ticks.
- Use surface crack marker with repeated `Block_Break_Stone_Dust` and brown dust.
- Consider `BlockBreakingDecal` only after a runtime proof proves spawn/control.
- Track buried victim by field/target so release is deterministic.

Ideal route:

- `engageSinkholeField` applies effect/status and logs.
- `processFieldTick` applies suffocation ticks and refreshes dust.
- `releaseSinkholeField` removes effect/status state and logs.

Proof gate:

- Logs show engage, tick, release for the same target id.
- User can visually identify buried target location during the duration.

## Style: Metal

### 4. Iron Wall

What it must do:

- Spawn a 3x3 metal wall one block in front of the player.
- Wall should face the player so its full width protects them.
- Wall lasts 4 seconds.
- Cooldown should start when the wall disappears.
- Enemies overlapped/too close at spawn are pushed away from the player/wall.
- Visual is a grounded 3x4 mixed-metal wall that connects to the ground and reads as rising from the earth.
- No body coating should occur from Iron Wall.

How to achieve it:

- Use temporary block wall selection.
- Resolve stable player origin and forward vector.
- Clamp center to a near player-relative position, not cursor target.
- Use metal/iron block IDs verified in local assets.
- Push overlapped enemy refs immediately after placement.
- Track wall expiry and cleanup.

Ideal route:

- Temporary block selection using the strongest iron/metal block found locally.
- Delayed cooldown if current cooldown system cannot naturally start on disappearance.

Proof gate:

- User casts while facing several directions; wall appears in front every time.
- Enemy overlap push logs count and direction.
- Wall restores after 4s.

### 5. Metal Coat

What it must do:

- Defensive coating around the player.
- Visual should be the strong dark gray metal coating the user approved.
- It can affect the held item if that preserves the good visual.
- It must not fade into faint white smoke.

How to achieve it:

- Preserve the strong `EntityEffect`/`ModelVFX` route discovered during review.
- Use top/bottom tint palette close to iron-wall color.
- Avoid particle-only smoke as the main visual.
- Apply and remove by source ability.

Ideal route:

- Dedicated `MOTM_Proof_Coating_Metal` or final renamed effect with dark gray top/bottom tint and strong opacity.

Proof gate:

- Third-person review confirms coating is dark gray/metallic and visible for full duration.
- Logs show effect apply/remove.

### 6. Alloy Enhancement

What it must do:

- Applies only to physical melee weapons/tools.
- Does not apply to ranged or magic weapons.
- The next eligible item action binds the buff to that item.
- The next 3 uses are boosted.
- Swapping away after binding ends the remaining buff.
- Weapon/tool should show the same coating family as Metal Coat.
- Swing/impact frames should also read gray/metallic.

How to achieve it:

- Track an `ActiveWeaponFollowUp` with source ability, first eligible item id, remaining uses, expiry.
- On hit/tool use, verify active item matches bound id.
- Apply damage bonus/durability protection for exactly 3 uses.
- Clear on slot/item switch.
- Use item/body coating plus short impact-frame VFX.

Ideal route:

- Runtime state owns the gameplay; visual effect is just feedback.
- Add logs for bind, charge use, rejected item, swap cleanup, duration cleanup.

Proof gate:

- Damage numbers show 3 boosted uses.
- Fourth use is normal.
- Swap after binding clears buff.
- User sees item coating and gray impact frames.

## Style: Magma

### 7. Lava Pool

What it must do:

- Spawn from the caster's body, not cursor position.
- Create a visible lava pool/ring on the ground.
- Enemies are affected.
- Caster, allies, allied summons are not slowed, damaged, burned, or visually cluttered by fire.
- Cleanup must be reliable.

How to achieve it:

- Use caster-centered field runtime.
- Prefer actual temporary lava/fluid blocks only if a safety proof passes.
- If real lava always causes hostile native behavior, use lava-like temporary blocks/visuals plus server-side field damage instead.
- Add caster speed/damage immunity compensation only if logs prove it covers all native lava effects.

Ideal route:

- Temporary lava-looking field owned by player + server-side AoE logic; caster/allies ignored.

Proof gate:

- User walks through own pool with normal movement and no damage/fire.
- Enemy in field takes effect.
- Logs show all temporary terrain restored.

### 8. Obsidian Skin

What it must do:

- During first phase, player is immobile/rooted inside a lava block shell/box.
- Lava shell should be visually around player, but camera-safe.
- After lava shell disappears, player receives a dark purple-black obsidian coating.
- No client crash.

How to achieve it:

- Replace unsafe full camera-overlapping shell geometry with a safer 3x3x4-ish shell or visual arrangement that avoids client index errors.
- Use explicit root/immobility state for lava phase.
- Queue the obsidian coating to apply after shell phase.
- Use near-black midnight purple top/bottom tint over stoneskin-style VFX.

Ideal route:

- Short temporary shell + player anchor/root + delayed `MOTM_Proof_Coating_Obsidian`.

Proof gate:

- 5 repeated casts from cold launch do not crash.
- User sees lava phase and distinct purple-black coating phase.
- Logs show root start/end, shell cleanup, coating apply/remove.

### 9. Magma Sling

What it must do:

- Fire an aimed lava blob projectile.
- Projectile visibly travels, hits enemies/surfaces, and despawns.
- It must not be a living mob or show a health bar/nameplate.

How to achieve it:

- Use `ProjectileModule.spawnProjectile` and existing projectile config candidates first.
- Candidate visuals from local assets: fireball, flame projectile, lava/fire textures, small projectile model.
- If projectile config cannot be recolored enough, use first-class projectile for physics and attach a non-living visual/trail if possible.
- Server logs should own hit detection if native projectile interactions are hard to hook.

Ideal route:

- First-class fire/lava projectile config, direction from player aim, impact callback or server-side collision sweep.

Proof gate:

- User sees projectile path aligned with aim.
- Target hit logs match visual impact.
- No mob health bar/nameplate appears.

## Style: Stone

### 10. Rubble Rouser

What it must do:

- Buff melee/unarmed stone strikes.
- Visual should imply stone-coated arms, not just hands.
- If arm-only coating is not exposed, use strong full-body/held-item stone coating as fallback.

How to achieve it:

- Track active melee buff with duration/charges.
- Hook melee hit events for damage/knockback/status.
- Apply stone coating effect and short impact dust on hit.
- Research/try attachment-specific VFX only if local API exposes it; do not block on perfect arm isolation.

Ideal route:

- Gameplay buff + full-body/held-item stone coating + stone impact frames.

Proof gate:

- Melee hit during buff has increased/changed effect.
- Visual is clearly stone, not metal/obsidian.
- Buff cleans after duration/uses.

### 11. Pillar Strike

What it must do:

- Spawn a 1x1x4 stone pillar at the target location and remove it 0.6s after it reaches full height.
- Pillar appears rapidly staged upward.
- Target is launched/stunned/damaged as if pillar erupts beneath them.

How to achieve it:

- Use staged temporary block column with stage logs.
- Anchor under target or selected ground point.
- Apply launch/stun pulse when column completes or as it starts.

Ideal route:

- `placeStackingColumnSelection` using stone blocks + target combat pulse.

Proof gate:

- User sees pillar stack to 3 high.
- Target is launched/stunned.
- Column restores.

### 12. Rockslide

What it must do:

- Forward stone/dust control attack.
- Should read like rocks/debris moving forward, not invisible damage.

How to achieve it:

- Use a line sweep with stone/rubble visual nodes.
- Add stone dust/block-break particles and maybe small rock proxy/projectile if stable.
- Apply damage/knockback/debuff along path.

Ideal route:

- Server-side line hit sweep + temporary rubble/stone trail + particles.

Proof gate:

- Targets in path are hit; targets outside are not.
- User sees forward-moving rocky wave.

## Style: Arbor

### 13. Rooted

What it must do:

- Root/heal/defensive state as designed.
- Visual roots/vines at lower body connected to ground.
- No floor/support block replacement.

How to achieve it:

- Use surface decoration anchor for roots/vines above support block.
- Apply self status separately.
- Restore root markers on expiry.

Ideal route:

- `surfaceDecorationAnchor` fixed/centralized + root EntityEffect if useful.

Proof gate:

- User sees roots on top of ground.
- Blocks beneath player remain unchanged.
- Cleanup logs fire.

### 14. Vines

What it must do:

- No cooldown.
- Only one target affected at a time.
- Moving Vines to a new target clears old target.
- Target death clears state.
- Visual remains on current target.

How to achieve it:

- Maintain `activeVinesByPlayer`.
- On cast, release prior target state/effect.
- Apply root/slow/control and vine visual to new target.
- Listen for entity removal/death where possible.

Ideal route:

- Single-target state table + target status/effect + cleanup on recast/death/expiry.

Proof gate:

- Cast on target A, then B: A releases, B roots.
- Kill B: vines clear.

### 15. Sapling

What it must do:

- Projectile marks a ground point.
- It should not directly hurt enemies.
- On ground impact, spawn an emerald temple statue marker/taunt object with pink glow.

How to achieve it:

- Use projectile or line trace to determine ground impact.
- Surface-place `Furniture_Temple_Emerald_Statue` at impact point.
- Start taunt/lure field from the statue marker.
- Ignore enemy collision or pass through enemies.

Ideal route:

- First-class projectile with ground impact behavior if possible; otherwise server-simulated ground ray.

Proof gate:

- Aiming through an enemy still places sapling on ground.
- Emerald statue marker lures/taunts nearby enemies.
- Cleanup restores marker.

## Style: Bloom

### 16. Nightshade

What it must do:

- Projectile passes through enemies and lands on surface/object.
- Creates flower at landing point.
- Flower lures enemies within 5 blocks.
- Poison explosion damages/poisons after lure.
- Poison visual is light purple smoke/effect hugging target body.

How to achieve it:

- Use ground/surface resolving projectile behavior.
- Surface-place purple flower marker.
- Create short lure field.
- Apply poison DoT/status and body-hugging poison effect at explosion.

Ideal route:

- Server-simulated pass-through projectile until surface hit + flower field.

Proof gate:

- Enemy between caster and wall does not stop projectile.
- Flower appears on surface and lure/explosion logs fire.

### 17. Frolick

What it must do:

- While active and moving, player leaves flower trail behind them.
- Flowers are placed on top of ground behind player.
- It must not replace floor blocks or push player underground.

How to achieve it:

- Sample player position while active.
- Compute behind-player positions.
- Use surface decoration placement above support block.
- Add spacing so it does not spam blocks every tick.

Ideal route:

- Moving terrain trail with corrected anchor and cleanup owner state.

Proof gate:

- Movement test shows flowers behind player.
- No support/floor replacement.
- Cleanup restores all flowers.

### 18. Cacti Cluster

What it must do:

- Slow, large cactus-like projectile.
- Sticks to first enemy or first surface.
- If attached to enemy: that enemy receives 4s DoT worth 5% caster max HP and 20% slow.
- After 4s, cactus explodes visually.
- Explosion does not add extra damage to attached target.
- Explosion applies same DoT/slow to enemies within 4 blocks without attaching more cacti.

How to achieve it:

- Use projectile path/collision with cactus visual.
- Track `AttachedCactus` state with target/surface anchor.
- Schedule delayed explosion.
- Apply secondary DoT/slow through AoE with attached target excluded from extra burst.

Ideal route:

- First-class or server-simulated projectile + cactus surface/target marker + delayed AoE.

Proof gate:

- Direct hit attaches and ticks.
- Nearby secondary mobs get DoT/slow after explosion.
- Attached target does not get double-burst damage.

## Style: Self Petrification

### 19. Gargoyle

What it must do:

- Player enters stone-coated form.
- Ability can end naturally or be manually canceled.
- Cooldown starts after end/cancel.
- Cooldown is 6 seconds.

How to achieve it:

- Toggle/status effect with finite duration.
- Strong stone coating effect.
- On toggle off/expiry, clear effect and start cooldown.

Ideal route:

- `StyleManager` toggle + `deactivateAbilityRuntime` cleanup + stone VFX.

Proof gate:

- User toggles/cancels; cooldown starts after end.
- Coating is visually distinct and removed.

### 20. Glare

What it must do:

- Petrifies target with same stone coating as Gargoyle.
- On release, stone coating disappears.
- Target remains slowed for 2 seconds after release.

How to achieve it:

- Apply target root/stun/petrify status and stone coating.
- Schedule release cleanup and delayed slow tail.
- Log target id through all phases.

Ideal route:

- Target effect state table with `petrifyUntil` and `slowUntil`.

Proof gate:

- User sees target coated, then coating gone while slow remains briefly.
- Logs show petrify/release/slow tail.

### 21. Tunnel

What it must do:

- No resources.
- Player becomes/reads as a singular stone block/form.
- Player can traverse through/under ground while active.
- Can be used with Gargoyle.
- When duration ends, player is pulled up to safe surface or valid cave air pocket.
- Player must never be stuck in terrain.

How to achieve it:

- Duration-based movement state.
- Apply stone block form cue/coating.
- Movement samples check current/next position.
- Safe exit resolver searches nearby valid air/surface.
- If no cave/air-pocket proof, always surface first for safety.

Ideal route:

- Controlled movement state + safe-exit resolver + stone visual proxy/coating.

Proof gate:

- 10 casts in safe test lane; no stuck state.
- Gargoyle + Tunnel combo works.
- Logs show exit reason: surface or valid air pocket.

## Style: Soil

### 22. Burrow

What it must do:

- Player visually drops down, dashes forward 4 blocks, re-emerges.
- Exit moment damages/knocks nearby enemies.
- It feels like evasion/mobility, distinct from Tunnel.

How to achieve it:

- Movement dash with short hidden/dipped visual state.
- Entry and exit ground dust/blocks.
- Damage/knockback pulse at exit position.
- Safe lane/edge detection prevents platform fall.

Ideal route:

- Server movement instruction or teleport/dash + entry/exit terrain cues.

Proof gate:

- User moves exactly forward-ish 4 blocks and reappears.
- Exit AoE hits nearby targets.
- No void/off-platform movement.

### 23. Mudpit

What it must do:

- Expanding brown muddy water/pool field.
- Enemy debuff field.
- Counts as water for future Hydro interaction where relevant.
- Caster/allies move normally and are not slowed/damaged/debuffed.

How to achieve it:

- Use water/fluid only if safe-field proof passes.
- Add brown overlay through particles/blocks if fluid tint cannot be changed directly.
- Server-side field applies debuffs to enemies only.
- Friendly/caster skip is mandatory.

Ideal route:

- Safe water/fluid field + brown debris/dust overlay + enemy-only field ticks.

Proof gate:

- Caster walks normally through field.
- Enemy is slowed/debuffed.
- Brown visual reads as mud.

### 24. Debris

What it must do:

- Forward traveling brown dust/debris wave.
- Applies blind/vulnerability or intended debuffs.
- Not a thrown dirt block.

How to achieve it:

- Use line sweep for mechanics.
- Use brown smoke/dust particles, dirt/stone block break particles, and possibly trail assets.
- Visual nodes travel forward with timing.

Ideal route:

- Server line control + dust/debris trail using particles/trail textures.

Proof gate:

- User sees forward wave.
- Enemies in path receive debuffs; outside path skipped.

## Style: Sand

### 25. Sandstorm

What it must do:

- No resources.
- Toggleable 10 second active duration.
- Manual deactivate allowed.
- 2 second cooldown after deactivation or expiry.
- Cloud follows player.
- Enemy-only damage/debuffs.
- Visual should look beige-yellow sand, not plain white smoke.

How to achieve it:

- Use `StyleManager` toggle state.
- Follow-owner field runtime.
- Use sand-colored particles/trails/block ring; test trails because smoke tint may be weak.
- Apply enemy-only field ticks.

Ideal route:

- Toggle + follow field + sand/debris trail/particles + structured field logs.

Proof gate:

- Activate, wait 10s: auto ends, 2s cooldown.
- Activate, manually deactivate: 2s cooldown.
- Enemy affected, caster/allies skipped.
- User sees sand-colored radius.

### 26. Dust Devil

What it must do:

- Can only be used while Sandstorm is active.
- If Sandstorm inactive, cast fails clearly.
- During use, player dashes forward with Sandstorm/tornado visual.
- Caught enemies are dragged during dash.
- At dash end, enemies are expelled/knocked away.
- Dust Devil ends/consumes Sandstorm.

How to achieve it:

- Use active-toggle precondition already added in `StyleManager`.
- Use movement dash runtime.
- Field/line control pulls during dash and knockbacks at end.
- Call Sandstorm runtime cleanup when Dust Devil fires.

Ideal route:

- Active Sandstorm state + dash movement + temporary pull field + end burst.

Proof gate:

- Inactive cast fails with message.
- Active cast moves player, affects enemies, and Sandstorm ends.
- Logs show consume/end and target expel.

### 27. Vitrification

What it must do:

- Works alongside Sandstorm and Dust Devil.
- Applies burn/glass/superheated-sand effect.
- Visual layers without hiding sand cloud.

How to achieve it:

- Keep as projectile/status or short targeted effect.
- Use glass/spark/sand particles that are visually thinner than Sandstorm.
- Do not clear Sandstorm toggle/field.

Ideal route:

- Projectile or targeted status effect with compatible overlay.

Proof gate:

- Cast while Sandstorm active; both remain visible/functioning.
- Cast during/after Dust Devil does not break combo state.

## Style: Gem

### 28. Lapidary

What it must do:

- Persistent controllable/recallable gem object.
- One active gem per player.
- Has HP tracking/bar/readable nameplate.
- Visual is floating 2x2x2 green gem/crystal cube one block above ground, unless a better green gem asset is proven.

How to achieve it:

- Temporary floating block cluster for the cube.
- Proxy/nameplate for HP if block cluster alone cannot show HP.
- Owner state stores HP, location, expiry/recall.
- Recast recalls/replaces old gem.

Ideal route:

- Temporary green crystal block cluster + proxy HP marker + state table.

Proof gate:

- User sees cube and HP/readout.
- Logs show spawn, HP update, recall/despawn.

### 29. Fracture

What it must do:

- Explosion originates from Lapidary gem, not player.
- Expanding green sphere/circle visual, not instant full-radius flash.
- Does not affect caster/allies/allied summons.

How to achieve it:

- Require active Lapidary.
- Use staged expanding AoE ticks from gem center.
- Use green pulse particles/proxy ring/sphere visual.
- Apply damage only when expanding radius reaches targets.

Ideal route:

- Gem-centered expanding pulse runtime.

Proof gate:

- No gem means clear failure or alternate behavior if approved.
- With gem, expansion visibly starts at gem and hits enemies by distance.

### 30. Refraction

What it must do:

- Aura/shield/radius effect originates from Lapidary gem.
- Bright green sphere/aura shows active radius.
- Works with Lapidary state and cleans when gem disappears.

How to achieve it:

- Require or prefer active Lapidary.
- Spawn field/aura visual anchored to gem proxy/location.
- Apply shield/reflect/refraction mechanics according to final data.
- Cleanup if gem despawns/recalls.

Ideal route:

- Gem-anchored field visual and server-side effect state.

Proof gate:

- User sees aura around gem, not player.
- Logs show aura attach, refresh, remove with gem.

## Cross-Ability Dependencies

```
╔═════════════════════╦════════════════════════════════════════════════╗
║ Dependency          ║ Abilities Depending On It                      ║
╠═════════════════════╬════════════════════════════════════════════════╣
║ Structured logs     ║ all 30                                         ║
║ Friendly filtering  ║ all negative AoEs/fields/projectiles           ║
║ Surface placement   ║ Rooted, Vines, Sapling, Frolick, Nightshade    ║
║ First-class project ║ Magma Sling, Sapling, Nightshade, Cacti, Vitri ║
║ Coating palette     ║ Metal Coat, Alloy, Gargoyle, Glare, Obsidian   ║
║ Safe field/fluid    ║ Lava Pool, Obsidian shell, Mudpit              ║
║ Movement safety     ║ Stomp, Burrow, Tunnel, Dust Devil              ║
║ Persistent objects  ║ Lapidary, Fracture, Refraction, Sapling flower ║
╚═════════════════════╩════════════════════════════════════════════════╝
```

## Implementation Order I Should Use

1. Add structured Terra telemetry and cleanup counting.
2. Fix surface placement once, then reuse for Arbor/Bloom/Gem/Stone markers.
3. Prove and wire first-class projectiles.
4. Lock coating palette and runtime cleanup.
5. Fix movement safety.
6. Prove safe fields/fluids or choose visual-only fallback.
7. Finish styles in this order:
   - Metal
   - Quake
   - Stone
   - Arbor
   - Bloom
   - Magma
   - Soil
   - Sand
   - Self Petrification
   - Gem

This order minimizes repeated rework because the later styles depend on the earlier primitive proofs.

## Final Terra Acceptance

Terra is complete when:

- all 30 abilities pass normal input tests, not only dev commands;
- all resource costs remain zero;
- all negative effects skip caster/allies/allied summons;
- all temporary blocks/fluids/proxies/effects clean up;
- projectile abilities are not living mobs with health bars;
- user confirms visuals in third person;
- structured logs prove every cast, hit, skip, and cleanup.
