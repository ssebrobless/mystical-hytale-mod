# Phase 9 Hydro/Freshwater Validation

- Run: 2026-05-21T22-43-55
- Server log: C:\Users\fishe\AppData\Roaming\Hytale\UserData\Saves\MOTM Creative Test\logs\2026-05-21_22-42-24_server.log
- Data edit: `river_rapids.effect` changed surgically from `attack_buff` to `attack_buff+speed` before build.

## Ability Gates
- PASS: leap_frog cast
  - [2026/05/22 02:44:07   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=leap_frog result=[MOTM] Cast Leap Frog! Charges 1/2. Runtime: hydro wave cast visuals | dash 9.0m forward +2.0m vertical | 1 hit for 22.7 damage | applied vulnerability to 1 target.
- PASS: river_rapids cast
  - [2026/05/22 02:44:18   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=river_rapids result=[MOTM] Cast River Rapids! Runtime: hydro wave cast visuals | self attack buff | self speed | weapon follow-up ready x3 via River Rapids.
- PASS: swamp_monster cast
  - [2026/05/22 02:44:28   INFO]                  [MOTM] [MOTM] Queued ability cast result: playerId=6d49dfd8-b4aa-48ec-b608-4e0c15f00a4d abilityId=swamp_monster result=[MOTM] Cast Swamp Monster! Runtime: hydro wave cast visuals | summoned Frog Green.

## Residual Scan
- PASS: no blocking asset/class errors
- NOTE: non-blocking unmapped NPC/proxy warnings
  - [2026/05/22 02:44:03   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Goblin_Scrapper, modelAssetId=Goblin_Scrapper
  - [2026/05/22 02:44:03   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Bat, modelAssetId=Bat
  - [2026/05/22 02:44:28   INFO]                  [MOTM] [MOTM] Unmapped NPC type encountered. npcTypeId=Frog_Green, modelAssetId=Frog_Green

## Screenshots
- leap_frog.png
- river_rapids.png
- swamp_monster.png

PASS
