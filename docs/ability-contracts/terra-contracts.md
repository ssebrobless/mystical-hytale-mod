# Terra Ability Contracts

Fantasy: earth, stone, metal, sand, growth; heavyweight defense and terrain control (Terra canon Sec. 2).
Palette: primary `#8B4513`, secondary `#228B22`, accent `#CD853F` (class JSON).
Passive **Immovable** (quoted): "Reduce knockback taken by 20% without increasing knockback dealt. Regenerate 1% max health per second while below 30% health, mine 50% faster with pickaxes, and gain cave vision underground."

## Applicable universal visual grammar
- Rule 1: **Physical object first, particles second.** Lava ring = actual lava blocks; pillars = stone blocks; sapling = tree model. Particles accent, they don’t substitute.
- Rule 2: **Visible travel before impact.** Projectiles/waves must be watchable and dodgeable — never instant-hit with an impact flash.
- Rule 3: **Burst, not teleport.** Dash-family movement shows start burst + trail + end cue over time. `dust_devil` was explicitly rejected as too teleport-like; `burrow` is the one allowed disappearance exception.
- Rule 4: **Summons visibly fight.** Spawning is not the fantasy; owner-attributed combat is.
- Rule 5: **Tethers are visible links.** Thin particle line caster<->target, themed (water stream / vine / chain / void pull / wind funnel), synced with movement. `rip_current`’s water-fluid trace is an accepted exception.
- Rule 6: **Coatings hug the body.** Tight tint+model effect; no permanent class tint, no permanent trails (Aqua Barrier: one large opaque blue bubble until destroyed).
- Rule 7: **Composition stack = identity.** Silhouette first, palette second (tint pair + 2-3 same-family particle ids), motion third (cast/travel/impact/loop distinct), silence fourth (strip nameplate/collision on proxies). Max one wrong-family asset per stack.
- Rule 8: **Cleanup is part of the visual.** No drops, no stuck visuals, no lingering tints.

## Reference example — dust_devil
> CONFIRMED GAP -> Phase 2.4 (dash family), full authored contract restated so the sweep cannot under-build it: dust_devil = caster dashes 5 blocks AS a rolling sand tornado over 2s (travel_type rolling_tornado), knocking back (force 4) everything in the swept radius-5 path; canon rule 3 (burst + trail + end cue over time, grill rejected teleport-read), sand canon (no white smoke). Current build: no client displacement, instant snap-knockback, foot dust - misses the identity, not just the movement. Displacement mechanism is PROVEN territory: GrapplingHook (catalog mod-patterns.md) moves live players via velocity impulses - use that, not position writes. Acceptance: player position delta in evidence + author confirms the tornado read.

Terra build references: proposals T1–T12 in `docs/improvement-proposals-2026-07-16.md`; high-fidelity ceilings are quake, gem, sandstorm, and sinkhole.

## Style `quake` — Quake
Theme: "Knockback, AoE, ground control"

### stomp — Stomp (quake)
- **author says:** "Arm your next jump so the landing releases a 3-block shockwave that damages and knocks enemies away from the impact point" — style theme: "Knockback, AoE, ground control"
- **data:** `{"id":"stomp","name":"Stomp","description":"Arm your next jump so the landing releases a 3-block shockwave that damages and knocks enemies away from the impact point","damage_percent":6,"cooldown_seconds":2,"cast_time_seconds":0.25,"recovery_seconds":0.18,"effect":"knockback","categories":["damage","crowd_control"],"charges":4,"charge_recharge_seconds":2.0,"resource_cost":0,"cast_type":"ground_burst","target_type":"self_centered","radius":3,"knockback_force":4.5,"terrain_effect":"seismic_shockwave","ground_targets_only":true,"visual_overlay":"ground_cracks","trigger":"jump_land"}`
- **behavior over time:** t0: arm next jump during 0.25s cast -> on jump landing the 3-block shockwave activates -> impact resolves damage/knockback then the 0.18s recovery ends. The caster jump/landing and the shockwave are visible; enemies are pushed from the impact point. [INFERRED]
- **targeting:** self-centered ground burst; no aim target; trigger is jump_land.
- **visuals:** Physical 3-block seismic shockwave with ground-crack cast/impact; particles accent an actual ground event (rules 1,7), T6. CURRENT STATE manifest row: `{"cast":"MOTM_Terra_Quake_Cast","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"MOTM_Terra_Quake_Impact","loop":null,"model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** SFX_Golem_Earth_Stomp + SFX_Golem_Earth_Stomp_Impact (P5).
- **cleanup:** Remove shockwave, cracks, and any temporary seismic marker after resolution; restore terrain if the seismic_shockwave implementation changed blocks.
- **locks:** rules 1,2,7,8; T6; quake high-fidelity reference.
- **current gap:** Manifest legacy=true and already maps the MOTM Quake cast/impact/travel stack, but has no model/role/projectileConfig; physical shockwave/terrain ownership remains to prove. [INFERRED]

### aftershock — Aftershock (quake)
- **author says:** "Release an 8-block spherical aftershock that damages, pushes, and staggers enemies from the center" — style theme: "Knockback, AoE, ground control"
- **data:** `{"id":"aftershock","name":"Aftershock","description":"Release an 8-block spherical aftershock that damages, pushes, and staggers enemies from the center","damage_percent":5,"cooldown_seconds":5,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"slow+knockback","categories":["debuff"],"resource_cost":0,"cast_type":"ground_zone","target_type":"self_centered","radius":8,"duration_seconds":2,"terrain_effect":"lingering_tremor","ground_targets_only":false,"visual_overlay":"ground_cracks"}`
- **behavior over time:** t0: charge during 0.55s cast -> an 8-block spherical aftershock expands from the caster and persists for 2s as lingering tremor -> damage/push/stagger/slow resolve through the active window, then 0.32s recovery and cleanup. [INFERRED]
- **targeting:** self-centered spherical ground zone; no crosshair aim.
- **visuals:** MOTM_Terra_Quake_Cast/Impact/Loop composition: radial stone/earth silhouette and ground cracks, with the spherical field visibly expanding before its loop (rules 1,2,7), T6/T12. CURRENT STATE manifest row: `{"cast":"MOTM_Terra_Quake_Cast","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"MOTM_Terra_Quake_Impact","loop":"MOTM_Terra_Quake_Loop","model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** SFX_Golem_Earth_Stomp_Impact (P5) [INFERRED].
- **cleanup:** Remove spherical tremor loop, cracks, and owned field markers at 2s; restore any altered terrain.
- **locks:** rules 1,2,7,8; T6,T12; quake high-fidelity reference.
- **current gap:** Manifest legacy=true maps the MOTM Quake cast/impact/loop and stone dust, but the row has no physical-field model or role; owned field timing/cleanup is not represented. [INFERRED]

### sinkhole — Sinkhole (quake)
- **author says:** "Bury enemy, dealing suffocation damage" — style theme: "Knockback, AoE, ground control"
- **data:** `{"id":"sinkhole","name":"Sinkhole","description":"Bury enemy, dealing suffocation damage","damage_percent":15,"cooldown_seconds":6,"cast_time_seconds":0.6,"recovery_seconds":0.28,"effect":"root+dot","categories":["damage","crowd_control"],"resource_cost":0,"cast_type":"ground_target","target_type":"enemy","range":10,"radius":3,"delay_seconds":0.6,"duration_seconds":3,"terrain_effect":"sinkhole","ground_targets_only":true,"visual_overlay":"ground_cracks","vertical_displace_blocks":2.5,"dot_percent_per_second":2}`
- **behavior over time:** t0: charge at 0.6s -> after the authored 0.6s delay a ground sinkhole opens under the enemy -> enemy is vertically displaced 2.5 blocks, rooted and suffocating for 3s while the MOTM crack loop reads the hole -> recovery 0.28s and the hole/visuals close. [INFERRED]
- **targeting:** enemy ground-target at range 10; crosshair selects the enemy/ground point and the effect is ground-targeted, not a projectile.
- **visuals:** Physical sinkhole opening and cracks, with the high-fidelity MOTM_Terra_Sinkhole_Cracks loop; actual terrain/marker first, particles second (rules 1,2,7), T12. CURRENT STATE manifest row: `{"cast":"MOTM_Terra_Quake_Cast","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"MOTM_Terra_Quake_Impact","loop":"MOTM_Terra_Sinkhole_Cracks","model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** SFX_Stone_Break (P5) [INFERRED].
- **cleanup:** Remove sinkhole, cracks, root marker, and DOT carrier after 3s; restore terrain exactly if blocks were displaced.
- **locks:** rules 1,2,7,8; T2,T12; sinkhole high-fidelity reference.
- **current gap:** Manifest legacy=true maps Quake cast/impact plus MOTM_Terra_Sinkhole_Cracks loop, but no physical sinkhole model/role; current implementation must prove 2.5-block displacement and terrain restore. [INFERRED]

## Style `metal` — Metal
Theme: "Defense, walls, self-healing"

### iron_wall — Iron Wall (metal)
- **author says:** "Create barrier, heal 10% HP" — style theme: "Defense, walls, self-healing"
- **data:** `{"id":"iron_wall","name":"Iron Wall","description":"Create barrier, heal 10% HP","damage_percent":0,"cooldown_seconds":4,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"heal","categories":["healing"],"heal_percent":10,"resource_cost":0,"cast_type":"barrier","target_type":"line","range":7,"width":3,"height":4,"duration_seconds":4,"toggleable":true,"toggle_cooldown_seconds":4,"terrain_effect":"iron_wall"}`
- **behavior over time:** t0: create the line barrier and heal the caster over 0.55s -> an actual 3-wide by 4-high iron wall persists for 4s, with toggleable early removal and protection during its life -> 0.32s recovery and cleanup. [INFERRED]
- **targeting:** line-target at range 7, oriented to the caster-facing/crosshair line; [INFERRED] line placement uses the aimed ground plane.
- **visuals:** An actual iron wall is the silhouette; earth/metal block faces and restrained sparks accent it. T2 physical-object-first; particles never replace the barrier (rule 1). CURRENT STATE manifest row: `{"cast":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner","loop":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** SFX_Stone_Break (P5) [INFERRED].
- **cleanup:** Despawn owned iron blocks/marker on expiry or toggle-off and restore every original block; remove heal/field visuals.
- **locks:** rules 1,7,8; T2; metal wall geometry remains under-authored.
- **current gap:** Manifest legacy=true uses generic brazier/stone dust/mace shockwave and no model/role; it does not communicate an iron wall or its 3x4x4s footprint. [INFERRED]

### metal_coat — Metal Coat (metal)
- **author says:** "Coat yourself in dark shiny metal, reducing all incoming damage by 50%" — style theme: "Defense, walls, self-healing"
- **data:** `{"id":"metal_coat","name":"Metal Coat","description":"Coat yourself in dark shiny metal, reducing all incoming damage by 50%","damage_percent":0,"cooldown_seconds":8,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"defense_buff","categories":["buff"],"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":8,"terrain_effect":"metal_plating"}`
- **behavior over time:** t0: apply the dark shiny metal coating during 0.28s -> all incoming damage is reduced by 50% for 8s -> coating ends, then 0.2s recovery. [INFERRED]
- **targeting:** self-targeted body coating; no aim.
- **visuals:** Tight dark shiny metal tint/model effect hugging the player, with no permanent class tint or trail (rule 6); MOTM_Proof_Coating_Metal is the current proof direction. CURRENT STATE manifest row: `{"cast":"MOTM_Proof_Coating_Metal","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"Server/Particles/Block/Metal/Spawners/Block_Break_Metal_Sparks.particlespawner","loop":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Remove the metal tint/model coating and defense status exactly at 8s; no residual trail or particles.
- **locks:** rules 6,7,8; T4 pattern (coating).
- **current gap:** Manifest legacy=true has the metal coating cast/impact but generic stone-dust travel and lantern loop; no model/role is declared, so body-hugging cleanup is not guaranteed. [INFERRED]

### alloy_enhancement — Alloy Enhancement (metal)
- **author says:** "Prime the next physical melee weapon or tool for three no-durability uses with 30% bonus effect" — style theme: "Defense, walls, self-healing"
- **data:** `{"id":"alloy_enhancement","name":"Alloy Enhancement","description":"Prime the next physical melee weapon or tool for three no-durability uses with 30% bonus effect","damage_percent":0,"cooldown_seconds":0,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"damage_buff","categories":["buff"],"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":8}`
- **behavior over time:** t0: prime the next physical melee weapon or tool during 0.28s -> the next three physical uses gain 30% bonus effect without durability loss -> after the third use (or 8s duration) the prime is consumed and 0.2s recovery ends. [INFERRED]
- **targeting:** self-targeted weapon/tool stance; no aim.
- **visuals:** Metallic prime flash and a tight weapon/tool highlight communicate the three-use state; do not leave a permanent class tint or trail (rule 6). T5/T6 data-driven buff semantics. CURRENT STATE manifest row: `{"cast":"MOTM_Proof_Alloy_Enhancement","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"MOTM_Proof_Alloy_Impact","loop":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Remove prime flash/highlight and buff state on third use or 8s expiry; no lingering coating.
- **locks:** rules 6,7,8; T6; metal-style visual details are an open author question.
- **current gap:** Manifest legacy=true maps MOTM proof cast/impact but generic stone-dust travel and lantern loop; no model/role/projectileConfig records the three-use weapon state. [INFERRED]

## Style `magma` — Magma
Theme: "Lava, area denial, burn damage"

### lava_pool — Lava Pool (magma)
- **author says:** "Create a caster-centered lava pool that burns enemies while protecting the caster and allies" — style theme: "Lava, area denial, burn damage"
- **data:** `{"id":"lava_pool","name":"Lava Pool","description":"Create a caster-centered lava pool that burns enemies while protecting the caster and allies","damage_percent":2,"cooldown_seconds":4,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"burn","categories":["dot","aoe"],"resource_cost":0,"cast_type":"ground_zone","target_type":"self_centered","range":12,"radius":5,"duration_seconds":6,"terrain_effect":"lava_pool"}`
- **behavior over time:** t0: build a caster-centered 5-radius lava zone over 0.55s -> lava blocks and bubbles persist 6s, burning enemies while protecting caster/allies -> 0.32s recovery and complete field removal. [INFERRED]
- **targeting:** self-centered ground zone, radius 5 (range 12 is authored data); no aim target.
- **visuals:** Actual lava blocks plus Block_Lava_Bubbles, with cooled volcanic edges and fire accents; the plan explicitly rejects the Golem_Firesteel plan model (misfit, T10). CURRENT STATE manifest row: `{"cast":"Server/Particles/Combat/Fire_Stick/Spawners/Fire_Charge1_Fire.particlespawner","travel":"Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Smoke.particlespawner","impact":"Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Fire.particlespawner","loop":"MOTM_Proof_Lava_Pool_Field","model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Remove all owned lava blocks, bubbles, burn/protection field, and restore originals at 6s (terrain_effect=lava_pool).
- **locks:** rules 1,7,8; T2,T10; Terra lava_pool misfit row.
- **current gap:** Manifest legacy=true has fire cast/smoke travel/fire impact and MOTM lava loop, but the current plan model is the Golem_Firesteel misfit; it lacks physical lava-block identity. [INFERRED]

### obsidian_skin — Obsidian Skin (magma)
- **author says:** "Root yourself in a short lava tower, then gain a dark obsidian coating with shield and damage reduction" — style theme: "Lava, area denial, burn damage"
- **data:** `{"id":"obsidian_skin","name":"Obsidian Skin","description":"Root yourself in a short lava tower, then gain a dark obsidian coating with shield and damage reduction","damage_percent":0,"cooldown_seconds":10,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"shield+damage_reduction","categories":["shielding"],"shield_percent":20,"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":6,"terrain_effect":"obsidian_plates"}`
- **behavior over time:** t0: root the player in a short lava tower during 0.28s -> apply shield and damage-reduction obsidian coating for 6s -> tower/coating vanish and 0.2s recovery ends. [INFERRED]
- **targeting:** self-targeted body transformation; no aim.
- **visuals:** Short physical lava tower at cast, then a dark obsidian plate/tint coating hugging the body; T10 says obsidian is a dark tint coat, while rule 6 forbids permanent trails. CURRENT STATE manifest row: `{"cast":"MOTM_Proof_Coating_Obsidian","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"MOTM_Proof_Coating_Obsidian","loop":"MOTM_Proof_Coating_Obsidian","model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Remove tower, obsidian plates/tint, shield visuals, and terrain_effect marker at 6s; restore any tower footprint.
- **locks:** rules 1,6,7,8; T10.
- **current gap:** Manifest legacy=true maps MOTM proof obsidian coat for cast/impact/loop but generic stone-dust travel and no model/role; tower/root and exact cleanup are not represented. [INFERRED]

### magma_sling — Magma Sling (magma)
- **author says:** "Fire a small molten projectile that burns the direct target and splashes nearby enemies" — style theme: "Lava, area denial, burn damage"
- **data:** `{"id":"magma_sling","name":"Magma Sling","description":"Fire a small molten projectile that burns the direct target and splashes nearby enemies","damage_percent":8,"cooldown_seconds":3,"cast_time_seconds":0.35,"recovery_seconds":0.24,"effect":"burn+slow","categories":["dot","debuff"],"resource_cost":0,"cast_type":"projectile","target_type":"enemy","range":18,"max_range":18,"projectile_speed":16,"travel_type":"arcing_shot"}`
- **behavior over time:** t0: charge 0.35s -> a small molten projectile travels as an arcing shot at speed 16 up to 18 blocks -> direct impact burns and nearby enemies receive splash slow -> 0.24s recovery. [INFERRED]
- **targeting:** crosshair-aimed, dodgeable arcing projectile to an enemy within 18; visible travel is required by rule 2 and live aim finding.
- **visuals:** Physical molten projectile first, arcing through a hot orange/red trail, then splash of lava/fire at impact; T1 earth-family recipe is not applicable, so use the magma-specific projectile row and no Spark_Living proxy. CURRENT STATE manifest row: `{"cast":"Server/Particles/Combat/Fire_Stick/Spawners/Fire_Charge1_Fire.particlespawner","travel":"MOTM_Proof_Magma_Sling_Travel","impact":"Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Fire.particlespawner","loop":"Server/Particles/Combat/Fire_Stick/Fire_Trap/Fire_AoE_Grow.particlesystem","model":"Common/Items/Projectiles/Fireball.blockymodel","role":"Spark_Living","projectileConfig":"Projectile_Config_MOTM_Magma_Sling_Visual","legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Remove projectile/travel/impact particles and splash carrier after impact; no persistent terrain_effect is authored.
- **locks:** rules 1,2,7,8; T10; U2 projectile engine; no white-smoke substitution.
- **current gap:** Manifest legacy=true includes Fireball.blockymodel with role Spark_Living and a MOTM magma travel row; this is explicitly a proxy debt and must become a physical molten projectile. [INFERRED]

## Style `stone` — Stone
Theme: "Heavy strikes, knockback, crowd control"

### rubble_rouser — Rubble Rouser (stone)
- **author says:** "Coat your whole body in stone and empower the next bare-fist hit with a rubble burst" — style theme: "Heavy strikes, knockback, crowd control"
- **data:** `{"id":"rubble_rouser","name":"Rubble Rouser","description":"Coat your whole body in stone and empower the next bare-fist hit with a rubble burst","damage_percent":10,"cooldown_seconds":9,"cast_time_seconds":0.35,"recovery_seconds":0.24,"effect":"knockback","categories":["damage","crowd_control"],"resource_cost":0,"cast_type":"self_buff","target_type":"self","range":16,"max_range":16,"projectile_speed":20,"knockback_force":3.5,"duration_seconds":8,"travel_type":"rubble_followup"}`
- **behavior over time:** t0: coat the body and arm the next bare-fist hit during 0.35s -> buff persists 8s until the empowered melee hit creates a rubble burst, damage and knockback -> 0.24s recovery. [INFERRED]
- **targeting:** self-targeted armed melee follow-up; no aim at cast; the eventual hit uses the normal melee contact.
- **visuals:** Stone body coat hugging the player, followed by a physical rubble burst at the fist impact; T1 uses Rubble_Default/Block_Break_Stone, while rule 6 keeps coating tight. CURRENT STATE manifest row: `{"cast":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner","loop":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","model":"Server/Models/Projectiles/Items/Rubble/Rubble_Stone.json","role":"Spark_Living","projectileConfig":null,"legacy":true}`.
- **sound:** SFX_Stone_Break (P5).
- **cleanup:** Remove stone coat/armed state and any rubble particles immediately after hit or at 8s; no trail.
- **locks:** rules 1,2,6,7,8; T1,T6; stone high-fidelity ceiling.
- **current gap:** Manifest legacy=true has Rubble_Stone model but role Spark_Living, generic travel/impact, and no projectileConfig; it partially reads rubble but misses body coat and hit-timed burst. [INFERRED]

### pillar_strike — Pillar Strike (stone)
- **author says:** "Rapidly stack a 1x1x4 stone pillar at an enemy or ground point, launching and stunning enemies" — style theme: "Heavy strikes, knockback, crowd control"
- **data:** `{"id":"pillar_strike","name":"Pillar Strike","description":"Rapidly stack a 1x1x4 stone pillar at an enemy or ground point, launching and stunning enemies","damage_percent":8,"cooldown_seconds":8,"cast_time_seconds":0.7,"recovery_seconds":0.28,"effect":"stun","categories":["damage","crowd_control"],"resource_cost":0,"cast_type":"ground_strike","target_type":"ground_target","range":14,"radius":2.5,"height":4,"delay_seconds":0.7,"launch_height":2,"knockup":true,"terrain_effect":"stone_pillar"}`
- **behavior over time:** t0: target and begin 0.7s rapid stacking -> serialize a 1x1x4 stone pillar, launch/stun enemies on completion -> 0.28s recovery and remove temporary ownership when its contract ends. [INFERRED]
- **targeting:** ground-target at range 14; crosshair selects an enemy or ground point, with pillar centered on that point.
- **visuals:** Physical Rock_Stone_Brick_Pillar_Base and _Middle segments rise over 0.7s, each accented by Block_Break_Stone; no generic shockwave substitute (T8, rule 1). CURRENT STATE manifest row: `{"cast":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner","loop":null,"model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** SFX_Stone_Break (P5).
- **cleanup:** Remove only owned temporary pillar segments at end and restore original blocks; remove launch/stun markers.
- **locks:** rules 1,2,7,8; T6,T8; physical-object-first.
- **current gap:** Manifest legacy=true is generic lantern/stone-dust/mace-shockwave with no model/role, so it cannot read the serialized 1x1x4 pillar. [INFERRED]

### rockslide — Rockslide (stone)
- **author says:** "Dash forward through enemies, pushing them aside while rocks, dirt, and debris kick up underfoot" — style theme: "Heavy strikes, knockback, crowd control"
- **data:** `{"id":"rockslide","name":"Rockslide","description":"Dash forward through enemies, pushing them aside while rocks, dirt, and debris kick up underfoot","damage_percent":4,"cooldown_seconds":6,"cast_time_seconds":0.25,"recovery_seconds":0.32,"effect":"knockback+grounded","categories":["damage","crowd_control"],"resource_cost":0,"cast_type":"dash","target_type":"self","range":5,"radius":2.4,"dash_distance":5,"duration_seconds":1.2,"terrain_effect":"ruptured_earth"}`
- **behavior over time:** t0: start 0.25s dash burst -> move forward 5 blocks over 1.2s through enemies, pushing them aside while rocks/dirt/debris kick up -> end cue and 0.32s recovery. [INFERRED]
- **targeting:** facing-directed self dash, range/dash_distance 5; [INFERRED] direction follows crosshair/facing, not an enemy auto-acquire.
- **visuals:** Start burst, continuous earthy trail, and end impact cue over time (rule 3), with rocks/dirt/debris physical silhouette; T6 data-driven force and no teleport read. CURRENT STATE manifest row: `{"cast":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner","loop":null,"model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** SFX_Golem_Earth_Stomp + SFX_Stone_Break (P5) [INFERRED].
- **cleanup:** Remove trail, debris, ruptured-earth marker, and force carrier after 1.2s; restore terrain_effect ruptured_earth.
- **locks:** rules 1,2,3,7,8; T6; dash family no-teleport contract.
- **current gap:** Manifest legacy=true is generic lantern/stone dust/mace shockwave with no model/role; current row does not show a dash trail or rupture footprint. [INFERRED]

## Style `arbor` — Arbor
Theme: "Nature, healing, minions"

### rooted — Rooted (arbor)
- **author says:** "Root yourself in place, heal immediately, and regenerate while wrapped in roots" — style theme: "Nature, healing, minions"
- **data:** `{"id":"rooted","name":"Rooted","description":"Root yourself in place, heal immediately, and regenerate while wrapped in roots","damage_percent":0,"cooldown_seconds":6,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"heal","categories":["healing"],"heal_percent":10,"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":5,"terrain_effect":"root_circle"}`
- **behavior over time:** t0: root the caster during 0.28s -> immediate 10% heal and regeneration persist 5s inside a visible root wrap/circle -> roots and effect end, then 0.2s recovery. [INFERRED]
- **targeting:** self-targeted stationary buff; no aim.
- **visuals:** Vine/root ring and roots visibly wrap the body and ground; physical root-circle footprint first, particles second (rules 1,6,7), T11. CURRENT STATE manifest row: `{"cast":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner","loop":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","model":"Common/NPC/Elemental/Spirit_Root/Models/Model.blockymodel","role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Remove root wrap, circle, and regeneration visuals at 5s; restore any root_circle terrain marker.
- **locks:** rules 1,6,7,8; T2,T11; Arbor per-ability model rule.
- **current gap:** Manifest legacy=true is generic lantern/stone dust/mace shockwave with blanket Spirit_Root model; it does not distinguish rooted’s root-circle healing. [INFERRED]

### vines — Vines (arbor)
- **author says:** "Entangle the enemy with thorny vines" — style theme: "Nature, healing, minions"
- **data:** `{"id":"vines","name":"Vines","description":"Entangle the enemy with thorny vines","damage_percent":1.5,"cooldown_seconds":0,"cast_time_seconds":0.4,"recovery_seconds":0.24,"effect":"root+dot","categories":["damage","crowd_control"],"resource_cost":0,"cast_type":"line_control","target_type":"enemy","range":14,"length":14,"width":2,"duration_seconds":5,"travel_type":"thorn_whip"}`
- **behavior over time:** t0: cast 0.4s -> thorny vines visibly travel along a 14-long, 2-wide line and tether the enemy for 5s, applying root and 1.5% damage -> 0.24s recovery and retract/cleanup. [INFERRED]
- **targeting:** enemy line-control at range/length 14; crosshair selects enemy and the visible line/tether must remain synced with target movement.
- **visuals:** Plant_Vine endpoint pieces plus Nature_Buff_Projectile particle chain or chain fallback, a visible vine/whip tether (rule 5, T3); never Wind_Sparks_Tail. CURRENT STATE manifest row: `{"cast":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner","loop":null,"model":"Common/NPC/Elemental/Spirit_Root/Models/Model.blockymodel","role":"Spark_Living","projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Retract/despawn vine endpoints, tether chain, root and DOT state at 5s; no stuck link or plant marker.
- **locks:** rules 1,2,5,7,8; T3/U4; Arbor vines HIGH misfit.
- **current gap:** Manifest legacy=true maps Wind/Spark_Living-style generic stone travel and Spirit_Root model; the canon explicitly says current Wind_Sparks_Tail travel should become vine/whip tether. [INFERRED]

### sapling — Sapling (arbor)
- **author says:** "Fire a seed that lands on the ground, raises an emerald temple statue with pink glow, lures enemies, and pulses healing" — style theme: "Nature, healing, minions"
- **data:** `{"id":"sapling","name":"Sapling","description":"Fire a seed that lands on the ground, raises an emerald temple statue with pink glow, lures enemies, and pulses healing","damage_percent":0,"cooldown_seconds":8,"cast_time_seconds":0.8,"recovery_seconds":0.4,"effect":"lure","categories":["crowd_control"],"resource_cost":0,"cast_type":"projectile_line","target_type":"ground_target","range":10,"duration_seconds":8,"summon_name":"","terrain_effect":"sprouting_grove"}`
- **behavior over time:** t0: fire a seed over 0.8s -> seed visibly travels in a line to a ground point within 10 -> it lands, raises an emerald temple statue with pink glow, lures enemies and pulses healing for 8s -> statue/seed field cleans up and 0.4s recovery ends. [INFERRED]
- **targeting:** crosshair-aimed, dodgeable ground-target seed line; landing point is within 10 and must be visible before impact.
- **visuals:** Seed projectile then per-ability tree/emerald temple statue with pink glow; wire orphaned MOTM_Arbor_Sapling_Pink_Glow + tree token, and summon visibly fights/acts (rules 1,2,4,7), T11. CURRENT STATE manifest row: `{"cast":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner","loop":null,"model":"Common/NPC/Elemental/Spirit_Root/Models/Model.blockymodel","role":"Spark_Living","projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Despawn seed, tree/statue, lure/healing pulse, and sprouting_grove terrain marker at 8s; restore any owned blocks.
- **locks:** rules 1,2,4,7,8; T2,T11; Arbor per-ability model rule.
- **current gap:** Manifest legacy=true uses blanket Spirit_Root model, Spark_Living role, and generic stone travel/impact; it misses seed travel and the emerald/pink sapling identity. [INFERRED]

## Style `bloom` — Bloom
Theme: "Poison, healing, nature magic"

### nightshade — Nightshade (bloom)
- **author says:** "Launch a nightshade seed that blooms into a poisonous lure" — style theme: "Poison, healing, nature magic"
- **data:** `{"id":"nightshade","name":"Nightshade","description":"Launch a nightshade seed that blooms into a poisonous lure","damage_percent":8,"cooldown_seconds":4,"cast_time_seconds":0.25,"recovery_seconds":0.18,"effect":"dot+slow","categories":["dot"],"resource_cost":0,"cast_type":"projectile_line","target_type":"ground_target","range":12,"radius":5,"projectile_speed":18,"travel_type":"nightshade_seed","duration_seconds":5,"terrain_effect":"toxic_spores"}`
- **behavior over time:** t0: launch seed during 0.25s -> visible seed travels at speed 18 to a ground point within 12 -> blooms into a radius-5 poisonous lure for 5s, dealing DOT and slow -> bloom expires and 0.18s recovery ends. [INFERRED]
- **targeting:** crosshair-aimed, dodgeable ground-target seed line at range 12; visible travel precedes bloom.
- **visuals:** Dark nightshade seed, physical bloom/lure silhouette, and toxic-spore field; T1 is not applicable, use plant/poison objects with particles as accents (rules 1,2,4,7). CURRENT STATE manifest row: `{"cast":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"MOTM_Proof_Coating_Poison","loop":null,"model":null,"role":"Spark_Living","projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Remove bloom, toxic spores, lure/DOT/slow state, and toxic_spores marker at 5s.
- **locks:** rules 1,2,4,7,8; T2; systemic Spark_Living misfit.
- **current gap:** Manifest legacy=true has Spark_Living role, generic stone-dust travel, and poison coating impact; it does not show a nightshade seed/bloom lure. [INFERRED]

### frolick — Frolick (bloom)
- **author says:** "Dance among flowers, healing and gaining speed while leaving a temporary mixed flower trail" — style theme: "Poison, healing, nature magic"
- **data:** `{"id":"frolick","name":"Frolick","description":"Dance among flowers, healing and gaining speed while leaving a temporary mixed flower trail","damage_percent":0,"cooldown_seconds":6,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"heal+attack_buff+speed","categories":["healing","buff"],"heal_percent":5,"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":10}`
- **behavior over time:** t0: enter dance/heal/speed stance over 0.28s -> for 10s heal 5%, gain attack/speed buff, and leave a temporary mixed flower trail on the ground as the caster moves -> trail and buffs end, then 0.2s recovery. [INFERRED]
- **targeting:** self-targeted moving buff; no aim; trail follows caster movement.
- **visuals:** Flowers/moss are actual temporary ground trail blocks or markers, with nature particles; Acid_Sparks palette is explicitly rejected by the MED misfit row. T11 and rules 1,6,7. CURRENT STATE manifest row: `{"cast":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner","loop":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Remove every flower/moss trail piece, buff aura, and heal visual at 10s; restore trail terrain.
- **locks:** rules 1,6,7,8; T2,T11; Bloom frolick MED misfit.
- **current gap:** Manifest legacy=true is generic lantern/stone dust/mace shockwave with lantern loop; the canon says current Acid_Sparks-style read must become a ground flower trail. [INFERRED]

### cacti_cluster — Cacti Cluster (bloom)
- **author says:** "Launch a heavy cactus cluster that sticks, poisons, slows, then bursts" — style theme: "Poison, healing, nature magic"
- **data:** `{"id":"cacti_cluster","name":"Cacti Cluster","description":"Launch a heavy cactus cluster that sticks, poisons, slows, then bursts","damage_percent":5,"cooldown_seconds":5,"cast_time_seconds":0.32,"recovery_seconds":0.22,"effect":"dot+slow","categories":["dot","debuff"],"resource_cost":0,"cast_type":"projectile","target_type":"enemy","range":14,"radius":4,"width":2,"duration_seconds":4,"projectile_speed":10,"travel_type":"cactus_cluster"}`
- **behavior over time:** t0: cast 0.32s -> heavy cactus cluster travels visibly at speed 10 to an enemy within 14, sticks for 4s, poisons/slows and then bursts -> 0.22s recovery and all cactus effects clear. [INFERRED]
- **targeting:** crosshair-aimed, dodgeable enemy projectile within 14; radius 4 and width 2 describe the cluster/hit volume.
- **visuals:** Physical cactus cluster silhouette first, visible slow travel, thorn stick/poison state, and a distinct burst at expiry; avoid Spark_Living and use same-family desert/plant accents (rules 1,2,7). CURRENT STATE manifest row: `{"cast":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"MOTM_Proof_Coating_Poison","loop":null,"model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Remove cactus model, poison/slow carrier, burst residue, and any trail after stick/expiry at 4s.
- **locks:** rules 1,2,7,8; U2 projectile engine; systemic Spark_Living misfit.
- **current gap:** Manifest legacy=true has generic lantern/stone travel and poison coating impact with no model/role; current row lacks the heavy cactus/stick/burst identity. [INFERRED]

## Style `self_petrification` — Self Petrification
Theme: "Stone form, stuns, invulnerability"

### gargoyle — Gargoyle (self_petrification)
- **author says:** "Become an ancient statue, heal, become untargetable, and heavily reduce incoming damage while locked in place" — style theme: "Stone form, stuns, invulnerability"
- **data:** `{"id":"gargoyle","name":"Gargoyle","description":"Become an ancient statue, heal, become untargetable, and heavily reduce incoming damage while locked in place","damage_percent":0,"cooldown_seconds":7,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"heal+damage_reduction+untargetable","categories":["healing","shielding"],"heal_percent":35,"shield_percent":0,"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":5,"terrain_effect":"stone_shell"}`
- **behavior over time:** t0: harden during 0.28s -> become an ancient statue, heal 35%, become untargetable and heavily reduce incoming damage while locked in place for 5s -> shell ends and 0.2s recovery. [INFERRED]
- **targeting:** self-targeted locked transformation; no aim.
- **visuals:** Stone statue model/tint and root/stone-shell silhouette hug the player; self_petrification HIGH misfit requires Stoneskin.json/stone-tint grammar and killing Impact_Ice_Shockwave (T4, rule 6). CURRENT STATE manifest row: `{"cast":"MOTM_Proof_Coating_Stone","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner","loop":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** SFX_Stone_Break (P5) [INFERRED].
- **cleanup:** Remove stone shell/tint, untargetable and damage-reduction visuals exactly at 5s; no permanent trail or residual target suppression.
- **locks:** rules 6,7,8; T4; self_petrification HIGH misfit.
- **current gap:** Manifest legacy=true uses MOTM stone coating cast, generic stone travel and mace shockwave impact; it lacks explicit Stoneskin/stone-shell model and cleanup proof. [INFERRED]

### glare — Glare (self_petrification)
- **author says:** "Petrifying gaze that stuns for 2 turns" — style theme: "Stone form, stuns, invulnerability"
- **data:** `{"id":"glare","name":"Glare","description":"Petrifying gaze that stuns for 2 turns","damage_percent":0,"cooldown_seconds":6,"cast_time_seconds":0.38,"recovery_seconds":0.22,"effect":"stun","categories":["crowd_control"],"resource_cost":0,"charges":2,"charge_recharge_seconds":6.0,"cast_type":"gaze","target_type":"enemy","range":16,"duration_seconds":2.5}`
- **behavior over time:** t0: focus gaze during 0.38s -> petrifying gaze stuns the enemy for 2.5s (two charges available) -> stun ends and 0.22s recovery; unused charge remains on its 6s recharge. [INFERRED]
- **targeting:** enemy gaze at range 16; [INFERRED] line-of-sight/crosshair-facing selection, not a projectile.
- **visuals:** Visible stone gaze/eye line from caster to target and a stone-hardening impact; no Ice_Shockwave proxy, following self-petrification’s stone-skin language (T4, rules 2,7). CURRENT STATE manifest row: `{"cast":"MOTM_Proof_Coating_Stone","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"MOTM_Proof_Coating_Stone","loop":null,"model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Remove gaze beam, petrification hit marker, and stun visual at 2.5s; charge state is HUD/gameplay only and must not leave a body tint.
- **locks:** rules 2,6,7,8; T4; self_petrification HIGH misfit.
- **current gap:** Manifest legacy=true has stone coating cast/impact but generic stone-dust travel and no gaze role; current mapping does not expose a visible gaze link or charge-specific cleanup. [INFERRED]

### tunnel — Tunnel (self_petrification)
- **author says:** "Burrow underground and strike from below" — style theme: "Stone form, stuns, invulnerability"
- **data:** `{"id":"tunnel","name":"Tunnel","description":"Burrow underground and strike from below","damage_percent":12,"cooldown_seconds":7,"cast_time_seconds":0.4,"recovery_seconds":0.16,"effect":"evasion","categories":["damage","evasion","dash"],"resource_cost":0,"cast_type":"dash","target_type":"enemy","range":10,"dash_distance":5,"duration_seconds":5,"delay_seconds":0.4,"launch_height":2.5,"knockup":true,"travel_type":"burrow_strike","terrain_effect":"tunnel_path"}`
- **behavior over time:** t0: begin 0.4s burrow strike -> dash toward enemy up to 5 blocks with a 0.4s delay, remain evasive through the 5s authored duration, then launch 2.5 blocks/knock up on emergence -> 0.16s recovery and tunnel path cleanup. [INFERRED]
- **targeting:** enemy-targeted dash at range 10; [INFERRED] crosshair enemy selection, with movement along the target path.
- **visuals:** Stone/soil emergence, underground tunnel-path marker and below-ground strike; dash shows burst/trail/end cue unless the disappearance exception is explicitly limited to burrow (rules 1,3,7), T4. CURRENT STATE manifest row: `{"cast":"MOTM_Proof_Coating_Stone","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner","loop":null,"model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** SFX_Stone_Break (P5) [INFERRED].
- **cleanup:** Remove tunnel_path, burrow/strike trail, evasion marker, and knockup cue at end; restore terrain.
- **locks:** rules 1,3,7,8; T2,T4; exception register: only burrow is allowed disappearance, so tunnel must retain readable transition.
- **current gap:** Manifest legacy=true is stone coating + generic dust/mace shockwave with no tunnel path/model; current row misses underground travel and emergence timing. [INFERRED]

## Style `soil` — Soil
Theme: "Burrowing, debuffs, area control"

### burrow — Burrow (soil)
- **author says:** "Emerge from the ground in a devastating strike" — style theme: "Burrowing, debuffs, area control"
- **data:** `{"id":"burrow","name":"Burrow","description":"Emerge from the ground in a devastating strike","damage_percent":7,"cooldown_seconds":6,"cast_time_seconds":0.2,"recovery_seconds":0.16,"effect":"knockback","categories":["damage","crowd_control","dash"],"charges":2,"charge_recharge_seconds":5.0,"resource_cost":0,"cast_type":"dash","target_type":"enemy","range":9,"dash_distance":4,"radius":3,"launch_height":3,"knockback_force":4.5,"knockup":true,"travel_type":"underground_burst","terrain_effect":"ruptured_earth"}`
- **behavior over time:** t0: use 0.2s dash cast -> the caster may disappear underground as the one allowed exception, then emerge in a devastating enemy strike over dash distance 4/range 9 -> launch 3 blocks and knock back force 4.5, followed by 0.16s recovery; the ruptured earth footprint is cleaned. [INFERRED]
- **targeting:** enemy-targeted dash to range 9; [INFERRED] crosshair enemy selection and underground path to the target.
- **visuals:** Underground burst with ruptured-earth physical footprint, strong emergence burst and launch cue; burrow is the only allowed disappearance exception to rule 3. T1/T12 replace generic proxies with owned block markers. CURRENT STATE manifest row: `{"cast":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner","loop":null,"model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** SFX_Golem_Earth_Stomp_Impact + SFX_Stone_Break (P5) [INFERRED].
- **cleanup:** Remove underground burst, rupture marker, dust, and knockup carrier after strike; restore ruptured_earth blocks.
- **locks:** rules 1,2,3,7,8; T1,T2,T12; exception register: burrow disappearance allowed.
- **current gap:** Manifest legacy=true is generic lantern/stone dust/mace shockwave with no model/role; current row does not prove the allowed underground disappearance or 4.5-force emergence. [INFERRED]

### mudpit — Mudpit (soil)
- **author says:** "Trap the enemy in sticky mud" — style theme: "Burrowing, debuffs, area control"
- **data:** `{"id":"mudpit","name":"Mudpit","description":"Trap the enemy in sticky mud","damage_percent":1,"cooldown_seconds":2,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"slow+vulnerability","categories":["damage","debuff"],"resource_cost":0,"cast_type":"ground_zone","target_type":"ground_target","range":12,"radius":5,"duration_seconds":6,"terrain_effect":"mudpit"}`
- **behavior over time:** t0: cast 0.55s at a ground point -> physical sticky mudpit radius 5 persists 6s, dealing 1% damage and slow+vulnerability -> field expires and 0.32s recovery ends. [INFERRED]
- **targeting:** crosshair-aimed ground-target at range 12; no projectile travel field is authored, so the ground zone must telegraph before activation. [INFERRED]
- **visuals:** Actual clay/mud blocks or owned mud marker with thick sticky silhouette; particles accent, not replace, the zone (rule 1), T2. CURRENT STATE manifest row: `{"cast":"Server/Particles/Item/Lantern/Spawners/Earth_Brazier_Glow.particlespawner","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"Server/Particles/Combat/Mace/Signature/Spawners/Mace_Signature_Shockwave.particlespawner","loop":"MOTM_Proof_Debris_Wave","model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Remove mudpit blocks/marker, slow/vulnerability/DOT carrier and restore original terrain after 6s.
- **locks:** rules 1,2,7,8; T2; systemic field proxy misfit.
- **current gap:** Manifest legacy=true is generic lantern/stone dust/mace shockwave with Debris_Wave loop; it does not read as a sticky mudpit or guarantee terrain restore. [INFERRED]

### debris — Debris (soil)
- **author says:** "Fling debris that blinds and weakens" — style theme: "Burrowing, debuffs, area control"
- **data:** `{"id":"debris","name":"Debris","description":"Fling debris that blinds and weakens","damage_percent":1,"cooldown_seconds":5,"cast_time_seconds":0.32,"recovery_seconds":0.22,"effect":"vulnerability+blind","categories":["damage","debuff"],"resource_cost":0,"cast_type":"projectile_volley","target_type":"enemy","range":10,"width":5,"height":6,"projectile_speed":20,"travel_type":"debris_spray"}`
- **behavior over time:** t0: cast 0.32s -> a visible debris spray/volley travels at speed 20 toward an enemy within 10, dealing 1% damage and applying vulnerability+blind -> volley ends and 0.22s recovery. [INFERRED]
- **targeting:** crosshair-aimed, dodgeable enemy projectile volley within 10; width 5 and height 6 are the authored spray volume.
- **visuals:** Physical rock/soil rubble volley with visible debris spray and impact dust; T1 `Soil_Debris`/Block_Break_Stone, not Spark_Living, while rules 1,2,7 govern readable travel. CURRENT STATE manifest row: `{"cast":"MOTM_Proof_Debris_Wave","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"MOTM_Proof_Debris_Wave","loop":"MOTM_Proof_Debris_Wave","model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** SFX_Stone_Break (P5) [INFERRED].
- **cleanup:** Remove every debris projectile, blind/vulnerability carrier, and spray particle after impact; no terrain_effect is authored.
- **locks:** rules 1,2,7,8; T1/U2; systemic Spark_Living misfit.
- **current gap:** Manifest legacy=true uses MOTM_Proof_Debris_Wave cast/impact/loop but generic stone-dust travel and no model/role; current row partially reads debris yet lacks authored volley geometry. [INFERRED]

## Style `sand` — Sand
Theme: "Desert storms, erosion, glass"

### sandstorm — Sandstorm (sand)
- **author says:** "Conjure a blinding sandstorm that damages over time" — style theme: "Desert storms, erosion, glass"
- **data:** `{"id":"sandstorm","name":"Sandstorm","description":"Conjure a blinding sandstorm that damages over time","damage_percent":1,"cooldown_seconds":6,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"dot+slow","categories":["dot","debuff","aoe"],"resource_cost":0,"cast_type":"self_buff","target_type":"self","radius":5,"duration_seconds":10,"toggleable":true,"toggle_cooldown_seconds":2,"terrain_effect":"sandstorm"}`
- **behavior over time:** t0: build a caster self-buff over 0.55s -> blinding sandstorm radius 5 damages 1% per tick and slows for 10s, toggleable with 2s toggle cooldown -> end/toggle cleanup and 0.32s recovery. [INFERRED]
- **targeting:** self-centered self-buff/weather field radius 5; no crosshair aim; toggle controls active duration.
- **visuals:** MOTM_Terra_Sand_Cast/Travel/Impact/Loop plus dense ochre sand cloud and erosion silhouettes; `Sand_Storm` family/Block_Break_Sand/Soil_Sand, never white smoke (T7 and sand canon). CURRENT STATE manifest row: `{"cast":"MOTM_Terra_Sand_Cast","travel":"MOTM_Terra_Sand_Travel","impact":"MOTM_Terra_Sand_Impact","loop":"MOTM_Terra_Sand_Loop","model":null,"role":null,"projectileConfig":null}`.
- **sound:** unassigned.
- **cleanup:** Remove sand cloud, tint, DOT/slow aura and sandstorm terrain/weather marker at toggle or 10s; no lingering particle tail or tint.
- **locks:** rules 1,6,7,8; T7; sand canon no white smoke; high-fidelity sandstorm reference.
- **current gap:** Current manifest has no legacy flag and maps all four MOTM sand phases, but the status log says Sand_Storm ambient scale was invisible and Block_Sprint_Sand is the correction; remaining gap is readable radius-5 gameplay scale/cleanup. [INFERRED]

### dust_devil — Dust Devil (sand)
- **author says:** "Spin up a dust devil that knocks back" — style theme: "Desert storms, erosion, glass"
- **data:** `{"id":"dust_devil","name":"Dust Devil","description":"Spin up a dust devil that knocks back","damage_percent":5,"cooldown_seconds":5,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"knockback","categories":["damage","crowd_control"],"resource_cost":0,"cast_type":"dash","target_type":"self_centered","range":5,"dash_distance":5,"radius":5,"duration_seconds":2,"knockback_force":4,"travel_type":"rolling_tornado","terrain_effect":"dust_devil"}`
- **behavior over time:** t0: caster dashes 5 blocks AS a rolling sand tornado over 2s (travel_type rolling_tornado), knocking back (force 4) everything in the swept radius-5 path; canon rule 3 (burst + trail + end cue over time, grill rejected teleport-read), sand canon (no white smoke). Current build: no client displacement, instant snap-knockback, foot dust - misses the identity, not just the movement. Displacement mechanism is PROVEN territory: GrapplingHook (catalog mod-patterns.md) moves live players via velocity impulses - use that, not position writes.
- **targeting:** caster self-centered dash; crosshair/facing sets the forward sweep [INFERRED], and the rolling tornado is a visible, dodgeable moving area rather than an instant hit.
- **visuals:** MOTM_Terra_Sand_Cast/Travel/Impact/Loop composed as a rolling ochre sand tornado; start burst, continuous swept radius-5 trail, end cue; no white smoke. Physical sand silhouette first, particles second (rules 1,2,3,7). CURRENT STATE manifest row: `{"cast":"MOTM_Terra_Sand_Cast","travel":"MOTM_Terra_Sand_Travel","impact":"MOTM_Terra_Sand_Impact","loop":"MOTM_Terra_Sand_Loop","model":null,"role":null,"projectileConfig":null}`.
- **sound:** unassigned.
- **cleanup:** Remove tornado, sand trail, knockback carrier, and dust_devil terrain marker immediately at the 2s end; no lingering tint/particles; no terrain blocks should remain.
- **locks:** rules 1,2,3,7,8; sand canon; Phase 2.4 status contract; no teleport exception.
- **current gap:** Current manifest has no legacy flag and maps MOTM sand phases, but status log confirms no client displacement, instant snap-knockback and foot dust only; it misses rolling velocity-impulse tornado identity. [INFERRED]

### vitrification — Vitrification (sand)
- **author says:** "Superheated sand burns the enemy" — style theme: "Desert storms, erosion, glass"
- **data:** `{"id":"vitrification","name":"Vitrification","description":"Superheated sand burns the enemy","damage_percent":0,"cooldown_seconds":4,"cast_time_seconds":0.35,"recovery_seconds":0.24,"effect":"sand_empower","categories":["dot"],"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":8,"travel_type":"heated_glass_shard"}`
- **behavior over time:** t0: apply superheated-sand empowerment during 0.35s -> for 8s the caster’s relevant attack burns its enemy using a heated-glass-shard travel read -> buff expires and 0.24s recovery ends. [INFERRED]
- **targeting:** self-targeted empowerment; no aim at cast; subsequent attack target uses its normal crosshair semantics. [INFERRED]
- **visuals:** Ochre superheated sand/glass-shard aura and visible heated-glass-shard attack accent; sand palette uses Sand_Storm/Block_Break_Sand/Soil_Sand and rejects white smoke (T7). CURRENT STATE manifest row: `{"cast":"MOTM_Terra_Sand_Cast","travel":"MOTM_Terra_Sand_Travel","impact":"MOTM_Terra_Sand_Impact","loop":"MOTM_Terra_Sand_Loop","model":null,"role":null,"projectileConfig":null}`.
- **sound:** unassigned.
- **cleanup:** Remove heat aura, shard-ready state and all burn carrier visuals at 8s or consumption; no lingering tint/trail.
- **locks:** rules 2,6,7,8; T1,T7; sand canon no white smoke; behavior of empowered attack is [INFERRED].
- **current gap:** Current manifest has no legacy flag and maps MOTM sand phases, but it does not declare a model/role/projectileConfig for the 8s sand empowerment; exact attack trigger remains [INFERRED].

## Style `gem` — Gem
Theme: "Crystals, shields, refraction"

### lapidary — Lapidary (gem)
- **author says:** "Place a persistent green gem cube that can be recalled and acts as the anchor for Gem abilities" — style theme: "Crystals, shields, refraction"
- **data:** `{"id":"lapidary","name":"Lapidary","description":"Place a persistent green gem cube that can be recalled and acts as the anchor for Gem abilities","damage_percent":0,"cooldown_seconds":7,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"persistent_object","categories":["summon","utility"],"shield_percent":0,"resource_cost":0,"cast_type":"ground_target","target_type":"ground_target","duration_seconds":30,"terrain_effect":"crystal_gem"}`
- **behavior over time:** t0: place a green gem cube at a ground target during 0.28s -> persistent crystal anchor lasts 30s and can be recalled/anchors Gem abilities -> expiry/recall removes it and 0.2s recovery ends. [INFERRED]
- **targeting:** ground-target at crosshair-selected point; no enemy aim; range/distance is not authored.
- **visuals:** Physical persistent green gem cube with full readable anchor silhouette; T9 uses Rock_Crystal_* block anchor and crystal glow, particles accent only (rule 1). CURRENT STATE manifest row: `{"cast":"MOTM_Proof_Gem_Green","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"MOTM_Proof_Gem_Green","loop":"MOTM_Proof_Gem_Green","model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Remove gem cube, glow, anchor registry and crystal_gem terrain marker at recall or 30s; restore original block.
- **locks:** rules 1,7,8; T2,T9; gem high-fidelity ceiling; lapidary presentation details are an open author question.
- **current gap:** Manifest legacy=true maps MOTM_Proof_Gem_Green cast/impact/loop but generic stone-dust travel and no physical gem model; current row does not express a persistent 30s anchor. [INFERRED]

### fracture — Fracture (gem)
- **author says:** "Shatter the active gem in a fast expanding green crystal burst" — style theme: "Crystals, shields, refraction"
- **data:** `{"id":"fracture","name":"Fracture","description":"Shatter the active gem in a fast expanding green crystal burst","damage_percent":40,"cooldown_seconds":8,"cast_time_seconds":0.35,"recovery_seconds":0.24,"effect":"burst","categories":["damage"],"resource_cost":0,"cast_type":"ground_burst","target_type":"self_centered","range":20,"radius":20,"height":12,"terrain_effect":"crystal_fracture","travel_type":"crystal_shatter"}`
- **behavior over time:** t0: shatter the active gem during 0.35s -> fast green crystal burst expands to radius 20 and height 12, dealing 40% burst damage -> impact cleanup and 0.24s recovery. [INFERRED]
- **targeting:** self-centered burst at the active gem anchor; authored target_type is self_centered, with range 20 describing reach.
- **visuals:** Physical green crystal shatter from active gem; high-fidelity crystal burst with crystal-shard travel/impact and entity-only Explode (DamageBlocks:false) per T9, avoiding terrain damage. CURRENT STATE manifest row: `{"cast":"MOTM_Proof_Gem_Green","travel":"Server/Particles/Block/Crystal/Spawners/Block_Break_Crystal_Sparks.particlespawner","impact":"MOTM_Proof_Gem_Green","loop":"MOTM_Proof_Gem_Green","model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Remove all crystal shards/burst particles and crystal_fracture marker immediately after burst; do not leave broken blocks or terrain damage (T9).
- **locks:** rules 1,2,7,8; T6,T9; gem high-fidelity ceiling.
- **current gap:** Manifest legacy=true maps Gem Green phases and crystal sparks travel but no model/role; it is close visually yet lacks explicit active-gem anchoring and entity-only fracture semantics. [INFERRED]

### refraction — Refraction (gem)
- **author says:** "Project a bright green light aura around the active gem that boosts allies inside it" — style theme: "Crystals, shields, refraction"
- **data:** `{"id":"refraction","name":"Refraction","description":"Project a bright green light aura around the active gem that boosts allies inside it","damage_percent":0,"cooldown_seconds":5,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"damage_reduction+heal+damage_buff","categories":["buff"],"resource_cost":0,"cast_type":"support_zone","target_type":"self_centered","radius":20,"height":12,"duration_seconds":0,"toggleable":true,"toggle_cooldown_seconds":5,"terrain_effect":"crystal_refraction"}`
- **behavior over time:** t0: activate bright green aura around the active gem during 0.28s -> allies inside radius 20/height 12 receive damage reduction, healing and damage buff while toggle remains active (duration_seconds=0) -> toggle-off/cooldown 5s removes the field and 0.2s recovery ends. [INFERRED]
- **targeting:** self-centered support zone centered on active gem; no crosshair aim; radius 20 and height 12 define the aura volume.
- **visuals:** Bright green light aura around the physical gem, using fullbright Light-block glow trick if needed (T9/R9); allies and field remain visibly associated with the anchor. CURRENT STATE manifest row: `{"cast":"MOTM_Proof_Gem_Green","travel":"Server/Particles/Block/Stone/Spawners/Block_Break_Stone_Dust.particlespawner","impact":"MOTM_Proof_Gem_Green","loop":"MOTM_Proof_Gem_Green","model":null,"role":null,"projectileConfig":null,"legacy":true}`.
- **sound:** unassigned.
- **cleanup:** Remove aura, support effects, light blocks/markers and crystal_refraction terrain state at toggle-off; restore any temporary blocks, no lingering glow.
- **locks:** rules 1,6,7,8; T2,T9; gem high-fidelity ceiling.
- **current gap:** Manifest legacy=true maps Gem Green cast/impact/loop but generic stone-dust travel and no model/role; current row lacks active-anchor association and physical light-block volume. [INFERRED]

## Open questions for the author

- `alloy_enhancement`: the JSON establishes a three-use physical prime, but does not author the exact weapon/tool visual or whether it expires on timeout versus only after uses.
- `iron_wall`: the JSON gives line/range/width/height/duration but does not author block material geometry beyond the `iron_wall` terrain effect.
- `vitrification`: the 8-second self-buff and `heated_glass_shard` travel type do not state which subsequent attack consumes the empowerment or its exact target behavior.
- `lapidary`: persistent green cube placement/recall presentation and the active-anchor interaction details are not author-stated beyond the description/data.
- `glare`: the gaze beam/line-of-sight presentation is [INFERRED] from `cast_type=gaze`; the author has not specified its exact visual carrier.
- `tunnel`: the authored 5-second duration, 0.4-second delay, and dash fields do not specify whether the player remains underground for the whole duration; timing above is [INFERRED].

Sources: Terra style rows `src/main/resources/data/styles/terra_styles.json`; class/passive `src/main/resources/data/classes/terra.json`; current visual mappings `src/main/resources/data/ability-visual-manifest.json`; canon Secs. 1–2 and G13 addendum; proposals T1–T12; implementation-plan Status log dust_devil finding.
