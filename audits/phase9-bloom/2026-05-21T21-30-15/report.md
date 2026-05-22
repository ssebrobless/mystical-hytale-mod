# Phase 9 Terra/Bloom Validation

- Run: 2026-05-21T21-30-15
- Server log: C:\Users\fishe\AppData\Roaming\Hytale\UserData\Saves\MOTM Creative Test\logs\2026-05-21_21-28-58_server.log
- Data edit: `frolick.effect` changed surgically from `heal+attack_buff` to `heal+attack_buff+speed` before build.

## Ability Gates
- PASS: nightshade cast
  - [2026/05/22 01:30:26   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=nightshade result=[MOTM] Cast Nightshade! Runtime: terra cast visuals | 1 hit for 18.1 damage | applied dot to 1 target.
- PASS: frolick cast
  - [2026/05/22 01:30:38   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=frolick result=[MOTM] Cast Frolick! Runtime: terra cast visuals | self attack buff | self speed | weapon follow-up ready x3 via Frolick.
- PASS: cacti_cluster cast
  - [2026/05/22 01:30:47   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=cacti_cluster result=[MOTM] Cast Cacti Cluster! Runtime: terra cast visuals | launched 5 projectiles at 24.0m/s | volley cadence.

## Residual Scan
- PASS: no missing role/effect/class errors
- NOTE: non-blocking unmapped NPC/proxy warnings
  - [2026/05/22 01:30:22   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Goblin_Scrapper, modelAssetId=Goblin_Scrapper
  - [2026/05/22 01:30:22   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Bat, modelAssetId=Bat
  - [2026/05/22 01:30:47   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Spark_Living, modelAssetId=Spark_Living

## Screenshots
- nightshade.png
- frolick.png
- cacti_cluster.png

PASS
