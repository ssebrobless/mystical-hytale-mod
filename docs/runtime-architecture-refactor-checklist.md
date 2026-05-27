# Runtime Architecture Refactor Checklist

Updated: 2026-05-26

This checklist turns `docs/hytale-complete-api-alignment-audit.md` into the
single implementation record for the full cleanup PR. The verification harness
branch has landed on `main`; keep this refactor rebased on current `main`.

```
╔════════════════════════════════════════════════════════════════════╗
║ Target Shape                                                      ║
╠════════════════════════════════════════════════════════════════════╣
║ MenteesMod                                                        ║
║   ├─ lifecycle + manager wiring                                   ║
║   ├─ runtime task queue owner                                     ║
║   ├─ command/harness surfaces                                     ║
║   └─ narrow compatibility facade                                  ║
║ Runtime services                                                  ║
║   ├─ runtime task queue                                           ║
║   ├─ inventory ops                                                ║
║   ├─ command auth/dev dispatch                                    ║
║   ├─ proof/scenario registry                                      ║
║   └─ gameplay playback slices                                     ║
╚════════════════════════════════════════════════════════════════════╝
```

## Checklist

- [x] Runtime task queue and mutation context
  - [x] Replace ad hoc pending sets/maps/queues in `MenteesMod` with a named
        runtime task owner.
  - [x] Preserve existing ordering and one-shot/coalescing behavior.
  - [x] Expose queue sizes to observability.
  - [x] Record failures with enough context to debug the task that failed.
  - [x] Replace public mutable task collection access with immutable pending
        views, typed completion/cancel methods, and accepted/executed/skipped/
        failed evidence events.
- [x] Inventory operations
  - [x] Add one helper for grant, remove-slot, remove-count, and set-slot
        operations.
  - [x] Verify current jar semantics by compiling against the installed Hytale
        jar rather than assuming old docs are exact.
  - [x] Replace direct inventory mutations in `MenteesMod` and
        `GameplayPlaybackManager`.
  - [x] Emit compact observability/log evidence for failed mutations.
  - [x] Move combined Hytale player inventory assembly into
        `MotmPlayerInventory`.
  - [x] Move Hydro waterskin identity, tier metadata, stack creation,
        resource-manager bridge lookup, and inventory sync/remove
        orchestration into `HydroContainerItems` and `HydroInventoryBridge`.
  - [x] Move spellbook/dev grimoire item identity and saved-inventory migration
        item tables into `SpellbookInventoryItems`, leaving current plugin
        public methods as delegates for callers.
  - [x] Move Terra resource item classification and units-per-item policy into
        `TerraInventoryResourcePolicy`.
  - [x] Move Terra inventory count/spend bridge orchestration into
        `TerraInventoryResourceBridge`, leaving the plugin shell with only
        bridge wiring for that path.
  - [x] Add an architecture ratchet preventing `MenteesMod` from re-owning Terra
        resource prefix tables, unit tables, local prefix matching helpers, or
        inventory count/spend bridge methods.
  - [x] Add an architecture ratchet preventing `MenteesMod` from re-owning
        combined player inventory container assembly.
  - [x] Add an architecture ratchet preventing `MenteesMod` from re-owning Hydro
        waterskin constants, BSON metadata parsing, stack creation, tier checks,
        or container sync/remove orchestration.
  - [x] Move Hydro water-source refill handling into
        `HydroContainerRefillHandler`, including source-block detection,
        resource refill, persistent sync, HUD refresh, and player messaging,
        with an architecture ratchet preventing `MenteesMod` from re-owning the
        refill behavior.
  - [x] Move block-damage interaction handling into
        `BlockDamageInteractionHandler`, including pickaxe detection, nearest
        Terra miner selection, block damage scaling, Alloy tool-use dispatch,
        and player messaging, with an architecture ratchet preventing
        `MenteesMod` from re-owning the behavior.
  - [x] Move spellbook input routing into `SpellbookInputHandler`, including
        interact/mouse/custom-interaction slot routing, spellbook/dev-book
        gesture policy, duplicate debounce ownership, Hydro refill handoff,
        weapon follow-up hit handoff, and player messaging, with an architecture
        ratchet preventing `MenteesMod` from re-owning the behavior.
  - [x] Move spellbook custom-page opening into `SpellbookPageActions`,
        including custom page construction, Hytale page-manager mutation,
        player-ref validation, custom-page client intent emission, and an
        architecture ratchet preventing `MenteesMod` from re-owning that page
        opening behavior.
  - [x] Move proof cleanup processing into `MotmProofCleanupProcessor`,
        including temporary selection restoration, proxy despawn, store/world
        filtering, cleanup removals, and diagnostic logging, with an
        architecture ratchet preventing `MenteesMod` from re-owning the cleanup
        loop.
  - [x] Move proof world/effect actions into `MotmProofActions`, including
        proof preflight, effect application, target effect lookup, temporary
        block/fluid placement, proxy spawn, movement proof teleport,
        cleanup-state registration, proof evidence emission, and an architecture
        ratchet preventing `MenteesMod` from re-owning proof mutation helpers.
  - [x] Move live style-test sequencing into
        `StyleTestSequenceRuntimeTaskProcessor` and target lookup into
        `StyleTestTargetResolver`, including active test advancement, nearest
        NPC target selection, target-block derivation, ability cast queueing,
        server-truth events, and an architecture ratchet preventing
        `MenteesMod` from re-owning the sequence loop.
  - [x] Move dev style-test command policy into `StyleTestCommandActions`,
        including style lookup, live style-test start/stop/status messaging,
        single-ability test queueing, proof request validation, style-test mob
        task queueing, review arena reset queueing, weapon follow-up probe
        execution, forced stomp landing, free-cast toggles, and an architecture
        ratchet preventing `MenteesMod` from re-owning that command policy.
  - [x] Move Terra review kit and inventory cleanup policy into
        `TerraReviewInventoryKit`, including kit contents, essential-item
        retention, review-item backfill, asset availability checks, inventory
        grant/remove loops, and an architecture ratchet preventing
        `MenteesMod` from re-owning that review inventory behavior.
  - [x] Move command-facing inventory task queue policy into
        `InventoryCommandActions`, including Hydro container sync queueing,
        spellbook/dev-book grant queueing, runtime-player id resolution,
        runtime-player handle refresh for player overloads, dev-tools gating,
        and an architecture ratchet preventing `MenteesMod` from re-owning that
        queue policy.
  - [x] Move free-cast test safety into `FreeCastSafetyProcessor`, including
        enabled-player ticking, health-drop diagnostics, stat refill, burn/dot
        clearing, movement normalization/reset, native invulnerability
        attach/remove, and an architecture ratchet preventing `MenteesMod` from
        re-owning that mutation loop.
  - [x] Move dev player test mutations into `DevPlayerTestActions`, including
        relocation destination policy, flatlands/lane platform placement,
        teleport execution, game-mode mutation, and an architecture ratchet
        preventing `MenteesMod` from re-owning those world/player mutations.
  - [x] Move dev runtime command queue policy into `DevRuntimeCommandActions`,
        including relocation target validation, daylight request queueing,
        review game-mode parsing, Terra review kit/cleanup queueing, dev-tools
        gating, player-runtime availability checks, and an architecture ratchet
        preventing `MenteesMod` from re-owning that command queue policy.
  - [x] Move style-test mob and review arena world actions into
        `StyleTestMobActions`, including spawn layout policy, NPC spawn/despawn,
        tracked-ref counts, nearby cleanup-role scanning, arena scrub block/fluid
        mutation, and an architecture ratchet preventing `MenteesMod` from
        re-owning those review-world actions.
  - [x] Move agent observability snapshot evidence-shape ownership into
        `MotmObservabilitySnapshotBuilder`, including pending runtime task
        counts, player data/runtime/native-effect/stat/movement/inventory
        snapshots, style-test target rows, proof native-effect readback, and an
        architecture ratchet preventing `MenteesMod` from re-owning snapshot
        assembly helpers.
  - [x] Move command-facing observability run control into
        `MotmObservabilityActions`, including start/stop/status, scenario
        mutation, markers, snapshot capture, metadata assembly, focused tests,
        and architecture smoke checks keeping public plugin methods
        delegation-only.
  - [x] Move observability event emission and trace-context policy into
        `MotmObservabilityEvents`, including control/causality/server-truth/
        client-intent writes, current trace lookup, enter/restore semantics,
        active-run client trace allocation, focused tests, and architecture
        ratchets preventing trace ownership from returning to the plugin shell.
  - [x] Move player progression runtime stat mutation and world-average level
        policy into `PlayerProgressionRuntimeActions`, including target-health
        tracking, health modifier apply/clear, free-cast max-health preservation,
        world-scoped average level anchors, all-online progression refresh
        iteration, and an architecture ratchet preventing `MenteesMod` from
        re-owning progression stat mutation.
  - [x] Move runtime-player lookup and geometry helpers into
        `RuntimePlayerView`, including player-id lookup, player-ref lookup,
        universe player refs, store/world membership checks, position/forward
        readback, movement-state/crouch readback, vector formatting, and dev
        position summaries, with an architecture ratchet preventing
        `MenteesMod` from re-owning those helpers.
  - [x] Move mob-scaling anchor-level policy into
        `PlayerProgressionRuntimeActions`, leaving `MenteesMod` spawn handling
        to delegate level-anchor selection to the progression owner.
  - [x] Move perk trigger registration and on-kill trigger health mutation into
        `PerkTriggerRuntimeActions`, leaving public plugin methods as delegates
        for current stat-modifier callers and adding an architecture ratchet
        preventing `MenteesMod` from re-owning perk-trigger runtime effects.
  - [x] Move player runtime rebuild sequencing into
        `PlayerRuntimeRebuildActions`, including cooldown reset, passive/status/
        reaction/resource cleanup, persistent resource sync, synergy/race reset,
        class resource/perk/race reapply, Hydro sync queueing, progression/HUD
        refresh, free-cast invulnerability handling, focused tests, and an
        architecture smoke check keeping the plugin method delegation-only.
  - [x] Move live mob-spawn scaling policy into `MobSpawnRuntimeActions`,
        including scaling-player lookup, category anchor resolution, base-stat
        fallback, boss/ordinary scaling, party/environment modifiers, elite
        promotion, display name/color resolution, model-level result ownership,
        focused tests, and an architecture ratchet preventing the plugin shell
        from re-owning that scaling pipeline.
  - [x] Move combat lifecycle kill/death side effects into
        `PlayerCombatLifecycleActions`, including mob-kill leveling/resource/
        passive/perk/achievement refresh flow, player-death statistics/combo
        reset, status/elemental/armed-stomp cleanup, HUD refresh, focused tests,
        and architecture smoke checks keeping public lifecycle methods
        delegation-only.
  - [x] Move player session lifecycle handling into
        `PlayerSessionLifecycleActions`, including join data/rested/resource/
        perk/race/passive rehydration, connect saved-loadout rebuild and
        spellbook recovery, ready-time dev cleanup/free-cast/HUD setup,
        disconnect persistence and runtime cleanup, focused tests, and
        architecture smoke checks keeping public session callback methods
        delegation-only.
  - [x] Move spellbook/dev-book delivery and saved-inventory migration into
        `SpellbookInventoryKit`, including legacy spellbook cleanup, default
        spellbook grant, dev grimoire grant, player messages, and an
        architecture ratchet preventing `MenteesMod` from re-owning those
        inventory mutation paths.
  - [x] Move custom HUD install/refresh behavior into `MotmStatusHudActions`,
        including HUD construction, install/refresh queuing, native HUD
        component hiding, custom HUD intent emission, stale HUD cleanup, and
        player/store filtering, with an architecture ratchet preventing
        `MenteesMod` from re-owning custom HUD behavior.
  - [x] Add an architecture ratchet preventing `MenteesMod` from re-owning
        spellbook/dev grimoire item identity tables.
- [x] Command authorization and dev dispatch
  - [x] Introduce a small command auth policy for public, dev, and future admin
        surfaces.
  - [x] Move dev-tools disabled messaging into `MotmCommandAuth`, so the plugin
        shell delegates build/config denial text instead of owning command
        authorization message policy.
  - [x] Keep public player commands available without dev tools.
  - [x] Move `/motm dev ...` routing out of the main command class or behind a
        clearly named collaborator.
  - [x] Keep file-backed harness commands gated to internal test builds and
        `dev_tools_enabled`.
  - [x] Move file-backed dev command inbox/outbox polling, normalization, command
        trace envelope, and outbox writes into `MotmDevCommandInbox`, leaving
        `MenteesMod` as a hook provider.
  - [x] Add an architecture ratchet preventing `MenteesMod` from re-owning the
        file-backed dev command inbox/outbox protocol.
- [x] Proof/scenario registry
  - [x] Replace the large proof switch with a registry/list that can be extended
        by feature work.
  - [x] Make available proof ids discoverable from help text and observability.
  - [x] Keep raw run evidence and trace ids unchanged.
- [x] Lifecycle registration cleanup
  - [x] Verify local API return types for event, command, and system
        registration.
  - [x] Store/unregister handles where the API supports it.
  - [x] Keep packet watcher cleanup unchanged or stronger.
  - [x] Move plugin data directory migration and scanner-safe legacy manifest
        handling into `MotmPluginDataDirectories`, leaving `MenteesMod` to
        consume the resolved operational path.
- [x] Gameplay playback slicing
  - [x] Split the most mechanically separable playback concerns out of
        `GameplayPlaybackManager`.
  - [x] Start with inventory/held-item helpers and shared geometry helpers
        before attempting ability-family splits.
  - [x] Move active projectile state into
        `runtime.ability.projectile.ActiveProjectile`, so the playback manager
        no longer owns the private projectile state class or mutates raw travel
        distance/visual refresh fields directly.
  - [x] Move the active projectile collection into
        `runtime.ability.projectile.ProjectileRuntimeState`, leaving
        Hytale-facing launch execution, impact application, and visual cleanup
        callbacks in the playback manager for now.
  - [x] Move projectile batch registration and owner cleanup orchestration into
        `runtime.ability.projectile.ProjectileRuntimeState`, leaving the
        playback manager with only facade-level reset wiring for projectiles.
  - [x] Move projectile visual runtime contract into
        `runtime.ability.projectile.ProjectileVisualRuntime`.
  - [x] Move concrete projectile visual proxy spawn/sync/refresh/despawn and
        identity-component hiding into
        `runtime.ability.projectile.ProjectileVisualHytaleAdapter`.
  - [x] Move concrete projectile launch origin/direction lookup into
        `runtime.ability.projectile.ProjectileLaunchHytaleAdapter`.
  - [x] Move concrete projectile hit scanning and impact/traversal target
        collection into `runtime.ability.projectile.ProjectileHitHytaleAdapter`.
  - [x] Move concrete projectile impact and traversal damage/effect mutation
        into `runtime.ability.projectile.ProjectileImpactHytaleAdapter`.
  - [x] Move generic execution-policy classification out of
        `GameplayPlaybackManager` into `AbilityExecutionPolicy`, including
        caster visual suppression, movement/line/multi-target cast families,
        caster/target effect token filters, Dominate extra target riders, Alloy
        caster-token exclusion, ground restriction, anchor-drag classification,
        direct-damage cause classification, special-damage policy selection, and
        an architecture ratchet preventing those branches from returning to the
        manager.
  - [x] Move runtime effect-id resolver policy out of `GameplayPlaybackManager`
        into `AbilityRuntimeEffects`, including class fallback effects, themed
        style overrides, runtime effect-id filtering, movement cast effect
        selection, and an architecture ratchet preventing those resolver tables
        from returning to the manager.
  - [x] Move ability status-effect construction policy out of
        `GameplayPlaybackManager` into `AbilityStatusEffects`, including token
        to status type/value mapping, configured/default duration handling,
        one-shot buff duration defaults, tests, and an architecture ratchet
        preventing that switch table from returning to the manager.
  - [x] Move pure ability runtime math policy out of `GameplayPlaybackManager`
        into `AbilityRuntimeMath`, including movement distance, vertical
        movement, range fallback, pulse damage, pull-step/pull-lift, base damage
        scaling, target-sequence damage multipliers, focused tests, and an
        architecture ratchet preventing those resolver methods from returning to
        the manager.
  - [x] Delete projectile visual wrapper methods from `GameplayPlaybackManager`
        so launch/tick callbacks call `ProjectileVisualHytaleAdapter` directly.
  - [x] Move projectile tick ordering into
        `runtime.ability.projectile.ProjectileTickRuntime`, leaving concrete
        Hytale-facing store access in the playback manager for now.
  - [x] Move concrete projectile tick hook wiring into
        `runtime.ability.projectile.ProjectileTickHytaleAdapter`.
  - [x] Move Hytale-facing projectile launch execution into
        `runtime.ability.projectile.ProjectileLaunchHytaleAdapter`.
  - [x] Move active projectile tick iteration, trace scoping, store filtering,
        reset cleanup, and active-count reporting into
        `runtime.ability.projectile.ProjectileLifecycleHytaleAdapter`.
  - [x] Collapse projectile state/adapter construction into
        `runtime.ability.projectile.ProjectileRuntimeFacade`, leaving
        `GameplayPlaybackManager` with one projectile-family dependency and
        generic cross-family support callbacks instead of individual projectile
        state/adapter fields, plus an architecture ratchet preventing those
        fields from returning.
  - [x] Move shared visual proxy tracking into
        `runtime.state.VisualProxyRuntimeState`, leaving concrete Hytale proxy
        spawn/despawn mutation callbacks in the playback manager for now.
  - [x] Move persistent field classification, geometry defaults, timing,
        pulse-damage ratios, pull-lift decisions, and field intervals into
        `runtime.ability.field.FieldRuntimeSpecs`.
  - [x] Move persistent field terrain policy into `runtime.ability.field`,
        including terrain kind, placement reason, restore-before-place behavior,
        block/fluid candidates, column height, brown-debris summary decoration,
        Iron Wall origin policy, and caster-centered origin policy.
  - [x] Move recent field-origin state into
        `runtime.ability.field.RecentFieldOrigin`, so the playback manager no
        longer owns the generic recent-position record for stable field-origin
        guards.
  - [x] Move recent field-origin guard maps into
        `runtime.ability.field.FieldOriginRuntimeState`, leaving only
        user-facing warning log formatting in the playback manager.
  - [x] Move active field state into `runtime.ability.field.ActiveField`, so
        the playback manager no longer owns the private field state class or
        mutates raw field scheduling/anchor fields directly.
  - [x] Move field batch registration into
        `runtime.ability.field.FieldRuntimeState`.
  - [x] Move persistent field activation state construction and summaries into
        `runtime.ability.field.FieldActivationRuntime`, leaving concrete Hytale
        origin/center lookup, terrain placement, overlap pushes, visual spawn,
        and field-state registration callbacks in the playback manager for now.
  - [x] Move field visual runtime contract into
        `runtime.ability.field.FieldVisualRuntime`.
  - [x] Move persistent field tick ordering into
        `runtime.ability.field.FieldTickRuntime`, leaving concrete Hytale-facing
        pulse mutations, terrain restoration, sinkhole mutation, and owner
        mobility callbacks in the playback manager for now.
  - [x] Move concrete Hytale field target collection into
        `runtime.ability.field.FieldTargetHytaleAdapter`.
  - [x] Move concrete Hytale field visual proxy spawn/sync/refresh/despawn and
        visual-position planning into
        `runtime.ability.field.FieldVisualHytaleAdapter`.
  - [x] Move concrete Hytale field pulse damage/effect mutation and per-target
        terrain token routing into
        `runtime.ability.field.FieldPulseHytaleAdapter`.
  - [x] Move concrete Hytale field support and owner pulse mutation into
        `runtime.ability.field.FieldSupportPulseHytaleAdapter`.
  - [x] Move concrete Hytale sinkhole mutation into
        `runtime.ability.field.FieldSinkholeHytaleAdapter`.
  - [x] Move concrete Hytale field terrain restoration into
        `runtime.ability.field.FieldTerrainHytaleAdapter`.
  - [x] Move concrete Hytale field owner mobility into
        `runtime.ability.field.FieldOwnerMobilityHytaleAdapter`.
  - [x] Move persistent field activation execution into
        `runtime.ability.field.FieldActivationHytaleAdapter`, leaving terrain
        placement and Iron Wall overlap-push support callbacks for the terrain
        world-mutation slice.
  - [x] Move supplemental non-persistent terrain trail/aura activation into
        `runtime.ability.terrain.TerrainSupplementalHytaleAdapter`, including
        trail/aura selection, visual spawn handoff, field-runtime registration,
        surface cue placement, summary formatting, and an architecture ratchet
        preventing the playback manager from re-owning that activation path.
  - [x] Move projectile launch loop and active projectile construction into
        `runtime.ability.projectile.ProjectileLaunchRuntime`, leaving
        projectile-state registration callbacks in the playback manager for now.
  - [x] Move terrain trail/aura classification, geometry decisions, timing
        constants, and temporary selection minimum lifetime into
        `runtime.ability.terrain.TerrainRuntimeSpecs`.
  - [x] Move temporary terrain selection, moving trail, stacking column, and
        terrain collection ownership into `runtime.ability.terrain`, so
        `GameplayPlaybackManager` no longer owns raw terrain lists or private
        terrain state classes.
  - [x] Move moving-trail, Lapidary gem, and stacking-column construction into
        `runtime.ability.terrain.TerrainActivationRuntime`, leaving concrete
        Hytale block/entity spawn and mutation callbacks in the playback
        manager for now.
  - [x] Move temporary terrain tick ordering into
        `runtime.ability.terrain.TerrainTickRuntime`, leaving concrete
        Hytale-facing block/fluid placement, selection restoration,
        block-selection construction, and world mutation callbacks in the
        playback manager for now.
  - [x] Move concrete Hytale terrain tick/restoration hooks into
        `runtime.ability.terrain.TerrainHytaleAdapter`.
  - [x] Move concrete Hytale terrain placement, block/fluid asset resolution,
        temporary selection registration, moving-trail registration,
        stacking-column registration, surface/ring/trail/column/shell/fluid
        `BlockSelection` construction, persistent field terrain placement, and
        Iron Wall overlap push mutation into
        `runtime.ability.terrain.TerrainPlacementHytaleAdapter`.
  - [x] Move ability-specific Terra terrain routing into
        `runtime.ability.terrain.TerrainAbilityHytaleAdapter`, leaving explicit
        callbacks for cross-family effects, position/direction reads, active gem
        anchor lookup, and Lapidary proxy spawn.
  - [x] Move Lapidary gem proxy lifecycle into
        `runtime.ability.terrain.TerrainGemHytaleAdapter`, including proxy NPC
        spawn, HP label mutation, visual-proxy registration/despawn, active gem
        tick processing, owner cleanup, and active gem anchor lookup.
  - [x] Move sinkhole terrain marker orchestration into
        `runtime.ability.terrain.TerrainSinkholeMarkerHytaleAdapter`, including
        crack and dust-ring placement around sinkhole fields.
  - [x] Move summon role/profile decisions into
        `runtime.ability.summon.SummonRuntimeSpecs`, including named summon
        model mappings, role classification, ranged/melee behavior,
        attack/chase ranges, cadence, hatch delay, damage multipliers, and
        default attack tokens.
  - [x] Move active summon state into `runtime.ability.summon.ActiveSummon`, so
        the playback manager no longer owns the private summon state class or
        mutates raw summon timing/target-lock fields directly.
  - [x] Move active summon construction into
        `runtime.ability.summon.SummonActivationRuntime`, leaving concrete
        Hytale NPC spawn, appearance mutation, impact effect mutation, and
        summon-state registration callbacks in the playback manager for now.
  - [x] Move the active summon owner index into
        `runtime.ability.summon.SummonRuntimeState`, leaving concrete Hytale
        spawn, movement, target acquisition, attack, splash, and despawn
        callbacks in the playback manager for now.
  - [x] Move summon tick decisions into
        `runtime.ability.summon.SummonTickRuntime`, leaving concrete
        Hytale-facing spawn, NPC movement, target acquisition, damage/effect
        mutation, splash effects, and despawn callbacks in the playback manager
        for now.
  - [x] Move summon attack-effect routing into
        `runtime.ability.summon.SummonAttackEffectRuntime`, leaving concrete
        Hytale token application, shield mutation, pull mutation, splash target
        collection, and splash damage callbacks in the playback manager for
        now.
  - [x] Move summon attack lifecycle into
        `runtime.ability.summon.SummonAttackRuntime`, leaving concrete Hytale
        damage execution, post-damage passive execution, lifesteal, impact
        effect mutation, attack-effect callbacks, and logging callbacks in the
        playback manager for now.
  - [x] Move summon splash iteration and splash damage math into
        `runtime.ability.summon.SummonSplashRuntime`, leaving concrete Hytale
        target collection, token mutation, damage execution, post-damage
        passive execution, and impact mutation as callbacks in the playback
        manager for now.
  - [x] Move summon buff/command routing into
        `runtime.ability.summon.SummonBuffRuntime`, leaving concrete Hytale
        position lookup, buff visual mutation, target acquisition, and attack
        execution callbacks in the playback manager for now.
  - [x] Move summon target selection into
        `runtime.ability.summon.SummonTargetRuntime`, leaving concrete Hytale
        target validation and nearest-target search callbacks in the playback
        manager for now.
  - [x] Move summon movement destination planning into
        `runtime.ability.summon.SummonMovementRuntime`, leaving concrete Hytale
        NPC lookup and `moveTo` mutation callbacks in the playback manager for
        now.
  - [x] Move concrete Hytale summon spawn, appearance mutation, active summon
        registration, raw base-damage calculation, spawn-position lookup, model
        resolution, owner cleanup, and despawn mutation into
        `runtime.ability.summon.SummonLifecycleHytaleAdapter`.
  - [x] Move concrete Hytale summon buff routing, tick hook wiring, NPC
        movement, target acquisition, awaken visual mutation, and clone
        repositioning into `runtime.ability.summon.SummonControlHytaleAdapter`.
  - [x] Move concrete Hytale summon damage, impact effects, attack-effect
        tokens, shields, pulls, splash target iteration, and splash damage into
        `runtime.ability.summon.SummonAttackHytaleAdapter`.
  - [x] Move weapon follow-up profile decisions into
        `runtime.ability.followup.WeaponFollowUpSpecs` so
        `GameplayPlaybackManager` arms a resolved follow-up spec instead of
        carrying duplicate ability-id lookup helpers.
  - [x] Move active weapon follow-up state into
        `runtime.ability.followup.ActiveWeaponFollowUp` so follow-up state is
        owned by the runtime family instead of a private playback-manager inner
        class.
  - [x] Move the active weapon follow-up map into
        `runtime.ability.followup.WeaponFollowUpRuntimeState` so player-indexed
        follow-up lifecycle state is no longer a raw playback-manager
        collection.
  - [x] Move pure Alloy item-binding decisions into
        `runtime.ability.followup.ActiveWeaponFollowUp`/
        `WeaponFollowUpItemBinding`, leaving only Hytale visual/durability
        effects in the playback manager for now.
  - [x] Move Alloy durability restoration into
        `runtime.ability.followup.WeaponFollowUpDurabilityRestorer`, leaving
        visual cleanup, damage/effect application, and splash application as the
        remaining Hytale-facing follow-up work in the playback manager.
  - [x] Move pure follow-up hit arithmetic into
        `runtime.ability.followup.WeaponFollowUpHitMath`, leaving external
        status/passive hooks and Hytale damage/effect mutations in the playback
        manager until a runtime service can preserve call ordering.
  - [x] Move transformation profile decisions into
        `runtime.ability.transformation.TransformationRuntimeSpecs` so visual
        effect ids, combat/movement bonuses, rider tokens, collision settings,
        and summaries are owned by the transformation runtime family.
  - [x] Move transformation capability kind, owner refresh tokens, owner shield
        refresh amount, and grounded-ending policy into the transformation
        runtime spec so the playback manager executes resolved capability
        profiles instead of raw transformation ability-id branches.
  - [x] Move active transformation state into
        `runtime.ability.transformation.ActiveTransformation` so the playback
        manager no longer owns the private form state class.
  - [x] Move the active transformation map and pulse schedule into
        `runtime.ability.transformation.TransformationRuntimeState`, so
        player lookup, ability deactivation, processed tick removal, and
        next-pulse ownership no longer live as playback-manager maps.
  - [x] Move transformation tick decisions into
        `runtime.ability.transformation.TransformationTickRuntime`, so owner
        validity, expiry, pulse cadence, player lookup, end-condition checks,
        owner-position gating, owner-state refresh ordering, locomotion
        pressure ordering, form pulse dispatch, and next-pulse scheduling are
        runtime-owned.
  - [x] Move transformation pulse and locomotion effect routing into
        `runtime.ability.transformation.TransformationEffectRuntime`, so
        target selection shape, damage ratios, rider tokens, knockback flags,
        movement-factor clamping, and charge-shield ordering are runtime-owned.
  - [x] Move concrete Hytale transformation activation, tick hook wiring, owner
        refresh mutation, locomotion/pulse damage and effects, charge impacts,
        weapon rider mutation, and transformation cleave into
        `runtime.ability.transformation.TransformationHytaleAdapter`.
  - [x] Move the active field collection and sinkhole burial map into
        `runtime.ability.field.FieldRuntimeState`, leaving concrete target
        collection, terrain mutation, pulse effects, and visual callbacks as the
        remaining Hytale-facing field work in the playback manager.
  - [x] Move follow-up rider/payoff orchestration into
        `runtime.ability.followup.WeaponFollowUpHitEffects`, leaving concrete
        Hytale token/shield/heal/splash callbacks in the playback manager.
  - [x] Move the Alloy held-item visual effect id and apply/clear callback
        contract into `runtime.ability.followup.WeaponFollowUpVisualEffects`,
        leaving concrete Hytale effect mutation callbacks in the playback
        manager.
  - [x] Move follow-up splash target iteration and per-target ordering into
        `runtime.ability.followup.WeaponFollowUpSplashRuntime`, leaving concrete
        Hytale splash damage, passive, rider, and impact callbacks in the
        playback manager.
  - [x] Move primary follow-up hit ordering into
        `runtime.ability.followup.WeaponFollowUpPrimaryHitRuntime`, leaving
        concrete Hytale primary damage, post-damage passive, lifesteal, impact,
        and rider callbacks in the playback manager.
  - [x] Move native Alloy hit and tool-use ordering into
        `runtime.ability.followup.WeaponFollowUpNativeAlloyRuntime`, leaving
        concrete Hytale damage mutation, rider/effect mutation, durability
        restore, visual mutation, and map removal as callbacks.
  - [x] Move weapon follow-up expiry, Alloy visual cleanup ordering, unavailable
        player handling, and cross-store mutation deferral into
        `runtime.ability.followup.WeaponFollowUpLifecycleRuntime`, leaving
        concrete Hytale player lookup and visual mutation callbacks in the
        playback manager.
  - [x] Move concrete Hytale follow-up primary hit mutation, payoff
        shields/healing/splash, native Alloy damage/tool-use hooks, Alloy
        held-item visual mutation, and follow-up splash mutation into
        `runtime.ability.followup.WeaponFollowUpHytaleAdapter`.
  - [x] Collapse the manager-local projectile launch facade wrapper and launch
        runtime/support fields into `ProjectileLaunchHytaleAdapter`
        construction, so `GameplayPlaybackManager` no longer passes launch
        internals through every cast.
  - [x] Move proof/coating/terrain one-off ability routing into
        `runtime.ability.specific.AbilitySpecificHytaleAdapter`, leaving the
        manager with a direct adapter call and adding a ratchet against
        reintroducing the switch in `GameplayPlaybackManager`.
  - [x] Move combat support bookkeeping into
        `runtime.ability.combat.CombatRuntimeState`, leaving concrete kill
        reporting and status-effect application callbacks in the playback
        manager for now.
  - [x] Move channel and line-control active state into
        `runtime.ability.channel`, so the playback manager no longer owns
        private channel/line-control state classes or mutates raw pulse
        scheduling fields directly.
  - [x] Move channel and line-control construction into
        `runtime.ability.channel.ChannelActivationRuntime`, so channel-family
        input validation and runtime object construction are owned outside the
        playback manager.
  - [x] Move channel and line-control lifecycle collections into
        `runtime.ability.channel.ChannelRuntimeState`, so replace-per-player,
        owner cleanup, ability deactivation, and processed tick removal are
        owned by the channel runtime family instead of raw playback-manager
        lists.
  - [x] Move concrete Hytale channel and line-control activation/tick mutation
        into `runtime.ability.channel.ChannelHytaleAdapter`, including
        owner/target validation, line-control duration inference, channel pulse
        damage, lifesteal/life-drain/healing, line-control pulls, repeat
        target-token application, processed runtime removal, and deactivation
        handoff.
  - [x] Move active self-effect and player-anchor state into
        `runtime.ability.self`, so the playback manager no longer owns private
        anchor/self-effect state classes or mutates raw self-effect refresh
        scheduling directly.
  - [x] Move self-effect and player-anchor construction into
        `runtime.ability.self.SelfActivationRuntime`, so input validation and
        active self runtime object construction are owned outside the playback
        manager.
  - [x] Move active self-effect and player-anchor lifecycle collections into
        `runtime.ability.self.SelfRuntimeState`, so replace-per-player anchors,
        effect replacement, owner cleanup, and processed tick removal are owned
        by the self runtime family instead of raw playback-manager lists.
  - [x] Move concrete Hytale self-effect and player-anchor mutation into
        `runtime.ability.self.SelfHytaleAdapter`, including repeated effect
        ticking, completion effect dispatch, anchor position enforcement,
        movement-freeze mutation, velocity zeroing, processed runtime removal,
        and owner cleanup.
  - [x] Move active Lapidary gem state into
        `runtime.ability.terrain.ActiveLapidaryGem`, so gem ownership, health,
        center copying, label updates, and expiry checks no longer live as an
        untyped playback-manager record.
  - [x] Move active Lapidary gem lifecycle collection into
        `runtime.ability.terrain.LapidaryGemRuntimeState`, leaving concrete
        Hytale proxy spawn/despawn, health reads, label mutation, and visual ref
        cleanup callbacks in the playback manager for now.
  - [x] Move Lava Pool and magma hazard bookkeeping into
        `runtime.ability.terrain.LavaHazardRuntimeState`, leaving concrete
        velocity and movement-manager mutation callbacks in the playback manager
        for now.
  - [x] Move armed stomp state into `runtime.ability.stomp.ArmedStomp`, so
        transform landing observation, airborne tracking, and trigger expiry are
        owned outside the playback manager.
  - [x] Move armed stomp lifecycle map into
        `runtime.ability.stomp.StompRuntimeState`, leaving concrete Hytale
        player-position observation and shockwave firing callbacks in the
        playback manager for now.
  - [x] Move buried-victim state into `runtime.ability.field.BuriedVictim`, so
        sinkhole victim refs, previous gravity, and expiry are owned by the
        field runtime family instead of an anonymous playback-manager record.
  - [x] Preserve the public `GameplayPlaybackManager` API until tests prove the
        slice is stable.
- [x] Documentation and agent workflow
  - [x] Keep `AGENTS.md`, README, and observability docs aligned with the final
        refactor shape.
  - [x] Ensure future feature requests direct agents to extend the harness when
        evidence is missing.
  - [x] Make fresh-slate replacement the default architecture policy: retained
        legacy code must have an active external consumer, a compatibility
        register entry, and a removal gate.
  - [x] Treat the compatibility register as an implementation allowlist so new
        Java legacy/compatibility code must name its boundary or be deleted.
  - [x] Document the deletion-biased fresh-slate rule: old internal package
        shape, method shape, pending queues, duplicate lookup tables, and stale
        manager responsibilities are not compatibility contracts.
  - [x] Document that broad fresh-slate rewrites are acceptable when they reduce
        internal concept count and preserve supported behavior through tests or
        harness evidence.
  - [x] Clarify that compatibility preserves supported behavior contracts, not
        old internal code, and that internal-only legacy should be rewritten
        cleanly and deleted once evidence proves parity.
  - [x] Clarify that deletion of obsolete internal code is the default success
        path and compatibility is an allowlisted exception for named external
        contracts only.
  - [x] Clarify that broad refactors are expected to pass a deletion gate:
        name the old concept that disappeared, prove the replacement through
        static/unit/harness evidence, and register any retained path as a
        failing compatibility exception with a concrete removal gate.
  - [x] Add an explicit fresh-slate review rule so agents prefer deleting
        obsolete internal code over preserving old paths for rollback, merge
        comfort, speculative debugging, or vague legacy support.
  - [x] Clarify that retained old paths containing policy, scheduling, state
        mutation, effect math, target selection, or ability branching are still
        implementation owners and must be migrated rather than treated as
        harmless compatibility.
  - [x] Add a `GameplayPlaybackManager` raw runtime collection ratchet so new
        private runtime `List`/`Map`/`Set` fields must move to runtime-family
        state owners instead of rebuilding manager state.
  - [x] Add a `GameplayPlaybackManager` migrated ability state construction
        ratchet so direct `ActiveProjectile`, `ActiveField`, `ActiveSummon`,
        `ActiveSelfEffect`, `ActivePlayerAnchor`, `ActiveMovingTerrainTrail`,
        `ActiveLapidaryGem`, `ActiveStackingColumn`, `ActiveChannel`, and
        `ActiveLineControl` construction stays inside runtime-family owners.
  - [x] Remove deprecated `Player.getPlayerRef()` usage and add an
        architecture rail requiring PlayerRef lookup through the entity-store
        component path.
  - [x] Keep scenario choreography in `scripts/scenarios/*.json`; the baseline
        runner executes catalog setup/body/cleanup phases instead of owning
        hard-coded scenario command flow.
  - [x] Move server-tick runtime sequencing out of `MenteesMod` into
        `MotmRuntimeLoop`, including processor order, free-cast safety,
        class-passive ticking, dev command inbox processing, proof cleanup,
        armed-stomp/gameplay ticks, HUD refresh cadence, observability
        heartbeat cadence, pending-task heartbeat payloads, DoT diagnostics,
        focused tests, and an architecture ratchet keeping `onServerTick`
        delegation-only.
  - [x] Move dev command inbox runtime-player selection and trace fallback out
        of `MenteesMod` into `MotmDevCommandInboxProcessor`, leaving file I/O
        in `MotmDevCommandInbox`, adding focused tests, and ratcheting the
        plugin helper to delegation-only.
  - [x] Move command-facing ability-cast queue policy out of `MenteesMod` into
        `AbilityCastCommandActions`, including request validation, queue
        evidence handoff through `MotmRuntimeTasks`, focused tests, and an
        architecture ratchet keeping the public queue method delegation-only.
  - [x] Move command-facing free-cast access out of `MenteesMod` into
        `FreeCastCommandActions`, including enabled-state readback,
        enable/disable mutation, invulnerability-clear task scheduling/
        cancellation, focused tests, and an architecture smoke check keeping
        public free-cast methods delegation-only.
- [x] Regression battery
  - [x] PowerShell parser checks for setup/harness scripts.
  - [x] `git diff --check`.
  - [x] `scripts/setup-agent-workstation.ps1`.
  - [x] Gradle wrapper build/install with `--warning-mode all`.
  - [x] Static data audits such as `scripts/audit-no-resource.ps1`.
  - [x] Agent observability baseline in Hytale after restarting the game onto
        the newly installed jar.
        Evidence: `mac-internal-baseline-20260525-1441`.
  - [x] Scenario catalog smoke runs on macOS with an internal tester jar loaded:
        `mac-command-observability-smoke-20260525-1442`,
        `mac-terra-projectile-magma-sling-20260525-1442`,
        `mac-terra-field-iron-wall-20260525-1442`,
        `mac-hydro-summon-snow-imp-20260525-1442`,
        `mac-aero-transformation-smoke-form-20260525-1442`, and
        `mac-terra-followup-alloy-enhancement-20260525-1452`.
  - [x] Harness verifies installed jars are real internal tester builds before
        sending runtime commands; this catches stale generated `MotmBuildInfo`
        output instead of timing out against public dev-tool gating.
  - [x] Harness command bridge writes agent-actionable diagnostics on setup or
        acknowledgement failure, including process state, latest client log tail,
        inbox/outbox paths, and recovery steps. Smoke evidence:
        `/tmp/motm-dev-command-diagnostic-test/dev-command-diagnostic.md`.

## Completion Bar

The refactor is complete only when the code structure reflects the target shape,
the old ad hoc ownership is gone, and any remaining legacy support is
intentionally isolated behind compatibility facades with active consumers and
removal gates. Do not keep old internal implementation paths for fallback,
merge comfort, or "maybe useful later" value. The regression battery must prove
the branch still builds, installs, and preserves the verification harness
contracts.
