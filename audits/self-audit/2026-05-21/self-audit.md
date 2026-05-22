# Self-Audit - 2026-05-21

Scope: hard audit of shipped state before Phase B corrections.

## Raw Outputs Captured

Required raw command outputs were saved under `audits/self-audit/2026-05-21/`:

- `git-status.txt`
- `git-log-oneline-20.txt`
- `git-diff-stat.txt`
- `git-diff-StyleManager.java.txt`
- `git-diff-GameplayPlaybackManager.java.txt`
- `git-diff-HytaleAssetResolver.java.txt`
- `audit-directories.txt`

Supplemental evidence files created for Q1-Q8:

- `b1-knockback-log-lines.txt`
- `b2-idle-slow-evidence.txt`
- `stylemanager-thread-grep.txt`
- `stylemanager-synchronized-lines.txt`
- `projectile-role-grep.txt`
- `c3d14e8-stat.txt`
- `start-hytale-subfolder-count.txt`
- `audit-text-files-over-100kb-detected.txt`
- `style-validation-ledger.txt`
- `style-validated-audit-paths.txt`
- `style-remaining-by-class.txt`
- `style-counts.txt`

## Q1. B1 KnockbackResult

Answer: B1 is unverified. The latest cold-launch retest did not exercise the target-hit crash path because the only captured Stomp landing line had `targets=0`.

Direct quote from `b1-knockback-log-lines.txt`:

```text
C:\Users\fishe\AppData\Roaming\Hytale\UserData\Saves\MOTM Creative Test\logs\2026-05-21_20-19-09_server.log:1393:[2026/05/22 00:26:05   INFO]                  [MOTM] [MOTM] Stomp landing resolved: targets=0 damage=0 effects=0 visual=applied
```

## Q2. B2 idle slow

Answer: the user's original symptom was perceived movement/game-feel slowness after idling, not a proven visible slow status. The probe only checked the MOTM status values (`slowMultiplier`, immobilized/root/freeze), so it did not fully prove the subjective symptom is gone. The master plan hypothesis walk did not identify an active MOTM slow/root status. Marking this as `SYMPTOM_UNRESOLVED` for lived feel until the user confirms it in-game.

Direct quote from `master-plan-b2-hypotheses.txt`:

```text
**Symptom (user-reported):** While standing still, the player accumulates some
slow or movement-impairment effect that is not part of the Phase 5 abilities.
```

Direct quote from `b2-idle-slow-evidence.txt`:

```text
audits\blockers\2026-05-21\b2-idle-slow\idle-slow-diagnosis.md:6:- Slow/root/stun/freeze probe failures: 0
audits\blockers\2026-05-21\b2-idle-slow\idle-slow-diagnosis.md:14:H6 runtime status probe: all checkpoints report slowMultiplier=1.000 and immobilized=false.
audits\blockers\2026-05-21\b2-idle-slow\idle-slow-diagnosis.md:25:No MOTM slow/root/immobilize status reproduced during idle, after Stomp, after Aftershock expiry, or after Sinkhole release. The only active effect was Human Adaptable ATTACK_BUFF, which is unrelated to movement. Keep /motm dev effects for future live checks if the visual symptom returns.
```

## Q3. StyleManager synchronized changes

Answer: no real off-thread caller of `StyleManager` was found. The grep found direct `StyleManager` use from command/UI/tick-facing code, plus unrelated threaded code in other managers. The method-level `synchronized` keywords are therefore unnecessary and must be reverted in Phase B.2.

Direct quote from `stylemanager-thread-grep.txt` showing the only thread/executor hit is not a `StyleManager` caller:

```text
C:\Users\fishe\Documents\projects\Mystical-Hytale-Mod\src\main\java\com\motm\manager\PlayerDataManager.java:33:private final ScheduledExecutorService autoSaveScheduler = Executors.newSingleThreadScheduledExecutor();
C:\Users\fishe\Documents\projects\Mystical-Hytale-Mod\src\main\java\com\motm\manager\PlayerDataManager.java:313:autoSaveScheduler.scheduleAtFixedRate(() -> {
```

Direct quote from `stylemanager-thread-grep.txt` showing representative actual callers:

```text
C:\Users\fishe\Documents\projects\Mystical-Hytale-Mod\src\main\java\com\motm\command\MotmCommand.java:695:StyleManager.AbilityUseResult useResult = styleManager.useAbility(player, abilityId);
C:\Users\fishe\Documents\projects\Mystical-Hytale-Mod\src\main\java\com\motm\ui\SpellbookPage.java:798:return mod.getStyleManager().getRemainingCooldownSeconds(player.getPlayerId(), ability.getId());
```

Direct quote from `stylemanager-synchronized-lines.txt` showing the added coarse locks:

```text
src\main\java\com\motm\manager\StyleManager.java:52:    public synchronized boolean selectStyles(PlayerData player, List<String> styleIds) {
src\main\java\com\motm\manager\StyleManager.java:110:    public synchronized AbilityUseResult useAbility(PlayerData player, String abilityId) {
src\main\java\com\motm\manager\StyleManager.java:518:    public synchronized void tickCooldowns() {
```

## Q4. Phase 9 Residual `motm_projectile`

Answer: projectile fix is not applied. The summon proxy was fixed to pass `modelId`, but the projectile visual proxy still passes `PROJECTILE_VISUAL_ROLE_NAME`, which is `"motm_projectile"`.

Direct quote from `projectile-role-grep.txt`:

```text
src\main\java\com\motm\manager\GameplayPlaybackManager.java:78:    private static final String PROJECTILE_VISUAL_ROLE_NAME = "motm_projectile";
src\main\java\com\motm\manager\GameplayPlaybackManager.java:2230:        proxy.setRoleName(PROJECTILE_VISUAL_ROLE_NAME);
src\main\java\com\motm\manager\GameplayPlaybackManager.java:3010:        summon.setRoleName(modelId);
```

Direct quote from `CODEX_PHASE9_RESIDUALS.md`:

```text
- `magma`: Hytale logs `Reloading nonexistent role motm_projectile!` when `magma_sling` spawns the projectile visual proxy. The ability still casts and launches successfully. Likely a cross-cutting projectile proxy-role issue, not Magma-specific.
```

## Q5. Commit hygiene

Answer: commit `c3d14e8` contains at least 8 distinct logical steps:

1. Master/phase plan import.
2. Harness scripts and templates.
3. Phase 5 Quake mechanics and visuals.
4. Phase 6 Spellbook UI.
5. Phase 7 perk/runtime stat wiring.
6. Phase 9 styles 2-5 validation.
7. Phase 10 archive moves.
8. Evidence/gitignore/README state updates.

Direct quote from `c3d14e8-stat.txt`:

```text
c3d14e8 Complete autonomous Hytale roadmap slice
47 files changed, 8981 insertions(+), 74 deletions(-)
```

## Q6. Audit-folder size

Answer: there are 39 `audits/harness/start-hytale/` subfolders. None are older than 2026-05-21, but Phase B.6 still needs retention cleanup to latest 3.

Direct quote from `start-hytale-subfolder-count.txt`:

```text
Start-hytale subfolders: 39
Older-than-2026-05-21 subfolders: 0
```

Direct quote from `start-hytale-subfolder-paths.txt`:

```text
C:\Users\fishe\Documents\projects\Mystical-Hytale-Mod\audits\harness\start-hytale\2026-05-21T15-36-28
C:\Users\fishe\Documents\projects\Mystical-Hytale-Mod\audits\harness\start-hytale\2026-05-21T15-36-58
C:\Users\fishe\Documents\projects\Mystical-Hytale-Mod\audits\harness\start-hytale\2026-05-21T15-38-14
```

## Q7. Discovery dumps

Answer: two audit text files over 100KB are UTF-16/NUL-interleaved and should be deleted/re-emitted as UTF-8 in Phase B.6: `package-files.txt` and `motm-command-symbols.txt`.

Direct quote from `audit-text-files-over-100kb-detected.txt`:

```text
FullName       : C:\Users\fishe\Documents\projects\Mystical-Hytale-Mod\audits\harness\2026-05-21\discovery\package-files.txt
Length         : 1110706
SampleNulBytes : 99999
Utf16Likely    : True

FullName       : C:\Users\fishe\Documents\projects\Mystical-Hytale-Mod\audits\harness\2026-05-21\discovery\motm-command-symbols.txt
Length         : 461192
SampleNulBytes : 99999
Utf16Likely    : True
```

## Q8. Phase 9 progress

Answer: 7 styles are validated and 33 remain.

Direct quote from `style-counts.txt`:

```text
Total styles: 40
Validated: 7
Remaining: 33
```

Direct quote from `style-validated-audit-paths.txt`:

```text
Terra/Quake -> audits/phase5/2026-05-21T17-19-18/report.md; audits/phase5/2026-05-21T17-20-55/report.md; latest regression audits/phase5/2026-05-21T20-18-44/report.md
Terra/Magma -> audits/phase9-magma/2026-05-21T18-51-28/magma-after-casts.png; commit bd3b7be Validate Terra Magma style slice
Terra/Metal -> audits/phase9-styles-2-5/2026-05-21T18-06-00/report.md
Hydro/Icicle -> audits/phase9-styles-2-5/2026-05-21T18-06-00/report.md
Hydro/Snow -> audits/phase9-snow/2026-05-21T20-17-49/snow-after-casts-role-model-2.png; commit 3872d8a Validate Hydro Snow style slice
Aero/Wind Blade -> audits/phase9-styles-2-5/2026-05-21T18-06-00/report.md
Corruptus/Flame -> audits/phase9-styles-2-5/2026-05-21T18-06-00/report.md
```

Direct quote from `style-remaining-by-class.txt`:

```text
[aero] gale_wizard, jet, jump, pollution, pressure, scream, smoke, thunder, tornado
[corruptus] attonement, hell_flame, imbuement, mentokinesis, necro, primordial, scarak, shadow, void
[hydro] bilgewater, boiling, freshwater, iceberg, rain, saltwater, surf, vapor
[terra] arbor, bloom, gem, sand, self_petrification, soil, stone
```

## Self-Audit Result

FAIL for claimed completion integrity. Phase A confirms three gaps that require Phase B correction before Phase C:

```text
B1: unverified because Stomp target count was 0.
B2: MOTM status slow not reproduced, but user-perceived slow remains SYMPTOM_UNRESOLVED.
B3: projectile proxy still uses motm_projectile.
```
