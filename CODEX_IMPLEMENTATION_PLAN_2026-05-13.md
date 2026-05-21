# Codex Implementation Plan — 2026-05-13 (v2, comprehensive)

> **Audience:** Codex (medium effort). This is the complete blueprint to make the mod functional end-to-end.
> Every phase has: exact files, drop-in code, exact acceptance criteria, decision tables for forks.
> **Do not regenerate `src/main/resources/data/styles/*.json` — surgical edits only. See CLAUDE.md.**
> **Do not assume an instruction is wrong because it's verbose. Be exhaustive, not clever.**

---

## 0. Current-state audit (read this before you touch anything)

### What is already wired and working (do not redo)
- 4 classes × 10 styles × 3 abilities = **120 fully-authored abilities** in `src/main/resources/data/styles/*.json` with spatial metadata (`cast_type`, `target_type`, `range`, `radius`, `knockback_force`, `projectile_speed`, `duration_seconds`, `terrain_effect`, etc.).
- 800 perks (declared), 12 races, 4 classes, leveling 1-200, mob scaling, elite titles — all loaded by `DataLoader`.
- Internal cast pipeline:
  `tryCastSpellbookSlot` → `MotmCommand.castAbilityBySlot` → cooldown/resource checks → `GameplayPlaybackManager.executeAbility(...)`.
- Visual layer: `HytaleAssetResolver` maps every ability to vanilla animation (`.blockyanim`), particle (`.particlespawner`/`.particlesystem`), and model (`.blockymodel`) IDs. `applyEffectById` uses `EntityEffect.getAssetMap().getAsset(effectId)` to actually fire them.
- 31 custom MOTM `EntityEffect` assets in `src/main/resources/Server/Entity/Effects/MOTM/` (e.g. `MOTM_Terra_Cast.json`, `MOTM_Hydro_Wave_Impact.json`).
- ECS tick wired (`MotmServerTickSystem`) — drives `MenteesMod.onServerTick(Store<EntityStore>)`.
- Mob spawn detection + on-first-sight scaling + death attribution (`MotmMobRuntimeSystem`) — drives `MenteesMod.onMobSpawn(...)` and `onMobKilled(...)`.
- Status HUD asset (`Common/UI/Custom/HUD/MOTM_StatusHud.ui`) and Java HUD class (`MotmStatusHud`).
- Custom item interactions package: **does not yet exist** (created in Phase 2).
- Spellbook custom UI page Java (`SpellbookPage`) — but its `.ui` document is **missing** (Phase 6).
- Perk effect application: **stub only**. `PerkManager.applyPerkEffects` only logs; never affects stats. (Phase 7.)

### Root-cause defects
1. **Custom spellbook input contract is wrong** (Phase 2). `MOTM_Spellbook_Focus.json` uses vanilla bare strings (`Block_Secondary`, `Root_Unarmed_Attack_Swing_Left`) for slot bindings. Per the [official guide](https://hytalemodding.dev/en/docs/guides/plugin/item-interaction), slot values must be **objects** containing `Interactions: [{ Type: "<custom_id>" }]` where `<custom_id>` is a Java-registered `SimpleInstantInteraction` subclass.
2. **`PlayerInteractEvent` is unreliable for custom items** in base API. Mitigated optionally via [PlayerInteractLib](https://www.curseforge.com/hytale/mods/playerinteractlib) (Phase 8). `PlayerMouseButtonEvent` is confirmed working for LMB ([Hytale-Docs#25](https://github.com/timiliris/Hytale-Docs/issues/25)).
3. **Lifecycle does duplicate work** (Phase 3). `onPlayerConnect` runs `onPlayerJoin` + `rebuildPlayerRuntime`, then `onPlayerReady` → `initializeRuntimePlayer` runs `onPlayerJoin` again + `rebuildPlayerRuntimeNow` + `ensureSpellbookItem`.
4. **Asset pack manifest** (Phase 1). The 2026-03-24 log shows `Skipping pack at com.motm_Mentees of the Mystical: missing or invalid manifest.json`. `build.gradle` does have `processResources { filesMatching('manifest.json') { expand(...) } }`, so this *should* work — but verify Phase 1 confirms the expanded manifest sits at jar root.
5. **Spellbook UI page document missing** (Phase 6). `SpellbookPage.java` line 36 references `Pages/MOTM_Spellbook.ui` but that file does not exist in `src/main/resources/Common/UI/Custom/Pages/`. Until it's created, crouch-Use opens nothing — chat fallback is what runs.
6. **Perks don't do anything** (Phase 7). `PerkManager.applyPerkEffects` is a `LOG.fine` stub. 800 perks select-and-save fine but never modify gameplay.

### Vertical-slice acceptance gate (the only gate that matters before Phase 9 broadening)
**Terra / Quake**, three abilities, three inputs:
- Slot 1 = **Stomp** ← Left Click
- Slot 2 = **Aftershock** ← Right Click
- Slot 3 = **Sinkhole** ← Use (E by default)

Two consecutive fresh-launch runs must pass before broadening (Phase 5).

### Phase summary
| Phase | Goal | Effort | Blocks |
| --- | --- | --- | --- |
| 1 | Build + manifest sanity | ~10 min | Everything downstream |
| 2 | Custom spellbook interactions | ~2 hr | Phase 5 |
| 3 | Lifecycle split | ~45 min | Phase 5 |
| 4 | Asset pack expansion workflow (reference) | n/a | Phase 6, 9 |
| 5 | Quake vertical slice acceptance | ~30 min | Phase 9 |
| 6 | Custom Spellbook UI page | ~3 hr | nothing critical |
| 7 | Perk effect integration | ~4 hr | nothing critical |
| 8 | PlayerInteractLib fallback (optional) | ~1 hr | nothing |
| 9 | Broaden across remaining 39 styles | content-driven | n/a |
| 10 | README + memory cleanup | ~15 min | n/a |

---

## Phase 1 — Verify build + asset pack manifest

### Goal
Prove the jar builds, the manifest tokens expand, and the asset pack scanner loads it. This is the lowest layer.

### Files
- `build.gradle` (read only)
- `src/main/resources/manifest.json` (no change expected)
- `scripts/build-install.ps1` (no change)

### Step 1.1 — Build a fresh jar
Run from project root:
```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-install.ps1
```
Expected outputs:
- `build/libs/mentees_of_the_mystical-1.0.1.jar` (or `-internal` suffix if `motm_build_channel=internal`)
- A copy at `%APPDATA%/Hytale/UserData/Mods/mentees_of_the_mystical-1.0.1.jar`

### Step 1.2 — Verify expanded manifest inside the built jar
```powershell
$jar = Get-ChildItem build/libs/mentees_of_the_mystical-*.jar | Sort-Object LastWriteTime -Descending | Select-Object -First 1
$tempDir = New-Item -ItemType Directory -Force -Path "$env:TEMP\motm_jar_check"
Push-Location $tempDir
& "$env:JAVA_HOME\bin\jar.exe" xf $jar.FullName manifest.json
Get-Content .\manifest.json
Pop-Location
Remove-Item -Recurse -Force $tempDir
```
**Required outcome:** the printed `manifest.json` shows real values:
- `"Group": "com.motm"`
- `"Name": "Mentees of the Mystical"`
- `"Version": "1.0.1"`
- `"Main": "com.motm.MenteesMod"`
- `"IncludesAssetPack": true`

NOT literal `${mod_name}` etc.

### Step 1.3 — Smoke launch and check pack load
Launch Hytale, log into a singleplayer creative world, then immediately exit. Open the newest log under `logs/` (Hytale game-client logs) **and** the mod-side `logs/<date>_server.log`. Required:
- `[PluginManager] - com.motm:Mentees of the Mystical from path mentees_of_the_mystical-1.0.1.jar`
- **No** `Skipping pack at com.motm_Mentees of the Mystical: missing or invalid manifest.json`
- **No** `Failed to load any asset packs`
- `[AssetModule|P] Loaded pack ...` referencing the MOTM pack
- `[MOTM] >>> onPlayerConnect`

### Decision table — Phase 1 forks
| Symptom | Action |
| --- | --- |
| Tokens like `${mod_name}` still literal in extracted manifest | In `build.gradle`, replace `expand(...)` with explicit `filter(org.apache.tools.ant.filters.ReplaceTokens, tokens: [...])` block and rebuild. The Groovy `expand` syntax can interact badly with `${...}` placeholders — fall back to ReplaceTokens with `@token@` style and update manifest.json placeholders to match. |
| Manifest missing from jar root entirely | Add explicit `jar { from('build/resources/main') { include 'manifest.json'; include 'Common/**'; include 'Server/**' } }` block in `build.gradle`. |
| `Skipping pack at com.motm_Mentees of the Mystical: missing or invalid manifest.json` still appears with valid manifest | Hytale may scan based on jar filename. Confirm jar archive contains `manifest.json` at root (use `jar tf <jar>`). If yes, try moving the file via processResources to the exact root. If still failing, check `IncludesAssetPack` is `true` AND that `Common/` and `Server/` folders are present in the jar (`jar tf <jar> \| Select-String "^(Common\|Server)"`). |
| Build error: `Could not find HytaleServer.jar at ...` | The path `%APPDATA%/Hytale/install/release/package/game/latest/Server/HytaleServer.jar` must exist. Open Hytale launcher; let it install the server. Re-run build. |
| Build error: JDK 25 not found | `scripts/build-install.ps1` downloads it to `.tools/`. Inspect the script and confirm `.tools/jdk-25/` exists. If absent, the download step failed — check network. |

### Acceptance gate
Phase 1 passes only when Step 1.3 outcomes ALL match. Do not proceed until they do.

---

## Phase 2 — Register custom MOTM Spellbook interactions (THE critical fix)

### Goal
Make Left Click / Right Click / Use on the MOTM Spellbook emit plugin-owned interaction IDs that route directly into `tryCastSpellbookSlot` — bypassing the unreliable vanilla `PlayerInteractEvent` path for custom items.

### Files (touched)
- `src/main/java/com/motm/interaction/MotmSpellbookInteraction.java` *(new)*
- `src/main/resources/Server/Item/Items/MOTM_Spellbook_Focus.json` *(rewrite)*
- `src/main/java/com/motm/MenteesMod.java` *(register codecs in `setup()`, add bridge method)*

### Step 2.1 — Create the new package + class

Create `src/main/java/com/motm/interaction/` and inside it create `MotmSpellbookInteraction.java`:

```java
package com.motm.interaction;

import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.codec.BuilderCodec;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.cooldown.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.motm.MenteesMod;

import javax.annotation.Nonnull;
import java.util.logging.Logger;

/**
 * Custom interactions registered as the spellbook's Primary/Secondary/Use slot bindings.
 * Each subclass routes to a slot (1/2/3) and calls into MenteesMod's existing cast pipeline.
 * MenteesMod handle is injected once at plugin setup via setMod().
 */
public abstract class MotmSpellbookInteraction extends SimpleInstantInteraction {

    private static final Logger LOG = Logger.getLogger("MOTM");
    private static volatile MenteesMod MOD;

    public static void setMod(MenteesMod mod) {
        MOD = mod;
    }

    protected abstract int slot();

    @Override
    protected void firstRun(@Nonnull InteractionType interactionType,
                            @Nonnull InteractionContext interactionContext,
                            @Nonnull CooldownHandler cooldownHandler) {
        MenteesMod mod = MOD;
        if (mod == null) {
            LOG.warning("[MOTM] MotmSpellbookInteraction fired before mod registration");
            return;
        }
        Player player = null;
        try {
            Object entity = interactionContext.getEntity();
            if (entity instanceof Player p) {
                player = p;
            }
        } catch (Throwable t) {
            LOG.warning("[MOTM] MotmSpellbookInteraction: could not read entity from context: " + t.getMessage());
        }
        if (player == null) {
            return;
        }
        LOG.info("[MOTM] Custom spellbook interaction fired: type=" + interactionType
                + " slot=" + slot() + " player=" + player);
        mod.castSpellbookSlotFromInteraction(player, slot());
    }

    public static final class Primary extends MotmSpellbookInteraction {
        public static final BuilderCodec<Primary> CODEC =
                BuilderCodec.builder(Primary.class, Primary::new, SimpleInstantInteraction.CODEC).build();
        @Override protected int slot() { return 1; }
    }

    public static final class Secondary extends MotmSpellbookInteraction {
        public static final BuilderCodec<Secondary> CODEC =
                BuilderCodec.builder(Secondary.class, Secondary::new, SimpleInstantInteraction.CODEC).build();
        @Override protected int slot() { return 2; }
    }

    public static final class Use extends MotmSpellbookInteraction {
        public static final BuilderCodec<Use> CODEC =
                BuilderCodec.builder(Use.class, Use::new, SimpleInstantInteraction.CODEC).build();
        @Override protected int slot() { return 3; }
    }
}
```

### Step 2.2 — Add the public bridge method on `MenteesMod`

In `src/main/java/com/motm/MenteesMod.java`, find `tryCastSpellbookSlot` (~line 2566). Add the following **public** method directly above it (so call-sites are adjacent):

```java
/**
 * Entry point used by MotmSpellbookInteraction subclasses (custom item interaction codec).
 * Resolves player data and routes into the existing cast pipeline.
 * Phase 2 of CODEX_IMPLEMENTATION_PLAN_2026-05-13.md.
 */
public void castSpellbookSlotFromInteraction(Player runtimePlayer, int slot) {
    if (runtimePlayer == null || slot <= 0) {
        return;
    }
    String playerId = getRuntimePlayerId(runtimePlayer);
    if (playerId == null) {
        return;
    }
    var playerData = playerDataManager.getOnlinePlayer(playerId);
    if (playerData == null) {
        return;
    }
    if (isDuplicateSpellbookInput(playerId, slot)) {
        return;
    }
    String response = tryCastSpellbookSlot(
            runtimePlayer,
            playerData,
            slot,
            "interaction:custom",
            null,
            null
    );
    if (response != null && !response.isBlank()) {
        runtimePlayer.sendMessage(com.hypixel.hytale.server.core.Message.raw(response));
    }
}
```

Note: same class can call private `isDuplicateSpellbookInput` and `tryCastSpellbookSlot`. Do **not** change their visibility.

### Step 2.3 — Register the codecs in `MenteesMod.setup()`

Find `registerHytaleHooks()` (~line 347 in current file). It's currently the last call in the existing setup-or-start path. Add the codec registrations **just before** that call. Use the existing `LOG` field that's already at file scope.

```java
// Custom spellbook interactions — wire the JSON Interactions block to our cast pipeline.
com.motm.interaction.MotmSpellbookInteraction.setMod(this);
var interactionCodecRegistry = getCodecRegistry(
        com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction.CODEC);
interactionCodecRegistry.register(
        "motm_spellbook_primary",
        com.motm.interaction.MotmSpellbookInteraction.Primary.class,
        com.motm.interaction.MotmSpellbookInteraction.Primary.CODEC);
interactionCodecRegistry.register(
        "motm_spellbook_secondary",
        com.motm.interaction.MotmSpellbookInteraction.Secondary.class,
        com.motm.interaction.MotmSpellbookInteraction.Secondary.CODEC);
interactionCodecRegistry.register(
        "motm_spellbook_use",
        com.motm.interaction.MotmSpellbookInteraction.Use.class,
        com.motm.interaction.MotmSpellbookInteraction.Use.CODEC);
LOG.info("[MOTM] Registered MOTM spellbook custom interactions: primary/secondary/use");
```

### Step 2.4 — Rewrite the item JSON

Overwrite `src/main/resources/Server/Item/Items/MOTM_Spellbook_Focus.json` with **exactly** this content:

```json
{
  "TranslationProperties": {
    "Name": "Mentees Spellbook"
  },
  "Categories": [
    "Items.Misc"
  ],
  "Icon": "Icons/ItemsGenerated/Weapon_Spellbook_Grimoire_Brown.png",
  "Model": "Items/Weapons/Spellbook/Grimoire.blockymodel",
  "Texture": "Items/Weapons/Spellbook/Grimoire_Brown_Texture.png",
  "Interactions": {
    "Primary": {
      "Interactions": [
        { "Type": "motm_spellbook_primary" }
      ]
    },
    "Secondary": {
      "Interactions": [
        { "Type": "motm_spellbook_secondary" }
      ]
    },
    "Use": {
      "Interactions": [
        { "Type": "motm_spellbook_use" }
      ]
    }
  },
  "MaxStack": 1,
  "Tags": {
    "Type": [
      "Misc"
    ],
    "Family": [
      "Spellbook",
      "MOTM"
    ]
  },
  "ItemSoundSetId": "ISS_Weapons_Books"
}
```

**Critical:** the old strings `"Root_Unarmed_Attack_Swing_Left"` and `"Block_Secondary"` are gone. Their presence is what caused right-click to behave like a vanilla block-place — the source of "idle damage / movement slowdown when book equipped."

### Step 2.5 — Build and smoke-test

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-install.ps1
```

Then launch Hytale, log into a creative world, and:
1. `/motm class terra`
2. `/motm style quake`
3. The spellbook should be granted automatically; if not, `/motm spellbook overview` (which queues a grant) or relog.
4. Equip the Mentees Spellbook.
5. Click Left, Right, Use once each (with at least 2s between clicks).

### Acceptance gate
Inspect the new `logs/<date>_server.log`. **Required to see, in order, per click:**
- `[MOTM] Custom spellbook interaction fired: type=Primary slot=1 player=...` for LMB
- `[MOTM] Custom spellbook interaction fired: type=Secondary slot=2 ...` for RMB
- `[MOTM] Custom spellbook interaction fired: type=Use slot=3 ...` for Use
- `[MOTM] Spellbook cast attempt: ... slot=N source=interaction:custom` per click
- `[MOTM] Queue ability cast: ... abilityId=stomp` (slot 1) / `abilityId=aftershock` (slot 2) / `abilityId=sinkhole` (slot 3)

**Visible** in-world ability behavior is the **Phase 5** gate, not this one. Phase 2 only proves the input contract.

### Decision table — Phase 2 build/runtime forks
| Build error | Action |
| --- | --- |
| Cannot resolve `BuilderCodec` import | Search HytaleServer.jar: `& "$env:JAVA_HOME\bin\jar.exe" tf "$env:APPDATA\Hytale\install\release\package\game\latest\Server\HytaleServer.jar" \| Select-String -Pattern "BuilderCodec"`. Use the package path returned. Update the import in `MotmSpellbookInteraction.java`. |
| Cannot resolve `SimpleInstantInteraction` | Same method, `Select-String -Pattern "SimpleInstantInteraction"`. Likely path `com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction`. Correct the import. |
| Cannot resolve `Interaction.CODEC` | `Select-String -Pattern "interaction/config/Interaction"` then inspect the class (`jar xf` it). Find the public static `BuilderCodec<Interaction>` field; use its name. |
| `getCodecRegistry(...)` doesn't exist | Search HytaleServer.jar for `CodecRegistry`. Likely surface is `getCodecRegistry(BuilderCodec)`. If not present on `JavaPlugin`, look for `getAssetStore()` or `Interaction.getAssetStore()` patterns. If the API requires `loadAssets(List.of(...))` instead of `register(name, class, codec)`, adapt. Document which API was used in a code comment. |
| `getCodecRegistry` exists but `register` signature differs | Match the available `register` signature exactly. Search HytaleServer.jar for `CodecRegistry.class` and dump its method list. |
| `InteractionContext.getEntity()` returns a non-Player ref | Reflectively unwrap. Look for `getOwner()`, `getCaster()`, or similar method via `interactionContext.getClass().getMethods()` in a one-time debug log. Use whichever returns a `Player`. |

| Runtime symptom | Action |
| --- | --- |
| Zero `Custom spellbook interaction fired` log lines on any click | The JSON Interactions block didn't bind. Check Hytale server log for `Unknown interaction type` or `Failed to deserialize interaction`. Most likely the slot key casing differs — try uppercase keys: `"PRIMARY"`, `"SECONDARY"`, `"USE"` in the JSON. Rebuild. If still nothing, double-check `Type` strings match exactly the `register(...)` IDs (case-sensitive). |
| Logs show interaction fired but `Spellbook cast attempt` line missing | `castSpellbookSlotFromInteraction` returned early. Check log for player-data resolution failure. Re-run `/motm style quake` to confirm save state. |
| Spellbook still feels like a block placer when right-clicking | The new JSON didn't ship. Extract jar and confirm `Server/Item/Items/MOTM_Spellbook_Focus.json` contents match Step 2.4. If yes, Hytale may cache the old item JSON — fully exit, delete `%LOCALAPPDATA%/Hytale/cache/*` if such a path exists, relaunch. |
| `Use` triggers `slot=3` but no ability fires | `MotmCommand.castAbilityBySlot` may not yet route slot 3 to ability index 2 in the style. Read `MotmCommand.java`'s `castAbilityBySlot` and verify it uses `slot - 1` against `style.getAbilities()`. If hard-coded for slots 1/2 only, add slot 3. |

---

## Phase 3 — Lifecycle split

### Goal
`PlayerConnectEvent` owns data + runtime + spellbook restore. `PlayerReadyEvent` owns HUD only. Strip duplicate `onPlayerJoin` calls.

### Files
- `src/main/java/com/motm/MenteesMod.java` (`onPlayerConnect`, `onPlayerReady`, delete `initializeRuntimePlayer`)

### Step 3.1 — Rewrite `onPlayerConnect`
Replace the existing `onPlayerConnect(Player runtimePlayer)` body (~lines 416-440) with:

```java
public void onPlayerConnect(Player runtimePlayer) {
    LOG.info("[MOTM] >>> onPlayerConnect: " + runtimePlayer);
    var playerRef = getUniversePlayerRef(runtimePlayer);
    if (playerRef == null || playerRef.getUuid() == null) {
        return;
    }
    String playerId = playerRef.getUuid().toString();
    long t0 = System.currentTimeMillis();

    onlineRuntimePlayers.put(playerId, runtimePlayer);
    if (initializedRuntimePlayers.add(playerId) || playerDataManager.getOnlinePlayer(playerId) == null) {
        onPlayerJoin(playerId, playerRef.getUsername());
    }

    var playerData = playerDataManager.getOnlinePlayer(playerId);
    boolean hasSavedLoadout = playerData != null
            && playerData.getPlayerClass() != null
            && playerData.getSelectedStyles() != null
            && !playerData.getSelectedStyles().isEmpty();
    LOG.info("[MOTM] onPlayerConnect hasSavedLoadout=" + hasSavedLoadout + " playerId=" + playerId);

    if (hasSavedLoadout) {
        rebuildPlayerRuntimeNow(playerData);
        boolean ensured = ensureSpellbookItem(runtimePlayer);
        LOG.info("[MOTM] onPlayerConnect ensureSpellbookItem=" + ensured
                + " hasSpellbook=" + playerHasSpellbook(runtimePlayer));
        if (!ensured && !playerHasSpellbook(runtimePlayer)) {
            queueSpellbookGrant(playerId);
        }
        refreshPlayerProgressionBonuses(playerId);
    }

    LOG.info("[MOTM] onPlayerConnect done dt=" + (System.currentTimeMillis() - t0)
            + "ms playerId=" + playerId);
}
```

### Step 3.2 — Rewrite `onPlayerReady`
Replace `onPlayerReady` (~lines 442-445) with:

```java
public void onPlayerReady(Player runtimePlayer) {
    LOG.info("[MOTM] >>> onPlayerReady: " + runtimePlayer);
    var playerRef = getUniversePlayerRef(runtimePlayer);
    if (playerRef == null || playerRef.getUuid() == null) {
        return;
    }
    String playerId = playerRef.getUuid().toString();
    onlineRuntimePlayers.put(playerId, runtimePlayer);

    if (isDevToolsEnabled()) {
        statusEffectManager.clearEffects(playerId);
        elementalReactionManager.clearMarks(playerId);
        setFreeCastEnabled(playerId, true);
        if (!ensureSpellbookItem(runtimePlayer)) {
            queueSpellbookGrant(playerId);
        }
    }

    queueStatusHudInstall(playerId);
    LOG.info("[MOTM] onPlayerReady done playerId=" + playerId);
}
```

### Step 3.3 — Delete `initializeRuntimePlayer`
Delete the entire `initializeRuntimePlayer` method (currently ~lines 447-492). It's no longer called. **Do not leave a stub.**

### Acceptance gate
Fresh launch, log in to a world with a previously-saved Terra/Quake loadout. In `logs/<latest>_server.log`, in order, **for a single playerId**:
1. `[MOTM] >>> onPlayerConnect: ...`
2. `[MOTM] onPlayerConnect hasSavedLoadout=true ...`
3. `[MOTM] onPlayerConnect ensureSpellbookItem=...`
4. `[MOTM] onPlayerConnect done dt=Xms ...` — **X < 500ms**
5. `[MOTM] >>> onPlayerReady: ...`
6. `[MOTM] onPlayerReady done ...`
7. Later: `[MOTM] Installing HUD: ...`

The string `>>> onPlayerJoin` must appear **at most once** per playerId (it's called from inside `onPlayerConnect`'s `initializedRuntimePlayers.add(...)` branch on first connect of the session).

### Decision table — Phase 3 forks
| Symptom | Action |
| --- | --- |
| Compile error: `rebuildPlayerRuntimeNow` private | Same class — direct call works. If lint flags it, ignore and build. |
| Build OK but player joins with class/style and no spellbook | `ensureSpellbookItem` returned false because `entityRef` wasn't valid at connect time. The `queueSpellbookGrant` fallback handles this — verify a follow-up `[MOTM] Processing pending spellbook grant for ...` line within ~1s. |
| HUD installs but no spellbook lasting >5s | `pendingSpellbookGrants` is being processed but somehow failing. Add diagnostic log at the grant-processing site. Out of scope to root-cause in this phase. |

---

## Phase 4 — Asset pack expansion workflow (reference, not implementation)

> This phase is reference material for the human user and for future expansion. Codex should not modify files in this phase; treat it as documentation that you, Codex, can consult when later phases ask you to add custom assets.

### The four kinds of assets in this mod
1. **Vanilla-referenced assets** (already used heavily via `HytaleAssetResolver`): you reference an existing Hytale asset by its full path string (e.g. `"Common/Characters/Animations/Default/Interact.blockyanim"`). No file ships in our jar.
2. **Custom MOTM EntityEffect JSONs** (already present): live in `src/main/resources/Server/Entity/Effects/MOTM/*.json`. These compose vanilla particles + sounds into named effect configs we trigger from `applyEffectById(...)`.
3. **Custom MOTM UI documents** (partially present): `.ui` files in `src/main/resources/Common/UI/Custom/`. The Status HUD has one (`MOTM_StatusHud.ui`); the Spellbook page is missing (`Pages/MOTM_Spellbook.ui` — Phase 6 creates it).
4. **Brand-new bespoke models / animations / textures / particles** (none yet, but possible). For these you need Blockbench + the Hytale plugin, plus the in-game Asset Editor for particle/VFX work.

### Discovering vanilla asset IDs
**Easiest path:** install [Effect Showcase](https://www.curseforge.com/hytale/mods/effect-showcase) (v1.0.2, drop into `mods/` folder).
- `/showcase particles` — cycles through every vanilla particle system in front of you with a name label
- `/showcase effects` — same for entity effects
- `/showcase dump` — exports a full sorted list of all vanilla asset IDs to a text file
This is the only practical way to discover the exact string IDs you need.

**Inside the game:** press **B** to open Creative Tools Hub → Assets → Asset Editor. Three-dots menu → "Add Pack" creates a new editable pack. You can browse the full vanilla directory tree (`Audio`, `Camera`, `Entity`, `Environment`, `Item`, `Particles`, `GameplayConfig`) and use the **"Copy Asset"** workflow to duplicate any vanilla asset into your pack for modification.

### Folder layout for our pack (already correct)
```
src/main/resources/
├── manifest.json                      # IncludesAssetPack=true; loaded by AssetModule
├── Common/
│   └── UI/Custom/
│       ├── HUD/
│       │   └── MOTM_StatusHud.ui
│       └── Pages/
│           └── MOTM_Spellbook.ui      # PHASE 6 CREATES THIS
└── Server/
    ├── Entity/Effects/MOTM/           # 31 custom MOTM effects already authored
    └── Item/Items/
        └── MOTM_Spellbook_Focus.json
```

### Creating bespoke models (Blockbench)
Reference: [Hytale Blockbench Plugin](https://github.com/JannisX11/hytale-blockbench-plugin), [official guide](https://hytale.game/en/blockbench-and-asset-creation/).
1. Install Blockbench, then install the Hytale plugin from Blockbench's plugin menu.
2. New project → "Hytale Model" template.
3. Build with cuboids only (no triangles, edge loops, or spheres — Hytale models are voxel-style).
4. Texture, then export as `.blockymodel` to `src/main/resources/Common/Models/<Category>/<YourModel>.blockymodel`.
5. Animate in the same project using keyframe animation editor; export as `.blockyanim` to `src/main/resources/Common/Animations/<Category>/<YourAnim>.blockyanim`.
6. Reference from item JSON: `"Model": "Common/Models/<Category>/<YourModel>.blockymodel"`.
7. Reference from `HytaleAssetResolver.java`: add a `private static final String ANIM_MY_NEW = "Common/Animations/<Category>/<YourAnim>.blockyanim";` constant and wire it in the resolver's switch.

### Creating bespoke particles (in-game)
1. Open Creative Tools Hub → Assets → Asset Editor → your pack.
2. Right-click `Server/Particles/` → New → Particle Spawner.
3. The editor exposes: emission rate, velocity, lifetime, texture, color animation, gravity/wind, collision, attachment.
4. Save; the file lands in `<your-pack>/Server/Particles/<Name>.particlespawner`.
5. Optionally compose multiple spawners into a `.particlesystem` (same editor, New → Particle System).
6. Once you're happy, export the pack from the in-game editor; copy the new files into `src/main/resources/Server/Particles/MOTM/` so the gradle build packs them.
7. Reference from a `MOTM_*.json` EntityEffect, or directly from `HytaleAssetResolver`.

### Creating bespoke VFX (in-game)
The in-game VFX editor layers particles + light sources + sound effects into a node-based effect. Saved files use the same `.particlesystem` extension and ship the same way.

### Creating bespoke UI documents (`.ui` files)
Hytale `.ui` files are server-shipped XML-like documents that the client renders. Reference: `src/main/resources/Common/UI/Custom/HUD/MOTM_StatusHud.ui` — open it to see the structure (already authored).
The Hytale Asset Editor does not yet have a built-in UI document editor as of EA. The current authoring path is hand-edit the XML, then test in-game with hot reload (Apply Changes button in the in-game Asset Editor).

### Sound IDs
Sounds are referenced by string ID like `"ISS_Weapons_Books"` (item sound set, see the spellbook JSON). Discover IDs via Effect Showcase's `/showcase dump` or by inspecting vanilla item JSONs through the in-game asset editor's Copy Asset workflow.

### When to use vanilla vs. bespoke
- **Use vanilla** for prototypes and the 95% case. `HytaleAssetResolver` already does this.
- **Use bespoke MOTM EntityEffect JSON** when you want a named, reusable composition (already done for 31 effects). Surgical edits welcome; structural changes need an isolated test pass.
- **Use bespoke models / particles** only when an ability's identity genuinely requires it (a unique class signature, a transformation, an iconic spell). Defer until Phase 9 if at all.

---

## Phase 5 — Quake vertical-slice acceptance

### Goal
Two consecutive cold launches must both produce the complete slice without manual `/motm cast`.

### Test script (run twice — both must pass)

1. Kill any running Hytale server. (Close the game window or Ctrl+C the server console.)
2. Launch Hytale, enter a singleplayer creative world (or your usual test world).
3. Open chat. Run, in order:
   - `/motm class terra` (skip if already set)
   - `/motm race human` (skip if already set; race id doesn't matter for this test)
   - `/motm style quake` (skip if already set)
4. Verify spellbook is in inventory. If not, run `/motm spellbook overview` to queue a grant; relog if needed.
5. Equip the Mentees Spellbook into your hand.
6. **Left Click once.** Within 1 second observe:
   - Visible: a shockwave AoE around you, knockback on any test target dummies nearby.
   - In log: `[MOTM] Custom spellbook interaction fired: type=Primary slot=1 ...` AND `[MOTM] Queue ability cast: ... abilityId=stomp ...`
7. Wait 2 seconds (Stomp cooldown).
8. **Right Click once.** Observe:
   - Visible: a ground zone of ~5 block radius, particle field for 4 seconds, any in-zone targets slowed/disoriented.
   - In log: `abilityId=aftershock`
9. Wait 5 seconds (Aftershock cooldown).
10. **Use (default key E) once** while looking at a target up to 10 blocks away. Observe:
    - Visible: target buried briefly, suffocation tick damage, ~3 block radius ground disturbance.
    - In log: `abilityId=sinkhole`
11. Stand idle 10 seconds. **No HP loss. No movement slowdown.**
12. Re-equip Stomp's slot (LMB) once more to confirm cooldown messages appear correctly if pressed before 2s.

### Acceptance gate
Both runs must pass. If either fails, do **not** proceed to Phase 9. Use the decision table.

### Decision table — Phase 5 forks
| Symptom | Action |
| --- | --- |
| Click registers in log but no visible AoE/projectile | Issue is in `GameplayPlaybackManager.executeAbility` for that ability's `cast_type`. Check `HytaleAssetResolver` mapping for the class+style+ability. If the resolver returns `empty()`, fall through to a debug log. Out of scope to fix here; record as a follow-up. |
| One slot fires, others don't | JSON slot key for the silent ones is wrong (casing). Try Phase 2's decision-table escalation. |
| Idle HP loss returns | Leftover Block_Secondary binding survives somewhere. Re-extract jar, grep contents for `Block_Secondary`. Could also be a vanilla weapon swing colliding with environment — verify the spellbook's `Model` reference renders correctly (it should be a book held in hand, not a weapon). |
| Spellbook removed from inventory after a cast | Check `MaxStack: 1` is still in the JSON. Custom interactions shouldn't consume the item but a stack of 0 can be misread. |
| All three slots fire correctly but no log line for `Queue ability cast` | `tryCastSpellbookSlot` is early-returning. Most likely cause: cooldown still active. Wait the full cooldown shown in `/motm abilities` and retry. |

---

## Phase 6 — Custom Spellbook UI page

### Goal
Create the missing `Pages/MOTM_Spellbook.ui` file so `SpellbookPage.java` can open a real UI on crouch-Use instead of falling back to chat rendering.

### Files
- `src/main/resources/Common/UI/Custom/Pages/MOTM_Spellbook.ui` *(new)*

### Step 6.1 — Read the existing HUD ui as a template

Open `src/main/resources/Common/UI/Custom/HUD/MOTM_StatusHud.ui`. Note its structure (root tag, panels, text elements, image references). Use that exact same dialect for the spellbook page.

### Step 6.2 — Build a minimal-but-complete spellbook UI

Create `src/main/resources/Common/UI/Custom/Pages/MOTM_Spellbook.ui` with these required elements (matching the bindings `SpellbookPage.java` already declares):

**Navigation row** — buttons that fire `Navigate` events with `section` field set to:
- `overview`, `journey`, `grimoire`, `perks`, `resources`, `codex`, `journal`

**Class selector** — 4 buttons (Terra, Hydro, Aero, Corruptus) firing `ChooseClass` events with `value` set to the class id.

**Race selector** — up to 12 buttons firing `ChooseRaceSlot` events with `value = <race index>`.

**Style selector** — up to 10 buttons firing `ChooseStyleSlot` events with `value = <style index>` for the current class.

**Perk selector** — up to 10 toggle buttons (matches `MAX_PERK_ROWS=10`) firing `TogglePerkSlot` events with `value = <perk index>`.

**Ability rows** — 3 ability cards (matches `MAX_ABILITY_ROWS=3`) showing name + description + cooldown text. No event binding required.

**Section text panel** — a large multi-line text component bound to the `${section_body}` variable so the Java side can `commands.setText("section_body", ...)`.

**Status message line** — a single-line text bound to `${status_message}`.

**Pseudocode skeleton** (translate to the actual `.ui` XML dialect by reading `MOTM_StatusHud.ui` for syntax):
```xml
<page id="motm_spellbook">
  <panel id="root">
    <panel id="nav">
      <button id="nav_overview" event="Navigate" section="overview">Overview</button>
      <button id="nav_journey" event="Navigate" section="journey">Journey</button>
      <button id="nav_grimoire" event="Navigate" section="grimoire">Grimoire</button>
      <button id="nav_perks" event="Navigate" section="perks">Perks</button>
      <button id="nav_resources" event="Navigate" section="resources">Resources</button>
      <button id="nav_codex" event="Navigate" section="codex">Codex</button>
      <button id="nav_journal" event="Navigate" section="journal">Journal</button>
    </panel>
    <panel id="class_row">
      <button event="ChooseClass" value="terra">Terra</button>
      <button event="ChooseClass" value="hydro">Hydro</button>
      <button event="ChooseClass" value="aero">Aero</button>
      <button event="ChooseClass" value="corruptus">Corruptus</button>
    </panel>
    <panel id="race_row">
      <!-- 12 buttons, indexed 0..11 -->
      <button event="ChooseRaceSlot" value="0" id="race_0">Race 0</button>
      <!-- repeat through 11 -->
    </panel>
    <panel id="style_row">
      <!-- 10 buttons, indexed 0..9 -->
      <button event="ChooseStyleSlot" value="0" id="style_0">Style 0</button>
      <!-- repeat through 9 -->
    </panel>
    <panel id="perk_row">
      <!-- 10 toggles, indexed 0..9 -->
      <toggle event="TogglePerkSlot" value="0" id="perk_0">Perk 0</toggle>
      <!-- repeat through 9 -->
    </panel>
    <panel id="abilities">
      <text id="ability_0_name">${ability_0_name}</text>
      <text id="ability_0_desc">${ability_0_desc}</text>
      <text id="ability_1_name">${ability_1_name}</text>
      <text id="ability_1_desc">${ability_1_desc}</text>
      <text id="ability_2_name">${ability_2_name}</text>
      <text id="ability_2_desc">${ability_2_desc}</text>
    </panel>
    <text id="section_body" multiline="true">${section_body}</text>
    <text id="status_message">${status_message}</text>
  </panel>
</page>
```

The actual dialect is whatever `MOTM_StatusHud.ui` uses. Mirror it exactly — element names (`page` vs `Document`, `button` vs `Button`, etc.), attribute casing, and the variable substitution syntax (`${...}` vs `{0}` vs `<bind/>`).

### Step 6.3 — Confirm `SpellbookPage.java` binds match

Open `src/main/java/com/motm/ui/SpellbookPage.java`. Scan the `bindNavigation`, `bindJourneyActions`, `bindGrimoireActions`, `bindPerkActions` methods. Each declares which event names it expects and which fields. The `.ui` file's `event="..."` and `value="..."` attributes must match those bindings exactly. **Do not modify `SpellbookPage.java`** in this phase — fit the UI to the existing bindings.

### Step 6.4 — Verify the page loads

Build + relaunch. In game, hold the spellbook, crouch + Use. Expect:
- The custom UI page appears (whatever the dialect renders looks like).
- Closing it with Esc dismisses cleanly.
- Clicking nav buttons fires `[MOTM] Spellbook page event: Navigate section=...` logs (or whatever logs `handleDataEvent` writes).

### Acceptance gate
- Crouch + Use opens the custom UI page (not the chat fallback).
- All seven nav buttons render and respond to clicks.
- The ability rows display the currently-selected style's three abilities.

### Decision table — Phase 6 forks
| Symptom | Action |
| --- | --- |
| `openSpellbook` returns false (no UI) | `CUSTOM_PAGE_UI_ENABLED` may be false somewhere. Search `MenteesMod.java` for the constant; set to true. |
| UI opens but is blank / no content | Variable substitution syntax wrong. Check `MOTM_StatusHud.ui` for the exact syntax it uses for `${...}`-style binds. Mirror exactly. |
| UI opens with content but buttons do nothing | Event/value attribute names don't match what `SpellbookPage.bindNavigation()` etc. register. Read those methods and align the `.ui` attributes. |
| Cosmetic only (alignment, font, color is ugly) | Acceptable. Polish is out of scope for this phase. |

---

## Phase 7 — Perk effect integration

### Goal
Wire `PerkManager.applyPerkEffects` to actually mutate player stats / register triggers. Currently it logs only — 800 perks are dead-weight.

### Files
- `src/main/java/com/motm/manager/PerkManager.java`
- `src/main/java/com/motm/model/Perk.java` (read only — to understand `Perk.Effect` shape)
- `src/main/java/com/motm/manager/PlayerStatModifierManager.java` *(new — see Step 7.2)*
- `src/main/java/com/motm/MenteesMod.java` (add wiring)

### Step 7.1 — Audit existing perk effect types

From `PerkManager.applyPerkEffects` comments, the effect types are:
- `stat_increase` — flat +X to a named stat
- `stat_multiplier` — *X to a named stat
- `damage_increase` — % bonus to outgoing damage (global or filtered by tag)
- `damage_reduction` — % reduction to incoming damage
- `ability` — grants a passive secondary ability (rare; treat as out-of-scope and log-only for now)
- `summon` — passive summon (rare; log-only for now)
- `passive` — generic flag a system can read (e.g. "double_jump_unlocked"); just set a key in `PlayerData.passiveFlags`
- `on_hit` — registers a trigger to run when player attacks
- `on_kill` — registers a trigger to run when player kills a mob (hook into existing `MenteesMod.onMobKilled`)
- `aura` — passive AoE buff/debuff; log-only for now
- `transformation` — log-only for now
- `conditional_buff` — log-only for now
- `immunity` — adds an immunity tag to `PlayerData`

Open `Perk.java` and `Perk.Effect.java` to confirm the field names actually used (e.g. `getType()`, `getValue()`, `getTargetStat()`, `getTag()`, `getCondition()`). The plan below uses `effect.getType()` / `effect.getValue()` / `effect.getTargetStat()` — if the real names differ, substitute.

### Step 7.2 — Create `PlayerStatModifierManager`

This new manager owns the cumulative stat picture per player so we can rebuild it deterministically when perks change.

Create `src/main/java/com/motm/manager/PlayerStatModifierManager.java`:

```java
package com.motm.manager;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap;
import com.hypixel.hytale.server.core.modules.entitystats.EntityStatType;
import com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.Modifier;
import com.hypixel.hytale.server.core.modules.entitystats.modifier.StaticModifier;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.motm.MenteesMod;
import com.motm.model.Perk;
import com.motm.model.PlayerData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Maintains the cumulative stat picture from MOTM perks on each player.
 * Owns Hytale EntityStatMap modifier IDs prefixed "motm_perk_".
 * On reapply: clears all owned modifiers, then re-adds based on player's current perks.
 */
public class PlayerStatModifierManager {

    private static final Logger LOG = Logger.getLogger("MOTM");
    private static final String MODIFIER_PREFIX = "motm_perk_";

    private final MenteesMod mod;
    /** Track modifier IDs added per player so we can clear them on rebuild. */
    private final Map<String, Set<String>> ownedModifiers = new HashMap<>();

    public PlayerStatModifierManager(MenteesMod mod) {
        this.mod = mod;
    }

    public void rebuildFromPerks(PlayerData playerData, List<Perk> perks) {
        if (playerData == null) {
            return;
        }
        String playerId = playerData.getPlayerId();
        Player runtimePlayer = mod.getRuntimePlayer(playerId);
        if (runtimePlayer == null) {
            return; // Will be retried on next progression refresh.
        }
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            return;
        }
        EntityStatMap statMap = playerRef.getStore().getComponent(playerRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            return;
        }

        clearOwnedModifiers(playerId, statMap);

        if (perks == null || perks.isEmpty()) {
            return;
        }

        int applied = 0;
        for (Perk perk : perks) {
            if (perk == null || perk.getEffects() == null) {
                continue;
            }
            for (Perk.Effect effect : perk.getEffects()) {
                if (applyOneEffect(playerId, statMap, perk, effect)) {
                    applied++;
                }
            }
        }
        LOG.info("[MOTM] Rebuilt perk stat modifiers: player=" + playerData.getPlayerName()
                + " perks=" + perks.size() + " modifiersApplied=" + applied);
    }

    public void clearForPlayer(String playerId) {
        Player runtimePlayer = mod.getRuntimePlayer(playerId);
        if (runtimePlayer == null) {
            ownedModifiers.remove(playerId);
            return;
        }
        Ref<EntityStore> playerRef = runtimePlayer.getReference();
        if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) {
            ownedModifiers.remove(playerId);
            return;
        }
        EntityStatMap statMap = playerRef.getStore().getComponent(playerRef, EntityStatMap.getComponentType());
        if (statMap == null) {
            ownedModifiers.remove(playerId);
            return;
        }
        clearOwnedModifiers(playerId, statMap);
    }

    private void clearOwnedModifiers(String playerId, EntityStatMap statMap) {
        Set<String> existing = ownedModifiers.remove(playerId);
        if (existing == null) {
            return;
        }
        for (String modId : existing) {
            // The stat type the modifier belongs to was encoded in modId after the prefix.
            EntityStatType<?> statType = decodeStatTypeFromModId(modId);
            if (statType != null) {
                statMap.removeModifier(statType, modId);
            }
        }
    }

    private boolean applyOneEffect(String playerId, EntityStatMap statMap, Perk perk, Perk.Effect effect) {
        if (effect == null || effect.getType() == null) {
            return false;
        }
        String effectType = effect.getType().toLowerCase(Locale.ROOT);
        switch (effectType) {
            case "stat_increase":
                return applyStat(playerId, statMap, perk, effect, Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.ADDITIVE);
            case "stat_multiplier":
                return applyStat(playerId, statMap, perk, effect, Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.MULTIPLICATIVE);
            case "damage_reduction":
                // Map to incoming-damage stat if Hytale exposes one. For Early Access we proxy
                // through max-health uplift as a fallback. Log so we can revisit.
                LOG.fine("[MOTM] perk effect damage_reduction proxied to health: perk=" + perk.getId());
                return applyHealthProxy(playerId, statMap, perk, effect.getValue());
            case "damage_increase":
                // Strength scaling: prefer DefaultEntityStatTypes.getAttack() if available.
                EntityStatType<?> attackStat = tryGetStatType("attack");
                if (attackStat == null) {
                    LOG.fine("[MOTM] perk effect damage_increase skipped: no attack stat type");
                    return false;
                }
                String modId = encodeModId(perk.getId(), effect.getType(), "attack");
                statMap.putModifier(attackStat, modId,
                        new StaticModifier(Modifier.ModifierTarget.MAX,
                                StaticModifier.CalculationType.MULTIPLICATIVE,
                                (float) (1.0 + effect.getValue())));
                rememberOwned(playerId, modId);
                return true;
            case "passive":
            case "immunity":
                // Flag-style: cache in PlayerData passive flags; nothing to do at stat layer.
                return false;
            case "on_hit":
            case "on_kill":
                // Register triggers via mod.getPerkTriggerRegistry().register(...)
                // — see Step 7.3.
                mod.registerPerkTrigger(playerId, perk, effect);
                return true;
            default:
                LOG.fine("[MOTM] perk effect type not yet wired: " + effectType
                        + " (perk=" + perk.getId() + ")");
                return false;
        }
    }

    private boolean applyStat(String playerId, EntityStatMap statMap, Perk perk, Perk.Effect effect,
                              Modifier.ModifierTarget target, StaticModifier.CalculationType calc) {
        String statName = normalize(effect.getTargetStat());
        if (statName == null) {
            return false;
        }
        EntityStatType<?> statType = tryGetStatType(statName);
        if (statType == null) {
            LOG.fine("[MOTM] perk effect skipped: unknown stat '" + statName
                    + "' (perk=" + perk.getId() + ")");
            return false;
        }
        String modId = encodeModId(perk.getId(), effect.getType(), statName);
        float value = (float) effect.getValue();
        if (calc == StaticModifier.CalculationType.MULTIPLICATIVE) {
            value = 1.0f + value; // 0.10 means +10% multiplier
        }
        statMap.putModifier(statType, modId, new StaticModifier(target, calc, value));
        rememberOwned(playerId, modId);
        return true;
    }

    private boolean applyHealthProxy(String playerId, EntityStatMap statMap, Perk perk, double value) {
        EntityStatType<?> healthType = DefaultEntityStatTypes.getHealth();
        if (healthType == null) {
            return false;
        }
        String modId = encodeModId(perk.getId(), "damage_reduction", "health");
        statMap.putModifier(healthType, modId,
                new StaticModifier(Modifier.ModifierTarget.MAX,
                        StaticModifier.CalculationType.MULTIPLICATIVE,
                        1.0f + (float) value));
        rememberOwned(playerId, modId);
        return true;
    }

    private EntityStatType<?> tryGetStatType(String statName) {
        switch (statName) {
            case "health": return DefaultEntityStatTypes.getHealth();
            case "mana":
                // Not a vanilla stat — would need a custom EntityStatType registered separately.
                return null;
            case "stamina":
                // Same — not a vanilla stat at the EntityStatMap layer.
                return null;
            default:
                return null;
        }
    }

    private EntityStatType<?> decodeStatTypeFromModId(String modId) {
        int lastColon = modId.lastIndexOf(':');
        if (lastColon < 0) return null;
        String statName = modId.substring(lastColon + 1);
        return tryGetStatType(statName);
    }

    private String encodeModId(String perkId, String effectType, String statName) {
        return MODIFIER_PREFIX + perkId + ":" + effectType + ":" + statName;
    }

    private void rememberOwned(String playerId, String modId) {
        ownedModifiers.computeIfAbsent(playerId, id -> new HashSet<>()).add(modId);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }
}
```

### Step 7.3 — Add a simple perk trigger registry to `MenteesMod`

For `on_hit` / `on_kill` effects, we need a place to register and fire triggers. Add this minimal scaffold to `MenteesMod.java`:

```java
private final java.util.Map<String, java.util.List<com.motm.model.PerkTriggerBinding>> perkTriggersByPlayer
        = new java.util.concurrent.ConcurrentHashMap<>();

public void registerPerkTrigger(String playerId, com.motm.model.Perk perk, com.motm.model.Perk.Effect effect) {
    if (playerId == null || perk == null || effect == null) return;
    perkTriggersByPlayer.computeIfAbsent(playerId, id -> new java.util.concurrent.CopyOnWriteArrayList<>())
            .add(new com.motm.model.PerkTriggerBinding(perk.getId(), effect.getType(), effect.getValue()));
}

public void clearPerkTriggers(String playerId) {
    perkTriggersByPlayer.remove(playerId);
}

public java.util.List<com.motm.model.PerkTriggerBinding> getPerkTriggers(String playerId, String type) {
    var all = perkTriggersByPlayer.get(playerId);
    if (all == null || all.isEmpty()) return java.util.List.of();
    return all.stream().filter(b -> type.equalsIgnoreCase(b.effectType())).toList();
}
```

Then in `onPlayerDisconnect` (already exists), add a line:
```java
clearPerkTriggers(playerId);
```

And in `onMobKilled` (also exists), after the existing logic, add:
```java
for (var trigger : getPerkTriggers(playerId, "on_kill")) {
    LOG.fine("[MOTM] perk on_kill trigger: perk=" + trigger.perkId() + " value=" + trigger.value());
    // Heal-on-kill default behavior; richer triggers can be added later.
    // For now: heal the player for trigger.value() * player's max HP.
    Player runtimePlayer = onlineRuntimePlayers.get(playerId);
    if (runtimePlayer != null) {
        applyHealFraction(runtimePlayer, trigger.value());
    }
}
```

`applyHealFraction` is a small helper — add it near other player-helper methods in `MenteesMod`:
```java
private void applyHealFraction(Player runtimePlayer, double fraction) {
    if (runtimePlayer == null || fraction <= 0) return;
    var playerRef = runtimePlayer.getReference();
    if (playerRef == null || !playerRef.isValid() || playerRef.getStore() == null) return;
    var statMap = playerRef.getStore().getComponent(playerRef,
            com.hypixel.hytale.server.core.modules.entitystats.EntityStatMap.getComponentType());
    if (statMap == null) return;
    var health = statMap.get(com.hypixel.hytale.server.core.modules.entitystats.asset.DefaultEntityStatTypes.getHealth());
    if (health == null) return;
    float current = health.get();
    float max = health.getMax();
    float restored = Math.min(max, current + (float) (max * fraction));
    health.set(restored);
}
```

`on_hit` triggers can be wired similarly later — they need a `DamageEvent` hook which is out of scope for this phase. Log them and move on.

### Step 7.4 — Create the `PerkTriggerBinding` record

Create `src/main/java/com/motm/model/PerkTriggerBinding.java`:
```java
package com.motm.model;

public record PerkTriggerBinding(String perkId, String effectType, double value) {}
```

### Step 7.5 — Wire `PerkManager` to call the new manager

In `PerkManager.java`:

1. Add a constructor parameter `PlayerStatModifierManager statModifiers`. Update the field list, constructor, and (in `MenteesMod.setup()`) the instantiation: add a `PlayerStatModifierManager` instance and pass it.

2. Replace the body of `applyPerkEffects(PlayerData player, Perk perk)`:
```java
public void applyPerkEffects(PlayerData player, Perk perk) {
    // No-op individual application — we rebuild from the full perk list on each change
    // to keep the modifier set deterministic. See applyAllOwnedPerks().
}
```

3. Add a new method:
```java
public void applyAllOwnedPerks(PlayerData player) {
    if (player == null) return;
    List<Perk> perks = new ArrayList<>();
    for (String perkId : player.getSelectedPerks()) {
        Perk perk = dataLoader.getPerkById(perkId, player.getPlayerClass());
        if (perk != null) {
            perks.add(perk);
        }
    }
    statModifiers.rebuildFromPerks(player, perks);
}
```

4. Change `reapplyAllPerks` body to:
```java
public void reapplyAllPerks(PlayerData player, SynergyEngine synergyEngine) {
    applyAllOwnedPerks(player);
    synergyEngine.recalculateSynergies(player);
}
```

5. In `applyPerkSelection`, after the `for (String perkId : selectedPerkIds)` loop, add:
```java
applyAllOwnedPerks(player);
```

### Step 7.6 — Add `getRuntimePlayer` and use existing wiring

`PlayerStatModifierManager` calls `mod.getRuntimePlayer(playerId)`. Verify `MenteesMod` has that method — it should, since other managers use it. If not, add a one-liner:
```java
public Player getRuntimePlayer(String playerId) {
    return onlineRuntimePlayers.get(playerId);
}
```

### Acceptance gate
1. Build cleanly.
2. Log into a world with a Terra player at level 5+ that has chosen at least one perk (use `/motm select` from existing commands).
3. Run `/motm spellbook journey` or `/motm perks` — confirm the perk shows as selected in `PlayerData`.
4. **In log**, on selection: `[MOTM] Rebuilt perk stat modifiers: player=... perks=N modifiersApplied=K`.
5. Check max HP via `/motm` or in-game HP bar **before and after** a known stat_increase perk on health. The number must visibly differ.
6. Kill a test mob with an `on_kill` perk active. Confirm `[MOTM] perk on_kill trigger:` log and a small heal.

### Decision table — Phase 7 forks
| Symptom | Action |
| --- | --- |
| `EntityStatMap.putModifier(...)` signature differs | Inspect `EntityStatMap.class` via `jar xf HytaleServer.jar; javap EntityStatMap.class`. Update calls to match the real method. |
| `DefaultEntityStatTypes.getAttack()` doesn't exist | Skip damage_increase wiring; treat as log-only for this phase. Document in code comment. |
| Mana / stamina aren't first-class stats | They aren't, at the entity layer. Treat them as MOTM-managed `PlayerData` fields (already handled in `PlayerData`) — perks that target mana/stamina remain log-only in this phase. |
| `Perk.Effect.getTargetStat()` method missing | Read `Perk.java`. The field may be `getStat()`, `getAttribute()`, or stored in a `params` map. Adapt the code to the real API. |
| Modifiers stick across re-logins (cumulative) | `clearOwnedModifiers` not called. Verify `onPlayerDisconnect` runs `statModifiers.clearForPlayer(playerId)`. Add the call. |

---

## Phase 8 — PlayerInteractLib fallback (optional)

### Goal
A defensive safety net. If Phase 2's custom interactions ever stop firing for a build of Hytale where the API shifts, the lib's `PlayerInteractionEvent` provides a working alternate path. **Only do this phase if Phase 5 has passed twice.**

### Files
- `src/main/resources/manifest.json`
- `src/main/java/com/motm/MenteesMod.java`
- `mods/` *(user-provided jar)*

### Step 8.1 — Declare dependency
In `src/main/resources/manifest.json`, change `Dependencies` from `{}` to:
```json
"Dependencies": {
  "PlayerInteractLib": "1.0.1"
}
```

### Step 8.2 — Subscribe with reflection (graceful if absent)

Add to `MenteesMod.java`, called from inside `start()` after `registerHytaleHooks()`:
```java
private void trySubscribePlayerInteractLib() {
    try {
        Class<?> libClass = Class.forName("net.fancyinnovations.playerinteractlib.PlayerInteractLib");
        Object publisher = libClass.getMethod("getInteractionPublisher").invoke(null);
        java.lang.reflect.Method subscribe = publisher.getClass()
                .getMethod("subscribe", java.util.concurrent.Flow.Subscriber.class);
        subscribe.invoke(publisher, new java.util.concurrent.Flow.Subscriber<Object>() {
            @Override public void onSubscribe(java.util.concurrent.Flow.Subscription s) { s.request(Long.MAX_VALUE); }
            @Override public void onNext(Object event) { /* no-op until needed */ }
            @Override public void onError(Throwable t) { LOG.warning("[MOTM] PlayerInteractLib stream error: " + t.getMessage()); }
            @Override public void onComplete() {}
        });
        LOG.info("[MOTM] PlayerInteractLib fallback subscribed");
    } catch (ClassNotFoundException e) {
        LOG.info("[MOTM] PlayerInteractLib not present — primary interaction path is custom codecs");
    } catch (Throwable t) {
        LOG.warning("[MOTM] PlayerInteractLib subscription failed: " + t.getMessage());
    }
}
```

The actual class name (`net.fancyinnovations.playerinteractlib.PlayerInteractLib`) is a guess. If the user has dropped the jar into `mods/`, run:
```powershell
& "$env:JAVA_HOME\bin\jar.exe" tf "$env:APPDATA\Hytale\UserData\Mods\PlayerInteractLib-1.0.1.jar" | Select-String -Pattern "PlayerInteractLib|Publisher"
```
Use the real class name from output. **Do not commit reflective access to a wrong path.**

### Acceptance gate
- If the lib jar is in `mods/`: `[MOTM] PlayerInteractLib fallback subscribed` appears at server start.
- If absent: `[MOTM] PlayerInteractLib not present` appears; mod continues to work via Phase 2 path.

---

## Phase 9 — Broaden across remaining 39 styles

> **Do not start this phase until Phase 5 passes twice in a row.**

### Goal
Expand visible-and-validated coverage from Terra/Quake to all 40 styles. The cast pipeline is content-shared — once the input contract works, every ability runs through it. The only per-style work is verifying the `cast_type` behavior in `GameplayPlaybackManager` and confirming asset bindings in `HytaleAssetResolver`.

### Files (most styles will require zero new code; some may add to)
- `src/main/java/com/motm/manager/GameplayPlaybackManager.java`
- `src/main/java/com/motm/util/HytaleAssetResolver.java`
- (Custom MOTM EntityEffect JSONs in `src/main/resources/Server/Entity/Effects/MOTM/` — add new ones if a style needs unique visuals)

### Expansion order (one style per session; rotate classes)
1. Terra / Quake — **already proven in Phase 5**
2. Terra / Metal — defensive variants (Iron Wall, Metal Coat, Alloy Enhancement)
3. Hydro / Icicle — Frozen Needles, Stalactite Crash, Skate
4. Aero / Wind Blade — Air Slash, Gale Cutter, Razor Wind
5. Corruptus / Flame — Fireball, Ignite, Combust
6. Terra / Magma
7. Hydro / Snow
8. Aero / Thunder
9. Corruptus / Necro
10. ... continue rotating classes through all 40 styles

### Per-style acceptance template

For each style, run the same test pattern as Phase 5 but with that style:
1. `/motm style <styleId>`
2. Three abilities cast cleanly via LMB / RMB / Use.
3. Each ability's stated `cast_type` produces visible behavior (e.g. `projectile` actually travels; `self_buff` shows a buff icon or particle loop; `barrier` actually spawns blocks).
4. Cooldowns enforced (re-clicking during cooldown produces "Ready in Xs" message).
5. Resource cost deducted (`/motm resources` shows the spend).
6. No idle damage.

### What to do per style

For each style, in this exact order:
1. **Read the style data.** `src/main/resources/data/styles/<class>_styles.json`, find the style entry, read all three abilities.
2. **Cross-check `HytaleAssetResolver`** — does the resolver return non-empty assets for this class+style? Look for the resolver's switch entries. If a style is missing, add a new private static constant and route it in the switch. Pick vanilla asset IDs by:
   - Running `/showcase particles` and `/showcase dump` ([Effect Showcase](https://www.curseforge.com/hytale/mods/effect-showcase)) to discover IDs
   - Matching the style's `theme` and the ability's `cast_type` to an existing vanilla effect (e.g. fire abilities → `Server/Particles/Combat/Fire_Stick/...`)
3. **Cross-check `GameplayPlaybackManager`'s cast-type sets** (top of file, ~lines 47-67). If the style uses a new `cast_type` token not in any of the existing sets (`MOVEMENT_CAST_TYPES`, `LINE_CAST_TYPES`, `AREA_CAST_TYPES`, etc.), add it to the right set so the runtime knows how to handle it.
4. **Run the per-style acceptance template.**
5. **If a style has unique visuals that vanilla can't approximate**, create a new `MOTM_<Class>_<Style>_Cast.json` (and matching `_Impact`, `_Field`, `_Move` as needed) in `src/main/resources/Server/Entity/Effects/MOTM/`. Use the existing files (`MOTM_Terra_Gem_Cast.json` etc.) as templates.

### Never touch
- `src/main/resources/data/styles/<class>_styles.json` — surgical edits only on explicit request, never as part of this phase's expansion. Three "Restore" commits in git history show prior incidents where these were lost.

---

## Phase 10 — Cleanup + memory hygiene

### Files
- `README.md`
- `C:\Users\fishe\.claude\projects\C--Users-fishe-Documents-projects\memory\project_mystical_hytale_mod.md` (user's auto-memory)
- `CLAUDE.md`

### Step 10.1 — Update README

Current README says these are "still in progress":
- real server tick registration
- real mob spawn/death event hooks
- deeper perk effect integration into live Hytale systems
- full in-game validation and public release polish

Server tick and mob spawn/death hooks are **done** (`MotmServerTickSystem`, `MotmMobRuntimeSystem`). Move them to "Implemented so far." Keep:
- Deeper perk effect integration — partly addressed in Phase 7; mark "Phase 7 wires stat-layer effects (stat_increase, stat_multiplier, on_kill heal). Damage triggers, transformations, auras still TODO."
- Full in-game validation — Quake slice validated per Phase 5; broader validation per Phase 9.

### Step 10.2 — Delete stale CODEX_*.md docs

These are stale planning docs from March:
- `CLAUDE_HANDOFF_2026-03-30.md`
- `CLAUDE_HANDOFF_PROMPT_2026-03-30.txt`
- `CLAUDE_RECOVERED_PLAN_2026-03-31.md`
- `CLAUDE_RESUMED_PLAN_2026-03-31.md`
- `CODEX_CORRECTIONS_PLAN.md`
- `CODEX_IMPLEMENTATION_PROMPT_2026-03-31.txt`

Move them to a new folder `docs/archive/` so they're preserved but out of the project root. Do **not** delete outright — they document past decisions.

### Step 10.3 — Memory update (only if running with Claude Code)
This is for Claude on the next session. Codex can skip this step. If running this in Claude Code, update memory with the user's confirmation:
- "Mystical-Hytale-Mod: custom item interactions registered (Phase 2 of CODEX_IMPLEMENTATION_PLAN_2026-05-13.md). Spellbook routes LMB/RMB/Use to slots 1/2/3 via SimpleInstantInteraction subclasses, not vanilla actions."

---

## Reference appendix — what NOT to do

- **Do not** add `"Primary": "Root_Unarmed_Attack_Swing_Left"` or any bare-string slot value back to the spellbook JSON.
- **Do not** regenerate `data/styles/*.json` for any reason. Surgical edits only.
- **Do not** revert the spellbook item to a Recipe Book or Weapon shell.
- **Do not** add `--no-verify` to commits.
- **Do not** broaden beyond Quake until Phase 5 passes twice consecutively.
- **Do not** delete the archived CODEX_*.md handoff docs; move them to `docs/archive/`.
- **Do not** add empty `try { ... } catch (Throwable t) {}` blocks. Always log.

## Reference appendix — file/class cheat sheet

| Concern | Location |
| --- | --- |
| Custom item Interactions JSON | `src/main/resources/Server/Item/Items/MOTM_Spellbook_Focus.json` |
| Java interaction classes (new in Phase 2) | `src/main/java/com/motm/interaction/MotmSpellbookInteraction.java` |
| Codec registration | `MenteesMod.setup()` → ~line 250-318 |
| Event registration | `MenteesMod.registerHytaleHooks()` → ~line 347 |
| Input → cast pipeline | `MenteesMod.tryCastSpellbookSlot` (~line 2566) → `MotmCommand.castAbilityBySlot` → `GameplayPlaybackManager.executeAbility` |
| Visual asset resolution | `com.motm.util.HytaleAssetResolver` |
| Visual playback (`applyEffectById`) | `GameplayPlaybackManager` ~line 1851 |
| Style/ability data | `src/main/resources/data/styles/{aero,corruptus,hydro,terra}_styles.json` |
| Class data | `src/main/resources/data/classes/{aero,corruptus,hydro,terra}.json` |
| Perk data | `src/main/resources/data/perks/{aero,corruptus,hydro,terra}_perks.json` |
| Race data | `src/main/resources/data/races/races.json` |
| Lifecycle handlers | `MenteesMod.onPlayerConnect` / `onPlayerReady` (~lines 416-492) |
| Custom MOTM effects | `src/main/resources/Server/Entity/Effects/MOTM/*.json` (31 files) |
| Status HUD asset | `src/main/resources/Common/UI/Custom/HUD/MOTM_StatusHud.ui` |
| Spellbook page asset (PHASE 6) | `src/main/resources/Common/UI/Custom/Pages/MOTM_Spellbook.ui` |
| ECS tick bridge | `com.motm.system.MotmServerTickSystem` |
| Mob spawn/death tracker | `com.motm.system.MotmMobRuntimeSystem` |
| Perk effect stub (PHASE 7) | `com.motm.manager.PerkManager.applyPerkEffects` |
| Build script | `scripts/build-install.ps1` |
| Plugin manifest | `src/main/resources/manifest.json` |
| Gradle config | `build.gradle`, `gradle.properties` |

## Reference appendix — discovery commands

| What you want | Command |
| --- | --- |
| List vanilla particle IDs | In-game: `/showcase dump` (from Effect Showcase mod) |
| Inspect built jar contents | `& "$env:JAVA_HOME\bin\jar.exe" tf <path-to-jar>` |
| Extract a single file from jar | `& "$env:JAVA_HOME\bin\jar.exe" xf <jar> <internal-path>` |
| Find an API class location | `& "$env:JAVA_HOME\bin\jar.exe" tf "$env:APPDATA\Hytale\install\release\package\game\latest\Server\HytaleServer.jar" \| Select-String -Pattern "<ClassName>"` |
| Inspect class method list | `& "$env:JAVA_HOME\bin\javap.exe" -cp <jar> <fully.qualified.ClassName>` |

## Reference appendix — sources

- Custom item interactions guide: https://hytalemodding.dev/en/docs/guides/plugin/item-interaction
- Interaction system reference: https://almanax-21.github.io/Hytale-documentation.github.io/systems/interactions.html
- PlayerInteractLib (fallback): https://www.curseforge.com/hytale/mods/playerinteractlib
- PlayerMouseButtonEvent confirmed working: https://github.com/timiliris/Hytale-Docs/issues/25
- Asset pack overview: https://doctale.dev/asset-development/overview/
- Custom interaction example with imports: https://www.hytalevault.dev/en/examples/custom-interaction/
- Effect Showcase (vanilla asset discovery): https://www.curseforge.com/hytale/mods/effect-showcase
- Asset Editor & Blockbench guide: https://hytalecharts.com/news/hytale-asset-editor-blockbench-custom-models-guide
- Hytale Blockbench plugin: https://github.com/JannisX11/hytale-blockbench-plugin
- Animation API docs: https://hytale-docs.pages.dev/modding/npc-ai/animations/
- Hytale modding strategy/status: https://hytale.com/news/2025/11/hytale-modding-strategy-and-status
