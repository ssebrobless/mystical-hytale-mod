# Phase 9 Terra/Soil Validation

- Run: 2026-05-21T21-38-16
- Server log: C:\Users\fishe\AppData\Roaming\Hytale\UserData\Saves\MOTM Creative Test\logs\2026-05-21_21-36-56_server.log
- Data edit: `debris.effect` changed surgically from `vulnerability` to `vulnerability+blind` before build.

## Ability Gates
- PASS: burrow cast
  - [2026/05/22 01:38:27   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=burrow result=[MOTM] Cast Burrow! Charges 1/2. Runtime: terra move visuals | dash 9.0m forward +3.0m vertical | ruptured earth trail | 3 nodes | 3s | 1 hit for 45.4 damage.
- PASS: mudpit cast
  - [2026/05/22 01:38:38   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=mudpit result=[MOTM] Cast Mudpit! Runtime: terra cast visuals | field active for 5s | radius 4.0m | 1 hit for 22.7 damage | applied slow, vulnerability to 1 target.
- PASS: debris cast
  - [2026/05/22 01:38:51   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=debris result=[MOTM] Cast Debris! Runtime: terra cast visuals | launched 4 projectiles at 20.0m/s | volley cadence.

## Residual Scan
- PASS: no missing role/effect/class errors
- NOTE: non-blocking unmapped NPC/proxy warnings
  - [2026/05/22 01:38:23   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Goblin_Scrapper, modelAssetId=Goblin_Scrapper
  - [2026/05/22 01:38:23   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Bat, modelAssetId=Bat
  - [2026/05/22 01:38:51   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Spark_Living, modelAssetId=Spark_Living
- NOTE: residual logged: Burrow still needs underground concealment visual per realignment plan.
- NOTE: Debris data now includes blind, but the live cast log only proves projectile launch; target-side blind/vulnerability should be checked in a later projectile-impact audit.

## Screenshots
- burrow.png
- mudpit.png
- debris.png

PASS
