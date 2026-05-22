# Phase 9 terra Flatlands Class Audit

- Run: 2026-05-22T02-47-33
- World: MOTM Creative Test
- Style source: C:\Users\fishe\Documents\projects\Mystical-Hytale-Mod\src\main\resources\data\styles\terra_styles.json
- Styles: quake, metal, magma, stone, arbor, bloom, self_petrification, soil, sand, gem

- Server log: C:\Users\fishe\AppData\Roaming\Hytale\UserData\Saves\MOTM Creative Test\logs\2026-05-22_00-29-59_server.log

## quake

- PASS: `stomp` cast
  - [2026/05/22 06:47:47   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=stomp result=[MOTM] Cast Stomp! Charges 3/4. Runtime: Stomp armed (jump -> land to release the shockwave).
- PASS: `aftershock` cast
  - [2026/05/22 06:47:53   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=aftershock result=[MOTM] Cast Aftershock! Runtime: terra cast visuals | field active for 4s | radius 5.0m | applied slow, disoriented to 2 targets.
  - REVIEW: [2026/05/22 06:47:53 SEVERE]          [NPC|P] Reloading nonexistent role motm_field!
  - REVIEW: [2026/05/22 06:47:53 SEVERE]          [NPC|P] Reloading nonexistent role motm_field!
  - REVIEW: [2026/05/22 06:47:53 SEVERE]          [NPC|P] Reloading nonexistent role motm_field!
  - REVIEW: [2026/05/22 06:47:53 SEVERE]          [NPC|P] Reloading nonexistent role motm_field!
  - REVIEW: [2026/05/22 06:47:53 SEVERE]          [NPC|P] Reloading nonexistent role motm_field!
  - REVIEW: [2026/05/22 06:47:53 SEVERE]          [NPC|P] Reloading nonexistent role motm_field!
  - REVIEW: [2026/05/22 06:47:53 SEVERE]          [NPC|P] Reloading nonexistent role motm_field!
  - REVIEW: [2026/05/22 06:47:53 SEVERE]          [NPC|P] Reloading nonexistent role motm_field!
  - REVIEW: [2026/05/22 06:47:53 SEVERE]          [NPC|P] Reloading nonexistent role motm_field!
- PASS: `sinkhole` cast
  - [2026/05/22 06:47:59   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=sinkhole result=[MOTM] Cast Sinkhole! Runtime: terra cast visuals | hazard arms in 0.6s | lasts 2.5s | radius 3.0m | 1 hit for 35.0 damage | applied stun to 1 target.
  - REVIEW: [2026/05/22 06:47:59 SEVERE]          [NPC|P] Reloading nonexistent role motm_field!

## metal

- PASS: `iron_wall` cast
  - [2026/05/22 06:48:11   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=iron_wall result=[MOTM] Cast Iron Wall! Runtime: terra cast visuals | barrier active for 6s | width 6.0m.
- PASS: `metal_coat` cast
  - [2026/05/22 06:48:16   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=metal_coat result=[MOTM] Cast Metal Coat! Runtime: terra cast visuals | self defense buff | weapon follow-up ready x2 via Metal Coat.
- PASS: `alloy_enhancement` cast
  - [2026/05/22 06:48:22   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=alloy_enhancement result=[MOTM] Cast Alloy Enhancement! Runtime: terra cast visuals | self damage buff | weapon follow-up ready x1 via Alloy Enhancement.

## magma

- PASS: `lava_pool` cast
  - [2026/05/22 06:48:34   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=lava_pool result=[MOTM] Cast Lava Pool! Runtime: terra cast visuals | field active for 6s | radius 4.0m | 1 hit for 18.7 damage | applied burn to 1 target.
- PASS: `obsidian_skin` cast
  - [2026/05/22 06:48:39   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=obsidian_skin result=[MOTM] Cast Obsidian Skin! Runtime: terra cast visuals | shield 86.5 | weapon follow-up ready x2 via Obsidian Skin.
- PASS: `magma_sling` cast
  - [2026/05/22 06:48:45   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=magma_sling result=[MOTM] Cast Magma Sling! Runtime: terra cast visuals | launched 1 projectile at 22.0m/s.

## stone

- PASS: `rubble_rouser` cast
  - [2026/05/22 06:48:57   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=rubble_rouser result=[MOTM] Cast Rubble Rouser! Runtime: terra cast visuals | launched 1 projectile at 20.0m/s.
- PASS: `pillar_strike` cast
  - [2026/05/22 06:49:02   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=pillar_strike result=[MOTM] Cast Pillar Strike! Runtime: terra cast visuals | 2 hits for 70.1 damage | applied stun to 1 target.
- PASS: `rockslide` cast
  - [2026/05/22 06:49:08   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=rockslide result=[MOTM] Cast Rockslide! Runtime: terra cast visuals | field arms in 0.8s | lasts 3s | radius 6.0m | 1 hit for 46.7 damage.

## arbor

- PASS: `rooted` cast
  - [2026/05/22 06:49:20   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=rooted result=[MOTM] Cast Rooted! Runtime: terra cast visuals | root circle aura | radius 3.4m | 5s.
- PASS: `vines` cast
  - [2026/05/22 06:49:25   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=vines result=[MOTM] Cast Vines! Runtime: terra cast visuals | 1 hit for 11.7 damage | applied stun to 1 target.
- PASS: `sapling` cast
  - [2026/05/22 06:49:31   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=sapling result=[MOTM] Cast Sapling! Runtime: terra cast visuals | summoned Spirit Root.

## bloom

- PASS: `nightshade` cast
  - [2026/05/22 06:49:43   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=nightshade result=[MOTM] Cast Nightshade! Runtime: terra cast visuals | No valid target in range.
  - REVIEW: [2026/05/22 06:49:43   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=nightshade result=[MOTM] Cast Nightshade! Runtime: terra cast visuals | No valid target in range.
- PASS: `frolick` cast
  - [2026/05/22 06:49:48   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=frolick result=[MOTM] Cast Frolick! Runtime: terra cast visuals | self attack buff | self speed | weapon follow-up ready x3 via Frolick.
- PASS: `cacti_cluster` cast
  - [2026/05/22 06:49:54   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=cacti_cluster result=[MOTM] Cast Cacti Cluster! Runtime: terra cast visuals | launched 5 projectiles at 24.0m/s | volley cadence.

## self_petrification

- PASS: `gargoyle` cast
  - [2026/05/22 06:50:06   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=gargoyle result=[MOTM] Cast Gargoyle! Runtime: terra cast visuals | shield 57.7 | weapon follow-up ready x2 via Gargoyle.
- PASS: `glare` cast
  - [2026/05/22 06:50:11   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=glare result=[MOTM] Cast Glare! Runtime: terra cast visuals | applied stun to 2 targets.
- PASS: `tunnel` cast
  - [2026/05/22 06:50:17   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=tunnel result=[MOTM] Cast Tunnel! Runtime: terra move visuals | dash 10.0m forward +2.5m vertical | tunnel path trail | 3 nodes | 3s | self evasion | 1 hit for 28.0 damage.

## soil

- PASS: `burrow` cast
  - [2026/05/22 06:50:29   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=burrow result=[MOTM] Cast Burrow! Charges 1/2. Runtime: terra move visuals | dash 9.0m forward +3.0m vertical | ruptured earth trail | 3 nodes | 3s | No valid target in range.
  - REVIEW: [2026/05/22 06:50:29   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=burrow result=[MOTM] Cast Burrow! Charges 1/2. Runtime: terra move visuals | dash 9.0m forward +3.0m vertical | ruptured earth trail | 3 nodes | 3s | No valid target in range.
- PASS: `mudpit` cast
  - [2026/05/22 06:50:35   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=mudpit result=[MOTM] Cast Mudpit! Runtime: terra cast visuals | field active for 5s | radius 4.0m | 1 hit for 23.4 damage | applied slow, vulnerability to 1 target.
- PASS: `debris` cast
  - [2026/05/22 06:50:40   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=debris result=[MOTM] Cast Debris! Runtime: terra cast visuals | launched 4 projectiles at 20.0m/s | volley cadence.

## sand

- PASS: `sandstorm` cast
  - [2026/05/22 06:50:52   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=sandstorm result=[MOTM] Cast Sandstorm! Runtime: terra cast visuals | field active for 6s | radius 5.0m | 1 hit for 11.7 damage | applied dot, slow to 1 target.
- PASS: `dust_devil` cast
  - [2026/05/22 06:50:57   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=dust_devil result=[MOTM] Cast Dust Devil! Runtime: terra cast visuals | field active for 4s | radius 3.0m | 1 hit for 28.0 damage | applied knockback to 1 target.
- PASS: `vitrification` cast
  - [2026/05/22 06:51:03   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=vitrification result=[MOTM] Cast Vitrification! Charges 2/3. Runtime: terra cast visuals | launched 1 projectile at 24.0m/s.

## gem

- PASS: `lapidary` cast
  - [2026/05/22 06:51:15   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=lapidary result=[MOTM] Cast Lapidary! Runtime: terra gem cast visuals | shield 86.5 | weapon follow-up ready x2 via Lapidary.
- PASS: `fracture` cast
  - [2026/05/22 06:51:21   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=fracture result=[MOTM] Cast Fracture! Runtime: terra gem cast visuals | launched 1 projectile at 26.0m/s.
- PASS: `refraction` cast
  - [2026/05/22 06:51:26   INFO]           [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=refraction result=[MOTM] Cast Refraction! Runtime: terra gem cast visuals | self attack buff | self speed | weapon follow-up ready x3 via Refraction.

## Residual Scan

- PASS: no blocking class/runtime errors in class audit slice.

PASS
