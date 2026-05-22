# Phase 9 Terra/Gem Validation

- Run: 2026-05-21T21-47-00
- Server log: C:\Users\fishe\AppData\Roaming\Hytale\UserData\Saves\MOTM Creative Test\logs\2026-05-21_21-45-23_server.log
- Data/code edit: `refraction.effect` now includes speed; `fracture` impact resolves to crystal sparks.

## Ability Gates
- PASS: lapidary cast
  - [2026/05/22 01:47:11   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=lapidary result=[MOTM] Cast Lapidary! Runtime: terra gem cast visuals | shield 83.9 | weapon follow-up ready x2 via Lapidary.
- PASS: fracture cast
  - [2026/05/22 01:47:21   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=fracture result=[MOTM] Cast Fracture! Runtime: terra gem cast visuals | launched 1 projectile at 26.0m/s.
- PASS: refraction cast
  - [2026/05/22 01:47:32   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=refraction result=[MOTM] Cast Refraction! Runtime: terra gem cast visuals | self attack buff | self speed | weapon follow-up ready x3 via Refraction.

## Residual Scan
- PASS: no missing role/effect/class errors
- NOTE: non-blocking unmapped NPC/proxy warnings
  - [2026/05/22 01:47:07   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Goblin_Scrapper, modelAssetId=Goblin_Scrapper
  - [2026/05/22 01:47:07   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Bat, modelAssetId=Bat
  - [2026/05/22 01:47:21   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Spark_Living, modelAssetId=Spark_Living
- NOTE: Fracture cast log proves projectile-line launch; crystal impact routing was verified by build/resolver diff, not a target-side impact log.

## Screenshots
- lapidary.png
- fracture.png
- refraction.png

PASS
