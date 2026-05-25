# Phase 9 Hydro/Surf Validation

- Run: 2026-05-21T22-19-09
- Server log: C:\Users\fishe\AppData\Roaming\Hytale\UserData\Saves\MOTM Creative Test\logs\2026-05-21_22-17-50_server.log
- Data edit: `high_tide.effect` changed surgically from `knockback` to `knockback+speed` before build.

## Ability Gates
- PASS: high_tide cast
  - [2026/05/22 02:19:21   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=high_tide result=[MOTM] Cast High Tide! Runtime: hydro wave cast visuals | launched 1 projectile at 20.0m/s | self speed.
- PASS: waverider cast
  - [2026/05/22 02:19:32   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=waverider result=[MOTM] Cast Waverider! Runtime: hydro wave cast visuals | shield 39.1 | self speed | weapon follow-up ready x3 via Waverider.
- PASS: riptide cast
  - [2026/05/22 02:19:41   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=riptide result=[MOTM] Cast Riptide! Runtime: hydro wave cast visuals | 3 hits for 34.0 damage | applied vulnerability to 3 targets | pulled 3 targets | current pull 1.8s.

## Residual Scan
- PASS: no missing role/effect/class errors
- NOTE: non-blocking unmapped NPC/proxy warnings
  - [2026/05/22 02:19:17   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Goblin_Scrapper, modelAssetId=Goblin_Scrapper
  - [2026/05/22 02:19:17   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Bat, modelAssetId=Bat
  - [2026/05/22 02:19:21   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Spark_Living, modelAssetId=Spark_Living
- NOTE: High Tide data now includes speed and the cast log confirms `self speed`.

## Screenshots
- high_tide.png
- waverider.png
- riptide.png

PASS
