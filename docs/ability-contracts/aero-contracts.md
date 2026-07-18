# Aero Ability Contracts

## Class header

- **class fantasy:** "Swift as the wind and deadly as lightning."
- **palette:** `#9370DB/#FFD700/#E6E6FA`
- **passive:** **Wind Walker** — "You move 25% faster at all times and your native Hytale energy bar is increased by 80%. Vertical movement from Aero style abilities is handled by the style ability itself so Wind Walker does not duplicate jumps, dives, hovers, or launch resets."
- **universal rule 1:** Physical object first, particles second; particles accent rather than substitute for the physical read.
- **universal rule 2:** Projectiles and waves have visible travel before impact and remain watchable/dodgeable.
- **universal rule 3:** Dash-family movement is burst, not teleport: start burst, trail, and end cue over time; `burrow` is the only disappearance exception.
- **universal rule 4:** Summons visibly fight; spawning alone is not the fantasy.
- **universal rule 5:** Tethers are visible, themed links synchronized with movement.
- **universal rule 6:** Coatings hug the body; there is no permanent class tint or permanent trail.
- **universal rule 7:** Composition stack is silhouette, palette, motion, then silence; max one wrong-family asset per stack.
- **universal rule 8:** Cleanup is part of the visual: no drops, stuck visuals, or lingering tints.
- **Aero locked canon:** Wind Walker is deliberately visual-less; Aero impacts are dash/wind-burst cues, never portal cues. Smoke Form uses the player's own silhouette in dense smoke, moves through enemies, grants 50% projectile damage reduction, has no terrain noclip, and toggles for 7 seconds. Wind blades are pale-yellow arcs with visible travel; Gale Cutter crosses as an X; Razor Wind is five distinct sequential slashes; Chain Lightning shows one visible hop at a time, up to six targets with 3-block jumps; Pressure Burst is a visibly growing hold-charge shot.

## Scream — Sonic attacks, stuns, team buffs

### shriek — Shriek (scream)
- **author says:** "Deafen enemy, reduce their accuracy" — style theme: "Sonic attacks, stuns, team buffs"
- **data:** `{"id":"shriek","name":"Shriek","description":"Deafen enemy, reduce their accuracy","damage_percent":0,"cooldown_seconds":3,"cast_time_seconds":0.25,"recovery_seconds":0.18,"effect":"deafen","categories":["debuff"],"resource_cost":0,"cast_type":"cone","target_type":"cone","range":10,"cone_angle":70,"duration_seconds":4}`
- **behavior over time:** t0 (cast moment) opens a sonic cone after 0.25 seconds; [INFERRED] a readable vocal shock front occupies the 70-degree cone during the active hit window. Enemies in the cone receive `deafen` for 4 seconds; recovery is 0.18 seconds. [INFERRED] The cone ends after its pulse and leaves no persistent body effect beyond the status duration.
- **targeting:** Facing cone, 10-block range, 70-degree angle; it is not a crosshair projectile. The cone is resolved in front of the caster at cast time. [INFERRED] Bystanders outside that cone are unaffected.
- **visuals:** Intended cast/travel/impact: a physically readable vocal shockwave with pale-lavender/gold accent and a distinct outward sonic front, using the Scream A- stack; physical wave first, particles second, visible travel before the debuff impact. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** unassigned
- **cleanup:** End the sonic front immediately after its pulse; remove any cast/travel/impact particles and temporary tint at completion. The 4-second deafen status may expire on its own; no field or terrain remains.
- **locks:** Universal rules 1, 2, 7, 8; Aero Scream A- canon; A10 (scream/battle_cry); no G# lock.
- **current gap:** The legacy row uses Battleaxe/Spirit Wind generic assets rather than a dedicated sonic stack. `legacy=true`; the intended Scream visual is not represented by the current row.

### sonic_boom — Sonic Boom (scream)
- **author says:** "Shockwave that stuns" — style theme: "Sonic attacks, stuns, team buffs"
- **data:** `{"id":"sonic_boom","name":"Sonic Boom","description":"Shockwave that stuns","damage_percent":12,"cooldown_seconds":4,"cast_time_seconds":0.35,"recovery_seconds":0.24,"effect":"stun","categories":["damage","crowd_control"],"resource_cost":0,"cast_type":"wave_line","target_type":"line","range":14,"width":6,"knockback_force":3.5,"travel_type":"sonic_wave"}`
- **behavior over time:** t0 emits a line wave after 0.35 seconds; [INFERRED] the sonic wave visibly travels along its 14-block line and is active across width 6. It deals 12% damage, applies stun, and knocks targets with force 3.5; recovery is 0.24 seconds. [INFERRED] The wave ends at range or on its impact boundary rather than persisting.
- **targeting:** Visible, dodgeable wave-line aimed along the caster's facing line; range 14 and width 6. This is a line wave, not an auto-acquire projectile. [INFERRED] The line's forward direction is selected at cast time.
- **visuals:** Intended cast/travel/impact: a large physical sonic shock front with a visible linear travel phase, gold/lavender edge accents, and a strong impact cue; A10 explicitly adds `Battleaxe_Bash_Shockwave` to the sonic boom stack. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** unassigned
- **cleanup:** Remove the traveling shock front and impact burst at the end of the line; clear temporary stun presentation after the status expires. No terrain effect is set, so no terrain restoration is required.
- **locks:** Universal rules 1, 2, 7, 8; A10; no G# lock.
- **current gap:** Impact is the intended A10 cue, but cast/travel remain generic Battleaxe/Spirit Wind rows and the row is still marked legacy.

### battle_cry — Battle Cry (scream)
- **author says:** "Rally yourself and nearby allies with a sonic battle cry, granting +15% damage and +10% speed." — style theme: "Sonic attacks, stuns, team buffs"
- **data:** `{"id":"battle_cry","name":"Battle Cry","description":"Rally yourself and nearby allies with a sonic battle cry, granting +15% damage and +10% speed.","damage_percent":0,"cooldown_seconds":15,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"attack_buff+speed","categories":["buff"],"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":10,"radius":8,"terrain_effect":"resonant_aura"}`
- **behavior over time:** t0 begins a self-centered sonic pulse after 0.28 seconds. The caster and nearby allies within radius 8 receive `attack_buff+speed` for 10 seconds; recovery is 0.2 seconds. [INFERRED] The +15% damage and +10% speed values in the author description are represented by that effect for the active window. [INFERRED] The resonant aura remains readable around the group during the buff and ends with the status.
- **targeting:** Self-centered ally area, radius 8; no projectile aim or auto-acquire. [INFERRED] The caster is always included and eligible allies are those inside the radius at application.
- **visuals:** Intended cast/loop/impact: a sonic ring expands from the caster into a restrained resonant aura around caster and allies, with silhouette-first body readability and gold/lavender sonic accents. G14 locks radius 8; A10 calls for a 10-second ally pulse. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=Server/Particles/Combat/Battleaxe/Signature/Battleaxe_Signature_Whirlwind.particlesystem; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** unassigned
- **cleanup:** Remove the sonic pulse and resonant aura at 10 seconds or early expiration; clear attack and speed buff presentation. Restore any temporary terrain-effect marker for `resonant_aura`; no persistent terrain may remain.
- **locks:** G14 (radius 8); A10; universal rules 1, 7, 8; no G# lock beyond G14.
- **current gap:** The current loop is a generic Battleaxe whirlwind and all fields are legacy; it does not yet express the locked 8-block ally pulse as a dedicated sonic composition.

## Jet — Fast dashes, momentum

### jet_burst — Jet Burst (jet)
- **author says:** "Burst in the aimed direction, displacing enemies you pass without dealing damage." — style theme: "Fast dashes, momentum"
- **data:** `{"id":"jet_burst","name":"Jet Burst","description":"Burst in the aimed direction, displacing enemies you pass without dealing damage.","damage_percent":0,"cooldown_seconds":2,"cast_time_seconds":0.2,"recovery_seconds":0.16,"effect":"knockback","categories":["damage","crowd_control","dash"],"charges":3,"charge_recharge_seconds":2.0,"resource_cost":0,"cast_type":"dash","target_type":"enemy","range":10,"dash_distance":10,"launch_height":2.5,"knockup":true,"travel_type":"jet_burst"}`
- **behavior over time:** t0 starts the aimed dash after 0.2 seconds, covering dash distance 10 with launch height 2.5; [INFERRED] a start burst, continuous movement, and end cue span the dash rather than snapping. Enemies passed are displaced/knocked up without damage; three charges recharge at 2.0 seconds each. Recovery is 0.16 seconds.
- **targeting:** Aimed dash through the enemy path, range 10 and dash distance 10; not a projectile and not auto-acquire despite `target_type=enemy`. [INFERRED] The aim direction is the movement vector selected at cast time.
- **visuals:** Intended cast/travel/impact: gold jet-fire start burst, visible wind/jet trail, and a wind-burst end cue; A7 specifies the Zephyr movement pattern and A4 replaces portal cues with Dagger_Dash/Daggers_Dash_Straight/Wind_Sparks_Tail. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado (Aero wind palette; [INFERRED] mapping for Jet Burst)
- **cleanup:** Remove start/travel/end dash cues when movement ends; clear knock-up presentation and any temporary trail. No terrain effect is set.
- **locks:** Universal rule 3; A4; A7; Aero impact is dash/wind burst, never portal; no G# lock.
- **current gap:** Current legacy visuals are generic Battleaxe/Spirit Wind and lack the proven Zephyr/Dagger dash composition; the gap is a movement read, not a damage read.

### afterburner — Afterburner (jet)
- **author says:** "Accelerate with jet-fire trailing behind you for 8 seconds; enemies touching the trail burn." — style theme: "Fast dashes, momentum"
- **data:** `{"id":"afterburner","name":"Afterburner","description":"Accelerate with jet-fire trailing behind you for 8 seconds; enemies touching the trail burn.","damage_percent":15,"cooldown_seconds":5,"cast_time_seconds":0.2,"recovery_seconds":0.16,"effect":"burn","categories":["dot","dash"],"resource_cost":0,"cast_type":"dash","target_type":"line","range":14,"dash_distance":14,"duration_seconds":8,"travel_type":"afterburner_dash","terrain_effect":"ember_trail"}`
- **behavior over time:** t0 begins a 14-block dash after 0.2 seconds and leaves jet-fire behind for 8 seconds. Enemies touching the trail burn for the authored 15% damage; recovery is 0.16 seconds. [INFERRED] The ember trail persists as a bounded path for the duration and then expires.
- **targeting:** Line dash, range 14 and dash distance 14; the caster's aimed movement line defines the trail. [INFERRED] It is not a crosshair projectile; trail contact is the hit condition.
- **visuals:** Intended cast/travel/loop: gold jet-fire physically traces the dash path, with visible wind acceleration and a finite ember trail; A7 explicitly calls for gold `Fire_Charge1/Fire_AoE_Grow` for the jet/afterburner family. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado (Aero wind palette; [INFERRED] mapping for Afterburner)
- **cleanup:** Remove dash trail and ember terrain effect after 8 seconds; restore every terrain cell changed by `ember_trail`; clear burn visuals when their status ends. Rule 8 forbids residual fire/tint.
- **locks:** Universal rules 1, 3, 8; A7; Aero dash/wind-burst impact; no G# lock.
- **current gap:** Current legacy row has no jet-fire loop or terrain-specific asset and uses generic Battleaxe/Spirit Wind cues.

### mach_punch — Mach Punch (jet)
- **author says:** "Powerful strike after dash" — style theme: "Fast dashes, momentum"
- **data:** `{"id":"mach_punch","name":"Mach Punch","description":"Powerful strike after dash","damage_percent":20,"cooldown_seconds":3,"cast_time_seconds":0.2,"recovery_seconds":0.16,"effect":"stun_if_wall","categories":["damage","crowd_control","dash"],"resource_cost":0,"cast_type":"dash_strike","target_type":"enemy","range":8,"dash_distance":8,"knockback_force":4.5,"travel_type":"mach_punch"}`
- **behavior over time:** t0 starts an 8-block dash-strike after 0.2 seconds; [INFERRED] the strike lands at the dash endpoint during the active hit moment. It deals 20% damage and knockbacks with force 4.5; `stun_if_wall` applies when the hit drives an enemy into a wall. Recovery is 0.16 seconds.
- **targeting:** Enemy-facing dash strike, range and dash distance 8; [INFERRED] direction is aimed at cast time and does not auto-acquire a different target mid-dash.
- **visuals:** Intended cast/travel/impact: visible gold/wind dash burst and trail, then a solid compressed-air impact at the strike, never a portal; A7 Zephyr and A4 dash cue rules constrain the movement. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado (Aero wind palette; [INFERRED] mapping for Mach Punch)
- **cleanup:** Remove dash trail and endpoint burst after impact; clear wall-stun presentation when the conditional status ends. No terrain effect remains.
- **locks:** Universal rule 3; A4; A7; Aero impact is dash/wind burst, never portal; no G# lock.
- **current gap:** The current legacy generic row lacks the authored dash-strike sequencing and wall-impact-specific cue.

## Thunder — Stuns, lightning damage

### thunderclap — Thunderclap (thunder)
- **author says:** "Release a point-blank thunder shockwave that stuns enemies for 3.5 seconds." — style theme: "Stuns, lightning damage"
- **data:** `{"id":"thunderclap","name":"Thunderclap","description":"Release a point-blank thunder shockwave that stuns enemies for 3.5 seconds.","damage_percent":0,"cooldown_seconds":4,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"stun+shocked","categories":["crowd_control","debuff"],"resource_cost":0,"cast_type":"self_burst","target_type":"self_centered","radius":5,"duration_seconds":3.5,"terrain_effect":"thunderclap"}`
- **behavior over time:** t0 emits a self-centered shockwave after 0.28 seconds across radius 5. Enemies hit receive `stun+shocked` for 3.5 seconds; recovery is 0.2 seconds. [INFERRED] The burst is instantaneous at the radius boundary rather than a traveling projectile.
- **targeting:** Self-centered burst, radius 5; no aim, projectile, or auto-acquire semantics.
- **visuals:** Intended cast/impact: physical thunder shockwave with gold/lavender lightning accents, using A3's `Spirit_Thunder` family and Thunderclap cue; visible lightning should be a burst, not a portal. Current manifest row: `cast=Server/Particles/NPC/Void_Dragon/Spawners/Void_Lightning.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Sword/Signature/Spawners/Ready_Flash/Sword_Signature_Ready_Sparks.particlespawner; loop=null; model=Common/NPC/Elemental/Spirit_Thunder/Models/Model.blockymodel; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Global_Weather_Thunder
- **cleanup:** Remove the self-burst and terrain marker at the end of the cast; restore terrain changed by `thunderclap`; clear shocked/stun status visuals when 3.5 seconds elapse.
- **locks:** Universal rules 1, 7, 8; A3; no G# lock.
- **current gap:** Current legacy cast/travel/impact mix includes Void Dragon, Spirit Wind, and Sword assets; canon calls for the Spirit Thunder lightning family.

### smite — Smite (thunder)
- **author says:** "Call down a crosshair-aimed lightning strike with a 3x3 impact footprint." — style theme: "Stuns, lightning damage"
- **data:** `{"id":"smite","name":"Smite","description":"Call down a crosshair-aimed lightning strike with a 3x3 impact footprint.","damage_percent":20,"cooldown_seconds":3,"cast_time_seconds":0.35,"recovery_seconds":0.24,"effect":"lightning","categories":["damage"],"charges":2,"charge_recharge_seconds":3.0,"resource_cost":0,"cast_type":"projectile","target_type":"enemy","range":18,"radius":1.5,"projectile_speed":30,"travel_type":"lightning_bolt"}`
- **behavior over time:** t0 launches after 0.35 seconds; the lightning bolt travels at speed 30 toward range 18, then strikes a radius-1.5 (3x3 footprint) target area for 20% damage. [INFERRED] The projectile remains visible during travel and resolves its lightning impact at the crosshair destination. Two charges recharge at 3.0 seconds each; recovery is 0.24 seconds.
- **targeting:** Crosshair-aimed, dodgeable projectile per the authored description and universal rule 2; target type enemy, range 18. Live status confirms projectile aim must be crosshair, not an off-crosshair snap.
- **visuals:** Intended cast/travel/impact: visible pale-yellow lightning bolt with Spirit Thunder/Lightning family silhouette, then a 3x3 physical impact footprint; A3 names Spell/Lightning, Beam_Lightning2, Spirit_Thunder, and Lightning_Sword cues. Current manifest row: `cast=Server/Particles/NPC/Void_Dragon/Spawners/Void_Lightning.particlespawner; travel=Server/Particles/NPC/Void_Dragon/Spawners/Void_Lightning.particlespawner; impact=Server/Particles/Combat/Sword/Signature/Spawners/Ready_Flash/Sword_Signature_Ready_Sparks.particlespawner; loop=null; model=Common/NPC/Elemental/Spirit_Thunder/Models/Model.blockymodel; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Global_Weather_Thunder
- **cleanup:** Remove lightning travel and impact cues after the strike; clear any target flash at impact. No terrain effect is set.
- **locks:** Universal rules 1, 2, 7, 8; A3; no G# lock.
- **current gap:** Legacy row uses Void Dragon lightning and Sword sparks rather than the A3 Spirit Thunder/Spell Lightning composition; current state is marked legacy.

### chain_lightning — Chain Lightning (thunder)
- **author says:** "Chain lightning through up to six targets, jumping only to the closest target within 3 blocks." — style theme: "Stuns, lightning damage"
- **data:** `{"id":"chain_lightning","name":"Chain Lightning","description":"Chain lightning through up to six targets, jumping only to the closest target within 3 blocks.","damage_percent":10,"cooldown_seconds":5,"cast_time_seconds":0.4,"recovery_seconds":0.24,"effect":"dot","categories":["dot","aoe"],"resource_cost":0,"cast_type":"chain","target_type":"enemy_cluster","range":16,"radius":3,"duration_seconds":4,"travel_type":"chain_lightning"}`
- **behavior over time:** t0 begins the chain after 0.4 seconds and seeks a cluster within range 16. It applies 10% damage over the authored 4-second duration and jumps only to the closest target within 3 blocks, up to six targets. The locked presentation is one visible hop at a time; recovery is 0.24 seconds. [INFERRED] Each hop's link and target pulse end before the next hop begins.
- **targeting:** Enemy-cluster chain; nearest-target auto-acquire is limited to a 3-block jump radius and six-target maximum, not a free-range chain. [INFERRED] The first target is selected from the cluster within range 16.
- **visuals:** Intended cast/travel/impact: one visible lightning link/hop at a time, with each endpoint impact readable before the next hop; A3 explicitly requires per-hop bursts, Spirit Thunder/Spell Lightning family, and no generic proxy silhouette. Current manifest row: `cast=Server/Particles/NPC/Void_Dragon/Spawners/Void_Lightning.particlespawner; travel=Server/Particles/NPC/Void_Dragon/Spawners/Void_Lightning.particlespawner; impact=Server/Particles/Combat/Sword/Signature/Spawners/Ready_Flash/Sword_Signature_Ready_Sparks.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Global_Weather_Thunder
- **cleanup:** Remove each hop link immediately after its hop and remove the final damage-over-time visual at 4 seconds; no dangling tethers or lightning arcs remain.
- **locks:** Universal rules 2, 5, 7, 8; A3; locked one-hop-at-a-time, max six, 3-block jump canon; no G# lock.
- **current gap:** Legacy Void Dragon/Sword rows do not expose one hop at a time and remain marked legacy; per-hop visibility is the explicit live contract.

## Tornado — Whirlwinds, knockback, sustain

### twister — Twister (tornado)
- **author says:** "Send a smaller tornado forward, swirling enemies near the ground with light lift." — style theme: "Whirlwinds, knockback, sustain"
- **data:** `{"id":"twister","name":"Twister","description":"Send a smaller tornado forward, swirling enemies near the ground with light lift.","damage_percent":10,"cooldown_seconds":3,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"knockback","categories":["damage","crowd_control"],"resource_cost":0,"cast_type":"ground_zone","target_type":"ground_target","range":12,"radius":3,"duration_seconds":4,"knockback_force":4,"travel_type":"twister"}`
- **behavior over time:** t0 creates a ground-target tornado after 0.55 seconds, traveling toward range 12. During its 4-second active window and radius 3, it deals 10% damage and swirls enemies near the ground with light lift and knockback force 4. Recovery is 0.32 seconds. [INFERRED] The tornado moves as a visible ground-zone object rather than teleporting.
- **targeting:** Ground-target placement selected along the aimed direction, range 12; not auto-acquire. [INFERRED] The zone's travel path follows the ground from the chosen target point.
- **visuals:** Intended cast/travel/loop/impact: physical wind funnel silhouette moving along the ground, gold/lavender wind accents, visible travel and a bounded 4-second whirl; A1/A8 per-family wind stacks replace generic proxy use. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=Server/Particles/Combat/Battleaxe/Signature/Battleaxe_Signature_Whirlwind.particlesystem; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado
- **cleanup:** Remove moving funnel, lift/knockback particles, and loop at 4 seconds; no lingering wind ring remains.
- **locks:** Universal rules 1, 2, 7, 8; A1/A8; no G# lock.
- **current gap:** Legacy generic Battleaxe/Spirit Wind effects do not provide the authored smaller ground tornado silhouette and are marked legacy.

### funnel_cloud — Funnel Cloud (tornado)
- **author says:** "Create a large funnel cloud that pulls enemies inward and lifts them up to 12 blocks if space allows." — style theme: "Whirlwinds, knockback, sustain"
- **data:** `{"id":"funnel_cloud","name":"Funnel Cloud","description":"Create a large funnel cloud that pulls enemies inward and lifts them up to 12 blocks if space allows.","damage_percent":8,"cooldown_seconds":5,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"dot","categories":["dot","aoe"],"resource_cost":0,"cast_type":"ground_zone","target_type":"ground_target","range":14,"radius":5,"duration_seconds":6,"pull_force":3,"terrain_effect":"funnel_cloud"}`
- **behavior over time:** t0 creates a ground-target funnel after 0.55 seconds at range 14. For 6 seconds, radius 5 pulls enemies inward with force 3, deals 8% damage over time, and lifts them up to 12 blocks if space allows. Recovery is 0.32 seconds. [INFERRED] The cloud remains centered at its ground target while it pulls.
- **targeting:** Ground-target zone, range 14, radius 5; no projectile or auto-acquire semantics. [INFERRED] The target point is selected from the aimed ground location.
- **visuals:** Intended cast/impact/loop: large physical funnel-cloud silhouette at the target, visible inward pull/tether-like wind links, then a six-second bounded loop; A1/A8 per-style wind stack and universal rule 5 constrain the pull read. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=Server/Particles/Combat/Battleaxe/Signature/Battleaxe_Signature_Whirlwind.particlesystem; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado
- **cleanup:** Remove cloud, pull links, damage-over-time loop, and lift cues at 6 seconds; restore any terrain changed by `funnel_cloud` before clearing the effect.
- **locks:** Universal rules 1, 5, 7, 8; A1/A8; no G# lock.
- **current gap:** Current legacy loop is a generic Battleaxe whirlwind and does not establish the large funnel, pull, or 12-block lift read.

### eye_of_the_storm — Eye of the Storm (tornado)
- **author says:** "Create a caster-centered storm zone that protects allies while slowing and damaging enemies." — style theme: "Whirlwinds, knockback, sustain"
- **data:** `{"id":"eye_of_the_storm","name":"Eye of the Storm","description":"Create a caster-centered storm zone that protects allies while slowing and damaging enemies.","damage_percent":0,"cooldown_seconds":7,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"heal+shield","categories":["healing","shielding"],"heal_percent":10,"shield_percent":15,"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":6,"terrain_effect":"eye_of_the_storm"}`
- **behavior over time:** t0 establishes a caster-centered storm zone after 0.28 seconds. It lasts 6 seconds, heals allies for 10% and shields them for 15%; the author description also requires slowing and damaging enemies. Recovery is 0.2 seconds. [INFERRED] The storm zone is continuously active around the caster for the duration.
- **targeting:** Self-centered zone; the caster defines the zone center and no projectile aim is used. [INFERRED] Allies and enemies are filtered by their presence in the zone.
- **visuals:** Intended cast/loop/impact: a readable storm ring/eye centered on the caster, protective aura for allies, and distinct wind/lightning accents for enemy slow/damage; A1/A8 per-style wind stacks and rule 7 require a dedicated silhouette rather than generic proxy effects. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=Server/Particles/Combat/Battleaxe/Signature/Battleaxe_Signature_Whirlwind.particlesystem; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado
- **cleanup:** Remove storm loop, ally shield/heal presentation, enemy slow/damage cues, and terrain effect at 6 seconds; restore terrain affected by `eye_of_the_storm`.
- **locks:** Universal rules 1, 7, 8; A1/A8; no G# lock.
- **current gap:** The current legacy generic loop does not distinguish protective allies from affected enemies or express a caster-centered storm eye.

## Jump — Aerial mobility, diving strikes

### leap — Leap (jump)
- **author says:** "Arm your next jump for 5 seconds, making it 60% higher with forward boost and no damage." — style theme: "Aerial mobility, diving strikes"
- **data:** `{"id":"leap","name":"Leap","description":"Arm your next jump for 5 seconds, making it 60% higher with forward boost and no damage.","damage_percent":0,"cooldown_seconds":4,"cast_time_seconds":0.2,"recovery_seconds":0.16,"effect":"","categories":["damage","crowd_control","debuff","dash"],"charges":2,"charge_recharge_seconds":4.0,"resource_cost":0,"cast_type":"leap","target_type":"enemy","range":10,"dash_distance":10,"launch_height":6,"duration_seconds":5,"radius":3,"travel_type":"jump_arc"}`
- **behavior over time:** t0 arms the next jump after 0.2 seconds; the 5-second arm window provides launch height 6 and forward boost over dash distance 10, with no damage. [INFERRED] The effect is consumed by the next jump or expires at 5 seconds; two charges recharge at 4.0 seconds. Recovery is 0.16 seconds.
- **targeting:** Aerial movement aimed toward the enemy-facing direction, range 10; no damaging projectile. [INFERRED] Forward boost follows the player's selected movement aim.
- **visuals:** Intended cast/travel/impact: gold Wind Sparks Tail arcs show the jump's launch and airborne path, with a clean landing/end cue; A7 Zephyr jump pattern explicitly keeps Wind Walker invisible and gives verticality to the style ability. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado (Aero wind palette; [INFERRED] mapping for Leap)
- **cleanup:** Remove launch/travel arcs and landing cue after the jump; clear any unconsumed arm indicator at 5 seconds. No terrain effect is set.
- **locks:** Universal rule 3; A7; Wind Walker deliberately visual-less and does not duplicate the jump; no G# lock.
- **current gap:** Current legacy row is generic and does not show the authored 60%-higher forward-boost arc or arm/expiry state.

### divebomb — Divebomb (jump)
- **author says:** "While airborne, dive into an aimed ground impact that damages and knocks enemies away." — style theme: "Aerial mobility, diving strikes"
- **data:** `{"id":"divebomb","name":"Divebomb","description":"While airborne, dive into an aimed ground impact that damages and knocks enemies away.","damage_percent":20,"cooldown_seconds":6,"cast_time_seconds":0.5,"recovery_seconds":0.2,"effect":"slow","categories":["damage","debuff"],"resource_cost":0,"cast_type":"dive_strike","target_type":"ground_target","range":12,"radius":4,"launch_height":8,"delay_seconds":0.5,"knockback_force":4.5,"travel_type":"divebomb"}`
- **behavior over time:** t0 launches the caster to height 8 after 0.5 seconds, then dives toward the aimed ground target. The ground impact deals 20% damage, slows enemies, and knocks them away with force 4.5 in radius 4. [INFERRED] The authored `delay_seconds=0.5` is the airborne windup before impact; recovery is 0.2 seconds.
- **targeting:** Aimed ground-target dive, range 12, radius 4; not a projectile. [INFERRED] The ground target is fixed at cast time so bystanders can read and evade the impact.
- **visuals:** Intended cast/travel/impact: gold wind arcs during ascent and visible dive path, followed by a physical wind-burst ground impact; A7 Zephyr and universal rule 3 forbid teleport-like disappearance. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado (Aero wind palette; [INFERRED] mapping for Divebomb)
- **cleanup:** Remove airborne trail and impact burst after landing; clear slow presentation when its status expires. No terrain effect is set.
- **locks:** Universal rules 2, 3, 7, 8; A7; Aero impact is dash/wind burst, never portal; no G# lock.
- **current gap:** Current legacy row lacks an aerial launch, visible dive, and ground-impact-specific stack.

### hang_time — Hang Time (jump)
- **author says:** "Float in the air for 3 seconds, drifting slightly until canceled by jump or another movement ability." — style theme: "Aerial mobility, diving strikes"
- **data:** `{"id":"hang_time","name":"Hang Time","description":"Float in the air for 3 seconds, drifting slightly until canceled by jump or another movement ability.","damage_percent":0,"cooldown_seconds":5,"cast_time_seconds":0.2,"recovery_seconds":0.16,"effect":"evasion","categories":["evasion"],"charges":2,"charge_recharge_seconds":5.0,"resource_cost":0,"cast_type":"air_stall","target_type":"self","duration_seconds":3,"launch_height":5,"travel_type":"hang_time"}`
- **behavior over time:** t0 lifts the caster to launch height 5 after 0.2 seconds and enters a 3-second air stall. During the active window the caster drifts slightly and has `evasion`; jump or another movement ability cancels it. Two charges recharge at 5.0 seconds; recovery is 0.16 seconds. [INFERRED] Cancellation ends the visual immediately rather than waiting for the full duration.
- **targeting:** Self-only air stall; no enemy aim, projectile, or ground target.
- **visuals:** Intended cast/loop: a restrained lavender/gold wind arc around the player's own silhouette, readable as suspension rather than a permanent class tint; A7 keeps Wind Walker invisible and puts verticality in the style ability. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado (Aero wind palette; [INFERRED] mapping for Hang Time)
- **cleanup:** Clear air-stall arcs, evasion cue, and any lift marker on natural expiry or cancellation; no residual hover/tint remains.
- **locks:** Universal rules 6, 7, 8; A7; Wind Walker visual-less/no duplicate vertical movement; no G# lock.
- **current gap:** Current legacy generic row provides no air-stall state, drift cue, or cancellation cleanup.

## Wind Blade — Cutting wind, rapid strikes

### air_slash — Air Slash (wind_blade)
- **author says:** "Fire a 3-block-wide sideways pale-yellow cutting wind arc that pierces enemies." — style theme: "Cutting wind, rapid strikes"
- **data:** `{"id":"air_slash","name":"Air Slash","description":"Fire a 3-block-wide sideways pale-yellow cutting wind arc that pierces enemies.","damage_percent":8,"cooldown_seconds":2,"cast_time_seconds":0.35,"recovery_seconds":0.24,"effect":"","categories":["damage"],"resource_cost":0,"cast_type":"projectile_line","target_type":"enemy","range":16,"width":3,"projectile_speed":28,"travel_type":"wind_blade"}`
- **behavior over time:** t0 launches one cutting arc after 0.35 seconds. It travels visibly at speed 28 to range 16, is 3 blocks wide, and pierces enemies for 8% damage; recovery is 0.24 seconds. [INFERRED] The arc ends at max range or its impact boundary.
- **targeting:** Crosshair-aimed, dodgeable projectile-line; visible travel is mandatory under rule 2 and live projectile aim is crosshair. Target type is enemy, range 16, width 3.
- **visuals:** Intended cast/travel/impact: one distinct pale-yellow (`#FFD700`) wind arc with lavender/white edge (`#E6E6FA`), physically readable silhouette first, visible travel before impact; A2 locks the one-arc geometry and A1 supplies the wind projectile family. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado (Aero wind palette; [INFERRED] mapping for Air Slash)
- **cleanup:** Remove the arc after impact or range 16; clear impact particles and any hit flash immediately. No terrain effect remains.
- **locks:** Universal rules 1, 2, 7, 8; A1/A2; pale-yellow arc and visible travel canon; no G# lock.
- **current gap:** Legacy row is generic Battleaxe/Spirit Wind and does not express the authored pale-yellow 3-wide cutting arc.

### gale_cutter — Gale Cutter (wind_blade)
- **author says:** "Fire two crossing pale-yellow wind slashes in an X formation that pierce enemies." — style theme: "Cutting wind, rapid strikes"
- **data:** `{"id":"gale_cutter","name":"Gale Cutter","description":"Fire two crossing pale-yellow wind slashes in an X formation that pierce enemies.","damage_percent":12,"cooldown_seconds":5,"cast_time_seconds":0.35,"recovery_seconds":0.24,"effect":"","categories":["damage","crowd_control"],"resource_cost":0,"cast_type":"projectile_line","target_type":"enemy","range":18,"width":3,"projectile_speed":30,"knockback_force":4,"travel_type":"gale_cutter"}`
- **behavior over time:** t0 launches two crossing slash projectiles after 0.35 seconds. Each travels visibly at speed 30 to range 18, combining as an X and piercing enemies for 12% damage; recovery is 0.24 seconds. [INFERRED] The two slashes are simultaneous, not a sequential five-shot volley.
- **targeting:** Crosshair-aimed, dodgeable projectile-line pair; target type enemy, width 3, range 18. Live projectile finding requires crosshair aim rather than off-crosshair snapping.
- **visuals:** Intended cast/travel/impact: two distinct pale-yellow arcs crossing visibly into an X, with lavender edge highlights and separate slash silhouettes; A2 locks mirrored configurations with rotation offsets and A1 supplies the wind projectile family. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado (Aero wind palette; [INFERRED] mapping for Gale Cutter)
- **cleanup:** Remove both slash trails and X impact cue after the pair resolves; no arc or hit flash persists.
- **locks:** Universal rules 1, 2, 7, 8; A1/A2; locked X-shaped crossing slashes; no G# lock.
- **current gap:** Current legacy row has no mirrored two-projectile geometry and uses generic wind/impact assets.

### razor_wind — Razor Wind (wind_blade)
- **author says:** "Perform five rapid little wind slashes against enemies in front of you." — style theme: "Cutting wind, rapid strikes"
- **data:** `{"id":"razor_wind","name":"Razor Wind","description":"Perform five rapid little wind slashes against enemies in front of you.","damage_percent":5,"cooldown_seconds":6,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"","categories":["buff"],"resource_cost":0,"cast_type":"projectile_volley","target_type":"enemy_cluster","range":12,"width":5,"projectile_speed":34,"travel_type":"razor_wind"}`
- **behavior over time:** t0 begins a volley after 0.28 seconds. Exactly five rapid, distinct slashes travel in sequence at speed 34 toward the enemy cluster within range 12 and width 5; each applies the authored 5% damage; recovery is 0.2 seconds. [INFERRED] The sequence ends after shot five rather than collapsing into one pulse.
- **targeting:** Crosshair/facing-aimed projectile volley against an enemy cluster, range 12, width 5; each shot must remain visible and dodgeable under rule 2. [INFERRED] The cluster is evaluated along the forward volley lane.
- **visuals:** Intended cast/travel/impact: five individually readable pale-yellow wind slashes with a visible temporal gap/order, not one merged flash; A2 locks `AeroWindVolleyState` and exactly five sequential shots. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado (Aero wind palette; [INFERRED] mapping for Razor Wind)
- **cleanup:** Remove each slash after its impact/range end and clear the fifth-shot volley state; no lingering trail or merged slash remains.
- **locks:** Universal rules 1, 2, 7, 8; A1/A2; locked five distinct sequential slashes; no G# lock.
- **current gap:** Legacy row has generic one-shot-looking cues and does not prove five sequential, distinct slash events.

## Smoke — Stealth, debuffs, evasion

### smoke_bomb — Smoke Bomb (smoke)
- **author says:** "Throw a smoke bomb that creates a 5-block cloud for 6 seconds, blinding and slowing enemies." — style theme: "Stealth, debuffs, evasion"
- **data:** `{"id":"smoke_bomb","name":"Smoke Bomb","description":"Throw a smoke bomb that creates a 5-block cloud for 6 seconds, blinding and slowing enemies.","damage_percent":0,"cooldown_seconds":5,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"blind+slow","categories":["debuff","buff"],"resource_cost":0,"cast_type":"ground_zone","target_type":"ground_target","range":10,"radius":5,"duration_seconds":6,"terrain_effect":"smoke_bomb"}`
- **behavior over time:** t0 throws/places a ground zone after 0.55 seconds at range 10. A radius-5 smoke cloud lasts 6 seconds and blinds/slows enemies; recovery is 0.32 seconds. [INFERRED] The cloud is an actual bounded field at the target rather than a caster-only tint.
- **targeting:** Ground-target zone, range 10, radius 5; no auto-acquire or crosshair projectile semantics.
- **visuals:** Intended cast/impact/loop: physical smoke-bomb cloud first, dense black smoke field (`SmokesRnD+Smoke_Black`) second, with no unrelated wind silhouette; A6 names `Projectile_Config_Bomb_Base` and the six-second field. Current manifest row: `cast=Server/Particles/Combat/Mace/Signature/Spawners/Cast/Mace_Signature_Cast_Smoke.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=Server/Particles/Combat/Mace/Signature/Spawners/Cast/Mace_Signature_Cast_End_Smoke.particlespawner; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** unassigned
- **cleanup:** Remove the bomb, cloud, blind/slow presentation, and loop at 6 seconds; restore any terrain affected by `smoke_bomb`; no smoke particles or tint may linger.
- **locks:** Universal rules 1, 7, 8; A6; smoke field is a physical/object-first field; no G# lock.
- **current gap:** The current legacy travel/impact use Spirit Wind and Battleaxe shockwave assets, not the authored smoke bomb cloud; lingering smoke was a live defect, now fixed for lifecycle but still a fidelity watch item.

### vanish — Vanish (smoke)
- **author says:** "Vanish in smoke for 3 seconds; attacking or casting a damaging ability ends it early." — style theme: "Stealth, debuffs, evasion"
- **data:** `{"id":"vanish","name":"Vanish","description":"Vanish in smoke for 3 seconds; attacking or casting a damaging ability ends it early.","damage_percent":0,"cooldown_seconds":6,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"stealth","categories":["stealth"],"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":3,"terrain_effect":"vanish"}`
- **behavior over time:** t0 wraps the caster in smoke after 0.28 seconds and grants stealth for up to 3 seconds. Attacking or casting a damaging ability ends it early; recovery is 0.2 seconds. [INFERRED] The smoke body cue persists only while stealth is active.
- **targeting:** Self-only buff; no aim or target acquisition.
- **visuals:** Intended cast/loop/end: dense smoke hugs and obscures the player's own silhouette, with a clear fade-in and fade-out; A6 smoke family and universal rule 6 prevent a permanent class tint. Current manifest row: `cast=Server/Particles/Combat/Mace/Signature/Spawners/Cast/Mace_Signature_Cast_Smoke.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=Server/Particles/Combat/Battleaxe/Signature/Battleaxe_Signature_Whirlwind.particlesystem; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** unassigned
- **cleanup:** Remove smoke, stealth tint, and vanish terrain marker at 3 seconds or early cancellation; restore terrain affected by `vanish`; no residue may persist after an attack/cast.
- **locks:** Universal rules 6, 7, 8; A6; no G# lock.
- **current gap:** Legacy row has a generic whirlwind loop and no explicit early-cancel cleanup proof; status log records lingering-smoke history, with lifecycle fixes observed but fidelity still requiring the smoke-family composition.

### smoke_form — Smoke Form (smoke)
- **author says:** "Become smoke, move through enemies, and reduce projectile damage by 50% without terrain noclip." — style theme: "Stealth, debuffs, evasion"
- **data:** `{"id":"smoke_form","name":"Smoke Form","description":"Become smoke, move through enemies, and reduce projectile damage by 50% without terrain noclip.","damage_percent":0,"cooldown_seconds":7,"cast_time_seconds":0.85,"recovery_seconds":0.45,"effect":"evasion","categories":["evasion"],"resource_cost":0,"cast_type":"transformation","target_type":"self","duration_seconds":5,"toggleable":true,"toggle_cooldown_seconds":7.0,"travel_type":"smoke_form"}`
- **behavior over time:** t0 starts the transformation after 0.85 seconds, wrapping the player's own body in dense smoke and allowing movement through enemies. The authored lock grants 50% projectile damage reduction, not 40% evasion; no terrain noclip is permitted. `toggleable=true`; the locked intended toggle-active window is 7 seconds, while the JSON `duration_seconds=5` and `toggle_cooldown_seconds=7.0` remain recorded as authored data. Recovery is 0.45 seconds. [INFERRED] Toggle-off or the locked 7-second end removes the shroud and resistance together.
- **targeting:** Self transformation; no projectile, aim, ground target, or auto-acquire. Movement through enemies is the collision interaction; terrain remains solid.
- **visuals:** Intended cast/loop/end: keep the player's own silhouette wrapped in a dense smoke shroud and `Intangible` phasing, never a Bat morph; G5 locks this composition and A6 names `DamageResistance{Projectile:0.50}`. Current manifest row: `cast=Server/Particles/Combat/Mace/Signature/Spawners/Cast/Mace_Signature_Cast_Smoke.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=Server/Particles/Combat/Mace/Signature/Spawners/Cast/Mace_Signature_Cast_End_Smoke.particlespawner; model=Common/NPC/Flying_Critter/Bat/Models/Model.blockymodel; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). The Bat model is a stale legacy misfit and must be removed.
- **sound:** unassigned
- **cleanup:** On toggle-off or duration end, remove dense smoke, `Intangible`, projectile DR status, and every temporary tint/effect; do not alter or leave terrain noclip state. Rule 8 and the live lingering-smoke finding require explicit expiry cleanup.
- **locks:** G5 (player model plus smoke shroud, no ModelChange/Bat); A6 (50% projectile DR and Intangible); universal rules 6, 7, 8; 7-second toggle canon; no G# lock beyond G5.
- **current gap:** Current legacy row explicitly points to the Bat model and generic wind/Battleaxe assets; this conflicts with G5 and the authored 50% DR/no-terrain-noclip contract. The stale 40% evasion figure is not this contract.

## Gale Wizard — Wind magic, control, reflection

### gust — Gust (gale_wizard)
- **author says:** "Blast enemies with a crosshair-aimed gust that pushes and disorients them." — style theme: "Wind magic, control, reflection"
- **data:** `{"id":"gust","name":"Gust","description":"Blast enemies with a crosshair-aimed gust that pushes and disorients them.","damage_percent":12,"cooldown_seconds":4,"cast_time_seconds":0.35,"recovery_seconds":0.24,"effect":"knockback","categories":["damage","crowd_control"],"resource_cost":0,"cast_type":"projectile","target_type":"enemy","range":16,"projectile_speed":26,"knockback_force":4,"travel_type":"gust_blast"}`
- **behavior over time:** t0 launches a gust after 0.35 seconds. It travels visibly at speed 26 to range 16, deals 12% damage, pushes with force 4, and disorients enemies; recovery is 0.24 seconds. [INFERRED] The gust ends at impact or max range.
- **targeting:** Crosshair-aimed, dodgeable projectile; live status requires crosshair aim for this projectile family, not off-crosshair auto-snap. Target type enemy, range 16.
- **visuals:** Intended cast/travel/impact: compressed wind-burst physical silhouette, gold/lavender palette, visible travel before impact; A1/A8 per-style wind stack replaces the current Spark_Living model. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=Common/NPC/Beast/Spark_Living/Models/Model.blockymodel; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado (Aero wind palette; [INFERRED] mapping for Gust)
- **cleanup:** Remove projectile travel and impact burst after hit/range; clear disorientation presentation when its effect ends. No terrain effect is set.
- **locks:** Universal rules 1, 2, 7, 8; A1/A8; Spark_Living is a systemic misfit; no G# lock.
- **current gap:** Current legacy row uses a Spark_Living proxy model and generic wind particles; it does not yet read as a compressed gust blast.

### cyclone_shield — Cyclone Shield (gale_wizard)
- **author says:** "Surround yourself with a tight cyclone, reducing projectile damage by 65% and all damage by 10%." — style theme: "Wind magic, control, reflection"
- **data:** `{"id":"cyclone_shield","name":"Cyclone Shield","description":"Surround yourself with a tight cyclone, reducing projectile damage by 65% and all damage by 10%.","damage_percent":0,"cooldown_seconds":6,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"defense_buff+shield","categories":["buff","shielding"],"shield_percent":15,"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":6,"terrain_effect":"cyclone_shield"}`
- **behavior over time:** t0 forms a self-centered cyclone after 0.28 seconds. For 6 seconds it grants `defense_buff+shield`, with authored 65% projectile damage reduction, 10% all-damage reduction, and shield percent 15; recovery is 0.2 seconds. [INFERRED] The cyclone rotates around the body for the active window.
- **targeting:** Self-only buff; no projectile aim or acquisition.
- **visuals:** Intended cast/loop/end: tight wind cyclone hugs the player's silhouette, with a gold/lavender shield read and no permanent class tint; A8 per-style stacks and rule 6 constrain the body-hugging composition. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=Server/Particles/Combat/Battleaxe/Signature/Battleaxe_Signature_Whirlwind.particlesystem; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado
- **cleanup:** Remove cyclone loop, shield cue, and defensive tint at 6 seconds; restore any terrain touched by `cyclone_shield`; do not leave a permanent trail.
- **locks:** Universal rules 6, 7, 8; A8; no G# lock.
- **current gap:** Current legacy row is a generic whirlwind and does not expose the tight protective cyclone or the distinct projectile/all-damage mitigation read.

### tempest — Tempest (gale_wizard)
- **author says:** "Place a storm field that pulls, slows, and repeatedly hits enemies caught inside." — style theme: "Wind magic, control, reflection"
- **data:** `{"id":"tempest","name":"Tempest","description":"Place a storm field that pulls, slows, and repeatedly hits enemies caught inside.","damage_percent":15,"cooldown_seconds":8,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"stun","categories":["damage","crowd_control"],"resource_cost":0,"cast_type":"ground_zone","target_type":"ground_target","range":14,"radius":6,"duration_seconds":4,"pull_force":3,"terrain_effect":"tempest"}`
- **behavior over time:** t0 places a ground-target storm field after 0.55 seconds at range 14. For 4 seconds and radius 6 it pulls with force 3, slows, and repeatedly hits enemies for 15% damage; the data effect is `stun`; recovery is 0.32 seconds. [INFERRED] Repeated hits occur during the active loop rather than as one impact.
- **targeting:** Ground-target field, range 14, radius 6; no projectile aim or auto-acquire.
- **visuals:** Intended cast/loop/impact: physically bounded storm field with visible wind pull links and repeated impact beats; A8 per-style wind stack, rule 5 tether visibility, and rule 7 composition apply. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=Server/Particles/Combat/Battleaxe/Signature/Battleaxe_Signature_Whirlwind.particlesystem; model=Common/NPC/Beast/Spark_Living/Models/Model.blockymodel; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado
- **cleanup:** Remove storm field, pull links, repeated-hit beats, and stun/slow visuals at 4 seconds; restore terrain affected by `tempest` and clear Spark_Living proxy state.
- **locks:** Universal rules 1, 5, 7, 8; A8; Spark_Living systemic misfit; no G# lock.
- **current gap:** Current legacy row includes Spark_Living and generic effects, not a radius-6 storm field with synchronized pull and repeated hits.

## Pressure — Air pressure, charged attacks

### air_shot — Air Shot (pressure)
- **author says:** "Fire a fast compressed-air projectile that travels 15 blocks." — style theme: "Air pressure, charged attacks"
- **data:** `{"id":"air_shot","name":"Air Shot","description":"Fire a fast compressed-air projectile that travels 15 blocks.","damage_percent":15,"cooldown_seconds":3,"cast_time_seconds":0.35,"recovery_seconds":0.24,"effect":"","categories":["damage"],"resource_cost":0,"cast_type":"projectile","target_type":"enemy","range":15,"projectile_speed":32,"travel_type":"compressed_air_shot"}`
- **behavior over time:** t0 launches the compressed-air projectile after 0.35 seconds. It travels visibly at speed 32 for up to 15 blocks and deals 15% damage; recovery is 0.24 seconds. [INFERRED] The shot ends at impact or its 15-block limit.
- **targeting:** Crosshair-aimed, dodgeable enemy projectile; live finding requires crosshair aim, not off-crosshair target snap.
- **visuals:** Intended cast/travel/impact: compact physical compression-ring projectile with gold/lavender air accents and visible travel; A1/A8 pressure family and U2 native projectile actors replace Spark_Living proxies. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado (Aero wind palette; [INFERRED] mapping for Air Shot)
- **cleanup:** Remove projectile and impact cue at hit/range; no lingering compression ring remains.
- **locks:** Universal rules 1, 2, 7, 8; A1/A8; live crosshair aim; no G# lock.
- **current gap:** Current legacy generic row lacks a compressed-air projectile silhouette and native projectile configuration.

### bullet_storm — Bullet Storm (pressure)
- **author says:** "Fire a rapid volley of compressed-air bullets that each travel 15 blocks." — style theme: "Air pressure, charged attacks"
- **data:** `{"id":"bullet_storm","name":"Bullet Storm","description":"Fire a rapid volley of compressed-air bullets that each travel 15 blocks.","damage_percent":12,"cooldown_seconds":5,"cast_time_seconds":0.32,"recovery_seconds":0.22,"effect":"slow","categories":["damage","debuff"],"resource_cost":0,"cast_type":"projectile_volley","target_type":"enemy","range":15,"width":4,"projectile_speed":34,"travel_type":"air_bullets"}`
- **behavior over time:** t0 starts a rapid volley after 0.32 seconds. Compressed-air bullets travel visibly at speed 34 to 15 blocks within width 4, deal 12% damage, and apply slow; recovery is 0.22 seconds. [INFERRED] Each bullet has its own travel and impact rather than an instant aggregate hit.
- **targeting:** Crosshair-aimed enemy projectile volley, range 15, width 4; each bullet is dodgeable under universal rule 2.
- **visuals:** Intended cast/travel/impact: multiple distinct compressed-air bullet silhouettes with visible spacing and gold/lavender accents, never a single generic particle flash; A1/A8 pressure family applies. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado (Aero wind palette; [INFERRED] mapping for Bullet Storm)
- **cleanup:** Remove every bullet trail and impact at its endpoint; clear slow cue when its status ends; no projectile remains after the volley.
- **locks:** Universal rules 1, 2, 7, 8; A1/A8; live crosshair aim; no G# lock.
- **current gap:** Current legacy row has no distinct compressed-air bullet volley and uses generic wind/Battleaxe cues.

### pressure_burst — Pressure Burst (pressure)
- **author says:** "Charge up to 4 seconds, then fire a larger fast compressed-air shot up to 20 blocks." — style theme: "Air pressure, charged attacks"
- **data:** `{"id":"pressure_burst","name":"Pressure Burst","description":"Charge up to 4 seconds, then fire a larger fast compressed-air shot up to 20 blocks.","damage_percent":20,"cooldown_seconds":7,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"knockback","categories":["damage","crowd_control"],"resource_cost":0,"cast_type":"projectile","target_type":"enemy","range":20,"duration_seconds":4,"projectile_speed":32,"knockback_force":5,"travel_type":"pressure_burst"}`
- **behavior over time:** t0 enters hold-charge after 0.28 seconds; the charge lasts up to 4 seconds and visibly grows. On release, a larger compressed-air shot travels at speed 32 to range 20, deals 20% damage, and knocks back with force 5; recovery is 0.2 seconds. A5 requires `Charging` interaction, `PressureChargeState`, and `EntityScaleComponent` growth on the projectile, never the player. [INFERRED] Releasing early fires the currently grown shot.
- **targeting:** Crosshair-aimed, dodgeable projectile released toward the crosshair; target type enemy, range 20. Charge changes projectile size, not player size.
- **visuals:** Intended cast/travel/impact: a clearly growing physical compression sphere/ring during hold, then a fast large compressed-air projectile with visible travel and wind-burst impact; A5 is proven representative and A1 pressure family supplies the stack. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Combat/Battleaxe/Bash/Spawners/Battleaxe_Bash_Shockwave.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** SFX_Tornado (Aero wind palette; [INFERRED] mapping for Pressure Burst)
- **cleanup:** Remove charge proxy and scale state on release/cancel; remove projectile and impact after hit/range; never leave the player scaled or a charge visual after cooldown.
- **locks:** A5 (4-second hold, `EntityScaleComponent`, scale projectile only); universal rules 1, 2, 7, 8; live crosshair aim; no G# lock.
- **current gap:** Current legacy row has no charge state, visible growth, or native compressed-air projectile; A5's proven R7 growth contract is absent from the current manifest row.

## Pollution — Toxic air, corrosion, debuffs

### smog — Smog (pollution)
- **author says:** "Create a lingering toxic cloud that damages, slows, and weakens enemies." — style theme: "Toxic air, corrosion, debuffs"
- **data:** `{"id":"smog","name":"Smog","description":"Create a lingering toxic cloud that damages, slows, and weakens enemies.","damage_percent":5,"cooldown_seconds":5,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"dot+slow","categories":["dot","debuff","aoe"],"resource_cost":0,"cast_type":"ground_zone","target_type":"ground_target","range":12,"radius":5,"duration_seconds":6,"terrain_effect":"smog_cloud"}`
- **behavior over time:** t0 creates a ground-target toxic cloud after 0.55 seconds at range 12. For 6 seconds in radius 5 it deals 5% damage over time, slows, and weakens enemies; recovery is 0.32 seconds. [INFERRED] The cloud remains at the ground target for the full duration.
- **targeting:** Ground-target area, range 12 and radius 5; no projectile or auto-acquire.
- **visuals:** Intended cast/loop/impact: persistent toxic-air cloud with physical density first and poison-family accents second; A8 deliberately permits pollution to borrow the poison family while rule 7 limits wrong-family assets. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Projectile/Acid/Spawners/Acid_Sparks.particlespawner; loop=Server/Particles/Combat/Mace/Signature/Spawners/Cast/Mace_Signature_Cast_End_Smoke.particlespawner; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** unassigned
- **cleanup:** Remove cloud, DoT/slow/weakening cues, smoke loop, and terrain effect at 6 seconds; restore terrain changed by `smog_cloud`; no lingering toxic tint remains.
- **locks:** Universal rules 1, 7, 8; A8 (pollution deliberately borrows poison family); no G# lock.
- **current gap:** Current legacy cast/travel are generic Aero wind, impact is Acid Sparks, and loop is Mace smoke; A8's intentional poison-family direction is not yet a coherent composition.

### toxic_breath — Toxic Breath (pollution)
- **author says:** "Exhale a 60-degree cone of toxic breath up to 8 blocks, applying poison and vulnerability for 5 seconds." — style theme: "Toxic air, corrosion, debuffs"
- **data:** `{"id":"toxic_breath","name":"Toxic Breath","description":"Exhale a 60-degree cone of toxic breath up to 8 blocks, applying poison and vulnerability for 5 seconds.","damage_percent":8,"cooldown_seconds":6,"cast_time_seconds":0.25,"recovery_seconds":0.18,"effect":"dot+vulnerability","categories":["dot","debuff"],"resource_cost":0,"cast_type":"cone","target_type":"cone","range":8,"cone_angle":60,"duration_seconds":5,"terrain_effect":"toxic_breath"}`
- **behavior over time:** t0 exhales a 60-degree cone after 0.25 seconds, reaching 8 blocks. Targets in it take 8% damage and receive poison plus vulnerability for 5 seconds; recovery is 0.18 seconds. [INFERRED] The breath is a short active cone pulse, with status effects persisting after the breath ends.
- **targeting:** Facing cone, range 8, cone angle 60; no projectile travel or auto-acquire. [INFERRED] Direction is fixed by facing at cast time.
- **visuals:** Intended cast/impact: physical toxic breath volume first, acid/poison accents second, with a clear cone boundary and no generic wind projectile; A8 pollution may deliberately borrow poison family. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Projectile/Acid/Spawners/Acid_Sparks.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** unassigned
- **cleanup:** Remove breath volume and impact particles after the cone pulse; clear poison/vulnerability cues at 5 seconds and restore terrain touched by `toxic_breath`.
- **locks:** Universal rules 1, 7, 8; A8 pollution poison-family direction; no G# lock.
- **current gap:** Current legacy cast/travel are generic wind and only the impact is Acid Sparks; there is no authored toxic-breath cone composition.

### acid_rain — Acid Rain (pollution)
- **author says:** "Call acidic green rain at the target area for 6 seconds, damaging and weakening enemies." — style theme: "Toxic air, corrosion, debuffs"
- **data:** `{"id":"acid_rain","name":"Acid Rain","description":"Call acidic green rain at the target area for 6 seconds, damaging and weakening enemies.","damage_percent":10,"cooldown_seconds":8,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"dot+vulnerability","categories":["dot","debuff","aoe"],"resource_cost":0,"cast_type":"ground_zone","target_type":"ground_target","range":14,"radius":5,"duration_seconds":6,"terrain_effect":"acid_rain"}`
- **behavior over time:** t0 calls rain at the ground target after 0.55 seconds, within range 14 and radius 5. For 6 seconds it deals 10% damage over time and weakens enemies through `dot+vulnerability`; recovery is 0.32 seconds. [INFERRED] The rain remains a bounded overhead/ground field for the full duration.
- **targeting:** Ground-target area, range 14, radius 5; no projectile or auto-acquire.
- **visuals:** Intended cast/loop/impact: physically readable acidic green rain over the target, with poison/acid family accents and a bounded six-second field; A8 allows pollution's deliberate poison-family borrow, while rule 1 requires the field read as more than particles alone. Current manifest row: `cast=Server/Particles/Combat/Battleaxe/Signature/Spawners/Battleaxe_Signature_Whirlwind_Spin.particlespawner; travel=Server/Particles/NPC/Spirit_Wind/Spawners/Wind_Sparks_Tail.particlespawner; impact=Server/Particles/Projectile/Acid/Spawners/Acid_Sparks.particlespawner; loop=Server/Particles/Deployables/Slowness_Totem/Totem_Slow_SmokeFlat_Constant.particlespawner; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** unassigned
- **cleanup:** Remove falling rain, acid impact, weakening/DoT visuals, and loop at 6 seconds; restore terrain affected by `acid_rain`; no green rain or smoke remains.
- **locks:** Universal rules 1, 7, 8; A8 pollution poison-family direction; no G# lock.
- **current gap:** Current legacy loop is a Slowness Totem smoke spawner and cast/travel are generic Aero, so the row does not yet show acidic green rain as a physical target-area field.

## Open questions for the author

The following behavior details are not grounded enough to enter an engine without a grill decision:

- `sonic_boom`: exact shockwave thickness/profile and whether the 12% hit is one front-wide hit or repeated along the line.
- `afterburner`: exact ember-trail block placement, collision cadence, and whether its 8-second duration starts at dash launch or after the dash completes.
- `eye_of_the_storm`: exact ally/enemy filtering and the authored description's slow/damage behavior versus the JSON `heal+shield` effect fields.
- `hang_time`: exact drift control, cancellation input precedence, and landing behavior.
- `tempest`: exact repeated-hit cadence and how the JSON `stun` effect coexists with the authored slow wording.
- `pollution` fields (`smog`, `toxic_breath`, `acid_rain`): exact weakening/vulnerability magnitude and field geometry beyond the recorded range/radius/cone values.
