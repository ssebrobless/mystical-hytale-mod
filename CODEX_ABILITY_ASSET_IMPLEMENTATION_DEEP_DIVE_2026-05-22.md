# Ability Asset Implementation Deep Dive - 2026-05-22

## Shape

```text
Official concept
  │
  ├─ protected style JSON
  │    └─ 40 styles / 120 abilities, no data mutation
  │
  ├─ concept visual bridge
  │    └─ palette + feel + motion shape
  │
  ├─ Hytale asset/API knowledge
  │    ├─ local Assets.zip catalogs
  │    ├─ local HytaleServer.jar discovery
  │    └─ public docs as directional support
  │
  └─ ability asset plan
       ├─ cast asset
       ├─ travel asset
       ├─ impact asset
       ├─ loop asset
       ├─ model candidate
       ├─ implementation bridge
       └─ proof requirement
```

## Generated Artifact

Run:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/generate-ability-asset-plan.ps1 -NoTimestamp
```

Output:

```text
audits/ability-asset-plan/latest/
  ├─ ability-asset-plan.md
  └─ ability-asset-plan.json
```

Current generation result:

```text
Ability rows:                120
Class coverage:              30 per class
Local MOTM effects checked:  30
Hytale asset paths checked:  492
Missing references:          0
Showcase-review rows:        6
```

The six showcase-review rows use existing local portal/void portal assets. They
are valid paths, but they should be viewed in-game before final visual PASS
because portal assets can be too cinematic or too large for combat abilities.

## Research Conclusions

Public Hytale references support the approach already proven locally:

- Hytale's modding strategy describes the modding surface as plugins plus data
  and art assets, with asset-pack/mod distribution still evolving.
  Source: https://hytale.com/news/2025/11/hytale-modding-strategy-and-status
- Custom UI is driven from server-side Java commands against client `.ui`
  markup, which matches the planned Spellbook implementation path.
  Source: https://hytalemodding.dev/pl-PL/docs/official-documentation/custom-ui
- Entity effects are the right bridge for body tint, model changes, particle
  attachment, and duration/overlap behavior.
  Source: https://hytale-docs.pages.dev/modding/systems/entity-effects/
- Local discovery remains the final authority for this mod: the generated plan
  validates every referenced asset against the local asset catalogs before it is
  treated as usable.

## What This Gives Us

```text
Before
  └─ "Cast ability, see if something happened"

After
  ├─ know what the ability concept is supposed to read as
  ├─ know which existing Hytale assets can carry that read
  ├─ know whether those assets exist locally
  ├─ know which runtime bridge must express the motion
  └─ know what screenshot/log proof is required
```

Examples:

```text
Terra / Quake / Stomp
  ├─ cast: MOTM_Terra_Quake_Cast
  ├─ impact: MOTM_Terra_Quake_Impact + Mace_Signature_Ground_Hit_Crack
  ├─ loop: MOTM_Terra_Quake_Loop
  └─ proof: armed jump, landing resolution, targets hit, crack ring screenshot

Hydro / Boiling / Geyser
  ├─ cast: Water_Beam_Spawn
  ├─ travel: Water_Beam
  ├─ impact: Water_Beam_Splash + Impact_Smoke
  ├─ loop: Geyzer
  └─ proof: ground telegraph, vertical eruption, scald/burn result

Aero / Jet / Afterburner
  ├─ cast: Sword ready sparks
  ├─ travel: Wind_Sparks_Tail
  ├─ impact: Battleaxe_Bash_Shockwave
  ├─ loop: Impact_Smoke
  └─ proof: start/end position, trail, damage path, target result

Corruptus / Void / Rift
  ├─ cast: Void_Sparks
  ├─ travel: MagicPortal_VoidSparks
  ├─ impact: VoidImpact
  ├─ loop: MagicPortal_VoidWaves
  └─ proof: field engage/tick/release, pull/status result, showcase review
```

## How To Use It During Implementation

For each ability:

```text
1. Read the generated row.
2. Check the scenario and proof requirement.
3. Implement the missing runtime bridge, if any.
4. Route visuals through HytaleAssetResolver or a local MOTM EntityEffect.
5. Run the ability-specific harness scenario.
6. Capture log proof plus third-person screenshot/video where required.
7. Mark runtime, mechanical, and visual results separately.
```

No ability should receive final visual PASS solely because the server log says
the ability cast. The generated proof requirement is the acceptance bar.

## Next Work Order

```text
1. Keep Phase 6 Spellbook scoped to:
   ├─ Class
   ├─ Style
   ├─ Abilities
   └─ Perks

2. Add a separate dev/test Spellbook variant:
   ├─ same player-facing information
   └─ class/style switching controls for test speed

3. Upgrade harness scenarios:
   ├─ movement abilities must move
   ├─ jump abilities must jump/land
   ├─ self buffs must use third-person view
   ├─ fields must show radius and ticks
   └─ summons must verify role/model plus behavior

4. Begin the class visual/mechanical pass with Terra from the top:
   ├─ Quake
   ├─ Metal
   ├─ Magma
   ├─ Stone
   ├─ Arbor
   ├─ Bloom
   ├─ Self Petrification
   ├─ Soil
   ├─ Sand
   └─ Gem
```
