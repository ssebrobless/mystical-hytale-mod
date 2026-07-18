# Corruptus Ability Contracts

Class fantasy: "Dark Magic + Fire" — corruption, souls, flame, void, and swarm; the class sacrifices essence for overwhelming destruction. Palette: primary `#8B0000` (dark red), secondary `#4B0082` (dark purple), accent `#FF4500` (orange-red); locked exceptions below override these defaults. Passive: **Soul Harvest** — "Hostile kills build up to 5 Soul Harvest stacks. Each stack fuels Infernal Aura, granting 2% increased damage and 1% damage reduction per stack. At 5 stacks, lethal damage heals you to half HP instead of killing you, clears the stacks, and starts a 10 minute lockout that prevents all Corruptus passive stack gain. Corruptus abilities use cooldowns instead of Souls."

## Applicable universal rules

1. **Physical object first, particles second:** physical lava/stone/terrain objects carry identity; particles accent them.
2. **Visible travel before impact:** projectiles and waves are watchable and dodgeable, never instant-hit flashes.
3. **Burst, not teleport:** dash movement has start burst, trail, and end cue over time; `burrow` is the sole disappearance exception.
4. **Summons visibly fight:** owner-attributed combat, not merely spawning, is the fantasy.
5. **Tethers are visible links:** a thin themed caster-to-target link stays synced with movement; `rip_current` is the approved fluid-trace exception.
6. **Coatings hug the body:** tight tint/model effect, no permanent class tint or permanent trail.
7. **Composition stack = identity:** silhouette, palette, distinct cast/travel/impact/loop motion, then silence; maximum one wrong-family asset per stack.
8. **Cleanup is part of the visual:** no drops, stuck visuals, lingering tints, or un-restored terrain.

## Flame — Fire attacks, burning DoT

### fireball — Fireball (flame)
- **author says:** "Explosive fire, applies burn" — style theme: "Fire attacks, burning DoT"
- **data:** `{"id":"fireball","name":"Fireball","description":"Explosive fire, applies burn","damage_percent":8,"cooldown_seconds":2,"cast_time_seconds":0.35,"recovery_seconds":0.24,"effect":"burn","categories":["dot"],"charges":3,"charge_recharge_seconds":2.0,"resource_cost":0,"cast_type":"projectile","target_type":"enemy","range":18,"radius":3,"projectile_speed":24,"travel_type":"explosive_fireball"}`
- **behavior over time:** t0 (0.35s cast) forms and releases a fireball -> active window is visible travel toward the aimed enemy, then a radius-3 explosive impact applies burn -> end is the finite impact/burn state and recovery (0.24s). [INFERRED] The burn duration is not authored in this row.
- **targeting:** Crosshair-aimed, dodgeable projectile: `cast_type=projectile`, `target_type=enemy`, range 18, speed 24; aim is crosshair rather than auto-acquire per the live finding and universal rules 2-3.
- **visuals:** Intended physical fireball silhouette first, orange-red fire palette with visible cast/travel/impact phases; the travel must be watchable. Current manifest row: `cast=Server/Particles/Combat/Fire_Stick/Spawners/Fire_Charge1_Fire.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Smoke.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Fire.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). The live status specifically found no travel visual and previously purple fire impact; C1/U2 require fire-family routing.
- **sound:** `SFX_Staff_Flame_Fireball_Launch` and impact variant, per P5; exact impact key unassigned.
- **cleanup:** Remove projectile and impact emitters at expiry; burn status and any tint must end at their authored/runtime expiry; no terrain effect is set.
- **locks:** universal rules 1, 2, 7, 8; C1 (fire routing); U2/live aim finding.
- **current gap:** Current manifest is legacy and uses an impact-smoke travel slot, while the live finding says fireball has no travel visual; the authored visible, dodgeable explosive-fireball contract is not met.

### ignite — Ignite (flame)
- **author says:** "Set self on fire, damage nearby enemies" — style theme: "Fire attacks, burning DoT"
- **data:** `{"id":"ignite","name":"Ignite","description":"Set self on fire, damage nearby enemies","damage_percent":12,"cooldown_seconds":4,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"self_burn+aoe","categories":["damage","self_damage","aoe"],"resource_cost":0,"cast_type":"self_burst","target_type":"self_centered","radius":4,"duration_seconds":4,"terrain_effect":"living_flame"}`
- **behavior over time:** t0 (0.28s) ignites the caster -> active window lasts 4s: caster remains the center of a radius-4 burst/field, takes the authored self-burn and damages nearby enemies -> end removes the 4-second burn/field and enters 0.2s recovery. [INFERRED] Tick cadence and exact self-damage split are not authored.
- **targeting:** Self-centered burst, not aimed at an enemy: `cast_type=self_burst`, `target_type=self_centered`, radius 4.
- **visuals:** Body-hugging fire coating plus a readable radius-4 living-flame physical/ground cue; current manifest: `cast=Server/Particles/Combat/Fire_Stick/Spawners/Fire_Charge1_Fire.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Fire.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). P1 names `Fire_AoE_Grow` + `Effect_Fire` for the trigger-moment Ignite visual; the VoidSmoke travel slot is a wrong-family misfit.
- **sound:** Corruptus burn palette: `SFX_Effect_Burn_World`; launch key unassigned.
- **cleanup:** At 4s remove living-flame terrain, self tint, fire loop and any AoE emitter; restore original terrain and player appearance.
- **locks:** universal rules 1, 6, 7, 8; P1 Ignite recipe; C1 fire routing; live finding defect #13 requires no lingering self residue.
- **current gap:** Legacy manifest routes travel through VoidSmoke and does not expose the authored radius field composition; lingering smoke/self-burst residue was a live defect (reported fixed, cleanup remains required).

### combust — Combust (flame)
- **author says:** "Consume burns for massive damage" — style theme: "Fire attacks, burning DoT"
- **data:** `{"id":"combust","name":"Combust","description":"Consume burns for massive damage","damage_percent":25,"cooldown_seconds":5,"cast_time_seconds":0.3,"recovery_seconds":0.22,"effect":"consume_burn","categories":["damage"],"resource_cost":0,"cast_type":"execute","target_type":"enemy","range":14}`
- **behavior over time:** t0 (0.3s) resolves an execute against one enemy within 14 -> active window is the immediate burn-consumption/massive-damage event -> end consumes eligible burn state and enters 0.22s recovery. [INFERRED] Whether damage scales with stacks or uses one fixed 25% hit is not authored.
- **targeting:** Enemy execute at range 14; no projectile, cone, or radius is authored, so it is not auto-acquire or self-centered.
- **visuals:** Fire-family cast and compact detonation/consumption impact; current manifest: `cast=Server/Particles/Combat/Fire_Stick/Spawners/Fire_Charge1_Fire.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Fire/Spawners/Impact_Fire.particlespawner; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). VoidSmoke travel is a wrong-family current proxy; C1 requires fire routing.
- **sound:** `SFX_Effect_Burn_World`; execute impact sound unassigned.
- **cleanup:** Remove the impact burst and consumed burn state immediately; no terrain effect is set.
- **locks:** universal rules 7, 8; C1 fire routing.
- **current gap:** Legacy manifest still has a VoidSmoke travel slot for a non-travel execute and does not record explicit burn-consumption feedback.

## Necro — Undead summons, life drain

### raise_dead — Raise Dead (necro)
- **author says:** "Summon one stronger undead ally that follows you and attacks hostile mobs." — style theme: "Undead summons, life drain"
- **data:** `{"id":"raise_dead","name":"Raise Dead","description":"Summon one stronger undead ally that follows you and attacks hostile mobs.","damage_percent":5,"cooldown_seconds":3,"cast_time_seconds":0.8,"recovery_seconds":0.4,"effect":"summon","categories":["summon","damage"],"resource_cost":0,"cast_type":"summon","target_type":"ground_target","range":10,"duration_seconds":20,"summon_name":"skeleton_minion","terrain_effect":"grave_rise"}`
- **behavior over time:** t0 (0.8s) selects a ground point within 10 and raises one ally -> active window lasts 20s while the Shadow_Knight follows the caster and attacks hostile mobs -> end despawns the ally and removes the grave-rise field. [INFERRED] Exact attack cadence and leash behavior are not authored.
- **targeting:** Ground-target summon within range 10, not auto-acquire; the summon point is selected by the caster.
- **visuals:** Bone/undead grave-rise physical cue followed by a visibly fighting Shadow_Knight. The assignment-author lock supersedes the stale skeleton wording in the current misfit/proposal: this is **Shadow_Knight, not a skeleton**. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE); the manifest lacks the required Shadow_Knight model row. C4/U3 summon-role work must preserve the author lock.
- **sound:** `SFX_Portal_Void` for raise cue; attack sound unassigned.
- **cleanup:** At 20s despawn Shadow_Knight, remove grave-rise blocks/field, particles, ownership links, and nameplate/collision proxy state.
- **locks:** universal rules 1, 4, 7, 8; author lock Shadow_Knight/not skeleton; C4 summon table (with this assignment's correction).
- **current gap:** Current legacy manifest has no model/role and the canon misfit table/proposal says skeleton; this contract requires the author-stated Shadow_Knight and a visibly fighting summon.

### life_drain — Life Drain (necro)
- **author says:** "Channel a dark tether for 4 seconds, damaging the target each second and healing for 50% of actual damage dealt." — style theme: "Undead summons, life drain"
- **data:** `{"id":"life_drain","name":"Life Drain","description":"Channel a dark tether for 4 seconds, damaging the target each second and healing for 50% of actual damage dealt.","damage_percent":10,"cooldown_seconds":4,"cast_time_seconds":0.45,"recovery_seconds":0.24,"effect":"lifesteal","categories":["damage","lifesteal"],"resource_cost":0,"cast_type":"channel","target_type":"enemy","range":14,"duration_seconds":4}`
- **behavior over time:** t0 (0.45s) locks one enemy within 14 and opens a dark tether -> active window lasts 4s, damaging that target each second and healing the caster for 50% of actual damage -> end closes the channel/tether and enters 0.24s recovery. [INFERRED] Interrupt rules are not authored.
- **targeting:** Enemy-targeted channel at range 14; the tether is the targeting/travel link, not a projectile.
- **visuals:** Dark tether with a visible caster-target link (C9 calls for `Spectre_Void_Body` drain language and endpoint effects until R6 chain); current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). C9's tether endpoint is missing from this legacy row.
- **sound:** `SFX_Portal_Void`; channel loop sound unassigned.
- **cleanup:** Close the tether and endpoint particles at 4s or interruption; remove drain tint/status and any healing-channel registration.
- **locks:** universal rules 5, 7, 8; C9 necro grammar; R6 tether chain gate.
- **current gap:** Legacy manifest has generic VoidSmoke/VoidImpact and no visible caster-target tether, while the authored channel requires four seconds of linked damage/healing.

### death_mark — Death Mark (necro)
- **author says:** "Mark one enemy for 8 seconds, making them take 20% more damage; death triggers a small enemy-only dark explosion." — style theme: "Undead summons, life drain"
- **data:** `{"id":"death_mark","name":"Death Mark","description":"Mark one enemy for 8 seconds, making them take 20% more damage; death triggers a small enemy-only dark explosion.","damage_percent":0,"cooldown_seconds":5,"cast_time_seconds":0.38,"recovery_seconds":0.22,"effect":"vulnerability","categories":["debuff"],"resource_cost":0,"cast_type":"curse","target_type":"enemy","range":16,"duration_seconds":8}`
- **behavior over time:** t0 (0.38s) curses one enemy within 16 -> active window is an 8s mark causing 20% more damage; if the marked enemy dies, an enemy-only dark explosion triggers -> end at expiry or death removes the mark and enters 0.22s recovery.
- **targeting:** Enemy-targeted curse at range 16; no projectile travel or auto-acquire is authored.
- **visuals:** A restrained void/death mark silhouette, then a small dark enemy-only death impact; current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). C9 names `Spectre_Void_Body` for curse language.
- **sound:** `SFX_Effect_Burn_World` for death/debuff punctuation; mark sound unassigned.
- **cleanup:** Remove the mark, vulnerability, target tint, and any pending death listener at 8s or target death; explosion must not affect friendlies or caster.
- **locks:** universal rules 7, 8; C9 necro grammar; enemy-only death explosion from author text.
- **current gap:** Current legacy row is generic void and does not distinguish the 8s persistent mark from the conditional enemy-only death explosion.

## Shadow — Stealth, clones, evasion

### shadow_step — Shadow Step (shadow)
- **author says:** "Blink forward a fixed safe distance, leaving dark smoke at the start and end points." — style theme: "Stealth, clones, evasion"
- **data:** `{"id":"shadow_step","name":"Shadow Step","description":"Blink forward a fixed safe distance, leaving dark smoke at the start and end points.","damage_percent":0,"cooldown_seconds":3,"cast_time_seconds":0.2,"recovery_seconds":0.16,"effect":"stealth","categories":["damage","dash","stealth"],"charges":2,"charge_recharge_seconds":3.0,"resource_cost":0,"cast_type":"teleport","target_type":"self","range":12,"dash_distance":12,"duration_seconds":4,"summon_name":"shadow_clone","travel_type":"shadow_step"}`
- **behavior over time:** t0 (0.2s) emits a start burst -> active window moves the caster a fixed safe 12-block distance with start/end smoke and a 4s shadow-clone/stealth window -> end resolves the clone/stealth and enters 0.16s recovery; two charges recharge every 3s. [INFERRED] Exact collision/path resolution is not authored.
- **targeting:** Self-targeted fixed dash/teleport, range and dash distance 12; no enemy auto-acquire. Universal rule 3 still requires burst/trail/end readability rather than an instant teleport read.
- **visuals:** G11 locked interim: dark-tinted Mannequin silhouette wreathed in void smoke; R11 may upgrade to a player-model clone. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=null; model=Common/NPC/Undead/Shadow_Knight/Models/Model.blockymodel; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). Shadow_Knight is the manifest misfit; the clone is not an undead knight.
- **sound:** `SFX_Portal_Void`.
- **cleanup:** Remove start/end smoke, clone, stealth tint and proxy nameplate/collision after the 4s window; restore the player model and all charge state cleanly.
- **locks:** universal rules 3, 7, 8; G11/R11 shadow clone; C11 interim recipe.
- **current gap:** Legacy manifest uses a Shadow_Knight model instead of the locked dark Mannequin + void-smoke interim and has no player-clone upgrade path.

### umbral_veil — Umbral Veil (shadow)
- **author says:** "Wrap yourself in black-purple wisps, becoming hard to target and reducing incoming damage until you attack." — style theme: "Stealth, clones, evasion"
- **data:** `{"id":"umbral_veil","name":"Umbral Veil","description":"Wrap yourself in black-purple wisps, becoming hard to target and reducing incoming damage until you attack.","damage_percent":0,"cooldown_seconds":5,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"stealth","categories":["stealth"],"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":5,"terrain_effect":"umbral_shroud"}`
- **behavior over time:** t0 (0.28s) wraps the caster in veil wisps -> active window lasts up to 5s, making the caster hard to target and reducing incoming damage until the caster attacks -> end is attack break or 5s expiry, then 0.2s recovery. [INFERRED] The exact reduction and targetability algorithm are not authored.
- **targeting:** Self buff, no aim or enemy acquisition.
- **visuals:** Tight black-purple body-hugging wisps; `umbral_shroud` is a temporary shroud, not a permanent class tint. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** `SFX_Portal_Void`.
- **cleanup:** On attack or expiry remove shroud terrain/effect, wisps, tint and targeting modifier; no visual may linger.
- **locks:** universal rules 6, 7, 8; G5 body-coating grammar by analogy.
- **current gap:** Legacy row uses generic cast/travel/impact and an unbounded-looking loop slot; it does not prove the attack-break cleanup or body-hugging black-purple read.

### dark_embrace — Dark Embrace (shadow)
- **author says:** "Wrap yourself or a targeted ally/summon in dark protection, granting damage reduction and small healing over time." — style theme: "Stealth, clones, evasion"
- **data:** `{"id":"dark_embrace","name":"Dark Embrace","description":"Wrap yourself or a targeted ally/summon in dark protection, granting damage reduction and small healing over time.","damage_percent":0,"cooldown_seconds":6,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"defense_buff+heal","categories":["damage","evasion","aoe"],"resource_cost":0,"cast_type":"self_buff","target_type":"self","range":10,"duration_seconds":6,"terrain_effect":"shadow_zone"}`
- **behavior over time:** t0 (0.55s) applies dark protection to self or the selected allied target within 10 -> active window lasts 6s with damage reduction and small healing over time -> end removes protection/shadow zone and enters 0.32s recovery. [INFERRED] Despite `target_type=self`, the description's ally/summon targeting needs an explicit runtime interpretation.
- **targeting:** Data says self buff/self target with range 10, while author text says self or targeted ally/summon; this is an unresolved targeting contradiction, not auto-acquire.
- **visuals:** Body-hugging dark protection on the recipient, with a bounded shadow-zone field; current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** `SFX_Portal_Void`.
- **cleanup:** At 6s or interruption remove recipient tint, healing/defense state, shadow zone and all particles; restore any terrain changed by `shadow_zone`.
- **locks:** universal rules 6, 7, 8; open targeting contradiction.
- **current gap:** Legacy generic-void row does not implement the described ally/summon option and has no explicit bounded protection/HoT feedback.

## Hell Flame — Blue soul-fire, hellfire ground, and burning debuffs

### hellfire — Hellfire (hell_flame)
- **author says:** "Breathe blue hellfire in a cone in front of you, burning and slowing enemies." — style theme: "Blue soul-fire, hellfire ground, and burning debuffs"
- **data:** `{"id":"hellfire","name":"Hellfire","description":"Breathe blue hellfire in a cone in front of you, burning and slowing enemies.","damage_percent":8,"cooldown_seconds":4,"cast_time_seconds":0.32,"recovery_seconds":0.22,"effect":"burn+slow","categories":["dot","debuff"],"resource_cost":0,"cast_type":"cone","target_type":"cone","range":9,"cone_angle":70,"duration_seconds":5,"terrain_effect":"blue_hellfire_breath"}`
- **behavior over time:** t0 (0.32s) charges blue fire at the caster's facing -> active window breathes a 70-degree cone to range 9 for 5s, burning and slowing enemies -> end removes blue breath/ground residue and enters 0.22s recovery.
- **targeting:** Facing cone, not crosshair projectile: `cast_type=cone`, `target_type=cone`, range 9, cone angle 70.
- **visuals:** G4 locked custom BLUE fire: shipped Phase 1 `MOTM_Corruptus_HellFlame_Cast`, `MOTM_Corruptus_HellFlame_Loop`, `MOTM_Corruptus_HellFlame_Impact`; interim may use native `Fire_Center_Blue`, `Fire_Blue`, `Fire_Blue_Smoke`, with custom projectile-shaped blue fire after R8. Current manifest: `cast=MOTM_Corruptus_HellFlame_Cast; travel=null; impact=MOTM_Corruptus_HellFlame_Impact; loop=MOTM_Corruptus_HellFlame_Loop; model=Common/NPC/Elemental/Golem_Firesteel/Models/Model.blockymodel; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). The Firesteel model is a current wrong-family model for a cone breath.
- **sound:** `SFX_Staff_Flame_Fireball_Launch` + `SFX_Effect_Burn_World`; exact cone impact unassigned.
- **cleanup:** Remove blue-fire breath, slow/burn statuses, terrain `blue_hellfire_breath`, and all cast/loop/impact emitters at 5s.
- **locks:** universal rules 1, 7, 8; G4 blue-fire lock; C1/C5; shipped `MOTM_Corruptus_HellFlame_*` trio.
- **current gap:** The shipped custom-blue trio is present, but the manifest is legacy, has no travel slot, and still carries an unrelated Firesteel model; cone-range/dodgeable composition and cleanup must be verified.

### infernal_ground — Infernal Ground (hell_flame)
- **author says:** "Set blue infernal ground ablaze under the caster; enemies burn, friendlies are safe." — style theme: "Blue soul-fire, hellfire ground, and burning debuffs"
- **data:** `{"id":"infernal_ground","name":"Infernal Ground","description":"Set blue infernal ground ablaze under the caster; enemies burn, friendlies are safe.","damage_percent":5,"cooldown_seconds":7,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"burn+attack_buff","categories":["dot","buff","aoe"],"resource_cost":0,"cast_type":"ground_zone","target_type":"self_centered","range":0,"radius":5,"duration_seconds":7,"terrain_effect":"infernal_ground"}`
- **behavior over time:** t0 (0.55s) ignites the ground beneath the caster -> active window lasts 7s as a radius-5 blue zone: enemies burn while friendlies are safe and the caster receives the authored attack-buff effect -> end restores the ground and removes every zone/status visual.
- **targeting:** Self-centered ground zone, radius 5, range 0; no enemy auto-acquire.
- **visuals:** G4 custom blue-fire ground composition; physical ground/terrain is primary and `Fire_Blue`/`Fire_Center_Blue`/`Fire_Blue_Smoke` accent it. Current manifest: `cast=MOTM_Corruptus_HellFlame_Cast; travel=null; impact=MOTM_Corruptus_HellFlame_Impact; loop=MOTM_Corruptus_HellFlame_Loop; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). C5 prescribes Fire_Center_Blue cast/core, Fire_Blue travel/loop/ground, Fire_Blue_Smoke impact/expiry.
- **sound:** `SFX_Effect_Burn_World`.
- **cleanup:** Restore all terrain changed by `infernal_ground` at 7s; remove blue loop/smoke, burn, attack buff, and friendly-safe zone registration.
- **locks:** universal rules 1, 7, 8; G4; C5; class's blue hellfire canon.
- **current gap:** Current legacy row has the shipped trio but not the authored physical radius-5 ground composition or explicit terrain restore.

### soul_scorch — Soul Scorch (hell_flame)
- **author says:** "Wrap one target in blue soul-fire, dealing damage over time and increasing damage taken." — style theme: "Blue soul-fire, hellfire ground, and burning debuffs"
- **data:** `{"id":"soul_scorch","name":"Soul Scorch","description":"Wrap one target in blue soul-fire, dealing damage over time and increasing damage taken.","damage_percent":8,"cooldown_seconds":8,"cast_time_seconds":0.38,"recovery_seconds":0.22,"effect":"dot+vulnerability","categories":["debuff"],"resource_cost":0,"cast_type":"curse","target_type":"enemy","range":16,"duration_seconds":6}`
- **behavior over time:** t0 (0.38s) curses one enemy within 16 -> active window lasts 6s with blue soul-fire wrapped around the target, damage over time, and increased damage taken -> end removes the wrap/status and enters 0.22s recovery.
- **targeting:** Enemy-targeted curse at range 16, not a projectile; no auto-acquire is authored.
- **visuals:** G4 blue-fire family wrapped tightly around target; use `Fire_Center_Blue` cast/core, `Fire_Blue` loop, and `Fire_Blue_Smoke` impact/expiry. Current manifest: `cast=MOTM_Corruptus_HellFlame_Cast; travel=null; impact=MOTM_Corruptus_HellFlame_Impact; loop=MOTM_Corruptus_HellFlame_Loop; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). C1 explicitly prevents this id falling to void routing.
- **sound:** `SFX_Effect_Burn_World`; curse cast sound unassigned.
- **cleanup:** At 6s remove target blue-fire coating, DoT/vulnerability, smoke and any target tint; no terrain effect is authored.
- **locks:** universal rules 6, 7, 8; G4; C1/C5.
- **current gap:** Current legacy row has blue MOTM slots but no explicit target-hugging six-second coating and is vulnerable to the documented old fire-routing misfit.

## Mentokinesis — Mind control, psychic damage, stuns

### dominate — Dominate (mentokinesis)
- **author says:** "Dominate a single enemy permanently until toggled off or the target dies; dominated targets become friendly allies." — style theme: "Mind control, psychic damage, stuns"
- **data:** `{"id":"dominate","name":"Dominate","description":"Dominate a single enemy permanently until toggled off or the target dies; dominated targets become friendly allies.","damage_percent":0,"cooldown_seconds":5,"cast_time_seconds":0.38,"recovery_seconds":0.22,"effect":"root+disoriented","categories":["crowd_control","debuff"],"resource_cost":0,"cast_type":"gaze","target_type":"enemy","range":15,"duration_seconds":999}`
- **behavior over time:** t0 (0.38s) acquires one enemy in the caster's gaze within 15 -> active control window is approximately 15-20s per G2, during which the target is a friendly controlled ally; recast releases it early, and expiry releases it -> end restores NORMAL AI with no auto-aggro and no death, then starts cooldown on release/expiry. [INFERRED] The authored 999s duration is superseded for control timing by G2.
- **targeting:** Gaze/facing target at range 15; single enemy, not projectile or ground-target.
- **visuals:** G1 interim bright-pink outline read: faint pink tint plus pink shimmer hugging the target silhouette while the mob keeps its own colors; cast/impact psychic stack uses pink `Eye_Void_Smoke`/`MagicBlast`, travel `Flying_Orb`; R8 halo is the upgrade. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** `SFX_Portal_Void`; control-acquired/released sound unassigned.
- **cleanup:** On recast, expiry, death, disconnect, or owner loss remove pink marker, control override, nameplate/collision proxy state, and all ownership links; target resumes NORMAL AI.
- **locks:** G1 marker; G2 15-20s/recast-release/NORMAL-AI; C2/C7 controlled ally; universal rules 7-8.
- **current gap:** Legacy generic-void row implements neither a controlled ally nor pink marker; its authored permanent/999 duration is superseded by the locked finite control clock.

### mind_shatter — Mind Shatter (mentokinesis)
- **author says:** "Create a bright pink 6-block psychic explosion centered on your dominated ally if present, otherwise on you." — style theme: "Mind control, psychic damage, stuns"
- **data:** `{"id":"mind_shatter","name":"Mind Shatter","description":"Create a bright pink 6-block psychic explosion centered on your dominated ally if present, otherwise on you.","damage_percent":25,"cooldown_seconds":6,"cast_time_seconds":0.35,"recovery_seconds":0.24,"effect":"stun","categories":["damage","crowd_control"],"resource_cost":0,"cast_type":"self_burst","target_type":"self_centered","radius":6,"terrain_effect":"psychic_shatter"}`
- **behavior over time:** t0 (0.35s) resolves the center on the controlled ally if present, otherwise the caster -> active window is an immediate bright-pink radius-6 psychic explosion dealing 25% damage and stun -> end clears the burst/terrain marker and enters 0.24s recovery.
- **targeting:** Self-centered burst with special `resolveCenter`: dominated ally first, caster fallback; radius 6, no crosshair projectile.
- **visuals:** Bright pink psychic silhouette/impact: `MagicBlast` cast/impact, `Flying_Orb` where a travel cue is needed, faint pink tint/shimmer consistent with G1/C7; current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** `SFX_Portal_Void`; psychic impact sound unassigned.
- **cleanup:** Remove psychic shatter terrain, pink burst/tints, and stun marker at effect end; do not leave a marker on an uncontrolled target.
- **locks:** G1/C7 pink psychic language; C2 `resolveCenter`; universal rules 7-8.
- **current gap:** Legacy generic-void visuals are not bright pink psychic language and do not prove controlled-ally-center resolution.

### hivemind — Hivemind (mentokinesis)
- **author says:** "Control all enemies within 7 blocks for 6 seconds; Mind Shatter can detonate each controlled target." — style theme: "Mind control, psychic damage, stuns"
- **data:** `{"id":"hivemind","name":"Hivemind","description":"Control all enemies within 7 blocks for 6 seconds; Mind Shatter can detonate each controlled target.","damage_percent":0,"cooldown_seconds":12,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"root+disoriented","categories":["buff"],"resource_cost":0,"cast_type":"self_burst","target_type":"self_centered","radius":7,"duration_seconds":6,"terrain_effect":"psychic_link"}`
- **behavior over time:** t0 (0.28s) bursts around the caster -> active window controls all eligible enemies in radius 7 using the same approximately 15-20s G2 control clock (hivemind radius/multi variant); Mind Shatter may detonate each controlled target -> end/recast releases every target, restores NORMAL AI, and starts cooldown on release/expiry. [INFERRED] Exact simultaneous target cap is not authored.
- **targeting:** Self-centered radius-7 multi-target control; no aim or auto-acquire beyond eligible enemies inside the authored radius.
- **visuals:** Pink control marker on every controlled target (G1), linked psychic shimmer for `psychic_link`, and C7 pink `Eye_Void_Smoke`/`MagicBlast`/`Flying_Orb`; current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** `SFX_Portal_Void`; control pulse sound unassigned.
- **cleanup:** Release all controlled targets together or individually, remove pink markers/psychic links and proxy state, and restore NORMAL AI; restore any `psychic_link` terrain/effect.
- **locks:** G1/G2; C2/C7; universal rules 5, 7, 8.
- **current gap:** Legacy generic void has no multi-control, pink markers, linked radius-7 field, or G2 release clock; data's 6s stun-era duration is superseded for control.

## Imbuement — Self-buffs, enhancement magic

### imbue_power — Imbue: Power (imbuement)
- **author says:** "Self-only dark red imbuement that boosts damage; replaces other imbuements." — style theme: "Self-buffs, enhancement magic"
- **data:** `{"id":"imbue_power","name":"Imbue: Power","description":"Self-only dark red imbuement that boosts damage; replaces other imbuements.","damage_percent":0,"cooldown_seconds":6,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"attack_buff","categories":["buff"],"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":8}`
- **behavior over time:** t0 (0.28s) switches the caster into Power stance -> active window lasts 8s with the damage-boosting self buff and full-body dark-red aura; it replaces any other imbuement -> end removes aura/buff at expiry or visible stance swap and enters 0.2s recovery.
- **targeting:** Exclusive self-only stance; no enemy, ally, or ground targeting.
- **visuals:** G13 correction: full-body aura in authored dark red `#8B0000`, exactly one active, visible one-shot flash on swap; C8 uses Aura_Sphere + Effect_Fire/Effect_Crown_Gold accents and Overwrite/single source slot. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** `SFX_Portal_Void`; stance-swap sound unassigned.
- **cleanup:** Remove previous stance aura before applying the new one; on expiry remove buff, body aura, flash residue and runtime source slot.
- **locks:** G13 (stance, dark red, exclusive, full-body, visible swap); C8; universal rules 6-8.
- **current gap:** Legacy generic-void visuals do not show the authored dark-red full-body stance or exclusive overwrite/swap flash.

### imbue_fortitude — Imbue: Fortitude (imbuement)
- **author says:** "Self-only dark green imbuement that boosts damage reduction; replaces other imbuements." — style theme: "Self-buffs, enhancement magic"
- **data:** `{"id":"imbue_fortitude","name":"Imbue: Fortitude","description":"Self-only dark green imbuement that boosts damage reduction; replaces other imbuements.","damage_percent":0,"cooldown_seconds":6,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"defense_buff+heal","categories":["buff","healing"],"heal_percent":10,"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":8,"terrain_effect":"abyssal_armor"}`
- **behavior over time:** t0 (0.28s) switches the caster to Fortitude -> active window lasts 8s with damage reduction and the authored 10% heal effect, full-body dark-green aura, and `abyssal_armor` state -> end removes stance/buff and restores terrain/effect at expiry or swap.
- **targeting:** Exclusive self-only stance.
- **visuals:** G13 authored dark green full-body aura, not a weapon coating; C8 single overwrite source with Aura_Sphere and dark-green body particles. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** `SFX_Portal_Void`; stance swap unassigned.
- **cleanup:** Remove old stance before switch, then remove dark-green aura, healing/defense effect, swap flash, and restore `abyssal_armor` terrain/effect at end.
- **locks:** G13 (dark green, exclusive self stance, full-body, visible swap); C8; universal rules 6-8.
- **current gap:** Legacy generic-void row lacks dark-green aura, authored 10% heal semantics, exclusive switching, and terrain restoration.

### imbue_swiftness — Imbue: Swiftness (imbuement)
- **author says:** "Self-only bright yellow imbuement that boosts movement and attack speed; replaces other imbuements." — style theme: "Self-buffs, enhancement magic"
- **data:** `{"id":"imbue_swiftness","name":"Imbue: Swiftness","description":"Self-only bright yellow imbuement that boosts movement and attack speed; replaces other imbuements.","damage_percent":0,"cooldown_seconds":6,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"evasion+attack_buff","categories":["buff","evasion"],"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":8,"dash_distance":9,"travel_type":"shadow_haste"}`
- **behavior over time:** t0 (0.28s) switches the caster to Swiftness -> active window lasts 8s with movement/attack-speed boost, evasion, full-body bright-yellow aura, and `shadow_haste` movement state -> end removes stance and aura at expiry or swap, then enters 0.2s recovery. [INFERRED] The purpose of authored `dash_distance` 9 is not further specified.
- **targeting:** Exclusive self-only stance; no aim or enemy targeting.
- **visuals:** G13 authored bright yellow full-body aura; `shadow_haste` must not turn into a permanent trail. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** `SFX_Portal_Void`; stance swap unassigned.
- **cleanup:** Remove prior stance, yellow aura, loop/trail residue and speed/evasion effects at swap or 8s; clear the runtime source slot.
- **locks:** G13 (bright yellow, exclusive, full-body, visible swap); C8; universal rules 6-8.
- **current gap:** Legacy generic-void row has no yellow body aura and its loop may imply a permanent trail, contrary to G13/universal cleanup.

## Atonement — Purification, healing, cleansing

### sanctuary — Sanctuary (attonement)
- **author says:** "Place a holy circle that heals and protects friendlies while lightly weakening enemies." — style theme: "Purification, healing, cleansing"
- **data:** `{"id":"sanctuary","name":"Sanctuary","description":"Place a holy circle that heals and protects friendlies while lightly weakening enemies.","damage_percent":0,"cooldown_seconds":7,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"heal","categories":["healing"],"heal_percent":15,"resource_cost":0,"cast_type":"support_zone","target_type":"ground_target","range":12,"radius":5,"duration_seconds":6,"terrain_effect":"sanctuary"}`
- **behavior over time:** t0 (0.55s) places a ground circle within 12 -> active window lasts 6s as a radius-5 zone healing/protecting friendlies and lightly weakening enemies -> end removes the circle and enters 0.32s recovery.
- **targeting:** Ground-target support zone, range 12, radius 5; not self-centered and not auto-acquire.
- **visuals:** G10 corrupted-holy palette: white primary `#FFFFFF`, gold glows `#FFD700`, dark-purple accents `#4B0082`; Totem_Heal_AoE/BeamStart/GlowStart plus temporary `Build_Lightsource_White`. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=Server/Particles/Deployables/Healing_Totem/Totem_Heal_Sparks_Constant.particlespawner; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** `SFX_Portal_Void`; healing-zone sound unassigned.
- **cleanup:** At 6s remove circle, white lightsource, beams/glows, healing/protection/weakening statuses and restore any `sanctuary` terrain.
- **locks:** G10 palette and unchanged names; C6; universal rules 1, 7, 8.
- **current gap:** Current legacy row has a healing-totem loop but generic void cast/impact and no explicit white/gold/purple corrupted-holy composition.

### absorb — Absorb (attonement)
- **author says:** "Create a caster-only white-gold shield that absorbs damage and converts part of it into healing." — style theme: "Purification, healing, cleansing"
- **data:** `{"id":"absorb","name":"Absorb","description":"Create a caster-only white-gold shield that absorbs damage and converts part of it into healing.","damage_percent":0,"cooldown_seconds":6,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"defense_buff+heal","categories":["buff","healing"],"heal_percent":10,"resource_cost":0,"cast_type":"self_buff","target_type":"self","duration_seconds":5}`
- **behavior over time:** t0 (0.28s) creates a caster-only shield -> active window lasts 5s or until its absorption is spent, converting part of absorbed damage into healing -> end removes shield and conversion state, then enters 0.2s recovery. [INFERRED] Shield capacity and conversion ratio beyond `heal_percent=10` are not authored.
- **targeting:** Self-only buff; no aim, ally, or ground targeting.
- **visuals:** G10 white primary, gold glows, dark-purple accents; a tight shield coating, not a permanent trail. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). C6 names per-ability `MOTM_Corruptus_Atonement_*` stacks.
- **sound:** `SFX_Portal_Void`; shield break sound unassigned.
- **cleanup:** Remove white-gold shield, dark-purple undertone, conversion/heal effect and any tint on expiry or break.
- **locks:** G10; C6; universal rules 6-8.
- **current gap:** Legacy generic-void row has no caster-only white-gold shield, absorption break, or G10 palette.

### purify — Purify (attonement)
- **author says:** "Cleanse negative effects from friendlies in a radius and burst white-gold damage into enemies." — style theme: "Purification, healing, cleansing"
- **data:** `{"id":"purify","name":"Purify","description":"Cleanse negative effects from friendlies in a radius and burst white-gold damage into enemies.","damage_percent":0,"cooldown_seconds":8,"cast_time_seconds":0.38,"recovery_seconds":0.22,"effect":"burn+vulnerability","categories":["buff"],"resource_cost":0,"cast_type":"self_burst","target_type":"self_centered","radius":6,"duration_seconds":6,"terrain_effect":"purifying_aura"}`
- **behavior over time:** t0 (0.38s) bursts from the caster -> active window resolves a radius-6 purifying aura: cleanses friendly negative effects and bursts white-gold damage into enemies -> end at 6s removes aura/terrain and enters 0.22s recovery. [INFERRED] The row's `effect` labels burn+vulnerability while the description says cleanse/damage; exact status mapping is unresolved.
- **targeting:** Self-centered radius-6 burst/aura; friendlies and enemies are filtered by faction inside the radius.
- **visuals:** G10 white primary + gold glows + dark-purple accents; `Totem_Heal_AoE/BeamStart/GlowStart` and `Build_Lightsource_White` are the C6 visual recipe. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** `SFX_Portal_Void`; cleansing sound unassigned.
- **cleanup:** Remove purifying aura, white/gold/purple burst particles, cleansed-effect registration and terrain at 6s; no lingering enemy tint.
- **locks:** G10; C6; universal rules 7-8; data/description status mismatch is open.
- **current gap:** Legacy row is generic void and does not show the corrupted-holy palette, faction split, or cleanse-versus-enemy burst.

## Void — Void magic, summoning, destruction

### rift — Rift (void)
- **author says:** "Open a black-purple void tear that pulls and slows enemies." — style theme: "Void magic, summoning, destruction"
- **data:** `{"id":"rift","name":"Rift","description":"Open a black-purple void tear that pulls and slows enemies.","damage_percent":8,"cooldown_seconds":6,"cast_time_seconds":0.55,"recovery_seconds":0.32,"effect":"dot+slow","categories":["dot","aoe"],"resource_cost":0,"cast_type":"ground_zone","target_type":"ground_target","range":14,"radius":5,"duration_seconds":6,"pull_force":4,"terrain_effect":"void_rift"}`
- **behavior over time:** t0 (0.55s) opens a ground-target tear within 14 -> active window lasts 6s as a radius-5 rift that damages, slows, and pulls enemies with force 4 -> end closes the tear and restores terrain, then enters 0.32s recovery.
- **targeting:** Ground-target zone at range 14, radius 5; not self-centered and no auto-acquire.
- **visuals:** Physical black-purple void tear first, Eye_Void silhouette where appropriate, with particles as accents; current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=Server/Particles/Combat/Impact/Misc/Void/VoidSplash.particlespawner; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). The loop is a current generic splash proxy for the ground tear.
- **sound:** `SFX_Portal_Void`.
- **cleanup:** Remove rift object, pull/slow/DoT states and all particles at 6s; restore terrain changed by `void_rift`.
- **locks:** universal rules 1, 7, 8; C1 void-family routing.
- **current gap:** Legacy row has generic void slots and an Eye_Void model but no proven physical radius-5 pull field or terrain restoration.

### void_spawn — Void Spawn (void)
- **author says:** "Summon three friendly Crawler_Void allies in a triangle around you for 10 seconds." — style theme: "Void magic, summoning, destruction"
- **data:** `{"id":"void_spawn","name":"Void Spawn","description":"Summon three friendly Crawler_Void allies in a triangle around you for 10 seconds.","damage_percent":0,"cooldown_seconds":8,"cast_time_seconds":0.8,"recovery_seconds":0.4,"effect":"summon","categories":["summon"],"resource_cost":0,"cast_type":"summon","target_type":"self_centered","range":0,"duration_seconds":10,"summon_name":"crawler_void","terrain_effect":"void_gate"}`
- **behavior over time:** t0 (0.8s) opens a void gate around the caster -> active window lasts 10s while exactly three friendly Crawler_Void allies fight in a triangle around the caster -> end despawns all three and removes the gate, then enters 0.4s recovery. [INFERRED] Triangle spacing and attack cadence are not authored.
- **targeting:** Self-centered summon, range 0; no enemy target.
- **visuals:** Physical three-ally silhouette first, void gate second; C4/U3 require Crawler_Void x3. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=null; model=Common/NPC/Void/Eye_Void/Models/Model.blockymodel; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). Manifest Eye_Void is the documented model drift; C4 says Crawler_Void x3.
- **sound:** `SFX_Portal_Void`.
- **cleanup:** Despawn exactly three allies at 10s, remove void gate, ownership links, nameplates/collision proxies and all particles.
- **locks:** universal rules 1, 4, 7, 8; C4/U3 Crawler_Void x3; documented void_spawn model drift.
- **current gap:** Legacy row points to Eye_Void and generic void effects, not three visibly fighting Crawler_Void allies.

### consume — Consume (void)
- **author says:** "Consume a target with void damage, healing yourself; targets below 10% health are executed." — style theme: "Void magic, summoning, destruction"
- **data:** `{"id":"consume","name":"Consume","description":"Consume a target with void damage, healing yourself; targets below 10% health are executed.","damage_percent":30,"cooldown_seconds":9,"cast_time_seconds":0.3,"recovery_seconds":0.22,"effect":"lifesteal","categories":["damage","crowd_control"],"resource_cost":0,"cast_type":"execute","target_type":"enemy","range":12,"duration_seconds":2.5}`
- **behavior over time:** t0 (0.3s) resolves one enemy within 12 -> active window is the 2.5s consume/void-damage/heal state, with targets below 10% health executed -> end removes the consume state and enters 0.22s recovery. [INFERRED] Whether the target remains immobilized during 2.5s is not authored despite the crowd-control category.
- **targeting:** Enemy execute at range 12; not projectile, cone, or auto-acquire.
- **visuals:** Void silhouette-first execution impact, with a finite dark pull/impact; current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** `SFX_Portal_Void`; execution sound unassigned.
- **cleanup:** Remove consume tether/impact, target control and caster healing registration at 2.5s or execution; no terrain effect is authored.
- **locks:** universal rules 7, 8; C1 void routing.
- **current gap:** Legacy generic void does not distinguish 30% lifesteal, below-10% execute, or the 2.5s active window.

## Scarak — Insect swarms, summoning, overwhelming numbers

### scarak_egg — Scarak Egg (scarak)
- **author says:** "Place Deco_Scarak_Eggsacks; after 4 seconds it hatches into friendly Scarak Seeker and Fighter summons." — style theme: "Insect swarms, summoning, overwhelming numbers"
- **data:** `{"id":"scarak_egg","name":"Scarak Egg","description":"Place Deco_Scarak_Eggsacks; after 4 seconds it hatches into friendly Scarak Seeker and Fighter summons.","damage_percent":0,"cooldown_seconds":8,"cast_time_seconds":0.8,"recovery_seconds":0.4,"effect":"summon","categories":["summon"],"resource_cost":0,"cast_type":"summon","target_type":"ground_target","range":8,"duration_seconds":30,"summon_name":"scarak_egg","terrain_effect":"brood_nest"}`
- **behavior over time:** t0 (0.8s) places `Deco_Scarak_Eggsacks` on ground within 8 -> after 4s active hatching creates friendly Scarak Seeker and Fighter summons, which fight for the 30s authored duration -> end despawns hatchlings/egg state and removes brood nest. [INFERRED] Whether the egg object persists through the full 30s is not authored.
- **targeting:** Ground-target summon range 8.
- **visuals:** Physical `Deco_Scarak_Eggsacks` first, delayed hatch, then visible hatchling swarm; C10 requires exactly Seeker+Fighter+Defender for the implementation lane, while the author description names Seeker and Fighter. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** `SFX_Portal_Void`; hatch sound unassigned.
- **cleanup:** Remove eggsacks/brood nest, hatchling summons and all summon ownership/particles at 30s; no orphan egg object.
- **locks:** universal rules 1, 4, 7, 8; C4/C10; author text's Seeker+Fighter versus C10's implementation trio is an open count question.
- **current gap:** Legacy generic-void row has no eggsacks, four-second hatch, or visible fighting Scarak summons.

### brood_surge — Brood Surge (scarak)
- **author says:** "Instantly hatch active Scarak eggs and grant Scarak summons +40% movement speed for 6 seconds." — style theme: "Insect swarms, summoning, overwhelming numbers"
- **data:** `{"id":"brood_surge","name":"Brood Surge","description":"Instantly hatch active Scarak eggs and grant Scarak summons +40% movement speed for 6 seconds.","damage_percent":0,"cooldown_seconds":7,"cast_time_seconds":0.28,"recovery_seconds":0.2,"effect":"attack_buff","categories":["buff"],"resource_cost":0,"cast_type":"summon_buff","target_type":"allied_summons","radius":12,"duration_seconds":6}`
- **behavior over time:** t0 (0.28s) immediately hatches active Scarak eggs and buffs allied summons in radius 12 -> active window lasts 6s with +40% movement speed -> end removes the speed buff and any hatch-trigger residue, then enters 0.2s recovery.
- **targeting:** Allied-summon targeting, radius 12; not enemy auto-acquire.
- **visuals:** Physical egg hatch and visible swarm surge, then a body-hugging Scarak speed accent on allied summons; current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). Scarak style identity is absent from the generic void row.
- **sound:** `SFX_Portal_Void`; swarm hatch/surge sound unassigned.
- **cleanup:** Remove speed aura and surge particles after 6s; do not remove allied summons except those naturally governed by their own duration.
- **locks:** universal rules 4, 6, 7, 8; C4/C10 summon grammar.
- **current gap:** Legacy row provides no egg hatch trigger, allied-summon filter, +40% speed indicator, or Scarak-family visuals.

### locust_queen — Locust Queen (scarak)
- **author says:** "Summon a friendly Scarak fighter queen proxy directly for 20 seconds." — style theme: "Insect swarms, summoning, overwhelming numbers"
- **data:** `{"id":"locust_queen","name":"Locust Queen","description":"Summon a friendly Scarak fighter queen proxy directly for 20 seconds.","damage_percent":0,"cooldown_seconds":10,"cast_time_seconds":0.8,"recovery_seconds":0.4,"effect":"summon","categories":["summon"],"resource_cost":0,"cast_type":"summon","target_type":"ground_target","range":10,"duration_seconds":20,"summon_name":"locust_queen","terrain_effect":"swarm_gate"}`
- **behavior over time:** t0 (0.8s) summons a friendly queen proxy at ground range 10 -> active window lasts 20s while it visibly fights for the caster -> end despawns it and closes the swarm gate, then enters 0.4s recovery. [INFERRED] Exact queen attack set is not authored.
- **targeting:** Ground-target summon range 10.
- **visuals:** Locked identity is `Scarak_Broodmother`, not `Scarak_Fighter`; physical queen silhouette first, swarm gate second. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=null; model=null; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). The canon misfit specifically records current `Scarak_Fighter` versus required `Scarak_Broodmother`.
- **sound:** `SFX_Portal_Void`; queen summon sound unassigned.
- **cleanup:** Despawn Broodmother at 20s, remove swarm gate, summon ownership/nameplate/collision proxy and all particles.
- **locks:** universal rules 1, 4, 7, 8; C4/U3; locked `Scarak_Broodmother` queen silhouette.
- **current gap:** Legacy generic-void row has no Scarak identity; the documented current misfit is `Scarak_Fighter`, which must become `Scarak_Broodmother`.

## Primordial — Ancient beast forms, transformation

### pterodactyl_form — Pterodactyl Form (primordial)
- **author says:** "Transform into Pterodactyl for 30 seconds. Hold Space to fly; form abilities are Swoop and Carry On." — style theme: "Ancient beast forms, transformation"
- **data:** `{"id":"pterodactyl_form","name":"Pterodactyl Form","description":"Transform into Pterodactyl for 30 seconds. Hold Space to fly; form abilities are Swoop and Carry On.","damage_percent":10,"cooldown_seconds":8,"cast_time_seconds":0.85,"recovery_seconds":0.45,"effect":"evasion+attack_buff","categories":["damage","evasion","buff"],"resource_cost":0,"cast_type":"transformation","target_type":"self","duration_seconds":30,"toggleable":true,"toggle_cooldown_seconds":15.0,"launch_height":6,"travel_type":"flight_form"}`
- **behavior over time:** t0 (0.85s) transforms self -> active form lasts up to 30s; Space enables flight and Swoop/Carry On are the form actions -> end is toggle-off or expiry, reverting the player and entering 0.45s recovery (toggle cooldown 15s). [INFERRED] Exact flight physics and hotbar restriction implementation are not in the row.
- **targeting:** Self transformation; no enemy aim.
- **visuals:** Primordial identity must be a readable Pterodactyl model with form-specific actions, third-person proof, hotbar restrictions, and early exit. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; model=Common/NPC/Flying_Beast/Pterodactyl/Models/Model.blockymodel; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** `SFX_Portal_Void`; flight sound unassigned.
- **cleanup:** On exit/expiry remove Pterodactyl model/effects, flight state, launch-height handling, form actions and temporary hotbar restrictions; restore player identity and all particles.
- **locks:** universal rules 7-8; primordial transformation canon; implementation plan transformation gate (identity/actions/exit/third-person).
- **current gap:** Legacy manifest has the Pterodactyl model but generic-void cast/travel/impact and no evidence of flight/action/hotbar/early-exit behavior.

### triceratops_form — Triceratops Form (primordial)
- **author says:** "Transform into Trillodon for 30 seconds. Stampede breaks forward and Horn Guard protects nearby allies." — style theme: "Ancient beast forms, transformation"
- **data:** `{"id":"triceratops_form","name":"Triceratops Form","description":"Transform into Trillodon for 30 seconds. Stampede breaks forward and Horn Guard protects nearby allies.","damage_percent":12,"cooldown_seconds":8,"cast_time_seconds":0.85,"recovery_seconds":0.45,"effect":"defense_buff","categories":["damage","buff"],"resource_cost":0,"cast_type":"transformation","target_type":"self","duration_seconds":30,"toggleable":true,"toggle_cooldown_seconds":15.0,"dash_distance":10,"knockback_force":5,"travel_type":"stampede_charge"}`
- **behavior over time:** t0 (0.85s) transforms self -> active form lasts up to 30s; Stampede charges 10 blocks with knockback force 5 and Horn Guard protects nearby allies -> end is toggle-off or expiry, reverting to the player and entering 0.45s recovery (toggle cooldown 15s). [INFERRED] Horn Guard radius is not authored.
- **targeting:** Self transformation; Stampede is a forward dash/action, not auto-acquire.
- **visuals:** Q9/G9 lock: identity is **Trillodon everywhere** while display name remains "Triceratops Form"; form actions and third-person proof are required. Current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Ice/Spawner/Impact_Ice_Shockwave.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; model=Common/NPC/Wildlife/Trillodon/Models/Model.blockymodel; role=null; projectileConfig=null; legacy=true` (CURRENT STATE). Ice shockwave is a wrong-family current travel cue.
- **sound:** `SFX_Portal_Void`; stampede impact sound unassigned.
- **cleanup:** Revert Trillodon model, Stampede/Horn Guard state, ally-protection effects, dash collision changes and all particles at early exit or 30s; restore original player identity.
- **locks:** Q9/G9 Trillodon identity and unchanged display name; universal rules 3, 7, 8; transformation gate.
- **current gap:** Manifest has the correct Trillodon model but legacy void/ice cues and no proven Stampede, Horn Guard, toggle exit, or form hotbar contract.

### t_rex_form — T-Rex Form (primordial)
- **author says:** "Transform into Rex_Cave for 30 seconds. Crushing Bite executes low-health enemies and Primal Roar fears foes." — style theme: "Ancient beast forms, transformation"
- **data:** `{"id":"t_rex_form","name":"T-Rex Form","description":"Transform into Rex_Cave for 30 seconds. Crushing Bite executes low-health enemies and Primal Roar fears foes.","damage_percent":20,"cooldown_seconds":10,"cast_time_seconds":0.85,"recovery_seconds":0.45,"effect":"attack_buff+stun","categories":["damage","buff","crowd_control"],"resource_cost":0,"cast_type":"transformation","target_type":"self","duration_seconds":30,"toggleable":true,"toggle_cooldown_seconds":15.0,"radius":4,"terrain_effect":"primal_roar"}`
- **behavior over time:** t0 (0.85s) transforms self -> active form lasts up to 30s with Crushing Bite executing low-health enemies and Primal Roar fearing foes in radius 4 -> end is toggle-off or expiry, reverting player identity and entering 0.45s recovery (toggle cooldown 15s). [INFERRED] The low-health threshold and fear duration are not authored.
- **targeting:** Self transformation; Primal Roar is self-centered radius 4, while Crushing Bite is a form action against enemies.
- **visuals:** Readable Rex_Cave identity, form-specific actions, third-person proof and early exit; current manifest: `cast=Server/Particles/NPC/Spectre_Void/Spawners/Void_Sparks.particlespawner; travel=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; impact=Server/Particles/Combat/Impact/Misc/Void/VoidImpact.particlesystem; loop=Server/Particles/Combat/Impact/Misc/Void/VoidSmoke_Impact.particlespawner; model=Common/NPC/Beast/Rex_Cave/Models/Model.blockymodel; role=null; projectileConfig=null; legacy=true` (CURRENT STATE).
- **sound:** `SFX_Portal_Void`; roar sound unassigned.
- **cleanup:** At exit/30s remove Rex_Cave model, roar fear/attack/stun state, terrain `primal_roar`, hotbar restrictions and all loop/impact particles; restore original player model.
- **locks:** universal rules 7, 8; primordial transformation canon and transformation gate.
- **current gap:** Legacy row has Rex_Cave identity but generic void visuals and no proven Crushing Bite/Primal Roar action wiring, toggle exit, or terrain restore.

## Open questions for the author

- `dark_embrace`: should `target_type=self` be corrected to allow the description's targeted ally/summon, or is ally selection an implementation detail of the self buff?
- `purify`: should the authored `effect=burn+vulnerability` and `categories=[buff]` be retained as runtime semantics alongside the description's cleanse and white-gold enemy damage?
- `scarak_egg`: should the authored Seeker+Fighter pair be the complete hatch, or should the C10 implementation contract's Seeker+Fighter+Defender trio be canon?
- `raise_dead`: the data says `summon_name=skeleton_minion`, while this contract follows the assignment's author-stated Shadow_Knight lock; confirm the intended source-data correction.
- `imbue_swiftness`: what gameplay does `dash_distance=9` describe within the self-buff stance?
- `triceratops_form`: what is the Horn Guard protection radius?
- `t_rex_form`: what low-health threshold does Crushing Bite execute, and how long does Primal Roar fear?
- `fireball`: what is the burn duration/tick cadence?
- `consume`: does the `duration_seconds=2.5` window apply crowd control, channeling, or only delayed execution?
- `hivemind`: is there a simultaneous target cap beyond radius 7?
